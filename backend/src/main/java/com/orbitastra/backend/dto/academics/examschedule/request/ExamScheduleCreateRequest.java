package com.orbitastra.backend.dto.academics.examschedule.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #8 needs to add a paper: one subject, one class, one or more sections, all at
 * the same date and time. One paper row is saved per section.
 */
public record ExamScheduleCreateRequest(

        /** The class sitting the paper. Must be a class of this year. */
        @NotBlank @Size(max = 60) String classDocsId,

        /** The sections sitting it together — at least one. Each must exist in the class. */
        @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 20) String> sectionNos,

        /** The subject. Must be taught in every section sent. */
        @NotBlank @Size(max = 60) String subjectCode,

        /**
         * The part of the subject — "Theory", "Practical", "Oral". Leave it out when the subject
         * has one paper. Unique with the subject for a section, ignoring case.
         */
        @Size(max = 120) String componentName,

        /** The day of the paper. Must be inside the exam's dates, and a working day. */
        @NotNull LocalDate examDate,

        /** When it starts, in the school's own time. */
        @NotNull LocalTime startTime,

        /** When it ends. Must be after startTime. */
        @NotNull LocalTime endTime,

        /** The most marks a child can get. Above 0. */
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal maximumMarks,

        /** The marks needed to pass. Optional, and at most maximumMarks. */
        @DecimalMin("0") BigDecimal passingMarks,

        /** The room or hall. Optional while the datesheet is still being made. */
        @Size(max = 60) String facilityResourceDocsId,

        /** Staff on duty in the hall. Optional. */
        @Size(max = 20) List<@NotBlank @Size(max = 60) String> invigilatorDocsIds) {
}
