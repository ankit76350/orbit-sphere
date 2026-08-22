package com.orbitastra.backend.models.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One named academic year belonging to one school.
 *
 * <p>Other collections reference this document by the immutable {@code name}
 * stored in their {@code academicYear} field, never by AcademicYear.id.
 * Therefore, a created name must not be changed. Every lookup must combine the
 * inherited {@code schoolId} with the academic-year name.
 *
 * <p>The holiday calendar is embedded because its dates belong exclusively to
 * this academic year. Date ordering, overlap prevention, duplicate holidays,
 * and lock/unlock authorization are service and request-DTO rules.
 */
@Document(collection = "academic_years")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_dates_idx",
                def = "{'schoolId': 1, 'startDate': 1, 'endDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends SchoolBase {

    // Immutable reference used by child documents. Example: "2026-2027"
    @NotBlank
    @Setter(AccessLevel.NONE)
    private String name;

    // First school day boundary. Example: 2026-04-01
    @NotNull
    private LocalDate startDate;

    // Last school day boundary. Example: 2027-03-31
    @NotNull
    private LocalDate endDate;

    // Embedded dated holidays and weekly offs. Example: [{ "name": "Diwali", "date": "2026-11-08" }]
    @Builder.Default
    private List<HolidayDetail> holidays = new ArrayList<>();

    // Controls whether new enrollments may be assigned to this year. Example: true
    @NotNull
    @Builder.Default
    private Boolean enrollmentEnabled = false;

    // Prevents result changes after publication/finalization. Example: false
    @NotNull
    @Builder.Default
    private Boolean resultsLocked = false;
}
