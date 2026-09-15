# controllers/people/organization — API plan

**#9, #10, #12, #13, #14 and #52 are built; the rest are not. #11 was absorbed into #10 on 2026-09-15.** **#52 is not in the original plan** — it was added on 2026-09-15, numbered on the end because these numbers are referenced from the catalogue and the Postman collection and none is ever reused. This is the detailed plan for the **organization package** — the org chart a
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
  ├── Position  "Senior Mathematics Teacher"     addressed by its document id
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
| <a id="t10"></a>10 — **built** | [`PATCH /departments/{id}`](#e10) | Rename it, describe it, name its head, retire it. Never its code, and **never its parent**. |
| <a id="t11"></a>11 — **superseded** | ~~[`POST /departments/{id}/deactivate`](#e11) · [`/reactivate`](#e11)~~ | **Absorbed into [#10](#t10) on 2026-09-15** as its `active` field, refusal and all. |
| <a id="t12"></a>12 — **built** | [`GET /departments`](#e12) | The tree, or one flat filtered page. |
| <a id="t13"></a>13 — **built** | [`POST /positions`](#e13) | Create an approved seat inside a department. |
| <a id="t14"></a>14 — **built** | [`PATCH /positions/{id}`](#e14) | Retitle it, move the headcount, change its line, retire it. **Never its department.** |
| <a id="t15"></a>15 | [`GET /positions`](#e15) | Seats, with **filled counts computed**. |
| <a id="t52"></a>52 — **built** | [`GET /departments/{id}`](#e52) | One unit and everything it is made of — its parent department, its sub-departments, its head and its seats. **Added 2026-09-15**, after the plan. |

---

# Build order

Ordered by **what it unblocks**, not by number. Nothing else in `people` can start until a
`positionDocsId` exists, so the two creates come before everything that is merely useful.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **0** | ~~`positionCode` removed, and the index that replaced it~~ — **done 2026-09-15** | *no endpoint; see [#13](#e13)* |
| **1** | A seat exists, so somebody can be hired into it | ~~9~~, ~~13~~ — **complete** |
| **2** | The chart is maintainable and readable | ~~12~~, ~~52~~, 15, ~~10~~, ~~14~~, ~~11~~ *(absorbed into [#10](#e10))* |

**Only [#15](#e15) is left**, and it is the one endpoint here that cannot be written yet: its whole
point is `filledHeadcount`, counted from `employment_records`, and nothing writes one. See the two
checks [#14](#e14) owes for the same reason.

**`#9` and `#13` are the first two endpoints of the entire people module.** Everything downstream —
staff, employment, payroll, a teacher picker — waits on a `positionDocsId` existing.

**`#12` and `#15` come before the edits**, unusually. A school that has created two departments
needs to *see* them before it needs to rename one, and the reads are what make `#10` and `#14`
testable at all. That held in practice: [#52](#e52) was added mid-phase because rendering one
unit's page took four requests, and it is what both edit screens are built on.

**[#52](#e52) is not in the original plan** and sits in phase 2 with the other reads. It was added
on 2026-09-15, numbered on the end because these numbers are referenced from the API catalogue and
the Postman collection and none is ever reused.

**[#11](#e11) was absorbed into [#10](#e10)**, not built and not dropped. `active` became a field
on the department edit rather than an endpoint pair — and it carried its `DEPARTMENT_NOT_EMPTY`
refusal with it, because the rule belongs to the transition and not to whichever endpoint performs
it.

**Phase 0 is a model change, and it earned a row the way a broken index did in
[`academics/structure`](../../academics/structure/README.md#build-order).** Removing `positionCode`
left nothing constraining a duplicate seat, so `school_department_title_uniq` had to replace it in
the same change — a unique index on a field the model no longer declares is not inert, it is a
collection that accepts exactly one row per key prefix.

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

**Departments are cheap:** a school has tens, so [#10](#e10) was to walk up from the proposed
parent in memory and refuse at `409 DEPARTMENT_CYCLE`. One read of the whole collection, which is
what [#12](#e12) does anyway.

**That walk was never built, because the move it guarded was dropped.** On 2026-09-15 [#10](#e10)
stopped accepting a parent at all, so **no endpoint in this product can write a department cycle**
— [#9](#e9) cannot either, since a new unit has no children. `PeopleHelper` stays unearned.

**[#12](#e12) keeps its visited set anyway, and that is the point of this item.** The sentence
above is true of the API, not of the collection: a hand-edited document, a restored backup, or a
later endpoint that does move units can all make it false without touching the tree builder. The
guard is what turns "nothing writes a cycle" into "a cycle cannot crash the read".

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
| `title` | String, required | What the seat is called. Editable. **Not unique** — see the note under the index. |
| `departmentDocsId` | String, required | The owning unit, **active** at create. **Never changes** — see [what this package will not have](#things-this-package-deliberately-will-not-have). |
| `reportsToPositionDocsId` | String | A second hierarchy — see [open item 1](#1-there-are-two-hierarchies-and-they-can-disagree). |
| `approvedHeadcount` | Integer | Defaults to **1**. **Not nullable** — the model declares it `@NotNull`, so "uncapped" is not a storable state despite what this table said before 2026-09-15. [#13](#e13) turns an absent or null value into 1. **Filled count is never stored.** |
| `teachingPosition` | Boolean, required | Defaults to `false`. **What every teacher picker in the product filters on.** |
| `active` | Boolean, required | `true` at create. Retiring a seat does not end anybody's employment. |

**Index:** `school_department_position_active_idx {schoolId, departmentDocsId, active, title}`
— and **no unique index at all**.

> **`positionCode` was removed on 2026-09-15**, and `school_position_code_uniq` had to go with it.
> A unique index naming a field the model no longer declares is not inert: every document then
> indexes a *missing* value, so the key is identical for all of them and the collection accepts
> exactly **one** row per school. That is the `school_year_class_code_uniq` defect this project
> already shipped, found live, and migrated 659 documents out of. `staff_positions` did not exist
> in `edusphere_dev` when the field was removed, so there was no built index to drop.
>
> **A position is addressed by its document id** — which is what `EmploymentRecord` stores as
> `positionDocsId`, and what every reference to a seat already uses. The same call this project
> made for a class on 2026-09-10.
>
> **Nothing makes a position unique now.** Two seats with the same `title` in one department are
> accepted, and that is a real structure — two Mathematics Teacher seats reporting to different
> heads — but it is a change from the plan as written, not an oversight in it.

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `DEPARTMENT_NOT_FOUND` · `POSITION_NOT_FOUND` | 404 | not this school's |
| `DEPARTMENT_CODE_TAKEN` | 409 | already this school's. **There is no `POSITION_CODE_TAKEN`** — `positionCode` was removed on 2026-09-15 |
| ~~`DEPARTMENT_CYCLE`~~ | — | **never implemented.** [#10](#e10) does not accept a parent, so no endpoint can write one |
| `POSITION_CYCLE` | 409 | [#14](#e14) — nor can a reporting line. **Not [#13](#e13)**: a new seat has nothing reporting to it, so the check could never fire |
| `DEPARTMENT_NOT_EMPTY` | 409 | [#10](#e10) `active:false` — active positions remain. **Names how many** |
| `DEPARTMENT_NOT_ACTIVE` | 409 | [#13](#e13) — creating a seat in a retired unit |
| `POSITION_STILL_FILLED` | 409 | [#14](#e14) — retiring a seat somebody holds. **Not implemented**: nothing employs anybody yet, so the count could only be zero. Owed by [#16](../staff/README.md#e16) |
| `HEADCOUNT_BELOW_FILLED` | *warning* | [#14](#e14) — **reported, not refused**, and **not implemented** for the same reason. Owed by [#16](../staff/README.md#e16) |
| `NOTHING_TO_UPDATE` | 400 | [#10](#e10), [#14](#e14) — reuses core's code. A body of only uneditable fields counts as empty |
| `DEPARTMENT_NAME_REQUIRED` | 400 | [#10](#e10) — `name` sent blank. A name is replaced, never removed |
| `POSITION_TITLE_REQUIRED` | 400 | [#14](#e14) — `title` sent blank. A title is replaced, never removed |

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
**[10](#t10) · `PATCH /departments/{id}`** — built

- *updates*: `name`, `description`, `headStaffDocsId`, `active`
- **Absent means "leave it alone", and an empty body is `400 NOTHING_TO_UPDATE`** rather than a no-op `200`. A PATCH that changes nothing and answers success lets a client with a broken form look healthy. A body of *only* uneditable fields is the same 400 — neither is on the request record, so the request is still empty.
- **Never `departmentCode`.** Nothing joins on it, which is exactly what makes editing it dangerous: no query would break, and every export, filter and report naming the old code would quietly stop matching. The `termCode` reasoning, unchanged.
- **Never the parent — a unit cannot be moved.** The plan had this endpoint moving one and refusing a cycle at `409 DEPARTMENT_CYCLE`; **that was dropped on 2026-09-15**. Where a unit sits is decided when it is created, under the parent whose page created it, and a move is the one edit here that changes what every *other* unit's page shows.
- **So no endpoint in this product can write a cycle into the department chart.** [#9](#e9) cannot, because a new unit has no children; #10 cannot, because it does not accept a parent. [#12](#e12)'s tree builder **still carries its visited set** — that is what makes the sentence true of the *data* rather than of the code's current shape, and [open item 2](#2-both-hierarchies-can-cycle-and-only-one-is-cheap-to-check) is why that distinction matters.
- **`""` clears `description` and `headStaffDocsId`; `""` on `name` is `400 DEPARTMENT_NAME_REQUIRED`.** The model requires a name and it is the only thing on this document a person reads.
- **The head is checked to EXIST, not to be employed** — the same rule [#9](#e9) follows. Another school's real staff id is a `404`, not a cross-tenant head. It is validated **before anything is saved**, so a request carrying a good name and a bad head changes neither.
- **`active` lives here rather than on [#11](#e11)'s endpoint pair**, asked for on 2026-09-15. See below — it carries #11's refusal with it.

<a id="e11"></a>
**[11](#t11) · ~~`POST /departments/{id}/deactivate` · `/reactivate`~~** — superseded by [#10](#e10)

- *was*: `active`, as an idempotent endpoint pair with no body — the shape every lifecycle flag in this project uses: `/results/lock` on a term, `/enrollment/enable` on a year, `/deactivate` on a grading scheme.
- **Absorbed into #10 as a field on 2026-09-15**, which is a real departure from that shape and is recorded here rather than quietly dropped.
- **The refusal moved with it, and that was the whole point.** Retiring a unit that still holds active positions is `409 DEPARTMENT_NOT_EMPTY` **naming how many**, exactly as specified here — because "retire the four seats first" is actionable and "not empty" is not. The rule belongs to the transition, not to whichever endpoint performs it; a field reaching the same state without the check would have been a back door around a decision this module already made. See [open item 3](#3-deactivating-a-department-what-happens-to-its-positions).
- **Reactivating still has no check**, and needs none: restoring a unit cannot invalidate anything.
- **Sub-departments do not block a retire**, and that asymmetry is deliberate: [#12](#e12) already answers for a retired parent whose children are still active by lifting them to the top and marking them `liftedToTop`. That state is designed for. A live seat has no such answer.

<a id="e12"></a>
**[12](#t12) · `GET /departments`** — built

- *reads*: the school's departments, filtered
- **`?tree=true` returns the nesting**, built in the service from **one flat read** — not recursive queries. A school has tens of departments; a read-per-level would be a query storm for a structure that fits in memory.
- **`?active=` filters, and absent returns both** — the tristate rule this project uses everywhere. **A retired department still appears in the tree when the filter is absent**, marked, because its children are still there and a tree with a hole in the middle is not a tree.
- **The flat side IS paged — changed 2026-09-15.** The plan said not to, reasoning that a department list is tens of rows. True of most schools and not a constraint anywhere: nothing caps the count, and a group running forty units through one tenant would page. The cost is the shared record and factory that already exist, and this row's own title said "one flat filtered **page**".
- **The tree is NOT paged, and asking is `400 TREE_CANNOT_BE_PAGED` rather than ignored.** This half of the plan's reasoning was exactly right and is the more important half: a page boundary in a tree cuts children off their parents, so page 2 is not part of a chart — it is a broken one that still looks like a chart. Silently dropping the parameter would hide that.
- **Five filters, all optional, all AND-ed:** `?active=` · `?search=` · `?parentDepartmentDocsId=` · `?headStaffDocsId=` · `?topLevelOnly=`
- **`?search=` matches `name` **or** `departmentCode`**, regex-quoted so a stray `(` is an empty answer rather than a 500.
- **`?topLevelOnly=` asks `exists` on the parent, not a null comparison** — a document written before the field existed has no key at all and must read as top-level.
- **Sorted by `name`, tiebroken by `departmentCode`.** Two units may share a name — this file says so — so `name` alone ties, and a tie with no tiebreaker puts one row on two pages while another appears on none. `departmentCode` is unique per school and settles it. [`PageResponse.pageableOf`](../../../common/web/PageResponse.java) appends whichever the caller did not name.
- **A filter can orphan a node, and orphans are lifted rather than dropped.** `?tree=true&active=true` excludes a retired parent whose children are still active — those children are real units the caller asked to see, so they surface at the top marked `liftedToTop` instead of vanishing. The response carries the count. **Dropping them was the alternative and would have hidden active departments**; leaving a hole where the parent was is not a tree.
- **The builder carries a visited set, and that is not defensive habit.** Nothing can write a cycle today — [#9](#e9) cannot, because a new unit has no children — but [#10](#e10) will be able to, and [open item 2](#2-both-hierarchies-can-cycle-and-only-one-is-cheap-to-check) says a cycle written then is a crash in whatever first draws the chart. This is that thing, so it is where the guard belongs.
- **The two shapes are two records**, not one with a sometimes-populated `subDepartments`. A field present on some responses and absent on others is a field every client has to guard — the same call #28 and #29 of academics made.

<a id="e13"></a>
**[13](#t13) · `POST /positions`** — built

- [`staff_departments`](../../../models/people/organization/Department.java) — *reads*: the department, which must be **active**
- [`staff_positions`](../../../models/people/organization/Position.java) — *reads*: `reportsToPositionDocsId` exists and does not cycle. **No code check** — `positionCode` was removed on 2026-09-15 and nothing makes a position unique now
- *insert*: `schoolId`, `title`, `departmentDocsId`, `reportsToPositionDocsId`, `approvedHeadcount`, `teachingPosition`, `active` = `true`
- **`409 DEPARTMENT_NOT_ACTIVE`** for a retired unit. A seat nobody may be hired into, inside a unit that no longer exists, is two problems.
- **`teachingPosition` defaults to `false`** and should almost always be sent. It is what [#7](../staff/README.md#e7) filters a teacher picker on, and a school that leaves it false on every seat gets an empty picker with no error to explain it — worth a `warning` on the response when a school's *first* positions are all non-teaching.
- **`approvedHeadcount` defaults to 1.** Null means uncapped, which is different from 1 and worth sending deliberately.
- **The reporting line is not required to be in the same department** — see [open item 1](#1-there-are-two-hierarchies-and-they-can-disagree). Asserted directly: a Finance seat reporting to an Academics one is a `201`.
- **`409 POSITION_TITLE_TAKEN`, scoped to the department** — added 2026-09-15 with the removal of `positionCode`. With the code gone nothing constrained a duplicate seat, so `title` carries that job and `school_department_title_uniq` enforces it. The same title in a *different* department is fine.
- **The title check folds case; the index does not.** Mongo compares a unique index key case-sensitively, so `"Mathematics Teacher"` and `"mathematics teacher"` are two keys to the index and one title to the service. The service is the stricter of the two and therefore the enforcement in practice — and the index is what catches anything that ever bypasses it.
- **A retired seat keeps its title**, because `school_department_title_uniq` does not filter on `active` — consistent with a retired term keeping its code and a retired department keeping its own. A check that skipped retired rows would accept a write the index then refuses.
- **No cycle walk here.** A brand-new seat has nothing reporting to it, so it cannot be its own ancestor whatever it reports to — the same reason [#9](#e9) has none. [Open item 2](#2-both-hierarchies-can-cycle-and-only-one-is-cheap-to-check) recommends checking at #13 anyway; that check could never fire, and #14 is where it will.
- **`approvedHeadcount` follows the model, not this plan.** The field table below says "null means uncapped"; [`Position`](../../../models/people/organization/Position.java) declares it `@NotNull` with a builder default of **1**, so uncapped is not a state a stored seat can be in. Absent becomes 1 and a zero or negative is a `400`. **Making uncapped real means dropping `@NotNull` from the model** — a model change, and not this endpoint's to make.
- **The teaching warning is per department, not per school.** A unit whose seats are all non-teaching gets a `warning` on the response — legitimate for Finance, and also exactly what an empty teacher picker looks like. Creating Facilities does not warn a school whose Academics seats are correctly flagged.

<a id="e52"></a>
**[52](#t52) · `GET /departments/{id}`** — built, and **not in the original plan**

- *reads*: the unit; its parent department and its head, **resolved**; its direct sub-departments; every seat in it
- **Added 2026-09-15.** The plan has a list ([#12](#e12)) and a tree, and no "tell me about this one" — rendering a department's page meant four requests. Numbered on the end rather than beside #12 because these numbers are referenced from the API catalogue and the Postman collection, and none is ever reused.
- **The one endpoint in this package that resolves an id to a name.** Everywhere else `parentDepartmentDocsId` and `headStaffDocsId` come back raw, because one place should decide how a unit and a person are presented and a write's response is not it. A detail view **is** that place — the whole question it answers is "tell me about this unit".
- **The head resolves to a name and nothing else.** A `Staff` document carries an address, a date of birth and a national identity number. Returning the record would leak the most sensitive data this product holds through an endpoint nobody would think to check — and the module plan says authorization matters more here than anywhere and does not exist yet.
- **A dangling reference leaves the page readable.** A parent or a head deleted out from under the unit is **omitted**, not a 404: the department the caller asked for still exists, and refusing to describe it because something it points at is gone would make the damage worse. Both directions are asserted.
- **Direct sub-departments only.** The whole nesting is [#12](#e12) with `?tree=true`, which builds it from one flat read; repeating that walk here would be a second implementation of it.
- **Retired seats are included and marked.** A retired seat is still part of what a unit is made of — records made against it still name it.
- **The positions are here, and that is a boundary worth stating.** This answers "what is this unit made of"; [#15](#e15) will answer "find seats across the school", filtered and paged. When it arrives the two return the same rows in two shapes — the situation [#29 of academics](../../academics/structure/README.md#e29) ended up in and had to be trimmed out of. **If it becomes a problem, #15 wins and this trims**, for the same reason: the endpoint that owns the question keeps it.
- **Not paged.** One document's composition is not a list.
- **`loadDepartment` moved to `OrganizationServiceUtils` when this arrived** — three callers wanted the same tenant-scoped lookup, and two was a coincidence where three is a rule with three places to get it wrong.

<a id="e14"></a>
**[14](#t14) · `PATCH /positions/{id}`** — built

- *updates*: `title`, `reportsToPositionDocsId`, `approvedHeadcount`, `teachingPosition`, `active`
- **Absent means "leave it alone", and an empty body is `400 NOTHING_TO_UPDATE`.** A body of *only* `departmentDocsId` is the same 400 — it is not on the request record, so the request is still empty.
- **Never `departmentDocsId`.** A seat that moves department is a new seat: editing it in place rewrites where every past holder worked, and the employment records under it would silently change department too. It is also what keeps `school_department_title_uniq` meaningful — a title is unique *within* a unit, and a seat that could move would carry its title across that boundary. The same call [#10](#e10) makes about a department's parent, for a different reason: **a department's parent is structure, a seat's department is history.**
- **This is the endpoint that can write a reporting cycle, so this is where the walk is** — `409 POSITION_CYCLE`. [#13](#e13) needs none: a brand-new seat has nothing reporting to it. Reporting to itself is the one-step case of the same walk, named separately only because the message can be clearer. See [open item 2](#2-both-hierarchies-can-cycle-and-only-one-is-cheap-to-check).
- **The walk carries a visited set**, and that is not habit: a cycle already in the collection — hand-written, restored from a backup, left by a future writer — would make the walk itself loop forever. It stops and reports rather than hanging the request. **One read per level**, not one read of the collection: a reporting chain is a handful of seats deep, where a department tree is read whole by [#12](#e12) anyway.
- **The title carries the uniqueness `positionCode` used to**, scoped to the department, **retired seats included** — `school_department_title_uniq` does not filter on `active`. **The duplicate check skips a title that only changed case**, because that is the same seat and `existsBy…` cannot exclude it: `"mathematics teacher"` → `"Mathematics Teacher"` is a correction, not a collision.
- **Turning `teachingPosition` off is the interesting direction.** It is how a unit that had one teaching seat stops having any — the same empty teacher picker [#13](#e13) warns about, arriving by a different route — so the warning is computed here on the way **out** as well. A warning rides on a `200`; the seat is saved.
- **`active` is a field here rather than an endpoint pair**, unlike [#11](#e11) — and that inconsistency is now resolved the other way: #11 was absorbed into [#10](#e10) on 2026-09-15, so both a unit and a seat are retired by a field. A department's transition has a rule of its own; a seat's does not, yet.
- **The department is not checked, and that asymmetry is on purpose.** [#13](#e13) refuses a seat in a retired unit — `409 DEPARTMENT_NOT_ACTIVE` — because a seat nobody may be hired into, inside a unit that no longer exists, is two problems. #14 does not: a seat that already exists in a unit since retired still needs correcting, and refusing to edit it would strand it.

**Two checks this plan specifies and #14 does NOT implement.** Both are recorded rather than quietly skipped, and both are owed the moment [#16](../staff/README.md#e16) lands:

- **Lowering `approvedHeadcount` below the filled count is to be a `warning`, not a refusal.** A school reducing an approved count that is already over-filled is describing something that has already happened, and refusing it makes the number impossible to correct. The same call #16 makes about `POSITION_FULL`, and the same one the term-weight sum made.
- **Retiring a position somebody currently holds is to be refused** — `409 POSITION_STILL_FILLED`. They are separated or transferred first, which is [#17](../staff/README.md#e17) or #16.
- **Neither can fire yet.** There is no `EmploymentRecordRepository`, no endpoint writes one, and the collection does not exist — so the filled count could only ever be zero. A check that can never fail is not a check, which is the same call [#13](#e13) made about its cycle walk.

<a id="e15"></a>
**[15](#t15) · `GET /positions`**

- [`staff_positions`](../../../models/people/organization/Position.java) — *reads*: the filtered page
- [`employment_records`](../../../models/people/staff/EmploymentRecord.java) — *reads*: the **count** of current records per position on that page
- **`filledHeadcount` is computed, never stored.** A stored counter drifts the first time a writer forgets it — the objection that also keeps a weight total off `AcademicTerm` and a gap warning off `GradingScheme`.
- **Counted for the page, not the collection.** One grouped count over the position ids on the page, so a school with two hundred seats does not aggregate all of them to render twenty.
- **Filters**: `?departmentDocsId=`, `?active=`, `?teaching=`, `?vacant=`. **`?vacant=true` is the interesting one** — approved headcount not yet filled — and it is the query a school actually runs at the start of a hiring round.
- **Served by `school_department_position_active_idx`** on `{schoolId, departmentDocsId, active, title}`, which is why the default order is title within department.
- **No gates.** A suspended school still reads its own org chart.
