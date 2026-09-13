# controllers/academics/grading — API plan

**Nothing is built.** This file is the full set of endpoints the grading feature needs, written
before any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../../core/README.md), [`controllers/plans`](../../plans/README.md) and
[`controllers/academics/structure`](../structure/README.md) were done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/academics/grading`](../../../models/academics/grading), whose README already
states the rules: *"Services must reject overlapping bands, gaps that are not intentional, invalid
boundaries, and changes to a scheme already used for published results."* **These endpoints
enforce that sentence.** Two things in it are not yet decidable and are open items below.

> **Two blockers were settled before writing this — 2026-09-13.**
>
> **1.** `GradingScheme` had a unique index on `schemeCode`, a field it never declared and which
> appeared **nowhere else in the project**. MongoDB indexes a missing field as null, so the index
> read `{schoolId, null, schemeVersion}` — one version string per school, making "CBSE Percentage"
> 2026.1 and "IB Points" 2026.1 mutually exclusive for no reason a caller could work out. It is
> now `school_grading_name_version_uniq`. See [open item 1](#1-schemecode-did-not-exist--settled-2026-09-13).
>
> **2.** `GradeBand.minimumValue` and `maximumValue` were `@NotNull`, which left
> `GradingScaleType.DESCRIPTOR` impossible to store: a descriptor scheme had to invent numbers for
> fields nothing reads. Both are nullable now, and which schemes may omit them is a service rule.
> See [open item 2](#2-descriptor-could-not-be-stored--settled-2026-09-13).

---

## What this module is

**The school's rulebook for turning a mark into a grade.** A teacher enters `94`. Whether that is
an `A1`, a `7`, or "Outstanding" is not something the number says — it is something the school
decides once and every exam, mark and report card then reads.

```text
GradingScheme  "CBSE Percentage Grading"  v2026.1  PERCENTAGE  max 100
  |
  └── gradeBands[]            ordered, non-overlapping, inside the max
        A1   91..100   point 10   "Outstanding"        passed
        A2   81..90    point  9   "Excellent"          passed
        B1   71..80    point  8   "Very Good"          passed
        ...
        E     0..32    point  0   "Needs Improvement"  FAILED
```

One document and one embedded type:

| Document | Collection | What it holds |
|---|---|---|
| [`GradingScheme`](../../../models/academics/grading/GradingScheme.java) | `grading_schemes` | one version of one rulebook — the scale, its ceiling, and its bands |
| [`GradeBand`](../../../models/academics/grading/embedded/GradeBand.java) | *embedded* | one band, identified by `gradeCode` within its scheme |

**A band is embedded because it has no life outside its scheme.** Nothing anywhere stores a
`gradeCode` as a reference — a report card snapshots the grade it printed, it does not point at
the band that produced it. Compare `ClassSection`, embedded for the same reason, and contrast
`AcademicTerm`, which is a document precisely because six things reference it.

### Why there is no academicYear here

Every other document in `models/academics` carries one. A scheme deliberately does not, and that
is the single most important thing about this module:

**A rulebook outlives a year.** A school does not re-enter its grade boundaries every April. The
same `CBSE Percentage Grading v2026.1` grades exams in 2026-2027 and 2027-2028, and a report card
issued in either year must reprint identically five years later. Scoping a scheme to a year would
force a copy per year and make "which A1 was this?" a question with as many answers as there are
years.

**`schemeVersion` is what changes instead of the year.** It moves when the *rules* move, not when
the calendar does.

## What this module is not

- **Not marks, exams or report cards.** Those are `models/academics/examination`, which has no
  endpoints at all. This module defines the rules; that one applies them.
- **Not "what grade did this student get".** Nothing here knows about a student. #8 converts a
  number to a band, and that is arithmetic against a stored table, not a student record.
- **Not curriculum documents**, though `models/academics/README.md` discusses the two together.
  A `CurriculumDocument` is a file-publishing record and shares nothing with this but a paragraph.

---

## One surface, and why

| Surface | Base path | Who is calling | Tenant comes from |
|---|---|---|---|
| **School** | `/schools/current/grading-schemes/…` | the school itself | `CurrentSchoolResolver`, never the URL |

**There is no platform surface, and there must not be one.** Where a school draws the line between
A1 and A2 is the school's own academic policy. No operator of ours should be setting it, for the
same reason `controllers/core` gives no platform surface for academic years.

**And no `{year}` segment**, unlike every path in [`controllers/academics/structure`](../structure/README.md).
See [above](#why-there-is-no-academicyear-here). A shorter path than that module's, and for a
reason rather than by accident:

```text
/schools/current/grading-schemes/6aa29f6d5fb6199794c87e87/resolve?value=94
                                 ^^^^^^^^^^^^^^^^^^^^^^^^         ^^
                                 the scheme id                    the mark
```

### Addressed by id, and keyed by name + version

The rule this project settled on 2026-09-10 is *use whatever other collections already store* —
and three of them store `gradingSchemeDocsId`:

| Thing | Referenced by | Addressed in the URL by |
|---|---|---|
| a grading scheme | `gradingSchemeDocsId`, in **3** places — [`ClassSubject`](../../../models/academics/structure/embedded/ClassSubject.java), [`Exam`](../../../models/academics/examination/Exam.java), [`ReportCard`](../../../models/academics/examination/ReportCard.java) | its **id** |
| a grade band | nothing — a report card snapshots the grade, not the band | `gradeCode`, within its scheme |

**But the unique key is `name` + `schemeVersion`**, and those two are not the same job:

| | what it does | may it change? |
|---|---|---|
| `_id` | what other documents point at | never — it is the identity |
| `name` + `schemeVersion` | what a *person* identifies the rulebook by, and what the unique index enforces | **no — see below** |

**Which means a scheme cannot be renamed, and that is a real cost.** A class and a term are both
addressed by id *and* freely renameable, because nothing joins on their names. A scheme is
addressed by id but its name is half its unique key, so renaming one makes it stop looking like a
version of its own earlier self. **There is deliberately no rename endpoint**, and one must not be
added without first moving the index off `name`. See
[open item 3](#3-name-is-a-key-so-there-can-be-no-rename).

---

## Numbers in messages

The date rule from the other modules does not apply — a scheme has no dates. The equivalent rule
here is about **decimals**, and it matters more than it looks.

```
in a field   "minimumValue": 91.00
in a message "... A1 covers 91 to 100, so 90.5 falls in the gap below it."
```

Every numeric field is `BigDecimal` stored as `DECIMAL128`, never `double`. A boundary of `91.00`
that arrives back as `90.99999999` is a student on the wrong side of a grade line, and floating
point makes that a matter of luck. Messages print through `stripTrailingZeros().toPlainString()`
so `91.00` reads as `91` and `7.50` reads as `7.5` — the same helper
[`AcademicsHelper`](../../../services/academics/helper/AcademicsHelper.java) added for term
weights on 2026-09-12.

---

## Which gates every endpoint runs

[`ActionGate`](../../../common/access/ActionGate.java), the same pattern as everywhere else:

| | Gates |
|---|---|
| every **write** below | 1 school is live · 2 school is paying |
| every **read** below | none — looking at a rulebook is not an action on it |

Called in the controller under their `//! Gate N — …` banners, never in the service.

**Gate 4 is not used here, and it is not a judgement call.** Gate 4 asks whether *a named academic
year* is the school's working year. There is no year in any of these paths, so there is nothing to
ask it about. Applying it would mean inventing a year the request never mentioned.

**Which makes this the one academics module a school can still configure after ending a year.**
[`controllers/core`](../../core/README.md) documents the freeze that `POST .../end` creates:
holidays, enrollment and result locks all stop answering. Grading schemes keep working, because
they were never scoped to the year that ended — and they have to, since correcting a 2026 report
card means reading the 2026 scheme.

---

# The endpoints

Numbered straight through, 1 to 9. **A number is never reused for a different endpoint** — these
get referenced from the service, the API catalogue and the Postman collection. Grouped only so the
list is readable. Every path below is relative to **`/schools/current/grading-schemes`**.

## 1. The schemes — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>1 | [`POST /`](#e1) | Create a rulebook and its bands in one write. The only endpoint that creates a scheme from nothing; #2 creates one from another. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t2"></a>2 | [`POST /{id}/versions`](#e2) | The next version of the same rulebook — "A1 moves to 90". **This is what an edit is**, because editing in place rewrites every report card ever issued. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t3"></a>3 | [`PUT /{id}/bands`](#e3) | Replace the band set of a scheme **nothing has used yet** — fixing a typo during setup. Refused the moment anything references it; then it is #2. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t4"></a>4 | [`POST /{id}/deactivate`](#e4) | Retire a version so it stops being offered for new work, while old report cards still resolve through it. Idempotent. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t5"></a>5 | [`POST /{id}/reactivate`](#e5) | Put it back. The pair exists because there is no `DELETE`. Idempotent. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |

## 2. The schemes — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t6"></a>6 | [`GET /`](#e6) | The school's schemes, filtered by `?active=`, `?scaleType=`, `?search=`, sorted and paged. The dropdown behind every "how is this graded" field. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t7"></a>7 | [`GET /{id}`](#e7) | One scheme with every band, in order. What a school reads to check its own boundaries. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t8"></a>8 | [`GET /{id}/resolve?value=`](#e8) | **Turn a mark into a grade.** The whole purpose of the model, and the only endpoint that proves the bands were entered correctly. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t9"></a>9 | [`GET /{id}/versions`](#e9) | Every version of one rulebook, oldest first. Answers "what did A1 mean in 2026?" without knowing the id of the old one. | [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |

---

# Build order

Ordered by **what it unblocks**, not by number.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **1** | A rulebook exists, can be read, and demonstrably converts a mark | 1, 6, 7, 8 |
| **2** | Setup mistakes are fixable, and history is protected properly | 3, 2, 9 |
| **3** | Versions can be retired without being deleted | 4, 5 |

**Phase 1 is the whole of the first cut**, and #8 is in it on purpose. Without it, #1's band rules
are enforced on write and never exercised on read — the fastest way to ship a validator that is
subtly wrong and not find out. #8 is a pure function over data #1 just stored, so it costs almost
nothing and turns every band rule into something a person can see working.

**#3 before #2.** Replacing bands on an unused scheme is what a school does the same afternoon it
made a typo; creating a version is what it does a year later. Building the simpler write first
means #2 can be written knowing what a valid band set looks like, rather than the reverse.

**#9 with #2**, because a version list is meaningless until there is more than one version.

**Nothing else in the project is blocked on any of this.** `Exam` and `ReportCard` would consume a
scheme and neither has an endpoint; `ClassSubject` already stores a `gradingSchemeDocsId` and
validates it exists, which is all it needs. This module is therefore **safe to build slowly** and
is not on anyone's critical path.

---

# Things this module deliberately will not have

- **No `DELETE`.** Three places store `gradingSchemeDocsId` and none of those references is a
  foreign key. A deleted scheme leaves every one of them pointing at nothing, and *nothing would
  fail* — a report card would simply reprint with no grades and nobody would know why. Retirement
  is #4, the same decision `controllers/academics/structure` made for classes, terms, sections and
  subjects.
- **No rename, and no `PATCH` of any kind.** Work through the fields and there is nothing a patch
  could safely touch: `name` and `schemeVersion` are the unique key, `scaleType` and
  `maximumValue` reinterpret every band under them, `gradeBands` is the history, and `active` has
  its own pair of endpoints. **An endpoint with no legal field is not an endpoint**, so there is
  no #-number reserved for one. The nearest thing is #3, which replaces bands and only while
  nothing has used them.
- **No cross-scheme resolve.** "Grade this mark for this subject" has to walk
  `ClassSubject.gradingSchemeDocsId` → `Exam.gradingSchemeDocsId` → none, and two of those three
  live in a module with no endpoints. #8 resolves against **one named scheme** and leaves the
  chain to the caller that owns it. See [open item 6](#6-the-fallback-chain-has-no-owner).
- **No band-level endpoints.** No `POST /{id}/bands/{gradeCode}`. A band set is only ever valid as
  a whole — adding one band always risks an overlap or a gap with its neighbours — so it is
  written as a set, by #1 and #3, or not at all. Same reasoning that gave the academic year a
  whole-calendar `PUT`.
- **No `DESCRIPTOR` resolution by value.** #8 answers `404` for a descriptor scheme rather than
  guessing. A descriptor grade is *chosen*, not computed; see
  [open item 2](#2-descriptor-could-not-be-stored--settled-2026-09-13).

---

# To settle before building

## 1. `schemeCode` did not exist — settled 2026-09-13

[`GradingScheme`](../../../models/academics/grading/GradingScheme.java) declared:

```java
@CompoundIndex(
        name = "school_grading_code_version_uniq",
        def = "{'schoolId': 1, 'schemeCode': 1, 'schemeVersion': 1}",
        unique = true)
```

`schemeCode` appeared **exactly once in the entire project** — in that string. No field declared
it, no service wrote it, nothing read it.

MongoDB does not error on an index over a missing field; it indexes the field as `null`. So the
index was effectively `{schoolId, null, schemeVersion}`: **one scheme per school per version
string.** A school creating "CBSE Percentage" `v2026.1` and then "IB Points" `v2026.1` would have
had the second refused as a duplicate, with an error naming a field it had never heard of.

**It is the same bug `SchoolClass` carried**, settled by [open item 1 of the structure
plan](../structure/README.md#1-classcode-did-not-exist--settled-2026-09-10) — where the index
named a `classCode` no model declared and allowed one class per school per year. Twice is a
pattern, and the lesson is the same: *an index is code, and an index naming a field that does not
exist is a silent constraint on something else entirely.*

**Settled: the index is `school_grading_name_version_uniq` on `{schoolId, name, schemeVersion}`.**
No code field is introduced. The decision went the other way from the class one — a class got no
code at all and is keyed on `name`; a scheme is keyed on `name` *plus* a version, because the
whole point of a scheme is that the same rulebook exists more than once.

## 2. `DESCRIPTOR` could not be stored — settled 2026-09-13

[`GradingScaleType`](../../../models/academics/enums/GradingScaleType.java) offers three scales,
and one of them had no valid representation:

```java
// GradeBand, before
@NotNull private BigDecimal minimumValue;
@NotNull private BigDecimal maximumValue;
```

A `PERCENTAGE` or `POINT` band resolves a mark by range, so both bounds are essential. A
`DESCRIPTOR` band — "Beginning", "Developing", "Secure" — has no mark to compare: a teacher picks
it directly. Storing one meant inventing `0..1`, `1..2`, `2..3` for fields nothing would ever read.

**Settled: both bounds are nullable on the model, and required by the service.** Bean validation
cannot express *"required unless a sibling field says otherwise"*, so the rule lives in
`GradingHelper`, which can see `scaleType`:

| `scaleType` | `maximumValue` on the scheme | bounds on each band | how #8 resolves |
|---|---|---|---|
| `PERCENTAGE` | **required** — usually 100 | **required**, ordered, no overlaps | by range |
| `POINT` | **required** — 7 for IB, 4 for a GPA | **required**, ordered, no overlaps | by range |
| `DESCRIPTOR` | **refused** — nothing to measure | **refused** | not at all — `404` |

The alternative was deleting the enum constant. It was kept because the model README names all
three deliberately and a scale a school picks from a list is easier to add than to re-derive.

## 3. `name` is a key, so there can be no rename

Settling item 1 by indexing `name` has a consequence worth stating plainly rather than discovering
later: **`name` stopped being a label and became half the identity.**

| | addressed by | keyed by | renameable |
|---|---|---|---|
| a class | id | `name` | **yes** — #13 does it |
| a term | id | `termCode` | **name yes, code no** — #3 |
| a grading scheme | id | `name` + `schemeVersion` | **no endpoint exists** |

Renaming `CBSE Percentage Grading v2026.1` to `CBSE Grading v2026.1` would leave it looking
unrelated to `CBSE Percentage Grading v2027.1` — the two stop being versions of one rulebook,
which is the only thing `schemeVersion` is for. #9 would return one row where it should return
two.

**Unsettled: whether this is permanent.** The alternative is the `schemeCode` field the index
originally implied — stable, uppercase, `CBSE_PCT`, exactly the shape `termCode` settled on — with
`name` freed to be edited. That is the design [#3 of the structure
plan](../structure/README.md#e3) arrived at for terms, and the argument for it is the same one.
**It was not taken here**, on the grounds that a scheme has far fewer consumers than a term and a
fourth field earns its place less easily. Revisit if a school asks to rename one.

## 4. "Has this scheme been used?" is a query across three collections

#3 refuses to replace bands on a scheme anything references. Finding out whether anything does
means asking:

| Collection | Field | Endpoint that writes it |
|---|---|---|
| `school_classes.subjects[]` | `gradingSchemeDocsId` | **built** — #22 and #24 of the structure module |
| `exams` | `gradingSchemeDocsId` | none |
| `report_cards` | `gradingSchemeDocsId` | none |

**Only the first is reachable today**, and `ClassSubject` is an embedded row, so the query is
`school_classes` with `subjects.gradingSchemeDocsId` — an unindexed nested field.

**The same shape as the term-deletion problem**, which the structure module answered by never
deleting. The answers available:

- **a)** Check only what is reachable — `school_classes` today, the other two as they arrive.
  Honest, incomplete, and gets quietly less wrong over time. **Needs an index** on
  `subjects.gradingSchemeDocsId` or it is a collection scan per call.
- **b)** Refuse #3 outright once a scheme is older than its creation request — no reference check
  at all, versioning is the only path. Simplest and most conservative; makes fixing a setup typo
  needlessly painful.
- **c)** Carry a `usageCount` on the scheme, incremented by whatever references it. Fast to read
  and wrong the first time any writer forgets to increment.

**Recommendation: (a)**, with the index, and the refusal message naming which collections were
actually checked so nobody reads a pass as a guarantee. **Decide before #3**, not before #1 —
phase 1 does not touch this.

## 5. A gap between bands is not always a mistake

The model README says services must reject *"gaps that are not intentional"*, which is not
something a service can tell apart. `81..90` beside `91..100` is contiguous; `81..89` beside
`91..100` leaves `90` unresolvable. Both are typeable and only one is a typo — but a scheme that
deliberately grades `0..32` as `E` and says nothing about `33..40` because no subject is marked
out of 40 is also legitimate.

**Proposed: report, never refuse** — the same answer the term-weight sum arrived at in
[open item 3 of the structure plan](../structure/README.md#3-term-weights-cannot-be-validated-one-patch-at-a-time--settled-2026-09-12).
#1 and #3 return a `warning` naming the uncovered ranges; #8 answers `404 GRADE_NOT_RESOLVABLE`
for a value that lands in one, which is where a real gap actually hurts and where the message can
be specific about it.

**Overlaps are refused, and that asymmetry is the point.** A gap means one mark has *no* grade,
which is visible and fixable. An overlap means one mark has *two*, and which one wins depends on
the order the bands happen to be stored in — silently, differently, per scheme.

## 6. The fallback chain has no owner

Three fields resolve a subject's grading scheme, in order:

```text
ClassSubject.gradingSchemeDocsId    this subject grades differently
      ↓ if null
Exam.gradingSchemeDocsId            the exam's default
      ↓ if null
none                                raw marks, no letter grade
```

The first is built. The second lives on [`Exam`](../../../models/academics/examination/Exam.java),
which has no endpoints, and the chain has **no single place that walks it** — the `examination`
module's README describes the order in prose and no code implements it.

**This module deliberately does not implement it either.** #8 resolves against one scheme the
caller names. Whoever builds mark entry owns the chain, and it should be written **once** there
rather than twice — the same conclusion [open item 7 of the structure
plan](../structure/README.md#7-academictermresultslocked-and-academicyearresultslocked-both-exist)
reached about the two `resultsLocked` flags.

---

# Where the code will live

Following the four folder rules in `memory/backend/code-writing-rules`:

```text
controllers/academics/grading/
├── README.md                            <- this file
└── GradingSchemeController.java

services/academics/
├── GradingSchemeService.java
├── helper/GradingHelper.java            band coherence, and the resolve arithmetic
└── utils/GradingSchemeServiceUtils.java one scheme by id, scoped to the school

repositories/academics/gradingscheme/
├── GradingSchemeRepository.java         exists — one method, an existence check
├── GradingSchemeRepositoryCustom.java   #6's filtered, paged search
└── GradingSchemeRepositoryImpl.java     beside its interface, or Spring finds nothing

dto/academics/gradingscheme/{request,response}/
```

**Its own controller, not `SchoolClassController`'s.** A scheme shares no path segment with a
class — no `{year}`, no `{classId}` — and the only thing connecting them is a stored id.

**A helper of its own, not `AcademicsHelper`.** That class is explicitly *"the rules MongoDB
cannot express about terms"* and its methods all take a term list. Band coherence shares nothing
with it but a module name. The two conventions it must follow are the ones already written there:
no check calls another check, and every method takes the band set rather than reading it.

**One warning, repeated from the plans module:** Spring Data resolves a custom fragment
`XRepositoryImpl` by **name and package**. Put it beside its interface, or the app compiles,
starts, and fails only when somebody calls `search`.

---

# Appendix — what each field can hold

## `grading_schemes` — [GradingScheme](../../../models/academics/grading/GradingScheme.java)

| Field | Type | What can be in it |
|---|---|---|
| `name` | String, required | The rulebook's name — `"CBSE Percentage Grading"`. **Unique with `schoolId + schemeVersion`**, so it is a key rather than a label: there is no rename. Versions of one rulebook must carry the identical name or #9 stops finding them. |
| `schemeVersion` | String, required | `"2026.1"`. Free text, ordered by the school's own convention. **Never changes on a saved scheme** — moving a boundary means a new document, not a new value here. |
| `scaleType` | Enum, required | `PERCENTAGE` · `POINT` · `DESCRIPTOR`. Decides whether bands carry bounds at all, and whether #8 can answer. **Never changes** — it reinterprets every band under it. |
| `maximumValue` | BigDecimal, `DECIMAL128` | The ceiling bands are read against: `100` for a percentage, `7` for IB points. **Required for `PERCENTAGE` and `POINT`, refused for `DESCRIPTOR`.** |
| `gradeBands` | List, required non-empty | Ordered, non-overlapping, inside `maximumValue`, `gradeCode` unique within the set. Written whole by #1 and #3; never one at a time. |
| `active` | Boolean, required | `true` at create; `false` from #4, `true` from #5. **Not the record lifecycle** — an inactive scheme is still resolved by every report card that used it; it just stops appearing in dropdowns. |

## `grading_schemes.gradeBands[]` — [GradeBand](../../../models/academics/grading/embedded/GradeBand.java)

| Field | Type | What can be in it |
|---|---|---|
| `gradeCode` | String, required | What prints on the card — `"A1"`, `"7"`, `"DEVELOPING"`. Unique within the scheme, case-insensitively. |
| `minimumValue` | BigDecimal, `DECIMAL128` | Inclusive lower bound. **Required for `PERCENTAGE`/`POINT`, refused for `DESCRIPTOR`.** |
| `maximumValue` | BigDecimal, `DECIMAL128` | Inclusive upper bound — a band ending `90` includes `90`. Same rule as above, and must be `>= minimumValue`. |
| `gradePoint` | BigDecimal, `DECIMAL128`, optional | What this band contributes to a CGPA. Null means the scheme grades without points, which is normal — not a missing value. |
| `description` | String, optional | `"Outstanding"`. What a parent reads beside the code. |
| `passed` | Boolean, required | Whether this band is a pass. **Stored, not derived from a threshold**: a practical may pass at 40 where theory passes at 33, and some boards have no pass/fail at all. |

## The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `GRADING_SCHEME_NOT_FOUND` | 404 | no scheme with that id in this school |
| `GRADE_NOT_RESOLVABLE` | 404 | #8 — the value is inside the scale but lands in a gap between bands |
| `SCHEME_VERSION_TAKEN` | 409 | this school already has that `name` + `schemeVersion` |
| `GRADE_BANDS_REQUIRED` | 400 | an empty band list — a scheme that grades nothing is not a scheme |
| `GRADE_BAND_CODE_TAKEN` | 409 | two bands in one set share a `gradeCode` |
| `GRADE_BAND_BOUNDS_REQUIRED` | 400 | a `PERCENTAGE`/`POINT` band with no bounds |
| `GRADE_BAND_BOUNDS_NOT_ALLOWED` | 400 | a `DESCRIPTOR` band carrying bounds, or a `DESCRIPTOR` scheme carrying `maximumValue` |
| `SCALE_MAXIMUM_REQUIRED` | 400 | a `PERCENTAGE`/`POINT` scheme with no `maximumValue` |
| `INVALID_GRADE_BAND_RANGE` | 400 | `maximumValue` below `minimumValue` on one band |
| `GRADE_BANDS_OVERLAP` | 409 | two bands cover the same value — refused, unlike a gap |
| `GRADE_BAND_OUTSIDE_SCALE` | 409 | a band reaches past the scheme's `maximumValue`, or below zero |
| `VALUE_OUTSIDE_SCALE` | 400 | #8 — the value asked about is above `maximumValue` or below zero |
| `SCHEME_NOT_RESOLVABLE_BY_VALUE` | 409 | #8 against a `DESCRIPTOR` scheme — a descriptor grade is chosen, not computed |
| `SCHEME_STILL_REFERENCED` | 409 | #3 — something already uses this scheme, so its bands are history now; use #2 |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

## The schemes — writes · 1–5

<a id="e1"></a>
**[1](#t1) · `POST /`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: whether this school already has that `name` + `schemeVersion`
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *insert*: `schoolId`, `name`, `schemeVersion`, `scaleType`, `maximumValue`, `gradeBands`, `active` = `true`
- **The scale decides the shape of everything under it**, and is checked first: `DESCRIPTOR` refuses `maximumValue` and refuses bounds on every band; `PERCENTAGE` and `POINT` require all three. Checking a band's bounds before knowing the scale would produce the right refusal for the wrong reason.
- **Then the band set, as a set**, in this order: each band's own range is not inverted (`400 INVALID_GRADE_BAND_RANGE`), no code repeats (`409 GRADE_BAND_CODE_TAKEN`), none reaches outside the scale (`409 GRADE_BAND_OUTSIDE_SCALE`), none overlaps another (`409 GRADE_BANDS_OVERLAP`). Order matters for the same reason it does in the term plan: an inverted range checked last gets reported as an overlap, which is true and says the wrong thing.
- **Gaps are reported, not refused** — a `warning` naming the uncovered ranges. See [open item 5](#5-a-gap-between-bands-is-not-always-a-mistake).
- **`active` is not accepted at create.** It is an event with its own endpoints, #4 and #5, and a scheme created already retired is a state nothing asked for. Same rule as every other create in `academics`.
- **Bands are stored in the order given, not re-sorted.** A school listing `A1` first means A1 first, and a response that silently reordered them would make a typo hard to spot against the paper it was copied from.

<a id="e2"></a>
**[2](#t2) · `POST /{id}/versions`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: the source scheme, for `name` and `scaleType`
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *insert*: a new document — `name` and `scaleType` **copied**, `schemeVersion` and `gradeBands` from the body, `active` = `true`
- **`name` is copied, never accepted.** It is what makes the new document a version *of this one*; taking it from the body would let a caller create an unrelated scheme through an endpoint whose whole purpose is relatedness.
- **`scaleType` is copied too.** A "version" that changed a percentage scale into a point scale is a different rulebook wearing the same name, and #9 would list the two as though a student could be compared across them.
- **`maximumValue` may change**, and that is the interesting exception: a school moving from marks out of 100 to marks out of 50 is the same rulebook rescaled. Every band is re-checked against the new ceiling.
- **The source is not modified**, including not being deactivated. Retiring the old version is #4, a separate decision a school may not want yet — both versions being active at once is normal while a year is in flight.
- **`409 SCHEME_VERSION_TAKEN`** if that `name` already has that version.

<a id="e3"></a>
**[3](#t3) · `PUT /{id}/bands`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: the scheme, for `scaleType` and `maximumValue`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: whether any `subjects[].gradingSchemeDocsId` is this scheme
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *updates*: `gradeBands`, and nothing else
- **Refused the moment anything references this scheme** — `409 SCHEME_STILL_REFERENCED`, naming what was found. Then the path is #2, which is what versioning is for.
- **The reference check is incomplete and says so.** Only `school_classes` is reachable today; `exams` and `report_cards` have no endpoints and therefore no rows. The message names which collections were actually checked, so a pass is never read as a guarantee. See [open item 4](#4-has-this-scheme-been-used-is-a-query-across-three-collections).
- **The same band rules as #1**, run against the whole new set — this is a replace, so the old set has no say in whether the new one is valid.
- **Not a `PATCH`, and not per-band.** A band set is only valid as a whole; adding one band always risks an overlap or a gap with its neighbours.

<a id="e4"></a>
**[4](#t4) · `POST /{id}/deactivate`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *updates*: `active` = `false`
- **Idempotent** — already inactive is a `200` saying so, not a `409`. Same rule as the term lock pair.
- **It does not stop the scheme resolving.** #7, #8 and #9 all still answer for an inactive scheme, and they must: a report card issued in 2026 has to reprint through the 2026 rules long after the school moved to 2027's. `active` governs what is *offered for new work*, nothing else.
- **No reference check, deliberately** — unlike #3. Retiring a scheme everything uses is exactly what a school does when it publishes the next version.

<a id="e5"></a>
**[5](#t5) · `POST /{id}/reactivate`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *updates*: `active` = `true`
- **Idempotent**, and exists because there is no `DELETE`. A school that retired the wrong version needs a way back that does not involve creating a third one.

## The schemes — reads · 6–9

<a id="e6"></a>
**[6](#t6) · `GET /`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: one page, filtered
- **Three filters, all optional, all AND-ed**: `?active=` (absent returns both, which is not the same as `false`), `?scaleType=`, `?search=` matching `name` case-insensitively anywhere.
- **`?search=` matches `name` only**, unlike the term list which also matches a code. There is no code to match — which is itself an argument for [open item 3](#3-name-is-a-key-so-there-can-be-no-rename).
- **The needle is `Pattern.quote`d**, so a stray `(` is an empty result rather than a 500 from `PatternSyntaxException`. Same as every other search in this project.
- **Bands are not returned**, only `bandCount`. A twelve-scheme page carrying twelve full band tables is a large response nobody reads; #7 is one call away.
- **Sorted by `name`, then `schemeVersion`** — the two that are unique together, so paging is stable and cannot put one row on two pages while another is never seen. `?sort=` is an allowlist: `name`, `schemeVersion`, `scaleType`, `createdAt`, `updatedAt`.
- **No gates.** A suspended school still reads its own grading rules.

<a id="e7"></a>
**[7](#t7) · `GET /{id}`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: the scheme with every band
- **Bands in stored order**, which is the order they were written in — see #1.
- **Carries the same gap `warning` #1 returned**, recomputed rather than stored. A school that ignored the warning on create should still see it every time it looks, and storing it would mean a stale sentence surviving a later fix.
- **Answers for an inactive scheme.** See #4.

<a id="e8"></a>
**[8](#t8) · `GET /{id}/resolve?value=`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `scaleType`, `maximumValue`, `gradeBands`
- **Writes nothing.** It is arithmetic over one document, which is why it is a `GET` with the value in the query string rather than a `POST`.
- **The whole reason #1's rules are worth enforcing**, and the reason it is in phase 1: a band set that validates but resolves wrongly is the failure mode a create-only module cannot detect.
- **In order**: the scheme must be resolvable by value at all (`409 SCHEME_NOT_RESOLVABLE_BY_VALUE` for `DESCRIPTOR`), the value must be inside the scale (`400 VALUE_OUTSIDE_SCALE`), and then a band must cover it (`404 GRADE_NOT_RESOLVABLE`).
- **A gap is a `404`, not a `409`.** The caller asked for the grade at `90.5` and there is none — that is a thing not found, and the message names the two bands it falls between so the school can see the hole in its own table.
- **Both bounds inclusive**, so a value equal to a boundary resolves to the band that declares it. Overlaps being refused at write is what makes that unambiguous.
- **Returns the whole band**, not just the code: `gradeCode`, `gradePoint`, `description`, `passed`. A caller that has to make a second call to find out whether the grade was a pass would be a design that guaranteed two round trips per mark.

<a id="e9"></a>
**[9](#t9) · `GET /{id}/versions`**

- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: the scheme, for its `name`
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: every scheme in this school with that `name`
- **Addressed by id, answered by name.** The caller has an id — from a report card, say — and wants the rulebook it belongs to. Requiring them to know the name first would make this endpoint useless to the one caller that needs it.
- **Which is exactly what [open item 3](#3-name-is-a-key-so-there-can-be-no-rename) protects.** A rename breaks this endpoint and nothing else, silently, by splitting one rulebook's history into two.
- **Oldest first, by `schemeVersion`** — a string sort, so a school numbering `2026.1`, `2026.2`, `2026.10` gets `2026.10` in the middle. Documented rather than solved: the alternative is parsing a version string whose format is the school's, not ours.
- **Not paged.** A rulebook has versions in the low single digits, and unlike the term list there is no plausible school with twelve.
