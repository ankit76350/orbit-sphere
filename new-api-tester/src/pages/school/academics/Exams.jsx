import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The exams of one academic year: /school-academics/exams
 *
 * ONE ENDPOINT SO FAR — #1 creates an exam. It always starts as DRAFT. The list of the year's exams
 * is #6, which is not built yet, so the table below shows only the exams created on this page
 * during this visit.
 *
 * THE TERM AND THE GRADING SCHEME are picked from the year's terms and the school's schemes, or
 * typed — so a wrong id can still be sent to see 404 TERM_NOT_FOUND or GRADING_SCHEME_NOT_FOUND.
 *
 * NOTHING IS DISABLED — dates the wrong way round are 400 INVALID_DATE_RANGE, dates outside the
 * term are 409 EXAM_DATES_OUTSIDE_TERM, and a name already used this year (any case) is 409
 * EXAM_NAME_TAKEN.
 */

const EXAM_TYPES = ['UNIT_TEST', 'FORMATIVE', 'SUMMATIVE', 'MIDTERM', 'FINAL', 'PRACTICAL',
  'PRE_BOARD', 'BOARD']

export default function Exams() {
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const year = actingAcademicYear ?? ''
  const [open, setOpen] = useState(false)
  //! THE EXAMS MADE HERE, newest first. Only until #6 can list the year's exams.
  const [made, setMade] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen />

  return (
    <Card
      title="Exams"
      description={`The exam events of ${year || 'the selected year'} — "Half Yearly Examination, Term 1". The papers of an exam (the datesheet) come next, with #8.`}
      action={<Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create an exam</Button>}
    >
      {made.length === 0 ? (
        <Empty title="No exam created yet on this page"
          description="The list of the year's exams is #6, which is not built yet. Exams you create here are shown below." />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Name</th><th>Type</th><th>Term</th><th>Dates</th><th>Weight</th><th>Scheme</th><th>Status</th><th>Id</th></tr>
            </thead>
            <tbody>
              {made.map((one) => (
                <tr key={one.examDocsId}>
                  <td><b>{one.name}</b></td>
                  <td>{one.examType}</td>
                  <td>{one.termName ?? <span className="mono muted">{one.termDocsId}</span>}</td>
                  <td><span className="mono">{one.startDate}</span> to <span className="mono">{one.endDate}</span></td>
                  <td>{one.weightPercent != null ? `${one.weightPercent}%` : <span className="muted">—</span>}</td>
                  <td>{one.gradingSchemeName ?? <span className="muted">—</span>}</td>
                  <td><Badge>{one.status}</Badge></td>
                  <td><span className="mono muted">{one.examDocsId}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {open ? (
        <CreateExam
          year={year}
          onClose={() => setOpen(false)}
          onCreated={(exam) => { setMade((old) => [exam, ...old]); setOpen(false) }}
        />
      ) : null}
    </Card>
  )
}

/**
 * Creating one exam — #1. The boxes on one side, what will be sent on the other.
 */
function CreateExam({ year, onClose, onCreated }) {
  const { call } = useApi()
  const [name, setName] = useState('')
  const [termDocsId, setTermDocsId] = useState('')
  const [examType, setExamType] = useState('MIDTERM')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [weightPercent, setWeightPercent] = useState('')
  const [gradingSchemeDocsId, setGradingSchemeDocsId] = useState('')
  const [terms, setTerms] = useState([])
  const [schemes, setSchemes] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE YEAR'S TERMS, for the term dropdown. Retired ones are listed too, so TERM_NOT_ACTIVE
  //! can be reached.
  useEffect(() => {
    let live = true
    call('list-academic-terms', { label: 'Terms to pick from', pathParams: { year }, query: { size: '100' } })
      .then((answer) => { if (live) setTerms(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, year])

  //! THE SCHOOL'S GRADING SCHEMES, for the scheme dropdown. Retired ones are listed too.
  useEffect(() => {
    let live = true
    call('list-grading-schemes', { label: 'Grading schemes to pick from', query: { size: '100' } })
      .then((answer) => { if (live) setSchemes(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call])

  //! PICKING A TERM fills both dates with the term's own, when they are still empty.
  const pickTerm = (id) => {
    setTermDocsId(id)
    const term = terms.find((one) => one.termDocsId === id)
    if (term) {
      if (!startDate) setStartDate(term.startDate)
      if (!endDate) setEndDate(term.endDate)
    }
  }

  // An empty optional box sends nothing rather than "".
  const body = {
    name,
    termDocsId,
    examType,
    startDate,
    endDate,
    ...(weightPercent !== '' ? { weightPercent: Number(weightPercent) } : {}),
    ...(gradingSchemeDocsId ? { gradingSchemeDocsId } : {}),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('create-exam', { label: 'Create an exam', pathParams: { year }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onCreated(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Create an exam — ${year}`}
      description="It starts as DRAFT. The dates must fall inside the term; the name must be free in the year, ignoring case."
      endpoint={<EndpointTag id="create-exam" name="Create" look="primary" pathParams={{ year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Create</Button>}
    >
      <div className="field-grid">
        <Field label="Name" required hint="Unique in the year, ignoring case. A cancelled exam keeps its name.">
          <Input value={name} placeholder="Half Yearly Examination, Term 1" onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Type" required>
          <Select value={examType} options={EXAM_TYPES} onChange={setExamType} />
        </Field>
        <Field label="Term" required hint="Picked from the year's terms, or typed. Picking one fills empty dates with the term's own. Another year's id is 404 TERM_NOT_FOUND; a retired term is 409 TERM_NOT_ACTIVE.">
          <div className="picker-pair">
            <Select value={terms.some((one) => one.termDocsId === termDocsId) ? termDocsId : ''}
              options={['', ...terms.map((one) => ({
                value: one.termDocsId,
                label: `${one.name} · ${one.startDate} to ${one.endDate}${one.active === false ? ' · retired' : ''}`,
              }))]}
              onChange={pickTerm} />
            <Input value={termDocsId} placeholder="termDocsId" onChange={(e) => setTermDocsId(e.target.value)} />
          </div>
        </Field>
        <Field label="Starts" required hint="Must be inside the term.">
          <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </Field>
        <Field label="Ends" required hint="Not before the start, and inside the term.">
          <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </Field>
        <Field label="Weight %" hint="Above 0 and at most 100. Leave empty when exams are not weighted. Whether the term adds up to 100 is checked when report cards are made.">
          <Input value={weightPercent} placeholder="40" onChange={(e) => setWeightPercent(e.target.value)} />
        </Field>
        <Field label="Grading scheme" hint="Optional — used for a subject that has no scheme of its own. A retired one is 409 GRADING_SCHEME_NOT_ACTIVE.">
          <div className="picker-pair">
            <Select value={schemes.some((one) => one.gradingSchemeDocsId === gradingSchemeDocsId) ? gradingSchemeDocsId : ''}
              options={['', ...schemes.map((one) => ({
                value: one.gradingSchemeDocsId,
                label: `${one.name} ${one.schemeVersion ?? ''}${one.active === false ? ' · retired' : ''}`,
              }))]}
              onChange={setGradingSchemeDocsId} />
            <Input value={gradingSchemeDocsId} placeholder="gradingSchemeDocsId" onChange={(e) => setGradingSchemeDocsId(e.target.value)} />
          </div>
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
