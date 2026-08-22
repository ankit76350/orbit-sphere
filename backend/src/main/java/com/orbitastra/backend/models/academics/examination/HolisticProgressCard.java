package com.orbitastra.backend.models.new_new.academics.examination;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.NepStage;
import com.orbitastra.backend.models.new_new.academics.enums.ReportCardStatus;
import com.orbitastra.backend.models.new_new.academics.examination.embedded.DomainAssessment;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A rounded picture of one child over one term, in words rather than marks.
 *
 * <p>The Holistic Progress Card is what the 2020 education policy asks schools to give
 * families instead of a card that says 62 percent. It reports on how a child is developing
 * across several domains, in language a parent and the child can both read, with the teacher's
 * observations and the child's own view of themselves side by side.
 *
 * <p>**It sits beside ReportCard, not instead of it.** Most schools will produce both for
 * years: marks because a board and the next school ask for them, and this because it is
 * required and because it says things a mark cannot. Making one a variant of the other would
 * force a marks-shaped model onto something that has no marks in it. They are siblings, and a
 * child has one of each per term.
 *
 * <p>{@code domains} is the card. Everything else is context around it. A domain carries a
 * level, but the observation is what matters: a card of bare levels is a report card with
 * nicer words, and the service treats one as unfinished.
 *
 * <p>Three voices besides the teacher's, and this is what makes the card different in kind
 * from a report card rather than only in wording:
 *
 * <ul>
 * <li>{@code selfReflection} is the child's own account. On a report card the child is only
 * ever the subject; here they are one of the authors.</li>
 * <li>{@code peerFeedback} is what classmates said, summarised by the teacher. It is
 * summarised rather than collected verbatim because a card handed to a parent must not become
 * a place where one child's words about another are quoted back.</li>
 * <li>{@code parentFeedback} is the family's own view, and
 * {@code parentFeedbackByGuardianDocsId} says which of them wrote it. A card that asks for a
 * parent's view and then does not record whose view it was has not really asked.</li>
 * </ul>
 *
 * <p>{@code nepStage} is stored rather than worked out from the class, because a class is
 * renamed and reorganised over the years while the stage a child was in does not change after
 * the fact. A card for a five-year-old and one for a fifteen-year-old are different documents,
 * and years later only this field says which kind was being read.
 *
 * <p>Class, section and roll number are snapshotted for the same reason ReportCard snapshots
 * them: a card reprinted after the child has moved up has to show the class they were in when
 * it was written.
 *
 * <p>A published card is never edited. A mistake means a new version with
 * {@code cardVersion + 1} and the old one revoked, which is how ReportCard already behaves. A
 * document a family has been given is not something the school can quietly change.
 *
 * <p>{@code developmentGoals} is the one part of the card the school is promising something
 * about, and it is what next term's card should be read against. A goal nobody revisits is a
 * sentence written to fill a box.
 *
 * <p>The service checks that every domain the policy names is present for the child's stage,
 * that each domain carries an observation and not only a level, that a published card is never
 * edited, that a domain is not listed twice, and that publishing requires a named member of
 * staff.
 */
@Document(collection = "holistic_progress_cards")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_hpc_term_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'termDocsId': 1, 'cardVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_hpc_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'cardVersion': -1}"),
        @CompoundIndex(
                name = "school_year_hpc_class_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionNo': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_hpc_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'termDocsId': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HolisticProgressCard extends AcademicStudentSchoolBase {

    // Links to StudentAcademicRecord.id this card belongs to.
    // Example: "67aa15d9dc3f7d0077777777"
    private String studentAcademicRecordDocsId;

    // Links to AcademicTerm.id the card covers. Example: "67ab5511dc3f7d0099887766"
    @NotBlank
    private String termDocsId;

    // Term name copied in, so an old card reads without loading the term.
    // Example: "Term 1 - April to September"
    @NotBlank
    private String termName;

    // Which stage of schooling the child was in. Stored rather than worked out, because a
    // class is renamed over the years and the stage is not. Example: NepStage.FOUNDATIONAL
    @NotNull
    private NepStage nepStage;

    // Links to SchoolClass.id, snapshotted so a reprint shows the class the child was
    // actually in. Example: "67ab3322dc3f7d0044556677"
    private String classDocsId;

    // Class name copied in at the same time. Example: "Class II"
    private String className;

    // Section the child was in. Example: "A"
    private String sectionNo;

    // Roll number at the time. Example: "14"
    private String rollNo;

    // Goes up when a published card has to be replaced. The old one is revoked rather than
    // edited. Example: 1
    @NotNull
    @Builder.Default
    private Integer cardVersion = 1;

    // The card itself: one entry per area of development, each with an observation and not
    // only a level. A card with no domains says nothing.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<DomainAssessment> domains = new ArrayList<>();

    // The teacher's overall view of the child this term, in words a parent can read.
    // Example: "A settled, curious term. She has found her voice in group work and now
    // volunteers to read aloud."
    private String teacherRemark;

    // The child's own account of their term. On a report card the child is only ever the
    // subject; here they are one of the authors.
    // Example: "I liked making the volcano. Writing is still hard for me."
    private String selfReflection;

    // What classmates said, summarised by the teacher rather than quoted, so a card handed
    // to a family never becomes a place where one child's words about another are repeated.
    // Example: "Classmates say she shares materials and explains things patiently."
    private String peerFeedback;

    // The family's own view of how the child is getting on.
    // Example: "She talks about school at home now, which she did not last year."
    private String parentFeedback;

    // Links to Guardian.id for whoever in the family wrote it. Asking for a parent's view
    // and not recording whose view it was is not really asking.
    // Example: "67aa15d9dc3f7d0066666666"
    private String parentFeedbackByGuardianDocsId;

    // What the school will work on next term. The one part of the card the school is
    // promising something about, and what next term's card should be read against.
    // Example: "Build writing stamina with short daily tasks; keep encouraging her to read
    // aloud to the class."
    private String developmentGoals;

    // How many days the school was open, copied in as ReportCard does.
    // Example: 108
    private Integer attendanceWorkingDays;

    // How many of them the child was there for. Example: 101
    private Integer attendancePresentDays;

    // Links to Staff.id for the teacher who wrote the card.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String assessedByStaffDocsId;

    // Example: ReportCardStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    // When the family could first see it. Example: 2026-10-12T05:30:00Z
    private Instant publishedAt;

    // Links to Staff.id for whoever published it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String publishedByDocsId;

    // Why a published card was replaced. Required when the status is REVOKED.
    // Example: "Domains for the wrong term were entered; corrected card issued."
    private String revocationReason;

    // Links to DocumentRecord.id for the printed card, so a reprint hands out the same file
    // rather than rebuilding it. Example: "67c01122dc3f7d0011223344"
    private String generatedDocumentDocsId;
}
