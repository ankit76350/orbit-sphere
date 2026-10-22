import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'

/**
 * One paper, on its own page: /school-academics/exams/{termId}/exams/{examId}/papers/{paperId}
 *
 * ONE ENDPOINT — #12: the paper with its class, subject, rooms and invigilators, then who is
 * expected in the hall and how they were marked, then the marks entered for it.
 *
 * EXPECTED is the section's children on the paper's date who are ELIGIBLE candidates, plus anyone
 * already marked at this paper. Debarred children and children who are not candidates are counted
 * here, never listed on the hall register.
 */
export default function ExamPaper() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id: termId, examId, paperId } = useParams()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-exam-paper', { label: 'One paper', pathParams: { year, examScheduleDocsId: paperId ?? '' } })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, paperId])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const paper = data?.paper
  const hall = data?.hall
  const marks = data?.marks
  const title = paper
    ? `${paper.subjectName ?? paper.subjectCode}${paper.componentName ? ` (${paper.componentName})` : ''} — ${paper.className ?? ''} ${paper.sectionNo}`
    : 'Paper'

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{title}</h1>
          <p className="muted">{data?.examName ?? ''}{paper ? ` · ${paper.examDate} ${paper.startTime}–${paper.endTime}` : ''}</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(childPath('school', 'academics', 'exams', termId ?? '', 'exams', examId ?? ''))}>
          {data?.examName ?? 'Exam'}
        </Button>
        <EndpointTag id="get-exam-paper" name="Paper" pathParams={{ year, examScheduleDocsId: paperId }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {paper ? (
        <>
          <Card title="The paper">
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><th>Class</th><td>{paper.className ?? paper.classDocsId} — section {paper.sectionNo}</td></tr>
                  <tr><th>Subject</th><td>{paper.subjectName ?? paper.subjectCode} <span className="mono muted">{paper.subjectCode}</span></td></tr>
                  <tr><th>Part</th><td>{paper.componentName ?? <span className="muted">whole subject</span>}</td></tr>
                  <tr><th>When</th><td className="mono">{paper.examDate} {paper.startTime}–{paper.endTime}</td></tr>
                  <tr><th>Marks</th><td>{paper.maximumMarks}{paper.passingMarks != null ? ` (pass ${paper.passingMarks})` : ''}</td></tr>
                  <tr><th>Rooms</th><td>
                    {paper.rooms.length === 0 ? <span className="muted">no rooms yet</span> : paper.rooms.map((r) => (
                      <div key={r.roomDocsId}>
                        <b>{r.roomName ?? r.roomDocsId}</b> · {r.seatCount} seat{r.seatCount === 1 ? '' : 's'} for the sitting
                        {r.invigilators.length ? ` · ${r.invigilators.map((i) => i.fullName ?? i.staffDocsId).join(', ')}` : ''}
                      </div>
                    ))}
                    {paper.rooms.length ? <div className="muted">{paper.totalSeats} seats in all, shared by the sections of this sitting</div> : null}
                  </td></tr>
                  <tr><th>Status</th><td><Badge>{paper.status}</Badge></td></tr>
                  <tr><th>Version</th><td className="mono">{paper.version}</td></tr>
                  <tr><th>Id</th><td className="mono muted">{paper.examScheduleDocsId}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card title="In the hall" description="Who should sit this paper, and what the hall register says so far.">
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><th>In the section that day</th><td>{hall.sectionSize}</td></tr>
                  <tr><th>Eligible candidates</th><td>{hall.eligible}</td></tr>
                  <tr><th>Debarred — not on the register</th><td>{hall.debarred}</td></tr>
                  <tr><th>Not candidates — not on the register</th><td>{hall.notCandidates}</td></tr>
                  <tr><th>Expected in the hall</th><td><b>{hall.expected}</b>{paper.rooms.length ? <span className="muted"> · {paper.totalSeats} seats</span> : null}</td></tr>
                  <tr><th>Present</th><td>{hall.present}</td></tr>
                  <tr><th>Absent</th><td>{hall.absent}</td></tr>
                  <tr><th>Unfair means</th><td>{hall.unfairMeans}</td></tr>
                  <tr><th>Exempt</th><td>{hall.exempt}</td></tr>
                  <tr><th>Not marked yet</th><td>{hall.notMarked}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card title={`Marks — ${marks.entered} entered`} description="Only a child marked present gets a mark.">
            <p>
              {Object.entries(marks.byStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
            <p className="muted">Present with no mark yet: {marks.notEntered}</p>
          </Card>
        </>
      ) : null}
    </div>
  )
}
