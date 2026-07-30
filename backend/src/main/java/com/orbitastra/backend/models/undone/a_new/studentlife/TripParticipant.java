package com.orbitastra.backend.models.undone.a_new.studentlife;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "trip_participants")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_trip_student_uniq",
                def = "{'tenantId':1,'tripPlanDocsId':1,'studentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_trip_status_idx",
                def = "{'tenantId':1,'tripPlanDocsId':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TripParticipant extends AcademicScopedDocument {

    private String tripPlanDocsId;
    private String studentDocsId;
    private String guardianDocsId;
    private String status;
    private String consentRecordDocsId;
    private String paymentDocsId;
    private String healthClearanceDocsId;
    private Instant checkedInAt;
    private Instant checkedOutAt;
    private String specialInstructions;
}
