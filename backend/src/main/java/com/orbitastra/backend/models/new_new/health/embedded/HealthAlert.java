package com.orbitastra.backend.models.new_new.health.embedded;

import com.orbitastra.backend.models.new_new.health.enums.AlertSeverity;
import com.orbitastra.backend.models.new_new.health.enums.HealthAlertType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing about a child that staff have to know.
 *
 * <p>It has no collection of its own. A child has a handful of these and they are
 * always read together with the profile, because the question is never "what is
 * this child's third allergy" but "what do I need to know about this child".
 *
 * <p>{@code whatToDo} is the field that matters most and is the reason this is not
 * just a list of words. "Nut allergy" tells a teacher nothing useful at the moment
 * it counts. "Do not let him eat anything from outside. If his face swells, use the
 * adrenaline pen in the staff room and call an ambulance" is a plan somebody can
 * follow while frightened.
 *
 * <p>{@code severity} decides what a teacher sees first. A nut allergy that can
 * kill and a dislike of onions are both allergies, and showing them the same way is
 * how the important one gets missed.
 *
 * <p>The details are held as ordinary text rather than encrypted, unlike the
 * clinical notes on a visit. That is on purpose: an alert is no use locked away.
 * The whole point is that a teacher on a trip, with no signal and no clinical
 * training, can read it in seconds. What protects it is who is allowed to see the
 * child at all, not encryption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlert {

    // What kind of thing this is. Example: HealthAlertType.ALLERGY
    @NotNull
    private HealthAlertType alertType;

    // How serious it is, which decides how loudly it is shown.
    // Example: AlertSeverity.LIFE_THREATENING
    @NotNull
    private AlertSeverity severity;

    // Short name staff will recognise. Example: "Peanut allergy"
    @NotBlank
    private String title;

    // What is actually going on, in plain words.
    // Example: "Swells and struggles to breathe within minutes of eating peanuts."
    private String description;

    // What a member of staff should do. The most important field here, because it
    // has to be followed by somebody who is not a nurse and is frightened.
    // Example: "Use the adrenaline pen in the staff room, then call an ambulance
    // and ring the mother on the number in the profile."
    private String whatToDo;

    // Whether this is shown on every screen that names the child, rather than
    // only in the health record. Example: true
    @NotNull
    @Builder.Default
    private Boolean showOnStudentScreens = false;

    // Who told the school, so it can be checked later.
    // Example: "Mother, at admission. Paediatrician's letter on file."
    private String reportedBy;
}
