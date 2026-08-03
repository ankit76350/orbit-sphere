package com.orbitastra.backend.models.new_new.academics.timetable;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.SubstitutionStatus;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A dated teacher replacement for one recurring TimetableEntry. */
@Document(collection = "teacher_substitutions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_timetable_date_substitution_uniq",
                def = "{'schoolId': 1, 'timetableEntryDocsId': 1, 'occurrenceDate': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_substitute_date_status_idx",
                def = "{'schoolId': 1, 'substituteStaffDocsId': 1, 'occurrenceDate': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_absent_staff_date_idx",
                def = "{'schoolId': 1, 'absentStaffDocsId': 1, 'occurrenceDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSubstitution extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to TimetableEntry.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String timetableEntryDocsId;

    // Date of the affected lesson. Example: 2026-08-14
    @NotNull
    private LocalDate occurrenceDate;

    // Links to the unavailable Staff.id.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String absentStaffDocsId;

    // Links to the replacement Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String substituteStaffDocsId;

    // Example: SubstitutionStatus.CONFIRMED
    @NotNull
    @Builder.Default
    private SubstitutionStatus status = SubstitutionStatus.DRAFT;

    // Example: "Teacher is on approved leave"
    private String reason;

    // Example: 2026-08-13T14:30:00Z
    private Instant notifiedAt;

    // Example: 2026-08-13T15:00:00Z
    private Instant acknowledgedAt;
}
