package com.orbitastra.backend.models.undone.a_new.legal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "insurance_policies")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_policy_no_uniq",
                def = "{'tenantId':1,'policyNo':1}", unique = true),
        @CompoundIndex(name = "tenant_insurance_expiry_idx",
                def = "{'tenantId':1,'status':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePolicy extends TenantScopedDocument {

    private String policyNo;
    private String insurerVendorDocsId;
    private String policyType;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private BigDecimal insuredAmount;
    private BigDecimal premiumAmount;
    private String currencyCode;
    private String status;
    private String policyDocumentDocsId;

    @Builder.Default
    private List<CoveredObject> coveredObjects = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoveredObject {
        private String objectType;
        private String objectDocsId;
        private BigDecimal coverageLimit;
    }
}
