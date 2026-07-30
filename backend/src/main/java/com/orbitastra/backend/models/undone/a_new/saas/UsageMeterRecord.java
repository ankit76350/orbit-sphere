package com.orbitastra.backend.models.undone.a_new.saas;

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

@Document(collection = "usage_meter_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_metric_window_source_uniq",
                def = "{'tenantId':1,'metricKey':1,'windowStart':1,'windowEnd':1,'sourceKey':1}", unique = true),
        @CompoundIndex(name = "tenant_usage_billing_idx",
                def = "{'tenantId':1,'billingStatus':1,'windowEnd':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMeterRecord extends TenantScopedDocument {

    private String metricKey;
    private String sourceKey;
    private Instant windowStart;
    private Instant windowEnd;
    private BigDecimal quantity;
    private String unit;
    private String billingStatus;
    private String billingInvoiceReference;
    private String evidenceHash;
}
