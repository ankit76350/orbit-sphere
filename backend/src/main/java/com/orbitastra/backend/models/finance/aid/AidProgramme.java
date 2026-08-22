package com.orbitastra.backend.models.new_new.finance.aid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.AidType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A scholarship or financial-help scheme the school runs for one academic year.
 *
 * <p>A programme differs from a ConcessionPolicy in one important way: it has a
 * fund behind it. {@code budgetAmount} and {@code awardedAmount} are what stop the
 * school from promising more help than it set aside, which a plain discount rule
 * cannot do.
 *
 * <p>Families apply through AidApplication, and an approved application produces
 * an AidAward. The award is what actually reduces an invoice line, so the money
 * granted can always be traced back to the fund it came out of.
 *
 * <p>{@code eligibleFeeHeadDocsIds} being empty means the help may cover any
 * head. When the list has entries, only those heads may be covered, so a tuition
 * scholarship does not also pay for the bus.
 */
@Document(collection = "aid_programmes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_aid_programme_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'programmeCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_aid_type_active_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'aidType': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidProgramme extends SchoolBase {

    // Links to AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable key used by applications and awards. Example: "MERIT_TOPPERS"
    @NotBlank
    private String programmeCode;

    // Example: "Merit Scholarship for Class Toppers"
    @NotBlank
    private String name;

    // Example: "Covers tuition for the top three students of each class."
    private String description;

    // Example: AidType.MERIT
    @NotNull
    private AidType aidType;

    // Where the money comes from, such as a trust or a named donor.
    // Example: "Orbitastra Alumni Trust"
    private String fundingSourceName;

    // Money set aside for the whole programme this year. Example: 1500000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal budgetAmount;

    // Money already promised through awards, added to as awards are made.
    // Example: 875000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal awardedAmount = BigDecimal.ZERO;

    // Money already used against invoices. Example: 320000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    // Most one student may be given under this programme. Example: 45000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumAwardAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Fee heads this help may cover. Empty means any head.
    @Builder.Default
    private List<String> eligibleFeeHeadDocsIds = new ArrayList<>();

    // Written rule the committee checks.
    // Example: "Above 90 percent in the previous year and no fee default."
    private String eligibilityCriteria;

    // Proof a family has to send with the application.
    // Example: "Previous year mark sheet and an income certificate."
    private String requiredEvidence;

    // Example: 2026-03-01
    private LocalDate applicationOpenDate;

    // Example: 2026-04-15
    private LocalDate applicationCloseDate;

    // Most students who may be helped this year. Example: 30
    private Integer maximumAwardCount;

    // Awards already made this year. Example: 21
    @NotNull
    @Builder.Default
    private Integer awardCount = 0;

    // Whether an award may carry into the next year. Example: true
    @NotNull
    @Builder.Default
    private Boolean renewable = false;

    // Whether new applications may still be taken. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
