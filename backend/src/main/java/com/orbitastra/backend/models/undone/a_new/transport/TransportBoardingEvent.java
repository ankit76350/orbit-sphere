package com.orbitastra.backend.models.undone.a_new.transport;

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

@Document(collection = "transport_boarding_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_trip_student_event_uniq",
                def = "{'tenantId':1,'transportTripDocsId':1,'studentDocsId':1,'eventType':1}", unique = true),
        @CompoundIndex(name = "tenant_student_transport_event_time_idx",
                def = "{'tenantId':1,'studentDocsId':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportBoardingEvent extends AcademicScopedDocument {

    private String transportTripDocsId;
    private String studentDocsId;
    private String transportAllocationDocsId;
    private String eventType;
    private String routeStopDocsId;
    private Instant occurredAt;
    private String recordedByDocsId;
    private String captureMethod;
    private Double latitude;
    private Double longitude;
    private String exceptionCode;
    private String notificationDocsId;
}
