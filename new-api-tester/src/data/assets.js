/** Total portfolio value the allocations are computed against. */
export const WALLET_TOTAL = 12433.35
export const WALLET_CURRENCY = 'EUR'

/**
 * Allocation percentages sum to exactly 100 across all 12 assets, so the
 * donut is a true part-to-whole. The four named holdings carry the
 * categorical colors; the remaining eight are aggregated as "Other".
 */
const RAW = [
  { symbol: 'BTC',   name: 'Bitcoin',      price: 61240.55, change24h:  2.14, alloc: 24.0 },
  { symbol: 'ETH',   name: 'Ethereum',     price:  3382.10, change24h: -1.08, alloc: 18.0 },
  { symbol: 'SHARD', name: 'Shard',        price:     0.4185, change24h: 5.62, alloc: 32.0 },
  { symbol: 'BNB',   name: 'Binance Coin', price:   598.32, change24h:  0.87, alloc: 22.0 },
  { symbol: 'SOL',   name: 'Solana',       price:   148.20, change24h:  3.41, alloc: 1.1 },
  { symbol: 'ADA',   name: 'Cardano',      price:     0.4523, change24h: -0.54, alloc: 0.8 },
  { symbol: 'XRP',   name: 'Ripple',       price:     0.6112, change24h: 1.22, alloc: 0.6 },
  { symbol: 'DOT',   name: 'Polkadot',     price:     6.84, change24h: -2.31, alloc: 0.5 },
  { symbol: 'AVAX',  name: 'Avalanche',    price:    27.45, change24h:  4.08, alloc: 0.4 },
  { symbol: 'MATIC', name: 'Polygon',      price:     0.5231, change24h: -0.76, alloc: 0.3 },
  { symbol: 'LTC',   name: 'Litecoin',     price:    84.10, change24h:  0.44, alloc: 0.2 },
  { symbol: 'LINK',  name: 'Chainlink',    price:    14.22, change24h:  1.97, alloc: 0.1 },
]

/** Value and holdings are derived, so the table can never contradict itself. */
export const ASSETS = RAW.map((a) => {
  const value = (WALLET_TOTAL * a.alloc) / 100
  return { ...a, value, holdings: value / a.price }
})

export const TOP_ASSETS = ASSETS.slice(0, 4)

export const OTHER_ASSETS = ASSETS.slice(4)

export const OTHER_ALLOC = OTHER_ASSETS.reduce((s, a) => s + a.alloc, 0)

export const ASSET_CLASSES = ['Crypto', 'Fiat', 'Stablecoins', 'NFTs']
