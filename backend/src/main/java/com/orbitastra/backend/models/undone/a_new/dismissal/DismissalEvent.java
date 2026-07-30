package com.orbitastra.backend.models.undone.a_new.dismissal;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "dismissal_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_date_event_seq_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'eventDate':1,'eventSequence':1}", unique = true),
        @CompoundIndex(name = "tenant_campus_date_status_idx",
                def = "{'tenantId':1,'campusDocsId':1,'eventDate':1,'status':1,'occurredAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DismissalEvent extends AcademicScopedDocument {

    private String studentDocsId;
    private LocalDate eventDate;
    private Integer eventSequence;
    private String eventType;
    private String status;
    private String dismissalMode;
    private String pickupAuthorizationDocsId;
    private String verifiedByDocsId;
    private String verificationMethod;
    private Instant occurredAt;
    private String gateDocsId;
    private String exceptionReason;
    private String notificationDocsId;
}
