package com.orbitastra.backend.models.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.crm.embedded.AdmissionFormQuestion;
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
 * <p>The extra questions this round asks are embedded in {@code questions}, the same way seat
 * limits are. A cycle used to point at a form definition document through
 * {@code applicationFormDefinitionDocsId}; that was removed on 2026-09-21 because the document
 * it pointed at was never built. The questions are kept here instead, because they belong to
 * one round and nothing outside that round reads them.
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

        // Published application window. Both dates are required from 2026-09-22.
        // Applications are accepted only within this window.

        // Dates cannot be after the academic year ends.
        // Dates may be before the academic year starts.

        // OPEN sets applicationOpenAt to now; CLOSED sets applicationCloseAt to now.
        // Other status changes do not modify these dates.

        // Reopening CLOSED → OPEN resets applicationOpenAt to now.
        // The close date must be moved forward if it has already passed.

        // @NotNull is enforced through AdmissionCycleCreateRequest, not MongoDB.
        // Existing cycles may still contain null dates, so #17 handles them.
    // Example: 2026-02-01T00:00:00Z
    @NotNull
    private Instant applicationOpenAt;

    // Example: 2026-05-31T23:59:59Z
    @NotNull
    private Instant applicationCloseAt;

    // Example: AdmissionCycleStatus.OPEN
    @NotNull
    @Builder.Default
    private AdmissionCycleStatus status = AdmissionCycleStatus.DRAFT;

    // Example: [{ "classDocsId": "67aa...", "totalSeats": 60, "reservedSeats": 10 }]
    @Builder.Default
    private List<IntakeCapacity> capacities = new ArrayList<>();

    // The extra questions this round asks, on top of the fixed fields on the form.
    // The family's answers are saved in AdmissionApplication.formAnswers, keyed by the QUESTION
    // ITSELF -- the wording, not the id (changed 2026-10-01, so the answers can be read without
    // the cycle open beside them). Each question still carries an id, which is how #2 rewords one
    // in place; the service has to make those ids, because only the document's own is filled in
    // for us and these sit inside a list.
    //
    // A round may ask nothing extra, so an empty list is normal.
    // The order of the list is the order the questions are asked in.
    // Example: [{ "id": "67aa15d9dc3f7d0011111111", "question": "Which school did the child go
    // to before?", "required": true }]
    // and the answer to it is saved as { "Which school did the child go to before?": "ABC School" }
    @Builder.Default
    private List<AdmissionFormQuestion> questions = new ArrayList<>();

    // Example: "Admission is open for Grades 1 to 10."
    private String notes;
}
