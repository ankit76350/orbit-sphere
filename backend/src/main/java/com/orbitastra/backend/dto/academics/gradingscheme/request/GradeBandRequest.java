package com.orbitastra.backend.dto.academics.gradingscheme.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One band of a grading scheme. Endpoint #1.
 *
 * <p><b>The bounds are optional here and required by the service</b>, because whether they are
 * required depends on the parent's {@code scaleType} and bean validation cannot see a sibling
 * field. A {@code PERCENTAGE} or {@code POINT} band needs both; a {@code DESCRIPTOR} band must
 * have neither. Marking them {@code @NotNull} would make a descriptor scheme unsendable — the
 * state the model was in until 2026-09-13, and the reason the model's own bounds are nullable now.
 *
 * <p><b>No {@code @DecimalMin} either</b>, and for a related reason: the floor is 0 on a
 * percentage and on a point scale alike, but the ceiling is the scheme's {@code maximumValue},
 * which this record cannot see. Both ends are checked together in {@code GradingHelper}, so
 * checking one of them here would split one rule across two files.
 */
public record GradeBandRequest(

        /** The grade as it prints — {@code A1}, {@code 7}, {@code DEVELOPING}. Unique in the set. */
        @NotBlank @Size(max = 40) String gradeCode,

        /** Inclusive lower bound. Required for PERCENTAGE and POINT, refused for DESCRIPTOR. */
        BigDecimal minimumValue,

        /** Inclusive upper bound — a band ending 90 includes 90. Same rule as above. */
        BigDecimal maximumValue,

        /**
         * What this band is worth in a CGPA. Optional.
         *
         * <p>Null means the scheme grades without points, which is a normal way to grade — not a
         * missing value. Nothing here requires all bands to agree: a school may point some and
         * not others, because unlike a term weight a grade point is not summed across the set.
         */
        BigDecimal gradePoint,

        /** What a parent reads beside the code — "Outstanding". Optional. */
        @Size(max = 200) String description,

        /**
         * Whether this band is a pass. Null means {@code true}.
         *
         * <p>Stored rather than derived from a threshold: a practical may pass at 40 where the
         * theory paper passes at 33, and some boards have no pass/fail concept at all. Defaulting
         * to true means a scheme author lists the failing bands rather than repeating "passed"
         * seven times.
         */
        Boolean passed) {
}
