package com.orbitastra.backend.models.undone.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.payroll.enums.PayslipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One staff member's payslip for one month — a snapshot of computed earnings and
 * statutory deductions produced by a payroll run.
 */
@Document(collection = "payslips")
@CompoundIndex(name = "staff_month_uniq", def = "{'staffId': 1, 'month': 1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends SchoolBase {

    @Indexed
    private String staffId;

    // Pay period in "YYYY-MM" form, e.g. "2026-05".
    @Indexed
    private String month;

    // Earnings
    private BigDecimal gross;
    private BigDecimal basic;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal special;

    // Statutory + other deductions
    private BigDecimal pf;
    private BigDecimal esi;
    private BigDecimal pt;
    private BigDecimal tds;
    private BigDecimal deductions; // total deductions

    private BigDecimal net;        // net pay

    @Builder.Default
    private PayslipStatus status = PayslipStatus.GENERATED;
}
