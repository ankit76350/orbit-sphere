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

  // TWO DIFFERENT SEARCHES, AND THE DIFFERENCE MATTERS.
  //
  // Typing filters the hundred schools already loaded. That is instant and covers nothing else:
  // the list endpoint caps a page at 100, so on a platform with more schools than that, a name
  // that is not in the first hundred simply does not appear — and an empty list looks identical
  // to "no such school".
  //
  // The Search button sends `?search=` instead, which the API matches against the school name
  // AND the subdomain across every school there is. `found` holds that answer; while it is set
  // the local filter is bypassed, because the server already did the filtering and re-filtering
  // its result against the same text would only ever remove rows it deliberately returned.
  const [found, setFound] = useState(null)
  const [searching, setSearching] = useState(false)

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

  /** Asks the API for this name, rather than filtering what happens to be loaded. */
  const runSearch = async () => {
    const needle = query.trim()
    if (!needle) { setFound(null); return }
    setSearching(true)
    const result = await call('list-schools', {
      label: `Schools matching "${needle}"`,
      query: { page: 0, size: 100, sort: 'name,asc', search: needle },
    })
    setSearching(false)
    setFound(result.ok ? (result.bodyJson?.content ?? []) : [])
  }

  // Editing the box drops the previous answer. Keeping it would leave results on screen that no
  // longer match what the box says, which is the one thing worse than no results.
  const retype = (next) => {
    setQuery(next)
    if (found) setFound(null)
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
    // A server answer is already filtered, and by the same two fields. Filtering it again here
    // would drop rows the API matched on a field this one does not look at.
    if (found) return found
    const list = schools ?? []
    const needle = query.trim().toLowerCase()
    if (!needle) return list
    // Both, because you may know a school by either — and the subdomain is what goes on the wire.
    return list.filter(
      (one) => one.subdomain?.toLowerCase().includes(needle)
        || one.schoolName?.toLowerCase().includes(needle),
    )
  }, [schools, query, found])

  // What to show on the trigger. When the value is an id it is meaningless to read, so the
  // school's name is looked up where the list has been loaded.
  const shownValue = useMemo(() => {
    if (!value) return null
    if (as === 'subdomain') return value
    // Look through the search answer as well as the loaded page: a school picked out of a
    // server search is often not in the first hundred, and the trigger would otherwise show a
    // raw Mongo id for exactly the schools the search exists to reach.
    // (Named `match`, not `found` — the state above owns that name.)
    const match = [...(found ?? []), ...(schools ?? [])].find((one) => one.schoolId === value)
    return match ? match.subdomain : value
  }, [value, as, schools, found])

  const pick = (school) => {
    onChange(school ? (as === 'id' ? school.schoolId : school.subdomain) : null, school)
    setOpen(false)
    setQuery('')
    setFound(null)
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
              placeholder="Filter the loaded list, or Search all"
              aria-label="Search schools"
              onChange={(event) => retype(event.target.value)}
              onKeyDown={(event) => {
                // ENTER SEARCHES THE SERVER, which is what a box with a Search button beside it
                // is expected to do. It used to take the typed subdomain as a school — that
                // escape hatch is still there, but as a click in the empty state below, where it
                // can say what it is rather than being a keystroke nobody discovers.
                if (event.key === 'Enter') {
                  event.preventDefault()
                  runSearch()
                }
              }}
            />
            {query ? (
              <button
                type="button"
                className="picker-clear"
                aria-label="Clear the search"
                onClick={() => { retype(''); field.current?.focus() }}
              >
                <X size={13} />
              </button>
            ) : null}
          </div>

          {/* The list endpoint caps a page at 100. Typing filters those hundred; this asks the
              API, which matches the name AND the subdomain across every school there is. */}
          <div className="picker-search-actions">
            <button
              type="button"
              className="picker-plain"
              onClick={runSearch}
              title="Ask the API for this name or subdomain, across every school — not just the hundred loaded here."
            >
              <Search size={12} /> {searching ? 'Searching…' : 'Search all schools'}
            </button>
            {found ? (
              <button type="button" className="picker-plain" onClick={() => setFound(null)}>
                Back to the loaded list
              </button>
            ) : null}
          </div>

          <div className="picker-list">
            {searching ? (
              <p className="picker-note">Asking the API for &ldquo;{query.trim()}&rdquo;&hellip;</p>
            ) : loading && !schools ? (
              <p className="picker-note">Loading the schools…</p>
            ) : shown.length === 0 ? (
              <p className="picker-note">
                {found ? (
                  // A server answer of nothing is a real answer: no school anywhere matches.
                  <>No school matches &ldquo;{query.trim()}&rdquo;. That is the API&rsquo;s answer
                    for every school, not just the loaded page.</>
                ) : query ? (
                  // A local miss says nothing about the schools beyond the loaded hundred.
                  <>Nothing in the loaded list matches. It only holds the first hundred —{' '}
                    <button type="button" className="picker-plain" onClick={runSearch}>
                      search all schools
                    </button>{' '}for &ldquo;{query.trim()}&rdquo;.</>
                ) : 'No schools came back.'}
                {query && as === 'subdomain' ? (
                  <>
                    {' '}Or{' '}
                    <button
                      type="button"
                      className="picker-plain"
                      onClick={() => pick({ subdomain: query.trim() })}
                    >
                      use &ldquo;{query.trim()}&rdquo; as the subdomain anyway
                    </button>.
                  </>
                ) : null}
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
            {/* Say which list this counts. "12 of 100" after a server search would be a lie
                about where the twelve came from, and "100 of 100" hides that there may be more
                schools than the page can hold. */}
            <span className="picker-note">
              {found
                ? `${found.length} found across every school`
                : schools
                  ? `${shown.length} of ${schools.length} loaded`
                    + (schools.length >= 100 ? ' — search to reach the rest' : '')
                  : ''}
            </span>
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
