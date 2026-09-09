# controllers/core — API plan

**Twenty-two writes and nine reads are built.** What is left is deferred by decision, not waiting
on anything: #12 until something is encrypted, #13 to #17 until offboarding is actually wanted,
and #28 and G11 were always marked optional.

Mirrors [`models/core`](../../models/core), whose README already describes the two documents, the
status workflow and the validation split. **These endpoints enforce that file. They do not invent
new rules.**

---

## What this module is

The tenant itself, and the school's calendar. Everything else in Orbit Sphere hangs off these two
documents: nothing can exist without a `School` above it, and almost nothing academic means
anything without knowing which year it belongs to and which days the school is open.

| Document | Collection | What it holds |
|---|---|---|
| [`School`](../../models/core/School.java) | [`schools`](../../models/core/School.java) | the tenant root — the only document in the system with no `schoolId` |
| [`AcademicYear`](../../models/core/AcademicYear.java) | [`academic_years`](../../models/core/AcademicYear.java) | a named year, and the school's holiday calendar embedded in it |

Three collections owned by other modules are written or read from here, and are linked in the
tables below where that happens: [`roles`](../../models/identity/Role.java) and
[`number_sequences`](../../models/institution/NumberSequence.java), both seeded by #2, and
[`school_subscriptions`](../../models/plans/SchoolSubscription.java), which #3 reads and never
copies.

## Two surfaces, and why

| Surface | Base path | Who is calling | Tenant comes from |
|---|---|---|---|
| **Platform** | `/platform/schools` | our operator | the `{id}` in the URL — they are outside the tenant |
| **School** | `/schools/current` | the school itself | [`CurrentSchoolResolver`](../../common/current/CurrentSchoolResolver.java), never the URL |

**The school is never named in the URL on its own surface.** A path parameter invites the bug
where a school admin passes somebody else's id and edits their school. Resolving the tenant
outside the request path makes that structurally impossible — a caller cannot name a school they
do not belong to, because they never name one at all. Keep it that way when the header is replaced
by a session.

---

# The endpoints

**The numbers are #1 to #28 and G1 to G11, and they are not renumbered.** They are quoted from the
Postman collection, the service banners and half the javadoc in this package, so closing the gaps
left by dropped endpoints would break every one of those references. `D1` and `D2` are the two
`DELETE`s, which the original plan left unnumbered.

## 1. School — the platform surface · [Build order ↓](#build-order)

Provisioning and the tenant lifecycle. The caller is outside the tenant — when #1 runs there is no
user, staff record, role or session belonging to that school yet — so the school is named in the
URL and this cannot sit behind the same authentication as everything else.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>#1 | [`POST /platform/schools`](#e1) | **Built.** Create a tenant. This is the first thing that happens for a new customer, and it is the only document in the system with no `schoolId` above it. | [`schools`](../../models/core/School.java) |
| <a id="t2"></a>#2 | [`POST /platform/schools/{id}/complete-provisioning`](#e2) | **Built.** Finish the setup #1 leaves undone: create every missing number sequence and the starting roles. Safe to run twice — it only fills in what is missing. | [`schools`](../../models/core/School.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`roles`](../../models/identity/Role.java) |
| <a id="t3"></a>#3 | [`POST /platform/schools/{id}/activate`](#e3) | **Built.** Take the school live. Refuses unless #2 has actually run, because a school with no `SCHOOL_ADMIN` role or missing sequences fails on first use rather than at activation. | [`schools`](../../models/core/School.java), [`roles`](../../models/identity/Role.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t4"></a>#4 | [`POST /platform/schools/{id}/suspend`](#e4) | **Built.** Block a school and record why. Only an `ACTIVE` school can be suspended. | [`schools`](../../models/core/School.java) |
| <a id="t5"></a>#5 | [`POST /platform/schools/{id}/reactivate`](#e5) | **Built.** Put a suspended school back. `suspendedAt` and the old reason are kept on purpose, as the record of the last suspension. | [`schools`](../../models/core/School.java) |
| <a id="t10"></a>#10 | [`PATCH /platform/schools/{id}/subdomain`](#e10) | **Built.** Change the label a school answers to. On the platform surface because this is the key that resolves every request to the tenant, not a profile detail. | [`schools`](../../models/core/School.java) |
| <a id="t12"></a>#12 | [`POST /platform/schools/{id}/rotate-encryption-key`](#e12) | **Deferred.** Point the school at a new key. Deferred because nothing is encrypted yet, so there is no key to rotate. | [`schools`](../../models/core/School.java) |
| <a id="t13"></a>#13 | [`POST /platform/schools/{id}/offboard`](#e13) | **Deferred.** Start winding a school down. Deferred until offboarding is actually wanted. | [`schools`](../../models/core/School.java) |
| <a id="t14"></a>#14 | [`POST /platform/schools/{id}/close`](#e14) | **Deferred.** Close a school that has finished offboarding. | [`schools`](../../models/core/School.java) |
| <a id="t15"></a>#15 | [`POST /platform/schools/{id}/request-deletion`](#e15) | **Deferred.** Ask for the school's data to be erased, starting a waiting period. | [`schools`](../../models/core/School.java) |
| <a id="t16"></a>#16 | [`POST /platform/schools/{id}/cancel-deletion`](#e16) | **Deferred.** Change our mind during the waiting period. Not in the model's README — proposed here. | [`schools`](../../models/core/School.java) |
| <a id="t17"></a>#17 | [`POST /platform/schools/{id}/confirm-deletion`](#e17) | **Deferred.** Actually erase the data once the waiting period is over. | [`schools`](../../models/core/School.java) |

## 2. School — the school's own surface · [Build order ↓](#build-order)

A school editing itself. Nothing here can reach `status`, `subdomain` or `encryptionKeyReference`
— the methods do not exist and the fields are not on the DTOs.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t6"></a>#6 | [`PATCH /schools/current/profile`](#e6) | **Built.** The school edits its own name and contact details. Partial: a missing field is left alone, an empty string clears it, an empty body is a 400. | [`schools`](../../models/core/School.java) |
| <a id="t7"></a>#7 | [`PUT /schools/current/address`](#e7) | **Built.** Replace the whole postal address. All-or-nothing on purpose: a patched address can name a city in the wrong state. | [`schools`](../../models/core/School.java) |
| <a id="t8"></a>#8 | [`PATCH /schools/current/localization`](#e8) | **Built.** Set language and time zone. The time zone reinterprets which calendar date every existing attendance record and holiday falls on, so it needs confirming and is refused mid-year. | [`schools`](../../models/core/School.java), [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t9"></a>#9 | [`PUT /schools/current/logo`](#e9) | **Built.** Replace the logo, or remove it when the URL is blank. https and an allow-listed host only. | [`schools`](../../models/core/School.java) |
| <a id="t11"></a>~~#11~~ | [~~PATCH /platform/schools/{id}/account-holder~~](#e11) | **Dropped.** Dropped on 2026-08-31 and folded into #6. The account holder's name is an ordinary profile field, and a second endpoint for one string is a second thing to keep in step. | [`schools`](../../models/core/School.java) |

## 3. Academic year — writes · [Build order ↓](#build-order)

School surface only: a year belongs to one school's calendar and no operator should be setting
one. **There is no rename and no `DELETE`** — see the two notes below the table.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t18"></a>#18 | [`POST /schools/current/academic-years`](#e18) | **Built.** Create a year with an empty calendar. **The name can never change** — every other collection stores it as a string, so a rename would orphan them all silently. | [`schools`](../../models/core/School.java), [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t19"></a>#19 | [`PATCH /schools/current/academic-years/{name}/dates`](#e19) | **Built.** Move the start or end date. Refused when a closed day would end up outside the new range, because a holiday stored outside its year is invisible to every query that asks about it. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t20"></a>#20 | [`PUT /schools/current/academic-years/{name}/holidays`](#e20) | **Built.** Replace the whole calendar in one go. What a school does when it has the year's holiday list from the board and wants it in as one action. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t21"></a>#21 | [`POST /schools/current/academic-years/{name}/holidays`](#e21) | **Built.** Add one closed day, or add a second reason to a day that is already closed. A Sunday that is also Diwali is one day with two reasons. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t22"></a>#22 | [`PATCH /schools/current/academic-years/{name}/holidays/{date}?type=`](#e22) | **Built.** Rename a reason, change its description, or change its type. The date itself cannot move — remove it and add it back instead. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="td1"></a>D1 | [`DELETE /schools/current/academic-years/{name}/holidays/{date}?type=`](#ed1) | **Built.** Remove one reason from a day, or the whole day when no type is given. A day that loses its last reason becomes a working day again. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t23"></a>#23 | [`POST /schools/current/academic-years/{name}/holidays/generate-weekly-off`](#e23) | **Built.** Turn "we are closed on Sundays" into the ~52 dated entries the model requires. Not a convenience: every closure has to be a real date, so without this somebody types 52 of them. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="td2"></a>D2 | [`DELETE /schools/current/academic-years/{name}/holidays?type=`](#ed2) | **Built.** Clear every closure of one type. `type` is required precisely because forgetting it must not wipe a whole calendar. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t24"></a>#24 | [`POST /schools/current/academic-years/{name}/enrollment/enable`](#e24) | **Built.** Open the year to new enrollments. Idempotent — already open comes back 200 saying so. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t25"></a>#25 | [`POST /schools/current/academic-years/{name}/enrollment/disable`](#e25) | **Built.** Close the year to new enrollments. A gate on new writes only — students already enrolled are untouched. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t26"></a>#26 | [`POST /schools/current/academic-years/{name}/results/lock`](#e26) | **Built.** Lock results against further change. What happens when marks are published. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t27"></a>#27 | [`POST /schools/current/academic-years/{name}/results/unlock`](#e27) | **Built.** Unlock results so they can be corrected. **Records nothing about who unlocked, or why** — see the debt noted below. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t29"></a>#29 | [`POST /schools/current/academic-years/{name}/end`](#e29) | **Built.** End the year today: stop it running and close its dates on today, in the school's own timezone. **Refuses to extend** a year that already finished, and refuses one that has not started. Allows a year cut very short, which [#19](#t19) rejects as implausible. Re-ending is a 200 saying nothing changed. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="t28"></a>#28 | [`POST /schools/current/academic-years/{name}/clone`](#e28) | **Optional.** Copy last year's calendar into a new year, so a school does not re-enter it. Convenience only — #18 plus #20 already do it. | [`academic_years`](../../models/core/AcademicYear.java) |

## 4. Reads — platform · [Build order ↓](#build-order)

The operator's console. G1 is the only list in this module that pages, because it is the only one
that grows without limit.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="tg1"></a>G1 | [`GET /platform/schools`](#eg1) | **Built.** The operator's school list: filter, search, sort, page. A bare call gives the newest twenty, which is what somebody opening the console usually wants. | [`schools`](../../models/core/School.java) |
| <a id="tg2"></a>G2 | [`GET /platform/schools/{id}`](#eg2) | **Built.** One school in full for the operator, including the lifecycle fields the school itself never sees. Returns a school at any status — closed and deleted included. | [`schools`](../../models/core/School.java) |
| <a id="tg3"></a>~~G3~~ | [~~GET /platform/schools/subdomain-available?value=~~](#eg3) | **Dropped.** Dropped on 2026-08-31, having been built the same day. #1 and #10 already answer it with the same codes, so a signup form submits once instead of asking per keystroke. | [`schools`](../../models/core/School.java) |

## 5. Reads — school surface · [Build order ↓](#build-order)

All eight resolve the tenant with `require`, not `requireUsable`: a suspended school can still
read its own profile and calendar. Being blocked from editing is not being blocked from looking,
and `409 SCHOOL_NOT_EDITABLE` is not a true answer to a `GET`.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="tg4"></a>G4 | [`GET /schools/current`](#eg4) | **Built.** The school reading its own details — the read behind #6 to #9. Returns the identical record those four return, so one screen loads and saves with one shape. | [`schools`](../../models/core/School.java) |
| <a id="tg5"></a>G5 | [`GET /schools/current/academic-years`](#eg5) | **Built.** Every year the school has, newest first. Sorted on `startDate`, not `createdAt` — "newest" means furthest along the calendar, not typed most recently. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg6"></a>G6 | [`GET /schools/current/academic-years/current`](#eg6) | **Built.** The year today falls in, or a 404 that says which kind of nothing. Worked out from the dates, never stored — a `current` flag would be a second source that can disagree. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg7"></a>G7 | [`GET /schools/current/academic-years/{name}`](#eg7) | **Built.** One year by name. Keyed on the name because that is what the whole system means when it says "which year", and the lookup is by school and name, so another school's year is a 404. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg8"></a>G8 | [`GET /schools/current/academic-years/{name}/holidays`](#eg8) | **Built.** The whole calendar, sorted by date, with both counts. `closedDayCount` is days; `eventCount` is reasons, and it is larger whenever a weekly off lands on a festival. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg9"></a>G9 | [`GET /schools/current/academic-years/{name}/holidays/{date}`](#eg9) | **Built.** **Is the school closed that day, and why.** The question attendance, timetables, transport and fee due dates all ask. An open day is a 200 with `closed: false`, never a 404. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg10"></a>G10 | [`GET /schools/current/academic-years/{name}/working-days?from=&to=`](#eg10) | **Built.** Which days in a range are working days, and how many. G9 in bulk — attendance percentages and fee proration need the whole range, not two hundred calls. | [`academic_years`](../../models/core/AcademicYear.java) |
| <a id="tg11"></a>G11 | [`GET /schools/current/academic-years/{name}/holidays/export?format=csv`](#eg11) | **Optional.** The calendar as a file a school can hand to somebody. Optional — G8 already returns it as JSON. | [`academic_years`](../../models/core/AcademicYear.java) |

---

# Build order

Sequenced by dependency first, then by risk. Phase 0 was the plumbing every endpoint assumes, and
it is done: the one error shape, the page envelope, the tenant resolver, the auditing hook and the
Mongo transaction manager.

| Phase | What it gives you | Endpoints | |
|---|---|---|---|
| **1** | A tenant exists | #1, #2 | built |
| **2** | The tenant is usable | #3, #4, #5 | built |
| **3** | A school can edit itself | #6–#9 | built |
| **4** | A year exists | #18, #19 | built |
| **5** | The year has a calendar | #20–#23, D1, D2 | built |
| **6** | Gates and sensitive edits | #10, #24–#27, #29 built; #12 deferred | part |
| **7** | The reads | G1, G2, G4–G10 | built |
| **8** | Offboarding and deletion | #13–#17 | deferred |
| **9** | Convenience | #28, G11 | |

**Why this order.** Nothing exists until phase 1. Phase 2 makes a tenant usable. Phase 3 is the
first thing a real school touches. Phases 4 and 5 give the school a calendar, which attendance,
timetable, transport and fees all read. Phase 6 groups everything needing elevated permission.

---

# The rules that outrank everything else

### A year's name can never change, and there is no rename endpoint

Nothing references a year by id. Every other collection stores the year's *name* as a string in
its own `academicYear` field — `"2026-2027"` **is** the join key across `FeeInvoice`,
`TransportTrip`, `FeedbackCampaign` and dozens more. A rename would not fail and would not
cascade: it would leave every stored `"2026-2027"` pointing at a year that no longer answers to
it, with every row still looking valid. Nobody would notice until a report came back empty. That
is also why the URL is keyed by name rather than by id.

### There is no `DELETE` on a year either

"Is this year used anywhere?" cannot be a foreign-key check when the references are strings; it is
a query across every collection carrying an `academicYear` field. Until that is cheap, a year
created by mistake is hidden through `recordState`, not removed.

### `current` is derived from the dates, never stored

`AcademicYear` has no `current` field on purpose, and there is no "set current year" endpoint. Two
sources for "which year is it" is two sources that can disagree, and somebody eventually forgets
to move the flag. G6 works it out from the dates — which is also why #18 refuses overlapping
years: if two years covered one day, the question would have two answers.

### No closure is ever inferred from the day of the week

Schools here may run on a Sunday and take the weekly off on another day. **Only a dated entry on
the calendar closes a day**, which is why #23 exists to generate the weekly ones and why G9 and
G10 contain no weekday test. `dayOfWeek` appears on responses for a person to read. `dayOfWeek ==
SUNDAY` in a caller is the bug.

### `encryptionKeyReference` never appears on any response, on either surface

It is a pointer to a key. It is already absent from every write response, and a read is the
likelier place for it to be added by accident. `statusReason` is nearly as sensitive: it is
written for the operator — "Non-payment. Third invoice unpaid past 60 days." — so it is on G1 and
G2 only, and never on G4.

### PATCH for a partial edit, PUT where the value is replaced whole

#6 and #8 are `PATCH`: a missing field is left alone, an empty string clears it, an empty body is
a 400. #7 and #9 are `PUT` because both are all-or-nothing — a patched address can name a city in
the wrong state, and a logo either exists or does not. Transitions are `POST` to a verb rather
than `PATCH /status`, because a status field a caller can set to anything is a state machine with
no guard.

### A read carries no `nextStep` and no `changeSummary`

Those are write fields — they say what just happened. Both are annotated `@JsonInclude(NON_NULL)`
on the shared response records, so they drop out of the JSON on G5 to G10 rather than coming back
null on every row.

---

# Things this module deliberately will not have

- **`DELETE` on a School.** A tenant walks the lifecycle to `DELETED` through #13 to #17, which are deferred — so today there is no way to remove one at all. The only `DELETE`s here are on holidays.
- **Subscription or plan changes.** `SchoolSubscription` is its own resource with its own controller — see [`controllers/plans`](../plans/README.md). #3 reads it and never copies it.
- **Plan or subscription fields on `School`.** There would then be two answers to "what plan is this school on", and one of them would be stale.
- **Terms.** [`AcademicTerm`](../../models/academics/structure/AcademicTerm.java) lives in `academics` and gets a controller there.
- **A stored "current academic year".** Derived from the dates — see above.
- **Bulk tenant operations.** Suspending forty schools at once is an operational script, not an endpoint.
- **Notifications.** "Your school has been suspended", "results have been unlocked" — those are messages, and `notification` is designed last by the decision of 2026-08-14. Nothing sends from here, and there is no `notifiedAt` field.
- **Per-field reads** such as `GET /schools/current/localization`. G4 returns the whole profile; four endpoints returning slices of one small document is four things to keep in step.
- **Anything under `audit_events`.** The trail is not written yet — #26 and #27 record nothing — so a read of it would return an empty collection and imply a guarantee that does not exist.

---

# Debts and open questions

### There is no authentication on any of this

`/platform/schools` provisions tenants and seeds their roles, unauthenticated — the most valuable
unauthenticated endpoint an attacker could ask for. `/schools/current` trusts an
`X-School-Subdomain` header any caller can set to any school's subdomain, which means **anybody
can edit any school.** Fine on a developer machine, unacceptable anywhere else. When sessions
exist only [`CurrentSchoolResolver`](../../common/current/CurrentSchoolResolver.java) changes; the
controllers, services and DTOs stay exactly as they are. That is the whole reason it is one class
rather than a check in each endpoint.

### #27 records nothing about who unlocked results, or why

All four gates are idempotent and flip freely, and none of them writes an audit row. Unlocking
published results is the most consequential thing in this package, and today it leaves no trace.
Before results are real, #27 needs a reason on the request and an `AuditEvent` written — which
needs a writer, not just a repository.

### The server clock decides what "today" is

G6, G10, `AcademicYearResponse.current` and #8's year-in-progress guard all call
`LocalDate.now()`, which uses the server's zone rather than the school's `defaultTimeZone`. For a
school in a different zone that is wrong for a few hours around midnight. **It is one change
everywhere or none** — fixing it in a single place would make an endpoint pick a year against one
date and then report `current` against another.

### A year could once be named `current`

`current` is a fixed path segment, so Spring matches G6 ahead of G7's `/{name}`. A year actually
called `current` could be created, listed, and then never opened. #18 now refuses the name through
`CoreValidator.validateAcademicYearName`. **Add to that list if another fixed word is ever put
under `/academic-years/`.**

### #12 is a `POST`, not a `PATCH`

Rotating an encryption key is not a field edit, so it is a `POST` to a verb like the other
transitions. It stays deferred until something is actually encrypted.

---

# Appendix — what every API touches, field by field

The same 41 endpoints, with the fields each one reads and each one writes. Written so that whoever
changes an endpoint does not have to work this out again from the models, and so a reviewer can
see at a glance whether a change reaches a field it should not.

Read **updates** as "changes an existing document", **insert** as "writes a new one", and
**reads** as "looks at it but does not change it".

Three things are left out of every entry because they are true of all of them:

- **The audit fields** — `createdAt`, `updatedAt`, `createdByDocsId`, `updatedByDocsId` and `version` — are filled in by Spring Data on every write. No endpoint sets them by hand.
- **`schoolId`** is on `academic_years`, `roles`, `number_sequences` and `school_subscriptions`, and every query must carry it. `schools` is the exception: it *is* the tenant, so it has none.
- **Every `/schools/current` endpoint reads `schools` first**, by `subdomain`, to work out which tenant is calling. It is listed only where the endpoint also cares about a field on the school, such as `status`.

## What each field can hold

The entries below name the fields; this names the **values**. Stated once here rather than
repeated across 41 entries, so there is one place to correct when a rule changes.

**Where a set is closed, it is an enum and the list is exhaustive** — anything else is a `400`
naming the field and listing what is accepted. Where it is open (`schoolName`, `statusReason`, a
holiday's `name`) the column says so, because an open set is a thing a reviewer should notice.

Every value below is written by an endpoint that exists today, except where it says otherwise —
**all 22 writes and 9 reads in this module are built**, so unlike the plans module this is a
description of running code rather than a plan.

### `schools` — [School](../../models/core/School.java)

| Field | Type | What can be in it |
|---|---|---|
| `schoolName` | String, required | **Open** — free text, trimmed, up to 200 characters. #6 refuses an empty string: a school cannot lose its name, so blanking it is `400 SCHOOL_NAME_REQUIRED` rather than a silent clear. |
| `accountHolderName` | String, required | **Open** — the name on the contract, trimmed. Blanking is `400 ACCOUNT_HOLDER_NAME_REQUIRED`. **#1 caps it at 150 and #6 at 200** — see the note under Debts. It is a plain name, not a link to a person: the signer and the school's first administrator are often not the same. |
| `subdomain` | String, required, unique | **Normalised first**: trimmed, lowercased, every run of spaces and underscores becomes one `-`, so `"St Marys"` → `st-marys`. Then it must match `^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$` — 1 to 63 characters, no leading or trailing hyphen — else `409 SUBDOMAIN_INVALID`. **44 names are reserved** (`www` `api` `admin` `app` `platform` `mail` `login` `auth` `docs` `test` `staging` `dev` `demo` `billing` `webhooks` and the rest) → `409 SUBDOMAIN_RESERVED`. Already taken → `409 SUBDOMAIN_TAKEN`. Missing → `400 SUBDOMAIN_REQUIRED`. |
| `logoUrl` | String, optional | **`https://` only** → else `400 LOGO_URL_NOT_HTTPS`, and the host must be one of **four**: `cdn.example.com` `res.cloudinary.com` `s3.amazonaws.com` `storage.googleapis.com` → else `400 LOGO_HOST_NOT_ALLOWED`. Unparseable → `400 LOGO_URL_INVALID`. **Blank removes the logo** rather than failing. A file upload would be better; there is no storage service yet. |
| `phoneNumber` | String, optional | **Open** — no shape is enforced, up to 30 characters. Blank is stored as null, never as `""`. |
| `emailAddress` | String, optional | **Lowercased and trimmed.** #1 uses Jakarta `@Email`; #6 uses `^[^@\s]+@[^@\s]+\.[^@\s]{2,}$` → `400 EMAIL_INVALID`. Blank clears it. |
| `encryptionKeyReference` | String, optional | **Nothing writes it and nothing returns it.** #12 would, and #12 is deferred, so it is null on every row. It must never appear on a response on either surface. |
| `defaultLocale` | String, required | An IETF language tag — `^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$`, so `en`, `en-IN`, `hi-Deva-IN`. Blanking through #8 is `400 LOCALE_REQUIRED`. **#8 caps it at 35 characters and #1 does not cap it at all** — see Debts. |
| `defaultTimeZone` | String, required | **Any IANA zone id the JVM knows** — checked against `ZoneId.getAvailableZoneIds()`, so `Asia/Kolkata` yes and `Asia/Pune` `409 TIME_ZONE_INVALID`. Changing it needs `confirmTimeZoneChange: true` → else `409 TIME_ZONE_CHANGE_NOT_CONFIRMED`, **and is refused outright while a year is running** → `409 ACADEMIC_YEAR_IN_PROGRESS`, because it reinterprets which date every stored attendance record and holiday falls on. |
| `addressLine` `city` `stateOrProvince` `postalCode` | String, optional | **Open** — 200, 100, 100 and 20 characters. #7 is a `PUT`, so **an omitted field is cleared, not kept**. |
| `countryCode` | String, required | **Two letters, uppercased** — `^[A-Za-z]{2}$`, an ISO 3166-1 alpha-2 code. Set by #1 and **never editable afterwards**: it is deliberately absent from #7, because moving a school between countries changes its tax and reporting rules and is not an address edit. |
| `status` | [SchoolStatus](../../models/core/enums/SchoolStatus.java), required | **`PROVISIONING`** at create — the only starting state. Built moves: **`PROVISIONING` → `ACTIVE`** (#3), **`ACTIVE` → `SUSPENDED`** (#4), **`SUSPENDED` → `ACTIVE`** (#5). Refusals are `409 SCHOOL_NOT_ACTIVATABLE`, `SCHOOL_NOT_SUSPENDABLE`, `SCHOOL_NOT_REACTIVATABLE`. The other four — `OFFBOARDING` `CLOSED` `DELETION_PENDING` `DELETED` — **have no endpoint that can reach them**, because #13 to #17 are deferred. |
| `activatedAt` | Instant, optional | **Set once, on the first activation only.** #3 checks it to tell a first activation from a repeat, so re-activating a school later never overwrites the date it originally went live. |
| `suspendedAt` | Instant, optional | Set by #4. **Kept on purpose after #5** — with `statusReason`, it is the record of the last suspension. |
| `statusReason` | String, optional | **Open**, up to 500 characters. Required on #4, optional on #5. Written **for the operator** — "Non-payment. Third invoice unpaid past 60 days." — so it is on G1 and G2 and **never on G4**. |

### `academic_years` — [AcademicYear](../../models/core/AcademicYear.java)

| Field | Type | What can be in it |
|---|---|---|
| `name` | String, required, unique per school | **Open** — free text up to 40 characters, trimmed, conventionally `2026-2027`. **It can never change**: there is no rename endpoint and there must never be one, because every other collection stores this string as its `academicYear`. Already used → `409 ACADEMIC_YEAR_NAME_TAKEN`. **`current` is reserved** in any case or padding → `409 ACADEMIC_YEAR_NAME_RESERVED`, because G6 owns that path segment and a year called it could never be opened. |
| `startDate` `endDate` | LocalDate, required | **ISO `YYYY-MM-DD`** — anything else is `400 INVALID_PARAMETER` from the type-mismatch handler. Start must be strictly before end → `400 INVALID_DATE_RANGE`. The span must be **30 to 800 days inclusive** → `400 IMPLAUSIBLE_DATE_RANGE`; that is a typo guard, not a rule about how schools work. **No overlap with another year of the same school** → `409 ACADEMIC_YEAR_OVERLAP`, and adjacency stays legal: one year ending 03-31 and the next starting 04-01 is fine. #19 also refuses a narrowing that would strand a closed day outside the new range → `409 HOLIDAYS_OUTSIDE_NEW_RANGE`. |
| `holidays` | List, required | **`[]`** at create — #18 never accepts holidays, the calendar has its own endpoints. Keyed by `date`, one entry per closed day, each carrying one or more reasons. Rows below. |
| `enrollmentEnabled` | Boolean, required | **`false`** at create; `true` from #24, `false` from #25. A gate on **new** enrollments only — students already enrolled are untouched. Both are idempotent: already in the asked-for state comes back `200` saying so. |
| `resultsLocked` | Boolean, required | **`false`** at create; `true` from #26, `false` from #27. Idempotent both ways, and **neither records who did it or why** — see Debts. |
| `isThisYearRunning` | Boolean, required | **`false`** at create, and **null on every document written before 2026-09-09** — which reads as not running, which is the right way round for a gate. `false` from [#29](#e29); **nothing sets it true yet**. It says the school has *switched over* to a year the calendar says has begun, which the dates cannot say on their own. Read only **alongside** the dates, never alone: one left true after its year ended would keep a finished year live. `AcademicYearResponse` argues against storing this at all — see the field's own javadoc, which records both sides. Wants a partial unique index on `{schoolId, isThisYearRunning}` filtered to true, once something writes it. |
| `recordState` | [RecordState](../../models/base/enums/RecordState.java), required | **`ACTIVE`** always. `INACTIVE` `ARCHIVED` `DELETED` exist on the base class and **nothing in this module writes them**, which is why G5 returns every year rather than filtering: a filter here would be the only one in the codebase, and it would disagree with the overlap check. |

### `academic_years.holidays[]` — [HolidayDetail](../../models/core/embedded/HolidayDetail.java)

| Field | Type | What can be in it |
|---|---|---|
| `date` | LocalDate, required | **ISO, and inside the year** → else `400 HOLIDAY_OUTSIDE_YEAR` on a write, or `400 DATE_OUTSIDE_ACADEMIC_YEAR` on G9 and G10. Both ends of the year are inclusive. **The date cannot be edited** — #22 changes a reason, not the day it falls on; move a holiday by removing it and adding it back. |
| `events` | List, required, never empty | **One or more reasons.** A day that loses its last reason has its whole entry removed and becomes a working day again — the list is never left empty on a stored day. |

### `academic_years.holidays[].events[]` — [HolidayEvent](../../models/core/embedded/HolidayEvent.java)

| Field | Type | What can be in it |
|---|---|---|
| `name` | String, required | **Open** — free text up to 120 characters, trimmed. #22 refuses an empty string → `400 HOLIDAY_NAME_REQUIRED`. #23 defaults it to `"Weekly Off"` when the request does not say. |
| `description` | String, optional | **Open** — up to 300 characters, or absent. Blank is stored as null. |
| `type` | [HolidayType](../../models/core/enums/HolidayType.java), required | One of **eight**: `WEEKLY_OFF` `PUBLIC_HOLIDAY` `FESTIVAL` `RELIGIOUS` `SCHOOL_EVENT` `VACATION` `EXAM_BREAK` `OTHER`. **Each type may appear at most once on a date** → `409 HOLIDAY_ENTRY_EXISTS` on #21, or `400 DUPLICATE_HOLIDAY_ENTRY` when #20 is sent the same pair twice in one list. That is what makes `?type=` enough to identify one reason on a day that has several — and why omitting it on #22 or D1 is `400 HOLIDAY_TYPE_REQUIRED` when the day has more than one. |

### Written by this module but owned elsewhere

| Collection | Field | What #2 and #3 put there |
|---|---|---|
| [`roles`](../../models/identity/Role.java) | `roles[].roleKey` | **Three seeded**: `SCHOOL_ADMIN` (every module, school-wide), `TEACHER` (own classes only), `GUARDIAN` (own child only). #2 creates only what is missing, so running it twice adds nothing. #3 refuses to activate without `SCHOOL_ADMIN` → `409 SETUP_INCOMPLETE`. |
| `roles` | `roles[].systemManaged` `roles[].active` | **`true`** on all three. `systemManaged` marks them as ours rather than the school's. |
| [`number_sequences`](../../models/institution/NumberSequence.java) | `counters[].sequenceType` | **One array entry per value of [NumberSequenceType](../../models/institution/enums/NumberSequenceType.java) — 48 of them, in one document.** #3 counts the entries and refuses to activate if any is missing → `409 SETUP_INCOMPLETE`, because almost every business document takes its human-readable number from one and the failure would otherwise surface to whoever first tries to admit a student. |
| `number_sequences` | `counters[]` `scopeKey` `nextValue` `paddingWidth` `resetPolicy` | **`"GLOBAL"`, `1`, `6`, `NEVER`** on every seeded entry. |
| [`school_subscriptions`](../../models/plans/SchoolSubscription.java) | `status` `current` | **Read only, never written and never copied onto the school.** #3 warns when there is no subscription and activates anyway, and refuses only when one exists and is `CANCELLED` or `EXPIRED` → `409 SUBSCRIPTION_NOT_ACTIVE`. That leniency is temporary and says so in the response — see [`controllers/plans`](../plans/README.md) #13. |
## School — the platform surface  ·  #1–#5, #10, #12–#17

<a id="e1"></a>
**[#1](#t1) · `POST /platform/schools`**

- [`schools`](../../models/core/School.java) — *insert*: `schoolName`, `accountHolderName`, `subdomain`, `phoneNumber`, `emailAddress`, `defaultLocale`, `defaultTimeZone`, `addressLine`, `city`, `stateOrProvince`, `postalCode`, `countryCode`, `status` = `PROVISIONING`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "schoolName": "Springfield High",   // REQUIRED, max 200
  "accountHolderName": "Seymour Skinner",  // REQUIRED, max 150
  "subdomain": "springfield-high",    // REQUIRED, max 63
  "defaultLocale": "en-IN",           // REQUIRED, IETF tag
  "defaultTimeZone": "Asia/Kolkata",  // REQUIRED, max 64
  "countryCode": "IN",                // REQUIRED, 2 letters

  "phoneNumber": "+91 98765 43210",   // optional, max 30
  "emailAddress": "head@shs.edu.in",  // optional, valid email
  "addressLine": "12 Mill Road",      // optional, max 200
  "city": "Pune",                     // optional, max 100
  "stateOrProvince": "Maharashtra",   // optional, max 100
  "postalCode": "411001"              // optional, max 20
}
</pre></td>
<td><pre>
201 Created
Location: /platform/schools/6a9e598382db56ca8afb6086

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "schoolName": "Springfield High",
  "subdomain": "springfield-high",
  "status": "PROVISIONING",
  "createdAt": "2026-09-07T07:09:06.130Z",
  "nextStep": "Run complete-provisioning next: the school has no roles and no number sequences yet, so nothing can be created in it."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `schoolName` | **yes** | Max 200, `@NotBlank`. What appears on the school's own screens. |
| `accountHolderName` | **yes** | Max 150. The person answerable for the account — not a user account, which does not exist yet. |
| `subdomain` | **yes** | Max 63, the DNS label limit. **Normalised** — `Norm_Check 12` becomes `norm-check-12` — then checked for uniqueness (`409 SUBDOMAIN_TAKEN`) and against the reserved list (`409 SUBDOMAIN_RESERVED`). It is how every school-surface request identifies its tenant, so it is the one field that has to be right. |
| `defaultLocale` | **yes** | An IETF language tag — `en-IN`, `hi`, `mr-IN`. Pattern-checked, so `english` is a `400` naming the shape. |
| `defaultTimeZone` | **yes** | An IANA zone — `Asia/Kolkata`. **Everything dated in this school is worked out in it**, including a subscription's billing period, so a wrong one is wrong for years. |
| `countryCode` | **yes** | ISO 3166-1 alpha-2, exactly two letters, case-insensitive on the way in. |
| `phoneNumber` | no | Max 30, free text — numbers are formatted differently everywhere and a pattern would refuse valid ones. |
| `emailAddress` | no | `@Email`, max 254. |
| `addressLine`, `city`, `stateOrProvince`, `postalCode` | no | The address, all optional. A school can be created before anybody knows where it is, and #7 replaces the four together later. |

**`status` is not on the request.** Every school starts `PROVISIONING` — there is no starting-state
choice, because `ACTIVE` would skip the subscription check, and there is no `TRIAL` any more (a
trial belongs to the subscription, where it has a plan and a period behind it).

**It creates no user.** So the school cannot be logged into yet, which is what `nextStep` says.

<a id="e2"></a>
**[#2](#t2) · `POST /platform/schools/{id}/complete-provisioning`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — a closed or deleted school cannot be provisioned
- [`number_sequences`](../../models/institution/NumberSequence.java) — *reads*: `counters` — which `sequenceType` values the school already has
- [`number_sequences`](../../models/institution/NumberSequence.java) — *insert or updates*: the school's one document, with a `counters` entry per missing type — each carrying `sequenceType`, `scopeKey`, `nextValue`, `paddingWidth`, `resetPolicy`. An insert on the first run so the auditing hook fills in `createdAt`; a `$push` afterwards, because saving the document back would reset every counter the school is already using
- [`roles`](../../models/identity/Role.java) — *reads*: `roles` — which `roleKey` values the school already has
- [`roles`](../../models/identity/Role.java) — *insert or updates*: the school's one document, with a `roles` entry per missing default — each carrying `roleKey`, `name`, `description`, `permissions`, `systemManaged`, `active`, for `SCHOOL_ADMIN`, `TEACHER` and `GUARDIAN`. A `$push` when the document exists, so a school's edited permissions are never overwritten

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — POST with nothing.

Path:  {id}  the school's Mongo id

Idempotent: safe to send repeatedly, and
safe when you do not know what state the
school is in.
</pre></td>
<td><pre>
200 OK

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "PROVISIONING",
  "numberSequencesCreated": 48,
  "numberSequencesAlreadyPresent": 0,
  "rolesCreated": 3,
  "rolesAlreadyPresent": 0,
  "roleKeys": ["GUARDIAN", "SCHOOL_ADMIN", "TEACHER"],
  "readyToActivate": true,
  "nextStep": "Ready. Sell it a subscription and it goes live — see plans #13."
}

Sent again, nothing is created twice:

{
  "numberSequencesCreated": 0,
  "numberSequencesAlreadyPresent": 48,
  "rolesCreated": 0,
  "rolesAlreadyPresent": 3,
  "readyToActivate": true
}
</pre></td>
</tr>
</table>

**No request fields.** The four counts are the whole point of the response — they say what this
call did as against what was already there, which is what makes re-running it safe to read:

| Field | What it answers |
|---|---|
| `numberSequencesCreated` / `...AlreadyPresent` | Entries added to the school's one `number_sequences` document against entries already in its `counters` array. A counter part-way through its numbering keeps its `nextValue` — the seeding is a `$push` of the gaps, not a rewrite. |
| `rolesCreated` / `...AlreadyPresent` | The same for the `roles` document. A role whose permissions the school has edited is never overwritten by our defaults. |
| `roleKeys` | Which roles exist now, so a caller does not need a second read to know what a user can be given. |
| `readyToActivate` | Both sets are complete. This is the same condition `whyNotReadyToActivate` checks, so a `true` here means plans #13 will take the school live when it sells it a subscription. |

<a id="e3"></a>
**[#3](#t3) · `POST /platform/schools/{id}/activate`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — only `PROVISIONING`; `activatedAt` to tell a first activation from a repeat
- [`roles`](../../models/identity/Role.java) — *reads*: `roles.roleKey` — `SCHOOL_ADMIN` must be in the array
- [`number_sequences`](../../models/institution/NumberSequence.java) — *reads*: `counters` — **its length**, not a document count. `countBySchoolId` only ever answers 0 or 1 now, so it would pass however empty the array was
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `current` — read, never copied onto the school
- [`schools`](../../models/core/School.java) — *updates*: `status` = `ACTIVE`, `activatedAt` on the first activation only

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — POST with nothing.

Path:  {id}  the school's Mongo id

PROVISIONING → ACTIVE. Anything else is
a 409.
</pre></td>
<td><pre>
200 OK

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "activatedAt": "2026-09-07T07:12:12.708Z",
  "firstActivation": true,
  "subscriptionStatus": "ACTIVE",
  "subscriptionNote": null,
  "nextStep": "Live. Users can sign in and the product is available."
}

409 SETUP_INCOMPLETE      — no SCHOOL_ADMIN role,
                            or missing sequences
409 SCHOOL_NOT_ACTIVATABLE — not PROVISIONING
</pre></td>
</tr>
</table>

**No request fields.** Three things about the response are worth knowing:

| Field | What it answers |
|---|---|
| `firstActivation` | `activatedAt` was null before this call. It is stamped **once**, so a school suspended and brought back keeps its original go-live date — and this flag says which of the two happened. |
| `subscriptionStatus` | What the school is paying for, read from `school_subscriptions`. `NONE` when there is nothing. |
| `subscriptionNote` | Non-null only when something is worth saying — historically that nothing could create a subscription at all. |

**A school does not always need this call.** Plans #13 activates a `PROVISIONING` school whose
provisioning is finished, because a subscription is the last thing such a school is waiting for.
This endpoint stays for the school that is activated before it is sold to.

**The setup gates are shared, not duplicated.** `whyNotReadyToActivate` in
[`SchoolPlatformService`](../../services/core/SchoolPlatformService.java) is the one
implementation of "is this school ready", called from here and from plans #13 — two copies is how
the two come to disagree, and the wrong copy is the one that lets a broken school go live.

<a id="e4"></a>
**[#4](#t4) · `POST /platform/schools/{id}/suspend`**

- [`schools`](../../models/core/School.java) — *updates*: `status` = `SUSPENDED`, `suspendedAt`, `statusReason`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "reason": "Unpaid invoices since June."   // REQUIRED, max 500
}
</pre></td>
<td><pre>
200 OK

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "SUSPENDED",
  "activatedAt": "2026-09-07T07:12:12.708Z",
  "suspendedAt": "2026-11-02T09:40:00Z",
  "statusReason": "Unpaid invoices since June.",
  "nextStep": "Nobody at this school can sign in. Reactivate it when the reason is resolved."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `reason` | **yes** | Max 500, `@NotBlank`. **The only field, and it is required on purpose**: suspending a school stops everybody at it working, and a suspension nobody can explain is one nobody can lift with confidence. It is stored as `statusReason` and comes back on every read of the school — #G2 and #G1 both carry it. |

`activatedAt` survives untouched, so the school's original go-live date is not lost by a
suspension. `suspendedAt` is stamped now.

<a id="e5"></a>
**[#5](#t5) · `POST /platform/schools/{id}/reactivate`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — only `SUSPENDED`
- [`schools`](../../models/core/School.java) — *updates*: `status` = `ACTIVE`, `statusReason` only when a note is sent

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "note": "Invoices settled on the 2nd."    // optional, max 500
}

// The body is optional — POST with nothing
// is a valid reactivation.
</pre></td>
<td><pre>
200 OK

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "activatedAt": "2026-09-07T07:12:12.708Z",
  "suspendedAt": null,
  "statusReason": "Invoices settled on the 2nd.",
  "nextStep": "Back in service. Everybody at the school can sign in again."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `note` | no | Max 500. Why the suspension was lifted, stored over `statusReason`. Absent leaves the field holding the **suspension's** reason, which is deliberate: the last thing written about the school's status is still true history until somebody replaces it. |

**Why the body is optional here but required on #4.** Suspending takes something away from
everybody at the school and needs an answer to "why"; giving it back does not. Sending nothing is
a valid reactivation.

**`activatedAt` is not touched.** It is the original go-live date, not the last one — see
`firstActivation` on #3.

<a id="e10"></a>
**[#10](#t10) · `PATCH /platform/schools/{id}/subdomain`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain` — the body must confirm the current one, and `status`
- [`schools`](../../models/core/School.java) — *updates*: `subdomain`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "currentSubdomain": "springfield-high",   // REQUIRED, max 63
  "newSubdomain": "springfield-senior"      // REQUIRED, max 63
}
</pre></td>
<td><pre>
200 OK

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "schoolName": "Springfield High",
  "previousSubdomain": "springfield-high",
  "subdomain": "springfield-senior",
  "nextStep": "Every school-surface request must now send X-School-Subdomain: springfield-senior. The old one resolves to nothing."
}

409 SUBDOMAIN_MISMATCH  — currentSubdomain is wrong
409 SUBDOMAIN_TAKEN     — somebody else has the new one
409 SUBDOMAIN_RESERVED  — it is on the reserved list
409 SCHOOL_NOT_EDITABLE — the school is past ACTIVE
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `currentSubdomain` | **yes** | Max 63, `@NotBlank`. **The whole reason this endpoint has a body.** It is not used to find the school — `{id}` does that — it is used to refuse the request when it does not match. Renaming a subdomain breaks every integration pointing at the old one, so the caller has to prove they know which school they are renaming. Wrong value is `409 SUBDOMAIN_MISMATCH`. |
| `newSubdomain` | **yes** | Max 63. Normalised, then checked for uniqueness and against the reserved list, exactly as on #1. |

**Both come back on the response**, `previousSubdomain` and `subdomain`, because the caller has
to update whatever was using the old one and `nextStep` spells out what that is.

<a id="e12"></a>
**[#12](#t12) · `POST /platform/schools/{id}/rotate-encryption-key`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `encryptionKeyReference`

<a id="e13"></a>
**[#13](#t13) · `POST /platform/schools/{id}/offboard`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `status` = `OFFBOARDING`, `statusReason`

<a id="e14"></a>
**[#14](#t14) · `POST /platform/schools/{id}/close`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `status` = `CLOSED`, `statusReason`

<a id="e15"></a>
**[#15](#t15) · `POST /platform/schools/{id}/request-deletion`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `status` = `DELETION_PENDING`, `statusReason`

<a id="e16"></a>
**[#16](#t16) · `POST /platform/schools/{id}/cancel-deletion`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `status` back to `CLOSED`, `statusReason`

<a id="e17"></a>
**[#17](#t17) · `POST /platform/schools/{id}/confirm-deletion`**  ·  deferred

- [`schools`](../../models/core/School.java) — *updates*: `status` = `DELETED`, `deletedAt`

## School — the school's own surface  ·  #6–#9, ~~#11~~

<a id="e6"></a>
**[#6](#t6) · `PATCH /schools/current/profile`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain` — the tenant, and `status` — must be editable
- [`schools`](../../models/core/School.java) — *updates*: `schoolName`, `accountHolderName`, `phoneNumber`, `emailAddress`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
         REQUIRED on every school-surface call

{
  "schoolName": "Springfield Senior",   // optional, max 200
  "accountHolderName": "S. Skinner",    // optional, max 200
  "phoneNumber": "+91 98765 43210",     // optional, max 30
  "emailAddress": "head@shs.edu.in"     // optional, max 254
}

// All four optional, but a body with none
// of them is a 400.
</pre></td>
<td><pre>
200 OK — SchoolProfileResponse, the same shape every
profile write returns

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "logoUrl": "https://cdn.example.com/logo.png",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN"
}
</pre></td>
</tr>
</table>

**Every field on the request.** Absent always means "leave it exactly as it is".

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `schoolName` | no | Max 200. Cannot be cleared — a school with no name is not a state worth supporting. |
| `accountHolderName` | no | Max 200. |
| `phoneNumber` | no | Max 30, free text. |
| `emailAddress` | no | Max 254. |

**What a school cannot change about itself**, and where each one lives instead: `subdomain` is
#10 (platform only — it is the tenant key, and a school renaming its own is a school breaking
everybody's integrations), `status` is #3 to #5, the address is #7, the locale and zone are #8,
the logo is #9, and `countryCode` is not editable at all.

**Four narrow endpoints rather than one wide PATCH**, which is the shape of this whole group:
the address is replaced as a unit (#7), the time zone needs a confirmation (#8), and the logo is
a URL somebody has to have uploaded first (#9). One endpoint taking all of it would need all
three sets of rules at once.

<a id="e7"></a>
**[#7](#t7) · `PUT /schools/current/address`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`schools`](../../models/core/School.java) — *updates*: `addressLine`, `city`, `stateOrProvince`, `postalCode` — an omitted field is cleared. **Not** `countryCode`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

{
  "addressLine": "12 Mill Road",      // optional, max 200
  "city": "Pune",                     // optional, max 100
  "stateOrProvince": "Maharashtra",   // optional, max 100
  "postalCode": "411001"              // optional, max 20
}

// A PUT: the four are replaced together.
// An omitted field is CLEARED, not kept.
</pre></td>
<td><pre>
200 OK — SchoolProfileResponse, the same shape every
profile write returns

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "logoUrl": "https://cdn.example.com/logo.png",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN"
}
</pre></td>
</tr>
</table>

**Every field on the request.** All four optional individually — but read the note below, because
absence means something different here from everywhere else in this module.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `addressLine` | no | Max 200. **Absent clears it.** |
| `city` | no | Max 100. Absent clears it. |
| `stateOrProvince` | no | Max 100. Absent clears it. |
| `postalCode` | no | Max 20. Absent clears it. |

**It is a `PUT`, and that is the point.** An address is one thing, not four independent fields: a
school that moves from Pune to Mumbai and PATCHes `city` alone is left with the old street and
the old postcode, which is a worse address than either. Sending all four together makes the
result somebody's deliberate answer.

**So an empty body `{}` clears the whole address**, which is a legitimate thing to want and the
reason no field is individually required.

<a id="e8"></a>
**[#8](#t8) · `PATCH /schools/current/localization`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`, `defaultTimeZone`
- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — is a year running today
- [`schools`](../../models/core/School.java) — *updates*: `defaultLocale`, `defaultTimeZone`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

{
  "defaultLocale": "hi-IN",           // optional, IETF tag
  "defaultTimeZone": "Asia/Kolkata",  // optional, max 64
  "confirmTimeZoneChange": true       // REQUIRED to change the zone
}
</pre></td>
<td><pre>
200 OK — SchoolProfileResponse, the same shape every
profile write returns

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "logoUrl": "https://cdn.example.com/logo.png",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN"
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `defaultLocale` | no | An IETF tag, max 35, pattern-checked — and `""` is explicitly allowed by the pattern, which is how the locale is cleared back to the platform default. Absent leaves it. |
| `defaultTimeZone` | no | An IANA zone, max 64. Absent leaves it. **Changing it needs `confirmTimeZoneChange`.** |
| `confirmTimeZoneChange` | **to change the zone** | `true` is the acknowledgement. A different `defaultTimeZone` without it is `409 TIME_ZONE_CHANGE_NOT_CONFIRMED`, whose message spells out what the change reinterprets. Not needed when the zone is absent or unchanged. |

**Why the time zone needs confirming and the locale does not.** A locale changes what words a
screen shows. A time zone changes **what day it is** — which dates count as holidays, when a
working day starts and ends, when an academic year begins, and which day a billing period runs
from. Every dated record already in the school was worked out in the old zone and is not
recalculated, so the change is quietly retroactive. That is worth one extra field.

<a id="e9"></a>
**[#9](#t9) · `PUT /schools/current/logo`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`schools`](../../models/core/School.java) — *updates*: `logoUrl`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

{
  "logoUrl": "https://cdn.example.com/logo.png"   // optional, max 500
}

// A PUT of one field: send "" or omit it
// to remove the logo.
</pre></td>
<td><pre>
200 OK — SchoolProfileResponse, the same shape every
profile write returns

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "logoUrl": "https://cdn.example.com/logo.png",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN"
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `logoUrl` | no | Max 500. A URL to an image somebody has **already uploaded somewhere else** — this endpoint stores a string, it does not accept a file. Absent or empty removes the logo, which is why it is a `PUT` of one field rather than a PATCH: there is no third state between "this logo" and "no logo". |

**There is no upload here.** File storage is a separate concern with its own limits, virus
scanning and CDN, and none of it exists yet. When it does, it will hand back a URL and this
endpoint is what stores it.

<a id="e11"></a>
**[#11](#t11) · `PATCH /platform/schools/{id}/account-holder`**  ·  dropped

- [`schools`](../../models/core/School.java) — *updates*: would have been `accountHolderName` — #6 does it

## Academic year — writes  ·  #18–#29, D1, D2

<a id="e18"></a>
**[#18](#t18) · `POST /schools/current/academic-years`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `name` — unique per school; `startDate`, `endDate` of every other year — no overlap allowed
- [`academic_years`](../../models/core/AcademicYear.java) — *insert*: `schoolId`, `name`, `startDate`, `endDate`, `holidays` = empty, `enrollmentEnabled` = false, `resultsLocked` = false

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

{
  "name": "2026-27",                  // REQUIRED, max 40
  "startDate": "2026-06-01",          // REQUIRED
  "endDate": "2027-04-30"             // REQUIRED
}
</pre></td>
<td><pre>
200 OK — AcademicYearResponse, the shape every year write returns

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": false,
  "resultsLocked": false,
  "nextStep": "The year has no calendar yet. Add holidays next — and use generate-weekly-off for the recurring ones."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | **yes** | Max 40, `@NotBlank`. **This is the key every other collection references**, and every academic-year URL addresses the year by it rather than by an id. It is therefore **immutable**: #19 has no `name` field at all, so no request can rename a year — renaming one would orphan every record pointing at it. `current` is refused here as a name (`409 ACADEMIC_YEAR_NAME_RESERVED`), because a year called `current` could be created and then never opened: that word is already the URL for "the year we are in". |
| `startDate` | **yes** | A date, not an instant. |
| `endDate` | **yes** | Must be after `startDate`, and the span is checked for overlap against the school's other years — `409` naming the year it collides with. |

**`holidays` is not on the request.** A year is created empty and the calendar is filled in
afterwards by #20 (replace all), #21 (add one) or #23 (generate the weekly off). Accepting a
calendar here would mean validating every date against a span that is being created in the same
breath.

**`current` is not on the request either.** It is worked out from the dates — the year containing
today — so there is no flag to move and no way for two years to both claim it.

**`enrollmentEnabled` and `resultsLocked` both start false**, and are #24/#25 and #26/#27.

<a id="e19"></a>
**[#19](#t19) · `PATCH /schools/current/academic-years/{name}/dates`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` of this and every other year; `holidays` — any `date` that would fall outside
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `startDate`, `endDate`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year, e.g. 2026-27

{
  "startDate": "2026-06-15",          // optional
  "endDate": "2027-05-15"             // optional
}

// Either or both. A body with neither is
// a 400.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse, the shape every year write returns

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": false,
  "resultsLocked": false,
  "nextStep": "Dates updated. Note that records in other collections reference this year by name, and the name has not changed."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `startDate` | no | A date. Absent leaves it. Checked against the resulting `endDate`, whichever of the two moved. |
| `endDate` | no | A date. Absent leaves it. The resulting span is re-checked for overlap with the school's other years. |

**`name` is deliberately absent, and this is the endpoint where that matters most.** It is the
natural key other collections reference, so renaming a year would leave every record pointing at
a year that no longer exists. The name is in the URL; a PATCH that could change the thing it is
addressing would name one year and mean another.

**Moving the dates can move `current`.** It is derived from the span, so a year edited to no
longer contain today stops being current — and a different year may become it. That is why the
response returns `current` rather than making the caller re-read.

**A body with neither date is `400 NOTHING_TO_UPDATE`** — *"Send startDate, endDate, or both."*

**Holidays that would fall outside the new span are refused, not silently dropped** — `409
HOLIDAYS_OUTSIDE_NEW_RANGE`, naming how many and the first one: *"1 closed day(s) would fall
outside the new dates, starting with 2026-11-09 (Solo (FESTIVAL))."* Shrinking a year over dates
somebody has already marked would either delete their festival or strand it outside the year, and
neither is something to do without being asked.

<a id="e20"></a>
**[#20](#t20) · `PUT /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — every date must be inside
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — the whole list, each `date` with its `events` of `name`, `description`, `type`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}

{
  "holidays": [                       // REQUIRED, the whole list
    {
      "name": "Independence Day",     // REQUIRED, max 120
      "type": "PUBLIC_HOLIDAY",       // REQUIRED
      "date": "2026-08-15",           // REQUIRED
      "description": "National."      // optional, max 300
    }
  ]
}

// A PUT: the list sent IS the calendar.
// [] empties it.
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 42,
  "eventCount": 45,
  "countsByType": {
    "WEEKLY_OFF": 35,
    "PUBLIC_HOLIDAY": 8,
    "FESTIVAL": 2
  },
  "holidays": [
    {
      "date": "2026-08-15",
      "dayOfWeek": "SATURDAY",
      "events": [
        {
          "name": "Independence Day",
          "description": "National holiday.",
          "type": "PUBLIC_HOLIDAY"
        },
        { "name": "Weekly off", "type": "WEEKLY_OFF" }
      ]
    }
  ],
  "changeSummary": "Replaced the calendar: 45 events across 42 days."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `holidays` | **yes** | `@NotNull` — the list itself must be present. **An empty array empties the calendar**, which is a legitimate thing to want and the reason this is a `PUT`. |
| `holidays[].name` | **yes** | Max 120, `@NotBlank`. What a calendar shows. |
| `holidays[].type` | **yes** | A `HolidayType` — `WEEKLY_OFF`, `PUBLIC_HOLIDAY`, `FESTIVAL`, and so on. **The type is what makes two events on one date meaningful**: a festival that falls on a Sunday is still a festival, and `countsByType` counts events rather than days for exactly that reason. |
| `holidays[].date` | **yes** | A date. Validated against the year's span. |
| `holidays[].description` | no | Max 300. Absent means null. |

**It replaces the whole calendar**, so it is the endpoint for importing a year's holidays in one
go. Adding one is #21, which leaves everything else alone; there is no endpoint that merges a
partial list, because "merge" would have to guess whether a missing date means "delete it" or
"I did not mention it".

**Several events may share a date.** The response groups them: `holidays` is a list of *days*,
each carrying an `events` array, and `closedDayCount` counts days while `eventCount` counts
events. A day is closed if anything on it closes the school.

<a id="e21"></a>
**[#21](#t21) · `POST /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — is that `date` already closed for this `type`
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — a new `date` entry, or one more `events` row on an existing one

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}

{
  "name": "Ganesh Chaturthi",         // REQUIRED, max 120
  "type": "FESTIVAL",                 // REQUIRED
  "date": "2026-09-14",               // REQUIRED
  "description": "Local festival."    // optional, max 300
}
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 42,
  "eventCount": 45,
  "countsByType": {
    "WEEKLY_OFF": 35,
    "PUBLIC_HOLIDAY": 8,
    "FESTIVAL": 2
  },
  "holidays": [
    {
      "date": "2026-08-15",
      "dayOfWeek": "SATURDAY",
      "events": [
        {
          "name": "Independence Day",
          "description": "National holiday.",
          "type": "PUBLIC_HOLIDAY"
        },
        { "name": "Weekly off", "type": "WEEKLY_OFF" }
      ]
    }
  ],
  "changeSummary": "Added 'Weekly off' on 2026-08-15 alongside 1 existing."
}
</pre></td>
</tr>
</table>

**Every field on the request** — the same four as one entry of #20's list.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | **yes** | Max 120, `@NotBlank`. |
| `type` | **yes** | A `HolidayType`. |
| `date` | **yes** | A date inside the year's span. |
| `description` | no | Max 300. |

**Adding to a date that already has something on it is allowed, and is the interesting case.**
A `FESTIVAL` on a date already carrying a `WEEKLY_OFF` gives that day two events: the school was
already closed, and now there is a reason worth naming. `changeSummary` says so — *"Added 'Weekly
off' on 2026-08-15 alongside 1 existing."* — because `closedDayCount` does not move and a caller
could otherwise think nothing happened. `eventCount` is the number that changes.

**A second event of the *same type* on one date is refused** — `409 HOLIDAY_ENTRY_EXISTS`,
*"There is already a PUBLIC_HOLIDAY entry on 2026-08-15. Edit or remove it first."* It is the
**type** that collides, not the name: one date cannot hold two public holidays whatever they are
called, because `{date, type}` is how #22 and #D1 address a single event.

<a id="e22"></a>
**[#22](#t22) · `PATCH /schools/current/academic-years/{name}/holidays/{date}?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` — the `date`, and `type` to pick which reason when a day has several
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: the matching `events` row's `name`, `description`, `type`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}   the year
         {date}   the day, 2026-08-15
Query:   ?type=PUBLIC_HOLIDAY   optional

{
  "name": "Independence Day (obs.)",  // optional, max 120
  "description": "Moved to Monday.",  // optional, max 300
  "newType": "PUBLIC_HOLIDAY"         // optional
}
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse, as #20 returns

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 41,
  "eventCount": 43,
  "countsByType": { "WEEKLY_OFF": 35, "PUBLIC_HOLIDAY": 8 },
  "holidays": [ /* one entry per closed day */ ],
  "changeSummary": "1 event updated."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | no | Max 120. Absent leaves it. |
| `description` | no | Max 300. Absent leaves it. |
| `newType` | no | A `HolidayType`. Absent leaves it. Named `newType` rather than `type` because `type` is already the **query parameter that selects which event to edit** — two fields called the same thing, one choosing the target and one setting the value, is a mistake waiting to be made. |

**`?type=` is how one event on a shared date is picked out.** A date can carry several events, so
`{date}` alone does not identify one. Omitting it works when the day holds exactly one; on a day
holding more it is `400 HOLIDAY_TYPE_REQUIRED`, whose message names the reasons the day is closed
for rather than editing an arbitrary one. A body with none of the three fields is `400
NOTHING_TO_UPDATE` — *"Send at least one of name, description or newType."*

**The date itself cannot be changed.** It is in the URL, and an event that moves to another day
is a removal (#D1) and an addition (#21) — two calendars, two `changeSummary` lines, rather than
one edit that silently empties one day and fills another.

<a id="ed1"></a>
**[D1](#td1) · `DELETE /schools/current/academic-years/{name}/holidays/{date}?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — one `events` row removed, and the `date` entry too once its `events` list is empty

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}   the year
         {date}   the day
Query:   ?type=WEEKLY_OFF   optional

No body — DELETE.
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse, as #20 returns

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 41,
  "eventCount": 43,
  "countsByType": { "WEEKLY_OFF": 35, "PUBLIC_HOLIDAY": 8 },
  "holidays": [ /* one entry per closed day */ ],
  "changeSummary": "1 event removed. The day is now open."
}
</pre></td>
</tr>
</table>

**No request body.** The only inputs are the path and one optional query parameter.

| Parameter | Required | What it accepts, and what its absence means |
|---|---|---|
| `{date}` | **yes** | The day to remove something from. |
| `?type=` | no | Which event on that day. **Absent removes the whole day** — every event on it — so the day becomes open, and `changeSummary` lists what went: *"Removed Diwali (FESTIVAL), Weekly off (WEEKLY_OFF) on 2026-11-08."* With it, only that one goes: `?type=FESTIVAL` on a date also carrying a `WEEKLY_OFF` removes the festival and the school stays closed. |

**It differs from #22 here, deliberately.** A PATCH with no `?type=` on a date holding several
events is refused with `400 HOLIDAY_TYPE_REQUIRED`, because editing an arbitrary one of them is a
guess. A DELETE with no `?type=` is not ambiguous at all — "clear this day" is a coherent
instruction, and it is the one somebody wants when a date was entered by mistake.

**`changeSummary` says whether the day is now open**, which is the fact a caller actually needs:
removing a festival from a Sunday changes what the calendar *says* without changing whether
anybody has to come in. `closedDayCount` and `eventCount` moving by different amounts is the same
information in numbers.

<a id="e23"></a>
**[#23](#t23) · `POST /schools/current/academic-years/{name}/holidays/generate-weekly-off`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — which dates already have a `WEEKLY_OFF`
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — one entry per matching weekday, skipping any that already had one

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}

{
  "dayOfWeek": "SUNDAY",              // REQUIRED
  "fromDate": "2026-06-01",           // optional
  "toDate": "2027-04-30",             // optional
  "name": "Weekly off"                // optional, max 120
}
</pre></td>
<td><pre>
200 OK — WeeklyOffGenerateResponse, not the calendar

{
  "academicYearName": "2026-27",
  "dayOfWeek": "SUNDAY",
  "fromDate": "2026-06-01",
  "toDate": "2027-04-30",
  "generated": 44,
  "skippedAlreadyWeeklyOff": 4,
  "skippedDates": [
    "2026-06-07", "2026-06-14", "2026-08-16", "2026-12-27"
  ],
  "closedDayCountAfter": 86,
  "eventCountAfter": 89,
  "changeSummary": "44 Sundays closed. 4 were already weekly offs and were left alone."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `dayOfWeek` | **yes** | `@NotNull`, a `java.time.DayOfWeek` — `SUNDAY`, `FRIDAY`, `SATURDAY`. **There is no default**, and that is deliberate: this codebase assumes nothing about which day a school is closed. A school may run on Sunday with its weekly off on any other day, so guessing would be wrong somewhere. |
| `fromDate` | no | Absent means the year's `startDate`. |
| `toDate` | no | Absent means the year's `endDate`. Together they are how a school changes its weekly off mid-year — generate Sundays to December, Saturdays after. |
| `name` | no | Max 120. Absent means a sensible default. What each generated entry is called. |

**It skips rather than duplicating.** A date that already carries a `WEEKLY_OFF` is left exactly
as it is and listed in `skippedDates`, so running it twice is safe and the second run reports
`generated: 0`. It does **not** skip dates that are closed for other reasons — a public holiday
falling on a Sunday gets the weekly off too, because both are true and the type is what
distinguishes them.

**It answers its own response type, not the calendar.** `generated` against
`skippedAlreadyWeeklyOff` is the question a caller has, and returning 86 closed days would not
answer it.

<a id="ed2"></a>
**[D2](#td2) · `DELETE /schools/current/academic-years/{name}/holidays?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — every `events` row of that `type` removed, and any `date` left with none

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}
Query:   ?type=WEEKLY_OFF   REQUIRED

No body — DELETE.
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse, as #20 returns

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 41,
  "eventCount": 43,
  "countsByType": { "WEEKLY_OFF": 35, "PUBLIC_HOLIDAY": 8 },
  "holidays": [ /* one entry per closed day */ ],
  "changeSummary": "35 WEEKLY_OFF events removed across 35 days."
}
</pre></td>
</tr>
</table>

**No request body.** One query parameter, and unlike #D1's it is **required**.

| Parameter | Required | What it accepts, and what its absence means |
|---|---|---|
| `?type=` | **yes** | A `HolidayType`. Every event of that type in the year goes. |

**Required precisely because absent would mean "all of them".** A `DELETE` on a year's holidays
with no qualifier would empty the calendar, and that is not something to make reachable by
forgetting a parameter. Emptying it deliberately is #20 with `{"holidays": []}` — a request whose
shape says what it does.

**This is the undo for #23.** Generating the wrong weekly off — Sundays for a school that closes
on Fridays — is fixed by `?type=WEEKLY_OFF` and then generating again, without touching the
public holidays and festivals somebody entered by hand.

<a id="e24"></a>
**[#24](#t24) · `POST /schools/current/academic-years/{name}/enrollment/enable`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `enrollmentEnabled` = true

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year

No body — POST with nothing.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false,
  "nextStep": "Admissions can be taken against 2026-27."
}
</pre></td>
</tr>
</table>

**No request fields.** The year is in the path and there is nothing to configure: the endpoint
does one thing, and its URL says which.

**Admissions can be taken against this year.** The flag is what an admission checks before it
is accepted, so a year with enrollment closed cannot gain students by accident.

**Idempotent.** Enabling an already-enabled year answers `200` and says so in `nextStep` rather
than refusing: the caller wants it open, and it is open.

**Four endpoints rather than two toggles.** `enrollment/enable` states the wanted end state, so two
callers racing each other cannot leave it in the state neither asked for — and a request that
says "enable" is auditable in a way one that says "flip" is not.

<a id="e25"></a>
**[#25](#t25) · `POST /schools/current/academic-years/{name}/enrollment/disable`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `enrollmentEnabled` = false

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year

No body — POST with nothing.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false,
  "nextStep": "Admissions can be taken against 2026-27."
}
</pre></td>
</tr>
</table>

**No request fields.** The year is in the path and there is nothing to configure: the endpoint
does one thing, and its URL says which.

**Admissions stop being accepted against this year.** Students already admitted are untouched —
this closes the door, it does not undo anything.

**Idempotent**, for the same reason as #24.

**Four endpoints rather than two toggles.** `enrollment/disable` states the wanted end state, so two
callers racing each other cannot leave it in the state neither asked for — and a request that
says "enable" is auditable in a way one that says "flip" is not.

<a id="e26"></a>
**[#26](#t26) · `POST /schools/current/academic-years/{name}/results/lock`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `resultsLocked` = true

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year

No body — POST with nothing.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false,
  "nextStep": "Admissions can be taken against 2026-27."
}
</pre></td>
</tr>
</table>

**No request fields.** The year is in the path and there is nothing to configure: the endpoint
does one thing, and its URL says which.

**Marks stop being editable for this year.** What it protects is the moment after results are
published: a mark changed afterwards, with no record of who changed it, is the thing this flag
exists to prevent.

**Idempotent.** Locking a locked year is a `200`.

**Four endpoints rather than two toggles.** `results/lock` states the wanted end state, so two
callers racing each other cannot leave it in the state neither asked for — and a request that
says "enable" is auditable in a way one that says "flip" is not.

<a id="e27"></a>
**[#27](#t27) · `POST /schools/current/academic-years/{name}/results/unlock`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `resultsLocked` = false

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year

No body — POST with nothing.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false,
  "nextStep": "Admissions can be taken against 2026-27."
}
</pre></td>
</tr>
</table>

**No request fields.** The year is in the path and there is nothing to configure: the endpoint
does one thing, and its URL says which.

**Marks become editable again**, which is a decision rather than a correction — hence a separate
endpoint from #26 rather than a toggle. Somebody has to ask for it in a request that says what it
does.

**Idempotent**, like the other three.

**Four endpoints rather than two toggles.** `results/unlock` states the wanted end state, so two
callers racing each other cannot leave it in the state neither asked for — and a request that
says "enable" is auditable in a way one that says "flip" is not.

<a id="e29"></a>
**[#29](#t29) · `POST /schools/current/academic-years/{name}/end`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays`, `isThisYearRunning`
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `isThisYearRunning` = false, `endDate` = today
- [`schools`](../../models/core/School.java) — *reads*: `defaultTimeZone`, to know which day today is

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year

No body — POST with nothing.
</pre></td>
<td><pre>
200 OK — AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2026-09-09",
  "durationDays": 101,
  "current": true,
  "holidayCount": 12,
  "enrollmentEnabled": false,
  "resultsLocked": false,
  "nextStep": "'2026-27' has been ended — it is no
    longer the year this school is running, and its
    last day is now Wednesday 9 September 2026, 233
    day(s) earlier than planned. Enrollment and
    result locking are unchanged; set those
    separately if this year should also stop
    accepting records."
}
</pre></td>
</tr>
</table>

**No request fields.** The year is in the path and there is nothing to configure — like #24 to
#27, the endpoint does one thing and its URL says which.

### An event, not a re-plan — which is why it is not [#19](#e19)

#19 moves a year's boundaries because somebody **decided** they should be different. This records
that the year **is over**, as of today: a term finished early, a school closing, a calendar
superseded. The difference is not cosmetic, because the two are allowed to refuse different
things:

| | [#19](#e19) `PATCH .../dates` | #29 `POST .../end` |
|---|---|---|
| A 6-day year | `400 IMPLAUSIBLE_DATE_RANGE` | **allowed** |
| Moving `endDate` later | allowed — that is the point | `409 ACADEMIC_YEAR_ALREADY_ENDED` |
| Which date it writes | whatever you send | today, always |
| `isThisYearRunning` | untouched | set false |

**#29 deliberately does not call `validateAcademicYearRange`.** That validator rejects any range
under 30 days as "almost certainly a typo", which is right when somebody is *planning* a year and
wrong here: a school that shut two weeks into term really did have a two-week year, and refusing
to record it would leave the calendar claiming a year that is still running.

### Today is the *school's* today

`Dates.todayIn(school.defaultTimeZone)`, not the server's date. At 23:00 in Asia/Kolkata it is
still the previous day in UTC, and closing a year a day early loses a day of the school's work.
This was mutation-tested: swapping it for `LocalDate.now()` is invisible to any test whose school
shares the server's zone, so the check uses two zones 25 hours apart — Kiritimati and Midway —
which are never on the same calendar date.

### What it refuses

| Code | Status | When |
|---|---|---|
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | this school has no year by that name |
| `ACADEMIC_YEAR_NOT_STARTED` | 409 | today is on or before its first day — it would end before it began |
| `ACADEMIC_YEAR_ALREADY_ENDED` | 409 | it finished in the past, so writing today would move that date **forward** |
| `HOLIDAYS_OUTSIDE_NEW_RANGE` | 409 | closed days after today would be stranded outside the shortened year |

**`ACADEMIC_YEAR_ALREADY_ENDED` is the trap this endpoint exists to avoid.** Writing today's date
onto a year that closed last March would push its end forward by months — the exact opposite of
ending it — and it would look like it worked.

**Stranded closed days are refused, not deleted**, with the same code and the same policy as
[#19](#e19), so the two cannot answer the question differently. Deleting a school's calendar
entries as a side effect of a different action is not something this should do quietly. A closed
day already *behind* today is fine and is kept: it is still inside the shortened year.

### Idempotent, like #24 to #27

Ending an already-ended year answers `200` with *"Nothing changed: '2026-27' was already closed
on … and was not marked as running."* — the same shape as the enrollment and results flags, and
as [#14](../plans/README.md#e14) in the plans module. The caller wants the year ended, and it is
ended.

The note reports the two halves separately, because they move independently: whether it stopped
running, and whether its dates moved. Saying "year ended" for a call that changed nothing would
be telling the caller something untrue.

### What it does NOT touch

**`enrollmentEnabled` and `resultsLocked` are left alone**, and the response says so. Both are
arguably implied by a year ending — a finished year should not take admissions — but neither was
asked for, and a write that quietly changed three flags when it was asked to change one is worse
than a second call. #24 to #27 are those flags, one deliberate act each.

### Tests

**41 end-to-end assertions**: the happy path and the day count, re-ending as a no-op, a year not
yet started, a year already finished (and that its `endDate` really was left alone), closed days
after today refused with the first one named, closed days before today kept, a 6-day year allowed
here and refused by #19, the 404, the missing tenant header, that `GET`/`PATCH`/`DELETE` are not
mapped, and the two-zone case above.

**Seven mutations, all caught**: dropping either date guard, stranding the holidays, forgetting
either write, using the server's date, and applying the planning-range validator.

<a id="e28"></a>
**[#28](#t28) · `POST /schools/current/academic-years/{name}/clone`**  ·  optional

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` of the year being copied
- [`academic_years`](../../models/core/AcademicYear.java) — *insert*: a new year with `holidays` copied and dates shifted

## Reads — platform  ·  G1, G2, ~~G3~~

<a id="eg1"></a>
**[G1](#tg1) · `GET /platform/schools`**

- [`schools`](../../models/core/School.java) — *reads*: `status`, `schoolName`, `subdomain`, `countryCode`, `city`, `createdAt` — the filters; then `statusReason`, `accountHolderName`, `emailAddress`, `phoneNumber`, `activatedAt`, `suspendedAt`. **Never** `encryptionKeyReference`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET. Every parameter optional.

?status=ACTIVE&status=SUSPENDED
&search=springfield
&countryCode=in
&city=pune
&createdFrom=2026-01-01T00:00:00Z
&createdTo=2026-12-31T23:59:59Z
&page=0
&size=20
&sort=createdAt,desc

A bare call returns the newest twenty.
</pre></td>
<td><pre>
200 OK — a page of SchoolSummaryResponse

{
  "content": [
    {
      "schoolId": "6a9e598382db56ca8afb6086",
      "schoolName": "Springfield High",
      "subdomain": "springfield-high",
      "status": "ACTIVE",
      "statusReason": null,
      "accountHolderName": "Seymour Skinner",
      "emailAddress": "head@shs.edu.in",
      "phoneNumber": "+91 98765 43210",
      "city": "Pune",
      "countryCode": "IN",
      "createdAt": "2026-09-07T07:09:06.130Z",
      "activatedAt": "2026-09-07T07:12:12.708Z",
      "suspendedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
</pre></td>
</tr>
</table>

**Every parameter on the request.** All optional; there is no body.

| Parameter | Required | What it accepts, and what its absence means |
|---|---|---|
| `status` | no | **Repeatable** — `?status=ACTIVE&status=SUSPENDED` means either. `OR` within the field, because "show me the live ones and the ones we have cut off" is one question. Absent means every status. A misspelling is `400 INVALID_PARAMETER` listing the accepted values. |
| `search` | no | Partial, case-insensitive, on **school name or subdomain**. Deliberately not the address — including it would make `?search=pune` return every school in the city, which is what `city` is for. The term is escaped, so `?search=.*` matches literally and returns nothing. |
| `countryCode` | no | Exact, case-insensitive. |
| `city` | no | Exact, case-insensitive. |
| `createdFrom` | no | ISO instant, inclusive. |
| `createdTo` | no | ISO instant, inclusive. |
| `page` | no | Zero-based, absent means 0. |
| `size` | no | Absent means 20, and it is bounded. |
| `sort` | no | `field,direction`. Absent means newest first. An unknown field or direction is a `400` naming what is allowed. |

**No address on the rows**, beyond `city`. A school list is a list of who and where-roughly;
#G2 is where the full record lives.

<a id="eg2"></a>
**[G2](#tg2) · `GET /platform/schools/{id}`**

- [`schools`](../../models/core/School.java) — *reads*: every field except `encryptionKeyReference`, including `statusReason`, `activatedAt`, `suspendedAt`, `createdAt`, `updatedAt`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Path:  {id}  the school's Mongo id
</pre></td>
<td><pre>
200 OK — SchoolDetailResponse: everything, including the
three lifecycle fields the school itself never sees

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "subdomain": "springfield-high",
  "logoUrl": "https://cdn.example.com/logo.png",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN",
  "status": "ACTIVE",
  "statusReason": null,
  "activatedAt": "2026-09-07T07:12:12.708Z",
  "suspendedAt": null,
  "createdAt": "2026-09-07T07:09:06.130Z",
  "updatedAt": "2026-09-07T07:12:12.708Z"
}

404 SCHOOL_NOT_FOUND
</pre></td>
</tr>
</table>

**No request fields.** The row from #G1, opened. What it adds over the summary is the address in
full, the locale and zone, the logo — and the three fields that make this the platform's view
rather than the school's:

| Field | Why it is here and not on #G4 |
|---|---|
| `statusReason` | Why the school was suspended, in the operator's words. A school reading "unpaid invoices since June" about itself is a conversation somebody should be having on purpose, not a field on a profile screen. |
| `activatedAt` | When it first went live. Stamped once — see `firstActivation` on #3. |
| `suspendedAt` | When it was last cut off, and null once reactivated. |

Compare [#G4](#eg4), which is the same school read by itself and returns
`SchoolProfileResponse` — no lifecycle timestamps, no status reason, no `createdAt`.

<a id="eg3"></a>
**[G3](#tg3) · `GET /platform/schools/subdomain-available?value=`**  ·  dropped

- [`schools`](../../models/core/School.java) — *reads*: would have been `subdomain` — #1 and #10 do it

## Reads — school surface  ·  G4–G11

<a id="eg4"></a>
**[G4](#tg4) · `GET /schools/current`**

- [`schools`](../../models/core/School.java) — *reads*: `schoolName`, `accountHolderName`, `phoneNumber`, `emailAddress`, `logoUrl`, `defaultLocale`, `defaultTimeZone`, `addressLine`, `city`, `stateOrProvince`, `postalCode`, `countryCode`, `subdomain`, `status`. **Not** `statusReason`, `activatedAt`, `suspendedAt` or `encryptionKeyReference`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
         REQUIRED

No body — GET. The school is never named
in the path: it comes from the tenant, so
no school can read another's profile.
</pre></td>
<td><pre>
200 OK — SchoolProfileResponse, the same 15 fields every
profile write returns

{
  "schoolId": "6a9e598382db56ca8afb6086",
  "subdomain": "springfield-high",
  "status": "ACTIVE",
  "schoolName": "Springfield High",
  "accountHolderName": "Seymour Skinner",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "head@shs.edu.in",
  "logoUrl": "https://cdn.example.com/logo.png",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "addressLine": "12 Mill Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "countryCode": "IN"
}
</pre></td>
</tr>
</table>

**No request fields.** What matters is what is left out against [#G2](#eg2), which is the same
school read by the platform:

| Withheld | Why |
|---|---|
| `statusReason` | Why the school was suspended, in the operator's words. A school reading "unpaid invoices since June" about itself is a conversation somebody should have on purpose, not a field on its own profile screen. |
| `activatedAt`, `suspendedAt` | Lifecycle timestamps kept for the operator. |
| `createdAt`, `updatedAt` | The same. |

`status` **is** returned, because a school being suspended is something its own screens have to
be able to say. It is the reason that is withheld, not the fact.

**Every profile write returns this same type**, so a screen that PATCHes and then renders needs
no second call — see #6 to #9.

<a id="eg5"></a>
**[G5](#tg5) · `GET /schools/current/academic-years`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate`, `enrollmentEnabled`, `resultsLocked`, and a count of `holidays`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

No body — GET. No paging: a school has
academic years, not thousands of them.
</pre></td>
<td><pre>
200 OK — a plain array, newest first

[
  {
    "academicYearId": "6a9ea1b2c3d4e5f601234567",
    "name": "2026-27",
    "startDate": "2026-06-01",
    "endDate": "2027-04-30",
    "durationDays": 334,
    "current": true,
    "holidayCount": 42,
    "enrollmentEnabled": true,
    "resultsLocked": false
  },
  {
    "academicYearId": "6a9e88c1b2d3e4f501234566",
    "name": "2025-26",
    "startDate": "2025-06-01",
    "endDate": "2026-04-30",
    "durationDays": 334,
    "current": false,
    "holidayCount": 44,
    "enrollmentEnabled": false,
    "resultsLocked": true
  }
]
</pre></td>
</tr>
</table>

**No request fields**, and no parameters at all — no filter, no page, no sort. A school has a
handful of years, and every one of them is worth showing: the current one, the last one whose
results are locked, and next year while it is being set up.

**`current` is on each row rather than being a separate call.** It is derived from the dates — the
year containing today — so exactly one row can carry `true`, and a screen picking out "this year"
needs no second request. That is also why [#G6](#eg6) exists as a convenience rather than a
necessity.

**`holidayCount` counts closed days, not events.** A festival on a Sunday is one closed day. The
breakdown by type is [#G8](#eg8).

<a id="eg6"></a>
**[G6](#tg6) · `GET /schools/current/academic-years/current`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the lookup; then the same fields as G5

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high

No body — GET.

The literal word `current` in the path,
where a year name normally goes.
</pre></td>
<td><pre>
200 OK — one AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false
}

404 NO_CURRENT_ACADEMIC_YEAR
"This school has no academic years yet."
</pre></td>
</tr>
</table>

**No request fields.** The year is not named: **`current` is worked out from the dates**, meaning
the one whose span contains today. So there is no flag anywhere to set, nothing to keep in step,
and no way for two years to both claim it — which is exactly why the earlier design of a
`current` boolean on the document was dropped.

**The `404` is a real state, not an error to hide.** A school between years — last year ended on
30 April, next year starts on 1 June — genuinely has no current year for a month, and a caller
has to handle it. Inventing an answer (the nearest year, the newest one) would make a screen show
a year nobody is in.

**This is why a year may not be called `current`.** #18 refuses the name, because a year called
`current` could be created, listed, and then never opened — this URL would always resolve to the
derived one instead.

<a id="eg7"></a>
**[G7](#tg7) · `GET /schools/current/academic-years/{name}`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: the same fields as G5, found by `schoolId` and `name`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}  the year, e.g. 2026-27

No body — GET.
</pre></td>
<td><pre>
200 OK — one AcademicYearResponse

{
  "academicYearId": "6a9ea1b2c3d4e5f601234567",
  "name": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "durationDays": 334,
  "current": true,
  "holidayCount": 42,
  "enrollmentEnabled": true,
  "resultsLocked": false
}

404 ACADEMIC_YEAR_NOT_FOUND
</pre></td>
</tr>
</table>

**No request fields.** Addressed by `name`, not by an id, like every other academic-year
endpoint — the name is the natural key other collections reference, so it is what a caller
already has in hand.

| Field | What it answers |
|---|---|
| `durationDays` | The span in days, inclusive. Computed, not stored. |
| `current` | This year contains today. |
| `holidayCount` | Closed days in the calendar — days, not events. |
| `enrollmentEnabled` | Admissions may be taken against it (#24/#25). |
| `resultsLocked` | Marks are frozen (#26/#27). |

`nextStep` is omitted from the response entirely when there is nothing to say —
`@JsonInclude(NON_NULL)` — so a read of a settled year does not carry an empty field.

<a id="eg8"></a>
**[G8](#tg8) · `GET /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` — every `date` with its `events` of `name`, `description`, `type`; plus `startDate`, `endDate`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}

No body — GET. The whole calendar; no
paging and no date filter, which is #G10's
job.
</pre></td>
<td><pre>
200 OK — HolidayCalendarResponse without a changeSummary

{
  "academicYearName": "2026-27",
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "closedDayCount": 42,
  "eventCount": 45,
  "countsByType": {
    "WEEKLY_OFF": 35,
    "PUBLIC_HOLIDAY": 8,
    "FESTIVAL": 2
  },
  "holidays": [
    {
      "date": "2026-08-15",
      "dayOfWeek": "SATURDAY",
      "events": [
        {
          "name": "Independence Day",
          "description": "National holiday.",
          "type": "PUBLIC_HOLIDAY"
        },
        { "name": "Weekly off", "type": "WEEKLY_OFF" }
      ]
    }
  ]
}
</pre></td>
</tr>
</table>

**No request fields.** The response is the same type the four calendar writes return, minus
`changeSummary` — which is `@JsonInclude(NON_NULL)` and only means something after a change.

**`holidays` is a list of days, each with its events.** That grouping is the whole shape of the
calendar: a date can carry a weekly off *and* a festival, both true, and a screen rendering a
month wants one cell per day rather than two entries fighting over it.

| Field | What it answers |
|---|---|
| `closedDayCount` | How many days the school is shut. |
| `eventCount` | How many reasons there are. Higher than the day count whenever anything shares a date. |
| `countsByType` | **Counts events, not days** — so a festival falling on a Sunday still counts as a festival. That is the number somebody wants when they ask how many festivals the year has. |
| `dayOfWeek` | On each day, so a caller does not recompute it from the date to render a calendar. |

<a id="eg9"></a>
**[G9](#tg9) · `GET /schools/current/academic-years/{name}/holidays/{date}`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the date must be inside; `holidays` — one lookup on `date`, returning every `events` row on it

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}   the year
         {date}   the day, 2026-08-15

No body — GET. One day: "is the school
open, and why not".
</pre></td>
<td><pre>
200 OK — DayStatusResponse

{
  "academicYearName": "2026-27",
  "date": "2026-08-15",
  "dayOfWeek": "SATURDAY",
  "closed": true,
  "events": [
    {
      "name": "Independence Day",
      "description": "National holiday.",
      "type": "PUBLIC_HOLIDAY"
    },
    { "name": "Weekly off", "type": "WEEKLY_OFF" }
  ]
}

An ordinary working day:

{
  "date": "2026-08-17",
  "dayOfWeek": "MONDAY",
  "closed": false,
  "events": []
}
</pre></td>
</tr>
</table>

**No request fields.** Two path values, and the answer is one boolean plus its reasons.

| Field | What a caller does with it |
|---|---|
| `closed` | **The one field to branch on.** True when anything on the day closes the school. Attendance, timetables and admissions all ask this before doing anything dated. |
| `events` | Why. Empty on a working day — not null, so a caller can loop without checking. Several entries when a date carries several reasons. |
| `dayOfWeek` | Returned rather than inferred, so nothing has to know the school's week to render the answer. |

**There is no "is it a weekend" anywhere in this.** A school may run on Sunday with its weekly
off on any other day, so the only source of truth about a closed day is the calendar — a dated
entry somebody put there, or one generated by #23. Nothing in the codebase assumes which day of
the week is special.

<a id="eg10"></a>
**[G10](#tg10) · `GET /schools/current/academic-years/{name}/working-days?from=&to=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`; `holidays` — the closed `date` values in range, counted as days and not reasons

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
Header:  X-School-Subdomain: springfield-high
Path:    {name}
Query:   ?from=2026-06-01   optional
         &to=2026-06-30     optional

No body — GET. Both absent means the whole
year.
</pre></td>
<td><pre>
200 OK — WorkingDaysResponse

{
  "academicYearName": "2026-27",
  "from": "2026-06-01",
  "to": "2026-06-30",
  "totalDayCount": 30,
  "workingDayCount": 25,
  "closedDayCount": 5,
  "workingDays": [
    { "date": "2026-06-01", "dayOfWeek": "MONDAY" },
    { "date": "2026-06-02", "dayOfWeek": "TUESDAY" }
  ]
}
</pre></td>
</tr>
</table>

**No request body.** Two optional query parameters.

| Parameter | Required | What it accepts, and what its absence means |
|---|---|---|
| `from` | no | A date. **Absent means the year's `startDate`.** |
| `to` | no | A date. Absent means the year's `endDate`. Must not be before `from`. |

Both are clamped to the year's span, so a range spilling past either end returns the overlap
rather than refusing — asking for June to next December on a year ending in April is a reasonable
thing to do by accident.

| Field | What it answers |
|---|---|
| `totalDayCount` | Days in the range, inclusive of both ends. |
| `workingDayCount` | Days the school is open. **This is the number attendance percentages and fee proration divide by**, which is why it is computed here once rather than in every module that needs it. |
| `closedDayCount` | The remainder. The two always sum to the total. |
| `workingDays` | Each open day with its `dayOfWeek`. The list, not just the count, because a timetable generator needs the actual dates. |

<a id="eg11"></a>
**[G11](#tg11) · `GET /schools/current/academic-years/{name}/holidays/export?format=csv`**  ·  optional

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays`, `startDate`, `endDate`, `name`
