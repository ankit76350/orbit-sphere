/**
 * Monotone cubic Hermite path (Fritsch–Carlson tangents).
 * A plain Catmull-Rom / cardinal spline overshoots at the shelves in this
 * series and would draw balances the data never had — including below zero.
 * Monotone interpolation cannot overshoot, so the curve stays truthful.
 */
export function monotonePath(points) {
  const n = points.length
  if (n === 0) return ''
  if (n === 1) return `M ${points[0].x} ${points[0].y}`
  if (n === 2) return `M ${points[0].x} ${points[0].y} L ${points[1].x} ${points[1].y}`

  const dx = [], dy = [], slope = []
  for (let i = 0; i < n - 1; i++) {
    dx[i] = points[i + 1].x - points[i].x
    dy[i] = points[i + 1].y - points[i].y
    slope[i] = dy[i] / dx[i]
  }

  const m = [slope[0]]
  for (let i = 1; i < n - 1; i++) {
    if (slope[i - 1] * slope[i] <= 0) {
      m[i] = 0 // local extremum — flatten to prevent overshoot
    } else {
      const w1 = 2 * dx[i] + dx[i - 1]
      const w2 = dx[i] + 2 * dx[i - 1]
      m[i] = (w1 + w2) / (w1 / slope[i - 1] + w2 / slope[i])
    }
  }
  m[n - 1] = slope[n - 2]

  let d = `M ${points[0].x} ${points[0].y}`
  for (let i = 0; i < n - 1; i++) {
    const c1x = points[i].x + dx[i] / 3
    const c1y = points[i].y + (m[i] * dx[i]) / 3
    const c2x = points[i + 1].x - dx[i] / 3
    const c2y = points[i + 1].y - (m[i + 1] * dx[i]) / 3
    d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${points[i + 1].x} ${points[i + 1].y}`
  }
  return d
}

/** Round a max up to a clean axis top and return evenly spaced ticks. */
export function niceTicks(max, count = 6) {
  const raw = max / count
  const mag = Math.pow(10, Math.floor(Math.log10(raw)))
  const step = [1, 2, 2.5, 5, 10].map((s) => s * mag).find((s) => s >= raw) ?? 10 * mag
  const top = Math.ceil(max / step) * step
  const ticks = []
  for (let v = 0; v <= top + 1e-9; v += step) ticks.push(v)
  return { top, ticks }
}
