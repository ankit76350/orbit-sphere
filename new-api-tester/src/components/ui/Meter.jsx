/**
 * Allocation meter. The value is always shown as text beside it, so the
 * bar is a redundant encoding rather than the only way to read the number.
 */
export default function Meter({ value, max = 100, color, thickness = 6 }) {
  const ratio = Math.max(0, Math.min(1, value / max))
  return (
    <span className="meter" style={{ '--meter-h': `${thickness}px` }} aria-hidden="true">
      <span
        className="meter-fill"
        style={{
          // A non-zero holding always leaves a visible sliver.
          width: ratio > 0 ? `max(3px, ${ratio * 100}%)` : 0,
          background: color,
        }}
      />
    </span>
  )
}
