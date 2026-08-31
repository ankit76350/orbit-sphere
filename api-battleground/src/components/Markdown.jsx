/**
 * Draws the endpoint notes that come from the Postman collection.
 *
 * Those notes are markdown — headings, tables, bold, inline code and fenced code blocks — so
 * showing them as plain text would waste them. This handles that much and nothing more; it is
 * not a general markdown library and does not need to be.
 */

import { Fragment } from 'react';

/** Handles **bold** and `code` inside a line of text. */
function inline(text, keyPrefix) {
  const parts = [];
  // One pass over both patterns, so a `code` span inside bold still comes out right.
  const pattern = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0;
  let match;
  let index = 0;
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > last) parts.push(<Fragment key={`${keyPrefix}-t${index++}`}>{text.slice(last, match.index)}</Fragment>);
    const token = match[0];
    if (token.startsWith('**')) {
      parts.push(
        <strong key={`${keyPrefix}-b${index++}`} className="font-semibold text-slate-900">
          {token.slice(2, -2)}
        </strong>,
      );
    } else {
      parts.push(
        <code key={`${keyPrefix}-c${index++}`} className="rounded bg-slate-100 px-1 py-0.5 font-mono text-[11px] text-blue-700">
          {token.slice(1, -1)}
        </code>,
      );
    }
    last = match.index + token.length;
  }
  if (last < text.length) parts.push(<Fragment key={`${keyPrefix}-t${index++}`}>{text.slice(last)}</Fragment>);
  return parts;
}

/** Splits a markdown table row into its cells. */
function cellsOf(line) {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cell.trim());
}

export default function Markdown({ text, className = '' }) {
  if (!text) return null;

  const lines = String(text).split('\n');
  const blocks = [];
  let i = 0;
  let key = 0;

  while (i < lines.length) {
    const line = lines[i];

    // A fenced code block. The test-case listings live in these, so they keep their spacing.
    if (line.trim().startsWith('```')) {
      const code = [];
      i += 1;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        code.push(lines[i]);
        i += 1;
      }
      i += 1;
      blocks.push(
        <pre
          key={key++}
          className="overflow-x-auto rounded-lg border border-slate-200 bg-slate-50 p-3 font-mono text-[11px] leading-relaxed text-slate-700"
        >
          {code.join('\n')}
        </pre>,
      );
      continue;
    }

    // A heading.
    const heading = line.match(/^(#{1,6})\s+(.*)$/);
    if (heading) {
      blocks.push(
        <h4 key={key++} className="pt-1 text-xs font-semibold text-slate-900">
          {inline(heading[2], `h${key}`)}
        </h4>,
      );
      i += 1;
      continue;
    }

    // A table: a header row, a row of dashes, then the body.
    if (line.includes('|') && i + 1 < lines.length && /^[\s|:-]+$/.test(lines[i + 1]) && lines[i + 1].includes('-')) {
      const header = cellsOf(line);
      i += 2;
      const rows = [];
      while (i < lines.length && lines[i].includes('|') && lines[i].trim() !== '') {
        rows.push(cellsOf(lines[i]));
        i += 1;
      }
      blocks.push(
        <div key={key++} className="overflow-x-auto rounded-lg border border-slate-200">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500">
              <tr>
                {header.map((cell, index) => (
                  <th key={index} className="px-3 py-1.5 font-medium">
                    {cell}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, rowIndex) => (
                <tr key={rowIndex} className="border-t border-slate-100">
                  {row.map((cell, cellIndex) => (
                    <td key={cellIndex} className="px-3 py-1.5 align-top text-slate-600">
                      {inline(cell, `r${rowIndex}c${cellIndex}`)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>,
      );
      continue;
    }

    // A bullet list.
    if (/^\s*[-*]\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^\s*[-*]\s+/, ''));
        i += 1;
      }
      blocks.push(
        <ul key={key++} className="list-disc space-y-1 pl-5 text-xs leading-relaxed text-slate-600">
          {items.map((item, index) => (
            <li key={index}>{inline(item, `li${index}`)}</li>
          ))}
        </ul>,
      );
      continue;
    }

    if (line.trim() === '') {
      i += 1;
      continue;
    }

    // Anything else is a paragraph. Wrapped lines in the source are one paragraph here.
    const paragraph = [];
    while (
      i < lines.length &&
      lines[i].trim() !== '' &&
      !lines[i].trim().startsWith('```') &&
      !/^#{1,6}\s/.test(lines[i]) &&
      !/^\s*[-*]\s+/.test(lines[i]) &&
      !(lines[i].includes('|') && i + 1 < lines.length && /^[\s|:-]+$/.test(lines[i + 1]))
    ) {
      paragraph.push(lines[i].trim());
      i += 1;
    }
    blocks.push(
      <p key={key++} className="text-xs leading-relaxed text-slate-600">
        {inline(paragraph.join(' '), `p${key}`)}
      </p>,
    );
  }

  return <div className={`space-y-3 ${className}`}>{blocks}</div>;
}
