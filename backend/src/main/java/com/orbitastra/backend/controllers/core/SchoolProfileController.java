package com.orbitastra.backend.controllers.core;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.profile.SchoolAddressRequest;
import com.orbitastra.backend.dto.core.profile.SchoolLocalizationRequest;
import com.orbitastra.backend.dto.core.profile.SchoolLogoRequest;
import com.orbitastra.backend.dto.core.profile.SchoolProfileResponse;
import com.orbitastra.backend.dto.core.profile.SchoolProfileUpdateRequest;
import com.orbitastra.backend.services.core.SchoolProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The school surface. Endpoints #6 to #9 of the plan in this package's README.
 *
 * <p><b>The path is {@code /schools/current}, never {@code /schools/{id}}.</b> That is the most
 * important thing about this controller. The tenant comes from CurrentSchoolResolver — today a
 * header, tomorrow the session — and never from the URL. A path parameter invites the bug where
 * a school admin passes somebody else's id and edits their school; with no id in the path, a
 * caller cannot name a school they do not belong to because they never name one at all.
 *
 * <p>Separate from SchoolController, which is the platform surface at {@code /platform/schools}.
 * That one creates and suspends tenants and its caller is outside the tenant entirely. This one
 * is a school editing itself. Nothing here can reach {@code status}, {@code subdomain} or
 * {@code encryptionKeyReference} — the methods do not exist and the fields are not on the DTOs.
 *
 * <p><b>There is no authentication yet.</b> The tenant header is a stand-in and any caller can
 * set it to any school's subdomain, which means anybody can edit any school. Fine on a
 * developer machine, unacceptable anywhere else. See CurrentSchoolResolver.
 *
 * <p>PATCH where the request is a partial edit, PUT where the value is replaced whole. Address
 * and logo are PUTs because both are all-or-nothing: a patched address can name a city in the
 * wrong state, and a logo either exists or does not.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current")
public class SchoolProfileController {

    private final SchoolProfileService schoolProfileService;

    /**
     * Endpoint #6 — the school's own name and contact details.
     *
     * <p>Partial: null leaves a field alone, {@code ""} clears it. An empty body is a 400.
     */
    @PatchMapping("/profile")
    public ResponseEntity<SchoolProfileResponse> updateProfile(
            @Valid @RequestBody SchoolProfileUpdateRequest request) {

        return ResponseEntity.ok(schoolProfileService.updateProfile(request));
    }

    /**
     * Endpoint #7 — replaces the whole postal address.
     *
     * <p>An omitted field is cleared. {@code countryCode} is not editable here.
     */
    @PutMapping("/address")
    public ResponseEntity<SchoolProfileResponse> replaceAddress(
            @Valid @RequestBody SchoolAddressRequest request) {

        return ResponseEntity.ok(schoolProfileService.replaceAddress(request));
    }

    /**
     * Endpoint #8 — language and time zone.
     *
     * <p>A time-zone change needs {@code confirmTimeZoneChange: true} and is refused outright
     * while an academic year is running.
     */
    @PatchMapping("/localization")
    public ResponseEntity<SchoolProfileResponse> updateLocalization(
            @Valid @RequestBody SchoolLocalizationRequest request) {

        return ResponseEntity.ok(schoolProfileService.updateLocalization(request));
    }

    /**
     * Endpoint #9 — replaces the logo, or removes it when the URL is blank.
     *
     * <p>https and an allow-listed host only. A file upload would be better; there is no storage
     * service yet.
     */
    @PutMapping("/logo")
    public ResponseEntity<SchoolProfileResponse> replaceLogo(
            @Valid @RequestBody SchoolLogoRequest request) {

        return ResponseEntity.ok(schoolProfileService.replaceLogo(request));
    }
}
