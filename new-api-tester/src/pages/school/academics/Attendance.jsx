import { useState } from 'react'
import { CalendarCheck, CalendarRange, LayoutList, RefreshCw, UserX, Users } from 'lucide-react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useApiState } from '../../../api/apiContext.js'
import { Button } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { tabPath } from '../../../paths.js'
import AttendanceClasses from './AttendanceClasses.jsx'
import AttendanceAbsentees from './AttendanceAbsentees.jsx'
import AttendanceDay from './AttendanceDay.jsx'
import AttendanceMonthly from './AttendanceMonthly.jsx'
import AttendanceStudents from './AttendanceStudents.jsx'

/**
 * Attendance: two tabs, each its own address.
 *
 * - /school-academics/attendance/day     — which sections have taken attendance on a date (#8)
 * - /school-academics/attendance/classes — the classes, leading to sections and their registers
 * - /school-academics/attendance/students — pick a child, see their attendance (#9)
 * - /school-academics/attendance/monthly  — pick a class and section, see the monthly register (#10)
 * - /school-academics/attendance/absentees — who was absent on a date, and who has not been told (#11)
 *
 * THE BARE ADDRESS REDIRECTS to the day tab, so a link written before the split still lands
 * somewhere. The same pattern as the timetable's tabs.
 */

const DAY = tabPath('school', 'academics', 'attendance', 'day')
const CLASSES = tabPath('school', 'academics', 'attendance', 'classes')
const STUDENTS = tabPath('school', 'academics', 'attendance', 'students')
const MONTHLY = tabPath('school', 'academics', 'attendance', 'monthly')
const ABSENTEES = tabPath('school', 'academics', 'attendance', 'absentees')

export default function Attendance() {
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  //! BUMPED BY REFRESH, so whichever tab is showing reads again.
  const [reloadKey, setReloadKey] = useState(0)

  const onDay = pathname.startsWith(DAY)
  const onClasses = pathname.startsWith(CLASSES)
  const onStudents = pathname.startsWith(STUDENTS)
  const onMonthly = pathname.startsWith(MONTHLY)
  const onAbsentees = pathname.startsWith(ABSENTEES)

  if (!onDay && !onClasses && !onStudents && !onMonthly && !onAbsentees) {
    return <Navigate to={DAY} replace />
  }
  if (!actingSubdomain) return <NoSchoolChosen what="Attendance" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Attendance</h1>
          <p className="muted">
            In <b>{actingAcademicYear || 'no year selected in the navbar'}</b>.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <div className="btn-row">
          <Button icon={CalendarCheck} look={onDay ? 'primary' : undefined}
            onClick={() => navigate(DAY)}>Attendance on a date</Button>
          <Button icon={LayoutList} look={onClasses ? 'primary' : undefined}
            onClick={() => navigate(CLASSES)}>Classes</Button>
          <Button icon={Users} look={onStudents ? 'primary' : undefined}
            onClick={() => navigate(STUDENTS)}>Students</Button>
          <Button icon={CalendarRange} look={onMonthly ? 'primary' : undefined}
            onClick={() => navigate(MONTHLY)}>Monthly register</Button>
          <Button icon={UserX} look={onAbsentees ? 'primary' : undefined}
            onClick={() => navigate(ABSENTEES)}>Absentees</Button>
        </div>
        <Button icon={RefreshCw} onClick={() => setReloadKey((k) => k + 1)}>Refresh</Button>
      </div>

      {onDay ? <AttendanceDay reloadKey={reloadKey} /> : null}
      {onClasses ? <AttendanceClasses reloadKey={reloadKey} /> : null}
      {onStudents ? <AttendanceStudents reloadKey={reloadKey} /> : null}
      {onMonthly ? <AttendanceMonthly reloadKey={reloadKey} /> : null}
      {onAbsentees ? <AttendanceAbsentees reloadKey={reloadKey} /> : null}
    </div>
  )
}
