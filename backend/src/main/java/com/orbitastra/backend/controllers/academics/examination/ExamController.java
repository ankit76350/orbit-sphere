package com.orbitastra.backend.controllers.academics.examination;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.exam.request.ExamCreateRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamMarkEntryRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamPublishRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamSearchRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamUpdateRequest;
import com.orbitastra.backend.dto.academics.exam.response.ExamDetailResponse;
import com.orbitastra.backend.dto.academics.exam.response.ExamMarkEntryResponse;
import com.orbitastra.backend.dto.academics.exam.response.ExamResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.examination.ExamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exams of one academic year — the exam event itself, such as "Half Yearly Examination, Term 1".
 * Endpoints #1–#7 and #26–#28 of {@code controllers/academics/examination/README.md}; #1, #2, #3,
 * #6, #7 and #26 are built.
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

    /**
     * Endpoint #6 — <b>the year's exams</b>, filtered and paged, each with its paper count.
     *
     * <p>Filters, all optional: {@code termDocsId}, {@code examType}, {@code status}, {@code search}.
     * Paging: {@code page}, {@code size}, {@code sort} (startDate, endDate, name, createdAt,
     * updatedAt, each with {@code ,desc}).
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 400 VALIDATION_FAILED           an examType or status that does not exist
     * 400 INVALID_PAGE · INVALID_PAGE_SIZE · INVALID_SORT_FIELD
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ExamResponse>> list(@PathVariable String year,
            ExamSearchRequest request) {

        return ResponseEntity.ok(examService.listExams(year, request));
    }

    /**
     * Endpoint #7 — <b>one exam in full</b>: the exam, its term and scheme named, its papers per
     * status and its marks per status.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_NOT_FOUND              no exam of that id in this year
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/{examDocsId}")
    public ResponseEntity<ExamDetailResponse> get(@PathVariable String year,
            @PathVariable String examDocsId) {

        return ResponseEntity.ok(examService.getExam(year, examDocsId));
    }

    /**
     * Endpoint #2 — <b>fix an exam</b>: name, term, dates, weight or grading scheme. A field left
     * out is left as it is; the exam's {@code version} is required.
     *
     * <pre>
     * 400 NOTHING_TO_UPDATE           nothing but the version was sent
     * 400 EXAM_NAME_REQUIRED          "name": "" — a name can be replaced, not removed
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_NOT_FOUND              no exam of that id in this year
     * 409 CONCURRENT_MODIFICATION     the exam changed since this version was read
     * 409 EXAM_NOT_EDITABLE           the exam is COMPLETED or CANCELLED
     * 409 EXAM_HAS_PAPERS             a new term, and the exam already has papers
     * 404 TERM_NOT_FOUND              the new term is not this year's
     * 409 TERM_NOT_ACTIVE             the new term has been retired
     * 400 INVALID_DATE_RANGE          the dates end up the wrong way round
     * 409 EXAM_DATES_OUTSIDE_TERM     the dates end up outside the term
     * 409 PAPERS_OUTSIDE_DATES        a paper would fall outside the new dates — the message names them
     * 404 GRADING_SCHEME_NOT_FOUND    gradingSchemeDocsId sent, and not this school's
     * 409 GRADING_SCHEME_NOT_ACTIVE   a newly chosen scheme has been retired
     * 409 EXAM_NAME_TAKEN             the new name is used this year, ignoring case
     * 400 VALIDATION_FAILED           no version, or weightPercent not in (0, 100]
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PatchMapping("/{examDocsId}")
    public ResponseEntity<ExamResponse> update(@PathVariable String year,
            @PathVariable String examDocsId, @Valid @RequestBody ExamUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(examService.updateExam(year, examDocsId, request));
    }

    /**
     * Endpoint #3 — <b>publish the datesheet</b>. The exam and every draft paper become
     * {@code PUBLISHED}. The exam's {@code version} is required.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_NOT_FOUND              no exam of that id in this year
     * 409 CONCURRENT_MODIFICATION     the exam changed since this version was read
     * 409 EXAM_ALREADY_PUBLISHED      the exam is PUBLISHED or IN_PROGRESS
     * 409 EXAM_NOT_EDITABLE           the exam is COMPLETED or CANCELLED
     * 409 EXAM_HAS_NO_PAPERS          no papers, cancelled ones left out
     * 409 PAPERS_WITHOUT_ROOMS        a paper has no room — the message names them
     * 409 SECTION_CLASH               a section sits two of its papers at once — names them
     * 400 VALIDATION_FAILED           no version
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping("/{examDocsId}/publish")
    public ResponseEntity<ExamResponse> publish(@PathVariable String year,
            @PathVariable String examDocsId, @Valid @RequestBody ExamPublishRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(examService.publishExam(year, examDocsId, request));
    }

    /**
     * Endpoint #26 — <b>where every paper is with its marks</b>: one row per paper with how many
     * sat, and how many marks are entered, draft and published, and the stage it has reached.
     * {@code ?pending=true} keeps only the papers still holding something up.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 EXAM_NOT_FOUND              no exam of that id in this year
     * 400 VALIDATION_FAILED           pending is not true or false
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/{examDocsId}/mark-entry")
    public ResponseEntity<ExamMarkEntryResponse> markEntry(@PathVariable String year,
            @PathVariable String examDocsId, ExamMarkEntryRequest request) {

        return ResponseEntity.ok(examService.getMarkEntry(year, examDocsId, request));
    }
}
