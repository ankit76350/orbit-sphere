package com.orbitastra.backend.models.transport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.transport.embedded.RouteStop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One path the bus takes, and every place it stops along the way.
 *
 * <p>The stops live inside the route rather than in their own collection. A route
 * has ten or twenty of them, they are always read together with the route, and
 * their order is part of what the route is. The same reasoning that keeps sections
 * inside SchoolClass and installments inside FeeStructure.
 *
 * <p>Because the stops are embedded they have no document ids, so allocations
 * point at a stop by {@code RouteStop.stopCode} instead. That code must not be
 * renamed once students are using it.
 *
 * <p>A route says where the bus goes, not who drives it. Drivers and vehicles
 * change week to week and are recorded in RouteAssignment, so a route can outlive
 * any particular bus.
 *
 * <p>{@code active} being false stops new students being allocated but leaves the
 * ones already on the route alone. Closing a route for real means moving those
 * students first, which the service checks before allowing it.
 *
 * <p>The service checks that stop codes are unique inside the route, that
 * {@code sequenceNo} runs from 1 with no gaps, that pickup times go forward along
 * the route and drop times go backward, and that a stop still used by an active
 * allocation is not removed.
 */
@Document(collection = "transport_routes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_route_code_uniq",
                def = "{'schoolId': 1, 'routeCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_route_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'routeName': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute extends SchoolBase {

    // Stable key allocations and assignments point at. Example: "RT-ANDHERI-01"
    @NotBlank
    private String routeCode;

    // Name staff and parents see. Example: "Andheri West Morning Route"
    @NotBlank
    private String routeName;

    // Where the route starts from. Example: "Lokhandwala Circle"
    private String startingPointName;

    // Where the route ends, usually the school. Example: "School Main Gate"
    private String endingPointName;

    // Every place the bus stops, in order. A route with no stops cannot carry
    // anybody, so there must be at least one.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<RouteStop> stops = new ArrayList<>();

    // Roughly how far the whole route is, used for planning. Example: 18.40
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedDistanceKm;

    // Roughly how long the whole route takes. Example: 55
    private Integer estimatedDurationMinutes;

    // Whether new students may still be allocated to this route. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Avoids the flyover during monsoon."
    private String remarks;
}
