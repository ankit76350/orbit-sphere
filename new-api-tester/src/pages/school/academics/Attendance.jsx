import { useCallback, useEffect, useState } from 'react'
import { ChevronRight, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath, detailPath } from '../../../paths.js'

/**
 * Attendance, step 1 of 3: /school-academics/attendance
 *
 * ON TOP, #8 — WHICH SECTIONS HAVE TAKEN ATTENDANCE on a date (today by default), and which have
 * not. A row opens that section on that date.
 *
 * BELOW, THE CLASSES OF THE YEAR SELECTED IN THE NAVBAR. Pick one to see its sections, pick a section to
 * see its registers, open a register to see its children. Each step is its own address, so a
 * reload or a link lands on the same step.
 */
/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default function Attendance() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const year = actingAcademicYear ?? ''

  const [classes, setClasses] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [date, setDate] = useState(today())
  const [day, setDay] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('list-school-classes', {
      label: `Classes in ${year}`,
      pathParams: { year },
      query: { page: '0', size: '100' },
    })
    setLoading(false)
    if (answer.ok) { setClasses(answer.bodyJson?.content ?? []); setProblem(null) } else { setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year])

  useEffect(() => { load() }, [load])

  //! #8 — WHO HAS TAKEN ATTENDANCE ON THE DATE. A holiday or a date outside the year comes back as
  //! a refusal, shown in place of the table.
  const loadDay = useCallback(async () => {
    if (!actingSubdomain || !year || !date) return
    const answer = await call('attendance-day', {
      label: `Attendance on ${date}`,
      pathParams: { year },
      query: { date },
    })
    setDay(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, date])

  useEffect(() => { loadDay() }, [loadDay])

  if (!actingSubdomain) return <NoSchoolChosen what="Attendance" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Attendance</h1>
          <p className="muted">
            Pick a class in <b>{year || 'no year selected in the navbar'}</b>, then a section, then a
            register.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} />
        <Button icon={RefreshCw} onClick={() => { load(); loadDay() }} busy={loading}>Refresh</Button>
      </div>

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

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      <Card title={classes ? `${classes.length} class${classes.length === 1 ? '' : 'es'}` : 'Classes'}>
        {classes && classes.length === 0 ? (
          <Empty title="No classes in this year" description="Create classes under Academics › Classes first." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Class</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {(classes ?? []).map((c) => (
                  <tr key={c.schoolClassId} style={{ cursor: 'pointer' }}
                    onClick={() => navigate(detailPath('school', 'academics', 'attendance', c.schoolClassId))}>
                    <td><b>{c.name}</b></td>
                    <td>{c.active === false ? <Badge>not active</Badge> : <Badge tone="good">active</Badge>}</td>
                    <td><Button icon={ChevronRight}>Sections</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
