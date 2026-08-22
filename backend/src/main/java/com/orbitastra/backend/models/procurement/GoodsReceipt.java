package com.orbitastra.backend.models.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.procurement.embedded.GoodsReceiptLine;
import com.orbitastra.backend.models.procurement.enums.GoodsReceiptStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What actually turned up, and what the school decided about it.
 *
 * <p>**This is the model that joins procurement to inventory.** Accepting a receipt writes
 * one RECEIPT row into the stock ledger for each line, and each line keeps the id of the row
 * it wrote. Before this model existed, stock arrived from nowhere: a StockMovement of type
 * RECEIPT with a supplier's name typed into a string field, and no order, no price agreed
 * and nobody to argue with about it.
 *
 * <p>Ordered, received and accepted are three different quantities and the lines carry all
 * three. Ordered against received is a vendor who did not send enough. Received against
 * accepted is a vendor who sent rubbish. Those are two completely different complaints, and
 * a school that cannot tell them apart cannot make either one.
 *
 * <p>**Only the accepted quantity becomes stock.** Rejected goods were never the school's in
 * any sense a store balance should reflect, and they go back on the lorry they came on.
 *
 * <p>DRAFT exists because unloading and checking are not the same moment. A lorry arrives at
 * seven, the store keeper writes down what came off it, and the cook looks at the vegetables
 * at eight. Nothing moves into stock while the receipt is DRAFT, so
 * {@code stockMovementDocsId} on each line being null is precisely what says the stock has
 * not moved yet.
 *
 * <p>{@code purchaseOrderDocsId} may be null. A cook who buys vegetables at the market
 * because the delivery failed has bought something the school owns, and refusing to record
 * it because there was no order would mean the store balance is simply wrong. So a direct
 * purchase is allowed, and {@code directPurchaseReason} makes it explain itself. A school
 * with many of these has a problem, and it should be able to count them.
 *
 * <p>{@code vendorChallanNo} is the vendor's own delivery note number, which is the piece of
 * paper the driver hands over. It is not the bill; the bill comes later and separately, as a
 * SupplierInvoice, because goods and paperwork arrive on different days.
 *
 * <p>{@code inspectedByStaffDocsId} is deliberately separate from
 * {@code receivedByStaffDocsId}. The store keeper counts the sacks; whether it is good rice
 * is the cook's judgement. One field for both would mean the person who signs for a delivery
 * is also the person who passes it, which is the arrangement that lets bad goods through.
 *
 * <p>The service checks that accepted is never more than received, that a rejection carries
 * a reason, that a batch tracked item has a batch number and an expiry date, that accepting
 * writes the stock movements in one operation and fills in their ids, that cancelling
 * reverses them with compensating rows rather than deleting them, and that the parent
 * purchase order's received and accepted totals stay in step with these lines.
 */
@Document(collection = "goods_receipts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_goods_receipt_no_uniq",
                def = "{'schoolId': 1, 'receiptNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_goods_receipt_order_idx",
                def = "{'schoolId': 1, 'purchaseOrderDocsId': 1, 'receiptDate': -1}",
                partialFilter = "{'purchaseOrderDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_goods_receipt_vendor_idx",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'receiptDate': -1}"),
        @CompoundIndex(
                name = "school_goods_receipt_store_idx",
                def = "{'schoolId': 1, 'inventoryStoreDocsId': 1, 'receiptDate': -1}"),
        @CompoundIndex(
                name = "school_goods_receipt_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'receiptDate': -1}"),
        @CompoundIndex(
                name = "school_goods_receipt_challan_idx",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'vendorChallanNo': 1}",
                partialFilter = "{'vendorChallanNo': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt extends SchoolBase {

    // School-scoped number from NumberSequence type GOODS_RECEIPT.
    // Example: "GRN/2026/000305"
    @NotBlank
    private String receiptNo;

    // Links to PurchaseOrder.id. Null for a direct purchase with no order behind it.
    // Example: "67bd1126dc3f7d0055667788"
    private String purchaseOrderDocsId;

    // The order number copied in, so a receipt reads without loading the order.
    // Example: "PO/2026/000241"
    private String purchaseOrderNo;

    // Why there was no order. Required when purchaseOrderDocsId is null, because stock
    // appearing with no order and no explanation is the shape most quiet losses take.
    // Example: "Delivery failed; cook bought vegetables at Dadar market for lunch."
    private String directPurchaseReason;

    // Links to Vendor.id the goods came from. Example: "67bd1122dc3f7d0011223344"
    @NotBlank
    private String vendorDocsId;

    // Links to InventoryStore.id the goods went into. This decides which balance moves.
    // Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String inventoryStoreDocsId;

    // The day the goods arrived. Example: 2026-08-20
    @NotNull
    private LocalDate receiptDate;

    // The vendor's own delivery note number, from the paper the driver handed over. Not
    // the bill. Example: "ST/CH/2026/1187"
    private String vendorChallanNo;

    // The date on that delivery note, which can differ from the day it arrived.
    // Example: 2026-08-19
    private LocalDate vendorChallanDate;

    // The vehicle it came on, worth having when a delivery is later disputed.
    // Example: "MH-02-CD-4471"
    private String vehicleNumber;

    // What came, and what was done with it. At least one line.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    // What the school has taken in, valued at the rates paid. Added up from the
    // accepted quantities, so it is what the vendor may bill for. Example: 11377.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal acceptedValueAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Whether the stock has moved. Example: GoodsReceiptStatus.PARTIALLY_REJECTED
    @NotNull
    @Builder.Default
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    // Links to Staff.id of whoever took the delivery and counted it.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String receivedByStaffDocsId;

    // Links to Staff.id of whoever judged whether the goods were any good. Separate
    // from the person who counted them on purpose.
    // Example: "67aa15d9dc3f7d0066666666"
    private String inspectedByStaffDocsId;

    // When the receipt was accepted and the stock actually moved. Null while DRAFT.
    // Example: 2026-08-20T03:15:00Z
    private Instant acceptedAt;

    // Why the whole delivery was refused. Required for REJECTED.
    // Example: "Entire load wet through. Driver took it back the same morning."
    private String rejectionReason;

    // Why the receipt was withdrawn. Required for CANCELLED.
    // Example: "Entered twice. This is the duplicate; stock rows reversed."
    private String cancellationReason;

    // Links to DocumentRecord.id for a photograph of the delivery note, or of damaged
    // goods being sent back. Example: ["67bd1127dc3f7d0066778899"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    // Anything worth knowing.
    // Example: "Driver waited while the cook checked. Six kilos went back with him."
    private String remarks;
}
