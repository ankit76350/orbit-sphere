package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.Size;

/**
 * The school's whole postal address. Endpoint #7.
 *
 * <p><b>A PUT, not a PATCH, and this is the one place in the package where PUT is clearly
 * right.</b> An address is an all-or-nothing value: patching {@code city} without
 * {@code stateOrProvince} produces a real-looking address for a place that does not exist.
 * Sending the whole block every time makes a half-updated address impossible.
 *
 * <p>So every field here is replaced by what is sent. An omitted field is cleared, not left
 * alone — that is what replace means, and it is the opposite of how {@link
 * SchoolProfileUpdateRequest} behaves. Sending {@code {}} empties the address entirely, which
 * is a legitimate thing to want.
 *
 * <p><b>{@code countryCode} is deliberately not here.</b> Changing a school's country changes
 * which tax rules and identity documents apply — {@code GovernmentIdentityType} holds
 * {@code AADHAAR} and {@code APAAR}; {@code FeeHead.taxRatePercent} means GST. Schools do not
 * move countries. Somebody mistyping it at signup is the real scenario, and that is a platform
 * correction while the tenant is still PROVISIONING, not a self-service edit after go-live.
 */
public record SchoolAddressRequest(

        /** Example: "12, MG Road" */
        @Size(max = 200) String addressLine,

        /** Example: "Pune" */
        @Size(max = 100) String city,

        /** Example: "Maharashtra" */
        @Size(max = 100) String stateOrProvince,

        /** Text, not a number — leading zeros and non-numeric formats are real. Example: "411001" */
        @Size(max = 20) String postalCode) {
}
