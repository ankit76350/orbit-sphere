import { Menu, Moon, Sun } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { useTheme } from '../theme/themeContext.js'
import { surfaceOf } from '../paths.js'
import ActingAs from './ActingAs.jsx'

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
 * `ActingAs` ONLY SHOWS ON THE SCHOOL SURFACE. It names the tenant for the `X-School-Subdomain`
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
