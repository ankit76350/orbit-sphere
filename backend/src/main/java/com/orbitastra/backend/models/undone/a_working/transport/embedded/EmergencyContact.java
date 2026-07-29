package com.orbitastra.backend.models.undone.a_working.transport.embedded;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Driver emergency contact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    /**
     * Contact person's name.
     */
    private String name;

    /**
     * Relationship with driver.
     *
     * Example:
     * Wife
     * Father
     * Brother
     */
    private String relationship;

    /**
     * Contact phone number.
     */
    private String phone;

}