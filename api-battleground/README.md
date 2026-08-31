# api-battleground

A place to fire requests at the Orbit Sphere backend and see exactly what happened.

Postman-style request building and response inspection, plus an application-like view of the
data for reads. It is a separate app from [`../frontend`](../frontend) and shares nothing with
it — different port, different dependencies, its own storage.

**Every call it makes is a real one.** There is no sample data and no offline mode anywhere in
this app. If the backend is down you get a "could not reach the backend" panel, not a made-up
answer.

---

## Running it

Two terminals.

```bash
cd backend && ./mvnw spring-boot:run      # port 3456, takes about a minute
```

```bash
cd api-battleground && npm install && npm run dev    # http://localhost:1300
```

## What it knows about

Read out of the backend, not typed in by hand:

| Where from | What it gave |
|---|---|
| `controllers/core/SchoolController.java` | the 5 endpoints that exist, their methods and paths |
| `dto/core/*.java` | which fields are required, which are optional, what comes back |
| `services/core/SchoolService.java` | every error code and when it happens |
| `common/error/ApiError.java` | the error shape, so `fieldErrors` gets its own table |
| `postman/…postman_collection.json` | the numbered test cases, one click each |
| `controllers/core/README.md` | the other 25 write endpoints, marked **plan** |

**5 built, 25 planned.** The planned ones are greyed out and can still be sent — a 404 from a
path that does not exist is a real answer, and a list that pretends otherwise is worse than an
honest short one. "Built only" in the sidebar hides them.

### The flow that works today

`Create School` → `Complete Provisioning` → `Activate School` → `Suspend School` →
`Reactivate School`

The id links them, and you do not have to copy it: a successful create saves `{{schoolId}}` and
`{{createdSubdomain}}`, and the next four endpoints already point at them.

## The panels

**Left** — endpoints grouped by backend package, with method badges, search, and the built-only
filter.

**Request** — method, URL, Send, and tabs:

| Tab | What is on it |
|---|---|
| Params | path parameters, and query parameters that can be switched off without deleting |
| Headers | same, with a tick per row |
| Auth | none / bearer / basic / API key. Nothing needs it yet; it is ready for when it does |
| Body | JSON editor with pretty, minify, clear, copy, and a warning when the JSON is broken |
| Docs | required and optional fields, what comes back, and every error this endpoint can give |
| Cases | the Postman test cases. "Use this" loads that body |

A broken body is still sent exactly as typed — that is how you check what the backend does with
one.

**Response** — status, time, size, start and finish times, then:

| Tab | What is on it |
|---|---|
| Overview / Data | for a GET, the data drawn as a table or a set of fields. For a write, the same view of what was written, plus the error panel |
| Body | the JSON tree — collapsible, searchable, colour-coded, copyable, with a raw view |
| Response headers | all of them. `Authorization` and any API key are shortened |
| Request details | method, URL, filled-in path parameters, headers, body, and the timings |

For a GET the Data tab opens first, because the data is the point. For a write the Body tab
opens first, because the exact answer is the point.

### The Data view

It works out what to draw from the shape of the response, not from a list of known endpoints:

- a list of records → a table, with the columns being every field across all rows
- a page (`content`, `items`, `data`, `results`) → the table plus the page details
- one record → labelled fields, with nested things as their own tables and cards
- `nextStep` is pulled out into its own line, because it is the part written for a person

That means it already works for the GET endpoints nobody has built yet.

## Errors

Every one is shown as itself, with the backend's `code`, its message, and a plain sentence
about what that status means here.

- `VALIDATION_FAILED` gets a field-by-field table straight from `fieldErrors`
- 409 gets said plainly, because in this API it means *the request is fine and the answer is
  still no* — not *your JSON is wrong*
- a non-JSON body (a Spring error page, say) is shown as text rather than an empty panel
- no answer at all — backend down, CORS, timeout — gets its own panel saying which, how long it
  waited, and what to check

## History

The last 100 calls, kept in this browser. Method, path, status, time, size, when, and which
environment. **Inspect** reopens that exact request and response; **Resend** runs it again.
Filter by path or status, or show failures only.

## Environments

Nothing writes a base URL into the code. Four to start with, all editable in Settings, all
saved in this browser:

| Environment | Base URL | Notes |
|---|---|---|
| Development (proxy) | *(empty)* | Same origin. This dev server forwards to 3456. **Use this one** |
| Development (direct) | `http://localhost:3456` | Needs `DevCorsConfig` on the backend |
| Staging | placeholder | Edit it when there is a staging server |
| Production | placeholder | These endpoints create and suspend real schools |

Prefer the proxy: no CORS, and the browser lets the page read every response header — which
matters, because `Location` is where the new school's id comes back.

The direct environment works because of
[`DevCorsConfig`](../backend/src/main/java/com/orbitastra/backend/config/DevCorsConfig.java),
which allows any localhost port and exposes `Location`. It is on for the `dev` profile only.

## Variables

`{{name}}` anywhere in a URL, a header or a body, the same as the Postman collection so text
can be pasted between them.

| Variable | Filled by |
|---|---|
| `{{schoolId}}` | a successful Create School, Complete Provisioning or Activate |
| `{{createdSubdomain}}` | a successful Create School |
| `{{academicYear}}` | you — used by the planned academic-year paths |
| `{{$timestamp}}` | made fresh on every send. This is what keeps a subdomain unique |

Also `{{$isoTimestamp}}`, `{{$randomInt}}`, `{{$guid}}`. A placeholder with no value is left
visible in the URL rather than blanked, and the URL bar warns about it before you send.

## Other things worth knowing

- **Ctrl / Cmd + Enter** sends.
- **Cancel** during a request drops the answer when it arrives, rather than letting a stale one
  land in the panel.
- Requests give up after 30 seconds by default. Raise it in Settings if you are starting the
  backend at the same time.
- Nothing is sent with cookies (`credentials: 'omit'`), so the tester behaves like a plain
  client.
- Everything saved — environments, variables, credentials, history — lives in this browser's
  local storage under `orbit.battleground.*`. Nothing leaves the machine.

## Adding an endpoint when one gets built

One file: [`src/config/endpoints.js`](src/config/endpoints.js). Move the row out of the planned
table into the live list and fill in `requiredFields`, `responseFields`, `errors`, `captures`
and the `examples`. Nothing else needs touching — the sidebar, the docs tab, the cases tab and
the counts all read from there.
