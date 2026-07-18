package com.orbitastra.backend.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.orbitastra.backend.models.finance.enums.FeeType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Client payload for raising a fee invoice. Only fields the caller is allowed to
 * set are present — {@code id}, {@code invoiceNo}, {@code paidAmount},
 * {@code status} and the audit timestamps are server-owned and cannot be
 * injected via the request body (prevents mass-assignment).
 */
@Data
public class CreateFeeRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    // Optional: resolved to the current academic year of the school when omitted.
    private String academicYear;

    @NotBlank(message = "studentId is required")
    private String studentId;

    @NotNull(message = "fee type is required")
    private FeeType type;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "discount cannot be negative")
    private BigDecimal discount;

    private LocalDate dueDate;
}
