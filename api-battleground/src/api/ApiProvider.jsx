/**
 * The one place the app talks to the backend.
 *
 * Screens call `call('activate-school', { pathParams: { id } })` and get the result back.
 * Everything else happens here: the tenant header is added, the call is written into the
 * activity log, and anything that CHANGES something opens the details pop-up straight away —
 * what was sent, what came back, how long it took.
 *
 * Reads are silent. A GET runs when a page loads, and a pop-up on every page load would be
 * unusable; a list that fails says so in the page itself instead.
 *
 * The paths and methods are not repeated here — they come from config/endpoints.js, which is
 * generated from the Postman collection.
 *
 * The contexts and the hooks that read them are in apiContext.js — see the note there for why
 * there are two of them, and why this file must export nothing but its component.
 */

import { useCallback, useMemo, useRef, useState } from 'react';
import { sendRequest } from '../lib/httpClient.js';
import { store } from '../lib/store.js';
import { buildCall } from './buildCall.js';
import { ApiActionsContext, ApiStateContext } from './apiContext.js';

/**
 * The stand-in result for an environment with no address. Shaped exactly like a real one, so
 * the activity list and the details pop-up handle it without a special case.
 */
function notConfigured(prepared, environment) {
  const now = new Date().toISOString();
  return {
    ok: false,
    request: prepared,
    status: null,
    statusText: null,
    headers: {},
    bodyText: '',
    bodyJson: null,
    jsonParseError: null,
    sizeBytes: 0,
    durationMs: 0,
    startedAtIso: now,
    finishedAtIso: now,
    timeoutMs: 0,
    error: {
      kind: 'not-configured',
      title: `${environment.name} has no address`,
      message: `Nothing was sent. ${environment.name} has no server address set, so there is nowhere to send it.`,
      hint: 'Pick "Development (proxy)" in the header to work against the local backend.',
    },
  };
}

export default function ApiProvider({ children }) {
  const [environments] = useState(() => store.loadEnvironments());
  const [environmentId, setEnvironmentId] = useState(() => store.loadActiveEnvironmentId());
  const [log, setLog] = useState([]);
  const [inspecting, setInspecting] = useState(null);

  const environment = useMemo(
    () => environments.find((one) => one.id === environmentId) || environments[0],
    [environments, environmentId],
  );

  // Read through a ref inside call(), so switching environment does not give call() a new
  // identity and quietly reintroduce the reload loop.
  const environmentRef = useRef(environment);
  environmentRef.current = environment;

  const chooseEnvironment = useCallback((id) => {
    setEnvironmentId(id);
    store.saveActiveEnvironmentId(id);
  }, []);

  const call = useCallback(
    async (endpointId, options = {}) => {
      const environment = environmentRef.current;
      const { endpoint, prepared } = buildCall(endpointId, options, environment);

      // An environment nobody has set up yet has no address. Say that, rather than letting the
      // browser fail to look up a host and report it as a network error.
      const result = environment?.placeholder && !environment.baseUrl
        ? notConfigured(prepared, environment)
        : await sendRequest(prepared, { timeoutMs: store.loadTimeout() });

      const entry = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        at: result.startedAtIso,
        endpointId,
        endpointName: endpoint.name,
        action: options.label || endpoint.name,
        module: endpoint.module,
        method: result.request.method,
        path: result.request.path,
        url: result.request.url,
        status: result.status,
        ok: result.ok,
        durationMs: result.durationMs,
        sizeBytes: result.sizeBytes,
        environmentName: environment?.name,
        result,
        endpoint,
      };
      setLog((was) => [entry, ...was].slice(0, 100));

      // Anything that changes something shows its answer straight away. A read does not: it
      // runs on page load, and the page shows its own message if it fails.
      if (endpoint.method !== 'GET') setInspecting(entry);

      return result;
    },
    [],
  );

  const clearLog = useCallback(() => setLog([]), []);

  const actions = useMemo(
    () => ({ call, inspect: setInspecting, clearLog, chooseEnvironment }),
    [call, clearLog, chooseEnvironment],
  );

  const state = useMemo(
    () => ({ log, inspecting, environment, environments }),
    [log, inspecting, environment, environments],
  );

  return (
    <ApiActionsContext.Provider value={actions}>
      <ApiStateContext.Provider value={state}>{children}</ApiStateContext.Provider>
    </ApiActionsContext.Provider>
  );
}
