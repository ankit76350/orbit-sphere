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
import com.orbitastra.backend.models.new_new.feedback.embedded.FeedbackReportMessage;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackAnonymityMode;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackReportCategory;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackReportStatus;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackSubjectType;
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
 * One thing somebody chose to tell the head, in their own words.
 *
 * <p>A student, a parent or a member of staff opens the page at any hour, with nobody having
 * asked them to, writes what happened, and sends it straight to the principal. That is this
 * model. It is not FeedbackSubmission with different fields — the two are different acts:
 *
 * <pre>
 * FeedbackSubmission          FeedbackReport
 * -----------------------     -----------------------
 * the school asks             the person decides
 * fixed questions             a subject line and their own words
 * one declared subject type   anything at all
 * becomes a score             expects an answer
 * one per campaign            as many as somebody needs to send
 * </pre>
 *
 * <p>Everything the anonymity design says about FeedbackSubmission applies here **without
 * exception** — the createdByDocsId sentinel, auditing the write without an actor, the salted
 * fingerprint. Read that class comment. What follows is only what is different here.
 *
 * <p>**{@code accessCodeHash} is what makes anonymous reporting worth having.** A truly
 * anonymous reporter has no login to come back to, so without a code they can never learn
 * whether anything happened. That makes the channel a black hole, and a black hole gets used
 * once. The reporter is shown a code at submission — printed once, never recoverable — and
 * returns with it to read the status, answer a question, or add something they forgot. Only the
 * hash is stored, so a person reading the database cannot use the codes to impersonate
 * reporters.
 *
 * <p>Half of what arrives in a channel like this cannot be acted on as written: "a teacher was
 * shouting at a child in the corridor" needs somebody to ask which corridor and which day.
 * {@code messages} is that conversation, and it works while the reporter stays anonymous. See
 * FeedbackReportMessage.
 *
 * <p>{@code aboutSubjectType} **is** stored here beside {@code aboutSubjectDocsId}, and that is
 * not a contradiction of dropping it from FeedbackSubmission. The test is whether anything else
 * on the row already knows: a submission always has its topic, and the topic declares the type.
 * A report is about anything the reporter chose, and there is no configuration that knows what.
 * This is the FeeInvoice.sourceType case, where the type genuinely has to be stored beside the
 * id.
 *
 * <p>Both are optional, because "about anything" includes things with no record in the system —
 * a broken railing on the stairs, a rumour, a policy the reporter thinks is unfair.
 *
 * <p>{@code acknowledgementDueBy} carries the promise from the channel. A report that was never
 * acknowledged is then a measurable failure with a date on it rather than a matter of opinion,
 * and "how many reports did we leave unanswered last term" becomes a question with an answer.
 *
 * <p>{@code routedToStaffDocsId} is resolved at submission and stored, not read through the
 * channel. A report has to keep showing who it was actually sent to after the principal
 * changes, and — more importantly — a report **about** the recipient is deliberately routed
 * elsewhere. Reading the recipient live through the channel would send it to the person it is
 * about.
 *
 * <p>{@code requiresImmediateAttention} is one clear question — is somebody in danger now? — in
 * place of a severity scale a frightened reporter cannot calibrate. It does one thing: this
 * jumps the queue.
 *
 * <p>ESCALATED means the report left this module. This package receives things properly and
 * routes them fast; it does not investigate anybody. An allegation that a child was struck
 * needs a process with an accused who can answer, and that is deliberately not built here. See
 * the README.
 *
 * <p>The service checks the anonymity field pattern for the mode, that the mode is allowed by
 * the channel, that the report is not routed to the person it is about, that the access code is
 * shown exactly once and only its hash is kept, that an internal message is never returned to a
 * reporter, that ACTIONED and DISMISSED carry an outcome, and that ESCALATED names the person
 * who took it.
 */
@Document(collection = "feedback_reports")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_feedback_report_no_uniq",
                def = "{'schoolId': 1, 'reportNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_feedback_report_inbox_idx",
                def = "{'schoolId': 1, 'routedToStaffDocsId': 1, 'status': 1, 'submittedAt': -1}"),
        @CompoundIndex(
                name = "school_feedback_report_urgent_idx",
                def = "{'schoolId': 1, 'requiresImmediateAttention': 1, 'status': 1, 'submittedAt': -1}"),
        @CompoundIndex(
                name = "school_feedback_report_overdue_idx",
                def = "{'schoolId': 1, 'status': 1, 'acknowledgementDueBy': 1}"),
        @CompoundIndex(
                name = "school_feedback_report_category_idx",
                def = "{'schoolId': 1, 'reportCategory': 1, 'submittedAt': -1}"),
        @CompoundIndex(
                name = "school_feedback_report_about_idx",
                def = "{'schoolId': 1, 'aboutSubjectDocsId': 1, 'submittedAt': -1}",
                partialFilter = "{'aboutSubjectDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReport extends SchoolBase {

    // School-scoped number from NumberSequence type FEEDBACK_REPORT. What the school
    // quotes internally. Not enough on its own for an anonymous reporter to come back
    // with, which is what the access code is for. Example: "RPT/2026/000087"
    @NotBlank
    private String reportNo;

    // A one-way hash of the code the reporter was shown once at submission. They return
    // with the code to read the status, answer a question or add something. Only the hash
    // is kept, so somebody reading the database cannot impersonate a reporter.
    //
    // Indexed without schoolId, the same as AuthSession.refreshTokenHash: a code is looked
    // up before the school is necessarily known, and it must be unique everywhere.
    // Example: "a7f3c19b4e02d85617cab93f2e0d7455"
    @Indexed(unique = true, sparse = true)
    private String accessCodeHash;

    // Links to FeedbackReportChannel.id this came in on, which is what decided where it
    // went. Example: "67be1126dc3f7d0055667788"
    @NotBlank
    private String feedbackReportChannelDocsId;

    // What kind of thing this is. Copied from the channel at submission so the report
    // still reads correctly if the school reorganises its channels.
    // Example: FeedbackReportCategory.SAFETY_CONCERN
    @NotNull
    private FeedbackReportCategory reportCategory;

    // The one-line heading the reporter wrote. What the recipient sees in a list, so it
    // is the sentence that decides whether a report is read this morning or on Friday.
    // Example: "Broken railing on the stairs near the science block"
    @NotBlank
    private String subject;

    // The whole thing, in the reporter's own words. Deliberately unstructured: a person
    // with something difficult to say should not be fighting a form to say it.
    // Example: "The railing on the second flight has come away from the wall. Two of the
    // bolts are missing. Children lean on it going down after the lunch break."
    @NotBlank
    private String description;

    // When it happened, if it was one event and the reporter knows. Null for something
    // ongoing, something with no date, or a suggestion. Example: 2026-12-04
    private LocalDate incidentDate;

    // Roughly when, in words, because "just after the lunch bell" is what a person
    // actually remembers and a time field would force them to invent a number.
    // Example: "Just after the lunch bell, around one o'clock"
    private String incidentTimeNote;

    // Where, in the reporter's words. Left as text on purpose: the useful answer is
    // "the second flight of stairs by the science block", which no room record holds.
    // Example: "Second flight of stairs, science block side"
    private String incidentLocation;

    // What the report is about, when it is about something the system knows.
    // Example: FeedbackSubjectType.FACILITY
    private FeedbackSubjectType aboutSubjectType;

    // Links to the record named by aboutSubjectType. Both this and the type are optional,
    // because "anything" includes things with no record at all.
    // Example: "67aa15d9dc3f7d0066666666"
    private String aboutSubjectDocsId;

    // The name of whatever it is about, copied in so an old report still reads after a
    // member of staff leaves or a record is archived. Example: "Mrs A. Sharma"
    private String aboutSubjectNameSnapshot;

    // What the school promised this reporter. Decides which identity fields below may be
    // filled at all. Example: FeedbackAnonymityMode.ANONYMOUS
    @NotNull
    private FeedbackAnonymityMode anonymityMode;

    // Whether a student, a parent or a colleague sent it. Optional here, unlike on a
    // campaign submission: a reporter who does not want to say should not have to, and on
    // a channel with few reports the role alone can narrow it to a handful of people.
    // Example: PersonType.GUARDIAN
    private PersonType submitterType;

    // Links to UserAccount.id. Filled in only for IDENTIFIED, and must be null for the
    // other two modes. Example: "67af1122dc3f7d0011223344"
    private String submitterUserAccountDocsId;

    // The reporter's id, encrypted. Filled in only for CONFIDENTIAL. Revealing it is its
    // own permission and every reveal is written to AuditEvent.
    // Example: "enc:v1:6b5a4938271605f4"
    private String encryptedSubmitterReference;

    // A contact the reporter chose to give so the school can reach them, even while
    // staying anonymous otherwise. A throwaway email address is a real and reasonable
    // answer. Never inferred from an account. Example: "reach.me.4471@example.com"
    private String reporterContactNote;

    // Whether somebody is in danger now. One clear question instead of a severity scale a
    // frightened reporter cannot calibrate, and it does one thing: jumps the queue.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean requiresImmediateAttention = false;

    // When it was sent. Example: 2026-12-04T09:14:00Z
    @NotNull
    private Instant submittedAt;

    // Links to Staff.id of whoever this actually went to. Resolved at submission and
    // stored, because a report about the recipient is routed to the backup instead, and
    // reading it live through the channel would send it to the person it is about.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String routedToStaffDocsId;

    // Why it went to the backup rather than the usual recipient. Set only when it did.
    // Example: "Report is about the Principal. Routed to the Chair of the Trust."
    private String routingNote;

    // The date the school promised to have acknowledged it by, carried from the channel.
    // Makes an unanswered report a measurable failure rather than an opinion.
    // Example: 2026-12-06
    private LocalDate acknowledgementDueBy;

    // Where it has got to. Example: FeedbackReportStatus.ACKNOWLEDGED
    @NotNull
    @Builder.Default
    private FeedbackReportStatus status = FeedbackReportStatus.SUBMITTED;

    // When the recipient confirmed reading it. The single most important date here: a
    // reporter who hears nothing concludes the channel does not work and never uses it
    // again. Example: 2026-12-04T11:00:00Z
    private Instant acknowledgedAt;

    // Links to Staff.id of whoever acknowledged it. Example: "67aa15d9dc3f7d0055555555"
    private String acknowledgedByStaffDocsId;

    // The conversation, including the school's questions and the reporter's answers.
    @Valid
    @Builder.Default
    private List<FeedbackReportMessage> messages = new ArrayList<>();

    // What was done, or why nothing was done. Required for ACTIONED and DISMISSED.
    // Example: "Maintenance rebolted the railing on 5 December. Checked by the estate
    // supervisor."
    private String outcomeNote;

    // Links to Staff.id of whoever decided. Example: "67aa15d9dc3f7d0055555555"
    private String decidedByStaffDocsId;

    // When. Example: 2026-12-05T12:30:00Z
    private Instant decidedAt;

    // Links to Staff.id of whoever took this out of the module. Required for ESCALATED.
    // Example: "67aa15d9dc3f7d0077777777"
    private String escalatedToStaffDocsId;

    // When it was handed over. Example: 2026-12-04T09:40:00Z
    private Instant escalatedAt;

    // Where it went and why. What this record keeps instead of a process this package
    // deliberately does not run.
    // Example: "Alleges a child was struck. Given to the Principal in person, same hour."
    private String escalationNote;

    // When the reporter took it back. Example: 2026-12-05T04:00:00Z
    private Instant withdrawnAt;

    // Links to DocumentRecord.id for photographs or documents sent with the report.
    // Example: ["67be1127dc3f7d0066778899"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();
}
