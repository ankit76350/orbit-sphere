import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One department, at its own address: /school-people/departments/{id}
 *
 * ITS OWN PAGE, NOT A MODAL. A unit carries the unit above it, the units under it and every seat —
 * more than a modal's worth of screen — and a page has an address, so it can be linked, reloaded
 * and shared. The same call this project made for a class.
 *
 * ONE ENDPOINT FILLS IT. #52 returns the unit, its parentDepartment and head RESOLVED, its
 * sub-departments and its seats, in one read. Without it this page was four requests.
 *
 * THIS IS THE ONLY SCREEN IN THE MODULE THAT SHOWS A NAME WHERE THE OTHERS SHOW AN ID. #9, #12 and
 * #13 all return raw ids on purpose, so that one place decides how a unit and a person are
 * presented — and this is that place.
 *
 * A DANGLING PARENT OR HEAD IS OMITTED BY THE API, NOT A 404. So the page renders "none" and says
 * which, because "there is no head" and "the head's record was deleted" look identical from here.
 *
 * THE SEATS ARE #52'S, NOT #15'S. When GET /positions is built the two will return the same rows,
 * and #15 wins — it owns the question. That is what happened to #29 of academics.
 */

const LIST = screenPath('school', 'people', 'departments')

export default function DepartmentDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-department', {
      label: 'One department in full',
      pathParams: { id },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="This department" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All departments</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'DEPARTMENT_NOT_FOUND'
                ? 'No department with this id in this school. A unit belongs to one school, so '
                  + "another school's real id is a 404 rather than somebody else's org chart."
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  return (
    <div className="page stack">
      <Link className="back" to={LIST}><ArrowLeft size={13} /> All departments</Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.name ?? 'Reading the department'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data ? <> · <span className="mono">{data.departmentCode}</span></> : null}
            {data ? ` · ${data.subDepartmentCount} sub-unit${data.subDepartmentCount === 1 ? '' : 's'}` : ''}
            {data ? ` · ${data.positionCount} seat${data.positionCount === 1 ? '' : 's'}` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      <Card
        title="The unit"
        description="Everything #52 returns about it, in one read — with the unit above it and the head resolved."
        action={<EndpointTag id="get-department" name="Read" pathParams={{ id }} />}
      >
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Code</td>
                <td><span className="mono">{data?.departmentCode}</span></td></tr>
              <tr><td className="muted">Name</td><td>{data?.name}</td></tr>
              {data?.description
                ? <tr><td className="muted">Description</td><td>{data.description}</td></tr>
                : null}
              <tr><td className="muted">Status</td>
                <td>{data
                  ? <Badge tone={data.active ? 'good' : undefined}>
                      {data.active ? 'active' : 'retired'}
                    </Badge>
                  : null}</td></tr>
              {/* RESOLVED — the one endpoint in this module that does. */}
              <tr><td className="muted">Parent department</td>
                <td>{data?.parentDepartment
                  ? <>
                      <span className="mono">{data.parentDepartment.departmentCode}</span>{' '}
                      {data.parentDepartment.name}
                      {data.parentDepartment.active ? null : <Badge>retired</Badge>}
                    </>
                  : <span className="muted">top level, or that record was deleted</span>}</td></tr>
              <tr><td className="muted">Head</td>
                <td>{data?.headStaff
                  ? <>{data.headStaff.fullName}{' '}
                      <span className="muted mono">{data.headStaff.staffDocsId}</span></>
                  : <span className="muted">none named, or the record was deleted</span>}</td></tr>
              <tr><td className="muted">Unit id</td>
                <td><span className="mono">{data?.departmentDocsId}</span></td></tr>
            </tbody>
          </table>
        </div>
        <p className="muted">
          <Info size={12} /> The head resolves to a <b>name and nothing else</b>. A staff record
          carries an address, a date of birth and a national identity number, and this module has
          no authorization yet.
        </p>
      </Card>

      <Card
        title="Sub-departments"
        description="Only one level — the whole nesting is #12 with ?tree=true, built from one flat read."
        action={<Badge>{data?.subDepartmentCount ?? 0} total</Badge>}
      >
        {(data?.subDepartments ?? []).length === 0 ? (
          <Empty title="A leaf" description="Nothing sits under this unit." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                {(data?.subDepartments ?? []).map((one) => (
                  <tr key={one.departmentDocsId}>
                    <td><span className="mono">{one.departmentCode}</span></td>
                    <td>{one.name}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card
        title="Seats"
        description="Every position in this unit, retired ones included and marked — a record made against one still names it."
        action={
          <div className="btn-row">
            <Badge>{data?.positionCount ?? 0} total</Badge>
            <Badge tone="good">{data?.activePositionCount ?? 0} active</Badge>
            <Badge tone={data?.teachingPositionCount ? 'brand' : undefined}>
              {data?.teachingPositionCount ?? 0} teaching
            </Badge>
          </div>
        }
      >
        {(data?.positions ?? []).length === 0 ? (
          <Empty
            title="No seats yet"
            description="Nobody can be employed here until one exists — #13 creates one, from the department list."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Approved</th>
                  <th>Teaching</th>
                  <th>Reports to</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {(data?.positions ?? []).map((one) => (
                  <tr key={one.positionDocsId}>
                    {/* The title IS the identity — positionCode was removed 2026-09-15. */}
                    <td>{one.title}</td>
                    <td>{one.approvedHeadcount}</td>
                    <td>{one.teachingPosition
                      ? <Badge tone="brand">teaching</Badge>
                      : <span className="muted">no</span>}</td>
                    {/* A raw id, and it may point at a seat in ANOTHER department — the org tree
                        and the reporting line are deliberately not kept consistent. */}
                    <td>{one.reportsToPositionDocsId
                      ? <span className="mono">{one.reportsToPositionDocsId}</span>
                      : <span className="muted">nobody</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> <b>The filled count is not here.</b> It comes from current employment
          records, so a number returned now would be stale the moment somebody is hired. #15
          computes it, and is not built.
        </p>
      </Card>
    </div>
  )
}
