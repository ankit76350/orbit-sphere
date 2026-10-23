import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ChevronRight, Search } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import StudentAttendanceCard from '../student/StudentAttendanceCard.jsx'

/**
 * Attendance tab "Students": /school-academics/attendance/students?student=
 *
 * THE SCHOOL'S CHILDREN, searchable by name or admission number. Clicking one shows their
 * attendance (#9) on this same tab; the chosen child is in the address, so a reload lands on them.
 */
export default function AttendanceStudents({ reloadKey }) {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const [search, setSearch] = useSearchParams()
  const chosen = search.get('student') ?? ''

  const [draft, setDraft] = useState('')
  const [applied, setApplied] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)

  //! THE ROLL, one page at a time, narrowed by name or admission number.
  const query = { page: String(page), size: '20', ...(applied ? { search: applied } : {}) }

  const load = useCallback(async () => {
    if (!actingSubdomain || chosen) return
    const answer = await call('list-students', { label: 'Children to pick from', query })
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, chosen, page, applied, reloadKey])

  useEffect(() => { load() }, [load])

  const pick = (id) => setSearch({ student: id })
  const back = () => setSearch({})

  //! ONE CHILD CHOSEN — their attendance, with the way back to the list.
  if (chosen) {
    return (
      <>
        <div className="toolbar">
          <Button icon={ArrowLeft} onClick={back}>All students</Button>
        </div>
        <StudentAttendanceCard studentDocsId={chosen} key={`${chosen}-${reloadKey}`} />
      </>
    )
  }

  const rows = data?.content ?? []

  return (
    <Card
      title={data ? `${data.totalElements} child${data.totalElements === 1 ? '' : 'ren'}` : 'Students'}
      description="Pick a child to see their attendance."
      action={<EndpointTag id="list-students" name="Students" query={query} />}
    >
      <div className="field-grid">
        <Field label="Search" hint="Name or admission number.">
          <Input value={draft} placeholder="Ayan, or ADM-2026-0001"
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { setPage(0); setApplied(draft) } }} />
        </Field>
      </div>
      <div className="toolbar">
        <Button icon={Search} onClick={() => { setPage(0); setApplied(draft) }}>Search</Button>
      </div>

      {problem ? (
        <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
      ) : rows.length === 0 && data ? (
        <Empty title="No children" description={applied ? 'Nobody matches that search.' : 'The roll is empty.'} />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead><tr><th>Child</th><th>Admission no</th><th>Status</th><th>Placed</th><th></th></tr></thead>
            <tbody>
              {rows.map((one) => (
                <tr key={one.studentDocsId} style={{ cursor: 'pointer' }} onClick={() => pick(one.studentDocsId)}>
                  <td><b>{one.fullName}</b></td>
                  <td className="mono">{one.admissionNo}</td>
                  <td><Badge>{one.status}</Badge></td>
                  <td>{one.placed ? <Badge tone="good">placed</Badge> : <span className="muted">not placed</span>}</td>
                  <td><Button icon={ChevronRight}>Attendance</Button></td>
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
    </Card>
  )
}
