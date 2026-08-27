# postman — request collection for the Orbit Sphere API

`orbit-sphere.postman_collection.json` — import it into Postman with **Import → File**.

## Running it

Start the backend first:

```bash
cd backend && ./mvnw spring-boot:run     # ~15s, port 3456
```

Then **Run collection** in Postman. Every request carries tests, so a run is a regression
suite rather than a list of calls — 10 requests, 26 assertions.

Run the folder **top to bottom**. Request 01 saves `schoolId` and `createdSubdomain` as
collection variables, and request 05 needs `createdSubdomain` to test the duplicate case.

## Why subdomains use `{{$timestamp}}`

`subdomain` is **globally unique**, not per-school. A fixed value works exactly once and returns
`409 SUBDOMAIN_TAKEN` on every run after that, so the create requests build one per run.

Requests 06 and 07 use fixed values on purpose — `api` and `-bad-` are meant to be rejected
every time.

## Variables

| Variable | Set by | Used by |
|---|---|---|
| `baseUrl` | you | everything |
| `schoolId` | request 01 | future endpoints taking `{id}` |
| `createdSubdomain` | request 01 | request 05 |

`baseUrl` defaults to `http://localhost:3456`, which is `server.port` in
`application.properties`.

## What is covered

**1 of 28 planned write endpoints.** The other 27 are specified in
`backend/src/main/java/com/orbitastra/backend/controllers/core/README.md` and are not built, so
they are not in here — a collection full of 404s is worse than a short one.

| # | Request | Expects |
|---|---|---|
| 01–03 | create: full, minimum, trial | 201 |
| 04 | subdomain normalisation | 201, `Norm_Check 123` → `norm-check-123` |
| 05 | duplicate subdomain | 409 `SUBDOMAIN_TAKEN` |
| 06 | reserved subdomain | 409 `SUBDOMAIN_RESERVED` |
| 07 | malformed subdomain | 409 `SUBDOMAIN_INVALID` |
| 08 | unknown time zone | 409 `TIME_ZONE_INVALID` |
| 09 | missing/invalid fields | 400 + per-field `fieldErrors` |
| 10 | malformed JSON | 400, and **asserts no stack trace leaks** |

## Note on the 409s

Five requests expect 409 rather than 400, and that is deliberate throughout this API. A 400
means *this is not a well-formed request*. A 409 means *the request is fine and the answer is
still no* — the subdomain is spelled correctly and taken, the time zone is a reasonable guess
that does not exist.

Told 400 for a taken subdomain, a caller goes hunting their JSON for a mistake that is not
there.

## Adding to it as endpoints get built

Keep one folder per model package, mirroring `controllers/`. The next folder is
**Core / Academic Year**. Give every request tests — the collection is only worth keeping if
running it proves something.
