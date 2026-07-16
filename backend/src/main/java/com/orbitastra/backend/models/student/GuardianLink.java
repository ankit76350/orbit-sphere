package com.orbitastra.backend.models.student;

import com.orbitastra.backend.models.student.enums.GuardianRelation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The link between a {@link Student} and a {@link Guardian}, embedded in the
 * student's {@code guardians} array. Carries the role and per-relationship flags
 * (the same guardian can be "father + primary + portal" to one child and just
 * "emergency contact" to another). {@code guardianId} references {@link Guardian#getId()}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianLink {

    private String guardianId;

    private GuardianRelation relation;

    private boolean primary;

    private boolean emergencyContact;

    private boolean pickupApproved;

    private boolean portalAccess;
}
