import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Info, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'

/**
 * Reading a year back: the "View timetable" half of /school-academics/timetable
 *
 * TWO ENDPOINTS — #10, which lists the dates a year already has, and #7 behind every row. This is
 * deliberately NOT the builder: "what has this year got already" and "what am I about to write"
 * are two questions, and a tester answering the second should not have to scroll past the first.
 *
 * COUNTS, NEVER PERIODS. A full day is about 120 KB, so a page of twenty carrying its periods
 * would be megabytes to render twenty dates. The counts are computed in the database and the
 * periods never leave it — and #7 is one click away for the day somebody actually wants.
 *
 * NO PAGE HEADER HERE. The two-button switch and the title live in Timetable.jsx, which renders
 * this as one half of that page; this file is the cards only.
 *
 * NOTHING HERE IS DISABLED — the paging buttons walk off both ends on purpose, because an empty
 * page rather than a 404 is exactly what this endpoint answers and is worth seeing.
 */
export default function TimetableView() {
  const { call } = useApi()
  const navigate = useNavigate()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const [days, setDays] = useState(null)
  const [filters, setFilters] = useState({ from: '', to: '', teacherDocsId: '' })
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  //! THE TEACHER FILTER NEEDS NAMES, and that is the only reason this screen reads staff. The
  //! builder loads the whole class/section/subject structure; a list of dates does not need it.
  const [teachers, setTeachers] = useState([])

  useEffect(() => {
    if (!actingSubdomain) return
    let alive = true
    call('list-staff', { label: 'Who can be filtered on', query: { size: '100' } })
      .then((answer) => { if (alive) setTeachers(answer.bodyJson?.content ?? []) })
    return () => { alive = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain])

  const load = useCallback(async () => {
    if (!actingSubdomain || !actingAcademicYear) return
    setLoading(true)
    const query = { page: String(page), size: '20' }
    if (filters.from) query.from = filters.from
    if (filters.to) query.to = filters.to
    if (filters.teacherDocsId) query.teacherDocsId = filters.teacherDocsId
    const answer = await call('list-timetables', {
      label: "The year's days",
      pathParams: { year: actingAcademicYear },
      query,
    })
    setLoading(false)
    setDays(answer.ok ? answer.bodyJson : null)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, actingAcademicYear, filters, page])

  useEffect(() => { load() }, [load])

  return (
    <>
      {/* #10 — WHAT THE YEAR ALREADY HAS. Counts, not periods: a full day is about 120 KB and a
          page of twenty carrying its periods would be megabytes to render twenty dates. */}
      <Card
        title="Days already written"
        description="From #10 — one row per date, as counts. The periods stay in the database. Click a row to open that day in full, which is #7."
        action={
          <div className="btn-row">
            <EndpointTag id="list-timetables" name="List"
              pathParams={{ year: actingAcademicYear }} />
            <Badge>{days?.totalElements ?? 0} days</Badge>
            <Button icon={RefreshCw} onClick={load} busy={loading}>Reload</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="From" hint="Optional. Absent starts at the beginning of the year.">
            <Input type="date" value={filters.from}
              onChange={(e) => { setPage(0); setFilters((f) => ({ ...f, from: e.target.value })) }} />
          </Field>
          <Field label="To" hint="Optional and independent of 'from' — either alone is meaningful.">
            <Input type="date" value={filters.to}
              onChange={(e) => { setPage(0); setFilters((f) => ({ ...f, to: e.target.value })) }} />
          </Field>
          <Field label="Teacher" hint="One person's working days. A break they supervise counts.">
            <Select label="Teacher" value={filters.teacherDocsId}
              onChange={(v) => { setPage(0); setFilters((f) => ({ ...f, teacherDocsId: v })) }}
              options={[{ value: '', label: '— anybody —' },
                ...teachers.map((t) => ({ value: t.staffDocsId, label: t.fullName }))]} />
          </Field>
        </div>

        {(days?.content ?? []).length === 0 ? (
          <Empty
            title="No day matches"
            description="An empty page, never a 404 — this year simply has no timetable written for those dates yet."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Periods</th>
                  <th>Lessons</th>
                  <th>Classes</th>
                  <th>Sections</th>
                  <th>Staff</th>
                  <th>Timetable id</th>
                </tr>
              </thead>
              <tbody>
                {days.content.map((row) => (
                  //! A ROW OPENS ITS DAY — #7, addressed by the DATE rather than by the id in the
                  //! last column. That is what the endpoint takes, and the date is the column a
                  //! reader was already looking at.
                  <tr
                    key={row.dailyTimetableDocsId}
                    data-opens
                    onClick={() => navigate(
                      detailPath('school', 'academics', 'timetable', row.date))}
                  >
                    <td>{row.date}</td>
                    <td><b>{row.entryCount}</b></td>
                    {/* LESSONS vs PERIODS: the difference is the breaks, assemblies and
                        activities, which is worth seeing at a glance. */}
                    <td>{row.lessonCount}
                      {row.entryCount > row.lessonCount
                        ? <span className="muted"> · {row.entryCount - row.lessonCount} other</span>
                        : null}</td>
                    <td>{row.classCount}</td>
                    {/* PAIRED WITH THE CLASS — "A" of one class and "A" of another are two. */}
                    <td>{row.sectionCount}</td>
                    <td>{row.teacherCount}</td>
                    <td><span className="muted mono">{row.dailyTimetableDocsId}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
          <span className="muted">page {page}</span>
          <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
        </div>

        <p className="muted">
          <Info size={12} /> <b>A row carries counts, never periods.</b> A full day is about 120 KB;
          a page of twenty carrying its periods would be megabytes to render twenty dates. The
          counts are computed in the database and the periods never leave it.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Sections are counted paired with their class</b> — &ldquo;A&rdquo;
          of one class and &ldquo;A&rdquo; of another are two, so a twelve-class school does not
          report three sections.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Click a row to open that day in full.</b> That is #7, and it is
          addressed by the <b>date</b> rather than by the id in the last column — a caller always
          knows the date and never knows the id.
        </p>
      </Card>
    </>
  )
}
