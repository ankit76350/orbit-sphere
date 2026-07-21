import { useMemo, useState, useEffect } from 'react';
import { CalendarPlus, Plus, Trash2, PartyPopper, CalendarOff, X, RefreshCw } from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';
import { DAYS, DAY_LABEL, niceDate } from '../lib/date.js';

const HOLIDAY_TYPES = ['PUBLIC_HOLIDAY', 'FESTIVAL', 'RELIGIOUS', 'SCHOOL_EVENT', 'VACATION', 'EXAM_BREAK', 'OTHER'];

export default function AcademicYearsScreen({ schoolId, years, year, reload }) {
  const toast = useToast();
  const current = useMemo(() => years.find((y) => y.name === year), [years, year]);

  if (!schoolId) return <Empty icon={CalendarPlus} title="Pick a school to begin" hint="Use the selector in the top bar." />;

  return current
    ? <YearManager schoolId={schoolId} yearDoc={current} reload={reload} toast={toast} />
    : <CreateYear schoolId={schoolId} reload={reload} toast={toast} />;
}

const emptyHoliday = () => ({ name: '', description: '', type: 'FESTIVAL', date: '' });

function HolidayEditor({ holidays, onChange }) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-600">Initial holidays <code className="font-mono text-[10px] font-medium text-slate-400">holidays[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
        <button type="button" onClick={() => onChange([...holidays, emptyHoliday()])} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add holiday</button>
      </div>
      {holidays.map((holiday, index) => (
        <div key={index} className="border border-slate-200 rounded-lg p-3 space-y-2 bg-slate-50/50">
          <div className="grid grid-cols-2 gap-2">
            <Input value={holiday.name} onChange={(e) => onChange(holidays.map((item, i) => i === index ? { ...item, name: e.target.value } : item))} placeholder="name (optional)" />
            <Input type="date" value={holiday.date} onChange={(e) => onChange(holidays.map((item, i) => i === index ? { ...item, date: e.target.value } : item))} />
            <Select value={holiday.type} onChange={(e) => onChange(holidays.map((item, i) => i === index ? { ...item, type: e.target.value } : item))}>
              {HOLIDAY_TYPES.map((type) => <option key={type} value={type}>{type.replaceAll('_', ' ')}</option>)}
            </Select>
            <div className="flex gap-2">
              <Input value={holiday.description} onChange={(e) => onChange(holidays.map((item, i) => i === index ? { ...item, description: e.target.value } : item))} placeholder="description (optional)" className="flex-1" />
              <button type="button" onClick={() => onChange(holidays.filter((_, i) => i !== index))} className="p-2 text-slate-400 hover:text-rose-600"><Trash2 size={14} /></button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function CreateYear({ schoolId, reload, toast }) {
  const [form, setForm] = useState({ name: '', startDate: '', endDate: '', holidays: [] });
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit() {
    setBusy(true);
    try {
      await api.createAcademicYear({
        schoolId,
        ...form,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        holidays: form.holidays.map((holiday) => ({ ...holiday, date: holiday.date || null })),
      });
      toast.success(`Academic year “${form.name}” created.`);
      reload(form.name);
    } catch (e) { toast.error(e.message); } finally { setBusy(false); }
  }

  return (
    <div className="max-w-xl">
      <Card title="Create an academic year" subtitle="A school runs on academic years — set one up to start.">
        <div className="grid grid-cols-1 gap-4">
          <Field label="School ID" apiName="schoolId" required><Input value={schoolId} readOnly className="bg-slate-50 font-mono text-xs" /></Field>
          <Field label="Name" apiName="name" required hint="e.g. 2026-2027 — cannot be changed later.">
            <Input value={form.name} onChange={set('name')} placeholder="2026-2027" />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Starts on" apiName="startDate" required={false}><Input type="date" value={form.startDate} onChange={set('startDate')} /></Field>
            <Field label="Ends on" apiName="endDate" required={false}><Input type="date" value={form.endDate} onChange={set('endDate')} /></Field>
          </div>
          <HolidayEditor holidays={form.holidays} onChange={(holidays) => setForm((current) => ({ ...current, holidays }))} />
          <div>
            <Button variant="primary" onClick={submit} disabled={busy || !form.name}>
              <CalendarPlus size={16} /> {busy ? 'Creating…' : 'Create academic year'}
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}

function YearManager({ schoolId, yearDoc, reload, toast }) {
  const holidays = (yearDoc.holidays || []).slice().sort((a, b) => (a.date || '').localeCompare(b.date || ''));
  const dated = holidays.filter((h) => h.type !== 'WEEKLY_OFF');
  const weeklyOffDays = [...new Set(holidays.filter((h) => h.type === 'WEEKLY_OFF' && h.date)
    .map((h) => new Date(h.date + 'T00:00:00Z').getUTCDay()))];
  const weeklyOffNames = weeklyOffDays.map((d) => DAYS[(d + 6) % 7]);

  const [hol, setHol] = useState({ name: '', description: '', type: 'FESTIVAL', date: '' });
  const [offDay, setOffDay] = useState('SUNDAY');
  const [busy, setBusy] = useState(false);

  // New Academic Year modal states
  const [showYearModal, setShowYearModal] = useState(false);
  const [yearForm, setYearForm] = useState({ name: '', startDate: '', endDate: '', holidays: [] });
  const [busyYear, setBusyYear] = useState(false);

  const handleYearFormChange = (k) => (e) => setYearForm((f) => ({ ...f, [k]: e.target.value }));

  const submitNewAcademicYear = async () => {
    if (!yearForm.name) {
      toast.error("Name is required to create a new academic year.");
      return;
    }
    setBusyYear(true);
    try {
      await api.createAcademicYear({
        schoolId,
        ...yearForm,
        startDate: yearForm.startDate || null,
        endDate: yearForm.endDate || null,
        holidays: yearForm.holidays.map((holiday) => ({ ...holiday, date: holiday.date || null })),
      });
      toast.success(`Academic year “${yearForm.name}” created.`);
      setShowYearModal(false);
      setYearForm({ name: '', startDate: '', endDate: '', holidays: [] });
      reload(yearForm.name); // Reload context and select new year
    } catch (e) {
      toast.error(e.message || "Failed to create academic year.");
    } finally {
      setBusyYear(false);
    }
  };

  const run = async (fn, ok) => {
    setBusy(true);
    try { await fn(); toast.success(ok); reload(yearDoc.name); }
    catch (e) { toast.error(e.message); } finally { setBusy(false); }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      {/* summary */}
      <Card title={yearDoc.name} subtitle="Academic year" className="lg:col-span-1"
        right={<Badge color="blue">Active</Badge>}>
        <dl className="text-sm space-y-2">
          <div className="flex justify-between"><dt className="text-slate-500">Starts</dt><dd className="font-medium">{niceDate(yearDoc.startDate)} {yearDoc.startDate?.slice(0,4)}</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Ends</dt><dd className="font-medium">{niceDate(yearDoc.endDate)} {yearDoc.endDate?.slice(0,4)}</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Holidays</dt><dd className="font-medium">{dated.length} dates</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Weekly off</dt><dd className="font-medium">{weeklyOffNames.length ? weeklyOffNames.map((d) => DAY_LABEL[d].slice(0, 3)).join(', ') : '—'}</dd></div>
        </dl>
        <div className="mt-5 pt-4 border-t border-slate-100">
          <Button variant="primary" className="w-full justify-center" onClick={() => setShowYearModal(true)}>
            <CalendarPlus size={15} /> New Academic Year
          </Button>
        </div>
      </Card>

      {/* add holiday + weekly off */}
      <Card title="Add holidays" subtitle="Dated holidays and recurring weekly offs are skipped when building timetables." className="lg:col-span-2">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* dated */}
          <div>
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700 mb-3"><PartyPopper size={16} /> A holiday on a date</div>
            <div className="space-y-3">
              <Field label="Name"><Input value={hol.name} onChange={(e) => setHol({ ...hol, name: e.target.value })} placeholder="Diwali" /></Field>
              <Field label="Description" apiName="description" required={false}><Input value={hol.description} onChange={(e) => setHol({ ...hol, description: e.target.value })} placeholder="Optional details" /></Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Type">
                  <Select value={hol.type} onChange={(e) => setHol({ ...hol, type: e.target.value })}>
                    {HOLIDAY_TYPES.map((t) => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
                  </Select>
                </Field>
                <Field label="Date"><Input type="date" min={yearDoc.startDate} max={yearDoc.endDate} value={hol.date} onChange={(e) => setHol({ ...hol, date: e.target.value })} /></Field>
              </div>
              <Button variant="primary" size="sm" disabled={busy || !hol.name || !hol.date}
                onClick={() => run(() => api.addHolidays(yearDoc.id, [{ name: hol.name, description: hol.description || null, type: hol.type, date: hol.date }]).then(() => setHol({ name: '', description: '', type: 'FESTIVAL', date: '' })), `Added “${hol.name}”.`)}>
                <Plus size={15} /> Add holiday
              </Button>
            </div>
          </div>
          {/* weekly off */}
          <div>
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700 mb-3"><CalendarOff size={16} /> A weekly off day</div>
            <p className="text-xs text-slate-500 mb-3">Marks every occurrence of a weekday off for the whole year (e.g. every Sunday). Schools that run on Sunday can pick a different day.</p>
            <div className="flex items-end gap-2">
              <Field label="Day of week">
                <Select value={offDay} onChange={(e) => setOffDay(e.target.value)}>
                  {DAYS.map((d) => <option key={d} value={d}>{DAY_LABEL[d]}</option>)}
                </Select>
              </Field>
              <Button variant="primary" size="sm" disabled={busy}
                onClick={() => run(() => api.addWeeklyOff(yearDoc.id, offDay), `Every ${DAY_LABEL[offDay]} marked off.`)}>
                <Plus size={15} /> Add
              </Button>
            </div>
            {weeklyOffNames.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-4">
                {weeklyOffNames.map((d) => (
                  <span key={d} className="flex items-center gap-1 bg-slate-100 rounded-full pl-3 pr-1 py-1 text-xs">
                    {DAY_LABEL[d]}
                    <button className="text-rose-500 hover:bg-rose-100 rounded-full p-0.5" title="Remove series"
                      onClick={() => run(() => api.removeWeeklyOff(yearDoc.id, d), `Removed ${DAY_LABEL[d]} offs.`)}>
                      <Trash2 size={13} />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      </Card>

      {/* holiday list */}
      <Card title="Holiday calendar" subtitle={`${dated.length} dated holiday${dated.length === 1 ? '' : 's'} this year`} className="lg:col-span-3">
        {dated.length === 0 ? (
          <Empty icon={PartyPopper} title="No dated holidays yet" hint="Add festivals, national holidays and school events above." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
            {dated.map((h, i) => (
              <div key={i} className="flex items-center justify-between bg-slate-50 border border-slate-100 rounded-lg px-3 py-2">
                <div>
                  <div className="text-sm font-medium text-slate-800">{h.name}</div>
                  <div className="text-xs text-slate-500">{niceDate(h.date)} {h.date?.slice(0,4)} · {String(h.type).replace('_', ' ').toLowerCase()}</div>
                </div>
                <button className="text-rose-500 hover:bg-rose-100 rounded-lg p-1.5" title="Remove"
                  onClick={() => run(() => api.removeHoliday(yearDoc.id, h.date, h.name), `Removed “${h.name}”.`)}>
                  <Trash2 size={15} />
                </button>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* New Academic Year Modal */}
      {showYearModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl shadow-xl max-w-md w-full p-6 text-slate-800 animate-in fade-in zoom-in-95 duration-150 relative">
            <button 
              onClick={() => setShowYearModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 hover:bg-slate-50 p-1.5 rounded-lg transition"
              title="Close modal"
            >
              <X size={16} />
            </button>
            
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <CalendarPlus size={18} className="text-blue-600" />
              <span>Create academic year</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 mb-5">Set up a new academic year range for this school.</p>

            <div className="space-y-4">
              <Field label="School ID" apiName="schoolId" required><Input value={schoolId} readOnly className="bg-slate-50 font-mono text-xs" /></Field>
              <Field label="Name" apiName="name" required hint="e.g. 2026-2027 — cannot be changed later.">
                <Input value={yearForm.name} onChange={handleYearFormChange('name')} placeholder="2026-2027" className="text-slate-800" />
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Starts on" apiName="startDate" required={false}>
                  <Input type="date" value={yearForm.startDate} onChange={handleYearFormChange('startDate')} className="text-slate-800" />
                </Field>
                <Field label="Ends on" apiName="endDate" required={false}>
                  <Input type="date" value={yearForm.endDate} onChange={handleYearFormChange('endDate')} className="text-slate-800" />
                </Field>
              </div>
              <HolidayEditor holidays={yearForm.holidays} onChange={(holidays) => setYearForm((current) => ({ ...current, holidays }))} />

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100 mt-5">
                <Button variant="default" size="sm" onClick={() => setShowYearModal(false)}>
                  Cancel
                </Button>
                <Button 
                  variant="primary" 
                  size="sm" 
                  onClick={submitNewAcademicYear}
                  disabled={busyYear || !yearForm.name}
                >
                  {busyYear ? 'Creating…' : 'Create academic year'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
