/**
 * Facts about a plan that both the catalogue and one version's page need.
 *
 * In its own module for two reasons. Fast refresh only works when a component file exports
 * nothing but components, and these were exported from `Catalogue.jsx`. And both screens had
 * their own copy of the status tones and the billing cycles — two copies of a list that has to
 * match the backend's enum is one more chance to be wrong about it.
 */

/** Green is sellable, amber is waiting on somebody, plain is off the menu. */
export const STATUS_TONE = { DRAFT: 'warn', ACTIVE: 'good', RETIRED: undefined }

/** BillingCycle, mirrored. A value the enum does not have is a 400 listing the accepted ones. */
export const BILLING_CYCLES = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'CUSTOM']

/**
 * Why this plan cannot be sold today.
 *
 * The API returns `sellable` as one boolean off three separate facts — published, on the public
 * list, inside its selling window — so when it is false the useful thing is WHICH of them is
 * missing, not the false itself.
 *
 * One definition, used by both screens: two screens disagreeing about why a plan is unsellable
 * would be worse than neither of them saying.
 */
export function whyNotSellable(plan) {
  if (plan.sellable) return null
  if (plan.status === 'DRAFT') return 'Not published yet'
  if (plan.status === 'RETIRED') return 'Retired'
  const now = Date.now()
  if (plan.effectiveFrom && new Date(plan.effectiveFrom).getTime() > now) return 'Not on sale yet'
  if (plan.effectiveUntil && new Date(plan.effectiveUntil).getTime() <= now) {
    return 'Its selling window closed'
  }
  if (plan.publiclyAvailable === false) return 'Off the public list — quote only'
  return 'Not sellable'
}

/**
 * Whether this endpoint would actually sell the plan, and what to call it if not.
 *
 * `sellable` on the response is NOT the answer here, and the difference matters. It requires
 * `publiclyAvailable`, but creating a subscription deliberately does not check that field —
 * PlatformSubscriptionService says so in as many words: "a plan that is published but off the
 * public list is exactly a private quote, and this endpoint is how a private quote gets sold."
 *
 * So a private plan reads `sellable: false` and sells perfectly well. Labelling it "not sellable"
 * in the one dialog whose job is selling would talk somebody out of a plan the API would have
 * accepted.
 *
 * What genuinely stops a sale is the status and the selling window, which is exactly what #13
 * checks and refuses with `409 PLAN_NOT_SELLABLE`.
 */
export function sellability(plan) {
  if (plan.status === 'DRAFT') {
    return { canSell: false, label: 'draft — publish it first' }
  }
  if (plan.status === 'RETIRED') {
    return { canSell: false, label: 'retired — cannot be sold again' }
  }

  const now = Date.now()
  if (plan.effectiveFrom && now < Date.parse(plan.effectiveFrom)) {
    return { canSell: false, label: `not on sale until ${plan.effectiveFrom.slice(0, 10)}` }
  }
  if (plan.effectiveUntil && now > Date.parse(plan.effectiveUntil)) {
    return { canSell: false, label: `stopped selling ${plan.effectiveUntil.slice(0, 10)}` }
  }

  // Published, in its window, just not advertised. Sellable — privately.
  if (!plan.publiclyAvailable) {
    return { canSell: true, label: 'quote only — not on the public list', private: true }
  }

  return { canSell: true, label: null }
}
