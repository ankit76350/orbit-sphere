/**
 * API Battleground — a place to fire requests at the Orbit Sphere backend and see exactly what
 * happened.
 *
 * It holds the endpoint you picked, the request you are editing, the last response, and the
 * history. Every call it makes is a real one; there is no sample data anywhere in this app.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Settings, Activity, Zap, ChevronDown } from 'lucide-react';
import Sidebar from './components/Sidebar.jsx';
import RequestPanel from './components/RequestPanel.jsx';
import ResponsePanel from './components/ResponsePanel.jsx';
import HistoryPanel from './components/HistoryPanel.jsx';
import SettingsDrawer from './components/SettingsDrawer.jsx';
import { Pill } from './components/ui.jsx';
import { ALL_ENDPOINTS, findEndpoint, LIVE_COUNT } from './config/endpoints.js';
import { buildRequest, sendRequest } from './lib/httpClient.js';
import { readPath } from './lib/variables.js';
import { store, toHistoryEntry, HISTORY_LIMIT } from './lib/store.js';

/** Builds the editable request from an endpoint's definition. */
function draftFromEndpoint(endpoint, auth) {
  if (!endpoint) return null;
  return {
    method: endpoint.method,
    path: endpoint.path,
    pathParams: (endpoint.pathParams || []).map((param) => ({ ...param })),
    queryParams: (endpoint.queryParams || []).map((param) => ({ enabled: true, ...param })),
    headers: (endpoint.headers || []).map((header) => ({ enabled: true, ...header })),
    body: endpoint.body ?? '',
    auth,
  };
}

export default function App() {
  const firstEndpoint = ALL_ENDPOINTS[0];

  const [selectedId, setSelectedId] = useState(firstEndpoint.id);
  const [environments, setEnvironments] = useState(() => store.loadEnvironments());
  const [activeEnvironmentId, setActiveEnvironmentId] = useState(() => store.loadActiveEnvironmentId());
  const [auth, setAuth] = useState(() => store.loadAuth());
  const [variables, setVariables] = useState(() => store.loadVariables());
  const [timeoutMs, setTimeoutMs] = useState(() => store.loadTimeout());
  const [history, setHistory] = useState(() => store.loadHistory());

  const [draft, setDraft] = useState(() => draftFromEndpoint(firstEndpoint, store.loadAuth()));
  const [requestTab, setRequestTab] = useState('body');
  const [result, setResult] = useState(null);
  const [sending, setSending] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [activeHistoryId, setActiveHistoryId] = useState(null);
  const [lastCapture, setLastCapture] = useState(null);

  // Kept so Cancel can drop a request that is still in flight.
  const inFlight = useRef(null);

  const endpoint = useMemo(() => findEndpoint(selectedId), [selectedId]);
  const environment = useMemo(
    () => environments.find((env) => env.id === activeEnvironmentId) || environments[0],
    [environments, activeEnvironmentId],
  );

  // Save the settings whenever they change, so a refresh does not lose them.
  useEffect(() => store.saveEnvironments(environments), [environments]);
  useEffect(() => store.saveActiveEnvironmentId(activeEnvironmentId), [activeEnvironmentId]);
  useEffect(() => store.saveAuth(auth), [auth]);
  useEffect(() => store.saveVariables(variables), [variables]);
  useEffect(() => store.saveTimeout(timeoutMs), [timeoutMs]);
  useEffect(() => store.saveHistory(history), [history]);

  // Picking a different endpoint loads its request and clears the old response.
  const selectEndpoint = useCallback(
    (id) => {
      const next = findEndpoint(id);
      if (!next) return;
      setSelectedId(id);
      setDraft(draftFromEndpoint(next, auth));
      setResult(null);
      setActiveHistoryId(null);
      setLastCapture(null);
      setRequestTab(next.bodyAllowed ? 'body' : 'docs');
    },
    [auth],
  );

  // The Auth tab edits the draft, and the credentials are shared by every endpoint, so keep
  // the two in step.
  const draftAuth = draft?.auth;
  useEffect(() => {
    if (draftAuth && draftAuth !== auth) setAuth(draftAuth);
  }, [draftAuth]); // eslint-disable-line react-hooks/exhaustive-deps

  const send = useCallback(
    async (overrideDraft) => {
      const useDraft = overrideDraft || draft;
      if (!useDraft) return;

      const prepared = buildRequest(useDraft, environment, variables);
      setSending(true);

      // A token per send, so an answer that arrives after Cancel, or after a newer send
      // started, is thrown away instead of overwriting the panel.
      const token = {};
      inFlight.current = token;

      const finished = await sendRequest(prepared, { timeoutMs });
      if (inFlight.current !== token) return;
      inFlight.current = null;

      setSending(false);
      setResult(finished);
      setActiveHistoryId(null);

      // Remember anything the endpoint says to remember, so the next call in the flow works
      // without copying an id by hand.
      if (finished.ok && endpoint?.captures?.length && finished.bodyJson) {
        const captured = {};
        endpoint.captures.forEach((rule) => {
          const value = readPath(finished.bodyJson, rule.from);
          if (value != null && value !== '') captured[rule.variable] = String(value);
        });
        if (Object.keys(captured).length > 0) {
          setVariables((was) => ({ ...was, ...captured }));
          setLastCapture(captured);
          setTimeout(() => setLastCapture(null), 6000);
        }
      }

      setHistory((was) =>
        [toHistoryEntry({ endpoint, environment, result: finished, draft: useDraft }), ...was].slice(
          0,
          HISTORY_LIMIT,
        ),
      );
    },
    [draft, environment, variables, timeoutMs, endpoint],
  );

  const cancel = useCallback(() => {
    inFlight.current = null;
    setSending(false);
  }, []);

  // Ctrl or Cmd + Enter sends, the way every other tool does it.
  useEffect(() => {
    const onKeyDown = (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
        event.preventDefault();
        if (!sending) send();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [send, sending]);

  const inspectHistory = useCallback((entry) => {
    setResult(entry.result);
    setActiveHistoryId(entry.id);
    if (entry.endpointId) setSelectedId(entry.endpointId);
    if (entry.draft) setDraft(entry.draft);
  }, []);

  const resendHistory = useCallback(
    (entry) => {
      if (entry.endpointId) setSelectedId(entry.endpointId);
      if (entry.draft) setDraft(entry.draft);
      send(entry.draft);
    },
    [send],
  );

  return (
    <div className="flex h-screen flex-col bg-slate-950 text-slate-200">
      <header className="flex flex-wrap items-center gap-3 border-b border-slate-800 bg-slate-900 px-4 py-2.5">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-sky-500/15">
            <Zap size={15} className="text-sky-400" />
          </div>
          <div>
            <h1 className="text-sm font-semibold leading-tight text-slate-100">API Battleground</h1>
            <p className="text-[10px] leading-tight text-slate-500">
              Orbit Sphere · {LIVE_COUNT} endpoints built · real calls, no sample data
            </p>
          </div>
        </div>

        <div className="ml-auto flex flex-wrap items-center gap-2">
          {lastCapture && (
            <Pill tone="emerald">
              Saved{' '}
              {Object.entries(lastCapture)
                .map(([name, value]) => `${name} = ${value}`)
                .join(', ')}
            </Pill>
          )}

          <label className="flex items-center gap-2 rounded-md border border-slate-700 bg-slate-800/60 px-2 py-1">
            <Activity size={12} className="text-slate-500" />
            <span className="text-[10px] uppercase tracking-wide text-slate-500">Env</span>
            <div className="relative">
              <select
                value={activeEnvironmentId}
                onChange={(event) => setActiveEnvironmentId(event.target.value)}
                className="appearance-none bg-transparent pr-4 text-xs font-medium text-slate-100 focus:outline-none"
              >
                {environments.map((env) => (
                  <option key={env.id} value={env.id} className="bg-slate-900">
                    {env.name}
                  </option>
                ))}
              </select>
              <ChevronDown size={11} className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-slate-500" />
            </div>
            <span className="font-mono text-[10px] text-slate-500">
              {environment?.baseUrl || 'same origin (proxy)'}
            </span>
          </label>

          <button
            type="button"
            onClick={() => setSettingsOpen(true)}
            className="inline-flex items-center gap-1.5 rounded-md border border-slate-700 px-2.5 py-1.5 text-xs text-slate-300 hover:border-slate-600 hover:text-slate-100"
          >
            <Settings size={13} /> Settings
          </button>
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <Sidebar selectedId={selectedId} onSelect={selectEndpoint} />

        <main className="flex min-h-0 min-w-0 flex-1 flex-col">
          <div className="border-b border-slate-800 bg-slate-900/40 px-4 py-2">
            <div className="flex flex-wrap items-baseline gap-2">
              <h2 className="text-sm font-semibold text-slate-100">{endpoint?.name}</h2>
              <span className="text-[11px] text-slate-500">{endpoint?.module}</span>
              {endpoint?.status === 'planned' && <Pill tone="amber">Not built yet — expect 404</Pill>}
              {endpoint?.phase && <Pill tone="slate">Phase {endpoint.phase}</Pill>}
            </div>
            {endpoint?.summary && <p className="mt-0.5 text-[11px] text-slate-400">{endpoint.summary}</p>}
          </div>

          {draft && (
            <RequestPanel
              endpoint={endpoint}
              draft={draft}
              onDraftChange={setDraft}
              environment={environment}
              variables={variables}
              onSend={() => send()}
              onCancel={cancel}
              sending={sending}
              activeTab={requestTab}
              onTabChange={setRequestTab}
              onClearRequest={() => setDraft(draftFromEndpoint(endpoint, auth))}
              onUseExample={(example) => {
                setDraft((was) => ({ ...was, body: example.body ?? '' }));
                setRequestTab('body');
              }}
            />
          )}

          <ResponsePanel
            result={result}
            sending={sending}
            method={draft?.method}
            onClear={() => {
              setResult(null);
              setActiveHistoryId(null);
            }}
            onResend={() => send()}
          />
        </main>

        <HistoryPanel
          history={history}
          activeId={activeHistoryId}
          onInspect={inspectHistory}
          onResend={resendHistory}
          onClear={() => {
            setHistory([]);
            store.clearHistory();
          }}
        />
      </div>

      <SettingsDrawer
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        environments={environments}
        onEnvironmentsChange={setEnvironments}
        activeEnvironmentId={activeEnvironmentId}
        variables={variables}
        onVariablesChange={setVariables}
        timeoutMs={timeoutMs}
        onTimeoutChange={setTimeoutMs}
      />
    </div>
  );
}
