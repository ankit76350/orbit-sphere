package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "payment_allocations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_payment_invoice_allocation_uniq",
                def = "{'tenantId':1,'paymentTransactionDocsId':1,'receivableInvoiceDocsId':1,'allocationSequence':1}",
                unique = true),
        @CompoundIndex(name = "tenant_invoice_allocation_idx",
                def = "{'tenantId':1,'receivableInvoiceDocsId':1,'status':1,'allocatedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation extends TenantScopedDocument {

    private String paymentTransactionDocsId;
    private String receivableInvoiceDocsId;
    private Integer allocationSequence;
    private BigDecimal amount;
    private String currencyCode;
    private String status;
    private Instant allocatedAt;
    private String allocatedByDocsId;
    private String reversedByAllocationDocsId;
}
