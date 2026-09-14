package com.orbitastra.backend.dto.academics.gradingscheme.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.GradingScheme;

/**
 * One scheme as a row of #6's page — everything except the bands.
 *
 * <p><b>A separate record from {@link GradingSchemeResponse}, not a flag on it.</b> A twelve-row
 * page carrying twelve full band tables is a large response nobody reads: the CBSE scale alone is
 * eight bands of six fields, so a page of it is roughly six hundred values to render a dropdown
 * with twelve entries. #7 is one call away for the caller that actually wants them.
 *
 * <p><b>{@code bandCount} is what survives that trim</b>, and it earns its place: a scheme with
 * zero bands cannot exist — #1 refuses it — so the count is never a way of saying "empty". It is
 * how a person recognises a scale they know. Eight bands is the CBSE one; three is probably
 * descriptors.
 *
 * <p><b>No {@code warning} either.</b> The gap note is recomputed per read rather than stored, so
 * putting it on a row would mean walking every band of every scheme on the page to render a list
 * — and a warning nobody asked for, on a row that cannot act on it. #7 carries it, where the
 * bands it describes are also on screen.
 */
public record GradingSchemeSummary(
        String gradingSchemeDocsId,
        String name,
        String schemeVersion,
        GradingScaleType scaleType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal maximumValue,

        int bandCount,
        Boolean active) {

    public static GradingSchemeSummary fromScheme(GradingScheme scheme) {
        return new GradingSchemeSummary(
                scheme.getId(),
                scheme.getName(),
                scheme.getSchemeVersion(),
                scheme.getScaleType(),
                scheme.getMaximumValue(),
                scheme.getGradeBands() == null ? 0 : scheme.getGradeBands().size(),
                scheme.getActive());
    }
}
