# controllers/people/leave — API plan

**Nothing is built.** This is the detailed plan for the **leave package** — the policy, the
entitlement and the application. It expands the group that [`controllers/people`](../README.md)
lists.

> **Numbers are the domain's, not this file's.** `#30` here is `#30` there — the numbers run
> `30–39`.

Mirrors [`models/people/leave`](../../../models/people/leave) — three documents, no encryption.

> **This is the package with the hardest correctness problem in the module**, and it is not the
> arithmetic. Every write here moves days between two documents that must agree, and **this project
> configures no MongoDB transaction manager**. See
> [open item 1](#1-two-documents-must-move-together-and-there-are-no-transactions).

---

## What this package is

**Three documents, and each is a different kind of thing.**

```text
LeaveType  "Casual Leave"  CASUAL          the POLICY
  annualAllowanceDays 12 · paid · carryForwardAllowed false
  │
  └── StaffLeaveBalance                    the ENTITLEMENT — one per staff per type per year
        allocated 12 · carriedForward 0 · adjustment +2
        used 4 · pending 1
        available = 12 + 0 + 2 - 4 - 1 = 9      <- never stored
        │
        └── StaffLeaveRequest  LR-0031      the APPLICATION
              2026-11-03 → 2026-11-05 · 3 days · SUBMITTED
```

| Document | Collection | Phase |
|---|---|---|
| [`LeaveType`](../../../models/people/leave/LeaveType.java) | `staff_leave_types` | **4** |
| [`StaffLeaveBalance`](../../../models/people/leave/StaffLeaveBalance.java) | `staff_leave_balances` | **4** |
| [`StaffLeaveRequest`](../../../models/people/leave/StaffLeaveRequest.java) | `staff_leave_requests` | **4** |

### The policy and the entitlement are separate on purpose

**Changing `LeaveType.annualAllowanceDays` must not move anybody's balance.** A balance is opened
*from* the policy once, by [#33](#e33), and then belongs to the year.

A school that raises casual leave from 12 to 15 days in November is deciding about *next* year. If
the policy fed the balance live, every staff member's entitlement would silently change mid-year,
and the leave they had already taken would be measured against a number that did not exist when
they took it.

**This is the same separation `AcademicTerm` has from `AcademicYear`, and the same one a grading
scheme has from a report card**: the rule is copied at the moment it applies, and the copy is what
history is measured against.

### `available` is arithmetic, and the model stores only the inputs

```text
available = allocatedDays + carriedForwardDays + adjustmentDays − usedDays − pendingDays
```

Five stored fields, one derivation, and **nothing stores `available`** — which is right. It would
go stale the instant a request was approved, and a stale entitlement is worse than a computed one
because somebody books against it.

[#35](#e35) computes it on every read, exactly as [#7](../../academics/grading/README.md#e7) of the
grading module recomputes its gap warning rather than storing it.

---

## Which gates every endpoint runs

| | Gates |
|---|---|
| every **write** here | 1 school is live · 2 school is paying |
| every **read** here | none |

**No gate 4, even though balances carry an `academicYear`.** The year is in the *body*, not the
path — and [#33](#e33) opens next year's balances *before* that year starts, which is exactly the
call gate 4 would refuse. `controllers/core` documents that argument in full.

---

# The endpoints

## 1. The policy

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t30"></a>30 | [`POST /leave-types`](#e30) | Define a leave policy — sick, casual, earned, unpaid. |
| <a id="t31"></a>31 | [`PATCH /leave-types/{id}`](#e31) | Change next year's allowance. **Never this year's balances.** |
| <a id="t32"></a>32 | [`GET /leave-types`](#e32) | The policy list. |

## 2. The entitlement

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t33"></a>33 | [`POST /leave-balances/open-year`](#e33) | Open every balance for a year in one write. **Idempotent, and re-runnable.** |
| <a id="t34"></a>34 | [`POST /leave-balances/{id}/adjust`](#e34) | Add or remove days **with a reason**. Never a silent edit. |
| <a id="t35"></a>35 | [`GET /staff/{id}/leave-balances`](#e35) | What this person has left, computed. |

## 3. The application

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t36"></a>36 | [`POST /staff/{id}/leave-requests`](#e36) | Apply. **Reserves days in the same write.** |
| <a id="t37"></a>37 | [`POST /leave-requests/{id}/approve`](#e37) | Pending → used, atomically. |
| <a id="t38"></a>38 | [`POST /leave-requests/{id}/reject`](#e38) · [`/cancel`](#e38) | Release the reservation. **`cancel` has no status to write** — see [open item 2](#2-there-is-no-cancelled-status-and-38-needs-one). |
| <a id="t39"></a>39 | [`GET /leave-requests`](#e39) | The approver's queue. |

---

# Build order

| Phase | What it gives you | Endpoints |
|---|---|---|
| **4a** | A policy exists and can be read | 30, 32, 31 |
| **4b** | People have entitlements | 33, 35, 34 |
| **4c** | Leave can be applied for and decided | 36, 39, 37, 38 |

**The whole package is phase 4** — nothing outside `people` references any of it, so it blocks
nothing and can wait until staff and employment are solid.

**4b before 4c, strictly.** [#36](#e36) writes to a balance; there is nothing to write to until
[#33](#e33) has run.

**[#39](#e39) before [#37](#e37).** The queue is how you find a request to approve, and building
the decision first means testing it by guessing ids.

**[#37](#e37) and [#38](#e38) last, and together.** They are the same problem in two directions —
move days between two documents without losing them — and building one without the other means
solving it twice.

---

# Things this package deliberately will not have

- **No `DELETE` on a leave type.** Balances and requests reference it by id, and a deleted policy
  leaves a staff member's history naming nothing. Retiring is `active` on [#31](#e31).
- **No balance edit.** [#34](#e34) *adjusts*, with a reason, and the adjustment is a separate field
  from the allocation. A PATCH that set `allocatedDays` directly would let somebody quietly rewrite
  what the policy gave, and the only record would be the number itself.
- **No leave calendar, and no clash detection.** "Is anybody else off that week" is a real question
  and a different endpoint: it spans every staff member, wants a date range rather than a person,
  and belongs wherever the school's calendar lives. `coverStaffDocsId` records the answer a human
  reached; it does not compute one.
- **No half-day arithmetic in the model.** `requestedDays` is a `BigDecimal`, so `0.5` stores
  cleanly — but nothing computes it from `fromDate` and `toDate`, and nothing knows about weekends
  or the school's holiday calendar. See [open item 3](#3-requesteddays-is-sent-not-computed--and-nothing-knows-about-holidays).
- **No accrual.** Leave is allocated annually by [#33](#e33), not earned monthly. `earned` leave in
  the policy list is a *name*, not a mechanism.

---

# To settle before building

## 1. Two documents must move together, and there are no transactions

Every one of [#36](#e36), [#37](#e37) and [#38](#e38) writes **a request and a balance**:

```text
#36 apply     request DRAFT→SUBMITTED   balance pendingDays  += requested
#37 approve   request →APPROVED         balance pendingDays  -= requested
                                                usedDays     += requested
#38 reject    request →REJECTED         balance pendingDays  -= requested
```

**If the second write fails, the days are lost or double-counted**, and nothing in the data says
which. The model README says to use *"a MongoDB transaction or equivalent atomic workflow"*.

**This project has no transaction manager configured for Mongo.** `@Transactional` appears on
several services and currently does nothing — which has been harmless everywhere else, because
nothing else writes two documents that must agree.

Two ways:

- **a) Configure `MongoTransactionManager`.** Correct, and it needs a replica set — Atlas provides
  one, a bare local `mongod` does not, so it changes what a developer needs running.
- **b) Optimistic locking on the balance.** `AuditedDocument` already carries `@Version`, so a
  conditional update that fails on a concurrent change is available today with no infrastructure.
  Write the balance **first**, then the request; if the request write fails, the balance is wrong
  in the *safe* direction — days reserved that nobody booked, visible on [#35](#e35) and fixable
  with [#34](#e34).

**Recommendation: (b) for the first build, (a) before real use.** The version field is there for
exactly this, and ordering the writes so a failure over-reserves rather than under-reserves means
the worst case is a staff member who has to ask for two days back — not two days taken twice.

**Decide before [#36](#e36).** It is the first endpoint that touches both.

## 2. There is no `CANCELLED` status, and #38 needs one

```java
public enum LeaveRequestStatus { DRAFT, SUBMITTED, APPROVED, REJECTED }
```

**Four values, and cancelling is not one of them.** [#38](#e38) is specified as reject *and* cancel,
and they are genuinely different events:

| | who does it | when | what it means |
|---|---|---|---|
| reject | the approver | before the leave | "no" |
| cancel | the staff member | before **or after** approval | "I no longer need it" |

Writing a cancellation as `REJECTED` loses that distinction permanently, and a staff member whose
own withdrawal shows as a rejection on their record has a legitimate complaint.

**This is a model gap, not a plan one.** Three options:

- **a) Add `CANCELLED` to the enum.** One line, and the right answer.
- **b) Add a `cancelledAt` instant beside the status.** Preserves the enum and makes every reader
  check two fields.
- **c) Refuse to cancel an approved request**, and let cancellation only mean "withdraw a draft" —
  which real schools will not accept.

**Recommendation: (a), and it should be done before [#36](#e36) is built** so no request is ever
written under the wrong vocabulary. Cancelling an *approved* request also has to return
`usedDays`, not `pendingDays` — a second reason the two cannot share a code path.

## 3. `requestedDays` is sent, not computed — and nothing knows about holidays

`StaffLeaveRequest` carries `fromDate`, `toDate` **and** `requestedDays`, and nothing reconciles
them. A request for 3 November to 5 November with `requestedDays: 10` stores cleanly.

**Computing it is not as simple as a date subtraction.** It has to skip weekends, school holidays
and the school's own closed days — all of which live on
[`AcademicYear.holidays`](../../../models/core/AcademicYear.java), in a different module, and
`controllers/core` already has `GET .../working-days` that answers exactly this.

Three ways:

- **a) Trust the caller.** Simplest, and lets a client's off-by-one become an entitlement error.
- **b) Compute it and ignore what was sent**, reusing core's working-days count. Correct, and
  couples leave to the academic-year calendar — which is arguably where it belongs.
- **c) Compute it, compare, and refuse a mismatch.** Pedantic, and the error message is useful
  precisely when a client is wrong.

**Recommendation: (b)**, reusing `Dates` and the year's holiday calendar rather than reimplementing
working days a second time. The endpoint then returns the computed figure and the caller learns it
from the response — and half-days stay possible because `BigDecimal` allows the caller to override
downward with a reason.

**Which academic year's calendar?** The request carries `academicYear` already, for exactly this.

---

# Where the code will live

```text
controllers/people/leave/
├── README.md                <- this file
└── LeaveController.java     #30–#39

services/people/
├── LeaveService.java
└── helper/LeaveHelper.java  the balance arithmetic, and the one place it lives

repositories/people/leave/
├── LeaveTypeRepository.java
├── StaffLeaveBalanceRepository.java
└── StaffLeaveRequestRepository.java   + Custom/Impl for #39's queue

dto/people/leave/{request,response}/
```

**`LeaveHelper`, not `PeopleHelper`.** The staff package's helper takes staff and employment
records; this one takes balances and does arithmetic. A helper that validated two unrelated
documents would be a folder, not a class — the same reasoning that gave grading its own
`GradingHelper` rather than a section of `AcademicsHelper`.

**The `available` computation lives there and nowhere else.** Three endpoints need it —
[#35](#e35), [#36](#e36) and [#39](#e39) — and three copies of a five-term subtraction is three
chances to get a sign wrong.

---

# Appendix — what each field can hold

## `staff_leave_types` — [LeaveType](../../../models/people/leave/LeaveType.java)

| Field | Type | What can be in it |
|---|---|---|
| `leaveTypeCode` | String, required | Unique with `schoolId`. Given, not derived. **Never changes.** |
| `name` · `description` | String | Editable. |
| `annualAllowanceDays` | BigDecimal, required | Defaults to `0`. **What [#33](#e33) copies into a balance**, and nothing else reads it. |
| `paid` | Boolean, required | Defaults `true`. Payroll's to consume; this module only records it. |
| `carryForwardAllowed` | Boolean, required | Defaults `false`. |
| `maximumCarryForwardDays` | BigDecimal, required | Defaults `0`. **Meaningless unless `carryForwardAllowed`** — and nothing enforces that pairing, so [#30](#e30) does. |
| `active` | Boolean, required | Retiring a type does not touch existing balances. |

**Index:** `school_leave_type_code_uniq {schoolId, leaveTypeCode}` unique ·
`school_leave_type_active_name_idx {schoolId, active, name}`

## `staff_leave_balances` — [StaffLeaveBalance](../../../models/people/leave/StaffLeaveBalance.java)

| Field | Type | What can be in it |
|---|---|---|
| `staffDocsId` · `leaveTypeDocsId` · `academicYear` | String | **The key**, with `schoolId`. `academicYear` is `AcademicYear.name`, never its id — the project-wide rule. |
| `allocatedDays` | BigDecimal | What the policy gave, copied once by [#33](#e33). **Never edited.** |
| `carriedForwardDays` | BigDecimal | What last year left, capped by `maximumCarryForwardDays`. |
| `adjustmentDays` | BigDecimal | Signed. **Positive adds, negative removes** — [#34](#e34), with a reason. |
| `usedDays` | BigDecimal | Approved leave, **including approved future leave**. |
| `pendingDays` | BigDecimal | Awaiting a decision, and nothing else. |
| `version` | Long, inherited | **The concurrency control** — see [open item 1](#1-two-documents-must-move-together-and-there-are-no-transactions). |

**Index:** `school_year_staff_leave_type_uniq {schoolId, academicYear, staffDocsId, leaveTypeDocsId}`
unique — **which is what makes [#33](#e33) idempotent.**

## `staff_leave_requests` — [StaffLeaveRequest](../../../models/people/leave/StaffLeaveRequest.java)

| Field | Type | What can be in it |
|---|---|---|
| `requestNo` | String, required | **Generated** — `NumberSequenceType.STAFF_LEAVE_REQUEST`, which is already declared and whose service is already built. |
| `staffDocsId` · `leaveTypeDocsId` · `academicYear` | String | Who, what, when. |
| `staffLeaveBalanceDocsId` | String | **The direct link**, so [#37](#e37) does not have to re-derive the balance from three fields under concurrency. |
| `fromDate` · `toDate` | LocalDate | Inclusive both ends. |
| `requestedDays` | BigDecimal | See [open item 3](#3-requesteddays-is-sent-not-computed--and-nothing-knows-about-holidays). |
| `reason` | String | |
| `status` | Enum, required | `DRAFT` · `SUBMITTED` · `APPROVED` · `REJECTED`. **No `CANCELLED`** — [open item 2](#2-there-is-no-cancelled-status-and-38-needs-one). |
| `coverRequired` · `coverStaffDocsId` | Boolean · String | Who covers the absence. Recorded, never computed. |
| `evidenceDocumentDocsIds` | List | Medical certificates and the like. |
| `submittedAt` · `decidedByDocsId` · `decidedAt` · `decisionNote` | | The audit trail this document keeps for itself. |

**Index:** `school_leave_request_no_uniq` unique ·
`school_staff_leave_dates_status_idx` · `school_leave_decision_queue_idx {schoolId, status, submittedAt}`
— **the last one is [#39](#e39)'s whole reason for being fast.**

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `LEAVE_TYPE_NOT_FOUND` · `LEAVE_BALANCE_NOT_FOUND` · `LEAVE_REQUEST_NOT_FOUND` | 404 | not this school's |
| `LEAVE_TYPE_CODE_TAKEN` | 409 | already this school's |
| `CARRY_FORWARD_NOT_ALLOWED` | 400 | [#30](#e30) — a maximum set on a type that does not carry forward |
| `LEAVE_TYPE_NOT_ACTIVE` | 409 | [#33](#e33), [#36](#e36) — a retired policy |
| `BALANCE_ALREADY_OPEN` | *not an error* | [#33](#e33) — idempotent by the unique key; re-running reports what it skipped |
| `ADJUSTMENT_REASON_REQUIRED` | 400 | [#34](#e34) — an adjustment without a reason is an unexplained number |
| `INVALID_LEAVE_RANGE` | 400 | `toDate` before `fromDate` |
| `INSUFFICIENT_LEAVE_BALANCE` | 409 | [#36](#e36) — **names how many days are available** |
| `LEAVE_REQUEST_NOT_PENDING` | 409 | [#37](#e37), [#38](#e38) — deciding one already decided |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

## The policy · 30–32

<a id="e30"></a>
**[30](#t30) · `POST /leave-types`** — `leaveTypeCode` unique per school, given not derived. **`maximumCarryForwardDays` set on a type with `carryForwardAllowed: false` is `400 CARRY_FORWARD_NOT_ALLOWED`**, not silently ignored: a school that configured a 5-day carry-forward and got none would have no way to tell why. `active` is not accepted; it starts `true`.

<a id="e31"></a>
**[31](#t31) · `PATCH /leave-types/{id}`** — `name`, `description`, `annualAllowanceDays`, `paid`, `carryForwardAllowed`, `maximumCarryForwardDays`, `active`. **Never `leaveTypeCode`.**

**Changing the allowance does not touch a single existing balance**, and the response says so. That is the entire reason the two documents are separate: a balance is what the policy gave *at the moment it was opened*, and a mid-year policy change that silently moved everybody's entitlement would measure leave already taken against a number that did not exist when they took it.

<a id="e32"></a>
**[32](#t32) · `GET /leave-types`** — `?active=`, tristate. Not paged; a school has a handful.

## The entitlement · 33–35

<a id="e33"></a>
**[33](#t33) · `POST /leave-balances/open-year`**

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: every **active** type
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *reads*: who is currently employed
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: last year's, for carry-forward
- *insert*: one balance per employed staff member per active type, for the named `academicYear`
- **Idempotent by the unique key** `{schoolId, academicYear, staffDocsId, leaveTypeDocsId}`. Running it twice adds nothing — **which is what makes it safe to re-run after hiring somebody mid-year**, and that is the normal case rather than an edge one.
- **The response reports what it skipped**, not just what it wrote. "412 opened, 38 already existed" is the difference between a successful re-run and a caller wondering whether it worked.
- **Carry-forward is capped by the policy**, and computed from last year's `available` — not from `allocatedDays`. Somebody who used everything carries nothing forward.
- **Only currently-employed staff get balances.** A leaver does not accrue entitlement, and [#17](../staff/README.md#e17) is what makes that knowable.
- **Not gated on the year existing** — see the gates note above. A school opens next year's balances in March.

<a id="e34"></a>
**[34](#t34) · `POST /leave-balances/{id}/adjust`**

- *updates*: `adjustmentDays` — **added to, never replaced**
- **A reason is required** — `400 ADJUSTMENT_REASON_REQUIRED`. An adjustment is a decision somebody made, and a signed number with no explanation is unauditable the moment the person who made it leaves.
- **Never touches `allocatedDays`.** The allocation is what the policy gave; an adjustment is what a human decided afterwards, and collapsing the two loses which is which.
- **Signed**: positive adds, negative removes. **A negative adjustment may take `available` below zero**, and that is allowed — a school correcting an over-grant needs to be able to, and refusing it would leave the error in place.
- **Where the reason is stored is an open question**: the model has no field for it. It wants either a field on the balance or an audit row, and this project has no audit writer — the same gap `controllers/core` records for its results-unlock endpoint.

<a id="e35"></a>
**[35](#t35) · `GET /staff/{id}/leave-balances`**

- *reads*: every balance for one person, for `?academicYear=` or the running one
- **`available` is computed on every read**, from the five stored inputs. Never stored, for the reason a grading scheme's gap warning is never stored: it would go stale the instant a request was approved, and somebody would book against it.
- **Returns every active leave type**, including ones with a zero balance, so a staff member can see they have no sick leave left rather than seeing nothing and wondering.
- **No gates**, and no authorization yet — which for somebody's leave record is one of the sharper cases of [the domain's open item 2](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization).

## The application · 36–39

<a id="e36"></a>
**[36](#t36) · `POST /staff/{id}/leave-requests`**

- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: the balance, by the four-part key
- *updates*: `pendingDays` += the requested days
- [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) — *insert*: `requestNo` **generated**, status `SUBMITTED`, `staffLeaveBalanceDocsId` linked
- **The reservation happens in the same call as the request.** A request that exists without reserving days is a request that can be approved twice over the same entitlement.
- **`409 INSUFFICIENT_LEAVE_BALANCE` names how many days are available**, because "you have 2 left" is actionable and "insufficient" is not.
- **`staffLeaveBalanceDocsId` is stored on the request**, so [#37](#e37) does not re-derive the balance from four fields under concurrency — which is where a race would otherwise live.
- **The balance is written first**, then the request. See [open item 1](#1-two-documents-must-move-together-and-there-are-no-transactions): if the second write fails, days are over-reserved rather than double-spent, which is visible on [#35](#e35) and fixable with [#34](#e34).
- **`requestedDays` — see [open item 3](#3-requesteddays-is-sent-not-computed--and-nothing-knows-about-holidays)**, which must be settled before this is built.

<a id="e37"></a>
**[37](#t37) · `POST /leave-requests/{id}/approve`**

- *updates*: the request — `status` = `APPROVED`, `decidedByDocsId`, `decidedAt`, `decisionNote`
- *updates*: the balance — `pendingDays` −= days, `usedDays` += days
- **The days move between two fields of one document**, which is the one part of this that is genuinely atomic. The risk is the *request* and the *balance* diverging, not the two fields.
- **`409 LEAVE_REQUEST_NOT_PENDING`** for anything not `SUBMITTED`. **Not idempotent**, deliberately, unlike the lock/retire pairs elsewhere in this project: "it was already approved" and "you just approved it" are different facts, and a caller who cannot tell them apart will assume the wrong one about somebody's entitlement.
- **Approving future leave still moves it to `usedDays`** — the model README is explicit, and it is right: leave booked for March is spent in November, or two people book the same last day.

<a id="e38"></a>
**[38](#t38) · `POST /leave-requests/{id}/reject` · `/cancel`**

- *updates*: the request's status and decision fields; the balance's `pendingDays` −= days
- **Two endpoints, not one with a flag**, because they are different events by different people at different times — see [open item 2](#2-there-is-no-cancelled-status-and-38-needs-one), which must be settled first: **there is no `CANCELLED` status to write.**
- **Cancelling an `APPROVED` request returns `usedDays`, not `pendingDays`.** A second reason the two cannot share a code path, and the case that makes cancel-after-approval worth supporting at all.
- **Rejection needs a `decisionNote`**; cancellation does not. The approver owes an explanation and the applicant does not.

<a id="e39"></a>
**[39](#t39) · `GET /leave-requests`**

- *reads*: one page, filtered
- **The approver's queue**, and `school_leave_decision_queue_idx` on `{schoolId, status, submittedAt}` exists for exactly this — default `?status=SUBMITTED`, oldest first, so the longest-waiting request is the first one seen.
- **Filters**: `?status=`, `?staffDocsId=`, `?leaveTypeDocsId=`, `?from=`/`?to=` overlapping a range, `?academicYear=`.
- **The row carries the applicant's name and remaining balance**, because an approver deciding without knowing what is left is deciding blind — and it costs one grouped read for the page rather than one per row.
- **No gates.** A suspended school still reads its own queue.
