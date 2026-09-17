import { useState } from 'react'
import { AlertTriangle, Info, Plus, RotateCcw, Trash2 } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'

const SLOT_TYPES = ['LESSON', 'BREAK', 'ASSEMBLY', 'ACTIVITY']

const BLANK = {
  timetableEntryId: '',
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

/** One stored period as a row of this editor — every field a string, so nothing is coerced. */
const rowOf = (entry) => ({
  timetableEntryId: entry.timetableEntryId ?? '',
  periodCode: entry.periodCode ?? '',
  classDocsId: entry.classDocsId ?? '',
  sectionNo: entry.sectionNo ?? '',
  slotType: entry.slotType ?? 'LESSON',
  subjectCode: entry.subjectCode ?? '',
  teacherDocsId: entry.teacherDocsId ?? '',
  slotLabel: entry.slotLabel ?? '',
  startTime: entry.startTime ?? '',
  endTime: entry.endTime ?? '',
  facilityResourceDocsId: entry.facilityResourceDocsId ?? '',
})

/**
 * Replacing a day: the #2 half of /school-academics/timetable/:date
 *
 * ONE ENDPOINT — #2, the only full-document write this module has, and the one its own plan calls
 * "a footgun with an audit trail". A period left out of the list is GONE.
 *
 * PREFILLED FROM THE DAY #7 JUST READ, ids and all, because that is the only way a replace is
 * survivable: a period sent back with its id keeps its identity, and the attendance pointing at it
 * still points at it. Reset puts the form back to what the server has.
 *
 * THE ENTRY ID IS AN EDITABLE TEXT BOX, deliberately. Clearing one turns that period into a new
 * period — which is a real thing to do and also how TIMETABLE_ENTRY_NOT_FOUND and
 * DUPLICATE_TIMETABLE_ENTRY_ID get sent on purpose. The form never manages ids for you.
 *
 * THE VERSION IS EDITABLE TOO. It is prefilled with the one that was read, so the common call
 * works; typing an old number is how CONCURRENT_MODIFICATION — the refusal this whole endpoint
 * rests on — gets triggered without needing a second browser.
 *
 * WHAT IS ABOUT TO BE LOST IS SHOWN BEFORE THE WRITE, worked out the same way the server does it:
 * a stored id that no row carries any more. The server's own answer is what is rendered afterwards,
 * because the form's arithmetic is a courtesy and the response is the fact.
 *
 * NOTHING IS DISABLED. Every refusal above must be reachable by hand.
 */
export default function TimetableReplace({ day, date, onReplaced }) {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  //! PREFILLED AT MOUNT, not synchronised in an effect. The parent gives this component a `key`
  //! carrying the day's id and version, so a replace elsewhere — or this form's own successful
  //! write — remounts it against the truth instead of leaving a draft that would be sent at a
  //! version that has moved. Copying props into state in an effect would do the same thing one
  //! render later, and wrongly.
  const [rows, setRows] = useState(() => (day?.entries ?? []).map(rowOf))
  const [version, setVersion] = useState(() =>
    (day?.version === null || day?.version === undefined ? '' : String(day.version)))
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  //! An event handler, not an effect: this runs because somebody pressed Reset.
  const reset = () => {
    setRows((day?.entries ?? []).map(rowOf))
    setVersion(day?.version === null || day?.version === undefined ? '' : String(day.version))
    setResult(null)
  }

  const setRow = (index, field) => (value) =>
    setRows((current) => current.map((row, i) =>
      (i === index ? { ...row, [field]: typeof value === 'string' ? value : value.target.value }
        : row)))

  //! ONLY THE FIELDS THAT CARRY SOMETHING. An empty string is not a value: the API treats a blank
  //! subjectCode as absent, and sending "" everywhere would make a BREAK look like it carries a
  //! subject. The entry id is the same — blank means "new", which is exactly what it should mean.
  const body = () => ({
    version: version.trim() === '' ? null : Number(version),
    entries: rows.map((row) => {
      const entry = {
        periodCode: row.periodCode,
        classDocsId: row.classDocsId,
        sectionNo: row.sectionNo,
        slotType: row.slotType,
        startTime: row.startTime,
        endTime: row.endTime,
      }
      if (row.timetableEntryId.trim()) entry.timetableEntryId = row.timetableEntryId.trim()
      if (row.subjectCode.trim()) entry.subjectCode = row.subjectCode.trim()
      if (row.teacherDocsId.trim()) entry.teacherDocsId = row.teacherDocsId.trim()
      if (row.slotLabel.trim()) entry.slotLabel = row.slotLabel.trim()
      if (row.facilityResourceDocsId.trim()) {
        entry.facilityResourceDocsId = row.facilityResourceDocsId.trim()
      }
      return entry
    }),
  })

  //! WHAT THIS DRAFT WOULD DESTROY, worked out the way the server does: a stored id that no row
  //! carries any more. Shown before the button rather than after the write, because afterwards it
  //! is already gone — and this is the one number on the page worth reading twice.
  const sentIds = new Set(rows.map((row) => row.timetableEntryId.trim()).filter(Boolean))
  const wouldRemove = (day?.entries ?? [])
    .map((entry) => entry.timetableEntryId)
    .filter((id) => !sentIds.has(id))

  const submit = async () => {
    setSending(true)
    const answer = await call('replace-timetable', {
      label: 'Replace a day',
      pathParams: { year: actingAcademicYear ?? '', date: date ?? '' },
      body: body(),
    })
    setSending(false)
    setResult(answer)
    //! A SUCCESSFUL REPLACE MOVES THE VERSION, so the page behind this has to re-read. Without it
    //! the next replace would be sent against a version that no longer exists — from this very
    //! form, which is the mistake the endpoint is built to catch.
    if (answer.ok && onReplaced) onReplaced()
  }

  if (!actingSubdomain) return null

  const answered = result?.ok ? result.bodyJson : null

  return (
    <>
      <Card
        title="Replace this day"
        description="#2 — the only full-document write. Every period is sent; one left out is gone. Send an id back to keep that period's identity, clear it to make it a new period."
        action={
          <div className="btn-row">
            <EndpointTag id="replace-timetable" name="Replace"
              pathParams={{ year: actingAcademicYear, date }} />
            <Badge>{rows.length} periods</Badge>
            <Button icon={RotateCcw} onClick={reset}>Reset from the day</Button>
            <Button icon={Plus} onClick={() => setRows((c) => [...c, { ...BLANK }])}>
              Add a period
            </Button>
            <Button look="primary" busy={sending} onClick={submit}>Replace the day</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field
            label="Version"
            hint="Required. Prefilled with the version that was read. Type an older number to trigger CONCURRENT_MODIFICATION on purpose."
          >
            <Input value={version} onChange={(e) => setVersion(e.target.value)}
              placeholder="from #7" />
          </Field>
        </div>

        {/* THE ONE NUMBER WORTH READING TWICE, and it is shown BEFORE the button. */}
        {wouldRemove.length > 0 ? (
          <p className="muted">
            <AlertTriangle size={12} /> <b>{wouldRemove.length} period(s) would be removed</b> —
            no row carries{' '}
            {wouldRemove.map((id) => <span key={id} className="mono">{id} </span>)}
            any more. An attendance session naming one of them would be left pointing at nothing.
          </p>
        ) : null}

        {rows.length === 0 ? (
          <Empty
            title="No periods in the draft"
            description="Sending this is 400 — a day cannot be emptied through #2, deliberately. Add a period, or reset from the day."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Entry id</th>
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
                    {/* EDITABLE ON PURPOSE. Clear it and the period becomes a new one; paste
                        another day's id and the answer is TIMETABLE_ENTRY_NOT_FOUND, which is
                        the check that stops one id living on two dates. */}
                    <td><Input value={row.timetableEntryId}
                      onChange={setRow(index, 'timetableEntryId')} placeholder="blank = new" /></td>
                    <td><Input value={row.periodCode} onChange={setRow(index, 'periodCode')}
                      placeholder="P1" /></td>
                    <td><Input value={row.classDocsId} onChange={setRow(index, 'classDocsId')} /></td>
                    <td><Input value={row.sectionNo} onChange={setRow(index, 'sectionNo')} /></td>
                    <td>
                      <Select label="Slot" value={row.slotType}
                        onChange={setRow(index, 'slotType')} options={SLOT_TYPES} />
                    </td>
                    {/* NOT GATED ON slotType: a BREAK carrying a subject is
                        SLOT_FIELDS_NOT_ALLOWED, and that refusal is worth triggering. */}
                    <td><Input value={row.subjectCode} onChange={setRow(index, 'subjectCode')} /></td>
                    <td><Input value={row.teacherDocsId}
                      onChange={setRow(index, 'teacherDocsId')} /></td>
                    <td><Input value={row.slotLabel} onChange={setRow(index, 'slotLabel')} /></td>
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
        )}

        <p className="muted">
          <Info size={12} /> <b>The version is what stands between two clerks.</b> A replace
          overwrites periods the caller may never have seen, so it is the one write here that
          refuses without it — <span className="mono">409 CONCURRENT_MODIFICATION</span>, naming
          both versions.
        </p>
        <p className="muted">
          <Info size={12} /> <b>An id that is not this day&apos;s is refused</b>, including a real
          id belonging to another date. One id on two days would make an attendance session&apos;s
          <span className="mono"> timetableEntryId</span> ambiguous, which is the single thing
          generated ids exist to prevent.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Reach for #4 instead, when it exists.</b> Correcting one period is
          a substitution, not a rewrite of the day — this endpoint is the blunt instrument, and its
          own plan says so.
        </p>
      </Card>

      {result ? (
        <Card
          title={answered ? 'Replaced' : (result.bodyJson?.code ?? `The server answered ${result.status}`)}
          description={answered
            ? 'What the day is now, and what it cost. The removed ids are named rather than counted — they are the closest thing this endpoint has to an undo.'
            : 'The refusal exactly as it came. Nothing on this page predicted it.'}
          action={<Badge>{result.status}</Badge>}
        >
          {answered ? (
            <>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Version</th><th>Kept</th><th>Added</th><th>Removed</th>
                      <th>Periods now</th></tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><b>{answered.version}</b></td>
                      <td>{answered.keptCount}</td>
                      <td>{answered.addedCount}</td>
                      <td>{(answered.removedEntryIds ?? []).length}</td>
                      <td>{answered.timetable?.entryCount}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              {(answered.removedEntryIds ?? []).length > 0 ? (
                <p className="muted">
                  <AlertTriangle size={12} /> <b>Removed:</b>{' '}
                  {answered.removedEntryIds.map((id) => (
                    <span key={id} className="mono">{id} </span>
                  ))}
                </p>
              ) : null}
              <p className="muted">{answered.nextStep}</p>
            </>
          ) : (
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="false">
                  {result.bodyJson?.code ?? result.status}
                </span>
              </div>
              <p>{result.bodyJson?.message ?? 'Nothing came back.'}</p>
            </div>
          )}
        </Card>
      ) : null}
    </>
  )
}
