import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, GraduationCap, Info, Link2, Link2Off, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import AlreadyTaken from './AlreadyTaken.jsx'
import { detailPath, screenPath } from '../../../paths.js'

/**
 * One child: /school-student/students/{id}
 *
 * TWO ENDPOINTS — #5 reads the child and #2 corrects them. Moving their status is #3 and the
 * guardian endpoints are #11 to #13; neither is built, and the page says so rather than offering
 * buttons that would 404.
 *
 * THE CORRECT BUTTON IS NEVER SWITCHED OFF BY STATUS, and that is not this screen being lax — #2
 * has no status gate at all. A child who left in March can still have a misspelt name put right,
 * because a correction is the school admitting it wrote something down wrong rather than something
 * that happened to the child.
 *
 * THE GUARDIANS ARE WHY THIS PAGE EXISTS. A row on the roll carries a count; this carries the
 * people, resolved in one read. Two documents are shown as one line each: the PERSON (name,
 * number, address), which is shared by every child they belong to, and the ROLE (father, primary,
 * may collect), which belongs to this child alone. The same man is "father, primary, portal" to
 * one child and only an emergency number for their cousin.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth reaching by editing the address bar.
 */

const TONE = { ACTIVE: 'good', WITHDRAWN: 'bad', TRANSFERRED: 'bad', SUSPENDED: 'warn' }
const GENDERS = ['MALE', 'FEMALE', 'OTHER']
// THE WHOLE ENUM, checked against GuardianRelation.java rather than guessed.
const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
  'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']

export default function StudentDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [child, setChild] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [correcting, setCorrecting] = useState(false)
  const [adding, setAdding] = useState(false)
  const [placing, setPlacing] = useState(false)
  //! THE 201 FROM #14, KEPT. It is the only place the class name, the section and the roll number
  //! are ever seen — #5 carries the record's ID and nothing inside it, and the read that would
  //! (#15, #20) is not built. Lost on Refresh, which is honest: it was never on the child.
  const [placement, setPlacement] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-student', {
      label: 'One child in full',
      pathParams: { studentDocsId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setChild(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="A child" />

  const guardians = child?.guardians ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{child?.fullName ?? 'Student'}</h1>
          <p className="muted">
            <span className="mono">{child?.admissionNo ?? id}</span>
            {child ? ` · born ${child.dateOfBirth}` : ' · reading the child…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(screenPath('school', 'student', 'students'))}>
          The roll
        </Button>
        <EndpointTag id="get-student" name="Read" pathParams={{ studentDocsId: id ?? '' }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        {/* NEVER GATED ON THE STATUS. #2 has none — see the note at the top of this file. */}
        <Button look="primary" icon={Pencil} onClick={() => setCorrecting(true)}>Correct it</Button>
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
          <p className="muted">
            <Info size={12} /> <b>Another school&rsquo;s child is this same 404</b>, not a 403 — a
            403 would confirm they exist, and behind it are a date of birth and a family&rsquo;s
            phone numbers. The read is scoped by school <b>in the query</b>, never checked after.
          </p>
        </Card>
      ) : child ? (
        <>
          <Card
            title="The child"
            description="#5 reads it; #2 corrects it. Never the admission number, the status or the guardians."
            action={
              <Button look="primary" icon={Pencil} onClick={() => setCorrecting(true)}>
                Correct it
              </Button>
            }
          >
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><td className="muted">Admission no</td>
                    <td><span className="mono">{child.admissionNo}</span></td></tr>
                  <tr><td className="muted">Name</td><td>{child.fullName}</td></tr>
                  <tr><td className="muted">Status</td>
                    <td><Badge tone={TONE[child.status]}>{child.status}</Badge></td></tr>
                  <tr><td className="muted">Date of birth</td><td>{child.dateOfBirth}</td></tr>
                  <tr><td className="muted">Gender</td><td>{child.gender}</td></tr>
                  <tr><td className="muted">Admitted</td><td>{child.admissionDate}</td></tr>
                  <tr><td className="muted">Nationality</td>
                    <td>{child.nationalityCode ?? <span className="muted">none on file</span>}</td></tr>
                  <tr><td className="muted">Preferred language</td>
                    <td>{child.preferredLanguage ?? <span className="muted">none on file</span>}</td></tr>
                  <tr><td className="muted">Their own phone</td>
                    <td>{child.phoneNumber
                      ? <span className="mono">{child.phoneNumber}</span>
                      : <span className="muted">none — most children have none</span>}</td></tr>
                  <tr><td className="muted">Their own email</td>
                    <td>{child.emailAddress ?? <span className="muted">none on file</span>}</td></tr>
                  <tr><td className="muted">Student id</td>
                    <td><span className="mono">{child.studentDocsId}</span>{' '}
                      <span className="muted">what every other collection will store</span></td></tr>
                  <tr><td className="muted">Version</td><td>{child.version}</td></tr>
                  <tr><td className="muted">Created</td>
                    <td title={child.createdAt}>{readable(child.createdAt)}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card
            title={`Guardians — ${guardians.length}`}
            description="Resolved into people in ONE read. The person is shared across their children; the flags belong to this child alone."
            action={
              <Button look="primary" icon={Plus} onClick={() => setAdding(true)}>
                Add a guardian
              </Button>
            }
          >
            {guardians.length === 0 ? (
              <Empty
                title="No contacts"
                description="#1 refuses a child with no guardian, so this should be impossible — a child here with none means a row written straight into Mongo."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Contact</th>
                      <th>Relation</th>
                      <th>Phone</th>
                      <th>Email</th>
                      <th>For this child</th>
                      <th>Guardian id</th>
                    </tr>
                  </thead>
                  <tbody>
                    {guardians.map((g) => (
                      <tr key={g.guardianDocsId}>
                        <td>
                          {g.fullName}
                          {g.occupation ? <div className="muted">{g.occupation}</div> : null}
                          {g.address ? <div className="muted">{g.address}</div> : null}
                        </td>
                        <td>{g.relation}</td>
                        <td>
                          <span className="mono">{g.phoneNumber ?? '—'}</span>
                          {g.alternatePhoneNumber
                            ? <div className="muted mono">alt {g.alternatePhoneNumber}</div>
                            : null}
                        </td>
                        <td>{g.emailAddress ?? <span className="muted">none</span>}</td>
                        <td>
                          {g.primaryContact ? <Badge tone="good">primary</Badge> : null}{' '}
                          {g.emergencyContact ? <Badge tone="warn">emergency</Badge> : null}{' '}
                          {g.pickupAuthorized ? <Badge>may collect</Badge> : null}{' '}
                          {g.portalAccess ? <Badge>portal</Badge> : null}
                        </td>
                        <td><span className="mono muted">{g.guardianDocsId}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="muted">
              <Info size={12} /> <b>The same person can be on several children.</b> A guardian is
              one real person per school — their phone number says so, and the index enforces it —
              so admitting a sibling links this same{' '}
              <span className="mono">guardianDocsId</span> rather than writing a second row. The
              flags above are <i>this</i> child&rsquo;s; the same man can be an emergency number
              only, on the next one.
            </p>
          </Card>

          <Card
            title="Academic record"
            description="Which class and section they hold, for one year. #14 writes it — the endpoint every roster, mark sheet and timetable waits for."
            action={
              <Button look="primary" icon={GraduationCap} onClick={() => setPlacing(true)}>
                Place in a class
              </Button>
            }
          >
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr>
                    <td className="muted">Placed</td>
                    <td>{child.placed
                      ? <Badge tone="good">yes</Badge>
                      : <span className="muted">not yet</span>}</td>
                  </tr>
                  <tr>
                    <td className="muted">Record id</td>
                    <td>{child.currentAcademicRecordDocsId
                      ? <span className="mono">{child.currentAcademicRecordDocsId}</span>
                      : <span className="muted">none</span>}</td>
                  </tr>
                  {/* EVERYTHING BELOW COMES FROM THE 201, not from the child — see the note. */}
                  {placement ? (
                    <>
                      <tr>
                        <td className="muted">Year</td>
                        <td>{placement.academicYear}</td>
                      </tr>
                      <tr>
                        <td className="muted">Class and section</td>
                        <td><b>{placement.className}</b> {placement.sectionNo}</td>
                      </tr>
                      <tr>
                        <td className="muted">Roll number</td>
                        <td>{placement.rollNo
                          ? <span className="mono">{placement.rollNo}</span>
                          : <span className="muted">none — #14 does not generate one</span>}</td>
                      </tr>
                      <tr>
                        <td className="muted">In effect from</td>
                        <td>{placement.effectiveFrom}</td>
                      </tr>
                      <tr>
                        <td className="muted">Status</td>
                        <td><Badge tone="good">{placement.status}</Badge></td>
                      </tr>
                    </>
                  ) : null}
                </tbody>
              </table>
            </div>

            {child.placed && !placement ? (
              <p className="muted">
                <Info size={12} /> <b>The id is all this page can show.</b> #5 stores the pointer
                and nothing inside the record — the class, the section and the roll number live on
                the record itself, and <b>reading one back is #15 and #20</b>, neither of which is
                built. Place a child from here and the answer fills the rows above; a Refresh
                clears them again, because they were never on the child.
              </p>
            ) : null}

            <p className="muted">
              <Info size={12} /> <b>A child with no class is a normal state, not a half-finished
              one.</b> A school knows it has admitted a child in January without knowing which
              section they are in until June — which is also why <b>#14 runs no gate 4</b>, where
              every other write against a year refuses one that is not running.
            </p>
            <p className="muted">
              <Info size={12} /> <b>One ACTIVE record per child per year, and the index says
              so.</b> Placing a child who is already placed is{' '}
              <span className="mono">409 STUDENT_ALREADY_PLACED</span> — <b>moving them is #17</b>,
              which closes one record and opens another in the same transaction. A{' '}
              <span className="mono">PATCH</span> of the class cannot do it: for an instant two
              active records would exist, and editing in place erases where the child sat for the
              first half of the year, which is what that half&rsquo;s attendance is attached to.
            </p>
          </Card>

          <Card
            title="Where this child came from"
            description="The form they arrived on, if they arrived on one."
          >
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr>
                    <td className="muted">Admission application</td>
                    <td>
                      {child.admissionApplicationDocsId ? (
                        <Button onClick={() => navigate(detailPath('school', 'crm', 'applications',
                          child.admissionApplicationDocsId))}>
                          Open the form
                        </Button>
                      ) : (
                        <span className="muted">
                          none — a transfer, a walk-in, or a child typed straight in
                        </span>
                      )}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="muted">
              <Info size={12} /> <b>The recorded flow is inquiry → admission → student → academic
              record</b>, and a child may join it at any point: a transfer or a walk-in has no
              form behind them, which is a normal state rather than a missing one. Where they go
              next is the card above.
            </p>
          </Card>

          <Card
            title="What cannot be done here yet"
            description="Said plainly rather than offered as buttons that would 404."
          >
            <p className="muted">
              <b>#3 moves a child through the status graph</b> with a reason, and <b>#12 and
              #13</b> change a guardian's flags for one child and detach them. None is built: they
              are phase 8,
              and the reason they waited is that <b>CRM #33 needs none of them</b> — the four
              endpoints this module started with existed to unblock the handover and nothing else.
              <b> #2 was the first thing added beyond that</b>, because a roll nobody can correct
              is a roll that gets worse every week.
            </p>
            <p className="muted">
              <Info size={12} /> <b>The admission date is not correctable either</b>, and that is
              worth knowing rather than discovering: it is not in #2&rsquo;s field list, so a date
              typed wrongly when a school entered its existing roll stays wrong.
            </p>
          </Card>
        </>
      ) : null}

      {/* MOUNTED ONLY WHILE OPEN. Every box is SEEDED from the child — including the version —
          and a component that stayed mounted would keep the values it was first given, so a
          Refresh behind this would leave the version one behind and the next write stale for no
          reason. */}
      {correcting && child ? (
        <CorrectChild
          child={child}
          onClose={() => setCorrecting(false)}
          onCorrected={load}
        />
      ) : null}

      {placing && child ? (
        <PlaceInClass
          child={child}
          onClose={() => setPlacing(false)}
          onPlaced={(answer) => { setPlacement(answer); load() }}
        />
      ) : null}

      {/* MOUNTED ONLY WHILE OPEN, so the version is seeded from the read each time. */}
      {adding && child ? (
        <AddGuardianToChild
          child={child}
          onClose={() => setAdding(false)}
          onAdded={load}
        />
      ) : null}
    </div>
  )
}

/**
 * #2 — correct a child's details.
 *
 * EVERY BOX STARTS FROM THE CHILD, so the ordinary act is editing one field and sending. What is
 * sent is only what CHANGED: a box left exactly as it was is left out of the body entirely, which
 * is what makes "absent means leave it alone" visible in the JSON pane rather than something you
 * have to take on trust.
 *
 * CLEARING IS AN EXPLICIT EMPTY BOX. Delete the contents of the phone and the body carries
 * "phoneNumber": "" — the clear. The three that can be emptied say so; the rest say what they will
 * answer instead.
 *
 * NOTHING IS DISABLED. A blank name, a stale version, "" on a closed set — each is a documented
 * refusal, and this is the tool for reaching them.
 */
function CorrectChild({ child, onClose, onCorrected }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    fullName: child.fullName ?? '',
    dateOfBirth: child.dateOfBirth ?? '',
    gender: child.gender ?? '',
    nationalityCode: child.nationalityCode ?? '',
    preferredLanguage: child.preferredLanguage ?? '',
    phoneNumber: child.phoneNumber ?? '',
    emailAddress: child.emailAddress ?? '',
  })
  const [version, setVersion] = useState(String(child.version ?? 0))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! ONLY WHAT MOVED. A field identical to what was read is absent from the body, which is what
  //! "leave it alone" means — and an emptied box is sent as "", which is the clear. The two cases
  //! look alike in a form and are opposites on the wire, so the rule lives in one place.
  const changed = (key, was) => (form[key] === (was ?? '') ? {} : { [key]: form[key] })

  const body = {
    ...changed('fullName', child.fullName),
    ...changed('dateOfBirth', child.dateOfBirth),
    ...changed('gender', child.gender),
    ...changed('nationalityCode', child.nationalityCode),
    ...changed('preferredLanguage', child.preferredLanguage),
    ...changed('phoneNumber', child.phoneNumber),
    ...changed('emailAddress', child.emailAddress),
    //! ALWAYS SENT, and typed rather than hidden. Clearing the box reaches
    //! 400 VALIDATION_FAILED and typing an older number reaches
    //! 409 CONCURRENT_MODIFICATION, which are both worth being able to press.
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('update-student', {
      label: `Correct ${child.fullName}`,
      pathParams: { studentDocsId: child.studentDocsId },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onCorrected()
  }

  const moves = Object.keys(body).filter((k) => k !== 'version').length

  return (
    <Modal
      open
      onClose={onClose}
      title={`Correct ${child.fullName}`}
      description='A correction is not an event, which is why it is a PATCH. "" clears a field; a box left as it was is left out of the body.'
      endpoint={<EndpointTag id="update-student" name="Correct" look="primary"
        pathParams={{ studentDocsId: child.studentDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <div className="field-grid">
        <Field label="Full name"
          hint='The one field where "" is REFUSED instead of obeyed — a child with no name cannot be searched for.'>
          <Input value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Date of birth" hint="Correctable, never removable — the model requires one.">
          <Input type="date" value={form.dateOfBirth}
            onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
        </Field>
        <Field label="Gender" hint="Correctable, never removable.">
          <Select value={form.gender} options={GENDERS}
            onChange={(v) => setForm({ ...form, gender: v })} />
        </Field>
        <Field label="Nationality"
          hint='Correctable but NOT removable — an enum takes no "". Emptying this box reaches 400 INVALID_VALUE.'>
          <Input value={form.nationalityCode}
            onChange={(e) => setForm({ ...form, nationalityCode: e.target.value })} />
        </Field>
        <Field label="Preferred language" hint="The same limitation.">
          <Input value={form.preferredLanguage}
            onChange={(e) => setForm({ ...form, preferredLanguage: e.target.value })} />
        </Field>
        <Field label="The child's own phone" hint='Empty the box and "" is sent — the clear.'>
          <Input value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
        </Field>
        <Field label="The child's own email" hint='Empty the box to remove it. Trimmed and lowercased on the way in.'>
          <Input value={form.emailAddress}
            onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} />
        </Field>
        <Field label="Version" required wide
          hint="Seeded from the read. Clear it for 400 VALIDATION_FAILED; type an older number for 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>

      <p className="muted">
        <Info size={12} /> <b>{moves} field{moves === 1 ? '' : 's'} will move.</b>{' '}
        {moves === 0
          ? <>Sending this is <span className="mono">400 NOTHING_TO_UPDATE</span> — the version
            alone is not a change, and a PATCH that changes nothing and answers 200 lets a broken
            form look healthy. The button still sends.</>
          : 'Everything else is left out of the body entirely, which is what "leave it alone" means.'}
      </p>
      <p className="muted">
        <Info size={12} /> <b>The admission number, the status and the guardians are not here.</b>{' '}
        The number is printed on certificates; the status is seven moves with preconditions (#3);
        the guardians are their own documents shared between siblings (#11 to #13). Sending any of
        them is not refused — it is <i>ignored</i>, because none is on the request record.
      </p>
      <p className="muted">
        <Info size={12} /> <b>Nor the photo.</b>{' '}
        <span className="mono">profilePhotoDocumentId</span> names a document record, and a file is
        <i> uploaded</i> rather than typed — an id box for it would be the tail end of an endpoint
        that does not exist. Nothing writes that field today, #1 included.
      </p>

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} OK` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <p className="muted">
              <b>{result.bodyJson?.fullName}</b> is corrected, now at version{' '}
              <span className="mono">{result.bodyJson?.version}</span>. The box above has not moved
              — send again and it is <span className="mono">409 CONCURRENT_MODIFICATION</span>,
              which is the guard doing its job.
            </p>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}

/**
 * #11 — put a guardian on this child.
 *
 * IT DOES BOTH JOBS, and you do not have to know which in advance. Type a name and a number and a
 * new guardian is written; but the phone and the email are checked WHILE YOU TYPE, and a number the
 * school already holds gets a red box naming whoever has it. "Link this guardian" turns the form
 * into a link: the id is sent, the stored person is attached, and the typed details are ignored.
 *
 * THE RELATION AND THE FLAGS STAY EDITABLE EITHER WAY, and that is the point — they are facts about
 * this person AND THIS CHILD. The same man is a father here and an emergency number on his niece.
 *
 * PRIMARY IS A SWAP, NOT AN ADDITION. Ticking it clears the flag on the child's other guardians,
 * and the answer says who lost it. The form says so before you send rather than after.
 */
function AddGuardianToChild({ child, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    guardianDocsId: '',
    fullName: '', phoneNumber: '', emailAddress: '', alternatePhoneNumber: '',
    address: '', occupation: '',
    relation: 'FATHER',
    primaryContact: false, emergencyContact: false, pickupAuthorized: false, portalAccess: false,
  })
  const [version, setVersion] = useState(String(child.version ?? 0))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const linked = !!form.guardianDocsId

  //! A LINKED ROW SENDS THE ID AND THE LINK, NOTHING ELSE. The stored person's details are what
  //! the school already holds, and correcting them is #8 — which would change them for every
  //! child they belong to, and is not something attaching one child should do by accident.
  const body = linked
    ? {
      guardianDocsId: form.guardianDocsId,
      relation: form.relation,
      primaryContact: form.primaryContact,
      emergencyContact: form.emergencyContact,
      pickupAuthorized: form.pickupAuthorized,
      portalAccess: form.portalAccess,
      version: version === '' ? undefined : Number(version),
    }
    : {
      fullName: form.fullName,
      relation: form.relation,
      ...(form.phoneNumber ? { phoneNumber: form.phoneNumber } : {}),
      ...(form.emailAddress ? { emailAddress: form.emailAddress } : {}),
      ...(form.alternatePhoneNumber ? { alternatePhoneNumber: form.alternatePhoneNumber } : {}),
      ...(form.address ? { address: form.address } : {}),
      ...(form.occupation ? { occupation: form.occupation } : {}),
      primaryContact: form.primaryContact,
      emergencyContact: form.emergencyContact,
      pickupAuthorized: form.pickupAuthorized,
      portalAccess: form.portalAccess,
      version: version === '' ? undefined : Number(version),
    }

  const send = async () => {
    setSending(true)
    const answer = await call('link-student-guardian', {
      label: `Add a guardian to ${child.fullName}`,
      pathParams: { studentDocsId: child.studentDocsId },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onAdded()
  }

  const takeFound = (found) => setForm({
    ...form,
    guardianDocsId: found.guardianDocsId,
    fullName: found.fullName,
    phoneNumber: found.phoneNumber ?? '',
    emailAddress: found.emailAddress ?? '',
    alternatePhoneNumber: found.alternatePhoneNumber ?? '',
    address: found.address ?? '',
    occupation: found.occupation ?? '',
  })

  const currentPrimary = (child.guardians ?? []).find((g) => g.primaryContact)

  return (
    <Modal
      open
      onClose={onClose}
      title={`Add a guardian to ${child.fullName}`}
      description="Type somebody new, or link somebody the school already holds. The relation and the flags are always this child's."
      endpoint={<EndpointTag id="link-student-guardian" name="Add" look="primary"
        pathParams={{ studentDocsId: child.studentDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      {linked ? (
        <p className="muted">
          <Link2 size={12} /> <b>Linking {form.fullName}</b>, who the school already holds —{' '}
          <span className="mono">{form.guardianDocsId}</span>. Their details are the stored ones and
          are sent as an id rather than retyped. <b>The relation and the flags below are still this
          child&rsquo;s.</b>{' '}
          <Button icon={Link2Off}
            onClick={() => setForm({ ...form, guardianDocsId: '' })}>Unlink</Button>
        </p>
      ) : null}

      <div className="field-grid">
        <Field label="Full name" required={!linked}
          hint={linked ? 'The stored name. Correcting it is #8.' : undefined}>
          <Input value={form.fullName} disabled={linked}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Relation" required
          hint="Always this child's — never read off the guardian, even when linking.">
          <Select value={form.relation} options={RELATIONS}
            onChange={(v) => setForm({ ...form, relation: v })} />
        </Field>
        <Field label="Phone"
          hint="Checked while you type. A number already somebody's is refused unless you link them below.">
          <Input value={form.phoneNumber} disabled={linked}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
          <AlreadyTaken by="phone" value={form.phoneNumber} linked={linked} onLink={takeFound} />
        </Field>
        <Field label="Email" hint="Checked the same way.">
          <Input value={form.emailAddress} disabled={linked}
            onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} />
          <AlreadyTaken by="email" value={form.emailAddress} linked={linked} onLink={takeFound} />
        </Field>
        <Field label="Alternate phone" hint="Not checked — a shared family landline.">
          <Input value={form.alternatePhoneNumber} disabled={linked}
            onChange={(e) => setForm({ ...form, alternatePhoneNumber: e.target.value })} />
        </Field>
        <Field label="Occupation">
          <Input value={form.occupation} disabled={linked}
            onChange={(e) => setForm({ ...form, occupation: e.target.value })} />
        </Field>
        <Field label="Address" wide>
          <Input value={form.address} disabled={linked}
            onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
        <Field label="Version" required wide
          hint="THE CHILD'S, not the guardian's — the link lives in the child's document.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>

      <div className="toolbar">
        <label className="check">
          <input type="checkbox" checked={form.primaryContact}
            onChange={(e) => setForm({ ...form, primaryContact: e.target.checked })} />
          <span>Primary contact</span>
        </label>
        <label className="check">
          <input type="checkbox" checked={form.emergencyContact}
            onChange={(e) => setForm({ ...form, emergencyContact: e.target.checked })} />
          <span>Emergency</span>
        </label>
        <label className="check">
          <input type="checkbox" checked={form.pickupAuthorized}
            onChange={(e) => setForm({ ...form, pickupAuthorized: e.target.checked })} />
          <span>May collect</span>
        </label>
        <label className="check">
          <input type="checkbox" checked={form.portalAccess}
            onChange={(e) => setForm({ ...form, portalAccess: e.target.checked })} />
          <span>Portal</span>
        </label>
      </div>

      {form.primaryContact && currentPrimary ? (
        <p className="muted">
          <Info size={12} /> <b>{currentPrimary.fullName} will stop being the primary contact</b>{' '}
          for this child, in the same write. Two primaries is not a state worth reaching — and
          refusing instead would make &ldquo;this is the person to ring now&rdquo; impossible to
          say. The answer names who was demoted.
        </p>
      ) : null}

      <p className="muted">
        <Info size={12} /> <b>Sending neither an id nor a name is{' '}
        <span className="mono">400</span>.</b> It was a <span className="mono">201</span> until
        2026-10-07 and wrote a guardian with an empty name — a row nobody could find again.
      </p>

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} OK` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <p className="muted">
              <b>{result.bodyJson?.fullName}</b> now has{' '}
              {(result.bodyJson?.guardians ?? []).length} guardian
              {(result.bodyJson?.guardians ?? []).length === 1 ? '' : 's'}.{' '}
              {result.bodyJson?.nextStep}
            </p>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}

/**
 * #14 — put this child in a class and a section.
 *
 * THE THREE PICKERS ARE CHAINED, because the three ids are. A class belongs to a year and a
 * section belongs to a class, so choosing a year is what makes the classes knowable and choosing a
 * class is what makes the sections knowable. Typing them independently is how you reach
 * CLASS_NOT_IN_YEAR and SECTION_NOT_IN_CLASS — which the boxes below still allow, because those
 * are documented refusals and this is the tool for reaching them.
 *
 * THE YEAR IS A NAME, NOT AN ID. AcademicYear.name is what every other collection references, and
 * what the path carries.
 *
 * A SECTION HAS NO ID. It lives inside the class document and is identified by sectionNo within
 * it, which is why the third picker reads the class rather than a collection of its own.
 *
 * NOTHING IS DISABLED, including Send with an empty form and a section the picker knows is
 * inactive. Each is a documented answer.
 */
function PlaceInClass({ child, onClose, onPlaced }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    year: '', classDocsId: '', sectionNo: '', rollNo: '', effectiveFrom: '',
  })
  const [years, setYears] = useState(null)
  const [classes, setClasses] = useState(null)
  const [sections, setSections] = useState(null)
  const [busy, setBusy] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! BUILT IN RENDER so the JSON pane and the request are one object. rollNo and effectiveFrom are
  //! OMITTED when empty rather than sent as "": an empty roll number is "no roll number", which
  //! the server stores as null, and an empty date would be a 400 about a field nobody filled in.
  const body = {
    studentDocsId: child.studentDocsId,
    classDocsId: form.classDocsId,
    sectionNo: form.sectionNo,
    ...(form.rollNo ? { rollNo: form.rollNo } : {}),
    ...(form.effectiveFrom ? { effectiveFrom: form.effectiveFrom } : {}),
  }

  //! THE YEARS, ONCE. Read on open rather than on a button, because nothing can be chosen until
  //! one is picked and a form that opens empty with no way forward is a puzzle.
  useEffect(() => {
    let live = true
    const run = async () => {
      setBusy('years')
      const answer = await call('list-academic-years', { label: 'The years to place into' })
      if (!live) return
      setBusy('')
      const rows = answer.ok
        ? (Array.isArray(answer.bodyJson) ? answer.bodyJson : (answer.bodyJson?.content ?? []))
        : []
      setYears(rows)
    }
    run()
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  //! CHOOSING A YEAR CLEARS THE TWO BELOW IT. Keeping a class from the previous year would send a
  //! real id under the wrong year — which is exactly CLASS_NOT_IN_YEAR, reached by accident
  //! rather than on purpose.
  const pickYear = async (year) => {
    setForm({ ...form, year, classDocsId: '', sectionNo: '' })
    setClasses(null)
    setSections(null)
    if (!year) return
    setBusy('classes')
    const answer = await call('list-school-classes', {
      label: `Classes in ${year}`,
      pathParams: { year },
      query: { page: '0', size: '100' },
    })
    setBusy('')
    setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : [])
  }

  //! AND CHOOSING A CLASS CLEARS THE SECTION, for the same reason — a section letter that exists
  //! in one class need not exist in the next.
  const pickClass = async (classDocsId) => {
    setForm({ ...form, classDocsId, sectionNo: '' })
    setSections(null)
    if (!classDocsId) return
    setBusy('sections')
    const answer = await call('list-class-sections', {
      label: 'Sections in that class',
      pathParams: { year: form.year, id: classDocsId },
    })
    setBusy('')
    setSections(answer.ok ? (answer.bodyJson?.sections ?? []) : [])
  }

  const send = async () => {
    setSending(true)
    const answer = await call('place-student', {
      label: `Place ${child.fullName}`,
      pathParams: { year: form.year },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onPlaced(answer.bodyJson)
  }

  const chosenSection = (sections ?? []).find((one) => one.sectionNo === form.sectionNo)

  return (
    <Modal
      open
      onClose={onClose}
      title={`Place ${child.fullName} in a class`}
      description="#14 — two documents in one transaction: the record is written, then the child is pointed at it. The one every roster, mark sheet and timetable waits for."
      endpoint={<EndpointTag id="place-student" name="Place" look="primary"
        pathParams={{ year: form.year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <div className="field-grid">
        <Field label="Academic year" required
          hint="THE NAME, not an id — it is what every other collection references, and what the path carries. Choosing one reloads the classes.">
          <Select value={form.year} options={['', ...(years ?? []).map((y) => y.name)]}
            onChange={pickYear} />
        </Field>
        <Field label="Class" required
          hint="Must belong to the year above. A class id from another year is a REAL id, which is why sending one is CLASS_NOT_IN_YEAR rather than not-found.">
          {/* OBJECT OPTIONS, so the picker shows "Class 1" while sending the 24-character id.
              A plain string is its own label, which is why every other picker here passes one. */}
          <Select
            value={form.classDocsId}
            options={['', ...(classes ?? []).map((c) => ({
              value: c.schoolClassId,
              label: c.active === false ? `${c.name} (not active)` : c.name,
            }))]}
            onChange={pickClass} />
        </Field>
        <Field label="Section" required
          hint="A section has no id — it is identified by sectionNo inside one class, which is why this list comes from reading the class.">
          <Select value={form.sectionNo}
            options={['', ...(sections ?? []).map((one) => ({
              value: one.sectionNo,
              label: one.active === false ? `${one.sectionNo} (not active)` : one.sectionNo,
            }))]}
            onChange={(v) => setForm({ ...form, sectionNo: v })} />
        </Field>
        <Field label="Roll number"
          hint="OPTIONAL AND NOT GENERATED. Scoped per section — the same number is free in the class next door. Blank is stored as null, not ''.">
          <Input value={form.rollNo} placeholder="7"
            onChange={(e) => setForm({ ...form, rollNo: e.target.value })} />
        </Field>
        <Field label="In effect from"
          hint="Defaults to today. A FUTURE date is allowed and is the point: a January placement into a June year carries the June date.">
          <Input type="date" value={form.effectiveFrom}
            onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
        </Field>
      </div>

      {busy ? <p className="muted">Reading the {busy}…</p> : null}

      {form.year && classes !== null && classes.length === 0 ? (
        <p className="muted">
          <Info size={12} /> <b>{form.year} has no classes.</b> There is nothing to place a child
          into until one exists — that is the structure module, not this one.
        </p>
      ) : null}

      {chosenSection ? (
        <p className="muted">
          <Info size={12} /> Section <b>{chosenSection.sectionNo}</b>
          {chosenSection.capacity ? <> holds <b>{chosenSection.capacity}</b></> : null}
          {chosenSection.active === false ? (
            <> and is <b>switched off</b> — sending this is{' '}
              <span className="mono">409 SECTION_NOT_ACTIVE</span>, which is a different answer
              from &ldquo;no such section&rdquo; on purpose: one is a typo, the other is a
              class-structure decision.</>
          ) : <> and is active.</>}
          {' '}<b>Capacity is not enforced by #14</b> — counting a section against it is #22, which
          is not built.
        </p>
      ) : null}

      {child.placed ? (
        <p className="muted">
          <Info size={12} /> <b>{child.fullName} already holds a record</b> —{' '}
          <span className="mono">{child.currentAcademicRecordDocsId}</span>. If it is for the year
          you pick, this is <span className="mono">409 STUDENT_ALREADY_PLACED</span>: one ACTIVE
          record per child per year, enforced by the index rather than by the check. <b>A different
          year is fine</b> — that is how a child moves up.
        </p>
      ) : null}

      <p className="muted">
        <Info size={12} /> <b>Two documents, one transaction.</b> The record is inserted, then the
        child&rsquo;s <span className="mono">currentAcademicRecordDocsId</span> is pointed at it.
        Either both happen or neither does — a record with no pointer is a child who is placed and
        reads back as unplaced everywhere.
      </p>

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
                <b>{result.bodyJson?.studentName}</b> is in{' '}
                <b>{result.bodyJson?.className} {result.bodyJson?.sectionNo}</b> for{' '}
                {result.bodyJson?.academicYear}
                {result.bodyJson?.rollNo
                  ? <>, roll number <span className="mono">{result.bodyJson.rollNo}</span></>
                  : <>, with <b>no roll number</b></>}
                , in effect from {result.bodyJson?.effectiveFrom}.{' '}
                <b>The child behind this modal has been re-read</b> — watch{' '}
                <span className="mono">placed</span> turn true.
              </p>
              <pre className="resp-body">{result.bodyJson?.nextStep}</pre>
            </>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}
