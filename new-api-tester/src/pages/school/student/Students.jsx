import { useCallback, useEffect, useState } from 'react'
import { Info, Plus, RefreshCw, Trash2, UserSearch } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import { detailPath } from '../../../paths.js'

/**
 * The roll: /school-student/students
 *
 * THREE ENDPOINTS HERE — #4 lists the roll, #1 admits a child, #6 is the duplicate check made
 * before every admission. #5 opens one child and is its own page.
 *
 * THE DUPLICATE CHECK SITS ABOVE THE ROLL, not inside the admit form, and that is the product
 * rule rather than a layout choice. #1 does NOT refuse duplicates: refusing there would mean
 * deciding that two children sharing a surname and a phone number are one child, which siblings
 * are not. The judgement belongs to the person at the desk, so the answer is put in front of them
 * and the Admit button stays live either way.
 *
 * THE SIBLING IS THE CASE WORTH PRESSING TWICE. A guardian's phone is unique per school and two
 * siblings share a father, so #1 matches an existing contact and links them. The form keeps what
 * you typed after a successful admit for exactly that reason: change the child, leave the father,
 * send it again, and watch `matched` come back true with the same guardianDocsId.
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

/** One blank contact row. A form opens with a single one, because most families send one. */
const blankGuardian = (primary) => ({
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
  const [filters, setFilters] = useState({
    search: '', status: '', gender: '', placed: '', fromAdmissions: '', sort: '',
  })
  const [open, setOpen] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-students', {
      label: 'The roll',
      queryParams: {
        page: String(page), size: '20',
        //! EVERY FILTER IS SENT ONLY WHEN IT HAS A VALUE. An empty one means "do not filter",
        //! which is a different question from "filter on the empty string".
        ...(filters.search ? { search: filters.search } : {}),
        ...(filters.status ? { status: filters.status } : {}),
        ...(filters.gender ? { gender: filters.gender } : {}),
        ...(filters.placed ? { placed: filters.placed } : {}),
        ...(filters.fromAdmissions ? { fromAdmissions: filters.fromAdmissions } : {}),
        ...(filters.sort ? { sort: filters.sort } : {}),
      },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, page, filters])

  useEffect(() => { load() }, [load])

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

      <KnownChild />

      <Card
        title="Filters"
        description="Every one is optional. The class filters are not here — a class lives on the academic record, which #14 writes and which does not exist yet."
        action={<EndpointTag id="list-students" name="Read" />}
      >
        <div className="field-grid">
          <Field label="Name or admission number" hint="Matches anywhere, either field.">
            <Input value={filters.search}
              onChange={(e) => { setPage(0); setFilters({ ...filters, search: e.target.value }) }}
              placeholder="sharma" />
          </Field>
          <Field label="Status">
            <Select value={filters.status} options={STATUSES}
              onChange={(v) => { setPage(0); setFilters({ ...filters, status: v }) }} />
          </Field>
          <Field label="Gender">
            <Select value={filters.gender} options={GENDERS}
              onChange={(v) => { setPage(0); setFilters({ ...filters, gender: v }) }} />
          </Field>
          <Field label="Placed in a class" hint="The start-of-term question. Everything is false until #14.">
            <Select value={filters.placed} options={['', 'true', 'false']}
              onChange={(v) => { setPage(0); setFilters({ ...filters, placed: v }) }} />
          </Field>
          <Field label="Came from admissions" hint="true is a child CRM #33 enrolled. false is a transfer or a walk-in.">
            <Select value={filters.fromAdmissions} options={['', 'true', 'false']}
              onChange={(v) => { setPage(0); setFilters({ ...filters, fromAdmissions: v }) }} />
          </Field>
          <Field label="Sort"
            hint="fullName · admissionNo · admissionDate · createdAt. Anything else is 400 — the allowlist is a security control.">
            <Input value={filters.sort}
              onChange={(e) => { setPage(0); setFilters({ ...filters, sort: e.target.value }) }}
              placeholder="admissionDate,desc" />
          </Field>
        </div>
        <p className="muted">
          <Info size={12} /> <b>The sort allowlist is not a convenience.</b> An open sort field lets
          a caller order the roll by a date of birth and read the values back out of the ordering
          without this endpoint ever returning them. Type <span className="mono">dateOfBirth</span>{' '}
          to see it refuse.
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

/**
 * #6 — the duplicate check, above the roll because it is asked BEFORE an admission.
 *
 * IT SEARCHES THE GUARDIANS, which is the thing worth seeing: a seven year old has no phone, so
 * the number the school holds is their mother's. Type a parent's number here and their children
 * come back.
 */
function KnownChild() {
  const { call } = useApi()
  const [asked, setAsked] = useState({ phone: '', admissionNo: '', name: '' })
  const [found, setFound] = useState(null)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)

  const search = async () => {
    setBusy(true)
    const result = await call('find-known-child', {
      label: 'Is this child known',
      queryParams: {
        ...(asked.phone ? { phone: asked.phone } : {}),
        ...(asked.admissionNo ? { admissionNo: asked.admissionNo } : {}),
        ...(asked.name ? { name: asked.name } : {}),
      },
    })
    setBusy(false)
    if (result.ok) { setFound(result.bodyJson ?? []); setProblem(null) }
    else { setFound(null); setProblem(result) }
  }

  return (
    <Card
      title="Is this child already here?"
      description="#6 — asked before every admission. It searches the child's own number AND their guardians', because a seven year old has no phone."
      action={<EndpointTag id="find-known-child" name="Search" />}
    >
      <div className="field-grid">
        <Field label="Phone" hint="Any shape. Ten digits or more compares on the last ten.">
          <Input value={asked.phone} onChange={(e) => setAsked({ ...asked, phone: e.target.value })}
            placeholder="098765-43210" />
        </Field>
        <Field label="Admission number" hint="Whole and case-insensitive — a question about identity.">
          <Input value={asked.admissionNo}
            onChange={(e) => setAsked({ ...asked, admissionNo: e.target.value })}
            placeholder="ADM/2026/09/000001" />
        </Field>
        <Field label="Name" hint="Matches anywhere. A name is not an identifier.">
          <Input value={asked.name} onChange={(e) => setAsked({ ...asked, name: e.target.value })}
            placeholder="sharma" />
        </Field>
      </div>
      <div className="toolbar">
        <span className="toolbar-spacer" />
        {/* NEVER DISABLED. Sending all three blank is 400 NOTHING_TO_SEARCH_FOR, which is a real
            refusal worth reaching — a search for nothing would be the whole roll. */}
        <Button look="primary" icon={UserSearch} onClick={search} busy={busy}>Check</Button>
      </div>

      {problem ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok="false">
              {problem.bodyJson?.code ?? problem.status}
            </span>
          </div>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </div>
      ) : found === null ? null : found.length === 0 ? (
        <p className="muted">
          <Info size={12} /> <b>Nobody.</b> Safe to admit — and if that was a parent&rsquo;s number,
          it means the school has no sibling on the roll either.
        </p>
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Admission no</th><th>Child</th><th>Born</th><th>Status</th></tr>
            </thead>
            <tbody>
              {found.map((one) => (
                <tr key={one.studentDocsId}>
                  <td><span className="mono">{one.admissionNo}</span></td>
                  <td>{one.fullName}</td>
                  <td>{one.dateOfBirth}</td>
                  <td><Badge tone={TONE[one.status]}>{one.status}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="muted">
        <Info size={12} /> <b>This is why Admit does not refuse duplicates itself.</b> Refusing
        there would mean deciding that two children sharing a surname and a phone number are one
        child, <i>which siblings are not</i>. The judgement is yours; this shows you what you need
        to make it.
      </p>
    </Card>
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
    guardians: guardians.map((g) => ({
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
              <Field label={`Contact ${i + 1} — full name`} required>
                <Input value={g.fullName} onChange={(e) => setG(i, { fullName: e.target.value })} />
              </Field>
              <Field label="Relation" required>
                <Select value={g.relation} options={RELATIONS}
                  onChange={(v) => setG(i, { relation: v })} />
              </Field>
              <Field label="Phone" hint="THE MATCH KEY. Stored without spaces and brackets.">
                <Input value={g.phoneNumber}
                  onChange={(e) => setG(i, { phoneNumber: e.target.value })} />
              </Field>
              <Field label="Email" hint="The second match key, tried only when the phone finds nobody.">
                <Input value={g.emailAddress}
                  onChange={(e) => setG(i, { emailAddress: e.target.value })} />
              </Field>
              <Field label="Alternate phone" hint="NOT a match key — a shared family landline would make two people one.">
                <Input value={g.alternatePhoneNumber}
                  onChange={(e) => setG(i, { alternatePhoneNumber: e.target.value })} />
              </Field>
              <Field label="Occupation">
                <Input value={g.occupation}
                  onChange={(e) => setG(i, { occupation: e.target.value })} />
              </Field>
              <Field label="Address" wide>
                <Input value={g.address} onChange={(e) => setG(i, { address: e.target.value })} />
              </Field>
            </div>
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
                leave the father exactly as he is — or type his number differently — and send it
                again. He comes back <span className="mono">matched</span> with the same{' '}
                <span className="mono">guardianDocsId</span>, and his stored name is <i>not</i>{' '}
                overwritten. The form is deliberately not cleared so you can.
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
