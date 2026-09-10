# controllers/academics/structure — API plan

**None of the 36 are built.** This is the full set of endpoints the academic-structure feature
needs, written before any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../../core/README.md) and [`controllers/plans`](../../plans/README.md) were
done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/academics/structure`](../../../models/academics/structure), whose README already
describes the two documents, the two embedded types and the reference rules. **These endpoints
enforce that file. They do not invent new rules** — with one exception that file itself asks for,
the missing `classCode`; see [To settle before building](#to-settle-before-building).

> **One thing blocks every write below.** `SchoolClass` has a unique index on a field it does not
> declare, which allows exactly **one class per school per academic year**. Nothing here can be
> built until that is settled. It is the first item in
> [To settle before building](#to-settle-before-building), and it is not a big change.

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
        "GRADE_7"  displayOrder 7
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
school_year_class_code_uniq   {schoolId, academicYear, classCode}   unique
school_year_term_code_uniq    {schoolId, academicYear, termCode}    unique
```

So `/schools/current/classes/GRADE_7` names nothing — every school has a `GRADE_7` in every year
it has ever run. The year has to be in the URL for the rest of the path to identify a document,
and putting it in a query parameter instead would mean a `PATCH` whose target is half in the path
and half in the query.

```text
/schools/current/academic-years/2026-2027/classes/GRADE_7/sections/A
                               ^^^^^^^^^         ^^^^^^^          ^
                               parent key        the class    the section
```

Long, and correct. Each segment names one real thing, and the whole path is the document plus the
two keys inside it.

### Addressed by code, never by id

Same rule the academic year established: `sectionNo`, `subjectCode`, `termCode` and `classCode`
are what other collections store, so they are what the URL says. Using ids in the URL would mean
one vocabulary in the database and a different one in the API.

**And there is no rename endpoint for any of the four codes.** The counts are the reason:

| Key | Stored as a plain string by | A rename would |
|---|---|---|
| `sectionNo` | **8** collections — `attendance_sessions`, `exam_schedules`, `homework`, `report_cards`, `holistic_progress_cards`, `student_academic_records`, `fee_invoices` directly, and `daily_timetables` through its embedded `TimetableEntry` | leave all eight pointing at a section that no longer answers to it |
| `subjectCode` | **7** — `attendance_sessions`, `curriculum_documents`, `homework`, `student_marks`, `exam_schedules` directly, plus `report_cards` and `daily_timetables` through embedded rows | orphan every mark and register for that subject |
| `termCode` | referenced by **id**, so a rename is safe — which is exactly why `AcademicTerm` is a collection and not a string. See its model README. | be safe, and is still not offered: `name` is the display field, so nothing needs it |
| `classCode` | does not exist yet | — |

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

Numbered straight through, 1 to 36. Grouped only so the list is readable. Every path below is
relative to **`/schools/current/academic-years/{year}`**, which is left off the table to keep it
readable — so `POST /terms` is `POST /schools/current/academic-years/2026-2027/terms`.

## 1. The terms — writes · [Build order ↓](#build-order)

A term is the unit a report card is issued for. Six other documents point at one by
`termDocsId`, across three modules — `Exam`, `ReportCard`, `HolisticProgressCard`,
`FeedbackCampaign`, `FeeInstallment` and `FeeInvoice`.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>1 | [`POST /terms`](#e1) | Add one reporting period to the year. The ordinary way a term is created once the year is running and somebody realises a period is missing. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t2"></a>2 | [`PUT /terms`](#e2) | Set the year's whole term structure in one write — "two semesters", "four quarters". What year setup actually does, and the only endpoint that can validate the weights sum to 100, because it is the only one that sees all of them. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t3"></a>3 | [`PATCH /terms/{termCode}`](#e3) | Fix one term's name, dates or weight. Cannot change `termCode` or `sequence` — see #4 for order. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t4"></a>4 | [`PUT /terms/order`](#e4) | Reorder the year's terms in one write. **This has to exist**: `sequence` is unique per year, so swapping two terms one `PATCH` at a time hits the unique index halfway through. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t5"></a>5 | [`POST /terms/{termCode}/results/lock`](#e5) | Freeze results for this period while another is still being marked. Idempotent. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t6"></a>6 | [`POST /terms/{termCode}/results/unlock`](#e6) | Reopen one period's results to correct a mark. Idempotent, and — like core's #27 — records nothing about who or why until there is an audit writer. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t7"></a>7 | [`POST /terms/{termCode}/deactivate`](#e7) | Take a term out of use without deleting it, so the exams and cards that reference it stay resolvable. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t8"></a>8 | [`POST /terms/{termCode}/reactivate`](#e8) | Put it back. The pair exists because there is no `DELETE`. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |

## 2. The terms — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t9"></a>9 | [`GET /terms`](#e9) | Every term in the year, in `sequence` order. Filter by `active`. Empty list if the year has none. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t10"></a>10 | [`GET /terms/current`](#e10) | Which term today falls in. What a mark-entry screen opens on, so a teacher does not pick the period by hand. `404` when no term covers today — a legitimate answer during a holiday between terms. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |
| <a id="t11"></a>11 | [`GET /terms/{termCode}`](#e11) | One term in full. `404` when the year has no term by that code. | [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) |

## 3. The classes — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t12"></a>12 | [`POST /classes`](#e12) | Create a grade for this year. **Sections and subjects cannot be supplied here** — they go on afterwards through #17 and #22. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`academic_years`](../../../models/core/AcademicYear.java) |
| <a id="t13"></a>13 | [`PATCH /classes/{classCode}`](#e13) | Fix the class's display name, sort order, or the affiliation programme it runs under. Not `classCode`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) |
| <a id="t14"></a>14 | [`PUT /classes/order`](#e14) | Set `displayOrder` across the year's classes in one write, so "Nursery, LKG, UKG, 1, 2, …" comes out in the order a school reads it rather than alphabetically. Unlike #4 this is a convenience, not a necessity — `displayOrder` has no unique index. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t15"></a>15 | [`POST /classes/{classCode}/deactivate`](#e15) | A grade this school no longer runs. Its sections stay resolvable for the records that reference them. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t16"></a>16 | [`POST /classes/{classCode}/reactivate`](#e16) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 4. The sections inside a class · [Build order ↓](#build-order)

`sectionNo` is the key **eight** other collections store as a plain string — counted from the
models, and one more than the [model README's own "six"](../../../models/academics/structure/README.md), which predates
`FeeInvoice` and `StudentAcademicRecord` carrying it. Everything in this group is shaped by that.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t17"></a>17 | [`POST /classes/{classCode}/sections`](#e17) | Add one section — `sectionNo`, capacity, class teacher. **The single most-waited-on endpoint in the module**: `StudentAcademicRecord.sectionNo` cannot be filled until a section exists. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t18"></a>18 | [`PUT /classes/{classCode}/sections`](#e18) | Replace the class's whole section list. For setup — "Grade 7 has A, B, C, D" in one call. Must refuse to drop a section anything already references. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t19"></a>19 | [`PATCH /classes/{classCode}/sections/{sectionNo}`](#e19) | Change a section's capacity or class teacher. **Never its `sectionNo`.** | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t20"></a>20 | [`POST /classes/{classCode}/sections/{sectionNo}/deactivate`](#e20) | Stop using a section without removing it. This is what a school does with a section that has emptied out, and it is the **only** way to retire one. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t21"></a>21 | [`POST /classes/{classCode}/sections/{sectionNo}/reactivate`](#e21) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 5. The subjects taught in a class · [Build order ↓](#build-order)

The key here is the **pair** `(subjectCode, sectionNo)`, because a class-wide subject and a
per-section one are different assignments of the same subject. `sectionNo` is `null` for
class-wide. So every endpoint that names a single assignment takes `?sectionNo=` — exactly the
shape core's `PATCH .../holidays/{date}?type=` already uses for the same reason.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t22"></a>22 | [`POST /classes/{classCode}/subjects`](#e22) | Assign a subject to the class, or to one section of it, with its teachers and grading scheme. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t23"></a>23 | [`PUT /classes/{classCode}/subjects`](#e23) | Replace the class's whole subject list. The year-setup call, and the only one that can check the whole list for a duplicate pair. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t24"></a>24 | [`PATCH /classes/{classCode}/subjects/{subjectCode}?sectionNo=`](#e24) | Change one assignment's display name, short name, type or grading scheme. Not `subjectCode`, and not `sectionNo` — moving an assignment between sections is a delete and an add, and there is no delete. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t25"></a>25 | [`PUT /classes/{classCode}/subjects/{subjectCode}/teachers?sectionNo=`](#e25) | Set who teaches it. Its own endpoint because it is the one thing on a subject that changes mid-year — a teacher leaves, a substitute takes over — and it replaces a list rather than editing a field. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t26"></a>26 | [`POST /classes/{classCode}/subjects/{subjectCode}/deactivate?sectionNo=`](#e26) | A subject this class has stopped teaching. Deactivated, not removed, because seven collections store `subjectCode`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t27"></a>27 | [`POST /classes/{classCode}/subjects/{subjectCode}/reactivate?sectionNo=`](#e27) | Put it back. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |

## 6. The reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t28"></a>28 | [`GET /classes`](#e28) | The year's classes in `displayOrder`, filtered by `active` and searchable by name. Paged. The screen a school opens to see its own structure. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t29"></a>29 | [`GET /classes/{classCode}`](#e29) | One class in full, sections and subjects included. One read, because they are embedded — which is the whole reason they are embedded. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t30"></a>30 | [`GET /classes/{classCode}/sections`](#e30) | Just the sections, with capacity and class teacher. What a "move a student" dropdown reads instead of pulling the whole class. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t31"></a>31 | [`GET /classes/{classCode}/subjects`](#e31) | Just the subject assignments, optionally for one `sectionNo`. What a mark-entry screen reads to know which subjects exist for a section. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t32"></a>32 | [`GET /subjects`](#e32) | Every distinct subject taught anywhere in the year, and which classes teach it. Answers "do we teach Sanskrit at all", which no per-class read can. Served by the `school_year_subject_code_idx` that already exists for it. | [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t33"></a>33 | [`GET /staff/{staffDocsId}/teaching`](#e33) | One teacher's whole load for the year — the sections they are class teacher of, and every subject they are assigned. A teacher's own home screen, and what somebody checks before a teacher resigns. Two indexes already exist for exactly this: `school_year_class_teacher_idx` and `school_year_subject_teacher_idx`. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`staff`](../../../models/people/staff/Staff.java) |
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
| **0** | `classCode` settled — nothing below compiles into a working collection without it | *no endpoint; see below* |
| **1** | A class with sections exists, so a student can be placed in one | 12, 17, 28, 29, 30 |
| **2** | Subjects are assigned, so marks and registers have something to be about | 22, 24, 31 |
| **3** | The year is divided, so an exam and a report card have a period | 1, 3, 9, 10, 11 |
| **4** | Setup stops being one call at a time | 2, 4, 14, 18, 23 |
| **5** | Things can be retired without being deleted | 5, 6, 7, 8, 15, 16, 20, 21, 25, 26, 27 |
| **6** | Next April does not mean retyping 132 objects | 35, 36 |
| **7** | The reads nothing is blocked on | 13, 32, 33, 34 |

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
- **No rename of `classCode`, `termCode`, `sectionNo` or `subjectCode`.** A rename would not fail
  and would not cascade. See [the table above](#addressed-by-code-never-by-id).
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

## 1. `classCode` does not exist — and this blocks everything

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

| Option | What it means | Cost |
|---|---|---|
| **Add `classCode`** *(recommended)* | A stable business key beside `name`, exactly like `termCode` beside its own `name`. Derived from the name on create — "Grade 7" → `GRADE_7` — the way `planCode` already is, with an explicit code accepted when the derived one is taken. | One field, one derivation, consistent with `termCode`, `programmeCode` and `examCode`. |
| **Repoint the index at `name`** | Uniqueness on the display name; `/classes/Grade%207` in every URL. | Renaming a class becomes impossible for the same reasons a code rename is, so `name` stops being a display field and the module loses the one field it could safely edit. |

**Recommended: add `classCode`.** It is the only option that leaves `name` editable, and this
whole plan is written assuming it — every path above says `{classCode}`. If the index is
repointed at `name` instead, the URLs change and #13 loses half its body.

*Note: `app.mongo.sync-indexes=true` builds indexes on demand, so the collision will not appear
until the index is actually built. It will not show up in a first test.*

## 2. Three referenced collections have no repository, so no reference can be validated

The model README requires "existence and tenant ownership of referenced staff, grading scheme,
programme". None of the three is reachable:

| Reference | Collection | Repository |
|---|---|---|
| `ClassSection.classTeacherDocsId`, `ClassSubject.teacherDocsIds` | `staff` | **none** |
| `ClassSubject.gradingSchemeDocsId` | `grading_schemes` | **none** |
| `SchoolClass.affiliationProgrammeDocsId` | `affiliation_programmes` | **none** |

So #17, #19, #22, #23 and #25 cannot check that a staff id belongs to this school, or exists.
Two honest options, and one dishonest one:

- **Build the three repositories** as part of phase 1. Each is one interface plus a
  `findByIdAndSchoolId`. This is the small, correct answer.
- **Accept the ids unvalidated and say so in the response**, the way every gate response already
  carries `NO_AUTHORIZATION_YET`. Acceptable only if the message is impossible to miss.
- **Validate nothing and say nothing.** This is how a class ends up with a teacher id belonging to
  another school, discovered when a timetable prints.

**Recommended: build the three repositories.** A tenant-crossing id is exactly the bug `SchoolBase`
exists to prevent, and this module is the first that could introduce one.

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
| `classCode` | String, required | **Does not exist on the model yet** — [open item 1](#1-classcode-does-not-exist--and-this-blocks-everything), and the whole plan waits on it. Intended: unique with `schoolId + academicYear`, derived from `name` — "Grade 7" → `GRADE_7`. **Never changes.** |
| `name` | String, required | **Open** — `@NotBlank`. `"Grade 7"`, `"Nursery"`, `"XII Science"`. Editable through #13, and safe to edit only because `classCode` carries the identity. |
| `affiliationProgrammeDocsId` | String, optional | `AffiliationProgramme.id`, or null. **Open, and unvalidatable today** — no repository. See [open item 2](#2-three-referenced-collections-have-no-repository-so-no-reference-can-be-validated). |
| `displayOrder` | Integer, optional | Sort order for the UI. **Not unique**, deliberately — so #14 is a convenience where #4 is a necessity. Null sorts last. |
| `sections` | List, required | `[]` at create (#12). #17 adds one, #18 replaces the list. Rows below. |
| `subjects` | List, required | `[]` at create (#12). #22 adds one, #23 replaces the list. Rows below. |
| `active` | Boolean, required | `true` at create; `false` from #15, `true` from #16. |

## `school_classes.sections[]` — [ClassSection](../../../models/academics/structure/embedded/ClassSection.java)

| Field | Type | What can be in it |
|---|---|---|
| `sectionNo` | String, required | `@NotBlank`, unique inside the owning class. `"A"`, `"B"`, `"Blue"` — it is both the reference and the display value, so there is no separate name field and there must not be one. **Never changes**: [eight collections](#addressed-by-code-never-by-id) store it. |
| `classTeacherDocsId` | String, optional | `Staff.id`, or null. **Open, unvalidatable today.** Null means no class teacher assigned, which is a normal state before staff are onboarded. |
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
| `teacherDocsIds` | List of String, required | `[]` or `Staff.id` values. **Open, unvalidatable today.** `[]` is legitimate — a subject with no teacher yet. Replaced wholesale by #25, never appended to, so removing a teacher is the same call as adding one. |
| `gradingSchemeDocsId` | String, optional | `GradingScheme.id`, or null. **Open, unvalidatable today.** Null falls through to `Exam.gradingSchemeDocsId` and then to none — the resolution order is in the model README. |
| `active` | Boolean, required | `true` at create; `false` from #26, `true` from #27. |

## The refusal codes this module introduces

Named here so two endpoints do not invent two codes for one condition — which is the mistake
`controllers/core` is currently carrying between `SCHOOL_NOT_ACTIVE` and `SCHOOL_NOT_EDITABLE`.

| Code | Status | When |
|---|---|---|
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | the `{year}` in the path is not a year of this school — reuses core's code and sentence |
| `CLASS_NOT_FOUND` | 404 | no class with that `classCode` in that year |
| `TERM_NOT_FOUND` | 404 | no term with that `termCode` in that year |
| `SECTION_NOT_FOUND` | 404 | that class has no such `sectionNo` |
| `SUBJECT_NOT_FOUND` | 404 | that class has no such `(subjectCode, sectionNo)` |
| `CLASS_CODE_TAKEN` | 409 | that year already has that `classCode` |
| `TERM_CODE_TAKEN` | 409 | that year already has that `termCode` |
| `SECTION_ALREADY_EXISTS` | 409 | that class already has that `sectionNo` |
| `SUBJECT_ALREADY_ASSIGNED` | 409 | that class already has that `(subjectCode, sectionNo)` pair |
| `TERM_SEQUENCE_TAKEN` | 409 | another active term in the year holds that `sequence` |
| `TERMS_OVERLAP` | 409 | the dates cover a day another term already covers |
| `TERM_OUTSIDE_ACADEMIC_YEAR` | 409 | the dates fall outside the year's own range |
| `INVALID_TERM_RANGE` | 400 | `endDate` before `startDate` |
| `TERM_WEIGHTS_INVALID` | 409 | a whole-set write (#2, #4) leaves active weights not summing to 100 |
| `SECTION_STILL_REFERENCED` | 409 | a replace (#18) would drop a `sectionNo` something stores |
| `SUBJECT_STILL_REFERENCED` | 409 | a replace (#23) would drop a `subjectCode` something stores |
| `NOTHING_TO_UPDATE` | 400 | a `PATCH` body that asks for nothing — reuses core's code |
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
**[1](#t1) · `POST /terms`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the term has to fall inside the year
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `termCode` and `sequence` for uniqueness in the year, and every active term's `startDate`, `endDate` for the overlap check. Served by `school_year_term_active_dates_idx`.
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *insert*: `academicYear` = the `{year}` path segment, `termCode` derived from `name`, `name`, `sequence`, `startDate`, `endDate`, `weightPercent`, `resultsLocked` = `false`, `active` = `true`

<a id="e2"></a>
**[2](#t2) · `PUT /terms`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — containment, for every row in the body
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every existing row's `termCode`, so a code in the body is an edit and a code missing from it is a removal
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *insert*: the same fields as #1, for each `termCode` not already there
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `name`, `sequence`, `startDate`, `endDate`, `weightPercent` for each `termCode` that is
- **Writes nothing when a code would be dropped** — `409 SECTION_STILL_REFERENCED`'s term equivalent. The whole request is refused rather than partly applied; a half-replaced term structure is worse than none.
- **The only endpoint that can check the weight sum**, because it is the only one holding every row at once. `409 TERM_WEIGHTS_INVALID`.

<a id="e3"></a>
**[3](#t3) · `PATCH /terms/{termCode}`**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — only when dates are sent
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: the other active terms' `startDate`, `endDate` — only when dates are sent
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `name`, `startDate`, `endDate`, `weightPercent`
- **Never `termCode`** — six documents point at this term, and see [the rename table](#addressed-by-code-never-by-id). **Never `sequence`** either: it is unique per year, so it moves through #4.
- **Reports a broken weight sum, does not refuse it** — see [open item 3](#3-term-weights-cannot-be-validated-one-patch-at-a-time). Refusing here makes 20/80 → 30/70 impossible.

<a id="e4"></a>
**[4](#t4) · `PUT /terms/order`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every term's `termCode` — the body must name all of them, or the ones left out keep numbers that collide
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `sequence`, on every term in the year
- **`school_year_term_sequence_uniq` is the reason this endpoint exists.** Swapping 1 and 2 by two `PATCH`es fails on the first. Even here the writes cannot go straight in: moving every term to a free range first, then to its target, is what keeps the unique index satisfied at every point in between. One transaction.

<a id="e5"></a>
**[5](#t5) · `POST /terms/{termCode}/results/lock`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — already `true` is a `200` saying so, not a refusal
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `resultsLocked` = `true`
- **Does not touch `AcademicYear.resultsLocked`.** That is the stronger, wider control and this is the narrow one; see [open item 7](#7-academictermresultslocked-and-academicyearresultslocked-both-exist).

<a id="e6"></a>
**[6](#t6) · `POST /terms/{termCode}/results/unlock`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked`
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `resultsLocked` = `false`
- **Records nothing about who unlocked, or why.** The same hole core's #27 carries, and the same answer: it needs a reason on the request and an `AuditEvent`, which needs an audit writer. Do not build this on real results without one.

<a id="e7"></a>
**[7](#t7) · `POST /terms/{termCode}/deactivate`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `active`, and `weightPercent` — deactivating a weighted term changes what the remaining weights sum to
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `active` = `false`
- **Writes `active`, never `recordState`** — [open item 4](#4-active-and-recordstate-are-two-flags-for-overlapping-things).
- **Frees its `sequence` and its `termCode`?** No. Both stay on the row, and both stay taken: the unique indexes do not filter on `active`. A deactivated `TERM_1` means the year can never have another.

<a id="e8"></a>
**[8](#t8) · `POST /terms/{termCode}/reactivate`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `active`, `sequence`, `startDate`, `endDate` — the year may have moved on since, so the overlap and sequence checks run again
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *updates*: `active` = `true`

## The terms — reads  ·  9–11

<a id="e9"></a>
**[9](#t9) · `GET /terms`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field, ordered by `sequence`. `?active=` filters. Served by `school_year_term_active_dates_idx`.
- **Not paged.** A year has two to four terms, not two hundred; a page cursor on a four-row list is machinery nobody uses.

<a id="e10"></a>
**[10](#t10) · `GET /terms/current`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `startDate`, `endDate`, and every field of the one that matches
- **"Today" is today in the school's zone**, through `Dates.todayIn(schoolZone.current())`. `LocalDate.now()` here is the exact bug core had in three places and fixed on 2026-09-10.
- **`404` is a real answer**, not an error to design around: a school between terms is genuinely in no term. Say which two it falls between.

<a id="e11"></a>
**[11](#t11) · `GET /terms/{termCode}`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field. `404 TERM_NOT_FOUND` when the year has no term by that code.

## The classes — writes  ·  12–16

<a id="e12"></a>
**[12](#t12) · `POST /classes`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `classCode` for uniqueness in the year. Served by `school_year_class_code_uniq` — **the index this whole plan is blocked on**; see [open item 1](#1-classcode-does-not-exist--and-this-blocks-everything).
- [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) — *reads*: existence and `schoolId` — **cannot be checked today**, no repository
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *insert*: `academicYear` = the `{year}` path segment, `classCode` derived from `name`, `name`, `affiliationProgrammeDocsId`, `displayOrder`, `sections` = `[]`, `subjects` = `[]`, `active` = `true`
- **`sections` and `subjects` are not accepted from the caller.** Both start empty and #17 and #22 fill them — the shape an academic year already uses for holidays and a plan for features.

<a id="e13"></a>
**[13](#t13) · `PATCH /classes/{classCode}`**

- [`affiliation_programmes`](../../../models/institution/AffiliationProgramme.java) — *reads*: existence and `schoolId` — only when the field is sent, and unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `name`, `displayOrder`, `affiliationProgrammeDocsId`
- **Never `classCode`**, and this is the endpoint that makes adding `classCode` worth it: with the index repointed at `name` instead, `name` becomes immutable and this request has one field left.
- `""` on `affiliationProgrammeDocsId` clears it; absent leaves it. The distinction core's `PATCH` endpoints already draw.

<a id="e14"></a>
**[14](#t14) · `PUT /classes/order`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every `classCode` in the year
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `displayOrder`, on each class named
- **No two-phase write, unlike #4.** `displayOrder` has no unique index, so duplicates are legal and a straight write is safe. Whether two classes *should* share a sort position is a UI question, not an integrity one — so this endpoint permits it and does not pretend otherwise.

<a id="e15"></a>
**[15](#t15) · `POST /classes/{classCode}/deactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `active` = `false`
- **Leaves every `sections[].active` and `subjects[].active` alone.** Arguably they are implied, but a write that changed forty flags when asked to change one is worse than a second call — the same rule core's `POST .../end` follows.

<a id="e16"></a>
**[16](#t16) · `POST /classes/{classCode}/reactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `active` = `true`

## The sections inside a class  ·  17–21

Every write here is a positional update on **`school_classes`**. A section is embedded, so there is
no section document to write — the document saved is always its class.

<a id="e17"></a>
**[17](#t17) · `POST /classes/{classCode}/sections`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo` — the new one must not already be there, `active` or not
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: existence and `schoolId` of `classTeacherDocsId` — **cannot be checked today**, no repository. A teacher id from another school would be accepted.
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: appends to `sections[]` — `sectionNo`, `classTeacherDocsId`, `capacity`, `active` = `true`
- **The endpoint the `student` module is waiting for.** `StudentAcademicRecord.sectionNo` has nothing to point at until this runs.
- **A duplicate is `409 SECTION_ALREADY_EXISTS`, checked in the service.** Mongo cannot enforce uniqueness *inside* an array, so nothing but this check stands between the class and two sections both called `A`.

<a id="e18"></a>
**[18](#t18) · `PUT /classes/{classCode}/sections`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every existing `sections[].sectionNo`, to tell an edit from an addition from a removal
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: every `classTeacherDocsId` in the body — unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: replaces `sections[]` wholesale
- **Refuses to drop a `sectionNo` that is not in the body** — `409 SECTION_STILL_REFERENCED`. Eight collections may store it and none of them can be checked cheaply, so a replace is add-and-edit only; removals go through #20. [Open item 5](#5-put-on-an-embedded-list-can-silently-drop-a-referenced-key).
- **`active` is preserved, not reset.** A row already deactivated stays deactivated unless the body says otherwise, or a setup replace would silently switch retired sections back on.

<a id="e19"></a>
**[19](#t19) · `PATCH /classes/{classCode}/sections/{sectionNo}`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo` — to find the row, `404 SECTION_NOT_FOUND` otherwise
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `classTeacherDocsId` when sent — unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].classTeacherDocsId`, `sections[…].capacity`
- **Never `sections[…].sectionNo`.** It is both the reference and the display value, which is precisely why there is no rename: there is no separate label to change instead.
- **`capacity` is not checked against anything.** Lowering it below the number of students already placed is allowed, because this module cannot count students. The count lives in `student`, and so does the refusal.

<a id="e20"></a>
**[20](#t20) · `POST /classes/{classCode}/sections/{sectionNo}/deactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo`, `sections[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].active` = `false`
- **This is the only way to retire a section**, and the whole reason there is no `DELETE`.
- **Leaves `subjects[]` alone**, including the per-section assignments pointing at this `sectionNo`. They are still true — that section was taught that subject — and #26 retires them if a school wants them gone.

<a id="e21"></a>
**[21](#t21) · `POST /classes/{classCode}/sections/{sectionNo}/reactivate`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo`, `sections[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `sections[…].active` = `true`

## The subjects taught in a class  ·  22–27

The row key is the **pair** `(subjectCode, sectionNo)`. Every endpoint naming one assignment takes
`?sectionNo=`; **omitting it means the class-wide row**, where `sectionNo` is `null` — not "any row".
That distinction is the whole reason the parameter exists, and it is the same shape as core's
`PATCH .../holidays/{date}?type=`.

**Moving `subjects` onto `ClassSection` was considered and rejected — 2026-09-10.** The nullable
`sectionNo` is awkward, and the two notes below say why: `?sectionNo=` reads differently here than
in #31, and nothing defines which row wins when a class-wide and a per-section row both exist. A
subject list on each section removes both. It was not taken because only **one** field on
`ClassSubject` — `teacherDocsIds` — actually varies by section, so four sections × ten subjects
would store the same `name`, `shortName`, `subjectType` and `gradingSchemeDocsId` four times, and
renaming a subject would become four edits inside one document that can drift apart. A split —
subject identity on the class, teachers on the section — avoids both, and was also not taken.
**The model stands as it is**, so the two flaws below are known and accepted, and the service check
in #22 is what keeps them from becoming data problems.

<a id="e22"></a>
**[22](#t22) · `POST /classes/{classCode}/subjects`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `subjects[].subjectCode` with `subjects[].sectionNo` — the pair must be free, `409 SUBJECT_ALREADY_ASSIGNED` otherwise
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo` — a non-null `sectionNo` in the body has to be a section this class actually has
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: every `teacherDocsIds` entry — unvalidatable today
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradingSchemeDocsId` — unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: appends to `subjects[]` — `subjectCode`, `name`, `shortName`, `subjectType`, `sectionNo`, `teacherDocsIds`, `gradingSchemeDocsId`, `active` = `true`
- **A class-wide row and a per-section row for one subject may both exist.** Nothing here forbids `MATHEMATICS` with `sectionNo: null` alongside `MATHEMATICS` with `sectionNo: "A"`, and which one wins is undefined. Worth settling before mark entry reads this list — it is not in [To settle](#to-settle-before-building) because it only becomes a bug when something consumes it.

<a id="e23"></a>
**[23](#t23) · `PUT /classes/{classCode}/subjects`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every existing `(subjectCode, sectionNo)` pair, and `sections[].sectionNo`
- [`staff`](../../../models/people/staff/Staff.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: every referenced id — unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: replaces `subjects[]` wholesale
- **Refuses to drop a `subjectCode`** — `409 SUBJECT_STILL_REFERENCED`, same rule as #18. Seven collections store it.
- **The only endpoint that sees the whole list**, so the only one that can catch a duplicate pair *within the body* rather than against what is stored.

<a id="e24"></a>
**[24](#t24) · `PATCH /classes/{classCode}/subjects/{subjectCode}?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `404 SUBJECT_NOT_FOUND` otherwise
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradingSchemeDocsId` when sent — unvalidatable today
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].name`, `subjects[…].shortName`, `subjects[…].subjectType`, `subjects[…].gradingSchemeDocsId`
- **Never `subjectCode` and never `sectionNo`** — both are the key. Moving an assignment to another section is #26 on the old row plus #22 on the new one, and the history reads correctly that way.
- **Not `teacherDocsIds` either** — that is #25, because it replaces a list rather than setting a field.

<a id="e25"></a>
**[25](#t25) · `PUT /classes/{classCode}/subjects/{subjectCode}/teachers?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: existence and `schoolId` of every id in the body — unvalidatable today, and the most consequential of the three gaps: this is the field that decides who can enter marks
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].teacherDocsIds`, replaced whole
- **A `PUT` of the whole list, not add and remove.** Two endpoints for one array is two ways to end up with a duplicate id in it. `[]` is a legitimate body: a subject with no teacher assigned yet.
- **Changes nothing already recorded.** Marks and registers store `subjectCode`, not the teacher, so replacing this list does not rewrite history — which is why it can be a plain `PUT` and not an event.

<a id="e26"></a>
**[26](#t26) · `POST /classes/{classCode}/subjects/{subjectCode}/deactivate?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `subjects[…].active`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].active` = `false`

<a id="e27"></a>
**[27](#t27) · `POST /classes/{classCode}/subjects/{subjectCode}/reactivate?sectionNo=`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the `(subjectCode, sectionNo)` row, `subjects[…].active`, and `sections[].sectionNo` — the section it belongs to may have been retired since
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *updates*: `subjects[…].active` = `true`

## The reads  ·  28–34

None of these runs a gate, and none of them writes anything.

<a id="e28"></a>
**[28](#t28) · `GET /classes`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `classCode`, `name`, `displayOrder`, `active`, `affiliationProgrammeDocsId`, and counts off `sections[]` and `subjects[]`
- **Sorted by `displayOrder`, then `classCode`.** `displayOrder` is optional and not unique, so it cannot be the whole sort — a tiebreaker is required or the page order changes between two identical requests. The same `_id`-tiebreaker problem #30 and #31 of the plans module hit.
- **Does not return the embedded lists**, only their sizes. A year of twelve classes with four sections and ten subjects each is 168 embedded rows in one response nobody reads. #29 is for one class in full.
- Filters: `?active=`, `?search=` on `name`. Served by `school_year_class_active_order_idx`.

<a id="e29"></a>
**[29](#t29) · `GET /classes/{classCode}`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every field, `sections[]` and `subjects[]` in full
- **One document, one query, no joins** — which is the entire reason sections and subjects are embedded rather than collections of their own.
- **The staff and grading-scheme ids come back raw**, not resolved to names. Resolving them needs the two repositories that do not exist; when they do, decide whether this endpoint resolves them or the caller does — do not do both.

<a id="e30"></a>
**[30](#t30) · `GET /classes/{classCode}/sections`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]` — `sectionNo`, `classTeacherDocsId`, `capacity`, `active`
- A projection of #29, existing because a "move this student" dropdown wants four fields and not a class with forty embedded rows behind them. `?active=true` is what that dropdown actually sends.

<a id="e31"></a>
**[31](#t31) · `GET /classes/{classCode}/subjects`**

- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `subjects[]`, optionally filtered to one `?sectionNo=`
- **`?sectionNo=A` returns the class-wide rows too**, and this is the one place that parameter does not mean "the row whose `sectionNo` is A". A section is taught its own assignments *and* the class's — so filtering to `sectionNo == "A"` alone would hide most of what section A studies. Say so in the response, because it contradicts #24's reading of the same parameter name.
- **It does not answer "which subjects does this student take."** Nothing does; see [open item 6](#6-the-upstream-gap--nothing-records-what-a-student-takes). For an `ELECTIVE` row this list is what is *offered*, not what is taken.

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
- **Does not check the staff id exists** — no repository — so an unknown id returns two empty lists rather than a `404`. Say which, or "no assignments" and "no such person" look identical.

<a id="e34"></a>
**[34](#t34) · `GET /structure`**

- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: every field of every term, in `sequence` order
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: every field of every class, `sections[]` and `subjects[]` included, in `displayOrder`
- The one-call bootstrap: what an app fetches on login instead of #9, #28 and #29 twelve times.
- **This is the response that gets big** — 168 embedded rows for a twelve-class school, and it is the only endpoint here with no filter and no page. That is deliberate for a bootstrap, and it is the endpoint to check first if the app feels slow. `?active=true` should be the default rather than an option.

## Rolling the structure into the next year  ·  35–36

Both read one year and write another, so both name **two** `{year}` values and must resolve both.

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
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *insert*: one document per source class — `classCode`, `name`, `displayOrder`, `affiliationProgrammeDocsId` copied; `sections[]` copied with `sectionNo` and `capacity`; `subjects[]` copied with `subjectCode`, `name`, `shortName`, `subjectType`, `sectionNo`, `gradingSchemeDocsId`; every `active` reset to `true`
- **`classTeacherDocsId` and `teacherDocsIds` are dropped unless `includeTeachers` is `true`, and the default is `false`.** This is the one real decision in the endpoint. Copying them silently assigns staff who may have resigned, and a teacher who left in March would be class teacher of a section in June with nobody having said so. Dropping them leaves a school re-assigning teachers — which it was going to do anyway, because that is what changes between years.
- **Copies only `active` rows**, so last year's retired sections do not come back to life in a fresh year.
- **Not a link, a copy.** The new year's documents are independent from the moment they are written; editing Grade 7 in 2027-2028 does not touch 2026-2027. That is what makes the per-year model safe, and it is also why nothing keeps the two in step afterwards.
