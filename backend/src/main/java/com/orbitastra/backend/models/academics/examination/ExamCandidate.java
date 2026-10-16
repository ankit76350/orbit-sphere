package com.orbitastra.backend.models.academics.examination;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;
import com.orbitastra.backend.models.academics.examination.embedded.ExamCandidateStatusChange;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One child on one exam's candidate list — the children allowed to sit it.
 *
 * <p><b>The hall register is built from this list, not from the section.</b> A child is on a
 * paper's register only when they are a candidate of that exam, {@code ELIGIBLE}, in that
 * paper's class and section, and they take that paper's subject. A child who is not a
 * candidate, or is {@code DEBARRED} — for example because fees are unpaid — does not appear,
 * so no attendance and no marks can be recorded for them.
 *
 * <p><b>Hall attendance lives here</b>, in {@code papers}: one entry per paper the child was
 * marked at. There is no separate attendance collection, so only a candidate can ever be
 * marked, and eligibility and attendance are read and written in one document.
 *
 * <p><b>Debarring does not rewrite what already happened.</b> A paper the child already sat
 * keeps its entry in {@code papers}. Debarring stops them from the papers still to come, and letting them back
 * in puts them on every paper not yet completed.
 *
 * <p><b>{@code subjectCodes} is for electives.</b> Empty means every paper of the child's
 * section. A list means only those subjects — so a child who does not take Sanskrit is not on
 * the Sanskrit register.
 *
 * <p><b>The class and section are not stored here.</b> They come from the child's academic
 * record: a paper's register reads the section's roster on the paper's date, then the
 * candidates among those children. So a child who moves section mid-exam sits the papers of the
 * section they are in that day, and nothing here goes stale.
 */
@Document(collection = "exam_candidates")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_exam_student_candidate_uniq",
                def = "{'schoolId': 1, 'examDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_exam_candidate_debarment_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'status': 1, 'debarmentReason': 1}"),
        @CompoundIndex(
                name = "school_exam_paper_attendance_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'papers.examScheduleDocsId': 1, 'papers.attendanceStatus': 1}"),
        @CompoundIndex(
                name = "school_exam_answer_copy_no_uniq",
                def = "{'schoolId': 1, 'examDocsId': 1, 'papers.answerCopyNo': 1}",
                unique = true,
                partialFilter = "{'papers.answerCopyNo': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_student_exam_candidate_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCandidate extends AcademicStudentSchoolBase {

    // Links to Exam.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String examDocsId;

    // Links to the child's StudentAcademicRecord.id when they were added.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // The subjects this child sits. Empty means every paper of their section.
    // Example: ["MATH", "ENG", "SANSKRIT"]
    @Builder.Default
    private List<String> subjectCodes = new ArrayList<>();

    // Example: ExamCandidateStatus.ELIGIBLE
    @NotNull
    @Builder.Default
    private ExamCandidateStatus status = ExamCandidateStatus.ELIGIBLE;

    // Why the child is debarred. Empty while ELIGIBLE. Example: ExamDebarmentReason.FEES_DUE
    private ExamDebarmentReason debarmentReason;

    // What the office wrote about the debarment. Empty while ELIGIBLE.
    // Example: "Term 2 fee unpaid."
    private String debarmentNote;

    // Every debarment and every return, oldest first.
    @Builder.Default
    private List<ExamCandidateStatusChange> statusChanges = new ArrayList<>();

    // Hall attendance, one entry per paper the child was marked at. No entry means "not marked".
    @Builder.Default
    private List<ExamPaperAttendance> papers = new ArrayList<>();
}
