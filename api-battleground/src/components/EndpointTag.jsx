/**
 * The little "GET /platform/schools" label that sits next to whatever triggers a call.
 *
 * WHY IT EXISTS. This app is a battleground for the API, not a product. Somebody looking at a
 * button wants to know which endpoint it hits, and the only ways to find out were to read the
 * source or to press it and open the activity log. Now every control says so on its face.
 *
 * It shows the real request, not a description of one: path parameters are filled in and the
 * query string is the one actually being sent, so the tag under the school list reads
 * `/platform/schools?page=0&size=20&sort=createdAt,desc` and changes as you filter.
 *
 * CLICK IT to reopen the last call this endpoint made — the request, the response, the timing.
 * Until it has been called there is nothing to open, so it renders as plain text and says so on
 * hover rather than looking like a dead button.
 *
 * The method and path are never typed in here: they come from config/endpoints.js, which is
 * generated from the Postman collection. A tag cannot disagree with the call it describes.
 */

import { findEndpoint } from '../config/endpoints.js';
import { useApi, useApiState } from '../api/apiContext.js';

/** One colour per verb, shared with the activity list and the details pop-up. */
export const METHOD_LOOK = {
  GET: 'green',
  POST: 'amber',
  PUT: 'blue',
  PATCH: 'violet',
  DELETE: 'red',
};

const METHOD_CLASS = {
  green: 'bg-emerald-100 text-emerald-800',
  amber: 'bg-amber-100 text-amber-900',
  blue: 'bg-blue-100 text-blue-800',
  violet: 'bg-violet-100 text-violet-800',
  red: 'bg-red-100 text-red-800',
  grey: 'bg-slate-200 text-slate-700',
};

/** `{id}` → the real value, when one was given. Left as `{id}` when it was not. */
function fillPath(path, pathParams) {
  if (!pathParams) return path;
  return path.replace(/\{(\w+)\}/g, (whole, name) => {
    const value = pathParams[name];
    return value === undefined || value === null || value === '' ? whole : String(value);
  });
}

/**
 * The query string as it will actually be sent.
 *
 * Repeats a key per value rather than joining with commas, because that is what the backend
 * reads: `?status=ACTIVE&status=TRIAL` is how a repeatable parameter arrives.
 */
function toQueryString(query) {
  const parts = [];
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((one) => {
        if (one !== undefined && one !== null && one !== '') parts.push(`${key}=${one}`);
      });
      return;
    }
    parts.push(`${key}=${value}`);
  });
  return parts.length ? `?${parts.join('&')}` : '';
}

export default function EndpointTag({
  id,
  pathParams,
  query,
  className = '',
  showPath = true,
}) {
  const { inspect } = useApi();
  const { log } = useApiState();

  const endpoint = findEndpoint(id);
  if (!endpoint) {
    // A tag naming an endpoint that does not exist is worse than no tag: it would be believed.
    return null;
  }

  const shown = `${fillPath(endpoint.path, pathParams)}${toQueryString(query)}`;
  const last = log.find((entry) => entry.endpointId === id);

  const body = (
    <>
      <span
        className={`rounded px-1.5 py-px font-mono text-[10px] font-bold tracking-wide ${
          METHOD_CLASS[METHOD_LOOK[endpoint.method]] || METHOD_CLASS.grey
        }`}
      >
        {endpoint.method}
      </span>
      {showPath && <span className="truncate font-mono text-[11px]">{shown}</span>}
    </>
  );

  const shape = `inline-flex max-w-full items-center gap-1.5 rounded-md border px-1.5 py-0.5 ${className}`;

  if (!last) {
    return (
      <span
        title={`${endpoint.method} ${shown} — not called yet`}
        className={`${shape} border-slate-200 bg-slate-50 text-slate-500`}
      >
        {body}
      </span>
    );
  }

  return (
    <button
      type="button"
      onClick={(event) => {
        // These tags often sit inside a clickable row or a form. Neither should react.
        event.preventDefault();
        event.stopPropagation();
        inspect(last);
      }}
      title={`${endpoint.method} ${shown} — click to see the last call (${last.status ?? 'failed'})`}
      className={`${shape} cursor-pointer border-slate-200 bg-white text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900`}
    >
      {body}
      <span
        className={`h-1.5 w-1.5 shrink-0 rounded-full ${last.ok ? 'bg-emerald-500' : 'bg-red-500'}`}
        aria-hidden="true"
      />
    </button>
  );
}
