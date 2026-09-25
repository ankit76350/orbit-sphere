package com.orbitastra.backend.models.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Defines one configurable admission window for one school and academic year.
 *
 * <p>Applications link to this document through
 * {@code AdmissionApplication.admissionCycleDocsId}. Seat limits are embedded
 * as {@link IntakeCapacity} values because they exist only within this cycle.
 * The cycle does not contain application ids, preventing an unbounded array as
 * applications grow.
 *
 * <p>{@code academicYear} stores {@code AcademicYear.name}, not its document id.
 *
 * <p>There is no form definition on a cycle. It had an
 * {@code applicationFormDefinitionDocsId}, removed 2026-09-21, because no form
 * definition model was ever built for it to point at. Put it back with the form
 * builder, not before.
 */
@Document(collection = "admission_cycles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_academic_year_cycle_name_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_cycle_status_dates_idx",
                def = "{'schoolId': 1, 'status': 1, 'applicationOpenAt': 1, 'applicationCloseAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionCycle extends SchoolBase {

    // Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Example: "Admissions 2026-2027"
    @NotBlank
    private String name;

    // The school's published calendar. All four are required from 2026-09-22: a round with no
    // dates is one nobody can be told about, and #17 checks the application window before it takes
    // a form.
    //
    // ALL FOUR MUST FALL INSIDE THE ACADEMIC YEAR from 2026-09-25 — #1 and #2 both refuse a date
    // outside it with 400 CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR. Until then a cycle for 2026-2027 could
    // carry an enrolment deadline in 2099 or an inquiry date in 2019, and both were in the
    // database.
    //
    // THE EXAMPLES BELOW NO LONGER PASS THAT RULE, and they are left as they are on purpose. A
    // school normally admits for a year BEFORE it begins — these open enquiries on 1 January for a
    // year starting in April — which is the ordinary admissions calendar and is now refused. The
    // rule was asked for as "all the dates must belong in the academic year range"; relaxing it to
    // "nothing after the year ends" would allow this calendar and still catch the 2099 deadline.
    //
    // @NotNull here is a CONTRACT, not a guard. This project registers no
    // ValidatingMongoEventListener, so nothing enforces it on save — the enforcement is @NotNull
    // on AdmissionCycleCreateRequest, which is what a caller actually goes through. Cycles created
    // before this rule may still have nulls, which is why #3 fills them in and #17 checks for them.

    // Example: 2026-01-01T00:00:00Z
    @NotNull
    private Instant inquiryOpenAt;

    // Example: 2026-02-01T00:00:00Z
    @NotNull
    private Instant applicationOpenAt;

    // Example: 2026-05-31T23:59:59Z
    @NotNull
    private Instant applicationCloseAt;

    // Example: 2026-06-30T23:59:59Z
    @NotNull
    private Instant enrollmentDeadlineAt;

    // Example: AdmissionCycleStatus.OPEN
    @NotNull
    @Builder.Default
    private AdmissionCycleStatus status = AdmissionCycleStatus.DRAFT;

    // Example: [{ "classDocsId": "67aa...", "totalSeats": 60, "reservedSeats": 10 }]
    @Builder.Default
    private List<IntakeCapacity> capacities = new ArrayList<>();

    // Example: "Admission is open for Grades 1 to 10."
    private String notes;
}
