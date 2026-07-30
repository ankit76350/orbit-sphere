package com.orbitastra.backend.models.undone.a_new.privacy;

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

@Document(collection = "retention_rules")
@CompoundIndex(name = "tenant_retention_rule_key_version_uniq",
        def = "{'tenantId':1,'ruleKey':1,'ruleVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionRule extends TenantScopedDocument {

    private String ruleKey;
    private Integer ruleVersion;
    private String name;
    private String triggerEvent;
    private Integer retainForDays;
    private String terminalAction;
    private String legalBasis;
    private Boolean active;

    @Builder.Default
    private List<String> collectionNames = new ArrayList<>();

    @Builder.Default
    private List<String> jurisdictionCodes = new ArrayList<>();
}
