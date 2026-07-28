package com.orbitastra.backend.models.undone.a_working.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.payroll.enums.PayrollRunStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "payroll_runs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun extends SchoolBase {

    /**
     * Payroll Year
     */
    private Integer year;

    /**
     * Payroll Month
     */
    private Integer month;

    /**
     * Payroll generated date.
     */
    private LocalDate generatedDate;

    /**
     * Payroll processed by.
     */
    private String processedByDocsId;

    /**
     * Total Employees
     */
    private Integer totalEmployees;

    /**
     * Total Gross Salary
     */
    private BigDecimal totalGrossSalary;

    /**
     * Total Net Salary
     */
    private BigDecimal totalNetSalary;

    /**
     * Payroll Status
     */
    @Builder.Default
    private PayrollRunStatus status = PayrollRunStatus.GENERATED;
}