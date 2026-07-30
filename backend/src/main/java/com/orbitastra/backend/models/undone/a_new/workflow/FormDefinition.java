package com.orbitastra.backend.models.undone.a_new.workflow;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "form_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_form_key_version_uniq",
                def = "{'tenantId':1,'formKey':1,'formVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_form_state_effective_idx",
                def = "{'tenantId':1,'formKey':1,'state':1,'effectiveFrom':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinition extends TenantScopedDocument {

    private String formKey;
    private Integer formVersion;
    private String name;
    private String entityType;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;

    /** Validated JSON Schema; arbitrary client keys are rejected at publish. */
    @Builder.Default
    private Map<String, Object> schema = new HashMap<>();

    /** Declarative UI hints, never executable JavaScript. */
    @Builder.Default
    private Map<String, Object> uiSchema = new HashMap<>();
}
