package com.orbitastra.backend.dto.academics.gradingscheme.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;

/**
 * One band, as every grading endpoint returns it.
 *
 * <p><b>Its own file rather than a record nested in {@link GradingSchemeResponse}</b>, because #8
 * returns a single band with no scheme around it and a nested type would have meant two shapes for
 * one thing. The section list in {@code schoolclass} learned this the same way.
 *
 * <p><b>{@code minimumValue} and {@code maximumValue} are omitted when null</b>, which is how a
 * {@code DESCRIPTOR} band comes back. A client seeing the keys absent is being told the band has
 * no range, rather than being handed a zero it might do arithmetic with.
 */
public record GradeBandView(
        String gradeCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal minimumValue,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal maximumValue,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal gradePoint,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,

        Boolean passed) {

    public static GradeBandView fromBand(GradeBand band) {
        return new GradeBandView(
                band.getGradeCode(),
                band.getMinimumValue(),
                band.getMaximumValue(),
                band.getGradePoint(),
                band.getDescription(),
                band.getPassed());
    }
}
