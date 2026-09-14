# controllers/people/development — API plan

**Nothing is built.** This is the detailed plan for the **development package** — training, what it
cost, and whether it was worth it. It expands the group that [`controllers/people`](../README.md)
lists.

> **Numbers are the domain's.** `#44` came from that file; **`#48`–`#51` are new**. The domain plan
> wrote this package as a single `POST · GET` row, which turned out to be wrong: the model carries a
> **six-state approval workflow**, and a plan that ignored it would have had somebody inventing
> endpoints while writing the service. Numbers are never reused; new ones go on the end.

Mirrors [`models/people/development`](../../../models/people/development) — one document, no
embedded types, no encryption, nothing blocked.

---

## What this package is

**One document, and it is not the log it looks like.**

```text
StaffDevelopmentRecord
  developmentType  TRAINING · WORKSHOP · CERTIFICATION · CONFERENCE
                   MENTORING · COACHING · HIGHER_EDUCATION
  title · provider
  plannedOn ──────────────▶ completedOn
  hours · cost · currencyCode
  status  PLANNED → PENDING_APPROVAL → APPROVED → IN_PROGRESS → COMPLETED
                                    └─────────▶ REJECTED
  approvedByDocsId
  impactEvaluation          <- written AFTER, and the reason the record exists
  certificateDocumentDocsId
  skillCodes[]
```

| Document | Collection | Phase |
|---|---|---|
| [`StaffDevelopmentRecord`](../../../models/people/development/StaffDevelopmentRecord.java) | `staff_development_records` | **5** |

### It has money and an approver, so it is a request before it is a record

`cost`, `currencyCode`, `approvedByDocsId` and a `PENDING_APPROVAL` state together say what this
document actually is: **a spend request that becomes a history entry once it is done.**

A plan that treated it as "record what training happened" would have no way to express *"Priya has
asked to attend a ₹40,000 conference and nobody has said yes yet"* — which is the state the record
spends most of its life in, and the one a budget holder cares about.

**That is why `#44` became five endpoints.** One create, one edit, one approval pair, one
completion, one read.

### `impactEvaluation` is the field that justifies the whole document

Every other field is bookkeeping. `impactEvaluation` is the school's own answer to *"was that worth
doing"* — written weeks after the training, by somebody who watched the person teach afterwards.

**It is the only field that can be set on a `COMPLETED` record**, and [#50](#e50) leaves the record
editable for exactly that reason. A completion endpoint that froze everything would make the field
unwritable in practice, because nobody evaluates impact the same afternoon.

---

## Which gates every endpoint runs

| | Gates |
|---|---|
| every **write** here | 1 school is live · 2 school is paying |
| every **read** here | none |

**No gate 4.** Training is not scoped to an academic year — the document carries no `academicYear`
at all, which is deliberate: a two-year part-time degree is one record.

---

# The endpoints

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t44"></a>44 | [`POST /staff/{id}/development`](#e44) | Record a plan, or something already done. |
| <a id="t48"></a>48 | [`PATCH /development/{id}`](#e48) | Fix the details, and write the impact evaluation. *(new)* |
| <a id="t49"></a>49 | [`POST /development/{id}/approve`](#e49) · [`/reject`](#e49) | The budget decision. *(new)* |
| <a id="t50"></a>50 | [`POST /development/{id}/complete`](#e50) | It happened. Records hours and the certificate. *(new)* |
| <a id="t51"></a>51 | [`GET /staff/{id}/development`](#e51) · [`GET /development`](#e51) | One person's, or the school's spend. *(new)* |

---

# Build order

| Phase | What it gives you | Endpoints |
|---|---|---|
| **5** | Training is planned, approved, completed and reportable | 44, 51, 49, 50, 48 |

**The whole package is phase 5, and it is the last thing in `people`.** Nothing references a
development record — not `payroll`, not `academics`, not the rest of `people`. It blocks nothing
and can wait until everything else is solid.

**`#51` second.** The read is what makes the three writes testable without guessing ids.

**`#48` last**, because the field it exists for — `impactEvaluation` — cannot be written until
something has reached `COMPLETED`.

---

# Things this package deliberately will not have

- **No `DELETE`.** A cancelled plan is a `REJECTED` record; a mistaken one is a data problem, not
  an endpoint. The spend history is what a budget review reads.
- **No budget tracking.** `cost` is recorded per record, and *"how much has Academics spent this
  year"* is [#51](#e51) with a filter and a sum. **There is no budget document**, no allocation and
  no limit — and inventing one here would put half a finance feature in the wrong module.
- **No skill taxonomy.** `skillCodes` is a free list of strings. Nothing validates them against a
  catalogue, because no catalogue exists — and a `skills` collection is a real thing that wants its
  own model before this references it. See [open item 2](#2-skillcodes-points-at-a-catalogue-that-does-not-exist).
- **No renewal reminders.** A certification that expires is a
  [`StaffCredential`](../staff/README.md#e20) with a `validUntil`, and
  [#23](../staff/README.md#e23) already reports those. **A development record that produced a
  certificate should create a credential** — see [open item 3](#3-a-certification-produces-a-credential-and-nothing-connects-them).
- **No attendance or completion verification.** `certificateDocumentDocsId` is evidence somebody
  uploaded; nothing checks it.

---

# To settle before building

## 1. Six states, and the model does not say which transitions are legal

```java
PLANNED  PENDING_APPROVAL  APPROVED  IN_PROGRESS  COMPLETED  REJECTED
```

**Nothing constrains the order**, and several paths are plausible:

```text
PLANNED ─▶ PENDING_APPROVAL ─▶ APPROVED ─▶ IN_PROGRESS ─▶ COMPLETED
                    └────────▶ REJECTED
```

But **must a record pass through approval at all?** Two real cases say no:

- **Free training.** A teacher attends a free webinar. `cost` is zero, nobody needs to approve it,
  and forcing it through `PENDING_APPROVAL` makes the workflow theatre.
- **Recording the past.** A school entering last year's training history has records that are
  `COMPLETED` and were never in this system when they were planned.

**Recommendation: approval is optional, and the endpoints say so.**

| | |
|---|---|
| [#44](#e44) accepts `PLANNED` **or** `COMPLETED` | the second is "this already happened" |
| [#49](#e49) moves `PENDING_APPROVAL` → `APPROVED`/`REJECTED` only | it is the budget gate, not a required step |
| [#50](#e50) accepts any non-`REJECTED`, non-`COMPLETED` state | free training goes `PLANNED` → `COMPLETED` directly |

**`IN_PROGRESS` is the odd one.** Nothing in this plan writes it — a two-day workshop is not worth
a state change, and a two-year degree is. **Recommendation: [#48](#e48) may set it, and no
dedicated endpoint exists** until somebody asks; inventing `/start` for a state nobody sets is
guessing.

**Settle before [#44](#e44)**, since it decides what that endpoint accepts.

## 2. `skillCodes` points at a catalogue that does not exist

`List<String> skillCodes` — free text, validated against nothing.

Two futures, and they are not compatible:

- **a) It stays free text.** A school types whatever it likes. `"Differentiation"`, `"differentiation"`
  and `"Diff. instruction"` are three skills, and the "who can teach X" query this field exists for
  never works.
- **b) A `Skill` catalogue exists**, and these are codes into it. The query works, and somebody has
  to maintain a taxonomy.

**Recommendation: (a) for now, and say so in the field's own documentation** — free text, not a
reference, and no endpoint filters on it. What must **not** happen is [#51](#e51) offering a
`?skillCode=` filter over unvalidated strings, because that looks like it works and quietly misses
two thirds of the matches.

## 3. A certification produces a credential, and nothing connects them

`developmentType` includes `CERTIFICATION`, and `certificateDocumentDocsId` holds the evidence. A
[`StaffCredential`](../staff/README.md#e20) is *also* a certificate, with an issuer, an expiry and a
verification status — and it is the one that appears in
[the compliance report](../staff/README.md#e23).

**So a teacher who completes a first-aid certification through [#50](#e50) is not in the compliance
report**, because nothing created a credential. The school finds out when the certificate has
lapsed and an inspection asks.

Three ways:

- **a) Leave them separate, and document it.** The school records the training here and the
  credential there. Two entries for one fact, and somebody will do one of them.
- **b) [#50](#e50) creates a credential when `developmentType = CERTIFICATION`** and a
  `validUntil` was supplied. Convenient, and it writes a document in a different package as a side
  effect — which is the kind of hidden write that surprises people later.
- **c) [#50](#e50) returns a `nextStep` pointing at [#20](../staff/README.md#e20)**, and the UI
  offers it. No hidden write, and the school is told.

**Recommendation: (c).** This project already uses `nextStep` to say what a caller should do
next, and a cross-package write triggered by a side door is exactly what makes a system hard to
reason about. **Decide before [#50](#e50).**

---

# Where the code will live

```text
controllers/people/development/
├── README.md                   <- this file
└── DevelopmentController.java  #44, #48–#51

services/people/
└── DevelopmentService.java     no helper — there is no rule MongoDB cannot express here

repositories/people/development/
└── StaffDevelopmentRecordRepository.java   + Custom/Impl for #51's filters

dto/people/development/{request,response}/
```

**No helper class**, and that is worth stating rather than leaving as an omission. Every other
package here has one because it has a rule the database cannot hold — band overlaps, criterion
weights, leave arithmetic. This package has a state machine and nothing else, and a state machine
belongs in the service beside the write it guards.

---

# Appendix — what each field can hold

## `staff_development_records` — [StaffDevelopmentRecord](../../../models/people/development/StaffDevelopmentRecord.java)

| Field | Type | What can be in it |
|---|---|---|
| `staffDocsId` | String, required | Who. Indexed with `status` and `completedOn`. |
| `developmentType` | Enum, required | `TRAINING` · `WORKSHOP` · `CERTIFICATION` · `CONFERENCE` · `MENTORING` · `COACHING` · `HIGHER_EDUCATION` |
| `title` · `provider` | String | What, and who ran it. |
| `plannedOn` | LocalDate | When it is meant to happen. |
| `completedOn` | LocalDate | **Set by [#50](#e50)**, and the field `school_staff_development_status_date_idx` sorts on. |
| `hours` | BigDecimal | Learning hours. What a professional-development requirement is counted in. |
| `cost` · `currencyCode` | BigDecimal · String | **Why there is an approval step at all.** |
| `status` | Enum, required | Six values — [open item 1](#1-six-states-and-the-model-does-not-say-which-transitions-are-legal). |
| `approvedByDocsId` | String | A `Staff`. Set by [#49](#e49). **No `approvedAt`** — a gap worth noting: the decision has a who and no when. |
| `impactEvaluation` | String | **Written after completion**, and the reason the record exists. |
| `certificateDocumentDocsId` | String | The evidence. See [open item 3](#3-a-certification-produces-a-credential-and-nothing-connects-them). |
| `skillCodes` | List | **Free text, not a reference** — [open item 2](#2-skillcodes-points-at-a-catalogue-that-does-not-exist). |

**Index:** `school_staff_development_status_date_idx {schoolId, staffDocsId, status, completedOn}`
— which is [#51](#e51)'s per-person read, and the reason its default order is newest completion
first.

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `DEVELOPMENT_RECORD_NOT_FOUND` | 404 | not this school's |
| `INVALID_DEVELOPMENT_TRANSITION` | 409 | **naming which states this one can reach** |
| `DEVELOPMENT_NOT_PENDING_APPROVAL` | 409 | [#49](#e49) — deciding one nobody submitted |
| `DEVELOPMENT_ALREADY_COMPLETED` | 409 | [#50](#e50) — completing it twice |
| `DEVELOPMENT_REJECTED` | 409 | [#50](#e50) — completing something that was refused |
| `INVALID_DEVELOPMENT_DATES` | 400 | `completedOn` before `plannedOn` |
| `CURRENCY_REQUIRED` | 400 | a `cost` with no `currencyCode` — a number with no unit |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

<a id="e44"></a>
**[44](#t44) · `POST /staff/{id}/development`**

- [`staff`](../../../models/people/staff/Staff.java) — *reads*: the person exists in this school
- *insert*: `staffDocsId`, `developmentType`, `title`, `provider`, `plannedOn`, `hours`, `cost`, `currencyCode`, `skillCodes`, `status`
- **It accepts `PLANNED` or `COMPLETED`, and nothing else.** Two real entry points: something a school is arranging, and something it is recording after the fact — a year of history entered during onboarding. Every other state is reached through [#49](#e49) or [#50](#e50). See [open item 1](#1-six-states-and-the-model-does-not-say-which-transitions-are-legal).
- **`PENDING_APPROVAL` is set by sending a `cost` and asking for it**, not by default. Free training that needs nobody's permission should not sit in a queue, and a workflow that makes it wait is a workflow people route around.
- **A `cost` with no `currencyCode` is `400 CURRENCY_REQUIRED`** — a number with no unit is not a cost, and the school's default currency is not this module's to assume.
- **`approvedByDocsId` and `impactEvaluation` are not accepted.** One is [#49](#e49)'s, the other is written after the fact by [#48](#e48).

<a id="e48"></a>
**[48](#t48) · `PATCH /development/{id}`** *(new)*

- *updates*: `title`, `provider`, `plannedOn`, `hours`, `cost`, `currencyCode`, `skillCodes`, `impactEvaluation`, `certificateDocumentDocsId`, and `status` **only to `IN_PROGRESS`**
- **This is where `impactEvaluation` is written**, which is the field the document exists for — weeks after completion, by somebody who watched the person teach afterwards. **So a `COMPLETED` record stays editable**, unlike most terminal states in this project: freezing it would make the field unwritable in practice.
- **`cost` is not editable once `APPROVED`.** Somebody approved a number; changing it afterwards spends money nobody agreed to. Raising it means a new record, or a rejection and a re-request.
- **Never `staffDocsId`** — a record belongs to the person it was created for, and moving it rewrites two people's histories at once.
- **Never `status`, except to `IN_PROGRESS`**, which is the one transition with no endpoint of its own. Approval is [#49](#e49) and completion is [#50](#e50), and a PATCH that could set either would bypass their checks.

<a id="e49"></a>
**[49](#t49) · `POST /development/{id}/approve` · `/reject`**

- *updates*: `status`, `approvedByDocsId`
- **The budget decision**, and the reason `cost` is on the document.
- **`409 DEVELOPMENT_NOT_PENDING_APPROVAL`** for anything else. **Not idempotent**, deliberately: "it was already approved" and "you just approved it" are different facts about somebody's money, and the same call [#37](../leave/README.md#e37) makes about a leave decision.
- **Two endpoints, not one with a flag** — the URL says which way it goes, so a half-read body cannot approve a spend somebody meant to refuse.
- **A rejection should carry a reason, and the model has no field for one.** `impactEvaluation` is not it. Worth adding before this is built — the same gap [#21](../staff/README.md#e21) has for a rejected credential.
- **`approvedByDocsId` has no `approvedAt` beside it.** The decision records who and not when, which is a model gap: a budget review a year later can see who approved a spend and not whether it was before or after the training happened.

<a id="e50"></a>
**[50](#t50) · `POST /development/{id}/complete`**

- *updates*: `status` = `COMPLETED`, `completedOn`, `hours`, `certificateDocumentDocsId`
- **Accepts any state except `REJECTED` and `COMPLETED`.** Free training goes `PLANNED` → `COMPLETED` with no approval in between, which is the common case and must not require a detour.
- **`409 DEVELOPMENT_REJECTED`** for something that was refused — completing it would record a spend nobody authorised.
- **`completedOn` defaults to today** and may be backdated, because a school entering last week's workshop should not have to lie about the date.
- **`hours` may be corrected here.** What was planned and what happened differ, and the planned figure is not worth preserving.
- **It does not create a credential**, even for a `CERTIFICATION`. It returns a `nextStep` pointing at [#20](../staff/README.md#e20) — see [open item 3](#3-a-certification-produces-a-credential-and-nothing-connects-them). A cross-package write as a side effect of completing a training record is exactly the kind of hidden behaviour that makes a system hard to reason about.

<a id="e51"></a>
**[51](#t51) · `GET /staff/{id}/development` · `GET /development`** *(new)*

- *reads*: one person's records, or the school's, filtered
- **Two paths, one handler.** The per-person read is the common one and is served directly by `school_staff_development_status_date_idx`; the school-wide read is the budget question.
- **Filters**: `?status=`, `?developmentType=`, `?from=`/`?to=` on `completedOn`, `?departmentDocsId=` on the school-wide read.
- **The school-wide read carries a `totalCost` for the filtered set**, which is the only reason somebody calls it. *"What has Academics spent on training this year"* is the question, and returning rows without the sum makes every client add them up differently.
- **`?departmentDocsId=` needs the same join [#7](../staff/README.md#e7) does** — a development record has no department, only a `staffDocsId`, so the filter reads current employment first. See [the domain's open item 3](../README.md#3-7s-filters-need-a-join-mongo-will-not-do).
- **No `?skillCode=` filter**, and that is deliberate — see [open item 2](#2-skillcodes-points-at-a-catalogue-that-does-not-exist). A filter over unvalidated free text looks like it works and quietly misses most matches.
- **The per-person read is not paged**; the school-wide one is.
- **No gates.** A suspended school still reads its own training history.
