import { useCallback, useEffect, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'

/**
 * Attendance tab "Classes": /school-academics/attendance/classes
 *
 * THE CLASSES OF THE YEAR SELECTED IN THE NAVBAR. Pick one to see its sections, pick a section to
 * see its registers, open a register to see its children. Each step is its own address.
 */
export default function AttendanceClasses({ reloadKey }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const year = actingAcademicYear ?? ''
  const [classes, setClasses] = useState(null)
  const [problem, setProblem] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    const answer = await call('list-school-classes', {
      label: `Classes in ${year}`,
      pathParams: { year },
      query: { page: '0', size: '100' },
    })
    if (answer.ok) { setClasses(answer.bodyJson?.content ?? []); setProblem(null) } else { setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, reloadKey])

  useEffect(() => { load() }, [load])

  return (
    <>
      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      <Card
        title={classes ? `${classes.length} class${classes.length === 1 ? '' : 'es'}` : 'Classes'}
        description="Pick a class, then a section, then a register."
        action={<EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} />}
      >
        {classes && classes.length === 0 ? (
          <Empty title="No classes in this year" description="Create classes under Academics › Classes first." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Class</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {(classes ?? []).map((c) => (
                  <tr key={c.schoolClassId} style={{ cursor: 'pointer' }}
                    onClick={() => navigate(detailPath('school', 'academics', 'attendance', c.schoolClassId))}>
                    <td><b>{c.name}</b></td>
                    <td>{c.active === false ? <Badge>not active</Badge> : <Badge tone="good">active</Badge>}</td>
                    <td><Button icon={ChevronRight}>Sections</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  )
}
