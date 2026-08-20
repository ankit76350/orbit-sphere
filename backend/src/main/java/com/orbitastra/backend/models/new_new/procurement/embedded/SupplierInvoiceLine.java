package com.orbitastra.backend.models.new_new.procurement.embedded;

import java.math.BigDecimal;

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
 * One charge on a vendor's bill, as the vendor wrote it.
 *
 * <p>These lines are what the vendor claims, not what the school agreed. That distinction
 * is the entire reason this class exists, and it is why {@code billedUnitRate} sits next to
 * {@code orderedUnitRate} and {@code billedQuantity} next to {@code acceptedQuantity}.
 *
 * <p>**This is the check that catches the money.** A vendor bills 63 a kilogram when the
 * order said 61.50, on two hundred kilograms, and the difference is three hundred rupees
 * that nobody notices on a bill with fourteen lines on it. Or a vendor bills for the full
 * two hundred kilograms when fifteen went back as damp. Neither is caught by looking at the
 * bill total, because the bill total is always internally consistent — the vendor added it
 * up correctly. It is only caught line by line, against the order and the delivery.
 *
 * <p>A school without this ends up paying whatever the bill says, because the alternative
 * is a clerk holding three pieces of paper side by side and doing the arithmetic by hand
 * fourteen times.
 *
 * <p>The comparison figures are copied in when the bill is entered. The service works out
 * {@code rateVariance} and {@code quantityVariance} and stores them, so a payables screen
 * can show every bill with a variance without recomputing the join. A variance of zero is
 * the normal case and is stored as zero rather than left null, because null would then mean
 * both "no difference" and "nobody checked".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoiceLine {

    // Order this line appears in on the vendor's bill. Example: 1
    @NotNull
    private Integer lineNo;

    // Links to InventoryItem.id. Example: "67bc1124dc3f7d0033445566"
    @NotBlank
    private String inventoryItemDocsId;

    // What the vendor called it on their bill, which is not always what the school
    // calls it. Example: "Sona Masoori Rice (Medium)"
    @NotBlank
    private String billedDescription;

    // How much the vendor says they supplied. Example: 200.000
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal billedQuantity;

    // What that quantity is counted in. Example: UnitOfMeasure.KILOGRAM
    @NotNull
    private UnitOfMeasure unitOfMeasure;

    // What the vendor is charging per unit. Example: 63.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal billedUnitRate;

    // What the purchase order said the rate would be, copied in for comparison.
    // Example: 61.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal orderedUnitRate;

    // What the school actually took in, copied from the goods receipt for comparison.
    // Example: 185.000
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal acceptedQuantity;

    // billedUnitRate less orderedUnitRate. Zero when they agree, and stored as zero
    // rather than left null so that null can mean nobody has checked. Example: 1.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal rateVariance;

    // billedQuantity less acceptedQuantity. Positive means the vendor is billing for
    // more than the school took. Example: 15.000
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityVariance;

    // GST rate on this line. Example: 5.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxRatePercent;

    // The tax the vendor has charged on this line. Example: 630.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // What the vendor says this line comes to. Example: 13230.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lineTotal;

    // What the school has agreed to pay for this line, once a variance has been argued
    // out. Equal to lineTotal in the ordinary case. Example: 11377.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal agreedLineTotal;

    // Why the agreed figure differs from what was billed.
    // Example: "Billed at 63; order rate 61.50. Rate difference not accepted."
    private String varianceNote;
}
