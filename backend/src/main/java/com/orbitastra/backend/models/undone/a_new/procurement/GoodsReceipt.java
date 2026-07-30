package com.orbitastra.backend.models.undone.a_new.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "goods_receipts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_receipt_no_uniq",
                def = "{'tenantId':1,'receiptNo':1}", unique = true),
        @CompoundIndex(name = "tenant_po_receipt_time_idx",
                def = "{'tenantId':1,'purchaseOrderDocsId':1,'receivedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt extends CampusScopedDocument {

    public enum InspectionStatus {
        PENDING,
        ACCEPTED,
        PARTIALLY_ACCEPTED,
        REJECTED
    }

    private String receiptNo;
    private String purchaseOrderDocsId;
    private String vendorDocsId;
    private Instant receivedAt;
    private String receivedByDocsId;
    private InspectionStatus inspectionStatus;
    private String deliveryNoteNo;
    private String evidenceDocumentDocsId;

    @Builder.Default
    private List<ReceiptLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptLine {
        private Integer purchaseOrderLineNo;
        private BigDecimal receivedQuantity;
        private BigDecimal acceptedQuantity;
        private BigDecimal rejectedQuantity;
        private String rejectionReason;
        private String inventoryBatchDocsId;
    }
}
