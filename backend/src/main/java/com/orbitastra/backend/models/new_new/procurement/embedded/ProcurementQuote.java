package com.orbitastra.backend.models.new_new.procurement.embedded;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One price somebody quoted for what a request asks for.
 *
 * <p>This is what replaces the formal tendering models the reference sketch had. A school
 * does not run a sealed-bid process. Somebody rings three shops, or sends a photograph of
 * the list on WhatsApp, and writes down what each said. Three quotes on the request is
 * exactly that, and it is enough to show that a price was compared before money was spent.
 *
 * <p>Embedded rather than a collection because quotes are compared once, at the moment of
 * approval, and then never queried again. A sketch with SourcingEvent and VendorBid as two
 * separate collections was solving a government procurement problem the school does not
 * have.
 *
 * <p>{@code vendorDocsId} may be null, with {@code vendorName} filled in instead. The
 * shop that quoted the highest price and did not get the order should not have to be
 * created as a vendor record. Only the vendor that wins needs to exist properly, because
 * only they will be paid.
 *
 * <p>{@code selected} says which quote the order went to. Keeping the losing quotes is the
 * whole reason this exists: a single price on a request proves nothing, and three prices
 * with the cheapest chosen proves something. Three prices with the *dearest* chosen and a
 * note saying why is also fine, and is the case worth being able to see.
 *
 * <p>{@code quoteDocumentDocsId} points at the photograph or the emailed sheet, because a
 * typed number that nobody can trace back is worth very little in an audit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementQuote {

    // Links to Vendor.id when the quote came from a vendor the school already has.
    // Null for a shop that was only asked for a price. Example: "67bd1122dc3f7d0011223344"
    private String vendorDocsId;

    // Who quoted, in plain words. Always filled in, including when a vendor is named,
    // so the quote reads on its own. Example: "Shree Traders, Dadar"
    @NotNull
    private String vendorName;

    // What they said the whole lot would cost. Example: 12300.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quotedAmount;

    // When they said it. A price from three months ago is not a price.
    // Example: 2026-08-14
    private LocalDate quotedOn;

    // How long they will hold that price for. Example: 2026-08-31
    private LocalDate validUntil;

    // Whether the order went to this one. Exactly one quote on a request may be true.
    // Example: true
    @Builder.Default
    private Boolean selected = false;

    // Why this quote was chosen when it was not the cheapest. The field that makes
    // choosing the dearer vendor an explained decision rather than a suspicious one.
    // Example: "Only supplier who will deliver to the kitchen door."
    private String selectionNote;

    // Links to DocumentRecord.id for the photograph or emailed sheet the price came
    // from. Example: "67bd1123dc3f7d0022334455"
    private String quoteDocumentDocsId;
}
