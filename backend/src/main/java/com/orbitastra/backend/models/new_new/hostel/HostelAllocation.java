package com.orbitastra.backend.models.new_new.hostel;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.hostel.enums.HostelAllocationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One child's bed for one academic year, and what the family pays for it.
 *
 * <p>This is the record that connects a child to a bed, and it is also what causes them
 * to be charged. It follows exactly the shape TransportAllocation set: the amounts are
 * copied on at allocation time and never rewritten, and a fee head says where the charge
 * lands.
 *
 * <p>Three separate charges live here because a boarding school raises three:
 *
 * <ul>
 * <li>{@code monthlyHostelFee} for the bed, billed under FeeCategory.HOSTEL.</li>
 * <li>{@code monthlyMessFee} for the food, billed under FeeCategory.MESS. Mess charges
 * ride on this record rather than on a separate subscription because at a boarding
 * school everybody who has a bed eats.</li>
 * <li>{@code securityDepositAmount} taken once, billed under FeeCategory.DEPOSIT, and
 * given back at the end less anything owed. It is money held rather than money earned,
 * which is why it has its own category and its own refund date.</li>
 * </ul>
 *
 * <p>A student may hold only one ACTIVE allocation per year, which the unique index
 * enforces. Moving room means ending one and starting another, so the change has a date
 * on it and the old arrangement is still readable.
 *
 * <p>A child away for a weekend stays ACTIVE. Going home for two nights is a leave
 * request, not a change of residence, and treating it as one would take them off the
 * roll call they most need to be on.
 *
 * <p>{@code guardianConsentDocumentDocsId} is the family agreeing their child lives at
 * school. A boarding school without that on file is exposed in a way no other module is.
 *
 * <p>The service checks that the bed is AVAILABLE and in a building whose HostelType
 * suits the child, that the bed is set to OCCUPIED in the same step, that the fee heads
 * carry the right categories, and that the dates sit inside the academic year.
 */
@Document(collection = "hostel_allocations")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_hostel_allocation_no_uniq",
                def = "{'schoolId': 1, 'allocationNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_hostel_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_hostel_bed_active_uniq",
                def = "{'schoolId': 1, 'hostelBedDocsId': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_year_building_allocation_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'hostelBuildingDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_hostel_billing_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'checkInDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelAllocation extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type HOSTEL_ALLOCATION.
    // Example: "HA/2026/000214"
    @NotBlank
    private String allocationNo;

    // Links to HostelBed.id. The exact bed, so a roll call can say where a child
    // sleeps. Example: "67ba1124dc3f7d0033445566"
    @NotBlank
    private String hostelBedDocsId;

    // Links to HostelRoom.id, copied in so a room list reads without loading beds.
    // Example: "67ba1123dc3f7d0022334455"
    @NotBlank
    private String hostelRoomDocsId;

    // Links to HostelBuilding.id, copied in for the same reason.
    // Example: "67ba1122dc3f7d0011223344"
    @NotBlank
    private String hostelBuildingDocsId;

    // What the family pays each month for the bed. Copied when the allocation was
    // made and never rewritten when the price list changes. Example: 6000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyHostelFee;

    // What the family pays each month for food, on the same terms.
    // Example: 4500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyMessFee;

    // Taken once and given back at the end, less anything owed. Money held, not
    // money earned. Example: 15000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal securityDepositAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Links to FeeHead.id for the bed charge, normally FeeCategory.HOSTEL.
    // Example: "67ac1188dc3f7d0011aa22bb"
    @NotBlank
    private String hostelFeeHeadDocsId;

    // Links to FeeHead.id for the food charge, normally FeeCategory.MESS.
    // Example: "67ac1189dc3f7d0011aa22cc"
    private String messFeeHeadDocsId;

    // Links to FeeHead.id for the deposit, normally FeeCategory.DEPOSIT.
    // Example: "67ac118adc3f7d0011aa22dd"
    private String depositFeeHeadDocsId;

    // First day the child lives here. Example: 2026-04-05
    @NotNull
    private LocalDate checkInDate;

    // Last day, when it is known in advance. Example: 2027-03-25
    private LocalDate plannedCheckOutDate;

    // The day they actually moved out. Example: 2027-03-24
    private LocalDate actualCheckOutDate;

    // Example: HostelAllocationStatus.ACTIVE
    @NotNull
    @Builder.Default
    private HostelAllocationStatus status = HostelAllocationStatus.ACTIVE;

    // Why it was suspended or ended. Example: "Family moved to Pune."
    private String statusReason;

    // How much of the deposit was given back. Example: 14200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal depositRefundedAmount;

    // When it was given back. Example: 2027-04-10
    private LocalDate depositRefundedOn;

    // Why some of it was kept. Example: "Broken window latch, 800."
    private String depositDeductionReason;

    // Links to DocumentRecord.id for the family's signed agreement that their child
    // boards here. Example: "67ba1125dc3f7d0044556677"
    private String guardianConsentDocumentDocsId;

    // Links to the staff identity that approved the allocation.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: "Placed with his brother's room on the mother's request."
    private String remarks;
}
