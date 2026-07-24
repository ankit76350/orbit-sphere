package com.orbitastra.backend.models.undone.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A staff member's earning/deduction breakup master (CTC components), from which
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

    @Indexed
    private String staffDocsId;

    // Earnings
    private BigDecimal basic;
    private BigDecimal hra;      // House Rent Allowance
    private BigDecimal da;       // Dearness Allowance
    private BigDecimal special;  // Special allowance

    // Statutory deduction components
    private BigDecimal pf;       // Provident Fund
    private BigDecimal esi;      // Employees' State Insurance
    private BigDecimal pt;       // Professional Tax

    private LocalDate effectiveFrom;
}
