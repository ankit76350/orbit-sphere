import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  UserPlus, Phone, Mail, Plus, Trash2, RefreshCw, X, Check,
  ArrowRight, FileText, GraduationCap, User, ClipboardList, History, Clock
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

const INQUIRY_STAGES = ['INQUIRY', 'COUNSELING', 'VISIT', 'DOCUMENT_VERIFICATION', 'ADMISSION', 'CLOSED'];
const ADMISSION_STAGES = ['PENDING', 'APPROVED', 'REJECTED', 'CONFIRMED'];
const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT', 'LEGAL_GUARDIAN', 'SIBLING', 'OTHER'];

const inquiryColor = (s) => ({
  INQUIRY: 'slate', COUNSELING: 'blue', VISIT: 'blue',
  DOCUMENT_VERIFICATION: 'amber', ADMISSION: 'green', CLOSED: 'slate',
}[s] || 'slate');
const admissionColor = (s) => ({
  PENDING: 'amber', APPROVED: 'blue', REJECTED: 'rose', CONFIRMED: 'green',
}[s] || 'slate');

export default function CrmScreen({ schoolId, year, staff = [] }) {
  const toast = useToast();
  const [subTab, setSubTab] = useState('inquiries'); // 'inquiries' | 'admissions'

  const [inquiries, setInquiries] = useState([]);
  const [admissions, setAdmissions] = useState([]);
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const emptyInquiry = {
    studentName: '',
    source: 'WALK_IN', counselorId: '',
    note: '', nextFollowUp: '', // seed the first follow-up entry
    guardians: [{ name: '', relation: 'MOTHER', phone: '', email: '', address: '', occupation: '' }],
  };
  const [inquiryForm, setInquiryForm] = useState(emptyInquiry);

  // Follow-up / status-change modal
  const [followUpModal, setFollowUpModal] = useState(null); // the inquiry being worked
  const [followUpForm, setFollowUpForm] = useState({ status: 'INQUIRY', note: '', nextFollowUp: '' });

  const setGuardian = (idx, patch) =>
    setInquiryForm((f) => ({ ...f, guardians: f.guardians.map((g, i) => (i === idx ? { ...g, ...patch } : g)) }));
  const addGuardianRow = () =>
    setInquiryForm((f) => ({ ...f, guardians: [...f.guardians, { name: '', relation: 'FATHER', phone: '', email: '', address: '', occupation: '' }] }));
  const removeGuardianRow = (idx) =>
    setInquiryForm((f) => ({ ...f, guardians: f.guardians.filter((_, i) => i !== idx) }));

  const [admissionForm, setAdmissionForm] = useState({ inquiryId: '', documents: '', admissionDate: new Date().toISOString().slice(0, 10) });

  const [convert, setConvert] = useState(null); // admission being converted
  const [convertForm, setConvertForm] = useState(null);

  const fetchAll = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    try {
      const [inq, adm, cls] = await Promise.all([
        api.inquiries(schoolId),
        year ? api.admissionsByYear(schoolId, year) : api.admissions(schoolId),
        api.classes(schoolId),
      ]);
      setInquiries(inq || []);
      setAdmissions(adm || []);
      setClasses(cls || []);
    } catch (e) {
      console.error(e);
      toast.error('Failed to load CRM data.');
    } finally {
      setLoading(false);
    }
  }, [schoolId, year, toast]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  // ---- helpers ----
  const inquiryById = useMemo(() => Object.fromEntries(inquiries.map((i) => [i.id, i])), [inquiries]);
  const staffName = (id) => {
    const s = staff.find((x) => x.id === id);
    return s ? `${s.firstName || ''} ${s.lastName || ''}`.trim() || s.employeeId : '—';
  };
  const stageCounts = useMemo(() => {
    const c = {};
    INQUIRY_STAGES.forEach((s) => (c[s] = 0));
    inquiries.forEach((i) => { if (c[i.status] != null) c[i.status]++; });
    return c;
  }, [inquiries]);

  // ---- inquiry actions ----
  const submitInquiry = async () => {
    const guardians = (inquiryForm.guardians || []).filter((g) => g.name && g.name.trim());
    if (!inquiryForm.studentName && guardians.length === 0) {
      toast.error('Enter a student name or at least one guardian.');
      return;
    }
    setBusy(true);
    try {
      // Seed the follow-up timeline with the first entry.
      const followUps = [{ status: 'INQUIRY', note: inquiryForm.note || null, nextFollowUp: inquiryForm.nextFollowUp || null, counselorId: inquiryForm.counselorId || null }];
      await api.createInquiry({
        schoolId,
        status: 'INQUIRY',
        studentName: inquiryForm.studentName,
        source: inquiryForm.source,
        counselorId: inquiryForm.counselorId || null,
        guardians,
        followUps,
      });
      toast.success('Inquiry created.');
      setInquiryForm(emptyInquiry);
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Failed to create inquiry.');
    } finally { setBusy(false); }
  };

  const openFollowUp = (inq) => {
    setFollowUpModal(inq);
    setFollowUpForm({ status: inq.status || 'INQUIRY', note: '', nextFollowUp: '', counselorId: inq.counselorId || '' });
  };

  const submitFollowUp = async () => {
    setBusy(true);
    try {
      await api.recordInquiryFollowUp(followUpModal.id, {
        status: followUpForm.status,
        note: followUpForm.note || null,
        nextFollowUp: followUpForm.nextFollowUp || null,
        counselorId: followUpForm.counselorId || null,
      });
      toast.success(`Logged → ${followUpForm.status}`);
      setFollowUpModal(null);
      fetchAll();
    } catch (e) { toast.error(e.message || 'Failed to record follow-up.'); }
    finally { setBusy(false); }
  };

  const latestFollowUp = (inq) => (inq.followUps && inq.followUps.length ? inq.followUps[inq.followUps.length - 1] : null);

  const deleteInquiry = async (inq) => {
    if (!confirm(`Delete inquiry for ${inq.studentName || (inq.guardians && inq.guardians[0] && inq.guardians[0].name) || 'this lead'}?`)) return;
    try { await api.deleteInquiry(inq.id); toast.success('Inquiry deleted.'); fetchAll(); }
    catch (e) { toast.error(e.message); }
  };

  // Jump to Admissions tab with this inquiry preselected in the create form.
  const startAdmission = (inq) => {
    setAdmissionForm({ inquiryId: inq.id, documents: '', admissionDate: new Date().toISOString().slice(0, 10) });
    setSubTab('admissions');
  };

  // ---- admission actions ----
  const submitAdmission = async () => {
    if (!year) { toast.error('Select an academic year in the top bar first.'); return; }
    setBusy(true);
    try {
      await api.createAdmission({
        schoolId,
        academicYear: year,
        inquiryId: admissionForm.inquiryId || null,
        status: 'PENDING',
        documents: admissionForm.documents ? admissionForm.documents.split(',').map((d) => d.trim()).filter(Boolean) : [],
        admissionDate: admissionForm.admissionDate,
      });
      toast.success('Admission created (inquiry auto-advanced to ADMISSION).');
      setAdmissionForm({ inquiryId: '', documents: '', admissionDate: new Date().toISOString().slice(0, 10) });
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Failed to create admission.');
    } finally { setBusy(false); }
  };

  const changeAdmissionStatus = async (adm, status) => {
    try {
      await api.updateAdmission(adm.id, { status });
      toast.success(`Admission → ${status}`);
      fetchAll();
    } catch (e) { toast.error(e.message || 'Update failed.'); }
  };

  const deleteAdmission = async (adm) => {
    if (!confirm('Delete this admission?')) return;
    try { await api.deleteAdmission(adm.id); toast.success('Admission deleted.'); fetchAll(); }
    catch (e) { toast.error(e.message); }
  };

  const openConvert = (adm) => {
    const inq = adm.inquiryId ? inquiryById[adm.inquiryId] : null;
    const nameParts = (inq?.studentName || '').split(' ');
    setConvert(adm);
    setConvertForm({
      admissionNo: '',
      firstName: nameParts[0] || '',
      lastName: nameParts.slice(1).join(' ') || '',
      dob: '',
      gender: 'MALE',
      bloodGroup: '',
      classDocId: '',
      sectionId: '',
      rollNo: '',
    });
  };

  const submitConvert = async () => {
    if (!convertForm.admissionNo || !convertForm.firstName) {
      toast.error('Admission No. and First Name are required.');
      return;
    }
    setBusy(true);
    try {
      const payload = {
        admissionNo: convertForm.admissionNo,
        firstName: convertForm.firstName,
        lastName: convertForm.lastName || null,
        dob: convertForm.dob || null,
        gender: convertForm.gender,
        bloodGroup: convertForm.bloodGroup || null,
        status: 'ACTIVE',
        currentAcademicRecord: {
          classDocId: convertForm.classDocId || null,
          sectionId: convertForm.sectionId || null,
          rollNo: convertForm.rollNo || null,
        },
      };
      const student = await api.convertAdmission(convert.id, payload);
      toast.success(`Enrolled ${student.firstName} — admission CONFIRMED.`);
      setConvert(null);
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Conversion failed.');
    } finally { setBusy(false); }
  };

  if (!schoolId) {
    return <Empty icon={UserPlus} title="Pick a school to begin" hint="Select a school in the top bar to manage the admissions pipeline." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex items-center justify-between bg-white px-4 py-3 rounded-xl shadow-sm shrink-0">
        <div>
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
            <UserPlus size={16} className="text-blue-600" /> CRM · Admissions Funnel
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">Lead → Admission → Enrolled Student{year ? ` · ${year}` : ''}</p>
        </div>
        <button onClick={fetchAll} className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold transition">
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Reload Data
        </button>
      </div>

      {/* Funnel strip — the flow, visualized */}
      <div className="bg-white rounded-xl shadow-sm px-4 py-3 shrink-0 flex items-center gap-1 overflow-x-auto">
        {INQUIRY_STAGES.map((s, i) => (
          <div key={s} className="flex items-center gap-1 shrink-0">
            <div className={`px-3 py-1.5 rounded-lg text-center border ${stageCounts[s] > 0 ? 'border-blue-200 bg-blue-50' : 'border-slate-100 bg-slate-50'}`}>
              <div className="text-[10px] font-semibold text-slate-500 uppercase tracking-wide">{s.replace('_', ' ')}</div>
              <div className="text-sm font-bold text-slate-800">{stageCounts[s]}</div>
            </div>
            {i < INQUIRY_STAGES.length - 1 && <ArrowRight size={13} className="text-slate-300 shrink-0" />}
          </div>
        ))}
        <ArrowRight size={13} className="text-slate-300 shrink-0" />
        <div className="px-3 py-1.5 rounded-lg text-center border border-emerald-200 bg-emerald-50 shrink-0">
          <div className="text-[10px] font-semibold text-emerald-600 uppercase tracking-wide flex items-center gap-1"><GraduationCap size={11} /> Enrolled</div>
          <div className="text-sm font-bold text-emerald-700">{admissions.filter((a) => a.status === 'CONFIRMED').length}</div>
        </div>
      </div>

      {/* Sub-tabs */}
      <div className="flex gap-1 border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm shrink-0">
        <button onClick={() => setSubTab('inquiries')} className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${subTab === 'inquiries' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <ClipboardList size={16} /> Inquiries ({inquiries.length})
        </button>
        <button onClick={() => setSubTab('admissions')} className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${subTab === 'admissions' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          <FileText size={16} /> Admissions ({admissions.length})
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto grid grid-cols-1 xl:grid-cols-3 gap-4">
        {subTab === 'inquiries' && (
          <>
            {/* Inquiries table */}
            <div className="xl:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
              <header className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h3 className="font-bold text-slate-800 text-sm">Leads / Inquiries</h3>
                <p className="text-[11px] text-slate-500 mt-0.5">Move a lead along the pipeline, then create an admission from it.</p>
              </header>
              <div className="flex-1 overflow-x-auto">
                {loading ? <Empty icon={RefreshCw} title="Loading..." /> : inquiries.length === 0 ? (
                  <Empty icon={ClipboardList} title="No inquiries yet" hint="Create one from the form on the right." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Prospect</th>
                        <th className="px-4 py-3">Contact</th>
                        <th className="px-4 py-3">Counselor</th>
                        <th className="px-4 py-3">Stage</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {inquiries.map((inq) => (
                        <tr key={inq.id} className="hover:bg-slate-50/50 transition">
                          <td className="px-4 py-3">
                            <div className="font-bold text-slate-900">{inq.studentName || '—'}</div>
                            <div className="text-[10px] text-slate-400">
                              {inq.guardians && inq.guardians.length
                                ? `${inq.guardians[0].name}${inq.guardians[0].relation ? ` (${inq.guardians[0].relation.replace('_', ' ')})` : ''}${inq.guardians.length > 1 ? ` +${inq.guardians.length - 1} more` : ''}`
                                : 'No guardian'}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-slate-500">
                            {(() => { const g0 = inq.guardians && inq.guardians[0]; return g0 ? (
                              <>
                                {g0.phone && <div className="flex items-center gap-1"><Phone size={9} /> {g0.phone}</div>}
                                {g0.email && <div className="flex items-center gap-1"><Mail size={9} /> {g0.email}</div>}
                              </>
                            ) : <span className="text-slate-300">—</span>; })()}
                          </td>
                          <td className="px-4 py-3 text-slate-500">{inq.counselorId ? staffName(inq.counselorId) : '—'}</td>
                          <td className="px-4 py-3">
                            <Badge color={inquiryColor(inq.status)}>{(inq.status || '').replace('_', ' ')}</Badge>
                            {latestFollowUp(inq)?.nextFollowUp && (
                              <div className="text-[9px] text-slate-400 mt-1 flex items-center gap-0.5">
                                <Clock size={8} /> next {new Date(latestFollowUp(inq).nextFollowUp).toLocaleDateString()}
                              </div>
                            )}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex justify-end gap-1 items-center">
                              <button onClick={() => openFollowUp(inq)} className="flex items-center gap-0.5 bg-slate-50 text-slate-600 border border-slate-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-slate-100 transition" title="Log follow-up / change stage">
                                <History size={10} /> Log
                              </button>
                              <button onClick={() => startAdmission(inq)} className="flex items-center gap-0.5 bg-blue-50 text-blue-700 border border-blue-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-blue-100 transition" title="Create admission from this lead">
                                <ArrowRight size={10} /> Admit
                              </button>
                              <button onClick={() => deleteInquiry(inq)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition"><Trash2 size={13} /></button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Create inquiry */}
            <div className="xl:col-span-1">
              <Card title="New Inquiry" subtitle="Capture a fresh lead (walk-in / call / online).">
                <div className="space-y-3">
                  <Field label="Student Name"><Input value={inquiryForm.studentName} onChange={(e) => setInquiryForm({ ...inquiryForm, studentName: e.target.value })} placeholder="e.g. Aarav Nair" /></Field>

                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-600">Guardians</span>
                      <button onClick={addGuardianRow} className="text-[11px] font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"><Plus size={11} /> Add guardian</button>
                    </div>
                    {inquiryForm.guardians.map((g, idx) => (
                      <div key={idx} className="border border-slate-200 rounded-lg p-2.5 space-y-2 bg-slate-50/40">
                        <div className="flex items-center gap-2">
                          <Input value={g.name} onChange={(e) => setGuardian(idx, { name: e.target.value })} placeholder="Full name" className="flex-1" />
                          <Select value={g.relation} onChange={(e) => setGuardian(idx, { relation: e.target.value })} className="!py-1.5 w-32 shrink-0">
                            {RELATIONS.map((r) => <option key={r} value={r}>{r.replace('_', ' ')}</option>)}
                          </Select>
                          {inquiryForm.guardians.length > 1 && (
                            <button onClick={() => removeGuardianRow(idx)} className="text-slate-300 hover:text-rose-600 p-1 shrink-0" title="Remove"><X size={14} /></button>
                          )}
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <Input value={g.phone} onChange={(e) => setGuardian(idx, { phone: e.target.value })} placeholder="Phone" />
                          <Input value={g.email} onChange={(e) => setGuardian(idx, { email: e.target.value })} placeholder="Email" />
                          <Input value={g.occupation} onChange={(e) => setGuardian(idx, { occupation: e.target.value })} placeholder="Occupation" />
                          <Input value={g.address} onChange={(e) => setGuardian(idx, { address: e.target.value })} placeholder="Address" />
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Source">
                      <Select value={inquiryForm.source} onChange={(e) => setInquiryForm({ ...inquiryForm, source: e.target.value })}>
                        {['WALK_IN', 'PHONE', 'ONLINE', 'REFERRAL', 'OTHER'].map((s) => <option key={s} value={s}>{s}</option>)}
                      </Select>
                    </Field>
                    <Field label="Counselor">
                      <Select value={inquiryForm.counselorId} onChange={(e) => setInquiryForm({ ...inquiryForm, counselorId: e.target.value })}>
                        <option value="">— none —</option>
                        {staff.map((s) => <option key={s.id} value={s.id}>{`${s.firstName || ''} ${s.lastName || ''}`.trim() || s.employeeId}</option>)}
                      </Select>
                    </Field>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Initial Note"><Input value={inquiryForm.note} onChange={(e) => setInquiryForm({ ...inquiryForm, note: e.target.value })} placeholder="Interested in Grade 6…" /></Field>
                    <Field label="Next Follow-up"><Input type="date" value={inquiryForm.nextFollowUp} onChange={(e) => setInquiryForm({ ...inquiryForm, nextFollowUp: e.target.value })} /></Field>
                  </div>
                  <div className="pt-2 border-t border-slate-100 flex justify-end">
                    <Button variant="primary" onClick={submitInquiry} disabled={busy}>
                      {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />} Create Inquiry
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </>
        )}

        {subTab === 'admissions' && (
          <>
            {/* Admissions table */}
            <div className="xl:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
              <header className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h3 className="font-bold text-slate-800 text-sm">Admissions {year ? `· ${year}` : ''}</h3>
                <p className="text-[11px] text-slate-500 mt-0.5">Approve, then convert to an enrolled student.</p>
              </header>
              <div className="flex-1 overflow-x-auto">
                {loading ? <Empty icon={RefreshCw} title="Loading..." /> : admissions.length === 0 ? (
                  <Empty icon={FileText} title="No admissions yet" hint="Create one from an inquiry (Inquiries tab → Admit) or the form here." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Prospect (from inquiry)</th>
                        <th className="px-4 py-3">Docs</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3">Student</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {admissions.map((adm) => {
                        const inq = adm.inquiryId ? inquiryById[adm.inquiryId] : null;
                        const converted = adm.studentId && adm.studentId.length > 0;
                        return (
                          <tr key={adm.id} className="hover:bg-slate-50/50 transition">
                            <td className="px-4 py-3">
                              <div className="font-bold text-slate-900">{inq?.studentName || '(no linked inquiry)'}</div>
                              <div className="text-[10px] text-slate-400">{adm.admissionDate || '—'}</div>
                            </td>
                            <td className="px-4 py-3 text-slate-500">{(adm.documents || []).length}</td>
                            <td className="px-4 py-3">
                              <Select value={adm.status} onChange={(e) => changeAdmissionStatus(adm, e.target.value)} disabled={converted} className="!py-1 !text-[11px]">
                                {ADMISSION_STAGES.map((s) => <option key={s} value={s}>{s}</option>)}
                              </Select>
                            </td>
                            <td className="px-4 py-3">
                              {converted
                                ? <Badge color="green">enrolled</Badge>
                                : <span className="text-slate-300">—</span>}
                            </td>
                            <td className="px-4 py-3">
                              <div className="flex justify-end gap-1 items-center">
                                {!converted && (
                                  <button onClick={() => openConvert(adm)} className="flex items-center gap-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-emerald-100 transition" title="Convert to enrolled student">
                                    <GraduationCap size={10} /> Convert
                                  </button>
                                )}
                                <button onClick={() => deleteAdmission(adm)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition"><Trash2 size={13} /></button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Create admission */}
            <div className="xl:col-span-1">
              <Card title="New Admission" subtitle={year ? `Formal application for ${year}.` : 'Select an academic year first.'}>
                <div className="space-y-3">
                  <Field label="From Inquiry" hint="Linking auto-advances the inquiry to ADMISSION.">
                    <Select value={admissionForm.inquiryId} onChange={(e) => setAdmissionForm({ ...admissionForm, inquiryId: e.target.value })}>
                      <option value="">— none (standalone) —</option>
                      {inquiries.map((i) => <option key={i.id} value={i.id}>{i.studentName || (i.guardians && i.guardians[0] && i.guardians[0].name) || i.id}</option>)}
                    </Select>
                  </Field>
                  <Field label="Admission Date"><Input type="date" value={admissionForm.admissionDate} onChange={(e) => setAdmissionForm({ ...admissionForm, admissionDate: e.target.value })} /></Field>
                  <Field label="Documents" hint="Comma-separated file names."><Input value={admissionForm.documents} onChange={(e) => setAdmissionForm({ ...admissionForm, documents: e.target.value })} placeholder="birth-cert.pdf, report-card.pdf" /></Field>
                  <div className="pt-2 border-t border-slate-100 flex justify-end">
                    <Button variant="primary" onClick={submitAdmission} disabled={busy || !year}>
                      {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />} Create Admission
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </>
        )}
      </div>

      {/* CONVERT MODAL */}
      {convert && convertForm && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm flex items-center gap-1.5"><GraduationCap size={15} /> Convert to Enrolled Student</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">Creates the Student record and marks this admission CONFIRMED.</p>
              </div>
              <button onClick={() => setConvert(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 space-y-3 overflow-y-auto">
              <div className="grid grid-cols-2 gap-3">
                <Field label="Admission No. *"><Input value={convertForm.admissionNo} onChange={(e) => setConvertForm({ ...convertForm, admissionNo: e.target.value })} placeholder="ADM-2026-0007" /></Field>
                <Field label="Roll No."><Input value={convertForm.rollNo} onChange={(e) => setConvertForm({ ...convertForm, rollNo: e.target.value })} /></Field>
                <Field label="First Name *"><Input value={convertForm.firstName} onChange={(e) => setConvertForm({ ...convertForm, firstName: e.target.value })} /></Field>
                <Field label="Last Name"><Input value={convertForm.lastName} onChange={(e) => setConvertForm({ ...convertForm, lastName: e.target.value })} /></Field>
                <Field label="Date of Birth"><Input type="date" value={convertForm.dob} onChange={(e) => setConvertForm({ ...convertForm, dob: e.target.value })} /></Field>
                <Field label="Gender">
                  <Select value={convertForm.gender} onChange={(e) => setConvertForm({ ...convertForm, gender: e.target.value })}>
                    {['MALE', 'FEMALE', 'OTHER'].map((g) => <option key={g} value={g}>{g}</option>)}
                  </Select>
                </Field>
              </div>
              <p className="text-[11px] text-slate-400 bg-slate-50 border border-slate-100 rounded-lg px-3 py-2">
                Link guardians after enrollment in the <span className="font-semibold text-slate-500">Guardians</span> tab (many-to-many, with roles &amp; flags).
              </p>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Class">
                  <Select value={convertForm.classDocId} onChange={(e) => setConvertForm({ ...convertForm, classDocId: e.target.value })}>
                    <option value="">— none —</option>
                    {classes.map((c) => <option key={c.id} value={c.id}>{c.name || c.className || c.id}</option>)}
                  </Select>
                </Field>
                <Field label="Section (id)"><Input value={convertForm.sectionId} onChange={(e) => setConvertForm({ ...convertForm, sectionId: e.target.value })} placeholder="e.g. A" /></Field>
              </div>
            </div>
            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 shrink-0 flex justify-end gap-2">
              <Button variant="default" onClick={() => setConvert(null)}>Cancel</Button>
              <Button variant="primary" onClick={submitConvert} disabled={busy || !convertForm.admissionNo || !convertForm.firstName}>
                {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />} Enroll Student
              </Button>
            </footer>
          </div>
        </div>
      )}

      {/* FOLLOW-UP / STATUS-CHANGE MODAL */}
      {followUpModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm flex items-center gap-1.5"><History size={15} /> Follow-up Timeline</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">{followUpModal.studentName || (followUpModal.guardians && followUpModal.guardians[0] && followUpModal.guardians[0].name) || 'Lead'}</p>
              </div>
              <button onClick={() => setFollowUpModal(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>

            {/* Existing timeline */}
            <div className="px-5 py-4 overflow-y-auto min-h-0 flex-1">
              {(followUpModal.followUps || []).length === 0 ? (
                <Empty icon={Clock} title="No follow-ups yet" hint="Log the first one below." />
              ) : (
                <ol className="relative border-l border-slate-200 ml-2 space-y-4">
                  {(followUpModal.followUps || []).map((f, i) => (
                    <li key={i} className="ml-4">
                      <div className="absolute -left-1.5 mt-1 w-3 h-3 rounded-full bg-blue-500 border-2 border-white" />
                      <div className="flex items-center gap-2">
                        <Badge color={inquiryColor(f.status)}>{(f.status || '').replace('_', ' ')}</Badge>
                        <span className="text-[10px] text-slate-400">{f.recordedAt ? new Date(f.recordedAt).toLocaleString() : ''}</span>
                      </div>
                      {f.note && <div className="text-xs text-slate-700 mt-1">{f.note}</div>}
                      <div className="text-[10px] text-slate-400 mt-0.5 flex flex-wrap gap-x-3">
                        {f.nextFollowUp && <span className="flex items-center gap-0.5"><Clock size={9} /> next {new Date(f.nextFollowUp).toLocaleDateString()}</span>}
                        {f.counselorId && <span className="flex items-center gap-0.5"><User size={9} /> {staffName(f.counselorId)}</span>}
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </div>

            {/* New follow-up form */}
            <div className="px-5 py-4 border-t border-slate-100 bg-slate-50/50 shrink-0 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <Field label="New Stage *">
                  <Select value={followUpForm.status} onChange={(e) => setFollowUpForm({ ...followUpForm, status: e.target.value })}>
                    {INQUIRY_STAGES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                  </Select>
                </Field>
                <Field label="Next Follow-up">
                  <Input type="date" value={followUpForm.nextFollowUp} onChange={(e) => setFollowUpForm({ ...followUpForm, nextFollowUp: e.target.value })} />
                </Field>
              </div>
              <Field label="Handled by">
                <Select value={followUpForm.counselorId} onChange={(e) => setFollowUpForm({ ...followUpForm, counselorId: e.target.value })}>
                  <option value="">— unassigned —</option>
                  {staff.map((s) => <option key={s.id} value={s.id}>{`${s.firstName || ''} ${s.lastName || ''}`.trim() || s.employeeId}</option>)}
                </Select>
              </Field>
              <Field label="Note">
                <Input value={followUpForm.note} onChange={(e) => setFollowUpForm({ ...followUpForm, note: e.target.value })} placeholder="What happened on this touch-point?" />
              </Field>
              <div className="flex justify-end gap-2">
                <Button variant="default" onClick={() => setFollowUpModal(null)}>Close</Button>
                <Button variant="primary" onClick={submitFollowUp} disabled={busy}>
                  {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />} Log follow-up
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
