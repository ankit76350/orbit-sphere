# postman — request collection for the Orbit Sphere API

`Orbit Sphere — API.postman_collection.json` — **Import → File** in Postman.

## The convention: one request per endpoint

Not one request per test case. **One request per API endpoint**, with every test case for that
endpoint living inside the request body as a numbered, commented-out block.

To run a case: swap the active body for the block you want, and Send. Postman strips `//`
comments from a raw JSON body before sending, so the comments cost nothing.

> If your Postman version does not strip them, the request fails with `MALFORMED_REQUEST`.
> Delete the comment block for that one send.

## How to write a new request

### 1. Name it after the endpoint, not the case

`Create School`, `Activate School`, `Create Academic Year`. The method and path live in the
request; the name should read as the operation.

### 2. Active body = the ordinary happy path

Valid JSON, no comments above it, so the request works the moment somebody hits Send without
reading anything.

### 3. Below it, the test-case block

```
// ===========================================================================
//  TEST CASES  —  POST /platform/schools
//  Swap the body above for any block below, then Send.
//  Postman strips these // comments before sending.
// ===========================================================================
//
// ---------------------------------------------------------------------------
// 02  MINIMUM PAYLOAD — only the 6 required fields          -> 201 Created
//     Everything omitted is stored as null, not "".
// {
//   "schoolName": "Minimum Fields School",
//   ...
// }
// ---------------------------------------------------------------------------
```

Every case carries:

| Part | Why |
|---|---|
| **Two-digit number** | so it can be referred to — "case 07 fails" |
| **SHORT NAME IN CAPS** | scannable down the left edge |
| **`-> STATUS`** | the expected result, on the same line |
| **`IN:` / `OUT:`** where useful | the input that triggers it, the shape that comes back |
| **A one-line why** | especially for a 409, which people mistake for a bug |
| **The payload, commented** | ready to paste over the active body |

Case `01` is the active body, so its block documents rather than repeats it.

### 4. Order the cases: success first, then failures

`01` full · `02` minimum · `03` variants · then normalisation · then every error, grouped by
the field they concern.

### 5. Repeat the contract in the request description

The **Description** field is markdown and renders in Postman's docs pane. Put the required and
optional field tables there, plus the fields the endpoint **refuses** if sent. The body block is
for running; the description is for understanding.

### 6. Write tests for the active body

The tests assert case `01`. Say so in a comment at the top of the test script, because they will
be wrong for whichever case somebody swaps in — that is expected, not a bug.

Save anything later cases need:

```js
pm.collectionVariables.set('schoolId', body.schoolId);
pm.collectionVariables.set('createdSubdomain', body.subdomain);
```

### 7. Make unique values unique per run

`subdomain` is **globally unique**, so a fixed value works once and returns `409` forever after.
Use `{{$timestamp}}`:

```json
"subdomain": "orbit-astra-{{$timestamp}}"
```

Fixed values are right where the case is *meant* to fail every time — `api`, `-bad-`.

## Running it

```bash
cd backend && ./mvnw spring-boot:run     # ~15s, port 3456
```

Then **run `Session / Sign in` first**, and only then Send or **Run collection**.

### Sign in is not optional any more — 2026-09-21

The backend used to work out which school a request was for from the `X-School-Subdomain` header.
**It no longer reads that header at all.** The school now comes from the `idtoken` cookie, which
`POST /local-user` sets.

So every request under `/schools/current` answers `400 TENANT_NOT_RESOLVED` until
`Session / Sign in` has been run once. Postman keeps the cookie in its jar for the host, so one run
covers the whole collection.

The `X-School-Subdomain` header is still on most requests. It is simply ignored now — harmless, and
left in place so it is obvious it no longer does anything. `Session / Sign in` case 03 clears the
cookie, which is how the refusal is tested.

## Variables

| Variable | Set by | Used by |
|---|---|---|
| `baseUrl` | you — defaults to `http://localhost:3456` | everything |
| `schoolId` | a successful create | endpoints taking `{id}` |
| `createdSubdomain` | a successful create, **and Change Subdomain** | case 05, duplicate; the `X-School-Subdomain` header on every school-surface request |
| `academicYearName` | a successful Create Academic Year | every `/academic-years/{name}` URL, holidays included |
| `planCode` | a successful Create Plan Draft | every `/platform/plans/{code}` URL |
| `planVersion` | a successful Create Plan Draft | every `/versions/{version}` URL |
| `subscriptionNo` | a successful Create Subscription | `Get Subscription History` |
| `schoolClassId` | a successful **Create Class** | every `/classes/{id}` URL, sections included |
| `sectionNo` | set by hand — a section has no id to capture | `Get Section`. Defaults to `A`; change it to read another. |
| `admissionCycleDocsId` | a successful **Create Admission Cycle** | every `/admission-cycles/{id}` URL, when #2 to #7 are built |

## Folders mirror `controllers/`

`Core / School — platform`, `Core / School — profile`, `Core / Academic Year`,
`Plans / Plan catalogue`, `Plans / Subscriptions`, `Plans / Subscription — the school's own view`
and `Academics / Terms` and `Academics / Classes` — two controllers, because a term and a
class are independent documents and the `{year}` prefix is all they share. One folder per
controller, so the collection and the code stay
findable from each other.

**`CRM` arrived 2026-09-21** and is now **twelve of thirty-four**, in two folders. `Admission
Cycles` holds six — #1 opens a year for admissions, #2 corrects one, #3 moves it DRAFT → SCHEDULED
→ OPEN → CLOSED → COMPLETED, #4 sets its seats, #5 lists the rounds and #6 opens one in full.
`Applications` holds six — #17 starts a form against an open cycle, #19 submits it, #20 decides
it, #24 reads the pipeline back, #25 opens one form in full and #26 puts it on a reviewer's desk.
The plan for the other twenty-two is in that package's README.

**#5 and #24 are where the sort allowlist is worth poking at.** `?sort=schoolId` and `?sort=notes`
on #5, `?sort=dateOfBirth` on #24, are all `400 INVALID_SORT_FIELD` — a security control rather
than a convenience: ordering is a read, so sorting by a field and walking the pages tells you its
values even when nothing displays them. On #24 that field is a child's date of birth, which is the
clearest case of it: nothing on the screen shows an age, and sorting would hand over every one.

**#25 is where the empty arrays are the interesting part.** `reviews` and `offers` come back `[]`
for every application in the database, because #26 and #29 are what create them and neither is
built. That is the endpoint working, not a gap — and case 02 says so, because an empty array with
no explanation reads as a bug.

**#20 is the one where the refusals are the product.** Case 07 — approve a form and then try to
reject it — is refused on purpose, and the message sends you to #29 and #31 rather than just saying
no. Case 05 is the other: `RESUME_REVIEW` is legal from exactly one status, and trying it anywhere
else is how you see that.

**#26 needs a staff id, and the case worth running uses SOMEBODY ELSE'S.** A ghost reviewer id is
refused whether or not the lookup is tenant-scoped, so it proves nothing; a real staff id belonging
to another school is the only thing that does. Case 07 is that one.

**#19 is the one to run twice.** The second send is `409 INVALID_APPLICATION_TRANSITION` and the
message says the form is *already in* rather than blaming the round — which is the ordering of the
checks showing itself. Case 08 is the other one worth running: take the applied class out of the
seat table and submit anyway, and it goes through, because capacity is decided when a seat is
offered rather than when the family sends the form.

**#17 and #3 are a pair, and are best tested in that order.** #17 refuses a form unless the cycle
is OPEN *and* now is inside its published application window, so `APPLICATIONS_NOT_OPEN_YET` and
`APPLICATIONS_CLOSED` need a cycle whose dates you set with #1 or #2. #3 is what makes the cycle
OPEN in the first place, and it fills a date the school left empty as it moves — so a round moved
to OPEN with no `applicationOpenAt` comes back with one.

**It is the one module that does NOT need the year to be the running one**, and `Create Admission
Cycle` case 02 is there to show it: the same year that a `Create Class` refuses with
`ACADEMIC_YEAR_NOT_RUNNING` is accepted here. A school sets up next year's admissions in the middle
of this one, so refusing that would refuse the module's whole purpose.

**`Session` arrived the same day** and has to be run first. See above.

**`Academics` arrived 2026-09-11** with the first eleven endpoints of
`controllers/academics/structure`. A class is addressed by its **MongoDB document id** — twelve
other documents store it as `classDocsId` — and a section by its `sectionNo`, which is all a
section has, being embedded in its class. Run **Create Class** first: it saves `schoolClassId`.

## Coverage

**115 requests.** Counted 2026-09-22.

**The old claim here said 94 and that every endpoint was covered.** It had gone stale by eight
before anybody noticed — the count is the kind of claim that rots, which is why it now carries the
date it was taken. Recount with:

```bash
python3 -c "import json;d=json.load(open('postman/Orbit Sphere — API.postman_collection.json'));
n=0
def w(i):
    global n
    for x in i:
        w(x['item']) if 'item' in x else globals().__setitem__('n',n+1)
w(d['item']);print(n)"
```

**The two-way agreement with `new-api-tester/src/config/endpoints.js` holds again as of
2026-09-22**, at 115 each — every CRM endpoint now has a screen. It is not a rule, though, and it
broke once already: a request lands here the day its endpoint is built and the screen can follow
later. Everything the tester drives is present here; the reverse is the part that drifts.

**People is 16 of 52**, in two folders. `Department` holds eight — create a department and a
position, edit either, list the departments as a flat page or a tree, read one department in full,
list the positions **with their filled headcounts**, and read one position with **everybody
currently in it**. `Staff` holds eight — create a person, list them, edit one, read one in full,
employ or promote them, correct an employment record, change its status with a reason, and read
one person's whole employment history. The other thirty-six are specified in
`backend/src/main/java/com/orbitastra/backend/controllers/people/README.md` and are not built.

**The two position reads arrived 2026-09-16.** `GET /positions` is #15, which could not be written
until something wrote an employment record — its whole point is a count of them.
`GET /positions/{id}` is **#53**, numbered on the end the way #52 was: #15 answers "3 of 5 filled"
and the next question is always *which three*.

**Academics is 23 of 37**, now across four folders — `Terms`, `Classes`, `Grading` and `Timetable` — add a term and list the year's terms; and create a class, edit it,
add a section, add a subject, edit one, list the year's classes, read one class in full, read its
sections, read one section, and read the subjects one section studies. **Timetable is ten** —
#1 writes a day or a range of them, #10 lists a year's days as counts, **#7 opens one of them in
full**, addressed by the **date** rather than a document id because a caller always knows the date
and never knows the id, **#2 replaces a whole day**, and **#6 builds one day from another**, which
is what a school actually does: Monday is typed once and the rest of the week is copied from it.
`Create Timetable` captures `timetableDate`, `timetableVersion` and `timetableEntryId`; `Get
Timetable` refreshes those and records the source day's ids, which `Copy Timetable` uses to prove
**not one id crosses** — a copied period is a different period on a different date. `Replace
Timetable` needs the version, which is **required** on that write, and a stale one is
`409 CONCURRENT_MODIFICATION`. Set `timetableCopyTarget` to an empty working day before running
`Copy Timetable`. **#8 and #9** are the two reads a parent's app and a teacher's app actually
make — one section's day and one person's, both **earliest first**, which is a real order there
where it is a guess in #7: a section and a person can each only be in one place at a time. `Get
Timetable` also records the first `teacherDocsId` it sees as `staffDocsId`, which is what `Get
Teacher Day` asks about. **#3, #4 and #5 are the single-entry writes** — add one period with a
`$push`, correct one with a targeted `$set` through an array filter (**the substitution**), remove
one with a `$pull`. They act on one embedded period and never re-save the day, which is the whole
of this module's persistence risk. `Add Timetable Entry` captures the `timetableEntryId` that
`Patch` and `Remove` then act on. Two behaviours are worth reading the notes for: **a no-op patch is
a 200, not a 404** (the write counts documents *matched*, not modified), and **a second removal is a
404, not another 204**. The other two of that folder's twelve are specified in
`backend/src/main/java/com/orbitastra/backend/controllers/academics/timetable/README.md`.
The other twenty-five are
specified in
`backend/src/main/java/com/orbitastra/backend/controllers/academics/structure/README.md` and are
not built.

**Core is 20 of 27 writes and 9 of 11 reads.** Plus the two `DELETE`s that were never in the
plan, that is 31 requests. The rest are specified in
`backend/src/main/java/com/orbitastra/backend/controllers/core/README.md` and are not built —
a collection full of 404s is worse than a short honest one.

The two reads still missing are **G3** (is this subdomain free?) and **G11** (the calendar as a
file).

**Plans is 22 of 71 — the entire plan catalogue except versioning (#5), the subscription
lifecycle, and five reads.** Create a draft, edit it, set its features, publish it, list it
publicly, retire it; and read it three ways. On the subscription side: create, edit, change plan,
renew, suspend, resume, cancel, plus one school's subscriptions, one subscription's history, and
**every school's subscriptions in one list** — the only request in the collection that names no
school at all. The whole module plan lives in
`backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md`.

**None of the seven missing core writes is "next".** Six are deferred by decision — #12 until
something is actually encrypted, #13 to #17 (offboarding and deletion) until they are wanted —
and #28 was always optional. One consequence to know while testing: **a school you create cannot
be closed or deleted.** `SUSPENDED` is as far as it goes, so test schools accumulate.

The plan is 27 rather than 28 because #11 `account-holder` was dropped on 2026-08-31 and folded
into #6 — it is a plain label that nothing links to an account, so its own platform endpoint was
ceremony.

The request count is 22 rather than 20 because two of the calendar endpoints were not in the
original plan: the single `DELETE /holidays/{date}` and the bulk `DELETE /holidays?type=`. Both are
undo for endpoints that create in bulk, and an API that can generate 52 rows in one call and
cannot remove them is not finished.

### Running the holiday requests in order

`Core / Academic Year` is the one folder where order matters. Create School → Complete
Provisioning → Activate → Create Academic Year, then the calendar requests. The holiday requests
assume specific dates exist:

| Request | Assumes |
|---|---|
| Replace Holiday Calendar | nothing — it is the reset |
| Add Holiday | nothing |
| Update Holiday | `2026-11-08` exists and holds a `FESTIVAL` |
| Remove Holiday | `2026-11-08` exists and holds a `WEEKLY_OFF` |
| Generate Weekly Off | nothing |
| Remove Holidays By Type | Generate Weekly Off has run |
| Unlock Results | Lock Results has run — otherwise it is an idempotent no-op |

Run **Replace Holiday Calendar** first to put the calendar in the state the rest expect.

## A note on all those 409s

Five of the ten cases expect `409`, not `400`, and that is deliberate across this whole API.

- **400** — *this is not a well-formed request*: nothing sent, a date that is not a date.
- **409** — *the request is fine and the answer is still no*: the subdomain is spelled correctly
  and taken; the time zone is a reasonable guess that does not exist.

Told `400` for a taken subdomain, a caller goes hunting their JSON for a mistake that is not
there.

## A note on the holiday requests

One date can be closed for more than one reason — a Sunday that is also Diwali — so the calendar
stores an **array of reasons per date**, and the requests are shaped around that:

- **Sending two rows with the same date is not an error.** `PUT` groups them into one closed day
  with two reasons. Sending the same *type* twice for one date is the error.
- **`POST` on an already-closed date is not a conflict.** The reason joins the day. Only the same
  type twice is refused.
- **`PATCH` and the single `DELETE` take `?type=`** to say which reason they mean. Optional when
  the day has one, required when it has more.
- **Two counts come back everywhere**: `closedDayCount` (days the school is shut) and
  `eventCount` (reasons recorded). They differ wherever a day carries more than one reason, and
  `countsByType` counts reasons — so a festival that falls on a Sunday is still a festival.

## Subscriptions: #13 sells, #14 edits, #16 moves the plan, #17 starts the next period

`POST /platform/schools/{id}/subscriptions` is what makes a school a paying customer. Before it,
**Activate School** always answered `"subscriptionStatus": "NONE"` with a note saying activation
was allowed anyway because nothing could create one. Create a subscription first and the same
call reports `ACTIVE`.

Order: **Create School → Complete Provisioning → (a published plan) → Create Subscription.**
**Activate School is no longer a step** — a school still `PROVISIONING` with its provisioning
finished is waiting only on a subscription, so Create Subscription activates it and says so in
`nextStep`. Run it against a school whose provisioning is *not* finished and the subscription is
still created, the school stays `PROVISIONING`, and `nextStep` names what is missing.

- **Three fields is the ordinary request** — the plan's code and version, and
  `currentPeriodStart`. Price, currency and cycle come from the plan; the period end is derived
  from the cadence. **The start used to default to today and no longer does**: it is the anchor the
  end is measured from, and on a yearly sale it fixes which day the school is billed on for as long
  as it stays, so it is somebody's decision rather than a convenience. Omitting it is
  `400 VALIDATION_FAILED`.
- **On #14 a cadence names the dates it needs**: sending `billingCycle` requires
  `currentPeriodStart` (`400 PERIOD_START_REQUIRED`), and a `CUSTOM` cadence requires
  `currentPeriodEnd` as well. Every other edit leaves both alone.
- **Only a `CUSTOM` cadence takes `currentPeriodEnd` at all.** On `MONTHLY`, `QUARTERLY`,
  `HALF_YEARLY` and `YEARLY` the field is refused — `400 BILLING_PERIOD_END_NOT_ALLOWED` — because
  those four *are* their length. To move one of their period ends, move `currentPeriodStart`; the
  end follows it. Applies to #13, #14, #16 and #17 alike.
- **The cycle can be negotiated too.** `billingCycle` is optional and absent takes the plan's, so
  the same plan can be sold on a different cadence — stored on the *subscription*, leaving the plan
  and every other school on it alone. Whichever cycle applies is what decides the period dates
  **and** whether `currentPeriodEnd` may be sent at all: a `YEARLY` plan sold `CUSTOM` needs a
  date, and a `CUSTOM` plan sold `MONTHLY` refuses one.
- **One subscription per school.** A second is a `409`. Nothing cancels one or changes its plan
  yet, so a test school stays on its first plan.
- **Numbering is per school**, so every school's first subscription is `SUB/2026/09/000001`.
- **A billing period starts today or later**, on #13 and #14 both: a past `currentPeriodStart` is
  `400 PERIOD_START_IN_PAST`. Nothing invoices a period, so a backdated start would record a school
  as paying for time nothing could charge it for. Today counts, and "today" is the **school's** day
  — an Asia/Kolkata school's midnight is `18:30Z` the previous day and that instant is accepted, so
  a UTC comparison would wrongly reject it. On #14 only a start being *set* is checked; a running
  subscription's stored start is in the past by definition.

**There is no Activate Subscription request any more.** #15 was built and then withdrawn on
2026-09-07: whether a subscription starts as `TRIAL` or `ACTIVE` is decided when it is sold, from
`"trial": true` on Create Subscription, and a trial that later starts paying is either a status
edit through #14 or — when the school is buying a different plan from the one it tried — a new
subscription. Three ways to say "this school is paying now" was two too many.

**Edit Subscription (#14)** edits when a subscription runs, what state it is in, and how much of
the product it may use: `status`, `billingCycle`, both period dates, `autoRenew` and the two
capacity overrides. It replaced extend-trial — pushing a trial's end date out is
`currentPeriodEnd` on it — took in #23 and #24, and absorbed what #15 used to do:
`{"status": "ACTIVE", "reason": …}` is how a trial becomes a paying subscription.

- **Only a `TRIAL` or `ACTIVE` subscription may be edited.** `PAST_DUE`, `SUSPENDED`, `CANCELLED`
  and `EXPIRED` are all `409 SUBSCRIPTION_NOT_EDITABLE`: each was somebody's decision or a date
  arriving, and each has an endpoint that owns the way out — #20 resume, #17 renew, #16 change
  plan. The refusal names it, so it is a signpost rather than a dead end, and nothing is written.
  **It stays a one-way door the other way**: from `TRIAL` or `ACTIVE` this can set any of the six,
  which is how `PAST_DUE` and `EXPIRED` are set by hand while no job exists.
- **Nothing about the money.** The price and currency stay on #25, the billing customer on #26,
  the plan on #16. Which means **nothing built can change a price** after Create Subscription set
  it: #25 is not built, and #14 will not do it.
- **A `reason` is required.** It is the only field that must be sent, and it is stored on the
  subscription as `reasonForChanges` as well as on the history row — every field this endpoint
  edits is something a school is paying for, so an unexplained change is one nobody can answer
  for later. Blank counts as missing.
- **The cadence decides the period**, as on #13. Changing `billingCycle` recalculates
  `currentPeriodEnd` from the start plus the new cycle's days, and moving the start does the same;
  moving to `CUSTOM` needs `currentPeriodEnd` on the same request, because CUSTOM has no length and
  the date on record belongs to the cadence being left. A derived change shows up in the
  changed-field list, so it is never silent. This **reverses** what the endpoint used to do:
  leaving the old end in place billed a school for a year while the document said it paid monthly.
- **On a fixed cadence the end is only ever derived, never sent.** `currentPeriodEnd` is refused
  there, read against the cadence the subscription will be on **after** the edit — so
  (`CUSTOM` → `MONTHLY` + an end date) is one refused request, not an accepted one. Which makes
  the rule one sentence: the end moves when what it is measured from moves, and at no other time.
  An edit to the price or the capacity leaves the period exactly where it was.
- **Absent means unchanged**, and every field is flat. The two overrides take **`0` to mean
  "remove it"** — Jackson cannot tell an omitted field from an explicit `null`, so zero is what
  says "use the plan's own limit". Negative is a `400`. The period dates are `@NotNull` on the
  model and cannot be cleared at all, which is what lets a trial's end date move on its own.
- **Nothing changed is a 200 that says so**, with no history row and no new reason stored. An
  *empty* body is a `400 VALIDATION_FAILED` naming `reason`; `NO_CHANGES_REQUESTED` is what you
  get when a reason *was* given and no editable field was.
- **One history row per edit**, whatever moved, with the field list and your reason on it.
  `TERMS_CHANGED` was added to the enum for the case where the status did not move; `PLAN_CHANGED`
  is never written here, because this endpoint cannot move the plan.

**Its URL says `current`, not a subscription number.** A number is `SUB/2026/09/000001`; the
slashes end the path segment, and writing them as `%2F` gets `400 Invalid URI: [The encoded slash
character is not allowed]` from Tomcat before Spring routes anything. A school has exactly one
current subscription, so `current` names it without ambiguity.

**Get Subscription (#27)** reads it all back: the plan and its **features listed rather than
counted**, the price paid against the plan's list price, the status, and when the period ends.
Its URL is **singular** — `/subscription` is the one they are on, `/subscriptions` is the
collection you post to.

It works out three things so no caller has to: `daysRemaining`, `planRetired`, and `periodEnded`
— the period's end has passed while the status still says the school is paying. That last one is
real today rather than hypothetical: nothing marks a subscription expired on its own, and #17
starts the next period only when somebody calls it, which nothing does on a schedule. So a period
just lapses and the status stays put, and a screen trusting `status` alone would show a school as
paying months after it ran out.

**Change Plan (#16)** moves a school onto a different plan, or a newer version of its own. It is
**immediate** — there is no timing field, because a subscription holds one plan rather than a
current and a pending one — and it **writes two rows**: the row being left is closed
(`current` = false, its period trimmed to the day of the change) and a new one opens on the new
plan's terms with a `subscriptionNo` of its own.

- **`reason` and `currentPeriodStart` are required**, and the price and both ceilings come from the
  **new** plan unless the request names them. The start has to be today or later, and it decides
  two dates: the anchor the new period's end is measured from, and the instant the row being left
  stops serving — so the two periods meet. A future start moves the *period*, not the plan pointer.
- **The cadence can be negotiated here too.** `billingCycle` is optional and absent takes the new
  plan's; whichever applies decides the period and whether `currentPeriodEnd` is required. A negotiated ceiling does **not** carry across: it was agreed against a
  particular plan, so moving to a different one means the terms are agreed again.
- **No status is refused, and the new row is always `ACTIVE`.** A plan change is somebody buying
  this school a plan, so the row it lands on has to be usable: `TRIAL` converts this way (which is
  why #15 was withdrawn), and `CANCELLED`/`EXPIRED` is how a school comes back — that used to be a
  `409`. Only two things still depend on whether the old row had *finished*: `autoRenew` goes back
  on, and the closed row's dates are left alone rather than trimmed, because it really did stop
  then.
- **It takes the school ACTIVE.** `PROVISIONING`, `ACTIVE` and `SUSPENDED` schools may change plan
  and all three come out `ACTIVE` — this is the one endpoint that **un-suspends**, on the argument
  that a school moving onto a new plan has sorted out whatever the suspension was for. The four
  wind-down states are `409 SCHOOL_NOT_PLAN_CHANGEABLE`.
- **No money moves**, and nothing is asked about it.

**Renew Subscription (#17)** starts the next billing period. Normally the nightly job would call
it; an operator calls it by hand when something went wrong. **Nothing calls it on a schedule yet.**

- **The ordinary renewal has no body at all.** The plan, version, price, currency, both ceilings,
  the cycle,
  `autoRenew` and the billing customer reference all carry across **untouched** — negotiated ones
  included, because nobody agreed to renegotiate anything by renewing. That is the opposite of
  #16, and it is why there is nothing to send: anything you could pass would make it a change.
- **Two rows again**, but the closed one keeps its dates: it ran its full course, where #16 trims
  the row it closes. The new period starts exactly where the old one ended, so no day is unbilled
  and none is billed twice.
- **Each call advances exactly one period.** A subscription three cycles behind needs three calls,
  and each row is a real record of a real period rather than one row covering the gap. The chain
  stops itself with `409 PERIOD_NOT_ENDED` once the current period reaches the future.
- **`ACTIVE`, `PAST_DUE` and `EXPIRED` renew, and all three come out `ACTIVE`.** `EXPIRED` is the
  case it exists to repair. A `PAST_DUE` renewal does **not** settle the previous period — nothing
  here takes a payment, and the `note` says so.
- **`autoRenew` is still read by nothing.** #17 does not consult it: nothing calls this endpoint
  on a schedule, so a renewal is always an operator deciding to renew *this* school now, and
  refusing that over a flag would just mean editing the flag first to get past the endpoint. It is
  carried onto the new row, and #33 still tells a school its subscription does not renew
  automatically. `TRIAL`, `SUSPENDED` and `CANCELLED` are `409 SUBSCRIPTION_NOT_RENEWABLE`.
- **The plan has to still be current.** A renewal re-commits the school to the *same* plan, so a
  plan that is retired, back to `DRAFT`, or outside its effective window is
  `409 PLAN_NOT_RENEWABLE`, and the message names the change-plan endpoint — the school is already
  on this plan, so the only fix is moving it to one that is current. A private plan
  (`publiclyAvailable: false`) renews like any other: that is a negotiated quote, not an invalid
  state. This is deliberately *not* the sale-time check, whose advice ("publish it first") is the
  wrong answer for a school already on the plan.
- **A `CUSTOM` cycle is asked for a date, not refused.** It has no length, so there is nothing to
  derive — which is the only reason this endpoint has a request body at all. Send
  `currentPeriodEnd`; without it, `400 BILLING_PERIOD_END_REQUIRED`. The date is never guessed: a
  fallback would put a date nobody signed off into a billing record. On the four fixed cycles the
  field is **refused** (`400 BILLING_PERIOD_END_NOT_ALLOWED`) rather than being an override, so
  omitting the body is not merely the ordinary call — it is the only one.
- **No invoice, no money.** `subscription_invoices` has no repository and no writer anywhere, and
  `subTotal`/`taxAmount`/`dueDate` are commercial decisions rather than derivations from a plan's
  price. So #17 moves the billing period and records the renewal; it does not charge for it.
- **It does not touch the school's own status**, unlike #16: a renewal continues an arrangement
  rather than starting one.

**Suspend (#19) and Resume (#20)** are the non-payment pair, and both require a `reason`.

- **Suspend moves two documents**, because one would stop nothing: `school_subscriptions.status`
  goes `SUSPENDED`, which turns every feature off through #34, and `schools.status` goes
  `SUSPENDED` too, which is what blocks the tenant — a school-surface write then answers
  `409 SCHOOL_NOT_EDITABLE`. Worth proving both ways round: `200` before, `409` while suspended,
  `200` again after resuming.
- **`ACTIVE` and `PAST_DUE` only.** A trial is refused — there is no unpaid bill behind one — and
  that refusal is what lets **Resume** go straight back to `ACTIVE` without looking up what the
  status used to be. `CANCELLED` and `EXPIRED` ended rather than paused.
- **This is not `{"status": "SUSPENDED"}` on Edit Subscription.** That moves the field and nothing
  else: no school status, no `SUSPENDED` history event, none of the refusals. Use it to correct a
  record, not to cut a school off.
- **Neither writes a second row**, unlike Change Plan and Renew — the same subscription is paused
  and unpaused.
- **The period is not paused or extended.** A school suspended for three weeks comes back to the
  same `currentPeriodEnd`, having paid for time it could not use. Crediting that is a money
  decision nothing here can make; Edit Subscription is where a date moves if a credit was agreed.
- **What suspending does NOT stop:** live sessions and scheduled jobs, because neither exists yet.
  Somebody already signed in is refused at their next request rather than thrown out. The `note`
  says so.
- **`suspendedAt` on the school survives a resume**, on purpose — it is when the suspension began.

**Cancel (#21)** ends the contract, and **the school usually keeps working until the period it
already paid for runs out.**

- **No field was added to the model for that.** The status goes `CANCELLED` either way — the
  contract is over either way — and what decides the access is `currentPeriodEnd`: `whyNotActive`
  now lets a cancelled subscription grant until that date passes. `immediate: true` **trims the
  period to now**, which is what stops it. The status says cancelled; the dates say how long for.
- **Nothing undoes it, and no new check was needed.** Renew already refuses a `CANCELLED`
  subscription and Resume already refuses one. Bringing the school back means selling it something
  new.
- **The immediate shape overwrites what the school paid for**, so the history row's reason keeps
  the original end date — the only place it survives.
- **Almost everything can be cancelled**, the opposite of Suspend and Resume: a trial that did not
  convert, a suspended school that never paid, one that is `PAST_DUE`. A `CLOSED` or `OFFBOARDING`
  school too, because cancelling is *part of* winding one down. Only a deleted school is refused.
- **Cancelling twice** is `409 CANCELLATION_ALREADY_SCHEDULED` while the period still runs — but
  escalating to `immediate` is allowed, because that is a real decision.
- **It does not touch the school**, and no money moves. And nothing marks a lapsed subscription
  `EXPIRED`, so one that has served out its period reads `CANCELLED` with `periodEnded: true`.
  **A job will close these** — there is no endpoint for it, because a period end passing is a date
  arriving rather than a decision. Same for `PAST_DUE`. Read `periodEnded` alongside `status`: the
  record is never wrong, only untidied.

## The school's own view: #33 and #34

`Subscription — the school's own view` holds the two reads a school makes about itself. Both take
the `X-School-Subdomain` header and **neither has an `{id}` in the path** — the tenant comes from
`CurrentSchoolResolver`, so a caller cannot ask about a school it does not belong to.

**Get My Subscription (#33)** is the billing screen, and it is deliberately shorter than the
platform's read of the same subscription (#27): no `planListPrice`, no
`billingCustomerReference`, no limit overrides, no `planCode`. A school on a negotiated price
should not be shown a number it is not paying. Its saved tests assert those four are *absent* —
that is the design, not an accident.

**Get Feature access (#34)** is the one the rest of the product asks. Read `allowed`, not
`includedInPlan`: the first is whether it may be used right now, the second is only what the
plan says. `allowed` is false on every feature when the subscription grants nothing, so a
caller that forgets the top-level `active` flag still gets the right answer.

**The case worth running by hand** is a subscription whose `currentPeriodEnd` is in the past.
`status` still reads `ACTIVE` — nothing marks a subscription expired yet — but `active` is false,
`reason` says the period ended, and every feature comes back `allowed: false` while
`includedInPlan` stays true. That is the case a module reading the plan's features directly would
get wrong, which is the whole reason #34 exists.

## Plan catalogue: nine endpoints, and #5 is the one that is missing

`POST /platform/plans/drafts` always creates a **`DRAFT` at version 1 that is not publicly
available** — `status`, `planVersion` and `publiclyAvailable` are not on the request, so sending
them does nothing. Everything except #5, a new version of a published plan, is built.

Two things that will bite while testing:

**Run them in order: Create Plan Draft → Set Plan Features → Publish Plan → Set Plan
Availability → Retire Plan.** Publishing refuses a plan with no features, and retiring blocks
availability afterwards — which is why Retire is last in the folder.

**List Plan Versions (#9) will show one version per plan** until #5 is built — nothing in the API
creates a second version. Its `schoolsOnThisVersion` is 0 everywhere for the same kind of reason:
nothing had created a subscription when it was written. #13 does now, so the count moves once a
school is put on a version.

**List Plans (#8) is the broader read.** Every parameter is optional, and the saved request sends
`page` and `size` with the rest present but **disabled**, so you can tick one on rather than
typing it. Its fifteen cases are in the request description — Postman sends no body on a `GET`.

**Only after Set Plan Availability is a plan `sellable`.** It takes all three: `ACTIVE`, on the
public list, and inside the selling window. Every plan response reports `sellable` and says which
of the three is missing.

- **Publishing cannot be undone, and there is no way back.** #2 and #3 refuse the plan
  afterwards, there is no unpublish, and **#5 (new version) is deferred by decision** — so a
  published plan's price can never be changed by anything that exists. The `409` messages tell
  you to "make a new version", which is #5; that advice cannot be followed yet. Put
  `{{$timestamp}}` in the **name** if you are going to run this repeatedly, since the code is
  derived from the name.
- **A published plan is not yet sellable.** `publiclyAvailable` stays false until #7, which is
  not built, so `sellable` is always false today.
- **Nothing can be deleted, but a plan can be retired (#6).** That is the only way to take a
  draft or a published plan out of the catalogue, and it does not release the `planCode`. Retiring
  is terminal — there is no un-retire.
- **Retiring does not touch subscriptions.** Schools already on the plan keep it. Nothing in this
  collection cancels anybody.
- **`featureCount` is always 0.** Features come from #3, which is not built. `sellable` is
  therefore always `false`.

Create Plan Draft saves `planCode` and `planVersion`, which is what the `PATCH` URL uses — run
it first.

## List Schools (G1) is the first read

The only `GET` in the collection so far. Every parameter is optional — the saved request sends
`page`, `size` and `sort`, with `status`, `search`, `countryCode`, `city` and the date range
present but **disabled**, so you can tick one on rather than typing it.

Its fifteen test cases are in the request **description**, not a body block: Postman sends no
body on a `GET`.

Worth knowing while testing: bad paging is **refused, not clamped** (`size=5000` is a `400`), and
`sort` accepts only an allow-list of fields.

## Change Subdomain (#10) rewrites a variable

It is the one request that invalidates another. The subdomain is the key every school-surface
request resolves through, so the moment it changes, the old label 404s. Its test script updates
`createdSubdomain` to the new value — **if you run it by hand and skip the script, every
`/schools/current` request afterwards fails** until you fix the variable.

Nothing reserves the old label, either: it is free for any school to claim the instant the
change commits.

## A note on the four gates (#24–27)

They take **no body** — anything sent is ignored — and **all four are idempotent**: send one
twice and the second is a `200` saying nothing changed. Each pair is independent, so locking
results does not touch enrollment. #25 does not touch students already enrolled.

All four announce the same gap in every response's `nextStep` rather than hide it: **no
authorization is enforced**, because the permission model does not exist yet.

**#27 records nothing about who unlocked, or why.** It is built simple on purpose — with no
authentication an audit row could not name an actor anyway — and what it must gain before results
are real is written down in
`backend/src/main/java/com/orbitastra/backend/controllers/core/README.md`.
