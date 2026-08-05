# Academics domain model

## Package structure

```text
academics/
├── structure/
│   ├── SchoolClass.java
│   └── embedded/
│       ├── ClassSection.java
│       └── ClassSubject.java
├── curriculum/
│   └── CurriculumDocument.java
├── grading/
│   ├── GradingScheme.java
│   └── embedded/GradeBand.java
├── timetable/
│   ├── DailyTimetable.java
│   └── embedded/TimetableEntry.java
├── homework/
│   ├── Homework.java
│   └── HomeworkSubmission.java
├── attendance/
│   ├── AttendanceSession.java
│   └── StudentAttendanceRecord.java
├── examination/
│   ├── Exam.java
│   ├── ExamSchedule.java
│   ├── StudentMark.java
│   ├── ReportCard.java
│   └── embedded/ReportCardSubjectResult.java
└── enums/
```

All top-level documents extend `SchoolBase`. Every reference lookup must also
include `schoolId`. Every `academicYear` field stores `AcademicYear.name`, such
as `"2026-2027"`; it never stores `AcademicYear.id`.

## Main relationship map

```text
AffiliationProgramme (optional)
└── SchoolClass[] (per AcademicYear.name)
    ├── ClassSection[] (embedded; referenced by sectionCode)
    ├── ClassSubject[] (embedded; referenced by subjectCode)
    ├── CurriculumDocument[] ──> DocumentRecord
    ├── StudentAcademicRecord[]
    ├── Homework[]
    │   └── HomeworkSubmission[] (per student and attempt)
    └── AttendanceSession[]
        └── StudentAttendanceRecord[] (per student)

School + date
└── DailyTimetable (one document)
    └── TimetableEntry[] (all class and section periods for that date)

GradingScheme
├── ClassSubject (optional default scheme)
└── Exam (optional default scheme)
    └── ExamSchedule[] (class/section/subject/component)
        └── StudentMark[]

StudentMark[] + attendance summary
└── ReportCard (versioned published snapshot)
```

## Structure

`SchoolClass` is the only top-level structure collection. Its bounded
`ClassSection` and `ClassSubject` lists are embedded. This keeps class setup
simple and allows the class, its sections, subjects, and teacher assignments to
be created or loaded in one operation.

`StudentAcademicRecord.classDocsId` references `SchoolClass.id` and
`StudentAcademicRecord.sectionCode` references an embedded
`SchoolClass.sections[].sectionCode`. Other academic documents use
`subjectCode` and `sectionCode`; embedded records do not have MongoDB document
IDs. Services must keep those codes unique inside a class and must not rename a
code after dependent records exist.

For a class-wide subject, `ClassSubject.sectionCode` is null. When different
sections have different teachers, repeat that subject with the appropriate
section code and teacher list. Services must enforce uniqueness of
`(subjectCode, sectionCode)` inside one SchoolClass.

## Curriculum documents and grading

`CurriculumDocument` is a lightweight publishing record created by a school
department for one `classDocsId` and embedded `subjectCode`. It stores the
academic year, department, title, document version, and draft/published state.

The actual PDF, image, DOCX, or other file is stored in object storage through
`DocumentRecord`; MongoDB stores only `documentDocsId`. A revised file creates
the next `documentVersion`. Publish the new version and archive the old version
instead of overwriting historical curriculum files.

`GradingScheme` embeds a small ordered list of `GradeBand` values. Services must
reject overlapping bands, gaps that are not intentional, invalid boundaries,
and changes to a scheme already used for published results. Create a new scheme
version instead of rewriting historical grading rules.

## Timetable

`DailyTimetable` is the complete timetable of one school for one calendar date.
Its embedded entries contain every class and section period for that day. The
unique `schoolId + date` index guarantees that a school cannot have two daily
timetable documents for the same date. Holidays and weekly offs do not require
a document.

An entry has its own service-generated `entryId`, allowing one embedded period
to be updated with MongoDB array filters. A teacher substitution is handled by
changing that date's entry, so a separate substitution collection is not used.
The inherited optimistic-lock version prevents concurrent whole-document edits
from silently overwriting each other.

Services must validate unique entry IDs, class and section ownership, subject
and teacher assignments, time ordering, and overlapping periods before saving
the complete daily document.

## Homework

`Homework` stores assignment instructions and targeting. `HomeworkSubmission`
stores one student's numbered attempt. Submission counts are calculated with a
count query and are not persisted on `Homework`. Files reference private
`DocumentRecord.id` values; URLs are never stored.

For `HomeworkScope.SELECTED_STUDENTS`, `targetStudentDocsIds` contains the small
selected set. Class and section assignments use `classDocsId` and `sectionCode`
and do not copy an entire roster into the homework document.

## Attendance

`AttendanceSession` is the register header for daily or period attendance.
`StudentAttendanceRecord` is one student row. This split allows concurrent
updates and efficient student history without growing a single class document.

The student row stores both `attendanceSessionDocsId` and
`studentAcademicRecordDocsId`. Services must copy `attendanceDate` and
`academicYear` from the session, obtain the student from the academic record,
and reject students outside that session's class/section. Locking a session and
making final row changes should be transactional. Staff attendance and hostel
night roll calls belong to their own people and hostel modules.

A period attendance session may optionally link to `DailyTimetable.id` through
`dailyTimetableDocsId` and to one embedded entry through `timetableEntryId`.

## Examinations and report cards

`Exam` is the overall examination. `ExamSchedule` is one class/section subject
component, such as theory or practical. `StudentMark` stores one student's mark
for that component. The schedule owns maximum and passing marks; the student
mark stores the obtained value and participation state.

`ReportCard` is a versioned snapshot generated from selected exams, grading
rules, and attendance. Once published, do not recalculate it in place. A
correction creates the next `reportVersion`; the generated PDF, if any, is a
`DocumentRecord` reference.

Mark submission, locking, publication, report generation, and report
publication require transactions or idempotent workflows. The service must
ensure `AcademicYear.resultsLocked` is respected.

## Mapping from the reference models

- `CourseOffering`, standalone subjects, and standalone sections became
  embedded `ClassSubject` and `ClassSection` values inside `SchoolClass`.
- `ScheduleDefinition`, `ScheduleOccurrence`, and `SubstitutionAssignment` were
  replaced by one date-specific `DailyTimetable` containing embedded entries.
- `LearningActivity` and `LearnerSubmission` are represented by `Homework` and
  `HomeworkSubmission` for the current ERP scope.
- `AttendanceSession` and `StudentAttendanceRecord` were retained with school
  tenancy and `AcademicYear.name` conventions.
- `AssessmentSession` was separated into `Exam` and `ExamSchedule`.
- `AssessmentAttempt`, `GradebookRecord`, and the old `AcademicResult` were
  consolidated into `StudentMark`, while published summaries use `ReportCard`.
- `CurriculumFramework`, `CurriculumUnit`, and `LearningOutcome` were replaced
  by one document-driven `CurriculumDocument`. `ReportCard` was retained in a
  simplified form.

## Intentionally deferred or moved

- `AssessmentBankItem`: defer until a question-bank and online-exam module is
  designed.
- `AccommodationPlan`: belongs to a future student inclusion/support module.
- `ScheduleConstraint`: add later with automated/AI timetable generation.
- Generic LMS activities, discussions, plagiarism checks, rubrics, and virtual
  classes: design in a future learning/LMS module; homework remains here.
- `MedicalRecord`: moved out of academics; design it in the health module.
- `DisciplineLog`: moved out of academics; design it in the conduct module.
- Promotion/rollover does not need a duplicate result model. It closes the old
  `StudentAcademicRecord`, creates the next year's record, and updates
  `Student.currentAcademicRecordDocsId` transactionally. Add a separate batch
  audit model only when the rollover workflow is implemented.

## Validation responsibility

Models contain only essential persistence requirements. DTOs and services must
validate tenant ownership, immutable academic-year names, date/time ordering,
class-section-code and subject-code relationships, teacher assignments, timetable collisions,
homework targeting, score ranges, grading bands, state transitions, publication
authorization, and cross-document consistency.
