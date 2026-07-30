package com.orbitastra.backend.models.undone.a_new.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "security_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_security_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_security_owner_status_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'status':1,'openedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityCase extends CampusScopedDocument {

    private String caseNo;
    private String title;
    private String incidentType;
    private String severity;
    private String status;
    private String ownerDocsId;
    private Confidentiality confidentiality;
    private Instant openedAt;
    private Instant closedAt;
    private String resolution;
    private String policeReference;
    private String safeguardingCaseDocsId;
    private String privacyIncidentDocsId;
    private String legalHoldDocsId;

    @Builder.Default
    private List<String> securityEventDocsIds = new ArrayList<>();
}
