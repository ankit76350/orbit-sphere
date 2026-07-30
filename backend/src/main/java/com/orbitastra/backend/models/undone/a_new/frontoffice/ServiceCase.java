package com.orbitastra.backend.models.undone.a_new.frontoffice;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "front_office_service_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_service_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_service_case_owner_status_due_idx",
                def = "{'tenantId':1,'assignedToDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCase extends CampusScopedDocument {

    private String caseNo;
    private String caseType;
    private String categoryCode;
    private String priority;
    private String raisedByType;
    private String raisedByDocsId;
    private String encryptedExternalContact;
    private String subject;
    private String description;
    private Confidentiality confidentiality;
    private String status;
    private String assignedToDocsId;
    private Instant openedAt;
    private Instant dueAt;
    private Instant resolvedAt;
    private String resolution;
    private String satisfactionSurveyResponseDocsId;
}
