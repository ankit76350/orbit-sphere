/**
 * A JSON viewer with colours, collapsing and search. Written by hand rather than pulled in as
 * a package, because all it has to do is walk a value and print it.
 */

import { useMemo, useState } from 'react';
import { ChevronRight, Copy, Check, Search, Maximize2, Minimize2 } from 'lucide-react';

/** One colour per value type, shared by the tree view and the raw view. */
const TOKEN_CLASS = {
  key: 'text-sky-300',
  string: 'text-emerald-300',
  number: 'text-amber-300',
  boolean: 'text-fuchsia-300',
  null: 'text-slate-500 italic',
  punctuation: 'text-slate-500',
  count: 'text-slate-500',
};

function typeOf(value) {
  if (value === null) return 'null';
  if (Array.isArray(value)) return 'array';
  return typeof value;
}

/** Wraps whatever matches the search text in a highlight. */
function Highlighted({ text, search }) {
  const full = String(text);
  if (!search) return <>{full}</>;
  const lower = full.toLowerCase();
  const needle = search.toLowerCase();
  if (!lower.includes(needle)) return <>{full}</>;

  const pieces = [];
  let from = 0;
  let at = lower.indexOf(needle, from);
  let index = 0;
  while (at !== -1) {
    if (at > from) pieces.push(<span key={index++}>{full.slice(from, at)}</span>);
    pieces.push(
      <mark key={index++} className="rounded-sm bg-amber-400/30 text-amber-200">
        {full.slice(at, at + needle.length)}
      </mark>,
    );
    from = at + needle.length;
    at = lower.indexOf(needle, from);
  }
  if (from < full.length) pieces.push(<span key={index++}>{full.slice(from)}</span>);
  return <>{pieces}</>;
}

/** True when this value, or anything inside it, matches the search. */
function matches(value, search) {
  if (!search) return false;
  const needle = search.toLowerCase();
  const walk = (current) => {
    if (current == null) return 'null'.includes(needle);
    if (typeof current === 'object') {
      return Object.entries(current).some(([key, child]) => key.toLowerCase().includes(needle) || walk(child));
    }
    return String(current).toLowerCase().includes(needle);
  };
  return walk(value);
}

function Leaf({ value, search }) {
  const kind = typeOf(value);
  if (kind === 'string') {
    return (
      <span className={TOKEN_CLASS.string}>
        "<Highlighted text={value} search={search} />"
      </span>
    );
  }
  if (kind === 'null') return <span className={TOKEN_CLASS.null}>null</span>;
  if (kind === 'boolean') {
    return (
      <span className={TOKEN_CLASS.boolean}>
        <Highlighted text={String(value)} search={search} />
      </span>
    );
  }
  return (
    <span className={TOKEN_CLASS.number}>
      <Highlighted text={String(value)} search={search} />
    </span>
  );
}

function Node({ name, value, depth, defaultDepth, search, isLast, forceOpen }) {
  const kind = typeOf(value);
  const isBranch = kind === 'object' || kind === 'array';
  const hasMatch = search ? matches(value, search) : false;
  // Open a few levels by default, and always open a branch the search hit.
  const [open, setOpen] = useState(depth < defaultDepth);
  const isOpen = forceOpen || hasMatch || open;

  const label =
    name == null ? null : (
      <>
        <span className={TOKEN_CLASS.key}>
          "<Highlighted text={name} search={search} />"
        </span>
        <span className={TOKEN_CLASS.punctuation}>: </span>
      </>
    );

  if (!isBranch) {
    return (
      <div className="whitespace-pre-wrap break-words leading-6" style={{ paddingLeft: depth * 14 }}>
        <span className="inline-block w-4" />
        {label}
        <Leaf value={value} search={search} />
        {!isLast && <span className={TOKEN_CLASS.punctuation}>,</span>}
      </div>
    );
  }

  const entries = kind === 'array' ? value.map((item, i) => [String(i), item]) : Object.entries(value);
  const openBrace = kind === 'array' ? '[' : '{';
  const closeBrace = kind === 'array' ? ']' : '}';

  return (
    <div>
      <div className="leading-6" style={{ paddingLeft: depth * 14 }}>
        <button
          type="button"
          onClick={() => setOpen((was) => !was)}
          className="inline-flex w-4 items-center justify-center align-middle text-slate-500 hover:text-slate-200"
          aria-label={isOpen ? 'Collapse' : 'Expand'}
        >
          <ChevronRight size={12} className={`transition-transform ${isOpen ? 'rotate-90' : ''}`} />
        </button>
        {label}
        <span className={TOKEN_CLASS.punctuation}>{openBrace}</span>
        {!isOpen && (
          <>
            <span className={TOKEN_CLASS.count}>
              {' '}
              {entries.length} {kind === 'array' ? 'item' : 'field'}
              {entries.length === 1 ? '' : 's'}{' '}
            </span>
            <span className={TOKEN_CLASS.punctuation}>{closeBrace}</span>
            {!isLast && <span className={TOKEN_CLASS.punctuation}>,</span>}
          </>
        )}
      </div>
      {isOpen && (
        <>
          {entries.map(([key, child], index) => (
            <Node
              key={key}
              name={kind === 'array' ? null : key}
              value={child}
              depth={depth + 1}
              defaultDepth={defaultDepth}
              search={search}
              isLast={index === entries.length - 1}
              forceOpen={forceOpen}
            />
          ))}
          <div className="leading-6" style={{ paddingLeft: depth * 14 }}>
            <span className="inline-block w-4" />
            <span className={TOKEN_CLASS.punctuation}>{closeBrace}</span>
            {!isLast && <span className={TOKEN_CLASS.punctuation}>,</span>}
          </div>
        </>
      )}
    </div>
  );
}

/** Small toolbar button used by the viewer and the panels. */
export function ToolButton({ onClick, title, children, active }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={title}
      className={`inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-[11px] font-medium transition ${
        active ? 'bg-sky-500/20 text-sky-200' : 'text-slate-400 hover:bg-slate-700/60 hover:text-slate-100'
      }`}
    >
      {children}
    </button>
  );
}

/** Copies text and shows a tick for a moment, so you know it worked. */
export function CopyButton({ text, label = 'Copy', title }) {
  const [copied, setCopied] = useState(false);
  return (
    <ToolButton
      title={title || label}
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(text ?? '');
          setCopied(true);
          setTimeout(() => setCopied(false), 1200);
        } catch {
          setCopied(false);
        }
      }}
    >
      {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
      {copied ? 'Copied' : label}
    </ToolButton>
  );
}

/**
 * The viewer itself. Give it either a parsed value or raw text; it shows a tree when the value
 * is JSON and falls back to plain text when it is not.
 */
export default function JsonViewer({
  value,
  rawText,
  emptyMessage = 'No body.',
  defaultDepth = 3,
  toolbar = true,
  maxHeight = '28rem',
}) {
  const [search, setSearch] = useState('');
  const [showRaw, setShowRaw] = useState(false);
  const [expandAll, setExpandAll] = useState(false);

  const pretty = useMemo(() => {
    if (value !== undefined && value !== null) {
      try {
        return JSON.stringify(value, null, 2);
      } catch {
        return String(rawText ?? '');
      }
    }
    return rawText ?? '';
  }, [value, rawText]);

  const hasTree = value !== undefined && value !== null && typeof value === 'object';
  const isEmpty = !hasTree && (rawText == null || rawText === '') && value === undefined;

  // How many lines contain the search text, so the box can say "3 lines".
  const matchCount = useMemo(() => {
    if (!search) return 0;
    const needle = search.toLowerCase();
    return pretty.toLowerCase().split('\n').filter((line) => line.includes(needle)).length;
  }, [pretty, search]);

  return (
    <div className="rounded-lg border border-slate-700/70 bg-slate-900/70">
      {toolbar && (
        <div className="flex flex-wrap items-center gap-1 border-b border-slate-700/70 px-2 py-1.5">
          <div className="relative">
            <Search size={12} className="pointer-events-none absolute left-2 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search in response"
              className="w-44 rounded-md border border-slate-700 bg-slate-800/70 py-1 pl-7 pr-2 text-[11px] text-slate-200 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
            />
          </div>
          {search && (
            <span className="text-[11px] text-slate-500">
              {matchCount} line{matchCount === 1 ? '' : 's'}
            </span>
          )}
          <div className="ml-auto flex items-center gap-1">
            {hasTree && !showRaw && (
              <ToolButton
                title={expandAll ? 'Collapse everything' : 'Expand everything'}
                onClick={() => setExpandAll((was) => !was)}
              >
                {expandAll ? <Minimize2 size={12} /> : <Maximize2 size={12} />}
                {expandAll ? 'Collapse all' : 'Expand all'}
              </ToolButton>
            )}
            {hasTree && (
              <ToolButton
                title="Switch between the tree and the raw text"
                active={showRaw}
                onClick={() => setShowRaw((was) => !was)}
              >
                {showRaw ? 'Tree' : 'Raw'}
              </ToolButton>
            )}
            <CopyButton text={pretty} label="Copy JSON" />
          </div>
        </div>
      )}

      <div className="overflow-auto p-3 font-mono text-[12px]" style={{ maxHeight }}>
        {isEmpty ? (
          <p className="italic text-slate-500">{emptyMessage}</p>
        ) : hasTree && !showRaw ? (
          <Node
            name={null}
            value={value}
            depth={0}
            defaultDepth={defaultDepth}
            search={search}
            isLast
            forceOpen={expandAll}
          />
        ) : (
          <pre className="whitespace-pre-wrap break-words text-slate-300">
            <Highlighted text={pretty} search={search} />
          </pre>
        )}
      </div>
    </div>
  );
}
