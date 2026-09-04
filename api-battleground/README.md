# api-battleground

The Orbit Sphere school administration screens, running against the real backend.

It looks and works like the application a school actually uses — a list of schools, forms,
buttons, switches, a holiday calendar. Nothing is a mock: every button press is a real HTTP call
to Spring Boot, and if the backend is down you get "could not reach the backend", not made-up
data.

**Every action shows you what it did.** Anything that changes something — a create, a save, a
lifecycle button, a switch — opens a pop-up as soon as it finishes. It leads with the answer in
plain terms (what changed, and the fields that came back, laid out and labelled), with the wire
detail a tab away: the address, both sets of headers, both bodies as JSON, and the timing.

Reads are silent — a pop-up on every page load would be unusable — so a list that fails says so
in the page itself. The **Activity** button in the header lists every call the app has made;
click any of them to reopen its details.

Separate from [`../frontend`](../frontend), which it does not touch.

---

## Running it

Two terminals.

```bash
cd backend && ./mvnw spring-boot:run      # port 3456, takes about a minute
```

```bash
cd api-battleground && npm install && npm run dev    # http://localhost:1300
```

## The side panel

Navigation lives on the left: which module, and which of its screens.

Navigation is three levels, each in the place that suits it.

**1 — the side panel: which module.** One question, one level, nothing nested. **Core** is schools
and their academic years; **Plans** is what we sell and for how much. A module with no screens
yet is listed and disabled rather than hidden, because a panel showing only what is finished
suggests that is all there is.

**2 — a navbar inside the module: which of its screens, and what is open.** It sits in the
content column because that is where there is room: a row of labels reads at a glance, where the
same list indented three deep in a narrow panel did not. When something is open the bar keeps its
screen selected and shows what is open after it — `Catalogue › PREMIUM v2` — so it is both a
statement of where you are and the way back out.

**3 — the tab strip on a detail screen: which part of the open thing.** Overview, Settings,
Academic year for a school; Overview, Features, Versions for a plan.

Each module keeps its own open thing and its own tab. One shared tab would mean leaving Core on
Settings and arriving in Plans on a tab that does not exist there.

The panel used to hold all three levels at once, with the current module filled near-black and
the current tab ringed in blue: two things looked selected, in two different languages, and
neither read as the screen you were on. Now exactly one thing is highlighted at each level.

The panel and the tab strip inside a school are two ways of choosing the same thing, so the
choice is held once in the shell rather than in both — which is how they would come to disagree.

## The screens

### Schools

The list, the way an operator would use it: a search box, status filter chips, sorting, page
size and paging. Clicking a row opens that school.

`Add a school` is an ordinary sign-up form — the web address is suggested from the school's name
and previewed as `name.orbitastra.com`, and if the backend rejects a field the message appears
under that field rather than as a wall of JSON.

### One school

Opening a school reads the whole record — the list row is only a summary, so the address, the
logo, the language and the last-changed time come from `GET /platform/schools/{id}`.

The buttons that show depend on where the school is in its life:

| The school is | You get |
|---|---|
| being set up / on trial | **Finish setting up**, then **Take it live** |
| live | **Suspend** — asks for the reason, which is required |
| suspended | **Let it back in** — a note is optional |
| any | **Change web address** — asks you to confirm, and warns about saved links |

**Finish setting up** reports what it created — 47 numbering sequences, 3 roles — as counts and
role chips, not a JSON blob.

### Subscription

The tab that makes a school a paying customer: `POST /platform/schools/{id}/subscriptions`. It is
the piece `core` complains about — `activateSchool` was written to require a subscription, found
nothing could create one, and settled for a soft check that says so in every response. Create one
here and that response stops apologising.

**The plan is picked, not typed.** The list is `GET /platform/plans?status=ACTIVE`, so the only
plans offered are ones that exist. A plan that cannot be sold today is still listed, with the
reason: "Basic — BASIC v1 (Not published yet)" answers the question an absent row only
raises, and choosing it warns that the API will refuse with `409 PLAN_NOT_SELLABLE`. `planCode` and
`planVersion` both come off the chosen row, the way every plan URL names a plan.

Everything past the plan is optional and hidden behind **Set the negotiated terms**: the agreed
price, the period, per-school limit overrides, the gateway's customer reference, and a reason
that lands on the first `subscription_history` row. **An empty box is left out of the request
entirely** rather than sent as `null` — an empty field means "use the API's default", and a null
would overrule one. A `CUSTOM` billing cycle is the exception: it has no length of its own, so
the form asks for the period end up front instead of letting you find out via
`400 BILLING_PERIOD_END_REQUIRED`.

**It cannot read a subscription back, and says so.** The reads are #27–38 and none are built, so
the tab shows what it just created and loses it when you leave. That is stated on screen: a blank
panel meaning "nothing here" and a blank panel meaning "no endpoint to ask" are different
problems, and guessing between them is how somebody tries to create a second subscription for a
school that already has one. Today the way to find that out is the refusal —
`409 SUBSCRIPTION_ALREADY_EXISTS` — which the tab shows with what it means.

### Settings

The forms are filled from `GET /schools/current` — the read that sits behind the four write
endpoints — so the boxes show what is actually stored, not what the list happened to carry.

Four small forms, each saved on its own, because the backend keeps them as four operations and
one big Save would hide which one failed. Each save answers with the whole profile, and the
boxes refill from it, so a value the backend tidied up on the way in shows as it was stored.

Changing the time zone is refused by the backend until it is confirmed. The app catches that and
shows a dialog explaining what actually changes: no record moves, but the *day* the records
already belong to does.

### Academic year

Create the year, then set admissions and result locking with two switches.

The calendar shows the closed days grouped by month. **A day can be closed for more than one
reason** — a Sunday that is also Diwali — so each day carries its reasons as chips, each of
which can be edited or removed on its own; "Reopen" removes the whole day. **Weekly day off**
generates one real dated entry per occurrence of a weekday, because there is no "weekly off day"
setting anywhere in this system on purpose: a school may run on Sunday and close on another day.

Two questions the calendar can answer, each its own small panel:

- **Is the school open?** — pick a day and it says open or closed, and for closed days which
  reasons apply.
- **How many working days?** — pick a range (or leave it empty for the whole year) and it counts
  the working days, the closed days and the total.

Everything on this screen is read back from the backend. After any change the calendar is
re-read rather than guessed at, so what is on screen is what is stored.
### Plans

The catalogue, one row per plan **version** — `PREMIUM v1` and `PREMIUM v2` are two prices, and a
school is on exactly one of them. Search, status chips, sorting and paging, like the school list.

**"Can be bought" is the column worth having.** The API returns `sellable` as a single boolean off
three separate facts — published, on the public list, inside its selling window — so when it is
false the screen says *which* one is missing: "Not published yet", "Not on the public list", "Past
its selling window".

Opening a plan gives three sections:

- **Overview** — the commercial terms, the selling window, and how many schools are on this
  version. That count is 0 for everything until subscriptions can be created, and the screen
  passes the API's own note along rather than showing a bare zero.
- **Features** — the 24 features as tick boxes. Ticking and saving replaces the **whole list**,
  because a feature list is priced as a set. A limit box only appears on the features that have
  something to count: "attendance" is included or it is not. Read-only once published.
- **Versions** — every version newest first, with the price change against the one before it.

**The lifecycle buttons depend on where the plan is.** A draft can be edited, given features and
published. A published plan can be listed publicly or retired. A retired one can only be read.
Publish and retire both ask first and say what will not be possible afterwards, because neither
can be undone and there is no endpoint that returns a plan to draft.

**Every control says which endpoint it calls**, the same as on the Core screens — the live
request under the heading (filters and paging included), and the endpoint under each lifecycle
button. Click a tag to reopen that endpoint's last call.

**New plan** asks for the name, not the code: the code is derived from the name — "Premium Plus"
becomes `PREMIUM_PLUS` — and the form shows what it will be as you type, since it is permanent.

**Not reachable from these screens yet:** creating a subscription (`POST
/platform/schools/{id}/subscriptions`). It is built and belongs to a school rather than to a plan,
so it wants a tab on the school detail.

## Where the calls come from

The screens never write a URL. They say what they want:

```js
api.call('activate-school', { pathParams: { id: school.schoolId } })
api.call('update-profile', { subdomain: school.subdomain, body: { schoolName } })
```

[`src/api/buildCall.js`](src/api/buildCall.js) turns that into a request, and the paths and
methods come from [`src/config/endpoints.js`](src/config/endpoints.js), which is generated from
the Postman collection:

```bash
python3 tools/generate-endpoints.py
```

All 23 endpoints are covered, with the collection's own notes carried across — they are what the
*About this call* tab in the details pop-up shows.

### The tenant header

Every `/schools/current/…` call needs `X-School-Subdomain`; without it the backend answers
`400 TENANT_NOT_RESOLVED`. There is no sign-in yet, so `CurrentSchoolResolver` reads the tenant
from that header. The app adds it from the school you are working on — nobody types a header.

## Which backend

Picked in the header, remembered in this browser. No base URL is written into the code.

| Environment | Base URL | Notes |
|---|---|---|
| Development (proxy) | *(empty)* | Same origin; this dev server forwards to 3456. **Use this one** |
| Development (direct) | `http://localhost:3456` | Needs `DevCorsConfig` on the backend |
| Staging / Production | placeholders | Edit them when those servers exist |

Prefer the proxy: no CORS, and the browser lets the page read every response header — which
matters, because `Location` is where a new school's id comes back.

## Tests

```bash
npm test               # render + behaviour, no backend needed
npm run test:render    # renders every screen headlessly; fails on anything that throws
npm run test:behaviour # mounts screens in a real DOM against a stubbed backend
npm run test:e2e       # walks every screen action against a real backend on 3456
```

`test:render` paints 25 screens and states; `test:behaviour` runs 67 checks against a stubbed
backend. `test:e2e` covers all 31 endpoints — 59 checks including the refusals the screens are
built around: a duplicate web address, a suspension with no reason, an unconfirmed time-zone change,
the same kind of holiday twice on one day, shrinking a year past a closed day, and a 404 for a
year name that does not exist.

`test:behaviour` is the one that runs effects, so it can check what the other two cannot: that a
screen loads **once** rather than in a loop, that what comes back reaches the screen, and that a
dead backend is named as such. The reload loop that once hammered the backend is a single
assertion there.

`test:render` also runs two static checks, for two bugs that got past everything else:

- **no screen depends on the whole `api` object.** When one context carried both the actions and
  the activity log, a screen that loaded itself re-ran after every call — and each run called
  again. The context is split now (`useApi()` for what you do, `useApiState()` for what changes),
  and this check stops a future merge bringing the loop back.
- **every endpoint name a screen asks for exists.** A wrong name only throws when the button is
  pressed, which is how a rename silently broke three handlers.

## Other things worth knowing

- The details pop-up opens for POST, PUT, PATCH and DELETE. GET stays quiet.
- Requests give up after 30 seconds.
- **If the backend is not running, it says so.** A dev server whose proxy target is dead answers
  `500 text/plain` with an empty body, which would otherwise read as "the backend threw, check
  the server log" and send you looking through logs that do not exist. The client spots the
  empty body and names the real cause instead.
- The dev server uses `strictPort`, so a second `npm run dev` fails rather than quietly starting
  another copy on 1301 — two servers means two tabs both loading, which looks in the log like
  the app asking for the same thing over and over.
- Staging and Production have no address set. Choosing one shows a banner and sends nothing,
  rather than failing as a name-lookup error that reads like a bug.
- Nothing is sent with cookies, so the app behaves like a plain client.
- Only the chosen environment and the timeout are stored in the browser.
