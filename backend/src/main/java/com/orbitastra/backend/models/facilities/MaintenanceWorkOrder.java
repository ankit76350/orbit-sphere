package com.orbitastra.backend.models.facilities;

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

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.facilities.embedded.MaintenanceTaskResult;
import com.orbitastra.backend.models.facilities.enums.MaintenancePriority;
import com.orbitastra.backend.models.facilities.enums.MaintenanceTargetType;
import com.orbitastra.backend.models.facilities.enums.WorkOrderStatus;

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
 * One job of work on one thing.
 *
 * <p>Two kinds of job arrive here and they are the same shape. A plan came due — the generator
 * needs its quarterly service. Or somebody reported something — the fan in Class VI-B has
 * stopped. Both are a thing that needs doing to a named target by a named person.
 *
 * <p>**Which kind it is, is not a stored field.** A null {@code maintenancePlanDocsId} means
 * somebody reported it; a non-null one means a plan raised it. A `maintenanceKind` enum beside
 * that pointer would be a second fact able to disagree with it, which is the same reason
 * `inventory` has no direction field beside its movement type and MedicationAdministration lost
 * its `usedStandingConsent` boolean.
 *
 * <p>The distinction still matters, and it is the most useful number this collection produces:
 * **a school where most work orders have no plan behind them is a school repairing things
 * rather than maintaining them.** That ratio is a query, not a field.
 *
 * <p>{@code tasks} is copied from the plan's checklist when the job is raised, never read
 * through the plan. See MaintenanceTaskResult: adding a check to the plan next year must not
 * make last March's completed job look as though it skipped one.
 *
 * <p>{@code reportedByType} is deliberately absent. Anybody may report a broken fan — a
 * teacher, a warden, a parent through the reporting channel — and rather than a second
 * polymorphic pointer, a report that came in through `feedback` carries
 * {@code sourceReportDocsId}. Everything else is staff, and {@code reportedByStaffDocsId} says
 * who.
 *
 * <p>CLOSED_UNRESOLVED is the state that keeps this list honest. Sometimes the roof cannot be
 * fixed this year, and a job that has to be either open forever or marked COMPLETED will end up
 * marked COMPLETED — which is a lie the next person reads as a fact about whether that classroom
 * is safe. See WorkOrderStatus.
 *
 * <p>{@code actualCost} is what was spent. Where a vendor billed for it,
 * {@code supplierInvoiceDocsId} links to the bill in `procurement`, so maintenance spending can
 * be totalled from paid invoices rather than from numbers somebody typed here. The two should
 * agree, and the invoice wins.
 *
 * <p>The service checks that the target exists in the collection its type names, that a
 * COMPLETED job has a completion date and every task accounted for, that CLOSED_UNRESOLVED and
 * CANCELLED both carry a reason, that completing a job raised by a plan moves that plan's
 * {@code nextDueDate} forward, and that an EMERGENCY job on a CRITICAL inspection finding is
 * never closed while the finding is unresolved.
 */
@Document(collection = "maintenance_work_orders")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_work_order_no_uniq",
                def = "{'schoolId': 1, 'workOrderNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_work_order_open_idx",
                def = "{'schoolId': 1, 'status': 1, 'priority': -1, 'reportedAt': 1}"),
        @CompoundIndex(
                name = "school_work_order_target_idx",
                def = "{'schoolId': 1, 'targetType': 1, 'targetDocsId': 1, 'reportedAt': -1}"),
        @CompoundIndex(
                name = "school_work_order_plan_idx",
                def = "{'schoolId': 1, 'maintenancePlanDocsId': 1, 'reportedAt': -1}",
                partialFilter = "{'maintenancePlanDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_work_order_assignee_idx",
                def = "{'schoolId': 1, 'assignedToStaffDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_work_order_due_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueBy': 1}",
                partialFilter = "{'dueBy': {'$type': 'date'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceWorkOrder extends SchoolBase {

    // School-scoped number from NumberSequence type MAINTENANCE_WORK_ORDER. What gets
    // quoted to a contractor and written on a job card. Example: "WO/2026/000512"
    @NotBlank
    private String workOrderNo;

    // Links to MaintenancePlan.id when a plan raised this. Null when somebody reported
    // it, and that null is the only thing that distinguishes the two kinds of job.
    // Example: "67c31127dc3f7d0066778899"
    private String maintenancePlanDocsId;

    // What needs work. Example: MaintenanceTargetType.FACILITY_RESOURCE
    @NotNull
    private MaintenanceTargetType targetType;

    // Links to the record named by targetType. Example: "67c31122dc3f7d0011223344"
    @NotBlank
    private String targetDocsId;

    // A one-line summary, which is what appears in a list of jobs.
    // Example: "Ceiling fan in VI-B not running"
    @NotBlank
    private String title;

    // The detail. Example: "Fan makes a humming noise and does not turn. Regulator
    // works on the other two fans in the room."
    private String description;

    // How urgent. Example: MaintenancePriority.NORMAL
    @NotNull
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.NORMAL;

    // Where it stands. Example: WorkOrderStatus.IN_PROGRESS
    @NotNull
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.REPORTED;

    // Links to Staff.id of whoever reported it.
    // Example: "67aa15d9dc3f7d0044444444"
    private String reportedByStaffDocsId;

    // Links to FeedbackReport.id when this came in through the reporting channel rather
    // than from a member of staff directly. Keeps the reporter's promise of anonymity
    // intact: this points at the report, never at a person.
    // Example: "67be1128dc3f7d0077889900"
    private String sourceReportDocsId;

    // Links to FacilityInspection.id when this job came out of an inspection finding.
    // Example: "67c31129dc3f7d0088990011"
    private String sourceInspectionDocsId;

    // When it was raised. Example: 2026-08-21T04:10:00Z
    @NotNull
    private Instant reportedAt;

    // When it should be done by. From the plan's due date, or set when the job is
    // triaged. Example: 2026-08-28
    private LocalDate dueBy;

    // Links to Staff.id of whoever is doing it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String assignedToStaffDocsId;

    // Links to Vendor.id when an outside firm is doing it.
    // Example: "67bd1122dc3f7d0011223344"
    private String vendorDocsId;

    // What was to be done and whether it was, copied from the plan's checklist. Empty for
    // a reported job that had no checklist.
    @Valid
    @Builder.Default
    private List<MaintenanceTaskResult> tasks = new ArrayList<>();

    // When work started. Example: 2026-08-22T05:00:00Z
    private Instant startedAt;

    // When it was finished. Example: 2026-08-22T07:30:00Z
    private Instant completedAt;

    // What was actually done, in words. Required for COMPLETED.
    // Example: "Capacitor replaced. Fan runs on all three speeds."
    private String workDoneNote;

    // Why it will not be done, or why it was called off. Required for CLOSED_UNRESOLVED
    // and CANCELLED.
    // Example: "Roof needs replacing, not patching. Deferred to the summer break; room
    // stays closed until then."
    private String closureReason;

    // What it cost the school. Example: 850.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal actualCost;

    // Links to SupplierInvoice.id when a vendor billed for this, so maintenance spending
    // can be totalled from real bills rather than typed figures. Where the two disagree,
    // the invoice wins. Example: "67bd1128dc3f7d0077889900"
    private String supplierInvoiceDocsId;

    // Links to StockIssue.id when parts came out of the school's own store, so a repair
    // that used two bulbs and a switch shows up in both places.
    // Example: "67bc1129dc3f7d0088990011"
    private String stockIssueDocsId;

    // Links to DocumentRecord.id for photographs before and after, or a contractor's
    // job sheet. Example: ["67c31130dc3f7d0099001122"]
    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    // Anything worth knowing.
    // Example: "Second time this fan has failed in a year. Consider replacing it."
    private String remarks;
}
