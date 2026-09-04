import { assetColor } from '../../theme/palette.js'
import { useTheme } from '../../theme/themeContext.js'

/** The colored token that carries an asset's identity next to its name. */
export default function AssetMark({ symbol, size = 34 }) {
  const { theme } = useTheme()
  const color = assetColor(symbol, theme)
  return (
    <span
      className="asset-mark"
      style={{ '--mark-size': `${size}px`, background: color }}
      aria-hidden="true"
    >
      {symbol.slice(0, 3)}
    </span>
  )
}
