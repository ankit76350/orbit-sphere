# transport — buses, routes, and who is on them

These models answer four questions:

1. Which bus, and is it fit to run today?
2. Where does it go, and who is driving it this week?
3. Which children ride it, from which stop, and what do they pay?
4. Did every child who should have been on it actually get on and get off?

## Relationship overview

```text
TransportVehicle        TransportDriver ----> Staff
   (the bus)              (licence only; name lives on Staff)
        \                 /
         \               /
        RouteAssignment          who drives what, for a stretch of days
              |
              v
        TransportRoute
          +--> RouteStop[]       embedded, ordered, each with its own fare
              |
              |<---- TransportAllocation ----> Student
              |        pickupStopCode / dropStopCode
              |        monthlyFareAmount + feeHeadDocsId ---> FeeInvoice
              v
        TransportTrip            one run, one route, one day, one direction
              |
              +--> TransportBoardingRecord[]   one per child per trip
```

## The collections

| Collection | Purpose |
|---|---|
| `transport_vehicles` | One bus, van or car, with the four expiry dates that can stop it legally. |
| `transport_drivers` | The driving side of a staff member — licence and badge only. |
| `transport_routes` | Where the bus goes, with stops embedded in order. |
| `route_assignments` | Which vehicle and driver are on a route, for a stretch of days. |
| `transport_allocations` | One student's seat for one year, and what they pay. |
| `transport_trips` | One run of one route on one day, in one direction. |
| `transport_boarding_records` | What happened to one child on one trip. |

`RouteStop` and `GeoLocation` are embedded and have no collections of their own.

## Plan and reality are separate

The split that shapes this whole package:

- `TransportRoute` and `RouteAssignment` are the **plan**. They barely change.
- `TransportTrip` and `TransportBoardingRecord` are **what happened**. Two rows a
  day per route, forever.

They are kept apart because a plan gets edited and a record must not. When the
route assignment changes next week, last Tuesday's trip still has to show who was
actually driving. That is why the trip copies `vehicleDocsId`,
`transportDriverDocsId` and `attendantStaffDocsId` in rather than reading them
through the assignment — the same reason `FeeInvoiceLine` keeps its own copy of
the fee head name.

## Three decisions you asked about

### 1. Live GPS tracking is not in this package

`TransportVehicle.gpsDeviceId` names the tracker, and `TransportBoardingRecord`
keeps the one location that matters legally — where the bus was when a child got
on. **Where the bus is right now is not stored here.**

A tracker sends a position every few seconds. Twenty buses running two trips a day
is on the order of a million rows a month, all of it worthless within the hour.
That is a time-series problem with its own storage, its own retention rules and
its own scaling story, and none of it looks like the seven collections above.
Mixing it in would drag every one of them toward a shape they do not need.

When live tracking is built, it should be its own decision: a MongoDB time-series
collection with a `TTL` index, or a purpose-built store, fed straight from the
devices and read by the parent app without going near these models.

Three sketches existed for this and all were deleted with `models/undone` on 2026-08-21:
`VehicleLocation` and `VehicleLocationHistory` (a current position plus an append-only trail),
and an earlier `TransportTelemetryPoint` that already assumed a time-series collection. That
last instinct was the right one — it just does not belong in the same design pass as the seven
collections above.

### 2. Allocation drives billing

`TransportAllocation` carries `monthlyFareAmount` and `feeHeadDocsId`, and that is
the whole connection to money. The fee run reads every `ACTIVE` allocation and
adds a line for that head. Nothing else in transport touches money.

Without it, somebody has to remember to bill transport by hand for every family
every month, and `FeeCategory.TRANSPORT` would exist with nothing feeding it.

`monthlyFareAmount` is **copied** from the stop when the allocation is made and is
never rewritten. Changing the price list in November must not silently change what
a family agreed to in April. A new price means ending the allocation and starting
another, so the change has a date on it. This is the same rule `ConcessionRequest`
follows when it copies a rate from its policy.

### 3. Fares are per stop

`RouteStop.monthlyFareAmount` is what makes distance pricing work — a family
twenty minutes further out pays more. Setting the same amount on every stop gives
a flat fare instead, so both ways of charging work with no extra setting and no
fare-band model.

The stop holds the **current price list**. What any one family pays lives on their
allocation, as above.

## Stops are embedded, so allocations point at `stopCode`

A route has ten or twenty stops, they are always read with the route, and their
order is part of what the route is. So they are embedded, following
`SchoolClass.sections` and `FeeStructure.installments`.

That means a stop has no document id. `TransportAllocation.pickupStopCode` and
`dropStopCode` name a stop by `RouteStop.stopCode` instead — the same pattern as
`sectionNo` inside `SchoolClass`. A `stopCode` must be unique inside its route and
must not be renamed once students are allocated to it.

Either stop code may be null. Plenty of families use the bus one way only, and a
null means the child does not travel that way.

## One boarding row per child per trip

Not one row per event. Getting on and getting off are two moments in one story, so
they are two timestamps on one row rather than two rows somebody has to pair up. A
child who got on and never got off is then a single row that is obviously wrong,
instead of a missing row nobody notices.

**Rows are created in advance**, when the trip list is built, with status
`EXPECTED`. This matters more than it sounds. If rows only appeared when a child
was scanned, a child who never turned up would leave no trace at all — and "who is
missing" is the question this model exists to answer.

`MISSED` and `NOT_TRAVELLING` are deliberately different. `NOT_TRAVELLING` is the
family having told the school in advance. `MISSED` is nobody knowing where the
child is, and that is the one that has to reach a parent quickly.

## Expiry dates are checked on the day, not by status

A vehicle has four dates that can stop it legally — insurance, fitness, pollution,
permit — and a driver has two, licence and badge.

The service must check these **when a trip starts**, not trust that somebody
changed `VehicleStatus` in time. A certificate expiring is an event with no user
action behind it; nobody is sitting at a screen on the morning it lapses. Relying
on the status field means the first person to notice is a traffic inspector.

## Naming: `TransportAllocationStatus`, not `AllocationStatus`

`finance` already has an `AllocationStatus`, and that one is about splitting a
payment across invoices. Completely different meaning. The prefix is there so the
two can never be confused or auto-imported into each other.

## Notifications are not here

"Tell the parent their child boarded" is the obvious next thought, and it is not
in this package. Nothing here records whether a message went out.

That belongs to `notification`, which by decision on 2026-08-14 is designed
**last**. Until then, boarding alerts, bus-delay messages and missed-child warnings
are all deferred. Do not add a `notificationSentAt` field here to get around it —
see `notification/README.md`.

## Deliberately left out

- **Live vehicle position** — see above.
- **Fuel, odometer and telemetry.** The sketch had `VehicleHealth` with fuel level, engine
  temperature and battery voltage. Still deferred — that is fleet telemetry, a time-series
  problem like live position above.
  **Servicing, however, now has a home.** [`facilities`](../facilities/README.md) was built on
  2026-08-21, and a vehicle is a valid `MaintenanceTargetType`: the quarterly service is a
  [`MaintenancePlan`](../facilities/MaintenancePlan.java), the job is a
  [`MaintenanceWorkOrder`](../facilities/MaintenanceWorkOrder.java), and the fitness check is a
  [`FacilityInspection`](../facilities/FacilityInspection.java) whose
  `certificateValidUntil` is read on the day — the same rule this package already follows for
  insurance and permits.
- **Route optimisation.** Working out the best order of stops is an algorithm, not
  a model. `sequenceNo` holds whatever order a human decided.
- **Trip numbers from `NumberSequence`.** A trip is already identified by route,
  date and direction, which are unique together. Hundreds of daily trips should
  not queue for numbers.
- **Ad-hoc trips** such as a field trip or a match. Those are a different thing
  from a daily route run and would need their own model rather than bending
  `TransportTrip`.

## Rules the services must enforce

**Setup**

1. A `stopCode` is unique inside its route, and `sequenceNo` runs from 1 with no
   gaps.
2. Pickup times go forward along the route; drop times go backward.
3. A stop still used by an `ACTIVE` allocation is never removed or renamed.
4. A route cannot be deactivated while `ACTIVE` allocations point at it.

**Assignment**

5. The vehicle and driver must both be `ACTIVE`, and none of their expiry dates
   may have passed on the day.
6. No two `active` assignments overlap for the same route and direction.
7. A driver is not on two routes at the same time of day.

**Allocation**

8. One `ACTIVE` allocation per student per academic year.
9. Both stop codes must exist on the named route, and at least one must be set.
10. Allocations on a route must not exceed the assigned vehicle's `capacity`.
11. `monthlyFareAmount` is copied at allocation time and never rewritten in place.
12. The named fee head must exist and allow a transport charge.
13. `startDate` and `endDate` sit inside the academic year, and `startDate` is not
    after `endDate`.

**Trips and boarding**

14. Vehicle and driver fitness is re-checked when the trip starts, not taken from
    the assignment.
15. A `CANCELLED` trip carries a reason.
16. Boarding rows are created with the trip list, one per applicable `ACTIVE`
    allocation, at status `EXPECTED`.
17. `boardedAt` is never after `alightedAt`.
18. `TransportTrip`'s three counts must always be rebuildable from the boarding
    rows.
19. Non-working days come from `AcademicYear.holidays`; no weekday is assumed to
    be a day off.
