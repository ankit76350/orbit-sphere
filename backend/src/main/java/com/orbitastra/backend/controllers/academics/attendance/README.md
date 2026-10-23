# controllers/academics/attendance — API plan

**Ten of eleven are built — [#1](#e1) to [#4](#e4) and [#6](#e6) to [#11](#e11), on 2026-10-15. Only [#5](#e5) cancel is left.**

This is the full set of endpoints student attendance needs, written before
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

| Document | Collection | What it holds |
|---|---|---|
| [`AttendanceSession`](../../../models/academics/attendance/AttendanceSession.java) | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) | **the register header.** One per section, date and kind: `DAILY` (once for the day) or `PERIOD` (one timetable period). Unique on `{schoolId, academicYear, attendanceDate, classDocsId, sectionNo, sessionType, periodCode}`. Moves `OPEN` → `LOCKED`, or → `CANCELLED`. |
| [`StudentAttendanceRecord`](../../../models/academics/attendance/StudentAttendanceRecord.java) | [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) | **one child's mark in one register.** Unique on `{schoolId, attendanceSessionDocsId, studentDocsId}`, so re-sending a mark updates it rather than duplicating it. Status is one of `PRESENT`, `LATE`, `AUTHORISED_ABSENCE`, `UNAUTHORISED_ABSENCE`, and **every status other than `PRESENT` needs a `reason`** — the model enforces that. |

**The marks are separate documents on purpose**, so the register does not grow with the class and
two teachers can mark at once without overwriting each other.

## What this module is not

- **Not staff attendance.** That belongs to `people`. Hostel night roll calls belong to `hostel`, and
  mess attendance (`MessAttendance`) to `mess`. Exam-hall attendance is
  [`ExamCandidate.papers`](../../../models/academics/examination/ExamCandidate.java), with examinations.
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

**What the marker in the `#` column means**, the same four words [`controllers/core`](../../core/README.md)
uses:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. 10 of them. |
| **deferred** | Planned, not started, and not next. |
| **optional** | A convenience nobody is waiting on. |
| **not being built** | Decided against, struck through, and the reason is in its own section. |

Anything unmarked does not exist yet, and a request to it returns a 404.

## 1. The register — writes · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>#1 — **built** | [`POST /attendance-sessions`](#e1) | **Open a register** for a section on a date — daily, or one period. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`academic_years`](../../../models/core/AcademicYear.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) |
| <a id="t2"></a>#2 — **built** | [`PUT /attendance-sessions/{id}/marks`](#e2) | **Mark the children** — one, several or all at once. Re-sending a child's mark corrects it. | [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java), [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) |
| <a id="t3"></a>#3 — **built** | [`POST /attendance-sessions/{id}/lock`](#e3) | **Lock it** — the register is final. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) |
| <a id="t4"></a>#4 — **built** | [`POST /attendance-sessions/{id}/unlock`](#e4) | Re-open a locked register to correct it, with a reason. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) |
| <a id="t5"></a>#5 | [`POST /attendance-sessions/{id}/cancel`](#e5) | Cancel a register opened by mistake. Its marks stop counting. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) |

## 2. The register — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t6"></a>#6 — **built** | [`GET /attendance-sessions/{id}`](#e6) | **One register in full** — every child who belongs in it, their mark or "not marked yet". What the teacher's screen opens. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java), [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`students`](../../../models/student/Student.java) |
| <a id="t7"></a>#7 — **built** | [`GET /attendance-sessions?date=&classDocsId=&sectionNo=&sessionType=&status=`](#e7) | Registers, filtered and paged, with counts per status. | [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) |

## 3. Reports · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t8"></a>#8 — **built** | [`GET /attendance/day?date=`](#e8) | **The office's morning question**: which sections have taken attendance today and which have not. | [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) |
| <a id="t9"></a>#9 — **built** | [`GET /students/{studentDocsId}/attendance?from=&to=`](#e9) | **One child's attendance** — every mark in a range, and the counts and percentage. | [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) |
| <a id="t10"></a>#10 — **built** | [`GET /attendance/sections/{classDocsId}/{sectionNo}?from=&to=`](#e10) | **The monthly register** — one row per child, counts and percentage, one aggregation. | [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java), [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`students`](../../../models/student/Student.java) |
| <a id="t11"></a>#11 — **built** | [`GET /attendance/absentees?date=`](#e11) | **Today's absentees**, for follow-up. Who has not been told yet is visible. | [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java), [`students`](../../../models/student/Student.java) |

---

<a id="build-order"></a>
# Build order

**#1, #2, #6 first** — open a register, mark it, read it back. That is attendance working end to
end, and everything else reads what those three write. **Then #3** (lock), **then #8** (who has not
taken it), **then #9 and #10** (the numbers a school is asked for), **then #11**, **then #4 and #5**
(correction paths), and **#7 last**.

| Step | What it gives you | Endpoints | |
|---|---|---|---|
| **1** | Attendance working end to end — open a register, mark it, read it back | [#1](#t1), [#2](#t2), [#6](#t6) | built |
| **2** | The register can be locked | [#3](#t3) | built |
| **3** | Who has not taken attendance | [#8](#t8) | built |
| **4** | The numbers a school is asked for | [#9](#t9), [#10](#t10) | built |
| **5** | Today's absentees | [#11](#t11) | built |
| **6** | Correction paths | [#4](#t4) built; [#5](#t5) not built | part |
| **7** | The register list | [#7](#t7) | built |

**Before #1: [open item 1](#1-the-roster-question-is-student-21s).** The "who is in the register"
read is the roster, and it should be written once.

---

<a id="the-endpoints-in-detail"></a>
# Appendix — what every API touches, field by field

<a id="e1"></a>
**[#1](#t1) · `POST /attendance-sessions`** — built 2026-10-15 — *open a register*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the date must be inside; `holidays` — the date must be a working day
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `sections[].sectionNo`, `sections[].active` — the section must exist and be active; `sections[].classTeacherDocsId` — the default teacher for `DAILY`; `name`
- [`daily_timetables`](../../../models/academics/timetable/DailyTimetable.java) — *reads*: `date`, `entries[].periodCode`, `entries[].teacherDocsId` — for `PERIOD` only
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: the `teacherDocsId` when sent — must be this school's staff
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: the unique key — an existing register is `SESSION_ALREADY_OPEN`, a `CANCELLED` one is opened again
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *inserts*: `academicYear`, `attendanceDate`, `classDocsId`, `sectionNo`, `sessionType`, `periodCode`, `timetableDocsId`, `teacherDocsId`, `status` = `OPEN`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/attendance-sessions
Cookie:  idtoken=…   REQUIRED — names the school

{
  "attendanceDate": "2026-10-15",   // REQUIRED
  "classDocsId": "6a9c…",           // REQUIRED, max 60
  "sectionNo": "A",                 // REQUIRED, max 20
  "sessionType": "PERIOD",          // REQUIRED, DAILY or PERIOD
  "periodCode": "P1",               // PERIOD only, max 20
  "timetableDocsId": "6a9d…",       // optional, PERIOD only, max 60
  "teacherDocsId": "6a9b…"          // optional, max 60
}
</pre></td>
<td><pre>
201 Created — AttendanceSessionResponse
   (200 OK when a CANCELLED register is opened again)
Location: /schools/current/academic-years/2026-2027/attendance-sessions/6a9f…

{
  "attendanceSessionDocsId": "6a9f…",
  "academicYear": "2026-2027",
  "attendanceDate": "2026-10-15",
  "classDocsId": "6a9c…",
  "className": "Class 1",
  "sectionNo": "A",
  "sessionType": "PERIOD",
  "periodCode": "P1",
  "timetableDocsId": "6a9d…",
  "teacherDocsId": "6a9b…",
  "teacherName": "…",
  "status": "OPEN",
  "reopened": false,
  "version": 0,
  "nextStep": "…"
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `year` (path) | **yes** | The year's name. Must be the running year (gate 4). |
| `attendanceDate` | **yes** | A date inside the year, and a working day. |
| `classDocsId` | **yes** | Max 60. A class of the year in the path. |
| `sectionNo` | **yes** | Max 20. A section of that class, and active. |
| `sessionType` | **yes** | `DAILY` or `PERIOD`. |
| `periodCode` | **for `PERIOD`** | Max 20. A period that section has in that day's timetable. Refused on `DAILY`. |
| `timetableDocsId` | no | Max 60, `PERIOD` only. Absent: the server finds the day's timetable. Sent: must be that date's timetable. |
| `teacherDocsId` | no | Max 60. Absent: the period's teacher for `PERIOD`, the section's class teacher for `DAILY`. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `CLASS_NOT_FOUND` | No class of that id in this year. |
| 400 | `SECTION_NOT_IN_CLASS` | The class has no section by that `sectionNo`. |
| 409 | `SECTION_NOT_ACTIVE` | The section has been switched off. |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | The date is not inside the year. |
| 404 | `NOT_A_WORKING_DAY` | The date is a holiday or weekly off — the message names it. |
| 400 | `PERIOD_CODE_REQUIRED` | `PERIOD` with no `periodCode`. |
| 400 | `PERIOD_CODE_NOT_ALLOWED` | `DAILY` with a `periodCode`. |
| 404 | `PERIOD_NOT_IN_TIMETABLE` | The section has no such period that day, or no timetable. |
| 400 | `TIMETABLE_NOT_FOR_DATE` | `timetableDocsId` sent, and not that date's timetable. |
| 400 | `TIMETABLE_DOCS_ID_NOT_ALLOWED` | `DAILY` with a `timetableDocsId`. |
| 404 | `TEACHER_NOT_FOUND` | `teacherDocsId` sent, and not this school's staff. |
| 409 | `SESSION_ALREADY_OPEN` | The register exists — the message carries its id. |
| 400 | `VALIDATION_FAILED` | A required field is missing. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed (gate 1, 2). |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year is not the one the school is running (gate 4). |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

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
| `timetableDocsId` | String | no, **`PERIOD` only** | The day's timetable. Sent: must be the timetable of `attendanceDate` (`TIMETABLE_NOT_FOR_DATE`). Absent: found by the server. Refused on `DAILY`. |
| `teacherDocsId` | String | no | Defaults to the period's teacher for `PERIOD`; for `DAILY`, the section's class teacher when one is set. |

**`timetableDocsId` may be sent** (since 2026-10-15) — the section screen fills it when a period is
picked — but it is **checked, not trusted**: it must be the timetable of that date. The period itself is found by the
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
| `400 TIMETABLE_NOT_FOR_DATE` | `timetableDocsId` sent, and not the timetable of that date. **Names the right one.** |
| `400 TIMETABLE_DOCS_ID_NOT_ALLOWED` | `DAILY` with a `timetableDocsId`. |
| `404 TEACHER_NOT_FOUND` | `teacherDocsId` sent, and not this school's staff. |
| `409 SESSION_ALREADY_OPEN` | The register exists, `OPEN` or `LOCKED`. **The message carries its id.** |
| `409 ACADEMIC_YEAR_NOT_RUNNING` | Gate 4 — the year in the path is not the running one. |

<a id="e2"></a>
**[#2](#t2) · `PUT /attendance-sessions/{id}/marks`** — built 2026-10-15 — *mark the children*

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `status` — must be `OPEN`; `version`; `attendanceDate`, `classDocsId`, `sectionNo`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `effectiveFrom`, `effectiveUntil`, `status` — who belongs in the register on that date
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *inserts or updates* (one upsert per child): `attendanceSessionDocsId`, `studentDocsId`, `studentAcademicRecordDocsId`, `attendanceDate`, `status`, `reason`, `arrivalAt`, `source` = `MANUAL`
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *updates*: `version` — moved on with every save
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo` — for the answer

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}/attendance-sessions/{attendanceSessionDocsId}/marks
Cookie:  idtoken=…   REQUIRED — names the school

{
  "marks": [                       // REQUIRED, 1 to 300 rows
    { "studentDocsId": "…",        // REQUIRED, max 60
      "status": "LATE",            // REQUIRED, PRESENT | LATE |
                                   //   AUTHORISED_ABSENCE |
                                   //   UNAUTHORISED_ABSENCE
      "reason": "BUS",             // required unless PRESENT, max 100
      "arrivalAt": "2026-06-05T03:02:00Z" }  // optional
  ],
  "version": 3                     // REQUIRED — the register's
}
</pre></td>
<td><pre>
200 OK — AttendanceRegisterResponse, the register as it now stands (#6's shape)

{
  "session": {
    "attendanceSessionDocsId": "6a9f…",
    "academicYear": "2026-2027",
    "attendanceDate": "2026-10-15",
    "classDocsId": "6a9c…",
    "className": "Class 1",
    "sectionNo": "A",
    "sessionType": "DAILY",
    "teacherDocsId": "6a9b…",
    "teacherName": "…",
    "status": "OPEN",
    "reopened": false,
    "version": 3
  },
  "studentCount": 2,
  "markedCount": 1,
  "unmarkedCount": 1,
  "markCounts": { "PRESENT": 1, "LATE": 0,
                  "AUTHORISED_ABSENCE": 0, "UNAUTHORISED_ABSENCE": 0 },
  "students": [
    { "studentDocsId": "…", "fullName": "Ranu", "admissionNo": "…",
      "rollNo": "7", "studentAcademicRecordDocsId": "…",
      "onRoster": true,
      "mark": { "status": "PRESENT", "source": "MANUAL" } },
    { "studentDocsId": "…", "fullName": "Ayan khan", "rollNo": "41",
      "studentAcademicRecordDocsId": "…", "onRoster": true }
  ],
  "nextStep": "…"
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `attendanceSessionDocsId` (path) | **yes** | A register of this year. |
| `marks` | **yes** | At most 300 rows. Empty is `NOTHING_TO_MARK`. The same child twice is `DUPLICATE_STUDENT_IN_REQUEST`. |
| `marks[].studentDocsId` | **yes** | Max 60. The child must have been in the section on the register's date. |
| `marks[].status` | **yes** | `PRESENT`, `LATE`, `AUTHORISED_ABSENCE` or `UNAUTHORISED_ABSENCE`. |
| `marks[].reason` | **unless `PRESENT`** | Max 100, e.g. `MEDICAL` or `BUS`. A blank one is treated as missing. |
| `marks[].arrivalAt` | no | An instant — when the child arrived. Useful with `LATE`. |
| `version` | **yes** | The register's version, as last read. A register changed since is refused. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `ATTENDANCE_SESSION_NOT_FOUND` | No register of that id in this year. |
| 409 | `SESSION_NOT_OPEN` | The register is `LOCKED` or `CANCELLED`. |
| 409 | `CONCURRENT_MODIFICATION` | The register changed since this version was read. |
| 400 | `STUDENT_NOT_IN_REGISTER` | A child who was not in the section that day — names them. |
| 400 | `REASON_REQUIRED` | A mark other than `PRESENT` with no reason — names the child. |
| 400 | `DUPLICATE_STUDENT_IN_REQUEST` | The same child twice in `marks`. |
| 400 | `NOTHING_TO_MARK` | `marks` is empty. |
| 400 | `VALIDATION_FAILED` | No version, or a mark with no `studentDocsId` or `status`. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed (gate 1, 2). |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year is not the one the school is running (gate 4). |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

**Built 2026-10-15, then simplified the same day, and moved to `AttendanceRecordController` / `AttendanceRecordService` — the URL did not change.** The request is only `marks`, each with
`studentDocsId`, `status`, `reason` and `arrivalAt` — **no `markRemainingAs` and no `departureAt`**.
To mark a whole class, every child is sent. [Open item 4](#4-unmarked-children-when-a-register-is-locked) is therefore option (a)
— #3 will refuse a lock while anyone is unmarked. The answer is the register as it now stands (#6's
shape). Also refused: `NOTHING_TO_MARK` (empty `marks`) and `DUPLICATE_STUDENT_IN_REQUEST`. A stored
`departureAt` is left untouched by a correction.

```json
{ "marks": [
    { "studentDocsId": "…", "status": "PRESENT" },
    { "studentDocsId": "…", "status": "LATE", "reason": "BUS", "arrivalAt": "2026-06-05T03:02:00Z" },
    { "studentDocsId": "…", "status": "AUTHORISED_ABSENCE", "reason": "MEDICAL" } ],
  "version": 3 }
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

**The register's `version` is required, and every save moves it on** — decided 2026-10-15, see
[open item 3](#3-two-people-correcting-the-same-child). A save from a stale copy is `409
CONCURRENT_MODIFICATION`; no version is `400 VALIDATION_FAILED`. The marks and the version bump are
one transaction, so two saves racing past the check together cannot both land. Measured 2026-10-15
with no writes: Class 1 A P1 at version 0 — no version → `VALIDATION_FAILED`, version 5 →
`CONCURRENT_MODIFICATION`.

<a id="e3"></a>
**[#3](#t3) · `POST /attendance-sessions/{id}/lock`** — built 2026-10-15

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `status` — must be `OPEN`; `version`
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: the marks — refused while any child is unmarked
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: who belongs in the register on that date
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *updates*: `status` = `LOCKED`, `lockedAt`, `lockedByDocsId`, `version`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/attendance-sessions/{attendanceSessionDocsId}/lock
Cookie:  idtoken=…   REQUIRED — names the school

{
  "version": 3                     // REQUIRED — the register's
}
</pre></td>
<td><pre>
200 OK — AttendanceRegisterResponse (#6's shape),
with "status": "LOCKED" and "lockedAt" set

{
  "session": {
    "attendanceSessionDocsId": "6a9f…",
    "academicYear": "2026-2027",
    "attendanceDate": "2026-10-15",
    "classDocsId": "6a9c…",
    "className": "Class 1",
    "sectionNo": "A",
    "sessionType": "DAILY",
    "teacherDocsId": "6a9b…",
    "teacherName": "…",
    "status": "LOCKED",
    "lockedAt": "2026-10-15T05:10:00Z",
    "reopened": false,
    "version": 4
  },
  "studentCount": 2,
  "markedCount": 2,
  "unmarkedCount": 0,
  "markCounts": { "PRESENT": 0, "LATE": 1,
                  "AUTHORISED_ABSENCE": 0, "UNAUTHORISED_ABSENCE": 1 },
  "students": [
    { "studentDocsId": "…", "fullName": "Ranu", "admissionNo": "…",
      "rollNo": "7", "studentAcademicRecordDocsId": "…",
      "onRoster": true,
      "mark": { "status": "UNAUTHORISED_ABSENCE", "reason": "LALA",
                "source": "MANUAL" } },
    { "studentDocsId": "…", "fullName": "Ayan khan", "rollNo": "41",
      "studentAcademicRecordDocsId": "…", "onRoster": true,
      "mark": { "status": "LATE", "reason": "BUS", "source": "MANUAL" } }
  ],
  "nextStep": "…"
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `attendanceSessionDocsId` (path) | **yes** | A register of this year. |
| `version` | **yes** | The register's version, as last read. A register changed since — by another lock, an unlock or a correction — is refused. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `ATTENDANCE_SESSION_NOT_FOUND` | No register of that id in this year. |
| 409 | `SESSION_NOT_OPEN` | The register is already `LOCKED`, or `CANCELLED`. |
| 409 | `CONCURRENT_MODIFICATION` | The register changed since it was read. |
| 409 | `UNMARKED_STUDENTS` | Children still without a mark — says how many. |
| 400 | `VALIDATION_FAILED` | No version. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed (gate 1, 2). |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year is not the one the school is running (gate 4). |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

`{ "version": n }`. Sets `LOCKED`,
`lockedAt`, `lockedByDocsId`. **Refused while any child is unmarked** (`UNMARKED_STUDENTS`, with the
count) — see [open item 4](#4-unmarked-children-when-a-register-is-locked). Locking a locked register
is `409 SESSION_NOT_OPEN`.

Built as planned: refused while anyone is unmarked (`UNMARKED_STUDENTS`, with the count), version is
the register's, `lockedByDocsId` is the signed-in staff member when there is one. Answers with the
register (#6's shape). Measured 2026-10-15 with no writes: Class 1 A P1 (nobody marked) answered
`UNMARKED_STUDENTS`, a stale version `CONCURRENT_MODIFICATION`, no version `VALIDATION_FAILED`.

**The gap with #2 is closed (2026-10-15).** #2 now takes the register's version and bumps it, so a
save racing a lock fails on the version instead of landing marks in a locked register. A lock after
a save has to send the version the save answered with.

<a id="e4"></a>
**[#4](#t4) · `POST /attendance-sessions/{id}/unlock`** — built 2026-10-15

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `status` — must be `LOCKED`; `version`; `lockedAt`, `lockedByDocsId` — copied into the history
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *updates*: `status` = `OPEN`, `lockedAt` and `lockedByDocsId` cleared, one entry added to `unlocks` (`reason`, `unlockedAt`, `unlockedByDocsId`, `previousLockedAt`, `previousLockedByDocsId`), `version`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/attendance-sessions/{attendanceSessionDocsId}/unlock
Cookie:  idtoken=…   REQUIRED — names the school

{
  "reason": "Ranu was marked absent by mistake",
                                   // REQUIRED, max 500
  "version": 4                     // REQUIRED — the register's
}
</pre></td>
<td><pre>
200 OK — AttendanceRegisterResponse (#6's shape),
with the register OPEN again and its unlock history

{
  "session": {
    "attendanceSessionDocsId": "6a9f…",
    "status": "OPEN",
    "unlocks": [
      { "reason": "Ranu was marked absent by mistake",
        "unlockedAt": "2026-10-15T06:00:00Z",
        "unlockedByDocsId": "6a9b…",
        "previousLockedAt": "2026-10-15T05:10:00Z",
        "previousLockedByDocsId": "6a9b…" }
    ],
    "version": 5,
    …
  },
  "studentCount": 2,
  …
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `attendanceSessionDocsId` (path) | **yes** | A register of this year. |
| `reason` | **yes** | Max 500. Why a final register is being re-opened. Saved in the unlock history. A blank one is refused. |
| `version` | **yes** | The register's version, as last read. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `REASON_REQUIRED` | No reason, or a blank one. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `ATTENDANCE_SESSION_NOT_FOUND` | No register of that id in this year. |
| 409 | `SESSION_NOT_LOCKED` | The register is `OPEN` or `CANCELLED`. |
| 409 | `CONCURRENT_MODIFICATION` | The register changed since it was read. |
| 400 | `VALIDATION_FAILED` | No version, or a reason over 500 characters. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed (gate 1, 2). |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year is not the one the school is running (gate 4). |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

`{ "reason": "…", "version": n }`. Back to
`OPEN`, clearing `lockedAt` and `lockedByDocsId`. **The reason is required**, because re-opening a
final register is exactly what somebody will ask about later. See
[open item 5](#5-who-may-unlock).

**The reason is stored.** The model gained `unlocks` — a list of
[`AttendanceUnlock`](../../../models/academics/attendance/embedded/AttendanceUnlock.java)
(`reason`, `unlockedAt`, `unlockedByDocsId`, `previousLockedAt`, `previousLockedByDocsId`), oldest
first — rather than one field, so a register unlocked twice keeps both reasons. The unlock clears
`lockedAt`/`lockedByDocsId`; the history keeps them. The register response returns `unlocks`.
Measured 2026-10-15 with no writes: no or blank reason → `REASON_REQUIRED`, an `OPEN` register →
`SESSION_NOT_LOCKED`, no version → `VALIDATION_FAILED`.

<a id="e5"></a>
**[#5](#t5) · `POST /attendance-sessions/{id}/cancel`** — planned

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `status` — must be `OPEN` or `LOCKED`; `version`
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *updates*: `status` = `CANCELLED`
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — untouched: **the marks are kept, not deleted**

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/attendance-sessions/{attendanceSessionDocsId}/cancel
Cookie:  idtoken=…   REQUIRED — names the school

{
  "version": n                     // the register's
}
</pre></td>
<td><pre>
Not designed yet. Not built — a request
to it returns a 404.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `attendanceSessionDocsId` (path) | **yes** | A register of this year. |
| `version` | **yes** | The register's version, as last read. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ATTENDANCE_SESSION_NOT_FOUND` | No register with that id in this school. |
| 409 | `SESSION_NOT_OPEN` | Listed for #5 in [the refusal codes](#the-refusal-codes-this-module-introduces): a register that is locked or cancelled. The plan above lets a `LOCKED` register be cancelled — settle which when #5 is built. |

`{ "version": n }`. `OPEN` or `LOCKED` →
`CANCELLED`. **The marks are kept, not deleted**, and every report leaves a cancelled register out.
A cancelled register frees nothing in the unique index — re-opening the same register is
[open item 2](#2-re-opening-a-cancelled-register).

<a id="e6"></a>
**[#6](#t6) · `GET /attendance-sessions/{id}`** — built 2026-10-15

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: the whole register
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: `studentDocsId`, `status`, `reason`, `source`, `arrivalAt`, `departureAt` — the marks
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo`, `effectiveFrom`, `effectiveUntil`, `status` — the roster on that date
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/attendance-sessions/{attendanceSessionDocsId}
Cookie:  idtoken=…   REQUIRED — names the school

No body.
</pre></td>
<td><pre>
200 OK — AttendanceRegisterResponse

{
  "session": {
    "attendanceSessionDocsId": "6a9f…",
    "academicYear": "2026-2027",
    "attendanceDate": "2026-10-15",
    "classDocsId": "6a9c…",
    "className": "Class 1",
    "sectionNo": "A",
    "sessionType": "DAILY",
    "teacherDocsId": "6a9b…",
    "teacherName": "…",
    "status": "OPEN",
    "reopened": false,
    "version": 3
  },
  "studentCount": 2,
  "markedCount": 1,
  "unmarkedCount": 1,
  "markCounts": { "PRESENT": 1, "LATE": 0,
                  "AUTHORISED_ABSENCE": 0, "UNAUTHORISED_ABSENCE": 0 },
  "students": [
    { "studentDocsId": "…", "fullName": "Ranu", "admissionNo": "…",
      "rollNo": "7", "studentAcademicRecordDocsId": "…",
      "onRoster": true,
      "mark": { "status": "PRESENT", "source": "MANUAL" } },
    { "studentDocsId": "…", "fullName": "Ayan khan", "rollNo": "41",
      "studentAcademicRecordDocsId": "…", "onRoster": true }
  ],
  "nextStep": "…"
}
</pre></td>
</tr>
</table>

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `ATTENDANCE_SESSION_NOT_FOUND` | No register of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

the header, then **one row per child who belongs in
it**: name, roll number, and their mark or `null` for "not marked yet". Counts per status and
`unmarkedCount`. Three reads — the session, the marks, the roster — plus the names, never one per row.

<a id="e7"></a>
**[#7](#t7) · `GET /attendance-sessions?date=&classDocsId=&sectionNo=&sessionType=&status=`** — built 2026-10-15

- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `attendanceDate`, `classDocsId`, `sectionNo`, `sessionType`, `status`, `periodCode`, `teacherDocsId`, `version` — the page
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: `status` — counted per register in **one aggregation for the page**

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/attendance-sessions
    ?date=2026-10-15        // optional
    &classDocsId=6a9c…      // optional
    &sectionNo=A            // optional
    &sessionType=DAILY      // optional, DAILY | PERIOD
    &status=LOCKED          // optional, OPEN | LOCKED | CANCELLED
    &page=0                 // optional, from 0
    &size=20                // optional, 1 to 100
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — PageResponse of AttendanceSessionRowResponse,
newest date first

{
  "content": [
    { "attendanceSessionDocsId": "6a9f…",
      "attendanceDate": "2026-10-15",
      "classDocsId": "6a9c…",
      "className": "Class 1",
      "sectionNo": "A",
      "sessionType": "DAILY",
      "teacherDocsId": "6a9b…",
      "teacherName": "…",
      "status": "LOCKED",
      "markedCount": 2,
      "markCounts": { "PRESENT": 0, "LATE": 1,
                      "AUTHORISED_ABSENCE": 0,
                      "UNAUTHORISED_ABSENCE": 1 },
      "version": 4 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `date` | no | Only registers of this date. Absent: every date. |
| `classDocsId` | no | Only registers of this class. |
| `sectionNo` | no | Only registers of this section. Usually with `classDocsId`. |
| `sessionType` | no | `DAILY` or `PERIOD`. |
| `status` | no | `OPEN`, `LOCKED` or `CANCELLED`. |
| `page` | no | Page number, from 0. |
| `size` | no | Rows per page, 1 to 100. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 400 | `INVALID_PAGE` | `page` is negative. |
| 400 | `INVALID_PAGE_SIZE` | `size` is not between 1 and 100. |
| 400 | `VALIDATION_FAILED` | A date, kind or status that does not parse. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

paged; filters `date`, `classDocsId`, `sectionNo`,
`sessionType`, `status`. Each row carries counts per mark status from **one aggregation for the
page**.

<a id="e8"></a>
**[#8](#t8) · `GET /attendance/day?date=`** — built 2026-10-15

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — the date must be a working day in the year
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `active`, `sections[].sectionNo`, `sections[].active` — every active section of every active class
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: the day's registers — `classDocsId`, `sectionNo`, `sessionType`, `status`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: one aggregation — children per section on that date
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: one aggregation — marks per register and status

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/attendance/day
    ?date=2026-10-15        // REQUIRED
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — AttendanceDayResponse

{
  "academicYear": "2026-2027",
  "date": "2026-10-15",
  "sectionCount": 5,
  "takenCount": 1,
  "notTakenCount": 4,
  "lockedCount": 1,
  "sections": [
    { "classDocsId": "6a9c…",
      "className": "Class 1",
      "sectionNo": "A",
      "studentCount": 2,
      "taken": true,
      "attendanceSessionDocsId": "6a9f…",
      "status": "LOCKED",
      "markedCount": 2,
      "unmarkedCount": 0,
      "markCounts": { "PRESENT": 0, "LATE": 1,
                      "AUTHORISED_ABSENCE": 0,
                      "UNAUTHORISED_ABSENCE": 1 },
      "periodRegisterCount": 2 },
    …
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `date` | **yes** | A date (YYYY-MM-DD) inside the year, and a working day. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | The date is not inside the year. |
| 404 | `NOT_A_WORKING_DAY` | A holiday or weekly off — the message names it. |
| 400 | `MISSING_PARAMETER` | No date. |
| 400 | `INVALID_PARAMETER` | A date that is not YYYY-MM-DD. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

**every active section of every
active class**, and for each: whether its `DAILY` register is **taken** (exists and is not
cancelled), its status, the **headcount for that date**, how many are marked and not marked, and how
many `PERIOD` registers were opened. Totals: `sectionCount`, `takenCount`, `notTakenCount`,
`lockedCount`. **"Not taken" is a row, not an absence** — the point is seeing the sections nobody has
marked.

**Four reads for the whole school**: the classes, the day's registers, one aggregation of children
per section (`StudentAcademicRecordRepository.countInSectionsOn` — the same date rule as #6), one
aggregation of marks. `date` is required; a holiday or weekly off is `404 NOT_A_WORKING_DAY`. Lives on
`AttendanceRecordController` / `AttendanceRecordService`. Measured 2026-10-15: 5 sections, 1 taken
(Class 1 A, `LOCKED`, 2 of 2 marked, 2 period registers).

<a id="e9"></a>
**[#9](#t9) · `GET /students/{studentDocsId}/attendance?from=&to=`** — built 2026-10-15

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the default range
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo` — the child must be this school's
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: `attendanceDate`, `status`, `reason`, `arrivalAt`, `attendanceSessionDocsId` — every mark in the range
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `sessionType`, `periodCode`, `classDocsId`, `sectionNo`, `status` — the registers behind the marks

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/students/{studentDocsId}/attendance
    ?from=2026-06-01        // optional, default: the year's start
    &to=2026-10-31          // optional, default: the year's end
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — StudentAttendanceResponse

{
  "studentDocsId": "…",
  "studentName": "Ayan khan",
  "admissionNo": "…",
  "academicYear": "2026-2027",
  "from": "2026-06-01",
  "to": "2027-04-30",
  "dailyCounts": { "PRESENT": 0, "LATE": 1,
                   "AUTHORISED_ABSENCE": 0,
                   "UNAUTHORISED_ABSENCE": 0 },
  "markedDays": 1,
  "attendedDays": 1,
  "attendancePercentage": 100.0,
  "periodMarkCount": 0,
  "records": [
    { "attendanceDate": "2026-10-15",
      "sessionType": "DAILY",
      "classDocsId": "6a9c…",
      "sectionNo": "A",
      "status": "LATE",
      "reason": "BUS",
      "attendanceSessionDocsId": "6a9f…",
      "registerStatus": "LOCKED" }
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `studentDocsId` (path) | **yes** | A child of this school. |
| `from` | no | A date. Absent: the year's start. Included. |
| `to` | no | A date. Absent: the year's end. Included. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `STUDENT_NOT_FOUND` | No child of that id in this school. |
| 400 | `INVALID_DATE_RANGE` | `from` is after `to`. |
| 400 | `INVALID_PARAMETER` | A date that is not YYYY-MM-DD. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

every mark in the range, newest
first, with counts per status and a percentage. Served by `school_student_attendance_history_idx`.
**Cancelled registers are left out.** How `LATE` counts is
[open item 7](#7-does-late-count-as-present).

On `AttendanceRecordController`. `from`/`to` default to the year's own dates; both ends are
included; `from` after `to` is `400 INVALID_DATE_RANGE`. Answers `dailyCounts`, `markedDays`,
`attendedDays`, `attendancePercentage` (one decimal, absent when nothing is marked),
`periodMarkCount` and every mark with its register. Three reads: the child, their marks, the
registers. Measured 2026-10-15: Ayan khan 1 day LATE → 100%; Ranu 1 day unauthorised absence → 0%.

<a id="e10"></a>
**[#10](#t10) · `GET /attendance/sections/{classDocsId}/{sectionNo}?from=&to=`** — built 2026-10-15

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the default range
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `sections[].sectionNo` — the section must exist
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `rollNo`, `effectiveFrom`, `effectiveUntil` — every child in the section at any time in the range
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: the section's `DAILY` registers in the range
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: one aggregation — marks per child and status
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/attendance/sections/{classDocsId}/{sectionNo}
    ?from=2026-10-01        // optional, default: the year's start
    &to=2026-10-31          // optional, default: the year's end
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — SectionAttendanceResponse

{
  "academicYear": "2026-2027",
  "classDocsId": "6a9c…",
  "className": "Class 1",
  "sectionNo": "A",
  "from": "2026-06-01",
  "to": "2027-04-30",
  "registerCount": 1,
  "studentCount": 2,
  "students": [
    { "studentDocsId": "…",
      "fullName": "Ranu",
      "admissionNo": "…",
      "rollNo": "7",
      "placedFrom": "2026-06-01",
      "registerDays": 1,
      "markedDays": 1,
      "attendedDays": 0,
      "unmarkedDays": 0,
      "counts": { "PRESENT": 0, "LATE": 0,
                  "AUTHORISED_ABSENCE": 0,
                  "UNAUTHORISED_ABSENCE": 1 },
      "attendancePercentage": 0.0 },
    …
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `classDocsId` (path) | **yes** | A class of this year. |
| `sectionNo` (path) | **yes** | A section of that class. |
| `from` | no | A date. Absent: the year's start. Included. |
| `to` | no | A date. Absent: the year's end. Included. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 404 | `CLASS_NOT_FOUND` | No class of that id in this year. |
| 400 | `SECTION_NOT_IN_CLASS` | The class has no section by that `sectionNo`. |
| 400 | `INVALID_DATE_RANGE` | `from` is after `to`. |
| 400 | `INVALID_PARAMETER` | A date that is not YYYY-MM-DD. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

one row per child
who was in the section during the range, with counts per status and a percentage, from **one
aggregation**. What a school prints at the end of the month.

On `AttendanceRecordController`. Every child **in the section at any time in the range** — read with
`StudentAcademicRecordRepository.findInSectionBetween` — and each is counted **only against the
registers held while they were in it** (`registerDays`), so a child who joined mid-month is not
shown as absent before it. Same rules as #9. Rows by roll number **numerically** (7 before 41 —
`AttendanceServiceUtils.ROLL_ORDER`, now also used by #6). Five reads, one of them the single
aggregation of marks per child and status. Measured 2026-10-15: Class 1 A — roll 7 Ranu 0%, roll 41
Ayan khan 100%; Class 1 C — 4 children, no registers.

<a id="e11"></a>
**[#11](#t11) · `GET /attendance/absentees?date=`** — built 2026-10-15

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — the date must be a working day in the year
- [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: `status`, `reason`, `guardianNotified`, `guardianAcknowledgedAt`, `attendanceSessionDocsId` — the day's absences. **Read-only on the notification flags**
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java) — *reads*: `sessionType`, `status`, `classDocsId`, `sectionNo` — `DAILY` registers only, cancelled ones left out
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/attendance/absentees
    ?date=2026-10-15        // REQUIRED
    &guardianNotified=false // optional, true | false
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — AbsenteesResponse

{
  "academicYear": "2026-2027",
  "date": "2026-10-15",
  "guardianNotified": false,
  "absentCount": 1,
  "authorisedCount": 0,
  "unauthorisedCount": 1,
  "notNotifiedCount": 1,
  "absentees": [
    { "studentDocsId": "…",
      "fullName": "Ranu",
      "admissionNo": "…",
      "classDocsId": "6a9c…",
      "className": "Class 1",
      "sectionNo": "A",
      "status": "UNAUTHORISED_ABSENCE",
      "reason": "LALA",
      "guardianNotified": false,
      "attendanceSessionDocsId": "6a9f…",
      "registerStatus": "LOCKED" }
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `date` | **yes** | A date (YYYY-MM-DD) inside the year, and a working day. |
| `guardianNotified` | no | `true` or `false`. `false` is the follow-up list. Absent: every absence. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | No year of that name in this school. |
| 409 | `DATE_OUTSIDE_ACADEMIC_YEAR` | The date is not inside the year. |
| 404 | `NOT_A_WORKING_DAY` | A holiday or weekly off — the message names it. |
| 400 | `MISSING_PARAMETER` | No date. |
| 400 | `INVALID_PARAMETER` | A date that is not YYYY-MM-DD, or `guardianNotified` not true/false. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

every `AUTHORISED_ABSENCE` and
`UNAUTHORISED_ABSENCE` that day, with the child, the section, the reason and
`guardianNotified`. Served by `school_date_attendance_status_idx`. **Read-only on the notification
flags** — setting them is the notification system's.

**Built 2026-10-15** in `AttendanceRecordController` / `AttendanceRecordService.getAbsentees`, answering
`AbsenteesResponse`. Optional `guardianNotified=false` is the follow-up list. Counts **DAILY
registers only** (a child absent from three periods is one row, not three) and leaves cancelled
registers out — the same rule as #8–#10. The year must hold the date, and a holiday is `404
NOT_A_WORKING_DAY`. Four reads: the absences
([`findAbsencesOn`](../../../repositories/academics/attendance/StudentAttendanceRecordRepositoryCustom.java)),
their registers, the children, the classes. Totals: `absentCount`, `authorisedCount`,
`unauthorisedCount`, `notNotifiedCount`. Measured 2026-10-15: 1 absent — Ranu, Class 1 A,
`UNAUTHORISED_ABSENCE` "LALA", not told.

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

**Decided 2026-10-15: the register's version.** #2 takes the register's `version` and moves it on
with every save, so a second person saving from a stale copy is refused and reads the register
again — nobody silently overwrites a child someone else has just marked. The cost, accepted: a save
is refused even when the other person marked *different* children.

A version per mark was rejected — it would make marking a class of forty unusable.

## 4. Unmarked children when a register is locked

**Decided 2026-10-15: (a).** #2 takes only `marks` — no `markRemainingAs` — so #3 will refuse a
lock while anyone is unmarked.

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

**Confirmed 2026-10-15:** `LATE` counts as attended in every percentage (#9, #10), and is also
counted on its own.

The enum's own note says "if school policy counts it", and no setting exists. **Recommendation:**
count `LATE` as **attended** in every percentage, and report it separately so a school that wants
otherwise can see it. A school setting can come later.

## 8. `DAILY` and `PERIOD` on the same day

**Confirmed 2026-10-15:** #8–#11 count `DAILY` registers only; #9 lists period marks and counts them
in `periodMarkCount`.

Both can exist for one section on one date. **Recommendation:** allow both; the day report (#8) and
the percentages (#9, #10) count **`DAILY` registers only**, and period registers are for subject
attendance. A school that only takes period attendance is a later question.

---

# Where the code will live

```text
controllers/academics/attendance/
    AttendanceSessionController.java   #1, #3–#7 — the register
    AttendanceRecordController.java    #2, #8–#11 — the marks, and the reads over them

services/academics/attendance/
    AttendanceSessionService.java
    AttendanceRecordService.java
    utils/AttendanceServiceUtils.java   registerOf — the register answer both services return

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
