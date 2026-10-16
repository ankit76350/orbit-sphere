package com.orbitastra.backend.controllers.academics.examination;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.dto.academics.exam.request.ExamCreateRequest;
import com.orbitastra.backend.dto.academics.exam.response.ExamResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.ExamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exams of one academic year — the exam event itself, such as "Half Yearly Examination, Term 1".
 * Endpoints #1–#7 and #26–#28 of {@code controllers/academics/examination/README.md}; #1 is
 * built.
 *
 * <p>An exam is a year's, so every path is under {@code /academic-years/{year}}.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — <b>create an exam</b>. 201, and it starts as {@code DRAFT}.
     *
     * <pre>
     * 400 INVALID_DATE_RANGE          startDate is after endDate
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 TERM_NOT_FOUND              no term of that id in this year
     * 409 TERM_NOT_ACTIVE             the term has been retired
     * 409 EXAM_DATES_OUTSIDE_TERM     the dates are not inside the term — the message names it
     * 404 GRADING_SCHEME_NOT_FOUND    gradingSchemeDocsId sent, and not this school's
     * 409 GRADING_SCHEME_NOT_ACTIVE   the grading scheme has been retired
     * 409 EXAM_NAME_TAKEN             the year has an exam of that name, any case, cancelled too
     * 400 VALIDATION_FAILED           a required field is missing, or weightPercent not in (0, 100]
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<ExamResponse> create(@PathVariable String year,
            @Valid @RequestBody ExamCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        ExamResponse response = examService.createExam(year, request);

        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/exams/"
                        + response.examDocsId()))
                .body(response);
    }
}
