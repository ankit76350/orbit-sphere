package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "payment_transactions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_payment_no_uniq",
                def = "{'tenantId':1,'paymentNo':1}", unique = true),
        @CompoundIndex(name = "tenant_provider_reference_uniq",
                def = "{'tenantId':1,'providerKey':1,'providerPaymentReference':1}", unique = true,
                partialFilter = "{'providerPaymentReference':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_student_payment_time_idx",
                def = "{'tenantId':1,'studentDocsId':1,'paidAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction extends AcademicScopedDocument {

    private String paymentNo;
    private String receiptNo;
    private String studentDocsId;
    private String payerType;
    private String payerDocsId;
    private BigDecimal amount;
    private String currencyCode;
    private String paymentMethod;
    private String providerKey;
    private String providerPaymentReference;
    private String idempotencyKey;
    private String status;
    private Instant initiatedAt;
    private Instant paidAt;
    private String collectedByDocsId;
    private String bankAccountDocsId;
    private String storedValueAccountDocsId;
    private String journalEntryDocsId;
    private String receiptDocumentDocsId;
}
