package com.orbitastra.backend.models.core;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One academic year of one school, e.g. "2026-2027" running 2026-04-01 to
 * 2027-03-31. Owns the school's holiday calendar (dated holidays and weekly
 * offs) for that year. Other collections (student academic records, classes,
 * timetables, ...) reference this year by its "name" (unique per school and
 * human readable) in their "academicYear" field — not by the document id.
 * Renaming a year therefore orphans existing references, so the name is
 * immutable after creation.
 */
@Document(collection = "academic_years")
@CompoundIndex(name = "school_year_name_idx", def = "{'schoolId': 1, 'name': 1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends SchoolBase {

    // e.g. "2026-2027" — unique per school; this is what other collections
    // store in their "academicYear" field
    @Setter(AccessLevel.NONE)
    private String name;

    // First day of the academic year
    private LocalDate startDate;

    // Last day of the academic year
    private LocalDate endDate;

    private List<HolidayDetail> holidays;
}
