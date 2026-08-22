package com.orbitastra.backend.models.new_new.procurement.embedded;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Where to send a vendor's money.
 *
 * <p>Embedded in Vendor rather than kept in its own collection. A vendor has one account
 * the school pays into, it is read whenever somebody is paying them, and there is no
 * separate group of people who should see the vendor but not their bank details — the whole
 * package sits behind the PROCUREMENT permission. StaffBankAccount is its own collection
 * for the opposite reason: a head of department has business with a colleague's record and
 * none at all with their account number.
 *
 * <p>The account number is held as the same three fields used by StaffBankAccount,
 * StudentGovernmentIdentity and Visitor. The encrypted value holds the real number, the
 * lookup hash finds a duplicate without anything being decrypted, and the masked version
 * is what a screen shows.
 *
 * <p>The lookup hash earns its place here more than anywhere else in the system. **Two
 * vendors sharing a bank account is the single most common way a school is defrauded** —
 * somebody adds a second vendor under a different name, paying into the account they
 * already control, and splits orders between the two so neither looks large. The hash makes
 * that an automatic check at the moment the second vendor is saved, instead of something
 * an auditor might notice a year later.
 *
 * <p>{@code accountHolderName} is separate from the vendor's own name on purpose, and the
 * gap between them is worth looking at. A firm called Shree Traders whose account is in one
 * person's name may be perfectly ordinary, or it may be the thing an audit asks about.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorBankAccount {

    // The name on the bank account, which is not always the vendor's trading name.
    // Example: "Shree Traders"
    @NotBlank
    private String accountHolderName;

    // The real account number, encrypted before it is saved.
    // Example: "enc:v1:9d8c7b6a5f4e3d2c"
    @NotBlank
    private String encryptedAccountNumber;

    // A one-way hash of the account number, so the same account can be spotted on two
    // vendors without anything being decrypted.
    // Example: "b41c9a77e0f3d15a8c62b9f4e7d0a3c1"
    @NotBlank
    private String accountNumberLookupHash;

    // What a screen shows, so a clerk can confirm they have the right account without
    // the number being readable. Example: "XXXXXX4471"
    @NotBlank
    private String maskedAccountNumber;

    // Example: "Bank of Maharashtra"
    @NotBlank
    private String bankName;

    // Which branch, which is what an IFSC code identifies.
    // Example: "HDFC0001234"
    @NotBlank
    private String ifscCode;

    // Example: "Dadar West, Mumbai"
    private String branchName;

    // Whether somebody has confirmed these details against a document from the bank,
    // rather than typing them from an email. A wrong digit sends the money to a
    // stranger, and it is not coming back. Example: true
    @Builder.Default
    private Boolean verified = false;
}
