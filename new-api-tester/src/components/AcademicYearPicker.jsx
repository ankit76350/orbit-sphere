import { useEffect, useMemo, useRef, useState } from 'react'
import { CalendarRange, Check, ChevronDown, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../api/apiContext.js'

/**
 * Which academic year the school surface is working in.
 *
 * WHY IT SITS BESIDE THE SCHOOL PICKER. A year is the second half of the same mode. Fourteen of
 * the eighteen academic-year endpoints act on ONE year and name it in the path, and so will
 * everything that records attendance, marks or fees against a year. Picking it per screen is how
 * somebody writes to last year without noticing — the same argument that put the school up here.
 *
 * IT IS SCOPED TO THE CHOSEN SCHOOL, AND ONLY MEANINGFUL WITH ONE. A year is identified by its
 * NAME, and a name is unique only within a school: "2026-2027" is a different document for every
 * tenant. So the provider clears this whenever the school changes, and with no school chosen
 * there is nothing to list — the popover says so rather than the control disappearing, because a
 * control that vanishes teaches nobody why.
 *
 * NOTHING IS DISABLED. The trigger always opens, the reload always runs. With no school the list
 * is empty and explains itself; that is showing the condition rather than gating on it.
 *
 * THE LIST LOADS WHEN THE POPOVER FIRST OPENS, like the school picker. `GET
 * /schools/current/academic-years` on every page would be a request most screens never use — and
 * on a platform screen it would 400, because the tenant header is not sent there.
 *
 * NO SEARCH BOX. A school has a handful of years, not eighty. The school picker owns a search
 * because a list that long needs one; adding it here would be furniture.
 */
export default function AcademicYearPicker() {
  const { call, chooseAcademicYear } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const box = useRef(null)

  // THE CACHE REMEMBERS WHICH SCHOOL IT IS FOR, and staleness is worked out during render rather
  // than reset in an effect. Anything loaded belonged to the school chosen at the time, so after
  // a school switch the previous tenant's years must not be shown — they would look pickable.
  //
  // The first version did this with `useEffect(() => setYears(null), [actingSubdomain])`, which
  // is a setState inside an effect: an extra render, and one more thing that can run in the wrong
  // order. Deriving it needs neither.
  const [cache, setCache] = useState({ subdomain: null, years: null })
  const years = cache.subdomain === actingSubdomain ? cache.years : null

  const load = async (force) => {
    if (!actingSubdomain) { setCache({ subdomain: null, years: [] }); return }
    if (years && !force) return
    setLoading(true)
    const result = await call('list-academic-years', { label: 'Years to choose from' })
    setLoading(false)
    setCache({ subdomain: actingSubdomain, years: result.ok ? (result.bodyJson ?? []) : [] })
  }

  // Escape and a click outside both close it, the same as the school picker.
  useEffect(() => {
    if (!open) return undefined
    const onKey = (event) => { if (event.key === 'Escape') setOpen(false) }
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

  const list = useMemo(() => years ?? [], [years])

  const pick = (name) => {
    chooseAcademicYear(name)
    setOpen(false)
  }

  return (
    <div className="picker" ref={box}>
      <button
        type="button"
        className="picker-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        title="Which academic year the School endpoints act on. Named per school, so it clears when you switch school."
        onClick={() => { setOpen(!open); if (!open) load(false) }}
      >
        <CalendarRange size={13} className="picker-icon" aria-hidden="true" />
        <span className="picker-label">Year</span>
        <span className={`picker-value${actingAcademicYear ? '' : ' is-empty'}`}>
          {actingAcademicYear || 'none chosen'}
        </span>
        <ChevronDown size={13} className="picker-caret" aria-hidden="true" />
      </button>

      {open ? (
        <div className="picker-pop" role="listbox" aria-label="Academic years">
          <div className="picker-list">
            {!actingSubdomain ? (
              <p className="picker-note">
                Choose a school first. A year is named, and a name only means something inside one
                school.
              </p>
            ) : loading && !years ? (
              <p className="picker-note">Reading this school&rsquo;s years&hellip;</p>
            ) : list.length === 0 ? (
              <p className="picker-note">
                This school has no academic years yet. Create one under School › Academic years.
              </p>
            ) : (
              list.map((year) => {
                const here = year.name === actingAcademicYear
                return (
                  <button
                    key={year.academicYearId}
                    type="button"
                    role="option"
                    aria-selected={here}
                    className={`picker-item${here ? ' is-on' : ''}`}
                    onClick={() => pick(year.name)}
                  >
                    <span className="picker-item-main">
                      {/* The name leads: it is the thing being chosen, and the thing every
                          other collection stores as its join key. */}
                      <span className="picker-item-sub">{year.name}</span>
                      <span className="picker-item-name">
                        {year.startDate} → {year.endDate}
                      </span>
                    </span>
                    {/* `current` is derived from the dates; isThisYearRunning is stored. They can
                        disagree, which is exactly why both are shown. */}
                    {year.current ? (
                      <span className="picker-item-status" data-status="ACTIVE">today</span>
                    ) : null}
                    {year.isThisYearRunning ? (
                      <span className="picker-item-status" data-status="ACTIVE">running</span>
                    ) : null}
                    {here ? <Check size={14} className="picker-item-tick" /> : null}
                  </button>
                )
              })
            )}
          </div>

          <div className="picker-foot">
            <span className="picker-note">{years ? `${list.length} year(s)` : ''}</span>
            <span className="toolbar-spacer" />
            {actingAcademicYear ? (
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
