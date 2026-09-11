package com.orbitastra.backend.dto.academics.academicterm.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;

/**
 * One term, as every term endpoint returns it.
 *
 * <p><b>{@code termDocsId} is the field that matters to everything else.</b> Six documents across
 * three modules — {@code Exam}, {@code ReportCard}, {@code HolisticProgressCard},
 * {@code FeedbackCampaign}, {@code FeeInstallment} and {@code FeeInvoice} — reference a term by
 * it, which is why a term is a document with an id rather than an embedded row like a section.
 *
 * <p><b>{@code warning} is not an error.</b> It carries the weight-sum note, which is reported
 * rather than refused: a school moving from 20/80 to 30/70 passes through a total of 110, and an
 * endpoint that refused that would make the values impossible to change. Absent when the weights
 * are fine or unused, so a client that ignores it is never wrong about anything that mattered.
 */
public record AcademicTermResponse(
        String termDocsId,
        String academicYear,
        String termCode,
        String name,
        Integer sequence,
        LocalDate startDate,
        LocalDate endDate,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal weightPercent,

        Boolean resultsLocked,
        Boolean active,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static AcademicTermResponse fromTerm(AcademicTerm term) {
        return fromTerm(term, null, null);
    }

    public static AcademicTermResponse fromTerm(AcademicTerm term, String warning,
            String nextStep) {

        return new AcademicTermResponse(
                term.getId(),
                term.getAcademicYear(),
                term.getTermCode(),
                term.getName(),
                term.getSequence(),
                term.getStartDate(),
                term.getEndDate(),
                term.getWeightPercent(),
                term.getResultsLocked(),
                term.getActive(),
                warning,
                nextStep);
    }
}
