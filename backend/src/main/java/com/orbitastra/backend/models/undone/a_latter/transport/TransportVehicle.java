package com.orbitastra.backend.models.undone.a_latter.transport;



import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_latter.transport.embedded.VehicleHealth;
import com.orbitastra.backend.models.undone.a_latter.transport.enums.FuelType;
import com.orbitastra.backend.models.undone.a_latter.transport.enums.VehicleStatus;
import com.orbitastra.backend.models.undone.a_latter.transport.enums.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "transport_vehicles")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransportVehicle extends SchoolBase {

    /**
     * School vehicle number.
     * Example:
     * BUS-01
     */
    @Indexed(unique = true)
    private String vehicleNumber;

    /**
     * Government registration number.
     */
    @Indexed(unique = true)
    private String registrationNumber;

    /**
     * Bus / Van / Mini Bus.
     */
    private VehicleType vehicleType;

    /**
     * Seating capacity.
     */
    private Integer capacity;

    /**
     * Manufacturer.
     * Example:
     * Tata
     * Ashok Leyland
     */
    private String manufacturer;

    /**
     * Vehicle model.
     */
    private String model;

    /**
     * Manufacturing year.
     */
    private Integer manufacturingYear;

    /**
     * Fuel type.
     */
    private FuelType fuelType;

    /**
     * GPS tracker unique id.
     */
    @Indexed
    private String gpsDeviceId;

    /**
     * SIM number installed in GPS.
     */
    private String simNumber;

    /**
     * Insurance expiry.
     */
    @Indexed
    private LocalDate insuranceExpiry;

    /**
     * Fitness certificate expiry.
     */
    @Indexed
    private LocalDate fitnessExpiry;

    /**
     * Pollution certificate expiry.
     */
    @Indexed
    private LocalDate pollutionExpiry;

    /**
     * Next maintenance date.
     */
    @Indexed
    private LocalDate maintenanceDueDate;

    /**
     * Vehicle health.
     */
    private VehicleHealth vehicleHealth;

    /**
     * Current status.
     */
    @Indexed
    private VehicleStatus status;

    /**
     * Additional notes.
     */
    private String remarks;

}
