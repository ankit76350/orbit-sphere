package com.orbitastra.backend.models.new_new.support.embedded;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing the school is trying to get a child to.
 *
 * <p>It has no collection of its own. A plan's goals are read with it.
 *
 * <p>{@code baseline} is the field that makes a goal mean anything. "Improve reading" is not a
 * goal; "reads twenty words a minute now, aiming for forty by December" is one, because in
 * December somebody can say whether it worked.
 *
 * <p>Without a baseline written down at the start, every review turns into an argument about
 * whether the child has improved, and nobody can settle it. The baseline is the thing everybody
 * forgets to record and then wishes they had.
 *
 * <p>{@code progressNote} is filled in at review rather than at the start. It is the honest
 * answer to whether the target was reached, and a plan whose goals are all left blank at review
 * is a plan that was written and never read again.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportGoal {

    // What the school is aiming for, in words somebody can check.
    // Example: "Read a Class III passage aloud at forty words a minute."
    @NotBlank
    private String goal;

    // Where the child is now, recorded before anything starts. Without this, no review can
    // say whether the goal was met.
    // Example: "Reads twenty words a minute with frequent substitutions."
    @NotBlank
    private String baseline;

    // How anybody will know it has been reached.
    // Example: "Timed reading check by the remedial teacher at the end of term."
    private String measure;

    // What actually happened, filled in at review rather than at the start.
    // Example: "Thirty-four words a minute in December. Short of target but a clear gain."
    private String progressNote;
}
