# controllers/academics/structure — API plan

**Twelve of 37 are built — #1, #9, #12, #13, #17, #22, #24, #28, #29, #30, #31 and #37.** A class can be created
for an academic year, its name and affiliation programme edited, sections added to it, subjects
assigned to the class or to one section and then edited, the year's classes listed — filtered,
searched, sorted and paged — and one class read in full, or just its sections, or just the
subjects one section studies.

**Phase 1 is complete.** Everything the `student` module needs from this one exists: a class, a
section inside it, a list to choose from, and a read of one.

**#17 is the one that unblocks another module.** `StudentAcademicRecord` stores `sectionNo` as a
plain string, so no student could be placed anywhere until a section existed. Six of
`models/academics`' own documents were behind the same wall.

**#13 was brought forward from phase 7**, on request, because nothing depends on it and it is the
endpoint that proves the id-addressed design: a class can be renamed, which an academic year
never can. Everything else below
is the full set of endpoints the academic-structure feature needs, written before any of them, so
they can be built and reviewed one at a time — the same way
[`controllers/core`](../../core/README.md) and [`controllers/plans`](../../plans/README.md) were
done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/academics/structure`](../../../models/academics/structure), whose README already
describes the two documents, the two embedded types and the reference rules. **These endpoints
enforce that file. They do not invent new rules** — with one exception that file itself asked
for: the index naming a `classCode` no model declared, now settled. See
[To settle before building](#to-settle-before-building).

> **The blocker is settled — 2026-09-10.** `SchoolClass` had a unique index on `classCode`, a
> field it never declared, which allowed exactly **one class per school per academic year**. The
> index is now `school_year_class_name_uniq` on `name`, and there is no code field: a class is
> addressed and referenced by its **document id**, which is what twelve other documents already
> store as `classDocsId`. See
> [open item 1](#1-classcode-did-not-exist--settled-2026-09-10).

---

## What this module is

The **academic skeleton of one school year**: how the year is divided for reporting, and what is
taught inside it.

```text
AcademicYear  "2026-2027"   2026-04-01 .. 2027-03-31      <- core, built
  |
  ├── AcademicTerm[]     how the year is divided for reporting
  |     "TERM_1"  sequence 1  2026-04-01..2026-09-30  weight 20%
  |     "TERM_2"  sequence 2  2026-10-01..2027-03-31  weight 80%
  |
  └── SchoolClass[]      what is taught in the year
        "Grade 7"
          ├── sections[]   A (cap 40, teacher X) · B · C      embedded
          └── subjects[]   MATHEMATICS · SCIENCE · HINDI       embedded
```

**It is the last root in the design.** [`models/README.md`](../../../models/README.md) names two
stable business keys that the whole system references as plain strings: `AcademicYear.name`, which
`controllers/core` has finished, and `ClassSection.sectionNo`, which lives here and has nothing
built. Everything downstream waits on it.

Two documents and two embedded types:

| Document | Collection | What it holds |
|---|---|---|
| [`AcademicTerm`](../../../models/academics/structure/AcademicTerm.java) | `academic_terms` | one reporting period of a year — the unit a report card is issued for |
| [`SchoolClass`](../../../models/academics/structure/SchoolClass.java) | `school_classes` | one grade taught in a year, with its sections and subjects inside it |
| [`ClassSection`](../../../models/academics/structure/embedded/ClassSection.java) | *embedded* | one section of a class, identified by `sectionNo` |
| [`ClassSubject`](../../../models/academics/structure/embedded/ClassSubject.java) | *embedded* | one subject-and-teacher assignment, identified by `(subjectCode, sectionNo)` |

**A term and a class are independent.** A term does not contain classes and a class does not
belong to a term: a class runs the whole year, while terms slice that year into reporting
periods. They are in one README because they are created together during year setup, and in one
controller for the same reason.

## What this module is not

- **Not attendance, exams, homework or the timetable.** Those are the other six folders of
  `models/academics`, and every one of them references a class or a `sectionNo`. They come after
  this, not with it.
- **Not per-student anything.** A class offers Physics; nothing here says a student *takes*
  Physics. See [the upstream gap](#6-the-upstream-gap--nothing-records-what-a-student-takes).
- **Not staff management.** A section names a class teacher by `Staff.id` and a subject names its
  teachers the same way. Creating those staff records is the `people` module.

---

## One surface, and why

| Surface | Base path | Who is calling | Tenant comes from |
|---|---|---|---|
| **School** | `/schools/current/academic-years/{year}/…` | the school itself | `CurrentSchoolResolver`, never the URL |

**There is no platform surface, and there must not be one.** A class list is a school's own
teaching structure. No operator of ours should be deciding that a school teaches Grade 7, for the
same reason `controllers/core` gives no platform surface for academic years.

### Why the year is in the path

Neither code is unique per school. Both are unique per **school and year**:

```text
school_year_class_name_uniq   {schoolId, academicYear, name}       unique
school_year_term_code_uniq    {schoolId, academicYear, termCode}    unique
```

A class **id** is globally unique on its own, so the year is not needed to *find* one. It is in
the path so that an id pasted from last year's URL answers `404` rather than quietly editing last
year's structure, and so the path reads as what it is — a year, and something inside it. A
`termCode` genuinely needs it, being unique only per year.

```text
/schools/current/academic-years/2026-2027/classes/6aa29f6d5fb6199794c87e87/sections/A
                               ^^^^^^^^^         ^^^^^^^^^^^^^^^^^^^^^^^^          ^
                               the year          the class id                the section
```

Long, and correct. Each segment names one real thing, and the whole path is the document plus the
two keys inside it.

### Addressed by id, except where the thing has no id

**Settled 2026-09-10, and it went the other way from this file's first draft.** The rule is not
"prefer codes" — it is *use whatever other collections already store*, and for the two top-level
documents here that is the document id:

| Thing | Referenced by | Addressed in the URL by |
|---|---|---|
| a class | `classDocsId`, in **12** documents | its **id** |
| a term | `termDocsId`, in **6** documents | its **id** |
| a section | `sectionNo`, in **8** collections | `sectionNo` |
| a subject assignment | `subjectCode`, in **7** collections | `subjectCode` + `?sectionNo=` |

**A section and a subject are codes only because they are embedded** and so have no id to be
referenced by. A top-level document has one, and twelve consumers were already using it — an
earlier draft of this plan invented a `classCode` for consistency with `sectionNo`, and that was
the wrong half of the design to be consistent with.

**Which is what keeps `name` editable.** A class is its id, so renaming it joins nothing and
breaks nothing; the name only has to stay unique inside the year, which
`school_year_class_name_uniq` enforces. Contrast the academic year, which *is* its name to every
other collection and therefore can never be renamed.

**There is still no rename for `sectionNo` or `subjectCode`.** The counts are the reason:

| Key | Stored as a plain string by | A rename would |
|---|---|---|
| `sectionNo` | **8** collections — `attendance_sessions`, `exam_schedules`, `homework`, `report_cards`, `holistic_progress_cards`, `student_academic_records`, `fee_invoices` directly, and `daily_timetables` through its embedded `TimetableEntry` | leave all eight pointing at a section that no longer answers to it |
| `subjectCode` | **7** — `attendance_sessions`, `curriculum_documents`, `homework`, `student_marks`, `exam_schedules` directly, plus `report_cards` and `daily_timetables` through embedded rows | orphan every mark and register for that subject |
| `termCode` | nothing — consumers store `termDocsId`. It is a school-facing label, not a join key. | be safe. #3 may edit it. |
| a class's `name` | nothing — consumers store `classDocsId` | be safe. #13 edits it. |

None of those references is a foreign key. Nothing would fail, nothing would cascade, and every
row would still look valid — the same trap `controllers/core` documents for the academic year
name. A report would come back empty and nobody would know why.

---

## Dates in messages

Same rule as everywhere else, and it applies here to term dates. **Fields carry ISO-8601;
messages spell the date out**, through [`common/time/Dates`](../../../common/time/Dates.java), in
the **school's own timezone** via [`SchoolZone`](../../../common/time/SchoolZone.java).

```
in a field   "startDate": "2026-04-01"
in a message "... Term 1 runs to Wednesday 30 September 2026, so Term 2 cannot start on the 29th."
```

`AcademicTerm.startDate` and `endDate` are `LocalDate`, not `Instant` — a term boundary is a
calendar day, not a moment — so there is no zone conversion to get wrong on the way in. The zone
still matters for **"today"**: `GET .../terms/current` asks which term today falls in, and that is
today in the school's zone. Getting this wrong is a live bug the core module already had and
fixed; see [its README](../../core/README.md#the-server-clock-used-to-decide-what-today-is--fixed-2026-09-10).

---

## Which gates every endpoint runs

[`ActionGate`](../../../common/access/ActionGate.java) is wired and the pattern is settled — see
the [table in `controllers/core`](../../core/README.md#which-gates-run-on-the-academic-year-endpoints).
This module follows it exactly:

| | Gates |
|---|---|
| every **write** below | 1 school is live · 2 school is paying · 4 the year is the school's working year |
| every **read** below | none — looking at a structure is not an action on it |

Called in the controller under their `//! Gate N — …` banners, never in the service.

**Gate 3 is not used here either, and for the same reason.** Next year's classes and terms are set
up in February and March, before that year starts — `requireRunningAcademicYear` would refuse the
one call a school actually makes. The whole argument is in the core README and is not repeated.

**Plus one check no gate covers: the `{year}` in the path has to exist.** Every endpoint resolves
it through `AcademicYearRepository.findBySchoolIdAndName` and answers
`404 ACADEMIC_YEAR_NOT_FOUND` — the same sentence the core module gives — before doing anything
else. A term written against a year that does not exist is an orphan the moment it is saved.

---

# The endpoints

Numbered straight through, 1 to 37. **A number is never reused for a different endpoint** — these
are referenced from the service, the API catalogue and the Postman collection. **#37 was added
after the plan was written**, and is numbered on the end rather than inserted beside #30, for the
same reason. Grouped only so the
list is readable. Every path below is
relative to **`/schools/current/academic-years/{year}`**, which is left off the table to keep it
readable — so `POST /terms` is `POST /schools/current/academic-years/2026-2027/terms`.

## 1. The terms — writes · [Build order ↓](#build-order)

A term is the unit a report card is issued for. Six other documents point at one by
`termDocsId`, across three modules — `Exam`, `ReportCard`, `HolisticProgressCard`,
`FeedbackCampaign`, `FeeInstallment` and `FeeInvoice`.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /terms`](#e1) | Add one reporting period to the year. The ordinary way a term is created once the year is running and somebody realises a period is missing. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t2"></a>2 | [`PUT /terms`](#e2) | Set the year's whole term structure in one write — "two semesters", "four quarters". What year setup actually does, and the only endpoint that can validate the weights sum to 100, because it is the only one that sees all of them. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t3"></a>3 | [`PATCH /terms/{termId}`](#e3) | Fix one term's name, dates or weight. Cannot change `termCode` or `sequence` — see #4 for order. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t4"></a>4 | [`PUT /terms/order`](#e4) | Reorder the year's terms in one write. **This has to exist**: `sequence` is unique per year, so swapping two terms one `PATCH` at a time hits the unique index halfway through. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t5"></a>5 | [`POST /terms/{termId}/results/lock`](#e5) | Freeze results for this period while another is still being marked. Idempotent. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t6"></a>6 | [`POST /terms/{termId}/results/unlock`](#e6) | Reopen one period's results to correct a mark. Idempotent, and — like core's #27 — records nothing about who or why until there is an audit writer. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t7"></a>7 | [`POST /terms/{termId}/deactivate`](#e7) | Take a term out of use without deleting it, so the exams and cards that reference it stay resolvable. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t8"></a>8 | [`POST /terms/{termId}/reactivate`](#e8) | Put it back. The pair exists because there is no `DELETE`. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |

## 2. The terms — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t9"></a>9 — **built** | [`GET /terms?active=&search=&resultsLocked=&weighted=&coversDate=`](#e9) | Every term in the year, in `sequence` order. Five filters, and **paged** — the plan said not to, and that was revisited. Empty page if the year has none. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t10"></a>10 | [`GET /terms/current`](#e10) | Which term today falls in. What a mark-entry screen opens on, so a teacher does not pick the period by hand. `404` when no term covers today — a legitimate answer during a holiday between terms. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t11"></a>11 | [`GET /terms/{termId}`](#e11) | One term in full. `404` when the year has no term by that code. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |

## 3. The classes — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t12"></a>12 — **built** | [`POST /classes`](#e12) | Create a grade for this year. **Sections and subjects cannot be supplied here** — they go on afterwards through #17 and #22. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t13"></a>13 — **built** | [`PATCH /classes/{id}`](#e13) | Fix the class's display name, sort order, or the affiliation programme it runs under. Nothing structural — no section, no subject, no `active`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) |
| <a id="t14"></a>[~~14~~](#e14) **removed** | `PUT /classes/order` | **Dropped 2026-09-11 with `displayOrder`.** There is no per-class position left to set, so there is nothing for this endpoint to reorder. | — |
| <a id="t15"></a>15 | [`POST /classes/{id}/deactivate`](#e15) | A grade this school no longer runs. Its sections stay resolvable for the records that reference them. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t16"></a>16 | [`POST /classes/{id}/reactivate`](#e16) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 4. The sections inside a class · [Build order ↓](#build-order)

`sectionNo` is the key **eight** other collections store as a plain string — counted from the
models, and one more than the [model README's own "six"](../../../models/academics/structure/README.md), which predates
`FeeInvoice` and `StudentAcademicRecord` carrying it. Everything in this group is shaped by that.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t17"></a>17 — **built** | [`POST /classes/{id}/sections`](#e17) | Add one section — `sectionNo`, capacity, class teacher. **The single most-waited-on endpoint in the module**: `StudentAcademicRecord.sectionNo` cannot be filled until a section exists. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t18"></a>18 | [`PUT /classes/{id}/sections`](#e18) | Replace the class's whole section list. For setup — "Grade 7 has A, B, C, D" in one call. Must refuse to drop a section anything already references. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t19"></a>19 | [`PATCH /classes/{id}/sections/{sectionNo}`](#e19) | Change a section's capacity or class teacher. **Never its `sectionNo`.** | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t20"></a>20 | [`POST /classes/{id}/sections/{sectionNo}/deactivate`](#e20) | Stop using a section without removing it. This is what a school does with a section that has emptied out, and it is the **only** way to retire one. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t21"></a>21 | [`POST /classes/{id}/sections/{sectionNo}/reactivate`](#e21) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 5. The subjects taught in a class · [Build order ↓](#build-order)

The key here is the **pair** `(subjectCode, sectionNo)`, because a class-wide subject and a
per-section one are different assignments of the same subject. `sectionNo` is `null` for
class-wide. So every endpoint that names a single assignment takes `?sectionNo=` — exactly the
shape core's `PATCH .../holidays/{date}?type=` already uses for the same reason.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t22"></a>22 — **built** | [`POST /classes/{id}/subjects`](#e22) | Assign a subject to the class, or to one section of it, with its teachers and grading scheme. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t23"></a>23 | [`PUT /classes/{id}/subjects`](#e23) | Replace the class's whole subject list. The year-setup call, and the only one that can check the whole list for a duplicate pair. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t24"></a>24 — **built** | [`PATCH /classes/{id}/subjects/{subjectCode}?sectionNo=`](#e24) | Change one assignment's display name, short name, type or grading scheme. Not `subjectCode`, and not `sectionNo` — moving an assignment between sections is a delete and an add, and there is no delete. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t25"></a>25 | [`PUT /classes/{id}/subjects/{subjectCode}/teachers?sectionNo=`](#e25) | Set who teaches it. Its own endpoint because it is the one thing on a subject that changes mid-year — a teacher leaves, a substitute takes over — and it replaces a list rather than editing a field. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t26"></a>26 | [`POST /classes/{id}/subjects/{subjectCode}/deactivate?sectionNo=`](#e26) | A subject this class has stopped teaching. Deactivated, not removed, because seven collections store `subjectCode`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t27"></a>27 | [`POST /classes/{id}/subjects/{subjectCode}/reactivate?sectionNo=`](#e27) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 6. The reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t28"></a>28 — **built** | [`GET /classes`](#e28) | The year's classes by `name`, filtered by `active` and searchable. Paged. The screen a school opens to see its own structure. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t29"></a>29 — **built** | [`GET /classes/{id}`](#e29) | The class's own facts, and how much is inside it: four counts, no rows. **The `sections[]` and `subjects[]` arrays were removed on 2026-09-11**, once #30, #31 and #37 each owned one. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t30"></a>30 — **built** | [`GET /classes/{id}/sections`](#e30) | Just the sections, with capacity and class teacher. What a "move a student" dropdown reads instead of pulling the whole class. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t31"></a>31 — **built** | [`GET /classes/{id}/subjects?sectionNo=`](#e31) | The class's subject assignments, or the ones one section studies — **its own rows plus the class-wide ones**. Dropped once and rebuilt with that rule settled. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t32"></a>32 | [`GET /subjects`](#e32) | Every distinct subject taught anywhere in the year, and which classes teach it. Answers "do we teach Sanskrit at all", which no per-class read can. Served by the `school_year_subject_code_idx` that already exists for it. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t33"></a>33 | [`GET /staff/{staffDocsId}/teaching`](#e33) | One teacher's whole load for the year — the sections they are class teacher of, and every subject they are assigned. A teacher's own home screen, and what somebody checks before a teacher resigns. Two indexes already exist for exactly this: `school_year_class_teacher_idx` and `school_year_subject_teacher_idx`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t37"></a>37 — **built** | [`GET /classes/{id}/sections/{sectionNo}`](#e37) | One section, by its number. **Added 2026-09-11**, after the plan: #30 answers "which sections are there" and this answers "this one", which is the difference between a dropdown and a page. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t34"></a>34 | [`GET /structure`](#e34) | The year's whole skeleton in one response: terms, then classes with their sections and subjects. What a mobile app fetches once on login instead of making twenty calls. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 7. Rolling the structure into the next year · [Build order ↓](#build-order)

Both documents are **per year**, so every new academic year starts with no terms and no classes.
A real school's structure barely changes year to year: twelve classes, four sections each, ten
subjects each is 132 objects to retype every April. These two endpoints are why a school will
tolerate the model being per-year at all.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t35"></a>35 | [`POST /terms/copy-from/{sourceYear}`](#e35) | Copy last year's term structure into this year, shifting the dates by one year and refusing anything that lands outside the new year's range. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t36"></a>36 | [`POST /classes/copy-from/{sourceYear}`](#e36) | Copy last year's classes, sections and subjects into this year. **Teacher assignments are the question** — copying them silently assigns staff who may have left; dropping them makes the copy half-useful. A `includeTeachers` flag, defaulting to false, is the honest shape. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`academic_years`](../../../models/core/AcademicYear.java), [`staff`](../../../models/people/staff/Staff.java) |

---

# Build order

Ordered by **what it unblocks**, not by number. The `student` module cannot start at all until a
section exists, so sections come before everything that is merely useful.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **0** | ~~the broken unique index settled~~ — **done 2026-09-10** | *no endpoint; see open item 1* |
| **1** | A class with sections exists, so a student can be placed in one | ~~12~~, ~~17~~, ~~28~~, ~~29~~, ~~30~~ — **complete** |
| **2** | Subjects are assigned, so marks and registers have something to be about | ~~22~~, ~~24~~, ~~31~~ — **complete** |
| **3** | The year is divided, so an exam and a report card have a period | ~~1~~, ~~9~~, 3, 10, 11 |
| **4** | Setup stops being one call at a time | 2, 4, 14, 18, 23 |
| **5** | Things can be retired without being deleted | 5, 6, 7, 8, 15, 16, 20, 21, 25, 26, 27 |
| **6** | Next April does not mean retyping 132 objects | 35, 36 |
| **7** | The reads nothing is blocked on | ~~13~~ *(built early, on request)*, ~~37~~, 32, 33, 34 |

**Phase 1 is the whole point of picking this module.** `StudentAcademicRecord.sectionNo` is the
join between a student and everything academic, and `sectionNo` does not exist anywhere until #17
runs. Six of `models/academics`' own documents are equally stuck behind it.

**#17 before #18.** Adding one section is what a school does when it opens a new one; replacing
the list is what it does once, during setup. Building the single-add first means the replace can
be written knowing what one add validates, rather than the reverse.

**Terms are phase 3, not phase 1**, even though they look like the simpler document. Nothing is
blocked on them: `Exam` and `ReportCard` need `termDocsId`, and neither has an endpoint. Classes
and sections have eight collections waiting.

---

# Things this module deliberately will not have

- **No `DELETE` on a class, a term, a section or a subject.** Eight collections store `sectionNo`,
  seven store `subjectCode`, and six documents across three modules point at a term by
  `termDocsId` — `Exam`, `ReportCard`, `HolisticProgressCard`, `FeedbackCampaign`, `FeeInstallment`
  and `FeeInvoice`. "Is this still used?" is not a
  foreign-key check when the references are strings — it is a query across every collection that
  carries the field, in three modules. Until that is cheap, everything is **deactivated**. This is
  the same decision, for the same reason, that left `controllers/core` with no `DELETE` on an
  academic year.
- **No rename of `sectionNo` or `subjectCode`.** Eight and seven collections store them as plain
  strings, so a rename would neither fail nor cascade. See
  [the table above](#addressed-by-id-except-where-the-thing-has-no-id). A class `name` and a
  `termCode` *are* editable — nothing joins on either.
- **No read that is *only* another read with fields removed.** Sections and subjects are
  *embedded*, so every per-class read is the same `findById` on the same document — a narrower
  response saves a few hundred bytes and adds an endpoint to keep in step. Each of the three earns
  its place by owning a question instead: #30 owns `?active=`, #31 owns "what does this section
  study", #32 aggregates across classes. #31 was dropped on this reasoning and rebuilt once the
  question it owns was made explicit — the projection was never the point.
- **No moving a subject assignment between sections.** `(subjectCode, sectionNo)` is the key, so a
  move is a delete and an add, and there is no delete. Deactivate the old assignment and add the
  new one — two calls, and the history reads correctly.
- **No platform surface.** No operator of ours decides that a school teaches Grade 7.
- **No sections or subjects in the class create body.** #12 makes an empty class. This is the
  shape the codebase already uses twice — an academic year is created with no holidays, a plan
  with no features — and the reason is the same: a create that can fail on either a bad class name
  or a bad subject leaves the caller working out which, and a half-written subject list is worse
  than an empty one.
- **No class or section fields copied onto `Student` or `StudentAcademicRecord` beyond
  `sectionNo`.** There would then be two answers to "which class is this student in".
- **No `capacity` enforcement here.** A section says it plans for 40. Refusing the 41st student is
  the student module's decision, at the point of placement, where the count actually lives.
- **No weight arithmetic on a single-term write.** See the open item below — this one is a trap,
  not a simplification.

---

# To settle before building

## 1. `classCode` did not exist — settled 2026-09-10

`SchoolClass` carries a **unique** index on a field it never declares:

```java
@CompoundIndex(name = "school_year_class_code_uniq",
        def = "{'schoolId': 1, 'academicYear': 1, 'classCode': 1}", unique = true)
```

Every document therefore indexes a **missing** value, all of them collide, and a school can hold
exactly **one class per academic year**. The second `POST /classes` is a duplicate-key error.
[The model README already flags this](../../../models/academics/structure/README.md#open-item--classcode)
and says it must be resolved before the collection is used. It is the reason nothing else in this
plan can be built.

The two options that README names, and what each costs:

**Neither option was taken. A third one was, and it is better than both.** The premise was
wrong: a class does not need a code at all. **Twelve documents already reference a class by
`classDocsId`** and not one stores a class code, so the index was repointed at `name` and the
class is addressed by its id.

```
school_year_class_code_uniq  {schoolId, academicYear, classCode}  ->  DROPPED
school_year_class_name_uniq  {schoolId, academicYear, name}       ->  CREATED, unique
```

| Option considered | Why not |
|---|---|
| Add a `classCode` business key | It was added, then removed the same day. `sectionNo` and `subjectCode` are codes because they are **embedded** and have no id to be referenced by; a top-level document has one. Inventing a code for a class was being consistent with the wrong half of the design. |
| Repoint the index at `name`, keeping code-addressed URLs | Half right. Repointing the index was correct; keeping `name` in the URL was not, because it would make the name a join key and therefore immutable. |
| **Repoint at `name`, address by id** | Taken. `name` carries a real uniqueness guarantee and stays editable, because nothing joins on it. |

Uniqueness on `name` does **not** make the name immutable, which is the objection that made
repointing look bad at first. That would only be true if the name were the join key — and it is
not, because the id is. #13 edits it freely.

**Both index changes were applied to `edusphere_dev` by hand**, because `app.mongo.sync-indexes`
is `false` in dev and building all 707 indexes costs about six minutes. **Anyone with their own
database has to do the same**, or the dropped index keeps enforcing uniqueness on a field that no
longer exists:

```js
db.school_classes.dropIndex("school_year_class_code_uniq")
db.school_classes.createIndex({schoolId: 1, academicYear: 1, name: 1},
                              {name: "school_year_class_name_uniq", unique: true})
```

*This is also why the bug survived design review: with `sync-indexes` off, the index is absent on
a fresh database and the collision appears only where it has been built once. It was live in
`edusphere_dev`.*


## 2. Three referenced collections had no repository — settled 2026-09-11

The model README requires "existence and tenant ownership of referenced staff, grading scheme,
programme". None of the three is reachable:

| Reference | Collection | Repository |
|---|---|---|
| `ClassSection.classTeacherDocsId`, `ClassSubject.teacherDocsIds` | `staff` | `StaffRepository` — **built 2026-09-11 with #17** |
| `ClassSubject.gradingSchemeDocsId` | `grading_schemes` | `GradingSchemeRepository` — **built 2026-09-11 with #22** |
| `SchoolClass.affiliationProgrammeDocsId` | `affiliation_programmes` | `AffiliationProgrammeRepository` — **built 2026-09-10 with #12** |

**Settled 2026-09-11 — all three are built**, each one interface with a single
`findByIdAndSchoolId`: `AffiliationProgrammeRepository` with #12, `StaffRepository` with #17,
`GradingSchemeRepository` with #22. The recommendation was to build them rather than accept the ids
unvalidated, and that is what was done.

**The tenant in the query is the whole point.** A *real* id belonging to another school is a `404`,
which is the case worth testing and the one a plain `findById` would have accepted. Every endpoint
using them has a test for exactly that, because in all three cases a mutation swapping the lookup
for `findById` left every other assertion green — an other-school row is the only fixture that
tells the two apart.

## 3. Term weights cannot be validated one `PATCH` at a time

Active term weights must sum to 100 when a school weights at all. With two terms at 20 and 80,
changing them to 30 and 70 means two `PATCH` calls, and **the first one is always invalid** —
30 + 80 = 110. Enforce the sum on a single-term write and the values can never be changed.

The same shape as the `sequence` problem, and it wants the same answer:

- **#2 and #4 validate the whole set**, because they see all of it.
- **#3 does not refuse a broken sum.** It reports it — a `warning` on the response saying the
  active weights now total 110 and the annual result cannot be computed until they total 100.

Which means **the sum is a property of the set, checked wherever the set is written, and reported
everywhere else.** Decide this before #3, because retrofitting it means changing what #3 refuses.

## 4. `active` and `recordState` are two flags for overlapping things

Every document here inherits `recordState` (`ACTIVE` · `INACTIVE` · `ARCHIVED` · `DELETED`) from
[`SchoolBase`](../../../models/base/SchoolBase.java) **and** declares its own `active` boolean.
The deactivate endpoints (#7, #15, #20, #26) have to write one of them, and picking wrongly means
two sources of truth for "is this in use".

**Recommended split:** `active` is the *academic* fact — this school does not teach this subject
this year — and is the only one this module's endpoints write or expose. `recordState` stays the
*record* lifecycle, for archival and soft deletion, and is nobody's business here. Say it once, in
the service, or the two will drift.

## 5. `PUT` on an embedded list can silently drop a referenced key

#18 and #23 replace a whole list. A section left out of the new list disappears — and eight
collections may already store its `sectionNo`. The replace has to compare old against new and
refuse to drop a key that is referenced, which needs the very cross-collection query the
[no-`DELETE` decision](#things-this-module-deliberately-will-not-have) says is not cheap yet.

Two workable shapes: **refuse any replace that drops an existing key** (a replace becomes
add-and-edit only, and removals go through deactivate), or **build the reference check** and let
it drop what nothing uses. The first is one comparison and needs nothing new.

**Recommended: refuse to drop.** It makes #18 and #23 safe to build in phase 4 rather than
waiting on six other modules.

## 6. The upstream gap — nothing records what a student *takes*

Straight from the model README, and it belongs in this plan because #31 and #32 look like they
answer it and do not. `ClassSubject` records that Grade 11 offers Physics and Biology. **Nothing
records that a particular student takes Physics.** So a register or a mark sheet generated from a
section roster will list every student for every elective.

It needs a subject list on `StudentAcademicRecord`, or a small `StudentSubjectEnrollment`
collection — in the `student` module, not here. **Nothing in this plan is blocked by it**, but
attendance and mark entry both are, so it wants deciding while the student module is being
designed rather than after.

## 7. `AcademicTerm.resultsLocked` and `AcademicYear.resultsLocked` both exist

The model README settles the rule — the year-wide flag is the stronger control and a result write
must satisfy **both** — so #5 and #6 are the narrower control and must not touch the year's flag.
Nothing in this module reads either one; the endpoint that has to satisfy both is mark entry, in
`examination`. Worth a shared check when it arrives, not two.

## 8. Containment inside the academic year is a service rule, and there are three of them

MongoDB cannot express any of these. Every one is a service check, and each needs a refusal code
of its own:

| Rule | Applies to |
|---|---|
| a term's dates fall inside the academic year's `startDate`..`endDate` | 1, 2, 3, 35 |
| terms do not overlap each other | 1, 2, 3, 35 |
| `sequence` is unique in the year | 1, 2, 4, 35 |

The core module solved the identical overlap problem for academic years in
`CoreHelper.validateNoAcademicYearOverlap`. **Read that before writing a second one** — a term
overlap and a year overlap are the same query with a different scope, and two implementations
will eventually disagree about whether touching endpoints overlap.

---

# Where the code will live

Following the four folder rules in `memory/backend/code-writing-rules`:

```text
controllers/academics/structure/
├── README.md                        <- this file
├── AcademicTermController.java
└── SchoolClassController.java       sections and subjects hang off the class

services/academics/
├── AcademicTermService.java
├── SchoolClassService.java
├── helper/AcademicsHelper.java      validation shared across the module
└── utils/SchoolClassServiceUtils.java

repositories/academics/
├── academicterm/AcademicTermRepository.java
└── schoolclass/SchoolClassRepository.java

dto/academics/academicterm/{request,response}/
dto/academics/schoolclass/{request,response}/
```

**Two controllers, not one.** A term and a class are independent documents with independent keys;
one controller holding both would be 36 endpoints deep and the `{year}` prefix is all they share.

**Sections and subjects get no controller of their own.** They are embedded — there is no document
to address, only a path into one — so they belong to `SchoolClassController`, the way holidays
belong to `AcademicYearController`.

**Repositories go in a document folder each**, not loose in `repositories/academics/` — and note
that `identity` and `institution` still break this rule, so do not copy them.

**One warning from the plans module:** Spring Data resolves a custom fragment `XRepositoryImpl` by
**name and package**. Put it beside its interface, or the app compiles, starts, and fails only
when somebody calls the method.

---

# Appendix — what each field can hold

The entries above name the endpoints; this names the **values**. Stated once here rather than
repeated across 36 entries, so there is one place to correct when a set changes.

**Where a set is closed, it is an enum and the list is exhaustive** — anything else is a `400`
naming the field and listing what is accepted. Where it is open (`name`, every `DocsId`) the
column says so, because an open set is a thing a reviewer should notice.

Two things are left out of every entry because they are true of all of them:

- **The audit fields** — `createdAt`, `updatedAt`, `createdByDocsId`, `updatedByDocsId`,
  `version` — are filled in by Spring Data on every write. No endpoint sets them by hand.
- **`schoolId`** is on both documents and every query must carry it. It comes from
  `CurrentSchoolResolver`, never from the URL, on every endpoint here.

**Nothing below is written by any endpoint yet**, so unlike the plans appendix there is no bold
"written today" marking — the whole table is a plan.

## `academic_terms` — [AcademicTerm](../../../models/academics/structure/AcademicTerm.java)

| Field | Type | What can be in it |
|---|---|---|
| `academicYear` | String, required | `AcademicYear.name` — `"2026-2027"`. Comes from the `{year}` path segment, never from the body, and the year must exist. **Never changes**: a term cannot be moved to another year, because the two codes are only unique together. |
| `termCode` | String, required | Stable school-scoped key, unique with `schoolId + academicYear` — `TERM_1`, `SEM_2`, `Q3`. Derived from `name` the way `planCode` is: trimmed, uppercased, runs of non-alphanumerics to `_`. **Never changes.** |
| `name` | String, required | **Open** — free text, `@NotBlank`. `"Term 1"`, `"Semester 2"`, `"Annual"`. The only field a term is renamed by, and it is safe to rename because consumers store `termDocsId` and snapshot the name. |
| `sequence` | Integer, required | Order inside the year, **unique** with `schoolId + academicYear`. Not editable through #3 — see #4 and [open item 3](#3-term-weights-cannot-be-validated-one-patch-at-a-time). |
| `startDate` | LocalDate, required | Must fall inside the academic year's range, and must not overlap another term. First day of the period, inclusive. |
| `endDate` | LocalDate, required | Same, and must be on or after `startDate`. Last day, inclusive — so a term ending 30 September includes the 30th. |
| `weightPercent` | BigDecimal, `DECIMAL128`, optional | `0` to `100`, or null. **Null means this school does not weight the annual result**, and null on one term while another has a value is the mixed state to refuse. Active weights sum to 100 — checked by #2 and #4, reported by #3. |
| `resultsLocked` | Boolean, required | `false` at create; `true` from #5, `false` from #6. Narrower than `AcademicYear.resultsLocked`, which overrides it. |
| `active` | Boolean, required | `true` at create; `false` from #7, `true` from #8. The academic fact, not the record lifecycle — see [open item 4](#4-active-and-recordstate-are-two-flags-for-overlapping-things). |

## `school_classes` — [SchoolClass](../../../models/academics/structure/SchoolClass.java)

| Field | Type | What can be in it |
|---|---|---|
| `academicYear` | String, required | As above. From the path, never the body. **Never changes.** |
| `_id` | ObjectId | The identity, and what **12** other documents store as `classDocsId`. There is no code field — see [open item 1](#1-classcode-did-not-exist--settled-2026-09-10). |
| `name` | String, required | **Open** — `@NotBlank`, max 120, **unique within the year** (`school_year_class_name_uniq`). `"Grade 7"`, `"Nursery"`, `"XII Science"`. Editable through #13, and safe to edit because the id carries the identity, not this. |
| `affiliationProgrammeDocsId` | String, optional | `AffiliationProgramme.id`, or null. Checked by #12 and #13 with the tenant in the query. See [open item 2](#2-three-referenced-collections-had-no-repository--settled-2026-09-11). |
| ~~`displayOrder`~~ | — | **Removed 2026-09-11.** It went through three designs in two days — optional and repeatable, then required and unique as a dense 1..N — before being dropped entirely. A class now carries no school-defined position, and #28 orders by `name`. |
| `sections` | List, required | `[]` at create (#12). #17 adds one, #18 replaces the list. Rows below. |
| `subjects` | List, required | `[]` at create (#12). #22 adds one, #23 replaces the list. Rows below. |
| `active` | Boolean, required | `true` at create; `false` from #15, `true` from #16. |

## `school_classes.sections[]` — [ClassSection](../../../models/academics/structure/embedded/ClassSection.java)

| Field | Type | What can be in it |
|---|---|---|
| `sectionNo` | String, required | `@NotBlank`, unique inside the owning class. `"A"`, `"B"`, `"Blue"` — it is both the reference and the display value, so there is no separate name field and there must not be one. **Never changes**: [eight collections](#addressed-by-id-except-where-the-thing-has-no-id) store it. |
| `classTeacherDocsId` | String, optional | `Staff.id`, or null. Checked by #17 with the tenant in the query. Null means no class teacher assigned, which is a normal state before staff are onboarded. |
| `capacity` | Integer, optional | Planned student count, or null for no plan. **Not enforced anywhere** — refusing the 41st student belongs to the student module, where the count lives. `0` should be a `400`: a section nobody can be placed in is not a section. |
| `active` | Boolean, required | `true` at create; `false` from #20, `true` from #21. The only way to retire a section. |

## `school_classes.subjects[]` — [ClassSubject](../../../models/academics/structure/embedded/ClassSubject.java)

The row key is the **pair** `(subjectCode, sectionNo)`, not `subjectCode` alone.

| Field | Type | What can be in it |
|---|---|---|
| `subjectCode` | String, required | `@NotBlank`. `MATHEMATICS`, `SCIENCE`, `HINDI` — uppercased and normalised like the other codes. May repeat within one class **only** with a different `sectionNo`. A duplicate pair is a `400`. **Never changes**: seven collections store it. |
| `name` | String, required | **Open**, `@NotBlank`. `"Mathematics"`. Editable through #24. |
| `shortName` | String, optional | **Open**, or null. `"Maths"` — for a timetable cell or a report-card column, where the full name will not fit. |
| `subjectType` | [SubjectType](../../../models/academics/enums/SubjectType.java), required | `CORE` · `ELECTIVE` · `LANGUAGE` · `ACTIVITY` · `VOCATIONAL`. Five, exhaustive. **`ELECTIVE` is the one with a consequence**: it is precisely the case [open item 6](#6-the-upstream-gap--nothing-records-what-a-student-takes) cannot resolve, because nothing records which students chose it. |
| `sectionNo` | String, optional | An existing `sectionNo` of this class, or **null meaning every section**. Null is the ordinary case; a value is how one section gets its own teacher. Not editable — [see the exclusions](#things-this-module-deliberately-will-not-have). |
| `teacherDocsIds` | List of String, required | `[]` or `Staff.id` values. Each checked by #22 with the tenant in the query. `[]` is legitimate — a subject with no teacher yet. Replaced wholesale by #25, never appended to, so removing a teacher is the same call as adding one. |
| `gradingSchemeDocsId` | String, optional | `GradingScheme.id`, or null. Checked by #22 with the tenant in the query. Null falls through to `Exam.gradingSchemeDocsId` and then to none — the resolution order is in the model README. |
| `active` | Boolean, required | `true` at create; `false` from #26, `true` from #27. |

## The refusal codes this module introduces

Named here so two endpoints do not invent two codes for one condition — which is the mistake
`controllers/core` is currently carrying between `SCHOOL_NOT_ACTIVE` and `SCHOOL_NOT_EDITABLE`.

| Code | Status | When |
|---|---|---|
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | the `{year}` in the path is not a year of this school — reuses core's code and sentence |
| `CLASS_NOT_FOUND` | 404 | no class with that id in that year, or it belongs to another school |
| `TERM_NOT_FOUND` | 404 | no term with that `termCode` in that year |
| `SECTION_NOT_FOUND` | 404 | that class has no such `sectionNo` |
| `SUBJECT_NOT_FOUND` | 404 | that class has no such `(subjectCode, sectionNo)` |
| `CLASS_NAME_TAKEN` | 409 | that year already has a class with that `name` |
| `TERM_CODE_TAKEN` | 409 | that year already has that `termCode` |
| `SECTION_ALREADY_EXISTS` | 409 | that class already has that `sectionNo` |
| `SUBJECT_ALREADY_ASSIGNED` | 409 | that class already has that `(subjectCode, sectionNo)` pair |
| `TERM_SEQUENCE_TAKEN` | 409 | another active term in the year holds that `sequence` |
| `TERMS_OVERLAP` | 409 | the dates cover a day another term already covers |
| `TERM_OUTSIDE_ACADEMIC_YEAR` | 409 | the dates fall outside the year's own range |
| `INVALID_TERM_RANGE` | 400 | `endDate` before `startDate` |
| `TERM_WEIGHTS_INVALID` | 409 | a whole-set write (#2, #4) leaves active weights not summing to 100 |
| `TERM_WEIGHT_MIXED` | 409 | one active term carries a weight and another does not — added with #1. Distinct from the above: a wrong *total* is transient and only reported, a *mixture* computes to nothing and is refused |
| `TERM_CODE_INVALID` | 409 | the name has no letter or digit to derive a `termCode` from — added with #1, the term equivalent of `SUBJECT_CODE_INVALID` |
| `SECTION_STILL_REFERENCED` | 409 | a replace (#18) would drop a `sectionNo` something stores |
| `SUBJECT_STILL_REFERENCED` | 409 | a replace (#23) would drop a `subjectCode` something stores |
| `NOTHING_TO_UPDATE` | 400 | a `PATCH` body that asks for nothing — reuses core's code |
| `CLASS_NAME_REQUIRED` | 400 | `"name": ""` — a name cannot be removed, only replaced |
| `SOURCE_YEAR_EMPTY` | 409 | #35 or #36 asked to copy from a year with nothing in it |
| `TARGET_YEAR_NOT_EMPTY` | 409 | #35 or #36 would overwrite a structure that already exists |

**Every one of these is a 409 rather than a 403 where it is about state**, matching the rest of the
project: nothing here is about who the caller is.

---

# What every API touches, field by field

The same 36 endpoints, with the fields each one reads and each one writes. Written so that whoever
builds an endpoint does not have to work this out again from the models, and so a reviewer can see
at a glance whether a change reaches a field it should not.

Read **updates** as "changes an existing document", **insert** as "writes a new one", and
**reads** as "looks at it but does not change it".

**Four things are left out of every entry**, because they are true of all 36:

- **The audit fields** — `createdAt`, `updatedAt`, `createdByDocsId`, `updatedByDocsId`,
  `version` — filled in by Spring Data on every write.
- **`schoolId`**, on both documents and in every query, always from `CurrentSchoolResolver`.
- **The `{year}` check.** Every endpoint reads [`academic_years`](../../../models/core/AcademicYear.java)
  by `{schoolId, name}` first and answers `404 ACADEMIC_YEAR_NOT_FOUND`. Named per-entry only where
  the endpoint reads more of the year than its existence.
- **The gates.** Every **write** reads [`schools`](../../../models/core/School.java) and
  [`school_subscriptions`](../../../models/plans/SchoolSubscription.java) through gates 1 and 2, and
  the year's `isThisYearRunning` through gate 4. Reads run no gate at all.

**`sections[…]` and `subjects[…]` mean the one matched embedded row**, not the whole list. A write
to `sections[…]` is a positional update inside `school_classes`; the document written is always the
class, because a section has no document of its own.

## The terms — writes  ·  1–8

<a id="e1"></a>
**[1](#t1) · `POST /terms`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: the year as a **document**, for `startDate` and `endDate` — the term has to fall inside it, so existence alone is not enough here. That is what separates `AcademicTermServiceUtils.loadAcademicYear` from the classes side's `requireAcademicYear`.
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: the year's terms, **once**. A year holds two to four, so the whole set is loaded and every check below runs against that one snapshot rather than making four round trips that could each see a different state.
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *insert*: `schoolId`, `academicYear` = the `{year}` path segment, `termCode` derived from `name`, `name`, `sequence`, `startDate`, `endDate`, `weightPercent`, `resultsLocked` = `false`, `active` = `true`
- **Five rules MongoDB cannot express**, in this order: the range is not inverted (`400 INVALID_TERM_RANGE`), it falls inside the year (`409 TERM_OUTSIDE_ACADEMIC_YEAR`), the code is free (`409 TERM_CODE_TAKEN`), the sequence is free (`409 TERM_SEQUENCE_TAKEN`), the dates are free (`409 TERMS_OVERLAP`). The order matters: an inverted range checked last would be reported as "outside the year", which is true and says the wrong thing.
- **Two of those count retired terms and one does not.** A retired term keeps its `termCode` and its `sequence`, because neither unique index filters on `active` — a check that skipped retired rows would accept a write the database then refuses. It releases its **dates**, because nothing is taught in it and `school_year_term_active_dates_idx` is indexed on `active` for exactly that query.
- **`termCode` is derived from `name`, never accepted.** Trimmed, uppercased, runs of non-alphanumerics to `_` — the rule `planCode` uses. A name with nothing to derive from is `409 TERM_CODE_INVALID`. Accepting both would let a term named "Term 1" be coded `SEMESTER_2`, and the code is what six documents across three modules store.
- **The weight *mixture* is refused; the weight *total* is only reported.** `409 TERM_WEIGHT_MIXED` when one active term is weighted and another is not, because that computes to nothing and no sequence of edits passes through it legitimately. A total that is not 100 comes back as a `warning` on the response instead — [open item 3](#3-term-weights-cannot-be-validated-one-patch-at-a-time) settled that, because 20/80 → 30/70 passes through 110 and refusing it would make the values impossible to change.
- **Overlap is [`Dates.overlaps`](../../../common/time/Dates.java), shared with core's academic-year check.** The plan warned that two implementations would eventually disagree about touching endpoints; the predicate was extracted on 2026-09-11 and `CoreHelper.validateNoAcademicYearOverlap` now reads it too. Nothing had covered `ACADEMIC_YEAR_OVERLAP` before that, so `verify1.py` covers both callers.
- **`resultsLocked` and `active` are not accepted at create.** Both are events with their own endpoints — #5, #6, #7, #8 — and a term created already locked is a state nothing asked for.

<a id="e2"></a>
**[2](#t2) · `PUT /terms`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — containment, for every row in the body
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every existing row's `termCode`, so a code in the body is an edit and a code missing from it is a removal
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *insert*: the same fields as #1, for each `termCode` not already there
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `name`, `sequence`, `startDate`, `endDate`, `weightPercent` for each `termCode` that is
- **Writes nothing when a code would be dropped** — `409 SECTION_STILL_REFERENCED`'s term equivalent. The whole request is refused rather than partly applied; a half-replaced term structure is worse than none.
- **The only endpoint that can check the weight sum**, because it is the only one holding every row at once. `409 TERM_WEIGHTS_INVALID`.

<a id="e3"></a>
**[3](#t3) · `PATCH /terms/{termId}`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — only when dates are sent
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: the other active terms' `startDate`, `endDate` — only when dates are sent
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `name`, `startDate`, `endDate`, `weightPercent`
- **Never `termCode`** — six documents point at this term, and see [the rename table](#addressed-by-id-except-where-the-thing-has-no-id). **Never `sequence`** either: it is unique per year, so it moves through #4.
- **Reports a broken weight sum, does not refuse it** — see [open item 3](#3-term-weights-cannot-be-validated-one-patch-at-a-time). Refusing here makes 20/80 → 30/70 impossible.

<a id="e4"></a>
**[4](#t4) · `PUT /terms/order`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every term's `termCode` — the body must name all of them, or the ones left out keep numbers that collide
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `sequence`, on every term in the year
- **`school_year_term_sequence_uniq` is the reason this endpoint exists.** Swapping 1 and 2 by two `PATCH`es fails on the first. Even here the writes cannot go straight in: moving every term to a free range first, then to its target, is what keeps the unique index satisfied at every point in between. One transaction.

<a id="e5"></a>
**[5](#t5) · `POST /terms/{termId}/results/lock`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — already `true` is a `200` saying so, not a refusal
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `resultsLocked` = `true`
- **Does not touch `AcademicYear.resultsLocked`.** That is the stronger, wider control and this is the narrow one; see [open item 7](#7-academictermresultslocked-and-academicyearresultslocked-both-exist).

<a id="e6"></a>
**[6](#t6) · `POST /terms/{termId}/results/unlock`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked`
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `resultsLocked` = `false`
- **Records nothing about who unlocked, or why.** The same hole core's #27 carries, and the same answer: it needs a reason on the request and an `AuditEvent`, which needs an audit writer. Do not build this on real results without one.

<a id="e7"></a>
**[7](#t7) · `POST /terms/{termId}/deactivate`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `active`, and `weightPercent` — deactivating a weighted term changes what the remaining weights sum to
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `active` = `false`
- **Writes `active`, never `recordState`** — [open item 4](#4-active-and-recordstate-are-two-flags-for-overlapping-things).
- **Frees its `sequence` and its `termCode`?** No. Both stay on the row, and both stay taken: the unique indexes do not filter on `active`. A deactivated `TERM_1` means the year can never have another.

<a id="e8"></a>
**[8](#t8) · `POST /terms/{termId}/reactivate`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `active`, `sequence`, `startDate`, `endDate` — the year may have moved on since, so the overlap and sequence checks run again
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `active` = `true`

## The terms — reads  ·  9–11

<a id="e9"></a>
**[9](#t9) · `GET /terms`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`. No gate runs on a read, so this is what answers `404 ACADEMIC_YEAR_NOT_FOUND` — without it an unknown year would return an empty page, which reads as "this year has no terms".
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field, ordered by `sequence`. Served by `school_year_term_active_dates_idx` for the `?active=` case.
- **Paged — the plan said not to, and that was revisited on 2026-09-11.** The original reasoning was that a year holds two to four terms and a page cursor on a four-row list is machinery nobody uses. Nothing enforces two to four, though: a school running monthly reporting periods has twelve. The cost is one shared record and one shared factory that already existed for #28, and a client that handles every list in this API the same way is worth more than four rows saved.
- **Five filters, all optional, all AND-ed:** `?active=` · `?search=` · `?resultsLocked=` · `?weighted=` · `?coversDate=`
- **`?search=` matches `name` **or** `termCode`.** A person looking for a term types whichever they remember, and which one that is is not something this endpoint gets to decide. The needle is `Pattern.quote`d, so a stray `(` is an empty result rather than a 500.
- **`?weighted=` asks `exists`, not `ne: null`** — a term written before the field existed has no key at all and must read as unweighted. The same reason #28 asks `sections.0` rather than a stored count.
- **`?coversDate=` is the question [#10](#e10) asks only about *today*.** Both ends inclusive, so the last day of a term is inside it. No time zone is involved, because the caller names the date rather than the server deciding what "today" means — which is the part #10 has to get right and this does not.
- **Sorted by `sequence`, which is also the tiebreaker on every other sort** — `?sort=name` is really `name, sequence`. That is not this endpoint's doing: [`PageResponse.pageableOf`](../../../common/web/PageResponse.java) appends the fallback order to whatever the caller named, minus any key they already used, and [#28](#e28) gets the same treatment from its own `name` fallback.
- **Which makes the choice of fallback the decision that matters.** `sequence` is unique within a year — #1 enforces it and `school_year_term_sequence_uniq` declares it — so every sort ends in a total order and paging cannot put one row on two pages while another appears on none. A term `name` could not have served: only `termCode` and `sequence` are unique. **A hand-written `withStableOrder` was added here first and deleted the same day**, once a mutation removing it changed nothing — the shared helper already did it.
- **`?sort=` is an allowlist** — `sequence`, `name`, `startDate`, `endDate`, `createdAt`, `updatedAt`. Anything else is `400 INVALID_SORT_FIELD` listing what is allowed, because an arbitrary field name reaching a Mongo sort is how a caller makes the database read every row to answer.
- **The page is validated before the year is read**, so a malformed `?page=` costs no round trip — and reports the page rather than the year.

<a id="e10"></a>
**[10](#t10) · `GET /terms/current`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `startDate`, `endDate`, and every field of the one that matches
- **"Today" is today in the school's zone**, through `Dates.todayIn(schoolZone.current())`. `LocalDate.now()` here is the exact bug core had in three places and fixed on 2026-09-10.
- **`404` is a real answer**, not an error to design around: a school between terms is genuinely in no term. Say which two it falls between.

<a id="e11"></a>
**[11](#t11) · `GET /terms/{termId}`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field. `404 TERM_NOT_FOUND` when the year has no term by that code.

## The classes — writes  ·  12–16

<a id="e12"></a>
**[12](#t12) · `POST /classes`** — built

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name` for uniqueness in the year. Served by `school_year_class_name_uniq`, which **replaced** the index this plan was blocked on; see [open item 1](#1-classcode-did-not-exist--settled-2026-09-10).
- [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) — *reads*: existence and `schoolId` — `404 AFFILIATION_PROGRAMME_NOT_FOUND`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *insert*: `academicYear` = the `{year}` path segment, `name` trimmed, `affiliationProgrammeDocsId`, `sections` = `[]`, `subjects` = `[]`, `active` = `true`
- [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) — *reads*: existence **and `schoolId`**, only when the field is sent. `AffiliationProgrammeRepository` was created with this endpoint; the id is real but another school's is a `404`.
- **`sections` and `subjects` are written as empty arrays, not left absent.** The response reports `0` either way — its counts are null-safe — so only the stored document shows the difference, and a mutation test is the only thing that catches it.
- **The service's own year check is unreachable through HTTP.** Gate 4 loads the same year and throws the same `404 ACADEMIC_YEAR_NOT_FOUND` first. It stays because #35 and #36 will call the service with a year no gate saw.
- **`sections` and `subjects` are not accepted from the caller.** Both start empty and #17 and #22 fill them — the shape an academic year already uses for holidays and a plan for features.

<a id="e13"></a>
**[13](#t13) · `PATCH /classes/{id}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`. Unreachable through HTTP, like #12's — gate 4 answers first — and kept because **without it the two endpoints disagree**: a bad year gives #12 an `ACADEMIC_YEAR_NOT_FOUND` and #13 a `CLASS_NOT_FOUND`, which is true but says the wrong thing about what is wrong. Found by removing gate 4 and watching the codes diverge.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class by `{_id, schoolId, academicYear}` — all three, because the id alone is globally unique and would otherwise find another school's class, or last year's
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class holding the new `name` in that year, when a rename is sent
- [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) — *reads*: existence **and `schoolId`**, only when the field is sent and non-blank
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `name`, `affiliationProgrammeDocsId` — whichever were sent
- **The `name` is editable, and this endpoint is why the id-addressed design matters.** Had the URL kept a code derived from the name, or had `name` become the join key, this request would have one field left. Nothing joins on either, so both are free to change — the name only has to stay unique in the year, else `409 CLASS_NAME_TAKEN`.
- **A class may keep its own name.** The check compares **ids**, not names, so `{"name": "Grade 7"}` on the class already called Grade 7 is not a conflict with itself. Using the cheaper `exists` here — which #12 does use — would have refused every request that resent an unchanged name.
- **What can be cleared is not symmetric, and JSON is the reason:**

  ```
  "affiliationProgrammeDocsId": ""     clears it
  "affiliationProgrammeDocsId": null   leaves it       (same as absent)
  "name": ""                           400 CLASS_NAME_REQUIRED
  ```

  **There is no ordering field.** `displayOrder` was removed on 2026-09-11, so a class has no position to set or clear.
- **A blank name is refused rather than treated as a clear**, matching `HOLIDAY_NAME_REQUIRED` in the core module: a required field sent empty is a client bug, and silently keeping the old value hides it.
- **Nothing structural is reachable.** `sections`, `subjects` and `active` are not on the request, so sending them does nothing. An edit that could replace forty embedded rows while looking like a rename is the shape this avoids; `active` is #15 and #16.
- **An empty body is `400 NOTHING_TO_UPDATE`**, not a 200. A `PATCH` that changes nothing and reports success lets a client with a broken form look healthy.

<a id="e14"></a>
**[14](#t14) · `PUT /classes/order`**


<a id="e15"></a>
**[15](#t15) · `POST /classes/{id}/deactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `active` = `false`
- **Leaves every `sections[].active` and `subjects[].active` alone.** Arguably they are implied, but a write that changed forty flags when asked to change one is worse than a second call — the same rule core's `POST .../end` follows.

<a id="e16"></a>
**[16](#t16) · `POST /classes/{id}/reactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `active` = `true`

## The sections inside a class  ·  17–21

Every write here is a positional update on **`school_classes`**. A section is embedded, so there is
no section document to write — the document saved is always its class.

<a id="e17"></a>
**[17](#t17) · `POST /classes/{id}/sections`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`. Unreachable through HTTP as on #12 and #13, and kept for the same reason.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class by `{_id, schoolId, academicYear}`, then `sections[].sectionNo` — the new one must not already be there, `active` or not
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: existence **and `schoolId`** of `classTeacherDocsId`, when one is sent. `StaffRepository` was created with this endpoint — the second of the three the README listed as missing — so another school's *real* staff id is a `404` rather than silently accepted as this class's teacher.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: appends to `sections[]` — `sectionNo` trimmed, `classTeacherDocsId`, `capacity`, `active` = `true`
- **The endpoint the `student` module is waiting for.** `StudentAcademicRecord.sectionNo` has nothing to point at until this runs.
- **A duplicate is `409 SECTION_ALREADY_EXISTS`, checked in the service, and that check is the only guard there is.** Mongo cannot enforce uniqueness *inside* an array, so unlike a class name there is no index to fall back on if the check is ever removed — a mutation test that deletes it is the only thing that notices.
- **Checked case-insensitively, stored as typed.** "A" and "a" in one class is a typo every time, not two sections, and the two would be indistinguishable on screen. But `sectionNo` is the display value as well as the reference, so what a school typed is what is kept — a school naming its sections "Blue" and "Red" is not making a mistake, and no shape is imposed.
- **The document written is the class.** A section is embedded: no collection, no id, no `schoolId` of its own. Which is also why `sectionNo` can never change — eight collections store it as a plain string and there is no id for them to reference instead.
- **The response is the class's whole section list**, not the one row added — the same shape every calendar endpoint in `core` returns for a holiday, and what #30 will return for a read. `activeCount` sits beside `sectionCount` because a retired section still holds its number and still appears.
- **`capacity` is a plan, not a limit.** Nothing enforces it; this module cannot count students. `0` is a `400` — a section nobody can be placed in is not a section — and absent means no plan was recorded, which is different from a plan of zero.
- **Nothing else about the class moves.** Name, subjects and `active` are untouched, and there is a test that says so.

<a id="e18"></a>
**[18](#t18) · `PUT /classes/{id}/sections`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every existing `sections[].sectionNo`, to tell an edit from an addition from a removal
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: every `classTeacherDocsId` in the body — `StaffRepository` exists, so this is checkable
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: replaces `sections[]` wholesale
- **Refuses to drop a `sectionNo` that is not in the body** — `409 SECTION_STILL_REFERENCED`. Eight collections may store it and none of them can be checked cheaply, so a replace is add-and-edit only; removals go through #20. [Open item 5](#5-put-on-an-embedded-list-can-silently-drop-a-referenced-key).
- **`active` is preserved, not reset.** A row already deactivated stays deactivated unless the body says otherwise, or a setup replace would silently switch retired sections back on.

<a id="e19"></a>
**[19](#t19) · `PATCH /classes/{id}/sections/{sectionNo}`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo` — to find the row, `404 SECTION_NOT_FOUND` otherwise
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `classTeacherDocsId` when sent — `StaffRepository` exists, so this is checkable
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].classTeacherDocsId`, `sections[…].capacity`
- **Never `sections[…].sectionNo`.** It is both the reference and the display value, which is precisely why there is no rename: there is no separate label to change instead.
- **`capacity` is not checked against anything.** Lowering it below the number of students already placed is allowed, because this module cannot count students. The count lives in `student`, and so does the refusal.

<a id="e20"></a>
**[20](#t20) · `POST /classes/{id}/sections/{sectionNo}/deactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo`, `sections[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].active` = `false`
- **This is the only way to retire a section**, and the whole reason there is no `DELETE`.
- **Leaves `subjects[]` alone**, including the per-section assignments pointing at this `sectionNo`. They are still true — that section was taught that subject — and #26 retires them if a school wants them gone.

<a id="e21"></a>
**[21](#t21) · `POST /classes/{id}/sections/{sectionNo}/reactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo`, `sections[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].active` = `true`

## The subjects taught in a class  ·  22–27

The row key is the **pair** `(subjectCode, sectionNo)`. Every endpoint naming one assignment takes
`?sectionNo=`; **omitting it means the class-wide row**, where `sectionNo` is `null` — not "any row".
That distinction is the whole reason the parameter exists, and it is the same shape as core's
`PATCH .../holidays/{date}?type=`.

**Moving `subjects` onto `ClassSection` was considered and rejected — 2026-09-10.** The nullable
`sectionNo` is awkward, and the two notes below say why: `?sectionNo=` reads differently here than
in #31, and nothing defined which row wins when a class-wide and a per-section row both exist. A
subject list on each section removes both. **One objection is settled and the other is now
deliberate** — #22 forbids the mixture outright, so precedence can never arise; and #31's
`?sectionNo=` means *audience* where #24's means *key*, which is documented at both ends rather
than designed away. The rejection stands: only `teacherDocsIds` actually varies by section. It was not taken because only **one** field on
`ClassSubject` — `teacherDocsIds` — actually varies by section, so four sections × ten subjects
would store the same `name`, `shortName`, `subjectType` and `gradingSchemeDocsId` four times, and
renaming a subject would become four edits inside one document that can drift apart. A split —
subject identity on the class, teachers on the section — avoids both, and was also not taken.
**The model stands as it is**, so the two flaws below are known and accepted, and the service check
in #22 is what keeps them from becoming data problems.

<a id="e22"></a>
**[22](#t22) · `POST /classes/{id}/subjects`** — built

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `subjects[].subjectCode` with `subjects[].sectionNo` — the pair must be free, `409 SUBJECT_ALREADY_ASSIGNED` otherwise
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo` — a non-null `sectionNo` in the body has to be a section this class actually has
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: every `teacherDocsIds` entry, each with `schoolId` in the query — `404 STAFF_NOT_FOUND`. A repeat is `400 DUPLICATE_TEACHER`, and it fires *after* existence, so an unknown id sent twice is the 404
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradingSchemeDocsId` with `schoolId` — `404 GRADING_SCHEME_NOT_FOUND`. `GradingSchemeRepository` was built here, closing [open item 2](#2-three-referenced-collections-had-no-repository--settled-2026-09-11)
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: appends to `subjects[]` — `subjectCode`, `name`, `shortName`, `subjectType`, `sectionNo`, `teacherDocsIds`, `gradingSchemeDocsId`, `active` = `true`
- **Class-wide OR per-section, never both — settled here.** `MATHEMATICS` with `sectionNo: null` alongside `MATHEMATICS` with `sectionNo: "A"` is `409 SUBJECT_ASSIGNMENT_CONFLICT`, refused in *both* directions. A section studies its own rows **plus** the class's — as `SectionDetail.jsx` does, and as every
future consumer must — so the mixture gives that section the subject twice with no precedence. The model README's "repeat the same subjectCode with each section code" means *each* section, not one beside a class-wide row. Relaxing this needs a precedence rule, not a deletion.
- **`subjectCode` is normalised, `sectionNo` is not.** Uppercased with every run of non-alphanumerics collapsed to one underscore, so `maths-2` stores as `MATHS_2`; a code with nothing left is `409 SUBJECT_CODE_INVALID`, which `@NotBlank` cannot catch because the input was not blank. `sectionNo` is matched case-insensitively and stored **as the class spells it** — two spellings would read as two sections.

<a id="e23"></a>
**[23](#t23) · `PUT /classes/{id}/subjects`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every existing `(subjectCode, sectionNo)` pair, and `sections[].sectionNo`
- [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: every referenced id — both repositories exist, so these are checkable
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: replaces `subjects[]` wholesale
- **Refuses to drop a `subjectCode`** — `409 SUBJECT_STILL_REFERENCED`, same rule as #18. Seven collections store it.
- **The only endpoint that sees the whole list**, so the only one that can catch a duplicate pair *within the body* rather than against what is stored.

<a id="e24"></a>
**[24](#t24) · `PATCH /classes/{id}/subjects/{subjectCode}?sectionNo=`** — built

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `404 SUBJECT_NOT_FOUND` otherwise
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradingSchemeDocsId` with `schoolId` in the query when a value is sent — `404 GRADING_SCHEME_NOT_FOUND`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].name`, `subjects[…].shortName`, `subjects[…].subjectType`, `subjects[…].gradingSchemeDocsId`
- **Never `subjectCode` and never `sectionNo`** — both are the key. Moving an assignment to another section is #26 on the old row plus #22 on the new one, and the history reads correctly that way. Neither has a field in the request record, so sending one is ignored rather than refused — the ordinary shape for a `PATCH` body.
- **Not `teacherDocsIds` either** — that is #25, because it replaces a list rather than setting a field. Nor `active`: that is #26 and #27, because retiring a subject is an event rather than a flag.
- **The section half of the key is a query parameter, not a path segment**, because it is usually absent: a class-wide subject has no section, and the ordinary case should not need an empty segment. `?sectionNo=` blank reads the same as leaving it off.
- **The code in the path is normalised the way #22 stored it**, so the URL that created `maths-2` finds `MATHS_2`. Without that the endpoint would refuse the spelling its own create call accepted.
- **A 404 says which way round the subject actually is.** #22 forbids a subject being both class-wide and per-section, so at most one exists — asking for the wrong one gets "drop `?sectionNo=`" or "use `?sectionNo=A`" rather than a bare not-found, which is true and useless.
- **`""` clears `shortName` and `gradingSchemeDocsId`; `""` on `name` is `400 SUBJECT_NAME_REQUIRED`.** Clearing the scheme is not "no grading" — the resolution order falls through to `Exam.gradingSchemeDocsId`.
- **An empty body is `400 NOTHING_TO_UPDATE`**, not a 200, so a client with a broken form finds out. Same rule as #13.

<a id="e25"></a>
**[25](#t25) · `PUT /classes/{id}/subjects/{subjectCode}/teachers?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: existence and `schoolId` of every id in the body — the most consequential of the three, because this is the field that decides who can enter marks
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].teacherDocsIds`, replaced whole
- **A `PUT` of the whole list, not add and remove.** Two endpoints for one array is two ways to end up with a duplicate id in it. `[]` is a legitimate body: a subject with no teacher assigned yet.
- **Changes nothing already recorded.** Marks and registers store `subjectCode`, not the teacher, so replacing this list does not rewrite history — which is why it can be a plain `PUT` and not an event.

<a id="e26"></a>
**[26](#t26) · `POST /classes/{id}/subjects/{subjectCode}/deactivate?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `subjects[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].active` = `false`

<a id="e27"></a>
**[27](#t27) · `POST /classes/{id}/subjects/{subjectCode}/reactivate?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `subjects[…].active`, and `sections[].sectionNo` — the section it belongs to may have been retired since
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].active` = `true`

## The reads  ·  28–34

None of these runs a gate, and none of them writes anything.

<a id="e28"></a>
**[28](#t28) · `GET /classes`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`. **This is the one endpoint where that check actually fires**: no gate runs on a read, so unlike #12 and #13 — where gate 4 answers first and their own checks are unreachable — this is what returns `404 ACADEMIC_YEAR_NOT_FOUND`.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `_id`, `name`, `active`, `affiliationProgrammeDocsId`, and counts off `sections[]` and `subjects[]`
- **Filters, all optional and all AND-ed:** `?active=` · `?search=` (name, case-insensitive, contains) · `?affiliationProgrammeDocsId=` · `?hasSections=` · `?hasSubjects=`
- **`?hasSections=false` is the setup checklist**, and the reason that filter exists: a class with no section cannot hold a student, because `StudentAcademicRecord` stores `sectionNo`. "What have I not finished setting up" is the question a school asks during year setup, and nothing else on this endpoint answers it. Asked of `sections.0` rather than a stored count, so there is no second field to keep in step with the list.
- **Sorted by `name`, with no tiebreaker — and that is safe only because `name` is unique within the year.** `displayOrder` was removed on 2026-09-11, so there is no school-defined order to sort by. **Alphabetical is a real downgrade:** "Grade 10" sorts before "Grade 2", and "Nursery, LKG, UKG, 1, 2, 3" cannot be expressed at all. What it buys is a total order that `school_year_class_name_uniq` serves, so no blocking in-memory sort.
- **Does not return the embedded lists**, only their sizes. A year of twelve classes with four sections and ten subjects each is 168 embedded rows in one response nobody reads. #29 is for one class in full.
- **Refused, never clamped:** `?size=101` is a `400 INVALID_PAGE_SIZE`. A caller who asked for 5000 rows and silently got 100 has been handed a page they will read as the whole answer.
- **A year with no classes is an empty page; an unknown year is a 404.** Those are different answers, and a school acting on the first would wait for classes that can never appear.
- **No `@Transactional` and no gates.** A suspended school can still read its structure and cannot write to it — that asymmetry has a test.

#### What the indexes actually do — measured, and it changed twice in two days

```
bare list            IXSCAN school_year_class_name_uniq   index-ordered
?active=true         IXSCAN school_year_class_active_idx  + SORT
?search=grade        IXSCAN school_year_class_name_uniq   index-ordered
```

**No index was added for #28.** The filter is always an index scan, never a COLLSCAN, because
`schoolId` and `academicYear` are pinned and every index begins with that pair. The default order
is `name`, which `school_year_class_name_uniq` already serves — so the ordinary list sorts from
the index.

**The history is worth keeping, because the same query was measured three ways.** While
`displayOrder` was optional and repeatable, the order needed an `_id` tiebreaker that no index
carried, so every list did a blocking sort. Making the field unique removed the tiebreaker and an
index served the order. Removing the field moved that job to `name`, which was unique all along —
so the outcome is the same as the middle state, reached by deleting a field rather than
constraining one.

`?active=` is the one case that still sorts in memory: `school_year_class_active_idx` ends at
`active`, so it filters from the index and orders afterwards. Tens of documents per year, and the
alternative is a fourth index on a collection that already has six.

`?search=` and `?affiliationProgrammeDocsId=` have nothing of their own and want nothing: both
are applied after the query is pinned to one school and one year, and a case-insensitive
*contains* regex cannot use an index in any case.

<a id="e29"></a>
**[29](#t29) · `GET /classes/{id}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`. No gate runs on a read, so this is the check that answers `404 ACADEMIC_YEAR_NOT_FOUND`.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class by `{_id, schoolId, academicYear}` — its own fields, plus `sections[]` and `subjects[]` **to count them**
- **The counts are here; the rows are not — trimmed 2026-09-11.** This returned both arrays in full until #30, #31 and #37 each owned one. Three responses carrying the same embedded rows is three places to keep in step, and the first client reading a section from here would depend on a shape this endpoint has no claim on.
- **The counts stayed, and they are the reason this endpoint still exists.** "3 sections, 3 subjects" is what a class page header shows, and making a caller fetch two lists to count them is exactly the call this saves. They are also the only **derived** thing in the response — every other field is a column on the document.
- **Four counts, not two.** Active and total differ for both: a retired section keeps its `sectionNo` and still counts, because records reference it — but it is not one a student can be placed in. A class with four sections and none active would otherwise look ready.
- **`affiliationProgrammeDocsId` is the one field no other endpoint returns**, and it comes back as a raw id. Resolving it is possible and deliberately not done — one place should decide how a programme is presented.
- **Why this is still not #28's `SchoolClassResponse`.** The two now carry nearly the same fields, and the difference is that one field, which a page of rows has no use for. Kept separate because #28 returns many and this returns one; a record shared by both grows a field for whichever caller needs it next.
- **One document, one query, no joins** — still true, and still why sections and subjects are embedded rather than collections. It is what lets #30, #31 and #37 each be a single lookup rather than a join.
- **A class with nothing in it is a `200` with four zero counts**, never a 404 — a class created by #12 and not yet filled by #17 and #22.

<a id="e30"></a>
**[30](#t30) · `GET /classes/{id}/sections`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of the `{year}`, as #29 does
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the same document #29 reads, returning only `sections[]`
- **The document read is identical to #29's.** A section is embedded, so there is nothing cheaper to fetch — what this saves is the *response*, which is a tenth of the size, and that is the part that crosses the network. A "move this student" dropdown wants four fields per section, not a class with every subject assignment behind it.
- **`?active=true` is what that dropdown sends.** A retired section still holds its `sectionNo` and still appears unfiltered, because records reference it, but nobody should be placed in one. `?active=false` gives the retired ones; **absent is not the same as `false`**.
- **The counts describe the whole class, not the filtered view.** `sectionCount` answers "how many does this class have", which does not change because a caller asked to see some of them — a filtered count would make `?active=true` on a class with two retired sections report two sections and two active, a lie in both halves.
- **It shares `SectionView` with #29 and with #17's response**, so a section has one shape across every endpoint that returns one. It was nested inside `SectionListResponse` until #29 needed it; a second copy would have been two shapes for one thing.
- **A class with no sections is an empty list**, never a 404 — and while #17 is the only section write built, that is the state most classes are in.
- **`forRead(...)` rather than a third `fromSchoolClass` overload.** Two overloads differing only in a nullable second argument were ambiguous to the compiler — `fromSchoolClass(cls, null)` matched both — and would have been ambiguous to a reader. The name says which half of the API is calling: writes pass a summary, reads pass a filter.

<a id="e31"></a>
**[31](#t31) · `GET /classes/{id}/subjects?sectionNo=`** — built

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `subjects[]`, optionally narrowed to one section's audience
- **`?sectionNo=` names an audience here, not a row.** Omitted or blank, the answer is every assignment the class holds. Given, the answer is what that section is taught: **its own rows *and* the class-wide ones**, because a row with no `sectionNo` applies to every section.
- **Matching strictly would have made it useless.** In a class where only the languages are split by section, a strict `sectionNo == "A"` answer hides maths, science and everything else section A actually studies. Measured on the test fixture: strict returns 2 of the 4 rows section A is taught.
- **A class-wide row is still identifiable** — it comes back with no `sectionNo` field at all, so a caller that genuinely wants the strict set can filter in one line. The union is the default because it is the answer to the question asked.
- **This is the one place `?sectionNo=` does not mean what it means on [#24](#e24)**, where it is half of a row's key. Same word, two jobs: there it asks *which row*, here it asks *taught to whom*. Documented at both ends rather than renamed, because "which section" is the honest reading of both.
- **An unknown section is `404 SECTION_NOT_FOUND`, not an empty-ish 200.** Without that check a typo answers with just the class-wide rows — which looks exactly like a real section that has nothing of its own, and there are real sections like that. `?sectionNo=Z` must not be indistinguishable from `?sectionNo=C`.
- **The counts describe the whole class, not the slice** — `subjectCount` stays 5 while 2 rows come back. The same rule #30 follows: "how many does this class teach" is not "how many did you ask to see".
- **It reads the same document as #29 and returns the same rows**, asserted directly in `verify31.py`. That is not a defect — it is why the response shape is shared. What it owns is the question, not the query.
- **It does not answer "which subjects does this student take."** Nothing does; see [open item 6](#6-the-upstream-gap--nothing-records-what-a-student-takes). For an `ELECTIVE` row this list is what is *offered*, not what is taken.
- **Dropped 2026-09-11 and rebuilt the same day.** The drop was right on the evidence then: as specified it was "just the subject assignments", which is #29 with fields removed — same document, same single query, ~430 bytes saved on the largest class in the database. What brought it back was fixing the specification, not the measurement: the endpoint's value was never the projection, it was owning the union rule so that every caller does not reimplement it. The history is kept here because "we built a read that saves 430 bytes" and "we built the one place that knows what a section studies" are different decisions, and only the second one is defensible.

<a id="e32"></a>
**[32](#t32) · `GET /subjects`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `subjects[].subjectCode`, `subjects[].name`, `subjects[].subjectType`, `subjects[].active`, and the `classCode` of each class teaching it
- Answers "do we teach Sanskrit at all, and where", which no per-class read can. An aggregation across the year's classes, served by `school_year_subject_code_idx`.
- **`name` may disagree between classes.** `subjectCode` is per-class, so `MATHEMATICS` can be "Mathematics" in Grade 7 and "Maths" in Grade 8 — nothing forbids it. Return the distinct names rather than picking one, or the response quietly hides a data problem a school would want to fix.

<a id="e33"></a>
**[33](#t33) · `GET /staff/{staffDocsId}/teaching`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].classTeacherDocsId` and `subjects[].teacherDocsIds`, returning the `classCode`, `sectionNo` and `subjectCode` of every match
- **Two indexes already exist for exactly this**: `school_year_class_teacher_idx` and `school_year_subject_teacher_idx`. Their presence on a model with no endpoints is the clearest signal in the module of what was intended.
- **Two questions in one response**, kept separate: the sections this person is *class teacher* of, and the subjects they *teach*. They are different relationships and a flat list of both would be unreadable.
- **Checks the staff id exists** — `StaffRepository` arrived with #17 — so an unknown id is a `404` rather than two empty lists. Otherwise "no assignments" and "no such person" look identical.

<a id="e34"></a>
**[34](#t34) · `GET /structure`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field of every term, in `sequence` order
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every field of every class, `sections[]` and `subjects[]` included, in `displayOrder`
- The one-call bootstrap: what an app fetches on login instead of #9, #28 and #29 twelve times.
- **This is the response that gets big** — 168 embedded rows for a twelve-class school, and it is the only endpoint here with no filter and no page. That is deliberate for a bootstrap, and it is the endpoint to check first if the app feels slow. `?active=true` should be the default rather than an option.

## Rolling the structure into the next year  ·  35–36

Both read one year and write another, so both name **two** `{year}` values and must resolve both.

<a id="e37"></a>
**[37](#t37) · `GET /classes/{id}/sections/{sectionNo}`** — built

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the one `sections[]` row that number names, `404 SECTION_NOT_FOUND` otherwise
- **The only response in this package that is not a list.** #30 answers "which sections does this class have"; this answers "this section". A caller wanting one used to read all of them and pick in its own code — the same duplication [#31](#e31) was built to end for subjects.
- **`sectionNo` is a path segment, not a query parameter**, because it names the thing being fetched rather than filtering something else. Compare #30, where the list *is* the resource, and #31, where `?sectionNo=` narrows an audience. Three endpoints, three jobs for one word, each in the position that matches its job.
- **Matched case-insensitively and trimmed**, because "a" and "A" are one section everywhere else in this module and a URL is not where that should start to differ.
- **The class is carried around the section** — `className`, `academicYear`, `schoolClassId`. A section is embedded and has no identity away from its class, so a response holding only `sectionNo` would name something that means nothing on its own.
- **The counts describe the class, not the section** — `sectionCount` and `activeCount` are the class's, the same rule #30 and #31 follow. "One of three" is what a page shows beside a section's name.
- **The subjects are not here.** That is #31 with `?sectionNo=`, which applies the class-wide union — a rule this response has no business restating, and would drift from if it did.
- **It returns the identical row #30 does**, asserted field-for-field in `verify37.py`. The shape is shared on purpose; what differs is the question.

<a id="e35"></a>
**[35](#t35) · `POST /terms/copy-from/{sourceYear}`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` of **both** years — the source, to measure each term's offset from its start; the target, to check the shifted dates land inside it
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every active term of `{sourceYear}`. `409 SOURCE_YEAR_EMPTY` when there are none.
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: whether the target year already has terms. `409 TARGET_YEAR_NOT_EMPTY` — this endpoint adds to nothing, so it refuses rather than merging.
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *insert*: one row per source term — `termCode`, `name`, `sequence`, `weightPercent` copied as they are; `startDate` and `endDate` **shifted**; `resultsLocked` = `false` and `active` = `true` reset, never copied
- **Shift by the offset from the year's start, not by 365 days.** A term starting on day 0 of a year starts on day 0 of the next one. Adding a year to the date instead drifts whenever the two years are different lengths, and leap years make that a certainty rather than an edge case.
- **`resultsLocked` is reset on purpose.** Copying a lock forward would open a new year with results frozen and nothing that looks like a cause.

<a id="e36"></a>
**[36](#t36) · `POST /classes/copy-from/{sourceYear}`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: existence of both years
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every active class of `{sourceYear}` with its `sections[]` and `subjects[]`. `409 SOURCE_YEAR_EMPTY` when there are none, `409 TARGET_YEAR_NOT_EMPTY` when the target already has classes.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *insert*: one document per source class, **each with a new id** — `name`, `affiliationProgrammeDocsId` copied; `sections[]` copied with `sectionNo` and `capacity`; `subjects[]` copied with `subjectCode`, `name`, `shortName`, `subjectType`, `sectionNo`, `gradingSchemeDocsId`; every `active` reset to `true`
- **New ids, so nothing that referenced last year's class now points at this year's.** That is the whole reason a copy is safe: `classDocsId` on a report card still resolves to the year it was issued in.
- **`classTeacherDocsId` and `teacherDocsIds` are dropped unless `includeTeachers` is `true`, and the default is `false`.** This is the one real decision in the endpoint. Copying them silently assigns staff who may have resigned, and a teacher who left in March would be class teacher of a section in June with nobody having said so. Dropping them leaves a school re-assigning teachers — which it was going to do anyway, because that is what changes between years.
- **Copies only `active` rows**, so last year's retired sections do not come back to life in a fresh year.
- **Not a link, a copy.** The new year's documents are independent from the moment they are written; editing Grade 7 in 2027-2028 does not touch 2026-2027. That is what makes the per-year model safe, and it is also why nothing keeps the two in step afterwards.
