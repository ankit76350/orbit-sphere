# controllers/student — API plan

**Nine of twenty-two are built.** [#1](#e1), [#4](#e4) and [#5](#e5) went in on
2026-10-06 — what [`controllers/README.md`](../README.md) calls **phase 5: "the minimum, not the
module"**, built to unblock [`crm` #33](../crm/README.md#e33), the handover where an applicant
becomes a child on a register. That endpoint went in the same day.

**[#2](#e2) and [#7](#e7) followed on 2026-10-07.** #2 is a front desk mishearing a name — a roll
nobody can correct is a roll that gets worse every week. #7 is the other door onto `guardians`: a
guardian who turns up without a child, [#9](#e9) lists them, [#10](#e10) opens one with every child
they are attached to, and [#8](#e8) corrects one — **for all of those children at once**, which is
the point of the shared row. Everything else here is still a plan.

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
| <a id="t2"></a>2 — **built** | [`PATCH /students/{id}`](#e2) | Correct name, date of birth, contact. **Not the photo** — see the entry. | [`students`](../../models/student/Student.java) |
| <a id="t3"></a>3 | [`POST /students/{id}/status`](#e3) | Move through the status graph, with a reason. | `students` |

## 2. The student — reads · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t4"></a>4 — **built** | [`GET /students`](#e4) | **The roll.** Filtered by status, gender, placed, from-admissions. **Not by class** — see the entry. | [`students`](../../models/student/Student.java) |
| <a id="t5"></a>5 — **built** | [`GET /students/{id}`](#e5) | One child in full, with guardians resolved. **No academic record** — #14 is not built. | [`students`](../../models/student/Student.java), [`guardians`](../../models/student/Guardian.java) |
| <a id="t6"></a>~~6~~ — **removed 2026-10-08** | ~~`GET /students/search`~~ | Asked "is this child already here?" before an admission. **Dropped**: [#1](#e1) refuses a guardian whose number the school already holds and names them, so the duplicate question is answered where it matters rather than as a separate step somebody has to remember to take. **The number is retired, not reused.** | — |

## 3. The guardian — writes · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t7"></a>7 — **built** | [`POST /guardians`](#e7) | Add a guardian who is not being created with a child. | [`guardians`](../../models/student/Guardian.java) |
| <a id="t8"></a>8 — **built** | [`PATCH /guardians/{id}`](#e8) | Correct the guardian. **Changes them for every child they are linked to**, and says how many. | [`guardians`](../../models/student/Guardian.java), [`students`](../../models/student/Student.java) |

## 4. The guardian — reads · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t9"></a>9 — **built** | [`GET /guardians?phone=&email=&name=`](#e9) | **The list, and the check made before a second one.** Every filter optional. | [`guardians`](../../models/student/Guardian.java) |
| <a id="t10"></a>10 — **built** | [`GET /guardians/{id}`](#e10) | One guardian and **every child they are attached to**, with what they are to each. | [`guardians`](../../models/student/Guardian.java), [`students`](../../models/student/Student.java) |

## 5. The link between them · [Build order ↗](../README.md#the-order)

| # | Method and endpoint | What this API is for | Collections |
|---|---|---|---|
| <a id="t11"></a>11 — **built** | [`POST /students/{id}/guardians`](#e11) | Attach a guardian to a child — **or create one and attach them in the same call**. | [`students`](../../models/student/Student.java), [`guardians`](../../models/student/Guardian.java) |
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

**Settled 2026-10-06 as "match on the phone", and reversed on 2026-10-07.** The recommendation
below was built and then used, and using it was the problem: typing one guardian's name on a number
the school already held returned a *different* person, silently. [#1](#e1) now **refuses** a taken
number and links only when the caller sends `guardianDocsId`.

**What survives the reversal is everything about identity.** A phone number still identifies one
person per school, the comparison is still on the digits, two entries sharing a phone in one request
are still refused, and a guardian with neither a phone nor an email is still always a new row. The
only thing that changed is **who decides** that two records are the same person: the endpoint used
to, and now the caller does.

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
| `STUDENT_NAME_REQUIRED` | 400 | [#2](#e2) sent `fullName` as `""` or spaces. A child's name is the one thing on the document they are found by. |
| `NOTHING_TO_UPDATE` | 400 | [#2](#e2) with a body that moves nothing. |
| `STUDENT_HAS_ACTIVE_RECORD` | 409 | [#3](#e3), if [open item 6](#6-what-a-students-status-does-to-their-record-and-the-reverse) is decided as *refuse*. |
| `GUARDIAN_NOT_FOUND` | 404 | No guardian with that id in this school. |
| `GUARDIAN_PHONE_TAKEN` | 409 | [#7](#e7)/[#8](#e8) on a number another guardian holds. |
| `GUARDIAN_EMAIL_TAKEN` | 409 | The same for email. |
| `GUARDIAN_NAME_REQUIRED` | 400 | [#8](#e8) sent `fullName` as `""` or spaces. |
| `DUPLICATE_GUARDIAN_IN_REQUEST` | 400 | [#1](#e1) sent two guardians with one phone, or one email. |
| `PRIMARY_CONTACT_REQUIRED` | 400 | [#1](#e1) with no primary contact among the guardians, or more than one. **Not reachable through [`crm` #33](../crm/README.md#e33)**, which fills one in. |
| `ADMISSION_APPLICATION_NOT_FOUND` | 404 | [#1](#e1) naming an `admissionApplicationDocsId` that is not a form in this school — **another school's real one included**. |
| `APPLICATION_ALREADY_ENROLLED` | 409 | [#1](#e1) naming an `admissionApplicationDocsId` that already produced a child. The mirror of `crm`'s `ALREADY_ENROLLED` — see [`crm` open item 3](../crm/README.md#3-the-applicationstudent-link). |
| `GUARDIAN_ALREADY_LINKED` | 409 | [#11](#e11) for a guardian this child already has. |
| `TOO_MANY_GUARDIANS` | 409 | [#11](#e11) on a child who already has ten. |
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

### A taken number is REFUSED, not quietly linked — changed 2026-10-07

**This is the opposite of what #1 did for its first day**, and the reason is a real session:

> Admitting "Allo" created the guardian "Hero" on `07635046798`. Admitting a second child and typing
> **"ANKIT KUMAR"** on that same number returned a child whose father was **"Hero"** — a different
> person, with no warning. The caller had typed one name and been given another.

That is the **right** answer for a sibling and an alarming one for everybody else, and **this
endpoint cannot tell which it is looking at**. So the caller says:

| | What happens |
|---|---|
| `guardianDocsId` **sent** | That person is linked as they are. The typed name, number and address are **ignored** — the stored row wins, and correcting it is [#8](#e8), which would change them for every child they belong to. |
| **left out** | The phone and the email must be **free**. A taken one is `409 GUARDIAN_PHONE_TAKEN` / `GUARDIAN_EMAIL_TAKEN`, **naming the holder and quoting their id** so the caller can send it back if it really is them. |

**So a sibling's father is attached deliberately**: look him up with [#9](#e9), send his id. The
`matched` flag on the response now means *"you asked for this one"* rather than *"we guessed"*.

**[`crm` #33](../crm/README.md#e33) is the one exception and still links by number.** Those
guardians were typed by the family months ago; refusing at the handover would strand a family who
hold an accepted offer. The decision is an **argument** to `createStudent` rather than a field on
the request, because it is not the family's choice — it is a fact about which door the request came
through.

**And it links on the same loose comparison, since 2026-10-07.** It used to match the stored string,
so an application carrying `07635046798` against a school holding `+917635046798` wrote a *second
row for one man* — the duplicate this module exists to prevent, arriving through the one door that
was not looking for it. There is now **one notion of "the same number"** everywhere:

| Endpoint | On a number that is already somebody's |
|---|---|
| [#1](#e1) admit · [#7](#e7) add · [#8](#e8) correct · [#11](#e11) attach | **refuse**, naming the holder and quoting their id |
| [`crm` #33](../crm/README.md#e33) enrol | **link** them |
| [#9](#e9) list | **find** them — and it is the only one that also matches the *alternate* number, because that is how a second parent is found |

### The refusal compares digits

The loose rule [#7](#e7), [#8](#e8) and [#9](#e9) use, so **the check a desk makes before admitting
and the refusal they get here agree about one number**. `+91 97000 08080` is refused against a
stored `9700008080`. Verified 2026-10-07.

**Two entries sharing a phone in one request is still `DUPLICATE_GUARDIAN_IN_REQUEST`**, checked
before anything is written.

**A guardian with no phone and no email is always a new row.** Nothing identifies them, so a family
that gives neither will slowly collect duplicates. Stated rather than solved badly.

[Open item 1](#1-what-counts-as-the-same-guardian) was settled as "match on the phone" and is now
settled the other way for this endpoint; the index it rests on has not changed.

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
**[2](#t2) · `PATCH /students/{id}`** — built — *the front desk mishears things*

- [`students`](../../models/student/Student.java) — *reads*: the child by `_id` **and `schoolId`**; *updates*: whichever of the eight fields were sent
- [`guardians`](../../models/student/Guardian.java) — *reads*: only to draw the answer. **This endpoint never writes one.**

Accepts `fullName`, `dateOfBirth`, `gender`, `nationalityCode`, `preferredLanguage`, `phoneNumber`,
`emailAddress`, and a required `version`.

**The photo came off on 2026-10-07.** `profilePhotoDocumentId` names a `DocumentRecord`, and a file
is *uploaded* rather than typed — an id box for it is the tail end of an endpoint that does not
exist yet. **Nothing writes that field today**: [#1](#e1) does not accept one either, so it sits on
the model waiting for [`documents`](../../models/documents), which owns the upload.
[`people` #2](../people/staff/README.md) still takes a `profileImageDocsId`, so the two modules
disagree — deliberately, and this is the newer call.

### A correction is not an event, which is why it is a PATCH

A name spelled as it sounded, a date of birth with the year transposed, a number taken down wrong
— none of those is something that *happened to the child*. They are the school admitting it wrote
something down wrong. [#3](#e3) is where things that happen get verbs.

**`""` clears, absent leaves alone.** That is the whole reason this is not a `PUT`: a form that
sent every field would wipe whatever it did not know about.

### What it will not touch, and why each

| Not here | Because |
|---|---|
| `admissionNo` | Generated, and printed on certificates and receipts. A rename leaves a paper trail pointing at nobody. |
| `status` | Seven values with real preconditions on each move — [#3](#e3). A `PATCH` that set it would be seven endpoints wearing one name, and would let somebody write `GRADUATED` onto a child who left in March. |
| `guardians` | Their own documents, **shared between siblings**. A list replaced whole here would quietly unlink a father from a child whose form did not mention him. [#11](#e11)–[#13](#e13). |
| `currentAcademicRecordDocsId` | Owned by [#14](#e14) and [#17](#e17). |
| `admissionApplicationDocsId` | Written once by [`crm` #33](../crm/README.md#e33) — the link back to how this child arrived. |

**Sending one of them is not refused; it is ignored.** None is on the request record, so nothing
binds — measured 2026-10-07 by sending `admissionNo`, `status` and an empty `guardians` together
and reading all three back unchanged.

**`admissionDate` is not in the list either**, and that is worth stating rather than assuming: a
date typed wrongly when a school entered its existing roll cannot be corrected here today.

### Two fields clear, two correct, two are a limitation

| Field | `""` does |
|---|---|
| `phoneNumber`, `emailAddress` | **clears it** |
| `fullName` | **`400 STUDENT_NAME_REQUIRED`** — the model requires one, and it is the only thing on this document a child is found by. Clearing it would leave a child on the roll nobody can search for. |
| `dateOfBirth`, `gender` | nothing to send — there is no `""` for a date or an enum, and the model requires both anyway |
| `nationalityCode`, `preferredLanguage` | **`400`, and that is a limitation** |

The last pair can be **corrected but not removed**. They are enums, so `""` is not a value they
take — measured, it is `400 INVALID_VALUE` with a message naming the field — and `null` already
means "leave it alone", with no way to tell an absent field from one deliberately emptied.
Expressing both would need `JsonNullable`, which this project does not use.
[`people` #2](../people/staff/README.md) records the same limitation for the same reason.

### The child's phone is not normalised, and a guardian's is

Nothing matches on a student's own number and no index constrains it. A **guardian's** is the match
key — the stored shape is what makes two spellings one person — which is why [#1](#e1) strips one
and this does not.

### `version` is required

So `409 CONCURRENT_MODIFICATION` is reachable, which is the point: two clerks correcting one child's
record is exactly what it exists for. The guard is a plain comparison with no null check — the field
is `@NotNull` and every body in this project is `@Valid`.

| Refusal | When |
|---|---|
| `404 STUDENT_NOT_FOUND` | No child of that id **in this school**. |
| `400 NOTHING_TO_UPDATE` | The body moves nothing. **Asked before anything is read**, because it costs no round trip — and a `PATCH` that changes nothing and answers 200 lets a client with a broken form look healthy. |
| `400 STUDENT_NAME_REQUIRED` | `fullName` sent as `""` or spaces. |
| `400 VALIDATION_FAILED` | A field over its length, a future `dateOfBirth`, or no `version`. |
| `400 INVALID_VALUE` | `""` or an unknown value for one of the two enums. |
| `409 CONCURRENT_MODIFICATION` | Somebody wrote to this child first. |
| `409 SCHOOL_NOT_EDITABLE` | Gate 1 or 2. |

**Gates 1 and 2. No gate 4** — correcting a name has nothing to do with which year is running.

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

<a id="e7"></a>
**[7](#t7) · `POST /guardians`** — built — *the guardian who turns up on their own*

- [`guardians`](../../models/student/Guardian.java) — *reads*: is this number or address already somebody's; *insert*: the guardian

`fullName` required; `phoneNumber`, `alternatePhoneNumber`, `emailAddress`, `address`,
`occupation`, `preferredLanguage` optional.

### No relation and no flags

"Father", "primary contact", "may collect" are facts about a person **and a child together** — the
same man is "father, primary, portal" to one child and only an emergency number for their cousin.
They live on the link, which is [#11](#e11). **A guardian created here belongs to nobody**, and that
is a normal state rather than a half-finished one: an emergency number on file before the child
arrives is exactly what this is for.

### It REFUSES a taken number. [#1](#e1) MATCHES one. That is the design

The database says a phone number identifies one person per school — `school_guardian_phone_uniq` —
and both endpoints obey it. What they do when they meet one is **opposite**, because the caller is
saying something different:

| | The caller is saying | A taken number |
|---|---|---|
| [**#1**](#e1) | *"this child's father is on 98765 43210"* — describing a family | **matched and linked** |
| **#7** | *"add this guardian"* — asserting a new person | **`409 GUARDIAN_PHONE_TAKEN`** |

Refusing in #1 would make a sibling's admission fail for a reason the front desk cannot act on.
Matching in #7 would look like a successful create and leave somebody believing a guardian exists
that does not.

**The refusal names the person who holds the number**, so the caller can go and look: either they
have the wrong number, or they meant to correct that guardian rather than add one.

**Measured 2026-10-07**, because a contradiction here would be invisible: created a guardian on
`+91 98765 00011` through #7, then admitted a child naming `+91 (98765) 00011` through #1 — the
same guardian id came back with `matched: true` and **the stored name kept**, while #7 on that same
number answered `409`. One index, two right answers.

### The normalising lives in one place, and it had to

`StudentHelper.normalisePhone` — the module's helper file, created with this endpoint. #1 looking a
person up and #7 refusing a duplicate write into **one** unique index, so the two have to agree
character for character about the stored form of a number. Two copies of that rule drifting apart
would mean #1 quietly creating the person #7 says already exists.

**A number given with a country code one time and without it the next is still two rows** —
`+919876543210` and `09876543210` are different strings, and that is what the index thinks too.

### What is allowed that looks like it should not be

- **A guardian with no phone and no email**, twice over. Nothing identifies them, so a family that
  gives no details will slowly collect duplicates. Stated rather than solved badly.
- **Two people on one family landline.** `alternatePhoneNumber` is not unique and not checked —
  making it so would make a mother and a father impossible to enter.
- **The same number in another school.** Uniqueness is per school; the two rows are two different
  people as far as the product is concerned.

A number made only of punctuation — `"( ) --"` — stores **nothing** rather than `""`. A blank number
behind a request that looked filled in is worse than no number, and it would take the one slot the
index allows for "no phone".

| Refusal | When |
|---|---|
| `400 VALIDATION_FAILED` | `fullName` missing, or a field over its length. |
| `409 GUARDIAN_PHONE_TAKEN` | Somebody in this school already has that number. **The message names them.** |
| `409 GUARDIAN_EMAIL_TAKEN` | Somebody already has that address. |
| `409 SCHOOL_NOT_EDITABLE` | Gate 1 or 2. |

**Gates 1 and 2. No gate 4** — a contact has nothing to do with which year is running.

**The checks are the enforcement, not a nicety in front of the indexes.** Both are declared on the
model and built on demand, and the dev database has only `_id_` on `guardians` — measured
2026-10-06. Where they *are* built, this turns a duplicate-key 500 into a 409 that says which field
and who holds it.

<a id="e8"></a>
**[8](#t8) · `PATCH /guardians/{id}`** — built — *one row, every child*

- [`guardians`](../../models/student/Guardian.java) — *reads*: the guardian by `_id` **and `schoolId`**, then whether the new number or address is somebody **else's**; *updates*: whichever of the seven fields were sent
- [`students`](../../models/student/Student.java) — *reads*: **a count**, not the documents — how many children this correction reached

Accepts all seven fields on `Guardian` — `fullName`, `phoneNumber`, `alternatePhoneNumber`,
`emailAddress`, `address`, `occupation`, `preferredLanguage` — and a required `version`.

### This changes the person for EVERY child linked to them

That is the point of the shared row, and the one thing to know before using it. A guardian is one
real person per school, so correcting a mother's number corrects it on **all four of her children at
once**. There is no way to change it for one of them, and there should not be: the alternative is
four rows for one woman and no way to tell which is current.

**So the answer carries `childrenAffected`** — a caller who did not expect that finds out from the
response rather than from a parent. Measured 2026-10-07: correcting a guardian shared by three
children answered `childrenAffected: 3`, and the new name was visible through both [#10](#e10) and
[#5](#e5) immediately.

**A count, not the children.** Reading three whole students to print the number 3 would carry their
dates of birth across to report a digit. [#10](#e10) is where they are listed.

### Never the relation or the flags

`FATHER`, "primary contact", "may collect", "portal" are facts about a person **and a child
together** — the same man is all four to one child and only an emergency number for their cousin.
They live on `GuardianLink`, embedded in the student, and #12 changes them one child at a time.

**Putting them here would mean changing somebody's relation to all their children at once**, which
is not a thing that happens.

### The uniqueness checks skip the guardian being corrected

Without that, **editing somebody's name would refuse on their own phone number** — the most ordinary
use of this endpoint there is. Verified: re-sending a guardian their own number is a `200`, and so is
sending it spelled differently (`9700005151` → `+91 97000 05151`), because the loose check finds only
themselves and skips them.

**The phone check is loose, the same as [#7](#e7)'s**, and for the same reason: it only *refuses*, so
being stricter than the unique index saves a duplicate human and costs one message. [#9](#e9)
compares digits too, so the check a caller makes first and the refusal they get here agree.

**The alternate number is normalised but not checked.** It is deliberately shared — a family landline
— so two guardians holding it is the ordinary case rather than a mistake.

### `""` clears, absent leaves alone

Except `fullName`, where blank is `400 GUARDIAN_NAME_REQUIRED`: a guardian with no name is a row
nobody can find. **Clearing the phone is allowed** — a guardian nothing identifies is a state #7 can
create too.

`preferredLanguage` can be **corrected but not removed**: it is an enum, so `""` is
`400 INVALID_VALUE`, and `null` already means "leave it alone". The same limitation [#2](#e2) and
[`people` #2](../people/staff/README.md) record.

| Refusal | When |
|---|---|
| `404 GUARDIAN_NOT_FOUND` | No guardian of that id **in this school**. |
| `400 NOTHING_TO_UPDATE` | The body moves nothing. **Asked before anything is read.** |
| `400 GUARDIAN_NAME_REQUIRED` | `fullName` sent as `""` or spaces. |
| `400 VALIDATION_FAILED` | A field over its length, or no `version`. |
| `400 INVALID_VALUE` | `""` or an unknown value for `preferredLanguage`. |
| `409 GUARDIAN_PHONE_TAKEN` | That number is somebody **else's**. |
| `409 GUARDIAN_EMAIL_TAKEN` | That address is somebody else's. |
| `409 CONCURRENT_MODIFICATION` | Somebody wrote to this guardian first. **It matters more here than on most writes** — the row is shared, so two offices correcting one mother's number are genuinely likely to collide. |

**Gates 1 and 2. No gate 4.**

<a id="e9"></a>
**[9](#t9) · `GET /guardians?phone=&email=&name=`** — built — *the list, and the check before a second one*

- [`guardians`](../../models/student/Guardian.java) — *reads*: by `schoolId`, plus whichever of the three filters were sent

**Find the existing one before making a second.** The endpoint that makes [#11](#e11) usable, and
the one a careful [#1](#e1) caller hits first.

### It is the list as well as the search, and it had to be

**Sending no filters returns everybody**, paged, in name order. That is a deliberate difference
from [`crm` #15](../crm/README.md#e15), which refuses an empty query with
`NOTHING_TO_SEARCH_FOR`.

It can, because a list endpoint sits beside it — #13, the worklist. Students had the same pairing
until 2026-10-08: [~~#6~~](#t6) refused an empty query and [#4](#e4) was the list beside it. **Guardians have no such endpoint and the plan has no tenth read to put one in**, so
refusing here would mean a school could never see its own contacts at all.

### The filters narrow; #6's widen

They are **AND**-ed — the caller is filtering a list rather than asking *"is this one person
here"*.

| Filter | Matched |
|---|---|
| `phone` | **on its digits**, across `phoneNumber` *and* `alternatePhoneNumber` |
| `email` | **whole** and case-insensitively — a question about identity |
| `name` | **anywhere** — a name is not an identifier |

### The phone filter must find what [#7](#e7) refuses on, and once it did not

**This was a bug, measured 2026-10-07 and fixed the same day.** #9 has always compared digits; #7
compared the *stored string*. So for one number — `098765 11111` against a stored `+919876511111` —
**#9 answered "found 1" and #7 answered `201 Created`**, which is the check and the refusal telling
a caller two different things about one person, and a duplicate human in the database.

**#7 now compares digits too.** Being stricter than the unique index is safe there, because #7 only
*refuses*: a number that is probably already somebody's costs the caller one message and saves a
duplicate. [#1](#e1) stays exact, because it **matches and links** — and a loose match there risks
attaching the wrong man to a child.

**The one deliberate asymmetry that remains:** #9 finds a guardian by their *alternate* number and
#7 does not refuse on one. That number is a shared family landline, so refusing would make a mother
impossible to add once the father listed it as his second. Verified: #9 finds the landline, #7
still lets the mother in.

### Sortable by `fullName`, `createdAt`, `updatedAt`, and nothing else

**The allowlist is a security control.** An open sort field lets a caller order a school's families
by their addresses and read the values back out of the ordering without the endpoint ever returning
them.

**Three sort keys by default** — `fullName`, then `createdAt`, then the id. Two guardians genuinely
share a name, and a guardian has no number of their own to break the tie with, so the id settles
what `createdAt` cannot.

| Refusal | When |
|---|---|
| `400 INVALID_SORT_FIELD` | A sort field outside the allowlist. The refusal lists what is allowed. |
| `400 INVALID_PAGE_SIZE` | A page bigger than the cap. |

**No gates.** A read — and a suspended school still has to ring a parent.

### There is no "attached to nobody" filter, and it is still the one worth having

[#7](#e7) creates guardians attached to no child, so *"which of these has nobody"* is the obvious
follow-up. It is not here because the answer lives in `students` — `guardians.guardianDocsId` — and
this endpoint reads one collection.

**[#10](#e10) crossing that boundary did not solve it**, and it is worth saying why rather than
leaving it looking forgotten: #10 reads *one* guardian's children through an index seek, where a
filter here would need the opposite — every guardian id that appears anywhere in `students`,
aggregated, to subtract from a page. That is a different query, and it belongs with whatever first
needs it rather than being guessed at now.

<a id="e10"></a>
**[10](#t10) · `GET /guardians/{id}`** — built — *one guardian, and what they are to each child*

- [`guardians`](../../models/student/Guardian.java) — *reads*: the guardian by `_id` **and `schoolId`**
- [`students`](../../models/student/Student.java) — *reads*: every child whose `guardians.guardianDocsId` is this one, **in one query**

### The flags are why this is more than a read of one document

`relation`, `primaryContact`, `emergencyContact`, `pickupAuthorized` and `portalAccess` live on
`GuardianLink`, which is **embedded in the student** — not on the guardian. Because the same man is
*"father, primary contact, may collect, portal"* to one child and **only an emergency number** for
their cousin.

So they come back **per child**. A guardian's page that printed one set of them would be printing a
fiction. Measured 2026-10-07 on one guardian shared by two children:

| Child | relation | flags |
|---|---|---|
| Child One | `FATHER` | primary, pickup, portal |
| Cousin Two | `UNCLE` | emergency |

One document, two different answers to *"what are you to this child"*.

### Two queries, and the second is one index seek

`school_guardian_students_idx` is keyed `{schoolId, guardians.guardianDocsId}` and exists for
exactly this — **not one read per child**.

The link is then found **on the child** rather than assumed. A student the query returned whose
array does not actually name this guardian would mean the index and the document disagree; that row
is skipped rather than drawn with empty flags.

### An empty `children` list is a real answer here

On [#7](#e7) and [#9](#e9) the field is **absent altogether** — neither reads `students`, and an
empty list there would say *"this guardian has no children"* where the truth is *"nobody asked"*.
**Present and empty** on #10 means a guardian #7 created and #11 has never attached: the normal
state of an emergency number put on file before the child arrives.

### The children are read second, so a guardian with none is still a 200

The only `404` is the guardian themselves.

**An id from another school is that same `404`, not a `403`** — a 403 would confirm they exist, and
behind it are a family's phone number and home address. **Scoped in the query**, which this endpoint
got wrong on the first attempt: it read `findById(...)` and then filtered the school out in Java,
which is the same leak with extra steps — the read has already happened, and the `if` that undoes
it is one edit away from being dropped. `findByIdAndSchoolId` was added for it.

| Refusal | When |
|---|---|
| `404 GUARDIAN_NOT_FOUND` | No guardian of that id **in this school**. |

**No gates.** A read.

<a id="e11"></a>
**[11](#t11) · `POST /students/{id}/guardians`** — built — *create and attach, or attach what is there*

- [`guardians`](../../models/student/Guardian.java) — *reads*: the person named, or whether the typed number is already somebody's; *insert*: a new guardian, when one is being created
- [`students`](../../models/student/Student.java) — *updates*: `guardians[]` — the new link pushed on, and the old primary demoted when one is being replaced

| Field | Type | Required | Notes |
|---|---|---|---|
| `guardianDocsId` | String | no | An existing guardian. Sent: linked, and everything below except the relation and the flags is **ignored**. |
| `fullName` | String | **when `guardianDocsId` is not sent** | A new guardian is written from it. |
| `phoneNumber` · `alternatePhoneNumber` · `emailAddress` · `address` · `occupation` · `preferredLanguage` | | no | The new guardian's. Ignored when linking. |
| `relation` | GuardianRelation | **yes** | Always — it is the one thing a link cannot be without. |
| `primaryContact` · `emergencyContact` · `pickupAuthorized` · `portalAccess` | Boolean | no | Default false. **Always this child's**, even when linking. |
| `version` | Long | **yes** | **The student's** — the link lives in the child's document. |

### It does both jobs, which is wider than the plan asked for

The plan made `guardianDocsId` **required** — link only, with [#7](#e7) for creating. **Widened
2026-10-07**, because the desk does not work that way: somebody adding a father types his name and
his number, and whether the school already holds him is the thing they are about to find out.

So a taken number is the **same refusal [#1](#e1) gives**, naming the holder and **quoting the id to
send back**. That round trip is the whole flow: type a number, be told whose it is, decide, link.

**The identity rule is not written twice.** This hands its one row to the same `linkGuardians` #1
uses, with the "exactly one primary" check off — that question is about a child's whole list, and
this can only see the row being added.

### The relation and the flags are always this child's

Even when an existing guardian is linked. The same man is "father, primary, may collect, portal" to
one child and only an emergency number for their cousin, so they are **never read off the guardian
and never written back to them**.

**`primaryContact: true` clears it on the child's other guardians** in the same write, and the
answer names who was demoted. Two primaries is not a state worth reaching, and refusing instead
would make *"this is the person to ring now"* impossible to say.

### A record built in Java is not validated, and that cost a bug

The row handed to `linkGuardians` is constructed in code rather than bound from a body, so the
`@NotBlank` on `fullName` **never fires for it**. Measured 2026-10-07: a request with neither an id
nor a name answered `200` and wrote **a guardian with an empty name** — a row nobody could ever find
again. The check is now explicit, before anything is written.

| Refusal | When |
|---|---|
| `404 STUDENT_NOT_FOUND` | No child of that id **in this school**. |
| `404 GUARDIAN_NOT_FOUND` | `guardianDocsId` names nobody in this school. |
| `400 GUARDIAN_NAME_REQUIRED` | Neither an id nor a `fullName`. |
| `400 VALIDATION_FAILED` | No `relation`, no `version`, or a field over its length. |
| `409 GUARDIAN_PHONE_TAKEN` | Creating one on a number that is already somebody's. **The message quotes their id.** |
| `409 GUARDIAN_EMAIL_TAKEN` | The same for an address. |
| `409 GUARDIAN_ALREADY_LINKED` | That person is already a guardian of this child. **Checked after the person is resolved**, because a caller who typed a number without an id has no way of knowing it is them. |
| `409 TOO_MANY_GUARDIANS` | The child already has ten — the same cap [#1](#e1) puts on the list it accepts, enforced here because this is how that list grows afterwards. |
| `409 CONCURRENT_MODIFICATION` | Somebody wrote to this child first — two people adding a contact at once is exactly this. |

**Gates 1 and 2. No gate 4.**

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
