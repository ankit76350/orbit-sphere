package com.orbitastra.backend.models.people.staff.enums;

/**
 * Verification lifecycle of a StaffCredential.
 */
public enum CredentialVerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED,
    EXPIRED
}
