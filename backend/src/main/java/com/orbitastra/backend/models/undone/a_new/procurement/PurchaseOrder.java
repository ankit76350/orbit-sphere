package com.orbitastra.backend.models.undone.a_new.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "purchase_orders")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_po_no_uniq",
                def = "{'tenantId':1,'purchaseOrderNo':1}", unique = true),
        @CompoundIndex(name = "tenant_vendor_po_state_idx",
                def = "{'tenantId':1,'vendorDocsId':1,'state':1,'orderDate':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder extends CampusScopedDocument {

    private String purchaseOrderNo;
    private Integer revisionNo;
    private String procurementRequestDocsId;
    private String sourcingEventDocsId;
    private String awardedBidDocsId;
    private String vendorDocsId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String currencyCode;
    private BigDecimal subTotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private ApprovalState state;
    private String approvedByDocsId;
    private String contractDocsId;

    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLine {
        private Integer lineNo;
        private String itemDocsId;
        private String description;
        private BigDecimal orderedQuantity;
        private String unitCode;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal receivedQuantity;
    }
}
