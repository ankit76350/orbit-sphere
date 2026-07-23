package com.orbitastra.backend.models.finance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcadmicStudentSchoolBase;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.FeeType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "fee_invoices")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeInvoice extends AcadmicStudentSchoolBase {

    // Human-readable invoice number (e.g. "INV-A1B2C3D4"), generated on creation.
    // The Mongo id stays the technical key; this is what appears on the printed bill.
    @Indexed
    private String invoiceNo;

    private FeeType type;

    private BigDecimal amount;

    // Concession applied to this invoice (sibling waiver, scholarship, staff-child, etc.).
    // Net payable = amount - discount; paidAmount/status are measured against the net payable.
    private BigDecimal discount;

    private BigDecimal paidAmount;

    private LocalDate dueDate;

    private FeeStatus status;
}
