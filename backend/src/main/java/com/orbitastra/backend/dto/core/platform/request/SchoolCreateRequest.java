package com.orbitastra.backend.dto.core.platform.request;

import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a platform operator sends to provision a new school.
 *
 * <p>**This deliberately does not accept every field on School.** Four are refused outright
 * rather than ignored, because each one, if settable, hands the caller something the document is
 * meant to defend:
 *
 * <ul>
 * <li>{@code status} — always PROVISIONING or TRIAL on create. A caller who could post ACTIVE
 * would skip whatever activation checks, including the subscription check.</li>
 * <li>{@code encryptionKeyReference} — a KMS pointer the platform derives. A caller who set it
 * could aim a new tenant at another tenant's key.</li>
 * <li>{@code activatedAt} and {@code suspendedAt} — stamped by their own transitions. Supplying
 * them would let a school claim a history it never had.</li>
 * </ul>
 *
 * <p>Being a separate type from the model is what makes that refusal real. Binding the request
 * straight onto School would accept all four silently.
 *
 * <p><b>There is no starting-state choice.</b> Every school starts at PROVISIONING. A school
 * once started at TRIAL instead, chosen by a {@code trial} flag on this request — but nothing in
 * the codebase ever treated the two differently, so it was a second word for one state. A trial
 * is a property of what a school PAYS for and lives on the subscription instead, where it has a
 * plan and a period behind it.
 */
public record SchoolCreateRequest(

        @NotBlank @Size(max = 200) String schoolName,

        @NotBlank @Size(max = 150) String accountHolderName,

        /** Normalised and vetted by CoreHelper; the shape check here is a first pass. */
        @NotBlank @Size(max = 63) String subdomain,

        @Size(max = 30) String phoneNumber,

        @Email @Size(max = 254) String emailAddress,

        /** IETF language tag, such as "en-IN". */
        @NotBlank @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$",
                message = "must be an IETF language tag such as en-IN")
        String defaultLocale,

        /**
         * IANA zone id, such as "Asia/Kolkata".
         *
         * <p><b>An enum since 2026-09-10.</b> It was a {@code @Size(max = 64)} String the service
         * then checked against the JVM's zone set — two places, and the second one is where the
         * refusal actually came from. Now an unknown zone is refused by the binder, before any
         * service sees the request, and the wire value is unchanged.
         */
        @NotNull SchoolTimeZone defaultTimeZone,

        /** ISO 3166-1 alpha-2. Settable only here — see the controller README. */
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$",
                message = "must be a two-letter ISO 3166-1 alpha-2 country code")
        String countryCode,

        @Size(max = 200) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 100) String stateOrProvince,
        @Size(max = 20) String postalCode) {

    public SchoolStatus initialStatus() {
        return SchoolStatus.PROVISIONING;
    }
}
