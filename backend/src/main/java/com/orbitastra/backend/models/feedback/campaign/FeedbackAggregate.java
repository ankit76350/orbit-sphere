package com.orbitastra.backend.models.feedback.campaign;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.feedback.campaign.embedded.FeedbackQuestionAggregate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The numbers for one subject in one campaign.
 *
 * <p>Everywhere else in this system I have argued that a report is a report and not a model,
 * and that a total which can be derived should be derived. This one is a collection anyway,
 * and the reason is not performance.
 *
 * <p>**It exists so that showing a teacher their results never requires reading the
 * submissions.** If a teacher's screen computed the average on the fly, that request path
 * would have to open thirty anonymous submissions belonging to children they teach — and then
 * the only thing standing between the teacher and the raw comments is that the code currently
 * chooses not to return them. One bug, one debug endpoint, one hurried export feature, and the
 * anonymity is gone. Materialising the numbers means the teacher's request touches a document
 * that never contained a name in the first place.
 *
 * <p>So this is a **privacy boundary that happens to look like a cache.** It must still be
 * rebuildable from the submissions, which stay the real record.
 *
 * <p>**No comments are stored here.** Text answers contribute only their count; the words stay
 * on the submissions where the visibility rules still apply. Copying them in would move them
 * past the one thing protecting them, because this is the object that gets released.
 *
 * <p>{@code suppressed} is what the threshold produces. Four responses in a class of six is
 * not anonymous arithmetic — if two of the four were kind, the teacher knows who the other two
 * were. A suppressed aggregate is still built, still readable by the reviewer, and simply never
 * released. It is not deleted, because "we asked and too few answered" is itself worth knowing,
 * and because deleting it would mean rebuilding it to find out.
 *
 * <p>{@code releasedAt} being null is what says the subject has not seen this yet. That is the
 * whole mechanism behind FeedbackCampaign's split between CLOSED and PUBLISHED.
 *
 * <p>There is deliberately **no comparison against other staff.** No rank, no percentile, no
 * "above school average" flag. A school that ranks its teachers on student ratings has built a
 * league table, and the field that made it possible was always an innocent-looking one.
 * Whether to make that comparison is a decision for a head looking at a report, not a number
 * this model hands them by default.
 *
 * <p>The service checks that this is rebuilt from the submissions rather than incremented,
 * that {@code responseCount} below the topic's minimum forces {@code suppressed}, that the
 * threshold is applied to every breakdown and not only to the total, and that nothing is
 * released while the campaign is not PUBLISHED.
 */
@Document(collection = "feedback_aggregates")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_feedback_aggregate_uniq",
                def = "{'schoolId': 1, 'feedbackCampaignDocsId': 1, 'subjectDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_feedback_aggregate_subject_idx",
                def = "{'schoolId': 1, 'subjectDocsId': 1, 'academicYear': 1}",
                partialFilter = "{'subjectDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_feedback_aggregate_release_idx",
                def = "{'schoolId': 1, 'feedbackCampaignDocsId': 1, 'suppressed': 1, 'releasedAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAggregate extends SchoolBase {

    // Links to FeedbackCampaign.id. Example: "67be1123dc3f7d0022334455"
    @NotBlank
    private String feedbackCampaignDocsId;

    // Links to AcademicYear.name, copied from the campaign so a subject's results across
    // years read without loading every campaign. Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // Links to the record the campaign's topic names. Null for a school-level aggregate.
    // No subjectType beside it, for the same reason as on the submission: the topic
    // already says so. Example: "67aa15d9dc3f7d0066666666"
    private String subjectDocsId;

    // The subject's name copied in, so a released result reads on its own even after the
    // staff record is archived. Example: "Mrs A. Sharma"
    private String subjectNameSnapshot;

    // How many submissions went into these numbers. The figure the threshold is checked
    // against. Example: 31
    @NotNull
    @Builder.Default
    private Integer responseCount = 0;

    // The mean of the overall ratings, for the topics that ask for one. Example: 4.21
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal averageOverallRating;

    // The summary for each question, with its distribution.
    @Valid
    @Builder.Default
    private List<FeedbackQuestionAggregate> questionAggregates = new ArrayList<>();

    // How many submissions had something written in the comment box. A count, not the
    // words: those stay on the submissions. Example: 18
    @NotNull
    @Builder.Default
    private Integer commentCount = 0;

    // Whether too few people answered for this to be shown to the subject at all.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean suppressed = false;

    // Why it is held back, in words a reviewer can pass on.
    // Example: "Only 3 responses. The topic needs 5 before anything is shown."
    private String suppressionReason;

    // When the subject was allowed to see this. Null until then, and that null is the
    // whole mechanism behind a campaign being closed but not yet published.
    // Example: 2026-12-22T05:00:00Z
    private Instant releasedAt;

    // When these numbers were last worked out from the submissions. Kept because a stale
    // aggregate and a fresh one look identical otherwise, and this is a figure people
    // make decisions about somebody's work from. Example: 2026-12-16T02:00:00Z
    @NotNull
    private Instant computedAt;
}
