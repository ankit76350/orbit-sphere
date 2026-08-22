package com.orbitastra.backend.models.finance.billing.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One discount on a FeeInvoiceLine, and where it came from.
 *
 * <p>A line can have more than one discount on it. A student may have a year-long
 * 25 percent tuition discount and still be given something extra on one bill, and
 * the printed line has to show both. We keep one entry for each source, so the fee
 * desk can always say which approval paid for which part.
 *
 * <p>At most one of {@code concessionRequestDocsId} and {@code aidAwardDocsId} is
 * filled in. If both are empty, somebody entered the discount by hand and
 * {@code reason} is the only record of why it was allowed.
 *
 * <p>A concession request is only linked here after it is APPROVED. A request that
 * is still being written, or still waiting for a decision, takes nothing off a
 * bill.
 *
 * <p>{@code percentApplied} is filled in only when the discount came from a
 * percentage concession. We keep it so a reprint years later can still show
 * "25% on tuition" and not just an amount on its own.
 *
 * <p>These entries are also how we work out how much of a fee head's yearly limit
 * is already used. Add up {@code amount} for one student, one year and one head
 * across the bills that are not void, and that is the answer. This is why we do
 * not keep a separate total anywhere.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineDiscount {

    //! Links to ConcessionRequest.id when a school discount caused it.
    //! link only when if that thing is Request aprove....
    // Example: "67ac7788dc3f7d0033445566"
    private String concessionRequestDocsId;

    // Links to AidAward.id when a scholarship caused it.
    // Example: "67ac8899dc3f7d0044556677"
    private String aidAwardDocsId;

    // Share used when the source was a percentage concession. Example: 25.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentApplied;

    // Money this source takes off the line. Example: 2500.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // What the discount worked out to before the fee head's yearly limit cut it
    // down. The same as amount when nothing was cut. We keep it so the office can
    // tell a parent why this month's discount is smaller than last month's.
    // Example: 2500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal uncappedAmount;

    // Printed under the line so the parent can see what the discount was.
    // Example: "Sibling waiver, 25% on tuition"
    private String reason;
}
