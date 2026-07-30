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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

    // Example: "67ab10e8dc3f7d0055443322"
    private String subscriptionInvoiceDocsId;

    // Example: "67ab19f4dc3f7d0077889900"
    private String subscriptionPaymentDocsId;

    // Example: 2
    private Integer attemptNo;

    // Example: PaymentAttemptStatus.FAILED
    private PaymentAttemptStatus status;

    // Example: SubscriptionPaymentMethod.CARD
    private SubscriptionPaymentMethod paymentMethod;

    // Example: 53100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Example: "INR"
    private String currencyCode;

    // Example: "RAZORPAY"
    private String gatewayProvider;

    // Example: "subscription-invoice-67ab10e8-attempt-2"
    private String idempotencyKey;

    // Example: "pay_R7pc3Q1j9"
    private String gatewayAttemptReference;

    // Example: "CARD_DECLINED"
    private String failureCode;

    // Example: "The issuing bank declined the payment."
    private String failureMessage;

    // Example: 2026-04-10T09:10:00Z
    private Instant attemptedAt;

    // Example: 2026-04-10T09:10:07Z
    private Instant completedAt;
}
