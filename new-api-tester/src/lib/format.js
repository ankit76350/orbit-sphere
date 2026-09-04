const EUR = new Intl.NumberFormat('en-IE', {
  style: 'currency', currency: 'EUR', minimumFractionDigits: 2, maximumFractionDigits: 2,
})

export const money = (n) => EUR.format(n)

/** Prices span 6 orders of magnitude here, so precision has to adapt. */
export const price = (n) =>
  '€ ' + n.toLocaleString('en-IE', {
    minimumFractionDigits: n < 1 ? 4 : 2,
    maximumFractionDigits: n < 1 ? 4 : 2,
  })

export const plain = (n, dp = 2) =>
  n.toLocaleString('en-IE', { minimumFractionDigits: dp, maximumFractionDigits: dp })

export const holdings = (n, symbol) => {
  const dp = n >= 1000 ? 2 : n >= 1 ? 4 : 4
  return `${plain(n, dp)} ${symbol}`
}

export const pct = (n, dp = 2) => `${n.toFixed(dp)}%`

export const signedPct = (n) => `${n > 0 ? '+' : n < 0 ? '−' : ''}${Math.abs(n).toFixed(2)}%`

export const axisMoney = (n) => (n === 0 ? '0€' : `${Math.round(n / 1000)}k`)

export const shortDate = (d) =>
  `${String(d.getUTCDate()).padStart(2, '0')}/${String(d.getUTCMonth() + 1).padStart(2, '0')}`

export const longDate = (d) =>
  d.toLocaleDateString('en-IE', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })

/** Split "12,433.35" so the cents can be de-emphasised in the hero number. */
export function splitCents(n) {
  const [whole, cents] = plain(n).split('.')
  return { whole, cents }
}
