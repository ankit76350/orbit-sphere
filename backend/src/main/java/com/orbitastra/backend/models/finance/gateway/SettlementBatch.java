package com.orbitastra.backend.models.new_new.finance.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.GatewayProvider;
import com.orbitastra.backend.models.new_new.finance.enums.SettlementStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One payout from a payment gateway into the school's bank account.
 *
 * <p>A gateway does not send each fee payment to the bank one by one. It collects
 * many of them, keeps its own charge, and transfers the rest as a single amount a
 * day or two later. This record explains that single bank credit.
 *
 * <p>Without it, the bank statement can never be matched: the school collected
 * 100000 in fees but only 98200 arrived, and the missing 1800 is the gateway's
 * charge. {@code grossAmount}, {@code feeAmount} and {@code netAmount} are what
 * make that difference explainable instead of a mystery.
 *
 * <p>Each FeePayment in the batch points back at it through
 * {@code FeePayment.settlementBatchDocsId}, so the payout can be broken down to
 * the individual receipts inside it.
 */
@Document(collection = "settlement_batches")
@CompoundIndexes({
        @CompoundIndex(
                name = "provider_settlement_reference_uniq",
                def = "{'gatewayProvider': 1, 'providerSettlementReference': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_settlement_status_date_idx",
                def = "{'schoolId': 1, 'status': 1, 'settlementDate': -1}"),
        @CompoundIndex(
                name = "school_gateway_settlement_date_idx",
                def = "{'schoolId': 1, 'paymentGatewayDocsId': 1, 'settlementDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatch extends SchoolBase {

    // Links to PaymentGateway.id that made the payout.
    // Example: "67ad5566dc3f7d0055667788"
    @NotBlank
    private String paymentGatewayDocsId;

    // Provider copied in so the unique index works without a lookup.
    // Example: GatewayProvider.RAZORPAY
    @NotNull
    private GatewayProvider gatewayProvider;

    // The provider's own payout id. Example: "setl_R8bQ92mL4"
    @NotBlank
    private String providerSettlementReference;

    // Date the provider says the payout was made. Example: 2026-04-10
    @NotNull
    private LocalDate settlementDate;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Fees collected in this batch, before the provider's charge.
    // Example: 100000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal grossAmount;

    // Charge the provider kept. Example: 1800.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    // Tax on the provider's charge. Example: 324.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // Refunds taken out of this payout. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    // What should actually reach the bank. Example: 97876.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal netAmount;

    // Payments the provider says are in this batch. Example: 42
    private Integer transactionCount;

    // Payments the school has managed to link to it. Example: 42
    @NotNull
    @Builder.Default
    private Integer matchedTransactionCount = 0;

    // Example: SettlementStatus.RECONCILED
    @NotNull
    @Builder.Default
    private SettlementStatus status = SettlementStatus.EXPECTED;

    // Bank account the payout went into. Example: "67ad8899dc3f7d0088990011"
    private String bankAccountDocsId;

    // Reference the bank showed for the credit. Example: "NEFT/RZP/9137524608"
    private String bankReference;

    // Links to DocumentRecord.id for the provider's payout statement.
    // Example: "67ad3344dc3f7d0066778899"
    private String statementDocumentDocsId;

    // When the payout details were loaded into the school's records.
    // Example: 2026-04-11T03:30:00Z
    private Instant importedAt;

    // What is being queried with the provider.
    // Example: "Payout is 500 short of the expected amount."
    private String remarks;
}
