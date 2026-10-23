package com.orbitastra.backend.dto.academics.examschedule.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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

        /**
         * The rooms the paper is written in, each with how many students sit there and who is on
         * duty. Several when the students do not fit in one. Optional while the datesheet is still
         * a draft.
         */
        @Valid @Size(max = 20) List<@NotNull @Valid Room> rooms) {

    /** One room of the paper, for all the sections sent together. */
    public record Room(

            /** The room. Must be this school's and in use. */
            @NotBlank @Size(max = 60) String roomDocsId,

            /**
             * How many students sit here — all the sections sent, together, not one section.
             * Optional. When given, at least 1, and with everyone else in the room at that time,
             * within its capacity. Without it the room cannot be shared with another sitting.
            */
            @Min(1) Integer seatCount,

            /** Staff on duty in this room. Optional. A person can be in one room only. */
            @Size(max = 20) List<@NotBlank @Size(max = 60) String> invigilatorDocsIds) {
    }
}
