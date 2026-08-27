package com.orbitastra.backend.dto.core;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * What activating a school did.
 *
 * <p>{@code firstActivation} exists because {@code activatedAt} is stamped once and never
 * rewritten — a school suspended and brought back keeps its original go-live date. So the flag
 * says whether this call set it, which is the difference between "the school went live today"
 * and "the school went live in April and we just re-activated it".
 *
 * <p>{@code subscriptionStatus} is reported rather than merely checked, because activation is
 * the moment it matters and the answer is currently allowed to be "there isn't one" — see
 * {@code subscriptionNote}.
 */
public record SchoolActivateResponse(
        String schoolId,
        String subdomain,
        SchoolStatus status,
        Instant activatedAt,
        boolean firstActivation,
        String subscriptionStatus,
        String subscriptionNote,
        String nextStep) {

    public static SchoolActivateResponse fromActivateResponse(
            School school,
            boolean firstActivation,
            String subscriptionStatus,
            String subscriptionNote) {

        return new SchoolActivateResponse(
                school.getId(),
                school.getSubdomain(),
                school.getStatus(),
                school.getActivatedAt(),
                firstActivation,
                subscriptionStatus,
                subscriptionNote,
                "The school is live. Create the first administrator account if you have not "
                        + "already.");
    }
}
