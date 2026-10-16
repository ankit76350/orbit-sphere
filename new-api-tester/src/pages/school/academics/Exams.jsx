import { useCallback, useEffect, useState } from 'react'
import { ChevronRight, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'

/**
 * Exams, step 1 of 2: /school-academics/exams
 *
 * THE YEAR'S TERMS. An exam belongs to a term, so the terms come first; open one to see its exams
 * and create one. Retired terms are listed too — opening one and creating an exam in it is the
 * documented 409 TERM_NOT_ACTIVE.
 */
export default function Exams() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const year = actingAcademicYear ?? ''

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('list-academic-terms', {
      label: 'Terms of the year',
      pathParams: { year },
      query: { size: '100' },
    })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  const terms = data?.content ?? []

  return (
    <Card
      title={`Exams — ${year || 'pick a year'}`}
      description="Every exam belongs to a term. Open a term to see its exams and create one."
      action={
        <div className="toolbar">
          <EndpointTag id="list-academic-terms" name="Terms" pathParams={{ year }} query={{ size: '100' }} />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        </div>
      }
    >
      {problem ? (
        <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
      ) : terms.length === 0 && data ? (
        <Empty title="This year has no terms" description="Add a term first, under Academics → Terms." />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Term</th><th>Dates</th><th>Weight</th><th>Status</th><th /></tr>
            </thead>
            <tbody>
              {terms.map((term) => (
                <tr key={term.termDocsId}>
                  <td><b>{term.name}</b><div className="mono muted">{term.termDocsId}</div></td>
                  <td><span className="mono">{term.startDate}</span> to <span className="mono">{term.endDate}</span></td>
                  <td>{term.weightPercent != null ? `${term.weightPercent}%` : <span className="muted">—</span>}</td>
                  <td>
                    {term.active === false ? <Badge tone="warn">retired</Badge> : <Badge tone="good">active</Badge>}
                    {term.resultsLocked ? <Badge>results locked</Badge> : null}
                  </td>
                  <td>
                    <Button icon={ChevronRight}
                      onClick={() => navigate(detailPath('school', 'academics', 'exams', term.termDocsId))}>
                      Exams
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
