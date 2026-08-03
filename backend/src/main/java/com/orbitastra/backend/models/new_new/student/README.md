# Student domain model mapping

## Package contents

```text
student/
├── Student.java
├── Guardian.java
├── StudentAcademicRecord.java
├── embedded/
│   └── GuardianLink.java
└── enums/
    ├── StudentStatus.java
    └── AcademicRecordStatus.java
```

Gender and GuardianRelation are shared from `new_new/common/enums` instead of
being duplicated inside the student package.

## Relationship

```text
AdmissionApplication (optional)
└── Student
    ├── GuardianLink[] (embedded)
    │   └── guardianDocsId -> Guardian.id
    ├── profilePhotoDocumentId -> DocumentRecord.id
    └── StudentAcademicRecord[]
        ├── academicYear -> AcademicYear.name
        ├── classDocsId  -> future academic class/grade id
        └── sectionDocsId -> future academic section id
```

All top-level documents extend `SchoolBase`. Every reference lookup must include
`schoolId`.

## Student — `students`

Stores stable student identity and contact information. `admissionNo` is
generated with `NumberSequenceType.STUDENT_ADMISSION` and is unique within one
school. `admissionApplicationDocsId` is optional because schools may create a
student directly without using CRM admissions.

GuardianLink values are embedded because one student has a small, bounded list
of guardians. Guardian remains a separate collection so one guardian can be
reused for siblings. The service must prevent duplicate guardian links and
allow at most one `primaryContact = true` per student.

Student intentionally does not store:

- current academic-record id;
- health or medical records;
- hostel room or boarding assignment;
- transport assignment;
- wallet or fee-account references;
- attendance or academic results;
- temporary or signed profile-photo URLs.

Those values belong to their owning modules. The current academic record is
queried from StudentAcademicRecord using its active-record index, avoiding a
duplicated pointer that can become stale.

## Guardian — `guardians`

Stores one real guardian/contact profile. `phoneNumber` and `emailAddress` are
unique within one school and support find-or-reuse during student creation.
`alternatePhoneNumber` is not unique because it may be a shared family number.

The DTO/service must require at least one usable contact method and normalize
phone numbers and email addresses before persistence. Blank phone/email values
must be converted to null so they do not participate in partial unique indexes.

## GuardianLink — embedded

Connects a Student to a Guardian and stores information that belongs to that
specific relationship: relation, primary contact, emergency contact, pickup
authorization, and portal access.

## StudentAcademicRecord — `student_academic_records`

Stores class and section placement for one student in one academic year. It
extends `AcademicStudentSchoolBase`, so `schoolId`, `studentDocsId`, and
`academicYear` are inherited.

One student can have academic placement history, but only one ACTIVE academic
record is allowed in the same academic year. When changing class or section:

1. set the old record's status to TRANSFERRED;
2. set its `effectiveUntil`;
3. create the new ACTIVE academic record;
4. link `previousAcademicRecordDocsId` to the old record.

These changes should be performed transactionally. `rollNo` is generated using
`NumberSequenceType.STUDENT_ROLL_NUMBER` when the school enables automatic roll
numbers.

## Deferred models

The old references were intentionally not copied as follows:

- `PersonRecord`: student identity is stored directly in Student.
- `GuardianStudentRelationship`: replaced by embedded GuardianLink.
- `StudentEnrollment`: redesigned as StudentAcademicRecord.
- `StudentLifecycleEvent`: defer until lifecycle audit history is required.
- Health, hostel, transport, attendance, conduct, and student-life records:
  build them in their own future modules.

## Validation responsibility

Models contain only essential persistence requirements. DTOs and services
validate date ordering, contact formats, guardian-link rules, tenant ownership,
student status transitions, academic-record transitions, class/section ownership,
roll-number generation, and admission conversion.
