import { Menu, Moon, Sun } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { useTheme } from '../theme/themeContext.js'
import { surfaceOf } from '../paths.js'
import AcademicYearPicker from './AcademicYearPicker.jsx'
import ActingAs from './ActingAs.jsx'
import SignIn from './SignIn.jsx'
import StaffPicker from './StaffPicker.jsx'

/**
 * The top bar.
 *
 * WHAT WAS TAKEN OUT, AND WHY. The template shipped a notifications bell, a messages icon, a
 * language flag and a signed-in user reading "Austin Robertson — Marketing Administrator". None
 * of them did anything, and the user was invented: this app has no accounts and no sign-in, so a
 * name and a role on screen would be a claim about somebody who does not exist. A control that
 * looks live and is not costs more than the space it fills.
 *
 * THE SEARCH BOX WENT FOR THE SAME REASON. It said "Search endpoints" and searched nothing. The
 * two screens that do have a search have their own, next to the list it filters, which is where
 * a search belongs — filtering something you can see.
 *
 * `ActingAs` AND `AcademicYearPicker` ONLY SHOW ON THE SCHOOL SURFACE. It names the tenant for the `X-School-Subdomain`
 * header, and no platform endpoint reads that header: they all name their school in the URL. On
 * a platform screen it was a control that changed nothing, and worse, one that implied the
 * screen in front of you was scoped to it. The platform's subscription screen has its own school
 * picker, because there the school is an argument to the call rather than a mode.
 */
export default function Topbar({ onMenuClick }) {
  const { theme, toggleTheme } = useTheme()
  const { pathname } = useLocation()
  const onSchoolSurface = surfaceOf(pathname) === 'school'

  return (
    <header className="topbar">
      <button type="button" className="topbar-menu" onClick={onMenuClick} aria-label="Open navigation">
        <Menu size={20} />
      </button>

      <div className="topbar-actions">
        {onSchoolSurface ? <ActingAs /> : null}
        {/* The year is the second half of the same mode, so it sits next to the school and is
            hidden on the platform surface for the same reason: no platform endpoint reads the
            tenant header, and none of them name a year. */}
        {onSchoolSurface ? <AcademicYearPicker /> : null}
        {/* THE THIRD PART OF THE SAME MODE: school, year, person. The server resolves the actor from it — it
            feeds the local-user cookie, which the provider re-sends whenever any of the three
            changes. Hidden on the platform surface with the other two, because a staff member
            belongs to a tenant and the platform surface has none. */}
        {onSchoolSurface ? <StaffPicker /> : null}
        {/* LAST, because it acts on the three to its left. It writes them into the local-user
            cookie — and only when pressed: following the pickers automatically put three calls
            in the log for one decision, two of them storing a context nobody meant. */}
        {onSchoolSurface ? <SignIn /> : null}
        <button
          type="button"
          className="theme-toggle"
          role="switch"
          aria-checked={theme === 'dark'}
          onClick={toggleTheme}
        >
          <span className="sr-only">Dark mode</span>
          <span className="theme-toggle-knob" aria-hidden="true">
            {theme === 'dark' ? <Moon size={11} /> : <Sun size={11} />}
          </span>
        </button>
      </div>
    </header>
  )
}
