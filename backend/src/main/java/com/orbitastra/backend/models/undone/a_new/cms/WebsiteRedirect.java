package com.orbitastra.backend.models.undone.a_new.cms;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "website_redirects")
@CompoundIndex(name = "tenant_site_source_path_uniq",
        def = "{'tenantId':1,'websiteSiteDocsId':1,'sourcePath':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteRedirect extends TenantScopedDocument {

    private String websiteSiteDocsId;
    private String sourcePath;
    private String targetPath;
    private Integer httpStatus;
    private Boolean active;
}
