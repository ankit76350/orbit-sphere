import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, CheckCircle2, ClipboardCheck, PenLine, RefreshCw, Upload } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'
import { readableDateTime } from '../../../lib/dates.js'

/**
 * One paper, on its own page: /school-academics/exams/{termId}/exams/{examId}/papers/{paperId}
 *
 * SEVEN ENDPOINTS — #12: the paper with its class, subject, rooms and invigilators, then who is
 * expected in the hall and how they were marked, then the marks entered for it. #14: the hall
 * register itself — every child expected, in roll order, with their hall entry or none. #15:
 * marking it, from the register's own rows. #16: completing the paper, which closes the hall. #18:
 * the mark sheet — every child with a hall entry, those present with their mark or none yet. #19:
 * entering marks, from the sheet's own rows. #22: publishing them — DRAFT → PUBLISHED, together.
 *
 * EXPECTED is the section's children on the paper's date who are ELIGIBLE candidates, plus anyone
 * already marked at this paper. Debarred children and children who are not candidates are counted
 * here, never listed on the hall register.
 */
export default function ExamPaper() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id: termId, examId, paperId } = useParams()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  //! THE HALL REGISTER (#14), read beside the paper.
  const [register, setRegister] = useState(null)
  const [registerProblem, setRegisterProblem] = useState(null)
  const [marking, setMarking] = useState(false)
  const [completing, setCompleting] = useState(false)
  //! THE MARK SHEET (#18), read beside the register.
  const [sheet, setSheet] = useState(null)
  const [sheetProblem, setSheetProblem] = useState(null)
  const [entering, setEntering] = useState(false)
  const [publishing, setPublishing] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-exam-paper', { label: 'One paper', pathParams: { year, examScheduleDocsId: paperId ?? '' } })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, paperId])

  useEffect(() => { load() }, [load])

  const loadRegister = useCallback(async () => {
    if (!actingSubdomain || !year) return
    const answer = await call('exam-hall-register', { label: 'The hall register', pathParams: { year, examScheduleDocsId: paperId ?? '' } })
    if (answer.ok) { setRegister(answer.bodyJson); setRegisterProblem(null) } else { setRegister(null); setRegisterProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, paperId])

  useEffect(() => { loadRegister() }, [loadRegister])

  const loadSheet = useCallback(async () => {
    if (!actingSubdomain || !year) return
    const answer = await call('exam-mark-sheet', { label: 'The mark sheet', pathParams: { year, examScheduleDocsId: paperId ?? '' } })
    if (answer.ok) { setSheet(answer.bodyJson); setSheetProblem(null) } else { setSheet(null); setSheetProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, paperId])

  useEffect(() => { loadSheet() }, [loadSheet])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const paper = data?.paper
  const hall = data?.hall
  const marks = data?.marks
  const title = paper
    ? `${paper.subjectName ?? paper.subjectCode}${paper.componentName ? ` (${paper.componentName})` : ''} — ${paper.className ?? ''} ${paper.sectionNo}`
    : 'Paper'

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{title}</h1>
          <p className="muted">{data?.examName ?? ''}{paper ? ` · ${paper.examDate} ${paper.startTime}–${paper.endTime}` : ''}</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(childPath('school', 'academics', 'exams', termId ?? '', 'exams', examId ?? ''))}>
          {data?.examName ?? 'Exam'}
        </Button>
        <EndpointTag id="get-exam-paper" name="Paper" pathParams={{ year, examScheduleDocsId: paperId }} />
        <Button icon={RefreshCw} onClick={() => { load(); loadRegister(); loadSheet() }} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {paper ? (
        <>
          <Card title="The paper">
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><th>Class</th><td>{paper.className ?? paper.classDocsId} — section {paper.sectionNo}</td></tr>
                  <tr><th>Subject</th><td>{paper.subjectName ?? paper.subjectCode} <span className="mono muted">{paper.subjectCode}</span></td></tr>
                  <tr><th>Part</th><td>{paper.componentName ?? <span className="muted">whole subject</span>}</td></tr>
                  <tr><th>When</th><td className="mono">{paper.examDate} {paper.startTime}–{paper.endTime}</td></tr>
                  <tr><th>Marks</th><td>{paper.maximumMarks}{paper.passingMarks != null ? ` (pass ${paper.passingMarks})` : ''}</td></tr>
                  <tr><th>Rooms</th><td>
                    {paper.rooms.length === 0 ? <span className="muted">no rooms yet</span> : paper.rooms.map((r) => (
                      <div key={r.roomDocsId}>
                        <b>{r.roomName ?? r.roomDocsId}</b> · {r.seatCount == null ? 'seats not given' : `${r.seatCount} seat${r.seatCount === 1 ? '' : 's'} for the sitting`}
                        {r.invigilators.length ? ` · ${r.invigilators.map((i) => i.fullName ?? i.staffDocsId).join(', ')}` : ''}
                      </div>
                    ))}
                    {paper.rooms.length ? <div className="muted">{paper.totalSeats} seats in all, shared by the sections of this sitting</div> : null}
                  </td></tr>
                  <tr><th>Status</th><td><Badge>{paper.status}</Badge></td></tr>
                  <tr><th>Version</th><td className="mono">{paper.version}</td></tr>
                  <tr><th>Id</th><td className="mono muted">{paper.examScheduleDocsId}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card title="In the hall" description="Who should sit this paper, and what the hall register says so far.">
            <div className="table-scroll">
              <table className="data-table">
                <tbody>
                  <tr><th>In the section that day</th><td>{hall.sectionSize}</td></tr>
                  <tr><th>Eligible candidates</th><td>{hall.eligible}</td></tr>
                  <tr><th>Debarred — not on the register</th><td>{hall.debarred}</td></tr>
                  <tr><th>Not candidates — not on the register</th><td>{hall.notCandidates}</td></tr>
                  <tr><th>Expected in the hall</th><td><b>{hall.expected}</b>{paper.rooms.length ? <span className="muted"> · {paper.totalSeats} seats</span> : null}</td></tr>
                  <tr><th>Present</th><td>{hall.present}</td></tr>
                  <tr><th>Absent</th><td>{hall.absent}</td></tr>
                  <tr><th>Unfair means</th><td>{hall.unfairMeans}</td></tr>
                  <tr><th>Exempt</th><td>{hall.exempt}</td></tr>
                  <tr><th>Not marked yet</th><td>{hall.notMarked}</td></tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card
            title={register ? `Hall register — ${register.counts.expected} expected` : 'Hall register'}
            description="Every eligible candidate of the section on the paper's date, in roll order, with their hall entry — plus anyone already marked here. Debarred children are counted, never named."
            action={(
              <>
                <EndpointTag id="exam-hall-register" name="Register" pathParams={{ year, examScheduleDocsId: paperId }} />{' '}
                <Button icon={ClipboardCheck} onClick={() => setMarking(true)}>Mark the hall</Button>{' '}
                <Button icon={CheckCircle2} onClick={() => setCompleting(true)}>Complete the paper</Button>
              </>
            )}
          >
            {registerProblem ? (
              <pre className="resp-body">{registerProblem.bodyJson?.message ?? registerProblem.bodyText}</pre>
            ) : !register ? null : (
              <>
                <p>
                  <b>{register.counts.marked}</b> marked, <b>{register.counts.notMarked}</b> not marked
                  {Object.entries(register.counts.byStatus).filter(([, count]) => count > 0).map(([status, count]) => (
                    <span key={status}>{'  '}<Badge>{status}</Badge> {count}</span>
                  ))}
                  {register.counts.debarred ? <span className="muted">{'  '}· {register.counts.debarred} debarred, not listed</span> : null}
                  {register.counts.notCandidates ? <span className="muted">{'  '}· {register.counts.notCandidates} in the section but not candidates</span> : null}
                </p>
                {register.students.length === 0 ? (
                  <p className="muted">Nobody is expected. Add the section's candidates on the exam page.</p>
                ) : (
                  <div className="table-scroll">
                    <table className="data-table">
                      <thead>
                        <tr><th>Roll</th><th>Name</th><th>Admission no.</th><th>Hall</th><th>Answer Copy No</th><th>Room · seat</th><th>In · out</th><th>Marked</th></tr>
                      </thead>
                      <tbody>
                        {register.students.map((one) => (
                          <tr key={one.studentDocsId}>
                            <td>{one.rollNo ?? '—'}</td>
                            <td>
                              {one.fullName ?? one.studentDocsId}
                              {one.candidateStatus === 'DEBARRED' ? <div className="muted">debarred after sitting</div> : null}
                              {one.inSection ? null : <div className="muted">has left the section</div>}
                            </td>
                            <td>{one.admissionNo ?? '—'}</td>
                            <td>{one.entry ? <Badge>{one.entry.attendanceStatus}</Badge> : <span className="muted">not marked</span>}</td>
                            <td>
                              {one.entry?.answerCopyNo ?? '—'}
                              {one.entry?.additionalAnswerCopyNos?.length ? <div className="muted">+ {one.entry.additionalAnswerCopyNos.join(', ')}</div> : null}
                            </td>
                            <td>{one.entry ? `${one.entry.roomName ?? one.entry.roomDocsId ?? '—'} · ${one.entry.seatNo ?? '—'}` : '—'}</td>
                            <td>
                              {one.entry?.reportedAt ? <div>{readableDateTime(one.entry.reportedAt)}{one.entry.late ? <b> · late</b> : null}</div> : null}
                              {one.entry?.submittedAt ? <div>{readableDateTime(one.entry.submittedAt)}{one.entry.leftEarly ? <b> · left early</b> : null}</div> : null}
                              {!one.entry?.reportedAt && !one.entry?.submittedAt ? '—' : null}
                            </td>
                            <td className="muted">{one.entry?.markedAt ? readableDateTime(one.entry.markedAt) : '—'}{one.entry?.remarks ? <div>{one.entry.remarks}</div> : null}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </Card>

          <Card
            title={sheet ? `Mark sheet — ${sheet.counts.entered} of ${sheet.counts.present} entered` : 'Mark sheet'}
            description="Every child with a hall entry, in roll order. Only a child marked PRESENT takes a mark; the rest show their hall status."
            action={(
              <>
                <EndpointTag id="exam-mark-sheet" name="Mark sheet" pathParams={{ year, examScheduleDocsId: paperId }} />{' '}
                <Button icon={PenLine} onClick={() => setEntering(true)}>Enter marks</Button>{' '}
                <Button icon={Upload} onClick={() => setPublishing(true)}>Publish marks</Button>
              </>
            )}
          >
            {sheetProblem ? (
              <pre className="resp-body">{sheetProblem.bodyJson?.message ?? sheetProblem.bodyText}</pre>
            ) : !sheet ? null : (
              <>
                <p>
                  Out of <b>{sheet.maximumMarks}</b>{sheet.passingMarks != null ? <>, pass at <b>{sheet.passingMarks}</b></> : null}.
                  {' '}{sheet.gradingScheme
                    ? <>Grades from <b>{sheet.gradingScheme.name}</b> ({sheet.gradingScheme.scaleType}, the {sheet.gradingScheme.from === 'SUBJECT' ? "subject's" : "exam's"} scheme): {sheet.gradingScheme.gradeCodes.join(', ')}.</>
                    : <span className="muted">No grading scheme — neither the subject nor the exam names one.</span>}
                </p>
                <p>
                  <b>{sheet.counts.entered}</b> entered, <b>{sheet.counts.notEntered}</b> not entered
                  {Object.entries(sheet.counts.byMarkStatus).filter(([, count]) => count > 0).map(([status, count]) => (
                    <span key={status}>{'  '}<Badge>{status}</Badge> {count}</span>
                  ))}
                  {sheet.status !== 'COMPLETED' ? <span className="muted">{'  '}· the paper is {sheet.status} — marks are entered once it is COMPLETED</span> : null}
                </p>
                {sheet.students.length === 0 ? (
                  <p className="muted">Nobody has a hall entry on this paper yet — mark the hall first.</p>
                ) : (
                  <div className="table-scroll">
                    <table className="data-table">
                      <thead>
                        <tr><th>Roll</th><th>Name</th><th>Hall</th><th>Obtained</th><th>Grace</th><th>Effective</th><th>Grade</th><th>Status</th><th>Entered</th></tr>
                      </thead>
                      <tbody>
                        {sheet.students.map((one) => (
                          <tr key={one.studentDocsId}>
                            <td>{one.rollNo ?? '—'}</td>
                            <td>{one.fullName ?? one.studentDocsId}<div className="muted">{one.admissionNo ?? ''}</div></td>
                            <td><Badge>{one.attendanceStatus}</Badge></td>
                            {one.mark ? (
                              <>
                                <td>{one.mark.obtainedMarks ?? '—'}</td>
                                <td>{one.mark.graceMarks ?? '—'}</td>
                                <td><b>{one.mark.effectiveMarks ?? '—'}</b></td>
                                <td>{one.mark.gradeCode ?? '—'}</td>
                                <td><Badge>{one.mark.status}</Badge></td>
                                <td className="muted">{one.mark.enteredAt ? readableDateTime(one.mark.enteredAt) : '—'}</td>
                              </>
                            ) : (
                              <td colSpan={6} className="muted">{one.canBeMarked ? 'not entered' : 'no mark — did not sit'}</td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </Card>

          <Card title={`Marks — ${marks.entered} entered`} description="Only a child marked present gets a mark.">
            <p>
              {Object.entries(marks.byStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
            <p className="muted">Present with no mark yet: {marks.notEntered}</p>
          </Card>
        </>
      ) : null}

      {publishing && sheet ? (
        <PublishMarks
          year={year}
          sheet={sheet}
          onClose={() => setPublishing(false)}
          onPublished={(next) => { setPublishing(false); setSheet(next); load() }}
        />
      ) : null}

      {entering && sheet ? (
        <EnterMarks
          year={year}
          sheet={sheet}
          onClose={() => setEntering(false)}
          onSaved={(next) => { setSheet(next); load() }}
        />
      ) : null}

      {completing && register ? (
        <CompletePaper
          year={year}
          register={register}
          onClose={() => setCompleting(false)}
          onCompleted={(next) => { setCompleting(false); setRegister(next); load() }}
        />
      ) : null}

      {marking && register ? (
        <MarkHall
          year={year}
          register={register}
          onClose={() => setMarking(false)}
          onSaved={(next) => { setRegister(next); load() }}
        />
      ) : null}
    </div>
  )
}

/**
 * Publishing a paper's marks — #22. Open whatever the sheet says, so every refusal can be seen.
 */
function PublishMarks({ year, sheet, onClose, onPublished }) {
  const { call } = useApi()
  const [version, setVersion] = useState(String(sheet.version ?? ''))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const body = { version: version === '' ? undefined : Number(version) }

  const send = async () => {
    setSending(true)
    const answer = await call('publish-exam-marks', { label: 'Publish marks', pathParams: { year, examScheduleDocsId: sheet.examScheduleDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onPublished(answer.bodyJson)
  }

  const moved = Object.entries(sheet.counts.byMarkStatus).filter(([status, count]) => status !== 'DRAFT' && count > 0).map(([status]) => status)

  return (
    <Modal
      open
      onClose={onClose}
      title={`Publish marks — ${sheet.subjectName ?? sheet.subjectCode} ${sheet.className ?? ''} ${sheet.sectionNo}`}
      description="Every mark on the paper moves DRAFT → PUBLISHED together. They are final from here — entering marks again is refused."
      endpoint={<EndpointTag id="publish-exam-marks" name="Publish" look="primary" pathParams={{ year, examScheduleDocsId: sheet.examScheduleDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Publish</Button>}
    >
      <p className="muted">
        This paper is <b>{sheet.status}</b>{sheet.status !== 'COMPLETED' ? ' — expect 409 PAPER_NOT_COMPLETED' : ''}.
        {' '}{sheet.counts.notEntered
          ? `${sheet.counts.notEntered} of ${sheet.counts.present} who sat have no mark — expect 409 UNENTERED_MARKS.`
          : sheet.counts.present
            ? `All ${sheet.counts.present} who sat have a mark.`
            : 'Nobody sat this paper — expect 409 NO_MARKS_TO_PUBLISH.'}
        {moved.length ? ` The marks are already ${moved.join(', ')} — expect 409 MARKS_NOT_EDITABLE.` : ''}
      </p>
      <div className="field-grid">
        <Field label="The paper's version" required hint="Filled from the mark sheet. Change it to see 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
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
 * Entering marks — #19. One row per child on the sheet, filled from their mark when they have one.
 * ONLY THE TICKED ROWS ARE SENT. Every child is listed — not only those PRESENT — so 400
 * STUDENT_NOT_PRESENT can be seen; a child id can also be added by hand. STAYS OPEN AFTER A SAVE
 * with the new version, because marks are entered in rounds.
 */
function EnterMarks({ year, sheet, onClose, onSaved }) {
  const { call } = useApi()
  const rowOf = (one) => ({
    studentDocsId: one.studentDocsId,
    name: one.fullName ?? one.studentDocsId,
    hall: one.attendanceStatus ?? '—',
    send: false,
    obtainedMarks: one.mark?.obtainedMarks != null ? String(one.mark.obtainedMarks) : '',
    gradeCode: one.mark?.obtainedMarks == null ? (one.mark?.gradeCode ?? '') : '',
    remarks: one.mark?.remarks ?? '',
  })
  const [rows, setRows] = useState(() => sheet.students.map(rowOf))
  const [typed, setTyped] = useState('')
  const [version, setVersion] = useState(String(sheet.version ?? ''))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const update = (index, change) => setRows((old) => old.map((one, i) => (i === index ? { ...one, ...change, send: true } : one)))
  const addTyped = () => {
    const id = typed.trim()
    if (id) setRows((old) => [...old, { ...rowOf({ studentDocsId: id }), send: true }])
    setTyped('')
  }

  //! WHAT IS SENT: the ticked rows, each with only its filled boxes. A mark typed as text that is not
  //! a number is sent as typed, so the server's own refusal can be seen.
  const body = {
    marks: rows.filter((one) => one.send).map((one) => ({
      studentDocsId: one.studentDocsId,
      ...(one.obtainedMarks !== '' ? { obtainedMarks: Number.isNaN(Number(one.obtainedMarks)) ? one.obtainedMarks : Number(one.obtainedMarks) } : {}),
      ...(one.gradeCode ? { gradeCode: one.gradeCode } : {}),
      ...(one.remarks ? { remarks: one.remarks } : {}),
    })),
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('enter-exam-marks', { label: 'Enter marks', pathParams: { year, examScheduleDocsId: sheet.examScheduleDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) {
      setVersion(String(answer.bodyJson.version ?? ''))
      setRows((old) => old.map((one) => ({ ...one, send: false })))
      onSaved(answer.bodyJson)
    }
  }

  const gradeOptions = ['', ...(sheet.gradingScheme?.gradeCodes ?? [])]

  return (
    <Modal
      open
      onClose={onClose}
      title={`Enter marks — ${sheet.subjectName ?? sheet.subjectCode} ${sheet.className ?? ''} ${sheet.sectionNo}`}
      description="Tick the children to send. Marks OR a grade, not both — the grade is worked out from the marks. Re-sending a child corrects them."
      endpoint={<EndpointTag id="enter-exam-marks" name="Save" look="primary" pathParams={{ year, examScheduleDocsId: sheet.examScheduleDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Save</Button>}
    >
      <p className="muted">
        This paper is <b>{sheet.status}</b>{sheet.status !== 'COMPLETED' ? ' — marks need a COMPLETED paper (409 PAPER_NOT_COMPLETED)' : ''}.
        {' '}Out of <b>{sheet.maximumMarks}</b> (0 to {sheet.maximumMarks}, else 400 MARKS_OUT_OF_RANGE).
        {' '}{sheet.gradingScheme ? `Grades come from ${sheet.gradingScheme.name}.` : 'No grading scheme, so a grade cannot be sent (400 GRADE_NOT_IN_SCHEME).'}
        {' '}Only a child PRESENT in the hall gets a mark (400 STUDENT_NOT_PRESENT). Published marks are 409 MARKS_NOT_EDITABLE.
      </p>
      <div className="field-grid">
        <Field label="The paper's version" required hint="Filled from the mark sheet, and moved on after each save. Change it to see 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            <tr><th>Send</th><th>Child</th><th>Hall</th><th>Obtained marks</th><th>Or grade</th><th>Remarks</th></tr>
          </thead>
          <tbody>
            {rows.map((one, index) => (
              <tr key={one.studentDocsId}>
                <td><input type="checkbox" checked={one.send} onChange={() => setRows((old) => old.map((x, i) => (i === index ? { ...x, send: !x.send } : x)))} /></td>
                <td>{one.name}</td>
                <td><Badge>{one.hall}</Badge></td>
                <td><Input value={one.obtainedMarks} placeholder={`0 to ${sheet.maximumMarks}`} onChange={(e) => update(index, { obtainedMarks: e.target.value })} /></td>
                <td><Select value={gradeOptions.includes(one.gradeCode) ? one.gradeCode : ''} options={gradeOptions} onChange={(v) => update(index, { gradeCode: v })} /></td>
                <td><Input value={one.remarks} onChange={(e) => update(index, { remarks: e.target.value })} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="picker-pair">
        <Input value={typed} placeholder="another studentDocsId" onChange={(e) => setTyped(e.target.value)} />
        <Button onClick={addTyped}>Add by id</Button>
      </div>
      {result?.ok ? <p className="muted">Saved — {result.bodyJson.counts.entered} of {result.bodyJson.counts.present} present now have a mark.</p> : null}
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
 * Closing the paper's hall — #16. Open whatever the paper's state, so every refusal can be seen.
 */
function CompletePaper({ year, register, onClose, onCompleted }) {
  const { call } = useApi()
  const [version, setVersion] = useState(String(register.version ?? ''))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const body = { version: version === '' ? undefined : Number(version) }

  const send = async () => {
    setSending(true)
    const answer = await call('complete-exam-paper', { label: 'Complete the paper', pathParams: { year, examScheduleDocsId: register.examScheduleDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onCompleted(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Complete ${register.subjectName ?? register.subjectCode} ${register.className ?? ''} ${register.sectionNo}`}
      description="The paper becomes COMPLETED and its hall is closed — no more hall marks after this."
      endpoint={<EndpointTag id="complete-exam-paper" name="Complete" look="primary" pathParams={{ year, examScheduleDocsId: register.examScheduleDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Complete</Button>}
    >
      <p className="muted">
        This paper is <b>{register.status}</b> on {register.examDate}.
        {' '}{register.counts.notMarked
          ? `${register.counts.notMarked} of ${register.counts.expected} on the register are not marked, so expect 409 UNMARKED_STUDENTS.`
          : `Everyone on the register is marked (${register.counts.marked} of ${register.counts.expected}).`}
        {register.counts.debarred ? ` ${register.counts.debarred} debarred — they do not hold it up.` : ''}
        {' '}EXEMPT counts as marked. Only a PUBLISHED paper, from its day.
      </p>
      <div className="field-grid">
        <Field label="The paper's version" required hint="Filled from the register. Change it to see 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
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
 * A stored instant as the picker wants it — "2026-11-13T09:05", in this browser's time. Empty for none.
 */
function toPicker(instant) {
  if (!instant) return ''
  const at = new Date(instant)
  if (Number.isNaN(at.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${at.getFullYear()}-${pad(at.getMonth() + 1)}-${pad(at.getDate())}T${pad(at.getHours())}:${pad(at.getMinutes())}`
}

/**
 * The picker's local time as the instant the API takes — "2026-11-13T03:35:00.000Z". A value the
 * browser cannot read is sent as typed, so the server's own refusal can be seen.
 */
function fromPicker(local) {
  const at = new Date(local)
  return Number.isNaN(at.getTime()) ? local : at.toISOString()
}

/**
 * Marking the hall — #15. One row per child on the register, filled from their entry when they
 * have one. ONLY THE TICKED ROWS ARE SENT, so one child, several or all can be marked. A child
 * id can be added by hand, so 400 STUDENT_NOT_IN_PAPER can be seen. STAYS OPEN AFTER A SAVE with
 * the new version, because a hall is marked in rounds as children come in.
 */
function MarkHall({ year, register, onClose, onSaved }) {
  const { call } = useApi()
  const rowOf = (one) => ({
    studentDocsId: one.studentDocsId,
    name: one.fullName ?? one.studentDocsId,
    send: false,
    attendanceStatus: one.entry?.attendanceStatus ?? 'PRESENT',
    answerCopyNo: one.entry?.answerCopyNo ?? '',
    additional: (one.entry?.additionalAnswerCopyNos ?? []).join(', '),
    roomDocsId: one.entry?.roomDocsId ?? '',
    seatNo: one.entry?.seatNo ?? '',
    reportedAt: toPicker(one.entry?.reportedAt),
    submittedAt: toPicker(one.entry?.submittedAt),
    remarks: one.entry?.remarks ?? '',
  })
  const [rows, setRows] = useState(() => register.students.map(rowOf))
  const [typed, setTyped] = useState('')
  const [version, setVersion] = useState(String(register.version ?? ''))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const update = (index, change) => setRows((old) => old.map((one, i) => (i === index ? { ...one, ...change } : one)))
  const addTyped = () => {
    const id = typed.trim()
    if (id) setRows((old) => [...old, { ...rowOf({ studentDocsId: id }), send: true }])
    setTyped('')
  }

  //! WHAT IS SENT: the ticked rows, each with only its filled boxes. An empty box sends nothing.
  const body = {
    marks: rows.filter((one) => one.send).map((one) => ({
      studentDocsId: one.studentDocsId,
      attendanceStatus: one.attendanceStatus || undefined,
      ...(one.answerCopyNo ? { answerCopyNo: one.answerCopyNo } : {}),
      ...(one.additional.trim() ? { additionalAnswerCopyNos: one.additional.split(',').map((x) => x.trim()).filter(Boolean) } : {}),
      ...(one.roomDocsId ? { roomDocsId: one.roomDocsId } : {}),
      ...(one.seatNo ? { seatNo: one.seatNo } : {}),
      ...(one.reportedAt ? { reportedAt: fromPicker(one.reportedAt) } : {}),
      ...(one.submittedAt ? { submittedAt: fromPicker(one.submittedAt) } : {}),
      ...(one.remarks ? { remarks: one.remarks } : {}),
    })),
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('mark-exam-hall', { label: 'Mark the hall', pathParams: { year, examScheduleDocsId: register.examScheduleDocsId }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) {
      setVersion(String(answer.bodyJson.version ?? ''))
      setRows((old) => old.map((one) => ({ ...one, send: false })))
      onSaved(answer.bodyJson)
    }
  }

  const roomOptions = ['', ...register.rooms.map((room) => ({ value: room.roomDocsId, label: room.roomName ?? room.roomDocsId }))]

  return (
    <Modal
      open
      onClose={onClose}
      title={`Mark the hall — ${register.subjectName ?? register.subjectCode} ${register.className ?? ''} ${register.sectionNo}`}
      description="Tick the children to send. Re-sending a child replaces their whole entry. Everything is checked before anything is saved."
      endpoint={<EndpointTag id="mark-exam-hall" name="Save" look="primary" pathParams={{ year, examScheduleDocsId: register.examScheduleDocsId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Save</Button>}
    >
      <p className="muted">
        This paper is <b>{register.status}</b> on {register.examDate}
        {register.status !== 'PUBLISHED' ? ' — only a PUBLISHED paper takes hall marks (409 PAPER_NOT_OPEN)' : ''}.
        Before its day is 409 PAPER_NOT_YET_SAT.
        PRESENT and UNFAIR_MEANS need a Answer Copy No; ABSENT and EXEMPT must not have one. A Answer Copy No is used once in the whole exam (409 ANSWER_COPY_TAKEN).
        {register.rooms.length > 1 ? ' This paper has several rooms, so a child who sat needs one (400 ROOM_REQUIRED).' : ' With one room, it is filled in for a child who sat.'}
        {' '}In and Out are picked in this browser's own time and sent as UTC instants — see the preview.
      </p>
      <div className="field-grid">
        <Field label="The paper's version" required hint="Filled from the register, and moved on after each save. Change it to see 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            <tr><th>Send</th><th>Child</th><th>Hall</th><th>Answer Copy No</th><th>Extra Answer Copy Nos</th><th>Room</th><th>Seat</th><th>In</th><th>Out</th><th>Remarks</th></tr>
          </thead>
          <tbody>
            {rows.map((one, index) => (
              <tr key={one.studentDocsId}>
                <td><input type="checkbox" checked={one.send} onChange={() => update(index, { send: !one.send })} /></td>
                <td>{one.name}</td>
                <td><Select value={one.attendanceStatus} options={['', 'PRESENT', 'ABSENT', 'UNFAIR_MEANS', 'EXEMPT']} onChange={(v) => update(index, { attendanceStatus: v, send: true })} /></td>
                <td><Input value={one.answerCopyNo} placeholder="MATH-0041" onChange={(e) => update(index, { answerCopyNo: e.target.value, send: true })} /></td>
                <td><Input value={one.additional} placeholder="comma separated" onChange={(e) => update(index, { additional: e.target.value, send: true })} /></td>
                <td><Select value={one.roomDocsId} options={roomOptions} onChange={(v) => update(index, { roomDocsId: v, send: true })} /></td>
                <td><Input value={one.seatNo} placeholder="H2-14" onChange={(e) => update(index, { seatNo: e.target.value, send: true })} /></td>
                <td><Input type="datetime-local" value={one.reportedAt} onChange={(e) => update(index, { reportedAt: e.target.value, send: true })} /></td>
                <td><Input type="datetime-local" value={one.submittedAt} onChange={(e) => update(index, { submittedAt: e.target.value, send: true })} /></td>
                <td><Input value={one.remarks} onChange={(e) => update(index, { remarks: e.target.value, send: true })} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="picker-pair">
        <Input value={typed} placeholder="another studentDocsId" onChange={(e) => setTyped(e.target.value)} />
        <Button onClick={addTyped}>Add by id</Button>
      </div>
      {result?.ok ? <p className="muted">Saved — the register now has {result.bodyJson.counts.marked} marked and {result.bodyJson.counts.notMarked} not marked.</p> : null}
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
