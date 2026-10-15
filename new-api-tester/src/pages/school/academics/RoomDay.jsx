import { useCallback, useState } from 'react'
import { Info, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One room's day: /school-academics/timetable/room-day
 *
 * ONE ENDPOINT — #11, "what is the lab doing today", asked before a room booking is approved.
 *
 * THE ROOM ID IS TYPED, because rooms cannot be listed yet — nothing reads FacilityResource. That
 * is also why an unknown id is an empty day and not a 404: the server cannot check it either.
 *
 * NOTHING IS DISABLED.
 */
export default function RoomDay() {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const [date, setDate] = useState('')
  const [roomId, setRoomId] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const pathParams = { year: actingAcademicYear ?? '', date, facilityResourceDocsId: roomId }

  const look = useCallback(async () => {
    setLoading(true)
    const answer = await call('get-room-day', { label: "One room's day", pathParams })
    setLoading(false)
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingAcademicYear, date, roomId])

  if (!actingSubdomain) return <NoSchoolChosen what="A room's day" />

  const day = result?.ok ? result.bodyJson : null
  const rows = day?.entries ?? []

  return (
    <>
      <Card
        title="One room's day"
        description="Every period booked in one room on one date — what is checked before a room booking is approved."
        action={
          <div className="btn-row">
            <EndpointTag id="get-room-day" name="Open" pathParams={pathParams} />
            <Button look="primary" icon={Search} busy={loading} onClick={look}>Open the day</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="Date" hint="A holiday answers 404 NOT_A_WORKING_DAY; a working day with nothing written answers 404 TIMETABLE_NOT_FOUND.">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          <Field label="Room id" hint="The facilityResourceDocsId a period stores. Typed, because rooms cannot be listed yet.">
            <Input value={roomId} placeholder="the room's id"
              onChange={(e) => setRoomId(e.target.value)} />
          </Field>
        </div>
        <p className="muted">
          <Info size={12} /> <b>The room id is not checked.</b> Nothing in the backend reads rooms
          yet, so a mistyped id answers 200 with no periods — the same as an empty room. The answer
          says so.
        </p>
      </Card>

      {result && !day ? (
        <Card title={result.bodyJson?.code ?? `The server answered ${result.status}`}
          action={<Badge>{result.status}</Badge>}>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </Card>
      ) : null}

      {day ? (
        <Card
          title={`Room ${day.facilityResourceDocsId}`}
          description={day.firstStartTime
            ? `In use from ${day.firstStartTime} to ${day.lastEndTime}. Earliest first.`
            : 'Not in use on this date.'}
          action={<Badge>{day.entryCount} periods</Badge>}
        >
          {rows.length === 0 ? (
            <Empty title="Nothing booked" description={day.nextStep} />
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
                    <th>Teacher</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((entry) => (
                    <tr key={entry.timetableEntryId}>
                      <td><span className="mono">{entry.periodCode}</span></td>
                      <td className="mono">{entry.startTime}–{entry.endTime}</td>
                      <td>{entry.className ?? <span className="mono muted">{entry.classDocsId}</span>}</td>
                      <td>{entry.sectionNo}</td>
                      <td><Badge>{entry.slotType}</Badge></td>
                      <td>{entry.subjectName ?? entry.subjectCode ?? <span className="muted">—</span>}</td>
                      <td>{entry.teacherName ?? <span className="muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      ) : null}
    </>
  )
}
