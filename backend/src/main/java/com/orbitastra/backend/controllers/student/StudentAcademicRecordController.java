package com.orbitastra.backend.controllers.student;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordCreateRequest;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.student.StudentAcademicRecordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Where a child sits, year by year. Endpoint #14 of the plan in this package's README; #15 to #22
 * are not built.
 *
 * <p><b>This is the one everything else waits for.</b> Attendance is taken against a section, a
 * mark sheet lists one, and a timetable is drawn for one. None of them can exist until a child is
 * in a section — and until #14 nothing put them there, which is why every student on the roll read
 * back as {@code placed: false}.
 *
 * <p><b>Its own controller because {@code student_academic_records} is its own collection</b>, and
 * because the route says so: a record hangs off a <i>year</i>, not off a child. The year is what
 * scopes it — a child has one record per year and the same child reappears next year in a
 * different class.
 *
 * <p><b>School surface only.</b> The school comes from the {@code idtoken} cookie and never from
 * the URL.
 *
 * <h2>Gates: 1 and 2 on a write, and deliberately no gate 4</h2>
 *
 * <p>Every write in {@code academics} refuses a year that is not running. <b>This one must
 * not.</b> The handover it exists for runs the other way: a child admitted in January is placed
 * into a year that starts in June, and refusing until June would mean no class list could be built
 * before term began.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}/student-records")
@RequiredArgsConstructor
public class StudentAcademicRecordController {

    private final StudentAcademicRecordService academicRecordService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #14 — <b>put a child in a class and a section</b>.
     *
     * <p><b>Two documents in one transaction</b>: the record is inserted, then the child's
     * {@code currentAcademicRecordDocsId} is pointed at it. Either both happen or neither does.
     *
     * <p><b>One {@code ACTIVE} record per child per year</b>, and the database says so —
     * {@code school_year_student_active_academic_record_uniq}. The service checks first only so
     * the ordinary case answers {@code 409} naming the class they are already in, rather than a
     * duplicate-key 500.
     *
     * <p><b>Moving a child is not this endpoint.</b> That is #17, which closes one record and
     * opens another in the same transaction — a {@code PATCH} of {@code classDocsId} cannot do it,
     * because for an instant two {@code ACTIVE} records would exist, and because editing the class
     * in place erases where the child sat for the first half of the year.
     *
     * <p><b>{@code rollNo} is caller-supplied and not generated</b>, which differs from the plan:
     * the number service allocates against {@code GLOBAL_SCOPE} while the roll index scopes the
     * value to {@code {year, class, section}}. Absent means no roll number, which is a real state.
     *
     * <p><b>No {@code status} field.</b> The plan offers {@code ACTIVE} or {@code PLANNED};
     * {@code PLANNED} was removed from {@code AcademicRecordStatus} on 2026-10-09, and the three
     * values left beside {@code ACTIVE} are terminal states #16 and #17 move a record into. A
     * record created here is always {@code ACTIVE}.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  no year of that name in this school
     * 404 STUDENT_NOT_FOUND        no child of that id in this school
     * 404 CLASS_NOT_FOUND          no class of that id in this school
     * 400 CLASS_NOT_IN_YEAR        a real class, but it belongs to another year
     * 400 SECTION_NOT_IN_CLASS     that class has no section by that sectionNo
     * 400 VALIDATION_FAILED        studentDocsId, classDocsId or sectionNo missing
     * 409 SECTION_NOT_ACTIVE       the section exists but has been switched off
     * 409 STUDENT_NOT_PLACEABLE    the child is WITHDRAWN, TRANSFERRED or GRADUATED
     * 409 STUDENT_ALREADY_PLACED   they already hold an ACTIVE record for this year
     * 409 ROLL_NUMBER_TAKEN        that number is used in that section this year
     * 409 SCHOOL_NOT_EDITABLE      the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<StudentAcademicRecordResponse> assignStudentToClass( @PathVariable String year, 
        @Valid @RequestBody StudentAcademicRecordCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! NO GATE 4, and it is load-bearing: a child admitted in January is placed into a year
        //! that starts in June, so requiring the year to be running would break the handover this
        //! endpoint exists for.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        StudentAcademicRecordResponse response = academicRecordService.assignStudentToClass(year, request);

        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year
                        + "/student-records/" + response.academicRecordDocsId()))
                .body(response);
    }
}
