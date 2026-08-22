package com.orbitastra.backend.models.new_new.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.inventory.enums.IssuedToType;
import com.orbitastra.backend.models.new_new.inventory.enums.StockIssueStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Something given out that is expected back.
 *
 * <p>Only for NON_CONSUMABLE items. Issuing chalk is the end of the story and needs
 * nothing beyond a movement; issuing a microscope, a football or ten bedsheets is not.
 *
 * <p>So a consumable issue is **one StockMovement and nothing else**, while a
 * non-consumable issue is a StockMovement plus one of these to say who has it and whether
 * it came back. That keeps the common case cheap instead of putting return fields on every
 * bag of rice.
 *
 * <p>Same shape as the library: BookIssued tracks a book that has to come back, and this
 * tracks everything else that does. The difference is quantity — a library issues one copy,
 * a store issues ten bedsheets and may get eight back.
 *
 * <p>{@code quantityReturned} is what makes partial returns sayable. Eight bedsheets back
 * out of ten is the normal outcome at the end of a term, and a status alone could not
 * express it.
 *
 * <p>NOT_RETURNED is the state that matters. With only ISSUED and RETURNED, everything
 * never given back would sit as ISSUED forever, so the list of things lost would look
 * exactly like the list of things in use. Writing it off is a decision, so it carries an
 * approver and a reason.
 *
 * <p>{@code issuedToType} covers places as well as people, because both are real. Ten
 * bedsheets go to a hostel room rather than to a child, and a box of chalk goes to the
 * science department rather than to whoever collected it.
 *
 * <p>{@code replacementCharge} is what a family or a member of staff is asked to pay for
 * something lost. It is recorded here and billed by finance under a head with
 * FeeCategory.FINE, the same route a library fine and a conduct fine already take. Whether
 * it has been paid is finance's answer, not kept here.
 *
 * <p>The service checks that only a NON_CONSUMABLE item is issued this way, that the
 * returned quantity never exceeds the issued quantity, that returning writes a RETURN
 * movement, and that writing something off carries an approver and a reason.
 */
@Document(collection = "stock_issues")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_stock_issue_no_uniq",
                def = "{'schoolId': 1, 'issueNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_stock_issue_holder_idx",
                def = "{'schoolId': 1, 'issuedToType': 1, 'issuedToDocsId': 1, 'issuedOn': -1}"),
        @CompoundIndex(
                name = "school_stock_issue_outstanding_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueBackOn': 1}"),
        @CompoundIndex(
                name = "school_stock_issue_item_idx",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockIssue extends SchoolBase {

    // School-scoped number from NumberSequence type STOCK_ISSUE, quoted on the slip the
    // person signs. Example: "SI/2026/001482"
    @NotBlank
    private String issueNo;

    // Links to InventoryItem.id. Example: "67bc112adc3f7d0099001122"
    @NotBlank
    private String inventoryItemDocsId;

    // Links to InventoryStore.id it came out of.
    // Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String inventoryStoreDocsId;

    // Whether it went to a person, a department or a place.
    // Example: IssuedToType.HOSTEL_ROOM
    @NotNull
    private IssuedToType issuedToType;

    // Links to Staff.id, Student.id, Department.id, HostelRoom.id or SchoolClass.id,
    // depending on issuedToType. Example: "67ba1123dc3f7d0022334455"
    @NotBlank
    private String issuedToDocsId;

    // How much went out. Example: 10.000
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityIssued;

    // How much has come back so far. Partial returns are the normal case at the end of
    // a term. Example: 8.000
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal quantityReturned = BigDecimal.ZERO;

    // Example: StockIssueStatus.PARTIALLY_RETURNED
    @NotNull
    @Builder.Default
    private StockIssueStatus status = StockIssueStatus.ISSUED;

    // The day it went out. Example: 2026-04-10
    @NotNull
    private LocalDate issuedOn;

    // When it should be back. Worked out from the item's defaultReturnDays where there
    // is one. Null for something with no fixed return date, such as room linen.
    // Example: 2027-03-25
    private LocalDate dueBackOn;

    // The day the last part of it came back. Example: 2027-03-24
    private LocalDate lastReturnedOn;

    // Why it is being asked for. Example: "Inter-house football tournament."
    private String purpose;

    // What is owed for anything not returned or returned broken. Billed by finance
    // under a FINE head; whether it has been paid is finance's answer. Example: 450.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal replacementCharge;

    // Links to FeeInvoice.id once finance has billed the replacement charge. Null means
    // decided and never charged. Example: "67ad2233dc3f7d0022334455"
    private String feeInvoiceDocsId;

    // Links to Staff.id for whoever handed it over.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String issuedByStaffDocsId;

    // Links to Staff.id for whoever took it back.
    // Example: "67aa15d9dc3f7d0044444444"
    private String returnedToStaffDocsId;

    // Links to Staff.id for whoever agreed to write off what never came back.
    // Example: "67aa15d9dc3f7d0055555555"
    private String writeOffApprovedByStaffDocsId;

    // Example: 2027-04-02T06:00:00Z
    private Instant writeOffAt;

    // Why it was written off. Required when the status is NOT_RETURNED.
    // Example: "Two bedsheets missing at the end of term; charged to the room."
    private String writeOffReason;

    // Example: "Ball returned with a split seam; usable for practice only."
    private String remarks;
}
