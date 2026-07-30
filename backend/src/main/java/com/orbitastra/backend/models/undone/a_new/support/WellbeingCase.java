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

@Document(collection = "wellbeing_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_wellbeing_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_wellbeing_owner_status_followup_idx",
                def = "{'tenantId':1,'caseOwnerDocsId':1,'status':1,'nextReviewDate':1}"),
        @CompoundIndex(name = "tenant_student_wellbeing_status_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'openedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WellbeingCase extends CampusScopedDocument {

    public enum CaseStatus {
        REFERRED,
        TRIAGED,
        ACTIVE,
        ON_HOLD,
        ESCALATED,
        CLOSED
    }

    private String caseNo;
    private String studentDocsId;
    private String referralSourceType;
    private String referralSourceDocsId;
    private String caseOwnerDocsId;
    private CaseStatus status;
    private Confidentiality confidentiality;
    private String riskLevel;
    private Instant openedAt;
    private LocalDate nextReviewDate;
    private Instant closedAt;
    private String closureReason;
    private String safeguardingCaseDocsId;
    private String consentRecordDocsId;
    private String retentionRuleDocsId;

    @Builder.Default
    private List<String> permittedPrincipalDocsIds = new ArrayList<>();
}
