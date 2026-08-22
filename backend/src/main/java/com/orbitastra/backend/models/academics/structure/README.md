# Academic structure collection model mapping

This package defines the academic skeleton of one school year: the reporting
periods it is divided into, and the classes taught inside it.

## Package contents

```text
structure/
├── AcademicTerm.java          collection: academic_terms
├── SchoolClass.java           collection: school_classes
└── embedded/
    ├── ClassSection.java      embedded in SchoolClass.sections
    └── ClassSubject.java      embedded in SchoolClass.subjects
```

Both top-level documents extend `SchoolBase`, so every lookup and reference check
must include `schoolId`. Every `academicYear` field stores `AcademicYear.name`
such as `"2026-2027"`; it never stores `AcademicYear.id`.

## Relationship

```text
AcademicYear   name = "2026-2027"
  |
  ├── AcademicTerm[]     how the year is divided for reporting
  |     └── Exam[]  ──>  ReportCard[]
  |
  └── SchoolClass[]      what is taught in the year
        ├── ClassSection[]  (embedded, referenced by sectionNo)
        └── ClassSubject[]  (embedded, referenced by subjectCode)
```

`AcademicTerm` and `SchoolClass` are independent. A term does not contain
classes, and a class does not belong to a term — a class runs for the whole year
while terms slice that year into reporting periods.

## AcademicTerm — `academic_terms`

One named reporting period of an academic year. This is the unit a report card is
issued for.

```text
AcademicYear  "2026-2027"   2026-04-01 .. 2027-03-31
  ├── termCode "TERM_1"  sequence 1  2026-04-01..2026-09-30  weightPercent 20
  └── termCode "TERM_2"  sequence 2  2026-10-01..2027-03-31  weightPercent 80
```

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `academicYear` | `AcademicYear.name`, for example `2026-2027`. |
| `termCode` | Stable school-scoped key, unique with `schoolId + academicYear`. |
| `name` | Display name copied into report-card snapshots. |
| `sequence` | Order inside the year; unique with `schoolId + academicYear`. |
| `startDate` | First date of the period. |
| `endDate` | Last date of the period. |
| `weightPercent` | Share of the annual result; null means no annual weighting. |
| `resultsLocked` | Blocks result changes for this period only. |
| `active` | Whether the term is in use. |

### Why this is a collection and not a string

`Exam` and `ReportCard` previously stored a free-text `reportingPeriodName`, and
that value sat inside the report-card uniqueness key:

```text
{schoolId, academicYear, studentDocsId, reportingPeriodName, reportVersion}
```

Nothing could validate it, because there was no list of valid periods to check
against. `"Term 1"` on one exam and `"Term-1"` on another produced two separate
report-card lineages for the same student and the same real period, both at
`reportVersion = 1`, with no error raised. Referencing a term by `termDocsId`
removes that class of bug: an unknown id fails, and renaming a term does not
orphan the exams and cards that point at it.

Reporting periods also differ per school, which is why this cannot be a Java
enum:

```text
CBSE school     Term 1, Term 2
IB school       Semester 1, Semester 2
State board     four quarters
Another school  three trimesters plus Annual
```

An enum would be the union of every school's structure, visible to all schools,
and onboarding a school with a new structure would need a code deploy. A
school-scoped configuration collection is the same pattern already used by
`LeaveType`, `Department`, `Position`, `GradingScheme`, and
`AffiliationProgramme`.

### Where the term is used

| Consumer | Field | Purpose |
|---|---|---|
| `Exam` | `termDocsId` | Which reporting period the exam counts towards. |
| `ReportCard` | `termDocsId` | Which period the card reports; part of its uniqueness key. |
| `ReportCard` | `termName` | Snapshot so a later rename cannot alter an issued card. |
| Portal and print order | `sequence` | Correct term ordering; alphabetical sorting of names is unreliable. |
| Result aggregation | `weightPercent` | Annual result from term results. |
| Result locking | `resultsLocked` | Freeze one period while another is still being marked. |

### Weighting

Weighting has exactly two levels and needs no join table:

```text
Exam.weightPercent          share of its term's result
AcademicTerm.weightPercent  share of the annual result
```

Both are optional. When null, aggregate raw marks. Services must validate that
active exam weights inside a term sum to 100 and active term weights inside the
year sum to 100 whenever weighting is used at all, and must not mix weighted and
unweighted exams in one term.

### Rules

1. **`termCode` is immutable once referenced.** Exams and report cards resolve
   the term by id, so the code may be corrected while nothing references it, but
   treat a later change as a coordinated update.
2. **`sequence` is unique inside the year.** Two terms must not claim the same
   position.
3. **Terms must not overlap** and should be contained inside the owning
   `AcademicYear` date range. MongoDB cannot express this; the service must.
4. **Deleting a term is not safe once used.** Set `active = false` instead, and
   only remove it when no exam or report card references it.
5. **`AcademicYear.resultsLocked` overrides `AcademicTerm.resultsLocked`.** The
   year-wide flag is the stronger control; a result write must satisfy both.

## SchoolClass — `school_classes`

One grade or class configured for one academic year. Its bounded section and
subject lists are embedded so class setup can be created or loaded in one
operation.

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `academicYear` | `AcademicYear.name`. |
| `name` | Display name, for example `Grade 7`. |
| `affiliationProgrammeDocsId` | Optional link to `AffiliationProgramme.id`. |
| `displayOrder` | Sorting order used by the UI. |
| `sections` | Embedded `ClassSection` values. |
| `subjects` | Embedded `ClassSubject` values. |
| `active` | Whether the class is in use. |

### Open item — `classCode`

The `school_year_class_code_uniq` index is defined on
`{schoolId, academicYear, classCode}`, but `SchoolClass` declares no `classCode`
field. Every document would therefore index a missing value and collide, which
allows only one class per school per academic year.

This must be resolved before the collection is used: either add a `classCode`
business key, matching `termCode`, `programmeCode`, and `examCode`, or repoint
the index at `name`.

## ClassSection — embedded

Stored inside `SchoolClass.sections`. It has no collection, no id, and no
`schoolId`; it inherits all of those from its parent.

| Field | Meaning |
|---|---|
| `sectionNo` | The only section identifier, and also the display value. |
| `classTeacherDocsId` | Optional link to the class teacher's `Staff.id`. |
| `capacity` | Maximum planned student count. |
| `active` | Whether the section is in use. |

`sectionNo` is a stable business key stored as a plain string by six other
collections. The shared rules for it — uniqueness inside the owning class,
immutability once referenced, and deactivation instead of deletion — are in
`models/README.md` and apply here.

## ClassSubject — embedded

Stored inside `SchoolClass.subjects`.

| Field | Meaning |
|---|---|
| `subjectCode` | Stable reference within the class. |
| `name` | Display name. |
| `shortName` | Abbreviated label. |
| `subjectType` | `SubjectType` classification. |
| `sectionNo` | Optional `ClassSection.sectionNo`; null means all sections. |
| `teacherDocsIds` | Assigned `Staff.id` values. |
| `gradingSchemeDocsId` | Optional `GradingScheme.id` for this subject. |
| `active` | Whether the assignment is in use. |

For a class-wide subject, `sectionNo` is null. When sections have different
teachers, repeat the same `subjectCode` with each section code and its own
teacher list. Services must enforce uniqueness of `(subjectCode, sectionNo)`
inside one `SchoolClass`.

Grading scheme resolution for a subject is:

```text
ClassSubject.gradingSchemeDocsId  ->  Exam.gradingSchemeDocsId  ->  none
```

### Why sections and subjects are embedded but terms are not

Sections and subjects exist only inside one class, are always written with it,
and are small and bounded. Terms are referenced by `Exam` and `ReportCard` in
other packages, need their own uniqueness and ordering guarantees, and carry a
lock flag that is updated independently of any class. Embedded values cannot be
referenced by id, which is exactly what those consumers require.

## Missing upstream: per-student subject choice

`ClassSubject` records that a class offers Physics and Biology. Nothing records
that a specific student takes Physics. Attendance registers, exam attendance
rows, and mark rows generated from a section roster will therefore include
students who do not sit an elective subject.

A subject list on `StudentAcademicRecord`, or a small
`StudentSubjectEnrollment` collection, is required before elective mark entry can
be generated correctly. It belongs to the student package rather than here.

## Validation responsibility

The models carry only structural constraints. Request DTOs and services validate:

- code and name formats and length limits;
- term date ordering, non-overlap, and containment inside the academic year;
- term sequence and weight totals;
- uniqueness of `sectionNo` and `(subjectCode, sectionNo)` inside a class;
- existence and tenant ownership of referenced staff, grading scheme, programme,
  and academic-year documents;
- rename and deactivation rules for codes that other collections already store;
- authorization for result locking.

MongoDB indexes and collection validators should be deployed through controlled
database migrations. Java annotations alone do not enforce the stored schema.
