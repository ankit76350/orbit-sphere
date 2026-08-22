package com.orbitastra.backend.models.new_new.feedback.campaign.embedded;

import java.util.ArrayList;
import java.util.List;


import com.orbitastra.backend.models.new_new.feedback.campaign.enums.FeedbackQuestionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One question on a feedback form.
 *
 * <p>Embedded in FeedbackTopic rather than in the campaign, and that is a deliberate choice
 * with a consequence. Questions on the topic mean every term's drive asks the same thing, so
 * this December can be compared with last December. Questions on the campaign would let
 * somebody reword them each time, and then the two numbers are not measuring the same thing
 * and the comparison quietly lies.
 *
 * <p>{@code questionCode} is what an answer points at, and it must not be renamed once
 * submissions exist. Renaming it orphans every answer already given, the same rule
 * {@code headCode} and {@code stopCode} follow.
 *
 * <p>Rewording {@code questionText} is allowed, because an answer keeps its own copy of the
 * wording it was given — see FeedbackAnswer. Changing what a question *means* is not a
 * rewording; it is a new question with a new code, and the old one should be retired.
 *
 * <p>{@code options} is only for the two choice types. A rating question with options in it
 * is somebody misunderstanding the form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestion {

    // Order the question appears in. Example: 1
    @NotNull
    private Integer questionNo;

    // The stable key an answer points at. Never renamed once submissions exist.
    // Example: "EXPLAINS_CLEARLY"
    @NotBlank
    private String questionCode;

    // What the person is actually asked. May be reworded; answers keep their own copy.
    // Example: "Does this teacher explain things in a way you understand?"
    @NotBlank
    private String questionText;

    // How it is answered, which decides whether it can be averaged.
    // Example: FeedbackQuestionType.RATING
    @NotNull
    private FeedbackQuestionType questionType;

    // The choices, for SINGLE_CHOICE and MULTI_CHOICE only.
    // Example: ["Too fast", "About right", "Too slow"]
    @Builder.Default
    private List<String> options = new ArrayList<>();

    // Whether the form can be submitted without answering this. Example: true
    @NotNull
    @Builder.Default
    private Boolean required = true;

    // A note under the question, for anything that needs explaining.
    // Example: "Think about the whole term, not just this week."
    private String helpText;
}
