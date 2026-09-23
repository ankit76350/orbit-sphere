import { useCallback, useEffect, useState } from 'react'
import { Info, Search, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One teacher's day: /school-academics/timetable/teacher-day
 *
 * ONE ENDPOINT — #9, and it is what a teacher's app opens.
 *
 * ITS OWN ADDRESS, like #8's. "What a teacher opens" is an entry point, not a filter somebody
 * arrives at after reading the whole school's Tuesday.
 *
 * A BREAK THEY SUPERVISE IS IN THE LIST, and the counts say so: entryCount is what they are
 * committed to, lessonCount is what they teach, and the difference is duty. Somebody who only
 * supervises lunch still has a working day — which is exactly why they cannot also be teaching
 * period 4.
 *
 * THE ENDS OF THE DAY ARE NOT "FREE FROM" AND "FREE UNTIL". firstStartTime and lastEndTime bracket
 * what this person is committed to; the gaps between are not computed, and #12 — who can cover this
 * period — is the endpoint that answers coverage against every member of staff rather than one.
 * The page says so rather than letting the two numbers imply otherwise.
 *
 * AN UNKNOWN ID AND A FREE DAY LOOK DIFFERENT HERE, because they are different in the API: a real
 * person with nothing on is a 200 with no rows, an id that is not staff is a 404.
 *
 * NOTHING IS DISABLED.
 */
export default function TeacherDay() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [date, setDate] = useState('')
  const [teacherDocsId, setTeacherDocsId] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  //! THE PEOPLE TO CHOOSE FROM. One read, and the only one this screen makes of its own.
  const [teachers, setTeachers] = useState([])

  useEffect(() => {
    if (!actingSubdomain) return
    let alive = true
    call('list-staff', { label: 'Who can be looked up', query: { size: '100' } })
      .then((answer) => { if (alive) setTeachers(answer.bodyJson?.content ?? []) })
    return () => { alive = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain])

  const look = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const answer = await call('get-teacher-day', {
      label: "One teacher's day",
      pathParams: { year: actingAcademicYear ?? '', date, teacherDocsId },
    })
    setLoading(false)
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, date, teacherDocsId])

  if (!actingSubdomain) return <NoSchoolChosen what="A teacher's day" />

  const day = result?.ok ? result.bodyJson : null
  const rows = day?.entries ?? []

  return (
    <>
      <Card
        title="Whose day"
        description="One member of staff, one date. This is the read a teacher's app makes when it opens in the morning."
        action={
          <div className="btn-row">
            <EndpointTag id="get-teacher-day" name="Open"
              pathParams={{ year: actingAcademicYear, date, teacherDocsId }} />
            <Button look="primary" icon={Search} busy={loading} onClick={look}>Open the day</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="Date" hint="A holiday answers 404 NOT_A_WORKING_DAY and names it; a working day with nothing written answers 404 TIMETABLE_NOT_FOUND.">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          {/* THE READ THAT FILLS THE BOX BELOW. Not a button of its own — it runs when the
              screen opens — but it is a call this page makes, and the tag is how this app says
              so. */}
          <p className="muted">
            <EndpointTag id="list-staff" name="Who can be looked up" query={{ size: '100' }} />
          </p>
          <Field label="Staff member" hint="Must be this school's. An unknown id is 404 TEACHER_NOT_FOUND, never an empty day — an app must not show a free morning for a wrong id.">
            <Select
              label="Staff member"
              value={teacherDocsId}
              onChange={setTeacherDocsId}
              options={[{ value: '', label: '— pick somebody —' },
                ...teachers.map((one) => ({ value: one.staffDocsId, label: one.fullName }))]}
            />
          </Field>
        </div>
        <p className="muted">
          <Info size={12} /> <b>Anybody on staff can be asked about, not only teachers.</b> The
          field is a <span className="mono">teacherDocsId</span> because that is what a period
          stores, but what it points at is a staff record — and whoever is named on a break is on
          duty that hour whether or not they teach.
        </p>
      </Card>

      {result && !day ? (
        <Card
          title={result.bodyJson?.code ?? `The server answered ${result.status}`}
          description="Nothing on this page predicted it. The three day-level refusals are the same three #7 gives."
          action={<Badge>{result.status}</Badge>}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {result.bodyJson?.code ?? result.status}
              </span>
            </div>
            <p>{result.bodyJson?.message ?? 'Nothing came back.'}</p>
          </div>
          <p className="muted">
            <Info size={12} /> <b>404 TEACHER_NOT_FOUND is not the same as a free day.</b> A real
            person with nothing scheduled answers <b>200</b> with no rows — which is the whole
            reason the two are different answers.
          </p>
        </Card>
      ) : null}

      {day ? (
        <Card
          title={day.teacherName ?? 'That person'}
          description="Earliest first — a real order here, because a teacher cannot be in two places at once."
          action={
            <div className="btn-row">
              <Badge>{day.entryCount} periods</Badge>
              <Badge>{day.lessonCount} lessons</Badge>
              <Badge>{day.sectionCount} sections</Badge>
            </div>
          }
        >
          {/* THE ENDS OF THE DAY, and what they are not. */}
          {day.firstStartTime ? (
            <p className="muted">
              <Info size={12} /> <b>Committed from {day.firstStartTime} to {day.lastEndTime}.</b>
              {' '}That is the bracket, <b>not</b> a statement that the hours between are busy — the
              gaps are not computed here. Who is <em>free</em> to cover a period is #12, which is
              not built.
            </p>
          ) : null}

          {rows.length === 0 ? (
            <Empty
              title="Nothing on that day"
              description="A 200, not a 404 — this is a real person with a free day. An unknown id would have been 404 TEACHER_NOT_FOUND, and no first or last time is reported because there is no first period."
            />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Period</th>
                    <th>Time</th>
                    <th>Class</th>
                    <th>Section</th>
                    <th>Slot</th>
                    <th>Subject</th>
                    <th>Label</th>
                    <th>Room id</th>
                    <th>Entry id</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((entry) => (
                    <tr key={entry.timetableEntryId}>
                      <td><span className="mono">{entry.periodCode}</span></td>
                      <td>{entry.startTime} – {entry.endTime}</td>
                      <td>
                        {entry.className ?? <span className="muted">unknown</span>}
                        <div className="muted mono">{entry.classDocsId}</div>
                      </td>
                      <td>{entry.sectionNo}</td>
                      <td><Badge>{entry.slotType}</Badge></td>
                      <td>
                        {entry.subjectName || entry.subjectCode || <span className="muted">—</span>}
                      </td>
                      <td>{entry.slotLabel || <span className="muted">—</span>}</td>
                      <td>
                        {entry.facilityResourceDocsId
                          ? <span className="mono">{entry.facilityResourceDocsId}</span>
                          : <span className="muted">—</span>}
                      </td>
                      <td><span className="muted mono">{entry.timetableEntryId}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted">
            <Info size={12} /> <b>A break they supervise is in this list.</b> It counts against
            their day, which is why <span className="mono">entryCount</span> and{' '}
            <span className="mono">lessonCount</span> differ — and why somebody supervising lunch
            cannot also be teaching period 4.
          </p>
          <p className="muted">
            <Info size={12} /> <b>Sections are counted paired with their class</b> —
            &ldquo;A&rdquo; of one class and &ldquo;A&rdquo; of another are two.
          </p>
          <p className="muted">{day.nextStep}</p>
        </Card>
      ) : null}

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> This is the read a
          teacher&apos;s app would make, and right now anybody with the subdomain can make it for
          any member of staff.
        </p>
      </Card>
    </>
  )
}
