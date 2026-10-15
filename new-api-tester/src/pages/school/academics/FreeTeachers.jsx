import { useCallback, useState } from 'react'
import { Info, Search, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * Who is free to cover: /school-academics/timetable/free-teachers
 *
 * ONE ENDPOINT — #12, the 07:40 question: a teacher is absent, who can take period 3?
 *
 * FREE MEANS "NOTHING ON THE TIMETABLE IN THIS WINDOW", NOT "AT WORK". Leave is not built and
 * nothing marks who teaches, so every staff member is checked — the page says so in a warning,
 * because it is the one thing about this answer that would mislead somebody acting on it.
 *
 * NOTHING IS DISABLED — a backwards window is 400 INVALID_TIME_WINDOW, worth being able to send.
 */
export default function FreeTeachers() {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const [date, setDate] = useState('')
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('09:45')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const pathParams = { year: actingAcademicYear ?? '', date }
  //! ONLY THE TIMES THAT HAVE A VALUE are sent, so a missing one reaches 400 MISSING_PARAMETER.
  const query = {
    ...(startTime ? { startTime } : {}),
    ...(endTime ? { endTime } : {}),
  }

  const look = useCallback(async () => {
    setLoading(true)
    const answer = await call('get-free-teachers', { label: 'Who is free to cover', pathParams, query })
    setLoading(false)
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingAcademicYear, date, startTime, endTime])

  if (!actingSubdomain) return <NoSchoolChosen what="Free teachers" />

  const answer = result?.ok ? result.bodyJson : null
  const rows = answer?.teachers ?? []

  return (
    <>
      <Card
        title="Who is free to cover"
        description="Everybody with nothing on the timetable during one window of one date, least busy first."
        action={
          <div className="btn-row">
            <EndpointTag id="get-free-teachers" name="Find" pathParams={pathParams} query={query} />
            <Button look="primary" icon={Search} busy={loading} onClick={look}>Find</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="Date" hint="A holiday answers 404 NOT_A_WORKING_DAY; a working day with nothing written answers 404 TIMETABLE_NOT_FOUND.">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          <Field label="From" hint="HH:mm. Must be before To, or it is 400 INVALID_TIME_WINDOW. Empty is 400 MISSING_PARAMETER.">
            <Input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
          </Field>
          <Field label="To" hint="A period that ends exactly at From, or starts exactly at To, does not count as busy.">
            <Input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </Field>
        </div>
        <p className="muted">
          <ShieldAlert size={12} /> <b>Free does not mean at work.</b> Leave is not built, and
          nothing marks which staff teach — so every staff member is checked, and somebody on leave
          reads as free. Check before you send them to a classroom.
        </p>
      </Card>

      {result && !answer ? (
        <Card title={result.bodyJson?.code ?? `The server answered ${result.status}`}
          action={<Badge>{result.status}</Badge>}>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </Card>
      ) : null}

      {answer ? (
        <Card
          title={`${answer.freeCount} of ${answer.staffCount} free from ${answer.startTime} to ${answer.endTime}`}
          description="Least busy first. The counts are the whole day, so you can weigh free against already busy."
        >
          {rows.length === 0 ? (
            <Empty title="Nobody is free" description="Every staff member has a period overlapping this window." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Staff</th>
                    <th>Employee no</th>
                    <th className="num">Periods today</th>
                    <th className="num">Lessons today</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((one) => (
                    <tr key={one.staffDocsId}>
                      <td>{one.fullName}<div className="mono muted">{one.staffDocsId}</div></td>
                      <td className="mono">{one.employeeNo ?? '—'}</td>
                      <td className="num">{one.periodCount}</td>
                      <td className="num">{one.lessonCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted"><Info size={12} /> {answer.nextStep}</p>
        </Card>
      ) : null}
    </>
  )
}
