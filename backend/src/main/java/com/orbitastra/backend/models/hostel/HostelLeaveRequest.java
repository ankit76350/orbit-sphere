package com.orbitastra.backend.models.new_new.hostel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.common.embedded.GuardianInformed;
import com.orbitastra.backend.models.new_new.hostel.enums.HostelLeaveStatus;

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
 * A boarder going home, or anywhere else, and coming back.
 *
 * <p>This is the hostel equivalent of the gate's StudentOutPass, and it works the same
 * way for the same reason: a child is being handed to somebody, and the school will be
 * asked exactly who took them and when they came back.
 *
 * <p>{@code collectedByGuardianDocsId} must be a guardian of this child with
 * {@code GuardianLink.pickupAuthorized} set. The check lives in one place — the
 * student's guardian list — and this package enforces it rather than keeping its own
 * list of who may collect whom.
 *
 * <p>{@code studentOutPassDocsId} links to the gate record for the same departure.
 * Leaving the hostel and leaving the campus are one journey seen from two places, and
 * joining them means the warden's record and the gate's record cannot disagree about
 * when the child actually left.
 *
 * <p>DEPARTED and OVERDUE are the states that matter at night. A child at DEPARTED is
 * not in the building and is not missing. A child at OVERDUE should have been back and
 * nobody has seen them, and that is a phone call home rather than a row in a list.
 *
 * <p>{@code emergencyContactDuringLeave} is written down separately because a child at
 * their grandmother's for a week is not reachable on the number the school usually
 * rings, and the one time that matters is the one time nobody thought to ask.
 *
 * <p>The service checks that the collector is an authorised guardian, that the approver
 * is a warden or senior member of staff, that a rejection carries a reason, and that a
 * child on APPROVED leave is marked ON_APPROVED_LEAVE at roll call rather than absent.
 */
@Document(collection = "hostel_leave_requests")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_hostel_leave_no_uniq",
                def = "{'schoolId': 1, 'requestNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_hostel_leave_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'departureDate': -1}"),
        @CompoundIndex(
                name = "school_hostel_leave_queue_idx",
                def = "{'schoolId': 1, 'status': 1, 'departureDate': -1}"),
        @CompoundIndex(
                name = "school_hostel_leave_outstanding_idx",
                def = "{'schoolId': 1, 'status': 1, 'expectedReturnAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelLeaveRequest extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type HOSTEL_LEAVE_REQUEST.
    // Example: "HL/2026/000731"
    @NotBlank
    private String requestNo;

    // Links to HostelAllocation.id, so the request is tied to the stay it belongs to.
    // Example: "67ba1126dc3f7d0055667788"
    @NotBlank
    private String hostelAllocationDocsId;

    // The day the child is leaving. Example: 2026-08-21
    @NotNull
    private LocalDate departureDate;

    // Why they are going. Example: "Cousin's wedding in Nashik."
    @NotBlank
    private String reason;

    // Where they will be. Example: "Grandmother's house, Nashik."
    private String destination;

    // A number that reaches the child while they are away, which is often not the
    // one the school normally rings. Example: "+919812345678"
    private String emergencyContactDuringLeave;

    // Links to Guardian.id for whoever is coming to fetch them. Must be a guardian
    // of this child with pickupAuthorized set.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String collectedByGuardianDocsId;

    // Example: HostelLeaveStatus.RETURNED
    @NotNull
    @Builder.Default
    private HostelLeaveStatus status = HostelLeaveStatus.DRAFT;

    // When they are expected to go. Example: 2026-08-21T10:00:00Z
    private Instant expectedDepartureAt;

    // When they are expected back. Example: 2026-08-24T13:00:00Z
    @NotNull
    private Instant expectedReturnAt;

    // When they actually left. Example: 2026-08-21T10:20:00Z
    private Instant actualDepartureAt;

    // When they actually came back. Example: 2026-08-24T14:05:00Z
    private Instant actualReturnAt;

    // Links to StudentOutPass.id for the same departure at the gate.
    // Example: "67b61126dc3f7d0055667788"
    private String studentOutPassDocsId;

    // Links to the staff identity that allowed it, normally the warden.
    // Example: "67aa15d9dc3f7d0044444444"
    private String approvedByDocsId;

    // Example: 2026-08-20T11:30:00Z
    private Instant approvedAt;

    // Why it was refused. Needed whenever the status is REJECTED.
    // Example: "Exams start on Monday; leave refused until they finish."
    private String rejectionReason;

    // Every guardian told, each with the time. Used when a child is late back.
    @Valid
    @Builder.Default
    private List<GuardianInformed> guardiansInformed = new ArrayList<>();

    // Example: "Father rang to say the train was delayed by four hours."
    private String remarks;
}
