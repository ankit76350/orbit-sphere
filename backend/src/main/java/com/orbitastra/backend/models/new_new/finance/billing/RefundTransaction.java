package com.orbitastra.backend.models.new_new.finance.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.PaymentMode;
import com.orbitastra.backend.models.new_new.finance.enums.RefundReason;
import com.orbitastra.backend.models.new_new.finance.enums.RefundStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Money going back out to a payer, with a named approver behind it.
 *
 * <p>Money leaving the school is the highest-risk thing this module does, so a
 * refund is always its own record with its own approval. It is never done by
 * lowering an invoice total or editing a payment amount.
 *
 * <p>The person who raises a refund must not be the person who approves it. The
 * service enforces that, using {@code requestedByDocsId} and
 * {@code approvedByDocsId}.
 *
 * <p>A refund always points at the FeePayment the money originally came in on,
 * so it can never send back more than was actually received.
 * {@code FeePayment.refundedAmount} is the running total that keeps this in
 * check.
 *
 * <p>{@code refundMode} may differ from how the money arrived. A cash payment is
 * often refunded by bank transfer, and a gateway payment may be pushed back into
 * the student's wallet instead of to the card.
 */
@Document(collection = "refund_transactions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_refund_no_uniq",
                def = "{'schoolId': 1, 'refundNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "gateway_refund_reference_uniq",
                def = "{'gatewayRefundReference': 1}",
                unique = true,
                partialFilter = "{'gatewayRefundReference': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_payment_refund_status_idx",
                def = "{'schoolId': 1, 'feePaymentDocsId': 1, 'status': 1, 'requestedAt': -1}"),
        @CompoundIndex(
                name = "school_refund_status_requested_idx",
                def = "{'schoolId': 1, 'status': 1, 'requestedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction extends SchoolBase {

    // School-scoped number from NumberSequence type FEE_REFUND.
    // Example: "REF/2026/000012"
    @NotBlank
    private String refundNo;

    // Links to FeePayment.id the money originally came in on.
    // Example: "67ae1122dc3f7d0011223344"
    @NotBlank
    private String feePaymentDocsId;

    // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // Links to FeeInvoice.id when the refund is tied to one bill.
    // Example: "67ae2233dc3f7d0022334455"
    private String feeInvoiceDocsId;

    // Money being sent back. Example: 2500.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: RefundReason.SERVICE_NOT_AVAILED
    @NotNull
    private RefundReason reasonCode;

    // Detail behind the reason code.
    // Example: "Transport was not used after the family moved house."
    @NotBlank
    private String reasonDetail;

    // Example: RefundStatus.COMPLETED
    @NotNull
    @Builder.Default
    private RefundStatus status = RefundStatus.REQUESTED;

    // How the money is being sent back. Example: PaymentMode.BANK_TRANSFER
    @NotNull
    private PaymentMode refundMode;

    // Wallet the money is being put into, when the refund goes to a wallet.
    // Example: "67ad7788dc3f7d0077889900"
    private String storedValueAccountDocsId;

    // Bank account the money is going out from.
    // Example: "67ad8899dc3f7d0088990011"
    private String bankAccountDocsId;

    // Provider's own refund id. Example: "rfnd_R8qd4T2k1"
    private String gatewayRefundReference;

    // Links to the staff identity that raised the refund.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String requestedByDocsId;

    // Example: 2026-05-02T06:20:00Z
    @NotNull
    private Instant requestedAt;

    // Links to the staff identity that approved it. Must be a different person
    // from the one who raised it, which the service checks.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: 2026-05-03T04:45:00Z
    private Instant approvedAt;

    // Why the refund was turned down.
    // Example: "Transport was used for two months, so a full refund is not due."
    private String rejectionReason;

    // When the payer actually got the money. Example: 2026-05-05T09:00:00Z
    private Instant completedAt;

    // Why sending the money back failed.
    // Example: "Bank rejected the transfer because the account number was wrong."
    private String failureReason;

    // Links to JournalEntry.id posted for this refund.
    // Example: "67ae5566dc3f7d0055667788"
    private String journalEntryDocsId;
}
