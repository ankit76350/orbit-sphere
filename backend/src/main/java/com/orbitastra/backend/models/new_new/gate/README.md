# gate — who is inside the school, and who may leave

One question shapes this whole package:

> **The fire alarm has gone off. Who is in the building?**

Everything here exists to answer that in seconds, for everybody — students, staff,
visitors and vehicles — and to make sure a child is never handed to the wrong
adult.

## Relationship overview

```text
Gate                      one way in or out
  ^
  |
GateMovement              one person, one direction, one moment   <-- the log
  ^        ^        ^
  |        |        |
  |        |        +--- IdCard            (documents) how they were identified
  |        |
  |        +------------ VisitorPass  ---> Visitor      (the person, reused)
  |                        one visit         encrypted contact + ID
  |
  +--------------------- StudentOutPass ---> Guardian
                           child leaving      must have pickupAuthorized
```

## The collections

| Collection | Purpose |
|---|---|
| `gates` | One way in or out. Two or three rows per school. |
| `visitors` | One person from outside, kept so they are not retyped every visit. |
| `visitor_passes` | One visit on one day. |
| `student_out_passes` | Permission for one child to leave during school hours. |
| `gate_movements` | The log. One row per person per direction per moment. |

## Person and visit are separate

The old sketch put both in one `Visitor` model. That means the courier who comes
three times a week gets retyped — name, phone, ID — on every trip.

So it splits:

- **`Visitor`** is the person. One row, reused forever, findable by phone number.
- **`VisitorPass`** is the visit. Hundreds of rows for that one courier.

A parent of a student here is normally **not** a `Visitor` at all. They have a
`Guardian` record and often an `IdCard`. This collection is for people with no
other place in the system.

## "Who is inside right now" is a single query

```text
db.visitor_passes.find({ schoolId, status: "CHECKED_IN" })
```

Every pass at `CHECKED_IN` is a person still in the building. That is why
`CHECKED_IN` and `CHECKED_OUT` are separate states rather than a `checkedOutAt`
being null — the state is indexed and the query is a match, not a null test.

For students and staff it comes from `gate_movements`: their latest row today,
where `direction = IN` and no later `OUT`.

A pass sitting at `CHECKED_IN` overnight is either somebody nobody checked out or a
guard who forgot. Either way it needs looking at, and it is visible rather than
buried.

## `StudentOutPass` is the serious one

Everything else here is about knowing who is in the building. This one is about
**handing a child to somebody**. Getting it wrong means a child leaves with the
wrong adult.

`collectedByGuardianDocsId` names who is coming. The service must check that
guardian has **`GuardianLink.pickupAuthorized = true`** for this child.

That flag already exists on the student's guardian list, which is why this package
has **no pickup-authorisation model of its own**. There is one place that answers
"may this adult take this child", and duplicating it would mean two answers that
drift apart — and the day they disagree is the day it matters.

### EXITED and RETURNED are separate on purpose

A child at `EXITED` is out of the school during school hours. If they were expected
back and the status has not moved to `RETURNED`, somebody has to find out why.

A single "gone" flag could not raise that. Two states plus `expectedReturnAt` make
a list of children who are late back, which is a list a school actually wants.

### `emergency` buys speed and leaves a trace

A real emergency will not wait for an approval queue, so `emergency = true` lets a
pass be created and used at once.

It is recorded rather than silent because **skipping the check has to leave a
trace, or it stops being an exception.** A school can look back at every time the
normal route was bypassed, and if that list is long, the normal route is too slow
and needs fixing.

## `verificationMethod` says what a row is worth

Not all records are equally trustworthy:

- `RFID_TAP` / `ID_CARD_SCAN` — happened at a moment the system saw for itself.
- `MANUAL` — a guard typed a name. Fine, but it is somebody's word.

When a parent argues about what time their child left, this field is the difference
between evidence and a recollection. `Gate.hasCardReader` tells you which gates can
only ever produce `MANUAL` rows.

Same reasoning as `BoardingCaptureMethod` on a transport boarding record.

## This is not attendance

A child scanning in at the gate has arrived at the **school**. Whether they were in
the **classroom** is a different question with a different answer, and it belongs
to the attendance models in `academics`.

The gate log may *feed* attendance. It must never quietly *become* it — a child can
be inside the building and still absent from a lesson, and a school that marks
attendance from the gate will insist a truanting child was present.

## The log is append-only

`gate_movements` rows are added and never changed. A record of who was where at
what time is worth nothing if it can be tidied up afterwards.

A wrong entry is corrected by **adding the right row** and explaining it in
`remarks`. Same rule as `FeeInvoice` and the wallet ledger.

`movementDate` repeats the date part of `occurredAt` on purpose. The gate screen
runs "today's movements" all day long, and a plain equality match on a date beats a
range scan on a timestamp.

## One log for everybody

Students, staff, visitors and vehicles all go in `gate_movements`, told apart by
`subjectType`.

Separate logs per kind of person would make the one question that matters
impossible: *who is inside right now* needs everybody in one place, ordered by
time. Four logs would mean four queries and a merge, every time.

## QR codes follow the ID card rule, not the certificate rule

`VisitorPass.scanPayload` is a **meaningless random token** — never a web address,
never the pass number. A badge dropped in a car park tells a stranger nothing, and
only a signed-in staff member can turn the token into a name.

This is the same rule as `IdCard.scanPayload` and the **opposite** of
`IssuedDocument.scanPayload`, where a public page is the whole point because an
employer verifying a certificate has no school login.

As everywhere: the QR picture is drawn from the string and never stored.

## Visitor contact details are encrypted

`Visitor` holds a phone number and often an ID document number. Both use the
three-field pattern from `BankAccount`:

| Field | Job |
|---|---|
| `encryptedContactNumber` | the real value, encrypted |
| `contactLookupHash` | find the same person again without decrypting |
| `maskedContactNumber` | the only version safe to show |

The masked one matters more here than anywhere else in the system, because a gate
screen sits in a doorway where anybody walking past can read it.

## `blocked` is the field with teeth

Somebody told not to come back must be stopped at the gate. A guard cannot be
expected to remember a name from a note passed round months ago.

A blocked visitor is refused a new pass, and `blockReason` tells the guard what to
say. It also records who blocked them and when, because barring somebody from a
school is a decision that needs an owner.

## Added to NumberSequenceType

- `STUDENT_OUT_PASS` — new, for `StudentOutPass.outPassNo`
- `VISITOR_PASS` — was already there, unused. `VisitorPass.passNo` is what it was
  reserved for.

## Deliberately left out

- **Organised daily dismissal.** The `a_new` sketch had a whole `dismissal` package
  — dismissal plans, queues, pickup lanes. That is a scheduled daily operation for
  large schools with car queues, and it is a different problem from a one-off early
  exit. Design it separately if a school asks.
- **Face recognition devices.** `VerificationMethod.FACE_RECOGNITION` is a value
  with nothing behind it. Modelling cameras, enrolled faces and match confidence is
  its own package, and it carries privacy obligations that deserve a deliberate
  decision rather than a field.
- **CCTV.** `a_latter/security` has cameras and recordings. Watching the gate is a
  different job from recording who came through it.
- **Staff attendance from gate scans.** Tempting, and wrong for the same reason as
  student attendance. A teacher in the building is not necessarily teaching.
- **Vehicle gate passes for parents' own cars.** Nobody has asked, and it would
  need a vehicle register for non-school vehicles.

## Rules the services must enforce

**Gates**

1. A movement may only name an `active` gate.
2. A visitor may only be checked in at a gate with `visitorsAllowed = true`.

**Visitors and passes**

3. A `blocked` visitor is never issued a pass.
4. `hostStaffDocsId` must be a current member of staff. A visitor nobody is
   expecting is a visitor who talked their way in.
5. A pass is never checked out before it is checked in.
6. `scanPayload` is a random token — never a web address, the pass number, or any
   personal detail.
7. Resolving a `scanPayload` to a person requires a signed-in staff member.
8. The masked contact and ID values carry no more than the last few characters.

**Out passes**

9. `collectedByGuardianDocsId` must be a guardian of this student with
   `GuardianLink.pickupAuthorized = true`. This is the check that matters most in
   the package.
10. The approver is a member of staff and is not the person who asked.
11. A rejection carries a reason.
12. An `emergency` pass still names a real authorised collector. Emergency skips
    the queue, never the identity check.
13. A child is never released on a pass whose `passDate` is not today.
14. Children at `EXITED` past their `expectedReturnAt` appear on a daily
    exceptions list.

**The movement log**

15. Rows are never edited or deleted. A wrong row is corrected by adding the right
    one with an explanation in `remarks`.
16. A student going `OUT` during school hours without a `studentOutPassDocsId` is
    written with an `exceptionCode`, not refused silently.
17. Attendance is never derived from gate movements without an explicit decision to
    do so. Being in the building is not being in class.
18. Non-working days come from `AcademicYear.holidays`; no weekday is assumed to be
    a day off.
