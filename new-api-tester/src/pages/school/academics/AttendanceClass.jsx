import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath, screenPath } from '../../../paths.js'

/**
 * Attendance, step 2 of 3: /school-academics/attendance/{classId}
 *
 * THE SECTIONS OF ONE CLASS. Pick one to see its registers. A section that is switched off is
 * still listed and still opens — opening a register in it is a documented 409.
 */
export default function AttendanceClass() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('list-class-sections', {
      label: 'Sections of the class',
      pathParams: { year, id: id ?? '' },
    })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Attendance" />

  const sections = data?.sections ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.className ?? 'Class'}</h1>
          <p className="muted">Pick a section to see its attendance registers.</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(screenPath('school', 'academics', 'attendance'))}>Classes</Button>
        <EndpointTag id="list-class-sections" name="Sections" pathParams={{ year, id }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {data ? (
        <Card title={`${sections.length} section${sections.length === 1 ? '' : 's'}`}>
          {sections.length === 0 ? (
            <Empty title="No sections" description="This class has no sections yet." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Section</th><th>Status</th><th>Capacity</th><th></th></tr></thead>
                <tbody>
                  {sections.map((s) => (
                    <tr key={s.sectionNo} style={{ cursor: 'pointer' }}
                      onClick={() => navigate(childPath('school', 'academics', 'attendance', id ?? '', 'sections', s.sectionNo))}>
                      <td><b>{s.sectionNo}</b></td>
                      <td>{s.active === false ? <Badge>not active</Badge> : <Badge tone="good">active</Badge>}</td>
                      <td>{s.capacity ?? <span className="muted">—</span>}</td>
                      <td><Button icon={ChevronRight}>Registers</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      ) : null}
    </div>
  )
}
