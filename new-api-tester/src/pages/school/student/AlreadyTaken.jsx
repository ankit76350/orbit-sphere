import { useEffect, useState } from 'react'
import { AlertTriangle, Link2 } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import useDebounced from '../../../lib/useDebounced.js'
import { Button } from '../../../components/ui/Kit.jsx'
import { readable } from '../crm/admissionDates.js'

/** Just the digits, for telling which of a guardian's two numbers was matched. */
const digitsOf = (value) => (value ?? '').replace(/[^0-9]/g, '')

/**
 * The red box under a phone or an email box, wherever a guardian is being typed in.
 *
 * TWO SCREENS USE IT — admitting a child (#1) and adding a guardian to one (#11) — because both
 * write into the same unique index and both refuse a number that is already somebody's.
 *
 * WHY IT EXISTS. #1 used to link a taken number silently, so typing one guardian's name could
 * return a different person's — correct for a sibling, alarming for everybody else. The server
 * refuses now, and this is what stops the refusal being a surprise: it asks #9 while you type and
 * says, before you press Send, whose number that is.
 *
 * IT ASKS #9, THE SAME ENDPOINT THE SERVER'S REFUSAL AGREES WITH. Both compare digits, so a number
 * this finds is a number Send would be refused on — no "the check said nothing and it refused
 * anyway".
 *
 * DEBOUNCED, because it runs on every keystroke of a phone number. 400ms after you stop.
 *
 * THE WHOLE PERSON IS ONE CLICK AWAY. The box names them; opening it shows every field the school
 * holds, so "is this the same man" is answerable without leaving the form.
 *
 * LINKING IS A DELIBERATE PRESS. It fills guardianDocsId, which is the only thing that makes #1
 * attach an existing person — and it locks the typed fields, because the stored row wins and
 * correcting it is #8.
 */
export default function AlreadyTaken({ by, value, linked, onLink }) {
  const { call } = useApi()
  const [found, setFound] = useState(null)
  const [open, setOpen] = useState(false)

  //! THE TYPING STOPS HERE. Everything below sees a value that is 400ms late, so the effect is
  //! about asking the question rather than about when to ask it. One pause, one request —
  //! a ten digit phone number is one question, not ten.
  const settled = useDebounced((value ?? '').trim(), 400)

  //! THE BOX NEVER OUTLIVES THE TEXT IT IS ABOUT. Cleared on the RAW value rather than the
  //! debounced one, so the moment a character changes the warning goes — it does not sit there
  //! naming somebody for a number that is no longer on screen while the next query runs.
  //!
  //! Measured 2026-10-08 on live data: typing "07635046798" passes through "076350467", which is
  //! a genuine guardian in that school, so the stale box named a person whose number did not
  //! match the field.
  useEffect(() => { setFound(null); setOpen(false) }, [value, linked])

  useEffect(() => {
    //! A LINKED ROW ASKS NOTHING. The number in the box is the linked person's own, so the only
    //! thing it could find is them — and a red box saying "this is who you just linked" is noise.
    if (linked) return undefined

    //! A PHONE IS ONLY WORTH ASKING ABOUT ONCE THERE IS SOMETHING TO ASK. Six digits is short
    //! enough that the server compares the whole number, which is where a false "we know them"
    //! would come from.
    if (settled.length < (by === 'phone' ? 6 : 5)) return undefined

    //! THE ANSWER TO A QUESTION NOBODY IS ASKING ANY MORE IS DROPPED. The debounce makes this
    //! rare — one request per pause — but a slow reply to an old pause could still land after a
    //! fast reply to a new one, and the box would show the older of the two.
    let dropped = false

    void (async () => {
      const result = await call('list-guardians', {
        label: `Is this ${by} already somebody's`,
        //! IT GOES IN THE REQUEST LOG LIKE EVERYTHING ELSE, deliberately. This is an API testing
        //! tool: a call the screen made on your behalf is exactly the kind you want to see. The
        //! debounce is what keeps it to one entry per pause rather than one per keystroke.
        queryParams: { [by]: settled, page: '0', size: '1' },
      })
      if (dropped) return
      setFound(result.ok ? (result.bodyJson?.content ?? [])[0] ?? null : null)
    })()

    return () => { dropped = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [settled, by, linked])

  if (!found) return null

  //! A GUARDIAN WITH NO NAME IS A REAL ROW, and the box has to say something about them. Student
  //! #11 wrote a few before 2026-10-07 — a record built in Java skipped the @NotBlank — and
  //! "belongs to  — who is that?" reads as a broken screen rather than as the broken DATA it is.
  const who = (found.fullName ?? '').trim();
  const label = who || 'a guardian with no name on file';

  //! WHICH VALUE THE MATCH IS ON, shown whenever it is not the one on screen.
  //!
  //! THE MATCH IS LOOSE — digits only, last ten — so "+91 98765 43210" finds a stored
  //! "098765 43210", which is right and does not look it. And a guardian can match on their
  //! ALTERNATE number, which is the case that matters: that number is a shared family landline,
  //! so #1, #7 and #11 will NOT refuse on it. Saying "as their second number" is the difference
  //! between a box that explains the refusal and one that contradicts it.
  const typed = (value ?? '').trim();
  const theirs = by === 'phone' ? found.phoneNumber : found.emailAddress;
  const onAlternate = by === 'phone'
    && found.alternatePhoneNumber
    && digitsOf(found.alternatePhoneNumber) === digitsOf(typed)
    && digitsOf(found.phoneNumber) !== digitsOf(typed);
  const differs = (theirs ?? '') !== typed;

  return (
    <div className="taken">
      <div className="taken-head">
        <AlertTriangle size={13} />
        <span className="taken-what">
          This {by === 'phone' ? 'number' : 'address'} belongs to <b>{label}</b>
          {onAlternate ? (
            <> — <b>as their second number</b>, so this will not be refused</>
          ) : differs ? (
            <>, whose {by === 'phone' ? 'number' : 'address'} is{' '}
              <span className="mono">{theirs || 'not set'}</span></>
          ) : null}
        </span>
        {/* THE ACTION IS RIGHT HERE, not behind the details. Finding out whose number it is and
            deciding to use them are the same thought, and making somebody expand a panel first
            put the only useful button one click further away than the problem. */}
        <Button look="primary" icon={Link2} onClick={() => onLink(found)}>
          Use this guardian
        </Button>
        <Button look="quiet" onClick={() => setOpen((o) => !o)}>
          {open ? 'Hide' : 'Details'}
        </Button>
      </div>

      {open ? (
        <div className="taken-body">
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Name</td>
                  <td>{who || <span className="muted">none on file — correct it with #8</span>}</td></tr>
                <tr><td className="muted">Phone</td>
                  <td><span className="mono">{found.phoneNumber ?? '—'}</span></td></tr>
                <tr><td className="muted">Alternate</td>
                  <td><span className="mono">{found.alternatePhoneNumber ?? '—'}</span></td></tr>
                <tr><td className="muted">Email</td><td>{found.emailAddress ?? '—'}</td></tr>
                <tr><td className="muted">Address</td><td>{found.address ?? '—'}</td></tr>
                <tr><td className="muted">Occupation</td><td>{found.occupation ?? '—'}</td></tr>
                <tr><td className="muted">Guardian id</td>
                  <td><span className="mono">{found.guardianDocsId}</span></td></tr>
                <tr><td className="muted">Added</td>
                  <td title={found.createdAt}>{readable(found.createdAt)}</td></tr>
              </tbody>
            </table>
          </div>
          <p className="muted">
            <b>Is this the same person?</b> If it is — a sibling&rsquo;s father, say —{' '}
            <b>Use this guardian</b> attaches this child to the row the school already holds, and
            their stored details win. If it is not, the{' '}
            {by === 'phone' ? 'number' : 'address'} is wrong on one of the two, and sending as-is
            is <span className="mono">409</span>.
          </p>
          <div className="toolbar">
            <span className="toolbar-spacer" />
            <Button look="primary" icon={Link2} onClick={() => onLink(found)}>
              Use this guardian
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
