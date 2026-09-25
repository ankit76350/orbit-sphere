# controllers/crm — API plan

**Twenty-eight of the thirty-three are built, plus four that were not in the plan** — the whole cycle
half except [#7](#e7), the four application endpoints that take a form, send it and read it back,
and the whole review half.
[#1](#e1) opens a year for admissions, [#2](#e2) corrects one, [#3](#e3) moves it through its
lifecycle, [#4](#e4) sets its seats, [#5](#e5) lists the rounds and [#6](#e6) opens one in full.

**A family can apply, send the form, and have it read back.** [#17](#e17) takes one against an
open cycle, [#19](#e19) submits it and freezes the snapshot, [#24](#e24) lists the pipeline and
[#25](#e25) opens one in full — guardians, answers, evidence, and the reviews and offers from
their own collections.

**Phases 2 and 3 are complete.** [#22](#e22) gives a form to an admission officer, [#26](#e26)
puts it on a reviewer's desk, [#27](#e27) records what they found, [#27c](#e27c) finishes it and
[#27d](#e27d) calls it off, [#28](#e28) is their queue, and [#20](#e20) records what the school
decided — a form now runs from `DRAFT` all the way to `APPROVED`, owned by somebody the whole way,
with its assessment history behind it.

**Phase 4 is complete.** [#29](#e29) issues an offer, [#30](#e30) records the family's answer,
[#31](#e31) takes it back and [#32](#e32) is the chase list. **The module now runs out of road at
`OFFER_ACCEPTED`**, where it always said it would: the next thing that happens is that a seat becomes
a child on a register, and that is [#33](#e33), which needs [`student`](../student/README.md).

**The offer half was nearly deleted on 2026-09-23, and the argument is worth keeping.** It is a
collection, two enums, four endpoints, a revision model and expiry semantics for a step many schools
do with a phone call — and nothing had ever written to it, so removing it would have been free. What
kept it: **approving is the SCHOOL saying yes and is not the FAMILY saying yes.** A family applies
to five schools, three approve, one child arrives. A school that treats `APPROVED` as admitted
cannot tell a child who is coming from one who went elsewhere, and its seat counts are fiction.

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

**A review now has three of them**, and they are the clearest case in the module:
[`/start`](#e27b), [`/complete`](#e27c) and [`/cancel`](#e27d). [#27](#e27) can set all three
statuses itself, so these are not *capability* — they are the shapes the moves actually have. A
verb can insist on what its own move needs and refuse nothing else: `/complete` requires a
recommendation, `/cancel` requires a reason, and neither can be handed a body that asks for
nothing. A `PATCH` cannot say that without a rule of the form "this field is required, but only
when that field holds this value".

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

**Deeper in, the document's own status keeps taking that role.** [#26](#e26) asks whether the
*application* is reviewable; [#27](#e27), [#27b](#e27b), [#27c](#e27c) and [#27d](#e27d) ask where
the *review* is in its graph. Every one of those is a check the service makes with the document in
hand, which is why none of them is a gate: a gate answers before anything has been read.

**No gate runs on a read.** A suspended school still reads last year's admissions, because the
students it enrolled are still enrolled.

---

# The endpoints

Numbered by area, not by build order. **Build order is below** and differs.

**The numbers are #1 to #34 and they are not renumbered.** They are quoted from the Postman
collection, the service banners, the API tester's catalogue and half the javadoc in this package,
so closing a gap would break every one of those references.

**An endpoint the plan did not have gets a LETTER, not the next number.** [#27b](#e27b) was the
first — `#35` would read as the last item of a plan that never contained it, where `27b` says what
it is: something that arrived later and belongs beside [#27](#e27). The same call
[`controllers/core`](../core/README.md) made with its `D1` and `D2`.

**There are four of them now.** [#27b](#e27b) starts a review, [#27c](#e27c) finishes it,
[#27d](#e27d) calls it off — and [#27](#e27) can set every one of those statuses itself.
[#29b](#e29b) is the odd one out and the most necessary: it is a `PATCH`, not a verb, and it exists
because **the one-offer rule left a state nothing could get out of** — a lapsed letter that could
not be extended and could not be replaced. The other three are shape; that one is a fix. They exist because **starting, finishing and calling off are EVENTS**, not fields
being set, and a verb can insist on what its move needs: `/complete` asks for a recommendation,
`/cancel` asks for a reason, and neither can be sent an empty body that means nothing. It is the
same reading that gave [#19](#e19) `/submit` and [#3](#e3) `/status` instead of one large `PATCH`.

**What the marker in the `#` column means**, the same words
[`controllers/core`](../core/README.md) and [`controllers/plans`](../plans/README.md) use:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. **29 of the thirty-three** — #1 to #10, #12 to #15, #17 to #22, #24 to #32 — **plus [#27b](#e27b), [#27c](#e27c), [#27d](#e27d) and [#29b](#e29b)**, which the plan did not have. |
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
| <a id="t7"></a>7 — **built** | [`GET /admission-cycles/{id}/capacity`](#e7) | **Seats against applications** — and the only thing that says a round has **over-offered**. | `admission_cycles`, `admission_applications` |

## 3. The lead — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t8"></a>8 — **built** | [`POST /inquiries`](#e8) | Capture a lead. **The front desk's call**, and almost every field is optional. | [`inquiries`](../../models/crm/Inquiry.java) |
| <a id="t9"></a>9 — **built** | [`PATCH /inquiries/{id}`](#e9) | Correct the child's details or the guardians. **No status gate**, unlike [#18](#e18). | [`inquiries`](../../models/crm/Inquiry.java), [`school_classes`](../../models/academics/structure/SchoolClass.java), [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t10"></a>10 — **built** | [`POST /inquiries/{id}/follow-ups`](#e10) | Log one interaction. `$push`, and it moves `nextFollowUpAt`. | [`inquiries`](../../models/crm/Inquiry.java), [`staff`](../../models/people/staff/Staff.java) |
| <a id="t11"></a>~~11~~ — **removed** | ~~`POST /inquiries/{id}/assign`~~ | ~~Give the lead to a counsellor.~~ **Dropped 2026-09-24**, with the `assignedCounselorDocsId` it would have set. A lead is not owned by anybody: the school works one queue. |
| <a id="t12"></a>12 — **built** | [`POST /inquiries/{id}/status`](#e12) | Move it, including `LOST` with a reason. **The only thing that may.** | [`inquiries`](../../models/crm/Inquiry.java), [`staff`](../../models/people/staff/Staff.java) |

## 4. The lead — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t13"></a>13 — **built** | [`GET /inquiries`](#e13) | **The counsellor's worklist** — whose, what state, what is overdue. | [`inquiries`](../../models/crm/Inquiry.java), [`staff`](../../models/people/staff/Staff.java) |
| <a id="t14"></a>14 — **built** | [`GET /inquiries/{id}`](#e14) | One lead with its whole timeline. | [`inquiries`](../../models/crm/Inquiry.java), [`staff`](../../models/people/staff/Staff.java), [`school_classes`](../../models/academics/structure/SchoolClass.java) |
| <a id="t15"></a>15 — **built** | [`GET /inquiries/search?phone=&email=`](#e15) | **Is this family already known?** Asked before every new lead. | [`inquiries`](../../models/crm/Inquiry.java) |
| <a id="t16"></a>16 | [`GET /inquiries/{id}/applications`](#t16) | What the lead became. | `admission_applications` |

## 5. The application — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t17"></a>17 — **built** | [`POST /applications`](#e17) | Start one, optionally from an inquiry. | [`admission_applications`](../../models/crm/AdmissionApplication.java) |
| <a id="t18"></a>18 — **built** | [`PATCH /applications/{id}`](#e18) | Edit it **while it is still `DRAFT`**. | `admission_applications` |
| <a id="t19"></a>19 — **built** | [`POST /applications/{id}/submit`](#e19) | `DRAFT → SUBMITTED`. **Freezes the snapshot.** | `admission_applications`, `inquiries` |
| <a id="t20"></a>20 — **built** | [`POST /applications/{id}/decision`](#e20) | The review outcome: approve, reject, waitlist, ask for more. | `admission_applications` |
| <a id="t21"></a>21 — **built** | [`POST /applications/{id}/withdraw`](#e21) | The family pulls out. **From anywhere before `ENROLLED`**, and it needs a reason. | `admission_applications` |
| <a id="t22"></a>22 — **built** | [`POST /applications/{id}/assign`](#e22) | Give it to an admission officer. **Owning is not deciding** — it moves no status. | `admission_applications`, `staff` |
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
| <a id="t27"></a>27 — **built** | [`PATCH /reviews/{id}`](#e27) | **Submit the result** — score, criteria, recommendation. | `admission_reviews` |
| <a id="t27b"></a>27b — **built** | [`POST /reviews/{id}/start`](#e27b) | The reviewer has picked it up. `PENDING → IN_PROGRESS`, and nothing else. | `admission_reviews` |
| <a id="t27c"></a>27c — **built** | [`POST /reviews/{id}/complete`](#e27c) | **They are finished.** `COMPLETED`, with the recommendation — and the only thing that stamps `completedAt`. | `admission_reviews` |
| <a id="t27d"></a>27d — **built** | [`POST /reviews/{id}/cancel`](#e27d) | **The school called it off**, with a reason. What a `DELETE` would have been. | `admission_reviews` |
| <a id="t28"></a>28 — **built** | [`GET /reviews`](#e28) | **A reviewer's own queue.** What is due, and when. | `admission_reviews` |

## 8. The offer · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t29"></a>29 — **built** | [`POST /applications/{id}/offers`](#e29) | Issue an offer. **One letter per admission** — extended and corrected in place. | [`admission_offers`](../../models/crm/AdmissionOffer.java) |
| <a id="t29b"></a>29b — **built** | [`PATCH /offers/{id}`](#e29b) | **Correct that letter** — extend a lapsed one, or change the grade. Not in the plan. | `admission_offers` |
| <a id="t30"></a>30 — **built** | [`POST /offers/{id}/respond`](#e30) | The family answers: accepted or declined. | `admission_offers`, `admission_applications` |
| <a id="t31"></a>31 — **built** | [`POST /offers/{id}/withdraw`](#e31) | The school takes it back, with a reason. | `admission_offers` |
| <a id="t32"></a>32 — **built** | [`GET /offers`](#e32) | **What is expiring.** The chase list. | `admission_offers` |

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
| **3** | [3](../README.md#the-phases) | The pipeline can be worked | ~~20~~, ~~26~~, ~~27~~, ~~28~~, ~~22~~ |
| **4** | [4](../README.md#the-phases) | Offers can be made and answered — **and here it stops** | ~~29~~, ~~30~~, ~~32~~, ~~31~~ |
| **5** | [6](../README.md#the-phases) | A student comes out of the other end | 33 |
| **6** | [9](../README.md#the-phases) | The lead half, which nothing else needs | ~~8~~, ~~13~~, ~~14~~, ~~10~~, ~~12~~, ~~~11~~~, ~~9~~, ~~15~~, 16 |
| **7** | [10](../README.md#the-phases) | The rest | ~~2~~, ~~4~~, ~~7~~, ~~18~~, ~~21~~, 23, 34 |

**A ~~struck~~ number is built** — the same twenty-nine the `#` column marks, out of a plan that
is now **thirty-three** rather than thirty-four: [#11](#t11) was removed on 2026-09-24 rather than
built, said here so the order
shows where it has got to. **The lettered verbs are not in this table**, because the table is the
*plan's* order and they were never in the plan; [#27b](#e27b), [#27c](#e27c) and [#27d](#e27d)
arrived beside ~~27~~ once it was built, and [#29b](#e29b) beside ~~29~~. **Phases 1 to 4 are DONE**: a form runs from `DRAFT`
to `APPROVED`, is owned by an admission officer the whole way, can be put on a reviewer's desk,
started, assessed, finished or called off, and read back off their queue — then offered a seat,
answered by the family, and chased when it lapses. **What is left is #33**, which needs the
`student` module, and the rest of the lead half nothing depends on.

**Phase 6 can now be worked end to end.** [#8](#e8) captures a lead, [#9](#e9) fixes what the desk
misheard, [#10](#e10) logs each call, [#12](#e12) moves it — including giving up on it with a
reason — [#13](#e13) is the worklist and [#14](#e14) opens one in full. **Every row of the
[transition table](#inquirystatus--12) is now reachable.**

**[#10](#e10) is the one that made the other two mean anything.** #13 sorts on `nextFollowUpAt` and
#14 renders a timeline, and until it existed **every lead in the database had neither** — both reads
were correct and had nothing to show. It is worth knowing before the same order is chosen again:
two reads were built and verified against fields that nothing could write.

**What is left of the half is [#16](#t16)** — what a lead became. **[#11](#t11) was removed rather
than built**: a lead is not owned by anybody, so there is nobody to assign it to. That is why [#13](#e13)'s
`overdue` filter matches nothing today and [#14](#e14)'s timeline is always empty: the endpoints
are right and there is nothing yet to put in them. **[#10](#e10) is the one that changes that** —
it writes both `followUps[]` and `nextFollowUpAt`, which is every field the two reads exist to
show.

**#1 first, and nothing else works without it.** Every application names a cycle, and
[#17](#e17) refuses without one.

**Phase 4 is where this module runs out of road.** An application can reach `OFFER_ACCEPTED` and go
no further, because the next thing that happens to it is becoming a child on a register. That is the
moment [`student`](../student/README.md) gets built — four endpoints, just enough for [#33](#e33).

**The inquiry half is phase 6, not phase 1** — which looks backwards, since a lead comes before an
application in real life. It is deliberate: **an application does not need an inquiry**
(`inquiryDocsId` is nullable, for the family that walks in with a completed form), so the pipeline
is testable end to end without a single lead in the database.

**It did leave two write paths nothing could reach, and [#8](#e8) closed that on 2026-09-24.**
[#17](#e17) moves a named lead to `APPLICATION_STARTED` and [#19](#e19) to
`APPLICATION_SUBMITTED` — and until an inquiry could be created through the API, both branches were
reachable only by writing to Mongo directly, which is how the suites had been exercising them. That
is the cost of building a half nothing depends on last, and it is worth knowing before the same
order is chosen again. Building leads first would mean four
endpoints nothing else depends on before the module does anything. It is also **the one block that
is free to move earlier** if the lead-first experience is wanted sooner.

**#33 is no longer "blocked", it is scheduled.** See
[open item 1](#1-enrollment-is-blocked-on-a-module-that-does-not-exist).

**~~#7~~ and #34 are last of all.** Both are aggregations over applications, and both are much
easier to write once there is a realistic spread of statuses to aggregate — which is exactly how #7
went: it was built on 2026-09-24, after the offer half, because until offers existed there was
nothing to count against the seats.

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

No endpoint here takes a status on a document it is otherwise editing. Each move has its own
preconditions, its own side effects and its own refusals: `OFFERED` is [#29](#e29)'s consequence,
`OFFER_ACCEPTED` is [#30](#e30)'s, `ENROLLED` is [#33](#e33)'s. A single "set the status" endpoint
would be ten endpoints wearing one name, and the refusals would have nowhere to live. Writes that
are events get a verb — the same call [`people` #18b](../people/staff/README.md) made for
employment status.

**[#27](#e27) is the one place this is not true, and it is worth saying rather than hiding.** It is
a `PATCH` that accepts `status`, and it carries two conditional rules because of it —
`RECOMMENDATION_REQUIRED` fires only when the status being sent is `COMPLETED`, and
`CANCELLATION_NOTE_REQUIRED` only when it is `CANCELLED`. That is exactly the shape this rule exists
to avoid: a field whose requiredness depends on another field's value.

**[#27b](#e27b), [#27c](#e27c) and [#27d](#e27d) are the module moving back toward the rule.** Each
takes one move and asks for what that move needs, unconditionally. [#27](#e27) stays because a
reviewer part-way through wants to save a score without declaring themselves anything — the field
edit is real, and it is the *status* riding along with it that was the mistake.

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

An inquiry that came to nothing is `LOST`; an application is `WITHDRAWN`; a review is `CANCELLED`
([#27d](#e27d)); an offer is `WITHDRAWN` or `EXPIRED`. Admissions is the record of what a school
**decided** about a child, and the decision not to admit is exactly the part worth keeping. Where a
refusal needs explaining the reason is required rather than optional — `lostReason`,
`withdrawalReason`, and the note [#27d](#e27d) will not cancel without.

**[#27d](#e27d) is what a `DELETE /reviews/{id}` would have been**, and the difference is the whole
rule in one endpoint: the row stays, it says it was called off and why, and whatever the reviewer
had recorded before the school changed its mind is still on it.

---

# Things this module deliberately will not have

- **No `DELETE` on anything.** An inquiry that came to nothing is `LOST`; an application is
  `WITHDRAWN`; a review is `CANCELLED` ([#27d](#e27d)); an offer is `WITHDRAWN` or `EXPIRED`.
  Admissions is a record of what a school decided about a child, and the decision not to admit is
  exactly the part worth keeping.
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
    InquiryController.java            #8–#16 — all but #16 built; #11 removed
    AdmissionApplicationController.java  #17–#25, #33
    AdmissionReviewController.java    #26–#28, and #27b #27c #27d
    AdmissionOfferController.java     #29–#32 — all four, and #29b

services/crm/
    AdmissionCycleService.java
    InquiryService.java
    AdmissionApplicationService.java
    AdmissionReviewService.java
    AdmissionOfferService.java
    utils/    the reads each service makes more than once
        AdmissionApplicationServiceUtils.java   4 methods
        AdmissionCycleServiceUtils.java         1 method
        AdmissionReviewServiceUtils.java        4 methods
        AdmissionOfferServiceUtils.java         5 methods
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
under its `//! step N`, where it reads in the order it happens. **It counts callers, not queries**:
`AdmissionReviewServiceUtils` holds two lookups and two methods that read nothing at all —
`nextStepFor`, which four endpoints answer with, and `names`, which three refuse with.

**And a utils method may not call another one, which shows up as an argument.**
`AdmissionOfferServiceUtils.answerFor` was one method with `nextStepFor` inside it while both were
private in the service. Moving both across would have made one call the other, so `nextStep` became
a **parameter** and the service composes them: `utils.answerFor(school, offer, form,
utils.nextStepFor(offer) + ...)`. Two arguments in place of a hidden dependency, which is the
shape the rule is there to produce. That is why
`AdmissionCycleServiceUtils` holds one method and not three: `datesRunForwards` and that service's
`nextStepFor` each have a single caller, and moving them would buy a longer import list and a jump
to nowhere.

**`AdmissionReviewService` earned its `utils` file when [#27](#e27) and [#28](#e28) arrived** —
which is what the note here predicted while it still had one public method and nothing that could
repeat. Worth saying which two methods qualified and which did not.

**The shared reads are turning reviewer ids into names, and application ids into forms.** #27,
[#27b](#e27b), [#27c](#e27c) and [#27d](#e27d) each ask about one of each; #28 asks about a page —
five callers now, and every one of them answers with the whole review.

**Both are written as the BULK form even for one id**, deliberately. Writing the single-id version
as well would leave two ways to do one lookup, and the one-at-a-time way is the one that ends up
inside a loop. For a single id the bulk call is the same single query, so the safety costs nothing.

**The two lookups that THROW stayed inline.** [#26](#e26) refuses an unknown reviewer with
`STAFF_NOT_FOUND` and an unknown form with `APPLICATION_NOT_FOUND`; those are one-caller guards,
not shared reads. A tolerant lookup and a throwing one look alike and are not — folding them
together would mean a flag deciding whether an endpoint refuses.

**`nextStepFor` and `names` moved there too**, and they are the interesting pair, because
**neither of them reads anything**. One turns a status into a sentence about what to do next; the
other turns a set of statuses into a list a refusal can print. An earlier note here argued they
belonged beside the statuses they describe rather than in a file of queries.

**The rule counts callers, not queries.** Four endpoints answer with `nextStepFor` and three refuse
with `names`, and a private copy in the service is a second place for the module's own vocabulary
to drift from itself — which is exactly what a caller notices when [#27b](#e27b) tells them
something [#27c](#e27c) contradicts. `utils` is where a thing with more than one caller lives; that
it is a `switch` rather than a `find` does not change the count.

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
| `APPLICATION_NOT_EDITABLE` | 409 | [#18](#e18) on anything past `DRAFT`. The snapshot is frozen — what the family declared is the thing an admissions record is for. |
| `BLANK_APPLICANT_NAME` | 400 | [#18](#e18) sent an applicant name as `""` rather than leaving the field out. Absent keeps what is there; empty is a caller trying to remove a name the document requires. |
| `INVALID_APPLICATION_TRANSITION` | 409 | [#19](#e19) on anything that is not a `DRAFT` (re-submitting included), or [#20](#e20)/[#21](#t21) asking for a move the status graph does not have. **[#20](#e20)'s message lists what IS reachable**, and when nothing is, says why. |
| `APPLICATION_NOT_ASSIGNABLE` | 409 | [#22](#e22) tried to give a `REJECTED`, `WITHDRAWN` or `ENROLLED` form to an admission officer. A form is given to somebody so they can move it along, and those have stopped. **A `DRAFT` is fine** — unlike a review. |
| `DECISION_NOTE_REQUIRED` | 400 | [#20](#e20) moved a form to `REJECTED` or `ADDITIONAL_INFORMATION_REQUIRED` with no reason. A blank one counts as none. |
| `REVIEWS_STILL_OUTSTANDING` | 409 | [#20](#e20) tried to move a form to `APPROVED` while one of its reviews is `PENDING` or `IN_PROGRESS`. **The message names the rounds.** `CANCELLED` and `COMPLETED` do not hold it up, and a form with no reviews is unaffected — this is the only decision the rule applies to. |
| `DUPLICATE_CAPACITY_CLASS` | 409 | [#4](#e4) listed one class twice. |
| `RESERVED_EXCEEDS_TOTAL` | 400 | [#4](#e4) reserved more seats than the class offers. |
| `CLASS_NOT_IN_CYCLE_YEAR` | 409 | The applied class belongs to a different academic year than the cycle. |
| `CLASS_NOT_IN_CAPACITY` | 409 | The cycle's seat table does not list that class. |
| `REVIEW_NOT_FOUND` | 404 | No review with that id in this school. |
| `APPLICATION_NOT_REVIEWABLE` | 409 | [#26](#e26) on a `DRAFT` nobody sent, or on a form already decided. |
| `REVIEWER_ALREADY_ASSIGNED` | 409 | [#26](#e26) — that reviewer already has that round of that application. A *different* person on the same round is fine. |
| `REVIEW_ROUND_OUT_OF_ORDER` | 409 | [#26](#e26) asked for a round with nothing before it — round 2 on a form with no round 1, or round 5 out of nowhere. |
| `REVIEW_ALREADY_COMPLETED` | 409 | [#27](#e27), [#27b](#e27b), [#27c](#e27c) or [#27d](#e27d) on a review that is already `COMPLETED`. Cancel and assign another instead — that keeps both in the history. |
| `REVIEW_CANCELLED` | 409 | The same four, on one the school called off. The other terminal end. |
| `INVALID_REVIEW_TRANSITION` | 409 | A move the review's graph does not have — going backwards on [#27](#e27), mostly, or starting one somebody has already picked up ([#27b](#e27b)). |
| `RECOMMENDATION_REQUIRED` | 400 | [#27](#e27) or [#27c](#e27c) completing a review that has never said what it recommends. **One vocabulary across both** — a caller should not learn two names for the same fact. |
| `CANCELLATION_NOTE_REQUIRED` | 400 | [#27](#e27) or [#27d](#e27d) cancelling one that has never said why. |
| `OFFER_NOT_FOUND` | 404 | No offer with that id in this school. |
| `APPLICATION_NOT_ELIGIBLE_FOR_OFFER` | 409 | [#29](#e29) on a form that is not `APPROVED` or `WAITLISTED`. An offer follows a decision rather than making one. **It was `APPLICATION_NOT_APPROVED` until 2026-09-23** — renamed because the set allows `WAITLISTED` too, so "not approved" was describing a rule the endpoint does not have. |
| `FEE_INVOICE_NOT_FOUND` | 404 | [#29](#e29) named a deposit invoice that is not this school's. **Every id is refused today** — nothing writes `fee_invoices`, so there is none to point at, and that is the honest answer rather than storing whatever was typed. |
| `OFFER_NOT_FOUND` | 404 | [#30](#e30) or [#31](#e31) on an offer id that is not this school's. |
| `OFFER_NOT_OPEN` | 409 | [#30](#e30) or [#31](#e31) on an offer that is not `ISSUED` — already answered, already withdrawn, or never issued. **An `ACCEPTED` one refuses [#31](#e31) with a message about [#20](#e20)**: the family holds the seat, and taking it away is a decision about the child. |
| `OFFER_EXPIRED` | 409 | [#30](#e30) on an offer past its `expiresAt`. **Its stored status still reads `ISSUED`** — nothing writes `EXPIRED` — so only the clock knows, and [#32](#e32) is how a school finds them first. |
| `OFFER_EXPIRY_IN_THE_PAST` | 400 | [#29](#e29) asked for a deadline that has already gone — **including the cycle's own `enrollmentDeadlineAt`**, when the request named none. An offer nobody could accept is not an offer. |
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


## The four dates must fall inside the academic year — 2026-09-25

**[#1](#e1) and [#2](#e2) both refuse a cycle date outside the year it admits for**, with
`400 CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR`. Until this, a cycle for `2026-2027` could carry an
enrolment deadline in **2099** or an inquiry date in **2019** — both were in the database when the
rule was written, and neither is a date anybody meant.

**The rule itself is shared, and the NAME OF THE REFUSAL is not.** The check lives in
[`common/time/AcademicYearWindow`](../../common/time/AcademicYearWindow.java), modelled on
[`ActionGate`](../../common/access/ActionGate.java) — one component, `require...` methods, one
place for a rule that has to read the same way everywhere. **The caller passes the error code in**:
a cycle's is `CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR`, a term's will be its own. A single code inside the
component would make every module's error table describe somebody else's endpoint.

**It is NOT in `common.access` beside the gates**, and that is deliberate. Every gate there answers
*"may this caller act"* and is called from the **controller** under its `Gate N` banner. This one
checks **values**, and a PATCH's values are the stored ones merged with the sent ones — which only
the service has. Filing it with the gates would invite the controller-only rule to be applied to it
and be wrong for [#2](#e2) every time.

**Checked before the order check**, and that ordering is load-bearing: a *middle* date outside the
year is necessarily out of order too, so with the order check first, three of the four fields could
only ever report `CYCLE_DATES_OUT_OF_ORDER` — telling a caller to reorder dates whose real problem
is the year. Measured: the suite could not prove the middle two were range-checked at all.

**The whole of the end day counts, in the school's own zone.** A year's `endDate` is a `LocalDate`,
so a deadline at 18:00 on the last day is inside the year rather than a day past it. Comparing a
school in Kolkata against UTC midnight would refuse the last afternoon of every year — and a
mutation that did exactly that survived the suite until two instants straddling the difference were
added.

**#2 is checked on the MERGED four, not on what was sent.** Without that the rule is one call away
from being bypassed: create inside the year, then patch the deadline to 2099.

### A consequence worth knowing before relying on it

**A school normally admits for a year before that year begins.** The model's own documented example
opens enquiries on 1 January for a year starting 1 April. **This rule refuses that.** It was asked
for as *"all the dates must belong in the academic year range"*, and that is what it does — the
suite pins the refusal rather than hiding it, so the day somebody wants the ordinary calendar back
it is clear the behaviour is deliberate. Relaxing it to *"nothing after the year ends"* would allow
the normal calendar and still catch the 2099 deadline.

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

              APPLICATION_STARTED ──> APPLICATION_SUBMITTED ──> CLOSED

and the early half may skip forward:

  NEW · CONTACTED · COUNSELLING                    ──> VISIT_SCHEDULED
  NEW · CONTACTED · COUNSELLING · VISIT_SCHEDULED  ──> VISITED

any status before a form exists ──> APPLICATION_STARTED   (#17 only)
any non-terminal                ──> LOST                  (requires lostReason)
```

**The early half skips forward, and that is deliberate.** `VISIT_SCHEDULED` is reachable from
`NEW`, `CONTACTED` and `COUNSELLING`; `VISITED` from all three of those **and** from
`VISIT_SCHEDULED`. Two real things happen that a strict chain would refuse:

- **A family walks in.** They visited, and nobody scheduled anything — the lead was `NEW` that
  morning. Forcing the desk through `CONTACTED` and `VISIT_SCHEDULED` first would be three calls
  logged that never happened.
- **A family books a visit on the first call.** A parent rings, asks to come and see the place, and
  a date is agreed. There was no separate counselling step, and inventing one would put a fiction
  in the timeline.

**What it still refuses is going backwards.** A lead that has visited cannot return to `NEW`, and
nothing reaches the two below without a form.

`APPLICATION_STARTED` and `APPLICATION_SUBMITTED` are set by [#17](#e17) and [#19](#e19), **not by
[#12](#t12)** — a lead's application state is a fact about the application, and letting a counsellor
type it would let the two disagree.

**`APPLICATION_STARTED` is reachable from every pre-application status, and this drawing used to
say otherwise.** It had the edge coming only from `COUNSELLING` and `VISITED`, which was a route
[#17](#e17) has never taken: **#17 does not consult this table at all**, and sets the status
unconditionally once a form naming the lead is saved. A family can fill a form in on the first
call, and nothing makes them ring twice first. Corrected 2026-09-24, when the screen's move table
started counting rows and the counts did not add up.

**A consequence worth knowing: [#17](#e17) will revive a `LOST` lead.** It sets
`APPLICATION_STARTED` without looking at where the lead is, so naming a lost lead on a new form
moves it back into the pipeline. That is arguably right — the family came back — but it is not a
decision anybody made, and the table above says `LOST` leads nowhere. See
[open items](#things-this-module-deliberately-will-not-have).

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

WAITLISTED ──> OFFERED                   (#29 — straight off the waiting list)
anything before ENROLLED ──> WITHDRAWN   (requires withdrawalReason)
```

**`WAITLISTED` reaches `OFFERED` directly** — added when [#29](#e29) was built, and the plan's own
wording is what asked for it: an offer may be issued against an `APPROVED` *or* a `WAITLISTED` form.
A seat comes free and the school offers it; going through [#20](#e20) first would record a decision
it never made separately from the offer.

**`SUBMITTED` reaches the outcomes directly, without passing through `UNDER_REVIEW`** — added when
[#20](#e20) was built. A school that decides in a conversation never assigns a reviewer, and the
endpoint would otherwise force it to invent one.

Which endpoint owns which move is the point: `OFFERED` is [#29](#e29)'s side effect,
`OFFER_ACCEPTED` is [#30](#e30)'s, and `ENROLLED` is [#33](#e33)'s. **No endpoint sets these by
being told to** — they are consequences.

<a id="review-status-graph"></a>
## `AdmissionReviewStatus` — [#27](#e27), [#27b](#e27b), [#27c](#e27c), [#27d](#e27d)

```text
PENDING ──> IN_PROGRESS ──> COMPLETED     (requires a recommendation)
   │                            
   └──> COMPLETED               (PENDING skips the middle)

PENDING · IN_PROGRESS ──> CANCELLED       (requires a note saying why)
```

**This graph was missing from this file until 2026-09-23**, while four endpoints moved it. It is
`REVIEW_MOVES` on `AdmissionReviewService`, and the terminal ends are spelled out there as empty
sets rather than left absent — an absent key and an empty set mean the same thing to the code, but
only one of them says it was decided.

**`PENDING → COMPLETED` skips `IN_PROGRESS` on purpose.** Most reviews are done in one sitting, and
forcing a "I have started" call first would be ceremony nobody would keep up.

**Both ends are terminal, and that is the rule the rest follows from.** A score recorded wrongly is
corrected by cancelling this review and assigning another with [#26](#e26), which leaves **both** in
the history rather than quietly overwriting one. It is also why there is no `DELETE`.

**`completedAt` is stamped on the way into `COMPLETED` and nowhere else** — not by
[#27b](#e27b), and not by a cancellation however much of the review was filled in.

**A review is never `APPROVED` or `REJECTED`.** Those are the *application's* status
([#20](#e20)) and the reviewer's `recommendation`, which is a different field on the same document.
Four statuses is all `AdmissionReviewStatus` has, and every endpoint above guards all four.

**Nothing here touches the application.** [#26](#e26) moves a form to `UNDER_REVIEW` when the first
review is assigned, and after that the two graphs are independent: three reviewers recommending
`APPROVE` do not approve anybody. [#20](#e20) is the school's decision and it reads no review at all.

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

The same 34 endpoints — **plus [#27b](#e27b), [#27c](#e27c) and [#27d](#e27d), which the plan did
not have** — with the fields each one reads and each one writes. Written so that whoever changes an
endpoint does not have to work this out again from the models, and so a reviewer can see at a glance
whether a change reaches a field it should not.

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
still be wrong when it is built. Twenty-eight of the thirty-three are built, plus the four lettered
verbs, and an entry gets its field tables and its request and response the day its endpoint does — so an unmarked entry is deliberately
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
| `assignedAdmissionOfficerDocsId` | String, optional | A `staff` id, set by [#22](#e22) — the only endpoint that writes it. **Checked against this school's staff on the way in** (`404 STAFF_NOT_FOUND`), so it is never a dangling id at the moment it is written; it can still dangle later if the person leaves, which [#24](#e24) and [#25](#e25) answer by returning the id with no name. Reassigning is a plain overwrite. |
| `submittedAt` | Instant, optional | **Set once, by [#19](#e19).** Absent while `DRAFT`. **Not the default sort on [#24](#e24)** although it looks like the obvious choice: a `DRAFT` has none, so every unsubmitted form would sort together in an order nothing decides. |
| `decidedAt` | Instant, optional | Set by [#20](#e20) every time the school decides. **Added with that endpoint on 2026-09-22**, because there was nowhere to put the answer: the model carried `withdrawnAt`/`withdrawalReason` for [#21](#t21) and nothing for the decision itself. **Not the same as `updatedAt`** — a later edit moves that; this stays on the moment the school made up its mind. |
| `decisionNote` | String, optional | **Open** — `max 2000`. **Required** when [#20](#e20) moves a form to `REJECTED` or `ADDITIONAL_INFORMATION_REQUIRED` → `400 DECISION_NOTE_REQUIRED`; a blank counts as none. **Kept, not logged and dropped** — a refusal with no reason is the part of an admissions record worth the most. Read back on [#25](#e25) only; a [#24](#e24) row does not carry it. A decision that sends no note leaves the previous one alone. |
| `withdrawnAt` `withdrawalReason` | Instant / String, optional | [#21](#e21)'s, and the reason is **required** — see [the rules](#there-is-no-delete-on-anything-and-the-reasons-are-in-the-record). **Not the same fields as `decidedAt`/`decisionNote`**: those are the school's own word about what *it* decided, these are what the family said when they left, and [#21](#e21) writes neither of the others. |
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
status. [#8](#e8) captures one, [#13](#e13) lists them and [#14](#e14) opens one in full.

| Field | Type | What can be in it |
|---|---|---|
| `inquiryNo` | String, required, unique per school | From `NumberSequenceType.ADMISSION_INQUIRY`. Generated, never supplied. |
| `prospectiveStudentName` | String, required | **Open.** **An inquiry is per prospective CHILD, not per family** — a parent enquiring about two children is two inquiries, which is what makes `school_cycle_inquiry_uniq` a sane rule. |
| `dateOfBirth` `gender` | LocalDate / Gender, optional | **Optional here and required on an application.** A parent ringing to ask about fees has not filled anything in. |
| `guardians` | List, required | The same embedded type as above. |
| `academicYear` | String, required | The year they are asking about. A property, not a scope — same as the cycle. |
| `interestedClassDocsId` | String, optional | What they asked about, not what they applied for. |
| `status` | [InquiryStatus](../../models/crm/enums/InquiryStatus.java), required | **`NEW`** at create. Nine values. [#10](#e10) and [#12](#e12) walk the [table](#inquirystatus--12); [#17](#e17) sets `APPLICATION_STARTED` and [#19](#e19) sets `APPLICATION_SUBMITTED`, which neither of the other two may type. `LOST` is [#12](#e12)'s alone and requires `lostReason` → `400 LOST_REASON_REQUIRED`. |
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

**[#26](#e26) creates a row, [#27](#e27) records on it, and [#25](#e25) and [#28](#e28) read it.**
The whole collection's lifecycle exists.

| Field | Type | What can be in it |
|---|---|---|
| `admissionApplicationDocsId` | String, required | Which form. |
| `reviewRound` | Integer, required | **Defaults to `1`**, and `1..20` on the request — the cap is a typo guard, not a rule about how often a school may review somebody. **Rounds run 1, 2, 3 with no gaps** (2026-09-23): round N needs round N-1 to exist on that application, by anybody. **A round may hold more than one review** — an interview and a test — which is why `school_application_round_reviewer_uniq` is keyed on the *reviewer* too, and why [#25](#e25) orders by round **and then `createdAt`**. |
| `reviewerDocsId` | String, required | `max 60`. **Must be staff of this school** → `404 STAFF_NOT_FOUND`, another school's real id included. [#26](#e26) *reads* the record rather than checking it exists, because the name is wanted on the answer. |
| `reviewerRole` | String, required | **Open** — `max 60`. Schools run interviews, entrance tests and principal rounds under names of their own, so an enum would be wrong within a month. See [open item 7](#7-reviewerrole-is-a-free-string). |
| `status` | [AdmissionReviewStatus](../../models/crm/enums/AdmissionReviewStatus.java), required | **`PENDING`** at create; [#27](#e27) moves it, and so do the three verbs — [#27b](#e27b) to `IN_PROGRESS`, [#27c](#e27c) to `COMPLETED`, [#27d](#e27d) to `CANCELLED`. [The graph](#review-status-graph) is `PENDING → IN_PROGRESS → COMPLETED`, with `CANCELLED` reachable from either live state and `PENDING → COMPLETED` skipping the middle. Off-graph is `409 INVALID_REVIEW_TRANSITION`. **Both ends are terminal** — a score recorded wrongly is corrected by cancelling and assigning another, which is why there is no `DELETE`. |
| `dueAt` | Instant, optional | What [#28](#e28)'s queue sorts on, and half of what `overdue` asks. **A date in the past is accepted** — a school catching up on paperwork records a review that was due last week, and refusing it would make the backlog unrecordable. |
| `completedAt` | Instant, optional | Stamped by [#27](#e27) and [#27c](#e27c) **on the way into `COMPLETED` and nowhere else** — not by [#27b](#e27b), and not by [#27d](#e27d) — a cancelled review was not completed, however much of it was filled in. |
| `score` | BigDecimal, optional | **`@PositiveOrZero`, and no upper bound** — out of 100, out of 50, out of 5 is the school's business and a cap would refuse a school for marking differently. Negative is a typo, not a scale, and the digit limits are a typo guard for the same reason. |
| `recommendation` | [AdmissionRecommendation](../../models/crm/enums/AdmissionRecommendation.java), optional | The same four values [#20](#e20)'s decision takes. |
| `criterionScores` | Map, optional | **Open** — `{"INTERVIEW": 42.50}`, `max 50` entries. Nothing checks the keys: there is no criterion-definition model, so a school names its own parts. **[#27](#e27) and [#27c](#e27c) REPLACE the whole map rather than merging into it** — merging would leave no way to remove a criterion recorded by mistake, and `{}` therefore clears it. Left off a response when empty. |
| `notes` | String, optional | **Open.** |

### `admission_offers` — [AdmissionOffer](../../models/crm/AdmissionOffer.java)

**[#29](#e29) writes this collection**; [#30](#e30) and [#31](#e31) are not built. [#25](#e25)
reads it.

| Field | Type | What can be in it |
|---|---|---|
| `offerNo` | String, required, unique per school | From `NumberSequenceType.ADMISSION_OFFER`. |
| `revisionNo` | Integer, required | **Always `1`** since the one-offer rule replaced revisions on 2026-09-23. It is kept because the declared unique index `school_application_offer_revision_uniq` uses it: pinning it to 1 is what makes that index mean *one offer per application*. It was `max + 1` per application, and [#25](#e25) still needs no tiebreaker on offers for the same reason — there is at most one. |
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
| the four dates | **yes, all four** | **Changed 2026-09-22; they used to be optional.** Each must fall **inside the academic year** → `400 CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR` *(added 2026-09-25, checked first)*, and they must run forwards — enquiries open, applications open, applications close, enrollment deadline → `400 CYCLE_DATES_OUT_OF_ORDER`. An Instant is UTC: `2027-01-31T18:29:59Z` is one second to midnight in India, and `23:59:59Z` would hand the school most of the next day. |
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
**[#7](#t7) · `GET /admission-cycles/{id}/capacity`** — built — *seats against reality*

- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `capacities` — what the school configured. **Throws** if the round is gone
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `appliedClassDocsId`, `status` — **counted per class, never listed**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `name`, one query for the whole table

One row per configured class: `totalSeats`, `reservedSeats`, `openSeats`, and the **computed**
`pending`, `approved`, `waitlisted`, `offered`, `accepted`, `enrolled`, `rejected`, `withdrawn`,
`committed`, `freeSeats`, `overCommitted` — plus the same numbers totalled and a count of
over-committed classes.

**This is the counterpart to a decision [#29](#e29) made on purpose.** #29 does **not** cap offers
against the seat table, because schools deliberately over-offer — sixty letters for forty places,
because a fifth of families go elsewhere. The note written there was *"counting offers against
places is #7's job"*, and until this was built **over-offering was invisible**: nothing anywhere
told a school it had promised more seats than it has.

**`freeSeats` may be negative, and that is the endpoint.** Clamping it at zero would hide the one
thing it exists for — *"0 free"* cannot tell *"exactly full"* from *"twenty over"*.

**Approvals are not commitments.** `committed` is `offered + accepted + enrolled`. A school that has
approved forty children has *decided* something; it has not promised anybody a seat until a letter
goes out. Counting approvals would make every round look over-subscribed the moment it started
deciding — and it survived mutation testing only because the suite asserts it directly.

**A `DRAFT` is in no bucket at all.** The family has not sent it, so it is not this round's problem.

**One row per CONFIGURED class**, and only those. A class nobody set seats for is not part of this
round: [#17](#e17) refuses an application for it, so it cannot have applicants either.

**The counts are computed, never stored** — the model README says so, and the reason is the one
`AdmissionCycle` would otherwise become: a high-contention document every application write has to
touch. **One grouped aggregation for the whole table**, not one query per class; that is the same
N+1 [`people` #15](../people/department/README.md#e15) names about `filledHeadcount`. It is the only
aggregation in this module, because it is the only endpoint that answers a question about totals
rather than about rows.

**Two things these numbers get wrong on purpose, for now, and both are measured rather than
guessed:**

- **A declined family still holds a seat.** [#30](#e30) with `DECLINED` marks the *offer* declined
  and leaves the *application* at `OFFERED`, so that seat stays `committed` for ever. A round that
  offers forty and is declined by ten reads as forty committed. The plan's reason for leaving the
  form there — *"the school may issue another revision"* — stopped being true when
  one-letter-per-admission replaced revisions.
- **A different grade is counted against the applied class.** The counts group by
  `appliedClassDocsId`, and [#29](#e29) deliberately allows offering another grade. Fixing it means
  a second aggregation over `admission_offers` keyed on `offeredClassDocsId`, which this endpoint's
  own collection list predates.

**The tenant scope on the aggregation survived mutation, and stays anyway.** A cycle id is a
globally unique ObjectId and the cycle is loaded school-scoped first, so a foreign id is a 404
before the aggregation runs — the `schoolId` in the `$match` is defence in depth rather than a
reachable boundary. That is exactly the kind of line somebody later "simplifies" away.

<a id="e8"></a>
**[#8](#t8) · `POST /inquiries`** — built — *the front desk's call*

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: whether the year exists. **Not whether it is running**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: the interested class, **only when one is named**, in that year
- [`staff`](../../models/people/staff/Staff.java) — *reads*: the counsellor, **only when one is named**
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: the `ADMISSION_INQUIRY` counter
- [`inquiries`](../../models/crm/Inquiry.java) — *insert*: `inquiryNo`, `prospectiveStudentName`, `dateOfBirth`, `gender`, `guardians`, `academicYear`, `interestedClassDocsId`, `source`, `sourceDetails`, `notes`, `status` = `NEW`, `followUps` = `[]`

| Field | Type | Required | Notes |
|---|---|---|---|
| `prospectiveStudentName` | String | **yes** | |
| `academicYear` | String | **yes** | Must exist. Need not be running. |
| `dateOfBirth` · `gender` | LocalDate · Gender | no | A phone enquiry often has neither. |
| `interestedClassDocsId` | String | no | Must belong to `academicYear` when given. |
| `guardians[]` | InquiryGuardian | no | At least one is strongly wanted but **not required**: a walk-in with a name and nothing else is a real lead. |
| `source` · `sourceDetails` · `notes` | String | no | |

`inquiryNo` is generated from `NumberSequenceType.ADMISSION_INQUIRY`. Status starts `NEW`.

**Almost everything is optional, and that is the endpoint's whole character.** A phone call is *"a
mother rang about her son for next year"* — a name, a year, and nothing else. An endpoint that
demanded a date of birth and a guardian's email would be **refusing the commonest lead there is**,
and the front desk would stop using it.

**Every field on a GUARDIAN is optional too**, which is where it differs from
[#17](#e17)'s. On an application a guardian's `fullName` and `relation` are required, because the
family filled a form in; on a lead the desk writes down a first name and a phone number, and a
record that refused that would refuse the call. **That phone number is what [#15](#e15) searches
on**, and it can only find the families who left one.

**It creates `NEW` and nothing else.** Every other status is somebody having *done* something —
[#10](#e10) logs a call, [#12](#t12) moves it, and [#17](#e17) and [#19](#e19) move it as a side
effect of the family applying.

**It does NOT check for duplicates, and it should not.** [#15](#e15) asks *"is this family already
known"* and is asked **before** this, by the person at the desk who can see the answer and decide.
Refusing here would mean guessing that two children sharing a phone number are one enquiry —
**which a family with two children is not**.

**Both optional ids are read scoped by school**, and mutation could not tell that until the suite
sent a **real id belonging to another school**. A nonexistent id answers the same either way; only
somebody else's real one proves the scope.

<a id="e9"></a>
**[#9](#t9) · `PATCH /inquiries/{id}`** — built — *correct what the front desk misheard*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**
- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: by `schoolId` + `name` — only when the year is being moved
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `_id` + `schoolId` + `academicYear` — the class sent, **or the one already stored when the year moves**
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `prospectiveStudentName`, `academicYear`, `dateOfBirth`, `gender`, `interestedClassDocsId`, `guardians`, `source`, `sourceDetails`, `notes`

### A lead is the school's own notes, not a declaration the family signed

That one sentence is what the endpoint follows from, and it is the whole difference between it and
[#18](#e18).

**There is no status gate.** #18 refuses anything but a `DRAFT` application, because [#19](#e19)
freezes a snapshot of what the family declared and a school that could rewrite it afterwards could
not answer what they actually said. **Nobody declares a lead.** Somebody took a phone call and
wrote down what they heard, and the commonest thing that happens to a phone call is mishearing it —
so a `LOST` lead can still have a misspelt name put right.

**Correcting a lead never touches the application it produced.** [#17](#e17) *copies* the guardians
onto the form when it starts one, so the two have been separate records ever since. A lead at
`APPLICATION_SUBMITTED` is correctable and the form it produced is still frozen — which is the
clearest way to see why one of these has a gate and the other does not.

### A blank clears; an absent field is left alone

`""` is how a caller says *"they no longer have a class in mind"* or *"that note was a mistake"*.
**This is the first endpoint in the module to have that convention**, and it is here because
without it an optional field could be set once and never taken back. [#29b](#e29b) has the same
gap and has not been changed for it.

A **required** field refuses a blank instead — `400 BLANK_STUDENT_NAME` and
`400 BLANK_ACADEMIC_YEAR` — exactly as #18 refuses an empty `applicantName`.

**`dateOfBirth` and `gender` cannot be cleared, and that is a gap rather than a decision.** Neither
is a string, so neither has a blank to send. Inventing a sentinel for two fields would be worse
than saying so.

### Moving the year re-checks the class — including one nobody mentioned

A lead's interested class must be a class of the year the lead is about: [#8](#e8) enforces it and
[#14](#e14) resolves the name with it. **A year that moved and left an unrelated class behind would
break that silently**, and the lead would come back with no class name and no reason.

So `academicYear` alone, on a lead that has a class, is `409 CLASS_NOT_IN_CYCLE_YEAR` — about a
field the request never mentioned. The message names both ways out: send a class of the new year as
well, or send `""` to clear it.

**Testing the first of those needed a planted class.** Creating one through the API runs **gate 4**,
which refuses every year but the running one — and the running year is the one the lead is already
about. So the only year a lead can be moved to is a non-running one with no classes in it.

### Guardians are replaced whole, and `[]` clears them

A list is one value and there is no id on a guardian to merge *by* — they are embedded, not
documents. **An empty list is allowed here and refused by #18**: an application with no guardian is
not one a school can act on, but a lead with none is the walk-in who gave a child's name and left,
which #8 is built to accept. Refusing it would be this endpoint disagreeing with the one that
creates them.

### `academicYear` IS a field here, unlike #18's cycle

A form belongs to the round it was created against — the round decides the seat table and the
window #19 checks — so moving it is not a correction, it is a different application. A lead's year
is a label on a phone call, and *"next year"* is the first thing anybody says and the first thing
anybody mishears. **Nothing downstream reads it**: #17 takes its year from the *cycle*.

### What another endpoint owns is not a field here

| Field | Whose it is |
|---|---|
| `status`, `lostReason` | [#12](#t12) — the only thing that may walk the [transition table](#inquirystatus--12) |
| `nextFollowUpAt`, `followUps` | [#10](#e10) — which writes both together. A chase date with no call logged beside it is a promise with no record of who made it |
| `inquiryNo` | generated; nobody picks their own |

Sent anyway they are **ignored, not refused** — Jackson drops unknown fields.

| Refusal | When |
|---|---|
| `404 INQUIRY_NOT_FOUND` | No lead of that id **in this school** — including one that is real and somebody else's. |
| `400 NOTHING_TO_UPDATE` | A body that asks for nothing. **Checked before the version**, so a stale version cannot mask it. |
| `409 CONCURRENT_MODIFICATION` | The `version` sent is not the one stored. Leaving it out skips the check. |
| `400 BLANK_STUDENT_NAME` · `400 BLANK_ACADEMIC_YEAR` | `""` where the field is required. |
| `404 ACADEMIC_YEAR_NOT_FOUND` | A year this school does not have. |
| `409 CLASS_NOT_IN_CYCLE_YEAR` | A class that is not of the lead's year — the one sent, **or the one already stored**. |
| `400 VALIDATION_FAILED` | A date of birth in the future, more than ten guardians, or a field over its length. |
| `409 SCHOOL_NOT_EDITABLE` · `409 SUBSCRIPTION_NOT_USABLE` | Gates 1 and 2. A write. |

<a id="e10"></a>
**[#10](#t10) · `POST /inquiries/{id}/follow-ups`** — built — *log one interaction*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**
- [`staff`](../../models/people/staff/Staff.java) — *reads*: by `_id` + `schoolId` — only when the caller says who logged it
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `followUps` — **a `$push`, not a save** — and `nextFollowUpAt` always, `status` only when the call moved it

| Field | Type | Required | Notes |
|---|---|---|---|
| `note` | String | **yes** | What happened. **The only required field.** |
| `communicationChannel` | String | no | `"PHONE"`, `"WHATSAPP"`, `"VISIT"`. Free text — a school names its own channels. |
| `status` | InquiryStatus | no | When the call moved the lead. Walks the [transition table](#inquirystatus--12), with three destinations refused on top of it. |
| `nextFollowUpAt` | Instant | no | **Also written to `Inquiry.nextFollowUpAt`**, which is what the worklist index sorts on. **Absent CLEARS it.** |
| `counselorDocsId` | String | no | Who logged it. This school's staff. |
| `version` | Long | no | Checked before the push **and guarded in the query**. |

### This is the endpoint the lead half was waiting for

[#13](#e13) sorts a worklist by `nextFollowUpAt` and [#14](#e14) renders a timeline — and until this
existed, **every lead in the database had an empty timeline and no chase date.** Both reads were
correct and had nothing to show. Worth knowing before the same build order is chosen again.

### A `$push`, never a re-save

Reading the lead, adding to its list and saving the whole document back would overwrite every entry
anybody else logged in between, and a timeline is exactly the kind of list two counsellors write to
at once. The same call [`timetable` #3](../academics/timetable/README.md#e3) makes, for the same
reason.

**The version guards the query as well as being checked before it.** The check gives the caller a
sentence; the guard wins the race. No sequential test can tell the two apart — recorded here
because a mutation that removes the guard survives the suite, and that is a fact about what a test
harness can reach rather than about the code.

### The chase date is rewritten every time, including to nothing

**The one place #10 departs from "only what was sent".** The field means *the next call is due at*:
once this call has been made and no new date promised, there is no next call due. Leaving the old
one would keep showing a family as **overdue on the day somebody rang them**.

**The entry keeps what was promised**, so the history is not lost — the entry records what was
*said*, the parent carries what is *due*.

### Moving the status is optional, and most calls move nothing

A counsellor rings, nobody answers, the lead is still `CONTACTED`. **The entry stores exactly what
was sent**, so a null on the timeline reads "left as it was" rather than repeating a status the lead
already had.

**Sending the status the lead already has is accepted and moves nothing.** A second call about a
lead that is still `CONTACTED` is not an illegal transition.

### Three destinations are refused on top of the table

| Status | Why | Code |
|---|---|---|
| `LOST` | Needs a reason, and a follow-up has nowhere to put one. [#12](#t12) owns it. | `409 LOST_NEEDS_A_REASON` |
| `APPLICATION_STARTED` | A fact about an application. [#17](#e17) sets it. | `409 INQUIRY_STATUS_NOT_BY_HAND` |
| `APPLICATION_SUBMITTED` | The same. [#19](#e19) sets it. | `409 INQUIRY_STATUS_NOT_BY_HAND` |

**All three are ON the table** — they are legal *moves* owned by another endpoint, not impossible
ones. That is why both checks run **before** the table: telling a caller "it cannot go there" would
be a lie, and the table has to keep describing the product rather than describing who may drive it.

### The model's annotations disagree with this, and they are the ones that are wrong

[`InquiryFollowUp`](../../models/crm/embedded/InquiryFollowUp.java) carries `@NotNull` on `status`
and `@NotBlank` on `communicationChannel` and `counselorDocsId`, and **nothing on `note`** — the
opposite of the table above in every field. There is no `ValidatingMongoEventListener` in this
project, so those annotations are documentation of intent; the enforcement is `@Valid` on the
request record, which follows the plan. `status` required would mean every logged call has to claim
it moved the lead, when most calls move nothing.

### `recordedAt` is the server's

A caller who could name the time a call happened could log one into next week, and a timeline sorted
on a caller-supplied instant is not a record of anything.

### `counselorDocsId` is optional, and it should not be

The point of a timeline is who said what, and #14 exists to name them. It is optional because
**nothing in this project knows who is asking yet** — so the only way to fill it is for the caller
to say, and a caller who genuinely does not know should still be able to record that the call
happened. #14 renders the gap as "not recorded" rather than hiding the entry. **When authorization
arrives this becomes the caller and stops being a field.**

### A finished lead can be logged against but not moved

Somebody ringing back a family that gave up is exactly the call worth recording. The lead cannot go
anywhere — `LOST` and `CLOSED` lead nowhere on the table — but the note is still true.

| Refusal | When |
|---|---|
| `404 INQUIRY_NOT_FOUND` | No lead of that id **in this school**. |
| `404 STAFF_NOT_FOUND` | A counsellor who is not this school's staff, including a real id from another school. |
| `409 INQUIRY_STATUS_NOT_BY_HAND` | `APPLICATION_STARTED` or `APPLICATION_SUBMITTED`. |
| `409 LOST_NEEDS_A_REASON` | `LOST`. |
| `409 INQUIRY_TRANSITION_NOT_ALLOWED` | A move the table does not have. The message lists what it can go to, or `nothing`. |
| `409 CONCURRENT_MODIFICATION` | The `version` sent is not the one stored, or somebody won the race. |
| `400 VALIDATION_FAILED` | No note, a blank one, or a field over its length. |
| `409 SCHOOL_NOT_EDITABLE` · `409 SUBSCRIPTION_NOT_USABLE` | Gates 1 and 2. A write. |

<a id="e12"></a>
**[#12](#t12) · `POST /inquiries/{id}/status`** — built — *move it, and say why when it is a loss*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**
- [`staff`](../../models/people/staff/Staff.java) — *reads*: by `_id` + `schoolId` — only when the caller says who moved it
- [`inquiries`](../../models/crm/Inquiry.java) — *updates*: `status`, `lostReason`, `followUps` — **a `$push`, not a save** — and **unsets** `nextFollowUpAt`

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | InquiryStatus | **yes** | Where it is going. Walks the [table](#inquirystatus--12). |
| `lostReason` | String | **on `LOST` only** | And **refused** on anything else. |
| `note` | String | no | For the timeline. Falls back to the reason on a loss. |
| `counselorDocsId` | String | no | Who moved it. This school's staff. |
| `version` | Long | no | Checked before the move **and guarded in the query**. |

### [#10](#e10) can move a lead too, and the split is the point

**#10 logs a call that *happened to* move it.** A counsellor rings, books a visit, and the note goes
on the timeline beside the move.

**#12 is the move on its own** — a school writing a family off in January because nobody has
answered since October. There was no call, and pretending there was one to record the outcome would
put a fiction in the timeline.

### It is the only thing that may set `LOST`

Because it is the only one with somewhere to put the reason. **A lead marked lost with no reason is
a record that answers nothing** — *why* is the only question anybody asks of one six months later,
and `LOST` on its own is the one thing that cannot answer it. #10 refuses that status with
`409 LOST_NEEDS_A_REASON` and names this endpoint.

**The reason is refused on any other move** rather than quietly dropped: a reason attached to a move
that is not a loss is a caller who has misunderstood something, and silence would let them go on
believing it. The message says to send the words as a `note` instead.

### Every move lands on the timeline

With or without a note. **A history that showed every phone call but not the moment a family was
written off would be misleading about the one thing that matters most.**

The entry's note **falls back to the reason** on a loss — almost always the sentence somebody would
have typed — and to **nothing** otherwise, rather than to an invented sentence like *"Moved to
CLOSED"*, which would be the row repeating its own status column back at itself.

### And every move ends the chasing

`nextFollowUpAt` is **cleared**. Nobody owes a call to a family that has gone elsewhere, and a lead
whose file has been closed is not waiting for one — leaving the date would keep it on [#13](#e13)'s
overdue worklist for ever. The entry that *promised* the date still carries it.

### Moving it to where it already is is refused here, and accepted by #10

#10's status is a detail of a call that did happen, so echoing the current one is harmless. **This
endpoint's whole job is the move**, and a request that moves nothing has asked for nothing.

**In the code that clause is currently dead**, and is kept anyway: no status is a member of its own
set in `LEAD_MOVES`, so the table already refuses every self-move. It is what would still refuse one
the day the table gains a loop. **Measured by a mutation**, not assumed.

### It still cannot set the two the application half owns

Checked **before** the table, because the table **permits** them — they are legal moves owned by
[#17](#e17) and [#19](#e19). Telling a caller "it cannot go there" would be a lie, and what they
need to know is who does set it. The case that proves the ordering is `VISITED → APPLICATION_SUBMITTED`,
which the table does not list at all: with the checks the other way round it would blame the table
instead of naming #19.

| Refusal | When |
|---|---|
| `404 INQUIRY_NOT_FOUND` | No lead of that id **in this school**. |
| `404 STAFF_NOT_FOUND` | A counsellor who is not this school's staff, including a real id from another school. |
| `409 INQUIRY_STATUS_NOT_BY_HAND` | `APPLICATION_STARTED` or `APPLICATION_SUBMITTED`. |
| `400 LOST_REASON_REQUIRED` | `LOST` with no reason, or a blank one. |
| `400 LOST_REASON_NOT_ALLOWED` | A reason on a move that is not a loss. |
| `409 INQUIRY_TRANSITION_NOT_ALLOWED` | A move the table does not have, **including one that moves nothing**. The message lists what it can go to, or `nothing`. |
| `409 CONCURRENT_MODIFICATION` | The `version` sent is not the one stored, or somebody won the race. |
| `400 VALIDATION_FAILED` | No status, or a field over its length. |
| `409 SCHOOL_NOT_EDITABLE` · `409 SUBSCRIPTION_NOT_USABLE` | Gates 1 and 2. A write. |

**A finished lead can still be logged against by [#10](#e10)** — somebody ringing back a family that
gave up is exactly the call worth recording. It just cannot be moved anywhere.

<a id="e13"></a>
**[#13](#t13) · `GET /inquiries`** — built — *the counsellor's worklist*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: `status`, `academicYear`, `nextFollowUpAt` (overdue), `prospectiveStudentName` + `inquiryNo` (searched). **`schoolId` is added to the query and never taken from the request**

**It read `staff` as well until 2026-09-24**, to name each row's counsellor in one query for the
page. `assignedCounselorDocsId` and [#11](#t11) were removed together, so a row has nobody to name
and the endpoint is **one query rather than two**.

| Parameter | Type | Notes |
|---|---|---|
| `status` | enum | One state. The first key after the school in `school_inquiry_pipeline_idx`. |
| — | **It filtered by counsellor until 2026-09-24.** That field and [#11](#t11) were removed together, and an unknown query parameter is ignored rather than refused — so sending it now narrows nothing. |
| `academicYear` | String | One intake's leads. A school runs more than one at a time. |
| `overdue` | Boolean | **Past its follow-up date AND not finished.** Not simply "has a past date". |
| `search` | String | The child's name **or** the inquiry number, anywhere, ignoring case. |

**`overdue` is two conditions, not one**, exactly as [#28](#e28)'s is and [#32](#e32)'s `expired`
is. A lead somebody gave up on last month has a past date too, and nobody owes it a phone call. So
it is *past its date and not `LOST` or `CLOSED`*, and `overdue=false` is the mirror — it **includes
the given-up ones and the ones with no date at all**.

**`APPLICATION_SUBMITTED` is deliberately not treated as finished.** The family sent a form, which
is the best outcome there is — but the lead stays live until somebody closes it, and a counsellor
who promised to ring them back still owes that call.

**A lead with no `nextFollowUpAt` is never overdue**, because `$lt` does not match a missing field.
Today that is *every* lead: [#10](#e10) sets the date and is not built, so the branch is reached by
nothing this API can produce. **Measured while testing this**, the same way [#32](#e32)'s
missing-expiry branch was.

**The flag is on every row, not only a filtered one.** A caller listing everything still wants to
see which rows are late, and asking them to compare a timestamp themselves is how two screens end
up disagreeing about what "overdue" means. It is computed once in the service and used by
[#14](#e14) as well.

**A row is thinner than the lead**: no `notes`, no `sourceDetails`, no timeline. All three are
paragraphs a counsellor wrote about one family, and a page of twenty would carry every word of them
to draw a list that shows none. **But the phone number is on it** — the primary guardian's, or the
first one with a number. The point of a worklist is to pick the phone up, and a row that showed
nothing because the *first* guardian happened to have no phone would be a row nobody can use.

**Soonest to chase first, then by `id`.** The fallback must be total and `inquiryNo` is only unique
per school, so the document id is the only total order available. A lead with **no** follow-up date
sorts to the **front**, because Mongo puts a missing field before every value — arguably the wrong
end of a chase list, and also the honest one: a lead nobody has promised to ring is the one most
likely to be forgotten.

**`notes`, `lostReason`, `sourceDetails` and `guardians` are off the sort allowlist.** Three are
free text about a family and the fourth would sort by its first element, which means nothing.

**There is no "me".** Nothing in this project knows who is asking yet, so whose worklist it is has
to be named in the query. That is the [missing authorization](#what-this-module-is-not), not a gap
in this endpoint.

**No gates.** A read — a suspended school still owes these families a call back.

<a id="e14"></a>
**[#14](#t14) · `GET /inquiries/{id}`** — built — *one lead with its whole timeline*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: the lead by `_id` **and `schoolId`**; then everything on it, including `followUps[]`
- [`staff`](../../models/people/staff/Staff.java) — *reads*: `fullName` — the assigned counsellor **and everybody who logged a follow-up, in one query**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `name` — the interested class, scoped by the **lead's** year

**Everything a [#13](#e13) row leaves off**, plus the follow-ups in the order they happened with
whoever logged each one named.

**The timeline reads oldest first**, because a conversation reads forwards: the question being
asked of it is *"what have we already told this family"*. It is **sorted here rather than trusted**
— [#10](#e10) pushes in order, but a `$push` is not a promise about order once anything else
touches the array. An entry with no `recordedAt` sorts **last** rather than throwing; nothing
writes one, and a null comparator that blew up would take the whole lead with it.

**One staff query for the whole lead.** The counsellor it is assigned to and everybody who logged a
follow-up are asked about together — a timeline of ten calls by three people is one read, not ten.
The same shape [#32](#e32) uses for a page.

**Names are resolved tolerantly.** A class that was removed, or a counsellor who has left, leaves
the name off and the lead readable — somebody who has left the school still made the call they
made, and dropping the entry or inventing a name would hide that. The staff lookup is
**school-scoped**, so another school's real staff id is not named either.

**An id from another school is a `404`, not a `403`.** It is a real id, and saying which would
confirm that another tenant's lead exists. The read is scoped by school **in the query**, never
checked after.

| Refusal | When |
|---|---|
| `404 INQUIRY_NOT_FOUND` | No lead of that id **in this school** — including one that is real and somebody else's. |
| `400 TENANT_NOT_RESOLVED` | No `idtoken` cookie. |

**No gates.** A read.

<a id="e15"></a>
**[#15](#t15) · `GET /inquiries/search?phone=&email=`** — built — *is this family already known*

- [`inquiries`](../../models/crm/Inquiry.java) — *reads*: `guardians[].phoneNumber`, `guardians[].emailAddress` — **the only query in the module that reaches into an embedded array to match**. `schoolId` is added to the query and never taken from the request

### This is the endpoint [#8](#e8) has been pointing at

#8 does **not** refuse duplicates, and its own documentation says why: refusing would mean guessing
that two children sharing a phone number are one enquiry, *which a family with two children is
not*. **The judgement belongs to the person at the desk**, and this shows them the answer so they
can make it. Until it existed, that was a promise about an endpoint that was not there.

### One of the two is required; both together is an OR

A family that left a **phone number last year** and an **email this year** is the same family, and
requiring both would miss exactly the case this exists for. Neither is `400 NOTHING_TO_SEARCH_FOR`
— that would be every lead in the school, which is [#13](#e13)'s job.

**It matches across guardians on one lead**: a mother's phone and a father's email is still that
family. That is why the query does **not** use `elemMatch`, which asks whether *one* guardian
satisfies everything. A dotted path matches across the array, which is the behaviour wanted.

### The phone is matched on its DIGITS

#8 stores whatever the desk typed, and the desk types it differently every time.

| Query | Finds a stored `9876543210` |
|---|---|
| `9876543210` · `+91 98765 43210` · `098765-43210` · `(98765) 43210` | yes |
| `543210` | **no** |

**Ten digits or more is compared on the last ten**, so a country code or a trunk `0` on either side
stops mattering. **Fewer than ten must match the whole number** — by its tail, `543210` matches
every number ending in those six digits, and a false *"we already know them"* is the worst answer
this endpoint can give: the desk merges two families, or skips a lead that was never there.

**All three rules were wrong on the first build and found by the suite.** The first version
anchored at the end only, which missed both prefixes *and* matched every suffix.

### The email is matched whole and case-insensitively

`Priya@Example.com` and `priya@example.com` are the same mailbox. **Whole, not partial** — unlike
[#13](#e13)'s `search`, which matches anywhere. This is a question about identity: `a@b.com` must
not match `maria@b.com` merely because the letters appear in it. Both needles are **quoted**, so
`.*@.*` matches nothing rather than everything.

### A list, not a page

The answer is one lead, or two for a second child, or none. **Twenty means somebody has been typing
the school's own number into the guardian field** — worth *seeing* rather than paging through.
Capped at 25, newest first. The rows are [#13](#e13)'s thin ones; [#14](#e14) is one click away.

### A note on the two indexes

`Inquiry` declares `school_inquiry_guardian_phone_idx` and `school_inquiry_guardian_email_idx`, and
**this endpoint is the only thing that would ever use them**. Neither is built in the dev database —
measured, only `_id_` exists — so what keeps this read small is the `schoolId` filter. A match that
ignores separators could not use them anyway.

| Refusal | When |
|---|---|
| `400 NOTHING_TO_SEARCH_FOR` | Neither a phone nor an email — **including a phone with no digits in it**. |
| `400 VALIDATION_FAILED` | A field over its length. |

**No gates, and this one least of all.** A suspended school is still answering the phone, and a desk
that cannot check for duplicates makes them.

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

<a id="e18"></a>
**[#18](#t18) · `PATCH /applications/{id}`** — built — *correcting a form nobody has sent*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` and `version`
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) · [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: **only when the class is changing** — the round decides the year and holds the seat table
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: whichever of `appliedClassDocsId`, `applicantName`, `dateOfBirth`, `gender`, `guardians`, `formAnswers` were sent

| Field | Type | Required | Notes |
|---|---|---|---|
| `applicantName` | String | no | `""` is `400 BLANK_APPLICANT_NAME`. Absent keeps what is there. |
| `dateOfBirth` · `gender` | LocalDate / enum | no | `@Past` on the date. |
| `appliedClassDocsId` | String | no | Re-checked exactly as [#17](#e17) checks it. |
| `guardians` | List | no | **Replaced whole**, `@Size(min = 1, max = 10)`. |
| `formAnswers` | Map | no | **Replaced whole**, `{}` clears. Max 200. |
| `version` | Long | no | Sent → `409 CONCURRENT_MODIFICATION`. |

**Families fill a form over several sittings.** Before this, [#17](#e17) created one and nothing
could change it — **a typo in a child's name meant starting again**.

**`DRAFT` and nothing else, which is the line this module is built around.** After [#19](#e19) the
applicant and guardian fields stop being a draft the family is filling in and become a record of
what they actually declared; a school that could rewrite them afterwards could not answer *"what did
they tell us"*.

**Lists and maps are REPLACED, not merged.** A guardian has no id to merge *by* — they are embedded,
not documents — and merging answers would leave no way to remove one typed by mistake.

**`@Size(min = 1)` on the guardians, NOT `@NotEmpty`** — and the difference is the whole of what a
`PATCH` means. `@NotEmpty` rejects *null*, so on an endpoint where every field is optional it would
make guardians **required on every call**: correcting a name would fail unless the caller re-sent
them. `@Size` ignores an absent field and still refuses an empty list. **The suite found that on its
first run**, when correcting a name answered *"guardians: must not be empty"*.

**Three things it will not change**, and sending them does nothing:

| | Why |
|---|---|
| the **cycle** | A form belongs to the round it was created against — that round decides the year, the seat table and the window [#19](#e19) checks. Moving it is a different application. |
| the **inquiry** | [#17](#e17) takes one and moves that lead to `APPLICATION_STARTED`. Re-pointing it would leave the old lead claiming a form it no longer has — and this endpoint has no business writing a second collection. |
| the **status** | `DRAFT → SUBMITTED` is [#19](#e19), which freezes the snapshot as it goes. An edit that could set it would be a way to submit without freezing anything. |

**The class check moved to `utils` when this was built**, because [#17](#e17) and [#18](#e18) ask
the same two questions — is it a class of the *cycle's* year, and does the round have seats for it.
The offer side asks the same pair and keeps its own copy, which is the folder rule rather than an
oversight: a main service uses its own `utils`, and the two refusals differ because one is about a
class a family **applied** for and the other about one a school **offered**.

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
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: `reviewRound` and `status`, **only when the move is to `APPROVED`** — the open ones, by `schoolId` and `admissionApplicationDocsId` with the status in the query
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

**But it will not APPROVE a form somebody is still assessing — 2026-09-23.** If any review of the
form is `PENDING` or `IN_PROGRESS`, `APPROVED` is `409 REVIEWS_STILL_OUTSTANDING` and the message
names the rounds: *"round 1 (PENDING), round 2 (IN_PROGRESS)"*. A count would send somebody hunting;
the rounds tell them what to chase.

**The two rules do not fight, because having no reviews and having all of them settled are the same
answer.** The check reads the open ones; an empty list is an empty list whether nobody was ever
asked or everybody has finished.

| Review status | Holds an approval up | Why |
|---|---|---|
| `PENDING` | **yes** | Somebody was asked and has not looked |
| `IN_PROGRESS` | **yes** | Somebody is looking now |
| `COMPLETED` | no | They said what they found |
| `CANCELLED` | no | **The school called it off** — a settled answer, and waiting for it would mean waiting for something that is never going to happen |

**Only `APPROVED`, and the asymmetry is the rule rather than an oversight.** Refusing, waitlisting
and asking for more are all answers a head can give over an incomplete picture — and
`ADDITIONAL_INFORMATION_REQUIRED` is often exactly *why* a review is still open, so blocking it
would deadlock the form. Admitting a child is the one decision that claims every assessment was
seen. **Approving from `WAITLISTED` is still an approval** and still checked.

**It reads the reviews, not their recommendations.** Three reviewers recommending `REJECT` do not
stop an approval, and one recommending `APPROVE` does not cause one. The rule is that every
assessment was *seen*, not that they all agreed — the school decides, which is the whole point of
this endpoint.

**Where it sits among the refusals**, all measured: `CONCURRENT_MODIFICATION`, then
`INVALID_APPLICATION_TRANSITION`, then `DECISION_NOTE_REQUIRED`, then this. The request's own shape
and the form's own graph are cheaper questions and answer first; a query against another collection
is the last thing worth doing.

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

<a id="e21"></a>
**[#21](#t21) · `POST /applications/{id}/withdraw`** — built — *the family pulls out*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` and `version`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `WITHDRAWN`, `withdrawnAt`, `withdrawalReason`
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) · [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: the year and class name, for the answer. Both **tolerantly**

| Field | Type | Required | Notes |
|---|---|---|---|
| `withdrawalReason` | String | **yes** | `@NotBlank`, max 2000. The **family's** reason. |
| `version` | Long | no | Sent → `409 CONCURRENT_MODIFICATION` if somebody moved it. |

**The family's act, not the school's.** [#20](#e20) is where a school records what *it* decided;
this is where it records that the family stopped. They reach the same kind of ending from opposite
directions, and conflating them would lose which one happened — a school that refused a child and a
family that went elsewhere are very different numbers at the end of a season.

**Which is why it writes `withdrawalReason` and not `decisionNote`**, and stamps `withdrawnAt` and
not `decidedAt`. Two facts, four fields, and this endpoint touches neither of the other two — a
withdrawal stays out of every "how long did we take to decide" count.

| From | |
|---|---|
| `DRAFT` | **withdraws** — a form they started and gave up on, and the one case needing no other endpoint to have run first |
| `SUBMITTED` · `UNDER_REVIEW` · `ADDITIONAL_INFORMATION_REQUIRED` | **withdraws** |
| `APPROVED` · `WAITLISTED` · `REJECTED` · `OFFERED` · `OFFER_ACCEPTED` | **withdraws** |
| `ENROLLED` | `409 INVALID_APPLICATION_TRANSITION` — the child is a student, and leaving is the `student` module's business |
| `WITHDRAWN` | `409` — it already happened, and a second would overwrite what they said |

**`ENROLLED` is unreachable through the API** — [#33](#e33) sets it and needs the `student` module —
so the suite plants one to exercise that guard. Without the plant it is a branch nothing tests.

**It touches nothing but the application, which is the plan's collection list — and that has a
consequence.** A form withdrawn while it holds a live offer leaves that offer `ISSUED`, so
[#32](#e32)'s chase list will still show it and somebody will ring a family that has already gone.
An open review is left `PENDING` for the same reason. The endpoint that ends an offer is
[#30](#e30) with `DECLINED`, or [#31](#e31); the one that cancels a review is [#27d](#e27d). **Two
calls, because each records a different fact** — folding them in here would make #21 guess which of
them happened.

<a id="e22"></a>
**[#22](#t22) · `POST /applications/{id}/assign`** — built — *whose form this is*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status` and `version`
- [`staff`](../../models/people/staff/Staff.java) — *reads*: the officer by `_id` **and `schoolId`**; then `fullName`, which goes on the answer
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `assignedAdmissionOfficerDocsId`. **One field, and nothing else**
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) · [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: the year and the class name, for the answer. Both **tolerantly**

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "assignedAdmissionOfficerDocsId":
      "6aa9...ce22",   // REQUIRED. There is
                       // no unassign.

  "version": 2         // optional
}

THE ONLY FIELD. Assigning an owner is not a
decision, so there is nothing else to send.
</pre></td>
<td><pre>
200 OK

{
  "admissionApplicationId": "6ab2...e50",
  "applicationNo": "APP/2026/09/000123",
  "applicantName": "Aarav Sharma",
  "appliedClassName": "Grade 7",
  "status": "SUBMITTED",      // UNCHANGED
  "assignedAdmissionOfficerDocsId":
      "6aa9...ce22",
  "assignedAdmissionOfficerName": "Anita",
  "version": 3,
  "nextStep": "..."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `assignedAdmissionOfficerDocsId` | **yes** | A `staff` id **in this school** → `404 STAFF_NOT_FOUND` otherwise. Blank or absent is `400 VALIDATION_FAILED`: there is no unassign. |
| `version` | no | The version last read. Sent → a form somebody else reassigned answers `409 CONCURRENT_MODIFICATION`. Absent → last write wins. |

**An admission officer OWNS the form; a reviewer ASSESSES it.** Different jobs. The officer chases
the missing birth certificate, answers the family's calls and makes sure the form does not sit for
three weeks — which is what [#24](#e24)'s `assignedAdmissionOfficerDocsId` filter is for, and why
that filter matched nothing for every id until this existed.

**It moves no status and stamps no date**, and that is the difference from [#26](#e26): putting a
form on a *reviewer's* desk moves it to `UNDER_REVIEW` because assessment has started, but giving it
to an officer says nothing about where the form has got to.

**Reassigning is the normal case, not an error**, and assigning the same person twice is a quiet
200. Contrast [#27b](#e27b), which refuses a second start: *"make sure this is on Anita's list"* is
worth being idempotent, *"pick up work nobody has"* is a claim two people cannot both make.

**There is no unassign.** The plan has none, and a form belonging to nobody is the state this
endpoint exists to get rid of. A school whose officer leaves gives the form to somebody else.

| Application status | Can be given to an officer |
|---|---|
| `DRAFT` · `SUBMITTED` · `UNDER_REVIEW` | **yes** |
| `ADDITIONAL_INFORMATION_REQUIRED` · `WAITLISTED` | **yes** |
| `APPROVED` · `OFFERED` · `OFFER_ACCEPTED` | **yes** — the offer and the enrollment are still to come |
| `REJECTED` · `WITHDRAWN` · `ENROLLED` | `409 APPLICATION_NOT_ASSIGNABLE` |

**The set is written as what is REFUSED rather than what is allowed**, because an officer owns a
form from the moment it exists until it stops being anybody's problem — the short list is the
exceptions.

**`DRAFT` is deliberately allowed**, where [#26](#e26) refuses it. There is nothing to assess on a
form the family has not sent, but keying a paper form in and handing it to somebody to chase what is
missing is a real day's work, and refusing it would be inventing a rule the plan does not have.

**The officer is READ, not checked for existence**, exactly as [#26](#e26) reads a reviewer — which
is why the name on the answer costs no second query.

**Order of the refusals**, measured: `CONCURRENT_MODIFICATION`, then `APPLICATION_NOT_ASSIGNABLE`,
then `STAFF_NOT_FOUND`. The form's own state is a question about what is already in hand; the staff
lookup is another collection.

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
| `assignedAdmissionOfficerDocsId` | String | Whose worklist. **[#22](#e22) is what fills it** — this filter matched nothing for every id until that was built. Exact id, and the page now names the officer too, resolved in **one query for the whole page**. |
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
  "reviews": [], "reviewCount": 0, // #26 fills it
  "offers":  [], "offerCount":  0, // #29 fills it, with at
                                   // most ONE row
  // Each nested review and each nested offer carries its
  // own VERSION — #27, #27c, #27d, #30 and #31 all take
  // one, and this is the only endpoint that hands it out.
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

**The class, the cycle and the PEOPLE are all named now.** `reviewerDocsId` and
`assignedAdmissionOfficerDocsId` came back as bare ids while [#26](#e26) and [#22](#e22) did not
exist, because a name-resolving branch could never run and could never be tested. Each was written
the day the endpoint that fills its field was — the reviewers when [#26](#e26) arrived, the officer
when [#22](#e22) did.

**And they share ONE query.** The officer's id is added to the list of reviewer ids the staff
lookup already asks about, rather than a second read of the same collection for one more id. A name
that comes back missing is left off rather than invented: somebody who has left the school was still
working on this form.

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

**Rounds run 1, 2, 3 with no gaps — changed 2026-09-23.** Round 3 needs a round 2 to exist on the
application already, **by anybody**: a second assessor joining round 1 does not open round 2,
because the rounds are the school's stages rather than one person's. Asking for a round with
nothing before it is `409 REVIEW_ROUND_OUT_OF_ORDER`, and the message names the round that is
missing.

**This entry used to say the opposite** — that a school numbering its rounds 1 and 3 was doing
something odd rather than something wrong, and that a rule there would be invented. It was wrong: a
gap is somebody typing the wrong number, and the round it leaves behind can never be filled in
afterwards, because the rule that would let them is the one that was missing. Proven by mutation,
including the near-miss where the check keys on the reviewer too and quietly turns the rounds into
one person's rather than the school's.

<a id="e27"></a>
**[#27](#t27) · `PATCH /reviews/{id}`** — built — *what the reviewer found*

- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: the review by `_id` **and `schoolId`**; then `status` and `version`
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *updates*: `score`, `recommendation`, `criterionScores`, `notes`, `status`, and `completedAt` **only on the way into `COMPLETED`**
- [`staff`](../../models/people/staff/Staff.java) — *reads*: `fullName`, for the answer. Tolerantly
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `applicationNo`, for the answer. Tolerantly too

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "status": "COMPLETED",     // optional
  "score": 86.50,            // optional
  "recommendation": "APPROVE",
  "criterionScores": {       // REPLACES the map
    "INTERVIEW": 42.50,
    "ENTRANCE_TEST": 44.00
  },
  "notes": "Performed well.",
  "version": 2               // optional
}

Every field is optional. A body carrying
none of them is 400 NOTHING_TO_UPDATE.
</pre></td>
<td><pre>
200 OK

{
  "admissionReviewId": "6ab3...f0e",
  "admissionApplicationDocsId": "6ab3...f0d",
  "applicationNo": "APP/2026/09/000123",
  "reviewRound": 1,
  "reviewerDocsId": "6aa9...ce22",
  "reviewerName": "Anita",
  "reviewerRole": "ADMISSION_OFFICER",
  "status": "COMPLETED",
  "dueAt": "2026-03-15T17:00:00Z",
  "completedAt": "2026-09-23T07:12:40Z",
  "score": 86.50,
  "recommendation": "APPROVE",
  "criterionScores": { ... },
  "notes": "Performed well.",
  "nextStep": "It is done and can no longer
               be changed..."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `status` | no | An [`AdmissionReviewStatus`](../../models/crm/enums/AdmissionReviewStatus.java). **Absent leaves it where it is**, which is what lets a reviewer save a score without declaring themselves done. |
| `score` | no | `@PositiveOrZero`, six digits and two decimals. **No upper bound** — the scale is the school's. |
| `recommendation` | no, except | **Required to reach `COMPLETED`** → `400 RECOMMENDATION_REQUIRED`. |
| `criterionScores` | no | `max 50` entries. **Sent replaces the whole map**; `{}` clears it; absent leaves it alone — three different requests, and an editor that offered only "rows" could not say the middle one. A value that is not a number is `400 MALFORMED_REQUEST` from the JSON reader, **before this endpoint runs at all**, so it beats even `REVIEW_ALREADY_COMPLETED` on a finished review. |
| `notes` | no, except | Max 2000. `""` clears. **Required to reach `CANCELLED`** → `400 CANCELLATION_NOTE_REQUIRED`. |
| `version` | no | A stale one is `409 CONCURRENT_MODIFICATION`. **[#25](#e25), [#26](#e26), #27 and [#28](#e28) all return it** as of 2026-09-23 — before that the field was accepted and the only way to learn its value was to read the document out of Mongo. The same gap [#20](#e20) had, closed the same way. |

**Only what you send moves.** A reviewer can save a score today and add the recommendation
tomorrow. A body that carries nothing is `400 NOTHING_TO_UPDATE` — and **that check runs first**,
before the version and before the status graph. It is the only check about the *request* rather
than about the world, and `{"version": 5}` on its own answered `CONCURRENT_MODIFICATION` until it
was moved: true, and no help at all to somebody who sent an empty body.

**Moving it to `COMPLETED` is the completion.** There is no separate "finish" verb — the status is
named directly, as [#3](#e3) and [#20](#e20) do — and that move is what stamps `completedAt`.
Nothing else does: **a cancelled review was not completed**, however much of it was filled in.

| From | Can be moved to |
|---|---|
| `PENDING` | `IN_PROGRESS` `COMPLETED` `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED` `CANCELLED` |
| `COMPLETED` · `CANCELLED` | **nothing — both ends are terminal** |

**`PENDING → COMPLETED` skips `IN_PROGRESS`, deliberately.** Most reviews are done in one sitting,
and an "I have started" call nobody would keep up with is ceremony rather than a record.

**A finished review is a record, not a draft** — `REVIEW_ALREADY_COMPLETED` and `REVIEW_CANCELLED`.
A score typed wrong is corrected by **cancelling this review and assigning another**, which leaves
both in the history rather than quietly overwriting one. That is also why there is no `DELETE`.

**`criterionScores` replaces rather than merges.** A map is one value, and merging would leave no
way to remove a criterion recorded by mistake. Proven by mutation: a merging version passes every
other assertion.

**A recommendation is not a decision.** It is what *one person* thinks, and it lives on their
review; [#20](#e20) is what the school does, and the school may decide something no reviewer
recommended. **#20 does not read this review and does not have to** — which is why the two are
separate enums even though four of the values read alike.

<a id="e27b"></a>
**[#27b](#t27b) · `POST /reviews/{id}/start`** — built — *the reviewer has picked it up*

- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: the review by `_id` **and `schoolId`**; then `status`
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *updates*: `status` = `IN_PROGRESS`, and **nothing else**
- [`staff`](../../models/people/staff/Staff.java) · [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the names for the answer, the same two lookups [#27](#e27) and [#28](#e28) make

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/reviews/{id}/start

NO BODY. The id in the path is the whole
request — there is nothing to say.
</pre></td>
<td><pre>
200 OK   — the WHOLE review

{
  "admissionReviewId": "6ab3...f0e",
  "admissionApplicationDocsId": "6ab3...f0d",
  "applicationNo": "APP/2026/09/000123",
  "reviewRound": 1,
  "reviewerDocsId": "6aa9...ce22",
  "reviewerName": "Anita",
  "reviewerRole": "ADMISSION_OFFICER",
  "status": "IN_PROGRESS",
  "dueAt": "2026-03-15T17:00:00Z",
  "createdAt": "...",
  "version": 1,
  "nextStep": "It is being worked on..."
}

No completedAt, no score, no recommendation —
starting is not finishing.
</pre></td>
</tr>
</table>

**#27 can make the same move, so why does this exist.** Because *starting* is an event rather than
a field being set, and this module's rule is that events get a verb — the same call [#19](#e19) and
[#3](#e3) make. It is what **opening a review fires on its own** in the API tester, and a `PATCH`
carrying a status would be a strange shape for that.

| From | |
|---|---|
| `PENDING` | **starts** |
| `IN_PROGRESS` | `409 INVALID_REVIEW_TRANSITION` |
| `COMPLETED` | `409 REVIEW_ALREADY_COMPLETED` |
| `CANCELLED` | `409 REVIEW_CANCELLED` |

**The same codes [#27](#e27) uses, not new ones.** A caller should not have to learn two
vocabularies for "this review is finished".

**Not idempotent, deliberately.** Starting something already `IN_PROGRESS` is a refusal rather than
a shrug: the caller believed they were picking up work nobody had, and a silent 200 would hide that
two people are on it. That is also why the screen only fires it automatically on a `PENDING` review
and keeps a button for everything else — otherwise every refusal here would be out of reach.

**A review is never `APPROVED` or `REJECTED`.** Those are the application's status ([#20](#e20)) and
the reviewer's *recommendation*, which lives on the review and is a different field. The four
guarded here are the four `AdmissionReviewStatus` has.

**It answers with the whole review** so the caller can show the new state without reading again —
which is what makes the tester's automatic start invisible rather than a flash of stale data.

<a id="e27c"></a>
**[#27c](#t27c) · `POST /reviews/{id}/complete`** — built — *the reviewer is finished*

- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: the review by `_id` **and `schoolId`**; then `status`, `version`, `recommendation`
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *updates*: `status` = `COMPLETED`, `completedAt`, and whichever of `recommendation`, `score`, `criterionScores`, `notes` were sent
- [`staff`](../../models/people/staff/Staff.java) · [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the names for the answer, the same two lookups [#27](#e27) and [#28](#e28) make

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "recommendation": "APPROVE",
  "score": 86.50,
  "criterionScores": {
    "INTERVIEW": 42.50,
    "ENTRANCE_TEST": 44.00
  },
  "notes": "Strong in the interaction.",
  "version": 2
}

EVERY FIELD IS OPTIONAL, including the
recommendation — see below. THERE IS NO
STATUS FIELD: the status is the endpoint.
</pre></td>
<td><pre>
200 OK   — the WHOLE review

{
  "admissionReviewId": "6ab3...f0e",
  "applicationNo": "APP/2026/09/000123",
  "reviewRound": 1,
  "reviewerName": "Anita",
  "reviewerRole": "ADMISSION_OFFICER",
  "status": "COMPLETED",
  "completedAt": "2026-09-23T10:31:04Z",
  "score": 86.50,
  "recommendation": "APPROVE",
  "criterionScores": { ... },
  "notes": "Strong in the interaction.",
  "version": 3,
  "nextStep": "It is done and can no longer..."
}
</pre></td>
</tr>
</table>

**The recommendation is the point.** It is the one thing a review exists to produce, so a
completion without one is `400 RECOMMENDATION_REQUIRED` — the same code [#27](#e27) uses.

**But it is the REVIEW'S recommendation, not the body's.** Somebody who saved one earlier with
[#27](#e27) does not send it twice: an empty body completes that review. The rule is *the review
says what it recommends by the time it is done*, which is what [#27](#e27) already enforces.

**No `NOTHING_TO_UPDATE`, and that is the difference between a verb and a `PATCH`.** An empty body
on [#27](#e27) asks for nothing; an empty body here asks for the move the path names.

| From | |
|---|---|
| `PENDING` | **completes**, skipping `IN_PROGRESS` — most reviews are done in one sitting |
| `IN_PROGRESS` | **completes** |
| `COMPLETED` | `409 REVIEW_ALREADY_COMPLETED` |
| `CANCELLED` | `409 REVIEW_CANCELLED` |

**It stamps `completedAt`**, which nothing else in this module does — not [#27b](#e27b), and not
[#27d](#e27d) however much of the review was filled in.

**The criterion map is REPLACED, not merged**, as on [#27](#e27): merging would leave no way to
remove a criterion recorded by mistake, so `{}` clears it and leaving the field out keeps it.

**And it does not touch the application.** A completed review is one person's opinion; the school's
decision is [#20](#e20), which reads none of these and does not have to. Three reviewers
recommending `APPROVE` do not approve anybody.

<a id="e27d"></a>
**[#27d](#t27d) · `POST /reviews/{id}/cancel`** — built — *the school called it off*

- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: the review by `_id` **and `schoolId`**; then `status`, `version`, `notes`
- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *updates*: `status` = `CANCELLED`, and `notes` if a reason was sent. **Nothing else, and no `completedAt`**
- [`staff`](../../models/people/staff/Staff.java) · [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the names for the answer

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "notes": "The reviewer has left the
            school, so this round is
            being reassigned.",
  "version": 1
}

THE REASON IS THE WHOLE BODY. It is written
to the review's own notes field, and it is
called notes here for that reason — one
field should not have two names.
</pre></td>
<td><pre>
200 OK   — the WHOLE review

{
  "admissionReviewId": "6ab3...f0e",
  "applicationNo": "APP/2026/09/000123",
  "reviewRound": 1,
  "reviewerName": "Anita",
  "status": "CANCELLED",
  "score": 61.50,
  "recommendation": "REJECT",
  "notes": "The reviewer has left...",
  "version": 2,
  "nextStep": "The school called it off..."
}

NO completedAt. And the score and the
recommendation somebody had already recorded
are STILL THERE — untouched.
</pre></td>
</tr>
</table>

**This is what a `DELETE` would have been, and it is not one.** There is no `DELETE` on this
controller: admissions keeps what it decided, including who it asked and that it changed its mind.
And a `PENDING` row nobody is ever going to work is worse than a cancelled one — it sits in
[#28](#e28)'s queue for ever and makes the backlog a lie.

**A reason is required**, `400 CANCELLATION_NOTE_REQUIRED` without one — the same reading that makes
`lostReason` required on a lost inquiry. Work abandoned with nothing said is a gap in the record.

**The review's own notes count**, as the recommendation does on [#27c](#e27c). A reason that *is*
sent replaces them; one that is not leaves what the reviewer wrote rather than blanking it.

| From | |
|---|---|
| `PENDING` | **cancels** — the commonest case, a row nobody will work |
| `IN_PROGRESS` | **cancels**, keeping whatever was recorded so far |
| `COMPLETED` | `409 REVIEW_ALREADY_COMPLETED` — the school undoes it in [#20](#e20), not here |
| `CANCELLED` | `409 REVIEW_CANCELLED` |

**It does not stamp `completedAt`.** A cancelled review was not completed, however much of it was
filled in — and that is also why the score and the recommendation already on it are left exactly
where they are. That is the history the cancellation is being written into.

**The reviewer is still named**, tolerantly as everywhere in this module — and **a reviewer who has
left the school is very often exactly why the review is being cancelled**, so refusing to name them
would refuse the commonest case this endpoint exists for.

<a id="e28"></a>
**[#28](#t28) · `GET /reviews`** — built — *the queue*

- [`admission_reviews`](../../models/crm/AdmissionReview.java) — *reads*: `reviewerDocsId`, `status`, `admissionApplicationDocsId`, `reviewRound`, `dueAt`. **`schoolId` is added to the query and never taken from the request**
- [`staff`](../../models/people/staff/Staff.java) — *reads*: `_id`, `fullName` — **one query for the whole page**
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `_id`, `applicationNo`, `applicantName` — **one query for the whole page**

### Request and response

<table>
<tr><th align="left">Query string</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/reviews
      ?reviewerDocsId=6aa9...ce22
      &status=PENDING
      &admissionApplicationDocsId=...
      &reviewRound=1
      &overdue=true
      &page=0&size=20
      &sort=dueAt

Every filter is optional. NO BODY.
</pre></td>
<td><pre>
200 OK   — rows are THIN

{
  "content": [
    { "admissionReviewId": "6ab3...f0e",
      "admissionApplicationDocsId": "6ab3...f0d",
      "applicationNo": "APP/2026/09/000123",
      "applicantName": "Aarav Sharma",
      "reviewRound": 1,
      "reviewerDocsId": "6aa9...ce22",
      "reviewerName": "Anita",
      "reviewerRole": "ADMISSION_OFFICER",
      "status": "PENDING",
      "dueAt": "2026-03-15T17:00:00Z",
      "createdAt": "..." }
      // no criterionScores, no notes
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
| `reviewerDocsId` | String | Whose queue. **Absent returns everybody's**, which is the office's view. **There is no "me"** — nothing in this project knows who is calling yet. |
| `status` | enum | One state. Absent returns every one, `CANCELLED` included. |
| `admissionApplicationDocsId` | String | One form's reviews, for walking down from an application. |
| `reviewRound` | Integer | One stage. |
| `overdue` | Boolean | **Two conditions, not one** — see below. |

The first two are the first two keys of `school_reviewer_status_due_idx`
(`{schoolId, reviewerDocsId, status, dueAt}`), which is what this endpoint is named for.

**`overdue` is past its date AND still owed.** A review somebody finished a month late also has a
past due date and is **not** outstanding — a one-condition filter would put it on the list of things
to do. `false` is the mirror: everything not late, the finished ones and the undated ones included.
**A review with no `dueAt` is never overdue**, because `$lt` does not match a missing field — that
falls out of the query rather than needing a rule.

**Soonest due first, and a review with no date sorts FIRST.** Mongo puts a missing field before
every value in an ascending sort, which is the wrong end of a queue. It is not worth an aggregation
to fix, and `overdue=true` is what actually answers "what is late".

**`id` is the tiebreaker, and it had to be something.** A review has no unique business key — its
uniqueness is the triple of application, round and reviewer — so the document id is the only total
order available. Proven by mutation, and only after the fixture was changed: six reviews of one form
that all share an *absent* due date. Two reviews with different dates are already ordered and prove
nothing.

**Rows are thin.** No `criterionScores` and no `notes` — fifty criteria and two thousand characters
each, twenty rows at a time, to draw a list that shows neither. Both are on the review.

**But the applicant is named**, in one query for the whole page rather than one per row. A queue of
raw ids is not a queue anybody can work from.

**`notes` and `criterionScores` are off the sort allowlist.** Mongo would order them by their first
element, which means nothing — and both carry what a reviewer wrote about a child. Allowed:
`dueAt`, `completedAt`, `reviewRound`, `status`, `score`, `createdAt`, `updatedAt`.

**No gates.** A read — a suspended school still sees what it owes.

**It has no screen in the API tester, as of 2026-09-23.** It had one for a few hours and the queue
duplicated what an application's own page already shows — a school works through reviews from the
form they are about. The endpoint is real and Postman drives it; the tester's catalogue enforces
"every endpoint has a screen", so it is out of that list rather than sitting in it unreachable.

<a id="e29"></a>
**[#29](#t29) · `POST /applications/{id}/offers`** — built — *the school offers a seat*

- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form by `_id` **and `schoolId`**; then `status`
- [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: `academicYear`, `capacities`, `enrollmentDeadlineAt`. **Throws** — see below
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: the offered class by `_id`, `schoolId` **and the cycle's year**; then `name`
- [`staff`](../../models/people/staff/Staff.java) — *reads*: the issuer by `_id` **and `schoolId`**, only when one is sent
- [`fee_invoices`](../../models/finance/billing/FeeInvoice.java) — *reads*: the deposit invoice by `_id` **and `schoolId`**, only when one is sent. **The first read this project makes of the finance module**
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: whether this form has an offer already — **any status counts**, because a `WITHDRAWN` or `DECLINED` one is still its offer
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: the `ADMISSION_OFFER` counter
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *insert*: `offerNo`, `revisionNo` = 1, `admissionApplicationDocsId`, `offeredClassDocsId`, `expiresAt`, `depositInvoiceDocsId`, `issuedByDocsId`, `status` = `ISSUED`, `offeredAt`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *updates*: `status` = `OFFERED`, **as a consequence rather than a request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `offeredClassDocsId` | String | **yes** | Usually the applied class; **not always** — a school offers a different grade after assessment. Must be a class of the **cycle's** year that the round has seats for. |
| `expiresAt` | Instant | no | Defaults to the cycle's `enrollmentDeadlineAt`. Already past is `400 OFFER_EXPIRY_IN_THE_PAST` **either way**. |
| `depositInvoiceDocsId` | String | no | **Checked** against `fee_invoices` in this school → `404 FEE_INVOICE_NOT_FOUND`. See below: every id is refused today. |
| `issuedByDocsId` | String | no | This school's staff → `404 STAFF_NOT_FOUND` otherwise. Optional because nothing knows who is calling yet. |

**There is no `status` and no `revisionNo` on the request.** Issuing is the endpoint, so the offer is
created `ISSUED` and `offeredAt` is stamped; the revision is `max + 1` worked out from what is
stored. A caller-supplied revision is a caller who can rewrite the history of what was offered,
which is the one thing keeping every revision is for.

**`DRAFT` is unreachable.** It is on the enum and no endpoint writes it — the same honest gap as
`EXPIRED`, which is what a date in the past *means* rather than a call anybody makes.

**ONE OFFER LETTER PER ADMISSION — decided 2026-09-23, replacing the plan's supersede model.** A
school issues one letter; if it expires the school extends it, and if anything else changes the
school edits it. There is no second document and no revision history.

**The plan asked for the opposite** — "a later one supersedes the last", every revision kept — and
that was built first. It was replaced because two documents for one seat is two things to keep in
step, and the question it answered, *"what did we originally offer"*, is one this module has never
been asked. **`SUPERSEDED` is now unreachable, like `DRAFT`.**

| Application status | Can be offered |
|---|---|
| `APPROVED` | **yes** — the obvious one |
| `WAITLISTED` | **yes** — a seat came free, and going through [#20](#e20) first would record a decision the school never made separately |
| `OFFERED` | no — it already has its one letter |
| `OFFER_ACCEPTED` | no — the family agreed to something specific |
| everything else | `409 APPLICATION_NOT_ELIGIBLE_FOR_OFFER` |

**Which refusal you get depends on how you got there, and both are real.** Issuing moves the form to
`OFFERED`, so a straightforward second attempt is answered by the *status* check —
`409 APPLICATION_NOT_ELIGIBLE_FOR_OFFER`. `409 OFFER_ALREADY_ISSUED` is the guard behind it, for a form that
still looks offerable but already has a letter. **Through the API that state cannot be reached** —
nothing moves a form back out of `OFFERED` — so the suite plants an offer against an `APPROVED` form
to exercise it. It is worth keeping for exactly the reason it is hard to reach: it is the check that
holds when something else changes.

**A finished offer does not free the slot.** `WITHDRAWN`, `DECLINED` and `EXPIRED` all still block a
second one — that offer is the application's offer, and its status is the record of what became of
it. None of the three is reachable while [#30](#t30) and [#31](#t31) do not exist, so all three are
planted in the suite.

**[#29b](#e29b) is what edits one**, and it was built for exactly this: the one-offer rule left a
lapsed letter in a state nothing could get out of — it could not be extended and #29 would not
replace it, so a family that missed the deadline could not be given a seat by any route. **That gap
was open for one endpoint's worth of time and is recorded rather than quietly closed**, because it
is the clearest example in this module of a rule creating a hole somewhere else.

**The cycle is read with `CrmHelper.loadCycle`, which THROWS**, unlike [#25](#e25)'s tolerant read.
An offer is a promise about a seat in a round; a round nobody can find has no seat table to check
and no deadline to promise against.

**It does NOT cap offers against the seat table, and that is a decision.** Schools deliberately
over-offer — sixty offers for forty places, because a fifth of families go elsewhere — so a refusal
at `totalSeats` would refuse the normal case. What it refuses is a class the round has **no** seats
for at all, which is [#17](#e17)'s rule and is nonsense rather than strategy. Counting offers
against places is [#7](#e7)'s job.

**`revisionNo` is pinned to 1**, which lines the rule up with the declared unique index
`school_application_offer_revision_uniq` on `(schoolId, admissionApplicationDocsId, revisionNo)`.
**That index is declared and NOT built** — measured 2026-09-23: this project keeps Mongo's
auto-index-creation off and syncs indexes on demand, so a development database has only `_id_` and a
duplicate inserted straight into Mongo is accepted. **Until the indexes are synced the service check
is the only thing enforcing this**, and a test that assumed otherwise is how that was found.

**`depositInvoiceDocsId` is checked, and today it refuses everything — 2026-09-23.** An id nothing
verifies is an id that can be anything, and `"13212313"` was accepted and stored until this existed.
Two things are true at once and both are worth writing down:

- **Nothing writes `fee_invoices`.** The finance module has models and no service, and the
  collection is empty — measured, not assumed. So there is no id this will accept, and the field is
  unusable until that module is built.
- **It would not fit even then, as things stand.** `FeeInvoice` extends
  [`AcademicStudentSchoolBase`](../../models/base/AcademicStudentSchoolBase.java), which requires a
  `studentDocsId` — and an applicant is not a student until [#33](#e33) enrolls them. An admission
  *deposit* for somebody who is not yet a student is a shape that collection does not have.

**The alternative was to drop the field from the request**, and it is still open: an endpoint that
cannot accept a field is arguably an endpoint that should not offer one. It was kept because the
model carries it, the check is already right for the day finance exists, and refusing says more than
silence does.

**The school scope on that lookup survived mutation until an invoice was planted elsewhere.** With
the collection empty in every school, `findById` and `findByIdAndSchoolId` answer the same "nothing"
for every id — the scope only becomes testable once another school has one.

<a id="e29b"></a>
**[#29b](#t29b) · `PATCH /offers/{id}`** — built — *correcting the one letter*

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: the offer by `_id` **and `schoolId`**; then `status`, `version`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) · [`admission_cycles`](../../models/crm/AdmissionCycle.java) — *reads*: **only when the class is changing** — the form names the round, and the round is what has a year and a seat table
- [`school_classes`](../../models/academics/structure/SchoolClass.java) · [`fee_invoices`](../../models/finance/billing/FeeInvoice.java) — *reads*: the same two checks [#29](#e29) makes, and for that reason they live in `utils`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: whichever of `expiresAt`, `offeredClassDocsId`, `depositInvoiceDocsId` were sent

| Field | Type | Required | Notes |
|---|---|---|---|
| `expiresAt` | Instant | no | The one it is for. Never into the past. **No way to clear it** — an `Instant` has no empty form, and "a seat held for ever" is not a correction anybody means to make. |
| `offeredClassDocsId` | String | no | Checked exactly as [#29](#e29) checks it. |
| `depositInvoiceDocsId` | String | no | Checked exactly as [#29](#e29) checks it — and refuses everything today. |
| `version` | Long | no | Sent → `409 CONCURRENT_MODIFICATION`. |

**It exists because the one-offer rule opened a hole.** A school issues one letter per admission, so
when that letter lapsed nothing could extend it and [#29](#e29) would not issue another — **a family
that missed the deadline could not be given a seat by any route.** A dead end in something already
shipped, which is why it was built before the rest of the plan.

**Extending a lapsed offer works, and that is the point.** Nothing writes `EXPIRED` — a date in the
past is what it *means* — so a lapsed offer is still stored as `ISSUED` and is still an offer this
can reach. **The design decision that looked like an omission is what makes the fix possible.**

**A `PATCH`, not a verb.** This module gives verbs to *events*; correcting a letter is fields being
set, which is what [#27](#e27) is for reviews. A body carrying nothing is `400 NOTHING_TO_UPDATE`,
asked **first** — before the version and before the status, the order [#27](#e27) settled on.

| Offer status | |
|---|---|
| `ISSUED` | **corrects** — including one that has already lapsed |
| `ACCEPTED` · `DECLINED` | `409 OFFER_NOT_OPEN` — changing the letter under a family rewrites what they agreed to |
| `WITHDRAWN` | `409 OFFER_NOT_OPEN` |

**It cannot answer for the family.** `status` and `response` are [#30](#e30)'s and [#31](#e31)'s and
are not fields here; an edit that could set them would be a second way to say yes on a family's
behalf.

**And it does not move `offeredAt`, the offer number or the revision.** A correction is not a
reissue — this is the same letter, and moving that date would lose how long the family has actually
had it.

**Bringing a deadline FORWARD is allowed**, into the past is not. A school shortening a window it
published is its own business; `400 OFFER_EXPIRY_IN_THE_PAST` is for a date already gone, because
that is not an extension.

<a id="e30"></a>
**[#30](#t30) · `POST /offers/{id}/respond`** — built — *the family answers*

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: the offer by `_id` **and `schoolId`**; then `status`, `expiresAt`, `version`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: `response`, `respondedAt`, `status`, and `acceptanceSignatureDocsId` when one is sent
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form, for the answer; *updates*: `status` = `OFFER_ACCEPTED` **on `ACCEPTED` only**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) · [`staff`](../../models/people/staff/Staff.java) — *reads*: the class and issuer names for the answer. Both tolerantly

| Field | Type | Required | Notes |
|---|---|---|---|
| `response` | AdmissionResponse | **yes** | `ACCEPTED` · `DECLINED`. |
| `acceptanceSignatureDocsId` | String | no | **Not validated**, unlike #29's deposit invoice — it points at `document_records`, which has no service either, and refusing every value on a field the acceptance carries would block the acceptance itself. The deposit is optional to the act of offering; this is part of it. |
| `version` | Long | no | Sent → `409 CONCURRENT_MODIFICATION` if somebody moved it. |

**This is why the offer half exists.** Approving is the school saying yes; this is the *family*
saying yes.

**It takes the ANSWER, not a status.** `AdmissionResponse` has two values and `AdmissionOfferStatus`
has seven — the endpoint maps one onto the other. Different from [#20](#e20), where the school
chooses among *its own* statuses; here the family chooses between yes and no, and the status that
follows is the school's bookkeeping.

**`DECLINED` moves nothing.** A declined offer is **not** a rejected applicant — the school decided
to admit this child and the family chose otherwise, and those are different facts. *The plan added
"and the school may issue another revision", which the one-offer rule has since removed: the form
stays `OFFERED` with a `DECLINED` letter, and nothing can offer that seat again until something can
edit an offer.*

**The offer must be `ISSUED` and not past `expiresAt`.** A lapsed one is `409 OFFER_EXPIRED` **even
though its stored status still reads `ISSUED`** — nothing writes `EXPIRED`, a date in the past is
what it means, and only the clock knows. [#32](#e32) is how a school finds them first.

<a id="e31"></a>
**[#31](#t31) · `POST /offers/{id}/withdraw`** — built — *the school takes it back*

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: the offer by `_id` **and `schoolId`**; then `status`, `version`
- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *updates*: `status` = `WITHDRAWN`, `withdrawalReason`
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: the form, **for the answer only**

**`withdrawalReason` is required**, `@NotBlank` on the request — unlike [#27d](#e27d)'s note, which
could fall back on what a reviewer had already written. There is nothing on an offer that could
stand in for why it was withdrawn.

**Only an offer still out can be taken back.** An `ACCEPTED` one is `409 OFFER_NOT_OPEN`, and that
is the interesting line: the family holds the seat, and taking it away is a decision about the
*application* ([#20](#e20)) rather than a tidy-up of the letter.

**It does not touch the application.** Withdrawing an offer does not un-approve a child.

**It does not stamp `respondedAt`.** The family did not answer — the school changed its mind — and
stamping it would make a withdrawal read as a decline in every list that shows that field.

<a id="e32"></a>
**[#32](#t32) · `GET /offers`** — built — *the chase list*

- [`admission_offers`](../../models/crm/AdmissionOffer.java) — *reads*: `status`, `expiresAt`, `admissionApplicationDocsId`, `offeredClassDocsId`. **`schoolId` is added to the query and never taken from the request**
- [`admission_applications`](../../models/crm/AdmissionApplication.java) — *reads*: `applicationNo`, `applicantName` — **one query for the whole page**
- [`school_classes`](../../models/academics/structure/SchoolClass.java) — *reads*: `name` — one query for the whole page, and **without a year**

| Parameter | Type | Notes |
|---|---|---|
| `status` | enum | One status. The first key after the school in `school_offer_status_expiry_idx`. |
| `expiringBefore` | Instant | Everything lapsing before then — the window. Says nothing about status, so it returns answered offers too. |
| `expired` | Boolean | **Past its date AND still `ISSUED`.** Not simply "has a past date". |
| `admissionApplicationDocsId` · `offeredClassDocsId` | String | One form's offer, one class's offers. |

**`expired` is two conditions, not one**, exactly as [#28](#e28)'s `overdue` is. An offer a family
accepted last month has a past date too, and nobody needs chasing about it. `expired=false` is the
mirror, and it **includes the answered ones** — "not lapsed" is not the same as "still out".

**An offer with no `expiresAt` is never expired**, because `$lt` does not match a missing field. In
practice there are none: a cycle's four dates are required and cannot be cleared, so every round has
an enrollment deadline and [#29](#e29) defaults to it. **Measured while testing this** — the branch
is correct and nothing this API can produce exercises it.

**The classes are read WITHOUT a year**, which is the one place in this module that happens. A page
can hold offers from several rounds and a round admits into its own year, so there is no single year
to scope by — the school is the scope, and `SchoolClassRepository.findBySchoolIdAndIdIn` was added
for it.

**Soonest to lapse first, then by `id`.** The fallback must be total and `offerNo` is only unique per
school, so the document id is the only total order available. An offer with no expiry sorts to the
**front**, because Mongo puts a missing field before every value — the wrong end of a chase list,
and `expired=true` is the filter that answers "what has lapsed" rather than the order.

**`withdrawalReason` is off the sort allowlist**, with the three document ids. It is something a
school wrote about one family, and paging a sorted field walks its values out.

**No gates.** A read — a suspended school still needs to know what it promised.

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

*Endpoints without an appendix entry — [#16](#t16) and
[#23](#t23) — take
what their tables and the status graphs above already say. An appendix row is written when the
endpoint is, so that it describes what was built rather than what was imagined. **Every one of the
twenty-three built endpoints now has a row**, which is the rule finally holding rather than a new one:
[#5](#e5) went in without one and got its row on 2026-09-22, when [#24](#e24) made the sort
allowlist worth writing down twice.*
