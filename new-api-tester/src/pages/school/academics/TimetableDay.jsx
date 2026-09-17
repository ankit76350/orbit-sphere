import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Info, RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { screenPath } from '../../../paths.js'

const LIST = screenPath('school', 'academics', 'timetable')

/**
 * One school day: /school-academics/timetable/:date
 *
 * ONE ENDPOINT — #7, opened by clicking a row of #10's list.
 *
 * ADDRESSED BY THE DATE, NOT BY A DOCUMENT ID, which is unusual in this app and is what the API
 * does: a caller always knows the date and never knows the id. So the address is the date, and a
 * row of the list carries the reader here by the column they were already reading.
 *
 * THE NAMES COME FROM THE SERVER. Each period arrives with className, subjectName and teacherName
 * beside its ids — two extra queries there against about seventy round trips here. The ids are
 * shown too, in mono and muted, because this is an API testing tool and what was stored is the
 * thing being tested.
 *
 * GROUPED BY SECTION HERE, NOT THERE. The API returns entries in stored order and says why:
 * periods of different sections run at the same hour, so "by time" is a tie with a hidden second
 * key, and which grouping a screen wants is the screen's question. This screen wants sections, so
 * it groups them — and the stored order is one click away, because losing it would hide what #7
 * actually guarantees.
 *
 * THREE REFUSALS, RENDERED AS THEY CAME. A holiday, a working day with nothing on it, and a date
 * outside the year are three different answers, and the whole point of #7 having three codes is
 * that a screen can tell them apart. Nothing here predicts which one fires.
 *
 * NOTHING IS DISABLED. Every refusal must stay reachable by typing a date into the address.
 */
export default function TimetableDay() {
  const { date } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [day, setDay] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  //! STORED ORDER IS ONE CLICK AWAY, and it is the default. #7's contract is that entries come
  //! back as they were written; a page that only ever showed them regrouped would make that
  //! guarantee unobservable, which in an API tester is the same as not having it.
  const [grouped, setGrouped] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-timetable', {
      label: 'One school day',
      pathParams: { year: actingAcademicYear ?? '', date: date ?? '' },
    })
    setLoading(false)
    if (result.ok) { setDay(result.bodyJson); setProblem(null) } else { setDay(null); setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, date])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="A school day" />

  const entries = day?.entries ?? []

  //! ONE GROUP PER CLASS-AND-SECTION, in the order each pair first appears — so the grouping never
  //! reorders a section's own periods, only gathers them. "A" of one class and "A" of another are
  //! two groups, the same pairing sectionCount counts by.
  const groups = []
  const seen = new Map()
  for (const entry of entries) {
    const key = JSON.stringify([entry.classDocsId, entry.sectionNo])
    let group = seen.get(key)
    if (!group) {
      group = {
        key,
        className: entry.className,
        classDocsId: entry.classDocsId,
        sectionNo: entry.sectionNo,
        rows: [],
      }
      seen.set(key, group)
      groups.push(group)
    }
    group.rows.push(entry)
  }

  const row = (entry) => (
    <tr key={entry.timetableEntryId}>
      <td><span className="mono">{entry.periodCode}</span></td>
      <td>
        {entry.startTime} – {entry.endTime}
      </td>
      <td><Badge>{entry.slotType}</Badge></td>
      <td>
        {/* THE NAME, AND THE ID UNDER IT. A break carries neither, and shows nothing. */}
        {entry.subjectName || entry.subjectCode || <span className="muted">—</span>}
        {entry.subjectName && entry.subjectCode
          ? <div className="muted mono">{entry.subjectCode}</div> : null}
      </td>
      <td>
        {entry.teacherName || entry.teacherDocsId || <span className="muted">—</span>}
        {entry.teacherName
          ? <div className="muted mono">{entry.teacherDocsId}</div> : null}
      </td>
      <td>{entry.slotLabel || <span className="muted">—</span>}</td>
      <td>
        {/* NOT RESOLVED, deliberately: #1 never checked the room exists, so there is no name to
            show and inventing one would hide that. */}
        {entry.facilityResourceDocsId
          ? <span className="mono">{entry.facilityResourceDocsId}</span>
          : <span className="muted">—</span>}
      </td>
      <td><span className="muted mono">{entry.timetableEntryId}</span></td>
    </tr>
  )

  const head = (
    <thead>
      <tr>
        <th>Period</th>
        <th>Time</th>
        <th>Slot</th>
        <th>Subject</th>
        <th>Teacher</th>
        <th>Label</th>
        <th>Room id</th>
        <th>Entry id</th>
      </tr>
    </thead>
  )

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{date}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · one school day in full · in{' '}
            <span className="mono">{actingAcademicYear ?? 'no year chosen'}</span>
            {day ? <> · <span className="mono">{day.dailyTimetableDocsId}</span></> : null}
          </p>
        </div>
        <span className="toolbar-spacer" />
        {/* A Link, not a Button — Button renders a <button>, which cannot be an address. */}
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All days</Link>
        <EndpointTag id="get-timetable" name="Get"
          pathParams={{ year: actingAcademicYear, date }} />
        <Button onClick={() => setGrouped((g) => !g)}>
          {grouped ? 'Stored order' : 'Group by section'}
        </Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card
          title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}
          description="Three different refusals live here, and which one fired is the point. Nothing on this page predicts it."
          action={<Badge>{problem.status}</Badge>}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <p>{problem.bodyJson?.message ?? 'Nothing came back.'}</p>
          </div>
          <p className="muted">
            <Info size={12} /> <b>A holiday is 404 NOT_A_WORKING_DAY and names the holiday.</b>
            {' '}Nothing is missing — the school was closed, and that is not something to act on.
          </p>
          <p className="muted">
            <Info size={12} /> <b>A working day with nothing on it is 404 TIMETABLE_NOT_FOUND.</b>
            {' '}This is the one a school acts on, and the only reason the two are separate codes.
          </p>
          <p className="muted">
            <Info size={12} /> <b>409 DATE_OUTSIDE_ACADEMIC_YEAR means the year above is wrong
            for this date</b>, not that the day is missing. Change the year in the header.
          </p>
        </Card>
      ) : null}

      {day ? (
        <Card
          title="The day"
          description="The same five counts a row of the list carries — repeated here because a day reached by a link never saw the list."
          action={
            <div className="btn-row">
              <Badge>{day.entryCount} periods</Badge>
              <Badge>{day.lessonCount} lessons</Badge>
            </div>
          }
        >
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th><th>Year</th><th>Periods</th><th>Lessons</th>
                  <th>Classes</th><th>Sections</th><th>Staff</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{day.date}</td>
                  <td><span className="mono">{day.academicYear}</span></td>
                  <td><b>{day.entryCount}</b></td>
                  <td>{day.lessonCount}
                    {day.entryCount > day.lessonCount
                      ? <span className="muted"> · {day.entryCount - day.lessonCount} other</span>
                      : null}</td>
                  <td>{day.classCount}</td>
                  <td>{day.sectionCount}</td>
                  <td>{day.teacherCount}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p className="muted">
            <Info size={12} /> <b>Sections are counted paired with their class</b> —
            &ldquo;A&rdquo; of one class and &ldquo;A&rdquo; of another are two.
          </p>
        </Card>
      ) : null}

      {day ? (
        <Card
          title={grouped ? 'Periods, grouped by section' : 'Periods, in stored order'}
          description={grouped
            ? 'Gathered by class and section here, not by the API. A group never reorders a section’s own periods.'
            : 'Exactly as #7 returned them — the order they were written, never re-sorted.'}
          action={<Badge>{entries.length} rows</Badge>}
        >
          {entries.length === 0 ? (
            <Empty
              title="A day with no periods"
              description="The document exists and its entries array is empty. #1 cannot write that, so it means something removed them."
            />
          ) : grouped ? (
            groups.map((group) => (
              <div key={group.key} className="stack">
                <p className="muted">
                  <b>{group.className ?? 'Unknown class'} · section {group.sectionNo}</b>
                  {' '}<span className="mono">{group.classDocsId}</span>
                  {' · '}{group.rows.length} periods
                </p>
                <div className="table-scroll">
                  <table className="data-table">
                    {head}
                    <tbody>{group.rows.map(row)}</tbody>
                  </table>
                </div>
              </div>
            ))
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Period</th>
                    <th>Class</th>
                    <th>Section</th>
                    <th>Time</th>
                    <th>Slot</th>
                    <th>Subject</th>
                    <th>Teacher</th>
                    <th>Label</th>
                    <th>Room id</th>
                    <th>Entry id</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => (
                    <tr key={entry.timetableEntryId}>
                      <td><span className="mono">{entry.periodCode}</span></td>
                      <td>
                        {entry.className ?? <span className="muted">unknown</span>}
                        <div className="muted mono">{entry.classDocsId}</div>
                      </td>
                      <td>{entry.sectionNo}</td>
                      <td>{entry.startTime} – {entry.endTime}</td>
                      <td><Badge>{entry.slotType}</Badge></td>
                      <td>
                        {entry.subjectName || entry.subjectCode || <span className="muted">—</span>}
                        {entry.subjectName && entry.subjectCode
                          ? <div className="muted mono">{entry.subjectCode}</div> : null}
                      </td>
                      <td>
                        {entry.teacherName || entry.teacherDocsId
                          || <span className="muted">—</span>}
                        {entry.teacherName
                          ? <div className="muted mono">{entry.teacherDocsId}</div> : null}
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
            <Info size={12} /> <b>A name is resolved by the server, and absent when it cannot
            be.</b> A class deleted or a staff member removed after the day was written leaves the
            id and drops the name — the day still reads back.
          </p>
          <p className="muted">
            <Info size={12} /> <b>A room is never resolved.</b> #1 does not check that a room
            exists when it writes one, so there is no name to show and a blank one would hide that.
          </p>
          <p className="muted">
            <Info size={12} /> <b>Keep the entry id.</b> It is what an attendance session stores
            and what #4 will address a period by when a substitution is made.
          </p>
        </Card>
      ) : null}

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> Anybody who can reach the
          API with a school subdomain can read a school&apos;s whole day.
        </p>
        <p className="muted">
          <Info size={12} /> <b>There is still no way to change one period.</b> #4 — the
          substitution — is the next endpoint, and the one this module exists for.
        </p>
      </Card>
    </div>
  )
}
