/**
 * What a PATCH to `/platform/schools/{id}/subscriptions/{no}` should carry.
 *
 * Its own module because it is the whole contract of that endpoint expressed as a function, and a
 * function is testable in a way a form is not. The screen renders boxes; this decides what the
 * boxes mean.
 *
 * THE ENDPOINT READS AN ABSENT FIELD AS "LEAVE IT ALONE", so only the difference is sent. A form
 * that posted every box would send twelve fields to change one, and the history row it writes
 * would say twelve fields were edited — which is worse than useless when somebody is later asking
 * which field moved.
 */

/** "" for a field with no value, so every box holds a string and none of them is undefined. */
export const asText = (value) => (value == null ? '' : String(value))

/** The boxes as they should stand for a subscription, and what the diff is taken against. */
export const storedForm = (subscription) => ({
  status: subscription.status,
  planKey: `${subscription.planCode}@${subscription.planVersion}`,
  billingCycle: subscription.billingCycle,
  currentPeriodStart: asText(subscription.currentPeriodStart),
  currentPeriodEnd: asText(subscription.currentPeriodEnd),
  autoRenew: Boolean(subscription.autoRenew),
  contractedPrice: asText(subscription.contractedPrice),
  currencyCode: asText(subscription.currencyCode),
  billingCustomerReference: asText(subscription.billingCustomerReference),
  maxStudentsOverride: asText(subscription.maxStudentsOverride),
  maxUsersOverride: asText(subscription.maxUsersOverride),
  cancelledAt: asText(subscription.cancelledAt),
  cancellationReason: asText(subscription.cancellationReason),
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

  for (const field of ['currentPeriodStart', 'currentPeriodEnd']) {
    // These two are @NotNull on the model, so an emptied box is not a way to clear them — it is
    // a box the caller has not finished filling in, and sending "" would be a 400.
    if (form[field].trim() && form[field] !== stored[field]) out[field] = form[field].trim()
  }

  if (form.contractedPrice.trim() && form.contractedPrice !== stored.contractedPrice) {
    out.contractedPrice = Number(form.contractedPrice)
  }
  if (form.currencyCode.trim() && form.currencyCode !== stored.currencyCode) {
    out.currencyCode = form.currencyCode.trim()
  }
  // The one field where "" is meaningful: it is how this endpoint clears a string.
  if (form.billingCustomerReference !== stored.billingCustomerReference) {
    out.billingCustomerReference = form.billingCustomerReference.trim()
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

  if (form.cancelledAt !== stored.cancelledAt
      || form.cancellationReason !== stored.cancellationReason) {
    out.cancellation = {
      cancelledAt: form.cancelledAt.trim() || null,
      cancellationReason: form.cancellationReason.trim() || null,
    }
  }

  if (form.planKey !== stored.planKey) {
    const [planCode, planVersion] = form.planKey.split('@')
    out.plan = { planCode, planVersion: Number(planVersion) }
  }

  // Never part of the diff: a reason changes nothing on the subscription, it only explains the
  // change on the history row. Sent alone it would be a 400 NO_CHANGES_REQUESTED, so it goes
  // only when something else is going with it.
  if (form.reason?.trim() && Object.keys(out).length > 0) out.reason = form.reason.trim()

  return out
}

/** The fields this edit actually changes — the reason explains an edit rather than being one. */
export const changedFields = (body) => Object.keys(body).filter((key) => key !== 'reason')
