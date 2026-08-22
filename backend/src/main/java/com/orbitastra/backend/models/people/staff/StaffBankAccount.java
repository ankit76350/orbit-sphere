package com.orbitastra.backend.models.new_new.people.staff;

import java.time.Instant;

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
 * Where one member of staff's salary is sent.
 *
 * <p>Without this, payroll can work out what everybody is owed and pay nobody. The school's
 * own accounts are in finance/banking; this is the other end of the transfer.
 *
 * <p>It is a collection rather than a field on Staff because people change banks. A new
 * account is a new row and the old one is closed, so August's payslip can still say which
 * account it actually went to after somebody switches banks in November. Making it a single
 * field would rewrite history every time a person changed bank.
 *
 * <p>The account number is never stored in plain text. The same three fields as BankAccount
 * and Visitor, each doing one job: the encrypted value holds the real number, the lookup
 * hash finds a duplicate without anything being decrypted, and the masked version is the
 * only one safe to show on a screen. This follows the pattern StaffGovernmentIdentity
 * already uses for identity numbers.
 *
 * <p>{@code accountHolderName} is separate from the staff member's own name on purpose. A
 * joint account in a spouse's name, or a spelling that differs from the school's records, is
 * the usual reason a salary transfer bounces. Keeping it lets somebody check the two match
 * before payday rather than after.
 *
 * <p>{@code verifiedAt} is what a school should insist on before the first payment. A
 * cancelled cheque or a passbook copy checked against these values once is what stops a
 * salary going to a mistyped account, and there is no getting it back afterwards.
 *
 * <p>Only one account per member of staff is primary at a time, which the unique index
 * enforces.
 *
 * <p>The service checks that a salary is never paid to an unverified or inactive account,
 * that closing the primary account requires another to be made primary, and that reading
 * these records needs the PAYROLL module rather than plain staff access.
 */
@Document(collection = "staff_bank_accounts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_bank_primary_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1}",
                unique = true,
                partialFilter = "{'primaryAccount': true, 'active': true}"),
        @CompoundIndex(
                name = "school_staff_bank_lookup_idx",
                def = "{'schoolId': 1, 'accountNumberLookupHash': 1}"),
        @CompoundIndex(
                name = "school_staff_bank_staff_idx",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffBankAccount extends SchoolBase {

    // Links to Staff.id. Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String staffDocsId;

    // Name the account is held in. Often not the same as the staff member's own name,
    // and the usual reason a transfer bounces. Example: "Priya S Nair"
    @NotBlank
    private String accountHolderName;

    // Real account number, encrypted before it is saved.
    // Example: "enc:v1:6d5c4b3a29187766"
    @NotBlank
    private String encryptedAccountNumber;

    // One-way hash used to spot the same account twice without decrypting.
    // Example: "sha256:9c8b7a6d5e4f30211122334455667788"
    @NotBlank
    private String accountNumberLookupHash;

    // The only version safe to show on a screen or a payslip. Example: "XXXXXX4821"
    @NotBlank
    private String maskedAccountNumber;

    // Example: "State Bank of India"
    @NotBlank
    private String bankName;

    // Example: "Andheri East, Mumbai"
    private String branchName;

    // IFSC code, needed for the transfer itself. Example: "SBIN0011234"
    @NotBlank
    private String ifscCode;

    // Whether this is the account salary goes to. Only one per member of staff.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean primaryAccount = true;

    // Links to DocumentRecord.id for a cancelled cheque or passbook copy.
    // Example: "67be1122dc3f7d0011223344"
    private String evidenceDocumentDocsId;

    // Links to the staff identity that checked the evidence against these values.
    // A salary paid into a mistyped account does not come back.
    // Example: "67aa15d9dc3f7d0055555555"
    private String verifiedByDocsId;

    // When it was checked. A school should insist on this before the first payment.
    // Example: 2026-04-02T06:15:00Z
    private Instant verifiedAt;

    // Whether the account may still be used. A closed one is kept so old payslips still
    // read correctly. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Why it was closed. Example: "Staff member moved to HDFC in November 2026."
    private String statusReason;
}
