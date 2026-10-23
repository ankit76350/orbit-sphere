import { useCallback, useEffect, useState } from 'react'
import { Info, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import Meter from '../../../components/ui/Meter.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * Headcount per class and section: /school-student/strength
 *
 * ONE ENDPOINT — #22 counts the ACTIVE records of the year selected in the navbar, per class and
 * section, next to each section's capacity. One aggregation plus one read of the classes.
 *
 * EMPTY SECTIONS ARE LISTED, as 0. A section with records but no longer on its class is listed
 * too, marked, so the totals add up.
 */
export default function Strength() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('student-strength', {
      label: `Strength of ${year}`,
      pathParams: { year },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setData(null); setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Strength" />

  const classes = data?.classes ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Strength</h1>
          <p className="muted">
            How many children are placed in each section of{' '}
            <b>{year || 'no year selected in the navbar'}</b>, against its seats.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="student-strength" name="Count" pathParams={{ year }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {data ? (
        <Card
          title={`${data.totalEnrolled} placed · ${data.totalCapacity} seats`}
          description="ACTIVE records only. A section with no capacity declared shows no bar."
        >
          {classes.length === 0 ? (
            <Empty title="No classes" description="This year has no classes yet, and nobody is placed in it." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Class</th>
                    <th>Section</th>
                    <th>Placed</th>
                    <th>Capacity</th>
                    <th>Seats left</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {classes.map((c) => [
                    <tr key={c.classDocsId}>
                      <td>
                        <b>{c.className ?? <span className="muted">class deleted</span>}</b>
                        {c.active === false ? <> <Badge>not active</Badge></> : null}
                      </td>
                      <td className="muted">all</td>
                      <td><b>{c.enrolled}</b></td>
                      <td>{c.capacity || <span className="muted">—</span>}</td>
                      <td></td>
                      <td></td>
                    </tr>,
                    ...c.sections.map((s) => (
                      <tr key={`${c.classDocsId}-${s.sectionNo}`}>
                        <td></td>
                        <td>
                          {s.sectionNo}
                          {s.active === false ? <> <Badge>not active</Badge></> : null}
                          {s.onClass ? null : <> <Badge tone="warn">not on the class</Badge></>}
                        </td>
                        <td>{s.enrolled}</td>
                        <td>{s.capacity ?? <span className="muted">none set</span>}</td>
                        <td>
                          {s.seatsLeft == null ? <span className="muted">—</span> : s.seatsLeft}
                          {s.overCapacity ? <> <Badge tone="bad">over</Badge></> : null}
                        </td>
                        <td style={{ minWidth: 120 }}>
                          {s.capacity ? <Meter value={s.enrolled} max={s.capacity} /> : null}
                        </td>
                      </tr>
                    )),
                  ])}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted">
            <Info size={12} /> <b>Two reads for the whole year</b>: one aggregation counting the
            ACTIVE records per class and section, and one read of the classes for the names and the
            seats. Never a count per section.
          </p>
        </Card>
      ) : null}
    </div>
  )
}
