# controllers/academics/timetable — API plan

**11 of 12 planned endpoints are built, and the twelfth, #11, was removed.**

[#12](#e12) was the last, on 2026-10-15, and [#11](#e11) (one room's day) was built and removed the same day as not needed yet. A school can write a day's
periods across one date or a range of them, with every period validated as a set, holidays inside
the range skipped and named, and a lesson refused unless that section actually studies the subject.
It can list a year's days as counts, open any one of them in full with the names behind its ids
resolved, replace a whole day against the version it was read at, and build one day from
another — which is what a school actually does. It can also open one section's day and one
teacher's, which are the two reads a parent's app and a teacher's app actually make. And it can
add, correct and remove **one period** without rewriting the day — which is what the module was
for.

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

| Document | Collection | What it holds |
|---|---|---|
| [`DailyTimetable`](../../../models/academics/timetable/DailyTimetable.java) | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) | one school's whole day on one calendar date — every class and section |
| [`TimetableEntry`](../../../models/academics/timetable/embedded/TimetableEntry.java) | *embedded* in [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) | one period: a subject, a teacher, a time, and sometimes a room |

**A substitution is the write this module lives for.** A teacher calls in sick at 07:40 and six
periods need covering before 08:00. Everything below is arranged so that this is one small atomic
write ([#4](#e4)) against one embedded entry, preceded by one query that answers "who is free"
([#12](#e12)).

## What this module is not

- **Not attendance.** [`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java)
  points at a period by the day's `timetableDocsId` plus its class, section and `periodCode`; nothing about attendance is stored here. The model
  README is explicit: *"Attendance sessions and records, homework and submissions, exams, marks,
  report cards, documents, and communication data remain in their own collections."*
- **Not room booking.** There is no booking of rooms anywhere — the `room` package holds only
  the rooms themselves. A timetable entry naming a room is a standing arrangement;
  [open item 3](#3-a-room-can-be-double-booked-across-two-collections) is about the one other thing
  that can hold a room, an exam paper.
- **Not a generator.** Nothing here solves a timetable. `ScheduleConstraint` is listed in
  [`models/academics/README.md`](../../../models/academics/README.md) as *"add later with
  automated/AI timetable generation"*, and that stays true.
- **Not an exam schedule.** `ExamSchedule` is its own document in its own module, and also books
  rooms.

## One surface, and why

```text
/schools/current/academic-years/{year}/timetables
```

> **Changed twice, and the second change reversed the model contract — 2026-09-16 and 2026-09-17.**
>
> The plan said `POST /timetables/{date}` with **no year anywhere**, because the model's
> persistence contract required the academic year to be *derived* from the date. #1 writes a
> **range**, so the dates moved into the body first: a date in the path beside a range in the body
> would be two sources for one fact.
>
> **Then deriving the year turned out to be wrong.** A school holding two academic years whose
> ranges both covered a September date had its timetable written into the year it had **not**
> picked, and the refusal it eventually saw named a year it had never mentioned. **Every date
> resolves to *something*, so "you asked for the wrong year" was not a sentence this endpoint could
> say.** The year is now stated in the path, and a date outside it is
> `409 DATE_OUTSIDE_ACADEMIC_YEAR` naming that year's own range.
>
> **That also removed a deviation.** With the year named in the URL, gate 4 runs in the controller
> like everywhere else, and the service lost its per-date year lookup. This module is now on the
> same shape as the rest of `academics`.

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

**Gate 4 runs in the controller, like everywhere else** — since the year moved into the path on
2026-09-17. A timetable *is* a year's, and scheduling into a year the school has ended is a mistake
with no sensible reading.

It briefly could not: while the year was derived from a date in the body, a range could span two
years and there was nothing for the controller to ask about before the service had read the
request, so the check lived in the service as a per-date refusal. **A range can no longer span two
years**, because every date must fall inside the year named in the path — which is what made the
gate possible again.

**No gate runs on a read.** A suspended school still reads its own timetable, and last year's
Tuesday still answers, because attendance taken against it has to stay explicable.

---

# The endpoints

Numbered by area, not by build order. **Build order is below** and differs.

**What the marker in the `#` column means**, the same four words [`controllers/core`](../../core/README.md)
uses:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. 11 of them. |
| **deferred** | Planned, not started, and not next. |
| **optional** | A convenience nobody is waiting on. |
| **not being built** | Decided against, struck through, and the reason is in its own section. |

## 1. The day — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>#1 — **built** | [`POST /timetables`](#e1) | Create a day's periods across one date **or a range**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t2"></a>#2 — **built** | [`PUT /timetables/{date}`](#e2) | Replace the complete day. The only full-document write. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t6"></a>#6 — **built** | [`POST /timetables/{date}/copy-from`](#e6) | Build this day from another day. **What schools actually do.** | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |

## 2. One period — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t3"></a>#3 — **built** | [`POST /timetables/{date}/entries`](#e3) | Add one period. `$push`, never a re-save. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t4"></a>#4 — **built** | [`PATCH /timetables/{date}/entries/{entryId}`](#e4) | Correct one period — **the substitution**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t5"></a>#5 — **built** | [`DELETE /timetables/{date}/entries/{entryId}`](#e5) | Remove one period. `$pull`. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |

## 3. Reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t7"></a>#7 — **built** | [`GET /timetables/{date}`](#e7) | The whole school's day. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t8"></a>#8 — **built** | [`GET /timetables/{date}/sections/{classDocsId}/{sectionNo}`](#e8) | One section's day — **what a child's parent opens**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t9"></a>#9 — **built** | [`GET /timetables/{date}/teachers/{teacherDocsId}`](#e9) | One teacher's day — **what a teacher's app opens**. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t10"></a>#10 — **built** | [`GET /timetables?from=&to=`](#e10) | A year's days, date-wise, as **counts** rather than periods. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t11"></a>~~#11~~ **not being built** — removed 2026-10-15 | [~~GET /timetables/{date}/rooms/{roomDocsId}~~](#e11) | One room's day. **Not needed yet**; the number is retired, not reused. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t12"></a>#12 — **built** | [`GET /timetables/{date}/free-teachers?startTime=&endTime=`](#e12) | **Who can cover this period.** The 07:40 query. | [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java), [`staff`](../../../models/people/staff/Staff.java) |

---

<a id="build-order"></a>
# Build order

Ordered by **what it unblocks**, not by number.

| Phase | What it gives you | Endpoints | |
|---|---|---|---|
| **1** | A day exists and can be read back | ~~1~~, ~~7~~ | built |
| **2** | One period can be fixed without rewriting the day | ~~4~~, ~~3~~, ~~5~~ | built |
| **3** | The reads a school actually opens | ~~8~~, ~~9~~, ~~12~~ | built |
| **4** | A week is buildable without typing it five times | ~~6~~, ~~2~~, ~~10~~, 11 (removed) | built |

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

> **Built on 2026-09-17, ahead of #3, #4 and #5.** The correcting still has to be done with
> [#2](#e2), which replaces the whole day — so a school can now build a week in four calls and fix
> it with a blunt instrument. That is worse than #4 and much better than typing 2,000 periods.

**#2 is last and may never be needed.** Full-document replacement is the one write the contract
allows to be a whole-document save, and every other endpoint exists so that it is not required. If
#1, #3, #4, #5 and #6 cover the real work, #2 is a footgun with an audit trail.

> **Built anyway, out of order, on 2026-09-17 — and the warning above still stands.** It was asked
> for, and a school that has typed a day wrongly in forty places wants one call rather than forty.
> Three things were done to blunt the footgun: `version` is **required**, entry ids are **kept
> where they are sent** so a surviving period keeps its identity, and the response **names every
> id it removed** rather than counting them. That last one is the closest thing this endpoint has
> to an undo, and it is what makes the destruction visible instead of silent. **#3, #4 and #5 are
> still the endpoints a school should reach for**, and they do not exist yet.

**Nothing outside this module is blocked on any of it.** `AttendanceSession` carries
`timetableEntryId` as an *optional* link and works without it.

---

# Things this module deliberately will not have

- **No `DELETE /timetables/{date}`.** A day is not created on a holiday, so there is nothing to
  delete; and a day that ran is a day attendance may reference. Deleting it would leave
  `AttendanceSession.timetableDocsId` pointing at nothing, and **nothing would fail** — the
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

[`TimetableEntry.roomDocsId`](../../../models/academics/timetable/embedded/TimetableEntry.java)
and [`ExamSchedule.roomDocsId`](../../../models/academics/examination/ExamSchedule.java)
name the same rooms. A physics practical in the lab at 11:00 and an exam paper in the lab at 11:00
are a clash nothing currently detects: the timetable checks rooms against itself, and adding a paper
(examination #8) checks rooms against other papers.

**Both sides use a date and two local times**, so comparing them is a plain query. **Open:** whether
adding a paper should also refuse a room the timetable has at that hour. During exams the timetable
is usually suspended, which is why it is not done yet.

(Room bookings were part of this question until 2026-10-21, when the old `facilities` package was cut back to rooms
only.)

## 4. Removing an entry that attendance references

An attendance register finds its period by
[`AttendanceSession.timetableDocsId`](../../../models/academics/attendance/AttendanceSession.java)
(the day) plus its class, section and `periodCode` — a link with no foreign key behind it. [#5](#e5)
`$pull`s an entry by `_id`; any register for it would be left pointing at nothing, and nothing fails.

**Built: [#5](#e5) refuses (`ENTRY_STILL_REFERENCED`), and since 2026-10-15 [#4](#e4) refuses to
change the `periodCode` of such a period too** — the code is how the register finds it.

**The grading module faced this and answered with retirement instead of deletion.** A timetable
entry has no `active` flag and should not grow one — a period that is not happening is a period that
is not in the day.

**Proposal:** [#5](#e5) refuses when an `AttendanceSession` references the entry, with a code naming
the session, and the message points at correcting the session first. Cost: one query per removal,
against `school_attendance_session_*`. The alternative — allowing it and accepting dangling links —
is the thing this project has refused everywhere else.

## 8. A `LocalTime` is stored as a date, and cannot be compared in a query

**Measured 2026-09-18.** `TimetableEntry.startTime` is a `LocalTime` in Java. In MongoDB it is a
**BSON date carrying the day the document was written**:

```text
09:00  ->  2026-09-17T03:30:00.000Z      (IST, written on the 17th)
```

Two days written on one afternoon share that date part; two written a day apart do not. So
`{ "entries.startTime": { $lt: … } }` compares *when the row was typed* as much as *what time the
period runs*, and is wrong across writes.

**Nothing in the code does that today** — the overlap rules are all evaluated in Java on
deserialised `LocalTime` values, which drop the date part and are correct. The cost is that they
**can only** be evaluated that way: [#3](#e3) and [#4](#e4) must read the day, check in Java, and
then write, leaving the narrow race their sections describe. A guard that made overlap part of the
update is not available.

**Options, none of them free:** store the time as a zero-padded `"HH:mm"` string, which compares
correctly and needs a migration of every existing entry; or store minutes-since-midnight as an int,
which is smaller and less readable in the shell. Either would make the atomic guard possible. **Do
not add a query that compares these fields until one of them is done.**

## 5. The attendance link is documented wrongly

[`models/academics/README.md`](../../../models/academics/README.md) says a session links to
`DailyTimetable.id` through **`dailyTimetableDocsId`**. No such field exists on
[`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java), and none is
needed: the session carries `attendanceDate`, and `school_timetable_date_uniq` makes
`schoolId + date` resolve the document in one read.

**The model is right; the README is stale.** Fix the README rather than adding the field — a second
way to find the same document is a second thing to keep consistent. *(Found 2026-09-16.)*

> **Reversed 2026-10-15.** The field was added after all, as `timetableDocsId` — the day's
> document id — with the period found by class, section and `periodCode`. See the
> [models README](../../../models/academics/README.md#attendance).

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
| `subjectCode` | String | **Required for `LESSON`**, refused on every other slot type — a break teaches nothing. Must be in the class's `subjects[]`. |
| `teacherDocsId` | String | **Required for `LESSON`**, optional on every other slot type — somebody supervises the break, runs the assembly, takes the activity. Must be staff of this school, and a teacher named on a break still counts against their day. |
| `slotLabel` | String | For `BREAK`, `ASSEMBLY`, `ACTIVITY` — "Lunch Break". |
| `startTime` · `endTime` | LocalTime, required | `startTime` strictly before `endTime`. |
| `roomDocsId` | String | Optional and **normally absent** — most sections have one room all day. See [open item 3](#3-a-room-can-be-double-booked-across-two-collections). |

## The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `TIMETABLE_NOT_FOUND` | 404 | No timetable for that school and date. |
| `TIMETABLE_ALREADY_EXISTS` | 409 | [#1](#e1) on a date that already has one — use [#3](#e3) or [#2](#e2). |
| `TIMETABLE_ENTRY_NOT_FOUND` | 404 | No entry with that `_id` in that day. |
| `NOT_A_WORKING_DAY` | 409 | Every date in the range is a `HolidayDetail` in the year — including a `WEEKLY_OFF`. |
| `DATE_OUTSIDE_ACADEMIC_YEAR` | 409 | A date falls outside the year named in the path. **The refusal deriving the year could not give.** |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | No year of that name in this school. |
| `INVALID_PERIOD_TIMES` | 400 | `startTime` is not before `endTime`. |
| `SECTION_PERIOD_OVERLAP` | 409 | The same class and section has two periods covering one minute. |
| `TEACHER_PERIOD_OVERLAP` | 409 | The same teacher is in two places at once. |
| `ROOM_PERIOD_OVERLAP` | 409 | Two sections are sent to the same room at once. |
| `PERIOD_CODE_TAKEN` | 409 | Two periods of one section carry one `periodCode`. |
| `SUBJECT_NOT_IN_CLASS` | 409 | The `subjectCode` is not in that class's `subjects[]`. |
| `SECTION_NOT_IN_CLASS` | 409 | The `sectionNo` is not in that class's `sections[]`. |
| `SLOT_FIELDS_NOT_ALLOWED` | 400 | A non-lesson carrying a **subject**. A teacher on one is fine — somebody supervises it. |
| `SLOT_FIELDS_REQUIRED` | 400 | A `LESSON` missing one. |
| `TIMETABLE_TOO_LARGE` | 409 | The entry cap. See [open item 2](#2-the-bson-thresholds-guard-the-wrong-thing). |
| `ENTRY_STILL_REFERENCED` | 409 | [#5](#e5) on an entry an attendance register uses, and [#4](#e4) changing such an entry's `periodCode` (since 2026-10-15). See [open item 4](#4-removing-an-entry-that-attendance-references). |
| `DUPLICATE_TIMETABLE_ENTRY_ID` | 400 | [#2](#e2) sent one `timetableEntryId` twice in a request. |
| `SOURCE_IS_TARGET` | 400 | [#6](#e6) was asked to build a day from itself. |
| `SECTION_WITHOUT_CLASS` | 400 | [#6](#e6) filtered on `sectionNo` with no `classDocsId`. |
| `NOTHING_TO_COPY` | 409 | [#6](#e6)'s filter matched no period of the source day. |
| `NOTHING_TO_UPDATE` | 400 | [#4](#e4) sent no field to change. A correction has to say what it corrects. |
| `CONCURRENT_MODIFICATION` | 409 | Shared. Another write changed the day first — and on [#2](#e2), the required `version` did not match; on [#4](#e4), the optional one did not. |

**The date-versus-year arithmetic is shared**, in [`common/time/AcademicYearWindow`](../../../common/time/AcademicYearWindow.java) since 2026-09-28. This module keeps its own refusal **code** and **status** and its own choice of which end of the year to enforce; what it no longer keeps is its own answer at the boundary. Five modules wrote that answer out separately, each with its own wording and nothing to notice when one drifted.


---

<a id="what-every-api-touches-field-by-field"></a>
# Appendix — what every API touches, field by field

<a id="e1"></a>
**[#1](#t1) · `POST /timetables`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate` — every date must fall inside `{year}`; `holidays` — which dates to skip
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]`, `subjects[]` of every class the periods name, in `{year}`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — every `teacherDocsId` must be staff of this school
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `date` — is any date in the range already taken
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *inserts*: `schoolId`, `academicYear`, `date`, `entries[]` — one document per working date
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
      "roomDocsId": null
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
| `entries[].subjectCode` | **LESSON only** | Max 40. Required for a `LESSON`, **refused** on every other slot type — a break teaches nothing, so a subject there is either a mistake or a lesson wearing the wrong type. Must be a subject that section studies — see below. |
| `entries[].teacherDocsId` | **LESSON: yes** | Max 60, staff of this school. Required for a `LESSON` and **optional on everything else** — somebody *does* supervise lunch, and recording who is what makes their day add up. Changed 2026-09-17; it was refused on a non-lesson until then, which refused the field in the one case it is most useful in. |
| `entries[].slotLabel` | no | Max 120 — what a printed timetable calls a non-lesson. "Lunch Break". |
| `entries[].roomDocsId` | no | Max 60. **Normally absent**: in most Indian schools a section has one classroom all day and the timetable moves teachers, not children. It matters for the periods that break the pattern — a practical in the lab, games in the hall — which are exactly the ones two sections can be sent to at once. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | a required field is missing, `entries` is empty or holds more than 4,000, or a field is longer than its max |
| 400 | `INVALID_DATE_RANGE` | `endDate` is before `startDate` |
| 400 | `DATE_RANGE_TOO_LONG` | the range covers more than 120 days |
| 400 | `INVALID_PERIOD_TIMES` | a period's `startTime` is not before its `endTime` |
| 400 | `SLOT_FIELDS_REQUIRED` | a `LESSON` with no `subjectCode` or no `teacherDocsId` |
| 400 | `SLOT_FIELDS_NOT_ALLOWED` | a non-lesson carrying a `subjectCode` |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `CLASS_NOT_FOUND` | a `classDocsId` is not a class of this school in `{year}` |
| 404 | `TEACHER_NOT_FOUND` | a `teacherDocsId` is not staff of this school |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | a date in the range falls outside `{year}` |
| 409 | `TIMETABLE_ALREADY_EXISTS` | a date in the range already has a timetable; every such date is named |
| 409 | `NOT_A_WORKING_DAY` | every date in the range is a holiday, so nothing would be written |
| 409 | `SECTION_NOT_IN_CLASS` | the `sectionNo` is not an active section of that class |
| 409 | `SUBJECT_NOT_IN_SECTION` | that section does not study the `subjectCode` |
| 409 | `PERIOD_CODE_TAKEN` | two periods of one section carry the same `periodCode` |
| 409 | `SECTION_PERIOD_OVERLAP` | one section has two periods at the same time |
| 409 | `TEACHER_PERIOD_OVERLAP` | one teacher is in two periods at the same time |
| 409 | `ROOM_PERIOD_OVERLAP` | one room is given to two periods at the same time |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

**No `id` on an entry, and none is accepted.** The service generates one ObjectId per period per
date — rule 1 of the persistence contract, because MongoDB does not generate `_id` for embedded
documents. **Each date gets its own**: two dates sharing an id would make
that id ambiguous.

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
**[#2](#t2) · `PUT /schools/current/academic-years/{year}/timetables/{date}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `academicYear` — must be `{year}`; `version` — must match the one sent; `entries[]._id` — which ids are kept
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]`, `subjects[]` of every class the periods name, in `{year}`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — every `teacherDocsId` must be staff of this school
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *updates*: `entries[]` — replaced whole; `version` goes up by one
- *writes*: the whole document · *reads*: `school_classes`, `staff`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}
    /timetables/{date}

{year}  REQUIRED, the year's name, e.g. 2026-27
{date}  REQUIRED, ISO date, e.g. 2026-08-03

{
  "version": 4,               // REQUIRED, from #7
                              //   or #1's echo

  "entries": [                // REQUIRED, 1 to 4000
    {
      "timetableEntryId": "6ab..", // optional, max 60
                              //   absent or "" = a
                              //   new period
      "periodCode": "P1",     // REQUIRED, max 40
      "classDocsId": "6aa...",// REQUIRED, max 60
      "sectionNo": "A",       // REQUIRED, max 20
      "slotType": "LESSON",   // REQUIRED, 4 values
      "startTime": "09:00:00",// REQUIRED
      "endTime": "09:45:00",  // REQUIRED

      "subjectCode": "MATHS", // LESSON only, max 40
      "teacherDocsId": "6aa..",// LESSON: REQUIRED,
                              //   others optional
      "slotLabel": null,      // optional, max 120
      "roomDocsId": null      // optional, max 60
    }
  ]
}
</pre></td>
<td><pre>
200 OK — TimetableReplaceResponse

{
  "dailyTimetableDocsId": "6ab1...",
  "version": 5,
  "timetable": {
    "dailyTimetableDocsId": "6ab1...",
    "date": "2026-08-03",
    "academicYear": "2026-27",
    "version": 5,
    "entryCount": 8,
    "entries": [
      {
        "timetableEntryId": "6ab2...",
        "periodCode": "P1",
        "classDocsId": "6aa...",
        "sectionNo": "A",
        "slotType": "LESSON",
        "subjectCode": "MATHS",
        "teacherDocsId": "6aa...",
        "startTime": "09:00:00",
        "endTime": "09:45:00"
      }
    ]
  },
  "keptCount": 7,
  "addedCount": 1,
  "removedEntryIds": [ "6ab9..." ],
  "nextStep": "..."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. |
| `{date}` | **yes** | ISO date. The day to replace. It must already have a timetable. |
| `version` | **yes** | The day's version as [#7](#e7) or [#1](#e1) reported it. A different one is `409 CONCURRENT_MODIFICATION`. |
| `entries` | **yes** | 1 to 4,000 periods. **This is the whole day** — a stored period not in this list is removed. |
| `entries[].timetableEntryId` | no | Max 60. Send it back to keep a period's id. Absent or `""` means a new period, which gets a new id. An id not in this day is `404`; one id sent twice is `400`. |
| `entries[].periodCode` | **yes** | Max 40. Same rule as [#1](#e1). |
| `entries[].classDocsId` | **yes** | Max 60. Same rule as [#1](#e1). |
| `entries[].sectionNo` | **yes** | Max 20. Same rule as [#1](#e1). |
| `entries[].slotType` | **yes** | `LESSON` · `BREAK` · `ASSEMBLY` · `ACTIVITY`. Same rule as [#1](#e1). |
| `entries[].startTime` · `endTime` | **yes** | `startTime` strictly before `endTime`. |
| `entries[].subjectCode` | **LESSON only** | Max 40. Same rule as [#1](#e1). |
| `entries[].teacherDocsId` | **LESSON: yes** | Max 60. Same rule as [#1](#e1). |
| `entries[].slotLabel` | no | Max 120. |
| `entries[].roomDocsId` | no | Max 60. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | `version` or another required field is missing, `entries` is empty or holds more than 4,000, or a field is longer than its max |
| 400 | `DUPLICATE_TIMETABLE_ENTRY_ID` | one `timetableEntryId` is sent twice |
| 400 | `INVALID_PERIOD_TIMES` | a period's `startTime` is not before its `endTime` |
| 400 | `SLOT_FIELDS_REQUIRED` | a `LESSON` with no `subjectCode` or no `teacherDocsId` |
| 400 | `SLOT_FIELDS_NOT_ALLOWED` | a non-lesson carrying a `subjectCode` |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `TIMETABLE_NOT_FOUND` | the date has no timetable — create it with [#1](#e1) |
| 404 | `TIMETABLE_ENTRY_NOT_FOUND` | a `timetableEntryId` that is not in this day |
| 404 | `CLASS_NOT_FOUND` | a `classDocsId` is not a class of this school in `{year}` |
| 404 | `TEACHER_NOT_FOUND` | a `teacherDocsId` is not staff of this school |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year |
| 409 | `CONCURRENT_MODIFICATION` | `version` does not match, or another write saved the day first |
| 409 | `SECTION_NOT_IN_CLASS` | the `sectionNo` is not an active section of that class |
| 409 | `SUBJECT_NOT_IN_SECTION` | that section does not study the `subjectCode` |
| 409 | `PERIOD_CODE_TAKEN` | two periods of one section carry the same `periodCode` |
| 409 | `SECTION_PERIOD_OVERLAP` | one section has two periods at the same time |
| 409 | `TEACHER_PERIOD_OVERLAP` | one teacher is in two periods at the same time |
| 409 | `ROOM_PERIOD_OVERLAP` | one room is given to two periods at the same time |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

- **The one full-document write the contract permits**, and it requires the expected `version` — it
  overwrites entries the caller may never have seen.
- **`version` is required in the body**, unlike on any other write here. It comes from
  [#7](#e7) or from [#1](#e1)'s echo, both of which now return it — a required field with no way to
  obtain it would be a refusal nobody could satisfy. A mismatch is
  `409 CONCURRENT_MODIFICATION` naming **both** versions.
- **Checked twice, deliberately.** Once against the document just read, which is what gives the
  caller that message; and once by `@Version` on the save itself, which closes the window between
  that read and the write. The first is for the person, the second is for the race.
- **Entry ids are preserved where they are sent back and generated where they are not**, so a
  replace that keeps a period keeps its identity and the attendance pointing at it.
- **An id that is not in *this* day is `404 TIMETABLE_ENTRY_NOT_FOUND`**, including a real id
  belonging to another date. One id on two days would make
  that id ambiguous, which is the single thing generated ids exist to
  prevent. An id sent twice is `400 DUPLICATE_TIMETABLE_ENTRY_ID`. A **blank** id means "new", the
  same as leaving it off.
- **The response names every removed id**, rather than counting them. This is the destructive half
  of the endpoint, and those ids are what an `AttendanceSession` may still be pointing at — see
  [open item 4](#4-removing-an-entry-that-attendance-references), which this endpoint does **not**
  enforce. Visibility, not enforcement.
- **Every rule [#1](#e1) applies, this applies** — the section normalised to the class's own
  spelling, a subject that section actually studies, the three overlap checks, the period-code
  rule, and every teacher being this school's. They are the same code: the structure step and the
  teacher check moved into `utils` when this endpoint needed exactly them.
- **It replaces; it does not create.** A date with no timetable is `404 TIMETABLE_NOT_FOUND`
  pointing at [#1](#e1). The pair is what keeps both honest: #1 refuses a taken date, #2 refuses a
  free one.
- **A date that has since become a holiday can still be corrected**, and that is deliberate. #1
  skips holidays because it *chooses* its dates; this endpoint is handed one that already has a
  document, and refusing would trap a school that cannot delete the day either.
- **All three gates**, unlike the reads. This is the most destructive write in the module.
- Build last, and consider not building it at all — [it was built anyway](#build-order), and the
  reasoning is recorded there.

<a id="e3"></a>
**[#3](#t3) · `POST /timetables/{date}/entries`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `academicYear` — must be `{year}`; `entries[]` — the day the new period has to fit beside
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]`, `subjects[]` of the period's class, in `{year}`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — the `teacherDocsId` must be staff of this school; `fullName` — for `teacherName`
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *updates*: `entries[]` — one period pushed, guarded on its `periodCode`
- *writes*: one `$push`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}
     /timetables/{date}/entries

{year}  REQUIRED, the year's name
{date}  REQUIRED, ISO date — the day
        must already exist

{
  "periodCode": "P9",         // REQUIRED, max 40
  "classDocsId": "6aa...",    // REQUIRED, max 60
  "sectionNo": "A",           // REQUIRED, max 20
  "slotType": "LESSON",       // REQUIRED, LESSON ·
                              //   BREAK · ASSEMBLY ·
                              //   ACTIVITY
  "startTime": "14:00:00",    // REQUIRED
  "endTime": "14:45:00",      // REQUIRED

  "subjectCode": "MATHS",     // LESSON only, max 40
  "teacherDocsId": "6aa...",  // LESSON: REQUIRED,
                              //   others optional
  "slotLabel": null,          // optional, max 120
  "roomDocsId": null          // optional, max 60
}

// One period, not a list. No id: the
// server makes one.
</pre></td>
<td><pre>
201 Created — TimetableEntryDetailResponse

{
  "timetableEntryId": "6ab2...",
  "periodCode": "P9",
  "classDocsId": "6aa...",
  "className": "Class 10",
  "sectionNo": "A",
  "slotType": "LESSON",
  "subjectCode": "MATHS",
  "subjectName": "Mathematics",
  "teacherDocsId": "6aa...",
  "teacherName": "R. Sharma",
  "startTime": "14:00:00",
  "endTime": "14:45:00"
}

// slotLabel and roomDocsId appear only
// when they hold a value. A name that
// cannot be found is left out.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. |
| `{date}` | **yes** | ISO date. The day must already have a timetable — creating one is [#1](#e1). |
| `periodCode` | **yes** | Max 40. Must not already be used by another period of the same section that day. |
| `classDocsId` | **yes** | Max 60. A class of this school in `{year}`. |
| `sectionNo` | **yes** | Max 20. An active section of that class. |
| `slotType` | **yes** | `LESSON` · `BREAK` · `ASSEMBLY` · `ACTIVITY`. Decides which other fields are allowed. |
| `startTime` · `endTime` | **yes** | `startTime` strictly before `endTime`. |
| `subjectCode` | **LESSON only** | Max 40. Required for a `LESSON`, refused on any other slot type. Must be a subject that section studies. |
| `teacherDocsId` | **LESSON: yes** | Max 60, staff of this school. Optional on a non-lesson. |
| `slotLabel` | no | Max 120. |
| `roomDocsId` | no | Max 60. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | a required field is missing, or a field is longer than its max |
| 400 | `INVALID_PERIOD_TIMES` | `startTime` is not before `endTime` |
| 400 | `SLOT_FIELDS_REQUIRED` | a `LESSON` with no `subjectCode` or no `teacherDocsId` |
| 400 | `SLOT_FIELDS_NOT_ALLOWED` | a non-lesson carrying a `subjectCode` |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday, so it has no timetable |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet — create it with [#1](#e1) |
| 404 | `CLASS_NOT_FOUND` | the `classDocsId` is not a class of this school in `{year}` |
| 404 | `TEACHER_NOT_FOUND` | the `teacherDocsId` is not staff of this school |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |
| 409 | `SECTION_NOT_IN_CLASS` | the `sectionNo` is not an active section of that class |
| 409 | `SUBJECT_NOT_IN_SECTION` | that section does not study the `subjectCode` |
| 409 | `PERIOD_CODE_TAKEN` | the section already has a period with this code — or the day was removed while this was being checked |
| 409 | `SECTION_PERIOD_OVERLAP` | the new period overlaps another period of the same section |
| 409 | `TEACHER_PERIOD_OVERLAP` | the teacher is already in another period at that time |
| 409 | `ROOM_PERIOD_OVERLAP` | the room is already given to another period at that time |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

- **Validated against the stored day plus the new entry**, which is the same "validate the resulting
  document" rule [#3 of grading](../grading/README.md#e3) follows.
- **A `$push`, never a re-save.** A day is about 120 KB; rewriting all of it to add one period
  would make every addition a race with every other edit of that morning, and would overwrite
  periods the caller never saw.
- **Re-validated immediately before the write**, per the contract's rule 3 — the read and the write
  are not atomic together, and a period can appear between them. The new period is checked against
  the **whole day as it stands**, not on its own: whether it fits beside the others is the only
  thing worth checking.
- **The period code is guarded in the update itself** — `$push` matched on "this section has no
  period with this code". That is the one conflict rule expressible without comparing times, and it
  is the one most likely to be raced, because a period code is what a person types twice.
- **`201`, returning the period with its generated id** and the names behind its ids.
- **The day must already exist** — `404 TIMETABLE_NOT_FOUND`. Creating one is [#1](#e1).
- No `version` in the match: a `$push` cannot lose somebody else's edit to a different entry.

> **The residual race is narrow, real, and cannot be closed here.** Two clerks adding *overlapping*
> periods with *different* codes in the same instant would both be accepted. Closing it needs
> either a version in the match — which [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped)
> argues against, because it would also refuse two clerks working on two sections — or times stored
> as something MongoDB can compare, which they are not: see
> [open item 8](#8-a-localtime-is-stored-as-a-date-and-cannot-be-compared-in-a-query).

<a id="e4"></a>
**[#4](#t4) · `PATCH /timetables/{date}/entries/{entryId}`** — built · **the substitution**

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `academicYear` — must be `{year}`; `version` — must match the one sent; `entries[]` — the period, and the rest of the day it has to fit beside
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `timetableDocsId`, `classDocsId`, `sectionNo`, `periodCode` — only when `periodCode` is changing
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]`, `subjects[]` of the period's class, in `{year}`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — the `teacherDocsId` must be staff of this school; `fullName` — for `teacherName`
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *updates*: of one entry, any of `periodCode`, `subjectCode`, `teacherDocsId`, `slotLabel`, `startTime`, `endTime`, `roomDocsId`
- *writes*: one targeted `$set entries.$[entry].<field>` with an array filter

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}
      /timetables/{date}/entries/{entryId}

{year}     REQUIRED, the year's name
{date}     REQUIRED, ISO date
{entryId}  REQUIRED, the period's
           timetableEntryId

{
  "version": 5,               // REQUIRED, from #7

  "teacherDocsId": "6aa...",  // optional, max 60
  "subjectCode": "MATHS",     // optional, max 40
  "startTime": "09:00:00",    // optional
  "endTime": "09:45:00",      // optional
  "slotLabel": "Lunch",       // optional, max 120
  "roomDocsId": "",           // optional, max 60
  "periodCode": "P1"          // optional, max 40
}

// Absent = leave it. "" = clear it, except
// periodCode, startTime and endTime.
// At least one field besides version.
</pre></td>
<td><pre>
200 OK — TimetableEntryDetailResponse

{
  "timetableEntryId": "6ab2...",
  "periodCode": "P1",
  "classDocsId": "6aa...",
  "className": "Class 10",
  "sectionNo": "A",
  "slotType": "LESSON",
  "subjectCode": "MATHS",
  "subjectName": "Mathematics",
  "teacherDocsId": "6aa...",
  "teacherName": "P. Iyer",
  "startTime": "09:00:00",
  "endTime": "09:45:00"
}

// The period as it is after the change.
</pre></td>
</tr>
</table>

**Every field on the request.** Absent always means "leave it exactly as it is".

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. |
| `{date}` | **yes** | ISO date. The day must already have a timetable. |
| `{entryId}` | **yes** | The period's `timetableEntryId`. Not in this day is `404 TIMETABLE_ENTRY_NOT_FOUND`. |
| `version` | **yes** | The day's version the correction was decided against. `@NotNull` in the code since 2026-09-30, so leaving it out is `400 VALIDATION_FAILED`. A different one is `409 CONCURRENT_MODIFICATION`. |
| `teacherDocsId` | no | Max 60, staff of this school. `""` clears it — which a `LESSON` then refuses. |
| `subjectCode` | no | Max 40. Must be a subject that section studies. `""` clears it — which only a non-lesson may end up with. |
| `startTime` | no | A time. Must still be before `endTime`. Cannot be cleared. |
| `endTime` | no | A time. Must still be after `startTime`. Cannot be cleared. |
| `slotLabel` | no | Max 120. `""` clears it. |
| `roomDocsId` | no | Max 60. `""` clears it — the only way to take a room away. |
| `periodCode` | no | Max 40. Cannot be cleared. Refused if attendance was taken against this period. |

`classDocsId`, `sectionNo` and `slotType` are not on the request — see below.

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | `version` is missing, or a field is longer than its max |
| 400 | `NOTHING_TO_UPDATE` | no field besides `version` was sent |
| 400 | `INVALID_PERIOD_TIMES` | the corrected `startTime` is not before `endTime` |
| 400 | `SLOT_FIELDS_REQUIRED` | the change leaves a `LESSON` with no subject or no teacher |
| 400 | `SLOT_FIELDS_NOT_ALLOWED` | the change puts a subject on a non-lesson |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday, so it has no timetable |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet |
| 404 | `TIMETABLE_ENTRY_NOT_FOUND` | no period with that id in this day |
| 404 | `CLASS_NOT_FOUND` | the period's class is no longer a class of this school in `{year}` |
| 404 | `TEACHER_NOT_FOUND` | the `teacherDocsId` is not staff of this school |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |
| 409 | `CONCURRENT_MODIFICATION` | `version` does not match, or the period was removed or the day changed while this was being checked |
| 409 | `ENTRY_STILL_REFERENCED` | `periodCode` is changing and attendance was taken against this period |
| 409 | `SECTION_NOT_IN_CLASS` | the period's section is no longer an active section of its class |
| 409 | `SUBJECT_NOT_IN_SECTION` | that section does not study the `subjectCode` |
| 409 | `PERIOD_CODE_TAKEN` | another period of the same section already has this code |
| 409 | `SECTION_PERIOD_OVERLAP` | the corrected period overlaps another period of the same section |
| 409 | `TEACHER_PERIOD_OVERLAP` | the covering teacher is already in another period at that time |
| 409 | `ROOM_PERIOD_OVERLAP` | the room is already given to another period at that time |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

- **This is the endpoint the module exists for.** A teacher is absent; six periods need a different
  `teacherDocsId` before the bell.
- **Editable**: `teacherDocsId`, `subjectCode`, `startTime`, `endTime`, `slotLabel`,
  `roomDocsId`, `periodCode`.
- **Not editable**: `classDocsId`, `sectionNo`, `_id`. Moving a period to another section is
  deleting one and adding another, and pretending otherwise keeps attendance pointing at a period
  that changed identity.
- **`slotType` is not editable either**, because it decides which other fields are legal — turning a
  `LESSON` into a `BREAK` in place would leave a subject and a teacher on a break. Delete and add.
- **Exactly one document must MATCH.** Zero means the entry is gone or the version moved:
  `404` or `409`, never a silent success. The contract's rule 2, and the one thing to get right.
- **Matched, not modified — and the difference is a trap.** A `$set` writing the value a field
  already holds modifies nothing, and reading that as "the entry is gone" would turn a perfectly
  good no-op patch into a `404`. The count that decides is the matched one.
- **`""` clears; an absent key leaves the field alone.** A room is removed by sending
  `roomDocsId: ""`, and there is no other way to say it — treating absent as a clear
  would empty a field every time somebody patched a different one. It does not apply to
  `periodCode`, `startTime` and `endTime`, which a period cannot be without.
- **A patch that changes nothing is `400 NOTHING_TO_UPDATE`.** A correction has to say what it
  corrects.
- **The corrected period is checked beside the others**, which is the whole point of a
  substitution: the covering teacher must not already be somewhere else at that hour.
- The expected `version` is honoured when sent and not required. See
  [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped).

<a id="e5"></a>
**[#5](#t5) · `DELETE /timetables/{date}/entries/{entryId}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `academicYear` — must be `{year}`; `entries[]._id` — the period must be in this day
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `timetableDocsId`, `classDocsId`, `sectionNo`, `periodCode` — has attendance been taken against this period
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *updates*: `entries[]` — one period pulled; `version` goes up by one
- *writes*: one `$pull` by `_id`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
DELETE /schools/current/academic-years/{year}
       /timetables/{date}/entries/{entryId}

{year}     REQUIRED, the year's name
{date}     REQUIRED, ISO date
{entryId}  REQUIRED, the period's
           timetableEntryId

No body.
</pre></td>
<td><pre>
204 No Content

No body.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. |
| `{date}` | **yes** | ISO date. The day must already have a timetable. |
| `{entryId}` | **yes** | The period's `timetableEntryId`. Not in this day is `404 TIMETABLE_ENTRY_NOT_FOUND`. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday, so it has no timetable |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet |
| 404 | `TIMETABLE_ENTRY_NOT_FOUND` | no period with that id in this day |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |
| 409 | `ENTRY_STILL_REFERENCED` | attendance was taken against this period |
| 409 | `CONCURRENT_MODIFICATION` | somebody else removed the period while this was being checked |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

- **Refused when an `AttendanceSession` names the entry** — `409 ENTRY_STILL_REFERENCED`. This is
  [open item 4](#4-removing-an-entry-that-attendance-references)'s proposal, built:
  `AttendanceSessionRepository` exists for this one question. **Nothing writes that collection
  yet**, so the refusal cannot fire through the API today — it costs one query and becomes live the
  moment attendance is built, rather than needing to be remembered then.
- **A `204`, and a `404` when the entry was not there.** Not an idempotent `204` either way: a
  caller deleting a period that has already gone has a stale screen and should know. **Here the
  modified count is the right signal**, unlike [#4](#e4): a `$pull` that removes nothing modifies
  nothing.
- **No `version`**, per [open item 1](#1-the-version-match-serialises-edits-that-never-overlapped):
  a `$pull` by id is position-independent. The removal still **bumps** the version, so #2 can tell
  the day moved.

<a id="e6"></a>
**[#6](#t6) · `POST /schools/current/academic-years/{year}/timetables/{date}/copy-from`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate` — the target must fall inside `{year}`; `holidays` — the target must be a working day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: the source day — `academicYear` must be `{year}`, and its `entries[]` are what gets copied
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: the target day, if there is one — `academicYear` must be `{year}`, and its `entries[]` are kept on a merge
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[]`, `subjects[]` of every class the combined periods name, in `{year}`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — every `teacherDocsId` must be staff of this school
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *inserts*: `schoolId`, `academicYear`, `date`, `entries[]` — when the target had no day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *updates*: `entries[]` — copied periods added; `version` goes up by one — when merging
- *reads* one day, *writes* another · also reads `school_classes`, `staff`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}
     /timetables/{date}/copy-from

{year}  REQUIRED, the year's name
{date}  REQUIRED, ISO date — the day
        being BUILT

{
  "sourceDate": "2026-08-03", // REQUIRED, ISO date
                              //   the day copied FROM
  "classDocsId": "6aa...",    // optional, max 60
                              //   one class only
  "sectionNo": "A",           // optional, max 20
                              //   needs classDocsId
  "merge": false              // optional, true or
                              //   false; absent =
                              //   false
}
</pre></td>
<td><pre>
201 Created — a new day was built
200 OK      — merged into an existing day
TimetableCopyResponse

{
  "date": "2026-08-04",
  "sourceDate": "2026-08-03",
  "dailyTimetableDocsId": "6ab3...",
  "version": 0,
  "merged": false,
  "copiedCount": 8,
  "keptCount": 0,
  "timetable": {
    "dailyTimetableDocsId": "6ab3...",
    "date": "2026-08-04",
    "academicYear": "2026-27",
    "version": 0,
    "entryCount": 8,
    "entries": [ ... ]
  },
  "nextStep": "..."
}

// Every copied period has a NEW
// timetableEntryId.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Both days must belong to it. |
| `{date}` | **yes** | ISO date. The day being built. Must be inside `{year}` and a working day. |
| `sourceDate` | **yes** | ISO date. The day to copy from. It must already have a timetable in `{year}`, and must not be `{date}`. |
| `classDocsId` | no | Max 60. Copy only this class's periods. Absent copies every class. |
| `sectionNo` | no | Max 20. Copy only this section's periods. Needs `classDocsId`; alone it is `400 SECTION_WITHOUT_CLASS`. |
| `merge` | no | `true` adds the copied periods to a day that already exists. Absent or `false` refuses a day that already exists with `409 TIMETABLE_ALREADY_EXISTS`. It never replaces. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | `sourceDate` is missing, or a field is longer than its max |
| 400 | `SOURCE_IS_TARGET` | `sourceDate` is the same as `{date}` |
| 400 | `SECTION_WITHOUT_CLASS` | `sectionNo` sent without `classDocsId` |
| 400 | `INVALID_PERIOD_TIMES` · `SLOT_FIELDS_REQUIRED` · `SLOT_FIELDS_NOT_ALLOWED` | the combined day breaks a shape rule |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `TIMETABLE_NOT_FOUND` | `sourceDate` has no timetable, so there is nothing to copy |
| 404 | `CLASS_NOT_FOUND` | a copied period's class is not a class of this school in `{year}` |
| 404 | `TEACHER_NOT_FOUND` | a copied period's teacher is not staff of this school |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the source day belongs to another year, `{date}` is outside `{year}`, or the existing target day belongs to another year |
| 409 | `NOT_A_WORKING_DAY` | `{date}` is a holiday |
| 409 | `TIMETABLE_ALREADY_EXISTS` | `{date}` already has a timetable and `merge` is not `true` |
| 409 | `NOTHING_TO_COPY` | no period of the source day matches the filter |
| 409 | `SECTION_NOT_IN_CLASS` · `SUBJECT_NOT_IN_SECTION` | a copied period no longer fits the school's structure |
| 409 | `PERIOD_CODE_TAKEN` · `SECTION_PERIOD_OVERLAP` · `TEACHER_PERIOD_OVERLAP` · `ROOM_PERIOD_OVERLAP` | the combined day has a clash — on a merge, the copied periods against the ones already there |
| 409 | `CONCURRENT_MODIFICATION` | another write saved the target day first |
| 409 | `SCHOOL_NOT_ACTIVE` · `SUBSCRIPTION_NOT_USABLE` · `ACADEMIC_YEAR_NOT_RUNNING` | gate 1, 2 or 4 — see [Which gates every endpoint runs](#which-gates-every-endpoint-runs) |

- **What a school actually does.** Monday is typed once; Tuesday through Friday are copied and then
  corrected. Without this, five days of a 400-entry timetable is 2,000 entries typed by hand.
- **The target is in the path, the source is in the body.** The day being *built* is what this acts
  on, so it is the address — the same reading that puts `{date}` in the path on [#2](#e2) and
  [#7](#e7).
- **New ids for every copied entry.** They are different periods on a different date; sharing an id
  would make `timetableEntryId` ambiguous across days. A copy *generates* rather than reuses, even
  though it is copying.
- **The target date is validated like [#1](#e1)** — its own year, its own holiday check. Copying
  Monday onto a festival is refused. **This is where it differs from #1**, which *skips* a holiday:
  #1 takes a range and any range longer than about five days contains a weekly off, while a copy
  names one date and a caller who named a festival meant a different day.
- **The source must be in the same year.** A class belongs to exactly one, so last year's Monday
  names `classDocsId`s this year does not have — `409 DATE_OUTSIDE_ACADEMIC_YEAR` says that,
  instead of letting the structure step report a missing class.
- **A day cannot be built from itself** — `400 SOURCE_IS_TARGET`. Without a merge it is a no-op
  dressed as a write; with one it would duplicate every period onto itself and every duplicate
  would clash with the original.
- **Optional filters**: `classDocsId` copies one class, with `sectionNo` one section — which is
  what a school adding a section mid-term wants. **`sectionNo` alone is `400 SECTION_WITHOUT_CLASS`**:
  "section A" is not one thing across a school, the same pairing rule [#10](#e10) applies. A filter
  that matches nothing is `409 NOTHING_TO_COPY`, not an empty success.
- `409 TIMETABLE_ALREADY_EXISTS` unless the caller asks to merge, and **merging re-runs every
  conflict check against the combined list** — because a teacher free in Monday and free in Tuesday
  can still be in two places once Monday's periods are added to Tuesday's. Merging only ever
  **adds**; replacing a day whole is [#2](#e2).
- **`201` when it built a day, `200` when it merged into one.** The status says whether something
  came into being.
- **No `version` is required, unlike [#2](#e2).** A merge cannot erase a period the caller never
  saw. The save still carries `@Version`, so a writer that got in between the read and the write is
  `409 CONCURRENT_MODIFICATION` rather than a lost edit.

> **The structure step on a pure copy cannot currently refuse — measured 2026-09-17.** A copied
> period was already valid when it was written, so `normaliseAgainstStructure` can only reject it
> if the school's structure moved since — and no endpoint can move it that way: `SubjectUpdateRequest`
> has no `active` field, a section can be neither deactivated nor removed, and `loadClassForYear`
> returns a retired class deliberately. **It stays and is pinned at the source instead**, because
> [`structure`](../structure/README.md) will grow a deactivate, and a copy that silently carried a
> retired subject forward is exactly what this catches on the day it does.

<a id="e7"></a>
**[#7](#t7) · `GET /schools/current/academic-years/{year}/timetables/{date}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`; `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `_id`, `date`, `academicYear`, `version`, `entries[]`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name` — for `className`; `subjects[]` — for `subjectName`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `fullName` — for `teacherName`
- *reads*: `daily_timetables`, `school_classes`, `staff`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}
    /timetables/{date}

{year}  REQUIRED, the year's name
{date}  REQUIRED, ISO date

No body.
</pre></td>
<td><pre>
200 OK — DailyTimetableDetailResponse

{
  "dailyTimetableDocsId": "6ab1...",
  "date": "2026-08-03",
  "academicYear": "2026-27",
  "version": 5,
  "entryCount": 400,
  "lessonCount": 360,
  "classCount": 12,
  "sectionCount": 40,
  "teacherCount": 60,
  "entries": [
    {
      "timetableEntryId": "6ab2...",
      "periodCode": "P1",
      "classDocsId": "6aa...",
      "className": "Class 10",
      "sectionNo": "A",
      "slotType": "LESSON",
      "subjectCode": "MATHS",
      "subjectName": "Mathematics",
      "teacherDocsId": "6aa...",
      "teacherName": "R. Sharma",
      "startTime": "09:00:00",
      "endTime": "09:45:00"
    }
  ],
  "nextStep": "..."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. A year that has ended still answers. |
| `{date}` | **yes** | ISO date. The day to read. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday; the message names it |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |

- **The whole school's day**, entries in stored order — the order they were written, never
  re-sorted, the same call [#7 of grading](../grading/README.md#e7) made for bands.
- **Addressed by the date**, not by a document id. A caller always knows the date and never knows
  the id; `school_timetable_date_uniq` is what makes the date sufficient.
- **Every period carries the names behind its ids** — `className`, `subjectName`, `teacherName` —
  resolved in **two extra queries** rather than one request per id, which would be about seventy
  round trips for a day of four hundred periods across twelve classes and sixty staff. The subject
  name is resolved by the *same* class-wide rule [#1](#e1) enforces, so two sections under one
  `subjectCode` get two different names.
- **A name that cannot be found is left out and the id stays.** A class deleted or a staff member
  removed *after* the day was written must not stop last Tuesday from answering.
- **`roomDocsId` is not resolved.** #1 never checks that a room exists when it writes
  one, so a stored id may name nothing and a blank name would hide that — see
  [open item 3](#3-a-room-can-be-double-booked-across-two-collections).
- **The same five counts as a row of [#10](#e10)**, repeated because a caller reaching a day by a
  link never saw the list. They are worked out in memory, not by an aggregation: the entries are
  already loaded.
- **`404` when the day does not exist**, and that is not an error state: a holiday has no document.
  A holiday answers `404 NOT_A_WORKING_DAY` **naming the holiday**; a working day with nothing on
  it answers `404 TIMETABLE_NOT_FOUND`. Two codes, because "no timetable" and "the school is
  closed" are different facts and only one needs acting on.
- **A date outside `{year}` is `409 DATE_OUTSIDE_ACADEMIC_YEAR`**, the same refusal #1 gives.
- **No gate.** Last year's Tuesday still answers.

> **The stored year decides, not today's version of the year's range — measured 2026-09-17.**
> The day is read **first**, and what the document says about itself is what answers. Checking the
> date against the year's range *before* reading is tidier and was measured wrong: two academic
> years may never overlap, so the only way a stored day falls outside its own year is for the
> school to **edit the year's dates afterwards** — which it is allowed to do. A range shrunk past
> an already-written day made this endpoint answer `409` for a date [#10](#e10) was still listing.
> **Two reads of one module disagreeing about whether a day exists is worse than either answer.**
> The range now only ever explains an *absence*.

<a id="e8"></a>
**[#8](#t8) · `GET /schools/current/academic-years/{year}/timetables/{date}/sections/{classDocsId}/{sectionNo}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`; `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `sections[]` — the class must be in `{year}` and hold the section; `subjects[]` — for `subjectName`
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `_id`, `date`, `academicYear`, `entries[]` — only this section's are returned
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `fullName` — for `teacherName`
- *reads*: `daily_timetables`, `school_classes`, `staff`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}
    /timetables/{date}
    /sections/{classDocsId}/{sectionNo}

{year}         REQUIRED, the year's name
{date}         REQUIRED, ISO date
{classDocsId}  REQUIRED, a class in {year}
{sectionNo}    REQUIRED, any case — "a"
               answers as "A"

No body.
</pre></td>
<td><pre>
200 OK — SectionDayResponse

{
  "date": "2026-08-03",
  "academicYear": "2026-27",
  "dailyTimetableDocsId": "6ab1...",
  "classDocsId": "6aa...",
  "className": "Class 10",
  "sectionNo": "A",
  "entryCount": 8,
  "lessonCount": 7,
  "teacherCount": 6,
  "entries": [
    {
      "timetableEntryId": "6ab2...",
      "periodCode": "P1",
      "classDocsId": "6aa...",
      "className": "Class 10",
      "sectionNo": "A",
      "slotType": "LESSON",
      "subjectCode": "MATHS",
      "subjectName": "Mathematics",
      "teacherDocsId": "6aa...",
      "teacherName": "R. Sharma",
      "startTime": "09:00:00",
      "endTime": "09:45:00"
    }
  ],
  "nextStep": "..."
}

// entries are earliest first. No periods
// is a 200 with an empty list.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. A year that has ended still answers. |
| `{date}` | **yes** | ISO date. The day to read. |
| `{classDocsId}` | **yes** | A class of this school in `{year}`. Otherwise `404 CLASS_NOT_FOUND`. |
| `{sectionNo}` | **yes** | A section of that class, in any case. A retired section still answers. Otherwise `409 SECTION_NOT_IN_CLASS`. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `CLASS_NOT_FOUND` | the `classDocsId` is not a class of this school in `{year}` |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday; the message names it |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet |
| 409 | `SECTION_NOT_IN_CLASS` | the class has no section with that `sectionNo` |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |

- **What a child's parent opens.** Nobody outside the office wants the whole school's four hundred
  periods; they want the eight their child sits through.
- **Earliest first, where [#7](#e7) is in stored order.** #7 returns the whole school's day as
  stored and says why: periods of different sections run at the same hour, so "by time" there is a
  tie with a hidden second key. **That objection does not apply to one section** — a section cannot
  be in two places at once, so within one `startTime` is a *total* order, and it is the order a
  parent reads the day in.
- **An empty answer is a `200`.** The date has a timetable and this section has nothing in it: a
  fact about the section, not a missing document. The three 404s belong to the **day** and are the
  same three #7 gives.
- **"Nothing scheduled" and "wrong section" never look alike.** A mistyped `classDocsId` is
  `404 CLASS_NOT_FOUND`; a section the class does not hold is `409 SECTION_NOT_IN_CLASS`. Returning
  an empty list for either would leave a parent's app unable to tell "no school today" from "I
  asked for the wrong child".
- **A retired section still answers**, and this does *not* use `requireActiveSection` as every
  write does. No gate runs on a read: a section retired in March must not make February's Tuesday
  unreadable, because attendance taken against it has to stay explicable.
- **The names come resolved** — `className`, `subjectName`, `teacherName` — and the subject name
  follows the same class-wide rule [#1](#e1) enforces, so two sections under one `subjectCode` get
  two different names.
- **No gate.** Last year's Tuesday still answers.

> **The section case-fold in the filter cannot currently be reached — measured 2026-09-17.**
> Comparing the resolved spelling to the stored one with `equals` instead of `equalsIgnoreCase`
> passes every test, because [#1](#e1), [#2](#e2) and [#6](#e6) all run
> `normaliseAgainstStructure`, which rewrites every stored `sectionNo` to the class's own spelling.
> The case-insensitivity that *is* reachable is the lookup above it — asking for `a` answers, and
> answers `A`. **The fold stays and is pinned at the source**, for the reason #1's helper keeps
> its own: a comparison that is only correct because of what some other write did first is a trap
> for the next caller.

> **Filtered in memory, not with a projection — as the plan said.** The document is already
> loaded and measures hundreds of kilobytes at worst, and a `$elemMatch` projection returns only
> the **first** match, which is exactly wrong for a read whose whole job is every period of one
> section. The same is true of [#9](#e9).

<a id="e9"></a>
**[#9](#t9) · `GET /schools/current/academic-years/{year}/timetables/{date}/teachers/{teacherDocsId}`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`; `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id` — the person must be staff of this school; `fullName` — for `teacherName`
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `_id`, `date`, `academicYear`, `entries[]` — only this person's are returned
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name` — for `className`; `subjects[]` — for `subjectName`
- *reads*: `daily_timetables`, `school_classes`, `staff`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}
    /timetables/{date}
    /teachers/{teacherDocsId}

{year}           REQUIRED, the year's name
{date}           REQUIRED, ISO date
{teacherDocsId}  REQUIRED, staff of this
                 school

No body.
</pre></td>
<td><pre>
200 OK — TeacherDayResponse

{
  "date": "2026-08-03",
  "academicYear": "2026-27",
  "dailyTimetableDocsId": "6ab1...",
  "teacherDocsId": "6aa...",
  "teacherName": "R. Sharma",
  "entryCount": 6,
  "lessonCount": 5,
  "sectionCount": 4,
  "firstStartTime": "09:00:00",
  "lastEndTime": "14:45:00",
  "entries": [
    {
      "timetableEntryId": "6ab2...",
      "periodCode": "P1",
      "classDocsId": "6aa...",
      "className": "Class 10",
      "sectionNo": "A",
      "slotType": "LESSON",
      "subjectCode": "MATHS",
      "subjectName": "Mathematics",
      "teacherDocsId": "6aa...",
      "teacherName": "R. Sharma",
      "startTime": "09:00:00",
      "endTime": "09:45:00"
    }
  ],
  "nextStep": "..."
}

// entries are earliest first. A free day
// is a 200 with an empty list and no
// firstStartTime or lastEndTime.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. A year that has ended still answers. |
| `{date}` | **yes** | ISO date. The day to read. |
| `{teacherDocsId}` | **yes** | A staff member of this school. Otherwise `404 TEACHER_NOT_FOUND`. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |
| 404 | `TEACHER_NOT_FOUND` | the `teacherDocsId` is not staff of this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday; the message names it |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable yet |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year, or the date is outside `{year}` |

- **What a teacher's app opens.** One person, one date, in the order their day happens.
- **A break they supervise is in the list.** Since 2026-09-17 a non-lesson may carry a
  `teacherDocsId` — somebody supervises lunch, runs the assembly, takes the activity — and those
  periods count against their day. That is why `lessonCount` and `entryCount` differ: **a teacher
  with no lessons can still have a working day**, and a teacher named on a break cannot also be
  teaching period 4.
- **Earliest first**, for the reason [#8](#e8) is: a teacher cannot be in two places at once, so
  `startTime` is a total order within one person's day.
- **An unknown id is `404 TEACHER_NOT_FOUND`, not a free day.** An app that could not tell those
  apart would show an empty morning to somebody whose id it had got wrong. A **real** person with
  nothing that day is a `200` with an empty list and **no** `firstStartTime` or `lastEndTime` —
  absent rather than null, because there is no first period to report a time for.
- **`firstStartTime` and `lastEndTime` are not "free from" and "free until".** They bracket what
  this person is *committed to*; the gaps between are not computed here, and [#12](#e12) is the
  endpoint that answers coverage — against every member of staff rather than one.
- **`sectionCount` pairs class with section**, because "A" of one class and "A" of another are two.
- **Staff are not year-scoped**, so this endpoint reaches the day check where #8 stops at the class:
  asking for a day under a year it does not belong to is `409 DATE_OUTSIDE_ACADEMIC_YEAR` here and
  `404 CLASS_NOT_FOUND` there.
- **The read that makes a substitution checkable.** Once [#4](#e4) exists, this is where the
  covering teacher sees the period land.
- **No gate.** Last year's Tuesday still answers.

<a id="e10"></a>
**[#10](#t10) · `GET /timetables?from=&to=`** — built

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year must be one of this school's
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `_id`, `date`, `academicYear`, and `entries[]` inside the database only — to count them and to filter on `classDocsId`, `sectionNo`, `teacherDocsId`, `roomDocsId`
- *reads*: one page of the year's days, **as counts**

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}
    /timetables
    ?from=2026-08-01      // optional, ISO date
    &to=2026-08-31        // optional, ISO date
    &classDocsId=6aa...   // optional
    &sectionNo=A          // optional
    &teacherDocsId=6aa... // optional
    &roomDocsId=6aa...    // optional
    &page=0               // optional, 0 or more
    &size=20              // optional, 1 to 100
    &sort=date,desc       // optional, date ·
                          //   createdAt · updatedAt,
                          //   asc or desc

{year}  REQUIRED, the year's name

No body.
</pre></td>
<td><pre>
200 OK — PageResponse of
DailyTimetableSummaryResponse

{
  "content": [
    {
      "dailyTimetableDocsId": "6ab1...",
      "date": "2026-08-03",
      "academicYear": "2026-27",
      "entryCount": 400,
      "lessonCount": 360,
      "classCount": 12,
      "sectionCount": 40,
      "teacherCount": 60
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 22,
  "totalPages": 2,
  "hasNext": true,
  "hasPrevious": false
}

// No periods in a row. #7 has the day
// in full.
</pre></td>
</tr>
</table>

**Every field on the request.** Every filter is optional; absent means "do not filter on this".

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. A year that has ended still answers. |
| `from` | no | ISO date, the first date to include. Absent starts at the beginning of the year. |
| `to` | no | ISO date, the last date to include. Absent runs to the end of the year. |
| `classDocsId` | no | Days on which this class has a period. |
| `sectionNo` | no | Days on which this section has a period. Matched together with `classDocsId` as one period. |
| `teacherDocsId` | no | Days this person is named on a period, a supervised break included. |
| `roomDocsId` | no | Days on which this room is used. |
| `page` | no | 0 or more. Absent is 0. |
| `size` | no | 1 to 100. Absent is 20. |
| `sort` | no | `date`, `createdAt` or `updatedAt`, then `,asc` or `,desc`. Absent is `date` ascending. None of the counts can be sorted on. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_PAGE` | `page` is negative |
| 400 | `INVALID_PAGE_SIZE` | `size` is outside 1 to 100 |
| 400 | `INVALID_SORT_FIELD` | `sort` names a field that is not `date`, `createdAt` or `updatedAt` |
| 400 | `INVALID_SORT_DIRECTION` | anything but `asc` or `desc` |
| 400 | `TENANT_NOT_RESOLVED` | the school could not be worked out from the caller |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year called `{year}` in this school |

- **A list carries what a list needs.** A full day measures about **120 KB**, so a page of twenty carrying its periods would be two and a half megabytes shipped to render twenty dates and five numbers. The counts are computed by an **aggregation** and the `entries` array never leaves the database — the same call [#6 of grading](../grading/README.md#e6) makes about bands and [#53 of positions](../../people/department/README.md#e53) about holders. **[#7](#e7) is one call away** for a day in full.
- **The plan said "capped at a range a school actually views".** That cap is gone and is not needed: the response no longer grows with the size of a day, only with the number of rows, so ordinary paging is the whole answer. `?size=` is capped at 100 like every list here.
- **The five counts** are `entryCount`, `lessonCount`, `classCount`, `sectionCount` and `teacherCount`. **`sectionCount` pairs the class with the section** — "A" of one class and "A" of another are two sections, and counting the bare `sectionNo` would report a twelve-class school as having three.
- **None of the counts is sortable**, and that is not an oversight: they are not on the document, so ordering by one would mean counting the whole year before choosing a page. "The busiest day" is a different endpoint.
- **`?from=` and `?to=` are independent.** Either alone is meaningful, neither is required, and a window with nothing in it is an **empty page, never a `404`** — the plan's "missing days are absent, not empty" rule, which falls out of the query rather than needing to be written.
- **`?classDocsId=` and `?sectionNo=` are matched as ONE period**, via `$elemMatch`. A day holds every class's periods, so two separate conditions would match a day where one entry belongs to class X and an unrelated entry belongs to some other class's section B — which is nearly every day.
- **`?teacherDocsId=` counts a break they supervise**, which is the point of [#1](#e1) allowing a teacher on a non-lesson.
- **A teacher's absence is not a person.** The filter behind `teacherCount` tests `$type` rather than `$ne null`: an absent field is *absent* in these documents, and in an **aggregation expression** `$ne` against null evaluates **true** for a missing value — the opposite of the query language. Measured on a day with one lesson and one unsupervised break: `$ne null` counted two teachers, `$type` counted one.
- **No gates**, and a year the school has **ended still answers** — reading last year's Tuesday is how an attendance record taken against it gets explained.

<a id="e11"></a>
**[~~#11~~](#t11) · ~~`GET /timetables/{date}/rooms/{roomDocsId}`~~** — not being built · **removed 2026-10-15**

- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: would have read the day's `entries[]` whose `roomDocsId` is that room — nothing does this now

One room's day. It was built and then removed the same day: **not needed yet**. Rooms
(`Room`) have no API, so the room id could not even be checked, and nothing in the
product asks the question today. **The number is retired, not reused** — if a room's day is wanted
later, it comes back as #11. `school_timetable_room_idx` stays on the model for it.

<a id="e12"></a>
**[#12](#t12) · `GET /timetables/{date}/free-teachers?startTime=&endTime=`** — built 2026-10-15

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name`; `startDate`, `endDate`, `holidays` — only to explain a missing day
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `_id`, `date`, `academicYear`; `entries[]` — `teacherDocsId`, `slotType`, `startTime`, `endTime` of every period
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `_id`, `fullName`, `employeeNo` of every staff member of the school
- *reads*: the day, and **every staff member of the school** (`StaffRepository.findBySchoolId`)

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}
    /timetables/{date}/free-teachers
    ?startTime=10:30      // REQUIRED, HH:mm
    &endTime=11:15        // REQUIRED, HH:mm,
                          //   after startTime

{year}  REQUIRED, the year's name
{date}  REQUIRED, ISO date

No body.
</pre></td>
<td><pre>
200 OK — FreeTeachersResponse

{
  "date": "2026-11-02",
  "academicYear": "2026-27",
  "dailyTimetableDocsId": "6ab1...",
  "startTime": "10:30:00",
  "endTime": "11:15:00",
  "staffCount": 5,
  "freeCount": 3,
  "teachers": [
    {
      "staffDocsId": "6aa...",
      "fullName": "P. Iyer",
      "employeeNo": "EMP-014",
      "periodCount": 2,
      "lessonCount": 2
    }
  ],
  "nextStep": "Free means nothing on ..."
}

// nextStep also says that free does NOT
// mean at work.

// teachers: least busy first, then by name.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` | **yes** | The academic year's name. Must be a year of this school. |
| `{date}` | **yes** | ISO date. The day to check. |
| `startTime` | **yes** | `HH:mm`. The start of the period to cover. Absent is `400 MISSING_PARAMETER`. |
| `endTime` | **yes** | `HH:mm`. The end of the period to cover. Must be after `startTime`. Absent is `400 MISSING_PARAMETER`. |

**Refusals** — copied from the controller method's javadoc.

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_TIME_WINDOW` | `startTime` is not before `endTime` |
| 400 | `MISSING_PARAMETER` | no `startTime` or no `endTime` |
| 400 | `INVALID_PARAMETER` | a time that is not `HH:mm`, or a date that is not ISO |
| 400 | `TENANT_NOT_RESOLVED` | no idtoken cookie |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | no year of that name in this school |
| 404 | `NOT_A_WORKING_DAY` | the date is a holiday or weekly off |
| 404 | `TIMETABLE_NOT_FOUND` | a working day with no timetable written yet |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | the stored day belongs to another year |

- **The 07:40 query.** A teacher is absent and somebody has to decide who covers period 3.
- **Free means "has no period overlapping this window on this date"** — computed from the day
  already loaded, not stored. A period overlaps when it starts before the window ends and ends after
  it starts, so one ending exactly at `startTime` does **not** count. A supervised break **does**.
- **It cannot mean "is at work".** Leave has no endpoints, and nothing marks which staff teach —
  that is on positions and employment records, not built — so **every staff member is the pool**
  and somebody on leave reads as free. **The response says so** in `nextStep`.
- Each free person comes back with `periodCount` and `lessonCount` for the whole day, **least busy
  first**, because "free" and "free but already teaching six periods" are different answers.
- Refusals: `400 INVALID_TIME_WINDOW` (start not before end), `400 MISSING_PARAMETER`, `400
  INVALID_PARAMETER` (not `HH:mm`), plus the day-level ones. No gate.
- Measured 2026-10-15 on 2 Nov 2026: 16:30–17:00 → 3 of 5 free; 17:09–17:10 → the teacher whose
  lesson ends at 17:09 is free.

