/**
 * Draws whatever the backend sent back as something a person can read — labelled fields,
 * counts, chips — instead of leaving them to read JSON.
 *
 * It works out what to draw from the shape of the answer, not from a list of known endpoints,
 * so a response that gains a field keeps rendering.
 */

import { Check, X } from 'lucide-react';
import { Badge, StatusBadge, Detail } from './ui.jsx';

/** Turns schoolId into "School id", closedDayCount into "Closed day count". */
function humanise(key) {
  const spaced = String(key).replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').trim();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}

/**
 * The values that come back as SCREAMING_SNAKE — role keys, holiday kinds — are identifiers,
 * and reading "SCHOOL_ADMIN" is the sort of thing this app is meant to stop doing.
 */
function readable(value) {
  const text = String(value);
  return /^[A-Z][A-Z0-9_]*$/.test(text) ? humanise(text) : text;
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?)?$/;

/** The keys worth calling out at the top rather than listing with everything else. */
const SENTENCES = ['changeSummary', 'nextStep', 'subscriptionNote'];

function Value({ name, value }) {
  if (value === null || value === undefined || value === '') {
    return <span className="text-slate-400">Not set</span>;
  }
  if (typeof value === 'boolean') {
    return (
      <Badge look={value ? 'green' : 'grey'}>
        {value ? <Check size={11} /> : <X size={11} />}
        {value ? 'Yes' : 'No'}
      </Badge>
    );
  }
  if (/status$/i.test(name) && typeof value === 'string' && /^[A-Z_]+$/.test(value)) {
    return <StatusBadge status={value} />;
  }
  if (typeof value === 'number') return <span className="tabular-nums">{value.toLocaleString()}</span>;
  if (typeof value === 'string') {
    if (ISO_DATE.test(value)) {
      const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
      if (!Number.isNaN(date.getTime())) {
        return (
          <span title={value}>
            {value.length === 10 ? date.toLocaleDateString() : date.toLocaleString()}
          </span>
        );
      }
    }
    if (/(id|no|key|reference|subdomain|code)$/i.test(name) && value.length < 60) {
      return <span className="font-mono text-xs">{value}</span>;
    }
    return <span>{value}</span>;
  }
  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="text-slate-400">None</span>;
    if (value.every((one) => one === null || typeof one !== 'object')) {
      return (
        <span className="flex flex-wrap gap-1">
          {value.map((one, index) => (
            <Badge key={index} title={String(one)}>
              {readable(one)}
            </Badge>
          ))}
        </span>
      );
    }
    return null;
  }
  return null;
}

/** A list of records — the school list, or the days of the calendar. */
function Rows({ records, title }) {
  const columns = [];
  records.forEach((record) => {
    Object.keys(record || {}).forEach((key) => {
      if (!columns.includes(key)) columns.push(key);
    });
  });
  return (
    <div>
      <p className="mb-1.5 text-xs font-medium text-slate-700">
        {title} <span className="font-normal text-slate-400">· {records.length}</span>
      </p>
      <div className="max-h-64 overflow-auto rounded-lg border border-slate-200">
        <table className="w-full text-left text-xs">
          <thead className="sticky top-0 bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500">
            <tr>
              {columns.map((column) => (
                <th key={column} className="whitespace-nowrap px-3 py-2 font-medium">
                  {humanise(column)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {records.map((record, index) => (
              <tr key={index} className="border-t border-slate-100">
                {columns.map((column) => {
                  const cell = record?.[column];
                  const drawn = Value({ name: column, value: cell });
                  return (
                    <td key={column} className="px-3 py-2 align-top text-slate-700">
                      {drawn ?? <Nested value={cell} />}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/** A list of small records inside a cell — a closed day's reasons, for instance. */
function Nested({ value }) {
  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="text-slate-400">None</span>;
    return (
      <span className="flex flex-wrap gap-1">
        {value.map((one, index) => (
          <Badge key={index} look="blue" title={one?.description || undefined}>
            {one?.name ?? readable(one?.type) ?? `${Object.keys(one || {}).length} fields`}
          </Badge>
        ))}
      </span>
    );
  }
  if (value && typeof value === 'object') {
    return (
      <span className="flex flex-wrap gap-1">
        {Object.entries(value).map(([key, inner]) => (
          // One string, not two pieces either side of a separator, so it stays one text node.
          <Badge key={key}>{`${humanise(key)} · ${inner}`}</Badge>
        ))}
      </span>
    );
  }
  return <span className="text-slate-400">Not set</span>;
}

export default function ResponseSummary({ value }) {
  if (value === null || value === undefined) return null;
  if (typeof value !== 'object') {
    return <p className="text-sm text-slate-800">{String(value)}</p>;
  }

  if (Array.isArray(value)) {
    return value.length ? <Rows records={value} title="Result" /> : null;
  }

  // A page of results, the shape the school list comes back in.
  const listKey = ['content', 'items', 'data', 'results'].find((key) => Array.isArray(value[key]));

  const entries = Object.entries(value).filter(
    ([key]) => !SENTENCES.includes(key) && key !== listKey,
  );
  const plain = entries.filter(([, one]) => Value({ name: 'x', value: one }) !== null);
  const complex = entries.filter(([, one]) => Value({ name: 'x', value: one }) === null);

  return (
    <div className="space-y-4">
      {plain.length > 0 && (
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3 sm:grid-cols-3">
          {plain.map(([key, one]) => (
            <Detail key={key} label={humanise(key)}>
              <Value name={key} value={one} />
            </Detail>
          ))}
        </dl>
      )}

      {listKey && value[listKey].length > 0 && (
        <Rows records={value[listKey]} title={humanise(listKey)} />
      )}

      {complex.map(([key, one]) =>
        Array.isArray(one) ? (
          <Rows key={key} records={one} title={humanise(key)} />
        ) : (
          <div key={key}>
            <p className="mb-1.5 text-xs font-medium text-slate-700">{humanise(key)}</p>
            <Nested value={one} />
          </div>
        ),
      )}
    </div>
  );
}
