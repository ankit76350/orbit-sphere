package com.orbitastra.backend.models.new_new.transport;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.transport.enums.TransportDriverStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The driving side of one member of staff.
 *
 * <p>A driver is a member of staff first. Their name, phone number, address and
 * emergency contact live on the Staff record and are not copied here, so there is
 * only one place to correct them. This model holds only the things that are true
 * because they drive: the licence, the badge, and whether they may drive today.
 *
 * <p>One Staff record has at most one of these, which the unique index enforces.
 *
 * <p>{@code licenceExpiryDate} is the one that matters. A driver whose licence
 * has run out must not take a bus out, and the service checks the date when a trip
 * starts rather than trusting somebody to have changed the status in time. The
 * date passing is the event, and nobody is watching on the day it happens.
 *
 * <p>An attendant, the person who rides along and helps children on and off, is
 * also recorded as a member of staff. They do not need a row here unless they also
 * drive, because everything in this model is about driving.
 */
@Document(collection = "transport_drivers")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_driver_staff_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_driver_licence_uniq",
                def = "{'schoolId': 1, 'licenceNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_driver_status_idx",
                def = "{'schoolId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportDriver extends SchoolBase {

    // Links to Staff.id. The person's name and phone number stay on that record.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String staffDocsId;

    // Driving licence number. Example: "MH0220110012345"
    @NotBlank
    private String licenceNumber;

    // Last day the licence is valid. Example: 2028-05-19
    @NotNull
    private LocalDate licenceExpiryDate;

    // Badge number issued by the transport authority for carrying passengers.
    // Example: "BDG-2024-8891"
    private String badgeNumber;

    // Last day the badge is valid. Example: 2027-04-30
    private LocalDate badgeExpiryDate;

    // Day they started driving for the school. Example: 2024-06-01
    private LocalDate drivingSinceDate;

    // Example: TransportDriverStatus.ACTIVE
    @NotNull
    @Builder.Default
    private TransportDriverStatus status = TransportDriverStatus.ACTIVE;

    // Why they are not driving, when the status is not ACTIVE.
    // Example: "On leave until 25 August."
    private String statusReason;

    // Example: "Has driven the Andheri route since 2024."
    private String remarks;
}
