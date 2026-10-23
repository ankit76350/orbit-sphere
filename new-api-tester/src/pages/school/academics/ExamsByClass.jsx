import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ChevronRight, FileText, RefreshCw } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input, Modal, TextArea } from '../../../components/ui/Kit.jsx'

/**
 * The "By class" tab of Exams — /school-academics/exams/classes
 *
 * THREE STEPS, EACH ITS OWN ADDRESS (?class=…&section=…&exam=…), so a class, a section and a sheet
 * can be linked, reloaded and shared, and Back steps back:
 *
 *   1. the year's classes                                         (list-school-classes)
 *   2. one class's sections                                        (list-class-sections)
 *   3. one section's tabulation sheet for one exam                 (#27, exam-tabulation)
 *      and, below it, the section's students — each one's report card for a term, worked out by
 *      #29b (draft-report-card) into an editable pop-up and saved by #29 (save-report-card)
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
  return (
    <>
      <SectionSheet {...{ year, classDocsId, sectionNo, examDocsId, className, exams, tabulation, tabulationProblem, loadTabulation, go }} />
      <ReportCards year={year} classDocsId={classDocsId} sectionNo={sectionNo} className={className} />
    </>
  )
}

/**
 * One section's tabulation sheet for the exam in the address (#27), with the exam picker.
 */
function SectionSheet({ year, classDocsId, sectionNo, examDocsId, className, exams, tabulation, tabulationProblem, loadTabulation, go }) {
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

/**
 * The section's students, each with their report card for a term — ONE CHILD AT A TIME. "Generate
 * card" opens a pop-up filled by #29b with the card as the marks and the register make it; every
 * field can be edited, and Save sends it to #29. The term starts on the one today falls in.
 */
function ReportCards({ year, classDocsId, sectionNo, className }) {
  const { call } = useApi()
  const [terms, setTerms] = useState([])
  const [termDocsId, setTermDocsId] = useState('')
  const [roster, setRoster] = useState([])
  const [open, setOpen] = useState(null)
  const [saved, setSaved] = useState({})

  useEffect(() => {
    let live = true
    call('list-academic-terms', { label: 'Terms of the year', pathParams: { year }, query: { size: '100' } })
      .then((answer) => {
        if (!live) return
        const list = answer.ok ? (answer.bodyJson?.content ?? []) : []
        setTerms(list)
        const today = new Date().toISOString().slice(0, 10)
        const pick = list.find((one) => one.startDate <= today && today <= one.endDate) ?? list[0]
        setTermDocsId((old) => old || (pick?.termDocsId ?? ''))
      })
    return () => { live = false }
  }, [call, year])

  //! THE SECTION'S STUDENT RECORDS — one row per record, cancelled ones left out. A card is made on a
  //! record, so a child who moved within the section's year shows once per placement here.
  const rosterQuery = { classDocsId, sectionNo, page: '0', size: '100', sort: 'rollNo' }
  useEffect(() => {
    let live = true
    call('list-student-records', { label: 'Students of the section', pathParams: { year }, query: rosterQuery })
      .then((answer) => {
        if (!live) return
        setRoster((answer.ok ? (answer.bodyJson?.content ?? []) : []).filter((one) => one.status !== 'CANCELLED'))
      })
    return () => { live = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, year, classDocsId, sectionNo])

  return (
    <Card
      title={`Report cards — ${className} ${sectionNo}`}
      description="One student at a time. Generate card works it out from the term's exams and the register, then you check, edit and save it as a DRAFT."
      action={<EndpointTag id="list-student-records" name="Students" pathParams={{ year }} query={rosterQuery} />}
    >
      <div className="field-grid">
        <Field label="Term" hint="The card is built from every exam in this term.">
          <div className="picker-pair">
            <Select value={terms.some((one) => one.termDocsId === termDocsId) ? termDocsId : ''}
              options={['', ...terms.map((one) => ({ value: one.termDocsId, label: `${one.name} · ${one.startDate} to ${one.endDate}` }))]}
              onChange={setTermDocsId} />
            <Input value={termDocsId} placeholder="termDocsId" onChange={(e) => setTermDocsId(e.target.value)} />
          </div>
        </Field>
      </div>
      <p className="muted"><EndpointTag id="list-academic-terms" name="Terms" pathParams={{ year }} query={{ size: '100' }} /></p>
      {roster.length === 0 ? <p className="muted">Nobody has a record in this section.</p> : (
        <div className="table-scroll">
          <table className="data-table">
            <thead><tr><th>Roll</th><th>Name</th><th>Admission no.</th><th>Placement</th><th>Saved here</th><th /></tr></thead>
            <tbody>
              {roster.map((one) => (
                <tr key={one.academicRecordDocsId}>
                  <td>{one.rollNo ?? '—'}</td>
                  <td>{one.studentName ?? one.studentDocsId}<div className="muted mono">{one.academicRecordDocsId}</div></td>
                  <td>{one.admissionNo ?? '—'}</td>
                  <td><Badge>{one.status}</Badge> <span className="muted">from {one.effectiveFrom}{one.effectiveUntil ? ` to ${one.effectiveUntil}` : ''}</span></td>
                  <td>{saved[one.academicRecordDocsId] ? <>v{saved[one.academicRecordDocsId].reportVersion} <Badge>{saved[one.academicRecordDocsId].status}</Badge> {saved[one.academicRecordDocsId].resultStatus}</> : <span className="muted">—</span>}</td>
                  <td><Button icon={FileText} onClick={() => setOpen(one)}>Generate card</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {open ? (
        <ReportCardForm
          year={year}
          termDocsId={termDocsId}
          student={open}
          onClose={() => setOpen(null)}
          onSaved={(card) => setSaved((old) => ({ ...old, [card.studentAcademicRecordDocsId]: card }))}
        />
      ) : null}
    </Card>
  )
}

const text = (value) => (value == null ? '' : String(value))
const num = (value) => (value === '' || value == null ? undefined : (Number.isNaN(Number(value)) ? value : Number(value)))
const bool = (value) => (value === 'true' ? true : value === 'false' ? false : undefined)

/**
 * The pop-up: #29b fills it, every box can be changed, Save sends it to #29. The papers under each
 * subject are shown as they are and sent back unchanged. STAYS OPEN AFTER A SAVE, so the saved card
 * can be checked and saved again.
 */
function ReportCardForm({ year, termDocsId, student, onClose, onSaved }) {
  const { call } = useApi()
  const [draft, setDraft] = useState(null)
  const [draftProblem, setDraftProblem] = useState(null)
  const [card, setCard] = useState(null)
  const [subjects, setSubjects] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  useEffect(() => {
    let live = true
    call('draft-report-card', { label: 'Work out the card', pathParams: { year, studentAcademicRecordDocsId: student.academicRecordDocsId }, query: { termDocsId } })
      .then((answer) => {
        if (!live) return
        if (!answer.ok) { setDraftProblem(answer); return }
        const d = answer.bodyJson
        setDraft(d)
        setCard({
          totalMaximumMarks: text(d.totalMaximumMarks), totalObtainedMarks: text(d.totalObtainedMarks),
          overallPercentage: text(d.overallPercentage), overallGradeCode: text(d.overallGradeCode),
          resultStatus: d.resultStatus ?? '', attendanceWorkingDays: text(d.attendanceWorkingDays),
          attendancePresentDays: text(d.attendancePresentDays), classTeacherRemark: '', principalRemark: '',
        })
        setSubjects((d.subjectResults ?? []).map((one) => ({
          ...one,
          subjectName: text(one.subjectName), maximumMarks: text(one.maximumMarks), obtainedMarks: text(one.obtainedMarks),
          percentage: text(one.percentage), gradeCode: text(one.gradeCode), passed: one.passed == null ? '' : String(one.passed),
          countsTowardTotal: one.countsTowardTotal !== false, teacherRemark: text(one.teacherRemark),
        })))
      })
    return () => { live = false }
  }, [call, year, termDocsId, student.academicRecordDocsId])

  const setField = (key, value) => setCard((old) => ({ ...old, [key]: value }))
  const setSubject = (index, change) => setSubjects((old) => old.map((one, i) => (i === index ? { ...one, ...change } : one)))

  const body = !draft || !card ? null : {
    termDocsId,
    examDocsIds: draft.examDocsIds,
    gradingSchemeDocsId: draft.gradingSchemeDocsId,
    subjectResults: subjects.map((one) => ({
      subjectCode: one.subjectCode, subjectName: one.subjectName, subjectType: one.subjectType,
      countsTowardTotal: one.countsTowardTotal, components: one.components,
      maximumMarks: num(one.maximumMarks), obtainedMarks: num(one.obtainedMarks), percentage: num(one.percentage),
      gradeCode: one.gradeCode || undefined, gradingSchemeDocsId: one.gradingSchemeDocsId, passed: bool(one.passed),
      teacherRemark: one.teacherRemark || undefined,
    })),
    totalMaximumMarks: num(card.totalMaximumMarks), totalObtainedMarks: num(card.totalObtainedMarks),
    overallPercentage: num(card.overallPercentage), overallGradeCode: card.overallGradeCode || undefined,
    resultStatus: card.resultStatus || undefined,
    attendanceWorkingDays: num(card.attendanceWorkingDays), attendancePresentDays: num(card.attendancePresentDays),
    classTeacherRemark: card.classTeacherRemark || undefined, principalRemark: card.principalRemark || undefined,
  }

  const save = async () => {
    setSending(true)
    const answer = await call('save-report-card', { label: 'Save the card', pathParams: { year, studentAcademicRecordDocsId: student.academicRecordDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onSaved(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Report card — ${student.studentName ?? student.studentDocsId}`}
      description="Worked out from the term's exams and the register. Change anything before saving — what you save is what the card says."
      endpoint={<EndpointTag id="save-report-card" name="Save" look="primary" pathParams={{ year, studentAcademicRecordDocsId: student.academicRecordDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body ?? {}}
      footer={<Button look="primary" onClick={save} busy={sending}>Save card</Button>}
    >
      <p className="muted"><EndpointTag id="draft-report-card" name="Work it out" pathParams={{ year, studentAcademicRecordDocsId: student.academicRecordDocsId }} query={{ termDocsId }} /></p>
      {draftProblem ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{draftProblem.bodyJson?.code ?? draftProblem.status}</span></div>
          <pre className="resp-body">{draftProblem.bodyJson?.message ?? draftProblem.bodyText}</pre>
        </div>
      ) : !draft || !card ? <p className="muted">Working it out…</p> : (
        <>
          <p>
            <b>{draft.className} {draft.sectionNo}</b>, roll {draft.rollNo ?? '—'} · <b>{draft.termName}</b> · {draft.examNames.join(', ') || 'no exams'}
            {draft.weighted ? ' · weighted' : ''}{draft.gradingSchemeName ? ` · grades from ${draft.gradingSchemeName}` : ''}
          </p>
          <p>
            Saving will <Badge>{draft.saveWill}</Badge>
            {draft.existing ? <span className="muted"> — the latest card is v{draft.existing.reportVersion} <Badge>{draft.existing.status}</Badge></span> : null}
          </p>
          {draft.warnings?.length ? (
            <div className="resp"><pre className="resp-body">{draft.warnings.map((one) => `• ${one}`).join('\n')}</pre></div>
          ) : null}

          <h4>Subjects</h4>
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Subject</th><th>In total</th><th>Max</th><th>Obtained</th><th>%</th><th>Grade</th><th>Passed</th><th>Teacher's remark</th></tr></thead>
              <tbody>
                {subjects.map((one, index) => (
                  <tr key={one.subjectCode}>
                    <td>
                      <Input value={one.subjectName} onChange={(e) => setSubject(index, { subjectName: e.target.value })} />
                      {(one.components ?? []).map((part, i) => (
                        <div key={i} className="muted">
                          {part.examName}{part.componentName ? ` · ${part.componentName}` : ''}: {part.debarred ? 'debarred'
                            : part.obtainedMarks != null ? `${part.obtainedMarks}/${part.maximumMarks}${part.graceMarks ? ` (+${part.graceMarks} grace)` : ''}`
                            : (part.attendanceStatus ?? 'no mark').toLowerCase()}
                          {part.weightPercent != null ? ` · weight ${part.weightPercent}%` : ''}
                        </div>
                      ))}
                    </td>
                    <td><input type="checkbox" checked={one.countsTowardTotal} onChange={() => setSubject(index, { countsTowardTotal: !one.countsTowardTotal })} /></td>
                    <td><Input value={one.maximumMarks} onChange={(e) => setSubject(index, { maximumMarks: e.target.value })} /></td>
                    <td><Input value={one.obtainedMarks} onChange={(e) => setSubject(index, { obtainedMarks: e.target.value })} /></td>
                    <td><Input value={one.percentage} onChange={(e) => setSubject(index, { percentage: e.target.value })} /></td>
                    <td><Input value={one.gradeCode} onChange={(e) => setSubject(index, { gradeCode: e.target.value })} /></td>
                    <td><Select value={one.passed} options={['', 'true', 'false']} onChange={(v) => setSubject(index, { passed: v })} /></td>
                    <td><Input value={one.teacherRemark} onChange={(e) => setSubject(index, { teacherRemark: e.target.value })} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <h4>The card</h4>
          <div className="field-grid">
            <Field label="Total obtained / maximum">
              <div className="picker-pair">
                <Input value={card.totalObtainedMarks} onChange={(e) => setField('totalObtainedMarks', e.target.value)} />
                <Input value={card.totalMaximumMarks} onChange={(e) => setField('totalMaximumMarks', e.target.value)} />
              </div>
            </Field>
            <Field label="Overall %" hint="0 to 100.">
              <Input value={card.overallPercentage} onChange={(e) => setField('overallPercentage', e.target.value)} />
            </Field>
            <Field label="Overall grade">
              <Input value={card.overallGradeCode} onChange={(e) => setField('overallGradeCode', e.target.value)} />
            </Field>
            <Field label="Result" required>
              <Select value={card.resultStatus} options={['', 'PASS', 'FAIL', 'WITHHELD', 'INCOMPLETE']} onChange={(v) => setField('resultStatus', v)} />
            </Field>
            <Field label="Attendance — present / working days" hint="Present cannot be more than the working days (400 CARD_NUMBERS_DO_NOT_ADD_UP).">
              <div className="picker-pair">
                <Input value={card.attendancePresentDays} onChange={(e) => setField('attendancePresentDays', e.target.value)} />
                <Input value={card.attendanceWorkingDays} onChange={(e) => setField('attendanceWorkingDays', e.target.value)} />
              </div>
            </Field>
            <Field label="Class teacher's remark" wide>
              <TextArea value={card.classTeacherRemark} onChange={(e) => setField('classTeacherRemark', e.target.value)} />
            </Field>
            <Field label="Principal's remark" wide>
              <TextArea value={card.principalRemark} onChange={(e) => setField('principalRemark', e.target.value)} />
            </Field>
          </div>
        </>
      )}
      {result?.ok ? <p className="muted">Saved — card v{result.bodyJson.reportVersion}, {result.bodyJson.status}. {result.bodyJson.nextStep}</p> : null}
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}

