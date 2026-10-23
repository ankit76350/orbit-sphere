package com.orbitastra.backend.models.conduct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.common.embedded.GuardianInformed;
import com.orbitastra.backend.models.conduct.enums.ConductCaseStatus;
import com.orbitastra.backend.models.conduct.enums.ConductSeverity;

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
 * What the school is doing about one child, arising from one incident.
 *
 * <p>One event can produce several of these, one per child who was RESPONSIBLE. It
 * never produces one for a child who was AFFECTED: being hurt is not something a
 * school opens a case against you for.
 *
 * <p>The split from ConductEvent is what lets the same fight be serious for the boy
 * who threw the punch and minor for the one who joined in at the end. One incident,
 * two different answers, and both defensible because they are separate records.
 *
 * <p>{@code guardiansInformed} is the field a school gets asked about. Telling a
 * family their child is in trouble is not optional above MINOR severity, and "we rang
 * them" is not a record. It uses the same shape as a clinic visit, from common,
 * because proving you told a family is the same problem wherever it comes up.
 *
 * <p>{@code escalatedToSafeguarding} is a deliberate stopping point rather than a
 * link. Some cases are not discipline at all: a child who lashes out may be being hurt
 * at home. The safeguarding module is not built, so this flag and its note record that
 * somebody recognised it and passed it on, and the case stops being treated as a
 * detention to arrange. When safeguarding exists, this becomes a link to it.
 *
 * <p>A case is closed with an outcome, not deleted. A case that turned out not to have
 * happened is closed as WITHDRAWN with the reason, because a child accused of something
 * they did not do deserves that to be on the record rather than absent from it.
 *
 * <p>The service checks that the student is a RESPONSIBLE or PRESENT participant of the
 * named event, that a case above MINOR has a guardian informed before it closes, that a
 * SEVERE assessed case is assigned to a senior member of staff, and that closing carries an
 * outcome.
 */
@Document(collection = "student_conduct_cases")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_conduct_case_no_uniq",
                def = "{'schoolId': 1, 'caseNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_event_student_case_uniq",
                def = "{'schoolId': 1, 'conductEventDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_case_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'openedAt': -1}"),
        @CompoundIndex(
                name = "school_conduct_case_queue_idx",
                def = "{'schoolId': 1, 'status': 1, 'assessedSeverity': 1, 'openedAt': -1}"),
        @CompoundIndex(
                name = "school_conduct_safeguarding_idx",
                def = "{'schoolId': 1, 'escalatedToSafeguarding': 1, 'openedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentConductCase extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type CONDUCT_CASE.
    // Example: "CC/2026/000318"
    @NotBlank
    private String caseNo;

    // Links to ConductEvent.id this case came from.
    // Example: "67b81122dc3f7d0011223344"
    @NotBlank
    private String conductEventDocsId;

    // How serious it turned out to be for this child, once somebody had looked into it.
    // Named to pair with the event's reportedSeverity: that one is a first impression, this
    // one is the finding. It can differ from the event's and from other children in the
    // same event.
    // Example: ConductSeverity.SERIOUS
    @NotNull
    private ConductSeverity assessedSeverity;

    // Example: ConductCaseStatus.CLOSED
    @NotNull
    @Builder.Default
    private ConductCaseStatus status = ConductCaseStatus.OPEN;

    // Links to Staff.id for whoever is dealing with it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String assignedToStaffDocsId;

    // When the case was opened. Example: 2026-08-19T07:20:00Z
    @NotNull
    private Instant openedAt;

    // When it should be dealt with by. Example: 2026-08-21T12:00:00Z
    private Instant dueAt;

    // When it was closed. Null while it is still open.
    // Example: 2026-08-22T09:00:00Z
    private Instant closedAt;

    // Links to Staff.id for whoever closed it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String closedByStaffDocsId;

    // What was concluded, in plain words. Required when the status is CLOSED.
    // Example: "Both boys apologised. Detention served. No repeat since."
    private String outcome;

    // What the child said about it, recorded because they are entitled to be heard.
    // Example: "Says he was pushed first and did not mean to hurt anybody."
    private String studentStatement;

    // Every guardian who was told, each with the time. Not optional above MINOR.
    @Valid
    @Builder.Default
    private List<GuardianInformed> guardiansInformed = new ArrayList<>();

    // True when somebody recognised this is more than discipline and passed it on.
    // The case then stops being treated as a detention to arrange. Becomes a link
    // when the safeguarding module is built. Example: false
    @NotNull
    @Builder.Default
    private Boolean escalatedToSafeguarding = false;

    // Why it was passed on, and to whom, while there is no safeguarding module.
    // Example: "Third outburst this term; passed to the head of pastoral care."
    private String escalationNote;

    // Anything worth knowing.
    // Example: "Family asked for the meeting to be after work hours."
    private String remarks;
}
