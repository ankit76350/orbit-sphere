package com.orbitastra.backend.models.new_new.finance.billing.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.finance.enums.FeeCategory;

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
 * <p>{@code concessionRequestDocsId} and {@code aidAwardDocsId} say where the
 * discount on this line came from. When both are null but a discount exists, it
 * was a one-off entry and {@code discountReason} has to explain it.
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

    // Money taken off this line. Example: 1875.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Links to ConcessionRequest.id when a school discount caused it.
    // Example: "67ac7788dc3f7d0033445566"
    private String concessionRequestDocsId;

    // Links to AidAward.id when a scholarship caused it.
    // Example: "67ac8899dc3f7d0044556677"
    private String aidAwardDocsId;

    // Needed when a discount was given with no policy or award behind it.
    // Example: "One-off waiver approved by the principal."
    private String discountReason;

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

    // Income account this line posts to, copied from the head so a later change
    // to the head does not move an old posting.
    // Example: "67ac20a1dc3f7d0066554433"
    private String revenueLedgerAccountDocsId;
}
