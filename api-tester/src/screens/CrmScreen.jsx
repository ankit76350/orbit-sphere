import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  UserPlus, Phone, Mail, Plus, Trash2, RefreshCw, X, Check,
  ArrowRight, FileText, GraduationCap, User, ClipboardList, History, Clock
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

const INQUIRY_STAGES = ['INQUIRY', 'COUNSELING', 'VISIT', 'ADMITTED', 'LOST'];
const ADMISSION_STAGES = ['PENDING', 'APPROVED', 'REJECTED', 'CONFIRMED'];
const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT', 'LEGAL_GUARDIAN', 'SIBLING', 'OTHER'];

const inquiryColor = (s) => ({
  INQUIRY: 'slate', COUNSELING: 'blue', VISIT: 'amber',
  ADMITTED: 'green', LOST: 'rose',
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
    schoolId: schoolId || '', studentName: '', status: 'INQUIRY',
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

  const emptyAdmission = {
    schoolId: schoolId || '',
    inquiryId: '', documents: 'birth-certificate.pdf, report-card.pdf', admissionDate: new Date().toISOString().slice(0, 10),
    studentName: 'Lucas Johnson', dob: '2015-06-19', gender: 'MALE', status: 'PENDING',
    guardians: [{ name: 'Priya Sharma', relation: 'MOTHER', phone: '+61-400-555-666', email: 'priya@example.com', address: '9 Oak Ave', occupation: 'Teacher' }],
  };
  const [admissionForm, setAdmissionForm] = useState(emptyAdmission);
  const setAdmGuardian = (idx, patch) =>
    setAdmissionForm((f) => ({ ...f, guardians: f.guardians.map((g, i) => (i === idx ? { ...g, ...patch } : g)) }));
  const addAdmGuardian = () =>
    setAdmissionForm((f) => ({ ...f, guardians: [...f.guardians, { name: '', relation: 'FATHER', phone: '', email: '', address: '', occupation: '' }] }));
  const removeAdmGuardian = (idx) =>
    setAdmissionForm((f) => ({ ...f, guardians: f.guardians.filter((_, i) => i !== idx) }));
  // Selecting an inquiry pulls its applicant snapshot into the admission form; clearing it = direct admission.
  const pickAdmissionInquiry = (inquiryId) => {
    if (!inquiryId) { setAdmissionForm((f) => ({ ...f, inquiryId: '', studentName: '', guardians: [] })); return; }
    const inq = inquiryById[inquiryId];
    setAdmissionForm((f) => ({
      ...f,
      inquiryId,
      studentName: inq?.studentName || '',
      guardians: (inq?.guardians || []).map((g) => ({ ...g })),
    }));
  };

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
  useEffect(() => {
    setInquiryForm((current) => ({ ...current, schoolId: schoolId || '' }));
    setAdmissionForm((current) => ({ ...current, schoolId: schoolId || '' }));
  }, [schoolId]);

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
      const followUps = [{ status: inquiryForm.status || null, note: inquiryForm.note || null, nextFollowUp: inquiryForm.nextFollowUp || null, counselorId: inquiryForm.counselorId || null }];
      await api.createInquiry({
        schoolId: inquiryForm.schoolId,
        status: inquiryForm.status || null,
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
    setAdmissionForm({
      ...emptyAdmission,
      inquiryId: inq.id,
      studentName: inq.studentName || '',
      guardians: (inq.guardians || []).map((g) => ({ ...g })),
    });
    setSubTab('admissions');
  };

  // ---- admission actions ----
  const submitAdmission = async () => {
    if (!admissionForm.inquiryId && (!admissionForm.studentName || !admissionForm.studentName.trim())) {
      toast.error('Student Name is required for direct admission.');
      return;
    }
    setBusy(true);
    try {
      await api.createAdmission({
        schoolId: admissionForm.schoolId,
        inquiryId: admissionForm.inquiryId || null,
        studentName: admissionForm.studentName || null,
        dob: admissionForm.dob || null,
        gender: admissionForm.gender || null,
        guardians: (admissionForm.guardians || []).filter((g) => g.name && g.name.trim()),
        status: admissionForm.status || null,
        documents: admissionForm.documents ? admissionForm.documents.split(',').map((d) => d.trim()).filter(Boolean) : [],
        admissionDate: admissionForm.admissionDate || null,
      });
      toast.success(admissionForm.inquiryId ? 'Admission created (inquiry auto-advanced to ADMITTED).' : 'Direct admission created.');
      setAdmissionForm(emptyAdmission);
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
    setConvert(adm);
    setConvertForm({
      admissionNo: '',
      name: adm.studentName || '',
      dob: adm.dob || '',
      gender: adm.gender || 'MALE',
      bloodGroup: '',
      photoUrl: '', walletId: '', medicalRecordId: '', status: 'ACTIVE', admissionDate: adm.admissionDate || '',
      admissionId: adm.id, schoolId, academicYear: year || '',
      guardians: [],
      currentAcademicRecord: {
        academicYear: '', studentNo: '', rollNo: '', classDocId: '', sectionNo: '', hostelRoomNo: '', status: '',
      },
    });
  };

  const submitConvert = async () => {
    setBusy(true);
    try {
      const payload = {
        admissionId: convertForm.admissionId || null,
        schoolId: convertForm.schoolId || null,
        academicYear: convertForm.academicYear || null,
        admissionNo: convertForm.admissionNo || null,
        name: convertForm.name || null,
        dob: convertForm.dob || null,
        gender: convertForm.gender || null,
        bloodGroup: convertForm.bloodGroup || null,
        photoUrl: convertForm.photoUrl || null,
        walletId: convertForm.walletId || null,
        medicalRecordId: convertForm.medicalRecordId || null,
        status: convertForm.status || null,
        admissionDate: convertForm.admissionDate || null,
        guardians: convertForm.guardians.map((guardian) => ({ ...guardian, guardianId: guardian.guardianId || null, relation: guardian.relation || null })),
        currentAcademicRecord: Object.values(convertForm.currentAcademicRecord).some(Boolean)
          ? Object.fromEntries(Object.entries(convertForm.currentAcademicRecord).map(([key, value]) => [key, value || null]))
          : null,
      };
      const student = await api.convertAdmission(convert.id, payload);
      toast.success(`Enrolled ${student.name} — admission CONFIRMED.`);
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
                  <Field label="School ID" apiName="schoolId" required><Input value={inquiryForm.schoolId} onChange={(e) => setInquiryForm({ ...inquiryForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                  <Field label="Student Name" apiName="studentName" required={false}><Input value={inquiryForm.studentName} onChange={(e) => setInquiryForm({ ...inquiryForm, studentName: e.target.value })} placeholder="e.g. Aarav Nair" /></Field>

                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-600">Guardians <code className="font-mono text-[10px] font-medium text-slate-400">guardians[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
                      <button onClick={addGuardianRow} className="text-[11px] font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"><Plus size={11} /> Add guardian</button>
                    </div>
                    {inquiryForm.guardians.map((g, idx) => (
                      <div key={idx} className="border border-slate-200 rounded-lg p-2.5 space-y-2 bg-slate-50/40">
                        <div className="flex items-center gap-2">
                          <Input value={g.name} onChange={(e) => setGuardian(idx, { name: e.target.value })} placeholder="name (required)" className="flex-1" />
                          <Select value={g.relation} onChange={(e) => setGuardian(idx, { relation: e.target.value })} className="!py-1.5 w-32 shrink-0">
                            <option value="">— omitted —</option>
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
                    <Field label="Source" apiName="source" required={false}>
                      <Select value={inquiryForm.source} onChange={(e) => setInquiryForm({ ...inquiryForm, source: e.target.value })}>
                        {['WALK_IN', 'PHONE', 'ONLINE', 'REFERRAL', 'OTHER'].map((s) => <option key={s} value={s}>{s}</option>)}
                      </Select>
                    </Field>
                    <Field label="Counselor" apiName="counselorId" required={false}>
                      <Select value={inquiryForm.counselorId} onChange={(e) => setInquiryForm({ ...inquiryForm, counselorId: e.target.value })}>
                        <option value="">— none —</option>
                        {staff.map((s) => <option key={s.id} value={s.id}>{`${s.firstName || ''} ${s.lastName || ''}`.trim() || s.employeeId}</option>)}
                      </Select>
                    </Field>
                  </div>
                  <Field label="Initial Status" apiName="status" required={false}>
                    <Select value={inquiryForm.status} onChange={(e) => setInquiryForm({ ...inquiryForm, status: e.target.value })}><option value="">— omitted —</option>{INQUIRY_STAGES.map((status) => <option key={status} value={status}>{status}</option>)}</Select>
                  </Field>
                  <div className="text-[10px] font-semibold text-slate-400 uppercase tracking-wide">Initial followUps[0] (optional)</div>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Initial Note" apiName="note" required={false}><Input value={inquiryForm.note} onChange={(e) => setInquiryForm({ ...inquiryForm, note: e.target.value })} placeholder="Interested in Grade 6…" /></Field>
                    <Field label="Next Follow-up" apiName="nextFollowUp" required={false}><Input type="date" min={new Date().toISOString().slice(0, 10)} value={inquiryForm.nextFollowUp} onChange={(e) => setInquiryForm({ ...inquiryForm, nextFollowUp: e.target.value })} /></Field>
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
                              <div className="font-bold text-slate-900">{adm.studentName || inq?.studentName || '(unnamed applicant)'}</div>
                              <div className="text-[10px] text-slate-400">
                                {adm.inquiryId ? 'from inquiry' : 'direct'}
                                {(adm.guardians && adm.guardians.length) ? ` · ${adm.guardians.length} guardian${adm.guardians.length === 1 ? '' : 's'}` : ''}
                                {` · ${adm.admissionDate || '—'}`}
                              </div>
                            </td>
                            <td className="px-4 py-3 text-slate-500">{(adm.documents || []).length}</td>
                            <td className="px-4 py-3">
                              <Select value={adm.status} onChange={(e) => changeAdmissionStatus(adm, e.target.value)} className="!py-1 !text-[11px]">
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
              <Card title="New Admission" subtitle="Admissions intentionally carry no academic-year field.">
                <div className="space-y-3">
                  <Field label="School ID" apiName="schoolId" required><Input value={admissionForm.schoolId} onChange={(e) => setAdmissionForm({ ...admissionForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                  <Field label="From Inquiry" apiName="inquiryId" required={false} hint={admissionForm.inquiryId ? 'Applicant data pulled from the inquiry (editable). Auto-advances it to ADMITTED.' : 'Leave as none for a direct/walk-in admission and fill the details below.'}>
                    <Select value={admissionForm.inquiryId} onChange={(e) => pickAdmissionInquiry(e.target.value)}>
                      <option value="">— none (direct admission) —</option>
                      {inquiries.map((i) => <option key={i.id} value={i.id}>{i.studentName || (i.guardians && i.guardians[0] && i.guardians[0].name) || i.id}</option>)}
                    </Select>
                  </Field>

                  <Field label="Student Name" apiName="studentName" required={false}><Input value={admissionForm.studentName} onChange={(e) => setAdmissionForm({ ...admissionForm, studentName: e.target.value })} placeholder="e.g. Aarav Nair" /></Field>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="Date of Birth" apiName="dob" required={false}><Input type="date" value={admissionForm.dob} onChange={(e) => setAdmissionForm({ ...admissionForm, dob: e.target.value })} /></Field>
                    <Field label="Gender" apiName="gender" required={false}>
                      <Select value={admissionForm.gender} onChange={(e) => setAdmissionForm({ ...admissionForm, gender: e.target.value })}>
                        <option value="">— omitted —</option>
                        {['MALE', 'FEMALE', 'OTHER'].map((g) => <option key={g} value={g}>{g}</option>)}
                      </Select>
                    </Field>
                  </div>

                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-600">Guardians</span>
                      <button onClick={addAdmGuardian} className="text-[11px] font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"><Plus size={11} /> Add guardian</button>
                    </div>
                    {admissionForm.guardians.length === 0 && <p className="text-[11px] text-slate-400">None yet — pull from an inquiry above or add directly.</p>}
                    {admissionForm.guardians.map((g, idx) => (
                      <div key={idx} className="border border-slate-200 rounded-lg p-2.5 space-y-2 bg-slate-50/40">
                        <div className="flex items-center gap-2">
                          <Input value={g.name} onChange={(e) => setAdmGuardian(idx, { name: e.target.value })} placeholder="name (required)" className="flex-1" />
                          <Select value={g.relation || ''} onChange={(e) => setAdmGuardian(idx, { relation: e.target.value })} className="!py-1.5 w-32 shrink-0">
                            <option value="">— omitted —</option>
                            {RELATIONS.map((r) => <option key={r} value={r}>{r.replace('_', ' ')}</option>)}
                          </Select>
                          <button onClick={() => removeAdmGuardian(idx)} className="text-slate-300 hover:text-rose-600 p-1 shrink-0" title="Remove"><X size={14} /></button>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <Input value={g.phone || ''} onChange={(e) => setAdmGuardian(idx, { phone: e.target.value })} placeholder="Phone" />
                          <Input value={g.email || ''} onChange={(e) => setAdmGuardian(idx, { email: e.target.value })} placeholder="Email" />
                          <Input value={g.occupation || ''} onChange={(e) => setAdmGuardian(idx, { occupation: e.target.value })} placeholder="Occupation" />
                          <Input value={g.address || ''} onChange={(e) => setAdmGuardian(idx, { address: e.target.value })} placeholder="Address" />
                        </div>
                      </div>
                    ))}
                  </div>

                  <Field label="Admission Status" apiName="status" required={false}><Select value={admissionForm.status} onChange={(e) => setAdmissionForm({ ...admissionForm, status: e.target.value })}><option value="">— omitted —</option>{ADMISSION_STAGES.map((status) => <option key={status} value={status}>{status}</option>)}</Select></Field>
                  <Field label="Admission Date" apiName="admissionDate" required={false}><Input type="date" value={admissionForm.admissionDate} onChange={(e) => setAdmissionForm({ ...admissionForm, admissionDate: e.target.value })} /></Field>
                  <Field label="Documents" apiName="documents[]" required={false} hint="Comma-separated file names."><Input value={admissionForm.documents} onChange={(e) => setAdmissionForm({ ...admissionForm, documents: e.target.value })} placeholder="birth-cert.pdf, report-card.pdf" /></Field>
                  <div className="pt-2 border-t border-slate-100 flex justify-end">
                    <Button variant="primary" onClick={submitAdmission} disabled={busy}>
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
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm flex items-center gap-1.5"><GraduationCap size={15} /> Convert to Enrolled Student</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">Creates the Student record and marks this admission CONFIRMED.</p>
              </div>
              <button onClick={() => setConvert(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 space-y-3 overflow-y-auto">
              <div className="grid grid-cols-3 gap-3">
                <Field label="Admission ID" apiName="admissionId" required={false} hint="Accepted for payload parity; path id remains authoritative."><Input value={convertForm.admissionId} onChange={(e) => setConvertForm({ ...convertForm, admissionId: e.target.value })} /></Field>
                <Field label="School ID" apiName="schoolId" required={false} hint="Accepted but ignored."><Input value={convertForm.schoolId} onChange={(e) => setConvertForm({ ...convertForm, schoolId: e.target.value })} /></Field>
                <Field label="Academic Year" apiName="academicYear" required={false} hint="Accepted but ignored."><Input value={convertForm.academicYear} onChange={(e) => setConvertForm({ ...convertForm, academicYear: e.target.value })} /></Field>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Admission No." apiName="admissionNo" required={false}><Input value={convertForm.admissionNo} onChange={(e) => setConvertForm({ ...convertForm, admissionNo: e.target.value })} placeholder="ADM-2026-0007" /></Field>
                <Field label="Full Name" apiName="name" required={false}><Input value={convertForm.name} onChange={(e) => setConvertForm({ ...convertForm, name: e.target.value })} /></Field>
                <Field label="Date of Birth" apiName="dob" required={false}><Input type="date" value={convertForm.dob} onChange={(e) => setConvertForm({ ...convertForm, dob: e.target.value })} /></Field>
                <Field label="Gender" apiName="gender" required={false}>
                  <Select value={convertForm.gender} onChange={(e) => setConvertForm({ ...convertForm, gender: e.target.value })}>
                    <option value="">— omitted —</option>
                    {['MALE', 'FEMALE', 'OTHER'].map((g) => <option key={g} value={g}>{g}</option>)}
                  </Select>
                </Field>
                <Field label="Blood Group" apiName="bloodGroup" required={false}><Input value={convertForm.bloodGroup} onChange={(e) => setConvertForm({ ...convertForm, bloodGroup: e.target.value })} /></Field>
                <Field label="Photo URL" apiName="photoUrl" required={false}><Input value={convertForm.photoUrl} onChange={(e) => setConvertForm({ ...convertForm, photoUrl: e.target.value })} /></Field>
                <Field label="Wallet ID" apiName="walletId" required={false}><Input value={convertForm.walletId} onChange={(e) => setConvertForm({ ...convertForm, walletId: e.target.value })} /></Field>
                <Field label="Medical Record ID" apiName="medicalRecordId" required={false}><Input value={convertForm.medicalRecordId} onChange={(e) => setConvertForm({ ...convertForm, medicalRecordId: e.target.value })} /></Field>
                <Field label="Student Status" apiName="status" required={false}><Select value={convertForm.status} onChange={(e) => setConvertForm({ ...convertForm, status: e.target.value })}><option value="">— omitted —</option>{['ACTIVE', 'INACTIVE', 'SUSPENDED', 'ALUMNI'].map((status) => <option key={status} value={status}>{status}</option>)}</Select></Field>
                <Field label="Admission Date" apiName="admissionDate" required={false}><Input type="date" value={convertForm.admissionDate} onChange={(e) => setConvertForm({ ...convertForm, admissionDate: e.target.value })} /></Field>
              </div>
              {(convert.guardians && convert.guardians.length > 0) ? (
                <div className="bg-slate-50 border border-slate-100 rounded-lg px-3 py-2">
                  <div className="text-[11px] font-semibold text-slate-500 mb-1">{convert.guardians.length} guardian{convert.guardians.length === 1 ? '' : 's'} will be created &amp; linked:</div>
                  <div className="flex flex-wrap gap-1">
                    {convert.guardians.map((g, i) => (
                      <span key={i} className="text-[10px] bg-white border border-slate-200 rounded px-1.5 py-0.5 text-slate-600">
                        {g.name}{g.relation ? ` · ${g.relation.replace('_', ' ')}` : ''}
                      </span>
                    ))}
                  </div>
                </div>
              ) : (
                <p className="text-[11px] text-slate-400 bg-slate-50 border border-slate-100 rounded-lg px-3 py-2">
                  No guardians on this admission — add them later in the <span className="font-semibold text-slate-500">Guardians</span> tab.
                </p>
              )}
              <div className="space-y-2 border-t border-slate-100 pt-3">
                <div className="text-xs font-bold text-slate-700">Current academic record <code className="font-mono text-[10px] text-slate-400">currentAcademicRecord</code> <span className="text-[9px] uppercase text-slate-400">optional</span></div>
                <div className="grid grid-cols-2 gap-3">
                  {[
                    ['academicYear', 'Academic Year'], ['studentNo', 'Student No'], ['rollNo', 'Roll No'],
                    ['classDocId', 'Class Document ID'], ['sectionNo', 'Section No'], ['hostelRoomNo', 'Hostel Room No'],
                  ].map(([key, label]) => <Field key={key} label={label} apiName={key} required={false}><Input value={convertForm.currentAcademicRecord[key]} onChange={(e) => setConvertForm({ ...convertForm, currentAcademicRecord: { ...convertForm.currentAcademicRecord, [key]: e.target.value } })} /></Field>)}
                  <Field label="Status" apiName="status" required={false}><Select value={convertForm.currentAcademicRecord.status} onChange={(e) => setConvertForm({ ...convertForm, currentAcademicRecord: { ...convertForm.currentAcademicRecord, status: e.target.value } })}><option value="">— omitted —</option>{['ACTIVE', 'INACTIVE', 'SUSPENDED', 'ALUMNI'].map((status) => <option key={status} value={status}>{status}</option>)}</Select></Field>
                </div>
              </div>

              <div className="space-y-2 border-t border-slate-100 pt-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-bold text-slate-700">Existing guardian links <code className="font-mono text-[10px] text-slate-400">guardians[]</code> <span className="text-[9px] uppercase text-slate-400">optional</span></span>
                  <button type="button" onClick={() => setConvertForm({ ...convertForm, guardians: [...convertForm.guardians, { guardianId: '', relation: '', primary: false, emergencyContact: false, pickupApproved: false, portalAccess: false }] })} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add</button>
                </div>
                {convertForm.guardians.map((guardian, index) => {
                  const updateGuardian = (patch) => setConvertForm({ ...convertForm, guardians: convertForm.guardians.map((item, i) => i === index ? { ...item, ...patch } : item) });
                  return <div key={index} className="border border-slate-200 rounded-lg p-3 space-y-2 bg-slate-50/50">
                    <div className="grid grid-cols-[1fr_1fr_auto] gap-2"><Input value={guardian.guardianId} onChange={(e) => updateGuardian({ guardianId: e.target.value })} placeholder="guardianId (required per row)" /><Select value={guardian.relation} onChange={(e) => updateGuardian({ relation: e.target.value })}><option value="">relation (optional)</option>{RELATIONS.map((relation) => <option key={relation} value={relation}>{relation}</option>)}</Select><button type="button" onClick={() => setConvertForm({ ...convertForm, guardians: convertForm.guardians.filter((_, i) => i !== index) })} className="text-slate-400 hover:text-rose-600"><X size={14} /></button></div>
                    <div className="grid grid-cols-2 gap-1 text-[10px] text-slate-600">{['primary', 'emergencyContact', 'pickupApproved', 'portalAccess'].map((flag) => <label key={flag} className="flex items-center gap-1"><input type="checkbox" checked={guardian[flag]} onChange={(e) => updateGuardian({ [flag]: e.target.checked })} /> {flag}</label>)}</div>
                  </div>;
                })}
              </div>
            </div>
            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 shrink-0 flex justify-end gap-2">
              <Button variant="default" onClick={() => setConvert(null)}>Cancel</Button>
              <Button variant="primary" onClick={submitConvert} disabled={busy}>
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
                <Field label="New Stage *" apiName="status" required={false} hint="Editable in the tester so backend transition validation can be exercised.">
                  <Select value={followUpForm.status} onChange={(e) => setFollowUpForm({ ...followUpForm, status: e.target.value })}>
                    {INQUIRY_STAGES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                  </Select>
                </Field>
                <Field label="Next Follow-up">
                  <Input type="date" min={new Date().toISOString().slice(0, 10)} value={followUpForm.nextFollowUp} onChange={(e) => setFollowUpForm({ ...followUpForm, nextFollowUp: e.target.value })} />
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
