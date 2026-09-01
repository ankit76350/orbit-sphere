/** The list of every call this session made, newest first. Click one to reopen its details. */

import { Activity as ActivityIcon, Trash2 } from 'lucide-react';
import { Modal, Button, Badge, EmptyState } from './ui.jsx';
import { METHOD_LOOK } from './EndpointTag.jsx';
import { formatDuration, formatDateTime, timeAgo } from '../lib/format.js';

/** Everything this session has sent, for when somebody wants to look back. */
export function ActivityModal({ open, onClose, log, onInspect, onClear }) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      width="max-w-2xl"
      title="Activity"
      description="Every call this app has made since the page loaded"
      footer={
        <>
          <Button icon={Trash2} onClick={onClear} disabled={log.length === 0}>
            Clear
          </Button>
          <Button look="primary" onClick={onClose}>
            Close
          </Button>
        </>
      }
    >
      {log.length === 0 ? (
        <EmptyState
          icon={ActivityIcon}
          title="Nothing yet"
          description="Every action you take is recorded here, so you can look back at exactly what was sent and what came back."
        />
      ) : (
        <ul className="-my-1 divide-y divide-slate-100">
          {log.map((entry) => (
            <li key={entry.id}>
              <button
                type="button"
                onClick={() => onInspect(entry)}
                className="flex w-full items-center gap-3 rounded-lg px-2 py-2.5 text-left transition hover:bg-slate-50"
              >
                <Badge look={METHOD_LOOK[entry.method] || 'grey'} className="w-16 justify-center">
                  {entry.method}
                </Badge>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-slate-800">{entry.action}</span>
                  <span className="block truncate font-mono text-[11px] text-slate-500">{entry.path}</span>
                </span>
                <span className="shrink-0 text-right">
                  <span
                    className={`block text-xs font-semibold ${
                      entry.ok ? 'text-emerald-700' : 'text-red-600'
                    }`}
                  >
                    {entry.status ?? 'failed'}
                  </span>
                  <span className="block text-[11px] text-slate-400" title={formatDateTime(entry.at)}>
                    {formatDuration(entry.durationMs)} · {timeAgo(entry.at)}
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
