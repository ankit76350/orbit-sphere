/**
 * Wallet balance history. Shaped to the reference: a climb to a ~3k shelf,
 * a spike near 13/05, a retrace, then a step up to a ~5.2k plateau and a
 * pullback at the end. Deterministic — no random jitter between renders.
 */
const SHAPE = [
  0, 40, 120, 260, 470, 760, 1120, 1520, 1900, 2260, 2560, 2790, 2930, 2990,
  3020, 3180, 3560, 3980, 4210, 4120, 3760, 3380, 3160, 3080, 3140, 3260,
  3210, 3110, 3433, 3980, 4520, 4930, 5150, 5210, 5190, 5205, 5170, 4720,
  3980, 3620, 3433.35,
]

const START = new Date(Date.UTC(2024, 4, 1)) // 01 May 2024

export const BALANCE_SERIES = SHAPE.map((value, i) => {
  const date = new Date(START)
  date.setUTCDate(START.getUTCDate() + i)
  return { date, value }
})

export const RANGES = ['1D', '7D', '1M', '3M', '6M', '1Y', 'ALL']

/** Slice the series for a range so the control actually changes the chart. */
export function seriesForRange(range) {
  const n = BALANCE_SERIES.length
  const spans = { '1D': 2, '7D': 8, '1M': 31, '3M': n, '6M': n, '1Y': n, ALL: n }
  const span = Math.min(spans[range] ?? n, n)
  return BALANCE_SERIES.slice(n - span)
}
