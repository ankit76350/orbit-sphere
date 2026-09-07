import { endOfDay, startOfDay, toDateInput } from '../../../lib/dates.js'

/**
 * What a PATCH to `/platform/schools/{id}/subscriptions/{no}` should carry.
 *
 * Its own module because it is the whole contract of that endpoint expressed as a function, and a
 * function is testable in a way a form is not. The screen renders boxes; this decides what the
 * boxes mean.
 *
 * THE ENDPOINT READS AN ABSENT FIELD AS "LEAVE IT ALONE", so only the difference is sent. A form
 * that posted every box would send eight fields to change one, and the history row it writes
 * would say eight fields were edited — which is worse than useless when somebody is later asking
 * which field moved.
 *
 * WHAT IS NOT HERE: the price, the currency, the billing customer reference and the plan. Those
 * are #25, #26 and #16 — money and entitlement, each with its own endpoint, deliberately not
 * reachable from an edit that moves dates around. Nor the cancellation: `cancelledAt` and
 * `cancellationReason` no longer exist on the model, and `reasonForChanges` is written by the API
 * from `reason` rather than set as a field.
 */

/** "" for a field with no value, so every box holds a string and none of them is undefined. */
export const asText = (value) => (value == null ? '' : String(value))

/** The boxes as they should stand for a subscription, and what the diff is taken against. */
export const storedForm = (subscription) => ({
  status: subscription.status,
  billingCycle: subscription.billingCycle,
  // The three date fields are held as yyyy-MM-dd, because that is what a date input — and the
  // calendar it opens — works in. They go back out as instants; see patchBody.
  currentPeriodStart: toDateInput(subscription.currentPeriodStart),
  currentPeriodEnd: toDateInput(subscription.currentPeriodEnd),
  autoRenew: Boolean(subscription.autoRenew),
  maxStudentsOverride: asText(subscription.maxStudentsOverride),
  maxUsersOverride: asText(subscription.maxUsersOverride),
})

/**
 * The body: what differs from what is stored, and nothing else.
 *
 * The two blocks are all-or-nothing on purpose — the endpoint reads a sent block as "replace
 * both", so if either half moved both halves go, and an emptied box becomes a null inside the
 * block rather than a missing key. That is how an override is removed.
 */
export function patchBody(form, stored) {
  if (!form || !stored) return {}
  const out = {}

  if (form.status !== stored.status) out.status = form.status
  if (form.billingCycle !== stored.billingCycle) out.billingCycle = form.billingCycle
  if (form.autoRenew !== stored.autoRenew) out.autoRenew = form.autoRenew

  // A picked day becomes an instant. The period START is the beginning of that day and the END
  // is the last second of it: "runs to 31 March" means the 31st is included, and midnight on the
  // 31st would end the period before that day began.
  //
  // These two are @NotNull on the model, so an emptied box is not a way to clear them — it is a
  // box somebody has not finished filling in, and sending null would be a 400.
  if (form.currentPeriodStart && form.currentPeriodStart !== stored.currentPeriodStart) {
    out.currentPeriodStart = startOfDay(form.currentPeriodStart)
  }
  if (form.currentPeriodEnd && form.currentPeriodEnd !== stored.currentPeriodEnd) {
    out.currentPeriodEnd = endOfDay(form.currentPeriodEnd)
  }

  if (form.maxStudentsOverride !== stored.maxStudentsOverride
      || form.maxUsersOverride !== stored.maxUsersOverride) {
    out.limitOverrides = {
      maxStudentsOverride: form.maxStudentsOverride.trim()
        ? Number(form.maxStudentsOverride) : null,
      maxUsersOverride: form.maxUsersOverride.trim()
        ? Number(form.maxUsersOverride) : null,
    }
  }

  // Never part of the diff, though it IS stored: the API writes it to reasonForChanges on the
  // subscription and onto the history row, but a reason is not itself a change. Sent alone it
  // would be a 400 NO_CHANGES_REQUESTED, so it goes only when something else is going with it.
  //
  // Left out when blank on purpose: the API overwrites reasonForChanges on every edit, and an
  // absent reason is how "this change has no recorded reason" is said.
  if (form.reason?.trim() && Object.keys(out).length > 0) out.reason = form.reason.trim()

  return out
}

/** The fields this edit actually changes — the reason explains an edit rather than being one. */
export const changedFields = (body) => Object.keys(body).filter((key) => key !== 'reason')
