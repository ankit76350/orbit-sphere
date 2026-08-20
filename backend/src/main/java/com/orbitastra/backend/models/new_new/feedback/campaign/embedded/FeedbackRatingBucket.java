package com.orbitastra.backend.models.new_new.feedback.campaign.embedded;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How many people gave one particular answer.
 *
 * <p>The distribution matters more than the average, and a school that only stores the mean
 * will miss the thing worth knowing. Thirty students split fifteen at five and fifteen at
 * one average out to three, and so do thirty students who all said three. The first is a
 * teacher half the class cannot follow. The second is a teacher everybody finds ordinary.
 * They need completely different conversations and the average cannot tell them apart.
 *
 * <p>Also used for the choice question types, where {@code label} holds the option text
 * instead of a number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRatingBucket {

    // What was answered. A rating as text, or the option chosen. Example: "4"
    @NotNull
    private String label;

    // How many people answered that. Example: 11
    @NotNull
    private Integer count;
}
