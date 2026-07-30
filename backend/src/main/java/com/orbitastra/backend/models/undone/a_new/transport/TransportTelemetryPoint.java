package com.orbitastra.backend.models.undone.a_new.transport;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Schema for a MongoDB time-series collection. Create the collection explicitly
 * with {@code timeField=recordedAt} and {@code metaField=telemetryMeta}; do not
 * rely on automatic collection creation.
 */
@Document(collection = "transport_telemetry")
@CompoundIndex(name = "tenant_vehicle_recorded_idx",
        def = "{'tenantId':1,'vehicleDocsId':1,'recordedAt':-1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTelemetryPoint extends TenantScopedDocument {

    private String vehicleDocsId;
    private String transportTripDocsId;
    private String deviceId;
    private Double longitude;
    private Double latitude;
    private Double speedKph;
    private Double headingDegrees;
    private Double accuracyMeters;
    private Boolean ignitionOn;

    @Indexed
    private Instant recordedAt;

    @Indexed(expireAfter = "0s")
    private Instant expireAt;
}
