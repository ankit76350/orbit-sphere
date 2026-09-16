package com.orbitastra.backend.dto.academics.gradingscheme.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;

/**
 * One mark, turned into a grade. Endpoint #8.
 *
 * <h2>The whole band comes back, not just the code</h2>
 *
 * <p>{@code gradePoint} feeds a CGPA and {@code passed} decides whether a subject is cleared —
 * both are needed by whatever asked for the grade. A response carrying only {@code gradeCode}
 * would guarantee a second call per mark, and a report card resolves one per subject per term.
 *
 * <h2>The scheme is echoed with it</h2>
 *
 * <p>Not decoration: the same mark resolves differently under two versions of one rulebook, which
 * is the entire reason {@code schemeVersion} exists. A stored result that records "82 → A2" without
 * saying which rulebook said so cannot be checked later, and #9 exists precisely because "what did
 * A1 mean in 2026" is a real question.
 *
 * <h2>What is deliberately absent</h2>
 *
 * <p><b>No {@code warning}.</b> The gap note on #1, #3 and #7 describes the whole scale; this
 * answers about one value. A value that fell in a gap never reaches this response at all — it is a
 * {@code 404 GRADE_NOT_RESOLVABLE} naming the two bands it fell between.
 *
 * <p><b>Nothing is stored.</b> This is arithmetic over one document, which is why it is a
 * {@code GET}. A resolved grade is written by whatever records the mark, not by the act of asking.
 */
public record GradeResolutionResponse(

        String gradingSchemeDocsId,

        /** Which rulebook answered — the same mark resolves differently under another version. */
        String schemeName,
        String schemeVersion,
        GradingScaleType scaleType,
        BigDecimal maximumValue,

        /** The value that was asked about, echoed exactly as it was read. */
        BigDecimal value,

        /** What prints on the card. */
        String gradeCode,

        /** What it is worth in a CGPA. Absent when the scheme grades without points. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal gradePoint,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,

        /** Whether this band clears the subject. Stored per band, never derived from a threshold. */
        Boolean passed,

        /** The band's own bounds, so a caller can show why this grade was chosen. */
        BigDecimal bandMinimumValue,
        BigDecimal bandMaximumValue,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static GradeResolutionResponse of(GradingScheme scheme, GradeBand band,
            BigDecimal value, String nextStep) {

        return new GradeResolutionResponse(
                scheme.getId(),
                scheme.getName(),
                scheme.getSchemeVersion(),
                scheme.getScaleType(),
                scheme.getMaximumValue(),
                value,
                band.getGradeCode(),
                band.getGradePoint(),
                band.getDescription(),
                band.getPassed(),
                band.getMinimumValue(),
                band.getMaximumValue(),
                nextStep);
    }
}
