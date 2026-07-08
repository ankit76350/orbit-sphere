import { GraduationCap, CalendarDays } from 'lucide-react';

export default function TopBar({ schools, schoolId, onSchool, years, year, onYear }) {
  const selectCls =
    'bg-white/10 text-white text-sm rounded-lg px-3 py-1.5 outline-none border border-white/15 focus:border-white/40';
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
      </div>
    </header>
  );
}
