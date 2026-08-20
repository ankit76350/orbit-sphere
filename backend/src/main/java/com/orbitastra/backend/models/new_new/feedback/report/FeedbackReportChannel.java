package com.orbitastra.backend.models.new_new.feedback.report;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.feedback.enums.FeedbackAnonymityMode;
import com.orbitastra.backend.models.new_new.feedback.report.enums.FeedbackReportCategory;
import com.orbitastra.backend.models.new_new.identity.enums.PersonType;

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
 * Where reports of one category go, and who is allowed to read them.
 *
 * <p>The reporting channel is always open and anybody may say anything, but "it goes to the
 * principal" has to be written down somewhere or it is a hardcoded id in a service. This is
 * where. One row per category the school accepts, saying who receives it, how quickly they
 * promised to acknowledge it, and which anonymity modes are on offer.
 *
 * <p>It is the standing configuration; a FeedbackReport is the dated event. Same split as
 * FeedbackTopic against FeedbackSubmission, and ConcessionPolicy against ConcessionRequest.
 *
 * <p>{@code backupRecipientStaffDocsId} is the field that looks like belt-and-braces and is
 * not. **A report about the principal must not be delivered to the principal.** That is not a
 * rare edge case: a channel that promises to hear anything will eventually be used to report
 * the person who runs the school, and if that report lands in their inbox the reporter is worse
 * off than if the channel had never existed. The service must route to the backup whenever the
 * subject of the report is the recipient, and there has to be somewhere for it to go.
 *
 * <p>{@code acknowledgementDays} is a promise, not a preference. It exists so that "we never
 * replied" is a measurable failure with a date attached rather than a matter of opinion. A
 * channel with no clock on it fills up with reports nobody answered and nobody can prove
 * nobody answered.
 *
 * <p>{@code additionalReaderStaffDocsIds} is for the committees Indian schools commonly run —
 * a grievance committee, an internal complaints committee. It is a list of named people rather
 * than a role, because "whoever holds this role" changes silently and a report the reporter
 * believed three people could see must not quietly become readable by a fourth.
 *
 * <p>{@code allowedAnonymityModes} deserves thought per category rather than one setting for
 * the school. HARASSMENT_OR_BULLYING must allow ANONYMOUS or the people who most need it will
 * not use it. A SUGGESTION about the canteen probably should not be anonymous, because there
 * is nothing to fear and an anonymous suggestion cannot be discussed with whoever made it.
 *
 * <p>The service checks that the recipient and the backup are two different people, that a
 * report whose subject is the recipient is routed to the backup, that the requested anonymity
 * mode is allowed for the category, and that no channel is deactivated while reports on it are
 * still open.
 */
@Document(collection = "feedback_report_channels")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_report_channel_category_uniq",
                def = "{'schoolId': 1, 'reportCategory': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_report_channel_recipient_idx",
                def = "{'schoolId': 1, 'recipientStaffDocsId': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_report_channel_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'reportCategory': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReportChannel extends SchoolBase {

    // Which category of report this row governs. One channel per category.
    // Example: FeedbackReportCategory.HARASSMENT_OR_BULLYING
    @NotNull
    private FeedbackReportCategory reportCategory;

    // What the reporter sees as the choice on the form. Worth wording carefully: this is
    // what a frightened child reads before deciding whether to type anything.
    // Example: "Somebody is being bullied or picked on"
    @NotBlank
    private String displayLabel;

    // A sentence under it, saying what happens next and what will not happen.
    // Example: "Goes straight to the Principal. Nobody else is told you sent it."
    private String displayHelpText;

    // Links to Staff.id of the person these reports go to. The principal or the
    // director, normally. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String recipientStaffDocsId;

    // Links to Staff.id of who receives it instead when the report is about the
    // recipient. Must be a different person, and must exist: a channel that promises to
    // hear anything will eventually be used to report the head.
    // Example: "67aa15d9dc3f7d0077777777"
    @NotBlank
    private String backupRecipientStaffDocsId;

    // Links to Staff.id for each additional person allowed to read these, such as a
    // grievance committee. Named people rather than a role, so the set cannot widen
    // silently after a reporter was told who could see it.
    // Example: ["67aa15d9dc3f7d0088888888"]
    @Builder.Default
    private List<String> additionalReaderStaffDocsIds = new ArrayList<>();

    // Who may send this kind of report. At least one.
    // Example: [PersonType.STUDENT, PersonType.GUARDIAN, PersonType.STAFF]
    @NotEmpty
    @Builder.Default
    private List<PersonType> allowedSubmitterTypes = new ArrayList<>();

    // Which promises the school will make for this category. At least one, and worth
    // deciding per category rather than once for the school.
    // Example: [FeedbackAnonymityMode.ANONYMOUS, FeedbackAnonymityMode.IDENTIFIED]
    @NotEmpty
    @Builder.Default
    private List<FeedbackAnonymityMode> allowedAnonymityModes = new ArrayList<>();

    // Which one the form offers first. Must be one of the allowed modes.
    // Example: FeedbackAnonymityMode.ANONYMOUS
    @NotNull
    private FeedbackAnonymityMode defaultAnonymityMode;

    // How many days the school has promised to take to acknowledge a report. Not to
    // resolve it — to say it has been read. Example: 2
    @NotNull
    @Min(1)
    @Builder.Default
    private Integer acknowledgementDays = 3;

    // Whether a report in this category jumps the queue and is flagged the moment it
    // arrives. True for harassment and safety; false for a canteen suggestion.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean urgentByDefault = false;

    // Whether a reporter may attach photographs or documents. Off for categories where
    // an attachment is more likely to identify the reporter than to help.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean allowsAttachments = true;

    // Whether the school may ask the reporter follow-up questions on this channel. On
    // normally, because most reports cannot be acted on as first written.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean allowsFollowUpConversation = true;

    // Whether reports in this category may still be sent. Turning it off leaves every
    // report already made alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
