package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "guardian_student_relationships")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_guardian_relation_term_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'guardianPersonDocsId':1,'effectiveFrom':1}", unique = true),
        @CompoundIndex(name = "tenant_guardian_active_relationship_idx",
                def = "{'tenantId':1,'guardianPersonDocsId':1,'active':1}"),
        @CompoundIndex(name = "tenant_student_primary_guardian_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'primary':1,'active':1}",
                unique = true, partialFilter = "{'primary':true,'active':true}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianStudentRelationship extends TenantScopedDocument {

    private String studentDocsId;
    private String guardianPersonDocsId;
    private String relationshipCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Boolean active;
    private Boolean primary;
    private Boolean emergencyContact;
    private Boolean portalAccess;
    private Boolean academicDecisionAuthority;
    private Boolean medicalDecisionAuthority;
    private Boolean financialResponsibility;
    private Boolean pickupAuthority;
    private String custodyRestrictionCode;
    private String custodyDocumentDocsId;
    private Confidentiality confidentiality;
}
