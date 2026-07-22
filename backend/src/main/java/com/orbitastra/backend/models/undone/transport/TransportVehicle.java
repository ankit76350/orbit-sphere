package com.orbitastra.backend.models.undone.transport;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.undone.transport.enums.TransportVehicleStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_vehicles")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportVehicle extends BaseDocument {

    private String vehicleNo;

    private Integer capacity;

    private LocalDate insuranceExpiry;

    private LocalDate fitnessExpiry;

    private String fuelType;

    private TransportVehicleStatus status;

    private LocalDate maintenanceSchedule;
}
