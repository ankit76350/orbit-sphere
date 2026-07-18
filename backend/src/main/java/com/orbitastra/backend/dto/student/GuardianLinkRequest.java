package com.orbitastra.backend.dto.student;

import java.util.List;
import java.util.stream.Collectors;

import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.enums.GuardianRelation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for linking a guardian to a student with a role and flags.
 * References an existing {@code Guardian} by id — the guardian must already
 * exist and belong to the student's school (validated in the service).
 */
@Data
public class GuardianLinkRequest {

    @NotBlank(message = "guardianId is required")
    private String guardianId;

    private GuardianRelation relation;

    private boolean primary;

    private boolean emergencyContact;

    private boolean pickupApproved;

    private boolean portalAccess;

    public GuardianLink toModel() {
        return GuardianLink.builder()
                .guardianId(guardianId)
                .relation(relation)
                .primary(primary)
                .emergencyContact(emergencyContact)
                .pickupApproved(pickupApproved)
                .portalAccess(portalAccess)
                .build();
    }

    public static List<GuardianLink> toModels(List<GuardianLinkRequest> requests) {
        if (requests == null) return new java.util.ArrayList<>();
        return requests.stream().map(GuardianLinkRequest::toModel).collect(Collectors.toList());
    }
}
