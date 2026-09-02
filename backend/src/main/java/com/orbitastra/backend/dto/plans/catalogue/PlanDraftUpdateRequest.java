package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;

import jakarta.validation.constraints.Size;

/**
 * Fixes the details of a plan that is still a draft. Endpoint #2.
 *
 * <p><b>Only a draft can be edited.</b> Once a plan is published a school can be on it, and
 * changing the price of something somebody already bought would change what they agreed to pay
 * without anybody agreeing to it. Editing a published plan is refused; #5 copies it into a new
 * draft version instead.
 *
 * <p><b>How PATCH behaves here</b>, because absent and null look the same to Jackson:
 *
 * <pre>
 * field omitted, or null   -> leave it exactly as it is
 * field is ""              -> clear it (description only)
 * field has a value        -> replace it
 * </pre>
 *
 * <p>{@code name} cannot be cleared. It is {@code @NotBlank} on the model, and a plan with no
 * name is not a state worth supporting, so {@code ""} there is a 400 rather than a deletion.
 *
 * <h2>What is deliberately absent</h2>
 *
 * <p>{@code planCode} and {@code planVersion} are the plan's identity — they are in the URL, and
 * a PATCH that could change the thing it is addressing would name one plan and mean another.
 * {@code status} is not here either: publishing is #4, which checks the plan is complete first,
 * and retiring is #6. {@code publiclyAvailable} is #7, and {@code features} is #3.
 *
 * <p>Every one of those is a decision with its own rules. A PATCH that could set them all would
 * make "put this on sale" look identical to "fix a typo".
 */
public record PlanDraftUpdateRequest(

        /** Example: "Premium Plus". Cannot be cleared. */
        @Size(max = 120) String name,

        /** Example: "Now with AI reports." Send "" to remove it. */
        @Size(max = 500) String description,

        /** Example: BillingCycle.MONTHLY */
        BillingCycle billingCycle,

        /** Example: 44999.00. Zero is allowed; negative is not. */
        BigDecimal listPrice,

        /** ISO 4217. Example: "INR" */
        @Size(max = 3) String currencyCode,

        /** Example: 3000 */
        Long maxStudents,

        /** Example: 300 */
        Long maxUsers,

        /**
         * When the plan may be sold, replaced as a pair.
         *
         * <p><b>Both dates together or neither</b>, which is why they are nested rather than two
         * loose fields. They are only meaningful next to each other — an {@code effectiveUntil}
         * moved earlier than the existing {@code effectiveFrom} is a plan that can never be sold
         * — so a PATCH that changed one of them alone could create a window nobody asked for.
         * The same reasoning puts the school's address behind a PUT.
         *
         * <p>Omit it to leave the window untouched. Send it to replace both, and send
         * {@code null} inside it to clear one — which is the only way to clear a date here,
         * since {@code ""} cannot mean anything to an instant.
         */
        SellingWindow sellingWindow) {

    /** The pair. A null field inside means "no date", not "leave it alone". */
    public record SellingWindow(Instant effectiveFrom, Instant effectiveUntil) {
    }

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return name == null && description == null && billingCycle == null && listPrice == null
                && currencyCode == null && maxStudents == null && maxUsers == null
                && sellingWindow == null;
    }
}
