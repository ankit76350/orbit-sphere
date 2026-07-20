package com.orbitastra.backend.models.undone.feeengine;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiMandate {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

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
