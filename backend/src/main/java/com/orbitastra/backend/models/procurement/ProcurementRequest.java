package com.orbitastra.backend.models.new_new.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.procurement.embedded.ProcurementQuote;
import com.orbitastra.backend.models.new_new.procurement.embedded.ProcurementRequestLine;
import com.orbitastra.backend.models.new_new.procurement.enums.ProcurementRequestStatus;
import com.orbitastra.backend.models.new_new.procurement.enums.ProcurementUrgency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A department asking to buy something, before anybody has spent anything.
 *
 * <p>This is the control in the whole package, and it is the model a simpler design would
 * skip. Without it the first record of a purchase is the purchase order — which means the
 * first record of a purchase is the school already being committed to it. Money is agreed
 * to here, not at the vendor's door.
 *
 * <p>It also answers a question nothing else can: what did a department ask for and not
 * get? A kitchen that has asked for a new mixer four times and been refused four times is
 * something a head should be able to see, and a design that deletes refused requests hides
 * it. So REJECTED is a state, not a deletion.
 *
 * <p>{@code quotes} is what replaces formal tendering. The reference sketch had
 * SourcingEvent and VendorBid as two separate collections, which is a sealed-bid process a
 * school does not run. What a school actually does is ring three shops and write down what
 * each said, so three quotes on the request is the honest model of it — and keeping the
 * losing quotes is the point, because one price proves nothing and three prices with a
 * choice made between them proves something.
 *
 * <p>A request is not tied to an academic year. Buying happens on a financial calendar and
 * a sack of rice does not belong to a school session, so this extends SchoolBase and dates
 * itself with {@code requestedOn}.
 *
 * <p>{@code urgency} decides which approval path the request takes, and EMERGENCY exists so
 * that buying first and approving afterwards is a countable, visible thing rather than
 * something people do quietly. A school with many emergency purchases has a planning
 * problem and should be able to see it.
 *
 * <p>{@code estimatedTotalAmount} is added up from the lines and is a guess, deliberately.
 * Its job is to give an approver a figure to react to. What is actually paid is settled on
 * the purchase order.
 *
 * <p>The service checks that a submitted request has at least one line, that approval is
 * recorded with a person and a time, that a rejection carries a reason, that at most one
 * quote is marked selected, that a selected quote dearer than the cheapest carries a
 * selection note, and that the status follows the ordered quantities on the lines rather
 * than being set by hand.
 */
@Document(collection = "procurement_requests")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_procurement_request_no_uniq",
                def = "{'schoolId': 1, 'requestNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_procurement_request_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'requiredByDate': 1}"),
        @CompoundIndex(
                name = "school_procurement_request_dept_idx",
                def = "{'schoolId': 1, 'requestingDepartmentDocsId': 1, 'requestedOn': -1}"),
        @CompoundIndex(
                name = "school_procurement_request_urgency_idx",
                def = "{'schoolId': 1, 'urgency': 1, 'status': 1, 'requestedOn': -1}"),
        @CompoundIndex(
                name = "school_procurement_request_requester_idx",
                def = "{'schoolId': 1, 'requestedByStaffDocsId': 1, 'requestedOn': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementRequest extends SchoolBase {

    // School-scoped number from NumberSequence type PROCUREMENT_REQUEST.
    // Example: "PR/2026/000118"
    @NotBlank
    private String requestNo;

    // Links to Department.id that wants this. The budget conversation happens by
    // department, so the link is to the department and not just to the person.
    // Example: "67aa2211dc3f7d0011223344"
    @NotBlank
    private String requestingDepartmentDocsId;

    // Links to Staff.id of whoever raised it.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String requestedByStaffDocsId;

    // The day it was raised. Example: 2026-08-14
    @NotNull
    private LocalDate requestedOn;

    // When the department needs it by, which is what an approval queue should be sorted
    // on. Example: 2026-08-22
    private LocalDate requiredByDate;

    // How soon it is needed, and therefore how much checking it gets.
    // Example: ProcurementUrgency.NORMAL
    @NotNull
    @Builder.Default
    private ProcurementUrgency urgency = ProcurementUrgency.NORMAL;

    // Why the school needs this at all, in the requester's words. The sentence an
    // approver actually reads. Example: "Monthly provisions for the hostel kitchen."
    @NotBlank
    private String purpose;

    // What is being asked for. At least one, because a request for nothing is not a
    // request.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<ProcurementRequestLine> lines = new ArrayList<>();

    // The prices that were compared before choosing. Empty is allowed for a small
    // routine purchase; a school that wants three quotes on everything above a figure
    // enforces that in the service, because the figure is a school policy and not a
    // property of this model.
    @Valid
    @Builder.Default
    private List<ProcurementQuote> quotes = new ArrayList<>();

    // The line estimates added up. A guess, for an approver to react to.
    // Example: 12500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedTotalAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Where the request has got to. Example: ProcurementRequestStatus.APPROVED
    @NotNull
    @Builder.Default
    private ProcurementRequestStatus status = ProcurementRequestStatus.DRAFT;

    // When it was sent for approval, which is the clock an approval queue is measured
    // against. Example: 2026-08-14T06:20:00Z
    private Instant submittedAt;

    // Links to Staff.id of whoever approved or refused it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String decidedByStaffDocsId;

    // When they did. Example: 2026-08-15T04:10:00Z
    private Instant decidedAt;

    // Why it was refused. Required for REJECTED, because a refusal with no reason is
    // one the department will simply raise again next week.
    // Example: "Buy after the September fee collection; nothing left in this quarter."
    private String rejectionReason;

    // Why it was withdrawn. Required for CANCELLED.
    // Example: "Kitchen found a full sack in the back store."
    private String cancellationReason;

    // Links to DocumentRecord.id for anything attached: a photograph of the broken
    // thing, a written quotation, a specification sheet.
    // Example: ["67bd1123dc3f7d0022334455"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    // Anything else worth knowing.
    // Example: "Same list as July, less the oil."
    private String remarks;
}
