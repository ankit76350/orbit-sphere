import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'

/**
 * One exam, on its own page: /school-academics/exams/{termId}/exams/{examId}
 *
 * THREE ENDPOINTS — #7 reads the exam: its facts, its papers per status and its marks per status.
 * #2 fixes it: Edit opens a modal, the boxes on one side and the request on the other. #8 adds a
 * paper — one subject for one or more sections at one date and time. The list of an exam's papers
 * is #11, not built yet, so the page shows the papers added during this visit.
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
  //! THE PAPERS ADDED HERE, newest first. Only until #11 can list an exam's papers.
  const [added, setAdded] = useState([])

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-exam', { label: 'One exam', pathParams: { year, examDocsId: examId ?? '' } })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, examId])

  useEffect(() => { load() }, [load])

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
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
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

          <Card title={`Papers — ${data.paperTotal}`} description="Every paper of the exam, by status. Papers are added with #8, not built yet.">
            <p>
              {Object.entries(data.papersByStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
          </Card>

          <Card title="Papers added on this page" description="One row per section. The full list of an exam's papers is #11, not built yet.">
            {added.length === 0 ? (
              <p className="muted">None yet. Add a paper with the button at the top.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Class</th><th>Section</th><th>Subject</th><th>Component</th><th>Date</th><th>Time</th><th>Marks</th><th>Room</th><th>Invigilators</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {added.map((one) => (
                      <tr key={one.examScheduleDocsId}>
                        <td>{one.className ?? one.classDocsId}</td>
                        <td>{one.sectionNo}</td>
                        <td>{one.subjectName ?? one.subjectCode}<div className="mono muted">{one.subjectCode}</div></td>
                        <td>{one.componentName ?? <span className="muted">whole subject</span>}</td>
                        <td className="mono">{one.examDate}</td>
                        <td className="mono">{one.startTime}–{one.endTime}</td>
                        <td>{one.maximumMarks}{one.passingMarks != null ? ` (pass ${one.passingMarks})` : ''}</td>
                        <td>{one.roomName ?? (one.roomDocsId ? <span className="mono">{one.roomDocsId}</span> : <span className="muted">—</span>)}</td>
                        <td>{one.invigilators.length ? one.invigilators.map((i) => i.fullName ?? i.staffDocsId).join(', ') : <span className="muted">—</span>}</td>
                        <td><Badge>{one.status}</Badge></td>
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
          onAdded={(answer) => { setAdding(false); setAdded((old) => [...answer.papers, ...old]); load() }}
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
  const [roomDocsId, setRoomDocsId] = useState('')
  const [staff, setStaff] = useState([])
  const [invigilators, setInvigilators] = useState([])
  const [typedInvigilator, setTypedInvigilator] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE YEAR'S CLASSES AND THE SCHOOL'S STAFF, for the dropdowns.
  useEffect(() => {
    let live = true
    call('list-school-classes', { label: `Classes in ${year}`, pathParams: { year }, query: { page: '0', size: '100' } })
      .then((answer) => { if (live) setClasses(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    call('list-staff', { label: 'Staff to pick invigilators from', query: { size: '100' } })
      .then((answer) => { if (live) setStaff(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
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
  const addInvigilator = (id) => { if (id && !invigilators.includes(id)) setInvigilators((old) => [...old, id]) }

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
    ...(roomDocsId ? { roomDocsId } : {}),
    ...(invigilators.length ? { invigilatorDocsIds: invigilators } : {}),
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
        <Field label="Room id" hint="Optional. A room's roomDocsId — there is no rooms list in this tool yet. Unknown is 404 ROOM_NOT_FOUND; one holding another sitting then is 409 ROOM_CLASH.">
          <Input value={roomDocsId} placeholder="roomDocsId" onChange={(e) => setRoomDocsId(e.target.value)} />
        </Field>
        <Field label="Invigilators" hint="Picked from staff, or typed. Not staff is 404 STAFF_NOT_FOUND; on duty at another sitting then is 409 INVIGILATOR_CLASH.">
          <div>
            <div className="picker-pair">
              <Select value="" options={['', ...staff.map((one) => ({ value: one.staffDocsId, label: one.fullName }))]} onChange={addInvigilator} />
              <Input value={typedInvigilator} placeholder="staffDocsId, then Add" onChange={(e) => setTypedInvigilator(e.target.value)} />
              <Button onClick={() => { addInvigilator(typedInvigilator.trim()); setTypedInvigilator('') }}>Add</Button>
            </div>
            {invigilators.map((id) => (
              <span key={id} style={{ marginRight: 8 }}>
                <Badge>{nameOf(id)}</Badge>{' '}
                <Button onClick={() => setInvigilators((old) => old.filter((one) => one !== id))}>Remove</Button>
              </span>
            ))}
          </div>
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="list-school-classes" name="Classes" pathParams={{ year }} query={{ page: '0', size: '100' }} />{' '}
        <EndpointTag id="list-class-sections" name="Sections" pathParams={{ year, id: classDocsId }} />{' '}
        <EndpointTag id="list-class-subjects" name="Subjects" pathParams={{ year, id: classDocsId }} />{' '}
        <EndpointTag id="list-staff" name="Staff" query={{ size: '100' }} />
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
