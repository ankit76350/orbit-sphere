/**
 * A price, in the currency the API said it was in.
 *
 * The currency comes off the plan rather than being assumed, because a plan carries its own
 * `currencyCode` and the platform sells in more than one. An unknown code falls back to printing
 * the number with the code beside it — better than throwing, and better than silently showing a
 * rupee sign on a dollar price.
 */
export function money(amount, currencyCode) {
  if (amount === null || amount === undefined) return null
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode || 'INR',
      maximumFractionDigits: 2,
    }).format(amount)
  } catch {
    return `${amount} ${currencyCode ?? ''}`.trim()
  }
}

/** A count with its unit, so "1 feature" does not read as "1 features". */
export const plural = (n, one, many) => `${n} ${n === 1 ? one : many ?? `${one}s`}`
