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
import com.orbitastra.backend.models.new_new.finance.enums.ConcessionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A request to give one student a discount, raised by one staff member and
 * decided by another.
 *
 * <p>Nothing comes off a student's fees until this record is APPROVED. That is
 * the whole reason it exists: a discount always has a named person who asked and
 * a named person who allowed it.
 *
 * <p>The amounts are copied from the policy when the request is raised, and the
 * approver may change them. Whatever is stored here at approval time is what the
 * invoice uses, so later edits to the policy do not quietly change a discount
 * that was already granted.
 *
 * <p>{@code concessionPolicyDocsId} being null means a one-off discount with no
 * standing policy behind it. Those still need an approver, and the reason field
 * becomes the only record of why it was allowed.
 *
 * <p>A student may hold only one APPROVED request per policy per year, which the
 * unique index enforces. It deliberately covers approved rows only, so a request
 * that was turned down or taken back does not block the family from asking again
 * with better paperwork.
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
                partialFilter = "{'concessionPolicyDocsId': {'$type': 'string'}, 'status': 'APPROVED'}"),
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

    // Links to ConcessionPolicy.id. Null for a one-off discount.
    // Example: "67ac6677dc3f7d0022334455"
    private String concessionPolicyDocsId;

    // Copied from the policy when the request was raised.
    // Example: ConcessionType.PERCENT
    @NotNull
    private ConcessionType concessionType;

    // Share to take off when the type is PERCENT. Example: 25.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percent;

    // Money to take off when the type is FIXED_AMOUNT. Example: 5000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fixedAmount;

    // Most that may be taken off across the year under this request.
    // Example: 20000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumAmountPerYear;

    // Discount actually used up so far, added to as invoices are made.
    // Example: 7500.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Heads this request may reduce. Empty means every head the policy allows.
    @Builder.Default
    private List<String> appliedFeeHeadDocsIds = new ArrayList<>();

    // First date an invoice may use this discount. Example: 2026-04-01
    private LocalDate validFrom;

    // Last date an invoice may use this discount. Example: 2027-03-31
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

    // Links to the staff identity that raised the request.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String requestedByDocsId;

    // Example: 2026-04-02T05:20:00Z
    private Instant requestedAt;

    // Links to the staff identity that decided it. Must not be the same person
    // who raised it, which the service checks.
    // Example: "67aa15d9dc3f7d0055555555"
    private String reviewedByDocsId;

    // Example: 2026-04-03T09:10:00Z
    private Instant reviewedAt;

    // Note left by the approver, and the reason when it is turned down.
    // Example: "Approved for this year only; recheck at renewal."
    private String reviewRemarks;
}
