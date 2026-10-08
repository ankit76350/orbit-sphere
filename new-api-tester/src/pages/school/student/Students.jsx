import { useCallback, useEffect, useState } from 'react'
import { Info, Link2, Link2Off, Plus, RefreshCw, Search, Trash2, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import AlreadyTaken from './AlreadyTaken.jsx'
import { detailPath } from '../../../paths.js'

/**
 * The roll: /school-student/students
 *
 * TWO ENDPOINTS HERE — #4 lists the roll and #1 admits a child. #5 opens one and is its own page.
 *
 * THERE WAS A THIRD UNTIL 2026-10-08. #6 asked "is this child already here?" before an admission,
 * and it was removed: #1 refuses a guardian whose number the school already holds and names them,
 * so the duplicate question is answered at the moment it matters rather than as a separate step
 * somebody has to remember to take.
 *
 * A TAKEN NUMBER IS REFUSED, NOT QUIETLY LINKED — changed 2026-10-07 after exactly the confusion
 * it causes: typing "ANKIT KUMAR" on a number the school already held for "Hero" returned a child
 * whose father was Hero, a different person, with no warning.
 *
 * SO THE FORM CHECKS WHILE YOU TYPE. The phone and the email boxes each ask #9 after you stop
 * typing, and a number that is already somebody's gets a RED BOX under the box naming them. Open
 * it to see the whole person, and press "Link this guardian" if it really is them — that is what
 * sends guardianDocsId, and it is the only way #1 attaches an existing person.
 *
 * THE SIBLING IS STILL THE CASE WORTH PRESSING TWICE. Admit one child, then admit another naming
 * the same father: the warning appears, you link him deliberately, and `matched` comes back true
 * with his id. The difference from before is that nothing happens behind your back.
 *
 * NOTHING IS DISABLED. Admit sends with whatever is in the boxes — no primary contact, two of
 * them, a date in the future — because each of those is a documented refusal and this is the tool
 * for reaching them.
 */

const GENDERS = ['', 'MALE', 'FEMALE', 'OTHER']
const STATUSES = ['', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'WITHDRAWN', 'TRANSFERRED', 'GRADUATED']
// THE WHOLE ENUM, and it is checked against GuardianRelation.java rather than guessed.
// Measured 2026-10-07: GUARDIAN and GRANDPARENT are NOT values — a picker offering them
// sends a 400 INVALID_VALUE for a choice the screen itself put in front of somebody.
const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
  'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']
const TONE = { ACTIVE: 'good', WITHDRAWN: 'bad', TRANSFERRED: 'bad', SUSPENDED: 'warn' }

/** Every filter #4 takes, all empty. One shape, so "is anything set" is a count rather than a list. */
const BLANK_FILTERS = {
  search: '', phone: '', email: '', status: '', gender: '', placed: '', fromAdmissions: '',
  admittedFrom: '', admittedTo: '', sort: '',
}

/** One blank guardian row. A form opens with a single one, because most families send one. */
const blankGuardian = (primary) => ({
  //! SET ONLY BY PRESSING "Link this guardian" on the warning below the phone box. Empty means
  //! "this is a new person", and the server refuses if the number turns out to be somebody's.
  guardianDocsId: '',
  fullName: '', relation: 'FATHER', phoneNumber: '', emailAddress: '',
  alternatePhoneNumber: '', address: '', occupation: '',
  primaryContact: primary, emergencyContact: false, pickupAuthorized: false, portalAccess: false,
})

export default function Students() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)

  //! TWO COPIES OF THE FILTERS, and that is the whole of the Search button.
  //!
  //! `draft` is what the boxes hold; `applied` is what the last press asked for, and only
  //! `applied` is in the load effect's deps. They were one object before, so every keystroke in
  //! the name box was a round trip — fine for a toy roll, wrong for a school with two thousand
  //! children, and wrong in an API tester for a second reason: the request log filled with
  //! requests nobody made.
  const [draft, setDraft] = useState(BLANK_FILTERS)
  const [applied, setApplied] = useState(BLANK_FILTERS)
  const [open, setOpen] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-students', {
      label: 'The roll',
      //! `query`, NOT `queryParams`. buildCall reads options.query and silently ignores anything
      //! else — a wrong name sends no filters AND no paging, and the list still looks like it
      //! works because the server has defaults for both. That was live for a day.
      query: {
        page: String(page), size: '20',
        //! EVERY FILTER IS SENT ONLY WHEN IT HAS A VALUE. An empty one means "do not filter",
        //! which is a different question from "filter on the empty string".
        ...Object.fromEntries(Object.entries(applied).filter(([, v]) => v !== '')),
      },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, page, applied])

  useEffect(() => { load() }, [load])

  //! A SEARCH ALWAYS GOES BACK TO PAGE ONE. Staying on page 4 of the old answer and asking for
  //! page 4 of a new one is how somebody lands on an empty page and reads it as "no matches".
  const apply = () => { setPage(0); setApplied(draft) }
  const clear = () => { setPage(0); setDraft(BLANK_FILTERS); setApplied(BLANK_FILTERS) }

  const activeCount = Object.values(applied).filter((v) => v !== '').length
  const dirty = JSON.stringify(draft) !== JSON.stringify(applied)

  if (!actingSubdomain) return <NoSchoolChosen what="The roll" />

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">The roll</h1>
          <p className="muted">
            Every child the school has accepted. <b>Placing them in a class is #14</b>, which is not
            built — so everybody reads back as not placed.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-students" name="List" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Admit a child</Button>
      </div>

      <Card
        title="Filters"
        description="Every one is optional, and nothing is sent until you press Search. The class filters are not here — a class lives on the academic record, which #14 writes and which does not exist yet."
        action={<EndpointTag id="list-students" name="Read" />}
      >
        {/* ENTER SEARCHES. A form rather than a div, so the key that means "go" in every other
            search box on earth means it here too — and the button is the submit, which is why it
            needs no onClick of its own. */}
        <form onSubmit={(e) => { e.preventDefault(); apply() }}>
          <div className="field-grid">
            <Field label="Name or admission number" hint="Matches anywhere, either field.">
              <Input value={draft.search} placeholder="sharma"
                onChange={(e) => setDraft({ ...draft, search: e.target.value })} />
            </Field>
            <Field label="Phone"
              hint="THE CHILD'S OWN AND THEIR GUARDIANS'. A seven year old has no phone — the number a school holds is their mother's. Matched on its digits.">
              <Input value={draft.phone} placeholder="098765 43210"
                onChange={(e) => setDraft({ ...draft, phone: e.target.value })} />
            </Field>
            <Field label="Email"
              hint="The child's own and their guardians'. Matched whole — an address is an identity.">
              <Input value={draft.email} placeholder="parent@example.com"
                onChange={(e) => setDraft({ ...draft, email: e.target.value })} />
            </Field>
            <Field label="Status">
              <Select value={draft.status} options={STATUSES}
                onChange={(v) => setDraft({ ...draft, status: v })} />
            </Field>
            <Field label="Gender">
              <Select value={draft.gender} options={GENDERS}
                onChange={(v) => setDraft({ ...draft, gender: v })} />
            </Field>
            <Field label="Placed in a class" hint="The start-of-term question. Everything is false until #14.">
              <Select value={draft.placed} options={['', 'true', 'false']}
                onChange={(v) => setDraft({ ...draft, placed: v })} />
            </Field>
            <Field label="Came from admissions" hint="true is a child CRM #33 enrolled. false is a transfer or a walk-in.">
              <Select value={draft.fromAdmissions} options={['', 'true', 'false']}
                onChange={(v) => setDraft({ ...draft, fromAdmissions: v })} />
            </Field>
            <Field label="Admitted from" hint="Inclusive. Set both to the same day to ask about one day.">
              <Input type="date" value={draft.admittedFrom}
                onChange={(e) => setDraft({ ...draft, admittedFrom: e.target.value })} />
            </Field>
            <Field label="Admitted to" hint="Inclusive.">
              <Input type="date" value={draft.admittedTo}
                onChange={(e) => setDraft({ ...draft, admittedTo: e.target.value })} />
            </Field>
            <Field label="Sort"
              hint="fullName · admissionNo · admissionDate · createdAt. Anything else is 400 — the allowlist is a security control.">
              <Input value={draft.sort} placeholder="admissionDate,desc"
                onChange={(e) => setDraft({ ...draft, sort: e.target.value })} />
            </Field>
          </div>

          <div className="toolbar">
            <span className="muted">
              {activeCount === 0
                ? 'No filters — this is the whole roll.'
                : `${activeCount} filter${activeCount === 1 ? '' : 's'} applied.`}
              {dirty ? ' Edited since the last search.' : ''}
            </span>
            <span className="toolbar-spacer" />
            {/* NEVER DISABLED, even with nothing typed and nothing changed: pressing Search on an
                empty form is a real request — the whole roll — and this is the tool for making
                requests on purpose. */}
            <Button icon={X} onClick={clear}>Clear</Button>
            <Button look="primary" icon={Search} type="submit" busy={loading}>Search</Button>
          </div>
        </form>

        <p className="muted">
          <Info size={12} /> <b>Phone and email look at the guardians too</b>, which is the only way
          either is useful — a child is found through their parent far more often than through their
          own details. It costs one extra read: the guardians are resolved first and their ids go
          into the student query, rather than a query per child.
        </p>
        <p className="muted">
          <Info size={12} /> <b>The sort allowlist is not a convenience.</b> An open sort field lets
          a caller order the roll by a date of birth and read the values back out of the ordering
          without this endpoint ever returning them. Type <span className="mono">dateOfBirth</span>{' '}
          and press Search to see it refuse.
        </p>
      </Card>

      <Card
        title={data ? `${data.totalElements} child${data.totalElements === 1 ? '' : 'ren'}` : 'The roll'}
        description="A row carries no guardians — fifty children with two contacts each is a hundred families' details to draw a list that shows none of them. It carries the count, because 'no contact on file' is worth seeing."
      >
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        ) : rows.length === 0 ? (
          <Empty
            title="Nobody yet"
            description="An empty page, never a 404. Admit a child, or run the CRM handover (#33) and watch one arrive from the other side."
            action={<Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Admit one</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Admission no</th>
                  <th>Child</th>
                  <th>Born</th>
                  <th>Status</th>
                  <th className="num">Contacts</th>
                  <th>Placed</th>
                  <th>From admissions</th>
                  <th>Admitted</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  // Opening a row is its own address, so it can be linked and reloaded — and #5
                  // is the only endpoint that resolves the guardians into people.
                  <tr key={one.studentDocsId} data-opens
                    onClick={() => navigate(detailPath('school', 'student', 'students',
                      one.studentDocsId))}>
                    <td><span className="mono">{one.admissionNo}</span></td>
                    <td>{one.fullName}</td>
                    <td title={one.dateOfBirth}>{one.dateOfBirth}</td>
                    <td><Badge tone={TONE[one.status]}>{one.status}</Badge></td>
                    <td className="num">
                      {one.guardianCount === 0
                        ? <Badge tone="bad">none</Badge>
                        : one.guardianCount}
                    </td>
                    <td>
                      {one.placed
                        ? <Badge tone="good">in a class</Badge>
                        : <span className="muted">not yet</span>}
                    </td>
                    <td>
                      {one.admissionApplicationDocsId
                        ? <Badge tone="good">CRM #33</Badge>
                        : <span className="muted">typed in</span>}
                    </td>
                    <td title={one.createdAt}>{one.admissionDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          <span className="toolbar-spacer" />
          <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
        </div>
      </Card>

      {open ? <AdmitChild onClose={() => setOpen(false)} onAdmitted={load} /> : null}
    </div>
  )
}

/** #1 — admit a child. The guardian list is the whole of it. */
function AdmitChild({ onClose, onAdmitted }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    fullName: '', dateOfBirth: '', gender: 'MALE', admissionDate: '',
    nationalityCode: '', preferredLanguage: '', phoneNumber: '', emailAddress: '',
    admissionApplicationDocsId: '',
  })
  const [guardians, setGuardians] = useState([blankGuardian(true)])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const setG = (i, patch) =>
    setGuardians(guardians.map((g, n) => (n === i ? { ...g, ...patch } : g)))

  //! BUILT IN RENDER, NOT INSIDE send(), and that is the whole reason the JSON pane is honest.
  //! It is recomputed on every keystroke and the SAME object is both shown and sent, so the two
  //! cannot drift — a preview assembled separately from the request is a preview that lies the
  //! first time somebody edits one and forgets the other.
  //!
  //! AN EMPTY OPTIONAL IS OMITTED, not sent as "". The three closed-set fields in particular:
  //! "nationalityCode": "" is 400 VALIDATION_FAILED rather than "no country", so a blank box has
  //! to leave the key out. The four required ones are always present, because sending them blank
  //! is how VALIDATION_FAILED is reached on purpose.
  const body = {
    fullName: form.fullName,
    dateOfBirth: form.dateOfBirth,
    gender: form.gender,
    ...(form.admissionDate ? { admissionDate: form.admissionDate } : {}),
    ...(form.nationalityCode ? { nationalityCode: form.nationalityCode } : {}),
    ...(form.preferredLanguage ? { preferredLanguage: form.preferredLanguage } : {}),
    ...(form.phoneNumber ? { phoneNumber: form.phoneNumber } : {}),
    ...(form.emailAddress ? { emailAddress: form.emailAddress } : {}),
    ...(form.admissionApplicationDocsId
      ? { admissionApplicationDocsId: form.admissionApplicationDocsId } : {}),
    //! EVERY ROW IS SENT, including a blank one. The list is the thing being tested here — an
    //! empty guardians array is 400 VALIDATION_FAILED and a row with no name is the same, and
    //! quietly dropping either would put both refusals out of reach.
    //! A LINKED ROW SENDS THE ID AND ALMOST NOTHING ELSE. The stored person wins — their name,
    //! number and address are what the school already holds, and correcting them is #8, which
    //! would change them for every child they belong to. The relation and the flags still come
    //! from this form, because those belong to THIS child.
    guardians: guardians.map((g) => (g.guardianDocsId ? {
      guardianDocsId: g.guardianDocsId,
      fullName: g.fullName,
      relation: g.relation,
      primaryContact: g.primaryContact,
      emergencyContact: g.emergencyContact,
      pickupAuthorized: g.pickupAuthorized,
      portalAccess: g.portalAccess,
    } : {
      fullName: g.fullName,
      relation: g.relation,
      ...(g.phoneNumber ? { phoneNumber: g.phoneNumber } : {}),
      ...(g.emailAddress ? { emailAddress: g.emailAddress } : {}),
      ...(g.alternatePhoneNumber ? { alternatePhoneNumber: g.alternatePhoneNumber } : {}),
      ...(g.address ? { address: g.address } : {}),
      ...(g.occupation ? { occupation: g.occupation } : {}),
      primaryContact: g.primaryContact,
      emergencyContact: g.emergencyContact,
      pickupAuthorized: g.pickupAuthorized,
      portalAccess: g.portalAccess,
    })),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('admit-student', { label: 'Admit a child', body })
    setSending(false)
    setResult(answer)
    //! THE FORM IS NOT CLEARED ON SUCCESS, on purpose. The next thing worth doing is admitting a
    //! sibling: change the child's name, leave the father exactly as he is, and send it again.
    //! That is the one case this endpoint exists to get right, and clearing the boxes would make
    //! it fiddly to reach.
    if (answer.ok) onAdmitted()
  }

  const primaries = guardians.filter((g) => g.primaryContact).length

  return (
    <Modal
      open
      onClose={onClose}
      title="Admit a child"
      description="Creates the student and matches or creates their guardians. No class and no academic year — placing them is #14."
      endpoint={<EndpointTag id="admit-student" name="Admit" look="primary" />}
      // THE FORM ON THE LEFT, THE EXACT BODY ON THE RIGHT, recomputed as you type. On an API
      // testing tool the payload is as much the subject as the form is — and watching a blank
      // optional field disappear from the JSON is how the ""-is-not-a-value rule is seen rather
      // than read about.
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      footer={
        // NEVER DISABLED. No primary contact, two of them, a blank name, a date in the future —
        // each is a documented refusal, and this is the tool for reaching them.
        <Button look="primary" onClick={send} busy={sending}>Send it</Button>
      }
    >
      <div className="field-grid">
        <Field label="Full name" required>
          <Input value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Date of birth" required hint="Must be in the past.">
          <Input type="date" value={form.dateOfBirth}
            onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
        </Field>
        <Field label="Gender" required>
          <Select value={form.gender} options={GENDERS.filter(Boolean)}
            onChange={(v) => setForm({ ...form, gender: v })} />
        </Field>
        <Field label="Admission date" hint="Blank means today. A past date is allowed — a school typing in the roll it already had needs one.">
          <Input type="date" value={form.admissionDate}
            onChange={(e) => setForm({ ...form, admissionDate: e.target.value })} />
        </Field>
        <Field label="Nationality" hint="A closed set — IN, GB, US. 'in' is refused rather than corrected.">
          <Input value={form.nationalityCode}
            onChange={(e) => setForm({ ...form, nationalityCode: e.target.value })}
            placeholder="IN" />
        </Field>
        <Field label="Preferred language" hint="A closed set — en-IN, hi-IN.">
          <Input value={form.preferredLanguage}
            onChange={(e) => setForm({ ...form, preferredLanguage: e.target.value })}
            placeholder="en-IN" />
        </Field>
        <Field label="The child's own phone" hint="For an older student. Most children have none.">
          <Input value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
        </Field>
        <Field label="The child's own email">
          <Input value={form.emailAddress}
            onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} />
        </Field>
        <Field label="Admission application id" wide
          hint="CRM #33 sets this. An id that is not a real form in this school is 404 ADMISSION_APPLICATION_NOT_FOUND; a real one that already made a child is 409 APPLICATION_ALREADY_ENROLLED.">
          <Input value={form.admissionApplicationDocsId}
            onChange={(e) => setForm({ ...form, admissionApplicationDocsId: e.target.value })}
            placeholder="leave blank unless you are testing the link" />
        </Field>
      </div>

      <Card
        title={`Guardians — ${guardians.length}`}
        description="Matched, not blindly created: a phone number identifies one person per school, and two siblings share a father."
        action={
          <Button icon={Plus} onClick={() => setGuardians([...guardians, blankGuardian(false)])}>
            Add a guardian
          </Button>
        }
      >
        {guardians.map((g, i) => (
          <div key={i} className="stack">
            <div className="field-grid">
              <Field label={`Guardian ${i + 1} — full name`} required
                hint={g.guardianDocsId
                  ? 'Linked to somebody the school already holds. Their stored details win — correcting them is #8.'
                  : undefined}>
                <Input value={g.fullName} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { fullName: e.target.value })} />
              </Field>
              <Field label="Relation" required>
                <Select value={g.relation} options={RELATIONS}
                  onChange={(v) => setG(i, { relation: v })} />
              </Field>
              <Field label="Phone"
                hint="Checked while you type. A number that is already somebody's is refused unless you link them below.">
                <Input value={g.phoneNumber} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { phoneNumber: e.target.value })} />
                <AlreadyTaken by="phone" value={g.phoneNumber} linked={!!g.guardianDocsId}
                  onLink={(found) => setG(i, {
                    guardianDocsId: found.guardianDocsId,
                    fullName: found.fullName,
                    phoneNumber: found.phoneNumber ?? '',
                    emailAddress: found.emailAddress ?? '',
                    alternatePhoneNumber: found.alternatePhoneNumber ?? '',
                    address: found.address ?? '',
                    occupation: found.occupation ?? '',
                  })} />
              </Field>
              <Field label="Email"
                hint="Checked the same way. Unique per school, the same as the number.">
                <Input value={g.emailAddress} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { emailAddress: e.target.value })} />
                <AlreadyTaken by="email" value={g.emailAddress} linked={!!g.guardianDocsId}
                  onLink={(found) => setG(i, {
                    guardianDocsId: found.guardianDocsId,
                    fullName: found.fullName,
                    phoneNumber: found.phoneNumber ?? '',
                    emailAddress: found.emailAddress ?? '',
                    alternatePhoneNumber: found.alternatePhoneNumber ?? '',
                    address: found.address ?? '',
                    occupation: found.occupation ?? '',
                  })} />
              </Field>
              <Field label="Alternate phone" hint="Not checked — a shared family landline would make two people one.">
                <Input value={g.alternatePhoneNumber} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { alternatePhoneNumber: e.target.value })} />
              </Field>
              <Field label="Occupation">
                <Input value={g.occupation} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { occupation: e.target.value })} />
              </Field>
              <Field label="Address" wide>
                <Input value={g.address} disabled={!!g.guardianDocsId}
                  onChange={(e) => setG(i, { address: e.target.value })} />
              </Field>
            </div>
            {g.guardianDocsId ? (
              <p className="muted">
                <Link2 size={12} /> <b>Linked to {g.fullName}</b>, who the school already holds —{' '}
                <span className="mono">{g.guardianDocsId}</span>. Their details above are the
                stored ones and are sent as an id rather than retyped. <b>The flags below are still
                this child&rsquo;s</b>, because what somebody is to one child is not what they are
                to another.{' '}
                <Button icon={Link2Off}
                  onClick={() => setG(i, { guardianDocsId: '' })}>Unlink</Button>
              </p>
            ) : null}

            <div className="toolbar">
              <label className="check">
                <input type="checkbox" checked={g.primaryContact}
                  onChange={(e) => setG(i, { primaryContact: e.target.checked })} />
                <span>Primary contact</span>
              </label>
              <label className="check">
                <input type="checkbox" checked={g.emergencyContact}
                  onChange={(e) => setG(i, { emergencyContact: e.target.checked })} />
                <span>Emergency</span>
              </label>
              <label className="check">
                <input type="checkbox" checked={g.pickupAuthorized}
                  onChange={(e) => setG(i, { pickupAuthorized: e.target.checked })} />
                <span>May collect</span>
              </label>
              <label className="check">
                <input type="checkbox" checked={g.portalAccess}
                  onChange={(e) => setG(i, { portalAccess: e.target.checked })} />
                <span>Portal</span>
              </label>
              <span className="toolbar-spacer" />
              {/* Removing the last contact is allowed, and sending none is
                  400 VALIDATION_FAILED — a documented refusal worth reaching. */}
              <Button icon={Trash2}
                onClick={() => setGuardians(guardians.filter((_, n) => n !== i))}>
                Remove
              </Button>
            </div>
            <hr />
          </div>
        ))}

        <p className="muted">
          <Info size={12} /> <b>{primaries} marked as the primary contact.</b>{' '}
          {primaries === 1
            ? 'Exactly one, which is what #1 requires.'
            : <>Sending this is <span className="mono">400 PRIMARY_CONTACT_REQUIRED</span> — none
              means &ldquo;ring the family&rdquo; has no answer, two means it has two. The button
              still sends, because the refusal is worth reading.</>}
        </p>
      </Card>

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} Created` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <>
              <p className="muted">
                <b>{result.bodyJson?.fullName}</b> is{' '}
                <span className="mono">{result.bodyJson?.admissionNo}</span>, admitted{' '}
                {readable(result.bodyJson?.createdAt)}.
              </p>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Contact</th><th>Relation</th><th>Phone (stored)</th><th>Found or new</th></tr>
                  </thead>
                  <tbody>
                    {(result.bodyJson?.guardians ?? []).map((g) => (
                      <tr key={g.guardianDocsId}>
                        <td>{g.fullName}</td>
                        <td>{g.relation}</td>
                        <td><span className="mono">{g.phoneNumber ?? '—'}</span></td>
                        <td>
                          {g.matched
                            ? <Badge tone="good">matched — already here</Badge>
                            : <Badge>new</Badge>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="muted">
                <Info size={12} /> <b>Now admit the sibling.</b> Change the child&rsquo;s name,
                leave the father exactly as he is, and send it again — you will get a{' '}
                <span className="mono">409</span>, because the number is now his. <b>That is the
                point:</b> the red box under the phone appears as you type, and pressing{' '}
                <b>Link this guardian</b> is how you say &ldquo;yes, same man&rdquo;. Nothing
                happens behind your back any more.
              </p>
            </>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}

