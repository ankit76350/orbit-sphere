package com.orbitastra.backend.controllers.academics.examination;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.examattendance.request.ExamHallMarksRequest;
import com.orbitastra.backend.dto.academics.examattendance.response.ExamHallRegisterResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.ExamAttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The exam hall — who sat each paper. Endpoints #14–#17 of
 * {@code controllers/academics/examination/README.md}; #14 and #15 are built. The entries are written into
 * {@code ExamCandidate.papers}.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class ExamAttendanceController {

    private final ExamAttendanceService examAttendanceService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

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

    /**
     * Endpoint #15 — <b>mark the hall</b>: one child, several or all. Re-sending a child corrects
     * their entry. Everything is checked before anything is written; the paper's {@code version}
     * is required and moves on with every save. Answers with the register, as #14.
     *
     * <pre>
     * 400 DUPLICATE_STUDENT_IN_REQUEST   a child is in marks twice
     * 400 ANSWER_COPY_REQUIRED           PRESENT or UNFAIR_MEANS with no booklet
     * 400 ANSWER_COPY_NOT_ALLOWED        ABSENT or EXEMPT with a booklet
     * 400 INVALID_TIME_RANGE             submittedAt before reportedAt
     * 404 ACADEMIC_YEAR_NOT_FOUND        no year of that name in this school
     * 404 EXAM_SCHEDULE_NOT_FOUND        no paper of that id in this year
     * 409 CONCURRENT_MODIFICATION        the paper, or a candidate, changed since it was read
     * 409 PAPER_NOT_OPEN                 the paper is not PUBLISHED
     * 409 PAPER_NOT_YET_SAT              the paper's day has not come, in the school's time zone
     * 400 ROOM_NOT_IN_PAPER              a room that is not one of the paper's
     * 400 ROOM_REQUIRED                  a paper with several rooms, and no room for a child who sat
     * 400 STUDENT_NOT_IN_PAPER           a child #14 does not list — names them
     * 409 STUDENT_DEBARRED               a debarred child with no entry here — names them and why
     * 409 ANSWER_COPY_TAKEN              a booklet already given in this exam, or twice here — names it
     * 400 VALIDATION_FAILED              no marks, no version, or a field too long
     * 400 INVALID_VALUE                  an attendanceStatus that does not exist
     * 409 SCHOOL_NOT_EDITABLE            the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING      the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED            no idtoken cookie
     * </pre>
     */
    @PutMapping("/exam-schedules/{examScheduleDocsId}/attendance")
    public ResponseEntity<ExamHallRegisterResponse> mark(@PathVariable String year,
            @PathVariable String examScheduleDocsId, @Valid @RequestBody ExamHallMarksRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(examAttendanceService.markHall(year, examScheduleDocsId, request));
    }
}
