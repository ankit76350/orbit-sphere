package com.orbitastra.backend.models.undone.a_new.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "safeguarding_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_safeguarding_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_dsl_status_review_idx",
                def = "{'tenantId':1,'designatedLeadDocsId':1,'status':1,'nextReviewDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SafeguardingCase extends CampusScopedDocument {

    public enum SafeguardingStatus {
        RECEIVED,
        IMMEDIATE_TRIAGE,
        ASSESSMENT,
        REFERRED,
        PROTECTION_PLAN,
        MONITORING,
        CLOSED
    }

    private String caseNo;
    private String subjectPersonType;
    private String subjectPersonDocsId;
    private String reporterType;
    private String reporterDocsId;
    private Boolean reporterIdentityProtected;
    private String designatedLeadDocsId;
    private SafeguardingStatus status;
    private Confidentiality confidentiality;
    private String riskLevel;
    private Instant receivedAt;
    private Boolean immediateDanger;
    private String statutoryCategory;
    private LocalDate nextReviewDate;
    private Instant closedAt;
    private String retentionRuleDocsId;
    private String legalHoldDocsId;

    @Builder.Default
    private List<String> permittedPrincipalDocsIds = new ArrayList<>();
}
