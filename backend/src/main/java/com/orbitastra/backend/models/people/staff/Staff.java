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
                def = "{'schoolId': 1, 'fullName': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Staff extends SchoolBase {

    // School-scoped number generated using NumberSequenceType.EMPLOYEE_NUMBER.
    // Example: "EMP/2026/000001"
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
    private String phoneNumber;

    // Stored trimmed and lowercase. Example: "anita.sharma@example.com"
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
