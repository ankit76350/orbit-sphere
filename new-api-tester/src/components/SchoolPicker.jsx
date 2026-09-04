import { useEffect, useMemo, useRef, useState } from 'react'
import { Building2, Check, ChevronDown, RefreshCw, Search, X } from 'lucide-react'
import { useApi } from '../api/apiContext.js'

/**
 * Pick a school, from a searchable popover.
 *
 * TWO SCREENS NEED THIS FOR DIFFERENT REASONS, which is why it is a component rather than part
 * of one of them:
 *
 *   - On the SCHOOL surface it is a MODE: everything under `School` asks what this one school
 *     sees, and the answer is remembered between screens and reloads. Labelled just "School",
 *     because that is what is being chosen.
 *   - On the PLATFORM surface it is an ARGUMENT. The subscription endpoints name a school in the
 *     URL, so it is a parameter of the call and belongs to the screen, not to the session.
 *
 * The distinction matters enough to keep in the callers: this component only knows about a value
 * and a change, and neither caller can accidentally read the other's.
 *
 * IT IS A REAL POPOVER, NOT A `<datalist>`. It was a datalist first, and with eighty-odd schools
 * the browser drew an unstyled list down most of the window: no search of its own, no theme, and
 * the school name in grey under a subdomain you could not read. A list that long has to be
 * searchable in place, which means owning the dropdown.
 *
 * THE LIST LOADS WHEN THE POPOVER FIRST OPENS. That call is `GET /platform/schools`, and doing
 * it on mount would mean every page spending a request on a control most screens never use.
 */
export default function SchoolPicker({
  value,
  onChange,
  label = 'School',
  /** What the value is: 'subdomain' for the school surface, 'id' for platform URLs. */
  as = 'subdomain',
  placeholder = 'no school',
  /** What it does, for the hover. A one-word label needs somewhere to explain itself. */
  title,
}) {
  const { call } = useApi()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [schools, setSchools] = useState(null)
  const [loading, setLoading] = useState(false)
  const box = useRef(null)
  const field = useRef(null)

  const load = async (force) => {
    if (schools && !force) return
    setLoading(true)
    const result = await call('list-schools', {
      label: 'Schools to choose from',
      query: { page: 0, size: 100, sort: 'name,asc' },
    })
    setLoading(false)
    setSchools(result.ok ? (result.bodyJson?.content ?? []) : [])
  }

  // Escape and a click outside both close it. Neither is free once this is a div rather than a
  // native select.
  useEffect(() => {
    if (!open) return undefined
    const onKey = (event) => {
      if (event.key === 'Escape') setOpen(false)
    }
    const onDown = (event) => {
      if (box.current && !box.current.contains(event.target)) setOpen(false)
    }
    window.addEventListener('keydown', onKey)
    window.addEventListener('mousedown', onDown)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('mousedown', onDown)
    }
  }, [open])

  useEffect(() => {
    if (open) field.current?.focus()
  }, [open])

  const shown = useMemo(() => {
    const list = schools ?? []
    const needle = query.trim().toLowerCase()
    if (!needle) return list
    // Both, because you may know a school by either — and the subdomain is what goes on the wire.
    return list.filter(
      (one) => one.subdomain?.toLowerCase().includes(needle)
        || one.schoolName?.toLowerCase().includes(needle),
    )
  }, [schools, query])

  // What to show on the trigger. When the value is an id it is meaningless to read, so the
  // school's name is looked up where the list has been loaded.
  const shownValue = useMemo(() => {
    if (!value) return null
    if (as === 'subdomain') return value
    const found = (schools ?? []).find((one) => one.schoolId === value)
    return found ? found.subdomain : value
  }, [value, as, schools])

  const pick = (school) => {
    onChange(school ? (as === 'id' ? school.schoolId : school.subdomain) : null, school)
    setOpen(false)
    setQuery('')
  }

  return (
    <div className="picker" ref={box}>
      <button
        type="button"
        className="picker-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        title={title}
        onClick={() => { setOpen(!open); if (!open) load(false) }}
      >
        <Building2 size={13} className="picker-icon" aria-hidden="true" />
        <span className="picker-label">{label}</span>
        <span className={`picker-value${shownValue ? '' : ' is-empty'}`}>
          {shownValue || placeholder}
        </span>
        <ChevronDown size={13} className="picker-caret" aria-hidden="true" />
      </button>

      {open ? (
        <div className="picker-pop" role="listbox" aria-label="Schools">
          <div className="picker-search">
            <Search size={15} className="picker-search-icon" aria-hidden="true" />
            <input
              ref={field}
              className="picker-search-input"
              value={query}
              placeholder="Search name or subdomain"
              aria-label="Search schools"
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                // Enter takes a typed subdomain, so a school outside the loaded page is still
                // reachable. Only for the subdomain form — an id cannot sensibly be typed.
                if (event.key === 'Enter' && as === 'subdomain' && query.trim()) {
                  pick({ subdomain: query.trim() })
                }
              }}
            />
            {query ? (
              <button
                type="button"
                className="picker-clear"
                aria-label="Clear the search"
                onClick={() => { setQuery(''); field.current?.focus() }}
              >
                <X size={13} />
              </button>
            ) : null}
          </div>

          <div className="picker-list">
            {loading && !schools ? (
              <p className="picker-note">Loading the schools…</p>
            ) : shown.length === 0 ? (
              <p className="picker-note">
                {query && as === 'subdomain'
                  ? <>Nothing matches. Press Enter to use <strong>{query.trim()}</strong> anyway.</>
                  : query ? 'Nothing matches.' : 'No schools came back.'}
              </p>
            ) : (
              shown.map((school) => {
                const here = as === 'id'
                  ? school.schoolId === value
                  : school.subdomain === value
                return (
                  <button
                    key={school.schoolId}
                    type="button"
                    role="option"
                    aria-selected={here}
                    className={`picker-item${here ? ' is-on' : ''}`}
                    onClick={() => pick(school)}
                  >
                    <span className="picker-item-main">
                      {/* The subdomain leads: it is the thing being chosen. The name is how a
                          person recognises it. */}
                      <span className="picker-item-sub">{school.subdomain}</span>
                      <span className="picker-item-name">{school.schoolName}</span>
                    </span>
                    <span className="picker-item-status" data-status={school.status}>
                      {school.status}
                    </span>
                    {here ? <Check size={14} className="picker-item-tick" /> : null}
                  </button>
                )
              })
            )}
          </div>

          <div className="picker-foot">
            <span className="picker-note">{schools ? `${shown.length} of ${schools.length}` : ''}</span>
            <span className="toolbar-spacer" />
            {value ? (
              <button type="button" className="picker-plain" onClick={() => pick(null)}>Clear</button>
            ) : null}
            <button type="button" className="picker-plain" onClick={() => load(true)}>
              <RefreshCw size={12} /> Reload
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
