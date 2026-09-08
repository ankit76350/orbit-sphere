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
 * ONE FIELD BREAKS THAT RULE, because the endpoint makes it: `currentPeriodEnd` is sent unchanged
 * when the cadence is moving to CUSTOM, which is the one transition that requires it. See the
 * note beside it.
 *
 * WHAT IS NOT SENT ON A FIXED CADENCE is that same `currentPeriodEnd`: MONTHLY, QUARTERLY,
 * HALF_YEARLY and YEARLY have a length, so the API derives the end from the start and sending one
 * would override a date the cadence already decides. The form disables that box for them.
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
 * Every field goes on its own, including the two overrides — so raising the student ceiling sends
 * one field and leaves the user ceiling untouched. An emptied override box is the exception worth
 * knowing: it becomes a 0, which is how this endpoint says "remove the override and use the
 * plan's own limit".
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
  // ONE EXCEPTION TO "ONLY THE DIFFERENCE", and the API is what forces it: moving to a CUSTOM
  // cadence requires currentPeriodEnd on the same request, because CUSTOM has no length to derive
  // from and the date on record belongs to the cadence being left. So on that transition the
  // box's value goes even when it has not been edited — otherwise somebody happy with the date
  // already showing would get a 400 for changing nothing.
  const movingToCustom = form.billingCycle === 'CUSTOM'
    && form.billingCycle !== stored.billingCycle

  if (form.currentPeriodEnd
    && (movingToCustom || form.currentPeriodEnd !== stored.currentPeriodEnd)) {
    out.currentPeriodEnd = endOfDay(form.currentPeriodEnd)
  }

  // The two overrides are flat, and each goes on its own — leaving one box alone leaves that
  // override alone. Emptying a box is how somebody says "take this override away", and the API
  // spells that 0: an omitted field and an explicit null are the same value to Jackson, so zero
  // is what carries the removal.
  for (const field of ['maxStudentsOverride', 'maxUsersOverride']) {
    if (form[field] === stored[field]) continue
    out[field] = form[field].trim() ? Number(form[field]) : 0
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
