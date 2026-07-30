package com.orbitastra.backend.models.undone.a_new.ai;

import java.time.LocalDate;
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

@Document(collection = "ai_use_cases")
@CompoundIndex(name = "tenant_ai_use_case_key_version_uniq",
        def = "{'tenantId':1,'useCaseKey':1,'policyVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiUseCase extends TenantScopedDocument {

    private String useCaseKey;
    private Integer policyVersion;
    private String name;
    private String description;
    private String ownerDocsId;
    private String riskTier;
    private ApprovalState state;
    private Boolean childrenDataAllowed;
    private Boolean sensitiveDataAllowed;
    private Boolean automatedDecisionAllowed;
    private Boolean humanReviewRequired;
    private String requiredPermission;
    private String consentPurposeKey;
    private String dataProtectionAssessmentDocsId;
    private LocalDate nextReviewDate;
    private String killSwitchStatus;

    @Builder.Default
    private List<String> allowedRoleKeys = new ArrayList<>();

    @Builder.Default
    private List<String> prohibitedActions = new ArrayList<>();

    @Builder.Default
    private List<String> allowedKnowledgeSourceDocsIds = new ArrayList<>();
}
