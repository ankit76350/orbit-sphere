package com.orbitastra.backend.models.finance.billing.embedded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.finance.enums.FeeCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One charge on a FeeInvoice, as it stood when the bill was made.
 *
 * <p>It has no collection identity of its own. The head name, category and tax
 * rate are copied in on purpose: a bill has to keep showing what the parent was
 * actually charged, even after the fee head is renamed, retaken or made inactive.
 *
 * <p>The line total is {@code quantity * unitAmount - discountAmount + taxAmount}.
 * The service works it out and the report DTOs never recalculate it, so a printed
 * bill and a reprint years later always show the same numbers.
 *
 * <p>{@code discounts} says where the money taken off this line came from, with
 * one entry for each source. A line can have more than one discount on it at the
 * same time: a student may have a year-long tuition discount and still be given
 * extra help on this one bill. {@code discountAmount} is those entries added up,
 * and it is the only figure the line total uses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeInvoiceLine {

    // Order this line appears in on the bill. Example: 1
    @NotNull
    private Integer lineNo;

    // Links to FeeHead.id. Example: "67ac1188dc3f7d0011aa22bb"
    @NotBlank
    private String feeHeadDocsId;

    // Head code copied in at billing time. Example: "TUITION"
    @NotBlank
    private String feeHeadCode;

    // Head name copied in at billing time. Example: "Tuition Fee"
    @NotBlank
    private String feeHeadName;

    // Grouping copied in at billing time. Example: FeeCategory.TUITION
    private FeeCategory category;

    // Extra detail printed under the head name.
    // Example: "April 2026 to June 2026"
    private String description;

    // How many units are charged, usually 1. Example: 1.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantity = BigDecimal.ONE;

    // Charge for one unit before any discount or tax. Example: 7500.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitAmount;

    // Money taken off this line, and the sum of the discounts list below.
    // Example: 2500.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Where each part of the discount came from. Empty when nothing was taken off.
    @Valid
    @Builder.Default
    private List<InvoiceLineDiscount> discounts = new ArrayList<>();

    // Tax rate copied in at billing time. Example: 0.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxRatePercent;

    // Tax worked out on the amount after the discount. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // What the parent owes for this line. Example: 5625.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lineTotal;
}
