package com.orbitastra.backend.models.transport.embedded;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One place a bus stops on a route.
 *
 * <p>It has no collection of its own. A route has ten or twenty stops, they are
 * always read together with the route, and their order is part of what a route is.
 * The same reasoning that keeps sections inside SchoolClass and installments
 * inside FeeStructure.
 *
 * <p>{@code stopCode} is what allocations point at, because an embedded stop has
 * no document id of its own. It has to be unique inside the route and must not be
 * renamed once students are allocated to it. This is the same idea as
 * {@code sectionNo} inside SchoolClass.
 *
 * <p>{@code monthlyFareAmount} is what makes the fare depend on the stop. A family
 * living twenty minutes further out pays more than one near the school, and this
 * is where that difference lives. Setting the same amount on every stop gives a
 * flat fare instead, so both ways of charging work without a separate setting.
 *
 * <p>The fare here is the current price list. It is not what any one family pays.
 * That amount is copied onto TransportAllocation when the family is allocated, so
 * changing the price list in November does not silently change what somebody
 * agreed to in April.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    // Stable key allocations point at. Unique inside this route, and must not be
    // renamed once students are using it. Example: "ANDHERI_W_01"
    @NotBlank
    private String stopCode;

    // Name parents see. May be reworded at any time. Example: "Andheri West Metro"
    @NotBlank
    private String stopName;

    // Order along the route, starting at 1. Example: 3
    @NotNull
    private Integer sequenceNo;

    // Where the stop is, used to show it on a map. Example: 19.1198, 72.8475
    @Valid
    private GeoLocation location;

    // How close the bus has to get before it counts as being at this stop.
    // Example: 100
    private Integer radiusMeters;

    // When the bus is expected here on the way to school. Example: 07:15
    private LocalTime pickupTime;

    // When the bus is expected here on the way home. Example: 15:40
    private LocalTime dropTime;

    // Current price for a student joining at this stop, for one month. What a
    // family actually pays is copied onto their allocation and does not change
    // when this does. Example: 2000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyFareAmount;

    // Whether new students may still be allocated to this stop. Turning it off
    // does not move the students already using it. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
