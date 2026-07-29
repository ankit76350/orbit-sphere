package com.orbitastra.backend.models.undone.a_latter.transport;



import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_latter.transport.enums.AssignmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Assigns a Driver and Vehicle to a Route for a specific period.
 *
 * A Route never permanently owns a Driver or Vehicle.
 *
 * Example:
 *
 * Monday
 * Route R001
 * Driver A
 * Vehicle BUS-01
 *
 * Tuesday
 * Route R001
 * Driver B
 * Vehicle BUS-02
 */
@Document(collection = "route_assignments")
@CompoundIndex(
        name = "route_assignment_idx",
        def = "{'routeDocsId':1,'effectiveFrom':1}"
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RouteAssignment extends SchoolBase {

    //            Driver
    //              │
    //              │
    //              ▼
    //       RouteAssignment
    //        ▲          ▲
    //        │          │
    //        │          │
    //   TransportRoute  TransportVehicle

    /**
     * Transport Route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Assigned Vehicle.
     */
    @Indexed
    private String vehicleDocsId;

    /**
     * Assigned Driver.
     */
    @Indexed
    private String driverDocsId;

    /**
     * Assignment start date.
     */
    @Indexed
    private LocalDate effectiveFrom;

    /**
     * Assignment end date.
     *
     * Null = Still Active
     */
    private LocalDate effectiveTo;

    /**
     * Assignment status.
     */
    @Indexed
    private AssignmentStatus status;

    /**
     * Reason for assignment.
     *
     * Examples:
     * Regular Route
     * Driver Replacement
     * Vehicle Maintenance
     */
    private String assignmentReason;

    /**
     * Staff/Admin who created this assignment.
     */
    private String assignedByDocsId;

    /**
     * Additional remarks.
     */
    private String remarks;

}
