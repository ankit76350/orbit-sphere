# controllers/core — write API plan

**Twenty of 27 endpoints are built — #1 to #10 and #18 to #27**, plus the two `DELETE`s that
pair with the calendar endpoints. Phases 1 to 5 are complete; Phase 6 has the four gates and the
subdomain change.

**Nothing here is "next".** Of the seven not built, six are deferred by decision — #12 until
something is encrypted, #13 to #17 until offboarding is actually wanted — and #28 was always
optional. The write API for `core` is done for now; the next module starts elsewhere.

**27, not 28.** #11 `account-holder` was dropped on 2026-08-31 and folded into #6 — the reasoning
is under "10–12" below. Numbering is left alone rather than closed up, because the numbers are
referenced from the Postman collection, the service banners and half the javadoc in this package. This is the complete inventory of every `POST`,
`PUT` and `PATCH` the `core` module needs, sequenced so it can be built and reviewed one step at
a time.

| Controller | Endpoints |
|---|---|
| [`SchoolPlatformController`](SchoolPlatformController.java) | #1–5, #10 |
| [`SchoolProfileController`](SchoolProfileController.java) | #6–9 |
| [`AcademicYearController`](AcademicYearController.java) | #18–27 + 2 `DELETE` |

All twenty-two requests are exercised by `postman/Orbit Sphere — API.postman_collection.json`,
which runs green end to end.

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

# Complete inventory — 27 write endpoints

**18 POST · 6 PATCH · 3 PUT** (+2 DELETE)

## School — 16

| # | Method | Path | Phase |
|---|---|---|---|
| 1 | `POST` | `/platform/schools` | 1 — **built** |
| 2 | `POST` | `/platform/schools/{id}/complete-provisioning` | 1 — **built** |
| 3 | `POST` | `/platform/schools/{id}/activate` | 2 — **built** |
| 4 | `POST` | `/platform/schools/{id}/suspend` | 2 — **built** |
| 5 | `POST` | `/platform/schools/{id}/reactivate` | 2 — **built** |
| 6 | `PATCH` | `/schools/current/profile` | 3 — **built** |
| 7 | `PUT` | `/schools/current/address` | 3 — **built** |
| 8 | `PATCH` | `/schools/current/localization` | 3 — **built** |
| 9 | `PUT` | `/schools/current/logo` | 3 — **built** |
| 10 | `PATCH` | `/platform/schools/{id}/subdomain` | 6 — **built** |
| ~~11~~ | ~~`PATCH`~~ | ~~`/platform/schools/{id}/account-holder`~~ | **dropped — folded into #6** |
| 12 | `POST` | `/platform/schools/{id}/rotate-encryption-key` | deferred — nothing is encrypted yet |
| 13 | `POST` | `/platform/schools/{id}/offboard` | 7 — deferred |
| 14 | `POST` | `/platform/schools/{id}/close` | 7 — deferred |
| 15 | `POST` | `/platform/schools/{id}/request-deletion` | 7 — deferred |
| 16 | `POST` | `/platform/schools/{id}/cancel-deletion` | 7 — deferred |
| 17 | `POST` | `/platform/schools/{id}/confirm-deletion` | 7 — deferred |

## Academic year — 11

All paths below are under `/schools/current/academic-years`.

| # | Method | Path | Phase |
|---|---|---|---|
| 18 | `POST` | `/` | 4 — **built** |
| 19 | `PATCH` | `/{name}/dates` | 4 — **built** |
| 20 | `PUT` | `/{name}/holidays` | 5 — **built** |
| 21 | `POST` | `/{name}/holidays` | 5 — **built** |
| 22 | `PATCH` | `/{name}/holidays/{date}?type=` | 5 — **built** |
| — | `DELETE` | `/{name}/holidays/{date}?type=` | 5 — **built** |
| 23 | `POST` | `/{name}/holidays/generate-weekly-off` | 5 — **built** |
| — | `DELETE` | `/{name}/holidays?type=` | 5 — **built** |
| 24 | `POST` | `/{name}/enrollment/enable` | 6 — **built** |
| 25 | `POST` | `/{name}/enrollment/disable` | 6 — **built** |
| 26 | `POST` | `/{name}/results/lock` | 6 — **built** |
| 27 | `POST` | `/{name}/results/unlock` | 6 — **built** |
| 28 | `POST` | `/{name}/clone` | 8 — optional, see below |

---

# Build order

Sequenced by dependency first, then by risk. **Phase 0 is not optional and not skippable.**

| Phase | What | Endpoints | |
|---|---|---|---|
| **0** | Foundations — no endpoints | — | built |
| **1** | Create a tenant | 1, 2 | built |
| **2** | Tenant lifecycle | 3, 4, 5 | built |
| **3** | School self-service edits | 6, 7, 8, 9 | built |
| **4** | Academic year exists | 18, 19 | built |
| **5** | The holiday calendar | 20–23 + 2 `DELETE` | built |
| **6** | Gates and sensitive edits | 10, 24–27 **built**; 12 deferred | part |
| **7** | Offboarding and deletion | 13–17 | deferred |
| **8** | Convenience | 28 | |

**Why this order:** nothing exists until phase 1. Phase 2 makes a tenant usable. Phase 3 is the
first thing a real school touches. Phases 4–5 give the school a calendar, which attendance,
timetable, transport and fees all read. Phase 6 groups everything needing elevated permission.
Phase 7 is last because it is rarely used and the most destructive — build it when the rest is
proven.

## Phase 0 — the plumbing every endpoint assumes

Three pieces. Getting them wrong is expensive to undo. **All three are now built. Idempotency,
which sits inside 0.3, is not.**

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

**0.2 — Tenant resolution. BUILT, as a stand-in.**
[`CurrentSchoolResolver`](../../common/current/CurrentSchoolResolver.java) is the single place
every `/schools/current` endpoint learns which school is asking. It reads an
`X-School-Subdomain` header today and will read the session tomorrow; because it is one class,
that swap touches nothing else.

**It is not safe yet, and the danger is worth stating plainly: any caller can set that header
to any school's subdomain, so anybody can edit any school.** Fine on a developer machine,
unacceptable anywhere reachable. The request reaches it through Spring's request-scoped proxy
and a thread-local, which is why the services take no request parameter — and why this cannot
be called from a background thread.

**0.3 — Error contract. BUILT.** `ApiError` is the single response shape. One `ApiException`
carries its own status through a single `@ExceptionHandler`, replacing the three near-identical
exception classes that were there first. `409` means "well-formed request, wrong state" against
`400` for "malformed", and every transition below leans on that distinction.
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

## 2. `POST /platform/schools/{id}/complete-provisioning` — BUILT

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

## 3–5, 13–17. Lifecycle transitions — 3, 4, 5 BUILT · 13–17 DEFERRED

```text
TRIAL / PROVISIONING -> ACTIVE
ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE -> OFFBOARDING -> CLOSED -> DELETION_PENDING -> DELETED
```

Anything not on that list is a **`409`**, not a `400` — the request is well-formed, the tenant
is just not in a state where it makes sense.

| # | Endpoint | Requires | Side effects |
|---|---|---|---|
| 3 | `activate` | #2 already run — checked. Subscription — **partially**, see below | stamps `activatedAt` on first activation only |
| 4 | `suspend` | a reason, stored on `School.statusReason` | stamps `suspendedAt`. **Does not yet revoke sessions or stop jobs** |
| 5 | `reactivate` | — | restores access; does **not** clear `suspendedAt` or `statusReason`, does **not** re-stamp `activatedAt` |
| ~~13~~ | `offboard` | a reason | starts data export — **deferred** |
| ~~14~~ | `close` | export complete | tenant no longer reachable — **deferred** |
| ~~15~~ | `request-deletion` | explicit confirmation | starts the retention clock — **deferred** |
| ~~16~~ | `cancel-deletion` | — | back to `CLOSED` — **deferred** |
| ~~17~~ | `confirm-deletion` | second confirmation | irreversible — **deferred** |

### 13 to 17 are deferred, and here is what that leaves true

Deferred on 2026-08-31, by decision — not blocked on anything. The design above stands and is
what to build from when offboarding is wanted.

**Half the lifecycle has no way to be entered.** Of the eight `SchoolStatus` values, four are now
unreachable through the API:

| Status | Reached by |
|---|---|
| `PROVISIONING` | #1 — every school starts here |
| `TRIAL` | #1 with `trial: true` |
| `ACTIVE` | #3, #5 |
| `SUSPENDED` | #4 |
| `OFFBOARDING` | nothing |
| `CLOSED` | nothing |
| `DELETION_PENDING` | nothing |
| `DELETED` | nothing |

**So a school cannot currently be got rid of.** There is no close, no delete, and no export.
`SUSPENDED` is as far as a tenant can be pushed, and #10 already refuses to edit a school at
`DELETED` or `DELETION_PENDING` — a guard against states nothing can currently produce, which is
correct and will stop looking odd the day these are built.

**The consequence to remember is the export, not the delete.** #13 starts a data export, and a
school leaving is entitled to its own records. Until #13 exists, an offboarding school has no
supported way to take its data with it — that is the gap worth flagging to whoever asks for
these, not the missing status transition.

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

### What #3 to #5 actually enforce, where it differs from this plan

**The subscription check on #3 is deliberately soft, and says so in its own response.** This
plan requires an active subscription to activate. Nothing in the system creates a subscription
— there is no endpoint, no service, no repository until one was added for this check — so
enforcing it strictly would mean **every activation returns 409 and #3 could never be used.**

So it enforces what it can: a subscription that exists and is `CANCELLED` or `EXPIRED` blocks
activation; `ACTIVE`, `TRIAL`, `PAST_DUE` and `SUSPENDED` all pass. A school with **no**
subscription row activates, and the response carries:

```json
"subscriptionStatus": "NONE",
"subscriptionNote": "No subscription exists for this school. Activation was allowed anyway
                     because nothing creates subscriptions yet — this check must become a
                     hard requirement once it does."
```

Announcing it on every call is the point. A silently skipped check becomes permanent because
nobody sees it. **Tighten this the day the subscription endpoints exist.**

**#4 required a model change.** `School` had nowhere to store why a school was suspended, so
requiring a reason would have meant validating a string and discarding it. `statusReason` was
added to `School` on 2026-08-27, matching the sixteen other models that already carried one —
`Vendor`, `FacilityResource`, `UserAccount` and the rest. School was the only one without it.

**#4 does not lock anything yet.** It sets a status and a date. Live `AuthSession` rows are not
revoked and scheduled jobs are not stopped, because neither service exists. **A suspended
school's users stay logged in until their tokens expire — suspension is currently a flag, not a
lock.** Wire both in when sessions are built.

**#5 keeps the history on purpose.** `suspendedAt` is not cleared and `activatedAt` is not
re-stamped, so a school suspended in June and brought back in July still reads as having gone
live in April, with a suspension on record. It also skips #3's setup and subscription checks: a
suspended school was already live once, so it passed them, and re-running them would mean a
school suspended for non-payment could never be let back in as a goodwill gesture — which is
what reactivation is for.

### `cancel-deletion` is a proposal, not in the model's README

`DELETION_PENDING` is defined as "requested but not yet executed", so the window exists on
purpose — and a window with no way back means one mis-click destroys a school. Drop #16 if you
prefer the strictly documented one-way path, but then say out loud that deletion is
irreversible from the moment it is requested.

## 6–9. School self-service edits — ALL BUILT

| # | Method | Path | Fields |
|---|---|---|---|
| 6 | `PATCH` | `/schools/current/profile` | `schoolName`, `accountHolderName`, `phoneNumber`, `emailAddress` |
| 7 | `PUT` | `/schools/current/address` | `addressLine`, `city`, `stateOrProvince`, `postalCode` |
| 8 | `PATCH` | `/schools/current/localization` | `defaultLocale`, `defaultTimeZone` |
| 9 | `PUT` | `/schools/current/logo` | replaces `logoUrl` |

All four resolve the tenant through
[`CurrentSchoolResolver`](../../common/current/CurrentSchoolResolver.java) and refuse a school
that is not `ACTIVE`, `TRIAL` or `PROVISIONING` — a closed or suspended tenant should not be
quietly edited by its own admin while the suspension is being argued about.

### The PATCH convention these needed, which this plan had not settled

A Java record cannot tell *"the caller omitted this field"* from *"the caller sent null"* —
Jackson gives you null for both. Without a rule, an optional field could **never be cleared**:
every PATCH that omitted a phone number would look identical to one asking to remove it, and the
safe reading — leave it alone — would win forever.

So, on #6 and #8:

```
field omitted, or null   -> leave it exactly as it is
field is ""              -> clear it (null in the database)
field has a value        -> replace it
```

`schoolName` and `defaultLocale` are exceptions: both are `@NotBlank` on the model, so `""`
there is a `400` rather than a deletion. And a PATCH whose body asks for nothing is a `400`
`NOTHING_TO_UPDATE`, not a silent `200` — an empty body is almost always a client bug.

**#7 is the opposite, and that is what PUT means.** An omitted field is *cleared*, not kept:
`PUT {"city": "Mumbai"}` wipes `addressLine`, `stateOrProvince` and `postalCode`. Worth knowing
before writing a client against it.

### #8 has two guards, not one

The plan said to warn, confirm, and refuse a time-zone change once an academic year is running.
Both are implemented, and both are needed:

1. `confirmTimeZoneChange: true` must be sent, or `409 TIME_ZONE_CHANGE_NOT_CONFIRMED`.
2. If an academic year covers today, the change is refused outright —
   `409 ACADEMIC_YEAR_IN_PROGRESS`.

The flag alone would be theatre; people tick boxes. The second is what actually protects the
attendance register. `AcademicYearRepository` was added for it, and asks the dates rather than a
`current` flag — because [`AcademicYear`](../../models/core/AcademicYear.java) deliberately has
no such flag.

The locale stays editable while a year runs. Only the zone is dangerous.

### #9 takes a URL, and validates more than the plan asked

`https` only, and the host must be on an allow-list held in the service. A school-supplied URL
is loaded on pages parents open, so an arbitrary one is somebody else's server deciding what
parents see, and a tracker there is invisible to us. `logoUrl: ""` removes the logo, which is
why there is no separate `DELETE`.

A file upload would still be better, for the reason the plan gives. There is no storage service
yet.

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

## 10. `PATCH /platform/schools/{id}/subdomain` — BUILT

The subdomain is the **globally unique key that resolves a request to a tenant** —
[`CurrentSchoolResolver`](../../common/current/CurrentSchoolResolver.java) finds a school by it
on every `/schools/current` call. That is why it is here and not in #6: changing it is not a
detail edit, it is moving the school's address.

The immediate need is duller than rebranding. `subdomain` was set once at #1 and nothing could
ever change it, so **a typo at provisioning had no fix but a database edit.**

### The body confirms the current subdomain

`currentSubdomain` must match what the school answers to today, or the request is `409`
`SUBDOMAIN_CONFIRMATION_MISMATCH`. Nothing reads the value — it exists so that the one endpoint
that can take a tenant off the air cannot be aimed at the wrong one by a mis-pasted id.

It is the only endpoint in this package with a confirmation field, and that asymmetry is the
point: every other platform endpoint changes one field of one school, and being wrong is
recoverable.

### What it refuses

| Case | Code |
|---|---|
| id names no school | `404 SCHOOL_NOT_FOUND` |
| school is `DELETED` or `DELETION_PENDING` | `409 SCHOOL_NOT_EDITABLE` |
| confirmation does not match | `409 SUBDOMAIN_CONFIRMATION_MISMATCH` |
| bad shape, or a reserved word | `409 SUBDOMAIN_INVALID` / `SUBDOMAIN_RESERVED` |
| same as the current one | `409 SUBDOMAIN_UNCHANGED` |
| already in use | `409 SUBDOMAIN_TAKEN` |

Shape and reserved words come from
[`CoreValidator.validateSubdomain`](../../services/core/helper/CoreValidator.java), shared with
#1 — so a label refused at provisioning is refused here, without a second list to keep in step.

**Reserved subdomains** — the list refuses `www`, `api`, `admin`, `app`, `platform`, `status`,
`mail`, `smtp`, `ftp`, `cdn`, `static`, `assets`, `login`, `auth`, `support`, `help`, `docs`,
`blog`, `test`, `staging`, `dev` and more. A school that claimed `api` or `login` would receive
traffic and credentials meant for the platform.

### What it does not do, and the response says so

Nothing invalidates a routing cache, rewrites a stored link, or tells anyone at the school their
address changed. **The old label is released immediately** — no reservation — so the next school
to ask can have it, and every bookmark, saved link and stored login pointing at it is simply
dead. The response's `nextStep` states all of that, because a caller who does not know it will
find out from the school.

## ~~11.~~ `account-holder` — dropped, folded into #6

`accountHolderName` is a plain `String` on [`School`](../../models/core/School.java). Nothing
links it to a `UserAccount`, nothing reads it, and nothing is granted by it — changing it grants
no one anything and revokes nothing. That makes it the same class of edit as `schoolName`, which
#6 already handles, and a platform-only endpoint for one unreferenced string was ceremony.

It is now a field on #6, refusing `""` the same way `schoolName` does.

**If it ever becomes contractual** — who signed, who gets billed — the fix is to link the field
to a real account, not to move it back out to its own endpoint. At that point it belongs with
[`SchoolSubscription`](../../models/plans/SchoolSubscription.java) rather than here.

## 12. `encryption-key` — deferred, and it is not a `PATCH`

`encryptionKeyReference` appears **nowhere in the code** — only on the model and in
`models/core/README.md`. Nothing encrypts anything yet, so an endpoint to change the key would be
ceremony around a field no code reads.

It matters later, because the fields waiting on it are the most sensitive in the system:

| Field | Model |
|---|---|
| `encryptedIdentityNumber` | `StudentGovernmentIdentity`, `StaffGovernmentIdentity` |
| `encryptedAccountNumber` | `StaffBankAccount`, `VendorBankAccount` |
| `encryptedCredentialNumber` | `StaffCredential` |
| `encryptedSubmitterReference` | `FeedbackSubmission`, `FeedbackReport` |
| `encryptedPayload` | `BillingWebhookEvent` |

Aadhaar numbers, bank accounts, and the identity behind an anonymous complaint.

**Which is why the planned shape was wrong.** A `PATCH` that swaps the reference and returns
`200` makes every existing ciphertext unreadable — silently, with every document still looking
perfectly valid, discovered whenever somebody next opens a student's identity record. Rotation is
not a field update: it re-encrypts under the new key, or reads old and new through a migration
window.

So when it is built it is **`POST /platform/schools/{id}/rotate-encryption-key`** with a
re-encryption step, not a `PATCH` of a string. Build it after something is actually encrypted,
never before.

Platform-only either way. It must not appear on the school surface at all.

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

## 18. `POST /schools/current/academic-years` — BUILT

**Validates:** `name` unique within the school; `startDate` before `endDate`; **no overlap with
any existing year**; the range is plausible — 30 to 800 days, outside which it is a typo rather
than a calendar.

**Holidays are not accepted here.** The plan originally allowed supplying them at creation; that
was removed on 2026-08-31. A year is always created with an empty calendar and filled through
#20 to #23.

The reason is that one request could otherwise fail for two unrelated things — a bad date range
or a stray holiday — leaving the caller to work out which, and doubling the create endpoint's
validation surface for a convenience the calendar endpoints already provide. A `holidays` array
sent to this endpoint is ignored rather than honoured, because the field is not on the DTO.

**The overlap test is written as "unless one ends before the other starts"**, not as four date
comparisons. The four-way version is where off-by-one bugs live, and adjacency must stay legal:
a year ending 03-31 and the next starting 04-01 do not overlap.

**No shape is enforced on `name`.** A school may use `2026-2027`, `2026-27` or `AY2026-27`.
Imposing one would be this platform deciding something that is not its business; it only has to
be unique and never change.

**`current` is computed in the response, never stored.** That is the same rule as the model —
two sources for "which year is it" is two that can disagree — and it is why the overlap check
matters: two years covering one day would make the flag true for both.

## 19. `PATCH /{name}/dates` — BUILT

Riskier than it looks. Moving a boundary after the year has started orphans data at both ends:
an attendance record, an invoice or a trip now sits outside the year that owns it.

**`name` is absent from the request**, not optional and not ignored. A request that includes one
is rejected by the DTO shape itself rather than quietly dropped.

**What it does check:** the new range is still plausible, still does not overlap another year,
and still contains **every closed day already on the year**. Shrinking past one is a `409`
`HOLIDAYS_OUTSIDE_NEW_RANGE`, naming the first stranded date and every reason on it — since the
restructure a date can be closed for more than one, and naming a single holiday would have
under-reported what the shrink was about to strand.

**What it does not check, and should.** Only holidays. Attendance records, invoices and trips
reference a year by *name string*, in collections that have no repository yet — so a shrink can
still orphan those, silently, and nothing anywhere will complain. When those repositories exist,
this must refuse to move a boundary past the earliest or latest row referencing the year. It is
recorded in the method's javadoc and in the response's `nextStep`.

## 20–23. The holiday calendar — ALL BUILT

`holidays` is an embedded `List<`[`HolidayDetail`](../../models/core/embedded/HolidayDetail.java)`>`,
a sub-resource keyed by **date**. Each entry holds the date and a list of
[`HolidayEvent`](../../models/core/embedded/HolidayEvent.java) — the reasons the school is shut
that day.

| # | Method | Use |
|---|---|---|
| 20 | `PUT` `/{name}/holidays` | replace the whole calendar — the bulk import case |
| 21 | `POST` `/{name}/holidays` | add one reason — a bandh, an unexpected closure |
| 22 | `PATCH` `/{name}/holidays/{date}?type=` | edit one reason on a day |
| — | `DELETE` `/{name}/holidays/{date}?type=` | remove one reason, or the whole day |
| 23 | `POST` `/{name}/holidays/generate-weekly-off` | generate a weekday's offs across the year |
| — | `DELETE` `/{name}/holidays?type=` | clear every entry of one type before regenerating |

### One date, several reasons

`HolidayDetail` was restructured on 2026-08-31. It used to be a flat row — name, description,
type, date — one per reason, so a date could appear several times and there was no single answer
to *is the school open on the 8th*.

Now the **date is the key** and the reasons hang off it. A Sunday that is also Holi is one closed
day with two events, not two days sharing a date. Attendance, timetables, transport and fee due
dates all ask that same question, and it is now one lookup returning one entry rather than a scan
that must not stop at the first match.

The API is shaped around that, and three consequences run through all six endpoints:

**Requests are flat, storage is grouped.** A caller sends one row per reason, with its date —
which is what a spreadsheet holds and what a person adding Holi knows. The service groups by
date. So two rows sharing a date is *not* a duplicate; the same **type** twice on one date is.
Without this, adding Holi to a Sunday would mean fetching the day, appending to its array and
sending the whole thing back.

**`?type=` says which reason.** #22 and the single `DELETE` can no longer be addressed by date
alone. The parameter is optional when the day has one reason and required when it has more — and
the error lists what is on that day, so the next request is obvious. Picking the first of two
would be wrong as often as right, and invisible when it was.

**Two counts come back.** `closedDayCount` is how many days the school is shut — what attendance
and fees care about. `eventCount` is how many reasons are recorded. They differ wherever a day
carries more than one, and reporting only one number would make a Sunday that is also Holi look
like either a lost entry or an extra closed day. `countsByType` counts **reasons**, so "how many
festivals" is not reduced by the ones that landed on a Sunday.

**Validated on all of them:** every date inside the year's own `startDate`–`endDate`, and no two
entries of the same type on one date.

#20 is the legitimate bulk `PUT`: a school publishes next year's calendar in one go from a
spreadsheet, and sending the complete list makes a half-imported calendar impossible.

### #23 is not a convenience — the model requires it

There is **no "weekly off day" field anywhere in this system**, deliberately. Schools in this
market may run on Sunday with the weekly off on any other day, so every non-working day is a
**dated** entry with type `WEEKLY_OFF`.

That is the right model and it has a direct API consequence: a year needs roughly 52 dated
`WEEKLY_OFF` entries, and nobody is typing those in.

So #23 takes a day of the week, optionally a date range, and generates one entry per occurrence.
**A date that already has a festival still gets its weekly off** — the day ends up with both
reasons. The school was closed for Diwali *and* it was their weekly off, and a report knowing
only one of those is wrong about the other. Only a date that already carries a `WEEKLY_OFF` is
skipped, which is also what makes running it twice safe.

Without #23, either somebody enters 52 dates by hand or a developer eventually hardcodes Sunday
somewhere — the exact assumption the model was designed to prevent. **No service anywhere may
infer a non-working day from the weekday.**

### The two `DELETE`s are not extras

They were listed uncounted in the original plan; both are now built, because an API that creates
52 rows in one call and cannot remove them is not finished.

The single `DELETE` removes one reason with `?type=`, or the whole day without it. **Removing the
last reason removes the day** — a closed day with nothing saying why reads as corruption to
whoever finds it.

The bulk `DELETE` strips one type across the calendar and drops only the days left with nothing.
A Sunday that was also Holi survives as Holi. Its `type` is **required**: a bulk delete that
cleared the whole calendar when a query parameter was forgotten would be the most destructive
accident in this package. That guard needed a `MissingServletRequestParameterException` handler
in [`GlobalExceptionHandler`](../../common/error/GlobalExceptionHandler.java), or Spring answered
with its own error page — a stack trace in dev, and nothing resembling the rest of our errors
anywhere else. A `MethodArgumentTypeMismatchException` handler went in beside it, so a misspelled
`?type=WEEKLYOFF` comes back naming the accepted values.

## 24–27. Gates — ALL BUILT

`enrollmentEnabled` and `resultsLocked` are booleans, so `PATCH` would work mechanically. They
are `POST` actions because both are **gates**: they change what other modules may do — admissions
may write, examinations may not — rather than editing the year's own data.

All four live in [`AcademicYearService`](../../services/core/AcademicYearService.java) with the
rest of the year's endpoints; one resource, one service.

| # | What it gates | Body | Idempotent |
|---|---|---|---|
| 24 | admissions may write to this year | none | yes |
| 25 | admissions may not | none | yes |
| 26 | results are frozen | none | yes |
| 27 | results are editable again | none | yes |

**All four are idempotent and flip freely.** A year already in the asked-for state comes back
`200` saying so. Refusing a retry would only teach callers to read first and then race.

**The two pairs are independent.** Locking results does not touch enrollment, and vice versa.

**#25 does not touch students already enrolled.** It is a gate on new writes, not a withdrawal.

### #27 is built simple, and that is a debt with a name

Unlocking lets somebody change a mark a parent has already seen. As built, **the endpoint records
nothing about who unlocked, or why** — no reason is asked for, and no trace is left.

That is a deliberate simplification, not an oversight. There is no authentication, so an audit
row could not name who acted anyway, and a trail whose every entry says "unknown" is close to
worthless. What it must gain before results are real is written down here so it does not have to
be worked out twice:

**A required reason on #27**, stored. It is the only part of an audit row a person can read six
months later and understand — everything else says what happened, and this says why.

**An audit row on every call, refusals included** — and this is the part with a trap in it. A
refusal ends in a thrown exception, which rolls the transaction back and would take the evidence
with it. **A denial recorded inside the transaction that denies it is never written.** It needs
two write paths: one joining the caller's transaction, so a change and its evidence commit
together; and one running `REQUIRES_NEW`, so a refusal survives the rollback that caused it.

**#26 audited too**, though locking is the safe direction. A lock alone is uninteresting; a lock
and an unlock **together** say how long results were open and who opened them.

**`action` naming the operation, not the outcome** — `RESULTS_UNLOCK` whether it succeeded or was
refused, with `outcome` carrying the result — so one query returns every attempt. Two actions
would mean a search for unlocks quietly missing the failures.

**And then #27 should stop being idempotent.** It is idempotent today because with no trail a
no-op is harmless. Once there is one, unlocking a year that is not locked should be a `409`: a
no-op logged as a successful unlock puts a row in the trail claiming results were opened when
they never were.

### `audit_events` will need a writer, not a repository

A `MongoRepository` cannot be insert-only — it arrives with `save`, `delete` and `deleteAll`, and
`save` silently becomes an update when the id is set.
[`AuditEvent`](../../models/audit/AuditEvent.java)'s own javadoc asks for an insert-only
interface, so the writer should call `MongoTemplate.insert`, which fails on a duplicate id rather
than overwriting. History then cannot be edited even by mistake.

### The authorization gap all four announce

**There is no authorization.** Anybody who can reach these can unlock a year's results. The
permission model does not exist, so there is nothing to check against — every response says so in
its `nextStep`, the same way #3 announces its subscription gap, so the hole is visible in every
response rather than only in this file.

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
| allowed `SchoolStatus` transitions | 3, 4, 5 (13–17 deferred) |
| an active `SchoolSubscription` exists | 3 |
| academic-year date ordering **and overlap** | 18, 19 |
| one reason of each type per date, dates inside the year | 20, 21, 22, 23 |
| authorization for lock/unlock and enrollment | 24–27 — **not built, see above** |
| text lengths on every free-text field | all |

**Query parameters are validated globally, not per endpoint.** A missing required parameter and
a misspelled enum value are the same two mistakes on every endpoint that takes one, so
[`GlobalExceptionHandler`](../../common/error/GlobalExceptionHandler.java) answers both in our
error shape — `MISSING_PARAMETER` and `INVALID_PARAMETER`, the latter listing the accepted
values. Left to Spring they arrive as its own error page, which in dev carries a full stack
trace. #22 and the two calendar `DELETE`s are the first endpoints here to take one; they will
not be the last.

## Deliberately not here

- **`DELETE` on a School.** A tenant is never removed with `DELETE`; it walks the lifecycle to
  `DELETED` — through #13 to #17, which are deferred, so **today there is no way to remove one at
  all.** The only `DELETE`s in this package are on holidays.
- **Subscription and plan changes.** `SchoolSubscription` is its own resource with its own
  controller. `School`'s javadoc says plan data is deliberately not embedded.
- **Terms.** [`AcademicTerm`](../../models/academics/structure/AcademicTerm.java) lives in
  `academics` and gets a controller there.
- **A "current academic year" endpoint.** See above — derived from dates, never stored.
- **Bulk tenant operations.** Suspending forty schools at once is an operational script.
- **Notifications.** "Your school has been suspended", "results have been unlocked" — messages,
  and `notification` is designed last by the decision of 2026-08-14. No `notifiedAt` field, and
  nothing sends from here.
