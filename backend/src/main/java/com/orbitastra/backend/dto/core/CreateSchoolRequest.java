package com.orbitastra.backend.dto.core;

import com.orbitastra.backend.models.core.enums.SubscriptionTier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for onboarding a school (tenant). Server-owned fields
 * ({@code id}, {@code active}, audit timestamps) are set by the service and are
 * not accepted from the request body.
 */
@Data
public class CreateSchoolRequest {

    @NotBlank(message = "schoolName is required")
    private String schoolName;

    @NotBlank(message = "subdomain is required")
    private String subdomain;

    private String logo;

    private String address;

    private String phone;

    @Email(message = "email must be a valid address")
    private String email;

    private SubscriptionTier subscriptionTier;

    @Min(value = 0, message = "maxStudents cannot be negative")
    private Integer maxStudents;

    @Min(value = 0, message = "maxUsers cannot be negative")
    private Integer maxUsers;
}
