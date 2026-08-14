package com.orbitastra.backend.models.new_new.transport;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.transport.enums.FuelType;
import com.orbitastra.backend.models.new_new.transport.enums.VehicleStatus;
import com.orbitastra.backend.models.new_new.transport.enums.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One bus, van or car the school runs.
 *
 * <p>{@code registrationNumber} is the number plate and is what everybody outside
 * the school knows the vehicle by, so it is unique inside a school.
 * {@code fleetCode} is the school's own short name for it, such as BUS-01, and is
 * what staff say to each other.
 *
 * <p>The four expiry dates are the ones that stop a bus legally. A vehicle whose
 * insurance, fitness, pollution or permit has run out must not carry children,
 * whatever its status says. The service checks this before a trip starts rather
 * than trusting somebody to have changed the status in time, because the date
 * passing is the event and nobody is watching on the day it happens.
 *
 * <p>{@code capacity} is how many students may be allocated to this vehicle, and
 * the service uses it to refuse an allocation that would overfill a route.
 *
 * <p>{@code gpsDeviceId} is only the name of the tracker fitted to this vehicle.
 * Live position is not stored in this package. See the README for why.
 */
@Document(collection = "transport_vehicles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_vehicle_registration_uniq",
                def = "{'schoolId': 1, 'registrationNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vehicle_fleet_code_uniq",
                def = "{'schoolId': 1, 'fleetCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vehicle_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'fleetCode': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportVehicle extends SchoolBase {

    // The school's own short name for the vehicle. Example: "BUS-01"
    @NotBlank
    private String fleetCode;

    // Number plate. Example: "MH 02 AB 1234"
    @NotBlank
    private String registrationNumber;

    // Example: VehicleType.BUS
    @NotNull
    private VehicleType vehicleType;

    // How many students may ride at once. Used to stop a route being overfilled.
    // Example: 45
    @NotNull
    @Positive
    private Integer capacity;

    // Example: "Tata Motors"
    private String manufacturer;

    // Example: "Starbus Skool"
    private String model;

    // Year it was made. Example: 2022
    private Integer manufacturingYear;

    // Example: FuelType.DIESEL
    private FuelType fuelType;

    // Name of the tracker fitted to this vehicle. Live position is not kept in
    // this package. Example: "TRK-99183"
    private String gpsDeviceId;

    // Last day the insurance covers the vehicle. Example: 2027-03-31
    private LocalDate insuranceExpiryDate;

    // Last day the fitness certificate is valid. Example: 2027-01-15
    private LocalDate fitnessExpiryDate;

    // Last day the pollution certificate is valid. Example: 2026-11-30
    private LocalDate pollutionExpiryDate;

    // Last day the road permit is valid. Example: 2027-06-30
    private LocalDate permitExpiryDate;

    // Example: VehicleStatus.ACTIVE
    @NotNull
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;

    // Why the vehicle is not running, when the status is not ACTIVE.
    // Example: "Gearbox being repaired, back on 20 August."
    private String statusReason;

    // Example: "Fitted with cameras and a speed governor."
    private String remarks;
}
