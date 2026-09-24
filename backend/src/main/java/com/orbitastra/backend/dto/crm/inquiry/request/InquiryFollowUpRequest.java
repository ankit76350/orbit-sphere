package com.orbitastra.backend.dto.crm.inquiry.request;

import java.time.Instant;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #10 carries — <b>one interaction, logged</b>.
 *
 * <p><b>This is the endpoint the lead half was waiting for.</b> #13 sorts a worklist by
 * {@code nextFollowUpAt} and #14 renders a timeline, and until this existed <i>every lead in the
 * system had an empty timeline and no chase date</i> — both reads were correct and had nothing to
 * show. This writes the only two fields either of them is really about.
 *
 * <p><b>The model's annotations and this record disagree, and the disagreement is deliberate.</b>
 * {@link com.orbitastra.backend.models.crm.embedded.InquiryFollowUp} carries {@code @NotNull} on
 * {@code status} and {@code @NotBlank} on {@code communicationChannel} and
 * {@code counselorDocsId}, and nothing on {@code note}. This record requires the <b>note</b> and
 * nothing else, which is what the plan's field table for #10 says. Two reasons to follow the plan
 * rather than the model:
 *
 * <ul>
 *   <li><b>The model annotations are not enforced.</b> There is no
 *       {@code ValidatingMongoEventListener} in this project, so they are documentation of intent
 *       — and the enforcement is {@code @Valid} on this record.</li>
 *   <li><b>They describe the wrong thing.</b> {@code status} required would mean every logged call
 *       has to claim it moved the lead, when most calls move nothing. And a lead's status moving
 *       is the <i>parent's</i> business — an entry's copy is a note about what this call did.</li>
 * </ul>
 *
 * <p><b>{@code recordedAt} is not a field here.</b> The server writes {@code now}. A caller who
 * could name the time a call happened could log one into next week, and a timeline sorted on a
 * caller-supplied instant is not a record of anything.
 *
 * <p><b>Neither is {@code lostReason}.</b> Marking a lead lost needs one, and #12 is what carries
 * it — see {@code status} below.
 */
public record InquiryFollowUpRequest(

        /**
         * What happened. <b>The one required field.</b>
         *
         * <p>A timeline entry that says nothing is a row that makes a lead look worked when
         * nobody did anything. Everything else about a call can be missing and the entry is still
         * worth having; the note cannot.
         */
        @NotBlank @Size(max = 2000) String note,

        /**
         * How they were reached — {@code "PHONE"}, {@code "WHATSAPP"}, {@code "VISIT"}.
         *
         * <p><b>Free text, and optional.</b> Nothing validates it, because a school names its own
         * channels — the same reading #8 makes about {@code source}. "Caught the father in the
         * corridor" is a real interaction with no channel worth naming.
         */
        @Size(max = 60) String communicationChannel,

        /**
         * What the lead became <b>as a result of this call</b>, when it became anything.
         *
         * <p><b>Absent means the call moved nothing</b>, which is the common case: a counsellor
         * rings, nobody answers, the lead is still {@code CONTACTED}. The entry stores exactly what
         * was sent, so a null on the timeline reads "left as it was" rather than repeating the
         * status the lead already had.
         *
         * <p><b>It walks the transition table</b>, which is the module's product rule, and three
         * destinations are refused here on top of it:
         *
         * <ul>
         *   <li>{@code LOST} needs a reason, and there is nowhere to put one on a follow-up.
         *       #12 owns it.</li>
         *   <li>{@code APPLICATION_STARTED} and {@code APPLICATION_SUBMITTED} are facts about an
         *       <i>application</i>, set by #17 and #19. Letting a counsellor type either would let
         *       the lead and the form disagree about whether a form exists.</li>
         * </ul>
         *
         * <p><b>Sending the status the lead already has is accepted and moves nothing.</b> A
         * second call about a lead that is still {@code CONTACTED} is not an illegal transition.
         */
        InquiryStatus status,

        /**
         * When the next call is due.
         *
         * <p><b>This is written to the LEAD as well as to the entry</b>, and it is what #13's
         * worklist sorts on — the reason the field exists on the parent at all.
         *
         * <p><b>Absent CLEARS the lead's chase date rather than leaving it alone</b>, which is the
         * one place #10 departs from "only what was sent". The field means <i>the next call is due
         * at</i>: once this call has been made and no new date promised, there is no next call
         * due. Leaving the old date would keep showing a family as overdue on the very day
         * somebody rang them.
         *
         * <p>The entry keeps what was promised either way, so the history is not lost.
         */
        Instant nextFollowUpAt,

        /**
         * Who logged it. <b>This school's staff, when it is sent.</b>
         *
         * <p><b>Optional, and it should not be.</b> The point of a timeline is who said what, and
         * #14 exists to name them. It is optional because <i>nothing in this project knows who is
         * asking yet</i> — so the only way to fill it is for the caller to say, and a caller who
         * genuinely does not know should still be able to record that the call happened. #14
         * renders the gap as "not recorded" rather than hiding the entry.
         *
         * <p>When authorization arrives this becomes the caller and stops being a field.
         */
        @Size(max = 60) String counselorDocsId,

        /** Optimistic check. Absent skips it. */
        Long version) {
}
