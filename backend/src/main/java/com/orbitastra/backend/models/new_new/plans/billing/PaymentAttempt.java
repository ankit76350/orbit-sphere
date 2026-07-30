package com.orbitastra.backend.models.new_new.plans.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.billing.enums.PaymentAttemptStatus;
import com.orbitastra.backend.models.new_new.plans.billing.enums.SubscriptionPaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One gateway attempt to collect a SubscriptionInvoice payment.
 *
 * <p>Multiple attempts may exist for an invoice. The unique attempt number
 * orders retries inside the school and invoice scope. {@code idempotencyKey}
 * prevents the same provider operation from being created twice.
 * {@code subscriptionPaymentDocsId} optionally links to the resulting
 * SubscriptionPayment aggregate.
 */
@Document(collection = "subscription_payment_attempts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_invoice_attempt_no_uniq",
                def = "{'schoolId': 1, 'subscriptionInvoiceDocsId': 1, 'attemptNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "gateway_attempt_idempotency_uniq",
                def = "{'gatewayProvider': 1, 'idempotencyKey': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_payment_attempt_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'attemptedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt extends SchoolBase {

    // Links to SubscriptionInvoice.id. Example: "67ab10e8dc3f7d0055443322"
    @NotBlank
    private String subscriptionInvoiceDocsId;

    // Links to SubscriptionPayment.id when one exists. Example: "67ab19f4dc3f7d0077889900"
    private String subscriptionPaymentDocsId;

    // Example: 2
    @NotNull
    private Integer attemptNo;

    // Example: PaymentAttemptStatus.FAILED
    @NotNull
    private PaymentAttemptStatus status;

    // Example: SubscriptionPaymentMethod.CARD
    @NotNull
    private SubscriptionPaymentMethod paymentMethod;

    // Example: 53100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: "RAZORPAY"
    @NotBlank
    private String gatewayProvider;

    // Example: "subscription-invoice-67ab10e8-attempt-2"
    @NotBlank
    private String idempotencyKey;

    // Example: "pay_R7pc3Q1j9"
    private String gatewayAttemptReference;

    // Example: "CARD_DECLINED"
    private String failureCode;

    // Example: "The issuing bank declined the payment."
    private String failureMessage;

    // Example: 2026-04-10T09:10:00Z
    @NotNull
    private Instant attemptedAt;

    // Example: 2026-04-10T09:10:07Z
    private Instant completedAt;
}
