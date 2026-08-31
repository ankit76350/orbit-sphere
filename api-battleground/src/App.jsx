/**
 * Orbit Sphere — the school administration screens.
 *
 * This is a working application, not a request builder: you fill in forms and press buttons,
 * and it calls the real backend. Nothing here is sample data.
 *
 * Anything that changes something shows its answer straight away: the details pop-up opens with
 * a plain summary of what changed, and the full request and response a tab away. The Activity
 * button in the header lists every call the app has made.
 */

import { useState } from 'react';
import { GraduationCap, Activity as ActivityIcon, Server, AlertTriangle } from 'lucide-react';
import ApiProvider, { useApi, useApiState } from './api/ApiProvider.jsx';
import SchoolsPage from './pages/SchoolsPage.jsx';
import SchoolDetail from './pages/SchoolDetail.jsx';
import ApiDetailsModal from './components/ApiDetailsModal.jsx';
import { ActivityModal } from './components/ApiActivity.jsx';
import { SelectInput, Badge } from './components/ui.jsx';

function Shell() {
  const api = useApi();
  const { log, inspecting, environment, environments } = useApiState();
  const [school, setSchool] = useState(null);
  const [activityOpen, setActivityOpen] = useState(false);

  const failures = log.filter((one) => !one.ok).length;

  return (
    <div className="min-h-full">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-3 px-6 py-3">
          <button
            type="button"
            onClick={() => setSchool(null)}
            className="flex items-center gap-2.5 text-left"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600">
              <GraduationCap size={17} className="text-white" />
            </span>
            <span>
              <span className="block text-sm font-semibold leading-tight text-slate-900">Orbit Sphere</span>
              <span className="block text-[11px] leading-tight text-slate-500">School administration</span>
            </span>
          </button>

          <div className="ml-auto flex flex-wrap items-center gap-2">
            {/* Which backend the app is talking to. */}
            <span className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 py-1 pl-2.5 pr-1">
              <Server size={13} className="text-slate-400" />
              <SelectInput
                value={environment?.id}
                onChange={(event) => api.chooseEnvironment(event.target.value)}
                className="w-auto border-0 bg-transparent px-1 py-0.5 text-xs shadow-none focus:ring-0"
              >
                {environments.map((one) => (
                  <option key={one.id} value={one.id}>
                    {one.name}
                    {one.placeholder && !one.baseUrl ? ' — not set up' : ''}
                  </option>
                ))}
              </SelectInput>
            </span>

            <button
              type="button"
              onClick={() => setActivityOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
            >
              <ActivityIcon size={14} className="text-slate-400" />
              Activity
              {log.length > 0 && (
                <Badge look={failures > 0 ? 'red' : 'grey'}>
                  {failures > 0 ? `${failures} failed` : log.length}
                </Badge>
              )}
            </button>
          </div>
        </div>
      </header>

      {environment?.placeholder && !environment.baseUrl && (
        <div className="border-b border-amber-200 bg-amber-50">
          <div className="mx-auto flex max-w-7xl items-center gap-2.5 px-6 py-2.5">
            <AlertTriangle size={15} className="shrink-0 text-amber-600" />
            <p className="text-xs text-amber-900">
              <strong>{environment.name}</strong> has no server address set, so nothing can be
              loaded or saved. Switch to <strong>Development (proxy)</strong> in the header to work
              against the local backend.
            </p>
          </div>
        </div>
      )}

      <main className="mx-auto max-w-7xl px-6 py-7">
        {school ? (
          <SchoolDetail
            school={school}
            onBack={() => setSchool(null)}
            onChanged={(updated) => setSchool(updated)}
          />
        ) : (
          <SchoolsPage onOpenSchool={setSchool} />
        )}
      </main>

      <ActivityModal
        open={activityOpen}
        onClose={() => setActivityOpen(false)}
        log={log}
        onInspect={(entry) => {
          setActivityOpen(false);
          api.inspect(entry);
        }}
        onClear={api.clearLog}
      />

      <ApiDetailsModal entry={inspecting} onClose={() => api.inspect(null)} />
    </div>
  );
}

export default function App() {
  return (
    <ApiProvider>
      <Shell />
    </ApiProvider>
  );
}
