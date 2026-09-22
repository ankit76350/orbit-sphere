# controllers/crm — API plan

**Seven of thirty-four are built — the whole cycle half except [#7](#e7), plus the first
application endpoint.**
[#1](#e1) opens a year for admissions, [#2](#e2) corrects one, [#3](#e3) moves it through its
lifecycle, [#4](#e4) sets its seats, [#5](#t5) lists the rounds and [#6](#e6) opens one in full.

**A family can now apply.** [#17](#e17) takes a form against an open cycle. Next is
[#19](#e19) — submitting it, which freezes the snapshot — then the reads [#24](#t24) and
[#25](#t25). [#7](#e7) needs applications to count, so it comes after those.

The rest is the full set of endpoints the admissions feature needs, written before
any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md), [`controllers/plans`](../plans/README.md),
[`controllers/people`](../people/README.md) and
[`controllers/academics/timetable`](../academics/timetable/README.md) were done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/crm`](../../models/crm) — five collections, three embedded types, seven enums.
That package's README is a **persistence contract**; this file is what may be done to it over HTTP.

> **Six things were measured before writing this — 2026-09-18.** The models were read as written and
> checked against the rest of the codebase rather than taken on trust.
>
> **1. The running-year gate cannot apply to this module.** Every write in `academics` runs
> `requireYearMarkedAsRunning`. **Admissions is the one module whose entire purpose is a year that
> is not running yet**: a school opens its 2027-2028 cycle in the middle of 2026-2027, and often
> works both at once — late admissions into the running year while the next year's cycle is open.
> Gate 4 would refuse the module's normal case. See [gates](#which-gates-every-endpoint-runs).
>
> **2. The business numbers are already provisioned.**
> [`NumberSequenceType`](../../models/institution/enums/NumberSequenceType.java) already carries
> `ADMISSION_INQUIRY`, `ADMISSION_APPLICATION`, `ADMISSION_OFFER` and `STUDENT_ADMISSION`, and
> [`NumberSequenceService.next`](../../services/institution/NumberSequenceService.java) is the
> same call `StaffService` makes for `employeeNo`. Nothing needs inventing for `inquiryNo`,
> `applicationNo` or `offerNo`.
>
> **3. The application↔student link is stored twice, and BOTH sides are enforced.**
> ~~`Student.admissionApplicationDocsId` has no index at all.~~ **That was wrong, and it was wrong
> when this file was written — corrected 2026-09-21.**
> [`Student`](../../models/student/Student.java) carries `school_admission_application_uniq`,
> unique on `{schoolId, admissionApplicationDocsId}` and partial on the field existing, added
> 2026-08-22 — a month *before* this plan claimed it was missing.
> `AdmissionApplication.resultingStudentDocsId` has the matching partial-unique index.
>
> **So the link cannot be doubled from either side**, and the consequence for [#33](#e33) is the
> opposite of what open item 3 originally said: nothing needs adding, and **two indexes can now
> refuse the same write**. See [open item 3](#3-the-applicationstudent-link).
>
> **4. One inquiry can produce only one application per cycle.**
> `school_cycle_inquiry_uniq` is unique on `{schoolId, admissionCycleDocsId, inquiryDocsId}`,
> partial on `inquiryDocsId` existing. That is a real product rule and it is not written down
> anywhere: a family enquiring once and applying for **two classes** in one cycle cannot be
> recorded. See [open item 2](#2-one-inquiry-one-application-per-cycle).
>
> **5. Five inquiry statuses and three application statuses exist in the enums but not in the
> model README's workflow.** `InquiryStatus` has `COUNSELLING`, `VISIT_SCHEDULED`, `VISITED`,
> `LOST` and `CLOSED`; the README's diagram shows four of nine. `AdmissionApplicationStatus` adds
> `ADDITIONAL_INFORMATION_REQUIRED` and `WITHDRAWN`; `AdmissionOfferStatus` adds `SUPERSEDED`.
> **The endpoints below are the only thing that can define the legal moves**, so the transition
> tables here are the specification, not the diagram there.
>
> **6. `models/student` is not built either** — no service, repository, controller or DTO. The
> enrollment write ([#33](#e33)) creates a `Student`, so **it is blocked on a module outside this
> one**, and it is the only endpoint here that is.
>
> **Since 2026-09-21 that module has a plan too** — [`controllers/student`](../student/README.md) —
> and the two are now built **interleaved**, with the order in
> [`controllers/README.md`](../README.md). [#33](#e33) is phase 6 of ten. See
> [open item 1](#1-enrollment-is-blocked-on-a-module-that-does-not-exist).

---

## What this module is

**Everything that happens before a child is a student.** A parent enquires; the school follows it
up; an application is submitted against an open cycle; somebody reviews it; an offer is issued and
answered; and on acceptance the applicant becomes a `Student` and stops being this module's
business.

**It is a pipeline, and its state lives in three places on purpose.** The `Inquiry` tracks the
*lead*, the `AdmissionApplication` tracks the *application*, and the `AdmissionOffer` tracks the
*offer* — each with its own status enum. A single status field across all three would make
"withdrawn after an offer was issued but before it was answered" unrepresentable.

**Snapshots, not references, once an application is submitted.** Guardian and applicant details are
copied into the application at submission. Editing the inquiry afterwards must not rewrite an
application the school has already acted on — the model README says so, and
[#17](#e17) and [#18](#t18) are where it is enforced.

## What this module is not

- **Not the student record.** [`Student`](../../models/student/Student.java) and
  [`StudentAcademicRecord`](../../models/student/StudentAcademicRecord.java) are their own module,
  planned in [`controllers/student`](../student/README.md). This module creates a student once, at
  [#33](#e33), and then points at it.
- **Not fees.** An admission deposit is a `FeeInvoice` in `finance`. The offer stores
  `depositInvoiceDocsId` and nothing else about money.
- **Not documents.** Evidence uploads are `DocumentRecord` ids. This module stores the ids;
  `documents` owns the files.
- **Not the application form builder.** There is no form definition model, and the three fields
  that named one were deleted on 2026-09-21. `formAnswers` is an unvalidated map — see
  [open item 4](#4-formanswers-is-an-unvalidated-map).
- **Not a CRM in the sales sense.** No campaigns, no lead scoring, no email sequences. A follow-up
  is a dated note with a channel on it.

## One surface, and why

```text
/schools/current/admission-cycles
/schools/current/inquiries
/schools/current/applications
/schools/current/reviews
/schools/current/offers
```

**No `{year}` in the path**, unlike every route in [`academics`](../academics/timetable/README.md).
That is the deliberate opposite of the decision timetable reached, and for a reason that only
applies here:

> **In `academics` the year is the scope. In admissions it is a property.** Every class, term and
> timetable belongs to the year the school is running. An admission cycle belongs to a year the
> school is **not** running yet, and a school routinely has two live at once — late admissions into
> 2026-2027 while the 2027-2028 cycle is open. A path segment would state a scope this module does
> not have, and would make "show me both" unaddressable.

So `academicYear` is a **required field** on a cycle and an inquiry, and a **filter** on every list.
Applications, reviews and offers do not carry one at all: an application's year is whichever year
its cycle names, and storing it again would be two sources for one fact.

**Applications, reviews and offers are addressed by their own id**, not nested under the cycle.
`/applications/{id}` rather than `/admission-cycles/{cycleId}/applications/{id}`: an admission
officer opens an application from a worklist and a search result far more often than by walking down
from a cycle, and a nested address would make the common call carry an id nobody had.

**Writes that are events get a verb.** `POST /applications/{id}/submit` rather than a `PATCH` that
sets `status`. The status moves are not interchangeable field edits — each has its own
preconditions, its own side effects and its own refusals — and a single `PATCH status` endpoint
would be nine endpoints wearing one name. The same call [`people` #18b](../people/staff/README.md)
made for employment status.

## Which gates every endpoint runs

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | every write |
| **2** | Is the subscription usable | every write |
| **4** | Is the year the running one | **never** |

**Gate 4 never runs in this module, and that is the finding worth repeating.** Every other module
treats a write against a non-running year as a mistake. Here it is the *normal case*: a cycle for
2027-2028 is created, opened, filled and enrolled from entirely inside 2026-2027. An endpoint here
that called `requireYearMarkedAsRunning` would refuse the work the module exists to do.

**What replaces it** is the cycle's own status. An application cannot be submitted into a cycle that
is not `OPEN`, and that check — [`CYCLE_NOT_OPEN`](#the-refusal-codes-this-module-introduces) — is
this module's equivalent of gate 4. It is a check on the *cycle*, not on the year.

**No gate runs on a read.** A suspended school still reads last year's admissions, because the
students it enrolled are still enrolled.

---

# The endpoints

Numbered by area, not by build order. **Build order is below** and differs.

## 1. The cycle — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /admission-cycles`](#e1) | Open a year for admissions. **The first call anyone makes.** | [`admission_cycles`](../../models/crm/AdmissionCycle.java) |
| <a id="t2"></a>2 — **built** | [`PATCH /admission-cycles/{id}`](#e2) | Correct its name, dates or notes. | `admission_cycles` |
| <a id="t3"></a>3 — **built** | [`POST /admission-cycles/{id}/status`](#e3) | Move it through `DRAFT → SCHEDULED → OPEN → CLOSED → COMPLETED`. | `admission_cycles` |
| <a id="t4"></a>4 — **built** | [`PUT /admission-cycles/{id}/capacities`](#e4) | Set the seat table, whole. | `admission_cycles` |

## 2. The cycle — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t5"></a>5 — **built** | [`GET /admission-cycles`](#t5) | Every cycle, filtered by year and status. | `admission_cycles` |
| <a id="t6"></a>6 — **built** | [`GET /admission-cycles/{id}`](#e6) | One cycle in full, with its seat table. | `admission_cycles`, `school_classes` |
| <a id="t7"></a>7 | [`GET /admission-cycles/{id}/capacity`](#e7) | **Seats against applications** — offered, enrolled, waitlisted, free. | `admission_cycles`, `admission_applications` |

## 3. The lead — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t8"></a>8 | [`POST /inquiries`](#e8) | Capture a lead. **The front desk's call.** | [`inquiries`](../../models/crm/Inquiry.java) |
| <a id="t9"></a>9 | [`PATCH /inquiries/{id}`](#t9) | Correct the child's details or the guardians. | `inquiries` |
| <a id="t10"></a>10 | [`POST /inquiries/{id}/follow-ups`](#e10) | Log one interaction. `$push`, and it moves `nextFollowUpAt`. | `inquiries` |
| <a id="t11"></a>11 | [`POST /inquiries/{id}/assign`](#t11) | Give the lead to a counsellor. | `inquiries`, `staff` |
| <a id="t12"></a>12 | [`POST /inquiries/{id}/status`](#t12) | Move it, including `LOST` with a reason. | `inquiries` |

## 4. The lead — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t13"></a>13 | [`GET /inquiries`](#e13) | **The counsellor's worklist** — whose, what state, what is overdue. | `inquiries` |
| <a id="t14"></a>14 | [`GET /inquiries/{id}`](#t14) | One lead with its whole timeline. | `inquiries` |
| <a id="t15"></a>15 | [`GET /inquiries/search?phone=&email=`](#e15) | **Is this family already known?** Asked before every new lead. | `inquiries` |
| <a id="t16"></a>16 | [`GET /inquiries/{id}/applications`](#t16) | What the lead became. | `admission_applications` |

## 5. The application — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t17"></a>17 — **built** | [`POST /applications`](#e17) | Start one, optionally from an inquiry. | [`admission_applications`](../../models/crm/AdmissionApplication.java) |
| <a id="t18"></a>18 | [`PATCH /applications/{id}`](#t18) | Edit it **while it is still `DRAFT`**. | `admission_applications` |
| <a id="t19"></a>19 | [`POST /applications/{id}/submit`](#e19) | `DRAFT → SUBMITTED`. **Freezes the snapshot.** | `admission_applications`, `inquiries` |
| <a id="t20"></a>20 | [`POST /applications/{id}/decision`](#e20) | The review outcome: approve, reject, waitlist, ask for more. | `admission_applications` |
| <a id="t21"></a>21 | [`POST /applications/{id}/withdraw`](#t21) | The family pulls out. | `admission_applications` |
| <a id="t22"></a>22 | [`POST /applications/{id}/assign`](#t22) | Give it to an admission officer. | `admission_applications`, `staff` |
| <a id="t23"></a>23 | [`PUT /applications/{id}/documents`](#t23) | Attach or replace the evidence list. | `admission_applications`, `document_records` |

## 6. The application — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t24"></a>24 | [`GET /applications`](#t24) | **The pipeline.** Filtered by cycle, class, status, officer. | `admission_applications` |
| <a id="t25"></a>25 | [`GET /applications/{id}`](#t25) | One application in full, with its reviews and offers. | `admission_applications`, `admission_reviews`, `admission_offers` |

## 7. The review · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t26"></a>26 | [`POST /applications/{id}/reviews`](#t26) | Assign a reviewer for a round. | [`admission_reviews`](../../models/crm/AdmissionReview.java) |
| <a id="t27"></a>27 | [`PATCH /reviews/{id}`](#t27) | **Submit the result** — score, criteria, recommendation. | `admission_reviews` |
| <a id="t28"></a>28 | [`GET /reviews`](#t28) | **A reviewer's own queue.** What is due, and when. | `admission_reviews` |

## 8. The offer · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t29"></a>29 | [`POST /applications/{id}/offers`](#e29) | Issue an offer. A later one **supersedes** the last. | [`admission_offers`](../../models/crm/AdmissionOffer.java) |
| <a id="t30"></a>30 | [`POST /offers/{id}/respond`](#e30) | The family answers: accepted or declined. | `admission_offers`, `admission_applications` |
| <a id="t31"></a>31 | [`POST /offers/{id}/withdraw`](#e31) | The school takes it back, with a reason. | `admission_offers` |
| <a id="t32"></a>32 | [`GET /offers`](#t32) | **What is expiring.** The chase list. | `admission_offers` |

## 9. Enrollment · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t33"></a>33 | [`POST /applications/{id}/enroll`](#e33) | **The whole point.** Applicant becomes a `Student`. | `admission_applications`, `admission_offers`, `inquiries`, `students` |

## 10. The numbers · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t34"></a>34 | [`GET /admission-funnel`](#e34) | Leads in, applications, offers, enrolments — per cycle. | `inquiries`, `admission_applications`, `admission_offers` |

---

# Build order

**The authoritative order is [`controllers/README.md`](../README.md#the-order)**, because this
module and [`student`](../student/README.md) are built interleaved — [#33](#e33) is the join, and it
sits between two `student` phases.

This module's own endpoints, ordered by **what they unblock** rather than by number:

| This module's phase | Cross-module phase | What it gives you | Endpoints |
|---|---|---|---|
| **1** | [1](../README.md#the-phases) | A cycle exists and can be read back | 1, 5, 6, 3 |
| **2** | [2](../README.md#the-phases) | Applications can be taken and seen | 17, 19, 24, 25 |
| **3** | [3](../README.md#the-phases) | The pipeline can be worked | 20, 26, 27, 28, 22 |
| **4** | [4](../README.md#the-phases) | Offers can be made and answered — **and here it stops** | 29, 30, 32, 31 |
| **5** | [6](../README.md#the-phases) | A student comes out of the other end | 33 |
| **6** | [9](../README.md#the-phases) | The lead half, which nothing else needs | 8, 13, 14, 10, 12, 11, 9, 15, 16 |
| **7** | [10](../README.md#the-phases) | The rest | 2, 4, 7, 18, 21, 23, 34 |

**#1 first, and nothing else works without it.** Every application names a cycle, and
[#17](#e17) refuses without one.

**Phase 4 is where this module runs out of road.** An application can reach `OFFER_ACCEPTED` and go
no further, because the next thing that happens to it is becoming a child on a register. That is the
moment [`student`](../student/README.md) gets built — four endpoints, just enough for [#33](#e33).

**The inquiry half is phase 6, not phase 1** — which looks backwards, since a lead comes before an
application in real life. It is deliberate: **an application does not need an inquiry**
(`inquiryDocsId` is nullable, for the family that walks in with a completed form), so the pipeline
is testable end to end without a single lead in the database. Building leads first would mean four
endpoints nothing else depends on before the module does anything. It is also **the one block that
is free to move earlier** if the lead-first experience is wanted sooner.

**#33 is no longer "blocked", it is scheduled.** See
[open item 1](#1-enrollment-is-blocked-on-a-module-that-does-not-exist).

**#7 and #34 are last of all.** Both are aggregations over applications, and both are much easier to
write once there is a realistic spread of statuses to aggregate.
---

# Things this module deliberately will not have

- **No `DELETE` on anything.** An inquiry that came to nothing is `LOST`; an application is
  `WITHDRAWN`; an offer is `WITHDRAWN` or `EXPIRED`. Admissions is a record of what a school
  decided about a child, and the decision not to admit is exactly the part worth keeping.
- **No bulk import.** A CSV of two hundred leads is a real request and it is not an endpoint — it is
  a job, with a file, a dry run and a report. It belongs with the other imports when those exist.
- **No merge.** Two inquiries for one family is common and the answer is
  [#15](#e15) — find the existing one before creating a second — not a merge tool that has to
  reconcile two timelines.
- **No public-facing application submission.** Everything here is staff-authenticated. A parent
  portal is a different surface with a different auth story, and putting it behind the same
  controller would be the shortcut that makes that impossible later.
- **No offer letter generation.** `offerDocumentDocsId` points at a `DocumentRecord` that something
  else produced.

---

# To settle before building

## 1. Enrollment is blocked on a module that does not exist

**Settled 2026-09-21.** Kept here because the reasoning is what the build order rests on.

[#33](#e33) must create a `Student`, set `admissionNo` from `NumberSequenceType.STUDENT_ADMISSION`,
copy the guardians, and link both directions. `models/student` still has no repository, service, DTO
or controller.

**The options were:** build the student module first and treat #33 as part of it; or build #33 here
and let it write `students` through a repository this module owns — which puts a second module's
collection under this module's service and is the thing the folder rules exist to prevent.

**What was decided:** neither, exactly. The two modules are built **interleaved** —
[`controllers/README.md`](../README.md#the-order). This module runs to phase 4, where an application
can reach `OFFER_ACCEPTED` and go no further; the four `student` endpoints that #33 actually needs
are phase 5; **#33 is phase 6**, and it calls `StudentService` rather than owning `students`.

**Why that beats "student first":** everything up to `OFFER_ACCEPTED` is buildable today, so
building the student module first would leave four `crm` phases waiting on a module that does not
need them. And it beats "#33 here" because the folder rule stands.

The rest of the shape was already decided — the project's recorded flow is
*inquiry → admission → student → academic record, with the student created first without an
academic year*, which is exactly what [`student` #1](../student/README.md#e1) and
[#14](../student/README.md#e14) are split along.

## 2. One inquiry, one application per cycle

**Settled 2026-09-22: the rule stands, and [#17](#e17) enforces it.**

`school_cycle_inquiry_uniq` was always going to enforce it; the question was whether to keep the
index. **Kept**, because an `Inquiry` carries the prospective child's own `prospectiveStudentName`,
`dateOfBirth` and `gender` — it is **per child**, not per family. So "one inquiry, one application
per cycle" reads as *one child applies once per round*, which is right. Siblings are naturally two
inquiries, because they are two children.

[#17](#e17) checks before writing and answers `APPLICATION_ALREADY_EXISTS` with a message saying a
second child needs their own inquiry — so the index never fires as a duplicate-key 500.

**The same inquiry in a different cycle is allowed**, which is what the index's third key gives.

**To reverse it:** delete the check in `createApplication` and the index. One line and one
annotation. The case it would unlock is one child applying for two classes in one round, which a
school normally expresses by applying once and letting the school place them.

## 3. The application↔student link

**Rewritten 2026-09-21 — the original premise was wrong.** It said
`Student.admissionApplicationDocsId` had no index and that something needed adding. It has had
`school_admission_application_uniq` (unique, partial on the field existing) since 2026-08-22.
`AdmissionApplication.resultingStudentDocsId` has the matching one. **Nothing needs adding.**

The real question is the opposite one. [#33](#e33) writes **both** sides in one transaction, and
**both sides can now refuse it**:

- a second application naming a student that already has one → the student's index fires;
- a second student naming an application that already has one → the application's index fires.

**Decide:** which of those two duplicate-key errors becomes `ALREADY_ENROLLED` and which becomes
something else — they are different mistakes. The first is "this child is already enrolled"; the
second is "this application already produced a child". **Recommendation:** check the application's
`resultingStudentDocsId` first and refuse `ALREADY_ENROLLED` before writing anything, so the
transaction is never entered for the common case, and treat either index firing afterwards as
`CONCURRENT_MODIFICATION` — because if the pre-check passed, a race is the only way left to get
there.

## 4. `formAnswers` is an unvalidated map

**Settled 2026-09-21: the two fields that named a form definition were deleted.**
`AdmissionCycle.applicationFormDefinitionDocsId`,
`AdmissionApplication.applicationFormDefinitionDocsId` and `applicationFormVersion` pointed at a
model that was never built. Three fields naming nothing, and a model README instructing the service
layer to validate against them — an instruction that could not be followed.

`formAnswers` stays, as `Map<String, Object>`. It is the answers themselves rather than a pointer to
anything missing, it works on its own, and a school still needs somewhere to put "previous school".

**So [#17](#e17) and [#18](#t18) accept the map as given and cap its size.** Nothing can check that
the answers match the questions, that required ones are present, or that a number is a number, and
**nothing should pretend to** — validation rules invented here are rules the form builder will later
contradict.

**What this costs:** an application no longer records which questions it was answering. A school
that changes its form mid-cycle cannot tell, later, which version an application answered. That is
a real loss and it is the reason the fields existed; it is also unrecoverable today, because there
was no form definition to version in the first place.

## 5. A cycle's dates are not checked against the academic year

`AdmissionCycle` carries four `Instant`s and an `academicYear` string. Nothing relates them. A cycle
for 2027-2028 whose `applicationCloseAt` falls in 2025 is storable.

**Decide:** whether [#1](#e1) validates the dates against
[`AcademicYear`](../../models/core/AcademicYear.java)'s own range, and how strictly — admissions
legitimately open a year *before* the year starts, so it cannot simply be "inside the year".
Ordering the four instants against each other is uncontroversial and should happen regardless.

## 6. Nothing stops two cycles being `OPEN` for one year

There is no index or rule preventing it. It may be correct — a school could run a general cycle and
a separate one for a scholarship intake — but [#17](#e17) has to name a cycle explicitly either way,
so the question is only whether [#3](#t3) warns.

## 7. `reviewerRole` is a free string

`AdmissionReview.reviewerRole` is `@NotBlank String`, not an enum, while every other closed set in
this project is one. `"ADMISSION_OFFICER"` today, `"admission officer"` tomorrow, and the review
sheet groups by a field that no longer groups.

**Decide:** promote it to an enum — the project's stated rule for closed sets — or document that it
is deliberately open because schools name their panels differently.

---

# Where the code will live

```text
controllers/crm/
    AdmissionCycleController.java     #1–#7
    InquiryController.java            #8–#16
    AdmissionApplicationController.java  #17–#25, #33
    AdmissionReviewController.java    #26–#28
    AdmissionOfferController.java     #29–#32

services/crm/
    AdmissionCycleService.java
    InquiryService.java
    AdmissionApplicationService.java
    AdmissionReviewService.java
    AdmissionOfferService.java
    utils/    the reads each service makes more than once
    helper/   the rules MongoDB cannot express

repositories/crm/{inquiry,admissioncycle,admissionapplication,admissionreview,admissionoffer}/
dto/crm/{inquiry,admissioncycle,admissionapplication,admissionreview,admissionoffer}/{request,response}/
```

**Five controllers, not one.** `people` put departments and positions in one controller and it grew
to fifteen endpoints across two documents — which is the file this project just renamed because
nobody could say what it was about. Five collections get five.

**One helper per service, and a helper never calls another helper.** The rules that will live there:
the status transition tables, the "an application's class must belong to its cycle's year" check,
and the snapshot rule.

---

# The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `ADMISSION_CYCLE_NOT_FOUND` | 404 | No cycle with that id in this school. |
| `CYCLE_NAME_TAKEN` | 409 | That year already has a cycle of that name. |
| `APPLICATIONS_NOT_OPEN_YET` | 409 | [#17](#e17) — the cycle is `OPEN` but its published `applicationOpenAt` has not arrived. |
| `APPLICATIONS_CLOSED` | 409 | [#17](#e17) — the cycle is still `OPEN` but its published `applicationCloseAt` has passed. Nobody closed it. |
| `CYCLE_NOT_OPEN` | 409 | [#17](#e17)/[#19](#e19) against a cycle that is not `OPEN`. **This module's gate 4.** |
| `CYCLE_HAS_NO_SEATS` | 409 | [#3](#e3) opening a cycle whose seat table is empty. |
| `INVALID_CYCLE_TRANSITION` | 409 | [#3](#t3) asked for a move the status graph does not have. |
| `BLANK_CYCLE_NAME` | 400 | [#2](#e2) sent `name: ""`. A cycle needs one. |
| `UNKNOWN_CLEAR_FIELD` | 400 | [#2](#e2)'s `clear` names something that is not clearable. |
| `CLEAR_CONFLICTS_WITH_VALUE` | 400 | [#2](#e2) both set and cleared one field. |
| `NOTHING_TO_UPDATE` | 400 | [#2](#e2)'s body moves nothing. |
| `CYCLE_DATES_OUT_OF_ORDER` | 400 | Open after close, or enrollment deadline before either. |
| `INQUIRY_NOT_FOUND` | 404 | No inquiry with that id in this school. |
| `INVALID_INQUIRY_TRANSITION` | 409 | [#12](#t12) asked for a move the status graph does not have. |
| `LOST_REASON_REQUIRED` | 400 | Moving to `LOST` without saying why. |
| `INQUIRY_NOT_FOUND` | 404 | An inquiry that is not this school's. Shared with the lead endpoints when they are built. |
| `TOO_MANY_FORM_ANSWERS` | 400 | [#17](#e17) sent more answers than the cap. Nothing can validate what they are, so the count is all that can be bounded. |
| `APPLICATION_NOT_FOUND` | 404 | No application with that id in this school. |
| `APPLICATION_ALREADY_EXISTS` | 409 | That inquiry already has an application in that cycle. See [open item 2](#2-one-inquiry-one-application-per-cycle). |
| `APPLICATION_NOT_EDITABLE` | 409 | [#18](#t18) on anything past `DRAFT`. The snapshot is frozen. |
| `INVALID_APPLICATION_TRANSITION` | 409 | [#20](#e20)/[#21](#t21) asked for a move the status graph does not have. |
| `DUPLICATE_CAPACITY_CLASS` | 409 | [#4](#e4) listed one class twice. |
| `RESERVED_EXCEEDS_TOTAL` | 400 | [#4](#e4) reserved more seats than the class offers. |
| `CLASS_NOT_IN_CYCLE_YEAR` | 409 | The applied class belongs to a different academic year than the cycle. |
| `CLASS_NOT_IN_CAPACITY` | 409 | The cycle's seat table does not list that class. |
| `REVIEW_NOT_FOUND` | 404 | No review with that id in this school. |
| `REVIEWER_ALREADY_ASSIGNED` | 409 | That reviewer already has that round of that application. |
| `REVIEW_ALREADY_COMPLETED` | 409 | [#27](#t27) on a review that is already `COMPLETED`. |
| `OFFER_NOT_FOUND` | 404 | No offer with that id in this school. |
| `APPLICATION_NOT_APPROVED` | 409 | [#29](#e29) on an application that has not been approved. |
| `OFFER_NOT_ANSWERABLE` | 409 | [#30](#e30) on an offer that is not `ISSUED`. |
| `OFFER_EXPIRED` | 409 | [#30](#e30) after `expiresAt`. |
| `OFFER_NOT_ACCEPTED` | 409 | [#33](#e33) without an accepted offer. |
| `ALREADY_ENROLLED` | 409 | [#33](#e33) on an application that already has a `resultingStudentDocsId`. |
| `SEATS_EXHAUSTED` | 409 | [#33](#e33) when the class's configured seats are full. |
| `STAFF_NOT_FOUND` | 404 | Shared. An assigned counsellor, officer or reviewer who is not this school's staff. |
| `CONCURRENT_MODIFICATION` | 409 | Shared. Another write changed the document first. |

---


## The four dates are required, and checked — 2026-09-22

**[#1](#e1) requires all four.** They were optional, on the grounds that a school often creates a
cycle before its calendar is settled. A round nobody can be told the dates of is not a round, and a
window with no ends cannot be checked.

**[#17](#e17) checks the application window**, not just the status. The two catch different
mistakes:

| | catches |
|---|---|
| the **status** being `OPEN` | a round nobody opened |
| `applicationOpenAt` ≤ now ≤ `applicationCloseAt` | a round nobody remembered to **close** |

A school that publishes "applications close 31 August" and forgets to move the cycle to `CLOSED` on
the 1st would otherwise keep taking forms.

**[#3](#e3) records the moment where the school published nothing**, and never overwrites a date
that is set. Since the four are now required at creation, that fill only ever applies to cycles made
**before this rule** — which is also the only place an absent date can still be found.

**[#2](#e2) can no longer clear a date.** Only `notes`. Emptying one would leave a cycle
[#1](#e1) would have refused to make; a date can be moved instead.

**One field, two facts.** The plan and the actual both want to live in these four and only one can.
Where a date is published, the plan wins and the actual is recorded nowhere — an `actualOpenedAt`
on the model is what would fix that.

---

# The status graphs

**These are the specification.** The model README's diagram shows a subset; the enums carry values
it does not mention, and an endpoint that accepted an undefined move would be inventing product.

## `AdmissionCycleStatus` — [#3](#t3)

```text
DRAFT ──> SCHEDULED ──> OPEN ──> CLOSED ──> COMPLETED
  │           │           │         │
  └───────────┴───────────┴─────────┴──> CANCELLED
```

`COMPLETED` is terminal. `CANCELLED` is terminal and reachable from anywhere before it.

## `InquiryStatus` — [#12](#t12)

```text
NEW ──> CONTACTED ──> COUNSELLING ──> VISIT_SCHEDULED ──> VISITED
                           │                                 │
                           └──────────┬──────────────────────┘
                                      v
                        APPLICATION_STARTED ──> APPLICATION_SUBMITTED ──> CLOSED
                                      
any non-terminal ──> LOST   (requires lostReason)
```

`APPLICATION_STARTED` and `APPLICATION_SUBMITTED` are set by [#17](#e17) and [#19](#e19), **not by
[#12](#t12)** — a lead's application state is a fact about the application, and letting a counsellor
type it would let the two disagree.

## `AdmissionApplicationStatus` — [#19](#e19), [#20](#e20), [#21](#t21), [#29](#e29), [#30](#e30), [#33](#e33)

```text
DRAFT ──> SUBMITTED ──> UNDER_REVIEW ──┬──> APPROVED ──> OFFERED ──> OFFER_ACCEPTED ──> ENROLLED
                            │          ├──> REJECTED
                            │          └──> WAITLISTED ──> APPROVED
                            v
              ADDITIONAL_INFORMATION_REQUIRED ──> UNDER_REVIEW

anything before ENROLLED ──> WITHDRAWN   (requires withdrawalReason)
```

Which endpoint owns which move is the point: `OFFERED` is [#29](#e29)'s side effect,
`OFFER_ACCEPTED` is [#30](#e30)'s, and `ENROLLED` is [#33](#e33)'s. **No endpoint sets these by
being told to** — they are consequences.

## `AdmissionOfferStatus` — [#29](#e29), [#30](#e30), [#31](#e31)

```text
DRAFT ──> ISSUED ──┬──> ACCEPTED
                   ├──> DECLINED
                   ├──> EXPIRED      (by time, not by a call)
                   └──> WITHDRAWN    (requires withdrawalReason)

ISSUED ──> SUPERSEDED   (when a later revision is issued)
```

**`EXPIRED` has no endpoint.** It is what `expiresAt` in the past means, and
[#32](#t32) is how a school finds them. A scheduled job that writes the status is a different
conversation — until then, a read must treat an `ISSUED` offer past its date as expired.

---

# Appendix — what each API takes and returns

Only the fields an endpoint accepts or answers with. Everything else on the model is either
inherited, set by the service, or not writable over HTTP.

<a id="e1"></a>
**[1](#t1) · `POST /admission-cycles`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `academicYear` | String | **yes** | `AcademicYear.name`. Must exist in this school. **Need not be running** — that is the point of this module. |
| `name` | String | **yes** | Unique with the year. `"Main intake"`, `"Scholarship round"`. |
| `inquiryOpenAt` · `applicationOpenAt` · `applicationCloseAt` · `enrollmentDeadlineAt` | Instant | no | Ordered against each other when present — see [open item 5](#5-a-cycles-dates-are-not-checked-against-the-academic-year). |
| `notes` | String | no | |

Creates in `DRAFT`. The seat table is [#4](#e4), not here — a cycle is named and dated before
anybody knows the seats.

<a id="e2"></a>
**[2](#t2) · `PATCH /admission-cycles/{id}`** — *only what you send moves*

| Field | Notes |
|---|---|
| `name` | Still has to be free in the year. **Cannot be blanked** — `""` is `BLANK_CYCLE_NAME`, not a clear. |
| the four dates | Any of them. Cleared by naming them in `clear`. |
| `notes` | `""` clears, or name it in `clear`. Both work. |
| `clear` | The fields to empty. An unknown name is a refusal, not an ignore. |
| `version` | Optional. Sent → a stale read is `409 CONCURRENT_MODIFICATION`; absent → last write wins. |

**Clearing needed its own list, and this is the first place in the project that was true.** The
convention elsewhere is `""` clears — which cannot work for an `Instant`: there is no empty instant,
and a record cannot tell an absent key from a `null` one, because both arrive as null.

**The dates are checked AS THEY WILL END UP**, merged with what is stored. A lone `applicationCloseAt`
can be fine on its own and wrong against the `applicationOpenAt` already on the document; checking
the request alone would let it through. This is the endpoint's one real difficulty.

**`academicYear`, `status` and `capacities` are not accepted.** The first would change which names
the cycle must be unique against and the year every application under it is for — a different
cycle, not a correction. The second is [#3](#t3). The third is [#4](#e4). All three are *ignored*
rather than refused, so sending one alone answers `NOTHING_TO_UPDATE`.

**A body that changes nothing is `400`**, not a silent 200: a no-op that answers 200 is
indistinguishable from a change that worked.

**Open question for [#3](#t3):** nothing stops a `COMPLETED` or `CANCELLED` cycle being corrected.
It is unreachable today — no cycle can leave `DRAFT` until #3 exists — so no rule was invented for
it. #3 should decide whether a finished round is still editable.

<a id="e3"></a>
**[3](#t3) · `POST /admission-cycles/{id}/status`** — *the one the module waited for*

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | AdmissionCycleStatus | **yes** | Must be a legal move from where the cycle is. |
| `version` | Long | no | Sent → a cycle moved since answers `409 CONCURRENT_MODIFICATION`. |

**It only goes forwards**, and both ends are terminal. Skipping is refused
(`DRAFT → COMPLETED` is not a move), and so is asking for the status it already has — a silent
`200` there would tell a caller they opened a cycle when they did not. **The refusal always lists
what is reachable** from where the cycle actually is.

**`CANCELLED` and `COMPLETED` are terminal, which means NOTHING is reachable** — not one thing.
That distinction was measured: a mutation that made `CANCELLED → DRAFT` legal survived a test that
only tried `CANCELLED → COMPLETED`.

**Opening needs a seat table** — `CYCLE_HAS_NO_SEATS`. Not an invented rule:
[#17](#e17) refuses an application whose class is not in the cycle's capacities
([`CLASS_NOT_IN_CAPACITY`](#the-refusal-codes-this-module-introduces)), so a cycle opened with an
empty table is a round nobody can apply to. **Checked only on the way in**: a cycle already open
whose table was emptied afterwards can still be closed or cancelled, because trapping it would be
worse.

**There is no reason field.** Cancelling is the move worth recording one for, and `AdmissionCycle`
has nowhere to put it — only `notes`, which describes the round rather than what happened to it.
Asking for a reason and dropping it would be worse than not asking. **A `cancellationReason` on the
model would fix it.**

**Open question, unchanged from [#2](#e2):** nothing stops a `CANCELLED` or `COMPLETED` cycle being
corrected by #2. Now reachable for the first time, and worth settling.

<a id="e4"></a>
**[4](#t4) · `PUT /admission-cycles/{id}/capacities`** — *the whole table, replaced*

| Field | Type | Required | Notes |
|---|---|---|---|
| `capacities[].classDocsId` | String | **yes** | Must be a class of the cycle's **academic year**. |
| `capacities[].totalSeats` | Integer | **yes** | ≥ 0. |
| `capacities[].reservedSeats` | Integer | no | ≥ 0, ≤ `totalSeats`. Defaults to 0. |

A `PUT` because the table is read and rewritten as a unit by whoever sets intake, and a per-row
`PATCH` would need a row identity that `IntakeCapacity` does not have.

**It REPLACES.** A shorter list removes the rows left out; `{"capacities": []}` clears the table. A
*missing* `capacities` is a refusal rather than a clear — forgetting the field and deliberately
emptying it must not be the same request.

**One bad row refuses the whole table**, so a partly-applied table is not a state that can exist.

`reservedSeats` defaults to **0**, never null. `totalSeats: 0` is a row, not an omission: it says
the school considered that class and is offering nothing.

Takes an optional `version`, and it matters more here than on [#2](#e2) — this write replaces, so
two people setting intake from stale screens means one silently loses every row the other added.

Answers the **whole cycle**, the shape [#6](#e6) returns, so the table comes back with its class
names resolved.

<a id="e6"></a>
**[6](#t6) · `GET /admission-cycles/{id}`** — *one cycle in full*

Adds two things a [#5](#t5) row cannot carry: `notes`, and the **seat table itself** rather than a
count of it. Plus `totalSeats`, summed server-side so every caller gets the same number.

**Each seat row carries its class's `className`**, resolved in **one** query for all of them — not
one per row. **This is why the Collections column names `school_classes`**, which the plan
originally did not: a table of raw `classDocsId`s is not something anybody can read, and the same
call [`timetable` #7](../academics/timetable/README.md) already made.

**A class that is gone still gets a row, with no name.** A cycle holding seats for a class the
school no longer has is a real problem; inventing a name, or dropping the row, hides it.

**It says nothing about how the seats are DOING.** Offered, accepted, enrolled, free — those come
from `admission_applications` and they are [#7](#e7). This reads one document.

An empty seat table is normal, not a refusal: `capacities: []`, `capacityCount: 0`,
`totalSeats: 0`. Every cycle is created that way and [#4](#e4) is not built.

**The id is scoped to the school in the query**, not checked after the read — another school's real
id answers `ADMISSION_CYCLE_NOT_FOUND`. A "not found" that depends on remembering to check is one
refactor from a leak.

<a id="e7"></a>
**[7](#t7) · `GET /admission-cycles/{id}/capacity`** — *seats against reality*

Returns one row per configured class: `totalSeats`, `reservedSeats`, and **computed**
`applied`, `offered`, `accepted`, `enrolled`, `waitlisted`, `free`.

**The counts are computed, never stored** — the model README says so, and the reason is the one
`AdmissionCycle` would otherwise become: a high-contention document every application write has to
touch. **One grouped aggregation for the whole table**, not one query per class; that is the same
N+1 [`people` #15](../people/department/README.md#e15) names about `filledHeadcount`.

<a id="e8"></a>
**[8](#t8) · `POST /inquiries`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `prospectiveStudentName` | String | **yes** | |
| `academicYear` | String | **yes** | Must exist. Need not be running. |
| `dateOfBirth` · `gender` | LocalDate · Gender | no | A phone enquiry often has neither. |
| `interestedClassDocsId` | String | no | Must belong to `academicYear` when given. |
| `guardians[]` | InquiryGuardian | no | At least one is strongly wanted but **not required**: a walk-in with a name and nothing else is a real lead. |
| `assignedCounselorDocsId` | String | no | Must be this school's staff. |
| `source` · `sourceDetails` · `notes` | String | no | |

`inquiryNo` is generated from `NumberSequenceType.ADMISSION_INQUIRY`. Status starts `NEW`.

<a id="e10"></a>
**[10](#t10) · `POST /inquiries/{id}/follow-ups`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `note` | String | **yes** | What happened. |
| `communicationChannel` | String | no | `"PHONE"`, `"WHATSAPP"`, `"VISIT"`. |
| `status` | InquiryStatus | no | When the call moved the lead. Runs the same transition check [#12](#t12) does. |
| `nextFollowUpAt` | Instant | no | **Also written to `Inquiry.nextFollowUpAt`**, which is what the worklist index sorts on. |

A `$push`, never a re-save — the same call [`timetable` #3](../academics/timetable/README.md#e3)
makes, and for the same reason.

<a id="e13"></a>
**[13](#t13) · `GET /inquiries`** — *the counsellor's worklist*

`?academicYear=` · `?status=` · `?assignedCounselorDocsId=` · `?overdue=true` ·
`?search=` · `?page=` · `?size=` · `?sort=`

**`?overdue=true` is the one that matters**: `nextFollowUpAt` before now, on a lead that is not
`LOST` or `CLOSED`. It is what `school_inquiry_pipeline_idx` was built for.

<a id="e15"></a>
**[15](#t15) · `GET /inquiries/search?phone=&email=`** — *is this family already known*

Matches `guardians.phoneNumber` and `guardians.emailAddress`, which have indexes of their own.
**Asked before every new lead**, and the reason [merge](#things-this-module-deliberately-will-not-have)
is not an endpoint.

<a id="e17"></a>
**[17](#t17) · `POST /applications`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `admissionCycleDocsId` | String | **yes** | Must be `OPEN` — `CYCLE_NOT_OPEN`. |
| `inquiryDocsId` | String | no | **Nullable on purpose**: the family that walks in with a form. When given, one per cycle — [open item 2](#2-one-inquiry-one-application-per-cycle). |
| `appliedClassDocsId` | String | **yes** | Must belong to the **cycle's** year, and be in its seat table. |
| `applicantName` · `dateOfBirth` · `gender` | | **yes** | Required here where the inquiry made two of them optional — an application is a formal document. |
| `guardians[]` | InquiryGuardian | **yes**, ≥1 | Copied from the inquiry when one is named, and **overridable**: the parent filling the form is the one who signs. |
| `formAnswers` | Map | no | Unvalidated — [open item 4](#4-formanswers-is-an-unvalidated-map). |

Creates in `DRAFT`. `applicationNo` from `NumberSequenceType.ADMISSION_APPLICATION`. When an inquiry
is named, its status moves to `APPLICATION_STARTED`.

<a id="e19"></a>
**[19](#t19) · `POST /applications/{id}/submit`** — *the snapshot freezes here*

No body. `DRAFT → SUBMITTED`, stamps `submittedAt`, moves any named inquiry to
`APPLICATION_SUBMITTED`.

**After this, [#18](#t18) refuses.** Guardian and applicant fields are a snapshot of what the family
declared, and a school that could edit them afterwards could not answer "what did they actually
tell us".

<a id="e20"></a>
**[20](#t20) · `POST /applications/{id}/decision`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `decision` | enum | **yes** | `APPROVE` · `REJECT` · `WAITLIST` · `REQUEST_MORE_INFORMATION` — the same four as `AdmissionRecommendation`. |
| `note` | String | no | Required for `REJECT`, by the same reading that makes `lostReason` required. |

**Does not require a completed review.** Small schools decide in a conversation, and an endpoint
that insisted on a review row would make them invent one.

<a id="e29"></a>
**[29](#t29) · `POST /applications/{id}/offers`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `offeredClassDocsId` | String | **yes** | Usually the applied class; **not always** — a school offers a different grade after assessment. |
| `expiresAt` | Instant | no | Defaults to the cycle's `enrollmentDeadlineAt` when it has one. |
| `depositInvoiceDocsId` | String | no | |

The application must be `APPROVED` or `WAITLISTED`. `revisionNo` is `max + 1` for that application,
and **issuing a new one supersedes the last** — `SUPERSEDED`, which is the enum value the model
README's diagram omits. `offerNo` from `NumberSequenceType.ADMISSION_OFFER`.

<a id="e30"></a>
**[30](#t30) · `POST /offers/{id}/respond`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `response` | AdmissionResponse | **yes** | `ACCEPTED` · `DECLINED`. |
| `acceptanceSignatureDocsId` | String | no | |

Offer must be `ISSUED` and not past `expiresAt`. On `ACCEPTED` the application moves to
`OFFER_ACCEPTED`; on `DECLINED` it stays where it is — **a declined offer is not a rejected
applicant**, and the school may issue another revision.

<a id="e31"></a>
**[31](#t31) · `POST /offers/{id}/withdraw`** — `withdrawalReason` required.

<a id="e33"></a>
**[33](#t33) · `POST /applications/{id}/enroll`** — *the whole point, and the one that is blocked*

No body. In one transaction:

1. create the `Student` — `admissionNo` from `NumberSequenceType.STUDENT_ADMISSION`, guardians
   copied, **no academic year** (the academic record is assigned separately);
2. set `AdmissionApplication.resultingStudentDocsId` and `Student.admissionApplicationDocsId`;
3. move the application to `ENROLLED`;
4. move the accepted offer's response and status;
5. close the originating inquiry when there is one.

Refuses without an `ACCEPTED` offer, on an application that already has a student, and when the
class's seats are full.

**Step 1 is a call to `StudentService`, not a write this module makes.** It is
[`student` #1](../student/README.md#e1) with `admissionApplicationDocsId` set — so the guardian
matching, the `admissionNo` sequence and the student's own refusals belong to that module and are
not reimplemented here. That is what keeps this endpoint small.

**No academic record is created.** The child is placed by
[`student` #14](../student/README.md#e14), separately and often later — a school knows it has
admitted a child before it knows which section they are in. Scheduled as
[cross-module phase 6](../README.md#the-phases); see
[open item 1](#1-enrollment-is-blocked-on-a-module-that-does-not-exist) and
[open item 3](#3-the-applicationstudent-link).

<a id="e34"></a>
**[34](#t34) · `GET /admission-funnel?admissionCycleDocsId=`**

Leads, applications, offers and enrolments as counts per stage, for one cycle. Three aggregations,
one per collection — **not** a join, because the three collections do not share a key that makes one
possible.

---

*Endpoints without an appendix entry — [#5](#t5), [#9](#t9),
[#11](#t11), [#12](#t12), [#14](#t14), [#16](#t16), [#18](#t18), [#21](#t21), [#22](#t22),
[#23](#t23), [#24](#t24), [#25](#t25), [#26](#t26), [#27](#t27), [#28](#t28), [#32](#t32) — take
what their tables and the status graphs above already say. An appendix row is written when the
endpoint is, so that it describes what was built rather than what was imagined.*
