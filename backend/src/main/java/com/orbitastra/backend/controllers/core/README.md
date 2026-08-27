# controllers/core — write API plan

**One endpoint is built — #1.** This is the complete inventory of every `POST`, `PUT` and `PATCH` the
`core` module needs, sequenced so it can be built and reviewed one step at a time.

Mirrors [`models/core`](../../models/core), which holds exactly two collections:

| Resource | Model | What it is |
|---|---|---|
| School | [`School.java`](../../models/core/School.java) | the tenant root — the only document with no `schoolId` |
| Academic year | [`AcademicYear.java`](../../models/core/AcademicYear.java) | a named year, and the school's holiday calendar |

The state machines and validation split are not invented here — they are already written in
[`models/core/README.md`](../../models/core/README.md). These endpoints enforce that file.

**Scope: writes only**, as asked. Reads are a separate pass. Two `DELETE`s appear in the
inventory because they exist and you should see them, but they are not counted in the totals.

---

# Complete inventory — 28 write endpoints

**18 POST · 7 PATCH · 3 PUT** (+2 DELETE)

## School — 17

| # | Method | Path | Phase |
|---|---|---|---|
| 1 | `POST` | `/platform/schools` | 1 — **built** |
| 2 | `POST` | `/platform/schools/{id}/complete-provisioning` | 1 |
| 3 | `POST` | `/platform/schools/{id}/activate` | 2 |
| 4 | `POST` | `/platform/schools/{id}/suspend` | 2 |
| 5 | `POST` | `/platform/schools/{id}/reactivate` | 2 |
| 6 | `PATCH` | `/schools/current/profile` | 3 |
| 7 | `PUT` | `/schools/current/address` | 3 |
| 8 | `PATCH` | `/schools/current/localization` | 3 |
| 9 | `PUT` | `/schools/current/logo` | 3 |
| 10 | `PATCH` | `/platform/schools/{id}/subdomain` | 6 |
| 11 | `PATCH` | `/platform/schools/{id}/account-holder` | 6 |
| 12 | `PATCH` | `/platform/schools/{id}/encryption-key` | 6 |
| 13 | `POST` | `/platform/schools/{id}/offboard` | 7 |
| 14 | `POST` | `/platform/schools/{id}/close` | 7 |
| 15 | `POST` | `/platform/schools/{id}/request-deletion` | 7 |
| 16 | `POST` | `/platform/schools/{id}/cancel-deletion` | 7 |
| 17 | `POST` | `/platform/schools/{id}/confirm-deletion` | 7 |

## Academic year — 11

All paths below are under `/schools/current/academic-years`.

| # | Method | Path | Phase |
|---|---|---|---|
| 18 | `POST` | `/` | 4 |
| 19 | `PATCH` | `/{name}/dates` | 4 |
| 20 | `PUT` | `/{name}/holidays` | 5 |
| 21 | `POST` | `/{name}/holidays` | 5 |
| 22 | `PATCH` | `/{name}/holidays/{date}` | 5 |
| — | `DELETE` | `/{name}/holidays/{date}` | 5 |
| 23 | `POST` | `/{name}/holidays/generate-weekly-off` | 5 |
| — | `DELETE` | `/{name}/holidays?type=WEEKLY_OFF` | 5 |
| 24 | `POST` | `/{name}/enrollment/enable` | 6 |
| 25 | `POST` | `/{name}/enrollment/disable` | 6 |
| 26 | `POST` | `/{name}/results/lock` | 6 |
| 27 | `POST` | `/{name}/results/unlock` | 6 |
| 28 | `POST` | `/{name}/clone` | 8 — optional, see below |

---

# Build order

Sequenced by dependency first, then by risk. **Phase 0 is not optional and not skippable.**

| Phase | What | Endpoints |
|---|---|---|
| **0** | Foundations — no endpoints | — |
| **1** | Create a tenant | 1, 2 |
| **2** | Tenant lifecycle | 3, 4, 5 |
| **3** | School self-service edits | 6, 7, 8, 9 |
| **4** | Academic year exists | 18, 19 |
| **5** | The holiday calendar | 20–23 + 2 DELETE |
| **6** | Gates and sensitive edits | 24, 25, 26, 27, 10, 11, 12 |
| **7** | Offboarding and deletion | 13–17 |
| **8** | Convenience | 28 |

**Why this order:** nothing exists until phase 1. Phase 2 makes a tenant usable. Phase 3 is the
first thing a real school touches. Phases 4–5 give the school a calendar, which attendance,
timetable, transport and fees all read. Phase 6 groups everything needing elevated permission.
Phase 7 is last because it is rarely used and the most destructive — build it when the rest is
proven.

## Phase 0 — the plumbing every endpoint assumes

Three pieces. Getting them wrong is expensive to undo. **0.1 and most of 0.3 were built with
endpoint #1; 0.2 and idempotency are still open.**

**0.1 — The audit actor sentinel. BUILT.**
[`AuditedDocument.createdByDocsId`](../../models/base/AuditedDocument.java) is filled
automatically by `@EnableMongoAuditing`. **Endpoint #1 has no account to attribute anything
to** — no `UserAccount` exists for that school at all when it runs. So `AuditingConfig` supplies
an `AuditorAware` returning the `SystemActors.PLATFORM` sentinel, and provisioned rows carry
`createdByDocsId: "SYSTEM_PLATFORM"`.

Note it currently returns that sentinel for **every** write in the system, because there is no
authentication to ask. When sessions exist it must return the real `UserAccount` id, and keep
the sentinel only for genuine platform writes.

Build this properly, because the same mechanism is needed by
[`feedback`](../../models/feedback/README.md), where anonymous submissions **must** write the
sentinel `"ANONYMOUS"` instead of a real user id. Get it right once here and anonymity works
later; assume there is always a user and you will retrofit it in a way that silently
deanonymises children.

**0.2 — Tenant resolution. NOT BUILT.** Every school-surface endpoint uses `current`, resolved
from the session and subdomain. One place, not per-controller. Nothing needs it yet because
endpoint #1 is on the platform surface, but #6 onwards cannot be built without it.

**0.3 — Error contract. BUILT.** `ApiError` is the single response shape, `ApiException` carries
the status through one `@ExceptionHandler`, and `409` means "well-formed request, wrong state"
against `400` for "malformed". The transitions lean on that distinction heavily.
**Idempotency is the part still missing** — see #1.

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

**Creating a school cannot be a school operation.** When endpoint #1 runs there is no user,
staff record, role or session belonging to that school — they are all created *by* that request.
The caller is necessarily outside the tenant.

**The school surface uses `current`, never `{id}`.** The tenant comes from the session, never a
path parameter. `PATCH /schools/{id}/profile` invites the bug where an admin passes somebody
else's id; `current` makes it structurally impossible. Every school-scoped controller in this
system should follow that rule.

---

# Part 1 — School

## 1. `POST /platform/schools` — BUILT

Creates the `School` row at `PROVISIONING` or `TRIAL`. **That is all it does.**

An earlier version of this plan had it seed a `NumberSequence` for every type and a starting set
of `Role`s in the same transaction, and create the first `Staff` and `UserAccount` too. All of
that was removed on 2026-08-21. Two separate reasons, and both are worth keeping written down:

**The staff and account could not work.** `Staff` requires `dateOfBirth` and `gender`, both
non-null. A platform operator provisioning a school for a client does not know the principal's
birthday, and inventing one puts a false date into a record payroll and government reporting
will later treat as fact. The contract signatory and the school's first administrator are also
not necessarily the same person — a trustee may sign while an IT contractor does the setup. So
`School.accountHolderName` stays a plain name, and creating the first administrator is its own
endpoint that asks for what `Staff` actually requires.

**The seeding was removed by decision**, to be settled separately.

### What that leaves undone, and how it fails

A school at `PROVISIONING` currently has no number sequences and no roles. Neither absence
shows up here; both show up later, to somebody trying to use the school:

| Missing | Fails when |
|---|---|
| `NumberSequence` rows | the first student admission asks for a number and finds no counter to increment |
| `Role` rows | the first `UserAccount` is created and has nothing to point `roleDocsIds` at |

That is exactly what `PROVISIONING` as a starting status is for — *exists, not usable yet*. But
something must fill both in before #3 `activate` can be allowed to succeed. See #2.

**Accepts:** `schoolName`, `accountHolderName`, `subdomain`, `defaultLocale`,
`defaultTimeZone`, `countryCode`, and the account holder's contact details.

**Must reject:**

| Field | Why |
|---|---|
| `status` | always `PROVISIONING`/`TRIAL` on create — a caller posting `ACTIVE` skipped activation's checks |
| `encryptionKeyReference` | a KMS pointer the platform derives; a caller who sets it can aim a new tenant at another tenant's key |
| `activatedAt`, `suspendedAt` | stamped by their transitions, never supplied |

**Idempotency — NOT BUILT.** It should require an `Idempotency-Key` header so a retry after a
timeout returns the first result rather than provisioning a second tenant. Today it does not:
the unique index on `subdomain` catches an exact repeat, but a retry differing by one character
sails through and you find out months later. Needed before this faces a real network.

**Also not built:** any authentication. An unauthenticated endpoint that provisions tenants is
the most useful thing an attacker could be handed.

## 2. `POST /platform/schools/{id}/complete-provisioning`

**Renamed from `retry-provisioning` on 2026-08-27, because its job changed.**

It was a recovery endpoint: #1 was atomic in the database, but its *side effects* were not — a
KMS key, a DNS record, a storage bucket cannot be rolled back by a transaction, so a school
could end up with its DNS record made and its key missing. `retry-provisioning` re-ran whatever
had failed.

Now that #1 writes a single row, there is nothing to fail halfway and nothing to retry. What
there *is*, is a tenant that is deliberately incomplete — no number sequences, no roles. So this
endpoint stops meaning *recover from a failure* and starts meaning **finish the setup**:

1. seed a `NumberSequence` for every type, at `scopeKey` `GLOBAL`
2. seed the starting `Role` set — enough to attach a first administrator to

**Idempotent per step.** Running it twice must not produce a second set of 47 sequences. Check
what exists and fill the gaps, rather than assuming an empty tenant.

Whether this stays a separate endpoint or folds into whatever creates the first administrator is
still open. It has to happen somewhere before #3 `activate` can be allowed to succeed, and #3
should refuse a school that has no roles rather than activating one nobody can log into.

If #1 ever goes back to seeding inline, delete this endpoint rather than leaving it as a no-op.

## 3–5, 13–17. Lifecycle transitions

```text
TRIAL / PROVISIONING -> ACTIVE
ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE -> OFFBOARDING -> CLOSED -> DELETION_PENDING -> DELETED
```

Anything not on that list is a **`409`**, not a `400` — the request is well-formed, the tenant
is just not in a state where it makes sense.

| # | Endpoint | Requires | Side effects |
|---|---|---|---|
| 3 | `activate` | an active `SchoolSubscription`, **and #2 already run** | stamps `activatedAt` on first activation only |
| 4 | `suspend` | a reason | blocks every live `AuthSession`, stops scheduled jobs, stamps `suspendedAt` |
| 5 | `reactivate` | — | restores access; does **not** clear `suspendedAt` (it is "most recent") |
| 13 | `offboard` | a reason | starts data export |
| 14 | `close` | export complete | tenant no longer reachable |
| 15 | `request-deletion` | explicit confirmation | starts the retention clock |
| 16 | `cancel-deletion` | — | back to `CLOSED` |
| 17 | `confirm-deletion` | second confirmation | irreversible |

### Why transitions are `POST`, not `PATCH /status`

They are not field edits. `suspend` **does things** — kills sessions, stops jobs, records who
and why. Modelling that as `PATCH {"status": "SUSPENDED"}` says "set a field", and invites
setting any status from any other, which is how a `DELETED` school comes back to life or an
un-provisioned one goes live. One endpoint per transition also means each takes only what it
needs: a suspension needs a reason, an activation does not.

### `activate` reads the subscription rather than copying it

`SchoolStatus` and `SubscriptionStatus` are deliberately separate models, and
`models/core/README.md` says payment status "must not be stored in SchoolStatus". So #3 checks
[`SchoolSubscription`](../../models/plans/SchoolSubscription.java) at the moment of activation.

### `cancel-deletion` is a proposal, not in the model's README

`DELETION_PENDING` is defined as "requested but not yet executed", so the window exists on
purpose — and a window with no way back means one mis-click destroys a school. Drop #16 if you
prefer the strictly documented one-way path, but then say out loud that deletion is
irreversible from the moment it is requested.

## 6–9. School self-service edits

| # | Method | Path | Fields |
|---|---|---|---|
| 6 | `PATCH` | `/schools/current/profile` | `schoolName`, `phoneNumber`, `emailAddress` |
| 7 | `PUT` | `/schools/current/address` | `addressLine`, `city`, `stateOrProvince`, `postalCode` |
| 8 | `PATCH` | `/schools/current/localization` | `defaultLocale`, `defaultTimeZone` |
| 9 | `PUT` | `/schools/current/logo` | replaces `logoUrl` |

### Where `PUT` is right, and where it is not

Only **three** `PUT`s exist in this whole plan, and all three are sub-resources that are
complete values.

`PUT` means *replace this whole thing*. Correct for an **address** — patching `city` without
`stateOrProvince` produces a real address for a place that does not exist. Correct for a
**logo**, and for the **holiday calendar** (#20).

`PUT` is **wrong for either root document.** There must be no `PUT /platform/schools/{id}` and
no `PUT /academic-years/{name}`. A full replace of a resource holding `status`, `activatedAt`
and `encryptionKeyReference` hands a caller every field the document defends, and a client
omitting a field it did not know about will blank it. Partial edits by intent, always.

### `countryCode` is missing from #7 on purpose

Changing a school's country changes which tax rules and identity documents apply —
`GovernmentIdentityType` holds `AADHAAR` and `APAAR`; `FeeHead.taxRatePercent` means GST.
Schools do not move countries. Somebody mistyping at signup is the real scenario, and that is a
platform correction during `PROVISIONING`, not a self-service edit after go-live.

**Recommendation:** editable on the platform surface while `PROVISIONING`/`TRIAL`, immutable
afterwards.

### `defaultTimeZone` in #8 is the most dangerous field in this package

Every `Instant` is UTC, so changing the zone rewrites nothing — and that is the problem. It
silently reinterprets every **school-local** decision already made: which calendar date an
attendance record falls on, whether a holiday covers a day, when a timetable period starts,
which day a transport trip ran.

A school moving `Asia/Kolkata` → `Asia/Dubai` mid-year has an attendance register that shifts
under it. Warn, require confirmation, and **refuse outright once an `AcademicYear` is in
progress.** It is not a settings toggle.

### #9 logo upload

`logoUrl` is a public CDN URL, so this has two shapes: accept a URL the school already hosts,
or accept a file, store it, return the URL. **Prefer the second** — a school-supplied URL can
rot, change to something unwanted, or point at a tracker on a page parents load. If you accept
a URL, require `https` and an allow-listed host.

## 10–12. Platform-only edits

### #10 `subdomain` is its own endpoint for a reason

It is the **globally unique key that resolves a request to a tenant.** Changing it breaks every
bookmark and saved link, invalidates routing caches, and may need the old label held in reserve
so another school cannot claim it and collect the first school's logins. None of that is true
of a phone number. Burying it inside #6 alongside an email address means somebody does it by
accident. Require explicit confirmation of the old value.

**Reserved subdomains** — refuse at least: `www`, `api`, `admin`, `app`, `platform`, `status`,
`mail`, `smtp`, `ftp`, `cdn`, `static`, `assets`, `login`, `auth`, `support`, `help`, `docs`,
`blog`, `test`, `staging`, `dev`. A school that claims `api` or `login` receives traffic and
credentials meant for the platform.

#12 `encryption-key` is platform-only for the same class of reason, and must not appear on the
school surface at all.

---

# Part 2 — Academic year

All on the school surface. There is no platform surface here: a year belongs to one school's
calendar and no platform operator should be setting one.

## The rule that outranks everything else: there is no rename

**`AcademicYear.name` can never change. Not by `PATCH`, not by anything.**

Other collections do not reference this document by id. They store the **name as a string** in
their own `academicYear` field — `FeeInvoice`, `TransportTrip`, `FeedbackCampaign`,
`FacilityInspection` and dozens more. `"2026-2027"` *is* the join key.

Which means **there is no referential integrity to lean on.** A rename does not fail loudly and
does not cascade; it leaves every one of those strings naming a year that no longer answers to
it, and every row still looks perfectly valid. You would find out when a fee report came back
empty.

So:

- No endpoint accepts `name` on update. The field is not in any update DTO.
- A request containing `name` is a **`400`**, not silently ignored.
- **The URL is keyed by `name`** — `/academic-years/2026-2027`. Deliberate: it matches how the
  whole system refers to a year, and a URL that cannot change is a daily reminder that the
  thing it names cannot either.

## There is also no "set current year"

`AcademicYear` has **no `current` flag** — deliberately. The current year is derived from
`startDate` and `endDate`. Do not add an endpoint, a field, or a cached "current year" anywhere:
two sources for "which year is it" is two sources that can disagree, and the dates are already
authoritative.

This is also why the overlap check in #18 matters so much. Two overlapping years mean a date
belongs to both, and every "which year is this?" lookup gets two answers.

## 18. `POST /schools/current/academic-years`

**Validates:** `name` unique within the school; `startDate` before `endDate`; **no overlap with
any existing year**; the range is plausible — roughly a year, not three days or five.

Holidays may be supplied here or added afterwards.

## 19. `PATCH /{name}/dates`

Riskier than it looks. Moving a boundary after the year has started orphans data at both ends:
an attendance record, an invoice or a trip now sits outside the year that owns it.

**Must refuse to move a boundary past existing data.** Shrinking is the dangerous direction;
extending is usually harmless. Re-check overlap against other years on every change.

## 20–23. The holiday calendar

`holidays` is an embedded `List<HolidayDetail>` — a sub-resource keyed by `date`. Each entry
has `name`, `description`, `type` and `date`.

| # | Method | Use |
|---|---|---|
| 20 | `PUT` `/{name}/holidays` | replace the whole calendar — the bulk import case |
| 21 | `POST` `/{name}/holidays` | add one — a bandh, an unexpected closure |
| 22 | `PATCH` `/{name}/holidays/{date}` | edit one — renaming "Diwali" to "Diwali (day 2)" |
| — | `DELETE` `/{name}/holidays/{date}` | remove one — a holiday moved by notification |
| 23 | `POST` `/{name}/holidays/generate-weekly-off` | generate a weekday's offs across the year |
| — | `DELETE` `/{name}/holidays?type=WEEKLY_OFF` | clear generated offs before regenerating |

**Validate on all of them:** no duplicate dates, and every date inside the year's own
`startDate`–`endDate`.

#20 is the legitimate bulk `PUT`: a school publishes next year's calendar in one go from a
spreadsheet, and sending the complete list makes a half-imported calendar impossible.

### #23 is not a convenience — the model requires it

There is **no "weekly off day" field anywhere in this system**, deliberately. Schools in this
market may run on Sunday with the weekly off on any other day, so every non-working day is a
**dated** `HolidayDetail` with type `WEEKLY_OFF`.

That is the right model and it has a direct API consequence: a year needs roughly 52 dated
`WEEKLY_OFF` rows, and nobody is typing those in.

So #23 takes a day of the week, optionally a date range, generates one entry per occurrence,
and **skips dates that already have a holiday**. The paired bulk `DELETE` exists because the
first thing anyone does is pick the wrong weekday.

Without #23, either somebody enters 52 dates by hand or a developer eventually hardcodes Sunday
somewhere — the exact assumption the model was designed to prevent. **No service anywhere may
infer a non-working day from the weekday.**

## 24–27. Gates

`enrollmentEnabled` and `resultsLocked` are booleans, so `PATCH` would work mechanically. They
are `POST` actions because both are **gates with authorization attached**, and
`models/core/README.md` names "authorization for result locking and enrollment controls" as an
API responsibility.

**#27 `results/unlock` is the one to be careful with.** Locking results is routine; unlocking
means somebody can change a mark a parent has already seen. It needs a higher permission than
locking, a reason recorded, and an [`AuditEvent`](../../models/audit/AuditEvent.java) every
time — **including failed attempts**. A single `PATCH {"resultsLocked": false}` would make the
most sensitive operation in the package look like the least.

## 28. `POST /{name}/clone` — optional, build last or not at all

Creates the next year copying the previous one's calendar. **Its value is limited and you
should know why before building it:** most Indian school holidays are festivals on lunar dates,
so Diwali, Holi and Eid all move. Only fixed-date holidays transfer — Republic Day, Independence
Day, Gandhi Jayanti — plus the weekly-off pattern, which #23 already generates in one call.

Realistically this saves entering three dates. Listed for completeness; I would not build it
until somebody asks twice.

---

# Shared concerns

## Deleting a year has no cheap answer

Because references are strings, "is this year used anywhere?" cannot be a foreign-key check. It
is a query across every collection carrying an `academicYear` field.

**Recommendation: no hard delete at all.** A year created by mistake and never referenced can be
removed while nothing points at it; past that, it stays. `RecordState` on
[`SchoolBase`](../../models/base/SchoolBase.java) already exists for this, and hiding a year is
not the same as breaking every row that names it.

## Validation ownership

`models/core/README.md` keeps model annotations thin on purpose and hands these to the API
layer. Assigned:

| Validation | Endpoint |
|---|---|
| subdomain format, normalization, reserved words | 1, 10 |
| email lowercased, phone normalized to E.164 | 1, 6 |
| IETF language tag, IANA zone id | 1, 8 |
| ISO 3166-1 alpha-2 country | 1 only |
| `https` and allow-listed host for logo | 9 |
| allowed `SchoolStatus` transitions | 3, 4, 5, 13–17 |
| an active `SchoolSubscription` exists | 3 |
| academic-year date ordering **and overlap** | 18, 19 |
| duplicate holiday dates, dates inside the year | 20, 21, 22, 23 |
| authorization for lock/unlock and enrollment | 24–27 |
| text lengths on every free-text field | all |

## Deliberately not here

- **`DELETE` on a School.** A tenant is never removed with `DELETE`; it walks the lifecycle to
  `DELETED`. The only `DELETE`s in this package are on holidays.
- **Subscription and plan changes.** `SchoolSubscription` is its own resource with its own
  controller. `School`'s javadoc says plan data is deliberately not embedded.
- **Terms.** [`AcademicTerm`](../../models/academics/structure/AcademicTerm.java) lives in
  `academics` and gets a controller there.
- **A "current academic year" endpoint.** See above — derived from dates, never stored.
- **Bulk tenant operations.** Suspending forty schools at once is an operational script.
- **Notifications.** "Your school has been suspended", "results have been unlocked" — messages,
  and `notification` is designed last by the decision of 2026-08-14. No `notifiedAt` field, and
  nothing sends from here.
