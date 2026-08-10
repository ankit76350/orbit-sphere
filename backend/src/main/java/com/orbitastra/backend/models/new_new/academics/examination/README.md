# Examination persistence contract

These rules are mandatory when the examination repositories and services are
implemented. Every query and reference check must include `schoolId`.

## 1. Collections and their grain

```text
AcademicTerm (academics/structure)
└── Exam                       one examination event, one term
    └── ExamSchedule           one class + section + subject + component
        ├── ExamAttendance     one row per student   <-- hall presence + answer copy
        └── StudentMark         one row per student   <-- evaluated result
```

`ExamSchedule` is the register header. `ExamAttendance` therefore stands in the
same relation to it as `StudentAttendanceRecord` does to `AttendanceSession`, and
no separate exam-attendance session document is needed — the schedule already
owns the date, time, room, invigilators, and status.

`ExamAttendance` and `StudentMark` share the grain
`schoolId + examScheduleDocsId + studentDocsId`, but they are separate
collections on purpose. They are written by different people at different times:
the invigilator records presence in the hall, and the evaluator enters marks days
later. Splitting them also keeps mark entry independent of attendance locking and
makes blind evaluation possible.

## 2. Weighting

Weighting has exactly two levels and no join table:

```text
Exam.weightPercent          share of the term result
AcademicTerm.weightPercent  share of the annual result
```

Both are optional. When null, aggregate raw marks. Services must validate that
the active exam weights within a term sum to 100, and that active term weights
within the year sum to 100, whenever weighting is used at all. Never mix weighted
and unweighted exams inside the same term.

## 3. Answer copy accountability

- `ExamAttendance.answerCopyNo` is unique within one `Exam`, enforced by
  `school_exam_answer_copy_no_uniq`. The same physical booklet cannot be recorded
  against two students.
- Schools that restart numbering per subject must prefix the value, for example
  `"MATH-0001"`, otherwise distinct booklets collide inside one exam.
- `additionalAnswerCopyNos` holds supplementary booklets. MongoDB cannot extend
  the unique index across that list, so the service must check every submitted
  supplementary number against both `answerCopyNo` and
  `additionalAnswerCopyNos` within the same exam before saving.
- `answerCopyNo` is null when `attendanceStatus` is `ABSENT`, and must be present
  when the status is `PRESENT`. The partial index ignores null values so absent
  rows do not collide.
- An issued copy number is immutable. A wrongly recorded number is corrected by
  updating the row under its expected `version`, and the correction is visible
  through the inherited audit fields.

## 4. Late arrival and early departure

These are derived, not stored as states:

```text
reportedAt  > ExamSchedule.startTime  -> admitted late
submittedAt < ExamSchedule.endTime    -> left early
```

`ExamAttendanceStatus` therefore has only `PRESENT`, `ABSENT`, and
`UNFAIR_MEANS`. Exemption and a withheld result are result-side states and belong
to `StudentMark.participationStatus`.

## 5. Attendance and marks must agree

- `ExamAttendance` is the source of truth for whether a student appeared. The
  service copies that outcome into `StudentMark.participationStatus` at mark
  entry.
- Reject a mark for a student whose attendance row is `ABSENT`.
- A row with `UNFAIR_MEANS` must not receive a normal mark; set
  `participationStatus` to `WITHHELD` until the case is decided.
- Attendance closes when the owning `ExamSchedule` reaches `COMPLETED`. There is
  no per-row lock field.
- Generate attendance rows from the section roster
  (`StudentAcademicRecord` where `status = ACTIVE`) filtered to the students who
  actually take that subject. Until per-student elective choice exists upstream,
  an elective subject will otherwise produce rows for students who do not sit it.

## 6. Blind evaluation

Because the copy number lives on `ExamAttendance` and not on `StudentMark`, an
evaluator can be given a copy-number list with no student identity. The service
resolves `answerCopyNo` to `studentDocsId` through `ExamAttendance` and writes the
`StudentMark` row itself. Do not add `answerCopyNo` to `StudentMark`; that would
defeat the separation.

## 7. Grace marks and corrections

`obtainedMarks` is the evaluator's award and is never overwritten by moderation.
An adjustment is recorded in `graceMarks` with a `graceReason`, so the effective
mark is:

```text
effectiveMarks = obtainedMarks + graceMarks
```

A revaluation reopens the row: unlock, edit, relock. The inherited audit fields
and `version` carry the history; there is no separate revaluation collection.

## 8. Lock and publish order

```text
ExamSchedule COMPLETED
  -> StudentMark SUBMITTED -> LOCKED
    -> ReportCard DRAFT -> REVIEWED -> PUBLISHED
```

Before any result write, check both locks:

```text
AcademicYear.resultsLocked   year-wide, overriding
AcademicTerm.resultsLocked   this reporting period only
```

Mark submission, locking, report generation, and publication require transactions
or idempotent workflows.

## 9. Report cards are snapshots

A published `ReportCard` must be reprintable years later without reading any
other collection. That is why it copies in `termName`, `className`, `sectionNo`,
`rollNo`, `gradingSchemeDocsId`, the attendance day counts, and the per-component
breakdown in `ReportCardSubjectResult.components`.

- Never recalculate a published card in place. A correction creates the next
  `reportVersion`.
- `classRank` and `rankedStudentCount` are frozen at publication. A rank computed
  on read would drift as other students' marks change.
- `countsTowardTotal` records whether a subject was included in the totals for
  this snapshot. It is separate from `subjectType`, which only groups rows on the
  printed card.
- Grade-only subjects store `gradeCode` with every marks field left null.

## 10. Validation responsibility

The models carry only structural constraints. DTOs and services validate tenant
ownership, that `termDocsId` belongs to the same school and academic year, that
`classDocsId`/`sectionNo`/`subjectCode` resolve inside the referenced
`SchoolClass`, date and time ordering, marks within `0..maximumMarks`, grading
band coverage, answer-copy uniqueness including supplementary booklets, weight
totals, status transitions, and publication authorization.

Grading scheme resolution order is:

```text
ClassSubject.gradingSchemeDocsId  ->  Exam.gradingSchemeDocsId  ->  none
```

No third override exists on `ExamSchedule`; do not add one.
