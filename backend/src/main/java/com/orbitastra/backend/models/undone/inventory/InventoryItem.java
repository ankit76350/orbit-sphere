package com.orbitastra.backend.models.undone.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.inventory.enums.InventoryItemStatus;
import com.orbitastra.backend.models.undone.inventory.enums.InventoryUnit;



@Document(collection = "inventory_items")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends SchoolBase {

    /**
     * Stationery Category
     */
    private String categoryDocsId;

    /**
     * Ball Pen
     * Football
     * Beaker
     */
    private String name;

    /**
     * Optional description.
     */
    private String description;

    /**
     * PEN001
     * BALL001
     * TT001
     */
    @Indexed(unique = true)
    private String itemCode;


    /**
     * Optional image.
     */
    private String imageUrl;

    /**
     * Current stock.
     */
    @Builder.Default
    private Integer stockQuantity = 0;

    /**
     * Minimum stock before alert.
     */
    @Builder.Default
    private Integer minimumStock = 0;



    /**
     * Suggested quantity to purchase.
     */
    @Builder.Default
    private Integer reorderQuantity = 0;

    /**
     * Purchase price of one unit.
     */
    private BigDecimal unitPurchasePrice;


    /**
     * Sale price per unit.
     */
    private BigDecimal unitSalePrice;

    /**
     * Unit of measurement.
     */
    private InventoryUnit unit;

 
    /**
     * Store Room A
     * Rack A-02
     * Lab Shelf-4
     */
    private String storageLocationCode;

    /**
     * Active / Out Of Stock / Discontinued
     */
    @Builder.Default
    private InventoryItemStatus status = InventoryItemStatus.ACTIVE;

}
