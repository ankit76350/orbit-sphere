package com.orbitastra.backend.dto.core.platform;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * One school as it appears in the platform list. Endpoint G1.
 *
 * <p>A <b>summary</b>, not the whole document. A list is read to find a school, not to work on
 * one, and returning every field of every row makes the page heavier for nothing — G2 returns the
 * full record once the operator has picked one.
 *
 * <p>What is here is what an operator scans or acts on: which school, what state it is in, who to
 * ring, and when it last changed hands.
 *
 * <p><b>{@code encryptionKeyReference} is deliberately absent</b>, as it is from every other
 * response in this package. It is a pointer to a key, and a list endpoint is the easiest place
 * for a field to be added without anyone thinking about it.
 *
 * <p>{@code statusReason} <b>is</b> here, and it is the one field that must never leak to the
 * school surface: it is written for the operator — "Non-payment. Third invoice unpaid past 60
 * days." — not as a message to the school. It belongs on the platform list precisely because
 * that is the screen where somebody asks why a tenant is suspended.
 */
public record SchoolSummaryResponse(
        String schoolId,
        String schoolName,
        String subdomain,
        SchoolStatus status,
        String statusReason,
        String accountHolderName,
        String emailAddress,
        String phoneNumber,
        String city,
        String countryCode,
        Instant createdAt,
        Instant activatedAt,
        Instant suspendedAt) {

    public static SchoolSummaryResponse fromSchool(School school) {
        return new SchoolSummaryResponse(
                school.getId(),
                school.getSchoolName(),
                school.getSubdomain(),
                school.getStatus(),
                school.getStatusReason(),
                school.getAccountHolderName(),
                school.getEmailAddress(),
                school.getPhoneNumber(),
                school.getCity(),
                school.getCountryCode(),
                school.getCreatedAt(),
                school.getActivatedAt(),
                school.getSuspendedAt());
    }
}
