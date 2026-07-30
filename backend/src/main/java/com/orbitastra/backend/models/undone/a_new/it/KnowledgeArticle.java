package com.orbitastra.backend.models.undone.a_new.it;

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

@Document(collection = "knowledge_articles")
@CompoundIndex(name = "tenant_article_key_version_uniq",
        def = "{'tenantId':1,'articleKey':1,'articleVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle extends TenantScopedDocument {

    private String articleKey;
    private Integer articleVersion;
    private String title;
    private String category;
    private String content;
    private ApprovalState state;
    private String ownerDocsId;
    private Instant publishedAt;

    @Builder.Default
    private List<String> audienceRoleKeys = new ArrayList<>();

    @Builder.Default
    private List<String> searchTags = new ArrayList<>();
}
