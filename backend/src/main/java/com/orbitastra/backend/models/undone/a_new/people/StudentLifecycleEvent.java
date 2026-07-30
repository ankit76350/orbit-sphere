package com.orbitastra.backend.models.undone.a_new.people;

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

@Document(collection = "student_lifecycle_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_lifecycle_sequence_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_lifecycle_type_time_idx",
                def = "{'tenantId':1,'eventType':1,'effectiveAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLifecycleEvent extends TenantScopedDocument {

    private String studentDocsId;
    private Long sequenceNo;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private Instant effectiveAt;
    private String reasonCode;
    private String initiatedByDocsId;
    private String approvedByDocsId;
    private String evidenceDocumentDocsId;
    private String relatedEnrollmentDocsId;
}
