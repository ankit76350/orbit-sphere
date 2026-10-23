import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronRight, Info, RefreshCw, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import { childPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * Every seat in the school, with the count of who holds it: /school-people/positions
 *
 * THIS IS WHERE #15 IS ACTUALLY RUN. The department page calls it too, but with a fixed query —
 * one unit, size 100 — purely to fill a column. None of its filters were reachable from anywhere,
 * which in an API tester means they were built and could not be tested.
 *
 * #15 AND #52 ANSWER DIFFERENT QUESTIONS, and that is why this screen exists alongside the
 * department page. #52 answers "what is this unit made of" and returns its seats as part of the
 * unit. #15 answers "find seats across the school" — every unit at once, filtered, paged, and
 * carrying the one number #52 deliberately does not: the filled headcount.
 *
 * THERE IS NO "ADD" BUTTON HERE, ON PURPOSE. A position cannot exist outside a department — #13
 * requires a departmentDocsId — so creating one belongs on the unit that will own it, and it
 * stays there. This screen reads.
 *
 * ?vacant=true IS THE QUERY THIS PAGE IS FOR. "Which seats can we still hire into" is what a
 * school asks at the start of a hiring round, and it is the one filter that cannot be answered
 * from the positions collection alone — see the note under the table.
 *
 * THE COUNTS DESCRIBE EVERY MATCH, NOT THIS PAGE. totalElements is the size of the filtered set,
 * which under ?vacant= is the size of the VACANT set rather than of the collection.
 *
 * A ROW OPENS THE SEAT AT ITS ONE ADDRESS, under the department that owns it. A seat does not get
 * a second address just because a second screen lists it.
 *
 * NOTHING HERE IS DISABLED. Every filter combination, including ones that return nothing, has to
 * be reachable.
 */

const TRISTATE = ['', 'true', 'false']
const SORTS = ['', 'title', 'title,desc', 'approvedHeadcount', 'approvedHeadcount,desc',
  'createdAt,desc', 'updatedAt,desc', 'filledHeadcount']
const SIZES = ['5', '20', '100']

export default function Positions() {
  const { call } = useApi()
  const navigate = useNavigate()
  const { environment, actingSubdomain } = useApiState()

  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [departmentDocsId, setDepartmentDocsId] = useState('')
  const [active, setActive] = useState('')
  const [teaching, setTeaching] = useState('')
  const [vacant, setVacant] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  //! BLANK SENDS NOTHING AT ALL, which is not the same as sending false. ?active= left off
  //! returns retired seats as well as active ones — "the whole chart" rather than "what is
  //! closed" — and the same is true of teaching and vacant.
  const query = useCallback(() => {
    const out = { page: String(page), size }
    if (search.trim() !== '') out.search = search.trim()
    if (departmentDocsId.trim() !== '') out.departmentDocsId = departmentDocsId.trim()
    if (active) out.active = active
    if (teaching) out.teaching = teaching
    if (vacant) out.vacant = vacant
    if (sort) out.sort = sort
    return out
  }, [search, departmentDocsId, active, teaching, vacant, sort, page, size])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-positions', { label: 'Seats and their filled counts', query: query() })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  const rows = data?.content ?? []

  if (!actingSubdomain) return <NoSchoolChosen what="Positions" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Positions</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · every approved seat in the school ·
            the filled count is computed from employment records, never stored
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      <Card
        title="Filters"
        description="All five are AND-ed, and blank sends nothing at all — which is not the same as sending false."
        action={<EndpointTag id="list-positions" name="List" query={query()} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Field label="Search" hint="Matches title, case-insensitive, anywhere. Quoted before it is compiled, so 'Teacher(' is an empty answer rather than a 500.">
              <Input value={typed} onChange={(e) => setTyped(e.target.value)}
                placeholder="Teacher" />
            </Field>
            <Button icon={Search} onClick={() => { setSearch(typed); setPage(0) }}>Search</Button>
            <Button onClick={() => { setSearch(''); setTyped(''); setPage(0) }}>Clear</Button>
          </div>

          <div className="field-grid">
            <Field label="Department id" hint="The seats of one unit. Blank returns every unit's. Copy it from a department page.">
              <Input value={departmentDocsId}
                onChange={(e) => { setDepartmentDocsId(e.target.value); setPage(0) }}
                placeholder="from Get Department" />
            </Field>
            <Field label="Vacant" hint="true is the query a hiring round runs: approved headcount not yet filled. false means NOT vacant — which includes an over-filled seat, not only a full one.">
              <Select label="Vacant" value={vacant}
                onChange={(value) => { setVacant(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Active" hint="Retired seats keep their title and still name the records made against them. Blank returns BOTH, not just active ones.">
              <Select label="Active" value={active}
                onChange={(value) => { setActive(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Teaching" hint="false is asked as 'not true', so a seat written before the field had a default still answers — is(false) would drop it from both answers.">
              <Select label="Teaching" value={teaching}
                onChange={(value) => { setTeaching(value); setPage(0) }} options={TRISTATE} />
            </Field>
          </div>

          <div className="toolbar">
            <Field label="Sort" hint="Tiebroken by department then title, which is unique per school. filledHeadcount is in this list ON PURPOSE and is a 400: it is computed after the page is chosen, so ordering by it would mean counting the whole collection.">
              <Select label="Sort" value={sort}
                onChange={(value) => { setSort(value); setPage(0) }} options={SORTS} />
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
        title="Seats"
        description="From #15 — ordered by department then title. The counts describe every match, not this page."
        action={
          <div className="btn-row">
            <EndpointTag id="get-position" name="Open one" />
            <Badge>{data?.totalElements ?? 0} matching</Badge>
            <Badge tone="brand">
              {rows.reduce((sum, one) => sum + (one.filledHeadcount ?? 0), 0)} filled here
            </Badge>
            <Badge tone="good">
              {rows.reduce((sum, one) => sum + (one.vacancies ?? 0), 0)} vacant here
            </Badge>
          </div>
        }
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'INVALID_SORT_FIELD'
                ? `${problem.bodyJson?.message} — filledHeadcount is computed after the page is `
                  + 'chosen, so sorting by it would mean counting the whole collection first.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : rows.length === 0 ? (
          <Empty
            title="No seat matches"
            description="An empty page, never a 404. Clear the filters to see whether the school has any positions at all — they are created inside a department, not here."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Approved</th>
                  <th>Filled</th>
                  <th>Vacancies</th>
                  <th>Teaching</th>
                  <th>Status</th>
                  <th>Department id</th>
                  <th>Position id</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr
                    key={one.positionDocsId}
                    data-opens
                    /* #53, at the seat's ONE address — under the department that owns it. A seat
                       does not get a second address just because a second screen lists it. */
                    onClick={() => navigate(childPath('school', 'people', 'departments',
                      one.departmentDocsId, 'positions', one.positionDocsId))}
                  >
                    {/* The title IS the identity — positionCode was removed 2026-09-15. */}
                    <td>{one.title}</td>
                    <td>{one.approvedHeadcount}</td>
                    {/* COMPUTED FROM CURRENT EMPLOYMENT RECORDS, never stored. */}
                    <td><b>{one.filledHeadcount}</b></td>
                    <td>
                      {/* #16 WARNS RATHER THAN REFUSING when a school over-hires, so filled can
                          exceed approved. vacancies is floored at 0 — a negative would read as a
                          seat owing people — and this badge is how the two are told apart. */}
                      {one.overFilled
                        ? <Badge>over by {one.filledHeadcount - (one.approvedHeadcount ?? 0)}</Badge>
                        : one.vacancies
                          ? <Badge tone="good">{one.vacancies}</Badge>
                          : <span className="muted">full</span>}
                    </td>
                    <td>{one.teachingPosition
                      ? <Badge tone="brand">teaching</Badge>
                      : <span className="muted">no</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    <td><span className="muted mono">{one.departmentDocsId}</span></td>
                    <td><span className="muted mono">{one.positionDocsId}</span></td>
                    <td><ChevronRight size={14} className="muted" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <p className="muted">
          <Info size={12} /> <b>The filled count is computed on every call, never stored.</b> A
          counter on the position document would drift the first time a writer forgot it — the same
          objection that keeps a weight total off a term. One grouped count covers the whole page
          rather than one query per row.
        </p>
        <p className="muted">
          <Info size={12} /> <b>?vacant= is the one filter that cannot be applied before paging.</b>{' '}
          Vacancy is not a field — it is filled &lt; approved, and the left side lives in another
          collection. Filtering after paging would return short pages, so that path reads every
          matching seat, counts it, filters, and pages in memory. Every other call counts only the
          rows it returns.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Seats are created inside a department, not here.</b> #13 requires a
          departmentDocsId, so the button lives on the unit that will own the seat. This screen
          reads; #14 edits from the department page too.
        </p>
      </Card>
    </div>
  )
}
