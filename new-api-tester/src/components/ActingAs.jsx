import { useEffect, useMemo, useRef, useState } from 'react'
import { Building2, Check, ChevronDown, RefreshCw, Search, X } from 'lucide-react'
import { useApi, useApiState } from '../api/apiContext.js'

/**
 * Which school the app is acting as, on the school surface.
 *
 * WHY THIS HAS TO EXIST. The school surface never names a school in the URL — the tenant comes
 * from the `X-School-Subdomain` header, which is the stand-in for a session until there is
 * sign-in. So `GET /schools/current/profile` is meaningless without somebody having said which
 * school "current" is, and every screen under `School` needs the answer.
 *
 * IT IS IN THE TOP BAR BECAUSE IT IS A MODE, NOT A PAGE'S SETTING. Everything under `School`
 * asks "what does THIS school see", and the answer should not change as you move between its
 * screens — nor should it be re-picked on each one, which is how somebody ends up testing the
 * wrong tenant without noticing. It is remembered between reloads for the same reason.
 *
 * IT IS A REAL POPOVER, NOT A `<datalist>`. It was a datalist first, and with eighty-odd schools
 * the browser drew an unstyled list down most of the window: no search of its own, no theme, and
 * the school NAME in grey underneath a subdomain you could not read. A list this long has to be
 * searchable in place, which means owning the dropdown.
 *
 * THE LIST IS LOADED WHEN THE DROPDOWN FIRST OPENS. That call is `GET /platform/schools` — a
 * platform read filling a school-surface control, the one place the two surfaces touch in this
 * app. On mount would mean every page load spending a request on a control most screens never
 * use.
 *
 * TYPING IS STILL ALLOWED. The search box takes a subdomain that is not in the list and Enter
 * accepts it, because the fetch is one page of a hundred and a school outside it must still be
 * reachable.
 */
export default function ActingAs() {
  const { call, chooseSchool } = useApi()
  const { actingSubdomain } = useApiState()

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
      label: 'Schools to act as',
      query: { page: 0, size: 100, sort: 'name,asc' },
    })
    setLoading(false)
    setSchools(result.ok ? (result.bodyJson?.content ?? []) : [])
  }

  // Escape closes it, and so does a click anywhere else. Both are what a dropdown is expected
  // to do, and neither happens for free once it is a div rather than a select.
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
    // Both, because you may know the school by either — and the subdomain is the thing that
    // actually goes in the header.
    return list.filter(
      (one) => one.subdomain?.toLowerCase().includes(needle)
        || one.schoolName?.toLowerCase().includes(needle),
    )
  }, [schools, query])

  const pick = (subdomain) => {
    chooseSchool(subdomain || null)
    setOpen(false)
    setQuery('')
  }

  const openIt = () => {
    setOpen(true)
    load(false)
  }

  return (
    <div className="picker" ref={box}>
      <button
        type="button"
        className="picker-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => (open ? setOpen(false) : openIt())}
        title="Which school the school-surface endpoints act as"
      >
        <Building2 size={13} className="picker-icon" aria-hidden="true" />
        <span className="picker-label">Acting as</span>
        <span className={`picker-value${actingSubdomain ? '' : ' is-empty'}`}>
          {actingSubdomain || 'no school'}
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
                // Enter takes whatever is typed, so a school outside the loaded page is still
                // reachable — the list is one page of a hundred, not the whole set.
                if (event.key === 'Enter' && query.trim()) pick(query.trim())
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
                {query
                  ? <>Nothing matches. Press Enter to use <strong>{query.trim()}</strong> anyway.</>
                  : 'No schools came back.'}
              </p>
            ) : (
              shown.map((school) => {
                const here = school.subdomain === actingSubdomain
                return (
                  <button
                    key={school.schoolId}
                    type="button"
                    role="option"
                    aria-selected={here}
                    className={`picker-item${here ? ' is-on' : ''}`}
                    onClick={() => pick(school.subdomain)}
                  >
                    <span className="picker-item-main">
                      {/* The subdomain leads: it is what goes in the header, so it is the
                          thing being chosen. The name is how a person recognises it. */}
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
            <span className="picker-note">
              {schools ? `${shown.length} of ${schools.length}` : ''}
            </span>
            <span className="toolbar-spacer" />
            {actingSubdomain ? (
              <button type="button" className="picker-plain" onClick={() => pick(null)}>
                Clear
              </button>
            ) : null}
            <button
              type="button"
              className="picker-plain"
              onClick={() => load(true)}
              title="Read the list again"
            >
              <RefreshCw size={12} /> Reload
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
