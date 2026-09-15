package com.orbitastra.backend.dto.people.staff.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.people.staff.Staff;

/**
 * One person, as #1 returns them.
 *
 * <p><b>{@code employeeNo} leads, because it is the thing the school writes down.</b> It is the
 * only field on this response the caller did not send, and the only reason to read the response at
 * all beyond the id.
 *
 * <p><b>{@code staffDocsId} is what every other collection stores.</b> An employment record, a
 * leave balance, a credential, a review — all of them hold this id, never the employee number.
 * The number is for people; the id is for the database.
 *
 * <p><b>There is no status, no department and no joining date here</b>, because there is none on
 * the document. A person is not employed by existing — #16 writes the job, and until it runs this
 * person is somebody the school has entered and not yet hired. That is a real state, and it is the
 * one this endpoint leaves them in.
 */
public record StaffCreatedResponse(
        String employeeNo,
        String staffDocsId,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        CountryCode nationalityCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        SchoolLocale preferredLanguage,

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

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static StaffCreatedResponse fromStaff(Staff staff, String nextStep) {
        return new StaffCreatedResponse(
                staff.getEmployeeNo(),
                staff.getId(),
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
                nextStep);
    }
}
