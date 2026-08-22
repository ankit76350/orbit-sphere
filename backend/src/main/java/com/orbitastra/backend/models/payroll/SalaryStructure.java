package com.orbitastra.backend.models.payroll;

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

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.payroll.embedded.StructureComponent;
import com.orbitastra.backend.models.payroll.enums.SalaryRevisionType;

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
 * What one member of staff is paid, from one date until it changes.
 *
 * <p>A new structure is created every time somebody's pay changes, and the old one is
 * closed with an end date. **Structures are never edited.** A raise in April must not
 * change what March's payslip meant, and it will if the structure March was computed from
 * has been overwritten.
 *
 * <p>That chain of structures **is** the salary history. The reference sketch had a separate
 * SalaryRevision model recording the previous and new figures alongside a mutable
 * structure; that is the same history kept twice, in two places that can disagree, and the
 * revision copy would be the one nobody updates. So {@code revisionType},
 * {@code revisionReason} and {@code approvedByStaffDocsId} live here instead, on the
 * structure that resulted.
 *
 * <p>Only one structure per member of staff is current at a time, which the unique index
 * enforces.
 *
 * <p>{@code monthlyGrossAmount} and {@code monthlyCostToSchool} are worked out from the
 * components and stored, so a list of staff salaries loads without recomputing every row.
 * They must always be rebuildable from {@code components}.
 *
 * <p>The two totals are different questions and both are asked. Gross is earnings, and it
 * is what a bank statement and a payslip talk about. Cost to the school adds the employer's
 * own contributions, which never appear in anybody's take-home pay but are money the school
 * spends. Reporting one as the other is how a budget goes wrong.
 *
 * <p>Salary is the most private thing the school holds about its staff, more so than
 * anything in a personnel file. It sits behind its own PAYROLL module rather than the STAFF
 * one, because a head of department needs a colleague's timetable and must not see their
 * pay.
 *
 * <p>The service checks that exactly one component is marked as basic pay, that a
 * percentage component carries a rate and a fixed one an amount, that the totals match the
 * components, that dates do not overlap for one member of staff, and that a structure a
 * payslip was computed from is never edited or deleted.
 */
@Document(collection = "salary_structures")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_salary_structure_current_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1}",
                unique = true,
                partialFilter = "{'current': true}"),
        @CompoundIndex(
                name = "school_salary_structure_history_idx",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'effectiveFrom': -1}"),
        @CompoundIndex(
                name = "school_salary_structure_effective_idx",
                def = "{'schoolId': 1, 'effectiveFrom': 1, 'effectiveTo': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructure extends SchoolBase {

    // Links to Staff.id. Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String staffDocsId;

    // Every line of this person's pay, with the figure agreed for them. A structure with
    // no components pays nothing, so there must be at least one.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<StructureComponent> components = new ArrayList<>();

    // Total earnings each month, worked out from the components. What a payslip and a
    // bank statement talk about. Example: 48000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyGrossAmount;

    // Total deductions each month. Example: 6200.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyDeductionAmount = BigDecimal.ZERO;

    // What actually reaches the bank: gross minus deductions. Example: 41800.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyNetAmount;

    // What the school spends: earnings plus the employer's own contributions. Never the
    // same as gross, and never what the staff member is told they earn.
    // Example: 50880.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyCostToSchool;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // First day this applies. Example: 2026-04-01
    @NotNull
    private LocalDate effectiveFrom;

    // Last day it applied. Null while it is the current one. Example: 2027-03-31
    private LocalDate effectiveTo;

    // Whether this is the structure in force now. Older ones stay with this turned off,
    // which is what makes the history readable. Example: true
    @NotNull
    @Builder.Default
    private Boolean current = true;

    // Why the pay changed. Example: SalaryRevisionType.INCREMENT
    @NotNull
    @Builder.Default
    private SalaryRevisionType revisionType = SalaryRevisionType.INITIAL;

    // In the school's own words. Example: "Annual increment, 8 percent on basic."
    private String revisionReason;

    // Links to SalaryStructure.id this one replaced. Null for somebody's first.
    // Example: "67bd1123dc3f7d0022334455"
    private String supersedesStructureDocsId;

    // Links to the staff identity that approved this pay. A salary nobody approved is a
    // salary nobody will answer for. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String approvedByStaffDocsId;

    // Example: 2026-03-28T09:00:00Z
    @NotNull
    private Instant approvedAt;

    // Links to DocumentRecord.id for a signed letter of appointment or revision.
    // Example: "67bd1124dc3f7d0033445566"
    private String letterDocumentDocsId;

    // Example: "Agreed at the March management meeting."
    private String remarks;
}
