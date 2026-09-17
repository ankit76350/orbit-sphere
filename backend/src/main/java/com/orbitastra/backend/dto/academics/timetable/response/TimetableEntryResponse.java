package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;

/**
 * One period, as it is stored.
 *
 * <p><b>{@code timetableEntryId} is the field everything else needs.</b> It is what
 * {@link com.orbitastra.backend.models.academics.attendance.AttendanceSession} stores, and what
 * every targeted update addresses an entry by — so a caller that intends to correct a period later
 * has to keep it.
 */
public record TimetableEntryResponse(

        String timetableEntryId,
        String periodCode,
        String classDocsId,
        String sectionNo,
        TimetableSlotType slotType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String slotLabel,

        LocalTime startTime,
        LocalTime endTime,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String facilityResourceDocsId) {

    public static TimetableEntryResponse of(TimetableEntry entry) {
        return new TimetableEntryResponse(
                entry.getId(),
                entry.getPeriodCode(),
                entry.getClassDocsId(),
                entry.getSectionNo(),
                entry.getSlotType(),
                entry.getSubjectCode(),
                entry.getTeacherDocsId(),
                entry.getSlotLabel(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getFacilityResourceDocsId());
    }
}
