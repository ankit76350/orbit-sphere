package com.orbitastra.backend.dto.people.staff.request;

import jakarta.validation.constraints.Size;

/**
 * One address on a staff profile. Used by #1, and by #3 when that is built.
 *
 * <p><b>Every field is optional, including the country.</b> A school entering a roll of people
 * often has a city and nothing else, and refusing a partial address would mean holding the whole
 * thing back until somebody chases a postal code.
 *
 * <p><b>An address with nothing in it is stored as no address at all</b>, rather than as an object
 * of six nulls — see {@link #isEmpty()}. A reader should not have to tell those two apart.
 */
public record StaffAddressRequest(

        @Size(max = 160) String addressLine1,
        @Size(max = 160) String addressLine2,
        @Size(max = 80) String city,
        @Size(max = 80) String stateOrProvince,
        @Size(max = 20) String postalCode,

        /** ISO 3166-1 alpha-2, stored upper-cased. "IN". */
        @Size(max = 2) String countryCode) {

    /** Whether every field is absent or blank, which is the same as sending no address. */
    public boolean isEmpty() {
        return blank(addressLine1) && blank(addressLine2) && blank(city)
                && blank(stateOrProvince) && blank(postalCode) && blank(countryCode);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
