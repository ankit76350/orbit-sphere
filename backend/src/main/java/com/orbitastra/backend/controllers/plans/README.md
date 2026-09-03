# controllers/plans — API plan

**Five of 71 are built — #1 to #4 and #6**, the plan catalogue's whole life apart from
versioning: create a draft, edit it, set its features, publish it, retire it.

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

# The endpoints

Numbered straight through, 1 to 71. Grouped only so the list is readable.

## 1. The plan catalogue — writes (platform) · [Build order ↓](#build-order)

A plan version is **immutable once published**. That is the rule the whole group is shaped
around: you edit a draft, and after that you make a new version instead.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 1 — **built** | [`POST /platform/plans/drafts`](#e1) | Make a new plan. It starts as `DRAFT`, so nobody can buy it while we are still deciding the price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 2 — **built** | [`PATCH /platform/plans/{code}/versions/{version}`](#e2) | Fix the details of a plan that is still a draft — name, price, limits. Refused once the plan is published, because schools have already bought it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 3 — **built** | [`PUT /platform/plans/{code}/versions/{version}/features`](#e3) | Set the whole feature list of a draft plan in one go. Replacing the list is safer than editing one feature at a time, because a half-edited feature list is a plan nobody can price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 4 — **built** | [`POST /platform/plans/{code}/versions/{version}/publish`](#e4) | Turn a draft into a real plan schools can buy. From here the plan can never be edited again. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 5 — **deferred** | [`POST /platform/plans/{code}/versions/{version}/new-version`](#e5) | Copy a published plan into a new draft version, so we can change the price. The old version stays exactly as it was for the schools already on it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 6 — **built** | [`POST /platform/plans/{code}/versions/{version}/retire`](#e6) | Stop selling a plan. Schools already on it keep it and keep working; new schools just cannot pick it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 7 | [`PATCH /platform/plans/{code}/versions/{version}/availability`](#e7) | Say whether a plan shows on the public list or is only offered privately in a quote. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |

## 2. The plan catalogue — reads · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 8 | [`GET /platform/plans`](#e8) | The operator's list of every plan, filtered by status or code. This is the screen somebody opens to see what we sell. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 9 | [`GET /platform/plans/{code}/versions`](#e9) | Every version of one plan, newest first. Shows how the price changed over time and which version each school is on. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 10 | [`GET /platform/plans/{code}/versions/{version}`](#e10) | One plan version in full, with all its features. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 11 | [`GET /schools/current/plans`](#e11) | The plans **this school** is allowed to move to — published, still on sale, and public. The school's own upgrade screen reads this. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 12 | [`GET /schools/current/plans/{code}/versions/{version}/comparison`](#e12) | What would change if this school moved to that plan: the price difference, and any limit that would drop below what the school is already using. Stops a school upgrading into a plan that immediately blocks it. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 3. The subscription lifecycle — writes (platform) · [Build order ↓](#build-order)

The allowed moves are already written in [`models/plans/README.md`](../../models/plans/README.md)
under "Status workflow".

**Where the status changes, a `SubscriptionHistory` row is written in the same transaction** —
13, 14, 16, 17, 18, 19, 20, 21 and 22. That is not optional: the history is how anybody later
answers "why is this school suspended".

**15 and 23 to 26 write no history row, and that is a gap, not a decision.** They change real
commercial terms — the trial end date, the price, the agreed limits — but
[`SubscriptionEventType`](../../models/plans/enums/SubscriptionEventType.java) has no value that
describes any of them, so there is nothing honest to write. See the note at the end of this
file.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 13 | [`POST /platform/schools/{id}/subscriptions`](#e13) | Give a school its first subscription. This is what makes a school a paying customer, and it is the missing piece the core module already complains about — `activateSchool` currently lets a school go live with no subscription at all. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 14 | [`POST /platform/schools/{id}/subscriptions/{no}/activate`](#e14) | Move a trial to a paying subscription once the school has agreed to buy. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 15 | [`POST /platform/schools/{id}/subscriptions/{no}/extend-trial`](#e15) | Push the trial end date out. A sales decision, so only an operator can do it. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 16 | [`POST /platform/schools/{id}/subscriptions/{no}/change-plan`](#e16) | Move the school onto a different plan or a newer version, and say when the change starts and what happens to the money already paid. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 17 | [`POST /platform/schools/{id}/subscriptions/{no}/renew`](#e17) | Start the next billing period. Normally the nightly job calls this; an operator can call it by hand when something went wrong. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 18 | [`POST /platform/schools/{id}/subscriptions/{no}/mark-past-due`](#e18) | Mark that the bill was not paid on time. The school keeps working — this is the warning stage before anything is switched off. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 19 | [`POST /platform/schools/{id}/subscriptions/{no}/suspend`](#e19) | Stop the school using the product because the bill is still unpaid. Separate from #18 so nobody is cut off the day after a due date. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 20 | [`POST /platform/schools/{id}/subscriptions/{no}/resume`](#e20) | Switch the school back on after it pays. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 21 | [`POST /platform/schools/{id}/subscriptions/{no}/cancel`](#e21) | End the subscription with a reason. The school usually keeps working until the period it already paid for runs out. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 22 | [`POST /platform/schools/{id}/subscriptions/{no}/expire`](#e22) | Close a subscription whose last paid period has now ended. Normally the nightly job does this. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 23 | [`PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew`](#e23) | Turn automatic renewal on or off. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 24 | [`PATCH /platform/schools/{id}/subscriptions/{no}/overrides`](#e24) | Give one school a bigger student or user limit than its plan normally allows, because that is what was negotiated. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 25 | [`PATCH /platform/schools/{id}/subscriptions/{no}/price`](#e25) | Change the agreed price for this one school without changing the plan everybody else is on. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 26 | [`PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer`](#e26) | Save the payment provider's customer id against the school, so future charges can be raised against it. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 4. The subscription — reads (platform) · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 27 | [`GET /platform/schools/{id}/subscription`](#e27) | What this school is on right now: plan, price, status, when the period ends. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 28 | [`GET /platform/schools/{id}/subscriptions`](#e28) | Every subscription this school has ever had, including old cancelled ones. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 29 | [`GET /platform/schools/{id}/subscriptions/{no}/history`](#e29) | The full trail of what changed, when, who did it and why. The answer to "why did this school get suspended". | [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 30 | [`GET /platform/subscriptions`](#e30) | Every school's subscription in one list, filtered by status. The operator's main screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 31 | [`GET /platform/subscriptions/renewals-due`](#e31) | Which subscriptions renew in the next N days. Lets somebody see a renewal coming before it fails. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 32 | [`GET /platform/subscriptions/at-risk`](#e32) | Everything past due, suspended, or ending soon with auto-renew off. The list somebody works through on a Monday morning. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 5. The subscription — the school's own view · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 33 | [`GET /schools/current/subscription`](#e33) | What plan am I on, what does it cost, when does it renew. The school's billing screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 34 | [`GET /schools/current/subscription/entitlements`](#e34) | **The one the rest of the product needs.** Answers "is this school allowed to use this feature, and how much of it is left". Every module that gates a feature must ask this instead of reading the plan itself, because the moment two places work out entitlements they disagree. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 35 | [`GET /schools/current/subscription/usage`](#e35) | How much of each limit the school has used — students, users, whatever a feature counts. Shown next to the limits so a school can see itself getting close. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java), [`students`](../../models/student/Student.java), [`user_accounts`](../../models/identity/UserAccount.java) |
| 36 | [`GET /schools/current/subscription/history`](#e36) | The school's own view of its plan changes. Shows what happened, but not the operator's internal notes. | [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 37 | [`PATCH /schools/current/subscription/auto-renew`](#e37) | Lets a school turn off automatic renewal itself, rather than having to email us. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 38 | [`POST /schools/current/subscription/cancel-request`](#e38) | The school asks to cancel. It **requests** — it does not cancel. Cancelling is #21, and it stays with the operator so somebody talks to the school first. | **none — no model holds a cancellation request yet.** See the note at the end of this file. |

## 6. Invoices — writes (platform) · [Build order ↓](#build-order)

Money rules from [`models/plans/billing/README.md`](../../models/plans/billing/README.md):
`totalAmount = subTotal + taxAmount` and `outstandingAmount = totalAmount - paidAmount`. **Every
endpoint below keeps both true or fails.** An issued invoice is never deleted or quietly changed.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 39 | [`POST /platform/schools/{id}/subscription/invoices`](#e39) | Raise a bill for one billing period. Created as a draft so the amounts can be checked before the school sees it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 40 | [`PATCH /platform/schools/{id}/subscription/invoices/{no}`](#e40) | Correct a draft invoice before it is sent. Refused once it is issued. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 41 | [`POST /platform/schools/{id}/subscription/invoices/{no}/issue`](#e41) | Send the bill to the school. After this the amounts are fixed, and the only way to undo it is #42. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 42 | [`POST /platform/schools/{id}/subscription/invoices/{no}/void`](#e42) | Cancel a bill that should not have been sent, with a reason. The invoice stays in the records — it is marked void, never deleted, because a missing invoice number is a hole somebody has to explain. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 43 | [`PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date`](#e43) | Give a school more time to pay. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 44 | [`POST /platform/schools/{id}/subscription/invoices/{no}/record-payment`](#e44) | Write down money that came in outside the gateway — a bank transfer, a cheque, cash. Without this, any school not paying online can never be marked paid. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 45 | [`POST /platform/schools/{id}/subscription/invoices/{no}/remind`](#e45) | Send the school a reminder that the bill is unpaid. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 46 | [`POST /platform/schools/{id}/subscription/invoices/{no}/write-off`](#e46) | Accept that a bill will never be paid and close it, with a reason. Keeps the outstanding list honest instead of full of debts nobody is chasing. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 7. Invoices — the school's own view · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 47 | [`GET /schools/current/subscription/invoices`](#e47) | Every bill we have sent this school, newest first. Drafts are hidden — the school should not see a bill we have not sent. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 48 | [`GET /schools/current/subscription/invoices/{no}`](#e48) | One bill in full, with what is still owed on it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 49 | [`GET /schools/current/subscription/invoices/{no}/pdf`](#e49) | The bill as a file the school can download, keep and give to its accountant. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 50 | [`GET /schools/current/subscription/outstanding`](#e50) | One number: how much this school owes right now. What a banner at the top of the screen shows. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 8. Invoices — reads (platform) · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 51 | [`GET /platform/schools/{id}/subscription/invoices`](#e51) | Every invoice for one school, drafts included. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 52 | [`GET /platform/invoices`](#e52) | Invoices across every school, filtered by status and due date. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 53 | [`GET /platform/invoices/overdue`](#e53) | Everything unpaid and past its due date. The collections list. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 54 | [`GET /platform/invoices/{no}`](#e54) | One invoice in full, with its payments and every attempt at paying it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |

## 9. Paying — the school · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 55 | [`POST /schools/current/subscription/invoices/{no}/pay`](#e55) | Start paying a bill. Creates the attempt record and hands back whatever the gateway needs to show the school a payment page. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 56 | [`GET /schools/current/subscription/payments/{no}`](#e56) | Check whether a payment went through. The page the school lands back on after paying asks this. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| 57 | [`GET /schools/current/subscription/payments`](#e57) | Every payment this school has made. Its receipt list. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 58 | [`GET /schools/current/subscription/payments/{no}/receipt`](#e58) | A receipt for one payment, as a file. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 59 | [`POST /schools/current/subscription/payment-method`](#e59) | Save a card or set up a UPI mandate so renewals can charge automatically instead of somebody paying by hand every month. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 60 | [`DELETE /schools/current/subscription/payment-method`](#e60) | Remove the saved payment method. Auto-renew has to be dealt with at the same time, or the next renewal fails silently. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 10. Payments — platform · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 61 | [`GET /platform/schools/{id}/subscription/payments`](#e61) | Every payment from one school. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 62 | [`GET /platform/invoices/{no}/attempts`](#e62) | Every try at paying one bill, failures included, with what the gateway said went wrong. This is the screen for "the school says they paid and it did not work". | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| 63 | [`POST /platform/schools/{id}/subscription/payments/{no}/refund`](#e63) | Give money back, in full or in part. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 64 | [`POST /platform/schools/{id}/subscription/payments/{no}/reconcile`](#e64) | Match a payment to the money the bank actually settled, and mark it settled. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 65 | [`POST /platform/schools/{id}/subscription/payments/{no}/retry`](#e65) | Try a failed automatic charge again. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |

## 11. The payment provider talking to us · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 66 | [`POST /billing/webhooks/{provider}`](#e66) | Where the payment provider tells us a payment succeeded or failed. It checks the signature, saves the event exactly as it arrived, and only then applies it. **The same event arriving twice must change nothing the second time**, which is what the unique `provider + providerEventId` index is for. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 67 | [`GET /platform/billing/webhooks`](#e67) | Every event the provider sent us, and whether we managed to process it. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| 68 | [`GET /platform/billing/webhooks/{id}`](#e68) | One event in full, including what went wrong if it failed. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| 69 | [`POST /platform/billing/webhooks/{id}/replay`](#e69) | Process a failed event again after the bug is fixed. The raw payload was saved for exactly this. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

## 12. The jobs that run on their own · [Build order ↓](#build-order)

These do the work nobody clicks a button for. They are endpoints as well as scheduled jobs so
they can be run by hand when something needs fixing, and so they can be tested.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 70 | [`POST /platform/billing/jobs/renew-due`](#e70) | Find every subscription whose period ends today, raise the next invoice, and charge the saved payment method. The job that keeps the money coming in. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 71 | [`POST /platform/billing/jobs/age-overdue`](#e71) | Find bills that are past their due date and move those subscriptions to past due, then to suspended once the grace period runs out. The job that stops schools using the product for free forever. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

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
`GET /schools/current/subscription/entitlements`. Until it exists, each of them will invent its
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

**Grace period length.** #18 to #19 need a number of days, and #71 needs the same number. It
belongs in configuration, not in two services.

**What happens to money on a mid-period plan change.** #16 has to decide: charge the difference
now, credit it, or start the new plan at the next period. That is a commercial decision, not a
technical one, and it should be answered before #16 is written.

**`SubscriptionEventType` cannot describe five of these endpoints.** Filling in the collections
column above is what turned this up. The enum has `CREATED`, `TRIAL_STARTED`, `ACTIVATED`,
`PLAN_CHANGED`, `RENEWED`, `PAYMENT_PAST_DUE`, `SUSPENDED`, `RESUMED`, `CANCELLED` and `EXPIRED`
— all of them status moves. It has nothing for **#15 extend trial**, **#23 auto-renew changed**,
**#24 limits overridden**, **#25 price changed** or **#26 billing customer set**.

Four of those five change what the school is paying or what it is allowed to use, and right now
they would change it leaving no trace of who did it or why. Either add event types for them, or
decide in writing that these terms are silently mutable. **Adding the event types is almost
certainly right** — "who raised this school's student limit" is exactly the question the history
collection exists to answer.

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

## The plan catalogue — writes  ·  1–7

<a id="e1"></a>
**1 · `POST /platform/plans/drafts`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *insert*: `planCode`, `planVersion` = 1, `name`, `description`, `status` = `DRAFT`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `effectiveFrom`, `effectiveUntil`, `publiclyAvailable` = false, `features` = `[]`

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
**2 · `PATCH /platform/plans/{code}/versions/{version}`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status` — must be `DRAFT` or the edit is refused
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `effectiveFrom`, `effectiveUntil`

<a id="e3"></a>
**3 · `PUT /platform/plans/{code}/versions/{version}/features`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `features` — the whole list is replaced

### `featureCode` became an enum while this was built

It was a `String` on [`PlanFeature`](../../models/plans/embedded/PlanFeature.java). That accepted
`STUDNET_MANAGEMENT` with a `200`: the plan looked correct on every screen, and the entitlement
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
**4 · `POST /platform/plans/{code}/versions/{version}/publish`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`, `features`, `effectiveUntil`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `status` = `ACTIVE`, `effectiveFrom` if it was empty

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
**5 · `POST /platform/plans/{code}/versions/{version}/new-version`** — DEFERRED

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
**6 · `POST /platform/plans/{code}/versions/{version}/retire`** — built

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `status` = `RETIRED`, `effectiveUntil`
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
**7 · `PATCH /platform/plans/{code}/versions/{version}/availability`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *updates*: `publiclyAvailable`

## The plan catalogue — reads  ·  8–12

<a id="e8"></a>
**8 · `GET /platform/plans`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `name`, `status`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `publiclyAvailable`, `effectiveFrom`, `effectiveUntil`

<a id="e9"></a>
**9 · `GET /platform/plans/{code}/versions`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `planCode`, `planVersion`, `status`, `listPrice`, `effectiveFrom`, `effectiveUntil`, `createdAt`

<a id="e10"></a>
**10 · `GET /platform/plans/{code}/versions/{version}`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: every field, `features` included

<a id="e11"></a>
**11 · `GET /schools/current/plans`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`, `publiclyAvailable`, `effectiveFrom`, `effectiveUntil` — the filter; then `name`, `description`, `billingCycle`, `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `features`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `planDefinitionDocsId`, `planVersion` — to mark the plan the school is already on

<a id="e12"></a>
**12 · `GET /schools/current/plans/{code}/versions/{version}/comparison`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `planDefinitionDocsId`, `planVersion`, `contractedPrice`, `maxStudentsOverride`, `maxUsersOverride`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `listPrice`, `currencyCode`, `maxStudents`, `maxUsers`, `features` — of both the `current` and the target `version`

## The subscription lifecycle — writes  ·  13–26

<a id="e13"></a>
**13 · `POST /platform/schools/{id}/subscriptions`**

- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue` — to get the `subscriptionNo`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *insert*: `schoolId`, `subscriptionNo`, `planDefinitionDocsId`, `planVersion`, `status` = `TRIAL` or `ACTIVE`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `contractedPrice`, `currencyCode`, `maxStudentsOverride`, `maxUsersOverride`, `current` = true
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `schoolSubscriptionDocsId`, `eventType` = `CREATED` or `TRIAL_STARTED`, `previousStatus` = null, `newStatus`, `source`, `reason`, `performedByDocsId`, `effectiveAt`

<a id="e14"></a>
**14 · `POST /platform/schools/{id}/subscriptions/{no}/activate`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status` — must be `TRIAL`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `ACTIVE`, `currentPeriodStart`, `currentPeriodEnd`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `ACTIVATED`, `previousStatus` = `TRIAL`, `newStatus` = `ACTIVE`, `source`, `performedByDocsId`, `effectiveAt`

<a id="e15"></a>
**15 · `POST /platform/schools/{id}/subscriptions/{no}/extend-trial`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status` — must be `TRIAL`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `currentPeriodEnd`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: **nothing today** — no `eventType` describes it. See the note above.

<a id="e16"></a>
**16 · `POST /platform/schools/{id}/subscriptions/{no}/change-plan`**

- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `status`, `planVersion`, `listPrice`, `currencyCode`, `billingCycle`, `maxStudents`, `maxUsers` — of the target `version`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `planDefinitionDocsId`, `planVersion`, `contractedPrice`, `currencyCode`, `billingCycle`, and `currentPeriodStart`, `currentPeriodEnd` if the period restarts
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `PLAN_CHANGED`, `previousPlanDefinitionDocsId`, `newPlanDefinitionDocsId`, `previousStatus`, `newStatus`, `reason`, `performedByDocsId`, `effectiveAt`

<a id="e17"></a>
**17 · `POST /platform/schools/{id}/subscriptions/{no}/renew`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd`, `billingCycle`, `contractedPrice`, `autoRenew`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `currentPeriodStart`, `currentPeriodEnd` — moved to the next period
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue` — to get the `invoiceNo`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *insert*: `invoiceNo`, `schoolSubscriptionDocsId`, `billingPeriodStart`, `billingPeriodEnd`, `issueDate`, `dueDate`, `status` = `DRAFT`, `currencyCode`, `subTotal`, `taxAmount`, `totalAmount`, `paidAmount` = 0, `outstandingAmount` = `totalAmount`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RENEWED`, `previousStatus`, `newStatus`, `source`, `effectiveAt`

<a id="e18"></a>
**18 · `POST /platform/schools/{id}/subscriptions/{no}/mark-past-due`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `PAST_DUE`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `PAYMENT_PAST_DUE`, `previousStatus` = `ACTIVE`, `newStatus` = `PAST_DUE`, `reason`, `effectiveAt`

<a id="e19"></a>
**19 · `POST /platform/schools/{id}/subscriptions/{no}/suspend`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `SUSPENDED`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `SUSPENDED`, `previousStatus`, `newStatus` = `SUSPENDED`, `reason`, `performedByDocsId`, `effectiveAt`

<a id="e20"></a>
**20 · `POST /platform/schools/{id}/subscriptions/{no}/resume`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `ACTIVE`
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RESUMED`, `previousStatus`, `newStatus` = `ACTIVE`, `reason`, `effectiveAt`

<a id="e21"></a>
**21 · `POST /platform/schools/{id}/subscriptions/{no}/cancel`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `CANCELLED`, `cancelledAt`, `cancellationReason`, `autoRenew` = false
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `CANCELLED`, `previousStatus`, `newStatus` = `CANCELLED`, `reason`, `performedByDocsId`, `effectiveAt`

<a id="e22"></a>
**22 · `POST /platform/schools/{id}/subscriptions/{no}/expire`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd` — must already have passed
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `EXPIRED`, `current` = false
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `EXPIRED`, `previousStatus`, `newStatus` = `EXPIRED`, `source`, `effectiveAt`

<a id="e23"></a>
**23 · `PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `autoRenew`

<a id="e24"></a>
**24 · `PATCH /platform/schools/{id}/subscriptions/{no}/overrides`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `maxStudentsOverride`, `maxUsersOverride` — either may be set back to null to fall through to the plan's own limits

<a id="e25"></a>
**25 · `PATCH /platform/schools/{id}/subscriptions/{no}/price`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `contractedPrice`, `currencyCode`

<a id="e26"></a>
**26 · `PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference`

## The subscription — platform reads  ·  27–32

<a id="e27"></a>
**27 · `GET /platform/schools/{id}/subscription`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: every field, found by `schoolId` and `current` = true
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `name`, `planCode`, `maxStudents`, `maxUsers`, `features`

<a id="e28"></a>
**28 · `GET /platform/schools/{id}/subscriptions`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `subscriptionNo`, `status`, `planDefinitionDocsId`, `planVersion`, `currentPeriodStart`, `currentPeriodEnd`, `contractedPrice`, `currencyCode`, `current`, `cancelledAt`, `cancellationReason`

<a id="e29"></a>
**29 · `GET /platform/schools/{id}/subscriptions/{no}/history`**

- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *reads*: every field — `eventType`, `previousStatus`, `newStatus`, `previousPlanDefinitionDocsId`, `newPlanDefinitionDocsId`, `source`, `sourceEventId`, `reason`, `performedByDocsId`, `effectiveAt`

<a id="e30"></a>
**30 · `GET /platform/subscriptions`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `schoolId`, `subscriptionNo`, `status`, `planDefinitionDocsId`, `planVersion`, `currentPeriodEnd`, `contractedPrice`, `currencyCode`, `autoRenew`, `current`

<a id="e31"></a>
**31 · `GET /platform/subscriptions/renewals-due`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd` — the filter; plus `status`, `autoRenew`, `contractedPrice`, `current`

<a id="e32"></a>
**32 · `GET /platform/subscriptions/at-risk`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `currentPeriodEnd`, `autoRenew`, `current`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status`, `dueDate`, `outstandingAmount`

## The subscription — the school's own view  ·  33–38

<a id="e33"></a>
**33 · `GET /schools/current/subscription`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `subscriptionNo`, `status`, `planVersion`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`, `autoRenew`, `contractedPrice`, `currencyCode`. **Not** `billingCustomerReference` — that is ours, not theirs
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `name`, `description`

<a id="e34"></a>
**34 · `GET /schools/current/subscription/entitlements`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `planDefinitionDocsId`, `planVersion`, `maxStudentsOverride`, `maxUsersOverride`, `currentPeriodEnd`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `maxStudents`, `maxUsers`, `features` — each feature's `featureCode`, `enabled`, `usageLimit`, `usageMetric` and `overagePolicy`

<a id="e35"></a>
**35 · `GET /schools/current/subscription/usage`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `maxStudentsOverride`, `maxUsersOverride`
- [`plan_definitions`](../../models/plans/PlanDefinition.java) — *reads*: `maxStudents`, `maxUsers`, `features`
- [`students`](../../models/student/Student.java) — *reads*: a count by `schoolId` and `status`
- [`user_accounts`](../../models/identity/UserAccount.java) — *reads*: a count by `schoolId` and `status`

<a id="e36"></a>
**36 · `GET /schools/current/subscription/history`**

- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *reads*: `eventType`, `previousStatus`, `newStatus`, `effectiveAt`. **Not** `reason` or `performedByDocsId` — those are the operator's internal notes

<a id="e37"></a>
**37 · `PATCH /schools/current/subscription/auto-renew`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `autoRenew` — and nothing else the school could reach

<a id="e38"></a>
**38 · `POST /schools/current/subscription/cancel-request`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `status`, `currentPeriodEnd` — to tell the school what it would lose and when

## Invoices — writes  ·  39–46

<a id="e39"></a>
**39 · `POST /platform/schools/{id}/subscription/invoices`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `contractedPrice`, `currencyCode`, `billingCycle`, `currentPeriodStart`, `currentPeriodEnd`
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *insert*: `invoiceNo`, `schoolSubscriptionDocsId`, `billingPeriodStart`, `billingPeriodEnd`, `issueDate`, `dueDate`, `status` = `DRAFT`, `currencyCode`, `subTotal`, `taxAmount`, `totalAmount` = `subTotal` + `taxAmount`, `paidAmount` = 0, `outstandingAmount` = `totalAmount`

<a id="e40"></a>
**40 · `PATCH /platform/schools/{id}/subscription/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status` — must be `DRAFT`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `billingPeriodStart`, `billingPeriodEnd`, `dueDate`, `subTotal`, `taxAmount`, and `totalAmount` and `outstandingAmount` recalculated from them

<a id="e41"></a>
**41 · `POST /platform/schools/{id}/subscription/invoices/{no}/issue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status` — must be `DRAFT`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `ISSUED`, `issuedAt`, `issueDate`

<a id="e42"></a>
**42 · `POST /platform/schools/{id}/subscription/invoices/{no}/void`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `paidAmount` — a bill with money against it cannot simply be voided
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `VOID`, `voidedAt`, `voidReason`, `outstandingAmount` = 0

<a id="e43"></a>
**43 · `PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `dueDate`, and `status` back from `OVERDUE` if the new date is in the future

<a id="e44"></a>
**44 · `POST /platform/schools/{id}/subscription/invoices/{no}/record-payment`**

- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue` — to get the `paymentNo`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *insert*: `paymentNo`, `schoolSubscriptionDocsId`, `subscriptionInvoiceDocsId`, `status` = `SUCCEEDED`, `paymentMethod`, `amount`, `currencyCode`, `receivedAt`. `gatewayProvider` stays null — no gateway was involved
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status` = `PARTIALLY_PAID` or `PAID`, `paidAt` when `outstandingAmount` reaches zero
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `ACTIVE`, if the school was `PAST_DUE` and this clears it
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RESUMED`, only when the `status` above changed

<a id="e45"></a>
**45 · `POST /platform/schools/{id}/subscription/invoices/{no}/remind`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `invoiceNo`, `dueDate`, `outstandingAmount`, `currencyCode`, `status` — nothing is written

<a id="e46"></a>
**46 · `POST /platform/schools/{id}/subscription/invoices/{no}/write-off`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `VOID`, `voidedAt`, `voidReason`, `outstandingAmount` = 0 — **there is no `WRITTEN_OFF` `status`; see the note above**

## Invoices — the school's own view  ·  47–50

<a id="e47"></a>
**47 · `GET /schools/current/subscription/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `invoiceNo`, `billingPeriodStart`, `billingPeriodEnd`, `issueDate`, `dueDate`, `status`, `currencyCode`, `totalAmount`, `paidAmount`, `outstandingAmount`. Rows with `status` = `DRAFT` are left out

<a id="e48"></a>
**48 · `GET /schools/current/subscription/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: as above plus `taxAmount`, `subTotal`, `paidAt`, `voidedAt`, `voidReason`

<a id="e49"></a>
**49 · `GET /schools/current/subscription/invoices/{no}/pdf`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field that appears on the printed bill

<a id="e50"></a>
**50 · `GET /schools/current/subscription/outstanding`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `outstandingAmount` summed, plus `status`, `dueDate` and `currencyCode` to work out what is overdue

## Invoices — platform reads  ·  51–54

<a id="e51"></a>
**51 · `GET /platform/schools/{id}/subscription/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field, drafts included

<a id="e52"></a>
**52 · `GET /platform/invoices`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `schoolId`, `invoiceNo`, `status`, `issueDate`, `dueDate`, `currencyCode`, `totalAmount`, `paidAmount`, `outstandingAmount`

<a id="e53"></a>
**53 · `GET /platform/invoices/overdue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `dueDate` and `status` — the filter; plus `outstandingAmount`, `schoolId`, `invoiceNo`

<a id="e54"></a>
**54 · `GET /platform/invoices/{no}`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: every field
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `status`, `paymentMethod`, `amount`, `receivedAt`
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo`, `status`, `failureCode`, `failureMessage`, `attemptedAt`

## Paying — the school  ·  55–60

<a id="e55"></a>
**55 · `POST /schools/current/subscription/invoices/{no}/pay`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `status`, `outstandingAmount`, `currencyCode` — you cannot pay a draft, a void or a settled bill
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo` — the highest so far, to number this one
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: `subscriptionInvoiceDocsId`, `attemptNo`, `status` = `INITIATED`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `idempotencyKey`, `attemptedAt`

<a id="e56"></a>
**56 · `GET /schools/current/subscription/payments/{no}`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `status`, `amount`, `currencyCode`, `receivedAt`, `failureReason`
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `status`, `failureCode`, `failureMessage`

<a id="e57"></a>
**57 · `GET /schools/current/subscription/payments`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `status`, `paymentMethod`, `amount`, `currencyCode`, `receivedAt`, `subscriptionInvoiceDocsId`

<a id="e58"></a>
**58 · `GET /schools/current/subscription/payments/{no}/receipt`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: `paymentNo`, `amount`, `currencyCode`, `paymentMethod`, `receivedAt`, `subscriptionInvoiceDocsId`

<a id="e59"></a>
**59 · `POST /schools/current/subscription/payment-method`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference` — the card itself never touches us, only the provider's reference to it

<a id="e60"></a>
**60 · `DELETE /schools/current/subscription/payment-method`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `billingCustomerReference` = null, and `autoRenew` = false, because a renewal with nothing to charge fails silently

## Payments — platform  ·  61–65

<a id="e61"></a>
**61 · `GET /platform/schools/{id}/subscription/payments`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *reads*: every field, gateway references included

<a id="e62"></a>
**62 · `GET /platform/invoices/{no}/attempts`**

- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *reads*: `attemptNo`, `status`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `gatewayAttemptReference`, `idempotencyKey`, `failureCode`, `failureMessage`, `attemptedAt`, `completedAt`

<a id="e63"></a>
**63 · `POST /platform/schools/{id}/subscription/payments/{no}/refund`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status` = `REFUNDED` or `PARTIALLY_REFUNDED`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status`, `paidAt` cleared if the bill is no longer fully paid

<a id="e64"></a>
**64 · `POST /platform/schools/{id}/subscription/payments/{no}/reconcile`**

- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `settlementReference`, `settledAt`

<a id="e65"></a>
**65 · `POST /platform/schools/{id}/subscription/payments/{no}/retry`**

- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: `attemptNo` — one higher, a **new** `idempotencyKey`, `status` = `INITIATED`, `paymentMethod`, `amount`, `currencyCode`, `gatewayProvider`, `attemptedAt`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status` — back to `PENDING` while the retry runs

## The payment provider talking to us  ·  66–69

<a id="e66"></a>
**66 · `POST /billing/webhooks/{provider}`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *insert*: `gatewayProvider`, `providerEventId`, `providerEventType`, `processingStatus` = `RECEIVED`, `signatureValid`, `payloadHash`, `encryptedPayload`, `receivedAt`, `processingAttemptCount` = 0
- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *updates*: `processingStatus` through `VERIFIED` to `PROCESSED`, `relatedEntityType`, `relatedEntityDocsId`, `processedAt` — or `failureCode`, `failureMessage` and `nextRetryAt` when it goes wrong
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: `status`, `gatewayPaymentReference`, `gatewayOrderReference`, `receivedAt`, `failureReason`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `paidAmount`, `outstandingAmount`, `status`, `paidAt`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` — when a payment brings a `PAST_DUE` school back
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType`, `source` = the provider, `sourceEventId` = `providerEventId`, `newStatus`, `effectiveAt`

<a id="e67"></a>
**67 · `GET /platform/billing/webhooks`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: `gatewayProvider`, `providerEventId`, `providerEventType`, `processingStatus`, `signatureValid`, `receivedAt`, `processedAt`, `processingAttemptCount`, `nextRetryAt`. **Never** `encryptedPayload`

<a id="e68"></a>
**68 · `GET /platform/billing/webhooks/{id}`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: as above plus `relatedEntityType`, `relatedEntityDocsId`, `payloadHash`, `failureCode`, `failureMessage`. Still not `encryptedPayload`

<a id="e69"></a>
**69 · `POST /platform/billing/webhooks/{id}/replay`**

- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *reads*: `encryptedPayload` — the only thing that decrypts it
- [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) — *updates*: `processingStatus`, `processingAttemptCount`, `processedAt`, `nextRetryAt`, `failureCode`, `failureMessage`
- [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) — *updates*: the same fields #66 would have set
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: the same fields #66 would have set
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: the same fields #66 would have set
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: only if #66 never got that far — `sourceEventId` stops it being written twice

## The jobs  ·  70–71

<a id="e70"></a>
**70 · `POST /platform/billing/jobs/renew-due`**

- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *reads*: `currentPeriodEnd`, `autoRenew`, `status`, `current` — the ones due today
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `currentPeriodStart`, `currentPeriodEnd`
- [`number_sequences`](../../models/institution/NumberSequence.java) — *updates*: `nextValue`, once per invoice raised
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *insert*: the same fields as #39
- [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) — *insert*: the same fields as #55, for schools with a `billingCustomerReference` saved
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `RENEWED`, `source` = the job, `effectiveAt`

<a id="e71"></a>
**71 · `POST /platform/billing/jobs/age-overdue`**

- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *reads*: `dueDate`, `status`, `outstandingAmount`
- [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) — *updates*: `status` = `OVERDUE`
- [`school_subscriptions`](../../models/plans/SchoolSubscription.java) — *updates*: `status` = `PAST_DUE`, then `SUSPENDED` once the grace period has run out
- [`subscription_history`](../../models/plans/SubscriptionHistory.java) — *insert*: `eventType` = `PAYMENT_PAST_DUE` or `SUSPENDED`, `source` = the job, `reason`, `effectiveAt`
