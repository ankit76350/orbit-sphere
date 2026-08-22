package com.orbitastra.backend.models.new_new.feedback.campaign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.feedback.campaign.embedded.FeedbackAnswer;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackAnonymityMode;
import com.orbitastra.backend.models.new_new.feedback.campaign.enums.FeedbackSubmissionStatus;
import com.orbitastra.backend.models.new_new.identity.enums.PersonType;

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
 * One piece of feedback somebody gave.
 *
 * <p>**Read the next four paragraphs before writing any code against this model.** Anonymity
 * in a school system is not a field, it is a property of everything that touches the record,
 * and there are three separate ways to destroy it by accident. All three are in this codebase
 * already.
 *
 * <p>**One: the base class records the author.** Every document in this system extends
 * AuditedDocument, which has {@code createdByDocsId}. Saving an anonymous submission through
 * the ordinary auditing path writes the submitter's id into it, and the promise is broken
 * before the row is a second old — silently, by a mechanism nobody looked at, in a field
 * nothing on screen displays. For ANONYMOUS submissions {@code createdByDocsId} and
 * {@code updatedByDocsId} **must** be written as the fixed sentinel {@code "ANONYMOUS"}, never
 * left to the auditing interceptor. This is the single most important rule in the package.
 *
 * <p>**Two: the audit trail records the write.** An AuditEvent saying "user 4471 created
 * feedback_submission 8812 at 10:03" deanonymises the submission completely, no matter what
 * this document contains. Anonymous feedback writes must be audited **without an actor and
 * without this document's id** — the school can know that a submission happened, or who was
 * logged in, but never both together.
 *
 * <p>**Three: the duplicate check can be brute-forced.** {@code submitterFingerprint} exists so
 * one student cannot submit forty times, and it is a hash rather than an id so nobody can read
 * it back. But a school has five hundred students: hashing every one of their ids against this
 * campaign takes a laptop a fraction of a second. **The fingerprint is only anonymous if the
 * hash includes a secret the database does not contain** — a key held in application config or
 * a key store, never in a collection, never in a backup that travels with the data. Without
 * that, this field is a name in a thin disguise.
 *
 * <p>Which fields are filled depends entirely on {@code anonymityMode}:
 *
 * <pre>
 * ANONYMOUS      submitterUserAccountDocsId = null
 *                encryptedSubmitterReference = null
 *                submitterFingerprint = salted hash   (duplicate check only)
 *                createdByDocsId = "ANONYMOUS"
 *                -> nobody can ever be identified. No follow-up question possible.
 *
 * CONFIDENTIAL   submitterUserAccountDocsId = null
 *                encryptedSubmitterReference = encrypted id
 *                createdByDocsId = "CONFIDENTIAL"
 *                -> one narrow role can reveal it, and every reveal is audited.
 *
 * IDENTIFIED     submitterUserAccountDocsId = the real id
 *                -> shown to whoever the topic's visibility allows.
 * </pre>
 *
 * <p>{@code submitterType} and {@code submitterClassDocsId} are kept because feedback cannot be
 * read without them — "students say one thing and parents another" is the finding, and it needs
 * both. But they are **quasi-identifiers**, and that has a consequence the aggregate has to
 * honour: thirty responses in total with three of them from Class VI-A means the per-class
 * breakdown must be suppressed even though the overall count passes the threshold. The
 * threshold applies to every group it is broken down by, not just to the total.
 *
 * <p>{@code subjectDocsId} carries no type beside it. The topic already declares what this
 * kind of feedback is about, so a copy here would be a second field able to disagree with the
 * first — the mistake StockMovement avoids by deriving direction from movementType. It is a
 * different case from FeeInvoice.sourceType, where nothing else on the row knows the type; a
 * submission always has its topic. The trade is that a topic's subjectType becomes immutable
 * once submissions exist, which it should have been anyway.
 *
 * <p>{@code feedbackCampaignDocsId} is null for unsolicited feedback. A parent ringing about
 * the bus driver is not answering a survey, and inventing a campaign for every complaint would
 * be nonsense.
 *
 * <p>ESCALATED is the status that says this left the module. Some of what arrives here is not
 * feedback at all — it is an allegation that a child was hurt. This package cannot handle that
 * and must not pretend to; see the README for why an anonymous accusation is the wrong
 * foundation for a disciplinary process. All this model does is record that it went somewhere
 * else and who took it.
 *
 * <p>WITHDRAWN is unreachable for ANONYMOUS submissions, because nobody can prove a submission
 * was theirs. That is a real cost of true anonymity and belongs in whatever the submitter is
 * shown before they choose.
 *
 * <p>The service checks the field pattern above for each mode and rejects any submission that
 * carries more identity than its mode allows, that the mode is one the topic permits, that an
 * anonymous submission about a STUDENT subject is refused unless the topic allows it, that
 * ratings fall inside the topic's scale, that ACTIONED and DISMISSED both carry an outcome
 * note, and that no query path ever returns a submitter reference to a caller holding only the
 * subject's own permission.
 */
@Document(collection = "feedback_submissions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_feedback_submission_ref_uniq",
                def = "{'schoolId': 1, 'referenceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_feedback_no_duplicate_uniq",
                def = "{'schoolId': 1, 'feedbackCampaignDocsId': 1, 'subjectDocsId': 1, 'submitterFingerprint': 1}",
                unique = true,
                partialFilter = "{'submitterFingerprint': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_feedback_subject_idx",
                def = "{'schoolId': 1, 'subjectDocsId': 1, 'submittedAt': -1}",
                partialFilter = "{'subjectDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_feedback_campaign_idx",
                def = "{'schoolId': 1, 'feedbackCampaignDocsId': 1, 'subjectDocsId': 1}"),
        @CompoundIndex(
                name = "school_feedback_topic_status_idx",
                def = "{'schoolId': 1, 'feedbackTopicDocsId': 1, 'status': 1, 'submittedAt': -1}"),
        @CompoundIndex(
                name = "school_feedback_attention_idx",
                def = "{'schoolId': 1, 'status': 1, 'submittedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmission extends SchoolBase {

    // School-scoped number from NumberSequence type FEEDBACK_SUBMISSION. Given even to
    // anonymous feedback, so somebody can be told "FB/2026/000412 was acted on" without
    // anybody knowing whose it was. Example: "FB/2026/000412"
    @NotBlank
    private String referenceNo;

    // Links to FeedbackTopic.id, which holds the rules this submission was taken under.
    // Example: "67be1122dc3f7d0011223344"
    @NotBlank
    private String feedbackTopicDocsId;

    // Links to FeedbackCampaign.id. Null for feedback nobody asked for.
    // Example: "67be1123dc3f7d0022334455"
    private String feedbackCampaignDocsId;

    // Links to the record the topic's subjectType names. Null when the topic is about
    // SCHOOL, which is the place rather than anybody in it.
    //
    // There is no subjectType field beside this. The topic already says what this kind of
    // feedback is about, and a second copy here could disagree with it — the same reason
    // StockMovement has no direction field beside its movementType.
    // Example: "67aa15d9dc3f7d0066666666"
    private String subjectDocsId;

    // The subject's name copied in at submission time. This one IS a snapshot rather
    // than a duplicate: a member of staff who leaves is archived, and without it an old
    // submission reads as being about nobody. Example: "Mrs A. Sharma"
    private String subjectNameSnapshot;

    // What the school promised the person who wrote this. Decides which of the fields
    // below may be filled. Example: FeedbackAnonymityMode.ANONYMOUS
    @NotNull
    private FeedbackAnonymityMode anonymityMode;

    // Whether a student, a parent or a colleague said it. Needed to read the results at
    // all, and a quasi-identifier: see the class comment.
    // Example: PersonType.STUDENT
    @NotNull
    private PersonType submitterType;

    // Links to SchoolClass.id the submitter belongs to, or whose child does. Kept
    // because "Class VI rates her lower than Class IX" is the useful finding. Also a
    // quasi-identifier, so a per-class breakdown needs the threshold applied per class.
    // Example: "67ab3322dc3f7d0044556677"
    private String submitterClassDocsId;

    // Links to UserAccount.id. **Filled in only for IDENTIFIED.** Must be null for the
    // other two modes, and a service that sets it anyway has broken the promise the
    // submitter was shown. Example: "67af1122dc3f7d0011223344"
    private String submitterUserAccountDocsId;

    // The submitter's id, encrypted. **Filled in only for CONFIDENTIAL.** Revealing it
    // is one narrow role's permission and every reveal is written to AuditEvent; there
    // is no unmask log here because duplicating the audit trail would give two records
    // that can disagree. Example: "enc:v1:4c3b2a1908f7e6d5"
    private String encryptedSubmitterReference;

    // A salted one-way hash of the submitter, so the same person cannot submit twice
    // about the same subject in the same campaign. Set only when the topic disallows
    // repeats, which is what lets the unique index above be a partial one.
    //
    // **The salt must be a secret the database does not hold.** Five hundred student ids
    // hashed against one campaign is a fraction of a second's work otherwise, and this
    // field becomes a name in a thin disguise.
    // Example: "9f2c41a7be05d38c17e6a94b2f0d7c53"
    private String submitterFingerprint;

    // The answers to the topic's questions.
    @Valid
    @Builder.Default
    private List<FeedbackAnswer> answers = new ArrayList<>();

    // One overall score, when the topic asks for one. Kept beside the per-question
    // answers because it is the number comparable across topics that ask different
    // things. Example: 4
    private Integer overallRating;

    // What the person actually wrote, which is usually the part worth reading.
    // Example: "She explains well but goes too fast when we are near the exams."
    private String comment;

    // When it was given. The only timestamp on an anonymous submission, and precise
    // enough to identify somebody if a single person was logged in at that minute — so
    // a reviewer's screen should show the date, not the second.
    // Example: 2026-12-04T07:12:00Z
    @NotNull
    private Instant submittedAt;

    // What has happened to it since. Example: FeedbackSubmissionStatus.ACTIONED
    @NotNull
    @Builder.Default
    private FeedbackSubmissionStatus status = FeedbackSubmissionStatus.SUBMITTED;

    // Links to Staff.id of whoever looked at it. Example: "67aa15d9dc3f7d0055555555"
    private String reviewedByStaffDocsId;

    // When they did. Example: 2026-12-18T05:30:00Z
    private Instant reviewedAt;

    // What was done, or why nothing was done. Required for both ACTIONED and DISMISSED,
    // because feedback that can be closed with no reason gets closed with no reason —
    // and then a school can say it reviewed everything while having acted on none of it.
    // Example: "Spoke to her about pace before the January tests. She agreed to recap."
    private String outcomeNote;

    // Links to Staff.id of whoever decided. Example: "67aa15d9dc3f7d0055555555"
    private String decidedByStaffDocsId;

    // When the decision was made. Example: 2026-12-19T06:00:00Z
    private Instant decidedAt;

    // Links to Staff.id of whoever took this out of the module. Required for ESCALATED.
    // Example: "67aa15d9dc3f7d0077777777"
    private String escalatedToStaffDocsId;

    // When it was handed over. Example: 2026-12-04T09:00:00Z
    private Instant escalatedAt;

    // Where it went and why, in words. What this record keeps instead of the process
    // itself, which is deliberately not built here.
    // Example: "Alleges a child was struck. Given to the Principal in person, same day."
    private String escalationNote;

    // When the submitter took it back. Never set for ANONYMOUS, because nobody can prove
    // a submission was theirs. Example: 2026-12-05T04:00:00Z
    private Instant withdrawnAt;

    // Links to DocumentRecord.id for anything attached: a photograph of a broken desk, a
    // screenshot. Example: ["67be1124dc3f7d0033445566"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();
}
