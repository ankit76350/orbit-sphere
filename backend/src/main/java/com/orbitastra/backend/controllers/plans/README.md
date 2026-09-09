# controllers/plans — API plan

**Fifteen of 71 are built — #1 to #4, #6 to #10, #13, #14, #16, #27, #33 and #34.** #15 was
built and then withdrawn; see [its entry](#e15). The entire plan
catalogue except versioning; giving a school its first subscription, turning that trial into a
paying one, and reading back what a school is on; and the school's own two reads — its billing
screen, and the feature-access check the rest of the product asks.

**#13 closes the gap `core` has been announcing.** `activateSchool` was written to require an
active subscription, found that nothing could create one, and settled for a soft check that says
so in every response: *"Activation was allowed anyway because nothing creates subscriptions yet —
this check must become a hard requirement once it does."* It does now. **Making that check hard
is a decision still to take** — see the note under "13".

Create a draft, edit it, set its features, publish it, list it publicly, retire it; and read it
three ways — the whole catalogue, one plan's version history, or one version in full.

The only thing missing from the group is **#5**, deferred by decision, and its absence means a
published plan's price can never be changed. See "5" below.

**#5 is deferred by decision.** That has a consequence worth knowing before you hit it: **a
published plan's price can never be changed by any endpoint that exists today.** #2, #3 and #4
all refuse a published plan and tell the caller to make a new version instead — which is #5. So
today the advice in those messages cannot be followed. See "5" below. Everything else is
listed below and not built. This is the full set of endpoints the `plans` module needs, written
before any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md) was done.

Built endpoints are marked **built** in the `#` column. Anything unmarked does not exist yet, and
a request to it returns a 404.

Mirrors [`models/plans`](../../models/plans) and
[`models/plans/billing`](../../models/plans/billing), whose two READMEs already describe the
documents, the status workflow and the money rules. **These endpoints enforce those two files.
They do not invent new rules.**

---

## What this module is

This is how a school **pays us for Orbit Sphere**. Which plan the school bought, what it costs,
when it renews, the bills we send them, and the money coming back.

**It is not the student fee module.** That is [`models/finance`](../../models/finance), and it is
a completely different thing: fees are money a parent pays a school. This is money a school pays
the platform. The two never meet, and no endpoint here may touch a `FeeInvoice`.

Seven documents:

| Document | Collection | What it holds |
|---|---|---|
| [`PlanDefinition`](../../models/plans/PlanDefinition.java) | [`plan_definitions`](../../models/plans/PlanDefinition.java) | one version of a plan we sell — price, limits, features |
| [`SchoolSubscription`](../../models/plans/SchoolSubscription.java) | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) | what one school actually bought |
| [`SubscriptionHistory`](../../models/plans/SubscriptionHistory.java) | [`subscription_history`](../../models/plans/SubscriptionHistory.java) | every change to that, never edited |
| [`SubscriptionInvoice`](../../models/plans/billing/SubscriptionInvoice.java) | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) | one bill for one billing period |
| [`SubscriptionPayment`](../../models/plans/billing/SubscriptionPayment.java) | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) | money that arrived against a bill |
| [`PaymentAttempt`](../../models/plans/billing/PaymentAttempt.java) | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) | every try at the gateway, failures included |
| [`BillingWebhookEvent`](../../models/plans/billing/BillingWebhookEvent.java) | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) | what the payment provider told us |

## Three surfaces, and why

`PlanDefinition` is the only document here with **no** `schoolId`. It is platform configuration
shared by every tenant. Everything else belongs to one school.

| Surface | Base path | Who is calling | Tenant comes from |
|---|---|---|---|
| **Platform** | `/platform/…` | our operator | the URL — they are outside the tenant |
| **School** | `/schools/current/…` | the school itself | `CurrentSchoolResolver`, never the URL |
| **Webhook** | `/billing/webhooks/…` | the payment provider | the signed payload |

The school surface follows the same rule core does: **the school is never named in the URL.** A
caller cannot ask about a school they do not belong to, because they never name one at all.

**The split is not cosmetic.** A school may look at its own subscription and pay its own bills.
A school may **not** change its own price, extend its own trial, raise its own student limit, or
mark its own invoice paid. Those endpoints only exist on the platform surface, so there is no
request a school can send that does them.

---

## Dates in messages

**Requests and responses carry ISO-8601 instants; messages spell the date out.**

```
in a field   "currentPeriodEnd": "2027-10-08T16:31:00Z"
in a message "... runs to Friday 8 October 2027 10:01PM, which has not passed yet ..."
```

A field is parsed by a program, so it stays machine-readable. A `message`, `note`, `nextStep` or
`reasonForChanges` is read by a person deciding what to do next, and `2027-10-08T23:59:59Z` in the
middle of a sentence is something nobody reads. One helper — `common/time/Dates` — renders all of
them, so the format is one decision rather than thirty concatenations that drift apart.

**The zone is not cosmetic — it decides the calendar day.** A billing period starts at midnight in
the school's own timezone, which for an Indian school is stored as an instant 5½ hours earlier:

| the stored instant | rendered in UTC | rendered in `Asia/Kolkata` |
|---|---|---|
| `2026-09-07T18:30:00Z` | Monday 7 September 2026 6:30PM | **Tuesday 8 September 2026 12:00AM** |

The right-hand column is what the school means, so **every date belonging to a school is rendered
in that school's `defaultTimeZone`** — on both surfaces. Telling a school its period began on the
7th when its own calendar says the 8th is worse than telling it nothing. A **plan's** selling
window belongs to the platform and to no school, so those read in UTC.

**The time is always shown**, even where it looks redundant, because two of these messages compare
one date against another — "must be after", "is before the start of today" — and both ends can
fall on the same day. Without the time, `INVALID_BILLING_PERIOD` reads as a contradiction.

A date that is genuinely absent reads `(not set)`. It used to print the literal word `null`.

---

# The endpoints

Numbered straight through, 1 to 71. Grouped only so the list is readable.

## 1. The plan catalogue — writes (platform) · [Build order ↓](#build-order)

A plan version is **immutable once published**. That is the rule the whole group is shaped
around: you edit a draft, and after that you make a new version instead.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /platform/plans/drafts`](#e1) | Make a new plan. It starts as `DRAFT`, so nobody can buy it while we are still deciding the price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t2"></a>2 — **built** | [`PATCH /platform/plans/{code}/versions/{version}`](#e2) | Fix the details of a plan that is still a draft — name, price, limits. Refused once the plan is published, because schools have already bought it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t3"></a>3 — **built** | [`PUT /platform/plans/{code}/versions/{version}/features`](#e3) | Set the whole feature list of a draft plan in one go. Replacing the list is safer than editing one feature at a time, because a half-edited feature list is a plan nobody can price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t4"></a>4 — **built** | [`POST /platform/plans/{code}/versions/{version}/publish`](#e4) | Turn a draft into a real plan schools can buy. From here the plan can never be edited again. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t5"></a>5 — **deferred** | [`POST /platform/plans/{code}/versions/{version}/new-version`](#e5) | Copy a published plan into a new draft version, so we can change the price. The old version stays exactly as it was for the schools already on it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t6"></a>6 — **built** | [`POST /platform/plans/{code}/versions/{version}/retire`](#e6) | Stop selling a plan. Schools already on it keep it and keep working; new schools just cannot pick it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t7"></a>7 — **built** | [`PATCH /platform/plans/{code}/versions/{version}/availability`](#e7) | Say whether a plan shows on the public list or is only offered privately in a quote. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |

## 2. The plan catalogue — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t8"></a>8 — **built** | [`GET /platform/plans`](#e8) | The operator's list of every plan, filtered by status or code. This is the screen somebody opens to see what we sell. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t9"></a>9 — **built** | [`GET /platform/plans/{code}/versions`](#e9) | Every version of one plan, newest first. Shows how the price changed over time and which version each school is on. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t10"></a>10 — **built** | [`GET /platform/plans/{code}/versions/{version}`](#e10) | One plan version in full, with all its features. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t11"></a>11 | [`GET /schools/current/plans`](#e11) | The plans **this school** is allowed to move to — published, still on sale, and public. The school's own upgrade screen reads this. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t12"></a>12 | [`GET /schools/current/plans/{code}/versions/{version}/comparison`](#e12) | What would change if this school moved to that plan: the price difference, and any limit that would drop below what the school is already using. Stops a school upgrading into a plan that immediately blocks it. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 3. The subscription lifecycle — writes (platform) · [Build order ↓](#build-order)

The allowed moves are already written in [`models/plans/README.md`](../../models/plans/README.md)
under "Status workflow".

**Where the status changes, a `SubscriptionHistory` row is written in the same transaction** —
13, 14, 16, 17, 18, 19, 20, 21 and 22. That is not optional: the history is how anybody later
answers "why is this school suspended".

**#14 writes a history row, and #25 and #26 still do not.** That was the gap:
[`SubscriptionEventType`](../../models/plans/enums/SubscriptionEventType.java) described only
status moves, so a change to the trial end date, the agreed limits or the price had nothing honest
to write. #14 closed it for the terms it edits — the enum gained `TERMS_CHANGED`, and #23 and #24
folded into it. The two money endpoints that stayed separate still have no event type of their
own; `TERMS_CHANGED` fits both when they are built. See the note at the end of this file.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t13"></a>13 — **built** | [`POST /platform/schools/{id}/subscriptions`](#e13) | Give a school its first subscription. **The billing cycle can be negotiated** — absent takes the plan's, and whichever applies decides the period dates and whether an end date is required. This is what makes a school a paying customer, and it is the missing piece the core module already complains about — `activateSchool` currently lets a school go live with no subscription at all. **A school still `PROVISIONING` with everything else in place goes `ACTIVE` here**, because a subscription was the last thing it was waiting for. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`schools`](../../models/core/School.java) |
| <a id="t14"></a>14 — **built** | [`PATCH /platform/schools/{id}/subscriptions/current`](#e14) | Edit when a subscription runs, what state it is in, and how much of the product it may use: status, billing cycle, both period dates, auto-renewal, the two capacity overrides. **Only a `TRIAL` or `ACTIVE` subscription may be edited** — the other four each have an endpoint that owns the way out of them. **The cadence decides the period** — changing the cycle or the start recalculates the end, moving to `CUSTOM` requires a date with it, and on the four fixed cadences an end date is **refused** (`400 BILLING_PERIOD_END_NOT_ALLOWED`): only `CUSTOM` takes one. **A `reason` is required** and is stored as `reasonForChanges`. **Nothing about the money** — price and currency are #25, the billing customer #26, the plan #16. **Replaced extend-trial**, which moved one date — that is now `currentPeriodEnd` here — and supersedes #23 and #24. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t15"></a>[~~15~~](#e15) **removed** | ~~`POST /platform/schools/{id}/subscriptions/{no}/activate`~~ | Move a trial to a paying subscription. **Withdrawn 2026-09-07** — whether a subscription starts as `TRIAL` or `ACTIVE` is decided when it is sold (#13), and a trial that later becomes a paying one is either a status edit (#14) or, when the school is buying a different plan from the one it tried, a new subscription. A whole endpoint for one status move was a third way to do the same thing. | — |
| <a id="t16"></a>16 — **built** | [`POST /platform/schools/{id}/subscriptions/current/change-plan`](#e16) | Move the school onto a different plan or a newer version, and say when the change starts and what happens to the money already paid. **The plan moves immediately**; `currentPeriodStart` is **required** and chooses when the new billing period begins, with the closed row ending at that same instant. **The cadence can be negotiated** — absent takes the new plan's, and whichever applies decides the period and whether an end date is required. Price and both capacity ceilings come from the new plan unless the request names them. **No money moves** — nothing raises invoices yet. Takes the school `ACTIVE`, and refuses a school being wound down. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t17"></a>17 — **built** | [`POST /platform/schools/{id}/subscriptions/current/renew`](#e17) | Start the next billing period. Normally the nightly job calls this; an operator can call it by hand when something went wrong. **No request body** — the plan, price, ceilings and cycle all carry across untouched. Writes a second row and closes the period that ended, so the new period starts exactly where the last one finished. **No invoice is raised** — nothing writes `subscription_invoices` yet. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| <a id="t18"></a>[~~18~~](#e18) **not being built** | ~~`POST /platform/schools/{id}/subscriptions/{no}/mark-past-due`~~ | Mark that the bill was not paid on time. **Dropped 2026-09-07** — it is one status move, and [#14](#t14) makes status moves with a required reason and a history row. `PATCH .../subscriptions/current` with `{"status": "PAST_DUE", "reason": …}` is the whole endpoint. | — |
| <a id="t19"></a>19 — **built** | [`POST /platform/schools/{id}/subscriptions/current/suspend`](#e19) | Stop the school using the product because the bill is still unpaid. Kept separate from a bare status change because cutting a school off is a decision with a grace period behind it, not a field edit. **Moves two documents**: the subscription goes `SUSPENDED`, which turns every feature off, and the school goes `SUSPENDED` too, which is what blocks the tenant. `ACTIVE` and `PAST_DUE` only, and a `reason` is required. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`schools`](../../models/core/School.java) |
| <a id="t20"></a>20 — **built** | [`POST /platform/schools/{id}/subscriptions/current/resume`](#e20) | Switch the school back on after it pays. The exact reverse of #19, and only that — **the period is not extended**, so a school suspended for three weeks comes back to the same end date. `SUSPENDED` only, and a `reason` is required. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`schools`](../../models/core/School.java) |
| <a id="t21"></a>21 — **built** | [`POST /platform/schools/{id}/subscriptions/current/cancel`](#e21) | End the subscription with a reason. The school usually keeps working until the period it already paid for runs out. **The status goes `CANCELLED` either way** — the contract is over — and it is the **period** that decides the access: a cancelled subscription keeps granting until `currentPeriodEnd` passes. `immediate: true` trims that date to now instead. **Does not touch the school**, and no money moves. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| <a id="t22"></a>[~~22~~](#e22) **not being built** | ~~`POST /platform/schools/{id}/subscriptions/{no}/expire`~~ | Close a subscription whose last paid period has now ended. **Dropped 2026-09-08 — a job will do it.** Its own description said "normally the nightly job does this", and nothing else ever calls it: expiring is not a decision somebody makes, it is a date arriving. An operator who needs it now has [#14](#t14). **Moving *into* `PAST_DUE` and `EXPIRED` is job territory**, the same conclusion [#18](#t18) reached. | — |
| <a id="t23"></a>[~~23~~](#e23) **superseded by [#14](#t14)** | ~~`PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew`~~ | Turn automatic renewal on or off. **#14 does this**, so this endpoint is not being built — see the note below. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t24"></a>[~~24~~](#e24) **superseded by [#14](#t14)** | ~~`PATCH /platform/schools/{id}/subscriptions/{no}/overrides`~~ | Give one school a bigger student or user limit than its plan normally allows, because that is what was negotiated. **#14 does this**, so this endpoint is not being built — see the note below. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t25"></a>25 | [`PATCH /platform/schools/{id}/subscriptions/{no}/price`](#e25) | Change the agreed price for this one school without changing the plan everybody else is on. **#14 deliberately cannot**: what a school pays gets invoiced, so it keeps its own endpoint. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t26"></a>26 | [`PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer`](#e26) | Save the payment provider's customer id against the school, so future charges can be raised against it. **#14 deliberately cannot**: it is a payment-provider handle, and it belongs with the money. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 4. The subscription — reads (platform) · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t27"></a>27 — **built** | [`GET /platform/schools/{id}/subscription`](#e27) | What this school is on right now: plan, price, status, when the period ends. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t28"></a>28 — **built** | [`GET /platform/schools/{id}/subscriptions`](#e28) | Every subscription this school has ever had, including old cancelled ones. **Paged, filtered and sorted in the database.** A school with none gets an **empty page**, not a 404. Filters: `status`, `billingCycle`, `planCode`, `planVersion`, `autoRenew`, `current`, and the two period windows. **There is no `trial` filter** — a trial is a status, so `?status=TRIAL` is the whole answer. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t29"></a>29 — **built** | [`GET /platform/schools/{id}/subscriptions/{no}/history`](#e29) | The full trail of what changed, when, who did it and why. The answer to "why did this school get suspended". **Paged, filtered and sorted in the database.** Use `current`, or the `subscriptionId` from #28 — a subscription number has slashes and cannot go in a URL. Filters: `eventType`, `status`, `previousStatus`, `source`, `performedByDocsId`, `sourceEventId`, `reason`, and the two date windows. **`effectiveAt` and `createdAt` are different dates** and both are filterable. Another school's subscription is a **404**, not a 403. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t30"></a>30 | [`GET /platform/subscriptions`](#e30) | Every school's subscription in one list, filtered by status. The operator's main screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t31"></a>31 | [`GET /platform/subscriptions/renewals-due`](#e31) | Which subscriptions renew in the next N days. Lets somebody see a renewal coming before it fails. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t32"></a>32 | [`GET /platform/subscriptions/at-risk`](#e32) | Everything past due, suspended, or ending soon with auto-renew off. The list somebody works through on a Monday morning. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 5. The subscription — the school's own view · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t33"></a>33 — **built** | [`GET /schools/current/subscription`](#e33) | What plan am I on, what does it cost, when does it renew. The school's billing screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t34"></a>34 — **built** | [`GET /schools/current/subscription/feature-access`](#e34) | **The one the rest of the product needs.** Answers "is this school allowed to use this feature, and how much of it is left". Every module that gates a feature must ask this instead of reading the plan itself, because the moment two places work out feature access they disagree. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| <a id="t35"></a>35 | [`GET /schools/current/subscription/usage`](#e35) | How much of each limit the school has used — students, users, whatever a feature counts. Shown next to the limits so a school can see itself getting close. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java), [`students`](../../models/student/Student.java), [`user_accounts`](../../models/identity/UserAccount.java) |
| <a id="t36"></a>36 | [`GET /schools/current/subscription/history`](#e36) | The school's own view of its plan changes. Shows what happened, but not the operator's internal notes. | [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| <a id="t37"></a>37 | [`PATCH /schools/current/subscription/auto-renew`](#e37) | Lets a school turn off automatic renewal itself, rather than having to email us. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t38"></a>38 | [`POST /schools/current/subscription/cancel-request`](#e38) | The school asks to cancel. It **requests** — it does not cancel. Cancelling is #21, and it stays with the operator so somebody talks to the school first. | **none — no model holds a cancellation request yet.** See the note at the end of this file. |

## 6. Invoices — writes (platform) · [Build order ↓](#build-order)

Money rules from [`models/plans/billing/README.md`](../../models/plans/billing/README.md):
`totalAmount = subTotal + taxAmount` and `outstandingAmount = totalAmount - paidAmount`. **Every
endpoint below keeps both true or fails.** An issued invoice is never deleted or quietly changed.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t39"></a>39 | [`POST /platform/schools/{id}/subscription/invoices`](#e39) | Raise a bill for one billing period. Created as a draft so the amounts can be checked before the school sees it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| <a id="t40"></a>40 | [`PATCH /platform/schools/{id}/subscription/invoices/{no}`](#e40) | Correct a draft invoice before it is sent. Refused once it is issued. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t41"></a>41 | [`POST /platform/schools/{id}/subscription/invoices/{no}/issue`](#e41) | Send the bill to the school. After this the amounts are fixed, and the only way to undo it is #42. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t42"></a>42 | [`POST /platform/schools/{id}/subscription/invoices/{no}/void`](#e42) | Cancel a bill that should not have been sent, with a reason. The invoice stays in the records — it is marked void, never deleted, because a missing invoice number is a hole somebody has to explain. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t43"></a>43 | [`PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date`](#e43) | Give a school more time to pay. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t44"></a>44 | [`POST /platform/schools/{id}/subscription/invoices/{no}/record-payment`](#e44) | Write down money that came in outside the gateway — a bank transfer, a cheque, cash. Without this, any school not paying online can never be marked paid. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| <a id="t45"></a>45 | [`POST /platform/schools/{id}/subscription/invoices/{no}/remind`](#e45) | Send the school a reminder that the bill is unpaid. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t46"></a>46 | [`POST /platform/schools/{id}/subscription/invoices/{no}/write-off`](#e46) | Accept that a bill will never be paid and close it, with a reason. Keeps the outstanding list honest instead of full of debts nobody is chasing. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 7. Invoices — the school's own view · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t47"></a>47 | [`GET /schools/current/subscription/invoices`](#e47) | Every bill we have sent this school, newest first. Drafts are hidden — the school should not see a bill we have not sent. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t48"></a>48 | [`GET /schools/current/subscription/invoices/{no}`](#e48) | One bill in full, with what is still owed on it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t49"></a>49 | [`GET /schools/current/subscription/invoices/{no}/pdf`](#e49) | The bill as a file the school can download, keep and give to its accountant. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t50"></a>50 | [`GET /schools/current/subscription/outstanding`](#e50) | One number: how much this school owes right now. What a banner at the top of the screen shows. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 8. Invoices — reads (platform) · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t51"></a>51 | [`GET /platform/schools/{id}/subscription/invoices`](#e51) | Every invoice for one school, drafts included. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t52"></a>52 | [`GET /platform/invoices`](#e52) | Invoices across every school, filtered by status and due date. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t53"></a>53 | [`GET /platform/invoices/overdue`](#e53) | Everything unpaid and past its due date. The collections list. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t54"></a>54 | [`GET /platform/invoices/{no}`](#e54) | One invoice in full, with its payments and every attempt at paying it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |

## 9. Paying — the school · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t55"></a>55 | [`POST /schools/current/subscription/invoices/{no}/pay`](#e55) | Start paying a bill. Creates the attempt record and hands back whatever the gateway needs to show the school a payment page. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t56"></a>56 | [`GET /schools/current/subscription/payments/{no}`](#e56) | Check whether a payment went through. The page the school lands back on after paying asks this. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| <a id="t57"></a>57 | [`GET /schools/current/subscription/payments`](#e57) | Every payment this school has made. Its receipt list. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| <a id="t58"></a>58 | [`GET /schools/current/subscription/payments/{no}/receipt`](#e58) | A receipt for one payment, as a file. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| <a id="t59"></a>59 | [`POST /schools/current/subscription/payment-method`](#e59) | Save a card or set up a UPI mandate so renewals can charge automatically instead of somebody paying by hand every month. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| <a id="t60"></a>60 | [`DELETE /schools/current/subscription/payment-method`](#e60) | Remove the saved payment method. Auto-renew has to be dealt with at the same time, or the next renewal fails silently. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 10. Payments — platform · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t61"></a>61 | [`GET /platform/schools/{id}/subscription/payments`](#e61) | Every payment from one school. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| <a id="t62"></a>62 | [`GET /platform/invoices/{no}/attempts`](#e62) | Every try at paying one bill, failures included, with what the gateway said went wrong. This is the screen for "the school says they paid and it did not work". | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| <a id="t63"></a>63 | [`POST /platform/schools/{id}/subscription/payments/{no}/refund`](#e63) | Give money back, in full or in part. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| <a id="t64"></a>64 | [`POST /platform/schools/{id}/subscription/payments/{no}/reconcile`](#e64) | Match a payment to the money the bank actually settled, and mark it settled. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| <a id="t65"></a>65 | [`POST /platform/schools/{id}/subscription/payments/{no}/retry`](#e65) | Try a failed automatic charge again. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |

## 11. The payment provider talking to us · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t66"></a>66 | [`POST /billing/webhooks/{provider}`](#e66) | Where the payment provider tells us a payment succeeded or failed. It checks the signature, saves the event exactly as it arrived, and only then applies it. **The same event arriving twice must change nothing the second time**, which is what the unique `provider + providerEventId` index is for. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| <a id="t67"></a>67 | [`GET /platform/billing/webhooks`](#e67) | Every event the provider sent us, and whether we managed to process it. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| <a id="t68"></a>68 | [`GET /platform/billing/webhooks/{id}`](#e68) | One event in full, including what went wrong if it failed. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| <a id="t69"></a>69 | [`POST /platform/billing/webhooks/{id}/replay`](#e69) | Process a failed event again after the bug is fixed. The raw payload was saved for exactly this. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

## 12. The jobs that run on their own · [Build order ↓](#build-order)

These do the work nobody clicks a button for. They are endpoints as well as scheduled jobs so
they can be run by hand when something needs fixing, and so they can be tested.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t70"></a>70 | [`POST /platform/billing/jobs/renew-due`](#e70) | Find every subscription whose period ends today, raise the next invoice, and charge the saved payment method. The job that keeps the money coming in. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| <a id="t71"></a>71 | [`POST /platform/billing/jobs/age-overdue`](#e71) | Find bills that are past their due date and move those subscriptions to past due, then to suspended once the grace period runs out. The job that stops schools using the product for free forever. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

---

<a id="build-order"></a>
# Build order

Nothing about this module works until a school can have a subscription at all, and nothing about
the money works until there is something to bill.

| Phase | What it gives you | Endpoints |
|---|---|---|
| **1** | We can define what we sell | 1–10 |
| **2** | A school can have a subscription, and the product can ask what it is allowed to do | 13, 14, 27, 33, **34** |
| **3** | The subscription can move through its life | 15–26, 28–32, 36 |
| **4** | We can bill a school and take money by hand | 39–44, 47–54, 61 |
| **5** | The school can pay online | 55–58, 62, 66–69 |
| **6** | It runs without us | 59, 60, 63–65, 70, 71 |
| **7** | The rest | 11, 12, 35, 37, 38, 45, 46 |

**#34 is the one to build early.** Every other module that has a paid feature is waiting on
`GET /schools/current/subscription/feature-access`. Until it exists, each of them will invent its
own way of checking, and then they will disagree — the same problem G9 solved for working days
in the core module.

**#13 closes a hole that already exists.** `SchoolPlatformService.activateSchool` currently
allows a school to go live with no subscription, and says so in its own comment: *"Activation was
allowed anyway because nothing creates subscriptions yet — this check must become a hard
requirement once it does."* Building #13 is what lets that become a hard requirement.

---

# Things this module deliberately will not have

- **No `DELETE` on a plan, an invoice, a payment or a history row.** A plan is retired, an
  invoice is voided, a payment is refunded. Money records that vanish are money records somebody
  has to explain later.
- **No editing a published plan.** #5 makes a new version instead. A school that bought
  `PREMIUM v1` keeps `PREMIUM v1` exactly as it was on the day they signed.
- **No editing a `SubscriptionHistory` row.** It is written once, by whichever endpoint caused
  the change, and never touched again.
- **No school-facing endpoint that changes money.** A school cannot set its own price, raise its
  own limits, extend its own trial or mark its own invoice paid. Those requests do not exist.
- **No plan or subscription fields copied onto `School`.** There would then be two answers to
  "what plan is this school on", and one of them would be stale.
- **No raw webhook payload readable through the API.** It is stored encrypted for audit and
  reprocessing only.

---

# To settle before building

**Authentication does not exist yet.** Everything under `/platform/` here creates and cancels
paying customers, and everything under `/schools/current/` is behind the same
`X-School-Subdomain` header any caller can set. That is worse in this module than in core: core
lets an attacker edit a school's address, this one lets them cancel a subscription or mark an
invoice paid. **Do not put this module anywhere reachable before platform credentials are in
front of it.**

**Who writes the history row.** Every lifecycle endpoint must write one, and it must be in the
same transaction as the change itself. A status that moved with no history row is a change nobody
can explain later. Decide whether the service does it directly or through one shared helper —
one shared helper, probably, so it cannot be forgotten.

**Where invoice and payment numbers come from.** `NumberSequence` already exists and is already
seeded for every school by `complete-provisioning`. Check which sequence types are there before
inventing new ones.

**Money type.** Every amount is `BigDecimal` stored as Decimal128, already. No endpoint may
accept or return a floating-point number for money.

**Grace period length.** #19 needs a number of days and #71 needs the same number. It belongs in
configuration, not in two services. (This used to say "#18 to #19"; #18 is no longer being
built.)

**What happens to money on a mid-period plan change — still open, and #16 does not settle it.**
The three possibilities are: charge the difference now, credit it, or leave the paid period alone.
It is a commercial decision, and #16 deliberately neither asks nor assumes: it moves the plan,
restarts the period, and **charges, credits and refunds nothing**. Its response says exactly that,
so nobody reads a plan change as a payment.

**It can stay open because nothing could act on it anyway.** `subscription_invoices` has no
writer. #17 is built and deliberately raises no invoice either — see [the money](#e17-money) —
so there is still nothing in the codebase that could charge, credit or refund. The question
belongs with whatever eventually raises invoices, not with the request that moves a plan;
answering it now would mean storing a decision no code could honour, on every plan change, for
however long invoicing takes.

**The third possibility turned out to be a different question, and is not offered.** "Start the
new plan at the next period" is not about money at all, it is about scheduling — and there is
nowhere on a subscription to hold a change that has not happened, because it holds one plan, not a
current one and a pending one. So #16 has **no timing field**: the change is immediate, the period
restarts with it, and moving the pointer now while calling it next period is exactly the
dishonesty the missing field avoids. If deferring a change is ever wanted it belongs next to
[#17](#e17), which is the endpoint that knows when a period ends — it does not defer anything
today.

**`SubscriptionEventType` could not describe five of these endpoints — settled 2026-09-07.**
Filling in the collections column above is what turned it up. Every constant the enum had was a
status move, so nothing described extend-trial (#14) or the four term edits (#23 to #26): they
changed what a school pays or what it may use, and would have changed it leaving no trace of who
did it or why.

The answer turned out to be one endpoint rather than five event types — for three of the five.
extend-trial, #23 and #24 were three endpoints editing three columns of one document, so they
became **#14**, and the enum gained one constant, **`TERMS_CHANGED`**, for an edit where the
status did not move. A status move made through #14 still writes the type that names it — a
suspension recorded there and one recorded through #19 read identically in the history, which is
what "when was this school suspended" needs.

**#25 and #26 kept their own endpoints, decided 2026-09-07.** Both write money: what a school is
charged, and the provider handle it is charged against. Neither is a term somebody adjusts while
moving a date, and an edit that could reprice a school while looking like an administrative
change is the one worth making a caller ask for separately. Their history rows are still
unmodelled — `TERMS_CHANGED` fits both when they are built.

Adding a constant is safe in a way removing one is not: an unrecognised stored value fails the
whole query it appears in, not the single row.

**Nothing models a cancellation request (#38).** A school asking to cancel is not a status change
and not a history event, so there is nowhere to put it. Either give it a small document of its
own, or make #38 raise a notification and hold no state. Decide before building it, because a
request that is accepted and then forgotten is worse than no endpoint at all.

---

# Appendix — what every API touches, field by field

The same 71 endpoints, with the fields each one reads and each one writes. Written so that whoever
builds an endpoint does not have to work this out again from the models, and so a reviewer can see
at a glance whether a change reaches a field it should not.

Read **updates** as "changes an existing document", **insert** as "writes a new one", and
**reads** as "looks at it but does not change it".

Two things are left out of every entry because they are true of all of them:

- **The audit fields** — `createdAt`, `updatedAt`, `createdByDocsId`, `updatedByDocsId` and `version` — are filled in by Spring Data on every write. No endpoint sets them by hand.
- **`schoolId`** is on every document here except `plan_definitions`, and every query must carry it. On the school surface it comes from `CurrentSchoolResolver`; on the platform surface it comes from the `{id}` in the URL.

The entries name the fields. **[What each field can hold](#what-each-field-can-hold)**, below,
names the values — every enum in full, and for the open ones, what actually gets written.

## What each field can hold

The entries below name the fields; this names the **values**. Stated once here rather than
repeated across 71 entries, so there is one place to correct when a set changes.

**Where a set is closed, it is an enum and the list is exhaustive** — anything else is a `400`
naming the field and listing what is accepted. Where it is open (`name`, `reason`, every gateway
reference) the column says so, because an open set is a thing a reviewer should notice.

A value in **bold** is written by an endpoint that exists today. Everything else is what the
endpoint will write when it is built — **ten of 71 are built**, so most of the billing tables
below are still a plan.

### `plan_definitions` — [PlanDefinition](../../models/plans/PlanDefinition.java)

| Field | Type | What can be in it |
|---|---|---|
| `planCode` | String, required | Derived from `name`: trimmed, uppercased, every run of non-alphanumerics becomes `_`, leading and trailing `_` dropped — "Premium Plus" → **`PREMIUM_PLUS`**. An explicit code is accepted for when the derived one is taken. **Never changes**: it is the family key joining v1, v2 and v3. |
| `planVersion` | Integer, required | **`1`** at create. #5 would raise it and #5 is not built, so every row is `1` today. |
| `name` | String, required | **Open** — free text, and the only field a plan is renamed by. |
| `description` | String, optional | **Open** — free text, or absent. |
| `status` | [PlanStatus](../../models/plans/enums/PlanStatus.java), required | **`DRAFT`** (#1) → **`ACTIVE`** (#4) → **`RETIRED`** (#6). One-way in that order: no unpublish, and retiring is not a way back to draft. |
| `billingCycle` | [BillingCycle](../../models/plans/enums/BillingCycle.java), required | **`MONTHLY`** · **`QUARTERLY`** · **`HALF_YEARLY`** · **`YEARLY`** · **`CUSTOM`** |
| `listPrice` | BigDecimal, `DECIMAL128`, required | **`0` or more, at most 2 decimal places**, stored at exactly scale 2. `-1` → `400 PRICE_NEGATIVE`; `99.999` → `400 PRICE_TOO_PRECISE` — over-precision is refused, never rounded, because rounding somebody's price for them is how a plan quietly sells at the wrong number. `0` is allowed: a free plan is a real plan. |
| `currencyCode` | String, required | **Any ISO 4217 code**, trimmed and uppercased — `INR`, `USD`, `AED`. **Not an enum on purpose**: the set is the world's, not ours, and it is checked against `java.util.Currency` instead. Unknown → `409 CURRENCY_INVALID`. |
| `maxStudents` | Long, required | **`1` or more.** `0` → `400 LIMIT_TOO_LOW`, since a plan nobody can enrol a student on is not a plan. |
| `maxUsers` | Long, required | **`1` or more**, same rule. |
| `effectiveFrom` | Instant, optional | **Any instant, or absent.** If absent when #4 publishes, publish fills it with the moment of publishing. Must be strictly before `effectiveUntil` → else `400 INVALID_SELLING_WINDOW`. |
| `effectiveUntil` | Instant, optional | **Any instant after `effectiveFrom`, or absent** — absent means the plan sells until retired. #6 sets it when retiring. Already in the past at publish → `409 PLAN_WINDOW_ALREADY_CLOSED`. |
| `publiclyAvailable` | Boolean, required | **`false`** at create (#1); **`true` or `false`** from #7. `false` does not mean unsellable — it means quote-only. |
| `features` | List, required | **`[]`** at create; #3 replaces the whole list. Rows below. |

### `plan_definitions.features[]` — [PlanFeature](../../models/plans/embedded/PlanFeature.java)

| Field | Type | What can be in it |
|---|---|---|
| `featureCode` | [FeatureCode](../../models/plans/enums/FeatureCode.java), required | One of **24**, and each may appear once per plan — a repeat is `400 DUPLICATE_FEATURE`. `STUDENT_MANAGEMENT` `ACADEMICS` `ATTENDANCE` `TIMETABLE` `EXAMINATIONS` `HOMEWORK` `FEE_MANAGEMENT` `PAYROLL` `STAFF_MANAGEMENT` `ADMISSIONS_CRM` `TRANSPORT` `LIBRARY` `HOSTEL` `MESS` `HEALTH` `FRONT_OFFICE` `INVENTORY` `PROCUREMENT` `FACILITIES` `NOTIFICATIONS` `DOCUMENTS` `GALLERY` `FEEDBACK` `STUDENT_LIFE` |
| `enabled` | Boolean, required | **`true`** or **`false`**. `false` is deliberate, not a deletion: it lists the feature as *not* included so a comparison table can show a cross rather than a gap. |
| `usageLimit` | Long, optional | **`1` or more, or null.** Null means no numeric cap. `0` with `enabled: true` → `400 FEATURE_LIMIT_ZERO`; negative → `400 FEATURE_LIMIT_NEGATIVE`. A limit on a feature that counts nothing → `400 FEATURE_NOT_MEASURABLE`. |
| `usageMetric` | [UsageMetric](../../models/plans/enums/UsageMetric.java), optional | **Not accepted from the caller** — copied from the feature and frozen. Null whenever `usageLimit` is null. `ACTIVE_STUDENTS` `ACTIVE_STAFF` `USER_ACCOUNTS` `VEHICLES` `HOSTEL_BEDS` `LIBRARY_TITLES` `STORAGE_MEGABYTES` `SMS_MESSAGES` `EMAIL_MESSAGES` |
| `overagePolicy` | [OveragePolicy](../../models/plans/enums/OveragePolicy.java), required | **`BLOCK`** (the default) · `WARN` · `ALLOW` · `CHARGE` |

### `school_subscriptions` — [SchoolSubscription](../../models/plans/SchoolSubscription.java)

| Field | Type | What can be in it |
|---|---|---|
| `subscriptionNo` | String, required | **`SUB/2026/09/000001`** — prefix `SUB/{YYYY}/{MM}/`, six digits zero-padded, allocated by `NumberSequenceService` and never chosen by the caller. (The model's `// Example:` comment still shows `SUB/2026/000001`, from before the house format took a month — the service is what writes, and it writes the month.) |
| `planDefinitionDocsId` | String, required | **The `_id` of an `ACTIVE` plan version.** A `DRAFT` or `RETIRED` plan is refused. |
| `planVersion` | Integer, required | **Copied from the plan**, not sent. |
| `status` | [SubscriptionStatus](../../models/plans/enums/SubscriptionStatus.java), required | **`TRIAL`** when the request says `trial: true`, otherwise **`ACTIVE`** (#13). **`TRIAL`** or **`ACTIVE`** on create, from the `trial` flag (#13). Later moves have their own endpoints where the transition matters: **#19** suspends (`ACTIVE`/`PAST_DUE` → `SUSPENDED`), **#20** resumes (`SUSPENDED` → `ACTIVE`) — both carrying the school's status with them — and **#21** ends it (`CANCELLED`). **#14** is the override for everything else, including `TRIAL` → `ACTIVE`, and it applies no transition rules at all. **Nothing pushes a subscription into `PAST_DUE` or `EXPIRED`**: both are the passage of time noticing something rather than a decision, so a **job** will do them. [#18](#t18) and [#22](#t22) were dropped for that reason. |
| `billingCycle` | BillingCycle, required | **From the plan by default, or the cycle named on #13's request.** Same five values. It lives here rather than being read through to the plan precisely so a school can be sold a plan on a cadence the plan is not listed at — and so changing the plan's listed cycle later cannot silently re-bill every school on it. #14 can move it afterwards. |
| `currentPeriodStart` | Instant, required | **Today or later, never the past.** Absent on create means midnight at the start of today **in the school's own zone**, not the moment the request arrived — a billing period is a pair of dates somebody reads. Sending one is how a contract that begins later is recorded; sending one already past is `400 PERIOD_START_IN_PAST` on both #13 and #14. The stored value goes stale as the period runs, and that is fine — the rule applies to a value being set. |
| `currentPeriodEnd` | Instant, required | **Start plus a fixed count of days** taken from the cycle — `MONTHLY` 30, `QUARTERLY` 90, `HALF_YEARLY` 180, `YEARLY` 365. Equal periods rather than equal dates, with the drift that implies; see the note under [#13](#e13). **`CUSTOM` has no length**, so the caller must send it — `400 BILLING_PERIOD_END_REQUIRED` if they do not, and it has to be after the start. On the four fixed cycles the caller may **not** send one: `400 BILLING_PERIOD_END_NOT_ALLOWED`. |
| `autoRenew` | Boolean, required | **`true`** unless the request says otherwise. |
| `contractedPrice` | BigDecimal, `DECIMAL128`, required | **The plan's `listPrice`, or an override** — same rules: `0` or more, at most 2 decimals. This is what makes a private discount possible without a new plan version. |
| `currencyCode` | String, required | **The plan's currency, never the caller's.** A subscription priced in a different currency from its plan is a mistake nobody would catch until an invoice went out in the wrong money. |
| `maxStudentsOverride` | Long, optional | **A number.** #13 always writes one, copying the plan's `maxStudents` when the sale named none, so the subscription answers "what may this school use" on its own. Null means fall back to the plan and is what #14 stores when an override is removed — no longer the ordinary state of a new subscription. |
| `maxUsersOverride` | Long, optional | **A number**, same as `maxStudentsOverride`: copied from the plan's `maxUsers` on create unless the sale named one. |
| `current` | Boolean, required | **`true`** on create, and on the row #16 opens. Exactly one row per school may be `true`; the flag is what makes "the school's subscription" a single document rather than a sort by date. **`false`** on a row #16 has closed — one row per plan period, so a school that has changed plan twice has three rows. |
| `billingCustomerReference` | String, optional | **Open** — the gateway's own customer id, e.g. `customer_Qx7B2mR9`, or null until there is one. |
| `reasonForChanges` | String, optional | Free text, max 500. **Written by #14 from its `reason`, on every edit, overwritten each time.** That request field is **required**, so an edited subscription always carries one — null here means nothing has ever edited it. Replaced `cancelledAt` and `cancellationReason` (2026-09-07): the date duplicated the `CANCELLED` history row's `effectiveAt`, and a cancellation-only reason left every other change unexplained. |

### `subscription_history` — [SubscriptionHistory](../../models/plans/SubscriptionHistory.java)

One row per thing that happened. Nothing in this collection is ever updated: a correction is a
new row, because a history you can edit is not a history.

| Field | Type | What can be in it |
|---|---|---|
| `schoolSubscriptionDocsId` | String, required | **The `_id` of the subscription this happened to.** |
| `eventType` | [SubscriptionEventType](../../models/plans/enums/SubscriptionEventType.java), required | **`CREATED`** and **`TRIAL_STARTED`** are written by #13. **#14 writes any of seven**: `TERMS_CHANGED` when the status did not move, and otherwise the one that names the status it moved to — `TRIAL_STARTED` `ACTIVATED` `RESUMED` `PAYMENT_PAST_DUE` `SUSPENDED` `CANCELLED` `EXPIRED`. **`PLAN_CHANGED`** is written by #16, **`RENEWED`** by #17, and **`SUSPENDED`**/**`RESUMED`** by #19 and #20 — which are the only writers of those two that apply the transition's own rules. Every value is written by something. |
| `previousPlanDefinitionDocsId` | String, optional | **Null on the first row**; the plan moved off, on a `PLAN_CHANGED`. |
| `previousStatus` | SubscriptionStatus, optional | **Null on the first row** — there was no previous status. Otherwise any of the six. |
| `newPlanDefinitionDocsId` | String, optional | **The plan moved to.** |
| `newStatus` | SubscriptionStatus, required | **`TRIAL`** or **`ACTIVE`** today; any of the six once the lifecycle endpoints exist. |
| `reason` | String, optional | **Open** — free text from the caller, or null. |
| `source` | String, required | **`ADMIN_PORTAL`** is the only value written today, from a constant in `PlatformSubscriptionService`. **The set is closed in intent but open in the type** — a `String`, not an enum, so a typo would be stored and a later "who did this" report would silently miss the row. The other sources this is meant to distinguish are the school's own portal, the payment gateway's webhook, and the scheduled jobs (#70–71); it should become an enum when the second one appears. |
| `sourceEventId` | String, optional | **Open** — the id of whatever caused this outside our system, so a row can be traced back to it: a `billing_webhook_events` `providerEventId` for a gateway event, a job run id for #70–71. **Null for anything a person did**, which is every row today, since `ADMIN_PORTAL` is the only source. It is what makes a webhook replay safe to detect — same `sourceEventId`, same event, do not write it twice. |
| `effectiveAt` | Instant, required | **When the change took effect**, which is not when the row was written: a cancellation agreed today for the end of the period is dated at the end of the period. `createdAt` is the write time and Spring Data fills that. |
| `performedByDocsId` | String, optional | **Null for anything automated** — and null on every row today, because #13 does not yet resolve the acting account. Otherwise the `_id` of the identity that acted. |

### `subscription_invoices` — [SubscriptionInvoice](../../models/plans/billing/SubscriptionInvoice.java)

Nothing writes this yet — #39–46 are not built. The values below are what those endpoints will write.

| Field | Type | What can be in it |
|---|---|---|
| `invoiceNo` | String, required | `SINV/2026/09/000001` — the house format `XXX/{YYYY}/{MM}/`, from the same allocator. (The model comment still shows the pre-`{MM}` `SINV/2026/000001`.) |
| `schoolSubscriptionDocsId` | String, required | The `_id` of the subscription being billed. |
| `billingPeriodStart` / `billingPeriodEnd` | LocalDate, required | The period the invoice covers — dates, not instants: an invoice is for a period, not a moment. |
| `issueDate` / `dueDate` | LocalDate, required | `dueDate` on or after `issueDate`. |
| `status` | [SubscriptionInvoiceStatus](../../models/plans/billing/enums/SubscriptionInvoiceStatus.java), required | `DRAFT` → `ISSUED` → `PARTIALLY_PAID` → `PAID`, with `OVERDUE` when the due date passes unpaid and `VOID` as the only way to undo an issued invoice. |
| `currencyCode` | String, required | ISO 4217, copied from the subscription. |
| `subTotal`, `taxAmount`, `totalAmount`, `paidAmount`, `outstandingAmount` | BigDecimal, `DECIMAL128`, required | `0` or more, scale 2. `totalAmount` = `subTotal` + `taxAmount`; `outstandingAmount` = `totalAmount` − `paidAmount`, and reaching `0` is what moves the status to `PAID`. |
| `gatewayInvoiceReference` | String, optional | **Open** — the gateway's id, or null when the invoice was never sent to one. |
| `paidAt`, `issuedAt`, `voidedAt` | Instant, optional | Null until each happens; each one is the moment of the status change that names it. |
| `voidReason` | String, optional | **Open** — free text, required in practice whenever `voidedAt` is set. |

### `subscription_payments` — [SubscriptionPayment](../../models/plans/billing/SubscriptionPayment.java)

Nothing writes this yet — #55–65 are not built.

| Field | Type | What can be in it |
|---|---|---|
| `paymentNo` | String, required | `SPAY/2026/09/000001`, same house format. (The model comment still shows the pre-`{MM}` `SPAY/2026/000001`.) |
| `schoolSubscriptionDocsId`, `subscriptionInvoiceDocsId` | String, required | The subscription and the invoice this pays. |
| `status` | [SubscriptionPaymentStatus](../../models/plans/billing/enums/SubscriptionPaymentStatus.java), required | `PENDING` · `SUCCEEDED` · `FAILED` · `CANCELLED` · `PARTIALLY_REFUNDED` · `REFUNDED` |
| `paymentMethod` | [SubscriptionPaymentMethod](../../models/plans/billing/enums/SubscriptionPaymentMethod.java), required | `UPI` · `CARD` · `NET_BANKING` · `BANK_TRANSFER` · `DIRECT_DEBIT` · `WALLET` · `CHEQUE` · `CASH` · `OTHER` |
| `amount` | BigDecimal, `DECIMAL128`, required | Greater than `0`, scale 2. |
| `currencyCode` | String, required | ISO 4217, and it must match the invoice's. |
| `gatewayProvider` | String, optional | **Open** — `RAZORPAY` today. Null for a payment taken outside a gateway, which is what `CHEQUE` and `CASH` are for. |
| `gatewayPaymentReference`, `gatewayOrderReference`, `settlementReference` | String, optional | **Open** — the gateway's own ids, null when there is no gateway. |
| `receivedAt`, `settledAt` | Instant, optional | Money received, and money actually in the account — days apart, and the pair is the reason both exist. |
| `failureReason` | String, optional | **Open** — free text, set with `FAILED`. |

### `subscription_payment_attempts` — [PaymentAttempt](../../models/plans/billing/PaymentAttempt.java)

Nothing writes this yet. One row per try, kept even when it fails — a payment that took four
attempts is a support call, and the four rows are the answer to it.

| Field | Type | What can be in it |
|---|---|---|
| `subscriptionInvoiceDocsId` | String, required | The invoice being paid. |
| `subscriptionPaymentDocsId` | String, optional | Null until the attempt succeeds, then the payment it produced. |
| `attemptNo` | Integer, required | `1` upward, per invoice. |
| `status` | [PaymentAttemptStatus](../../models/plans/billing/enums/PaymentAttemptStatus.java), required | `INITIATED` → `PROCESSING` → `SUCCEEDED`, with `REQUIRES_ACTION` for 3-D Secure and the like, and `FAILED` / `CANCELLED` as the two ways it ends badly. |
| `paymentMethod` | SubscriptionPaymentMethod, required | The nine above. |
| `amount` | BigDecimal, `DECIMAL128`, required | Greater than `0`, scale 2. |
| `currencyCode` | String, required | ISO 4217. |
| `gatewayProvider` | String, required | **Open** — `RAZORPAY` today. Required here, unlike on the payment: an attempt only exists because a gateway was called. |
| `idempotencyKey` | String, required | `subscription-invoice-{id}-attempt-{n}` — **the field that stops a double charge.** Sent to the gateway so a retried request is recognised as the same one rather than taking the money twice. |
| `gatewayAttemptReference` | String, optional | **Open** — the gateway's id for this try. |
| `failureCode` | String, optional | **Open, and the gateway's vocabulary, not ours** — `CARD_DECLINED`, `INSUFFICIENT_FUNDS`. Stored verbatim so it can be matched against their documentation. |
| `failureMessage` | String, optional | **Open** — the gateway's sentence, for a human to read. |
| `attemptedAt`, `completedAt` | Instant | `attemptedAt` required; `completedAt` null while the attempt is still open. |

### `billing_webhook_events` — [BillingWebhookEvent](../../models/plans/billing/BillingWebhookEvent.java)

Nothing writes this yet — #66–69 are not built.

| Field | Type | What can be in it |
|---|---|---|
| `gatewayProvider` | String, required | **Open** — `RAZORPAY` today. |
| `providerEventId` | String, required | **Open** — the gateway's event id, e.g. `evt_R7pw2V6n8`. **Unique per provider**: it is what makes a replayed webhook a no-op instead of a second refund. |
| `providerEventType` | String, required | **Open, and theirs** — `payment.captured`, `payment.failed`. Not an enum, because the gateway adds event types without asking us. |
| `processingStatus` | [WebhookProcessingStatus](../../models/plans/billing/enums/WebhookProcessingStatus.java), required | `RECEIVED` (the default, written before anything is trusted) → `VERIFIED` → `PROCESSED`, with `IGNORED` for events we do not act on, `FAILED` for a retryable error, and `DEAD_LETTER` once the retries are exhausted. |
| `signatureValid` | Boolean, required | `false` until the signature is checked. **The row is written before verification**, so an event with a forged signature is still on record. |
| `payloadHash` | String, required | `sha256:…` of the raw body. |
| `encryptedPayload` | String, required | The body, encrypted at rest — a webhook carries payment details, so it is not stored in the clear. |
| `relatedEntityType` | String, optional | **Open** — `SUBSCRIPTION_PAYMENT`, `SUBSCRIPTION_INVOICE`, or null before the event is matched to anything. |
| `relatedEntityDocsId` | String, optional | The `_id` of that thing, null until matched. |
| `processingAttemptCount` | Integer, required | `0` upward; what `DEAD_LETTER` is decided on. |
| `receivedAt`, `processedAt`, `nextRetryAt` | Instant | `receivedAt` required; the others null until they happen. |
| `failureCode`, `failureMessage` | String, optional | **Open, and ours here, not the gateway's** — `INVOICE_NOT_FOUND` and a sentence explaining it. |

### `number_sequences` — [NumberSequence](../../models/institution/NumberSequence.java)

Shared with the rest of the platform; the plans module uses three of its types.

**One document per school holding a `counters` array**, restructured 2026-09-05. The fields
below are on the array entries, not the document. Allocation is a `findAndModify` with `$inc`
through the positional operator, owned by `NumberSequenceRepositoryCustom` — nothing in this
module writes the collection directly.

| Field | Type | What can be in it |
|---|---|---|
| `counters[].sequenceType` | [NumberSequenceType](../../models/institution/enums/NumberSequenceType.java), required | 48 values across the whole platform. This module uses **`SUBSCRIPTION`**, and will use `SUBSCRIPTION_INVOICE` and `SUBSCRIPTION_PAYMENT`. |
| `scopeKey` | String, required | **`GLOBAL`** today, and the partner of `resetPolicy`: that says *how* the scope is worked out, this is the answer. `NEVER` → `GLOBAL`; `ACADEMIC_YEAR` → `2026-2027`; `CALENDAR_YEAR` → `2026`; `MONTHLY` → `2026-09`. **Deliberately not an enum**: `2026-2027` is decided by the calendar, not by us, so an enum would need a new constant every April — and the year that deploy was late, nobody could be admitted. |
| `prefixTemplate` | String, optional | **`SUB/{YYYY}/{MM}/`** for subscriptions. `{YYYY}` four-digit year, `{YY}` last two, `{MM}` zero-padded month, resolved when the number is made and then **stored on the row**, so numbering cannot change shape mid-run. **It must end in a separator** — `SUB/{YYYY}/{MM}` produces `SUB/2026/09000001`, with the month running into the digits. |
| `suffixTemplate` | String, optional | **`""`** — nothing uses it yet. |
| `nextValue` | Long, required | **`1` upward.** Raised by `findAndModify` with `$inc`, never read-add-save: two overlapping callers doing that are handed the same number. |
| `paddingWidth` | Integer, required | **`6`** — `000001`. A number longer than the padding is not truncated; it just gets longer. |
| `resetPolicy` | [SequenceResetPolicy](../../models/institution/enums/SequenceResetPolicy.java), required | **`NEVER`** in practice. `CALENDAR_YEAR` · `ACADEMIC_YEAR` · `MONTHLY` are declared but **nothing reads them yet** — no code opens a new scope, so every row is `NEVER` + `GLOBAL`. |
| `lastResetAt` | Instant, optional | Null. Unused for the same reason. |

### The two collections this module only counts

[`students`](../../models/student/Student.java) and
[`user_accounts`](../../models/identity/UserAccount.java) appear once each, in #35, and are
**only ever counted** — `schoolId` plus `status`, to compare a school's usage against its limits.
No endpoint in this module writes to either.

## The plan catalogue — writes  ·  1–7

<a id="e1"></a>
**[1](#t1) · `POST /platform/plans/drafts`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *insert*: `planCode`, `planVersion` = 1, `name`, `description`, `status` = `DRAFT`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `effectiveFrom`, `effectiveUntil`, `publiclyAvailable` = false, `features` = `[]`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "name": "Premium",                  // REQUIRED, max 120
  "billingCycle": "YEARLY",           // REQUIRED
  "listPrice": 49999.00,              // REQUIRED
  "currencyCode": "INR",              // REQUIRED, exactly 3
  "maxStudents": 2000,                // REQUIRED
  "maxUsers": 250,                    // REQUIRED

  "planCode": "PREMIUM",              // optional, max 40
  "description": "Everything.",       // optional, max 500
  "effectiveFrom": null,              // optional
  "effectiveUntil": null              // optional
}
</pre></td>
<td><pre>
201 Created

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium",
  "description": "Everything.",
  "status": "DRAFT",
  "billingCycle": "YEARLY",
  "listPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250,
  "effectiveFrom": null,
  "effectiveUntil": null,
  "publiclyAvailable": false,
  "featureCount": 0,
  "sellable": false,
  "nextStep": "Draft created. Nobody can buy it yet: set its features, then publish it."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | **yes** | Max 120, `@NotBlank`. What a school reads. |
| `billingCycle` | **yes** | `MONTHLY`, `QUARTERLY`, `HALF_YEARLY`, `YEARLY` or `CUSTOM`. Decides how long a subscription's period runs — see #13 — and `CUSTOM` makes the period end a required field there. |
| `listPrice` | **yes** | The public price. Zero is allowed; negative is `400 PRICE_NEGATIVE`. A JSON number, never a string. |
| `currencyCode` | **yes** | ISO 4217, exactly 3, `@NotBlank`. Normalised to upper case, and checked against the real currency list — `XYZ` is `409 CURRENCY_INVALID`. Every subscription sold on this plan inherits it. |
| `maxStudents` | **yes** | The plan's student ceiling, at least 1 — `0` is `400 LIMIT_TOO_LOW`. #13 copies it onto every subscription sold. |
| `maxUsers` | **yes** | The same for staff accounts. |
| `planCode` | no | The family key, max 40. **Absent derives one from the name** — "Premium Plus" becomes `PREMIUM_PLUS` — so a caller does not have to invent a key. Sending one that **already exists is `409 PLAN_CODE_TAKEN`**: this endpoint only ever makes version 1 of a new family. A second version of an existing plan comes from #5, which copies a published version into a fresh draft. |
| `description` | no | Max 500. Absent means null; a school sees no description. |
| `effectiveFrom` | no | When it may start being sold. Absent means it is settled at publish time (#4), not here — a draft is not on sale at all. |
| `effectiveUntil` | no | When it stops being sold. Absent means never, until somebody retires it. |

**`status` is not on the request.** Every plan starts `DRAFT`. Publishing is #4, which checks the
plan is complete first; a caller who could ask for `ACTIVE` would skip that.

**Two things came out differently from this plan, both deliberate:**

- **The path is `/platform/plans/drafts`**, not `/platform/plans`, so nobody can read the URL and
  think they are putting a plan on sale. Everything after #1 addresses the plan by code and
  version, because from then on draft-ness is a status on a plan that exists.
- **`planCode` is not sent.** It is derived from `name` — "Premium Plus" becomes `PREMIUM_PLUS` —
  so a create form asks for one thing rather than the same words twice in two shapes. An explicit
  code is still accepted for when the derived one is taken. The field stays on the model because
  it is the **family key**: the only thing joining v1, v2 and v3 of a plan, which an editable
  `name` cannot be.
- **`features` is not accepted either.** A plan is created with an empty list and #3 sets it, the
  same shape academic years use for holidays: a create that can fail on either a bad price or a
  bad feature leaves the caller working out which, and a part-filled feature list is the "plan
  nobody can price" that #3 exists to prevent.

<a id="e2"></a>
**[2](#t2) · `PATCH /platform/plans/{code}/versions/{version}`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status` — must be `DRAFT` or the edit is refused
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `effectiveFrom`, `effectiveUntil`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "name": "Premium Plus",             // optional, max 120
  "description": "",                  // optional — "" clears it
  "billingCycle": "MONTHLY",          // optional
  "listPrice": 44999.00,              // optional
  "currencyCode": "INR",              // optional, exactly 3
  "maxStudents": 3000,                // optional
  "maxUsers": 300,                    // optional
  "sellingWindow": {                  // optional, replaced as a pair
    "effectiveFrom": "2026-04-01T00:00:00Z",
    "effectiveUntil": null
  }
}

// Nothing is required, but a body with
// nothing in it is a 400: answering 200
// would tell a caller who misspelled a
// field that their edit worked.
</pre></td>
<td><pre>
200 OK — the draft as it now stands (PlanResponse)

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium Plus",
  "description": null,
  "status": "DRAFT",
  "billingCycle": "MONTHLY",
  "listPrice": 44999.00,
  "currencyCode": "INR",
  "maxStudents": 3000,
  "maxUsers": 300,
  "effectiveFrom": "2026-04-01T00:00:00Z",
  "effectiveUntil": null,
  "publiclyAvailable": false,
  "featureCount": 2,
  "sellable": false,
  "nextStep": "Draft updated. Nobody can buy it yet: set its features, then publish it."
}

409 PLAN_NOT_EDITABLE — it is published
</pre></td>
</tr>
</table>

**Every field on the request.** All optional, and absent always means "leave it exactly as it is".

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | no | Max 120. **Cannot be cleared** — it is `@NotBlank` on the model, so `""` here is a `400` rather than a deletion. |
| `description` | no | Max 500. **`""` clears it.** The one field where an empty string is an instruction rather than a mistake. |
| `billingCycle` | no | Any of the five. Nothing derived from it is stored on the draft, so changing it is free until the plan is published. |
| `listPrice` | no | Zero allowed, negative refused. |
| `currencyCode` | no | Validated and upper-cased as on #1. |
| `maxStudents` | no | At least 1. |
| `maxUsers` | no | At least 1. |
| `sellingWindow` | no | **A pair, replaced together.** Omit the block to leave both dates; send it to replace both, and send `null` inside for either to clear that one. Nested because the two are only meaningful next to each other — an `effectiveUntil` moved earlier than the existing `effectiveFrom` is a plan that can never be sold, which a PATCH changing one alone could create. |

**Only a `DRAFT` can be edited**, and that is the whole shape of this endpoint: once a plan is
published a school can be on it, and changing the price of something somebody already bought
would change what they agreed to pay without anybody agreeing to it. `409 PLAN_NOT_EDITABLE`.

**`status`, `publiclyAvailable` and `features` are deliberately absent.** Publishing is #4,
retiring #6, availability #7, features #3 — each a decision with its own rules. A PATCH that could
set them all would make "put this on sale" look identical to "fix a typo".

<a id="e3"></a>
**[3](#t3) · `PUT /platform/plans/{code}/versions/{version}/features`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `features` — the whole list is replaced

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "features": [                       // REQUIRED, the whole list
    {
      "featureCode": "STUDENT_MANAGEMENT",  // REQUIRED
      "enabled": true,                      // optional
      "usageLimit": null,                   // optional
      "overagePolicy": null                 // optional
    },
    {
      "featureCode": "SMS_NOTIFICATIONS",
      "enabled": true,
      "usageLimit": 5000,
      "overagePolicy": "BLOCK"
    }
  ]
}

// A PUT: the list sent IS the list. An
// empty array is allowed and clears them.
</pre></td>
<td><pre>
200 OK — PlanFeatureListResponse

{
  "planCode": "PREMIUM",
  "planVersion": 1,
  "status": "DRAFT",
  "featureCount": 2,
  "features": [
    {
      "featureCode": "STUDENT_MANAGEMENT",
      "label": "Student management",
      "description": "Admissions, records and transfers.",
      "enabled": true,
      "usageLimit": null,
      "usageMetric": null,
      "overagePolicy": null
    },
    {
      "featureCode": "SMS_NOTIFICATIONS",
      "label": "SMS notifications",
      "description": "Texts to guardians.",
      "enabled": true,
      "usageLimit": 5000,
      "usageMetric": "MESSAGES_PER_MONTH",
      "overagePolicy": "BLOCK"
    }
  ],
  "changeSummary": "Replaced the feature list: 0 out, 2 in. Still a DRAFT — publish it to sell it."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `features` | **yes** | `@NotNull` — the list itself must be present. **An empty array is legal** and means "this plan grants nothing", which is a state a draft can be in; publishing one is what #4 refuses. |
| `features[].featureCode` | **yes** | `@NotNull`, from the `FeatureCode` enum. An unknown code is `400 INVALID_VALUE` **naming the position** — *"'NOT_A_FEATURE' is not a valid value for `features[0].featureCode`"* — because "invalid feature" in a list of twelve is not a usable error. The same code twice in one request is `400 DUPLICATE_FEATURE`: each feature is listed once, with one limit. |
| `features[].enabled` | no | Absent means `true`. Sending `false` records the feature as deliberately switched off rather than leaving it out — which is how a plan says "not this one" instead of saying nothing. |
| `features[].usageLimit` | no | How many, for a metered feature. Absent means unmetered — no ceiling on it. **`0` is `400 FEATURE_LIMIT_ZERO`**, because a feature that is enabled and allows nothing is two ways of saying "off, but confusingly": the refusal tells you to send `"enabled": false` instead. The `usageMetric` it counts comes back on the response from the feature's own definition — the plan says how many, the feature says of what. |
| `features[].overagePolicy` | no | What happens past the limit — `BLOCK`, `ALLOW` or `CHARGE`. **Accepted without a `usageLimit`**, where it simply has nothing to act on; it is not refused. |

**It is a `PUT`, not a `POST`.** The list sent is the list the plan ends with: features are not
added one at a time, because a caller who sends the same feature twice by accident should end up
with one, and because "what does this plan grant" has to be answerable by reading one request.
`changeSummary` says what actually moved.

**`label`, `description` and `usageMetric` are never sent.** They belong to the feature's own
definition, the same for every plan, and come back on the response so a screen does not need a
second lookup.

### `featureCode` became an enum while this was built

It was a `String` on [`PlanFeature`](../../models/plans/embedded/PlanFeature.java). That accepted
`STUDNET_MANAGEMENT` with a `200`: the plan looked correct on every screen, and the feature access
service — asking for `STUDENT_MANAGEMENT` — found nothing and locked the school out of what they
had paid for. One transposed letter, discovered when they rang up.

**A feature code points at behaviour in this codebase, not at anything a user invents**, so the
set is closed by definition and belongs in
[`FeatureCode`](../../models/plans/enums/FeatureCode.java). A misspelling is now a `400` naming
the row and listing every accepted value.

| Group | Features |
|---|---|
| Teaching | `STUDENT_MANAGEMENT` `ACADEMICS` `ATTENDANCE` `TIMETABLE` `EXAMINATIONS` `HOMEWORK` |
| Money | `FEE_MANAGEMENT` `PAYROLL` |
| People | `STAFF_MANAGEMENT` `ADMISSIONS_CRM` |
| Daily operations | `TRANSPORT` `LIBRARY` `HOSTEL` `MESS` `HEALTH` `FRONT_OFFICE` |
| Stores and premises | `INVENTORY` `PROCUREMENT` `FACILITIES` |
| Communication | `NOTIFICATIONS` `DOCUMENTS` `GALLERY` `FEEDBACK` `STUDENT_LIFE` |

**Not on the list:** the tenant itself, accounts and sign-in, the audit trail, and the plan and
billing machinery. Every plan includes those and nobody is charged for them separately — a
feature nobody can be sold is not a feature, and listing one invites somebody to switch it off.

**The rule for changing the list: add constants, never rename or remove one.** The name is what
is stored in every existing plan, and a published version is immutable — a rename would orphan
the feature on every plan already sold, silently, with the rows still looking valid. Adding one
needs a deploy, which costs nothing: the software has to be able to do the new thing before a
plan can sell it.

### Each feature declares its own metric, so `usageMetric` left the request

A limit is a bare number. [`UsageMetric`](../../models/plans/enums/UsageMetric.java) says what it
counts, and **the feature knows** — `TRANSPORT` in `VEHICLES`, `STUDENT_MANAGEMENT` in
`ACTIVE_STUDENTS`. So callers no longer send one, and "student management limited to 2000
gigabytes" is not refused by a rule; it cannot be written down.

**It is stored on the row, not looked up on read.** If `TRANSPORT` were ever changed from
`VEHICLES` to `ROUTES`, a plan sold last year would silently become a different contract — same
row in the database, different meaning. Copying the metric in when the plan is written freezes
it, which is the same immutability the rest of this group is built on.

**A feature with nothing to count refuses a limit.** `ATTENDANCE` is included or it is not; a
limit of 500 on it would be a number nothing reads, and a plan that reads as capped and behaves
as unlimited is worse than one with no cap at all. This replaced the earlier "a limit needs a
metric" check, which could only refuse the mismatch after the fact.

### What #3 refuses

| Case | Code |
|---|---|
| unknown or misspelled feature | `400 INVALID_VALUE`, with the accepted values |
| the same feature twice | `400 DUPLICATE_FEATURE` |
| a limit on a feature with nothing to count | `400 FEATURE_NOT_MEASURABLE` |
| `enabled: true` with a limit of 0 | `400 FEATURE_LIMIT_ZERO` |
| a negative limit | `400 FEATURE_LIMIT_NEGATIVE` |
| no `features` key at all | `400`, so a forgotten field cannot wipe the list |
| the plan is not a `DRAFT` | `409 PLAN_NOT_EDITABLE` |

`{ "features": [] }` empties the list, which is why there is no separate delete.

### The body is an object, not a bare array

`{ "features": [ … ] }`. A bare array made Spring report a bad row as a Java method signature and
an error count; as an object it validates like every other endpoint, and a bad row comes back as
`features[1].featureCode`. The holiday calendar in `core` was changed the same way and for the
same reason.

<a id="e4"></a>
**[4](#t4) · `POST /platform/plans/{code}/versions/{version}/publish`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`, `features`, `effectiveUntil`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `status` = `ACTIVE`, `effectiveFrom` if it was empty

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "effectiveFrom": "2026-04-01T00:00:00Z",   // optional
  "effectiveUntil": null                     // optional
}

// The body itself is optional. Send none
// and it goes on sale now, for ever.
</pre></td>
<td><pre>
200 OK

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium",
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "listPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250,
  "effectiveFrom": "2026-04-01T00:00:00Z",
  "effectiveUntil": null,
  "publiclyAvailable": false,
  "featureCount": 2,
  "sellable": false,
  "nextStep": "Published, and now permanent: this version can never be edited again. It is NOT on the public list yet — it can only be offered privately in a quote until #7 puts it there."
}

409 PLAN_HAS_NO_FEATURES — nothing to sell
409 PLAN_NOT_DRAFT      — already published
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `effectiveFrom` | no | When it may start being sold. **Absent means now**, so a plain publish puts it on sale immediately. A future date schedules it — published, but `sellable` false until then, and #13 refuses it with `409 PLAN_NOT_SELLABLE` in the meantime. |
| `effectiveUntil` | no | When it stops being sold. **Absent means never** — it sells until somebody retires it with #6. |

**The body as a whole is optional** (`@RequestBody(required = false)`), so `POST` with no body at
all publishes with both defaults. Both fields exist because publishing is a **one-way door**: a
published plan cannot be edited, so the selling window it goes out with is permanent, and a
dialog that never asked would set it silently.

**A plan with no features is refused.** A school buying it would get nothing, and the refusal
names the fix rather than the rule: *"Set its features first."*

### The one-way door, and what makes it safe to walk through

This is the endpoint after which nothing can be changed. #2 and #3 both refuse a plan that is not
a `DRAFT`, and **there is no unpublish** — a school can be on the plan from the moment it goes
live, and editing what they bought after they bought it is what this whole group is arranged to
prevent. A new price is #5, a new version, and the schools on this one stay where they are.

Because it cannot be undone, it is **checked rather than trusted**:

| Refused | Code | Why |
|---|---|---|
| the plan has no features | `409 PLAN_HAS_NO_FEATURES` | a school would pay and be granted nothing |
| its selling window has already closed | `409 PLAN_WINDOW_ALREADY_CLOSED` | it could never be bought |
| it is already `ACTIVE` | `409 PLAN_ALREADY_PUBLISHED` | see below |
| it is `RETIRED` | `409 PLAN_NOT_EDITABLE` | retiring is not a way back to draft |

**The completeness check is only about features.** The plan spec listed `listPrice`,
`currencyCode`, `maxStudents` and `maxUsers` as things to verify here, and they need no check:
all four are `@NotNull` on the model and validated by #1 and #2, so a draft cannot exist without
them. `features` is the only one that can legitimately be empty, and the only one worth a guard.

**Publishing twice is a `409`, not an idempotent `200`.** The enrollment and results gates in
`core` are idempotent because a no-op there is harmless. Here it is the opposite: "it was already
published" and "you just published it" are different facts about the one action that cannot be
undone, and a caller who cannot tell them apart will assume the wrong one.

### Publishing is not listing

`publiclyAvailable` is untouched, so straight after publishing the plan is `ACTIVE` and
`sellable` is still **false**. A published plan is real and can be offered privately in a quote;
whether it appears on the pricing page is #7's decision. Two decisions, two endpoints.

### `effectiveFrom`

Stamped with now if it was empty. A future date set while the plan was a draft is **kept**, so a
scheduled launch works: the plan becomes `ACTIVE` immediately and `sellable` only when the window
opens. The response's `nextStep` names the date when that is the case.

<a id="e5"></a>
**[5](#t5) · `POST /platform/plans/{code}/versions/{version}/new-version`** — DEFERRED

Deferred on 2026-09-03, by decision. The design below stands and is what to build from.

**What being without it means.** #1 to #4 make a plan and freeze it. Nothing then reopens it:

- #2 refuses a published plan — *"Make a new version of it instead."*
- #3 refuses its features — *"Make a new version of it instead."*
- #4 refuses a second publish — *"To change it, make a new version."*
- and #4's success message ends *"To change the price, make a new version."*

Four messages point at an endpoint that does not exist. They are left as they are on purpose: they
describe the design, and softening them to "you cannot change this" would be wrong the moment #5
is built. But **a published plan is currently permanent in the strongest sense** — the only way to
sell at a different price is #1, a brand-new plan with its own code, which is not the same thing
and leaves no link between the old price and the new one.

**What #5 has to do when it is built** — and the hard part is not the copy:

- insert a new document with the **same** `planCode`, `planVersion` one higher, `status` =
  `DRAFT`, and `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`,
  `maxUsers` and `features` copied from the version being copied
- `publiclyAvailable` = false and both selling-window dates cleared, because they are decisions
  about the new version rather than facts inherited from the old one
- **the version number must be one higher than the highest that exists**, not one higher than the
  version being copied. Copying v1 when v2 already exists must not try to create a second v2 —
  the unique index on `{planCode, planVersion}` would refuse it, but with a duplicate-key error
  rather than anything a caller could act on.
- **only one draft per plan at a time.** Two open drafts of `PREMIUM` is two answers to "what are
  we about to sell", and whichever is published second silently wins.

`PlanDefinitionRepository.findByPlanCodeOrderByPlanVersionDesc` already exists for exactly this
— it was written with #5 and #9 in mind and is currently unused.

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: every field of the `version` being copied
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *insert*: `planCode` — the same, `planVersion` — one higher, `status` = `DRAFT`, and `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `features` copied from the old `version`

<a id="e6"></a>
**[6](#t6) · `POST /platform/plans/{code}/versions/{version}/retire`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `status` = `RETIRED`, `effectiveUntil`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — POST with nothing.

Path:  {code}     the plan family
       {version}  which version
</pre></td>
<td><pre>
200 OK

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium",
  "status": "RETIRED",
  "billingCycle": "YEARLY",
  "listPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250,
  "effectiveFrom": "2026-04-01T00:00:00Z",
  "effectiveUntil": null,
  "publiclyAvailable": false,
  "featureCount": 2,
  "sellable": false,
  "nextStep": "Retired, and no longer on the menu: no school can pick it from here. Schools already on it keep it."
}

409 PLAN_NOT_ACTIVE — a draft is deleted, not retired
</pre></td>
</tr>
</table>

**No request fields**, and that is the design: retiring takes a plan off the menu and there is
nothing to configure about it. What it does **not** do is the part worth stating —

- **Schools already on it keep it.** Retiring stops new sales; it does not move anybody. The
  `nextStep` counts how many schools that is, because "can I retire this" usually means "who is
  on it".
- **It cannot be undone**, and there is no endpoint to un-retire. #5 copies a retired version
  into a new draft, which is the honest way back: a new version with its own number, rather than
  a price somebody thought was withdrawn quietly coming back.
- **A `DRAFT` cannot be retired.** Nothing was ever on sale, so there is nothing to withdraw —
  `409 PLAN_NOT_ACTIVE`.
- **Nothing else.** No subscription, no invoice, no school is touched.

### It is about the menu, not about anybody's subscription

Schools already on the plan keep it — same price, same features — and nothing about their
subscription changes. Retiring says only that the plan is no longer something a school can pick.

**That distinction is the whole endpoint.** Retiring a popular plan is a routine commercial
decision. If it reached into subscriptions it would cut off every school on it at once, on a
single call that reads like a catalogue tidy-up. Cancelling a school is #19, one school at a
time, on purpose.

### A draft can be retired too

Not in the original spec, and added deliberately: **no endpoint in this module deletes anything**,
so without it a draft created by mistake would sit in the catalogue for ever with its
`planCode` permanently taken. Nobody is on a draft, so withdrawing one costs nothing.

The response distinguishes the two, because they are different facts:

| Was | Response says |
|---|---|
| `DRAFT` | *"Withdrawn. It was still a draft, so it was never sold to anybody… Its plan code stays taken."* |
| `ACTIVE` | *"Retired, and no longer on the menu… Schools ALREADY on it keep it…"* |

**It does not release the `planCode`.** Retiring is not deleting, and the unique index still
holds — so a mistaken draft can be got out of the way but its code is spent.

### Terminal, and everything after it is refused

| Then | Code |
|---|---|
| retire again | `409 PLAN_ALREADY_RETIRED` |
| `PATCH` the details (#2) | `409 PLAN_NOT_EDITABLE` |
| `PUT` the features (#3) | `409 PLAN_NOT_EDITABLE` |
| publish (#4) | `409 PLAN_NOT_EDITABLE` |

There is no un-retire endpoint, in this plan or in the code. Retiring is not a way back to draft.

All three refusals end *"Make a new version of it instead"* — which is #5, and #5 is deferred, so
that advice cannot be followed today.

### `effectiveUntil`

Set to now, **unless it is already in the past**, in which case it is kept: that is when the plan
actually stopped being sold, and moving the date forward would rewrite it. A date in the future
is brought forward to now — it stops being sold now, not in 2030.

### `publiclyAvailable` is left alone

It belongs to #7. Touching it here would make no difference anyway: every list of buyable plans
filters on `ACTIVE` first, so a retired plan is off the pricing page whatever that flag says.

<a id="e7"></a>
**[7](#t7) · `PATCH /platform/plans/{code}/versions/{version}/availability`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `publiclyAvailable`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "publiclyAvailable": true           // REQUIRED
}
</pre></td>
<td><pre>
200 OK

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium",
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "listPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250,
  "effectiveFrom": "2026-04-01T00:00:00Z",
  "effectiveUntil": null,
  "publiclyAvailable": true,
  "featureCount": 2,
  "sellable": true,
  "nextStep": "Now on the public list."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `publiclyAvailable` | **yes** | `@NotNull`, so there is no default and no "toggle" — the caller says which state they want. `true` puts the plan on the list schools can see (#11); `false` takes it off. |

**Why it is `@NotNull` rather than a toggle.** A toggle endpoint answers differently depending on
what it found, so two callers racing each other end up with the state neither asked for. Naming
the wanted state makes the request idempotent: sending `true` twice leaves it public.

**Not public is not the same as not sellable.** A published plan that is off the list is exactly a
**private quote**, and #13 sells it happily — `sellable` on the response tracks the selling
window and the status, not this flag. The distinction is why `PlanResponse` returns both.

### Public list, or private quote

The difference between a plan a school can find and pick for itself, and one that only exists in
a quote somebody sends them. A bespoke price for one large trust is a real plan — published,
sellable, and deliberately not on the pricing page. That is why it is a separate decision from
publishing, and a separate endpoint.

### `sellable` is three facts, and this owns one

| Fact | Set by |
|---|---|
| `status` is `ACTIVE` | #4 publish |
| `publiclyAvailable` is true | **#7** |
| today is inside `effectiveFrom`–`effectiveUntil` | #1 or #2 |

`sellable` is derived from all three on every plan response, and **#7's response names whichever
of the other two is still missing**. Without that, somebody who has just made a plan public and
still sees `sellable: false` has no way to tell which fact is short, and the obvious guess is
that the call failed.

### Idempotent, unlike #4 and #6

| | Repeat call | Why |
|---|---|---|
| #4 publish | `409` | one-way door — "it was already published" is a fact the caller needs |
| #6 retire | `409` | one-way door, same reason |
| **#7 availability** | `200`, saying nothing changed | a switch that flips back in one call |

Refusing a repeat here would only teach callers to read first and then race.

### A retired plan cannot be listed, but can be unlisted

`409 PLAN_RETIRED` for the "on" direction: nobody can buy a retired plan, so advertising it would
put something on the pricing page that every purchase would refuse.

**The "off" direction is allowed.** The objection is specifically about advertising, and taking a
retired plan off the list is tidying up. Refusing both would also have meant a message that lies
about half the cases — the first version said "listing it publicly would…" in response to a
request to *unlist* it.

### A draft can be made public

Allowed, so the decision can be made before the plan goes live. It changes nothing on its own —
`sellable` stays false until #4 — and the response says so.

### `publiclyAvailable` is required, and boxed

An omitted `boolean` arrives as `false`, which is indistinguishable from deliberately hiding the
plan. A forgotten field would pull a plan off the pricing page and report success.

## The plan catalogue — reads  ·  8–12

<a id="e8"></a>
**[8](#t8) · `GET /platform/plans`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `name`, `status`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `publiclyAvailable`, `effectiveFrom`, `effectiveUntil`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET. Everything is a query
parameter, and every one is optional.

?status=ACTIVE&status=DRAFT
&planCode=PREMIUM
&name=prem
&publiclyAvailable=true
&search=prem
&page=0
&size=20
&sort=listPrice,desc

A bare call returns the newest twenty.
</pre></td>
<td><pre>
200 OK — a page of PlanSummaryResponse

{
  "content": [
    {
      "planId": "67aa1202dc3f7d0012345678",
      "planCode": "PREMIUM",
      "planVersion": 1,
      "name": "Premium",
      "status": "ACTIVE",
      "billingCycle": "YEARLY",
      "listPrice": 49999.00,
      "currencyCode": "INR",
      "maxStudents": 2000,
      "maxUsers": 250,
      "publiclyAvailable": true,
      "sellable": true,
      "featureCount": 2,
      "effectiveFrom": "2026-04-01T00:00:00Z",
      "effectiveUntil": null,
      "createdAt": "2026-03-02T09:14:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
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
| `status` | no | **Repeatable** — `?status=ACTIVE&status=DRAFT` means either. `OR` within the field, because "show me the ones I can sell and the ones I am still writing" is one question. Absent means every status. A misspelling is `400 INVALID_PARAMETER` listing the accepted values. |
| `planCode` | no | Exact, case-insensitive. Names one family, all its versions. |
| `name` | no | Partial, case-insensitive, on the name alone. |
| `publiclyAvailable` | no | `true` or `false`. Absent means both — which matters, because private quotes are the ones somebody usually wants to find. |
| `search` | no | Partial, case-insensitive, across **name and code together**. The term is escaped, so `?search=.*` matches literally and returns nothing rather than everything. |
| `page` | no | Zero-based. Absent means 0. |
| `size` | no | Absent means 20. **Between 1 and 100** — `?size=5000` is `400 INVALID_PAGE_SIZE`, so a caller cannot ask for the whole collection in one response. |
| `sort` | no | `field,direction`, e.g. `listPrice,desc`. Absent means newest first. An unknown field is `400 INVALID_SORT_FIELD` and an unknown direction `400 INVALID_SORT_DIRECTION`, both naming what is allowed. |

**No features on the rows.** A list of plans is a list of prices and ceilings; `featureCount` says
how many there are and #10 is where the list of them lives. Twenty plans × twelve features each
would be a response nobody reads to answer a question nobody asked.

### Filters, search, sort, page

| Parameter | Behaviour |
|---|---|
| `status` | repeatable; `?status=DRAFT&status=ACTIVE` means either |
| `planCode` | **exact**, case-insensitive, normalized — `premium-plus` finds `PREMIUM_PLUS` |
| `name` | **partial**, case-insensitive |
| `publiclyAvailable` | true or false |
| `search` | partial, across code **or** name |
| `page`, `size` | zero-based; 20 by default, 100 maximum |
| `sort` | `name`, `planCode`, `planVersion`, `status`, `listPrice`, `createdAt`, `updatedAt` |

All optional, so a bare call is the first page of the catalogue. AND between them; OR within
`status`.

**`planCode` is exact and `name` is partial**, deliberately. An exact-code filter is how somebody
asks for every version of one plan; an exact-name filter would be unusable, because nobody types
a plan's full display name to find it. `search` covers both partially for the one box on a screen.

### One row per plan *version*

`PREMIUM` v1 and v2 are two documents with two prices, and a school is on exactly one of them, so
a list that collapsed them would hide what somebody opened it to see. **The default order groups
them** — code ascending, version descending — which reads as a menu rather than as a change log.

### The same four decisions the school list made

Built on the same parts, for the same reasons:

- **It runs in the database.** [`PlanDefinitionRepositoryImpl`](../../repositories/plans/plandefinition/PlanDefinitionRepositoryImpl.java) builds one query; one page of documents is read however large the catalogue grows.
- **Bad paging is refused, not clamped.** `size=5000` is a `400`. A clamped page looks like a complete result.
- **`sort` is an allow-list.** An arbitrary field means an unindexed collection scan per request — and the *order* of a field can leak it even when the value is never returned.
- **Every sort ends with the plan's identity**, `planCode` asc + `planVersion` desc. That pair is unique, so paging is deterministic; without a tiebreaker, paging equal sort keys can show one row twice and miss another.

### Two things this endpoint made shared

**`sellable` moved to one definition.** It is three facts — `ACTIVE`, public, in window — and both
this list and the single-plan responses report it. Two screens combining the same three
conditions slightly differently is how a plan comes to look buyable on one page and not on
another, so it is computed in
[`PlanResponse.isSellable`](../../dto/plans/plandefinition/response/PlanResponse.java) and called from both.

**Regex escaping moved to [`CriteriaText`](../../common/mongo/CriteriaText.java).** The school
list already had it. One copy was fine; two copies of a security rule is one that gets fixed and
one that quietly does not. `?search=.*` returns nothing, because the term is matched literally.

### An empty result is `200` with `[]`

`totalElements: 0`, not a `404`. "No plan matches" is a successful answer.

<a id="e9"></a>
**[9](#t9) · `GET /platform/plans/{code}/versions`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `status`, `listPrice`, `effectiveFrom`, `effectiveUntil`, `createdAt`

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *counts*: how many point at each version

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Path:  {code}  the plan family

Every version of it, newest first. No
paging: a family has versions, not
thousands of them.
</pre></td>
<td><pre>
200 OK — PlanVersionHistoryResponse

{
  "planCode": "PREMIUM",
  "name": "Premium",
  "versionCount": 3,
  "versions": [
    {
      "planVersion": 3,
      "name": "Premium",
      "status": "DRAFT",
      "listPrice": 54999.00,
      "currencyCode": "INR",
      "priceChangeFromPrevious": 5000.00,
      "publiclyAvailable": false,
      "sellable": false,
      "featureCount": 3,
      "schoolsOnThisVersion": 0,
      "effectiveFrom": null,
      "effectiveUntil": null
    },
    {
      "planVersion": 2,
      "status": "ACTIVE",
      "listPrice": 49999.00,
      "priceChangeFromPrevious": 5000.00,
      "publiclyAvailable": true,
      "sellable": true,
      "featureCount": 2,
      "schoolsOnThisVersion": 12,
      "effectiveFrom": "2026-04-01T00:00:00Z",
      "effectiveUntil": null
    }
  ],
  "note": "Version 2 is the one being sold. 12 schools are on it."
}
</pre></td>
</tr>
</table>

**No request fields.** The only input is `{code}` in the path. Two computed fields are the reason
this endpoint exists rather than a filtered #8:

| Field | What it answers |
|---|---|
| `priceChangeFromPrevious` | What this version did to the price. Null on version 1, which had nothing before it. The question "when did we put the price up" is otherwise a subtraction done by hand down a list. |
| `schoolsOnThisVersion` | How many schools are on each one — **including cancelled and expired subscriptions**, on purpose. "Nobody is on it now" and "nobody was ever on it" are different facts, and the second is the one that says a version can be dropped without explaining anything to anybody. This is the query the `subscription_plan_version_idx` index exists for. |

### It answers two questions #8 cannot

**How did the price move**, and **can the old versions be forgotten**. The first needs the
versions next to each other in order; the second needs to know who is still on each one. Neither
falls out of a filtered list.

So each row carries `priceChangeFromPrevious` — the subtraction done for the reader rather than
by eye down a column — and `schoolsOnThisVersion`.

### Not paged, unlike #8

A price does not change fifty times. A history read in pages is not a history.

### `priceChangeFromPrevious` is null in two places

On the **oldest** version, which has nothing before it — and **across a currency change**, because
49999 INR to 699 USD is not a difference of −49300. A number there would be arithmetic on two
different currencies, which is worse than saying nothing.

### The subscription count needed an index

The endpoint's own description says it shows "which version each school is on", which the spec's
field list did not cover. Counting it meant asking `school_subscriptions` a question none of its
indexes answered — `planDefinitionDocsId` was unindexed, so the count was a scan of every
subscription on the platform, on a collection that eventually holds one row per school.

So [`SchoolSubscription`](../../models/plans/SchoolSubscription.java) gained
`subscription_plan_version_idx`. It is the only index there that is **not** school-scoped, which
is right: the question is about a plan, and a plan belongs to no school.

The count includes **cancelled and expired** subscriptions on purpose. "Nobody is on it now" and
"nobody ever was" are different answers to "can this version be forgotten", and only the second
one means it can.

### The counts are all 0 today, and the response says so

Nothing creates subscriptions — that is #13, not built. A column of zeroes would read as "this
plan has no customers", so the response carries a `note` saying to read them as *unknown*. Same
approach #3 in `core` takes with its subscription gap.

### A missing code is a `404`, not an empty list

You asked about one specific plan. Contrast #8, where no matches is a `200` and `[]` — there the
question was "which plans match this", and "none" is an answer.

### Testing it needs a hand-inserted version

#5 is deferred, so nothing in the API creates a second version and every plan has exactly one.
The `note` and the null price change are the only parts visible without inserting a v2 directly.

<a id="e10"></a>
**[10](#t10) · `GET /platform/plans/{code}/versions/{version}`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: every field, `features` included

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *counts*: how many are on this version

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Path:  {code}     the plan family
       {version}  which version

One version in full, with its features.
</pre></td>
<td><pre>
200 OK — PlanDetailResponse

{
  "planId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "name": "Premium",
  "description": "Everything, for a school that has outgrown Starter.",
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "listPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250,
  "publiclyAvailable": true,
  "sellable": true,
  "effectiveFrom": "2026-04-01T00:00:00Z",
  "effectiveUntil": null,
  "featureCount": 2,
  "features": [
    {
      "featureCode": "STUDENT_MANAGEMENT",
      "label": "Student management",
      "description": "Admissions, records and transfers.",
      "enabled": true,
      "usageLimit": null,
      "usageMetric": null,
      "overagePolicy": null
    },
    {
      "featureCode": "SMS_NOTIFICATIONS",
      "label": "SMS notifications",
      "description": "Texts to guardians.",
      "enabled": true,
      "usageLimit": 5000,
      "usageMetric": "MESSAGES_PER_MONTH",
      "overagePolicy": "BLOCK"
    }
  ],
  "schoolsOnThisVersion": 12,
  "createdAt": "2026-03-02T09:14:00Z",
  "updatedAt": "2026-04-01T00:00:00Z",
  "note": null
}
</pre></td>
</tr>
</table>

**No request fields.** `{code}` and `{version}` in the path, and the response is the whole
version — the one place `features` comes back with its labels and descriptions filled in from
each feature's own definition, so a screen showing a plan needs no second call.

| Field | Worth knowing |
|---|---|
| `sellable` | The plan is `ACTIVE` **and** inside its selling window. It does **not** include `publiclyAvailable`: a private quote is not on the public list and is still sellable, which is what #13 relies on. A screen that reads `sellable` to decide whether to offer a plan is right; one that reads it to explain *why* needs `status` and the two dates as well. |
| `features[].enabled` | `false` is a feature the plan deliberately switched off, which is different from one it never mentioned — the second is simply absent from the list. |
| `usageMetric` | Comes from the feature's definition, not the plan. The plan says how many; the feature says of what. |
| `schoolsOnThisVersion` | As on #9, counting cancelled and expired ones too. |
| `createdAt`, `updatedAt` | Set by Spring Data on every write. `updatedAt` moving on a published plan means an availability change (#7) or a retirement (#6) — nothing else can touch one. |

### Where the features are

#8 and #9 report a feature **count**, because a page of rows carrying twenty feature access each is
not a page anybody can read. This is the endpoint that returns them, so it is what a screen opens
after somebody picks a row.

Each row carries `label` and `description` from
[`FeatureCode`](../../models/plans/enums/FeatureCode.java) — the only place that wording is
written. Otherwise every screen showing "what this plan includes" keeps its own copy of 24
descriptions, and they drift.

### It does not reuse the writes' response

[`PlanResponse`](../../dto/plans/plandefinition/response/PlanResponse.java) carries a `nextStep` — a sentence
about what just happened and what to do next — and a read did not make anything happen. It also
reports only a count of features, which is right for a price change and wrong here.

So #10 has [`PlanDetailResponse`](../../dto/plans/plandefinition/response/PlanDetailResponse.java): the whole
document, and nothing about a transition. This is the rule `controllers/core/README.md` set out
for its own reads — *`nextStep` and `changeSummary` are write fields* — applied.

### The feature row is now shared

Building this made two endpoints return features, so the row moved into its own record,
[`PlanFeatureView`](../../dto/plans/plandefinition/response/PlanFeatureView.java). #3 and #10 return the
identical shape, so a client that can read one can read the other, and there is one place to
change if a field is ever added.

### `schoolsOnThisVersion`

Not in the field list above, and included for the same reason #9 has it: somebody looking at one
version is usually asking whether it can be retired. It costs one indexed count. Zero everywhere
until #13 exists, and the `note` says to read it as *unknown* rather than *nobody*.

<a id="e11"></a>
**[11](#t11) · `GET /schools/current/plans`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`, `publiclyAvailable`, `effectiveFrom`, `effectiveUntil` — the filter; then `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `features`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `planDefinitionDocsId`, `planVersion` — to mark the plan the school is already on

<a id="e12"></a>
**[12](#t12) · `GET /schools/current/plans/{code}/versions/{version}/comparison`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `planDefinitionDocsId`, `planVersion`, `contractedPrice`, `maxStudentsOverride`, `maxUsersOverride`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `features` — of both the `current` and the target `version`

## The subscription lifecycle — writes  ·  13–26

<a id="e13"></a>
**[13](#t13) · `POST /platform/schools/{id}/subscriptions`** — built

- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `counters.$.nextValue` — to get the `subscriptionNo`, through the positional operator
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *insert*: `schoolId`, `subscriptionNo`, `planDefinitionDocsId`, `planVersion`, `status` = `TRIAL` or `ACTIVE`, `billingCycle`, `currentPeriodStart` = midnight today in the school's zone unless sent, `currentPeriodEnd` = start plus the cycle's days, `autoRenew`, `contractedPrice`, `currencyCode`, `maxStudentsOverride` and `maxUsersOverride` = **the plan's limits unless the caller named others**, `current` = true
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `schoolSubscriptionDocsId`, `eventType` = `CREATED` or `TRIAL_STARTED`, `previousStatus` = null, `newStatus`, `source`, `reason`, `performedByDocsId`, `effectiveAt`
- [`schools`](../../models/core/School.java) — *updates*: `status` = `ACTIVE`, `activatedAt` — **only** when the school was `PROVISIONING` and its setup is otherwise complete

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "planCode": "PREMIUM",              // REQUIRED, max 40
  "planVersion": 1,                   // REQUIRED

  "currentPeriodStart": "2026-09-08T00:00:00Z",   // REQUIRED, today or later

  "trial": false,                     // optional
  "billingCycle": "QUARTERLY",        // optional — the plan's if absent
  "currentPeriodEnd": null,           // CUSTOM ONLY: required there, refused on the other four
  "autoRenew": true,                  // optional
  "contractedPrice": 39999.50,        // optional
  "maxStudentsOverride": 2500,        // optional
  "maxUsersOverride": 300,            // optional
  "billingCustomerReference": "cus_Qx7B2mR9",   // optional, max 120
  "reason": "Pilot, 20% partner discount."      // optional, max 500
}
</pre></td>
<td><pre>
201 Created
Location: /platform/schools/{id}/subscriptions/SUB/2026/09/000001

{
  "subscriptionId": "6a9e93e37feee1a04ace4a3b",
  "subscriptionNo": "SUB/2026/09/000001",
  "schoolId": "6a9e598382db56ca8afb6086",
  "planDefinitionDocsId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "planName": "Premium",
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "currentPeriodStart": "2026-09-06T18:30:00Z",
  "currentPeriodEnd": "2027-09-06T18:30:00Z",
  "autoRenew": true,
  "contractedPrice": 39999.50,
  "planListPrice": 49999.00,
  "currencyCode": "INR",
  "maxStudents": 2500,
  "maxUsers": 250,
  "hasLimitOverrides": true,
  "current": true,
  "nextStep": "Subscribed, and billed from Monday 7 September 2026 12:00AM. The school is now ACTIVE — a subscription was the last thing it needed. No invoice has been raised: that is a separate step."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `planCode` | **yes** | The plan's family key, max 40 — `PREMIUM`, not a Mongo id. With `planVersion` it names one immutable version. |
| `planVersion` | **yes** | Which version of that plan. Named rather than looked up, so a caller never has to read an id out of another response. |
| `trial` | no | `true` opens the subscription at `TRIAL` instead of `ACTIVE`. Nothing else differs — a trial has the same plan, price and limits. Absent or `false` means a paying subscription. **This is the only place the choice is made**; #15, which used to convert one, was withdrawn. |
| `billingCycle` | no | **Absent means the plan's own cadence**, which is the ordinary sale. Send one to sell the same plan on different terms — a school paying quarterly for a plan listed yearly buys the same feature access on a different cadence. It is stored on the **subscription**, so the plan itself is unchanged. **It is this cycle, not the plan's, that decides the period** and therefore whether `currentPeriodEnd` is required. |
| `currentPeriodStart` | **yes** | An instant, **today or later** — a past one is `400 PERIOD_START_IN_PAST`. **Required on every cycle**; it used to default to today and no longer does, because it is the anchor the end is measured from and on a yearly sale it fixes which day the school is billed on for as long as it stays. Midnight in the school's own day is the ordinary value — a billing period is a pair of dates somebody reads, so "your year runs from the 7th" rather than "from 12:47 on the 7th". |
| `currentPeriodEnd` | **on a `CUSTOM` cycle, and refused on every other** | **Only `CUSTOM` takes one.** A `CUSTOM` cycle has no length, so absent there is `400 BILLING_PERIOD_END_REQUIRED`, and the date must be after the start (`400 INVALID_BILLING_PERIOD`). On the four fixed cycles the end is derived — start **plus the cycle's days**, `MONTHLY` 30, `QUARTERLY` 90, `HALF_YEARLY` 180, `YEARLY` 365 — and sending one is `400 BILLING_PERIOD_END_NOT_ALLOWED`. **Which cycle counts is the one being sold**: a `YEARLY` plan sold `CUSTOM` needs a date, and a `CUSTOM` plan sold `MONTHLY` refuses one. |
| `autoRenew` | no | Absent means `true`. **Nothing acts on it.** [#17](#e17) starts the next period when it is called and does not consult the flag, and nothing calls #17 on a schedule — so there is no automatic renewal to switch off. It does appear on the school's own billing view, which tells the school its subscription does not renew automatically. |
| `contractedPrice` | no | What this school actually pays. Absent means the plan's `listPrice`. Zero is allowed — a free deal is a deal; negative is `400 PRICE_NEGATIVE`. The response shows it next to `planListPrice`, which is the only way to notice a discount. |
| `maxStudentsOverride` | no | A negotiated student ceiling. **Absent copies the plan's `maxStudents`** onto the subscription rather than leaving a null, so the document says what the school may use on its own. Zero is `400 LIMIT_TOO_LOW` here — on create there is nothing to remove. |
| `maxUsersOverride` | no | The same, from the plan's `maxUsers`. |
| `billingCustomerReference` | no | The payment provider's customer id, max 120. Absent means null; it is set later by #26. |
| `reason` | no | Free text, max 500, for the history row only — nothing on the subscription stores it. Unlike #14, where a reason is required, this one is optional: the row already records that the subscription was created. |

**The cycle decides the period, and the cycle can be negotiated.** These three fields work as one
unit:

| Cycle being sold | `currentPeriodStart` | `currentPeriodEnd` |
|---|---|---|
| `MONTHLY` | **required** | **refused** — derived as start + 30 days |
| `QUARTERLY` | **required** | **refused** — derived as start + 90 days |
| `HALF_YEARLY` | **required** | **refused** — derived as start + 180 days |
| `YEARLY` | **required** | **refused** — derived as start + 365 days |
| `CUSTOM` | **required** | **required** — `400 BILLING_PERIOD_END_REQUIRED` |

The start is required on **every** cycle. **Only `CUSTOM` takes an end date, and the other four
refuse one** — `400 BILLING_PERIOD_END_NOT_ALLOWED`. Those four *are* their length, so a date sent
with one either agrees with the derivation, in which case it said nothing, or disagrees with it,
in which case the record contradicts itself: a subscription reading `MONTHLY` whose period runs
six months bills the school for half a year while the document says it pays every month.

Refused rather than quietly dropped, which is the harder half of the choice: a caller answered
`201` with a different date than the one they sent has been ignored without being told.

"Being sold" means `billingCycle` on this request where one was given, and the plan's otherwise.
So the refusal follows what the **school** is billed on rather than what the plan is listed at —
verified both ways: a `YEARLY` plan sold as `CUSTOM` is refused without a date, and a `CUSTOM`
plan sold as `MONTHLY` derives 30 days and needs none.

**The cadence decides the period here too, exactly as on [#13](#e13).** These three fields are one
unit rather than three independent boxes:

| What the request moves | What happens to `currentPeriodEnd` |
|---|---|
| `billingCycle` to a fixed cycle | recalculated: start + 30 / 90 / 180 / 365 days. **`currentPeriodStart` is required with it** |
| `billingCycle` to `CUSTOM` | **both dates required on the same request** — `400 PERIOD_START_REQUIRED`, then `400 BILLING_PERIOD_END_REQUIRED` |
| `billingCycle` away from `CUSTOM` | recalculated from the new cycle; the start is still required with it |
| `currentPeriodStart`, on a fixed cycle | recalculated from the new start |
| `currentPeriodStart`, on `CUSTOM` | left alone — the end is a date somebody agreed |
| `currentPeriodEnd`, on `CUSTOM` | that date |
| `currentPeriodEnd`, on a fixed cycle | **refused** — `400 BILLING_PERIOD_END_NOT_ALLOWED` |
| neither the cycle nor the start | nothing derived |

**The end date is read against the cadence the subscription will be on AFTER this edit** — the one
on the request where `billingCycle` was sent, the stored one otherwise. So (`CUSTOM` → `MONTHLY`,
plus an end date) is refused as one request rather than accepted because `CUSTOM` was true when it
arrived. And a bare `currentPeriodEnd` on a subscription already billing `MONTHLY` is refused too,
which is the case a caller is most likely to try: **to move a fixed-cadence period end, move what
it is measured from** — send `currentPeriodStart`.

A derived change **appears in the changed-field list and the history row**, so it is never silent:
an edit sending only `billingCycle` answers `Edited billingCycle, currentPeriodEnd`.

**This is a deliberate reversal.** This row used to read *"the period dates are not recalculated
from it — an edit that moved the period end would change what the school is billed for while
looking like a change of cadence"*. The opposite turned out to be worse: an end derived as
"start + 365" is not the end of a `MONTHLY` period, so leaving it billed the school for a year
while the document said it paid monthly. Reporting the derived change is what answers the original
worry.

**`currencyCode` is deliberately not on the request.** It always comes from the plan. A
subscription priced in a different currency from the plan it points at is a mistake nobody would
catch until an invoice went out in the wrong money. Changing it later is #25.

### Two fields is the ordinary request

`planCode` and `planVersion`. The plan already knows the price, the currency, the billing cycle
and therefore when the first period ends, so everything else on the request exists for the deal
that is **not** ordinary — a discount, a raised limit, a trial, a custom period.

The plan is named by code and version rather than by id, the same way every plan URL names one.
Asking for a Mongo id would mean reading it out of another response first.

### Three documents, one transaction

| Document | Why it cannot be a second transaction |
|---|---|
| `school_subscriptions` | the subscription itself |
| `subscription_history` | a subscription with no history row is a customer nobody can explain, and the row that goes missing is the one explaining the thing that went wrong |
| `number_sequences` | a number handed out with no subscription attached is a permanent gap in the numbering that reads as a deleted record |

### The number sequence needed building

Nothing in this codebase had ever read a `NumberSequence`. Provisioning seeds the rows and
nothing consumed them, so #13 also built
[`NumberSequenceService`](../../services/institution/NumberSequenceService.java) — which every
module needs eventually: admission numbers, invoice numbers, receipts.

**The increment has to be a database operation.** Read-add-save hands the *same number to two
callers* the moment two requests overlap: both read 41, both write 42, and two subscriptions are
called `SUB/2026/09/000041`. It is a single `findAndModify`, so no two callers can see the same
value. That is the one thing in the class that cannot be written another way.

`NumberSequenceType` gained a **`SUBSCRIPTION`** constant. There were types for a subscription's
invoices and its payments but not for the subscription itself.

**The house format is `XXX/{YYYY}/{MM}/` plus six digits** — `SUB/2026/09/000001` — and every kind
of number in the system follows it, so they all read the same way and each one carries the month
it was issued in. `{YYYY}`, `{YY}` and `{MM}` are filled in when the number is made and the
resolved template is then stored on the counter row, so a school's numbering cannot change shape
part-way through a run.

The trailing separator is not decoration: the number is appended straight onto the prefix, so
`SUB/{YYYY}/{MM}` would produce `SUB/2026/09000001` with the month running into the digits.

### The capacity is copied onto the subscription

`maxStudentsOverride` and `maxUsersOverride` are written on every sale — the caller's figures
when they gave any, the plan's otherwise. Two reasons:

- **The subscription answers "what may this school use" on its own.** Reading it no longer means
  fetching the plan behind it to find out whether a null meant 500 or 5000.
- **A school keeps what it bought.** When the plan's next version raises its ceiling, schools
  already sold are unaffected, because their number is theirs rather than a pointer at whatever
  the plan currently says.

The cost is that "override" stops meaning "a figure was set" — it is set on every subscription. So
`hasLimitOverrides` in the responses compares against the plan rather than checking for null, and
means what it always meant to: **this school's ceiling is not its plan's**. A null check there
would answer true for every subscription ever sold.

### The plan must be sellable, and "publicly available" is not part of that

`ACTIVE`, and inside its selling window. A draft's price is still being decided; a retired plan
was taken off the menu. **A published plan that is not publicly available is allowed** — that is
exactly a private quote, and this endpoint is how a private quote gets sold.

The refusal for a retired plan says something different from the one for a draft, because
"publish it first" sent to a retired plan points at an endpoint that will refuse it.

### One current subscription per school

Enforced by a unique partial index on `{schoolId, current}`, but checked here first: a
duplicate-key error tells the caller nothing about what to do instead. The message names the
existing subscription and says to change its plan.

### The period is a fixed number of days, starting today

**The start is midnight at the beginning of today, in the school's own day.** Not the instant the
request arrived: a billing period is a pair of dates somebody reads, and "your year runs from the
7th" is what they expect rather than "from 12:47 on the 7th". The school's zone rather than UTC
decides which day today is — 06:00 in Kolkata is still yesterday in UTC. A caller who sends
`currentPeriodStart` gets that instead, which is how a contract that begins later is recorded.

**It cannot be in the past.** Today or later, or `400 PERIOD_START_IN_PAST` — on this endpoint and
on [#14](#e14). Nothing in the codebase invoices a period, so a backdated start would record a
school as paying for time nothing could ever charge it for: a figure sitting in the record that no
process could act on. Today itself counts, and "today" is the school's day rather than UTC's, so
the check agrees with the default above. On #14 only a start being **set** is checked; the stored
one is in the past by definition once the period is running.

**The end is the start plus a fixed count of days**, from the plan's cycle — and on these four
that is the *only* way it is ever set. A caller-supplied `currentPeriodEnd` is refused with
`400 BILLING_PERIOD_END_NOT_ALLOWED` rather than honoured, because a cadence and a period length
that disagree make the record contradict itself:

| Cycle | Days | May the caller send an end date? |
|---|---|---|
| `MONTHLY` | 30 | no |
| `QUARTERLY` | 90 | no |
| `HALF_YEARLY` | 180 | no |
| `YEARLY` | 365 | no |
| `CUSTOM` | — no length | **required** |

**This is equal periods rather than equal dates, and the cost is worth knowing.** Twelve 30-day
months come to 360 days, so a monthly subscription renewed through a year finishes five days
before the year does; a 365-day year that runs through 29 February ends a day early. The
alternative — adding calendar months — makes every period a different length and lands the end
date on a different day of the month depending on where it started, and 31 January plus a month
has to be argued about. Equal lengths were chosen; if the drift matters more than the evenness,
this is the one function to change.

A `CUSTOM` cycle has no length, so `currentPeriodEnd` is **required** there. Guessing a month
would be inventing a contract term.

### A school waiting only on a subscription goes live here

`PROVISIONING` is the state a school sits in while its setup is being finished, and the last
thing missing is usually the subscription. So step 8 of this endpoint tries to activate the
school, and the response says what happened either way.

**The setup gates are not skipped.** `activateSchool` refuses a school with no `SCHOOL_ADMIN`
role or an incomplete set of number sequences, and its own comment explains why: activating
anyway "produces a live school that fails on first use". That check is now
[`whyNotReadyToActivate`](../../services/core/SchoolPlatformService.java) — one implementation,
two callers, so #3 and #13 can never disagree about what "ready" means.

**Activation never fails the subscription.** The three outcomes are all reported in `nextStep`,
none of them an error:

| The school was | What happens |
|---|---|
| `PROVISIONING`, setup complete | `status` becomes `ACTIVE`, `activatedAt` is stamped if this is the first time — *"a subscription was the last thing it needed"* |
| `PROVISIONING`, setup incomplete | left alone, and `nextStep` carries the actual reason — the missing role, or how many sequences of how many exist |
| anything else | left alone — *"the school itself is SUSPENDED, which a subscription does not change"*. Reinstating a suspended school is #5's job, and buying something is not an appeal |

`activatedAt` is stamped only on the first activation, so a school that is suspended and later
resubscribes keeps the date it originally went live.

### What is not done, and is not pretended

- **`performedByDocsId` on the history row is null.** Nobody is signed in — see
  `CurrentSchoolResolver`. A sentinel there would read as a real account.
- **No invoice is raised.** That is a separate endpoint, and the response says so.
- **The activation check is still soft.** #3 in `core` still lets a school go live with no
  subscription, and still says so in `subscriptionNote`. #13 approaches the same gap from the
  other side — a school that only needed a subscription is activated *here* — but that does not
  stop #3 activating a school that has none. Tightening #3 is a deliberate change to `core` and
  has not been made.

<a id="e14"></a>
**[14](#t14) · `PATCH /platform/schools/{id}/subscriptions/current`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `billingCycle`, `maxStudents`, `maxUsers`, `features` — once, at the end, to build the response
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: any of `status`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `maxStudentsOverride`, `maxUsersOverride` — **and `reasonForChanges` on every edit**, from the request's `reason`, which is **required**
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` (see below), `previousStatus`, `newStatus`, `newPlanDefinitionDocsId`, `source`, `reason` = the fields that moved plus the caller's words, `performedByDocsId`, `effectiveAt` — **and nothing at all when nothing changed**

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "reason": "Renegotiated at renewal.",   // REQUIRED, max 500, not blank

  "status": "ACTIVE",                 // optional
  "billingCycle": "YEARLY",           // optional
  "currentPeriodStart": "2026-04-01T00:00:00Z",   // REQUIRED with billingCycle
  "currentPeriodEnd": "2027-03-31T23:59:59Z",     // CUSTOM ONLY: required there, else 400
  "autoRenew": false,                 // optional
  "maxStudentsOverride": 2500,        // optional, 0 removes it
  "maxUsersOverride": 300             // optional, 0 removes it
}

// At least one field besides `reason`, or
// 400 NO_CHANGES_REQUESTED.
//
// A cadence names the dates it needs:
//   billingCycle sent -> currentPeriodStart too
//   ...and CUSTOM     -> currentPeriodEnd as well (and it is refused on the other four)
// Every other edit leaves both alone.
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "subscriptionId": "6a9e93e37feee1a04ace4a3b",
  "subscriptionNo": "SUB/2026/09/000001",
  "schoolId": "6a9e598382db56ca8afb6086",
  "planDefinitionDocsId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "planName": "Premium",
  "planStatus": "ACTIVE",
  "planRetired": false,
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "currentPeriodStart": "2026-04-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "daysRemaining": 205,
  "periodEnded": false,
  "autoRenew": false,
  "current": true,
  "contractedPrice": 39999.50,
  "planListPrice": 49999.00,
  "currencyCode": "INR",
  "hasDiscount": true,
  "maxStudents": 2500,
  "maxUsers": 250,
  "maxStudentsOverride": 2500,
  "maxUsersOverride": null,
  "hasLimitOverrides": true,
  "featureCount": 2,
  "features": [ /* as #10 */ ],
  "reasonForChanges": "Renegotiated at renewal.",
  "billingCustomerReference": "cus_Qx7B2mR9",
  "note": "Edited status, autoRenew. No invoice has been raised or credited: that is a separate step."
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `reason` | **yes** | Free text, max 500, `@NotBlank` so `"  "` is refused. The only field that must be sent: everything else here changes something a school is paying for, and an unexplained change is one nobody can answer for months later. It is stored on the subscription as `reasonForChanges` **and** on the history row. **It is not itself a change** — sent alone it is `400 NO_CHANGES_REQUESTED`, and on an edit that moved nothing it is not stored. |
| `status` | no | Any of the six. **No transition rules apply** — this is the operator's override, not the lifecycle endpoints. What constrains it is the status the subscription is *already* in: only `TRIAL` and `ACTIVE` may be edited, so this moves a subscription out of those two and never back. Absent leaves it. Moving to `CANCELLED` stamps no date: when it happened is the `effectiveAt` of the `CANCELLED` history row this same request writes. |
| `billingCycle` | no | Any of the five. **The cadence decides the period, so changing it moves `currentPeriodEnd`** — recalculated as the start plus the new cycle's days, and reported in the changed-field list like any other edit. **Moving to `CUSTOM` requires `currentPeriodEnd` with it** (`400 BILLING_PERIOD_END_REQUIRED`): CUSTOM has no length to derive from, and the date on record belongs to the cadence being left. Moving *away* from CUSTOM needs nothing extra, and **refuses** an end date sent with it (`400 BILLING_PERIOD_END_NOT_ALLOWED`) — the new cadence decides. |
| `currentPeriodStart` | **with `billingCycle`** | An instant, **today or later** — `400 PERIOD_START_IN_PAST` otherwise. **Required whenever `billingCycle` is sent** (`400 PERIOD_START_REQUIRED`): a period is measured from its start, so setting the cadence without it would derive an end from an anchor nobody restated. Every other edit leaves it alone when absent. The **stored** start is not checked: a running subscription's is in the past by definition, and refusing that would make every other field here unreachable. Checked against the resulting end, whichever of the two moved. Cannot be cleared: it is `@NotNull` on the model. **Moving it moves the end with it** on the four fixed cycles — a period that kept its old end would be a different length from the cadence being paid on. On `CUSTOM` the end stays: it is an agreed date, not a derivation. |
| `currentPeriodEnd` | **only on a `CUSTOM` cadence** | An instant, and **only a `CUSTOM` cadence takes one**: on the four fixed cycles sending it is `400 BILLING_PERIOD_END_NOT_ALLOWED`, because the cadence decides its own length. Read against the cadence **after** this edit, so (`CUSTOM` → `MONTHLY` + an end date) is refused as one request. On `CUSTOM` **this is what extend-trial used to do** — send it alone to push a custom period out, and nothing else is derived because neither the cadence nor the start moved. To move a *fixed*-cadence end, send `currentPeriodStart` instead. A period left running backwards is `400 INVALID_BILLING_PERIOD`. |
| `autoRenew` | no | `true` or `false`. Absent leaves it. |
| `maxStudentsOverride` | no | A ceiling. **`0` removes it** and falls back to the plan's own `maxStudents` — the fields are flat, and Jackson cannot tell an omitted field from an explicit `null`, so zero carries the removal. Negative is `400 LIMIT_TOO_LOW`. |
| `maxUsersOverride` | no | The same, against the plan's `maxUsers`. |

**What the request cannot reach**, each with its own endpoint or its own reason: `contractedPrice`
and `currencyCode` (#25), `billingCustomerReference` (#26), the plan (#16), `subscriptionNo` (the
number the sequence handed out, pointed at by invoices and history), `current` (owned by the
unique partial index) and `schoolId` (it is in the URL). Sending them is ignored like any other
unknown field — so a body of only those is `400 NO_CHANGES_REQUESTED`.

### One endpoint where there were three

extend-trial moved `currentPeriodEnd`, #23 moved `autoRenew` and #24 the two overrides. Three
endpoints editing three columns of one document meant three sets of rules to keep in step, and a
correction touching two of them was two requests, two writes and two history rows for one
decision. Pushing a trial's end date out is now `currentPeriodEnd` on this request.

### Nothing about the money is here

**What it edits:** when the subscription runs (both period dates, the billing cycle), what state
it is in (`status`, and the cancellation that goes with it), and how much of the product it may
use (the two capacity overrides, `autoRenew`).

**What it will not:** `contractedPrice` and `currencyCode` are [#25](#e25),
`billingCustomerReference` is [#26](#e26), and the plan is [#16](#e16). Changing a price changes
what gets invoiced; changing a currency changes what money it is invoiced in. Those are
commercial decisions with a paper trail of their own, and an edit that could reprice a school
while looking like a change of dates is the one worth making a caller ask for separately.

A consequence worth knowing: **there is no built endpoint that can change a price after #13 has
set it.** #25 is not built, and #14 will not do it. A subscription sold at the wrong price has to
be corrected in the database until #25 exists.

### Only a TRIAL or ACTIVE subscription may be edited

The other four are `409 SUBSCRIPTION_NOT_EDITABLE`. Each of them was somebody's decision or a date
arriving, and each has an endpoint that owns the way out — undoing one by writing a field here
would bypass that endpoint, and the history row would say *edited* where the real event was
*resumed* or *revived*.

| Status | Editable | The way out |
|---|---|---|
| `TRIAL` | **yes** | — |
| `ACTIVE` | **yes** | — |
| `PAST_DUE` | no | [#17](#e17) renew once the bill is settled, or [#16](#e16) change plan |
| `SUSPENDED` | no | [#20](#e20) resume |
| `CANCELLED` | no | [#16](#e16) change plan, which opens a new period at `ACTIVE` |
| `EXPIRED` | no | [#16](#e16) change plan |

The refusal message names the way out for the status it refused, so it is a signpost rather than a
dead end. Nothing is written when it refuses — verified on the `@Version` counter and the absence
of a history row.

**It is still a one-way door in the other direction, and that is deliberate.** From `TRIAL` or
`ACTIVE` this endpoint can set *any* of the six statuses — that is what makes it the override, and
it is how `PAST_DUE` and `EXPIRED` get set by hand while no job exists ([#18](#t18) and
[#22](#t22) were dropped for that). What it cannot do is edit the subscription afterwards. So
`{"status": "EXPIRED", "reason": …}` still works, and the next `PATCH` is refused.

### It applies no transition rules, deliberately

The lifecycle endpoints each know one transition and what it implies. [#17](#e17) renews only what
is renewable, and refuses a trial or a live period. [#19](#e19) suspends only an `ACTIVE` or
`PAST_DUE` subscription and takes the school's access down with it; [#20](#e20) puts both back.
Cancelling would decide what happens to money already paid.

This writes what it is told, which is what is needed when a subscription is already wrong and no
ordinary transition describes the fix. **So `{"status": "SUSPENDED"}` here is not #19**: it moves
the field and nothing else — no school status, no `SUSPENDED` history event, none of the refusals.
Use it to correct a record, not to cut a school off.

One thing is still refused, because there is no reading of it that is not a mistake:

| Refused | Code |
|---|---|
| no `reason`, or a blank one | `400 VALIDATION_FAILED` |
| a period left running backwards, checked against whichever end did **not** move | `400 INVALID_BILLING_PERIOD` |
| a negative override | `400 LIMIT_TOO_LOW` — zero is the removal, negative is a typo |

### Absent, null and cleared

Absent and null are the same value to Jackson, so every field has a defined absence:

| Sent | Means |
|---|---|
| omitted, or `null` | leave it exactly as it is |
| a value | replace it |
| `0`, on either override | remove it, and fall back to the plan's own limit |

**Every field is flat, and the two overrides pay a small price for it.** Jackson cannot tell a
field that was omitted from one sent as `null` — both arrive as `null`. For the five `@NotNull`
fields that costs nothing, because they cannot be cleared at all. For the two nullable overrides
it would collapse "leave this alone" and "take this away" into one request, so **zero carries the
removal**: a school permitted no students is not a ceiling anybody negotiated, which is precisely
why #13 refuses zero on create. A negative number is still refused here — that is a typo, not an
instruction.

### `reason` is required, and lands in two places

**The only field on this request that must be sent.** Every other field here changes something a
school is paying for — its status, its dates, its capacity — and an unexplained change to any of
it is one nobody can answer for months later. `@NotBlank`, so `"  "` is refused as well.

It goes onto the subscription as **`reasonForChanges`** and onto the history row next to the list
of fields that moved. The document keeps the latest; history keeps all of them. So "why does this
school's period end in December" is answerable from the document a screen already has, and "why
did it move three times" is answerable from history.

**Every edit overwrites it.** A reason left standing from an earlier edit would attribute the
wrong explanation to the current state. Because the field is required, an edited subscription
always carries one — `reasonForChanges` being null means nothing has ever edited it.

**A reason on its own changes nothing.** It is not in `isEmpty()`, so a request carrying only a
reason is `400 NO_CHANGES_REQUESTED`. And a request whose fields all already hold their values
keeps the reason it had rather than storing the new one — an explanation for an edit that did not
happen is not worth keeping.

**`reasonForChanges` is not in the changed-fields list**, though it moves on every edit — it would
otherwise appear in every history row's field list and every response note, sitting next to the
reason itself.

**Which refusal an empty body gets changed with this.** Bean validation runs before the service,
so `{}` is now `400 VALIDATION_FAILED` naming `reason`, not `NO_CHANGES_REQUESTED`. That code is
now only reachable when a reason *was* given and no editable field was.

### A status move stamps nothing

Moving to `CANCELLED` records no date on the document. When a subscription was cancelled is the
`effectiveAt` of its `CANCELLED` history row, which this endpoint writes anyway; a second copy on
the document could only ever come to disagree with it.

### A cycle that no longer matches the plan is reported, not corrected

`billingCycle` is editable, so an edit can leave a school billing `MONTHLY` on a plan that bills
`YEARLY`. That is a real arrangement rather than a mistake, so the response says so and leaves it
— rewriting it would undo a deliberate change, and saying nothing would leave it to be found on
an invoice.

### "Changed" means different from what was stored

A caller who resends the current price has not edited anything. Every field is compared before it
is set, and a request that only restates what is already there answers `200` saying so — with no
history row, because an audit trail whose rows record that nothing happened is one nobody can
read. A request that asks for **nothing at all** is a different thing and is refused with `400
NO_CHANGES_REQUESTED`: answering 200 to it would tell a caller who misspelled a field name that
their edit worked.

### Which event type one edit writes

One row per edit, whatever moved, so a single decision is a single row. It records the most
consequential thing that happened:

| What moved | `eventType` |
|---|---|
| the status, to `ACTIVE` from `SUSPENDED` | `RESUMED` |
| the status, otherwise | `TRIAL_STARTED`, `ACTIVATED`, `PAYMENT_PAST_DUE`, `SUSPENDED`, `CANCELLED`, `EXPIRED` |
| nothing but terms | `TERMS_CHANGED` — added for this endpoint |

`PLAN_CHANGED` is never written here: this endpoint cannot move the plan, and [#16](#e16) is what
does.

The `reason` on the row is the field list plus the caller's words, always: months later "the
price changed" is the question and "which fields moved" is the answer, and a reason of
"renegotiated" alone does not say what was renegotiated.

### Address it as `current`, because a subscription number will not fit in a URL

The house format is `SUB/2026/09/000001`. A slash ends a path segment, so
`.../subscriptions/SUB/2026/09/000001` is not the address of anything: after `subscriptions/` the
route expects one segment and the number supplies four, so nothing matches and it 404s.
Percent-encoding does not rescue it either:

```
GET /platform/plans/AB%2FCD/versions/1
-> 400  Invalid URI: [The encoded slash character is not allowed]   (Tomcat, before Spring)
```

So `{no}` takes the word **`current`**, which is the honest name for what is being asked anyway:
a school has exactly one current subscription and a unique partial index makes sure of it, so
there is nothing to disambiguate. A number that happens to have no slashes in it still works, and
the `404` for one that does says why.

**This affects every later endpoint that names a subscription in its path** — #16 through #26 all
have `{no}` in them. Either they all use `current`, or `subscriptionNo` stops containing slashes.
That is a decision about the number format, not about this endpoint.

**The school id is in the lookup, not just the URL.** Subscription numbers are only unique within
a school, so the pair is what identifies one — and a caller who guesses another school's number
gets a `404` rather than somebody else's record.

### What it will not edit

- **`subscriptionNo`** — the number the sequence handed out, and how this subscription is addressed. Renumbering a record that invoices and history rows point at leaves a gap in the numbering that reads as a deleted subscription.
- **`current`** — owned by the unique partial index on `{schoolId, current}`. False leaves a school whose every subscription read answers 404; true on a second one is a duplicate-key error. A school has one subscription, so there is nothing to switch between.
- **`schoolId`** — in the URL. A PATCH that could move a subscription to another school would name one school and mean another.

**A `CLOSED` school's subscription is editable**, unlike #13 which refuses to sell to one. A
school that has left still has a record that can need correcting, and refusing would leave the
wrong figure in place for ever. `DELETED` and `DELETION_PENDING` are refused with `409
SUBSCRIPTION_NOT_EDITABLE` — there is nothing left to be right about.

<a id="e15"></a>
**[~~15~~](#t15) · ~~`POST /platform/schools/{id}/subscriptions/{no}/activate`~~** — removed

**Withdrawn on 2026-09-07, after being built.** It existed to move one subscription from `TRIAL`
to `ACTIVE` and start a fresh paid period. Three things made it redundant:

- **#13 already decides.** A subscription is sold as a trial or as a paying one, from the `trial`
  flag on the create request. That is where the question belongs, because it is a commercial
  decision made at the point of sale.
- **#14 can move the status**, along with the period dates, in one request with a required reason
  and a history row. `PATCH .../subscriptions/current` with `{"status": "ACTIVE", "reason": …}`
  is what this endpoint did.
- **A trial converting onto a different plan is not a status move at all.** The school tried one
  thing and is buying another, so it gets a new subscription — which is #16's territory, and
  needs the trial marked not-current first.

So there were three ways to say "this school is paying now", two of which had to be kept in step
with each other. The endpoint, its request DTO, its two private helpers and its
`SUBSCRIPTION_NOT_TRIAL` refusal are gone; `SubscriptionStatus.TRIAL` stays, because a trial is
still a real thing to sell.

**What went with it that was worth keeping**: the explanation of why a subscription is addressed
as `current` rather than by its number. That moved to [#14](#e14), which is now the only built
endpoint that names a subscription in its path.

**The gap in the numbering is deliberate.** The numbers are referenced from the code, from Postman
and from the other module READMEs, so renumbering sixty endpoints to close a hole would break more
than it tidies.

<a id="e16"></a>
**[16](#t16) · `POST /platform/schools/{id}/subscriptions/current/change-plan`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: the plan being **left**, for its price and limits, so the response can name what it moved from and tell a negotiated ceiling from a plan-standard one; and the plan being **moved to**, for `status`, `effectiveFrom`, `effectiveUntil`, `listPrice`, `currencyCode`, `billingCycle`, `maxStudents`, `maxUsers`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates* the row being **left**: `current` = false, `currentPeriodEnd` = the day of the change, `reasonForChanges`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *insert*: a new row for the plan being moved **onto** — `subscriptionNo` (its own, from the sequence), `planDefinitionDocsId`, `planVersion`, `status` and `billingCustomerReference` carried over, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `contractedPrice`, `currencyCode`, `maxStudentsOverride`, `maxUsersOverride`, `reasonForChanges`, `current` = true
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `counters.$.nextValue` — the new row needs a `subscriptionNo` of its own, because a unique index forbids two rows of one school sharing one
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: one row against the **new** subscription — `eventType` = `PLAN_CHANGED`, `previousPlanDefinitionDocsId`, `newPlanDefinitionDocsId`, `previousStatus` and `newStatus` = the status, unchanged, `source`, `reason` = both plans, both subscription numbers and the caller's words, `performedByDocsId`, `effectiveAt`
- [`schools`](../../models/core/School.java) — *reads* `status`, `schoolName`, `defaultTimeZone`; *updates* `status` = `ACTIVE` and `activatedAt` (first time only). See [the school's own status](#e16-school-status) — this is the one write outside `school_subscriptions`
- **No invoice.** `subscription_invoices` is not touched, because nothing writes to it yet

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  "planCode": "PREMIUM",              // REQUIRED, max 40
  "planVersion": 1,                   // REQUIRED
  "reason": "Outgrew Starter's 500.", // REQUIRED, max 500
  "currentPeriodStart": "2026-10-01T00:00:00Z",   // REQUIRED, today or later

  "billingCycle": "QUARTERLY",        // optional — the new plan's if absent
  "contractedPrice": 39999.50,        // optional
  "maxStudentsOverride": 2500,        // optional
  "maxUsersOverride": 300,            // optional
  "autoRenew": false,                 // optional
  "currentPeriodEnd": null            // optional / REQUIRED on CUSTOM
}

// The PLAN changes immediately — a
// subscription cannot hold a pending one.
// currentPeriodStart chooses when the new
// billing PERIOD begins, and the row being
// left ends at that same instant, so the
// two meet.
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "subscriptionNo": "SUB/2026/09/000001",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "planName": "Premium",
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "currentPeriodStart": "2026-09-06T18:30:00Z",
  "currentPeriodEnd": "2027-09-06T18:30:00Z",
  "contractedPrice": 39999.50,
  "planListPrice": 49999.00,
  "currencyCode": "INR",
  "hasDiscount": true,
  "maxStudents": 2000,
  "maxUsers": 250,
  "maxStudentsOverride": 2000,
  "maxUsersOverride": 250,
  "hasLimitOverrides": false,
  "reasonForChanges": "Outgrew Starter's 500.",
  "note": "Upgraded from 'STARTER_PLAN' version 1 to 'PREMIUM' version 1. The period restarts today and runs to Tuesday 7 September 2027 12:00AM on the new plan's YEARLY cycle. NO money has moved for the period the school had already paid for: nothing raises invoices yet, so nothing was charged, credited or refunded. What should happen to it is still an open question."
}

409 PLAN_UNCHANGED               — already on that version
409 PLAN_NOT_SELLABLE            — a draft or retired target
409 SCHOOL_NOT_PLAN_CHANGEABLE   — the school is being wound down
400 PERIOD_START_IN_PAST         — a start already past
400 INVALID_BILLING_PERIOD       — an end not after the start
400 BILLING_PERIOD_END_REQUIRED  — a CUSTOM target, no end
400 LIMIT_TOO_LOW                — a ceiling sent as 0
404 SCHOOL_NOT_FOUND             — no such school
404 SUBSCRIPTION_NOT_FOUND       — the school has none
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `planCode` | **yes** | The family to move to, max 40. May be the plan the school is already on — that is how it moves to a newer version — but not the same code **and** version, which is `409 PLAN_UNCHANGED`. |
| `planVersion` | **yes** | Which version. The target must be sellable today: a draft's price is not settled and a retired plan is off the menu, both `409 PLAN_NOT_SELLABLE`. A published plan that is not publicly available is fine — that is a private quote. |
| `reason` | **yes** | Max 500, `@NotBlank`. Stored as `reasonForChanges` and on the history row. A plan change moves what a school is entitled to and what it pays — an unexplained one is the hardest record to answer questions about later. |
| `currentPeriodStart` | **yes** | An instant, **today or later** (`400 PERIOD_START_IN_PAST`). It used to be derived as "today" and is now stated, because it decides **two** dates rather than one: the anchor the new period's end is measured from, and the instant the row being left stops serving. A future date does **not** delay the plan change — only the period — and the closed row's end moves to meet it, so the school is never on neither. |
| `billingCycle` | no | **Absent means the new plan's own cadence**, which is the ordinary move. Send one to put the school on the new plan at a different cadence — the same negotiation #13 allows, stored on the **subscription** rather than the plan. **It is this cadence that decides the new period** and therefore whether `currentPeriodEnd` is required. |
| `contractedPrice` | no | **Absent means the new plan's list price.** A discount is **not** carried over automatically: it was agreed against a plan at a price, and this is a different plan at a different price, so continuing it silently would invent a deal nobody made. Send it to continue the same arrangement. Zero allowed, negative refused. |
| `maxStudentsOverride` | no | **Absent copies the new plan's `maxStudents`**, exactly as #13 does on a sale — so a ceiling negotiated on the old plan is **not** carried across unless it is restated here. At least 1; zero is `400 LIMIT_TOO_LOW`, because there is nothing to remove on a plan change. Removing an override is #14, where zero means exactly that. |
| `maxUsersOverride` | no | The same, against the new plan's `maxUsers`. |
| `autoRenew` | no | **Absent leaves it exactly as it is** — the one absence on this request that does *not* mean "take the new plan's". A plan has no opinion about renewal: it is the school's standing instruction, and a school that turned it off has not changed its mind by moving plan. Defaulting to `true` the way #13 does would switch it back on for precisely the school that asked for it off. **Nothing acts on it** — [#17](#e17) does not consult it. |
| `currentPeriodEnd` | **on a `CUSTOM` cadence** | An instant, after `currentPeriodStart` (`400 INVALID_BILLING_PERIOD`). Absent means the cadence decides — 30, 90, 180 or 365 days from `currentPeriodStart`. **Which cadence counts is `billingCycle` on this request, and the new plan's otherwise** — never the old plan's: a school moving from a yearly plan to a monthly one gets 30 days, and a yearly plan billed `CUSTOM` needs a date. |

### Every status may change plan, and the new row is always ACTIVE

**No status is refused, and none is carried forward.** A plan change is somebody buying this school
a plan, so the row it lands on has to be one the school can use — carrying `TRIAL`, `SUSPENDED`,
`PAST_DUE` or `CANCELLED` onto a plan just bought would sell it something it cannot reach.

| The old row was | The new row is |
|---|---|
| `ACTIVE` | `ACTIVE` |
| `TRIAL` | `ACTIVE` — **this is the conversion path**; a trial that starts paying is a plan change, which is why [#15](#e15) was withdrawn |
| `PAST_DUE` | `ACTIVE` |
| `SUSPENDED` | `ACTIVE` |
| `CANCELLED`, `EXPIRED` | `ACTIVE` — a finished subscription is how a school comes back |

`CANCELLED` and `EXPIRED` used to be `409 SUBSCRIPTION_NOT_CHANGEABLE` on the grounds that there
was "nothing to move", which mistook what this endpoint does: it never edits the row it is given,
it retires that row and opens a new one, so the state the old row ended in does not constrain the
new one at all.

**It also agrees with the school now.** [Step 14](#e16-school-status) takes the school `ACTIVE`
either way; this used to leave a `SUSPENDED` subscription suspended while doing so, which is a
state nothing could act on sensibly.

**`autoRenew` is carried across untouched, on every plan change including a revival.** It is the
school's standing instruction, and absent on the request means "leave it as it is" — no exception.

That has one visible consequence worth knowing: [#21](#e21) turns the flag off on its way out, so a
revived subscription inherits `autoRenew: false` unless the request names it. Nothing acts on the
flag, so it costs the school nothing — but its own billing view ([#33](#e33)) reads it, and will say
a subscription somebody has just bought does not renew. **Send `autoRenew: true` with the revival**
if it should say otherwise; the `note` points that out when the flag comes across off.

Overriding a school's standing instruction is deliberately the caller's decision rather than
something this endpoint infers.

### A closed period only ever shrinks

The row being retired has its `currentPeriodEnd` set to the handover **only when that is earlier
than the end it already has**. One rule, and the *dates* decide it rather than the status:

| The closed row's stored end | What happens |
|---|---|
| after the handover — it was still serving | **trimmed** to the handover; leaving it would claim the school was on this plan for months it was not |
| already at or before the handover — it had stopped | **left alone**; stretching it forward would claim it covered a gap the school was on nothing for |

**Keying on the status instead got the middle case wrong.** A *scheduled* cancellation is
`CANCELLED` with an end still in the future, and it really was serving until the handover — so it
does need trimming. Treating every `CANCELLED` row as finished left two rows claiming the same
days.

**One edge worth knowing.** `currentPeriodStart` may legitimately be earlier than when the old row
stopped — midnight today, say, against a cancellation at 09:35 the same morning. The rule then
shortens the closed row to the handover, because the alternative is two overlapping periods. If the
exact moment the old row stopped matters, ask for a handover at or after it.

**Why the closed row's dates are left alone.** On an ordinary move the two periods meet, because
the old plan really did serve until the new one took over. A cancelled row did not: it stopped when
it was cancelled. Moving its end forward would claim it covered a gap the school was on nothing
for. So the gap stays visible, and the `note` names both dates.

The history row is what records the move: `previousStatus` is whatever the old row was and
`newStatus` is `ACTIVE`. They are equal only when the school was already `ACTIVE`.

### The cadence and the period, and how the two rows meet

| Cadence being moved onto | `currentPeriodStart` | `currentPeriodEnd` |
|---|---|---|
| the new plan's, or one named on the request | **required**, today or later | start + 30 / 90 / 180 / 365 |
| `CUSTOM`, either way round | **required**, today or later | **required** |

"Either way round" is the part worth testing: a `CUSTOM` **plan** needs an end date, and so does a
`YEARLY` plan being billed `CUSTOM` — while a `CUSTOM` plan billed `MONTHLY` derives one and needs
none. The refusal follows what the school is actually billed on.

**The two rows meet exactly.** The row being left has its `currentPeriodEnd` set to the new
`currentPeriodStart`, so there is no day the school was on neither plan and none where it was on
both. Ordinarily that **trims** the old period — its stored end would otherwise claim months the
school was not on that plan — and for a start dated later it **extends** it instead, which is the
same statement: the old plan serves until the new one takes over.

### The change is immediate, and there is no field asking otherwise

A subscription holds **one** plan, not a current one and a pending one, so a change scheduled for
a future period would have nowhere to live. Moving the pointer now while calling it "next period"
would hand the school its new feature access early and bill it at the old price for the rest of the
period — so the endpoint does the honest thing instead: **the plan changes when the request is
made.**

**What `currentPeriodStart` chooses is when the new billing PERIOD begins**, which is a different
question. Today is the ordinary answer and reproduces exactly what this endpoint did when the
start was derived. A later date moves the period, not the feature access — and because the closed
row's end moves with it, the school keeps being billed for the old plan until the new period
opens. That is not the scheduling this section rules out; the plan pointer still moves now.

Deferring a change is #17's territory, if it is ever wanted: renewal is the moment a period ends,
which is the only moment a deferred change could take effect.

<a id="e16-school-status"></a>

### It moves the school's own status, and refuses a school being wound down

The only write this endpoint makes outside `school_subscriptions`. A school that is paying for a
plan should be able to use what it pays for, so a plan change leaves the school `ACTIVE` — and a
school that is being shut down cannot change plan at all.

| School status | Plan change | The school afterwards |
|---|---|---|
| `PROVISIONING` | allowed | `ACTIVE`, and `activatedAt` stamped if it was never set |
| `ACTIVE` | allowed | `ACTIVE` — unchanged |
| `SUSPENDED` | allowed | `ACTIVE` — **the suspension is lifted** |
| `OFFBOARDING` | `409 SCHOOL_NOT_PLAN_CHANGEABLE` | untouched |
| `CLOSED` | `409 SCHOOL_NOT_PLAN_CHANGEABLE` | untouched |
| `DELETION_PENDING` | `409 SCHOOL_NOT_PLAN_CHANGEABLE` | untouched |
| `DELETED` | `409 SCHOOL_NOT_PLAN_CHANGEABLE` | untouched |

Written as an **allow-list** of the three running states, so a status added to `SchoolStatus`
later is refused until somebody decides it should be allowed, rather than silently permitted.

**Two things worth knowing, because they are not what the neighbouring endpoints do.**

**It un-suspends, where a sale does not.** #13 leaves a `SUSPENDED` school suspended, on the
argument that lifting a suspension is a decision rather than a side effect of buying a plan. Here
it is the opposite: a suspension is ordinarily for non-payment, and a school being moved onto a
new plan has generally sorted that out — leaving it locked out would bill it for something it
cannot reach. The `note` always says the suspension was lifted, so it is never silent.

**`OFFBOARDING` is refused here and accepted everywhere else.** #13 and #14 both allow it: a
school being wound down still has a subscription that may need correcting, and refusing to *edit*
one would leave a wrong record un-fixable. But moving that school onto a *different* plan sells to
a customer who is leaving, and this endpoint takes the school `ACTIVE` as it goes — which would
reverse the wind-down as a side effect of a plan change.

**It does not re-run the provisioning checks**, unlike #13. A `PROVISIONING` school reaching a
plan change already has a subscription, so #13 has already run those checks and either activated
it or said why not; refusing again here would block a plan change over a setup step that has
nothing to do with the plan. Instead the `note` reports the incomplete setup, so putting such a
school live is visible in the response:

> The school was PROVISIONING and is now ACTIVE, but its setup is not finished: This school has
> no SCHOOL_ADMIN role. Run complete-provisioning first.

### Two rows, not one edited row

**The row being left is closed rather than rewritten.** `current` becomes false, and its
`currentPeriodEnd` is trimmed to the day of the change — that is the period it actually served,
and leaving the old end date would claim the school was on that plan for months it was not. Its
**status is not touched**: it did not expire and it was not cancelled, it was *superseded*, and
writing either of the other two words would put something false in the record.

**A new row is inserted** for the plan the school moves onto, with a `subscriptionNo` of its own
from the number sequence — two rows of one school cannot share a number, because a unique index on
`{schoolId, subscriptionNo}` says so, and an invoice pointing at a number matching two records
would be unanswerable.

**The order of the two writes matters.** The old row is closed *before* the new one is inserted:
the unique partial index on `{schoolId, current}` permits one current row per school, so inserting
first would collide with a row still claiming to be current.

So `school_subscriptions` holds **one row per plan period**. Verified on the running server, after
a sale and two changes:

| `subscriptionNo` | plan | `current` | period |
|---|---|---|---|
| SUB/2026/09/000001 | STARTER_PLAN | false | 06 Sep → 06 Sep |
| SUB/2026/09/000002 | PREMIUM | false | 06 Sep → 06 Sep |
| SUB/2026/09/000003 | STARTER_PLAN | **true** | 06 Sep → 06 Sep 2027 |

Every read of "the school's subscription" therefore goes through
`findBySchoolIdAndCurrentIsTrue`, never through "the row for this school" — confirmed for #14,
#27, #33 and #34, all of which answer with `SUB/2026/09/000003` above. #13 still refuses a second
sale with `SUBSCRIPTION_ALREADY_EXISTS`, naming the current row.

**What carries across to the new row**, beyond the plan and the negotiated terms: `status`, so a
suspended school stays suspended and a trial stays a trial; `billingCustomerReference`, because
the payment provider's handle belongs to the school rather than to the plan it is on; and
`autoRenew` unless the request names it.

### What follows the plan, and what survives it

| | |
|---|---|
| from the new plan | `billingCycle` and `currencyCode`, always |
| from the new plan | the price and both capacity ceilings, **unless the request names them** |
| kept as it is | `autoRenew`, **unless the request names it** |
| kept as it is | `status`, `billingCustomerReference`, `subscriptionNo`, `current` |

**There are two kinds of absence here, and the difference is deliberate.** A price or a ceiling
left out takes the **new plan's** figure, because those are terms agreed against a plan.
`autoRenew` left out keeps **the school's** existing setting, because a plan has no opinion about
renewal. Verified: a school that turns auto-renewal off and is then moved to Premium without the
field comes back `autoRenew: false`.

**The new plan is the starting point for everything negotiable**, and the request is how a
negotiated figure is carried across. A price and a ceiling are agreed against a *particular* plan,
so moving a school to a different one means those terms are being renegotiated whether or not
anybody says so. Copying the old figures over silently would be this endpoint inventing terms
nobody agreed to; re-stating them makes the new arrangement somebody's decision.

Worked through, and verified against the running server:

| The school was on | The request said | It ends on |
|---|---|---|
| Premium, negotiated 9,000 students | nothing about ceilings | Starter's own **500 / 50** — the 9,000 is gone |
| Premium | `maxStudentsOverride: 4000`, `maxUsersOverride: 400` | **4,000 / 400**, and `hasLimitOverrides` true |
| Starter | `maxStudentsOverride: 3000` only | **3,000 / 250** — the students named, the users from Premium |

That first row is the one to know about: **a negotiated ceiling does not survive a plan change on
its own.** It is the same rule #13 uses, and the alternative — carrying it across — would quietly
apply a term agreed for one plan to a different one.

### What it will not do

- **Raise, credit or refund anything.** Nothing writes `subscription_invoices`, so the school is
  part-way through a period it paid for and this endpoint does not touch that money. It does not
  ask what should happen to it either: that is a commercial question belonging to whatever raises
  the invoice, and the module's open-questions list still carries it. The response says so
  outright, because a plan moved and a payment taken are different facts.
- **Check that a downgrade is safe.** Nothing counts students yet, so moving a school to a smaller
  plan may leave it above its new ceiling and nobody would know. The response says so on a
  downgrade rather than implying the move was verified.
- **Touch anything #14 owns**, and #14 cannot touch the plan. Two endpoints, because "push the
  trial out a fortnight" and "move them to Enterprise" are not the same request.
- **Leave two current rows.** The old one is closed first, and the unique partial index on
  `{schoolId, current}` is the backstop — though that index is **not built on the dev database**
  yet, so today the write order in the code is the only thing enforcing it. Worth running the
  index sync before it matters.

<a id="e17"></a>
**[17](#t17) · `POST /platform/schools/{id}/subscriptions/current/renew`** — built

- [`schools`](../../models/core/School.java) — *reads* `status` and `schoolName`. **No write** — a renewal continues an arrangement rather than starting one, so unlike [#16](#e16) it does not take the school `ACTIVE` or lift a suspension
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads* the plan the subscription is already on, for its `status`, `effectiveFrom` and `effectiveUntil`, and to name it in the response. **The plan has to still be current**, or `409 PLAN_NOT_RENEWABLE` — see [the plan's own state](#e17-plan-state)
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates* the row whose period **ended**: `current` = false, `reasonForChanges`. Its **dates are left alone** — it ran its full course, which is what distinguishes this from #16, where the closed row's end is trimmed to the day of the change
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *insert*: the next period — `subscriptionNo` (its own, from the sequence), `currentPeriodStart` = the old `currentPeriodEnd`, `currentPeriodEnd` = that plus the cycle, `status` = `ACTIVE`, `current` = true, and `planDefinitionDocsId`, `planVersion`, `billingCycle`, `autoRenew`, `contractedPrice`, `currencyCode`, `maxStudentsOverride`, `maxUsersOverride`, `billingCustomerReference` all **copied unchanged**
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `counters.$.nextValue` — the new row needs a `subscriptionNo` of its own, because a unique index forbids two rows of one school sharing one
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: one row against the **new** subscription — `eventType` = `RENEWED`, `previousStatus`, `newStatus` = `ACTIVE`, `previousPlanDefinitionDocsId` and `newPlanDefinitionDocsId` = the same plan (written rather than left null, because that sameness is what tells a renewal from a plan change in a list of history rows), `source`, `reason`, `effectiveAt` = the new period's start
- **No invoice.** `subscription_invoices` is not touched — see [the money](#e17-money) below

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
(none — for the four fixed cycles)

// One optional field, and the ordinary
// renewal omits the body entirely:
{
  "currentPeriodEnd": "2027-06-30T23:59:59Z"
}

// REQUIRED on a CUSTOM cycle, which has no
// length for anything to derive from.
// Optional override on the other four.
//
// Nothing else, because anything that could
// change a term would make this a change
// rather than a renewal:
//
//   the terms -> #14
//   the plan  -> #16
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "subscriptionNo": "SUB/2026/09/000002",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "status": "ACTIVE",
  "billingCycle": "MONTHLY",
  "currentPeriodStart": "2026-08-01T00:00:00Z",
  "currentPeriodEnd": "2026-08-31T00:00:00Z",
  "contractedPrice": 4242.50,
  "planListPrice": 4999.00,
  "currencyCode": "INR",
  "maxStudents": 500,
  "maxUsers": 42,
  "hasLimitOverrides": true,
  "reasonForChanges": "Renewed from SUB/2026/09/000001 on the same terms.",
  "note": "Renewed on the same terms: 'PREMIUM' version 1 at 4242.50 INR. SUB/2026/09/000001 is closed and kept as history; this school is now on SUB/2026/09/000002, running from Saturday 1 August 2026 5:30AM to Monday 31 August 2026 5:30AM on its MONTHLY cycle. NO invoice was raised and no money was taken: nothing writes subscription_invoices yet, so this moved the billing period and recorded the renewal without charging for it."
}

409 PLAN_NOT_RENEWABLE           — the plan is no longer current
409 PERIOD_NOT_ENDED             — the period is still running
409 SUBSCRIPTION_NOT_RENEWABLE   — TRIAL, SUSPENDED or CANCELLED
409 SCHOOL_NOT_RENEWABLE         — the school is winding down
400 BILLING_PERIOD_END_REQUIRED  — a CUSTOM cycle, no date sent
400 INVALID_BILLING_PERIOD       — a date not after the start
404 SCHOOL_NOT_FOUND             — no such school
404 SUBSCRIPTION_NOT_FOUND       — the school has none
</pre></td>
</tr>
</table>

**The body may be omitted entirely**, and for the four fixed cycles it should be — that is the
point of the endpoint: it renews what is already there. `current` in the path is the literal word,
the same as [#14](#e14) and [#16](#e16) — a `subscriptionNo` contains slashes and Tomcat rejects
them encoded in a path before Spring sees the request.

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `currentPeriodEnd` | **on a `CUSTOM` cycle, and refused on every other** | An instant. **Required** when the subscription bills on `CUSTOM`, which has no length for anything to derive from — without it, `400 BILLING_PERIOD_END_REQUIRED`. **On `MONTHLY`, `QUARTERLY`, `HALF_YEARLY` or `YEARLY` the cycle decides** — 30, 90, 180 or 365 days from where the last period ended — which is the ordinary renewal, needs no body at all, and **refuses** a date sent with it: `400 BILLING_PERIOD_END_NOT_ALLOWED`, as on #13, #14 and #16. On `CUSTOM` it must be after the new period's start (the day the previous period ended), or `400 INVALID_BILLING_PERIOD` — a period that ended before it began is one no school was ever on. |

### What renews, and what is refused

| Subscription status | Renew | Result |
|---|---|---|
| `ACTIVE` | allowed | stays `ACTIVE` on the next period |
| `PAST_DUE` | allowed | becomes `ACTIVE` — see below |
| `EXPIRED` | allowed | becomes `ACTIVE`; this is the case the endpoint exists to repair |
| `TRIAL` | `409 SUBSCRIPTION_NOT_RENEWABLE` | a trial has no agreed next-period price. Extend it with #14, convert it with #16 |
| `autoRenew: false` | **allowed** | the flag is not checked — see below |
| a `CUSTOM` cycle | **allowed, with a date** | it is asked for the next period's end, not refused — see below |
| a plan no longer current | `409 PLAN_NOT_RENEWABLE` | retired, back to `DRAFT`, or outside its window — see below |
| `SUSPENDED` | `409 SUBSCRIPTION_NOT_RENEWABLE` | billing a school for a period it cannot use |
| `CANCELLED` | `409 SUBSCRIPTION_NOT_RENEWABLE` | deliberately ended; renewing would undo a decision |

**All three renewable statuses come out `ACTIVE`.** An `EXPIRED` row whose new period has just
started would contradict its own dates, and a `PAST_DUE` one carried across would say the new
period is already unpaid before anything has been invoiced for it. What a `PAST_DUE` renewal does
**not** do is settle the previous period — nothing here takes a payment — and the `note` says so.

**`autoRenew` is not checked, and refuses nothing.** Nothing calls this endpoint on a schedule, so
every renewal is an operator deciding to renew this particular school now — and refusing that
because of a flag would only mean editing the flag first to get past the endpoint, which records a
change nobody wanted for the sake of a call they did.

The flag is still **carried onto the new row**, and the school's own view (#33) still tells a
school its subscription does not renew automatically. What does not exist is anything that renews
on its own, which is what the flag would govern. If a scheduled job is ever built, the flag
belongs in *its* selection query — that is the thing whose behaviour it describes.

**A `CUSTOM` cycle is asked when the next period ends.** It has no length, so there is nothing to
derive — and that is a question rather than a refusal, which is the whole reason this endpoint has
a request body at all. Send `currentPeriodEnd`; without it, `400 BILLING_PERIOD_END_REQUIRED`.

The date is **not guessed**. Falling back to a year, or to the length of the last period, would put
a date in a billing record that nobody signed off. It is checked against the new period's start —
the day the previous one ended — so a renewal cannot be made to finish before it began
(`400 INVALID_BILLING_PERIOD`), and nothing is written when it is refused.

**A school being wound down is refused** — `OFFBOARDING`, `CLOSED`, `DELETION_PENDING`, `DELETED`
give `409 SCHOOL_NOT_RENEWABLE`. The same allow-list #16 uses, for a plainer reason: do not start a
new billing period for a customer who is leaving.

### The periods are contiguous, and each call advances exactly one

The new period starts at the **old period's end**, not today. So there is no day the school was
live but unbilled, and none it was billed twice for.

That is also why a period still running is refused rather than renewed early: starting the next
period before this one ends would leave the school's `current` row with a period that has not
begun, and every read of "what is this school on" would have to explain it.

**A subscription several periods behind catches up one call at a time.** Each renewal advances one
period, so a subscription whose period ended three cycles ago needs three calls — and each new row
is a real record of a real period rather than one row pretending to cover the whole gap:

```
SUB/…/000001   ran to 2026-08-01      closed
SUB/…/000002   2026-08-01 → 2026-08-31   closed by the next call
SUB/…/000003   2026-08-31 → 2026-09-30   current, period still running
                                          -> a further call is 409 PERIOD_NOT_ENDED
```

<a id="e17-plan-state"></a>

### The plan has to still be current, and a retired one is refused

A renewal commits the school to **the same plan** for another period. So the plan it is on has to
be one a school can still be put on:

| `plan_definitions` state | Renew |
|---|---|
| `ACTIVE`, inside its window | allowed |
| `ACTIVE`, `publiclyAvailable: false` | **allowed** — a private plan is a negotiated quote, not an invalid state |
| `RETIRED` | `409 PLAN_NOT_RENEWABLE` |
| back to `DRAFT` | `409 PLAN_NOT_RENEWABLE` — its terms are not settled |
| past `effectiveUntil` | `409 PLAN_NOT_RENEWABLE` |
| before `effectiveFrom` | `409 PLAN_NOT_RENEWABLE` |

**The refusal names #16, because that is the only fix.** The school is already on this plan and the
plan has gone; the question is not which plan to sell but whether to run this one for another
period, and the answer is to move the school onto a plan that is still current:

> 'STARTER_PLAN' version 1 has been retired, so SUB/2026/09/000001 cannot be renewed onto it for
> another period. Move this school to a current plan with the change-plan endpoint instead.

**Not the same check `loadSellablePlan` does**, though it looks it. That one is for *choosing* a
plan to sell, and its advice — "publish it first", "use a plan that is still on the menu" — is
wrong for a school already on the plan. Different question, different code, different sentence.

**Nothing is written when it is refused**, so a school on a retired plan keeps the period it has
until somebody moves it.

<a id="e17-money"></a>

### No invoice is raised, and that is what this endpoint is missing

The table for #17 named `subscription_invoices`, and it is deliberately not written. Two reasons,
and neither is an oversight:

- **There is no repository for it.** `SubscriptionInvoice` is a model and nothing else — no
  repository, no service, no writer anywhere in the codebase.
- **Its fields are commercial decisions, not derivations.** `subTotal`, `taxAmount`, `dueDate` and
  the invoice numbering cannot be worked out from a plan's price by this method. Guessing them
  would put numbers in a financial record that nobody agreed.

So #17 moves the billing period and records the renewal; it does not charge for it. The `note`
says so on every response rather than leaving a charge to be assumed. Raising the invoice belongs
with [#70](#e70), the job that also takes the payment, or with an invoicing endpoint of its own.

<a id="e18"></a>
**[~~18~~](#t18) · ~~`POST /platform/schools/{id}/subscriptions/{no}/mark-past-due`~~** — not being built

**Dropped on 2026-09-07, before being built.** It would have set `status` to `PAST_DUE` and
written one `PAYMENT_PAST_DUE` history row. [#14](#e14) already does exactly that:

```
PATCH /platform/schools/{id}/subscriptions/current
{ "status": "PAST_DUE", "reason": "Invoice INV/2026/09/000123 unpaid since the 14th." }
```

It writes the same `PAYMENT_PAST_DUE` row, with the same `previousStatus` and `newStatus`, plus a
reason it insists on. A dedicated endpoint would have been a second way to reach one field, with
its own refusals to keep in step — the same reasoning that withdrew [#15](#e15).

**`SubscriptionStatus.PAST_DUE` and `SubscriptionEventType.PAYMENT_PAST_DUE` both stay.** The
state is real and worth recording; what is gone is a separate door to it.

**The three below are not the same case, and are still planned.** #19 cuts a school off, which
needs a grace period behind it rather than a field edit; #21 also clears `autoRenew`; #22 also
sets `current` = false, which #14 deliberately cannot touch. #20, resume, is the one that looks
most like #18 — worth deciding on its own terms rather than by this precedent.

**The gap in the numbering is deliberate**, for the same reason as #15's: the numbers are
referenced from the code, from Postman and from the other module READMEs.

<a id="e19"></a>
**[19](#t19) · `POST /platform/schools/{id}/subscriptions/current/suspend`** — built

- [`schools`](../../models/core/School.java) — *reads* `status`, `schoolName`; *updates* `status` = `SUSPENDED`, `suspendedAt`, `statusReason` — **only when the school is `ACTIVE`**
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `SUSPENDED`, `reasonForChanges`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads* the plan, for the response only
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `SUSPENDED`, `previousStatus`, `newStatus` = `SUSPENDED`, both plan ids (the same plan), `source`, `reason`, `performedByDocsId`, `effectiveAt`
- **No date on the subscription.** When it happened is the history row's `effectiveAt`; a second copy on the document could only disagree with it. The *school* does get `suspendedAt`, because that field already exists and core's own suspend maintains it

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  // REQUIRED, max 500, not blank
  "reason": "Invoice INV/2026/08/000412
             unpaid 30 days past the
             grace period."
}

// One field, and it is required. This
// endpoint stops a school working, so
// "the bill is unpaid" is not enough on
// its own — which bill, and how far past
// the grace period, is what makes the
// decision answerable later.
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "subscriptionNo": "SUB/2026/09/000001",
  "status": "SUSPENDED",
  "reasonForChanges": "Invoice INV/... unpaid 30 days ...",
  ...every other field unchanged
  "note": "Suspended from ACTIVE. Every feature is now refused: #34 reads SUSPENDED and answers allowed:false on all of them. The school is now SUSPENDED too, which is what actually blocks it ... NOTHING killed the school's live sessions or stopped its scheduled jobs ... The period was not paused: it still ends Thursday 8 October 2026 5:30AM, so the school is losing time it has paid for."
}

409 SUBSCRIPTION_NOT_SUSPENDABLE — not ACTIVE or PAST_DUE
409 SCHOOL_NOT_SUSPENDABLE      — the school is winding down
400 VALIDATION_FAILED           — no reason, or a blank one
404 SCHOOL_NOT_FOUND            — no such school
404 SUBSCRIPTION_NOT_FOUND      — the school has none
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts |
|---|---|---|
| `reason` | **yes** | Max 500, `@NotBlank`. Stored in **three** places, because three people ask: on the subscription as `reasonForChanges`, on the school as `statusReason` (so whoever finds the school locked can see why), and on the history row. |

### It moves two documents, because one would stop nothing

| | What it does |
|---|---|
| `school_subscriptions.status` = `SUSPENDED` | turns **every feature** off — [#34](#e34) reads it and answers `allowed: false` on all of them |
| `schools.status` = `SUSPENDED` | blocks **the tenant** — `CurrentSchoolResolver.requireUsable()` reads it, so school-surface writes answer `409 SCHOOL_NOT_EDITABLE` |

Writing only the subscription would leave a school that had been "suspended" still editing its own
records. Verified both ways round: a school-surface `PATCH` answers `200` before, `409
SCHOOL_NOT_EDITABLE` while suspended, and `200` again after [#20](#e20).

**Only an `ACTIVE` school's status moves.** A `PROVISIONING` one was never usable, so there is
nothing to block, and one already `SUSPENDED` keeps the `suspendedAt` and reason it has rather than
having the clock reset. The `note` says which of the three happened.

### What it refuses, and why each one

| Subscription status | Suspend |
|---|---|
| `ACTIVE` | allowed |
| `PAST_DUE` | allowed — the ordinary case, the bill having gone unpaid |
| `SUSPENDED` | `409` — already suspended, nothing to do |
| `TRIAL` | `409` — no unpaid bill behind a trial, so this is not that decision |
| `CANCELLED`, `EXPIRED` | `409` — ended rather than paused, so there is no access left to stop |

**Refusing a trial is what keeps [#20](#e20) simple.** Because only `ACTIVE` and `PAST_DUE` can be
suspended — and a school that has paid is not `PAST_DUE` any more — resume can go straight to
`ACTIVE` without looking up what the status used to be.

### What it does NOT stop

Nothing kills the school's live sessions and nothing halts its scheduled jobs, because neither
exists in this codebase yet. A user already signed in is refused at the next request that checks,
not thrown out mid-page. The `note` says so on every response rather than leaving it assumed — the
same honesty core's own suspend carries.

<a id="e20"></a>
**[20](#t20) · `POST /platform/schools/{id}/subscriptions/current/resume`** — built

- [`schools`](../../models/core/School.java) — *reads* `status`, `schoolName`; *updates* `status` = `ACTIVE` and `statusReason` — **only when the school is `SUSPENDED`**. `suspendedAt` is deliberately left standing: it is when the suspension began, and a resumed school's history is worth keeping. Core's own reactivate leaves it too
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `ACTIVE`, `reasonForChanges`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads* the plan, for the response only
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RESUMED`, `previousStatus` = `SUSPENDED`, `newStatus` = `ACTIVE`, both plan ids, `source`, `reason`, `effectiveAt`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  // REQUIRED, max 500, not blank
  "reason": "Invoice INV/2026/08/000412
             paid in full on 2026-09-08."
}

// Required for the same reason #19's is:
// a record that says exactly why a school
// was cut off but only "resumed" for why
// it came back answers half the question.
// Naming the payment closes it.
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "status": "ACTIVE",
  "reasonForChanges": "Invoice INV/... paid in full ...",
  ...every other field unchanged, INCLUDING both period dates
  "note": "Resumed to ACTIVE. Every feature the plan includes is allowed again. The school is ACTIVE again, so the tenant is reachable. Its suspendedAt is left standing ... The period was NOT extended: it still ends Thursday 8 October 2026 5:30AM, so the school has paid for the time it was locked out of."
}

409 SUBSCRIPTION_NOT_RESUMABLE — not SUSPENDED
409 SCHOOL_NOT_RESUMABLE       — the school is winding down
400 VALIDATION_FAILED          — no reason, or a blank one
404 SCHOOL_NOT_FOUND           — no such school
404 SUBSCRIPTION_NOT_FOUND     — the school has none
</pre></td>
</tr>
</table>

### It resumes to ACTIVE without looking anything up

[#19](#e19) only ever suspends an `ACTIVE` or a `PAST_DUE` subscription, and a school that has paid
is not `PAST_DUE` any more — so `ACTIVE` is the only sensible answer and no history lookup is
needed to find it. Refusing to suspend a trial is what buys that.

**Nothing else moves.** Not the plan, not the price, not the ceilings, not either period date — a
suspension pauses access, and lifting it renegotiates nothing. Verified field by field across the
pair, and the collection still holds one row: unlike #16 and #17, this writes no second document.

### The period is not extended, and that is deliberate

A school suspended for three weeks comes back to the same `currentPeriodEnd` it had, so it has paid
for time it could not use. Crediting that is a **money** decision: nothing in this codebase raises
or credits an invoice, so quietly moving the end date would be this endpoint inventing a refund.
The `note` says so plainly instead. If a credit was agreed, [#14](#e14) is where the date moves —
deliberately, by somebody, with a reason recorded.

### What it refuses

| Subscription status | Resume |
|---|---|
| `SUSPENDED` | allowed |
| `ACTIVE` | `409` — nothing to resume |
| `TRIAL`, `PAST_DUE` | `409` — never suspended, so not paused |
| `CANCELLED`, `EXPIRED` | `409` — ended rather than paused; reopening one would be selling a period without saying so, which is [#13](#e13) or [#16](#e16) |

<a id="e21"></a>
**[21](#t21) · `POST /platform/schools/{id}/subscriptions/current/cancel`** — built

- [`schools`](../../models/core/School.java) — *reads* `status`, `schoolName`. **No write**: this is a commercial end, not a lock-out, and a `CLOSED` or `OFFBOARDING` school is deliberately allowed
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `CANCELLED`, `autoRenew` = false, `reasonForChanges`, and on `immediate: true` also `currentPeriodEnd` trimmed to now
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads* the plan, for the response only
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `CANCELLED`, `previousStatus`, `newStatus` = `CANCELLED`, both plan ids, `source`, `reason`, `effectiveAt`
- **No new field on the model.** See below

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
{
  // REQUIRED, max 500, not blank
  "reason": "School closing at the end of
             the academic year.",

  "immediate": false   // optional
}

// Absent or false is the ordinary
// cancellation: the school keeps the
// product for the time it paid for.
</pre></td>
<td><pre>
200 OK — the whole subscription, as #27 returns it

{
  "status": "CANCELLED",
  "autoRenew": false,
  "currentPeriodEnd": "2026-10-08T00:00:00Z",   // untouched
  "periodEnded": false,                          // still granting
  "reasonForChanges": "School closing ...",
  "note": "Cancelled, and the school keeps working until Thursday 8 October 2026 5:30AM ... It will NOT be renewed or resumed ... NOTHING marks it EXPIRED when that date passes."
}

409 SUBSCRIPTION_ALREADY_ENDED      — EXPIRED, or cancelled and lapsed
409 CANCELLATION_ALREADY_SCHEDULED  — cancelled, period still running
409 SUBSCRIPTION_NOT_CANCELLABLE    — the school is deleted
400 VALIDATION_FAILED               — no reason, or a blank one
404 SCHOOL_NOT_FOUND / SUBSCRIPTION_NOT_FOUND
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts |
|---|---|---|
| `reason` | **yes** | Max 500, `@NotBlank`. Stored as `reasonForChanges` and on the history row. This is the one transition nothing undoes, so an unexplained end is the one nobody can answer for. |
| `immediate` | no | Absent or `false` is the ordinary cancellation. `true` trims `currentPeriodEnd` to now, which is what stops the access. It refunds nothing. |

### The status says cancelled; the period says how long for

**No field was added for "cancelled but still running."** The status goes `CANCELLED` either way,
because the contract is over either way and that is simply true. What decides whether the school
can still work is `currentPeriodEnd` — `whyNotActive` now lets a cancelled subscription grant until
that date passes:

| | `status` | `currentPeriodEnd` | Access |
|---|---|---|---|
| the default | `CANCELLED` | left alone | until the period runs out |
| `immediate: true` | `CANCELLED` | **trimmed to now** | stops at once |

That is the same division of labour [#16](#e16) uses when it closes the row a school leaves: the
dates record the period actually served.

**Nothing quietly undoes it, and no new check was needed.** [#17](#e17) already refuses to renew a
`CANCELLED` subscription and [#20](#e20) already refuses to resume one. Bringing the school back
means selling it something new — [#13](#e13) or [#16](#e16).

**The immediate shape overwrites what the school had paid for**, so the history row's `reason`
records the original end date. That is the only place it survives, which is where #16 puts a
superseded subscription number for the same reason.

### What it refuses

| Subscription | Cancel |
|---|---|
| `ACTIVE`, `PAST_DUE`, `TRIAL`, `SUSPENDED` | allowed — a trial that did not convert and a school that never paid both end here |
| `CANCELLED`, period still running | `409 CANCELLATION_ALREADY_SCHEDULED` — but `immediate: true` **is** allowed, because escalating is a real decision |
| `CANCELLED`, period lapsed | `409 SUBSCRIPTION_ALREADY_ENDED` |
| `EXPIRED` | `409 SUBSCRIPTION_ALREADY_ENDED` |

**Almost everything can be cancelled**, which is the opposite of #19 and #20 — and a `CLOSED` or
`OFFBOARDING` school is allowed too, unlike there. Cancelling the subscription is *part of* winding
a school down; refusing it would leave a closed school with a live subscription nobody could end.
Only a deleted school is refused.

### What nothing does yet

Nothing marks a lapsed subscription `EXPIRED`. So a cancellation that has served out its period
reads `CANCELLED` with `periodEnded: true` rather than `EXPIRED` — every field correct, and still
not tidied away. **#22** is where that belongs.

And no money moves: an immediate cancellation keeps whatever was paid for the part of the period
being given up, because nothing in this codebase raises, credits or refunds an invoice.


<a id="e22"></a>
**[~~22~~](#t22) · ~~`POST /platform/schools/{id}/subscriptions/{no}/expire`~~** — not being built

**Dropped on 2026-09-08, before being built. A job will do it.**

Its own one-line description gave the reason away: *"normally the nightly job does this."* An
endpoint whose normal caller is a scheduler is a job with an HTTP door on it. Nothing about
expiring is a decision somebody makes — the period end passes, and the subscription is over. There
is no reason to weigh, no grace period to apply, nothing to refuse. It is a date arriving.

**The two statuses nothing may push a subscription into**, and why they are the same case:

| Status | How it is reached |
|---|---|
| [`PAST_DUE`](../../models/plans/enums/SubscriptionStatus.java) | **a job**, when an invoice goes unpaid past its terms — [#18](#e18) was dropped for this reason on 2026-09-07 |
| [`EXPIRED`](../../models/plans/enums/SubscriptionStatus.java) | **a job**, when `currentPeriodEnd` passes and nothing renewed it |

Both are the passage of time noticing something, not an operator deciding something. Every other
transition in this module has a person and a reason behind it — [#19](#e19) cuts a school off,
[#20](#e20) puts it back, [#21](#e21) ends the contract — and each requires a `reason` precisely
because somebody chose it. A date arriving has no reason to give.

**What this leaves open in the meantime**, which is the honest cost of not building it:

- A cancellation that has served out its period reads `CANCELLED` with `periodEnded: true` rather
  than `EXPIRED`. See [#21](#e21).
- A subscription whose period simply lapsed reads whatever it was — usually `ACTIVE` — with
  `periodEnded: true`. [#27](#e27) and [#33](#e33) both report that flag, and their `note` says so
  in a sentence, precisely so a screen does not have to trust `status` alone.

So the state is always *readable* and never wrong; it is only untidied. **Read `periodEnded`
alongside `status`** until the job exists.

**An operator who needs it now has [#14](#e14)**: `{"status": "EXPIRED", "reason": …}`. That is the
override, it records who did it and why, and it is the right tool for a one-off. What it is not is
a substitute for the job — nobody should be closing subscriptions by hand every morning.


<a id="e23"></a>
**[~~23~~](#t23) · ~~`PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew`~~** — superseded by [#14](#e14)

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `autoRenew`

Every field above is editable through [#14](#e14), which writes one history row for the whole
edit. Kept in the list so the number still resolves, and so it is clear this was decided rather
than forgotten.

<a id="e24"></a>
**[~~24~~](#t24) · ~~`PATCH /platform/schools/{id}/subscriptions/{no}/overrides`~~** — superseded by [#14](#e14)

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `maxStudentsOverride`, `maxUsersOverride` — either may be set back to null to fall through to the plan's own limits

Every field above is editable through [#14](#e14), which writes one history row for the whole
edit. Kept in the list so the number still resolves, and so it is clear this was decided rather
than forgotten.

<a id="e25"></a>
**[25](#t25) · `PATCH /platform/schools/{id}/subscriptions/{no}/price`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `contractedPrice`, `currencyCode`

**[#14](#e14) deliberately cannot do this.** What a school pays is what gets invoiced, and a
currency decides what money the invoice is in. Behind the same request as "push the trial out a
fortnight", repricing a school would look like an administrative tidy-up — so it stays a request
somebody has to make on purpose.

<a id="e26"></a>
**[26](#t26) · `PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference`

**[#14](#e14) deliberately cannot do this.** It is the payment provider's handle for this school
— the thing future charges are raised against — so it belongs with the money rather than with
the dates.

## The subscription — platform reads  ·  27–32

<a id="e27"></a>
**[27](#t27) · `GET /platform/schools/{id}/subscription`** — built

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: every field, found by `schoolId` and `current` = true
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `name`, `status`, `listPrice`, `maxStudents`, `maxUsers`, `features`
- [`schools`](../../models/core/School.java) — *reads*: `schoolName` — **only when there is no subscription**, to decide which `404` to give

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Path:  {id}  the school's Mongo id

The subscription is not named: a school has
exactly one current, and a unique partial
index makes sure of it.
</pre></td>
<td><pre>
200 OK — SubscriptionDetailResponse, the same shape #14 answers

{
  "subscriptionId": "6a9e93e37feee1a04ace4a3b",
  "subscriptionNo": "SUB/2026/09/000001",
  "schoolId": "6a9e598382db56ca8afb6086",
  "planDefinitionDocsId": "67aa1202dc3f7d0012345678",
  "planCode": "PREMIUM",
  "planVersion": 1,
  "planName": "Premium",
  "planStatus": "ACTIVE",
  "planRetired": false,
  "status": "ACTIVE",
  "billingCycle": "YEARLY",
  "currentPeriodStart": "2026-04-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "daysRemaining": 205,
  "periodEnded": false,
  "autoRenew": true,
  "current": true,
  "contractedPrice": 39999.50,
  "planListPrice": 49999.00,
  "currencyCode": "INR",
  "hasDiscount": true,
  "maxStudents": 2500,
  "maxUsers": 250,
  "maxStudentsOverride": 2500,
  "maxUsersOverride": null,
  "hasLimitOverrides": true,
  "featureCount": 2,
  "features": [
    {
      "featureCode": "STUDENT_MANAGEMENT",
      "label": "Student management",
      "description": "Admissions, records and transfers.",
      "enabled": true,
      "usageLimit": null,
      "usageMetric": null,
      "overagePolicy": null
    }
  ],
  "reasonForChanges": "Renegotiated at renewal.",
  "billingCustomerReference": "cus_Qx7B2mR9",
  "note": null
}

404 SUBSCRIPTION_NOT_FOUND  — the school has none
404 SCHOOL_NOT_FOUND       — no such school
</pre></td>
</tr>
</table>

**No request fields.** The only input is `{id}` in the path. The four computed fields are worth
naming, because nothing on the document holds them:

| Field | Where it comes from |
|---|---|
| `daysRemaining` | `currentPeriodEnd` minus now, in days. Negative once the period has run out, which is how a screen shows "ended 12 days ago". |
| `periodEnded` | `currentPeriodEnd` is in the past. Read it rather than `status`: nothing marks a subscription expired yet, so a lapsed period can sit behind a status that still says the school is paying. |
| `hasDiscount` | `contractedPrice` is below `planListPrice`. Both are returned, because the gap is what somebody rings up about. |
| `hasLimitOverrides` | This school's ceiling is **not** its plan's. Not a null check — #13 writes a figure onto every subscription, so a null check would answer true for all of them. |

### Singular, because a school has one

`/subscriptions` is the collection you post to; `/subscription` is the one they are on. A unique
partial index makes sure there is only ever one, so there is nothing to page through and nothing
to identify — which is also why this endpoint needs no `{no}` and so is not caught by the URL
problem [#14](#e14) ran into.

### The features are included here

The plan list reports a `featureCount`, because a few dozen feature rows on every row of a page
is noise. This is one school, and *what has this school actually paid for* is the question it
exists to answer, so the rows are the answer.

### Three things it works out, because every caller would otherwise do the same sum

| Field | What it says |
|---|---|
| `daysRemaining` | How long is left in the period. Counting days between two instants in a browser is where time zones go wrong. |
| `periodEnded` | The period's end has passed while the status still says the school is paying. |
| `planRetired` | The plan this school is on has been taken off the menu. Allowed and normal — see [#6](#e6) — but it is why the plan is not on the public list. |

**`periodEnded` is not hypothetical.** Nothing marks a subscription expired on its own — #21 and
#26 are not built — so a period lapses and the status stays as it was. A screen reading `status`
alone would show a school as paying months after its period ran out. `note` says so in a sentence
as well, and points at [#17](#e17): renewing is what starts the next period, one period per call.

`contractedPrice` and `planListPrice` are both reported, with `hasDiscount` when they differ: the
gap between them is the discount, and a discount is the thing somebody rings up about. The limits
work the same way — `maxStudents` is the ceiling in force, `maxStudentsOverride` says whether it
was negotiated.

### What #27 refuses

| Case | Code |
|---|---|
| no such school | `404 SCHOOL_NOT_FOUND` |
| the school exists but has no subscription | `404 SUBSCRIPTION_NOT_FOUND` |
| the plan the subscription points at is gone | `404 PLAN_NOT_FOUND` |

**Two 404s, not one.** The happy path is two reads — the subscription, then its plan. Only when
there is no subscription is the school looked up, to say which of the two problems it is: told
just "not found", somebody checks the subscription when the school id was wrong all along. That
read costs nothing on the path that succeeds.

<a id="e28"></a>
**[28](#t28) · `GET /platform/schools/{id}/subscriptions`** — built

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `subscriptionNo`, `status`, `planDefinitionDocsId`, `planVersion`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `contractedPrice`, `currencyCode`, `current`, `reasonForChanges`, `createdAt`, `updatedAt`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `name`. **One query for the whole page**, not one per row — see [performance](#e28-performance)

### What this answers that [#27](#e27) cannot

[#27](#e27) returns the row a school is on **now**, which is what a billing screen asks. This
returns **every row it has ever had**: the trial it started on, the plan it left, the period that
lapsed, the cancellation from two years ago. A school on its fourth plan has four documents in
`school_subscriptions`, and exactly one of them is `current`.

**Every status is included by default**, and that is the point — a history that hid the cancelled
and expired rows would hide the thing somebody opened it to find.

### Request

<table>
<tr><th align="left">Parameter</th><th align="left">Meaning</th></tr>
<tr valign="top"><td><code>page</code></td><td>Zero-based. Defaults to <b>0</b>. Negative is <code>400 INVALID_PAGE</code>.</td></tr>
<tr valign="top"><td><code>size</code></td><td>Defaults to <b>20</b>, capped at <b>100</b>. Outside 1–100 is <code>400 INVALID_PAGE_SIZE</code> — <b>refused, not clamped</b>: a caller who asked for 5000 rows and silently got 100 has been handed a page they will read as the whole answer.</td></tr>
<tr valign="top"><td><code>sort</code></td><td><code>field,direction</code>. Sortable on <code>currentPeriodStart</code>, <code>currentPeriodEnd</code>, <code>subscriptionNo</code>, <code>status</code>, <code>createdAt</code>, <code>updatedAt</code>. An <b>allow-list</b>, so nobody can order by an unindexed field or learn the document's shape by guessing names. Default <b><code>currentPeriodStart,desc</code></b>.</td></tr>
<tr valign="top"><td><code>status</code></td><td>Repeat for several — <code>?status=CANCELLED&amp;status=EXPIRED</code>. ORs within itself. <b>This is also how you ask for trials</b>: <code>?status=TRIAL</code>.</td></tr>
<tr valign="top"><td><code>billingCycle</code></td><td>Repeat for several. ORs within itself.</td></tr>
<tr valign="top"><td><code>planCode</code></td><td>Exact, case-insensitive, normalised on the way in — <code>?planCode=premium-plus</code> finds <code>PREMIUM_PLUS</code>. A code matching no plan gives an <b>empty page</b>, not an error.</td></tr>
<tr valign="top"><td><code>planVersion</code></td><td>One version. Only meaningful beside <code>planCode</code>.</td></tr>
<tr valign="top"><td><code>autoRenew</code></td><td><code>true</code> or <code>false</code>.</td></tr>
<tr valign="top"><td><code>current</code></td><td><code>true</code> is at most one row — the one [#27](#e27) returns. <code>false</code> is the history without it.</td></tr>
<tr valign="top"><td><code>startDateFrom</code>, <code>startDateTo</code></td><td>Instants, <b>inclusive</b> both ends. Filters <code>currentPeriodStart</code>.</td></tr>
<tr valign="top"><td><code>endDateFrom</code>, <code>endDateTo</code></td><td>Instants, <b>inclusive</b> both ends. Filters <code>currentPeriodEnd</code>.</td></tr>
</table>

The filters combine with **AND**; only `status` and `billingCycle` OR within themselves.

**There is no `trial` filter, and that is deliberate.** There is no `trial` field on
`school_subscriptions` — a trial is a **status**. [#13](#e13) takes a `trial` flag on the way in
and it lands on `status`; nothing keeps it as a field of its own. A boolean here would be a second
way to ask one question, and two ways to ask one question eventually disagree.

**`planCode` is not on the document either.** A subscription links to a plan *version* by
`planDefinitionDocsId`; the code lives on `plan_definitions`. So the filter resolves the code to
version ids first — one extra query, still in the database, never by reading subscriptions and
sifting them in memory.

### Response

`200 OK` — the shared [`PageResponse`](../../common/web/PageResponse.java) envelope, the same six
fields every list endpoint returns, wrapping `SubscriptionSummaryResponse` rows.

```jsonc
{
  "content": [
    {
      "subscriptionId": "6a9ffe1d7feee1a04ace4b7d",
      "subscriptionNo": "SUB/2026/09/000004",
      "planCode": "PREMIUM",          // from plan_definitions, so the row is readable
      "planVersion": 1,
      "planName": "Premium",
      "status": "CANCELLED",
      "billingCycle": "HALF_YEARLY",
      "currentPeriodStart": "2026-09-09T00:00:00Z",
      "currentPeriodEnd": "2027-03-08T00:00:00Z",
      "periodEnded": false,           // computed — NOT derivable from status, see below
      "autoRenew": false,
      "current": true,                // exactly one row in the whole history
      "contractedPrice": 100.00,
      "currencyCode": "INR",
      "reasonForChanges": "School closing at the end of the academic year.",
      "createdAt": "2026-09-08T12:22:53.310Z",
      "updatedAt": "2026-09-08T12:23:20.481Z"
    }
  ],
  "page": 0, "size": 20, "totalElements": 4,
  "totalPages": 1, "hasNext": false, "hasPrevious": false
}
```

**`periodEnded` is computed and is not derivable from `status`.** Nothing marks a lapsed
subscription `EXPIRED` yet, so a row can read `ACTIVE` with a period that finished months ago.
It is worked out once for the whole page from a single `now`, so no two rows on one page can
disagree about it.

**What is deliberately NOT in a row:**

| Withheld | Why |
|---|---|
| `planDefinitionDocsId` | An internal document id with no meaning to a caller. Everything it was needed for — which plan, which version — is spelled out beside it. |
| `schoolId` | It is in the URL. |
| `billingCustomerReference` | A payment-gateway customer id. A list is the wrong place to spray it across twenty rows; it belongs in [#27](#e27), where somebody is actually working on billing. |
| the plan's features | A list is read to find a period, not to work on one. [#27](#e27) returns them in full. |

`subscriptionId` **is** returned, because it is how [#29](#e29) asks for one row's history, and
`subscriptionNo` because that is the number printed on the record.

### Errors

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_PAGE` | `page` is negative |
| 400 | `INVALID_PAGE_SIZE` | `size` is outside 1–100 |
| 400 | `INVALID_SORT_FIELD` | a field off the allow-list; the message lists the allowed ones |
| 400 | `INVALID_SORT_DIRECTION` | anything but `asc` or `desc` |
| 400 | `INVALID_DATE_RANGE` | `startDateFrom` after `startDateTo`, or `endDateFrom` after `endDateTo` |
| 400 | `VALIDATION_FAILED` | an unknown enum value, or a date that is not an instant — through the type-mismatch handler |
| 404 | `SCHOOL_NOT_FOUND` | no such school |

**A backwards window is a 400, not an empty page.** Mongo would answer it with zero rows quite
happily, and the caller would read that as "this school has nothing in that range" rather than
"you sent `from` and `to` the wrong way round".

**Parameter validation runs before the school is read**, so a malformed request costs no database
round trip — and a `404` is then only ever the answer to an otherwise valid ask.

<a id="e28-performance"></a>
### Performance — two queries for a page, three with a `planCode` filter

| | |
|---|---|
| the count, for `totalElements` | the filter and nothing else — with the page's skip and limit it would only ever count one page |
| the page | the same filter, plus the paging and the sort |
| the plans behind that page | **one** `findAllById` over the **distinct** plan ids on the page |

**The plan lookup is the N+1 that was avoided.** Each row needs its plan's code and name, and the
obvious way to get them — a lookup per row — makes a twenty-row page cost twenty-one queries.
Typically it is **two** plan ids for twenty rows anyway: a history is mostly repeated renewals of
the same version, each pointing at the same document.

**No new index was added, and that was checked rather than assumed.**
`school_subscription_status_period_idx` is `{schoolId: 1, status: 1, currentPeriodEnd: 1}`. Mongo
can use an index prefix, so the school-scoped match every one of these queries starts with is
served by its first key, and `?status=` by its first two. The default sort is on
`currentPeriodStart`, which that index does not cover — so it is a sort in memory, **deliberately**:
the candidate set is one school's subscriptions, a handful of documents even after ten years, and
an index earning nothing still costs a write on every subscription ever created.

### Pagination is stable, which took a fix to the shared sort code

The default order ends in `subscriptionNo`, which is **unique within a school** — so the order is
total and no two rows compare equal. Without that, two subscriptions sharing a
`currentPeriodStart` (which [#16](#e16) produces every time a plan changes on the day a period
begins) could come back in either order per query, and a row could appear on page one *and* page
two while another was never seen at all.

The tiebreaker is appended under whatever the caller sorts by — and doing that exposed a bug that
had been in [#8](#e8) since it was written. A Mongo sort is a document and cannot hold one key
twice; the driver keeps the last. So appending a fallback of `planCode ASC` under a caller's
`planCode DESC` produced `{planCode: -1, planCode: 1}` and sorted **ascending**: `?sort=planCode,desc`
had never worked. [`PageResponse.pageableOf`](../../common/web/PageResponse.java) now drops any fallback order whose field
the caller already named, which fixes both endpoints.

> **Note on tied rows.** With a stable tiebreaker, `desc` is *not* the exact reverse of `asc`:
> the tiebreaker is applied in the same direction either way, so tied rows keep their relative
> order. That is the stability working, not a bug.

### Security

Platform surface, so the school is named in the path rather than taken from a header — the
opposite of [#33](#e33), which is the school's own view and can only ever see itself. The school
id goes into the Mongo filter **first and unconditionally**, so a query can never return another
school's rows.

> **There is no authentication or authorization in this codebase yet** — no Spring Security, no
> token, no roles. Every `/platform/**` endpoint is reachable by anyone who can reach the port,
> and this one is no more and no less exposed than the twenty before it. Saying "follows the
> existing authorization rules" would be saying it follows nothing. When auth arrives it belongs
> in front of the whole platform surface, not inside this endpoint.

### Tests

- **59 unit tests**, no database: [`PageResponseTest`](../../../../../../../test/java/com/orbitastra/backend/common/web/PageResponseTest.java) for the shared page/size/sort resolution, [`ListSubscriptionsTest`](../../../../../../../test/java/com/orbitastra/backend/services/plans/ListSubscriptionsTest.java) for what the service does around the query — how many queries run, what the repository was handed, which check fired first — and [`SchoolSubscriptionRepositoryImplTest`](../../../../../../../test/java/com/orbitastra/backend/repositories/plans/schoolsubscription/SchoolSubscriptionRepositoryImplTest.java), which captures the Mongo `Query` and reads its criteria, because a dropped filter returns *more* rows and that looks like data rather than a bug.
- **80 end-to-end assertions** against a real Mongo: every filter alone and in combination, every sortable field in both directions, first/last/beyond-the-end pages, `size=1`, the maximum size, order stability across repeated requests, every refusal, and that two schools never see each other's rows.

<a id="e29"></a>
**[29](#t29) · `GET /platform/schools/{id}/subscriptions/{no}/history`** — built

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `subscriptionNo` — and it is how the subscription in the path is resolved and checked against the school
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *reads*: every field — `eventType`, `previousStatus`, `newStatus`, `previousPlanDefinitionDocsId`, `newPlanDefinitionDocsId`, `source`, `sourceEventId`, `reason`, `performedByDocsId`, `effectiveAt`, `createdAt`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `name`, for both plans a row names

### What this answers

**What changed, when it changed, who changed it and why** — the answer to "why did this school get
suspended", months after whoever did it has forgotten. Every endpoint that moves a subscription
writes a history row *in the same transaction as the change*, so the trail cannot be missing the
one event that explains the state.

Read-only, and the collection is append-only. A correction is a new row written by whichever
endpoint made the change; nothing about this endpoint can alter one.

### Naming the subscription in the path

A subscription number looks like `SUB/2026/09/000002` and **cannot be written in a URL** — the
slashes are path separators, and Tomcat rejects them encoded. So three forms are accepted:

| In the path | What it means |
|---|---|
| `current` | the subscription the school is on now |
| `6aa100755e32971b99de6109` | its id — what [#28](#e28) returns as `subscriptionId` |
| `SUB-with-no-slashes` | the number itself, for a caller that can send it |

The id form is why [#28](#e28) returns `subscriptionId` at all. The lookup carries the school id in
every one of the three, so a subscription belonging to another school is a **404**, not that
school's audit trail.

### Request

```
?page=0&size=20                       the first page, newest change first
?eventType=SUSPENDED&eventType=RESUMED  the cut-offs and the switch-backs
?status=CANCELLED                      changes that moved it TO cancelled
?previousStatus=TRIAL                  when the trial ended
?source=ADMIN_PORTAL                   exact, case-insensitive
?performedByDocsId=...                 the acting identity
?sourceEventId=billing_event_00004519  trace a row to the webhook that caused it
?reason=non-payment                    free-text search of the reason
?effectiveFrom=2026-04-01T00:00:00Z    changes effective this academic year
?recordedTo=2026-09-01T00:00:00Z       rows written before September
?sort=effectiveAt,asc                  oldest change first
```

All optional; they combine with **AND**. `eventType`, `status` and `previousStatus` are OR within
themselves. Pagination and the refusals are the same as [#28](#e28) — same shared
[`PageResponse.pageableOf`](../../common/web/PageResponse.java), same four error codes, same
default of 20 and cap of 100, refused rather than clamped.

**`effectiveAt` and `createdAt` are different dates, and both are filterable.** A cancellation
agreed today for the end of the period is *effective* at the end of the period and *recorded*
today. Filtering the wrong one silently answers a different question, so each window is named
after what it means — `effectiveFrom`/`effectiveTo` and `recordedFrom`/`recordedTo`.

**There is no plan-code filter, unlike #28.** A history row stores *two* plan links, so "rows
involving PREMIUM" would have to guess whether the caller means moved-off or moved-to.
`?eventType=PLAN_CHANGED` is the question that actually gets asked, and the response names both
plans so the reader can see which.

**Sortable on** `effectiveAt`, `createdAt`, `eventType`, `newStatus`, `previousStatus` — an
allow-list, so nobody can order by a field the endpoint does not expose or probe the document's
shape by guessing names.

### Response

One page of [`SubscriptionHistoryEntryResponse`](../../dto/plans/subscription/response/SubscriptionHistoryEntryResponse.java),
in the same envelope as #28.

**Both plans are named, not just linked.** A pair of Mongo ids cannot answer the question the row
exists for — nobody can tell from them whether a school moved from Premium v1 to Standard v3. So
each side carries `planCode`, `planVersion` and `planName`.

**A gap is left as a gap.** An audit trail is read to settle what actually happened, so a field the
endpoint cannot answer is `null` rather than filled in with something reasonable:

- `previousStatus` is null on a subscription's first row, because there was no previous status. It
  does not mean "unknown".
- **`performedByDocsId` is null on every row that exists today.** Nothing populates it yet — #13
  does not resolve the acting account — so `source` is the only answer to "who" the record
  currently holds, and `ADMIN_PORTAL` is the only value ever written. The filter for it is
  implemented and matches nothing; that is the honest state, not a bug.
- `reason` is null when whoever made the change gave none.
- A plan whose document has since been deleted leaves that side's three plan fields null rather
  than failing the page. A history is exactly where a deleted plan turns up.

**What is deliberately NOT in a row:** `schoolSubscriptionDocsId`. The caller named the
subscription in the URL, so echoing its internal id back on all twenty rows says nothing.
`subscriptionNo` is there instead, so a row copied out of a page still says what it belongs to.

**An event where the plan did not move shows the same plan on both sides**, because that is what
#19/#20/#21 write — recording which plan was in force when the school was cut off is the point.
This endpoint reports it unchanged rather than tidying one side to null.

### Errors

| Code | Status | When |
|---|---|---|
| `INVALID_PAGE` | 400 | `page` is negative |
| `INVALID_PAGE_SIZE` | 400 | `size` is below 1 or above 100 |
| `INVALID_SORT_FIELD` | 400 | the sort field is off the allow-list |
| `INVALID_SORT_DIRECTION` | 400 | the direction is not `asc` or `desc` |
| `INVALID_DATE_RANGE` | 400 | a window's `from` is after its `to` |
| `SCHOOL_NOT_FOUND` | 404 | no such school |
| `SUBSCRIPTION_NOT_FOUND` | 404 | no such subscription **in this school** |

**A subscription that belongs to another school is a 404, not a 403.** Confirming that somebody
else's subscription exists is itself a disclosure about the other school.

**A subscription with no history is an empty page, not a 404** — the subscription exists and the
honest answer is "nothing has happened to it". In practice no subscription has an empty trail,
because #13 writes `CREATED` in the same transaction as the subscription.

**Parameter validation runs before anything is read**, so a malformed request costs no database
round trip and a 404 is only ever the answer to an otherwise valid ask.

### Performance — two queries for a page, three when a row names a plan

The filter, the sort and the paging are all on the query, so one page of rows is read however long
the trail is. Two round trips for the page and its total, built from the same `Criteria` object so
they cannot drift apart, then at most one more for the plans.

**The N+1 that was avoided.** Each row needs up to two plans named, and a page of twenty plan
changes would be forty lookups. Both sides of every row go into one set of distinct ids and are
fetched in a single `findAllById` — one query, or none at all when no row on the page names a
plan.

**No new index was added, and that was measured rather than assumed.**
`school_subscription_event_time_idx` on
`{schoolId: 1, schoolSubscriptionDocsId: 1, effectiveAt: -1, createdAt: -1}` was already declared
on the document and matches this endpoint exactly — the first two keys are the equality match every
request makes, the last two are the default order in the same direction:

```
without it   COLLSCAN, 4189 documents examined, 11 returned
with it      IXSCAN,     11 documents examined, 11 returned
```

**It is not built in the dev database**, where the collection has only `_id_`. That is deliberate:
auto-index-creation is off because it cost six minutes on every boot, so indexes are built on
demand with `app.mongo.sync-indexes=true`. Until that is run against an environment, this endpoint
collection-scans there — a deployment step, not a code change.

### Pagination is stable even when two rows share an instant

The default order is `effectiveAt` desc, then `createdAt` desc, then **the row id** — and the id is
there because it is the only unique key. Two rows really can share both dates, and rows that
compare equal may come back in either order, so one could appear on page one and again on page two
while another was never seen at all. An audit trail that loses a row when you page through it is
worse than useless.

Whatever a caller sorts by goes first and the default follows underneath it as the tiebreaker, with
any key the caller named removed from the tail — the same shared code, and the same duplicate-key
fix, as #28.

The trailing `_id` costs the sort the tail of its index, and measuring says that costs nothing
here: the filter is an equality match on one subscription, so Mongo finishes the ordering over
tens of rows. #28 could not make the same trade, which is why its tiebreaker is `subscriptionNo`.

### Security

No authentication exists in this project yet — there is no Spring Security on the classpath — so
there is nothing for this endpoint to opt into. What it does enforce is the **tenant boundary**:
`schoolId` is on the subscription lookup and on the history query, separately from the caller's
filters, so it is always applied and can never be omitted. Two schools cannot see each other's
rows even given each other's ids.

Audit data, so the read is deliberately narrow: no write of any kind, and `POST`/`PUT`/`PATCH`/
`DELETE` on the URL are not mapped. When authentication arrives it belongs in front of the whole
platform surface, not inside this endpoint.

### Tests

- **40 unit tests**, no database: [`GetSubscriptionHistoryTest`](../../../../../../../test/java/com/orbitastra/backend/services/plans/GetSubscriptionHistoryTest.java) for what the service does around the query — which check fires first, how many reads run, what the repository is handed, what a deleted plan does — and [`SubscriptionHistoryRepositoryImplTest`](../../../../../../../test/java/com/orbitastra/backend/repositories/plans/subscriptionhistory/SubscriptionHistoryRepositoryImplTest.java), which captures the Mongo `Query` and reads its criteria.
- **113 end-to-end assertions** against a real Mongo: a five-event trail written by the real endpoints (`CREATED`, `TERMS_CHANGED`, `SUSPENDED`, `RESUMED`, `CANCELLED`), every filter alone and in combination, every sortable field in both directions, first/last/beyond-the-end pages, `size=1`, the maximum size, every refusal, and that two schools never see each other's rows.
- **Stable ordering was tested with six rows sharing one instant**, which the API cannot produce on its own: they page through one at a time, three at a time, and under four different caller sorts, and every row is seen exactly once in one total order.
- **The tenant clause is guarded by the unit tests, because no end-to-end test can reach it.** A history row is keyed by the subscription's globally-unique id, so deleting the tenant clause returns identical rows — all 113 live assertions passed with it removed. Six unit tests fail instead.

### Two bugs this endpoint found

**`Map.of().get(null)` throws.** Both list endpoints fetch a page's plans in one query and then read
each row's plan out of the map, and both have rows whose plan id can be null. When *no* row on the
page names a plan the map is the empty one, and `Map.of()` rejects a null key rather than answering
null the way `HashMap` does — so the page that needed no plan lookup at all was the one that failed
with a `NullPointerException`. #28 had the same latent fault. Both now go through
`utils.planFrom`, which checks the id rather than relying on which empty map you got.

**A comment that was confidently wrong.** #28's repository said two `where` clauses on one field
would lose one, "because a document cannot hold the same key twice". That is true of a *sort*
document — it is why `?sort=planCode,desc` silently sorted ascending — but not of these filters:
`andOperator` gives each criteria its own element of the `$and` **array**, so both ends of a window
apply either way. Splitting one was mutation-tested and changed no result. Keeping both ends on one
`Criteria` is a clarity choice; the comment now says so in both files.

<a id="e30"></a>
**[30](#t30) · `GET /platform/subscriptions`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `schoolId`, `subscriptionNo`, `status`, `planDefinitionDocsId`, `planVersion`, `currentPeriodEnd`, `contractedPrice`, `currencyCode`, `autoRenew`, `current`

<a id="e31"></a>
**[31](#t31) · `GET /platform/subscriptions/renewals-due`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd` — the filter; plus `status`, `autoRenew`, `contractedPrice`, `current`

<a id="e32"></a>
**[32](#t32) · `GET /platform/subscriptions/at-risk`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `currentPeriodEnd`, `autoRenew`, `current`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status`, `dueDate`, `outstandingAmount`

## The subscription — the school's own view  ·  33–38

<a id="e33"></a>
**[33](#t33) · `GET /schools/current/subscription`** — built

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `subscriptionNo`, `status`, `planVersion`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `contractedPrice`, `currencyCode`. **Not** `billingCustomerReference` or `reasonForChanges` — a payment-gateway id and an operator's internal note are both ours, not theirs
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `name`, `description`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Header:  X-School-Subdomain: springfield-high
         REQUIRED

The school never names itself in the path.
The tenant comes from the header, so no
school can ask for another's subscription.
</pre></td>
<td><pre>
200 OK — MySubscriptionResponse: 14 fields, not the platform's 32

{
  "subscriptionNo": "SUB/2026/09/000001",
  "status": "ACTIVE",
  "planName": "Premium",
  "planDescription": "Everything, for a school that has outgrown Starter.",
  "planVersion": 1,
  "billingCycle": "YEARLY",
  "price": 39999.50,
  "currencyCode": "INR",
  "currentPeriodStart": "2026-04-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "daysRemaining": 205,
  "periodEnded": false,
  "autoRenew": true,
  "note": null
}

404 SUBSCRIPTION_NOT_FOUND — this school has none
</pre></td>
</tr>
</table>

**No request fields.** What matters here is what is **left out**, which is why this is a separate
type from #27's rather than the same one:

| Withheld | Why |
|---|---|
| `planListPrice` | A school on a negotiated price would be shown a number it is not paying — either a discount somebody then has to explain, or an increase they will ring up about. They are shown `price`, which is what they pay. |
| `billingCustomerReference` | The payment gateway's id for them. Ours to hold, not theirs to see. |
| `maxStudentsOverride`, `maxUsersOverride` | "Your limit is 2500" is useful; "your limit was negotiated up from the plan's 2000" is a commercial conversation, not a billing screen. |
| `planCode` | The internal family key. A school reads the name. |
| `reasonForChanges` | An operator's note on the last edit — "invoice overdue", "renegotiated at renewal". Handing it over turns an internal note into a statement to a customer. |
| `current`, `schoolId`, ids | A school has one subscription and knows which school it is. |

`contractedPrice` is renamed **`price`** on the way out: from where the school sits there is only
one price, so calling it "contracted" invites the question of what the other one was.

**`note` is written for a school to read.** #27's version of it explains that nothing marks a
subscription expired yet — true, useful internally, and not something to tell a customer.

### It is not #27 with a different URL

[#27](#e27) is the platform read and shows everything. This one is shorter, and the four things
left out are why there are two response types rather than one shared one:

| Left out | Why |
|---|---|
| `planListPrice` | A school on a negotiated price would see a number it is not paying — either a discount somebody then has to explain, or an increase they will ring up about. |
| `billingCustomerReference` | The payment gateway's id for them. Ours to hold, not theirs to see. |
| `maxStudentsOverride`, `maxUsersOverride` | "Your limit is 2500" is useful. "Your limit was negotiated up from the plan's 2000" is a commercial conversation, not a billing screen. |
| `planCode` | The internal family key. A school reads the name. |

`contractedPrice` comes back as **`price`**: from where the school sits there is only one price,
and "contracted" only means something next to a list price they are not being shown.

### `note` is written for the school, not for us

#27's note explains that nothing marks a subscription expired yet — true, useful internally, and
not something to tell a customer. This one has its own branches and each says what it means for
the person paying: *"The current period ended on … Talk to us to carry on."* Nothing here names
an endpoint or admits what the module cannot do yet.

### It uses `require()`, not `requireUsable()`

A suspended or closed school can still read its own billing screen. Refusing to show it to
exactly the school that needs to look would be the wrong way round.

### What #33 refuses

| Case | Code |
|---|---|
| no tenant header | `400 TENANT_NOT_RESOLVED` |
| the subdomain matches no school | `404 SCHOOL_NOT_FOUND` |
| the school has no subscription | `404 SUBSCRIPTION_NOT_FOUND` |
| the plan the subscription points at is gone | `404 PLAN_NOT_FOUND` |

**There is no `{id}` in the path**, and that is the point. The tenant comes from
`CurrentSchoolResolver`, so a caller cannot ask about a school it does not belong to — reading
another school's bill would otherwise be a matter of editing a URL.

<a id="e34"></a>
**[34](#t34) · `GET /schools/current/subscription/feature-access`** — built

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `subscriptionNo`, `planDefinitionDocsId`, `planVersion`, `maxStudentsOverride`, `maxUsersOverride`, `currentPeriodEnd`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `name`, `maxStudents`, `maxUsers`, `features` — each feature's `featureCode`, `enabled`, `usageLimit`, `usageMetric` and `overagePolicy`

### Request and response

<table>
<tr><th align="left">Request body</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
No body — GET.

Header:  X-School-Subdomain: springfield-high
         REQUIRED

Called by the rest of the product, not by a
screen: "may this school do X, and how much
of it".
</pre></td>
<td><pre>
200 OK — and 200 even when the answer is "nothing"

{
  "active": true,
  "reason": null,
  "subscriptionNo": "SUB/2026/09/000001",
  "status": "ACTIVE",
  "planName": "Premium",
  "planVersion": 1,
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "maxStudents": 2500,
  "maxUsers": 250,
  "featureCount": 2,
  "features": [
    {
      "featureCode": "STUDENT_MANAGEMENT",
      "label": "Student management",
      "includedInPlan": true,
      "allowed": true,
      "usageLimit": null,
      "usageMetric": null,
      "overagePolicy": null
    }
  ]
}

When the subscription exists but grants
nothing — a lapsed period, or a cancelled one:

{
  "active": false,
  "reason": "The paid period ended on Wednesday 1 April 2026 5:29AM.",
  "maxStudents": 0,
  "maxUsers": 0,
  "featureCount": 0,
  "features": []
}

A school with NO subscription is a 404:

404 SUBSCRIPTION_NOT_FOUND
"This school has no subscription, so it is
 not entitled to anything."
</pre></td>
</tr>
</table>

**No request fields.** The shape is the point: this is the endpoint the rest of the product asks
before letting a school do something, so it answers in a form nothing has to interpret.

| Field | What a caller does with it |
|---|---|
| `active` | The one field a gate needs. **False on a `200`** when the subscription exists but grants nothing — a lapsed period, a cancelled or expired one — because "this school may do nothing" is an answer about a real customer rather than a failure. **A school with no subscription at all is `404 SUBSCRIPTION_NOT_FOUND`**: there is nothing to describe, and the message says so — *"This school has no subscription, so it is not entitled to anything."* So a caller has two cases to handle, not one. |
| `reason` | Why not, in words, when `active` is false. Null when it is true. |
| `maxStudents`, `maxUsers` | The ceilings **in force** — the school's negotiated figure where it has one, the plan's otherwise. **Zero when `active` is false**, so a caller that only reads the numbers still refuses rather than letting everything through. |
| `features[].includedInPlan` | The plan grants this feature. |
| `features[].allowed` | `includedInPlan` **and** the subscription is live. The pair is deliberate: a caller checking one field gets the right answer, and a screen explaining why can show that the feature is bought but the subscription is not paying. |
| `features[].usageLimit`, `usageMetric`, `overagePolicy` | How much, of what, and what happens past it — for the features that are metered rather than on or off. |

### The logic lives in one class, and that is the whole point

[`SchoolSubscriptionService`](../../services/plans/SchoolSubscriptionService.java) is the only
place that decides what a school may use. **A module that gates a feature calls that service directly**;
this endpoint is the same method with a URL in front of it. Nothing else may read
`plan_definitions.features` and decide for itself — two places working it out disagree, and they
disagree quietly, in the direction of letting a school use what it has not paid for. Transport
checking *"is TRANSPORT in the features list"* looks right and misses that the subscription was
cancelled last month.

It sits with #33 in the school-surface service rather than anywhere a platform endpoint could
reach it, so "what may this school use" has exactly one implementation and it is where a school's
own reads live.

### Read `allowed`, not `includedInPlan`

| Field | Means |
|---|---|
| `includedInPlan` | What the plan says. |
| `allowed` | Whether the school may use it **right now** — the plan saying yes *and* the subscription granting anything at all. |

**`allowed` is false on every feature when the subscription grants nothing**, and that is
deliberate rather than left to the caller to remember. A caller that reads the feature rows and
forgets the top-level `active` flag still gets the right answer, because the flag is already
folded in. Making the safe reading the easy one is the only way a rule like this survives a dozen
call sites.

Both fields are returned because a screen wants to say *"your plan includes Transport, but your
subscription has lapsed"* rather than simply hiding it. A gate wants `allowed` and nothing else.

### What counts as granting nothing

| State | Grants? |
|---|---|
| `ACTIVE`, `TRIAL` | yes |
| `PAST_DUE` | **yes, deliberately** — an unpaid invoice is a conversation to have, not a reason to lock a school out of its attendance register mid-morning |
| `SUSPENDED`, `CANCELLED`, `EXPIRED` | no |
| period end in the past, whatever the status says | no |

**That last row is not hypothetical.** Nothing marks a subscription expired on its own — #21 and
#26 are not built — so a period lapses while the status still reads `ACTIVE`. Trusting the status
alone would keep a school on a plan it stopped paying for, for as long as nobody noticed.
[#17](#e17) is what starts the next period, but it has to be called: nothing calls it yet. `reason` says which of these it is, because *"your subscription was cancelled"* and
*"your period ran out"* lead a school to do different things.

### No usage counts, on purpose

This says what the ceiling is, not how much of it is gone. Counting students and user accounts is
[#35](#e35), and it is separate because a gate check runs on every request that touches a feature
— counting rows on each one would be the most expensive query in the product. Callers hold their
own count and compare it against the limit here.

### What #34 refuses

| Case | Code |
|---|---|
| no tenant header | `400 TENANT_NOT_RESOLVED` |
| the subdomain matches no school | `404 SCHOOL_NOT_FOUND` |
| the school has no subscription | `404 SUBSCRIPTION_NOT_FOUND` |
| the plan the subscription points at is gone | `404 PLAN_NOT_FOUND` |

**A school with no subscription is a `404`, not an empty allowance.** It is entitled to nothing
either way, but an empty feature list would be indistinguishable from a plan that was published
with no features — and those two need different fixing.

<a id="e35"></a>
**[35](#t35) · `GET /schools/current/subscription/usage`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `maxStudentsOverride`, `maxUsersOverride`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `maxStudents`, `maxUsers`, `features`
- [`students`](../../models/student/Student.java) — *reads*: a count by `schoolId` and `status`
- [`user_accounts`](../../models/identity/UserAccount.java) — *reads*: a count by `schoolId` and `status`

<a id="e36"></a>
**[36](#t36) · `GET /schools/current/subscription/history`**

- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *reads*: `eventType`, `previousStatus`, `newStatus`, `effectiveAt`. **Not** `reason` or `performedByDocsId` — those are the operator's internal notes

<a id="e37"></a>
**[37](#t37) · `PATCH /schools/current/subscription/auto-renew`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `autoRenew` — and nothing else the school could reach

<a id="e38"></a>
**[38](#t38) · `POST /schools/current/subscription/cancel-request`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `currentPeriodEnd` — to tell the school what it would lose and when

## Invoices — writes  ·  39–46

<a id="e39"></a>
**[39](#t39) · `POST /platform/schools/{id}/subscription/invoices`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `contractedPrice`, `currencyCode`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *insert*: `invoiceNo`, `schoolSubscriptionDocsId`, `billingPeriodStart`, `billingPeriodEnd`, `issueDate`, `dueDate`, `status` = `DRAFT`, `currencyCode`, `subTotal`, `taxAmount`, `totalAmount` = `subTotal` + `taxAmount`, `paidAmount` = 0, `outstandingAmount` = `totalAmount`

<a id="e40"></a>
**[40](#t40) · `PATCH /platform/schools/{id}/subscription/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status` — must be `DRAFT`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `billingPeriodStart`, `billingPeriodEnd`, `dueDate`, `subTotal`, `taxAmount`, and `totalAmount` and `outstandingAmount` recalculated from them

<a id="e41"></a>
**[41](#t41) · `POST /platform/schools/{id}/subscription/invoices/{no}/issue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status` — must be `DRAFT`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `ISSUED`, `issuedAt`, `issueDate`

<a id="e42"></a>
**[42](#t42) · `POST /platform/schools/{id}/subscription/invoices/{no}/void`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `paidAmount` — a bill with money against it cannot simply be voided
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `VOID`, `voidedAt`, `voidReason`, `outstandingAmount` = 0

<a id="e43"></a>
**[43](#t43) · `PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `dueDate`, and `status` back from `OVERDUE` if the new date is in the future

<a id="e44"></a>
**[44](#t44) · `POST /platform/schools/{id}/subscription/invoices/{no}/record-payment`**

- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue` — to get the `paymentNo`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *insert*: `paymentNo`, `schoolSubscriptionDocsId`, `subscriptionInvoiceDocsId`, `status` = `SUCCEEDED`, `paymentMethod`, `amount`, `currencyCode`, `receivedAt`. `gatewayProvider` stays null — no gateway was involved
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status` = `PARTIALLY_PAID` or `PAID`, `paidAt` when `outstandingAmount` reaches zero
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `ACTIVE`, if the school was `PAST_DUE` and this clears it
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RESUMED`, only when the `status` above changed

<a id="e45"></a>
**[45](#t45) · `POST /platform/schools/{id}/subscription/invoices/{no}/remind`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `invoiceNo`, `dueDate`, `outstandingAmount`, `currencyCode`, `status` — nothing is written

<a id="e46"></a>
**[46](#t46) · `POST /platform/schools/{id}/subscription/invoices/{no}/write-off`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `VOID`, `voidedAt`, `voidReason`, `outstandingAmount` = 0 — **there is no `WRITTEN_OFF` `status`; see the note above**

## Invoices — the school's own view  ·  47–50

<a id="e47"></a>
**[47](#t47) · `GET /schools/current/subscription/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `invoiceNo`, `billingPeriodStart`, `billingPeriodEnd`, `issueDate`, `dueDate`, `status`, `currencyCode`, `totalAmount`, `paidAmount`, `outstandingAmount`. Rows with `status` = `DRAFT` are left out

<a id="e48"></a>
**[48](#t48) · `GET /schools/current/subscription/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: as above plus `taxAmount`, `subTotal`, `paidAt`, `voidedAt`, `voidReason`

<a id="e49"></a>
**[49](#t49) · `GET /schools/current/subscription/invoices/{no}/pdf`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field that appears on the printed bill

<a id="e50"></a>
**[50](#t50) · `GET /schools/current/subscription/outstanding`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `outstandingAmount` summed, plus `status`, `dueDate` and `currencyCode` to work out what is overdue

## Invoices — platform reads  ·  51–54

<a id="e51"></a>
**[51](#t51) · `GET /platform/schools/{id}/subscription/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field, drafts included

<a id="e52"></a>
**[52](#t52) · `GET /platform/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `schoolId`, `invoiceNo`, `status`, `issueDate`, `dueDate`, `currencyCode`, `totalAmount`, `paidAmount`, `outstandingAmount`

<a id="e53"></a>
**[53](#t53) · `GET /platform/invoices/overdue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `dueDate` and `status` — the filter; plus `outstandingAmount`, `schoolId`, `invoiceNo`

<a id="e54"></a>
**[54](#t54) · `GET /platform/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `status`, `paymentMethod`, `amount`, `receivedAt`
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo`, `status`, `failureCode`, `failureMessage`, `attemptedAt`

## Paying — the school  ·  55–60

<a id="e55"></a>
**[55](#t55) · `POST /schools/current/subscription/invoices/{no}/pay`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status`, `outstandingAmount`, `currencyCode` — you cannot pay a draft, a void or a settled bill
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo` — the highest so far, to number this one
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: `subscriptionInvoiceDocsId`, `attemptNo`, `status` = `INITIATED`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `idempotencyKey`, `attemptedAt`

<a id="e56"></a>
**[56](#t56) · `GET /schools/current/subscription/payments/{no}`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `status`, `amount`, `currencyCode`, `receivedAt`, `failureReason`
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `status`, `failureCode`, `failureMessage`

<a id="e57"></a>
**[57](#t57) · `GET /schools/current/subscription/payments`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `status`, `paymentMethod`, `amount`, `currencyCode`, `receivedAt`, `subscriptionInvoiceDocsId`

<a id="e58"></a>
**[58](#t58) · `GET /schools/current/subscription/payments/{no}/receipt`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `amount`, `currencyCode`, `paymentMethod`, `receivedAt`, `subscriptionInvoiceDocsId`

<a id="e59"></a>
**[59](#t59) · `POST /schools/current/subscription/payment-method`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference` — the card itself never touches us, only the provider's reference to it

<a id="e60"></a>
**[60](#t60) · `DELETE /schools/current/subscription/payment-method`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference` = null, and `autoRenew` = false, because a renewal with nothing to charge fails silently

## Payments — platform  ·  61–65

<a id="e61"></a>
**[61](#t61) · `GET /platform/schools/{id}/subscription/payments`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: every field, gateway references included

<a id="e62"></a>
**[62](#t62) · `GET /platform/invoices/{no}/attempts`**

- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo`, `status`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `gatewayAttemptReference`, `idempotencyKey`, `failureCode`, `failureMessage`, `attemptedAt`, `completedAt`

<a id="e63"></a>
**[63](#t63) · `POST /platform/schools/{id}/subscription/payments/{no}/refund`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status` = `REFUNDED` or `PARTIALLY_REFUNDED`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status`, `paidAt` cleared if the bill is no longer fully paid

<a id="e64"></a>
**[64](#t64) · `POST /platform/schools/{id}/subscription/payments/{no}/reconcile`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `settlementReference`, `settledAt`

<a id="e65"></a>
**[65](#t65) · `POST /platform/schools/{id}/subscription/payments/{no}/retry`**

- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: `attemptNo` — one higher, a **new** `idempotencyKey`, `status` = `INITIATED`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `attemptedAt`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status` — back to `PENDING` while the retry runs

## The payment provider talking to us  ·  66–69

<a id="e66"></a>
**[66](#t66) · `POST /billing/webhooks/{provider}`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *insert*: `gatewayProvider`, `providerEventId`, `providerEventType`, `processingStatus` = `RECEIVED`, `signatureValid`, `payloadHash`, `encryptedPayload`, `receivedAt`, `processingAttemptCount` = 0
- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *updates*: `processingStatus` through `VERIFIED` to `PROCESSED`, `relatedEntityType`, `relatedEntityDocsId`, `processedAt` — or `failureCode`, `failureMessage` and `nextRetryAt` when it goes wrong
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status`, `gatewayPaymentReference`, `gatewayOrderReference`, `receivedAt`, `failureReason`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status`, `paidAt`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` — when a payment brings a `PAST_DUE` school back
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType`, `source` = the provider, `sourceEventId` = `providerEventId`, `newStatus`, `effectiveAt`

<a id="e67"></a>
**[67](#t67) · `GET /platform/billing/webhooks`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: `gatewayProvider`, `providerEventId`, `providerEventType`, `processingStatus`, `signatureValid`, `receivedAt`, `processedAt`, `processingAttemptCount`, `nextRetryAt`. **Never** `encryptedPayload`

<a id="e68"></a>
**[68](#t68) · `GET /platform/billing/webhooks/{id}`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: as above plus `relatedEntityType`, `relatedEntityDocsId`, `payloadHash`, `failureCode`, `failureMessage`. Still not `encryptedPayload`

<a id="e69"></a>
**[69](#t69) · `POST /platform/billing/webhooks/{id}/replay`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: `encryptedPayload` — the only thing that decrypts it
- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *updates*: `processingStatus`, `processingAttemptCount`, `processedAt`, `nextRetryAt`, `failureCode`, `failureMessage`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: the same fields #66 would have set
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: the same fields #66 would have set
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: the same fields #66 would have set
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: only if #66 never got that far — `sourceEventId` stops it being written twice

## The jobs  ·  70–71

<a id="e70"></a>
**[70](#t70) · `POST /platform/billing/jobs/renew-due`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd`, `autoRenew`, `status`, `current` — the ones due today
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `currentPeriodStart`, `currentPeriodEnd`
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue`, once per invoice raised
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *insert*: the same fields as #39
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: the same fields as #55, for schools with a `billingCustomerReference` saved
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RENEWED`, `source` = the job, `effectiveAt`

<a id="e71"></a>
**[71](#t71) · `POST /platform/billing/jobs/age-overdue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `dueDate`, `status`, `outstandingAmount`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `OVERDUE`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `PAST_DUE`, then `SUSPENDED` once the grace period has run out
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `PAYMENT_PAST_DUE` or `SUSPENDED`, `source` = the job, `reason`, `effectiveAt`
