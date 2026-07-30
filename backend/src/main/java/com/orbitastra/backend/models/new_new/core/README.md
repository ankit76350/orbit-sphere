# Core collection model mapping

The core package contains the tenant root and the school academic-year calendar.

## Relationship overview

```text
School
  |
  | School.id is stored as SchoolBase.schoolId
  |
  +--> AcademicYear
  |      |
  |      +--> HolidayDetail[] (embedded)
  |
  +--> Inquiry
  +--> AdmissionCycle
  +--> SchoolSubscription
  +--> all other school-owned collections

AcademicYear.name
  |
  +--> AdmissionCycle.academicYear
  +--> Inquiry.academicYear
  +--> AcademicStudentSchoolBase.academicYear
  +--> future classes, timetables, exams, and enrollment records
```

All school-owned references must be resolved using `schoolId` in addition to the
referenced value. The platform does not use a separate `tenantId` or campus
scope:

```text
tenant = school
tenant identifier = School.id = child SchoolBase.schoolId
```

## School — `schools`

`School` is the tenant root. It extends `AuditedDocument`, not `SchoolBase`,
because it cannot contain a schoolId pointing to itself.

| Field | Meaning and mapping |
|---|---|
| `id` | Tenant id copied into every school-owned document's `schoolId`. |
| `schoolName` | Display name of the school. |
| `accountHolderName` | Primary SaaS account/business contact name. |
| `subdomain` | Globally unique, lowercase tenant-routing label. |
| `logoUrl` | Public or CDN-hosted school logo URL. |
| `phoneNumber` | Primary school contact, normalized by the service. |
| `emailAddress` | Primary school contact email, normalized by the service. |
| `encryptionKeyReference` | KMS/key-vault key identifier; never raw key material. |
| `defaultLocale` | Default IETF locale, for example `en-IN`. |
| `defaultTimeZone` | Default IANA time zone, for example `Asia/Kolkata`. |
| `addressLine` | Street/building address. |
| `city` | City or locality. |
| `stateOrProvince` | State, province, or equivalent region. |
| `postalCode` | Text postal code, supporting leading zeros and international formats. |
| `countryCode` | ISO 3166-1 alpha-2 code such as `IN`, `GB`, or `US`. |
| `status` | Operational lifecycle of the tenant. |
| `activatedAt` | First successful activation time. |
| `suspendedAt` | Most recent suspension time. |

The subdomain should be normalized before persistence. For example:

```text
Orbit-School -> orbit-school
```

The unique MongoDB index on `subdomain` is the final concurrency-safe duplicate
check. An application-level availability check alone is not sufficient.

### Subscription mapping

Plan and subscription fields are intentionally absent from School. The current
subscription is found using:

```text
SchoolSubscription.schoolId = School.id
SchoolSubscription.current = true
```

That subscription then links to `PlanDefinition` through
`planDefinitionDocsId`.

## AcademicYear — `academic_years`

Represents one school-specific academic year and its dated calendar.

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `name` | Immutable child-document reference, for example `2026-2027`. |
| `startDate` | First date of the academic year. |
| `endDate` | Last date of the academic year. |
| `holidays` | Embedded dated HolidayDetail values. |
| `enrollmentEnabled` | Whether new enrollment assignment is currently allowed. |
| `resultsLocked` | Whether result changes are currently blocked. |

The system never stores AcademicYear.id in child records. It always stores:

```text
schoolId + academicYear name
```

Therefore, `name` is unique within one school and immutable after creation.
Changing the name would orphan references in other collections.

The date index supports finding the academic year covering a given date. The
service must still reject invalid or unintended overlapping academic years.

## HolidayDetail — embedded

`HolidayDetail` is stored inside `AcademicYear.holidays`. It is not a collection,
does not have its own id, and does not extend a base class.

| Field | Meaning |
|---|---|
| `name` | Display name of the holiday or weekly off. |
| `description` | Optional school-specific explanation. |
| `type` | HolidayType classification. |
| `date` | One concrete calendar date. |

Recurring weekly offs are expanded into concrete dates. This allows the school
to remove one Sunday or Saturday from the holiday list when it becomes a working
day without changing all other weekly offs.

## Enums

### SchoolStatus

Describes the tenant's operational lifecycle:

```text
TRIAL / PROVISIONING -> ACTIVE
ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE -> OFFBOARDING -> CLOSED -> DELETION_PENDING -> DELETED
```

Subscription payment status remains in `SchoolSubscription`; it must not be
stored in SchoolStatus.

### HolidayType

Classifies calendar entries such as weekly offs, public holidays, festivals,
vacations, and examination breaks.

### NotificationChannel

Lists supported delivery channels. It does not store a user's preference,
message, provider response, or delivery status; those belong to communication
models.

## Validation responsibility

The persistence models contain only essential required constraints:

- required school identity and tenant routing values;
- required academic-year name and date boundaries;
- required lifecycle enums and booleans;
- required holiday name, type, and date.

API request DTOs and services should validate:

- subdomain, email, phone, locale, time-zone, country, and URL formats;
- text lengths;
- academic-year date ordering and overlap;
- duplicate holiday dates;
- allowed SchoolStatus transitions;
- authorization for result locking and enrollment controls;
- normalization of subdomain, phone number, and email address;
- existence and tenant ownership of every referenced document.

MongoDB indexes and collection validators should be deployed through controlled
database migrations. Java annotations alone do not enforce the stored database
schema.
