package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "refund_transactions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_refund_no_uniq",
                def = "{'tenantId':1,'refundNo':1}", unique = true),
        @CompoundIndex(name = "tenant_payment_refund_status_idx",
                def = "{'tenantId':1,'paymentTransactionDocsId':1,'status':1,'requestedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction extends TenantScopedDocument {

    private String refundNo;
    private String paymentTransactionDocsId;
    private String studentDocsId;
    private BigDecimal amount;
    private String currencyCode;
    private String reasonCode;
    private ApprovalState status;
    private String requestedByDocsId;
    private Instant requestedAt;
    private String approvedByDocsId;
    private String providerRefundReference;
    private Instant completedAt;
    private String journalEntryDocsId;
}
