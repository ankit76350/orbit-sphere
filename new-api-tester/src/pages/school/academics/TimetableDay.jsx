import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CopyPlus, Info, Pencil, Plus, RefreshCw, ShieldAlert, SquarePen, Trash2 }
  from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { tabPath } from '../../../paths.js'
import TimetableCopy from './TimetableCopy.jsx'
import { AddEntryModal, PatchEntryModal } from './TimetableEntryForms.jsx'
import TimetableReplace from './TimetableReplace.jsx'

const LIST = tabPath('school', 'academics', 'timetable', 'view-timetable')

/**
 * One school day: /school-academics/timetable/:date
 *
 * SIX ENDPOINTS — #7 fills the page; #2 and #6 sit behind buttons; and #3, #4 and #5 act on ONE
 * period, which is why they are here rather than anywhere else: the period is already in front of
 * you.
 *
 * THE SINGLE-ENTRY WRITES ARE THE POINT OF THE MODULE, and the page says so by putting them on the
 * rows rather than in a form below. Correcting one period is a substitution; replacing the day (#2)
 * is the blunt instrument that existed until #4 did.
 *
 * #6 IS HERE BECAUSE THE SOURCE IS. "Monday is typed once, the rest of the week is copied from it"
 * starts with Monday on screen, so the form fills the source in and asks only where it is going —
 * the mirror of the API, which takes the target in the path because the day being BUILT is what it
 * acts on.
 *
 * #2 IS DELIBERATELY BEHIND A BUTTON. It is the only full-document write in the module and its own
 * plan calls it a footgun: a period left out of the list is gone. A screen that always showed the
 * editable copy would invite a replace where a reader only wanted to read.
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

  //! #2 IS OFF BY DEFAULT. The editor prefills itself from the day, ids and all, so opening it
  //! costs nothing — but it is the destructive write in this module, and a reader who came here
  //! to read should not find it already open.
  const [replacing, setReplacing] = useState(false)

  //! #6 OPENS A DIALOG, not a card below the day. It writes a DIFFERENT day than the one on
  //! screen, so it is its own little request with its own body — and the modal shows that body
  //! being built beside the fields that build it.
  const [copying, setCopying] = useState(false)

  //! #3 AND #4 ARE DIALOGS; #5 IS NOT. Adding and correcting have a body worth seeing built, and
  //! the modal shows it beside the fields. A removal has no body at all — it is a button, and what
  //! it needs is the answer afterwards, which lands in `removal` below.
  const [adding, setAdding] = useState(false)
  const [patching, setPatching] = useState(null)
  const [removal, setRemoval] = useState(null)

  //! REMOVING IS NOT GUARDED BEHIND A CONFIRMATION, deliberately. Every refusal #5 has —
  //! ENTRY_STILL_REFERENCED, a second removal answering 404 rather than a polite 204 — is
  //! something to trigger on purpose, and a dialog in the way makes each one two clicks slower.
  const removeEntry = async (entry) => {
    const answer = await call('remove-timetable-entry', {
      label: 'Remove one period',
      pathParams: {
        year: actingAcademicYear ?? '',
        date: date ?? '',
        entryId: entry.timetableEntryId,
      },
    })
    setRemoval({ entry, answer })
    if (answer.ok) load()
  }

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
      <td>{actions(entry)}</td>
    </tr>
  )

  //! THE SAME TWO BUTTONS IN BOTH TABLES. One function rather than two copies, because a row that
  //! could be corrected in one view and not the other would be a difference nobody meant.
  const actions = (entry) => (
    <div className="btn-row">
      <Button icon={SquarePen} onClick={() => setPatching(entry)}>Correct</Button>
      <Button icon={Trash2} onClick={() => removeEntry(entry)}>Remove</Button>
    </div>
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
        <th />
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
        <Button icon={Plus} onClick={() => setAdding(true)}>Add a period</Button>
        <Button icon={CopyPlus} onClick={() => setCopying(true)}>Copy this day</Button>
        <Button icon={Pencil} look={replacing ? 'primary' : undefined}
          onClick={() => setReplacing((r) => !r)}>
          {replacing ? 'Close the editor' : 'Replace the day'}
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
                  <th>Classes</th><th>Sections</th><th>Staff</th><th>Version</th>
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
                  {/* WHAT #2 REQUIRES, shown where a reader will look for it. */}
                  <td><span className="mono">{day.version}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <p className="muted">
            <Info size={12} /> <b>Sections are counted paired with their class</b> —
            &ldquo;A&rdquo; of one class and &ldquo;A&rdquo; of another are two.
          </p>
          <p className="muted">
            <Info size={12} /> <b>The version is what #2 replaces against.</b> A read carries it
            because the write requires it — a required field with no way to obtain it would be a
            refusal nobody could satisfy.
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
                    <th />
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
                      <td>{actions(entry)}</td>
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

      {/* #2 — THE DESTRUCTIVE WRITE, opened on purpose. It prefills from the day above, so the
          editor and the table can never disagree about what is stored. */}
      {replacing && day ? (
        <TimetableReplace
          key={`${day.dailyTimetableDocsId}:${day.version}`}
          day={day}
          date={date}
          onReplaced={load}
        />
      ) : null}

      {/* #5 — THE ANSWER TO A REMOVAL, which has no body of its own to show beforehand. A 404 on
          a second press and a 409 when attendance names the period are both worth reading. */}
      {removal ? (
        <Card
          title={removal.answer.ok
            ? `Removed ${removal.entry.periodCode}`
            : (removal.answer.bodyJson?.code ?? `The server answered ${removal.answer.status}`)}
          description="#5 — a $pull by id. Nothing else in the day is touched, and the day's version moves so #2 can tell."
          action={<Badge>{removal.answer.status}</Badge>}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok={removal.answer.ok ? 'true' : 'false'}>
                {removal.answer.ok
                  ? '204 · no content'
                  : (removal.answer.bodyJson?.code ?? removal.answer.status)}
              </span>
            </div>
            <p>
              {removal.answer.ok
                ? `${removal.entry.periodCode} is gone. Pressing Remove on it again would be a 404,
                   not a second 204 — a caller deleting a period that has already gone has a stale
                   screen and should know.`
                : (removal.answer.bodyJson?.message ?? 'Nothing came back.')}
            </p>
          </div>
        </Card>
      ) : null}

      {/* #6 — BUILDS A DIFFERENT DAY, from the one on screen. A dialog rather than a card: the
          fields and the request body they build sit side by side, which is what this tool is for.
          It reads nothing of its own — the classes and sections it offers are this day's. */}
      <TimetableCopy open={copying} onClose={() => setCopying(false)} day={day} date={date} />

      {/* #3 — ONE PERIOD INTO THIS DAY. A $push, so the periods already there are not rewritten. */}
      <AddEntryModal open={adding} onClose={() => setAdding(false)} day={day} date={date}
        onDone={load} />

      {/* #4 — THE SUBSTITUTION. Keyed on the period, so opening a different row starts from that
          row's values rather than the last one's. */}
      <PatchEntryModal
        key={patching?.timetableEntryId}
        open={!!patching}
        onClose={() => setPatching(null)}
        day={day}
        date={date}
        entry={patching}
        onDone={load}
      />

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> Anybody who can reach the
          API with a school subdomain can read a school&apos;s whole day.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Ten of twelve exist.</b> What is left is #11, one room&apos;s day,
          and #12 — who is <em>free</em> to cover a period, which is the query that makes a
          substitution possible without double-booking somebody by eye.
        </p>
        <p className="muted">
          <ShieldAlert size={12} /> <b>#2 does not check attendance; #5 does.</b> Replacing a day
          can silently drop a period an attendance session names — the removed ids are reported so
          it is at least visible — while removing one period refuses outright. Nothing writes that
          collection yet, so neither has fired in anger.
        </p>
      </Card>
    </div>
  )
}
