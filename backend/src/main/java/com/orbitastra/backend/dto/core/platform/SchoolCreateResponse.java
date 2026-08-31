package com.orbitastra.backend.dto.core.platform;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * What provisioning gives back.
 *
 * <p>It once also returned {@code numberSequencesCreated} and {@code rolesCreated}. Those went
 * when the seeding did on 2026-08-21; a count of rows this endpoint no longer writes would be a
 * response field that always said zero.
 *
 * <p>{@code nextStep} is here because **a provisioned school cannot be logged into yet.** No
 * user account exists, and no roles or number sequences either. Saying so in the response is the
 * difference between an operator moving on and an operator waiting for an invitation email that
 * nothing is going to send.
 *
 * <p>{@code encryptionKeyReference} is deliberately not exposed, on the way out as well as in.
 */
public record SchoolCreateResponse(
        String schoolId,
        String schoolName,
        String subdomain,
        SchoolStatus status,
        Instant createdAt,
        String nextStep) {

    public static SchoolCreateResponse fromSchool(School school) {
        return new SchoolCreateResponse(
                school.getId(),
                school.getSchoolName(),
                school.getSubdomain(),
                school.getStatus(),
                school.getCreatedAt(),
                "Create the first administrator account for this school, then activate it.");
    }
}
