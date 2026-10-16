import { useState } from 'react'
import { CalendarRange, ListChecks, RefreshCw } from 'lucide-react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useApiState } from '../../../api/apiContext.js'
import { Button } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { tabPath } from '../../../paths.js'
import ExamList from './ExamList.jsx'
import ExamsTerms from './ExamsTerms.jsx'

/**
 * Exams: two tabs, each its own address.
 *
 * - /school-academics/exams/all   — every exam of the year, with a term filter, and Create (#6, #1)
 * - /school-academics/exams/terms — the year's terms; opening one shows only its exams
 *
 * THE BARE ADDRESS REDIRECTS to the All exams tab. The same pattern as attendance's tabs.
 */

const ALL = tabPath('school', 'academics', 'exams', 'all')
const TERMS = tabPath('school', 'academics', 'exams', 'terms')

export default function Exams() {
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  //! BUMPED BY REFRESH, so whichever tab is showing reads again.
  const [reloadKey, setReloadKey] = useState(0)

  const onAll = pathname.startsWith(ALL)
  const onTerms = pathname.startsWith(TERMS)

  if (!onAll && !onTerms) return <Navigate to={ALL} replace />
  if (!actingSubdomain) return <NoSchoolChosen what="Exams" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Exams</h1>
          <p className="muted">
            In <b>{actingAcademicYear || 'no year selected in the navbar'}</b>.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <div className="btn-row">
          <Button icon={ListChecks} look={onAll ? 'primary' : undefined}
            onClick={() => navigate(ALL)}>All exams</Button>
          <Button icon={CalendarRange} look={onTerms ? 'primary' : undefined}
            onClick={() => navigate(TERMS)}>By term</Button>
        </div>
        <Button icon={RefreshCw} onClick={() => setReloadKey((k) => k + 1)}>Refresh</Button>
      </div>

      {onAll ? <ExamList reloadKey={reloadKey} /> : null}
      {onTerms ? <ExamsTerms reloadKey={reloadKey} /> : null}
    </div>
  )
}
