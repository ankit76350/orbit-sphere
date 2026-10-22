# controllers/people/leave — API plan

**0 of 10 planned endpoints are built — nothing is built yet.** This is the detailed plan for the **leave package** — the policy, the
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

| Document | Collection | What it holds |
|---|---|---|
| [`LeaveType`](../../../models/people/leave/LeaveType.java) | [`staff_leave_types`](../../../models/people/leave/LeaveType.java) | the **policy** — sick, casual, earned, unpaid, and the annual allowance each gives. Phase **4** |
| [`StaffLeaveBalance`](../../../models/people/leave/StaffLeaveBalance.java) | [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) | the **entitlement** — one per staff per type per year. Phase **4** |
| [`StaffLeaveRequest`](../../../models/people/leave/StaffLeaveRequest.java) | [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) | the **application** — dates, days and the decision. Phase **4** |

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

**What the marker in the `#` column means**, the same four words [`controllers/plans`](../../plans/README.md)
uses:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. 0 of them. |
| **deferred** | Planned, not started, and not next. |
| **optional** | A convenience nobody is waiting on. |
| **not being built** | Decided against, struck through, and the reason is in its own section. |

Anything unmarked does not exist yet, and a request to it returns a 404. Nothing in this package
carries a marker yet, because nothing here is built.

## 1. The policy · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t30"></a>#30 | [`POST /leave-types`](#e30) | Define a leave policy — sick, casual, earned, unpaid. | [`staff_leave_types`](../../../models/people/leave/LeaveType.java) |
| <a id="t31"></a>#31 | [`PATCH /leave-types/{id}`](#e31) | Change next year's allowance. **Never this year's balances.** | [`staff_leave_types`](../../../models/people/leave/LeaveType.java) |
| <a id="t32"></a>#32 | [`GET /leave-types`](#e32) | The policy list. | [`staff_leave_types`](../../../models/people/leave/LeaveType.java) |

## 2. The entitlement · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t33"></a>#33 | [`POST /leave-balances/open-year`](#e33) | Open every balance for a year in one write. **Idempotent, and re-runnable.** | [`staff_leave_types`](../../../models/people/leave/LeaveType.java), [`employment_records`](../../../models/people/staff/EmploymentRecord.java), [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t34"></a>#34 | [`POST /leave-balances/{id}/adjust`](#e34) | Add or remove days **with a reason**. Never a silent edit. | [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t35"></a>#35 | [`GET /staff/{id}/leave-balances`](#e35) | What this person has left, computed. | [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java), [`staff_leave_types`](../../../models/people/leave/LeaveType.java) |

## 3. The application · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t36"></a>#36 | [`POST /staff/{id}/leave-requests`](#e36) | Apply. **Reserves days in the same write.** | [`staff_leave_types`](../../../models/people/leave/LeaveType.java), [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java), [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java), [`number_sequences`](../../../models/institution/NumberSequence.java) |
| <a id="t37"></a>#37 | [`POST /leave-requests/{id}/approve`](#e37) | Pending → used, atomically. | [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java), [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t38"></a>#38 | [`POST /leave-requests/{id}/reject`](#e38) · [`/cancel`](#e38) | Release the reservation. **`cancel` has no status to write** — see [open item 2](#2-there-is-no-cancelled-status-and-38-needs-one). | [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java), [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t39"></a>#39 | [`GET /leave-requests`](#e39) | The approver's queue. | [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java), [`staff`](../../../models/people/staff/Staff.java), [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) |

---

<a id="build-order"></a>
# Build order

| Phase | What it gives you | Endpoints | |
|---|---|---|---|
| **4a** | A policy exists and can be read | 30, 32, 31 | not built |
| **4b** | People have entitlements | 33, 35, 34 | not built |
| **4c** | Leave can be applied for and decided | 36, 39, 37, 38 | not built |

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

<a id="the-endpoints-in-detail"></a>
# Appendix — what every API touches, field by field

## The policy · 30–32

<a id="e30"></a>
**[#30](#t30) · `POST /leave-types`** — planned

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: `leaveTypeCode` — unique per school
- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *insert*: `leaveTypeCode`, `name`, `description`, `annualAllowanceDays`, `paid`, `carryForwardAllowed`, `maximumCarryForwardDays`, `active` = true

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)

{
  "leaveTypeCode": "CASUAL",        // REQUIRED, never changes
  "name": "Casual Leave",           // REQUIRED
  "description": "...",             // optional
  "annualAllowanceDays": 12,        // optional, decimal — model
                                    //   default 0
  "paid": true,                     // optional — model default true
  "carryForwardAllowed": false,     // optional — model default false
  "maximumCarryForwardDays": 0      // optional, decimal — only with
                                    //   carryForwardAllowed: true
}
</pre></td>
<td><pre>
200 OK — the leave type. The response shape
is (not decided); these are the model's fields.

{
  "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
  "leaveTypeCode": "CASUAL",
  "name": "Casual Leave",
  "description": "Short personal absences.",
  "annualAllowanceDays": 12,
  "paid": true,
  "carryForwardAllowed": false,
  "maximumCarryForwardDays": 0,
  "active": true
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `leaveTypeCode` | **yes** | Unique with `schoolId`. Given, not derived. Never changes. |
| `name` | **yes** | Text. |
| `description` | no | Text. |
| `annualAllowanceDays` | no | Decimal. Absent means the model default, `0`. |
| `paid` | no | Absent means the model default, `true`. |
| `carryForwardAllowed` | no | Absent means the model default, `false`. |
| `maximumCarryForwardDays` | no | Decimal. Absent means `0`. Set on a type that does not carry forward is `400 CARRY_FORWARD_NOT_ALLOWED`. |

`active` is not on the request.

**Refusals**

| Status | Code | When |
|---|---|---|
| 409 | `LEAVE_TYPE_CODE_TAKEN` | already this school's |
| 400 | `CARRY_FORWARD_NOT_ALLOWED` | a maximum set on a type that does not carry forward |

`leaveTypeCode` unique per school, given not derived. **`maximumCarryForwardDays` set on a type with `carryForwardAllowed: false` is `400 CARRY_FORWARD_NOT_ALLOWED`**, not silently ignored: a school that configured a 5-day carry-forward and got none would have no way to tell why. `active` is not accepted; it starts `true`.

<a id="e31"></a>
**[#31](#t31) · `PATCH /leave-types/{id}`** — planned

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *updates*: `name`, `description`, `annualAllowanceDays`, `paid`, `carryForwardAllowed`, `maximumCarryForwardDays`, `active`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the leave type's DocsId

{
  "name": "Casual Leave",           // optional
  "description": "...",             // optional
  "annualAllowanceDays": 15,        // optional, decimal
  "paid": true,                     // optional
  "carryForwardAllowed": true,      // optional
  "maximumCarryForwardDays": 5,     // optional, decimal
  "active": false                   // optional — retires the type
}

// Never leaveTypeCode.
</pre></td>
<td><pre>
200 OK — the leave type. The response shape
is (not decided); these are the model's fields.

{
  "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
  "leaveTypeCode": "CASUAL",
  "name": "Casual Leave",
  "description": "Short personal absences.",
  "annualAllowanceDays": 12,
  "paid": true,
  "carryForwardAllowed": false,
  "maximumCarryForwardDays": 0,
  "active": true
}

// The response says that no existing
// balance was changed. Its wording is
// (not decided).
</pre></td>
</tr>
</table>

**Every field on the request.** Absent means "leave it as it is".

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` · `description` | no | Text. |
| `annualAllowanceDays` | no | Decimal. Changes the policy only — never an existing balance. |
| `paid` | no | Boolean. |
| `carryForwardAllowed` · `maximumCarryForwardDays` | no | Boolean · decimal. |
| `active` | no | `false` retires the type. Existing balances are not touched. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `LEAVE_TYPE_NOT_FOUND` | not this school's |
| 400 | `NOTHING_TO_UPDATE` | an empty body — reuses core's code |

`name`, `description`, `annualAllowanceDays`, `paid`, `carryForwardAllowed`, `maximumCarryForwardDays`, `active`. **Never `leaveTypeCode`.**

**Changing the allowance does not touch a single existing balance**, and the response says so. That is the entire reason the two documents are separate: a balance is what the policy gave *at the moment it was opened*, and a mid-year policy change that silently moved everybody's entitlement would measure leave already taken against a number that did not exist when they took it.

<a id="e32"></a>
**[#32](#t32) · `GET /leave-types`** — planned

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: every type, optionally filtered by `active`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)

?active=   optional — true, false, or absent
           for both (tristate)

// No body. Not paged.
</pre></td>
<td><pre>
200 OK — the list (shape not decided)

[
  {
    "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
    "leaveTypeCode": "CASUAL",
    "name": "Casual Leave",
    "annualAllowanceDays": 12,
    "paid": true,
    "carryForwardAllowed": false,
    "maximumCarryForwardDays": 0,
    "active": true
  }
]
</pre></td>
</tr>
</table>

**Every field on the request** — query parameters.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `active` | no | `true` or `false`. Absent means both. |

**Refusals**

| Status | Code | When |
|---|---|---|
| — | — | The plan names no refusal for this read. |

`?active=`, tristate. Not paged; a school has a handful.

## The entitlement · 33–35

<a id="e33"></a>
**[#33](#t33) · `POST /leave-balances/open-year`** — planned

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: every **active** type
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *reads*: who is currently employed
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: last year's, for carry-forward
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *insert*: one balance per employed staff member per active type, for the named `academicYear`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)

{
  "academicYear": "2026-2027"       // REQUIRED — AcademicYear.name,
                                    //   never its id
}
</pre></td>
<td><pre>
200 OK — what was opened and what was
skipped (shape not decided)

{
  "academicYear": "2026-2027",
  "opened": 412,
  "alreadyExisted": 38
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `academicYear` | **yes** | The year's name. The year does not have to exist yet — see below. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 409 | `LEAVE_TYPE_NOT_ACTIVE` | a retired policy |
| *not an error* | `BALANCE_ALREADY_OPEN` | idempotent by the unique key; re-running reports what it skipped |

- **Idempotent by the unique key** `{schoolId, academicYear, staffDocsId, leaveTypeDocsId}`. Running it twice adds nothing — **which is what makes it safe to re-run after hiring somebody mid-year**, and that is the normal case rather than an edge one.
- **The response reports what it skipped**, not just what it wrote. "412 opened, 38 already existed" is the difference between a successful re-run and a caller wondering whether it worked.
- **Carry-forward is capped by the policy**, and computed from last year's `available` — not from `allocatedDays`. Somebody who used everything carries nothing forward.
- **Only currently-employed staff get balances.** A leaver does not accrue entitlement, and [#17](../staff/README.md#e17) is what makes that knowable.
- **Not gated on the year existing** — see the gates note above. A school opens next year's balances in March.

<a id="e34"></a>
**[#34](#t34) · `POST /leave-balances/{id}/adjust`** — planned

- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *updates*: `adjustmentDays` — **added to, never replaced**

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the balance's DocsId

{
  "adjustmentDays": -2,             // REQUIRED, decimal, signed —
                                    //   the field name is (not decided)
  "reason": "Over-granted in June"  // REQUIRED
}
</pre></td>
<td><pre>
200 OK — the balance (shape not decided)

{
  "staffLeaveBalanceDocsId": "6b2b0a2c3d4e5f6012345678",
  "staffDocsId": "6b0e9a1b2c3d4e5f60123456",
  "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
  "academicYear": "2026-2027",
  "allocatedDays": 12,
  "carriedForwardDays": 0,
  "adjustmentDays": 0,
  "usedDays": 4,
  "pendingDays": 1,
  "available": 7
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| days to adjust | **yes** | Signed decimal. Positive adds, negative removes. May take `available` below zero. |
| `reason` | **yes** | Text. Absent is `400 ADJUSTMENT_REASON_REQUIRED`. Where it is stored is (not decided). |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `LEAVE_BALANCE_NOT_FOUND` | not this school's |
| 400 | `ADJUSTMENT_REASON_REQUIRED` | an adjustment without a reason |

- **A reason is required** — `400 ADJUSTMENT_REASON_REQUIRED`. An adjustment is a decision somebody made, and a signed number with no explanation is unauditable the moment the person who made it leaves.
- **Never touches `allocatedDays`.** The allocation is what the policy gave; an adjustment is what a human decided afterwards, and collapsing the two loses which is which.
- **Signed**: positive adds, negative removes. **A negative adjustment may take `available` below zero**, and that is allowed — a school correcting an over-grant needs to be able to, and refusing it would leave the error in place.
- **Where the reason is stored is an open question**: the model has no field for it. It wants either a field on the balance or an audit row, and this project has no audit writer — the same gap `controllers/core` records for its results-unlock endpoint.

<a id="e35"></a>
**[#35](#t35) · `GET /staff/{id}/leave-balances`** — planned

- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: every balance for one person, for `?academicYear=` or the running one
- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: every active leave type, so a zero balance still shows

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the staff member's DocsId

?academicYear=   optional — absent means the
                 running year

// No body.
</pre></td>
<td><pre>
200 OK — one row per active leave type
(shape not decided)

[
  {
    "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
    "leaveTypeName": "Casual Leave",
    "academicYear": "2026-2027",
    "allocatedDays": 12,
    "carriedForwardDays": 0,
    "adjustmentDays": 2,
    "usedDays": 4,
    "pendingDays": 1,
    "available": 9       // computed, never stored
  }
]
</pre></td>
</tr>
</table>

**Every field on the request** — query parameters.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `academicYear` | no | The year's name. Absent means the running year. |

**Refusals**

| Status | Code | When |
|---|---|---|
| — | — | The plan names no refusal for this read. |

- **`available` is computed on every read**, from the five stored inputs. Never stored, for the reason a grading scheme's gap warning is never stored: it would go stale the instant a request was approved, and somebody would book against it.
- **Returns every active leave type**, including ones with a zero balance, so a staff member can see they have no sick leave left rather than seeing nothing and wondering.
- **No gates**, and no authorization yet — which for somebody's leave record is one of the sharper cases of [the domain's open item 2](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization).

## The application · 36–39

<a id="e36"></a>
**[#36](#t36) · `POST /staff/{id}/leave-requests`** — planned

- [`staff_leave_types`](../../../models/people/leave/LeaveType.java) — *reads*: `active` — a retired policy is refused
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: the balance, by the four-part key
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *updates*: `pendingDays` += the requested days
- [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) — *insert*: `requestNo` **generated**, status `SUBMITTED`, `staffLeaveBalanceDocsId` linked
- [`number_sequences`](../../../models/institution/NumberSequence.java) — *updates*: the `STAFF_LEAVE_REQUEST` counter, to generate `requestNo`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the staff member's DocsId

{
  "leaveTypeDocsId": "6b2a...",     // REQUIRED
  "academicYear": "2026-2027",      // REQUIRED
  "fromDate": "2026-11-03",         // REQUIRED
  "toDate": "2026-11-05",           // REQUIRED, inclusive
  "requestedDays": 3,               // decimal — sent or computed is
                                    //   (not decided), open item 3
  "reason": "Family wedding",       // optional
  "coverRequired": true,            // optional — model default false
  "coverStaffDocsId": "6b0e...",    // optional
  "evidenceDocumentDocsIds": []     // optional
}
</pre></td>
<td><pre>
200 OK — the request (shape not decided)

{
  "staffLeaveRequestDocsId": "6b2c0a2c3d4e5f6012345678",
  "requestNo": "LR-0031",
  "staffDocsId": "6b0e9a1b2c3d4e5f60123456",
  "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
  "staffLeaveBalanceDocsId": "6b2b0a2c3d4e5f6012345678",
  "academicYear": "2026-2027",
  "fromDate": "2026-11-03",
  "toDate": "2026-11-05",
  "requestedDays": 3,
  "reason": "Family wedding",
  "status": "SUBMITTED",
  "coverRequired": true,
  "coverStaffDocsId": "6b0e9a1b2c3d4e5f60bbbbbb",
  "evidenceDocumentDocsIds": [],
  "submittedAt": "2026-10-22T09:15:00Z",
  "decidedByDocsId": null,
  "decidedAt": null,
  "decisionNote": null
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `leaveTypeDocsId` | **yes** | An active leave type. |
| `academicYear` | **yes** | The year's name. Picks the balance, and the calendar for working days. |
| `fromDate` · `toDate` | **yes** | Dates, inclusive both ends. `toDate` before `fromDate` is `400 INVALID_LEAVE_RANGE`. |
| `requestedDays` | (not decided) | Decimal, so `0.5` works. See [open item 3](#3-requesteddays-is-sent-not-computed--and-nothing-knows-about-holidays). |
| `reason` | no | Text. |
| `coverRequired` | no | Absent means `false`. |
| `coverStaffDocsId` | no | Who covers the absence. Recorded, never computed. |
| `evidenceDocumentDocsIds` | no | A list of document DocsIds. Absent means none. |

`requestNo`, `status` and the decision fields are not on the request.

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `LEAVE_BALANCE_NOT_FOUND` | not this school's |
| 409 | `LEAVE_TYPE_NOT_ACTIVE` | a retired policy |
| 400 | `INVALID_LEAVE_RANGE` | `toDate` before `fromDate` |
| 409 | `INSUFFICIENT_LEAVE_BALANCE` | **names how many days are available** |

- **The reservation happens in the same call as the request.** A request that exists without reserving days is a request that can be approved twice over the same entitlement.
- **`409 INSUFFICIENT_LEAVE_BALANCE` names how many days are available**, because "you have 2 left" is actionable and "insufficient" is not.
- **`staffLeaveBalanceDocsId` is stored on the request**, so [#37](#e37) does not re-derive the balance from four fields under concurrency — which is where a race would otherwise live.
- **The balance is written first**, then the request. See [open item 1](#1-two-documents-must-move-together-and-there-are-no-transactions): if the second write fails, days are over-reserved rather than double-spent, which is visible on [#35](#e35) and fixable with [#34](#e34).
- **`requestedDays` — see [open item 3](#3-requesteddays-is-sent-not-computed--and-nothing-knows-about-holidays)**, which must be settled before this is built.

<a id="e37"></a>
**[#37](#t37) · `POST /leave-requests/{id}/approve`** — planned

- [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) — *updates*: the request — `status` = `APPROVED`, `decidedByDocsId`, `decidedAt`, `decisionNote`
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *updates*: the balance — `pendingDays` −= days, `usedDays` += days

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the leave request's DocsId

{
  "decisionNote": "Approved."       // optional — whether it is
                                    //   required is (not decided)
}

// Where decidedByDocsId comes from is
// (not decided).
</pre></td>
<td><pre>
200 OK — the request (shape not decided)

{
  "staffLeaveRequestDocsId": "6b2c0a2c3d4e5f6012345678",
  "requestNo": "LR-0031",
  "staffDocsId": "6b0e9a1b2c3d4e5f60123456",
  "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
  "staffLeaveBalanceDocsId": "6b2b0a2c3d4e5f6012345678",
  "academicYear": "2026-2027",
  "fromDate": "2026-11-03",
  "toDate": "2026-11-05",
  "requestedDays": 3,
  "reason": "Family wedding",
  "status": "APPROVED",
  "coverRequired": true,
  "coverStaffDocsId": "6b0e9a1b2c3d4e5f60bbbbbb",
  "evidenceDocumentDocsIds": [],
  "submittedAt": "2026-10-22T09:15:00Z",
  "decidedByDocsId": null,
  "decidedAt": null,
  "decisionNote": null
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `decisionNote` | (not decided) | Text. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `LEAVE_REQUEST_NOT_FOUND` | not this school's |
| 409 | `LEAVE_REQUEST_NOT_PENDING` | deciding one already decided |

- **The days move between two fields of one document**, which is the one part of this that is genuinely atomic. The risk is the *request* and the *balance* diverging, not the two fields.
- **`409 LEAVE_REQUEST_NOT_PENDING`** for anything not `SUBMITTED`. **Not idempotent**, deliberately, unlike the lock/retire pairs elsewhere in this project: "it was already approved" and "you just approved it" are different facts, and a caller who cannot tell them apart will assume the wrong one about somebody's entitlement.
- **Approving future leave still moves it to `usedDays`** — the model README is explicit, and it is right: leave booked for March is spent in November, or two people book the same last day.

<a id="e38"></a>
**[#38](#t38) · `POST /leave-requests/{id}/reject` · `/cancel`** — planned

- [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) — *updates*: the request's status and decision fields
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *updates*: the balance's `pendingDays` −= days

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)
Path:    {id}  the leave request's DocsId

POST /leave-requests/{id}/reject
{
  "decisionNote": "Exam week."      // REQUIRED on reject
}

POST /leave-requests/{id}/cancel
{
  "decisionNote": "..."             // optional on cancel
}

// The status cancel writes does not exist
// yet — see open item 2.
</pre></td>
<td><pre>
200 OK — the request (shape not decided)

{
  "staffLeaveRequestDocsId": "6b2c0a2c3d4e5f6012345678",
  "requestNo": "LR-0031",
  "status": "REJECTED",
  "decidedByDocsId": "6b0e9a1b2c3d4e5f60aaaaaa",
  "decidedAt": "2026-10-23T10:00:00Z",
  "decisionNote": "Exam week."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `decisionNote` | **yes** on reject, no on cancel | Text. The refusal code for a reject without one is (not decided). |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `LEAVE_REQUEST_NOT_FOUND` | not this school's |
| 409 | `LEAVE_REQUEST_NOT_PENDING` | deciding one already decided |

- **Two endpoints, not one with a flag**, because they are different events by different people at different times — see [open item 2](#2-there-is-no-cancelled-status-and-38-needs-one), which must be settled first: **there is no `CANCELLED` status to write.**
- **Cancelling an `APPROVED` request returns `usedDays`, not `pendingDays`.** A second reason the two cannot share a code path, and the case that makes cancel-after-approval worth supporting at all.
- **Rejection needs a `decisionNote`**; cancellation does not. The approver owes an explanation and the applicant does not.

<a id="e39"></a>
**[#39](#t39) · `GET /leave-requests`** — planned

- [`staff_leave_requests`](../../../models/people/leave/StaffLeaveRequest.java) — *reads*: one page, filtered
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: the applicant's name, for each row
- [`staff_leave_balances`](../../../models/people/leave/StaffLeaveBalance.java) — *reads*: the applicant's remaining balance — one grouped read for the page

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Cookie:  idtoken=…  (the school is read from the sign-in cookie)

?status=           optional — default SUBMITTED
?staffDocsId=      optional
?leaveTypeDocsId=  optional
?from= ?to=        optional — requests that
                   overlap the range
?academicYear=     optional

// No body. Oldest first. Page parameters
// are (not decided).
</pre></td>
<td><pre>
200 OK — one page (shape not decided)

{
  "rows": [
    {
      "staffLeaveRequestDocsId": "6b2c0a2c3d4e5f6012345678",
      "requestNo": "LR-0031",
      "staffDocsId": "6b0e9a1b2c3d4e5f60123456",
      "staffName": "Priya Sharma",
      "leaveTypeDocsId": "6b2a0a2c3d4e5f6012345678",
      "fromDate": "2026-11-03",
      "toDate": "2026-11-05",
      "requestedDays": 3,
      "status": "SUBMITTED",
      "submittedAt": "2026-10-22T09:15:00Z",
      "available": 9
    }
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request** — query parameters.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `status` | no | A `LeaveRequestStatus` value. Absent means `SUBMITTED`. |
| `staffDocsId` | no | One applicant. Absent means everyone. |
| `leaveTypeDocsId` | no | One leave type. Absent means every type. |
| `from` · `to` | no | Dates. Matches requests that overlap the range. |
| `academicYear` | no | The year's name. Absent means every year. |

**Refusals**

| Status | Code | When |
|---|---|---|
| — | — | The plan names no refusal for this read. |

- **The approver's queue**, and `school_leave_decision_queue_idx` on `{schoolId, status, submittedAt}` exists for exactly this — default `?status=SUBMITTED`, oldest first, so the longest-waiting request is the first one seen.
- **Filters**: `?status=`, `?staffDocsId=`, `?leaveTypeDocsId=`, `?from=`/`?to=` overlapping a range, `?academicYear=`.
- **The row carries the applicant's name and remaining balance**, because an approver deciding without knowing what is left is deciding blind — and it costs one grouped read for the page rather than one per row.
- **No gates.** A suspended school still reads its own queue.
