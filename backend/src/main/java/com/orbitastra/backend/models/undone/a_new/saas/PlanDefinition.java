package com.orbitastra.backend.models.undone.a_new.saas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AuditedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "plan_definitions")
@CompoundIndex(name = "plan_code_version_uniq",
        def = "{'planCode':1,'planVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDefinition extends AuditedDocument {

    private String planCode;
    private Integer planVersion;
    private String name;
    private String billingPeriod;
    private BigDecimal listPrice;
    private String currencyCode;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private Boolean publiclyAvailable;

    @Builder.Default
    private List<PlanEntitlement> entitlements = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanEntitlement {
        private String featureKey;
        private Boolean enabled;
        private Long quota;
        private String quotaUnit;
        private String overagePolicy;
    }
}
