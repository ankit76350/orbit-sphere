# controllers/people/staff — API plan

**Nothing is built.** This is the detailed plan for the **staff package** — the person, their job,
and the three record types attached to them. It expands the group that
[`controllers/people`](../README.md) lists; that file is the domain map, this one is what gets
built from.

> **Numbers are the domain's, not this file's.** `#1` here is `#1` there. One endpoint keeps one
> number across the whole of `people`, so a service javadoc saying "#16" is unambiguous — which is
> why the numbers below run `1–8`, `16–19`, `20–29` with gaps where `organization`, `leave` and
> `reviews` sit. Those get their own files.

Mirrors [`models/people/staff`](../../../models/people/staff) — five documents and two embedded
types. **These endpoints enforce that package. They do not invent new rules**, and unlike the two
modules before it there is no model bug to settle first: every index names a field that exists, and
the two that need a `partialFilter` have one.

> **This is the package the product is waiting on.** `staffDocsId` is stored by **16 model files
> across 6 modules**, and nothing can create a staff member. [#1](#e1) and [#16](#e16) are the two
> writes that unblock a class teacher, a subject teacher, a timetable entry, an attendance session,
> a payslip and a bus driver.

---

## What this package is

**A person, and everything the school knows about employing them.**

```text
Staff  "Priya Sharma"  EMP-0042
  │      the human being: name, contact, addresses, emergency contact
  │
  ├── EmploymentRecord[]        the JOB — position, manager, status, dates
  │     one is current; the rest are history
  │
  ├── StaffCredential[]         degrees, licences, background checks — with expiry
  ├── StaffGovernmentIdentity[] Aadhaar, PAN, passport — encrypted, never plaintext
  └── StaffBankAccount[]        where payroll pays — encrypted, one primary
```

| Document | Collection | Phase |
|---|---|---|
| [`Staff`](../../../models/people/staff/Staff.java) | `staff` | **1** |
| [`EmploymentRecord`](../../../models/people/staff/EmploymentRecord.java) | `employment_records` | **1** |
| [`StaffCredential`](../../../models/people/staff/StaffCredential.java) | `staff_credentials` | 3, partly 6 |
| [`StaffGovernmentIdentity`](../../../models/people/staff/StaffGovernmentIdentity.java) | `staff_government_identities` | 6 — **blocked** |
| [`StaffBankAccount`](../../../models/people/staff/StaffBankAccount.java) | `staff_bank_accounts` | 6 — **blocked** |
| [`StaffAddress`](../../../models/people/staff/embedded/StaffAddress.java) · [`EmergencyContact`](../../../models/people/staff/embedded/EmergencyContact.java) | *embedded in Staff* | **1** |

### The person and the job are two documents, and that is the whole design

**`Staff` has no status, no department, no designation, no joining date.** Work through what is
actually on it — name, date of birth, gender, nationality, language, phone, email, two addresses,
an emergency contact, a photo — and every one is a fact about a *human being*, not about an
employment.

Everything about the job is on `EmploymentRecord`: `positionDocsId`, `managerDocsId`, `status`,
`employmentType`, `effectiveFrom`, `effectiveUntil`, `probationUntil`, `current`.

**Three things follow, and they decide most of this file:**

1. **A promotion is expressible.** One `Staff`, two `EmploymentRecord`s — and every mark that
   teacher entered under the old position still points at the same person.
2. **A leaver keeps their history.** Which is why there is no `DELETE` and why [#17](#e17)
   ends an employment rather than removing a person.
3. **[#1](#e1) alone cannot answer "who works here."** It creates a person; [#16](#e16) employs
   them. A staff list built on `Staff` alone would show leavers beside current staff with nothing
   to tell them apart — which is why [#7](#e7) reads `employment_records` first.

---

## Addressed by id, and `employeeNo` is not an address

| Thing | Referenced by | Addressed in the URL by |
|---|---|---|
| a staff member | `staffDocsId`, in **16** model files | its **id** |
| an employment record | nothing — it is read through its staff member | its **id**, on [#18](#e18) only |
| a credential · identity · bank account | nothing | its **id** |

**`employeeNo` is generated, unique per school, and printed on everything — and it is still not an
address.** Nothing stores it as a reference, so a URL built from it would be a second name for a
person that every consumer would have to learn. It is searchable through [#7](#e7) and returned on
every response, which is what a display key is for.

**Compare `termCode`**, which *is* typed by a person and *is* how a term is known — and is still
not the address, for the same reason. The rule settled on 2026-09-10 is *use whatever other
collections already store*, and here that is unambiguously the id.

---

## Personal data in messages

The rule the domain plan sets, restated because this package is where it bites:

```
in a field   "maskedIdentityNumber": "XXXX XXXX 4821"
in a message "... this Aadhaar is already recorded against another staff member."
             NOT "... already recorded against Priya Sharma (EMP-0042)."
```

**A refusal must not name a person the caller may not be entitled to see.** The duplicate checks in
[#24](#e24), [#27](#e27) and [#20](#e20) all span the whole school, so they say *that* there is a
clash without saying *who* — otherwise the endpoint becomes a way to enumerate staff by guessing
numbers.

**This inverts every other module's convention**, where naming the conflicting row is the point of
a good message: *"'Term 1' is already sequence 1 in this year"*. Here the conflicting row is a
person, and that changes the answer.

**A government identity number never appears in a message, a log or a URL** — masked or otherwise.

---

## Which gates every endpoint runs

| | Gates |
|---|---|
| every **write** here | 1 school is live · 2 school is paying |
| every **read** here | none |

**No gate 4** — no path here carries an academic year, and a person is employed across years rather
than inside one.

**And the authorization warning is not boilerplate in this package.** [#8](#e8) returns a date of
birth, a home address and an emergency contact; [#29](#e29) returns where somebody is paid. See
[open item 2 of the domain plan](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization).

---

# The endpoints

## 1. The person · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /staff`](#e1) | Create a person. `employeeNo` generated, never sent. **The write five other modules are waiting for.** |
| <a id="t2"></a>2 | [`PATCH /staff/{id}`](#e2) | Fix a name, a phone number, a date of birth. |
| <a id="t3"></a>3 | [`PUT /staff/{id}/addresses`](#e3) | Replace current and permanent as a pair. |
| <a id="t4"></a>4 | [`PUT /staff/{id}/emergency-contact`](#e4) | Replace it whole. |
| <a id="t5"></a>5 | [`PUT /staff/{id}/photo`](#e5) | Point at a `DocumentRecord`, or clear it. |
| <a id="t6"></a>6 | [`POST /staff/{id}/archive`](#e6) | Remove a profile created by mistake. **Not** how somebody leaves. |
| <a id="t7"></a>7 — **built** | [`GET /staff`](#e7) | The list behind every teacher picker — **minus the four filters that need [#16](#e16)**. |
| <a id="t8"></a>8 — **built** | [`GET /staff/{id}`](#e8) | One person, with their current employment folded in — **the block is absent until [#16](#e16)**. |

## 2. The job · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t16"></a>16 — **built** | [`POST /staff/{id}/employment`](#e16) | Hire, promote or transfer — **one write, because it is one event.** |
| <a id="t17"></a>17 | [`POST /staff/{id}/separate`](#e17) | End employment. The one way somebody leaves. |
| <a id="t18"></a>18 | [`PATCH /employment/{id}`](#e18) | Correct a date or a manager on a record already written. |
| <a id="t19"></a>19 | [`GET /staff/{id}/employment`](#e19) | One person's history, newest first. |

## 3. The records · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t20"></a>20 | [`POST /staff/{id}/credentials`](#e20) | Record a degree, licence or background check. |
| <a id="t21"></a>21 | [`POST /credentials/{id}/verify`](#e21) | Mark it checked or rejected, with who and when. |
| <a id="t22"></a>22 | [`GET /staff/{id}/credentials`](#e22) | One person's, with expiry **computed**. |
| <a id="t23"></a>23 | [`GET /credentials/expiring`](#e23) | **The compliance report.** |
| <a id="t24"></a>24 | [`POST /staff/{id}/identities`](#e24) | A government identity. Encrypted in, never echoed. |
| <a id="t25"></a>25 | [`GET /staff/{id}/identities`](#e25) | Masked only, at every permission level. |
| <a id="t26"></a>26 | [`POST /identities/{id}/reveal`](#e26) | Authorized recovery. **A POST because it is an event.** |
| <a id="t27"></a>27 | [`POST /staff/{id}/bank-accounts`](#e27) | Where payroll pays. One primary. |
| <a id="t28"></a>28 | [`POST /bank-accounts/{id}/verify`](#e28) | A penny-drop or document check, recorded. |
| <a id="t29"></a>29 | [`GET /staff/{id}/bank-accounts`](#e29) | Masked, with the primary marked. |

---

# Build order

Ordered by **what it unblocks**, not by number. `#1`, `#7`, `#8` and `#16` are built — **phase 1 is complete**.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **1** | A person exists and can be employed — **everything else in the product unblocks** | ~~1~~, ~~16~~, ~~7~~ *(partly)*, ~~8~~ — **complete** |
| **2** | The profile and the history are maintainable | 2, 6, 17, 18, 19 |
| **3** | The profile is complete, and compliance is reportable | 3, 4, 5, 21, 22, 23 |
| **6** | *Blocked on encryption* | 20, 24, 25, 26, 27, 28, 29 |

**Phase 1 depends on `organization` landing first.** [#16](#e16) writes a `positionDocsId`, so
`POST /departments` and `POST /positions` are the true first two calls — they live in
[`controllers/people/organization`](../README.md#3-the-organization--build-order-) and are numbered
`#9` and `#13` there.

**[#1](#e1) before [#16](#e16), and both before [#7](#e7).** A person with no employment record is
a real state — entered but not yet hired — and it is what [#1](#e1) leaves them in.

~~**#16 is now the whole critical path.**~~ **Built 2026-09-15.** The seat exists
([#9](../organization/README.md#e9), [#13](../organization/README.md#e13)), the person exists
([#1](#e1)), and employment joins them — so **phase 1 is complete and nothing in this product is
blocked on `people` any more.**

**What that unblocks, none of it built:** [#7](#e7)'s four employment filters;
[#15](../organization/README.md#e15)'s `filledHeadcount`; the two checks
[#14](../organization/README.md#e14) owes — `POSITION_STILL_FILLED` and
`HEADCOUNT_BELOW_FILLED` — which can now count something; and [#17](#e17), which is the one way
somebody leaves and the only thing that returns a person to having no current record.

**[#20](#e20) is split across phases**, which is worth saying: a credential's *number* is encrypted
and so blocked, but its title, authority, dates and expiry are not. Phase 3 can record
*"B.Ed, Delhi University, valid until 2030"* and leave the certificate number for phase 6.

**Phase 6 is blocked, not deferred.** Nothing in this project can encrypt a value or produce a
keyed hash. See
[open item 1 of the domain plan](../README.md#1-nothing-can-encrypt-anything--this-blocks-three-documents).

---

# Things this package deliberately will not have

- **No `DELETE` on a staff member.** Sixteen model files store `staffDocsId` and none of those
  references is a foreign key, so a deleted person leaves every payslip, attendance session and
  timetable entry pointing at nothing — and *nothing fails*. Leaving is [#17](#e17).
- **No plaintext identity in any `GET`.** [#26](#e26) is a POST for that reason: revealing a
  national identity number is an event that must be recorded, and a GET that writes an audit row
  is a GET that lies about being safe to retry.
- **No `PATCH` that sets `current` on an employment record.** [#18](#e18) corrects dates and a
  manager; flipping `current` is [#16](#e16) and [#17](#e17), which move two records together.
  A PATCH that could set it is how two records end up current.
- **No salary, anywhere.** Not on `Staff`, not on `EmploymentRecord`. `models/people/README.md` is
  explicit, and the reason is access: this package is readable by an HR admin and compensation is
  not.
- **No reporting-line query.** `managerDocsId` exists and *"everyone under Priya"* is a graph walk.
  It wants an aggregation and a use case, in that order.
- **No bulk import.** Two hundred staff from a CSV is a real need and a genuinely different
  endpoint — partial success, per-row errors, an idempotency key. "Call [#1](#e1) in a loop" is how
  you get two hundred half-created people.

---

# To settle before building

The domain plan carries the ones that span packages —
[encryption](../README.md#1-nothing-can-encrypt-anything--this-blocks-three-documents),
[authorization](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization),
[the join #7 needs](../README.md#3-7s-filters-need-a-join-mongo-will-not-do). These three are this
package's own.

## 1. May two employment records overlap? — decide before #16

Two indexes constrain them and neither prevents overlap:

```text
school_staff_employment_start_uniq    {schoolId, staffDocsId, effectiveFrom}   unique
school_staff_current_employment_uniq  {schoolId, staffDocsId, current}         unique, partial on current:true
```

So two records cannot **start** on the same day and two cannot be **current** — but a record
running `2024-01-01 → 2026-12-31` and another starting `2025-06-01` are both storable, and the
question *"what was this person's position on 3 March 2025"* has two answers.

**Is that a bug?** Probably not. A teacher may genuinely hold two concurrent part-time posts, and
nothing in the model says otherwise. But [#16](#e16) has to pick one:

- **a) Refuse any overlap.** Simple, and wrong for a school with a part-time music teacher who also
  runs the choir on a separate contract.
- **b) Allow overlap, and accept that "current position" is ambiguous** — `current = true` picks
  one arbitrarily, and [#8](#e8) shows whichever the index happened to allow.
- **c) Allow overlap but refuse a second *current* one**, which the index already does — so
  concurrent posts are expressible as history plus one current, and the "primary" post is the
  current one.

**Recommendation: (c).** It needs no new field, it is what the index already enforces, and it gives
"current position" a definite meaning: *the one the school considers primary*. [#16](#e16) then
closes the previous current record as it does today, and a genuinely concurrent second post is a
non-current record with an open `effectiveUntil` — which reads oddly and is honest.

## 2. `EmploymentStatus` has seven values and `current` is a separate boolean

```text
OFFERED  PROBATION  ACTIVE  ON_LEAVE  SUSPENDED  NOTICE_PERIOD  TERMINATED
```

**`current` and `status` can contradict each other**, and nothing stops them: a record with
`status = TERMINATED` and `current = true` is storable, as is `status = ACTIVE` with
`current = false`.

Which one does [#7](#e7)`?employed=true` read? They give different answers:

| | reads | includes |
|---|---|---|
| `current = true` | one record per person | a terminated person whose record nobody closed |
| `status` in a set | possibly several | excludes `ON_LEAVE`, which is wrong — they still work here |

**Recommendation: filter on `current = true` AND `status` not in `{OFFERED, TERMINATED}`.**
`current` says *which* record, `status` says *whether it counts*. And **[#17](#e17) must set both**
— `current = false` and a terminal `status` — in the same write, which is the check that keeps them
from diverging.

**`OFFERED` is the interesting one:** somebody who has accepted an offer but not started is a real
row that should not appear in a teacher picker. That is the case the `status` half of the filter
exists for.

## 3. A credential with no `validUntil` never expires — say so, do not infer it

`validUntil` is optional, and it should be: a degree does not expire, a first-aid certificate
does. But [#23](#e23) *"everything lapsing inside `?withinDays=`"* has to decide what a null means,
and the two readings are opposite:

- **null = never expires** → excluded from the report. Correct for a B.Ed.
- **null = unknown** → included, flagged as needing a date. Correct for a licence somebody entered
  in a hurry.

**Recommendation: null means never expires, and [#22](#e22) reports the count of undated
credentials separately.** Guessing that a missing date means "unknown" would put every degree in
the compliance report forever; showing the count lets a school notice the licence nobody dated
without drowning in the ones that genuinely have no expiry.

---

# Where the code will live

```text
controllers/people/staff/
├── README.md                   <- this file
└── StaffController.java        #1–#8, #16–#19, #20–#29

services/people/
├── StaffService.java           the person, the job
├── StaffRecordService.java     credentials, identities, bank accounts
├── helper/PeopleHelper.java    the rules MongoDB cannot express
└── utils/StaffServiceUtils.java  one staff member by id, and their current employment

repositories/people/staff/
├── StaffRepository.java              exists — findByIdAndSchoolId, built with #17 of academics
├── StaffRepositoryCustom.java + Impl
├── EmploymentRecordRepository.java
├── StaffCredentialRepository.java
├── StaffGovernmentIdentityRepository.java
└── StaffBankAccountRepository.java

dto/people/staff/{request,response}/
```

**One controller for twenty-two endpoints**, because they share `/staff/{id}` and a subject. The
three that are not staff-scoped — [#18](#e18), [#23](#e23), [#26](#e26), [#28](#e28) — sit here
too rather than in a file of their own: an `EmploymentRecord` has no life outside the person it
belongs to, and a controller split by URL prefix rather than by subject is two files for one thing.

**Two services, because the split is real.** `StaffService` owns the person and the job — the part
every other module depends on. `StaffRecordService` owns the three attachment types, all of which
are blocked on encryption, so the dependency runs one way and phase 1 never imports it.

**`StaffRepository` already exists**, built with `#17` of the academic-structure module so that a
section's `classTeacherDocsId` could be validated. It has one method; [#7](#e7) adds the custom
fragment.

---

# Appendix — what each field can hold

## `staff` — [Staff](../../../models/people/staff/Staff.java)

| Field | Type | What can be in it |
|---|---|---|
| `employeeNo` | String, required | **Generated, never accepted.** `NumberSequenceType.EMPLOYEE_NUMBER`, unique with `schoolId`. A display and search key; nothing references it. |
| `fullName` | String, required | **One field, not three.** Indexed with `schoolId`. The only field [#1](#e1) requires. |
| `dateOfBirth` | LocalDate | Personal. Returned by [#8](#e8), never in [#7](#e7)'s row. |
| `gender` | Enum | The shared `Gender`, not a people-specific one. |
| `nationalityCode` · `preferredLanguage` | String | ISO codes. |
| `phoneNumber` | String | **Normalised to international format** on the way in. |
| `emailAddress` | String | **Trimmed and lowercased** on the way in. **Not unique** — two staff may share a family address, and nothing references it. |
| `currentAddress` · `permanentAddress` | Embedded `StaffAddress` | `addressLine1`, `addressLine2`, `city`, `stateOrProvince`, `postalCode`, `countryCode`. Replaced as a pair by [#3](#e3). |
| `emergencyContact` | Embedded | `fullName`, `relationship`, `phoneNumber`. Replaced whole by [#4](#e4). |
| `profileImageDocsId` | String | A `DocumentRecord` id, validated to be this school's. |

## `employment_records` — [EmploymentRecord](../../../models/people/staff/EmploymentRecord.java)

| Field | Type | What can be in it |
|---|---|---|
| `staffDocsId` | String, required | Unique with `effectiveFrom` — two records cannot start the same day. |
| `positionDocsId` | String, required | An **active** `Position` of this school. |
| `managerDocsId` | String | Another `Staff`. **Not checked for cycles** — see [what this package will not have](#things-this-package-deliberately-will-not-have). |
| `status` | Enum, required | `OFFERED` · `PROBATION` · `ACTIVE` · `ON_LEAVE` · `SUSPENDED` · `NOTICE_PERIOD` · `TERMINATED`. See [open item 2](#2-employmentstatus-has-seven-values-and-current-is-a-separate-boolean). |
| `employmentType` | Enum, required | `FULL_TIME` · `PART_TIME` · `CONTRACT` · `TEMPORARY` · `SUBSTITUTE` · `INTERN`. |
| `effectiveFrom` | LocalDate, required | |
| `effectiveUntil` | LocalDate | Null means open-ended. |
| `probationUntil` | LocalDate | Optional. Nothing enforces it yet. |
| `current` | Boolean, required | **One per staff member**, by a partial unique index filtered to `true`. [#16](#e16) flips the old one in the same write. |
| `separationReason` | String | Set by [#17](#e17). |

## `staff_credentials` — [StaffCredential](../../../models/people/staff/StaffCredential.java)

| Field | Type | What can be in it |
|---|---|---|
| `credentialType` | Enum, required | `EDUCATIONAL_QUALIFICATION` · `TEACHING_LICENSE` · `PROFESSIONAL_CERTIFICATION` · `BACKGROUND_CHECK` · `MEDICAL_CLEARANCE` · `FIRST_AID_CERTIFICATION` · `DRIVING_LICENCE` · `WORK_PERMIT` |
| `title` · `issuingAuthority` | String | Free text. **Phase 3 — not encrypted.** |
| `encryptedCredentialNumber` · `credentialNumberLookupHash` | String | **Phase 6.** The unique index is partial on the hash being a string, so a credential with no number is storable — which is what makes the phase split work. |
| `issuedOn` · `validUntil` | LocalDate | `validUntil` null means **never expires** — see [open item 3](#3-a-credential-with-no-validuntil-never-expires--say-so-do-not-infer-it). |
| `verificationStatus` | Enum, required | `UNVERIFIED` · `PENDING` · `VERIFIED` · `REJECTED`. [#21](#e21) writes it. |
| `verifiedByDocsId` · `verifiedAt` | String · Instant | Who and when. |
| `evidenceDocumentDocsId` | String | The scan. |

## `staff_government_identities` — [StaffGovernmentIdentity](../../../models/people/staff/StaffGovernmentIdentity.java)

**Phase 6, blocked.** `identityType` is `AADHAAR` · `PAN` · `PASSPORT` · `NATIONAL_ID` · `TAX_ID` ·
`DRIVING_LICENCE` · `VOTER_ID` · `APAAR` · `PEN`, unique per staff member per type. The triple is
`encryptedIdentityNumber` (recoverable), `identityNumberLookupHash` (**keyed** — a bare digest is
brute-forceable from a stolen index in seconds) and `maskedIdentityNumber` (what a UI shows).
`verificationStatus` is the shared `IdentityVerificationStatus`, not the credential one.

## `staff_bank_accounts` — [StaffBankAccount](../../../models/people/staff/StaffBankAccount.java)

**Phase 6, blocked.** Same encryption triple on the account number, plus `accountHolderName`,
`bankName`, `branchName`, `ifscCode`, `primaryAccount`, `active`, `verifiedByDocsId`,
`verifiedAt`, `statusReason`. **One primary per staff member**, by a partial unique index filtered
to `{primaryAccount: true, active: true}` — correctly written, unlike the two index bugs this
project has already found.

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `STAFF_NOT_FOUND` | 404 | no staff member with that id in this school |
| `EMPLOYMENT_RECORD_NOT_FOUND` · `CREDENTIAL_NOT_FOUND` · `IDENTITY_NOT_FOUND` · `BANK_ACCOUNT_NOT_FOUND` | 404 | — |
| `POSITION_NOT_ACTIVE` | 409 | [#16](#e16) — hiring into a retired seat |
| `POSITION_FULL` | *warning* | [#16](#e16) — approved headcount reached. **Reported, not refused**; a school hiring a twelfth teacher into eleven seats is a budget conversation, not a data error |
| `EMPLOYMENT_ALREADY_CURRENT` | 409 | [#16](#e16) — a current record exists and the body did not say to close it |
| `NOT_EMPLOYED` | 409 | [#17](#e17) — separating somebody with no current record |
| `INVALID_EMPLOYMENT_RANGE` | 400 | `effectiveUntil` before `effectiveFrom` |
| `EMPLOYMENT_START_TAKEN` | 409 | two records starting the same day — what the unique index declares |
| `STAFF_STILL_EMPLOYED` | 409 | [#6](#e6) — archiving somebody with employment history. Use [#17](#e17) |
| `STAFF_NAME_REQUIRED` | 400 | `"fullName": ""` — replaced, never removed |
| `IDENTITY_ALREADY_RECORDED` · `BANK_ACCOUNT_ALREADY_RECORDED` · `CREDENTIAL_ALREADY_RECORDED` | 409 | **and none of them names whose** |
| `ENCRYPTION_UNAVAILABLE` | 503 | phase 6, until the key vault exists |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

## The person · 1–8

<a id="e1"></a>
**[1](#t1) · `POST /staff`** — built

- [`staff`](../../../models/people/staff/Staff.java) — *insert*: `schoolId`, `employeeNo` **generated**, `fullName`, and whatever personal fields arrived
- **`employeeNo` is generated, never accepted.** `NumberSequenceService.next(schoolId, EMPLOYEE_NUMBER, "EMP/{YYYY}/{MM}/")` was already built and already used by school creation and subscriptions. **Atomic**, so two simultaneous creates cannot be handed the same number, and **per school**, so two schools both hold `EMP/2026/09/000001` without colliding. A caller-supplied number lets two conventions collide inside one tenant, and nobody should pick their own staff number. Sending one is **ignored, not refused** — the ordinary shape for a field the request record does not declare.
- **The shape is `{PREFIX}/{YYYY}/{MM}/{000001}`, and that is the house style for every generated number** — `SUBSCRIPTION` already used `SUB/{YYYY}/{MM}/`, and `EMPLOYEE_NUMBER` was brought into line on 2026-09-15. Anything added to `NumberSequenceType` should follow it.
- **The counter's shape is set by the first caller, not by this line.** School provisioning creates it with a padding width of 6 and *no* template; `next` writes the first template it is given onto the counter, so every later number in that school's life reads the same — `EMP/2026/09/000001`, the example on `Staff.employeeNo`.
- **So changing the template in code does not restyle a school already numbering**, and must not: a number somebody has written down does not change under them. Restyling one means clearing its stored `prefixTemplate` deliberately — done once, for `wah-ji-wah`, on 2026-09-15.
- ~~**`fullName` is the only required field.**~~ **Overruled twice on 2026-09-15; five fields are required.**
  - **`dateOfBirth` and `gender`, by the model.** [`Staff`](../../../models/people/staff/Staff.java) declares both `@NotNull`.
  - **`phoneNumber` and `emailAddress`, by the school.** The model leaves them optional and this endpoint does not: a staff record with no way to contact the person is one the office has to chase later, and "we will fill it in afterwards" is what left 765 rows without a phone number.
  - **`"---"` is refused** — `400 STAFF_PHONE_REQUIRED`. It passes `@NotBlank` and then normalises away to nothing, which would be a blank number behind a request that looked valid.
  - **The consequence worth naming:** [`#7`](#e7)'s `?hasEmail=false` — "who are we missing contact details for" — can no longer match anybody created here. It still matches the pre-rule rows, which is exactly who it is for, and this rule is what stops that list growing. Nothing validates a document on save, so the plan's version would have stored rows violating their own declared constraints — invisible until something read them expecting a date. This is the same call [#13](../organization/README.md#e13) made about `approvedHeadcount`: **relaxing it is a model change, and not this endpoint's to make.** The original reasoning still stands and is why it is worth revisiting: a school entering two hundred people at the start of term has a name and nothing else on day one.
- **Everything else arrives through [#2](#e2), [#3](#e3) and [#4](#e4)**, and every optional field is accepted here too — a school that *does* have the details should not have to make three more calls.
- **An empty address or emergency contact is stored as absent**, not as an object of nulls: they are the same fact, and a reader should not have to tell them apart. A partly filled one is kept — a city and nothing else is real.
- **No `active`, no status, no department, no joining date.** A person is not employed by existing — that is [#16](#e16). This endpoint creates the human being, and the distinction is the package's whole design.
- **Email trimmed and lower-cased; phone stripped of spacing characters** — `"+91 98765-43210"` becomes `"+919876543210"`, which is the example on the model's own field.
- ~~**A duplicate email is not refused.**~~ **Overruled 2026-09-15: both a phone number and an email address identify one person within a school**, and a duplicate of either is refused — `409 STAFF_PHONE_TAKEN`, `409 STAFF_EMAIL_TAKEN`. The original reasoning was that two staff genuinely may share a family address; the new rule says a school that enters the same address twice has almost certainly entered the same person twice.
- **Checked AFTER normalising, or the check is a lie.** `"+91 98765-43210"` collides with `"+919876543210"` and `"Anita@X.com"` with `"anita@x.com"`. Comparing the raw strings would pass both, store both, and leave the school a duplicate the index would have refused had it ever been built.
- **`school_staff_phone_uniq` and `school_staff_email_uniq` stay PARTIAL even though both fields are now required**, because the 765 rows written before that rule have neither. `partialFilterExpression: { field: { $type: 'string' } }`, the same shape [`UserAccount`](../../../models/identity/UserAccount.java) uses. Neither field is required, and **a plain unique index treats every missing value as null** — one school could then hold exactly ONE person with no phone, with the database refusing the second and no message explaining why. **Measured 2026-09-15: 74 schools already hold more than one person with no phone.** This is the `school_year_class_code_uniq` defect this project shipped once already, and it cost a 659-document migration.
- **The service checks are the enforcement, not the indexes.** Both are declared on the model but built on demand (`app.mongo.sync-indexes`), so a database that has never synced carries no constraint at all — where they are built, the checks turn a duplicate-key 500 into a 409 naming the field and what to do about it.
- **School-scoped, both of them.** Another school may hold the same number and the same address, which is what makes them per-tenant identity rather than global.
- **No country code is invented, and that is a deliberate gap in "normalised to international format".** Turning a bare `9876543210` into `+91…` means guessing from the school's country, which needs a dialling-code table this project does not have. **A wrong guess writes a number that looks right and cannot be called** — worse than a national number that is obviously national. A number given without a `+` is stored as given.
- **The email is trimmed in the request record, not the service**, which is the one piece of normalisation that does not live where the rest of it does. `@Email` validates the *constructed* record and a compact constructor runs first, so without it a paste from a spreadsheet is refused as malformed — and a paste from a spreadsheet is exactly how a school enters two hundred people.
- **The response leads with `employeeNo`**, because it is the thing the school writes down.

<a id="e2"></a>
**[2](#t2) · `PATCH /staff/{id}`**

- *updates*: `fullName`, `dateOfBirth`, `gender`, `nationalityCode`, `preferredLanguage`, `phoneNumber`, `emailAddress`
- **Never `employeeNo`** — generated, and printed on things. A rename leaves a paper trail pointing at nobody.
- **Not the addresses, the emergency contact or the photo** — [#3](#e3), [#4](#e4), [#5](#e5). Each is replaced whole, for the reason the school's own address is: half a changed address is a delivery to the wrong place.
- `"fullName": ""` is `400 STAFF_NAME_REQUIRED`; an empty body is `400 NOTHING_TO_UPDATE`.

<a id="e3"></a>
**[3](#t3) · `PUT /staff/{id}/addresses`**

- *updates*: `currentAddress`, `permanentAddress`
- **Both, as a pair, because "same as current" is a real answer** — and expressing it as two independent PATCHes leaves a window where they disagree.
- Either may be cleared. A person with no recorded address is a normal state, not a missing one.

<a id="e4"></a>
**[4](#t4) · `PUT /staff/{id}/emergency-contact`**

- *updates*: `emergencyContact` — `fullName`, `relationship`, `phoneNumber`
- **Whole or not at all.** A contact with a new name and an old phone number is worse than no contact, because somebody will trust it in the one situation where it matters.

<a id="e5"></a>
**[5](#t5) · `PUT /staff/{id}/photo`**

- *updates*: `profileImageDocsId`, or clears it
- **A `DocumentRecord` id, validated to exist and to be this school's.** The file is object storage's; this stores a pointer, the same arrangement `CurriculumDocument` uses.

<a id="e6"></a>
**[6](#t6) · `POST /staff/{id}/archive`**

- *updates*: `recordState` = `ARCHIVED`, `archivedAt` — the `SchoolBase` lifecycle, not a new field
- **For a profile created by mistake, and nothing else**: a duplicate entry, a test record, a name typed into the wrong school.
- **Refused if the person has any employment record at all** — `409 STAFF_STILL_EMPLOYED`. Somebody who worked here leaves through [#17](#e17) and keeps their history; archiving them hides a person whose payslips and attendance still name them.
- **Not reversible through this endpoint.** Unarchiving is deliberately missing until somebody needs it.

<a id="e7"></a>
**[7](#t7) · `GET /staff`** — built, **minus the employment filters**

- [`staff`](../../../models/people/staff/Staff.java) — *reads*: the filtered, **paged** set
- **Built 2026-09-15 on `staff` alone.** The four filters that make this a teacher picker —  `?employed=`, `?departmentDocsId=`, `?positionDocsId=`, `?employmentType=` — every one lives on [`employment_records`](../../../models/people/staff/EmploymentRecord.java), which [#16](#e16) writes and which **has no repository and no collection in the database at all**. They are not accepted: a parameter that silently matches nothing is worse than one documented as absent, because a filter that looks like it works and returns an empty page reads as "there are no teachers here". They are **ignored rather than refused**, like every field not on a request record.
- **When #16 lands, the shape is already settled** — [the domain plan's open item 3](../README.md#3-7s-filters-need-a-join-mongo-will-not-do): two queries, **paging on `employment_records`**, then read `staff` by id. Every narrowing filter lives on the first collection, so paging the second gives pages that shrink after filtering. The fields below are facts about a *person* and do not move when that arrives.
- **`?employed=true` will mean `current = true` AND `status` not in `{OFFERED, TERMINATED}`** — see [open item 2](#2-employmentstatus-has-seven-values-and-current-is-a-separate-boolean). Somebody who accepted an offer but has not started should not appear in a teacher picker; somebody `ON_LEAVE` should.
- **What is built**: `?search=` across `fullName` **OR** `employeeNo` — an office looks somebody up by name and a payroll run looks them up by number — plus `?gender=`, `?nationalityCode=` (case-insensitive, matching how [#1](#e1) stores it), `?hasEmail=` and `?hasPhone=`.
- **`?hasEmail=false` is the one worth naming.** *"Who are we missing contact details for"* is a real question a school asks at the start of term, and this is the only way to ask it. **Asked with `exists`, not a null comparison**, because #1 stores an absent email as no key at all — the same reason [#12](../organization/README.md#e12) asks `exists` for `topLevelOnly`.
- **The search needle is regex-quoted**, so a caller typing `O'Brien (acting)` searches for those characters rather than injecting a group, and a stray `(` is an empty page instead of a `500`.
- **The row is deliberately thin**: name, employee number, gender, phone, email. **No date of birth, no address, no emergency contact.** Those are [#8](#e8), one call away — a list endpoint returning them puts every employee's personal data in every dropdown's network tab, and [this module has no authorization yet](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization), so "only the staff screen calls it" is not a control.
- **The phone and email ARE on the row**, and that is a line rather than an inconsistency: a staff list is a contact list, and the office reading it is looking for somebody to ring. A date of birth is never what a picker needs.
- **The sort allowlist is part of that decision, not paperwork.** `fullName` · `employeeNo` · `createdAt` · `updatedAt`. A sort field taken from the query string orders by anything on the document — **including the fields the row withholds, which leaks their values through the ordering**. `?sort=dateOfBirth` is a `400`.
- **Sorted by `fullName`, tiebroken by `employeeNo`.** Two people genuinely share a name — the most ordinary thing in a school roll — so `fullName` alone ties, and a tie with no tiebreaker puts one row on two pages while another appears on none.
- **No gates.** A suspended school still reads its own staff list.

<a id="e8"></a>
**[8](#t8) · `GET /staff/{id}`** — built, **minus the employment block**

- [`staff`](../../../models/people/staff/Staff.java) — *reads*: the person
- **The full profile** — both addresses, the emergency contact, the date of birth. **The fullest thing this product returns about a human being**, which is exactly why [authorization](../README.md#2-this-is-the-module-that-cannot-ship-without-authorization) is the open item that matters most, and why [#7](#e7)'s row carries none of it: a list is read by every dropdown, and this is read by one page. There is no authorization, and the response repeats that in a `note` on every read rather than leaving it to a README nobody opens.
- **The employment is folded in rather than linked**, because "who is this and what do they do" is one question and every caller would make the second call anyway. **It is not built**: [#16](#e16) writes the record and there is no `employment_records` collection at all.
- **The key is absent — not `null`, not `{}`** — and `employmentNote` says why. Three ways of saying "nothing here" is three cases a client has to handle, and an absence like that otherwise reads as a bug in the caller's own code.
- **That shape is not scaffolding.** A person with no employment record stays a real state once #16 exists — somebody the school has entered and not yet hired, which is what [#1](#e1) leaves them in. It is what that person will always look like.
- **Scoped by `schoolId`, never by id alone.** Another school's real id is a real id, and on this endpoint an unscoped lookup does not leak a name — it leaks a date of birth, a home address and an emergency contact. The refusal says nothing about the person either.
- **The id is trimmed** before the lookup, so a padded path parameter resolves rather than 404ing.
- **No gates.** A suspended or closed school still reads its own people.

## The job · 16–19

<a id="e16"></a>
**[16](#t16) · `POST /staff/{id}/employment`** — built

- [`staff_positions`](../../../models/people/organization/Position.java) — *reads*: the position, which must be **active**
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *reads*: the current record, if any
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *updates*: the previous record — `current` = `false`, `effectiveUntil`
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *insert*: the new one, `current` = `true`
- **Hire, promote and transfer are one endpoint because they are one event.** Three endpoints doing this would be three chances to leave two records current.
- **It closes the previous record in the same write** — `effectiveUntil` set to the day before the new `effectiveFrom`. The partial unique index is the backstop; this check is what turns a duplicate-key 500 into a readable 409.
- **`409 POSITION_NOT_ACTIVE`** for a retired seat. **`POSITION_FULL` is a warning, not a refusal** — a school hiring a twelfth teacher into eleven approved seats is a budget conversation, and refusing it stops the system recording something that has already happened. The same call the term-weight sum made.
- **Overlap with non-current records is not checked**, pending [open item 1](#1-may-two-employment-records-overlap--decide-before-16).
- ~~**this project configures no Mongo transaction manager**~~ — **out of date, corrected 2026-09-15.** [`MongoTransactionConfig`](../../../config/MongoTransactionConfig.java) registers a `MongoTransactionManager` and Atlas is a replica set, so `@Transactional` does what it says: the close and the insert commit together or neither does. No `version` check was needed.
- **The order is forced by the index, not chosen.** `school_staff_current_employment_uniq` is unique and partial on `current: true`, so a new current record cannot be inserted while the old one still is — close first, insert second. **Without the transaction a failure between them would leave the person with no current record at all**, which reads as unemployed and is worse than the duplicate the index already prevents.
- **The end date is computed, never sent.** The closed record ends the day before the new one begins, and there is no `effectiveUntil` on the request at all — two people typing two dates is how a gap or an overlap gets in.
- **`400 EMPLOYMENT_STATUS_TERMINAL`.** A record cannot be created already `TERMINATED`: current and finished at once is the contradiction [open item 2](#2-employmentstatus-has-seven-values-and-current-is-a-separate-boolean) warns about, and nothing in the model stops it. `OFFERED` is allowed and is the case that matters.
- **`409 EMPLOYMENT_ALREADY_STARTS_THEN`** in front of `school_staff_employment_start_uniq`, counting closed records too, and **`409 EMPLOYMENT_STARTS_BEFORE_CURRENT`** when the close would end a record before its own start.
- **`400 MANAGER_IS_SELF`**, and the manager is validated to be this school's. A **person**, not a seat — `Position.reportsToPositionDocsId` answers the structural question and this answers "who do I actually report to", which is why both exist.
- **Overlap with non-current records is not checked**, settling [open item 1](#1-may-two-employment-records-overlap--decide-before-16) on **(c)**: a part-time music teacher who also runs the choir on a separate contract is real, and `current` then means the post the school considers primary — which is exactly what the unique index already enforces.
- **[#8](#e8) folds the record in from the moment this exists**, and its "nobody is employed anywhere" note was rewritten in the same change: leaving a note that had become false would be worse than having none.

<a id="e17"></a>
**[17](#t17) · `POST /staff/{id}/separate`**

- *updates*: the current record — `current` = `false`, `effectiveUntil`, `status`, `separationReason`
- **The one way somebody leaves**, and the reason there is no `DELETE`. The person and every record attached to them stay exactly where they are.
- **It must set `current` and a terminal `status` together** — that pairing is what stops the two fields contradicting each other, which nothing else prevents.
- `409 NOT_EMPLOYED` when there is no current record.

<a id="e18"></a>
**[18](#t18) · `PATCH /employment/{id}`**

- *updates*: `effectiveFrom`, `effectiveUntil`, `managerDocsId`, `probationUntil`, `status`
- **Never `current`** — that is [#16](#e16) and [#17](#e17), which move two records together. A PATCH that could set it is exactly how two records end up current.
- **Never `positionDocsId`** either: moving somebody to a different seat is a transfer, which is a new record, which is [#16](#e16). Editing it in place would rewrite where they were last year.
- Addressed by the **record's** id rather than the staff member's, because a person has several and the URL has to say which.

<a id="e19"></a>
**[19](#t19) · `GET /staff/{id}/employment`**

- *reads*: every record for one person, newest `effectiveFrom` first, with the current one marked
- **Not paged.** Nobody has a hundred employment records, and a cursor on a five-row list is machinery nobody uses — the argument the term list eventually lost, and wins here.

## The records · 20–29

<a id="e20"></a>
**[20](#t20) · `POST /staff/{id}/credentials`** — **split across phases.** Title, authority, type, dates and evidence are phase 3; the credential *number* is encrypted and phase 6. The unique index is partial on the hash being a string, so a credential with no number stores cleanly — which is what makes the split work rather than merely tolerable. A duplicate number is `409 CREDENTIAL_ALREADY_RECORDED` **without naming whose**.

<a id="e21"></a>
**[21](#t21) · `POST /credentials/{id}/verify`** — sets `verificationStatus`, `verifiedByDocsId`, `verifiedAt`. Takes the new status in the body because there are four, not two — `UNVERIFIED` · `PENDING` · `VERIFIED` · `REJECTED` — so the deactivate/reactivate pair shape does not fit. A rejection wants a reason; the model has no field for one, which is worth noticing before this is built.

<a id="e22"></a>
**[22](#t22) · `GET /staff/{id}/credentials`** — with expiry **computed from `validUntil`**, never stored, the same way the grading module recomputes its gap warning. **A null `validUntil` means never expires**, and the response carries a separate count of undated credentials — see [open item 3](#3-a-credential-with-no-validuntil-never-expires--say-so-do-not-infer-it).

<a id="e23"></a>
**[23](#t23) · `GET /credentials/expiring`** — **the compliance report**, and the reason `school_credential_status_expiry_idx` exists on `{schoolId, verificationStatus, validUntil}`. `?withinDays=` is the query an inspection actually asks. Undated credentials are excluded, counted separately, and never silently treated as expiring.

<a id="e24"></a>
**[24](#t24) · `POST /staff/{id}/identities`** — **phase 6, blocked.** Encrypts on the way in, stores the keyed hash beside it, returns only the mask. **The duplicate check is on the hash and the refusal never names the other person** — `409 IDENTITY_ALREADY_RECORDED` and nothing more, or the endpoint becomes a way to enumerate staff by guessing numbers. One per type per staff member, by unique index.

<a id="e25"></a>
**[25](#t25) · `GET /staff/{id}/identities`** — **masked only, at every permission level.** The plaintext is not on this endpoint and no parameter adds it.

<a id="e26"></a>
**[26](#t26) · `POST /identities/{id}/reveal`** — **a POST, and that is the design.** Revealing a national identity number is an event that must be recorded; a GET that writes an audit row is a GET that lies about being safe to retry, and something will retry it. It needs an audit writer this project does not have — which is the same gap `controllers/core` records for its unlock endpoint.

<a id="e27"></a>
**[27](#t27) · `POST /staff/{id}/bank-accounts`** — **phase 6, blocked.** Same triple. **One primary per staff member**, by a partial unique index filtered to `{primaryAccount: true, active: true}` — so a second primary is refused while a retired old account keeps its flag. Correctly written, unlike the two index bugs this project has already found.

<a id="e28"></a>
**[28](#t28) · `POST /bank-accounts/{id}/verify`** — records a penny-drop or document check. **Payroll should refuse to pay an unverified account, and that is payroll's rule to make** — this endpoint records the fact, it does not enforce the consequence.

<a id="e29"></a>
**[29](#t29) · `GET /staff/{id}/bank-accounts`** — masked, primary marked, inactive included. A closed account stays readable because a payslip issued against it has to stay explicable.
