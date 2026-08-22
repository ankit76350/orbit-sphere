package com.orbitastra.backend.models.common.embedded;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One guardian who was told about a clinic visit, and when.
 *
 * <p>It has no collection of its own. These are only ever read together with the
 * record they belong to.
 *
 * <p>It sits in common because more than one part of the school has to prove it told
 * a family something. A child sent home ill and a child in trouble both need it, and
 * neither owns the idea.
 *
 * <p>Two fields rather than two parallel lists, because a name and a time only mean
 * something as a pair. Two lists could fall out of step and leave nobody able to say
 * which parent was rung at which time, which is the one thing this record is for.
 *
 * <p>It is kept as a list because ringing one guardian often is not the end of
 * it. The nurse tries the mother, gets no answer, tries the father, reaches him. Both
 * matter: one shows the school tried, the other shows somebody actually knows.
 *
 * <p>Only guardians who were **reached** belong here. Attempts that failed go in the
 * parent record's remarks, because a list of people who did not answer is not a
 * record that anybody was told.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianInformed {

    // Links to Guardian.id for the person who was told.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String guardianDocsId;

    // When they were told. Example: 2026-08-19T05:40:00Z
    @NotNull
    private Instant informedAt;
}
