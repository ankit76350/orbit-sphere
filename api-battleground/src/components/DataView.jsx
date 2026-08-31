/**
 * Shows a response the way an application would, instead of as raw JSON.
 *
 * A list of records becomes a table. A single record becomes a set of labelled fields, with
 * anything nested shown as its own small table or card. Statuses, flags and dates are
 * formatted, so the answer reads at a glance.
 *
 * It works out what to draw from the shape of the value, not from a list of known endpoints,
 * so it will keep working for the GET endpoints that are not built yet.
 */

import { Check, X, ArrowRight } from 'lucide-react';
import { Pill } from './ui.jsx';

/** Turns schoolId into "School id", numberSequencesCreated into "Number sequences created". */
function humanise(key) {
  const spaced = key.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').trim();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}

/** The statuses this system uses, coloured by what they mean. */
const STATUS_TONE = {
  ACTIVE: 'emerald',
  TRIAL: 'sky',
  PROVISIONING: 'amber',
  SUSPENDED: 'rose',
  OFFBOARDING: 'amber',
  CLOSED: 'slate',
  DELETION_PENDING: 'rose',
  DELETED: 'rose',
  NONE: 'slate',
};

const ISO_DATE = /^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?)?$/;

function looksLikeStatus(key, value) {
  return typeof value === 'string' && /status$/i.test(key) && /^[A-Z_]+$/.test(value);
}

/** Draws one value. Returns null when the value needs a table or a card of its own. */
function ValueCell({ name, value }) {
  if (value === null || value === undefined) {
    return <span className="italic text-slate-500">not set</span>;
  }
  if (typeof value === 'boolean') {
    return (
      <Pill tone={value ? 'emerald' : 'slate'}>
        {value ? <Check size={11} /> : <X size={11} />}
        {value ? 'Yes' : 'No'}
      </Pill>
    );
  }
  if (looksLikeStatus(name, value)) {
    return <Pill tone={STATUS_TONE[value] || 'sky'}>{value}</Pill>;
  }
  if (typeof value === 'number') {
    return <span className="font-mono text-slate-200">{value.toLocaleString()}</span>;
  }
  if (typeof value === 'string') {
    if (ISO_DATE.test(value)) {
      const date = new Date(value);
      if (!Number.isNaN(date.getTime())) {
        return (
          <span className="text-slate-200" title={value}>
            {date.toLocaleString()}
          </span>
        );
      }
    }
    if (/^https?:\/\//.test(value)) {
      return (
        <a href={value} target="_blank" rel="noreferrer" className="text-sky-300 underline decoration-dotted">
          {value}
        </a>
      );
    }
    // Ids and other machine values read better in a monospace font.
    if (/(id|no|key|reference|subdomain|code)$/i.test(name) && value.length < 60) {
      return <span className="font-mono text-[11px] text-slate-200">{value}</span>;
    }
    return <span className="text-slate-200">{value}</span>;
  }
  if (Array.isArray(value) && value.every((item) => item === null || typeof item !== 'object')) {
    return (
      <span className="flex flex-wrap gap-1">
        {value.length === 0 ? (
          <span className="italic text-slate-500">empty</span>
        ) : (
          value.map((item, index) => (
            <Pill key={index} tone="slate">
              {String(item)}
            </Pill>
          ))
        )}
      </span>
    );
  }
  return null;
}

/** Every column across all the records, so a row missing a field still lines up. */
function columnsOf(records) {
  const seen = [];
  records.forEach((record) => {
    Object.keys(record || {}).forEach((key) => {
      if (!seen.includes(key)) seen.push(key);
    });
  });
  return seen;
}

function RecordTable({ records, title }) {
  const columns = columnsOf(records);
  return (
    <div className="space-y-2">
      {title && (
        <div className="flex items-baseline gap-2">
          <h4 className="text-xs font-semibold text-slate-300">{title}</h4>
          <span className="text-[11px] text-slate-500">
            {records.length} row{records.length === 1 ? '' : 's'}
          </span>
        </div>
      )}
      <div className="overflow-x-auto rounded-lg border border-slate-700/70">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-800/60 text-[10px] uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-3 py-2 font-medium">#</th>
              {columns.map((column) => (
                <th key={column} className="whitespace-nowrap px-3 py-2 font-medium">
                  {humanise(column)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {records.map((record, index) => (
              <tr key={index} className="border-t border-slate-700/50 hover:bg-slate-800/40">
                <td className="px-3 py-2 font-mono text-[11px] text-slate-500">{index + 1}</td>
                {columns.map((column) => {
                  const cell = record?.[column];
                  return (
                    <td key={column} className="px-3 py-2 align-top">
                      {ValueCell({ name: column, value: cell }) ?? (
                        <span className="font-mono text-[11px] text-slate-400">
                          {Array.isArray(cell) ? `${cell.length} items` : 'nested object'}
                        </span>
                      )}
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

/** True when this value needs a section of its own rather than a cell. */
function needsOwnSection(value) {
  if (Array.isArray(value)) return value.some((item) => item && typeof item === 'object');
  return Boolean(value) && typeof value === 'object';
}

function FieldGrid({ record }) {
  // Long free text takes a full-width row; everything else sits two to a line.
  const isWide = (value) => typeof value === 'string' && value.length > 60;

  const entries = Object.entries(record).filter(([, value]) => !needsOwnSection(value));
  if (entries.length === 0) return null;

  return (
    <div className="grid grid-cols-1 gap-px overflow-hidden rounded-lg border border-slate-700/70 bg-slate-700/40 sm:grid-cols-2">
      {entries.map(([key, value]) => (
        <div key={key} className={`bg-slate-900/70 px-3 py-2.5 ${isWide(value) ? 'sm:col-span-2' : ''}`}>
          <div className="text-[10px] uppercase tracking-wide text-slate-500">{humanise(key)}</div>
          <div className="mt-1 break-words text-xs">
            <ValueCell name={key} value={value} />
          </div>
        </div>
      ))}
    </div>
  );
}

/** Anything inside a record that deserves its own table or card. */
function NestedSections({ record }) {
  const sections = Object.entries(record).filter(([, value]) => needsOwnSection(value));
  if (sections.length === 0) return null;
  return (
    <div className="space-y-4">
      {sections.map(([key, value]) =>
        Array.isArray(value) ? (
          <RecordTable key={key} records={value} title={humanise(key)} />
        ) : (
          <div key={key} className="space-y-2">
            <h4 className="text-xs font-semibold text-slate-300">{humanise(key)}</h4>
            <FieldGrid record={value} />
            <NestedSections record={value} />
          </div>
        ),
      )}
    </div>
  );
}

/**
 * The line the endpoints put in nextStep. Worth pulling out of the field list: it is the one
 * part of the response written for a person to act on.
 */
function NextStep({ text }) {
  if (!text) return null;
  return (
    <div className="flex items-start gap-2 rounded-lg border border-sky-500/30 bg-sky-500/10 px-3 py-2.5">
      <ArrowRight size={14} className="mt-0.5 shrink-0 text-sky-300" />
      <div>
        <div className="text-[10px] font-semibold uppercase tracking-wide text-sky-300">Next step</div>
        <p className="mt-0.5 text-xs text-sky-100">{text}</p>
      </div>
    </div>
  );
}

export default function DataView({ value, emptyMessage = 'Nothing came back in the body.' }) {
  if (value === null || value === undefined) {
    return (
      <p className="rounded-lg border border-dashed border-slate-700 px-3 py-6 text-center text-xs text-slate-500">
        {emptyMessage}
      </p>
    );
  }

  // A plain value, which some endpoints do return.
  if (typeof value !== 'object') {
    return (
      <div className="rounded-lg border border-slate-700/70 bg-slate-900/70 px-3 py-3 text-sm">
        <ValueCell name="value" value={value} />
      </div>
    );
  }

  // A list. This is the shape a GET collection endpoint returns, so it becomes a table.
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return (
        <p className="rounded-lg border border-dashed border-slate-700 px-3 py-6 text-center text-xs text-slate-500">
          The list came back empty.
        </p>
      );
    }
    if (value.every((item) => item && typeof item === 'object' && !Array.isArray(item))) {
      return <RecordTable records={value} />;
    }
    return (
      <ul className="space-y-1">
        {value.map((item, index) => (
          <li key={index} className="rounded border border-slate-700/70 bg-slate-900/70 px-3 py-1.5 text-xs">
            <ValueCell name="item" value={item} />
          </li>
        ))}
      </ul>
    );
  }

  // A page of results, the shape Spring Data hands back. Show the rows and the page info.
  const listKey = ['content', 'items', 'data', 'results'].find((key) => Array.isArray(value[key]));
  if (listKey) {
    const rest = { ...value };
    delete rest[listKey];
    return (
      <div className="space-y-4">
        <RecordTable records={value[listKey]} title={humanise(listKey)} />
        {Object.keys(rest).length > 0 && (
          <div className="space-y-2">
            <h4 className="text-xs font-semibold text-slate-300">Page details</h4>
            <FieldGrid record={rest} />
          </div>
        )}
      </div>
    );
  }

  // A single record.
  const { nextStep, ...rest } = value;
  return (
    <div className="space-y-4">
      <NextStep text={typeof nextStep === 'string' ? nextStep : null} />
      <FieldGrid record={rest} />
      <NestedSections record={rest} />
    </div>
  );
}
