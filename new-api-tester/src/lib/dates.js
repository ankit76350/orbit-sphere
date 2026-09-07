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

/** A stored instant as something readable next to a picker that can only show the day. */
export function readableInstant(instant) {
  if (!instant) return null
  const at = new Date(instant)
  return Number.isNaN(at.getTime()) ? String(instant) : at.toISOString().replace('.000', '')
}
