package com.orbitastra.backend.dto.people.staff.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.people.staff.embedded.StaffAddress;

/** One address, as every staff endpoint returns it. Absent fields are omitted, never null. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffAddressResponse(
        String addressLine1,
        String addressLine2,
        String city,
        String stateOrProvince,
        String postalCode,
        CountryCode countryCode) {

    /** Null in, null out — an absent address is an absent key, not an object of six nulls. */
    public static StaffAddressResponse of(StaffAddress address) {
        return address == null ? null : new StaffAddressResponse(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getStateOrProvince(),
                address.getPostalCode(),
                address.getCountryCode());
    }
}
