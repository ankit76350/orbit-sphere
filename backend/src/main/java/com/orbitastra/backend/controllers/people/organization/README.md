# controllers/people/organization — API plan

**#9 is built; #10 to #15 are not.** This is the detailed plan for the **organization package** — the org chart a
school is hiring into. It expands the group that [`controllers/people`](../README.md) lists.

> **Numbers are the domain's, not this file's.** `#9` here is `#9` there. One endpoint keeps one
> number across the whole of `people`, so the numbers below run `9–15` with nothing before or
> after them.

Mirrors [`models/people/organization`](../../../models/people/organization) — two documents, no
embedded types, no encryption, nothing blocked.

> **This is the package phase 1 starts with**, and that surprises people. `POST /staff` looks like
> the first call, but [#16](../staff/README.md#e16) — the write that actually employs somebody —
> needs a `positionDocsId`, and a position needs a department. **`#9` and `#13` are the first two
> calls anyone makes against this product's people module.**

---

## What this package is

**Two documents that describe seats, not people.**

```text
Department  "Academics"                    departmentCode ACADEMICS
  │           may nest: parentDepartmentDocsId
  │           may name a head: headStaffDocsId -> Staff
  │
  ├── Position  "Senior Mathematics Teacher"     positionCode SR_MATHS
  │     approvedHeadcount 4
  │     teachingPosition TRUE          <- what a teacher picker filters on
  │     reportsToPositionDocsId        <- a SECOND hierarchy, see open item 1
  │
  └── Position  "Head of Academics"
        approvedHeadcount 1
        teachingPosition false
```

| Document | Collection | Phase |
|---|---|---|
| [`Department`](../../../models/people/organization/Department.java) | `staff_departments` | **1** |
| [`Position`](../../../models/people/organization/Position.java) | `staff_positions` | **1** |

### `teachingPosition` is the field the rest of the product is waiting for

`Position` carries a boolean that nothing else in `people` uses and that
[`academics`](../../academics/structure/README.md) needs constantly:

```java
private Boolean teachingPosition = false;
```

**A class teacher, a subject teacher and a timetable entry all want "staff who teach", not "staff".**
Without it, every teacher picker in the product would list the bursar and the receptionist.

It lives on the **position**, not the person, which is right: a teacher promoted to head of
academics stops teaching the day their employment record points at a non-teaching seat, and nothing
about them has to change. **[#7](../staff/README.md#e7) gains a `?teaching=true` filter because of
this field**, and it was missed when the domain plan was written.

### Headcount is approved here and filled somewhere else

`approvedHeadcount` says how many people *may* hold a seat. **How many do is never stored** — it is
counted from current employment records by [#15](#e15), because the model README says so and
because a stored counter drifts the first time a writer forgets it.

The same reasoning keeps `Position` free of a `filledHeadcount`, `AcademicTerm` free of a stored
weight total, and `GradingScheme` free of a stored gap warning. Three modules, one rule.

---

## Which gates every endpoint runs

| | Gates |
|---|---|
| every **write** here | 1 school is live · 2 school is paying |
| every **read** here | none |

**No gate 4** — an org chart is not scoped to an academic year. A department created in March is
used in April, and gate 4 would refuse the one call a school actually makes.

---

# The endpoints

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t9"></a>9 — **built** | [`POST /departments`](#e9) | Create an org unit, optionally under another. |
| <a id="t10"></a>10 | [`PATCH /departments/{id}`](#e10) | Rename it, move it, or name its head. Never its code. |
| <a id="t11"></a>11 | [`POST /departments/{id}/deactivate`](#e11) · [`/reactivate`](#e11) | Retire a unit without deleting it. Idempotent pair. |
| <a id="t12"></a>12 | [`GET /departments`](#e12) | The tree, or one flat filtered page. |
| <a id="t13"></a>13 | [`POST /positions`](#e13) | Create an approved seat inside a department. |
| <a id="t14"></a>14 | [`PATCH /positions/{id}`](#e14) | Retitle it, move the headcount, retire it. |
| <a id="t15"></a>15 | [`GET /positions`](#e15) | Seats, with **filled counts computed**. |

---

# Build order

| Phase | What it gives you | Endpoints |
|---|---|---|
| **1** | A seat exists, so somebody can be hired into it | 9, 13 |
| **2** | The chart is maintainable and readable | 12, 15, 10, 14, 11 |

**`#9` and `#13` are the first two endpoints of the entire people module.** Everything downstream —
staff, employment, payroll, a teacher picker — waits on a `positionDocsId` existing.

**`#12` and `#15` come before the edits**, unusually. A school that has created two departments
needs to *see* them before it needs to rename one, and the reads are what make `#10` and `#14`
testable at all.

---

# Things this package deliberately will not have

- **No `DELETE`.** A retired department is [#11](#e11); a retired position is a field on
  [#14](#e14). Employment records point at positions, and a deleted seat would leave a person's
  history naming nothing.
- **No org-chart endpoint that walks people.** [#12](#e12) returns the *department* tree.
  "Everyone under Priya" walks `EmploymentRecord.managerDocsId`, which is a different graph, in a
  different package, and wants a use case before it wants an endpoint.
- **No headcount counter on the document.** [#15](#e15) computes it. See above.
- **No moving a position between departments.** `departmentDocsId` is set at create and never
  changes — see [#14](#e14). A seat that moves department is a new seat, and the employment records
  under the old one stay truthful.

---

# To settle before building

## 1. There are two hierarchies and they can disagree

```text
Department.parentDepartmentDocsId      Academics → Science → Physics
Position.reportsToPositionDocsId       Physics Teacher → Head of Science
```

**Both exist on the model, and nothing keeps them consistent.** A position in *Physics* may report
to a position in *Finance*, and both documents would be valid.

They are also answering different questions — the first is *"where does this seat sit in the
organisation"*, the second is *"who approves this person's leave"* — so making one derive from the
other would be wrong.

**Recommendation: validate neither against the other, and say so.** [#13](#e13) checks that
`reportsToPositionDocsId` is a position of the same school and stops there. What it must not do is
quietly require the same department, because a school with a single Head of Safeguarding that every
department reports to on that one line is a real and sensible structure.

**What it does need is a cycle check on each**, separately — see [open item 2](#2-both-hierarchies-can-cycle-and-only-one-is-cheap-to-check).

## 2. Both hierarchies can cycle, and only one is cheap to check

A department that is its own ancestor makes [#12](#e12)'s tree build loop forever. So does a
position chain that closes on itself, the moment anything walks it.

**Departments are cheap:** a school has tens, so [#10](#e10) walks up from the proposed parent in
memory and refuses at `409 DEPARTMENT_CYCLE`. One read of the whole collection, which is what
[#12](#e12) does anyway.

**Positions are cheaper still but nothing walks them yet.** No endpoint in this plan reads
`reportsToPositionDocsId` — it is stored and never traversed. **Recommendation: check it anyway at
[#13](#e13) and [#14](#e14).** A cycle written today is a crash in whatever first tries to draw the
chart, probably months later and in a different module.

## 3. Deactivating a department: what happens to its positions?

[#11](#e11) retires a unit. Its positions are unaffected by the write — and that leaves a state the
org chart cannot draw: a retired department holding active seats, possibly with people in them.

Three ways:

- **a) Refuse while active positions remain** — `409 DEPARTMENT_NOT_EMPTY`. The school retires the
  seats first, which is what it means to close a department.
- **b) Cascade** — retire every position under it in the same write. Convenient, and it silently
  changes rows nobody mentioned.
- **c) Allow it, and let the chart render the oddity.** Honest, and pushes the problem to every
  reader.

**Recommendation: (a).** It is the only one where the data can never be in a state the product
cannot explain, and the extra call is the school saying out loud that those seats are gone. The
refusal should name how many positions are blocking it, because "retire the four seats first" is
actionable and "not empty" is not.

**It does not check for people.** A position with a current employment record is a separate
problem, and it belongs to [#14](#e14) retiring that position — not to the department above it.

---

# Where the code will live

```text
controllers/people/organization/
├── README.md                      <- this file
└── OrganizationController.java    #9–#15

services/people/
├── OrganizationService.java
└── helper/PeopleHelper.java       shared with staff — the cycle walk lives here

repositories/people/organization/
├── DepartmentRepository.java
└── PositionRepository.java        + Custom/Impl for #15's filled-count aggregation

dto/people/organization/{request,response}/
```

**One controller, because two documents that only exist in relation to each other are one subject.**
A position outside a department is not a thing.

**The cycle walk goes in `PeopleHelper`, not `OrganizationService`** — it is the rule MongoDB
cannot express, which is what that class is for in every other module.

---

# Appendix — what each field can hold

## `staff_departments` — [Department](../../../models/people/organization/Department.java)

| Field | Type | What can be in it |
|---|---|---|
| `departmentCode` | String, required | Unique with `schoolId`. **Given, not derived** — the `termCode` rule, settled 2026-09-12. **Never changes**, because it is what a person types into a filter and reads on an export. |
| `name` | String, required | Editable. Indexed with `active` for [#12](#e12). |
| `description` | String | Free text. |
| `parentDepartmentDocsId` | String | Optional nesting. **Refused if it would create a cycle.** |
| `headStaffDocsId` | String | A `Staff` of this school, validated to exist. **Not required to be employed** — see [#9](#e9). |
| `active` | Boolean, required | `true` at create, never accepted. [#11](#e11) flips it. |

**Index:** `school_department_code_uniq {schoolId, departmentCode}` unique ·
`school_department_active_name_idx {schoolId, active, name}`

## `staff_positions` — [Position](../../../models/people/organization/Position.java)

| Field | Type | What can be in it |
|---|---|---|
| `positionCode` | String, required | Unique with `schoolId`. **Never changes.** |
| `title` | String, required | What the seat is called. Editable. |
| `departmentDocsId` | String, required | The owning unit, **active** at create. **Never changes** — see [what this package will not have](#things-this-package-deliberately-will-not-have). |
| `reportsToPositionDocsId` | String | A second hierarchy — see [open item 1](#1-there-are-two-hierarchies-and-they-can-disagree). |
| `approvedHeadcount` | Integer | Defaults to **1**. Null means uncapped. **Filled count is never stored.** |
| `teachingPosition` | Boolean, required | Defaults to `false`. **What every teacher picker in the product filters on.** |
| `active` | Boolean, required | `true` at create. Retiring a seat does not end anybody's employment. |

**Index:** `school_position_code_uniq {schoolId, positionCode}` unique ·
`school_department_position_active_idx {schoolId, departmentDocsId, active, title}`

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `DEPARTMENT_NOT_FOUND` · `POSITION_NOT_FOUND` | 404 | not this school's |
| `DEPARTMENT_CODE_TAKEN` · `POSITION_CODE_TAKEN` | 409 | already this school's |
| `DEPARTMENT_CYCLE` | 409 | [#10](#e10) — a department cannot be its own ancestor |
| `POSITION_CYCLE` | 409 | [#13](#e13), [#14](#e14) — nor can a reporting line |
| `DEPARTMENT_NOT_EMPTY` | 409 | [#11](#e11) — active positions remain. **Names how many** |
| `DEPARTMENT_NOT_ACTIVE` | 409 | [#13](#e13) — creating a seat in a retired unit |
| `POSITION_STILL_FILLED` | 409 | [#14](#e14) — retiring a seat somebody currently holds |
| `HEADCOUNT_BELOW_FILLED` | *warning* | [#14](#e14) — **reported, not refused** |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

<a id="e9"></a>
**[9](#t9) · `POST /departments`** — built

- [`staff_departments`](../../../models/people/organization/Department.java) — *reads*: the code is free; the parent exists, if one was named
- [`staff`](../../../models/people/staff/Staff.java) — *reads*: `headStaffDocsId` exists in this school, if one was named
- [`staff_departments`](../../../models/people/organization/Department.java) — *insert*: `schoolId`, `departmentCode`, `name`, `description`, `parentDepartmentDocsId`, `headStaffDocsId`, `active` = `true`
- **`departmentCode` is given, never derived.** The rule this project settled on 2026-09-12 for `termCode`: deriving a code from a name ties two fields that do not move together, and a school renaming "Academics" to "Teaching & Learning" should not be offered a new code for a department twenty positions reference.
- **The head is validated to exist but not to be employed.** A department may be headed by somebody whose employment record has not been written yet — during setup that is the normal order, and refusing it would force a school to enter its org chart backwards.
- **`active` is not accepted.** It starts `true`; retiring is [#11](#e11), an event with its own endpoint, the way every lifecycle flag in this project works.
- **No cycle is possible at create** — a brand-new department cannot be its own ancestor — so the walk only runs on [#10](#e10). `PeopleHelper` is therefore **not created yet**: a helper holding one method for one caller is the abstraction this project's service rules exist to prevent, and #10 is what will earn it.
- **The code is stored trimmed and upper-cased.** `"  admin  "` and `"ADMIN"` are one code, because a person typing a filter should not have to know which case the school used that day. The uniqueness check runs on the normalised value, so the two cannot both exist.
- **Two departments may share a `name`; only the code is unique.** The index says so — `school_department_code_uniq` names `departmentCode` alone — and a school with two units both called "Science" under different parents is a real org chart, not a mistake.
- **`schoolId` is set explicitly on the insert**, and that is not boilerplate. `SchoolBase` declares it `@NotBlank` but nothing validates a document on save: a unit written without it is stored, invisible to every tenant-scoped query, and found only by reading the raw collection. That exact bug shipped in this project's term create on 2026-09-11.
- **The uniqueness check is the enforcement, not a nicety in front of the index.** `school_department_code_uniq` is declared on the model but built on demand (`app.mongo.sync-indexes`), so a database that has never synced carries no such constraint at all.

<a id="e10"></a>
**[10](#t10) · `PATCH /departments/{id}`**

- *updates*: `name`, `description`, `parentDepartmentDocsId`, `headStaffDocsId`
- **Never `departmentCode`.** Nothing joins on it, which is exactly what makes editing it dangerous: no query would break, and every export, filter and report naming the old code would quietly stop matching. The `termCode` reasoning, unchanged.
- **A cycle is refused** — `409 DEPARTMENT_CYCLE` — by walking up from the proposed parent. One read of the collection, which [#12](#e12) does anyway, so the cost is a round trip and not a design problem.
- **The parent may be cleared**, promoting a department to the top level. **It may not be set to itself**, which is the one-step case of the cycle check.

<a id="e11"></a>
**[11](#t11) · `POST /departments/{id}/deactivate` · `/reactivate`**

- *updates*: `active`
- **Idempotent pair, no body** — the shape every lifecycle flag in this project uses: `/results/lock` on a term, `/enrollment/enable` on a year, `/deactivate` on a grading scheme. Already retired is a `200` saying so.
- **Deactivate is refused while active positions remain** — `409 DEPARTMENT_NOT_EMPTY`, **naming how many**, because "retire the four seats first" is actionable and "not empty" is not. See [open item 3](#3-deactivating-a-department-what-happens-to-its-positions).
- **Reactivate has no such check**, and needs none: restoring a unit cannot invalidate anything.

<a id="e12"></a>
**[12](#t12) · `GET /departments`**

- *reads*: the school's departments, filtered
- **`?tree=true` returns the nesting**, built in the service from **one flat read** — not recursive queries. A school has tens of departments; a read-per-level would be a query storm for a structure that fits in memory.
- **`?active=` filters, and absent returns both** — the tristate rule this project uses everywhere. **A retired department still appears in the tree when the filter is absent**, marked, because its children are still there and a tree with a hole in the middle is not a tree.
- **Not paged.** A department list is tens of rows and the tree has no meaningful page boundary.

<a id="e13"></a>
**[13](#t13) · `POST /positions`**

- [`staff_departments`](../../../models/people/organization/Department.java) — *reads*: the department, which must be **active**
- [`staff_positions`](../../../models/people/organization/Position.java) — *reads*: the code is free; `reportsToPositionDocsId` exists and does not cycle
- *insert*: `positionCode`, `title`, `departmentDocsId`, `reportsToPositionDocsId`, `approvedHeadcount`, `teachingPosition`, `active` = `true`
- **`409 DEPARTMENT_NOT_ACTIVE`** for a retired unit. A seat nobody may be hired into, inside a unit that no longer exists, is two problems.
- **`teachingPosition` defaults to `false`** and should almost always be sent. It is what [#7](../staff/README.md#e7) filters a teacher picker on, and a school that leaves it false on every seat gets an empty picker with no error to explain it — worth a `warning` on the response when a school's *first* positions are all non-teaching.
- **`approvedHeadcount` defaults to 1.** Null means uncapped, which is different from 1 and worth sending deliberately.
- **The reporting line is not required to be in the same department** — see [open item 1](#1-there-are-two-hierarchies-and-they-can-disagree).

<a id="e14"></a>
**[14](#t14) · `PATCH /positions/{id}`**

- *updates*: `title`, `reportsToPositionDocsId`, `approvedHeadcount`, `teachingPosition`, `active`
- **Never `positionCode`**, and **never `departmentDocsId`.** A seat that moves department is a new seat: editing it in place rewrites where every past holder worked, and the employment records under it would silently change department too.
- **Lowering `approvedHeadcount` below the filled count is a `warning`, not a refusal.** A school reducing an approved seat count that is already over-filled is describing something that has already happened, and refusing it makes the number impossible to correct. The same call [#16](../staff/README.md#e16) makes about `POSITION_FULL`, and the same one the term-weight sum made.
- **`active` is a field here rather than an endpoint pair**, unlike [#11](#e11) — and that is an inconsistency worth naming. A position has no dependents to check, so the event has no rules of its own; a department does. If a reason to check one appears, this becomes `/positions/{id}/deactivate` and the field comes out.
- **Retiring a position somebody currently holds is refused** — `409 POSITION_STILL_FILLED`. They are separated or transferred first, which is [#17](../staff/README.md#e17) or [#16](../staff/README.md#e16).

<a id="e15"></a>
**[15](#t15) · `GET /positions`**

- [`staff_positions`](../../../models/people/organization/Position.java) — *reads*: the filtered page
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *reads*: the **count** of current records per position on that page
- **`filledHeadcount` is computed, never stored.** A stored counter drifts the first time a writer forgets it — the objection that also keeps a weight total off `AcademicTerm` and a gap warning off `GradingScheme`.
- **Counted for the page, not the collection.** One grouped count over the position ids on the page, so a school with two hundred seats does not aggregate all of them to render twenty.
- **Filters**: `?departmentDocsId=`, `?active=`, `?teaching=`, `?vacant=`. **`?vacant=true` is the interesting one** — approved headcount not yet filled — and it is the query a school actually runs at the start of a hiring round.
- **Served by `school_department_position_active_idx`** on `{schoolId, departmentDocsId, active, title}`, which is why the default order is title within department.
- **No gates.** A suspended school still reads its own org chart.
