package com.orbitastra.backend.models.new_new.people.staff.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured address embedded in a Staff profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAddress {

    // Example: "Flat 12, Sunrise Apartments"
    private String addressLine1;

    // Example: "MG Road"
    private String addressLine2;

    // Example: "Pune"
    private String city;

    // Example: "Maharashtra"
    private String stateOrProvince;

    // Stored as text for leading zeros and international formats. Example: "411001"
    private String postalCode;

    // ISO 3166-1 alpha-2 country code. Example: "IN"
    private String countryCode;
}
