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

@Document(collection = "alumni_event_registrations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_event_profile_uniq",
                def = "{'tenantId':1,'alumniEventDocsId':1,'alumniProfileDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_event_status_idx",
                def = "{'tenantId':1,'alumniEventDocsId':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniEventRegistration extends TenantScopedDocument {

    private String alumniEventDocsId;
    private String alumniProfileDocsId;
    private String status;
    private Integer guestCount;
    private String formSubmissionDocsId;
    private String paymentTransactionDocsId;
    private Instant registeredAt;
    private Instant checkedInAt;
}
