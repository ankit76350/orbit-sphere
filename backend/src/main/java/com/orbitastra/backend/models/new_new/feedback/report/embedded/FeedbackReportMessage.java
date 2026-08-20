package com.orbitastra.backend.models.new_new.feedback.report.embedded;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.orbitastra.backend.models.new_new.feedback.report.enums.ReportMessageAuthorSide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One message in the conversation about a report.
 *
 * <p>**This is what stops an anonymous report being a black hole.** Half of what arrives in a
 * reporting channel cannot be acted on as written: "a teacher was shouting at a child in the
 * corridor" needs somebody to ask which corridor, which day, roughly what time. Without a way
 * to ask, the school either guesses or files it, and the reporter learns that speaking up
 * achieves nothing.
 *
 * <p>It works for an anonymous reporter because they hold an access code rather than a login.
 * They come back to the same page, put the code in, and see the question waiting — the school
 * still has no idea who they are. That combination is the whole reason the access code exists.
 *
 * <p>Embedded rather than a collection: a report has a handful of exchanges, they are always
 * read with the report, and there is no query that wants messages without it.
 *
 * <p>{@code authorSide} is an enum rather than "staff id is null means the reporter", because
 * an anonymous reporter has no id and the null would then mean two different things.
 *
 * <p>{@code visibleToReporter} exists so the recipient can write a note to themselves or to a
 * colleague on the same thread. A note the reporter must not see and a question meant for them
 * are both text on the same report, and one field is what keeps them apart. Getting this wrong
 * leaks an internal opinion to the person it is about.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReportMessage {

    // Order in the conversation. Example: 1
    @NotNull
    private Integer messageNo;

    // Which side wrote it. Example: ReportMessageAuthorSide.SCHOOL
    @NotNull
    private ReportMessageAuthorSide authorSide;

    // Links to Staff.id of whoever wrote it. Always set for SCHOOL, always null for
    // REPORTER — even an identified reporter, because the report already says who they
    // are and repeating it here would be a second copy that could disagree.
    // Example: "67aa15d9dc3f7d0055555555"
    private String authorStaffDocsId;

    // What was written. Example: "Which corridor was this, and roughly what time?"
    @NotBlank
    private String message;

    // When. Example: 2026-12-05T05:20:00Z
    @NotNull
    private Instant sentAt;

    // Whether the reporter may read this. False for an internal note the recipient wrote
    // to themselves or to a colleague. Always true for anything from the REPORTER side.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean visibleToReporter = true;

    // Links to DocumentRecord.id for anything attached to this message, such as a
    // photograph the reporter was asked for. Example: ["67be1125dc3f7d0044556677"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();
}
