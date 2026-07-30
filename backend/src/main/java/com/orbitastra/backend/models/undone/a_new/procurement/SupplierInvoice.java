package com.orbitastra.backend.models.undone.a_new.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "supplier_invoices")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_vendor_invoice_no_uniq",
                def = "{'tenantId':1,'vendorDocsId':1,'vendorInvoiceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_invoice_state_due_idx",
                def = "{'tenantId':1,'state':1,'dueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoice extends TenantScopedDocument {

    private String vendorDocsId;
    private String vendorInvoiceNo;
    private String purchaseOrderDocsId;
    private String goodsReceiptDocsId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currencyCode;
    private BigDecimal subTotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private ApprovalState state;
    private String matchStatus;
    private String varianceReason;
    private String journalEntryDocsId;
    private String invoiceDocumentDocsId;
}
