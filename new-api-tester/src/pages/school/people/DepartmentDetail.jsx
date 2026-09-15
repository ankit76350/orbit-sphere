import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Info, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import AddDepartment from './AddDepartment.jsx'
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
 *
 * A SUB-DEPARTMENT IS CREATED HERE TOO, for the same reason and with the same shape: #9's
 * parentDepartmentDocsId is this unit's id, which is on screen here and nowhere else. The list
 * page adds top-level units and does not draw the box at all.
 *
 * A SEAT IS CREATED HERE, AND NOWHERE ELSE. #13 needs a departmentDocsId, and this page is one
 * department — so the list no longer offers it. Moved 2026-09-15: the list used to hold a
 * session-only table of what it had created, which showed seats the school held nowhere and
 * showed none of the ones it did. Here the table is #52's answer, so a new seat appears in it by
 * re-reading rather than by being remembered.
 */

const LIST = screenPath('school', 'people', 'departments')

export default function DepartmentDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [seatOpen, setSeatOpen] = useState(false)
  const [subOpen, setSubOpen] = useState(false)

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
        <Button icon={Plus} onClick={() => setSubOpen(true)}>Add a sub-department</Button>
        <Button look="primary" icon={Plus} onClick={() => setSeatOpen(true)}>Add a seat</Button>
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
        action={
          <div className="btn-row">
            <EndpointTag id="create-department" name="Add a sub-department" />
            <Badge>{data?.subDepartmentCount ?? 0} total</Badge>
            <Button icon={Plus} onClick={() => setSubOpen(true)}>Add</Button>
          </div>
        }
      >
        {(data?.subDepartments ?? []).length === 0 ? (
          <Empty
            title="A leaf"
            description="Nothing sits under this unit. #9 nests one under it — the parent id is filled in for you."
            action={<Button icon={Plus} onClick={() => setSubOpen(true)}>Add a sub-department</Button>}
          />
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
            <EndpointTag id="create-position" name="Add a seat" />
            <Badge>{data?.positionCount ?? 0} total</Badge>
            <Badge tone="good">{data?.activePositionCount ?? 0} active</Badge>
            <Badge tone={data?.teachingPositionCount ? 'brand' : undefined}>
              {data?.teachingPositionCount ?? 0} teaching
            </Badge>
            <Button icon={Plus} onClick={() => setSeatOpen(true)}>Add</Button>
          </div>
        }
      >
        {(data?.positions ?? []).length === 0 ? (
          <Empty
            title="No seats yet"
            description="Nobody can be employed here until one exists — #13 creates one, from the button above."
            action={<Button icon={Plus} onClick={() => setSeatOpen(true)}>Add a seat</Button>}
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

      {/* The parent prop is what draws the parent box AND fills it — this unit's id. Editable,
          so DEPARTMENT_NOT_FOUND and a top-level unit both stay reachable from here. */}
      <AddDepartment
        open={subOpen}
        parent={data ?? { departmentDocsId: id }}
        onClose={() => setSubOpen(false)}
        onAdded={load}
      />

      <AddPosition
        open={seatOpen}
        department={data ?? { departmentDocsId: id }}
        onClose={() => setSeatOpen(false)}
        onAdded={load}
      />
    </div>
  )
}

/**
 * Adding a position — #13.
 *
 * THE DEPARTMENT BOX STAYS, EVEN THOUGH THE PAGE IS ONE DEPARTMENT. Pre-filled from the address
 * bar, not fixed: an id that is retired, another school's, or nonsense is how
 * DEPARTMENT_NOT_ACTIVE and DEPARTMENT_NOT_FOUND are reached, and a modal that hid the field
 * would make two documented refusals untestable. Change it and the seat lands elsewhere — the
 * table below will not show it, which is the honest answer and not a bug.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a department gets its seats in one sitting. Only the
 * title is cleared — the department, the headcount and the teaching flag usually repeat. Each add
 * re-reads #52 behind it, so the seats table is the school's answer rather than a memory of what
 * this tab happened to write.
 *
 * A WARNING IS NOT AN ERROR. A department whose seats are all non-teaching gets one on a 201, and
 * it renders beside the success rather than as a refusal — the seat exists.
 */
function AddPosition({ open, department, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = {
    title: '',
    departmentDocsId: department?.departmentDocsId ?? '',
    reportsToPositionDocsId: '',
    approvedHeadcount: '',
    teachingPosition: false,
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, department?.departmentDocsId])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))
  const toggle = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.checked }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = { title: current.title, departmentDocsId: current.departmentDocsId }
    if (current.reportsToPositionDocsId.trim() !== '') {
      out.reportsToPositionDocsId = current.reportsToPositionDocsId.trim()
    }
    if (current.approvedHeadcount !== '') {
      out.approvedHeadcount = Number(current.approvedHeadcount)
    }
    if (current.teachingPosition) out.teachingPosition = true
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-position', { label: 'Add a position', body })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({ ...(old ?? initial), title: '' }))
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title={department?.name ? `Add a seat in ${department.name}` : 'Add a seat'}
      description="A seat has no code — its title is what names it, and must be unique inside its department."
      endpoint={<EndpointTag id="create-position" name="Add" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Add</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{made.title}</span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        {/* Rides on a SUCCESSFUL response. Kept apart from the refusal above so it never reads
            as a failure — the seat was created. */}
        {made?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">created, with a warning</span>
            </div>
            <pre className="resp-body">{made.warning}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Title"
            required
            hint="What names the seat now that positions have no code. Unique within the department, case-folded — retired seats included."
            error={errors.title}
          >
            <Input value={current.title} error={errors.title}
              onChange={set('title')} placeholder="Mathematics Teacher" />
          </Field>
          <Field
            label="Department id"
            required
            hint="Must be ACTIVE, not merely present. Pre-filled with the unit on screen — change it to reach DEPARTMENT_NOT_ACTIVE or NOT_FOUND."
            error={errors.departmentDocsId}
          >
            <Input value={current.departmentDocsId} error={errors.departmentDocsId}
              onChange={set('departmentDocsId')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Approved headcount"
            hint="Blank becomes 1. Null is NOT uncapped — the model forbids it. 0 and below are refused."
            error={errors.approvedHeadcount}
          >
            <Input type="number" value={current.approvedHeadcount} error={errors.approvedHeadcount}
              onChange={set('approvedHeadcount')} placeholder="1" />
          </Field>
          <Field
            label="Reports to (position id)"
            hint="Optional, and may be in ANOTHER department — the org tree and the reporting line answer different questions."
            error={errors.reportsToPositionDocsId}
          >
            <Input value={current.reportsToPositionDocsId} error={errors.reportsToPositionDocsId}
              onChange={set('reportsToPositionDocsId')} placeholder="leave blank for nobody" />
          </Field>
        </div>

        <Field
          label="Teaching position"
          hint="Defaults to false, and should almost always be sent. It is what a teacher picker filters on."
        >
          <label className="check">
            <input type="checkbox" checked={current.teachingPosition}
              onChange={toggle('teachingPosition')} />
            <span>Somebody in this seat teaches</span>
          </label>
        </Field>

        <p className="muted">
          <Info size={12} /> A department whose seats are <b>all</b> non-teaching gets a warning on
          a successful create. That is legitimate for Finance — and it is also exactly what an
          empty teacher picker looks like, which nothing else would tell you.
        </p>
      </div>
    </Modal>
  )
}
