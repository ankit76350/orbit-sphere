package com.orbitastra.backend.models.new_new.transport;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.transport.enums.TransportAllocationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One student's seat on the bus for one academic year.
 *
 * <p>This is the record that connects a child to a route, and it is also what
 * causes them to be charged. Nothing else in transport touches money.
 *
 * <p>A student may hold only one ACTIVE allocation in a year, which the unique
 * index enforces. Moving house means ending the old allocation and making a new
 * one, so the change has a date on it and the old arrangement is still readable.
 *
 * <p>{@code pickupStopCode} and {@code dropStopCode} name stops inside the route
 * rather than pointing at documents, because stops are embedded in TransportRoute
 * and have no ids of their own. Either may be null: plenty of families use the bus
 * one way only, and a null means the child does not travel that way.
 *
 * <p>{@code monthlyFareAmount} is copied from the stop's price list when the
 * allocation is made, and is never rewritten afterwards. This is the same rule
 * concessions follow when they copy a rate from the policy. Changing the price
 * list in November must not silently change what a family agreed to in April; a
 * new price means ending this allocation and starting another.
 *
 * <p>{@code feeHeadDocsId} is how the charge reaches a bill. The fee run reads
 * every ACTIVE allocation and adds a line for that head at
 * {@code monthlyFareAmount}. The head is normally one with
 * FeeCategory.TRANSPORT. Without this field the school would have to remember to
 * bill transport by hand for every family, every month.
 *
 * <p>A SUSPENDED allocation is neither billed nor put on a trip list, which is
 * what makes it useful for a child away for a term.
 *
 * <p>The service checks that both stop codes exist on the named route, that the
 * route has room left against the vehicle's capacity, that the fee head allows a
 * transport charge, and that the dates sit inside the academic year.
 */
@Document(collection = "transport_allocations")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_transport_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_year_route_allocation_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'routeDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_allocation_billing_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'startDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAllocation extends AcademicStudentSchoolBase {

    // Links to TransportRoute.id. Example: "67b21122dc3f7d0011223344"
    @NotBlank
    private String routeDocsId;

    // Stop the child gets on at in the morning, named by RouteStop.stopCode.
    // Null when they do not use the morning bus. Example: "ANDHERI_W_01"
    private String pickupStopCode;

    // Stop the child gets off at in the afternoon, named by RouteStop.stopCode.
    // Null when they do not use the afternoon bus. Example: "ANDHERI_W_01"
    private String dropStopCode;

    // What this family pays each month, copied from the stop when the allocation
    // was made. Never rewritten when the price list changes. Example: 2000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyFareAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Links to FeeHead.id the transport charge is billed under. The fee run reads
    // this to put transport on the bill. Example: "67ac1188dc3f7d0011aa22bb"
    @NotBlank
    private String feeHeadDocsId;

    // First day the child uses the bus. Example: 2026-04-01
    @NotNull
    private LocalDate startDate;

    // Last day they use it. Null while the arrangement carries on.
    // Example: 2027-03-31
    private LocalDate endDate;

    // Example: TransportAllocationStatus.ACTIVE
    @NotNull
    @Builder.Default
    private TransportAllocationStatus status = TransportAllocationStatus.ACTIVE;

    // Why it was suspended or ended. Example: "Family moved to Pune."
    private String statusReason;

    // Links to the staff identity that approved the allocation.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: "Grandfather collects the child on Fridays."
    private String remarks;
}
