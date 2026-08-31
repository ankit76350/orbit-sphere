package com.orbitastra.backend.dto.core.platform;

import com.orbitastra.backend.models.core.School;

/**
 * The result of a subdomain change. Endpoint #10.
 *
 * <p>Its own record rather than {@code SchoolStatusResponse}, because the thing a caller needs
 * to know here does not exist on any other platform response: <b>what just broke</b>. Returning
 * only the new subdomain would read like a successful rename and say nothing about the links
 * that stopped working the moment it committed.
 *
 * <p>{@code previousSubdomain} is echoed back rather than stored anywhere. It is what the
 * caller needs in order to know which links just died, and it is gone from the system the moment
 * this response is read — the label is free for anyone to claim.
 */
public record SchoolSubdomainResponse(
        String schoolId,
        String schoolName,
        String previousSubdomain,
        String subdomain,
        String nextStep) {

    public static SchoolSubdomainResponse fromSchool(School school, String previousSubdomain,
            String nextStep) {

        return new SchoolSubdomainResponse(
                school.getId(),
                school.getSchoolName(),
                previousSubdomain,
                school.getSubdomain(),
                nextStep);
    }
}
