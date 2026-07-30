package com.orbitastra.backend.models.new_new.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionCycleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private String academicYear;

    // Example: "Admissions 2026-2027"
    private String name;

    // Example: 2026-01-01T00:00:00Z
    private Instant inquiryOpenAt;

    // Example: 2026-02-01T00:00:00Z
    private Instant applicationOpenAt;

    // Example: 2026-05-31T23:59:59Z
    private Instant applicationCloseAt;

    // Example: 2026-06-30T23:59:59Z
    private Instant enrollmentDeadlineAt;

    // Example: AdmissionCycleStatus.OPEN
    @Builder.Default
    private AdmissionCycleStatus status = AdmissionCycleStatus.DRAFT;

    // Example: [{ "classDocsId": "67aa...", "totalSeats": 60, "reservedSeats": 10 }]
    @Builder.Default
    private List<IntakeCapacity> capacities = new ArrayList<>();

    // Example: "Admission is open for Grades 1 to 10."
    private String notes;
}
