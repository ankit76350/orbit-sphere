package com.orbitastra.backend.dto.core.platform;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * What a lifecycle change did. Shared by suspend and reactivate.
 *
 * <p>One record for both rather than two identical ones. They answer the same question — what
 * is the status now, when did the last suspension happen, and why — and two records with the
 * same fields would drift apart the first time somebody added something to one of them.
 *
 * <p>Both dates are returned by both endpoints on purpose:
 *
 * <ul>
 * <li>{@code activatedAt} is never touched here. A school suspended in June and brought back in
 * July keeps its original go-live date, so this still reads as the day it first went live.</li>
 * <li>{@code suspendedAt} is the <b>most recent</b> suspension and is <b>not cleared</b> on
 * reactivation. After coming back, a school reads as ACTIVE with a suspendedAt in the past,
 * which is exactly the history somebody needs when the same school is suspended again.</li>
 * </ul>
 *
 * <p>{@code statusReason} survives reactivation for the same reason.
 */
public record SchoolStatusResponse(
        String schoolId,
        String subdomain,
        SchoolStatus status,
        Instant activatedAt,
        Instant suspendedAt,
        String statusReason,
        String nextStep) {

    public static SchoolStatusResponse fromSchool(School school, String nextStep) {
        return new SchoolStatusResponse(
                school.getId(),
                school.getSubdomain(),
                school.getStatus(),
                school.getActivatedAt(),
                school.getSuspendedAt(),
                school.getStatusReason(),
                nextStep);
    }
}
