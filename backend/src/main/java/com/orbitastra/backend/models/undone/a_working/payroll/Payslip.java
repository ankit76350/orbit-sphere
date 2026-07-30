package com.orbitastra.backend.models.undone.a_working.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.payroll.enums.PayslipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One staff member's payslip for one month — a snapshot of computed earnings
 * and
 * statutory deductions produced by a payroll run.
 */
@Document(collection = "payslips")
@CompoundIndex(
    name = "staff_payroll_unique",
    def = "{'staffDocsId':1,'year':1,'month':1}",
    unique = true
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends SchoolBase {

    /**
     * Payroll Run
     */
    @Indexed
    private String payrollRunDocsId;

    /**
     * Staff
     */
    @Indexed
    private String staffDocsId;

    /**
     * Payroll Year
     */
    private Integer year;

    /**
     * Payroll Month
     */
    private Integer month;

    /**
     * Earnings
     */
    private BigDecimal basicSalary;

    private BigDecimal hra;

    private BigDecimal da;

    private BigDecimal specialAllowance;

    /**
     * Deductions
     */
    private BigDecimal pf;

    private BigDecimal esi;

    private BigDecimal professionalTax;

    private BigDecimal tds;

    private BigDecimal totalDeductions;

    /**
     * Gross Salary
     */
    private BigDecimal grossSalary;

    /**
     * Net Salary
     */
    private BigDecimal netSalary;

    /**
     * Payslip generated date.
     */
    private LocalDate generatedDate;

    /**
     * Salary payment date.
     */
    private LocalDate paymentDate;

    /**
     * Status
     */
    @Builder.Default
    private PayslipStatus status = PayslipStatus.GENERATED;

    /**
     * Remarks
     */
    private String remarks;
}