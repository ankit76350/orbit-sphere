package com.orbitastra.backend.models.undone.a_new.mess;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "kitchen_stock_transactions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_kitchen_transaction_no_uniq",
                def = "{'tenantId':1,'transactionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_kitchen_item_time_idx",
                def = "{'tenantId':1,'kitchenStockItemDocsId':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenStockTransaction extends CampusScopedDocument {

    private String transactionNo;
    private String kitchenStockItemDocsId;
    private String transactionType;
    private BigDecimal quantity;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private String unitCode;
    private String sourceType;
    private String sourceDocsId;
    private String batchNo;
    private Instant occurredAt;
    private String recordedByDocsId;
    private String reason;
}
