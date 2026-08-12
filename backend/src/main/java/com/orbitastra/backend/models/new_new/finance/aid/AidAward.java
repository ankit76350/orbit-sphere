package com.orbitastra.backend.models.new_new.finance.aid;

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

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.AidAwardStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Help that has actually been granted to one student, and how much of it has been
 * used.
 *
 * <p>This is the record invoice lines point at when a scholarship reduces a
 * charge. {@code awardAmount} is what was promised and {@code utilizedAmount} is
 * what has been taken so far, so the school can see at any time how much of a
 * grant is still to be spent.
 *
 * <p>An award covers a date range rather than a whole year, because help can start
 * mid-year or be stopped early. Only an ACTIVE award inside its dates may reduce
 * a new invoice.
 *
 * <p>Suspending an award never rewrites bills that already used it. The invoices
 * already issued keep their discount, and only invoices made after the suspension
 * stop getting it.
 */
@Document(collection = "aid_awards")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_aid_award_no_uniq",
                def = "{'schoolId': 1, 'awardNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_aid_application_award_uniq",
                def = "{'schoolId': 1, 'aidApplicationDocsId': 1}",
                unique = true,
                partialFilter = "{'aidApplicationDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_year_student_award_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'status': 1, 'validUntil': 1}"),
        @CompoundIndex(
                name = "school_programme_award_status_idx",
                def = "{'schoolId': 1, 'aidProgrammeDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidAward extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type AID_AWARD.
    // Example: "AWD/2026/000019"
    @NotBlank
    private String awardNo;

    // Links to AidApplication.id. Null when the school granted help without an
    // application, such as an automatic staff-child grant.
    // Example: "67b03344dc3f7d0033445566"
    private String aidApplicationDocsId;

    // Links to AidProgramme.id the money comes out of.
    // Example: "67b02233dc3f7d0022334455"
    @NotBlank
    private String aidProgrammeDocsId;

    // Help promised for this period. Example: 40000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal awardAmount;

    // Help already taken through invoice lines. Example: 20000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Fee heads this award may cover. Empty means every head the programme
    // allows.
    @Builder.Default
    private List<String> coveredFeeHeadDocsIds = new ArrayList<>();

    // First date an invoice may use this award. Example: 2026-04-01
    @NotNull
    private LocalDate validFrom;

    // Last date an invoice may use this award. Example: 2027-03-31
    @NotNull
    private LocalDate validUntil;

    // Example: AidAwardStatus.ACTIVE
    @NotNull
    @Builder.Default
    private AidAwardStatus status = AidAwardStatus.ACTIVE;

    // What the student has to keep doing to hold on to the help.
    // Example: "Keep 85 percent attendance and pass every subject."
    private String renewalCondition;

    // Links to the staff identity that sanctioned the award.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String sanctionedByDocsId;

    // Example: 2026-03-28T10:20:00Z
    private Instant sanctionedAt;

    // Links to DocumentRecord.id for the letter sent to the family.
    // Example: "67ad3344dc3f7d0055667788"
    private String sanctionLetterDocsId;

    // Why the award was paused or taken back.
    // Example: "Attendance fell below the agreed level in the second term."
    private String statusReason;

    // Example: 2026-11-10T06:00:00Z
    private Instant statusChangedAt;

    // Links to the award that carried on from this one in the next year.
    // Example: "67b04455dc3f7d0044556677"
    private String renewedAsAwardDocsId;
}
