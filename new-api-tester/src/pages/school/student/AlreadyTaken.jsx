import { useEffect, useState } from 'react'
import { AlertTriangle, Link2 } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import { Button } from '../../../components/ui/Kit.jsx'
import { readable } from '../crm/admissionDates.js'

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

  useEffect(() => {
    //! A LINKED ROW ASKS NOTHING. The number in the box is the linked person's own, so the only
    //! thing it could find is them — and a red box saying "this is who you just linked" is noise.
    if (linked) { setFound(null); return undefined }

    const typed = (value ?? '').trim()
    //! A PHONE IS ONLY WORTH ASKING ABOUT ONCE THERE IS SOMETHING TO ASK. Six digits is short
    //! enough that the server compares the whole number, which is where a false "we know them"
    //! would come from.
    if (typed.length < (by === 'phone' ? 6 : 5)) { setFound(null); return undefined }

    let dropped = false
    //! DEBOUNCED. This runs on every keystroke of a phone number otherwise.
    const timer = setTimeout(async () => {
      const result = await call('list-guardians', {
        label: `Is this ${by} already somebody's`,
        //! IT GOES IN THE REQUEST LOG LIKE EVERYTHING ELSE, deliberately. This is an API testing
        //! tool: a call the screen made on your behalf is exactly the kind you want to see. The
        //! 400ms debounce is what keeps it to one entry per pause rather than one per keystroke.
        queryParams: { [by]: typed, page: '0', size: '1' },
      })
      if (dropped) return
      const first = result.ok ? (result.bodyJson?.content ?? [])[0] : null
      setFound(first ?? null)
      setOpen(false)
    }, 400)

    return () => { dropped = true; clearTimeout(timer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [value, by, linked])

  if (!found) return null

  return (
    <div className="taken">
      <button type="button" className="taken-head" onClick={() => setOpen((o) => !o)}>
        <AlertTriangle size={13} />
        <span>
          This {by === 'phone' ? 'number' : 'address'} belongs to <b>{found.fullName}</b>
          {' '}— {open ? 'hide' : 'who is that?'}
        </span>
      </button>

      {open ? (
        <div className="taken-body">
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Name</td><td>{found.fullName}</td></tr>
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
            <b>Is this the same person?</b> If it is — a sibling&rsquo;s father, say — link them and
            this child is attached to the row the school already holds. If it is not, the{' '}
            {by === 'phone' ? 'number' : 'address'} is wrong on one of the two, and sending as-is
            is <span className="mono">409</span>.
          </p>
          <div className="toolbar">
            <span className="toolbar-spacer" />
            <Button look="primary" icon={Link2} onClick={() => onLink(found)}>
              Link this guardian
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
