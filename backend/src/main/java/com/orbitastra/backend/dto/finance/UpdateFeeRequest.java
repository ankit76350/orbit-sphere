package com.orbitastra.backend.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.orbitastra.backend.models.finance.enums.FeeType;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Partial-update payload for a fee invoice (PATCH). Every field is optional; only
 * non-null fields are applied. {@code paidAmount} and {@code status} are derived
 * from payment records and are intentionally not editable here. {@code academicYear}
 * is accepted only so the service can reject an attempt to change it.
 */
@Data
public class UpdateFeeRequest {

    private String academicYear;

    private FeeType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "discount cannot be negative")
    private BigDecimal discount;

    private LocalDate dueDate;
}
