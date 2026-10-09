import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ArrowRightLeft, GraduationCap, Info, Link2, Link2Off, Pencil, Plus, RefreshCw } from 'lucide-react'
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
  const [transferring, setTransferring] = useState(false)
  //! THE HISTORY, READ BACK. It was the 201 from #14 kept in state until #20 existed — the only
  //! place the class, the section and the roll number were ever seen, and gone on every Refresh.
  //! Now it is a read like any other, so the card survives a reload and shows the years before
  //! this one too.
  const [history, setHistory] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)

    //! TWO READS, AND THE SECOND IS NOT CONDITIONAL ON THE FIRST. A child who is not placed still
    //! has a history worth asking for — it comes back empty, which is the answer — and waiting to
    //! find out would make the card flicker through "none" on every reload.
    const [one, records] = await Promise.all([
      call('get-student', {
        label: 'One child in full',
        pathParams: { studentDocsId: id ?? '' },
      }),
      call('student-academic-records', {
        label: 'Where this child has been',
        pathParams: { studentDocsId: id ?? '' },
      }),
    ])

    setLoading(false)
    if (one.ok) { setChild(one.bodyJson); setProblem(null) } else { setProblem(one) }
    //! A FAILED HISTORY IS NOT A FAILED PAGE. The child is what this screen is about; the records
    //! are one card on it, and null means "we could not ask" rather than "there are none".
    setHistory(records.ok ? records.bodyJson : null)
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
            title={`Academic record${history ? ` — ${history.recordCount}` : ''}`}
            description="Which class and section they hold, year by year. #14 writes a placement; #20 reads them all back, terminal records included."
            action={
              /* BOTH, ALWAYS. Transfer on a child with no record is a first placement rather
                 than a refusal, and Place on a child who has one is 409 STUDENT_ALREADY_PLACED —
                 a documented answer. Hiding either would put this screen's guess in front of the
                 server's. */
              <>
                <Button icon={ArrowRightLeft} onClick={() => setTransferring(true)}>
                  Transfer
                </Button>{' '}
                <Button look="primary" icon={GraduationCap} onClick={() => setPlacing(true)}>
                  Place in a class
                </Button>
              </>
            }
          >
            {history === null ? (
              <p className="muted">The history could not be read. Press Refresh.</p>
            ) : history.recordCount === 0 ? (
              <Empty
                title="Never placed"
                description="An empty list, not a 404 — the child is what has to exist here. A school knows it has admitted a child in January without knowing which section they are in until June, so this is a normal state rather than a half-finished one."
                action={
                  <>
                    <Button icon={ArrowRightLeft} onClick={() => setTransferring(true)}>
                      Transfer
                    </Button>{' '}
                    <Button look="primary" icon={GraduationCap} onClick={() => setPlacing(true)}>
                      Place in a class
                    </Button>
                  </>
                }
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Year</th>
                      <th>Class and section</th>
                      <th>Roll no</th>
                      <th>From</th>
                      <th>Until</th>
                      <th>Status</th>
                      <th>Record id</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.records.map((one) => (
                      <tr key={one.academicRecordDocsId}>
                        <td>{one.academicYear}</td>
                        <td>
                          {/* className IS NULL WHEN THE CLASS DOCUMENT IS GONE. The row stays:
                              the child was in that class whatever happened to it afterwards. */}
                          <b>{one.className ?? <span className="muted">class deleted</span>}</b>
                          {' '}{one.sectionNo}
                        </td>
                        <td>{one.rollNo
                          ? <span className="mono">{one.rollNo}</span>
                          : <span className="muted">none</span>}</td>
                        <td>{one.effectiveFrom}</td>
                        <td>{one.effectiveUntil ?? <span className="muted">open</span>}</td>
                        <td>
                          <Badge tone={one.status === 'ACTIVE' ? 'good' : undefined}>
                            {one.status}
                          </Badge>
                          {/* `current` IS THE STUDENT DOCUMENT'S POINTER, not this record's
                              status. Two copies of one fact, and showing both is how a
                              disagreement between them becomes visible. */}
                          {one.current ? <> <Badge tone="good">current</Badge></> : null}
                        </td>
                        <td><span className="mono muted">{one.academicRecordDocsId}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {history && history.recordCount > 0 ? (
              <p className="muted">
                <Info size={12} /> <b>Newest year first, and that order is the index&rsquo;s.</b>{' '}
                <span className="mono">school_student_academic_record_history_idx</span> is{' '}
                <span className="mono">
                  {'{schoolId, studentDocsId, academicYear: -1, effectiveFrom: -1}'}
                </span>, so asking for exactly that lets Mongo walk it and skip the sort stage.{' '}
                <b>The class names cost one query, not one per row</b> — the N+1 the plan names for
                #21, avoided here first because the shape is the same.
              </p>
            ) : null}

            <p className="muted">
              <Info size={12} /> <b>Terminal records stay in the list</b>, which is the point of a
              history: the question is where this child has <i>been</i>. Only{' '}
              <span className="mono">current</span> and{' '}
              <span className="mono">status: ACTIVE</span> say where they are now — and they are{' '}
              <b>two separate copies of that fact</b>, one on the record and one on the child, so
              seeing both is how a disagreement shows up instead of being quietly resolved.
            </p>
            <p className="muted">
              <Info size={12} /> <b>Changing class or section mid-year is Transfer, #17.</b> A
              child in 5C goes to 5B, or to 5D, or up to 6B in the middle of a running year — the
              ordinary thing a school does, and this is the endpoint for it. It <b>closes the open
              record as <span className="mono">TRANSFERRED</span> and opens a new one</b> in the
              same transaction, chained back through{' '}
              <span className="mono">previousAcademicRecordDocsId</span>, so the row above it stays
              in this table rather than being overwritten.
            </p>
            <p className="muted">
              <Info size={12} /> <b>A <span className="mono">PATCH</span> of the class could not do
              it.</b> One ACTIVE record per child per year is a unique index, so for an instant two
              would exist — and more importantly, editing in place <b>erases where the child sat
              for the first half of the year</b>, which is what that half&rsquo;s attendance and
              marks are attached to. <b>Place in a class is for a child who has none</b>: on one
              who already does it is{' '}
              <span className="mono">409 STUDENT_ALREADY_PLACED</span>, and <b>Transfer works
              either way</b> — with no open record it is a first placement, and says so.
            </p>
            <p className="muted">
              <Info size={12} /> <b>#14 runs no gate 4</b>, where every other write against a year
              refuses one that is not running — because a child admitted in January is placed into
              a year that starts in June. <b>#15 and #16 are still missing</b>: correcting a roll
              number and closing a record, neither built.
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

      {transferring && child ? (
        <TransferToClass
          child={child}
          //! THE OPEN RECORD, FOUND ON THE PAGE. The year and the current class come off it, so
          //! the form opens where the child actually is rather than empty.
          open={(history?.records ?? []).find((one) => one.current) ?? null}
          onClose={() => setTransferring(false)}
          onTransferred={load}
        />
      ) : null}

      {placing && child ? (
        <PlaceInClass
          child={child}
          onClose={() => setPlacing(false)}
          //! JUST RELOAD. The 201 used to be kept in state because nothing could read a record
          //! back; #20 can, so the card fills from the same read a Refresh uses.
          onPlaced={load}
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
 * What the dropdown should show when the box beside it holds something the dropdown cannot offer.
 *
 * <p>A `<select>` whose value is not one of its options renders blank, which is the right picture
 * — the list is not claiming a hand-typed id — but React keeps the stale selection highlighted
 * unless the value is cleared explicitly. So: show the value when it is on the list, and nothing
 * when it is not.
 */
const offered = (rows, keyOf, value) =>
  (rows ?? []).some((row) => keyOf(row) === value) ? value : ''

/**
 * #14 — put this child in a class and a section.
 *
 * EVERY DROPDOWN HAS AN EDITABLE BOX BESIDE IT, AND THE BOX IS WHAT IS SENT. The dropdown writes
 * into the box and nothing else reads it. That is what makes the refusals reachable: a form that
 * can only offer valid choices can never produce CLASS_NOT_IN_YEAR, SECTION_NOT_IN_CLASS or
 * ACADEMIC_YEAR_NOT_FOUND, which are three of this endpoint's ten documented answers.
 *
 * THE THREE PICKERS ARE CHAINED, because the three ids are. A class belongs to a year and a
 * section belongs to a class, so choosing a year is what makes the classes knowable and choosing a
 * class is what makes the sections knowable. PICKING chains; TYPING does not — a year typed by
 * hand changes what is sent without reloading a class list that would usually be empty anyway.
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
      {/* EVERY DROPDOWN HAS THE BOX IT FILLS IN BESIDE IT, and THE BOX IS WHAT IS SENT.
          The dropdown offers what exists and chains the next read; the box takes anything. That
          is the whole point: a class id from another year, a section letter that class does not
          have, a year nobody created — each is a documented refusal, and a form that only offers
          valid choices cannot reach any of them. */}
      <div className="field-grid">
        <Field label="Academic year" required
          hint="THE NAME, not an id — it is what every other collection references, and what the path carries. Picking one reloads the classes; typing one only changes what is sent.">
          <div className="picker-pair">
            <Select value={offered(years, (y) => y.name, form.year)}
              options={['', ...(years ?? []).map((y) => y.name)]}
              onChange={pickYear} />
            <Input value={form.year} placeholder="2026-2027"
              onChange={(e) => setForm({ ...form, year: e.target.value })} />
          </div>
        </Field>
        <Field label="Class" required
          hint="Must belong to the year above. A class id from another year is a REAL id — paste one into the box to see CLASS_NOT_IN_YEAR rather than not-found.">
          <div className="picker-pair">
            {/* OBJECT OPTIONS, so the picker shows "Class 1" while writing the 24-character id
                into the box. A plain string is its own label, which is why the others pass one. */}
            <Select
              value={offered(classes, (c) => c.schoolClassId, form.classDocsId)}
              options={['', ...(classes ?? []).map((c) => ({
                value: c.schoolClassId,
                label: c.active === false ? `${c.name} (not active)` : c.name,
              }))]}
              onChange={pickClass} />
            <Input value={form.classDocsId} placeholder="6aa39612224c2e933a1c854a"
              onChange={(e) => setForm({ ...form, classDocsId: e.target.value })} />
          </div>
        </Field>
        <Field label="Section" required
          hint="A section has no id — it is identified by sectionNo inside one class. Type a letter that class does not have to reach SECTION_NOT_IN_CLASS.">
          <div className="picker-pair">
            <Select value={offered(sections, (one) => one.sectionNo, form.sectionNo)}
              options={['', ...(sections ?? []).map((one) => ({
                value: one.sectionNo,
                label: one.active === false ? `${one.sectionNo} (not active)` : one.sectionNo,
              }))]}
              onChange={(v) => setForm({ ...form, sectionNo: v })} />
            <Input value={form.sectionNo} placeholder="A"
              onChange={(e) => setForm({ ...form, sectionNo: e.target.value })} />
          </div>
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

      {/* THE YEAR IS THE ONLY ONE OF THE FIVE THAT NEVER APPEARS IN THE BODY — it is the path.
          So the JSON pane cannot show it changing, and without this line editing that box would
          look like it did nothing. */}
      <p className="muted">
        <span className="mono">POST /schools/current/academic-years/</span>
        <b className="mono">{form.year || '{year}'}</b>
        <span className="mono">/student-records</span>
        {form.year ? null : (
          <> — <b>no year chosen</b>. That is a plain{' '}
            <span className="mono">404 NOT_FOUND</span>, not{' '}
            <span className="mono">ACADEMIC_YEAR_NOT_FOUND</span>: the empty segment collapses the
            URL and Spring matches no route at all, so the endpoint never runs. Measured
            2026-10-09.</>
        )}
      </p>

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

/**
 * #17 — transfer this child to another class or section.
 *
 * THE FORM OPENS WHERE THE CHILD IS. The year and the class come off their open record, read on
 * the page behind this — so a section change needs only a section, which is the common case.
 *
 * EVERY DROPDOWN HAS THE EDITABLE BOX IT FILLS IN BESIDE IT, and the box is what is sent. A form
 * that can only offer valid choices can never produce CLASS_NOT_IN_YEAR, SECTION_NOT_IN_CLASS or
 * ACADEMIC_YEAR_NOT_FOUND, which are three of this endpoint's documented answers.
 *
 * THE VERSION IS THE CHILD'S, seeded from the read behind this modal. It is checked BEFORE the
 * first write, because a transfer touches two documents and cannot be half-done.
 *
 * NOTHING IS DISABLED, including a transfer to the section they are already in — that is
 * 409 ALREADY_IN_THAT_SECTION, and the form says so rather than stopping you reaching it.
 */
function TransferToClass({ child, open, onClose, onTransferred }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    year: open?.academicYear ?? '',
    classDocsId: open?.classDocsId ?? '',
    sectionNo: '',
    rollNo: '',
    effectiveFrom: '',
    version: String(child.version ?? 0),
  })
  const [years, setYears] = useState(null)
  const [classes, setClasses] = useState(null)
  const [sections, setSections] = useState(null)
  const [busy, setBusy] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! BUILT IN RENDER so the JSON pane and the request are one object. classDocsId is sent only
  //! when it has a value: absent means "keep the class they are in", which is the difference
  //! between a section change and a class change.
  const body = {
    studentDocsId: child.studentDocsId,
    ...(form.classDocsId ? { classDocsId: form.classDocsId } : {}),
    sectionNo: form.sectionNo,
    ...(form.rollNo ? { rollNo: form.rollNo } : {}),
    ...(form.effectiveFrom ? { effectiveFrom: form.effectiveFrom } : {}),
    version: form.version === '' ? undefined : Number(form.version),
  }

  //! THE YEARS, AND THE CLASSES OF THE ONE THE CHILD IS IN, on open. The form is meant to be
  //! usable without touching the first two boxes at all.
  useEffect(() => {
    let live = true
    const run = async () => {
      setBusy('years')
      const answer = await call('list-academic-years', { label: 'The years' })
      if (!live) return
      const rows = answer.ok
        ? (Array.isArray(answer.bodyJson) ? answer.bodyJson : (answer.bodyJson?.content ?? []))
        : []
      setYears(rows)

      if (form.year) {
        setBusy('classes')
        const inYear = await call('list-school-classes', {
          label: `Classes in ${form.year}`,
          pathParams: { year: form.year },
          query: { page: '0', size: '100' },
        })
        if (!live) return
        setClasses(inYear.ok ? (inYear.bodyJson?.content ?? []) : [])

        if (form.classDocsId) {
          setBusy('sections')
          const inClass = await call('list-class-sections', {
            label: 'Sections in their class',
            pathParams: { year: form.year, id: form.classDocsId },
          })
          if (!live) return
          setSections(inClass.ok ? (inClass.bodyJson?.sections ?? []) : [])
        }
      }
      setBusy('')
    }
    run()
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  //! PICKING A YEAR CLEARS THE TWO BELOW IT. Keeping a class from another year would send a real
  //! id under the wrong year — CLASS_NOT_IN_YEAR reached by accident rather than on purpose.
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
    const answer = await call('transfer-student-record', {
      label: `Transfer ${child.fullName}`,
      pathParams: { year: form.year },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onTransferred()
  }

  const sameSpot = open
    && (form.classDocsId === '' || form.classDocsId === open.classDocsId)
    && form.sectionNo === open.sectionNo
  const chosenSection = (sections ?? []).find((one) => one.sectionNo === form.sectionNo)

  return (
    <Modal
      open
      onClose={onClose}
      title={`Transfer ${child.fullName}`}
      description="#17 — the mid-year change a school actually makes: 5C to 5B, to 5D, or up to 6B. It closes the open record as TRANSFERRED and opens a new one, in the same transaction."
      endpoint={<EndpointTag id="transfer-student-record" name="Transfer" look="primary"
        pathParams={{ year: form.year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      {open ? (
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><th>Leaving</th>
                <td><b>{open.className}</b> {open.sectionNo}
                  {open.rollNo ? <> · roll <span className="mono">{open.rollNo}</span></> : null}
                </td></tr>
              <tr><th>That record becomes</th>
                <td><Badge>TRANSFERRED</Badge>{' '}
                  <span className="muted">
                    with an effectiveUntil — <b>not deleted</b>, because it is where that part of
                    the year&rsquo;s attendance is attached
                  </span></td></tr>
            </tbody>
          </table>
        </div>
      ) : (
        <p className="muted">
          <Info size={12} /> <b>{child.fullName} has no open record.</b> There is nothing to close,
          so this is a <b>first placement</b> and the answer comes back with{' '}
          <span className="mono">transferred: false</span>. <b>The class is required here</b> —
          there is no current one to default to, and leaving it empty is{' '}
          <span className="mono">400 CLASS_REQUIRED</span>.
        </p>
      )}

      <p className="muted">
        <span className="mono">POST /schools/current/academic-years/</span>
        <b className="mono">{form.year || '{year}'}</b>
        <span className="mono">/student-records/transfer</span>
      </p>

      <div className="field-grid">
        <Field label="Academic year" required
          hint="THE NAME, not an id. Seeded from the record they hold. Picking one reloads the classes; typing one only changes what is sent.">
          <div className="picker-pair">
            <Select value={offered(years, (y) => y.name, form.year)}
              options={['', ...(years ?? []).map((y) => y.name)]}
              onChange={pickYear} />
            <Input value={form.year} placeholder="2026-2027"
              onChange={(e) => setForm({ ...form, year: e.target.value })} />
          </div>
        </Field>
        <Field label="Class"
          hint="EMPTY KEEPS THE CLASS THEY ARE IN — that is the difference between a section change and a class change. Required only when they have no open record.">
          <div className="picker-pair">
            <Select
              value={offered(classes, (c) => c.schoolClassId, form.classDocsId)}
              options={['', ...(classes ?? []).map((c) => ({
                value: c.schoolClassId,
                label: c.active === false ? `${c.name} (not active)` : c.name,
              }))]}
              onChange={pickClass} />
            <Input value={form.classDocsId} placeholder="keep the current class"
              onChange={(e) => setForm({ ...form, classDocsId: e.target.value })} />
          </div>
        </Field>
        <Field label="Section" required
          hint="Where they are going. A transfer with no destination is not a transfer.">
          <div className="picker-pair">
            <Select value={offered(sections, (one) => one.sectionNo, form.sectionNo)}
              options={['', ...(sections ?? []).map((one) => ({
                value: one.sectionNo,
                label: one.active === false ? `${one.sectionNo} (not active)` : one.sectionNo,
              }))]}
              onChange={(v) => setForm({ ...form, sectionNo: v })} />
            <Input value={form.sectionNo} placeholder="B"
              onChange={(e) => setForm({ ...form, sectionNo: e.target.value })} />
          </div>
        </Field>
        <Field label="Roll number"
          hint="IT DOES NOT CARRY OVER. Roll numbers are scoped to a section, so the one held in 5C says nothing about what is free in 5B. Empty leaves the new record without one.">
          <Input value={form.rollNo} placeholder="none"
            onChange={(e) => setForm({ ...form, rollNo: e.target.value })} />
        </Field>
        <Field label="In effect from"
          hint="Defaults to today. It is ALSO the old record's effectiveUntil, so the two meet rather than leaving a gap.">
          <Input type="date" value={form.effectiveFrom}
            onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
        </Field>
        <Field label="The CHILD'S version" required
          hint="Seeded from the read behind this modal. Checked BEFORE the first write — a transfer touches two documents and cannot be half-done, so that is the only safe place to refuse.">
          <Input value={form.version}
            onChange={(e) => setForm({ ...form, version: e.target.value })} />
        </Field>
      </div>

      {busy ? <p className="muted">Reading the {busy}…</p> : null}

      {sameSpot ? (
        <p className="muted">
          <Info size={12} /> <b>That is where they already are.</b> Sending it is{' '}
          <span className="mono">409 ALREADY_IN_THAT_SECTION</span> — refused rather than
          performed, because performing it would close a real record and open an identical one,
          <b> losing the original effectiveFrom</b>, which is the one fact that placement carried.
          Correcting a roll number or a date is #15, which is not built.
        </p>
      ) : null}

      {chosenSection && chosenSection.active === false ? (
        <p className="muted">
          <Info size={12} /> Section <b>{chosenSection.sectionNo}</b> is <b>switched off</b> —
          sending this is <span className="mono">409 SECTION_NOT_ACTIVE</span>, a different answer
          from &ldquo;no such section&rdquo; on purpose: one is a typo, the other is a
          class-structure decision.
        </p>
      ) : null}

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} OK` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <>
              <p className="muted">
                {result.bodyJson?.transferred ? (
                  <>
                    <b>{result.bodyJson?.studentName}</b> left{' '}
                    <b>{result.bodyJson?.transferredFrom?.className}{' '}
                      {result.bodyJson?.transferredFrom?.sectionNo}</b>{' '}
                    on {result.bodyJson?.transferredFrom?.effectiveUntil} and is now in{' '}
                    <b>{result.bodyJson?.now?.className} {result.bodyJson?.now?.sectionNo}</b>.{' '}
                    <b>The old record is TRANSFERRED, not deleted</b> — it is still in the table
                    behind this modal, which has been re-read.
                  </>
                ) : (
                  <>
                    <b>{result.bodyJson?.studentName}</b> had no open record, so this was a{' '}
                    <b>first placement</b> into{' '}
                    <b>{result.bodyJson?.now?.className} {result.bodyJson?.now?.sectionNo}</b>{' '}
                    rather than a transfer.
                  </>
                )}
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
