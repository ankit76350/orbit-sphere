package com.orbitastra.backend.models.undone.a_new.cms;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "website_sites")
@CompoundIndex(name = "tenant_site_key_uniq",
        def = "{'tenantId':1,'siteKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteSite extends TenantScopedDocument {

    private String siteKey;
    private String name;
    private String campusDocsId;
    private String tenantDomainDocsId;
    private String defaultLocale;
    private String themeKey;
    private String analyticsProviderKey;
    private String status;

    @Builder.Default
    private List<String> supportedLocales = new ArrayList<>();
}
