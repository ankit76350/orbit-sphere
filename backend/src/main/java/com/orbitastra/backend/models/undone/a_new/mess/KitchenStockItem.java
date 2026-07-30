package com.orbitastra.backend.models.undone.a_new.mess;

import java.math.BigDecimal;
import java.time.LocalDate;
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

@Document(collection = "kitchen_stock_items")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_kitchen_item_code_uniq",
                def = "{'tenantId':1,'itemCode':1}", unique = true),
        @CompoundIndex(name = "tenant_kitchen_reorder_expiry_idx",
                def = "{'tenantId':1,'active':1,'nextExpiryDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenStockItem extends CampusScopedDocument {

    private String itemCode;
    private String name;
    private String categoryCode;
    private String baseUnitCode;
    private BigDecimal quantityOnHand;
    private BigDecimal reorderLevel;
    private LocalDate nextExpiryDate;
    private String storageLocationDocsId;
    private Boolean active;

    @Builder.Default
    private List<String> allergenCodes = new ArrayList<>();
}
