package com.orbitastra.backend.controllers.academics.attendance;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.academics.attendance.response.AttendanceDayResponse;
import com.orbitastra.backend.services.academics.AttendanceRecordService;

import lombok.RequiredArgsConstructor;

/**
 * Attendance records — the reads over registers and marks. Endpoint #8 of the plan in this
 * package's README; #9 to #11 are not built. Every one is a read and runs no gates.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}/attendance")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;

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
    @GetMapping("/day")
    public ResponseEntity<AttendanceDayResponse> day(@PathVariable String year,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {

        //! NO GATES. A read.
        return ResponseEntity.ok(attendanceRecordService.getDay(year, date));
    }
}
