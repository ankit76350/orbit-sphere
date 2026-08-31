package com.orbitastra.backend.dto.core.platform;

import java.util.List;

import com.orbitastra.backend.models.core.School;

/**
 * The result of a subdomain change. Endpoint #10.
 *
 * <p>Its own record rather than {@code SchoolStatusResponse}, because the thing a caller needs
 * to know here does not exist on any other platform response: <b>what just broke</b>. Returning
 * only the new subdomain would read like a successful rename and say nothing about the links
 * that stopped working the moment it committed.
 *
 * <p>{@code previousSubdomains} is returned in full, not just the label released by this call.
 * They are all still held by this school, and seeing the list is the only way to know that
 * without reading the database.
 */
public record SchoolSubdomainResponse(
        String schoolId,
        String schoolName,
        String previousSubdomain,
        String subdomain,
        List<String> previousSubdomains,
        String nextStep) {

    public static SchoolSubdomainResponse fromSchool(School school, String previousSubdomain,
            String nextStep) {

        return new SchoolSubdomainResponse(
                school.getId(),
                school.getSchoolName(),
                previousSubdomain,
                school.getSubdomain(),
                school.getPreviousSubdomains() == null ? List.of()
                        : List.copyOf(school.getPreviousSubdomains()),
                nextStep);
    }
}
