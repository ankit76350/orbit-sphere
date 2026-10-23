package com.orbitastra.backend.models.plans.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.plans.billing.enums.SubscriptionInvoiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * SaaS invoice issued to one school for one SchoolSubscription billing period.
 *
 * <p>{@code schoolSubscriptionDocsId} links to SchoolSubscription.id. Payments
 * link back through {@code SubscriptionPayment.subscriptionInvoiceDocsId}.
 * Monetary values use MongoDB Decimal128 and the stored currency code applies to
 * every amount in this document.
 *
 * <p>Invoice totals and date ordering are calculated and validated by the
 * billing service. Issued invoices are financial records and should be voided,
 * not rewritten or physically deleted.
 */
@Document(collection = "subscription_invoices")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_subscription_invoice_no_uniq",
                def = "{'schoolId': 1, 'invoiceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_subscription_period_invoice_uniq",
                def = "{'schoolId': 1, 'schoolSubscriptionDocsId': 1, 'billingPeriodStart': 1, 'billingPeriodEnd': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_invoice_status_due_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionInvoice extends SchoolBase {

    // Example: "SINV/2026/000001"
    @NotBlank
    private String invoiceNo;

    // Links to SchoolSubscription.id. Example: "67aa1a44dc3f7d0011223344"
    @NotBlank
    private String schoolSubscriptionDocsId;

    // Example: 2026-04-01
    @NotNull
    private LocalDate billingPeriodStart;

    // Example: 2027-03-31
    @NotNull
    private LocalDate billingPeriodEnd;

    // Example: 2026-04-01
    @NotNull
    private LocalDate issueDate;

    // Example: 2026-04-15
    @NotNull
    private LocalDate dueDate;

    // Example: SubscriptionInvoiceStatus.ISSUED
    @NotNull
    private SubscriptionInvoiceStatus status;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: 45000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subTotal;

    // Example: 8100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // Example: 53100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    // Example: 25000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paidAmount;

    // Example: 28100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal outstandingAmount;

    // Example: "invoice_R7pQ4m2"
    private String gatewayInvoiceReference;

    // Example: 2026-04-10T09:15:00Z
    private Instant paidAt;

    // Example: 2026-04-02T10:30:00Z
    private Instant issuedAt;

    // Example: 2026-04-20T00:00:00Z
    private Instant voidedAt;

    // Example: "Duplicate invoice generated during billing retry."
    private String voidReason;
}
