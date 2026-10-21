# room — the school's rooms

## What this is

One collection, **`rooms`**: the rooms and spaces of a school — classrooms, labs, the hall, the
library, the playground.

It exists for one reason: so a timetable period and an exam paper can say **where** they happen,
and so two papers are never put in the same room at the same time.

| Model | What it is |
|---|---|
| [Room](Room.java) | One room: its name, kind, where it is, how many it seats, and whether it can be used |
| [RoomType](enums/RoomType.java) | `CLASSROOM`, `LABORATORY`, `HALL`, `LIBRARY`, `PLAYGROUND`, `OFFICE`, `OTHER` |
| [RoomStatus](enums/RoomStatus.java) | `IN_USE`, `UNDER_MAINTENANCE`, `CLOSED` |

## Who points here

| Field | Lives in |
|---|---|
| `TimetableEntry.roomDocsId` | [`academics/timetable`](../academics/timetable/embedded/TimetableEntry.java) — the room a period is in |
| `ExamSchedule.roomDocsId` | [`academics/examination`](../academics/examination/ExamSchedule.java) — the room a paper is written in |

## Rules

- **`name` is the room's only key** — unique in the school, ignoring case. There is no room code.
- **A room is never deleted.** One that is no longer used is `CLOSED`, because old timetables and
  papers still point at it. Only an `IN_USE` room can be given to a period or a paper.

## History

- **2026-10-21 — renamed.** This was the `facilities` package, and the model was
  `FacilityResource` in the `facility_resources` collection, linked by `facilityResourceDocsId`.
  It is now `Room`, `rooms` and `roomDocsId` — the word everyone in a school already uses. The
  stored timetable entries and exam papers were migrated (the field renamed in place).
- **2026-10-21 — minimised.** The package used to also model room bookings, an asset register,
  maintenance plans, repair work orders and safety inspections — five more models, two embedded
  classes and nine enums, plus four number-sequence types. Nothing in the project used any of
  them, so they were removed. If a school later needs one, it is designed then, on its own.

No API exists for rooms yet. [`RoomRepository`](../../repositories/room/room/RoomRepository.java)
has one lookup, used by exam papers to check a room.
