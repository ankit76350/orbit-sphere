package com.orbitastra.backend.models.undone.a_new.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "report_definitions")
@CompoundIndex(name = "tenant_report_key_version_uniq",
        def = "{'tenantId':1,'reportKey':1,'reportVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDefinition extends TenantScopedDocument {

    private String reportKey;
    private Integer reportVersion;
    private String name;
    private String dataDomain;
    private String ownerDocsId;
    private String requiredPermission;
    private String status;
    private String outputFormat;

    @Builder.Default
    private List<String> columns = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> filterSchema = new HashMap<>();

    @Builder.Default
    private Map<String, Object> defaultFilters = new HashMap<>();

    @Builder.Default
    private List<String> maskedFields = new ArrayList<>();
}
