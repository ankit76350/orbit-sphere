package com.orbitastra.backend.models.undone.a_new.procurement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "sourcing_events")
@CompoundIndex(name = "tenant_sourcing_no_uniq",
        def = "{'tenantId':1,'sourcingNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingEvent extends TenantScopedDocument {

    private String sourcingNo;
    private String procurementRequestDocsId;
    private String title;
    private Instant opensAt;
    private Instant closesAt;
    private ApprovalState state;
    private String evaluationMethod;
    private String committeeDocsId;

    @Builder.Default
    private List<String> invitedVendorDocsIds = new ArrayList<>();

    @Builder.Default
    private List<EvaluationCriterion> criteria = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationCriterion {
        private String criterionKey;
        private String label;
        private Integer weightPercent;
    }
}
