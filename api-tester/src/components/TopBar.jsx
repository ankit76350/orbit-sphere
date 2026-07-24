import { GraduationCap, CalendarDays } from 'lucide-react';

function formatAcademicDate(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return value;
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(year, month - 1, day));
}

export default function TopBar({ schools, schoolId, onSchool, years, year, onYear }) {
  const selectCls =
    'bg-white/10 text-white text-sm rounded-lg px-3 py-1.5 outline-none border border-white/15 focus:border-white/40';
  const selectedYear = years.find((item) => item.name === year);

  return (
    <header className="bg-gradient-to-r from-indigo-700 to-blue-700 text-white px-5 py-3 flex items-center gap-4 shrink-0 shadow">
      <div className="flex items-center gap-2">
        <GraduationCap size={22} />
        <div>
          <div className="font-bold leading-tight">School Admin</div>
          <div className="text-[11px] text-white/70 leading-tight">Edu Sphere</div>
        </div>
      </div>

      <div className="flex items-center gap-2 ml-4">
        <span className="text-[11px] uppercase tracking-wide text-white/60">School</span>
        <select className={selectCls} value={schoolId} onChange={(e) => onSchool(e.target.value)} style={{ maxWidth: 240 }}>
          <option value="">Choose a school…</option>
          {schools.map((s) => (
            <option key={s.id} value={s.id} className="text-slate-800">{s.schoolName || s.name || s.id}</option>
          ))}
        </select>
        {schoolId && (
          <span className="rounded-md border border-white/15 bg-white/10 px-2 py-1 text-[10px] text-white/70">
            MongoDB ID: <code className="font-mono font-semibold text-white select-all">{schoolId}</code>
          </span>
        )}
      </div>

      <div className="flex items-center gap-2">
        <CalendarDays size={15} className="text-white/70" />
        <span className="text-[11px] uppercase tracking-wide text-white/60">Year</span>
        <select className={selectCls} value={year} onChange={(e) => onYear(e.target.value)} disabled={!years.length}>
          {!years.length && <option value="">— none —</option>}
          {years.map((y) => (
            <option key={y.id} value={y.name} className="text-slate-800">{y.name}</option>
          ))}
        </select>
        {selectedYear && (
          <span className="rounded-md border border-white/15 bg-white/10 px-2 py-1 text-[10px] text-white/70 whitespace-nowrap">
            <code className="font-mono font-semibold text-white select-all cursor-text">{selectedYear.name}</code>
          </span>
        )}
        {selectedYear && (
          <span
            className="rounded-md border border-white/15 bg-white/10 px-2.5 py-1 text-[10px] text-white/75 whitespace-nowrap"
            title={`${selectedYear.startDate || 'No start date'} to ${selectedYear.endDate || 'No end date'}`}
          >
            <span className="text-white/55 uppercase tracking-wide">Start</span>{' '}
            <span className="font-semibold text-white">{formatAcademicDate(selectedYear.startDate)}</span>
            <span className="mx-1.5 text-white/35">•</span>
            <span className="text-white/55 uppercase tracking-wide">End</span>{' '}
            <span className="font-semibold text-white">{formatAcademicDate(selectedYear.endDate)}</span>
          </span>
        )}
      </div>
    </header>
  );
}
