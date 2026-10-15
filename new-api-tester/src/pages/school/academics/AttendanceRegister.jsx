import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, RefreshCw, Save } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Input } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'

/**
 * One attendance register, on its own page:
 * /school-academics/attendance/{classId}/sections/{sectionNo}/registers/{sessionId}
 *
 * TWO ENDPOINTS — #6 reads the register: every child who was in the section on its date, each with
 * their mark or "not marked". #2 saves marks: change a child's status (and reason, unless PRESENT),
 * then Save. Only the children changed on this page are sent.
 *
 * NOTHING IS DISABLED — saving with nothing changed is 400 NOTHING_TO_MARK, an absence with no
 * reason is 400 REASON_REQUIRED, and a locked register is 409 SESSION_NOT_OPEN.
 */

const STATUSES = ['', 'PRESENT', 'LATE', 'AUTHORISED_ABSENCE', 'UNAUTHORISED_ABSENCE']

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
  //! WHAT HAS BEEN CHANGED ON THIS PAGE, by child: { status, reason }. Only these are sent.
  const [draft, setDraft] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveResult, setSaveResult] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-attendance-register', {
      label: 'One register',
      pathParams: { year, attendanceSessionDocsId: sessionId ?? '' },
    })
    setLoading(false)
    if (answer.ok) { setRegister(answer.bodyJson); setProblem(null); setDraft({}) } else { setRegister(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, sessionId])

  //! THE MARKS TO SEND — every child changed here, with the status shown and the reason typed.
  const changedMarks = Object.entries(draft).map(([studentDocsId, one]) => ({
    studentDocsId,
    ...(one.status ? { status: one.status } : {}),
    ...(one.reason ? { reason: one.reason } : {}),
  }))

  const save = async () => {
    setSaving(true)
    const answer = await call('mark-attendance-register', {
      label: 'Save marks',
      pathParams: { year, attendanceSessionDocsId: sessionId ?? '' },
      body: { marks: changedMarks },
    })
    setSaving(false)
    setSaveResult(answer)
    //! THE ANSWER IS THE REGISTER AS IT NOW STANDS, so it replaces the page's copy.
    if (answer.ok) { setRegister(answer.bodyJson); setDraft({}) }
  }

  const statusOf = (row) => draft[row.studentDocsId]?.status ?? row.mark?.status ?? ''
  const reasonOf = (row) => draft[row.studentDocsId]?.reason ?? row.mark?.reason ?? ''
  const change = (row, key, value) => setDraft({
    ...draft,
    [row.studentDocsId]: { status: statusOf(row), reason: reasonOf(row), [key]: value },
  })

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
          action={
            <div className="btn-row">
              <EndpointTag id="mark-attendance-register" name="Mark"
                pathParams={{ year, attendanceSessionDocsId: sessionId }} />
              <Badge tone={session.status === 'OPEN' ? 'good' : undefined}>{session.status}</Badge>
            </div>
          }
        >
          {register.students.length === 0 ? (
            <Empty title="Nobody in this section on this date"
              description="No child's placement covered this date. Place children under Students › The roll." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Roll</th><th>Child</th><th>Admission no</th><th>Saved</th><th>Mark</th><th>Reason</th></tr></thead>
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
                      <td style={{ minWidth: 190 }}>
                        <Select value={statusOf(r)} options={STATUSES}
                          onChange={(v) => change(r, 'status', v)} />
                        {draft[r.studentDocsId] ? <div className="muted">changed</div> : null}
                      </td>
                      <td>
                        <Input value={reasonOf(r)}
                          placeholder={statusOf(r) && statusOf(r) !== 'PRESENT' ? 'required' : 'optional'}
                          onChange={(e) => change(r, 'reason', e.target.value)} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="toolbar">
            <Button look="primary" icon={Save} busy={saving} onClick={save}>
              Save marks{changedMarks.length ? ` (${changedMarks.length})` : ''}
            </Button>
          </div>

          {saveResult && !saveResult.ok ? (
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="false">{saveResult.bodyJson?.code ?? saveResult.status}</span>
              </div>
              <pre className="resp-body">{saveResult.bodyJson?.message ?? saveResult.bodyText}</pre>
            </div>
          ) : saveResult?.ok ? (
            <p className="muted"><Badge tone="good">saved</Badge> {saveResult.bodyJson?.nextStep}</p>
          ) : null}

          <p className="muted">
            <Info size={12} /> <b>Everyone who was in the section on this date</b> — not only who is
            in it today. <b>Only the children changed here are sent</b>; re-sending a child corrects
            their mark. Every mark except PRESENT needs a reason.
          </p>
        </Card>
      ) : null}
    </div>
  )
}
