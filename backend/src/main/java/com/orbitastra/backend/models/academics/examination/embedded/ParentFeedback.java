package com.orbitastra.backend.models.academics.examination.embedded;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One guardian's own view of how the child is getting on, on a Holistic Progress Card.
 *
 * <p>It has no collection of its own. It lives on the card, one entry per guardian, so each
 * parent can write their own view in their own words.
 *
 * <p><b>It can be added after the card is published.</b> Families usually write it after they
 * have seen the card, at the parent-teacher meeting. Everything the school wrote on the card is
 * frozen once published; the family's reply is the one part that is not, and adding it does not
 * make a new version of the card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentFeedback {

    // Links to Guardian.id — who wrote it. One entry per guardian.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String guardianDocsId;

    // What they wrote, in their own words.
    // Example: "She talks about school at home now, which she did not last year."
    @NotBlank
    private String feedback;

    // When it was written down. Example: 2026-10-18T06:30:00Z
    @NotNull
    private Instant givenAt;

    // Links to the Staff.id who typed it in on the family's behalf, when signed in as staff.
    // Empty when nobody did. Example: "67aa15d9dc3f7d0044444444"
    private String recordedByDocsId;
}
