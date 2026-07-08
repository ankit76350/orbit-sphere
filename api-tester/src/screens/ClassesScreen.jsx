import { useEffect, useState } from 'react';
import { BookOpen, Plus, Trash2, Users } from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Empty, Badge, useToast } from '../components/ui.jsx';

export default function ClassesScreen({ schoolId, year }) {
  const toast = useToast();
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ name: '', sections: 'A, B' });
  const [busy, setBusy] = useState(false);

  const load = () => {
    if (!schoolId || !year) { setClasses([]); return; }
    setLoading(true);
    api.classesByYear(schoolId, year).then((c) => { setClasses(c); setLoading(false); });
  };
  useEffect(load, [schoolId, year]);

  async function create() {
    const sections = form.sections.split(',').map((s) => s.trim()).filter(Boolean);
    setBusy(true);
    try {
      await api.createClass({ schoolId, name: form.name.trim(), academicYear: year, sections });
      toast.success(`Class “${form.name}” created.`);
      setForm({ name: '', sections: 'A, B' });
      load();
    } catch (e) { toast.error(e.message); } finally { setBusy(false); }
  }

  async function remove(c) {
    if (!confirm(`Delete ${c.name}? This cannot be undone.`)) return;
    try { await api.deleteClass(c.id); toast.success(`Deleted ${c.name}.`); load(); }
    catch (e) { toast.error(e.message); }
  }

  if (!schoolId) return <Empty icon={BookOpen} title="Pick a school to begin" />;
  if (!year) return <Empty icon={BookOpen} title="No academic year selected" hint="Create one under Academic Year first." />;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <Card title="Add a class" subtitle={`For academic year ${year}`} className="lg:col-span-1">
        <div className="space-y-4">
          <Field label="Class name" hint="Unique within this academic year.">
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Class 5" />
          </Field>
          <Field label="Sections" hint="Comma-separated, e.g. A, B, C">
            <Input value={form.sections} onChange={(e) => setForm({ ...form, sections: e.target.value })} placeholder="A, B" />
          </Field>
          <Button variant="primary" onClick={create} disabled={busy || !form.name.trim()}>
            <Plus size={16} /> {busy ? 'Creating…' : 'Create class'}
          </Button>
        </div>
      </Card>

      <Card title="Classes" subtitle={`${classes.length} class${classes.length === 1 ? '' : 'es'} in ${year}`} className="lg:col-span-2">
        {loading ? (
          <Empty icon={BookOpen} title="Loading…" />
        ) : classes.length === 0 ? (
          <Empty icon={BookOpen} title="No classes yet" hint="Add your first class on the left." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {classes.slice().sort((a, b) => a.name.localeCompare(b.name)).map((c) => (
              <div key={c.id} className="border border-slate-200 rounded-xl p-4 hover:shadow-sm transition">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 grid place-items-center"><BookOpen size={17} /></div>
                    <div>
                      <div className="font-semibold text-slate-800">{c.name}</div>
                      <div className="text-xs text-slate-500 flex items-center gap-1"><Users size={12} /> {(c.sections || []).length} section{(c.sections || []).length === 1 ? '' : 's'}</div>
                    </div>
                  </div>
                  <button className="text-rose-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg p-1.5" title="Delete class" onClick={() => remove(c)}>
                    <Trash2 size={15} />
                  </button>
                </div>
                <div className="flex flex-wrap gap-1.5 mt-3">
                  {(c.sections || []).length === 0
                    ? <span className="text-xs text-amber-600">No sections — add some to build a timetable.</span>
                    : c.sections.map((s) => <Badge key={s} color="slate">Section {s}</Badge>)}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
