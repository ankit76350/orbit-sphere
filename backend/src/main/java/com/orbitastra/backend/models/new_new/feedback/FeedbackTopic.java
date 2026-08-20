package com.orbitastra.backend.models.new_new.feedback;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.feedback.embedded.FeedbackQuestion;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackAnonymityMode;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackSubjectType;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackVisibility;
import com.orbitastra.backend.models.new_new.identity.enums.PersonType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
 * One kind of feedback the school collects, and the rules for it.
 *
 * <p>This is the standing configuration. "Teaching quality", "Mess food", "Bus service",
 * "How the office answers the telephone" — one row each, set up once and used for years. A
 * FeedbackSubmission is the dated event, the same split as ConcessionPolicy against
 * ConcessionRequest.
 *
 * <p>Everything configurable about feedback lives here rather than being decided per
 * submission, and that is the point. Who may give this kind of feedback, about whom, under
 * which promise of anonymity, and who is allowed to read it afterwards are all school
 * policy. Deciding them per submission means they get decided differently each time, and the
 * one that gets decided wrongly is the one that matters.
 *
 * <p>{@code allowedAnonymityModes} is a list because a school may reasonably let a student
 * choose. What it must never do is offer a mode and then store more than that mode promised.
 * See FeedbackAnonymityMode: ANONYMOUS and CONFIDENTIAL are two different promises, and a
 * boolean cannot hold the difference.
 *
 * <p>{@code minimumResponsesToReveal} is the field that makes anonymity actually hold, and it
 * is the one a simpler design leaves out. An average built from three responses in a class of
 * five is not anonymous arithmetic — the teacher can work out who said what, and if two of
 * the three were kind, they know exactly who the third was. Nothing is released to the
 * subject until this many responses exist. Five is a reasonable floor; a small school may
 * have to accept that some classes never produce a releasable result, and that is the
 * honest outcome rather than a problem to configure away.
 *
 * <p>{@code allowsAnonymousAboutStudents} defaults to false, and it is the one field here
 * that is a judgement rather than a mechanism. Feedback about a member of staff is a person
 * with a contract and a union and thirty years of adult life being criticised. Feedback
 * about a student is a child being accused by somebody they cannot see, with no way to
 * answer. A school may still want it — peer feedback on group work is real and useful — but
 * it should have to switch it on deliberately, and read this comment first.
 *
 * <p>{@code questions} sit here rather than on the campaign so that every term asks the same
 * thing and December can be compared with December. See FeedbackQuestion.
 *
 * <p>{@code subjectType} is recorded here and nowhere else. Neither FeedbackSubmission nor
 * FeedbackAggregate keeps a copy, because a second copy is a field that can disagree with this
 * one and nothing would say which was right. The cost is that this field must never change
 * once submissions exist.
 *
 * <p>The service checks that a submission's anonymity mode is one of the allowed ones, that
 * question codes are unique inside the topic and never renamed once submissions exist, that
 * a RATING question's answers fall inside the scale, and that
 * {@code allowsAnonymousAboutStudents} is respected before any anonymous submission about a
 * STUDENT subject is accepted, and that {@code subjectType} is never edited once any
 * submission points at this topic.
 */
@Document(collection = "feedback_topics")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_feedback_topic_code_uniq",
                def = "{'schoolId': 1, 'topicCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_feedback_topic_subject_idx",
                def = "{'schoolId': 1, 'subjectType': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_feedback_topic_submitter_idx",
                def = "{'schoolId': 1, 'allowedSubmitterTypes': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackTopic extends SchoolBase {

    // The school's own key for this kind of feedback. Never renamed once submissions
    // exist. Example: "TEACHING_QUALITY"
    @NotBlank
    private String topicCode;

    // What staff and submitters see on screen. Example: "Teaching quality"
    @NotBlank
    private String name;

    // What this is for, in words, so somebody setting up next year's drive knows what
    // it was meant to measure.
    // Example: "Students rating the teachers who take their own classes, each term."
    private String description;

    // What this kind of feedback is about. The only place this is recorded: submissions
    // and aggregates read it through here rather than keeping copies that could disagree.
    //
    // That makes it immutable once submissions exist. Changing a topic from STAFF to
    // STUDENT would silently rewrite what every submission under it was ever about.
    // Example: FeedbackSubjectType.STAFF
    @NotNull
    private FeedbackSubjectType subjectType;

    // Who may give it. At least one, because a topic nobody may submit to is not a
    // topic. Example: [PersonType.STUDENT, PersonType.GUARDIAN]
    @NotEmpty
    @Builder.Default
    private List<PersonType> allowedSubmitterTypes = new ArrayList<>();

    // Which promises the school is willing to make for this topic. At least one.
    // Example: [FeedbackAnonymityMode.ANONYMOUS]
    @NotEmpty
    @Builder.Default
    private List<FeedbackAnonymityMode> allowedAnonymityModes = new ArrayList<>();

    // Which one is offered first. Must be one of the allowed modes.
    // Example: FeedbackAnonymityMode.ANONYMOUS
    @NotNull
    private FeedbackAnonymityMode defaultAnonymityMode;

    // Who may read what comes in. Example: FeedbackVisibility.SUBJECT_AGGREGATE
    @NotNull
    @Builder.Default
    private FeedbackVisibility visibility = FeedbackVisibility.REVIEWER_ONLY;

    // How many responses must exist before anything is shown to the subject. The field
    // that makes anonymity hold rather than merely be promised. Example: 5
    @NotNull
    @Min(1)
    @Builder.Default
    private Integer minimumResponsesToReveal = 5;

    // Whether anonymous feedback may be given about a child. Off unless somebody turns
    // it on deliberately; read the class comment before doing so. Example: false
    @NotNull
    @Builder.Default
    private Boolean allowsAnonymousAboutStudents = false;

    // Whether somebody may submit this out of the blue, with no campaign open. True for
    // "the bus driver was rude this morning"; false for a term-end drive that should
    // only run when the school opens it. Example: true
    @NotNull
    @Builder.Default
    private Boolean allowsUnsolicited = true;

    // Whether one person may submit more than once about the same subject in the same
    // campaign. False for a rating drive, true for an open suggestion box. Example: false
    @NotNull
    @Builder.Default
    private Boolean allowsRepeatSubmission = false;

    // The top of the rating scale. Five and ten are the usual choices; changing it once
    // submissions exist makes old averages incomparable, so it is fixed in practice.
    // Example: 5
    @NotNull
    @Min(2)
    @Builder.Default
    private Integer ratingScaleMax = 5;

    // What is asked. Empty is allowed for a topic that only wants a comment, such as a
    // suggestion box.
    @Valid
    @Builder.Default
    private List<FeedbackQuestion> questions = new ArrayList<>();

    // Whether a single overall rating is asked for alongside the questions. Useful
    // because it gives one comparable number across topics that ask different things.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean asksOverallRating = true;

    // Whether the free-text comment must be filled in. Forcing a comment on a rating
    // drive produces thirty rows of "good", so this is normally false. Example: false
    @NotNull
    @Builder.Default
    private Boolean requiresComment = false;

    // Links to Staff.id of whoever handles what comes in under this topic. The person
    // an escalation goes to and the person the reviewing permission is checked against.
    // Example: "67aa15d9dc3f7d0044444444"
    private String coordinatorStaffDocsId;

    // Whether this topic may still be used for a new campaign or submission. Turning it
    // off leaves everything already collected alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
