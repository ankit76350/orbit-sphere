package com.orbitastra.backend.models.feedback.campaign.embedded;

import java.util.ArrayList;
import java.util.List;

import com.orbitastra.backend.models.feedback.campaign.enums.FeedbackQuestionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One answer to one question.
 *
 * <p>{@code questionText} is copied in at submission time, and this is the same rule
 * FeeInvoiceLine follows for the fee head name. Somebody reading a submission two years later
 * has to be able to see what was actually asked. A form reworded since would otherwise put
 * this term's question above last term's answer, which is worse than having no record at all
 * because it reads as though it is right.
 *
 * <p>{@code questionType} is copied for the same reason: it says which of the fields below
 * to read, and it must be the type as it was when the person answered.
 *
 * <p>Four value fields and only one is used, decided by the type. That is deliberate rather
 * than one string holding everything: a rating stored as text cannot be averaged without
 * parsing, and a parse that fails on one row silently drops it out of the average.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAnswer {

    // Links to FeedbackQuestion.questionCode on the topic.
    // Example: "EXPLAINS_CLEARLY"
    @NotBlank
    private String questionCode;

    // The wording as it was when this person answered it.
    // Example: "Does this teacher explain things in a way you understand?"
    @NotBlank
    private String questionText;

    // The type as it was when this person answered. Says which field below to read.
    // Example: FeedbackQuestionType.RATING
    @NotNull
    private FeedbackQuestionType questionType;

    // The answer to a RATING question. Example: 4
    private Integer ratingValue;

    // The answer to a YES_NO question. Example: true
    private Boolean booleanValue;

    // The chosen options, for SINGLE_CHOICE and MULTI_CHOICE. A single choice is a list
    // of one, so counting code does not need two paths. Example: ["About right"]
    @Builder.Default
    private List<String> selectedOptions = new ArrayList<>();

    // The answer to a TEXT question, in the person's own words.
    // Example: "She goes back over things when we don't get it, which helps."
    private String textValue;
}
