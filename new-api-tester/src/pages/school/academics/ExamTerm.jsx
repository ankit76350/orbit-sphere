import { useEffect, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Button } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { tabPath } from '../../../paths.js'
import ExamList from './ExamList.jsx'

/**
 * One term's exams: /school-academics/exams/{termId} — opened from the "By term" tab.
 *
 * The list is the shared ExamList with this term fixed: #6 always sends termDocsId, and the
 * create form (#1) starts on this term with its dates filled in.
 */
export default function ExamTerm() {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id: termId } = useParams()
  const year = actingAcademicYear ?? ''
  const [terms, setTerms] = useState([])

  //! THE YEAR'S TERMS — only to put this term's name and dates in the header.
  useEffect(() => {
    if (!actingSubdomain || !year) return undefined
    let live = true
    call('list-academic-terms', { label: 'Terms of the year', pathParams: { year }, query: { size: '100' } })
      .then((answer) => { if (live) setTerms(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call, actingSubdomain, year])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const term = terms.find((one) => one.termDocsId === termId)

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{term?.name ?? 'Term'} — exams</h1>
          <p className="muted">
            {term
              ? <><span className="mono">{term.startDate}</span> to <span className="mono">{term.endDate}</span>{term.active === false ? ' · retired' : ''}</>
              : <span className="mono">{termId}</span>}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(tabPath('school', 'academics', 'exams', 'terms'))}>By term</Button>
        <EndpointTag id="list-academic-terms" name="Terms" pathParams={{ year }} query={{ size: '100' }} />
      </div>
      <ExamList termId={termId ?? ''} />
    </div>
  )
}
