import { useCallback, useEffect, useState } from 'react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'

/**
 * Attendance tab "Absentees": /school-academics/attendance/absentees
 *
 * ONE ENDPOINT — #11: every child marked absent in a DAILY register on a date, and whether their
 * guardian has been told. "Not told yet" is the follow-up list.
 */

/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const WHO = [
  { value: '', label: 'Everyone absent' },
  { value: 'false', label: 'Guardian not told yet' },
  { value: 'true', label: 'Guardian told' },
]

export default function AttendanceAbsentees({ reloadKey }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [date, setDate] = useState(today())
  const [notified, setNotified] = useState('')
  const [result, setResult] = useState(null)

  const query = { date, ...(notified ? { guardianNotified: notified } : {}) }

  const load = useCallback(async () => {
    if (!actingSubdomain || !year || !date) return
    const answer = await call('attendance-absentees', {
      label: `Absentees on ${date}`,
      pathParams: { year },
      query,
    })
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, date, notified, reloadKey])

  useEffect(() => { load() }, [load])

  const data = result?.ok ? result.bodyJson : null

  return (
    <Card
      title={data ? `${data.absentCount} absent · ${data.notNotifiedCount} guardian${data.notNotifiedCount === 1 ? '' : 's'} not told` : 'Absentees'}
      description="Every child marked absent in a DAILY register on the date, for follow-up."
      action={<EndpointTag id="attendance-absentees" name="Absentees" pathParams={{ year }} query={query} />}
    >
      <div className="field-grid">
        <Field label="Date" hint="Defaults to today. A holiday or weekly off answers 404 NOT_A_WORKING_DAY.">
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
        <Field label="Show" hint="'Guardian not told yet' is the follow-up list.">
          <Select value={notified} options={WHO} onChange={setNotified} />
        </Field>
      </div>

      {result && !result.ok ? (
        <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
      ) : data && data.absentees.length === 0 ? (
        <Empty title="Nobody absent" description="No child was marked absent in a daily register on this date." />
      ) : data ? (
        <>
          <p className="muted">
            <Badge>AUTHORISED</Badge> {data.authorisedCount} · <Badge tone="bad">UNAUTHORISED</Badge> {data.unauthorisedCount}
          </p>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Section</th><th>Child</th><th>Absence</th><th>Reason</th><th>Guardian</th><th>Register</th></tr>
              </thead>
              <tbody>
                {data.absentees.map((r) => (
                  <tr key={`${r.attendanceSessionDocsId}-${r.studentDocsId}`}>
                    <td><b>{r.className ?? 'class deleted'}</b> {r.sectionNo}</td>
                    <td>
                      {r.fullName ?? <span className="mono muted">{r.studentDocsId}</span>}
                      {r.admissionNo ? <div className="mono muted">{r.admissionNo}</div> : null}
                    </td>
                    <td><Badge tone={r.status === 'UNAUTHORISED_ABSENCE' ? 'bad' : undefined}>{r.status}</Badge></td>
                    <td>{r.reason ?? <span className="muted">—</span>}</td>
                    <td>{r.guardianNotified
                      ? <Badge tone="good">told{r.guardianAcknowledgedAt ? ', acknowledged' : ''}</Badge>
                      : <Badge tone="warn">not told</Badge>}</td>
                    <td>{r.registerStatus}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </Card>
  )
}
