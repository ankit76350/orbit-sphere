package com.orbitastra.backend.dto.core;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * What provisioning gives back.
 *
 * <p>{@code numberSequencesCreated} and {@code rolesCreated} are returned rather than left for
 * the caller to go and check. Provisioning creates three kinds of thing in one transaction, and
 * a response that only confirmed the School row would leave an operator unable to tell a
 * complete tenant from one that needs {@code retry-provisioning}.
 *
 * <p>{@code nextStep} is here because **a provisioned school cannot be logged into yet.** No
 * user account exists. Saying so in the response is the difference between an operator moving on
 * and an operator waiting for an invitation email that nothing is going to send.
 *
 * <p>{@code encryptionKeyReference} is deliberately not exposed, on the way out as well as in.
 */
public record ProvisionSchoolResponse(
        String schoolId,
        String schoolName,
        String subdomain,
        SchoolStatus status,
        int numberSequencesCreated,
        int rolesCreated,
        Instant createdAt,
        String nextStep) {

    public static ProvisionSchoolResponse of(School school, int sequences, int roles) {
        return new ProvisionSchoolResponse(
                school.getId(),
                school.getSchoolName(),
                school.getSubdomain(),
                school.getStatus(),
                sequences,
                roles,
                school.getCreatedAt(),
                "Create the first administrator account for this school, then activate it.");
    }
}
