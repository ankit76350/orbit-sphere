package com.orbitastra.backend.models.finance.banking;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

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
 * <p>This is a plain record of where the school's money sits. Payments, refunds,
 * gateways and settlement batches all point at it to say which bank the money went
 * into or came out of.
 *
 * <p>There is no link to a bookkeeping account here. The books are not built yet,
 * so a field for that was removed rather than left pointing at nothing. When the
 * books are built, this is where the link back to them will go.
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
     //! this will belong to school means this is show the school bank account
    // Name staff pick from. Example: "SBI Fee Collection Account"
    @NotBlank
    private String name;

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
