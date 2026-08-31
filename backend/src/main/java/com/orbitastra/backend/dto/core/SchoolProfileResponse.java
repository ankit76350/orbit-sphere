package com.orbitastra.backend.dto.core;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * The school as it now stands, after a self-service edit. Shared by endpoints #6 to #9.
 *
 * <p>One record for all four rather than four nearly identical ones. Each returns the whole
 * editable profile rather than only the fields it touched, so a form can replace its state from
 * the response instead of guessing what changed — and four records with overlapping fields would
 * drift the first time somebody added something to one of them.
 *
 * <p><b>What is deliberately absent:</b> {@code encryptionKeyReference} (a KMS pointer, never
 * exposed in either direction), {@code activatedAt} and {@code suspendedAt} (lifecycle history,
 * which belongs to the platform surface). {@code subdomain} and {@code status} are here to be
 * read but cannot be changed by any endpoint on this surface.
 */
public record SchoolProfileResponse(
        String schoolId,
        String subdomain,
        SchoolStatus status,
        String schoolName,
        String accountHolderName,
        String phoneNumber,
        String emailAddress,
        String logoUrl,
        String defaultLocale,
        String defaultTimeZone,
        String addressLine,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode) {

    public static SchoolProfileResponse fromSchool(School school) {
        return new SchoolProfileResponse(
                school.getId(),
                school.getSubdomain(),
                school.getStatus(),
                school.getSchoolName(),
                school.getAccountHolderName(),
                school.getPhoneNumber(),
                school.getEmailAddress(),
                school.getLogoUrl(),
                school.getDefaultLocale(),
                school.getDefaultTimeZone(),
                school.getAddressLine(),
                school.getCity(),
                school.getStateOrProvince(),
                school.getPostalCode(),
                school.getCountryCode());
    }
}
