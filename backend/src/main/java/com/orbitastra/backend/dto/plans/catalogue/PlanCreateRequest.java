package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Makes a new plan. Endpoint #1, platform surface only.
 *
 * <p>Three fields the plan has are deliberately <b>not</b> on this request, and each is a rule
 * rather than an omission:
 *
 * <ul>
 * <li><b>{@code status}</b> — a new plan is always {@code DRAFT}. That is the whole point of the
 * endpoint: nobody can buy it while the price is still being argued about. Letting a caller send
 * {@code ACTIVE} would put an unfinished plan on sale in one request.</li>
 * <li><b>{@code planVersion}</b> — always 1. Later versions come from #5, which copies a
 * published version; a caller who could choose the number could create version 7 of a plan that
 * has no version 1.</li>
 * <li><b>{@code publiclyAvailable}</b> — always false to begin with. #7 decides whether a plan
 * shows on the public list, and that is a separate decision from whether it exists.</li>
 * </ul>
 *
 * <p><b>Features are not accepted here either.</b> A plan is created with an empty feature list
 * and #3 sets the whole list in one go — the same shape academic years use for holidays. Two
 * reasons: a create that can fail on either a bad price or a bad feature leaves the caller
 * working out which, and a partly-filled feature list is exactly the "plan nobody can price"
 * that #3 exists to prevent. Sending a {@code features} array does nothing; the field is not
 * here, so it is ignored rather than half-honoured.
 */
public record PlanCreateRequest(

        /**
         * The plan's permanent family code. <b>Optional</b> — leave it out. Example: "PREMIUM"
         *
         * <p>Derived from {@code name} when it is not sent: "Premium Plus" becomes
         * {@code PREMIUM_PLUS}. A create form should ask for the name only, rather than making
         * somebody type the same words twice in two shapes.
         *
         * <p>Send one explicitly only when the derived code will not do — the name would produce
         * a code another plan already has, or the code has to match something outside this
         * system.
         *
         * <p>Either way it is <b>fixed once the plan exists</b>, and every later version of the
         * plan carries it. It is the only thing joining the versions of one plan together, which
         * is why the editable {@code name} cannot do the job.
         */
        @Size(max = 40) String planCode,

        /** Example: "Premium" */
        @NotBlank @Size(max = 120) String name,

        /** Example: "Advanced ERP modules and AI capabilities for growing schools." */
        @Size(max = 500) String description,

        /** Example: BillingCycle.YEARLY */
        @NotNull BillingCycle billingCycle,

        /**
         * What the plan costs per billing cycle, before tax. Example: 49999.00
         *
         * <p>Zero is allowed — a free tier is a real plan. Negative is not.
         */
        @NotNull BigDecimal listPrice,

        /** ISO 4217, uppercased on the way in. Example: "INR" */
        @NotBlank @Size(max = 3) String currencyCode,

        /** Example: 2000 */
        @NotNull Long maxStudents,

        /** Example: 250 */
        @NotNull Long maxUsers,

        /**
         * When this version may start being sold. Optional. Example: 2026-04-01T00:00:00Z
         *
         * <p>Left empty here it is stamped by #4 when the plan is published, which is the more
         * honest answer: a draft has no date it goes on sale until somebody decides to sell it.
         */
        Instant effectiveFrom,

        /** When it stops being sold. Optional. Example: 2027-03-31T23:59:59Z */
        Instant effectiveUntil) {
}
