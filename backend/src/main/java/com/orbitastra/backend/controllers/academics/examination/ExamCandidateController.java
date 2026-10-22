package com.orbitastra.backend.controllers.academics.examination;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.examcandidate.request.ExamCandidatesAddRequest;
import com.orbitastra.backend.dto.academics.examcandidate.response.ExamCandidatesAddedResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.ExamCandidateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * An exam's candidates — the children allowed to sit it. Endpoints #43–#49 of
 * {@code controllers/academics/examination/README.md}; #43 is built.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class ExamCandidateController {

    private final ExamCandidateService examCandidateService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #43 — <b>add the children who may sit</b>: a whole section, or the children named.
     * 201 when anyone was added, 200 when everyone already was.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_NOT_FOUND              no exam of that id in this year
     * 409 EXAM_NOT_EDITABLE           the exam is COMPLETED or CANCELLED
     * 404 CLASS_NOT_FOUND             no class of that id in this year
     * 400 SECTION_NOT_IN_CLASS        the class has no such section
     * 409 SECTION_NOT_ACTIVE          the section has been switched off
     * 400 STUDENT_NOT_IN_SECTION      a named child is not in the section during the exam — names them
     * 400 VALIDATION_FAILED           no classDocsId or sectionNo, or more than 200 children
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping("/exams/{examDocsId}/candidates")
    public ResponseEntity<ExamCandidatesAddedResponse> add(@PathVariable String year,
            @PathVariable String examDocsId, @Valid @RequestBody ExamCandidatesAddRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        ExamCandidatesAddedResponse response = examCandidateService.addCandidates(year, examDocsId,
                request);

        if (response.addedCount() == 0) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/exams/"
                        + response.examDocsId() + "/candidates"))
                .body(response);
    }
}
