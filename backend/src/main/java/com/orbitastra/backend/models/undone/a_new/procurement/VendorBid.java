package com.orbitastra.backend.models.undone.a_new.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "vendor_bids")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_event_vendor_revision_uniq",
                def = "{'tenantId':1,'sourcingEventDocsId':1,'vendorDocsId':1,'revisionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_event_bid_state_idx",
                def = "{'tenantId':1,'sourcingEventDocsId':1,'status':1,'submittedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VendorBid extends TenantScopedDocument {

    public enum BidStatus {
        DRAFT,
        SUBMITTED,
        WITHDRAWN,
        DISQUALIFIED,
        SHORTLISTED,
        AWARDED,
        UNSUCCESSFUL
    }

    private String sourcingEventDocsId;
    private String vendorDocsId;
    private Integer revisionNo;
    private BidStatus status;
    private String currencyCode;
    private BigDecimal totalAmount;
    private Instant submittedAt;
    private String commercialDocumentDocsId;
    private String technicalDocumentDocsId;
    private Integer evaluationScore;

    @Builder.Default
    private List<BidLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidLine {
        private Integer requestLineNo;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxAmount;
        private Integer deliveryDays;
    }
}
