package com.orbitastra.backend.models.new_new.academics.timetable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One recurring weekly timetable slot for one class section.
 *
 * <p>Keeping each slot as its own document avoids one school-wide daily
 * document becoming a write-contention and document-growth hotspot.
 */
@Document(collection = "timetable_entries")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_section_day_start_active_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionCode': 1, 'dayOfWeek': 1, 'startTime': 1}",
                unique = true,
                partialFilter = "{'active': true}"),
        @CompoundIndex(
                name = "school_year_teacher_day_time_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'teacherDocsIds': 1, 'dayOfWeek': 1, 'startTime': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_year_class_section_day_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionCode': 1, 'dayOfWeek': 1, 'startTime': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntry extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // References an embedded SchoolClass.sections[].sectionCode. Example: "A"
    @NotBlank
    private String sectionCode;

    // Example: TimetableSlotType.LESSON
    @NotNull
    private TimetableSlotType slotType;

    // Required for LESSON; references SchoolClass.subjects[].subjectCode.
    // Example: "MATHEMATICS"
    private String subjectCode;

    // Required for LESSON; links to assigned Staff.id values.
    // Example: ["67aa15d9dc3f7d0044444444"]
    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();

    // Label used for a break, assembly, or activity. Example: "Lunch Break"
    private String slotLabel;

    // Example: DayOfWeek.MONDAY
    @NotNull
    private DayOfWeek dayOfWeek;

    // Example: 09:00:00
    @NotNull
    private LocalTime startTime;

    // Example: 09:45:00
    @NotNull
    private LocalTime endTime;

    // Optionally links to a future facility/resource document.
    // Example: "67aa15d9dc3f7d0055555555"
    private String roomResourceDocsId;

    // First date on which the entry applies. Example: 2026-04-01
    @NotNull
    private LocalDate effectiveFrom;

    // Last date on which the entry applies; null means through the year.
    // Example: 2027-03-31
    private LocalDate effectiveUntil;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
