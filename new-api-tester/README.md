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

**One screen is wired up: Platform › Core › Schools**, all eight endpoints of it. The other five
routes render a placeholder that says how many endpoints are waiting for them.

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

## The API layer

Ported from `../api-battleground` rather than rewritten, because it was already the answer:

| | |
|---|---|
| `src/config/endpoints.js` | 45 endpoints — paths, required and optional fields, refusal codes, worked examples |
| `src/config/environments.js` | which backend, and the timeout |
| `src/lib/httpClient.js` | building a request and sending it |
| `src/lib/store.js` | the chosen environment, remembered in this browser |
| `src/lib/variables.js` | `{{name}}` placeholders, the way Postman does them |
| `src/api/` | `ApiProvider`, the split contexts, and `buildCall` |

**`endpoints.js` is generated, and now written to both apps in one run.**
`api-battleground/tools/generate-endpoints.py` reads the Postman collection and writes its output
to both `src/config/endpoints.js` files. A copy taken by hand is a copy that drifts the first
time somebody regenerates and forgets the other one.

**The contexts are split** — `useApi()` for what you do, `useApiState()` for what changes. When
one context carried both, a screen that loaded itself re-ran after every call, and each run
called again. Anything that loads itself should depend on `call`, never on the whole object.

## Screens

They live at `src/pages/{surface}/{module}/`, so a file's path says which caller it acts as:

```
src/pages/
  platform/core/Schools.jsx            ← built: 3 endpoints
  platform/core/SchoolDetail.jsx       ← built: 5
  platform/plans/                      ← Plan catalogue (9), Subscriptions (3)
  school/core/Profile.jsx              ← built: 5
  school/core/AcademicYears.jsx        ← built: 3
  school/core/AcademicYearDetail.jsx   ← built: 15
  school/core/NoSchoolChosen.jsx       ← what the school surface shows with no tenant
  school/plans/                        ← Subscription (2)
```

**Core is done: 31 of the 45 endpoints.** What is left is Plans.

A submodule in `screens.js` with a `screen` renders it; one without falls back to `Placeholder`,
so the navigation stays complete while the screens are filled in one at a time.

### Platform › Core › Schools — two screens, two addresses

`Schools.jsx` is the list: search, status filter, sort, paging, and the create. Three endpoints.

`SchoolDetail.jsx` is **one school at its own address**, `/platform-core/schools/{id}`. Five
endpoints. Opening a row goes there rather than opening a dialog, because a school is a thing you
work on for a while — read it, set it up, take it live, come back to it — and all of that wants
an address you can link to, reload and paste to somebody. It also wants room: the record is
twenty fields and five actions.

**The id comes from the URL, so the page reads the school itself** rather than being handed a row
from the list. Arriving from a link, a reload and the back button all work the same way, and what
is on screen is never a stale copy of a row that has since changed.

The record is laid out as labelled values. **The raw JSON is still there, behind a disclosure** —
this app exists to exercise the API so the payload matters, but it is what the fields were read
*from*, not the thing to lead with.

**The lifecycle buttons depend on where the school is:** one being set up gets "Finish setting up"
and "Take it live", a live one gets "Suspend", a suspended one gets "Let it back in". Offering all
five always would mean four of them answering `409`, which tells you nothing you could not have
been told first. Suspend asks for its reason before sending, because the API requires one — better
to ask than to send a request you know will be refused. Changing the web address is offered at
every status, because an address can be wrong on a school being set up just as easily as on a live
one; it sends the current subdomain alongside the new one, which is how a stale page is stopped
from renaming a school somebody else already renamed.

`src/paths.js` holds the address builders, deliberately apart from `screens.js`. That file imports
every page, so a page building a link back to its list would import it back and close a cycle —
which is not theoretical: it threw `Cannot access 'screenPath' before initialization` at module
load, and the bundler was perfectly happy about it.

### Every control says which endpoint it calls

`EndpointTag` sits beside every control and reads **name · METHOD path**:

```
The list, as filtered · GET   /platform/schools?page=0&size=20&sort=createdAt,desc
Take it live          · POST  /platform/schools/6a9a76a4…/activate
Change web address    · PATCH /platform/schools/6a9a76a4…/subdomain
```

**The name is there because a bare method and path says what is sent but not by what**, and the
two are only obvious while the tag happens to sit next to its button. Read on its own — in the
response panel, in the log — the name is the half that identifies the control. It falls back to
the endpoint's own name from the Postman collection, so a tag is never nameless, and that
fallback is the string to search for in Postman.

**The name is a chip, like the method, and it takes the button's colour.** `look` is passed the
same value the button gets, so a red Suspend button gets a red label and a brand-coloured Create
gets a brand one — the pair reads as one control rather than two things that happen to sit next
to each other. A plain read carries no tone, because it is nobody's primary action.

Both the button and its tag read that value from **one place** (`action.look`), which is what
keeps them matching; the smoke test asserts it, because a hard-coded tone on one of the two is
exactly how they would drift.

The path is the **real** request: path parameters filled in and the query string actually being
sent, so the tag under the list changes as you filter. Click it to reopen that endpoint's last
call. Until it has made one there is nothing to open, so it renders as plain text rather than a
button that does nothing.

Nothing hand-types a method or a path. Both come from the generated catalogue, so a tag cannot
drift from the call it describes — the one place that did (`"Read from GET /platform/schools/{id}"`
typed into a card heading) has been replaced by a tag.

Anything that changes something opens `ResponseModal` by itself — the answer to "did that work"
should not need a second click. Reads stay quiet; they run on load and the screen shows its own
message when one fails.

### The school surface needs to know which school

The school surface never names a school in the URL — the tenant comes from the
`X-School-Subdomain` header, which stands in for a session until there is sign-in. So
`GET /schools/current/profile` is meaningless until somebody says which school "current" is.

**"Acting as" in the top bar is that answer**, and it is there because it is a mode rather than a
page's setting: everything under `School` asks "what does THIS school see", and re-picking it per
screen is how somebody ends up testing the wrong tenant without noticing. It is remembered
between reloads, and `call()` attaches it to every school-surface request — filled in centrally,
because a screen that forgot would get a `400 TENANT_NOT_RESOLVED` that reads as its own bug, and
one that passed the wrong school would be worse: it would answer, about the wrong tenant.

**The picker is a real popover, not a `<datalist>`.** It was a datalist first, and with
eighty-odd schools the browser drew an unstyled list down most of the window — no search of its
own, no theme, and the school name in grey under a subdomain you could not read. A list that long
has to be searchable in place, which means owning the dropdown.

So clicking it opens a panel with its own search field, shaped like the one in the top bar: it
filters on **both** the subdomain and the name, because you may know a school by either, and each
row shows the subdomain first (it is what goes in the header) with the name and status beside it.
The current choice is ticked. Escape or a click outside closes it.

The list comes from `GET /platform/schools`, **loaded when the dropdown first opens** rather than
on mount — most screens never touch this control, and drawing it on every page would mean
requesting on every page. That call is the one place the two surfaces meet in this app. Typing is
still allowed: Enter accepts whatever is in the box, so a school outside the loaded hundred is
still reachable.

With no school chosen, every school-surface screen says so and where to fix it rather than firing
a call that cannot work.

### School › Core › Profile

Four writes, and their shapes are the point: `PATCH /profile` is a partial edit, `PUT /address`
is replaced whole (a patched address can name a city in the wrong state and still look fine),
`PATCH /localization` is partial, `PUT /logo` replaced whole. So **each section sends its own
request** and its button says which. One Save would mean inventing a fifth endpoint on the client
and guessing which of the four to call.

The time zone asks before it moves: the API refuses the change while a year is running unless
`confirmTimeZoneChange` is sent, so **"Change it anyway" only appears once the API has said it
needs confirming** — sending the flag every time would defeat the check it exists for.

### School › Core › Academic years

A year is addressed by its **name** — `/school-core/academic-years/2026-2027`. That is safe in a
URL precisely because the API guarantees the name never changes: it is the natural key other
collections reference.

Fifteen of the eighteen endpoints are on the year's own page, grouped by the question they answer
rather than by verb: the dates, the two gates, the holiday calendar, and two reads that take an
argument. **The calendar is dated entries each holding a list of events**, because one date can
be a festival *and* a school event — the screen keeps that shape rather than flattening it, since
flattening is what loses the second reason. `PATCH` and `DELETE` on one event send the type as a
query parameter for the same reason: the date alone does not identify which event is meant.

Two writes are deliberately destructive and say so — `PUT` replaces the whole calendar, and
`DELETE ?type=` removes every entry of that kind across the year. Both are marked danger, and
neither is the default action of anything.

## What comes next

**Plans**, both surfaces: Plan catalogue (9), Subscriptions (3), and a school's own view (2).
