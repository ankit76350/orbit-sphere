package com.orbitastra.backend.models.plans.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.plans.billing.enums.SubscriptionPaymentMethod;
import com.orbitastra.backend.models.plans.billing.enums.SubscriptionPaymentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Financial payment aggregate received or attempted against one subscription
 * invoice.
 *
 * <p>{@code schoolSubscriptionDocsId} and
 * {@code subscriptionInvoiceDocsId} link to the contracted subscription and
 * invoice. Gateway references are external provider identifiers and are unique
 * per provider where present. Offline methods may leave gateway fields null.
 */
@Document(collection = "subscription_payments")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_subscription_payment_no_uniq",
                def = "{'schoolId': 1, 'paymentNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "gateway_payment_reference_uniq",
                def = "{'gatewayProvider': 1, 'gatewayPaymentReference': 1}",
                unique = true,
                partialFilter = "{'gatewayPaymentReference': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_invoice_payment_status_idx",
                def = "{'schoolId': 1, 'subscriptionInvoiceDocsId': 1, 'status': 1, 'receivedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment extends SchoolBase {

    // Example: "SPAY/2026/000001"
    @NotBlank
    private String paymentNo;

    // Links to SchoolSubscription.id. Example: "67aa1a44dc3f7d0011223344"
    @NotBlank
    private String schoolSubscriptionDocsId;

    // Links to SubscriptionInvoice.id. Example: "67ab10e8dc3f7d0055443322"
    @NotBlank
    private String subscriptionInvoiceDocsId;

    // Example: SubscriptionPaymentStatus.SUCCEEDED
    @NotNull
    private SubscriptionPaymentStatus status;

    // Example: SubscriptionPaymentMethod.UPI
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
    private String gatewayProvider;

    // Example: "pay_R7pc3Q1j9"
    private String gatewayPaymentReference;

    // Example: "order_R7pY8k2m4"
    private String gatewayOrderReference;

    // Example: "bank_reference_9137524608"
    private String settlementReference;

    // Example: 2026-04-10T09:15:00Z
    private Instant receivedAt;

    // Example: 2026-04-12T05:30:00Z
    private Instant settledAt;

    // Example: "Payment reversed by the gateway."
    private String failureReason;
}
