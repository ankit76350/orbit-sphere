package com.orbitastra.backend.models.undone.a_working.transport;



import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.transport.enums.AllocationStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Transport allocation of a student.
 *
 * One student can have only one active transport allocation
 * during an academic year.
 */
@Document(collection = "transport_allocations")
@CompoundIndex(
        name = "student_year_idx",
        def = "{'studentDocsId':1,'academicYear':1}",
        unique = true
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransportAllocation extends SchoolBase {

    //   Student
    //     │
    //     │
    //     ▼
    // TransportAllocation
    //         │
    //         ├────────────► TransportRoute
    //         │
    //         ├────────────► RouteStop (Pickup)
    //         │
    //         └────────────► RouteStop (Drop)

    /**
     * Academic Year.
     *
     * Example:
     * 2026-2027
     */
    @Indexed
    private String academicYear;

    /**
     * Student reference.
     */
    @Indexed
    private String studentDocsId;

    /**
     * Assigned transport route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Pickup stop.
     */
    private String pickupStopDocsId;

    /**
     * Drop stop.
     */
    private String dropStopDocsId;

    /**
     * Monthly/Yearly transport fee.
     */
    private BigDecimal feeAmount;

    /**
     * Allocation start date.
     */
    private LocalDate startDate;

    /**
     * Allocation end date.
     *
     * Null = Currently Active
     */
    private LocalDate endDate;

    /**
     * Allocation status.
     */
    @Indexed
    private AllocationStatus status;

    /**
     * Staff/Admin who approved the allocation.
     */
    private String approvedByDocsId;

    /**
     * Additional remarks.
     */
    private String remarks;

}
