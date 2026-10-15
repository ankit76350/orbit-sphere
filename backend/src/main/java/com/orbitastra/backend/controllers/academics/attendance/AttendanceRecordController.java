package com.orbitastra.backend.controllers.academics.attendance;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceMarksRequest;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceDayResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceRegisterResponse;
import com.orbitastra.backend.dto.academics.attendance.response.StudentAttendanceResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.AttendanceRecordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Attendance records — the marks, and the reads over them. Endpoints #2, #8 and #9 of the plan in
 * this package's README; #10 and #11 are not built.
 *
 * <p><b>Under the year</b>, so each route names its own path: marking stays at
 * {@code /attendance-sessions/{id}/marks}, beside the register it writes into, and the reads are
 * under {@code /attendance}.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #8 — <b>the office's morning question</b>: on one date, which sections have taken
     * attendance and which have not. One row per active section, with its headcount and how many
     * are marked. A section is taken when its DAILY register exists and is not cancelled.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 409 DATE_OUTSIDE_ACADEMIC_YEAR  the date is not inside the year
     * 404 NOT_A_WORKING_DAY           a holiday or weekly off — the message names it
     * 400 MISSING_PARAMETER           no date
     * 400 INVALID_PARAMETER           a date that is not YYYY-MM-DD
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @GetMapping("/attendance/day")
    public ResponseEntity<AttendanceDayResponse> day(@PathVariable String year,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {

        //! NO GATES. A read.
        return ResponseEntity.ok(attendanceRecordService.getDay(year, date));
    }

    /**
     * Endpoint #2 — <b>mark the children</b>: one, several or all at once. Re-sending a child's mark
     * corrects it; children not sent are left alone. Answers with the register as it now stands.
     *
     * <p>Everything is checked before anything is written — nothing is half saved.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND       no year of that name in this school
     * 404 ATTENDANCE_SESSION_NOT_FOUND  no register of that id in this year
     * 409 SESSION_NOT_OPEN              the register is LOCKED or CANCELLED
     * 400 STUDENT_NOT_IN_REGISTER       a child who was not in the section that day — names them
     * 400 REASON_REQUIRED               a mark other than PRESENT with no reason — names the child
     * 400 DUPLICATE_STUDENT_IN_REQUEST  the same child twice in marks
     * 400 NOTHING_TO_MARK               marks is empty
     * 400 VALIDATION_FAILED             a mark with no studentDocsId or status
     * 409 SCHOOL_NOT_EDITABLE           the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING     the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED           no idtoken cookie
     * </pre>
     */
    @PutMapping("/attendance-sessions/{attendanceSessionDocsId}/marks")
    public ResponseEntity<AttendanceRegisterResponse> mark(@PathVariable String year,
            @PathVariable String attendanceSessionDocsId,
            @Valid @RequestBody AttendanceMarksRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(
                attendanceRecordService.markRegister(year, attendanceSessionDocsId, request));
    }

    /**
     * Endpoint #9 — <b>one child's attendance</b> between two dates: every mark, newest first, and
     * the totals — DAILY registers only, with LATE counted as attended. {@code from} and {@code to}
     * default to the year's own dates.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  no year of that name in this school
     * 404 STUDENT_NOT_FOUND        no child of that id in this school
     * 400 INVALID_DATE_RANGE       from is after to
     * 400 INVALID_PARAMETER        a date that is not YYYY-MM-DD
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @GetMapping("/students/{studentDocsId}/attendance")
    public ResponseEntity<StudentAttendanceResponse> studentAttendance(@PathVariable String year,
            @PathVariable String studentDocsId,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {

        //! NO GATES. A read.
        return ResponseEntity.ok(
                attendanceRecordService.getStudentAttendance(year, studentDocsId, from, to));
    }
}
