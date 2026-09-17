# controllers/academics/timetable — API plan

**One of twelve is built — [#1](#e1).** A school can write a day's periods across one date or a
range of them, with every period validated as a set, holidays inside the range skipped and named,
and a lesson refused unless that section actually studies the subject.

Everything else below is the full set of endpoints the timetable feature needs, written before
any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../../core/README.md), [`controllers/plans`](../../plans/README.md),
[`controllers/academics/structure`](../structure/README.md) and
[`controllers/academics/grading`](../grading/README.md) were done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/academics/timetable`](../../../models/academics/timetable), whose README is not a
description but a **persistence contract** — nine numbered rules about how this document must be
written. **These endpoints are the only thing that can enforce it**, and four of its rules turned
out to need revision before they can be. Those are open items below.

> **Four things were measured before writing this — 2026-09-16.** The contract was read as written
> and then checked against the models and the database rather than taken on trust.
>
> **1. The BSON-size rules guard a limit this collection cannot reach.** The contract devotes its
> whole section 4 to the 16 MiB document limit: warn at 10 MiB, refuse writes at 12 MiB, publish a
> `$bsonSize` metric. **One entry measures 305 bytes.** A 50-section school at 8 periods a day is
> **119 KB**; a 300-section school at 12 periods — larger than any school in India — is **1 MB**,
> or 6% of the limit. Reaching 10 MiB needs roughly **34,000 entries**, about 2,800 sections. The
> thresholds are not wrong, they are unreachable, and a rule that can never fire is one nobody
> maintains. See [open item 2](#2-the-bson-thresholds-guard-the-wrong-thing).
>
> **2. The real constraint is contention, and the contract makes it worse.** One document holds
> the whole school's day, and section 2 requires the client's expected `version` in the match of
> **every** update — including a single-entry `$set`. Two clerks editing **different sections**
> therefore collide on one counter. Since 2026-09-16 that surfaces as
> `409 CONCURRENT_MODIFICATION` rather than a 500, so it is now visible rather than mysterious —
> but it is still a refusal for work that never overlapped. See
> [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped).
>
> **3. `dailyTimetableDocsId` does not exist.** [`models/academics/README.md`](../../../models/academics/README.md)
> says a period attendance session "may optionally link to `DailyTimetable.id` through
> `dailyTimetableDocsId`". [`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java)
> has no such field and does not need one: it carries `attendanceDate`, and
> `school_timetable_date_uniq` makes `schoolId + date` identify the document. The model is right
> and the README is stale. See [open item 5](#5-the-attendance-link-is-documented-wrongly).
>
> **4. A weekly off is already expressible, and nothing needs inventing.**
> [`HolidayType`](../../../models/core/enums/HolidayType.java) carries `WEEKLY_OFF`, so a
> non-working day — Sunday, or Friday, or whatever this school closes on — is a dated
> `HolidayDetail` in [`AcademicYear.holidays`](../../../models/core/AcademicYear.java) like any
> other. **No endpoint here may infer a non-working day from the day of the week.** A school that
> runs on Sunday is a normal school.

---

## What this module is

**Where every child is meant to be, hour by hour, on one date.** A section has eight periods; each
is a subject, a teacher, a time, and sometimes a room. That is the whole of it.

**It is written per date, not per week.** [`DailyTimetable`](../../../models/academics/timetable/DailyTimetable.java)
is one document per school per calendar date, holding the periods of *every* class and section for
that day. There is no "weekly template" document and no recurrence rule — the model made that
choice, and this plan follows it. What a school actually does is build Monday and copy it, which is
why [#6](#e6) exists.

**A substitution is the write this module lives for.** A teacher calls in sick at 07:40 and six
periods need covering before 08:00. Everything below is arranged so that this is one small atomic
write ([#4](#e4)) against one embedded entry, preceded by one query that answers "who is free"
([#12](#e12)).

## What this module is not

- **Not attendance.** [`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java)
  points at an entry by `timetableEntryId`; nothing about attendance is stored here. The model
  README is explicit: *"Attendance sessions and records, homework and submissions, exams, marks,
  report cards, documents, and communication data remain in their own collections."*
- **Not room booking.** [`ResourceBooking`](../../../models/facilities/ResourceBooking.java) is a
  request-and-approve workflow for one-off use of a hall. A timetable entry naming a room is a
  standing arrangement, not a booking. They collide, and [open item 3](#3-a-room-can-be-double-booked-across-two-collections)
  is about who notices.
- **Not a generator.** Nothing here solves a timetable. `ScheduleConstraint` is listed in
  [`models/academics/README.md`](../../../models/academics/README.md) as *"add later with
  automated/AI timetable generation"*, and that stays true.
- **Not an exam schedule.** `ExamSchedule` is its own document in its own module, and also books
  rooms.

## One surface, and why

```text
/schools/current/timetables
```

> **Changed when [#1](#e1) was built — 2026-09-16.** The plan said
> `POST /timetables/{date}`. It writes a **range** — `startDate` with an optional `endDate` — so
> the dates moved into the body: a date in the path and a range in the body would be two sources
> for one fact, and the first request that disagreed with itself would have no right answer. The
> reads below keep `{date}` in the path, because each of them is about exactly one day.

**No `{year}` in the path**, unlike every route in [`structure`](../structure/README.md). The
contract's own rule 3 says `academicYear` *"is derived from `date`, not trusted from the request"* —
so a year in the path would be a second source for one fact, and the two could disagree. The date
decides which year this is, and the service looks it up.

**`{date}` is an ISO date, `2026-08-05`**, and it is the business key rather than a document id.
That is unusual here — a class, a term and a scheme are all addressed by their MongoDB id — and it
is correct for exactly one reason: **a caller always knows the date and never knows the id**. A
teacher's app asks "what is on today"; nothing ever asks "what is on in document 67aa15…".
`school_timetable_date_uniq` is what makes the date sufficient.

**An entry is addressed by its embedded `_id`**, which the service generates. The contract's rule 1
requires that, because MongoDB does not generate `_id` for embedded documents.

## Which gates every endpoint runs

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | every write |
| **2** | Is the subscription usable | every write |
| **4** | Is the derived year the running one | **every write** |

**Gate 4's rule applies, but gate 4 itself cannot run in the controller — and that is this
module's one deviation from the project convention.** A timetable *is* a year's: the date names
exactly one `AcademicYear`, and scheduling into a year the school has ended is a mistake with no
sensible reading. But the year is derived from a date in the **body**, and [#1](#e1)'s range may
span two years — so there is nothing for the controller to ask about before the service has read
the request.

**The check therefore runs per date inside the service**, as `409 ACADEMIC_YEAR_NOT_RUNNING`. It is
a refusal rather than a gate, it is written down here rather than left to be discovered, and it is
the only place in this project where `gates go in the controller` bends.

> **A year that has ended fails two different ways, and they are worth telling apart.**
> `POST /academic-years/{name}/end` also **closes the year on today**, so a date *after* today now
> falls outside every year and answers `409 NO_ACADEMIC_YEAR_FOR_DATE`. Only a date still *inside*
> the shortened year reaches `ACADEMIC_YEAR_NOT_RUNNING`. Measured 2026-09-16, after a test
> asserted the wrong one of the two.

**No gate runs on a read.** A suspended school still reads its own timetable, and last year's
Tuesday still answers, because attendance taken against it has to stay explicable.

---

# The endpoints

Numbered by area, not by build order. **Build order is below** and differs.

## 1. The day — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /timetables`](#e1) | Create a day's periods across one date **or a range**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t2"></a>2 | [`PUT /timetables/{date}`](#e2) | Replace the complete day. The only full-document write. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t6"></a>6 | [`POST /timetables/{date}/copy-from`](#e6) | Build this day from another day. **What schools actually do.** | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |

## 2. One period — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t3"></a>3 | [`POST /timetables/{date}/entries`](#e3) | Add one period. `$push`, never a re-save. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t4"></a>4 | [`PATCH /timetables/{date}/entries/{entryId}`](#e4) | Correct one period — **the substitution**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t5"></a>5 | [`DELETE /timetables/{date}/entries/{entryId}`](#e5) | Remove one period. `$pull`. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |

## 3. Reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t7"></a>7 | [`GET /timetables/{date}`](#e7) | The whole school's day. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t8"></a>8 | [`GET /timetables/{date}/sections/{classDocsId}/{sectionNo}`](#e8) | One section's day — **what a child's parent opens**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t9"></a>9 | [`GET /timetables/{date}/teachers/{teacherDocsId}`](#e9) | One teacher's day — **what a teacher's app opens**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t10"></a>10 | [`GET /timetables?from=&to=`](#e10) | A date range, for the week view. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t11"></a>11 | [`GET /timetables/{date}/rooms/{facilityResourceDocsId}`](#e11) | One room's day. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t12"></a>12 | [`GET /timetables/{date}/free-teachers?startTime=&endTime=`](#e12) | **Who can cover this period.** The 07:40 query. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java), [`staff`](../../../models/people/staff/Staff.java) |

---

# Build order

Ordered by **what it unblocks**, not by number.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **1** | A day exists and can be read back | ~~1~~, 7 |
| **2** | One period can be fixed without rewriting the day | 4, 3, 5 |
| **3** | The reads a school actually opens | 8, 9, 12 |
| **4** | A week is buildable without typing it five times | 6, 2, 10, 11 |

**#1 and #7 first, and nothing else works without them.** Every other endpoint either edits a day
that must already exist or reads one.

**#4 before #3 and #5**, which looks backwards. It is the endpoint this module exists for — a
substitution at 07:40 — and it is the one that exercises the contract's hardest rule: a targeted
array-filter update that must modify exactly one document or fail. Getting `$set entries.$[entry]`
right is the whole of this module's persistence risk, and doing it first means `$push` and `$pull`
are written knowing what the pattern looks like.

**#12 with the reads, not after them.** "Who is free at 09:00" is the question that makes a
substitution possible, and a school doing it by eye from #7 is a school that will double-book a
teacher. It is also a pure function over one document already being read.

**#6 is worth more than its position suggests.** A school does not type five days; it types Monday
and copies it. Until #6 exists, this module is technically complete and practically unusable — but
it needs the single-entry writes to exist first so that the copied day can then be corrected.

**#2 is last and may never be needed.** Full-document replacement is the one write the contract
allows to be a whole-document save, and every other endpoint exists so that it is not required. If
#1, #3, #4, #5 and #6 cover the real work, #2 is a footgun with an audit trail.

**Nothing outside this module is blocked on any of it.** `AttendanceSession` carries
`timetableEntryId` as an *optional* link and works without it.

---

# Things this module deliberately will not have

- **No `DELETE /timetables/{date}`.** A day is not created on a holiday, so there is nothing to
  delete; and a day that ran is a day attendance may reference. Deleting it would leave
  `AttendanceSession.timetableEntryId` pointing at nothing, and **nothing would fail** — the
  session would simply lose its period, which is the same argument that keeps a `DELETE` off
  grading schemes. See [open item 4](#4-removing-an-entry-that-attendance-references).
- **No weekly template document.** The model chose per-date storage. A template is a second source
  of truth for the same fact, and the first time somebody edits Tuesday without editing the
  template they diverge silently. [#6](#e6) gives the convenience without the second document.
- **No recurrence rule.** Same reason, plus every Indian school's calendar is interrupted by
  festivals often enough that the exception list would be longer than the rule.
- **No period-bell schedule.** `periodCode` and the two times are on the entry. A school-wide
  "period 3 is 10:30–11:15" document would be a fourth place times live, and the first substitution
  that shifts a period would break the invariant.
- **No conflict *override*.** An overlapping teacher is refused, never warned. This is the opposite
  of grading's gap warning, and deliberately: a gap is a hole somebody can see, while a
  double-booked teacher is two rooms of children expecting the same person, and no school means it.

---

# To settle before building

## 1. The version match serialises edits that never overlapped

**The contract's rule 2 requires the client's expected `version` in the match of every targeted
update.** For a full-day replacement that is exactly right. For a single-entry `$set` it is
stronger than the problem:

```text
09:00  clerk A changes section 5A period 3's teacher
09:00  clerk B changes section 9C period 7's room
```

Both match on `version = 4`. One wins, the other is `409 CONCURRENT_MODIFICATION` and must re-read
a 400 KB document to retry a change to a period the other clerk never touched. In a school with two
people in the office on a Monday morning this is not an edge case.

**The array filter already makes the write safe.** `$set entries.$[entry].teacherDocsId` with
`arrayFilters: [{entry._id: …}]` targets one embedded document; MongoDB applies it atomically; no
other entry is read or rewritten. The version adds nothing except a false conflict — *unless* the
rule being protected is "the entry you are editing has not changed since you read it", which is a
real concern for a substitution made from a stale screen.

**Proposal, to be decided before [#4](#e4) is built:**

| Write | Version in the match | Why |
|---|---|---|
| [#2](#e2) replace the day | **required** | It overwrites everything, including entries the caller never saw. |
| [#4](#e4) correct one entry | **optional**, honoured when sent | A caller editing from a stale screen can ask for the check; two clerks on different sections should not collide. |
| [#3](#e3) add · [#5](#e5) remove | **not used** | `$push` and `$pull` by `_id` are position-independent and cannot lose a concurrent edit to another entry. |

**This is a change to the model README's rule 2**, not a reinterpretation of it, and it should be
made there rather than quietly diverged from here.

## 2. The BSON thresholds guard the wrong thing

Measured 2026-09-16, with `bsonsize()` on a representative entry:

| Shape | Entries | Document |
|---|---|---|
| 20 sections × 8 periods | 160 | **48 KB** |
| 50 sections × 8 periods | 400 | **119 KB** |
| 100 sections × 10 periods | 1,000 | **298 KB** |
| 300 sections × 12 periods | 3,600 | **1,072 KB** |

The contract's "warn at 10 MiB, refuse at 12 MiB" needs about **34,000 entries** — roughly 2,800
sections in one school. The largest school in this database has **11 sections**.

**A rule that cannot fire is a rule nobody maintains**, and the `$bsonSize` metric it asks for is
machinery with nothing behind it. **Proposal:** replace the byte thresholds with an entry-count cap
— a few thousand, refusing with a code that names the number — and keep one sentence explaining
that the 16 MiB limit is the reason a cap exists at all. The cheap check catches the runaway loop
that the expensive one was really there for.

## 3. A room can be double-booked across two collections

[`TimetableEntry.facilityResourceDocsId`](../../../models/academics/timetable/embedded/TimetableEntry.java)
and [`ResourceBooking.facilityResourceDocsId`](../../../models/facilities/ResourceBooking.java) name
the same rooms, and `ExamSchedule` names them too. A physics practical in the lab at 11:00 and an
approved booking of the lab at 11:00 are a clash nothing currently detects.

**They are not even comparable without work.** A timetable entry is a `LocalDate` on the parent plus
two `LocalTime`s; a booking is two `Instant`s. Comparing them means resolving the school's
`defaultTimeZone` and converting — so the check cannot be a simple query, and the time zone is a
third input.

**Three options, none free:**

1. **This module checks bookings on write.** Correct, and makes every timetable write depend on the
   facilities module.
2. **The facilities module checks timetables on approval.** Correct, and the same coupling pointing
   the other way — but bookings are approved one at a time by a person, which is the cheaper place
   to pay it.
3. **Neither checks; a report finds clashes after the fact.** Honest, cheap, and useless at 11:00.

**Option 2 is the recommendation**, with this module refusing only clashes *within the timetable*
— two sections sent to the same room in the same period, which it can see for free in the document
it is already holding. `school_timetable_room_idx` exists for exactly that query.

## 4. Removing an entry that attendance references

[`AttendanceSession.timetableEntryId`](../../../models/academics/attendance/AttendanceSession.java)
is an optional link with no foreign key behind it. [#5](#e5) `$pull`s an entry by `_id`; any session
naming it is left pointing at nothing, and nothing fails.

**The grading module faced this and answered with retirement instead of deletion.** A timetable
entry has no `active` flag and should not grow one — a period that is not happening is a period that
is not in the day.

**Proposal:** [#5](#e5) refuses when an `AttendanceSession` references the entry, with a code naming
the session, and the message points at correcting the session first. Cost: one query per removal,
against `school_attendance_session_*`. The alternative — allowing it and accepting dangling links —
is the thing this project has refused everywhere else.

## 5. The attendance link is documented wrongly

[`models/academics/README.md`](../../../models/academics/README.md) says a session links to
`DailyTimetable.id` through **`dailyTimetableDocsId`**. No such field exists on
[`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java), and none is
needed: the session carries `attendanceDate`, and `school_timetable_date_uniq` makes
`schoolId + date` resolve the document in one read.

**The model is right; the README is stale.** Fix the README rather than adding the field — a second
way to find the same document is a second thing to keep consistent. *(Found 2026-09-16.)*

## 6. A substitution loses who was originally scheduled

[#4](#e4) overwrites `teacherDocsId`. After it, nothing anywhere says who was meant to take the
period or why they did not. A school asking "how many periods did we cover for absent staff last
term" cannot answer.

This is a **model** question, not an endpoint one — it needs either a field on the entry
(`originalTeacherDocsId`, plus a reason) or a separate substitution-log document. **Deliberately
not decided here**, because the answer is cheap now and expensive after a term of data. #4 is
buildable either way and does not block on it.

## 7. `periodCode` has no uniqueness rule anywhere

The model calls it a *"school-defined period identifier"* with the example `"P03"` and no
constraint. Nothing stops one section's day carrying `P03` twice, or a day mixing `P3` and `P03`.

**Proposal:** unique per `classDocsId + sectionNo` within the day, checked in the service — there is
no index to enforce it, because the whole day is one document and `periodCode` is two levels down.
Overlapping *times* are refused anyway, so a duplicate code is only reachable for genuinely
non-overlapping periods; it is still a label a human reads off a printed sheet, and two P03s make
that sheet wrong.

---

# Where the code will live

```text
controllers/academics/timetable/
├── README.md                          this file
└── DailyTimetableController.java      all twelve

services/academics/
├── DailyTimetableService.java         the endpoints
└── utils/
    └── DailyTimetableServiceUtils.java   loadDay(), resolveYear()

services/academics/helper/
└── TimetableHelper.java               the conflict rules, unit-testable

repositories/academics/timetable/
├── DailyTimetableRepository.java
├── DailyTimetableRepositoryCustom.java    the array-filter writes
└── DailyTimetableRepositoryImpl.java      + the range read for #10

dto/academics/timetable/
├── request/
│   ├── DailyTimetableCreateRequest.java
│   ├── TimetableEntryRequest.java
│   ├── TimetableEntryUpdateRequest.java
│   ├── TimetableCopyRequest.java
│   └── TimetableSearchRequest.java
└── response/
    ├── DailyTimetableResponse.java
    ├── TimetableEntryResponse.java
    ├── SectionDayResponse.java
    ├── TeacherDayResponse.java
    └── FreeTeacherResponse.java
```

**The array-filter writes go in the repository fragment, not the service.** They are queries, and
the service rule in this project is that a service builds and validates while a repository reads and
writes. It also puts every `$set entries.$[entry]` path in one file, which is the file to read when
asking "what can a targeted update actually change".

**`TimetableHelper` exists for the same reason [`GradingHelper`](../../../services/academics/helper/GradingHelper.java)
does.** Overlap arithmetic over a list is exactly the thing that is easy to get subtly wrong and
easy to unit-test — and grading's resolver proved that: eight mutations of it were each caught by a
different assertion, and none of them would have been visible over HTTP.

---

# Appendix — what each field can hold

## `daily_timetables` — [DailyTimetable](../../../models/academics/timetable/DailyTimetable.java)

| Field | Type | Rule |
|---|---|---|
| `schoolId` | String, required | Set explicitly on insert. Nothing validates a document on save. |
| `academicYear` | String, required | **Derived from `date`**, never taken from the request. The `AcademicYear.name` whose range contains the date. |
| `date` | LocalDate, required | The business key, with `schoolId`. One document per school per date. |
| `entries` | List, required | May be empty only in the moment between [#1](#e1) and [#3](#e3); [#1](#e1) requires at least one. |
| `version` | Long | Inherited. See [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped). |

## `daily_timetables.entries[]` — [TimetableEntry](../../../models/academics/timetable/embedded/TimetableEntry.java)

| Field | Type | Rule |
|---|---|---|
| `_id` | ObjectId, required | **Generated by the service**; MongoDB does not generate embedded ids. Immutable, unique within the day. |
| `periodCode` | String, required | See [open item 7](#7-periodcode-has-no-uniqueness-rule-anywhere). |
| `classDocsId` | String, required | Must be a class of this school **and this academic year**. |
| `sectionNo` | String, required | Must exist in that class's `sections[]`. |
| `slotType` | enum, required | `LESSON` · `BREAK` · `ASSEMBLY` · `ACTIVITY`. Decides which other fields are required. |
| `subjectCode` | String | **Required for `LESSON`**, refused otherwise. Must be in the class's `subjects[]`. |
| `teacherDocsId` | String | **Required for `LESSON`**, refused otherwise. Must be staff of this school. |
| `slotLabel` | String | For `BREAK`, `ASSEMBLY`, `ACTIVITY` — "Lunch Break". |
| `startTime` · `endTime` | LocalTime, required | `startTime` strictly before `endTime`. |
| `facilityResourceDocsId` | String | Optional and **normally absent** — most sections have one room all day. See [open item 3](#3-a-room-can-be-double-booked-across-two-collections). |

## The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `TIMETABLE_NOT_FOUND` | 404 | No timetable for that school and date. |
| `TIMETABLE_ALREADY_EXISTS` | 409 | [#1](#e1) on a date that already has one — use [#3](#e3) or [#2](#e2). |
| `TIMETABLE_ENTRY_NOT_FOUND` | 404 | No entry with that `_id` in that day. |
| `NOT_A_WORKING_DAY` | 409 | The date is a `HolidayDetail` in the year — including a `WEEKLY_OFF`. |
| `NO_ACADEMIC_YEAR_FOR_DATE` | 409 | No `AcademicYear` of this school contains the date. |
| `INVALID_PERIOD_TIMES` | 400 | `startTime` is not before `endTime`. |
| `SECTION_PERIOD_OVERLAP` | 409 | The same class and section has two periods covering one minute. |
| `TEACHER_PERIOD_OVERLAP` | 409 | The same teacher is in two places at once. |
| `ROOM_PERIOD_OVERLAP` | 409 | Two sections are sent to the same room at once. |
| `PERIOD_CODE_TAKEN` | 409 | Two periods of one section carry one `periodCode`. |
| `SUBJECT_NOT_IN_CLASS` | 409 | The `subjectCode` is not in that class's `subjects[]`. |
| `SECTION_NOT_IN_CLASS` | 409 | The `sectionNo` is not in that class's `sections[]`. |
| `SLOT_FIELDS_NOT_ALLOWED` | 400 | A `BREAK` carrying a subject or a teacher. |
| `SLOT_FIELDS_REQUIRED` | 400 | A `LESSON` missing one. |
| `TIMETABLE_TOO_LARGE` | 409 | The entry cap. See [open item 2](#2-the-bson-thresholds-guard-the-wrong-thing). |
| `ENTRY_STILL_REFERENCED` | 409 | [#5](#e5) on an entry an `AttendanceSession` names. See [open item 4](#4-removing-an-entry-that-attendance-references). |
| `CONCURRENT_MODIFICATION` | 409 | Shared. Another write changed the day first. |

---

# What every API touches, field by field

<a id="e1"></a>
**[1](#t1) · `POST /timetables`** — built

- *writes*: one document per working date in the range, every entry inside it

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/timetables

{
  "startDate": "2026-08-03",  // REQUIRED, ISO date
  "endDate": "2026-08-07",    // optional; absent
                              //   writes ONE day

  "entries": [                // REQUIRED, 1 to 4000
    {
      "periodCode": "P1",     // REQUIRED, max 40
      "classDocsId": "6aa...",// REQUIRED, max 60
      "sectionNo": "A",       // REQUIRED, max 20
      "slotType": "LESSON",   // REQUIRED, 4 values
      "startTime": "09:00:00",// REQUIRED
      "endTime": "09:45:00",  // REQUIRED

      "subjectCode": "MATHS", // LESSON only
      "teacherDocsId": "6aa..",// LESSON only
      "slotLabel": null,      // non-LESSON only
      "facilityResourceDocsId": null
    }
  ]
}
</pre></td>
<td><pre>
201 Created

{
  "startDate": "2026-08-03",
  "endDate": "2026-08-07",
  "createdCount": 4,
  "entriesPerDay": 8,
  "createdDates": [
    "2026-08-03", "2026-08-04",
    "2026-08-05", "2026-08-06"
  ],
  "skippedDates": [
    { "date": "2026-08-07",
      "reason": "NOT_A_WORKING_DAY",
      "holidayName": "Weekly off" }
  ],
  "nextStep": "..."
}

// `timetable` carries the whole day, and
// ONLY when exactly one date was written.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `startDate` | **yes** | ISO date. The first date to write. |
| `endDate` | no | **Absent means "just `startDate`"** — one day, which is the single-date form of this endpoint. Equal to `startDate` means the same. Before it is `400 INVALID_DATE_RANGE`; more than 120 days after it is `400 DATE_RANGE_TOO_LONG`, about a term of working days. |
| `entries` | **yes** | 1 to 4,000 periods, **applied to every date in the range**. Empty is refused: a day with no periods is not a day, it is the absence of a document — which is already what a holiday looks like. |
| `entries[].periodCode` | **yes** | Max 40. Unique per section per day, case-folded. Every section has its own P03 and that is normal. |
| `entries[].classDocsId` | **yes** | Max 60. Must be a class of this school **in that date's academic year**. |
| `entries[].sectionNo` | **yes** | Max 20. Must be an **active** section of that class. |
| `entries[].slotType` | **yes** | `LESSON` · `BREAK` · `ASSEMBLY` · `ACTIVITY`. **Decides which other fields are legal.** |
| `entries[].startTime` · `endTime` | **yes** | `startTime` strictly before `endTime`. **Equal is refused** — a period from 09:00 to 09:00 is nothing happening, and every overlap check would silently pass it. |
| `entries[].subjectCode` | **LESSON only** | Max 40. Required for a `LESSON`, **refused** on anything else. Must be a subject that section studies — see below. |
| `entries[].teacherDocsId` | **LESSON only** | Max 60, staff of this school. Required for a `LESSON`, **refused** on anything else: a break carrying a teacher would make a caller believe somebody was supervising lunch. |
| `entries[].slotLabel` | no | Max 120 — what a printed timetable calls a non-lesson. "Lunch Break". |
| `entries[].facilityResourceDocsId` | no | Max 60. **Normally absent**: in most Indian schools a section has one classroom all day and the timetable moves teachers, not children. It matters for the periods that break the pattern — a practical in the lab, games in the hall — which are exactly the ones two sections can be sent to at once. |

**No `id` on an entry, and none is accepted.** The service generates one ObjectId per period per
date — rule 1 of the persistence contract, because MongoDB does not generate `_id` for embedded
documents. **Each date gets its own**: two dates sharing an id would make
`AttendanceSession.timetableEntryId` ambiguous.

**No `academicYear`.** Derived from each date. A range may cross a year boundary, and each date
resolves its own.

### A subject must be one that section actually studies

This is the rule the endpoint was asked for, and it is wrong in two opposite directions:

| A `ClassSubject` created… | Which sections may be timetabled for it |
|---|---|
| **without** a `sectionNo` | **every** section of the class — this is Maths |
| **with** a `sectionNo` | **that section alone** — 10-C does German, 10-A does not |

Matching only the section's own subjects would refuse Maths for every section in the school.
Ignoring `sectionNo` would let 10-A be timetabled for German it does not take, and nobody would
notice until a child sat an exam in it. Refused as `409 SUBJECT_NOT_IN_SECTION`.

**It is the same reading [`GET /classes/{id}/subjects?sectionNo=`](../structure/README.md#e31)
answers with**, and it has to be — a timetable built from that list must not then be refused here.

**A retired subject cannot be scheduled**, unlike a retired grading scheme which must keep
resolving report cards already issued. Nothing is being reprinted; the day has not happened yet.

### A holiday is skipped; a taken date refuses everything

Those look inconsistent and are not:

- **A holiday inside a range is expected.** Any range longer than about five days contains a weekly
  off, so refusing the whole request over one would make ranges useless. It is skipped and **named**
  in `skippedDates`, because a caller that asked for fourteen days and got ten has no other way to
  learn why. A range where **every** date is a holiday writes nothing and is `409 NOT_A_WORKING_DAY`
  — there is no partial success to report.
- **A date that already has a timetable is not expected.** It means the caller is rebuilding
  something, and a partial write across a range leaves a school unable to tell which days came from
  which request. **All of it or none**, with every colliding date named.

**A weekly off is a dated holiday, never a weekday.** `HolidayType.WEEKLY_OFF` is one of the types a
`HolidayDetail` event carries, so the date says *that* the school is closed and the type says *why*.
**Nothing here looks at the day of the week**, and nothing ever may.

### Touching periods do not clash

09:00–09:45 beside 09:45–10:30 is a normal school day. That is the **opposite** call from a grade
band, whose bounds are inclusive at both ends and whose neighbours therefore must not touch — and
the difference is real: a band covers the value 90, while a period does not occupy the instant it
ends.

### Validated once, written many times

The periods are identical for every date, so the shape rules — times, slot fields, period codes and
the three overlap checks — run **once** against the built entries. What is genuinely per date is the
academic year, the holiday check and the structure lookups, because a range can cross a year
boundary and a class belongs to one year.

**One insert for the whole range, in one transaction.** A refusal on the last date leaves none of
the earlier ones behind — verified by counting documents before and after a range refused on its
second period.
- **The year is derived and then gated.** One read of `AcademicYear` resolves the name from the
  date; gate 4 then asks whether that year is running. A date in no year is `409
  NO_ACADEMIC_YEAR_FOR_DATE` — a fact about the calendar, not a bad request.
- **A holiday is refused**, `WEEKLY_OFF` included, from `AcademicYear.holidays`. **Never from the
  day of the week** — a school that runs on Sunday is a normal school.
- **At least one entry.** A day with no periods is not a day; it is the absence of a document, which
  is what a holiday already looks like.
- **Every entry is validated as a set**, not one at a time: overlaps are a property of the whole
  list. The same shape [#1 of grading](../grading/README.md#e1) settled on for bands.
- **Entry ids are generated here**, never accepted from the caller — the contract's rule 1.
- `409 TIMETABLE_ALREADY_EXISTS` rather than an upsert: a second create for one date is either a
  double submit or a misunderstanding, and both want to hear about it.

<a id="e2"></a>
**[2](#t2) · `PUT /timetables/{date}`**

- *writes*: the whole document
- **The one full-document write the contract permits**, and it requires the expected `version` — it
  overwrites entries the caller may never have seen.
- **Entry ids are preserved where they are sent back and generated where they are not**, so a
  replace that keeps a period keeps its identity and the attendance pointing at it.
- Build last, and consider not building it at all. See [Build order](#build-order).

<a id="e3"></a>
**[3](#t3) · `POST /timetables/{date}/entries`**

- *writes*: one `$push`
- **Validated against the stored day plus the new entry**, which is the same "validate the resulting
  document" rule [#3 of grading](../grading/README.md#e3) follows.
- **Re-validated immediately before the write**, per the contract's rule 3 — the read and the write
  are not atomic together, and a period can appear between them.
- No `version` in the match: a `$push` cannot lose somebody else's edit to a different entry.

<a id="e4"></a>
**[4](#t4) · `PATCH /timetables/{date}/entries/{entryId}`** — **the substitution**

- *writes*: one targeted `$set entries.$[entry].<field>` with an array filter
- **This is the endpoint the module exists for.** A teacher is absent; six periods need a different
  `teacherDocsId` before the bell.
- **Editable**: `teacherDocsId`, `subjectCode`, `startTime`, `endTime`, `slotLabel`,
  `facilityResourceDocsId`, `periodCode`.
- **Not editable**: `classDocsId`, `sectionNo`, `_id`. Moving a period to another section is
  deleting one and adding another, and pretending otherwise keeps attendance pointing at a period
  that changed identity.
- **`slotType` is not editable either**, because it decides which other fields are legal — turning a
  `LESSON` into a `BREAK` in place would leave a subject and a teacher on a break. Delete and add.
- **Exactly one document must be modified.** Zero means the entry is gone or the version moved:
  `404` or `409`, never a silent success. The contract's rule 2, and the one thing to get right.
- The expected `version` is honoured when sent and not required. See
  [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped).

<a id="e5"></a>
**[5](#t5) · `DELETE /timetables/{date}/entries/{entryId}`**

- *writes*: one `$pull` by `_id`
- **Refused when an `AttendanceSession` names the entry** — see
  [open item 4](#4-removing-an-entry-that-attendance-references).
- **A `204`, and a `404` when the entry was not there.** Not an idempotent `204` either way: a
  caller deleting a period that has already gone has a stale screen and should know.

<a id="e6"></a>
**[6](#t6) · `POST /timetables/{date}/copy-from`**

- *reads* one day, *writes* another
- **What a school actually does.** Monday is typed once; Tuesday through Friday are copied and then
  corrected. Without this, five days of a 400-entry timetable is 2,000 entries typed by hand.
- **New ids for every copied entry.** They are different periods on a different date; sharing an id
  would make `timetableEntryId` ambiguous across days.
- **The target date is validated like [#1](#e1)** — its own year, its own holiday check. Copying
  Monday onto a festival is refused.
- **Optional filters**: copy one class, one section, or the whole day. A school adding a section
  mid-term copies just that section's pattern.
- `409 TIMETABLE_ALREADY_EXISTS` unless the caller asks to merge, and merging re-runs every conflict
  check against the combined list.

<a id="e7"></a>
**[7](#t7) · `GET /timetables/{date}`**

- *reads*: one document
- **The whole school's day**, entries in stored order — the order they were written, never
  re-sorted, the same call [#7 of grading](../grading/README.md#e7) made for bands.
- **`404` when the day does not exist**, and that is not an error state: a holiday has no document.
  The response for a date inside a holiday should say *which* holiday, because "no timetable" and
  "the school is closed" are different facts and only one needs acting on.
- **No gate.** Last year's Tuesday still answers.

<a id="e8"></a>
**[8](#t8) · `GET /timetables/{date}/sections/{classDocsId}/{sectionNo}`**

- *reads*: one document, filtered in the service
- **What a child's parent opens.** Six to eight rows, ordered by `startTime` — and this one *is*
  sorted, because a parent reads a day chronologically and stored order is entry order.
- Filtered in memory rather than with a projection: the document is already loaded and measures
  hundreds of kilobytes at worst; a `$elemMatch` projection returns only the first match and would
  be wrong here.

<a id="e9"></a>
**[9](#t9) · `GET /timetables/{date}/teachers/{teacherDocsId}`**

- *reads*: one document, filtered in the service
- **What a teacher's app opens**, and the read that makes a substitution checkable: after [#4](#e4),
  this is where the covering teacher sees the period land.
- Ordered by `startTime`, like [#8](#e8), and for the same reason.

<a id="e10"></a>
**[10](#t10) · `GET /timetables?from=&to=`**

- *reads*: a range of documents
- **The week view.** Served by `school_year_timetable_date_idx` on `{schoolId, academicYear, date}`
  once the year is derived, or by `school_timetable_date_uniq` on the date alone.
- **Capped at a range a school actually views** — a month, not a year — because each document is
  hundreds of kilobytes and thirty of them is a response nothing renders.
- **Missing days are absent, not empty.** A week with a holiday returns six documents; inventing an
  empty seventh would make a closed school look like an unplanned one.

<a id="e11"></a>
**[11](#t11) · `GET /timetables/{date}/rooms/{facilityResourceDocsId}`**

- *reads*: one document, filtered
- **Where `school_timetable_room_idx` earns its place** — it is partial on
  `entries.facilityResourceDocsId` existing, which is right, because most entries have no room.
- Answers "what is the lab doing today", which is the question asked before a booking is approved —
  and the cheap half of [open item 3](#3-a-room-can-be-double-booked-across-two-collections).

<a id="e12"></a>
**[12](#t12) · `GET /timetables/{date}/free-teachers?startTime=&endTime=`**

- *reads*: the day, and the school's staff
- **The 07:40 query.** A teacher is absent and somebody has to decide who covers period 3 in the
  next twenty minutes. Without this the school does it by eye and double-books somebody.
- **Free means "has no entry overlapping this window on this date"** — computed from the day
  already loaded, not stored anywhere.
- **It cannot mean "is at work".** Whether a teacher is on leave lives in the leave module, which
  has no endpoints; whether they are employed at all is [#7 of people](../../people/staff/README.md#e7),
  which has no employment filters yet. **The response must say so** rather than implying a
  confidence it does not have — the same note every unauthorized endpoint in this project carries.
- Returns the teacher and how many periods they already have that day, because "free" and "free and
  not already teaching six periods" are different answers and only the school can weigh them.
