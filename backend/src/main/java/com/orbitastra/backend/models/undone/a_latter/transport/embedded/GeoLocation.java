package com.orbitastra.backend.models.undone.a_latter.transport.embedded;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a geographical location.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    /**
     * Latitude.
     *
     * Example:
     * 18.520430
     */
    private Double latitude;

    /**
     * Longitude.
     *
     * Example:
     * 73.856743
     */
    private Double longitude;

}
