package com.orbitastra.backend.models.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One academic year of one school, e.g. "2026-2027" running 2026-04-01 to
 * 2027-03-31. Owns the school's holiday calendar (dated holidays and weekly
 * offs) for that year. Other collections (student academic records, classes,
 * timetables, ...) reference this year by its "name" (unique per school and
 * human readable) in their "academicYearId" field — not by the document id.
 * Renaming a year therefore orphans existing references; avoid renames once
 * a year is in use.
 */
@Document(collection = "academic_years")
@CompoundIndex(name = "school_year_name_idx", def = "{'schoolId': 1, 'name': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    // e.g. "2026-2027" — unique per school; this is what other collections
    // store in their "academicYearId" field
    private String name;

    // First day of the academic year
    private LocalDate startDate;

    // Last day of the academic year
    private LocalDate endDate;

    private List<HolidayDetail> holidays;
}
