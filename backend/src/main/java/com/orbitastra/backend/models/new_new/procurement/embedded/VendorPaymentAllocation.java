package com.orbitastra.backend.models.new_new.procurement.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How much of one payment went against one of the vendor's bills.
 *
 * <p>A school pays a vendor once a month and that one transfer settles four bills. Without
 * this, the payment carries a single amount and nobody can say which bills it cleared —
 * which means nobody can say which bills are still open, which is the only question the
 * payables list exists to answer.
 *
 * <p>This is the same shape as PaymentAllocation on the money-in side, with one deliberate
 * difference: it is **embedded** in VendorPayment rather than a collection of its own. A
 * vendor payment settles a handful of bills and they are always read with the payment. On
 * the fees side an allocation is queried constantly from the invoice direction, by hundreds
 * of parents at once, and needs to be a document. Here the query goes the other way, so an
 * index into this array does the job.
 *
 * <p>Part payment is the ordinary case, not an exception. A school short of cash in July
 * pays half of what it owes the rice supplier and settles the rest in August. So the amount
 * here is not required to equal the bill total, and the bill's own outstanding figure is
 * what says how much is left.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentAllocation {

    // Links to SupplierInvoice.id this part of the payment settles.
    // Example: "67bd1128dc3f7d0077889900"
    @NotBlank
    private String supplierInvoiceDocsId;

    // The school's own number for that bill, copied in so a payment advice reads
    // without loading every invoice. Example: "SINV/2026/000112"
    private String supplierInvoiceNo;

    // How much of this payment went against that bill. May be less than the bill
    // total. Example: 11377.50
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;
}
