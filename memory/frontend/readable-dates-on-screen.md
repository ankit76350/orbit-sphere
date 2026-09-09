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

## Where it stands — three places break it today

1. **`new-api-tester/src/lib/dates.js` — `readableInstant()` returns the raw ISO string**, despite
   the name, and there are **11 call sites** in
   [Subscriptions.jsx](../../new-api-tester/src/pages/platform/plans/Subscriptions.jsx). This is
   the main one to fix: the function is already the single place every screen goes through, so the
   rule lands in one edit.
2. **`api-battleground/src/lib/format.js` — `formatDateTime()`** uses
   `toLocaleString(undefined, …)`, so the format follows the viewer's machine, and it omits the
   year entirely.
3. **`formatClock()`** in the same file is a 24-hour clock. That one is arguably fine as it is —
   it timestamps API calls in an activity log to the millisecond, which is a different job from
   showing a subscription date — but it is the third place a time is formatted, and three
   formatters is how two of them drift.

## Two things to settle

**1. The space before AM/PM.** This rule was given as `10:90 AM/PM`, so `11:59 PM` with a space.
The backend's `Dates.readable` produces `11:59PM` with none. Both are fine on their own; the same
instant rendered both ways on one screen is not. Say which wins and both sides get it — my
suggestion is to match the backend, since its output travels inside API messages the frontend
cannot reformat.

**2. Which zone the screen renders in.** There are three candidate answers and they name
different days for the same instant:

- the **school's** zone — what the backend uses, and what the school means by its own dates
- the **viewer's** browser zone — what a person's own clock says
- **UTC** — what the raw field says, which is what is shown today

The backend picked the school's zone because a billing period starting at midnight in
Asia/Kolkata is stored as `18:30Z` the day before, and UTC names the 7th where the school says
the 8th. The screen shows the school's own subscription, so the school's zone is the consistent
answer — but an operator in another country reading it may expect their own. Not decided.

(`10:90` in the original is a typo — no such minute — read as any valid minute value.)
