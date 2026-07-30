package com.orbitastra.backend.models.undone.a_new.gate;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "visit_appointments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_visit_pass_code_uniq",
                def = "{'tenantId':1,'passCodeHash':1}", unique = true,
                partialFilter = "{'passCodeHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_visit_campus_status_start_idx",
                def = "{'tenantId':1,'campusDocsId':1,'status':1,'expectedStartAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VisitAppointment extends CampusScopedDocument {

    private String visitorProfileDocsId;
    private String visitType;
    private String purpose;
    private String hostPersonDocsId;
    private String studentDocsId;
    private String approvedByDocsId;
    private String passCodeHash;
    private String status;
    private Instant expectedStartAt;
    private Instant expectedEndAt;
    private Instant checkedInAt;
    private Instant checkedOutAt;
    private String badgeNo;
    private String vehicleRegistration;
    private String accessScope;
}
