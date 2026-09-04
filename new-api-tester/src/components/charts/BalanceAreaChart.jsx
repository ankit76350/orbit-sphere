import { useId, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { monotonePath, niceTicks } from '../../lib/curve.js'
import { axisMoney, longDate, money, shortDate, signedPct } from '../../lib/format.js'

const PAD = { top: 16, right: 16, bottom: 28, left: 40 }

export default function BalanceAreaChart({ data, height = 320 }) {
  const gradientId = useId()
  const wrapRef = useRef(null)
  const [box, setBox] = useState({ w: 720 })
  const [hover, setHover] = useState(null)

  // Measure the container so the SVG is fluid and follows window resizes.
  useLayoutEffect(() => {
    const node = wrapRef.current
    if (!node) return
    const apply = () => setBox({ w: node.clientWidth })
    apply()
    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', apply)
      return () => window.removeEventListener('resize', apply)
    }
    const ro = new ResizeObserver(apply)
    ro.observe(node)
    return () => ro.disconnect()
  }, [])

  const geo = useMemo(() => {
    const w = Math.max(box.w, 320)
    const h = height
    const innerW = w - PAD.left - PAD.right
    const innerH = h - PAD.top - PAD.bottom
    const max = Math.max(...data.map((d) => d.value), 1)
    const { top, ticks } = niceTicks(max)
    const x = (i) => PAD.left + (data.length === 1 ? innerW / 2 : (i / (data.length - 1)) * innerW)
    const y = (v) => PAD.top + innerH - (v / top) * innerH
    const pts = data.map((d, i) => ({ x: x(i), y: y(d.value) }))
    const line = monotonePath(pts)
    const area = `${line} L ${pts[pts.length - 1].x} ${PAD.top + innerH} L ${pts[0].x} ${PAD.top + innerH} Z`
    return { w, h, innerW, innerH, top, ticks, x, y, pts, line, area, baseY: PAD.top + innerH }
  }, [box.w, height, data])

  // Default the crosshair to the latest point, as the reference does.
  const activeIndex = hover ?? data.length - 1
  const active = data[activeIndex]
  const prev = data[activeIndex - 1]
  const delta = prev && prev.value !== 0 ? ((active.value - prev.value) / prev.value) * 100 : 0

  const indexFromEvent = (event) => {
    const rect = event.currentTarget.getBoundingClientRect()
    const px = ((event.clientX - rect.left) / rect.width) * geo.w
    const ratio = (px - PAD.left) / geo.innerW
    return Math.max(0, Math.min(data.length - 1, Math.round(ratio * (data.length - 1))))
  }

  const onKeyDown = (event) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
    event.preventDefault()
    const step = event.key === 'ArrowLeft' ? -1 : 1
    setHover(Math.max(0, Math.min(data.length - 1, activeIndex + step)))
  }

  // Label ~5 dates evenly. The final date is only labelled when it is far
  // enough from the previous one to not collide with it.
  const labelEvery = Math.max(1, Math.ceil(data.length / 5))
  const lastIndex = data.length - 1
  const lastLabelled = Math.floor(lastIndex / labelEvery) * labelEvery
  const showLastLabel = lastIndex - lastLabelled >= Math.ceil(labelEvery * 0.6)
  const cx = geo.x(activeIndex)
  const cy = geo.y(active.value)

  // Near an edge the tooltip anchors to its own side instead of centring,
  // so it is never clipped by the card.
  const tipAlign = cx > geo.w - 96 ? 'end' : cx < 96 ? 'start' : 'center'

  return (
    <div className="chart" ref={wrapRef}>
      <svg
        viewBox={`0 0 ${geo.w} ${geo.h}`}
        width="100%"
        height={geo.h}
        role="img"
        aria-label={`Wallet balance over time. Latest ${money(data[data.length - 1].value)}. Use the arrow keys to inspect points.`}
        tabIndex={0}
        onKeyDown={onKeyDown}
        onMouseMove={(e) => setHover(indexFromEvent(e))}
        onMouseLeave={() => setHover(null)}
        onTouchStart={(e) => e.touches[0] && setHover(indexFromEvent(e.touches[0]))}
        className="chart-svg"
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--chart-fill-top)" />
            <stop offset="100%" stopColor="var(--chart-fill-bottom)" />
          </linearGradient>
        </defs>

        {/* recessive grid — horizontal only */}
        {geo.ticks.map((t) => (
          <g key={t}>
            <line
              x1={PAD.left} x2={geo.w - PAD.right}
              y1={geo.y(t)} y2={geo.y(t)}
              stroke="var(--chart-grid)" strokeWidth="1"
            />
            <text x={PAD.left - 10} y={geo.y(t) + 4} textAnchor="end" className="chart-tick">
              {axisMoney(t)}
            </text>
          </g>
        ))}

        <path d={geo.area} fill={`url(#${gradientId})`} />
        <path d={geo.line} fill="none" stroke="var(--chart-line)" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round" />

        {data.map((d, i) =>
          i % labelEvery === 0 || (i === lastIndex && showLastLabel) ? (
            <text key={i} x={geo.x(i)} y={geo.h - 8} textAnchor="middle" className="chart-tick">
              {shortDate(d.date)}
            </text>
          ) : null,
        )}

        {/* crosshair + marker with a 2px surface ring */}
        <line x1={cx} x2={cx} y1={PAD.top} y2={geo.baseY}
              stroke="var(--chart-line)" strokeWidth="1.5" strokeDasharray="4 4" opacity="0.75" />
        <circle cx={cx} cy={cy} r="6" fill="var(--surface)" stroke="var(--chart-line)" strokeWidth="2" />
      </svg>

      <div
        className="chart-tooltip"
        data-align={tipAlign}
        data-below={cy < 74 || undefined}
        style={{
          left: `${(cx / geo.w) * 100}%`,
          top: `${(cy / geo.h) * 100}%`,
        }}
        role="status"
      >
        <span className="chart-tooltip-delta" data-dir={delta >= 0 ? 'up' : 'down'}>
          {signedPct(delta)}
        </span>
        <span className="chart-tooltip-value">{money(active.value)}</span>
        <span className="chart-tooltip-date">{longDate(active.date)}</span>
      </div>
    </div>
  )
}
