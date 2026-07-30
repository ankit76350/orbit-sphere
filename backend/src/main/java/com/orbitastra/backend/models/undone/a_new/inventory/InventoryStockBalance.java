package com.orbitastra.backend.models.undone.a_new.inventory;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "inventory_stock_balances")
@CompoundIndex(name = "tenant_item_location_lot_uniq",
        def = "{'tenantId':1,'inventoryItemDocsId':1,'storageLocationDocsId':1,'lotNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockBalance extends CampusScopedDocument {

    private String inventoryItemDocsId;
    private String storageLocationDocsId;
    private String lotNo;
    private String unitCode;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
    private BigDecimal reorderLevel;
    private Long lastMovementSequence;
}
