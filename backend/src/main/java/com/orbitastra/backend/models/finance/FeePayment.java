package com.orbitastra.backend.models.finance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcadmicStudentSchoolBase;
import com.orbitastra.backend.models.finance.enums.PaymentMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "fee_payments")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeePayment extends AcadmicStudentSchoolBase {

    // The Fee (invoice) this payment is collected against
    private String feeDocsId;

    @Indexed(unique = true)
    private String receiptNo;

    private BigDecimal amount;

    private PaymentMode paymentMode;

    private LocalDateTime paidOn;

    private String collectedByDocsId;

    private String remarks;
}
