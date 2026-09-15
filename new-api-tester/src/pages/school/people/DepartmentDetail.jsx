import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ChevronRight, Info, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { detailPath, screenPath } from '../../../paths.js'
import AddDepartment from './AddDepartment.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One department, at its own address: /school-people/departments/{id}
 *
 * IT OPENS ITSELF, AT EVERY DEPTH. A sub-department row navigates to this same page for that
 * unit, because a department is a department wherever it sits — so the chart is walked downwards
 * one address at a time, and each of those addresses can be linked and reloaded.
 *
 * ITS OWN PAGE, NOT A MODAL. A unit carries the unit above it, the units under it and every position —
 * more than a modal's worth of screen — and a page has an address, so it can be linked, reloaded
 * and shared. The same call this project made for a class.
 *
 * ONE ENDPOINT FILLS IT. #52 returns the unit, its parentDepartment and head RESOLVED, its
 * sub-departments and its positions, in one read. Without it this page was four requests.
 *
 * THIS IS THE ONLY SCREEN IN THE MODULE THAT SHOWS A NAME WHERE THE OTHERS SHOW AN ID. #9, #12 and
 * #13 all return raw ids on purpose, so that one place decides how a unit and a person are
 * presented — and this is that place.
 *
 * A DANGLING PARENT OR HEAD IS OMITTED BY THE API, NOT A 404. So the page renders "none" and says
 * which, because "there is no head" and "the head's record was deleted" look identical from here.
 *
 * THE POSITIONS ARE #52'S, NOT #15'S. When GET /positions is built the two will return the same rows,
 * and #15 wins — it owns the question. That is what happened to #29 of academics.
 *
 * AND IT IS WHERE A UNIT IS EDITED — #10, from the toolbar for this unit and from the row for
 * each one under it. The list cannot: a rename is a thing you do to the unit you are looking at,
 * and the two fields #10 refuses (the code and the parent) are shown in the modal as text beside
 * the ones it accepts, so what cannot move is visible rather than merely absent.
 *
 * A SUB-DEPARTMENT IS CREATED HERE TOO, for the same reason and with the same shape: #9's
 * parentDepartmentDocsId is this unit's id, which is on screen here and nowhere else. The list
 * page adds top-level units and does not draw the box at all.
 *
 * AND A POSITION IS EDITED HERE TOO — #14, from its row. A position row carries everything #14 edits, so
 * that modal opens on the row itself where a sub-department's has to read #52 first.
 *
 * A POSITION IS CREATED HERE, AND NOWHERE ELSE. #13 needs a departmentDocsId, and this page is one
 * department — so the list no longer offers it. Moved 2026-09-15: the list used to hold a
 * session-only table of what it had created, which showed positions the school held nowhere and
 * showed none of the ones it did. Here the table is #52's answer, so a new position appears in it by
 * re-reading rather than by being remembered.
 */

const LIST = screenPath('school', 'people', 'departments')

export default function DepartmentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [positionOpen, setPositionOpen] = useState(false)
  const [subOpen, setSubOpen] = useState(false)
  // THE DOCUMENT BEING EDITED, not a boolean — because a sub-department row can open this too
  // and #10 edits four fields, two of which a row does not carry. Opening on a summary would
  // show an empty description box for a unit that has one.
  const [editTarget, setEditTarget] = useState(null)
  // THE POSITION BEING EDITED. Unlike a sub-department row, a position row carries everything #14 edits —
  // #52 returns the whole PositionResponse — so there is nothing to read first.
  const [positionTarget, setPositionTarget] = useState(null)

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

  //! THE ROUTE PARAM CHANGES WITHOUT REMOUNTING. Opening a sub-department from a row swaps `id`
  //! under a live component, so a modal left open would keep editing the unit you just left.
  useEffect(() => { setEditTarget(null); setPositionTarget(null); setPositionOpen(false); setSubOpen(false) }, [id])

  //! A ROW IS A SUMMARY — four fields. #10 edits description and head too, so the full document
  //! is read first and the modal opens on that. One extra request, and the alternative is a form
  //! that shows blanks for values the unit actually holds.
  const openEditor = useCallback(async (departmentDocsId) => {
    if (departmentDocsId === id) { setEditTarget(data); return }
    const result = await call('get-department', {
      label: 'Read a sub-department before editing it',
      pathParams: { id: departmentDocsId },
    })
    setEditTarget(result.ok ? result.bodyJson : { departmentDocsId })
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, id, data])

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
            {data ? ` · ${data.positionCount} position${data.positionCount === 1 ? '' : 's'}` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button icon={Pencil} onClick={() => openEditor(id)}>Edit</Button>
        <Button icon={Plus} onClick={() => setSubOpen(true)}>Add a sub-department</Button>
        <Button look="primary" icon={Plus} onClick={() => setPositionOpen(true)}>Add a position</Button>
      </div>

      <Card
        title="The unit"
        description="Everything #52 returns about it, in one read — with the unit above it and the head resolved."
        action={
          <div className="btn-row">
            <EndpointTag id="get-department" name="Read" pathParams={{ id }} />
            <EndpointTag id="update-department" name="Edit" pathParams={{ id }} />
            <Button icon={Pencil} onClick={() => openEditor(id)}>Edit</Button>
          </div>
        }
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
              {/* A LINK, because the rows below navigate DOWNWARDS. Without this the only way
                  back up a chart you walked into is the browser's back button, and the list
                  behind "All departments" shows top-level units only. */}
              <tr><td className="muted">Parent department</td>
                <td>{data?.parentDepartment
                  ? <Link to={detailPath('school', 'people', 'departments',
                      data.parentDepartment.departmentDocsId)}>
                      <span className="mono">{data.parentDepartment.departmentCode}</span>{' '}
                      {data.parentDepartment.name}
                      {data.parentDepartment.active ? null : <Badge>retired</Badge>}
                    </Link>
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
                  /* THE SAME PAGE, FOR THE UNIT UNDER THIS ONE. A department is a department at
                     every depth, so opening one is an address rather than a different screen —
                     which is also how you reach ITS sub-departments, all the way down. */
                  <tr
                    key={one.departmentDocsId}
                    data-opens
                    onClick={() => navigate(detailPath('school', 'people', 'departments',
                      one.departmentDocsId))}
                  >
                    <td><span className="mono">{one.departmentCode}</span></td>
                    <td>{one.name}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    {/* #10 ON THE ROW. It reads the row's own document first — a summary carries
                        four fields and #10 edits two more. */}
                    <td>
                      {/* Stops the click, or editing would navigate away from the row first. */}
                      <Button icon={Pencil}
                        onClick={(event) => {
                          event.stopPropagation()
                          openEditor(one.departmentDocsId)
                        }}>
                        Edit
                      </Button>
                    </td>
                    <td><span className="muted">Open <ChevronRight size={13} /></span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card
        title="Positions/Positions"
        description="Every position in this unit, retired ones included and marked — a record made against one still names it."
        action={
          <div className="btn-row">
            <EndpointTag id="create-position" name="Add a position" />
            <EndpointTag id="update-position" name="Edit a position" />
            <Badge>{data?.positionCount ?? 0} total</Badge>
            <Badge tone="good">{data?.activePositionCount ?? 0} active</Badge>
            <Badge tone={data?.teachingPositionCount ? 'brand' : undefined}>
              {data?.teachingPositionCount ?? 0} teaching
            </Badge>
            <Button icon={Plus} onClick={() => setPositionOpen(true)}>Add</Button>
          </div>
        }
      >
        {(data?.positions ?? []).length === 0 ? (
          <Empty
            title="No positions yet"
            description="Nobody can be employed here until one exists — #13 creates one, from the button above."
            action={<Button icon={Plus} onClick={() => setPositionOpen(true)}>Add a position</Button>}
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
                  <th>Position id</th>
                  <th />
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
                    {/* A raw id, and it may point at a position in ANOTHER department — the org tree
                        and the reporting line are deliberately not kept consistent. */}
                    <td>{one.reportsToPositionDocsId
                      ? <span className="mono">{one.reportsToPositionDocsId}</span>
                      : <span className="muted">nobody</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    {/* The whole identity, since positionCode was removed — it is what an
                        employment record stores, and what #14 and #16 are addressed by. */}
                    <td><span className="muted mono">{one.positionDocsId}</span></td>
                    {/* #14. The row IS the document — nothing to read first. */}
                    <td>
                      <Button icon={Pencil} onClick={() => setPositionTarget(one)}>Edit</Button>
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
      <EditDepartment
        open={editTarget != null}
        department={editTarget}
        departmentDocsId={editTarget?.departmentDocsId ?? id}
        onClose={() => setEditTarget(null)}
        onSaved={load}
      />

      <AddDepartment
        open={subOpen}
        parent={data ?? { departmentDocsId: id }}
        onClose={() => setSubOpen(false)}
        onAdded={load}
      />

      <EditPosition
        open={positionTarget != null}
        position={positionTarget}
        onClose={() => setPositionTarget(null)}
        onSaved={load}
      />

      <AddPosition
        open={positionOpen}
        department={data ?? { departmentDocsId: id }}
        onClose={() => setPositionOpen(false)}
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
 * would make two documented refusals untestable. Change it and the position lands elsewhere — the
 * table below will not show it, which is the honest answer and not a bug.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a department gets its positions in one sitting. Only the
 * title is cleared — the department, the headcount and the teaching flag usually repeat. Each add
 * re-reads #52 behind it, so the positions table is the school's answer rather than a memory of what
 * this tab happened to write.
 *
 * A WARNING IS NOT AN ERROR. A department whose positions are all non-teaching gets one on a 201, and
 * it renders beside the success rather than as a refusal — the position exists.
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
      title={department?.name ? `Add a position in ${department.name}` : 'Add a position'}
      description="A position has no code — its title is what names it, and must be unique inside its department."
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
            as a failure — the position was created. */}
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
            hint="What names the position now that positions have no code. Unique within the department, case-folded — retired positions included."
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
            <span>Somebody in this position teaches</span>
          </label>
        </Field>

        <p className="muted">
          <Info size={12} /> A department whose positions are <b>all</b> non-teaching gets a warning on
          a successful create. That is legitimate for Finance — and it is also exactly what an
          empty teacher picker looks like, which nothing else would tell you.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Editing a unit — #10.
 *
 * IT SENDS WHAT YOU CHANGED, AND NOTHING ELSE. Every field on this endpoint is optional and
 * absent means "leave it alone", so a body carrying fields nobody touched would be a write
 * claiming more than the person asked for. The preview shows the exact body, always.
 *
 * WHICH MAKES THE EMPTY-BODY REFUSAL REACHABLE BY DOING NOTHING. Open this, change nothing, press
 * Save: the body is `{}` and the API answers 400 NOTHING_TO_UPDATE. That is a documented refusal,
 * and a modal that quietly refused to submit would have hidden it.
 *
 * THE CODE AND THE PARENT ARE SHOWN AS TEXT, NOT AS BOXES. They cannot be edited — and a unit's
 * page is where somebody goes looking to try, so saying "this is the code, and it does not change"
 * is more use than leaving them out and letting the reader wonder whether they were forgotten.
 * They are not inputs at all rather than inputs that are greyed out.
 *
 * CLEARING IS EMPTYING A BOX. "" on description or headStaffDocsId removes what is there; "" on
 * the name is refused by the API, which is why the box is still submittable empty.
 */
function EditDepartment({ open, department, departmentDocsId, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = {
    name: department?.name ?? '',
    description: department?.description ?? '',
    headStaffDocsId: department?.headStaff?.staffDocsId ?? '',
    active: department?.active ?? true,
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, department?.departmentDocsId, department?.name])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))
  const toggle = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.checked }))

  // ONLY WHAT MOVED. Absent means "leave it alone" on this endpoint, so an untouched field must
  // not appear — and when nothing moved the body is {}, which is the 400 this endpoint documents.
  const body = (() => {
    const out = {}
    if (current.name !== initial.name) out.name = current.name
    if (current.description !== initial.description) out.description = current.description
    if (current.headStaffDocsId !== initial.headStaffDocsId) {
      out.headStaffDocsId = current.headStaffDocsId
    }
    if (current.active !== initial.active) out.active = current.active
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-department', {
      label: 'Edit a department',
      pathParams: { id: departmentDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onSaved()
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
      title={department?.name ? `Edit ${department.name}` : 'Edit this department'}
      description="Only what you change is sent — absent means leave it alone, so an untouched field never appears in the body."
      endpoint={<EndpointTag id="update-department" name="Save" look="primary"
        pathParams={{ id: departmentDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
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
              <span className="resp-status" data-ok="true">{made.name}</span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Name"
            hint="Empty is refused, not treated as a clear — DEPARTMENT_NAME_REQUIRED. Try it."
            error={errors.name}
          >
            <Input value={current.name} error={errors.name} onChange={set('name')} />
          </Field>
          <Field
            label="Head staff id"
            hint="Empty removes the head. A real Staff.id is checked to EXIST, not to be employed — another school's is a 404."
            error={errors.headStaffDocsId}
          >
            <Input value={current.headStaffDocsId} error={errors.headStaffDocsId}
              onChange={set('headStaffDocsId')} placeholder="empty for no head" />
          </Field>
        </div>

        <Field label="Description" hint="Empty removes it." error={errors.description}>
          <Input value={current.description} error={errors.description}
            onChange={set('description')} placeholder="empty to remove it" />
        </Field>

        <Field
          label="Active"
          hint="Retiring is refused while positions are still active — 409 DEPARTMENT_NOT_EMPTY, naming how many. Restoring has no check."
        >
          <label className="check">
            <input type="checkbox" checked={current.active} onChange={toggle('active')} />
            <span>This unit is in use</span>
          </label>
        </Field>

        {/* TEXT, NOT BOXES. Neither can be edited, and this is the page somebody would come to
            looking to try — so they are stated rather than left out. */}
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Code, which #10 never accepts</td>
                <td><span className="mono">{department?.departmentCode}</span>{' '}
                  <span className="muted">
                    nothing joins on it, which is what makes editing it dangerous — no query would
                    break and every export naming the old code would quietly stop matching
                  </span></td></tr>
              <tr><td className="muted">Parent, which #10 never accepts</td>
                <td>{department?.parentDepartment
                  ? <span className="mono">{department.parentDepartment.departmentCode}</span>
                  : <span className="muted">top level</span>}{' '}
                  <span className="muted">
                    a unit cannot be moved — where it sits is decided when it is created, and
                    dropping the move is why nothing in this API can write a cycle any more
                  </span></td></tr>
            </tbody>
          </table>
        </div>

        <p className="muted">
          <Info size={12} /> Send them anyway if you like — they are not on the request record, so
          they are <b>ignored rather than refused</b>, and a body of only those two is the same{' '}
          <span className="mono">400 NOTHING_TO_UPDATE</span> as an empty one.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Editing a position — #14.
 *
 * THE ROW IS THE DOCUMENT. #52 returns every field #14 edits, so this opens on the row itself —
 * where a sub-department's editor has to read #52 first, because a sub-department row is a
 * four-field summary.
 *
 * IT SENDS WHAT YOU CHANGED, AND NOTHING ELSE, for the same reason as #10's modal: absent means
 * "leave it alone", so a body carrying untouched fields would claim more than the person asked
 * for. Change nothing and press Save and the body is `{}` — which is the documented
 * 400 NOTHING_TO_UPDATE, reachable by doing nothing.
 *
 * THE DEPARTMENT IS SHOWN AS TEXT, NOT A BOX. A position cannot move department, and this is the page
 * somebody would come to looking to try — so it is stated with its reason rather than left out.
 *
 * A WARNING IS NOT A REFUSAL. Turning off a unit's last teaching position succeeds and says so; the
 * warning renders beside the success, never as an error.
 */
function EditPosition({ open, position, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const initial = {
    title: position?.title ?? '',
    reportsToPositionDocsId: position?.reportsToPositionDocsId ?? '',
    approvedHeadcount: String(position?.approvedHeadcount ?? ''),
    teachingPosition: position?.teachingPosition ?? false,
    active: position?.active ?? true,
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, position?.positionDocsId])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))
  const toggle = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.checked }))

  // ONLY WHAT MOVED — and {} when nothing did, which is the 400 this endpoint documents.
  const body = (() => {
    const out = {}
    if (current.title !== initial.title) out.title = current.title
    if (current.reportsToPositionDocsId !== initial.reportsToPositionDocsId) {
      out.reportsToPositionDocsId = current.reportsToPositionDocsId
    }
    if (current.approvedHeadcount !== initial.approvedHeadcount) {
      // Sent as typed when it is not a number, so the validation refusal stays reachable.
      const asNumber = Number(current.approvedHeadcount)
      out.approvedHeadcount = current.approvedHeadcount === '' || Number.isNaN(asNumber)
        ? current.approvedHeadcount
        : asNumber
    }
    if (current.teachingPosition !== initial.teachingPosition) {
      out.teachingPosition = current.teachingPosition
    }
    if (current.active !== initial.active) out.active = current.active
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-position', {
      label: 'Edit a position',
      pathParams: { id: position?.positionDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onSaved()
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
      title={position?.title ? `Edit ${position.title}` : 'Edit this position'}
      description="Only what you change is sent — absent means leave it alone, so an untouched field never appears in the body."
      endpoint={<EndpointTag id="update-position" name="Save" look="primary"
        pathParams={{ id: position?.positionDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
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

        {/* Rides on a SUCCESSFUL response. Kept apart from the refusal above so it never reads as
            a failure — the position was saved. */}
        {made?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">saved, with a warning</span>
            </div>
            <pre className="resp-body">{made.warning}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Title"
            hint="Unique within this department, retired positions included, case-folded. Empty is refused — POSITION_TITLE_REQUIRED."
            error={errors.title}
          >
            <Input value={current.title} error={errors.title} onChange={set('title')} />
          </Field>
          <Field
            label="Approved headcount"
            hint="At least 1. Null is NOT uncapped — the model forbids it, so 0 and below are a 400 rather than being clamped."
            error={errors.approvedHeadcount}
          >
            <Input type="number" value={current.approvedHeadcount}
              error={errors.approvedHeadcount} onChange={set('approvedHeadcount')} />
          </Field>
        </div>

        <Field
          label="Reports to (position id)"
          hint="Empty means nobody. May be in ANOTHER department — the org tree and the reporting line answer different questions. A supervisor that reports back to this position is a 409 POSITION_CYCLE."
          error={errors.reportsToPositionDocsId}
        >
          <Input value={current.reportsToPositionDocsId} error={errors.reportsToPositionDocsId}
            onChange={set('reportsToPositionDocsId')} placeholder="empty for nobody" />
        </Field>

        <div className="field-grid">
          <Field
            label="Teaching position"
            hint="Turning it OFF can leave the unit with no teaching position at all — that returns a warning on a 200, not a refusal."
          >
            <label className="check">
              <input type="checkbox" checked={current.teachingPosition}
                onChange={toggle('teachingPosition')} />
              <span>Somebody in this position teaches</span>
            </label>
          </Field>
          <Field
            label="Active"
            hint="A retired position keeps its title — records made against it still name it. Refusing to retire a filled position is #14's, once #16 makes filling one possible."
          >
            <label className="check">
              <input type="checkbox" checked={current.active} onChange={toggle('active')} />
              <span>This position is in use</span>
            </label>
          </Field>
        </div>

        {/* TEXT, NOT A BOX. A position cannot move department, and this is where somebody would try. */}
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Department, which #14 never accepts</td>
                <td><span className="mono">{position?.departmentDocsId}</span>{' '}
                  <span className="muted">
                    a position that moves department is a new position — editing it in place would rewrite
                    where every past holder worked, and every employment record under it would
                    change department with it
                  </span></td></tr>
              <tr><td className="muted">Position id</td>
                <td><span className="mono">{position?.positionDocsId}</span>{' '}
                  <span className="muted">
                    the whole identity, since positionCode was removed — it is what an employment
                    record stores
                  </span></td></tr>
            </tbody>
          </table>
        </div>

        <p className="muted">
          <Info size={12} /> Send <span className="mono">departmentDocsId</span> anyway if you
          like — it is not on the request record, so it is <b>ignored rather than refused</b>, and a
          body of only that is the same <span className="mono">400 NOTHING_TO_UPDATE</span> as an
          empty one.
        </p>
      </div>
    </Modal>
  )
}
