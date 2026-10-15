import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, CircleStop, Info, RefreshCw } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'
import CloseRecord from './CloseRecord.jsx'

/**
 * One academic record: /school-student/students/{id}/academic-records/{recordId}?year=
 *
 * TWO ENDPOINTS — #19 reads the record and #16 closes it.
 *
 * THE YEAR IS IN THE QUERY, because #19's path needs it and a record id alone does not carry it
 * in the address. The child's page puts it there when a row is opened. It is also a box on this
 * page, so a wrong year can be typed on purpose — that is 400 RECORD_NOT_IN_YEAR.
 *
 * NOTHING IS DISABLED. Close is offered on a closed record too, which is 409 RECORD_NOT_OPEN.
 */
export default function AcademicRecordDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id, recordId } = useParams()
  const [search, setSearch] = useSearchParams()
  const year = search.get('year') ?? actingAcademicYear ?? ''

  const [record, setRecord] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [closing, setClosing] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const answer = await call('get-student-record', {
      label: 'One academic record',
      pathParams: { year, academicRecordDocsId: recordId ?? '' },
    })
    setLoading(false)
    if (answer.ok) { setRecord(answer.bodyJson); setProblem(null) } else { setRecord(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, recordId, year])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="An academic record" />

  const back = detailPath('school', 'student', 'students', id ?? '')

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">
            {record ? `${record.className ?? 'Class deleted'} ${record.sectionNo}` : 'Academic record'}
          </h1>
          <p className="muted">
            {record?.studentName ?? 'reading the record…'}
            {record ? <> · {record.academicYear}</> : null}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(back)}>The child</Button>
        <EndpointTag id="get-student-record" name="Read"
          pathParams={{ year, academicRecordDocsId: recordId ?? '' }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        {/* NEVER GATED ON THE STATUS. Closing a closed record is a documented 409. */}
        <Button look="primary" icon={CircleStop} onClick={() => setClosing(true)}>Close it</Button>
      </div>

      <Card title="Which year" description="#19's path needs the year as well as the record id.">
        <Field label="Academic year" required
          hint="THE NAME, not an id. Filled in from the row that was opened. A year the record is not in is 400 RECORD_NOT_IN_YEAR.">
          <Input value={year} placeholder="2026-2027"
            onChange={(e) => setSearch({ year: e.target.value }, { replace: true })} />
        </Field>
      </Card>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
          <p className="muted">
            <Info size={12} /> <b>Another school&rsquo;s record is this same 404</b> — the read is
            scoped by school in the query.
          </p>
        </Card>
      ) : record ? (
        <Card
          title="The record"
          description="#19 reads it, whatever its status. Close it with #16; correct a typo on the current one with Edit on the child's page (#15)."
          action={
            <Button look="primary" icon={CircleStop} onClick={() => setClosing(true)}>
              Close it
            </Button>
          }
        >
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Child</td>
                  <td>{record.studentName ?? <span className="muted">no longer in this school</span>}
                    {record.admissionNo ? <> · <span className="mono">{record.admissionNo}</span></> : null}
                  </td></tr>
                <tr><td className="muted">Academic year</td><td>{record.academicYear}</td></tr>
                <tr><td className="muted">Class and section</td>
                  <td><b>{record.className ?? <span className="muted">class deleted</span>}</b>{' '}
                    {record.sectionNo}</td></tr>
                <tr><td className="muted">Roll no</td>
                  <td>{record.rollNo
                    ? <span className="mono">{record.rollNo}</span>
                    : <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">From</td><td>{record.effectiveFrom}</td></tr>
                <tr><td className="muted">Until</td>
                  <td>{record.effectiveUntil ?? <span className="muted">open</span>}</td></tr>
                <tr><td className="muted">Status</td>
                  <td>
                    <Badge tone={record.status === 'ACTIVE' ? 'good' : undefined}>{record.status}</Badge>
                    {/* `current` IS THE CHILD'S POINTER, not this record's status. Showing both is
                        how a disagreement between them becomes visible. */}
                    {record.current ? <> <Badge tone="good">current</Badge></> : null}
                  </td></tr>
                <tr><td className="muted">Came from</td>
                  <td>{record.previousAcademicRecordDocsId
                    ? <span className="mono muted">{record.previousAcademicRecordDocsId}</span>
                    : <span className="muted">nothing — a first placement</span>}</td></tr>
                <tr><td className="muted">Record id</td>
                  <td><span className="mono muted">{record.academicRecordDocsId}</span></td></tr>
                <tr><td className="muted">Version</td><td>{record.version}</td></tr>
              </tbody>
            </table>
          </div>
          <p className="muted">
            <Info size={12} /> <b>Close ends the placement</b> as <span className="mono">COMPLETED</span>,{' '}
            <span className="mono">TRANSFERRED</span> or <span className="mono">CANCELLED</span>. The
            server sets the end date to today, the record stays in the history, and if it was the
            child&rsquo;s current record that is cleared. <b>A move inside this school is
            Transfer</b>, on the child&rsquo;s page.
          </p>
        </Card>
      ) : null}

      {/* MOUNTED ONLY WHILE OPEN, so the version is seeded from the latest read. With no record
          read, the id and year from the address still go — the refusal is the server's. */}
      {closing ? (
        <CloseRecord
          child={{ fullName: record?.studentName ?? 'this child' }}
          row={record ?? { academicRecordDocsId: recordId, academicYear: year, version: 0, status: 'NOT READ' }}
          onClose={() => setClosing(false)}
          onClosed={load}
        />
      ) : null}
    </div>
  )
}
