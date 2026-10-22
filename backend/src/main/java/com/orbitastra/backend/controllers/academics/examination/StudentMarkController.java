package com.orbitastra.backend.controllers.academics.examination;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.academics.studentmark.response.ExamMarkSheetResponse;
import com.orbitastra.backend.services.academics.StudentMarkService;

import lombok.RequiredArgsConstructor;

/**
 * Marks — what each child scored on each paper they sat. Endpoints #18–#25 of
 * {@code controllers/academics/examination/README.md}; #18 is built.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class StudentMarkController {

    private final StudentMarkService studentMarkService;

    /**
     * Endpoint #18 — <b>the mark sheet</b> of one paper: every child with a hall entry, in roll
     * order. Those PRESENT carry their mark or none yet; the rest carry their hall status and
     * cannot be marked. Names the grading scheme the paper's grades come from.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_SCHEDULE_NOT_FOUND     no paper of that id in this year
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/exam-schedules/{examScheduleDocsId}/marks")
    public ResponseEntity<ExamMarkSheetResponse> markSheet(@PathVariable String year,
            @PathVariable String examScheduleDocsId) {

        return ResponseEntity.ok(studentMarkService.getMarkSheet(year, examScheduleDocsId));
    }
}
