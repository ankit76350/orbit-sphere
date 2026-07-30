package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "governance_memberships")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_body_member_term_uniq",
                def = "{'tenantId':1,'governingBodyDocsId':1,'personDocsId':1,'termFrom':1}", unique = true),
        @CompoundIndex(name = "tenant_body_active_term_idx",
                def = "{'tenantId':1,'governingBodyDocsId':1,'termTo':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceMembership extends TenantScopedDocument {

    private String governingBodyDocsId;
    private String personDocsId;
    private String externalMemberName;
    private String role;
    private LocalDate termFrom;
    private LocalDate termTo;
    private Boolean votingMember;
    private String appointmentDocumentDocsId;

    @Builder.Default
    private List<InterestDeclaration> declarations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestDeclaration {
        private LocalDate declaredOn;
        private String subject;
        private String mitigation;
        private String evidenceDocumentDocsId;
    }
}
