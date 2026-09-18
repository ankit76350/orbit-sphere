package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Size;

/**
 * What one period may be corrected to. Endpoint #4 — <b>the substitution</b>.
 *
 * <h2>Every field is optional, and absent means "leave it"</h2>
 *
 * <p>This is the write the module exists for: a teacher calls in sick at 07:40 and six periods need
 * covering before 08:00. What changes is one field of one period, so sending the other six back
 * unchanged would be a chance to get one of them wrong.
 *
 * <h2>{@code ""} clears; absent leaves alone</h2>
 *
 * <p>The distinction the rest of this project makes, and it is needed here: a room booked for a
 * practical is removed by sending {@code facilityResourceDocsId: ""}, and there is no other way to
 * say it. An absent key and a blank one would otherwise be the same request with two meanings.
 *
 * <p>It does not apply to {@code periodCode}, {@code startTime} and {@code endTime}, which a period
 * cannot be without.
 *
 * <h2>What is NOT here, and why</h2>
 *
 * <p><b>{@code classDocsId} and {@code sectionNo}.</b> Moving a period to another section is
 * deleting one and adding another — pretending otherwise keeps an attendance session pointing at a
 * period that changed identity underneath it.
 *
 * <p><b>{@code slotType}.</b> It decides which other fields are legal, so turning a {@code LESSON}
 * into a {@code BREAK} in place would leave a subject on a break. Delete and add.
 *
 * <p><b>{@code timetableEntryId}.</b> It is in the path, and it is what the whole write is aimed at.
 */
public record TimetableEntryPatchRequest(

        /**
         * The version of the day this correction was decided against.
         *
         * <p><b>Optional, unlike on #2</b>, and honoured when sent. A targeted write cannot lose
         * somebody else's edit to a <i>different</i> period, so requiring it would refuse two
         * clerks working on two sections at once — which is open item 1's whole complaint. Send it
         * when the correction was decided from a screen that might be stale, and the answer is
         * {@code 409 CONCURRENT_MODIFICATION} rather than a change applied over somebody else's.
         */
        Long version,

        /** The school's own label for the period. Cannot be blanked — a period needs one. */
        @Size(max = 40) String periodCode,

        /**
         * What is taught. {@code ""} clears it, which only a non-lesson may end up with.
         *
         * <p>It still has to be a subject that section studies — the class-wide rule #1 enforces
         * does not relax because this is an edit.
         */
        @Size(max = 40) String subjectCode,

        /**
         * Who takes it. <b>This is the substitution.</b> {@code ""} clears it, which a
         * {@code LESSON} may not end up without.
         */
        @Size(max = 60) String teacherDocsId,

        /** What a timetable prints for a non-lesson. {@code ""} clears it. */
        @Size(max = 120) String slotLabel,

        /** When it starts. Still strictly before {@code endTime} afterwards. */
        LocalTime startTime,

        /** When it ends. */
        LocalTime endTime,

        /** The room. {@code ""} clears it — how a practical moved out of the lab is recorded. */
        @Size(max = 60) String facilityResourceDocsId) {
}
