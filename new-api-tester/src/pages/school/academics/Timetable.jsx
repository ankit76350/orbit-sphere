import { useCallback, useState } from 'react'
import { Info, Plus, ShieldAlert, Trash2 } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * Building a school day: /school-academics/timetable
 *
 * ONE ENDPOINT — #1, which writes a day's periods across one date or a RANGE of them. A school does
 * not build one Tuesday; it builds a pattern and applies it to a term, which is why the dates are
 * a range and why they are in the body rather than the path.
 *
 * LEAVE "TO" EMPTY FOR ONE DAY. That is the single-date form of the same call, and the page says so
 * rather than offering two buttons for one endpoint.
 *
 * A HOLIDAY INSIDE THE RANGE IS SKIPPED AND NAMED, not refused — any range longer than about five
 * days contains a weekly off. A date that ALREADY has a timetable refuses the whole request. Both
 * answers are rendered from the response rather than predicted here, because which one fires is
 * exactly what a tester is here to see.
 *
 * A LESSON NEEDS A SUBJECT AND A TEACHER; A BREAK MUST HAVE NEITHER. The form does not enforce
 * that: SLOT_FIELDS_REQUIRED and SLOT_FIELDS_NOT_ALLOWED are refusals worth triggering, and a form
 * that made them unreachable would be hiding the endpoint's own rules.
 *
 * THE SUBJECT MUST BE ONE THAT SECTION STUDIES — class-wide, or its own. The page states the rule
 * under the table because it is the one that surprises people, and it is a plain text box so a
 * subject the section does not take can be sent on purpose.
 *
 * NOTHING HERE IS DISABLED. Every refusal this endpoint has must be reachable by hand.
 */

const SLOT_TYPES = ['LESSON', 'BREAK', 'ASSEMBLY', 'ACTIVITY']

const BLANK_ROW = {
  periodCode: '',
  classDocsId: '',
  sectionNo: '',
  slotType: 'LESSON',
  subjectCode: '',
  teacherDocsId: '',
  slotLabel: '',
  startTime: '',
  endTime: '',
  facilityResourceDocsId: '',
}

export default function Timetable() {
  const { call } = useApi()
  const { actingSubdomain } = useApiState()

  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [rows, setRows] = useState([{ ...BLANK_ROW }])
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  const setRow = (index, field) => (event) => {
    const value = event?.target ? event.target.value : event
    setRows((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  //! BLANK MEANS NOT SENT. An empty string on the wire is a different thing from an absent field —
  //! the API treats "" as absent for the optional ones, and sending it anyway would test a
  //! normalisation rule rather than the rule the tester is looking at.
  const body = useCallback(() => {
    const out = { startDate, entries: rows.map((row) => {
      const entry = {
        periodCode: row.periodCode,
        classDocsId: row.classDocsId,
        sectionNo: row.sectionNo,
        slotType: row.slotType,
        startTime: row.startTime,
        endTime: row.endTime,
      }
      if (row.subjectCode.trim() !== '') entry.subjectCode = row.subjectCode.trim()
      if (row.teacherDocsId.trim() !== '') entry.teacherDocsId = row.teacherDocsId.trim()
      if (row.slotLabel.trim() !== '') entry.slotLabel = row.slotLabel.trim()
      if (row.facilityResourceDocsId.trim() !== '') {
        entry.facilityResourceDocsId = row.facilityResourceDocsId.trim()
      }
      return entry
    }) }
    if (endDate.trim() !== '') out.endDate = endDate.trim()
    return out
  }, [startDate, endDate, rows])

  const submit = useCallback(async () => {
    if (!actingSubdomain) return
    setSending(true)
    const answer = await call('create-timetable', { label: 'Build a day', body: body() })
    setSending(false)
    setResult(answer)
  }, [call, actingSubdomain, body])

  if (!actingSubdomain) return <NoSchoolChosen what="The timetable" />

  const created = result?.ok ? result.bodyJson : null

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Timetable</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · where every child is meant to be,
            hour by hour · the academic year is worked out from the date, never sent
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="create-timetable" name="Create" />
        <Button look="primary" busy={sending} onClick={submit}>Write it</Button>
      </div>

      <Card
        title="The dates"
        description="Leave 'to' empty to write one day. Fill it and every date between the two is written, each carrying the same periods."
      >
        <div className="field-grid">
          {/* A PICKER, so the value is always the ISO date the API wants. Both refusals stay
              reachable: a 'to' before 'from' is still selectable, and so is a range over 120
              days — the form constrains the FORMAT, never the request. */}
          <Field label="From" hint="Required. The first date to write.">
            <Input type="date" value={startDate}
              onChange={(e) => setStartDate(e.target.value)} />
          </Field>
          <Field
            label="To"
            hint="Optional — leave it empty for a single day. Earlier than 'from' is INVALID_DATE_RANGE; more than 120 days apart is DATE_RANGE_TOO_LONG."
          >
            <Input type="date" value={endDate}
              onChange={(e) => setEndDate(e.target.value)} />
          </Field>
        </div>
        <p className="muted">
          <Info size={12} /> <b>A holiday inside the range is skipped and named</b>, not refused —
          any range longer than about five days contains a weekly off. <b>A date that already has a
          timetable refuses the whole request</b>, because that means something is being rebuilt and
          half a range is worse than none.
        </p>
        <p className="muted">
          <Info size={12} /> <b>A weekly off is a dated holiday, never a weekday.</b> Nothing here
          infers a weekend. A school that runs on Sunday and closes on Friday is a normal school,
          and only the year&apos;s holiday list knows which.
        </p>
      </Card>

      <Card
        title={`The periods · ${rows.length}`}
        description="Applied to every date in the range. Nothing here is validated by the form — every refusal is the API's to give."
        action={
          <div className="btn-row">
            <Button icon={Plus} onClick={() => setRows((c) => [...c, { ...BLANK_ROW }])}>
              Add a period
            </Button>
          </div>
        }
      >
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Class id</th>
                <th>Section</th>
                <th>Slot</th>
                <th>Subject</th>
                <th>Teacher id</th>
                <th>Label</th>
                <th>From</th>
                <th>To</th>
                <th>Room id</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                // eslint-disable-next-line react/no-array-index-key
                <tr key={index}>
                  <td><Input value={row.periodCode} onChange={setRow(index, 'periodCode')}
                    placeholder="P1" /></td>
                  <td><Input value={row.classDocsId} onChange={setRow(index, 'classDocsId')}
                    placeholder="from Create Class" /></td>
                  <td><Input value={row.sectionNo} onChange={setRow(index, 'sectionNo')}
                    placeholder="A" /></td>
                  <td>
                    <Select label="Slot" value={row.slotType}
                      onChange={setRow(index, 'slotType')} options={SLOT_TYPES} />
                  </td>
                  {/* NOT GATED ON slotType: a BREAK carrying a teacher is
                      SLOT_FIELDS_NOT_ALLOWED, and that refusal is worth triggering. */}
                  <td><Input value={row.subjectCode} onChange={setRow(index, 'subjectCode')}
                    placeholder="MATHEMATICS" /></td>
                  <td><Input value={row.teacherDocsId} onChange={setRow(index, 'teacherDocsId')}
                    placeholder="from Create Staff" /></td>
                  <td><Input value={row.slotLabel} onChange={setRow(index, 'slotLabel')}
                    placeholder="Lunch Break" /></td>
                  {/* TIME PICKERS. A browser sends HH:MM and the API stores 09:00:00 either
                      way — measured, because a picker that emitted a format the endpoint
                      refused would break the form rather than help it. Equal times and an
                      inverted pair are both still selectable, so INVALID_PERIOD_TIMES stays
                      reachable. */}
                  <td><Input type="time" value={row.startTime}
                    onChange={setRow(index, 'startTime')} /></td>
                  <td><Input type="time" value={row.endTime}
                    onChange={setRow(index, 'endTime')} /></td>
                  <td><Input value={row.facilityResourceDocsId}
                    onChange={setRow(index, 'facilityResourceDocsId')} placeholder="optional" /></td>
                  <td>
                    <Button icon={Trash2}
                      onClick={() => setRows((c) => c.filter((_, i) => i !== index))}>
                      Remove
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <p className="muted">
          <Info size={12} /> <b>A subject must be one that section actually studies.</b> One created
          without a <span className="mono">sectionNo</span> is class-wide and every section takes
          it; one created with a <span className="mono">sectionNo</span> belongs to that section
          alone. Sending German to a section that does not take it is{' '}
          <span className="mono">409 SUBJECT_NOT_IN_SECTION</span>.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Back-to-back periods do not clash.</b> 09:00–09:45 beside
          09:45–10:30 is a normal day — the opposite call from a grade band, whose bounds are
          inclusive and whose neighbours must not touch.
        </p>
      </Card>

      {result ? (
        <Card
          title="What came back"
          action={
            <Badge tone={result.ok ? 'good' : undefined}>{result.status}</Badge>
          }
        >
          {created ? (
            <>
              <div className="btn-row">
                <Badge tone="good">{created.createdCount} written</Badge>
                <Badge>{created.entriesPerDay} periods a day</Badge>
                <Badge tone={created.skippedDates?.length ? 'brand' : undefined}>
                  {created.skippedDates?.length ?? 0} skipped
                </Badge>
              </div>
              <div className="resp">
                <div className="resp-head">
                  <span className="resp-status" data-ok="true">written</span>
                </div>
                <pre className="resp-body">{(created.createdDates ?? []).join('\n')}</pre>
              </div>
              {created.skippedDates?.length ? (
                <div className="resp">
                  <div className="resp-head">
                    <span className="resp-status" data-ok="false">skipped</span>
                    <span className="muted">a holiday is skipped, never refused</span>
                  </div>
                  <pre className="resp-body">
                    {created.skippedDates
                      .map((s) => `${s.date}  ${s.reason}  ${s.holidayName ?? ''}`)
                      .join('\n')}
                  </pre>
                </div>
              ) : null}
              {/* PRESENT ONLY WHEN ONE DATE WAS WRITTEN. A range returns a summary, because a
                  fortnight of a 400-period school is 4,000 entries the caller already holds. */}
              {created.timetable ? (
                <div className="resp">
                  <div className="resp-head">
                    <span className="resp-status" data-ok="true">
                      {created.timetable.date}
                    </span>
                    <span className="muted">
                      {created.timetable.academicYear} · derived from the date
                    </span>
                  </div>
                  <pre className="resp-body">
                    {(created.timetable.entries ?? [])
                      .map((e) => `${e.periodCode}  ${e.sectionNo}  ${e.slotType}  `
                        + `${e.subjectCode ?? e.slotLabel ?? ''}  ${e.startTime}-${e.endTime}  `
                        + `${e.timetableEntryId}`)
                      .join('\n')}
                  </pre>
                </div>
              ) : (
                <p className="muted">
                  <Info size={12} /> A range returns a summary rather than every period of every
                  day — the day itself comes back only when exactly one date was written.
                </p>
              )}
            </>
          ) : (
            <Empty
              title={result.bodyJson?.code || `The server answered ${result.status}`}
              description={result.bodyJson?.message || 'Nothing came back.'}
            />
          )}
        </Card>
      ) : null}

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> Anybody who can reach the
          API with a school subdomain can rewrite a school&apos;s day.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Only #1 exists.</b> There is no way yet to read a day back, correct
          one period, or find who is free to cover it — #4 and #7 are the next two, and #4 is the
          write this module exists for.
        </p>
        <p className="muted">
          <Info size={12} /> <b>A room clash outside the timetable is not detected.</b> A period in
          the lab and an approved resource booking of the lab at the same hour are two collections
          that do not consult each other; open item 3 of the plan says whose job that is.
        </p>
      </Card>
    </div>
  )
}
