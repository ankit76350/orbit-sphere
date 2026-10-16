package com.orbitastra.backend.dto.academics.exam.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.enums.ExamType;
import com.orbitastra.backend.models.academics.examination.Exam;

/**
 * One exam, with its term and grading scheme named.
 */
public record ExamResponse(

        String examDocsId,

        String academicYear,

        String name,

        String termDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String termName,

        ExamType examType,

        LocalDate startDate,

        LocalDate endDate,

        /** Absent when the school does not weight exams. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal weightPercent,

        /** Absent when the exam has no default grading scheme. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gradingSchemeDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gradingSchemeName,

        ExamStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant publishedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String publishedByDocsId,

        /** For the next write to this exam — edit, publish and cancel all want it. */
        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** Builds the answer from the saved exam and the two names behind its ids. */
    public static ExamResponse of(Exam exam, String termName, String gradingSchemeName,
            String nextStep) {

        return new ExamResponse(
                exam.getId(),
                exam.getAcademicYear(),
                exam.getName(),
                exam.getTermDocsId(),
                termName,
                exam.getExamType(),
                exam.getStartDate(),
                exam.getEndDate(),
                exam.getWeightPercent(),
                exam.getGradingSchemeDocsId(),
                gradingSchemeName,
                exam.getStatus(),
                exam.getPublishedAt(),
                exam.getPublishedByDocsId(),
                exam.getVersion(),
                nextStep);
    }
}
