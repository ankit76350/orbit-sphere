package com.orbitastra.backend.models.new_new.finance.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.FeePaymentStatus;
import com.orbitastra.backend.models.new_new.finance.enums.GatewayProvider;
import com.orbitastra.backend.models.new_new.finance.enums.PayerType;
import com.orbitastra.backend.models.new_new.finance.enums.PaymentMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One amount of money received from a family, however it arrived.
 *
 * <p>A payment is deliberately not tied to a single invoice. A parent pays a
 * round amount, and PaymentAllocation records how that amount was spread across
 * the bills. That is what makes a part payment, an advance payment, and one
 * payment clearing three of a student's invoices all work through the same
 * record.
 *
 * <p>A payment does belong to one student, because a receipt is always made out
 * for one student. A parent paying for two children gets two payments and two
 * receipts.
 *
 * <p>{@code unallocatedAmount} is money received but not yet placed against any
 * invoice. It is real money the school is holding, so it must never be quietly
 * dropped; it either gets allocated later, moved to the student's wallet, or
 * refunded.
 *
 * <p>{@code idempotencyKey} stops the same payment being recorded twice when a
 * gateway retries a callback or a cashier double-clicks. The gateway reference is
 * unique per provider for the same reason.
 *
 * <p>{@code receiptNo} is only filled in once the payment has actually succeeded,
 * so a failed attempt never burns a receipt number. Both numbers come from
 * NumberSequence and are unique inside one school, not across the database.
 */
@Document(collection = "fee_payments")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_fee_payment_no_uniq",
                def = "{'schoolId': 1, 'paymentNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_fee_receipt_no_uniq",
                def = "{'schoolId': 1, 'receiptNo': 1}",
                unique = true,
                partialFilter = "{'receiptNo': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_payment_idempotency_uniq",
                def = "{'schoolId': 1, 'idempotencyKey': 1}",
                unique = true,
                partialFilter = "{'idempotencyKey': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "gateway_fee_payment_reference_uniq",
                def = "{'gatewayProvider': 1, 'gatewayPaymentReference': 1}",
                unique = true,
                partialFilter = "{'gatewayPaymentReference': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_year_student_payment_time_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'paidAt': -1}"),
        @CompoundIndex(
                name = "school_payment_status_collected_idx",
                def = "{'schoolId': 1, 'status': 1, 'paidAt': -1}"),
        @CompoundIndex(
                name = "school_payment_unallocated_idx",
                def = "{'schoolId': 1, 'status': 1, 'unallocatedAmount': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeePayment extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type FEE_PAYMENT.
    // Example: "PAY/2026/000914"
    @NotBlank
    private String paymentNo;

    // School-scoped number from NumberSequence type FEE_RECEIPT, filled in only
    // once the payment succeeds. Example: "RCP/2026/000871"
    private String receiptNo;

    // Who handed over the money. Example: PayerType.GUARDIAN
    @NotNull
    private PayerType payerType;

    // Links to the payer named by payerType, usually Guardian.id.
    // Example: "67aa15d9dc3f7d0066666666"
    private String payerDocsId;

    // Name of the payer as given at the counter, kept for the receipt.
    // Example: "Priya Sharma"
    private String payerName;

    // Money received. Example: 5000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Part of the amount not yet placed against any invoice. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    // Part of the amount already sent back through a refund. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How the money arrived. Example: PaymentMode.UPI
    @NotNull
    private PaymentMode paymentMode;

    // Example: FeePaymentStatus.SUCCEEDED
    @NotNull
    @Builder.Default
    private FeePaymentStatus status = FeePaymentStatus.INITIATED;

    // Links to PaymentGateway.id used for an online payment.
    // Example: "67ad5566dc3f7d0055667788"
    private String paymentGatewayDocsId;

    // Provider copied in so the unique gateway index works without a lookup.
    // Example: GatewayProvider.RAZORPAY
    private GatewayProvider gatewayProvider;

    // Provider's own payment id. Example: "pay_R7pc3Q1j9"
    private String gatewayPaymentReference;

    // Provider's own order id. Example: "order_R7pY8k2m4"
    private String gatewayOrderReference;

    // Charge the gateway kept out of this payment. Example: 59.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal gatewayFeeAmount;

    // Cheque or draft number when one was handed over. Example: "004512"
    private String instrumentNo;

    // Date written on the cheque or draft. Example: 2026-04-08
    private LocalDate instrumentDate;

    // Bank the cheque or draft was drawn on. Example: "State Bank of India"
    private String instrumentBankName;

    // Stops the same payment being saved twice on a retry.
    // Example: "razorpay-pay_R7pc3Q1j9"
    private String idempotencyKey;

    // Links to UpiMandate.id when an AutoPay debit produced this payment.
    // Example: "67ad6677dc3f7d0066778899"
    private String upiMandateDocsId;

    // Wallet the money came from, or was topped up into.
    // Example: "67ad7788dc3f7d0077889900"
    private String storedValueAccountDocsId;

    // Bank account the money landed in. Example: "67ad8899dc3f7d0088990011"
    private String bankAccountDocsId;

    // Links to SettlementBatch.id once the gateway has paid this out to the bank.
    // Example: "67ad99aadc3f7d0099001122"
    private String settlementBatchDocsId;

    // Links to JournalEntry.id posted for this payment.
    // Example: "67adaabbdc3f7d0000112233"
    private String journalEntryDocsId;

    // Links to DocumentRecord.id for the printed receipt.
    // Example: "67adbbccdc3f7d0011223344"
    private String receiptDocumentDocsId;

    // When the payment was started, such as when the gateway page opened.
    // Example: 2026-04-08T10:12:00Z
    private Instant initiatedAt;

    // When the money was confirmed as received. Example: 2026-04-08T10:14:00Z
    private Instant paidAt;

    // When the gateway paid it out to the school's bank.
    // Example: 2026-04-10T05:30:00Z
    private Instant settledAt;

    // When the money was taken back, such as a bounced cheque.
    // Example: 2026-04-20T06:00:00Z
    private Instant reversedAt;

    // Links to the staff identity that collected an offline payment.
    // Example: "67aa15d9dc3f7d0044444444"
    private String collectedByDocsId;

    // Why the payment failed or was reversed.
    // Example: "Cheque returned by the bank for insufficient funds."
    private String failureReason;

    // Note typed in by the cashier. Example: "Paid at the front desk in cash."
    private String remarks;
}
