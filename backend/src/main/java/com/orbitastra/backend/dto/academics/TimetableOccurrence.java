package com.orbitastra.backend.dto.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import com.orbitastra.backend.models.academics.enums.SlotType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One concrete lesson on one concrete date, computed from a weekly
 * {@link com.orbitastra.backend.models.academics.TimetableSlot}. This is what
 * schedule (date range) endpoints return.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableOccurrence {
    private LocalDate date;

    private DayOfWeek dayOfWeek;

    private String slotId;

    private String schoolId;

    private String classId;

    private String section;

    private SlotType type;

    private String subject;

    private String teacherId;

    private LocalTime startTime;

    private LocalTime endTime;
}
