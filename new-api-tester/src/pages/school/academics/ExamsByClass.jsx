import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input } from '../../../components/ui/Kit.jsx'

/**
 * The "By class" tab of Exams — /school-academics/exams/classes
 *
 * THREE STEPS, EACH ITS OWN ADDRESS (?class=…&section=…&exam=…), so a class, a section and a sheet
 * can be linked, reloaded and shared, and Back steps back:
 *
 *   1. the year's classes                                         (list-school-classes)
 *   2. one class's sections                                        (list-class-sections)
 *   3. one section's tabulation sheet for one exam                 (#27, exam-tabulation)
 *
 * A TABULATION SHEET IS PER EXAM, and no exam has been chosen at this level — so step 3 has an exam
 * picker on top. It starts on the most recent exam that has papers for this section, worked out by
 * reading each exam's datesheet (#11) filtered to the class and section; the picker shows the count.
 */
export default function ExamsByClass({ reloadKey = 0 }) {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [search, setSearch] = useSearchParams()
  const classDocsId = search.get('class') ?? ''
  const sectionNo = search.get('section') ?? ''
  const examDocsId = search.get('exam') ?? ''
  const go = (where) => setSearch(where)

  const [classes, setClasses] = useState([])
  const [classesProblem, setClassesProblem] = useState(null)
  const [sections, setSections] = useState([])
  const [sectionsProblem, setSectionsProblem] = useState(null)
  //! THE YEAR'S EXAMS, each with how many papers it has for the chosen section.
  const [exams, setExams] = useState(null)
  const [tabulation, setTabulation] = useState(null)
  const [tabulationProblem, setTabulationProblem] = useState(null)

  //! STEP 1 — THE YEAR'S CLASSES.
  useEffect(() => {
    if (!actingSubdomain || !year) return undefined
    let live = true
    call('list-school-classes', { label: `Classes in ${year}`, pathParams: { year }, query: { page: '0', size: '100' } })
      .then((answer) => {
        if (!live) return
        if (answer.ok) { setClasses(answer.bodyJson?.content ?? []); setClassesProblem(null) } else { setClasses([]); setClassesProblem(answer) }
      })
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, reloadKey])

  //! STEP 2 — A CLASS CHOSEN: ITS SECTIONS.
  useEffect(() => {
    if (!classDocsId || !year) { setSections([]); setSectionsProblem(null); return undefined }
    let live = true
    call('list-class-sections', { label: 'Sections of the class', pathParams: { year, id: classDocsId } })
      .then((answer) => {
        if (!live) return
        if (answer.ok) { setSections(answer.bodyJson?.sections ?? []); setSectionsProblem(null) } else { setSections([]); setSectionsProblem(answer) }
      })
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, year, classDocsId, reloadKey])

  //! STEP 3a — A SECTION CHOSEN: THE YEAR'S EXAMS, and how many papers each has for this section.
  //! With no exam in the address yet, the newest one with papers is picked.
  useEffect(() => {
    if (!classDocsId || !sectionNo || !year) { setExams(null); return undefined }
    let live = true
    ;(async () => {
      const answer = await call('list-exams', { label: 'Exams of the year', pathParams: { year }, query: { page: '0', size: '100', sort: 'startDate,desc' } })
      const list = answer.ok ? (answer.bodyJson?.content ?? []) : []
      const counted = await Promise.all(list.map(async (exam) => {
        const sheet = await call('exam-datesheet', { label: `Papers of ${exam.name} for the section`, pathParams: { year, examDocsId: exam.examDocsId }, query: { classDocsId, sectionNo } })
        const papers = sheet.ok ? (sheet.bodyJson?.papers ?? []).filter((one) => one.status !== 'CANCELLED').length : 0
        return { ...exam, papers }
      }))
      if (!live) return
      setExams(counted)
      if (!search.get('exam')) {
        const pick = counted.find((one) => one.papers > 0) ?? counted[0]
        if (pick) setSearch({ class: classDocsId, section: sectionNo, exam: pick.examDocsId }, { replace: true })
      }
    })()
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, year, classDocsId, sectionNo, reloadKey])

  //! STEP 3b — THE SHEET (#27) for the chosen exam.
  const loadTabulation = useCallback(async () => {
    if (!classDocsId || !sectionNo || !examDocsId || !year) { setTabulation(null); setTabulationProblem(null); return }
    const answer = await call('exam-tabulation', { label: 'Tabulation sheet', pathParams: { year, examDocsId, classDocsId, sectionNo } })
    if (answer.ok) { setTabulation(answer.bodyJson); setTabulationProblem(null) } else { setTabulation(null); setTabulationProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, year, examDocsId, classDocsId, sectionNo, reloadKey])

  useEffect(() => { loadTabulation() }, [loadTabulation])

  const className = classes.find((one) => one.schoolClassId === classDocsId)?.name ?? classDocsId

  //! STEP 1.
  if (!classDocsId) {
    return (
      <Card
        title={`Classes — ${classes.length}`}
        description="Open a class to see its sections, then a section to see its tabulation sheet for an exam."
        action={<EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} query={{ page: '0', size: '100' }} />}
      >
        {classesProblem ? (
          <pre className="resp-body">{classesProblem.bodyJson?.message ?? classesProblem.bodyText}</pre>
        ) : classes.length === 0 ? <p className="muted">This year has no classes.</p> : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Class</th><th>Sections</th><th>Subjects</th><th /></tr></thead>
              <tbody>
                {classes.map((one) => (
                  <tr key={one.schoolClassId} data-opens onClick={() => go({ class: one.schoolClassId })}>
                    <td><b>{one.name}</b>{one.active === false ? <span className="muted"> · retired</span> : null}</td>
                    <td>{one.sectionCount ?? '—'}</td>
                    <td>{one.subjectCount ?? '—'}</td>
                    <td><Button icon={ChevronRight} onClick={(e) => { e.stopPropagation(); go({ class: one.schoolClassId }) }}>Open</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    )
  }

  //! STEP 2.
  if (!sectionNo) {
    return (
      <Card
        title={`${className} — sections`}
        description="Open a section to see its tabulation sheet."
        action={(
          <>
            <EndpointTag id="list-class-sections" name="Sections" pathParams={{ year, id: classDocsId }} />{' '}
            <Button icon={ArrowLeft} onClick={() => go({})}>All classes</Button>
          </>
        )}
      >
        {sectionsProblem ? (
          <pre className="resp-body">{sectionsProblem.bodyJson?.message ?? sectionsProblem.bodyText}</pre>
        ) : sections.length === 0 ? <p className="muted">This class has no sections.</p> : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Section</th><th>Capacity</th><th /></tr></thead>
              <tbody>
                {sections.map((one) => (
                  <tr key={one.sectionNo} data-opens onClick={() => go({ class: classDocsId, section: one.sectionNo })}>
                    <td><b>{one.sectionNo}</b>{one.active === false ? <span className="muted"> · off</span> : null}</td>
                    <td>{one.capacity ?? '—'}</td>
                    <td><Button icon={ChevronRight} onClick={(e) => { e.stopPropagation(); go({ class: classDocsId, section: one.sectionNo }) }}>Open</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    )
  }

  //! STEP 3.
  const examOptions = (exams ?? []).map((one) => ({
    value: one.examDocsId,
    label: `${one.name} · ${one.termName ?? ''} · ${one.status} · ${one.papers} paper${one.papers === 1 ? '' : 's'} here`,
  }))
  return (
    <Card
      title={`Tabulation sheet — ${className} ${tabulation?.sectionNo ?? sectionNo}`}
      description="A row per child, a column per paper, then total, percentage, grade and result. AB absent · EX exempt · WH withheld · DB debarred · NE sat, not entered · — not held yet. No rank."
      action={(
        <>
          <EndpointTag id="exam-tabulation" name="Tabulation" pathParams={{ year, examDocsId, classDocsId, sectionNo }} />{' '}
          <Button icon={ArrowLeft} onClick={() => go({})}>All classes</Button>{' '}
          <Button icon={ArrowLeft} onClick={() => go({ class: classDocsId })}>{className}</Button>{' '}
          <Button icon={RefreshCw} onClick={loadTabulation}>Refresh</Button>
        </>
      )}
    >
      <div className="field-grid">
        <Field label="Exam" hint="Starts on the newest exam with papers for this section. The id can be typed — another school's is 404 EXAM_NOT_FOUND.">
          <div className="picker-pair">
            <Select value={examOptions.some((one) => one.value === examDocsId) ? examDocsId : ''}
              options={['', ...examOptions]}
              onChange={(id) => go({ class: classDocsId, section: sectionNo, exam: id })} />
            <Input value={examDocsId} placeholder="examDocsId" onChange={(e) => go({ class: classDocsId, section: sectionNo, exam: e.target.value })} />
          </div>
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="list-exams" name="Exams" pathParams={{ year }} query={{ page: '0', size: '100', sort: 'startDate,desc' }} />{' '}
        <EndpointTag id="exam-datesheet" name="Papers per exam" pathParams={{ year, examDocsId }} query={{ classDocsId, sectionNo }} />
      </p>
      {exams && exams.length === 0 ? <p className="muted">This year has no exams yet.</p> : null}
      {!examDocsId ? null : tabulationProblem ? (
        <pre className="resp-body">{tabulationProblem.bodyJson?.message ?? tabulationProblem.bodyText}</pre>
      ) : !tabulation ? null : (
        <>
          <p>
            <b>{tabulation.examName}</b>.{' '}
            {tabulation.gradingSchemeName ? <>Grades from <b>{tabulation.gradingSchemeName}</b>. </> : <span className="muted">The exam has no grading scheme, so there is no overall grade. </span>}
            {tabulation.finalColumns} of {tabulation.columns.length} paper{tabulation.columns.length === 1 ? '' : 's'} final.{'  '}
            {Object.entries(tabulation.byResult).filter(([, count]) => count > 0).map(([result, count]) => (
              <span key={result}><Badge>{result}</Badge> {count}{'  '}</span>
            ))}
          </p>
          {tabulation.columns.length === 0 ? <p className="muted">This exam has no paper for this section.</p> : null}
          {tabulation.students.length === 0 ? <p className="muted">No candidates in this section for this exam — add them on the exam's page.</p> : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Roll</th><th>Name</th>
                    {tabulation.columns.map((col) => (
                      <th key={col.examScheduleDocsId}>
                        {col.subjectName ?? col.subjectCode}{col.componentName ? ` · ${col.componentName}` : ''}
                        <div className="muted">/{col.maximumMarks}{col.passingMarks != null ? `, pass ${col.passingMarks}` : ''}{col.isFinal ? ' · final' : ' · draft'}</div>
                      </th>
                    ))}
                    <th>Total</th><th>%</th><th>Grade</th><th>Result</th>
                  </tr>
                </thead>
                <tbody>
                  {tabulation.students.map((row) => (
                    <tr key={row.studentDocsId}>
                      <td>{row.rollNo ?? '—'}</td>
                      <td>{row.fullName ?? row.studentDocsId}{row.candidateStatus === 'DEBARRED' ? <div className="muted">debarred</div> : null}</td>
                      {row.cells.map((cell) => (
                        <td key={cell.examScheduleDocsId}>
                          {cell.code ?? (cell.effectiveMarks ?? cell.gradeCode)}
                          {cell.passed === false ? <b> ✗</b> : null}
                        </td>
                      ))}
                      <td>{row.total} / {row.maximumTotal}</td>
                      <td>{row.percentage ?? '—'}</td>
                      <td>{row.grade ?? '—'}</td>
                      <td><Badge>{row.result}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </Card>
  )
}
