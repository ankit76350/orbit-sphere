/**
 * A calendar day in a box, an instant on the wire.
 *
 * The API deals in instants — `2027-03-31T23:59:59Z` — and people deal in days. A date input
 * gives a day and a calendar to pick it from; these four turn one into the other.
 *
 * ONE COPY, BECAUSE THE END-OF-DAY RULE IS EASY TO GET WRONG. It was written twice before this
 * module existed, and two copies of a rule about billing dates is one copy that eventually says
 * midnight where the other says a second to midnight.
 *
 * EVERYTHING HERE IS UTC, matching the `Z` the API sends back, so a day picked in the box is the
 * same day when it is read again. The billing period the backend derives from a plan's cycle is
 * calculated in the school's own zone; that is a different question from what a picker sends, and
 * anywhere the distinction matters the screen shows the stored instant next to the box.
 */

/** An instant from the API as the yyyy-MM-dd a date input wants. "" for anything unreadable. */
export function toDateInput(instant) {
  if (!instant) return ''
  const at = new Date(instant)
  return Number.isNaN(at.getTime()) ? '' : at.toISOString().slice(0, 10)
}

/** Today, in the same shape, so a form's default matches the API's. */
export function todayInput() {
  return new Date().toISOString().slice(0, 10)
}

/** The start of a chosen day, or null for blank. "From the 1st" includes the whole 1st. */
export function startOfDay(value) {
  return value ? `${value}T00:00:00Z` : null
}

/**
 * The end of a chosen day, or null for blank.
 *
 * END OF DAY, NOT MIDNIGHT. "Stops being sold on 31 March" means the 31st is the last day it can
 * be sold, and midnight on the 31st would cut it off before that day started — it would close a
 * day earlier than whoever typed it expected.
 */
export function endOfDay(value) {
  return value ? `${value}T23:59:59Z` : null
}

/**
 * The instant a given day begins in a given IANA zone.
 *
 * NEEDED BECAUSE #13 MADE currentPeriodStart COMPULSORY. Before that the form could omit it and
 * let the API default to "the start of today in the school's zone"; now the form has to name that
 * instant, and `${day}T00:00:00Z` is the wrong one for any school not on UTC. For Asia/Kolkata
 * the day begins at 18:30Z the evening before; for America/New_York, at 04:00Z the same morning —
 * and that second case is what a naive UTC midnight gets refused for, because 00:00Z is still the
 * previous day there and the API reads it as a past start.
 *
 * Falls back to UTC midnight when the zone is unknown or unusable, which is the best guess
 * available and no worse than what the form sent before.
 */
export function startOfDayInZone(day, zone) {
  if (!day) return null
  const utcMidnight = Date.parse(`${day}T00:00:00Z`)
  if (Number.isNaN(utcMidnight)) return null
  if (!zone) return `${day}T00:00:00Z`

  try {
    // What UTC midnight of that day reads as in the zone, used to recover the zone's offset then.
    // Two passes, because the offset can itself differ either side of midnight on a DST boundary.
    let guess = utcMidnight
    for (let pass = 0; pass < 2; pass += 1) {
      const parts = new Intl.DateTimeFormat('en-GB', {
        timeZone: zone, hour12: false,
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
      }).formatToParts(new Date(guess))
      const at = Object.fromEntries(parts.filter((p) => p.type !== 'literal')
        .map((p) => [p.type, Number(p.value)]))
      const asIfUtc = Date.UTC(at.year, at.month - 1, at.day,
        at.hour % 24, at.minute, at.second)
      guess = utcMidnight - (asIfUtc - guess)
    }
    return new Date(guess).toISOString().replace('.000', '')
  } catch {
    // An unknown zone throws in Intl. UTC midnight is the honest fallback.
    return `${day}T00:00:00Z`
  }
}

/** Today's date as it stands in a given zone, which is not always today's UTC date. */
export function todayInZone(zone) {
  if (!zone) return todayInput()
  try {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit',
    }).format(new Date())
  } catch {
    return todayInput()
  }
}

/**
 * An instant as a person reads it: "Friday 8 October 2027 11:59 PM".
 *
 * THE PROJECT RULE FOR ANYTHING ON SCREEN. A raw `2027-10-08T23:59:59Z` in a table row is
 * correct and nobody reads it, and the backend already spells out every date inside its
 * messages — a screen that prints the raw field beside one of those messages undoes that.
 *
 * THE TIME IS ALWAYS SHOWN, even when it looks redundant, for the same reason the backend shows
 * it: these dates get compared against each other and both ends can fall on the same day.
 *
 * THE LOCALE IS PINNED, not taken from the browser. `undefined` makes the format a property of
 * whoever opened the page, and a screenshot in a bug report has to mean the same thing as what
 * the next person sees.
 *
 * UTC, matching the `Z` the API sends. The school's own zone would name a different day for the
 * same instant — see the note at the top of this module — and that choice is not settled; where
 * it matters, the screen shows the stored instant next to this via `readableInstant`.
 */
export function readableDateTime(instant) {
  if (!instant) return '—'
  const at = new Date(instant)
  if (Number.isNaN(at.getTime())) return String(instant)

  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'UTC', weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    hour: 'numeric', minute: '2-digit', hour12: true,
  }).formatToParts(at)
  const at_ = Object.fromEntries(parts.filter((p) => p.type !== 'literal')
    .map((p) => [p.type, p.value]))

  // Assembled rather than taken from the formatter's own joining, so the shape is ours: no comma
  // after the weekday, and AM/PM upper case with one space before it.
  return `${at_.weekday} ${at_.day} ${at_.month} ${at_.year} `
    + `${at_.hour}:${at_.minute} ${(at_.dayPeriod || '').toUpperCase()}`
}

/**
 * The exact stored instant, unchanged.
 *
 * KEPT ALONGSIDE `readableDateTime` ON PURPOSE, and it is not a violation of the readable-date
 * rule. This is an API testing tool: next to a date picker that can only show a day, the thing
 * somebody needs to see is the precise value on the wire, including the seconds and the `Z`.
 * Use this for "stored as ..." hints; use `readableDateTime` for anything presented as
 * information.
 */
export function readableInstant(instant) {
  if (!instant) return null
  const at = new Date(instant)
  return Number.isNaN(at.getTime()) ? String(instant) : at.toISOString().replace('.000', '')
}
