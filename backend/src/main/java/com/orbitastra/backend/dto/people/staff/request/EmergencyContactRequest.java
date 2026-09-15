package com.orbitastra.backend.dto.people.staff.request;

import jakarta.validation.constraints.Size;

/**
 * Who to call about one staff member. Used by #1, and by #4 when that is built.
 *
 * <p><b>Nothing here is required, not even the name.</b> A contact with only a phone number is
 * still worth having at three in the morning, and the alternative — refusing the whole profile
 * because a relationship was left blank — helps nobody.
 *
 * <p><b>An empty contact is stored as no contact</b>, rather than as an object of three nulls.
 */
public record EmergencyContactRequest(

        @Size(max = 120) String fullName,

        /** How they are related. Free text: "Spouse", "Mother", "Neighbour". */
        @Size(max = 60) String relationship,

        /** Normalised the same way the staff member's own number is. */
        @Size(max = 32) String phoneNumber) {

    /** Whether every field is absent or blank, which is the same as sending no contact. */
    public boolean isEmpty() {
        return blank(fullName) && blank(relationship) && blank(phoneNumber);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
