package com.orbitastra.backend.models.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.finance.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String referenceNo;

    private String remarks;

    private LocalDateTime transactionDate;
}
