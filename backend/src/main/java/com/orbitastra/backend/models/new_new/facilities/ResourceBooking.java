package com.orbitastra.backend.models.new_new.facilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.facilities.enums.BookingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Somebody asking to use a space for a stretch of time.
 *
 * <p>The assembly hall for a rehearsal, the computer lab for a Saturday workshop, the ground
 * for a match. This is what {@code FacilityResource.bookable} exists to gate: a classroom
 * belongs to the timetable and must never appear here, or somebody will book the room a lesson
 * is happening in.
 *
 * <p>**Only APPROVED bookings hold the space.** A REQUESTED one blocks nobody, which is
 * deliberate: if requests reserved the room, one person filling in a form for a "maybe" in
 * March would keep the hall empty until somebody remembered to cancel. That means two people
 * can request the same slot and the clash is caught at **approval**, which is the only moment
 * the school actually commits to anything.
 *
 * <p>That is the one rule a service must get right here, and the unique index cannot express
 * it — overlap is a range comparison, not an equality. So the check is code: no APPROVED
 * booking for the same resource may overlap the requested window.
 *
 * <p>{@code expectedAttendance} is checked against the resource's {@code capacity} rather than
 * being decoration. Two hundred people booked into a hall that holds a hundred and twenty is a
 * safety problem, and it is the sort of thing that only becomes visible on the day unless
 * something compares the two numbers in advance.
 *
 * <p>There is no recurrence here. "Every Tuesday for the term" is twelve bookings, and the
 * reason is that the fourth Tuesday is a holiday, the seventh clashes with an exam, and a
 * recurring booking that cannot have one instance moved is worse than twelve rows. Non-working
 * days come from AcademicYear.holidays; no weekday is assumed to be free.
 *
 * <p>{@code externalParty} covers the case a school actually has: an outside body hiring the
 * ground at the weekend. It is plain text with a contact, not a Vendor, because the cricket
 * club that hires the field on Sundays is nobody the school buys from.
 *
 * <p>The service checks that the resource is {@code bookable} and IN_USE, that no APPROVED
 * booking overlaps, that {@code startsAt} is before {@code endsAt}, that expected attendance
 * does not exceed capacity, that a rejection carries a reason, and that a booking is refused
 * on a space with an open CRITICAL inspection finding.
 */
@Document(collection = "resource_bookings")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_booking_no_uniq",
                def = "{'schoolId': 1, 'bookingNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_booking_resource_window_idx",
                def = "{'schoolId': 1, 'facilityResourceDocsId': 1, 'status': 1, 'startsAt': 1, 'endsAt': 1}"),
        @CompoundIndex(
                name = "school_booking_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'startsAt': 1}"),
        @CompoundIndex(
                name = "school_booking_requester_idx",
                def = "{'schoolId': 1, 'requestedByStaffDocsId': 1, 'startsAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBooking extends SchoolBase {

    // School-scoped number from NumberSequence type RESOURCE_BOOKING.
    // Example: "BKG/2026/000241"
    @NotBlank
    private String bookingNo;

    // Links to FacilityResource.id being asked for. Must be a bookable one.
    // Example: "67c31132dc3f7d0022003344"
    @NotBlank
    private String facilityResourceDocsId;

    // Links to Staff.id of whoever asked. Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String requestedByStaffDocsId;

    // Links to Department.id on whose behalf, when it is departmental.
    // Example: "67aa2211dc3f7d0011223344"
    private String departmentDocsId;

    // What it is for. Example: "Annual day rehearsal, Classes VIII to X"
    @NotBlank
    private String purpose;

    // When it starts. Example: 2026-11-14T09:30:00Z
    @NotNull
    private Instant startsAt;

    // When it ends. Example: 2026-11-14T12:00:00Z
    @NotNull
    private Instant endsAt;

    // How many people are expected, checked against the resource's capacity before this
    // is approved. Example: 180
    private Integer expectedAttendance;

    // Where it stands. Only APPROVED holds the space.
    // Example: BookingStatus.APPROVED
    @NotNull
    @Builder.Default
    private BookingStatus status = BookingStatus.REQUESTED;

    // Links to Staff.id of whoever approved or refused it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String decidedByStaffDocsId;

    // When they did. Example: 2026-11-02T06:00:00Z
    private Instant decidedAt;

    // Why it was refused. Required for REJECTED.
    // Example: "Hall already held for the board practical on that morning."
    private String rejectionReason;

    // Why it was called off. Required for CANCELLED.
    // Example: "Rehearsal moved to the ground; weather cleared."
    private String cancellationReason;

    // An outside body hiring the space, where that is what this is. Plain text with a
    // contact rather than a Vendor: the cricket club that hires the field on Sundays is
    // nobody the school buys from. Example: "Dadar Cricket Club — Mr Pawar, 9820011223"
    private String externalParty;

    // What the school charges for the hire, where it charges. Nothing here bills anybody;
    // a charge raised against an outside party is a FeeInvoice with sourceType MANUAL.
    // Example: 5000.00
    private Double hireChargeAmount;

    // Whether extra things were asked for, in words. A projector, chairs, a microphone.
    // Free text because the answer is a sentence, and a school that wants to allocate
    // tagged assets to a booking should link them from the asset instead.
    // Example: "Need the PA system and 40 extra chairs from the store."
    private String setupRequirements;

    // Links to DocumentRecord.id for a hire agreement or a written permission.
    // Example: ["67c31133dc3f7d0033004455"]
    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();

    // Anything worth knowing.
    // Example: "Overran by half an hour last year; warn the next booking."
    private String remarks;
}
