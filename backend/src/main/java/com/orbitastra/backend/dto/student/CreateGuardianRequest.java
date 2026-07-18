package com.orbitastra.backend.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for creating a guardian. Server-owned fields ({@code id} and
 * audit timestamps) are intentionally absent to prevent mass-assignment.
 */
@Data
public class CreateGuardianRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "name is required")
    private String name;

    private String phone;

    private String alternatePhone;

    @Email(message = "email must be a valid address")
    private String email;

    private String address;

    private String occupation;
}
