# controllers/academics/attendance — API plan

**Three of eleven are built — [#1](#e1), [#6](#e6) and [#7](#e7), on 2026-10-15.** This is the full set of endpoints student attendance needs, written before
any of them so they can be built and reviewed one at a time — the same way
[`timetable`](../timetable/README.md), [`student`](../../student/README.md) and
[`crm`](../../crm/README.md) were done. Built endpoints will be marked **built** in the `#` column.

Mirrors [`models/academics/attendance`](../../../models/academics/attendance) — two collections,
four enums, already written. The model README's
[Attendance section](../../../models/academics/README.md#attendance) is the persistence contract;
this file is what may be done to it over HTTP.

**Written 2026-10-15.** [Open items](#to-settle-before-building) need an answer before the first
endpoint is built — item 1 and item 4 change what #1 and #4 do.

---

## What this module is

**A register per section per date.** A teacher opens the register for 7B on Monday, marks every
child present, late or absent, and locks it. The office sees which sections have not taken
attendance yet, follows up the absentees, and a term later prints each child's percentage.

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — **the
  register header.** One per section, date and kind: `DAILY` (once for the day) or `PERIOD` (one
  timetable period). Unique on `{schoolId, academicYear, attendanceDate, classDocsId, sectionNo,
  sessionType, periodCode}`. Moves `OPEN` → `LOCKED`, or → `CANCELLED`.
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java)
  — **one child's mark in one register.** Unique on `{schoolId, attendanceSessionDocsId,
  studentDocsId}`, so re-sending a mark updates it rather than duplicating it. Status is one of
  `PRESENT`, `LATE`, `AUTHORISED_ABSENCE`, `UNAUTHORISED_ABSENCE`, and **every status other than
  `PRESENT` needs a `reason`** — the model enforces that.

**The marks are separate documents on purpose**, so the register does not grow with the class and
two teachers can mark at once without overwriting each other.

## What this module is not

- **Not staff attendance.** That belongs to `people`. Hostel night roll calls belong to `hostel`, and
  mess attendance (`MessAttendance`) to `mess`. Exam-hall attendance is
  [`ExamAttendance`](../../../models/academics/examination/ExamAttendance.java), with examinations.
- **Not guardian notifications.** `guardianNotified` and `guardianAcknowledgedAt` are on the model,
  but sending anything is the notification system's job, and that is designed last. Here they are
  read-only, for the follow-up list ([#11](#t11)).
- **Not biometric or QR capture.** `AttendanceSource` has `QR_CODE`, `BIOMETRIC`, `IMPORT` and
  `INTEGRATION`, but every endpoint below writes `MANUAL`. The others arrive with the devices.

## Who is in the register

**The children of a section on a date are the ones whose academic record covered that date**:
`classDocsId` and `sectionNo` match, `effectiveFrom ≤ date`, and `effectiveUntil` is empty or
`≥ date`, in any status except `CANCELLED`. **Not "whoever is ACTIVE today"** — a child who
transferred out of 7B on Wednesday was still in Monday's register, and a register re-opened next
month has to show them.

Each mark stores the `studentAcademicRecordDocsId` it was taken under, so a later class change never
rewrites history. This is the same question the roster ([`student` #21](../../student/README.md#t21))
answers — see [open item 1](#1-the-roster-question-is-student-21s).

## Which gates every endpoint runs

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | every write |
| **2** | Is the subscription usable | every write |
| **4** | Is the year in the path the running one | every write |

**Gate 4 runs**, like the timetable: attendance is taken in a running year. Correcting last year's
register after the year has ended is [open item 6](#6-correcting-a-register-after-the-year-has-ended).
**No gate runs on a read.**

**Every path is under `/schools/current/academic-years/{year}`** — a register *is* a year's.

---

# The endpoints

Numbered by area, not by build order. **Build order is below** and differs.

## 1. The register — writes

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /attendance-sessions`](#e1) | **Open a register** for a section on a date — daily, or one period. | `attendance_sessions`, `academic_years`, `school_classes`, `daily_timetables` |
| <a id="t2"></a>2 | [`PUT /attendance-sessions/{id}/marks`](#e2) | **Mark the children** — one, several or all at once. Re-sending a child's mark corrects it. | `student_attendance_records`, `student_academic_records` |
| <a id="t3"></a>3 | [`POST /attendance-sessions/{id}/lock`](#e3) | **Lock it** — the register is final. | `attendance_sessions`, `student_attendance_records` |
| <a id="t4"></a>4 | [`POST /attendance-sessions/{id}/unlock`](#e4) | Re-open a locked register to correct it, with a reason. | `attendance_sessions` |
| <a id="t5"></a>5 | [`POST /attendance-sessions/{id}/cancel`](#e5) | Cancel a register opened by mistake. Its marks stop counting. | `attendance_sessions` |

## 2. The register — reads

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t6"></a>6 — **built** | [`GET /attendance-sessions/{id}`](#e6) | **One register in full** — every child who belongs in it, their mark or "not marked yet". What the teacher's screen opens. | `attendance_sessions`, `student_attendance_records`, `student_academic_records`, `students` |
| <a id="t7"></a>7 — **built** | [`GET /attendance-sessions?date=&classDocsId=&sectionNo=&sessionType=&status=`](#e7) | Registers, filtered and paged, with counts per status. | `attendance_sessions`, `student_attendance_records` |

## 3. Reports

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t8"></a>8 | [`GET /attendance/day?date=`](#e8) | **The office's morning question**: which sections have taken attendance today and which have not. | `school_classes`, `attendance_sessions` |
| <a id="t9"></a>9 | [`GET /students/{studentDocsId}/attendance?from=&to=`](#e9) | **One child's attendance** — every mark in a range, and the counts and percentage. | `student_attendance_records` |
| <a id="t10"></a>10 | [`GET /attendance/sections/{classDocsId}/{sectionNo}?from=&to=`](#e10) | **The monthly register** — one row per child, counts and percentage, one aggregation. | `student_attendance_records`, `student_academic_records`, `students` |
| <a id="t11"></a>11 | [`GET /attendance/absentees?date=`](#e11) | **Today's absentees**, for follow-up. Who has not been told yet is visible. | `student_attendance_records`, `students` |

---

# Build order

**#1, #2, #6 first** — open a register, mark it, read it back. That is attendance working end to
end, and everything else reads what those three write. **Then #3** (lock), **then #8** (who has not
taken it), **then #9 and #10** (the numbers a school is asked for), **then #11**, **then #4 and #5**
(correction paths), and **#7 last**.

**Before #1: [open item 1](#1-the-roster-question-is-student-21s).** The "who is in the register"
read is the roster, and it should be written once.

---

# The endpoints in detail

<a id="e1"></a>
**[1](#t1) · `POST /attendance-sessions`** — built 2026-10-15 — *open a register*

**Built with [open item 2](#2-re-opening-a-cancelled-register) decided as (a)**: a `CANCELLED`
register is opened again — `200`, `reopened: true`, marks kept — rather than refused. Nothing can
cancel one until [#5](#e5) exists, so the branch is unreachable today. The teacher is checked only
when sent; a defaulted one that is no longer staff is kept as an id with no name.

Measured 2026-10-15 with no writes: every refusal below answered as documented — `1 Nov 2026` is
*"Weekly Off"*, a 2030 date is outside `2026-2027`, `P9` is not in Class 1 A's timetable, and the
`2020-2026` year is refused by gate 4.

| Field | Type | Required | Notes |
|---|---|---|---|
| `attendanceDate` | LocalDate | **yes** | Must be inside the year and a **working day** — a date in `AcademicYear.holidays` is refused. No weekday is assumed to be off. |
| `classDocsId` | String | **yes** | A class of this year. |
| `sectionNo` | String | **yes** | A section of that class, and active. |
| `sessionType` | AttendanceSessionType | **yes** | `DAILY` or `PERIOD`. |
| `periodCode` | String | **for `PERIOD`** | Must be a period that section has in that day's timetable. Refused on `DAILY`. |
| `teacherDocsId` | String | no | Defaults to the period's teacher for `PERIOD`; for `DAILY`, the section's class teacher when one is set. |

**Not `timetableDocsId` from the caller** — for a `PERIOD` register it is the id of the day's
timetable the period was found in, so it cannot disagree with it. The period itself is found by the
register's class, section and `periodCode` (decided 2026-10-15). **The subject is not stored
on the register** (removed 2026-10-15): it is the timetable entry's, read from there when wanted.

**Opening the same register twice is a 409 that names the existing one** (`SESSION_ALREADY_OPEN`),
so a second teacher's screen goes to it rather than failing. The unique index is the real guard.

| Refusal | When |
|---|---|
| `404 ACADEMIC_YEAR_NOT_FOUND` · `CLASS_NOT_FOUND` | |
| `400 SECTION_NOT_IN_CLASS` · `409 SECTION_NOT_ACTIVE` | |
| `404 NOT_A_WORKING_DAY` | The date is a holiday or weekly off in the year's calendar. The message names it. |
| `409 DATE_OUTSIDE_ACADEMIC_YEAR` | |
| `400 PERIOD_CODE_REQUIRED` · `400 PERIOD_CODE_NOT_ALLOWED` | `PERIOD` without one; `DAILY` with one. |
| `404 PERIOD_NOT_IN_TIMETABLE` | That section has no such period on that date — or no timetable that day. |
| `404 TEACHER_NOT_FOUND` | `teacherDocsId` sent, and not this school's staff. |
| `409 SESSION_ALREADY_OPEN` | The register exists, `OPEN` or `LOCKED`. **The message carries its id.** |
| `409 ACADEMIC_YEAR_NOT_RUNNING` | Gate 4 — the year in the path is not the running one. |

<a id="e2"></a>
**[2](#t2) · `PUT /attendance-sessions/{id}/marks`** — *mark the children*

```json
{ "marks": [
    { "studentDocsId": "…", "status": "PRESENT" },
    { "studentDocsId": "…", "status": "LATE", "reason": "BUS", "arrivalAt": "2026-06-05T03:02:00Z" },
    { "studentDocsId": "…", "status": "AUTHORISED_ABSENCE", "reason": "MEDICAL" } ] }
```

- **Each row is an upsert** on the unique index — sending a child again corrects their mark. Rows not
  sent are left alone, so two teachers can each mark half the class.
- **Every child must belong in the register on that date** ([who is in the register](#who-is-in-the-register)),
  or the whole request is refused naming them (`STUDENT_NOT_IN_REGISTER`) — nothing is half-written.
- **`reason` is required unless `PRESENT`**, the model's own rule, checked per row with the row
  named (`REASON_REQUIRED`). A blank reason is treated as missing.
- `attendanceDate`, `academicYear` and `studentAcademicRecordDocsId` are **copied by the server**,
  never sent.
- **Only an `OPEN` register takes marks** — `409 SESSION_NOT_OPEN` on `LOCKED` or `CANCELLED`.
- **"Mark everybody else present"** — see [open item 4](#4-unmarked-children-when-a-register-is-locked).

No `version`: each row is its own document and the rows are independent. Two people correcting
**the same child** at once is last-write-wins — [open item 3](#3-two-people-correcting-the-same-child).

<a id="e3"></a>
**[3](#t3) · `POST /attendance-sessions/{id}/lock`** — `{ "version": n }`. Sets `LOCKED`,
`lockedAt`, `lockedByDocsId`. **Refused while any child is unmarked** (`UNMARKED_STUDENTS`, with the
count) — see [open item 4](#4-unmarked-children-when-a-register-is-locked). Locking a locked register
is `409 SESSION_NOT_OPEN`.

<a id="e4"></a>
**[4](#t4) · `POST /attendance-sessions/{id}/unlock`** — `{ "reason": "…", "version": n }`. Back to
`OPEN`, clearing `lockedAt` and `lockedByDocsId`. **The reason is required**, because re-opening a
final register is exactly what somebody will ask about later. See
[open item 5](#5-who-may-unlock).

<a id="e5"></a>
**[5](#t5) · `POST /attendance-sessions/{id}/cancel`** — `{ "version": n }`. `OPEN` or `LOCKED` →
`CANCELLED`. **The marks are kept, not deleted**, and every report leaves a cancelled register out.
A cancelled register frees nothing in the unique index — re-opening the same register is
[open item 2](#2-re-opening-a-cancelled-register).

<a id="e6"></a>
**[6](#t6) · `GET /attendance-sessions/{id}`** — built 2026-10-15 — the header, then **one row per child who belongs in
it**: name, roll number, and their mark or `null` for "not marked yet". Counts per status and
`unmarkedCount`. Three reads — the session, the marks, the roster — plus the names, never one per row.

<a id="e7"></a>
**[7](#t7) · `GET /attendance-sessions`** — built 2026-10-15 — paged; filters `date`, `classDocsId`, `sectionNo`,
`sessionType`, `status`. Each row carries counts per mark status from **one aggregation for the
page**.

<a id="e8"></a>
**[8](#t8) · `GET /attendance/day?date=`** — **every active section of the year**, and for each:
whether a `DAILY` register exists, its status, and its counts. **"Not taken" is a row, not an
absence** — the whole point is seeing the sections nobody has marked. One read of the classes, one of
the day's sessions, one aggregation of the marks.

<a id="e9"></a>
**[9](#t9) · `GET /students/{studentDocsId}/attendance?from=&to=`** — every mark in the range, newest
first, with counts per status and a percentage. Served by `school_student_attendance_history_idx`.
**Cancelled registers are left out.** How `LATE` counts is
[open item 7](#7-does-late-count-as-present).

<a id="e10"></a>
**[10](#t10) · `GET /attendance/sections/{classDocsId}/{sectionNo}?from=&to=`** — one row per child
who was in the section during the range, with counts per status and a percentage, from **one
aggregation**. What a school prints at the end of the month.

<a id="e11"></a>
**[11](#t11) · `GET /attendance/absentees?date=`** — every `AUTHORISED_ABSENCE` and
`UNAUTHORISED_ABSENCE` that day, with the child, the section, the reason and
`guardianNotified`. Served by `school_date_attendance_status_idx`. **Read-only on the notification
flags** — setting them is the notification system's.

---

# To settle before building

## 1. The roster question is [`student` #21](../../student/README.md#t21)'s

**Partly answered 2026-10-15.** The query now exists once —
[`StudentAcademicRecordRepository.findInSectionOn`](../../../repositories/student/academicrecord/StudentAcademicRecordRepositoryCustom.java),
with the date rule above, both ends inclusive — and #6 uses it. When `student` #21 is built it should
call the same method rather than write its own.

"Which children were in 7B on 5 June" is the read #1, #2, #6 and #10 all make, and it is exactly
what the roster endpoint answers. **Recommendation: build `student` #21 first, with the date rule
above, and put the query in one shared place both modules call** — so attendance and the roster can
never disagree about who was in a section.

## 2. Re-opening a cancelled register

The unique index includes no status, so a `CANCELLED` register blocks opening the same one again.
**Options:** (a) #1 re-opens the cancelled one as `OPEN` instead of refusing; (b) refuse, and the
marks of a mistaken register stay where they are. **Recommendation: (a)**, keeping its marks — a
register cancelled by mistake is the likeliest thing to be re-opened.

## 3. Two people correcting the same child

Each mark is an upsert with no version, so two corrections of one child at once — last one wins,
silently. **Recommendation: accept it**, because the alternative (a version per row in #2's body)
makes marking a class of forty unusable, and the register's `updatedByDocsId` still shows who
changed it last.

## 4. Unmarked children when a register is locked

**Options:** (a) #3 refuses while anyone is unmarked; (b) #3 marks them `PRESENT` silently; (c) #2
takes `"markRemainingAs": "PRESENT"` and #3 refuses. **Recommendation: (c)** — the teacher's
common case ("everyone is here except three") is one request, and nobody is ever marked present by
a lock nobody looked at.

## 5. Who may unlock

There is no authorization anywhere yet, so anyone can. **Recommendation:** build #4 with the
required reason, and let roles decide later — that is the same as every other write in this project
today.

## 6. Correcting a register after the year has ended

Gate 4 refuses every write in a year that is not running, so a mistake found in July about March
cannot be corrected. **Recommendation: keep gate 4** for now — the same answer the timetable gave —
and revisit when report cards need it.

## 7. Does `LATE` count as present?

The enum's own note says "if school policy counts it", and no setting exists. **Recommendation:**
count `LATE` as **attended** in every percentage, and report it separately so a school that wants
otherwise can see it. A school setting can come later.

## 8. `DAILY` and `PERIOD` on the same day

Both can exist for one section on one date. **Recommendation:** allow both; the day report (#8) and
the percentages (#9, #10) count **`DAILY` registers only**, and period registers are for subject
attendance. A school that only takes period attendance is a later question.

---

# Where the code will live

```text
controllers/academics/attendance/
    AttendanceSessionController.java   #1–#7
    AttendanceReportController.java    #8–#11

services/academics/
    AttendanceSessionService.java
    AttendanceReportService.java
    utils/   the roster-on-a-date read, if not shared with student #21

repositories/academics/attendance/          (AttendanceSessionRepository exists)
    StudentAttendanceRecordRepository.java  (+ Custom/Impl for the aggregations)

dto/academics/attendance/{request,response}/
```

---

# The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `SESSION_ALREADY_OPEN` | 409 | [#1](#e1) for a register that exists. **Names its id.** |
| `PERIOD_CODE_REQUIRED` · `PERIOD_CODE_NOT_ALLOWED` | 400 | [#1](#e1) — `PERIOD` without a period, `DAILY` with one. |
| `PERIOD_NOT_IN_TIMETABLE` | 404 | [#1](#e1) — the section has no such period that day. |
| `ATTENDANCE_SESSION_NOT_FOUND` | 404 | No register with that id in this school. |
| `SESSION_NOT_OPEN` | 409 | [#2](#e2)/[#3](#e3)/[#5](#e5) on a register that is locked or cancelled. |
| `STUDENT_NOT_IN_REGISTER` | 400 | [#2](#e2) — a child who was not in that section on that date. **Names them.** |
| `REASON_REQUIRED` | 400 | [#2](#e2) — a non-`PRESENT` mark without a reason. **Names the child.** |
| `UNMARKED_STUDENTS` | 409 | [#3](#e3) — children still unmarked. **Says how many.** |
| `SESSION_NOT_LOCKED` | 409 | [#4](#e4) on a register that is not locked. |
| `NOT_A_WORKING_DAY` · `DATE_OUTSIDE_ACADEMIC_YEAR` | 404 · 409 | Shared with the timetable. |
