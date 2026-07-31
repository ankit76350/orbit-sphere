package com.orbitastra.backend.models.new_new.people.performance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.performance.embedded.PerformanceCriterion;
import com.orbitastra.backend.models.new_new.people.performance.enums.PerformanceCycleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * School-defined staff performance-review cycle for one academic year.
 */
@Document(collection = "staff_performance_cycles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_performance_year_cycle_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'cycleCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_performance_status_dates_idx",
                def = "{'schoolId': 1, 'status': 1, 'startDate': 1, 'endDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceCycle extends SchoolBase {

    // Stores AcademicYear.name, never its document id. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable code within the academic year. Example: "ANNUAL_REVIEW"
    @NotBlank
    private String cycleCode;

    // Example: "Annual Staff Performance Review"
    @NotBlank
    private String name;

    // Example: 2027-01-01
    @NotNull
    private LocalDate startDate;

    // Example: 2027-03-15
    @NotNull
    private LocalDate endDate;

    // Example: PerformanceCycleStatus.OPEN
    @NotNull
    @Builder.Default
    private PerformanceCycleStatus status = PerformanceCycleStatus.DRAFT;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean anonymousPeerFeedback = false;

    // Embedded scoring configuration.
    @Builder.Default
    private List<PerformanceCriterion> criteria = new ArrayList<>();
}
