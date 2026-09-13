package com.orbitastra.backend.models.academics.grading.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One result band embedded in a GradingScheme.
 *
 * <p><b>The bounds are nullable, and which schemes may omit them is a service
 * rule.</b> A PERCENTAGE or POINT scheme resolves a mark by finding the band
 * where {@code minimumValue <= mark <= maximumValue}, so both are required and
 * the set must not overlap. A DESCRIPTOR scheme has no mark to compare — a
 * teacher picks "Developing" directly — so both must be absent.
 *
 * <p>They were {@code @NotNull} until 2026-09-13, which left
 * {@code GradingScaleType.DESCRIPTOR} impossible to store: a descriptor scheme
 * had to invent numbers for fields nothing would read. Bean validation cannot
 * express "required unless the parent says otherwise", so the rule moved to
 * {@code GradingHelper} where it can see the scale type.
 *
 * <p>{@code passed} is stored rather than derived. A pass mark is not always 33
 * — a practical may pass at 40 and some boards have no pass/fail at all — so
 * the school states it per band instead of the code assuming one.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeBand {

    // Example: "A1"
    @NotBlank
    private String gradeCode;

    // Inclusive lower boundary. Required for PERCENTAGE and POINT, absent for
    // DESCRIPTOR. Example: 91.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal minimumValue;

    // Inclusive upper boundary. Required for PERCENTAGE and POINT, absent for
    // DESCRIPTOR. Example: 100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumValue;

    // Optional grade point. Example: 10.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal gradePoint;

    // Example: "Outstanding"
    private String description;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean passed = true;
}
