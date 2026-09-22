# controllers — what is built, and what to build next

Every module gets an **API plan README before any of its endpoints**, so the surface can be argued
about while it is still cheap to change. This file is the index of those plans, and the build order
for the two modules being worked on now.

---

## What exists today — 2026-09-21

| Module | Plan | Built |
|---|---|---|
| [`core`](core/README.md) | ✅ | 3 controllers · 32 endpoints |
| [`plans`](plans/README.md) | ✅ | 3 controllers · 22 endpoints |
| [`people/staff`](people/staff/README.md) | ✅ | 1 controller · 8 endpoints |
| [`people/department`](people/department/README.md) | ✅ | 1 controller · 8 endpoints |
| [`academics/structure`](academics/structure/README.md) | ✅ | 2 controllers · 15 endpoints |
| [`academics/timetable`](academics/timetable/README.md) | ✅ | 1 controller · 10 of 12 endpoints |
| [`academics/grading`](academics/grading/README.md) | ✅ | 1 controller · 7 endpoints |
| `localuser` | — | 1 controller · 1 endpoint |
| **[`crm`](crm/README.md)** | ✅ | **2 controllers · 10 of 34 endpoints** |
| **[`student`](student/README.md)** | ✅ | **nothing — 22 planned** |
| `people/leave` · `people/reviews` · `people/development` · `academics/attendance` | — | nothing |

---

# The order

**`crm` and `student` are built together, interleaved.** Not one and then the other, and this is the
whole reason this section exists.

## Why they interleave

**The dependency runs one way, and it is a single endpoint.**
[`crm` #33 `POST /applications/{id}/enroll`](crm/README.md#e33) creates a `Student`. That is the only
place either module touches the other. Everything else in `crm` — cycles, inquiries, applications,
reviews, offers — and everything in `student` are independent of each other.

So:

- **`crm` can be built until an offer is accepted**, and then it physically cannot continue. An
  accepted offer with no `POST /enroll` is a funnel that fills up and never empties.
- **`student` does not need `crm` at all.** `Student.admissionApplicationDocsId` is nullable and its
  unique index is partial, so transfers, walk-ins and the roll a school already has when it starts
  using the product all work with no admissions row anywhere.

Which means the cheapest order is: **run `crm` to the wall, build the minimum `student` surface to
get through it, join the two, then finish both.** Phase 5 is the whole point of the arrangement —
the day a lead becomes a child on a register.

## The phases

| Phase | Module | What you get at the end of it | Endpoints |
|---|---|---|---|
| ~~**1**~~ | `crm` | A cycle exists and can be read back | [~~1~~, ~~5~~, ~~6~~, ~~3~~](crm/README.md#t1) |
| ~~**2**~~ | `crm` | Applications can be taken and seen | [~~17~~, ~~19~~, ~~24~~, ~~25~~](crm/README.md#t17) |
| **3** | `crm` | The pipeline can be worked | [20, 26, 27, 28, 22](crm/README.md#t20) |
| **4** | `crm` | Offers can be made and answered — **and here it stops** | [29, 30, 32, 31](crm/README.md#t29) |
| **5** | `student` | A child exists, with their family attached | [1, 4, 5, 6](student/README.md#t1) |
| **6** | **both** | **A lead becomes a student.** The handover | [`crm` 33](crm/README.md#e33) |
| **7** | `student` | The child can be placed in a class — **the roster** | [14, 21, 18, 16, 17](student/README.md#t14) |
| **8** | `student` | The record can be corrected and the family managed | [2, 3, 7, 8, 9, 10, 11, 12, 13](student/README.md#t2) |
| **9** | `crm` | The lead half, which nothing else needs | [8, 13, 14, 10, 12, 11, 9, 15, 16](crm/README.md#t8) |
| **10** | both | Reads, corrections and counts | `crm` [~~2~~, ~~4~~, 7, 18, 21, 23, 34] · `student` [15, 19, 20, 22] |

**A ~~struck~~ number is built.** Ten of `crm`'s thirty-four and none of `student`'s
twenty-two — **phases 1 and 2 complete**, and the two corrections in phase 10 pulled forward
because [`crm` #17](crm/README.md#e17) cannot be tested without them. Phase 3 is next, and nothing
in it exists.

## What each phase boundary is actually for

**Phase 1 first, and nothing works without it.** Every application names a cycle, and
[`crm` #17](crm/README.md#e17) refuses without one.

**Phase 4 is the wall.** At the end of it an application can reach `OFFER_ACCEPTED` and go no
further. Stopping here deliberately — rather than pushing on into `student` half-built — is what
keeps phase 6 a single small endpoint instead of a rewrite.

**Phase 5 is the minimum, not the module.** Four endpoints: create a student, list, read one, and
search. It exists to unblock phase 6 and nothing else. The status graph, the guardian link
management and the corrections all wait for phase 8, because [`crm` #33](crm/README.md#e33) needs
none of them.

**Phase 6 is one endpoint and it is the point of both modules.** It creates the `Student`, sets
`admissionNo`, copies the guardians, links both directions, moves the application to `ENROLLED`,
settles the offer and closes the originating inquiry — in one transaction.

**Phase 7 is what the rest of the product is waiting on.** Attendance, mark sheets, fees and
`timetable` [#8](academics/timetable/README.md) all need
[`student` #21](student/README.md#e21), the section roster. Nothing outside these two modules can
start until it exists.

**Phase 9 looks backwards and is not.** A lead comes before an application in real life, so building
inquiries last seems wrong. It is deliberate: **an application does not need an inquiry**
(`inquiryDocsId` is nullable, for the family that walks in with a completed form), so the pipeline is
testable end to end without a single lead in the database. Building leads first means four endpoints
that nothing else depends on before the module does anything.

> **If you want the lead-first experience sooner**, phase 9 can move to phase 2 at no cost to
> anything else — it is the one block in this order that is free to slide. The reason it is late is
> economy, not correctness.

**Phase 10 last, because aggregations want data.** [`crm` #7](crm/README.md#e7) and
[#34](crm/README.md#e34) and [`student` #22](student/README.md#e22) are all counts over the
collections above, and all three are far easier to write — and to check — once there is a realistic
spread of statuses to count.

---

# Settle these before the phase that needs them

Each open item is in its own module's plan. This is only which phase it blocks.

| Before phase | Item | Why it blocks |
|---|---|---|
| **2** | [`crm` 2 — one inquiry, one application per cycle](crm/README.md#2-one-inquiry-one-application-per-cycle) | The unique index throws a duplicate-key 500 the first time it fires. |
| ~~2~~ | ~~[`crm` 4 — `formAnswers` is unvalidated](crm/README.md#4-formanswers-is-an-unvalidated-map)~~ | **Settled 2026-09-21.** The three fields naming a form definition were deleted; `formAnswers` stays unvalidated and #17 accepts it as sent. |
| **5** | [`student` 1 — what counts as the same guardian](student/README.md#1-what-counts-as-the-same-guardian) | **Two siblings share a father.** The unique phone index means the second admission fails unless guardians are matched. |
| **6** | [`crm` 3 — which side owns the application↔student link](crm/README.md#3-the-applicationstudent-link) | #33 writes both sides. Both are partial-unique, so both can refuse. |
| **7** | [`student` 2 — roll numbers need a scoped sequence](student/README.md#2-roll-numbers-need-a-scoped-sequence-that-does-not-exist) | `NumberSequenceService.next` hard-codes `GLOBAL_SCOPE`; roll numbers are per class and section. |
| **7** | [`student` 3 — gate 4 is off](student/README.md#3-gate-4-is-off-and-that-is-a-choice) · [4 — `effectiveFrom`](student/README.md#4-effectivefrom-when-a-record-is-created-before-the-year-starts) | A child admitted in January is placed in a June class. Both decide whether that is even expressible. |
| **8** | [`student` 6 — status cascade](student/README.md#6-what-a-students-status-does-to-their-record-and-the-reverse) | Whether withdrawing a child closes their record or refuses. |

**One is not a decision but a prerequisite:** [`student` open item 2](student/README.md#2-roll-numbers-need-a-scoped-sequence-that-does-not-exist)
needs a `next(schoolId, type, scopeKey, prefixTemplate)` overload on
[`NumberSequenceService`](../services/institution/NumberSequenceService.java). The repository's
`allocate` already takes the key — the service simply never passes one.

---

# The rule that produced both plans

**Models first, then the plan, then the endpoints — module by module.** Both
[`crm`](crm/README.md) and [`student`](student/README.md) have their models written and their plans
written, which is why they are the two modules buildable right now.

Each plan carries, before anything else, a short list of **things that were measured rather than
assumed** — indexes read as written, services checked for the method the plan expects, enums counted
against the diagrams that describe them. Three of the findings in those two files contradicted what
the model READMEs said. That section is the most valuable part of either document and it is worth
writing for the next module too.
