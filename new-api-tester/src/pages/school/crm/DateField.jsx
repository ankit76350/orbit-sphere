import { Field, Input } from '../../../components/ui/Kit.jsx'
import { readable, toInstant, toLocalInput } from './admissionDates.js'

/**
 * One instant, as a picker, with the instant it will actually send shown underneath.
 *
 * SHARED FROM 2026-09-28. It lived inside the create screen until #3 started taking a date of its
 * own — the closing date a reopen carries — and a second copy would have been a second place for
 * the seconds-vs-minutes decision below to be got wrong.
 *
 * THE INSTANT IS ALWAYS VISIBLE because the picker and the field are different things: the picker
 * holds a wall-clock reading in the browser's zone, the API stores a moment in UTC. Hiding the
 * conversion is how somebody sends `23:59:59Z` meaning midnight in India and quietly gets most of
 * the next day.
 *
 * RAW MODE IS A TEXT BOX and nothing is validated in it. A picker cannot express a malformed
 * instant, and this is an API tester — every refusal has to stay reachable, including the ones a
 * well-behaved control would make impossible.
 */
export default function DateField({ label, hint, required, raw, value, error, onChange }) {
  return (
    <Field label={label} hint={hint} required={required} error={error}>
      {raw ? (
        <Input value={value} error={error} onChange={(e) => onChange(e.target.value)}
          placeholder="2027-01-31T18:29:59Z" />
      ) : (
        <>
          <Input
            type="datetime-local"
            // Seconds matter here: an end-of-day is 23:59:59, and without this the picker
            // rounds to the minute and quietly sends :00.
            step="1"
            value={toLocalInput(value)}
            error={error}
            onChange={(e) => onChange(toInstant(e.target.value))}
          />
          {value ? (
            <p className="muted mono" style={{ marginTop: 4 }}>
              sends {value} — {readable(value)}
            </p>
          ) : null}
        </>
      )}
    </Field>
  )
}
