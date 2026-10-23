package com.orbitastra.backend.models.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One lot of a perishable item, with its own expiry date and its own quantity left.
 *
 * <p>Only used for items whose {@code requiresBatchTracking} is true, which is always the
 * case for PERISHABLE ones. Chalk does not need this; milk does.
 *
 * <p>The reason it exists is that a single quantity cannot answer the question a kitchen
 * actually has. "We have 40 litres of milk" is useless if 25 of them go off tomorrow.
 * Batches make the school able to use the oldest first and to throw away only what has
 * actually expired.
 *
 * <p>{@code quantityRemaining} on the batches of one item in one store must add up to
 * that StockBalance's {@code quantityOnHand}. The balance stays the fast answer; the
 * batches say what it is made of.
 *
 * <p>{@code batchNumber} is the supplier's own lot number where there is one. It matters
 * because a recall happens by lot: when a dairy withdraws a batch, the school needs the
 * list of what it has and where it went, and the batch number is the only way to find it.
 *
 * <p>Unlike a vaccination card, this batch number is worth capturing. The school is the
 * one receiving the goods, it is printed on the carton in front of the store keeper, and
 * it is read at the moment of receipt rather than copied off a faded card years later.
 *
 * <p>An expired batch is not deleted. Its remaining quantity is written off through a
 * WASTAGE movement, which leaves the loss on the record instead of making it disappear.
 *
 * <p>The service checks that the batches of an item in a store add up to its balance,
 * that the oldest unexpired batch is drawn on first, and that an expired batch is never
 * issued.
 */
@Document(collection = "stock_batches")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_stock_batch_uniq",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1, 'inventoryStoreDocsId': 1, 'batchNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_stock_batch_expiry_idx",
                def = "{'schoolId': 1, 'expiryDate': 1, 'quantityRemaining': -1}"),
        @CompoundIndex(
                name = "school_stock_batch_oldest_idx",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1, 'inventoryStoreDocsId': 1, 'expiryDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatch extends SchoolBase {

    // Links to InventoryItem.id. Example: "67bc1126dc3f7d0055667788"
    @NotBlank
    private String inventoryItemDocsId;

    // Links to InventoryStore.id. A batch split across two stores is two rows.
    // Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String inventoryStoreDocsId;

    // The supplier's lot number, or one the school makes up when there is none.
    // What a recall is traced by. Example: "AMUL-240819-B7"
    @NotBlank
    private String batchNumber;

    // How much of this batch arrived. Example: 40.000
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityReceived;

    // How much of it is left. Adds up with the other batches to the store balance.
    // Example: 25.000
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityRemaining = BigDecimal.ZERO;

    // The day it arrived. Example: 2026-08-19
    @NotNull
    private LocalDate receivedOn;

    // The day it goes off. What the oldest-first rule sorts on. Example: 2026-08-22
    private LocalDate expiryDate;

    // What was paid per unit for this particular lot. Kept per batch because the same
    // item bought twice in a month rarely costs the same, and this is what stock on
    // hand is actually worth. Example: 58.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitRate;

    // Who it came from. Example: "Amul distributor, Andheri"
    private String supplierName;

    // Example: "Two cartons dented on arrival; noted on the delivery challan."
    private String remarks;
}
