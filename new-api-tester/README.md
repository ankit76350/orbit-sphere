# new-api-tester

The environment for exercising the Orbit Sphere API by hand. It is the shell of the Extej
dashboard template with the crypto dashboard taken out — the layout, the sidebar, the theming and
a few UI primitives kept, and everything that was about wallets and coins removed.

```bash
cd new-api-tester && npm install && npm run dev    # http://localhost:1400
```

**Nothing calls the API yet.** Every route renders a placeholder. What is here is the frame the
screens will sit in.

## What was removed, and why

The template was a crypto dashboard. None of that content survives:

| Removed | What it was |
|---|---|
| `pages/Loans.jsx` | the demo dashboard |
| `PromoCard`, `WalletBalanceCard`, `AllocationCard`, `WalletsTable` | its four panels |
| `charts/BalanceAreaChart`, `charts/AllocationDonut` | hand-rolled SVG charts |
| `ui/AssetMark`, `ui/ChangeBadge` | a coin glyph and a ±% badge |
| `data/assets.js`, `data/balance.js` | sample holdings and a price series |
| `lib/format.js`, `lib/curve.js` | EUR money formatting, chart maths |
| `public/icons.svg` | an unreferenced sprite |
| 72 CSS rules | every rule whose only users were the files above |

**The top bar lost four controls too.** It shipped a notifications bell, a messages icon, a
language flag, and a signed-in user reading *"Austin Robertson — Marketing Administrator"*. None
of them did anything, and the user was invented: this app has no accounts and no sign-in, so a
name and a role on screen would be a claim about somebody who does not exist. A control that
looks live and is not costs more than the space it fills.

`components.css` went from 362 lines to 232, and the built stylesheet from 16.6 kB to 11.3 kB.

## What was kept

**The shell**, which is the reason for using this template at all: `Layout` (sidebar + sticky
topbar + routed page), `Sidebar` (collapse rail, mobile drawer, active pill), `Topbar` (search and
the theme switch), and the theme — light and dark, `tokens.css` plus a `ThemeProvider`.

**Three UI primitives** — `Meter`, `Segmented`, `Select`. Nothing renders them today; they are
kept because a request builder wants all three within the first screen.

**Ten CSS classes with no user yet**, for the same reason: `data-table`, `table-scroll`,
`empty-row`, `pager`, `pager-btn`, `pager-count`, `card-head`, `card-head-tools`, `ghost-btn`,
`search-sm`. The first list screen needs a table, a pager and a card header, and the styling for
them is already written and themed.

## The navigation follows the API's own groups

`SCREENS` in `src/screens.js` is the single list of routes; `App.jsx` builds the router from it
and `Sidebar.jsx` builds the nav from the same list, so a nav item cannot point at a route that
does not exist. It lives in its own module rather than beside the sidebar because fast refresh
only works when a component file exports nothing but components. The six entries are the six
groups the built endpoints fall into, with a badge counting how many are waiting:

| Screen | Endpoints built |
|---|---|
| Core → Schools | 8 |
| Core → School profile | 5 |
| Core → Academic years | 18 |
| Plans → Plan catalogue | 9 |
| Plans → Subscriptions | 3 |
| Plans → A school's own view | 2 |

Following the API's grouping rather than inventing one means a screen is always about **one
surface** of one module. That is why the platform's subscription endpoints and the school's own
two reads are separate rows rather than a single "Subscriptions" screen: they answer to different
callers, and one of them must never show what the other does — the school's read deliberately
withholds the plan's list price, the gateway's customer reference and the negotiated limits.

The counts add up to the 45 endpoints in the generated catalogue. Recount them there rather than
trusting the badges if they start to look stale.

## The dev server

**Port 1400**, so this and `../api-battleground` (1300) run side by side. `strictPort` is on:
starting a second copy fails rather than quietly moving to 1401, because two tabs both loading
reads in a request log as the app asking twice for everything.

`/platform/**` and `/schools/**` are proxied to the backend on **3456**, anchored to whole path
segments so a route in this app whose name merely starts with one of those words is not sent to
the backend by mistake. Going through the proxy means no CORS, and the browser can read every
response header — which matters, because `Location` is where a newly created id comes back.

## What comes next

The API layer. `../api-battleground` already has the pieces worth taking rather than rewriting:

- **`src/config/endpoints.js`** — 45 endpoints with their paths, required and optional fields,
  refusal codes and worked examples, generated from the Postman collection by
  `tools/generate-endpoints.py`. It is generated, so it cannot drift from the collection.
- **`src/api/`** — the send/inspect layer: environments, the tenant header, timeouts, and a log
  of every call made.

Whether to copy those across or import them is the first decision to take.
