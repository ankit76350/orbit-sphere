package com.orbitastra.backend.models.new_new.inventory;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * How much of one item is in one store, right now.
 *
 * <p>One row per item per store. This is what makes "50 kg of rice in the kitchen and 20
 * in the main store" sayable, and it is why quantity does not live on the item.
 *
 * <p>It is a running total kept for speed, and the movements remain the real record. The
 * number here must always be rebuildable by adding up StockMovement for the same item and
 * store, and a recompute job has to exist for the day a bulk operation half-fails. Exactly
 * the rule FeeInvoice follows for its payment totals.
 *
 * <p>{@code quantityOnHand} is what is physically there. {@code quantityReserved} is the
 * part of it already promised to somebody, such as provisions set aside for tomorrow's
 * lunch. {@code quantityAvailable} is the difference and is what a new issue may draw on.
 *
 * <p>Reserved quantity exists because a store that only knows its total will promise the
 * same sack of rice to the kitchen and the hostel on the same morning. The two find out
 * when one of them goes to collect it.
 *
 * <p>{@code quantityAvailable} is stored rather than worked out on every read, because
 * "what can I issue" is the question a store screen asks constantly and it has to be
 * indexable. It must always equal on-hand minus reserved.
 *
 * <p>Stock is never negative. An issue larger than what is available is refused rather
 * than allowed to go below zero, because a negative balance is a store that has lost
 * track and quietly kept going.
 *
 * <p>The service checks that the balance matches its movements, that available equals
 * on-hand minus reserved, that nothing goes below zero, and that a row is not deleted
 * while it holds stock.
 */
@Document(collection = "stock_balances")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_stock_balance_uniq",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1, 'inventoryStoreDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_stock_balance_store_idx",
                def = "{'schoolId': 1, 'inventoryStoreDocsId': 1, 'quantityAvailable': 1}"),
        @CompoundIndex(
                name = "school_stock_balance_item_idx",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalance extends SchoolBase {

    // Links to InventoryItem.id. Example: "67bc1124dc3f7d0033445566"
    @NotBlank
    private String inventoryItemDocsId;

    // Links to InventoryStore.id. Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String inventoryStoreDocsId;

    // What is physically in the store. Never negative. Example: 50.000
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    // The part already promised to somebody and not yet collected. Example: 12.000
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    // On hand minus reserved. What a new issue may draw on. Example: 38.000
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    // When stock last moved in or out of this store for this item, so a store keeper
    // can see what has been sitting untouched. Example: 2026-08-19T04:10:00Z
    private Instant lastMovementAt;

    // When somebody last physically counted this and agreed the number.
    // Example: 2026-07-31T11:00:00Z
    private Instant lastCountedAt;
}
