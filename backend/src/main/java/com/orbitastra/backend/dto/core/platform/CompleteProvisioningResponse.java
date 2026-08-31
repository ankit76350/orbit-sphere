package com.orbitastra.backend.dto.core.platform;

import java.util.List;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;

/**
 * What finishing a tenant's setup did.
 *
 * <p>Created and skipped are reported separately because this endpoint is idempotent, and the
 * two mean different things. Everything created means the tenant really was empty. Everything
 * skipped means it was already complete and this call changed nothing — a success, not a
 * failure, and an operator should not have to open the database to tell which happened.
 *
 * <p>{@code readyToActivate} is the question the operator actually has, so it is answered here
 * rather than left to be worked out from two counts.
 */
public record CompleteProvisioningResponse(
        String schoolId,
        String subdomain,
        SchoolStatus status,
        int numberSequencesCreated,
        int numberSequencesAlreadyPresent,
        int rolesCreated,
        int rolesAlreadyPresent,
        List<String> roleKeys,
        boolean readyToActivate,
        String nextStep) {

    public static CompleteProvisioningResponse fromSchool(
            School school,
            int sequencesCreated,
            int sequencesAlreadyPresent,
            int rolesCreated,
            int rolesAlreadyPresent,
            List<String> roleKeys) {

        // Ready means every sequence type has a row and SCHOOL_ADMIN exists — the two things
        // the first administrator account needs. Checked on totals rather than on what this
        // call created, so a school that was already complete still reads as ready.
        boolean ready = sequencesCreated + sequencesAlreadyPresent == NumberSequenceType.values().length
                && roleKeys.contains("SCHOOL_ADMIN");

        return new CompleteProvisioningResponse(
                school.getId(),
                school.getSubdomain(),
                school.getStatus(),
                sequencesCreated,
                sequencesAlreadyPresent,
                rolesCreated,
                rolesAlreadyPresent,
                roleKeys,
                ready,
                ready
                        ? "Create the first administrator account, then activate the school."
                        : "Setup is incomplete. Run this again and check the counts.");
    }
}
