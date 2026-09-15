import { useCallback, useEffect, useMemo, useState } from 'react'
import { Info, Plus, RefreshCw, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The org chart a school hires into: /school-people/departments
 *
 * THREE ENDPOINTS — #12 lists the units, #9 creates one, #13 creates a seat inside one. The
 * department table is #12's answer, so it shows what the school HOLDS. The position table is
 * still session-only: #15 is GET /positions and is not built, and the page says so rather than
 * rendering an empty table that looks like a school with no seats.
 *
 * #12 HAS TWO SHAPES AND THE TOGGLE PICKS ONE. Flat is a page envelope; ?tree=true is nested
 * roots. Paging a tree is refused by the API, so the page-size and page controls are still shown
 * in tree mode — sending them is how that refusal is reached, and hiding them would make a
 * documented 400 untestable.
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

const TRISTATE = ['', 'true', 'false']
const SORTS = ['', 'name', 'name,desc', 'departmentCode', 'departmentCode,desc',
  'createdAt,desc', 'updatedAt,desc']
const SIZES = ['5', '20', '100']

const BLANK = {
  departmentCode: '',
  name: '',
  description: '',
  parentDepartmentDocsId: '',
  headStaffDocsId: '',
}

export default function Departments() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  // Whether to send page and size at all. Kept separate from tree so the refusal stays reachable:
  // leave it on in tree mode and the API answers TREE_CANNOT_BE_PAGED, which is the point.
  const [paging, setPaging] = useState(true)
  const [open, setOpen] = useState(false)
  // Which department's row opened the seat modal. Null means the toolbar button did, and the
  // box starts empty — every refusal stays reachable either way.
  const [seatFor, setSeatFor] = useState(null)
  const [seats, setSeats] = useState([])

  const [tree, setTree] = useState('')
  const [active, setActive] = useState('')
  const [topLevelOnly, setTopLevelOnly] = useState('')
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  // Built at render, so the endpoint tag shows the URL that will actually be sent. An empty box
  // sends nothing rather than an empty parameter — `?active=` is not the same as no filter.
  const query = useMemo(() => {
    const out = {}
    if (tree) out.tree = tree
    if (active) out.active = active
    if (topLevelOnly) out.topLevelOnly = topLevelOnly
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    // Sent even in tree mode, on purpose: TREE_CANNOT_BE_PAGED is a documented refusal and
    // hiding these would make it unreachable from this screen.
    if (!(tree === 'true' && paging === false)) {
      out.page = page
      out.size = size
    }
    return out
  }, [tree, active, topLevelOnly, search, sort, page, size, paging])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-departments', { label: "The school's departments", query })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  const runSearch = () => { setPage(0); setSearch(typed) }
  const isTree = tree === 'true'
  const rows = data?.content ?? []

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
        title="Filters"
        description="All five are AND-ed, and blank sends nothing at all — which is not the same as sending false."
        action={<EndpointTag id="list-departments" name="List" query={query} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Field label="Search" wide
              hint="Matches name OR departmentCode, case-insensitive, anywhere. A stray '(' is an empty answer, not a 500.">
              <Input value={typed} onChange={(event) => setTyped(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') runSearch() }}
                placeholder="ACADEMICS, or Science" />
            </Field>
            <Button icon={Search} onClick={runSearch}>Search</Button>
            <Button onClick={() => { setSearch(''); setTyped(''); setPage(0) }}>Clear</Button>
            <span className="toolbar-spacer" />
            <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
          </div>

          <div className="field-grid">
            <Field label="Shape"
              hint="true nests the chart and changes the response shape. Blank or false is a flat page.">
              <Select label="Shape" value={tree}
                onChange={(value) => { setTree(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Active" hint="Blank returns BOTH — and a retired unit keeps its place in the tree.">
              <Select label="Active" value={active}
                onChange={(value) => { setActive(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Top level only" hint="Asked with exists, so a unit with no parent key reads as top-level.">
              <Select label="Top level only" value={topLevelOnly}
                onChange={(value) => { setTopLevelOnly(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Sort" hint="Tiebroken by departmentCode — two units may share a name.">
              <Select label="Sort" value={sort}
                onChange={(value) => { setSort(value); setPage(0) }} options={SORTS} />
            </Field>
          </div>

          <div className="toolbar">
            <Field label="Send page and size"
              hint="Leave on in tree mode to reach TREE_CANNOT_BE_PAGED — a tree has no page boundary, and the API refuses rather than ignoring.">
              <label className="check">
                <input type="checkbox" checked={paging}
                  onChange={(event) => setPaging(event.target.checked)} />
                <span>include paging parameters</span>
              </label>
            </Field>
            <Field label="Page size" hint="Defaults to 20, capped at 100. 0 and 101 are refused, never clamped.">
              <Select label="Page size" value={size}
                onChange={(value) => { setSize(value); setPage(0) }} options={SIZES} />
            </Field>
            <span className="toolbar-spacer" />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <span className="muted">page {page}</span>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      </Card>

      <Card
        title={isTree ? 'The org chart' : 'Departments'}
        description={isTree
          ? 'From #12 with ?tree=true — built from ONE flat read, not a query per level.'
          : 'From #12, ordered by name then departmentCode. The counts describe every match, not this page.'}
        action={
          <div className="btn-row">
            {isTree && data?.totalElements != null
              ? <Badge>{data.totalElements} at any depth</Badge>
              : <Badge>{data?.totalElements ?? 0} matching</Badge>}
            {isTree && data?.depth != null ? <Badge>depth {data.depth}</Badge> : null}
            {isTree && data?.liftedToTop ? <Badge tone="brand">{data.liftedToTop} lifted</Badge> : null}
            <Button icon={Plus} onClick={() => setOpen(true)}>Add</Button>
          </div>
        }
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'TREE_CANNOT_BE_PAGED'
                ? 'A tree has no page boundary — cutting one would separate children from their '
                  + 'parents. Turn off "include paging parameters", or switch the shape back to flat.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : isTree ? (
          (data?.roots ?? []).length === 0 ? (
            <Empty title="No units match"
              description="An empty tree, never a 404. Clear the filters to see whether the school has any." />
          ) : (
            <div className="stack">
              {(data?.roots ?? []).map((root) => (
                <TreeNode key={root.departmentDocsId} node={root} depth={0} onSeat={setSeatFor} />
              ))}
            </div>
          )
        ) : rows.length === 0 ? (
          <Empty
            title="No departments match"
            description="An empty page, never a 404. Clear the filters to see whether the school has any at all."
            action={<Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add one</Button>}
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
                {rows.map((one) => (
                  <tr key={one.departmentDocsId}>
                    {/* Given, never derived — and what positions and exports are written against. */}
                    <td><span className="mono">{one.departmentCode}</span></td>
                    <td>
                      {one.name}
                      {one.description ? <div className="muted">{one.description}</div> : null}
                    </td>
                    {/* Raw ids. Resolving them to names is #12's tree, decided once. */}
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
          <Info size={12} /> <b>No academic year is involved</b>, so gate 4 does not run — the
          writes still answer after every year has been ended. An org chart outlives them.
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
        onAdded={() => load()}
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

/**
 * One unit in the chart, and whatever hangs off it.
 *
 * INDENTED RATHER THAN NESTED IN TABLES, because a table per level makes a three-deep chart into
 * three scrollbars. Depth is expressed with padding, which is what a person reads it by.
 *
 * A LIFTED NODE IS MARKED HERE, not silently shown as a root. #12 lifts a unit whose parent the
 * filter excluded — it is a real unit the caller asked to see, and pretending it is top-level
 * would be a quieter lie than dropping it.
 */
function TreeNode({ node, depth, onSeat }) {
  return (
    <div className="stack" style={{ marginLeft: depth === 0 ? 0 : 20 }}>
      <div className="toolbar">
        <span className="mono">{node.departmentCode}</span>
        <span>{node.name}</span>
        <Badge tone={node.active ? 'good' : undefined}>
          {node.active ? 'active' : 'retired'}
        </Badge>
        {node.liftedToTop ? (
          <Badge tone="brand" title="Its parent was excluded by the filter, so it was lifted here rather than dropped">
            lifted to top
          </Badge>
        ) : null}
        {node.headStaffDocsId
          ? <span className="muted mono">head {node.headStaffDocsId}</span>
          : null}
        <span className="toolbar-spacer" />
        <Button icon={Plus} onClick={() => onSeat(node)}>Add a seat</Button>
      </div>
      {node.children.map((child) => (
        <TreeNode key={child.departmentDocsId} node={child} depth={depth + 1} onSeat={onSeat} />
      ))}
    </div>
  )
}
