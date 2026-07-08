import { BookOpen, LayoutGrid, Cpu } from 'lucide-react';

const ITEMS = [
  { key: 'core', label: 'Core', icon: Cpu },
  { key: 'classes', label: 'Classes', icon: BookOpen },
  { key: 'timetable', label: 'Timetable', icon: LayoutGrid },
];

export default function NavRail({ active, onChange }) {
  return (
    <nav className="w-52 bg-white border-r border-slate-200 py-3 shrink-0">
      {ITEMS.map((it) => {
        const on = active === it.key;
        const Icon = it.icon;
        return (
          <button
            key={it.key}
            onClick={() => onChange(it.key)}
            className={`w-full flex items-center gap-3 px-5 py-3 text-sm font-medium border-l-[3px] transition-colors ${on ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-transparent text-slate-600 hover:bg-slate-50'}`}
          >
            <Icon size={18} />
            {it.label}
          </button>
        );
      })}
    </nav>
  );
}
