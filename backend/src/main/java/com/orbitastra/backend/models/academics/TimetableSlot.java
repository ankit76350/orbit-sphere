package com.orbitastra.backend.models.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.SlotType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One recurring period of the weekly timetable, e.g. "Class 5 section A has
 * Mathematics with teacher X every MONDAY 09:00-09:45". The timetable for a
 * concrete date is computed from the slots whose dayOfWeek matches and whose
 * validity window (effectiveFrom..effectiveTo) contains the date. Storing the
 * weekly template instead of one document per date keeps the collection small
 * and lets a timetable run indefinitely without being re-created.
 */
@Document(collection = "timetable_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlot {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String classId;

    private String section;

    // LESSON needs subject + teacherId; BREAK has no teacher and the subject
    // is just a label like "Lunch Break". Breaks still occupy the section's
    // time, so nothing else can be scheduled over them.
    private SlotType type;

    private String subject;

    // Staff document id of the teacher (null for breaks)
    @Indexed
    private String teacherId;

    @Indexed
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    // The slot applies to dates within [effectiveFrom, effectiveTo].
    private LocalDate effectiveFrom;

    // null = ongoing, applies until the slot is ended or deleted
    private LocalDate effectiveTo;
}
