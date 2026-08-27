# postman — request collection for the Orbit Sphere API

`Orbit Sphere — API.postman_collection.json` — **Import → File** in Postman.

## The convention: one request per endpoint

Not one request per test case. **One request per API endpoint**, with every test case for that
endpoint living inside the request body as a numbered, commented-out block.

To run a case: swap the active body for the block you want, and Send. Postman strips `//`
comments from a raw JSON body before sending, so the comments cost nothing.

> If your Postman version does not strip them, the request fails with `MALFORMED_REQUEST`.
> Delete the comment block for that one send.

## How to write a new request

### 1. Name it after the endpoint, not the case

`Create School`, `Activate School`, `Create Academic Year`. The method and path live in the
request; the name should read as the operation.

### 2. Active body = the ordinary happy path

Valid JSON, no comments above it, so the request works the moment somebody hits Send without
reading anything.

### 3. Below it, the test-case block

```
// ===========================================================================
//  TEST CASES  —  POST /platform/schools
//  Swap the body above for any block below, then Send.
//  Postman strips these // comments before sending.
// ===========================================================================
//
// ---------------------------------------------------------------------------
// 02  MINIMUM PAYLOAD — only the 6 required fields          -> 201 Created
//     Everything omitted is stored as null, not "".
// {
//   "schoolName": "Minimum Fields School",
//   ...
// }
// ---------------------------------------------------------------------------
```

Every case carries:

| Part | Why |
|---|---|
| **Two-digit number** | so it can be referred to — "case 07 fails" |
| **SHORT NAME IN CAPS** | scannable down the left edge |
| **`-> STATUS`** | the expected result, on the same line |
| **`IN:` / `OUT:`** where useful | the input that triggers it, the shape that comes back |
| **A one-line why** | especially for a 409, which people mistake for a bug |
| **The payload, commented** | ready to paste over the active body |

Case `01` is the active body, so its block documents rather than repeats it.

### 4. Order the cases: success first, then failures

`01` full · `02` minimum · `03` variants · then normalisation · then every error, grouped by
the field they concern.

### 5. Repeat the contract in the request description

The **Description** field is markdown and renders in Postman's docs pane. Put the required and
optional field tables there, plus the fields the endpoint **refuses** if sent. The body block is
for running; the description is for understanding.

### 6. Write tests for the active body

The tests assert case `01`. Say so in a comment at the top of the test script, because they will
be wrong for whichever case somebody swaps in — that is expected, not a bug.

Save anything later cases need:

```js
pm.collectionVariables.set('schoolId', body.schoolId);
pm.collectionVariables.set('createdSubdomain', body.subdomain);
```

### 7. Make unique values unique per run

`subdomain` is **globally unique**, so a fixed value works once and returns `409` forever after.
Use `{{$timestamp}}`:

```json
"subdomain": "orbit-astra-{{$timestamp}}"
```

Fixed values are right where the case is *meant* to fail every time — `api`, `-bad-`.

## Running it

```bash
cd backend && ./mvnw spring-boot:run     # ~15s, port 3456
```

Then Send, or **Run collection** for the active bodies.

## Variables

| Variable | Set by | Used by |
|---|---|---|
| `baseUrl` | you — defaults to `http://localhost:3456` | everything |
| `schoolId` | a successful create | endpoints taking `{id}` |
| `createdSubdomain` | a successful create | case 05, duplicate |

## Folders mirror `controllers/`

`Core / School` today. Next is `Core / Academic Year`. One folder per model package, so the
collection and the code stay findable from each other.

## Coverage

**3 of 28 planned write endpoints.** The other 27 are specified in
`backend/src/main/java/com/orbitastra/backend/controllers/core/README.md` and are not built —
a collection full of 404s is worse than a short honest one.

## A note on all those 409s

Five of the ten cases expect `409`, not `400`, and that is deliberate across this whole API.

- **400** — *this is not a well-formed request*: nothing sent, a date that is not a date.
- **409** — *the request is fine and the answer is still no*: the subdomain is spelled correctly
  and taken; the time zone is a reasonable guess that does not exist.

Told `400` for a taken subdomain, a caller goes hunting their JSON for a mistake that is not
there.
