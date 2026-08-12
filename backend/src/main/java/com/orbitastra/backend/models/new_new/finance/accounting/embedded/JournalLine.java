package com.orbitastra.backend.models.new_new.finance.accounting.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One debit or credit inside a JournalEntry.
 *
 * <p>It has no collection identity of its own. The lines of an entry are always
 * read and posted together, and they must add up, so they live inside the entry
 * rather than in a collection where half of them could go missing.
 *
 * <p>Exactly one of {@code debit} and {@code credit} carries an amount on each
 * line; the other stays zero. Storing both as separate fields, instead of one
 * signed number, is what makes the entry read the way an accountant expects.
 *
 * <p>{@code partyType} and {@code partyDocsId} say who a line is about, so a
 * receivable account can be broken down by student without needing one ledger
 * account per family.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLine {

    // Order this line appears in. Example: 1
    @NotNull
    private Integer lineNo;

    // Links to LedgerAccount.id. Example: "67ac20a1dc3f7d0066554433"
    @NotBlank
    private String ledgerAccountDocsId;

    // Account code copied in so the line reads on its own. Example: "4100"
    @NotBlank
    private String ledgerAccountCode;

    // Money going in. Zero when this is a credit line. Example: 11350.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal debit = BigDecimal.ZERO;

    // Money going out. Zero when this is a debit line. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal credit = BigDecimal.ZERO;

    // What kind of party this line is about, such as STUDENT or VENDOR.
    // Example: "STUDENT"
    private String partyType;

    // Links to the party named by partyType. Example: "67aa15d9dc3f7d0055555555"
    private String partyDocsId;

    // Cost centre the amount belongs to, for internal reporting.
    // Example: "PRIMARY_WING"
    private String costCentreCode;

    // Short note about this line. Example: "April 2026 tuition raised."
    private String memo;
}
