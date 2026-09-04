# Extej — Crypto Dashboard

A React implementation of the Extej "Loans" dashboard: fixed sidebar, sticky topbar,
promo cards, wallet-balance area chart, allocation donut and a sortable-width wallets
table. Light and dark themes, responsive to 414px.

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run lint
```

## Stack

Vite + React 19, `react-router-dom` for navigation, `lucide-react` for icons.
Charts are hand-rolled SVG — no charting dependency — so the crosshair, the
gapped donut and the theme-aware palettes behave exactly as specified.

## Structure

```
src/
  components/
    Layout.jsx            app shell: sidebar + topbar + routed page
    Sidebar.jsx           nav sections, active pill, collapse rail, mobile drawer
    Topbar.jsx            search, notifications, user, theme switch, locale
    PromoCard.jsx         the two banner cards
    WalletBalanceCard.jsx hero figure + range control + chart/table toggle
    AllocationCard.jsx    donut + paged legend
    WalletsTable.jsx      holdings table with class filter and search
    charts/
      BalanceAreaChart.jsx  area chart, crosshair, keyboard inspection
      AllocationDonut.jsx   part-to-whole ring with hover detail
    ui/                   Segmented, Select, Meter, ChangeBadge, AssetMark
  data/                   assets.js, balance.js  (all mock data lives here)
  lib/                    format.js (i18n number/date), curve.js (path math)
  styles/                 tokens.css, base.css, layout.css, components.css
  theme/                  ThemeProvider, themeContext, palette.js
```

## Data integrity

`src/data/assets.js` stores price and allocation only; **value and holdings are
derived** (`value = total × alloc%`, `holdings = value / price`), so the table can
never show figures that contradict each other. Allocations across all 12 assets sum
to exactly 100, which is what makes the donut a true part-to-whole.

## Chart decisions worth knowing

These deviate slightly from the reference image, deliberately:

- **Series colors are validated, not sampled.** The reference's amber (`#f7a93b`)
  falls below 3:1 contrast on white, and its blue and magenta sit adjacent on the
  ring at ΔE 4.1 under protanopia — effectively the same color to a red-blind
  reader. Both themes now use steps that pass the lightness band, chroma floor,
  CVD separation, normal-vision floor and contrast checks. See `theme/palette.js`.
- **Ring order differs from legend order.** BNB's amber is drawn between ETH's blue
  and SHARD's magenta so that hard-to-separate pair is never adjacent. Each asset
  keeps its own fixed color, and the legend keeps the reference's reading order.
- **A 5th+ asset never gets a generated hue.** The eight small holdings aggregate
  into a neutral "Other" segment; the legend pager walks through their members.
- **Dark mode is a selected palette**, re-validated against the dark surface — not
  an automatic inversion of the light one.
- **Monotone cubic interpolation** (`lib/curve.js`) instead of a cardinal spline:
  a cardinal spline overshoots at this series' plateaus and would draw balances
  the data never had, including below zero.
- **Every chart has a table view** and the area chart is keyboard-inspectable
  (focus it, then arrow left/right to move the crosshair).

## Routes

`/` renders the Loans dashboard. Every other sidebar entry resolves to a
placeholder so navigation, active states and the shell can be exercised end to end.
