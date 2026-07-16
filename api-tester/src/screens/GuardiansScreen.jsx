import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  HeartHandshake, Plus, Trash2, Edit2, RefreshCw, X, Check, User,
  Phone, Mail, Link2, Star, ShieldAlert, Car, KeyRound, Users
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT', 'LEGAL_GUARDIAN', 'SIBLING', 'OTHER'];

export default function GuardiansScreen({ schoolId }) {
  const toast = useToast();
  const [subTab, setSubTab] = useState('directory'); // 'directory' | 'links'

  const [guardians, setGuardians] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const emptyGuardian = { name: '', phone: '', alternatePhone: '', email: '', address: '', occupation: '' };
  const [guardianForm, setGuardianForm] = useState(emptyGuardian);
  const [editing, setEditing] = useState(null);

  const [selectedStudentId, setSelectedStudentId] = useState('');
  const emptyLink = { guardianId: '', relation: 'FATHER', primary: false, emergencyContact: false, pickupApproved: false, portalAccess: false };
  const [linkForm, setLinkForm] = useState(emptyLink);

  const fetchAll = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    try {
      const [g, s] = await Promise.all([api.guardians(schoolId), api.students(schoolId)]);
      setGuardians(g || []);
      setStudents(s || []);
      if ((s || []).length && !s.some((x) => x.id === selectedStudentId)) setSelectedStudentId(s[0].id);
      if ((g || []).length && !g.some((x) => x.id === linkForm.guardianId)) setLinkForm((f) => ({ ...f, guardianId: g[0].id }));
    } catch (e) {
      console.error(e);
      toast.error('Failed to load guardians.');
    } finally { setLoading(false); }
  }, [schoolId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const guardianById = useMemo(() => Object.fromEntries(guardians.map((g) => [g.id, g])), [guardians]);
  const selectedStudent = students.find((s) => s.id === selectedStudentId) || null;
  const studentCountFor = (gid) => students.filter((s) => (s.guardians || []).some((l) => l.guardianId === gid)).length;

  // ---- guardian CRUD ----
  const resetGuardian = () => { setEditing(null); setGuardianForm(emptyGuardian); };

  const submitGuardian = async () => {
    if (!guardianForm.name) { toast.error('Name is required.'); return; }
    setBusy(true);
    try {
      if (editing) { await api.updateGuardian(editing.id, guardianForm); toast.success('Guardian updated.'); }
      else { await api.createGuardian({ schoolId, ...guardianForm }); toast.success('Guardian created.'); }
      resetGuardian();
      fetchAll();
    } catch (e) { toast.error(e.message || 'Failed to save guardian.'); }
    finally { setBusy(false); }
  };

  const editGuardian = (g) => {
    setEditing(g);
    setGuardianForm({ name: g.name || '', phone: g.phone || '', alternatePhone: g.alternatePhone || '', email: g.email || '', address: g.address || '', occupation: g.occupation || '' });
  };

  const deleteGuardian = async (g) => {
    const linked = studentCountFor(g.id);
    if (!confirm(`Delete guardian "${g.name}"?${linked ? ` They are still linked to ${linked} student(s).` : ''}`)) return;
    try { await api.deleteGuardian(g.id); toast.success('Guardian deleted.'); fetchAll(); }
    catch (e) { toast.error(e.message); }
  };

  // ---- link management ----
  const addLink = async () => {
    if (!selectedStudentId || !linkForm.guardianId) { toast.error('Pick a student and a guardian.'); return; }
    setBusy(true);
    try {
      await api.addGuardianLink(selectedStudentId, linkForm);
      toast.success('Guardian linked.');
      setLinkForm({ ...emptyLink, guardianId: linkForm.guardianId });
      fetchAll();
    } catch (e) { toast.error(e.message || 'Failed to link guardian.'); }
    finally { setBusy(false); }
  };

  const removeLink = async (guardianId) => {
    setBusy(true);
    try { await api.removeGuardianLink(selectedStudentId, guardianId); toast.success('Guardian unlinked.'); fetchAll(); }
    catch (e) { toast.error(e.message); }
    finally { setBusy(false); }
  };

  const flagChips = (l) => (
    <div className="flex flex-wrap gap-1">
      {l.primary && <Badge color="blue">primary</Badge>}
      {l.emergencyContact && <Badge color="rose">emergency</Badge>}
      {l.pickupApproved && <Badge color="green">pickup</Badge>}
      {l.portalAccess && <Badge color="amber">portal</Badge>}
    </div>
  );

  if (!schoolId) return <Empty icon={HeartHandshake} title="Pick a school to begin" hint="Select a school in the top bar to manage guardians." />;

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex items-center justify-between bg-white px-4 py-3 rounded-xl shadow-sm shrink-0">
        <div>
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2"><HeartHandshake size={16} className="text-blue-600" /> Guardians</h2>
          <p className="text-xs text-slate-500 mt-0.5">Individual contacts, linked many-to-many to students · {guardians.length} guardian{guardians.length === 1 ? '' : 's'}</p>
        </div>
        <button onClick={fetchAll} className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold transition">
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Reload Data
        </button>
      </div>

      {/* Sub-tabs */}
      <div className="flex gap-1 border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm shrink-0">
        <button onClick={() => setSubTab('directory')} className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${subTab === 'directory' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <User size={16} /> Directory
        </button>
        <button onClick={() => setSubTab('links')} className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${subTab === 'links' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <Link2 size={16} /> Link to Students
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto grid grid-cols-1 xl:grid-cols-3 gap-4">
        {subTab === 'directory' && (
          <>
            {/* Guardian table */}
            <div className="xl:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
              <header className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h3 className="font-bold text-slate-800 text-sm">Guardian Directory</h3>
                <p className="text-[11px] text-slate-500 mt-0.5">One row per person — shared across siblings.</p>
              </header>
              <div className="flex-1 overflow-x-auto">
                {loading ? <Empty icon={RefreshCw} title="Loading..." /> : guardians.length === 0 ? (
                  <Empty icon={User} title="No guardians yet" hint="Add one from the form on the right." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Name</th>
                        <th className="px-4 py-3">Contact</th>
                        <th className="px-4 py-3">Occupation</th>
                        <th className="px-4 py-3 text-center">Linked</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {guardians.map((g) => (
                        <tr key={g.id} className="hover:bg-slate-50/50 transition">
                          <td className="px-4 py-3 font-bold text-slate-900">{g.name}</td>
                          <td className="px-4 py-3 text-slate-500">
                            {g.phone && <div className="flex items-center gap-1"><Phone size={9} /> {g.phone}</div>}
                            {g.email && <div className="flex items-center gap-1"><Mail size={9} /> {g.email}</div>}
                          </td>
                          <td className="px-4 py-3 text-slate-500">{g.occupation || '—'}</td>
                          <td className="px-4 py-3 text-center">
                            <span className="inline-flex items-center gap-1 text-slate-500"><Users size={11} /> {studentCountFor(g.id)}</span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex justify-end gap-1">
                              <button onClick={() => editGuardian(g)} className="text-slate-400 hover:text-blue-600 p-1.5 rounded-lg transition"><Edit2 size={13} /></button>
                              <button onClick={() => deleteGuardian(g)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition"><Trash2 size={13} /></button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Create / edit guardian */}
            <div className="xl:col-span-1">
              <Card title={editing ? 'Edit Guardian' : 'New Guardian'} subtitle={editing ? 'Update contact details.' : 'Add a contact person.'}>
                <div className="space-y-3">
                  <Field label="Name *"><Input value={guardianForm.name} onChange={(e) => setGuardianForm({ ...guardianForm, name: e.target.value })} placeholder="e.g. Rajesh Nair" /></Field>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Phone"><Input value={guardianForm.phone} onChange={(e) => setGuardianForm({ ...guardianForm, phone: e.target.value })} /></Field>
                    <Field label="Alt. Phone"><Input value={guardianForm.alternatePhone} onChange={(e) => setGuardianForm({ ...guardianForm, alternatePhone: e.target.value })} /></Field>
                  </div>
                  <Field label="Email"><Input value={guardianForm.email} onChange={(e) => setGuardianForm({ ...guardianForm, email: e.target.value })} /></Field>
                  <Field label="Occupation"><Input value={guardianForm.occupation} onChange={(e) => setGuardianForm({ ...guardianForm, occupation: e.target.value })} /></Field>
                  <Field label="Address"><Input value={guardianForm.address} onChange={(e) => setGuardianForm({ ...guardianForm, address: e.target.value })} /></Field>
                  <div className="pt-2 border-t border-slate-100 flex justify-end gap-2">
                    {editing && <Button variant="default" onClick={resetGuardian}>Cancel</Button>}
                    <Button variant="primary" onClick={submitGuardian} disabled={busy || !guardianForm.name}>
                      {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />} {editing ? 'Save' : 'Create'}
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </>
        )}

        {subTab === 'links' && (
          <>
            {/* Current links for selected student */}
            <div className="xl:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
              <header className="px-5 py-3 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between gap-3">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">Student's Guardians</h3>
                  <p className="text-[11px] text-slate-500 mt-0.5">Who is linked to this student, and how.</p>
                </div>
                <Select value={selectedStudentId} onChange={(e) => setSelectedStudentId(e.target.value)} className="!py-1.5 max-w-[55%]">
                  {students.length === 0 && <option value="">No students</option>}
                  {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName || ''} ({s.admissionNo})</option>)}
                </Select>
              </header>
              <div className="flex-1 overflow-x-auto">
                {!selectedStudent ? (
                  <Empty icon={User} title="No student selected" hint="Enroll students first, then link guardians." />
                ) : (selectedStudent.guardians || []).length === 0 ? (
                  <Empty icon={Link2} title="No guardians linked" hint="Use the panel on the right to link one." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Guardian</th>
                        <th className="px-4 py-3">Relation</th>
                        <th className="px-4 py-3">Flags</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {(selectedStudent.guardians || []).map((l) => {
                        const g = guardianById[l.guardianId];
                        return (
                          <tr key={l.guardianId} className="hover:bg-slate-50/50 transition">
                            <td className="px-4 py-3">
                              <div className="font-bold text-slate-900">{g?.name || '(unknown / deleted)'}</div>
                              <div className="text-[10px] text-slate-400">{g?.phone || ''}</div>
                            </td>
                            <td className="px-4 py-3"><Badge color="slate">{l.relation}</Badge></td>
                            <td className="px-4 py-3">{flagChips(l)}</td>
                            <td className="px-4 py-3 text-right">
                              <button onClick={() => removeLink(l.guardianId)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition" title="Unlink"><Trash2 size={13} /></button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Add link */}
            <div className="xl:col-span-1">
              <Card title="Link a Guardian" subtitle="Attach a guardian to the selected student with a role + flags.">
                <div className="space-y-3">
                  <Field label="Guardian *">
                    <Select value={linkForm.guardianId} onChange={(e) => setLinkForm({ ...linkForm, guardianId: e.target.value })}>
                      {guardians.length === 0 && <option value="">Create a guardian first</option>}
                      {guardians.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
                    </Select>
                  </Field>
                  <Field label="Relation *">
                    <Select value={linkForm.relation} onChange={(e) => setLinkForm({ ...linkForm, relation: e.target.value })}>
                      {RELATIONS.map((r) => <option key={r} value={r}>{r.replace('_', ' ')}</option>)}
                    </Select>
                  </Field>
                  <div className="grid grid-cols-2 gap-2 pt-1">
                    {[
                      ['primary', 'Primary', Star],
                      ['emergencyContact', 'Emergency', ShieldAlert],
                      ['pickupApproved', 'Pickup OK', Car],
                      ['portalAccess', 'Portal login', KeyRound],
                    ].map(([key, label, Icon]) => (
                      <label key={key} className={`flex items-center gap-2 px-2.5 py-2 rounded-lg border cursor-pointer text-xs font-medium transition ${linkForm[key] ? 'border-blue-300 bg-blue-50 text-blue-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                        <input type="checkbox" checked={linkForm[key]} onChange={(e) => setLinkForm({ ...linkForm, [key]: e.target.checked })} className="accent-blue-600" />
                        <Icon size={13} /> {label}
                      </label>
                    ))}
                  </div>
                  <div className="pt-2 border-t border-slate-100 flex justify-end">
                    <Button variant="primary" onClick={addLink} disabled={busy || !selectedStudentId || !linkForm.guardianId}>
                      {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />} Link Guardian
                    </Button>
                  </div>
                  <p className="text-[11px] text-slate-400">Re-linking the same guardian updates their flags.</p>
                </div>
              </Card>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
