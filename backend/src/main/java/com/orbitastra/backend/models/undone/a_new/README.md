# EduSphere target data model

This directory is the proposed target model for the frontend scope currently
defined in `frontend/src/modules` and
`frontend/src/modules/enterprise/enterpriseCatalog.js`.

It contains **228 MongoDB document models in 43 domain packages**, plus four
shared base classes and a shared enum catalogue. These classes compile with the
current backend, but they are intentionally not connected to repositories,
services, controllers, or migrations yet.

Read [ARCHITECTURE_REVIEW.md](ARCHITECTURE_REVIEW.md) before adopting the
models. It records the observed problems in the current collections, the target
boundaries, the frontend-to-model coverage map, and a safe migration order.

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

| Area | Packages |
|---|---|
| SaaS foundation | `saas`, `institution`, `identity`, `audit` |
| People and SIS | `people`, `admissions`, `aid`, `alumni` |
| Academics | `academics`, `learning`, `conduct`, `studentlife`, `feedback` |
| Finance and HR | `billing`, `accounting`, `payroll`, `procurement` |
| Campus operations | `facilities`, `inventory`, `library`, `transport`, `hostel`, `mess`, `health`, `gate`, `dismissal`, `emergency`, `security`, `frontoffice`, `it` |
| Assurance | `governance`, `compliance`, `privacy`, `legal`, `support` |
| Platform and engagement | `workflow`, `integration`, `communication`, `documents`, `media`, `cms`, `reporting`, `ai` |

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
