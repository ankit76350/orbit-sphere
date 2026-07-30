package com.orbitastra.backend.models.undone.a_new.institution;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "custom_field_definitions")
@CompoundIndex(name = "tenant_entity_field_key_uniq",
        def = "{'tenantId':1,'entityType':1,'fieldKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinition extends TenantScopedDocument {

    public enum FieldType {
        TEXT,
        LONG_TEXT,
        NUMBER,
        MONEY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        SINGLE_SELECT,
        MULTI_SELECT,
        DOCUMENT_REFERENCE,
        ENTITY_REFERENCE
    }

    private String entityType;
    private String fieldKey;
    private String label;
    private FieldType fieldType;
    private Boolean required;
    private Boolean encrypted;
    private Boolean searchable;
    private Confidentiality confidentiality;
    private String validationExpression;

    @Builder.Default
    private List<String> allowedValues = new ArrayList<>();
}
