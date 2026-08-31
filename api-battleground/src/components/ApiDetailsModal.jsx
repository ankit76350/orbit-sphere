/**
 * What an action did.
 *
 * It opens on its own after anything that changes something, and leads with the answer in
 * plain terms — what changed, and the fields that came back, laid out and labelled. The wire
 * detail is a tab away for whoever wants it: the address, both sets of headers, both bodies as
 * JSON, and the timing.
 */

import { useState } from 'react';
import { CheckCircle2, AlertTriangle, WifiOff, Clock, HardDrive, Calendar } from 'lucide-react';
import { Modal, Badge, Button } from './ui.jsx';
import ResponseSummary from './ResponseSummary.jsx';
import JsonViewer, { CopyButton } from './JsonViewer.jsx';
import Markdown from './Markdown.jsx';
import {
  formatBytes,
  formatClock,
  formatDuration,
  statusText,
  STATUS_MEANING,
} from '../lib/format.js';

const METHOD_LOOK = {
  GET: 'green',
  POST: 'amber',
  PUT: 'blue',
  PATCH: 'violet',
  DELETE: 'red',
};

function Headers({ headers, empty }) {
  const rows = Object.entries(headers || {});
  if (rows.length === 0) return <p className="px-3 py-3 text-xs text-slate-500">{empty}</p>;
  return (
    <div className="overflow-hidden rounded-lg border border-slate-200">
      <table className="w-full text-left text-xs">
        <tbody>
          {rows.map(([key, value]) => (
            <tr key={key} className="border-t border-slate-100 first:border-t-0">
              <td className="w-52 bg-slate-50 px-3 py-1.5 align-top font-mono text-[11px] text-slate-600">{key}</td>
              <td className="break-all px-3 py-1.5 font-mono text-[11px] text-slate-800">{value}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function ApiDetailsModal({ entry, onClose }) {
  const [tab, setTab] = useState('summary');
  if (!entry) return null;

  const { result, endpoint } = entry;
  const failed = !result.ok;

  let sentBody;
  try {
    sentBody = result.request.body ? JSON.parse(result.request.body) : undefined;
  } catch {
    sentBody = undefined;
  }

  const tabs = [
    { id: 'summary', label: 'What happened' },
    { id: 'sent', label: 'What we sent' },
    { id: 'received', label: 'The raw answer' },
    { id: 'about', label: 'About this call' },
  ];

  return (
    <Modal
      open
      onClose={onClose}
      width="max-w-3xl"
      title={entry.action}
      description={
        result.error
          ? 'Nothing was sent, or nothing came back.'
          : failed
            ? 'The backend refused this.'
            : 'Done. Here is what changed.'
      }
      footer={
        <>
          <CopyButton
            text={result.bodyJson ? JSON.stringify(result.bodyJson, null, 2) : result.bodyText}
            label="Copy the answer"
          />
          <Button look="primary" onClick={onClose}>
            Close
          </Button>
        </>
      }
    >
      {/* The headline: did it work, how long did it take */}
      <div className="mb-4 flex flex-wrap items-center gap-x-5 gap-y-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
        <span className="flex items-center gap-2">
          {result.error ? (
            <WifiOff size={16} className="text-red-600" />
          ) : failed ? (
            <AlertTriangle size={16} className="text-amber-600" />
          ) : (
            <CheckCircle2 size={16} className="text-emerald-600" />
          )}
          <span className="text-sm font-semibold text-slate-900">
            {result.error
              ? result.error.title
              : `${result.status} ${statusText(result.status, result.statusText)}`}
          </span>
        </span>
        <span className="flex items-center gap-1.5 text-xs text-slate-600">
          <Clock size={13} className="text-slate-400" />
          {formatDuration(result.durationMs)}
        </span>
        <span className="flex items-center gap-1.5 text-xs text-slate-600">
          <HardDrive size={13} className="text-slate-400" />
          {formatBytes(result.sizeBytes)}
        </span>
        <span className="flex items-center gap-1.5 text-xs text-slate-600">
          <Calendar size={13} className="text-slate-400" />
          {formatClock(result.startedAtIso)} → {formatClock(result.finishedAtIso)}
        </span>
      </div>

      <div className="mb-4 flex gap-1 border-b border-slate-200">
        {tabs.map((one) => (
          <button
            key={one.id}
            type="button"
            onClick={() => setTab(one.id)}
            className={`-mb-px border-b-2 px-3 py-2 text-xs font-medium transition ${
              tab === one.id
                ? 'border-blue-600 text-blue-700'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            {one.label}
          </button>
        ))}
      </div>

      {tab === 'summary' && (
        <div className="space-y-4">
          {result.error ? (
            <div className="space-y-1.5 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
              <p className="text-sm font-medium text-red-800">{result.error.title}</p>
              <p className="text-xs text-red-700">{result.error.message}</p>
              {result.error.hint && <p className="text-xs text-red-600/80">{result.error.hint}</p>}
            </div>
          ) : failed ? (
            <div className="space-y-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-medium text-amber-900">
                  {result.status} {statusText(result.status, result.statusText)}
                </span>
                {result.bodyJson?.code && (
                  <Badge look="amber">{result.bodyJson.code}</Badge>
                )}
              </div>
              {result.bodyJson?.message && (
                <p className="text-xs text-amber-900">{result.bodyJson.message}</p>
              )}
              {STATUS_MEANING[result.status] && (
                <p className="text-xs text-amber-800/70">{STATUS_MEANING[result.status]}</p>
              )}
              {result.bodyJson?.fieldErrors && (
                <div className="overflow-hidden rounded-lg border border-amber-200 bg-white">
                  <table className="w-full text-left text-xs">
                    <tbody>
                      {Object.entries(result.bodyJson.fieldErrors).map(([field, messages]) => (
                        <tr key={field} className="border-t border-amber-100 first:border-t-0">
                          <td className="w-44 px-3 py-1.5 font-medium text-slate-700">{field}</td>
                          <td className="px-3 py-1.5 text-slate-600">
                            {(Array.isArray(messages) ? messages : [messages]).join(', ')}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          ) : (
            (result.bodyJson?.changeSummary || result.bodyJson?.nextStep) && (
              <div className="space-y-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3">
                {result.bodyJson.changeSummary && (
                  <p className="text-sm font-medium text-emerald-900">{result.bodyJson.changeSummary}</p>
                )}
                {result.bodyJson.nextStep && (
                  <p className="text-xs text-emerald-800">{result.bodyJson.nextStep}</p>
                )}
              </div>
            )
          )}

          {result.bodyJson != null && (
            <div>
              <p className="mb-2 text-xs font-medium text-slate-700">What came back</p>
              <ResponseSummary value={result.bodyJson} />
            </div>
          )}
        </div>
      )}

      {tab === 'summary' && (
        <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3">
          <Badge look={METHOD_LOOK[entry.method] || 'grey'}>{entry.method}</Badge>
          <code className="min-w-0 flex-1 truncate rounded-lg bg-slate-50 px-2.5 py-1.5 font-mono text-[11px] text-slate-500">
            {result.request.url}
          </code>
          <CopyButton text={result.request.url} label="Copy" />
        </div>
      )}

      {tab === 'sent' && (
        <div className="space-y-4">
          <div>
            <p className="mb-1.5 text-xs font-medium text-slate-700">Headers</p>
            <Headers headers={result.request.headers} empty="No headers were sent." />
          </div>
          <div>
            <p className="mb-1.5 text-xs font-medium text-slate-700">Body</p>
            <JsonViewer
              value={sentBody}
              rawText={result.request.body || ''}
              emptyMessage="This action sends no body."
              toolbar={false}
              maxHeight="20rem"
            />
          </div>
        </div>
      )}

      {tab === 'received' && (
        <div className="space-y-4">
          <div>
            <p className="mb-1.5 text-xs font-medium text-slate-700">Headers</p>
            <Headers
              headers={result.headers}
              empty={result.error ? 'Nothing came back, so there are no headers.' : 'No headers.'}
            />
          </div>
          <div>
            <p className="mb-1.5 text-xs font-medium text-slate-700">Body</p>
            <JsonViewer
              value={result.bodyJson ?? undefined}
              rawText={result.bodyText}
              emptyMessage="Nothing came back in the body."
              maxHeight="24rem"
            />
          </div>
        </div>
      )}

      {tab === 'about' && (
        <div className="space-y-3">
          {endpoint?.docs ? (
            <Markdown text={endpoint.docs} className="[&_p]:text-slate-600 [&_h4]:text-slate-900" />
          ) : (
            <p className="text-xs text-slate-500">No notes for this call.</p>
          )}
        </div>
      )}
    </Modal>
  );
}
