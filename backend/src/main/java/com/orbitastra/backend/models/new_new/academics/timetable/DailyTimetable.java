package com.orbitastra.backend.models.new_new.academics.timetable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.timetable.embedded.TimetableEntry;
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
 * Complete timetable of one school for one calendar date.
 *
 * <p>The {@code entries} list contains the periods of every class and section
 * for that day. A school has at most one document for a date. No document needs
 * to be created for a holiday or weekly off.
 *
 * <p>A teacher replacement is applied directly to that date's embedded entry.
 * The inherited optimistic-lock version prevents concurrent edits from
 * silently overwriting one another.
 */
@Document(collection = "daily_timetables")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_timetable_date_uniq",
                def = "{'schoolId': 1, 'date': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_timetable_date_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'date': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTimetable extends SchoolBase {

    // Stores AcademicYear.name and is derived from date by the service.
    // Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // The date represented by this complete timetable. Example: 2026-08-05
    @NotNull
    private LocalDate date;

    // Every class and section period scheduled for this date.
    @NotNull
    @Builder.Default
    private List<TimetableEntry> entries = new ArrayList<>();
}
