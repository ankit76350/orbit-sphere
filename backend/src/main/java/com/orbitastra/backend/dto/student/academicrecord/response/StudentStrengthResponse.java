package com.orbitastra.backend.dto.student.academicrecord.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #22 answers: how many children are placed in each class and section of one year, next to
 * how many seats each section has.
 *
 * <p>Only {@code ACTIVE} records are counted — a closed record is somebody who has left that
 * section. Every section of every class in the year is listed, <b>including empty ones</b>, so a
 * section with nobody in it reads as {@code 0} rather than being missing.
 */
public record StudentStrengthResponse(

        String academicYear,

        /** Children placed across the whole year. */
        long totalEnrolled,

        /** Seats across every section that declares a capacity. */
        long totalCapacity,

        List<ClassRow> classes) {

    /** One class and its sections. */
    public record ClassRow(

            String classDocsId,

            /** Null when the class document is gone but records still point at it. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            /** Null when the class document is gone. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Boolean active,

            /** Children placed in this class, all sections together. */
            long enrolled,

            /** The sum of the section capacities that are declared. */
            long capacity,

            List<SectionRow> sections) {
    }

    /** One section. */
    public record SectionRow(

            String sectionNo,

            /** Null when the section is not on the class any more. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Boolean active,

            /** Children placed in this section right now. */
            long enrolled,

            /** Seats in this section. Null when the section does not declare one. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Integer capacity,

            /** capacity minus enrolled. Null when there is no capacity. Negative when over. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Long seatsLeft,

            /** True when more children are placed than the section has seats. */
            boolean overCapacity,

            /**
             * False when records name a section the class no longer has. They are still
             * counted, so the numbers add up, and this flag says why the row looks odd.
             */
            boolean onClass) {
    }
}
