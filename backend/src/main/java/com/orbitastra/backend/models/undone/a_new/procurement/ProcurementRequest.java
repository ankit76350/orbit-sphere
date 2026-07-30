package com.orbitastra.backend.models.undone.a_new.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "procurement_requests")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_request_no_uniq",
                def = "{'tenantId':1,'requestNo':1}", unique = true),
        @CompoundIndex(name = "tenant_requester_state_due_idx",
                def = "{'tenantId':1,'requestedByDocsId':1,'state':1,'requiredBy':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementRequest extends CampusScopedDocument {

    private String requestNo;
    private String departmentDocsId;
    private String costCentreDocsId;
    private String budgetPlanDocsId;
    private String requestedByDocsId;
    private LocalDate requiredBy;
    private String justification;
    private String currencyCode;
    private BigDecimal estimatedTotal;
    private ApprovalState state;
    private String workflowRunDocsId;

    @Builder.Default
    private List<RequestLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestLine {
        private Integer lineNo;
        private String categoryCode;
        private String itemDocsId;
        private String description;
        private BigDecimal quantity;
        private String unitCode;
        private BigDecimal estimatedUnitPrice;
    }
}
