package com.orbitastra.backend.models.finance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.finance.enums.PaymentMode;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "fee_payments")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeePayment extends AcademicStudentSchoolBase {

    // The Fee (invoice) this payment is collected against
    private String feeDocsId;

    @Indexed(unique = true)
    private String receiptNo; // RPN/2026/05/2578 : -> RPN/YYYY/MM/DDSS make it unique in whole DB.

    private boolean collectedFromWallet;

    private String walletNo;

    private BigDecimal amount;

    private PaymentMode paymentMode;

    private LocalDateTime paidOn;

    private String collectedByDocsId;

    private String remarks;
}
