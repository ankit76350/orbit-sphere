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

## The screens

### Schools

The list, the way an operator would use it: a search box, status filter chips, sorting, page
size and paging. Clicking a row opens that school.

`Add a school` is an ordinary sign-up form — the web address is suggested from the school's name
and previewed as `name.orbitastra.com`, and if the backend rejects a field the message appears
under that field rather than as a wall of JSON.

### One school

The buttons that show depend on where the school is in its life:

| The school is | You get |
|---|---|
| being set up / on trial | **Finish setting up**, then **Take it live** |
| live | **Suspend** — asks for the reason, which is required |
| suspended | **Let it back in** — a note is optional |
| any | **Change web address** — asks you to confirm, and warns about saved links |

**Finish setting up** reports what it created — 47 numbering sequences, 3 roles — as counts and
role chips, not a JSON blob.

### Settings

Four small forms, each saved on its own, because the backend keeps them as four operations and
one big Save would hide which one failed.

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

> **One honest gap.** The backend has no endpoint that *reads* a year or its calendar — every
> academic-year endpoint is a write. So this page shows the answer from the last change made
> here, and says so on screen. When a read endpoint exists, this is the one page that changes.

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
npm run test:render   # renders every screen headlessly; fails on anything that throws
npm run test:e2e      # walks every screen action against a real backend on 3456
```

`test:e2e` covers all 23 endpoints — 46 checks including the refusals the screens are built
around: a duplicate web address, a suspension with no reason, an unconfirmed time-zone change,
the same kind of holiday twice on one day, and shrinking a year past a closed day.

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
