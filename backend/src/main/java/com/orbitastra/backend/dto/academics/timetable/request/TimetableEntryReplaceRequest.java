package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalTime;

import com.orbitastra.backend.models.academics.enums.TimetableSlotType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One period of a day being <b>replaced</b>. Endpoint #2.
 *
 * <h2>The only difference from {@link TimetableEntryRequest} is the id, and it is the whole point</h2>
 *
 * <p>#1 generates every id, and its request deliberately has no field for one: accepting an id on a
 * create would let two entries share it, and every targeted update afterwards addresses an entry by
 * exactly that. A <b>replace</b> is the one write where the caller legitimately knows an id —
 * because it just read the day — and sending it back is how a period that survives the replace
 * <b>keeps its identity</b>, and with it the attendance pointing at it.
 *
 * <p>Two near-identical records rather than one with a sometimes-legal field: the create then
 * <i>cannot</i> accept an id at all, which is a stronger guarantee than refusing one at runtime.
 *
 * <p><b>Which fields are required still depends on {@code slotType}</b>, and bean validation cannot
 * express that — a {@code LESSON} needs a subject and a teacher, a {@code BREAK} must carry no
 * subject. So {@code subjectCode} and {@code teacherDocsId} are optional here and decided in
 * {@code TimetableHelper}, where the slot type is visible.
 */
public record TimetableEntryReplaceRequest(

        /**
         * The id this period already has, or absent for one being added.
         *
         * <p><b>Absent means "new", not "any"</b> — a fresh ObjectId is generated for it. Sending
         * one is a claim that this period already exists in this day, so an id that is not in the
         * stored day is {@code 404 TIMETABLE_ENTRY_NOT_FOUND} rather than quietly accepted: an id
         * borrowed from another date would make {@code AttendanceSession.timetableEntryId}
         * ambiguous, which is the one thing the generated ids exist to prevent.
         *
         * <p>An id sent twice in one request is {@code 400 DUPLICATE_TIMETABLE_ENTRY_ID}.
         */
        @Size(max = 60) String timetableEntryId,

        /** The school's own label for the period — {@code "P03"}. Unique per section per day. */
        @NotBlank @Size(max = 40) String periodCode,

        /** The class this period belongs to. Must be a class of this school and this year. */
        @NotBlank @Size(max = 60) String classDocsId,

        /** Which section of it. Must exist in that class's {@code sections[]}. */
        @NotBlank @Size(max = 20) String sectionNo,

        /** {@code LESSON} · {@code BREAK} · {@code ASSEMBLY} · {@code ACTIVITY}. */
        @NotNull TimetableSlotType slotType,

        /**
         * What is taught. <b>Required for a {@code LESSON}, refused on every other slot type</b> —
         * a break teaches nothing, so a subject there is either a mistake or a lesson wearing the
         * wrong type.
         *
         * <p><b>It must be a subject this SECTION studies.</b> A subject created with no
         * {@code sectionNo} is class-wide and every section takes it; one created with a
         * {@code sectionNo} belongs to that section alone.
         */
        @Size(max = 40) String subjectCode,

        /**
         * Who takes it, or who supervises it.
         *
         * <p><b>Required for a {@code LESSON}, optional for everything else</b> — somebody
         * supervises lunch, runs the assembly and takes the activity. A teacher named on a break
         * still counts against their day: they cannot supervise lunch and teach period 4 at once.
         */
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
         * <p><b>Normally absent</b>: in most Indian schools a section has one classroom all day and
         * the timetable moves teachers, not children.
         */
        @Size(max = 60) String facilityResourceDocsId) {
}
