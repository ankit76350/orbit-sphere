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

@Document(collection = "access_movements")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_access_device_event_uniq",
                def = "{'tenantId':1,'securityDeviceDocsId':1,'providerEventId':1}", unique = true),
        @CompoundIndex(name = "tenant_subject_occurred_idx",
                def = "{'tenantId':1,'subjectType':1,'subjectDocsId':1,'occurredAt':-1}"),
        @CompoundIndex(name = "tenant_campus_gate_occurred_idx",
                def = "{'tenantId':1,'campusDocsId':1,'gateResourceDocsId':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccessMovement extends CampusScopedDocument {

    private String providerEventId;
    private String movementType;
    private String subjectType;
    private String subjectDocsId;
    private String visitAppointmentDocsId;
    private String studentOutPassDocsId;
    private String gateResourceDocsId;
    private String securityDeviceDocsId;
    private String recordedByDocsId;
    private String verificationMethod;
    private String result;
    private String exceptionCode;
    private Instant occurredAt;
}
