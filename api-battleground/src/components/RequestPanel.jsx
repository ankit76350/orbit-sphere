/**
 * The top half of the main area: the method, the URL, the Send button, and the tabs that make
 * up the request.
 */

import { useMemo } from 'react';
import { Send, Loader2, Eraser, Wand2, AlertTriangle, Info, Link as LinkIcon } from 'lucide-react';
import { HTTP_METHODS, METHOD_STYLE, TabBar, KeyValueEditor, Field, inputClass, Pill } from './ui.jsx';
import { CopyButton } from './JsonViewer.jsx';
import { AUTH_TYPES } from '../config/environments.js';
import { checkJson, prettyPrint, minifyJson } from '../lib/format.js';
import { buildRequest } from '../lib/httpClient.js';
import { missingVariables } from '../lib/variables.js';

function DocsTab({ endpoint }) {
  if (!endpoint) return null;
  return (
    <div className="space-y-4 text-xs">
      {endpoint.status === 'planned' && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2.5">
          <AlertTriangle size={14} className="mt-0.5 shrink-0 text-amber-300" />
          <p className="text-amber-100">
            This endpoint is not built. It comes from the plan in{' '}
            <span className="font-mono">controllers/core/README.md</span>. Sending it returns a 404
            from Spring, which is the real answer for a path that does not exist yet.
          </p>
        </div>
      )}
      <p className="whitespace-pre-line leading-relaxed text-slate-300">{endpoint.docs}</p>

      {(endpoint.requiredFields?.length || endpoint.optionalFields?.length) && (
        <div className="grid gap-3 sm:grid-cols-2">
          {endpoint.requiredFields?.length > 0 && (
            <div>
              <h4 className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-500">Required</h4>
              <div className="flex flex-wrap gap-1">
                {endpoint.requiredFields.map((field) => (
                  <Pill key={field} tone="rose">
                    {field}
                  </Pill>
                ))}
              </div>
            </div>
          )}
          {endpoint.optionalFields?.length > 0 && (
            <div>
              <h4 className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-500">Optional</h4>
              <div className="flex flex-wrap gap-1">
                {endpoint.optionalFields.map((field) => (
                  <Pill key={field} tone="slate">
                    {field}
                  </Pill>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {endpoint.responseFields?.length > 0 && (
        <div>
          <h4 className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-500">
            Comes back with ({endpoint.successStatus})
          </h4>
          <div className="flex flex-wrap gap-1">
            {endpoint.responseFields.map((field) => (
              <Pill key={field} tone="emerald">
                {field}
              </Pill>
            ))}
          </div>
          {endpoint.successNote && <p className="mt-1.5 text-slate-500">{endpoint.successNote}</p>}
        </div>
      )}

      {endpoint.errors?.length > 0 && (
        <div>
          <h4 className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-500">
            What can go wrong
          </h4>
          <div className="overflow-hidden rounded-lg border border-slate-700/70">
            <table className="w-full text-left">
              <tbody>
                {endpoint.errors.map((error, index) => (
                  <tr key={index} className="border-t border-slate-700/50 first:border-t-0">
                    <td className="w-14 px-3 py-2 align-top font-mono text-[11px] text-amber-300">{error.status}</td>
                    <td className="w-56 px-3 py-2 align-top font-mono text-[11px] text-slate-300">{error.code}</td>
                    <td className="px-3 py-2 text-slate-400">{error.when}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function CasesTab({ endpoint, onUseExample }) {
  if (!endpoint?.examples?.length) {
    return (
      <p className="rounded-md border border-dashed border-slate-700 px-3 py-6 text-center text-xs text-slate-500">
        No saved cases for this endpoint yet.
      </p>
    );
  }
  return (
    <div className="space-y-2">
      <p className="text-[11px] text-slate-500">
        The same cases as the Postman collection. Picking one loads its body into the request.
      </p>
      {endpoint.examples.map((example) => {
        const expectsError = !example.expect.startsWith('2');
        return (
          <div
            key={example.id}
            className="flex items-start gap-3 rounded-lg border border-slate-700/70 bg-slate-900/50 px-3 py-2.5"
          >
            <span className="mt-0.5 font-mono text-[11px] text-slate-500">{example.id}</span>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold text-slate-200">{example.name}</span>
                <Pill tone={expectsError ? 'amber' : 'emerald'}>{example.expect}</Pill>
              </div>
              <p className="mt-1 text-[11px] leading-relaxed text-slate-400">{example.notes}</p>
            </div>
            <button
              type="button"
              onClick={() => onUseExample(example)}
              className="shrink-0 rounded-md border border-slate-700 px-2.5 py-1 text-[11px] text-slate-300 hover:border-sky-500/60 hover:text-sky-200"
            >
              Use this
            </button>
          </div>
        );
      })}
    </div>
  );
}

export default function RequestPanel({
  endpoint,
  draft,
  onDraftChange,
  environment,
  variables,
  onSend,
  onCancel,
  sending,
  activeTab,
  onTabChange,
  onClearRequest,
  onUseExample,
}) {
  const bodyCheck = useMemo(() => checkJson(draft.body || ''), [draft.body]);
  const prepared = useMemo(() => buildRequest(draft, environment, variables), [draft, environment, variables]);

  // The whole request is checked for placeholders we have no value for, because a call sent
  // with a literal {{schoolId}} in the URL fails in a confusing way.
  const unresolved = useMemo(() => {
    const pieces = [
      ...(draft.pathParams || []).map((param) => param.value || ''),
      ...(draft.queryParams || []).map((param) => `${param.key}${param.value}`),
      draft.body || '',
    ].join('\n');
    return missingVariables(pieces, variables);
  }, [draft, variables]);

  const methodTakesBody = !['GET', 'HEAD'].includes(draft.method);

  const tabs = [
    {
      id: 'params',
      label: 'Params',
      count: (draft.pathParams?.length || 0) + (draft.queryParams?.filter((p) => p.enabled !== false).length || 0),
    },
    { id: 'headers', label: 'Headers', count: draft.headers?.filter((h) => h.enabled !== false).length || 0 },
    { id: 'auth', label: 'Auth', dot: draft.auth?.type !== 'none' },
    { id: 'body', label: 'Body', dot: methodTakesBody && Boolean(draft.body?.trim()) },
    { id: 'docs', label: 'Docs' },
    { id: 'cases', label: 'Cases', count: endpoint?.examples?.length || 0 },
  ];

  return (
    <div className="flex min-h-0 flex-col">
      {/* The URL bar */}
      <div className="flex flex-wrap items-center gap-2 border-b border-slate-800 px-4 py-3">
        <select
          value={draft.method}
          onChange={(event) => onDraftChange({ ...draft, method: event.target.value })}
          className={`rounded-md border bg-slate-900 px-2 py-1.5 font-mono text-xs font-bold focus:outline-none ${
            METHOD_STYLE[draft.method] || ''
          }`}
        >
          {HTTP_METHODS.map((method) => (
            <option key={method} value={method} className="bg-slate-900 text-slate-200">
              {method}
            </option>
          ))}
        </select>

        <div className="flex min-w-0 flex-1 items-center gap-2 rounded-md border border-slate-700 bg-slate-900 px-2.5 py-1.5">
          <span className="shrink-0 font-mono text-[11px] text-slate-500">
            {environment?.baseUrl || 'proxy'}
          </span>
          <input
            value={draft.path}
            onChange={(event) => onDraftChange({ ...draft, path: event.target.value })}
            spellCheck={false}
            className="min-w-0 flex-1 bg-transparent font-mono text-xs text-slate-100 focus:outline-none"
          />
        </div>

        <CopyButton text={prepared.url} label="Copy URL" title="Copy the full URL, with the variables filled in" />

        {sending ? (
          <button
            type="button"
            onClick={onCancel}
            className="inline-flex items-center gap-1.5 rounded-md bg-slate-700 px-4 py-1.5 text-xs font-semibold text-slate-100 hover:bg-slate-600"
          >
            <Loader2 size={13} className="animate-spin" />
            Cancel
          </button>
        ) : (
          <button
            type="button"
            onClick={onSend}
            title="Send (Ctrl or Cmd + Enter)"
            className="inline-flex items-center gap-1.5 rounded-md bg-sky-500 px-5 py-1.5 text-xs font-semibold text-white shadow-lg shadow-sky-500/20 transition hover:bg-sky-400"
          >
            <Send size={13} />
            Send
          </button>
        )}
        <button
          type="button"
          onClick={onClearRequest}
          title="Put this request back to how it started"
          className="rounded-md border border-slate-700 p-1.5 text-slate-400 hover:border-slate-600 hover:text-slate-200"
        >
          <Eraser size={13} />
        </button>
      </div>

      {/* What is actually going to be sent */}
      <div className="flex flex-wrap items-center gap-2 border-b border-slate-800 bg-slate-900/40 px-4 py-1.5 text-[11px]">
        <LinkIcon size={11} className="text-slate-600" />
        <span className="min-w-0 flex-1 truncate font-mono text-slate-500" title={prepared.url}>
          {prepared.url}
        </span>
        {unresolved.length > 0 && (
          <Pill tone="amber" title="These placeholders have no value yet">
            <AlertTriangle size={10} /> {unresolved.map((name) => `{{${name}}}`).join(' ')}
          </Pill>
        )}
      </div>

      <TabBar tabs={tabs} active={activeTab} onChange={onTabChange} className="px-4" />

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {activeTab === 'params' && (
          <div className="space-y-5">
            <div>
              <h4 className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                Path parameters
              </h4>
              {draft.pathParams?.length ? (
                <div className="space-y-2">
                  {draft.pathParams.map((param, index) => (
                    <div key={param.name} className="grid gap-2 sm:grid-cols-[10rem_1fr]">
                      <div className="pt-1.5 font-mono text-[11px] text-sky-300">{`{${param.name}}`}</div>
                      <div>
                        <input
                          value={param.value}
                          onChange={(event) => {
                            const next = draft.pathParams.map((one, i) =>
                              i === index ? { ...one, value: event.target.value } : one,
                            );
                            onDraftChange({ ...draft, pathParams: next });
                          }}
                          className={`${inputClass} font-mono`}
                        />
                        {param.description && <p className="mt-1 text-[11px] text-slate-500">{param.description}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="rounded-md border border-dashed border-slate-700 px-3 py-4 text-center text-xs text-slate-500">
                  This path has no parameters in it.
                </p>
              )}
            </div>

            <div>
              <h4 className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                Query parameters
              </h4>
              <KeyValueEditor
                rows={draft.queryParams || []}
                onChange={(rows) => onDraftChange({ ...draft, queryParams: rows })}
                keyPlaceholder="name"
                valuePlaceholder="value"
                emptyHint="No query parameters. Add one and it goes on the end of the URL."
              />
            </div>
          </div>
        )}

        {activeTab === 'headers' && (
          <div className="space-y-3">
            <KeyValueEditor
              rows={draft.headers || []}
              onChange={(rows) => onDraftChange({ ...draft, headers: rows })}
              keyPlaceholder="Header-Name"
              valuePlaceholder="value"
              emptyHint="No headers. Content-Type is added for you when there is a body."
            />
            <p className="text-[11px] text-slate-500">
              The Authorization header is not listed here — it is built from the Auth tab and added
              last.
            </p>
          </div>
        )}

        {activeTab === 'auth' && (
          <div className="max-w-lg space-y-4">
            <Field label="Type" hint="Nothing on this API needs credentials yet. It is here for when it does.">
              <select
                value={draft.auth?.type || 'none'}
                onChange={(event) => onDraftChange({ ...draft, auth: { ...draft.auth, type: event.target.value } })}
                className={inputClass}
              >
                {AUTH_TYPES.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.name}
                  </option>
                ))}
              </select>
            </Field>

            {draft.auth?.type === 'bearer' && (
              <Field label="Token" hint="Sent as: Authorization: Bearer <token>">
                <input
                  value={draft.auth.token}
                  onChange={(event) => onDraftChange({ ...draft, auth: { ...draft.auth, token: event.target.value } })}
                  placeholder="eyJhbGciOi..."
                  className={`${inputClass} font-mono`}
                />
              </Field>
            )}

            {draft.auth?.type === 'basic' && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Username">
                  <input
                    value={draft.auth.username}
                    onChange={(event) =>
                      onDraftChange({ ...draft, auth: { ...draft.auth, username: event.target.value } })
                    }
                    className={inputClass}
                  />
                </Field>
                <Field label="Password">
                  <input
                    type="password"
                    value={draft.auth.password}
                    onChange={(event) =>
                      onDraftChange({ ...draft, auth: { ...draft.auth, password: event.target.value } })
                    }
                    className={inputClass}
                  />
                </Field>
              </div>
            )}

            {draft.auth?.type === 'apiKey' && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Header name">
                  <input
                    value={draft.auth.apiKeyName}
                    onChange={(event) =>
                      onDraftChange({ ...draft, auth: { ...draft.auth, apiKeyName: event.target.value } })
                    }
                    className={`${inputClass} font-mono`}
                  />
                </Field>
                <Field label="Key">
                  <input
                    value={draft.auth.apiKeyValue}
                    onChange={(event) =>
                      onDraftChange({ ...draft, auth: { ...draft.auth, apiKeyValue: event.target.value } })
                    }
                    className={`${inputClass} font-mono`}
                  />
                </Field>
              </div>
            )}

            <div className="flex items-start gap-2 rounded-lg border border-slate-700/70 bg-slate-900/50 px-3 py-2.5">
              <Info size={13} className="mt-0.5 shrink-0 text-slate-500" />
              <p className="text-[11px] leading-relaxed text-slate-400">
                The credentials are saved in this browser so you do not retype them, and the request
                details panel shows them shortened rather than in full.
              </p>
            </div>
          </div>
        )}

        {activeTab === 'body' && (
          <div className="space-y-2">
            {!methodTakesBody ? (
              <p className="rounded-md border border-dashed border-slate-700 px-3 py-6 text-center text-xs text-slate-500">
                A {draft.method} does not send a body. Anything typed here is left out of the request.
              </p>
            ) : (
              <>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">JSON body</span>
                  {endpoint && !endpoint.bodyAllowed && <Pill tone="slate">This endpoint ignores the body</Pill>}
                  <div className="ml-auto flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => onDraftChange({ ...draft, body: prettyPrint(draft.body || '') })}
                      className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] text-slate-400 hover:bg-slate-700/60 hover:text-slate-100"
                      title="Tidy the JSON up"
                    >
                      <Wand2 size={12} /> Pretty
                    </button>
                    <button
                      type="button"
                      onClick={() => onDraftChange({ ...draft, body: minifyJson(draft.body || '') })}
                      className="rounded-md px-2 py-1 text-[11px] text-slate-400 hover:bg-slate-700/60 hover:text-slate-100"
                    >
                      Minify
                    </button>
                    <button
                      type="button"
                      onClick={() => onDraftChange({ ...draft, body: '' })}
                      className="rounded-md px-2 py-1 text-[11px] text-slate-400 hover:bg-slate-700/60 hover:text-slate-100"
                    >
                      Clear
                    </button>
                    <CopyButton text={draft.body || ''} label="Copy" />
                  </div>
                </div>

                <textarea
                  value={draft.body || ''}
                  onChange={(event) => onDraftChange({ ...draft, body: event.target.value })}
                  spellCheck={false}
                  rows={15}
                  placeholder={'{\n  "key": "value"\n}'}
                  className={`w-full rounded-lg border bg-slate-900/80 p-3 font-mono text-[12px] leading-6 text-slate-200 placeholder:text-slate-600 focus:outline-none ${
                    bodyCheck.valid ? 'border-slate-700 focus:border-sky-500' : 'border-rose-500/60'
                  }`}
                />

                {!bodyCheck.valid ? (
                  <p className="flex items-start gap-1.5 text-[11px] text-rose-300">
                    <AlertTriangle size={12} className="mt-0.5 shrink-0" />
                    Not valid JSON — {bodyCheck.error}. It is still sent exactly as typed, so you can
                    check what the backend does with a broken body.
                  </p>
                ) : (
                  <p className="text-[11px] text-slate-500">
                    Placeholders like <span className="font-mono">{'{{schoolId}}'}</span> and{' '}
                    <span className="font-mono">{'{{$timestamp}}'}</span> are filled in when you send.
                  </p>
                )}
              </>
            )}
          </div>
        )}

        {activeTab === 'docs' && <DocsTab endpoint={endpoint} />}
        {activeTab === 'cases' && <CasesTab endpoint={endpoint} onUseExample={onUseExample} />}
      </div>
    </div>
  );
}
