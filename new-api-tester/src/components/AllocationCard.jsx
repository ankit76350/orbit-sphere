import { useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import AllocationDonut from './charts/AllocationDonut.jsx'
import Meter from './ui/Meter.jsx'
import { ASSETS, OTHER_ALLOC, WALLET_TOTAL } from '../data/assets.js'
import { assetColor, OTHER_COLOR } from '../theme/palette.js'
import { useTheme } from '../theme/themeContext.js'
import { pct } from '../lib/format.js'

const PAGE_SIZE = 4

/**
 * Ring order is deliberate: BNB's amber sits between ETH's blue and SHARD's
 * magenta, because blue↔magenta is the one pair that fails CVD separation
 * when adjacent. Each asset keeps its own fixed color either way.
 */
const RING_ORDER = ['BTC', 'ETH', 'BNB', 'SHARD']

export default function AllocationCard() {
  const { theme } = useTheme()
  const [page, setPage] = useState(0)

  const pages = useMemo(() => {
    const out = []
    for (let i = 0; i < ASSETS.length; i += PAGE_SIZE) out.push(ASSETS.slice(i, i + PAGE_SIZE))
    return out
  }, [])

  const segments = useMemo(() => {
    const named = RING_ORDER.map((symbol) => {
      const asset = ASSETS.find((a) => a.symbol === symbol)
      return { key: symbol, label: asset.name, value: asset.alloc, color: assetColor(symbol, theme) }
    })
    // A 5th+ series is never a generated hue — the tail folds into "Other".
    return [...named, { key: 'OTHER', label: 'Other', value: OTHER_ALLOC, color: OTHER_COLOR[theme] }]
  }, [theme])

  const rows = pages[page]
  const maxAlloc = Math.max(...ASSETS.map((a) => a.alloc))
  const showingTail = page > 0

  return (
    <section className="card allocation-card">
      <AllocationDonut
        segments={segments}
        total={WALLET_TOTAL}
        assetCount={ASSETS.length}
        highlight={showingTail ? 'OTHER' : null}
      />

      <div className="legend-head">
        <span className="legend-note">
          {showingTail ? 'Grouped as “Other” in the ring' : 'Top holdings'}
        </span>
        <div className="pager">
          <button type="button" className="pager-btn" aria-label="Previous assets"
                  disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
            <ChevronLeft size={14} />
          </button>
          <span className="pager-count">{page + 1}/{pages.length}</span>
          <button type="button" className="pager-btn" aria-label="Next assets"
                  disabled={page === pages.length - 1}
                  onClick={() => setPage((p) => Math.min(pages.length - 1, p + 1))}>
            <ChevronRight size={14} />
          </button>
        </div>
      </div>

      {/* Legend doubles as the direct labels: name, ticker and exact share. */}
      <ul className="legend">
        {rows.map((asset) => {
          const color = assetColor(asset.symbol, theme)
          return (
            <li className="legend-row" key={asset.symbol}>
              <span className="legend-name">
                {asset.name} <span className="legend-ticker" style={{ color }}>({asset.symbol})</span>
              </span>
              <Meter value={asset.alloc} max={maxAlloc} color={color} />
              <span className="legend-value">{pct(asset.alloc, asset.alloc < 1 ? 1 : 0)}</span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
