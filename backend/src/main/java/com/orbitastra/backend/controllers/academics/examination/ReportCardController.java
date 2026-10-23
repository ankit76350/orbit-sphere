package com.orbitastra.backend.controllers.academics.examination;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.reportcard.request.ReportCardSaveRequest;
import com.orbitastra.backend.dto.academics.reportcard.response.ReportCardDraftResponse;
import com.orbitastra.backend.dto.academics.reportcard.response.ReportCardResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.examination.ReportCardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Report cards — a child's results for a term, made one student record at a time and snapshotted.
 * Endpoints #29–#35 of {@code controllers/academics/examination/README.md}; #29 and #29b are built.
 *
 * <p>Addressed by the <b>student record</b> — the child's placement this year — because that is
 * what a card snapshots: its class, section and roll number.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}/student-records/{studentAcademicRecordDocsId}")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #29b — <b>one student's report card, worked out but not saved</b>: subjects with every
     * paper as a component, totals, grades, result and attendance, from the term's exams and the
     * register, for the record's section. Not-final marks and weights that do not add up come back
     * as {@code warnings}, never a refusal, so the form can always be filled. Its fields are the
     * body #29 takes.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 STUDENT_RECORD_NOT_FOUND    no student record of that id in this year
     * 409 STUDENT_RECORD_CANCELLED    the record was cancelled
     * 404 TERM_NOT_FOUND              no term of that id in this year
     * 400 VALIDATION_FAILED           termDocsId missing
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/report-cards/draft")
    public ResponseEntity<ReportCardDraftResponse> draft(@PathVariable String year,
            @PathVariable String studentAcademicRecordDocsId, @RequestParam String termDocsId) {

        return ResponseEntity.ok(reportCardService.draftCard(year, studentAcademicRecordDocsId, termDocsId));
    }

    /**
     * Endpoint #29 — <b>save one student's report card</b> for a term, as DRAFT — what is sent is
     * what is stored, usually #29b's draft after editing. The record's class, section and roll and
     * the term's name are the server's. A DRAFT or REVIEWED card is replaced; after a REVOKED one,
     * the next version. Always 200 with the saved card — {@code reportVersion} and {@code nextStep}
     * say whether it was new or replaced.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 404 STUDENT_RECORD_NOT_FOUND    no student record of that id in this year
     * 409 STUDENT_RECORD_CANCELLED    the record was cancelled
     * 404 TERM_NOT_FOUND              no term of that id in this year
     * 409 RESULTS_LOCKED              the year's or the term's results are locked — says which
     * 400 CARD_NUMBERS_DO_NOT_ADD_UP  marks above their maximum, a subject twice, attendance above the days
     * 409 CARD_ALREADY_PUBLISHED      the student's card for the term is published — revoke it first
     * 400 VALIDATION_FAILED           a required field missing, a negative number, a percentage above 100
     * 400 INVALID_VALUE               a resultStatus, subjectType or attendanceStatus that does not exist
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING   the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping("/report-cards")
    public ResponseEntity<ReportCardResponse> save(@PathVariable String year,
            @PathVariable String studentAcademicRecordDocsId, @Valid @RequestBody ReportCardSaveRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(reportCardService.saveCard(year, studentAcademicRecordDocsId, request));
    }
}
