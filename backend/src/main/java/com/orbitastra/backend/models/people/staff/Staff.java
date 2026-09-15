package com.orbitastra.backend.models.people.staff;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.people.staff.embedded.EmergencyContact;
import com.orbitastra.backend.models.people.staff.embedded.StaffAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Personal and contact profile of one employee belonging to a school.
 *
 * <p>Employment terms, position history, salary, login roles, and government
 * identity numbers are deliberately stored in their own collections.
 */
@Document(collection = "staff")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_employee_no_uniq",
                def = "{'schoolId': 1, 'employeeNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_staff_name_idx",
                def = "{'schoolId': 1, 'fullName': 1}"),

        // Added 2026-09-15. A phone number and an email address each identify ONE person within
        // a school, and #1 refuses a duplicate of either.
        //
        // BOTH ARE PARTIAL, AND THAT IS NOT A DETAIL. Neither field is required, and a plain
        // unique index treats every missing value as null — so ONE staff member per school could
        // have no phone, and the second would be rejected by the database with no explanation.
        // Measured on 2026-09-15: 74 schools already hold more than one person with no phone.
        // The same shape UserAccount uses for normalizedEmail and normalizedPhone.
        //
        // MONGO COMPARES THEM EXACTLY. The service folds the email to lower case and strips the
        // spacing characters out of the phone before either is stored or checked, so it is the
        // stricter of the two and the enforcement in practice — the index is what catches
        // anything that ever bypasses it.
        @CompoundIndex(
                name = "school_staff_phone_uniq",
                def = "{'schoolId': 1, 'phoneNumber': 1}",
                unique = true,
                partialFilter = "{'phoneNumber': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_staff_email_uniq",
                def = "{'schoolId': 1, 'emailAddress': 1}",
                unique = true,
                partialFilter = "{'emailAddress': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Staff extends SchoolBase {

    // School-scoped number generated using NumberSequenceType.EMPLOYEE_NUMBER.
    // Example: "EMP/2026/09/000001"
    @NotBlank
    private String employeeNo;

    // Example: "Anita Sharma"
    @NotBlank
    private String fullName;

    // Example: 1990-08-14
    @NotNull
    private LocalDate dateOfBirth;

    // Example: Gender.FEMALE
    @NotNull
    private Gender gender;

    // ISO 3166-1 alpha-2 nationality code. Example: "IN"
    private String nationalityCode;

    // IETF language tag. Example: "en-IN"
    private String preferredLanguage;

    // Stored in normalized international format. Example: "+919876543210"
    // Unique within the school when present — school_staff_phone_uniq.
    private String phoneNumber;

    // Stored trimmed and lowercase. Example: "anita.sharma@example.com"
    // Unique within the school when present — school_staff_email_uniq.
    private String emailAddress;

    // Current residential address.
    private StaffAddress currentAddress;

    // Permanent or legal address.
    private StaffAddress permanentAddress;

    // Contact used during an emergency.
    private EmergencyContact emergencyContact;

    // Links to the stored profile-image document.
    // Example: "67aa15d9dc3f7d0012345678"
    private String profileImageDocsId;
}
