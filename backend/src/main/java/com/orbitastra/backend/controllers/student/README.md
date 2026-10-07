# controllers/student — API plan

**Four of twenty-two are built** — [#1](#e1), [#4](#e4), [#5](#e5) and [#6](#e6), on 2026-10-06.
They are what [`controllers/README.md`](../README.md) calls **phase 5: "the minimum, not the
module"**. They exist to unblock [`crm` #33](../crm/README.md#e33), the handover where an applicant
becomes a child on a register, and that endpoint went in the same day. Everything else here is
still a plan.

This is the full set of endpoints the student record needs, written before any
of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md), [`controllers/plans`](../plans/README.md),
[`controllers/people`](../people/README.md),
[`controllers/academics/timetable`](../academics/timetable/README.md) and
[`controllers/crm`](../crm/README.md) were done.

Built endpoints will be marked **built** in the `#` column. Anything unmarked does not exist yet,
and a request to it returns a 404.

Mirrors [`models/student`](../../models/student) — three collections, one embedded type, two enums.
That package's README is a **persistence contract**; this file is what may be done to it over HTTP.

**Build order for this module and [`crm`](../crm/README.md) together is in
[`controllers/README.md`](../README.md)**, because the two interleave: admissions runs until it
physically cannot continue, this module unblocks it, and then both finish.

> **Five things were measured before writing this — 2026-09-21.** The models were read as written
> and checked against the rest of the codebase rather than taken on trust.
>
> **1. A guardian's phone number is UNIQUE per school, and that decides the create endpoint.**
> `school_guardian_phone_uniq` is `{schoolId, phoneNumber}`, unique, partial on the field existing —
> and `school_guardian_email_uniq` is the same for email. **Two siblings share a father.** So
> [#1](#e1) cannot create a guardian row per student; it must match an existing guardian and link.
> Getting this wrong is not a style question — the second sibling's admission fails on a
> duplicate-key error. See [#1](#e1) and [open item 1](#1-what-counts-as-the-same-guardian).
>
> **2. One `ACTIVE` academic record per student per year is already enforced by the database.**
> `school_year_student_active_academic_record_uniq` is unique on
> `{schoolId, academicYear, studentDocsId, status}`, partial on `status: 'ACTIVE'`. A mid-year
> section move therefore **closes one record and opens another** — it cannot be a `PATCH` of
> `classDocsId` on a record that stays `ACTIVE`, because for an instant two would exist. That is
> [#17](#e17), and it is why [#16](#t16) cannot move a student between classes.
>
> **3. Roll numbers are scoped per class and section, and the number service cannot do that yet.**
> `school_year_class_section_active_roll_uniq` is unique on
> `{schoolId, academicYear, classDocsId, sectionNo, rollNo, status}`. But
> [`NumberSequenceService.next`](../../services/institution/NumberSequenceService.java) hard-codes
> `GLOBAL_SCOPE` — the *repository* takes a `scopeKey`
> (`allocate(schoolId, type, scopeKey)`), the service never passes one. **`STUDENT_ROLL_NUMBER`
> cannot be generated until that overload exists.** See
> [open item 2](#2-roll-numbers-need-a-scoped-sequence-that-does-not-exist).
>
> **4. `admissionNo` needs nothing built.** `NumberSequenceType.STUDENT_ADMISSION` exists and
> `NumberSequenceService.next` is the same call [`StaffService`](../../services/people/StaffService.java)
> already makes for `employeeNo`. It is school-global, so `GLOBAL_SCOPE` is correct for it — the
> problem in finding 3 is specific to roll numbers.
>
> **5. A student does not need an admission application.** `Student.admissionApplicationDocsId` is
> nullable — *"Optionally references AdmissionApplication.id when converted from CRM"* — and
> `school_admission_application_uniq` is **partial** on the field existing. So this module stands on
> its own: transfers, walk-ins and the roll a school already has when it starts using the product
> all work with no CRM row anywhere. **The dependency runs one way**, and only at
> [`crm` #33](../crm/README.md#e33).

---

## What this module is

**The child, once the school has accepted them.** A `Student` is the person and their identity; a
`Guardian` is a person the school contacts; a `StudentAcademicRecord` is **where that child sits in
one academic year** — class, section, roll number, from when until when.

**Three collections because they change at different rates.** A student's name changes almost never.
A guardian's phone changes occasionally, and the same guardian belongs to several students. Where a
child sits changes every year, and sometimes mid-year. Folding any two together means rewriting a
document to record a fact that did not change.

**The academic record is the one every other module actually wants.** Attendance marks a section's
roster. A mark sheet is a class's students. A fee is raised against a child in a grade. All of them
ask [#21](#e21), not [#5](#t5).

## What this module is not

- **Not admissions.** [`crm`](../crm/README.md) owns everything before the child is a student, and
  hands over exactly once at [#33](../crm/README.md#e33).
- **Not attendance, marks, fees, transport, health or conduct.** Each is its own module keyed on
  `studentDocsId`. This module answers *who* and *where*, and nothing about what happened to them.
- **Not promotion.** Moving a whole section up a grade at year end is a **job**, not an endpoint —
  it is hundreds of records, it needs a dry run, and it needs to be resumable. [#14](#e14) creates
  one record. See [things this module will not have](#things-this-module-deliberately-will-not-have).
- **Not the guardian's login.** `GuardianLink.portalAccess` is a flag this module stores. The portal
  that reads it is a different surface with its own auth story.
- **Not documents.** `profilePhotoDocumentId` is a `DocumentRecord` id. This module stores the id;
  [`documents`](../../models/documents) owns the file.

## One surface, and why

```text
/schools/current/students
/schools/current/guardians
/schools/current/academic-years/{year}/student-records
```

**The year is in the path for records and nowhere else**, and that is the deliberate opposite of
what [`crm`](../crm/README.md#one-surface-and-why) decided:

> **In `crm` the year is a property; here it is genuinely the scope.**
> `StudentAcademicRecord` extends
> [`AcademicStudentSchoolBase`](../../models/base/AcademicStudentSchoolBase.java), which carries
> `academicYear` because a record means nothing without one — the same reason every route in
> [`academics`](../academics/timetable/README.md) has the segment. A `Student`, by contrast, has no
> year at all: the child exists across all of them, which is the whole point of splitting the two.

So `/students/{id}` is never year-scoped, and `/student-records` always is. **The one exception is
[#20](#t20)** — a student's history *across* years — which hangs off the student because the
question is about the child, not about a year.

**Writes that are events get a verb.** `POST /students/{id}/status` rather than a `PATCH` that sets
`status`, and `POST /student-records/{id}/close` rather than a `PATCH` that sets `effectiveUntil`.
`StudentStatus` has seven values with real preconditions on each move; a single `PATCH` would be
seven endpoints wearing one name. Same call [`crm`](../crm/README.md#one-surface-and-why) and
[`people` #18b](../people/staff/README.md) made.

## Which gates every endpoint runs

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | every write |
| **2** | Is the subscription usable | every write |
| **4** | Is the year the running one | **never — see below** |

**Gate 4 does not run here either, and the reason is [`crm`](../crm/README.md#which-gates-every-endpoint-runs)'s.**
A school admits a child in January for a year that starts in June, and [#14](#e14) is how that child
gets a class and a section. If placing a student required the year to be running, the entire
admissions handover would refuse — a school would be unable to tell a family which section their
child is in until the day term starts.

**What replaces it** is `AcademicYear` having to exist, plus the class and section belonging to that
year ([`CLASS_NOT_IN_YEAR`](#the-refusal-codes-this-module-introduces)). That is a check on the
*year's contents*, not on which year is running.

This is a **decision, not a measurement** — see
[open item 3](#3-gate-4-is-off-and-that-is-a-choice).

**No gate runs on a read.** A suspended school still reads its students; they are still its students.

---

# The endpoints

Numbered by area, not by build order. **Build order is in
[`controllers/README.md`](../README.md)** and differs.

## 1. The student — writes · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /students`](#e1) | Admit a child. **Creates the student and matches or creates their guardians.** | [`students`](../../models/student/Student.java), [`guardians`](../../models/student/Guardian.java) |
| <a id="t2"></a>2 | [`PATCH /students/{id}`](#e2) | Correct name, date of birth, contact, photo. | `students` |
| <a id="t3"></a>3 | [`POST /students/{id}/status`](#e3) | Move through the status graph, with a reason. | `students` |

## 2. The student — reads · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t4"></a>4 — **built** | [`GET /students`](#e4) | **The roll.** Filtered by status, gender, placed, from-admissions. **Not by class** — see the entry. | [`students`](../../models/student/Student.java) |
| <a id="t5"></a>5 — **built** | [`GET /students/{id}`](#e5) | One child in full, with guardians resolved. **No academic record** — #14 is not built. | [`students`](../../models/student/Student.java), [`guardians`](../../models/student/Guardian.java) |
| <a id="t6"></a>6 — **built** | [`GET /students/search?phone=&admissionNo=&name=`](#e6) | **Is this child already here?** Asked before every admission. | [`students`](../../models/student/Student.java), [`guardians`](../../models/student/Guardian.java) |

## 3. The guardian — writes · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t7"></a>7 | [`POST /guardians`](#e7) | Add a contact who is not being created with a child. | `guardians` |
| <a id="t8"></a>8 | [`PATCH /guardians/{id}`](#e8) | Correct the contact. **Changes it for every child they are linked to.** | `guardians` |

## 4. The guardian — reads · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t9"></a>9 | [`GET /guardians?phone=&email=&name=`](#e9) | **Find the existing one before making a second.** | `guardians` |
| <a id="t10"></a>10 | [`GET /guardians/{id}`](#t10) | One contact and **every child they are attached to.** | `guardians`, `students` |

## 5. The link between them · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t11"></a>11 | [`POST /students/{id}/guardians`](#e11) | Attach a guardian to a child. | `students`, `guardians` |
| <a id="t12"></a>12 | [`PATCH /students/{id}/guardians/{guardianDocsId}`](#e12) | Change the flags — primary, emergency, pickup, portal. | `students` |
| <a id="t13"></a>13 | [`DELETE /students/{id}/guardians/{guardianDocsId}`](#e13) | Detach. **Unlinks; never deletes the guardian.** | `students` |

## 6. The academic record — writes · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t14"></a>14 | [`POST /academic-years/{year}/student-records`](#e14) | **Put a child in a class and section.** | [`student_academic_records`](../../models/student/StudentAcademicRecord.java), `students` |
| <a id="t15"></a>15 | [`PATCH /academic-years/{year}/student-records/{id}`](#t15) | Correct the roll number or the dates. **Not the class.** | `student_academic_records` |
| <a id="t16"></a>16 | [`POST /academic-years/{year}/student-records/{id}/close`](#e16) | End it — completed, transferred, withdrawn. | `student_academic_records`, `students` |
| <a id="t17"></a>17 | [`POST /academic-years/{year}/student-records/{id}/move`](#e17) | **Change section mid-year.** Closes this record, opens the next. | `student_academic_records`, `students` |

## 7. The academic record — reads · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t18"></a>18 | [`GET /academic-years/{year}/student-records`](#t18) | Every placement in a year, filtered. | `student_academic_records` |
| <a id="t19"></a>19 | [`GET /academic-years/{year}/student-records/{id}`](#t19) | One placement. | `student_academic_records` |
| <a id="t20"></a>20 | [`GET /students/{id}/academic-records`](#e20) | **A child's whole history**, newest year first. | `student_academic_records` |
| <a id="t21"></a>21 | [`GET /academic-years/{year}/classes/{classDocsId}/sections/{sectionNo}/roster`](#e21) | **THE ROSTER.** What attendance, marks and timetable all call. | `student_academic_records`, `students` |

## 8. The numbers · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t22"></a>22 | [`GET /academic-years/{year}/student-strength`](#e22) | Headcount per class and section, against capacity. | `student_academic_records`, `school_classes` |

---

# Things this module deliberately will not have

- **No `DELETE` on a student or a guardian.** A child who left is `TRANSFERRED` or `WITHDRAWN`; a
  guardian who is no longer a contact is unlinked ([#13](#e13)). A school's obligation to hold a
  record of a child it taught outlives the child's attendance, and a delete endpoint is how that gets
  lost in one click. [#13](#e13) is the only `DELETE` here and it removes a *link*.
- **No bulk promotion.** Moving every section up a grade at year end is the single most-wanted
  operation in this module and it is **not an endpoint**. It is hundreds of records, it needs a dry
  run, a report and the ability to resume; as a `POST` it is a request that times out halfway with
  no way to know what it did. It belongs with the other jobs.
- **No CSV import of the existing roll.** Same argument. It is the first thing a school onboarding
  needs and it is a job with a file, a validation pass and a rollback.
- **No merge of two students.** A child entered twice is fixed by withdrawing one and correcting the
  other, not by a tool that has to reconcile two attendance histories.
- **No guardian portal auth.** [#12](#e12) sets `portalAccess`. What reads it is not this module.

---

# To settle before building

## 1. What counts as "the same guardian"

**Settled 2026-10-06, exactly as recommended below.** [#1](#e1) matches on the stored phone, then on
the email, links what it finds, leaves the stored name alone, and returns `matched` per guardian.
Two entries sharing a phone — or an email — are refused. A guardian with neither is always a new
row. The one thing worth adding after building it: the match is on the **stored** number rather than
the bare digits, so a country code given one time and not the next is still two rows, which is what
the unique index thinks too.

`school_guardian_phone_uniq` and `school_guardian_email_uniq` mean the database has already decided
that **a phone number identifies a guardian within a school**. [#1](#e1) therefore has to match on
it. The questions it did not answer, and the answers it got:

- **A guardian sent with a phone that matches an existing row but a different name.** Is that a typo
  in the new request, a second parent using the family phone, or the same person who changed their
  name? **Recommendation:** match on the phone, link the existing guardian, **do not overwrite the
  stored name**, and return what was matched so the caller can see it happened.
- **A guardian sent with no phone and no email at all.** Nothing identifies them, so every such
  guardian is a new row, and a family that gives no number will accumulate duplicates. That is
  acceptable and should be stated rather than solved.
- **Two guardians in one request sharing a phone.** Must be refused
  (`DUPLICATE_GUARDIAN_IN_REQUEST`), because the second would silently match the first.

**It cannot be left undecided**, because the unique index throws a duplicate-key error as a 500 the
first time a sibling is admitted.

## 2. Roll numbers need a scoped sequence that does not exist

`STUDENT_ROLL_NUMBER` is a defined `NumberSequenceType`, and
`school_year_class_section_active_roll_uniq` scopes the value to `{year, class, section}`. But
`NumberSequenceService.next(schoolId, type, prefixTemplate)` always allocates against
`GLOBAL_SCOPE`. One roll-number sequence per school is not what the index describes.

**Decide:** add a `next(schoolId, type, scopeKey, prefixTemplate)` overload — the repository's
`allocate` already takes the key, so this is a service-level change, not a schema one — **or** make
`rollNo` caller-supplied and let the unique index reject collisions.

**Recommendation:** the overload. A roll number is exactly the kind of thing nobody should have to
pick, and `{year}|{classDocsId}|{sectionNo}` is a natural scope key. **This is a prerequisite for
[#14](#e14)**, so it is worth settling before that phase rather than during it.

## 3. Gate 4 is off, and that is a choice

Every module in `academics` refuses a write against a year that is not running. This plan says
[#14](#e14) must not, so that a child admitted in January can be placed in a June class.

**The cost:** nothing stops a record being created for a year that finished three years ago. The
year has to exist, and the class has to belong to it, but "this year is over" is not asked.

**Decide:** leave it off (the admissions handover works, and back-dated corrections stay possible),
or refuse writes against a year whose end date has passed — which is a *third* rule, neither gate 4
nor nothing, and would need writing.

## 4. `effectiveFrom` when a record is created before the year starts

[#14](#e14) takes `effectiveFrom`. For a child placed in January into a June year, the honest value
is the June date, not today — but nothing in the model says so, and a record whose `effectiveFrom`
is in the future is `ACTIVE` from the moment it is written.

**Decide:** whether `ACTIVE` means "this is the placement" or "this placement is in effect today".
[#21](#e21)'s roster is the endpoint that cares: on 1 March, does a class list a child whose record
starts in June? **Recommendation:** `ACTIVE` means *this is the placement*, the roster filters on
status only, and a `PLANNED` status exists on `AcademicRecordStatus` already for the other reading —
which suggests the model intended the distinction and nothing has used it yet.

## 5. Which side owns `currentAcademicRecordDocsId`

`Student.currentAcademicRecordDocsId` is a denormalised pointer with its own partial-unique index,
and `StudentAcademicRecord` is the record it points at. Two documents, one fact, and
[#14](#e14)/[#16](#e16)/[#17](#e17) all have to keep them in step.

**Decide:** whether every write updates both in one transaction (correct, and makes [#5](#t5) a
single read), or the pointer is dropped and "current" is always a query. **Recommendation:** keep it
and write both — [#5](#t5) and [#4](#e4) are called constantly, and the alternative is a lookup on
every student row.

## 6. What a student's status does to their record, and the reverse

`StudentStatus` and `AcademicRecordStatus` are separate enums that clearly interact — a `WITHDRAWN`
student should not have an `ACTIVE` record — and nothing in the models relates them.

**Decide:** whether [#3](#e3) cascades (moving a student to `WITHDRAWN` closes their open record) or
refuses (`STUDENT_HAS_ACTIVE_RECORD`, close it first). **Recommendation:** cascade, inside the
transaction, because the alternative makes the common case a two-call dance that will be got wrong.

---

# Where the code will live

```text
controllers/student/
    StudentController.java              #1–#6
    GuardianController.java             #7–#10
    StudentGuardianController.java      #11–#13
    StudentAcademicRecordController.java #14–#22

services/student/
    StudentService.java
    GuardianService.java
    StudentAcademicRecordService.java
    utils/    the reads each service makes more than once
    helper/   the rules MongoDB cannot express

repositories/student/{student,guardian,studentacademicrecord}/
dto/student/{student,guardian,studentacademicrecord}/{request,response}/
```

**Four controllers, not three.** The link ([#11](#e11)–[#13](#e13)) writes `students` but is *about*
the guardian relationship, and putting it on `StudentController` takes that file to thirteen
endpoints across two concerns — which is exactly how
[`people`](../people/department/README.md) got to the state that forced a rename.

**One helper per service, and a helper never calls another helper.** The rules that will live there:
the two status graphs, guardian matching ([open item 1](#1-what-counts-as-the-same-guardian)), and
the close-then-open pair that [#17](#e17) needs.

**`GuardianService` is used by `StudentService`** for matching during [#1](#e1). That is a service
calling a service, which this project has not needed before — if it is not wanted, guardian matching
becomes a `utils` under `StudentService` and `GuardianService` keeps only [#7](#e7)–[#10](#t10).

---

# The refusal codes this module introduces

| Code | Status | When |
|---|---|---|
| `STUDENT_NOT_FOUND` | 404 | No student with that id in this school. |
| `ADMISSION_NO_TAKEN` | 409 | Generated number collided. Should be unreachable; see [#1](#e1). |
| `INVALID_STUDENT_TRANSITION` | 409 | [#3](#e3) asked for a move the status graph does not have. |
| `STATUS_REASON_REQUIRED` | 400 | Moving to `WITHDRAWN` or `TRANSFERRED` without saying why. |
| `STUDENT_HAS_ACTIVE_RECORD` | 409 | [#3](#e3), if [open item 6](#6-what-a-students-status-does-to-their-record-and-the-reverse) is decided as *refuse*. |
| `GUARDIAN_NOT_FOUND` | 404 | No guardian with that id in this school. |
| `GUARDIAN_PHONE_TAKEN` | 409 | [#7](#e7)/[#8](#e8) on a number another guardian holds. |
| `GUARDIAN_EMAIL_TAKEN` | 409 | The same for email. |
| `DUPLICATE_GUARDIAN_IN_REQUEST` | 400 | [#1](#e1) sent two guardians with one phone, or one email. |
| `PRIMARY_CONTACT_REQUIRED` | 400 | [#1](#e1) with no primary contact among the guardians, or more than one. **Not reachable through [`crm` #33](../crm/README.md#e33)**, which fills one in. |
| `ADMISSION_APPLICATION_NOT_FOUND` | 404 | [#1](#e1) naming an `admissionApplicationDocsId` that is not a form in this school — **another school's real one included**. |
| `APPLICATION_ALREADY_ENROLLED` | 409 | [#1](#e1) naming an `admissionApplicationDocsId` that already produced a child. The mirror of `crm`'s `ALREADY_ENROLLED` — see [`crm` open item 3](../crm/README.md#3-the-applicationstudent-link). |
| `NOTHING_TO_SEARCH_FOR` | 400 | [#6](#e6) with no phone, admission number or name. |
| `GUARDIAN_ALREADY_LINKED` | 409 | [#11](#e11) for a guardian this child already has. |
| `GUARDIAN_NOT_LINKED` | 404 | [#12](#e12)/[#13](#e13) for one they do not. |
| `LAST_PRIMARY_CONTACT` | 409 | [#12](#e12)/[#13](#e13) would leave a child with no primary contact. |
| `ACADEMIC_RECORD_NOT_FOUND` | 404 | No record with that id in this school. |
| `STUDENT_ALREADY_PLACED` | 409 | [#14](#e14) when the child already has an `ACTIVE` record that year. **This is the index talking.** |
| `CLASS_NOT_IN_YEAR` | 409 | The class does not belong to the year in the path. |
| `SECTION_NOT_IN_CLASS` | 409 | That class has no such `sectionNo`. |
| `ROLL_NO_TAKEN` | 409 | Another active record in that section holds it. |
| `RECORD_NOT_OPEN` | 409 | [#16](#e16)/[#17](#e17) on a record that is not `ACTIVE`. |
| `SECTION_UNCHANGED` | 400 | [#17](#e17) asked to move a child to where they already are. |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | Shared. The year in the path is not this school's. |
| `CONCURRENT_MODIFICATION` | 409 | Shared. Another write changed the document first. |

---

# The status graphs

**These are the specification.** The model README describes the fields; the legal moves exist only
here, and an endpoint that accepted an undefined one would be inventing product.

## `StudentStatus` — [#3](#e3)

```text
                 ┌─────────────► SUSPENDED ──┐
                 │                            │
   ACTIVE ◄──────┴─────────────► INACTIVE ────┘
     │                                │
     │                                │
     ├──────► WITHDRAWN ──────────────┤
     ├──────► TRANSFERRED ────────────┤
     └──────► GRADUATED ──────► ALUMNI
```

| From | May become | Notes |
|---|---|---|
| `ACTIVE` | `INACTIVE`, `SUSPENDED`, `WITHDRAWN`, `TRANSFERRED`, `GRADUATED` | The only status that can go anywhere. |
| `INACTIVE` | `ACTIVE`, `WITHDRAWN`, `TRANSFERRED` | A long absence that came back, or did not. |
| `SUSPENDED` | `ACTIVE`, `WITHDRAWN` | Disciplinary. Ends by returning or leaving. |
| `GRADUATED` | `ALUMNI` | The only move out of it. |
| `WITHDRAWN` · `TRANSFERRED` · `ALUMNI` | **nothing** | Terminal. A child who returns is admitted again. |

`WITHDRAWN` and `TRANSFERRED` require a reason. **Terminal is terminal** — re-admitting a returning
child as a new student is correct, because the gap is a real fact about their schooling.

## `AcademicRecordStatus` — [#14](#e14), [#16](#e16), [#17](#e17)

```text
   PLANNED ──────► ACTIVE ──┬──► COMPLETED
                            ├──► TRANSFERRED
                            └──► WITHDRAWN
   PLANNED ──────────────────────► CANCELLED
```

| From | May become | Notes |
|---|---|---|
| `PLANNED` | `ACTIVE`, `CANCELLED` | A placement decided before the year began. See [open item 4](#4-effectivefrom-when-a-record-is-created-before-the-year-starts). |
| `ACTIVE` | `COMPLETED`, `TRANSFERRED`, `WITHDRAWN` | Closing sets `effectiveUntil`. |
| `COMPLETED` · `TRANSFERRED` · `WITHDRAWN` · `CANCELLED` | **nothing** | Terminal. |

**Only `ACTIVE` and `PLANNED` are affected by the unique index** (`partialFilter: {status: 'ACTIVE'}`
— so strictly, only `ACTIVE`). A child may have any number of closed records for one year; that is
what makes [#17](#e17) legal.

---

# Appendix — what each API takes and returns

Only the fields an endpoint accepts or answers with. Everything else on the model is either
inherited, set by the service, or not writable over HTTP.

<a id="e1"></a>
**[1](#t1) · `POST /students`** — built — *the one with the guardian problem*

| Field | Type | Required | Notes |
|---|---|---|---|
| `fullName` | String | **yes** | |
| `dateOfBirth` | LocalDate | **yes** | Must be in the past. |
| `gender` | [Gender](../../models/common/enums/Gender.java) | **yes** | |
| `admissionDate` | LocalDate | no | Defaults to today. |
| `guardians[].fullName` | String | **yes** | |
| `guardians[].relation` | [GuardianRelation](../../models/common/enums/GuardianRelation.java) | **yes** | |
| `guardians[].phoneNumber` | String | no | **The match key.** See below. |
| `guardians[].emailAddress` | String | no | Also a match key, also unique. |
| `guardians[].primaryContact` · `emergencyContact` · `pickupAuthorized` · `portalAccess` | Boolean | no | Default false. Exactly one primary is required across the list. |
| `nationalityCode` · `preferredLanguage` · `phoneNumber` · `emailAddress` | String | no | The student's own. |
| `admissionApplicationDocsId` | String | no | Set by [`crm` #33](../crm/README.md#e33); a direct caller leaves it out. **Checked when sent** — an id that is not a form in this school is `404`, another school's real one included. |

**`admissionNo` is not here.** It is generated from `NumberSequenceType.STUDENT_ADMISSION`,
template `ADM/{YYYY}/{MM}/`, which gives `ADM/2026/09/000001` — the same
call `StaffService` makes for `employeeNo`. Nobody picks their own admission number, and a
caller-supplied one lets two children collide.

**No `academicYear`, and no class.** The child is created first and placed second, at
[#14](#e14) — the flow this project already recorded. A student with no record yet is a normal,
expected state, not a half-finished one.

**Guardians are matched, not blindly created.** For each entry: if `phoneNumber` matches an existing
guardian in this school, **link that one** and leave its stored name alone; otherwise try the email;
otherwise insert. Two entries sharing a phone is `DUPLICATE_GUARDIAN_IN_REQUEST`. The response says,
per guardian, whether it was `matched`, because a silent match is how somebody's father quietly
becomes somebody else's. [Open item 1](#1-what-counts-as-the-same-guardian) is settled exactly as it
recommended.

### What the match is on, and what it is not

The comparison is the **stored** number — what is left after spaces, brackets, hyphens and dots come
off — and not the bare digits. So `+91 98765 43210` and `+919876543210` are one person.

**A number given with a country code one time and without it the next is still two rows.**
`+919876543210` and `9876543210` are different strings, and that is exactly what
`school_guardian_phone_uniq` thinks too. Matching more loosely than the index would mean this
endpoint and the database disagreed about who is who, which is a worse problem than the duplicate.

**A guardian with no phone and no email is always a new row.** Nothing identifies them, so a family
that gives neither will slowly collect duplicates. Stated rather than solved badly.

### Exactly one primary contact, and the refusal says which way it is wrong

None means "ring the family" has no answer; two means it has two. `400 PRIMARY_CONTACT_REQUIRED`
either way, and the message names the count.

**[`crm` #33](../crm/README.md#e33) never reaches it.** An admission form is not required to mark a
primary contact — [`crm` #17](../crm/README.md#e17) copies the flag it is given — so #33 fills one
in rather than refusing a family who hold an accepted offer. See that entry.

### What it answers with

The same shape [#5](#e5) answers with, plus `matched` inside each guardian. One object for the write
and the read, so a client has one thing to understand rather than two that are nearly the same.

| Refusal | When |
|---|---|
| `400 VALIDATION_FAILED` | A field missing or over its length. `guardians` empty is this. |
| `400 PRIMARY_CONTACT_REQUIRED` | No primary contact among the guardians, or more than one. |
| `400 DUPLICATE_GUARDIAN_IN_REQUEST` | Two guardians on one form share a phone, or an email. |
| `404 ADMISSION_APPLICATION_NOT_FOUND` | `admissionApplicationDocsId` names no form in this school. **Added 2026-10-07** — before that, `"abc"` was accepted and stored, leaving a child pointing at nothing. Nothing else in the project takes an id on trust; this was the outlier. |
| `409 APPLICATION_ALREADY_ENROLLED` | `admissionApplicationDocsId` names an application that already produced a child. **Both are asked before any guardian is written**, because guardians are saved one at a time and a refusal after that point would leave people in the school with no child attached to them. |
| `409 SCHOOL_NOT_EDITABLE` | Gate 1 or 2. |

**Gates 1 and 2. No gate 4** — a school admits in January for a year starting in June.

<a id="e2"></a>
**[2](#t2) · `PATCH /students/{id}`**

Accepts `fullName`, `dateOfBirth`, `gender`, `nationalityCode`, `preferredLanguage`, `phoneNumber`,
`emailAddress`, `profilePhotoDocumentId`. **Not `admissionNo`, not `status`, not `guardians`, not
`currentAcademicRecordDocsId`** — the first two have their own rules, the third is
[#11](#e11)–[#13](#e13), and the fourth is owned by the record endpoints.

A field sent as `""` clears it where the model allows null; absent means unchanged. `NOTHING_TO_UPDATE`
when the body moves nothing.

<a id="e3"></a>
**[3](#t3) · `POST /students/{id}/status`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | StudentStatus | **yes** | Must be a legal move from the current one. |
| `reason` | String | conditional | **Required** for `WITHDRAWN` and `TRANSFERRED`. |
| `effectiveOn` | LocalDate | no | Defaults to today. |

Cascades to the open academic record — see
[open item 6](#6-what-a-students-status-does-to-their-record-and-the-reverse).

<a id="e4"></a>
**[4](#t4) · `GET /students`** — built — *the roll*

- [`students`](../../models/student/Student.java) — *reads*: by `schoolId`, plus whichever filters were sent

Filters: `search` (name **or** admission number, anywhere, case-insensitive), `status`, `gender`,
`placed`, `fromAdmissions`. Paged, with a sort allowlist — `fullName`, `admissionNo`,
`admissionDate`, `createdAt`. **The allowlist is a security control**, not a convenience: an open
sort field lets a caller order the roll by a date of birth and read the values back out of the
ordering without this endpoint ever returning them.

### The class filters are not here, and that is not an oversight

The plan asked for `academicYear` + `classDocsId` + `sectionNo`. **None of the three is a field on a
student** — they live on `StudentAcademicRecord`, which [#14](#e14) writes and which has no
repository, no endpoint and no documents anywhere yet.

Taking them now would mean a filter that silently matched nothing, which is worse than one
documented as absent: a parameter that looks like it works and returns an empty page sends somebody
looking for the bug in their own code. They arrive with #14, and the plan already says how — page on
`student_academic_records` first and read `students` by id, **because every narrowing filter lives
on the first collection and paging the second gives pages that shrink after filtering**.

**`placed` is what stands in for them**, and it is a real question rather than a consolation: at the
start of a term the thing a school needs is the list of children nobody has put in a section yet.
Asked with `$exists` on `currentAcademicRecordDocsId`, because an absent id is stored as no key at
all and `is(null)` would match nothing.

### A row carries no guardians, but it carries the count

Fifty children with two contacts each is a hundred families' phone numbers and addresses crossing
the wire to draw a list that shows none of them. [#5](#e5) opens one child and has them all.
**The count stays**, because "this child has no contact on file" is worth seeing from a list and the
links are already on the document.

**Two sort keys by default** — `fullName` then `admissionNo` — because two children genuinely share
a name, and a tie with no tiebreaker puts one child on two pages while another appears on none.

**No gates.** A read.

<a id="e5"></a>
**[5](#t5) · `GET /students/{id}`** — built — *one child in full*

- [`students`](../../models/student/Student.java) — *reads*: the child by `_id` **and `schoolId`**
- [`guardians`](../../models/student/Guardian.java) — *reads*: the people the child's links point at, **in one query**

**Resolving the guardians is the whole difference from a row.** The student document stores ids and
flags; a page showing `67aa15d9…` where a mother's name belongs is no use to anybody. One read for
the whole child, not one per contact.

**Two documents come back as one object per guardian.** The person — name, number, address — is a
`Guardian`, shared by every child they belong to. The *role* — father, primary contact, allowed to
collect — is a `GuardianLink` on the student, because the same man is "father, primary, portal" to
one child and only an emergency number for their cousin. Anybody reading a child's page wants both
at once, and asking them to join two shapes in their own code would be handing them this module's
internal split.

**The order is the child's, not the database's.** The first guardian on the form is the one the
family wrote first.

**A link whose person has been deleted is skipped, not drawn blank.** It should not be possible —
nothing deletes a guardian today — but a row of empty fields where a mother's name belongs is the
kind of thing somebody reports as a bug in the screen.

**No class, no section, no roll number.** Those are on the academic record. `placed` says whether
there is one rather than pretending there might be.

**An id from another school is `404 STUDENT_NOT_FOUND`**, not a 403 — a 403 would confirm the child
exists, and behind it are a date of birth and a family's phone numbers. Scoped by school **in the
query**, never checked after.

**No gates.** A read.

<a id="e6"></a>
**[6](#t6) · `GET /students/search?phone=&admissionNo=&name=`** — built — *is this child already here*

- [`guardians`](../../models/student/Guardian.java) — *reads*: whose number this is, **first**
- [`students`](../../models/student/Student.java) — *reads*: their own number, their guardians' children, the admission number, or the name — one query, OR-ed

**The call made before every admission**, and the reason [#1](#e1) does not refuse duplicates
itself: refusing there would mean deciding that two children sharing a surname and a phone number
are one child, *which siblings are not*. The judgement belongs to the person at the desk. The
equivalent of [`crm` #15](../crm/README.md#e15), and it exists for the same reason — the
alternative is a second record for a child already on the roll, and nothing downstream can tell
afterwards that the two are one person.

### It searches the guardians, and that is the point

**A seven year old has no phone.** The number a school holds is their mother's, so a check against
the child's own contact details would miss nearly every child it exists to find. The number is
looked up in `guardians` first — including `alternatePhoneNumber`, which is the one a family gives
as "my husband's phone" — and the ids go into the student query. **Two reads, not one per person.**

### The phone is matched on its DIGITS

| Query | Finds a stored `+919876543210` |
|---|---|
| `9876543210` · `+91 98765 43210` · `098765-43210` · `(98765) 43210` | yes |
| `543210` | **no** |

**Ten digits or more is compared on the last ten**, so a country code or a trunk 0 on either side
stops mattering. **Fewer than ten must match the whole number** — by its tail, `543210` matches
every number ending in those six digits, and a false *"we already have this child"* is the worst
answer this endpoint can give: the school merges two children, or skips admitting one who was never
here.

### The admission number is whole; the name is not

`admissionNo` is anchored at both ends and case-insensitive — a question about identity, so
`ADM/2026/09/0001` must not match `ADM/2026/09/00010`. **The name matches anywhere**, because a name is
not an identifier: somebody typing "aarav" wants every Aarav on the roll to look at, and anchoring
it would answer "no" to a question that was really "show me who it might be". Both needles are
**quoted**, so a caller cannot send a regular expression.

### A list, not a page

One child, or two for a name a family shares, or none. **Thirty means the question was wrong** —
somebody searched for "a" — and that is worth seeing in one screen rather than paging through.
Capped at 25, newest first. The rows are [#4](#e4)'s.

`400 NOTHING_TO_SEARCH_FOR` when none of the three was sent — that would be the whole roll, which
is #4's job, and a caller who sent a blank phone probably believes they sent a real one.

**No gates, and this one least of all.** A school that cannot be edited still needs to know whether
it already has this child, or the desk duplicates them.

<a id="e7"></a>
**[7](#t7) · `POST /guardians`**

`fullName` required; `phoneNumber`, `alternatePhoneNumber`, `emailAddress`, `address`, `occupation`,
`preferredLanguage` optional. Refuses `GUARDIAN_PHONE_TAKEN` / `GUARDIAN_EMAIL_TAKEN` rather than
silently matching — **unlike [#1](#e1)**, because a caller asking for a guardian directly is
asserting a new person, while a caller admitting a child is describing a family.

<a id="e8"></a>
**[8](#t8) · `PATCH /guardians/{id}`**

**This changes the contact for every child linked to them**, which is the point of the shared row and
is worth saying in the response: it returns the count of students affected.

<a id="e9"></a>
**[9](#t9) · `GET /guardians?phone=&email=&name=`**

**Find the existing one before making a second.** The endpoint that makes [#11](#e11) usable, and the
one a careful [#1](#e1) caller hits first.

<a id="e11"></a>
**[11](#t11) · `POST /students/{id}/guardians`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `guardianDocsId` | String | **yes** | Must exist in this school. Use [#9](#e9) to find it. |
| `relation` | GuardianRelation | **yes** | |
| `primaryContact` · `emergencyContact` · `pickupAuthorized` · `portalAccess` | Boolean | no | Default false. |

`$push` onto `Student.guardians`. `GUARDIAN_ALREADY_LINKED` when the id is already in the array.
**Setting `primaryContact: true` clears it on the others** in the same update — two primaries is not
a state worth being able to reach.

<a id="e12"></a>
**[12](#t12) · `PATCH /students/{id}/guardians/{guardianDocsId}`**

Changes the four flags and `relation`. A positional array update, the same shape
[`timetable` #4](../academics/timetable/README.md) uses — **and the same hazard**: the entry is
matched by `guardianDocsId`, which is a plain string here rather than an `ObjectId`, so it does not
carry that endpoint's array-filter type problem.

`LAST_PRIMARY_CONTACT` when the change would leave the child with no primary.

<a id="e13"></a>
**[13](#t13) · `DELETE /students/{id}/guardians/{guardianDocsId}`**

`$pull` from the array. **The guardian row is untouched** — they may have other children here, and
even if not, a contact the school once had is not a thing to delete on an unlink. 204.

<a id="e14"></a>
**[14](#t14) · `POST /academic-years/{year}/student-records`** — *the one everything else waits for*

| Field | Type | Required | Notes |
|---|---|---|---|
| `studentDocsId` | String | **yes** | Must be this school's, and not `WITHDRAWN`/`TRANSFERRED`/`ALUMNI`. |
| `classDocsId` | String | **yes** | Must belong to `{year}` — `CLASS_NOT_IN_YEAR`. |
| `sectionNo` | String | **yes** | Must exist on that class — `SECTION_NOT_IN_CLASS`. |
| `rollNo` | String | no | Generated when absent — **blocked on [open item 2](#2-roll-numbers-need-a-scoped-sequence-that-does-not-exist)**. |
| `effectiveFrom` | LocalDate | no | Defaults to today. See [open item 4](#4-effectivefrom-when-a-record-is-created-before-the-year-starts). |
| `status` | AcademicRecordStatus | no | `ACTIVE` or `PLANNED`. Defaults to `ACTIVE`. |

In one transaction: insert the record, then set `Student.currentAcademicRecordDocsId`. Refuses
`STUDENT_ALREADY_PLACED` when an `ACTIVE` record exists for that student and year — **and the unique
index is the real enforcement**; the check is there to make the refusal a 409 with a sentence rather
than a duplicate-key 500.

**`academicYear` need not be running.** See [gates](#which-gates-every-endpoint-runs).

<a id="e16"></a>
**[16](#t16) · `POST /academic-years/{year}/student-records/{id}/close`**

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | AcademicRecordStatus | **yes** | `COMPLETED`, `TRANSFERRED` or `WITHDRAWN`. |
| `effectiveUntil` | LocalDate | no | Defaults to today. Must be ≥ `effectiveFrom`. |

Clears `Student.currentAcademicRecordDocsId` when it pointed here. `RECORD_NOT_OPEN` on anything not
`ACTIVE`.

<a id="e17"></a>
**[17](#t17) · `POST /academic-years/{year}/student-records/{id}/move`** — *why this is not a `PATCH`*

| Field | Type | Required | Notes |
|---|---|---|---|
| `classDocsId` | String | no | Defaults to the current one — a section move within a class. |
| `sectionNo` | String | **yes** | |
| `rollNo` | String | no | Generated for the new section when absent. |
| `effectiveFrom` | LocalDate | no | Defaults to today. Becomes the old record's `effectiveUntil`. |

In one transaction: close this record as `COMPLETED`, insert a new `ACTIVE` one pointing back through
`previousAcademicRecordDocsId`, and repoint the student.

**A `PATCH` of `classDocsId` cannot do this.** Two `ACTIVE` records for one student and year violate
`school_year_student_active_academic_record_uniq`, so the close and the open must be one transaction
— and more importantly, **editing the class in place erases where the child sat for the first half of
the year**, which is exactly what attendance and marks for that half are attached to.

<a id="e20"></a>
**[20](#t20) · `GET /students/{id}/academic-records`**

Every year, newest first, terminal records included. Served by
`school_student_academic_record_history_idx`, which is `{schoolId, studentDocsId, academicYear: -1,
effectiveFrom: -1}` — the sort is the index order, deliberately.

<a id="e21"></a>
**[21](#t21) · `GET /academic-years/{year}/classes/{classDocsId}/sections/{sectionNo}/roster`**

**The endpoint the rest of the product is waiting for.** Attendance marks it, a mark sheet lists it,
`timetable` [#8](../academics/timetable/README.md) sits beside it.

Returns one row per `ACTIVE` record: `studentDocsId`, `fullName`, `admissionNo`, `rollNo`, student
`status`. Sorted by `rollNo`, then name for the ones without. Served by
`school_year_class_section_roster_idx`.

**Two collections, one call, and it must not be an N+1.** The records carry no name; the students
carry no section. One `$lookup`, or one query for the records and one `findAllById` for the students
— never a read per row. The same trap [`people` #15](../people/department/README.md#e15) names.

<a id="e22"></a>
**[22](#t22) · `GET /academic-years/{year}/student-strength`**

One row per class and section: `enrolled`, and `capacity` when the class declares one. One grouped
aggregation for the whole year, not a query per section.

---

*Endpoints without an appendix entry — [#5](#t5), [#10](#t10), [#15](#t15), [#18](#t18),
[#19](#t19) — take what their tables and the status graphs above already say. An appendix row is
written when the endpoint is, so that it describes what was built rather than what was imagined.*
