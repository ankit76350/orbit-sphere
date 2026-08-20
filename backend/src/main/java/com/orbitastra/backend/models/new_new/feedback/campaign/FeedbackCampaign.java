package com.orbitastra.backend.models.new_new.feedback;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackAnonymityMode;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackCampaignStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One drive to collect feedback, open between two dates.
 *
 * <p>"End of term teaching feedback, Classes VI to XII, open the first to the fifteenth of
 * December." The topic says what is asked and who may answer; the campaign says when, and to
 * which part of the school.
 *
 * <p>A campaign is optional. FeedbackSubmission.feedbackCampaignDocsId may be null, because a
 * parent ringing to say the bus driver was rude this morning is not answering a survey. Those
 * two things are genuinely different: one is solicited from a known audience and produces
 * comparable numbers, the other arrives when it arrives and produces one thing somebody has
 * to deal with. Forcing the second into a campaign would mean inventing a campaign for every
 * complaint.
 *
 * <p>CLOSED and PUBLISHED are two states because they are two decisions, and this is the most
 * useful thing this model does. Closing stops new submissions. Publishing releases results to
 * the people they are about. A head wants the week in between: to read what came in first, and
 * to hold back a set of results with four responses rather than release it because the
 * calendar said so. A single {@code closed} flag means the moment the last student submits,
 * every teacher can read their comments.
 *
 * <p>{@code targetClassDocsIds} names the audience by class rather than listing every student.
 * A campaign that stored its audience as a list of names would go stale the moment a child
 * joined or left, and a child who joined in November would silently not be asked.
 *
 * <p>**The subjects are not listed here.** Who each student rates is worked out at submission
 * time from the timetable, because a campaign that stored the student-teacher matrix would be
 * thousands of rows that go wrong the moment a teacher is reallocated. The campaign says which
 * students; the timetable says which teachers each of them has.
 *
 * <p>{@code expectedResponseCount} and {@code receivedResponseCount} are running figures for
 * showing progress. They must be rebuildable from the submissions, which stay the real record.
 * The expected figure is a genuine estimate rather than a count of anything, so a shortfall
 * against it is a prompt to chase, not evidence of a bug.
 *
 * <p>The service checks that the campaign's anonymity mode is allowed by its topic, that
 * submissions are only accepted while OPEN and only from the target classes, that closing
 * builds the aggregates, that publishing releases only those above the topic's minimum, and
 * that a CANCELLED campaign never releases anything.
 */
@Document(collection = "feedback_campaigns")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_feedback_campaign_code_uniq",
                def = "{'schoolId': 1, 'campaignCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_feedback_campaign_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'opensOn': -1}"),
        @CompoundIndex(
                name = "school_feedback_campaign_topic_idx",
                def = "{'schoolId': 1, 'feedbackTopicDocsId': 1, 'opensOn': -1}"),
        @CompoundIndex(
                name = "school_feedback_campaign_open_idx",
                def = "{'schoolId': 1, 'status': 1, 'closesOn': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackCampaign extends SchoolBase {

    // The school's own key for this drive. Example: "TEACHING_2026_T2"
    @NotBlank
    private String campaignCode;

    // What it is called on screen. Example: "Teaching feedback, Term 2"
    @NotBlank
    private String name;

    // Links to AcademicYear.name. Feedback drives belong to a school session, unlike a
    // purchase order. Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // Links to FeedbackTopic.id, which decides what is asked and who may read it.
    // Example: "67be1122dc3f7d0011223344"
    @NotBlank
    private String feedbackTopicDocsId;

    // Links to AcademicTerm.id when the drive belongs to a term.
    // Example: "67ab5511dc3f7d0099887766"
    private String termDocsId;

    // First day submissions are accepted. Example: 2026-12-01
    @NotNull
    private LocalDate opensOn;

    // Last day submissions are accepted. Example: 2026-12-15
    @NotNull
    private LocalDate closesOn;

    // Links to SchoolClass.id for each class being asked. Empty means the whole school.
    // Named by class rather than by student so a child who joins in November is included
    // without anybody rebuilding a list. Example: ["67ab3322dc3f7d0044556677"]
    @Builder.Default
    private List<String> targetClassDocsIds = new ArrayList<>();

    // Which promise this drive makes. Must be one the topic allows. Fixed for the
    // campaign rather than chosen per submission, because a set of results where some
    // rows are anonymous and some are not cannot be released as one thing.
    // Example: FeedbackAnonymityMode.ANONYMOUS
    @NotNull
    private FeedbackAnonymityMode anonymityMode;

    // Where the drive has got to. Example: FeedbackCampaignStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private FeedbackCampaignStatus status = FeedbackCampaignStatus.DRAFT;

    // What the school hopes to get, for showing progress. An estimate, not a count of
    // anything. Example: 1240
    private Integer expectedResponseCount;

    // How many have come in. Rebuildable from the submissions. Example: 1103
    @NotNull
    @Builder.Default
    private Integer receivedResponseCount = 0;

    // What submitters are told before they start. The place the anonymity promise is
    // actually made in words, which matters because the enum is not what anybody reads.
    // Example: "Nobody will be able to see who wrote this, including the head."
    private String instructions;

    // When submissions were stopped. Example: 2026-12-15T18:30:00Z
    private Instant closedAt;

    // When results were released to the subjects. Null while they are still held back,
    // and that null is what says nobody has seen their own results yet.
    // Example: 2026-12-22T05:00:00Z
    private Instant publishedAt;

    // Links to Staff.id of whoever released them. Example: "67aa15d9dc3f7d0055555555"
    private String publishedByStaffDocsId;

    // Why the drive was called off. Required for CANCELLED.
    // Example: "Opened against the wrong term. Rerun as TEACHING_2026_T2B."
    private String cancellationReason;

    // Anything worth knowing.
    // Example: "First term we asked Class VI as well. Response rate was low on Fridays."
    private String remarks;
}
