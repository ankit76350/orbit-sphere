package com.orbitastra.backend.models.new_new.finance.billing.embedded;

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
 * <p>A line can be reduced more than once. A student may hold a year-long 25
 * percent tuition concession and still be given something extra on one bill, and
 * the printed line has to show both. One entry is kept per source so the fee desk
 * can always say which approval paid for which rupee.
 *
 * <p>At most one of {@code concessionRequestDocsId} and {@code aidAwardDocsId} is
 * set. When both are null the discount was entered by hand and {@code reason} is
 * the only record of why it was allowed.
 *
 * <p>{@code percentApplied} is filled in only when the source was a percentage
 * concession. It is kept so a reprint years later can still show "25% on tuition"
 * and not just a bare amount.
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

    // Printed under the line so the parent can see what the discount was.
    // Example: "Sibling waiver, 25% on tuition"
    private String reason;
}
