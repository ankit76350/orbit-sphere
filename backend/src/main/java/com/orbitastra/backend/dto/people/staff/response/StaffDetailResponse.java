package com.orbitastra.backend.dto.people.staff.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.people.staff.Staff;

/**
 * One person in full. Endpoint #8.
 *
 * <h2>This is the fullest thing this product returns about a human being</h2>
 *
 * <p>A date of birth, two addresses, an emergency contact. <b>Which is exactly why the module plan
 * calls authorization the open item that matters most here</b>, and why #7's row carries none of
 * it: a list is read by every dropdown, and this is read by one page on purpose.
 *
 * <p>There is <b>no</b> authorization yet. Every field below is readable by anybody who can reach
 * the API with a school subdomain, and {@code note} says so on every response rather than leaving
 * it to a README nobody opens.
 *
 * <h2>The employment is folded in, not linked</h2>
 *
 * <p>Because "who is this and what do they do" is one question, and every caller would make the
 * second call anyway. It is the person's {@code current = true} record — at most one can exist,
 * by unique partial index.
 *
 * <p><b>Absent when they have none, with {@code employmentNote} saying so.</b> Absent rather than
 * {@code null} or an empty object, for the reason every optional block in this project is: three
 * ways of saying "nothing here" is three cases a client has to handle.
 *
 * <p><b>A person with no employment record is a real state, not a broken row</b> — somebody the
 * school has entered and not yet hired, which is what #1 leaves them in and what #17 returns them
 * to. Before #16 was built on 2026-09-15 it was the <i>only</i> state; it is now one of two.
 */
public record StaffDetailResponse(
        String staffDocsId,
        String employeeNo,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nationalityCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String preferredLanguage,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String phoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailAddress,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        StaffAddressResponse currentAddress,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        StaffAddressResponse permanentAddress,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        EmergencyContactResponse emergencyContact,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String profileImageDocsId,

        /** What they do here now. Absent when they are not employed. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        EmploymentResponse employment,

        /** Why there is no employment block. Present only when there is none. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String employmentNote,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note) {

    public static StaffDetailResponse fromStaff(Staff staff, EmploymentResponse employment,
            String employmentNote, String note) {
        return new StaffDetailResponse(
                staff.getId(),
                staff.getEmployeeNo(),
                staff.getFullName(),
                staff.getDateOfBirth(),
                staff.getGender(),
                staff.getNationalityCode(),
                staff.getPreferredLanguage(),
                staff.getPhoneNumber(),
                staff.getEmailAddress(),
                StaffAddressResponse.of(staff.getCurrentAddress()),
                StaffAddressResponse.of(staff.getPermanentAddress()),
                EmergencyContactResponse.of(staff.getEmergencyContact()),
                staff.getProfileImageDocsId(),
                employment,
                employmentNote,
                note);
    }
}
