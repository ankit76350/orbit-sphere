package com.orbitastra.backend.models.new_new.finance.config.embedded;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One due date in a FeeStructure, and the share of the year's fee that falls on
 * it.
 *
 * <p>The lines of a structure say what is charged; the installments say when it
 * has to be paid. Each installment produces one FeeInvoice per student, which is
 * why {@code installmentNo} is stored on the invoice as well.
 *
 * <p>{@code sharePercent} is used when the structure splits the yearly total by
 * a share, such as four equal quarters of 25 percent each. {@code fixedAmount}
 * is used instead when the school states exact amounts per installment. Only one
 * of the two should be set, which the service checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeInstallment {

    // Position in the year, starting at 1. Example: 1
    @NotNull
    private Integer installmentNo;

    // Name shown to parents. Example: "Term 1 - April to June"
    @NotBlank
    private String label;

    // Links to AcademicTerm.id when the installment lines up with a term.
    // Null when the school bills monthly and not by term.
    // Example: "67ab5511dc3f7d0099887766"
    private String termDocsId;

    // Date the money has to reach the school by. Example: 2026-04-10
    @NotNull
    private LocalDate dueDate;

    // Date invoices for this installment should be sent out. Example: 2026-04-01
    private LocalDate invoiceDate;

    // Share of the year's total that falls on this installment. Example: 25.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal sharePercent;

    // Exact amount for this installment, used instead of a share. Example: 7500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fixedAmount;

    // Days after the due date before a late fee starts. Example: 7
    private Integer graceDays;

    // Late-payment charge added once the grace days run out. Example: 100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lateFeeAmount;
}
