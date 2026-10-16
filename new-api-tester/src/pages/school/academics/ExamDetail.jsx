import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Pencil, RefreshCw } from 'lucide-react'
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
 * TWO ENDPOINTS — #7 reads the exam: its facts, its papers per status and its marks per status.
 * #2 fixes it: Edit opens a modal, the boxes on one side and the request on the other.
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
        <Button look="primary" icon={Pencil} onClick={() => setEditing(true)}>Edit</Button>
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

          <Card title={`Marks — ${data.markTotal}`} description="Every mark entered for the exam, by status.">
            <p>
              {Object.entries(data.marksByStatus).map(([status, count]) => (
                <span key={status}><Badge>{status}</Badge> {count}{'  '}</span>
              ))}
            </p>
          </Card>
        </>
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
