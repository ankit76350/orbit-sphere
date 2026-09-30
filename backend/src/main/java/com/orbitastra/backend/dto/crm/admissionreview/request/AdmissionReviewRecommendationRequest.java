package com.orbitastra.backend.dto.crm.admissionreview.request;

import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the reviewer concludes. Endpoint #27e.
 *
 * <h2>Why the verdict has an endpoint of its own — 2026-09-30</h2>
 *
 * <p><b>It used to be a field on #27</b>, the general edit, alongside the score and the notes. It
 * is not the same kind of thing as those. A score is a measurement and a note is a remark; the
 * recommendation is <b>the one thing a review exists to produce</b>, and the module's own rule is
 * that something that happens gets a verb rather than a field being set quietly inside a PATCH.
 *
 * <p><b>#27 now records only what was FOUND</b> — {@code score}, {@code criterionScores},
 * {@code notes} — and every conclusion and every status move is its own call.
 *
 * <p><b>{@code @NotNull}, unlike the reason on #27d.</b> There is nothing else this endpoint could
 * mean: a body without a recommendation is not a partial recommendation, it is a caller who has
 * not said anything. A cancellation's reason can fall back to notes the review already carries;
 * a verdict has no such fallback and must not be guessed at.
 *
 * <p><b>It does not finish the review.</b> Recording what you think and declaring yourself done
 * are two decisions, and a reviewer often makes the first one days before the second. #27c is what
 * stamps {@code completedAt}, and it accepts a recommendation of its own for the reviewer who
 * makes both decisions at once.
 *
 * <p><b>And it can be changed until the review ends.</b> A verdict recorded on a
 * {@code PENDING} or {@code IN_PROGRESS} review is still the reviewer's working answer; once the
 * review is {@code COMPLETED} or {@code CANCELLED} it is a record, and this refuses it with the
 * codes #27 already uses.
 */
public record AdmissionReviewRecommendationRequest(

        /**
         * APPROVE, REJECT, WAITLIST or REQUEST_MORE_INFORMATION. Example: APPROVE
         *
         * <p>Recording a second one replaces the first — until the review is finished, this is the
         * reviewer changing their mind, which is allowed and is not worth its own history.
         */
        @NotNull AdmissionRecommendation recommendation,

        /**
         * Why. Example: "Strong in the interaction, comfortably above the mark on the paper."
         *
         * <p><b>It moved here from #27 on 2026-09-30.</b> What a reviewer writes is their
         * reasoning, and reasoning belongs beside the verdict it justifies rather than beside the
         * marks it is drawn from.
         *
         * <p><b>Optional, and {@code ""} clears it</b> — the project's convention for a String,
         * which works here because a note has an empty form. Absent leaves whatever is there, so
         * changing a verdict without retyping the reasoning keeps it.
         *
         * <p><b>One field, and #27d writes it too</b>, as the reason a review was called off. That
         * is a document with one {@code notes} and two things worth saying in it; a
         * {@code cancellationReason} of its own is what would separate them.
         */
        @Size(max = 2000) String notes,

        /**
         * The version the verdict was decided against. Optional, and honoured when sent.
         *
         * <p>Sent → a review somebody else recorded on in the meantime answers
         * {@code 409 CONCURRENT_MODIFICATION}. Absent → last write wins.
         */
        Long version) {
}
