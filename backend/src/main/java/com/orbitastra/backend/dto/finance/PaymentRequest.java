package com.orbitastra.backend.dto.finance;

import java.math.BigDecimal;

import com.orbitastra.backend.models.finance.enums.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for collecting a payment against a fee invoice. The single request
 * shape for every payment mode (cash, wallet, online, cheque).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "payment amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "paymentMode is required")
    private PaymentMode paymentMode;

    private String remarks;

    private String collectedByDocsId;
}
