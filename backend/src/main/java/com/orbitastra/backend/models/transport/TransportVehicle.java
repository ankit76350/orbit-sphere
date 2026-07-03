package com.orbitastra.backend.models.transport;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.transport.enums.TransportVehicleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportVehicle {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String vehicleNo;

    private Integer capacity;

    private LocalDate insuranceExpiry;

    private LocalDate fitnessExpiry;

    private String fuelType;

    private TransportVehicleStatus status;

    private LocalDate maintenanceSchedule;
}
