package com.orbitastra.backend.models.new_new.finance.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.config.embedded.FeeInstallment;
import com.orbitastra.backend.models.new_new.finance.config.embedded.FeeStructureLine;
import com.orbitastra.backend.models.new_new.finance.enums.FeeStructureStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The full set of charges one class pays in one academic year, plus the dates
 * those charges fall due.
 *
 * <p>A structure is the template and a FeeInvoice is what comes out of it.
 * Applying an ACTIVE structure to the students of a class creates one invoice
 * per student per installment.
 *
 * <p>Structures are versioned, not edited. Once a version has produced invoices
 * its lines must stay as they are, because those invoices are financial records.
 * A mid-year fee change means creating {@code version + 1}, marking the old
 * version SUPERSEDED, and leaving already-issued invoices alone.
 *
 * <p>{@code academicYear} stores the immutable AcademicYear name, never the
 * AcademicYear document id. {@code classDocsId} being null means the structure
 * applies to every class in the year, which is how a school charges one common
 * set of fees.
 *
 * <p>The service and request DTOs check that installment shares add up to 100,
 * that due dates fall inside the academic year, that a line's head is active,
 * and that only one version of a structure is ACTIVE at a time.
 */
@Document(collection = "fee_structures")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_structure_version_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'structureCode': 1, 'structureVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_class_structure_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_structure_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'effectiveFrom': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure extends SchoolBase {

    // Links to AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable key for this structure across its versions. Example: "PRIMARY_DAY"
    @NotBlank
    private String structureCode;

    // Version of the structure, starting at 1. Example: 1
    @NotNull
    @Builder.Default
    private Integer structureVersion = 1;

    // Name shown to staff. Example: "Primary Day Scholar Fees"
    @NotBlank
    private String name;

    // Links to SchoolClass.id. Null means every class in the year.
    // Example: "67ab3322dc3f7d0044556677"
    private String classDocsId;

    // Example: FeeStructureStatus.ACTIVE
    @NotNull
    @Builder.Default
    private FeeStructureStatus status = FeeStructureStatus.DRAFT;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // First date invoices may be created from this version. Example: 2026-04-01
    private LocalDate effectiveFrom;

    // Last date invoices may be created from this version. Example: 2027-03-31
    private LocalDate effectiveUntil;

    // Yearly total of all lines, worked out and saved so lists load fast.
    // Example: 48000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal annualTotal;

    // What is charged.
    @Valid
    @Builder.Default
    private List<FeeStructureLine> lines = new ArrayList<>();

    // When each part of it has to be paid.
    @Valid
    @Builder.Default
    private List<FeeInstallment> installments = new ArrayList<>();

    // Links to the staff identity that approved this version.
    // Example: "67aa15d9dc3f7d0044444444"
    private String approvedByDocsId;

    // Example: 2026-03-20T06:45:00Z
    private Instant approvedAt;

    // Version that replaced this one. Example: "67ac4455dc3f7d0088990011"
    private String supersededByStructureDocsId;

    // Why the fees changed, kept for parent questions later.
    // Example: "Board affiliation charge added from the second term."
    private String changeReason;
}
