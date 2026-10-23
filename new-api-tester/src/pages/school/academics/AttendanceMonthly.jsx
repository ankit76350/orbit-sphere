import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Card, Empty, Field } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import SectionMonthlyRegister from './SectionMonthlyRegister.jsx'

/**
 * Attendance tab "Monthly register": /school-academics/attendance/monthly?class=&section=
 *
 * PICK A CLASS AND A SECTION, then #10 shows one row per child for the month (changeable on the
 * card). The choice is in the address, so a reload lands on the same section.
 */

/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default function AttendanceMonthly({ reloadKey }) {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [search, setSearch] = useSearchParams()
  const classId = search.get('class') ?? ''
  const sectionNo = search.get('section') ?? ''

  const [classes, setClasses] = useState([])
  const [sections, setSections] = useState([])

  //! THE CLASSES OF THE NAVBAR'S YEAR, for the first dropdown.
  useEffect(() => {
    if (!actingSubdomain || !year) return
    let live = true
    call('list-school-classes', {
      label: `Classes in ${year}`,
      pathParams: { year },
      query: { page: '0', size: '100' },
    }).then((answer) => { if (live) setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, actingSubdomain, year, reloadKey])

  //! THE SECTIONS OF THE CHOSEN CLASS, for the second.
  useEffect(() => {
    if (!actingSubdomain || !year || !classId) return
    let live = true
    call('list-class-sections', {
      label: 'Sections of the class',
      pathParams: { year, id: classId },
    }).then((answer) => { if (live) setSections(answer.ok ? (answer.bodyJson?.sections ?? []) : []) })
    return () => { live = false }
  }, [call, actingSubdomain, year, classId, reloadKey])

  return (
    <>
      <Card
        title="Monthly register"
        description="Pick a class and a section. One row per child who was in it, with their days, marks and percentage."
        action={
          <div className="btn-row">
            <EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} />
            <EndpointTag id="list-class-sections" name="Sections" pathParams={{ year, id: classId }} />
          </div>
        }
      >
        <div className="field-grid">
          <Field label="Class" required>
            <Select value={classId}
              options={['', ...classes.map((c) => ({ value: c.schoolClassId, label: c.name }))]}
              onChange={(v) => setSearch(v ? { class: v } : {}, { replace: true })} />
          </Field>
          <Field label="Section" required>
            <Select value={sectionNo}
              options={['', ...(classId ? sections : []).map((s) => ({ value: s.sectionNo, label: s.sectionNo }))]}
              onChange={(v) => setSearch({ class: classId, ...(v ? { section: v } : {}) }, { replace: true })} />
          </Field>
        </div>
      </Card>

      {classId && sectionNo ? (
        <SectionMonthlyRegister key={`${classId}-${sectionNo}-${reloadKey}`}
          classDocsId={classId} sectionNo={sectionNo} date={today()} />
      ) : (
        <Empty title="Pick a class and a section" description="The register for the current month appears here." />
      )}
    </>
  )
}
