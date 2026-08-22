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
 * The list of fees one class pays in one year, and the dates each part has to be
 * paid by.
 *
 * <p>This is only a plan. Using the plan is what makes the real bills: for every
 * student in the class, one FeeInvoice for each installment.
 *
 * <p>A plan that has already made bills must never be changed. Parents were given
 * those bills, so the plan behind them has to stay exactly as it was. To change
 * fees in the middle of the year, save a new copy with the next version number,
 * mark the old copy SUPERSEDED, and leave the bills already sent alone.
 *
 * <p>Think of a school textbook. The title "Maths Class 5" stays the same, and
 * only the edition number changes when it is printed again. Here
 * {@code structureCode} is the title and {@code structureVersion} is the edition.
 *
 * <p>Before saving, the service makes sure the installment shares add up to 100,
 * the due dates fall inside the school year, every fee head used is still active,
 * and only one version of a plan is ACTIVE at a time.
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

    // The school year this plan is for. Holds the AcademicYear name, not its id.
    // Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // The short name you give this fee plan. It stays the same in every version,
    // like a book title that does not change when a new edition is printed. It is
    // what tells the system that version 1 and version 2 are the same plan.
    // The class cannot be used for this, because one class can have two plans,
    // such as one for day scholars and one for hostel students.
    // Example: "PRIMARY_DAY"
    @NotBlank
    private String structureCode;

    // Edition number of this plan, starting at 1. It goes up by one every time
    // the fees change during the year. Example: 1
    @NotNull
    @Builder.Default
    private Integer structureVersion = 1;

    // Full name staff see on screen. This one may be reworded any time.
    // Example: "Primary Day Scholar Fees"
    @NotBlank
    private String name;

    // The class this plan is for. Links to SchoolClass.id. Leave it empty when
    // the plan is for every class in the year.
    // Example: "67ab3322dc3f7d0044556677"
    private String classDocsId;

    // Only an ACTIVE plan may be used to make bills.
    // Example: FeeStructureStatus.ACTIVE
    @NotNull
    @Builder.Default
    private FeeStructureStatus status = FeeStructureStatus.DRAFT;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // First date bills may be made from this version. Example: 2026-04-01
    private LocalDate effectiveFrom;

    // Last date bills may be made from this version. Example: 2027-03-31
    private LocalDate effectiveUntil;

    // Everything in the lines added up for the year. It is saved here so lists
    // load fast and do not have to add it up every time. Example: 48000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal annualTotal;

    // What is charged.
    @Valid
    @Builder.Default
    private List<FeeStructureLine> lines = new ArrayList<>();

    // When each part has to be paid.
    @Valid
    @Builder.Default
    private List<FeeInstallment> installments = new ArrayList<>();

    // Who approved this version. Links to the staff identity.
    // Example: "67aa15d9dc3f7d0044444444"
    private String approvedByDocsId;

    // Example: 2026-03-20T06:45:00Z
    private Instant approvedAt;

    // The newer version that took over from this one.
    // Example: "67ac4455dc3f7d0088990011"
    private String supersededByStructureDocsId;

    // Why the fees changed. Kept so the school can answer parents later.
    // Example: "Board affiliation charge added from the second term."
    private String changeReason;
}
