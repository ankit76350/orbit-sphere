package com.orbitastra.backend.models.new_new.support.embedded;

import com.orbitastra.backend.models.new_new.support.enums.AccommodationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One adjustment the school will actually make.
 *
 * <p>It has no collection of its own. A plan's accommodations are always read together with it.
 *
 * <p>These are the concrete things that change a child's day, and they are the reason the module
 * is worth building. A plan full of good intentions changes nothing; "twenty-five percent extra
 * time, a separate room, and a reader for the question paper" changes an exam.
 *
 * <p>{@code appliesInClassroom} and {@code appliesInExamination} are separate flags because the
 * two settings are genuinely different. Sitting at the front helps every day and means nothing
 * in an exam hall. Extra time means nothing in an ordinary lesson and is the whole point in an
 * exam. Several accommodations are one or the other, and a single "where does this apply" would
 * force a wrong answer for half of them.
 *
 * <p>{@code extraTimePercent} is a field rather than something buried in the description because
 * it is the one accommodation that has to be arithmetic. An invigilator needs to know that a
 * ninety-minute paper becomes a hundred and thirteen minutes for this child, and reading that
 * out of a sentence is how it gets got wrong.
 *
 * <p>{@code description} is required even where the type says most of it. A teacher reading
 * "ASSISTIVE_DEVICE" needs to know it is a hearing aid on the left ear and that the child should
 * sit on the right side of the room.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAccommodation {

    // What adjustment this is. Example: AccommodationType.EXTRA_TIME
    @NotNull
    private AccommodationType accommodationType;

    // What it means in practice, for whoever has to do it. Required even where the type
    // seems obvious.
    // Example: "Hearing aid in the left ear; seat on the right side of the room facing the
    // teacher."
    @NotBlank
    private String description;

    // Whether it applies in ordinary lessons. Example: true
    @NotNull
    @Builder.Default
    private Boolean appliesInClassroom = false;

    // Whether it applies in exams. Kept apart from the classroom flag because sitting at
    // the front means nothing in an exam hall and extra time means nothing in a lesson.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean appliesInExamination = false;

    // How much longer the child gets, as a share of the normal duration. Only for
    // EXTRA_TIME. A field rather than a sentence because an invigilator has to work out that
    // a ninety-minute paper becomes a hundred and thirteen minutes. Example: 25
    private Integer extraTimePercent;

    // Which subjects it applies to, by subject code, when it is not all of them. Empty means
    // every subject. Example: ["ENG", "HIN"]
    private java.util.List<String> subjectCodes;
}
