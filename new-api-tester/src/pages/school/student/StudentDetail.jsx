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
 * One child: /school-student/students/{id}
 *
 * ONE ENDPOINT — #5. Correcting a child is #2 and moving their status is #3, and neither is built:
 * they are phase 8, because CRM #33 needs neither of them. The page says so rather than offering
 * buttons that would 404.
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

export default function StudentDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [child, setChild] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

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
          <Card title="The child" description="#5 — everything the roll's row leaves off.">
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
            description="Resolved into people in ONE read, not one per contact. The person is shared across their children; the flags belong to this child alone."
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
              <b>#2 corrects a profile</b> and <b>#3 moves a child through the status graph</b> with
              a reason. <b>#11 to #13</b> attach, re-flag and detach a guardian. None of them is
              built: they are phase 8, and the reason they waited is that{' '}
              <b>CRM #33 needs none of them</b> — the four endpoints in this module exist to unblock
              the handover and nothing else.
            </p>
          </Card>
        </>
      ) : null}
    </div>
  )
}
