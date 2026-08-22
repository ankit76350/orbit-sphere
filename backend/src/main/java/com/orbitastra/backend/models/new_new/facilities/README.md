# facilities — the buildings, the things in them, and keeping both working

## Status: models only

The **models in this package were designed on 2026-08-21** and are finished. No API has been
designed for any of it — endpoints come **at the very end**, after every module's models are
done. The "the service checks that..." notes in the javadoc and the numbered rules below are a
**specification waiting for an implementation**, not a description of code that exists.

## What these models answer

1. What spaces does the school have, and can they be used?
2. Which individual objects does it own, where are they, and who is answerable?
3. What is due for servicing, and what is overdue?
4. Which safety certificates are valid, and which have lapsed?
5. Who has the hall on Saturday?

## Why this was deferred three times, and what unblocked it

`inventory` and `procurement` both stopped at the same line and said so:

> Thirty microscopes are a quantity of thirty here, not thirty asset tags with service
> histories and depreciation.

And two other packages left notes pointing this way — [`transport`](../transport/README.md)
deferred fuel, odometer and servicing to "a facilities or maintenance module", and
[`hostel`](../hostel/README.md) deferred "room and mess inspection rounds" as "closer to
facilities than to residence."

The thing that made it awkward is that **the school's physical stuff already lives in three
finished packages.** A hostel room is in `hostel`. A dining hall is in `mess`. A bus is in
`transport`. All three need servicing and inspecting.

The two obvious designs were both bad:

| Option | Cost |
|---|---|
| Make everything a `FacilityResource` | Rewrite `hostel`, `mess` and `transport` |
| Give each package its own work orders | Four inspection systems, no single overdue list |

So there is a third: [`MaintenanceTargetType`](enums/MaintenanceTargetType.java). A work order,
plan or inspection names its target by **type and id**, and the packages that already own those
records are untouched.

## Relationship overview

```text
FacilityResource                  the space register: building -> floor -> room
   |   parentResourceDocsId points at another row here (like InventoryCategory)
   |   bookable / accessible / capacity / status
   |
   +--> AssetRegisterItem         one tagged object, sitting in a space
   |       inventoryItemDocsId --> what KIND of thing (inventory)
   |       goodsReceiptDocsId  --> which delivery it arrived on (procurement)
   |       custodianType + custodianDocsId --> staff | department | room
   |
   +--> ResourceBooking           only APPROVED holds the space
   |
MaintenancePlan                   "service the generator every 3 months"
   |   targetType + targetDocsId --> resource | asset | hostel room | mess hall | vehicle
   |   intervalMonths + nextDueDate + checklistItems[]
   |
   v
MaintenanceWorkOrder              one job. Plan came due, OR somebody reported it.
   |   null maintenancePlanDocsId == somebody reported it. No separate "kind" field.
   +--> MaintenanceTaskResult[]     checklist COPIED from the plan at raise time
   |
FacilityInspection                fire, wiring, water, lifts, playground
   +--> InspectionFinding[]         each with workOrderDocsId -- null == not acted on
         certificateValidUntil    READ ON THE DAY, never trusted to a status
```

## Models named here from other packages

| Model | Package | Why |
|---|---|---|
| [`InventoryItem`](../inventory/InventoryItem.java) | inventory | what *kind* of thing an asset is |
| [`InventoryCategory`](../inventory/InventoryCategory.java) | inventory | the self-referencing-parent precedent |
| [`StockMovement`](../inventory/StockMovement.java) | inventory | the no-redundant-direction-field precedent |
| [`StockIssue`](../inventory/StockIssue.java) | inventory | parts taken from the store for a repair |
| [`GoodsReceipt`](../procurement/GoodsReceipt.java) | procurement | populates the asset register from deliveries |
| [`PurchaseOrder`](../procurement/PurchaseOrder.java) | procurement | what an asset was ordered on |
| [`SupplierInvoice`](../procurement/SupplierInvoice.java) | procurement | what a repair actually cost |
| [`Vendor`](../procurement/Vendor.java) | procurement | who services it |
| [`HostelRoom`](../hostel/HostelRoom.java) | hostel | a maintenance target, **not** replaced by this package |
| [`MessHall`](../mess/MessHall.java) | mess | a maintenance target, **not** replaced |
| [`TransportVehicle`](../transport/TransportVehicle.java) | transport | a maintenance target — answers their deferral |
| [`TransportTrip`](../transport/TransportTrip.java) | transport | the check-expiry-on-the-day precedent |
| [`FeedbackReport`](../feedback/report/FeedbackReport.java) | feedback | a job reported through the anonymous channel |
| [`Staff`](../people/staff/Staff.java) | people | reporters, assignees, custodians, inspectors |
| [`Department`](../people/organization/Department.java) | people | a custodian, and a booking's owner |
| [`DocumentRecord`](../documents/DocumentRecord.java) | documents | certificates, floor plans, photographs |
| [`NumberSequence`](../institution/NumberSequence.java) | institution | asset tags and four document numbers |
| [`FeeInvoice`](../finance/billing/FeeInvoice.java) | finance | billing an outside party for a hall hire |
| [`AcademicYear`](../core/AcademicYear.java) | core | `holidays` — no weekday is assumed free |
| [`ConcessionPolicy`](../finance/config/ConcessionPolicy.java) | finance | the standing-thing / dated-event precedent |

## The collections

| Collection | Purpose |
|---|---|
| `facility_resources` | One space or structure. A tree: building → floor → room. |
| `asset_register_items` | One individually tagged object, with a service history. |
| `maintenance_plans` | A standing arrangement to service something on a cycle. |
| `maintenance_work_orders` | One job of work on one thing. |
| `facility_inspections` | One safety or condition check, on one day, with findings. |
| `resource_bookings` | Somebody asking to use a space for a stretch of time. |

`InspectionFinding` and `MaintenanceTaskResult` are embedded and have no collections.

## Six decisions worth explaining

### 1. Inventory quantity vs asset tag: one question decides it

**Does the school need to answer things about *this specific one*?**

```
"How many microscopes do we have?"        -> inventory. A StockBalance of 30.
"When was microscope 14 last serviced?"   -> here. Thirty rows, thirty tags.
```

In practice a thing crosses over when it gets a tag, has a service life, and is worth enough to
carry as an asset rather than expense when bought. A box of chalk never crosses. A projector
always does. **A football goes either way and the school decides.**

The two are joined rather than duplicated: `inventoryItemDocsId` says what kind of thing it is
so the item master is not re-keyed, and `goodsReceiptDocsId` says which delivery it came on.
Creating asset rows from an accepted [`GoodsReceipt`](../procurement/GoodsReceipt.java) of a
`NON_CONSUMABLE` item is how a register gets populated without anybody typing it twice — and it
closes the loop from `procurement` through `inventory` to here.

### 2. `MaintenanceTargetType` instead of rewriting three packages

See above. The type is stored **beside** the id, and that is correct here for the same reason it
was wrong on `FeedbackSubmission`: the test is *does anything else on this row already know?* A
feedback submission always had its topic, which declared the type. A work order has no
configuration that knows whether its target is a bus or a boiler. This is the
`FeeInvoice.sourceType` case.

### 3. Preventive vs reactive is a query, not a field

A **null `maintenancePlanDocsId` means somebody reported it.** A non-null one means a plan raised
it. There is no `maintenanceKind` enum, because it would be a second fact able to disagree with
the pointer beside it — the same reason [`StockMovement`](../inventory/StockMovement.java) has no
direction field beside its movement type.

The distinction still matters, and it produces the most useful number in this package: **a school
where most work orders have no plan behind them is repairing its buildings, not maintaining
them.** That ratio is one query.

### 4. Certificates are checked on the day, never trusted to a status

Borrowed wholesale from [`transport`](../transport/README.md), where the same argument covers a
vehicle's insurance and fitness papers.

A certificate expiring is **an event with no user action behind it.** Nobody is sitting at a
screen on the morning it lapses. So a service that reads a status field finds everything looks
fine right up until an inspector says otherwise. `certificateValidUntil` is indexed and must be
evaluated at the moment of use.

### 5. `intervalMonths`, not a recurrence expression

The sketch had `recurrenceExpression` as a string — a small programming language stored in a
database field. Nobody can validate it, the office cannot read it, and **"every 3 months" is what
a school actually means every single time.** A plan that genuinely needs "the first Tuesday after
the monsoon" is a human deciding, not a schedule.

Same reasoning kept recurrence off `ResourceBooking`. "Every Tuesday for the term" is twelve
rows, because the fourth Tuesday is a holiday and the seventh clashes with an exam — and a
recurring booking that cannot have one instance moved is worse than twelve rows.

### 6. Depreciation inputs, not a depreciation schedule

`acquisitionCost`, `usefulLifeYears` and `salvageValue` are stored. **No accumulated figure and
no schedule.** Straight-line depreciation from three numbers and a date is arithmetic, and a
stored schedule goes stale the day anybody revalues anything — the same rule that kept stock
valuation out of `inventory`.

There is also nowhere to post a depreciation entry: the bookkeeping models were deleted on
2026-08-12. When they return, this is where the inputs are.

## States that exist because a failure needs somewhere to live

Same rule as `NOT_COMPLETED`, `NOT_RETURNED`, `MISSED` and `SHORT_CLOSED` elsewhere.

| State | Without it |
|---|---|
| [`WorkOrderStatus.CLOSED_UNRESOLVED`](enums/WorkOrderStatus.java) | A roof that cannot be fixed this year is either open forever or marked `COMPLETED` — a lie the next person reads as a fact about whether that room is safe. |
| [`WorkOrderStatus.AWAITING_PARTS`](enums/WorkOrderStatus.java) | A job waiting six weeks for a pump looks like a job somebody forgot. Once everything is overdue, nothing is. |
| [`AssetStatus.LOST`](enums/AssetStatus.java) | Every missing microscope becomes a "disposal", and nobody can count what the school cannot find. |
| [`FacilityResourceStatus.CLOSED`](enums/FacilityResourceStatus.java) | A lab closed because there is no chemistry teacher looks like one waiting for a plumber. |

## Anonymous reports become work orders without unmasking anybody

`MaintenanceWorkOrder.sourceReportDocsId` links to
[`FeedbackReport`](../feedback/report/FeedbackReport.java), not to a person. A child who reports
a broken stair railing through the anonymous channel gets the railing fixed, and the work order
carries the report id rather than a reporter — so the promise made in
[`feedback/README.md`](../feedback/README.md) survives the job being done.

That link is the one that makes the reporting channel worth having on the facilities side: a
report that produces a work order is a report that changed something.

## Deliberately left out

- **Utility meter readings and consumption.** Electricity and water usage over time is a
  time-series problem with its own retention story, the same argument that kept live GPS out of
  `transport`. A monthly bill is a `SupplierInvoice`.
- **Fuel and odometer logs.** `transport` deferred these here, and they are still deferred: a
  bus's fuel efficiency is a fleet-telemetry question, not a maintenance job. What **has** landed
  is servicing — a vehicle is now a valid `MaintenanceTargetType`, so the quarterly service and
  the fitness inspection have a home.
- **Room allocation for the timetable.** Nothing in `academics` points here, and
  `TimetableEntry` still has no room. Giving lessons rooms is a timetabling change, and it should
  be made in `academics` when somebody wants it, not smuggled in from this side.
- **Cleaning schedules and housekeeping rosters.** A daily task list for cleaning staff is a
  rota, closer to staff duty allocation than to maintenance.
- **Contractor management.** A vendor with a maintenance contract is a
  [`Vendor`](../procurement/Vendor.java). Contract terms, SLAs and penalty clauses are a
  procurement concern and are not modelled anywhere yet.
- **Keys and access control.** Who holds the key to the chemistry store is real and belongs
  closer to `gate`.
- **Notifications.** "Your work order was completed", "the fire certificate expires in 30 days" —
  none of it is here, and nothing records whether a message went out. `notification` is designed
  **last** by the decision of 2026-08-14. The certificate-expiry warning is the one that will
  matter most; until then the indexed date makes it a query somebody has to run.

## Rules the services must enforce

**Resources**

1. A resource is never its own ancestor, and a parent is a plausible container for its child
   (a `CLASSROOM` does not contain a `BUILDING`).
2. `resourceCode` is unique per school and never renamed once anything points at it.
3. `CLOSED` and `DECOMMISSIONED` both require `statusReason`.
4. A resource with `APPROVED` future bookings or open work orders is not decommissioned until
   those are dealt with.
5. `DECOMMISSIONED` rows are never deleted — inspections and work orders point at them.

**Assets**

6. `assetTag` is never reused, even after disposal. A returning tag merges two service
   histories.
7. `LOST`, `DISPOSED` and `WRITTEN_OFF` all require `disposalNote`; disposal requires a date.
8. The custodian must exist in the collection `custodianType` names.
9. An asset is not `IN_USE` while its facility resource is `DECOMMISSIONED`.
10. No depreciation figure is stored. It is computed from cost, life, salvage and date.

**Plans and work orders**

11. The target must exist in the collection `targetType` names.
12. `nextDueDate` agrees with the last completed work order plus `intervalMonths`, and is
    rebuildable from them.
13. A plan is not deactivated while a work order it raised is open; deactivating a `statutory`
    plan records a reason and a person.
14. `tasks` are copied from the plan's checklist at raise time, never read through the plan.
15. A `COMPLETED` job has `completedAt`, `workDoneNote`, and every task either done or carrying
    a note saying why not.
16. `CLOSED_UNRESOLVED` and `CANCELLED` both require `closureReason`.
17. Completing a plan-raised job moves that plan's `nextDueDate` forward, in the same
    operation.
18. Where both `actualCost` and `supplierInvoiceDocsId` exist, the invoice is authoritative.

**Inspections**

19. `FAILED` carries at least one finding.
20. A `CRITICAL` finding forces the target's status to `UNDER_MAINTENANCE` or `CLOSED`, and the
    finding cannot be resolved while the work order for it is open.
21. An outcome that produced a certificate carries `certificateValidUntil`.
22. Certificate validity is evaluated **at the moment of use**, never cached in a status field.
23. Recurrence lives on the plan. No next-due date is stored on an inspection.

**Bookings**

24. The resource must be `bookable` and `IN_USE`.
25. No `APPROVED` booking may overlap another for the same resource. Checked in code at
    approval — overlap is a range comparison and no unique index can express it.
26. `REQUESTED` bookings hold nothing, so two may overlap; the clash is caught at approval.
27. `startsAt` is before `endsAt`; `expectedAttendance` does not exceed the resource's
    `capacity`.
28. `REJECTED` and `CANCELLED` both carry a reason.
29. A booking is refused on a space with an open `CRITICAL` inspection finding.
30. Non-working days come from [`AcademicYear.holidays`](../core/AcademicYear.java). No weekday
    is assumed to be free.
