package com.orbitastra.backend.models.undone.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A staff member's earning/deduction breakup master (CTC components), from
 * which
 * each month's {@link Payslip} is computed. The flat {@code staff.Staff.salary}
 * is only the headline figure; this decomposes it into statutory components.
 */
@Document(collection = "salary_structures")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructure extends SchoolBase {

    /**
     * Staff Member
     */
    @Indexed
    private String staffDocsId;

    /**
     * Monthly Basic Salary
     */
    private BigDecimal basicSalary;

    /**
     * House Rent Allowance
     */
    @Builder.Default
    private BigDecimal hra = BigDecimal.ZERO;

    /**
     * Dearness Allowance
     */
    @Builder.Default
    private BigDecimal da = BigDecimal.ZERO;

    /**
     * Special Allowance
     */
    @Builder.Default
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    /**
     * Provident Fund
     */
    @Builder.Default
    private BigDecimal pf = BigDecimal.ZERO;

    /**
     * Employee State Insurance
     */
    @Builder.Default
    private BigDecimal esi = BigDecimal.ZERO;

    /**
     * Professional Tax
     */
    @Builder.Default
    private BigDecimal professionalTax = BigDecimal.ZERO;

    /**
     * Tax Deducted at Source
     */
    @Builder.Default
    private BigDecimal tds = BigDecimal.ZERO;

    /**
     * Gross Salary
     */
    private BigDecimal grossSalary;

    /**
     * Cost To Company
     */
    private BigDecimal ctc;

    /**
     * Effective date.
     */
    private LocalDate effectiveFromDate;

    /**
     * Current active salary structure.
     */
    @Builder.Default
    private Boolean active = true;
}