# Examination persistence contract

These rules are mandatory when the examination repositories and services are
implemented. Every query and reference check must include `schoolId`.

**Design finished 2026-10-16.** The API plan built on it is
[`controllers/academics/examination/README.md`](../../../controllers/academics/examination/README.md).
What changed in that final pass, and why, is listed at the [end of this file](#decisions-taken-2026-10-16).

## How the models fit together

```text
AcademicTerm  ../structure/AcademicTerm.java
     |
     v
Exam                         "Half Yearly Examination, Term 1"
  |    one per exam event, per school year — name unique in the year
  |
  +--> ExamCandidate[]       one per child allowed to sit — ELIGIBLE or DEBARRED
  |        +--> statusChanges[]   every debarment and return, with its reason
  |        +--> papers[]          hall attendance, one per paper they were marked at —
  |                               present / absent / unfair means / exempt, answer booklet
  |
  +--> ExamSchedule[]        one per paper: class + section + subject + component
  |        maximumMarks, passingMarks, date, time, room, invigilators
  |
  +--> StudentMark[]         one per child per paper — ONLY for a child who sat it
           |                   obtainedMarks, graceMarks, entry status
           |
           v
      ReportCard             the term's marks, snapshotted
        +--> ReportCardSubjectResult[]
                +--> ReportCardComponentResult[]   one per paper, across the term's exams

      HolisticProgressCard   the same term, in words rather than marks
        +--> DomainAssessment[]
        +--> ParentFeedback[]  one per guardian — can be added after publishing
```

`ReportCard` and `HolisticProgressCard` are **siblings**. A child has one of each per term —
see the [section at the end](#holisticprogresscard--holistic_progress_cards).

### Models in this package

| Model | Grain — one row per… |
|---|---|
| [Exam](Exam.java) | exam event, per school year |
| [ExamCandidate](ExamCandidate.java) | child allowed to sit, per exam — with their hall attendance inside |
| [ExamSchedule](ExamSchedule.java) | paper: subject + component, per section, within an exam |
| [StudentMark](StudentMark.java) | child, per paper they sat |
| [ReportCard](ReportCard.java) | child, per term, per version |
| [HolisticProgressCard](HolisticProgressCard.java) | child, per term, per version |
| [ExamCandidateStatusChange](embedded/ExamCandidateStatusChange.java) | embedded in a candidate — one debarment or return |
| [ExamPaperAttendance](embedded/ExamPaperAttendance.java) | embedded in a candidate — hall attendance at one paper, with its own `_id` |
| [ReportCardSubjectResult](embedded/ReportCardSubjectResult.java) | embedded in a report card — one subject |
| [ReportCardComponentResult](embedded/ReportCardComponentResult.java) | embedded in a subject result — one paper |
| [DomainAssessment](embedded/DomainAssessment.java) | embedded in a progress card — one area of development |
| [ParentFeedback](embedded/ParentFeedback.java) | embedded in a progress card — one guardian's view |

### Enums this package uses

All in [`../enums`](../enums).

| Enum | Values |
|---|---|
| `ExamStatus` | `DRAFT`, `SCHEDULED`, `PUBLISHED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `ExamType` | `UNIT_TEST`, `FORMATIVE`, `SUMMATIVE`, `MIDTERM`, `FINAL`, `PRACTICAL`, `PRE_BOARD`, `BOARD` |
| `ExamScheduleStatus` | `DRAFT`, `SCHEDULED`, `PUBLISHED`, `COMPLETED`, `CANCELLED` |
| `ExamCandidateStatus` | `ELIGIBLE`, `DEBARRED` |
| `ExamDebarmentReason` | `FEES_DUE`, `ATTENDANCE_SHORTAGE`, `DISCIPLINARY`, `DOCUMENTS_PENDING`, `OTHER` (needs a note) |
| `ExamAttendanceStatus` | `PRESENT`, `ABSENT`, `UNFAIR_MEANS`, `EXEMPT` — **the only record of whether a child sat a paper** |
| `MarkEntryStatus` | `DRAFT`, `SUBMITTED`, `LOCKED`, `PUBLISHED` (unused — a mark is published through its card) |
| `ResultStatus` | `PASS`, `FAIL`, `WITHHELD`, `INCOMPLETE` |
| `ReportCardStatus` | `DRAFT`, `REVIEWED`, `PUBLISHED`, `REVOKED` — both card types |
| `NepStage` | `FOUNDATIONAL`, `PREPARATORY`, `MIDDLE`, `SECONDARY` |
| `LearningDomain` | the five policy domains, then `DIGITAL_LITERACY`, `LEADERSHIP_AND_INITIATIVE`, `COLLABORATION`, `VALUES_AND_CITIZENSHIP` |
| `HpcLevel` | `STREAM`, `RIVER`, `MOUNTAIN`, `SKY` — no failing level |

### Models from other packages

| Model | Lives in | Used for |
|---|---|---|
| [AcademicTerm](../structure/AcademicTerm.java) | `academics/structure` | the term an exam and a card belong to; its `weightPercent` and `resultsLocked` |
| [SchoolClass](../structure/SchoolClass.java) | `academics/structure` | the class sitting a paper; `ClassSubject` gives the subject codes and a grading scheme |
| [GradingScheme](../grading/GradingScheme.java) | `academics/grading` | turning marks into a grade band |
| [Student](../../student/Student.java) | `student` | who sat the paper and whose card it is |
| [StudentAcademicRecord](../../student/StudentAcademicRecord.java) | `student` | **the child's class and section** — a candidate stores neither |
| [Guardian](../../student/Guardian.java) | `student` | who wrote each parent feedback on a progress card |
| [Staff](../../people/staff/Staff.java) | `people/staff` | invigilators, who marked, entered, locked, reviewed, published, revoked |
| [FacilityResource](../../facilities/FacilityResource.java) | `facilities` | the exam room |
| [AttendanceSession](../attendance/AttendanceSession.java) | `academics/attendance` | the attendance days copied onto both cards |
| [DocumentRecord](../../documents/DocumentRecord.java) | `documents` | question paper, evidence, the printed card |
| [AcademicYear](../../core/AcademicYear.java) | `core` | working days, `resultsLocked`, and the year every row is scoped to |

## When each one comes into play

```text
BEFORE THE EXAM

  weeks ahead   Exam created                      "Half Yearly, Term 1", status DRAFT
  ~2 weeks      ExamSchedule rows added           one per paper per section —
                                                  the datesheet
                Exam published                    the datesheet goes out
                Candidates added                  the children allowed to sit,
                                                  ELIGIBLE
                Some debarred                     fees due, attendance short… —
                                                  DEBARRED, with a reason; let
                                                  back in when it is settled

ON THE DAY, PER PAPER

  in the hall   the register lists the section's  a debarred child's name is not
                ELIGIBLE candidates              there, so no attendance can be
                                                  taken for them
  in the hall   attendance marked                 PRESENT / ABSENT / UNFAIR_MEANS /
                (ExamCandidate.papers)            EXEMPT, answer booklet recorded
  after         paper COMPLETED                   hall attendance closes; refused
                                                  while an eligible child is unmarked

AFTER THE PAPER

  marking       StudentMark rows written          only for children PRESENT;
                                                  blind evaluation by booklet number
  submit, lock  marks SUBMITTED, then LOCKED      no further edits without an unlock

END OF TERM

  compile       ReportCard generated              every paper of the term's exams
                                                  snapshotted, grades from the
                                                  scheme in force
  write         HolisticProgressCard written       by the class teacher, with the
                                                  child's and classmates' words
  review        both REVIEWED                     a progress card is checked here
  publish       both PUBLISHED                    families can see them; neither is
                                                  edited afterwards — except that a
                                                  guardian can still add their own
                                                  feedback to the progress card
```

Two orderings matter and are enforced rather than assumed:

- **Attendance before marks.** A mark can only be written for a child whose hall entry is
  `PRESENT`, and only once the paper is `COMPLETED`. Hall entries are not created ahead of time;
  instead a paper cannot be completed while an eligible candidate is unmarked, so nobody is
  silently missing. Section 5 covers what must agree.
- **Lock before publish.** A report card is a snapshot; snapshotting marks that can still
  change produces a card that disagrees with the source a week later. Section 8 covers the
  order.

## 1. Collections and their grain

```text
AcademicTerm (academics/structure)
└── Exam                       one examination event, one term
    ├── ExamCandidate          one per child allowed to sit
    │   └── papers[]           embedded, one per paper  <-- hall presence + answer booklet
    └── ExamSchedule           one class + section + subject + component
        └── StudentMark        one per child who sat it  <-- evaluated result
```

**`Exam.name` is the exam's only natural key** — unique within the school's year
(`school_year_exam_name_uniq`), checked case-insensitively by the service. There is no exam
code: everything references an exam by `examDocsId`.

**`ExamSchedule` is the register header.** Its rows are the entries for that paper in
`ExamCandidate.papers` — the same relation `StudentAttendanceRecord` has to
`AttendanceSession` — and no separate exam-attendance session document is needed: the schedule
already owns the date, time, room, invigilators and status.

**Hall attendance is embedded in the candidate.** There is no `exam_attendances` collection.
Only a candidate can be marked, eligibility and attendance are read and written in one
document, and a child's whole exam is one read. A child sits a handful of papers, so the list
stays small. Each entry has its own `_id`, generated by the service, because MongoDB does not
make one for an embedded entry.

**Marks are a separate collection on purpose.** Attendance and marks are written by different
people at different times: the invigilator records presence in the hall, and the evaluator
enters marks days later. Keeping `StudentMark` apart keeps mark entry independent of attendance
and makes blind evaluation possible.

**Nothing is copied that a reference already gives.** A candidate stores no class, section or
subject; a mark stores no subject or component. Both point at what holds them — the academic
record and the paper. The two cards are the exception, and on purpose: they are snapshots
(section 9).

### Indexes

| Collection | Index | Serves |
|---|---|---|
| `exams` | `school_year_exam_name_uniq` — unique | one name per year |
| | `school_year_exam_status_start_idx` | the year's exams by status and date |
| | `school_term_exam_type_idx` | a term's exams by type |
| `exam_candidates` | `school_exam_student_candidate_uniq` — unique | one candidate per child per exam |
| | `school_exam_candidate_debarment_idx` | the debarred, by reason — the fee defaulters |
| | `school_exam_paper_attendance_idx` | one paper's hall entries and counts |
| | `school_exam_answer_copy_no_uniq` — unique, partial | one booklet per exam |
| | `school_student_exam_candidate_history_idx` | a child's exams over the years |
| `exam_schedules` | `school_exam_class_section_subject_component_uniq` — unique | one paper per section, subject and component |
| | `school_exam_date_status_idx` | the datesheet |
| | `school_invigilator_exam_date_idx` | an invigilator's duties, and clashes |
| | `school_exam_schedule_room_idx` — partial | a room's papers, and clashes |
| `student_marks` | `school_exam_schedule_student_mark_uniq` — unique | one mark per child per paper |
| | `school_exam_student_marks_idx` | one child's marks in an exam |
| | `school_exam_mark_entry_queue_idx` | which papers still need marks |
| `report_cards` | `school_year_student_term_report_version_uniq` — unique | one card per child, term and version |
| | `school_year_report_status_published_idx` | published cards |
| | `school_student_report_history_idx` | a child's cards over the years |
| | `school_term_class_section_report_idx` | a section's cards for a term |
| `holistic_progress_cards` | `school_year_student_hpc_term_uniq` — unique | one card per child, term and version |
| | `school_term_class_section_hpc_idx` | a section's cards for a term |
| | `school_year_hpc_status_idx` | cards by status |

## 2. Weighting

Weighting has exactly two levels and no join table:

```text
Exam.weightPercent          share of the term result
AcademicTerm.weightPercent  share of the annual result
```

Both are optional. When null, aggregate raw marks. Services must validate that the active exam
weights within a term sum to 100, and that active term weights within the year sum to 100,
whenever weighting is used at all — checked when a card is generated, because exams are added
one at a time. Never mix weighted and unweighted exams inside the same term.

## 3. Answer copy accountability

- `papers.answerCopyNo` is unique within one `Exam`, enforced by
  `school_exam_answer_copy_no_uniq` on `ExamCandidate`. A unique index on an array field is
  enforced across documents, so the same physical booklet cannot be recorded against two
  students. It is **not** enforced inside one document: the service must refuse one child
  holding the same number on two papers.
- Schools that restart numbering per subject must prefix the value, for example `"MATH-0001"`,
  otherwise distinct booklets collide inside one exam.
- `additionalAnswerCopyNos` holds supplementary booklets. MongoDB cannot extend the unique
  index across that list, so the service must check every submitted supplementary number
  against both `papers.answerCopyNo` and `papers.additionalAnswerCopyNos` across the exam's
  candidates before saving.
- `answerCopyNo` must be present when the status is `PRESENT` or `UNFAIR_MEANS`, and empty when
  it is `ABSENT` or `EXEMPT`. The partial index ignores empty values so those entries do not
  collide.
- An issued booklet number is not changed casually. A wrongly recorded one is corrected by
  updating the candidate under its expected `version`, and the correction is visible through
  the inherited audit fields.

## 4. Late arrival and early departure

These are derived, not stored as states:

```text
reportedAt  > ExamSchedule.startTime  -> admitted late
submittedAt < ExamSchedule.endTime    -> left early
```

`ExamAttendanceStatus` is therefore `PRESENT`, `ABSENT`, `UNFAIR_MEANS` and `EXEMPT` — and it
is **the only record of whether a child sat a paper**. There is no participation status on
`StudentMark`: it only repeated this.

## 5. Who sits, and attendance and marks must agree

- **Only a candidate can have hall attendance** — it lives inside the candidate. A paper's hall
  register is the section's children on the paper's date (from `StudentAcademicRecord`) who are
  `ELIGIBLE` candidates of the exam. A child who is not a candidate, or is `DEBARRED`, does not
  appear and gets no `papers` entry.
- **Every candidate is expected at every paper of their section.** Which child takes which
  elective is a year-long choice and belongs on `StudentAcademicRecord`, not here; until it
  exists, a child who does not take an elective is marked `EXEMPT` in the hall.
- **Debarring is not retroactive.** A paper already sat keeps its `papers` entry. Debarring
  removes the child from the papers still to come; letting them back in returns them to every
  paper not yet `COMPLETED`. Every change is kept in `statusChanges`, with the reason.
- The candidate's `papers` entry is the **only** record of whether a child appeared. Nothing is
  copied into `StudentMark`.
- **A `StudentMark` exists only for an entry that is `PRESENT`.** Reject a mark for any other
  child. `ABSENT`, `UNFAIR_MEANS` (result withheld) and `EXEMPT` are read from the entry.
- **A paper held while a child was debarred** has no `papers` entry and no `StudentMark`. A card
  reads the candidate's `statusChanges` against the paper's date and shows "debarred" rather
  than a gap or "absent".
- Attendance closes when the owning `ExamSchedule` reaches `COMPLETED`. There is no per-entry
  lock field.

## 6. Blind evaluation

Because the booklet number lives on `ExamCandidate.papers` and not on `StudentMark`, an
evaluator can be given a booklet list with no student identity. The service resolves
`answerCopyNo` to `studentDocsId` through the candidate and writes the `StudentMark` row
itself. Do not add `answerCopyNo` to `StudentMark`; that would defeat the separation.

## 7. Grace marks and corrections

`obtainedMarks` is the evaluator's award and is never overwritten by moderation. An adjustment
is recorded in `graceMarks` with a `graceReason`, so the effective mark is:

```text
effectiveMarks = obtainedMarks + graceMarks
```

A revaluation reopens the row: unlock, edit, relock. The inherited audit fields and `version`
carry the history; there is no separate revaluation collection.

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

Mark submission, locking, report generation and publication require transactions or
idempotent workflows.

## 9. Report cards are snapshots

A published `ReportCard` must be reprintable years later without reading any other
collection. That is why it copies in `termName`, `classDocsId`, `className`, `sectionNo`,
`rollNo`, the attendance day counts, and the per-paper breakdown.

- **One component per paper, across every exam in the term.** A term card can combine a unit
  test and the half-yearly, both with a Maths paper, so each `ReportCardComponentResult`
  records its `examDocsId`, `examName` and `weightPercent`.
- **A component says why it has no mark.** `obtainedMarks` is the effective mark (award plus
  grace) with `graceMarks` beside it; when there is no mark, `attendanceStatus` and `debarred`
  say why, so the card prints "absent", "exempt", "withheld" or "debarred" rather than a blank.
- **Grades record the scheme that made them** — each subject its own
  `gradingSchemeDocsId`, and the card one for `overallGradeCode`.
- **`overallPercentage` is stored, `attendancePercentage` is not.** With exam weights the
  overall is not a plain division, and it is the number the grade and result were decided
  from. The attendance percentage is just the two stored day counts divided.
- Never recalculate a published card in place. A correction revokes it — `revocationReason`,
  `revokedAt`, `revokedByDocsId` — and creates the next `reportVersion`. Review is recorded too:
  `reviewedAt`, `reviewedByDocsId`.
- `classRank` and `rankedStudentCount` are frozen at publication. A rank computed on read would
  drift as other students' marks change.
- `countsTowardTotal` records whether a subject was included in the totals for this snapshot.
  It is separate from `subjectType`, which only groups rows on the printed card.
- Grade-only subjects store `gradeCode` with every marks field left null.

## 10. Validation responsibility

The models carry only structural constraints. DTOs and services validate tenant ownership,
that `termDocsId` belongs to the same school and academic year, that `classDocsId`,
`sectionNo` and `subjectCode` resolve inside the referenced `SchoolClass`, date and time
ordering, marks within `0..maximumMarks`, grading band coverage, booklet uniqueness including
supplementary booklets, weight totals, status transitions, candidate eligibility, and
publication authorization.

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

Things worth knowing:

- **`DomainAssessment.observation` is the card.** `level` is almost a footnote beside it.
  "RIVER in Language and Literacy" tells a parent nothing they can act on; the observation
  does. A card of bare levels is a report card with nicer words.
- **A card starts empty and is checked at review, not on every save.** It is filled in over
  days — often a level first and the observation later — so the model does not demand domains
  or observations. Before a card can be `REVIEWED`, the service requires the five policy
  domains, an observation on every domain, and no domain listed twice.
- **`nepStage` is stored, not derived.** A class is renamed and reorganised over the years;
  the stage a child was in does not change after the fact.
- **`peerFeedback` is summarised, never quoted.** A card handed to a family must not become a
  place where one child's words about another are repeated back.
- **`parentFeedbacks` is one entry per guardian**, each with who wrote it and when — and it is
  **the one part that can be added to after publishing**, without a new version. Families write
  their view after they have seen the card; nothing the school wrote changes when they do.
- **`assessedByStaffDocsId` is optional.** It starts as the section's class teacher and becomes
  whoever writes the card; an admin with no staff record can still create cards.

Versioning, snapshotting and publication otherwise work exactly as they do for `ReportCard`: a
published card is never edited, a correction is `cardVersion + 1` with the old one revoked
(`revocationReason`, `revokedAt`, `revokedByDocsId`), review is recorded (`reviewedAt`,
`reviewedByDocsId`), and class, section and roll number are copied in so a reprint shows the
class the child was actually in.

## Decisions taken 2026-10-16

| Change | Why |
|---|---|
| `Exam.examCode` removed; `name` unique in the year | Nothing joined on the code — the same decision as `AcademicTerm.termCode`. |
| `ExamCandidate` added | A school must be able to stop a child from sitting (fees, attendance, discipline), and a debarred child's name must not be on the hall register. |
| Candidate stores no class, section or subjects | They come from the academic record and the paper; copies would go stale when a child moves section. |
| `ExamAttendance` collection removed — attendance embedded as `ExamCandidate.papers` | Only a candidate can be marked, and eligibility and attendance live in one document. |
| `ExamPaperAttendance` has its own `_id` | So one entry can be addressed and corrected on its own. |
| `ExamParticipationStatus` and `StudentMark.participationStatus` removed; `EXEMPT` added to `ExamAttendanceStatus` | Every value repeated the hall status or the candidate's debarment. A mark now exists only for a child who sat. |
| `StudentMark.subjectCode` and `componentCode` removed | Copies of the paper the mark already points at. |
| `ReportCardComponentResult` gains exam, weight, grace, hall status and `debarred` | A term card combines exams, and a missing mark must say why. |
| `ReportCardSubjectResult.gradingSchemeDocsId` added | Subjects can be graded by different schemes. |
| `ReportCard.attendancePercentage` removed; review and revoke fields added | Derived from two stored counts; review and revocation need recording. |
| `HolisticProgressCard`: drafts may be empty, `assessedByStaffDocsId` optional, review and revoke fields, indexes fixed | A card is filled in over days; checks belong at review. |
| `LearningDomain.OTHER` removed | A domain appears once and `OTHER` has no label; detail belongs in the observation. |
| `ParentFeedback` list replaces `parentFeedback` and its guardian id | Each guardian writes their own view, and it can arrive after the card is published. |
