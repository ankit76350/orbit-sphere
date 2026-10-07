import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import { detailPath, screenPath } from '../../../paths.js'

/**
 * One guardian: /school-student/guardians/{id}
 *
 * ONE ENDPOINT — #10. Correcting a guardian is #8, and attaching or detaching one is #11 to #13;
 * none is built, and the page says so rather than offering buttons that would 404.
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
          <Card title="The person" description="#10 — one document, shared by every child below.">
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
              <b>#8 corrects this guardian</b>, and it changes them <i>for every child above</i> —
              which is the whole point of the shared row, and the reason it is not folded into the
              child&rsquo;s page. <b>#11 attaches</b> them to a child, <b>#12 changes the flags</b>{' '}
              for one child, and <b>#13 detaches</b> — <b>unlinks, never deletes</b>, because this
              same row may be three other children&rsquo;s mother.
            </p>
          </Card>
        </>
      ) : null}
    </div>
  )
}
