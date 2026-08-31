/**
 * Where the environments, the saved variables and the timeout are edited.
 *
 * Everything here is kept in the browser, so nothing has to be typed twice, and no base URL is
 * written into the app's code anywhere.
 */

import { X, Plus, Trash2, RotateCcw } from 'lucide-react';
import { Field, inputClass, Pill } from './ui.jsx';
import { DEFAULT_ENVIRONMENTS } from '../config/environments.js';

export default function SettingsDrawer({
  open,
  onClose,
  environments,
  onEnvironmentsChange,
  activeEnvironmentId,
  variables,
  onVariablesChange,
  timeoutMs,
  onTimeoutChange,
}) {
  if (!open) return null;

  const updateEnvironment = (id, patch) =>
    onEnvironmentsChange(environments.map((env) => (env.id === id ? { ...env, ...patch } : env)));

  const addEnvironment = () =>
    onEnvironmentsChange([
      ...environments,
      {
        id: `custom-${Date.now()}`,
        name: 'New environment',
        baseUrl: 'http://localhost:8080',
        description: '',
      },
    ]);

  const isBuiltIn = (id) => DEFAULT_ENVIRONMENTS.some((env) => env.id === id);

  const setVariable = (name, value) => onVariablesChange({ ...variables, [name]: value });
  const removeVariable = (name) => {
    const next = { ...variables };
    delete next[name];
    onVariablesChange(next);
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <button
        type="button"
        aria-label="Close settings"
        onClick={onClose}
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
      />
      <div className="relative flex h-full w-full max-w-xl flex-col border-l border-slate-800 bg-slate-900 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 px-5 py-3">
          <h2 className="text-sm font-semibold text-slate-100">Settings</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-100"
          >
            <X size={16} />
          </button>
        </div>

        <div className="min-h-0 flex-1 space-y-7 overflow-y-auto p-5">
          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Environments</h3>
              <button
                type="button"
                onClick={addEnvironment}
                className="inline-flex items-center gap-1 rounded-md border border-slate-700 px-2 py-1 text-[11px] text-slate-300 hover:border-sky-500/60 hover:text-sky-200"
              >
                <Plus size={11} /> Add
              </button>
            </div>
            <p className="text-[11px] leading-relaxed text-slate-500">
              An empty base URL means the call goes to this page's own origin, and the dev server
              forwards it to the backend. That is the one to use locally: no CORS, and every response
              header is readable.
            </p>

            {environments.map((env) => (
              <div
                key={env.id}
                className={`space-y-2 rounded-lg border px-3 py-3 ${
                  env.id === activeEnvironmentId
                    ? 'border-sky-500/40 bg-sky-500/5'
                    : 'border-slate-700/70 bg-slate-900/50'
                }`}
              >
                <div className="flex items-center gap-2">
                  <input
                    value={env.name}
                    onChange={(event) => updateEnvironment(env.id, { name: event.target.value })}
                    className="flex-1 bg-transparent text-xs font-semibold text-slate-100 focus:outline-none"
                  />
                  {env.id === activeEnvironmentId && <Pill tone="sky">in use</Pill>}
                  {!isBuiltIn(env.id) && (
                    <button
                      type="button"
                      onClick={() => onEnvironmentsChange(environments.filter((one) => one.id !== env.id))}
                      className="rounded p-1 text-slate-500 hover:bg-rose-500/10 hover:text-rose-300"
                    >
                      <Trash2 size={12} />
                    </button>
                  )}
                </div>
                <input
                  value={env.baseUrl}
                  onChange={(event) => updateEnvironment(env.id, { baseUrl: event.target.value })}
                  placeholder="Leave empty to go through the dev proxy"
                  className={`${inputClass} font-mono`}
                />
                {env.description && <p className="text-[11px] text-slate-500">{env.description}</p>}
              </div>
            ))}
          </section>

          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Variables</h3>
              <button
                type="button"
                onClick={() => {
                  const name = window.prompt('Name of the new variable (without the braces)');
                  if (name && name.trim()) setVariable(name.trim(), '');
                }}
                className="inline-flex items-center gap-1 rounded-md border border-slate-700 px-2 py-1 text-[11px] text-slate-300 hover:border-sky-500/60 hover:text-sky-200"
              >
                <Plus size={11} /> Add
              </button>
            </div>
            <p className="text-[11px] leading-relaxed text-slate-500">
              Written as <span className="font-mono">{'{{name}}'}</span> in a URL, a header or a body.{' '}
              <span className="font-mono">schoolId</span> and{' '}
              <span className="font-mono">createdSubdomain</span> fill themselves in after a Create
              School that works. <span className="font-mono">{'{{$timestamp}}'}</span> is made fresh on
              every send, which is what keeps a subdomain unique.
            </p>
            <div className="space-y-2">
              {Object.entries(variables).map(([name, value]) => (
                <div key={name} className="flex items-center gap-2">
                  <span className="w-40 shrink-0 truncate font-mono text-[11px] text-sky-300">{`{{${name}}}`}</span>
                  <input
                    value={value}
                    onChange={(event) => setVariable(name, event.target.value)}
                    placeholder="not set"
                    className={`${inputClass} font-mono`}
                  />
                  <button
                    type="button"
                    onClick={() => removeVariable(name)}
                    className="rounded p-1 text-slate-500 hover:bg-rose-500/10 hover:text-rose-300"
                  >
                    <Trash2 size={12} />
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Timeout</h3>
            <Field
              label="Give up after"
              hint="Spring Boot takes about a minute to start, so allow for that if you are starting it at the same time."
            >
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  min={1000}
                  step={1000}
                  value={timeoutMs}
                  onChange={(event) => onTimeoutChange(Number(event.target.value) || 1000)}
                  className={`${inputClass} font-mono`}
                />
                <span className="shrink-0 text-xs text-slate-500">ms</span>
                <button
                  type="button"
                  onClick={() => onTimeoutChange(30000)}
                  className="shrink-0 rounded-md border border-slate-700 p-1.5 text-slate-400 hover:text-slate-200"
                  title="Back to 30 seconds"
                >
                  <RotateCcw size={12} />
                </button>
              </div>
            </Field>
          </section>
        </div>
      </div>
    </div>
  );
}
