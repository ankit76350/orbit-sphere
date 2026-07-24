import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Coffee, User } from 'lucide-react';
import { api } from '../api.js';
import { Field, Select, Empty } from '../components/ui.jsx';
import { DAYS, DAY_LABEL, mondayOf, addDays, dayOfWeek, niceDate, hhmm, today } from '../lib/date.js';

export default function TimetableView({ schoolId, classes, staff }) {
  const [mode, setMode] = useState('class'); // 'class' | 'teacher'
  const [classDocsId, setClassDocsId] = useState('');
  const [section, setSection] = useState('');
  const [teacherDocsId, setTeacherDocsId] = useState('');
  const [weekStart, setWeekStart] = useState(mondayOf(today()));
  const [byDate, setByDate] = useState({});
  const [loading, setLoading] = useState(false);

  const cls = classes.find((c) => c.id === classDocsId);
  const nameOf = (id) => (staff.find((s) => s.id === id) || {}).name || id;
  const classNameOf = (id) => (classes.find((c) => c.id === id) || {}).name || id;

  useEffect(() => {
    const end = addDays(weekStart, 6);
    const canClass = mode === 'class' && classDocsId && section;
    const canTeacher = mode === 'teacher' && teacherDocsId;
    if (!schoolId || (!canClass && !canTeacher)) { setByDate({}); return; }
    setLoading(true);
    const p = mode === 'class'
      ? api.sectionSchedule(schoolId, classDocsId, section, weekStart, end)
      : api.teacherSchedule(schoolId, teacherDocsId, weekStart, end);
    p.then((days) => {
      const map = {};
      days.forEach((d) => { map[d.date] = d.entries; });
      setByDate(map);
      setLoading(false);
    });
  }, [schoolId, mode, classDocsId, section, teacherDocsId, weekStart]);

  return (
    <div>
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <Field label="Show">
          <Select value={mode} onChange={(e) => setMode(e.target.value)}>
            <option value="class">A class &amp; section</option>
            <option value="teacher">A teacher</option>
          </Select>
        </Field>
        {mode === 'class' ? (
          <>
            <Field label="Class">
              <Select value={classDocsId} onChange={(e) => { const c = classes.find((x) => x.id === e.target.value); setClassDocsId(e.target.value); setSection((c && c.sections && c.sections[0]) || ''); }}>
                <option value="">Choose…</option>
                {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>
            </Field>
            <Field label="Section">
              <Select value={section} onChange={(e) => setSection(e.target.value)}>
                {(cls?.sections || []).map((s) => <option key={s} value={s}>{s}</option>)}
              </Select>
            </Field>
          </>
        ) : (
          <Field label="Teacher">
            <Select value={teacherDocsId} onChange={(e) => setTeacherDocsId(e.target.value)}>
              <option value="">Choose…</option>
              {staff.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </Select>
          </Field>
        )}

        <div className="flex items-center gap-1 ml-auto">
          <button className="p-2 rounded-lg hover:bg-slate-100" onClick={() => setWeekStart(addDays(weekStart, -7))}><ChevronLeft size={18} /></button>
          <div className="text-sm font-medium text-slate-600 w-40 text-center">
            {niceDate(weekStart)} – {niceDate(addDays(weekStart, 6))}
          </div>
          <button className="p-2 rounded-lg hover:bg-slate-100" onClick={() => setWeekStart(addDays(weekStart, 7))}><ChevronRight size={18} /></button>
        </div>
      </div>

      {loading ? (
        <Empty title="Loading…" />
      ) : (
        <div className="grid grid-cols-7 gap-2">
          {DAYS.map((_, i) => {
            const date = addDays(weekStart, i);
            const dow = dayOfWeek(date);
            const entries = byDate[date];
            return (
              <div key={date} className="bg-slate-50 border border-slate-200 rounded-xl p-2 min-h-[140px]">
                <div className="text-center mb-2">
                  <div className="text-xs font-semibold text-slate-700">{DAY_LABEL[dow]}</div>
                  <div className="text-[11px] text-slate-400">{niceDate(date)}</div>
                </div>
                {!entries ? (
                  <div className="text-[11px] text-slate-300 text-center mt-4">No school</div>
                ) : (
                  entries.slice().sort((a, b) => a.startTime.localeCompare(b.startTime)).map((e, k) => {
                    const brk = e.type === 'BREAK';
                    return (
                      <div key={k} className={`rounded-lg px-2 py-1.5 mb-1.5 border-l-[3px] ${brk ? 'bg-amber-50 border-amber-400' : 'bg-blue-50 border-blue-500'}`}>
                        <div className="text-[10px] text-slate-500">{hhmm(e.startTime)}–{hhmm(e.endTime)}</div>
                        <div className="text-[12px] font-semibold text-slate-800 flex items-center gap-1">
                          {brk && <Coffee size={11} />}{e.subject}
                        </div>
                        <div className="text-[11px] text-slate-500 flex items-center gap-1">
                          {mode === 'class'
                            ? (!brk && <><User size={10} /> {nameOf(e.teacherDocsId)}</>)
                            : `${classNameOf(e.classDocsId)} · ${e.section}`}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
