package com.orbitastra.backend.models.new_new.people.staff.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Emergency contact embedded in a Staff profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    // Example: "Rahul Sharma"
    private String fullName;

    // Example: "SPOUSE"
    private String relationship;

    // Stored in normalized international format. Example: "+919876543210"
    private String phoneNumber;
}
