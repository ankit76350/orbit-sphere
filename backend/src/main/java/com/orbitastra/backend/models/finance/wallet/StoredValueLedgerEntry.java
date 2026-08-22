package com.orbitastra.backend.models.new_new.finance.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.LedgerEntryDirection;
import com.orbitastra.backend.models.new_new.finance.enums.WalletEntryType;
import com.orbitastra.backend.models.new_new.finance.enums.WalletReferenceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One movement of money in or out of a wallet.
 *
 * <p>Entries are only ever added, never changed and never deleted. A wrong entry
 * is cancelled by writing a REVERSAL entry that points back at it through
 * {@code reversalOfLedgerEntryDocsId}. That is what lets the school answer "where
 * did this balance come from" months later.
 *
 * <p>{@code sequenceNo} is unique per wallet and has no gaps, so the entries can
 * be read as a statement in order. {@code balanceAfter} is saved on each entry so
 * a statement line shows the running balance without adding up everything before
 * it.
 *
 * <p>{@code idempotencyKey} is the only thing that stops the same movement being
 * written twice when a gateway callback or a retry comes in again, so the service
 * must always supply one. The reference fields are for looking an entry up from
 * the payment or refund behind it, and are deliberately not unique: a wallet may
 * legitimately receive two transfers from the same other wallet.
 *
 * <p>{@code holdAmount} entries move money between available and held without
 * changing the wallet total, so a HOLD is a real entry rather than a silent edit
 * to the balance.
 */
@Document(collection = "stored_value_ledger_entries")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_wallet_sequence_uniq",
                def = "{'schoolId': 1, 'storedValueAccountDocsId': 1, 'sequenceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_wallet_reference_entry_idx",
                def = "{'schoolId': 1, 'storedValueAccountDocsId': 1, 'referenceType': 1, 'referenceDocsId': 1, 'entryType': 1}"),
        @CompoundIndex(
                name = "school_wallet_idempotency_uniq",
                def = "{'schoolId': 1, 'idempotencyKey': 1}",
                unique = true,
                partialFilter = "{'idempotencyKey': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_wallet_statement_idx",
                def = "{'schoolId': 1, 'storedValueAccountDocsId': 1, 'occurredAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StoredValueLedgerEntry extends SchoolBase {

    // Links to StoredValueAccount.id. Example: "67ad7788dc3f7d0077889900"
    @NotBlank
    private String storedValueAccountDocsId;

    // Position in this wallet's history, starting at 1 with no gaps. Example: 47
    @NotNull
    private Long sequenceNo;

    // Which way the money moved. Example: LedgerEntryDirection.CREDIT
    @NotNull
    private LedgerEntryDirection direction;

    // Why the entry was written. Example: WalletEntryType.TOP_UP
    @NotNull
    private WalletEntryType entryType;

    // Money moved, always stored as a positive number. The direction says
    // whether it went in or out. Example: 2000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Spendable balance after this entry. Example: 3200.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balanceAfter;

    // Set-aside balance after this entry. Example: 800.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal heldBalanceAfter = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // What kind of record caused this entry.
    // Example: WalletReferenceType.FEE_PAYMENT
    @NotNull
    private WalletReferenceType referenceType;

    // Links to the record named by referenceType. Null only for a correction
    // typed in by hand. Example: "67ae1122dc3f7d0011223344"
    private String referenceDocsId;

    // Number a parent would recognise, such as the receipt on the top-up.
    // Example: "RCP/2026/000871"
    private String referenceNo;

    // Stops the same movement being written twice on a retry.
    // Example: "wallet-topup-pay_R7pc3Q1j9"
    private String idempotencyKey;

    // When the money actually moved, which may be before it was saved.
    // Example: 2026-06-14T08:30:00Z
    @NotNull
    private Instant occurredAt;

    // Links to the staff identity that wrote the entry, or null when the system
    // wrote it. Example: "67aa15d9dc3f7d0044444444"
    private String postedByDocsId;

    // Set on a reversal entry and points at the entry being cancelled.
    // Example: "67af1122dc3f7d0011223344"
    private String reversalOfLedgerEntryDocsId;

    // Set on the original entry once a reversal cancels it.
    // Example: "67af2233dc3f7d0022334455"
    private String reversedByLedgerEntryDocsId;

    // Plain reason for the entry, needed for every hand-typed correction.
    // Example: "Balance corrected after a cash top-up was entered twice."
    private String remarks;
}
