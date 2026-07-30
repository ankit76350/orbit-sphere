package com.orbitastra.backend.models.undone.a_new.compliance;

import java.time.Instant;
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

@Document(collection = "compliance_submissions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_submission_no_uniq",
                def = "{'tenantId':1,'submissionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_obligation_period_uniq",
                def = "{'tenantId':1,'complianceObligationDocsId':1,'reportingPeriodKey':1}", unique = true),
        @CompoundIndex(name = "tenant_submission_state_due_idx",
                def = "{'tenantId':1,'state':1,'dueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceSubmission extends CampusScopedDocument {

    private String submissionNo;
    private String complianceObligationDocsId;
    private String reportingPeriodKey;
    private LocalDate dueDate;
    private ApprovalState state;
    private String preparedByDocsId;
    private String approvedByDocsId;
    private Instant submittedAt;
    private String externalReference;
    private String externalStatus;
    private LocalDate validUntil;

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
