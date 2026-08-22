package com.orbitastra.backend.models.student;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A single guardian/contact person such as a father, mother, grandparent, or
 * legal guardian.
 *
 * A Guardian is one real person, deduplicated per school. The student ↔ guardian
 * relationship is many-to-many: one guardian can be linked to siblings, and a
 * student can have multiple guardians. Relationship-specific flags are stored
 * in GuardianLink embedded in Student.
 */
@Document(collection = "guardians")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_guardian_phone_uniq",
                def = "{'schoolId': 1, 'phoneNumber': 1}",
                unique = true,
                partialFilter = "{'phoneNumber': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_guardian_email_uniq",
                def = "{'schoolId': 1, 'emailAddress': 1}",
                unique = true,
                partialFilter = "{'emailAddress': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_guardian_name_idx",
                def = "{'schoolId': 1, 'fullName': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Guardian extends SchoolBase {

    // Example: "Rohan Sharma"
    @NotBlank
    private String fullName;

    // Normalized international number. Example: "+919876543210"
    private String phoneNumber;

    // Example: "+919812345678"
    // This value is not unique because it may be a shared family number.
    private String alternatePhoneNumber;

    // Stored trimmed and lowercase. Example: "rohan.sharma@example.com"
    private String emailAddress;

    // Example: "12 Park Road, Pune, Maharashtra 411001"
    private String address;

    // Example: "Software Engineer"
    private String occupation;

    // IETF language tag. Example: "en-IN"
    private String preferredLanguage;
}
