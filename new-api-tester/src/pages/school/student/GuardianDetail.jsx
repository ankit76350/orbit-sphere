import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Pencil, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import { detailPath, screenPath } from '../../../paths.js'

/**
 * One guardian: /school-student/guardians/{id}
 *
 * TWO ENDPOINTS — #10 reads the guardian with their children, #8 corrects the person. Attaching
 * or detaching one is #11 to #13; none is built, and the page says so rather than offering buttons
 * that would 404.
 *
 * CORRECTING CHANGES THEM FOR EVERY CHILD IN THE TABLE BELOW, which is why the button sits beside
 * that table's heading rather than hiding in a toolbar: the count is the warning. The answer says
 * how many it reached.
 *
 * THE FLAGS TABLE IS THE WHOLE PAGE. relation, primaryContact, emergencyContact, pickupAuthorized
 * and portalAccess live on GuardianLink, which is embedded in the STUDENT — not on the guardian —
 * because the same man is "father, primary, may collect, portal" to one child and only an
 * emergency number for their cousin. So they are drawn per child, and a single row of flags beside
 * the person's name would be a fiction.
 *
 * AN EMPTY CHILDREN LIST IS A REAL ANSWER. #7 creates guardians attached to nobody, and that is
 * the normal state of an emergency number put on file before the child arrives. On #9 the key is
 * absent instead, because that endpoint does not read students at all.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth reaching from the address bar.
 */

const TONE = { ACTIVE: 'good', WITHDRAWN: 'bad', TRANSFERRED: 'bad', SUSPENDED: 'warn' }

/** The four booleans, in the order a school reads them. */
const FLAGS = [
  ['primaryContact', 'primary', 'good'],
  ['emergencyContact', 'emergency', 'warn'],
  ['pickupAuthorized', 'may collect', undefined],
  ['portalAccess', 'portal', undefined],
]

export default function GuardianDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [person, setPerson] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [correcting, setCorrecting] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-guardian', {
      label: 'One guardian and their children',
      pathParams: { guardianDocsId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setPerson(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="A guardian" />

  const children = person?.children ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{person?.fullName ?? 'Guardian'}</h1>
          <p className="muted">
            <span className="mono">{person?.phoneNumber ?? id}</span>
            {person ? ` · ${children.length} child${children.length === 1 ? '' : 'ren'}` : ' · reading…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft}
          onClick={() => navigate(screenPath('school', 'student', 'guardians'))}>
          All guardians
        </Button>
        <EndpointTag id="get-guardian" name="Read" pathParams={{ guardianDocsId: id ?? '' }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
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
            <Info size={12} /> <b>Another school&rsquo;s guardian is this same 404</b>, not a 403 —
            a 403 would confirm they exist, and behind it are a family&rsquo;s phone number and home
            address. The read is scoped <b>in the query</b>, never checked after.
          </p>
        </Card>
      ) : person ? (
        <>
          <Card
            title="The person"
            description="#10 reads it; #8 corrects it — and that reaches every child below, which is the point of the shared row."
            action={
              <Button look="primary" icon={Pencil} onClick={() => setCorrecting(true)}>
                Correct it
              </Button>
            }
          >
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><td className="muted">Name</td><td>{person.fullName}</td></tr>
                  <tr><td className="muted">Phone</td>
                    <td>{person.phoneNumber
                      ? <span className="mono">{person.phoneNumber}</span>
                      : <span className="muted">none on file</span>}{' '}
                      <span className="muted">the match key — one person per school</span></td></tr>
                  <tr><td className="muted">Alternate phone</td>
                    <td>{person.alternatePhoneNumber
                      ? <><span className="mono">{person.alternatePhoneNumber}</span>{' '}
                        <span className="muted">not unique — a shared family landline</span></>
                      : <span className="muted">none</span>}</td></tr>
                  <tr><td className="muted">Email</td>
                    <td>{person.emailAddress ?? <span className="muted">none on file</span>}</td></tr>
                  <tr><td className="muted">Occupation</td>
                    <td>{person.occupation ?? <span className="muted">—</span>}</td></tr>
                  <tr><td className="muted">Address</td>
                    <td>{person.address ?? <span className="muted">—</span>}</td></tr>
                  <tr><td className="muted">Language</td>
                    <td>{person.preferredLanguage ?? <span className="muted">—</span>}</td></tr>
                  <tr><td className="muted">Guardian id</td>
                    <td><span className="mono">{person.guardianDocsId}</span></td></tr>
                  <tr><td className="muted">Added</td>
                    <td title={person.createdAt}>{readable(person.createdAt)}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card
            title={`What they are to each child — ${children.length}`}
            description="The flags live on the CHILD, not on this person. The same man can be a father here and an emergency number there."
          >
            {children.length === 0 ? (
              <Empty
                title="Attached to nobody"
                description="A real answer, not a gap. #7 creates a guardian on their own — an emergency number on file before the child arrives — and attaching them to a child is #11, which is not built."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Child</th>
                      <th>Admission no</th>
                      <th>Status</th>
                      <th>Relation</th>
                      <th>To this child</th>
                      <th>Placed</th>
                    </tr>
                  </thead>
                  <tbody>
                    {children.map((c) => (
                      // The row opens the CHILD — #5 — because that is the only other thing
                      // there is to look at from here.
                      <tr key={c.studentDocsId} data-opens
                        onClick={() => navigate(detailPath('school', 'student', 'students',
                          c.studentDocsId))}>
                        <td>{c.fullName}</td>
                        <td><span className="mono">{c.admissionNo}</span></td>
                        <td><Badge tone={TONE[c.status]}>{c.status}</Badge></td>
                        <td><Badge>{c.relation}</Badge></td>
                        <td>
                          {FLAGS.filter(([key]) => c[key]).length === 0
                            ? <span className="muted">no flags</span>
                            : FLAGS.filter(([key]) => c[key]).map(([key, label, tone]) => (
                              <span key={key}><Badge tone={tone}>{label}</Badge>{' '}</span>
                            ))}
                        </td>
                        <td>{c.placed
                          ? <Badge tone="good">in a class</Badge>
                          : <span className="muted">not yet</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="muted">
              <Info size={12} /> <b>Two queries, not one per child.</b>{' '}
              <span className="mono">school_guardian_students_idx</span> is keyed{' '}
              <span className="mono">{'{schoolId, guardians.guardianDocsId}'}</span> and exists for
              exactly this. The link is then found <i>on the child</i> rather than assumed — a row
              whose array does not name this guardian would mean the index and the document
              disagree, and it is skipped rather than drawn with empty flags.
            </p>
          </Card>

          <Card
            title="What cannot be done here yet"
            description="Said plainly rather than offered as buttons that would 404."
          >
            <p className="muted">
              <b>#11 attaches</b> this guardian to a child, <b>#12 changes the flags</b> for one
              child, and <b>#13 detaches</b> — <b>unlinks, never deletes</b>, because this same row
              may be three other children&rsquo;s mother. None is built.
            </p>
            <p className="muted">
              <Info size={12} /> <b>#12 is the one that is missing most.</b> Correcting the person
              is #8, above — but the <i>relation</i> and the four flags in the table are facts
              about this guardian <b>and one child</b>, and nothing can change them yet except
              re-admitting the child.
            </p>
          </Card>
        </>
      ) : null}

      {/* MOUNTED ONLY WHILE OPEN, so every box — the version included — is seeded from the read
          each time. A component that stayed mounted would keep the values it was first given, and
          a Refresh behind it would leave the version one behind. */}
      {correcting && person ? (
        <CorrectGuardian
          person={person}
          onClose={() => setCorrecting(false)}
          onCorrected={load}
        />
      ) : null}
    </div>
  )
}

/**
 * #8 — correct the person.
 *
 * THE COUNT IS THE WARNING, so it is stated before the boxes rather than after the send: this row
 * is shared, and a correction here reaches every child on the page behind.
 *
 * ONLY WHAT CHANGED IS SENT. A box left exactly as it was is left out of the body — which is what
 * "absent means leave it alone" means — and an emptied box is sent as "", which is the clear. The
 * two look alike in a form and are opposites on the wire.
 *
 * NOTHING IS DISABLED. A blank name, somebody else's number, a stale version — each is a
 * documented refusal.
 */
function CorrectGuardian({ person, onClose, onCorrected }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    fullName: person.fullName ?? '',
    phoneNumber: person.phoneNumber ?? '',
    alternatePhoneNumber: person.alternatePhoneNumber ?? '',
    emailAddress: person.emailAddress ?? '',
    address: person.address ?? '',
    occupation: person.occupation ?? '',
    preferredLanguage: person.preferredLanguage ?? '',
  })
  const [version, setVersion] = useState(String(person.version ?? 0))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const changed = (key, was) => (form[key] === (was ?? '') ? {} : { [key]: form[key] })

  const body = {
    ...changed('fullName', person.fullName),
    ...changed('phoneNumber', person.phoneNumber),
    ...changed('alternatePhoneNumber', person.alternatePhoneNumber),
    ...changed('emailAddress', person.emailAddress),
    ...changed('address', person.address),
    ...changed('occupation', person.occupation),
    ...changed('preferredLanguage', person.preferredLanguage),
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('update-guardian', {
      label: `Correct ${person.fullName}`,
      pathParams: { guardianDocsId: person.guardianDocsId },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onCorrected()
  }

  const moves = Object.keys(body).filter((k) => k !== 'version').length
  const children = person.children?.length ?? 0

  return (
    <Modal
      open
      onClose={onClose}
      title={`Correct ${person.fullName}`}
      description="One row, every child. The relation and the flags are not here — they belong to one child rather than to this person."
      endpoint={<EndpointTag id="update-guardian" name="Correct" look="primary"
        pathParams={{ guardianDocsId: person.guardianDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <p className="muted">
        <Info size={12} /> <b>This reaches {children} child{children === 1 ? '' : 'ren'}.</b>{' '}
        {children === 0
          ? 'They are attached to nobody, so this changes nothing else.'
          : <>A guardian is one real person per school, so correcting them corrects them on every
            child at once. <b>There is no way to change it for one of them</b> — the alternative is
            several rows for one woman and no way to tell which is current.</>}
      </p>

      <div className="field-grid">
        <Field label="Full name"
          hint='The one field where "" is REFUSED instead of obeyed — a guardian with no name is a row nobody can find.'>
          <Input value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Phone"
          hint="Checked against everybody ELSE. Re-sending their own number is fine — the check skips them.">
          <Input value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
        </Field>
        <Field label="Email" hint="Checked the same way, and skipped for themselves the same way.">
          <Input value={form.emailAddress}
            onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} />
        </Field>
        <Field label="Alternate phone"
          hint="Normalised but NOT checked — a shared family landline is the ordinary case.">
          <Input value={form.alternatePhoneNumber}
            onChange={(e) => setForm({ ...form, alternatePhoneNumber: e.target.value })} />
        </Field>
        <Field label="Occupation" hint='Empty the box to remove it.'>
          <Input value={form.occupation}
            onChange={(e) => setForm({ ...form, occupation: e.target.value })} />
        </Field>
        <Field label="Preferred language"
          hint='Correctable but NOT removable — an enum takes no "". Emptying it reaches 400 INVALID_VALUE.'>
          <Select value={form.preferredLanguage}
            options={['', 'en-IN', 'hi-IN', 'en-GB', 'en-US']}
            onChange={(v) => setForm({ ...form, preferredLanguage: v })} />
        </Field>
        <Field label="Address" wide hint='Empty the box to remove it.'>
          <Input value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
        <Field label="Version" required wide
          hint="Seeded from the read. It matters more here than on most writes — this row is shared, so two offices are genuinely likely to collide.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>

      <p className="muted">
        <Info size={12} /> <b>{moves} field{moves === 1 ? '' : 's'} will move.</b>{' '}
        {moves === 0
          ? <>Sending this is <span className="mono">400 NOTHING_TO_UPDATE</span> — the version
            alone is not a change. The button still sends.</>
          : 'Everything else is left out of the body entirely, which is what "leave it alone" means.'}
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
              <span className="mono">{result.bodyJson?.version}</span> — and that changed them for{' '}
              <b>{result.bodyJson?.childrenAffected}</b> child
              {result.bodyJson?.childrenAffected === 1 ? '' : 'ren'}.
            </p>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}
