import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Link2, Link2Off, Pencil, Plus, RefreshCw } from 'lucide-react'
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
            title="Where this child came from"
            description="And where they are going, which today is nowhere — #14 places a child and is not built."
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
                  <tr>
                    <td className="muted">Placed in a class</td>
                    <td>{child.placed
                      ? <Badge tone="good">{child.currentAcademicRecordDocsId}</Badge>
                      : <span className="muted">not yet</span>}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="muted">
              <Info size={12} /> <b>A child with no class is a normal state, not a half-finished
              one.</b> The recorded flow is inquiry → admission → student → academic record, and a
              school knows it has admitted a child in January without knowing which section they
              are in until June. Placing them is <b>#14</b>, which is not built.
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
