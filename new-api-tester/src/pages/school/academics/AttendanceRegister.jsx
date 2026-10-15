import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'

/**
 * One attendance register, on its own page:
 * /school-academics/attendance/{classId}/sections/{sectionNo}/registers/{sessionId}
 *
 * ONE ENDPOINT — #6: the register, and every child who was in the section on its date, each with
 * their mark or "not marked". Marking them (#2) comes next and will live on this page.
 */

const MARK_TONE = { PRESENT: 'good', LATE: 'warn', AUTHORISED_ABSENCE: undefined, UNAUTHORISED_ABSENCE: 'bad' }

export default function AttendanceRegister() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id, sectionNo, sessionId } = useParams()
  const year = actingAcademicYear ?? ''

  const [register, setRegister] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-attendance-register', {
      label: 'One register',
      pathParams: { year, attendanceSessionDocsId: sessionId ?? '' },
    })
    setLoading(false)
    if (answer.ok) { setRegister(answer.bodyJson); setProblem(null) } else { setRegister(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, sessionId])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="An attendance register" />

  const session = register?.session
  //! BACK TO THE SECTION, ON THE REGISTER'S OWN DATE — not today's, which may show nothing.
  const back = () => navigate(childPath('school', 'academics', 'attendance', id ?? '', 'sections', sectionNo ?? '')
    + (session?.attendanceDate ? `?date=${session.attendanceDate}` : ''))

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">
            {session
              ? `${session.className ?? 'Class'} ${session.sectionNo} · ${session.sessionType}${session.periodCode ? ` ${session.periodCode}` : ''}`
              : 'Attendance register'}
          </h1>
          <p className="muted">
            {session ? <>{session.attendanceDate} · {session.teacherName ?? 'no teacher'}</> : 'reading the register…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>Registers</Button>
        <EndpointTag id="get-attendance-register" name="Register"
          pathParams={{ year, attendanceSessionDocsId: sessionId }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {register ? (
        <Card
          title={`${register.studentCount} children`}
          description={`${register.markedCount} marked · ${register.unmarkedCount} not marked`}
          action={<Badge tone={session.status === 'OPEN' ? 'good' : undefined}>{session.status}</Badge>}
        >
          {register.students.length === 0 ? (
            <Empty title="Nobody in this section on this date"
              description="No child's placement covered this date. Place children under Students › The roll." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Roll</th><th>Child</th><th>Admission no</th><th>Mark</th><th>Reason</th></tr></thead>
                <tbody>
                  {register.students.map((r) => (
                    <tr key={r.studentDocsId}>
                      <td className="mono">{r.rollNo ?? '—'}</td>
                      <td>
                        {r.fullName ?? <span className="mono muted">{r.studentDocsId}</span>}
                        {r.onRoster ? null : <> <Badge tone="warn">no longer in this section</Badge></>}
                      </td>
                      <td className="mono">{r.admissionNo ?? '—'}</td>
                      <td>{r.mark ? <Badge tone={MARK_TONE[r.mark.status]}>{r.mark.status}</Badge> : <span className="muted">not marked</span>}</td>
                      <td>{r.mark?.reason ?? <span className="muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted">
            <Info size={12} /> <b>Everyone who was in the section on this date</b> — not only who is
            in it today, so a child who moved later still appears here. Marking is the next
            endpoint (#2), not built yet.
          </p>
        </Card>
      ) : null}
    </div>
  )
}
