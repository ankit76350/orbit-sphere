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
| **6** | Gates and sensitive edits | #10, #24–#27 built; #12 deferred | part |
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
| `status` | [SchoolStatus](../../models/core/enums/SchoolStatus.java), required | **`PROVISIONING`** at create, or **`TRIAL`** when the request says `trial: true`. Built moves: **`PROVISIONING`/`TRIAL` → `ACTIVE`** (#3), **`ACTIVE` → `SUSPENDED`** (#4), **`SUSPENDED` → `ACTIVE`** (#5). Refusals are `409 SCHOOL_NOT_ACTIVATABLE`, `SCHOOL_NOT_SUSPENDABLE`, `SCHOOL_NOT_REACTIVATABLE`. The other four — `OFFBOARDING` `CLOSED` `DELETION_PENDING` `DELETED` — **have no endpoint that can reach them**, because #13 to #17 are deferred. |
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
| [`roles`](../../models/identity/Role.java) | `roleKey` | **Three seeded**: `SCHOOL_ADMIN` (every module, school-wide), `TEACHER` (own classes only), `GUARDIAN` (own child only). #2 creates only what is missing, so running it twice adds nothing. #3 refuses to activate without `SCHOOL_ADMIN` → `409 SETUP_INCOMPLETE`. |
| `roles` | `systemManaged` `active` | **`true`** on all three. `systemManaged` marks them as ours rather than the school's. |
| [`number_sequences`](../../models/institution/NumberSequence.java) | `sequenceType` | **One row per value of [NumberSequenceType](../../models/institution/enums/NumberSequenceType.java) — 48 of them.** #3 counts them and refuses to activate if any is missing → `409 SETUP_INCOMPLETE`, because almost every business document takes its human-readable number from one and the failure would otherwise surface to whoever first tries to admit a student. |
| `number_sequences` | `scopeKey` `nextValue` `paddingWidth` `resetPolicy` | **`"GLOBAL"`, `1`, `6`, `NEVER`** on every seeded row. |
| [`school_subscriptions`](../../models/plans/SchoolSubscription.java) | `status` `current` | **Read only, never written and never copied onto the school.** #3 warns when there is no subscription and activates anyway, and refuses only when one exists and is `CANCELLED` or `EXPIRED` → `409 SUBSCRIPTION_NOT_ACTIVE`. That leniency is temporary and says so in the response — see [`controllers/plans`](../plans/README.md) #13. |
## School — the platform surface  ·  #1–#5, #10, #12–#17

<a id="e1"></a>
**[#1](#t1) · `POST /platform/schools`**

- [`schools`](../../models/core/School.java) — *insert*: `schoolName`, `accountHolderName`, `subdomain`, `phoneNumber`, `emailAddress`, `defaultLocale`, `defaultTimeZone`, `addressLine`, `city`, `stateOrProvince`, `postalCode`, `countryCode`, `status` = `PROVISIONING` or `TRIAL`

<a id="e2"></a>
**[#2](#t2) · `POST /platform/schools/{id}/complete-provisioning`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — a closed or deleted school cannot be provisioned
- [`number_sequences`](../../models/institution/NumberSequence.java) — *insert*: `schoolId`, `sequenceType`, `scopeKey`, `nextValue`, `paddingWidth`, `resetPolicy` — one row per missing type
- [`roles`](../../models/identity/Role.java) — *insert*: `schoolId`, `roleKey`, `name`, `description`, `permissions`, `systemManaged`, `active` — for `SCHOOL_ADMIN`, `TEACHER` and `GUARDIAN`

<a id="e3"></a>
**[#3](#t3) · `POST /platform/schools/{id}/activate`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — only `PROVISIONING` or `TRIAL`; `activatedAt` to tell a first activation from a repeat
- [`roles`](../../models/identity/Role.java) — *reads*: `roleKey` — `SCHOOL_ADMIN` must exist
- [`number_sequences`](../../models/institution/NumberSequence.java) — *reads*: a count by `schoolId` — every type must be present
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `current` — read, never copied onto the school
- [`schools`](../../models/core/School.java) — *updates*: `status` = `ACTIVE`, `activatedAt` on the first activation only

<a id="e4"></a>
**[#4](#t4) · `POST /platform/schools/{id}/suspend`**

- [`schools`](../../models/core/School.java) — *updates*: `status` = `SUSPENDED`, `suspendedAt`, `statusReason`

<a id="e5"></a>
**[#5](#t5) · `POST /platform/schools/{id}/reactivate`**

- [`schools`](../../models/core/School.java) — *reads*: `status` — only `SUSPENDED`
- [`schools`](../../models/core/School.java) — *updates*: `status` = `ACTIVE`, `statusReason` only when a note is sent

<a id="e10"></a>
**[#10](#t10) · `PATCH /platform/schools/{id}/subdomain`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain` — the body must confirm the current one, and `status`
- [`schools`](../../models/core/School.java) — *updates*: `subdomain`

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

<a id="e7"></a>
**[#7](#t7) · `PUT /schools/current/address`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`schools`](../../models/core/School.java) — *updates*: `addressLine`, `city`, `stateOrProvince`, `postalCode` — an omitted field is cleared. **Not** `countryCode`

<a id="e8"></a>
**[#8](#t8) · `PATCH /schools/current/localization`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`, `defaultTimeZone`
- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — is a year running today
- [`schools`](../../models/core/School.java) — *updates*: `defaultLocale`, `defaultTimeZone`

<a id="e9"></a>
**[#9](#t9) · `PUT /schools/current/logo`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`schools`](../../models/core/School.java) — *updates*: `logoUrl`

<a id="e11"></a>
**[#11](#t11) · `PATCH /platform/schools/{id}/account-holder`**  ·  dropped

- [`schools`](../../models/core/School.java) — *updates*: would have been `accountHolderName` — #6 does it

## Academic year — writes  ·  #18–#28, D1, D2

<a id="e18"></a>
**[#18](#t18) · `POST /schools/current/academic-years`**

- [`schools`](../../models/core/School.java) — *reads*: `subdomain`, `status`
- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `name` — unique per school; `startDate`, `endDate` of every other year — no overlap allowed
- [`academic_years`](../../models/core/AcademicYear.java) — *insert*: `schoolId`, `name`, `startDate`, `endDate`, `holidays` = empty, `enrollmentEnabled` = false, `resultsLocked` = false

<a id="e19"></a>
**[#19](#t19) · `PATCH /schools/current/academic-years/{name}/dates`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` of this and every other year; `holidays` — any `date` that would fall outside
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `startDate`, `endDate`

<a id="e20"></a>
**[#20](#t20) · `PUT /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — every date must be inside
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — the whole list, each `date` with its `events` of `name`, `description`, `type`

<a id="e21"></a>
**[#21](#t21) · `POST /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — is that `date` already closed for this `type`
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — a new `date` entry, or one more `events` row on an existing one

<a id="e22"></a>
**[#22](#t22) · `PATCH /schools/current/academic-years/{name}/holidays/{date}?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` — the `date`, and `type` to pick which reason when a day has several
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: the matching `events` row's `name`, `description`, `type`

<a id="ed1"></a>
**[D1](#td1) · `DELETE /schools/current/academic-years/{name}/holidays/{date}?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — one `events` row removed, and the `date` entry too once its `events` list is empty

<a id="e23"></a>
**[#23](#t23) · `POST /schools/current/academic-years/{name}/holidays/generate-weekly-off`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`, `holidays` — which dates already have a `WEEKLY_OFF`
- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — one entry per matching weekday, skipping any that already had one

<a id="ed2"></a>
**[D2](#td2) · `DELETE /schools/current/academic-years/{name}/holidays?type=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `holidays` — every `events` row of that `type` removed, and any `date` left with none

<a id="e24"></a>
**[#24](#t24) · `POST /schools/current/academic-years/{name}/enrollment/enable`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `enrollmentEnabled` = true

<a id="e25"></a>
**[#25](#t25) · `POST /schools/current/academic-years/{name}/enrollment/disable`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `enrollmentEnabled` = false

<a id="e26"></a>
**[#26](#t26) · `POST /schools/current/academic-years/{name}/results/lock`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `resultsLocked` = true

<a id="e27"></a>
**[#27](#t27) · `POST /schools/current/academic-years/{name}/results/unlock`**

- [`academic_years`](../../models/core/AcademicYear.java) — *updates*: `resultsLocked` = false

<a id="e28"></a>
**[#28](#t28) · `POST /schools/current/academic-years/{name}/clone`**  ·  optional

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` of the year being copied
- [`academic_years`](../../models/core/AcademicYear.java) — *insert*: a new year with `holidays` copied and dates shifted

## Reads — platform  ·  G1, G2, ~~G3~~

<a id="eg1"></a>
**[G1](#tg1) · `GET /platform/schools`**

- [`schools`](../../models/core/School.java) — *reads*: `status`, `schoolName`, `subdomain`, `countryCode`, `city`, `createdAt` — the filters; then `statusReason`, `accountHolderName`, `emailAddress`, `phoneNumber`, `activatedAt`, `suspendedAt`. **Never** `encryptionKeyReference`

<a id="eg2"></a>
**[G2](#tg2) · `GET /platform/schools/{id}`**

- [`schools`](../../models/core/School.java) — *reads*: every field except `encryptionKeyReference`, including `statusReason`, `activatedAt`, `suspendedAt`, `createdAt`, `updatedAt`

<a id="eg3"></a>
**[G3](#tg3) · `GET /platform/schools/subdomain-available?value=`**  ·  dropped

- [`schools`](../../models/core/School.java) — *reads*: would have been `subdomain` — #1 and #10 do it

## Reads — school surface  ·  G4–G11

<a id="eg4"></a>
**[G4](#tg4) · `GET /schools/current`**

- [`schools`](../../models/core/School.java) — *reads*: `schoolName`, `accountHolderName`, `phoneNumber`, `emailAddress`, `logoUrl`, `defaultLocale`, `defaultTimeZone`, `addressLine`, `city`, `stateOrProvince`, `postalCode`, `countryCode`, `subdomain`, `status`. **Not** `statusReason`, `activatedAt`, `suspendedAt` or `encryptionKeyReference`

<a id="eg5"></a>
**[G5](#tg5) · `GET /schools/current/academic-years`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `name`, `startDate`, `endDate`, `enrollmentEnabled`, `resultsLocked`, and a count of `holidays`

<a id="eg6"></a>
**[G6](#tg6) · `GET /schools/current/academic-years/current`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the lookup; then the same fields as G5

<a id="eg7"></a>
**[G7](#tg7) · `GET /schools/current/academic-years/{name}`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: the same fields as G5, found by `schoolId` and `name`

<a id="eg8"></a>
**[G8](#tg8) · `GET /schools/current/academic-years/{name}/holidays`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays` — every `date` with its `events` of `name`, `description`, `type`; plus `startDate`, `endDate`

<a id="eg9"></a>
**[G9](#tg9) · `GET /schools/current/academic-years/{name}/holidays/{date}`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate` — the date must be inside; `holidays` — one lookup on `date`, returning every `events` row on it

<a id="eg10"></a>
**[G10](#tg10) · `GET /schools/current/academic-years/{name}/working-days?from=&to=`**

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `startDate`, `endDate`; `holidays` — the closed `date` values in range, counted as days and not reasons

<a id="eg11"></a>
**[G11](#tg11) · `GET /schools/current/academic-years/{name}/holidays/export?format=csv`**  ·  optional

- [`academic_years`](../../models/core/AcademicYear.java) — *reads*: `holidays`, `startDate`, `endDate`, `name`
