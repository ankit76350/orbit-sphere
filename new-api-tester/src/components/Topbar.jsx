import { Mail, Menu, Moon, Search, Sun } from 'lucide-react'
import { useTheme } from '../theme/themeContext.js'

function BellIcon() {
  return (
    <span className="icon-btn-wrap">
      <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
        <path d="M18 8a6 6 0 10-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M13.7 21a2 2 0 01-3.4 0" />
      </svg>
      <span className="icon-dot" aria-hidden="true" />
    </span>
  )
}

export default function Topbar({ onMenuClick }) {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="topbar">
      <button type="button" className="topbar-menu" onClick={onMenuClick} aria-label="Open navigation">
        <Menu size={20} />
      </button>

      <div className="search">
        <Search size={16} className="search-icon" aria-hidden="true" />
        <input className="search-input" type="search" placeholder="Search" aria-label="Search" />
      </div>

      <div className="topbar-actions">
        <button type="button" className="icon-btn" aria-label="Notifications, unread">
          <BellIcon />
        </button>
        <button type="button" className="icon-btn" aria-label="Messages">
          <Mail size={19} strokeWidth={1.8} />
        </button>

        <div className="user">
          <span className="avatar">
            <svg viewBox="0 0 40 40" width="40" height="40" aria-hidden="true">
              <circle cx="20" cy="20" r="20" fill="var(--surface-sunken)" />
              <circle cx="20" cy="15.5" r="6.2" fill="var(--ink-muted)" />
              <path d="M6.5 34c2.6-6.4 7.6-9.6 13.5-9.6S30.9 27.6 33.5 34z" fill="var(--ink-muted)" />
            </svg>
            <span className="avatar-status" aria-label="Online" />
          </span>
          <span className="user-text">
            <span className="user-name">Austin Robertson</span>
            <span className="user-role">Marketing Administrator</span>
          </span>
        </div>

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

        <button type="button" className="flag-btn" aria-label="Language: English">
          <svg viewBox="0 0 24 16" width="24" height="16" aria-hidden="true" className="flag">
            <rect width="24" height="16" fill="#1b3a8c" />
            <path d="M0 0l24 16M24 0L0 16" stroke="#fff" strokeWidth="3.2" />
            <path d="M0 0l24 16M24 0L0 16" stroke="#c8102e" strokeWidth="1.6" />
            <path d="M12 0v16M0 8h24" stroke="#fff" strokeWidth="5" />
            <path d="M12 0v16M0 8h24" stroke="#c8102e" strokeWidth="3" />
          </svg>
        </button>
      </div>
    </header>
  )
}
