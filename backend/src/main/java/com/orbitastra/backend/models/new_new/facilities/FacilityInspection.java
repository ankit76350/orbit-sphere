package com.orbitastra.backend.models.new_new.facilities;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.facilities.embedded.InspectionFinding;
import com.orbitastra.backend.models.new_new.facilities.enums.InspectionOutcome;
import com.orbitastra.backend.models.new_new.facilities.enums.InspectionType;
import com.orbitastra.backend.models.new_new.facilities.enums.MaintenanceTargetType;

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
 * One safety or condition check on one thing, on one day.
 *
 * <p>Fire safety, wiring, the water tanks, the lift, the climbing frame. Most of these are not
 * the school's own idea: an authority requires them, on a cycle, and hands over a certificate
 * that expires.
 *
 * <p>**{@code certificateValidUntil} must be checked on the day, not trusted to a status.**
 * This is the single most important rule here, and it is borrowed wholesale from
 * `transport/README.md`, where the same argument applies to a vehicle's insurance and fitness
 * papers. A certificate expiring is an event with no user action behind it — nobody is sitting
 * at a screen on the morning it lapses — so a service that reads a status field will find
 * everything looks fine right up until an inspector says otherwise.
 *
 * <p>{@code externalInspector} and {@code inspectorStaffDocsId} are both here and only one is
 * filled. A fire officer from the municipality is not in the staff collection and never will
 * be, and a hygiene walk-round by the school's own estate manager is. Forcing the outside
 * inspector to become a Staff row would put strangers in the payroll's collection.
 *
 * <p>PASSED_WITH_OBSERVATIONS is where most real rounds land, and the {@code findings} beside
 * it are what somebody is meant to act on. Each finding carries {@code workOrderDocsId}, and a
 * finding with none is the school not having acted yet — see InspectionFinding. **An inspection
 * that passes with observations and raises no work orders is a round nobody read.**
 *
 * <p>{@code targetType} and {@code targetDocsId} mean a round can be about a building, a bus, a
 * hostel room, a mess hall or one tagged asset. That is what lets `hostel`'s deferred "room and
 * mess inspection rounds" and `transport`'s vehicle fitness checks both land here without
 * either package changing.
 *
 * <p>There is no next-due date on this row. When an inspection recurs it is because a
 * MaintenancePlan says so, and that plan holds {@code nextDueDate}. Two places holding the
 * next date is two places to disagree, and the plan is the one an overdue query already reads.
 *
 * <p>The service checks that the target exists in the collection its type names, that FAILED
 * carries at least one finding, that a CRITICAL finding forces the target's status to
 * UNDER_MAINTENANCE or CLOSED, that a certificate outcome has a validity date, and that the
 * validity date is evaluated at the moment of use rather than cached anywhere.
 */
@Document(collection = "facility_inspections")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inspection_no_uniq",
                def = "{'schoolId': 1, 'inspectionNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inspection_target_idx",
                def = "{'schoolId': 1, 'targetType': 1, 'targetDocsId': 1, 'inspectedOn': -1}"),
        @CompoundIndex(
                name = "school_inspection_type_idx",
                def = "{'schoolId': 1, 'inspectionType': 1, 'inspectedOn': -1}"),
        @CompoundIndex(
                name = "school_inspection_certificate_expiry_idx",
                def = "{'schoolId': 1, 'certificateValidUntil': 1}",
                partialFilter = "{'certificateValidUntil': {'$type': 'date'}}"),
        @CompoundIndex(
                name = "school_inspection_outcome_idx",
                def = "{'schoolId': 1, 'outcome': 1, 'inspectedOn': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityInspection extends SchoolBase {

    // School-scoped number from NumberSequence type FACILITY_INSPECTION. Quoted on a
    // certificate and in correspondence with an authority. Example: "INSP/2026/000077"
    @NotBlank
    private String inspectionNo;

    // What was inspected. Example: MaintenanceTargetType.FACILITY_RESOURCE
    @NotNull
    private MaintenanceTargetType targetType;

    // Links to the record named by targetType. Example: "67c31122dc3f7d0011223344"
    @NotBlank
    private String targetDocsId;

    // What was being checked. Example: InspectionType.FIRE_SAFETY
    @NotNull
    private InspectionType inspectionType;

    // Links to MaintenancePlan.id when this round came due from a plan. Null for a
    // one-off or an unannounced visit. Example: "67c31127dc3f7d0066778899"
    private String maintenancePlanDocsId;

    // The day it happened. Example: 2026-08-21
    @NotNull
    private LocalDate inspectedOn;

    // Links to Staff.id when the school's own person did it. Null for an outside
    // inspector. Example: "67aa15d9dc3f7d0044444444"
    private String inspectorStaffDocsId;

    // Who came, when they are not the school's own staff. A fire officer from the
    // municipality is not in the staff collection and should not be.
    // Example: "R. Deshmukh, Station Officer, Dadar Fire Station"
    private String externalInspector;

    // Which body they came from. Example: "Mumbai Fire Brigade"
    private String inspectingAuthority;

    // What was concluded. Example: InspectionOutcome.PASSED_WITH_OBSERVATIONS
    @NotNull
    private InspectionOutcome outcome;

    // Everything found, with a severity and a link to the job raised for it.
    @Valid
    @Builder.Default
    private List<InspectionFinding> findings = new ArrayList<>();

    // The certificate number the authority issued. Example: "MFB/FS/2026/11487"
    private String certificateNumber;

    // When the certificate runs out. **Read on the day, never trusted to a status
    // field.** Null where the inspection produces no certificate. Example: 2027-08-20
    private LocalDate certificateValidUntil;

    // Links to DocumentRecord.id for the certificate itself.
    // Example: "67c31123dc3f7d0022334455"
    private String certificateDocumentDocsId;

    // Links to DocumentRecord.id for the inspector's written report and any photographs.
    // Example: ["67c31131dc3f7d0011002233"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    // Links to Staff.id of whoever at the school accompanied the inspector and is
    // answerable for acting on it. Example: "67aa15d9dc3f7d0055555555"
    private String accompaniedByStaffDocsId;

    // When the record was entered, which can be days after the visit.
    // Example: 2026-08-24T05:00:00Z
    private Instant recordedAt;

    // Anything worth knowing.
    // Example: "Inspector asked for the assembly drill log at the next visit."
    private String remarks;
}
