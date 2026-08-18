package com.orbitastra.backend.models.new_new.conduct.embedded;

import com.orbitastra.backend.models.new_new.conduct.enums.ParticipantRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One child in an incident, and how they were involved.
 *
 * <p>It has no collection of its own. The people in an incident are always read with
 * the incident.
 *
 * <p>{@code role} is the whole reason this is a small object rather than a plain list
 * of student ids. A bullying incident has a child doing it and a child it was done to.
 * Storing both as "students involved" is how a school ends up disciplining a victim,
 * and it is exactly what the old DisciplineLog could not prevent.
 *
 * <p>A participant with role AFFECTED never gets a case opened against them for that
 * event. They may still need looking after, which is a different thing and belongs
 * with support rather than discipline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipant {

    // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // How this child was involved. Example: ParticipantRole.RESPONSIBLE
    @NotNull
    private ParticipantRole role;

    // What this particular child did or had happen to them, when the event's own
    // description does not cover it.
    // Example: "Pushed first, after being called a name."
    private String note;
}
