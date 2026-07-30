package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "receivable_invoices")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_invoice_no_uniq",
                def = "{'tenantId':1,'invoiceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_status_due_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'dueDate':1}"),
        @CompoundIndex(name = "tenant_status_due_idx",
                def = "{'tenantId':1,'status':1,'dueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableInvoice extends AcademicScopedDocument {

    private String invoiceNo;
    private String studentDocsId;
    private String guardianDocsId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currencyCode;
    private String status;
    private BigDecimal subTotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private BigDecimal allocatedPaymentTotal;
    private BigDecimal outstandingAmount;
    private String sourceType;
    private String sourceDocsId;
    private String journalEntryDocsId;
    private String invoiceDocumentDocsId;
    private String reversalOfInvoiceDocsId;

    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLine {
        private Integer lineNo;
        private String feeHeadDocsId;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        private String revenueAccountDocsId;
    }
}
