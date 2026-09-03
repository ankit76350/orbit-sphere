package com.orbitastra.backend.dto.plans.catalogue;

import jakarta.validation.constraints.NotNull;

/**
 * Whether a plan shows on the public list. Endpoint #7.
 *
 * <p>One field, and it is <b>required</b>. A {@code PATCH} whose only field is optional would
 * accept {@code &#123;&#125;} and have nothing to do, so there is no partial case to design here:
 * either the caller is setting the flag or they are not calling this.
 *
 * <p><b>Boxed, not a primitive.</b> An omitted {@code boolean} arrives as {@code false}, which is
 * indistinguishable from somebody deliberately hiding the plan — so a forgotten field would
 * quietly pull a plan off the pricing page and report success.
 */
public record PlanAvailabilityRequest(

        /**
         * True to list it publicly, false to offer it only in a private quote. Example: true
         *
         * <p>This is the last thing standing between a published plan and a school being able to
         * buy it. On its own it grants nothing: a plan also has to be {@code ACTIVE} and inside
         * its selling window.
         */
        @NotNull Boolean publiclyAvailable) {
}
