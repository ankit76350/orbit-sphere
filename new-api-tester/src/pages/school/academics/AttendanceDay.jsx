import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Card, Field, Input } from '../../../components/ui/Kit.jsx'
import { childPath } from '../../../paths.js'

/**
 * Attendance tab "Attendance on a date": /school-academics/attendance/day
 *
 * ONE ENDPOINT — #8: which sections have taken their DAILY register on a date (today by default),
 * and which have not. A row opens that section on that date.
 */

/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default function AttendanceDay({ reloadKey }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const year = actingAcademicYear ?? ''
  const [date, setDate] = useState(today())
  const [day, setDay] = useState(null)

  //! A holiday or a date outside the year comes back as a refusal, shown in place of the table.
  const load = useCallback(async () => {
    if (!actingSubdomain || !year || !date) return
    const answer = await call('attendance-day', {
      label: `Attendance on ${date}`,
      pathParams: { year },
      query: { date },
    })
    setDay(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, date, reloadKey])

  useEffect(() => { load() }, [load])

  return (
    <Card
      title={day?.ok ? `${day.bodyJson.takenCount} of ${day.bodyJson.sectionCount} sections taken` : 'Attendance on a date'}
      description="Which sections have taken their DAILY register, and which have not. A row opens that section on that date."
      action={<EndpointTag id="attendance-day" name="Day" pathParams={{ year }} query={{ date }} />}
    >
      <div className="field-grid">
        <Field label="Date" hint="Defaults to today. A holiday or weekly off answers 404 NOT_A_WORKING_DAY.">
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
      </div>
      {day && !day.ok ? (
        <pre className="resp-body">{day.bodyJson?.message ?? day.bodyText}</pre>
      ) : day?.ok ? (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Section</th><th>Daily register</th><th className="num">Children</th><th className="num">Marked</th><th className="num">Not marked</th><th className="num">Period registers</th></tr>
            </thead>
            <tbody>
              {day.bodyJson.sections.map((r) => (
                <tr key={`${r.classDocsId}-${r.sectionNo}`} style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`${childPath('school', 'academics', 'attendance', r.classDocsId, 'sections', r.sectionNo)}?date=${date}`)}>
                  <td><b>{r.className}</b> {r.sectionNo}</td>
                  <td>{r.taken
                    ? <Badge tone={r.status === 'LOCKED' ? 'good' : 'warn'}>{r.status}</Badge>
                    : <Badge tone="bad">not taken{r.status === 'CANCELLED' ? ' (cancelled)' : ''}</Badge>}</td>
                  <td className="num">{r.studentCount}</td>
                  <td className="num">{r.markedCount}</td>
                  <td className="num">{r.unmarkedCount}</td>
                  <td className="num">{r.periodRegisterCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </Card>
  )
}
