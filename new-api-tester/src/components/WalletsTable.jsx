import { useMemo, useState } from 'react'
import { MoreHorizontal, Search } from 'lucide-react'
import AssetMark from './ui/AssetMark.jsx'
import ChangeBadge from './ui/ChangeBadge.jsx'
import Meter from './ui/Meter.jsx'
import Select from './ui/Select.jsx'
import { ASSETS, ASSET_CLASSES } from '../data/assets.js'
import { assetColor } from '../theme/palette.js'
import { useTheme } from '../theme/themeContext.js'
import { holdings as fmtHoldings, money, pct, price } from '../lib/format.js'

export default function WalletsTable() {
  const { theme } = useTheme()
  const [assetClass, setAssetClass] = useState('Crypto')
  const [query, setQuery] = useState('')

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return ASSETS
    return ASSETS.filter(
      (a) => a.name.toLowerCase().includes(q) || a.symbol.toLowerCase().includes(q),
    )
  }, [query])

  const maxAlloc = Math.max(...ASSETS.map((a) => a.alloc))

  return (
    <section className="card wallets-card">
      <div className="card-head">
        <h2 className="card-title">Wallets</h2>
        <div className="card-head-tools">
          <Select
            label="Asset class"
            value={assetClass}
            onChange={setAssetClass}
            options={ASSET_CLASSES}
          />
          <div className="search search-sm">
            <Search size={15} className="search-icon" aria-hidden="true" />
            <input
              className="search-input"
              type="search"
              placeholder="Search crypto, proto"
              aria-label="Search assets"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="table-scroll">
        <table className="data-table wallets-table">
          <caption className="sr-only">
            {assetClass} holdings with price, 24 hour change, holdings, value and allocation
          </caption>
          <thead>
            <tr>
              <th scope="col">Asset</th>
              <th scope="col" className="num">Price</th>
              <th scope="col" className="num">24h</th>
              <th scope="col" className="num">Holdings</th>
              <th scope="col" className="num">Value</th>
              <th scope="col">Allocation</th>
              <th scope="col"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((asset) => (
              <tr key={asset.symbol}>
                <td>
                  <span className="asset-cell">
                    <AssetMark symbol={asset.symbol} />
                    <span className="asset-text">
                      <span className="asset-name">{asset.name}</span>
                      <span className="asset-ticker">{asset.symbol}</span>
                    </span>
                  </span>
                </td>
                <td className="num">{price(asset.price)}</td>
                <td className="num"><ChangeBadge value={asset.change24h} /></td>
                <td className="num">{fmtHoldings(asset.holdings, asset.symbol)}</td>
                <td className="num">{money(asset.value)}</td>
                <td>
                  <span className="alloc-cell">
                    <Meter value={asset.alloc} max={maxAlloc} color={assetColor(asset.symbol, theme)} />
                    <span className="alloc-value">{pct(asset.alloc, asset.alloc < 1 ? 1 : 0)}</span>
                  </span>
                </td>
                <td>
                  <button type="button" className="row-menu" aria-label={`Actions for ${asset.name}`}>
                    <MoreHorizontal size={18} />
                  </button>
                </td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-row">No assets match “{query}”.</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </section>
  )
}
