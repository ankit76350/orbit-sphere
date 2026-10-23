import { useCallback, useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'

/**
 * Attendance #9 on a child's page: every mark between two dates in the year selected in the
 * navbar, and the totals. Empty dates mean the whole year.
 *
 * THE PERCENTAGE IS DAILY REGISTERS ONLY, and LATE counts as attended — the card says so beside it.
 */

const TONE = { PRESENT: 'good', LATE: 'warn', AUTHORISED_ABSENCE: undefined, UNAUTHORISED_ABSENCE: 'bad' }

export default function StudentAttendanceCard({ studentDocsId }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  //! ONLY DATES WITH A VALUE are sent — empty means the year's own start or end.
  const query = { ...(from ? { from } : {}), ...(to ? { to } : {}) }

  const load = useCallback(async () => {
    if (!actingSubdomain || !year || !studentDocsId) return
    setLoading(true)
    const answer = await call('student-attendance', {
      label: "One child's attendance",
      pathParams: { year, studentDocsId },
      query,
    })
    setLoading(false)
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, studentDocsId, from, to])

  useEffect(() => { load() }, [load])

  const data = result?.ok ? result.bodyJson : null

  return (
    <Card
      title={data
        ? `${data.studentName ?? 'Attendance'}${data.admissionNo ? ` (${data.admissionNo})` : ''} — ${data.attendancePercentage == null ? 'no days marked' : `${data.attendancePercentage}% attended`}`
        : 'Attendance'}
      description={`In ${year || 'no year selected in the navbar'}. The percentage counts DAILY registers only, and LATE counts as attended.`}
      action={
        <div className="btn-row">
          <EndpointTag id="student-attendance" name="Attendance" pathParams={{ year, studentDocsId }} query={query} />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        </div>
      }
    >
      <div className="field-grid">
        <Field label="From" hint="Empty: the start of the year.">
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </Field>
        <Field label="To" hint="Empty: the end of the year. Before From is 400 INVALID_DATE_RANGE.">
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </Field>
      </div>

      {result && !result.ok ? (
        <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
      ) : null}

      {data ? (
        <>
          <p className="muted">
            {data.from} to {data.to} · <b>{data.attendedDays}</b> of <b>{data.markedDays}</b> marked
            days attended
            {Object.entries(data.dailyCounts).map(([status, count]) => (
              <span key={status}> · <Badge tone={TONE[status]}>{status}</Badge> {count}</span>
            ))}
            {data.periodMarkCount ? <> · {data.periodMarkCount} period mark(s), not in the percentage</> : null}
          </p>
          {data.records.length === 0 ? (
            <Empty title="No marks in this range" description="No register has marked this child between these dates." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Date</th><th>Register</th><th>Mark</th><th>Reason</th><th>Register status</th></tr></thead>
                <tbody>
                  {data.records.map((r) => (
                    <tr key={`${r.attendanceSessionDocsId}`}>
                      <td>{r.attendanceDate}</td>
                      <td>{r.sessionType}{r.periodCode ? ` · ${r.periodCode}` : ''} <span className="muted">{r.sectionNo}</span></td>
                      <td><Badge tone={TONE[r.status]}>{r.status}</Badge></td>
                      <td>{r.reason ?? <span className="muted">—</span>}</td>
                      <td>{r.registerStatus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : null}
    </Card>
  )
}
