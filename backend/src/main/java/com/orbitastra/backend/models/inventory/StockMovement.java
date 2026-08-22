package com.orbitastra.backend.models.new_new.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.inventory.enums.StockMovementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One change in the quantity of one item in one store.
 *
 * <p>This is the ledger, and it is the real record of everything in this package. Every
 * StockBalance is only a running total of these rows, and must always be rebuildable from
 * them.
 *
 * <p>Rows are added and never changed. A stock ledger that can be edited afterwards is
 * worth nothing: the whole point is that somebody can be asked where 20 kg of rice went
 * and the answer cannot have been tidied up. A mistake is corrected by adding a
 * compensating row and saying so in {@code remarks}, the same rule the gate log and the
 * wallet ledger follow.
 *
 * <p>{@code quantity} is always positive. Whether stock went up or down comes from
 * {@code movementType}, and there is deliberately no separate direction field: two fields
 * saying the same thing can disagree, and a RECEIPT that reduced stock would be
 * impossible to explain.
 *
 * <p>{@code quantityAfter} is the balance as it stood immediately after this row. It is
 * kept because it turns the ledger into something a person can actually audit: a store
 * keeper reading down the page sees the running figure and can spot exactly which row the
 * count went wrong at, without adding up from the beginning.
 *
 * <p>A transfer between stores is **two rows**, a TRANSFER_OUT and a TRANSFER_IN, linked
 * by {@code transferGroupId}. One row cannot be right, because the quantity leaves one
 * store's balance and joins another's, and a single row would have to belong to both.
 *
 * <p>WASTAGE and ADJUSTMENT_DECREASE both reduce stock and are separate on purpose.
 * Wastage is a known loss with a reason: milk that went off, a bat that snapped. An
 * adjustment is the count being wrong with nobody able to say where the difference went. A
 * store whose adjustments are large has a problem, and merging the two hides it.
 *
 * <p>The service checks that stock never goes below zero, that {@code quantityAfter}
 * agrees with the balance it wrote, that a batched item names a batch, that a transfer
 * writes both rows in one operation, and that an ADJUSTMENT carries a reason and an
 * approver.
 */
@Document(collection = "stock_movements")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_movement_item_store_idx",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1, 'inventoryStoreDocsId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_movement_store_day_idx",
                def = "{'schoolId': 1, 'inventoryStoreDocsId': 1, 'movementDate': -1}"),
        @CompoundIndex(
                name = "school_movement_type_idx",
                def = "{'schoolId': 1, 'movementType': 1, 'movementDate': -1}"),
        @CompoundIndex(
                name = "school_movement_transfer_idx",
                def = "{'schoolId': 1, 'transferGroupId': 1}",
                partialFilter = "{'transferGroupId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_movement_batch_idx",
                def = "{'schoolId': 1, 'stockBatchDocsId': 1}",
                partialFilter = "{'stockBatchDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends SchoolBase {

    // Links to InventoryItem.id. Example: "67bc1124dc3f7d0033445566"
    @NotBlank
    private String inventoryItemDocsId;

    // Links to InventoryStore.id whose balance this row changed.
    // Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String inventoryStoreDocsId;

    // Why the quantity changed, and therefore whether it went up or down.
    // Example: StockMovementType.ISSUE
    @NotNull
    private StockMovementType movementType;

    // How much moved. Always a positive number; the type says the direction.
    // Example: 12.500
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantity;

    // The store's balance immediately after this row, so somebody reading the ledger
    // can see the running figure and spot where a count went wrong. Example: 37.500
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityAfter;

    // The exact moment. Example: 2026-08-19T04:10:00Z
    @NotNull
    private Instant occurredAt;

    // The date part of occurredAt, repeated so a day's register reads with a plain
    // match instead of a range. Example: 2026-08-19
    @NotNull
    private LocalDate movementDate;

    // Links to StockBatch.id when the item is batch tracked. Required for those.
    // Example: "67bc1127dc3f7d0066778899"
    private String stockBatchDocsId;

    // What was paid per unit, on a RECEIPT. Kept so stock can be valued from the
    // ledger rather than from a single price on the item. Example: 58.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitRate;

    // Ties the two halves of a transfer together. Both the TRANSFER_OUT and the
    // TRANSFER_IN carry the same value. Example: "TRF-2026-08-19-0004"
    private String transferGroupId;

    // Links to InventoryStore.id at the other end of a transfer, so one row says where
    // stock came from or went to. Example: "67bc1128dc3f7d0077889900"
    private String counterpartStoreDocsId;

    // Links to StockIssue.id when this row is the giving out or the coming back of
    // something expected to return. Example: "67bc1129dc3f7d0088990011"
    private String stockIssueDocsId;

    // Who it came from, on a RECEIPT, in plain words. Kept for a receipt entered
    // straight into the store with no paperwork behind it, which is a real thing a small
    // school does. The proper record is a GoodsReceipt, whose line holds this row's id;
    // when one exists, prefer it and read the vendor through it.
    // Example: "Shree Traders, Dadar"
    private String supplierName;

    // The supplier's delivery note or bill number, so a bare receipt can still be
    // matched to paperwork. Also a GoodsReceipt field, and that one wins where both
    // exist. Example: "ST/2026/4471"
    private String supplierReference;

    // Links to Staff.id for whoever moved the stock.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String recordedByStaffDocsId;

    // Links to Staff.id for whoever approved it. Required for WASTAGE and both kinds
    // of ADJUSTMENT, because those are losses somebody has to answer for.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // Why. Required for WASTAGE, ADJUSTMENT_INCREASE and ADJUSTMENT_DECREASE.
    // Example: "Six litres soured in the power cut on 18 August."
    private String reason;

    // Anything worth knowing, including the explanation for a correcting row.
    // Example: "Corrects the issue of 19 August, which was entered as 25 instead of 15."
    private String remarks;
}
