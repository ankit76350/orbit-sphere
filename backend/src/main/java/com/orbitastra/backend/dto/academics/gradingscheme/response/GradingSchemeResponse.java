package com.orbitastra.backend.dto.academics.gradingscheme.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.GradingScheme;

/**
 * One grading scheme, as #1 and #7 return it.
 *
 * <p><b>{@code gradingSchemeDocsId} is the field that matters to everything else.</b> Three places
 * reference a scheme by it — {@code ClassSubject}, {@code Exam} and {@code ReportCard} — which is
 * why a scheme is a document with an id, and why the URL addresses it by that id rather than by
 * the {@code name + schemeVersion} pair a person reads.
 *
 * <p><b>{@code warning} is not an error.</b> It carries the gap note: a value that falls between
 * two bands has no grade, which is a real hole in a real table and is nonetheless a normal state
 * for a scheme a school is still entering. It is <b>recomputed on every read</b> rather than
 * stored, so a school that fixes its bands stops being told about them — a stored sentence would
 * outlive the problem it described.
 *
 * <p><b>An overlap never appears here</b>, because an overlap is refused at write. The asymmetry
 * is deliberate: a gap means one mark has no grade, which is visible and fixable; an overlap means
 * one mark has two, and which wins depends on the order the bands happen to be stored in.
 */
public record GradingSchemeResponse(
        String gradingSchemeDocsId,
        String name,
        String schemeVersion,
        GradingScaleType scaleType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal maximumValue,

        int bandCount,
        List<GradeBandResponse> gradeBands,
        Boolean active,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static GradingSchemeResponse fromScheme(GradingScheme scheme) {
        return fromScheme(scheme, null, null);
    }

    public static GradingSchemeResponse fromScheme(GradingScheme scheme, String warning,
            String nextStep) {

        List<GradeBandResponse> bands = scheme.getGradeBands() == null ? List.of()
                : scheme.getGradeBands().stream().map(GradeBandResponse::fromBand).toList();

        return new GradingSchemeResponse(
                scheme.getId(),
                scheme.getName(),
                scheme.getSchemeVersion(),
                scheme.getScaleType(),
                scheme.getMaximumValue(),
                bands.size(),
                bands,
                scheme.getActive(),
                warning,
                nextStep);
    }
}
