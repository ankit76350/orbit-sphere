# SaaS plan and subscription model mapping

This package controls which SaaS plan one school uses. It is separate from the
school's operational finance and student-fee modules.

## Relationship overview

```text
PlanDefinition (platform level)
  |
  | PlanDefinition.id + planVersion
  v
SchoolSubscription (school owned)
  |
  +--> SubscriptionHistory[]
  |
  +--> SubscriptionInvoice[]
          |
          +--> SubscriptionPayment[]
          +--> PaymentAttempt[]
          +--> BillingWebhookEvent[] through related entity links
```

`PlanDefinition` extends `AuditedDocument` because it is shared platform
configuration. Every other top-level document extends `SchoolBase`.

## PlanDefinition — `plan_definitions`

Represents one immutable version of a public or private SaaS plan.

| Field | Meaning and mapping |
|---|---|
| `planCode` | Stable plan family key such as `PREMIUM`. |
| `planVersion` | Version within that plan family. |
| `name` | Display name. |
| `description` | Plan description. |
| `status` | Draft, active, or retired publication state. |
| `billingCycle` | Default billing recurrence offered by the plan. |
| `listPrice` | Default price stored as MongoDB Decimal128. |
| `currencyCode` | ISO 4217 currency code such as `INR`. |
| `maxStudents` | Default active-student capacity. |
| `maxUsers` | Default active-user capacity. |
| `effectiveFrom` | First date/time when this version may be sold. |
| `effectiveUntil` | Last date/time when this version may be sold. |
| `publiclyAvailable` | Whether schools can select the plan without a private quotation. |
| `features` | Embedded PlanFeature entitlements. Set as a whole list by the API, never one at a time — a plan is priced as a set. |

`effectiveFrom` and `effectiveUntil` belong to the plan version. They do not
represent a school's current subscription period.

Published versions should be treated as immutable. A material price, capacity,
or feature change creates:

```text
planCode = PREMIUM
planVersion = 2
```

Existing subscriptions continue to reference their contracted version.

## PlanFeature — embedded

Represents one feature entitlement inside a PlanDefinition.

Read one row as a sentence: *this plan includes X, up to N of Y, and does Z when the school goes
past it.*

| Field | Meaning |
|---|---|
| `featureCode` | Which capability. One of [`FeatureCode`](enums/FeatureCode.java) — a fixed list of 24, not free text. |
| `enabled` | Whether the plan includes it. `false` deliberately lists it as excluded, so a comparison table can show a cross rather than omitting the row. |
| `usageLimit` | How much is included. Null means no numeric limit. |
| `usageMetric` | What that number counts, from [`UsageMetric`](enums/UsageMetric.java). Copied from the feature, never sent by a caller. Null whenever `usageLimit` is null. |
| `overagePolicy` | Block, warn, allow, or charge after reaching the limit. |

**`featureCode` was a `String` until 2026-09-03.** That accepted `STUDNET_MANAGEMENT` without
complaint: the plan looked right on every screen while the entitlement service, asking for
`STUDENT_MANAGEMENT`, found nothing and locked the school out of what they had paid for. A
feature code points at behaviour in this codebase rather than at anything a user invents, so the
set is closed and belongs in an enum. Add constants, never rename one — the name is stored in
every plan already sold.

**Each feature declares the metric it is measured in**, so `usageMetric` is copied in rather than
chosen: `TRANSPORT` counts `VEHICLES`, `STUDENT_MANAGEMENT` counts `ACTIVE_STUDENTS`. "Student
management limited to 2000 gigabytes" is not refused by a rule — it cannot be written down. A
feature with no metric has nothing to count, so a `usageLimit` on it is refused: `ATTENDANCE` is
included or it is not.

**The metric is stored, not derived on read.** A published plan version is immutable, so if a
feature's metric were ever changed, a plan sold last year must keep meaning what it meant when it
was sold.

Maximum users and students belong to PlanDefinition rather than PlanFeature
because they are core subscription capacities.

## SchoolSubscription — `school_subscriptions`

Stores contracted plan terms and the current commercial state for one school.

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `subscriptionNo` | School-scoped business number. |
| `planDefinitionDocsId` | Link to `PlanDefinition.id`. |
| `planVersion` | Contracted immutable plan version. |
| `status` | Trial, active, past due, suspended, cancelled, or expired. |
| `billingCycle` | Contracted recurrence, which may differ from the public default. |
| `currentPeriodStart` | Start of the current service/billing period. |
| `currentPeriodEnd` | End of the current service/billing period; also the trial end when status is `TRIAL`. |
| `autoRenew` | Whether renewal should be attempted. |
| `contractedPrice` | Negotiated period price stored as Decimal128. |
| `currencyCode` | Currency applying to the contracted price. |
| `maxStudentsOverride` | Optional negotiated capacity replacing PlanDefinition.maxStudents. |
| `maxUsersOverride` | Optional negotiated capacity replacing PlanDefinition.maxUsers. |
| `billingCustomerReference` | External payment-provider customer id. |
| `cancelledAt` | Cancellation time. |
| `cancellationReason` | Human-readable cancellation reason. |
| `current` | Marks the one subscription currently selected for the school. |

The current subscription is found with:

```text
schoolId = School.id
current = true
```

The partial unique index guarantees at most one current subscription per school.

Effective capacities are:

```text
effectiveMaxStudents =
  maxStudentsOverride != null
    ? maxStudentsOverride
    : PlanDefinition.maxStudents

effectiveMaxUsers =
  maxUsersOverride != null
    ? maxUsersOverride
    : PlanDefinition.maxUsers
```

Plan/subscription fields are intentionally not copied into School.

## SubscriptionHistory — `subscription_history`

Append-only audit history of changes to a SchoolSubscription.

| Field | Meaning and mapping |
|---|---|
| `schoolSubscriptionDocsId` | Link to SchoolSubscription.id. |
| `eventType` | Type of commercial transition. |
| `previousStatus` | Status before the event; null on initial creation. |
| `newStatus` | Status after the event. |
| `previousPlanDefinitionDocsId` | Previous plan id when the plan changes. |
| `newPlanDefinitionDocsId` | New plan id when the plan changes. |
| `source` | Origin such as `ADMIN_PORTAL`, `BILLING_JOB`, or gateway name. |
| `sourceEventId` | Optional external idempotency key. |
| `reason` | Human-readable explanation. |
| `performedByDocsId` | Acting identity/account; null for automated events. |
| `effectiveAt` | Business-effective event time. |

There is no `sequenceNo`. Events are ordered by `effectiveAt` and audit creation
time. A unique provider `source + sourceEventId` prevents duplicated external
events.

History events should not be edited or deleted during normal operations.

## Status workflow

```text
TRIAL -> ACTIVE
ACTIVE -> PAST_DUE -> ACTIVE
PAST_DUE -> SUSPENDED -> ACTIVE
ACTIVE/TRIAL/PAST_DUE -> CANCELLED
ACTIVE/CANCELLED -> EXPIRED at the final period end
```

Allowed transitions, grace periods, renewal rules, and access enforcement belong
to the subscription service.

## Validation responsibility

The models retain only essential required fields: identities, statuses, plan
versions, billing cycles, prices, currencies, capacity defaults, current flags,
and effective event timestamps.

Request DTOs and services validate:

- positive prices, capacities, limits, and versions;
- ISO currency and feature-code formats;
- date ordering and effective-date overlap;
- allowed status transitions;
- current-subscription replacement transactions;
- plan-version immutability;
- feature/metric compatibility;
- school and referenced-document ownership;
- cancellation and trial conditions.

MongoDB indexes and collection validators should be deployed through controlled
database migrations.
