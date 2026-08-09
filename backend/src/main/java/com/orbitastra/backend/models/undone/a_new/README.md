# EduSphere target data model

This directory is the proposed target model for the frontend scope currently
defined in `frontend/src/modules` and
`frontend/src/modules/enterprise/enterpriseCatalog.js`.

It started as 228 document models in 43 domain packages and now contains
**176 models in 39 packages**, because a package is deleted from here once its
final design has been produced in `models/new_new`.

This tree is reference material only. It is not connected to repositories,
services, controllers, or migrations, and it **no longer compiles**: 175 of its
176 classes import a `a_new.base` package that is not present in the repository.
Do not add it to a build.

Read [ARCHITECTURE_REVIEW.md](ARCHITECTURE_REVIEW.md) before adopting the
models. It records the observed problems in the current collections, the target
boundaries, the frontend-to-model coverage map, and a safe migration order. Its
section 3 mapping table covers every original model, including the ones already
deleted from this directory, so the design intent survives the deletions.

## Finalized and removed

These packages have been designed in `models/new_new` and deleted from here.
Recover any original file from git history if a deferred idea is needed later.

| Removed from a_new | Finalized in new_new |
|---|---|
| `academics` | `new_new/academics` (structure, curriculum, grading, timetable, homework, attendance, examination) |
| `people` | `new_new/people` and `new_new/student` |
| `admissions` | `new_new/crm` |
| `documents` | `new_new/documents`; generation and template models remain sketched in `a_working/document` |

Not carried across, and intentionally deferred rather than lost:
`AssessmentBankItem`, `AccommodationPlan`, `ScheduleConstraint`,
`StudentLifecycleEvent`, `JobVacancy`, `RecruitmentApplication`,
`OnboardingCase`, `DocumentTemplateDefinition`, `DocumentGenerationJob`,
`IssuedCredential`.

## Core conventions

- `tenantId` is the customer isolation boundary and candidate shard-key prefix.
- `legalEntityDocsId`, `campusDocsId`, `academicYearDocsId`, and
  `programmeDocsId` are separate scopes; none is inferred from a display name.
- References use immutable document IDs. Labels and names are snapshots only
  when preserving historical meaning is necessary.
- `Instant` is used for an event in time; `LocalDate` is used for a civil date.
  Tenant/campus time zone is stored separately where scheduling needs it.
- Every mutable aggregate has optimistic locking through `@Version`.
- Business identifiers are unique inside their actual tenant/legal
  entity/campus scope rather than globally.
- Unbounded histories and high-volume child data are separate collections.
  Small, bounded value objects that are written with their parent may be
  embedded.
- Financial postings, audit events, wallet entries, access movements, and other
  ledgers are append-only after posting. Corrections use reversal records.
- Secrets are represented by vault/KMS references. Sensitive identity,
  contact, health, safeguarding, and counselling values require application
  encryption; equality searches use keyed blind indexes.
- Stored files are referenced through `StoredObject`; business collections do
  not persist public or provider URLs as their source of truth.
- Lifecycle states are stable codes. User-visible labels are localized outside
  persisted workflow state.

## Domain packages

Packages struck through have been finalized in `new_new` and deleted from here.

| Area | Packages |
|---|---|
| SaaS foundation | `saas`, `institution`, `identity`, `audit` |
| People and SIS | ~~`people`~~, ~~`admissions`~~, `aid`, `alumni` |
| Academics | ~~`academics`~~, `learning`, `conduct`, `studentlife`, `feedback` |
| Finance and HR | `billing`, `accounting`, `payroll`, `procurement` |
| Campus operations | `facilities`, `inventory`, `library`, `transport`, `hostel`, `mess`, `health`, `gate`, `dismissal`, `emergency`, `security`, `frontoffice`, `it` |
| Assurance | `governance`, `compliance`, `privacy`, `legal`, `support` |
| Platform and engagement | `workflow`, `integration`, `communication`, ~~`documents`~~, `media`, `cms`, `reporting`, `ai` |

Note that `saas` was never created in this directory; its concerns are designed
as `new_new/plans` and `new_new/core`.

## Before persistence is enabled

1. Establish authenticated tenant context and reject client-supplied tenant
   scope.
2. Add API request DTO validation and database JSON Schema validators.
3. Implement controlled index migrations; disable automatic index creation in
   production.
4. Define aggregate transaction boundaries, idempotency behavior, outbox
   publishing, encryption converters, and audit interception.
5. Run a measured index/query review against representative tenant volumes.
6. Adopt the migration sequence in the architecture review. Do not point the
   new models at existing collection names without a backfill and rollback plan.
