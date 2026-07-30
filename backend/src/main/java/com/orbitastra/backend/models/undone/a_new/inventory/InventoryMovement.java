package com.orbitastra.backend.models.undone.a_new.inventory;

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

@Document(collection = "inventory_movements")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_movement_no_uniq",
                def = "{'tenantId':1,'movementNo':1}", unique = true),
        @CompoundIndex(name = "tenant_balance_sequence_uniq",
                def = "{'tenantId':1,'stockBalanceDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_item_occurred_idx",
                def = "{'tenantId':1,'inventoryItemDocsId':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovement extends CampusScopedDocument {

    private String movementNo;
    private String stockBalanceDocsId;
    private Long sequenceNo;
    private String inventoryItemDocsId;
    private String movementType;
    private BigDecimal quantity;
    private BigDecimal quantityAfter;
    private String unitCode;
    private String sourceLocationDocsId;
    private String targetLocationDocsId;
    private String counterpartyType;
    private String counterpartyDocsId;
    private String referenceType;
    private String referenceDocsId;
    private String idempotencyKey;
    private Instant occurredAt;
    private String recordedByDocsId;
    private String reversalOfMovementDocsId;
}
