# controllers/room — API plan

**Three of three are built — [#1](#e1), [#2](#e2) and [#3](#e3), on 2026-10-21.**

The school's rooms: add one, list them, read one. Mirrors [`models/room`](../../models/room/README.md) — one
collection, [`rooms`](../../models/room/Room.java).

---

## What this module is

| Document | Collection | What it holds |
|---|---|---|
| [`Room`](../../models/room/Room.java) | [`rooms`](../../models/room/Room.java) | One room: its name, kind, where it is, how many it seats, and whether it can be used |

**A room belongs to the school, not to a year**, so the paths carry no `{year}` and no gate 4 runs.
Timetable periods (`TimetableEntry.roomDocsId`) and exam papers (`ExamSchedule.rooms[].roomDocsId`)
point at rooms by id; adding a paper checks its rooms against this collection.

## Which gates run

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | #1 |
| **2** | Is the subscription usable | #1 |

**No gate runs on a read.**

---

# The endpoints

**What the marker in the `#` column means**, the same four words [`controllers/core`](../core/README.md)
uses:

| Marker | Meaning |
|---|---|
| **built** | It exists and answers. 3 of them. |
| **deferred** | Planned, not started, and not next. |
| **optional** | A convenience nobody is waiting on. |
| **not being built** | Decided against, struck through, and the reason is in its own section. |

Anything unmarked does not exist yet, and a request to it returns a 404.

## 1. Rooms · [Build order ↓](#build-order)

| # | Method and endpoint | What this API is for | Collections it touches |
|---|---|---|---|
| <a id="t1"></a>#1 — **built** | [`POST /schools/current/rooms`](#e1) | **Add a room** — name, type, location, seats. Starts `IN_USE`. | [`rooms`](../../models/room/Room.java) |
| <a id="t2"></a>#2 — **built** | [`GET /schools/current/rooms?resourceType=&status=&search=&minCapacity=`](#e2) | **The school's rooms**, filtered and paged. | [`rooms`](../../models/room/Room.java) |
| <a id="t3"></a>#3 — **built** | [`GET /schools/current/rooms/{roomDocsId}`](#e3) | **One room**, by its id. | [`rooms`](../../models/room/Room.java) |

**Not built, and not planned yet:** editing a room, and closing or re-opening one (`status`).
Today a room's status can only be changed in the database.

---

<a id="build-order"></a>
# Build order

| Phase | What it gives you | Endpoints | |
|---|---|---|---|
| **1** | Add a room, list them, read one | [#1](#t1), [#2](#t2), [#3](#t3) | built |

---

<a id="in-detail"></a>
<a id="the-endpoints-in-detail"></a>
# Appendix — what every API touches, field by field

<a id="e1"></a>
**[#1](#t1) · `POST /schools/current/rooms`** — built 2026-10-21

- [`rooms`](../../models/room/Room.java) — *reads*: `name` — must be free in the school, ignoring case, closed rooms included
- [`rooms`](../../models/room/Room.java) — *inserts*: `name`, `resourceType`, `location`, `capacity`, `remarks`, `status` = `IN_USE`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
POST /schools/current/rooms
Cookie:  idtoken=…   REQUIRED — names the school

{
  "name": "Main Hall",              // REQUIRED, max 120
  "resourceType": "HALL",           // REQUIRED, CLASSROOM | LABORATORY |
                                    //   HALL | LIBRARY | PLAYGROUND |
                                    //   OFFICE | OTHER
  "location": "Science Block, first floor",
                                    // optional, max 200
  "capacity": 200,                  // optional, at least 1
  "remarks": "…"                    // optional, max 500
}
</pre></td>
<td><pre>
201 Created — RoomResponse
Location: /schools/current/rooms/6aa1…

{
  "roomDocsId": "6aa1…",
  "name": "Main Hall",
  "resourceType": "HALL",
  "location": "Science Block, first floor",
  "capacity": 200,
  "status": "IN_USE",
  "remarks": "…",
  "createdAt": "2026-10-21T06:00:00Z",
  "updatedAt": "2026-10-21T06:00:00Z",
  "version": 0
}
</pre></td>
</tr>
</table>

**Every field on the request**

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `name` | **yes** | Max 120. "Room 12", "Main Hall". Unique in the school, ignoring upper and lower case. |
| `resourceType` | **yes** | `CLASSROOM`, `LABORATORY`, `HALL`, `LIBRARY`, `PLAYGROUND`, `OFFICE` or `OTHER`. |
| `location` | no | Max 200. Where it is, in words. |
| `capacity` | no | How many students it seats. At least 1 when sent. Exam papers are checked against it. |
| `remarks` | no | Max 500. Notes. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 409 | `ROOM_NAME_TAKEN` | The school has a room of that name, any case, closed too. |
| 400 | `INVALID_VALUE` | A `resourceType` that does not exist — lists the accepted ones. |
| 400 | `VALIDATION_FAILED` | No name or type, or capacity below 1. |
| 409 | `SCHOOL_NOT_EDITABLE` | The school is suspended or closed. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

`{ "name", "resourceType", "location",
"capacity", "remarks" }`.

- **The name is free in the school, ignoring case** — closed rooms keep theirs → `409
  ROOM_NAME_TAKEN`. There is no room code.
- `resourceType` is one of `CLASSROOM`, `LABORATORY`, `HALL`, `LIBRARY`, `PLAYGROUND`, `OFFICE`,
  `OTHER` → otherwise `400 INVALID_VALUE`, listing them.
- `capacity` is optional, at least 1 → `400 VALIDATION_FAILED`.
- Always starts `IN_USE`. `201` with a `Location` header.

<a id="e2"></a>
**[#2](#t2) · `GET /schools/current/rooms?resourceType=&status=&search=&minCapacity=`** — built 2026-10-21

- [`rooms`](../../models/room/Room.java) — *reads*: `resourceType`, `status`, `name`, `location`, `capacity` — the filters; the page is by `name`

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/rooms
    ?resourceType=HALL      // optional, a RoomType
    &status=IN_USE          // optional, IN_USE | UNDER_MAINTENANCE | CLOSED
    &search=hall            // optional, part of the name or location
    &minCapacity=60         // optional
    &page=0                 // optional
    &size=20                // optional
    &sort=name              // optional, name | capacity | createdAt |
                            //   updatedAt, each with ,desc
Cookie:  idtoken=…   REQUIRED — names the school
</pre></td>
<td><pre>
200 OK — PageResponse of RoomResponse

{
  "content": [
    {
      "roomDocsId": "6aa1…",
      "name": "Main Hall",
      "resourceType": "HALL",
      "location": "Science Block, first floor",
      "capacity": 200,
      "status": "IN_USE",
      "remarks": "…",
      "createdAt": "2026-10-21T06:00:00Z",
      "updatedAt": "2026-10-21T06:00:00Z",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
</pre></td>
</tr>
</table>

**Every field on the request** — all optional, all combined with AND.

| Field | Required | What it accepts, and what its absence means |
|---|---|---|
| `resourceType` | no | Only rooms of this kind, such as `HALL`. One that does not exist is a 400. |
| `status` | no | Only rooms in this state: `IN_USE`, `UNDER_MAINTENANCE` or `CLOSED`. |
| `search` | no | Part of the name or the location, ignoring upper and lower case. Blank is treated as absent. |
| `minCapacity` | no | Only rooms that seat at least this many. |
| `page`, `size` | no | Paging. |
| `sort` | no | `name`, `capacity`, `createdAt` or `updatedAt`, each with `,desc`. Absent: by `name`. |

**Refusals**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | A `resourceType` or `status` that does not exist. |
| 400 | `INVALID_PAGE` · `INVALID_PAGE_SIZE` · `INVALID_SORT_FIELD` | Bad paging or sort. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

filters `resourceType`, `status`, `search` (part of
the name **or** the location, ignoring case) and `minCapacity` ("which rooms can take 60"). Paged,
by `name` (unique, so paging is stable); `?sort=` accepts `name`, `capacity`, `createdAt`,
`updatedAt`. An unknown type or status is `400 VALIDATION_FAILED`.

<a id="e3"></a>
**[#3](#t3) · `GET /schools/current/rooms/{roomDocsId}`** — built 2026-10-21

- [`rooms`](../../models/room/Room.java) — *reads*: the whole room — it must be this school's

### Request and response

<table>
<tr><th align="left">Request</th><th align="left">Response body</th></tr>
<tr valign="top">
<td><pre>
GET /schools/current/rooms/{roomDocsId}
Cookie:  idtoken=…   REQUIRED — names the school

No body.
</pre></td>
<td><pre>
200 OK — RoomResponse

{
  "roomDocsId": "6aa1…",
  "name": "Main Hall",
  "resourceType": "HALL",
  "location": "Science Block, first floor",
  "capacity": 200,
  "status": "IN_USE",
  "remarks": "…",
  "createdAt": "2026-10-21T06:00:00Z",
  "updatedAt": "2026-10-21T06:00:00Z",
  "version": 0
}
</pre></td>
</tr>
</table>

**Refusals**

| Status | Code | When |
|---|---|---|
| 404 | `ROOM_NOT_FOUND` | No room of that id in this school. |
| 400 | `TENANT_NOT_RESOLVED` | No idtoken cookie. |

the room in full. Another school's
room is `404 ROOM_NOT_FOUND`.

**Measured 2026-10-21:** "Main Hall" (HALL, 200) and "Room 12" (CLASSROOM, 40) were added; "main
hall" again was `ROOM_NAME_TAKEN`; GARDEN was `INVALID_VALUE`; capacity 0 and no name were
`VALIDATION_FAILED`; every filter, sort and paging refusal and the 404 answered as documented.

---

# Where the code lives

```text
controllers/room/RoomController.java
services/room/RoomService.java
repositories/room/room/RoomRepository.java (+ Custom/Impl for #2)
dto/room/room/{request,response}/
```
