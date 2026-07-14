package com.orbitastra.backend.dto.finance;

import java.math.BigDecimal;

import com.orbitastra.backend.models.finance.enums.PaymentMode;

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
    private BigDecimal amount;

    private PaymentMode paymentMode;

    private String remarks;

    private String collectedBy;
}
