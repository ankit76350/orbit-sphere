package com.orbitastra.backend.dto.core.platform;

import com.orbitastra.backend.models.core.enums.SchoolStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
 * <p>{@code trial} decides between the two legal starting states. It is a boolean rather than
 * taking a SchoolStatus for the same reason: a boolean cannot express ACTIVE.
 *
 * <p>The account holder is the person on the contract, and only their name is taken. **No staff
 * record is created here** — see SchoolProvisioningService for why that turned out to be the
 * right call.
 */
public record SchoolCreateRequest(

        @NotBlank @Size(max = 200) String schoolName,

        @NotBlank @Size(max = 150) String accountHolderName,

        /** Normalised and vetted by CoreValidator; the shape check here is a first pass. */
        @NotBlank @Size(max = 63) String subdomain,

        @Size(max = 30) String phoneNumber,

        @Email @Size(max = 254) String emailAddress,

        /** IETF language tag, such as "en-IN". */
        @NotBlank @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$",
                message = "must be an IETF language tag such as en-IN")
        String defaultLocale,

        /** IANA zone id, such as "Asia/Kolkata". Validated as a real zone by the service. */
        @NotBlank @Size(max = 64) String defaultTimeZone,

        /** ISO 3166-1 alpha-2. Settable only here — see the controller README. */
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$",
                message = "must be a two-letter ISO 3166-1 alpha-2 country code")
        String countryCode,

        @Size(max = 200) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 100) String stateOrProvince,
        @Size(max = 20) String postalCode,

        /**
         * True starts the tenant at TRIAL instead of PROVISIONING.
         *
         * <p>A boxed Boolean, not a primitive. Jackson maps an absent JSON field to null, and
         * null into a primitive {@code boolean} is a hard parse failure — so omitting an
         * optional flag returned a 400 with a stack trace instead of defaulting to false.
         * Every optional scalar in a request record has to be boxed for this reason.
         */
        Boolean trial) {

    public SchoolStatus initialStatus() {
        return Boolean.TRUE.equals(trial) ? SchoolStatus.TRIAL : SchoolStatus.PROVISIONING;
    }
}
