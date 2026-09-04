import { TrendingDown, TrendingUp } from 'lucide-react'
import { signedPct } from '../../lib/format.js'

/** Status color ships with an icon and a sign — never color alone. */
export default function ChangeBadge({ value }) {
  const up = value >= 0
  const Icon = up ? TrendingUp : TrendingDown
  return (
    <span className="change-badge" data-dir={up ? 'up' : 'down'}>
      <Icon size={13} aria-hidden="true" strokeWidth={2.5} />
      {signedPct(value)}
    </span>
  )
}
