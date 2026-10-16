# controllers/academics/examination — API plan

**Nothing is built yet. Written 2026-10-15.** This is the full set of endpoints examinations need,
written before any of them so they can be built and reviewed one at a time — the same way
[`attendance`](../attendance/README.md), [`timetable`](../timetable/README.md) and
[`student`](../../student/README.md) were done. Built endpoints will be marked **built** in the `#`
column.

Mirrors [`models/academics/examination`](../../../models/academics/examination) — six collections,
already written. The model README
([`examination/README.md`](../../../models/academics/examination/README.md)) is the persistence
contract — answer-copy uniqueness, attendance before marks, lock before publish, snapshots; this file
is what may be done to it over HTTP. Where the two disagree, the model README wins until it is
changed.

[Open items](#to-settle-before-building) need an answer before the first endpoint is built — items
1, 2 and 5 change what the very first phase does.

---

## What this module is

**An exam event, its datesheet, who sat each paper, what they scored, and the term's report card.**
The office creates "Half Yearly, Term 1", lays out the datesheet — Maths for 7A, 7B and 7C on 12
November, 9:00 to 12:00, hall 2 — and publishes it. On the day the invigilator marks who sat the
paper and which answer booklet they wrote in. A week later the teacher enters marks, submits them,
and they are locked. At the end of the term every child's marks become a report card, and the class
teacher writes the holistic progress card beside it.

| Collection | Grain — one row per… | What moves it |
|---|---|---|
| [`exams`](../../../models/academics/examination/Exam.java) | exam event, per year | `DRAFT` → `PUBLISHED` → `IN_PROGRESS` → `COMPLETED`, or → `CANCELLED` ([open item 2](#2-the-exam-and-paper-status-graphs)) |
| [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) | **one paper**: exam + class + section + subject + component | the datesheet row, and the hall register's header |
| [`exam_attendances`](../../../models/academics/examination/ExamAttendance.java) | child, per paper | `PRESENT`, `ABSENT` or `UNFAIR_MEANS`, with the answer booklet |
| [`student_marks`](../../../models/academics/examination/StudentMark.java) | child, per paper | `DRAFT` → `SUBMITTED` → `LOCKED` |
| [`report_cards`](../../../models/academics/examination/ReportCard.java) | child, per term, per version | `DRAFT` → `REVIEWED` → `PUBLISHED`, or → `REVOKED` |
| [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) | child, per term, per version | the same four states |

**A paper is the unit everything hangs off.** `ExamSchedule` is to `ExamAttendance` and
`StudentMark` what `AttendanceSession` is to `StudentAttendanceRecord`: the header that owns the
date, time, room and status. So the hall register and the mark sheet are both *a paper's*, and most
endpoints below are under `/exam-schedules/{scheduleId}`.

### Models from other packages

| Model | Lives in | Used for |
|---|---|---|
| [AcademicYear](../../../models/core/AcademicYear.java) | `core` | the year in every path, working days, and `resultsLocked` |
| [AcademicTerm](../../../models/academics/structure/AcademicTerm.java) | `academics/structure` | the term an exam and a card belong to, its `weightPercent` and `resultsLocked` |
| [SchoolClass](../../../models/academics/structure/SchoolClass.java) · [ClassSection](../../../models/academics/structure/embedded/ClassSection.java) · [ClassSubject](../../../models/academics/structure/embedded/ClassSubject.java) | `academics/structure` | which sections exist, and which subjects each one is taught — a paper is only for a subject the section has |
| [GradingScheme](../../../models/academics/grading/GradingScheme.java) | `academics/grading` | marks into a grade: `ClassSubject.gradingSchemeDocsId` → `Exam.gradingSchemeDocsId` → none |
| [StudentAcademicRecord](../../../models/student/StudentAcademicRecord.java) | `student` | **who sits a paper** — the section on the paper's date ([open item 1](#1-who-sits-a-paper)) |
| [Student](../../../models/student/Student.java) | `student` | names on the hall register, mark sheet and card |
| [Staff](../../../models/people/staff/Staff.java) | `people/staff` | invigilators, who marked attendance, who entered and locked marks, who published |
| [FacilityResource](../../../models/facilities/FacilityResource.java) | `facilities` | the exam room |
| [AttendanceSession](../../../models/academics/attendance/AttendanceSession.java) · [StudentAttendanceRecord](../../../models/academics/attendance/StudentAttendanceRecord.java) | `academics/attendance` | the attendance days copied onto a report card |
| [DocumentRecord](../../../models/documents/DocumentRecord.java) | `documents` | question paper, evidence, the printed card — referenced only, never written here |

## What this module is not

- **Not printing.** `generatedDocumentDocsId` and `questionPaperDocumentDocsId` are references; making
  the PDF and storing the file belong to `documents`.
- **Not telling families.** Publishing a datesheet or a card makes it visible; sending a message about
  it is the notification system's, which is designed last.
- **Not the class's daily attendance.** Hall attendance is per paper and lives here; the daily
  register is [`attendance`](../attendance/README.md). A report card *reads* the daily register for
  its attendance days, and writes nothing to it.
- **Not grading schemes.** Those are [`grading`](../grading/README.md); here they are only resolved.
- **Not seating plans or invigilation rosters generated automatically.** `seatNo` and
  `invigilatorDocsIds` are typed in. Generating them is a later feature.

## Which gates every endpoint runs

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | every write |
| **2** | Is the subscription usable | every write |
| **4** | Is the year in the path the running one | every write ([open item 12](#12-gate-4-and-the-final-terms-cards)) |
| **Results lock** | Are `AcademicYear.resultsLocked` and `AcademicTerm.resultsLocked` both false | every write to marks, grace and report cards — not to the exam, datesheet or hall attendance |

**The results lock is this module's own gate**, and it runs in the service, not the controller,
because it needs the exam's term. It is the model README's section 8: a frozen term takes no
result writes at all — not even an unlock — until [`structure` #6](../structure/README.md#e6)
reopens it. **Publishing is allowed while frozen** ([open item 11](#11-what-the-results-lock-blocks)).

**No gate runs on a read.** **Every path is under `/schools/current/academic-years/{year}`** — an
exam *is* a year's.

---

# The endpoints

Numbered by area, not by build order. **[Build order](#build-order) is below** and differs.

## 1. The exam — writes and reads

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 | [`POST /exams`](#e1) | **Create an exam** — code, name, term, type, dates, weight, grading scheme. Starts `DRAFT`. | `exams`, `academic_terms`, `grading_schemes` |
| <a id="t2"></a>2 | [`PATCH /exams/{examId}`](#e2) | Fix the name, dates, weight or grading scheme. **Never the term** once papers exist. | `exams`, `exam_schedules` |
| <a id="t3"></a>3 | [`POST /exams/{examId}/publish`](#e3) | **Publish the datesheet** — the exam and every paper in it become visible. | `exams`, `exam_schedules` |
| <a id="t4"></a>4 | [`POST /exams/{examId}/complete`](#e4) | Every paper is over. Refused while a paper is still open. | `exams`, `exam_schedules` |
| <a id="t5"></a>5 | [`POST /exams/{examId}/cancel`](#e5) | Call the whole exam off, with a reason. Refused once any marks are locked. | `exams`, `exam_schedules`, `student_marks` |
| <a id="t6"></a>6 | [`GET /exams?termDocsId=&examType=&status=&search=`](#e6) | The year's exams, filtered and paged, with a paper count each. | `exams`, `exam_schedules` |
| <a id="t7"></a>7 | [`GET /exams/{examId}`](#e7) | **One exam in full** — its term, scheme, and how far along it is: papers held, marks entered, marks locked. | `exams`, `exam_schedules`, `student_marks` |

## 2. The datesheet — papers

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t8"></a>8 | [`POST /exams/{examId}/schedules`](#e8) | **Add a paper** — one subject, one class, **one or more sections** at the same date and time. One row per section. | `exam_schedules`, `school_classes`, `facility_resources`, `staff` |
| <a id="t9"></a>9 | [`PATCH /exam-schedules/{scheduleId}`](#e9) | Move a paper, change its room, invigilators or marks. Maximum marks is frozen once any mark is entered. | `exam_schedules`, `student_marks` |
| <a id="t10"></a>10 | [`POST /exam-schedules/{scheduleId}/cancel`](#e10) | Drop one paper, with a reason. Refused once marks are locked. | `exam_schedules`, `student_marks` |
| <a id="t11"></a>11 | [`GET /exams/{examId}/schedules?classDocsId=&sectionNo=&date=`](#e11) | **The datesheet** — every paper, by date and time. | `exam_schedules`, `school_classes`, `facility_resources` |
| <a id="t12"></a>12 | [`GET /exam-schedules/{scheduleId}`](#e12) | One paper in full, with its hall and mark counts. | `exam_schedules`, `exam_attendances`, `student_marks` |
| <a id="t13"></a>13 | [`GET /exam-schedules?date=&invigilatorDocsId=&facilityResourceDocsId=`](#e13) | **The day's papers across every exam** — what each hall holds, and who is on duty. | `exam_schedules` |

## 3. In the hall — attendance

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t14"></a>14 | [`GET /exam-schedules/{scheduleId}/attendance`](#e14) | **The hall register** — every child expected, with their row or "not marked". | `exam_attendances`, `student_academic_records`, `students` |
| <a id="t15"></a>15 | [`PUT /exam-schedules/{scheduleId}/attendance`](#e15) | **Mark the hall** — present, absent or unfair means, the answer booklet, seat, times. | `exam_attendances`, `exam_schedules` |
| <a id="t16"></a>16 | [`POST /exam-schedules/{scheduleId}/complete`](#e16) | **The paper is over.** Refused while anyone is unmarked. Closes hall attendance. | `exam_schedules`, `exam_attendances` |
| <a id="t17"></a>17 | [`POST /exam-schedules/{scheduleId}/reopen`](#e17) | Re-open a completed paper's hall register to correct it, with a reason. | `exam_schedules`, `student_marks` |

## 4. Marks

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t18"></a>18 | [`GET /exam-schedules/{scheduleId}/marks`](#e18) | **The mark sheet** — every child who sat, their mark or "not entered". | `student_marks`, `exam_attendances`, `students` |
| <a id="t19"></a>19 | [`PUT /exam-schedules/{scheduleId}/marks`](#e19) | **Enter marks** by child — one, several or all. Re-sending corrects. | `student_marks`, `exam_attendances`, `grading_schemes` |
| <a id="t20"></a>20 | [`GET /exam-schedules/{scheduleId}/answer-copies`](#e20) | **For blind evaluation** — booklet numbers with no names. | `exam_attendances`, `student_marks` |
| <a id="t21"></a>21 | [`PUT /exam-schedules/{scheduleId}/marks/by-copy`](#e21) | Enter marks **by booklet number**. The server finds the child. | `student_marks`, `exam_attendances` |
| <a id="t22"></a>22 | [`POST /exam-schedules/{scheduleId}/marks/submit`](#e22) | The teacher is done — `DRAFT` → `SUBMITTED`. Refused while anyone who sat is unmarked. | `student_marks` |
| <a id="t23"></a>23 | [`POST /exam-schedules/{scheduleId}/marks/lock`](#e23) | **Final** — `SUBMITTED` → `LOCKED`. What report cards wait for. | `student_marks` |
| <a id="t24"></a>24 | [`POST /exam-schedules/{scheduleId}/marks/unlock`](#e24) | **Revaluation** — back to `SUBMITTED`, with a reason. Refused once a card using them is published. | `student_marks`, `report_cards` |
| <a id="t25"></a>25 | [`PATCH /student-marks/{markId}/grace`](#e25) | **Grace marks** — moderation beside the award, never over it, with a reason. | `student_marks` |

## 5. Results — reads

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t26"></a>26 | [`GET /exams/{examId}/mark-entry`](#e26) | **The office's question**: which papers have marks entered, submitted, locked — and which have none. | `exam_schedules`, `student_marks` |
| <a id="t27"></a>27 | [`GET /exams/{examId}/results/{classDocsId}/{sectionNo}`](#e27) | **The tabulation sheet** — a row per child, a column per subject, total, percentage, grade. | `student_marks`, `exam_schedules`, `students`, `grading_schemes` |
| <a id="t28"></a>28 | [`GET /students/{studentDocsId}/marks?termDocsId=&examDocsId=`](#e28) | One child's marks across papers. | `student_marks`, `exam_schedules`, `exams` |

## 6. Report cards

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t29"></a>29 | [`POST /report-cards/generate`](#e29) | **Compile a section's cards for a term** from locked marks. Builds or rebuilds `DRAFT` cards. | `report_cards`, `student_marks`, `exams`, `exam_schedules`, `attendance_sessions`, `student_attendance_records`, `grading_schemes` |
| <a id="t30"></a>30 | [`GET /report-cards?termDocsId=&classDocsId=&sectionNo=&status=`](#e30) | A section's cards, latest version each. | `report_cards` |
| <a id="t31"></a>31 | [`GET /report-cards/{cardId}`](#e31) | One card in full — what gets printed. | `report_cards` |
| <a id="t32"></a>32 | [`PATCH /report-cards/{cardId}/remarks`](#e32) | Class teacher's, principal's and subject teachers' remarks. Only before publishing. | `report_cards` |
| <a id="t33"></a>33 | [`POST /report-cards/{cardId}/review`](#e33) | `DRAFT` → `REVIEWED` — checked and ready. | `report_cards` |
| <a id="t34"></a>34 | [`POST /report-cards/publish`](#e34) | **Publish a section's reviewed cards at once** — ranks freeze together. | `report_cards` |
| <a id="t35"></a>35 | [`POST /report-cards/{cardId}/revoke`](#e35) | Withdraw a published card, with a reason. The next generate is `reportVersion + 1`. | `report_cards` |

## 7. Holistic progress cards

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t36"></a>36 | [`POST /holistic-progress-cards/generate`](#e36) | **Start a section's cards for a term** — one empty `DRAFT` per child, class and roll copied in. | `holistic_progress_cards`, `student_academic_records`, `attendance_sessions`, `student_attendance_records` |
| <a id="t37"></a>37 | [`PATCH /holistic-progress-cards/{cardId}`](#e37) | **Write it** — domains with level and observation, the child's reflection, peers, family, goals. | `holistic_progress_cards`, `guardians` |
| <a id="t38"></a>38 | [`POST /holistic-progress-cards/{cardId}/review`](#e38) | `DRAFT` → `REVIEWED`. Refused while any domain has no observation. | `holistic_progress_cards` |
| <a id="t39"></a>39 | [`POST /holistic-progress-cards/{cardId}/publish`](#e39) | `REVIEWED` → `PUBLISHED`. Never edited afterwards. | `holistic_progress_cards` |
| <a id="t40"></a>40 | [`POST /holistic-progress-cards/{cardId}/revoke`](#e40) | Withdraw it, with a reason (`revocationReason`). | `holistic_progress_cards` |
| <a id="t41"></a>41 | [`GET /holistic-progress-cards?termDocsId=&classDocsId=&sectionNo=&status=`](#e41) | A section's cards, latest version each. | `holistic_progress_cards` |
| <a id="t42"></a>42 | [`GET /holistic-progress-cards/{cardId}`](#e42) | One card in full. | `holistic_progress_cards` |

---

# Build order

Each phase ends with something that works end to end.

| Phase | What you get at the end of it | Endpoints |
|---|---|---|
| **1** | An exam exists with a datesheet, and it can be read back and published | [#1](#e1), [#7](#e7), [#8](#e8), [#11](#e11), [#3](#e3), [#6](#e6) |
| **2** | A paper can be sat — the hall register, marked and closed | [#14](#e14), [#15](#e15), [#16](#e16), [#12](#e12) |
| **3** | **Marks can be entered and made final** | [#18](#e18), [#19](#e19), [#22](#e22), [#23](#e23) |
| **4** | The numbers a school is asked for | [#27](#e27), [#26](#e26), [#28](#e28) |
| **5** | **A term's report cards, published** | [#29](#e29), [#31](#e31), [#30](#e30), [#32](#e32), [#33](#e33), [#34](#e34) |
| **6** | Every correction path | [#2](#e2), [#9](#e9), [#10](#e10), [#17](#e17), [#24](#e24), [#25](#e25), [#35](#e35), [#4](#e4), [#5](#e5) |
| **7** | Blind evaluation, and the day view | [#20](#e20), [#21](#e21), [#13](#e13) |
| **8** | Holistic progress cards | [#36](#e36) to [#42](#e42) |

**Phases 1–3 are the module working**: an exam, a paper, who sat it, what they scored, locked. Phase 5
is what a school actually hands a family. **Corrections come late on purpose** — the same reason as
attendance: they are easier to build, and to check, once there is real data to correct.

**Holistic progress cards are last because nothing depends on them.** They share no rows with marks,
and the only thing they read from this module is the term.

---

# The endpoints in detail

## The exam

<a id="e1"></a>
**[1](#t1) · `POST /exams`** — *create an exam*

```json
{ "name": "Half Yearly Examination, Term 1",
  "termDocsId": "…", "examType": "MIDTERM",
  "startDate": "2026-11-10", "endDate": "2026-11-20",
  "weightPercent": 40, "gradingSchemeDocsId": "…" }
```

- **`name` unique in the year**, case-insensitively, retired and cancelled exams included
  (`school_year_exam_name_uniq`) → `409 EXAM_NAME_TAKEN`. There is no exam code — removed
  2026-10-16, the same decision as terms: everything references an exam by `examDocsId`.
- The term must be in this year and active → `404 TERM_NOT_FOUND`.
- **Both dates inside the term**, `startDate ≤ endDate` → `400 INVALID_DATE_RANGE`,
  `409 EXAM_DATES_OUTSIDE_TERM`. Not checked against holidays: an exam *window* can span a weekend;
  each paper's own date is checked ([#8](#e8)).
- `weightPercent` optional, `0 < w ≤ 100`. Whether a term's weights add to 100 is checked when a card
  is generated, not here — exams are added one at a time ([open item 9](#9-exam-weights)).
- `gradingSchemeDocsId` optional; when sent, it must exist and be active → `404
  GRADING_SCHEME_NOT_FOUND`.
- Starts `DRAFT`. Gates 1, 2, 4.

<a id="e2"></a>
**[2](#t2) · `PATCH /exams/{examId}`** — `name`, `startDate`, `endDate`, `weightPercent`,
`gradingSchemeDocsId`, `termDocsId`, `version`. A new `name` must be free in the year → `409
EXAM_NAME_TAKEN`. **`termDocsId` only while the exam has no papers** (`409 EXAM_HAS_PAPERS`) — a
term move would put papers outside their exam's window. New dates must still hold every paper → `409
PAPERS_OUTSIDE_DATES`, naming them. Refused on `COMPLETED` or `CANCELLED` (`409
EXAM_NOT_EDITABLE`).

<a id="e3"></a>
**[3](#t3) · `POST /exams/{examId}/publish`** — `{ "version": n }`. The datesheet goes out: the exam
and every `DRAFT` paper move to `PUBLISHED`, `publishedAt` and `publishedByDocsId` set. Refused with
no papers (`409 EXAM_HAS_NO_PAPERS`). A paper added after publishing is `PUBLISHED` from the start.
**Which states exist at all is [open item 2](#2-the-exam-and-paper-status-graphs).**

<a id="e4"></a>
**[4](#t4) · `POST /exams/{examId}/complete`** — `{ "version": n }`. Refused while any paper is not
`COMPLETED` or `CANCELLED` (`409 PAPERS_STILL_OPEN`, with the count). Marks can still be entered and
locked afterwards — completing the exam is about the hall, not the marks.

<a id="e5"></a>
**[5](#t5) · `POST /exams/{examId}/cancel`** — `{ "reason": "…", "version": n }`. The exam and
every paper that is not already cancelled → `CANCELLED`. **Refused once any mark in it is `LOCKED`**
(`409 MARKS_ALREADY_LOCKED`) — a cancelled exam with final marks is a contradiction. Hall attendance
and draft marks are kept, and stop counting. Where the reason is stored is
[open item 6](#6-where-correction-reasons-are-stored).

<a id="e6"></a>
**[6](#t6) · `GET /exams`** — filters `termDocsId`, `examType`, `status`, `search` (name),
paged, by `startDate`. Each row carries its paper count.

<a id="e7"></a>
**[7](#t7) · `GET /exams/{examId}`** — the exam, its term's name, the scheme's name, and progress:
papers by status, and marks by status across every paper. What the exam's own page opens on.

## The datesheet

<a id="e8"></a>
**[8](#t8) · `POST /exams/{examId}/schedules`** — *add a paper*

```json
{ "classDocsId": "…", "sectionNos": ["A", "B", "C"],
  "subjectCode": "MATH", "componentCode": "THEORY", "componentName": "Theory",
  "examDate": "2026-11-12", "startTime": "09:00", "endTime": "12:00",
  "maximumMarks": 80, "passingMarks": 27,
  "facilityResourceDocsId": "…", "invigilatorDocsIds": ["…"] }
```

**One subject, one class, one or more sections** — because "Maths for all of Class 7 on the 12th" is
how a datesheet is written. One `exam_schedules` row per section, in one transaction; the answer
lists them.

- Every section must exist in the class; **the subject must be taught in each one** (a `ClassSubject`
  for the whole class or for that section) → `400 SUBJECT_NOT_TAUGHT_IN_SECTION`, naming the
  sections.
- `componentCode` defaults to `MAIN`. Unique on exam + class + section + subject + component →
  `409 PAPER_ALREADY_SCHEDULED`, naming the section.
- **`examDate` inside the exam's window and a working day** → `409 PAPER_OUTSIDE_EXAM`,
  `404 NOT_A_WORKING_DAY`. `startTime < endTime` → `400 INVALID_TIME_RANGE`.
- `0 < maximumMarks`, `passingMarks ≤ maximumMarks` → `400 PASSING_MARKS_ABOVE_MAXIMUM`.
- **Clashes are refused, not warned about.** A room already holding *another exam's or another
  class's* paper at an overlapping time → `409 ROOM_CLASH`; an invigilator already on duty then →
  `409 INVIGILATOR_CLASH`. Several sections of one paper **may** share a room — that is the same
  sitting. Both name the other paper.
- Refused when the exam is `COMPLETED` or `CANCELLED`.

<a id="e9"></a>
**[9](#t9) · `PATCH /exam-schedules/{scheduleId}`** — date, times, room, invigilators, marks,
component name, `version`. The same checks as #8. **`maximumMarks` and `passingMarks` are frozen
once any mark is entered** (`409 MARKS_ALREADY_ENTERED`) — a mark of 70 means nothing if the paper
becomes out of 50. Date and time are frozen once the paper is `COMPLETED`.

<a id="e10"></a>
**[10](#t10) · `POST /exam-schedules/{scheduleId}/cancel`** — `{ "reason": "…", "version": n }`.
Refused once its marks are locked. The paper drops out of the datesheet, the tabulation and every
card.

<a id="e11"></a>
**[11](#t11) · `GET /exams/{examId}/schedules`** — **the datesheet**: by date, then start time, then
class and section. Each row carries the class name, subject name and room name. Filters for one class,
one section, one date. Cancelled papers are shown, marked, unless `status` filters them out.

<a id="e12"></a>
**[12](#t12) · `GET /exam-schedules/{scheduleId}`** — one paper, with hall counts (present, absent,
unfair means, not marked) and mark counts (by status, not entered).

<a id="e13"></a>
**[13](#t13) · `GET /exam-schedules`** — every paper on a date across every exam, filterable by
invigilator or room. The exam office's day view, and an invigilator's duty list. Served by
`school_invigilator_exam_date_idx` and `school_exam_schedule_room_idx`.

## In the hall

<a id="e14"></a>
**[14](#t14) · `GET /exam-schedules/{scheduleId}/attendance`** — **the hall register**: every child
expected — the section's roster on the paper's date ([open item 1](#1-who-sits-a-paper)), in roll
order — each with their row or "not marked". The same shape as
[attendance #6](../attendance/README.md#e6). **Rows are not created ahead of time** — an unmarked
child is shown as unmarked, and [#16](#e16) refuses while there is one
([open item 4](#4-hall-rows-ahead-of-time-or-not)).

<a id="e15"></a>
**[15](#t15) · `PUT /exam-schedules/{scheduleId}/attendance`** — *mark the hall*

```json
{ "marks": [
    { "studentDocsId": "…", "attendanceStatus": "PRESENT", "answerCopyNo": "MATH-0041",
      "additionalAnswerCopyNos": ["MATH-0102"], "seatNo": "H2-14",
      "reportedAt": "2026-11-12T03:35:00Z", "submittedAt": "2026-11-12T06:30:00Z" },
    { "studentDocsId": "…", "attendanceStatus": "ABSENT", "remarks": "Fever" } ],
  "version": 4 }
```

The same rules as attendance #2: one, several or all; re-sending corrects; **everything checked
before anything is written**; the paper's `version` is required and moves on with every save.

- Every child must be in the paper's roster → `400 STUDENT_NOT_IN_PAPER`, naming them.
- **`PRESENT` and `UNFAIR_MEANS` need `answerCopyNo`; `ABSENT` must not have one** → `400
  ANSWER_COPY_REQUIRED` / `ANSWER_COPY_NOT_ALLOWED`.
- **A booklet is used once in the whole exam** — `answerCopyNo` against every other row's
  `answerCopyNo` *and* `additionalAnswerCopyNos`, and the same for each supplementary number,
  including inside the request → `409 ANSWER_COPY_TAKEN`, naming the booklet and the child who has
  it. The unique index backs the main number; the supplementary ones only the service can check.
- **Only a `PUBLISHED` paper on or after its date** takes hall marks → `409 PAPER_NOT_OPEN`,
  `409 PAPER_NOT_YET_SAT`. The first save moves the exam to `IN_PROGRESS`.
- `markedByDocsId` and `markedAt` are the server's ([open item 5](#5-markedbydocsid-is-required)).
- Late and early are **not** stored — they are derived from `reportedAt` and `submittedAt` against
  the paper's times, and #14 shows them.

<a id="e16"></a>
**[16](#t16) · `POST /exam-schedules/{scheduleId}/complete`** — `{ "version": n }`. Refused while
any expected child is unmarked (`409 UNMARKED_STUDENTS`, with the count). `PUBLISHED` →
`COMPLETED`. **Hall attendance is closed from here** — the model README's rule; there is no per-row
lock.

<a id="e17"></a>
**[17](#t17) · `POST /exam-schedules/{scheduleId}/reopen`** — `{ "reason": "…", "version": n }`.
`COMPLETED` → `PUBLISHED`, so the hall register can be corrected. **Refused once marks are locked**
(`409 MARKS_ALREADY_LOCKED`) — unlock them first, so the two can never disagree.

## Marks

<a id="e18"></a>
**[18](#t18) · `GET /exam-schedules/{scheduleId}/marks`** — **the mark sheet**: every child with a
hall row, in roll order — their hall status, `participationStatus`, `obtainedMarks`, `graceMarks`,
the effective mark, the grade it resolves to, and the row's status. Children marked `ABSENT` are
listed and cannot be marked. Counts: entered, not entered, by status.

<a id="e19"></a>
**[19](#t19) · `PUT /exam-schedules/{scheduleId}/marks`** — *enter marks*

```json
{ "marks": [
    { "studentDocsId": "…", "obtainedMarks": 64.5 },
    { "studentDocsId": "…", "participationStatus": "EXEMPT", "remarks": "Medical exemption" },
    { "studentDocsId": "…", "gradeCode": "A1" } ],
  "version": 6 }
```

- **The paper must be `COMPLETED`** — hall attendance first, the model README's ordering → `409
  PAPER_NOT_COMPLETED`.
- **Hall attendance decides participation**, copied in by the server (section 5 of the model README):
  `PRESENT` → `ATTENDED`; **`ABSENT` → refused** (`400 STUDENT_ABSENT`, naming them);
  `UNFAIR_MEANS` → `WITHHELD`, and no mark is taken. The only thing a request may set is `EXEMPT`.
- `0 ≤ obtainedMarks ≤ maximumMarks` → `400 MARKS_OUT_OF_RANGE`, naming the child. **Marks or
  `gradeCode`, not both**; a grade must be a band of the resolved scheme → `400 GRADE_NOT_IN_SCHEME`.
- **Only `DRAFT` rows** take marks; a submitted or locked paper → `409 MARKS_NOT_EDITABLE`.
- `subjectCode`, `componentCode`, `examDocsId`, `studentAcademicRecordDocsId`, `enteredByDocsId` and
  `enteredAt` are the server's.
- The paper's `version`, required and moved on — the decision already taken for attendance #2.
- **Results lock** runs.

<a id="e20"></a>
**[20](#t20) · `GET /exam-schedules/{scheduleId}/answer-copies`** — every booklet number of a paper
(main and supplementary) with its entered mark or none. **No child's name, id or roll number.** What
an evaluator is handed.

<a id="e21"></a>
**[21](#t21) · `PUT /exam-schedules/{scheduleId}/marks/by-copy`** —
`{ "marks": [{ "answerCopyNo": "MATH-0041", "obtainedMarks": 64.5 }], "version": n }`. The server
resolves the booklet to the child through `exam_attendances` and writes the same row #19 would — **the
copy number never lands on `StudentMark`** (model README section 6). A supplementary booklet number
resolves to its child too. Unknown booklet → `404 ANSWER_COPY_NOT_FOUND`. Every other rule is #19's.

<a id="e22"></a>
**[22](#t22) · `POST /exam-schedules/{scheduleId}/marks/submit`** — `{ "version": n }`. Every
`DRAFT` row → `SUBMITTED`. **Refused while any child who sat has no mark** (`409 UNENTERED_MARKS`,
with the count). Children with no row yet who were `ABSENT` get an `ABSENT` row with no marks here,
so a card can show "absent" rather than a gap. **Results lock** runs.

<a id="e23"></a>
**[23](#t23) · `POST /exam-schedules/{scheduleId}/marks/lock`** — `{ "version": n }`. Every
`SUBMITTED` row → `LOCKED`, `lockedAt`, `lockedByDocsId`. Refused while any row is still `DRAFT`
(`409 MARKS_NOT_SUBMITTED`). **Results lock** runs.

<a id="e24"></a>
**[24](#t24) · `POST /exam-schedules/{scheduleId}/marks/unlock`** — `{ "reason": "…", "version":
n }`. `LOCKED` → `SUBMITTED` — not back to `DRAFT`, so a revaluation goes through submit and lock
again. **Refused once a published card uses these marks** (`409 USED_BY_PUBLISHED_CARD`) — revoke
the card first ([#35](#e35)). **Results lock** runs: a frozen term cannot be unlocked here.

<a id="e25"></a>
**[25](#t25) · `PATCH /student-marks/{markId}/grace`** — `{ "graceMarks": 2, "graceReason": "…",
"version": n }`. **`obtainedMarks` is never touched** — the evaluator's award stays as awarded.
`graceReason` is required with any non-zero grace; `obtainedMarks + graceMarks ≤ maximumMarks`.
Allowed on `SUBMITTED` and `LOCKED` rows ([open item 8](#8-grace-marks)), refused once a published card
uses the mark. **Results lock** runs. The row's own `version`, because grace is one child at a time.

## Results

<a id="e26"></a>
**[26](#t26) · `GET /exams/{examId}/mark-entry`** — one row per paper: class, section, subject,
paper status, marks not entered / draft / submitted / locked. Filter `?pending=true` for the papers
still holding something up. **The exam office's "who hasn't entered marks"**, the twin of
[attendance #8](../attendance/README.md#e8).

<a id="e27"></a>
**[27](#t27) · `GET /exams/{examId}/results/{classDocsId}/{sectionNo}`** — **the tabulation
sheet**: one row per child, one column per paper (effective marks, or `AB` / `EX` / `WH`), total,
percentage, grade, pass or fail. Reads marks in any status, and says per column whether it is final.
**Not a rank** — ranks exist only on a published card ([open item 10](#10-rank)).

<a id="e28"></a>
**[28](#t28) · `GET /students/{studentDocsId}/marks`** — every mark one child has, filtered by term
or exam, grouped by exam. What a parent-teacher meeting opens on.

## Report cards

<a id="e29"></a>
**[29](#t29) · `POST /report-cards/generate`** — `{ "termDocsId": "…", "classDocsId": "…",
"sectionNo": "A" }`

- **Every non-cancelled paper of every non-cancelled exam in the term, for this section, must be
  `LOCKED`** → `409 MARKS_NOT_LOCKED`, naming the papers. The model README's lock-before-publish.
- **Weights checked here**: in a term, either no exam is weighted or the weighted ones add to 100 →
  `409 WEIGHTS_DO_NOT_ADD_UP` ([open item 9](#9-exam-weights)).
- One card per child on the section's roster at the term's end. **Snapshotted**: term name, class
  name, section, roll number, scheme, every subject's components and totals, grades from the scheme
  in force, pass/fail ([open item 13](#13-pass-and-fail)).
- **Attendance days from the daily register**, the term's dates, counted as
  [attendance #10](../attendance/README.md#e10) counts them — DAILY only, LATE attended, cancelled
  registers out, only the days the child was in the section.
- **Re-running replaces `DRAFT` and `REVIEWED` cards** in place (back to `DRAFT`); a `PUBLISHED` one is
  left alone; after a `REVOKED` one it makes `reportVersion + 1`. `classRank` stays empty until #34.
- **Results lock** runs.

<a id="e30"></a>
**[30](#t30) · `GET /report-cards`** — a section's cards for a term, the latest version of each, with
total, percentage, grade, result and status. Filter by status.

<a id="e31"></a>
**[31](#t31) · `GET /report-cards/{cardId}`** — one card exactly as stored. **Reads no other
collection** — a published card must reprint years later from itself alone (model README section 9).

<a id="e32"></a>
**[32](#t32) · `PATCH /report-cards/{cardId}/remarks`** — `classTeacherRemark`, `principalRemark`,
and per subject `teacherRemark`, `version`. `DRAFT` or `REVIEWED` only → `409 CARD_PUBLISHED`. A
remark changed on a `REVIEWED` card sends it back to `DRAFT`.

<a id="e33"></a>
**[33](#t33) · `POST /report-cards/{cardId}/review`** — `{ "version": n }`. `DRAFT` → `REVIEWED`.

<a id="e34"></a>
**[34](#t34) · `POST /report-cards/publish`** — `{ "termDocsId": "…", "classDocsId": "…",
"sectionNo": "A" }`. **Every card of the section at once**, because ranks are frozen together: a rank
set while a classmate's card is still a draft would be wrong the moment that card changes. Refused
while any card in the section is still `DRAFT` (`409 CARDS_NOT_REVIEWED`, with the count). Sets
`classRank`, `rankedStudentCount`, `publishedAt`, `publishedByDocsId`. Allowed while the term's
results are locked.

<a id="e35"></a>
**[35](#t35) · `POST /report-cards/{cardId}/revoke`** — `{ "reason": "…", "version": n }`.
`PUBLISHED` → `REVOKED`. The marks behind it can then be unlocked and corrected, and #29 makes the
next version. **Never edited in place.**

## Holistic progress cards

<a id="e36"></a>
**[36](#t36) · `POST /holistic-progress-cards/generate`** — `{ "termDocsId": "…", "classDocsId":
"…", "sectionNo": "A", "nepStage": "MIDDLE" }`. One `DRAFT` per child on the roster who has none for
the term — existing ones are left alone. Class, section, roll and attendance days copied in as #29
does. **`nepStage` is sent**, never derived from the class name — the model says why.

<a id="e37"></a>
**[37](#t37) · `PATCH /holistic-progress-cards/{cardId}`** — `domains` (each `domain`, `level`,
`observation`, `strengths`, `nextStep`), `teacherRemark`, `selfReflection`, `peerFeedback`,
`parentFeedback` with `parentFeedbackByGuardianDocsId` (must be one of the child's guardians),
`developmentGoals`, `version`. A domain at most once. `DRAFT` or `REVIEWED` only.

<a id="e38"></a>
**[38](#t38) · `POST /holistic-progress-cards/{cardId}/review`** — refused while any domain has a
level but no `observation` (`400 OBSERVATION_REQUIRED`, naming the domains) — the model README: a
card of bare levels is unfinished.

<a id="e39"></a>
**[39](#t39) · `POST /holistic-progress-cards/{cardId}/publish`** — `REVIEWED` → `PUBLISHED`. One
card at a time: there is no rank to keep in step.

<a id="e40"></a>
**[40](#t40) · `POST /holistic-progress-cards/{cardId}/revoke`** — `revocationReason` required.
The next #36 for that child makes `cardVersion + 1`.

<a id="e41"></a>
**[41](#t41) · `GET /holistic-progress-cards`** and <a id="e42"></a>**[42](#t42) · `GET
/holistic-progress-cards/{cardId}`** — as [#30](#e30) and [#31](#e31).

---

# To settle before building

## 1. Who sits a paper

**The roster on the paper's date** — the same rule as attendance: an academic record in that class and
section covering `examDate`, any status except `CANCELLED`, through the existing
[`StudentAcademicRecordRepository.findInSectionOn`](../../../repositories/student/academicrecord/StudentAcademicRecordRepositoryCustom.java).
The model README says "ACTIVE records", which would drop a child who left the day after the paper;
the date rule is what attendance already uses.

**The elective problem**: nothing upstream records which child takes which elective, so a section's
Sanskrit paper would list every child in the section. **Options:** (a) everyone in the section is
expected, and a child who does not take the subject is marked `EXEMPT` at [#19](#e19); (b) add a
per-child subject list to `StudentAcademicRecord` first (a model change). **Recommendation: (a)
now** — it works for every core subject, which is most papers — and (b) when electives matter.

## 2. The exam and paper status graphs

`ExamStatus` is `DRAFT, SCHEDULED, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED`, and
`ExamScheduleStatus` is `DRAFT, SCHEDULED, PUBLISHED, COMPLETED, CANCELLED`. The model README says
"Exam published → status SCHEDULED", which uses `SCHEDULED` to mean published. **Options:** (a)
`SCHEDULED` is unused — `DRAFT` → `PUBLISHED` at [#3](#e3); (b) `SCHEDULED` means "datesheet final,
not yet shown to families", with its own endpoint before #3. **Recommendation: (a)** — nothing in this
plan shows a datesheet to families yet, so the difference between the two would be invisible.
`IN_PROGRESS` is set by the server on the first hall save ([#15](#e15)), never sent.

## 3. Submit and lock are per paper, not per child

`MarkEntryStatus` sits on every `StudentMark` row, so a single child's mark could in principle be
submitted or locked on its own. **Recommendation:** [#22](#e22)–[#24](#e24) move **every row of a
paper together** — a teacher submits Maths for 7A, not one child's Maths — and the row status is
just where that is recorded. `PUBLISHED` in `MarkEntryStatus` is left unused: a mark is published
through the card that carries it.

## 4. Hall rows ahead of time, or not

The model README says rows are created ahead of time "so an absent child leaves a row not a gap".
**Options:** (a) create a row per expected child when the paper is published; (b) no rows until
marked, the register shows the unmarked, and [#16](#e16) refuses to complete while anyone is unmarked
— exactly how attendance works. **Recommendation: (b)** — it reaches the same guarantee (nobody is
silently missing once a paper is complete), needs no "pending" status the enum does not have, and a
child who joins the section after publishing is picked up automatically.

## 5. `markedByDocsId` is required

`ExamAttendance.markedByDocsId` is `@NotBlank`, but the caller is not always a staff member — the
admin and the local user have no staff id. **Options:** (a) make it nullable, set from the signed-in
staff when there is one — what attendance does for `lockedByDocsId`; (b) the request names who
invigilated, and it must be one of the paper's invigilators. **Recommendation: (a)** — it is one
annotation, and (b) can be added on top when roles exist.

## 6. Where correction reasons are stored

[#5](#e5), [#10](#e10), [#17](#e17) and [#24](#e24) all take a reason, and none of the models has
anywhere to keep one. **Options:** (a) an `unlocks`-style history on `ExamSchedule` (reason, when, who,
what) like [`AttendanceUnlock`](../../../models/academics/attendance/embedded/AttendanceUnlock.java), and
a `cancellationReason` on `Exam` and `ExamSchedule`; (b) log the reason only. **Recommendation: (a)**
— you asked for the unlock reason to be stored in attendance, and the same question will be asked
here.

## 7. One version for the paper

Hall marks, mark entry, submit, lock and unlock are all *per paper*, so they all take **the paper's
`version`**, moved on with every save — the decision taken for attendance #2. Only grace ([#25](#e25))
uses the mark row's own version. **Recommendation: confirm.**

## 8. Grace marks

When may grace be given? **Recommendation:** on `SUBMITTED` and `LOCKED` rows (moderation happens
after marking), never past the maximum, never once a published card uses the mark, and never
negative. A school that moderates *down* is a later question.

## 9. Exam weights

The model README requires weighted exams in a term to add to 100. Exams are created one at a time, so
the sum is wrong until the last one exists. **Recommendation:** check it **only at [#29](#e29)** —
either no exam in the term is weighted (raw totals), or the weighted ones add to 100 and none is
unweighted. Term weights for an annual result belong to a year-end card, not in this plan.

## 10. Rank

`classRank` says *class*, but cards are generated per section. **Options:** (a) rank within the
section; (b) rank within the whole class, which means publishing every section together. **Also:**
ties share a rank (1, 1, 3), and a child with a `WITHHELD` or `INCOMPLETE` result is not ranked.
**Recommendation: (a)**, and rename nothing — the field can mean the section's class.

## 11. What the results lock blocks

**Recommendation:** every write that changes a result — [#19](#e19), [#21](#e21)–[#25](#e25),
[#29](#e29) — and **not** [#32](#e32)–[#34](#e34): remarks, review and publish change no number, and
a school typically freezes marks *and then* publishes.

## 12. Gate 4 and the final term's cards

Gate 4 refuses writes in a year that is not running. A final term's cards are often published after
the new year has started running. **Options:** (a) keep gate 4 everywhere, as attendance does; (b)
drop it from report cards and progress cards only ([#29](#e29)–[#40](#e40)). **Recommendation: (b)**
— the results lock already protects the numbers, and gate 4 would make the final term's cards
impossible to publish.

## 13. Pass and fail

**Recommendation:** a subject is passed when the effective mark reaches the paper's `passingMarks`
summed over its components; a subject with no passing mark always passes. The card is `FAIL` when any
subject with `countsTowardTotal` fails, `WITHHELD` when any mark is `WITHHELD`, `INCOMPLETE` when a
paper has no mark, otherwise `PASS`. Grade-only subjects never fail.

---

# Where the code will live

```text
controllers/academics/examination/
    ExamController.java                 #1–#7, #26–#28 — the exam, and its results
    ExamScheduleController.java         #8–#13 — the datesheet
    ExamAttendanceController.java       #14–#17 — the hall
    StudentMarkController.java          #18–#25 — marks
    ReportCardController.java           #29–#35
    HolisticProgressCardController.java #36–#42

services/academics/
    ExamService.java · ExamScheduleService.java · ExamAttendanceService.java
    StudentMarkService.java · ReportCardService.java · HolisticProgressCardService.java
    utils/ExaminationServiceUtils.java  loading an exam/paper, the results lock, the paper roster

repositories/academics/examination/
    ExamRepository · ExamScheduleRepository · ExamAttendanceRepository
    StudentMarkRepository · ReportCardRepository · HolisticProgressCardRepository
    (+ Custom/Impl where an aggregation is needed — #7, #12, #26, #27)

dto/academics/examination/{request,response}/
```

The attendance-days count [#29](#e29) and [#36](#e36) need is the one
[attendance #10](../attendance/README.md#e10) already does; it moves to a shared util rather than being
written twice.

---

# The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `EXAM_NOT_FOUND` · `EXAM_SCHEDULE_NOT_FOUND` · `STUDENT_MARK_NOT_FOUND` · `REPORT_CARD_NOT_FOUND` · `PROGRESS_CARD_NOT_FOUND` | 404 | No such row in this school and year. |
| `EXAM_NAME_TAKEN` | 409 | [#1](#e1)/[#2](#e2) — the name is used in the year, case-insensitively. |
| `EXAM_DATES_OUTSIDE_TERM` | 409 | [#1](#e1)/[#2](#e2) — the window is not inside the term. |
| `EXAM_HAS_PAPERS` · `PAPERS_OUTSIDE_DATES` · `EXAM_NOT_EDITABLE` | 409 | [#2](#e2). |
| `EXAM_HAS_NO_PAPERS` · `PAPERS_STILL_OPEN` | 409 | [#3](#e3), [#4](#e4). |
| `SUBJECT_NOT_TAUGHT_IN_SECTION` | 400 | [#8](#e8). **Names the sections.** |
| `PAPER_ALREADY_SCHEDULED` · `PAPER_OUTSIDE_EXAM` | 409 | [#8](#e8)/[#9](#e9). |
| `ROOM_CLASH` · `INVIGILATOR_CLASH` | 409 | [#8](#e8)/[#9](#e9). **Names the other paper.** |
| `PASSING_MARKS_ABOVE_MAXIMUM` · `INVALID_TIME_RANGE` | 400 | [#8](#e8)/[#9](#e9). |
| `MARKS_ALREADY_ENTERED` · `MARKS_ALREADY_LOCKED` | 409 | A paper or exam change that would contradict marks. |
| `PAPER_NOT_OPEN` · `PAPER_NOT_YET_SAT` · `PAPER_NOT_COMPLETED` | 409 | Hall and mark writes at the wrong moment. |
| `STUDENT_NOT_IN_PAPER` | 400 | [#15](#e15)/[#19](#e19). **Names them.** |
| `ANSWER_COPY_REQUIRED` · `ANSWER_COPY_NOT_ALLOWED` | 400 | [#15](#e15). |
| `ANSWER_COPY_TAKEN` | 409 | [#15](#e15). **Names the booklet and who has it.** |
| `ANSWER_COPY_NOT_FOUND` | 404 | [#21](#e21). |
| `STUDENT_ABSENT` · `MARKS_OUT_OF_RANGE` · `GRADE_NOT_IN_SCHEME` | 400 | [#19](#e19)/[#21](#e21). **Name the child.** |
| `MARKS_NOT_EDITABLE` · `UNENTERED_MARKS` · `MARKS_NOT_SUBMITTED` | 409 | [#19](#e19), [#22](#e22), [#23](#e23). |
| `USED_BY_PUBLISHED_CARD` | 409 | [#24](#e24)/[#25](#e25). |
| `RESULTS_LOCKED` | 409 | The year's or term's results are frozen. **Says which.** |
| `MARKS_NOT_LOCKED` · `WEIGHTS_DO_NOT_ADD_UP` | 409 | [#29](#e29). **Name the papers / exams.** |
| `CARD_PUBLISHED` · `CARDS_NOT_REVIEWED` | 409 | [#32](#e32), [#34](#e34). |
| `OBSERVATION_REQUIRED` | 400 | [#38](#e38). **Names the domains.** |
| `UNMARKED_STUDENTS` · `REASON_REQUIRED` · `CONCURRENT_MODIFICATION` · `NOT_A_WORKING_DAY` | — | Shared with attendance. |
