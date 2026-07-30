package com.orbitastra.backend.models.undone.a_working.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.payroll.enums.SalaryRevisionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection = "salary_revisions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRevision extends SchoolBase {

    @Indexed
    private String staffDocsId;

    /**
     * Previous Gross Salary
     */
    private BigDecimal previousGrossSalary;

    /**
     * New Gross Salary
     */
    private BigDecimal newGrossSalary;

    /**
     * Increment / Promotion etc.
     */
    private SalaryRevisionType revisionType;

    /**
     * Effective Date
     */
    private LocalDate effectiveFromDate;

    /**
     * Approved By
     */
    private String approvedByDocsId;

    /**
     * Remarks
     */
    private String remarks;
}