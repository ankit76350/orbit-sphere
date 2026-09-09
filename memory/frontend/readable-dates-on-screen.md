# Dates on screen are always readable, with the time

**Project-wide convention (set 2026-09-09):** anywhere the frontend shows a date to a person, it
shows a **human-readable date with the time on a 12-hour clock and AM/PM**. Never print a raw
ISO-8601 instant on screen.

## The rule

```
wrong   2027-10-08T23:59:59Z
right   Friday 8 October 2027 11:59 PM
```

The time is always shown, not just the day — the same reason as the backend rule: two of these
dates get compared against each other, and both ends can fall on the same day.

This is the screen half of
[Dates in API messages are always readable](../backend/api/readable-dates-in-api-messages.md).
The backend already spells out every date inside a `message`; a screen that prints the raw
`currentPeriodEnd` field beside that message undoes it.

## It applies to what is displayed, not to what is sent

| Where | Form |
|---|---|
| Any date rendered into the page — a row, a hint, a note, a heading | spelled out, 12-hour, AM/PM |
| Query parameters and request bodies | **ISO-8601 instants, unchanged** |
| `<input type="date">` values | `yyyy-MM-dd`, which is what the element requires |

The wire format is not negotiable — the API parses it. `src/lib/dates.js` already keeps that side
right (`startOfDay`, `endOfDay`, `startOfDayInZone`); this rule is only about the text a person
reads.

## Pin the locale, do not take the browser's

```js
// wrong: the output changes with the viewer's machine
date.toLocaleString(undefined, { ... })

// right: one format, everywhere
new Intl.DateTimeFormat('en-GB', { ... })
```

Same reasoning as `Locale.ENGLISH` on the backend. A screenshot in a bug report has to mean the
same thing as what the next person sees, and `undefined` makes the format a property of whoever
happened to open the page.

## The helper

[`new-api-tester/src/lib/dates.js`](../../new-api-tester/src/lib/dates.js) — `readableDateTime(instant)`:

```
2027-10-08T23:59:59Z      ->  Friday 8 October 2027 11:59 PM
2026-09-10T00:00:00Z      ->  Thursday 10 September 2026 12:00 AM
2026-09-09T06:45:09.751Z  ->  Wednesday 9 September 2026 6:45 AM
null                      ->  —
"nonsense"                ->  "nonsense"   (returned as given, not swallowed)
```

Built with `Intl.DateTimeFormat('en-GB', …).formatToParts` and reassembled by hand, so the shape
is ours rather than the formatter's: no comma after the weekday, AM/PM upper case, one space
before it.

## `readableInstant` stays, and that is not a loophole

The same module keeps `readableInstant`, which returns the exact stored instant. **This is an API
testing tool**, and next to a date picker that can only show a day, the thing somebody needs to
see is the precise value on the wire — seconds, milliseconds and the `Z`. So:

| Function | For |
|---|---|
| `readableDateTime` | anything presented as **information** — a row, a note, a heading |
| `readableInstant` | **"stored as …" hints** beside an input, where the wire value is the point |

Both are documented in the module with that split spelled out, so the next person does not "fix"
one into the other.

## Where it stands

- **Done** — the `#29` audit trail renders both its dates through it, and `#28`'s subscription
  rows do too. Those two tables sit on one screen, so they had to agree.
- **Guarded** — eight checks in `new-api-tester/smoke-test.mjs` under *"Dates on screen are
  readable, per the project rule"*: the helper exists, it renders a weekday and a full month on a
  12-hour clock, the locale is pinned, AM/PM is upper case with a space, null reads as a dash, the
  trail and `#28` both go through it, and `readableInstant` is still there for hints. The
  locale check is scoped to the function's own body — a whole-file version passed happily while
  the function was switched to the browser's locale, because `startOfDayInZone` also builds an
  `Intl` formatter.
- **Still open** — `api-battleground/src/lib/format.js`. `formatDateTime()` uses
  `toLocaleString(undefined, …)`, so its format follows the viewer's machine, and it drops the
  year. `formatClock()` is a 24-hour clock; that one is arguably fine as it is, since it
  timestamps API calls in an activity log to the millisecond rather than showing a subscription
  date — but it is a third place a time gets formatted, and three formatters is how two of them
  drift.
- **Not touched** — the eleven `readableInstant` call sites in `Subscriptions.jsx` that are
  "stored as …" hints. Per the split above, those are correct as they are.

## Two things to settle

**The space before AM/PM.** Implemented as given — `11:59 PM`, with a space. The backend's
`Dates.readable` produces `11:59PM` with none. Both are fine on their own; the same instant
rendered both ways on one screen is not, and the backend's version travels inside API messages
the frontend cannot reformat. Changing either is a one-line edit — say which wins.

**Which zone the screen renders in.** There are three candidate answers and they name
different days for the same instant:

- the **school's** zone — what the backend uses, and what the school means by its own dates
- the **viewer's** browser zone — what a person's own clock says
- **UTC** — what the raw field says, which is what is shown today

The backend picked the school's zone because a billing period starting at midnight in
Asia/Kolkata is stored as `18:30Z` the day before, and UTC names the 7th where the school says
the 8th. The screen shows the school's own subscription, so the school's zone is the consistent
answer — but an operator in another country reading it may expect their own. Not decided.

(`10:90` in the original is a typo — no such minute — read as any valid minute value.)
