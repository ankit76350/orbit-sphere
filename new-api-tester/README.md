# new-api-tester

The environment for exercising the Orbit Sphere API by hand. It is the shell of the Extej
dashboard template with the crypto dashboard taken out — the layout, the sidebar, the theming and
a few UI primitives kept, and everything that was about wallets and coins removed.

```bash
cd new-api-tester && npm install
npm run dev     # http://localhost:1400
npm test        # renders every route headlessly
npm run lint
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

## Navigation is three levels, each where it fits

**1 — the side panel, top level: which surface.** `Platform` is an operator from outside the
tenant, naming the school in the URL. `School` is a school acting on itself, naming nothing
because the tenant comes from the header. This is the most important split in the API — it is
what decides which fields a caller may see — so it is the outermost thing on screen. Mixing the
two in one list would put the endpoint that must not leak next to the one it must not leak from.

**2 — the side panel, inside a section: which module.** `Core` is schools and their academic
years, `Plans` is what we sell. The badge counts the endpoints built under that module.

**3 — a navbar in the body: which submodule.** The six groups the built endpoints fall into. It
sits in the content column because that is where a row of labels reads at a glance; the same
list as a third level of indent in a 15rem column does not, and it makes two things look
selected at once. Above the row it names where you are — `PLATFORM › Plans`. That matters more
here than in most apps: `Platform › Plans › Subscriptions` and `School › Plans › Subscription`
are different endpoints with deliberately different answers, and that line is the only thing
telling the two screens apart.

| Surface | Module | Submodules | Endpoints |
|---|---|---|---|
| Platform | Core | Schools | 8 |
| Platform | Plans | Plan catalogue, Subscriptions | 12 |
| School | Core | Profile, Academic years | 23 |
| School | Plans | Subscription | 2 |

That is 45, the number in the generated catalogue. Recount them there rather than trusting the
badges if they start to look stale.

### One list, and the router is built from it

`SURFACES` in `src/screens.js` is the only place the structure is written down. `Sidebar` builds
the sections from it, `ModuleNav` builds the submodule row from it, and `App` builds the router
from it — so a nav item pointing at an address with no screen is not something to remember to
check, it cannot be written down. It lives in its own module rather than beside a component
because fast refresh only works when a component file exports nothing but components.

### Why an address is `/platform-plans/catalogue` and not `/platform/plans/catalogue`

Because the dev server proxies `^/platform($|/)` and `^/schools($|/)` to the backend. A route
under `/platform/` would never reach the router: the proxy would send it to the API and Spring
would answer 404. Joining the surface and module with a hyphen cannot match either anchor, now
or when a new API group appears — and it happens to give one URL segment per level of
navigation, so the side panel picks the first and the body navbar picks the second.

## Tests

`npm test` renders all seven addresses through `StaticRouter` and checks the words that prove the
right screen answered, then checks five things about the navigation: both surfaces present, the
current module marked active, the navbar showing this module's submodules **and not another
module's**, and the surface named. It bundles with rolldown, which Vite already ships here, so
there is nothing to install.

It exists because this project had no harness at all and the failures it catches are the quiet
kind — a route that resolves to the wrong screen still renders a page that looks fine.

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
