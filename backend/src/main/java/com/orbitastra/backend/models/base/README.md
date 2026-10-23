# Base collection model mapping

This package defines persistence metadata inherited by every MongoDB
collection models. These classes are abstract and do not create collections by
themselves.

## Inheritance structure

```text
AuditedDocument
├── School
├── PlanDefinition
└── SchoolBase
    ├── Inquiry
    ├── AdmissionCycle
    ├── SchoolSubscription
    └── AcademicStudentSchoolBase
        ├── Attendance
        ├── AcademicResult
        └── other student-and-year collections
```

`School` extends `AuditedDocument` directly because it is the tenant root and
cannot contain a `schoolId` pointing to itself. Platform-level definitions such
as `PlanDefinition` also extend `AuditedDocument`.

Every collection document owned by a school extends `SchoolBase`.

Documents that always belong to both a student and an academic year extend
`AcademicStudentSchoolBase`.

Embedded value objects extend none of these classes. They are stored inside a
parent document and inherit its identity, school ownership, audit history, and
lifecycle.

## AuditedDocument

Provides collection identity, auditing, and optimistic locking.

| Field | Meaning and mapping |
|---|---|
| `id` | MongoDB `_id` of the concrete collection document. |
| `createdAt` | UTC creation time populated through `@CreatedDate`. |
| `updatedAt` | UTC last-modified time populated through `@LastModifiedDate`. |
| `createdByDocsId` | Identity/account document id of the creator, populated through `@CreatedBy`. |
| `updatedByDocsId` | Identity/account document id of the latest editor, populated through `@LastModifiedBy`. |
| `version` | Spring Data optimistic-lock value populated through `@Version`. |

The application must enable MongoDB auditing and provide an
`AuditorAware<String>` implementation for the audit fields to be populated.
System jobs may use a dedicated system identity.

`version` is not a database-schema version. It changes whenever the document is
updated and is used to reject stale concurrent writes.

## SchoolBase

Adds the school tenant boundary and recoverable record lifecycle.

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Required link to `School.id`; this is the tenant boundary. |
| `recordState` | General persistence lifecycle: active, inactive, archived, or deleted. |
| `archivedAt` | UTC time when the document entered `ARCHIVED`. |
| `deletedAt` | UTC time when the document was soft-deleted. |
| `deletedByDocsId` | Identity/account document id that performed the soft deletion. |

The SaaS platform supports many schools. It does not use a second `tenantId` or
a campus scope. For this design:

```text
tenant = school
tenant boundary field = schoolId
```

Every read, update, and delete query for a school-owned collection must include
`schoolId`, even when `_id` is supplied:

```text
Correct:   schoolId + id
Incorrect: id only
```

This check must also be applied to every `...DocsId` reference before linking
documents.

## AcademicStudentSchoolBase

Adds the two shared ownership fields needed by student-and-year collections.

| Field | Meaning and mapping |
|---|---|
| `academicYear` | Required immutable `AcademicYear.name`, for example `2026-2027`. |
| `studentDocsId` | Required link to `Student.id`. |

The system never stores an AcademicYear document id in child records. The
academic-year name must therefore remain immutable after creation.

Concrete collections should add compound indexes matching their actual queries,
for example:

```text
schoolId + academicYear + studentDocsId
```

The inherited single-field indexes are useful for basic lookup but do not
replace concrete compound indexes.

## RecordState lifecycle

`RecordState` is independent from business statuses. For example, an admission
application can be `REJECTED` while its general `recordState` is still `ACTIVE`.

```text
ACTIVE
  ├── INACTIVE
  ├── ARCHIVED  -> archivedAt is set
  └── DELETED   -> deletedAt and deletedByDocsId are set
```

Normal application operations should use soft deletion. Permanent deletion
should only happen after the applicable retention policy, legal hold, audit, and
backup rules allow it.

Lifecycle timestamp consistency and allowed state transitions belong in the
service layer.

## Validation responsibility

These persistence classes contain only essential required-field constraints:

- `schoolId` is required for school-owned documents.
- `recordState` is required.
- `academicYear` and `studentDocsId` are required for student-and-year documents.

Request-specific validation belongs in API request DTOs and services, including:

- identifier formats and length limits;
- authorization and tenant checks;
- archive/delete transition rules;
- timestamp consistency;
- existence of referenced School, Student, and identity documents.

MongoDB collection validators and indexes should be deployed through controlled
database migrations. Java validation annotations alone do not enforce the stored
database schema.
