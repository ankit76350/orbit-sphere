/**
 * Turning what a date-time picker holds into what the API stores, and back.
 *
 * THE API TAKES AN INSTANT — a moment in UTC, `2027-01-31T18:29:59Z`. A `datetime-local` input
 * holds a WALL CLOCK READING with no timezone at all, `2027-01-31T23:59:59`, which the browser
 * reads in whatever zone the machine is set to. They are not the same kind of thing, so a picker
 * cannot be wired straight to the field.
 *
 * WHICH IS THE WHOLE REASON TO USE A PICKER HERE. Typing the instant by hand is where the mistake
 * lives: an Indian school's end of day is `18:29:59Z`, and `23:59:59Z` — which looks obviously
 * right — hands it most of the next day as well. Picking 11:59:59 pm on a machine set to IST
 * produces `18:29:59Z` without anybody having to know that.
 *
 * THE FORM ALWAYS HOLDS THE INSTANT, never the local reading. The picker is a view over it: it
 * renders the instant in local time and writes an instant back. So what the form holds is exactly
 * what will be sent, and switching to the raw boxes shows the same string rather than a converted
 * one.
 *
 * THE BROWSER'S ZONE IS NOT THE SCHOOL'S. Nothing here reads the school's timezone, because the
 * value the API wants is an absolute moment and the picker's job is only to help somebody name
 * one. A tester in London picking 11:59 pm gets a different instant than one in Mumbai — which is
 * correct, and is why the instant is shown beside every picker rather than hidden behind it.
 */

/** Two digits, so `9` reads as `09`. */
const pad = (n) => String(n).padStart(2, '0')

/**
 * What a `datetime-local` input holds → the instant to send.
 *
 * `2027-01-31T23:59:59` on a machine in IST → `2027-01-31T18:29:59Z`.
 *
 * Returns '' for anything unusable, which is how a cleared box drops the field from the body
 * rather than sending an empty string the API would have to reject.
 */
export function toInstant(local) {
  if (!local) return ''
  //! NO Z AND NO OFFSET, so JavaScript reads this as LOCAL time — which is what the input means.
  //! Appending 'Z' here would be the exact bug this module exists to prevent.
  const when = new Date(local)
  if (Number.isNaN(when.getTime())) return ''
  // Seconds, never milliseconds: the API takes either, and `.000Z` is noise in a request body.
  return when.toISOString().replace(/\.\d{3}Z$/, 'Z')
}

/**
 * An instant → what a `datetime-local` input should show.
 *
 * `2027-01-31T18:29:59Z` on a machine in IST → `2027-01-31T23:59:59`.
 *
 * Returns '' for anything unparseable, so a half-typed raw value leaves the picker empty instead
 * of throwing. That matters: the two controls show the same state, and switching to a picker with
 * nonsense in the box must not break the screen.
 */
export function toLocalInput(instant) {
  if (!instant) return ''
  const when = new Date(instant)
  if (Number.isNaN(when.getTime())) return ''
  return `${when.getFullYear()}-${pad(when.getMonth() + 1)}-${pad(when.getDate())}`
    + `T${pad(when.getHours())}:${pad(when.getMinutes())}:${pad(when.getSeconds())}`
}

/**
 * The instant written the way a person reads it, for the line under each picker.
 *
 * Says the zone, because the whole point is that the reading and the instant differ — and without
 * naming the zone, "31 Jan 2027, 23:59:59" beside `18:29:59Z` looks like a bug rather than the
 * same moment said twice.
 */
export function readable(instant) {
  if (!instant) return ''
  const when = new Date(instant)
  if (Number.isNaN(when.getTime())) return ''
  try {
    //! NOT dateStyle/timeStyle. Intl forbids combining either with timeZoneName, and the throw
    //! is silent here because it lands in the catch below - which is how this read as an ISO
    //! string on the first run. Spelled-out components are the only way to get all three.
    return when.toLocaleString(undefined, {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
      timeZoneName: 'short',
    })
  } catch {
    // Intl can be absent in a stripped runtime. The instant itself is still shown beside this.
    return when.toISOString()
  }
}
