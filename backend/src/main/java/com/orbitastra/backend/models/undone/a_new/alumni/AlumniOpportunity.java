package com.orbitastra.backend.models.undone.a_new.alumni;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "alumni_opportunities")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_opportunity_code_uniq",
                def = "{'tenantId':1,'opportunityCode':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_opportunity_status_expiry_idx",
                def = "{'tenantId':1,'status':1,'expiresAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniOpportunity extends TenantScopedDocument {

    private String opportunityCode;
    private String postedByAlumniProfileDocsId;
    private String opportunityType;
    private String title;
    private String organizationName;
    private String location;
    private String description;
    private String applicationUrl;
    private String eligibility;
    private String status;
    private Instant publishedAt;
    private Instant expiresAt;
}
