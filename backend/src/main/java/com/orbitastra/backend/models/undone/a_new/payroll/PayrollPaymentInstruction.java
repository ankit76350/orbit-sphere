package com.orbitastra.backend.models.undone.a_new.payroll;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "payroll_payment_instructions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_result_payment_uniq",
                def = "{'tenantId':1,'payrollResultDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_batch_status_idx",
                def = "{'tenantId':1,'paymentBatchKey':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentInstruction extends TenantScopedDocument {

    private String payrollResultDocsId;
    private String paymentBatchKey;
    private String beneficiaryBankTokenReference;
    private String currencyCode;
    private BigDecimal amount;
    private String status;
    private String providerReference;
    private String failureCode;
    private Instant submittedAt;
    private Instant settledAt;
}
