package com.orbitastra.backend.models.new_new.procurement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.procurement.embedded.VendorBankAccount;
import com.orbitastra.backend.models.new_new.procurement.enums.VendorStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One business the school buys from.
 *
 * <p>The rice supplier, the stationery shop, the firm that services the lab equipment. One
 * row each, set up once and used for years. This is the standing thing; a PurchaseOrder is
 * the dated event, the same split as ConcessionPolicy against ConcessionRequest and
 * TransportRoute against TransportTrip.
 *
 * <p>Until this model existed, a vendor was a piece of free text. InventoryItem carried
 * {@code usualSupplierName} and StockMovement carried {@code supplierName}, both plain
 * strings, and "Shree Traders", "Shree Traders, Dadar" and "shree trader" were three
 * different suppliers as far as the database was concerned. Those two fields should now be
 * read as the fallback they were always described as.
 *
 * <p>{@code vendorCode} is what gets written on paper — quoted on an order, written on a
 * cheque stub, said down the telephone. It exists for the same reason InventoryItem has an
 * item code: nobody can read a document id aloud.
 *
 * <p>{@code suppliedCategoryDocsIds} answers the one question that actually gets asked at
 * the moment of raising a purchase — who can supply kitchen provisions? Without it, whoever
 * is ordering scrolls a list of every vendor the school has ever used. It links to
 * InventoryCategory rather than being its own enum, because the school has already decided
 * what its categories are and a second list would drift out of step with the first.
 *
 * <p>{@code status} is what stops a bad vendor being used again, and BLACKLISTED is
 * deliberately not the same as INACTIVE. See VendorStatus for why that difference is the
 * whole point of the field.
 *
 * <p>{@code bankAccount} is where the money goes, and its lookup hash is the check that
 * matters most in this package: **two vendors sharing one bank account is the commonest way
 * a school is defrauded.** The service must refuse to save a second vendor whose account
 * hash already exists, or at the very least make somebody senior confirm it.
 *
 * <p>{@code gstin} and {@code panNumber} are the vendor's own tax registrations, kept
 * because they have to be printed on a purchase order and quoted when tax is deducted. The
 * platform does not file anything with the tax authorities and holds no returns; see the
 * README.
 *
 * <p>There is no rating field. A number out of five that one person sets and nobody
 * maintains tells a reader nothing, and the honest version of "this vendor is trouble" is
 * SUSPENDED or BLACKLISTED with a reason written down.
 *
 * <p>The service checks that a blacklisted or suspended vendor is never named on a new
 * purchase order, that blacklisting carries a reason, and that a vendor with open orders or
 * unpaid bills is not deleted.
 */
@Document(collection = "vendors")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_vendor_code_uniq",
                def = "{'schoolId': 1, 'vendorCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vendor_name_idx",
                def = "{'schoolId': 1, 'legalName': 1}"),
        @CompoundIndex(
                name = "school_vendor_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'legalName': 1}"),
        @CompoundIndex(
                name = "school_vendor_category_idx",
                def = "{'schoolId': 1, 'suppliedCategoryDocsIds': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_vendor_gstin_uniq",
                def = "{'schoolId': 1, 'gstin': 1}",
                unique = true,
                partialFilter = "{'gstin': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_vendor_bank_hash_idx",
                def = "{'schoolId': 1, 'bankAccount.accountNumberLookupHash': 1}",
                partialFilter = "{'bankAccount.accountNumberLookupHash': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor extends SchoolBase {

    // The school's own short code for this vendor, quoted on an order and written on a
    // cheque stub. Comes from NumberSequence type VENDOR when the school does not supply
    // one. Do not rename it once orders exist. Example: "VEN-0042"
    @NotBlank
    private String vendorCode;

    // The registered name of the business, which is what goes on a purchase order and a
    // cheque. Example: "Shree Traders Private Limited"
    @NotBlank
    private String legalName;

    // What everybody actually calls them, when that is different. The name a store
    // keeper will recognise. Example: "Shree Traders, Dadar"
    private String tradeName;

    // Links to InventoryCategory.id for each kind of thing this vendor supplies, so
    // whoever is ordering provisions sees only the provisions suppliers.
    // Example: ["67bc1122dc3f7d0011223344"]
    @Builder.Default
    private List<String> suppliedCategoryDocsIds = new ArrayList<>();

    // Whether the school may order from them, and if not, whether that is because they
    // are dormant or because they are barred. Example: VendorStatus.ACTIVE
    @NotNull
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;

    // Why they were barred or suspended. Required for BLACKLISTED and SUSPENDED,
    // because a vendor nobody may use with no reason recorded is a decision that gets
    // quietly reversed by the next person.
    // Example: "Billed twice for the same September delivery, then denied it."
    private String statusReason;

    // The person the school actually deals with. Example: "Mahesh Kulkarni"
    private String contactPersonName;

    // Example: "+919820011223"
    @NotBlank
    private String primaryPhone;

    private String alternatePhone;

    // Example: "accounts@shreetraders.in"
    @Email
    private String email;

    // Where they are, for the delivery address on an order and for a tax invoice.
    // Example: "14, Ranade Road, Dadar West, Mumbai 400028"
    private String address;

    // Example: "Mumbai"
    private String city;

    // Example: "Maharashtra"
    private String state;

    // Example: "400028"
    private String postalCode;

    // The vendor's GST registration number, printed on a purchase order and needed for
    // the school to claim input credit. Unique inside the school when present, because
    // two vendor rows with one GSTIN is the same business entered twice.
    // Example: "27AABCS1429B1ZQ"
    private String gstin;

    // The vendor's PAN, needed when tax has to be deducted from a payment to them.
    // Example: "AABCS1429B"
    private String panNumber;

    // Where the money goes. Held here rather than in its own collection; see
    // VendorBankAccount for why, and for why its lookup hash matters most of all.
    @Valid
    private VendorBankAccount bankAccount;

    // How many days after a bill the school has agreed to pay. This is what a due date
    // is worked out from, so a payables list can be sorted by what is actually owed
    // now. Zero means payment on delivery. Example: 30
    @Builder.Default
    private Integer paymentTermDays = 0;

    // When the school first approved this vendor, and who did. Kept because "who let
    // this supplier in" is the first question asked when a supplier turns out badly.
    // Example: 2024-06-11
    private LocalDate approvedOn;

    // Links to Staff.id of whoever approved them.
    // Example: "67aa15d9dc3f7d0044444444"
    private String approvedByStaffDocsId;

    // Links to DocumentRecord.id for each paper the school holds on them: GST
    // certificate, a food safety licence for a caterer, a trade licence.
    // Example: ["67bd1123dc3f7d0022334455"]
    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();

    // Anything worth knowing about dealing with them.
    // Example: "Will not deliver on Sundays. Ring the day before."
    private String remarks;
}
