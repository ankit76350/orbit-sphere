package com.orbitastra.backend.models.undone.feeengine;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.feeengine.enums.MandateStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A UPI AutoPay recurring mandate authorising automatic fee debits from a
 * parent's account for a student, up to a capped amount.
 */
@Document(collection = "upi_mandates")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UpiMandate extends BaseDocument {

    private String studentId;

    private String parentId; // references Guardian.id

    // Maximum amount authorised per debit under the mandate.
    private BigDecimal amount;

    @Builder.Default
    private MandateStatus status = MandateStatus.ACTIVE;

    private LocalDate nextDebitDate;

    // UPI mandate reference (UMN) returned by the gateway.
    private String mandateRef;
}
