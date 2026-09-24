import { useState } from 'react'
import { Info, Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * #8 — the front desk captures a lead. /school-crm/inquiries
 *
 * THERE IS NO LIST ON THIS SCREEN, and that is not an omission. #13 is the counsellor's worklist
 * and it is not built, so there is nothing to page through — a table drawn from nothing would be an
 * empty state pretending to be a feature. What this page shows is what was captured in THIS
 * session, which is what a tester needs and is honest about where it came from.
 *
 * ALMOST EVERY FIELD IS OPTIONAL, and the form is laid out to say so: the two required ones sit at
 * the top on their own, and everything else is below under a heading that explains why.
 *
 * THE POINT OF THE GUARDIAN ROWS is the phone number and the email. #15 finds a family again by
 * those two, and it can only find the ones who left one — so the form asks for them even though
 * nothing requires them.
 */
const GENDERS = ['', 'MALE', 'FEMALE', 'OTHER']
const RELATIONS = ['', 'FATHER', 'MOTHER', 'GUARDIAN', 'OTHER']
const BLANK_GUARDIAN = { fullName: '', relation: '', phoneNumber: '', emailAddress: '' }

export default function Inquiries() {
  const { actingSubdomain } = useApiState()
  const [capturing, setCapturing] = useState(false)
  const [captured, setCaptured] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Inquiries" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Inquiries</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · a lead exists before a form does'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="create-inquiry" name="Capture" look="primary" />
        <Button look="primary" icon={Plus} onClick={() => setCapturing(true)}>
          Capture a lead
        </Button>
      </div>

      <Card
        title="What this screen can and cannot do"
        description="One endpoint of the nine the lead half has."
      >
        <p className="muted">
          <Info size={12} /> <b>#8 is built; #9 to #16 are not.</b> There is no list here because
          #13 — the counsellor&rsquo;s worklist — does not exist yet, and a table drawn from
          nothing would be an empty state pretending to be a feature. The rows below are what this
          browser captured in this session.
        </p>
        <p className="muted">
          <Info size={12} /> <b>What capturing a lead unblocked.</b> Until #8 existed nothing could
          create an inquiry through the API — yet #17 moves a named lead to{' '}
          <span className="mono">APPLICATION_STARTED</span> and #19 to{' '}
          <span className="mono">APPLICATION_SUBMITTED</span>. Those were two write paths in built
          endpoints that no call could reach. Capture one here, then name it on{' '}
          <b>Start an application</b> and watch the lead move.
        </p>
      </Card>

      <Card
        title={`Captured here — ${captured.length}`}
        description="This session only. Nothing reads them back yet: #14 opens one lead in full and #13 lists them, and neither is built."
        action={<Badge>{captured.length}</Badge>}
      >
        {captured.length === 0 ? (
          <Empty
            title="Nothing captured yet"
            description="A lead needs a child's name and an academic year. Everything else — the date of birth, the class, the guardians, who is looking after it — is optional, because a phone call usually has none of it."
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
                  <th>Interested in</th>
                  <th>With</th>
                  <th className="num">Guardians</th>
                </tr>
              </thead>
              <tbody>
                {captured.map((one) => (
                  <tr key={one.inquiryId}>
                    <td>
                      <span className="mono">{one.inquiryNo}</span>
                      <br />
                      <span className="mono muted">{one.inquiryId}</span>
                    </td>
                    <td>{one.prospectiveStudentName}</td>
                    <td className="mono">{one.academicYear}</td>
                    <td><Badge tone="warn">{one.status}</Badge></td>
                    <td>
                      {one.interestedClassName
                        ?? <span className="muted">nothing in mind</span>}
                    </td>
                    <td>
                      {one.assignedCounselorName
                        ?? <span className="muted">nobody yet</span>}
                    </td>
                    <td className="num">{(one.guardians ?? []).length}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {capturing ? (
        <CaptureLead
          onClose={() => setCapturing(false)}
          onCaptured={(one) => setCaptured((old) => [one, ...old])}
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
