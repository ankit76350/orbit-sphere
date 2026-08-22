package com.orbitastra.backend.models.new_new.conduct;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.conduct.embedded.EventParticipant;
import com.orbitastra.backend.models.new_new.conduct.enums.ConductEventType;
import com.orbitastra.backend.models.new_new.conduct.enums.ConductSeverity;

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
 * One thing that happened, once, involving one or more children.
 *
 * <p>This is the incident. What the school then does about each child is a separate
 * StudentConductCase, and keeping them apart is the point of the package.
 *
 * <p>The old {@code academics/DisciplineLog} held one student per row and no role. A
 * fight between three children became three unrelated rows, with no way to see it was
 * one event, who started it, or who was hurt. Somebody reading it a year later could
 * not tell a victim from an aggressor.
 *
 * <p>{@code participants} fixes that. Each child carries a ParticipantRole, so
 * RESPONSIBLE and AFFECTED are visibly different people in the same incident. A child
 * marked AFFECTED never has a case opened against them for this event.
 *
 * <p>The event is not academic-year scoped through the base class because it is not
 * about one student. It carries {@code academicYear} directly, the same way
 * AttendanceSession and TransportTrip do.
 *
 * <p>{@code severity} on the event is the reporter's first impression. Each case
 * carries its own severity afterwards, because one incident can be serious for the
 * child who did it and minor for the one who watched.
 *
 * <p>Nothing here is a decision. No warning, no detention, no outcome. An event only
 * says what happened, and it stays true even if every case arising from it is later
 * withdrawn.
 *
 * <p>The service checks that at least one participant is present, that a child is not
 * listed twice, that {@code occurredAt} is not in the future, and that recording an
 * event needs the CONDUCT module.
 */
@Document(collection = "conduct_events")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_conduct_event_no_uniq",
                def = "{'schoolId': 1, 'eventNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_conduct_event_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'eventDate': -1}"),
        @CompoundIndex(
                name = "school_conduct_event_student_idx",
                def = "{'schoolId': 1, 'participants.studentDocsId': 1, 'eventDate': -1}"),
        @CompoundIndex(
                name = "school_conduct_event_type_idx",
                def = "{'schoolId': 1, 'eventType': 1, 'eventDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConductEvent extends SchoolBase {

    // School-scoped number from NumberSequence type CONDUCT_EVENT.
    // Example: "CE/2026/000214"
    @NotBlank
    private String eventNo;

    // Links to AcademicYear.name. Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // What sort of thing happened. Example: ConductEventType.PHYSICAL_ALTERCATION
    @NotNull
    private ConductEventType eventType;

    // How serious it looked to whoever reported it. Each case carries its own
    // severity afterwards. Example: ConductSeverity.SERIOUS
    @NotNull
    private ConductSeverity reportedSeverity;

    // The day it happened. Example: 2026-08-19
    @NotNull
    private LocalDate eventDate;

    // The moment it happened, as closely as anybody knows.
    // Example: 2026-08-19T06:45:00Z
    @NotNull
    private Instant occurredAt;

    // Where it happened, in plain words. Example: "Behind the science block."
    private String location;

    // What happened, written by whoever reported it. Facts, not conclusions.
    // Example: "Two boys were pushing each other; one fell against the wall."
    @NotBlank
    private String description;

    // Every child in the incident, each with how they were involved. At least one,
    // because an incident with nobody in it is not an incident.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<EventParticipant> participants = new ArrayList<>();

    // Links to Staff.id for whoever reported it.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String reportedByStaffDocsId;

    // When it was reported, which can be well after it happened.
    // Example: 2026-08-19T07:10:00Z
    @NotNull
    private Instant reportedAt;

    // Links to Staff.id for any adults who saw it.
    @Builder.Default
    private List<String> witnessStaffDocsIds = new ArrayList<>();

    // Links to DocumentRecord.id for photographs, a written statement or similar.
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Anything worth knowing that is not part of what happened.
    // Example: "Reported the next morning; the boys had already made up."
    private String remarks;
}
