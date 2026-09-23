# controllers/crm — API plan

**Twelve of thirty-four are built — the whole cycle half except [#7](#e7), the four application
endpoints that take a form, send it and read it back, and the two that assess it.**
[#1](#e1) opens a year for admissions, [#2](#e2) corrects one, [#3](#e3) moves it through its
lifecycle, [#4](#e4) sets its seats, [#5](#e5) lists the rounds and [#6](#e6) opens one in full.

**A family can apply, send the form, and have it read back.** [#17](#e17) takes one against an
open cycle, [#19](#e19) submits it and freezes the snapshot, [#24](#e24) lists the pipeline and
[#25](#e25) opens one in full — guardians, answers, evidence, and the reviews and offers from
their own collections.

**Phase 2 is complete and phase 3 is most of the way.** [#26](#e26) puts an application on
somebody's desk and [#20](#e20) records what the school decided — and a form can now run from
`DRAFT` all the way to `APPROVED`. What is left of the phase is [#27](#t27), which records what a
reviewer found, and [#28](#t28), their own queue.

**The module now runs out of road at `APPROVED`**, where it always said it would: the next thing
that happens to an approved applicant is an offer, and that is phase 4.

The rest is the full set of endpoints the admissions feature needs, written before
any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md), [`controllers/plans`](../plans/README.md),
[`controllers/people`](../people/README.md) and
[`controllers/academics/timetable`](../academics/timetable/README.md) were done.

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

| Document | Collection | What it holds |
|---|---|---|
| [`AdmissionCycle`](../../models/crm/AdmissionCycle.java) | `admission_cycles` | one round of admissions for one year — its calendar, its status and its seat table |
| [`Inquiry`](../../models/crm/Inquiry.java) | `inquiries` | a lead: one prospective **child**, their guardians and the follow-ups logged against them |
| [`AdmissionApplication`](../../models/crm/AdmissionApplication.java) | `admission_applications` | the form a family fills in against a cycle, and the snapshot it freezes into |
| [`AdmissionReview`](../../models/crm/AdmissionReview.java) | `admission_reviews` | one person's assessment of one application, in one round |
| [`AdmissionOffer`](../../models/crm/AdmissionOffer.java) | `admission_offers` | one revision of a seat offered to a family, and their answer |

Four collections owned by other modules are read or written from here, and are linked in the
tables below where that happens: [`school_classes`](../../models/academics/structure/SchoolClass.java),
whose ids a cycle's seat table and an application both store;
[`number_sequences`](../../models/institution/NumberSequence.java), which supplies `inquiryNo`,
`applicationNo` and `offerNo`; [`students`](../../models/student/Student.java), which
[#33](#e33) creates; and [`document_records`](../../models/documents/DocumentRecord.java), whose
ids an application's evidence list holds.

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

**The numbers are #1 to #34 and they are not renumbered.** They are quoted from the Postman
collection, the service banners, the API tester's catalogue and half the javadoc in this package,
so closing a gap would break every one of those references.

**What the marker in the `#` column means**, the same words
[`controllers/core`](../core/README.md) and [`controllers/plans`](../plans/README.md) use:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. **12 of them** — #1 to #6, #17, #19, #20, #24, #25, #26. |
| *(unmarked)* | Planned. It does not exist, and a request to it returns a 404. |

There is no **deferred** or **not being built** in this module yet: nothing here has been decided
against, and the order everything is waiting on is [below](#build-order). Every marker in the
table is repeated on that endpoint's own entry in the appendix, so the two cannot drift.

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
| <a id="t5"></a>5 — **built** | [`GET /admission-cycles`](#e5) | Every cycle, filtered by year and status. | `admission_cycles` |
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
| <a id="t19"></a>19 — **built** | [`POST /applications/{id}/submit`](#e19) | `DRAFT → SUBMITTED`. **Freezes the snapshot.** | `admission_applications`, `inquiries` |
| <a id="t20"></a>20 — **built** | [`POST /applications/{id}/decision`](#e20) | The review outcome: approve, reject, waitlist, ask for more. | `admission_applications` |
| <a id="t21"></a>21 | [`POST /applications/{id}/withdraw`](#t21) | The family pulls out. | `admission_applications` |
| <a id="t22"></a>22 | [`POST /applications/{id}/assign`](#t22) | Give it to an admission officer. | `admission_applications`, `staff` |
| <a id="t23"></a>23 | [`PUT /applications/{id}/documents`](#t23) | Attach or replace the evidence list. | `admission_applications`, `document_records` |

## 6. The application — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t24"></a>24 — **built** | [`GET /applications`](#e24) | **The pipeline.** Filtered by cycle, class, status, officer. | `admission_applications` |
| <a id="t25"></a>25 — **built** | [`GET /applications/{id}`](#e25) | One application in full, with its reviews and offers. | `admission_applications`, `admission_reviews`, `admission_offers` |

## 7. The review · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t26"></a>26 — **built** | [`POST /applications/{id}/reviews`](#e26) | Assign a reviewer for a round. | [`admission_reviews`](../../models/crm/AdmissionReview.java) |
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

<a id="build-order"></a>
# Build order

**The authoritative order is [`controllers/README.md`](../README.md#the-order)**, because this
module and [`student`](../student/README.md) are built interleaved — [#33](#e33) is the join, and it
sits between two `student` phases.

This module's own endpoints, ordered by **what they unblock** rather than by number:

| This module's phase | Cross-module phase | What it gives you | Endpoints |
|---|---|---|---|
| ~~**1**~~ | [1](../README.md#the-phases) | A cycle exists and can be read back | ~~1~~, ~~5~~, ~~6~~, ~~3~~ |
| ~~**2**~~ | [2](../README.md#the-phases) | Applications can be taken and seen | ~~17~~, ~~19~~, ~~24~~, ~~25~~ |
| **3** | [3](../README.md#the-phases) | The pipeline can be worked | ~~20~~, ~~26~~, 27, 28, 22 |
| **4** | [4](../README.md#the-phases) | Offers can be made and answered — **and here it stops** | 29, 30, 32, 31 |
| **5** | [6](../README.md#the-phases) | A student comes out of the other end | 33 |
| **6** | [9](../README.md#the-phases) | The lead half, which nothing else needs | 8, 13, 14, 10, 12, 11, 9, 15, 16 |
| **7** | [10](../README.md#the-phases) | The rest | ~~2~~, ~~4~~, 7, 18, 21, 23, 34 |

**A ~~struck~~ number is built** — the same twelve the `#` column marks, said here so the order
shows where it has got to. **Phases 1 and 2 are done** and **phase 3 is most of the way**: a form
runs from `DRAFT` to `APPROVED`, and what is missing is the reviewer's own half — [#27](#t27) and
[#28](#t28).

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

**[#2](#e2) and [#4](#e4) were pulled forward out of phase 7, and that was right.** The order above
puts the corrections last because nothing depends on them. In practice a cycle you cannot correct
and cannot give seats to is a cycle you cannot test [#17](#e17) against — `CLASS_NOT_IN_CAPACITY`
needs a seat table to be missing a class from, and the date refusals need dates you can move. The
rule the order is really expressing is *what unblocks the next endpoint*, and for those two the
answer turned out to be "the one after it".
---

# The rules that outrank everything else

### Gate 4 can never run in this module, and the cycle's status is what replaces it

Every write in `academics` asks `requireYearMarkedAsRunning`. **Admissions is the one module whose
entire purpose is a year that is not running yet** — a school opens its 2027-2028 cycle in the
middle of 2026-2027, and often works both at once. An endpoint here that asked "is this the running
year" would refuse the work the module exists to do.

What takes its place is the cycle's own status, and it is a check on the *cycle* rather than on the
year: an application can only go into an `OPEN` one — `CYCLE_NOT_OPEN`. That is why the seat table
and the status live on the cycle rather than on the year, and why the year is a field rather than a
path segment. See [gates](#which-gates-every-endpoint-runs) and [one surface](#one-surface-and-why).

### The status is the switch and the dates are the calendar, and both are asked

A cycle's `status` says whether anybody pressed the button. Its `applicationOpenAt` and
`applicationCloseAt` say what the school **told families**. They catch different mistakes — a round
nobody opened, and a round nobody remembered to close — so [#17](#e17) and [#19](#e19) ask both.
Neither can stand in for the other, and an endpoint that checked only the status would keep taking
forms against a round whose published deadline passed a month ago.

**And the moment that counts is the moment of the write**, not the moment the draft was started. A
form begun an hour before the deadline and submitted an hour after it is a late application.

### Once an application is submitted, its snapshot can never be edited

Guardian and applicant details are a record of **what the family declared**, frozen by
[#19](#e19). [#18](#t18) refuses from that point on — `APPLICATION_NOT_EDITABLE` — and that is not
a convenience about stale data: a school that could rewrite those fields afterwards could not
answer "what did they actually tell us", which is the only question an admissions record exists to
answer.

It is also why the guardians are **copied** onto the application rather than linked to the inquiry.
Editing the lead afterwards must not rewrite a form the school has already acted on.

### No endpoint sets a status by being told to

There is no `PATCH` that writes `status` on anything here, and there must never be one. Each move
has its own preconditions, its own side effects and its own refusals: `OFFERED` is [#29](#e29)'s
consequence, `OFFER_ACCEPTED` is [#30](#e30)'s, `ENROLLED` is [#33](#e33)'s. A single "set the
status" endpoint would be ten endpoints wearing one name, and the refusals would have nowhere to
live. Writes that are events get a verb — the same call [`people` #18b](../people/staff/README.md)
made for employment status.

### The status graphs in this file are the specification

The [`models/crm`](../../models/crm) README draws a subset. The enums carry values it does not
mention — `COUNSELLING`, `VISIT_SCHEDULED`, `VISITED`, `LOST` and `CLOSED` on `InquiryStatus`,
`ADDITIONAL_INFORMATION_REQUIRED` and `WITHDRAWN` on `AdmissionApplicationStatus`, `SUPERSEDED` on
`AdmissionOfferStatus`. **The endpoints are the only thing that can define a legal move**, so
[the graphs below](#the-status-graphs) are the rule and the diagram over there is a picture.

### A sort allowlist is a security control, not a convenience

Ordering is a read. Sort by a field and walk the pages and you learn its values even where nothing
displays them, so every paged read here names the fields it will order by and refuses the rest with
`INVALID_SORT_FIELD`. **The sharpest case is [#24](#e24)**, where `dateOfBirth` is deliberately
absent: an application carries a child's birthday, and paging through it sorted would hand over
every applicant's age without a screen ever showing one. `guardians` and `formAnswers` are off for
the same reason.

**And every fallback order must be total.** Each list ends its sort with a field that is unique
within the school — `name` within a year for cycles, `applicationNo` for applications — because
without that, paging shows one row twice and never shows another. Both were proven by mutation, not
assumed.

### There is no `DELETE` on anything, and the reasons are in the record

An inquiry that came to nothing is `LOST`; an application is `WITHDRAWN`; an offer is `WITHDRAWN` or
`EXPIRED`. Admissions is the record of what a school **decided** about a child, and the decision not
to admit is exactly the part worth keeping. Where a refusal needs explaining the reason is required
rather than optional — `lostReason`, `withdrawalReason`.

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

# Debts and open questions

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
        AdmissionApplicationServiceUtils.java   4 methods
        AdmissionCycleServiceUtils.java         1 method
    helper/   the rules MongoDB cannot express
        CrmHelper.java                          2 methods

repositories/crm/{inquiry,admissioncycle,admissionapplication,admissionreview,admissionoffer}/
dto/crm/{inquiry,admissioncycle,admissionapplication,admissionreview,admissionoffer}/{request,response}/
```

**Five controllers, not one.** `people` put departments and positions in one controller and it grew
to fifteen endpoints across two documents — which is the file this project just renamed because
nobody could say what it was about. Five collections get five.

**One helper per service, and a helper never calls another helper.** The rules that will live there:
the status transition tables, the "an application's class must belong to its cycle's year" check,
and the snapshot rule.

**What goes in `utils/` is decided by counting, not by taste — 2026-09-23.** A method moves there
when **two or more** of its service's own methods call it; anything with one caller stays inline
under its `//! step N`, where it reads in the order it happens. That is why
`AdmissionCycleServiceUtils` holds one method and not three: `datesRunForwards` and that service's
`nextStepFor` each have a single caller, and moving them would buy a longer import list and a jump
to nowhere.

**`AdmissionReviewService` has no `utils` file**, and will not until it earns one. It has a single
public method, so nothing in it can repeat. [#27](#t27) and [#28](#t28) will give it three, and the
reads they share are what the file would be for.

---

# The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `ADMISSION_CYCLE_NOT_FOUND` | 404 | No cycle with that id in this school. |
| `CYCLE_NAME_TAKEN` | 409 | That year already has a cycle of that name. |
| `APPLICATIONS_NOT_OPEN_YET` | 409 | [#17](#e17)/[#19](#e19) — the cycle is `OPEN` but its published `applicationOpenAt` has not arrived. |
| `APPLICATIONS_CLOSED` | 409 | [#17](#e17)/[#19](#e19) — the cycle is still `OPEN` but its published `applicationCloseAt` has passed. Nobody closed it. |
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
| `INVALID_APPLICATION_TRANSITION` | 409 | [#19](#e19) on anything that is not a `DRAFT` (re-submitting included), or [#20](#e20)/[#21](#t21) asking for a move the status graph does not have. **[#20](#e20)'s message lists what IS reachable**, and when nothing is, says why. |
| `DECISION_NOTE_REQUIRED` | 400 | [#20](#e20) moved a form to `REJECTED` or `ADDITIONAL_INFORMATION_REQUIRED` with no reason. A blank one counts as none. |
| `DUPLICATE_CAPACITY_CLASS` | 409 | [#4](#e4) listed one class twice. |
| `RESERVED_EXCEEDS_TOTAL` | 400 | [#4](#e4) reserved more seats than the class offers. |
| `CLASS_NOT_IN_CYCLE_YEAR` | 409 | The applied class belongs to a different academic year than the cycle. |
| `CLASS_NOT_IN_CAPACITY` | 409 | The cycle's seat table does not list that class. |
| `REVIEW_NOT_FOUND` | 404 | No review with that id in this school. |
| `APPLICATION_NOT_REVIEWABLE` | 409 | [#26](#e26) on a `DRAFT` nobody sent, or on a form already decided. |
| `REVIEWER_ALREADY_ASSIGNED` | 409 | [#26](#e26) — that reviewer already has that round of that application. A *different* person on the same round is fine. |
| `REVIEW_ALREADY_COMPLETED` | 409 | [#27](#t27) on a review that is already `COMPLETED`. |
| `OFFER_NOT_FOUND` | 404 | No offer with that id in this school. |
| `APPLICATION_NOT_APPROVED` | 409 | [#29](#e29) on an application that has not been approved. |
| `OFFER_NOT_ANSWERABLE` | 409 | [#30](#e30) on an offer that is not `ISSUED`. |
| `OFFER_EXPIRED` | 409 | [#30](#e30) after `expiresAt`. |
| `OFFER_NOT_ACCEPTED` | 409 | [#33](#e33) without an accepted offer. |
| `ALREADY_ENROLLED` | 409 | [#33](#e33) on an application that already has a `resultingStudentDocsId`. |
| `SEATS_EXHAUSTED` | 409 | [#33](#e33) when the class's configured seats are full. |
| `STAFF_NOT_FOUND` | 404 | Shared. An assigned counsellor, officer or reviewer who is not this school's staff — **another school's real staff id included**, which is the case worth testing. Raised by [#26](#e26) today. |
| `CONCURRENT_MODIFICATION` | 409 | Shared. Another write changed the document first. |

**The paging refusals are not in that table because this module does not introduce them.**
[#5](#e5) and [#24](#e24) raise `INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INVALID_SORT_FIELD` and
`INVALID_SORT_DIRECTION` from `PageResponse.pageableOf`, the same four every paged read in the
codebase raises. **Only the allowlist is this module's**, and it is per endpoint — `#5` allows nine
fields of a cycle, `#24` six of an application, and each refusal lists its own.

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

<a id="cycle-status-graph"></a>
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

<a id="application-status-graph"></a>
## `AdmissionApplicationStatus` — [#19](#e19), [#20](#e20), [#21](#t21), [#29](#e29), [#30](#e30), [#33](#e33)

```text
DRAFT ──> SUBMITTED ──> UNDER_REVIEW ──┬──> APPROVED ──> OFFERED ──> OFFER_ACCEPTED ──> ENROLLED
              │             │          ├──> REJECTED
              │             │          └──> WAITLISTED ──> APPROVED
              │             v
              │  ADDITIONAL_INFORMATION_REQUIRED ──> UNDER_REVIEW   (#20 asks for it)
              │             │
              └─────────────┴──> the same four, moved to by #20

anything before ENROLLED ──> WITHDRAWN   (requires withdrawalReason)
```

**`SUBMITTED` reaches the outcomes directly, without passing through `UNDER_REVIEW`** — added when
[#20](#e20) was built. A school that decides in a conversation never assigns a reviewer, and the
endpoint would otherwise force it to invent one.

Which endpoint owns which move is the point: `OFFERED` is [#29](#e29)'s side effect,
`OFFER_ACCEPTED` is [#30](#e30)'s, and `ENROLLED` is [#33](#e33)'s. **No endpoint sets these by
being told to** — they are consequences.

<a id="offer-status-graph"></a>
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

# Appendix — what every API touches, field by field

The same 34 endpoints, with the fields each one reads and each one writes. Written so that whoever
changes an endpoint does not have to work this out again from the models, and so a reviewer can see
at a glance whether a change reaches a field it should not.

Read **updates** as "changes an existing document", **insert** as "writes a new one", and **reads**
as "looks at it but does not change it".

Three things are left out of every entry because they are true of all of them:

- **The audit fields** — `createdAt`, `updatedAt`, `createdByDocsId`, `updatedByDocsId` and
  `version` — are filled in by Spring Data on every write. No endpoint sets them by hand.
- **`schoolId`** is on all five collections and **every query must carry it**. Not one lookup in
  this module is by id alone: an id from another school is a real id, and finding it first and
  checking the school afterwards would already have read a child's date of birth and their
  guardians' phone numbers.
- **Every endpoint resolves the school first** from the `idtoken` cookie — `require()` on a read,
  `requireUsable()` on a write. It is listed only where the endpoint also cares about a field on the
  school.

**An entry marked *built* describes running code**; an unmarked one describes the plan and may
still be wrong when it is built. Twelve of the thirty-four are built, and an entry gets its field
tables and its request and response the day its endpoint does — so an unmarked entry is deliberately
thinner than a built one rather than neglected.

## What each field can hold

The entries below name the fields; this names the **values**. Stated once here rather than repeated
across thirty-four entries, so there is one place to correct when a rule changes.

**Where a set is closed, it is an enum and the list is exhaustive** — anything else is a `400` from
Spring's type-mismatch handler naming the field. Where it is open (`name`, `notes`, `applicantName`)
the column says so, because an open set is a thing a reviewer should notice.

**A cap written as `max N` is `@Size` on the REQUEST DTO, not on the model.** There is no
`ValidatingMongoEventListener` registered in this project, so `@NotNull` and `@NotBlank` on a model
are documentation rather than a guard — the enforcement is `@Valid` on the request. Anything written
directly to Mongo bypasses all of it, which is how this module's own test fixtures reach statuses no
endpoint can set.

### `admission_cycles` — [AdmissionCycle](../../models/crm/AdmissionCycle.java)

| Field | Type | What can be in it |
|---|---|---|
| `academicYear` | String, required | **Open** — the year's *name*, `max 40`, conventionally `2026-2027`. It is the string every other collection stores, not an id. **It does not have to be the year the school is running**, which is the whole point of the module. Unique with `name` per school — `school_academic_year_cycle_name_uniq`. |
| `name` | String, required | **Open** — `max 120`, what the school calls this round: `"Main intake"`, `"Scholarship round"`. Must differ from the other cycles of the same year → `409 CYCLE_NAME_TAKEN`. [#2](#e2) refuses an empty string → `400 BLANK_CYCLE_NAME`: a round cannot lose its name. |
| `inquiryOpenAt` `applicationOpenAt` `applicationCloseAt` `enrollmentDeadlineAt` | Instant, **all four required** | **ISO-8601, and they are UTC.** An Indian school's end of day is `18:29:59Z`, not `23:59:59Z` — five and a half hours earlier than it looks. They must run forwards in that order → `400 CYCLE_DATES_OUT_OF_ORDER`. **Required since 2026-09-22**; they were optional before, and rows created then can still have absent ones. [#2](#e2) can move a date but **can no longer clear one**, and [#3](#e3) fills an absent one as it moves. |
| `status` | [AdmissionCycleStatus](../../models/crm/enums/AdmissionCycleStatus.java), required | **`DRAFT`** at create — [#1](#e1) does not accept a status. Moves are [#3](#e3) only, along [the graph](#cycle-status-graph): `DRAFT → SCHEDULED → OPEN → CLOSED → COMPLETED`, and anything but the last two → `CANCELLED`. `COMPLETED` and `CANCELLED` are **terminal**. Off-graph is `409 INVALID_CYCLE_TRANSITION`; opening with an empty seat table is `409 CYCLE_HAS_NO_SEATS`. |
| `capacities` | List, required | **`[]`** at create — [#1](#e1) never accepts seats, because a round is named and dated before anybody has worked out how many places each class gets. Replaced **whole** by [#4](#e4), `max 200` rows. An empty table is a normal state for a `DRAFT`, not a missing one. Rows below. |
| `notes` | String, optional | **Open** — `max 2000`. **The only clearable field in the module**: `{"clear": ["notes"]}` on [#2](#e2). Anything else in `clear` is `400 UNKNOWN_CLEAR_FIELD`, and naming a field in `clear` while also sending it a value is `400 CLEAR_CONFLICTS_WITH_VALUE`. |

### `admission_cycles.capacities[]` — [IntakeCapacity](../../models/crm/embedded/IntakeCapacity.java)

| Field | Type | What can be in it |
|---|---|---|
| `classDocsId` | String, required | A [`SchoolClass`](../../models/academics/structure/SchoolClass.java) id, `max 60`, **of the cycle's own year**. One class may appear once → `409 DUPLICATE_CAPACITY_CLASS`. A class that is later deleted **keeps its row, with no name** on [#6](#e6): a cycle holding seats for a class the school no longer has is a real problem, and dropping the row would hide it. |
| `totalSeats` | Integer, required | **`@Min(0)`.** Zero is legal and means the class is listed with nothing to give. |
| `reservedSeats` | Integer, required | **`@Min(0)`, defaults to `0`.** Must not exceed `totalSeats` → `400 RESERVED_EXCEEDS_TOTAL`. **The `@Builder.Default` makes a null here unreachable through the API**, so the service's null-guard cannot be exercised — pinned rather than removed, because a document written directly can still carry one. |

### `admission_applications` — [AdmissionApplication](../../models/crm/AdmissionApplication.java)

| Field | Type | What can be in it |
|---|---|---|
| `applicationNo` | String, required, unique per school | **Generated, never supplied.** `NumberSequenceType.ADMISSION_APPLICATION` with the template `APP/{YYYY}/{MM}/`, so `APP/2026/09/000123`. `school_application_no_uniq` enforces it, and it is the **tiebreaker on every sort** [#24](#e24) can produce. **The stored counter's template wins over the one the code passes** — changing the code alone does nothing, which is why two rows carry the older `APP/2026/NNNNNN` shape. |
| `admissionCycleDocsId` | String, required | `max 60`. Must be a cycle of this school, and **`OPEN` and inside its window** at the moment of [#17](#e17) and again at [#19](#e19). |
| `inquiryDocsId` | String, optional | `max 60`. **Nullable on purpose** — the family that walks in with a completed form never enquired, and that is why the whole pipeline is testable without a lead in the database. One inquiry may have **one application per cycle** → `409 APPLICATION_ALREADY_EXISTS` (`school_cycle_inquiry_uniq`); see [open item 2](#2-one-inquiry-one-application-per-cycle). |
| `appliedClassDocsId` | String, required | `max 60`. Must be of the **cycle's** year → `409 CLASS_NOT_IN_CYCLE_YEAR`, and **in that cycle's seat table** → `409 CLASS_NOT_IN_CAPACITY`. Checked by [#17](#e17) only: [#19](#e19) deliberately does not re-check it. |
| `applicantName` | String, required | **Open** — `max 160`. Searched by [#24](#e24) alongside `applicationNo`. |
| `dateOfBirth` | LocalDate, required | **ISO `YYYY-MM-DD`, and `@Past`.** Required here where the inquiry left it optional — an application is a formal document. **Deliberately absent from [#24](#e24)'s sort allowlist**; see [the rules](#a-sort-allowlist-is-a-security-control-not-a-convenience). |
| `gender` | [Gender](../../models/common/enums/Gender.java), required | Closed set. Required here, optional on an inquiry, for the same reason as the birthday. |
| `guardians` | List, required, **at least one** | `@NotEmpty`, `max 10`. **Copied from the inquiry when one is named, and overridable** — the parent filling the form is the one who signs. **Frozen by [#19](#e19).** Rows below. |
| `status` | [AdmissionApplicationStatus](../../models/crm/enums/AdmissionApplicationStatus.java), required | **`DRAFT`** at create. Built moves: `DRAFT → SUBMITTED` ([#19](#e19)), `SUBMITTED → UNDER_REVIEW` ([#26](#e26)), and everything [#20](#e20) decides — which reaches `APPROVED`, `REJECTED`, `WAITLISTED`, `ADDITIONAL_INFORMATION_REQUIRED` and back to `UNDER_REVIEW`. What is left on [the graph](#application-status-graph) is `OFFERED`, `OFFER_ACCEPTED` and `ENROLLED`, which are [#29](#e29)'s, [#30](#e30)'s and [#33](#e33)'s side effects. Off-graph is `409 INVALID_APPLICATION_TRANSITION`, **including re-submitting** — which would overwrite the moment the family sent it. |
| `formAnswers` | Map, optional | **Completely open, and nothing validates it.** There is no form-definition model — the three fields that named one were deleted 2026-09-21 — so what comes back is what was sent. The only thing that can be bounded is how many there are: **200** → `400 TOO_MANY_FORM_ANSWERS`. Left off a response entirely when empty rather than sent as `{}`. See [open item 4](#4-formanswers-is-an-unvalidated-map). |
| `evidenceDocumentDocsIds` | List, required | **`[]`** always today — [#23](#t23) replaces the list and is not built. [`DocumentRecord`](../../models/documents/DocumentRecord.java) ids; this module stores ids and `documents` owns the files. Returned as an empty **list**, not omitted: a list that is there and empty is a different thing from a field nobody set. |
| `assignedAdmissionOfficerDocsId` | String, optional | A `staff` id. **Null on every row today** — [#22](#t22) assigns one and is not built — which is why [#24](#e24)'s officer filter returns nothing for any id. |
| `submittedAt` | Instant, optional | **Set once, by [#19](#e19).** Absent while `DRAFT`. **Not the default sort on [#24](#e24)** although it looks like the obvious choice: a `DRAFT` has none, so every unsubmitted form would sort together in an order nothing decides. |
| `decidedAt` | Instant, optional | Set by [#20](#e20) every time the school decides. **Added with that endpoint on 2026-09-22**, because there was nowhere to put the answer: the model carried `withdrawnAt`/`withdrawalReason` for [#21](#t21) and nothing for the decision itself. **Not the same as `updatedAt`** — a later edit moves that; this stays on the moment the school made up its mind. |
| `decisionNote` | String, optional | **Open** — `max 2000`. **Required** when [#20](#e20) moves a form to `REJECTED` or `ADDITIONAL_INFORMATION_REQUIRED` → `400 DECISION_NOTE_REQUIRED`; a blank counts as none. **Kept, not logged and dropped** — a refusal with no reason is the part of an admissions record worth the most. Read back on [#25](#e25) only; a [#24](#e24) row does not carry it. A decision that sends no note leaves the previous one alone. |
| `withdrawnAt` `withdrawalReason` | Instant / String, optional | [#21](#t21)'s, and it is not built. The reason is **required** when it is — see [the rules](#there-is-no-delete-on-anything-and-the-reasons-are-in-the-record). |
| `resultingStudentDocsId` | String, optional | Set by [#33](#e33), which is not built. Partial-unique both ways — `school_application_student_uniq` here and `school_admission_application_uniq` on [`Student`](../../models/student/Student.java) — so **two indexes can refuse the same write**; see [open item 3](#3-the-applicationstudent-link). |

### `admission_applications.guardians[]` — [InquiryGuardian](../../models/crm/embedded/InquiryGuardian.java)

The same embedded type the inquiry uses, which is why it is named for the inquiry.

| Field | Type | What can be in it |
|---|---|---|
| `fullName` | String, required | **Open** — `max 160`. |
| `relation` | [GuardianRelation](../../models/common/enums/GuardianRelation.java), required | Closed set — `FATHER`, `MOTHER`, `GUARDIAN` and the rest. |
| `phoneNumber` `emailAddress` `address` `occupation` | String, optional | **Open** — `max 40`, `160`, `400`, `120`. Absent from a response rather than returned as `""`. |
| `primaryContact` | Boolean, optional | Defaults to `false`. **Nothing enforces that exactly one guardian is primary**, and nothing reads it yet. |

### `inquiries` — [Inquiry](../../models/crm/Inquiry.java)

**Nothing writes this collection except [#17](#e17) and [#19](#e19)**, which move an existing lead's
status. [#8](#e8) captures one and is not built, so every row today was put there directly.

| Field | Type | What can be in it |
|---|---|---|
| `inquiryNo` | String, required, unique per school | From `NumberSequenceType.ADMISSION_INQUIRY`. Generated, never supplied. |
| `prospectiveStudentName` | String, required | **Open.** **An inquiry is per prospective CHILD, not per family** — a parent enquiring about two children is two inquiries, which is what makes `school_cycle_inquiry_uniq` a sane rule. |
| `dateOfBirth` `gender` | LocalDate / Gender, optional | **Optional here and required on an application.** A parent ringing to ask about fees has not filled anything in. |
| `guardians` | List, required | The same embedded type as above. |
| `academicYear` | String, required | The year they are asking about. A property, not a scope — same as the cycle. |
| `interestedClassDocsId` | String, optional | What they asked about, not what they applied for. |
| `status` | [InquiryStatus](../../models/crm/enums/InquiryStatus.java), required | **`NEW`** at create. Nine values, of which this module currently writes two: [#17](#e17) sets `APPLICATION_STARTED` and [#19](#e19) sets `APPLICATION_SUBMITTED`. The rest belong to [#12](#t12), which is not built. `LOST` requires `lostReason` → `400 LOST_REASON_REQUIRED`. |
| `assignedCounselorDocsId` | String, optional | [#11](#t11)'s, not built. |
| `source` `sourceDetails` | String, optional | **Open, and free text on purpose** — a school's channels are its own, and an enum would be wrong within a month. |
| `nextFollowUpAt` | Instant, optional | What [#13](#e13)'s worklist sorts on. Moved by [#10](#e10) as a side effect of logging a follow-up. |
| `followUps` | List, required | **`[]`** always today — [#10](#e10) pushes to it and is not built. Rows below. |
| `notes` `lostReason` | String, optional | **Open.** `lostReason` is required when the status becomes `LOST`. |

### `inquiries.followUps[]` — [InquiryFollowUp](../../models/crm/embedded/InquiryFollowUp.java)

| Field | Type | What can be in it |
|---|---|---|
| `status` | InquiryStatus, required | What the lead moved to **as a result of this contact** — the follow-up carries the move, so the timeline explains the status rather than sitting beside it. |
| `communicationChannel` | String, required | **Open** — `"PHONE"`, `"WHATSAPP"`, `"VISIT"`. Free text for the same reason as `source`. |
| `counselorDocsId` | String, required | Who made the contact. |
| `recordedAt` | Instant, required | When. |
| `note` `nextFollowUpAt` | String / Instant, optional | What was said, and when to try again. |

### `admission_reviews` — [AdmissionReview](../../models/crm/AdmissionReview.java)

**[#26](#e26) writes it and [#25](#e25) reads it.** [#27](#t27), which records the result, is not
built — so every row is `PENDING` and none carries a score.

| Field | Type | What can be in it |
|---|---|---|
| `admissionApplicationDocsId` | String, required | Which form. |
| `reviewRound` | Integer, required | **Defaults to `1`**, and `1..20` on the request — the cap is a typo guard, not a rule about how often a school may review somebody. **Nothing checks that round 1 exists before round 2 is assigned.** **A round may hold more than one review** — an interview and a test — which is why `school_application_round_reviewer_uniq` is keyed on the *reviewer* too, and why [#25](#e25) orders by round **and then `createdAt`**. |
| `reviewerDocsId` | String, required | `max 60`. **Must be staff of this school** → `404 STAFF_NOT_FOUND`, another school's real id included. [#26](#e26) *reads* the record rather than checking it exists, because the name is wanted on the answer. |
| `reviewerRole` | String, required | **Open** — `max 60`. Schools run interviews, entrance tests and principal rounds under names of their own, so an enum would be wrong within a month. See [open item 7](#7-reviewerrole-is-a-free-string). |
| `status` | [AdmissionReviewStatus](../../models/crm/enums/AdmissionReviewStatus.java), required | **`PENDING`** at create, and **nothing can move it** until [#27](#t27) exists. `IN_PROGRESS`, `COMPLETED` and `CANCELLED` are on the enum and unreachable — `CANCELLED` is what a review assigned by mistake becomes, which is why there is no `DELETE`. |
| `dueAt` | Instant, optional | What [#28](#t28)'s queue sorts on. **A date in the past is accepted** — a school catching up on paperwork records a review that was due last week, and refusing it would make the backlog unrecordable. |
| `completedAt` | Instant, optional | [#27](#t27)'s, and it is not built. |
| `score` | BigDecimal, optional | |
| `recommendation` | [AdmissionRecommendation](../../models/crm/enums/AdmissionRecommendation.java), optional | The same four values [#20](#e20)'s decision takes. |
| `criterionScores` | Map, optional | **Open** — `{"INTERVIEW": 42.50}`. Left off a response when empty. |
| `notes` | String, optional | **Open.** |

### `admission_offers` — [AdmissionOffer](../../models/crm/AdmissionOffer.java)

**Nothing writes this collection** either — [#29](#e29) to [#31](#e31) are not built. [#25](#e25)
reads it.

| Field | Type | What can be in it |
|---|---|---|
| `offerNo` | String, required, unique per school | From `NumberSequenceType.ADMISSION_OFFER`. |
| `revisionNo` | Integer, required | Defaults to `1`, and **`max + 1` per application**. Unique with the application — `school_application_offer_revision_uniq` — so it **is** a total order, which is why [#25](#e25) needs no tiebreaker on offers and does on reviews. |
| `admissionApplicationDocsId` | String, required | |
| `offeredClassDocsId` | String, required | **Usually the applied class and not always** — a school assesses a child and offers a different grade, which is why it is stored separately rather than read off the application. |
| `status` | [AdmissionOfferStatus](../../models/crm/enums/AdmissionOfferStatus.java), required | **`DRAFT`**, then [the graph](#offer-status-graph). **`EXPIRED` has no endpoint** — it is what `expiresAt` in the past *means*, and a read must treat an `ISSUED` offer past its date as expired. **Superseded revisions are kept and returned**, because they are the record of what the school offered first. |
| `offeredAt` `expiresAt` `respondedAt` | Instant, optional | `expiresAt` defaults to the cycle's `enrollmentDeadlineAt` when it has one. |
| `response` | [AdmissionResponse](../../models/crm/enums/AdmissionResponse.java), optional | `ACCEPTED` · `DECLINED`. **A declined offer is not a rejected applicant** — the application stays where it is and another revision may be issued. |
| `offerDocumentDocsId` `acceptanceSignatureDocsId` `depositInvoiceDocsId` | String, optional | Ids into `documents` and `finance`. **This module stores them and owns none of them** — an admission deposit is a `FeeInvoice`, and nothing here generates an offer letter. |
| `issuedByDocsId` | String, optional | |
| `withdrawalReason` | String, optional | **Required** when [#31](#e31) withdraws one. |

---

## The endpoints, one by one

<a id="e1"></a>
**[#1](#t1) · `POST /admission-cycles`** — built

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `name` — the year has to be one this school has. **Not `isThisYearRunning`**, which is the whole point of the module
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `academicYear`, `name` — is that name already taken inside that year
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *insert*: `schoolId`, `academicYear`, `name`, `inquiryOpenAt`, `applicationOpenAt`, `applicationCloseAt`, `enrollmentDeadlineAt`, `notes`, `status` = `DRAFT`, `capacities` = `[]`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "academicYear": "2027-2028",      // REQUIRED, max 40
  "name": "Main intake",            // REQUIRED, max 120

  // REQUIRED, all four, and in this order
  "inquiryOpenAt":       "2026-10-01T00:00:00Z",
  "applicationOpenAt":   "2026-11-01T00:00:00Z",
  "applicationCloseAt":  "2027-01-31T18:29:59Z",
  "enrollmentDeadlineAt":"2027-03-15T18:29:59Z",

  "notes": "Two rounds this year"   // optional, max 2000
}
</pre></td>
<td><pre>
201 Created
Location: /schools/current/admission-cycles/6ab1...

{
  "admissionCycleId": "6ab11f64cff1b9275e224dc7",
  "academicYear": "2027-2028",
  "name": "Main intake",
  "status": "DRAFT",
  "inquiryOpenAt":       "2026-10-01T00:00:00Z",
  "applicationOpenAt":   "2026-11-01T00:00:00Z",
  "applicationCloseAt":  "2027-01-31T18:29:59Z",
  "enrollmentDeadlineAt":"2027-03-15T18:29:59Z",
  "capacityCount": 0,
  "notes": "Two rounds this year",
  "nextStep": "Set its seats with #4, then open it with #3."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `academicYear` | **yes** | Max 40. The year's *name*, which must already exist in this school → `404 ACADEMIC_YEAR_NOT_FOUND`. **It need not be the running one.** |
| `name` | **yes** | Max 120. Unique inside that year → `409 CYCLE_NAME_TAKEN`. A school runs a general intake and a scholarship round in one year, and the name is how staff tell them apart. |
| the four dates | **yes, all four** | **Changed 2026-09-22; they used to be optional.** They must run forwards — enquiries open, applications open, applications close, enrollment deadline → `400 CYCLE_DATES_OUT_OF_ORDER`. An Instant is UTC: `2027-01-31T18:29:59Z` is one second to midnight in India, and `23:59:59Z` would hand the school most of the next day. |
| `notes` | no | Max 2000. The only field [#2](#e2) can clear. |

**`status` is not on the request.** Every cycle starts `DRAFT` — there is no starting-state choice,
because a cycle that could be created `OPEN` would skip the seat check [#3](#e3) makes.

**Neither is the seat table.** A round is named and dated before anybody has worked out how many
places each class gets, and a create that could fail on either a duplicate name or a bad seat row
would leave the caller working out which. Seats are [#4](#e4), on their own — the same shape as a
class being created with no sections.

**The dates are not checked against the academic year's own start and end.** See
[open item 5](#5-a-cycles-dates-are-not-checked-against-the-academic-year).

<a id="e2"></a>
**[#2](#t2) · `PATCH /admission-cycles/{id}`** — built — *only what you send moves*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`**; then `academicYear` + `name` again if the name is moving
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *updates*: `name`, `inquiryOpenAt`, `applicationOpenAt`, `applicationCloseAt`, `enrollmentDeadlineAt`, `notes` — **only the ones the body carries**

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "name": "Main intake 2027",    // optional, max 120
  "applicationCloseAt":          // optional, any of the four
      "2027-02-15T18:29:59Z",
  "notes": "Extended by a fortnight",

  "clear": ["notes"],            // optional; ONLY "notes"
  "version": 3                   // optional, see below
}
</pre></td>
<td><pre>
200 OK

{
  "admissionCycleId": "6ab11f64cff1b9275e224dc7",
  "academicYear": "2027-2028",
  "name": "Main intake 2027",
  "status": "DRAFT",
  "applicationCloseAt": "2027-02-15T18:29:59Z",
  ...
  "nextStep": "Moved the name and the closing date."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | no | Max 120, still unique in the year. **Cannot be blanked** — `""` is `400 BLANK_CYCLE_NAME`, not a clear. |
| the four dates | no | Any of them, individually. **They can be moved but no longer cleared** — see below. |
| `notes` | no | Max 2000. `""` clears it, or name it in `clear`. Both work. |
| `clear` | no | **`["notes"]` and nothing else.** Any other name is `400 UNKNOWN_CLEAR_FIELD` — a refusal, not an ignore, because a silently ignored clear looks like it worked. |
| `version` | no | Sent → a stale read is `409 CONCURRENT_MODIFICATION`; absent → last write wins. |

**Clearing needed its own list, and this is the first place in the project that was true.** The
convention elsewhere is `""` clears — which cannot work for an `Instant`: there is no empty instant,
and a record cannot tell an absent key from a `null` one, because both arrive as null.

**A date can no longer be cleared — changed 2026-09-22.** `clear` used to accept all five names.
Emptying a date would leave a cycle [#1](#e1) would have refused to create, and a window with no
ends cannot be checked by [#17](#e17) or [#19](#e19). Move a date instead.

**The dates are checked AS THEY WILL END UP**, merged with what is stored. A lone `applicationCloseAt`
can be fine on its own and wrong against the `applicationOpenAt` already on the document; checking
the request alone would let it through. This is the endpoint's one real difficulty.

**`academicYear`, `status` and `capacities` are not accepted.** The first would change which names
the cycle must be unique against and the year every application under it is for — a different
cycle, not a correction. The second is [#3](#e3). The third is [#4](#e4). All three are *ignored*
rather than refused, so sending one alone answers `400 NOTHING_TO_UPDATE`.

**A body that changes nothing is `400`**, not a silent 200: a no-op that answers 200 is
indistinguishable from a change that worked.

**Nothing stops a `COMPLETED` or `CANCELLED` cycle being corrected**, and that is still true now
that [#3](#e3) is built. A finished round whose name was misspelled should probably still be
fixable, and no case has come up that says otherwise — recorded here rather than decided.

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
**[#3](#t3) · `POST /admission-cycles/{id}/status`** — built — *the one the module waited for*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`**; then `status` for the move and `capacities` for the seat check
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *updates*: `status`, and **one date when it is absent** — `inquiryOpenAt` on `SCHEDULED`, `applicationOpenAt` on `OPEN`, `applicationCloseAt` on `CLOSED`, `enrollmentDeadlineAt` on `COMPLETED`. Never an already-set one, and never anything on `CANCELLED` or `DRAFT`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "status": "OPEN",   // REQUIRED, and a legal move
  "version": 4        // optional
}
</pre></td>
<td><pre>
200 OK

{
  "admissionCycleId": "6ab11f64cff1b9275e224dc7",
  "name": "Main intake",
  "status": "OPEN",
  "applicationOpenAt": "2026-11-01T00:00:00Z",
  "capacityCount": 3,
  "nextStep": "Applications can be submitted into it now.
               #17 is the endpoint that takes one."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `status` | **yes** | One of the six. Must be a legal move **from where the cycle actually is** → `409 INVALID_CYCLE_TRANSITION`, and the refusal lists what is reachable. |
| `version` | no | Sent → a cycle moved since answers `409 CONCURRENT_MODIFICATION`. |

**It fills a date the school never published.** Moving to `OPEN` with no `applicationOpenAt` stamps
now; moving to `OPEN` with one already set leaves it alone. **Since the four dates became required
at create, that fill only ever reaches cycles made before 2026-09-22** — which is also the only
place an absent date can still be found. `CANCELLED` and `DRAFT` fill nothing: neither is a moment
in a round's calendar.

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
**[#4](#t4) · `PUT /admission-cycles/{id}/capacities`** — built — *the whole table, replaced*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`**, for its `academicYear`
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `_id`, `name` — **one query for every row**, to check each class belongs to the cycle's year and to name it on the way back
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *updates*: `capacities` — **replaced whole**, never merged

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "capacities": [          // REQUIRED, max 200 rows
    { "classDocsId": "6aa3...854a",
      "totalSeats": 30,
      "reservedSeats": 5 },    // optional, default 0
    { "classDocsId": "6aa3...854b",
      "totalSeats": 25 }
  ],
  "version": 5             // optional, and it matters here
}
</pre></td>
<td><pre>
200 OK   — the WHOLE cycle, the #6 shape

{
  "admissionCycleId": "6ab1...dc7",
  "name": "Main intake",
  "status": "DRAFT",
  "capacities": [
    { "classDocsId": "6aa3...854a",
      "className": "Grade 7",     // resolved
      "totalSeats": 30, "reservedSeats": 5 },
    { "classDocsId": "6aa3...854b",
      "className": "Grade 8",
      "totalSeats": 25, "reservedSeats": 0 }
  ],
  "capacityCount": 2,
  "totalSeats": 55            // summed server-side
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `capacities` | **yes** | Max 200 rows. **A missing `capacities` is a refusal, not a clear** — forgetting the field and deliberately emptying it must not be the same request. `[]` clears the table. |
| `capacities[].classDocsId` | **yes** | Max 60. A class of the cycle's **academic year** → `409 CLASS_NOT_IN_CYCLE_YEAR`. Twice in one list → `409 DUPLICATE_CAPACITY_CLASS`. |
| `capacities[].totalSeats` | **yes** | `@Min(0)`. **`0` is a row, not an omission**: it says the school considered that class and is offering nothing. |
| `capacities[].reservedSeats` | no | `@Min(0)`, defaults to `0` and never null. Must not exceed `totalSeats` → `400 RESERVED_EXCEEDS_TOTAL`. |
| `version` | no | **Matters more here than on [#2](#e2)**: this write replaces, so two people setting intake from stale screens means one silently loses every row the other added. |

A `PUT` because the table is read and rewritten as a unit by whoever sets intake, and a per-row
`PATCH` would need a row identity that `IntakeCapacity` does not have.

**One bad row refuses the whole table**, so a partly-applied table is not a state that can exist.

**It answers the whole cycle**, the shape [#6](#e6) returns, so the table comes back with its class
names resolved rather than as the ids that were sent.

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

<a id="e5"></a>
**[#5](#t5) · `GET /admission-cycles`** — built — *every round, filtered*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `academicYear`, `status`, `name` (searched), `applicationOpenAt` + `applicationCloseAt` (the `openOn` window). **`schoolId` is added to the query and never taken from the request**

### Request and response

<table>
<tr><th align="left">Query string</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/admission-cycles
      ?academicYear=2027-2028   // optional
      &status=OPEN              // optional
      &search=main              // optional, name only
      &openOn=2026-12-01T00:00:00Z
      &page=0&size=20           // 1..100
      &sort=name,desc           // allowlist below

NO BODY — it is a GET.
</pre></td>
<td><pre>
200 OK

{
  "content": [
    { "admissionCycleId": "6ab1...dc7",
      "academicYear": "2027-2028",
      "name": "Main intake",
      "status": "OPEN",
      "applicationOpenAt": "2026-11-01T00:00:00Z",
      "capacityCount": 3,       // a COUNT, not the table
      "createdAt": "2026-09-20T05:11:42Z" }
  ],
  "page": 0, "size": 20,
  "totalElements": 1, "totalPages": 1,
  "hasNext": false, "hasPrevious": false
}
</pre></td>
</tr>
</table>

**Every parameter**

| Parameter | Type | Notes |
|---|---|---|
| `academicYear` | String | One year. A year with nothing in it is an **empty page**, never a 404 — there is no year in the path to be wrong about, which is the whole reason it is not one. |
| `status` | enum | One stage of the cycle's life. |
| `search` | String | The name, anywhere, ignoring case. Quoted before it is compiled, so `(` is an empty result rather than a 500. |
| `openOn` | Instant | Rounds whose application window covered that moment. **A cycle missing either date never matches** — it is not open forever, its calendar was never filled in. Since 2026-09-22 the dates are required, so this only reaches cycles made before that. |

**Rows are thin**: no `notes` and a `capacityCount` rather than the seat table. The table is
[#6](#e6).

**The default order is `academicYear desc, name asc`** — newest year first, then alphabetically
within it. That pair is unique within a school (`school_academic_year_cycle_name_uniq`), so the
order is **total**, and paging cannot show one row twice while never showing another. Proven by
mutation: twenty cycles planted in one year, and dropping `name` from the order made a page-by-page
walk return seven distinct rows out of twenty.

**The sort allowlist is a security control, not a convenience.** `?sort=schoolId` and `?sort=notes`
are both `INVALID_SORT_FIELD`, and the refusal names what is allowed. Ordering is a read: sort by a
field and walk the pages and you learn its values even where nothing displays them. [#24](#e24)
makes the same call about a child's date of birth, where it matters more.

**No gates, and no year has to be running.** This is the module's plainest demonstration of it — a
suspended school lists the rounds it ran, and a cycle for a year nobody has started is the normal
case rather than the exception.

<a id="e6"></a>
**[#6](#t6) · `GET /admission-cycles/{id}`** — built — *one cycle in full*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`** — everything on it, `capacities` and `notes` included
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `_id`, `name` — **one query for every seat row**, not one per row

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/admission-cycles/{id}

NO BODY, no query parameters.

The id is the cycle's document id — what every
application stores as admissionCycleDocsId.
</pre></td>
<td><pre>
200 OK

{
  "admissionCycleId": "6ab1...dc7",
  "academicYear": "2027-2028",
  "name": "Main intake",
  "status": "OPEN",
  "inquiryOpenAt":       "2026-10-01T00:00:00Z",
  "applicationOpenAt":   "2026-11-01T00:00:00Z",
  "applicationCloseAt":  "2027-01-31T18:29:59Z",
  "enrollmentDeadlineAt":"2027-03-15T18:29:59Z",
  "capacities": [
    { "classDocsId": "6aa3...854a",
      "className": "Grade 7",   // absent if gone
      "totalSeats": 30, "reservedSeats": 5 }
  ],
  "capacityCount": 1,
  "totalSeats": 30,             // summed here
  "notes": "Two rounds this year",
  "createdAt": "...", "updatedAt": "..."
}
</pre></td>
</tr>
</table>

Adds two things a [#5](#e5) row cannot carry: `notes`, and the **seat table itself** rather than a
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
`totalSeats: 0`. Every cycle is created that way, and stays that way until [#4](#e4) sets it.

**The id is scoped to the school in the query**, not checked after the read — another school's real
id answers `ADMISSION_CYCLE_NOT_FOUND`. A "not found" that depends on remembering to check is one
refactor from a leak.

<a id="e7"></a>
**[#7](#t7) · `GET /admission-cycles/{id}/capacity`** — *seats against reality*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `capacities` — what the school configured
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `appliedClassDocsId`, `status` — counted per class, never listed

Returns one row per configured class: `totalSeats`, `reservedSeats`, and **computed**
`applied`, `offered`, `accepted`, `enrolled`, `waitlisted`, `free`.

**The counts are computed, never stored** — the model README says so, and the reason is the one
`AdmissionCycle` would otherwise become: a high-contention document every application write has to
touch. **One grouped aggregation for the whole table**, not one query per class; that is the same
N+1 [`people` #15](../people/department/README.md#e15) names about `filledHeadcount`.

<a id="e8"></a>
**[#8](#t8) · `POST /inquiries`**

- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: the `ADMISSION_INQUIRY` counter
- [`inquiries`](../../models/crm/Inquiry.java) — *insert*: `inquiryNo`, `prospectiveStudentName`, `dateOfBirth`, `gender`, `guardians`, `academicYear`, `interestedClassDocsId`, `source`, `sourceDetails`, `notes`, `status` = `NEW`, `followUps` = `[]`

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
**[#10](#t10) · `POST /inquiries/{id}/follow-ups`**

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `followUps` — **a `$push`, not a save** — and `status` and `nextFollowUpAt` as side effects of the entry

| Field | Type | Required | Notes |
|---|---|---|---|
| `note` | String | **yes** | What happened. |
| `communicationChannel` | String | no | `"PHONE"`, `"WHATSAPP"`, `"VISIT"`. |
| `status` | InquiryStatus | no | When the call moved the lead. Runs the same transition check [#12](#t12) does. |
| `nextFollowUpAt` | Instant | no | **Also written to `Inquiry.nextFollowUpAt`**, which is what the worklist index sorts on. |

A `$push`, never a re-save — the same call [`timetable` #3](../academics/timetable/README.md#e3)
makes, and for the same reason.

<a id="e13"></a>
**[#13](#t13) · `GET /inquiries`** — *the counsellor's worklist*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: `status`, `assignedCounselorDocsId`, `academicYear`, `nextFollowUpAt` (overdue), `prospectiveStudentName` + `inquiryNo` (searched)

`?academicYear=` · `?status=` · `?assignedCounselorDocsId=` · `?overdue=true` ·
`?search=` · `?page=` · `?size=` · `?sort=`

**`?overdue=true` is the one that matters**: `nextFollowUpAt` before now, on a lead that is not
`LOST` or `CLOSED`. It is what `school_inquiry_pipeline_idx` was built for.

<a id="e15"></a>
**[#15](#t15) · `GET /inquiries/search?phone=&email=`** — *is this family already known*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: `guardians[].phoneNumber`, `guardians[].emailAddress` — **the only query in the module that reaches into an embedded array to match**

Matches `guardians.phoneNumber` and `guardians.emailAddress`, which have indexes of their own.
**Asked before every new lead**, and the reason [merge](#things-this-module-deliberately-will-not-have)
is not an endpoint.

<a id="e17"></a>
**[#17](#t17) · `POST /applications`** — built

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`**; then `status`, `applicationOpenAt`, `applicationCloseAt`, `academicYear`, `capacities`
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `_id`, `name` — the class must be of the **cycle's** year
- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**, when one is named; then `guardians` to copy
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `admissionCycleDocsId` + `inquiryDocsId` — has that lead already applied to that round
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: the `ADMISSION_APPLICATION` counter's `nextValue`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *insert*: `schoolId`, `applicationNo`, `admissionCycleDocsId`, `inquiryDocsId`, `appliedClassDocsId`, `applicantName`, `dateOfBirth`, `gender`, `guardians`, `formAnswers`, `status` = `DRAFT`, `evidenceDocumentDocsIds` = `[]`
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `status` = `APPLICATION_STARTED`, **after the insert** — an inquiry saying a form was started with no form is a worse lie than one that is late

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "admissionCycleDocsId": "6ab1...dc7",  // REQUIRED
  "appliedClassDocsId":   "6aa3...854a", // REQUIRED
  "applicantName": "Aarav Sharma",       // REQUIRED
  "dateOfBirth": "2018-08-14",           // REQUIRED, past
  "gender": "MALE",                      // REQUIRED

  "guardians": [                  // REQUIRED, 1..10
    { "fullName": "Rohan Sharma", // REQUIRED
      "relation": "FATHER",       // REQUIRED
      "phoneNumber": "+91 98765 43210",
      "primaryContact": true }
  ],

  "inquiryDocsId": "6ab2...e4f",  // optional
  "formAnswers": {                // optional, max 200
    "previousSchool": "ABC School"
  }
}
</pre></td>
<td><pre>
201 Created
Location: /schools/current/applications/6ab2...e50

{
  "admissionApplicationId": "6ab22aa9cff1b9275e224e50",
  "applicationNo": "APP/2026/09/000123",  // generated
  "admissionCycleDocsId": "6ab1...dc7",
  "appliedClassDocsId": "6aa3...854a",
  "appliedClassName": "Grade 7",
  "applicantName": "Aarav Sharma",
  "dateOfBirth": "2018-08-14",
  "gender": "MALE",
  "status": "DRAFT",
  "guardians": [ ... ],
  "formAnswers": { "previousSchool": "ABC School" },
  "createdAt": "2026-09-22T06:43:45Z",
  "nextStep": "'Aarav Sharma' has a DRAFT application
               for Grade 7. Submitting it is #19."
}
</pre></td>
</tr>
</table>

**Every field on the request**

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
**[#19](#t19) · `POST /applications/{id}/submit`** — built — *the snapshot freezes here*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` — only a `DRAFT`
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: the cycle by `_id` **and `schoolId`**; then `status`, `applicationOpenAt`, `applicationCloseAt`, `academicYear`
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `name`, for the answer. **Tolerantly** — a class that is gone must not stop a family submitting
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `SUBMITTED`, `submittedAt` = now
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `status` = `APPLICATION_SUBMITTED`, when one is named **and still there**

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/applications/{id}/submit

NO BODY.

Everything it needs is already on the form. It is a
POST to its own address rather than a PATCH that
sets status, because the move has its own
preconditions and its own side effects.
</pre></td>
<td><pre>
200 OK

{
  "admissionApplicationId": "6ab2...e50",
  "applicationNo": "APP/2026/09/000123",
  "applicantName": "Aarav Sharma",
  "appliedClassName": "Grade 7",
  "status": "SUBMITTED",       // moved
  "guardians": [ ... ],        // unchanged, and now frozen
  "createdAt": "2026-09-22T06:43:45Z",
  "nextStep": "'Aarav Sharma' is SUBMITTED, and the form
               is now frozen — #18 refuses to edit it
               from here. Next is a review (#26) or a
               decision (#20); neither is built."
}
</pre></td>
</tr>
</table>

`DRAFT → SUBMITTED`, stamps `submittedAt`, moves any named inquiry to `APPLICATION_SUBMITTED`.

**After this, [#18](#t18) refuses.** Guardian and applicant fields are a snapshot of what the family
declared, and a school that could edit them afterwards could not answer "what did they actually
tell us".

**Only a `DRAFT`, and the status is checked BEFORE the cycle.** That ordering is deliberate:
somebody pressing submit twice on a round that has since closed should be told the form is already
in, not that the round has shut — the second message is true and completely unhelpful. Re-submitting
is `INVALID_APPLICATION_TRANSITION`, and `submittedAt` is not overwritten.

**It asks the cycle the same two questions [#17](#e17) does**, through the same
`CrmHelper.loadOpenCycle`: the status says whether anybody opened the round, the published dates
say what the school promised families. **The moment that counts is the submission** — a form
started an hour before the deadline and sent an hour after it is a late application, and that is
the whole reason the window is asked here too rather than only at [#17](#e17).

**It does NOT re-check the seat table, and that is a decision rather than an omission.**
[#17](#e17) refuses a class with no seats, and a school can empty that table afterwards with
[#4](#e4). Submitting is the *family's* act; refusing it because the school changed its own plan
would punish the wrong side. **Capacity is decided when a seat is offered** — [#29](#e29) — and
counted by [#7](#e7). The dates are the calendar; the seat table is not.

**A named inquiry moves on, and a DELETED one is skipped.** [#17](#e17) refuses an inquiry it
cannot find, which is right when the family is naming one — but here the link was checked when the
draft was started, and a lead somebody removed since must not be able to block a family's
submission. The form is the thing that matters; the lead is a note about how it arrived. The
application still names it, because that is what happened.

**A refusal changes nothing.** Every one of the above leaves the form exactly as it was — still a
`DRAFT`, still with no `submittedAt`.

<a id="e20"></a>
**[#20](#t20) · `POST /applications/{id}/decision`** — built — *what the school decided*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` and `version`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status`, `decidedAt`, and `decisionNote` when one is sent
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `academicYear`, for the class lookup. **Tolerantly**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `name`, for the answer. Tolerantly too — a class that is gone must not stop a school recording what it decided

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "status": "REJECTED",   // REQUIRED, and it is the
                          // STATUS, not a verb

  "note": "Interview scores below the
           cut-off for Grade 7.",
        // REQUIRED for REJECTED and for
        // ADDITIONAL_INFORMATION_REQUIRED

  "version": 2            // optional
}
</pre></td>
<td><pre>
200 OK

{
  "admissionApplicationId": "6ab2...e50",
  "applicationNo": "APP/2026/09/000123",
  "applicantName": "Aarav Sharma",
  "appliedClassName": "Grade 7",
  "status": "REJECTED",
  "createdAt": "...",
  "version": 3,        // send THIS next time
  "nextStep": "The school decided against it, and
               the reason is on the form. Nothing
               moves from here."
}

decidedAt and decisionNote read back on #25.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `status` | **yes** | An [`AdmissionApplicationStatus`](../../models/crm/enums/AdmissionApplicationStatus.java) — **the status it is moving to**, exactly as [#3](#e3) takes for a cycle. Must be a move the table below has from where the form is. |
| `note` | no, except | **Required for `REJECTED` and `ADDITIONAL_INFORMATION_REQUIRED`** → `400 DECISION_NOTE_REQUIRED`. Max 2000, and a blank counts as none. A decision that sends none leaves the previous note alone. |
| `version` | no | The version last read. Sent → a form somebody else decided answers `409 CONCURRENT_MODIFICATION`. Absent → last write wins. |

**It names the status directly, and an earlier build of this did not.** The first version took its
own five-value enum — `APPROVE`, `REJECT`, `WAITLIST`, `REQUEST_MORE_INFORMATION`, `RESUME_REVIEW`
— on the argument that a body naming a status could write `ENROLLED` onto a form nobody had offered
a seat to. **That argument was wrong.** The transition table is what refuses `ENROLLED`, not the
shape of the vocabulary, and [#3](#e3) had already settled the question for the cycle. The parallel
enum bought nothing and made a caller learn two names for every move — `APPROVE` going in and
`APPROVED` coming back. It was deleted on 2026-09-22.

**Three statuses this endpoint can never set, whatever is sent**: `OFFERED`, `OFFER_ACCEPTED` and
`ENROLLED` are [#29](#e29)'s, [#30](#e30)'s and [#33](#e33)'s consequences. `WITHDRAWN` is
[#21](#t21)'s, and `DRAFT` and `SUBMITTED` are the family's side. All six are
`409 INVALID_APPLICATION_TRANSITION` — **and that refusal is the only thing stopping them**, which
is why it is worth testing directly rather than trusting the request shape.

**It does not require a review to exist, and `SUBMITTED` is therefore in the table.** The plan said
so and the reason holds: small schools decide in a conversation, and insisting on `UNDER_REVIEW`
would mean assigning a reviewer first — which *is* inventing a review row. That edge is not on the
drawn graph and the graph is the poorer for it.

| From | Can be moved to |
|---|---|
| `SUBMITTED` · `UNDER_REVIEW` | `APPROVED` `REJECTED` `WAITLISTED` `ADDITIONAL_INFORMATION_REQUIRED` |
| `ADDITIONAL_INFORMATION_REQUIRED` | `APPROVED` `REJECTED` `WAITLISTED`, **plus back to `UNDER_REVIEW`** |
| `WAITLISTED` | `APPROVED` `REJECTED` only |
| everything else | nothing, **and the refusal says why** rather than showing an empty list |

The table is `DECISION_MOVES`, and it mirrors `CYCLE_MOVES` on [#3](#e3) — including spelling out
the statuses that can go nowhere, because an absent key and an empty set mean the same thing to the
code but only one of them says it was decided.

**`APPROVED` is deliberately not decidable again.** The next thing that happens to an approved
applicant is an offer ([#29](#e29)); changing your mind is withdrawing it ([#31](#e31)). Leaving it
out keeps one answer to "what happened to this child".

**`WAITLISTED` allows only `APPROVE` and `REJECT`.** Waitlisting something already waitlisted moves
nothing, and asking a waitlisted family for more is a case nobody has described.

**Asking for `UNDER_REVIEW` is legal from exactly one status.** The graph draws
`ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW` and [#26](#e26) deliberately does not make that
move — assigning another reviewer is not what decides the information turned up — so #20 owns the
edge. `AdmissionRecommendation` is untouched by any of this: it is what *one reviewer suggests* and
lives on their review, which is [#27](#t27)'s.

**Two fields were added to the model with this endpoint — 2026-09-22.** `decidedAt` and
`decisionNote`. There was nowhere to put the answer before: the model carried `withdrawnAt` and
`withdrawalReason` for [#21](#t21) and nothing for the decision itself, so an endpoint that took a
reason would have had to throw it away — which [#3](#e3)'s entry already calls worse than not
asking. The pair completes a shape the withdrawal fields had already set.

**The note requirement grew by one.** The plan asked for it on the rejection;
`ADDITIONAL_INFORMATION_REQUIRED` was added when this was built, because asking a family for more
without saying what tells them nothing. Proven by mutation: narrowing it back to `REJECTED` alone
is caught.

**`version` is returned now, by [#17](#e17), [#19](#e19), #20 and [#25](#e25).** It was accepted and
never returned, so the only way to learn the value was to read the document out of Mongo — a
parameter nobody can learn the value of is one that cannot be used. **The cycle endpoints
([#2](#e2), [#3](#e3), [#4](#e4)) accept one and still return none**; that gap is older than this
endpoint and is not fixed here.

<a id="e24"></a>
**[#24](#t24) · `GET /applications`** — built — *the worklist*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `admissionCycleDocsId`, `appliedClassDocsId`, `status`, `assignedAdmissionOfficerDocsId`, `inquiryDocsId` (existence only), `applicantName` + `applicationNo` (searched). **`schoolId` is added to the query and never taken from the request**

### Request and response

<table>
<tr><th align="left">Query string</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/applications
      ?admissionCycleDocsId=6ab1...dc7
      &appliedClassDocsId=6aa3...854a
      &status=SUBMITTED
      &assignedAdmissionOfficerDocsId=...
      &search=aarav          // name OR number
      &fromInquiry=true      // false = walk-ins
      &page=0&size=20
      &sort=applicantName    // allowlist below

Every filter is optional. NO BODY.
</pre></td>
<td><pre>
200 OK   — rows are THIN

{
  "content": [
    { "admissionApplicationId": "6ab2...e50",
      "applicationNo": "APP/2026/09/000123",
      "admissionCycleDocsId": "6ab1...dc7",
      "appliedClassDocsId": "6aa3...854a",
      "applicantName": "Aarav Sharma",
      "dateOfBirth": "2018-08-14",
      "gender": "MALE",
      "status": "SUBMITTED",
      "submittedAt": "2026-09-22T07:10:03Z",
      "createdAt": "2026-09-22T06:43:45Z" }
      // no guardians, no formAnswers, no evidence,
      // and no appliedClassName — all on #25
  ],
  "page": 0, "size": 20,
  "totalElements": 1, "totalPages": 1,
  "hasNext": false, "hasPrevious": false
}
</pre></td>
</tr>
</table>

**Every parameter**

| Parameter | Type | Notes |
|---|---|---|
| `admissionCycleDocsId` | String | One round's forms. An id from another school is an **empty page**, never a 404 — the school is added to the query, so the filter matches nothing rather than confirming the id exists. |
| `appliedClassDocsId` | String | One class's applicants. |
| `status` | enum | One stage. Absent returns every stage, `DRAFT` and `WITHDRAWN` included. |
| `assignedAdmissionOfficerDocsId` | String | Whose worklist. **Always empty until [#22](#t22)**, which assigns the officer and is not built. |
| `search` | String | The applicant's name **or** the application number, anywhere, ignoring case. Two fields because a school looks a child up by either: the parent gives a name on the phone, the file carries a number. |
| `fromInquiry` | Boolean | `true` only the forms that name a lead, `false` only the walk-ins, absent both. **Neither side uses `$ne null`**: `true` asks `$type: string` and `false` asks `$exists: false` **or** `null`, because a missing field and a field holding null both have to read as "no inquiry" and `$ne null` excludes neither correctly. |

The first three are the first three keys of `school_cycle_class_status_idx`, in that order, which is
also the order an admissions officer narrows in.

**Rows are thin.** The guardians, the answers and the evidence are on [#25](#t25). A form takes up
to 200 answers, so a twenty-row page would otherwise carry four thousand of them to draw a table
that shows none.

**The default order is `createdAt desc, applicationNo asc`, and `applicationNo` is the tiebreaker on
every other sort.** It is unique within a school (`school_application_no_uniq`), so every ordering
this endpoint can produce is total — without that, paging shows one row twice and never shows
another. Proven by mutation: twenty applications planted with one identical `createdAt`, and
dropping the tiebreaker made a page-by-page walk return duplicates.

**Not `submittedAt` as the default**, which looks like the obvious choice. A `DRAFT` has none, so
every unsubmitted form would sort together in an order nothing decides.

**`dateOfBirth` is deliberately off the sort allowlist**, with `guardians` and `formAnswers`.
Sorting is a read: order by a date of birth and walk the pages, and you learn every applicant's age
without a screen ever having shown one. Allowed: `applicationNo`, `applicantName`, `status`,
`submittedAt`, `createdAt`, `updatedAt` — anything else is `INVALID_SORT_FIELD`, and the refusal
lists the six.

**No gates, and the cycle does not have to be open.** This is a read, so it takes
`currentSchool.require()` rather than `requireUsable()` — a suspended school still reviews the
round it ran, and a closed round whose applications could not be listed would be unreviewable the
moment it stopped taking forms.

<a id="e25"></a>
**[#25](#t25) · `GET /applications/{id}`** — built — *the form in full*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`** — every field on it
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `name`, `academicYear`. **Tolerantly**: a missing round leaves the name off rather than refusing the read
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: every review of this form, by `schoolId` + `admissionApplicationDocsId`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: every offer revision, the same way
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `_id`, `name` — **one query for the applied class and every offered one together**

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/applications/{id}

NO BODY, no query parameters.

Four reads, none of them per row: the form, its
cycle, its reviews and offers, and one query for
every class name.
</pre></td>
<td><pre>
200 OK

{
  "admissionApplicationId": "6ab2...e50",
  "applicationNo": "APP/2026/09/000123",
  "admissionCycleDocsId": "6ab1...dc7",
  "admissionCycleName": "Main intake",  // resolved
  "academicYear": "2027-2028",
  "appliedClassDocsId": "6aa3...854a",
  "appliedClassName": "Grade 7",
  "applicantName": "Aarav Sharma",
  "dateOfBirth": "2018-08-14",
  "gender": "MALE",
  "status": "SUBMITTED",
  "guardians": [
    { "fullName": "Rohan Sharma", "relation": "FATHER",
      "phoneNumber": "+91 98765 43210",
      "primaryContact": true }
  ],
  "formAnswers": { "previousSchool": "ABC School" },
  "evidenceDocumentDocsIds": [],   // a LIST, not absent
  "submittedAt": "2026-09-22T07:10:03Z",
  "reviews": [], "reviewCount": 0, // empty until #26
  "offers":  [], "offerCount":  0, // empty until #29
  "createdAt": "...", "updatedAt": "...",
  "nextStep": "It has been submitted, so the form is
               frozen — #18 refuses to edit it..."
}
</pre></td>
</tr>
</table>

| | a #24 row | here |
|---|---|---|
| `guardians` | left out | **the snapshot, with phone numbers** |
| `formAnswers` | left out | **as sent, unvalidated** |
| `evidenceDocumentDocsIds` | left out | **here** |
| `admissionCycleName` · `academicYear` · `appliedClassName` | ids only | **resolved** |
| `withdrawnAt` · `withdrawalReason` · `resultingStudentDocsId` | left out | **here** |
| `reviews` · `offers` | — | **read from two other collections** |

**Four reads, none of them per row**: the application by id, the cycle by id, the reviews and the
offers by an indexed pair, and one query for every class name — the applied one and each offered
one together.

**The reviews and the offers are empty, and that is an answer rather than a gap.** [#26](#t26) and
[#27](#t27) create a review; [#29](#e29) to [#31](#e31) create an offer; none are built. The
queries run and are scoped to the school, and they return nothing because there is nothing — which
is exactly what they will return for an unreviewed application long after those endpoints exist.
Proven by planting rows directly: three reviews and two offers come back, and the application
beside them still reads zero.

**Reviews come back oldest round first, then by when they were created.** Round alone is not a
total order — `school_application_round_reviewer_uniq` lets one round hold two reviews, which is
the normal case for an interview plus a test. **Offers come back first revision first**, and there
the revision IS total (`school_application_offer_revision_uniq`), so no second field is needed.

**Every offer revision is returned, superseded ones included.** A later offer supersedes the one
before it, and an endpoint that showed only the live one would make "what did we originally offer
this family" unanswerable.

**The class and the cycle are named; the PEOPLE are not.** `reviewerDocsId` and
`assignedAdmissionOfficerDocsId` stay ids, because [#22](#t22) assigns an officer and
[#26](#t26) assigns a reviewer and neither is built — a name-resolving branch here could never run
and could never be tested. It gets written when the endpoint that fills the field is.

**A form whose round is GONE still reads.** The cycle is read tolerantly rather than through
`CrmHelper.loadCycle`, which throws: the caller asked for an application, and answering "no
admission cycle found" to that is a confusing 404 for a form that reads perfectly well. The name
and the year are left off instead — and the class name goes with them, because classes are stored
per academic year and the year came from the cycle. Same call [#6](#e6) makes about a seat row
naming a class that is gone.

**No gates.** `currentSchool.require()`, not `requireUsable()` — a suspended school opens a form it
already took. The id is scoped to the school **in the query**, which matters more here than
anywhere else in the module: an application carries a child's date of birth and their guardians'
phone numbers.

<a id="e26"></a>
**[#26](#t26) · `POST /applications/{id}/reviews`** — built — *put it on somebody's desk*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` — it has to be one somebody can usefully look at
- [`staff`](../../models/people/staff/Staff.java) — *reads*: the reviewer by `_id` **and `schoolId`**; then `fullName`. **Read rather than checked for existence**, because the name is wanted on the answer and this is the read that has it
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: `admissionApplicationDocsId` + `reviewRound` + `reviewerDocsId` — does that person already have that round
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *insert*: `schoolId`, `admissionApplicationDocsId`, `reviewRound`, `reviewerDocsId`, `reviewerRole`, `dueAt`, `notes`, `status` = `PENDING`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `UNDER_REVIEW`, **only from `SUBMITTED`**, and **after** the review is saved

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "reviewerDocsId": "6aa91f16ebf05fbafaa4ce22", // REQUIRED
  "reviewerRole": "ADMISSION_OFFICER",          // REQUIRED

  "reviewRound": 1,          // optional, 1..20, default 1
  "dueAt": "2027-03-15T17:00:00Z",   // optional
  "notes": "Interview first"         // optional, max 2000
}
</pre></td>
<td><pre>
201 Created
Location: /schools/current/reviews/6ab2...f9a

{
  "admissionReviewId": "6ab273d2cc4ee22d8e5a1f9a",
  "admissionApplicationDocsId": "6ab2...e50",
  "applicationNo": "APP/2026/09/000123",
  "reviewRound": 1,
  "reviewerDocsId": "6aa91f16ebf05fbafaa4ce22",
  "reviewerName": "Anita",        // resolved, free
  "reviewerRole": "ADMISSION_OFFICER",
  "status": "PENDING",            // assigning, not doing
  "dueAt": "2027-03-15T17:00:00Z",
  "notes": "Interview first",
  "createdAt": "2026-09-22T09:12:04Z",
  "nextStep": "Anita has round 1 of 'Aarav Sharma' as
               ADMISSION_OFFICER. Recording what they
               found is #27, which is not built..."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `reviewerDocsId` | **yes** | Max 60. Staff of **this school** → `404 STAFF_NOT_FOUND` otherwise, **another school's real id included**. |
| `reviewerRole` | **yes** | Max 60, **a free string**. There is no reviewer-role enum: schools run interviews, entrance tests and principal rounds under names of their own. Stored as sent. |
| `reviewRound` | no | `1..20`. **Absent means round 1**, which is what most applications get. The cap is a typo guard — `2026` in this field is somebody's mistake rather than a round. |
| `dueAt` | no | An Instant, and **a date in the past is accepted**: a school catching up on paperwork records a review that was due last week, and refusing that would make the backlog unrecordable. |
| `notes` | no | Max 2000. Anything to tell the reviewer. |

**It assigns the work; it does not do it.** The score, the criteria and the recommendation are all
[#27](#t27). If one call did both, there would be no state in which a review is *outstanding* — and
that state is the whole of [#28](#t28), a reviewer's queue.

**A round holds more than one reviewer, so the rule is one reviewer per round.** An interview and
an entrance test on the same day are two reviews of round 1, which is why
`school_application_round_reviewer_uniq` is keyed on the reviewer as well. The same person twice is
`409 REVIEWER_ALREADY_ASSIGNED` and the message names them and the round; the same person on a
different round is fine. **Asked before the insert**, so the caller gets that message rather than a
duplicate-key error. Proven by mutation: dropping the reviewer from the check refused the second
reviewer of round 1.

**The form has to be one somebody can usefully look at.**

| Status | |
|---|---|
| `SUBMITTED` · `UNDER_REVIEW` · `ADDITIONAL_INFORMATION_REQUIRED` · `WAITLISTED` | **reviewable** |
| `DRAFT` | the family has not sent it — the refusal points at [#19](#e19) |
| `REJECTED` · `WITHDRAWN` · `OFFERED` · `OFFER_ACCEPTED` · `ENROLLED` | decided; a review assigned now is work nobody would read |

**`WAITLISTED` is in the set deliberately** — a school holding an applicant for a seat often looks
at them again when one comes free, and the graph allows `WAITLISTED → APPROVED` for exactly that.

**It moves the application to `UNDER_REVIEW`, and only from `SUBMITTED`.** That is the one status
move this endpoint owns. The second reviewer of a round moves nothing, and
`ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW` belongs to [#20](#e20), which is what decides the
information arrived. **Done after the review is saved**: an application saying `UNDER_REVIEW` with
nobody reviewing it is a worse lie than one that is late.

**The `Location` is `/reviews/{id}`, not the path the request was made to.** A review is *created*
under the form it is of, because it has no meaning apart from it — and *addressed by its own id*
from then on, because a reviewer opens their own queue far more often than they walk down from an
application.

**Nothing checks that round 1 exists before round 2 is assigned.** A school numbering its rounds 1
and 3 is doing something odd, not something wrong, and a rule there would be invented rather than
observed.

<a id="e29"></a>
**[#29](#t29) · `POST /applications/{id}/offers`**

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `status` — must be `APPROVED` or `WAITLISTED`
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `enrollmentDeadlineAt` — the default `expiresAt`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: `revisionNo` — the current maximum for this form
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: the `ADMISSION_OFFER` counter
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *insert*: `offerNo`, `revisionNo` = max + 1, `admissionApplicationDocsId`, `offeredClassDocsId`, `expiresAt`, `depositInvoiceDocsId`, `issuedByDocsId`, `status`, `offeredAt`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: the previous revision's `status` = `SUPERSEDED`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `OFFERED`, **as a consequence rather than a request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `offeredClassDocsId` | String | **yes** | Usually the applied class; **not always** — a school offers a different grade after assessment. |
| `expiresAt` | Instant | no | Defaults to the cycle's `enrollmentDeadlineAt` when it has one. |
| `depositInvoiceDocsId` | String | no | |

The application must be `APPROVED` or `WAITLISTED`. `revisionNo` is `max + 1` for that application,
and **issuing a new one supersedes the last** — `SUPERSEDED`, which is the enum value the model
README's diagram omits. `offerNo` from `NumberSequenceType.ADMISSION_OFFER`.

<a id="e30"></a>
**[#30](#t30) · `POST /offers/{id}/respond`**

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: the offer by `_id` **and `schoolId`**; then `status`, `expiresAt`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: `response`, `respondedAt`, `status`, `acceptanceSignatureDocsId`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `OFFER_ACCEPTED` **on `ACCEPTED` only** — a declined offer is not a rejected applicant

| Field | Type | Required | Notes |
|---|---|---|---|
| `response` | AdmissionResponse | **yes** | `ACCEPTED` · `DECLINED`. |
| `acceptanceSignatureDocsId` | String | no | |

Offer must be `ISSUED` and not past `expiresAt`. On `ACCEPTED` the application moves to
`OFFER_ACCEPTED`; on `DECLINED` it stays where it is — **a declined offer is not a rejected
applicant**, and the school may issue another revision.

<a id="e31"></a>
**[#31](#t31) · `POST /offers/{id}/withdraw`** — `withdrawalReason` required.

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: `status` = `WITHDRAWN`, `withdrawalReason`

<a id="e33"></a>
**[#33](#t33) · `POST /applications/{id}/enroll`** — *the whole point, and the one that is blocked*

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: `status`, `response` — there has to be an accepted one
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `resultingStudentDocsId` — already enrolled is a refusal
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `capacities` — are the class's seats full
- [`students`](../../models/student/Student.java) — *insert*: **through `StudentService`, not written here** — [`student` #1](../student/README.md#e1) with `admissionApplicationDocsId` set
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `resultingStudentDocsId`, `status` = `ENROLLED`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: the accepted offer's `status`
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `status` = `CLOSED`, when the form came from a lead

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
**[#34](#t34) · `GET /admission-funnel?admissionCycleDocsId=`**

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: counted by `status` for one cycle's year
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: counted by `status` for one cycle
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: counted by `status`. **Three aggregations, not a join** — the collections share no key that would make one possible

Leads, applications, offers and enrolments as counts per stage, for one cycle. Three aggregations,
one per collection — **not** a join, because the three collections do not share a key that makes one
possible.

---

*Endpoints without an appendix entry — [#9](#t9),
[#11](#t11), [#12](#t12), [#14](#t14), [#16](#t16), [#18](#t18), [#21](#t21), [#22](#t22),
[#23](#t23), [#27](#t27), [#28](#t28), [#32](#t32) — take
what their tables and the status graphs above already say. An appendix row is written when the
endpoint is, so that it describes what was built rather than what was imagined. **Every one of the
twelve built endpoints now has a row**, which is the rule finally holding rather than a new one:
[#5](#e5) went in without one and got its row on 2026-09-22, when [#24](#e24) made the sort
allowlist worth writing down twice.*
