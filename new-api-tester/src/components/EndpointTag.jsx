import { findEndpoint } from '../config/endpoints.js'
import { useApi, useApiState } from '../api/apiContext.js'

/**
 * The little "GET /platform/schools" label beside whatever triggers a call.
 *
 * WHY IT EXISTS. This app is for exercising the API, not a product. Somebody looking at a button
 * wants to know which endpoint it hits, and without this the only ways to find out are to read
 * the source or to press it and see.
 *
 * It shows the REAL request: path parameters filled in and the query string the call actually
 * sends, so the tag under a filtered list changes as you filter.
 *
 * CLICK IT to reopen the last call this endpoint made. Until it has made one there is nothing to
 * open, so it renders as plain text and says so on hover rather than looking like a dead button.
 *
 * IT LEADS WITH THE NAME OF THE CONTROL. "Take it live · POST /platform/schools/{id}/activate",
 * because a bare method and path answers "what is sent" but not "by what" — and the two are only
 * obvious while the tag happens to sit next to its button. In the response panel, in the log, and
 * anywhere the tag is read on its own, the name is the half that says which control did this.
 *
 * `name` falls back to the endpoint's own name from the Postman collection, so a tag is never
 * nameless — and that fallback is the string to search for in Postman, which is worth having in
 * an app whose whole job is exercising the same requests.
 *
 * The method and path are never typed in here — they come from config/endpoints.js, which is
 * generated from the Postman collection. A tag cannot disagree with the call it describes.
 */
export default function EndpointTag({ id, name, pathParams, query, showPath = true }) {
  const { inspect } = useApi()
  const { log } = useApiState()

  const endpoint = findEndpoint(id)
  // A tag naming an endpoint that does not exist is worse than no tag: it would be believed.
  if (!endpoint) return null

  let path = endpoint.path
  Object.entries(pathParams || {}).forEach(([key, value]) => {
    if (value) path = path.replace(`{${key}}`, String(value))
  })

  const search = new URLSearchParams()
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) value.forEach((one) => search.append(key, String(one)))
    else search.append(key, String(value))
  })
  const qs = search.toString()

  const shown = name || endpoint.name
  const last = log.find((entry) => entry.endpointId === id)
  const full = qs ? `${path}?${qs}` : path

  const label = (
    <>
      <span className="endpoint-tag-name">{shown}</span>
      <span className="endpoint-tag-method" data-method={endpoint.method}>{endpoint.method}</span>
      {showPath && <span className="endpoint-tag-path">{full}</span>}
    </>
  )

  if (!last) {
    return (
      <span className="endpoint-tag" title={`${shown} — ${endpoint.method} ${full}. Not called yet.`}>
        {label}
      </span>
    )
  }

  return (
    <button
      type="button"
      className="endpoint-tag"
      onClick={() => inspect(last)}
      title={`${shown} — the last ${endpoint.method} ${full} answered ${last.status}`}
    >
      {label}
    </button>
  )
}
