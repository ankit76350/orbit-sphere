package com.orbitastra.backend.dto.staff;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.orbitastra.backend.models.undone.user.enums.Role;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for creating a staff member. Server-owned fields ({@code id}
 * and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateStaffRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String employeeId;

    @NotBlank(message = "name is required")
    private String name;

    private String department;

    private String designation;

    @DecimalMin(value = "0.0", message = "salary cannot be negative")
    private BigDecimal salary;

    private LocalDate joiningDate;

    private Role role;

    private LocalDate dob;
}
