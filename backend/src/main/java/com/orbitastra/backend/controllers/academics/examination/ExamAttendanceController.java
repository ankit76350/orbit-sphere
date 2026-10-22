package com.orbitastra.backend.controllers.academics.examination;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.academics.examattendance.response.ExamHallRegisterResponse;
import com.orbitastra.backend.services.academics.ExamAttendanceService;

import lombok.RequiredArgsConstructor;

/**
 * The exam hall — who sat each paper. Endpoints #14–#17 of
 * {@code controllers/academics/examination/README.md}; #14 is built. The entries are written into
 * {@code ExamCandidate.papers}.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class ExamAttendanceController {

    private final ExamAttendanceService examAttendanceService;

    /**
     * Endpoint #14 — <b>the hall register</b> of one paper: every eligible candidate of the paper's
     * section on its date, in roll order, with their hall entry or none. A debarred child is counted,
     * not listed.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_SCHEDULE_NOT_FOUND     no paper of that id in this year
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/exam-schedules/{examScheduleDocsId}/attendance")
    public ResponseEntity<ExamHallRegisterResponse> register(@PathVariable String year,
            @PathVariable String examScheduleDocsId) {

        return ResponseEntity.ok(examAttendanceService.getHallRegister(year, examScheduleDocsId));
    }
}
