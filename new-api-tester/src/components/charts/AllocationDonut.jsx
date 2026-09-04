import { useState } from 'react'
import { pct, splitCents } from '../../lib/format.js'

const SIZE = 240
const STROKE = 22
const GAP_DEG = 1.6 // ~2px of surface between adjacent fills at this radius

const polar = (cx, cy, r, deg) => {
  const rad = ((deg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function arcPath(cx, cy, r, startDeg, endDeg) {
  const start = polar(cx, cy, r, endDeg)
  const end = polar(cx, cy, r, startDeg)
  const large = endDeg - startDeg <= 180 ? 0 : 1
  return `M ${start.x} ${start.y} A ${r} ${r} 0 ${large} 0 ${end.x} ${end.y}`
}

/** Cumulative arc angles, inset by half the gap on each side. */
function layoutArcs(segments, sum) {
  const arcs = []
  let cursor = 0
  for (const seg of segments) {
    const sweep = (seg.value / sum) * 360
    const inset = Math.min(GAP_DEG / 2, sweep / 4)
    arcs.push({ ...seg, start: cursor + inset, end: cursor + sweep - inset, sweep })
    cursor += sweep
  }
  return arcs
}

/**
 * Part-to-whole ring. `segments` arrive pre-ordered so that the two hues
 * hardest to separate under protan/deutan vision are never neighbours.
 */
export default function AllocationDonut({ segments, total, assetCount, highlight }) {
  const [hover, setHover] = useState(null)
  const cx = SIZE / 2
  const cy = SIZE / 2
  const r = (SIZE - STROKE) / 2
  const sum = segments.reduce((s, x) => s + x.value, 0) || 1

  const arcs = layoutArcs(segments, sum)

  const { whole, cents } = splitCents(total)
  const active = hover !== null ? arcs[hover] : null

  return (
    <div className="donut">
      <svg viewBox={`0 0 ${SIZE} ${SIZE}`} width={SIZE} height={SIZE} role="img"
           aria-label={`Portfolio allocation across ${assetCount} assets. ${segments
             .map((s) => `${s.label} ${pct(s.value, 1)}`)
             .join(', ')}.`}>
        {arcs.map((a, i) => (
          <path
            key={a.label}
            d={arcPath(cx, cy, r, a.start, a.end)}
            stroke={a.color}
            strokeWidth={hover === i || highlight === a.key ? STROKE + 5 : STROKE}
            strokeLinecap="butt"
            fill="none"
            opacity={hover === null || hover === i ? 1 : 0.45}
            onMouseEnter={() => setHover(i)}
            onMouseLeave={() => setHover(null)}
            className="donut-arc"
          />
        ))}
      </svg>

      <div className="donut-center">
        {active ? (
          <>
            <span className="donut-center-label">{active.label}</span>
            <span className="donut-center-value">{pct(active.value, 1)}</span>
            <span className="donut-center-sub">of portfolio</span>
          </>
        ) : (
          <>
            <span className="donut-center-label">My wallet</span>
            <span className="donut-center-value">
              € {whole}
              <span className="donut-cents">.{cents}</span>
            </span>
            <span className="donut-center-sub">{assetCount} Assets</span>
          </>
        )}
      </div>
    </div>
  )
}
