package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalTime;

import com.orbitastra.backend.models.academics.enums.TimetableSlotType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One period of one section. Endpoint #1.
 *
 * <p><b>Which fields are required depends on {@code slotType}</b>, and bean validation cannot
 * express that — a {@code LESSON} needs a subject and a teacher, a {@code BREAK} must carry
 * neither. So {@code subjectCode} and {@code teacherDocsId} are optional here and decided in
 * {@code TimetableHelper}, where the slot type is visible. The same split
 * {@link com.orbitastra.backend.dto.academics.gradingscheme.request.GradeBandRequest} makes for a
 * band's bounds.
 *
 * <p><b>No {@code id}.</b> The service generates one ObjectId per entry before insertion, because
 * MongoDB does not generate {@code _id} for embedded documents — rule 1 of the model's persistence
 * contract. Accepting one from the caller would let two entries share an id, and every targeted
 * update afterwards addresses an entry by exactly that.
 */
public record TimetableEntryRequest(

        /** The school's own label for the period — {@code "P03"}. Unique per section per day. */
        @NotBlank @Size(max = 40) String periodCode,

        /** The class this period belongs to. Must be a class of this school and this year. */
        @NotBlank @Size(max = 60) String classDocsId,

        /** Which section of it. Must exist in that class's {@code sections[]}. */
        @NotBlank @Size(max = 20) String sectionNo,

        /** {@code LESSON} · {@code BREAK} · {@code ASSEMBLY} · {@code ACTIVITY}. */
        @NotNull TimetableSlotType slotType,

        /**
         * What is taught. <b>Required for {@code LESSON}, refused otherwise.</b>
         *
         * <p><b>It must be a subject this SECTION studies</b>, which is not the same as "a subject
         * of the class". A subject created with no {@code sectionNo} is class-wide and every
         * section takes it; one created with a {@code sectionNo} belongs to that section alone.
         * So 10-C may be given German and 10-A may not, while both take Maths.
         */
        @Size(max = 40) String subjectCode,

        /** Who takes it. <b>Required for {@code LESSON}, refused otherwise.</b> */
        @Size(max = 60) String teacherDocsId,

        /** What a timetable prints for a non-lesson — {@code "Lunch Break"}. */
        @Size(max = 120) String slotLabel,

        /** When it starts. Strictly before {@code endTime}. */
        @NotNull LocalTime startTime,

        /** When it ends. */
        @NotNull LocalTime endTime,

        /**
         * The room, when the school allocates one.
         *
         * <p><b>Normally absent</b>, and that is not an omission: in most Indian schools a section
         * has one classroom all day and the timetable moves teachers, not children. It matters for
         * the periods that break the pattern — a practical in the lab, games in the hall — which
         * are exactly the periods two sections can be sent to at once.
         */
        @Size(max = 60) String facilityResourceDocsId) {
}
