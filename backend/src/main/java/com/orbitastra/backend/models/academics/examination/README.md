# Examination persistence contract

These rules are mandatory when the examination repositories and services are
implemented. Every query and reference check must include `schoolId`.

## How the models fit together

```text
AcademicTerm  ../structure/AcademicTerm.java
     |
     v
Exam                         "Half Yearly Examination, Term 1"
  |    one per exam event, per school
  |
  +--> ExamCandidate[]       one per child allowed to sit — ELIGIBLE or DEBARRED
  |        +--> papers[]       embedded, one per paper they were marked at —
  |                            present / absent, answer copy, reported, submitted
  |
  +--> ExamSchedule[]        one per subject per section — the datesheet
  |        maximumMarks, date, time, room
  |
  +--> StudentMark[]         one per student per subject — what they scored
           |                   obtainedMarks, participationStatus
           |
           v
      ReportCard             the term's marks, snapshotted
        +--> ReportCardSubjectResult[]
                +--> ReportCardComponentResult[]

      HolisticProgressCard   the same term, in words rather than marks
        +--> DomainAssessment[]
```

`ReportCard` and `HolisticProgressCard` are **siblings**. A child has one of each per term —
see the section at the end of this file.

### Models in this package

| Model | Grain — one row per… |
|---|---|
| [Exam](Exam.java) | exam event, per school |
| [ExamCandidate](ExamCandidate.java) | child allowed to sit, per exam |
| [ExamSchedule](ExamSchedule.java) | subject, per section, within an exam |
| [StudentMark](StudentMark.java) | student, per subject, per exam |
| [ReportCard](ReportCard.java) | student, per term |
| [HolisticProgressCard](HolisticProgressCard.java) | student, per term |
| [ExamCandidateStatusChange](embedded/ExamCandidateStatusChange.java) | embedded in a candidate — one debarment or return |
| [ExamPaperAttendance](embedded/ExamPaperAttendance.java) | embedded in a candidate — hall attendance at one paper |
| [ReportCardSubjectResult](embedded/ReportCardSubjectResult.java) | embedded in a report card |
| [ReportCardComponentResult](embedded/ReportCardComponentResult.java) | embedded in a subject result |
| [DomainAssessment](embedded/DomainAssessment.java) | embedded in a progress card |

### Models from other packages

| Model | Lives in | Used for |
|---|---|---|
| [AcademicTerm](../structure/AcademicTerm.java) | `academics/structure` | the term an exam and a card belong to |
| [SchoolClass](../structure/SchoolClass.java) | `academics/structure` | the class sitting a paper; `ClassSubject` gives the subject codes |
| [GradingScheme](../grading/GradingScheme.java) | `academics/grading` | turning marks into a grade band |
| [Student](../../student/Student.java) | `student` | who sat the paper and whose card it is |
| [StudentAcademicRecord](../../student/StudentAcademicRecord.java) | `student` | the child's placement for the year |
| [Guardian](../../student/Guardian.java) | `student` | who wrote the parent feedback on a progress card |
| [Staff](../../people/staff/Staff.java) | `people/staff` | invigilator, evaluator, who published |
| [DocumentRecord](../../documents/DocumentRecord.java) | `documents` | the printed card, and evidence on a domain |
| [AcademicYear](../../core/AcademicYear.java) | `core` | working days, and the year every row is scoped to |
| [AppModule](../../identity/enums/AppModule.java) | `identity/enums` | the `EXAMINATIONS` permission |

## When each one comes into play

```text
BEFORE THE EXAM

  weeks ahead   Exam created                      "Half Yearly, Term 1"
                                                  status DRAFT
  ~2 weeks      ExamSchedule rows added           one per subject per section
                                                  this is the datesheet parents see
                Exam published                    status SCHEDULED
                Candidates added                  the children allowed to sit,
                                                  ELIGIBLE; a child with fees
                                                  due is DEBARRED, with a reason

ON THE DAY, PER PAPER

  in the hall   the register lists only ELIGIBLE  a debarred child's name is not
                candidates who take the subject   there, so no attendance can be
                                                  taken for them
  in the hall   attendance marked                 present / absent / unfair means
                                                  answer copy number recorded

AFTER THE PAPER

  marking       StudentMark rows filled           one per student per subject
                                                  blind evaluation: copy number,
                                                  not the child's name
  checking      attendance and marks reconciled   a child marked absent cannot
                                                  have marks; see section 5
  lock          marks locked                      no further edits without a
                                                  recorded correction

END OF TERM

  compile       ReportCard generated              marks snapshotted from
                                                  StudentMark, grades from the
                                                  GradingScheme in force
  write         HolisticProgressCard written       by the class teacher, with the
                                                  child's, classmates' and
                                                  family's contributions
  publish       both published                    families can now see them;
                                                  neither is edited afterwards
```

Two orderings matter and are enforced rather than assumed:

- **Attendance before marks.** The rows exist before the paper is marked, so a child who did
  not sit it is visible rather than absent from the data. Section 5 covers what must agree.
- **Lock before publish.** A report card is a snapshot; snapshotting marks that can still
  change produces a card that disagrees with the source a week later. Section 8 covers the
  order.

## 1. Collections and their grain

```text
AcademicTerm (academics/structure)
└── Exam                       one examination event, one term
    ├── ExamCandidate          one per child allowed to sit
    │   └── papers[]           embedded, one per paper  <-- hall presence + answer copy
    └── ExamSchedule           one class + section + subject + component
        └── StudentMark        one row per student      <-- evaluated result
```

`ExamSchedule` is the register header. Its rows are the entries for that paper in
`ExamCandidate.papers` — the same relation `StudentAttendanceRecord` has to
`AttendanceSession` — and no separate exam-attendance session document is
needed: the schedule already owns the date, time, room, invigilators, and status.

**Hall attendance is embedded in the candidate, decided 2026-10-16.** There is no
`exam_attendances` collection. Only a candidate can be marked, eligibility and
attendance are read and written in one document, and a child's whole exam is one
read. A child sits a handful of papers, so the list stays small.

**Marks stay a separate collection on purpose.** Attendance and marks are written
by different people at different times: the invigilator records presence in the
hall, and the evaluator enters marks days later. Keeping `StudentMark` apart
keeps mark entry independent of attendance and makes blind evaluation possible.

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

- `papers.answerCopyNo` is unique within one `Exam`, enforced by
  `school_exam_answer_copy_no_uniq` on `ExamCandidate`. A unique index on an
  array field is enforced across documents, so the same physical booklet cannot
  be recorded against two students. It is **not** enforced inside one document:
  the service must refuse one child holding the same number on two papers.
- Schools that restart numbering per subject must prefix the value, for example
  `"MATH-0001"`, otherwise distinct booklets collide inside one exam.
- `additionalAnswerCopyNos` holds supplementary booklets. MongoDB cannot extend
  the unique index across that list, so the service must check every submitted
  supplementary number against both `papers.answerCopyNo` and
  `papers.additionalAnswerCopyNos` across the exam's candidates before saving.
- `answerCopyNo` is null when `attendanceStatus` is `ABSENT`, and must be present
  when the status is `PRESENT`. The partial index ignores null values so absent
  rows do not collide.
- An issued copy number is immutable. A wrongly recorded number is corrected by
  updating the candidate under its expected `version`, and the correction is
  visible through the inherited audit fields.

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

- The candidate's `papers` entry is the source of truth for whether a student appeared. The
  service copies that outcome into `StudentMark.participationStatus` at mark
  entry.
- Reject a mark for a student whose `papers` entry is `ABSENT`.
- An entry with `UNFAIR_MEANS` must not receive a normal mark; set
  `participationStatus` to `WITHHELD` until the case is decided.
- Attendance closes when the owning `ExamSchedule` reaches `COMPLETED`. There is
  no per-row lock field.
- **Only a candidate can have hall attendance** — it lives inside the candidate. A paper's hall register is the
  section's children on the paper's date (from `StudentAcademicRecord`) who are
  `ELIGIBLE` `ExamCandidate`s and either have an empty `subjectCodes` or list
  the paper's subject. The candidate stores no class or section of its own.
  A child who is not a candidate, or is `DEBARRED`, gets no `papers` entry.
- **Debarring is not retroactive.** A paper already sat keeps its `papers` entry. Debarring
  removes the child from the papers still to come; letting them back in returns
  them to every paper not yet `COMPLETED`.
- **A paper held while a child was debarred** gets a `StudentMark` with
  `participationStatus = DEBARRED` and no marks, so a report card shows
  "debarred" rather than a gap or "absent".

## 6. Blind evaluation

Because the copy number lives on `ExamCandidate.papers` and not on `StudentMark`, an
evaluator can be given a copy-number list with no student identity. The service
resolves `answerCopyNo` to `studentDocsId` through the candidate and writes the
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


## HolisticProgressCard — `holistic_progress_cards`

The 2020 education policy's replacement for a card that says 62 percent: a rounded picture of
a child over one term, in words rather than marks, across several domains of development.

**It is a sibling of `ReportCard`, not a replacement for it.** Most schools will produce both
for years — marks because a board and the next school ask for them, and this because it is
required and because it says things a mark cannot. A child has one of each per term.

Making one a variant of the other would force a marks-shaped model onto something with no
marks in it.

| | `ReportCard` | `HolisticProgressCard` |
|---|---|---|
| Says | marks, percentage, grade, rank | levels and observations per domain |
| Written by | the school | the teacher, the **child**, their **classmates** and their **family** |
| Comparable between children | yes, that is the point | no, deliberately |
| Can record a failure | yes | no — a level describes where somebody is on the way |

Three things worth knowing:

- **`DomainAssessment.observation` is the card.** `level` is almost a footnote beside it.
  "RIVER in Language and Literacy" tells a parent nothing they can act on; the observation
  does. A card of bare levels is a report card with nicer words, and the service treats one as
  unfinished.
- **`nepStage` is stored, not derived.** A class is renamed and reorganised over the years;
  the stage a child was in does not change after the fact. A card for a five-year-old and one
  for a fifteen-year-old are different documents, and years later only this field says which
  was being read.
- **`peerFeedback` is summarised, never quoted.** A card handed to a family must not become a
  place where one child's words about another are repeated back.

Versioning, snapshotting and publication work exactly as they do for `ReportCard`: a published
card is never edited, a correction is `cardVersion + 1` with the old one revoked, and class,
section and roll number are copied in so a reprint shows the class the child was actually in.
