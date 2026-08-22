package com.orbitastra.backend.models.new_new.procurement.embedded;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.inventory.enums.UnitOfMeasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One item on a delivery, and what the school decided about it.
 *
 * <p>Three quantities, and all three are needed. {@code orderedQuantity} is what the
 * purchase order said. {@code receivedQuantity} is what came off the lorry.
 * {@code acceptedQuantity} is what the school kept.
 *
 * <p>Ordered against received is the vendor short-delivering. Received against accepted is
 * the goods being no good. Those are two completely different complaints — one is a vendor
 * who did not send enough, the other is a vendor who sent rubbish — and a school that
 * cannot tell them apart cannot argue either one. The rejected quantity is the difference,
 * so it is not stored.
 *
 * <p>**Only {@code acceptedQuantity} becomes stock.** Rejected goods were never the
 * school's in any sense a store balance should reflect, and they go back on the same lorry.
 *
 * <p>{@code stockMovementDocsId} is the seam between this package and inventory, and it is
 * the most important field here. When the receipt is accepted, each line writes one RECEIPT
 * row into the stock ledger, and this holds its id. Without it, "where did these forty kilos
 * come from" and "did this delivery ever reach the store" are two questions nobody can
 * answer from either side. It is null while the receipt is still DRAFT, and that null is
 * exactly what says the stock has not moved yet.
 *
 * <p>{@code batchNumber} and {@code expiryDate} are filled in for anything the item master
 * marks as batch tracked, and they are what the created StockBatch is built from. Milk with
 * no expiry date recorded is milk nobody can be warned about.
 *
 * <p>{@code unitRate} is copied from the order line rather than read through it. A receipt
 * has to keep valuing stock at the price actually paid for it even after the order is
 * closed, and the stock ledger reads this figure straight through.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptLine {

    // Order this line appears in. Example: 1
    @NotNull
    private Integer lineNo;

    // Which line of the purchase order this answers. Null on a direct purchase with no
    // order behind it. Example: 1
    private Integer purchaseOrderLineNo;

    // Links to InventoryItem.id. Example: "67bc1124dc3f7d0033445566"
    @NotBlank
    private String inventoryItemDocsId;

    // Item code copied in at receipt time. Example: "PROV-RICE-01"
    @NotBlank
    private String itemCode;

    // Item name copied in at receipt time. Example: "Rice, Sona Masoori"
    @NotBlank
    private String itemName;

    // What the order asked for. Null on a direct purchase. Example: 200.000
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal orderedQuantity;

    // What actually came off the lorry. Example: 200.000
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal receivedQuantity;

    // What the school kept. The only figure that becomes stock, and the only figure the
    // vendor may bill for. Example: 185.000
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal acceptedQuantity;

    // What that quantity is counted in. Example: UnitOfMeasure.KILOGRAM
    @NotNull
    private UnitOfMeasure unitOfMeasure;

    // Why any of it was sent back. Required whenever accepted is less than received,
    // because "some went back" with no reason is not a record of anything.
    // Example: "Fifteen kilos damp and smelling musty."
    private String rejectionReason;

    // The price per unit paid, copied from the order line. The stock ledger reads this
    // straight through, so stock is valued at what was really paid. Example: 61.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitRate;

    // The vendor's batch or lot number, for anything batch tracked.
    // Example: "SM-2608-A"
    private String batchNumber;

    // When it goes off. Required for anything batch tracked, because a batch with no
    // expiry date cannot be warned about. Example: 2027-02-14
    private LocalDate expiryDate;

    // When it was made or packed, when the packet says so. Example: 2026-08-01
    private LocalDate manufacturedOn;

    // Links to StockBatch.id created for this line, for a batch tracked item.
    // Example: "67bc1127dc3f7d0066778899"
    private String stockBatchDocsId;

    // Links to StockMovement.id of the RECEIPT row this line wrote into the stock
    // ledger. Null until the receipt is accepted, and that null is what says the stock
    // has not moved. Example: "67bc1129dc3f7d0088990011"
    private String stockMovementDocsId;

    // Anything else worth knowing about this line.
    // Example: "Weighed on the school scale in front of the driver."
    private String remarks;
}
