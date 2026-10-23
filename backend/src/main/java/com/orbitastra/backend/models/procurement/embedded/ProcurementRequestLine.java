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
 * One thing a department has asked for.
 *
 * <p>{@code inventoryItemDocsId} may be null, and that is the point of having
 * {@code itemDescription} beside it. A department asking for two hundred kilograms of rice
 * is asking for an item the store already knows about. A department asking for a
 * three-dimensional printer for the science club is asking for something that has never
 * been in the store and has no item row yet. A request form that can only name existing
 * items cannot be used to buy anything new, which is most of what a school buys once and
 * never again.
 *
 * <p>The item row gets created later, when the thing is actually received — not when it is
 * requested. Creating one at request time would fill the item master with things the school
 * asked for, was refused, and never bought.
 *
 * <p>{@code estimatedUnitRate} is a guess, and it is labelled as one. Its job is to give
 * whoever approves the request a rough total to react to, not to be the price paid. The
 * real price is agreed on the purchase order. For an item the store already stocks the
 * guess can be filled in from InventoryItem.lastPurchaseRate.
 *
 * <p>{@code orderedQuantity} is how much of this line has actually made it onto a purchase
 * order. It is what makes PARTIALLY_ORDERED mean something, and it is rebuildable by adding
 * up the purchase order lines that point back at this request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementRequestLine {

    // Order this line appears in on the request. Example: 1
    @NotNull
    private Integer lineNo;

    // Links to InventoryItem.id when the store already stocks this. Null for something
    // the school has never bought. Example: "67bc1124dc3f7d0033445566"
    private String inventoryItemDocsId;

    // What is wanted, in the requester's own words. Always filled in, even when an
    // item is named, because the words on the request are what the department actually
    // asked for. Example: "Rice, Sona Masoori, medium grain"
    @NotBlank
    private String itemDescription;

    // How much is wanted. Example: 200.000
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantity;

    // What that quantity is counted in. Example: UnitOfMeasure.KILOGRAM
    @NotNull
    private UnitOfMeasure unitOfMeasure;

    // A rough price per unit, so the approver has a total to react to. Not the price
    // paid. Example: 62.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedUnitRate;

    // quantity times estimatedUnitRate, worked out by the service. Example: 12500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedAmount;

    // How much of this line has reached a purchase order. Starts at zero and is
    // rebuildable from the purchase order lines. Example: 200.000
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal orderedQuantity = BigDecimal.ZERO;

    // Why it is needed, when that is not obvious from the item.
    // Example: "Current sack finishes on Friday."
    private String justification;
}
