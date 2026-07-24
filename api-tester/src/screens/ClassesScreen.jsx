import { useEffect, useState } from 'react';
import { BookOpen, Eye, Plus, RefreshCw, Trash2, Users, X } from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Empty, Badge, useToast } from '../components/ui.jsx';

const emptyForm = { schoolId: '', academicYear: '', name: '', classTeacherDocsId: '', sections: 'A, B', subjects: [] };
const emptySubjectForm = { name: '', teacherDocsId: '' };

export default function ClassesScreen({ schoolId, year, staff = [] }) {
  const toast = useToast();
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [busy, setBusy] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsBusy, setDetailsBusy] = useState('');
  const [newSections, setNewSections] = useState('');
  const [newSubject, setNewSubject] = useState(emptySubjectForm);

  const load = () => {
    if (!schoolId || !year) { setClasses([]); return; }
    setLoading(true);
    api.classesByYear(schoolId, year).then((c) => { setClasses(c); setLoading(false); });
  };
  useEffect(load, [schoolId, year]);
  useEffect(() => setForm((current) => ({ ...current, schoolId: schoolId || '', academicYear: year || '' })), [schoolId, year]);

  async function create() {
    const sections = form.sections.split(',').map((s) => s.trim()).filter(Boolean);
    setBusy(true);
    try {
      await api.createClass({
        schoolId: form.schoolId,
        name: form.name.trim(),
        classTeacherDocsId: form.classTeacherDocsId || null,
        subjects: form.subjects.map((subject) => ({
          name: subject.name || null,
          teacherDocsId: subject.teacherDocsId || null,
        })),
        academicYear: form.academicYear || null,
        sections,
      });
      toast.success(`Class “${form.name}” created.`);
      setForm({ ...emptyForm, schoolId: schoolId || '', academicYear: year || '' });
      load();
    } catch (e) { toast.error(e.message); } finally { setBusy(false); }
  }

  async function remove(c) {
    if (!confirm(`Delete ${c.name}? This cannot be undone.`)) return;
    try { await api.deleteClass(c.id); toast.success(`Deleted ${c.name}.`); load(); }
    catch (e) { toast.error(e.message); }
  }

  async function openDetails(c) {
    setSelectedClass(c);
    setNewSections('');
    setNewSubject(emptySubjectForm);
    setDetailsLoading(true);
    try {
      setSelectedClass(await api.getClassById(c.id));
    } catch (e) {
      toast.error(e.message || `Failed to load details for ${c.name}.`);
    } finally {
      setDetailsLoading(false);
    }
  }

  const applyClassUpdate = (updatedClass) => {
    setSelectedClass(updatedClass);
    setClasses((current) => current.map((item) => item.id === updatedClass.id ? updatedClass : item));
  };

  async function addSections() {
    const sections = newSections.split(',').map((section) => section.trim()).filter(Boolean);
    if (sections.length === 0) {
      toast.error('Enter at least one section.');
      return;
    }

    setDetailsBusy('sections');
    try {
      const updatedClass = await api.addSections(selectedClass.id, sections);
      applyClassUpdate(updatedClass);
      setNewSections('');
      toast.success(`${sections.length === 1 ? 'Section' : 'Sections'} ${sections.join(', ')} added to ${updatedClass.name}.`);
    } catch (e) {
      toast.error(e.message || 'Failed to add sections.');
    } finally {
      setDetailsBusy('');
    }
  }

  async function addSubject() {
    const name = newSubject.name.trim();
    if (!name) {
      toast.error('Subject name is required.');
      return;
    }

    setDetailsBusy('subject');
    try {
      const updatedClass = await api.addSubject(selectedClass.id, {
        name,
        teacherDocsId: newSubject.teacherDocsId || null,
      });
      applyClassUpdate(updatedClass);
      setNewSubject(emptySubjectForm);
      toast.success(`Subject “${name}” added to ${updatedClass.name}.`);
    } catch (e) {
      toast.error(e.message || 'Failed to add subject.');
    } finally {
      setDetailsBusy('');
    }
  }

  const getStaffMember = (staffDocsId) => staff.find((member) => member.id === staffDocsId);

  const formatTimestamp = (value) => {
    if (!value) return '—';
    const timestamp = new Date(value);
    return Number.isNaN(timestamp.getTime()) ? value : timestamp.toLocaleString();
  };

  if (!schoolId) return <Empty icon={BookOpen} title="Pick a school to begin" />;
  if (!year) return <Empty icon={BookOpen} title="No academic year selected" hint="Create one under Academic Year first." />;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <Card title="Add a class" subtitle={`For academic year ${year}`} className="lg:col-span-1">
        <div className="space-y-4">
          <Field label="School ID" apiName="schoolId" required>
            <Input value={form.schoolId} onChange={(e) => setForm({ ...form, schoolId: e.target.value })} className="font-mono text-xs" />
          </Field>
          <Field label="Academic Year" apiName="academicYear" required={false}>
            <Input value={form.academicYear} onChange={(e) => setForm({ ...form, academicYear: e.target.value })} />
          </Field>
          <Field label="Class name" apiName="name" required hint="Unique within this academic year.">
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Class 5" />
          </Field>
          <Field label="Class teacher" apiName="classTeacherDocsId" required={false}>
            <Select value={form.classTeacherDocsId} onChange={(e) => setForm({ ...form, classTeacherDocsId: e.target.value })}>
              <option value="">— none —</option>
              {staff.map((member) => <option key={member.id} value={member.id}>{member.name || member.employeeNo || member.id}</option>)}
            </Select>
          </Field>
          <Field label="Sections" apiName="sections[]" required={false} hint="Comma-separated, e.g. A, B, C">
            <Input value={form.sections} onChange={(e) => setForm({ ...form, sections: e.target.value })} placeholder="A, B" />
          </Field>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-600">Subjects <code className="font-mono text-[10px] font-medium text-slate-400">subjects[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
              <button type="button" onClick={() => setForm({ ...form, subjects: [...form.subjects, { name: '', teacherDocsId: '' }] })} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add</button>
            </div>
            {form.subjects.map((subject, index) => (
              <div key={index} className="grid grid-cols-[1fr_1fr_auto] gap-2 items-center">
                <Input value={subject.name} onChange={(e) => setForm({ ...form, subjects: form.subjects.map((item, i) => i === index ? { ...item, name: e.target.value } : item) })} placeholder="name" />
                <Select value={subject.teacherDocsId} onChange={(e) => setForm({ ...form, subjects: form.subjects.map((item, i) => i === index ? { ...item, teacherDocsId: e.target.value } : item) })}>
                  <option value="">teacher (optional)</option>
                  {staff.map((member) => <option key={member.id} value={member.id}>{member.name || member.employeeNo || member.id}</option>)}
                </Select>
                <button type="button" onClick={() => setForm({ ...form, subjects: form.subjects.filter((_, i) => i !== index) })} className="p-1 text-slate-400 hover:text-rose-600"><X size={14} /></button>
              </div>
            ))}
          </div>
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
              <div
                key={c.id}
                role="button"
                tabIndex={0}
                aria-label={`View all details for ${c.name}`}
                onClick={() => openDetails(c)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openDetails(c);
                  }
                }}
                className="border border-slate-200 rounded-xl p-4 cursor-pointer hover:border-blue-300 hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400 transition"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 grid place-items-center"><BookOpen size={17} /></div>
                    <div>
                      <div className="font-semibold text-slate-800">{c.name}</div>
                      <div className="text-[10px] font-mono text-slate-400 break-all" title={c.id || ''}>MongoDB Object ID: {c.id || '—'}</div>
                      <div className="text-xs text-slate-500 flex items-center gap-1"><Users size={12} /> {(c.sections || []).length} section{(c.sections || []).length === 1 ? '' : 's'}</div>
                    </div>
                  </div>
                  <button
                    className="text-rose-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg p-1.5"
                    title="Delete class"
                    onClick={(event) => {
                      event.stopPropagation();
                      remove(c);
                    }}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
                <div className="flex flex-wrap gap-1.5 mt-3">
                  {(c.sections || []).length === 0
                    ? <span className="text-xs text-amber-600">No sections — add some to build a timetable.</span>
                    : c.sections.map((s) => <Badge key={s} color="slate">Section {s}</Badge>)}
                </div>
                <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] font-semibold text-blue-600">
                  <span>View all class details</span>
                  <Eye size={13} />
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {selectedClass && (
        <div
          className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setSelectedClass(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="class-details-title"
            className="bg-white border border-slate-200 rounded-2xl shadow-xl max-w-3xl w-full max-h-[90vh] flex flex-col text-slate-800 animate-in fade-in zoom-in-95 duration-150"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="px-6 py-4 border-b border-slate-100 bg-slate-50/70 flex items-start justify-between gap-4 rounded-t-2xl">
              <div>
                <h3 id="class-details-title" className="font-bold text-slate-900 text-base flex items-center gap-2">
                  <BookOpen size={18} className="text-blue-600" />
                  <span>{selectedClass.name || 'Class details'}</span>
                </h3>
                <p className="text-xs text-slate-500 mt-1">All details returned by GET /api/classes/{'{classDocsId}'}</p>
              </div>
              <button
                onClick={() => setSelectedClass(null)}
                className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                title="Close class details"
              >
                <X size={17} />
              </button>
            </header>

            <div className="p-6 overflow-y-auto space-y-5">
              {detailsLoading && (
                <div className="flex items-center gap-2 text-xs text-blue-600">
                  <RefreshCw size={13} className="animate-spin" />
                  Refreshing the complete class document…
                </div>
              )}

              <section className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {[
                  ['Class Name', selectedClass.name],
                  ['Academic Year', selectedClass.academicYear],
                  ['MongoDB Object ID', selectedClass.id],
                  ['School ID', selectedClass.schoolId],
                  ['Created At', formatTimestamp(selectedClass.createdAt)],
                  ['Updated At', formatTimestamp(selectedClass.updatedAt)],
                ].map(([label, value]) => (
                  <div key={label} className="border border-slate-100 bg-slate-50 rounded-xl px-4 py-3">
                    <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">{label}</div>
                    <div className={`mt-1 text-xs text-slate-800 break-all ${label.includes('ID') ? 'font-mono select-all' : 'font-semibold'}`}>
                      {value || '—'}
                    </div>
                  </div>
                ))}
              </section>

              <section>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Class Teacher</h4>
                {selectedClass.classTeacherDocsId ? (() => {
                  const teacher = getStaffMember(selectedClass.classTeacherDocsId);
                  return (
                    <div className="border border-slate-200 rounded-xl px-4 py-3">
                      <div className="font-semibold text-sm text-slate-800">{teacher?.name || 'Teacher record not loaded'}</div>
                      {teacher?.employeeNo && <div className="text-xs text-slate-500 mt-0.5">Employee No: {teacher.employeeNo}</div>}
                      <div className="font-mono text-[10px] text-slate-400 mt-1 break-all select-all">
                        classTeacherDocsId: {selectedClass.classTeacherDocsId}
                      </div>
                    </div>
                  );
                })() : (
                  <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">No class teacher assigned.</div>
                )}
              </section>

              <section>
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Sections</h4>
                  <span className="text-[10px] font-semibold text-slate-400">
                    {(selectedClass.sections || []).length} section{(selectedClass.sections || []).length === 1 ? '' : 's'}
                  </span>
                </div>
                {(selectedClass.sections || []).length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {selectedClass.sections.map((section) => <Badge key={section} color="blue">Section {section}</Badge>)}
                  </div>
                ) : (
                  <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">No sections configured.</div>
                )}
                <div className="mt-3 border border-blue-100 bg-blue-50/50 rounded-xl p-3">
                  <div className="text-[10px] font-mono text-blue-500 mb-2 break-all">
                    POST /api/classes/{selectedClass.id}/sections
                  </div>
                  <div className="flex flex-col sm:flex-row gap-2">
                    <Input
                      value={newSections}
                      onChange={(event) => setNewSections(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') addSections();
                      }}
                      placeholder="A, B, C"
                      aria-label="Sections to add"
                      className="flex-1"
                    />
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={addSections}
                      disabled={detailsBusy !== '' || !newSections.trim()}
                      className="justify-center"
                    >
                      {detailsBusy === 'sections' ? <RefreshCw size={13} className="animate-spin" /> : <Plus size={13} />}
                      Add section{newSections.includes(',') ? 's' : ''}
                    </Button>
                  </div>
                  <p className="text-[10px] text-slate-400 mt-1.5">Separate multiple section names with commas. Request field: <code>section[]</code>.</p>
                </div>
              </section>

              <section>
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Subjects</h4>
                  <span className="text-[10px] font-semibold text-slate-400">
                    {(selectedClass.subjects || []).length} subject{(selectedClass.subjects || []).length === 1 ? '' : 's'}
                  </span>
                </div>
                {(selectedClass.subjects || []).length > 0 ? (
                  <div className="space-y-2">
                    {selectedClass.subjects.map((subject, index) => {
                      const teacher = getStaffMember(subject.teacherDocsId);
                      return (
                        <div key={`${subject.name || 'subject'}-${index}`} className="border border-slate-200 rounded-xl px-4 py-3">
                          <div className="font-semibold text-sm text-slate-800">{subject.name || 'Unnamed subject'}</div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            Teacher: {teacher?.name || (subject.teacherDocsId ? 'Teacher record not loaded' : 'Not assigned')}
                            {teacher?.employeeNo ? ` · ${teacher.employeeNo}` : ''}
                          </div>
                          {subject.teacherDocsId && (
                            <div className="font-mono text-[10px] text-slate-400 mt-1 break-all select-all">
                              teacherDocsId: {subject.teacherDocsId}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">No subjects configured.</div>
                )}
                <div className="mt-3 border border-blue-100 bg-blue-50/50 rounded-xl p-3">
                  <div className="text-[10px] font-mono text-blue-500 mb-2 break-all">
                    POST /api/classes/{selectedClass.id}/subjects
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-[1fr_1fr_auto] gap-2">
                    <Input
                      value={newSubject.name}
                      onChange={(event) => setNewSubject({ ...newSubject, name: event.target.value })}
                      placeholder="Subject name"
                      aria-label="Subject name"
                    />
                    <Select
                      value={newSubject.teacherDocsId}
                      onChange={(event) => setNewSubject({ ...newSubject, teacherDocsId: event.target.value })}
                      aria-label="Subject teacher"
                    >
                      <option value="">Teacher (optional)</option>
                      {staff.map((member) => (
                        <option key={member.id} value={member.id}>
                          {member.name || member.employeeNo || member.id}
                        </option>
                      ))}
                    </Select>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={addSubject}
                      disabled={detailsBusy !== '' || !newSubject.name.trim()}
                      className="justify-center"
                    >
                      {detailsBusy === 'subject' ? <RefreshCw size={13} className="animate-spin" /> : <Plus size={13} />}
                      Add subject
                    </Button>
                  </div>
                  <p className="text-[10px] text-slate-400 mt-1.5">Sends <code>name</code> and optional <code>teacherDocsId</code>.</p>
                </div>
              </section>

              <details className="border border-slate-200 rounded-xl overflow-hidden">
                <summary className="cursor-pointer px-4 py-3 bg-slate-50 text-xs font-semibold text-slate-600">
                  Raw API document
                </summary>
                <pre className="p-4 bg-slate-950 text-slate-100 text-[11px] leading-relaxed overflow-x-auto select-all">
                  {JSON.stringify(selectedClass, null, 2)}
                </pre>
              </details>
            </div>

            <footer className="px-6 py-4 border-t border-slate-100 flex justify-end">
              <Button variant="default" size="sm" onClick={() => setSelectedClass(null)}>Close</Button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );
}
