package com.orbitastra.backend.models.undone.a_new.transport;

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

@Document(collection = "transport_trips")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_transport_trip_no_uniq",
                def = "{'tenantId':1,'tripNo':1}", unique = true),
        @CompoundIndex(name = "tenant_vehicle_date_run_uniq",
                def = "{'tenantId':1,'vehicleDocsId':1,'serviceDate':1,'runCode':1}", unique = true),
        @CompoundIndex(name = "tenant_route_date_status_idx",
                def = "{'tenantId':1,'routeDocsId':1,'serviceDate':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTrip extends AcademicScopedDocument {

    private String tripNo;
    private String runCode;
    private LocalDate serviceDate;
    private String routeDocsId;
    private String routeAssignmentDocsId;
    private String vehicleDocsId;
    private String driverDocsId;
    private String attendantDocsId;
    private String direction;
    private String status;
    private Instant plannedStartAt;
    private Instant actualStartAt;
    private Instant actualEndAt;
    private Integer plannedPassengerCount;
    private Integer boardedPassengerCount;
    private Integer droppedPassengerCount;
    private String emergencyCaseDocsId;
}
