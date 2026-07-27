package com.orbitastra.backend.models.undone.a_working.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.inventory.enums.InventoryTransactionType;


@Document(collection = "inventory_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction extends SchoolBase {


    //     Example Flow
    // Purchase
    // Buy

    // 100 Pens

    // ↓

    // Transaction

    // PURCHASE

    // Quantity

    // 100

    // Before

    // 0

    // After

    // 100

    // ↓

    // InventoryItem

    // Stock = 100
    // Teacher takes pens
    // Mrs Sharma

    // takes

    // 20 Pens

    // ↓

    // Transaction

    // ISSUE_TO_TEACHER

    // 20

    // Before

    // 100

    // After

    // 80

    // ↓

    // Inventory

    // Stock = 80
    // Five pens damaged
    // DAMAGE

    // 5

    // Before

    // 80

    // After

    // 75

    // ↓

    // Inventory

    // 75

    /**
     * Inventory Item.
     */
    @Indexed
    private String itemDocsId;

    /**
     * PURCHASE
     * ISSUE
     * RETURN
     * DAMAGE
     */
    private InventoryTransactionType transactionType;

    /**
     * Quantity moved.
     */
    private Integer quantity;

    /**
     * Quantity before transaction.
     */
    private Integer stockBefore;

    /**
     * Quantity after transaction.
     */
    private Integer stockAfter;

    /**
     * Student/Teacher/Department receiving item.
     */
    private String issuedToDocsId;

    /**
     * Invoice No / Issue Slip / Adjustment Ref.
     */
    private String referenceNumber;

    /**
     * Reason.
     */
    private String remarks;
}