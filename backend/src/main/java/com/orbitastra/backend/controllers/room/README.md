# controllers/room — API plan

**Three of three are built — [#1](#e1), [#2](#e2) and [#3](#e3), on 2026-10-21.** The school's
rooms: add one, list them, read one. Mirrors [`models/room`](../../models/room/README.md) — one
collection, [`rooms`](../../models/room/Room.java).

**A room belongs to the school, not to a year**, so the paths carry no `{year}` and no gate 4 runs.
Timetable periods (`TimetableEntry.roomDocsId`) and exam papers (`ExamSchedule.rooms[].roomDocsId`)
point at rooms by id; adding a paper checks its rooms against this collection.

## Which gates run

| Gate | Asks | Runs on |
|---|---|---|
| **1** | Is the school ACTIVE | #1 |
| **2** | Is the subscription usable | #1 |

**No gate runs on a read.**

## The endpoints

| # | Method and endpoint | What it is for | Collections |
|---|---|---|---|
| <a id="t1"></a>1 — **built** | [`POST /schools/current/rooms`](#e1) | **Add a room** — name, type, location, seats. Starts `IN_USE`. | `rooms` |
| <a id="t2"></a>2 — **built** | [`GET /schools/current/rooms?resourceType=&status=&search=&minCapacity=`](#e2) | **The school's rooms**, filtered and paged. | `rooms` |
| <a id="t3"></a>3 — **built** | [`GET /schools/current/rooms/{roomDocsId}`](#e3) | **One room**, by its id. | `rooms` |

**Not built, and not planned yet:** editing a room, and closing or re-opening one (`status`).
Today a room's status can only be changed in the database.

## In detail

<a id="e1"></a>
**[1](#t1) · `POST /rooms`** — built 2026-10-21 — `{ "name", "resourceType", "location",
"capacity", "remarks" }`.

- **The name is free in the school, ignoring case** — closed rooms keep theirs → `409
  ROOM_NAME_TAKEN`. There is no room code.
- `resourceType` is one of `CLASSROOM`, `LABORATORY`, `HALL`, `LIBRARY`, `PLAYGROUND`, `OFFICE`,
  `OTHER` → otherwise `400 INVALID_VALUE`, listing them.
- `capacity` is optional, at least 1 → `400 VALIDATION_FAILED`.
- Always starts `IN_USE`. `201` with a `Location` header.

<a id="e2"></a>
**[2](#t2) · `GET /rooms`** — built 2026-10-21 — filters `resourceType`, `status`, `search` (part of
the name **or** the location, ignoring case) and `minCapacity` ("which rooms can take 60"). Paged,
by `name` (unique, so paging is stable); `?sort=` accepts `name`, `capacity`, `createdAt`,
`updatedAt`. An unknown type or status is `400 VALIDATION_FAILED`.

<a id="e3"></a>
**[3](#t3) · `GET /rooms/{roomDocsId}`** — built 2026-10-21 — the room in full. Another school's
room is `404 ROOM_NOT_FOUND`.

**Measured 2026-10-21:** "Main Hall" (HALL, 200) and "Room 12" (CLASSROOM, 40) were added; "main
hall" again was `ROOM_NAME_TAKEN`; GARDEN was `INVALID_VALUE`; capacity 0 and no name were
`VALIDATION_FAILED`; every filter, sort and paging refusal and the 404 answered as documented.

## Where the code lives

```text
controllers/room/RoomController.java
services/room/RoomService.java
repositories/room/room/RoomRepository.java (+ Custom/Impl for #2)
dto/room/room/{request,response}/
```
