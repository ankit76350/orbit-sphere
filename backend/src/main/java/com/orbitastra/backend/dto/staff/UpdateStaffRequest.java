package com.orbitastra.backend.dto.staff;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Partial-update payload for a staff member (PATCH). All fields optional; only
 * non-null fields are applied. {@code schoolId} is intentionally omitted so a
 * staff member cannot be moved between tenants via an update.
 */
@Data
public class UpdateStaffRequest {

    private String employeeNo;

    private String name;

    private String department;

    private String designation;

    @DecimalMin(value = "0.0", message = "salary cannot be negative")
    private BigDecimal salary;

    private LocalDate joiningDate;

    private LocalDate dob;
}
