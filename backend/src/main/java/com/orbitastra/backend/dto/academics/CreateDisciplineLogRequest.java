package com.orbitastra.backend.dto.academics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for logging a discipline incident. Server-owned fields
 * ({@code id} and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateDisciplineLogRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String academicYear;

    @NotBlank(message = "studentId is required")
    private String studentId;

    private String violation;

    @DecimalMin(value = "0.0", message = "fineAmount cannot be negative")
    private BigDecimal fineAmount;

    private String actionTaken;

    private LocalDateTime incidentDate;
}
