# controllers/academics/examination — API plan

**Sixteen of 49 are built — [#1](#e1), [#2](#e2), [#6](#e6) and [#7](#e7) on 2026-10-16, [#8](#e8) on 2026-10-19, [#11](#e11) and [#12](#e12) on 2026-10-21, [#3](#e3), [#43](#e43)–[#46](#e46) and [#14](#e14)–[#16](#e16) and [#18](#e18) on 2026-10-22. Phases 1 and 2 are done; phase 3 has begun.**

Written 2026-10-15, redesigned 2026-10-16 around the candidate list —
[what changed](#what-changed-on-2026-10-16). This is the full set of endpoints examinations need,
written before any of them so they can be built and reviewed one at a time — the same way
[`attendance`](../attendance/README.md), [`timetable`](../timetable/README.md) and
[`student`](../../student/README.md) were done. Built endpoints will be marked **built** in the `#`
column.

Mirrors [`models/academics/examination`](../../../models/academics/examination) — six collections,
already written. The model README
([`examination/README.md`](../../../models/academics/examination/README.md)) is the persistence
contract — who sits, booklet uniqueness, attendance before marks, lock before publish, snapshots;
this file is what may be done to it over HTTP. Where the two disagree, the model README wins until
it is changed.

[Open items](#to-settle-before-building) 2 and 6 need an answer before the first endpoint is built —
they change what phase 1 writes. Items 1, 4 and 5 are settled.

## What changed on 2026-10-16

- **Who may sit is a list.** [#43](#e43)–[#49](#e49): the office adds candidates and debars the
  ones who may not sit (fees due, attendance short…), with a reason. A paper's hall register lists
  only eligible candidates.
- **Hall attendance lives inside the candidate** (`ExamCandidate.papers`). There is no
  `exam_attendances` collection.
- **A mark exists only for a child who sat.** `StudentMark` carries no participation status, no
  subject and no component; absent, unfair means, exempt and debarred are read from the candidate.
- **No exam code** — an exam's name is unique in the year.
- **Report cards** snapshot every paper of the term's exams with its exam and weight, and say why a
  mark is missing. **Progress cards** start empty, are checked at review, and take each guardian's
  feedback even after publishing ([#50](#e50)).
- **Removed:** #48 (a candidate's subjects).

---

## What this module is

**An exam event, its datesheet, who may sit it, who sat each paper, what they scored, and the
term's report card.** The office creates "Half Yearly, Term 1", lays out the datesheet — Maths for
7A, 7B and 7C on 12 November, 9:00 to 12:00, hall 2 — and publishes it. It adds the children who
may sit, and **debars** the ones who may not — fees unpaid, say — with a reason. On the day the
invigilator's register lists **only the eligible children**, and they mark who sat the paper and
which answer booklet they wrote in. A week later the teacher enters marks, submits them,
and they are locked. At the end of the term every child's marks become a report card, and the class
teacher writes the holistic progress card beside it.

| Document | Collection | What it holds |
|---|---|---|
| [`Exam`](../../../models/academics/examination/Exam.java) | [`exams`](../../../models/academics/examination/Exam.java) | One row per exam event, per year. What moves it: `DRAFT` → `PUBLISHED` → `IN_PROGRESS` → `COMPLETED`, or → `CANCELLED` ([open item 2](#2-the-exam-and-paper-status-graphs)) |
| [`ExamCandidate`](../../../models/academics/examination/ExamCandidate.java) | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) | One row per child allowed to sit, per exam. What moves it: `ELIGIBLE` ⇄ `DEBARRED`, every change kept with its reason — **and the hall attendance**: `papers[]`, one entry per paper, `PRESENT`, `ABSENT`, `UNFAIR_MEANS` or `EXEMPT` with the answer booklet |
| [`ExamSchedule`](../../../models/academics/examination/ExamSchedule.java) | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) | One row per **one paper**: exam + class + section + subject + component. What moves it: the datesheet row, and the hall register's header |
| [`StudentMark`](../../../models/academics/examination/StudentMark.java) | [`student_marks`](../../../models/academics/examination/StudentMark.java) | One row per child, per paper **they sat**. What moves it: `DRAFT` → `SUBMITTED` → `LOCKED` |
| [`ReportCard`](../../../models/academics/examination/ReportCard.java) | [`report_cards`](../../../models/academics/examination/ReportCard.java) | One row per child, per term, per version. What moves it: `DRAFT` → `REVIEWED` → `PUBLISHED`, or → `REVOKED` |
| [`HolisticProgressCard`](../../../models/academics/examination/HolisticProgressCard.java) | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) | One row per child, per term, per version. What moves it: the same four states |

**A paper is the unit everything hangs off.** `ExamSchedule` is to the candidates' `papers` entries
and to `StudentMark` what `AttendanceSession` is to `StudentAttendanceRecord`: the header that owns
the date, time, room and status. **Hall attendance is embedded in the candidate** — there is no
`exam_attendances` collection, so only a candidate can ever be marked. The hall register and the
mark sheet are both *a paper's*, and most endpoints below are under `/exam-schedules/{scheduleId}`.

**The candidate list decides who is on a hall register — not the section.** A paper's register is
the section's children on the paper's date (from their academic records) who are `ELIGIBLE`
candidates of the exam. A debarred child's name does not appear, so no attendance or marks can be
recorded for them until they are let back in. See [who may sit](#who-may-sit--candidates).

### Models from other packages

| Model | Lives in | Used for |
|---|---|---|
| [AcademicYear](../../../models/core/AcademicYear.java) | `core` | the year in every path, working days, and `resultsLocked` |
| [AcademicTerm](../../../models/academics/structure/AcademicTerm.java) | `academics/structure` | the term an exam and a card belong to, its `weightPercent` and `resultsLocked` |
| [SchoolClass](../../../models/academics/structure/SchoolClass.java) · [ClassSection](../../../models/academics/structure/embedded/ClassSection.java) · [ClassSubject](../../../models/academics/structure/embedded/ClassSubject.java) | `academics/structure` | which sections exist, and which subjects each one is taught — a paper is only for a subject the section has |
| [GradingScheme](../../../models/academics/grading/GradingScheme.java) | `academics/grading` | marks into a grade: `ClassSubject.gradingSchemeDocsId` → `Exam.gradingSchemeDocsId` → none |
| [StudentAcademicRecord](../../../models/student/StudentAcademicRecord.java) | `student` | **a child's class and section on a date** — the candidate stores neither ([open item 1](#1-who-sits-a-paper)) |
| [Student](../../../models/student/Student.java) | `student` | names on the candidate list, hall register, mark sheet and card |
| [Guardian](../../../models/student/Guardian.java) | `student` | who wrote each parent feedback on a progress card ([#50](#e50)) |
| [Staff](../../../models/people/staff/Staff.java) | `people/staff` | invigilators, and who debarred, marked, entered, locked, reviewed, published and revoked |
| [Room](../../../models/room/Room.java) | `room` | the exam room |
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

**What the marker in the `#` column means**, the same four words [`controllers/core`](../../core/README.md)
uses:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. 16 of them. |
| **deferred** | Planned, not started, and not next. |
| **optional** | A convenience nobody is waiting on. |
| **not being built** | Decided against, struck through, and the reason is in its own section. |

Anything unmarked does not exist yet, and a request to it returns a 404.

## 1. The exam — writes and reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>#1 — **built** | [`POST /exams`](#e1) | **Create an exam** — name, term, type, dates, weight, grading scheme. Starts `DRAFT`. | [`exams`](../../../models/academics/examination/Exam.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t2"></a>#2 — **built** | [`PATCH /exams/{examId}`](#e2) | Fix the name, dates, weight or grading scheme. **Never the term** once papers exist. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |
| <a id="t3"></a>#3 — **built** | [`POST /exams/{examId}/publish`](#e3) | **Publish the datesheet** — the exam and every paper in it become visible. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |
| <a id="t4"></a>#4 | [`POST /exams/{examId}/complete`](#e4) | Every paper is over. Refused while a paper is still open. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |
| <a id="t5"></a>#5 | [`POST /exams/{examId}/cancel`](#e5) | Call the whole exam off, with a reason. Refused once any marks are locked. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t6"></a>#6 — **built** | [`GET /exams?termDocsId=&examType=&status=&search=`](#e6) | The year's exams, filtered and paged, with a paper count each. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |
| <a id="t7"></a>#7 — **built** | [`GET /exams/{examId}`](#e7) | **One exam in full** — its term, scheme, and how far along it is: papers held, marks entered, marks locked. | [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |

## 2. The datesheet — papers · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t8"></a>#8 — **built** | [`POST /exams/{examId}/schedules`](#e8) | **Add a paper** — one subject, one class, **one or more sections** at the same date and time. One row per section. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`rooms`](../../../models/room/Room.java), [`staff`](../../../models/people/staff/Staff.java) |
| <a id="t9"></a>#9 | [`PATCH /exam-schedules/{scheduleId}`](#e9) | Move a paper, change its room, invigilators or marks. Maximum marks is frozen once any mark is entered. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t10"></a>#10 | [`POST /exam-schedules/{scheduleId}/cancel`](#e10) | Drop one paper, with a reason. Refused once marks are locked. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t11"></a>#11 — **built** | [`GET /exams/{examId}/schedules?classDocsId=&sectionNo=&date=`](#e11) | **The datesheet** — every paper, by date and time. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`rooms`](../../../models/room/Room.java) |
| <a id="t12"></a>#12 — **built** | [`GET /exam-schedules/{scheduleId}`](#e12) | One paper in full, with its hall and mark counts. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t13"></a>#13 | [`GET /exam-schedules?date=&invigilatorDocsId=&roomDocsId=`](#e13) | **The day's papers across every exam** — what each hall holds, and who is on duty. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |

## 2b. Who may sit — candidates · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t43"></a>#43 — **built** | [`POST /exams/{examId}/candidates`](#e43) | **Add children who may sit** — a whole section, or the ones named. They start `ELIGIBLE`. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java) |
| <a id="t44"></a>#44 — **built** | [`GET /exams/{examId}/candidates?classDocsId=&sectionNo=&status=&debarmentReason=`](#e44) | **The candidate list** — who may sit, who is debarred and why, with counts. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`students`](../../../models/student/Student.java) |
| <a id="t45"></a>#45 — **built** | [`POST /exam-candidates/{candidateId}/debar`](#e45) | **Stop a child from sitting**, with a reason — fees due, attendance shortage… Their name leaves every paper still to come. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) |
| <a id="t46"></a>#46 — **built** | [`POST /exam-candidates/{candidateId}/readmit`](#e46) | **Let them back in** — fees paid on the morning of the paper. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) |
| <a id="t47"></a>#47 | [`POST /exams/{examId}/candidates/debar`](#e47) | Debar **several at once** — the fee defaulters' list. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) |
| <a id="t48"></a>~~#48~~ **not being built** | **removed 2026-10-16** — `PUT /exam-candidates/{candidateId}/subjects` | The subject is on the paper; a candidate sits every paper of their section. See [open item 1](#1-who-sits-a-paper). | — |
| <a id="t49"></a>#49 | [`DELETE /exam-candidates/{candidateId}`](#e49) | Remove a child added by mistake. Refused once they have a hall entry or a mark. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |

## 3. In the hall — attendance · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t14"></a>#14 — **built** | [`GET /exam-schedules/{scheduleId}/attendance`](#e14) | **The hall register** — every **eligible candidate** for the paper, with their hall entry or "not marked". Debarred children are not listed. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`students`](../../../models/student/Student.java) |
| <a id="t15"></a>#15 — **built** | [`PUT /exam-schedules/{scheduleId}/attendance`](#e15) | **Mark the hall** — present, absent, unfair means or exempt, the answer booklet, seat, times. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) |
| <a id="t16"></a>#16 — **built** | [`POST /exam-schedules/{scheduleId}/complete`](#e16) | **The paper is over.** Refused while anyone is unmarked — `EXEMPT` counts as marked. Closes hall attendance. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) |
| <a id="t17"></a>#17 | [`POST /exam-schedules/{scheduleId}/reopen`](#e17) | Re-open a completed paper's hall register to correct it, with a reason. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |

## 4. Marks · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t18"></a>#18 — **built** | [`GET /exam-schedules/{scheduleId}/marks`](#e18) | **The mark sheet** — every child with a hall entry; those present with their mark or "not entered". | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`students`](../../../models/student/Student.java) |
| <a id="t19"></a>#19 | [`PUT /exam-schedules/{scheduleId}/marks`](#e19) | **Enter marks** by child — one, several or all. Re-sending corrects. | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t20"></a>#20 | [`GET /exam-schedules/{scheduleId}/answer-copies`](#e20) | **For blind evaluation** — booklet numbers with no names. | [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t21"></a>#21 | [`PUT /exam-schedules/{scheduleId}/marks/by-copy`](#e21) | Enter marks **by booklet number**. The server finds the child. | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) |
| <a id="t22"></a>#22 | [`POST /exam-schedules/{scheduleId}/marks/submit`](#e22) | The teacher is done — `DRAFT` → `SUBMITTED`. Refused while anyone who sat is unmarked. | [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t23"></a>#23 | [`POST /exam-schedules/{scheduleId}/marks/lock`](#e23) | **Final** — `SUBMITTED` → `LOCKED`. What report cards wait for. | [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t24"></a>#24 | [`POST /exam-schedules/{scheduleId}/marks/unlock`](#e24) | **Revaluation** — back to `SUBMITTED`, with a reason. Refused once a card using them is published. | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t25"></a>#25 | [`PATCH /student-marks/{markId}/grace`](#e25) | **Grace marks** — moderation beside the award, never over it, with a reason. | [`student_marks`](../../../models/academics/examination/StudentMark.java) |

## 5. Results — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t26"></a>#26 | [`GET /exams/{examId}/mark-entry`](#e26) | **The office's question**: which papers have marks entered, submitted, locked — and which have none. | [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`student_marks`](../../../models/academics/examination/StudentMark.java) |
| <a id="t27"></a>#27 | [`GET /exams/{examId}/results/{classDocsId}/{sectionNo}`](#e27) | **The tabulation sheet** — a row per child, a column per subject, total, percentage, grade. | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`students`](../../../models/student/Student.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t28"></a>#28 | [`GET /students/{studentDocsId}/marks?termDocsId=&examDocsId=`](#e28) | One child's marks across papers. | [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`exams`](../../../models/academics/examination/Exam.java) |

## 6. Report cards · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t29"></a>#29 | [`POST /report-cards/generate`](#e29) | **Compile a section's cards for a term** from locked marks. Builds or rebuilds `DRAFT` cards. | [`report_cards`](../../../models/academics/examination/ReportCard.java), [`student_marks`](../../../models/academics/examination/StudentMark.java), [`exams`](../../../models/academics/examination/Exam.java), [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java), [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java), [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) |
| <a id="t30"></a>#30 | [`GET /report-cards?termDocsId=&classDocsId=&sectionNo=&status=`](#e30) | A section's cards, latest version each. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t31"></a>#31 | [`GET /report-cards/{cardId}`](#e31) | One card in full — what gets printed. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t32"></a>#32 | [`PATCH /report-cards/{cardId}/remarks`](#e32) | Class teacher's, principal's and subject teachers' remarks. Only before publishing. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t33"></a>#33 | [`POST /report-cards/{cardId}/review`](#e33) | `DRAFT` → `REVIEWED` — checked and ready. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t34"></a>#34 | [`POST /report-cards/publish`](#e34) | **Publish a section's reviewed cards at once** — ranks freeze together. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |
| <a id="t35"></a>#35 | [`POST /report-cards/{cardId}/revoke`](#e35) | Withdraw a published card, with a reason. The next generate is `reportVersion + 1`. | [`report_cards`](../../../models/academics/examination/ReportCard.java) |

## 7. Holistic progress cards · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t36"></a>#36 | [`POST /holistic-progress-cards/generate`](#e36) | **Start a section's cards for a term** — one empty `DRAFT` per child, class and roll copied in. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java), [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) |
| <a id="t37"></a>#37 | [`PATCH /holistic-progress-cards/{cardId}`](#e37) | **Write it** — domains with level and observation, the child's reflection, peers, family, goals. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java), [`guardians`](../../../models/student/Guardian.java) |
| <a id="t38"></a>#38 | [`POST /holistic-progress-cards/{cardId}/review`](#e38) | `DRAFT` → `REVIEWED`. **Where the card is checked** — refused while a policy domain is missing or any domain has no observation. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) |
| <a id="t39"></a>#39 | [`POST /holistic-progress-cards/{cardId}/publish`](#e39) | `REVIEWED` → `PUBLISHED`. Never edited afterwards. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) |
| <a id="t40"></a>#40 | [`POST /holistic-progress-cards/{cardId}/revoke`](#e40) | Withdraw it, with a reason (`revocationReason`). | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) |
| <a id="t41"></a>#41 | [`GET /holistic-progress-cards?termDocsId=&classDocsId=&sectionNo=&status=`](#e41) | A section's cards, latest version each. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) |
| <a id="t42"></a>#42 | [`GET /holistic-progress-cards/{cardId}`](#e42) | One card in full. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) |
| <a id="t50"></a>#50 | [`PUT /holistic-progress-cards/{cardId}/parent-feedback`](#e50) | **A guardian's own view** — added or replaced, even after the card is published. | [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java), [`guardians`](../../../models/student/Guardian.java) |

---

<a id="build-order"></a>
# Build order

Each phase ends with something that works end to end.

| Phase | What you get at the end of it | Endpoints | |
|---|---|---|---|
| **1** | An exam exists with a datesheet, and it can be read back and published | [#1](#e1), [#7](#e7), [#8](#e8), [#11](#e11), [#3](#e3), [#6](#e6) | built |
| **2** | **Who may sit**, then a paper can be sat — the hall register, marked and closed | [#43](#e43), [#44](#e44), [#45](#e45), [#46](#e46), [#14](#e14), [#15](#e15), [#16](#e16), [#12](#e12) | built |
| **3** | **Marks can be entered and made final** | [#18](#e18), [#19](#e19), [#22](#e22), [#23](#e23) | part |
| **4** | The numbers a school is asked for | [#27](#e27), [#26](#e26), [#28](#e28) | |
| **5** | **A term's report cards, published** | [#29](#e29), [#31](#e31), [#30](#e30), [#32](#e32), [#33](#e33), [#34](#e34) | |
| **6** | Every correction path | [#47](#e47), [#49](#e49), [#2](#e2), [#9](#e9), [#10](#e10), [#17](#e17), [#24](#e24), [#25](#e25), [#35](#e35), [#4](#e4), [#5](#e5) | part |
| **7** | Blind evaluation, and the day view | [#20](#e20), [#21](#e21), [#13](#e13) | |
| **8** | Holistic progress cards | [#36](#e36) to [#42](#e42), [#50](#e50) | |

**Phases 1–3 are the module working**: an exam, a paper, who sat it, what they scored, locked. Phase 5
is what a school actually hands a family. **Corrections come late on purpose** — the same reason as
attendance: they are easier to build, and to check, once there is real data to correct.

**Holistic progress cards are last because nothing depends on them.** They share no rows with marks,
and the only thing they read from this module is the term.

---

<a id="the-endpoints-in-detail"></a>
# Appendix — what every API touches, field by field

## The exam

<a id="e1"></a>
**[#1](#t1) · `POST /exams`** — built 2026-10-16 — *create an exam*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `academicYear`, `active`, `name`, `startDate`, `endDate` — the term must be this year's and in use, and the exam's dates must sit inside it
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `active`, `name` — only when `gradingSchemeDocsId` is sent; it must be this school's and in use
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `name` — must be free in the year, ignoring case, cancelled exams too
- [`exams`](../../../models/academics/examination/Exam.java) — *inserts*: `schoolId`, `academicYear`, `name`, `termDocsId`, `examType`, `startDate`, `endDate`, `weightPercent`, `gradingSchemeDocsId`, `status` = `DRAFT`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "name": "Half Yearly Examination, Term 1",
                        // REQUIRED, max 150
  "termDocsId": "6a9e77b0c1d2e3f405162738",
                        // REQUIRED, max 60
  "examType": "MIDTERM",
                        // REQUIRED — UNIT_TEST, FORMATIVE,
                        // SUMMATIVE, MIDTERM, FINAL,
                        // PRACTICAL, PRE_BOARD or BOARD
  "startDate": "2026-11-10",   // REQUIRED
  "endDate": "2026-11-20",     // REQUIRED
  "weightPercent": 40,         // optional, above 0, at most 100
  "gradingSchemeDocsId": "6a9e8811a2b3c4d5e6f70819"
                        // optional, max 60
}
</pre></td>
<td><pre>
201 Created — ExamResponse
Location: /schools/current/academic-years/{year}/exams/{examDocsId}

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "academicYear": "2026-2027",
  "name": "Half Yearly Examination, Term 1",
  "termDocsId": "6a9e77b0c1d2e3f405162738",
  "termName": "Term 1",
  "examType": "MIDTERM",
  "startDate": "2026-11-10",
  "endDate": "2026-11-20",
  "weightPercent": 40,
  "gradingSchemeDocsId": "6a9e8811a2b3c4d5e6f70819",
  "gradingSchemeName": "CBSE Percentage Grading",
  "status": "DRAFT",
  "paperCount": 0,
  "version": 0,
  "nextStep": "Next, add the exam's papers — POST /exams/6a9f10c2e4b1a7d3c5e80a11/schedules (#8, not built yet)."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{year}` (path) | **yes** | The year's name, such as `2026-2027`. |
| `name` | **yes** | Max 150, not blank. Unique in the year, ignoring upper and lower case. |
| `termDocsId` | **yes** | Max 60. An active term of this year. |
| `examType` | **yes** | `UNIT_TEST`, `FORMATIVE`, `SUMMATIVE`, `MIDTERM`, `FINAL`, `PRACTICAL`, `PRE_BOARD` or `BOARD`. |
| `startDate` | **yes** | The first day of the exam window. Inside the term. |
| `endDate` | **yes** | The last day. Inside the term, and not before `startDate`. |
| `weightPercent` | no | Above 0, at most 100. Left out, the exam has no weight. |
| `gradingSchemeDocsId` | no | Max 60. This school's scheme, still in use. Left out, the exam has no default scheme. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_DATE_RANGE` | `startDate` is after `endDate`. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `TERM_NOT_FOUND` | There is no term of that id in this year. |
| 409 | `TERM_NOT_ACTIVE` | The term has been retired. |
| 409 | `EXAM_DATES_OUTSIDE_TERM` | The dates are not inside the term. The message names the term. |
| 404 | `GRADING_SCHEME_NOT_FOUND` | A `gradingSchemeDocsId` was sent and it is not this school's. |
| 409 | `GRADING_SCHEME_NOT_ACTIVE` | The grading scheme has been retired. |
| 409 | `EXAM_NAME_TAKEN` | The year already has an exam of that name, in any case, cancelled ones too. |
| 400 | `VALIDATION_FAILED` | A required field is missing, or `weightPercent` is not above 0 and at most 100. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, with two refusals added:** a retired term is `409 TERM_NOT_ACTIVE` (rather than
a 404, since the term does exist), and a retired grading scheme is `409 GRADING_SCHEME_NOT_ACTIVE`.
`ExamController` / `ExamService.createExam`, answering `ExamResponse` with the term's and scheme's
names. The year is read through `DailyTimetableServiceUtils.loadYearByName`, as attendance does.
The name check is `ExamRepository.findFirstBySchoolIdAndAcademicYearAndNameIgnoreCase`.

```json
{ "name": "Half Yearly Examination, Term 1",
  "termDocsId": "…", "examType": "MIDTERM",
  "startDate": "2026-11-10", "endDate": "2026-11-20",
  "weightPercent": 40, "gradingSchemeDocsId": "…" }
```

- **`name` unique in the year**, case-insensitively, retired and cancelled exams included
  (`school_year_exam_name_uniq`) → `409 EXAM_NAME_TAKEN`. There is no exam code — removed
  2026-10-16, the same decision as terms: everything references an exam by `examDocsId`.
- The term must be in this year → `404 TERM_NOT_FOUND`, and active → `409 TERM_NOT_ACTIVE`.
- **Both dates inside the term**, `startDate ≤ endDate` → `400 INVALID_DATE_RANGE`,
  `409 EXAM_DATES_OUTSIDE_TERM`. Not checked against holidays: an exam *window* can span a weekend;
  each paper's own date is checked ([#8](#e8)).
- `weightPercent` optional, `0 < w ≤ 100`. Whether a term's weights add to 100 is checked when a card
  is generated, not here — exams are added one at a time ([open item 9](#9-exam-weights)).
- `gradingSchemeDocsId` optional; when sent, it must be this school's → `404
  GRADING_SCHEME_NOT_FOUND`, and active → `409 GRADING_SCHEME_NOT_ACTIVE`.
- Starts `DRAFT`. Gates 1, 2, 4.

<a id="e2"></a>
**[#2](#t2) · `PATCH /exams/{examId}`** — built 2026-10-16

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `version` — must match; `status` — not `COMPLETED` or `CANCELLED`; `name` of the year's other exams — a new name must be free
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `examDate`, `status` — a new term is refused while the exam has papers, and new dates must still hold every paper (cancelled ones left out)
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `active`, `startDate`, `endDate`, `name` — the term the dates are checked against, and a newly chosen term
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `active`, `name` — only when `gradingSchemeDocsId` is sent and not empty
- [`exams`](../../../models/academics/examination/Exam.java) — *updates*: `name`, `termDocsId`, `startDate`, `endDate`, `weightPercent`, `gradingSchemeDocsId` — only what was sent; `version` moves on

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}/exams/{examDocsId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "name": "Half Yearly Examination, Term 1",
                        // optional, max 150, not ""
  "termDocsId": "6a9e77b0c1d2e3f405162738",
                        // optional, max 60 — only
                        // while the exam has no papers
  "startDate": "2026-11-10",   // optional
  "endDate": "2026-11-21",     // optional
  "weightPercent": 40,         // optional, above 0, at most 100
  "gradingSchemeDocsId": "",
                        // optional, max 60 — "" removes it
  "version": 1                 // REQUIRED
}

// Everything but version optional, but a body
// with nothing else is a 400.
</pre></td>
<td><pre>
200 OK — ExamResponse, the same shape #1 returns

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "academicYear": "2026-2027",
  "name": "Half Yearly Examination, Term 1",
  "termDocsId": "6a9e77b0c1d2e3f405162738",
  "termName": "Term 1",
  "examType": "MIDTERM",
  "startDate": "2026-11-10",
  "endDate": "2026-11-21",
  "weightPercent": 40,
  "status": "DRAFT",
  "paperCount": 0,
  "version": 2
}

// nextStep is not sent.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year. |
| `name` | no | Max 150. Free in the year, ignoring case. `""` is refused — a name can be replaced, not removed. |
| `termDocsId` | no | Max 60. An active term of this year. Only while the exam has no papers. |
| `startDate` | no | Sent alone, the stored `endDate` is kept, then both are checked against the term. |
| `endDate` | no | Sent alone, the stored `startDate` is kept. |
| `weightPercent` | no | Above 0, at most 100. Cannot be cleared. |
| `gradingSchemeDocsId` | no | Max 60. `""` removes the scheme; an id must be this school's and in use. |
| `version` | **yes** | The exam's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 400 | `NOTHING_TO_UPDATE` | Nothing but the version was sent. |
| 400 | `EXAM_NAME_REQUIRED` | `"name": ""` — a name can be replaced, not removed. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The exam changed since this version was read. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 409 | `EXAM_HAS_PAPERS` | A new term was sent, and the exam already has papers. |
| 404 | `TERM_NOT_FOUND` | The new term is not this year's. |
| 409 | `TERM_NOT_ACTIVE` | The new term has been retired. |
| 400 | `INVALID_DATE_RANGE` | The dates end up the wrong way round. |
| 409 | `EXAM_DATES_OUTSIDE_TERM` | The dates end up outside the term. |
| 409 | `PAPERS_OUTSIDE_DATES` | A paper would fall outside the new dates. The message names them. |
| 404 | `GRADING_SCHEME_NOT_FOUND` | A `gradingSchemeDocsId` was sent and it is not this school's. |
| 409 | `GRADING_SCHEME_NOT_ACTIVE` | A newly chosen scheme has been retired. |
| 409 | `EXAM_NAME_TAKEN` | The new name is used this year, ignoring case. |
| 400 | `VALIDATION_FAILED` | No version, or `weightPercent` is not above 0 and at most 100. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`name`, `startDate`, `endDate`, `weightPercent`,
`gradingSchemeDocsId`, `termDocsId`, `version`. A new `name` must be free in the year → `409
EXAM_NAME_TAKEN`. **`termDocsId` only while the exam has no papers** (`409 EXAM_HAS_PAPERS`) — a
term move would put papers outside their exam's window. New dates must still hold every paper → `409
PAPERS_OUTSIDE_DATES`, naming them. Refused on `COMPLETED` or `CANCELLED` (`409
EXAM_NOT_EDITABLE`).

**Built with these details:** only what is sent changes; nothing but the version is `400
NOTHING_TO_UPDATE`; `"name": ""` is `400 EXAM_NAME_REQUIRED`; the dates are a pair — one sent alone
keeps the other, then both are checked; `weightPercent` cannot be cleared, but
`"gradingSchemeDocsId": ""` removes the scheme; a newly chosen retired term or scheme is a `409`.
`examType` is not editable — the plan did not list it. Measured 2026-10-16: every refusal as
documented; "Unit Test 1" renamed and back, version 0 → 1 → 2.

<a id="e3"></a>
**[#3](#t3) · `POST /exams/{examId}/publish`** — built 2026-10-22 — *publish the datesheet*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `version` — must match; `status` — must be `DRAFT`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `status`, `rooms`, `sectionNo`, `examDate`, `startTime`, `endTime` — there must be papers, each with a room, and no section may sit two at once (cancelled papers left out)
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `name` — for the answer
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `name` — for the answer, when the exam has a scheme
- [`exams`](../../../models/academics/examination/Exam.java) — *updates*: `status` = `PUBLISHED`, `publishedAt`, `publishedByDocsId` (the signed-in staff, when there is one)
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: `status` `DRAFT` → `PUBLISHED`, for every draft paper, in the same transaction

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examDocsId}/publish
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 3          // REQUIRED — the exam's version
}
</pre></td>
<td><pre>
200 OK — ExamResponse, the same shape #1 returns

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "academicYear": "2026-2027",
  "name": "Half Yearly Examination, Term 1",
  "termDocsId": "6a9e77b0c1d2e3f405162738",
  "termName": "Term 1",
  "examType": "MIDTERM",
  "startDate": "2026-11-10",
  "endDate": "2026-11-20",
  "weightPercent": 40,
  "gradingSchemeDocsId": "6a9e8811a2b3c4d5e6f70819",
  "gradingSchemeName": "CBSE Percentage Grading",
  "status": "PUBLISHED",
  "publishedAt": "2026-10-22T05:30:00Z",
  "publishedByDocsId": "6a9e5f00a1b2c3d4e5f60718",
  "paperCount": 3,
  "version": 4,
  "nextStep": "3 paper(s) published with the datesheet. Next, add who may sit each paper."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year. |
| `version` | **yes** | The exam's version as last read. A changed exam is refused, so nobody publishes a datesheet they have not seen. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The exam changed since this version was read. |
| 409 | `EXAM_ALREADY_PUBLISHED` | The exam is `PUBLISHED` or `IN_PROGRESS`. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 409 | `EXAM_HAS_NO_PAPERS` | The exam has no papers, cancelled ones left out. |
| 409 | `PAPERS_WITHOUT_ROOMS` | A paper has no room. The message names them. |
| 409 | `SECTION_CLASH` | A section sits two of the exam's papers at once. The message names them. |
| 400 | `VALIDATION_FAILED` | No version. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, with two more refusals:** a paper with no room is `409 PAPERS_WITHOUT_ROOMS`
(families are told where to go), and a section sitting two of the exam's papers at once is
`409 SECTION_CLASH` — [#8](#e8) refuses that when a paper is added, but papers added before the rule
may still clash. Already out is `409 EXAM_ALREADY_PUBLISHED`; `COMPLETED`/`CANCELLED` is
`409 EXAM_NOT_EDITABLE`. Open item 2 went with **option (a)**: `DRAFT` → `PUBLISHED`, `SCHEDULED`
unused. `ExamController.publish` / `ExamService.publishExam` (`@Transactional` — the exam and its
papers save together), answering `ExamResponse` with a `nextStep`. Papers are read with
`ExamScheduleRepository.findBySchoolIdAndExamDocsIdAndStatusNot`. Measured: New Test refused with
`SECTION_CLASH` (English A and Maths A, 13 Nov 09:00–12:00); a test exam refused with no papers, then
with a roomless paper, then published (its paper `PUBLISHED`, version 0 → 1), refused a second time,
and a paper added afterwards came out `PUBLISHED`.

Planned: `{ "version": n }`. The datesheet goes out: the exam
and every `DRAFT` paper move to `PUBLISHED`, `publishedAt` and `publishedByDocsId` set. Refused with
no papers (`409 EXAM_HAS_NO_PAPERS`). A paper added after publishing is `PUBLISHED` from the start.
**Which states exist at all is [open item 2](#2-the-exam-and-paper-status-graphs).**

<a id="e4"></a>
**[#4](#t4) · `POST /exams/{examId}/complete`** — planned

- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `version`, `status`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `status` — every paper must be `COMPLETED` or `CANCELLED`
- [`exams`](../../../models/academics/examination/Exam.java) — *updates*: `status` → `COMPLETED`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examId}/complete
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 5          // REQUIRED — the exam's version
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examId}` (path) | **yes** | The exam's id. |
| `version` | **yes** | The exam's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `PAPERS_STILL_OPEN` | A paper is not yet `COMPLETED` or `CANCELLED`. The message gives the count. |
| 409 | `CONCURRENT_MODIFICATION` | The exam changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "version": n }`. Refused while any paper is not
`COMPLETED` or `CANCELLED` (`409 PAPERS_STILL_OPEN`, with the count). Marks can still be entered and
locked afterwards — completing the exam is about the hall, not the marks.

<a id="e5"></a>
**[#5](#t5) · `POST /exams/{examId}/cancel`** — planned

- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `version`, `status`
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — any `LOCKED` mark in the exam refuses the cancel
- [`exams`](../../../models/academics/examination/Exam.java) — *updates*: `status` → `CANCELLED`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: `status` → `CANCELLED`, for every paper not already cancelled
- Where the reason is stored is open item 6.

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examId}/cancel
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "…",        // REQUIRED — max length not set yet
  "version": 5          // REQUIRED — the exam's version
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examId}` (path) | **yes** | The exam's id. |
| `reason` | **yes** | Why the exam is called off. Where it is stored is open item 6. |
| `version` | **yes** | The exam's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `MARKS_ALREADY_LOCKED` | A mark in the exam is already `LOCKED`. |
| 409 | `CONCURRENT_MODIFICATION` | The exam changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "…", "version": n }`. The exam and
every paper that is not already cancelled → `CANCELLED`. **Refused once any mark in it is `LOCKED`**
(`409 MARKS_ALREADY_LOCKED`) — a cancelled exam with final marks is a contradiction. Hall attendance
and draft marks are kept, and stop counting. Where the reason is stored is
[open item 6](#6-where-correction-reasons-are-stored).

<a id="e6"></a>
**[#6](#t6) · `GET /exams?termDocsId=&examType=&status=&search=`** — built 2026-10-16

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: one page of the year's exams, filtered by `termDocsId`, `examType`, `status` and `name`
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `name` — every term of the year, for `termName`
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `name` — the schemes on the page, for `gradingSchemeName`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `examDocsId`, `status` — the paper count of each exam on the page, cancelled ones left out, in one aggregation

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams
    ?termDocsId=6a9e77b0c1d2e3f405162738
                        // optional
    &examType=MIDTERM   // optional — an ExamType
    &status=DRAFT       // optional — DRAFT, SCHEDULED,
                        // PUBLISHED, IN_PROGRESS,
                        // COMPLETED or CANCELLED
    &search=half        // optional — part of the name,
                        // ignoring case
    &page=0             // optional
    &size=20            // optional, default 20, at most 100
    &sort=startDate     // optional — startDate, endDate,
                        // name, createdAt, updatedAt,
                        // each with ,desc
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — PageResponse of ExamResponse

{
  "content": [
    {
      "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
      "academicYear": "2026-2027",
      "name": "Half Yearly Examination, Term 1",
      "termDocsId": "6a9e77b0c1d2e3f405162738",
      "termName": "Term 1",
      "examType": "MIDTERM",
      "startDate": "2026-11-10",
      "endDate": "2026-11-20",
      "weightPercent": 40,
      "gradingSchemeDocsId": "6a9e8811a2b3c4d5e6f70819",
      "gradingSchemeName": "CBSE Percentage Grading",
      "status": "DRAFT",
      "paperCount": 0,
      "version": 0
    }
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
| `termDocsId` | no | Only this term's exams. |
| `examType` | no | Only exams of this type. A type that does not exist is a 400. |
| `status` | no | Only exams in this status. A status that does not exist is a 400. |
| `search` | no | Part of the name, ignoring case. Blank is the same as absent. |
| `page` | no | The page, from 0. |
| `size` | no | Rows per page. 20 when absent, at most 100. |
| `sort` | no | `startDate`, `endDate`, `name`, `createdAt` or `updatedAt`, each with `,desc`. By `startDate` then `name` when absent. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 400 | `VALIDATION_FAILED` | An `examType` or `status` that does not exist. |
| 400 | `INVALID_PAGE` · `INVALID_PAGE_SIZE` · `INVALID_SORT_FIELD` | A bad page, page size or sort field. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

filters `termDocsId`, `examType`, `status`,
`search` (name), paged, by `startDate` then `name` (the name is unique in the year, so paging is
stable). `?sort=` accepts `startDate`, `endDate`, `name`, `createdAt`, `updatedAt`. Each row is the
exam with its `termName`, `gradingSchemeName` and **`paperCount`** — papers, cancelled ones left out,
counted for the whole page in one aggregation
(`ExamScheduleRepository.countPapersByExam`). An unknown `examType` or `status` is `400
VALIDATION_FAILED`; an unknown year is a 404, not an empty page. No gates. Measured 2026-10-16:
Term 1 — "Unit Test 1", `UNIT_TEST`, 40%, CBSE Percentage Grading, 0 papers, `DRAFT`.

**#1 was measured the same day** against the real terms: dates the wrong way round, outside the
term, an unknown term or scheme, a weight of 0 and no name all answered as documented; "Unit Test 1"
was created (201, `DRAFT`) and "unit test 1" was then `409 EXAM_NAME_TAKEN`.

<a id="e7"></a>
**[#7](#t7) · `GET /exams/{examId}`** — built 2026-10-16

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: the whole exam
- [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `name` — shown by id alone if the term has gone
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `name` — when the exam has a scheme
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `status` — papers per status, in one aggregation
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — marks per status, in one aggregation

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams/{examDocsId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — ExamDetailResponse

{
  "exam": {
    "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
    "academicYear": "2026-2027",
    "name": "Half Yearly Examination, Term 1",
    "termDocsId": "6a9e77b0c1d2e3f405162738",
    "termName": "Term 1",
    "examType": "MIDTERM",
    "startDate": "2026-11-10",
    "endDate": "2026-11-20",
    "weightPercent": 40,
    "gradingSchemeDocsId": "6a9e8811a2b3c4d5e6f70819",
    "gradingSchemeName": "CBSE Percentage Grading",
    "status": "DRAFT",
    "paperCount": 0,
    "version": 0
  },
  "paperTotal": 0,
  "papersByStatus": {
    "DRAFT": 0, "SCHEDULED": 0, "PUBLISHED": 0,
    "COMPLETED": 0, "CANCELLED": 0
  },
  "markTotal": 0,
  "marksByStatus": {
    "DRAFT": 0, "SUBMITTED": 0, "LOCKED": 0,
    "PUBLISHED": 0
  }
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

the exam (the same shape as #1 and #6),
then `papersByStatus` with `paperTotal` and `marksByStatus` with `markTotal` — **every status listed,
0 where there are none**. Two aggregations (`ExamScheduleRepository.countByStatusForExam`,
`StudentMarkRepository.countByStatusForExam`). Scoped by the year in the path. What the exam's own
page opens on. Measured 2026-10-16: "Unit Test 1" — 0 papers, 0 marks.

## The datesheet

<a id="e8"></a>
**[#8](#t8) · `POST /exams/{examId}/schedules`** — built 2026-10-19 — *add a paper*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist; `holidays` — the date must be a working day
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `status` — not `COMPLETED` or `CANCELLED`; `startDate`, `endDate` — the paper's date must be inside them; `name` — for the answer
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`; `sections` (`sectionNo`, `active`) — each section must exist and be in use; `subjects` (`subjectCode`, `name`, `sectionNo`, `active`) — the subject must be taught in each section
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: papers already there — the same paper for a section, a section's other papers that day, the rooms' other sittings that day and the invigilators' other duties that day
- [`rooms`](../../../models/room/Room.java) — *reads*: `name`, `status`, `capacity` — each room must be this school's and in use, and must fit everyone sitting there
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `fullName` — each invigilator must be this school's staff
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *inserts*: one row per section — `schoolId`, `academicYear`, `examDocsId`, `classDocsId`, `sectionNo`, `subjectCode`, `componentName`, `examDate`, `startTime`, `endTime`, `maximumMarks`, `passingMarks`, `rooms` (`roomDocsId`, `seatCount`, `invigilatorDocsIds`), `status` = `DRAFT` (or `PUBLISHED` once the datesheet is out)

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examDocsId}/schedules
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
                        // REQUIRED, max 60
  "sectionNos": ["A", "B", "C"],
                        // REQUIRED, 1 to 30, each max 20
  "subjectCode": "MATH",       // REQUIRED, max 60
  "componentName": "Theory",   // optional, max 120
  "examDate": "2026-11-12",    // REQUIRED
  "startTime": "09:00",        // REQUIRED
  "endTime": "12:00",          // REQUIRED, after startTime
  "maximumMarks": 80,          // REQUIRED, above 0
  "passingMarks": 27,          // optional, 0 to maximumMarks
  "rooms": [                   // optional, at most 20
    { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
                        // REQUIRED, max 60
      "seatCount": 200,        // optional, at least 1
      "invigilatorDocsIds": ["6a9e5f00a1b2c3d4e5f60718"]
                        // optional, at most 20,
                        // each max 60
    }
  ]
}
</pre></td>
<td><pre>
201 Created — ExamPapersAddedResponse
Location: /schools/current/academic-years/{year}/exams/{examDocsId}/schedules

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "paperCount": 3,
  "papers": [
    {
      "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
      "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
      "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
      "className": "Class 7",
      "sectionNo": "A",
      "subjectCode": "MATH",
      "subjectName": "Mathematics",
      "componentName": "Theory",
      "examDate": "2026-11-12",
      "startTime": "09:00",
      "endTime": "12:00",
      "maximumMarks": 80,
      "passingMarks": 27,
      "rooms": [
        { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
          "roomName": "Main Hall",
          "seatCount": 200,
          "invigilators": [
            { "staffDocsId": "6a9e5f00a1b2c3d4e5f60718",
              "fullName": "Abhay Kulkarni" } ] }
      ],
      "totalSeats": 200,
      "status": "DRAFT",
      "version": 0
    },
    …the papers of B and C, the same shape
  ],
  "nextStep": "200 seat(s) for these 3 section(s) across 1 room(s). Add the exam's other papers the same way."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year, not `COMPLETED` or `CANCELLED`. |
| `classDocsId` | **yes** | Max 60. A class of this year. |
| `sectionNos` | **yes** | 1 to 30 sections, each max 20. Each must be in the class and in use; matched ignoring case and stored the class's way. A section twice is a 400. |
| `subjectCode` | **yes** | Max 60. Taught in every section sent. Matched ignoring case. |
| `componentName` | no | Max 120. The part of the subject — "Theory", "Practical". Left out when the subject has one paper. |
| `examDate` | **yes** | Inside the exam's dates, and a working day. |
| `startTime` | **yes** | The school's own time. |
| `endTime` | **yes** | After `startTime`. |
| `maximumMarks` | **yes** | Above 0. |
| `passingMarks` | no | 0 or more, and at most `maximumMarks`. |
| `rooms` | no | At most 20. Left out while the datesheet is a draft. Each room once. |
| `rooms[].roomDocsId` | **yes** | Max 60. This school's room, in use. |
| `rooms[].seatCount` | no | At least 1. How many sit there from all the sections sent together. Without it, the room cannot be shared with another sitting. |
| `rooms[].invigilatorDocsIds` | no | At most 20, each max 60. This school's staff. A person may be in one room only. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_TIME_RANGE` | `startTime` is not before `endTime`. |
| 400 | `PASSING_MARKS_ABOVE_MAXIMUM` | `passingMarks` is more than `maximumMarks`. |
| 400 | `DUPLICATE_SECTION_IN_REQUEST` | A section is in `sectionNos` twice. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 409 | `PAPER_OUTSIDE_EXAM` | The date is not inside the exam's dates. |
| 404 | `NOT_A_WORKING_DAY` | The date is a holiday or weekly off. The message names it. |
| 404 | `CLASS_NOT_FOUND` | There is no class of that id in this year. |
| 400 | `SECTION_NOT_IN_CLASS` | A section the class does not have. The message names them. |
| 409 | `SECTION_NOT_ACTIVE` | A section that has been switched off. |
| 400 | `SUBJECT_NOT_TAUGHT_IN_SECTION` | The subject is not taught in a section. The message names them. |
| 409 | `PAPER_ALREADY_SCHEDULED` | A section already has this paper in this exam. The message names them. |
| 409 | `SECTION_CLASH` | A section already sits another paper then, in any exam. The message names it. |
| 400 | `DUPLICATE_ROOM_IN_REQUEST` | A room is in `rooms` twice. |
| 400 | `INVIGILATOR_IN_TWO_ROOMS` | A person is on duty in two rooms of the paper. |
| 404 | `ROOM_NOT_FOUND` | A room that is not this school's. The message names them. |
| 409 | `ROOM_NOT_IN_USE` | A room is under maintenance or closed. |
| 409 | `ROOM_CLASH` | A room holds another sitting then, and has no capacity or no seat count. |
| 409 | `ROOM_OVER_CAPACITY` | Everyone in a room at that time, from any exam, would pass its capacity. |
| 404 | `STAFF_NOT_FOUND` | An invigilator who is not this school's staff. |
| 409 | `INVIGILATOR_CLASH` | An invigilator is on duty in another room then. The message names it. |
| 400 | `VALIDATION_FAILED` | A required field is missing, or `maximumMarks` is not above 0. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, with these details:** `ExamScheduleController` / `ExamScheduleService.addPapers`
(one transaction), answering `ExamPapersAddedResponse` — the papers, each with the class, subject,
room and invigilators named. Extra refusals: `400 DUPLICATE_SECTION_IN_REQUEST`,
`409 SECTION_NOT_ACTIVE`, `404 CLASS_NOT_FOUND`, `404 ROOM_NOT_FOUND`, `409 ROOM_NOT_IN_USE`,
`404 STAFF_NOT_FOUND`, and `409 EXAM_NOT_EDITABLE`. Sections and the subject code are matched ignoring
case and stored the class's way. "The same sitting" — which may share a room and invigilators — is
the same exam, subject, part (`componentName`), date and times. A `RoomRepository` was added for the
room check. Room capacity against the number sitting is **not** checked yet. Measured 2026-10-19 on
"Unit Test 1": "a", "B", "c" and "mathematics" saved as three DRAFT papers A, B and C, MATHEMATICS;
the repeat, an invigilator clash, a subject not taught, a holiday (10 November is Diwali), an unknown
section, class, room or staff member all answered as documented.

**Several rooms per paper, 2026-10-21** — measured with two temporary rooms (a 60-seat hall, a
20-seat room), removed afterwards: sections A and B with 25 in the hall (two invigilators) and 10 in
the room (one) were added, 35 seats each; section C with 25 more in the hall was `ROOM_OVER_CAPACITY`
(75 > 60, counting the 50 already there); an invigilator already in the other room of the sitting
was `INVIGILATOR_CLASH`; another subject overlapping in the hall was `ROOM_CLASH`; a room twice, a
person in two rooms, an unknown room and 0 seats were all refused.

```json
{ "classDocsId": "…", "sectionNos": ["A", "B", "C"],
  "subjectCode": "MATH", "componentName": "Theory",
  "examDate": "2026-11-12", "startTime": "09:00", "endTime": "12:00",
  "maximumMarks": 80, "passingMarks": 27,
  "rooms": [
    { "roomDocsId": "…", "seatCount": 200, "invigilatorDocsIds": ["…", "…"] },
    { "roomDocsId": "…", "seatCount": 50,  "invigilatorDocsIds": ["…"] } ] }
```

**One subject, one class, one or more sections** — because "Maths for all of Class 7 on the 12th" is
how a datesheet is written. One `exam_schedules` row per section, in one transaction; the answer
lists them.

- Every section must exist in the class; **the subject must be taught in each one** (a `ClassSubject`
  for the whole class or for that section) → `400 SUBJECT_NOT_TAUGHT_IN_SECTION`, naming the
  sections.
- **No component code** (removed 2026-10-21). A subject split into parts names each in
  `componentName` — "Theory", "Practical"; a subject with one paper leaves it out. Unique on exam +
  class + section + subject + `componentName`, ignoring case → `409 PAPER_ALREADY_SCHEDULED`, naming
  the section.
- **`examDate` inside the exam's window and a working day** → `409 PAPER_OUTSIDE_EXAM`,
  `404 NOT_A_WORKING_DAY`. `startTime < endTime` → `400 INVALID_TIME_RANGE`.
- `0 < maximumMarks`, `passingMarks ≤ maximumMarks` → `400 PASSING_MARKS_ABOVE_MAXIMUM`.
- **A paper uses one room or several** (changed 2026-10-21) — `rooms`, each with `seatCount` — **how
  many sit there from all the sections sent together, not one section**, optional (2026-10-22) — and its **own invigilators**: 200 in the hall with two people on
  duty, 50 in Room 12 with one. Each room once (`400 DUPLICATE_ROOM_IN_REQUEST`); a person on duty in
  one room only (`400 INVIGILATOR_IN_TWO_ROOMS`). Optional while the datesheet is a draft.
- **A section sits one paper at a time** (added 2026-10-21): none of the sections may already have
  another paper — in any exam — at an overlapping time that day → `409 SECTION_CLASH`, naming it.
  Measured: section A with English 09:00–12:00 refused Mathematics1 at 10:00–11:00; a paper that only
  touches (12:00 after a 12:00 end) is allowed.
- **A room's seats are counted once per sitting**, not once per section — a sitting being the papers
  with the same exam, subject, part, date and times (no stored id; removed 2026-10-22). Sections
  joining a sitting later in a room it already uses add to its seats, and the new papers carry the
  room's new total. Measured: Room 12 (40) holding the
  Maths sitting of A, B and C with 3 seats refused 38 more (41) and took 37 (40).
- **A room can hold more than one exam at a time** (changed 2026-10-21) — a big hall with space
  left. Everyone sitting in it at an overlapping time, from any exam or sitting, must fit its
  `capacity` → `409 ROOM_OVER_CAPACITY`, saying how many would sit there. A room with **no capacity
  recorded**, or a sitting in it with **no `seatCount`** (sent or stored), cannot be shared with
  another sitting → `409 ROOM_CLASH`. With no other sitting there, a room without seats is accepted
  unchecked.
  Measured: Main Hall (200) with 20 already in it took 150 more for another class (201) and refused
  190 more (210 > 200).
- **A person is in one place at a time:** an invigilator already on duty in **another room** at an
  overlapping time → `409 INVIGILATOR_CLASH`. The same room is the same place, so one person may watch
  two exams sharing a hall. Measured: Abhay, on duty in Room 12, was refused for Main Hall at 10:00.
- **Later, at the hall:** each child's hall entry records which of the paper's rooms they sat in
  (`ExamPaperAttendance.roomDocsId`), and the rooms' seats must cover the eligible candidates before
  the paper can be completed ([#15](#e15), [#16](#e16)).
- Refused when the exam is `COMPLETED` or `CANCELLED`.

<a id="e9"></a>
**[#9](#t9) · `PATCH /exam-schedules/{scheduleId}`** — planned

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `version`, `status`, and the same rows #8 reads for its checks
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: whether any mark is entered for the paper — then `maximumMarks` and `passingMarks` are frozen
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: what was sent — `examDate`, `startTime`, `endTime`, `rooms`, `maximumMarks`, `passingMarks`, `componentName`; `version` moves on

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}/exam-schedules/{scheduleId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "examDate": "2026-11-13",    // optional
  "startTime": "09:00",        // optional
  "endTime": "12:00",          // optional
  "rooms": [ … ],              // optional — as in #8
  "maximumMarks": 80,          // optional
  "passingMarks": 27,          // optional
  "componentName": "Theory",   // optional
  "version": 2                 // REQUIRED — the paper's version
}

// The plan names these as "date, times, room,
// invigilators, marks, component name, version".
// The field names here are #8's.
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `examDate`, `startTime`, `endTime` | no | The same checks as #8. Frozen once the paper is `COMPLETED`. |
| `rooms` (with seats and invigilators) | no | The same checks as #8. |
| `maximumMarks`, `passingMarks` | no | The same checks as #8. Frozen once any mark is entered. |
| `componentName` | no | The same checks as #8. |
| `version` | **yes** | The paper's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| — | every #8 refusal | The same checks as #8. |
| 409 | `MARKS_ALREADY_ENTERED` | `maximumMarks` or `passingMarks` sent, and a mark is already entered. |
| 409 | not named yet | The date or time sent, and the paper is `COMPLETED`. |
| 409 | `CONCURRENT_MODIFICATION` | The paper changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

date, times, room, invigilators, marks,
component name, `version`. The same checks as #8. **`maximumMarks` and `passingMarks` are frozen
once any mark is entered** (`409 MARKS_ALREADY_ENTERED`) — a mark of 70 means nothing if the paper
becomes out of 50. Date and time are frozen once the paper is `COMPLETED`.

<a id="e10"></a>
**[#10](#t10) · `POST /exam-schedules/{scheduleId}/cancel`** — planned

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `version`, `status`
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — refused once the paper's marks are `LOCKED`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: `status` → `CANCELLED`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/cancel
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "…",        // REQUIRED — max length not set yet
  "version": 2          // REQUIRED — the paper's version
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `reason` | **yes** | Why the paper is dropped. Where it is stored is open item 6. |
| `version` | **yes** | The paper's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `MARKS_ALREADY_LOCKED` | The paper's marks are locked. |
| 409 | `CONCURRENT_MODIFICATION` | The paper changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "…", "version": n }`.
Refused once its marks are locked. The paper drops out of the datesheet, the tabulation and every
card.

<a id="e11"></a>
**[#11](#t11) · `GET /exams/{examId}/schedules?classDocsId=&sectionNo=&date=`** — built 2026-10-21

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `name`, `startDate`, `endDate`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: every paper of the exam that matches the filters, by date and start time
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `subjects` (`subjectCode`, `name`) — the class and subject names
- [`rooms`](../../../models/room/Room.java) — *reads*: `name` — the rooms on the datesheet
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `fullName` — the invigilators on the datesheet

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams/{examDocsId}/schedules
    ?classDocsId=6a9e91d4f5a6b7c8d9e0f1a2
                        // optional
    &sectionNo=b        // optional, ignoring case
    &date=2026-11-12    // optional, YYYY-MM-DD
    &status=PUBLISHED   // optional — DRAFT, SCHEDULED,
                        // PUBLISHED, COMPLETED or
                        // CANCELLED
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — ExamDatesheetResponse

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "startDate": "2026-11-10",
  "endDate": "2026-11-20",
  "paperCount": 3,
  "papers": [
    {
      "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
      "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
      "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
      "className": "Class 7",
      "sectionNo": "A",
      "subjectCode": "MATH",
      "subjectName": "Mathematics",
      "componentName": "Theory",
      "examDate": "2026-11-12",
      "startTime": "09:00",
      "endTime": "12:00",
      "maximumMarks": 80,
      "passingMarks": 27,
      "rooms": [
        { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
          "roomName": "Main Hall",
          "seatCount": 200,
          "invigilators": [
            { "staffDocsId": "6a9e5f00a1b2c3d4e5f60718",
              "fullName": "Abhay Kulkarni" } ] }
      ],
      "totalSeats": 200,
      "status": "DRAFT",
      "version": 0
    },
    …
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year. |
| `classDocsId` | no | Only this class's papers. |
| `sectionNo` | no | Only this section's papers, ignoring case. |
| `date` | no | Only the papers on this day. `YYYY-MM-DD`. |
| `status` | no | Only papers in this status. Absent shows every paper, cancelled ones included and marked. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 400 | `VALIDATION_FAILED` | A date that is not `YYYY-MM-DD`, or a status that does not exist. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**the datesheet**: by date,
then start time, then class name, section and subject. Each paper carries its class and subject
names, its rooms (seats and invigilators, named) and `totalSeats`. Filters for one class, one section
(ignoring case), one date, one status; cancelled papers are shown, marked, unless `status` filters
them out. **Not paged** — a datesheet is read whole. Five reads: the exam, the papers, then the
classes, rooms and staff behind them, one each (`ExamScheduleService.getDatesheet`,
`ExamScheduleRepository.findDatesheet`). Measured 2026-10-21 on "Unit Test 1": the three Mathematics
papers; `?sectionNo=b` gave B; a day with no papers gave 0; a malformed date or status was `400
VALIDATION_FAILED`; an unknown exam `404`.

<a id="e12"></a>
**[#12](#t12) · `GET /exam-schedules/{scheduleId}`** — built 2026-10-21

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the whole paper
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `name`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `subjects` (`subjectCode`, `name`)
- [`rooms`](../../../models/room/Room.java) — *reads*: `name`
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `fullName` — the invigilators
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `effectiveFrom`, `effectiveUntil`, `status` — who is in the section on the paper's date
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `status`, `papers` (`examScheduleDocsId`, `attendanceStatus`) — who may sit, and how they were marked here
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — the paper's marks per status

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exam-schedules/{examScheduleDocsId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — ExamScheduleDetailResponse

{
  "paper": {
    "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
    "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
    "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
    "className": "Class 7",
    "sectionNo": "A",
    "subjectCode": "MATH",
    "subjectName": "Mathematics",
    "componentName": "Theory",
    "examDate": "2026-11-12",
    "startTime": "09:00",
    "endTime": "12:00",
    "maximumMarks": 80,
    "passingMarks": 27,
    "rooms": [
      { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
        "roomName": "Main Hall",
        "seatCount": 200,
        "invigilators": [
          { "staffDocsId": "6a9e5f00a1b2c3d4e5f60718",
            "fullName": "Abhay Kulkarni" } ] }
    ],
    "totalSeats": 200,
    "status": "DRAFT",
    "version": 0
  },
  "examName": "Half Yearly Examination, Term 1",
  "hall": {
    "sectionSize": 40,
    "eligible": 38,
    "debarred": 2,
    "notCandidates": 0,
    "expected": 38,
    "present": 36,
    "absent": 1,
    "unfairMeans": 0,
    "exempt": 1,
    "notMarked": 0
  },
  "marks": {
    "byStatus": { "DRAFT": 30, "SUBMITTED": 0,
                  "LOCKED": 0, "PUBLISHED": 0 },
    "entered": 30,
    "notEntered": 6
  }
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examScheduleDocsId}` (path) | **yes** | The paper's id. Must be a paper of this year. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

one paper (the shape #8
and #11 return), its exam's name, then **`hall`**: `sectionSize` (the section's children on the
paper's date), `eligible`, `debarred` and `notCandidates` among them, **`expected`** (eligible
candidates, plus anyone already marked at this paper), and `present`, `absent`, `unfairMeans`,
`exempt`, `notMarked`; then **`marks`**: `byStatus` (every status listed), `entered`, and
`notEntered` (present with no mark). Reads the roster through
`StudentAcademicRecordRepository.findInSectionOn` and the candidates through a new
`ExamCandidateRepository`. Measured 2026-10-21: Class 1 A Maths — 2 in the section, both not
candidates, 0 expected; with two temporary candidates (one eligible and marked present, one
debarred) — eligible 1, debarred 1, expected 1, present 1, not marked 0, not entered 1. A paper of an
unknown id is `404 EXAM_SCHEDULE_NOT_FOUND`.

<a id="e13"></a>
**[#13](#t13) · `GET /exam-schedules?date=&invigilatorDocsId=&roomDocsId=`** — planned

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `examDate`, `rooms` (`roomDocsId`, `invigilatorDocsIds`) — every paper on the date, across every exam

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exam-schedules
    ?date=2026-11-12    // the day
    &invigilatorDocsId=…   // optional
    &roomDocsId=…          // optional
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `date` | — | The day. Whether it is required is not set yet. |
| `invigilatorDocsId` | no | Only the papers this person is on duty for. |
| `roomDocsId` | no | Only the papers in this room. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

every paper on a date across every exam, filterable by
invigilator or room. The exam office's day view, and an invigilator's duty list. Served by
`school_invigilator_exam_date_idx` and `school_exam_schedule_room_idx`.

## Who may sit — candidates

**Nobody is on a hall register until they are a candidate.** The office adds the children who may
sit, then debars the ones who may not. Debarring is not a deletion: the child stays on the list,
marked `DEBARRED` with a reason, and every debarment and return is kept in `statusChanges` — so
"debarred for fees on the 10th, let back in on the 12th" is one read. Candidate writes run gates
1, 2 and 4, and **not** the results lock — who may sit is not a result.

<a id="e43"></a>
**[#43](#t43) · `POST /exams/{examId}/candidates`** — built 2026-10-22 — *add children who may sit*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `status` — not `COMPLETED` or `CANCELLED`; `startDate`, `endDate` — the day (or the window) the section is read on; `name` — for the answer
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name`, `sections` (`sectionNo`, `active`) — the section must exist and be in use
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `effectiveFrom`, `effectiveUntil`, `status` — who is in the section on the exam's first day, or during the exam for named children
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `studentDocsId` — who is already a candidate, and is skipped
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo` — for the answer
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *inserts*: one row per new child — `schoolId`, `academicYear`, `studentDocsId`, `examDocsId`, `studentAcademicRecordDocsId`, `status` = `ELIGIBLE`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examDocsId}/candidates
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
                        // REQUIRED, max 60
  "sectionNo": "A",     // REQUIRED, max 20
  "studentDocsIds": [   // optional, at most 200,
    "6a9e40a1b2c3d4e5f6071829"
  ]                     // each max 60 — left out,
                        // the whole section
}
</pre></td>
<td><pre>
201 Created — ExamCandidatesAddedResponse
   (200 OK when everyone already was a candidate)
Location: /schools/current/academic-years/{year}/exams/{examDocsId}/candidates

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "addedCount": 1,
  "alreadyCandidateCount": 0,
  "added": [
    {
      "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
      "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
      "fullName": "Arya Deshmukh",
      "admissionNo": "ADM-2024-0117",
      "studentAcademicRecordDocsId": "6a9e41a1b2c3d4e5f607182a",
      "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
      "className": "Class 7",
      "sectionNo": "A",
      "rollNo": "12",
      "status": "ELIGIBLE",
      "version": 0
    }
  ],
  "alreadyCandidates": [],
  "nextStep": "1 child(ren) may now sit. Debar anyone who may not — fees due, attendance short — before the first paper."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year, not `COMPLETED` or `CANCELLED`. |
| `classDocsId` | **yes** | Max 60. A class of the exam's year. |
| `sectionNo` | **yes** | Max 20. A section of that class, in use. Matched ignoring case. |
| `studentDocsIds` | no | At most 200, each max 60. Left out, the whole section is added — everyone in it on the exam's first day. Sent, only these children, each of whom must be in the section on some day of the exam. A child sent twice is added once. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 404 | `CLASS_NOT_FOUND` | There is no class of that id in this year. |
| 400 | `SECTION_NOT_IN_CLASS` | The class has no such section. |
| 409 | `SECTION_NOT_ACTIVE` | The section has been switched off. |
| 400 | `STUDENT_NOT_IN_SECTION` | A named child is not in the section during the exam. The message names them. |
| 400 | `VALIDATION_FAILED` | No `classDocsId` or `sectionNo`, or more than 200 children. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned.** `ExamCandidateController` (a third controller) / `ExamCandidateService.addCandidates`,
answering `ExamCandidatesAddedResponse` — `addedCount`, `alreadyCandidateCount`, and both lists with
name, admission number and roll number. **201** when anyone was added, **200** when everyone already
was. The section is matched ignoring case (`SECTION_NOT_IN_CLASS`, `SECTION_NOT_ACTIVE`, as #8). The
roster is `StudentAcademicRecordRepository.findInSectionOn` (first day) or `findInSectionBetween`
(the exam's window, for named children); a child with two records that day keeps the later one.
Measured on New Test, Class 1: section "a" added 2 (of five records, one starts in 2027 and one
transferred out), stored as "A"; again added 0; a section C child named for A was refused; one B
child sent twice was added once. The test candidates were removed.

```json
{ "classDocsId": "…", "sectionNo": "A" }
{ "classDocsId": "…", "sectionNo": "A", "studentDocsIds": ["…", "…"] }
```

- **No `studentDocsIds` → the whole section**: every child whose academic record places them in
  that class and section on the exam's first day — the same date rule as attendance (`effectiveFrom
  ≤ date`, `effectiveUntil` empty or later, not `CANCELLED`).
- **With `studentDocsIds` → only those children.** Each must be in that section on some day of the
  exam's window → `400 STUDENT_NOT_IN_SECTION`, naming them — so a child who joins mid-exam can be
  added.
- **Idempotent**: a child who is already a candidate is skipped and listed as `alreadyCandidates`,
  never an error, so re-running for a section picks up only the new joiners.
- Only `studentAcademicRecordDocsId` is copied from the record. **Class and section are not stored
  on the candidate** — they are always read from the academic record, so a child who moves section
  mid-exam sits the papers of the section they are in that day. `status` is `ELIGIBLE`.
- Refused on a `COMPLETED` or `CANCELLED` exam (`409 EXAM_NOT_EDITABLE`). Gates 1, 2, 4.
- Answers with the counts — added, already there — and the added rows.

<a id="e44"></a>
**[#44](#t44) · `GET /exams/{examId}/candidates?classDocsId=&sectionNo=&status=&debarmentReason=`** — built 2026-10-22

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `name`
- [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: `name` — and a `classDocsId` asked for must be this year's
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: every candidate of the exam — `studentDocsId`, `studentAcademicRecordDocsId`, `status`, `debarmentReason`, `debarmentNote`, `statusChanges`, `version`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo` — from the record each candidate was added with
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams/{examDocsId}/candidates
    ?classDocsId=6a9e91d4f5a6b7c8d9e0f1a2
                        // optional
    &sectionNo=a        // optional, ignoring case
    &status=DEBARRED    // optional — ELIGIBLE or DEBARRED
    &debarmentReason=FEES_DUE
                        // optional — FEES_DUE,
                        // ATTENDANCE_SHORTAGE,
                        // DISCIPLINARY,
                        // DOCUMENTS_PENDING or OTHER
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — ExamCandidateListResponse

{
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "total": 8,
  "eligibleCount": 7,
  "debarredCount": 1,
  "debarredByReason": {
    "FEES_DUE": 1, "ATTENDANCE_SHORTAGE": 0,
    "DISCIPLINARY": 0, "DOCUMENTS_PENDING": 0,
    "OTHER": 0
  },
  "listedCount": 1,
  "candidates": [
    {
      "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
      "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
      "fullName": "Arya Deshmukh",
      "admissionNo": "ADM-2024-0117",
      "studentAcademicRecordDocsId": "6a9e41a1b2c3d4e5f607182a",
      "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
      "className": "Class 7",
      "sectionNo": "A",
      "rollNo": "12",
      "status": "DEBARRED",
      "debarmentReason": "FEES_DUE",
      "debarmentNote": "Term 2 fee unpaid",
      "statusChanges": [
        { "status": "DEBARRED", "reason": "FEES_DUE",
          "note": "Term 2 fee unpaid",
          "changedAt": "2026-11-10T04:00:00Z",
          "changedByDocsId": "6a9e5f00a1b2c3d4e5f60718" }
      ],
      "version": 1
    }
  ]
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examDocsId}` (path) | **yes** | The exam's id. Must be an exam of this year. |
| `classDocsId` | no | Only children whose academic record is in this class. Must be a class of this year. |
| `sectionNo` | no | Only this section, ignoring case. Usually with `classDocsId`. |
| `status` | no | `ELIGIBLE` or `DEBARRED`. Does not change the totals. |
| `debarmentReason` | no | Only children debarred for this reason. Does not change the totals. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 404 | `CLASS_NOT_FOUND` | A `classDocsId` was sent and it is not a class of this year. |
| 400 | `VALIDATION_FAILED` | A `status` or `debarmentReason` that does not exist. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, not paged** (an exam's list is read whole, like the datesheet).
`ExamCandidateController.list` / `ExamCandidateService.listCandidates`, answering
`ExamCandidateListResponse`: `total`, `eligibleCount`, `debarredCount`, `debarredByReason` (every
reason, zeros too) and `listedCount`, then the rows. The class, section and roll come from the
record each candidate was **added with** (`studentAcademicRecordDocsId`), read with
`StudentAcademicRecordRepository.findBySchoolIdAndIdIn`. **The totals are for the class and section
asked for, before the status and reason filters**, so the fee defaulters' list still says "1 of 8".
A `classDocsId` not of this year is `404 CLASS_NOT_FOUND`; a bad status or reason is
`400 VALIDATION_FAILED`. Rows go by class name, section, roll (numbers by value, as attendance) and
name. `ExamCandidateResponse` grew `classDocsId`, `className`, `sectionNo`, `debarmentNote` and
`statusChanges`, so #43 answers with the same row. Measured with Class 1 A, B and C added: 8 in
order; `sectionNo=b` gave 2; the fee defaulters listed 1 of 8.

Planned: the list, in class, section and roll order
(class and section read from each child's academic record):
each child's name, admission number, roll, status, `debarmentReason` and `debarmentNote`, and
the history. Filters for a class, a section, a status and a reason —
`?status=DEBARRED&debarmentReason=FEES_DUE` is the fee defaulters. Totals: eligible, debarred, and
debarred by reason.

<a id="e45"></a>
**[#45](#t45) · `POST /exam-candidates/{candidateId}/debar`** — built 2026-10-22

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `version` — must match; `status` — must be `ELIGIBLE`; `examDocsId`
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `status` — not `COMPLETED` or `CANCELLED`
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *updates*: `status` = `DEBARRED`, `debarmentReason`, `debarmentNote`, and one `statusChanges` entry added (`status`, `reason`, `note`, `changedAt`, `changedByDocsId`)
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`students`](../../../models/student/Student.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class, section, roll, name and admission number for the answer row (`ExamCandidateServiceUtils.rowOf`)

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-candidates/{examCandidateDocsId}/debar
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "FEES_DUE",  // REQUIRED — FEES_DUE,
                         // ATTENDANCE_SHORTAGE,
                         // DISCIPLINARY,
                         // DOCUMENTS_PENDING or OTHER
  "note": "Term 2 fee unpaid",
                         // optional, max 500 —
                         // REQUIRED with OTHER
  "version": 0           // REQUIRED — the candidate's
}
</pre></td>
<td><pre>
200 OK — ExamCandidateResponse, the row #44 lists

{
  "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
  "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
  "fullName": "Arya Deshmukh",
  "admissionNo": "ADM-2024-0117",
  "studentAcademicRecordDocsId": "6a9e41a1b2c3d4e5f607182a",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "rollNo": "12",
  "status": "DEBARRED",
  "debarmentReason": "FEES_DUE",
  "debarmentNote": "Term 2 fee unpaid",
  "statusChanges": [
    { "status": "DEBARRED", "reason": "FEES_DUE",
      "note": "Term 2 fee unpaid",
      "changedAt": "2026-11-10T04:00:00Z",
      "changedByDocsId": "6a9e5f00a1b2c3d4e5f60718" }
  ],
  "version": 1
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examCandidateDocsId}` (path) | **yes** | The candidate's id. Must be a candidate of this year. |
| `reason` | **yes** | `FEES_DUE`, `ATTENDANCE_SHORTAGE`, `DISCIPLINARY`, `DOCUMENTS_PENDING` or `OTHER`. |
| `note` | no | Max 500. Trimmed; a blank note is no note. Required with `OTHER`. |
| `version` | **yes** | The candidate's version as last read, so nobody debars a child they have not seen. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 400 | `NOTE_REQUIRED` | The reason is `OTHER` and there is no note. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `CANDIDATE_NOT_FOUND` | There is no candidate of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The candidate changed since this version was read. |
| 409 | `CANDIDATE_ALREADY_DEBARRED` | The child is already debarred. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 400 | `INVALID_VALUE` | A reason that does not exist. The message lists the five. |
| 400 | `VALIDATION_FAILED` | No reason, no version, or a note over 500. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "FEES_DUE", "note": "Term 2 fee unpaid", "version": n }`

**Built as planned**, under `/academic-years/{year}` like the other examination paths.
`ExamCandidateController.debar` / `ExamCandidateService.debarCandidate`, answering the same
`ExamCandidateResponse` row #44 lists. The candidate is read with
`ExamCandidateRepository.findByIdAndSchoolIdAndAcademicYear` (`404 CANDIDATE_NOT_FOUND`); the note is
trimmed and a blank one is no note; the history entry's `changedByDocsId` is the signed-in staff, when
there is one. A bad reason is `400 INVALID_VALUE` (an enum in a body); a missing one
`400 VALIDATION_FAILED`. The hall register (#14) is what leaves a debarred child off — nothing is
removed here. Measured on a test candidate: Arya, Class 1 B — debarred for `FEES_DUE`, version 0 → 1;
again was `CANDIDATE_ALREADY_DEBARRED`; #44's fee defaulters listed her, 1 of 2.

Planned:

- `ELIGIBLE` → `DEBARRED`; `debarmentReason` and `debarmentNote` set; one `statusChanges` entry
  added (status, reason, note, when, who). Already debarred → `409 CANDIDATE_ALREADY_DEBARRED`.
- `reason` is required; **`note` is required with `OTHER`** → `400 NOTE_REQUIRED`.
- **Not retroactive.** A paper the child already has a hall entry for keeps it; the child leaves the
  register of every paper **not yet `COMPLETED`**. Debarring a child who is in the middle of a paper
  — has an entry on a paper still open — is allowed; that entry stays.
- Refused on a `COMPLETED` or `CANCELLED` exam.

<a id="e46"></a>
**[#46](#t46) · `POST /exam-candidates/{candidateId}/readmit`** — built 2026-10-22

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `version` — must match; `status` — must be `DEBARRED`; `examDocsId`
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: `status` — not `COMPLETED` or `CANCELLED`
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *updates*: `status` = `ELIGIBLE`, `debarmentReason` and `debarmentNote` cleared, and one `statusChanges` entry added (`status` = `ELIGIBLE`, `note`, `changedAt`, `changedByDocsId`)
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java), [`students`](../../../models/student/Student.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java) — *reads*: the class, section, roll, name and admission number for the answer row

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-candidates/{examCandidateDocsId}/readmit
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "note": "Fee paid",   // optional, max 500
  "version": 1          // REQUIRED — the candidate's
}
</pre></td>
<td><pre>
200 OK — ExamCandidateResponse, the row #44 lists

{
  "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
  "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
  "fullName": "Arya Deshmukh",
  "admissionNo": "ADM-2024-0117",
  "studentAcademicRecordDocsId": "6a9e41a1b2c3d4e5f607182a",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "rollNo": "12",
  "status": "ELIGIBLE",
  "statusChanges": [
    { "status": "DEBARRED", "reason": "FEES_DUE",
      "note": "Term 2 fee unpaid",
      "changedAt": "2026-11-10T04:00:00Z",
      "changedByDocsId": "6a9e5f00a1b2c3d4e5f60718" },
    { "status": "ELIGIBLE", "reason": null,
      "note": "Fee paid",
      "changedAt": "2026-11-12T02:45:00Z",
      "changedByDocsId": "6a9e5f00a1b2c3d4e5f60718" }
  ],
  "version": 2
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examCandidateDocsId}` (path) | **yes** | The candidate's id. Must be a candidate of this year. |
| `note` | no | Max 500. What the office writes — "Fee paid this morning". Goes into the history entry. |
| `version` | **yes** | The candidate's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `CANDIDATE_NOT_FOUND` | There is no candidate of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The candidate changed since this version was read. |
| 409 | `CANDIDATE_NOT_DEBARRED` | The child is not debarred. |
| 409 | `EXAM_NOT_EDITABLE` | The exam is `COMPLETED` or `CANCELLED`. |
| 400 | `VALIDATION_FAILED` | No version, or a note over 500. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "note": "Fee paid", "version": n }`.

**Built as planned.** `ExamCandidateController.readmit` / `ExamCandidateService.readmitCandidate`. The
note is optional; the history entry is `ELIGIBLE` with that note, and the cleared reason stays in the
earlier `DEBARRED` entry. The answer row is built by `ExamCandidateServiceUtils.rowOf`, shared with
#45. Measured on a test candidate: readmitting an eligible child was `CANDIDATE_NOT_DEBARRED`;
debarred (FEES_DUE) → let back in → debarred again (DISCIPLINARY), three history entries, version
0 → 3.

Planned:
`DEBARRED` → `ELIGIBLE`, the reason and note cleared, a `statusChanges` entry added. The child is
back on the register of every paper not yet `COMPLETED`. Not debarred → `409 CANDIDATE_NOT_DEBARRED`.
A paper completed while they were debarred stays missed, and their card shows it as "debarred" —
see [#29](#e29).

<a id="e47"></a>
**[#47](#t47) · `POST /exams/{examId}/candidates/debar`** — planned

- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `version`, `status`, `examDocsId` of each candidate sent
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *updates*: as #45, for each — `status` = `DEBARRED`, `debarmentReason`, `debarmentNote`, one `statusChanges` entry

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exams/{examId}/candidates/debar
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "candidates": [       // REQUIRED
    { "candidateDocsId": "…",
      "version": 2 },   // each its own version
    …
  ],
  "reason": "FEES_DUE", // REQUIRED — as #45
  "note": "…"           // as #45 — REQUIRED with OTHER
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examId}` (path) | **yes** | The exam's id. |
| `candidates` | **yes** | The children to debar. All or nothing. |
| `candidates[].candidateDocsId` | **yes** | A candidate of this exam. |
| `candidates[].version` | **yes** | That candidate's own version. |
| `reason` | **yes** | As #45. |
| `note` | as #45 | Required with `OTHER`. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 409 | `CANDIDATE_ALREADY_DEBARRED` | One of them is already debarred. The whole request is refused and names them. |
| 409 | `CONCURRENT_MODIFICATION` | One of them changed since its version was read. The whole request is refused. |
| — | not named yet | One of them is not in this exam. The whole request is refused and names them. |
| 400 | `NOTE_REQUIRED` | The reason is `OTHER` and there is no note. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "candidates": [{ "candidateDocsId": "…", "version": 2 }, …], "reason": "FEES_DUE", "note": "…" }`.
#45 for many children, **all or nothing**: one already debarred, stale or not in this exam refuses
the whole request and names them. Each candidate carries its own `version`.

<a id="e48"></a>
**[~~#48~~](#t48) · `PUT /exam-candidates/{candidateId}/subjects`** — not being built — removed 2026-10-16

- None. It is not being built.

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Not being built.
</pre></td>
<td><pre>
Not being built.
</pre></td>
</tr>
</table>

No fields and no refusals — the route does not exist.

It set which subjects a candidate sits. Each hall entry
points at a paper, and the paper already holds the subject, so the candidate does not repeat it.

<a id="e49"></a>
**[#49](#t49) · `DELETE /exam-candidates/{candidateId}`** — planned

- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `version`, `papers` — refused once the child has any hall entry
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: whether the child has any mark in the exam
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *deletes*: the row

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
DELETE /schools/current/academic-years/{year}/exam-candidates/{candidateId}
    ?version=3          // REQUIRED — the candidate's
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

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
| `{candidateId}` (path) | **yes** | The candidate's id. |
| `version` (query) | **yes** | The candidate's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `CANDIDATE_NOT_FOUND` | There is no candidate of that id in this year. (The code #45 and #46 use; the code list at the end of this file says `EXAM_CANDIDATE_NOT_FOUND`.) |
| 409 | `CANDIDATE_HAS_RECORDS` | The child already has a hall entry or a mark in the exam. Debar them instead. |
| 409 | `CONCURRENT_MODIFICATION` | The candidate changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`?version=n`. For a child added by
mistake. **Refused once the child has any hall entry or mark in the exam** (`409
CANDIDATE_HAS_RECORDS`) — debar them instead. `204`.

## In the hall

<a id="e14"></a>
**[#14](#t14) · `GET /exam-schedules/{scheduleId}/attendance`** — built 2026-10-22

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the paper — class, section, subject, date, times, `rooms`, `status`, `version`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo`, `effectiveFrom`, `effectiveUntil`, `status` — who is in the section on the paper's date (`ExamAttendanceServiceUtils.rosterOf`)
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `status`, `papers` — the eligible candidates among them, plus anyone already marked at this paper
- [`exams`](../../../models/academics/examination/Exam.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`rooms`](../../../models/room/Room.java) — *reads*: `name` — the exam, class, subject and room names (`registerOf`)
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exam-schedules/{examScheduleDocsId}/attendance
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
200 OK — ExamHallRegisterResponse

{
  "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "subjectCode": "MATH",
  "subjectName": "Mathematics",
  "componentName": "Theory",
  "examDate": "2026-11-12",
  "startTime": "09:00",
  "endTime": "12:00",
  "status": "PUBLISHED",
  "rooms": [
    { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
      "roomName": "Main Hall" }
  ],
  "counts": {
    "expected": 2, "marked": 0, "notMarked": 2,
    "byStatus": { "PRESENT": 0, "ABSENT": 0,
                  "UNFAIR_MEANS": 0, "EXEMPT": 0 },
    "debarred": 1, "notCandidates": 0
  },
  "students": [
    { "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
      "fullName": "Arya Deshmukh",
      "admissionNo": "ADM-2024-0117",
      "rollNo": "12",
      "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
      "candidateStatus": "ELIGIBLE",
      "inSection": true,
      "entry": null },
    { "studentDocsId": "6a9e40a1b2c3d4e5f607182b",
      "fullName": "Ayan Shaikh",
      …,
      "entry": null }
  ],
  "version": 1
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examScheduleDocsId}` (path) | **yes** | The paper's id. Must be a paper of this year. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned.** A fourth controller, `ExamAttendanceController`, with
`ExamAttendanceService.getHallRegister`, answering `ExamHallRegisterResponse`: the paper, its rooms,
`counts` (`expected`, `marked`, `notMarked`, `byStatus`, `debarred`, `notCandidates`), the rows, and
the paper's `version` for #15. **Who the hall is about** — the section's records on the date, one per
child, and the candidates among them plus anyone marked here — is
`ExamAttendanceServiceUtils.rosterOf`, now shared with #12 (#12 used to work it out itself). Each row
carries `candidateStatus` (DEBARRED for a child debarred after sitting) and `inSection` (false for one
who has left). `late` and `leftEarly` are worked out against the paper's times in the school's
`defaultTimeZone` (UTC when unset). Measured on New Test, English A (09:00–12:00 IST): one expected
and one not a candidate; a test candidate added, debarred (counted, not named), then given a raw
entry 09:15–11:30 — listed again as DEBARRED, present, late and left early. #12's counts matched.

Planned: **the hall register**: every child
expected, in roll order, each with their hall entry or "not marked". **Expected means a child in this
paper's class and section on the paper's date — the roster, from academic records — who is an
`ELIGIBLE` candidate of the exam** — plus anyone who already has an entry on this paper (debarred
after they sat it).
**A debarred child's name is not listed**; the register carries only a count, "2 debarred", so the
invigilator knows a gap is deliberate. The same shape as
[attendance #6](../attendance/README.md#e6). **Entries are not created ahead of time** — an unmarked
child is shown as unmarked, and [#16](#e16) refuses while there is one
([open item 4](#4-hall-entries-ahead-of-time-or-not)).

<a id="e15"></a>
**[#15](#t15) · `PUT /exam-schedules/{scheduleId}/attendance`** — built 2026-10-22 — *mark the hall*

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the paper — class, section, subject, date, times, `rooms`, `status`, `version`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo`, `effectiveFrom`, `effectiveUntil`, `status` — who is in the section on the paper's date (`ExamAttendanceServiceUtils.rosterOf`)
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `status`, `papers` — the eligible candidates among them, plus anyone already marked at this paper
- [`exams`](../../../models/academics/examination/Exam.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`rooms`](../../../models/room/Room.java) — *reads*: `name` — the exam, class, subject and room names (`registerOf`)
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.answerCopyNo`, `papers.additionalAnswerCopyNos` of every candidate of the exam — a booklet is used once (`findHoldingBooklets`)
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *updates*: `papers` — this paper's entry is added or replaced: `id` (kept), `examScheduleDocsId`, `attendanceStatus`, `answerCopyNo`, `additionalAnswerCopyNos`, `roomDocsId`, `seatNo`, `reportedAt`, `submittedAt`, `remarks`, `evidenceDocumentDocsIds` (kept), `markedAt`, `markedByDocsId`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: saved so its `version` moves on
- [`exams`](../../../models/academics/examination/Exam.java) — *updates*: `status` `PUBLISHED` → `IN_PROGRESS`, on the first save

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}/exam-schedules/{examScheduleDocsId}/attendance
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "marks": [            // REQUIRED, 1 to 300,
                        // each child once
    { "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
                        // REQUIRED, max 60
      "attendanceStatus": "PRESENT",
                        // REQUIRED — PRESENT, ABSENT,
                        // UNFAIR_MEANS or EXEMPT
      "answerCopyNo": "MATH-0041",
                        // max 40 — REQUIRED with
                        // PRESENT and UNFAIR_MEANS,
                        // not allowed otherwise
      "additionalAnswerCopyNos": ["MATH-0102"],
                        // optional, at most 20,
                        // each max 40
      "roomDocsId": "6a9fa0b1c2d3e4f506172839",
                        // optional, max 60 — REQUIRED
                        // for a child who sat, when
                        // the paper has several rooms
      "seatNo": "H2-14",       // optional, max 40
      "reportedAt": "2026-11-12T03:35:00Z",
                        // optional
      "submittedAt": "2026-11-12T06:30:00Z",
                        // optional, not before
                        // reportedAt
      "remarks": null          // optional, max 500
    },
    { "studentDocsId": "6a9e40a1b2c3d4e5f607182b",
      "attendanceStatus": "ABSENT",
      "remarks": "Fever" }
  ],
  "version": 1          // REQUIRED — the paper's
}
</pre></td>
<td><pre>
200 OK — ExamHallRegisterResponse, the
register #14 gives, as it now stands

{
  "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "subjectCode": "MATH",
  "subjectName": "Mathematics",
  "componentName": "Theory",
  "examDate": "2026-11-12",
  "startTime": "09:00",
  "endTime": "12:00",
  "status": "PUBLISHED",
  "rooms": [
    { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
      "roomName": "Main Hall" }
  ],
  "counts": {
    "expected": 2, "marked": 2, "notMarked": 0,
    "byStatus": { "PRESENT": 1, "ABSENT": 1,
                  "UNFAIR_MEANS": 0, "EXEMPT": 0 },
    "debarred": 1, "notCandidates": 0
  },
  "students": [
    { "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
      "fullName": "Arya Deshmukh",
      "admissionNo": "ADM-2024-0117",
      "rollNo": "12",
      "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
      "candidateStatus": "ELIGIBLE",
      "inSection": true,
      "entry": {
        "id": "6a9f40f5a7e4d0a6f8b13d44",
        "attendanceStatus": "PRESENT",
        "answerCopyNo": "MATH-0041",
        "additionalAnswerCopyNos": ["MATH-0102"],
        "roomDocsId": "6a9fa0b1c2d3e4f506172839",
        "roomName": "Main Hall",
        "seatNo": "H2-14",
        "reportedAt": "2026-11-12T03:35:00Z",
        "submittedAt": "2026-11-12T06:30:00Z",
        "late": true,
        "leftEarly": false,
        "remarks": null,
        "markedAt": "2026-11-12T06:40:00Z",
        "markedByDocsId": "6a9e5f00a1b2c3d4e5f60718" } },
    { "studentDocsId": "6a9e40a1b2c3d4e5f607182b",
      "fullName": "Ayan Shaikh",
      …,
      "entry": { "attendanceStatus": "ABSENT",
                 "remarks": "Fever", … } }
  ],
  "version": 2
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examScheduleDocsId}` (path) | **yes** | The paper's id. Must be a paper of this year, `PUBLISHED`, and its day must have come. |
| `marks` | **yes** | 1 to 300 entries. Each child once. |
| `marks[].studentDocsId` | **yes** | Max 60. A child #14 lists. A debarred child may only have an entry already here corrected. |
| `marks[].attendanceStatus` | **yes** | `PRESENT`, `ABSENT`, `UNFAIR_MEANS` or `EXEMPT`. |
| `marks[].answerCopyNo` | with `PRESENT` / `UNFAIR_MEANS` | Max 40. Not allowed with `ABSENT` or `EXEMPT`. Used once in the whole exam. |
| `marks[].additionalAnswerCopyNos` | no | At most 20, each max 40. Supplementary booklets, under the same rules. |
| `marks[].roomDocsId` | for a child who sat, when the paper has several rooms | Max 60. One of the paper's rooms. With one room, that room is filled in. |
| `marks[].seatNo` | no | Max 40. Seat or desk in that room. |
| `marks[].reportedAt` | no | When the child came in. `late` is worked out from it. |
| `marks[].submittedAt` | no | When they handed in the booklet. Not before `reportedAt`. `leftEarly` is worked out from it. |
| `marks[].remarks` | no | Max 500. What the invigilator writes. |
| `version` | **yes** | The paper's version, as #14 last gave it. It moves on with every save. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 400 | `DUPLICATE_STUDENT_IN_REQUEST` | A child is in `marks` twice. |
| 400 | `ANSWER_COPY_REQUIRED` | `PRESENT` or `UNFAIR_MEANS` with no booklet. |
| 400 | `ANSWER_COPY_NOT_ALLOWED` | `ABSENT` or `EXEMPT` with a booklet. |
| 400 | `INVALID_TIME_RANGE` | `submittedAt` is before `reportedAt`. |
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The paper, or a candidate, changed since it was read. |
| 409 | `PAPER_NOT_OPEN` | The paper is not `PUBLISHED`. |
| 409 | `PAPER_NOT_YET_SAT` | The paper's day has not come, in the school's time zone. |
| 400 | `ROOM_NOT_IN_PAPER` | A room that is not one of the paper's. |
| 400 | `ROOM_REQUIRED` | The paper has several rooms, and a child who sat has no room. |
| 400 | `STUDENT_NOT_IN_PAPER` | A child #14 does not list. The message names them. |
| 409 | `STUDENT_DEBARRED` | A debarred child with no entry here. The message names them and why. |
| 409 | `ANSWER_COPY_TAKEN` | A booklet already given in this exam, or given twice in this request. The message names it. |
| 400 | `VALIDATION_FAILED` | No marks, no version, or a field too long. |
| 400 | `INVALID_VALUE` | An `attendanceStatus` that does not exist. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, with these details.** `ExamAttendanceController.mark` /
`ExamAttendanceService.markHall` (`@Transactional`), answering the register #14 gives — built by
`ExamAttendanceServiceUtils.registerOf`, which takes the roster from `rosterOf` rather than calling
it, so the utils stay flat. **Re-sending a child replaces the whole entry** (its id and evidence are
kept). **A debarred child who already has an entry here can have it corrected**; only a new entry is
`STUDENT_DEBARRED` — otherwise #14 would list a row nobody could fix. **Room:** a paper with one room
fills it in for a child who sat; several rooms make it `400 ROOM_REQUIRED`. Added refusals:
`DUPLICATE_STUDENT_IN_REQUEST`, `INVALID_TIME_RANGE` (out before in). Booklets are looked up with
`ExamCandidateRepository.findHoldingBooklets` (main or supplementary, any paper). The day is
`Dates.todayIn` the school's zone. The paper is saved to move its version on; the exam goes
`PUBLISHED` → `IN_PROGRESS` on the first save. Measured on a test exam with papers moved to the test
day: draft, future, booklet, duplicate, time, version, room, not-on-paper, bad-status and debarred
refusals; Ranu present (room filled in, late and early) and Ayan absent, version 1 → 2, the exam
`IN_PROGRESS`; a correction with the same booklet allowed; the booklet refused for another child and
on another paper; a debarred Ranu refused a new entry but allowed to correct hers. All removed after.

Planned:

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

- **Only the children #14 lists.** A child who is not a candidate, or is in another section → `400
  STUDENT_NOT_IN_PAPER`; **a debarred candidate → `409
  STUDENT_DEBARRED`**, naming them and the reason. Nothing is saved.
- **`PRESENT` and `UNFAIR_MEANS` need `answerCopyNo`; `ABSENT` and `EXEMPT` must not have one** → `400
  ANSWER_COPY_REQUIRED` / `ANSWER_COPY_NOT_ALLOWED`.
- **A booklet is used once in the whole exam** — `answerCopyNo` against every candidate's
  `papers.answerCopyNo` *and* `papers.additionalAnswerCopyNos`, the same for each supplementary
  number, and inside the request → `409 ANSWER_COPY_TAKEN`, naming the booklet and the child who has
  it. The unique index on `papers.answerCopyNo` stops two children sharing one; one child holding the
  same number on two papers, and every supplementary number, only the service can check.
- **`roomDocsId` on each mark** — which of the paper's rooms the child sat in. It must be one of
  the paper's `rooms` → `400 ROOM_NOT_IN_PAPER`.
- **Only a `PUBLISHED` paper on or after its date** takes hall marks → `409 PAPER_NOT_OPEN`,
  `409 PAPER_NOT_YET_SAT`. The first save moves the exam to `IN_PROGRESS`.
- **Each mark writes the child's candidate**: the `papers` entry for this paper is added or
  replaced. `markedByDocsId` and `markedAt` are the server's ([open item 5](#5-markedbydocsid-is-required)).
  One request is one transaction over every candidate it touches; a candidate debarred or changed
  in between fails the whole request with `409 CONCURRENT_MODIFICATION`.
- Late and early are **not** stored — they are derived from `reportedAt` and `submittedAt` against
  the paper's times, and #14 shows them.

<a id="e16"></a>
**[#16](#t16) · `POST /exam-schedules/{scheduleId}/complete`** — built 2026-10-22

- [`academic_years`](../../../models/core/AcademicYear.java) — *reads*: `name` — the year in the path must exist
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the paper — class, section, subject, date, times, `rooms`, `status`, `version`
- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo`, `effectiveFrom`, `effectiveUntil`, `status` — who is in the section on the paper's date (`ExamAttendanceServiceUtils.rosterOf`)
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `status`, `papers` — the eligible candidates among them, plus anyone already marked at this paper
- [`exams`](../../../models/academics/examination/Exam.java), [`school_classes`](../../../models/academics/structure/SchoolClass.java), [`rooms`](../../../models/room/Room.java) — *reads*: `name` — the exam, class, subject and room names (`registerOf`)
- [`students`](../../../models/student/Student.java) — *reads*: `fullName`, `admissionNo`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: `status` `PUBLISHED` → `COMPLETED`; `version` moves on

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{examScheduleDocsId}/complete
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 2          // REQUIRED — the paper's, as
                        // #14 last gave it
}
</pre></td>
<td><pre>
200 OK — ExamHallRegisterResponse, the
register #14 gives, as it now stands

{
  "examScheduleDocsId": "6a9f20d3e5c2b8e4d6f91b22",
  "examDocsId": "6a9f10c2e4b1a7d3c5e80a11",
  "examName": "Half Yearly Examination, Term 1",
  "classDocsId": "6a9e91d4f5a6b7c8d9e0f1a2",
  "className": "Class 7",
  "sectionNo": "A",
  "subjectCode": "MATH",
  "subjectName": "Mathematics",
  "componentName": "Theory",
  "examDate": "2026-11-12",
  "startTime": "09:00",
  "endTime": "12:00",
  "status": "COMPLETED",
  "rooms": [
    { "roomDocsId": "6a9fa0b1c2d3e4f506172839",
      "roomName": "Main Hall" }
  ],
  "counts": {
    "expected": 2, "marked": 2, "notMarked": 0,
    "byStatus": { "PRESENT": 1, "ABSENT": 1,
                  "UNFAIR_MEANS": 0, "EXEMPT": 0 },
    "debarred": 1, "notCandidates": 0
  },
  "students": [
    { "studentDocsId": "6a9e40a1b2c3d4e5f6071829",
      "fullName": "Arya Deshmukh",
      "admissionNo": "ADM-2024-0117",
      "rollNo": "12",
      "examCandidateDocsId": "6a9f30e4f6d3c9f5e7a02c33",
      "candidateStatus": "ELIGIBLE",
      "inSection": true,
      "entry": { … } },
    { "studentDocsId": "6a9e40a1b2c3d4e5f607182b",
      "fullName": "Ayan Shaikh",
      …,
      "entry": { … } }
  ],
  "version": 3
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examScheduleDocsId}` (path) | **yes** | The paper's id. Must be a paper of this year. |
| `version` | **yes** | The paper's version, as #14 last gave it. A register saved since is refused, so nobody closes a hall they have not seen. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The register was saved since this version was read. |
| 409 | `PAPER_ALREADY_COMPLETED` | The paper is already completed. |
| 409 | `PAPER_NOT_OPEN` | The paper is `DRAFT` or `CANCELLED`. |
| 409 | `PAPER_NOT_YET_SAT` | The paper's day has not come, in the school's time zone. |
| 409 | `UNMARKED_STUDENTS` | Someone on the register has no hall mark. The message gives the count. |
| 400 | `VALIDATION_FAILED` | No version. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**Built as planned, without `NOT_ENOUGH_SEATS`.** That check was written before a room's
`seatCount` became optional and counted for the whole sitting (2026-10-21/22): for one paper there is
often no number, and when there is it is the sitting's, not this section's. Room capacity stays #8's
check. Added: `PAPER_ALREADY_COMPLETED`, `PAPER_NOT_OPEN` (DRAFT or CANCELLED) and `PAPER_NOT_YET_SAT`.
`ExamScheduleController.complete` / `ExamScheduleService.completePaper` (moved there from the hall code on 2026-10-22 — closing a paper is the paper's own action), counting the unmarked with
the same `rosterOf` + `registerOf` #14 uses, and answering with the register. Measured on a test exam:
draft, future, no-version, stale, "2 of 2" and "1 of 2" unmarked refused; with Ayan debarred and no
entry it completed (version 2 → 3); a second paper with Ranu `EXEMPT` completed; completing again and
marking a completed paper were refused. All removed after.

Planned: `{ "version": n }`. Refused while
any expected child — eligible candidate — is unmarked (`409 UNMARKED_STUDENTS`, with the count);
the paper's rooms must seat every eligible candidate (`409 NOT_ENOUGH_SEATS`, with both numbers);
**debarred children do not hold it up**, and `EXEMPT` counts as marked. `PUBLISHED` → `COMPLETED`.
**Hall attendance is closed from here** — the model README's rule; there is no per-entry lock.

<a id="e17"></a>
**[#17](#t17) · `POST /exam-schedules/{scheduleId}/reopen`** — planned

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `version`, `status` — must be `COMPLETED`
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — refused once the paper's marks are `LOCKED`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *updates*: `status` `COMPLETED` → `PUBLISHED`
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — the hall entries in `papers` become open to correction again, through #15

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/reopen
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "…",        // REQUIRED — max length not set yet
  "version": 3          // REQUIRED — the paper's
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `reason` | **yes** | Why the register is re-opened. Where it is stored is open item 6. |
| `version` | **yes** | The paper's version as last read. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `MARKS_ALREADY_LOCKED` | The paper's marks are locked. Unlock them first. |
| 409 | `CONCURRENT_MODIFICATION` | The paper changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "…", "version": n }`.
`COMPLETED` → `PUBLISHED`, so the hall entries can be corrected. **Refused once marks are locked**
(`409 MARKS_ALREADY_LOCKED`) — unlock them first, so the two can never disagree.

## Marks

<a id="e18"></a>
**[#18](#t18) · `GET /exam-schedules/{scheduleId}/marks`** — built 2026-10-22

**Built as planned, in a fifth controller.** `StudentMarkController.markSheet` /
`StudentMarkService.getMarkSheet`, answering `ExamMarkSheetResponse`. **Who is on it** comes from
`ExamAttendanceServiceUtils.rosterOf` — the same roster as #14 — kept to the children with an entry
on this paper; marks are read with `StudentMarkRepository.findBySchoolIdAndExamScheduleDocsId`.
`effectiveMarks` is `obtainedMarks + graceMarks`, worked out on the read. **The grade shown is the
stored `gradeCode`** — #19 will set it on entry — not re-resolved here. **`gradingScheme`** follows
the model README's order, the class subject's (a row for this section before a class-wide one), else
the exam's, with `from: SUBJECT | EXAM`. Measured on a test exam out of 10 with the CBSE scheme:
empty before the hall; Ranu present "not entered" and Ayan absent and unmarkable; then, with a test
mark of 7.5 + 0.5 grace, effective 8.0, B1, DRAFT. All removed after.


- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers` (`examScheduleDocsId`, `attendanceStatus`) — every child with a hall entry on the paper
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `obtainedMarks`, `graceMarks`, `gradeCode`, `status`
- [`students`](../../../models/student/Student.java) — *reads*: the child's name

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- every child with a hall entry, in roll order
- each with their hall status, obtainedMarks,
  graceMarks, the effective mark, the grade it
  resolves to, and the row's status
- children not PRESENT listed with their hall
  status, and cannot be marked
- counts: entered, not entered, by status
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**the mark sheet**: every child with a
hall entry, in roll order — their hall status, `obtainedMarks`, `graceMarks`, the effective mark,
the grade it resolves to, and the row's status. Children not `PRESENT` (absent, unfair means,
exempt) are listed with their hall status and cannot be marked. Counts: entered, not entered, by status.

<a id="e19"></a>
**[#19](#t19) · `PUT /exam-schedules/{scheduleId}/marks`** — planned — *enter marks*

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `status` — must be `COMPLETED`; `maximumMarks`; `version` — required and moved on
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.attendanceStatus` — only a child `PRESENT` at this paper can be marked
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradeBands` — a `gradeCode` must be a band of the resolved scheme
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *inserts / updates*: `obtainedMarks` or `gradeCode`; `examDocsId`, `examScheduleDocsId`, `studentAcademicRecordDocsId`, `enteredByDocsId`, `enteredAt` set by the server; only `DRAFT` rows

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "marks": [            // one, several or all
    { "studentDocsId": "…",
      "obtainedMarks": 64.5 },
                        // 0 to maximumMarks
    { "studentDocsId": "…",
      "gradeCode": "A1" }
                        // marks or gradeCode,
                        // not both
  ],
  "version": 6          // REQUIRED — the paper's
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. The paper must be `COMPLETED`. |
| `marks` | **yes** | One, several or all children. Re-sending corrects. |
| `marks[].studentDocsId` | **yes** | A child whose hall entry is `PRESENT`. |
| `marks[].obtainedMarks` | one of the two | 0 to `maximumMarks`. |
| `marks[].gradeCode` | one of the two | A band of the resolved grading scheme. Not with `obtainedMarks`. |
| `version` | **yes** | The paper's version. It moves on with every save. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `PAPER_NOT_COMPLETED` | The paper is not `COMPLETED` — hall attendance comes first. |
| 400 | `STUDENT_NOT_PRESENT` | A child whose hall entry is not `PRESENT`. The message names them and their hall status. |
| 400 | `MARKS_OUT_OF_RANGE` | A mark below 0 or above `maximumMarks`. The message names the child. |
| 400 | `GRADE_NOT_IN_SCHEME` | A `gradeCode` that is not a band of the resolved scheme. |
| 409 | `MARKS_NOT_EDITABLE` | The paper's marks are submitted or locked. |
| 409 | `CONCURRENT_MODIFICATION` | The paper changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

```json
{ "marks": [
    { "studentDocsId": "…", "obtainedMarks": 64.5 },
    { "studentDocsId": "…", "gradeCode": "A1" } ],
  "version": 6 }
```

- **The paper must be `COMPLETED`** — hall attendance first, the model README's ordering → `409
  PAPER_NOT_COMPLETED`.
- **Only a child whose hall entry is `PRESENT` can be marked** — anyone else → `400
  STUDENT_NOT_PRESENT`, naming them and their hall status. Nothing about participation is stored on
  the mark (section 5 of the model README): absent, unfair means and exempt stay on the candidate.
- `0 ≤ obtainedMarks ≤ maximumMarks` → `400 MARKS_OUT_OF_RANGE`, naming the child. **Marks or
  `gradeCode`, not both**; a grade must be a band of the resolved scheme → `400 GRADE_NOT_IN_SCHEME`.
- **Only `DRAFT` rows** take marks; a submitted or locked paper → `409 MARKS_NOT_EDITABLE`.
- `examDocsId`, `studentAcademicRecordDocsId`, `enteredByDocsId` and
  `enteredAt` are the server's.
- The paper's `version`, required and moved on — the decision already taken for attendance #2.
- **Results lock** runs.

<a id="e20"></a>
**[#20](#t20) · `GET /exam-schedules/{scheduleId}/answer-copies`** — planned

- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.answerCopyNo`, `papers.additionalAnswerCopyNos` — every booklet of the paper
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `obtainedMarks` — the mark entered for each booklet, or none

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/answer-copies
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- every booklet number of the paper, main and
  supplementary
- each with its entered mark, or none
- no child's name, id or roll number
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

every booklet number of a paper
(main and supplementary) with its entered mark or none. **No child's name, id or roll number.** What
an evaluator is handed.

<a id="e21"></a>
**[#21](#t21) · `PUT /exam-schedules/{scheduleId}/marks/by-copy`** — planned

- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.answerCopyNo`, `papers.additionalAnswerCopyNos` — the booklet is turned into the child
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *inserts / updates*: the same row #19 would write. The booklet number is never stored on the mark

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks/by-copy
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "marks": [
    { "answerCopyNo": "MATH-0041",
      "obtainedMarks": 64.5 }
  ],
  "version": 6          // REQUIRED — the paper's
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `marks[].answerCopyNo` | **yes** | A main or supplementary booklet number of this paper. |
| `marks[].obtainedMarks` | as #19 | Every other rule is #19's. |
| `version` | **yes** | The paper's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ANSWER_COPY_NOT_FOUND` | No child holds that booklet. |
| — | every #19 refusal | Every other rule is #19's. |

`{ "marks": [{ "answerCopyNo": "MATH-0041", "obtainedMarks": 64.5 }], "version": n }`. The server
resolves the booklet to the child through the candidates' `papers` and writes the same row #19 would — **the
copy number never lands on `StudentMark`** (model README section 6). A supplementary booklet number
resolves to its child too. Unknown booklet → `404 ANSWER_COPY_NOT_FOUND`. Every other rule is #19's.

<a id="e22"></a>
**[#22](#t22) · `POST /exam-schedules/{scheduleId}/marks/submit`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — every child who sat must have a mark
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *updates*: `status` `DRAFT` → `SUBMITTED`, every row of the paper

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks/submit
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 7          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `version` | **yes** | The paper's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `UNENTERED_MARKS` | A child who sat has no mark. The message gives the count. |
| 409 | `CONCURRENT_MODIFICATION` | It changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "version": n }`. Every
`DRAFT` row → `SUBMITTED`. **Refused while any child who sat has no mark** (`409 UNENTERED_MARKS`,
with the count). **No placeholder rows** are created for children who did not sit — absent, unfair
means, exempt and debarred are all read from the candidate when a card or the tabulation needs
them. **Results lock** runs.

<a id="e23"></a>
**[#23](#t23) · `POST /exam-schedules/{scheduleId}/marks/lock`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — no row may still be `DRAFT`
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *updates*: `status` `SUBMITTED` → `LOCKED`, `lockedAt`, `lockedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks/lock
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 8          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `version` | **yes** | The paper's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `MARKS_NOT_SUBMITTED` | A row is still `DRAFT`. |
| 409 | `CONCURRENT_MODIFICATION` | It changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "version": n }`. Every
`SUBMITTED` row → `LOCKED`, `lockedAt`, `lockedByDocsId`. Refused while any row is still `DRAFT`
(`409 MARKS_NOT_SUBMITTED`). **Results lock** runs.

<a id="e24"></a>
**[#24](#t24) · `POST /exam-schedules/{scheduleId}/marks/unlock`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status`
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: `status` — refused once a `PUBLISHED` card uses these marks
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *updates*: `status` `LOCKED` → `SUBMITTED`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/exam-schedules/{scheduleId}/marks/unlock
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "…",        // REQUIRED — max length not set yet
  "version": 9          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{scheduleId}` (path) | **yes** | The paper's id. |
| `reason` | **yes** | Why — a revaluation. Where it is stored is open item 6. |
| `version` | **yes** | The paper's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_SCHEDULE_NOT_FOUND` | There is no paper of that id in this year. |
| 409 | `USED_BY_PUBLISHED_CARD` | A published card uses these marks. Revoke the card first. |
| 409 | `CONCURRENT_MODIFICATION` | It changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "…", "version":
n }`. `LOCKED` → `SUBMITTED` — not back to `DRAFT`, so a revaluation goes through submit and lock
again. **Refused once a published card uses these marks** (`409 USED_BY_PUBLISHED_CARD`) — revoke
the card first ([#35](#e35)). **Results lock** runs: a frozen term cannot be unlocked here.

<a id="e25"></a>
**[#25](#t25) · `PATCH /student-marks/{markId}/grace`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `obtainedMarks`, `status`, `version` — `obtainedMarks + graceMarks` must not pass the paper's maximum
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *updates*: `graceMarks`, `graceReason`. Never `obtainedMarks`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}/student-marks/{markId}/grace
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "graceMarks": 2,      // obtainedMarks + graceMarks
                        // at most maximumMarks
  "graceReason": "…",   // REQUIRED with any non-zero
                        // grace
  "version": 2          // REQUIRED — the row's own
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{markId}` (path) | **yes** | The mark's id. A `SUBMITTED` or `LOCKED` row. |
| `graceMarks` | **yes** | `obtainedMarks + graceMarks ≤ maximumMarks`. |
| `graceReason` | with any non-zero grace | Why the grace was given. |
| `version` | **yes** | The row's own version, because grace is one child at a time. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `STUDENT_MARK_NOT_FOUND` | There is no mark of that id in this year. |
| — | not named yet | Non-zero grace with no `graceReason`, or `obtainedMarks + graceMarks` above `maximumMarks`. |
| 409 | `USED_BY_PUBLISHED_CARD` | A published card uses the mark. |
| 409 | `CONCURRENT_MODIFICATION` | The row changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "graceMarks": 2, "graceReason": "…",
"version": n }`. **`obtainedMarks` is never touched** — the evaluator's award stays as awarded.
`graceReason` is required with any non-zero grace; `obtainedMarks + graceMarks ≤ maximumMarks`.
Allowed on `SUBMITTED` and `LOCKED` rows ([open item 8](#8-grace-marks)), refused once a published card
uses the mark. **Results lock** runs. The row's own `version`, because grace is one child at a time.

## Results

<a id="e26"></a>
**[#26](#t26) · `GET /exams/{examId}/mark-entry`** — planned

- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: `classDocsId`, `sectionNo`, `subjectCode`, `status` — one row per paper
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — marks not entered, draft, submitted, locked

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams/{examId}/mark-entry
    ?pending=true       // optional — only the papers
                        // still holding something up
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- one row per paper: class, section, subject,
  paper status, marks not entered / draft /
  submitted / locked
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examId}` (path) | **yes** | The exam's id. |
| `pending` | no | `true` lists only the papers still holding something up. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

one row per paper: class, section, subject,
paper status, marks not entered / draft / submitted / locked. Filter `?pending=true` for the papers
still holding something up. **The exam office's "who hasn't entered marks"**, the twin of
[attendance #8](../attendance/README.md#e8).

<a id="e27"></a>
**[#27](#t27) · `GET /exams/{examId}/results/{classDocsId}/{sectionNo}`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `obtainedMarks`, `graceMarks`, `status` — marks in any status
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the section's papers — one column each
- [`students`](../../../models/student/Student.java) — *reads*: the children's names
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradeBands` — the grade
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.attendanceStatus`, `status` — `AB` / `EX` / `WH` / `DB` where there is no mark (the plan text says this; the table does not list it)

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/exams/{examId}/results/{classDocsId}/{sectionNo}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- one row per child, one column per paper
  (effective marks, or AB / EX / WH / DB)
- total, percentage, grade, pass or fail
- per column, whether it is final
- no rank
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{examId}` (path) | **yes** | The exam's id. |
| `{classDocsId}` (path) | **yes** | The class. |
| `{sectionNo}` (path) | **yes** | The section. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `EXAM_NOT_FOUND` | There is no exam of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

**the tabulation
sheet**: one row per child, one column per paper (effective marks, or `AB` / `EX` / `WH` / `DB` read from the candidate), total,
percentage, grade, pass or fail. Reads marks in any status, and says per column whether it is final.
**Not a rank** — ranks exist only on a published card ([open item 10](#10-rank)).

<a id="e28"></a>
**[#28](#t28) · `GET /students/{studentDocsId}/marks?termDocsId=&examDocsId=`** — planned

- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: every mark of the child
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: the paper behind each mark
- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: the exam each mark is grouped under, and its term

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/students/{studentDocsId}/marks
    ?termDocsId=…       // optional
    &examDocsId=…       // optional
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- every mark the child has, grouped by exam
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{studentDocsId}` (path) | **yes** | The child. |
| `termDocsId` | no | Only this term's marks. |
| `examDocsId` | no | Only this exam's marks. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

every mark one child has, filtered by term
or exam, grouped by exam. What a parent-teacher meeting opens on.

## Report cards

<a id="e29"></a>
**[#29](#t29) · `POST /report-cards/generate`** — planned

- [`exams`](../../../models/academics/examination/Exam.java) — *reads*: the term's exams that are not cancelled — `name`, `weightPercent`
- [`exam_schedules`](../../../models/academics/examination/ExamSchedule.java) — *reads*: every non-cancelled paper of those exams for the section
- [`student_marks`](../../../models/academics/examination/StudentMark.java) — *reads*: `status` — every one must be `LOCKED`; `obtainedMarks`, `graceMarks`
- [`exam_candidates`](../../../models/academics/examination/ExamCandidate.java) — *reads*: `papers.attendanceStatus`, `status` — why a mark is missing (absent, exempt, withheld, debarred) (the plan text says this; the table does not list it)
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: the term's daily registers — the attendance days
- [`grading_schemes`](../../../models/academics/grading/GradingScheme.java) — *reads*: `gradeBands` — the scheme in force
- [`academic_years`](../../../models/core/AcademicYear.java), [`academic_terms`](../../../models/academics/structure/AcademicTerm.java) — *reads*: `resultsLocked` — the results lock
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *inserts / updates*: one card per child — `termName`, `className`, `sectionNo`, `rollNo`, `examDocsIds`, `gradingSchemeDocsId`, `subjectResults` (with `components`), `totalMaximumMarks`, `totalObtainedMarks`, `overallPercentage`, `overallGradeCode`, `resultStatus`, `attendanceWorkingDays`, `attendancePresentDays`, `status` = `DRAFT`, `reportVersion`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/report-cards/generate
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "termDocsId": "…",    // the term
  "classDocsId": "…",   // the class
  "sectionNo": "A"      // the section
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `termDocsId` | **yes** | The term. |
| `classDocsId` | **yes** | The class. |
| `sectionNo` | **yes** | The section. One card per child on its roster at the term's end. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 409 | `MARKS_NOT_LOCKED` | A paper of the term is not `LOCKED`. The message names the papers. |
| 409 | `WEIGHTS_DO_NOT_ADD_UP` | Some exams are weighted and the weights do not add to 100. The message names the exams. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "termDocsId": "…", "classDocsId": "…",
"sectionNo": "A" }`

- **Every non-cancelled paper of every non-cancelled exam in the term, for this section, must be
  `LOCKED`** → `409 MARKS_NOT_LOCKED`, naming the papers. The model README's lock-before-publish.
- **Weights checked here**: in a term, either no exam is weighted or the weighted ones add to 100 →
  `409 WEIGHTS_DO_NOT_ADD_UP` ([open item 9](#9-exam-weights)).
- **Every paper is snapshotted as a component** with its exam's id, name and weight, the effective
  mark and grace, and — when there is no mark — the hall status or `debarred`, so the card prints
  "absent", "exempt", "withheld" or "debarred". Each subject records the grading scheme that graded it.
- One card per child on the section's roster at the term's end. **A subject the child was debarred
  from** shows "debarred" rather than marks ([open item 14](#14-a-debarred-childs-report-card)). **Snapshotted**: term name, class
  name, section, roll number, scheme, every subject's components and totals, grades from the scheme
  in force, pass/fail ([open item 13](#13-pass-and-fail)).
- **Attendance days from the daily register**, the term's dates, counted as
  [attendance #10](../attendance/README.md#e10) counts them — DAILY only, LATE attended, cancelled
  registers out, only the days the child was in the section.
- **Re-running replaces `DRAFT` and `REVIEWED` cards** in place (back to `DRAFT`); a `PUBLISHED` one is
  left alone; after a `REVOKED` one it makes `reportVersion + 1`. `classRank` stays empty until #34.
- **Results lock** runs.

<a id="e30"></a>
**[#30](#t30) · `GET /report-cards?termDocsId=&classDocsId=&sectionNo=&status=`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: the latest `reportVersion` of each child's card — `totalObtainedMarks`, `overallPercentage`, `overallGradeCode`, `resultStatus`, `status`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/report-cards
    ?termDocsId=…
    &classDocsId=…
    &sectionNo=A
    &status=DRAFT       // optional — DRAFT, REVIEWED,
                        // PUBLISHED or REVOKED
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- a section's cards for a term, the latest
  version of each
- each with total, percentage, grade, result
  and status
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `termDocsId` | — | The term. |
| `classDocsId` | — | The class. |
| `sectionNo` | — | The section. |
| `status` | no | Only cards in this status. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

a section's cards for a term, the latest version of each, with
total, percentage, grade, result and status. Filter by status.

<a id="e31"></a>
**[#31](#t31) · `GET /report-cards/{cardId}`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: the whole card. No other collection

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/report-cards/{cardId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- the card exactly as stored
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `REPORT_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

one card exactly as stored. **Reads no other
collection** — a published card must reprint years later from itself alone (model README section 9).

<a id="e32"></a>
**[#32](#t32) · `PATCH /report-cards/{cardId}/remarks`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: `status` — `DRAFT` or `REVIEWED` only; `version`
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *updates*: `classTeacherRemark`, `principalRemark`, `subjectResults.teacherRemark`; a `REVIEWED` card goes back to `DRAFT`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}/report-cards/{cardId}/remarks
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "classTeacherRemark": "…",   // optional
  "principalRemark": "…",      // optional
  … per subject teacherRemark  // shape not set yet
  "version": 1                 // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |
| `classTeacherRemark` | no | The class teacher's remark. |
| `principalRemark` | no | The principal's remark. |
| per subject `teacherRemark` | no | A subject teacher's remark. |
| `version` | **yes** | The card's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `REPORT_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 409 | `CARD_PUBLISHED` | The card is not `DRAFT` or `REVIEWED`. |
| 409 | `CONCURRENT_MODIFICATION` | The card changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`classTeacherRemark`, `principalRemark`,
and per subject `teacherRemark`, `version`. `DRAFT` or `REVIEWED` only → `409 CARD_PUBLISHED`. A
remark changed on a `REVIEWED` card sends it back to `DRAFT`.

<a id="e33"></a>
**[#33](#t33) · `POST /report-cards/{cardId}/review`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: `status`, `version`
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *updates*: `status` `DRAFT` → `REVIEWED`, `reviewedAt`, `reviewedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/report-cards/{cardId}/review
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "version": 2          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |
| `version` | **yes** | The card's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `REPORT_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The card changed since this version was read. |
| 409 | `RESULTS_LOCKED` | The year's or the term's results are frozen. The message says which. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "version": n }`. `DRAFT` → `REVIEWED`,
with `reviewedAt` and `reviewedByDocsId`.

<a id="e34"></a>
**[#34](#t34) · `POST /report-cards/publish`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: `status` of every card of the section — none may still be `DRAFT`
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *updates*: `classRank`, `rankedStudentCount`, `publishedAt`, `publishedByDocsId`, `status` → `PUBLISHED`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/report-cards/publish
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "termDocsId": "…",
  "classDocsId": "…",
  "sectionNo": "A"
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `termDocsId` | **yes** | The term. |
| `classDocsId` | **yes** | The class. |
| `sectionNo` | **yes** | The section. Every card in it is published at once. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 409 | `CARDS_NOT_REVIEWED` | A card in the section is still `DRAFT`. The message gives the count. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

Not refused by the results lock — publishing is allowed while frozen.

`{ "termDocsId": "…", "classDocsId": "…",
"sectionNo": "A" }`. **Every card of the section at once**, because ranks are frozen together: a rank
set while a classmate's card is still a draft would be wrong the moment that card changes. Refused
while any card in the section is still `DRAFT` (`409 CARDS_NOT_REVIEWED`, with the count). Sets
`classRank`, `rankedStudentCount`, `publishedAt`, `publishedByDocsId`. Allowed while the term's
results are locked.

<a id="e35"></a>
**[#35](#t35) · `POST /report-cards/{cardId}/revoke`** — planned

- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *reads*: `status` — must be `PUBLISHED`; `version`
- [`report_cards`](../../../models/academics/examination/ReportCard.java) — *updates*: `status` → `REVOKED`, `revocationReason`, `revokedAt`, `revokedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/report-cards/{cardId}/revoke
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "reason": "…",        // REQUIRED
  "version": 3          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |
| `reason` | **yes** | Saved as `revocationReason`. |
| `version` | **yes** | The card's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `REPORT_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 409 | `CONCURRENT_MODIFICATION` | The card changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "reason": "…", "version": n }`.
`PUBLISHED` → `REVOKED`, with `revocationReason`, `revokedAt` and `revokedByDocsId` saved on the
card. The marks behind it can then be unlocked and corrected, and #29 makes the next version.
**Never edited in place.**

## Holistic progress cards

<a id="e36"></a>
**[#36](#t36) · `POST /holistic-progress-cards/generate`** — planned

- [`student_academic_records`](../../../models/student/StudentAcademicRecord.java) — *reads*: `classDocsId`, `sectionNo`, `rollNo` — the section's roster
- [`attendance_sessions`](../../../models/academics/attendance/AttendanceSession.java), [`student_attendance_records`](../../../models/academics/attendance/StudentAttendanceRecord.java) — *reads*: the attendance days, counted as #29 does
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: which children already have a card for the term — they are left alone
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *inserts*: one `DRAFT` per child — `termDocsId`, `termName`, `nepStage`, `classDocsId`, `className`, `sectionNo`, `rollNo`, `attendanceWorkingDays`, `attendancePresentDays`, `assessedByStaffDocsId` (the section's class teacher, or empty), `domains` = empty, `cardVersion`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/holistic-progress-cards/generate
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "termDocsId": "…",
  "classDocsId": "…",
  "sectionNo": "A",
  "nepStage": "MIDDLE"  // FOUNDATIONAL, PREPARATORY,
                        // MIDDLE or SECONDARY
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `termDocsId` | **yes** | The term. |
| `classDocsId` | **yes** | The class. |
| `sectionNo` | **yes** | The section. |
| `nepStage` | **yes** | `FOUNDATIONAL`, `PREPARATORY`, `MIDDLE` or `SECONDARY`. Sent, never worked out from the class name. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`{ "termDocsId": "…", "classDocsId":
"…", "sectionNo": "A", "nepStage": "MIDDLE" }`. One `DRAFT` per child on the roster who has none for
the term — existing ones are left alone. Class, section, roll and attendance days copied in as #29
does. **`nepStage` is sent**, never derived from the class name — the model says why. The card
starts with **no domains**; `assessedByStaffDocsId` starts as the section's class teacher, or empty.

<a id="e37"></a>
**[#37](#t37) · `PATCH /holistic-progress-cards/{cardId}`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: `status` — `DRAFT` or `REVIEWED` only; `version`
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *updates*: `domains`, `teacherRemark`, `selfReflection`, `peerFeedback`, `developmentGoals`; `assessedByStaffDocsId` = the signed-in staff member
- [`guardians`](../../../models/student/Guardian.java) — listed in the table above; parent feedback itself is #50

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PATCH /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "domains": [
    { "domain": "COGNITIVE_DEVELOPMENT",
      "level": "RIVER",
      "observation": "…",
      "strengths": "…",
      "nextStep": "…" }
  ],
  "teacherRemark": "…",
  "selfReflection": "…",
  "peerFeedback": "…",
  "developmentGoals": "…",
  "version": 1          // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |
| `domains` | no | Each `domain` at most once. `domain` is a `LearningDomain`, `level` an `HpcLevel` (`STREAM`, `RIVER`, `MOUNTAIN`, `SKY`). `observation` may be empty while the card is a draft. |
| `teacherRemark` | no | |
| `selfReflection` | no | The child's reflection. |
| `peerFeedback` | no | |
| `developmentGoals` | no | |
| `version` | **yes** | The card's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| — | not named yet | The card is not `DRAFT` or `REVIEWED`, or a domain is sent twice. |
| 409 | `CONCURRENT_MODIFICATION` | The card changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`domains` (each `domain`, `level`,
`observation`, `strengths`, `nextStep`), `teacherRemark`, `selfReflection`, `peerFeedback`,
`developmentGoals`, `version`. **Not parent feedback** — that is [#50](#e50). A domain at most once. An observation may be left empty while the
card is a draft. `assessedByStaffDocsId` becomes the signed-in staff member. `DRAFT` or `REVIEWED`
only.

<a id="e38"></a>
**[#38](#t38) · `POST /holistic-progress-cards/{cardId}/review`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: `status`, `domains` (`domain`, `level`, `observation`) — all five policy domains present, and every domain with a level has an observation
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *updates*: `status` `DRAFT` → `REVIEWED`, `reviewedAt`, `reviewedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}/review
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

Body not named in the plan.
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 400 | `DOMAINS_MISSING` | One of the five policy domains is missing. The message names them. |
| 400 | `OBSERVATION_REQUIRED` | A domain has a level but no `observation`. The message names the domains. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`DRAFT` → `REVIEWED`, with
`reviewedAt` and `reviewedByDocsId`. **This is where the card is checked**, not on every save:
refused while any of the five policy domains is missing (`400 DOMAINS_MISSING`, naming them) or any
domain has a level but no `observation` (`400 OBSERVATION_REQUIRED`, naming the domains) — a card of
bare levels is unfinished.

<a id="e39"></a>
**[#39](#t39) · `POST /holistic-progress-cards/{cardId}/publish`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: `status` — must be `REVIEWED`
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *updates*: `status` → `PUBLISHED`, `publishedAt`, `publishedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}/publish
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

Body not named in the plan.
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`REVIEWED` → `PUBLISHED`. One
card at a time: there is no rank to keep in step.

<a id="e40"></a>
**[#40](#t40) · `POST /holistic-progress-cards/{cardId}/revoke`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *updates*: `status` → `REVOKED`, `revocationReason`, `revokedAt`, `revokedByDocsId`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}/revoke
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "revocationReason": "…"   // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |
| `revocationReason` | **yes** | Why the card is withdrawn. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 409 | `ACADEMIC_YEAR_NOT_RUNNING` | The year in the path is not the one the school is running. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

`revocationReason` required;
`revokedAt` and `revokedByDocsId` saved beside it.
The next #36 for that child makes `cardVersion + 1`.

<a id="e50"></a>
**[#50](#t50) · `PUT /holistic-progress-cards/{cardId}/parent-feedback`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: `status` — not `REVOKED`; `studentDocsId`; `version`
- [`guardians`](../../../models/student/Guardian.java) — *reads*: the guardian must be one of the child's guardians
- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *updates*: `parentFeedbacks` — one entry per guardian, added or replaced: `guardianDocsId`, `feedback`, `givenAt`, `recordedByDocsId`. No new version

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
PUT /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}/parent-feedback
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

{
  "guardianDocsId": "…",   // one of the child's guardians
  "feedback": "…",
  "version": 4             // REQUIRED
}
</pre></td>
<td><pre>
Not designed yet.
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. Not `REVOKED`. A `PUBLISHED` card is allowed. |
| `guardianDocsId` | **yes** | One of the child's guardians. Sending one who already has an entry replaces it. |
| `feedback` | **yes** | The guardian's own words. |
| `version` | **yes** | The card's version. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 400 | `GUARDIAN_NOT_LINKED` | The guardian is not one of the child's. |
| 409 | `CARD_REVOKED` | The card is revoked. Write it on the card that replaced it. |
| 409 | `CONCURRENT_MODIFICATION` | The card changed since this version was read. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

Gates 1 and 2 only — not gate 4, and not the results lock.

`{ "guardianDocsId": "…", "feedback": "…", "version": n }`

- **One entry per guardian.** Sending a guardian who already has an entry replaces it — their own
  words, corrected. Each parent writes their own.
- The guardian must be **one of the child's guardians** → `400 GUARDIAN_NOT_LINKED`.
- **Allowed on a `PUBLISHED` card, and does not make a new version.** Families write their view
  after seeing the card. Everything the school wrote stays frozen; only `parentFeedbacks` changes.
  Refused on a `REVOKED` card (`409 CARD_REVOKED`) — write it on the card that replaced it.
- `givenAt` and `recordedByDocsId` are the server's.
- Gates 1 and 2. **Not gate 4** — a family's feedback on the final term's card often arrives after
  the new year has started ([open item 12](#12-gate-4-and-the-final-terms-cards)). Not the results
  lock: it is not a result.

<a id="e41"></a>
**[#41](#t41) · `GET /holistic-progress-cards?termDocsId=&classDocsId=&sectionNo=&status=`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: the latest `cardVersion` of each child's card in the section

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/holistic-progress-cards
    ?termDocsId=…
    &classDocsId=…
    &sectionNo=A
    &status=DRAFT       // optional — DRAFT, REVIEWED,
                        // PUBLISHED or REVOKED
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- a section's cards, the latest version each
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `termDocsId` | — | The term. |
| `classDocsId` | — | The class. |
| `sectionNo` | — | The section. |
| `status` | no | Only cards in this status. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

#41 and #42 — as [#30](#e30) and [#31](#e31).

<a id="e42"></a>
**[#42](#t42) · `GET /holistic-progress-cards/{cardId}`** — planned

- [`holistic_progress_cards`](../../../models/academics/examination/HolisticProgressCard.java) — *reads*: the whole card

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/academic-years/{year}/holistic-progress-cards/{cardId}
Cookie:  idtoken=…   REQUIRED on every call — it names
         the school (the X-School-Subdomain header
         is no longer read)

No body.
</pre></td>
<td><pre>
Not designed yet. The plan says it holds:

- one card in full
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `{cardId}` (path) | **yes** | The card's id. |

**When it is refused**

| Status | Code | When |
|---|---|---|
| 404 | `ACADEMIC_YEAR_NOT_FOUND` | There is no year of that name in this school. |
| 404 | `PROGRESS_CARD_NOT_FOUND` | There is no card of that id in this year. |
| 400 | `TENANT_NOT_RESOLVED` | There is no `idtoken` cookie, so the school is not known. |

As [#31](#e31).

---

# To settle before building

## 1. Who sits a paper

**Decided 2026-10-16: the candidate list** — [#43](#e43)–[#49](#e49). A school must be able to stop
a child from sitting (fees unpaid, attendance short, a disciplinary decision), and a debarred child's
name must not be on the hall register at all. So the register is built from
[`exam_candidates`](../../../models/academics/examination/ExamCandidate.java), not straight from the
section.

**Electives are still open.** A candidate is expected at every paper of their section — the paper
holds the subject, and the candidate stores none (a per-candidate subject list was tried and removed
on 2026-10-16). Which child takes which elective is a year-long choice, so it belongs on
`StudentAcademicRecord`, where attendance, the timetable and report cards can read it too. Until
that exists, a child who does not take an elective is marked `EXEMPT` in the hall at [#15](#e15).

**Still open: should publishing add candidates automatically?** (a) No — the office runs #43 once per
section, which matches "I add the children who may sit"; (b) yes — #3 adds every section's roster as
`ELIGIBLE`, and the office only debars. **Recommendation: (a)**, so nobody is a candidate without the
office having said so; #43 with no `studentDocsIds` makes it one call per section.

**Fees are recorded by hand for now.** The finance module is not built, so `FEES_DUE` is set by the
office. When finance exists, "debar every candidate with dues" can read it — a later endpoint, not a
change to this one.

## 2. The exam and paper status graphs

`ExamStatus` is `DRAFT, SCHEDULED, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED`, and
`ExamScheduleStatus` is `DRAFT, SCHEDULED, PUBLISHED, COMPLETED, CANCELLED`, and both have a
`SCHEDULED` that nothing in this plan needs. **Options:** (a)
`SCHEDULED` is unused — `DRAFT` → `PUBLISHED` at [#3](#e3); (b) `SCHEDULED` means "datesheet final,
not yet shown to families", with its own endpoint before #3. **Recommendation: (a)** — nothing in this
plan shows a datesheet to families yet, so the difference between the two would be invisible.
**Built as (a) on 2026-10-22** with [#3](#e3).
`IN_PROGRESS` is set by the server on the first hall save ([#15](#e15)), never sent.

## 3. Submit and lock are per paper, not per child

`MarkEntryStatus` sits on every `StudentMark` row, so a single child's mark could in principle be
submitted or locked on its own. **Recommendation:** [#22](#e22)–[#24](#e24) move **every row of a
paper together** — a teacher submits Maths for 7A, not one child's Maths — and the row status is
just where that is recorded. `PUBLISHED` in `MarkEntryStatus` is left unused: a mark is published
through the card that carries it.

## 4. Hall entries ahead of time, or not

**Settled 2026-10-16 as (b)**, and the model README now says so. "Expected" means an eligible
candidate ([item 1](#1-who-sits-a-paper)). **Options were:** (a) create a row per expected child when the paper is published; (b) no rows until
marked, the register shows the unmarked, and [#16](#e16) refuses to complete while anyone is unmarked
— exactly how attendance works. **Recommendation: (b)** — it reaches the same guarantee (nobody is
silently missing once a paper is complete), needs no "pending" status the enum does not have, and a
child who joins the section after publishing is picked up automatically.

## 5. `markedByDocsId` is required

**Settled 2026-10-16** — `ExamPaperAttendance.markedByDocsId` is optional, set from the signed-in
staff when there is one, because an admin or the local user has no staff id. Requiring it to be one
of the paper's invigilators can be added when roles exist.

## 6. Where correction reasons are stored

[#5](#e5), [#10](#e10), [#17](#e17) and [#24](#e24) all take a reason, and none of those models has
anywhere to keep one. (Debarment reasons are already stored — in the candidate's `statusChanges`.) **Options:** (a) an `unlocks`-style history on `ExamSchedule` (reason, when, who,
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
subject with `countsTowardTotal` fails, `WITHHELD` when any paper's hall entry is `UNFAIR_MEANS` or the child was debarred from one, `INCOMPLETE` when a
paper has no mark, otherwise `PASS`. Grade-only subjects never fail.

## 14. A debarred child's report card

A child debarred from a paper has no hall entry and no mark for it; the card reads the debarment from
the candidate.
**Options:** (a) the card's result is `WITHHELD` until the school decides — a re-sit, or readmission
for a later exam; (b) the subject counts as failed; (c) `INCOMPLETE`. **Recommendation: (a)** — a fee
debarment is not an academic failure, and `WITHHELD` is what a school says to a family in that case.

---

# Where the code will live

```text
controllers/academics/examination/
    ExamController.java                 #1–#7, #26–#28 — the exam, and its results
    ExamScheduleController.java         #8–#13, #16 — the datesheet, and completing a paper
    ExamCandidateController.java        #43–#49 — who may sit
    ExamAttendanceController.java       #14, #15, #17 — the hall (writes ExamCandidate.papers)
    StudentMarkController.java          #18–#25 — marks (#18 built)
    ReportCardController.java           #29–#35
    HolisticProgressCardController.java #36–#42, #50

services/academics/
    ExamService.java · ExamScheduleService.java · ExamCandidateService.java · ExamAttendanceService.java
                                        (ExamAttendanceService reads and writes ExamCandidate.papers)
    StudentMarkService.java · ReportCardService.java · HolisticProgressCardService.java
    utils/ExaminationServiceUtils.java  loading an exam/paper, the results lock, the paper roster

repositories/academics/examination/
    ExamRepository · ExamScheduleRepository · ExamCandidateRepository (+ Custom/Impl for papers[])
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
| `EXAM_NOT_FOUND` · `EXAM_SCHEDULE_NOT_FOUND` · `EXAM_CANDIDATE_NOT_FOUND` · `STUDENT_MARK_NOT_FOUND` · `REPORT_CARD_NOT_FOUND` · `PROGRESS_CARD_NOT_FOUND` | 404 | No such row in this school and year. |
| `EXAM_NAME_TAKEN` | 409 | [#1](#e1)/[#2](#e2) — the name is used in the year, case-insensitively. |
| `TERM_NOT_ACTIVE` · `GRADING_SCHEME_NOT_ACTIVE` | 409 | [#1](#e1) — the term or the scheme has been retired. |
| `EXAM_DATES_OUTSIDE_TERM` | 409 | [#1](#e1)/[#2](#e2) — the window is not inside the term. |
| `EXAM_HAS_PAPERS` · `PAPERS_OUTSIDE_DATES` · `EXAM_NOT_EDITABLE` | 409 | [#2](#e2). |
| `NOTHING_TO_UPDATE` · `EXAM_NAME_REQUIRED` | 400 | [#2](#e2). |
| `EXAM_HAS_NO_PAPERS` · `PAPERS_STILL_OPEN` | 409 | [#3](#e3), [#4](#e4). |
| `SUBJECT_NOT_TAUGHT_IN_SECTION` | 400 | [#8](#e8). **Names the sections.** |
| `PAPER_ALREADY_SCHEDULED` · `PAPER_OUTSIDE_EXAM` | 409 | [#8](#e8)/[#9](#e9). |
| `ROOM_CLASH` · `INVIGILATOR_CLASH` | 409 | [#8](#e8)/[#9](#e9) — a room with no capacity shared with another sitting; an invigilator in another room then. **Names the other paper.** |
| `DUPLICATE_SECTION_IN_REQUEST` | 400 | [#8](#e8). |
| `ROOM_NOT_FOUND` · `STAFF_NOT_FOUND` · `CLASS_NOT_FOUND` | 404 | [#8](#e8). |
| `ROOM_NOT_IN_USE` · `SECTION_NOT_ACTIVE` · `ROOM_OVER_CAPACITY` | 409 | [#8](#e8). |
| `DUPLICATE_ROOM_IN_REQUEST` · `INVIGILATOR_IN_TWO_ROOMS` | 400 | [#8](#e8). |
| `SECTION_CLASH` | 409 | [#8](#e8) — a section already sits another paper at an overlapping time that day. **Names it.** |
| `ROOM_NOT_IN_PAPER` | 400 | [#15](#e15) — a child marked in a room the paper does not use. |
| `NOT_ENOUGH_SEATS` | 409 | [#16](#e16) — the paper's rooms seat fewer than its eligible candidates. **Says both numbers.** |
| `PASSING_MARKS_ABOVE_MAXIMUM` · `INVALID_TIME_RANGE` | 400 | [#8](#e8)/[#9](#e9). |
| `MARKS_ALREADY_ENTERED` · `MARKS_ALREADY_LOCKED` | 409 | A paper or exam change that would contradict marks. |
| `PAPER_NOT_OPEN` · `PAPER_NOT_YET_SAT` · `PAPER_NOT_COMPLETED` | 409 | Hall and mark writes at the wrong moment. |
| `STUDENT_NOT_IN_PAPER` | 400 | [#15](#e15) — not an eligible candidate for this paper. **Names them.** |
| `STUDENT_DEBARRED` | 409 | [#15](#e15) — the child is debarred. **Names them and the reason.** |
| `STUDENT_NOT_IN_SECTION` | 400 | [#43](#e43) — a named child is not in that section during the exam. **Names them.** |
| `CANDIDATE_ALREADY_DEBARRED` · `CANDIDATE_NOT_DEBARRED` | 409 | [#45](#e45)–[#47](#e47). |
| `NOTE_REQUIRED` | 400 | [#45](#e45)/[#47](#e47) — reason `OTHER` with no note. |
| `CANDIDATE_HAS_RECORDS` | 409 | [#49](#e49) — the child already has a hall entry or a mark. **Names the paper.** |
| `ANSWER_COPY_REQUIRED` · `ANSWER_COPY_NOT_ALLOWED` | 400 | [#15](#e15). |
| `ANSWER_COPY_TAKEN` | 409 | [#15](#e15). **Names the booklet and who has it.** |
| `ANSWER_COPY_NOT_FOUND` | 404 | [#21](#e21). |
| `STUDENT_NOT_PRESENT` · `MARKS_OUT_OF_RANGE` · `GRADE_NOT_IN_SCHEME` | 400 | [#19](#e19)/[#21](#e21). **Name the child.** |
| `MARKS_NOT_EDITABLE` · `UNENTERED_MARKS` · `MARKS_NOT_SUBMITTED` | 409 | [#19](#e19), [#22](#e22), [#23](#e23). |
| `USED_BY_PUBLISHED_CARD` | 409 | [#24](#e24)/[#25](#e25). |
| `RESULTS_LOCKED` | 409 | The year's or term's results are frozen. **Says which.** |
| `MARKS_NOT_LOCKED` · `WEIGHTS_DO_NOT_ADD_UP` | 409 | [#29](#e29). **Name the papers / exams.** |
| `CARD_PUBLISHED` · `CARDS_NOT_REVIEWED` | 409 | [#32](#e32), [#34](#e34). |
| `OBSERVATION_REQUIRED` · `DOMAINS_MISSING` | 400 | [#38](#e38). **Name the domains.** |
| `GUARDIAN_NOT_LINKED` · `CARD_REVOKED` | 400 · 409 | [#50](#e50). |
| `UNMARKED_STUDENTS` · `REASON_REQUIRED` · `CONCURRENT_MODIFICATION` · `NOT_A_WORKING_DAY` | — | Shared with attendance. |
