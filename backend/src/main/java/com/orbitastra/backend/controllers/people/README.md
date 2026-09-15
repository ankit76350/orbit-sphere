# controllers/people — API plan

**One of 51 is built — [#9](#t9), `POST /departments`.** This file is the full set of endpoints
the people feature needs, written
before any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md), [`controllers/plans`](../plans/README.md),
[`controllers/academics/structure`](../academics/structure/README.md) and
[`controllers/academics/grading`](../academics/grading/README.md) were done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/people`](../../models/people), whose README already describes all thirteen
documents, the two embedded types and the reference rules. **These endpoints enforce that file.**

**This is the domain map. Each package has its own plan, and that is what gets built from:**

| Package | Endpoints | Plan |
|---|---|---|
| `staff` | #1–#8, #16–#19, #20–#29 | [`staff/README.md`](staff/README.md) |
| `organization` | #9–#15 | [`organization/README.md`](organization/README.md) |
| `leave` | #30–#39 | [`leave/README.md`](leave/README.md) |
| `reviews` | #40–#43, #45–#47 | [`reviews/README.md`](reviews/README.md) |
| `development` | #44, #48–#51 | [`development/README.md`](development/README.md) |

**One endpoint keeps one number across all five**, so a service javadoc saying "#16" is
unambiguous. The per-package files carry the detail, the open items and the field tables; this one
carries what spans them.

> **This module is picked because of one field.** `staffDocsId` is stored by **16 model files
> across 6 modules** — `academics` (5), `payroll` (2), `people` itself (7) and `transport` (1) —
> and nothing can create a staff member today. A class teacher, a subject teacher, a timetable
> entry, an attendance session, a payslip and a bus driver all name a person who cannot exist.
>
> **The models here are in better shape than the two that came before.** `SchoolClass` and
> `GradingScheme` both shipped a unique index over a field they never declared. Every index in
> `people` names a real field, and the two that needed a `partialFilter` — one current employment
> per staff member, one primary bank account — both have one. Nothing is blocked on a model fix.

## Where to start

**Which package to open, in order.** Rows 1 to 3 are one sitting — they only mean anything
together. The reasoning is in [Build order](#build-order); this is the answer.

| | Open | Build | Why now |
|---|---|---|---|
| **1** | [`organization/`](organization/README.md) | `#9` `#13` | Nobody can be hired into a position that does not exist. **Two endpoints.** |
| **2** | [`staff/`](staff/README.md) | `#1` `#16` | The person, then the job. **This is the write 16 model files across 6 modules are waiting for.** |
| **3** | [`staff/`](staff/README.md) | `#7` `#8` | The reads every other module will actually call. |
| | | | ⇢ *`payroll` and every teacher picker unblock here. Stop and use it before going on.* |
| **4** | [`organization/`](organization/README.md) | `#12` `#15` `#10` `#14` `#11` | The chart becomes readable, then editable. |
| **5** | [`staff/`](staff/README.md) | `#19` `#17` `#18` `#2` `#6` | Employment history, leaving, and corrections. |
| **6** | [`staff/`](staff/README.md) | `#3` `#4` `#5` `#20`\* `#21` `#22` `#23` | The rest of the profile, and the compliance report. |
| **7** | [`leave/`](leave/README.md) | `#30`–`#39` | Self-contained. **Settle its transaction question first.** |
| **8** | [`reviews/`](reviews/README.md) | `#40`–`#43`, `#45`–`#47` | Nothing outside the package references a review. |
| **9** | [`development/`](development/README.md) | `#44`, `#48`–`#51` | Nothing references a development record either. Genuinely last. |
| **⛔** | [`staff/`](staff/README.md) | `#24`–`#29`, and `#20`'s number field | **Blocked** on encryption — not deferred. |

\* **`#20` splits.** The title, authority and expiry of a credential are plaintext; only the
certificate number is encrypted, and the index over it is partial. Row 6 records *"B.Ed, Delhi
University, valid until 2030"* and leaves the number out.

**Rows 1 to 3 are the only ones that block anything outside `people`.** Everything from row 4 down
is this module improving itself, and can be paused or reordered without stranding another team.
**Rows 7, 8 and 9 are independent of each other** — pick by what a school is asking for.

---

## What this module is

**Everyone the school employs, and everything the school knows about employing them.**

```text
Department  "Academics"                         <- an org unit, may nest
  └── Position  "Senior Mathematics Teacher"    <- an approved seat, with a headcount
        └── EmploymentRecord                    <- who sits in it, and when
              └── Staff  "Priya Sharma"         <- the person

Staff
  ├── StaffCredential[]          degrees, licences, background checks
  ├── StaffGovernmentIdentity[]  encrypted, never plaintext
  ├── StaffBankAccount[]         encrypted, one primary
  ├── StaffLeaveBalance[]        one per leave type per year
  │     └── StaffLeaveRequest[]
  ├── StaffReview[]              one per reviewer per cycle
  └── StaffDevelopmentRecord[]   training, and what it was worth
```

Thirteen documents and two embedded types:

| Document | Collection | What it holds |
|---|---|---|
| [`Staff`](../../models/people/staff/Staff.java) | `staff` | the person — name, contact, addresses. **What 16 other files point at.** |
| [`EmploymentRecord`](../../models/people/staff/EmploymentRecord.java) | `employment_records` | one employment period. Only one may be `current` |
| [`Department`](../../models/people/organization/Department.java) | `staff_departments` | an org unit, optionally nested |
| [`Position`](../../models/people/organization/Position.java) | `staff_positions` | an approved seat inside a department |
| [`StaffCredential`](../../models/people/staff/StaffCredential.java) | `staff_credentials` | a qualification or licence, with an expiry |
| [`StaffGovernmentIdentity`](../../models/people/staff/StaffGovernmentIdentity.java) | `staff_government_identities` | Aadhaar, PAN, passport — encrypted |
| [`StaffBankAccount`](../../models/people/staff/StaffBankAccount.java) | `staff_bank_accounts` | where payroll pays — encrypted |
| [`LeaveType`](../../models/people/leave/LeaveType.java) | `staff_leave_types` | the school's leave policy |
| [`StaffLeaveBalance`](../../models/people/leave/StaffLeaveBalance.java) | `staff_leave_balances` | one staff member's entitlement for one year |
| [`StaffLeaveRequest`](../../models/people/leave/StaffLeaveRequest.java) | `staff_leave_requests` | one application against one balance |
| [`ReviewCycle`](../../models/people/reviews/ReviewCycle.java) | `staff_review_cycles` | a review period and its criteria |
| [`StaffReview`](../../models/people/reviews/StaffReview.java) | `staff_reviews` | one reviewer's submission |
| [`StaffDevelopmentRecord`](../../models/people/development/StaffDevelopmentRecord.java) | `staff_development_records` | training and its evaluated impact |
| [`StaffAddress`](../../models/people/staff/embedded/StaffAddress.java) · [`EmergencyContact`](../../models/people/staff/embedded/EmergencyContact.java) | *embedded in Staff* | no id, no tenant — they belong to the profile |

### The person and the job are two things, deliberately

**`Staff` has no department, no designation and no employment status.** It is the human being;
everything about the job lives on `EmploymentRecord`. The model README is explicit that Staff does
not store *"department/designation strings"* or *"joining or separation history"*.

That split is what makes a promotion expressible. A teacher who becomes head of department is one
`Staff` document and two `EmploymentRecord`s, and every mark they entered under the old position
still points at the same person.

**It also means a Staff-only slice cannot answer "who works here".** Every `staffDocsId` reference
would resolve, but a teacher picker would list leavers beside current staff with nothing to tell
them apart. That is why [phase 1](#build-order) is four documents rather than one.

## What this module is not

- **Not login, roles or permissions.** A `Staff` document is an employment record, not an account.
  `identity` owns who may sign in, and the two are deliberately separate: a teacher who leaves
  keeps their employment history and loses their login on the same day, through different writes.
- **Not payroll.** `SalaryStructure` and `Payslip` reference `staffDocsId` and live in `payroll`.
  Compensation is not a property of a person; it is a property of an agreement.
- **Not students, guardians or applicants.** Different people, different modules.
- **Not attendance.** A staff member's own attendance is not modelled anywhere yet — see
  [open item 6](#6-staff-attendance-is-not-modelled-at-all).

---

## One surface, and why

| Surface | Base path | Who is calling | Tenant comes from |
|---|---|---|---|
| **School** | `/schools/current/…` | the school itself | `CurrentSchoolResolver`, never the URL |

**There is no platform surface, and there must not be one.** A school's staff list is the most
personal data this product holds — names, addresses, dates of birth, emergency contacts,
government identities. No operator of ours should be reading it to do their job, and an endpoint
that allowed it would be the first thing a security review asked about.

**And no `{year}` segment on most of it.** A person is employed across years, not inside one. The
exceptions are the two documents that genuinely are per-year — `StaffLeaveBalance` and
`ReviewCycle` both store `AcademicYear.name` — and those carry the year in the body rather than
the path, because the resource is the balance and not the year.

### Addressed by id, everywhere

The rule this project settled on 2026-09-10 is *use whatever other collections already store*:

| Thing | Referenced by | Addressed in the URL by |
|---|---|---|
| a staff member | `staffDocsId`, in **16** model files across 6 modules | its **id** |
| a department | `departmentDocsId` | its **id** |
| a position | `positionDocsId` | its **id** |
| a leave type | `leaveTypeDocsId` | its **id** |
| a review cycle | `reviewCycleDocsId` | its **id** |

**`employeeNo` is a display key, not an address.** It is unique per school and generated rather
than typed, so it is stable enough to be one — but nothing stores it as a reference, and a URL
built from it would be a second way to name a person that every consumer would have to learn.
It is searchable through [#7](#e7) and printed on every response.

**Codes exist on the two configuration documents** — `departmentCode`, `positionCode`,
`leaveTypeCode`, `cycleCode` — for the same reason `termCode` does: a person types them into a
filter and reads them on an export. They are unique per school, given rather than derived, and
never change once anything references them.

---

## Personal data in messages

The other modules have a rule about dates. This one has a rule about **people**, and it is the
more important of the two.

```
in a field   "maskedIdentityNumber": "XXXX XXXX 4821"
in a message "... this Aadhaar is already recorded against another staff member."
             NOT "... already recorded against Priya Sharma (EMP-0042)."
```

**A refusal must not name a person the caller may not be entitled to see.** A duplicate-identity
check spans the whole school, so the message says *that* there is a clash without saying *who* —
otherwise the endpoint becomes a way to enumerate staff by guessing identity numbers. The same
rule applies to a duplicate bank account and a duplicate credential number.

**This is the opposite of every other module's convention**, where naming the conflicting row is
the whole point of a good message — "'Term 1' is already sequence 1 in this year". Here, the
conflicting row is a person.

**Government identity numbers never appear in a message, a log or a URL**, even masked. See
[open item 1](#1-nothing-can-encrypt-anything--this-blocks-three-documents).

---

## Which gates every endpoint runs

[`ActionGate`](../../common/access/ActionGate.java), the same pattern as everywhere else:

| | Gates |
|---|---|
| every **write** below | 1 school is live · 2 school is paying |
| every **read** below | none — looking at a record is not an action on it |

**Gate 4 is not used here, and it is not a judgement call.** Gate 4 asks whether a named academic
year is the school's working one. Most paths here carry no year at all, and the two that do —
leave balances and review cycles — are opened *before* the year starts, which is exactly the call
gate 4 would refuse. `controllers/core` documents that argument in full and it is not repeated.

**Authorization matters more here than anywhere and does not exist yet.** Every response in this
project currently ends with *"No authorization is enforced on this endpoint yet"*. On a class list
that is a note; on a staff member's address, date of birth and bank account it is the reason this
module must not go near production before `identity` does. See
[open item 2](#2-this-is-the-module-that-cannot-ship-without-authorization).

---

# The endpoints

Numbered straight through, 1 to 51. **`#45`–`#51` were appended** when the per-package plans
were written and found that `reviews` and `development` each carry a lifecycle the domain-level
tables had compressed into one row — a five-state review cycle and a six-state training
approval. Numbers are never reused; new ones go on the end. **A number is never reused for a different endpoint** — these
get referenced from the service, the API catalogue and the Postman collection. Every path below is
relative to **`/schools/current`**.

## 1. The people — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>1 | [`POST /staff`](#e1) | Create a person. `employeeNo` is generated, never sent. **The endpoint the other five modules are waiting for.** | [`staff`](../../models/people/staff/Staff.java) |
| <a id="t2"></a>2 | [`PATCH /staff/{id}`](#e2) | Fix a name, a phone number, a date of birth. | [`staff`](../../models/people/staff/Staff.java) |
| <a id="t3"></a>3 | [`PUT /staff/{id}/addresses`](#e3) | Replace current and permanent address as a pair — the two are only meaningful together. | [`staff`](../../models/people/staff/Staff.java) |
| <a id="t4"></a>4 | [`PUT /staff/{id}/emergency-contact`](#e4) | Replace it whole. A half-updated emergency contact is worse than none. | [`staff`](../../models/people/staff/Staff.java) |
| <a id="t5"></a>5 | [`PUT /staff/{id}/photo`](#e5) | Point at a `DocumentRecord`, or clear it. | [`staff`](../../models/people/staff/Staff.java) |
| <a id="t6"></a>6 | [`POST /staff/{id}/archive`](#e6) | Remove a profile created by mistake. **Not** how somebody leaves — that is [#16](#e16). | [`staff`](../../models/people/staff/Staff.java) |

## 2. The people — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t7"></a>7 | [`GET /staff`](#e7) | The list behind every teacher picker in the product. Filtered by employment, department, position, **`?teaching=`** and name. | [`staff`](../../models/people/staff/Staff.java), [`employment_records`](../../models/people/staff/EmploymentRecord.java) |
| <a id="t8"></a>8 | [`GET /staff/{id}`](#e8) | One person, with their current employment folded in. | [`staff`](../../models/people/staff/Staff.java), [`employment_records`](../../models/people/staff/EmploymentRecord.java) |

## 3. The organization · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t9"></a>9 — **built** | [`POST /departments`](#e9) | Create an org unit, optionally under another. | [`staff_departments`](../../models/people/organization/Department.java) |
| <a id="t10"></a>10 | [`PATCH /departments/{id}`](#e10) | Rename it, move it, or name its head. Never its code. | [`staff_departments`](../../models/people/organization/Department.java) |
| <a id="t11"></a>11 | [`POST /departments/{id}/deactivate`](#e11) · [`/reactivate`](#e11) | Retire an org unit without deleting it. Idempotent pair. | [`staff_departments`](../../models/people/organization/Department.java) |
| <a id="t12"></a>12 | [`GET /departments`](#e12) | The tree, or one flat filtered page. | [`staff_departments`](../../models/people/organization/Department.java) |
| <a id="t13"></a>13 | [`POST /positions`](#e13) | Create an approved seat inside a department, with a headcount. | [`staff_positions`](../../models/people/organization/Position.java) |
| <a id="t14"></a>14 | [`PATCH /positions/{id}`](#e14) | Retitle it, or change the approved headcount. | [`staff_positions`](../../models/people/organization/Position.java) |
| <a id="t15"></a>15 | [`GET /positions`](#e15) | Seats, with **filled counts computed** rather than stored. | [`staff_positions`](../../models/people/organization/Position.java), [`employment_records`](../../models/people/staff/EmploymentRecord.java) |

## 4. Employment · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t16"></a>16 | [`POST /staff/{id}/employment`](#e16) | Hire, promote or transfer. **Closes the previous record and opens the new one in one write.** | [`employment_records`](../../models/people/staff/EmploymentRecord.java) |
| <a id="t17"></a>17 | [`POST /staff/{id}/separate`](#e17) | End employment. The one way somebody leaves. | [`employment_records`](../../models/people/staff/EmploymentRecord.java) |
| <a id="t18"></a>18 | [`PATCH /employment/{id}`](#e18) | Correct a date or a manager on a record already written. | [`employment_records`](../../models/people/staff/EmploymentRecord.java) |
| <a id="t19"></a>19 | [`GET /staff/{id}/employment`](#e19) | One person's history, newest first. | [`employment_records`](../../models/people/staff/EmploymentRecord.java) |

## 5. Credentials, identities and bank accounts · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t20"></a>20 | [`POST /staff/{id}/credentials`](#e20) | Record a degree, licence or background check. | [`staff_credentials`](../../models/people/staff/StaffCredential.java) |
| <a id="t21"></a>21 | [`POST /credentials/{id}/verify`](#e21) | Mark it checked, or rejected, with who and when. | [`staff_credentials`](../../models/people/staff/StaffCredential.java) |
| <a id="t22"></a>22 | [`GET /staff/{id}/credentials`](#e22) | One person's, with expiry state computed. | [`staff_credentials`](../../models/people/staff/StaffCredential.java) |
| <a id="t23"></a>23 | [`GET /credentials/expiring`](#e23) | **The compliance report.** Everything lapsing inside a window. | [`staff_credentials`](../../models/people/staff/StaffCredential.java) |
| <a id="t24"></a>24 | [`POST /staff/{id}/identities`](#e24) | Record a government identity. Encrypted on the way in, never echoed. | [`staff_government_identities`](../../models/people/staff/StaffGovernmentIdentity.java) |
| <a id="t25"></a>25 | [`GET /staff/{id}/identities`](#e25) | Masked only. The plaintext is not on this endpoint at any permission level. | [`staff_government_identities`](../../models/people/staff/StaffGovernmentIdentity.java) |
| <a id="t26"></a>26 | [`POST /identities/{id}/reveal`](#e26) | Authorized recovery, audited. **A POST because it is an event, not a read.** | [`staff_government_identities`](../../models/people/staff/StaffGovernmentIdentity.java) |
| <a id="t27"></a>27 | [`POST /staff/{id}/bank-accounts`](#e27) | Where payroll pays. Encrypted, and only one may be primary. | [`staff_bank_accounts`](../../models/people/staff/StaffBankAccount.java) |
| <a id="t28"></a>28 | [`POST /bank-accounts/{id}/verify`](#e28) | A penny-drop or document check, recorded. | [`staff_bank_accounts`](../../models/people/staff/StaffBankAccount.java) |
| <a id="t29"></a>29 | [`GET /staff/{id}/bank-accounts`](#e29) | Masked, with the primary marked. | [`staff_bank_accounts`](../../models/people/staff/StaffBankAccount.java) |

## 6. Leave · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t30"></a>30 | [`POST /leave-types`](#e30) | Define a leave policy — sick, casual, earned, unpaid. | [`staff_leave_types`](../../models/people/leave/LeaveType.java) |
| <a id="t31"></a>31 | [`PATCH /leave-types/{id}`](#e31) | Change next year's allowance without touching this year's balances. | [`staff_leave_types`](../../models/people/leave/LeaveType.java) |
| <a id="t32"></a>32 | [`GET /leave-types`](#e32) | The policy list. | [`staff_leave_types`](../../models/people/leave/LeaveType.java) |
| <a id="t33"></a>33 | [`POST /leave-balances/open-year`](#e33) | Open every balance for a year in one write, carrying forward what the policy allows. | [`staff_leave_balances`](../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t34"></a>34 | [`POST /leave-balances/{id}/adjust`](#e34) | Add or remove days with a reason. Never a silent edit. | [`staff_leave_balances`](../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t35"></a>35 | [`GET /staff/{id}/leave-balances`](#e35) | What this person has left, computed rather than stored. | [`staff_leave_balances`](../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t36"></a>36 | [`POST /staff/{id}/leave-requests`](#e36) | Apply. Reserves days against the balance in the same transaction. | [`staff_leave_requests`](../../models/people/leave/StaffLeaveRequest.java), [`staff_leave_balances`](../../models/people/leave/StaffLeaveBalance.java) |
| <a id="t37"></a>37 | [`POST /leave-requests/{id}/approve`](#e37) | Move days from pending to used, atomically. | both |
| <a id="t38"></a>38 | [`POST /leave-requests/{id}/reject`](#e38) · [`/cancel`](#e38) | Release the reservation. | both |
| <a id="t39"></a>39 | [`GET /leave-requests`](#e39) | The approver's queue. | [`staff_leave_requests`](../../models/people/leave/StaffLeaveRequest.java) |

## 7. Reviews and development · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t40"></a>40 | [`POST /review-cycles`](#e40) | Open a review period with its criteria. | [`staff_review_cycles`](../../models/people/reviews/ReviewCycle.java) |
| <a id="t41"></a>41 | [`POST /review-cycles/{id}/open`](#e41) · [`/close`](#e41) | Start and stop accepting submissions. | [`staff_review_cycles`](../../models/people/reviews/ReviewCycle.java) |
| <a id="t42"></a>42 | [`POST /review-cycles/{id}/reviews`](#e42) | Submit one review. Anonymous submissions carry a hash, not an id. | [`staff_reviews`](../../models/people/reviews/StaffReview.java) |
| <a id="t43"></a>43 | [`GET /review-cycles/{id}/reviews`](#e43) | Aggregate or raw, and **which one you get is a policy decision, not a parameter.** | [`staff_reviews`](../../models/people/reviews/StaffReview.java) |
| <a id="t45"></a>45 · <a id="t46"></a>46 · <a id="t47"></a>47 | `PATCH /review-cycles/{id}` · `GET /review-cycles` · `POST /reviews/{id}/acknowledge` | Edit a draft cycle, list cycles, and the one write a reviewee makes. **Added with the [reviews plan](reviews/README.md).** | [`staff_review_cycles`](../../models/people/reviews/ReviewCycle.java), [`staff_reviews`](../../models/people/reviews/StaffReview.java) |
| <a id="t44"></a>44 | [`POST /staff/{id}/development`](#e44) | Record a plan, or something already done. | [`staff_development_records`](../../models/people/development/StaffDevelopmentRecord.java) |
| <a id="t48"></a>48 · <a id="t49"></a>49 · <a id="t50"></a>50 · <a id="t51"></a>51 | `PATCH /development/{id}` · `/approve` · `/complete` · `GET` | The six-state approval workflow the model carries and this table first missed. **Added with the [development plan](development/README.md).** | [`staff_development_records`](../../models/people/development/StaffDevelopmentRecord.java) |

---

# Build order

Ordered by **what it unblocks**, not by number. The short answer is
[**Where to start**](#where-to-start) at the top of this file — this section is why.

## What actually depends on what

```text
  organization                 staff
  ────────────                 ─────
  #9  Department
       │
       ▼
  #13 Position ───────────▶ #16 EmploymentRecord ──▶ #7  #8
                                     ▲
                                #1 Staff
```

**That is the whole of the module's internal ordering.** Everything else — leave, reviews,
development, credentials, the org-chart edits — needs only a `staffDocsId` to exist, which
[`#1`](staff/README.md#e1) gives them.

## Phase 1 — six endpoints, two packages, and the point of the module

| Order | Package | Endpoint | Why here |
|---|---|---|---|
| 1 | `organization` | [`#9 POST /departments`](organization/README.md#e9) | A position needs a department to sit in |
| 2 | `organization` | [`#13 POST /positions`](organization/README.md#e13) | An employment record needs a position to point at |
| 3 | `staff` | [`#1 POST /staff`](staff/README.md#e1) | The person |
| 4 | `staff` | [`#16 POST /staff/{id}/employment`](staff/README.md#e16) | The job — **this is what makes a staff list mean anything** |
| 5 | `staff` | [`#7 GET /staff`](staff/README.md#e7) | The teacher picker |
| 6 | `staff` | [`#8 GET /staff/{id}`](staff/README.md#e8) | One person in full |

**Finish `organization` `#9` and `#13` before opening `staff`.** [`#16`](staff/README.md#e16)
writes a `positionDocsId`, so there is nothing to test it against until a position exists — and
they are two small endpoints over two small documents.

**`#1` before `#16`, and both before `#7`.** A staff member with no employment record is a person
the school has entered but not yet hired — a real state, and the one [`#1`](staff/README.md#e1)
leaves them in.

**Stop after row 6 and use it.** `payroll` needs `staffDocsId` and nothing else from this module,
so it unblocks at the end of phase 1 rather than at the end of `people`.

## After phase 1, nothing here is on a critical path

| Phase | Package | Endpoints | What it gives you |
|---|---|---|---|
| **2** | [`organization`](organization/README.md) | 12, 15, 10, 14, 11 | The chart, then the edits to it. **Reads first** — [`#12`](organization/README.md#e12) and [`#15`](organization/README.md#e15) are how the edits get verified. |
| **3** | [`staff`](staff/README.md) | 19, 17, 18, 2, 6 | Employment history, leaving, and the corrections a school will ask for in week one. |
| **4** | [`staff`](staff/README.md) | 3, 4, 5, 20, 21, 22, 23 | Addresses, emergency contact, photo, credentials and the expiry report. **[`#20`](staff/README.md#e20) without its number field** — see below. |
| **5** | [`leave`](leave/README.md) | 30–39 | A feature end to end. **Settle [the two-document write](leave/README.md#1-two-documents-must-move-together-and-there-are-no-transactions) before starting** — it changes every service in the package. |
| **6** | [`reviews`](reviews/README.md) | 40, 45, 46, 41, 42, 43, 47 | The five-state cycle, cycle-shaped first. |
| **7** | [`development`](development/README.md) | 44, 51, 49, 50, 48 | The six-state approval. Nothing anywhere reads a development record. |

**Phases 5, 6 and 7 do not depend on one another.** Build whichever a school is asking for; none of
them blocks anything, inside this module or outside it.

## Blocked, not deferred

| Package | Endpoints | Blocked on |
|---|---|---|
| [`staff`](staff/README.md) | 24, 25, 26, 27, 28, 29, and the `number` field of 20 | [Open item 1 — nothing can encrypt anything](#1-nothing-can-encrypt-anything--this-blocks-three-documents) |

Three documents store an encrypted value and a keyed lookup hash, and nothing in this project can
produce either. **The shortcut here is the one that cannot be undone** — once a real Aadhaar or
account number is written as plaintext, no later migration takes it back out of the backups.

**[`#20`](staff/README.md#e20) is the exception, because it splits.** A credential's title,
issuing authority, dates and expiry are not encrypted — only `credentialNumber` is, and
`staff_credentials`' unique index over its hash is partial on that hash being a string. So phase 4
can record *"B.Ed, Delhi University, valid until 2030"*, and
[`#23`](staff/README.md#e23) — the compliance report, which reads dates and never numbers —
works fully without the key vault.

---

# Things this module deliberately will not have

- **No `DELETE` on anything.** Sixteen model files store `staffDocsId`, and none of those
  references is a foreign key: a deleted person would leave every attendance session, payslip and
  timetable entry pointing at nothing, and *nothing would fail*. Leaving is [#17](#e17), which
  ends employment and keeps the person. [#6](#e6) archives a profile created by mistake, which is
  a different act with a different word.
- **No endpoint that returns a plaintext government identity in a `GET`.** [#26](#e26) is a POST
  for that reason: revealing a national identity number is an **event** that must be recorded,
  and a GET that writes an audit row is a GET that lies about being safe to retry.
- **No "staff directory" for non-staff callers.** There is one staff list, [#7](#e7), and it
  returns addresses and dates of birth. A parent-facing "who teaches my child" needs a different
  endpoint with a different shape, and it belongs to whichever module owns the parent surface.
- **No bulk import.** A CSV of two hundred staff members is a real need and a genuinely different
  endpoint — partial success, per-row errors, an idempotency key. Designing it as "call #1 in a
  loop" is how you get two hundred half-created people. It gets its own plan when somebody asks.
- **No salary anywhere.** Not on `Staff`, not on `Position`, not on `EmploymentRecord`. The model
  README is explicit, and the reason is access: everything in this module is readable by an HR
  admin, and compensation is not.
- **No reporting-line queries.** `EmploymentRecord.managerDocsId` exists, and "show me everyone
  under Priya" is a graph walk this module will not do. It wants a materialised path or an
  aggregation, and it wants a use case first.

---

# To settle before building

## 1. Nothing can encrypt anything — this blocks three documents

`StaffGovernmentIdentity`, `StaffBankAccount` and `StaffCredential` each store the same triple:

```text
encryptedIdentityNumber      the real value, recoverable by an authorized caller
identityNumberLookupHash     a KEYED hash, for exact-duplicate checks only
maskedIdentityNumber         what the UI shows
```

**None of that exists.** There is no encryption service, no keyed-hash helper and no key-vault
reference resolver anywhere in `common` or `services`. The model README says the key *"must be
resolved through the school's key-vault/KMS reference"*, and nothing resolves one.

**This is a blocker, not a shortcut to take.** The tempting move — store the number as a plain
string "for now" and encrypt later — is unrecoverable: once a real Aadhaar is in the database in
plaintext, no later migration makes it as though it never was, and the backups keep it.

**What it needs, in order:**

1. a `KeyVaultReference` resolver per school, which `School` may already have a field for
2. an encrypt/decrypt pair over that key
3. a **keyed** hash — HMAC, not a bare digest, or a national identity number is brute-forceable in
   seconds from a stolen index
4. a masking rule per identity type, because an Aadhaar and a passport mask differently

**Until then, phase 6 is not started.** Phases 1 to 5 touch none of it.

## 2. This is the module that cannot ship without authorization

Every endpoint in this project currently ends its response with *"No authorization is enforced on
this endpoint yet: any caller who can reach it can run it."* Everywhere else that sentence is an
embarrassment. Here it is a data-protection incident waiting to happen.

**[#7](#e7) alone returns**, for every employee of the school: full name, date of birth, personal
phone number, personal email, home address and emergency contact.

**The rule this module needs and cannot enforce yet:**

| Who | Should see |
|---|---|
| the staff member | their own everything |
| their manager | profile, employment, leave — not identities, not bank |
| an HR admin | everything except the plaintext of [#26](#e26) |
| anyone else | nothing |

**Recommendation: build phases 1 and 2 behind whatever `identity` ships, and do not deploy any of
this to a real school before it exists.** Phase 1 is safe to build now because it is the unblocking
work — but the moment [#7](#e7) is reachable from a browser, the exposure is real.

## 3. `#7`'s filters need a join Mongo will not do

The one query every other module wants is *"current teachers in this department"*. That reads
`staff` for the person and `employment_records` for current, position and department — two
collections, and Mongo has no join in a plain `find`.

**Three ways, and the choice decides how `#7` is written:**

- **a) Two queries in the service.** Read `employment_records` where `current = true` and the
  filters match, collect `staffDocsId`, then read `staff` by id. Paging has to happen on the
  *first* query or the page sizes lie. Simple, and correct.
- **b) `$lookup` aggregation.** One round trip, and the paging is honest. Harder to read, and this
  project has no aggregation anywhere yet.
- **c) Denormalise `currentPositionDocsId` onto `Staff`.** Fast and wrong the first time a writer
  forgets to update it — the same objection that killed a stored `filledHeadcount` on `Position`.

**Recommendation: (a)**, paging on `employment_records`, because every filter that narrows the
result lives there. A staff member with no employment record then cannot appear in a filtered
list at all — which is correct, and needs saying in the response so nobody reads an empty page as
a missing person.

## 4. Nothing says when an employment record may overlap another

`school_staff_employment_start_uniq` stops two records starting on the same day, and
`school_staff_current_employment_uniq` stops two being current. **Neither stops overlapping
ranges**: a record running 2024-01-01 to 2026-12-31 and another starting 2025-06-01 are both
storable, and both non-current.

Is that a bug? **Probably not** — a teacher genuinely may hold two concurrent part-time positions,
and the model has no field saying otherwise. But #16 and #18 have to decide, because "what is this
person's position on 3 March 2025" has two answers today.

**Settle before #16**, since it is the endpoint that writes them.

## 5. A leave balance is arithmetic, and the model stores the inputs

```text
available = allocatedDays + carriedForwardDays + adjustmentDays - usedDays - pendingDays
```

Five stored fields and one derived. **Nothing stores `available`**, which is right — it would go
stale the instant a request was approved — so [#35](#e35) computes it on every read, the same way
the grading module recomputes its gap warning rather than storing it.

**The hard part is `#37`.** Approving a request has to move days from `pendingDays` to `usedDays`
on the balance *and* flip the request's status, and the two must not diverge. The model README
says to use "a MongoDB transaction or equivalent atomic workflow", and **this project runs no
transactions** — `@Transactional` appears on services but nothing configures a transaction manager
for Mongo.

**Decide before #36:** either configure transactions, or make the balance write conditional on the
inherited `version` and retry. The optimistic-lock field is already there for exactly this.

## 6. Staff attendance is not modelled at all

`AttendanceSession` and `StudentAttendanceRecord` are about students. There is no
`StaffAttendanceRecord`, no check-in, no muster roll — and `StaffLeaveRequest` covers planned
absence only.

That is a gap in `models`, not in this plan, and it is worth knowing before somebody promises a
school an attendance report. **Out of scope here**; it wants a model before it wants endpoints.

## 7. A review's anonymity is enforced by one hash, and nothing rotates it

`StaffReview` omits `reviewerDocsId` for anonymous feedback and stores a keyed
`reviewerLookupHash` instead, so the same person cannot submit twice without being identifiable.

**That is only as anonymous as the key.** Anyone who can read the key and the collection can
re-derive who said what, by hashing every staff id in the school and matching. The hash space is
the staff list — a few hundred entries.

**It needs the same key-vault work as [open item 1](#1-nothing-can-encrypt-anything--this-blocks-three-documents)**,
and it needs somebody to decide whether "anonymous" here means *anonymous to the reviewee* or
*anonymous to everyone including the school*. Those are different products, and the second one
this design cannot deliver.

---

# Where the code will live

Following the four folder rules in `memory/backend/code-writing-rules`:

```text
controllers/people/
├── README.md                      <- this file
├── StaffController.java           #1–#8, #16–#19  — the person and their job
├── OrganizationController.java    #9–#15          — departments and positions
├── StaffRecordController.java     #20–#29         — credentials, identities, bank
├── LeaveController.java           #30–#39
└── ReviewController.java          #40–#44

services/people/
├── StaffService.java
├── OrganizationService.java
├── StaffRecordService.java
├── LeaveService.java
├── ReviewService.java
├── helper/PeopleHelper.java       the rules MongoDB cannot express
└── utils/StaffServiceUtils.java   one staff member by id, and their current employment

repositories/people/
├── staff/StaffRepository.java     exists — findByIdAndSchoolId, built with #17 of academics
├── staff/StaffRepositoryCustom.java + Impl    #7's filtered, paged search
├── staff/EmploymentRecordRepository.java
├── organization/DepartmentRepository.java
├── organization/PositionRepository.java
├── leave/…  reviews/…  development/…

dto/people/{staff,organization,leave,reviews,development}/{request,response}/
```

**Five controllers, not one.** Forty-four endpoints in one file would be unreadable, and the five
groups share no path prefix beyond `/schools/current` — a leave request and a department have
nothing in common but a tenant.

**Employment lives on `StaffController`, not its own.** Every employment path is
`/staff/{id}/…`, and a controller that owned `/employment/{id}` alone would be two files for one
resource. The two endpoints that are not staff-scoped — [#18](#e18) and the position fill count in
[#15](#e15) — sit where their subject does.

**One warning, repeated from the plans module:** Spring Data resolves a custom fragment
`XRepositoryImpl` by **name and package**. Put it beside its interface, or the app compiles,
starts, and fails only when somebody calls `search`.

---

# Appendix — what each field can hold

Only the documents phase 1 and 2 touch are written out here. The rest are described in
[`models/people/README.md`](../../models/people/README.md), and get a table when their phase is
reached.

## `staff` — [Staff](../../models/people/staff/Staff.java)

| Field | Type | What can be in it |
|---|---|---|
| `employeeNo` | String, required | **Generated, never sent.** `NumberSequenceType.EMPLOYEE_NUMBER`, unique with `schoolId`. A display key and a search key — nothing references it. |
| `fullName` | String, required | One field, not three. Indexed with `schoolId` for `?search=`. |
| `dateOfBirth` | LocalDate | Personal data. Returned by [#8](#e8), not by [#7](#e7)'s row. |
| `gender` | Enum | Shared `Gender`, not a people-specific one. |
| `nationalityCode` · `preferredLanguage` | String | ISO codes. |
| `phoneNumber` · `emailAddress` | String | **Normalised on the way in** — international format, trimmed lowercase — per the model README. |
| `currentAddress` · `permanentAddress` | Embedded | Replaced as a pair by [#3](#e3). |
| `emergencyContact` | Embedded | Replaced whole by [#4](#e4). |
| `profileImageDocsId` | String | A `DocumentRecord` id. [#5](#e5) sets or clears it. |

**No status, no department, no designation** — all three live on `EmploymentRecord`, deliberately.

## `employment_records` — [EmploymentRecord](../../models/people/staff/EmploymentRecord.java)

| Field | Type | What can be in it |
|---|---|---|
| `staffDocsId` | String, required | The person. Unique with `effectiveFrom`, so two records cannot start the same day. |
| `positionDocsId` | String, required | The seat. Must be an active `Position` of this school. |
| `managerDocsId` | String | Another `Staff`. Not validated as acyclic — see [what this module will not have](#things-this-module-deliberately-will-not-have). |
| `status` | Enum, required | `EmploymentStatus`. What "employed" means for [#7](#e7)'s filter. |
| `employmentType` | Enum, required | Full time, part time, contract. |
| `effectiveFrom` · `effectiveUntil` | LocalDate | The period. `effectiveUntil` null means open-ended. |
| `probationUntil` | LocalDate | Optional, and nothing enforces it yet. |
| `current` | Boolean, required | **One per staff member**, enforced by a partial unique index filtered to `true`. [#16](#e16) flips the old one in the same write. |
| `separationReason` | String | Set by [#17](#e17). |

## `staff_departments` — [Department](../../models/people/organization/Department.java)

| Field | Type | What can be in it |
|---|---|---|
| `departmentCode` | String, required | Unique with `schoolId`. Given, not derived — the `termCode` rule. **Never changes.** |
| `name` | String, required | Editable, indexed with `active`. |
| `parentDepartmentDocsId` | String | Optional nesting. **A cycle is the caller's to avoid and [#10](#e10)'s to refuse.** |
| `headStaffDocsId` | String | A `Staff` id, validated to exist in this school. |
| `active` | Boolean, required | `true` at create; [#11](#e11) flips it. |

## `staff_positions` — [Position](../../models/people/organization/Position.java)

| Field | Type | What can be in it |
|---|---|---|
| `positionCode` | String, required | Unique with `schoolId`. **Never changes.** |
| `departmentDocsId` | String, required | The owning unit. Indexed with `active` and `title`. |
| `title` | String, required | What the seat is called. |
| `approvedHeadcount` | Integer | How many may hold it. **Filled headcount is never stored** — [#15](#e15) counts current employment records, because a stored counter drifts. |
| `active` | Boolean, required | Retiring a position does not end anybody's employment. |

## The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `STAFF_NOT_FOUND` | 404 | no staff member with that id in this school |
| `DEPARTMENT_NOT_FOUND` · `POSITION_NOT_FOUND` | 404 | same, for the org documents |
| `EMPLOYMENT_RECORD_NOT_FOUND` | 404 | — |
| `DEPARTMENT_CODE_TAKEN` · `POSITION_CODE_TAKEN` | 409 | that code is already this school's |
| `DEPARTMENT_CYCLE` | 409 | [#10](#e10) — a department cannot be its own ancestor |
| `DEPARTMENT_NOT_EMPTY` | 409 | [#11](#e11) — it still holds active positions |
| `POSITION_NOT_ACTIVE` | 409 | [#16](#e16) — hiring into a retired seat |
| `POSITION_FULL` | 409 | [#16](#e16) — approved headcount reached. A **warning** rather than a refusal if the school over-hires deliberately; see [#16](#e16) |
| `EMPLOYMENT_ALREADY_CURRENT` | 409 | [#16](#e16) — a current record exists and the body did not say to close it |
| `NOT_EMPLOYED` | 409 | [#17](#e17) — separating somebody with no current record |
| `INVALID_EMPLOYMENT_RANGE` | 400 | `effectiveUntil` before `effectiveFrom` |
| `STAFF_STILL_EMPLOYED` | 409 | [#6](#e6) — archiving a profile that has employment history. Use [#17](#e17) |
| `IDENTITY_ALREADY_RECORDED` | 409 | [#24](#e24) — **and the message never names whose** |
| `ENCRYPTION_UNAVAILABLE` | 503 | phase 6, until [open item 1](#1-nothing-can-encrypt-anything--this-blocks-three-documents) is done |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

Written in full for phase 1, which is what gets built first. The rest carry enough to be
reviewed, and are expanded as their phase is reached — the same way this project's other plans
grew.

## The people — writes · 1–6

<a id="e1"></a>
**[1](#t1) · `POST /staff`**

- [`staff`](../../models/people/staff/Staff.java) — *insert*: `schoolId`, `employeeNo` **generated**, `fullName`, and whatever personal fields were sent
- **`employeeNo` is generated, never accepted.** `NumberSequenceService.next(schoolId, EMPLOYEE_NUMBER, …)` — which is already built and already used by school creation and subscriptions. A caller-supplied employee number would let two schools' conventions collide inside one tenant, and there is no reason a person should pick their own staff number.
- **No `active`, no status, no department.** A person is not employed by existing; that is [#16](#e16). This endpoint creates the human being.
- **Phone and email are normalised on the way in** — international format, trimmed lowercase — per the model README. A duplicate email is **not** refused: two staff members genuinely may share a family address, and nothing references an email.
- **The only required field is `fullName`.** Everything else can arrive later through [#2](#e2), because a school entering two hundred people at the start of term has a name and an employee number and nothing else on day one.
- **Returns the generated `employeeNo` prominently.** It is the thing the school writes down.

<a id="e2"></a>
**[2](#t2) · `PATCH /staff/{id}`**

- [`staff`](../../models/people/staff/Staff.java) — *updates*: `fullName`, `dateOfBirth`, `gender`, `nationalityCode`, `preferredLanguage`, `phoneNumber`, `emailAddress`
- **Never `employeeNo`.** It is generated and printed on things; a rename would leave a paper trail pointing at nobody.
- **Not the addresses, not the emergency contact, not the photo** — [#3](#e3), [#4](#e4), [#5](#e5). Each is replaced whole for the same reason the school's address is: half a changed address is a delivery to the wrong place.
- **An empty body is `400 NOTHING_TO_UPDATE`.**

<a id="e3"></a>
**[3](#t3) · `PUT /staff/{id}/addresses`**

- *updates*: `currentAddress`, `permanentAddress`
- **Both, as a pair, because "same as current" is a real answer** and expressing it as two independent PATCHes means a window where they disagree.
- Either may be cleared; a person with no recorded address is a normal state.

<a id="e4"></a>
**[4](#t4) · `PUT /staff/{id}/emergency-contact`**

- *updates*: `emergencyContact`
- **Whole or not at all.** A contact with a new name and an old phone number is worse than no contact, because somebody will trust it in the one situation where it matters.

<a id="e5"></a>
**[5](#t5) · `PUT /staff/{id}/photo`**

- *updates*: `profileImageDocsId`, or clears it
- **A `DocumentRecord` id, validated to exist and to belong to this school.** The file itself is object storage's; this stores a pointer, the same arrangement `CurriculumDocument` uses.

<a id="e6"></a>
**[6](#t6) · `POST /staff/{id}/archive`**

- *updates*: `recordState` = `ARCHIVED`, `archivedAt` — the `SchoolBase` lifecycle, not a new field
- **This is for a profile created by mistake, and nothing else.** A duplicate entry, a test record, a name typed into the wrong school.
- **Refused if the person has any employment record** — `409 STAFF_STILL_EMPLOYED`. Somebody who worked here leaves through [#17](#e17) and keeps their history; archiving them would hide a person whose payslips and attendance still reference them.
- **Not a delete, and not reversible through this endpoint.** Unarchiving is deliberately missing until somebody needs it.

## The people — reads · 7–8

<a id="e7"></a>
**[7](#t7) · `GET /staff`**

- [`employment_records`](../../models/people/staff/EmploymentRecord.java) — *reads*: the filtered, paged set of **current** records
- [`staff`](../../models/people/staff/Staff.java) — *reads*: the people those records name
- **The list behind every teacher picker in the product**, which is what decides its filters: `?employed=`, `?departmentDocsId=`, `?positionDocsId=`, `?employmentType=`, `?search=` on name or employee number.
- **Two queries, and the paging happens on the first.** Every filter that narrows the result lives on `employment_records`, so paging `staff` instead would give pages that shrink after filtering. See [open item 3](#3-7s-filters-need-a-join-mongo-will-not-do).
- **`?employed=false` returns leavers**, and absent returns both — the tristate rule this project uses everywhere. **A person with no employment record at all appears only when the filter is absent**, and the response says so, because an empty filtered page otherwise reads as "this person does not exist".
- **The row is deliberately thin**: name, employee number, position title, department name, employment type. **No date of birth, no address, no phone.** Those are [#8](#e8), one call away, and a list endpoint that returned them would put every employee's personal data in every dropdown's network tab.
- **No gates.** A suspended school still reads its own staff list.

<a id="e8"></a>
**[8](#t8) · `GET /staff/{id}`**

- *reads*: the person, and their `current = true` employment record
- **The employment is folded in rather than linked**, because "who is this and what do they do" is one question and every caller would make the second call.
- **Returns the full profile** — addresses, emergency contact, date of birth. Which is exactly why [open item 2](#2-this-is-the-module-that-cannot-ship-without-authorization) is the one that matters.
- **Answers for a person with no employment record**, with the employment block absent rather than an error. That is the state [#1](#e1) leaves them in.

## The organization · 9–15

<a id="e9"></a>
**[9](#t9) · `POST /departments`** — `departmentCode` given not derived, unique per school; `parentDepartmentDocsId` optional and validated to exist; `headStaffDocsId` optional and validated to be a `Staff` of this school. `active` = `true`, never accepted.

<a id="e10"></a>
**[10](#t10) · `PATCH /departments/{id}`** — `name`, `parentDepartmentDocsId`, `headStaffDocsId`. **Never `departmentCode`.** **A cycle is refused** — `409 DEPARTMENT_CYCLE` — by walking up from the proposed parent; a department that is its own ancestor makes [#12](#e12) loop forever.

<a id="e11"></a>
**[11](#t11) · `POST /departments/{id}/deactivate` · `/reactivate`** — idempotent pair, no body, the shape every lifecycle flag in this project uses. **Refused while the department still holds active positions** — `409 DEPARTMENT_NOT_EMPTY` — because a retired unit with live seats is a state the org chart cannot draw.

<a id="e12"></a>
**[12](#t12) · `GET /departments`** — `?tree=true` returns the nesting, `?active=` filters, and the two together are the only interesting combination. **The tree is built in the service from one flat read**, not by recursive queries.

<a id="e13"></a>
**[13](#t13) · `POST /positions`** — inside an **active** department. `positionCode` unique per school. `approvedHeadcount` optional; null means uncapped.

<a id="e14"></a>
**[14](#t14) · `PATCH /positions/{id}`** — `title`, `approvedHeadcount`, `active`. **Lowering `approvedHeadcount` below the filled count is allowed with a `warning`**, not refused: a school reducing an approved seat count already over-filled is describing reality, and refusing it would make the number impossible to correct.

<a id="e15"></a>
**[15](#t15) · `GET /positions`** — with **`filledHeadcount` computed** from current employment records, never stored. A stored counter drifts the first time a writer forgets it — the same objection that keeps `Position` free of one and `AcademicTerm` free of a stored weight total.

## Employment · 16–19

<a id="e16"></a>
**[16](#t16) · `POST /staff/{id}/employment`**

- **Hire, promote and transfer are one endpoint**, because they are one write: close the previous current record, open a new one. Three endpoints doing that would be three chances to leave two records current.
- **Closes the previous record in the same write** — sets `current = false` and `effectiveUntil` to the day before the new `effectiveFrom`. The partial unique index on `{schoolId, staffDocsId, current}` filtered to `true` is the backstop, and this check is what turns a duplicate-key 500 into a readable 409.
- **Refuses a retired position** — `409 POSITION_NOT_ACTIVE`.
- **`POSITION_FULL` is a warning, not a refusal**, when `approvedHeadcount` is reached. A school hiring a twelfth teacher into eleven approved seats is a budget conversation, not a data error, and refusing it would stop the system recording something that has already happened. The same call the term-weight sum made.
- **Overlap with non-current records is not checked** — see [open item 4](#4-nothing-says-when-an-employment-record-may-overlap-another), which must be settled before this is built.
- **Needs a transaction, or the version field.** Two writes that must both land; see [open item 5](#5-a-leave-balance-is-arithmetic-and-the-model-stores-the-inputs) for the same problem in leave.

<a id="e17"></a>
**[17](#t17) · `POST /staff/{id}/separate`** — ends the current record: `current` = `false`, `effectiveUntil`, `status`, `separationReason`. **The one way somebody leaves.** `409 NOT_EMPLOYED` if there is no current record. The person and all their history stay exactly where they are — which is the whole reason there is no `DELETE`.

<a id="e18"></a>
**[18](#t18) · `PATCH /employment/{id}`** — corrects `effectiveFrom`, `effectiveUntil`, `managerDocsId`, `probationUntil` on a record already written. **Never `current`** — that is [#16](#e16) and [#17](#e17), and letting a PATCH set it is how two records end up current.

<a id="e19"></a>
**[19](#t19) · `GET /staff/{id}/employment`** — the history, newest first, with the current one marked. Not paged: nobody has a hundred employment records.

## Credentials, identities and bank accounts · 20–29

<a id="e20"></a><a id="e21"></a><a id="e22"></a><a id="e23"></a>
**[20](#t20)–[23](#t23) · credentials** — [#20](#e20) records one against a staff member; [#21](#e21) sets `verificationStatus` with who and when; [#22](#e22) lists one person's with expiry state **computed from `validUntil` rather than stored**; [#23](#e23) is the compliance report — everything lapsing inside `?withinDays=`, which is the query an inspection actually asks. A credential **number** is encrypted and so [#20](#e20) is phase 6; the rest are not.

<a id="e24"></a><a id="e25"></a><a id="e26"></a>
**[24](#t24)–[26](#t26) · government identities — phase 6, blocked**

- [#24](#e24) encrypts on the way in and stores the keyed hash beside it. **The duplicate check is on the hash and the refusal never names the other person** — `409 IDENTITY_ALREADY_RECORDED` and nothing more, or the endpoint becomes a way to enumerate staff by guessing numbers.
- [#25](#e25) returns **masked only**, at every permission level. The plaintext is not on this endpoint.
- [#26](#e26) is the recovery, and it is a **POST** — revealing a national identity number is an event that must be recorded, and a GET that writes an audit row is a GET that lies about being retryable.

<a id="e27"></a><a id="e28"></a><a id="e29"></a>
**[27](#t27)–[29](#t29) · bank accounts — phase 6, blocked** — same encryption triple. **One primary per staff member**, enforced by a partial unique index filtered to `{primaryAccount: true, active: true}` — which is correctly written, unlike the two index bugs this project has already found. [#28](#e28) records a penny-drop or document verification; payroll should refuse to pay an unverified account, and that is payroll's rule to make.

## Leave · 30–39

<a id="e30"></a><a id="e31"></a><a id="e32"></a>
**[30](#t30)–[32](#t32) · policy** — `leaveTypeCode` unique per school, given not derived. **[#31](#e31) changing an allowance does not touch existing balances**: a balance is opened from the policy once, by [#33](#e33), and then belongs to the year. Changing the policy mid-year and having everyone's entitlement silently move is the bug this separation prevents.

<a id="e33"></a>
**[33](#t33) · `POST /leave-balances/open-year`** — one write per staff member per leave type for one `academicYear`, carrying forward what the policy allows. **Idempotent by the unique key** `{schoolId, academicYear, staffDocsId, leaveTypeDocsId}` — running it twice adds nothing, which is what makes it safe to re-run after adding a staff member mid-year.

<a id="e34"></a>
**[34](#t34) · `POST /leave-balances/{id}/adjust`** — `adjustmentDays`, positive or negative, **with a required reason**. Never a silent edit of `allocatedDays`: the allocation is what the policy gave, and an adjustment is a decision somebody made.

<a id="e35"></a>
**[35](#t35) · `GET /staff/{id}/leave-balances`** — with `available` **computed on every read**, never stored. Five inputs, one derivation; storing it would go stale the instant a request was approved.

<a id="e36"></a><a id="e37"></a><a id="e38"></a><a id="e39"></a>
**[36](#t36)–[39](#t39) · requests** — [#36](#e36) reserves days into `pendingDays`; [#37](#e37) moves them to `usedDays`; [#38](#e38) releases them. **Each is two documents that must move together**, and this project has no transaction manager configured — see [open item 5](#5-a-leave-balance-is-arithmetic-and-the-model-stores-the-inputs), which must be settled before [#36](#e36) is built. [#39](#e39) is the approver's queue, served by `school_leave_decision_queue_idx`.

## Reviews and development · 40–44

<a id="e40"></a><a id="e41"></a>
**[40](#t40)–[41](#t41) · cycles** — criteria are **embedded and versioned with the cycle**, so a criterion reworded next year does not change what last year's scores meant. [#41](#e41) is the open/close pair; submissions are refused outside it. **The [reviews plan](reviews/README.md) expands this into four transitions**, because the status enum has five states.

<a id="e42"></a>
**[42](#t42) · `POST /review-cycles/{id}/reviews`** — one per reviewer per reviewee per cycle, enforced by the unique index on `{schoolId, reviewCycleDocsId, reviewedStaffDocsId, reviewerType, reviewerLookupHash}`. **Anonymous submissions carry the hash and omit the id** — and [open item 7](#7-a-reviews-anonymity-is-enforced-by-one-hash-and-nothing-rotates-it) is why that is weaker than it looks.

<a id="e43"></a>
**[43](#t43) · `GET /review-cycles/{id}/reviews`** — **aggregate or raw is a policy decision, not a query parameter.** A reviewee seeing five anonymous scores can often identify the outlier; whether they may is the school's rule, and an endpoint that took `?raw=true` would be handing that decision to whoever writes the client.

<a id="e44"></a>
**[44](#t44) · `POST /staff/{id}/development`** — training recorded with cost, learning hours and an optional impact evaluation. Nothing else references it, which is why it is last. **The [development plan](development/README.md) expands this into five endpoints**: the model carries a six-state approval workflow — `cost`, `approvedByDocsId` and a `PENDING_APPROVAL` state — which makes this a spend request before it is a history entry.
