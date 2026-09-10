import { Search } from 'lucide-react'

/**
 * The row above a plan `<select>`: type, then either filter nothing or ask the API.
 *
 * A NATIVE SELECT CANNOT HOLD A SEARCH, which is why this sits above it rather than inside. It
 * also means there is no local filtering here at all — unlike the school picker, where typing
 * narrows the loaded list in place. Here typing does nothing until the button is pressed, and the
 * note says so rather than leaving somebody typing into a box that appears broken.
 */
export default function PlanSearchRow({ picker }) {
  const { query, retype, runSearch, searching, fromSearch, options, loadedCount, clearSearch }
    = picker

  return (
    <div className="stack" style={{ gap: 6 }}>
      <div className="toolbar">
        <input
          className="input"
          value={query}
          placeholder="Plan code or name"
          aria-label="Search plans"
          onChange={(event) => retype(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') { event.preventDefault(); runSearch() }
          }}
        />
        <button type="button" className="btn" onClick={runSearch}>
          <Search size={13} /> {searching ? 'Searching…' : 'Search all plans'}
        </button>
        {fromSearch ? (
          <button type="button" className="btn" onClick={clearSearch}>
            Back to the first hundred
          </button>
        ) : null}
      </div>
      <span className="muted">
        {fromSearch
          ? `${options?.length ?? 0} plan(s) found across every published plan.`
          : `Showing the first ${loadedCount} published plans. Typing filters nothing here — `
            + 'press Search to ask the API, which matches the code and the name across all of them.'}
      </span>
    </div>
  )
}
