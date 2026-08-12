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
 * A request to give one student a discount. A parent or the fee desk asks for it,
 * and a staff member says yes or no.
 *
 * <p>No money comes off a student's fees until this record is APPROVED. That is
 * why the record exists. Every discount has a name against who asked for it and a
 * name against who allowed it.
 *
 * <p>Read {@code scope} first. It says how far the discount reaches:
 *
 * <ul>
 * <li>ACADEMIC_YEAR — the billing job finds this request on its own and takes the
 * discount off every matching bill dated between {@code validFrom} and
 * {@code validUntil}. The family asks once and does not have to ask again.</li>
 * <li>INVOICE — the discount comes off only the one bill named in
 * {@code targetInvoiceDocsId}, and off nothing else. This is extra help for one
 * hard month, and it must not carry over to the next bill.</li>
 * </ul>
 *
 * <p>This record does not hold a limit and does not hold a running total. The
 * discount is worked out again from {@code percent} on every bill, so a bigger
 * tuition amount simply gives a bigger discount.
 *
 * <p>If the school wants a yearly limit, it is set on the fee head as
 * {@code maximumConcessionPerYear}. How much of that limit is already used is
 * worked out by adding up the discounts on the student's bills. We do not keep a
 * total here, because a total has to be put right every time a bill is voided or
 * reversed, and if we forget to do that the student loses discount they should
 * have got. Adding up the bill lines is always right.
 *
 * <p>The rate and the fee heads are copied from the policy when the request is
 * made, and the approver may change them before saying yes. Whatever is saved here
 * at that moment is what the bills use, so changing the policy later does not
 * change a discount that was already given.
 *
 * <p>{@code appliedFeeHeadDocsIds} must name at least one fee head. Anything not
 * in that list is charged in full. This is what stops a tuition discount from also
 * coming off transport and hostel.
 *
 * <p>{@code concessionPolicyDocsId} being null means there is no policy behind
 * this discount. It still needs an approver, and {@code reason} is then the only
 * record of why it was allowed.
 *
 * <p>A student can have only one APPROVED ACADEMIC_YEAR request per policy per
 * year, and the unique index makes sure of it. The index only looks at approved
 * rows, so a request that was refused or taken back does not stop the family
 * asking again with better papers. It also skips INVOICE rows, so a family can ask
 * for one-off help as often as they need to.
 *
 * <p>The service checks all of this: an ACADEMIC_YEAR request has both dates and
 * no target bill, an INVOICE request has a target bill and no dates, the rate
 * matches the type, a guardian is never the approver, and the approver is not the
 * same person who asked.
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
