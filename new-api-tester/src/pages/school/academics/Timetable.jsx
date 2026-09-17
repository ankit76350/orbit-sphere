import { useCallback, useEffect, useState } from 'react'
import { Grid3x3, Info, Plus, RefreshCw, Rows3, ShieldAlert, SlidersHorizontal, Trash2 }
  from 'lucide-react'
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

const BLANK_GRID_ROW = {
  periodCode: '',
  slotType: 'LESSON',
  startTime: '',
  endTime: '',
  slotLabel: '',
}

const cellKey = (classDocsId, sectionNo) => `${classDocsId}|${sectionNo}`

/** A custom cell carries its own everything — see the custom card's note. */
const BLANK_CUSTOM_CELL = {
  periodCode: '',
  slotType: 'LESSON',
  subjectCode: '',
  teacherDocsId: '',
  slotLabel: '',
  startTime: '',
  endTime: '',
  facilityResourceDocsId: '',
}

/**
 * Whether two windows share a minute. END-EXCLUSIVE, the same call the server makes: 09:00-09:45
 * beside 09:45-10:30 is a normal day, not a clash.
 */
const overlaps = (aFrom, aTo, bFrom, bTo) => aFrom < bTo && bFrom < aTo

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
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [rows, setRows] = useState([{ ...BLANK_ROW }])
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  //! THE GRID IS THE POINT, and the raw rows stay for what it cannot express. A dropdown of a
  //! section's own subjects is exactly the rule the API enforces, so the grid can never send
  //! SUBJECT_NOT_IN_SECTION by accident — and this is an API tester, where being unable to send
  //! a refusal is a defect. The raw table is how that case stays reachable.
  const [mode, setMode] = useState('grid')

  //! The school's own structure, which is what the columns ARE. Loaded rather than typed: a
  //! grid asking for a classDocsId in every cell would be the flat table with more steps.
  const [structure, setStructure] = useState([])
  const [teachers, setTeachers] = useState([])
  const [loadingStructure, setLoadingStructure] = useState(false)

  //! One row per period, one cell per class-section. Keyed by class and section rather than by
  //! position, so adding a section does not silently shift every period one column across.
  const [gridRows, setGridRows] = useState([{ ...BLANK_GRID_ROW, cells: {} }])

  //! THE THIRD MODE, and the one the grid cannot express: every slot carries its OWN type and
  //! its OWN times. A double period in 10-A while 10-B runs two singles across the same two
  //! hours is a real timetable and an impossible row — the grid's shared bell schedule forces
  //! every section onto one rhythm, which most schools only mostly follow.
  //!
  //! KEYED BY SECTION, NOT BY ROW. Each column owns its own list, so one section can have five
  //! slots and its neighbour eight — which is the whole point, and which a grid of rows cannot
  //! say at all: a row there forces every column to have something at that position.
  const [customSlots, setCustomSlots] = useState({})

  //! #10 IS ITS OWN READ, and deliberately separate from the builder above: it answers "what has
  //! this year got already", which is a different question from "what am I about to write".
  const [days, setDays] = useState(null)
  const [dayFilters, setDayFilters] = useState({ from: '', to: '', teacherDocsId: '' })
  const [dayPage, setDayPage] = useState(0)
  const [loadingDays, setLoadingDays] = useState(false)

  const loadDays = useCallback(async () => {
    if (!actingSubdomain || !actingAcademicYear) return
    setLoadingDays(true)
    const query = { page: String(dayPage), size: '20' }
    if (dayFilters.from) query.from = dayFilters.from
    if (dayFilters.to) query.to = dayFilters.to
    if (dayFilters.teacherDocsId) query.teacherDocsId = dayFilters.teacherDocsId
    const answer = await call('list-timetables', {
      label: "The year's days",
      pathParams: { year: actingAcademicYear },
      query,
    })
    setLoadingDays(false)
    setDays(answer.ok ? answer.bodyJson : null)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, actingAcademicYear, dayFilters, dayPage])

  useEffect(() => { loadDays() }, [loadDays])

  //! THE COLUMNS ARE THE SCHOOL'S OWN STRUCTURE. One read for the classes, then one per class
  //! for its sections and one for its subjects — the subject list comes back WITHOUT ?sectionNo=
  //! so each row carries its own, and which sections may take it is worked out here with the
  //! same rule the API applies: no sectionNo means class-wide.
  const loadStructure = useCallback(async () => {
    if (!actingSubdomain || !actingAcademicYear) return
    setLoadingStructure(true)

    const list = await call('list-school-classes', {
      label: 'The classes this grid is built from',
      pathParams: { year: actingAcademicYear },
      query: { size: '100' },
    })

    const built = []
    for (const one of list.bodyJson?.content ?? []) {
      const sections = await call('list-class-sections', {
        label: `Sections of ${one.name}`,
        pathParams: { year: actingAcademicYear, id: one.schoolClassId },
      })
      const subjects = await call('list-class-subjects', {
        label: `Subjects of ${one.name}`,
        pathParams: { year: actingAcademicYear, id: one.schoolClassId },
      })
      built.push({
        schoolClassId: one.schoolClassId,
        name: one.name,
        sections: (sections.bodyJson?.sections ?? []).filter((x) => x.active),
        subjects: subjects.bodyJson?.subjects ?? [],
      })
    }

    const people = await call('list-staff', {
      label: 'Who can be given a period',
      query: { size: '100' },
    })

    setStructure(built)
    setTeachers(people.bodyJson?.content ?? [])
    setLoadingStructure(false)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, actingAcademicYear])

  useEffect(() => { loadStructure() }, [loadStructure])

  //! Every class-section pair, flattened, in the order the classes came back. `firstOfClass`
  //! is what draws the heavier rule between one class and the next.
  const columns = structure.flatMap((k) =>
    k.sections.map((sec, i) => ({
      classDocsId: k.schoolClassId,
      className: k.name,
      sectionNo: sec.sectionNo,
      firstOfClass: i === 0,
      //! THE SAME RULE THE API ENFORCES: a subject with no sectionNo is class-wide, one with a
      //! sectionNo belongs to that section alone. Every subject of the class is offered, and the
      //! ones this section does NOT study are labelled — so SUBJECT_NOT_IN_SECTION stays a
      //! refusal a tester can trigger on purpose rather than one the dropdown hides.
      subjects: k.subjects.filter((sub) => sub.active).map((sub) => {
        const classWide = !sub.sectionNo
        const mine = classWide || sub.sectionNo === sec.sectionNo
        return {
          value: sub.subjectCode,
          label: mine ? sub.name : `${sub.name} — not in ${sec.sectionNo}`,
        }
      }),
    })))

  const setGridRow = (index, field) => (event) => {
    const value = event?.target ? event.target.value : event
    setGridRows((c) => c.map((r, i) => (i === index ? { ...r, [field]: value } : r)))
  }

  const slotsOf = (key) => customSlots[key] ?? []

  const addSlot = (key) => setCustomSlots((c) => ({
    ...c, [key]: [...(c[key] ?? []), { ...BLANK_CUSTOM_CELL }],
  }))

  const removeSlot = (key, index) => setCustomSlots((c) => ({
    ...c, [key]: (c[key] ?? []).filter((_, i) => i !== index),
  }))

  const setSlot = (key, index, field) => (event) => {
    const value = event?.target ? event.target.value : event
    setCustomSlots((c) => ({
      ...c,
      [key]: (c[key] ?? []).map((slot, i) => (i === index ? { ...slot, [field]: value } : slot)),
    }))
  }

  //! A CUSTOM CELL BECOMES AN ENTRY once it has a code and both times — the three the API
  //! requires of every period whatever its slot type. Anything less is an empty cell rather
  //! than a broken one, which is what lets a section simply not have a slot the others do.
  const customEntries = useCallback(() => {
    const out = []
    for (const col of columns) {
      for (const cell of customSlots[cellKey(col.classDocsId, col.sectionNo)] ?? []) {
        if (cell.periodCode.trim() === '' || !cell.startTime || !cell.endTime) continue

        const entry = {
          periodCode: cell.periodCode.trim(),
          classDocsId: col.classDocsId,
          sectionNo: col.sectionNo,
          slotType: cell.slotType,
          startTime: cell.startTime,
          endTime: cell.endTime,
        }
        if (cell.subjectCode !== '') entry.subjectCode = cell.subjectCode
        if (cell.teacherDocsId !== '') entry.teacherDocsId = cell.teacherDocsId
        if (cell.slotLabel.trim() !== '') entry.slotLabel = cell.slotLabel.trim()
        if (cell.facilityResourceDocsId.trim() !== '') {
          entry.facilityResourceDocsId = cell.facilityResourceDocsId.trim()
        }
        out.push(entry)
      }
    }
    return out
  }, [customSlots, columns])

  //! THE SAME CLASH THE GRID MARKS, but here it needs real time arithmetic: two cells collide
  //! when their windows overlap, not merely when they sit in the same row. This is the mode
  //! where that is hardest to see by eye, which is why it is worth computing.
  const customClashes = useCallback(() => {
    const byTeacher = new Map()
    const clashing = new Set()

    for (const col of columns) {
      const key = cellKey(col.classDocsId, col.sectionNo)
      for (const [index, cell] of (customSlots[key] ?? []).entries()) {
        if (!cell.teacherDocsId || !cell.startTime || !cell.endTime) continue

        const mine = { id: `${key}|${index}`, from: cell.startTime, to: cell.endTime }
        const others = byTeacher.get(cell.teacherDocsId) ?? []
        for (const other of others) {
          if (overlaps(mine.from, mine.to, other.from, other.to)) {
            clashing.add(mine.id)
            clashing.add(other.id)
          }
        }
        others.push(mine)
        byTeacher.set(cell.teacherDocsId, others)
      }
    }
    return clashing
  }, [customSlots, columns])

  const setCell = (index, key, field) => (event) => {
    const value = event?.target ? event.target.value : event
    setGridRows((c) => c.map((r, i) => (i === index
      ? { ...r, cells: { ...r.cells, [key]: { ...(r.cells[key] ?? {}), [field]: value } } }
      : r)))
  }

  //! WHAT THE GRID MEANS, as entries. A non-lesson row is one thing happening everywhere, so it
  //! becomes an entry for every column; a lesson cell becomes one only when a subject was
  //! chosen, because an empty cell is a free period rather than an incomplete one.
  const gridEntries = useCallback(() => {
    const out = []
    for (const row of gridRows) {
      for (const col of columns) {
        const base = {
          periodCode: row.periodCode,
          classDocsId: col.classDocsId,
          sectionNo: col.sectionNo,
          slotType: row.slotType,
          startTime: row.startTime,
          endTime: row.endTime,
        }
        if (row.slotType !== 'LESSON') {
          if (row.slotLabel.trim() !== '') base.slotLabel = row.slotLabel.trim()
          out.push(base)
          continue
        }
        const cell = row.cells[cellKey(col.classDocsId, col.sectionNo)] ?? {}
        if (!cell.subjectCode) continue
        base.subjectCode = cell.subjectCode
        if ((cell.teacherDocsId ?? '') !== '') base.teacherDocsId = cell.teacherDocsId
        if ((cell.facilityResourceDocsId ?? '').trim() !== '') {
          base.facilityResourceDocsId = cell.facilityResourceDocsId.trim()
        }
        out.push(base)
      }
    }
    return out
  }, [gridRows, columns])

  //! A WIDE ROW MAKES ONE MISTAKE VERY EASY: giving the same teacher two sections in the same
  //! slot. The API refuses it — TEACHER_PERIOD_OVERLAP — but only after the whole grid has been
  //! filled in, and with four hundred cells the two that collide are hard to find.
  //!
  //! SHOWN, NEVER BLOCKED. The cell stays selectable and the request stays sendable; this only
  //! names what the refusal is going to say. Gating it would make the refusal untriggerable,
  //! which in an API tester is the defect rather than the fix.
  const clashingCells = useCallback(() => {
    const clashes = new Set()
    for (const [index, row] of gridRows.entries()) {
      if (row.slotType !== 'LESSON') continue
      const seen = new Map()
      for (const col of columns) {
        const key = cellKey(col.classDocsId, col.sectionNo)
        const teacher = row.cells[key]?.teacherDocsId
        const subject = row.cells[key]?.subjectCode
        if (!teacher || !subject) continue
        if (seen.has(teacher)) {
          clashes.add(`${index}|${key}`)
          clashes.add(`${index}|${seen.get(teacher)}`)
        } else {
          seen.set(teacher, key)
        }
      }
    }
    return clashes
  }, [gridRows, columns])

  const setRow = (index, field) => (event) => {
    const value = event?.target ? event.target.value : event
    setRows((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  //! BLANK MEANS NOT SENT. An empty string on the wire is a different thing from an absent field —
  //! the API treats "" as absent for the optional ones, and sending it anyway would test a
  //! normalisation rule rather than the rule the tester is looking at.
  const body = useCallback(() => {
    if (mode === 'grid' || mode === 'custom') {
      const out = { startDate, entries: mode === 'grid' ? gridEntries() : customEntries() }
      if (endDate.trim() !== '') out.endDate = endDate.trim()
      return out
    }
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
  }, [startDate, endDate, rows, mode, gridEntries, customEntries])

  const submit = useCallback(async () => {
    if (!actingSubdomain) return
    setSending(true)
    //! THE YEAR COMES FROM THE PICKER IN THE HEADER, not from the dates. That is the whole of
    //! the 2026-09-17 rework: a school holding two years whose ranges both covered a September
    //! date had its timetable written into the one it had not chosen.
    const answer = await call('create-timetable', {
      label: 'Build a day',
      pathParams: { year: actingAcademicYear ?? '' },
      body: body(),
    })
    setSending(false)
    setResult(answer)
  }, [call, actingSubdomain, actingAcademicYear, body])

  if (!actingSubdomain) return <NoSchoolChosen what="The timetable" />

  const created = result?.ok ? result.bodyJson : null

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Timetable</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · where every child is meant to be,
            hour by hour · written into{' '}
            <span className="mono">{actingAcademicYear ?? 'no year chosen'}</span>, the year picked
            above
          </p>
        </div>
        <span className="toolbar-spacer" />
        {/* THREE WAYS TO SAY THE SAME REQUEST, and the switch is here rather than inside a card
            so that which one is active is never in doubt. They do not share state: switching
            back finds what was left behind. */}
        <div className="btn-row">
          <Button icon={Rows3} look={mode === 'raw' ? 'primary' : undefined}
            onClick={() => setMode('raw')}>Rows</Button>
          <Button icon={Grid3x3} look={mode === 'grid' ? 'primary' : undefined}
            onClick={() => setMode('grid')}>Grid</Button>
          <Button icon={SlidersHorizontal} look={mode === 'custom' ? 'primary' : undefined}
            onClick={() => setMode('custom')}>Custom</Button>
        </div>
        <EndpointTag id="create-timetable" name="Create"
          pathParams={{ year: actingAcademicYear }} />
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
        <p className="muted">
          <Info size={12} /> <b>Both dates must fall inside the year picked in the header.</b> The
          year is <em>stated</em>, not worked out from the date — until 2026-09-17 it was derived,
          and a school with two years covering the same month had its timetable written into the
          one it had not chosen. A date outside the chosen year is now{' '}
          <span className="mono">409 DATE_OUTSIDE_ACADEMIC_YEAR</span>, naming that year&apos;s own
          range.
        </p>
      </Card>

      {mode === 'grid' ? (
        <Card
          title="The day"
          description="Columns are the school's classes and their sections; rows are the slots of the day. A break or an assembly is one band across every column, the way a school draws it on paper."
          action={
            <div className="btn-row">
              <Badge>{columns.length} sections</Badge>
              <Badge tone={gridEntries().length ? 'brand' : undefined}>
                {gridEntries().length} periods
              </Badge>
              {clashingCells().size ? (
                <Badge>{clashingCells().size / 2} teacher clashes</Badge>
              ) : null}
              <Button icon={RefreshCw} onClick={loadStructure} busy={loadingStructure}>
                Reload structure
              </Button>
              <Button icon={Plus}
                onClick={() => setGridRows((c) => [...c, { ...BLANK_GRID_ROW, cells: {} }])}>
                Add a slot
              </Button>
            </div>
          }
        >
          {columns.length === 0 ? (
            <Empty
              title={actingAcademicYear ? 'No classes in this year' : 'No year chosen'}
              description={actingAcademicYear
                ? 'The columns are the school\'s own classes and sections. Create a class and a section first, then reload.'
                : 'Pick an academic year in the header — the grid is built from that year\'s classes.'}
              action={<Button icon={RefreshCw} onClick={loadStructure}>Reload structure</Button>}
            />
          ) : (
            <div className="table-scroll">
              <table className="data-table tt-grid">
                <thead>
                  <tr>
                    <th className="tt-row-head">Slot</th>
                    {/* THE CLASS BAND, spanning its own sections — the top row of both
                        reference layouts, and what tells one class from the next. */}
                    {structure.filter((k) => k.sections.length > 0).map((k) => (
                      <th key={k.schoolClassId} className="tt-class-head"
                        colSpan={k.sections.length}>
                        {k.name}
                      </th>
                    ))}
                  </tr>
                  <tr>
                    <th className="tt-row-head">code · time</th>
                    {columns.map((col) => (
                      <th key={cellKey(col.classDocsId, col.sectionNo)}
                        className={`tt-section-head${col.firstOfClass ? ' tt-class-start' : ''}`}>
                        {col.sectionNo}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {gridRows.map((row, index) => (
                    // eslint-disable-next-line react/no-array-index-key
                    <tr key={index}
                      className={row.slotType === 'LESSON' ? undefined : 'tt-break-row'}>
                      {/* THE PERIOD'S OWN FIELDS, once for the row rather than once per cell:
                          a school's period 3 is period 3 for everybody, and repeating the
                          times in every column is how two of them drift apart. */}
                      <td className="tt-row-head">
                        <div className="tt-cell-stack">
                          <Input value={row.periodCode} onChange={setGridRow(index, 'periodCode')}
                            placeholder="P1" />
                          <Select label="Slot" value={row.slotType}
                            onChange={setGridRow(index, 'slotType')} options={SLOT_TYPES} />
                          <Input type="time" value={row.startTime}
                            onChange={setGridRow(index, 'startTime')} />
                          <Input type="time" value={row.endTime}
                            onChange={setGridRow(index, 'endTime')} />
                          <Button icon={Trash2}
                            onClick={() => setGridRows((c) => c.filter((_, i) => i !== index))}>
                            Remove
                          </Button>
                        </div>
                      </td>

                      {row.slotType !== 'LESSON' ? (
                        /* ONE BAND ACROSS THE WHOLE DAY. A break is the same event in every
                           section, so it is one cell rather than eight identical ones — and it
                           still becomes one entry per section on the wire. */
                        <td colSpan={columns.length}>
                          <Input value={row.slotLabel} onChange={setGridRow(index, 'slotLabel')}
                            placeholder="Lunch Break — the label every section gets" />
                        </td>
                      ) : columns.map((col) => {
                        const key = cellKey(col.classDocsId, col.sectionNo)
                        const cell = row.cells[key] ?? {}
                        const clashes = clashingCells().has(`${index}|${key}`)
                        return (
                          <td key={key}
                            className={`tt-cell${col.firstOfClass ? ' tt-class-start' : ''}`}>
                            <div className="tt-cell-stack">
                              {/* EVERY SUBJECT OF THE CLASS IS OFFERED, with the ones this
                                  section does not study labelled rather than hidden — so the
                                  rule is visible AND its refusal stays triggerable. */}
                              <Select label="Subject" value={cell.subjectCode ?? ''}
                                onChange={setCell(index, key, 'subjectCode')}
                                options={[{ value: '', label: '— free —' }, ...col.subjects]} />
                              <Select label="Teacher" value={cell.teacherDocsId ?? ''}
                                onChange={setCell(index, key, 'teacherDocsId')}
                                options={[{ value: '', label: '— no teacher —' },
                                  ...teachers.map((t) => ({
                                    value: t.staffDocsId,
                                    label: t.fullName,
                                  }))]} />
                              {/* NAMED, NOT BLOCKED — the cell stays usable and the request
                                  stays sendable. */}
                              {clashes ? (
                                <span className="muted">
                                  <ShieldAlert size={11} /> also teaching elsewhere this slot
                                </span>
                              ) : null}
                              <Input value={cell.facilityResourceDocsId ?? ''}
                                onChange={setCell(index, key, 'facilityResourceDocsId')}
                                placeholder="room id" />
                            </div>
                          </td>
                        )
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <p className="muted">
            <Info size={12} /> <b>An empty cell is a free period, not an incomplete one.</b> Only
            cells with a subject become entries; a slot a section does not have simply produces
            nothing for that column.
          </p>
          <p className="muted">
            <Info size={12} /> <b>A subject marked &ldquo;not in&nbsp;A&rdquo; is still
            selectable.</b> It belongs to another section of the same class, and picking it is how
            you trigger <span className="mono">409 SUBJECT_NOT_IN_SECTION</span> on purpose. The
            grid shows the rule; it does not enforce it.
          </p>
          <p className="muted">
            <Info size={12} /> <b>A teacher given two sections in one slot is marked, not
            blocked.</b> The API refuses it as{' '}
            <span className="mono">409 TEACHER_PERIOD_OVERLAP</span>, and with a few hundred cells
            the two that collide are hard to find afterwards — so the grid says which they are
            while leaving the request perfectly sendable.
          </p>
          <p className="muted">
            <Info size={12} /> <b>The grid cannot express everything.</b> A subject the class does
            not hold at all, a malformed id, a period code sent twice — those need{' '}
            <b>Raw rows</b>, which is the same request with nothing filled in for you.
          </p>
        </Card>
      ) : null}

      {mode === 'custom' ? (
        <Card
          title="The day, cell by cell"
          description="Every slot carries its own times, type, subject, teacher, label and room, and each section keeps its own list — add one at the foot of the section it belongs to."
          action={
            <div className="btn-row">
              <Badge>{columns.length} sections</Badge>
              <Badge tone={customEntries().length ? 'brand' : undefined}>
                {customEntries().length} periods
              </Badge>
              {customClashes().size ? (
                <Badge>{customClashes().size / 2} teacher clashes</Badge>
              ) : null}
              <Button icon={RefreshCw} onClick={loadStructure} busy={loadingStructure}>
                Reload structure
              </Button>
            </div>
          }
        >
          {columns.length === 0 ? (
            <Empty
              title={actingAcademicYear ? 'No classes in this year' : 'No year chosen'}
              description={actingAcademicYear
                ? 'The columns are the school\'s own classes and sections. Create a class and a section first, then reload.'
                : 'Pick an academic year in the header.'}
              action={<Button icon={RefreshCw} onClick={loadStructure}>Reload structure</Button>}
            />
          ) : (
            <div className="table-scroll">
              <table className="data-table tt-grid">
                <thead>
                  <tr>
                    <th className="tt-row-head">Slot</th>
                    {structure.filter((k) => k.sections.length > 0).map((k) => (
                      <th key={k.schoolClassId} className="tt-class-head"
                        colSpan={k.sections.length}>
                        {k.name}
                      </th>
                    ))}
                  </tr>
                  <tr>
                    <th className="tt-row-head">each section keeps its own</th>
                    {columns.map((col) => (
                      <th key={cellKey(col.classDocsId, col.sectionNo)}
                        className={`tt-section-head${col.firstOfClass ? ' tt-class-start' : ''}`}>
                        {col.sectionNo}
                        <div className="muted">
                          {slotsOf(cellKey(col.classDocsId, col.sectionNo)).length} slots
                        </div>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {/* AS MANY ROWS AS THE LONGEST COLUMN NEEDS. A row is not a shared slot here —
                      it is only where the nth slot of each section happens to be drawn, and a
                      column that has fewer simply stops. */}
                  {Array.from({ length: Math.max(0, ...columns.map(
                    (col) => slotsOf(cellKey(col.classDocsId, col.sectionNo)).length)) })
                    .map((_, index) => (
                      // eslint-disable-next-line react/no-array-index-key
                      <tr key={index}>
                        <td className="tt-row-head"><span className="muted">{index + 1}</span></td>
                        {columns.map((col) => {
                          const key = cellKey(col.classDocsId, col.sectionNo)
                          const cell = slotsOf(key)[index]
                          if (!cell) {
                            //! THIS SECTION HAS FEWER SLOTS, which is the whole reason this mode
                            //! exists. An empty cell here is not a gap to fill in.
                            return (
                              <td key={key}
                                className={`tt-cell${col.firstOfClass ? ' tt-class-start' : ''}`} />
                            )
                          }
                          const clashes = customClashes().has(`${key}|${index}`)
                          return (
                            <td key={key}
                              className={`tt-cell${col.firstOfClass ? ' tt-class-start' : ''}`}>
                              <div className="tt-cell-stack">
                                <Input value={cell.periodCode}
                                  onChange={setSlot(key, index, 'periodCode')}
                                  placeholder="P1" />
                                <div className="tt-cell-pair">
                                  <Input type="time" value={cell.startTime}
                                    onChange={setSlot(key, index, 'startTime')} />
                                  <Input type="time" value={cell.endTime}
                                    onChange={setSlot(key, index, 'endTime')} />
                                </div>
                                <Select label="Slot" value={cell.slotType}
                                  onChange={setSlot(key, index, 'slotType')}
                                  options={SLOT_TYPES} />
                                <Select label="Subject" value={cell.subjectCode}
                                  onChange={setSlot(key, index, 'subjectCode')}
                                  options={[{ value: '', label: '— no subject —' },
                                    ...col.subjects]} />
                                <Select label="Teacher" value={cell.teacherDocsId}
                                  onChange={setSlot(key, index, 'teacherDocsId')}
                                  options={[{ value: '', label: '— no teacher —' },
                                    ...teachers.map((t) => ({
                                      value: t.staffDocsId, label: t.fullName,
                                    }))]} />
                                {clashes ? (
                                  <span className="muted">
                                    <ShieldAlert size={11} /> this teacher overlaps another slot
                                  </span>
                                ) : null}
                                <Input value={cell.slotLabel}
                                  onChange={setSlot(key, index, 'slotLabel')}
                                  placeholder="label — for a break" />
                                <Input value={cell.facilityResourceDocsId}
                                  onChange={setSlot(key, index, 'facilityResourceDocsId')}
                                  placeholder="room id" />
                                {/* REMOVED ONE AT A TIME, from the slot itself. Taking one out
                                    of 10-B does not disturb 10-A, because the two lists are
                                    unrelated. */}
                                <Button icon={Trash2} onClick={() => removeSlot(key, index)}>
                                  Remove this slot
                                </Button>
                              </div>
                            </td>
                          )
                        })}
                      </tr>
                    ))}

                  {/* THE FOOT OF EACH SECTION, which is where a slot is added. There is no
                      "add a row" here on purpose: a row would put an empty slot in every
                      section, and the sections do not share a schedule. */}
                  <tr>
                    <td className="tt-row-head"><span className="muted">add</span></td>
                    {columns.map((col) => {
                      const key = cellKey(col.classDocsId, col.sectionNo)
                      return (
                        <td key={key}
                          className={`tt-cell${col.firstOfClass ? ' tt-class-start' : ''}`}>
                          <Button icon={Plus} onClick={() => addSlot(key)}>
                            Add a slot to {col.sectionNo}
                          </Button>
                        </td>
                      )
                    })}
                  </tr>
                </tbody>
              </table>
            </div>
          )}

          <p className="muted">
            <Info size={12} /> <b>This is the mode for a day that is not the same shape for
            everybody.</b> A double period in one section while another runs two singles across
            the same two hours is a real timetable and an impossible row — the grid puts every
            section on one bell schedule, which most schools only mostly follow.
          </p>
          <p className="muted">
            <Info size={12} /> <b>Slots are added and removed per section, at the foot of that
            section&apos;s own column.</b> There is no &ldquo;add a row&rdquo;: a row would put an
            empty slot into every section at once, and these sections do not share a schedule —
            one may end up with five slots and its neighbour eight.
          </p>
          <p className="muted">
            <Info size={12} /> <b>A slot becomes a period once it has a code and both times.</b>{' '}
            Those are the three the API requires of every period whatever its type. Anything less
            is left out of the request entirely.
          </p>
          <p className="muted">
            <Info size={12} /> <b>The clash check here compares real time windows</b>, not row
            positions — two cells collide when their minutes overlap. Marked, never blocked: the
            request stays sendable so{' '}
            <span className="mono">409 TEACHER_PERIOD_OVERLAP</span> can still be triggered.
          </p>
        </Card>
      ) : null}

      {mode === 'raw' ? (
      <Card
        title={`The periods · ${rows.length}`}
        description="Raw rows — every field typed by hand, applied to every date in the range. Nothing here is validated by the form, which is the point: this is how a refusal the grid cannot produce gets sent."
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
      ) : null}

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

      {/* #10 — WHAT THE YEAR ALREADY HAS. Counts, not periods: a full day is about 120 KB and a
          page of twenty carrying its periods would be megabytes to render twenty dates. */}
      <Card
        title="Days already written"
        description="From #10 — one row per date, as counts. The periods stay in the database; opening a day is #7, which is not built yet."
        action={
          <div className="btn-row">
            <EndpointTag id="list-timetables" name="List"
              pathParams={{ year: actingAcademicYear }} />
            <Badge>{days?.totalElements ?? 0} days</Badge>
            <Button icon={RefreshCw} onClick={loadDays} busy={loadingDays}>Reload</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="From" hint="Optional. Absent starts at the beginning of the year.">
            <Input type="date" value={dayFilters.from}
              onChange={(e) => { setDayPage(0); setDayFilters((f) => ({ ...f, from: e.target.value })) }} />
          </Field>
          <Field label="To" hint="Optional and independent of 'from' — either alone is meaningful.">
            <Input type="date" value={dayFilters.to}
              onChange={(e) => { setDayPage(0); setDayFilters((f) => ({ ...f, to: e.target.value })) }} />
          </Field>
          <Field label="Teacher" hint="One person's working days. A break they supervise counts.">
            <Select label="Teacher" value={dayFilters.teacherDocsId}
              onChange={(v) => { setDayPage(0); setDayFilters((f) => ({ ...f, teacherDocsId: v })) }}
              options={[{ value: '', label: '— anybody —' },
                ...teachers.map((t) => ({ value: t.staffDocsId, label: t.fullName }))]} />
          </Field>
        </div>

        {(days?.content ?? []).length === 0 ? (
          <Empty
            title="No day matches"
            description="An empty page, never a 404 — this year simply has no timetable written for those dates yet."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Periods</th>
                  <th>Lessons</th>
                  <th>Classes</th>
                  <th>Sections</th>
                  <th>Staff</th>
                  <th>Timetable id</th>
                </tr>
              </thead>
              <tbody>
                {days.content.map((row) => (
                  <tr key={row.dailyTimetableDocsId}>
                    <td>{row.date}</td>
                    <td><b>{row.entryCount}</b></td>
                    {/* LESSONS vs PERIODS: the difference is the breaks, assemblies and
                        activities, which is worth seeing at a glance. */}
                    <td>{row.lessonCount}
                      {row.entryCount > row.lessonCount
                        ? <span className="muted"> · {row.entryCount - row.lessonCount} other</span>
                        : null}</td>
                    <td>{row.classCount}</td>
                    {/* PAIRED WITH THE CLASS — "A" of one class and "A" of another are two. */}
                    <td>{row.sectionCount}</td>
                    <td>{row.teacherCount}</td>
                    <td><span className="muted mono">{row.dailyTimetableDocsId}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setDayPage((p) => p - 1)}>Previous</Button>
          <span className="muted">page {dayPage}</span>
          <Button onClick={() => setDayPage((p) => p + 1)}>Next</Button>
        </div>

        <p className="muted">
          <Info size={12} /> <b>A row carries counts, never periods.</b> A full day is about 120 KB;
          a page of twenty carrying its periods would be megabytes to render twenty dates. The
          counts are computed in the database and the periods never leave it.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Sections are counted paired with their class</b> — &ldquo;A&rdquo;
          of one class and &ldquo;A&rdquo; of another are two, so a twelve-class school does not
          report three sections.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Opening a day is #7, which is not built.</b> Until it is, the id in
          the last column is how a day will be addressed.
        </p>
      </Card>

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
