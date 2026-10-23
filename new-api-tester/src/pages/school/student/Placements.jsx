import { useCallback, useEffect, useState } from 'react'
import { ExternalLink, Info, RefreshCw, Search, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'

/**
 * Every placement in a year: /school-student/placements
 *
 * ONE ENDPOINT — #18 lists the academic records of the year selected in the navbar, filtered by
 * class, section, status or child. A row opens the record's own page (#19), where it is closed.
 *
 * THE CLASS DROPDOWN HAS ITS EDITABLE BOX BESIDE IT, and the box is what is sent — a filter is
 * not a resource, so an unknown class id is an empty page rather than a refusal, and that is
 * worth being able to see.
 */

const BLANK_FILTERS = { classDocsId: '', sectionNo: '', status: '', studentDocsId: '' }
const STATUSES = ['', 'ACTIVE', 'COMPLETED', 'TRANSFERRED', 'CANCELLED']
const SORTS = ['', 'sectionNo', 'rollNo', 'effectiveFrom,desc', 'status']

export default function Placements() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState('')
  //! TWO COPIES OF THE FILTERS, the same as The Roll: the boxes edit `draft`, Search applies it.
  const [draft, setDraft] = useState(BLANK_FILTERS)
  const [applied, setApplied] = useState(BLANK_FILTERS)
  const [classes, setClasses] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-student-records', {
      label: `Placements in ${year}`,
      pathParams: { year },
      query: {
        page: String(page), size: '20',
        ...(sort ? { sort } : {}),
        //! ONLY FILTERS WITH A VALUE are sent — empty means "do not filter".
        ...Object.fromEntries(Object.entries(applied).filter(([, v]) => v !== '')),
      },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setData(null); setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, page, sort, applied])

  useEffect(() => { load() }, [load])

  //! THE CLASSES OF THE YEAR, for the dropdown. Read again when the navbar year changes.
  useEffect(() => {
    if (!actingSubdomain || !year) return
    let live = true
    call('list-school-classes', {
      label: `Classes in ${year}`,
      pathParams: { year },
      query: { page: '0', size: '100' },
    }).then((answer) => {
      if (live) setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : [])
    })
    return () => { live = false }
  }, [call, environment.id, actingSubdomain, year])

  const apply = () => { setPage(0); setApplied(draft) }
  const clear = () => { setPage(0); setDraft(BLANK_FILTERS); setApplied(BLANK_FILTERS) }

  if (!actingSubdomain) return <NoSchoolChosen what="Placements" />

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Placements</h1>
          <p className="muted">
            Every academic record in <b>{year || 'no year selected in the navbar'}</b> — who is in
            which class and section, closed records included.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-student-records" name="List" pathParams={{ year }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      <Card title="Filters" description="All optional. Search applies them and goes back to page one.">
        <div className="field-grid">
          <Field label="Class" hint="The box is what is sent. An unknown id is an empty page, not a refusal.">
            <div className="picker-pair">
              <Select value={(classes ?? []).some((c) => c.schoolClassId === draft.classDocsId) ? draft.classDocsId : ''}
                options={['', ...(classes ?? []).map((c) => ({ value: c.schoolClassId, label: c.name }))]}
                onChange={(v) => setDraft({ ...draft, classDocsId: v })} />
              <Input value={draft.classDocsId} placeholder="any class"
                onChange={(e) => setDraft({ ...draft, classDocsId: e.target.value })} />
            </div>
          </Field>
          <Field label="Section" hint="Usually with a class — B in one class is not B in another.">
            <Input value={draft.sectionNo} placeholder="any section"
              onChange={(e) => setDraft({ ...draft, sectionNo: e.target.value })} />
          </Field>
          <Field label="Status" hint="ACTIVE is who is placed right now. Empty is every record.">
            <Select value={draft.status} options={STATUSES}
              onChange={(v) => setDraft({ ...draft, status: v })} />
          </Field>
          <Field label="Student id" hint="One child's records in this year.">
            <Input value={draft.studentDocsId} placeholder="any child"
              onChange={(e) => setDraft({ ...draft, studentDocsId: e.target.value })} />
          </Field>
          <Field label="Sort" hint="Empty is class, then section, then roll number.">
            <Select value={sort} options={SORTS} onChange={(v) => { setPage(0); setSort(v) }} />
          </Field>
        </div>
        <div className="toolbar">
          <Button look="primary" icon={Search} onClick={apply}>Search</Button>
          <Button icon={X} onClick={clear}>Clear</Button>
          <span className="toolbar-spacer" />
          <EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} />
        </div>
      </Card>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      <Card title={data ? `${data.totalElements} record${data.totalElements === 1 ? '' : 's'}` : 'Placements'}>
        {rows.length === 0 ? (
          <Empty title="No records"
            description="An empty page, never a 404 — the filters narrowed it to nothing, or nobody is placed in this year yet." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Child</th>
                  <th>Class and section</th>
                  <th>Roll no</th>
                  <th>From</th>
                  <th>Until</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.academicRecordDocsId}>
                    <td>
                      {one.studentName ?? <span className="muted">no longer in this school</span>}
                      {one.admissionNo ? <div className="muted mono">{one.admissionNo}</div> : null}
                    </td>
                    <td><b>{one.className ?? <span className="muted">class deleted</span>}</b> {one.sectionNo}</td>
                    <td>{one.rollNo ? <span className="mono">{one.rollNo}</span> : <span className="muted">none</span>}</td>
                    <td>{one.effectiveFrom}</td>
                    <td>{one.effectiveUntil ?? <span className="muted">open</span>}</td>
                    <td>
                      <Badge tone={one.status === 'ACTIVE' ? 'good' : undefined}>{one.status}</Badge>
                      {one.current ? <> <Badge tone="good">current</Badge></> : null}
                    </td>
                    <td>
                      <Button icon={ExternalLink} onClick={() => navigate(childPath('school',
                        'student', 'students', one.studentDocsId, 'academic-records',
                        one.academicRecordDocsId))}>
                        Open
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          <span className="toolbar-spacer" />
          <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
        </div>

        <p className="muted">
          <Info size={12} /> <b>The names cost two reads per page</b> — one for the children, one
          for the classes — never one per row.
        </p>
      </Card>
    </div>
  )
}
