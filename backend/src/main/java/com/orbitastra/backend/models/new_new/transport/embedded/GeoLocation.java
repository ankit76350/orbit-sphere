package com.orbitastra.backend.models.new_new.transport.embedded;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A point on the map.
 *
 * <p>It has no collection of its own. It is only ever part of something else, such
 * as where a stop is.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    // How far north or south. Example: 19.119800
    @NotNull
    @Min(-90)
    @Max(90)
    private Double latitude;

    // How far east or west. Example: 72.847500
    @NotNull
    @Min(-180)
    @Max(180)
    private Double longitude;
}
