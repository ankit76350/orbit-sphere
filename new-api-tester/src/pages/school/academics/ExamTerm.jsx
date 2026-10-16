import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Plus, RefreshCw, Search } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { screenPath } from '../../../paths.js'

/**
 * Exams, step 2 of 2: /school-academics/exams/{termId}
 *
 * ONE TERM'S EXAMS — #6 with termDocsId set to this term, plus the other filters (type, status,
 * part of the name) and paging. "Create an exam" is #1, with this term already filled in.
 *
 * NOTHING IS DISABLED — dates the wrong way round are 400 INVALID_DATE_RANGE, dates outside the
 * term are 409 EXAM_DATES_OUTSIDE_TERM, and a name already used this year (any case) is 409
 * EXAM_NAME_TAKEN. The term box can be changed to try another term, or a wrong id.
 */

const EXAM_TYPES = ['UNIT_TEST', 'FORMATIVE', 'SUMMATIVE', 'MIDTERM', 'FINAL', 'PRACTICAL',
  'PRE_BOARD', 'BOARD']
const STATUSES = ['DRAFT', 'SCHEDULED', 'PUBLISHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
const SORTS = ['', 'startDate', 'startDate,desc', 'endDate', 'name', 'name,desc', 'createdAt,desc']

export default function ExamTerm() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id: termId } = useParams()
  const year = actingAcademicYear ?? ''

  const [terms, setTerms] = useState([])
  const [examType, setExamType] = useState('')
  const [status, setStatus] = useState('')
  //! Typed, then SENT. `search` is what the last request used; `typed` is what the box holds.
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)

  //! THE YEAR'S TERMS — this term's name and dates for the header, and the create form's dropdown.
  useEffect(() => {
    if (!actingSubdomain || !year) return undefined
    let live = true
    call('list-academic-terms', { label: 'Terms of the year', pathParams: { year }, query: { size: '100' } })
      .then((answer) => { if (live) setTerms(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, actingSubdomain, year])

  const term = terms.find((one) => one.termDocsId === termId)

  //! THE QUERY #6 IS SENT. An empty filter sends nothing at all.
  const query = useMemo(() => {
    const out = { termDocsId: termId ?? '', page: String(page), size: '20' }
    if (examType) out.examType = examType
    if (status) out.status = status
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [termId, page, examType, status, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('list-exams', { label: "The term's exams", pathParams: { year }, query })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, query])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{term?.name ?? 'Term'} — exams</h1>
          <p className="muted">
            {term ? <><span className="mono">{term.startDate}</span> to <span className="mono">{term.endDate}</span>{term.active === false ? ' · retired' : ''}</> : <span className="mono">{termId}</span>}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(screenPath('school', 'academics', 'exams'))}>Terms</Button>
        <EndpointTag id="list-academic-terms" name="Terms" pathParams={{ year }} query={{ size: '100' }} />
        <EndpointTag id="list-exams" name="Exams" pathParams={{ year }} query={query} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create an exam</Button>
      </div>

      <Card title="Filters" description="All optional. Empty sends nothing.">
        <div className="field-grid">
          <Field label="Type">
            <Select value={examType} options={['', ...EXAM_TYPES]} onChange={(v) => { setPage(0); setExamType(v) }} />
          </Field>
          <Field label="Status">
            <Select value={status} options={['', ...STATUSES]} onChange={(v) => { setPage(0); setStatus(v) }} />
          </Field>
          <Field label="Name contains" hint="Ignoring case. A stray '(' is an empty result, not a 500.">
            <div className="picker-pair">
              <Input value={typed} placeholder="unit" onChange={(e) => setTyped(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { setPage(0); setSearch(typed) } }} />
              <Button icon={Search} onClick={() => { setPage(0); setSearch(typed) }}>Search</Button>
            </div>
          </Field>
          <Field label="Sort" hint="Any other field is 400 INVALID_SORT_FIELD.">
            <Select value={sort} options={SORTS} onChange={(v) => { setPage(0); setSort(v) }} />
          </Field>
        </div>
      </Card>

      <Card title={data ? `${data.totalElements} exam${data.totalElements === 1 ? '' : 's'}` : 'Exams'}>
        {problem ? (
          <>
            <div className="resp-head"><span className="resp-status" data-ok="false">{problem.bodyJson?.code ?? problem.status}</span></div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </>
        ) : rows.length === 0 && data ? (
          <Empty title="No exams" description="No exam in this term matches. Create one with the button above." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Name</th><th>Type</th><th>Dates</th><th>Weight</th><th>Scheme</th><th>Papers</th><th>Status</th></tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.examDocsId}>
                    <td><b>{one.name}</b><div className="mono muted">{one.examDocsId}</div></td>
                    <td>{one.examType}</td>
                    <td><span className="mono">{one.startDate}</span> to <span className="mono">{one.endDate}</span></td>
                    <td>{one.weightPercent != null ? `${one.weightPercent}%` : <span className="muted">—</span>}</td>
                    <td>{one.gradingSchemeName ?? <span className="muted">—</span>}</td>
                    <td>{one.paperCount}</td>
                    <td><Badge>{one.status}</Badge></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && data.totalPages > 1 ? (
          <div className="toolbar">
            <Button onClick={() => setPage(page - 1)}>Previous</Button>
            <span className="muted">Page {data.page + 1} of {data.totalPages}</span>
            <Button onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        ) : null}
      </Card>

      {open ? (
        <CreateExam
          year={year}
          terms={terms}
          initialTermDocsId={termId ?? ''}
          onClose={() => setOpen(false)}
          onCreated={() => { setOpen(false); load() }}
        />
      ) : null}
    </div>
  )
}

/**
 * Creating one exam — #1, with this term filled in. The boxes on one side, what will be sent on
 * the other.
 */
function CreateExam({ year, terms, initialTermDocsId, onClose, onCreated }) {
  const { call } = useApi()
  const startTerm = terms.find((one) => one.termDocsId === initialTermDocsId)
  const [name, setName] = useState('')
  const [termDocsId, setTermDocsId] = useState(initialTermDocsId)
  const [examType, setExamType] = useState('UNIT_TEST')
  const [startDate, setStartDate] = useState(startTerm?.startDate ?? '')
  const [endDate, setEndDate] = useState(startTerm?.endDate ?? '')
  const [weightPercent, setWeightPercent] = useState('')
  const [gradingSchemeDocsId, setGradingSchemeDocsId] = useState('')
  const [schemes, setSchemes] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE SCHOOL'S GRADING SCHEMES, for the scheme dropdown. Retired ones are listed too.
  useEffect(() => {
    let live = true
    call('list-grading-schemes', { label: 'Grading schemes to pick from', query: { size: '100' } })
      .then((answer) => { if (live) setSchemes(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call])

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
      title={`Create an exam — ${startTerm?.name ?? year}`}
      description="It starts as DRAFT. The dates start as the term's own and must stay inside it; the name must be free in the year, ignoring case."
      endpoint={<EndpointTag id="create-exam" name="Create" look="primary" pathParams={{ year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Create</Button>}
    >
      <div className="field-grid">
        <Field label="Name" required hint="Unique in the year, ignoring case. A cancelled exam keeps its name.">
          <Input value={name} placeholder="Unit Test 1" onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Type" required>
          <Select value={examType} options={EXAM_TYPES} onChange={setExamType} />
        </Field>
        <Field label="Term" required hint="This term, filled in. Change it to try another, or type an id. Another year's id is 404 TERM_NOT_FOUND; a retired term is 409 TERM_NOT_ACTIVE.">
          <div className="picker-pair">
            <Select value={terms.some((one) => one.termDocsId === termDocsId) ? termDocsId : ''}
              options={['', ...terms.map((one) => ({
                value: one.termDocsId,
                label: `${one.name} · ${one.startDate} to ${one.endDate}${one.active === false ? ' · retired' : ''}`,
              }))]}
              onChange={setTermDocsId} />
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
      <p className="muted"><EndpointTag id="list-grading-schemes" name="Schemes" query={{ size: '100' }} /></p>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
