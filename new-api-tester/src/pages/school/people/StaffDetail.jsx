import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Briefcase, Info, Pencil, Plus, RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One person, at their own address: /school-people/staff/{id}
 *
 * THREE BUTTONS, THREE ENDPOINTS, AND THAT IS THE POINT. "Change status" is 18b — something
 * HAPPENED to them, and it wants the reason. "Correct" is #18 — something was typed wrong, and it
 * refuses to touch the status at all. "Promote or transfer" is #16 — they moved, so this record
 * closes and another opens. Collapsing any two of those would lose the distinction the API is
 * built on.
 *
 * CORRECTING A RECORD IS NOT THE SAME BUTTON AS MOVING SOMEBODY. "Correct" is #18 — it fixes what
 * was typed wrong on the record that is already there. "Promote or transfer" is #16 — it closes
 * that record and opens another. One button for each, because they are different events, and the
 * modal for each says which.
 *
 * EVERYTHING ON THE PERSON IS EDITED FROM ONE MODAL — #2, which absorbed #3, #4 and #5. The
 * address and the contact cards carry their own Edit button because that is where somebody looks
 * for it, but all three open the same form: one endpoint, one write.
 *
 * THE PERSON COMES FIRST, THE EMPLOYMENT LAST. Who this is, then what they do here — the order a
 * reader asks them in, and the order #8 itself is documented in.
 *
 * HIRE, PROMOTE AND TRANSFER ARE ONE BUTTON, because #16 is one endpoint, because it is one
 * event. The modal does not ask which: it asks for a position and a start date, and the API closes
 * whatever was current. A form with three tabs would be three chances to describe the same write
 * three ways.
 *
 * WHEN SOMEBODY IS NOT EMPLOYED the card says so in the API's own words rather than showing a
 * blank panel — an absence like that reads as a bug in the page otherwise — and it says that the
 * state is real rather than broken.
 *
 * ONE ENDPOINT FILLS IT. #8 returns everything below in one read.
 *
 * THIS IS THE MOST SENSITIVE SCREEN IN THE PRODUCT, and it is the only one that says so on the
 * page. A date of birth, a home address and an emergency contact, with no authorization anywhere
 * behind them — the module plan calls that its biggest open item, and a tester should see it
 * where the data is rather than in a README.
 *
 * #7's ROW AND #8's RECORD ARE MEANT TO DISAGREE. The list carries none of this, because a list
 * is read by every dropdown. Opening one person is a deliberate act.
 */

const LIST = screenPath('school', 'people', 'staff')

export default function StaffDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [hireOpen, setHireOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [recordOpen, setRecordOpen] = useState(false)
  const [statusOpen, setStatusOpen] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-staff', {
      label: 'One person in full',
      pathParams: { id },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="This person" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All staff</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'STAFF_NOT_FOUND'
                ? 'No staff member with this id in this school. A person belongs to one school, '
                  + "so another school's real id is a 404 rather than somebody else's home "
                  + 'address — and the refusal says nothing about them either.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  const address = (one) => {
    if (!one) return <span className="muted">none on file</span>
    const parts = [one.addressLine1, one.addressLine2, one.city, one.stateOrProvince,
      one.postalCode, one.countryCode].filter(Boolean)
    return parts.join(', ')
  }

  return (
    <div className="page stack">
      <Link className="back" to={LIST}><ArrowLeft size={13} /> All staff</Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.fullName ?? 'Reading the person'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data ? <> · <span className="mono">{data.employeeNo}</span></> : null}
            {/* READ FROM THE RECORD, not assumed. This said "entered, not yet employed" on
                every person, including ones the card below showed as ACTIVE. */}
            {data
              ? (data.employment
                  ? ` · ${data.employment.status.toLowerCase().replace('_', ' ')}`
                  : ' · entered, not yet employed')
              : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button icon={Pencil} onClick={() => setEditOpen(true)}>Edit</Button>
        <Button look="primary" icon={Plus} onClick={() => setHireOpen(true)}>
          {data?.employment ? 'Promote or transfer' : 'Employ'}
        </Button>
      </div>

      <Card
        title="The person"
        description="Everything #8 returns, in one read. The list at #7 deliberately carries none of it."
        action={
          <div className="btn-row">
            <EndpointTag id="get-staff" name="Read" pathParams={{ id }} />
            <EndpointTag id="update-staff" name="Edit" pathParams={{ id }} />
            <Button icon={Pencil} onClick={() => setEditOpen(true)}>Edit</Button>
          </div>
        }
      >
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Employee no</td>
                <td><span className="mono">{data?.employeeNo}</span></td></tr>
              <tr><td className="muted">Name</td><td>{data?.fullName}</td></tr>
              <tr><td className="muted">Date of birth</td><td>{data?.dateOfBirth}</td></tr>
              <tr><td className="muted">Gender</td><td>{data?.gender}</td></tr>
              <tr><td className="muted">Phone</td>
                <td>{data?.phoneNumber
                  ? <span className="mono">{data.phoneNumber}</span>
                  : <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Email</td>
                <td>{data?.emailAddress ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Nationality</td>
                <td>{data?.nationalityCode ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Preferred language</td>
                <td>{data?.preferredLanguage ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Staff id</td>
                <td><span className="mono">{data?.staffDocsId}</span>{' '}
                  <span className="muted">what every other collection stores</span></td></tr>
            </tbody>
          </table>
        </div>
      </Card>

      <Card
        title="Addresses"
        description="Replaced WHOLE by #2, never merged — what you leave out is gone. The plan gave this to #3; it was folded in."
        action={<Button icon={Pencil} onClick={() => setEditOpen(true)}>Edit</Button>}
      >
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Current</td><td>{address(data?.currentAddress)}</td></tr>
              <tr><td className="muted">Permanent</td><td>{address(data?.permanentAddress)}</td></tr>
            </tbody>
          </table>
        </div>
        <p className="muted">
          <Info size={12} /> <b>An address with nothing in it was stored as no address</b>, so
          &quot;none on file&quot; here means exactly that — not an object of six empty fields.
        </p>
      </Card>

      <Card
        title="Emergency contact"
        description="Replaced WHOLE by #2. A new name beside an old number is worse than nothing, which is why it is never merged."
        action={<Button icon={Pencil} onClick={() => setEditOpen(true)}>Edit</Button>}
      >
        {data?.emergencyContact ? (
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Name</td>
                  <td>{data.emergencyContact.fullName ?? <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Relationship</td>
                  <td>{data.emergencyContact.relationship ?? <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Phone</td>
                  <td>{data.emergencyContact.phoneNumber
                    ? <span className="mono">{data.emergencyContact.phoneNumber}</span>
                    : <span className="muted">none</span>}</td></tr>
              </tbody>
            </table>
          </div>
        ) : (
          <Empty title="None on file" description="Nobody is recorded for this person." />
        )}
      </Card>

      {/* THE WARNING GOES WHERE THE DATA IS. A README nobody opens is not a control. */}
      <Card
        title="Employment"
        description="What they do here — the current record, folded into #8 rather than fetched separately."
        action={
          <div className="btn-row">
            <EndpointTag id="employ-staff" name="Hire, promote or transfer" pathParams={{ id }} />
            {data?.employment ? (
              <EndpointTag id="employment-status" name="Change status"
                pathParams={{ id: data.employment.employmentDocsId }} />
            ) : null}
            {data?.employment ? (
              <EndpointTag id="update-employment" name="Correct this record"
                pathParams={{ id: data.employment.employmentDocsId }} />
            ) : null}
            {data?.employment
              ? <Badge tone="good">{data.employment.status}</Badge>
              : <Badge>not employed</Badge>}
            {data?.employment ? (
              <Button onClick={() => setStatusOpen(true)}>Change status</Button>
            ) : null}
            {data?.employment ? (
              <Button icon={Pencil} onClick={() => setRecordOpen(true)}>Correct</Button>
            ) : null}
            <Button icon={Plus} onClick={() => setHireOpen(true)}>
              {data?.employment ? 'Promote or transfer' : 'Employ'}
            </Button>
          </div>
        }
      >
        {data?.employment ? (
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Position</td>
                  <td><span className="mono">{data.employment.positionDocsId}</span></td></tr>
                <tr><td className="muted">Status</td>
                  <td><Badge tone="good">{data.employment.status}</Badge></td></tr>
                <tr><td className="muted">Why</td>
                  <td>{data.employment.statusReason
                    ?? <span className="muted">no reason recorded — the status needed none</span>}</td></tr>
                <tr><td className="muted">Type</td><td>{data.employment.employmentType}</td></tr>
                <tr><td className="muted">Since</td><td>{data.employment.effectiveFrom}</td></tr>
                <tr><td className="muted">Until</td>
                  <td>{data.employment.effectiveUntil
                    ?? <span className="muted">open — this record is current</span>}</td></tr>
                <tr><td className="muted">Probation until</td>
                  <td>{data.employment.probationUntil ?? <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Manager</td>
                  <td>{data.employment.managerDocsId
                    ? <span className="mono">{data.employment.managerDocsId}</span>
                    : <span className="muted">nobody recorded</span>}</td></tr>
                <tr><td className="muted">Record id</td>
                  <td><span className="muted mono">{data.employment.employmentDocsId}</span></td></tr>
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title="Entered, but not employed"
            description={data?.employmentNote
              ?? 'This person has no current employment record. #16 is what employs them.'}
            action={<Button look="primary" icon={Plus} onClick={() => setHireOpen(true)}>Employ</Button>}
          />
        )}
        <p className="muted">
          <Briefcase size={12} /> <b>Hire, promote and transfer are one write</b>, because they are
          one event: #16 closes whichever record was current and opens a new one, inside a
          transaction. Three endpoints doing that would be three chances to leave two records
          current — or none, which is worse, because the person then reads as unemployed.
        </p>
        <p className="muted">
          <Info size={12} /> <b>When there is none, the block is absent rather than empty</b> — no{' '}
          <span className="mono">employment</span> key, not <span className="mono">null</span> and
          not <span className="mono">{'{}'}</span>. Three ways of saying &quot;nothing here&quot;
          is three cases a client has to handle, and that state is real: somebody entered and not
          yet hired, which is what #1 leaves them in and #17 returns them to.
        </p>
      </Card>

      <ChangeStatus
        open={statusOpen}
        record={data?.employment}
        onClose={() => setStatusOpen(false)}
        onSaved={load}
      />

      <EditEmployment
        open={recordOpen}
        record={data?.employment}
        onClose={() => setRecordOpen(false)}
        onSaved={load}
      />

      <EditStaff
        open={editOpen}
        staffDocsId={id}
        person={data}
        onClose={() => setEditOpen(false)}
        onSaved={load}
      />

      <EmployStaff
        open={hireOpen}
        staffDocsId={id}
        person={data}
        onClose={() => setHireOpen(false)}
        onSaved={load}
      />

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>This is the fullest thing the product returns about a human
          being, and nothing checks who is asking.</b> A date of birth, a home address and an
          emergency contact come back to anybody who can reach the API with a school subdomain.
          The module plan calls authorization its open item that matters most, and this is the
          endpoint it means.
        </p>
        <p className="muted">
          <Info size={12} /> The API repeats it on every response:{' '}
          <span className="mono">{data?.note ?? '—'}</span>
        </p>
      </Card>
    </div>
  )
}

/**
 * Hire, promote or transfer — #16.
 *
 * ONE FORM, NOT THREE. The endpoint does not ask which of the three this is, and neither does
 * this: it asks for a position and a start date, and the API closes whatever was current. A form with
 * three tabs would be three ways to describe one write.
 *
 * NO effectiveUntil BOX, and that is not an omission. The previous record's end is computed — the
 * day before this one starts — because two people typing two dates is how a gap or an overlap
 * gets in. The field is not on the request record at all.
 *
 * NO current BOX EITHER. The record this creates is current by definition; that is what the
 * endpoint means.
 *
 * TERMINATED IS OFFERED IN THE LIST ANYWAY, because the API refuses it and a tester needs to
 * reach that refusal. Leaving it out would hide a documented 400 behind a dropdown.
 */
// MIRRORS EmploymentStatus. OFFERED left the enum on 2026-09-16; RETIRED joined it. The two
// lists below are what the API enforces — shown here so the form can say which box is required
// before the request is sent, never so it can refuse one itself.
const STATUSES = ['ACTIVE', 'PROBATION', 'ON_LEAVE', 'SUSPENDED', 'NOTICE_PERIOD', 'TERMINATED',
  'RETIRED']
const NEEDS_REASON = ['ON_LEAVE', 'SUSPENDED', 'NOTICE_PERIOD', 'TERMINATED', 'RETIRED']
const TERMINAL = ['TERMINATED', 'RETIRED']
const TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'SUBSTITUTE', 'INTERN']

function EmployStaff({ open, staffDocsId, person, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = {
    positionDocsId: '',
    status: 'ACTIVE',
    employmentType: 'FULL_TIME',
    effectiveFrom: '',
    managerDocsId: '',
    probationUntil: '',
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, staffDocsId])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = {
      positionDocsId: current.positionDocsId,
      status: current.status,
      employmentType: current.employmentType,
      effectiveFrom: current.effectiveFrom,
    }
    if (current.managerDocsId.trim() !== '') out.managerDocsId = current.managerDocsId.trim()
    if (current.probationUntil !== '') out.probationUntil = current.probationUntil
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('employ-staff', {
      label: 'Hire, promote or transfer',
      pathParams: { id: staffDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onSaved()
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  const already = person?.employment

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title={already ? 'Promote or transfer' : 'Employ this person'}
      description="One write, because it is one event — the API closes whichever record is current and opens a new one."
      endpoint={<EndpointTag id="employ-staff" name="Save" look="primary"
        pathParams={{ id: staffDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {made.closed ? 'moved positions' : 'employed'}
              </span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        {/* BOTH HALVES. The closed record is the thing a caller could not have known. */}
        {made?.closed ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">previous record closed</span>
            </div>
            <pre className="resp-body">
              {`position ${made.closed.positionDocsId}
ran ${made.closed.effectiveFrom} to ${made.closed.effectiveUntil}
current: ${made.closed.current}

The end date was computed — the day before the new one starts — never sent.`}
            </pre>
          </div>
        ) : null}

        {/* Rides on a SUCCESSFUL response, apart from the refusal, so it never reads as failure. */}
        {made?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">saved, with a warning</span>
            </div>
            <pre className="resp-body">{made.warning}</pre>
          </div>
        ) : null}

        {already ? (
          <p className="muted">
            <Info size={12} /> They currently hold{' '}
            <span className="mono">{already.positionDocsId}</span> since{' '}
            {already.effectiveFrom}. <b>Saving closes that record</b> the day before the date
            below, in the same transaction.
          </p>
        ) : null}

        <div className="field-grid">
          <Field
            label="Position id"
            required
            hint="Must be a position of this school, and ACTIVE — a retired one is 409 POSITION_NOT_ACTIVE."
            error={errors.positionDocsId}
          >
            <Input value={current.positionDocsId} error={errors.positionDocsId}
              onChange={set('positionDocsId')} placeholder="from Create Position" />
          </Field>
          <Field
            label="Effective from"
            required
            hint="May be past or future. It must be AFTER the current record's own start, or closing that record would end it before it began."
            error={errors.effectiveFrom}
          >
            <Input type="date" value={current.effectiveFrom} error={errors.effectiveFrom}
              onChange={set('effectiveFrom')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Status"
            required
            hint="OFFERED is the one that matters: accepted an offer, not started, and must not appear in a teacher picker. TERMINATED is REFUSED — try it."
            error={errors.status}
          >
            <Select label="Status" value={current.status}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), status: value }))}
              options={STATUSES} />
          </Field>
          <Field label="Employment type" required error={errors.employmentType}>
            <Select label="Employment type" value={current.employmentType}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), employmentType: value }))}
              options={TYPES} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Manager (staff id)"
            hint="A PERSON, not a position — Position.reportsToPositionDocsId answers the structural question. Their own id is 400 MANAGER_IS_SELF."
            error={errors.managerDocsId}
          >
            <Input value={current.managerDocsId} error={errors.managerDocsId}
              onChange={set('managerDocsId')} placeholder="leave blank for nobody" />
          </Field>
          <Field
            label="Probation until"
            hint="Cannot be before the start date."
            error={errors.probationUntil}
          >
            <Input type="date" value={current.probationUntil} error={errors.probationUntil}
              onChange={set('probationUntil')} />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> <b>There is no end-date box, and that is not an omission.</b> The
          previous record&apos;s end is computed — the day before this one starts — because two
          people typing two dates is how a gap or an overlap gets in. Send{' '}
          <span className="mono">effectiveUntil</span>, <span className="mono">current</span> or{' '}
          <span className="mono">separationReason</span> anyway and they are{' '}
          <b>ignored, not refused</b>.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Filling a position past its approved headcount is a warning on a
          201</b>, never a refusal — a twelfth teacher in eleven positions has already happened, and
          refusing it would stop the system recording the truth.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Editing a person — #2.
 *
 * ONE FORM FOR THE WHOLE PROFILE, because #2 absorbed #3, #4 and #5. Three modals would be three
 * ways to describe one write.
 *
 * IT SENDS WHAT YOU CHANGED, so an untouched field never appears in the body — and saving without
 * typing produces `{}`, which is the documented 400 NOTHING_TO_UPDATE.
 *
 * AN ADDRESS AND THE CONTACT ARE SENT WHOLE, and that is the one place "only what changed" does
 * NOT apply. Touch any box in an address and the whole address goes, because the API replaces it
 * rather than merging — sending only the changed field would delete the rest. The form says so.
 *
 * CLEARING IS EMPTYING EVERY BOX IN THE GROUP: that sends `{}`, which is how the API removes one.
 *
 * NO employeeNo BOX. It is generated and printed on things; sending it is ignored rather than
 * refused, and offering a box would suggest otherwise.
 */
const EDIT_GENDERS = ['FEMALE', 'MALE', 'OTHER']

function EditStaff({ open, staffDocsId, person, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const addr = (one) => ({
    addressLine1: one?.addressLine1 ?? '',
    city: one?.city ?? '',
    postalCode: one?.postalCode ?? '',
    countryCode: one?.countryCode ?? '',
  })

  const initial = {
    fullName: person?.fullName ?? '',
    dateOfBirth: person?.dateOfBirth ?? '',
    gender: person?.gender ?? 'FEMALE',
    nationalityCode: person?.nationalityCode ?? '',
    preferredLanguage: person?.preferredLanguage ?? '',
    phoneNumber: person?.phoneNumber ?? '',
    emailAddress: person?.emailAddress ?? '',
    profileImageDocsId: person?.profileImageDocsId ?? '',
    current: addr(person?.currentAddress),
    permanent: addr(person?.permanentAddress),
    contactName: person?.emergencyContact?.fullName ?? '',
    contactRelationship: person?.emergencyContact?.relationship ?? '',
    contactPhone: person?.emergencyContact?.phoneNumber ?? '',
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, person?.staffDocsId, person?.fullName])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))
  const setAddr = (group, field) => (event) =>
    setForm((old) => {
      const base = old ?? initial
      return { ...base, [group]: { ...base[group], [field]: event.target.value } }
    })

  const strip = (group) => {
    const out = {}
    for (const [k, v] of Object.entries(group)) if (v.trim() !== '') out[k] = v.trim()
    return out
  }
  const same = (a, b) => JSON.stringify(a) === JSON.stringify(b)

  const body = (() => {
    const out = {}
    for (const f of ['fullName', 'dateOfBirth', 'gender', 'nationalityCode', 'preferredLanguage',
      'phoneNumber', 'emailAddress', 'profileImageDocsId']) {
      if (current[f] !== initial[f]) out[f] = current[f]
    }
    // WHOLE, not per-field. The API replaces an address; sending only the box you touched would
    // delete every other line on it.
    if (!same(current.current, initial.current)) out.currentAddress = strip(current.current)
    if (!same(current.permanent, initial.permanent)) out.permanentAddress = strip(current.permanent)

    const contactNow = { fullName: current.contactName, relationship: current.contactRelationship,
      phoneNumber: current.contactPhone }
    const contactWas = { fullName: initial.contactName, relationship: initial.contactRelationship,
      phoneNumber: initial.contactPhone }
    if (!same(contactNow, contactWas)) out.emergencyContact = strip(contactNow)
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-staff', {
      label: 'Edit a person',
      pathParams: { id: staffDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) { setMade(result.bodyJson); onSaved(); return }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title={person?.fullName ? `Edit ${person.fullName}` : 'Edit this person'}
      description="Only what you change is sent — except an address or the contact, which go WHOLE because the API replaces them."
      endpoint={<EndpointTag id="update-staff" name="Save" look="primary"
        pathParams={{ id: staffDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{made.fullName}</span>
            </div>
            <pre className="resp-body">Saved. The page below was re-read from #8.</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field label="Full name" hint="Empty is refused — STAFF_NAME_REQUIRED. Try it."
            error={errors.fullName}>
            <Input value={current.fullName} error={errors.fullName} onChange={set('fullName')} />
          </Field>
          <Field label="Date of birth" hint="Correctable, not removable — the model requires one. Must be in the past."
            error={errors.dateOfBirth}>
            <Input type="date" value={current.dateOfBirth} error={errors.dateOfBirth}
              onChange={set('dateOfBirth')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Gender" error={errors.gender}>
            <Select label="Gender" value={current.gender}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), gender: value }))}
              options={EDIT_GENDERS} />
          </Field>
          <Field label="Nationality code"
            hint="A CountryCode — correctable but NOT clearable: '' is not a value an enum takes, and null already means leave it alone."
            error={errors.nationalityCode}>
            <Input value={current.nationalityCode} error={errors.nationalityCode}
              onChange={set('nationalityCode')} placeholder="IN" />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Phone number"
            hint="Required and unique per school. Their OWN number is not a duplicate. Empty or '---' is refused, not cleared."
            error={errors.phoneNumber}>
            <Input value={current.phoneNumber} error={errors.phoneNumber}
              onChange={set('phoneNumber')} />
          </Field>
          <Field label="Email address"
            hint="Required and unique per school. Their own address in another case is a correction, not a collision."
            error={errors.emailAddress}>
            <Input value={current.emailAddress} error={errors.emailAddress}
              onChange={set('emailAddress')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Preferred language" hint="A SchoolLocale. Correctable, not clearable."
            error={errors.preferredLanguage}>
            <Input value={current.preferredLanguage} error={errors.preferredLanguage}
              onChange={set('preferredLanguage')} placeholder="en-IN" />
          </Field>
          <Field label="Photo document id" hint="Empty CLEARS it — the one id field here that can be removed."
            error={errors.profileImageDocsId}>
            <Input value={current.profileImageDocsId} error={errors.profileImageDocsId}
              onChange={set('profileImageDocsId')} />
          </Field>
        </div>

        {/* WHOLE, not per-field. Emptying every box sends {} and clears the address. */}
        <p className="muted">
          <Info size={12} /> <b>An address is replaced, not merged.</b> Change one box and the
          whole address is sent — anything you empty is <b>gone</b>. Empty every box in a group to
          send <span className="mono">{'{}'}</span>, which is how the API clears one.
        </p>

        <div className="field-grid">
          <Field label="Current address line"><Input value={current.current.addressLine1}
            onChange={setAddr('current', 'addressLine1')} /></Field>
          <Field label="Current city"><Input value={current.current.city}
            onChange={setAddr('current', 'city')} /></Field>
        </div>
        <div className="field-grid">
          <Field label="Current postal code"><Input value={current.current.postalCode}
            onChange={setAddr('current', 'postalCode')} /></Field>
          <Field label="Current country"><Input value={current.current.countryCode}
            onChange={setAddr('current', 'countryCode')} placeholder="IN" /></Field>
        </div>

        <div className="field-grid">
          <Field label="Permanent address line"><Input value={current.permanent.addressLine1}
            onChange={setAddr('permanent', 'addressLine1')} /></Field>
          <Field label="Permanent city"><Input value={current.permanent.city}
            onChange={setAddr('permanent', 'city')} /></Field>
        </div>
        <div className="field-grid">
          <Field label="Permanent postal code"><Input value={current.permanent.postalCode}
            onChange={setAddr('permanent', 'postalCode')} /></Field>
          <Field label="Permanent country"><Input value={current.permanent.countryCode}
            onChange={setAddr('permanent', 'countryCode')} placeholder="IN" /></Field>
        </div>

        <div className="field-grid">
          <Field label="Emergency contact name"><Input value={current.contactName}
            onChange={set('contactName')} /></Field>
          <Field label="Relationship"><Input value={current.contactRelationship}
            onChange={set('contactRelationship')} /></Field>
        </div>
        <Field label="Emergency contact phone"
          hint="Replaced whole with the rest of the contact — a new name beside an old number is worse than none.">
          <Input value={current.contactPhone} onChange={set('contactPhone')} />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>There is no employee-number box, and nothing about the job.</b> The
          number is generated and printed on things; the job is{' '}
          <span className="mono">EmploymentRecord</span>, which #16 writes. Send{' '}
          <span className="mono">employeeNo</span> anyway and it is <b>ignored, not refused</b>.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Correcting an employment record — #18.
 *
 * A CORRECTION, NOT AN EVENT, and the modal has to make that obvious: "Promote or transfer" beside
 * it closes this record and opens another. This one fixes what was typed wrong on the record that
 * is already there, and moves nobody.
 *
 * NO POSITION BOX AND NO current BOX. A transfer is a new record — editing the position in place
 * would rewrite where somebody worked last year — and a PATCH that could set `current` is how two
 * records end up current, or none. Both are shown as text with the reason.
 *
 * THE END DATE AND TERMINATED ARE OFFERED EVEN ON A CURRENT RECORD, because the API refuses both
 * and a tester has to be able to reach that refusal. The hints say what will happen.
 */
function EditEmployment({ open, record, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = {
    effectiveFrom: record?.effectiveFrom ?? '',
    effectiveUntil: record?.effectiveUntil ?? '',
    probationUntil: record?.probationUntil ?? '',
    managerDocsId: record?.managerDocsId ?? '',
    status: record?.status ?? 'ACTIVE',
    employmentType: record?.employmentType ?? 'FULL_TIME',
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, record?.employmentDocsId])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))

  // ONLY WHAT MOVED — and {} when nothing did, which is the 400 this endpoint documents.
  const body = (() => {
    const out = {}
    for (const f of Object.keys(initial)) if (current[f] !== initial[f]) out[f] = current[f]
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-employment', {
      label: 'Correct an employment record',
      pathParams: { id: record?.employmentDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) { setMade(result.bodyJson); onSaved(); return }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Correct this record"
      description="A correction, not an event — it fixes what was typed wrong and moves nobody. Promoting or transferring is the other button."
      endpoint={<EndpointTag id="update-employment" name="Save" look="primary"
        pathParams={{ id: record?.employmentDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">corrected</span>
            </div>
            <pre className="resp-body">
              {`${made.effectiveFrom} to ${made.effectiveUntil ?? 'open'}   ${made.status}`}
            </pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Effective from"
            hint="Checked against the NEIGHBOURS, which no index does — nothing in the database compares a start to the previous record's end. A gap is allowed; an overlap is not."
            error={errors.effectiveFrom}
          >
            <Input type="date" value={current.effectiveFrom} error={errors.effectiveFrom}
              onChange={set('effectiveFrom')} />
          </Field>
          <Field
            label="Effective until"
            hint={record?.current
              ? 'This record is CURRENT, so the API refuses an end date — try it. Ending an employment is #17.'
              : 'A closed record’s end is correctable. It cannot be cleared: null already means leave it alone.'}
            error={errors.effectiveUntil}
          >
            <Input type="date" value={current.effectiveUntil} error={errors.effectiveUntil}
              onChange={set('effectiveUntil')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Status"
            hint={record?.current
              ? 'TERMINATED is REFUSED on a current record — current and finished at once. #17 sets both together.'
              : 'A closed record may be marked TERMINATED here.'}
            error={errors.status}
          >
            <Select label="Status" value={current.status}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), status: value }))}
              options={STATUSES} />
          </Field>
          <Field
            label="Employment type"
            hint="Not in the plan's editable list — added because a mistyped contract type has no other fix."
            error={errors.employmentType}
          >
            <Select label="Employment type" value={current.employmentType}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), employmentType: value }))}
              options={TYPES} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Manager (staff id)"
            hint="Empty CLEARS it — they report to nobody. Their own id is 400 MANAGER_IS_SELF."
            error={errors.managerDocsId}
          >
            <Input value={current.managerDocsId} error={errors.managerDocsId}
              onChange={set('managerDocsId')} placeholder="empty for nobody" />
          </Field>
          <Field
            label="Probation until"
            hint="Cannot be before the start date. Cannot be cleared."
            error={errors.probationUntil}
          >
            <Input type="date" value={current.probationUntil} error={errors.probationUntil}
              onChange={set('probationUntil')} />
          </Field>
        </div>

        {/* TEXT, NOT BOXES. Neither can be corrected, and this is where somebody would try. */}
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Position, which #18 never accepts</td>
                <td><span className="mono">{record?.positionDocsId}</span>{' '}
                  <span className="muted">
                    moving somebody to another position is a TRANSFER — a new record, which is
                    #16. Editing it here would rewrite where they worked last year
                  </span></td></tr>
              <tr><td className="muted">Current, which #18 never accepts</td>
                <td>{String(record?.current)}{' '}
                  <span className="muted">
                    a PATCH that could set this is how two records end up current — or none, which
                    is worse, because the person then reads as unemployed
                  </span></td></tr>
            </tbody>
          </table>
        </div>

        <p className="muted">
          <Info size={12} /> Send <span className="mono">current</span> or{' '}
          <span className="mono">positionDocsId</span> anyway and they are{' '}
          <b>ignored, not refused</b> — and a body of only those two is the same{' '}
          <span className="mono">400 NOTHING_TO_UPDATE</span> as an empty one.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Changing an employment status — 18b.
 *
 * THE REASON BOX IS THE ENDPOINT. Five of the seven statuses cannot be set without one, because
 * six months later the status alone answers none of the questions somebody will ask about it.
 *
 * THE FORM SAYS WHICH, IT DOES NOT ENFORCE IT. The required marker follows the chosen status, but
 * Save is always live — the refusal is a documented one and a tester has to be able to reach it.
 *
 * A TERMINAL STATUS CLOSES THE RECORD, and the form warns before it happens rather than after: it
 * is the one choice here that cannot be undone by choosing again, because a closed record refuses
 * every further status change.
 */
function ChangeStatus({ open, record, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = { status: 'ON_LEAVE', reason: '', effectiveUntil: '' }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, record?.employmentDocsId])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))

  const needsReason = NEEDS_REASON.includes(current.status)
  const isTerminal = TERMINAL.includes(current.status)

  const body = (() => {
    const out = { status: current.status }
    if (current.reason.trim() !== '') out.reason = current.reason.trim()
    // Only read for a terminal status, so sending it otherwise would be noise in the preview.
    if (isTerminal && current.effectiveUntil !== '') out.effectiveUntil = current.effectiveUntil
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('employment-status', {
      label: 'Change an employment status',
      pathParams: { id: record?.employmentDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) { setMade(result.bodyJson); onSaved(); return }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Change the status"
      description="Something happened to them — and the reason is what a school needs six months later, not the new value."
      endpoint={<EndpointTag id="employment-status" name="Save" look="primary"
        pathParams={{ id: record?.employmentDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{made.status}</span>
            </div>
            <pre className="resp-body">
              {made.current === false
                ? `The employment ended ${made.effectiveUntil}. This record is closed, and its status cannot change again — what happens next is a new record, which is #16.`
                : (made.statusReason ?? 'No reason recorded — this status needed none.')}
            </pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Status"
            required
            hint="OFFERED was removed from the enum on 2026-09-16 — a future effectiveFrom says 'not started yet' now. RETIRED was added."
            error={errors.status}
          >
            <Select label="Status" value={current.status}
              onChange={(value) => setForm((old) => ({ ...(old ?? initial), status: value }))}
              options={STATUSES} />
          </Field>
          <Field
            label="Reason"
            required={needsReason}
            hint={needsReason
              ? `${current.status} REQUIRES a reason — save without one to see the refusal.`
              : `${current.status} needs no reason, but one sent here is KEPT rather than discarded.`}
            error={errors.reason}
          >
            <Input value={current.reason} error={errors.reason} onChange={set('reason')}
              placeholder={needsReason ? 'what happened' : 'optional'} />
          </Field>
        </div>

        {isTerminal ? (
          <>
            <Field
              label="Last day of employment"
              hint="Only read for a terminal status. Empty means TODAY. Before the record's start is a 400."
              error={errors.effectiveUntil}
            >
              <Input type="date" value={current.effectiveUntil} error={errors.effectiveUntil}
                onChange={set('effectiveUntil')} />
            </Field>
            <p className="muted">
              <Info size={12} /> <b>{current.status} ends the employment.</b> This write sets{' '}
              <span className="mono">current: false</span> and an end date together — it has to,
              because a record that is terminal and current at once is a contradiction nothing in
              the model prevents. <b>A closed record refuses every further status change</b>, so
              what happens after this is a new record, which is #16.
            </p>
          </>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>Five of the seven statuses require a reason</b> —{' '}
          <span className="mono">ON_LEAVE</span>, <span className="mono">SUSPENDED</span>,{' '}
          <span className="mono">NOTICE_PERIOD</span>, <span className="mono">TERMINATED</span>,{' '}
          <span className="mono">RETIRED</span>. The rule lives on the enum, not in a list in the
          service, so a status added later brings its own answer.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Choosing the status it already has is a 409</b>, not a quiet
          success — a write that did nothing would hide a client sending the wrong id, and the
          reason attached would explain something that never happened.
        </p>
      </div>
    </Modal>
  )
}
