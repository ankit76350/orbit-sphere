package com.orbitastra.backend.models.undone.a_working.transport.embedded;


import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vehicle operational information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHealth {

    /**
     * Total distance travelled.
     */
    private Double odometerKm;

    /**
     * Fuel percentage.
     */
    private Double fuelLevel;

    /**
     * Engine temperature.
     */
    private Double engineTemperature;

    /**
     * Battery voltage.
     */
    private Double batteryVoltage;

    /**
     * Last service date.
     */
    private LocalDate lastServiceDate;

}
