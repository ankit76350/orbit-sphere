package com.orbitastra.backend.dto.student;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Partial-update payload for a guardian (PATCH). All fields optional; only
 * non-null fields are applied. {@code schoolId} is not editable — a guardian
 * cannot be moved between tenants.
 */
@Data
public class UpdateGuardianRequest {

    private String name;

    private String phone;

    private String alternatePhone;

    @Email(message = "email must be a valid address")
    private String email;

    private String address;

    private String occupation;
}
