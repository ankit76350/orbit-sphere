package com.orbitastra.backend.models.new_new.student.embedded;

import com.orbitastra.backend.models.new_new.common.enums.GuardianRelation;
import com.orbitastra.backend.models.new_new.student.Guardian;
import com.orbitastra.backend.models.new_new.student.Student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The link between a {@link Student} and a {@link Guardian}, embedded in the
 * student's {@code guardians} array. Carries the role and per-relationship flags
 * (the same guardian can be "father + primary + portal" to one child and just
 * "emergency contact" to another). {@code guardianDocsId} references {@link Guardian#getId()}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianLink {

    // References Guardian.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String guardianDocsId;

    // Example: GuardianRelation.FATHER
    @NotNull
    private GuardianRelation relation;

    // Main person contacted by the school. Example: true
    @Builder.Default
    private Boolean primaryContact = false;

    // May be contacted during an emergency. Example: true
    @Builder.Default
    private Boolean emergencyContact = false;

    // Authorized to pick up the student. Example: true
    @Builder.Default
    private Boolean pickupAuthorized = false;

    // May receive guardian-portal access. Example: true
    @Builder.Default
    private Boolean portalAccess = false;
}
