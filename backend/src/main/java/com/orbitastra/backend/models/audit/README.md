# audit — who did what, and when

One collection. It answers a question no other model can:

> **Somebody changed this. Who, when, and what did they change it from?**

## Relationship overview

```text
AuditEvent                         append-only. one row per action.
  |
  +--> AuditFieldChange[]          field-by-field: from → to, or redacted
  |
  +--> actorDocsId        ------>  Staff / Guardian / Student  (or null for SYSTEM)
  +--> authSessionDocsId  ------>  ../identity/AuthSession.java
  +--> targetCollection
       + targetDocsId     ------>  any document in any collection
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [AuthSession](../identity/AuthSession.java) | `identity` | tying a run of events to one device |
| [Staff](../people/staff/Staff.java) | `people/staff` | the actor, when `actorType` is `STAFF` |
| [Guardian](../student/Guardian.java) | `student` | the actor, from the parent portal |
| [Student](../student/Student.java) | `student` | the actor, or the target |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `AUDIT` permission, and `moduleCode` |
| [SchoolBase](../base/SchoolBase.java) | `base` | tenancy, and the lifecycle fields this model must not use |

## Why it exists when every model already has `createdByDocsId`

`AuditedDocument` gives every record `createdAt`, `createdByDocsId`, `updatedAt`,
`updatedByDocsId`. That answers *"who touched this last."*

It cannot answer:

- who touched it **before** that — there is only one slot, and each change overwrites it
- **what** they changed — the old value is gone
- who **looked** at it without changing anything
- who **tried** and was refused
- what happened to a record that has since been **deleted**

Every one of those is what somebody actually asks. Hence a separate, append-only trail.

## Append-only, and why the inherited fields are a trap

`SchoolBase` brings `recordState`, `deletedAt`, `archivedAt`, `updatedAt` and `version`.
**None of them may ever be written on an audit event.** Setting `recordState = DELETED` would
hide evidence, which is the one thing this collection exists to prevent.

The model can't stop that structurally — it inherits the fields. So it is enforced elsewhere,
and the javadoc says how: an **insert-only repository interface** and an **insert-only
database role**, so a future service or a script cannot rewrite history even by accident.

**No TTL index on this collection, ever.** A TTL index deletes silently and leaves nothing to
say it happened. Ageing rows out is an evidenced archival process, and `retainLongTerm` marks
the rows that must survive it.

## `occurredAt` is not `createdAt`

- **`occurredAt`** — when the action happened. Every index orders by this.
- **`createdAt`** (inherited) — when the row was written.

Identical for a synchronous write. They diverge the moment events are queued through an
outbox, and then only `occurredAt` tells the truth about the order things happened in.

## `eventType` is an enum; `action` is text

Deliberately mixed, and it is the one place in this codebase where free text on a "type" field
is the right answer.

- **`eventType`** — a short, stable list, for grouping and retention decisions.
- **`action`** — the specific operation: `MARKS_PUBLISHED`, `PAYROLL_APPROVED`,
  `CONCESSION_REJECTED`. There are hundreds and every new workflow adds one.

Making `action` an enum would mean a schema change for every new feature, which is the
opposite of what an audit trail needs. Elsewhere I have argued *against* free-text type fields
— `partyType`, `exceptionCode` — and the difference is that those had **closed** sets of
maybe ten values. This one is open by nature.

**The cost is real and needs a guard:** nothing stops `MARKS_PUBLISHED` and `PUBLISH_MARKS`
both existing. Verb-last screaming snake case, defined as constants in one place, checked in
review. Without that discipline the `action` index becomes useless.

## Snapshots, because the trail outlives what it describes

`actorDisplayName`, `actorRoleCode`, `targetLabel` and `moduleCode` are copied in.

A staff member leaves and their record is archived. Two years later somebody reads the entry
where that person published a set of marks. **With only ids, the row reads as
`67aa15d9… changed 67aa15d9…`** — technically complete and humanly useless.

`actorRoleCode` is specifically the role they were acting under *at the time*, not their
current one. Somebody who was an exam controller in 2026 and is a class teacher now did that
action as an exam controller, and the trail has to say so.

## `AuditFieldChange.redacted` is the field that keeps this collection safe

Auditing a change to a salary, an Aadhaar number or a password hash must **not** copy the
value into a second collection that more people can read than the original.

When `redacted` is true, both value fields stay null and the row records only that the field
changed.

**An audit trail that leaks what it was auditing is worse than no audit trail**, because the
leak is now somewhere nobody thought to protect. `payroll` and `compliance` in particular must
always redact.

## `authSessionDocsId` — what it adds over an IP

`ipAddress` and `userAgent` describe a request. The session ties a **run** of events together.

*"Somebody signed in from an unfamiliar phone at 11pm and then changed twelve marks"* is two
unconnected facts without it, and one story with it. That is the shape almost every real
investigation takes.

## `DENIED` is not `FAILURE`

- **`DENIED`** — the system working. Somebody asked for something they may not have.
- **`FAILURE`** — the system not working. They were allowed and it broke.

Counting them together hides both. A run of `DENIED` events in one afternoon is the single
clearest signal this system can give that something is wrong, which is why there is an index
on `outcome`.

## `READ` events: selective, never universal

Logging every list screen would bury the collection and make it unreadable — the useful rows
would be one in ten thousand.

But somebody opening a child's **medical record**, a colleague's **payslip**, or a student's
**Aadhaar** is often the only thing worth auditing at all, because nothing changed and no
other trace exists.

So `READ` is recorded for a named list of places, decided deliberately, not by default.

## Reading the trail is its own permission

`AppModule.AUDIT`, and a narrow one. Somebody who can see who did what can see a great deal
about everybody — and **the people who most need watching are often the ones with the widest
access.**

## Deliberately left out

- **Full before-and-after document snapshots.** Field-level changes are more useful and
  vastly smaller. A whole-document copy of every version turns this into a second database and
  makes the redaction problem far worse.
- **Retention and archival rules as models.** `retainLongTerm` marks what must be kept; how
  long and where it goes is configuration and an archival job, and it belongs with a
  privacy/retention module if one is ever built.
- **Tamper-evident chaining.** Hashing each row with the previous one so a deletion is
  detectable. Genuinely stronger than an insert-only role, and genuinely more work — worth it
  only if somebody asks for it.
- **Alerting on a run of `DENIED` events.** The condition is queryable. Sending the message is
  `notification`, designed last.
- **A separate login-history collection.** `AUTHENTICATION` events belong here; a second log
  would fragment the timeline this model exists to make whole.

## Rules the services must enforce

**Writing**

1. Insert only. No update, no delete, no soft delete, through an insert-only repository and an
   insert-only database role.
2. `recordState` stays `ACTIVE`. `deletedAt`, `archivedAt`, `updatedAt`, `updatedByDocsId`
   and `version` are never written.
3. No TTL index on this collection, ever.
4. A write that fails must not fail the business operation it was recording, and must not be
   silently swallowed either. It goes to an outbox and is retried.

**Content**

5. `actorDocsId` is required unless `actorType` is `SYSTEM`.
6. If `targetDocsId` is set, `targetCollection` must be too. An id with no collection cannot
   be interpreted.
7. `actorDisplayName`, `actorRoleCode`, `targetLabel` and `moduleCode` are snapshotted at
   write time and never resolved live.
8. `reason` is required for every `DENIED` outcome and for the operations policy names as
   sensitive.
9. `failureReason` never contains a stack trace or anything the caller submitted.
10. A field whose value is sensitive is written with `redacted = true` and both values null.
    Never the value itself.
11. Fields that carry no meaning to a reader — `updatedAt`, `version` — are left out of
    `changes` rather than filling every row.

**Reading**

12. Reading requires the `AUDIT` module. It is never bundled with a module's own permission.
13. A `READ` event is recorded only for the places policy names. Never for every list screen.
14. `occurredAt` is the field every query and report orders by, never `createdAt`.
