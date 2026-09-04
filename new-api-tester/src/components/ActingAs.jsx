import { useState } from 'react'
import { Building2 } from 'lucide-react'
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
 * THE LIST IS LOADED ONLY WHEN THE PICKER IS OPENED. That call is `GET /platform/schools` — a
 * platform read, used here to fill a school-surface control, which is the one place the two
 * surfaces touch in this app. Doing it on mount would mean every page load spending a request
 * on a control most screens never use.
 *
 * A free-text input, not a select, so a subdomain that is not in the first hundred can still be
 * typed. The list is a suggestion, not the whole set.
 */
export default function ActingAs() {
  const { call, chooseSchool } = useApi()
  const { actingSubdomain } = useApiState()
  const [draft, setDraft] = useState(actingSubdomain || '')
  const [options, setOptions] = useState(null)

  const loadOptions = async () => {
    if (options) return
    const result = await call('list-schools', {
      label: 'Schools to act as',
      query: { page: 0, size: 100, sort: 'name,asc' },
    })
    setOptions(result.ok ? (result.bodyJson?.content ?? []) : [])
  }

  const commit = () => {
    const next = draft.trim()
    if (next !== (actingSubdomain || '')) chooseSchool(next || null)
  }

  return (
    <span className="acting" title="Which school the school-surface endpoints act as">
      <Building2 size={13} className="acting-icon" aria-hidden="true" />
      <span className="acting-label">Acting as</span>
      <input
        className="acting-input"
        list="acting-schools"
        value={draft}
        placeholder="no school chosen"
        aria-label="School subdomain to act as"
        onFocus={loadOptions}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === 'Enter') event.currentTarget.blur()
        }}
      />
      <datalist id="acting-schools">
        {(options ?? []).map((school) => (
          <option key={school.schoolId} value={school.subdomain}>{school.schoolName}</option>
        ))}
      </datalist>
    </span>
  )
}
