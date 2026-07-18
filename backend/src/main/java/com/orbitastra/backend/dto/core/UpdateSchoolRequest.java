package com.orbitastra.backend.dto.core;

import com.orbitastra.backend.models.core.enums.SubscriptionTier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Partial-update payload for a school (PATCH). All fields optional; only
 * non-null fields are applied. {@code active} toggles the tenant on/off.
 */
@Data
public class UpdateSchoolRequest {

    private String schoolName;

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

    private Boolean active;
}
