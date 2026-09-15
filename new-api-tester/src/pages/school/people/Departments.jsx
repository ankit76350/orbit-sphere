import { useEffect, useState } from 'react'
import { Info, Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The org chart a school hires into: /school-people/departments
 *
 * TWO ENDPOINTS — #9 creates a unit, #13 creates a seat inside one. There is no list yet (#12 is
 * GET /departments and #15 is GET /positions, neither built), so this shows what it has just
 * created rather than what the school holds. It says so, instead of rendering an empty table that
 * looks like a school with no org chart.
 *
 * A SEAT IS ADDED FROM ITS DEPARTMENT'S ROW, because #13 needs a departmentDocsId and the row is
 * where one is. Typing an id into a box would work too, and would be the only way to reach the
 * refusals — so the modal keeps the box, pre-filled from whichever row opened it.
 *
 * THIS IS WHERE THE PEOPLE MODULE STARTS, which surprises people. POST /staff looks like the
 * first call, but the write that employs somebody needs a positionDocsId, and a position needs a
 * department.
 *
 * NO YEAR ANYWHERE. An org chart outlives any academic year, so gate 4 does not run and this
 * endpoint still answers after every year has been ended — which the page says, because it is the
 * kind of thing somebody tests once and then wonders about.
 */

const BLANK = {
  departmentCode: '',
  name: '',
  description: '',
  parentDepartmentDocsId: '',
  headStaffDocsId: '',
}

export default function Departments() {
  const { actingSubdomain } = useApiState()
  const [open, setOpen] = useState(false)
  const [made, setMade] = useState([])
  // Which department's row opened the seat modal. Null means the toolbar button did, and the
  // box starts empty — every refusal stays reachable either way.
  const [seatFor, setSeatFor] = useState(null)
  const [seats, setSeats] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Departments" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Departments</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · the org chart positions hang off
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={Plus} onClick={() => setSeatFor({ departmentDocsId: '' })}>
          Add a position
        </Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a department</Button>
      </div>

      <Card
        title="Departments created here"
        description="#9 is the only organization endpoint built. There is no list yet — #12 is GET /departments — so this shows what this page created, not what the school holds."
        action={
          <div className="btn-row">
            <EndpointTag id="create-department" name="Add a department" />
            <Badge>{made.length} this session</Badge>
          </div>
        }
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing created here yet"
            description="This is not 'the school has no departments' — nothing can answer that until #12 is built. Add one to see what #9 returns."
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add the first</Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Parent</th>
                  <th>Head</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {made.map((one) => (
                  <tr key={one.departmentDocsId}>
                    {/* Given, never derived — and what positions and exports are written against. */}
                    <td><span className="mono">{one.departmentCode}</span></td>
                    <td>
                      {one.name}
                      {one.description
                        ? <div className="muted">{one.description}</div>
                        : null}
                    </td>
                    {/* Raw ids. #12 is where resolving them to names is decided, once. */}
                    <td>{one.parentDepartmentDocsId
                      ? <span className="mono">{one.parentDepartmentDocsId}</span>
                      : <span className="muted">top level</span>}</td>
                    <td>{one.headStaffDocsId
                      ? <span className="mono">{one.headStaffDocsId}</span>
                      : <span className="muted">none</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    <td>
                      <Button icon={Plus} onClick={() => setSeatFor(one)}>Add a seat</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> <b>This is the first call of the people module.</b>{' '}
          <span className="mono">POST /staff</span> looks like it should be, but employing somebody
          needs a position, and a position needs one of these.
        </p>
        <p className="muted">
          <Info size={12} /> <b>No academic year is involved</b>, so gate 4 does not run — this
          still answers after every year has been ended. An org chart outlives them.
        </p>
      </Card>

      <Card
        title="Positions created here"
        description="From #13 — an approved seat inside a department. A position has no code: its title is unique within its unit, and it is addressed by its document id."
        action={
          <div className="btn-row">
            <EndpointTag id="create-position" name="Add a seat" />
            <Badge>{seats.length} this session</Badge>
          </div>
        }
      >
        {seats.length === 0 ? (
          <Empty
            title="No seats created here yet"
            description="A seat needs an ACTIVE department. Add one above, then use its row."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Department</th>
                  <th>Reports to</th>
                  <th>Approved</th>
                  <th>Teaching</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {seats.map((one) => (
                  <tr key={one.positionDocsId}>
                    {/* The title IS the identity now — positionCode was removed 2026-09-15. */}
                    <td>{one.title}</td>
                    <td><span className="mono">{one.departmentDocsId}</span></td>
                    <td>{one.reportsToPositionDocsId
                      ? <span className="mono">{one.reportsToPositionDocsId}</span>
                      : <span className="muted">nobody</span>}</td>
                    <td>{one.approvedHeadcount}</td>
                    <td>
                      {one.teachingPosition
                        ? <Badge tone="brand">teaching</Badge>
                        : <span className="muted">no</span>}
                    </td>
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
          <Info size={12} /> <b>The filled count is not here and never will be on a write.</b> It
          comes from current employment records, so a number returned now would be stale the moment
          somebody is hired. #15 computes it.
        </p>
      </Card>

      <AddDepartment
        open={open}
        onClose={() => setOpen(false)}
        onAdded={(unit) => setMade((old) => [...old, unit])}
      />

      <AddPosition
        open={seatFor != null}
        department={seatFor}
        onClose={() => setSeatFor(null)}
        onAdded={(seat) => setSeats((old) => [...old, seat])}
      />
    </div>
  )
}

/**
 * Adding a department — #9.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a school enters its whole org chart in one sitting.
 * The code and name are cleared; the parent is kept, because the next unit is usually a sibling.
 *
 * EVERY BOX IS FREE TEXT AND NOTHING IS DISABLED, so every refusal stays reachable — a taken code,
 * an unknown parent, another school's staff id as the head.
 */
function AddDepartment({ open, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = { departmentCode: form.departmentCode, name: form.name }
    if (form.description.trim() !== '') out.description = form.description.trim()
    if (form.parentDepartmentDocsId.trim() !== '') {
      out.parentDepartmentDocsId = form.parentDepartmentDocsId.trim()
    }
    if (form.headStaffDocsId.trim() !== '') out.headStaffDocsId = form.headStaffDocsId.trim()
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-department', { label: 'Add a department', body })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({ ...old, departmentCode: '', name: '', description: '' }))
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
      title="Add a department"
      description="The code is given, never derived from the name — they move independently, and twenty positions may reference the code."
      endpoint={<EndpointTag id="create-department" name="Add" look="primary" />}
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
              <span className="resp-status" data-ok="true">{made.departmentCode}</span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Department code"
            required
            hint="Given, never derived. Stored trimmed and UPPER-CASED, so 'admin' and 'ADMIN' are one code."
            error={errors.departmentCode}
          >
            <Input value={form.departmentCode} error={errors.departmentCode}
              onChange={set('departmentCode')} placeholder="ACADEMICS" />
          </Field>
          <Field
            label="Name"
            required
            hint="What a person reads. Two units may share a name — only the code is unique."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Academic Department" />
          </Field>
        </div>

        <Field label="Description" hint="Optional free text." error={errors.description}>
          <Input value={form.description} error={errors.description}
            onChange={set('description')} placeholder="Curriculum and teaching operations." />
        </Field>

        <div className="field-grid">
          <Field
            label="Parent department id"
            hint="Optional. A department of THIS school — another school's real id is a 404. Blank means top level."
            error={errors.parentDepartmentDocsId}
          >
            <Input value={form.parentDepartmentDocsId} error={errors.parentDepartmentDocsId}
              onChange={set('parentDepartmentDocsId')} placeholder="leave blank for top level" />
          </Field>
          <Field
            label="Head staff id"
            hint="Optional Staff.id. Checked to EXIST, not to be employed — a school enters its org chart before its employment records."
            error={errors.headStaffDocsId}
          >
            <Input value={form.headStaffDocsId} error={errors.headStaffDocsId}
              onChange={set('headStaffDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> <span className="mono">active</span> is not accepted — a unit starts
          active and retiring is #11. Sending it is ignored rather than refused, which is the
          ordinary shape for a field the request record does not declare.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Adding a position — #13.
 *
 * THE DEPARTMENT BOX STAYS EVEN WHEN A ROW OPENED THIS. Pre-filled, not fixed: an id that is
 * retired, another school's, or nonsense is how DEPARTMENT_NOT_ACTIVE and DEPARTMENT_NOT_FOUND
 * are reached, and a modal that hid the field would make two documented refusals untestable.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a department gets its seats in one sitting. Only the
 * title is cleared — the department, the headcount and the teaching flag usually repeat.
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
      title={department?.name ? `Add a seat in ${department.name}` : 'Add a position'}
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
            hint="Must be ACTIVE, not merely present. Pre-filled from the row you opened this from — change it to reach DEPARTMENT_NOT_ACTIVE or NOT_FOUND."
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
