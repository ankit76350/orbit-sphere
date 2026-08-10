# Shared naming conventions

This file holds conventions that cross package boundaries in the `new_new`
collection design. Package-specific mapping is documented in each package's own
README.

Two stable business keys are referenced by many collections as plain strings
rather than by document id. Both are immutable after creation:

| Key | Defined on | Stored by other documents as |
|---|---|---|
| `sectionNo` | `ClassSection`, embedded in `SchoolClass` | `sectionNo` |
| `name` | `AcademicYear` | `academicYear` |

## Section identifier — `sectionNo`

A class section is always identified by `sectionNo`. The names `sectionCode` and
`sectionName` must not be used anywhere in this design, including new models
added later.

```text
SchoolClass (collection)
└── sections[]  (embedded ClassSection)
        └── sectionNo   <-- the only section identifier
```

`sectionNo` is defined once, on the embedded `ClassSection`, and stored as a
plain string by every document that refers to a section. Embedded values have no
MongoDB document id, so there is nothing else to reference them by.

### Single identifier, no separate display name

`ClassSection` intentionally has no `name` field. `sectionNo` is both the stable
reference and the value shown in the UI:

```text
sectionNo = "A"   ->  displayed as  "A"
```

A longer label such as `"Section A - Pioneers"` is not stored. If a school ever
needs one, it must be added as a new optional field beside `sectionNo`; it must
never replace `sectionNo` as the reference used by other documents.

### Where it is stored

| Document | Field | Required | Meaning when null |
|---|---|---|---|
| `ClassSection` (embedded in `SchoolClass`) | `sectionNo` | yes | — defines the section |
| `ClassSubject` (embedded in `SchoolClass`) | `sectionNo` | no | subject applies to all sections of the class |
| `StudentAcademicRecord` | `sectionNo` | no | student is placed in the class without a section |
| `Homework` | `sectionNo` | no | homework applies to the whole class |
| `ExamSchedule` | `sectionNo` | yes | — a paper is always scheduled per section |
| `AttendanceSession` | `sectionNo` | yes | — a register is always section-specific |
| `TimetableEntry` (embedded in `DailyTimetable`) | `sectionNo` | yes | — a period is always section-specific |
| `ReportCard` | `sectionNo` | no | placement snapshot; null when the student had no section |

Every one of these is resolved as `schoolId + classDocsId + sectionNo`. A
`sectionNo` is only meaningful inside its owning `SchoolClass`; the same value
`"A"` in two different classes refers to two different sections.

### Indexes that contain `sectionNo`

```text
StudentAcademicRecord  school_year_class_section_active_roll_uniq   (unique)
StudentAcademicRecord  school_year_class_section_roster_idx
Homework               school_year_class_section_homework_idx
AttendanceSession      school_attendance_session_uniq               (unique)
AttendanceSession      school_attendance_date_status_idx
ExamSchedule           school_exam_class_section_subject_component_uniq (unique)
ReportCard             school_term_class_section_report_idx
```

Renaming the field would silently invalidate these index definitions, which is
part of why the name is fixed.

### Rules

1. **Unique within the owning class.** Two `ClassSection` entries in the same
   `SchoolClass` must not share a `sectionNo`. MongoDB does not enforce
   uniqueness inside an embedded array, so the service must check it before
   every write to `SchoolClass.sections`.
2. **Immutable once referenced.** `sectionNo` is a stable business key, like
   `AcademicYear.name`. Six collections store it as a plain string with no
   foreign key, so a rename cannot be repaired by updating one document. Reject
   rename requests, or handle them as a coordinated migration across every
   collection in the table above.
3. **Deleting a section is not a plain array `$pull`.** Placements, homework,
   attendance registers, exam schedules and timetable entries may still point at
   it. Deactivate with `ClassSection.active = false` instead, and only remove the
   entry once no dependent record references it.
4. **Never trusted from a request.** A submitted `sectionNo` must be verified to
   exist in the referenced `SchoolClass` — inside the same `schoolId` and
   `academicYear` — before the write is accepted.
5. **Null means "not section-specific", not "unknown".** Where the field is
   optional, null carries the meaning given in the table above. Do not use an
   empty string to mean the same thing; blank values must be normalized to null
   so they behave consistently in queries and partial indexes.

## Academic year identifier — `AcademicYear.name`

`AcademicYear.name` is **not editable once the document is created.**

```java
// core/AcademicYear.java
@NotBlank
@Setter(AccessLevel.NONE)
private String name;          // "2026-2027"
```

### Why it cannot change

The system never stores `AcademicYear.id` in a child record. It always stores
the name:

```text
AcademicYear
  schoolId = "67aa15d9dc3f7d0033333333"
  name     = "2026-2027"      <-- the reference value
       |
       v
child documents
  schoolId      = "67aa15d9dc3f7d0033333333"
  academicYear  = "2026-2027"
```

There is no foreign key and no cascade. Changing `name` after creation would
orphan every document that already copied the old value, across 19 collections
and 29 compound indexes, with no way to detect the break at write time. The
lookup pair is always:

```text
schoolId + academicYear name
```

`name` is therefore unique within one school, enforced by the
`school_year_name_uniq` index on `{schoolId, name}`.

### Where the value is copied

Six collections inherit `academicYear` from `AcademicStudentSchoolBase`:

```text
StudentAcademicRecord   HomeworkSubmission   StudentAttendanceRecord
StudentMark             ReportCard           ExamAttendance
```

Thirteen more declare it directly:

```text
SchoolClass      AcademicTerm         CurriculumDocument  DailyTimetable
Homework         AttendanceSession    Exam                ExamSchedule
Inquiry          AdmissionCycle       StaffLeaveBalance   StaffLeaveRequest
ReviewCycle
```

### Rules

1. **Reject any update that changes `name`.** The update service must compare the
   submitted name against the stored one and fail the request; it must not
   silently ignore the field. Every other `AcademicYear` field — dates,
   `holidays`, `enrollmentEnabled`, `resultsLocked` — remains editable.
2. **`@Setter(AccessLevel.NONE)` is a hint, not enforcement.** Lombok suppresses
   `setName(...)`, but `@SuperBuilder`, `@AllArgsConstructor` and Spring Data's
   reflective mapping can all still write the field. The real guarantee has to
   live in the service layer.
3. **Never derive it from a request.** Where a document's academic year follows
   from a date — `DailyTimetable`, `AttendanceSession` — the service resolves the
   year from the date against the school's `AcademicYear` documents rather than
   trusting a submitted string.
4. **A mistyped year is fixed by recreating, not renaming.** If no child document
   references it yet, delete and recreate. If any does, treat it as a coordinated
   migration across all 19 collections.
5. **Rollover creates a new document.** Moving to the next year never edits the
   existing one; it creates `AcademicYear` for the new name and closes the
   previous year's records.

## Validation responsibility

The models carry only the structural constraints (`@NotBlank`, `@NotNull`).
Request DTOs and services validate:

**For `sectionNo`**

- format and length;
- uniqueness inside the owning `SchoolClass`;
- existence and tenant/academic-year ownership of the referenced section;
- rename and deletion rules described above;
- normalization of blank values to null on optional fields.

**For `AcademicYear.name`**

- rejection of any rename attempt on update;
- format of the name and uniqueness within the school;
- that a referenced `academicYear` exists for that `schoolId` before a child
  document is written;
- that a date-derived `academicYear` is resolved server-side, not accepted from
  the client.
