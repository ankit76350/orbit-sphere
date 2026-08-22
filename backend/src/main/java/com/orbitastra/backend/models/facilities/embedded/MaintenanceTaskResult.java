package com.orbitastra.backend.models.new_new.facilities.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One item on a maintenance checklist, and whether it was actually done.
 *
 * <p>{@code task} is **copied from MaintenancePlan.checklistItems** when the work order is
 * raised, rather than read through the plan. A plan gets edited — somebody adds "check the
 * earthing" next year — and a completed work order from last March has to keep showing the
 * seven things that were on the list then, not the eight there are now. This is the same rule
 * FeeInvoiceLine follows for the fee head name and PurchaseOrderLine for the item name.
 *
 * <p>{@code completed} being false on a COMPLETED work order is allowed, and is the reason
 * this is a list of objects rather than a list of strings. A generator service where six of
 * seven checks were done and the seventh could not be is a real outcome, and it needs somewhere
 * to say which one and why. A checklist that can only be all-or-nothing gets ticked entirely.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceTaskResult {

    // Order on the checklist. Example: 1
    @NotNull
    private Integer taskNo;

    // What was to be done, copied from the plan when the job was raised.
    // Example: "Check and top up the coolant."
    @NotBlank
    private String task;

    // Whether it was done. Example: true
    @NotNull
    @Builder.Default
    private Boolean completed = false;

    // What was found, or why it was not done. Required when completed is false,
    // because a checklist item skipped with no reason is one nobody will ever chase.
    // Example: "Could not reach the rear filter without lifting the unit."
    private String note;
}
