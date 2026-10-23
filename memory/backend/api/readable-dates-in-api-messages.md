# Dates in API messages are always readable

**Project-wide convention (set 2026-09-09):** every date or time that appears in a **message a
person reads** goes through
[`common/time/Dates.java`](../../../backend/src/main/java/com/orbitastra/backend/common/time/Dates.java).
Never concatenate an `Instant`, `LocalDate` or `LocalDateTime` into a string.

## The rule

```java
// wrong — prints the ISO form, which nobody reads
throw ApiException.conflict("PERIOD_NOT_ENDED",
        subscription.getSubscriptionNo() + " runs to " + previousPeriodEnd
                + ", which has not passed yet.");

// right
throw ApiException.conflict("PERIOD_NOT_ENDED",
        subscription.getSubscriptionNo() + " runs to "
                + Dates.readable(previousPeriodEnd, zone)
                + ", which has not passed yet.");
```

```
2027-10-08T23:59:59Z  in UTC  ->  Friday 8 October 2027 11:59PM
```

(The same instant in `Asia/Kolkata` is *Saturday 9 October 2027 5:29AM* — see the zone note below.)

The format is `EEEE d MMMM yyyy h:mma` — weekday, day, month, year, then the time.

## It applies to messages, not to fields

| Where | Form |
|---|---|
| `message`, `note`, `reason`, `nextStep` — anything a human reads | spelled out, via `Dates` |
| Response **fields** — `currentPeriodStart`, `createdAt`, `effectiveFrom` | **ISO-8601 instants, unchanged** |
| README and documented examples | spelled out, matching what the API really returns |

A field is parsed by a program, so it stays a machine date. Rendering a field as
`"Friday 8 October 2027"` would break every caller. The rule is about prose only.

## Which overload

```java
Dates.readable(instant, zone)   // a school owns the date  -> the school's own calendar
Dates.readable(instant)         // the platform owns it    -> UTC
Dates.readable(localDate)       // already a day           -> no time invented
```

**Pass the school's zone whenever a school is in hand** — `school.getDefaultTimeZone()`. The zone
is not cosmetic, it decides the calendar day:

```
2026-09-07T18:30:00Z  in UTC          -> Monday 7 September 2026 6:30PM
2026-09-07T18:30:00Z  in Asia/Kolkata -> Tuesday 8 September 2026 12:00AM   <- what the school means
```

A billing period that starts at midnight in Kolkata is stored 5½ hours earlier. Rendered in UTC it
names the day before, and telling a school its period began on the 7th when its own calendar says
the 8th is worse than being unreadable.

The no-zone form is for dates no school owns — a plan's selling window on the platform catalogue.

## Three things baked into the helper, so no caller repeats them

- **The time is always shown**, even when it looks redundant. Messages that compare two dates
  ("must be after", "is before the start of today") can have both ends on the same day, and
  *"(8 September 2026) must be after (8 September 2026)"* reads as a contradiction.
- **`Locale.ENGLISH` is pinned**, not left to the JVM. `en_IN` renders `10:01pm` in lower case and
  a JVM started elsewhere would render the month in another language. An API message is part of
  the contract, so it cannot depend on how the process was launched.
- **A null date reads `(not set)`**, not the literal word `null`. Several of these values are
  genuinely nullable — a subscription mid-migration with no period end, a plan with no closing
  date.

## Where the date logic lives, not just the formatting

`Dates.startOfTodayIn(zone)` is in the same class on purpose. "Today" is not a fixed instant —
midnight in Asia/Kolkata is 18:30Z the evening before, so "is this date in the past" answered in
UTC would call a school's own today yesterday. Keeping it beside `readable` means a rendered date
and a date comparison can never disagree about the zone.

## Where it stands

**72 call sites** across 9 service, utils and helper files; 22 of them pass a school zone.
Every service that builds a message is covered:

```
services/core/AcademicYearService, core/utils/AcademicYearServiceUtils, core/helper/CoreHelper
services/plans/PlanDefinitionService, plans/PlatformSubscriptionService
services/plans/utils/{PlanDefinition,PlatformSubscription,SchoolSubscription}ServiceUtils
services/plans/helper/PlansHelper
```

## The guard, and why it needs to be statement-based

`new-api-tester/smoke-test.mjs` fails the build on a raw date. Getting that detector right took
three attempts — a line-based, getter-only version missed:

- date-typed **locals** (most of these dates are locals, not getters)
- a value **wrapped alone onto a continuation line**, with no quote on it
- a string **opening a ternary branch**, with no `+` in front of it

Rebuilt around `;`-delimited statements with the whitespace flattened, it found **eighteen** live
sites the line version had missed, and four more after that. `validateSellingWindow` in
`PlansHelper` was still concatenating raw instants long after the first sweep looked finished.

So: when adding a message with a date, do not rely on remembering. Run the suite.
