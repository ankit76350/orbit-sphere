package com.orbitastra.backend.models.new_new.finance.accounting;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.LedgerAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One line in the school's chart of accounts, such as Tuition Income or Bank
 * Current Account.
 *
 * <p>Accounts form a tree through {@code parentAccountDocsId}. Group accounts
 * exist to add things up on a report and must have {@code postingAllowed} set to
 * false, so nothing is ever posted to a heading instead of to a real account.
 *
 * <p>{@code accountCode} is the stable key that fee heads, bank accounts and
 * journal lines point at. It must not be renumbered once entries exist, because
 * the code is what an accountant recognises and what an export to Tally or Zoho
 * Books matches on.
 */
@Document(collection = "ledger_accounts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_ledger_account_code_uniq",
                def = "{'schoolId': 1, 'accountCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_ledger_parent_sort_idx",
                def = "{'schoolId': 1, 'parentAccountDocsId': 1, 'sortOrder': 1}"),
        @CompoundIndex(
                name = "school_ledger_type_active_idx",
                def = "{'schoolId': 1, 'accountType': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount extends SchoolBase {

    // Stable key used across the books and in exports. Example: "4100"
    @NotBlank
    private String accountCode;

    // Example: "Tuition Fee Income"
    @NotBlank
    private String name;

    // Links to the group account above this one. Null for a top-level account.
    // Example: "67ac20a1dc3f7d0011223344"
    private String parentAccountDocsId;

    // Example: LedgerAccountType.REVENUE
    @NotNull
    private LedgerAccountType accountType;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // False for a group heading that only adds up the accounts under it.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean postingAllowed = true;

    // Whether this account has to be matched against a bank statement.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean reconciliationRequired = false;

    // Order this account appears in on reports. Example: 100
    @Builder.Default
    private Integer sortOrder = 0;

    // Code used when sending this account to an outside accounting package.
    // Example: "TALLY-4100"
    private String externalAccountCode;

    // Whether new entries may still be posted here. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // What this account is meant to be used for, so staff pick the right one.
    // Example: "All regular tuition income, excluding transport and hostel."
    private String description;
}
