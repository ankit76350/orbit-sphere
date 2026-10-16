package com.orbitastra.backend.models.academics.examination.embedded;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One candidate's hall attendance at one paper, with the answer booklet they wrote in.
 *
 * <p>It has no collection of its own. It lives in {@code ExamCandidate.papers}, one entry per
 * paper the child was marked at — so only a candidate can ever have hall attendance, and a
 * child with no entry for a paper is simply "not marked yet".
 *
 * <p><b>A booklet is used once in the whole exam.</b> The unique index on
 * {@code papers.answerCopyNo} stops two children holding the same booklet. It cannot stop one
 * child holding the same number twice, or a number turning up in
 * {@code additionalAnswerCopyNos}, so the service checks those before saving. Schools that
 * restart numbering per subject prefix it, for example {@code "MATH-0001"}.
 *
 * <p>Late arrival and early departure are not stored: they are {@code reportedAt} after the
 * paper's start time and {@code submittedAt} before its end time. Hall attendance for a paper
 * closes when its {@code ExamSchedule} reaches {@code COMPLETED}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPaperAttendance {

    // Links to ExamSchedule.id — the paper. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String examScheduleDocsId;

    // Example: ExamAttendanceStatus.PRESENT
    @NotNull
    private ExamAttendanceStatus attendanceStatus;

    // The answer booklet issued. Required when PRESENT or UNFAIR_MEANS, empty when ABSENT.
    // Example: "MATH-0041"
    private String answerCopyNo;

    // Supplementary booklets issued during the paper. Example: ["MATH-0102"]
    @Builder.Default
    private List<String> additionalAnswerCopyNos = new ArrayList<>();

    // Seat or desk in the hall. Example: "H2-14"
    private String seatNo;

    // When the child came into the hall. Example: 2026-11-12T03:35:00Z
    private Instant reportedAt;

    // When the child handed in the booklet. Example: 2026-11-12T06:30:00Z
    private Instant submittedAt;

    // Links to the Staff.id who marked it, when the caller was signed in as staff.
    // Example: "67aa15d9dc3f7d0044444444"
    private String markedByDocsId;

    // Example: 2026-11-12T03:40:00Z
    @NotNull
    private Instant markedAt;

    // DocumentRecord.id values, such as an unfair-means report. Example: ["67aa15d9dc3f7d0055555555"]
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Example: "Admitted 20 minutes late with the exam controller's approval."
    private String remarks;
}
