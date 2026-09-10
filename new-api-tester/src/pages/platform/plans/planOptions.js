import { useEffect, useMemo, useState } from 'react'
import { useApi } from '../../../api/apiContext.js'

/**
 * The plans a subscription modal can offer, and a way to look past the first hundred of them.
 *
 * WHY THIS IS SHARED RATHER THAN WRITTEN TWICE. Change Plan and New Subscription ask the same
 * question — "which published plan" — and had the same `list-plans` call copied into each. Two
 * copies of a capped list is two places to fix when somebody notices the cap, and the second one
 * gets missed.
 *
 * THE CAP IS THE POINT. `GET /platform/plans` pages at 100. A `<select>` built from one page
 * silently omits everything after it, and an option that is not there looks exactly like a plan
 * that does not exist — the same trap the school picker had, on a control with even less room to
 * explain itself. So the search sends `?search=`, which the API matches across the plan CODE and
 * the NAME, over every plan rather than the loaded page.
 *
 * IT KEEPS THE CALLER'S STATE. Each modal still owns `picked` and works out its own `chosen`,
 * because choosing a plan there fills three negotiable boxes from that plan's figures. This hook
 * only owns the list and the search, which is the part that was duplicated.
 */
export function usePlanOptions(open) {
  const { call } = useApi()
  const [loaded, setLoaded] = useState(null)
  const [found, setFound] = useState(null)
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)

  useEffect(() => {
    if (!open || loaded) return undefined
    let alive = true
    call('list-plans', {
      label: 'Plans to choose from',
      query: { status: 'ACTIVE', page: 0, size: 100 },
    }).then((result) => {
      if (alive) setLoaded(result.ok ? (result.bodyJson?.content ?? []) : [])
    })
    return () => { alive = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, loaded])

  const runSearch = async () => {
    const needle = query.trim()
    if (!needle) { setFound(null); return }
    setSearching(true)
    const result = await call('list-plans', {
      label: `Plans matching "${needle}"`,
      query: { status: 'ACTIVE', page: 0, size: 100, search: needle },
    })
    setSearching(false)
    setFound(result.ok ? (result.bodyJson?.content ?? []) : [])
  }

  // Editing the box drops the answer: options that no longer match what is typed are worse than
  // no options, because a `<select>` gives no hint that they are stale.
  const retype = (next) => {
    setQuery(next)
    if (found) setFound(null)
  }

  // What the select renders. A server answer replaces the loaded page rather than adding to it,
  // so the list always has one explainable source.
  const options = useMemo(() => found ?? loaded, [found, loaded])

  return {
    options,
    ready: options !== null,
    fromSearch: found !== null,
    query,
    retype,
    runSearch,
    searching,
    clearSearch: () => { setQuery(''); setFound(null) },
    loadedCount: loaded?.length ?? 0,
  }
}
