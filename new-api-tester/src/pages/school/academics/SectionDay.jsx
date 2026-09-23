import { useCallback, useEffect, useState } from 'react'
import { Info, Search, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One section's day: /school-academics/timetable/section-day
 *
 * ONE ENDPOINT — #8, and it is what a child's parent opens. Nobody outside the office wants the
 * whole school's four hundred periods; they want the eight their child sits through.
 *
 * ITS OWN ADDRESS, NOT A LENS ON THE DAY SCREEN. "What a parent opens" is an entry point, not a
 * filter somebody arrives at after reading the whole school's Tuesday — so it is a screen with a
 * link of its own, the same call the create/view split made.
 *
 * EARLIEST FIRST, AND THAT IS THE SERVER'S DOING. #7 returns the whole school's day as stored
 * because periods of different sections share an hour; one section cannot be in two places at
 * once, so here the time is a real order. Nothing on this page re-sorts anything.
 *
 * THE CLASS AND SECTION PICKERS ARE LOADED, NOT TYPED, because a classDocsId typed by hand is the
 * flat table with more steps. Both are still free of any gate: a section the class does not hold
 * is reachable by picking a class and then changing the class, and the refusals are the point.
 *
 * AN EMPTY DAY AND A WRONG SECTION LOOK DIFFERENT HERE, because they are different in the API: a
 * section with nothing on is a 200 with no rows, a section the class lacks is a 409.
 *
 * NOTHING IS DISABLED.
 */
export default function SectionDay() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [date, setDate] = useState('')
  const [classDocsId, setClassDocsId] = useState('')
  const [sectionNo, setSectionNo] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  //! THE SCHOOL'S OWN CLASSES AND THEIR SECTIONS. One read for the classes, then one per class for
  //! its sections — the same shape the builder uses, because it is the same question.
  const [structure, setStructure] = useState([])
  const [loadingStructure, setLoadingStructure] = useState(false)

  const loadStructure = useCallback(async () => {
    if (!actingSubdomain || !actingAcademicYear) return
    setLoadingStructure(true)
    const classes = await call('list-school-classes', {
      label: 'The classes to choose from',
      pathParams: { year: actingAcademicYear },
      query: { size: '100' },
    })

    const built = []
    for (const one of classes.bodyJson?.content ?? []) {
      const sections = await call('list-class-sections', {
        label: `Sections of ${one.name}`,
        pathParams: { year: actingAcademicYear, id: one.schoolClassId },
      })
      built.push({
        schoolClassId: one.schoolClassId,
        name: one.name,
        sections: sections.bodyJson?.sections ?? [],
      })
    }
    setStructure(built)
    setLoadingStructure(false)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, actingAcademicYear])

  useEffect(() => { loadStructure() }, [loadStructure])

  //! RETIRED SECTIONS ARE OFFERED TOO, deliberately. #8 does not use the active check every write
  //! uses: a section retired in March must not make February's Tuesday unreadable, and a picker
  //! that hid it would make that impossible to see.
  const sections = structure.find((one) => one.schoolClassId === classDocsId)?.sections ?? []

  const look = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const answer = await call('get-section-day', {
      label: "One section's day",
      pathParams: {
        year: actingAcademicYear ?? '',
        date,
        classDocsId,
        sectionNo,
      },
    })
    setLoading(false)
    setResult(answer)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, date, classDocsId, sectionNo])

  if (!actingSubdomain) return <NoSchoolChosen what="A section's day" />

  const day = result?.ok ? result.bodyJson : null
  const rows = day?.entries ?? []

  return (
    <>
      <Card
        title="Whose day"
        description="One section, one date. This is the read a parent's app makes — the eight periods a child sits through, not the school's four hundred."
        action={
          <div className="btn-row">
            <EndpointTag id="get-section-day" name="Open"
              pathParams={{ year: actingAcademicYear, date, classDocsId, sectionNo }} />
            <Button look="primary" icon={Search} busy={loading} onClick={look}>Open the day</Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field label="Date" hint="A holiday answers 404 NOT_A_WORKING_DAY and names it; a working day with nothing written answers 404 TIMETABLE_NOT_FOUND.">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          {/* THE TWO READS THAT FILL THESE TWO PICKERS. The classes, then EACH class's
              sections — {id} stays unsubstituted because it describes every class's call. */}
          <p className="muted">
            <EndpointTag id="list-school-classes" name="Classes to choose from"
              pathParams={{ year: actingAcademicYear }} query={{ size: '100' }} />
            {' '}
            <EndpointTag id="list-class-sections" name="Sections of each"
              pathParams={{ year: actingAcademicYear }} />
          </p>
          <Field label="Class" hint="Resolved in the year picked above. A class from another year is 404 CLASS_NOT_FOUND.">
            <Select
              label="Class"
              value={classDocsId}
              onChange={(value) => { setClassDocsId(value); setSectionNo('') }}
              options={[{ value: '', label: loadingStructure ? 'loading…' : '— pick a class —' },
                ...structure.map((one) => ({ value: one.schoolClassId, label: one.name }))]}
            />
          </Field>
          <Field label="Section" hint="Case-insensitive — the answer comes back in the class's own spelling. A section the class does not hold is 409 SECTION_NOT_IN_CLASS.">
            <Select
              label="Section"
              value={sectionNo}
              onChange={setSectionNo}
              options={[{ value: '', label: '— pick a section —' },
                ...sections.map((one) => ({
                  value: one.sectionNo,
                  label: one.active === false ? `${one.sectionNo} (retired)` : one.sectionNo,
                }))]}
            />
          </Field>
        </div>
        <p className="muted">
          <Info size={12} /> <b>A retired section still answers.</b> No gate runs on a read, and a
          section retired in March must not make February&apos;s Tuesday unreadable — attendance
          taken against it has to stay explicable. That is why #8 does not use the active-section
          check every write does, and why a retired one is still offered above.
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
            <Info size={12} /> <b>409 SECTION_NOT_IN_CLASS is not the same as an empty day.</b> A
            section that exists and has nothing scheduled answers <b>200</b> with no rows — which is
            the whole reason the two are different answers.
          </p>
        </Card>
      ) : null}

      {day ? (
        <Card
          title={`${day.className ?? 'Class'} · section ${day.sectionNo}`}
          description="Earliest first — a real order here, because a section cannot be in two places at once. #7 returns the whole school as stored, and says why."
          action={
            <div className="btn-row">
              <Badge>{day.entryCount} periods</Badge>
              <Badge>{day.lessonCount} lessons</Badge>
              <Badge>{day.teacherCount} staff</Badge>
            </div>
          }
        >
          {rows.length === 0 ? (
            <Empty
              title="Nothing scheduled for this section"
              description="A 200, not a 404 — the day exists and this section is simply free. The day-level 404s belong to the date, not to the section."
            />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
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
                <tbody>
                  {rows.map((entry) => (
                    <tr key={entry.timetableEntryId}>
                      <td><span className="mono">{entry.periodCode}</span></td>
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
            <Info size={12} /> <b>The subject name follows the section, not just the code.</b> One
            created without a <span className="mono">sectionNo</span> is class-wide; one created
            with it belongs to that section alone — so two sections under one code get two
            different names.
          </p>
          <p className="muted">{day.nextStep}</p>
        </Card>
      ) : null}

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> This is the read a
          parent&apos;s app would make, and right now anybody with the subdomain can make it for any
          child&apos;s section.
        </p>
      </Card>
    </>
  )
}
