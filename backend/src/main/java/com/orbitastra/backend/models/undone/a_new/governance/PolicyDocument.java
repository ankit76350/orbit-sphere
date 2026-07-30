package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "policy_documents")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_policy_version_uniq",
                def = "{'tenantId':1,'policyKey':1,'policyVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_policy_review_idx",
                def = "{'tenantId':1,'state':1,'nextReviewDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDocument extends TenantScopedDocument {

    private String policyKey;
    private Integer policyVersion;
    private String title;
    private String ownerDocsId;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private LocalDate effectiveDate;
    private LocalDate nextReviewDate;
    private String contentDocumentDocsId;
    private String supersedesPolicyDocsId;

    @Builder.Default
    private List<String> applicableScopeKeys = new ArrayList<>();

    @Builder.Default
    private List<String> acknowledgementAudienceKeys = new ArrayList<>();
}
