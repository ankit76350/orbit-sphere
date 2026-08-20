package com.orbitastra.backend.models.new_new.common.enums;

/**
 * How far the school has got in checking that an identity document is genuine.
 *
 * <p>In common because both staff and students hand over identity documents, and neither
 * owns the idea of having checked one.
 */
public enum IdentityVerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED,
    EXPIRED
}
