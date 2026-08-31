package com.orbitastra.backend.dto.core.platform;

import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * One school in full, for the operator. The answer to G2.
 *
 * <p>This is the platform version of
 * {@link com.orbitastra.backend.dto.core.profile.SchoolProfileResponse}. The profile record is
 * what a school sees about itself, so it stops at the details the school can edit. This one adds
 * the lifecycle fields the operator's console needs: when the school went live, when it was
 * suspended, why, and when the row was first written and last touched.
 *
 * <p><b>statusReason is on here and must never be on the school surface.</b> It is written for
 * the operator — "Non-payment. Third invoice unpaid past 60 days." — and is not a message to
 * show the school.
 *
 * <p><b>encryptionKeyReference is deliberately missing and must stay missing.</b> It points at a
 * key. No response on either surface may carry it, and a read is the likeliest place for it to
 * be added by accident.
 *
 * <p>There is no {@code nextStep} field. That belongs on the writes, which say what just
 * happened; a read did not change anything, so it has nothing to say.
 *
 * <p>Wider than {@link SchoolSummaryResponse}, which is one row in the G1 list and carries only
 * what a table column shows. This is the whole document.
 */
public record SchoolDetailResponse(
        String schoolId,
        String schoolName,
        String accountHolderName,
        String subdomain,
        String logoUrl,
        String phoneNumber,
        String emailAddress,
        String defaultLocale,
        String defaultTimeZone,
        String addressLine,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode,
        SchoolStatus status,
        String statusReason,
        Instant activatedAt,
        Instant suspendedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static SchoolDetailResponse fromSchool(School school) {
        return new SchoolDetailResponse(
                school.getId(),
                school.getSchoolName(),
                school.getAccountHolderName(),
                school.getSubdomain(),
                school.getLogoUrl(),
                school.getPhoneNumber(),
                school.getEmailAddress(),
                school.getDefaultLocale(),
                school.getDefaultTimeZone(),
                school.getAddressLine(),
                school.getCity(),
                school.getStateOrProvince(),
                school.getPostalCode(),
                school.getCountryCode(),
                school.getStatus(),
                school.getStatusReason(),
                school.getActivatedAt(),
                school.getSuspendedAt(),
                school.getCreatedAt(),
                school.getUpdatedAt());
    }
}
