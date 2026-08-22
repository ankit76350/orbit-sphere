package com.orbitastra.backend.models.procurement.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.inventory.enums.UnitOfMeasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing being ordered from a vendor, at the price agreed.
 *
 * <p>{@code itemCode} and {@code itemName} are copied in when the order is raised, and this
 * is the same rule FeeInvoiceLine follows for the fee head name. A purchase order is a
 * document that went out of the building. Renaming the item next year must not change what
 * the vendor was asked for, and an item made inactive must not turn a two-year-old order
 * into a row of blanks.
 *
 * <p>Note the difference from a live agreement: TransportAllocation copies a fare because
 * the family agreed to that figure on a date, and a purchase order copies a name and a rate
 * for the same reason. A snapshot on something still being negotiated would be different,
 * and would only manufacture staleness.
 *
 * <p>{@code unitRate} is the real price, agreed with the vendor. It is not
 * ProcurementRequestLine.estimatedUnitRate, which was a guess to help an approver. When the
 * two differ by a lot, that is worth somebody looking at, and keeping both is what makes
 * the comparison possible.
 *
 * <p>{@code receivedQuantity} and {@code acceptedQuantity} are running totals kept here so
 * a partly delivered order can be read without loading every goods receipt against it.
 * Both must always be rebuildable by adding up the receipt lines that point at this line,
 * which remain the real record.
 *
 * <p>They are two figures rather than one because a delivery can be refused. Forty kilograms
 * of tomatoes arrived and thirty-four were taken: received is forty, accepted is thirty-four.
 * The vendor may only bill for thirty-four, and only thirty-four became stock. One figure
 * would have to choose which of those two truths to tell.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLine {

    // Order this line appears in on the printed order. Example: 1
    @NotNull
    private Integer lineNo;

    // Links to InventoryItem.id. Example: "67bc1124dc3f7d0033445566"
    @NotBlank
    private String inventoryItemDocsId;

    // Item code copied in when the order was raised. Example: "PROV-RICE-01"
    @NotBlank
    private String itemCode;

    // Item name copied in when the order was raised. Example: "Rice, Sona Masoori"
    @NotBlank
    private String itemName;

    // Links to ProcurementRequest.id this line came from, so a department can see what
    // happened to what they asked for. Null for an order raised without a request.
    // Example: "67bd1124dc3f7d0033445566"
    private String procurementRequestDocsId;

    // Which line of that request. Example: 1
    private Integer procurementRequestLineNo;

    // How much is being ordered. Example: 200.000
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantity;

    // What that quantity is counted in, copied from the item. Example: UnitOfMeasure.KILOGRAM
    @NotNull
    private UnitOfMeasure unitOfMeasure;

    // The price per unit actually agreed with the vendor. Example: 61.50
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitRate;

    // Discount the vendor gave on this line, as an amount. Example: 200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountAmount;

    // GST rate on this line, as a share. Example: 5.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxRatePercent;

    // The tax worked out from that rate. Example: 605.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // quantity times unitRate, less the discount, plus the tax. Worked out by the
    // service so a reprint years later shows the same figure. Example: 12705.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lineTotal;

    // How much has turned up so far, accepted or not. Rebuildable from the goods
    // receipts. Example: 200.000
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    // How much of what turned up the school actually took. Only this part became
    // stock, and only this part may be billed. Example: 185.000
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal acceptedQuantity = BigDecimal.ZERO;

    // Anything the vendor needs to be told about this line.
    // Example: "Deliver in 50 kg sacks, not loose."
    private String remarks;
}
