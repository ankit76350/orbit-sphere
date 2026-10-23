import { useCallback, useEffect, useState } from 'react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'

/**
 * Attendance #10, on the Monthly register tab: the monthly register — one row per child who was in the
 * section in the range, with their counts and percentage.
 *
 * THE RANGE STARTS AS THE MONTH OF {@code date} (today, on the Monthly register tab), and both ends
 * can be changed.
 */

/** The first and last day of the month a YYYY-MM-DD date is in. */
const monthOf = (date) => {
  const [y, m] = date.split('-').map(Number)
  const last = new Date(y, m, 0).getDate()
  return [`${y}-${String(m).padStart(2, '0')}-01`, `${y}-${String(m).padStart(2, '0')}-${String(last).padStart(2, '0')}`]
}

export default function SectionMonthlyRegister({ classDocsId, sectionNo, date }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [from, setFrom] = useState(monthOf(date)[0])
  const [to, setTo] = useState(monthOf(date)[1])
  const [result, setResult] = useState(null)

  //! ONLY DATES WITH A VALUE are sent — empty means the year's own start or end.
  const query = { ...(from ? { from } : {}), ...(to ? { to } : {}) }

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    const answer = await call('section-attendance', {
      label: `Monthly register of ${sectionNo}`,
      pathParams: { year, classDocsId, sectionNo },
      query,
    })
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, classDocsId, sectionNo, from, to])

  useEffect(() => { load() }, [load])

  const data = result?.ok ? result.bodyJson : null

  return (
    <Card
      title={data ? `Monthly register — ${data.registerCount} register day${data.registerCount === 1 ? '' : 's'}` : 'Monthly register'}
      description="One row per child who was in this section in the range. DAILY registers only; LATE counts as attended. A child is counted only against the days they were in the section."
      action={<EndpointTag id="section-attendance" name="Monthly" pathParams={{ year, classDocsId, sectionNo }} query={query} />}
    >
      <div className="field-grid">
        <Field label="From" hint="Starts as the first of the month. Empty: the start of the year.">
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </Field>
        <Field label="To" hint="Starts as the last of the month. Before From is 400 INVALID_DATE_RANGE.">
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </Field>
      </div>

      {result && !result.ok ? (
        <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
      ) : data && data.students.length === 0 ? (
        <Empty title="Nobody in this section in the range" description="No child's placement overlapped these dates." />
      ) : data ? (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Roll</th><th>Child</th><th className="num">Days</th><th className="num">Marked</th>
                <th className="num">Attended</th><th className="num">Not marked</th><th className="num">%</th><th>Breakdown</th>
              </tr>
            </thead>
            <tbody>
              {data.students.map((r) => (
                <tr key={r.studentDocsId}>
                  <td className="mono">{r.rollNo ?? '—'}</td>
                  <td>
                    {r.fullName ?? <span className="mono muted">{r.studentDocsId}</span>}
                    {r.placedUntil ? <div className="muted">left {r.placedUntil}</div> : null}
                  </td>
                  <td className="num">{r.registerDays}</td>
                  <td className="num">{r.markedDays}</td>
                  <td className="num">{r.attendedDays}</td>
                  <td className="num">{r.unmarkedDays}</td>
                  <td className="num">{r.attendancePercentage == null ? <span className="muted">—</span> : <b>{r.attendancePercentage}%</b>}</td>
                  <td>
                    {Object.entries(r.counts).filter(([, n]) => n > 0).map(([status, n]) => (
                      <span key={status}><Badge>{status}</Badge> {n} </span>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </Card>
  )
}
