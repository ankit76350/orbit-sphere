package com.orbitastra.backend.models.undone.a_new.ai;

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

@Document(collection = "ai_prompt_versions")
@CompoundIndex(name = "tenant_prompt_key_version_uniq",
        def = "{'tenantId':1,'promptKey':1,'promptVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion extends TenantScopedDocument {

    private String promptKey;
    private Integer promptVersion;
    private String aiUseCaseDocsId;
    private String name;
    private String systemTemplate;
    private String userTemplate;
    private String outputSchema;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String checksum;

    @Builder.Default
    private List<String> requiredVariables = new ArrayList<>();

    @Builder.Default
    private List<String> forbiddenDataCategoryKeys = new ArrayList<>();
}
