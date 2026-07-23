package com.orbitastra.backend.dto.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.orbitastra.backend.models.student.enums.GuardianRelation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client payload for specifying a guardian during student creation or linking.
 * Supports either referencing an existing guardian by {@code guardianDocsId}, or
 * providing full person attributes ({@code name}, {@code phone}, {@code email}, etc.)
 * which will be deduplicated server-side via find-or-create.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGuardianRequest {

    // Reference an existing Guardian by ID (optional if full details below are provided)
    private String guardianDocsId;

    // Person attributes (used for find-or-create deduplication if guardianDocsId is absent)
    private String name;

    private GuardianRelation relation;

    private String phone;

    @Email(message = "email must be a valid address")
    private String email;

    private String address;

    private String occupation;

    // Relationship flags
    private Boolean primary;

    private Boolean emergencyContact;

    private Boolean pickupApproved;

    private Boolean portalAccess;

    @JsonIgnore
    @AssertTrue(message = "guardianDocsId or guardian name is required")
    public boolean isReferenceOrDetailsProvided() {
        return (guardianDocsId != null && !guardianDocsId.isBlank())
                || (name != null && !name.isBlank());
    }
}
