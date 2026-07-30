package com.orbitastra.backend.models.undone.a_new.cms;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

@Document(collection = "website_content_revisions")
@CompoundIndex(name = "tenant_entry_locale_revision_uniq",
        def = "{'tenantId':1,'contentEntryDocsId':1,'locale':1,'revisionNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRevision extends TenantScopedDocument {

    private String contentEntryDocsId;
    private String locale;
    private Integer revisionNo;
    private String title;
    private String slug;
    private String summary;
    private ApprovalState state;
    private String authoredByDocsId;
    private String reviewedByDocsId;
    private String approvedByDocsId;
    private Instant approvedAt;
    private String accessibilityStatus;
    private String seoTitle;
    private String seoDescription;
    private String checksum;

    @Builder.Default
    private Map<String, Object> contentBlocks = new HashMap<>();
}
