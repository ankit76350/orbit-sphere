package com.orbitastra.backend.models.people.development;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.people.development.enums.StaffDevelopmentStatus;
import com.orbitastra.backend.models.people.development.enums.StaffDevelopmentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Planned or completed professional development activity for one Staff member.
 */
@Document(collection = "staff_development_records")
@CompoundIndex(
        name = "school_staff_development_status_date_idx",
        def = "{'schoolId': 1, 'staffDocsId': 1, 'status': 1, 'completedOn': -1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDevelopmentRecord extends SchoolBase {

    // Links to Staff.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Example: StaffDevelopmentType.TRAINING
    @NotNull
    private StaffDevelopmentType developmentType;

    // Example: "Inclusive Classroom Strategies"
    @NotBlank
    private String title;

    // Example: "National Institute of Education"
    private String provider;

    // Example: 2026-10-10
    private LocalDate plannedOn;

    // Example: 2026-10-10
    private LocalDate completedOn;

    // Example: 8.0
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal hours;

    // Example: 2500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal cost;

    // Example: "INR"
    private String currencyCode;

    // Example: StaffDevelopmentStatus.COMPLETED
    @NotNull
    @Builder.Default
    private StaffDevelopmentStatus status = StaffDevelopmentStatus.PLANNED;

    // Links to the approving Staff or identity account.
    private String approvedByDocsId;

    // Example: "Teacher applied the strategy successfully in Grade 6."
    private String impactEvaluation;

    // Links to the completion certificate document.
    private String certificateDocumentDocsId;

    // Example: ["INCLUSIVE_EDUCATION", "CLASSROOM_MANAGEMENT"]
    @Builder.Default
    private List<String> skillCodes = new ArrayList<>();
}
