package com.orbitastra.backend.models.undone.a_new.cms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "website_content_entries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_site_entry_key_uniq",
                def = "{'tenantId':1,'websiteSiteDocsId':1,'entryKey':1}", unique = true),
        @CompoundIndex(name = "tenant_site_type_status_idx",
                def = "{'tenantId':1,'websiteSiteDocsId':1,'contentType':1,'status':1,'publishedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntry extends TenantScopedDocument {

    private String websiteSiteDocsId;
    private String entryKey;
    private String contentType;
    private String parentEntryDocsId;
    private String status;
    private String ownerDocsId;
    private String currentRevisionDocsId;
    private Instant publishedAt;
    private Instant archiveAt;
    private String sourceEntityType;
    private String sourceEntityDocsId;

    @Builder.Default
    private List<String> audienceKeys = new ArrayList<>();
}
