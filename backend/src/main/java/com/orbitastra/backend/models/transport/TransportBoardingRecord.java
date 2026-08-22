package com.orbitastra.backend.models.new_new.transport;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.common.enums.IdentificationMethod;
import com.orbitastra.backend.models.new_new.transport.embedded.GeoLocation;
import com.orbitastra.backend.models.new_new.transport.enums.BoardingStatus;
import com.orbitastra.backend.models.new_new.transport.enums.TripDirection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What happened to one child on one trip.
 *
 * <p>One row per child per trip, not one per event. Getting on and getting off are
 * two moments in the same story, so they are two timestamps on one row rather than
 * two rows that somebody then has to pair up. A child who got on but never got off
 * is then a single row that is obviously wrong, instead of a missing row nobody
 * notices.
 *
 * <p>Rows are made in advance, when the trip list is built, with status EXPECTED.
 * That matters more than it sounds. If rows were only made when a child was
 * scanned, a child who never turned up would leave no trace at all, and "who is
 * missing" is the question this model exists to answer.
 *
 * <p>MISSED and NOT_TRAVELLING are deliberately different states. NOT_TRAVELLING
 * is the family having told the school in advance. MISSED is nobody knowing where
 * the child is, and that is the one that has to reach a parent quickly.
 *
 * <p>{@code captureMethod} is kept because the answers are not equally
 * trustworthy. A card tap happened at a moment the system saw for itself. A manual
 * mark is somebody remembering, possibly after the fact. A parent disputing a
 * record deserves to know which of the two it was.
 *
 * <p>Sending the parent a message when a child boards is not this model's job.
 * That belongs to the notification system, which is not built yet. Nothing here
 * records whether a message went out.
 *
 * <p>The service checks that the child had an allocation covering that day, that
 * the stop is on the trip's route, that a boarding time is not after an alighting
 * time, and that the counts on TransportTrip stay in step with these rows.
 */
@Document(collection = "transport_boarding_records")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_trip_student_boarding_uniq",
                def = "{'schoolId': 1, 'transportTripDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_boarding_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'serviceDate': -1}"),
        @CompoundIndex(
                name = "school_trip_boarding_status_idx",
                def = "{'schoolId': 1, 'transportTripDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_boarding_exception_idx",
                def = "{'schoolId': 1, 'serviceDate': -1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportBoardingRecord extends AcademicStudentSchoolBase {

    // Links to TransportTrip.id. Example: "67b31122dc3f7d0011223344"
    @NotBlank
    private String transportTripDocsId;

    // Links to TransportAllocation.id this row came from.
    // Example: "67b31123dc3f7d0022334455"
    @NotBlank
    private String transportAllocationDocsId;

    // Copied from the trip so a child's travel history reads without loading
    // every trip. Example: 2026-08-18
    @NotNull
    private LocalDate serviceDate;

    // Copied from the trip for the same reason. Example: TripDirection.PICKUP
    @NotNull
    private TripDirection direction;

    // Stop this child was due at, named by RouteStop.stopCode.
    // Example: "ANDHERI_W_01"
    @NotBlank
    private String stopCode;

    // Example: BoardingStatus.COMPLETED
    @NotNull
    @Builder.Default
    private BoardingStatus status = BoardingStatus.EXPECTED;

    // When the child got on. Null until they do. Example: 2026-08-18T01:45:00Z
    private Instant boardedAt;

    // When the child got off. Null until they do. Example: 2026-08-18T02:10:00Z
    private Instant alightedAt;

    // How the boarding was recorded. Example: IdentificationMethod.RFID_TAP
    private IdentificationMethod captureMethod;

    // Where the bus was when the child got on, when the device reported it.
    // Example: 19.1198, 72.8475
    @Valid
    private GeoLocation boardedLocation;

    // Links to the staff identity that marked it, when it was done by hand.
    // Example: "67aa15d9dc3f7d0066666666"
    private String recordedByDocsId;

    // Why the child did not travel, or anything else worth knowing.
    // Example: "Mother called in the morning to say he is unwell."
    private String remarks;
}
