package com.orbitastra.backend.models.undone.a_new.identity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ScopeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "role_definitions")
@CompoundIndex(name = "tenant_role_key_uniq",
        def = "{'tenantId':1,'roleKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDefinition extends TenantScopedDocument {

    private String roleKey;
    private String displayName;
    private String description;
    private Boolean systemManaged;
    private ScopeType maximumScope;

    @Builder.Default
    private List<PermissionGrant> permissions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionGrant {
        private String resource;
        private String action;
        private String fieldSetKey;
        private String conditionExpression;
        private Boolean maker;
        private Boolean checker;
    }
}
