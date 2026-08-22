package com.orbitastra.backend.models.new_new.feedback.campaign.embedded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.feedback.campaign.enums.FeedbackQuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The summary of everybody's answers to one question.
 *
 * <p>{@code averageRating} is null for the question types that cannot be averaged, and that
 * null is meaningful rather than missing data. "Which topics did you find hard" has no mean,
 * and a zero there would read as a terrible score.
 *
 * <p>{@code distribution} is what makes this worth storing. See FeedbackRatingBucket: the
 * average alone cannot tell a divided class from an indifferent one.
 *
 * <p>**Text answers are not here.** A question of type TEXT contributes only its
 * {@code answeredCount}, and the words stay on the submissions where the visibility rules
 * still apply to them. Copying comments into the aggregate would move them past the one thing
 * protecting them, because the aggregate is the object that gets shown to the subject.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestionAggregate {

    // Links to FeedbackQuestion.questionCode. Example: "EXPLAINS_CLEARLY"
    @NotBlank
    private String questionCode;

    // The wording, copied so a released result reads on its own.
    // Example: "Does this teacher explain things in a way you understand?"
    @NotBlank
    private String questionText;

    // Which kind of question this was. Example: FeedbackQuestionType.RATING
    @NotNull
    private FeedbackQuestionType questionType;

    // How many people answered this particular question, which can be fewer than the
    // number who submitted when the question was optional. Example: 29
    @NotNull
    private Integer answeredCount;

    // The mean, for RATING only. Null for every other type, and that null means "cannot
    // be averaged" rather than "nobody answered". Example: 4.21
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal averageRating;

    // The share who said yes, for YES_NO only. Example: 86.20
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal yesPercent;

    // How the answers were spread. The part that says more than the average does.
    @Valid
    @Builder.Default
    private List<FeedbackRatingBucket> distribution = new ArrayList<>();
}
