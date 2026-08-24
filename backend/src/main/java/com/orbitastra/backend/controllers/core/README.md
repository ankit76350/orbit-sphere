# controllers/core — the tenant root and the academic calendar

**Nothing is built yet.** This file is the plan: every `POST`, `PUT` and `PATCH` the two `core`
resources need, why each exists separately, and what each must refuse.

Mirrors [`models/core`](../../models/core), which holds exactly two collections:

| Resource | Model | What it is |
|---|---|---|
| School | [`School.java`](../../models/core/School.java) | the tenant root — the only document with no `schoolId` |
| Academic year | [`AcademicYear.java`](../../models/core/AcademicYear.java) | a named year, and the school's holiday calendar |

The state machines and the validation split below are not invented here. They are written down
in [`models/core/README.md`](../../models/core/README.md), and these endpoints exist to enforce
what that file already says.

**Scope: writes only**, as asked. Reads are a separate pass — they turn on a different
question (what a school admin may see of their own tenant versus what a platform admin sees
across all of them), and mixing the two would settle neither well.

---

# Start here: two callers, not one

The most important structural decision in this package, and unfixable later.

|  | Platform surface | School surface |
|---|---|---|
| Base path | `/platform/schools` | `/schools/current` |
| Who calls it | platform operator | a school's own admin |
| Auth | platform credentials, **no `schoolId` in the token** | school session + `SCHOOL_SETTINGS` |
| Create a tenant? | yes | never |
| Change tenant lifecycle? | yes | never |
| Manage academic years? | no | yes |

**Creating a school cannot be a school operation.** At the moment `POST /platform/schools` runs
there is no user, staff record, role or session belonging to that school — they are all created
*by* that request. The caller is necessarily outside the tenant.

**The school surface uses `current`, never `{id}`.** The tenant comes from the session and the
subdomain, never a path parameter. `PATCH /schools/{id}/profile` invites the bug where an admin
passes somebody else's id; `PATCH /schools/current/profile` makes it structurally impossible.
Every school-scoped controller in this system should follow that rule.

---

# Part 1 — School

## POST, platform surface

| Method | Path | What it does |
|---|---|---|
| `POST` | `/platform/schools` | Provision a new tenant. **Atomic** — see below |
| `POST` | `/platform/schools/{id}/activate` | `TRIAL`\|`PROVISIONING` → `ACTIVE` |
| `POST` | `/platform/schools/{id}/suspend` | `ACTIVE` → `SUSPENDED`. Reason required |
| `POST` | `/platform/schools/{id}/reactivate` | `SUSPENDED` → `ACTIVE` |
| `POST` | `/platform/schools/{id}/offboard` | `ACTIVE` → `OFFBOARDING`. Starts data export |
| `POST` | `/platform/schools/{id}/close` | `OFFBOARDING` → `CLOSED` |
| `POST` | `/platform/schools/{id}/request-deletion` | `CLOSED` → `DELETION_PENDING` |
| `POST` | `/platform/schools/{id}/cancel-deletion` | `DELETION_PENDING` → `CLOSED` |
| `POST` | `/platform/schools/{id}/confirm-deletion` | `DELETION_PENDING` → `DELETED` |

### Why transitions are `POST`, not `PATCH /status`

They are not field edits. `POST /{id}/suspend` **does things**: blocks every live `AuthSession`,
stops scheduled jobs, stamps `suspendedAt`, records who and why. Modelling that as
`PATCH {"status": "SUSPENDED"}` says "set a field", and invites setting any status from any
other — which is how a `DELETED` school comes back to life, or an un-provisioned one goes live.

One endpoint per transition also means each takes **only** what it needs: a suspension needs a
reason, an activation does not.

### The transitions must match the documented machine

```text
TRIAL / PROVISIONING -> ACTIVE
ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE -> OFFBOARDING -> CLOSED -> DELETION_PENDING -> DELETED
```

Anything not on that list is a `409`, not a `400` — the request is well-formed, the tenant is
just not in a state where it makes sense.

**`cancel-deletion` is an addition I am proposing**, not something the model's README lists.
`DELETION_PENDING` is defined as "requested but not yet executed", so the window exists on
purpose — and a window with no way back means one mis-click destroys a school. Drop it if you
prefer the strictly documented one-way path, but then say out loud that deletion is
irreversible from the moment it is requested.

### `POST /platform/schools` is atomic, and does more than insert a row

A school that exists but cannot be logged into is worse than one that does not exist. One
transaction:

1. the `School` row, at `PROVISIONING` or `TRIAL`
2. all `NumberSequence` rows — every type, or nothing else can ever be created
3. the default `Role` set
4. the first `Staff` record for the account holder
5. the first `UserAccount`, linked to that staff and those roles
6. the first `AcademicYear`, if one was supplied

If step 5 fails, steps 1–4 roll back. **Half a tenant is not a useful outcome** and somebody
unpicks it by hand in the database.

The ordering is forced, not chosen: `UserAccount.personDocsId` points at a `Staff`, and `Staff`
needs `schoolId`, which needs the `School`.

**Refuse from the caller:** `status` (always `PROVISIONING`/`TRIAL` on create — a caller who can
post `ACTIVE` has skipped whatever activation checks), `encryptionKeyReference` (a KMS pointer
the platform derives; a caller who sets it can point a new tenant at another tenant's key),
`activatedAt` and `suspendedAt` (stamped by their transitions).

**Idempotency:** needs an `Idempotency-Key` header. A retry after a timeout must return the
first result, not provision a second tenant. The unique index on `subdomain` catches an exact
duplicate; a retry differing by one character sails through, and you find out months later.

## PATCH, platform surface

| Method | Path | What it changes |
|---|---|---|
| `PATCH` | `/platform/schools/{id}/subdomain` | the tenant routing label |
| `PATCH` | `/platform/schools/{id}/account-holder` | `accountHolderName` |
| `PATCH` | `/platform/schools/{id}/encryption-key` | rotate the KMS reference |

`subdomain` is its own endpoint because it is the **globally unique key that resolves a request
to a tenant**. Changing it breaks every bookmark and saved link, invalidates routing caches, and
may need the old label held in reserve so another school cannot claim it and collect the first
school's logins. None of that is true of a phone number. Burying it inside `PATCH /profile`
alongside an email address means somebody does it by accident. Require explicit confirmation of
the old value.

`encryption-key` is platform-only for the same class of reason, and must not appear on the
school surface at all.

## PATCH and PUT, school surface

| Method | Path | Fields |
|---|---|---|
| `PATCH` | `/schools/current/profile` | `schoolName`, `phoneNumber`, `emailAddress` |
| `PUT` | `/schools/current/address` | `addressLine`, `city`, `stateOrProvince`, `postalCode` |
| `PATCH` | `/schools/current/localization` | `defaultLocale`, `defaultTimeZone` |
| `PUT` | `/schools/current/logo` | replaces `logoUrl` |

### Where `PUT` is right, and where it is not

You asked what `PUT`s exist. The honest answer across this whole package is **four**, and they
are all sub-resources that are complete values.

`PUT` means *replace this whole thing*. Correct for an **address**, because patching `city`
without `stateOrProvince` produces a real address for a place that does not exist. Correct for a
**logo** — there is one or there is not.

`PUT` is **wrong for the School document itself**. There must be no
`PUT /platform/schools/{id}`. A full replace of a resource holding `status`, `activatedAt`,
`suspendedAt` and `encryptionKeyReference` hands a caller every field the document defends, and
a client omitting a field it did not know about will blank it. Partial edits by intent, always.

### `countryCode` is missing from the address `PUT` on purpose

Changing a school's country changes which tax rules, identity documents and board affiliations
apply — `GovernmentIdentityType` holds `AADHAAR` and `APAAR`; `FeeHead.taxRatePercent` means
GST. Schools do not move countries. Somebody mistyping at signup is the real scenario, and that
is a platform correction during `PROVISIONING`, not a self-service edit after go-live.

**Recommendation:** editable on the platform surface while `PROVISIONING` or `TRIAL`, immutable
afterwards.

### `defaultTimeZone` is the most dangerous field in this package

Every `Instant` in the database is UTC, so changing the zone rewrites nothing — and that is the
problem. It silently reinterprets every **school-local** decision already made: which calendar
date an attendance record falls on, whether a holiday covers a day, when a `DailyTimetable`
period starts, which day a `TransportTrip` ran.

A school moving `Asia/Kolkata` → `Asia/Dubai` mid-year has an attendance register that shifts
under it. Warn, require confirmation, and **refuse outright once an `AcademicYear` is in
progress.** It is not a settings toggle.

### Logo upload

`logoUrl` is a public CDN URL, so `PUT /logo` has two shapes: accept a URL the school already
hosts, or accept a file, store it, return the URL. Prefer the second — a school-supplied URL can
rot, change to something unwanted, or point at a tracker on a page parents load. If you accept a
URL, require `https` and an allow-listed host.

### Reserved subdomains

Refuse at least `www`, `api`, `admin`, `app`, `platform`, `status`, `mail`, `smtp`, `ftp`, `cdn`,
`static`, `assets`, `login`, `auth`, `support`, `help`, `docs`, `blog`, `test`, `staging`, `dev`.
A school that claims `api` or `login` receives traffic and credentials meant for the platform.

---

# Part 2 — AcademicYear

All on the school surface. There is no platform surface for academic years: a year belongs to
one school's calendar and no platform operator should be setting one.

| Method | Path | What it does |
|---|---|---|
| `POST` | `/schools/current/academic-years` | Create a year |
| `PATCH` | `/schools/current/academic-years/{name}/dates` | Move `startDate` / `endDate` |
| `POST` | `.../{name}/enrollment/enable` | `enrollmentEnabled` → true |
| `POST` | `.../{name}/enrollment/disable` | `enrollmentEnabled` → false |
| `POST` | `.../{name}/results/lock` | `resultsLocked` → true |
| `POST` | `.../{name}/results/unlock` | `resultsLocked` → false. **Privileged** |
| `PUT` | `.../{name}/holidays` | Replace the whole calendar |
| `POST` | `.../{name}/holidays` | Add one holiday |
| `PATCH` | `.../{name}/holidays/{date}` | Edit one holiday |
| `DELETE` | `.../{name}/holidays/{date}` | Remove one holiday |
| `POST` | `.../{name}/holidays/generate-weekly-off` | Generate a weekday's offs across the year |

## The rule that matters most: there is no rename endpoint

**`AcademicYear.name` can never be changed. Not by `PATCH`, not by anything.**

Other collections do not reference this document by id. They store the **name as a string** in
their own `academicYear` field — `FeeInvoice`, `TransportTrip`, `FeedbackCampaign`,
`FacilityInspection` and dozens more. `"2026-2027"` is the join key.

Which means **there is no referential integrity to lean on.** A rename does not fail loudly and
does not cascade; it leaves every one of those strings pointing at a year that no longer answers
to that name, and every row still looks perfectly valid. You would find out when a fee report
came back empty.

So:

- No `PATCH /academic-years/{name}` that accepts `name`. The field is not in any request DTO.
- A request containing `name` on an update is a `400`, not silently ignored.
- **The URL is keyed by `name`, not by id** — `/academic-years/2026-2027`. That is deliberate:
  it matches how the whole system refers to a year, and a URL that cannot change is a daily
  reminder that the thing it names cannot either.

## Deleting a year has the same problem, and no cheap answer

Because references are strings, "is this year used anywhere?" cannot be answered with a foreign
key check. It is a query across every collection that carries an `academicYear` field.

**Recommendation:** no hard delete at all. A year created by mistake and never used can be
removed while nothing references it; past that, it stays. `RecordState` on
[`SchoolBase`](../../models/base/SchoolBase.java) already exists for exactly this, and hiding a
year is not the same as breaking every row that names it.

## Creating a year

`POST /schools/current/academic-years` — validates:

- `name` unique within the school (the unique index enforces it; the API should say so nicely)
- `startDate` before `endDate`
- **no overlap with any existing year for this school.** Two overlapping years mean a date
  belongs to both, and every "which year is this?" lookup gets two answers
- the range is plausible — a year is roughly a year, not three days or five years

Holidays may be supplied at creation or added afterwards.

## `PATCH .../dates` is riskier than it looks

Moving a year's boundaries after the year has started orphans data at both ends: an attendance
record, an invoice or a trip now sits outside the year that owns it. The endpoint should refuse
to move a boundary **past existing data** — shrinking a year is the dangerous direction, and
extending it is usually harmless.

## Enrollment and results are `POST`, and for different reasons

`enrollmentEnabled` and `resultsLocked` are booleans, so `PATCH` would work mechanically. They
are `POST` actions because both are **gates with authorization attached**, and
`models/core/README.md` names "authorization for result locking and enrollment controls" as an
API responsibility.

**`results/unlock` is the one to be careful with.** Locking results is routine; unlocking them
means somebody can change a mark a parent has already seen. It needs a higher permission than
locking, a reason recorded, and an
[`AuditEvent`](../../models/audit/AuditEvent.java) every time — including failed attempts. A
single `PATCH {"resultsLocked": false}` makes the most sensitive operation in the package look
like the least.

## Holidays: why all four methods, and one generator

`holidays` is an embedded `List<HolidayDetail>` — a sub-resource, keyed by `date`.

- **`PUT`** replaces the whole calendar. This is the legitimate bulk case: a school publishes
  next year's calendar in one go, from a spreadsheet. Sending the complete list makes a
  half-imported calendar impossible.
- **`POST`** adds one. The ordinary case in-year — a bandh, an unexpected closure.
- **`PATCH`** edits one, keyed by date. Renaming "Diwali" to "Diwali (day 2)".
- **`DELETE`** removes one. A public holiday moved by government notification.

Validate: no duplicate dates, and every date inside the year's own `startDate`–`endDate`.

### The generator is not a convenience — the model requires it

There is **no "weekly off day" field anywhere in this system**, deliberately. Schools in this
market may run on Sunday with the weekly off on any other day, so every non-working day is a
**dated** `HolidayDetail` with type `WEEKLY_OFF`.

That is the right model and it has a direct API consequence: a year needs roughly 52 dated
`WEEKLY_OFF` rows, and nobody is typing those in. So:

`POST .../holidays/generate-weekly-off` taking a day of the week, and optionally a date range,
generating one `WEEKLY_OFF` entry per occurrence and skipping dates that already have a holiday.

Without it, either somebody enters 52 dates by hand or a developer eventually hardcodes Sunday
somewhere — which is the exact assumption the model was designed to prevent.

**No service anywhere may infer a non-working day from the weekday.** Attendance, timetable,
transport, bookings and fee due dates all read `holidays`. That rule is repeated across a dozen
model READMEs; this generator is what makes following it practical.

---

# Shared concerns

## The bootstrap problem, on the very first request

[`AuditedDocument.createdByDocsId`](../../models/base/AuditedDocument.java) is filled
automatically by `@EnableMongoAuditing` for every document. **`POST /platform/schools` has no
account to attribute anything to** — the first `UserAccount` is created by that same request.

So before this endpoint works at all, the auditing setup needs a deliberate concept of *a write
with no ordinary actor*, with a reserved sentinel for the platform. Not a nullable field filled
in later.

Build that concept properly, because the same mechanism is needed by
[`feedback`](../../models/feedback/README.md), where anonymous submissions **must** write the
sentinel `"ANONYMOUS"` instead of a real user id. Get it right once here and anonymity works;
assume there is always a user and you will retrofit it later, in a way that silently
deanonymises children.

## Validation ownership

`models/core/README.md` keeps the model annotations thin on purpose and hands these to the API
layer. Assigned:

| Validation | Endpoint |
|---|---|
| subdomain format, normalization, reserved words | school create, `PATCH /subdomain` |
| email lowercased, phone normalized to E.164 | school create, `PATCH /profile` |
| IETF language tag, IANA zone id | school create, `PATCH /localization` |
| ISO 3166-1 alpha-2 country | school create only |
| `https` and allow-listed host for logo | `PUT /logo` |
| allowed `SchoolStatus` transitions | each transition endpoint |
| an active `SchoolSubscription` exists | `POST /activate` |
| academic-year date ordering **and overlap** | year create, `PATCH /dates` |
| duplicate holiday dates, dates inside the year | all four holiday endpoints |
| authorization for lock/unlock and enrollment | those four endpoints |
| text lengths on every free-text field | all |

`activate` checking the subscription is worth calling out: `SchoolStatus` and
`SubscriptionStatus` are separate models, and `core/README.md` says payment status "must not be
stored in SchoolStatus". Activation reads
[`SchoolSubscription`](../../models/plans/SchoolSubscription.java) rather than the school
carrying a copy.

## Deliberately not here

- **`DELETE` on a School.** A tenant is never removed with `DELETE`; it walks the lifecycle to
  `DELETED`. The only `DELETE`s in this package are on holidays, and arguably on a logo.
- **Subscription and plan changes.** `SchoolSubscription` is its own resource with its own
  controller. `School`'s javadoc says plan data is deliberately not embedded.
- **Terms.** [`AcademicTerm`](../../models/academics/structure/AcademicTerm.java) lives in
  `academics`, not here, so it gets a controller there.
- **Bulk tenant operations.** Suspending forty schools at once is an operational script, not an
  endpoint.
- **Notifications.** "Your school has been suspended", "results have been unlocked" — messages,
  and `notification` is designed last by the decision of 2026-08-14. No `notifiedAt` field, and
  nothing sends from here.
