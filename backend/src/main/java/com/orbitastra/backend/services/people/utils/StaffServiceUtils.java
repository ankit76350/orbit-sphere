package com.orbitastra.backend.services.people.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.dto.people.staff.request.EmergencyContactRequest;
import com.orbitastra.backend.dto.people.staff.request.StaffAddressRequest;
import com.orbitastra.backend.models.people.staff.embedded.EmergencyContact;
import com.orbitastra.backend.models.people.staff.embedded.StaffAddress;

/**
 * The work {@link com.orbitastra.backend.services.people.StaffService} does around its writes
 * rather than inside them.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and a method here
 * never calls another method here.
 */
@Component
public class StaffServiceUtils {

    // What people type into phone numbers and nobody stores: spaces (including the non-breaking
    // one a paste from a spreadsheet brings), brackets, hyphens and dots.
    private static final String PHONE_NOISE = "[\\s\\u00A0()\\-.]";

    /**
     * Strips the characters people type into phone numbers but nobody stores.
     *
     * <p>{@code "+91 98765-43210"} becomes {@code "+919876543210"}, which is exactly the form
     * {@code Staff}'s own field comment gives as its example. Spaces, hyphens, brackets, dots and
     * non-breaking spaces go; a leading {@code +} stays, because it is the thing that makes a
     * number international rather than decoration.
     *
     * <p><b>It does NOT invent a country code, and that gap is deliberate.</b> The model README
     * says "normalized to international format", which for a bare {@code 9876543210} would mean
     * guessing {@code +91} from the school's {@code countryCode}. That needs a dialling-code table
     * this project does not have, and a wrong guess writes a number that looks right and cannot be
     * called — worse than a national number that is obviously national. A number given without a
     * {@code +} is stored as given.
     *
     * <p>Returns null for blank input, so an empty box is an absent field rather than "".
     *
     * Used by:
     * - createStaff()
     */
    public String normalisePhone(String value) {
        if (value == null) {
            return null;
        }

        return stripPhone(value);
    }

    /**
     * Turns an address request into the embedded document, or null when nothing was filled in.
     *
     * <p><b>An empty address is stored as no address at all.</b> Six nulls inside an object and no
     * object are the same fact, and a reader should not have to tell them apart — #8 renders "no
     * address on file" either way, so storing the shell only adds a case.
     *
     * <p>Country codes are upper-cased for the same reason every ISO code in this project is:
     * {@code "in"} and {@code "IN"} are one country, and a filter should not need to know which
     * the school typed that day.
     *
     * Used by:
     * - createStaff()
     */
    public StaffAddress toAddress(StaffAddressRequest request) {
        if (request == null || request.isEmpty()) {
            return null;
        }

        return StaffAddress.builder()
                .addressLine1(trimToNull(request.addressLine1()))
                .addressLine2(trimToNull(request.addressLine2()))
                .city(trimToNull(request.city()))
                .stateOrProvince(trimToNull(request.stateOrProvince()))
                .postalCode(trimToNull(request.postalCode()))
                .countryCode(upperOrNull(request.countryCode()))
                .build();
    }

    /**
     * Turns an emergency contact request into the embedded document, or null when it is empty.
     *
     * <p>The phone is normalised the same way the staff member's own is — it is the same kind of
     * thing, and a contact number that only works when typed one way is not a contact number.
     *
     * Used by:
     * - createStaff()
     */
    public EmergencyContact toEmergencyContact(EmergencyContactRequest request) {
        if (request == null || request.isEmpty()) {
            return null;
        }

        return EmergencyContact.builder()
                .fullName(trimToNull(request.fullName()))
                .relationship(trimToNull(request.relationship()))
                // Through the same private helper normalisePhone uses, NOT through normalisePhone
                // itself: a method here never calls another method here, and a private static is
                // how two of them share one rule without breaking that.
                .phoneNumber(stripPhone(request.phoneNumber()))
                .build();
    }

    private static String stripPhone(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.replaceAll(PHONE_NOISE, "");
        return stripped.isEmpty() ? null : stripped;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upperOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }
}
