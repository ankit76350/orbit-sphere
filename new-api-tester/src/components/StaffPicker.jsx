import { useEffect, useMemo, useRef, useState } from 'react'
import { Check, ChevronDown, RefreshCw, UserRound } from 'lucide-react'
import { useApi, useApiState } from '../api/apiContext.js'
import EndpointTag from './EndpointTag.jsx'

/**
 * Which staff member the app is acting as.
 *
 * WHY IT SITS BESIDE THE SCHOOL AND THE YEAR. It is the third part of the same mode: school, year,
 * person. What it feeds is the `staffDocsId` claim of the signed `idtoken` cookie, written when
 * Sign in is pressed.
 *
 * THE BACKEND NOW READS THAT CLAIM. CurrentUserResolver answers "who is acting" from it. Nothing
 * calls that resolver yet, so choosing a person here still changes no endpoint's behaviour — when
 * something does, this is where the answer will come from.
 *
 * IT IS STILL NOT A SIGN-IN. The cookie records who somebody CLAIMS to be. The signature proves the
 * server issued the token, not that the claim is true: anybody can ask for a token naming anybody.
 * There is no authentication in this product yet, which is why this control is a picker and not a
 * login box.
 *
 * IT IS SCOPED TO THE CHOSEN SCHOOL, and more strictly than the year is. A year is a NAME that the
 * next school may not have, so a stale one 404s and announces itself. A staff id is a DOCUMENT id
 * belonging to one tenant: carried across a school switch it would quietly name another school's
 * person. The provider clears it whenever the school changes.
 *
 * NOTHING IS DISABLED. The trigger always opens and the reload always runs. With no school the
 * list is empty and says why, which is showing the condition rather than gating on it.
 *
 * THE LIST LOADS WHEN THE POPOVER FIRST OPENS, like the other two. `GET /schools/current/staff` on
 * every page would be a request most screens never use, and on a platform screen it would 400
 * because the tenant header is not sent there.
 */
export default function StaffPicker() {
  const { call, chooseStaff } = useApi()
  const { actingSubdomain, actingStaffDocsId } = useApiState()

  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const box = useRef(null)

  // THE CACHE REMEMBERS WHICH SCHOOL IT IS FOR, and staleness is derived during render rather
  // than reset in an effect — the same call AcademicYearPicker makes, and for the same reason:
  // after a school switch the previous tenant's people must not still look pickable.
  const [cache, setCache] = useState({ subdomain: null, staff: null })
  const staff = cache.subdomain === actingSubdomain ? cache.staff : null

  const load = async (force) => {
    if (!actingSubdomain) { setCache({ subdomain: null, staff: [] }); return }
    if (staff && !force) return
    setLoading(true)
    const result = await call('list-staff', {
      label: 'Staff to act as',
      query: { page: 0, size: 100 },
    })
    setLoading(false)
    setCache({
      subdomain: actingSubdomain,
      staff: result.ok ? (result.bodyJson?.content ?? []) : [],
    })
  }

  // Escape and a click outside both close it, the same as the other two pickers.
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

  const list = useMemo(() => staff ?? [], [staff])

  //! THE NAME IS SHOWN, THE ID IS WHAT IS KEPT. The cookie stores staffDocsId, and the trigger
  //! shows whoever that is — falling back to the raw id when the list has not been loaded in this
  //! session, so a reloaded page still shows something true rather than "none chosen".
  const shown = useMemo(() => {
    if (!actingStaffDocsId) return null
    const match = list.find((one) => one.staffDocsId === actingStaffDocsId)
    return match ? match.fullName : actingStaffDocsId
  }, [actingStaffDocsId, list])

  const pick = (staffDocsId) => {
    chooseStaff(staffDocsId)
    setOpen(false)
  }

  return (
    <div className="picker" ref={box}>
      <button
        type="button"
        className="picker-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        title="Who this browser is acting as. Stored as the staffDocsId claim of the idtoken cookie. The server can read it, but nothing authenticates the claim — it is not a sign-in."
        onClick={() => { setOpen(!open); if (!open) load(false) }}
      >
        <UserRound size={13} className="picker-icon" aria-hidden="true" />
        <span className="picker-label">Staff</span>
        <span className={`picker-value${shown ? '' : ' is-empty'}`}>
          {shown || 'none chosen'}
        </span>
        <ChevronDown size={13} className="picker-caret" aria-hidden="true" />
      </button>

      {open ? (
        <div className="picker-pop" role="listbox" aria-label="Staff">
          <div className="picker-list">
            {!actingSubdomain ? (
              <p className="picker-note">
                Choose a school first. A staff id belongs to one school, so there is nothing to
                list until one is chosen.
              </p>
            ) : loading && !staff ? (
              <p className="picker-note">Reading this school&rsquo;s staff&hellip;</p>
            ) : list.length === 0 ? (
              <p className="picker-note">
                This school has no staff yet. Create somebody under School › Staff.
              </p>
            ) : (
              <>
                {/* CLEARING IS A CHOICE TOO, and it has to be reachable: "acting as nobody" is a
                    real thing to test, and without this the only way back is developer tools. */}
                <button
                  type="button"
                  role="option"
                  aria-selected={!actingStaffDocsId}
                  className={`picker-item${!actingStaffDocsId ? ' is-on' : ''}`}
                  onClick={() => pick(null)}
                >
                  <span className="picker-item-main">
                    <span className="picker-item-sub">— nobody —</span>
                    <span className="picker-item-name">clears the staff from the cookie</span>
                  </span>
                  {!actingStaffDocsId ? <Check size={14} className="picker-item-tick" /> : null}
                </button>

                {list.map((person) => {
                  const here = person.staffDocsId === actingStaffDocsId
                  return (
                    <button
                      key={person.staffDocsId}
                      type="button"
                      role="option"
                      aria-selected={here}
                      className={`picker-item${here ? ' is-on' : ''}`}
                      onClick={() => pick(person.staffDocsId)}
                    >
                      <span className="picker-item-main">
                        <span className="picker-item-sub">{person.fullName}</span>
                        {/* The employee number is what a school calls somebody; the document id
                            is what the cookie stores. Both, because they answer two questions. */}
                        <span className="picker-item-name">
                          {person.employeeNo || person.staffDocsId}
                        </span>
                      </span>
                      {here ? <Check size={14} className="picker-item-tick" /> : null}
                    </button>
                  )
                })}
              </>
            )}
          </div>

          {/* THE RELOAD BUTTON'S ENDPOINT, like every other control in this app that makes a
              call. A picker is chrome rather than a screen, which is exactly why it was missed —
              and a dropdown whose Reload hits an endpoint nobody can name is the one place a
              tester cannot see what the tool is doing. */}
          <div className="picker-foot">
            <EndpointTag id="list-staff" name="Reload" query={{ page: 0, size: 100 }} />
            <span className="toolbar-spacer" />
            <button type="button" className="picker-plain" onClick={() => load(true)}>
              <RefreshCw size={12} /> Reload
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
