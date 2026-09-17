package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;

/**
 * One period of a day that is being <b>read</b>, with the names behind its ids.
 *
 * <h2>Why this is not {@link TimetableEntryResponse}</h2>
 *
 * <p>That one echoes what a caller just sent: #1 hands back the periods of a single written date,
 * and the caller already holds every name in them. This one answers #7, where the caller holds
 * nothing but a date — and a period that says
 * {@code classDocsId: "68f2…", teacherDocsId: "68a7…"} is unreadable to the person the screen is
 * for.
 *
 * <p><b>The alternative was one request per id.</b> A day of four hundred periods across twelve
 * classes and sixty staff would be seventy-odd follow-up reads to render one screen; resolving
 * them here is three queries. That is the same call {@code PositionDetailResponse} makes with
 * {@code departmentName}.
 *
 * <h2>What is not resolved, and why</h2>
 *
 * <p><b>{@code facilityResourceDocsId} stays an id.</b> #1 does not check that a room exists when
 * it writes one, so a stored id here may name nothing — and a read that quietly showed a blank
 * name would hide that. Open item 3 of this module's plan is about who owns the room, and the name
 * belongs with it.
 */
public record TimetableEntryDetailResponse(

        String timetableEntryId,
        String periodCode,

        String classDocsId,

        /** What the school calls that class — "Grade 10", "Nursery". */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,
        TimetableSlotType slotType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectCode,

        /**
         * The subject's name as the class carries it. Absent when the code no longer matches an
         * active subject of that section — a subject retired after the day was written.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherDocsId,

        /** Absent when that staff member has since been deleted. The id still answers for itself. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String slotLabel,

        LocalTime startTime,
        LocalTime endTime,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String facilityResourceDocsId) {

    /**
     * One stored period plus the three names its ids stand for.
     *
     * <p><b>Every name is nullable and none of them is load-bearing.</b> A class deleted, a subject
     * retired or a staff member removed after the day was written leaves the id intact and the name
     * absent — which is the truth, and better than a read that fails because something the period
     * points at has moved on.
     */
    public static TimetableEntryDetailResponse of(TimetableEntry entry, String className,
            String subjectName, String teacherName) {

        return new TimetableEntryDetailResponse(
                entry.getId(),
                entry.getPeriodCode(),
                entry.getClassDocsId(),
                className,
                entry.getSectionNo(),
                entry.getSlotType(),
                entry.getSubjectCode(),
                subjectName,
                entry.getTeacherDocsId(),
                teacherName,
                entry.getSlotLabel(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getFacilityResourceDocsId());
    }
}
