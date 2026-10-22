import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLeft, ChevronRight, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath, grandchildPath } from '../../../paths.js'

/**
 * One exam, on its own page: /school-academics/exams/{termId}/exams/{examId}
 *
 * FOUR ENDPOINTS — #7 reads the exam: its facts, its papers per status and its marks per status.
 * #2 fixes it: Edit opens a modal, the boxes on one side and the request on the other. #8 adds a
 * paper — one subject for one or more sections at one date and time. #11 is the datesheet — every
 * paper, with filters for a class, a section, a day and a status — and it reloads after each add.
 *
 * ONLY WHAT WAS CHANGED IS SENT, with the exam's version. Sending nothing else is 400
 * NOTHING_TO_UPDATE, a stale version is 409 CONCURRENT_MODIFICATION, and a new term while the exam
 * has papers is 409 EXAM_HAS_PAPERS. Nothing is disabled.
 */
export default function ExamDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id: termId, examId } = useParams()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [editing, setEditing] = useState(false)
  const [adding, setAdding] = useState(false)
  //! THE DATESHEET (#11) AND ITS FILTERS. An empty filter sends nothing.
  const [sheet, setSheet] = useState(null)
  const [sheetProblem, setSheetProblem] = useState(null)
  const [classes, setClasses] = useState([])
  const [filterClass, setFilterClass] = useState('')
  const [filterSection, setFilterSection] = useState('')
  const [filterDate, setFilterDate] = useState('')
  const [filterStatus, setFilterStatus] = useState('')

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-exam', { label: 'One exam', pathParams: { year, examDocsId: examId ?? '' } })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, examId])

  useEffect(() => { load() }, [load])

  const sheetQuery = useMemo(() => {
    const out = {}
    if (filterClass) out.classDocsId = filterClass
    if (filterSection) out.sectionNo = filterSection
    if (filterDate) out.date = filterDate
    if (filterStatus) out.status = filterStatus
    return out
  }, [filterClass, filterSection, filterDate, filterStatus])

  const loadSheet = useCallback(async () => {
    if (!actingSubdomain || !year) return
    const answer = await call('exam-datesheet', { label: 'The datesheet', pathParams: { year, examDocsId: examId ?? '' }, query: sheetQuery })
    if (answer.ok) { setSheet(answer.bodyJson); setSheetProblem(null) } else { setSheet(null); setSheetProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, examId, sheetQuery])

  useEffect(() => { loadSheet() }, [loadSheet])

  //! THE YEAR'S CLASSES, for the datesheet's class filter.
  useEffect(() => {
    if (!actingSubdomain || !year) return undefined
    let live = true
    call('list-school-classes', { label: `Classes in ${year}`, pathParams: { year }, query: { page: '0', size: '100' } })
      .then((answer) => { if (live) setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, actingSubdomain, year])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const exam = data?.exam

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{exam?.name ?? 'Exam'}</h1>
          <p className="muted">{exam ? `${exam.termName ?? exam.termDocsId} · ${exam.examType}` : examId}</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(detailPath('school', 'academics', 'exams', termId ?? ''))}>
          {exam?.termName ?? 'Term'}
        </Button>
        <EndpointTag id="get-exam" name="Exam" pathParams={{ year, examDocsId: examId }} />
        <Button icon={RefreshCw} onClick={() => { load(); loadSheet() }} busy={loading}>Refresh</Button>
        <Button icon={Pencil} onClick={() => setEditing(true)}>Edit</Button>
        <Button look="primary" icon={Plus} onClick={() => setAdding(true)}>Add a paper</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {exam ? (
        <>
          <Card title="The exam">
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><th>Name</th><td><b>{exam.name}</b></td></tr>
                  <tr><th>Term</th><td>{exam.termName ?? <span className="mono muted">{exam.termDocsId}</span>}</td></tr>
                  <tr><th>Type</th><td>{exam.examType}</td></tr>
                  <tr><th>Dates</th><td><span className="mono">{exam.startDate}</span> to <span className="mono">{exam.endDate}</span></td></tr>
                  <tr><th>Weight</th><td>{exam.weightPercent != null ? `${exam.weightPercent}%` : <span className="muted">not weighted</span>}</td></tr>
                  <tr><th>Grading scheme</th><td>{exam.gradingSchemeName ?? <span className="muted">none</span>}</td></tr>
                  <tr><th>Status</th><td><Badge>{exam.status}</Badge></td></tr>
                  <tr><th>Version</th><td className="mono">{exam.version}</td></tr>
                  <tr><th>Id</th><td className="mono muted">{exam.examDocsId}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card title={`Papers — ${data.paperTotal}`} description="Every paper of the exam, by status.">
            <p>
              {Object.entries(data.papersByStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
          </Card>

          <Card
            title={sheet ? `Datesheet — ${sheet.paperCount} paper${sheet.paperCount === 1 ? '' : 's'}` : 'Datesheet'}
            description="Every paper, by date, time, class and section. Cancelled papers show unless the status filter leaves them out."
            action={<EndpointTag id="exam-datesheet" name="Datesheet" pathParams={{ year, examDocsId: examId }} query={sheetQuery} />}
          >
            <div className="field-grid">
              <Field label="Class">
                <Select value={filterClass} options={['', ...classes.map((one) => ({ value: one.schoolClassId, label: one.name }))]} onChange={setFilterClass} />
              </Field>
              <Field label="Section" hint="Ignoring case.">
                <Input value={filterSection} placeholder="A" onChange={(e) => setFilterSection(e.target.value)} />
              </Field>
              <Field label="Day">
                <Input type="date" value={filterDate} onChange={(e) => setFilterDate(e.target.value)} />
              </Field>
              <Field label="Status">
                <Select value={filterStatus} options={['', 'DRAFT', 'SCHEDULED', 'PUBLISHED', 'COMPLETED', 'CANCELLED']} onChange={setFilterStatus} />
              </Field>
            </div>
            {sheetProblem ? (
              <pre className="resp-body">{sheetProblem.bodyJson?.message ?? sheetProblem.bodyText}</pre>
            ) : !sheet || sheet.papers.length === 0 ? (
              <p className="muted">No papers{Object.keys(sheetQuery).length ? ' match' : ' yet. Add a paper with the button at the top'}.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Date</th><th>Time</th><th>Class</th><th>Section</th><th>Subject</th><th>Part</th><th>Marks</th><th>Rooms (seats, invigilators)</th><th>Status</th><th /></tr>
                  </thead>
                  <tbody>
                    {sheet.papers.map((one) => (
                      <tr key={one.examScheduleDocsId}>
                        <td className="mono">{one.examDate}</td>
                        <td className="mono">{one.startTime}–{one.endTime}</td>
                        <td>{one.className ?? one.classDocsId}</td>
                        <td>{one.sectionNo}</td>
                        <td>{one.subjectName ?? one.subjectCode}<div className="mono muted">{one.subjectCode}</div></td>
                        <td>{one.componentName ?? <span className="muted">whole subject</span>}</td>
                        <td>{one.maximumMarks}{one.passingMarks != null ? ` (pass ${one.passingMarks})` : ''}</td>
                        <td>
                          {one.rooms.length === 0 ? <span className="muted">no rooms yet</span> : one.rooms.map((r) => (
                            <div key={r.roomDocsId}>
                              <b>{r.roomName ?? r.roomDocsId}</b> · {r.seatCount} seat{r.seatCount === 1 ? '' : 's'} for the sitting
                              {r.invigilators.length ? ` · ${r.invigilators.map((i) => i.fullName ?? i.staffDocsId).join(', ')}` : ''}
                            </div>
                          ))}
                          {one.rooms.length > 1 ? <div className="muted">{one.totalSeats} seats in all, for the sitting</div> : null}
                        </td>
                        <td><Badge tone={one.status === 'CANCELLED' ? 'bad' : undefined}>{one.status}</Badge></td>
                        <td>
                          <Button icon={ChevronRight}
                            onClick={() => navigate(grandchildPath('school', 'academics', 'exams', termId ?? '', 'exams', examId ?? '', 'papers', one.examScheduleDocsId))}>
                            Open
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={`Marks — ${data.markTotal}`} description="Every mark entered for the exam, by status.">
            <p>
              {Object.entries(data.marksByStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
          </Card>
        </>
      ) : null}

      {adding && exam ? (
        <AddPaper
          year={year}
          exam={exam}
          onClose={() => setAdding(false)}
          onAdded={() => { setAdding(false); load(); loadSheet() }}
        />
      ) : null}

      {editing && exam ? (
        <EditExam
          year={year}
          exam={exam}
          onClose={() => setEditing(false)}
          onSaved={() => { setEditing(false); load() }}
        />
      ) : null}
    </div>
  )
}

/**
 * Fixing one exam — #2. Every box starts at the stored value; only a box that was changed is sent.
 */
function EditExam({ year, exam, onClose, onSaved }) {
  const { call } = useApi()
  const [name, setName] = useState(exam.name)
  const [termDocsId, setTermDocsId] = useState(exam.termDocsId)
  const [startDate, setStartDate] = useState(exam.startDate)
  const [endDate, setEndDate] = useState(exam.endDate)
  const [weightPercent, setWeightPercent] = useState(exam.weightPercent != null ? String(exam.weightPercent) : '')
  const [gradingSchemeDocsId, setGradingSchemeDocsId] = useState(exam.gradingSchemeDocsId ?? '')
  const [version, setVersion] = useState(String(exam.version ?? ''))
  const [terms, setTerms] = useState([])
  const [schemes, setSchemes] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE YEAR'S TERMS AND THE SCHOOL'S SCHEMES, for the two dropdowns. Retired ones are listed too.
  useEffect(() => {
    let live = true
    call('list-academic-terms', { label: 'Terms to pick from', pathParams: { year }, query: { size: '100' } })
      .then((answer) => { if (live) setTerms(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    call('list-grading-schemes', { label: 'Grading schemes to pick from', query: { size: '100' } })
      .then((answer) => { if (live) setSchemes(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, year])

  //! ONLY THE CHANGED BOXES. An emptied scheme box is sent as "" — that removes the scheme.
  const body = {}
  if (name !== exam.name) body.name = name
  if (termDocsId !== exam.termDocsId) body.termDocsId = termDocsId
  if (startDate !== exam.startDate) body.startDate = startDate
  if (endDate !== exam.endDate) body.endDate = endDate
  if (weightPercent !== (exam.weightPercent != null ? String(exam.weightPercent) : '') && weightPercent !== '') {
    body.weightPercent = Number(weightPercent)
  }
  if (gradingSchemeDocsId !== (exam.gradingSchemeDocsId ?? '')) body.gradingSchemeDocsId = gradingSchemeDocsId
  body.version = version === '' ? undefined : Number(version)

  const send = async () => {
    setSending(true)
    const answer = await call('update-exam', { label: 'Save the exam', pathParams: { year, examDocsId: exam.examDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onSaved(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Edit ${exam.name}`}
      description="Only the boxes you change are sent. The term can change only while the exam has no papers."
      endpoint={<EndpointTag id="update-exam" name="Save" look="primary" pathParams={{ year, examDocsId: exam.examDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Save</Button>}
    >
      <div className="field-grid">
        <Field label="Name" hint="Unique in the year, ignoring case. Emptied is 400 EXAM_NAME_REQUIRED.">
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Term" hint="Only while the exam has no papers (409 EXAM_HAS_PAPERS). The dates must fit the new term.">
          <div className="picker-pair">
            <Select value={terms.some((one) => one.termDocsId === termDocsId) ? termDocsId : ''}
              options={['', ...terms.map((one) => ({
                value: one.termDocsId,
                label: `${one.name} · ${one.startDate} to ${one.endDate}${one.active === false ? ' · retired' : ''}`,
              }))]}
              onChange={setTermDocsId} />
            <Input value={termDocsId} onChange={(e) => setTermDocsId(e.target.value)} />
          </div>
        </Field>
        <Field label="Starts" hint="Inside the term, and still holding every paper (409 PAPERS_OUTSIDE_DATES).">
          <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </Field>
        <Field label="Ends">
          <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </Field>
        <Field label="Weight %" hint="Above 0 and at most 100. It cannot be cleared here — an empty box sends nothing.">
          <Input value={weightPercent} onChange={(e) => setWeightPercent(e.target.value)} />
        </Field>
        <Field label="Grading scheme" hint="Empty the box to remove the scheme. A newly chosen retired one is 409 GRADING_SCHEME_NOT_ACTIVE.">
          <div className="picker-pair">
            <Select value={schemes.some((one) => one.gradingSchemeDocsId === gradingSchemeDocsId) ? gradingSchemeDocsId : ''}
              options={['', ...schemes.map((one) => ({
                value: one.gradingSchemeDocsId,
                label: `${one.name} ${one.schemeVersion ?? ''}${one.active === false ? ' · retired' : ''}`,
              }))]}
              onChange={setGradingSchemeDocsId} />
            <Input value={gradingSchemeDocsId} onChange={(e) => setGradingSchemeDocsId(e.target.value)} />
          </div>
        </Field>
        <Field label="The exam's version" required hint="Filled from the exam. Change it to see 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="list-academic-terms" name="Terms" pathParams={{ year }} query={{ size: '100' }} />{' '}
        <EndpointTag id="list-grading-schemes" name="Schemes" query={{ size: '100' }} />
      </p>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}

/**
 * Adding a paper — #8. One subject of one class, for the sections ticked, at one date and time.
 * One paper is saved per section. The boxes on one side, what will be sent on the other.
 */
function AddPaper({ year, exam, onClose, onAdded }) {
  const { call } = useApi()
  const [classes, setClasses] = useState([])
  const [classDocsId, setClassDocsId] = useState('')
  const [sections, setSections] = useState([])
  const [subjects, setSubjects] = useState([])
  const [ticked, setTicked] = useState([])
  //! EXTRA SECTIONS TYPED BY HAND, comma separated — so a section the class does not have can be
  //! sent to see 400 SECTION_NOT_IN_CLASS.
  const [extraSections, setExtraSections] = useState('')
  const [subjectCode, setSubjectCode] = useState('')
  const [componentName, setComponentName] = useState('')
  const [examDate, setExamDate] = useState(exam.startDate)
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('12:00')
  const [maximumMarks, setMaximumMarks] = useState('100')
  const [passingMarks, setPassingMarks] = useState('')
  //! THE PAPER'S ROOMS — each with its seats and its own invigilators. Several when the
  //! students do not fit in one.
  const [rooms, setRooms] = useState([])
  const [staff, setStaff] = useState([])
  const [schoolRooms, setSchoolRooms] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE YEAR'S CLASSES AND THE SCHOOL'S STAFF, for the dropdowns.
  useEffect(() => {
    let live = true
    call('list-school-classes', { label: `Classes in ${year}`, pathParams: { year }, query: { page: '0', size: '100' } })
      .then((answer) => { if (live) setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    call('list-staff', { label: 'Staff to pick invigilators from', query: { size: '100' } })
      .then((answer) => { if (live) setStaff(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    call('list-rooms', { label: 'Rooms to pick from', query: { size: '100' } })
      .then((answer) => { if (live) setSchoolRooms(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, year])

  //! THE CHOSEN CLASS'S SECTIONS AND SUBJECTS. Picking a class clears the ticks and the subject.
  useEffect(() => {
    if (!classDocsId) return undefined
    let live = true
    call('list-class-sections', { label: 'Sections of the class', pathParams: { year, id: classDocsId } })
      .then((answer) => { if (live) setSections(answer.ok ? (answer.bodyJson?.sections ?? []) : []) })
    call('list-class-subjects', { label: 'Subjects of the class', pathParams: { year, id: classDocsId } })
      .then((answer) => { if (live) setSubjects(answer.ok ? (answer.bodyJson?.subjects ?? []) : []) })
    return () => { live = false }
  }, [call, year, classDocsId])

  const pickClass = (id) => { setClassDocsId(id); setTicked([]); setSubjectCode(''); setSections([]); setSubjects([]) }
  const toggle = (sectionNo) => setTicked((old) => (old.includes(sectionNo) ? old.filter((one) => one !== sectionNo) : [...old, sectionNo]))
  const addRoom = () => setRooms((old) => [...old, { roomDocsId: '', seatCount: '', invigilators: [], typed: '' }])
  const updateRoom = (index, change) => setRooms((old) => old.map((one, i) => (i === index ? { ...one, ...change } : one)))
  const removeRoom = (index) => setRooms((old) => old.filter((_, i) => i !== index))
  const addInvigilator = (index, id) => {
    if (!id) return
    setRooms((old) => old.map((one, i) => (i === index && !one.invigilators.includes(id)
      ? { ...one, invigilators: [...one.invigilators, id], typed: '' } : one)))
  }

  const typed = extraSections.split(',').map((one) => one.trim()).filter(Boolean)
  const subjectOptions = [...new Set(subjects.map((one) => one.subjectCode))]

  // An empty optional box sends nothing rather than "".
  const body = {
    classDocsId,
    sectionNos: [...ticked, ...typed],
    subjectCode,
    ...(componentName ? { componentName } : {}),
    examDate,
    startTime,
    endTime,
    maximumMarks: maximumMarks === '' ? undefined : Number(maximumMarks),
    ...(passingMarks !== '' ? { passingMarks: Number(passingMarks) } : {}),
    ...(rooms.length ? {
      rooms: rooms.map((one) => ({
        roomDocsId: one.roomDocsId,
        seatCount: one.seatCount === '' ? undefined : Number(one.seatCount),
        ...(one.invigilators.length ? { invigilatorDocsIds: one.invigilators } : {}),
      })),
    } : {}),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('add-exam-paper', { label: 'Add a paper', pathParams: { year, examDocsId: exam.examDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onAdded(answer.bodyJson)
  }

  const nameOf = (id) => staff.find((one) => one.staffDocsId === id)?.fullName ?? id

  return (
    <Modal
      open
      onClose={onClose}
      title={`Add a paper — ${exam.name}`}
      description={`One subject for the sections ticked, at one date and time — one paper per section. The date must be inside ${exam.startDate} to ${exam.endDate} and a working day.`}
      endpoint={<EndpointTag id="add-exam-paper" name="Add" look="primary" pathParams={{ year, examDocsId: exam.examDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Add</Button>}
    >
      <div className="field-grid">
        <Field label="Class" required hint="A class of this year. Another id is 404 CLASS_NOT_FOUND.">
          <div className="picker-pair">
            <Select value={classes.some((one) => one.schoolClassId === classDocsId) ? classDocsId : ''}
              options={['', ...classes.map((one) => ({ value: one.schoolClassId, label: one.name }))]}
              onChange={pickClass} />
            <Input value={classDocsId} placeholder="classDocsId" onChange={(e) => setClassDocsId(e.target.value)} />
          </div>
        </Field>
        <Field label="Sections" required hint="Tick one or more. Typed ones are added too — a section the class lacks is 400 SECTION_NOT_IN_CLASS; the same one twice is 400 DUPLICATE_SECTION_IN_REQUEST.">
          <div>
            {sections.map((one) => (
              <label key={one.sectionNo} style={{ marginRight: 12 }}>
                <input type="checkbox" checked={ticked.includes(one.sectionNo)} onChange={() => toggle(one.sectionNo)} />
                {' '}{one.sectionNo}{one.active === false ? ' (off)' : ''}
              </label>
            ))}
            <Input value={extraSections} placeholder="more, comma separated" onChange={(e) => setExtraSections(e.target.value)} />
          </div>
        </Field>
        <Field label="Subject" required hint="Picked from the class's subjects, or typed. One not taught in a ticked section is 400 SUBJECT_NOT_TAUGHT_IN_SECTION, naming it.">
          <div className="picker-pair">
            <Select value={subjectOptions.includes(subjectCode) ? subjectCode : ''} options={['', ...subjectOptions]} onChange={setSubjectCode} />
            <Input value={subjectCode} placeholder="MATHEMATICS" onChange={(e) => setSubjectCode(e.target.value)} />
          </div>
        </Field>
        <Field label="Part" hint="Theory, Practical, Oral… Empty when the subject has one paper. The same subject and part twice for a section, ignoring case, is 409 PAPER_ALREADY_SCHEDULED.">
          <Input value={componentName} placeholder="Theory" onChange={(e) => setComponentName(e.target.value)} />
        </Field>
        <Field label="Date" required hint="Outside the exam is 409 PAPER_OUTSIDE_EXAM; a holiday is 404 NOT_A_WORKING_DAY.">
          <Input type="date" value={examDate} onChange={(e) => setExamDate(e.target.value)} />
        </Field>
        <Field label="Time" required hint="The start must be before the end (400 INVALID_TIME_RANGE).">
          <div className="picker-pair">
            <Input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            <Input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </div>
        </Field>
        <Field label="Marks" required hint="Maximum above 0; passing optional and not above the maximum (400 PASSING_MARKS_ABOVE_MAXIMUM).">
          <div className="picker-pair">
            <Input value={maximumMarks} placeholder="maximum" onChange={(e) => setMaximumMarks(e.target.value)} />
            <Input value={passingMarks} placeholder="passing" onChange={(e) => setPassingMarks(e.target.value)} />
          </div>
        </Field>
        <Field label="Rooms" hint="Optional while drafting. One room, or several when the students do not fit — each with how many sit there — all the ticked sections together, not one section — and who is on duty in it. A room is picked from the school's rooms, or its id typed. Unknown is 404 ROOM_NOT_FOUND; too many for it is 409 ROOM_OVER_CAPACITY; one holding another sitting is 409 ROOM_CLASH; the same room twice is 400 DUPLICATE_ROOM_IN_REQUEST.">
          <div className="stack">
            {rooms.map((one, index) => (
              <div key={index} className="card" style={{ padding: 8 }}>
                <div className="picker-pair">
                  <Select value={schoolRooms.some((room) => room.roomDocsId === one.roomDocsId) ? one.roomDocsId : ''}
                    options={['', ...schoolRooms.map((room) => ({
                      value: room.roomDocsId,
                      label: `${room.name}${room.capacity ? ` · ${room.capacity} seats` : ''}${room.status !== 'IN_USE' ? ` · ${room.status}` : ''}`,
                    }))]}
                    onChange={(id) => updateRoom(index, { roomDocsId: id })} />
                  <Input value={one.roomDocsId} placeholder="roomDocsId" onChange={(e) => updateRoom(index, { roomDocsId: e.target.value })} />
                  <Input value={one.seatCount} placeholder="seats, all sections" onChange={(e) => updateRoom(index, { seatCount: e.target.value })} />
                  <Button onClick={() => removeRoom(index)}>Remove room</Button>
                </div>
                {/* THE PEOPLE ALREADY ON DUTY IN THIS ROOM, then the picker for the next one below them. */}
                {/* One row per person: the name and the staff id, shown as text — to change one, remove
                    them and add again — and a Remove button. */}
                {one.invigilators.map((id) => (
                  <div key={id} className="picker-pair" style={{ alignItems: 'center' }}>
                    <b>{nameOf(id)}</b>
                    <span className="mono muted">{id}</span>
                    <Button onClick={() => updateRoom(index, { invigilators: one.invigilators.filter((x) => x !== id) })}>Remove</Button>
                  </div>
                ))}
                {/* PICKING A PERSON ONLY FILLS THE ID BOX. Nothing is added until "Add invigilator" is
                    clicked, and the id can be edited first — so an unknown id can still be sent. */}
                <div className="picker-pair">
                  <Select value={staff.some((person) => person.staffDocsId === one.typed) ? one.typed : ''}
                    options={['', ...staff.map((person) => ({ value: person.staffDocsId, label: person.fullName }))]}
                    onChange={(id) => updateRoom(index, { typed: id })} />
                  <Input value={one.typed} placeholder="staffDocsId" onChange={(e) => updateRoom(index, { typed: e.target.value })} />
                </div>
                <div>
                  <Button onClick={() => addInvigilator(index, one.typed.trim())}>Add invigilator</Button>
                </div>
              </div>
            ))}
            <Button onClick={addRoom}>Add a room</Button>
            <p className="muted">A person on duty in two rooms is 400 INVIGILATOR_IN_TWO_ROOMS; one already on duty at another sitting then is 409 INVIGILATOR_CLASH.</p>
          </div>
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} query={{ page: '0', size: '100' }} />{' '}
        <EndpointTag id="list-class-sections" name="Sections" pathParams={{ year, id: classDocsId }} />{' '}
        <EndpointTag id="list-class-subjects" name="Subjects" pathParams={{ year, id: classDocsId }} />{' '}
        <EndpointTag id="list-staff" name="Staff" query={{ size: '100' }} />{' '}
        <EndpointTag id="list-rooms" name="Rooms" query={{ size: '100' }} />
      </p>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
