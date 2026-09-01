# controllers/plans — API plan

**Nothing here is built yet.** This is the full list of endpoints the `plans` module needs,
written before any of them, so they can be built and reviewed one at a time — the same way
[`controllers/core`](../core/README.md) was done.

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
| `PlanDefinition` | `plan_definitions` | one version of a plan we sell — price, limits, features |
| `SchoolSubscription` | `school_subscriptions` | what one school actually bought |
| `SubscriptionHistory` | `subscription_history` | every change to that, never edited |
| `SubscriptionInvoice` | `subscription_invoices` | one bill for one billing period |
| `SubscriptionPayment` | `subscription_payments` | money that arrived against a bill |
| `PaymentAttempt` | `subscription_payment_attempts` | every try at the gateway, failures included |
| `BillingWebhookEvent` | `billing_webhook_events` | what the payment provider told us |

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

## 1. The plan catalogue — writes (platform)

A plan version is **immutable once published**. That is the rule the whole group is shaped
around: you edit a draft, and after that you make a new version instead.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 1 | `POST /platform/plans` | Make a new plan. It starts as `DRAFT`, so nobody can buy it while we are still deciding the price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 2 | `PATCH /platform/plans/{code}/versions/{version}` | Fix the details of a plan that is still a draft — name, price, limits. Refused once the plan is published, because schools have already bought it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 3 | `PUT /platform/plans/{code}/versions/{version}/features` | Set the whole feature list of a draft plan in one go. Replacing the list is safer than editing one feature at a time, because a half-edited feature list is a plan nobody can price. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 4 | `POST /platform/plans/{code}/versions/{version}/publish` | Turn a draft into a real plan schools can buy. From here the plan can never be edited again. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 5 | `POST /platform/plans/{code}/versions/{version}/new-version` | Copy a published plan into a new draft version, so we can change the price. The old version stays exactly as it was for the schools already on it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 6 | `POST /platform/plans/{code}/versions/{version}/retire` | Stop selling a plan. Schools already on it keep it and keep working; new schools just cannot pick it. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 7 | `PATCH /platform/plans/{code}/versions/{version}/availability` | Say whether a plan shows on the public list or is only offered privately in a quote. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |

## 2. The plan catalogue — reads

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 8 | `GET /platform/plans` | The operator's list of every plan, filtered by status or code. This is the screen somebody opens to see what we sell. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 9 | `GET /platform/plans/{code}/versions` | Every version of one plan, newest first. Shows how the price changed over time and which version each school is on. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 10 | `GET /platform/plans/{code}/versions/{version}` | One plan version in full, with all its features. | [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 11 | `GET /schools/current/plans` | The plans **this school** is allowed to move to — published, still on sale, and public. The school's own upgrade screen reads this. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 12 | `GET /schools/current/plans/{code}/versions/{version}/comparison` | What would change if this school moved to that plan: the price difference, and any limit that would drop below what the school is already using. Stops a school upgrading into a plan that immediately blocks it. | [`plan_definitions`](../../models/plans/PlanDefinition.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 3. The subscription lifecycle — writes (platform)

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
| 13 | `POST /platform/schools/{id}/subscriptions` | Give a school its first subscription. This is what makes a school a paying customer, and it is the missing piece the core module already complains about — `activateSchool` currently lets a school go live with no subscription at all. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 14 | `POST /platform/schools/{id}/subscriptions/{no}/activate` | Move a trial to a paying subscription once the school has agreed to buy. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 15 | `POST /platform/schools/{id}/subscriptions/{no}/extend-trial` | Push the trial end date out. A sales decision, so only an operator can do it. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 16 | `POST /platform/schools/{id}/subscriptions/{no}/change-plan` | Move the school onto a different plan or a newer version, and say when the change starts and what happens to the money already paid. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 17 | `POST /platform/schools/{id}/subscriptions/{no}/renew` | Start the next billing period. Normally the nightly job calls this; an operator can call it by hand when something went wrong. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 18 | `POST /platform/schools/{id}/subscriptions/{no}/mark-past-due` | Mark that the bill was not paid on time. The school keeps working — this is the warning stage before anything is switched off. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 19 | `POST /platform/schools/{id}/subscriptions/{no}/suspend` | Stop the school using the product because the bill is still unpaid. Separate from #18 so nobody is cut off the day after a due date. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 20 | `POST /platform/schools/{id}/subscriptions/{no}/resume` | Switch the school back on after it pays. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 21 | `POST /platform/schools/{id}/subscriptions/{no}/cancel` | End the subscription with a reason. The school usually keeps working until the period it already paid for runs out. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 22 | `POST /platform/schools/{id}/subscriptions/{no}/expire` | Close a subscription whose last paid period has now ended. Normally the nightly job does this. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 23 | `PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew` | Turn automatic renewal on or off. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 24 | `PATCH /platform/schools/{id}/subscriptions/{no}/overrides` | Give one school a bigger student or user limit than its plan normally allows, because that is what was negotiated. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 25 | `PATCH /platform/schools/{id}/subscriptions/{no}/price` | Change the agreed price for this one school without changing the plan everybody else is on. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 26 | `PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer` | Save the payment provider's customer id against the school, so future charges can be raised against it. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 4. The subscription — reads (platform)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 27 | `GET /platform/schools/{id}/subscription` | What this school is on right now: plan, price, status, when the period ends. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 28 | `GET /platform/schools/{id}/subscriptions` | Every subscription this school has ever had, including old cancelled ones. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 29 | `GET /platform/schools/{id}/subscriptions/{no}/history` | The full trail of what changed, when, who did it and why. The answer to "why did this school get suspended". | [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 30 | `GET /platform/subscriptions` | Every school's subscription in one list, filtered by status. The operator's main screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 31 | `GET /platform/subscriptions/renewals-due` | Which subscriptions renew in the next N days. Lets somebody see a renewal coming before it fails. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 32 | `GET /platform/subscriptions/at-risk` | Everything past due, suspended, or ending soon with auto-renew off. The list somebody works through on a Monday morning. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 5. The subscription — the school's own view

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 33 | `GET /schools/current/subscription` | What plan am I on, what does it cost, when does it renew. The school's billing screen. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 34 | `GET /schools/current/subscription/entitlements` | **The one the rest of the product needs.** Answers "is this school allowed to use this feature, and how much of it is left". Every module that gates a feature must ask this instead of reading the plan itself, because the moment two places work out entitlements they disagree. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java) |
| 35 | `GET /schools/current/subscription/usage` | How much of each limit the school has used — students, users, whatever a feature counts. Shown next to the limits so a school can see itself getting close. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`plan_definitions`](../../models/plans/PlanDefinition.java), [`students`](../../models/student/Student.java), [`user_accounts`](../../models/identity/UserAccount.java) |
| 36 | `GET /schools/current/subscription/history` | The school's own view of its plan changes. Shows what happened, but not the operator's internal notes. | [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 37 | `PATCH /schools/current/subscription/auto-renew` | Lets a school turn off automatic renewal itself, rather than having to email us. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 38 | `POST /schools/current/subscription/cancel-request` | The school asks to cancel. It **requests** — it does not cancel. Cancelling is #21, and it stays with the operator so somebody talks to the school first. | **none — no model holds a cancellation request yet.** See the note at the end of this file. |

## 6. Invoices — writes (platform)

Money rules from [`models/plans/billing/README.md`](../../models/plans/billing/README.md):
`totalAmount = subTotal + taxAmount` and `outstandingAmount = totalAmount - paidAmount`. **Every
endpoint below keeps both true or fails.** An issued invoice is never deleted or quietly changed.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 39 | `POST /platform/schools/{id}/subscription/invoices` | Raise a bill for one billing period. Created as a draft so the amounts can be checked before the school sees it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 40 | `PATCH /platform/schools/{id}/subscription/invoices/{no}` | Correct a draft invoice before it is sent. Refused once it is issued. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 41 | `POST /platform/schools/{id}/subscription/invoices/{no}/issue` | Send the bill to the school. After this the amounts are fixed, and the only way to undo it is #42. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 42 | `POST /platform/schools/{id}/subscription/invoices/{no}/void` | Cancel a bill that should not have been sent, with a reason. The invoice stays in the records — it is marked void, never deleted, because a missing invoice number is a hole somebody has to explain. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 43 | `PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date` | Give a school more time to pay. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 44 | `POST /platform/schools/{id}/subscription/invoices/{no}/record-payment` | Write down money that came in outside the gateway — a bank transfer, a cheque, cash. Without this, any school not paying online can never be marked paid. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`number_sequences`](../../models/institution/NumberSequence.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 45 | `POST /platform/schools/{id}/subscription/invoices/{no}/remind` | Send the school a reminder that the bill is unpaid. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 46 | `POST /platform/schools/{id}/subscription/invoices/{no}/write-off` | Accept that a bill will never be paid and close it, with a reason. Keeps the outstanding list honest instead of full of debts nobody is chasing. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 7. Invoices — the school's own view

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 47 | `GET /schools/current/subscription/invoices` | Every bill we have sent this school, newest first. Drafts are hidden — the school should not see a bill we have not sent. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 48 | `GET /schools/current/subscription/invoices/{no}` | One bill in full, with what is still owed on it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 49 | `GET /schools/current/subscription/invoices/{no}/pdf` | The bill as a file the school can download, keep and give to its accountant. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 50 | `GET /schools/current/subscription/outstanding` | One number: how much this school owes right now. What a banner at the top of the screen shows. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |

## 8. Invoices — reads (platform)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 51 | `GET /platform/schools/{id}/subscription/invoices` | Every invoice for one school, drafts included. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 52 | `GET /platform/invoices` | Invoices across every school, filtered by status and due date. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 53 | `GET /platform/invoices/overdue` | Everything unpaid and past its due date. The collections list. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 54 | `GET /platform/invoices/{no}` | One invoice in full, with its payments and every attempt at paying it. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |

## 9. Paying — the school

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 55 | `POST /schools/current/subscription/invoices/{no}/pay` | Start paying a bill. Creates the attempt record and hands back whatever the gateway needs to show the school a payment page. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 56 | `GET /schools/current/subscription/payments/{no}` | Check whether a payment went through. The page the school lands back on after paying asks this. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| 57 | `GET /schools/current/subscription/payments` | Every payment this school has made. Its receipt list. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 58 | `GET /schools/current/subscription/payments/{no}/receipt` | A receipt for one payment, as a file. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 59 | `POST /schools/current/subscription/payment-method` | Save a card or set up a UPI mandate so renewals can charge automatically instead of somebody paying by hand every month. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |
| 60 | `DELETE /schools/current/subscription/payment-method` | Remove the saved payment method. Auto-renew has to be dealt with at the same time, or the next renewal fails silently. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java) |

## 10. Payments — platform

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 61 | `GET /platform/schools/{id}/subscription/payments` | Every payment from one school. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 62 | `GET /platform/invoices/{no}/attempts` | Every try at paying one bill, failures included, with what the gateway said went wrong. This is the screen for "the school says they paid and it did not work". | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java) |
| 63 | `POST /platform/schools/{id}/subscription/payments/{no}/refund` | Give money back, in full or in part. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java) |
| 64 | `POST /platform/schools/{id}/subscription/payments/{no}/reconcile` | Match a payment to the money the bank actually settled, and mark it settled. | [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |
| 65 | `POST /platform/schools/{id}/subscription/payments/{no}/retry` | Try a failed automatic charge again. | [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java) |

## 11. The payment provider talking to us

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 66 | `POST /billing/webhooks/{provider}` | Where the payment provider tells us a payment succeeded or failed. It checks the signature, saves the event exactly as it arrived, and only then applies it. **The same event arriving twice must change nothing the second time**, which is what the unique `provider + providerEventId` index is for. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |
| 67 | `GET /platform/billing/webhooks` | Every event the provider sent us, and whether we managed to process it. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| 68 | `GET /platform/billing/webhooks/{id}` | One event in full, including what went wrong if it failed. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java) |
| 69 | `POST /platform/billing/webhooks/{id}/replay` | Process a failed event again after the bug is fixed. The raw payload was saved for exactly this. | [`billing_webhook_events`](../../models/plans/billing/BillingWebhookEvent.java), [`subscription_payments`](../../models/plans/billing/SubscriptionPayment.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

## 12. The jobs that run on their own

These do the work nobody clicks a button for. They are endpoints as well as scheduled jobs so
they can be run by hand when something needs fixing, and so they can be tested.

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| 70 | `POST /platform/billing/jobs/renew-due` | Find every subscription whose period ends today, raise the next invoice, and charge the saved payment method. The job that keeps the money coming in. | [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`subscription_payment_attempts`](../../models/plans/billing/PaymentAttempt.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java), [`number_sequences`](../../models/institution/NumberSequence.java) |
| 71 | `POST /platform/billing/jobs/age-overdue` | Find bills that are past their due date and move those subscriptions to past due, then to suspended once the grace period runs out. The job that stops schools using the product for free forever. | [`subscription_invoices`](../../models/plans/billing/SubscriptionInvoice.java), [`school_subscriptions`](../../models/plans/SchoolSubscription.java), [`subscription_history`](../../models/plans/SubscriptionHistory.java) |

---

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
