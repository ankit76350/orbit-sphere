import { useEffect, useMemo, useState } from 'react';
import { Save, Plus, Coffee, X, GripVertical, Trash2 } from 'lucide-react';
import { api } from '../api.js';
import { Field, Input, Select, Button, Empty, useToast } from '../components/ui.jsx';
import { overlaps, today } from '../lib/date.js';

const defaultPeriods = () => [
  { start: '09:00', end: '09:45', type: 'LESSON', subject: '', teacherDocsId: null },
  { start: '09:45', end: '10:30', type: 'LESSON', subject: '', teacherDocsId: null },
  { start: '10:30', end: '10:50', type: 'BREAK', subject: 'Recess', teacherDocsId: null },
  { start: '10:50', end: '11:35', type: 'LESSON', subject: '', teacherDocsId: null },
];

export default function TimetableBuilder({ schoolId, year, yearDoc, classes, staff }) {
  const toast = useToast();
  const [cols, setCols] = useState([]);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [payloadSchoolId, setPayloadSchoolId] = useState(schoolId || '');
  const [payloadAcademicYear, setPayloadAcademicYear] = useState(year || '');
  const [busy, setBusy] = useState(false);
  const [drag, setDrag] = useState(null); // teacherDocsId being dragged

  const staffName = (id) => (staff.find((s) => s.id === id) || {}).name || id;
  const minDate = yearDoc?.startDate;
  const maxDate = yearDoc?.endDate;

  useEffect(() => setPayloadSchoolId(schoolId || ''), [schoolId]);
  useEffect(() => setPayloadAcademicYear(year || ''), [year]);

  // section chips available
  const chips = useMemo(() => {
    const list = [];
    classes.forEach((c) => (c.sections || []).forEach((s) => list.push({ classDocsId: c.id, className: c.name, section: s })));
    return list;
  }, [classes]);

  const isOn = (classDocsId, section) => cols.some((c) => c.classDocsId === classDocsId && c.section === section);
  const toggle = (chip) => setCols((cs) => {
    const i = cs.findIndex((c) => c.classDocsId === chip.classDocsId && c.section === chip.section);
    if (i >= 0) return cs.filter((_, k) => k !== i);
    return [...cs, { ...chip, periods: defaultPeriods() }];
  });

  const mutate = (ci, fn) => setCols((cs) => cs.map((c, i) => (i === ci ? fn(c) : c)));

  // ---- live conflict detection (editor-only, instant feedback) ----
  const conflicts = useMemo(() => {
    const flat = [];
    cols.forEach((col, ci) => col.periods.forEach((p, pi) => {
      if (p.start && p.end && p.start < p.end) flat.push({ ci, pi, key: col.classDocsId + '|' + col.section, p });
    }));
    const bad = new Set();
    for (let i = 0; i < flat.length; i++)
      for (let j = i + 1; j < flat.length; j++) {
        const a = flat[i], b = flat[j];
        if (!overlaps(a.p.start, a.p.end, b.p.start, b.p.end)) continue;
        const sameSection = a.key === b.key;
        const sameTeacher = a.p.teacherDocsId && a.p.teacherDocsId === b.p.teacherDocsId;
        if (sameSection || sameTeacher) { bad.add(a.ci + '.' + a.pi); bad.add(b.ci + '.' + b.pi); }
      }
    return bad;
  }, [cols]);

  async function save() {
    if (!start) return toast.error('Choose a start date.');
    if (end && end < start) return toast.error('“Runs until” cannot be before the start date.');
    const classTimetables = [];
    let skipped = 0;
    cols.forEach((col) => {
      const periods = [];
      col.periods.forEach((p) => {
        if (!p.start || !p.end) return;
        if (p.type === 'BREAK') periods.push({ type: 'BREAK', subject: (p.subject || 'Break').trim(), startTime: p.start, endTime: p.end });
        else if (p.subject.trim() && p.teacherDocsId) periods.push({ type: 'LESSON', subject: p.subject.trim(), teacherDocsId: p.teacherDocsId, startTime: p.start, endTime: p.end });
        else if (p.subject.trim() || p.teacherDocsId) skipped++;
      });
      if (periods.length) classTimetables.push({ classDocsId: col.classDocsId, section: col.section, periods });
    });
    if (!classTimetables.length) return toast.error('Add at least one complete lesson (subject + teacher) or break.');

    setBusy(true);
    try {
      const res = await api.createTimetable({ schoolId: payloadSchoolId || null, academicYear: payloadAcademicYear || null, startDate: start, endDate: end || null, classTimetables });
      let msg = `Saved! ${res.daysCreated} day(s), ${res.totalEntries} entries.`;
      if (res.skipped?.length) msg += `\nSkipped ${res.skipped.length} holiday(s): ${res.skipped.map((s) => `${s.date} (${s.reason})`).join(', ')}`;
      if (skipped) msg += `\n(${skipped} incomplete lesson(s) ignored.)`;
      toast.success(msg);
    } catch (e) { toast.error(e.message); } finally { setBusy(false); }
  }

  if (!year) return <Empty title="No academic year selected" hint="Create one under Academic Year first." />;

  return (
    <div>
      {/* date range */}
      <div className="flex flex-wrap items-end gap-3 mb-4 bg-white border border-slate-200 rounded-xl p-4">
        <Field label="School ID" apiName="schoolId" required={false}><Input value={payloadSchoolId} onChange={(e) => setPayloadSchoolId(e.target.value)} className="font-mono text-xs max-w-40" /></Field>
        <Field label="Academic Year" apiName="academicYear" required={false}><Input value={payloadAcademicYear} onChange={(e) => setPayloadAcademicYear(e.target.value)} className="max-w-32" /></Field>
        <Field label="Timetable starts from" apiName="startDate" required={false}><Input type="date" min={minDate} max={maxDate} value={start} onChange={(e) => setStart(e.target.value)} /></Field>
        <Field label="Runs until (blank = one day)" apiName="endDate" required={false}><Input type="date" min={minDate} max={maxDate} value={end} onChange={(e) => setEnd(e.target.value)} /></Field>
        <div className="text-xs text-slate-500 max-w-xs">Holidays &amp; weekly offs of <b>{year}</b> are skipped automatically — you’ll see which after saving.</div>
        <Button variant="primary" size="lg" className="ml-auto" onClick={save} disabled={busy || !cols.length}>
          <Save size={16} /> {busy ? 'Saving…' : 'Save timetable'}
        </Button>
      </div>

      {/* section chips */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 mb-4">
        <div className="text-xs font-semibold text-slate-600 mb-2">1 · Pick the class sections to build</div>
        {chips.length === 0 ? (
          <div className="text-sm text-amber-600">No classes with sections. Add them under Classes first.</div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {chips.map((c, i) => {
              const on = isOn(c.classDocsId, c.section);
              return (
                <button key={i} onClick={() => toggle(c)}
                  className={`text-xs rounded-full px-3 py-1.5 border transition ${on ? 'bg-blue-600 border-blue-600 text-white' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                  {c.className} — {c.section}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {cols.length > 0 && (
        <div className="flex gap-4 items-start">
          {/* teachers */}
          <div className="w-52 shrink-0 bg-white border border-slate-200 rounded-xl p-3 sticky top-2">
            <div className="text-xs font-semibold text-slate-600 mb-2">2 · Drag a teacher onto a lesson</div>
            <div className="space-y-1.5 max-h-[70vh] overflow-y-auto">
              {staff.map((t) => (
                <div key={t.id} draggable
                  onDragStart={() => setDrag(t.id)} onDragEnd={() => setDrag(null)}
                  className="flex items-center gap-1.5 bg-indigo-50 border border-indigo-100 rounded-lg px-2 py-1.5 text-sm cursor-grab active:cursor-grabbing">
                  <GripVertical size={13} className="text-indigo-300" />
                  <div className="min-w-0">
                    <div className="truncate text-slate-800">{t.name}</div>
                    <div className="text-[10px] text-slate-400 truncate">{t.designation}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* columns */}
          <div className="flex-1 overflow-x-auto">
            <div className="flex gap-3 items-start pb-2">
              {cols.map((col, ci) => (
                <div key={ci} className="w-60 shrink-0 bg-slate-50 border border-slate-200 rounded-xl p-2.5">
                  <div className="text-sm font-semibold text-center text-slate-700 mb-2">{col.className} — {col.section}</div>
                  <div className="grid grid-cols-2 gap-1.5 mb-2">
                    <Input value={col.classDocsId} onChange={(e) => mutate(ci, (current) => ({ ...current, classDocsId: e.target.value }))} placeholder="classDocsId" className="!px-2 !py-1 text-[10px] font-mono" />
                    <Input value={col.section} onChange={(e) => mutate(ci, (current) => ({ ...current, section: e.target.value }))} placeholder="section" className="!px-2 !py-1 text-[10px]" />
                  </div>
                  {col.periods.map((p, pi) => {
                    const bad = conflicts.has(ci + '.' + pi);
                    const brk = p.type === 'BREAK';
                    return (
                      <div key={pi} className={`rounded-lg p-2 mb-2 border ${bad ? 'border-rose-400 ring-1 ring-rose-200 bg-rose-50' : brk ? 'bg-amber-50 border-amber-200' : 'bg-white border-slate-200'}`}>
                        <div className="flex items-center gap-1 mb-1.5">
                          <Input type="time" value={p.start} onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, start: e.target.value } : x) }))} className="!px-1.5 !py-1 text-xs w-[74px]" />
                          <span className="text-slate-400 text-xs">–</span>
                          <Input type="time" value={p.end} onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, end: e.target.value } : x) }))} className="!px-1.5 !py-1 text-xs w-[74px]" />
                          <button className="ml-auto text-rose-400 hover:bg-rose-100 rounded p-1" onClick={() => mutate(ci, (c) => ({ ...c, periods: c.periods.filter((_, k) => k !== pi) }))}><X size={13} /></button>
                        </div>
                        <Select value={p.type} onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, type: e.target.value } : x) }))} className="!px-2 !py-1 text-[10px] w-full mb-1.5">
                          <option value="LESSON">LESSON</option>
                          <option value="BREAK">BREAK</option>
                        </Select>
                        {brk ? (
                          <div>
                            <div className="text-[11px] font-semibold text-amber-700 flex items-center gap-1 mb-1"><Coffee size={12} /> Break</div>
                            <Input value={p.subject} placeholder="Label (Lunch)" onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, subject: e.target.value } : x) }))} className="!px-2 !py-1 text-xs w-full" />
                          </div>
                        ) : (
                          <div>
                            <Input value={p.subject} placeholder="Subject" onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, subject: e.target.value } : x) }))} className="!px-2 !py-1 text-xs w-full mb-1.5" />
                            <div
                              onDragOver={(e) => e.preventDefault()}
                              onDrop={() => { if (drag) mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, teacherDocsId: drag } : x) })); }}
                              className={`rounded-md px-2 py-1 text-xs flex items-center justify-between ${p.teacherDocsId ? 'bg-indigo-50 border border-indigo-200 text-slate-700' : 'border border-dashed border-slate-300 text-slate-400'}`}>
                              {p.teacherDocsId ? <><span className="truncate">👩‍🏫 {staffName(p.teacherDocsId)}</span><button className="text-rose-400" onClick={() => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, teacherDocsId: null } : x) }))}><X size={12} /></button></> : 'drop teacher here'}
                            </div>
                            <Input value={p.teacherDocsId || ''} onChange={(e) => mutate(ci, (c) => ({ ...c, periods: c.periods.map((x, k) => k === pi ? { ...x, teacherDocsId: e.target.value || null } : x) }))} placeholder="teacherDocsId" className="!px-2 !py-1 text-[10px] font-mono w-full mt-1.5" />
                          </div>
                        )}
                      </div>
                    );
                  })}
                  <div className="flex gap-1.5">
                    <Button size="sm" className="flex-1" onClick={() => mutate(ci, (c) => { const last = c.periods[c.periods.length - 1]; const s = last ? last.end : '09:00'; return { ...c, periods: [...c.periods, { start: s, end: s, type: 'LESSON', subject: '', teacherDocsId: null }] }; })}><Plus size={13} /> Lesson</Button>
                    <Button size="sm" className="flex-1" onClick={() => mutate(ci, (c) => { const last = c.periods[c.periods.length - 1]; const s = last ? last.end : '09:00'; return { ...c, periods: [...c.periods, { start: s, end: s, type: 'BREAK', subject: 'Break', teacherDocsId: null }] }; })}><Coffee size={13} /> Break</Button>
                  </div>
                </div>
              ))}
            </div>
            {conflicts.size > 0 && (
              <div className="text-xs text-rose-600 mt-1">Red cards clash — the same teacher in two overlapping periods, or two periods overlapping in one section. Fix them before saving.</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
