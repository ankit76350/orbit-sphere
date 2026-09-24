import { useCallback, useEffect, useState } from 'react'
import { Info, Plus, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { detailPath } from '../../../paths.js'

/**
 * #8 and #13 — capture a lead, and the counsellor's worklist. /school-crm/inquiries
 *
 * THE WORKLIST IS THE SCREEN NOW. It had no list while #13 did not exist, and the page said so
 * rather than drawing an empty table — a state this project prefers to a feature that is not
 * one. #13 is built, so the table is real and the session-only list it replaced is gone.
 *
 * OVERDUE IS THE FILTER WORTH UNDERSTANDING, and it is not "has a past date". A lead somebody gave
 * up on last month has one of those too, and nobody owes it a phone call. So it is past its date
 * AND still worth chasing — and the screen says so, because the difference is the whole point.
 *
 * THERE IS NO "MINE". Nothing in this project knows who is asking yet, so whose worklist it is has
 * to be typed in. That is a gap in the product, not in the screen, and naming it is the honest way
 * to show it.
 *
 * ALMOST EVERY FIELD ON THE CAPTURE FORM IS OPTIONAL, and it is laid out to say so: the two
 * required ones sit at the top on their own, and everything else is below under a heading that
 * explains why.
 */
const GENDERS = ['', 'MALE', 'FEMALE', 'OTHER']
const RELATIONS = ['', 'FATHER', 'MOTHER', 'GUARDIAN', 'OTHER']
const BLANK_GUARDIAN = { fullName: '', relation: '', phoneNumber: '', emailAddress: '' }
const STATUSES = ['', 'NEW', 'CONTACTED', 'VISIT_SCHEDULED', 'APPLICATION_STARTED',
  'APPLICATION_SUBMITTED', 'LOST', 'CLOSED']
const TONE = { APPLICATION_SUBMITTED: 'good', LOST: 'bad', CLOSED: 'bad', NEW: 'warn' }

export default function Inquiries() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()

  const [capturing, setCapturing] = useState(false)
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({
    status: '', overdue: '', assignedCounselorDocsId: '', academicYear: '', search: '',
  })

  const query = {
    page, size: 20,
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.overdue ? { overdue: filters.overdue } : {}),
    ...(filters.assignedCounselorDocsId
      ? { assignedCounselorDocsId: filters.assignedCounselorDocsId }
      : {}),
    ...(filters.academicYear ? { academicYear: filters.academicYear } : {}),
    ...(filters.search ? { search: filters.search } : {}),
  }

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-inquiries', { label: "The counsellor's worklist", query })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, page, filters.status, filters.overdue,
    filters.assignedCounselorDocsId, filters.academicYear, filters.search])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Inquiries" />

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Inquiries</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · soonest to chase first'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-inquiries" name="Worklist" query={query} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="create-inquiry" name="Capture" look="primary" />
        <Button look="primary" icon={Plus} onClick={() => setCapturing(true)}>
          Capture a lead
        </Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        </Card>
      ) : null}

      <Card
        title="What this screen can and cannot do"
        description="Three endpoints of the ten the lead half has."
      >
        <p className="muted">
          <Info size={12} /> <b>#8, #13 and #14 are built; #9 to #12, #15 and #16 are not.</b>{' '}
          Nothing logs a call, sets the next chase date, hands a lead to a counsellor or marks one
          lost — so every row below was captured by #8 and has stood still since. That is why
          most of them read <span className="mono">NEW</span> with nobody promising to ring.
        </p>
        <p className="muted">
          <Info size={12} /> <b>What capturing a lead unblocked.</b> Until #8 existed nothing could
          create an inquiry through the API — yet #17 moves a named lead to{' '}
          <span className="mono">APPLICATION_STARTED</span> and #19 to{' '}
          <span className="mono">APPLICATION_SUBMITTED</span>. Those were two write paths in built
          endpoints that no call could reach. Capture one here, then name it on{' '}
          <b>Start an application</b> and watch the lead move — on this list.
        </p>
      </Card>

      <Card
        title="Narrow it"
        description="status and the counsellor are the two keys school_inquiry_pipeline_idx leads with, in its order — together they are one person's open leads."
        action={<Badge>{data?.totalElements ?? 0} leads</Badge>}
      >
        <div className="field-grid">
          <Field label="Status" hint="One state. A lead moves through these by being worked on — #10 and #12 do that and are not built, so most rows will read NEW.">
            <Select
              value={filters.status}
              options={STATUSES.map((one) => ({ value: one, label: one === '' ? 'any' : one }))}
              label="Status"
              onChange={(v) => { setPage(0); setFilters((f) => ({ ...f, status: v })) }}
            />
          </Field>
          <Field label="Overdue" hint="NOT 'has a past date'. Past its follow-up date AND not LOST or CLOSED — a lead somebody gave up on last month has a past date too, and nobody owes it a call.">
            <Select
              value={filters.overdue}
              options={[
                { value: '', label: 'either' },
                { value: 'true', label: 'late — past its date and still open' },
                { value: 'false', label: 'not late — the given-up ones included' },
              ]}
              label="Overdue"
              onChange={(v) => { setPage(0); setFilters((f) => ({ ...f, overdue: v })) }}
            />
          </Field>
          <Field label="Academic year" hint="One intake's leads. A school runs more than one at a time, and a lead is usually about next year rather than this one.">
            <Input value={filters.academicYear} placeholder={actingAcademicYear ?? '2027-2028'}
              onChange={(e) => {
                setPage(0)
                setFilters((f) => ({ ...f, academicYear: e.target.value }))
              }} />
          </Field>
        </div>
        <div className="field-grid">
          <Field label="Counsellor's staff id" hint="THERE IS NO 'MINE'. Nothing here knows who is asking yet, so whose worklist it is has to be typed in. That is a gap in the product, not in this screen.">
            <Input value={filters.assignedCounselorDocsId}
              onChange={(e) => {
                setPage(0)
                setFilters((f) => ({ ...f, assignedCounselorDocsId: e.target.value }))
              }} />
          </Field>
          <Field label="Search" hint="The child's name OR the inquiry number, anywhere, ignoring case — the parent on the phone gives a name, the note on the pad carries a number. Send '.*' and nothing comes back: the needle is quoted.">
            <Input value={filters.search} placeholder="Aarav, or INQ/2026/09/000001"
              onChange={(e) => {
                setPage(0)
                setFilters((f) => ({ ...f, search: e.target.value }))
              }} />
          </Field>
        </div>
      </Card>

      <Card
        title="Who to ring"
        description="Soonest to chase first, then by id — the id is the tiebreaker because an inquiry number is only unique per school, and a fallback order has to be total or a row is shown twice while another is never shown."
      >
        {rows.length === 0 ? (
          <Empty
            title="Nothing matches"
            description="A lead is captured by #8. Nothing here has a follow-up date yet either — #10 logs a call and sets the next one, and it is not built."
            action={
              <Button look="primary" icon={Plus} onClick={() => setCapturing(true)}>
                Capture a lead
              </Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Inquiry no</th>
                  <th>Child</th>
                  <th>Year</th>
                  <th>Status</th>
                  <th>With</th>
                  <th>Ring</th>
                  <th>Chase by</th>
                  <th className="num">Calls</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.inquiryId}
                    onClick={() => navigate(detailPath('school', 'crm', 'inquiries',
                      one.inquiryId))}>
                    <td className="mono">{one.inquiryNo}</td>
                    <td>{one.prospectiveStudentName}</td>
                    <td className="mono">{one.academicYear}</td>
                    <td>
                      <Badge tone={TONE[one.status]}>{one.status}</Badge>
                      {one.overdue ? <> <Badge tone="bad">late</Badge></> : null}
                    </td>
                    <td>
                      {one.assignedCounselorName
                        ?? (one.assignedCounselorDocsId
                          ? <span className="muted">no longer staff</span>
                          : <span className="muted">nobody yet</span>)}
                    </td>
                    <td className="mono">
                      {one.contactPhoneNumber ?? <span className="muted">no number</span>}
                    </td>
                    <td title={one.nextFollowUpAt}>
                      {one.nextFollowUpAt
                        ? readable(one.nextFollowUpAt)
                        : <span className="muted">nobody promised</span>}
                    </td>
                    <td className="num">{one.followUpCount}</td>
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
          <Badge>page {(data?.page ?? 0) + 1} of {Math.max(1, data?.totalPages ?? 1)}</Badge>
        </div>

        <p className="muted">
          <Info size={12} /> <b>A lead with no chase date sits at the FRONT.</b> Mongo puts a
          missing field before every value, so the default order puts them first. On a chase list
          that is arguably the wrong end — but a lead nobody has promised to ring is also the
          one most likely to be forgotten, and none of them can ever be <b>late</b>:{' '}
          <span className="mono">$lt</span> does not match a field that is not there.
        </p>
      </Card>

      {capturing ? (
        <CaptureLead
          onClose={() => setCapturing(false)}
          onCaptured={() => load()}
        />
      ) : null}
    </div>
  )
}

/**
 * #8's modal.
 *
 * IT STAYS OPEN AFTER A SUCCESS, like the cycle creator. Leads are captured in a run — the desk
 * takes three calls in a morning — and closing after each one would make the common case the slow
 * one.
 */
function CaptureLead({ onClose, onCaptured }) {
  const { call } = useApi()
  const { actingAcademicYear } = useApiState()

  const [prospectiveStudentName, setName] = useState('')
  const [academicYear, setYear] = useState(actingAcademicYear ?? '')
  const [dateOfBirth, setDob] = useState('')
  const [gender, setGender] = useState('')
  const [interestedClassDocsId, setClass] = useState('')
  const [assignedCounselorDocsId, setCounselor] = useState('')
  const [source, setSource] = useState('')
  const [sourceDetails, setSourceDetails] = useState('')
  const [notes, setNotes] = useState('')
  const [guardiansOn, setGuardiansOn] = useState(false)
  const [guardians, setGuardians] = useState([{ ...BLANK_GUARDIAN }])
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)
  const [done, setDone] = useState(null)

  //! A GUARDIAN ROW IS ONLY SENT IF SOMETHING IS ON IT. The form ships one empty row so the
  //! fields are visible, and an empty row must not become an empty guardian on the document.
  const filledGuardians = guardians
    .map((one) => Object.fromEntries(Object.entries(one).filter(([, v]) => v !== '')))
    .filter((one) => Object.keys(one).length > 0)

  const body = {
    ...(prospectiveStudentName ? { prospectiveStudentName } : {}),
    ...(academicYear ? { academicYear } : {}),
    ...(dateOfBirth ? { dateOfBirth } : {}),
    ...(gender ? { gender } : {}),
    ...(interestedClassDocsId ? { interestedClassDocsId } : {}),
    ...(assignedCounselorDocsId ? { assignedCounselorDocsId } : {}),
    ...(source ? { source } : {}),
    ...(sourceDetails ? { sourceDetails } : {}),
    ...(notes ? { notes } : {}),
    ...(guardiansOn && filledGuardians.length ? { guardians: filledGuardians } : {}),
  }

  const setGuardian = (index, field, value) => setGuardians((old) =>
    old.map((row, n) => (n === index ? { ...row, [field]: value } : row)))

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('create-inquiry', {
      label: `Capture ${prospectiveStudentName || 'a lead'}`,
      body,
    })
    setSaving(false)
    if (result.ok) {
      onCaptured(result.bodyJson)
      setDone(result.bodyJson)
      //! ONLY THE CHILD'S DETAILS ARE CLEARED. The year, the counsellor and the source are the
      //! same for every call in a sitting, and re-typing them would be the friction that stops
      //! the desk using this at all.
      setName(''); setDob(''); setNotes('')
      setGuardians([{ ...BLANK_GUARDIAN }])
    } else {
      setRefused(result.bodyJson ?? {})
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title="Capture a lead"
      description="A name and a year are all it needs. Everything else is optional, because a phone call usually has none of it."
      endpoint={<EndpointTag id="create-inquiry" name="Capture" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Capture it</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code ?? 'refused'}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {done ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{done.inquiryNo}</span>
            </div>
            <pre className="resp-body">{done.nextStep}</pre>
          </div>
        ) : null}

        <Field label="Child's name" required hint="One of the two things this endpoint requires. A lead about nobody is not a lead.">
          <Input value={prospectiveStudentName} onChange={(e) => setName(e.target.value)}
            placeholder="Aarav Sharma" />
        </Field>

        <Field
          label="Academic year"
          required
          hint="The other. It must exist in this school but NEED NOT be the running one — a lead is about the future, which is the normal case rather than the edge."
        >
          <Input value={academicYear} onChange={(e) => setYear(e.target.value)}
            placeholder="2027-2028" />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>Everything below is optional.</b> A phone call is &ldquo;a mother
          rang about her son for next year&rdquo; — a name, a year, and nothing else. An endpoint
          that demanded the rest would be refusing the commonest lead there is.
        </p>

        <div className="field-grid">
          <Field label="Date of birth" hint="A date in the future is 400 VALIDATION_FAILED.">
            <Input type="date" value={dateOfBirth} onChange={(e) => setDob(e.target.value)} />
          </Field>
          <Field label="Gender" hint="Often unknown on a first call.">
            <Select
              value={gender}
              options={GENDERS.map((one) => ({
                value: one, label: one === '' ? 'not said' : one,
              }))}
              label="Gender"
              onChange={setGender}
            />
          </Field>
        </div>

        <Field
          label="Interested class id"
          hint="When the family has one in mind. It has to be a class of the YEAR above — a class of another year, or another school's, is 409 CLASS_NOT_IN_CYCLE_YEAR."
        >
          <Input value={interestedClassDocsId} onChange={(e) => setClass(e.target.value)} />
        </Field>

        <Field
          label="Counsellor's staff id"
          hint="When the desk hands it straight to somebody. Most leads are captured first and assigned after — #11 does that later, and is not built. Not this school's staff is 404 STAFF_NOT_FOUND."
        >
          <Input value={assignedCounselorDocsId} onChange={(e) => setCounselor(e.target.value)} />
        </Field>

        <div className="field-grid">
          <Field label="Source" hint="Free text — WALK_IN, PHONE, REFERRAL. Nothing validates it, because a school names its own channels.">
            <Input value={source} onChange={(e) => setSource(e.target.value)}
              placeholder="WALK_IN" />
          </Field>
          <Field label="Source details" hint="Where they heard about the school.">
            <Input value={sourceDetails} onChange={(e) => setSourceDetails(e.target.value)}
              placeholder="Saw the hoarding on the main road" />
          </Field>
        </div>

        <Field label="Notes" hint="Anything the desk wants to remember.">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="Wants a school bus on the east route." />
        </Field>

        <Field
          label="Guardians"
          hint="Not required — a walk-in who gives a child's name and leaves is a real lead. But the PHONE NUMBER and the EMAIL are what #15 searches on to answer 'is this family already known', and it can only find the ones who left one."
        >
          <Select
            value={guardiansOn ? 'on' : ''}
            options={[
              { value: '', label: 'none — the field is not sent' },
              { value: 'on', label: 'add the rows below' },
            ]}
            label="Guardians"
            onChange={(v) => setGuardiansOn(v === 'on')}
          />
        </Field>

        {guardiansOn ? (
          <div className="stack">
            {guardians.map((one, index) => (
              <div key={index} className="stack">
                <div className="field-grid">
                  <Field label={`Guardian ${index + 1}`} hint="Optional, unlike on an application — the desk often has a first name and nothing more.">
                    <Input value={one.fullName} placeholder="Priya Sharma"
                      onChange={(e) => setGuardian(index, 'fullName', e.target.value)} />
                  </Field>
                  <Field label="Relation" hint="Optional here too.">
                    <Select
                      value={one.relation}
                      options={RELATIONS.map((r) => ({
                        value: r, label: r === '' ? 'not said' : r,
                      }))}
                      label="Relation"
                      onChange={(v) => setGuardian(index, 'relation', v)}
                    />
                  </Field>
                </div>
                <div className="field-grid">
                  <Field label="Phone" hint="What #15 will search on.">
                    <Input value={one.phoneNumber} placeholder="9000000001"
                      onChange={(e) => setGuardian(index, 'phoneNumber', e.target.value)} />
                  </Field>
                  <Field label="Email" hint="The other thing #15 searches on.">
                    <Input value={one.emailAddress} placeholder="priya@example.com"
                      onChange={(e) => setGuardian(index, 'emailAddress', e.target.value)} />
                  </Field>
                </div>
              </div>
            ))}
            <div className="toolbar">
              <Button icon={Plus}
                onClick={() => setGuardians((old) => [...old, { ...BLANK_GUARDIAN }])}>
                Add a guardian
              </Button>
              <Button onClick={() => setGuardians((old) => old.slice(0, -1))}>
                Remove the last
              </Button>
              <span className="toolbar-spacer" />
              <Badge tone={filledGuardians.length === 0 ? 'warn' : undefined}>
                {filledGuardians.length} will be sent
              </Badge>
            </div>
            {filledGuardians.length === 0 ? (
              <p className="muted">
                <Info size={12} /> <b>An empty row is not a guardian.</b> Nothing is typed yet, so
                the field is left out of the body above entirely rather than sent as a blank
                person.
              </p>
            ) : null}
          </div>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>It does not check for duplicates, and it should not.</b> #15 asks
          &ldquo;is this family already known&rdquo; and is asked <i>before</i> this, by the person
          at the desk who can see the answer. Refusing here would mean guessing that two children
          sharing a phone number are one enquiry — <b>which a family with two children is
          not</b>. Capture the same number twice and watch both succeed.
        </p>
      </div>
    </Modal>
  )
}
