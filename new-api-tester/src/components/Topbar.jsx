import { Menu, Moon, Search, Sun } from 'lucide-react'
import { useTheme } from '../theme/themeContext.js'

/**
 * The top bar.
 *
 * WHAT WAS TAKEN OUT, AND WHY. The template shipped a notifications bell, a messages icon, a
 * language flag and a signed-in user reading "Austin Robertson — Marketing Administrator". None
 * of them did anything, and the user was invented: this app has no accounts and no sign-in, so a
 * name and a role on screen would be a claim about somebody who does not exist. A control that
 * looks live and is not costs more than the space it fills.
 *
 * WHAT BELONGS HERE INSTEAD, once the API layer lands: which backend the app is pointed at, and
 * how many calls have been made and how many failed. Both are real state that changes, which is
 * what a top bar is for. The search box and the theme switch stay because both will do something
 * — search over the endpoint list, and the switch already works.
 */
export default function Topbar({ onMenuClick }) {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="topbar">
      <button type="button" className="topbar-menu" onClick={onMenuClick} aria-label="Open navigation">
        <Menu size={20} />
      </button>

      <div className="search">
        <Search size={16} className="search-icon" aria-hidden="true" />
        <input className="search-input" type="search" placeholder="Search endpoints" aria-label="Search endpoints" />
      </div>

      <div className="topbar-actions">
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
