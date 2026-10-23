package com.orbitastra.backend.dto.academics.studentmark.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #19 needs to enter marks on one paper — one child, several or all. Re-sending
 * a child corrects their mark.
 *
 * @param marks   one per child, each child once
 * @param version <b>the paper's version</b>, as #18 last gave it. It moves on with every save.
 */
public record StudentMarksEnterRequest(
        @NotEmpty @Size(max = 300) List<@NotNull @Valid Mark> marks,
        @NotNull Long version) {

    /**
     * One child's mark: the marks they scored, or — for a grade-only subject — the grade itself.
     * One of the two, never both.
     *
     * @param studentDocsId the child — one the mark sheet lists as PRESENT
     * @param obtainedMarks 0 to the paper's maximumMarks. The grade is worked out from it
     * @param gradeCode     a band of the paper's grading scheme, sent instead of marks
     * @param remarks       what the teacher writes. Optional
     */
    public record Mark(
            @NotBlank @Size(max = 60) String studentDocsId,
            BigDecimal obtainedMarks,
            @Size(max = 20) String gradeCode,
            @Size(max = 500) String remarks) {
    }
}
