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
import com.orbitastra.backend.dto.academics.studentmark.request.StudentMarksEnterRequest;
import com.orbitastra.backend.dto.academics.studentmark.response.ExamMarkSheetResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.StudentMarkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Marks — what each child scored on each paper they sat. Endpoints #18–#25 of
 * {@code controllers/academics/examination/README.md}; #18 and #19 are built.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class StudentMarkController {

    private final StudentMarkService studentMarkService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

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

    /**
     * Endpoint #19 — <b>enter marks</b> on one paper: one child, several or all. Re-sending a child
     * corrects their mark. Marks or a grade, not both; the grade is worked out from the marks.
     * Everything is checked before anything is written; the paper's {@code version} is required
     * and moves on. Answers with the mark sheet, as #18.
     *
     * <pre>
     * 400 DUPLICATE_STUDENT_IN_REQUEST   a child is in marks twice
     * 400 MARKS_AND_GRADE_BOTH_SENT      a child has both obtainedMarks and gradeCode
     * 400 MARK_REQUIRED                  a child has neither
     * 404 ACADEMIC_YEAR_NOT_FOUND        no year of that name in this school
     * 404 EXAM_SCHEDULE_NOT_FOUND        no paper of that id in this year
     * 409 CONCURRENT_MODIFICATION        the paper, or a mark, changed since it was read
     * 409 PAPER_NOT_COMPLETED            the paper's hall is not completed yet
     * 409 RESULTS_LOCKED                 the year's or the term's results are locked — says which
     * 409 MARKS_NOT_EDITABLE             the paper's marks are submitted or locked
     * 400 STUDENT_NOT_PRESENT            a child not marked PRESENT at this paper — names them
     * 400 MARKS_OUT_OF_RANGE             a mark below 0 or above maximumMarks — names the child
     * 400 GRADE_NOT_IN_SCHEME            a gradeCode that is not a band of the paper's scheme
     * 400 VALIDATION_FAILED              no marks, no version, or a field too long
     * 409 SCHOOL_NOT_EDITABLE            the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING      the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED            no idtoken cookie
     * </pre>
     */
    @PutMapping("/exam-schedules/{examScheduleDocsId}/marks")
    public ResponseEntity<ExamMarkSheetResponse> enter(@PathVariable String year,
            @PathVariable String examScheduleDocsId, @Valid @RequestBody StudentMarksEnterRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(studentMarkService.enterMarks(year, examScheduleDocsId, request));
    }
}
