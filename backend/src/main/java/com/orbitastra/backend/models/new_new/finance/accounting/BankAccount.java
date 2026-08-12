package com.orbitastra.backend.models.new_new.finance.accounting;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One bank account the school collects fees into or pays refunds out of.
 *
 * <p>The full account number is never stored in plain text. Three fields are
 * kept instead, each for one job:
 *
 * <ul>
 * <li>{@code encryptedAccountNumber} holds the real value, encrypted;</li>
 * <li>{@code accountNumberLookupHash} lets the same account be found again and
 * kept unique without decrypting anything;</li>
 * <li>{@code maskedAccountNumber} is the only version safe to show on screen.</li>
 * </ul>
 *
 * <p>{@code ledgerAccountDocsId} ties the bank account to the books, so a
 * payment landing here posts to the matching asset account by itself.
 */
@Document(collection = "bank_accounts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_bank_account_lookup_uniq",
                def = "{'schoolId': 1, 'accountNumberLookupHash': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_bank_account_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'primaryAccount': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount extends SchoolBase {

    // Name staff pick from. Example: "SBI Fee Collection Account"
    @NotBlank
    private String name;

    // Asset account in the books this bank account maps to.
    // Example: "67ac20a1dc3f7d0011998877"
    private String ledgerAccountDocsId;

    // Example: "State Bank of India"
    @NotBlank
    private String bankName;

    // Example: "Andheri East, Mumbai"
    private String branchName;

    // Name the account is held in. Example: "Orbitastra Public School"
    @NotBlank
    private String accountHolderName;

    // Real account number, encrypted before it is saved.
    // Example: "enc:v1:9f8a7b6c5d4e3f2a1b0c"
    @NotBlank
    private String encryptedAccountNumber;

    // One-way hash used to find and keep the account unique without decrypting.
    // Example: "sha256:4d2a91c8e77b3f5a6c0d8e2b1f9a7c34"
    @NotBlank
    private String accountNumberLookupHash;

    // The only version safe to show on screen. Example: "XXXXXX4821"
    @NotBlank
    private String maskedAccountNumber;

    // IFSC or other routing code for the branch. Example: "SBIN0011234"
    private String routingCode;

    // Example: "SBININBB123"
    private String swiftCode;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Whether statement lines for this account have to be matched.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean reconciliationRequired = true;

    // The account used by default when none is chosen. Only one per school
    // should have this set, which the service checks. Example: true
    @NotNull
    @Builder.Default
    private Boolean primaryAccount = false;

    // Whether the account may still be used. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
