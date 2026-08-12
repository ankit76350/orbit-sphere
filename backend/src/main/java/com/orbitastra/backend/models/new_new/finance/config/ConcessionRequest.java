package com.orbitastra.backend.models.new_new.finance.config;

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

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.ApprovalStatus;
import com.orbitastra.backend.models.new_new.finance.enums.ConcessionScope;
import com.orbitastra.backend.models.new_new.finance.enums.ConcessionType;
import com.orbitastra.backend.models.new_new.finance.enums.RequesterType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
 * A request to give one student a discount, raised by a parent or by the fee desk
 * and decided by a member of staff.
 *
 * <p>Nothing comes off a student's fees until this record is APPROVED. That is
 * the whole reason it exists: a discount always has a named person who asked and
 * a named person who allowed it.
 *
 * <p>{@code scope} is what makes a discount standing or one-off, and it is the
 * field to read first:
 *
 * <ul>
 * <li>ACADEMIC_YEAR — invoice generation picks the request up by itself and
 * applies it to every eligible bill dated between {@code validFrom} and
 * {@code validUntil}. The family asks once and never again.</li>
 * <li>INVOICE — applies only to {@code targetInvoiceDocsId} and to nothing else.
 * This is the extra help a family asks for on one hard month, and it must not
 * leak into next month's bill.</li>
 * </ul>
 *
 * <p>There is no yearly ceiling and nothing to draw down. The discount is worked
 * out fresh from {@code percent} on every bill, so the same request applied to a
 * bigger tuition amount simply gives a bigger discount. Money that has a limit
 * belongs in AidAward, which tracks what is left of a fund.
 *
 * <p>The rate and the eligible heads are copied from the policy when the request
 * is raised, and the approver may change them before approving. Whatever is
 * stored here at approval time is what invoices use, so later edits to the policy
 * do not quietly change a discount that was already granted.
 *
 * <p>{@code appliedFeeHeadDocsIds} has to name at least one head. Anything not on
 * that list is billed in full, which is what keeps a tuition concession off the
 * transport and hostel lines.
 *
 * <p>{@code concessionPolicyDocsId} being null means a discount with no standing
 * policy behind it. Those still need an approver, and {@code reason} becomes the
 * only record of why it was allowed.
 *
 * <p>A student may hold only one APPROVED ACADEMIC_YEAR request per policy per
 * year, which the unique index enforces. It deliberately covers approved rows
 * only, so a request that was turned down or taken back does not block the family
 * from asking again with better paperwork, and it deliberately skips INVOICE rows,
 * so a family may ask for one-off help as often as the month demands.
 *
 * <p>The service checks that an ACADEMIC_YEAR request carries both validity dates
 * and no target invoice, that an INVOICE request carries a target invoice and no
 * validity dates, that the rate matches the type, that a GUARDIAN never appears as
 * the approver, and that the approver is not the person who raised it.
 */
@Document(collection = "concession_requests")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_concession_request_no_uniq",
                def = "{'schoolId': 1, 'requestNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_policy_concession_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'concessionPolicyDocsId': 1}",
                unique = true,
                partialFilter = "{'concessionPolicyDocsId': {'$type': 'string'}, 'status': 'APPROVED', 'scope': 'ACADEMIC_YEAR'}"),
        @CompoundIndex(
                name = "school_year_student_concession_apply_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'status': 1, 'scope': 1, 'validFrom': 1}"),
        @CompoundIndex(
                name = "school_concession_target_invoice_idx",
                def = "{'schoolId': 1, 'targetInvoiceDocsId': 1, 'status': 1}",
                partialFilter = "{'targetInvoiceDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_year_concession_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'requestedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConcessionRequest extends AcademicStudentSchoolBase {

    // Example: "CON/2026/000014"
    @NotBlank
    private String requestNo;

    // Whether this is a year-long discount or help on one bill.
    // Example: ConcessionScope.ACADEMIC_YEAR
    @NotNull
    @Builder.Default
    private ConcessionScope scope = ConcessionScope.ACADEMIC_YEAR;

    // Links to FeeInvoice.id. Set only when the scope is INVOICE.
    // Example: "67ad5566dc3f7d0055667788"
    private String targetInvoiceDocsId;

    // Links to ConcessionPolicy.id. Null for a discount with no policy behind it.
    // Example: "67ac6677dc3f7d0022334455"
    private String concessionPolicyDocsId;

    // Copied from the policy when the request was raised.
    // Example: ConcessionType.PERCENT
    @NotNull
    private ConcessionType concessionType;

    // Share to take off when the type is PERCENT. A full waiver is 100.00 here.
    // Example: 25.00
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percent;

    // Money to take off one bill when the type is FIXED_AMOUNT. This is per
    // invoice, and it is never more than the eligible amount on that invoice.
    // Example: 5000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fixedAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;


    //! here paranet/staff will request for all type of the fee head mneasn type of the fee schhol take ....
    // Heads this request may reduce. At least one, and every head left off this
    // list is billed in full. Example: the id of the TUITION head
    @NotEmpty
    @Builder.Default
    private List<String> appliedFeeHeadDocsIds = new ArrayList<>();

    // First invoice date this discount may be used on. Needed when the scope is
    // ACADEMIC_YEAR, left null when it is INVOICE. Example: 2026-04-01
    private LocalDate validFrom;

    // Last invoice date this discount may be used on. Needed when the scope is
    // ACADEMIC_YEAR, left null when it is INVOICE. Example: 2027-03-31
    private LocalDate validUntil;

    // Example: ApprovalStatus.APPROVED
    @NotNull
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    // Why the discount is being asked for.
    // Example: "Elder sibling already studying in class VIII."
    @NotBlank
    private String reason;

    // Proof the family gave. Links to DocumentRecord.id.
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Whether the parent asked from the portal or the fee desk raised it.
    // Example: RequesterType.GUARDIAN
    @NotNull
    private RequesterType requestedByType;

    // Links to Guardian.id or to the staff identity, depending on
    // requestedByType. Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String requestedByDocsId;

    // Date and time the family or the fee desk submitted it.
    // Example: 2026-08-15T05:20:00Z
    @NotNull
    private Instant requestedAt;

    // Links to the staff identity that decided it. Never a guardian, and never
    // the same person who raised it, which the service checks.
    // Example: "67aa15d9dc3f7d0055555555"
    private String reviewedByDocsId;

    // When it was approved or turned down. Example: 2026-08-17T09:10:00Z
    private Instant reviewedAt;

    // Note left by the approver, and the reason when it is turned down.
    // Example: "Approved for this year only; recheck at renewal."
    private String reviewRemarks;
}
