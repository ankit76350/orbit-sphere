import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Briefcase, Info, Plus, RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One person, at their own address: /school-people/staff/{id}
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
        <Button look="primary" icon={Plus} onClick={() => setHireOpen(true)}>
          {data?.employment ? 'Promote or transfer' : 'Employ'}
        </Button>
      </div>

      <Card
        title="The person"
        description="Everything #8 returns, in one read. The list at #7 deliberately carries none of it."
        action={<EndpointTag id="get-staff" name="Read" pathParams={{ id }} />}
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
        description="Current and permanent, replaced as a pair by #3 — which is not built."
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
        description="Replaced whole by #4 — which is not built. A new name beside an old number is worse than nothing."
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
            {data?.employment
              ? <Badge tone="good">{data.employment.status}</Badge>
              : <Badge>not employed</Badge>}
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
const STATUSES = ['ACTIVE', 'PROBATION', 'OFFERED', 'ON_LEAVE', 'SUSPENDED', 'NOTICE_PERIOD',
  'TERMINATED']
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
