package com.orbitastra.backend.controllers.student;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordCreateRequest;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordTransferRequest;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordUpdateRequest;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordHistoryResponse;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordTransferResponse;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.student.StudentAcademicRecordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Where a child sits, year by year. Endpoints #14, #15, #17 and #20 of the plan in this package's
 * README; the close (#16), the roster (#21) and the strength table (#22) are not built.
 *
 * <p><b>This is the one everything else waits for.</b> Attendance is taken against a section, a
 * mark sheet lists one, and a timetable is drawn for one. None of them can exist until a child is
 * in a section — and until #14 nothing put them there, which is why every student on the roll read
 * back as {@code placed: false}.
 *
 * <p><b>Its own controller because {@code student_academic_records} is its own collection</b> —
 * and the base mapping is {@code /schools/current} rather than one route, because this collection
 * is reached from two directions and the plan says so. <b>A write hangs off a year</b>
 * ({@code /academic-years/{year}/student-records}), because that is what scopes a placement.
 * <b>A history hangs off a child</b> ({@code /students/{id}/academic-records}), because that is
 * what the question is about. #21 and #22 will add a third shape, under a class and a year.
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
@RequestMapping("/schools/current")
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
     * values left beside {@code ACTIVE} are terminal states #16 and #17 put a record into. A
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
    @PostMapping("/academic-years/{year}/student-records")
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

    /**
     * Endpoint #20 — <b>a child's whole history, newest year first</b>.
     *
     * <p><b>Terminal records included.</b> The question is where this child has been, and a closed
     * record is most of the answer — only a read asking "where are they now" filters on
     * {@code ACTIVE}.
     *
     * <p><b>{@code academicYear} narrows it to one year and is optional.</b> Even one year is a
     * list: #17 closes one record and opens another every time a child changes section. The year
     * is echoed back in the answer, which is how a caller tells a narrowed read from a whole one
     * without comparing it against what they sent.
     *
     * <p><b>The sort is the index order, deliberately</b> —
     * {@code school_student_academic_record_history_idx} is
     * {@code {schoolId, studentDocsId, academicYear: -1, effectiveFrom: -1}}, so asking for that
     * order lets Mongo walk the index and skip the sort stage.
     *
     * <p><b>Two reads, never one per row.</b> The class names behind the ids are resolved in a
     * single query — a read per record is the N+1 the plan names for #21.
     *
     * <p><b>An empty list is a real answer, not a 404.</b> A child admitted in January and not yet
     * placed has no records; that is the state the roll shows as {@code placed: false}. The
     * <b>child</b> is what has to exist, and that is what the 404 is about.
     *
     * <p><b>Each row carries {@code current}</b>, which is not the same question as
     * {@code status == ACTIVE}: the student document holds a second copy of that fact, and keeping
     * both is how a disagreement between them becomes visible rather than silently resolved.
     *
     * <p><b>No gates.</b> A read — and a suspended school still has to answer where its children
     * sat.
     *
     * <pre>
     * 404 STUDENT_NOT_FOUND    no child of that id in this school
     * 400 TENANT_NOT_RESOLVED  no idtoken cookie
     * </pre>
     */
    @GetMapping("/students/{studentDocsId}/academic-records")
    public ResponseEntity<StudentAcademicRecordHistoryResponse> getStudentAcademicRecords(
            @PathVariable String studentDocsId,
            @RequestParam(required = false) String academicYear) {

        //! NO GATES. A read — a suspended school still reads its own records.
        return ResponseEntity.ok(academicRecordService.getStudentAcademicRecords(studentDocsId, academicYear));
    }

    /**
     * Endpoint #17 — <b>transfer a child to another class or section</b>.
     *
     * <p>In one transaction: <b>close the open record as {@code TRANSFERRED}</b>, insert a new
     * {@code ACTIVE} one chained back through {@code previousAcademicRecordDocsId}, and repoint
     * the child.
     *
     * <p><b>Why this is not a {@code PATCH} of the class.</b> The mechanical reason is the index:
     * two {@code ACTIVE} records for one child in one year are forbidden, so the close and the
     * open cannot be two requests. The reason that matters is that editing the class in place
     * <b>erases where the child sat for the first half of the year</b>, which is what that half's
     * attendance and marks are attached to. Correcting a placement typed wrongly is #15.
     *
     * <p><b>{@code TRANSFERRED}, not {@code COMPLETED}</b>, which differs from the plan. The enum
     * documents {@code TRANSFERRED} as "placement ended because the student changed class or
     * section" — this and only this. {@code COMPLETED} is for a year that ended normally.
     *
     * <p><b>It takes the child, not the record.</b> The plan puts the record id in the path; this
     * finds the open record from the child and the year, because that is the question a caller
     * has and the id is a thing they would otherwise look up in order to say it.
     *
     * <p><b>The class defaults to the one they are in</b>, so a section transfer inside a class needs
     * only a section. With no open record there is nothing to default to, and it is required.
     *
     * <p><b>A child with no open record is not refused</b> — there is nothing to close, so this is
     * a first placement, and {@code transferred: false} in the answer says so.
     *
     * <p><b>The roll number does not carry over.</b> Roll numbers are scoped to a section, so the
     * one held in 7A says nothing about what is free in 7B.
     *
     * <p><b>{@code version} is the CHILD'S, and it is checked before the first write.</b> A
     * transfer touches two documents and cannot be half-done, so the one check that can refuse it
     * has to happen before anything is saved. It catches two people acting on one child at once —
     * a second transfer, or a placement by #14, between the caller's read and their write — either
     * of which would leave this caller closing a record the child is no longer in.
     *
     * <p><b>No gate 4</b>, the same as #14: a year that is not running can still be rearranged.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  no year of that name in this school
     * 404 STUDENT_NOT_FOUND        no child of that id in this school
     * 404 CLASS_NOT_FOUND          no class of that id in this school
     * 400 CLASS_REQUIRED           no open record, so there is no current class to default to
     * 400 CLASS_NOT_IN_YEAR        a real class, but it belongs to another year
     * 400 SECTION_NOT_IN_CLASS     that class has no section by that sectionNo
     * 400 VALIDATION_FAILED        studentDocsId, sectionNo or version missing
     * 409 SECTION_NOT_ACTIVE       the section exists but has been switched off
     * 409 STUDENT_NOT_PLACEABLE    the child is WITHDRAWN, TRANSFERRED or GRADUATED
     * 409 ALREADY_IN_THAT_SECTION  they are already there; correcting a detail is #15
     * 409 ROLL_NUMBER_TAKEN        that number is held in the NEW section this year
     * 409 CONCURRENT_MODIFICATION  the CHILD was changed by somebody else first
     * 409 SCHOOL_NOT_EDITABLE      the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @PostMapping("/academic-years/{year}/student-records/transfer")
    public ResponseEntity<StudentAcademicRecordTransferResponse> transfer(@PathVariable String year,
            @Valid @RequestBody StudentAcademicRecordTransferRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4, the same as #14: a year that is not running can still be rearranged.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(academicRecordService.transferStudent(year, request));
    }

    /**
     * Endpoint #15 — <b>correct the open record</b>.
     *
     * <p>The typo endpoint: a roll number entered wrongly, a start date a day out. <b>It changes
     * what the record always should have said</b>, so it writes no history and is the only
     * academic-record write that touches a single document.
     *
     * <p>It corrects <b>{@code sectionNo}, {@code rollNo} and {@code effectiveFrom}</b>, and
     * nothing else. <b>Not the class</b> — that is #17, because moving between classes is never a
     * typo. <b>Not the status</b> — that is #16. <b>Not {@code effectiveUntil}</b>, which belongs
     * to a closed record.
     *
     * <p><b>A corrected {@code sectionNo} must exist on the class the record already names, and
     * be active</b> — a section lives inside a class document, so a letter valid in 7 need not be
     * valid in 8. Sending {@code ""} is refused: a record cannot be left without a section.
     *
     * <p><b>The section is here AND on #17, and the difference is what happened.</b> This one is
     * for <i>somebody typed B when the child has always been in C</i> — it corrects in place and
     * <b>writes no history</b>, because there was never a day the child sat in B. #17 is for
     * <i>the child was in B and is now in C</i>, which closes the old record with an end date and
     * opens a new one — and <b>that boundary is what the first half of the year's attendance hangs
     * off</b>. Using this one for a real move silently erases where the child was, and nothing can
     * detect it: only the caller knows which happened.
     *
     * <p>Not offering the class here keeps that mistake small — a correction can put a child in
     * the wrong section of the right class, never in the wrong class.
     *
     * <p><b>Only the child's current record can be edited.</b> The record is named in the path,
     * and it must be the one {@code Student.currentAcademicRecordDocsId} points to — anything else is {@code 409 RECORD_NOT_CURRENT}. That record must then be in the
     * path's year and still {@code ACTIVE}. A typo in last term's roll number is unreachable.
     *
     * <p><b>{@code rollNo: ""} clears it</b>, and a record with no roll number is a real state.
     * Absent leaves it alone. Their own record never blocks them — and the number is checked
     * against the section it will <b>end up</b> in, which is the corrected one when both fields
     * are sent together. A number <i>kept</i> across a corrected section is checked there too.
     *
     * <p><b>{@code effectiveFrom} still cannot be in the past</b>, which makes this narrower than
     * it looks: a start date typed as last Tuesday can only be corrected forward.
     *
     * <p><b>{@code version} is the RECORD'S</b>, not the child's — unlike #17, which repoints the
     * child and so guards them. #20 returns it on every row.
     *
     * <p><b>A body that moves nothing is refused</b>, including one that names the values already
     * stored: a 200 there would report a correction that did not happen.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  no year of that name in this school
     * 404 STUDENT_NOT_FOUND        no child of that id in this school
     * 404 NO_ACTIVE_RECORD         the child has no current academic record
     * 409 RECORD_NOT_CURRENT       the record in the path is not the child's current record
     * 404 ACADEMIC_RECORD_NOT_FOUND the current record the child points to does not exist
     * 400 RECORD_NOT_IN_YEAR       the current record belongs to another academic year
     * 409 RECORD_NOT_OPEN          the current record is not ACTIVE
     * 404 CLASS_NOT_FOUND          the class the record names no longer exists
     * 400 NOTHING_TO_UPDATE        the body moves nothing, or names what is already stored
     * 400 SECTION_REQUIRED         sectionNo sent as "" — a record cannot have none
     * 400 SECTION_NOT_IN_CLASS     that class has no section by that sectionNo
     * 400 VALIDATION_FAILED        no studentDocsId, no version, or a past effectiveFrom
     * 409 SECTION_NOT_ACTIVE       the section exists but has been switched off
     * 409 ROLL_NUMBER_TAKEN        that number is held by somebody else in the target section
     * 409 CONCURRENT_MODIFICATION  the RECORD was changed by somebody else first
     * 409 SCHOOL_NOT_EDITABLE      the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @PatchMapping("/academic-years/{year}/student-records/{academicRecordDocsId}")
    public ResponseEntity<StudentAcademicRecordResponse> correctCurrent(@PathVariable String year,
            @PathVariable String academicRecordDocsId,
            @Valid @RequestBody StudentAcademicRecordUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4, the same as the rest of this collection.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(academicRecordService.updateCurrentRecord(year, academicRecordDocsId, request));
    }
}
