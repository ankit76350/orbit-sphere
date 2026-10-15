package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #11 answers: every period booked in one room on one date, earliest first.
 *
 * <p>The question it answers is "what is the lab doing today", asked before a room booking is
 * approved.
 */
public record RoomDayResponse(

        LocalDate date,

        String academicYear,

        String dailyTimetableDocsId,

        /** The room asked about, echoed back. */
        String facilityResourceDocsId,

        /** How many periods use the room that day. */
        int entryCount,

        /** When the room is first in use. Absent when it is not used at all. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalTime firstStartTime,

        /** When the room is last in use. Absent when it is not used at all. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalTime lastEndTime,

        /** The periods, earliest first, with class, section, subject and teacher named. */
        List<TimetableEntryDetailResponse> entries,

        String nextStep) {
}
