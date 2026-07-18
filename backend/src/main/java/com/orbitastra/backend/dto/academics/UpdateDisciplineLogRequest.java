package com.orbitastra.backend.dto.academics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Partial-update payload for a discipline log (PATCH). All fields optional; only
 * non-null fields are applied. {@code academicYear} is accepted only so the
 * service can reject an attempt to change it.
 */
@Data
public class UpdateDisciplineLogRequest {

    private String schoolId;

    private String academicYear;

    private String studentId;

    private String violation;

    @DecimalMin(value = "0.0", message = "fineAmount cannot be negative")
    private BigDecimal fineAmount;

    private String actionTaken;

    private LocalDateTime incidentDate;
}
