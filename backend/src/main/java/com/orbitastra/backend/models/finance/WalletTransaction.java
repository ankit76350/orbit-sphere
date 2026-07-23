package com.orbitastra.backend.models.finance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;
import com.orbitastra.backend.models.finance.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "wallet_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction extends SchoolDocs {

    private String studentId;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    @Indexed(unique = true)
    private String referenceNo;

    private String remarks;

    private LocalDateTime transactionDate;
}
