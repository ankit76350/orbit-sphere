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
    // NOTHING MAY FALL AFTER THE ACADEMIC YEAR ENDS, and that is the whole of the rule —
    // 2026-09-28. #1, #2 and #3 all refuse such a date with 400 CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR.
    // Until 2026-09-25 a cycle for 2026-2027 could carry an enrolment deadline in 2099, and one
    // was in the database.
    //
    // THERE IS NO LOWER BOUND, AND ALL FOUR MAY FALL BEFORE THE YEAR STARTS. A school runs a whole
    // admissions round in the months running up to the year it admits for: enquiries open,
    // applications open, applications close and the deadline passes, all before the first day of
    // school. The rule was written on 2026-09-25 requiring every date inside the year, relaxed on
    // 2026-09-28 for the opening two, and relaxed again the same day for all four — each time
    // because it had refused an ordinary calendar.
    //
    // WHAT THAT COSTS: a cycle for 2026-2027 with all four dates in 2019 is now accepted. Nothing
    // here can tell that from a school working a long way ahead. A floor — nothing more than a
    // year before the year starts — is what would catch it, and no caller has asked for one.

    // OPENING THE ROUND OVERWRITES THE TWO OPENING DATES — changed 2026-09-28. #3 sets both to
    // now when a cycle moves to OPEN, whatever the school published, because opening a round is
    // the school saying it is taking applications from today. It is the one move that overwrites;
    // every other one fills an absent date and leaves a set one alone. The cost is that a round
    // whose enquiries genuinely opened in August loses that date — these fields hold one fact
    // each, and this makes it the actual rather than the plan. An actualOpenedAt would let both
    // be true.
    //
    // AND OPENING RE-CHECKS ALL FOUR, AND REFUSES. #3 is the only move that can be turned down
    // over its dates: #17 lets applications into an OPEN cycle, so it is the last moment anybody
    // looks at the calendar before families depend on it. The other moves record what they can
    // and carry on, so a round that has gone wrong can always still be CANCELLED.
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
