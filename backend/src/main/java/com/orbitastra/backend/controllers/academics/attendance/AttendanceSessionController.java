package com.orbitastra.backend.controllers.academics.attendance;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionCreateRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionLockRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionUnlockRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionSearchRequest;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceRegisterResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceSessionResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceSessionRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.attendance.AttendanceSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Attendance registers. Endpoints #1, #3, #4, #6 and #7 of the plan in this package's README.
 * Marking (#2) is on {@link AttendanceRecordController}; #5 is not built.
 *
 * <p><b>Under the year</b>, like the timetable: a register is a year's, and gate 4 asks about the
 * year in the path.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}/attendance-sessions")
@RequiredArgsConstructor
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — <b>open a register</b> for one section on one date. 201, or 200 when a
     * CANCELLED register was opened again.
     *
     * <p>{@code DAILY} is once for the day; {@code PERIOD} is one timetable period, and its timetable entry and
     * teacher are copied from the timetable. The teacher defaults to the period's, or the
     * section's class teacher for {@code DAILY}.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 CLASS_NOT_FOUND             no class of that id in this year
     * 400 SECTION_NOT_IN_CLASS        the class has no section by that sectionNo
     * 409 SECTION_NOT_ACTIVE          the section has been switched off
     * 409 DATE_OUTSIDE_ACADEMIC_YEAR  the date is not inside the year
     * 404 NOT_A_WORKING_DAY           the date is a holiday or weekly off — the message names it
     * 400 PERIOD_CODE_REQUIRED        PERIOD with no periodCode
     * 400 PERIOD_CODE_NOT_ALLOWED     DAILY with a periodCode
     * 404 PERIOD_NOT_IN_TIMETABLE     the section has no such period that day, or no timetable
     * 400 TIMETABLE_NOT_FOR_DATE      timetableDocsId sent, and not that date's timetable
     * 400 TIMETABLE_DOCS_ID_NOT_ALLOWED  DAILY with a timetableDocsId
     * 404 TEACHER_NOT_FOUND           teacherDocsId sent, and not this school's staff
     * 409 SESSION_ALREADY_OPEN        the register exists — the message carries its id
     * 400 VALIDATION_FAILED           a required field is missing
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<AttendanceSessionResponse> open(@PathVariable String year,
            @Valid @RequestBody AttendanceSessionCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        AttendanceSessionResponse response = attendanceSessionService.openSession(year, request);

        if (response.reopened()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year
                        + "/attendance-sessions/" + response.attendanceSessionDocsId()))
                .body(response);
    }

    /**
     * Endpoint #7 — <b>registers, one page at a time</b>. Filters, all optional: {@code date},
     * {@code classDocsId}, {@code sectionNo}, {@code sessionType}, {@code status}. Newest date
     * first. Each row carries its mark counts per status.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  no year of that name in this school
     * 400 INVALID_PAGE             page is negative
     * 400 INVALID_PAGE_SIZE        size is not between 1 and 100
     * 400 VALIDATION_FAILED        a date, kind or status that does not parse
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<AttendanceSessionRowResponse>> list(@PathVariable String year,
            AttendanceSessionSearchRequest request) {

        //! NO GATES. A read.
        return ResponseEntity.ok(attendanceSessionService.listSessions(year, request));
    }

    /**
     * Endpoint #6 — <b>one register in full</b>: every child who was in the section on its date,
     * each with their mark or none yet. What the teacher's screen opens.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND       no year of that name in this school
     * 404 ATTENDANCE_SESSION_NOT_FOUND  no register of that id in this year
     * 400 TENANT_NOT_RESOLVED           no idtoken cookie
     * </pre>
     */
    @GetMapping("/{attendanceSessionDocsId}")
    public ResponseEntity<AttendanceRegisterResponse> getOne(@PathVariable String year,
            @PathVariable String attendanceSessionDocsId) {

        //! NO GATES. A read — a suspended school still reads its own registers.
        return ResponseEntity.ok(attendanceSessionService.getRegister(year, attendanceSessionDocsId));
    }

    /**
     * Endpoint #3 — <b>lock a register</b>: it is final and takes no more marks. Refused while any
     * child is unmarked. The version is the register's, as last read. Answers with the register.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND       no year of that name in this school
     * 404 ATTENDANCE_SESSION_NOT_FOUND  no register of that id in this year
     * 409 SESSION_NOT_OPEN              the register is already LOCKED, or CANCELLED
     * 409 CONCURRENT_MODIFICATION       the register changed since it was read
     * 409 UNMARKED_STUDENTS             children still without a mark — says how many
     * 400 VALIDATION_FAILED             no version
     * 409 SCHOOL_NOT_EDITABLE           the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING     the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED           no idtoken cookie
     * </pre>
     */
    @PostMapping("/{attendanceSessionDocsId}/lock")
    public ResponseEntity<AttendanceRegisterResponse> lock(@PathVariable String year,
            @PathVariable String attendanceSessionDocsId,
            @Valid @RequestBody AttendanceSessionLockRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(
                attendanceSessionService.lockRegister(year, attendanceSessionDocsId, request));
    }

    /**
     * Endpoint #4 — <b>open a locked register again</b> to correct it. The reason is required and
     * is saved on the register's unlock history, with when, by whom and the lock it undid.
     *
     * <pre>
     * 400 REASON_REQUIRED               no reason, or a blank one
     * 404 ACADEMIC_YEAR_NOT_FOUND       no year of that name in this school
     * 404 ATTENDANCE_SESSION_NOT_FOUND  no register of that id in this year
     * 409 SESSION_NOT_LOCKED            the register is OPEN or CANCELLED
     * 409 CONCURRENT_MODIFICATION       the register changed since it was read
     * 400 VALIDATION_FAILED             no version, or a reason over 500 characters
     * 409 SCHOOL_NOT_EDITABLE           the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING     the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED           no idtoken cookie
     * </pre>
     */
    @PostMapping("/{attendanceSessionDocsId}/unlock")
    public ResponseEntity<AttendanceRegisterResponse> unlock(@PathVariable String year,
            @PathVariable String attendanceSessionDocsId,
            @Valid @RequestBody AttendanceSessionUnlockRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(
                attendanceSessionService.unlockRegister(year, attendanceSessionDocsId, request));
    }
}
