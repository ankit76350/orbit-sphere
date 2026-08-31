/**
 * Turns "do this action, on this school" into an actual HTTP request.
 *
 * Kept apart from ApiProvider so it can be exercised on its own — the header the school
 * surface needs and the way repeated query parameters are written are both easy to get wrong
 * and impossible to see from the screen.
 */

import { findEndpoint } from '../config/endpoints.js';
import { buildRequest } from '../lib/httpClient.js';

/** Turns { status: ['ACTIVE','TRIAL'], page: 0 } into the rows the request builder wants. */
export function toQueryRows(query) {
  const rows = [];
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((one) => {
        if (one !== undefined && one !== null && one !== '') {
          rows.push({ key, value: String(one), enabled: true });
        }
      });
      return;
    }
    rows.push({ key, value: String(value), enabled: true });
  });
  return rows;
}

/**
 * Builds the request for one action.
 *
 * `options` takes `pathParams`, `query`, `body` and `subdomain`. The subdomain is the school
 * being worked on: every school-facing endpoint needs it as a header, because there is no
 * sign-in yet and the backend reads the tenant from there. Nobody types a header.
 */
export function buildCall(endpointId, options = {}, environment) {
  const endpoint = findEndpoint(endpointId);
  if (!endpoint) throw new Error(`No endpoint called ${endpointId}`);

  const headers = [];
  const hasBody = options.body !== undefined && options.body !== null;
  if (hasBody) headers.push({ key: 'Content-Type', value: 'application/json', enabled: true });
  if (endpoint.schoolSurface && options.subdomain) {
    headers.push({ key: 'X-School-Subdomain', value: options.subdomain, enabled: true });
  }

  const draft = {
    method: endpoint.method,
    path: endpoint.path,
    pathParams: endpoint.pathParams.map((param) => ({
      ...param,
      value: options.pathParams?.[param.name] ?? '',
    })),
    queryParams: toQueryRows(options.query),
    headers,
    body: hasBody ? JSON.stringify(options.body, null, 2) : '',
    auth: { type: 'none' },
  };

  return { endpoint, prepared: buildRequest(draft, environment, {}) };
}
