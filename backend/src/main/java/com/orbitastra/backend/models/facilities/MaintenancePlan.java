package com.orbitastra.backend.models.facilities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.facilities.enums.MaintenancePriority;
import com.orbitastra.backend.models.facilities.enums.MaintenanceTargetType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A standing arrangement to service something on a cycle.
 *
 * <p>"Service the generator every three months." "Test the fire extinguishers every year."
 * "Clean the water tanks every six months." One row each, set up once and used for years. A
 * MaintenanceWorkOrder is the dated event this produces, the same split as ConcessionPolicy
 * against ConcessionRequest and TransportRoute against TransportTrip.
 *
 * <p>This is the whole difference between a school that maintains its buildings and one that
 * repairs them. Without a plan, every work order is somebody reporting something already
 * broken, and the maintenance record is a list of failures. With one, the pump gets serviced
 * before the monsoon rather than during it.
 *
 * <p>{@code intervalMonths} rather than a cron expression. The sketch had a
 * {@code recurrenceExpression} string, which is a small programming language stored in a
 * database field: nobody can validate it, the office cannot read it, and "every 3 months" is
 * what a school actually means every single time. A plan that genuinely needs "the first
 * Tuesday after the monsoon" is a human deciding, not a schedule.
 *
 * <p>{@code nextDueDate} is a running field, moved forward when a work order for this plan
 * completes. It must always be rebuildable from the last completed work order plus the
 * interval, and it is what the overdue query reads. Keeping it means "what is due this month"
 * is one indexed lookup rather than a scan of every plan working out dates.
 *
 * <p>{@code checklistItems} is a plain list of strings here, and becomes a list of
 * MaintenanceTaskResult when a work order copies it. That copy is the point: adding a check to
 * the plan next year must not make last March's completed job look like it skipped something.
 *
 * <p>{@code targetType} and {@code targetDocsId} let one plan cover a bus, a hostel room, a
 * generator or the assembly hall. See MaintenanceTargetType for why that indirection exists
 * rather than everything being a FacilityResource.
 *
 * <p>{@code statutory} marks the plans the school does not get to skip. A fire extinguisher
 * service is not a matter of opinion, and separating those from "repaint the corridor" is what
 * makes an overdue list something a principal has to act on rather than a wish list.
 *
 * <p>The service checks that the target exists in the collection its type names, that
 * {@code nextDueDate} agrees with the last completed work order, that a plan is not
 * deactivated while a work order it raised is still open, and that deactivating a
 * {@code statutory} plan is recorded with a reason and a person.
 */
@Document(collection = "maintenance_plans")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_maintenance_plan_code_uniq",
                def = "{'schoolId': 1, 'planCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_maintenance_plan_due_idx",
                def = "{'schoolId': 1, 'active': 1, 'nextDueDate': 1}"),
        @CompoundIndex(
                name = "school_maintenance_plan_target_idx",
                def = "{'schoolId': 1, 'targetType': 1, 'targetDocsId': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_maintenance_plan_statutory_idx",
                def = "{'schoolId': 1, 'statutory': 1, 'active': 1, 'nextDueDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenancePlan extends SchoolBase {

    // The school's own code for this plan. Example: "GEN-QTR-SERVICE"
    @NotBlank
    private String planCode;

    // What it is called. Example: "Generator quarterly service"
    @NotBlank
    private String name;

    // What is being serviced. Example: MaintenanceTargetType.ASSET
    @NotNull
    private MaintenanceTargetType targetType;

    // Links to the record named by targetType. Example: "67c31126dc3f7d0055667788"
    @NotBlank
    private String targetDocsId;

    // How often, in months. Three means quarterly, twelve means yearly. A plain number
    // rather than a recurrence expression, because that is what a school means.
    // Example: 3
    @NotNull
    @Min(1)
    private Integer intervalMonths;

    // When it is next due. Moved forward as work orders complete, and always rebuildable
    // from the last completed one plus the interval. Example: 2026-11-20
    @NotNull
    private LocalDate nextDueDate;

    // How many days before the due date a work order should be raised, so the job is
    // arranged rather than already late. Example: 14
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer leadDays = 7;

    // What has to be done each time. Copied onto each work order, so editing this does
    // not rewrite the history of jobs already done.
    // Example: ["Check and top up the coolant.", "Run for 15 minutes under load."]
    @NotEmpty
    @Builder.Default
    private List<String> checklistItems = new ArrayList<>();

    // How urgent the job is when it comes due. Example: MaintenancePriority.NORMAL
    @NotNull
    @Builder.Default
    private MaintenancePriority defaultPriority = MaintenancePriority.NORMAL;

    // Whether the law or a board requires this. These are the plans the school does not
    // get to skip, and the ones an overdue list must show first. Example: true
    @NotNull
    @Builder.Default
    private Boolean statutory = false;

    // Links to Staff.id of whoever the job normally goes to.
    // Example: "67aa15d9dc3f7d0044444444"
    private String assignedToStaffDocsId;

    // Links to Vendor.id when an outside firm does it under a contract.
    // Example: "67bd1122dc3f7d0011223344"
    private String vendorDocsId;

    // Roughly what one visit costs, so a year's maintenance can be budgeted for. Not what
    // any particular job cost — that is on the work order. Example: 3500.00
    private Double estimatedCostPerVisit;

    // Whether this plan still raises work orders. Turning it off leaves every job already
    // done alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Why it was switched off. Required when active is false, and it matters most for a
    // statutory plan, where "we stopped doing the fire checks" needs a name against it.
    // Example: "Generator sold in March. Plan closed with it."
    private String inactiveReason;

    // Anything worth knowing.
    // Example: "Vendor sends the same engineer. Ask for Suresh; he knows the wiring."
    private String remarks;
}
