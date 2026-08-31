package com.orbitastra.backend.services.core;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.profile.SchoolAddressRequest;
import com.orbitastra.backend.dto.core.profile.SchoolLocalizationRequest;
import com.orbitastra.backend.dto.core.profile.SchoolLogoRequest;
import com.orbitastra.backend.dto.core.profile.SchoolProfileResponse;
import com.orbitastra.backend.dto.core.profile.SchoolProfileUpdateRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.services.core.helper.CoreValidator;
import com.orbitastra.backend.services.core.helper.TextHelper;

import lombok.RequiredArgsConstructor;

/**
 * The school editing its own details. Endpoints #6 to #9.
 *
 * <p>Kept apart from SchoolPlatformService, which is the platform surface. The split is the same one the
 * controllers make: creating, activating and suspending a tenant are things done <i>to</i> a
 * school from outside it; changing an address is something a school does to itself. Nothing here
 * can reach a lifecycle status, the subdomain or the encryption key — not by permission, but
 * because the code to do it is not in this class and the fields are not on its DTOs.
 *
 * <p>Every method resolves the tenant through CurrentSchoolResolver rather than taking an id.
 * A caller never names the school they are editing, so they cannot name somebody else's.
 */
@Service
@RequiredArgsConstructor
public class SchoolProfileService {

    /** Hosts a logo may be served from. A school-supplied URL is not trusted on its own. */
    private static final Set<String> ALLOWED_LOGO_HOSTS = Set.of(
            "cdn.example.com", "res.cloudinary.com", "s3.amazonaws.com",
            "storage.googleapis.com");

    private final SchoolRepository schools;
    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;
    private final CoreValidator coreValidator;

    //! endpoint 6 — the school's own details ------------------------------------------

    // Gets the current school.
    // Rejects empty updates.
    // Updates only provided fields.
    // Saves the updated school.
    @Transactional
    public SchoolProfileResponse updateProfile(SchoolProfileUpdateRequest request) {
        // get the current schhool details from the login user
        //! step 1 - who is asking, and may they edit
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing
        // A PATCH with an empty body is almost always a client bug. Answering 200 hides it.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send at least one of schoolName, phoneNumber or emailAddress.");
        }

        //! step 3 - apply only what was sent
        if (request.schoolName() != null) {
            String name = request.schoolName().trim();
            if (name.isEmpty()) {
                throw ApiException.badRequest("SCHOOL_NAME_REQUIRED",
                        "A school name cannot be removed. Send a new one, or omit the field.");
            }
            school.setSchoolName(name);
        }
        if (request.phoneNumber() != null) {
            school.setPhoneNumber(TextHelper.blankToNull(request.phoneNumber()));
        }
        if (request.emailAddress() != null) {
            String email = TextHelper.lowercaseOrNull(request.emailAddress());
            if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
                throw ApiException.badRequest("EMAIL_INVALID",
                        "'" + request.emailAddress() + "' is not a valid email address.");
            }
            school.setEmailAddress(email);
        }

        
         //TODO: - save
        School savedSchool = schools.save(school);
        // countryCode is untouched on purpose and is not on the request. See the DTO.
        return SchoolProfileResponse.fromSchool(savedSchool);
    }

    //! endpoint 7 — the postal address ------------------------------------------------

    /**
     * Replaces the school's complete postal address.
    */
    @Transactional
    public SchoolProfileResponse replaceAddress(SchoolAddressRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - replace every field, including with null
        school.setAddressLine(TextHelper.blankToNull(request.addressLine()));
        school.setCity(TextHelper.blankToNull(request.city()));
        school.setStateOrProvince(TextHelper.blankToNull(request.stateOrProvince()));
        school.setPostalCode(TextHelper.blankToNull(request.postalCode()));

        //TODO: - save
        School savedSchool = schools.save(school);
        return SchoolProfileResponse.fromSchool(savedSchool);
    }

    //! endpoint 8 — language and time zone --------------------------------------------

    //? step 1 - get the current school
    //? step 2 - reject empty updates
    //? step 3 - update locale
    //? step 4 - validate and update time zone
    //? step 5 - save changes
    @Transactional
    public SchoolProfileResponse updateLocalization(SchoolLocalizationRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send defaultLocale, defaultTimeZone, or both.");
        }

        //! step 3 - locale, if sent
        if (request.defaultLocale() != null) {
            String locale = request.defaultLocale().trim();
            if (locale.isEmpty()) {
                throw ApiException.badRequest("LOCALE_REQUIRED",
                        "A default locale cannot be removed. Send a new one, or omit the field.");
            }
            school.setDefaultLocale(locale);
        }

        //! step 4 - time zone, if sent, and only with both guards satisfied
        if (request.defaultTimeZone() != null) {
            String zone = coreValidator.validateTimeZone(request.defaultTimeZone());

            if (!zone.equals(school.getDefaultTimeZone())) {
                if (!Boolean.TRUE.equals(request.confirmTimeZoneChange())) {
                    throw ApiException.conflict("TIME_ZONE_CHANGE_NOT_CONFIRMED",
                            "Changing the time zone from " + school.getDefaultTimeZone() + " to "
                                    + zone + " reinterprets which calendar date every existing "
                                    + "attendance record, holiday and trip falls on. Send "
                                    + "confirmTimeZoneChange: true if you mean it.");
                }
                // The guard that actually protects the data. Once a year is running, its
                // attendance and holidays are already anchored to the old zone, and moving it
                // shifts them with no error anywhere.
                LocalDate today = LocalDate.now();
                boolean yearInProgress = academicYears
                        .existsBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                school.getId(), today, today);
                if (yearInProgress) {
                    throw ApiException.conflict("ACADEMIC_YEAR_IN_PROGRESS",
                            "An academic year is running. The time zone cannot be changed until "
                                    + "it ends, because existing attendance and holidays are "
                                    + "anchored to " + school.getDefaultTimeZone() + ".");
                }
                school.setDefaultTimeZone(zone);
            }
        }


        //TODO: - save
        School savedSchool = schools.save(school);
        return SchoolProfileResponse.fromSchool(savedSchool);
    }

    //?? endpoint 9 — the logo ----------------------------------------------------------

    /**
    * Replaces or removes the school's logo.
    */
    @Transactional
    public SchoolProfileResponse replaceLogo(SchoolLogoRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - blank means remove
        String url = TextHelper.blankToNull(request.logoUrl());
        if (url == null) {
            school.setLogoUrl(null);
            return SchoolProfileResponse.fromSchool(schools.save(school));
        }

        //! step 3 - https only, allow-listed host only
        if (!url.startsWith("https://")) {
            throw ApiException.badRequest("LOGO_URL_NOT_HTTPS",
                    "A logo URL must start with https://. Parents load this on their own "
                            + "devices.");
        }
        String host;
        try {
            host = java.net.URI.create(url).getHost();
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("LOGO_URL_INVALID",
                    "'" + url + "' is not a usable URL.");
        }
        if (host == null || !ALLOWED_LOGO_HOSTS.contains(host.toLowerCase())) {
            throw ApiException.badRequest("LOGO_HOST_NOT_ALLOWED",
                    "Logos may only be served from " + String.join(", ", ALLOWED_LOGO_HOSTS)
                            + ". Received host: " + host);
        }

        //! step 4 - save
        school.setLogoUrl(url);
        
        //TODO: - save
        School savedSchool = schools.save(school);
        return SchoolProfileResponse.fromSchool(savedSchool);
    }
}
