/**
 * Categorical series colors, assigned in FIXED order and never cycled.
 * Both sets pass the six palette checks against their own surface:
 *   light  #e5175c,#1f6fe0,#c9820f,#a72bc4  on #ffffff
 *   dark   #ef4477,#4180e9,#bc8722,#bb5acf  on #1a1a19
 * A 5th+ asset is never given a generated hue — it folds into "Other".
 */
export const CATEGORICAL = {
  light: ['#e5175c', '#1f6fe0', '#c9820f', '#a72bc4'],
  dark: ['#ef4477', '#4180e9', '#bc8722', '#bb5acf'],
}

export const OTHER_COLOR = { light: '#b3bcd4', dark: '#5c6478' }

/** Color follows the entity, never its rank or the current filter. */
export const ASSET_COLOR_SLOT = { BTC: 0, ETH: 1, BNB: 2, SHARD: 3 }

export function assetColor(symbol, theme) {
  const slot = ASSET_COLOR_SLOT[symbol]
  if (slot === undefined) return OTHER_COLOR[theme]
  return CATEGORICAL[theme][slot]
}
