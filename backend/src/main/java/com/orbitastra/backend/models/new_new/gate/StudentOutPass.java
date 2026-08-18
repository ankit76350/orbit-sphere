package com.orbitastra.backend.models.new_new.gate;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.gate.enums.OutPassStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Permission for one child to leave the school during school hours.
 *
 * <p>This is the most serious record in the package. Everything else here is about
 * knowing who is in the building; this one is about handing a child to somebody.
 * Getting it wrong means a child leaves with the wrong adult.
 *
 * <p>{@code collectedByGuardianDocsId} names who is coming to fetch them, and the
 * service must check that person is a guardian of this child with
 * {@code GuardianLink.pickupAuthorized} set to true. That flag already exists on
 * the student's guardian list, which is why this model does not keep its own list
 * of who may collect whom. There is one place that answers "may this adult take
 * this child", and it is not here.
 *
 * <p>A child at EXITED is out of the school during school hours. If they were
 * expected back and the status has not moved to RETURNED, somebody has to find out
 * why. That is the whole reason the two states are separate rather than one "gone"
 * flag.
 *
 * <p>{@code emergency} exists because a real emergency will not wait for an
 * approval queue. It lets a pass be created and used at once, and it marks the
 * record so the school can look back afterwards at every time the normal check was
 * skipped. Skipping the queue must leave a trace, or it stops being an exception.
 *
 * <p>Leaving early is not the same as being absent. This model does not touch
 * attendance. A child who leaves at eleven was present in the morning, and how
 * that is recorded belongs to the attendance models, not to a gate record.
 *
 * <p>The service checks that the collector is an authorised guardian of this
 * child, that the approver is a member of staff and not the person who asked, that
 * a rejection carries a reason, that an emergency pass still names a real
 * collector, and that a child is not let out on a pass belonging to a different
 * day.
 */
@Document(collection = "student_out_passes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_out_pass_no_uniq",
                def = "{'schoolId': 1, 'outPassNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_out_pass_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'passDate': -1}"),
        @CompoundIndex(
                name = "school_out_pass_queue_idx",
                def = "{'schoolId': 1, 'status': 1, 'passDate': -1}"),
        @CompoundIndex(
                name = "school_out_pass_outstanding_idx",
                def = "{'schoolId': 1, 'passDate': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOutPass extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence. Example: "OP/2026/000914"
    @NotBlank
    private String outPassNo;

    // The day the child is leaving. Example: 2026-08-19
    @NotNull
    private LocalDate passDate;

    // Why they are going. Example: "Dentist appointment at 11:30."
    @NotBlank
    private String reason;

    // Links to Guardian.id for whoever is coming to fetch the child. The service
    // checks this guardian has pickupAuthorized set for this student.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String collectedByGuardianDocsId;

    // Links to Guardian.id for whoever asked. Often the same person who collects,
    // but not always. Example: "67aa15d9dc3f7d0066666666"
    private String requestedByGuardianDocsId;

    // Example: OutPassStatus.RETURNED
    @NotNull
    @Builder.Default
    private OutPassStatus status = OutPassStatus.DRAFT;

    // True when the normal approval queue was skipped because it could not wait.
    // Marked so the school can look back at every time that happened.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean emergency = false;

    // When the child is expected to leave. Example: 2026-08-19T05:30:00Z
    private Instant expectedExitAt;

    // When they are expected back. Null when they are not coming back today.
    // Example: 2026-08-19T08:00:00Z
    private Instant expectedReturnAt;

    // When they actually left. Example: 2026-08-19T05:36:00Z
    private Instant actualExitAt;

    // When they actually came back. Example: 2026-08-19T08:12:00Z
    private Instant actualReturnAt;

    // Links to Gate.id the child left through.
    // Example: "67b61124dc3f7d0033445566"
    private String exitGateDocsId;

    // Links to the staff identity that allowed it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: 2026-08-19T04:50:00Z
    private Instant approvedAt;

    // Why it was refused. Needed whenever the status is REJECTED.
    // Example: "No authorised guardian available to collect."
    private String rejectionReason;

    // Links to the staff identity at the gate that let the child out, after
    // checking the collector's face against the record.
    // Example: "67aa15d9dc3f7d0066666666"
    private String releasedByDocsId;

    // Anything worth knowing.
    // Example: "Mother showed her ID at the gate; matched the record."
    private String remarks;
}
