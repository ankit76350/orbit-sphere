package com.orbitastra.backend.models.new_new.plans.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.billing.enums.SubscriptionInvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "subscription_invoices")
@CompoundIndexes({
        @CompoundIndex(
                name = "subscription_invoice_no_uniq",
                def = "{'invoiceNo': 1}",
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
    private String invoiceNo;

    // Example: "67aa1a44dc3f7d0011223344"
    private String schoolSubscriptionDocsId;

    // Example: 2026-04-01
    private LocalDate billingPeriodStart;

    // Example: 2027-03-31
    private LocalDate billingPeriodEnd;

    // Example: 2026-04-01
    private LocalDate issueDate;

    // Example: 2026-04-15
    private LocalDate dueDate;

    // Example: SubscriptionInvoiceStatus.ISSUED
    private SubscriptionInvoiceStatus status;

    // Example: "INR"
    private String currencyCode;

    // Example: 45000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subTotal;

    // Example: 8100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // Example: 53100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    // Example: 25000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paidAmount;

    // Example: 28100.00
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
