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

| # | Method and endpoint | What this API is for |
|---|---|---|
| 1 | `POST /platform/plans` | Make a new plan. It starts as `DRAFT`, so nobody can buy it while we are still deciding the price. |
| 2 | `PATCH /platform/plans/{code}/versions/{version}` | Fix the details of a plan that is still a draft — name, price, limits. Refused once the plan is published, because schools have already bought it. |
| 3 | `PUT /platform/plans/{code}/versions/{version}/features` | Set the whole feature list of a draft plan in one go. Replacing the list is safer than editing one feature at a time, because a half-edited feature list is a plan nobody can price. |
| 4 | `POST /platform/plans/{code}/versions/{version}/publish` | Turn a draft into a real plan schools can buy. From here the plan can never be edited again. |
| 5 | `POST /platform/plans/{code}/versions/{version}/new-version` | Copy a published plan into a new draft version, so we can change the price. The old version stays exactly as it was for the schools already on it. |
| 6 | `POST /platform/plans/{code}/versions/{version}/retire` | Stop selling a plan. Schools already on it keep it and keep working; new schools just cannot pick it. |
| 7 | `PATCH /platform/plans/{code}/versions/{version}/availability` | Say whether a plan shows on the public list or is only offered privately in a quote. |

## 2. The plan catalogue — reads

| # | Method and endpoint | What this API is for |
|---|---|---|
| 8 | `GET /platform/plans` | The operator's list of every plan, filtered by status or code. This is the screen somebody opens to see what we sell. |
| 9 | `GET /platform/plans/{code}/versions` | Every version of one plan, newest first. Shows how the price changed over time and which version each school is on. |
| 10 | `GET /platform/plans/{code}/versions/{version}` | One plan version in full, with all its features. |
| 11 | `GET /schools/current/plans` | The plans **this school** is allowed to move to — published, still on sale, and public. The school's own upgrade screen reads this. |
| 12 | `GET /schools/current/plans/{code}/versions/{version}/comparison` | What would change if this school moved to that plan: the price difference, and any limit that would drop below what the school is already using. Stops a school upgrading into a plan that immediately blocks it. |

## 3. The subscription lifecycle — writes (platform)

The allowed moves are already written in [`models/plans/README.md`](../../models/plans/README.md)
under "Status workflow". **Every one of these endpoints also writes a `SubscriptionHistory` row.**
That is not optional — the history is how anybody later answers "why is this school suspended".

| # | Method and endpoint | What this API is for |
|---|---|---|
| 13 | `POST /platform/schools/{id}/subscriptions` | Give a school its first subscription. This is what makes a school a paying customer, and it is the missing piece the core module already complains about — `activateSchool` currently lets a school go live with no subscription at all. |
| 14 | `POST /platform/schools/{id}/subscriptions/{no}/activate` | Move a trial to a paying subscription once the school has agreed to buy. |
| 15 | `POST /platform/schools/{id}/subscriptions/{no}/extend-trial` | Push the trial end date out. A sales decision, so only an operator can do it. |
| 16 | `POST /platform/schools/{id}/subscriptions/{no}/change-plan` | Move the school onto a different plan or a newer version, and say when the change starts and what happens to the money already paid. |
| 17 | `POST /platform/schools/{id}/subscriptions/{no}/renew` | Start the next billing period. Normally the nightly job calls this; an operator can call it by hand when something went wrong. |
| 18 | `POST /platform/schools/{id}/subscriptions/{no}/mark-past-due` | Mark that the bill was not paid on time. The school keeps working — this is the warning stage before anything is switched off. |
| 19 | `POST /platform/schools/{id}/subscriptions/{no}/suspend` | Stop the school using the product because the bill is still unpaid. Separate from #18 so nobody is cut off the day after a due date. |
| 20 | `POST /platform/schools/{id}/subscriptions/{no}/resume` | Switch the school back on after it pays. |
| 21 | `POST /platform/schools/{id}/subscriptions/{no}/cancel` | End the subscription with a reason. The school usually keeps working until the period it already paid for runs out. |
| 22 | `POST /platform/schools/{id}/subscriptions/{no}/expire` | Close a subscription whose last paid period has now ended. Normally the nightly job does this. |
| 23 | `PATCH /platform/schools/{id}/subscriptions/{no}/auto-renew` | Turn automatic renewal on or off. |
| 24 | `PATCH /platform/schools/{id}/subscriptions/{no}/overrides` | Give one school a bigger student or user limit than its plan normally allows, because that is what was negotiated. |
| 25 | `PATCH /platform/schools/{id}/subscriptions/{no}/price` | Change the agreed price for this one school without changing the plan everybody else is on. |
| 26 | `PATCH /platform/schools/{id}/subscriptions/{no}/billing-customer` | Save the payment provider's customer id against the school, so future charges can be raised against it. |

## 4. The subscription — reads (platform)

| # | Method and endpoint | What this API is for |
|---|---|---|
| 27 | `GET /platform/schools/{id}/subscription` | What this school is on right now: plan, price, status, when the period ends. |
| 28 | `GET /platform/schools/{id}/subscriptions` | Every subscription this school has ever had, including old cancelled ones. |
| 29 | `GET /platform/schools/{id}/subscriptions/{no}/history` | The full trail of what changed, when, who did it and why. The answer to "why did this school get suspended". |
| 30 | `GET /platform/subscriptions` | Every school's subscription in one list, filtered by status. The operator's main screen. |
| 31 | `GET /platform/subscriptions/renewals-due` | Which subscriptions renew in the next N days. Lets somebody see a renewal coming before it fails. |
| 32 | `GET /platform/subscriptions/at-risk` | Everything past due, suspended, or ending soon with auto-renew off. The list somebody works through on a Monday morning. |

## 5. The subscription — the school's own view

| # | Method and endpoint | What this API is for |
|---|---|---|
| 33 | `GET /schools/current/subscription` | What plan am I on, what does it cost, when does it renew. The school's billing screen. |
| 34 | `GET /schools/current/subscription/entitlements` | **The one the rest of the product needs.** Answers "is this school allowed to use this feature, and how much of it is left". Every module that gates a feature must ask this instead of reading the plan itself, because the moment two places work out entitlements they disagree. |
| 35 | `GET /schools/current/subscription/usage` | How much of each limit the school has used — students, users, whatever a feature counts. Shown next to the limits so a school can see itself getting close. |
| 36 | `GET /schools/current/subscription/history` | The school's own view of its plan changes. Shows what happened, but not the operator's internal notes. |
| 37 | `PATCH /schools/current/subscription/auto-renew` | Lets a school turn off automatic renewal itself, rather than having to email us. |
| 38 | `POST /schools/current/subscription/cancel-request` | The school asks to cancel. It **requests** — it does not cancel. Cancelling is #21, and it stays with the operator so somebody talks to the school first. |

## 6. Invoices — writes (platform)

Money rules from [`models/plans/billing/README.md`](../../models/plans/billing/README.md):
`totalAmount = subTotal + taxAmount` and `outstandingAmount = totalAmount - paidAmount`. **Every
endpoint below keeps both true or fails.** An issued invoice is never deleted or quietly changed.

| # | Method and endpoint | What this API is for |
|---|---|---|
| 39 | `POST /platform/schools/{id}/subscription/invoices` | Raise a bill for one billing period. Created as a draft so the amounts can be checked before the school sees it. |
| 40 | `PATCH /platform/schools/{id}/subscription/invoices/{no}` | Correct a draft invoice before it is sent. Refused once it is issued. |
| 41 | `POST /platform/schools/{id}/subscription/invoices/{no}/issue` | Send the bill to the school. After this the amounts are fixed, and the only way to undo it is #42. |
| 42 | `POST /platform/schools/{id}/subscription/invoices/{no}/void` | Cancel a bill that should not have been sent, with a reason. The invoice stays in the records — it is marked void, never deleted, because a missing invoice number is a hole somebody has to explain. |
| 43 | `PATCH /platform/schools/{id}/subscription/invoices/{no}/due-date` | Give a school more time to pay. |
| 44 | `POST /platform/schools/{id}/subscription/invoices/{no}/record-payment` | Write down money that came in outside the gateway — a bank transfer, a cheque, cash. Without this, any school not paying online can never be marked paid. |
| 45 | `POST /platform/schools/{id}/subscription/invoices/{no}/remind` | Send the school a reminder that the bill is unpaid. |
| 46 | `POST /platform/schools/{id}/subscription/invoices/{no}/write-off` | Accept that a bill will never be paid and close it, with a reason. Keeps the outstanding list honest instead of full of debts nobody is chasing. |

## 7. Invoices — the school's own view

| # | Method and endpoint | What this API is for |
|---|---|---|
| 47 | `GET /schools/current/subscription/invoices` | Every bill we have sent this school, newest first. Drafts are hidden — the school should not see a bill we have not sent. |
| 48 | `GET /schools/current/subscription/invoices/{no}` | One bill in full, with what is still owed on it. |
| 49 | `GET /schools/current/subscription/invoices/{no}/pdf` | The bill as a file the school can download, keep and give to its accountant. |
| 50 | `GET /schools/current/subscription/outstanding` | One number: how much this school owes right now. What a banner at the top of the screen shows. |

## 8. Invoices — reads (platform)

| # | Method and endpoint | What this API is for |
|---|---|---|
| 51 | `GET /platform/schools/{id}/subscription/invoices` | Every invoice for one school, drafts included. |
| 52 | `GET /platform/invoices` | Invoices across every school, filtered by status and due date. |
| 53 | `GET /platform/invoices/overdue` | Everything unpaid and past its due date. The collections list. |
| 54 | `GET /platform/invoices/{no}` | One invoice in full, with its payments and every attempt at paying it. |

## 9. Paying — the school

| # | Method and endpoint | What this API is for |
|---|---|---|
| 55 | `POST /schools/current/subscription/invoices/{no}/pay` | Start paying a bill. Creates the attempt record and hands back whatever the gateway needs to show the school a payment page. |
| 56 | `GET /schools/current/subscription/payments/{no}` | Check whether a payment went through. The page the school lands back on after paying asks this. |
| 57 | `GET /schools/current/subscription/payments` | Every payment this school has made. Its receipt list. |
| 58 | `GET /schools/current/subscription/payments/{no}/receipt` | A receipt for one payment, as a file. |
| 59 | `POST /schools/current/subscription/payment-method` | Save a card or set up a UPI mandate so renewals can charge automatically instead of somebody paying by hand every month. |
| 60 | `DELETE /schools/current/subscription/payment-method` | Remove the saved payment method. Auto-renew has to be dealt with at the same time, or the next renewal fails silently. |

## 10. Payments — platform

| # | Method and endpoint | What this API is for |
|---|---|---|
| 61 | `GET /platform/schools/{id}/subscription/payments` | Every payment from one school. |
| 62 | `GET /platform/invoices/{no}/attempts` | Every try at paying one bill, failures included, with what the gateway said went wrong. This is the screen for "the school says they paid and it did not work". |
| 63 | `POST /platform/schools/{id}/subscription/payments/{no}/refund` | Give money back, in full or in part. |
| 64 | `POST /platform/schools/{id}/subscription/payments/{no}/reconcile` | Match a payment to the money the bank actually settled, and mark it settled. |
| 65 | `POST /platform/schools/{id}/subscription/payments/{no}/retry` | Try a failed automatic charge again. |

## 11. The payment provider talking to us

| # | Method and endpoint | What this API is for |
|---|---|---|
| 66 | `POST /billing/webhooks/{provider}` | Where the payment provider tells us a payment succeeded or failed. It checks the signature, saves the event exactly as it arrived, and only then applies it. **The same event arriving twice must change nothing the second time**, which is what the unique `provider + providerEventId` index is for. |
| 67 | `GET /platform/billing/webhooks` | Every event the provider sent us, and whether we managed to process it. |
| 68 | `GET /platform/billing/webhooks/{id}` | One event in full, including what went wrong if it failed. |
| 69 | `POST /platform/billing/webhooks/{id}/replay` | Process a failed event again after the bug is fixed. The raw payload was saved for exactly this. |

## 12. The jobs that run on their own

These do the work nobody clicks a button for. They are endpoints as well as scheduled jobs so
they can be run by hand when something needs fixing, and so they can be tested.

| # | Method and endpoint | What this API is for |
|---|---|---|
| 70 | `POST /platform/billing/jobs/renew-due` | Find every subscription whose period ends today, raise the next invoice, and charge the saved payment method. The job that keeps the money coming in. |
| 71 | `POST /platform/billing/jobs/age-overdue` | Find bills that are past their due date and move those subscriptions to past due, then to suspended once the grace period runs out. The job that stops schools using the product for free forever. |

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
