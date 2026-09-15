import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronRight, Info, Plus, RefreshCw, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'
import AddDepartment from './AddDepartment.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The org chart a school hires into: /school-people/departments
 *
 * TWO ENDPOINTS — #12 lists the units and #9 creates one. #52 opens one, and it does so on its OWN
 * PAGE rather than in a modal: a unit carries the one above it, the ones under it and every seat
 * in it, which is more than a modal's worth of screen — and a page has an address, so it can be
 * linked, reloaded and shared. The same call this project made for a class. The department table
 * is #12's answer, so it shows what the school HOLDS.
 *
 * #12 HAS TWO SHAPES AND THE TOGGLE PICKS ONE. Flat is a page envelope; ?tree=true is nested
 * roots. Paging a tree is refused by the API, so the page-size and page controls are still shown
 * in tree mode — sending them is how that refusal is reached, and hiding them would make a
 * documented 400 untestable.
 *
 * SEATS ARE NOT ADDED HERE, AND ARE NOT LISTED HERE. #13 needs a departmentDocsId, so it lives on
 * the unit's own page, where the seats it creates show up in #52's answer. Until 2026-09-15 this
 * page carried a session-only table of the positions it had created — which listed seats the
 * school held nowhere, and none of the ones it did.
 *
 * WHAT IS ADDED HERE IS A TOP-LEVEL UNIT, and the modal does not draw a parent box at all: there
 * is no id on this screen to nest under, so the only correct value would be empty. A sub-unit is
 * added from its parent's page, where the id is already known.
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


export default function Departments() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  // Whether to send page and size at all. Kept separate from tree so the refusal stays reachable:
  // leave it on in tree mode and the API answers TREE_CANNOT_BE_PAGED, which is the point.
  const [paging, setPaging] = useState(true)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

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
            <span className="mono">{actingSubdomain}</span> · the org chart positions hang off ·
            open a unit to add a seat or a sub-department
          </p>
        </div>
        <span className="toolbar-spacer" />
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
                <TreeNode key={root.departmentDocsId} node={root} depth={0}
                  onOpen={(id) => navigate(detailPath('school', 'people', 'departments', id))} />
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
                  <tr
                    key={one.departmentDocsId}
                    data-opens
                    onClick={() => navigate(detailPath('school', 'people', 'departments',
                      one.departmentDocsId))}
                  >
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
                    {/* Seats are added from the unit's own page — #13 needs a departmentDocsId,
                        and that page IS one department. */}
                    <td><span className="muted">Open <ChevronRight size={13} /></span></td>
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

      {/* No parent prop — from the list, a new unit is top-level. Nesting is done from the
          parent's own page, where its id is already on screen. */}
      <AddDepartment
        open={open}
        parent={null}
        onClose={() => setOpen(false)}
        onAdded={() => load()}
      />
    </div>
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
function TreeNode({ node, depth, onOpen }) {
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
        <Button onClick={() => onOpen(node.departmentDocsId)}>
          Open <ChevronRight size={13} />
        </Button>
      </div>
      {node.subDepartments.map((under) => (
        <TreeNode key={under.departmentDocsId} node={under} depth={depth + 1} onOpen={onOpen} />
      ))}
    </div>
  )
}
