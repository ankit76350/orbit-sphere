package com.orbitastra.backend.dto.people.staff.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.staff.embedded.EmergencyContact;

/** Who to call, as every staff endpoint returns it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmergencyContactResponse(
        String fullName,
        String relationship,
        String phoneNumber) {

    /** Null in, null out — an absent contact is an absent key. */
    public static EmergencyContactResponse of(EmergencyContact contact) {
        return contact == null ? null : new EmergencyContactResponse(
                contact.getFullName(),
                contact.getRelationship(),
                contact.getPhoneNumber());
    }
}
