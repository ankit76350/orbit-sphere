# hostel — who sleeps where, and who is missing tonight

Two questions, and the second one is why this package is the most safety-critical after
`gate`:

1. Which child is in which bed, and what does the family pay for it?
2. **At tonight's roll call, is every child where the school thinks they are?**

## Relationship overview

```text
HostelBuilding          the building, and the warden responsible for it
   +--> HostelRoom      the room, and how many beds it is built for
          +--> HostelBed    one bed  (status only — never who is in it)
                   ^
                   |
          HostelAllocation      one child, one bed, one year, and the money
                   |              ../finance/config/FeeHead.java
                   |              ../finance/billing/FeeInvoice.java
                   |
                   +--> HostelLeaveRequest     going home
                   |       +--> StudentOutPass    ../gate/StudentOutPass.java
                   |       +--> GuardianInformed  ../common/embedded/GuardianInformed.java
                   |
                   +--> HostelRollCall[]       the nightly headcount
                           +--> ClinicVisit       ../health/ClinicVisit.java
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [Student](../student/Student.java) | `student` | the boarder |
| [Guardian](../student/Guardian.java) | `student` | who collects a child on leave |
| [GuardianLink](../student/embedded/GuardianLink.java) | `student/embedded` | `pickupAuthorized` — may they collect? |
| [Staff](../people/staff/Staff.java) | `people/staff` | warden, and who took the roll call |
| [FeeHead](../finance/config/FeeHead.java) | `finance/config` | where hostel, mess and deposit charges land |
| [FeeCategory](../finance/enums/FeeCategory.java) | `finance/enums` | `HOSTEL`, `MESS`, `DEPOSIT` |
| [FeeInvoice](../finance/billing/FeeInvoice.java) | `finance/billing` | the bill those charges appear on |
| [StudentOutPass](../gate/StudentOutPass.java) | `gate` | the same departure, seen at the gate |
| [ClinicVisit](../health/ClinicVisit.java) | `health` | where a child is when they are in the clinic |
| [GuardianInformed](../common/embedded/GuardianInformed.java) | `common/embedded` | proving the family was told |
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | the boarding consent form |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `HOSTEL` permission |
| [NumberSequenceType](../institution/enums/NumberSequenceType.java) | `institution/enums` | `HOSTEL_ALLOCATION`, `HOSTEL_LEAVE_REQUEST` |

Named only as precedent: [TransportAllocation](../transport/TransportAllocation.java).

## The collections

| Collection | Purpose |
|---|---|
| `hostel_buildings` | One building, and the warden responsible for it. |
| `hostel_rooms` | One room, and how many beds it is built for. |
| `hostel_beds` | One bed. Status only. |
| `hostel_allocations` | One child's bed for a year, and what the family pays. |
| `hostel_leave_requests` | A boarder going home and coming back. |
| `hostel_roll_calls` | One child, at one headcount, on one night. |

## The bed does not know who is in it

The reference sketch put `studentDocsId` on `HostelBed` **and** on the stay record. That
is one fact in two places, and two places can disagree — usually after somebody moves
rooms at nine at night.

So `HostelBed` carries `status` only. **Occupancy lives on `HostelAllocation`**, and the
bed's status is kept in step with it in the same operation.

Status stays on the bed because a warden looking for space needs it in one read, without
walking every allocation in the building.

## Bed-level, not room-level

Allocation names an exact bed. "Somewhere in room 204" is not an answer at two in the
morning, and both a roll call and a fire register need a precise one.

## Three charges, all copied at allocation time

`HostelAllocation` follows exactly the shape `TransportAllocation` set — amounts copied
on, never rewritten, with a fee head saying where each lands:

| Field | Category | Nature |
|---|---|---|
| `monthlyHostelFee` | `HOSTEL` | the bed |
| `monthlyMessFee` | `MESS` | the food |
| `securityDepositAmount` | `DEPOSIT` | **money held, not money earned** |

The deposit is the one worth noticing. It is money the school is **holding**, not money
it has earned, which is why it has a category of its own. `FeeCategory.DEPOSIT` had
nothing feeding it until now.

**Giving it back is not recorded here.** A refund is money going out, and finance already
models that as a [RefundTransaction](../finance/billing/RefundTransaction.java) against
the invoice that charged the deposit. An earlier version of this model kept the refunded
amount, the date and the deduction reason as well; those were dropped on 2026-08-19,
because they were a second copy of one money movement and the hostel's copy is the one
nobody would update. Same reason a library fine does not record whether it was paid, and
a conduct fine does not either.

**Mess charges ride on this record** rather than a separate subscription, because at a
boarding school everybody with a bed eats. A day scholar who wants to eat in the mess is
not modelled — see below.

## A weekend at home is not a change of residence

A child away for two nights stays `ACTIVE`. Going home is a `HostelLeaveRequest`, not a
suspension of the allocation.

Treating it as one would take them **off the roll call they most need to be on** — the
roll call builds its rows from ACTIVE allocations, and a child marked as not living here
would simply not be counted.

`SUSPENDED` is for a long absence, such as a term away. Not billed, not counted.

## The roll call is built backwards, on purpose

Rows are created **in advance**, one per ACTIVE allocation, all at `UNACCOUNTED`.

This is the opposite of how it feels natural to build it. If rows were written when a
child answered, then a child who never answered would leave **no row at all** — and the
list of children nobody has seen would be empty. Which is precisely the list the whole
exercise exists to produce.

So the warden's job is to turn `UNACCOUNTED` rows into something else. Anything still
`UNACCOUNTED` when the count closes is a child nobody can find.

Same reasoning as `TransportBoardingRecord` creating rows at `EXPECTED`, and for the
same reason: absence has to be visible, and a missing row is invisible.

### Four ways of not being present

`ON_APPROVED_LEAVE`, `IN_CLINIC` and `EXCUSED` are all kept apart from `UNACCOUNTED`.

A child who is somewhere the school **put** them is not missing. Lumping the four
together is how a genuinely missing child disappears into a column of absences — so a
child on approved leave is pre-marked from their leave request rather than left to be
chased, and a child in the clinic links to their `ClinicVisit`.

## Leave follows the gate's rule on who may collect

`collectedByGuardianDocsId` must be a guardian with `GuardianLink.pickupAuthorized`.

That check lives in **one** place — the student's guardian list — and this package
enforces it rather than keeping its own list of who may collect whom. Exactly as
`StudentOutPass` does.

`studentOutPassDocsId` links the two records: leaving the hostel and leaving the campus
are one journey seen from two places, and joining them means the warden's record and the
gate's cannot disagree about when the child actually left.

`emergencyContactDuringLeave` is separate because a child at their grandmother's is not
reachable on the number the school usually rings — and the one time that matters is the
one time nobody thought to ask.

## Consent is recorded in one place

`guardianConsentDocsId` here is the family agreeing their child boards at the school, and it
points at [`GuardianConsent`](../compliance/GuardianConsent.java).

This was **fixed on 2026-08-20.** It used to be one of seven places recording that a guardian
agreed to something — this field, a boolean and a date on `HealthProfile`, a field and a second
boolean on `MedicationAdministration`, a field and a boolean on `StudentRecognition`, a field on
`SupportPlan`, and the real model in `compliance`.

`guardianConsentDocsId` now points at [`GuardianConsent`](../compliance/GuardianConsent.java),
which is the only place consent is recorded. The scanned agreement hangs off the consent rather
than off this row, and a withdrawal is visible here the moment it happens — which it was not
when this was a link to a document that could not be withdrawn.

It is `RECORD_SPECIFIC` with purpose `HOSTEL_RESIDENCE`, because agreeing to board is for a
stated period and next year is a new agreement.

## Deliberately left out

- **Room and mess inspection rounds.** Real, and closer to facilities than to residence.
- **Visitor rules for boarders' parents.** `gate` already models visitors; a parent
  visiting a boarder is a `VisitorPass`, not a hostel record.
- **Bed-linen, laundry and tuck-shop accounts.** Each is a small ledger of its own and
  none is core to knowing where a child sleeps.
- **Day scholars eating in the mess.** Mess charges ride on a hostel allocation, so a day
  scholar has nowhere to be charged from. Real at some schools; it needs a mess
  subscription of its own, and nobody has asked yet.
- **Telling a family their child is late back.** `guardiansInformed` records that
  somebody told them. Sending the message is `notification`, designed last.

## Rules the services must enforce

**Place**

1. Beds in a room may not exceed the room's `capacity`.
2. A bed with an `ACTIVE` allocation is never `WITHDRAWN`.
3. A building's `hostelType` is checked before any allocation. A girl is never allocated
   into a boys' building.

**Allocation**

4. Only an `AVAILABLE` bed may be allocated, and allocating sets it to `OCCUPIED` in the
   same operation.
5. One `ACTIVE` allocation per student per year, and one per bed.
6. Fee amounts are copied at allocation time and never rewritten in place.
7. The three fee heads carry `HOSTEL`, `MESS` and `DEPOSIT` respectively.
8. The deposit refund is never more than the amount taken.

**Leave**

9. `collectedByGuardianDocsId` must be a guardian of this child with
   `pickupAuthorized = true`.
10. The approver is the warden or a senior member of staff, and is not the person who
    asked.
11. A rejection carries a reason.
12. A child on `APPROVED` leave is pre-marked `ON_APPROVED_LEAVE` at roll call.
13. Children past `expectedReturnAt` move to `OVERDUE` and appear on a warden's list.

**Roll call**

14. Rows are created in advance for every `ACTIVE` allocation, at `UNACCOUNTED`.
15. A roll call cannot be closed while any row is `UNACCOUNTED` unless a warden has
    recorded what is being done about it.
16. Rows are never edited to tidy up. A child found later gets a status and a time, so
    how long they were unaccounted for stays on the record.
17. Non-working days come from `AcademicYear.holidays`; boarders are in residence on
    many of them, so no day is assumed to have no roll call.
