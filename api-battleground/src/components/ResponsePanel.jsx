/**
 * The bottom half of the main area: what came back.
 *
 * The first tab depends on the method. A GET opens on the app-like view, because for a read
 * the useful thing is the data itself. Everything else opens on the raw body, because for a
 * write the useful thing is exactly what the backend said.
 */

import { useEffect, useMemo, useState } from 'react';
import {
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  HardDrive,
  Timer,
  WifiOff,
  Eraser,
  RotateCw,
} from 'lucide-react';
import JsonViewer, { CopyButton } from './JsonViewer.jsx';
import DataView from './DataView.jsx';
import { TabBar, Pill } from './ui.jsx';
import {
  formatBytes,
  formatClock,
  formatDuration,
  statusText,
  statusTone,
  STATUS_MEANING,
} from '../lib/format.js';
import { maskSecret } from '../lib/httpClient.js';

const TONE_TEXT = {
  emerald: 'text-emerald-300',
  amber: 'text-amber-300',
  rose: 'text-rose-300',
  sky: 'text-sky-300',
};

/** The one line that says how it went: status, time, size. */
function StatusStrip({ result }) {
  const toneClass = TONE_TEXT[statusTone(result.status)];
  return (
    <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-b border-slate-800 bg-slate-900/50 px-4 py-2.5">
      <div className="flex items-center gap-2">
        {result.error ? (
          result.error.kind === 'timeout' ? (
            <Timer size={15} className="text-rose-300" />
          ) : (
            <WifiOff size={15} className="text-rose-300" />
          )
        ) : result.ok ? (
          <CheckCircle2 size={15} className="text-emerald-300" />
        ) : result.status >= 500 ? (
          <XCircle size={15} className="text-rose-300" />
        ) : (
          <AlertTriangle size={15} className="text-amber-300" />
        )}
        <span className={`font-mono text-sm font-bold ${toneClass}`}>
          {result.error ? result.error.title : `${result.status} ${statusText(result.status, result.statusText)}`}
        </span>
      </div>

      <div className="flex items-center gap-1.5 text-xs text-slate-400">
        <Clock size={12} className="text-slate-600" />
        {formatDuration(result.durationMs)}
      </div>
      <div className="flex items-center gap-1.5 text-xs text-slate-400">
        <HardDrive size={12} className="text-slate-600" />
        {formatBytes(result.sizeBytes)}
      </div>
      <div className="hidden items-center gap-1.5 text-xs text-slate-500 sm:flex">
        started {formatClock(result.startedAtIso)} · finished {formatClock(result.finishedAtIso)}
      </div>

      <div className="ml-auto">
        <CopyButton
          text={result.bodyJson ? JSON.stringify(result.bodyJson, null, 2) : result.bodyText}
          label="Copy body"
        />
      </div>
    </div>
  );
}

/** What went wrong, pulled out of the body and said plainly. */
function ErrorSummary({ result }) {
  if (result.error) {
    return (
      <div className="space-y-2 rounded-lg border border-rose-500/30 bg-rose-500/10 px-4 py-3">
        <div className="flex items-center gap-2">
          <WifiOff size={14} className="text-rose-300" />
          <h4 className="text-sm font-semibold text-rose-200">{result.error.title}</h4>
        </div>
        <p className="text-xs text-rose-100">{result.error.message}</p>
        {result.error.hint && <p className="text-[11px] text-rose-200/70">{result.error.hint}</p>}
        <p className="text-[11px] text-rose-200/60">
          Nothing came back, so there is no status code and no response body. It gave up after{' '}
          {formatDuration(result.durationMs)}.
        </p>
      </div>
    );
  }

  if (result.ok) return null;

  const body = result.bodyJson;
  const code = body?.code || body?.error;
  const message = body?.message || body?.detail;
  const fieldErrors = body?.fieldErrors;

  return (
    <div className="space-y-3 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-3">
      <div className="flex flex-wrap items-center gap-2">
        <AlertTriangle size={14} className="text-amber-300" />
        <span className="font-mono text-sm font-semibold text-amber-200">
          {result.status} {statusText(result.status, result.statusText)}
        </span>
        {code && <Pill tone="amber">{code}</Pill>}
      </div>
      {message && <p className="text-xs text-amber-50">{message}</p>}
      {STATUS_MEANING[result.status] && (
        <p className="text-[11px] text-amber-200/70">{STATUS_MEANING[result.status]}</p>
      )}

      {fieldErrors && Object.keys(fieldErrors).length > 0 && (
        <div className="overflow-hidden rounded-md border border-amber-500/20">
          <table className="w-full text-left text-xs">
            <thead className="bg-amber-500/10 text-[10px] uppercase tracking-wide text-amber-300/70">
              <tr>
                <th className="px-3 py-1.5 font-medium">Field</th>
                <th className="px-3 py-1.5 font-medium">What is wrong</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(fieldErrors).map(([field, messages]) => (
                <tr key={field} className="border-t border-amber-500/10">
                  <td className="px-3 py-1.5 font-mono text-[11px] text-amber-200">{field}</td>
                  <td className="px-3 py-1.5 text-amber-50">
                    {(Array.isArray(messages) ? messages : [messages]).join(', ')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!body && result.bodyText && (
        <div>
          <p className="mb-1 text-[11px] text-amber-200/70">
            The body was not JSON{result.jsonParseError ? ` — ${result.jsonParseError}` : ''}. Here it
            is as it came:
          </p>
          <pre className="max-h-40 overflow-auto rounded bg-slate-900/70 p-2 font-mono text-[11px] text-slate-300">
            {result.bodyText.slice(0, 4000)}
          </pre>
        </div>
      )}
    </div>
  );
}

/** A plain key/value list, used for both sets of headers. */
function HeaderTable({ headers, emptyMessage }) {
  const entries = Object.entries(headers || {});
  if (entries.length === 0) {
    return (
      <p className="rounded-md border border-dashed border-slate-700 px-3 py-4 text-center text-xs text-slate-500">
        {emptyMessage}
      </p>
    );
  }
  return (
    <div className="overflow-hidden rounded-lg border border-slate-700/70">
      <table className="w-full text-left text-xs">
        <tbody>
          {entries.map(([key, value]) => (
            <tr key={key} className="border-t border-slate-700/50 first:border-t-0">
              <td className="w-56 px-3 py-1.5 align-top font-mono text-[11px] text-sky-300">{key}</td>
              <td className="break-all px-3 py-1.5 font-mono text-[11px] text-slate-300">
                {/^authorization$/i.test(key) || /api-?key/i.test(key) ? maskSecret(value) : value}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Everything about the request that was sent, in the order it goes on the wire. */
function RequestDetails({ result }) {
  const { request } = result;
  let parsedBody;
  try {
    parsedBody = request.body ? JSON.parse(request.body) : undefined;
  } catch {
    parsedBody = undefined;
  }

  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-lg border border-slate-700/70">
        <table className="w-full text-left text-xs">
          <tbody>
            <tr className="border-b border-slate-700/50">
              <td className="w-40 bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">Method</td>
              <td className="px-3 py-1.5 font-mono text-slate-200">{request.method}</td>
            </tr>
            <tr className="border-b border-slate-700/50">
              <td className="bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">URL</td>
              <td className="break-all px-3 py-1.5 font-mono text-[11px] text-slate-200">{request.url}</td>
            </tr>
            {Object.keys(request.pathParams || {}).length > 0 && (
              <tr className="border-b border-slate-700/50">
                <td className="bg-slate-800/40 px-3 py-1.5 align-top text-[11px] text-slate-500">
                  Path parameters
                </td>
                <td className="px-3 py-1.5 font-mono text-[11px] text-slate-200">
                  {Object.entries(request.pathParams).map(([key, value]) => (
                    <div key={key}>{`{${key}} → ${value}`}</div>
                  ))}
                </td>
              </tr>
            )}
            <tr className="border-b border-slate-700/50">
              <td className="bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">Auth</td>
              <td className="px-3 py-1.5 font-mono text-[11px] text-slate-200">{request.authType}</td>
            </tr>
            <tr className="border-b border-slate-700/50">
              <td className="bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">Started</td>
              <td className="px-3 py-1.5 font-mono text-[11px] text-slate-200">
                {new Date(result.startedAtIso).toLocaleString()} ({formatClock(result.startedAtIso)})
              </td>
            </tr>
            <tr className="border-b border-slate-700/50">
              <td className="bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">Finished</td>
              <td className="px-3 py-1.5 font-mono text-[11px] text-slate-200">
                {new Date(result.finishedAtIso).toLocaleString()} ({formatClock(result.finishedAtIso)})
              </td>
            </tr>
            <tr>
              <td className="bg-slate-800/40 px-3 py-1.5 text-[11px] text-slate-500">Took</td>
              <td className="px-3 py-1.5 font-mono text-[11px] text-slate-200">
                {formatDuration(result.durationMs)} (would have given up after{' '}
                {formatDuration(result.timeoutMs)})
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div>
        <h4 className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-500">Request headers</h4>
        <HeaderTable headers={request.headers} emptyMessage="No headers were sent." />
      </div>

      <div>
        <div className="mb-2 flex items-center gap-2">
          <h4 className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Request body</h4>
          {request.body && <CopyButton text={request.body} label="Copy" />}
        </div>
        <JsonViewer
          value={parsedBody}
          rawText={request.body || ''}
          emptyMessage="No body was sent."
          toolbar={false}
          maxHeight="18rem"
        />
      </div>
    </div>
  );
}

export default function ResponsePanel({ result, sending, method, onClear, onResend }) {
  const isRead = method === 'GET';
  const [tab, setTab] = useState(isRead ? 'data' : 'body');

  // When a new result lands, go back to the tab that suits the method.
  useEffect(() => {
    if (result) setTab(isRead ? 'data' : 'body');
  }, [result, isRead]);

  const tabs = useMemo(
    () => [
      { id: 'data', label: isRead ? 'Data' : 'Overview' },
      { id: 'body', label: 'Body' },
      { id: 'headers', label: 'Response headers', count: Object.keys(result?.headers || {}).length },
      { id: 'request', label: 'Request details' },
    ],
    [result, isRead],
  );

  if (sending && !result) {
    return (
      <div className="flex flex-1 items-center justify-center border-t border-slate-800">
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <Loader2 size={16} className="animate-spin text-sky-400" />
          Sending…
        </div>
      </div>
    );
  }

  if (!result) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-2 border-t border-slate-800 px-6 text-center">
        <p className="text-sm text-slate-400">No response yet.</p>
        <p className="max-w-md text-xs text-slate-600">
          Press Send, or Ctrl/Cmd + Enter. The backend needs to be running —{' '}
          <span className="font-mono">cd backend &amp;&amp; ./mvnw spring-boot:run</span> starts it on
          port 3456.
        </p>
      </div>
    );
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col border-t border-slate-800">
      {sending && (
        <div className="flex items-center gap-2 bg-sky-500/10 px-4 py-1.5 text-[11px] text-sky-200">
          <Loader2 size={12} className="animate-spin" />
          Sending again…
        </div>
      )}
      <StatusStrip result={result} />

      <div className="flex items-center justify-between border-b border-slate-800 px-4">
        <TabBar tabs={tabs} active={tab} onChange={setTab} className="border-b-0" />
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={onResend}
            className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] text-slate-400 hover:bg-slate-700/60 hover:text-slate-100"
            title="Send the same request again"
          >
            <RotateCw size={12} /> Resend
          </button>
          <button
            type="button"
            onClick={onClear}
            className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] text-slate-400 hover:bg-slate-700/60 hover:text-slate-100"
            title="Clear the response"
          >
            <Eraser size={12} /> Clear
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {tab === 'data' && (
          <div className="space-y-4">
            <ErrorSummary result={result} />
            {!result.error && result.bodyJson != null && <DataView value={result.bodyJson} />}
            {!result.error && result.bodyJson == null && result.bodyText === '' && (
              <p className="rounded-lg border border-dashed border-slate-700 px-3 py-6 text-center text-xs text-slate-500">
                The response had no body. That is normal for a {result.status}.
              </p>
            )}
            {!result.error && result.bodyJson == null && result.bodyText !== '' && (
              <div>
                <p className="mb-2 text-[11px] text-slate-500">
                  The body was not JSON, so there is nothing to lay out. Here it is as it came.
                </p>
                <JsonViewer rawText={result.bodyText} toolbar={false} />
              </div>
            )}
          </div>
        )}

        {tab === 'body' && (
          <div className="space-y-3">
            {!result.ok && <ErrorSummary result={result} />}
            {!result.error && (
              <JsonViewer
                value={result.bodyJson ?? undefined}
                rawText={result.bodyText}
                emptyMessage={`The response had no body. That is normal for a ${result.status}.`}
                maxHeight="34rem"
              />
            )}
          </div>
        )}

        {tab === 'headers' && (
          <div className="space-y-2">
            <HeaderTable
              headers={result.headers}
              emptyMessage={
                result.error
                  ? 'Nothing came back, so there are no headers.'
                  : 'No headers were readable. On the direct environment the browser hides most of them unless the backend allows them through.'
              }
            />
            {result.headers?.location && (
              <p className="text-[11px] text-slate-500">
                The Location header points at the thing that was just created.
              </p>
            )}
          </div>
        )}

        {tab === 'request' && <RequestDetails result={result} />}
      </div>
    </div>
  );
}
