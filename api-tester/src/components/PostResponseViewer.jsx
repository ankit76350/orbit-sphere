import { useEffect, useMemo, useState } from 'react';
import { Check, Clipboard, Server, X } from 'lucide-react';
import { POST_RESPONSE_EVENT } from '../api.js';

function responseBodyText(data) {
  if (data === '' || data === undefined) return '';
  if (typeof data === 'string') return data;
  return JSON.stringify(data, null, 2);
}

export default function PostResponseViewer() {
  const [response, setResponse] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const showResponse = (event) => {
      setResponse(event.detail);
      setCopied(false);
    };
    window.addEventListener(POST_RESPONSE_EVENT, showResponse);
    return () => window.removeEventListener(POST_RESPONSE_EVENT, showResponse);
  }, []);

  const bodyText = useMemo(() => responseBodyText(response?.data), [response]);

  if (!response) return null;

  const statusLabel = response.status
    ? `${response.status}${response.statusText ? ` ${response.statusText}` : ''}`
    : response.statusText;

  const copyResponse = async () => {
    await navigator.clipboard.writeText(bodyText || '(empty response body)');
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div
      className="fixed inset-0 z-[80] bg-slate-950/50 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={() => setResponse(null)}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="post-response-title"
        className="bg-white border border-slate-200 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="px-5 py-4 border-b border-slate-100 bg-slate-50 flex items-start justify-between gap-4">
          <div>
            <h2 id="post-response-title" className="font-bold text-slate-900 text-sm flex items-center gap-2">
              <Server size={17} className={response.ok ? 'text-emerald-600' : 'text-rose-600'} />
              Backend POST response
            </h2>
            <p className="text-[11px] text-slate-500 mt-1">Complete response returned by the backend.</p>
          </div>
          <button
            type="button"
            onClick={() => setResponse(null)}
            className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
            title="Close response"
          >
            <X size={17} />
          </button>
        </header>

        <div className="px-5 py-4 border-b border-slate-100 grid grid-cols-1 sm:grid-cols-[auto_1fr] gap-3 text-xs">
          <div>
            <div className="text-[9px] font-bold uppercase tracking-wider text-slate-400 mb-1">Status</div>
            <span className={`inline-flex px-2.5 py-1 rounded-full font-bold ${
              response.ok
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-rose-100 text-rose-700'
            }`}>
              {statusLabel || 'Unknown status'}
            </span>
          </div>
          <div className="min-w-0">
            <div className="text-[9px] font-bold uppercase tracking-wider text-slate-400 mb-1">Endpoint</div>
            <div className="font-mono text-slate-700 bg-slate-100 rounded-lg px-3 py-1.5 break-all select-all">
              {response.method} {response.path}
            </div>
          </div>
        </div>

        <div className="p-5 min-h-0 flex-1 flex flex-col">
          <div className="flex items-center justify-between gap-3 mb-2">
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">Response body</h3>
              <p className="text-[10px] text-slate-400 mt-0.5">
                Received {new Date(response.receivedAt).toLocaleString()}
              </p>
            </div>
            <button
              type="button"
              onClick={copyResponse}
              className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-slate-600 transition"
            >
              {copied ? <Check size={13} className="text-emerald-600" /> : <Clipboard size={13} />}
              {copied ? 'Copied' : 'Copy response'}
            </button>
          </div>
          <pre className="flex-1 min-h-40 max-h-[55vh] overflow-auto rounded-xl bg-slate-950 text-slate-100 p-4 text-[11px] leading-relaxed font-mono whitespace-pre-wrap break-words select-all">
            {bodyText || '(empty response body)'}
          </pre>
        </div>

        <footer className="px-5 py-3 border-t border-slate-100 flex justify-end">
          <button
            type="button"
            onClick={() => setResponse(null)}
            className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-slate-700 transition"
          >
            Close
          </button>
        </footer>
      </section>
    </div>
  );
}
