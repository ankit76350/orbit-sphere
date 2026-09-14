# controllers/people/reviews — API plan

**Nothing is built.** This is the detailed plan for the **reviews package** — a review period, its
criteria, and what people said. It expands the group that [`controllers/people`](../README.md)
lists.

> **Numbers are the domain's.** `#40`–`#43` came from that file; **`#45`–`#47` are new**, appended
> rather than squeezed in, because the domain plan compressed a five-state lifecycle into one
> open/close row and a cycle cannot be edited at all without them. Numbers are never reused; new
> ones go on the end.

Mirrors [`models/people/reviews`](../../../models/people/reviews) — two documents and one embedded
type.

> **This package has the one problem this project cannot engineer its way out of.** Anonymity here
> is protected by a keyed hash over a set of a few hundred staff ids — which is not anonymity
> against anyone holding the key. See [open item 1](#1-anonymous-means-two-different-things-and-this-design-only-delivers-one).

---

## What this package is

```text
ReviewCycle  "Mid-year 2026-2027"  MID_2026        academicYear 2026-2027
  status DRAFT → SCHEDULED → OPEN → IN_REVIEW → CLOSED
  anonymousPeerFeedback  true
  │
  ├── criteria[]  (embedded, versioned WITH the cycle)
  │     PLANNING    "Lesson planning"   MANAGER  weight 40  max 5
  │     CLASSROOM   "Classroom manner"  PEER     weight 30  max 5
  │     ...
  │
  └── StaffReview[]
        reviewedStaffDocsId  → Staff
        reviewerType  SELF · MANAGER · PEER · STUDENT · PARENT
        reviewerDocsId  OR  reviewerLookupHash     <- never both, when anonymous
        criterionScores  { "PLANNING": 4, "CLASSROOM": 5 }
        finalScore
```

| Document | Collection | Phase |
|---|---|---|
| [`ReviewCycle`](../../../models/people/reviews/ReviewCycle.java) | `staff_review_cycles` | **5** |
| [`StaffReview`](../../../models/people/reviews/StaffReview.java) | `staff_reviews` | **5** |
| [`ReviewCriterion`](../../../models/people/reviews/embedded/ReviewCriterion.java) | *embedded in the cycle* | **5** |

### Criteria are embedded because they are versioned with the cycle

A criterion reworded next year must not change what last year's scores meant. `4 out of 5` on
*"Lesson planning"* is only interpretable beside the wording that produced it — so the wording
lives **inside** the cycle, and a new cycle gets its own copy.

**The same decision `GradeBand` makes inside a `GradingScheme`**, for the same reason: the rule is
copied at the moment it applies, and the copy is what history is measured against.

### Criteria are per reviewer type, which is the part people miss

`ReviewCriterion.reviewerType` means **a manager and a peer score different things**. A cycle is not
one questionnaire; it is up to five, keyed by who is answering.

That decides [#42](#e42)'s validation — a submission is checked against *the criteria for its
reviewer type*, not against all of them — and it decides [#43](#e43)'s shape, because an aggregate
across reviewer types is averaging answers to different questions.

---

## Which gates every endpoint runs

| | Gates |
|---|---|
| every **write** here | 1 school is live · 2 school is paying |
| every **read** here | none |

**No gate 4, though a cycle carries an `academicYear`.** It is in the body, and a cycle is created
before the period it covers.

---

# The endpoints

## 1. The cycle

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t40"></a>40 | [`POST /review-cycles`](#e40) | Open a review period with its criteria. |
| <a id="t45"></a>45 | [`PATCH /review-cycles/{id}`](#e45) | Fix a name, a date, a criterion — **only while `DRAFT`.** *(new)* |
| <a id="t41"></a>41 | [`POST /review-cycles/{id}/{transition}`](#e41) | `schedule` · `open` · `start-review` · `close`. **Five states, four transitions.** |
| <a id="t46"></a>46 | [`GET /review-cycles`](#e46) | The list, by year and status. *(new)* |

## 2. The reviews

| # | Method and endpoint | What this API is for |
|---|---|---|
| <a id="t42"></a>42 | [`POST /review-cycles/{id}/reviews`](#e42) | Submit one review. Anonymous ones carry a hash, not an id. |
| <a id="t43"></a>43 | [`GET /review-cycles/{id}/reviews`](#e43) | Aggregate or raw — **and which is a policy decision, not a parameter.** |
| <a id="t47"></a>47 | [`POST /reviews/{id}/acknowledge`](#e47) | The reviewee has read it. **The one write a reviewee makes.** *(new)* |

---

# Build order

| Phase | What it gives you | Endpoints |
|---|---|---|
| **5a** | A cycle exists and can be configured | 40, 46, 45 |
| **5b** | It has a lifecycle | 41 |
| **5c** | People can review and be reviewed | 42, 43, 47 |

**The whole package is phase 5.** Nothing outside `people` references a review, so it blocks
nothing.

**`#45` before `#41`.** A cycle is only editable while `DRAFT`, so the edit endpoint has to exist
before the one that moves it out of `DRAFT` — otherwise the first typo is permanent.

**`#43` before `#47`.** Acknowledging a review you cannot read is not a meaningful act.

---

# Things this package deliberately will not have

- **No `DELETE` on a cycle or a review.** A review is an opinion somebody recorded; deleting it
  rewrites what happened. A cycle closes, and [#41](#e41) is how.
- **No editing a submitted review.** `StaffReviewStatus` goes `DRAFT → SUBMITTED → ACKNOWLEDGED →
  FINALIZED` in one direction. A reviewer who changes their mind after submitting has said two
  things, and only one of them is recorded — which is the honest outcome.
- **No `?raw=true` on [#43](#e43).** Whether a reviewee may read individual anonymous scores is the
  school's policy, and a query parameter hands that decision to whoever writes the client. See
  [open item 2](#2-43-returns-two-different-things-and-the-caller-must-not-choose).
- **No cross-cycle comparison.** "Is Priya improving" spans cycles whose criteria differ by
  design — the embedding that protects history is exactly what makes the comparison invalid
  without a human deciding the criteria are equivalent.
- **No reviewer assignment.** Nothing here says *who should* review whom. That is a workflow with
  notifications and deadlines, and it wants its own model before it wants endpoints.

---

# To settle before building

## 1. "Anonymous" means two different things, and this design only delivers one

`StaffReview` omits `reviewerDocsId` for anonymous feedback and stores a keyed
`reviewerLookupHash` instead — enough to stop the same person submitting twice without recording
who they are.

**The hash space is the staff list.** A few hundred ids. Anyone holding the key and the collection
can hash every staff id and match — so the design delivers:

| | delivered? |
|---|---|
| anonymous **to the reviewee** | **yes** — they never see an id |
| anonymous **to a peer** | **yes** |
| anonymous **to the school** | **no** — an admin with database and key access can re-derive it |

**Those are different products**, and a school told "peer feedback is anonymous" will mean the
second one. Promising it and delivering the first is the kind of thing that ends badly and
publicly.

**It needs a decision, not a fix**, because the fix is expensive:

- **a) Say what it is.** Document that anonymity is against other staff, not against the system, and
  make the UI say so before somebody submits. Honest, cheap, and some schools will accept it.
- **b) Make it genuinely one-way.** A per-cycle key, destroyed when the cycle closes, so duplicate
  detection works while the cycle is open and nothing can re-derive afterwards. Real anonymity, and
  it needs the key-vault work [open item 1 of the domain plan](../README.md#1-nothing-can-encrypt-anything--this-blocks-three-documents) already blocks on.
- **c) Drop anonymity.** Signed reviews only. Some schools prefer this.

**Recommendation: (a) now, (b) when the key vault exists**, and under no circumstances ship a UI
that says "anonymous" without qualification while (a) is the truth.

## 2. `#43` returns two different things, and the caller must not choose

The same URL has to serve:

- **the reviewee**, who should see aggregate scores and perhaps comments, depending on the school
- **the reviewer**, who should see their own submission
- **HR**, who should see everything

**A `?raw=true` parameter is the wrong shape**, because it lets the client decide, and the client
is whichever page happens to be open. The decision belongs to the school's policy and the caller's
role.

**Until `identity` exists, [#43](#e43) cannot make it.** Three ways to proceed:

- **a) Return aggregate only**, always, and add the raw view when roles exist. Safe, and useless to
  HR for the moment.
- **b) Return raw, and rely on the endpoint not being exposed.** Fast, and exactly the assumption
  that produces incidents.
- **c) Build both shapes behind one flag now**, and wire the flag to a role later.

**Recommendation: (a).** The aggregate is the shape most callers need, it cannot leak an
identity, and adding a second shape later is cheaper than retracting one.

## 3. Criterion weights should sum to 100 — and this project has solved that twice already

`ReviewCriterion.weightPercent` exists, and nothing checks the set adds up.

**The identical problem, twice:** `AcademicTerm.weightPercent` inside a year, and `Exam.weightPercent`
inside a term. Both were settled on 2026-09-12, and the answer was not "always refuse":

| endpoint shape | rule |
|---|---|
| a **create** that only adds | refuse an **excess** — no sequence of creates needs to pass through >100 |
| a write that sees the **whole set** | require **exactly 100** |
| a **patch** of one item | **report** only — 20/80 → 30/70 passes through 110 |

**A cycle's criteria are written as a whole set**, by [#40](#e40) and [#45](#e45), so this is the
middle row: **exactly 100, per reviewer type.**

**Per reviewer type is the part to get right.** Manager criteria summing to 100 and peer criteria
summing to 100 are two independent checks — summing all criteria across types would demand 100
across five questionnaires, which is meaningless.

**Recommendation: reuse the shape, not the code.** `AcademicsHelper.validateWeightsTotal100` takes
terms; this needs a group-by first. The *rule* is settled and should not be re-litigated; the
implementation is a few lines in `ReviewHelper`.

---

# Where the code will live

```text
controllers/people/reviews/
├── README.md               <- this file
└── ReviewController.java   #40–#43, #45–#47

services/people/
├── ReviewService.java
└── helper/ReviewHelper.java   criterion weights, score validation, the aggregate

repositories/people/reviews/
├── ReviewCycleRepository.java
└── StaffReviewRepository.java   + Custom/Impl for #43's aggregate

dto/people/reviews/{request,response}/
```

**`ReviewHelper`, not `PeopleHelper`.** Criterion weights and score bounds share nothing with staff
or leave — the same reasoning that gave grading its own helper.

---

# Appendix — what each field can hold

## `staff_review_cycles` — [ReviewCycle](../../../models/people/reviews/ReviewCycle.java)

| Field | Type | What can be in it |
|---|---|---|
| `academicYear` | String, required | `AcademicYear.name`, never its id. |
| `cycleCode` | String, required | Unique with `schoolId + academicYear`. Given, not derived. **Never changes.** |
| `name` | String, required | Editable while `DRAFT`. |
| `startDate` · `endDate` | LocalDate, required | The window submissions are accepted in. |
| `status` | Enum, required | `DRAFT` · `SCHEDULED` · `OPEN` · `IN_REVIEW` · `CLOSED`. **Five states** — [#41](#e41). |
| `anonymousPeerFeedback` | Boolean, required | Defaults `false`. **Set at create and never changed** — flipping it mid-cycle would either expose reviewers who submitted expecting anonymity, or hide ones who did not. |
| `criteria` | List, embedded | Written whole. **Weights sum to 100 per reviewer type** — [open item 3](#3-criterion-weights-should-sum-to-100--and-this-project-has-solved-that-twice-already). |

**Index:** `school_review_year_cycle_code_uniq {schoolId, academicYear, cycleCode}` unique ·
`school_review_status_dates_idx {schoolId, status, startDate, endDate}`

## `staff_reviews` — [StaffReview](../../../models/people/reviews/StaffReview.java)

| Field | Type | What can be in it |
|---|---|---|
| `reviewCycleDocsId` · `reviewedStaffDocsId` | String, required | Which cycle, about whom. |
| `reviewerType` | Enum, required | `SELF` · `MANAGER` · `PEER` · `STUDENT` · `PARENT`. **Decides which criteria apply.** |
| `reviewerDocsId` | String | **Omitted when anonymous.** |
| `reviewerLookupHash` | String | **Keyed.** Present when anonymous, and part of the uniqueness key either way. |
| `status` | Enum, required | `DRAFT` · `SUBMITTED` · `ACKNOWLEDGED` · `FINALIZED`. One direction only. |
| `criterionScores` | Map | `criterionCode → score`. **Validated against the criteria for this reviewer type**, and against each one's `maximumScore`. |
| `finalScore` | BigDecimal | **Computed from the scores and weights**, never sent. |
| `reviewComments` | String | |
| `submittedAt` | Instant | |

**Index:** `school_review_cycle_staff_reviewer_uniq {schoolId, reviewCycleDocsId, reviewedStaffDocsId, reviewerType, reviewerLookupHash}`
unique — **which is what stops one person reviewing the same colleague twice**, anonymously or not.

## The refusal codes this package introduces

| Code | Status | When |
|---|---|---|
| `REVIEW_CYCLE_NOT_FOUND` · `REVIEW_NOT_FOUND` | 404 | not this school's |
| `CYCLE_CODE_TAKEN` | 409 | that year already has it |
| `CYCLE_NOT_EDITABLE` | 409 | [#45](#e45) — anything past `DRAFT` |
| `INVALID_CYCLE_TRANSITION` | 409 | [#41](#e41) — naming the states it *can* move to |
| `CYCLE_NOT_OPEN` | 409 | [#42](#e42) — submitting outside the window |
| `CRITERION_WEIGHTS_INVALID` | 409 | weights not summing to 100 for a reviewer type |
| `CRITERION_CODE_TAKEN` | 409 | two criteria sharing a code |
| `UNKNOWN_CRITERION` | 400 | [#42](#e42) — a score for a criterion this reviewer type has none of |
| `SCORE_OUT_OF_RANGE` | 400 | above the criterion's `maximumScore` |
| `REVIEW_ALREADY_SUBMITTED` | 409 | the uniqueness index, as a readable message |
| `REVIEW_NOT_SUBMITTED` | 409 | [#47](#e47) — acknowledging a draft |
| `INVALID_CYCLE_RANGE` | 400 | `endDate` before `startDate` |
| `NOTHING_TO_UPDATE` | 400 | reuses core's code |

---

# What every API touches, field by field

## The cycle · 40, 45, 41, 46

<a id="e40"></a>
**[40](#t40) · `POST /review-cycles`**

- *insert*: `academicYear`, `cycleCode`, `name`, `startDate`, `endDate`, `anonymousPeerFeedback`, `criteria`, `status` = `DRAFT`
- **Criteria come with the cycle, not after it.** A cycle with no criteria is not a cycle — the same reasoning that puts bands inside [#1](../../academics/grading/README.md#e1) of the grading module rather than behind a second call.
- **Weights are checked per reviewer type, each summing to 100.** See [open item 3](#3-criterion-weights-should-sum-to-100--and-this-project-has-solved-that-twice-already).
- **`status` is not accepted.** It starts `DRAFT`; moving it is [#41](#e41), the event-endpoint shape every lifecycle in this project uses.
- **`anonymousPeerFeedback` is set here and never again** — see the appendix. And a school setting it should be told what it does and does not guarantee: [open item 1](#1-anonymous-means-two-different-things-and-this-design-only-delivers-one).

<a id="e45"></a>
**[45](#t45) · `PATCH /review-cycles/{id}`** *(new)*

- *updates*: `name`, `startDate`, `endDate`, `criteria` — **only while `DRAFT`**
- **`409 CYCLE_NOT_EDITABLE` from `SCHEDULED` onwards.** Once a cycle is announced, its criteria are what people were told they would be scored on; changing them afterwards rewrites the question after the answers.
- **The criteria set is replaced whole**, never patched per-criterion — adding one changes every weight, and the check that catches it reads all of them. The same rule [#3](../../academics/grading/README.md#e3) of the grading module applies to bands.
- **Never `cycleCode`, never `academicYear`, never `anonymousPeerFeedback`.**

<a id="e41"></a>
**[41](#t41) · `POST /review-cycles/{id}/schedule` · `/open` · `/start-review` · `/close`**

- *updates*: `status`
- **Four endpoints, one per transition, because the states are not a toggle.** The domain plan wrote this as an open/close pair, which is wrong for five states:

```text
DRAFT ──schedule──▶ SCHEDULED ──open──▶ OPEN ──start-review──▶ IN_REVIEW ──close──▶ CLOSED
  │                                                                                    ▲
  └────────────────────────────── close ───────────────────────────────────────────────┘
```

- **`DRAFT` may close directly** — abandoning a cycle nobody used is a real act, and forcing it through three transitions to get there would be theatre.
- **`409 INVALID_CYCLE_TRANSITION` names which states this one can reach**, because "invalid" alone leaves a caller guessing at a five-state machine.
- **Idempotent within a state**: asking for the state it is already in is a `200` saying so, the shape every lifecycle endpoint in this project uses.
- **`OPEN` is the only state [#42](#e42) accepts submissions in.** `IN_REVIEW` means the window closed and the school is reading — which is why it is a separate state rather than part of `CLOSED`.

<a id="e46"></a>
**[46](#t46) · `GET /review-cycles`** *(new)* — `?academicYear=`, `?status=`. Served by `school_review_status_dates_idx`. Not paged; a school runs one or two cycles a year. **Criteria are returned** — a cycle without them is not readable, and there are a handful.

## The reviews · 42, 43, 47

<a id="e42"></a>
**[42](#t42) · `POST /review-cycles/{id}/reviews`**

- [`staff_review_cycles`](../../../models/people/reviews/ReviewCycle.java) — *reads*: the cycle, which must be `OPEN`, and its criteria
- [`staff_reviews`](../../../models/people/reviews/StaffReview.java) — *insert*: one review, `status` = `SUBMITTED`, `finalScore` **computed**
- **Scores are validated against the criteria for THIS reviewer type**, not all of them. A peer scoring a manager-only criterion is `400 UNKNOWN_CRITERION`, and a missing one is refused too: a partial questionnaire produces a `finalScore` that is not comparable with a complete one.
- **`finalScore` is computed, never sent** — `Σ (score / maximumScore × weightPercent)`. A caller-supplied final score beside per-criterion scores is two numbers that can disagree, and nothing would say which is right.
- **One submission per reviewer per reviewee per type**, by the unique index. `409 REVIEW_ALREADY_SUBMITTED` — **and when the cycle is anonymous, that refusal must not confirm who the caller is** by phrasing it personally.
- **Anonymous submissions carry `reviewerLookupHash` and omit `reviewerDocsId`.** Never both — storing the id beside the hash makes the hash decorative.
- **`409 CYCLE_NOT_OPEN`** outside the window, naming the cycle's current state.

<a id="e43"></a>
**[43](#t43) · `GET /review-cycles/{id}/reviews`**

- *reads*: reviews for the cycle, optionally narrowed to one reviewee
- **Aggregate only, for now** — per criterion, per reviewer type, with counts. **Not the raw submissions**, because whether a reviewee may read individual anonymous scores is the school's policy and this endpoint cannot ask anyone. See [open item 2](#2-43-returns-two-different-things-and-the-caller-must-not-choose).
- **A count below a threshold suppresses the breakdown.** Three anonymous peer scores where one is an outlier is not anonymous in any useful sense, and the aggregate should say "too few responses" rather than average them.
- **Aggregates never cross reviewer types**, because criteria differ by type — averaging them is averaging answers to different questions.
- **No gates**, and this is the endpoint where that matters most in the package.

<a id="e47"></a>
**[47](#t47) · `POST /reviews/{id}/acknowledge`** *(new)*

- *updates*: `status` `SUBMITTED` → `ACKNOWLEDGED`
- **The one write a reviewee makes**, and the reason the status enum has four values rather than two. It records that the person was shown their review, which is the thing an HR process actually needs evidence of.
- **`409 REVIEW_NOT_SUBMITTED`** for a draft. **Idempotent** once acknowledged.
- **`FINALIZED` is not written by any endpoint in this plan.** It belongs to whatever closes the HR process — a sign-off, a pay decision — and inventing an endpoint for it before that process exists would be guessing at who presses it.
