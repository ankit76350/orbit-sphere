import { useState, useEffect, useCallback } from 'react';
import { 
  Users, User, Plus, Trash2, Edit2, Calendar, Award, Phone, Mail, 
  MapPin, X, GraduationCap, Heart, Info, History, ShieldAlert, RefreshCw
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT', 'LEGAL_GUARDIAN', 'SIBLING', 'OTHER'];

const emptyAcademicRecord = () => ({
  academicYear: '', studentNo: '', rollNo: '', classDocId: '', sectionNo: '', hostelRoomNo: '', status: '',
});

const emptyInlineGuardian = () => ({
  guardianId: '', name: '', relation: '', phone: '', email: '', address: '', occupation: '',
  primary: false, emergencyContact: false, pickupApproved: false, portalAccess: false,
});

const emptyStudentForm = (schoolId = '') => ({
  schoolId,
  name: '', admissionNo: '', dob: '', gender: '', bloodGroup: '',
  photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
  walletDocsId: '', medicalRecordDocsId: '', documents: '', medicalRemark: '', status: '', admissionDate: '',
  academicYear: '', classDocId: '', classId: '', sectionNo: '', rollNo: '',
  guardians: [], currentAcademicRecord: emptyAcademicRecord(),
});

const nullable = (value) => value === '' ? null : value;

export default function StudentScreen({ schoolId, years, year, reload }) {
  const toast = useToast();

  // Lists
  const [students, setStudents] = useState([]);
  const [classes, setClasses] = useState([]);

  // Loading states
  const [loadingStudents, setLoadingStudents] = useState(false);

  // Form busy states
  const [busyStudent, setBusyStudent] = useState(false);

  // Student Form State
  const [editingStudent, setEditingStudent] = useState(null);
  const [studentForm, setStudentForm] = useState(() => emptyStudentForm(schoolId || ''));

  // Academic History Modal State
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [historyStudent, setHistoryStudent] = useState(null);
  const [academicHistory, setAcademicHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Promotion / Assign Record State
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [busyAssign, setBusyAssign] = useState(false);
  const [assignForm, setAssignForm] = useState({
    academicYear: year || '',
    studentNo: '',
    rollNo: '',
    classDocId: '',
    sectionNo: '',
    hostelRoomNo: '',
    status: 'ACTIVE'
  });

  // Fetch Students
  const fetchStudents = useCallback(async () => {
    if (!schoolId) return;
    setLoadingStudents(true);
    try {
      const data = await api.students(schoolId);
      setStudents(data || []);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load students list.");
    } finally {
      setLoadingStudents(false);
    }
  }, [schoolId, toast]);

  // Fetch Classes
  const fetchClasses = useCallback(async () => {
    if (!schoolId || !year) return;
    try {
      const data = await api.classesByYear(schoolId, year);
      setClasses(data || []);
    } catch (e) {
      console.error("Failed to fetch classes:", e);
    }
  }, [schoolId, year]);

  // Initial & Reactive Loads
  useEffect(() => {
    fetchStudents();
    fetchClasses();
  }, [fetchStudents, fetchClasses]);

  useEffect(() => setStudentForm((current) => ({ ...current, schoolId: schoolId || '' })), [schoolId]);

  const reloadAll = () => {
    fetchStudents();
    fetchClasses();
    if (reload) reload(year);
  };

  // --- STUDENT ACTIONS ---
  const handleEditStudentClick = (s) => {
    setEditingStudent(s);
    setStudentForm((current) => ({
      ...emptyStudentForm(schoolId || ''),
      name: s.name || '',
      admissionNo: s.admissionNo || '',
      dob: s.dob || '',
      gender: s.gender || '',
      bloodGroup: s.bloodGroup || '',
      photoUrl: s.photoUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
      walletDocsId: s.walletDocsId || '',
      medicalRecordDocsId: s.medicalRecordDocsId || '',
      documents: (s.documents || []).join(', '),
      medicalRemark: (s.medicalRemark || []).join(', '),
      status: s.status || '',
      admissionDate: s.admissionDate || '',
      currentAcademicRecord: s.currentAcademicRecord
        ? { ...emptyAcademicRecord(), ...s.currentAcademicRecord }
        : current.currentAcademicRecord,
    }));
  };

  const handleCancelStudentEdit = () => {
    setEditingStudent(null);
    setStudentForm(emptyStudentForm(schoolId || ''));
  };

  const submitStudent = async () => {
    if (!studentForm.name) {
      toast.error("Name is required.");
      return;
    }
    setBusyStudent(true);
    try {
      const currentAcademicRecord = Object.values(studentForm.currentAcademicRecord).some((value) => value !== '')
        ? Object.fromEntries(Object.entries(studentForm.currentAcademicRecord).map(([key, value]) => [key, nullable(value)]))
        : null;
      const common = {
        admissionNo: nullable(studentForm.admissionNo),
        name: studentForm.name,
        dob: nullable(studentForm.dob),
        gender: nullable(studentForm.gender),
        bloodGroup: nullable(studentForm.bloodGroup),
        photoUrl: nullable(studentForm.photoUrl),
        walletDocsId: nullable(studentForm.walletDocsId),
        medicalRecordDocsId: nullable(studentForm.medicalRecordDocsId),
        documents: studentForm.documents.split(',').map((value) => value.trim()).filter(Boolean),
        medicalRemark: studentForm.medicalRemark.split(',').map((value) => value.trim()).filter(Boolean),
        status: nullable(studentForm.status),
        admissionDate: nullable(studentForm.admissionDate),
        currentAcademicRecord,
      };
      if (editingStudent) {
        await api.updateStudent(editingStudent.id, common);
        toast.success(`Student "${studentForm.name}" updated successfully.`);
        setEditingStudent(null);
      } else {
        await api.createStudent({
          schoolId: studentForm.schoolId,
          ...common,
          academicYear: nullable(studentForm.academicYear),
          classDocId: nullable(studentForm.classDocId),
          classId: nullable(studentForm.classId),
          sectionNo: nullable(studentForm.sectionNo),
          rollNo: nullable(studentForm.rollNo),
          guardians: studentForm.guardians.map((guardian) => Object.fromEntries(
            Object.entries(guardian).map(([key, value]) => [key, typeof value === 'string' ? nullable(value) : value])
          )),
        });
        toast.success(`Student "${studentForm.name}" registered successfully.`);
      }
      handleCancelStudentEdit();
      fetchStudents();
    } catch (e) {
      toast.error(e.message || "Failed to save student record.");
    } finally {
      setBusyStudent(false);
    }
  };

  const deleteStudent = async (s) => {
    if (!confirm(`Are you sure you want to delete student "${s.name || ''}"?`)) return;
    try {
      await api.deleteStudent(s.id);
      toast.success("Student record deleted.");
      fetchStudents();
    } catch (e) {
      toast.error("Failed to delete student: " + e.message);
    }
  };

  // --- ACADEMIC HISTORY ACTIONS ---
  const openAcademicHistory = async (s) => {
    setHistoryStudent(s);
    setAcademicHistory([]);
    setLoadingHistory(true);
    setShowHistoryModal(true);
    try {
      const hist = await api.getStudentAcademicHistory(s.id);
      setAcademicHistory(hist || []);
    } catch (e) {
      toast.error("Failed to fetch academic history.");
    } finally {
      setLoadingHistory(false);
    }
  };

  const openAssignModal = () => {
    const firstClass = classes[0];
    setAssignForm({
      academicYear: year || '',
      studentNo: historyStudent ? historyStudent.admissionNo || '' : '',
      rollNo: '',
      classDocId: firstClass?.id || '',
      sectionNo: firstClass?.sections && firstClass.sections.length > 0 ? firstClass.sections[0] : '',
      hostelRoomNo: '',
      status: 'ACTIVE'
    });
    setShowAssignModal(true);
  };

  const handleClassSelectChange = (classId) => {
    const cls = classes.find(c => c.id === classId);
    setAssignForm(f => ({
      ...f,
      classDocId: classId,
      sectionNo: cls && cls.sections && cls.sections.length > 0 ? cls.sections[0] : ''
    }));
  };

  const submitAcademicRecord = async () => {
    setBusyAssign(true);
    try {
      await api.assignAcademicRecord(historyStudent.id, assignForm);
      toast.success("Academic year record assigned.");
      setShowAssignModal(false);
      // reload history list
      const hist = await api.getStudentAcademicHistory(historyStudent.id);
      setAcademicHistory(hist || []);
    } catch (e) {
      toast.error(e.message || "Failed to assign record.");
    } finally {
      setBusyAssign(false);
    }
  };

  const selectedClass = classes.find(c => c.id === assignForm.classDocId);
  const sections = selectedClass ? (selectedClass.sections || []) : [];

  if (!schoolId) {
    return <Empty icon={Users} title="Pick a school to begin" hint="Select a school from the top bar to manage students." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm justify-between items-center shrink-0">
        <div className="flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-blue-600 border-b-2 border-blue-600 -mb-px">
          <GraduationCap size={16} />
          Manage Students
        </div>

        <button
          onClick={reloadAll}
          className="flex items-center gap-1 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold mr-2 mb-2 transition"
          title="Refresh Data"
        >
          <RefreshCw size={13} />
          Reload List
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto">
        {/* ==================== STUDENTS ==================== */}
        {(
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
            {/* List of Students */}
            <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">Students Roster</h3>
                  <p className="text-xs text-slate-500 mt-0.5">List of students registered in this school.</p>
                </div>
                <span className="text-xs font-semibold px-2.5 py-0.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-full">
                  {students.length} Registered
                </span>
              </header>

              <div className="flex-1 overflow-x-auto">
                {loadingStudents ? (
                  <Empty icon={RefreshCw} title="Loading student roster..." hint="Please wait." />
                ) : students.length === 0 ? (
                  <Empty icon={GraduationCap} title="No students found" hint="Register your first student profile using the form on the right." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Student details</th>
                        <th className="px-4 py-3">Admission No</th>
                        <th className="px-4 py-3">Gender / DOB</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {students.map((s) => {
                        return (
                          <tr key={s.id} className="hover:bg-slate-50/50 transition">
                            {/* photo & name */}
                            <td className="px-4 py-3 min-w-[200px]">
                              <div className="flex items-center gap-3">
                                <img 
                                  src={s.photoUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80'} 
                                  alt="avatar" 
                                  className="w-9 h-9 rounded-full object-cover border border-slate-200 shrink-0"
                                  onError={(e) => { e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80'; }} 
                                />
                                <div className="min-w-0">
                                  <div className="font-bold text-slate-900 text-xs truncate">{s.name}</div>
                                  <div className="text-[10px] text-slate-400 font-medium truncate mt-0.5">
                                    {(s.guardians || []).length} guardian{(s.guardians || []).length === 1 ? '' : 's'} linked
                                  </div>
                                </div>
                              </div>
                            </td>
                            {/* admission no */}
                            <td className="px-4 py-3 font-mono font-bold text-slate-800 select-all">{s.admissionNo}</td>
                            {/* gender / dob */}
                            <td className="px-4 py-3">
                              <div className="text-slate-700">{s.gender}</div>
                              <div className="text-slate-400 text-[10px] mt-0.5">{s.dob ? new Date(s.dob).toLocaleDateString(undefined, {month: 'short', day: 'numeric', year: 'numeric'}) : '—'}</div>
                            </td>
                            {/* status */}
                            <td className="px-4 py-3">
                              <Badge color={s.status === 'ACTIVE' ? 'green' : s.status === 'SUSPENDED' ? 'rose' : 'slate'}>
                                {s.status || 'ACTIVE'}
                              </Badge>
                            </td>
                            {/* actions */}
                            <td className="px-4 py-3 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <button 
                                  onClick={() => openAcademicHistory(s)}
                                  className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                                  title="Academic Promotion History"
                                >
                                  <History size={14} />
                                </button>
                                <button 
                                  onClick={() => handleEditStudentClick(s)}
                                  className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                                  title="Edit student profile"
                                >
                                  <Edit2 size={13} />
                                </button>
                                <button 
                                  onClick={() => deleteStudent(s)}
                                  className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-1.5 rounded-lg transition"
                                  title="Delete student profile"
                                >
                                  <Trash2 size={13} />
                                </button>
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

            {/* Registration Form */}
            <div className="xl:col-span-4">
              <Card 
                title={editingStudent ? "Edit Student Info" : "Register Student"} 
                subtitle={editingStudent ? `Modifying profile: ${editingStudent.name}` : "Enroll a new student to the school roster."}
              >
                <div className="space-y-4">
                  {!editingStudent && <Field label="School ID" apiName="schoolId" required><Input value={studentForm.schoolId} onChange={(e) => setStudentForm({ ...studentForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>}
                  <Field label="Full Name" apiName="name" required>
                    <Input
                      value={studentForm.name}
                      onChange={(e) => setStudentForm({...studentForm, name: e.target.value})}
                      placeholder="e.g. John Doe"
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Admission No" apiName="admissionNo" required={false}>
                      <Input 
                        value={studentForm.admissionNo}
                        onChange={(e) => setStudentForm({...studentForm, admissionNo: e.target.value})}
                        placeholder="ADM-00212"
                      />
                    </Field>
                    <Field label="Blood Group" apiName="bloodGroup" required={false}>
                      <Input 
                        value={studentForm.bloodGroup}
                        onChange={(e) => setStudentForm({...studentForm, bloodGroup: e.target.value})}
                        placeholder="e.g. O+"
                      />
                    </Field>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Date of Birth" apiName="dob" required={false}>
                      <Input 
                        type="date"
                        value={studentForm.dob}
                        onChange={(e) => setStudentForm({...studentForm, dob: e.target.value})}
                      />
                    </Field>
                    <Field label="Gender" apiName="gender" required={false}>
                      <Select 
                        value={studentForm.gender}
                        onChange={(e) => setStudentForm({...studentForm, gender: e.target.value})}
                      >
                        <option value="">— omitted —</option>
                        <option value="MALE">MALE</option>
                        <option value="FEMALE">FEMALE</option>
                        <option value="OTHER">OTHER</option>
                      </Select>
                    </Field>
                  </div>

                  <Field label="Avatar Photo URL" apiName="photoUrl" required={false}>
                    <Input 
                      value={studentForm.photoUrl}
                      onChange={(e) => setStudentForm({...studentForm, photoUrl: e.target.value})}
                      placeholder="https://images.unsplash.com/..."
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Status" apiName="status" required={false}>
                      <Select 
                        value={studentForm.status}
                        onChange={(e) => setStudentForm({...studentForm, status: e.target.value})}
                      >
                        <option value="">— omitted —</option>
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="INACTIVE">INACTIVE</option>
                        <option value="SUSPENDED">SUSPENDED</option>
                        <option value="ALUMNI">ALUMNI</option>
                      </Select>
                    </Field>
                    <Field label="Admission Date" apiName="admissionDate" required={false}>
                      <Input 
                        type="date"
                        value={studentForm.admissionDate}
                        onChange={(e) => setStudentForm({...studentForm, admissionDate: e.target.value})}
                      />
                    </Field>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Wallet Document ID" apiName="walletDocsId" required={false}>
                      <Input value={studentForm.walletDocsId} onChange={(e) => setStudentForm({ ...studentForm, walletDocsId: e.target.value })} placeholder="MongoDB wallet document id" />
                    </Field>
                    <Field label="Medical Record Document ID" apiName="medicalRecordDocsId" required={false}>
                      <Input value={studentForm.medicalRecordDocsId} onChange={(e) => setStudentForm({ ...studentForm, medicalRecordDocsId: e.target.value })} placeholder="MongoDB medical record document id" />
                    </Field>
                  </div>

                  <Field label="Documents" apiName="documents[]" required={false} hint="Comma-separated document names or references.">
                    <Input value={studentForm.documents} onChange={(e) => setStudentForm({ ...studentForm, documents: e.target.value })} placeholder="birth-certificate.pdf, report-card.pdf" />
                  </Field>
                  <Field label="Medical Remarks" apiName="medicalRemark[]" required={false} hint="Comma-separated remarks.">
                    <Input value={studentForm.medicalRemark} onChange={(e) => setStudentForm({ ...studentForm, medicalRemark: e.target.value })} placeholder="Asthma, Penicillin allergy" />
                  </Field>

                  {!editingStudent && (
                    <div className="space-y-3 border-t border-slate-100 pt-3">
                      <div className="text-xs font-bold text-slate-700">Top-level placement fields</div>
                      <Field label="Academic Year" apiName="academicYear" required={false}>
                        <Select value={studentForm.academicYear} onChange={(e) => setStudentForm({ ...studentForm, academicYear: e.target.value })}>
                          <option value="">— resolve current year —</option>
                          {years.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
                        </Select>
                      </Field>
                      <div className="grid grid-cols-2 gap-3">
                        <Field label="Class Document ID" apiName="classDocId" required={false}>
                          <Select value={studentForm.classDocId} onChange={(e) => setStudentForm({ ...studentForm, classDocId: e.target.value })}>
                            <option value="">— omitted —</option>
                            {classes.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                          </Select>
                        </Field>
                        <Field label="Class ID Alias" apiName="classId" required={false} hint="Alias for classDocId; exposed separately for parity.">
                          <Input value={studentForm.classId} onChange={(e) => setStudentForm({ ...studentForm, classId: e.target.value })} placeholder="Alternate class id" />
                        </Field>
                        <Field label="Section No" apiName="sectionNo" required={false}>
                          <Input value={studentForm.sectionNo} onChange={(e) => setStudentForm({ ...studentForm, sectionNo: e.target.value })} />
                        </Field>
                        <Field label="Roll No" apiName="rollNo" required={false}>
                          <Input value={studentForm.rollNo} onChange={(e) => setStudentForm({ ...studentForm, rollNo: e.target.value })} />
                        </Field>
                      </div>
                    </div>
                  )}

                  <div className="space-y-3 border-t border-slate-100 pt-3">
                    <div className="text-xs font-bold text-slate-700">Current academic record <code className="font-mono text-[10px] font-medium text-slate-400">currentAcademicRecord</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></div>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Academic Year" apiName="academicYear" required={false}><Input value={studentForm.currentAcademicRecord.academicYear} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, academicYear: e.target.value } })} /></Field>
                      <Field label="Student No" apiName="studentNo" required={false}><Input value={studentForm.currentAcademicRecord.studentNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, studentNo: e.target.value } })} /></Field>
                      <Field label="Roll No" apiName="rollNo" required={false}><Input value={studentForm.currentAcademicRecord.rollNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, rollNo: e.target.value } })} /></Field>
                      <Field label="Class Document ID" apiName="classDocId" required={false}><Input value={studentForm.currentAcademicRecord.classDocId} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, classDocId: e.target.value } })} /></Field>
                      <Field label="Section No" apiName="sectionNo" required={false}><Input value={studentForm.currentAcademicRecord.sectionNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, sectionNo: e.target.value } })} /></Field>
                      <Field label="Hostel Room No" apiName="hostelRoomNo" required={false}><Input value={studentForm.currentAcademicRecord.hostelRoomNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, hostelRoomNo: e.target.value } })} /></Field>
                      <Field label="Status" apiName="status" required={false}>
                        <Select value={studentForm.currentAcademicRecord.status} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, status: e.target.value } })}>
                          <option value="">— omitted —</option>
                          {['ACTIVE', 'INACTIVE', 'SUSPENDED', 'ALUMNI'].map((status) => <option key={status} value={status}>{status}</option>)}
                        </Select>
                      </Field>
                    </div>
                  </div>

                  {!editingStudent && (
                    <div className="space-y-2 border-t border-slate-100 pt-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-slate-700">Inline guardians <code className="font-mono text-[10px] font-medium text-slate-400">guardians[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
                        <button type="button" onClick={() => setStudentForm({ ...studentForm, guardians: [...studentForm.guardians, emptyInlineGuardian()] })} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add</button>
                      </div>
                      {studentForm.guardians.map((guardian, index) => {
                        const updateGuardian = (patch) => setStudentForm({ ...studentForm, guardians: studentForm.guardians.map((item, i) => i === index ? { ...item, ...patch } : item) });
                        return (
                          <div key={index} className="border border-slate-200 rounded-lg p-3 space-y-2 bg-slate-50/50">
                            <div className="flex justify-between items-center"><span className="text-[11px] font-semibold text-slate-500">guardian[{index}]</span><button type="button" onClick={() => setStudentForm({ ...studentForm, guardians: studentForm.guardians.filter((_, i) => i !== index) })} className="text-slate-400 hover:text-rose-600"><X size={14} /></button></div>
                            <div className="grid grid-cols-2 gap-2">
                              <Input value={guardian.guardianId} onChange={(e) => updateGuardian({ guardianId: e.target.value })} placeholder="guardianId" />
                              <Input value={guardian.name} onChange={(e) => updateGuardian({ name: e.target.value })} placeholder="name" />
                              <Select value={guardian.relation} onChange={(e) => updateGuardian({ relation: e.target.value })}><option value="">relation (optional)</option>{RELATIONS.map((relation) => <option key={relation} value={relation}>{relation}</option>)}</Select>
                              <Input value={guardian.phone} onChange={(e) => updateGuardian({ phone: e.target.value })} placeholder="phone" />
                              <Input value={guardian.email} onChange={(e) => updateGuardian({ email: e.target.value })} placeholder="email" />
                              <Input value={guardian.occupation} onChange={(e) => updateGuardian({ occupation: e.target.value })} placeholder="occupation" />
                            </div>
                            <Input value={guardian.address} onChange={(e) => updateGuardian({ address: e.target.value })} placeholder="address" />
                            <div className="grid grid-cols-2 gap-1 text-[10px] text-slate-600">
                              {['primary', 'emergencyContact', 'pickupApproved', 'portalAccess'].map((flag) => <label key={flag} className="flex items-center gap-1"><input type="checkbox" checked={guardian[flag]} onChange={(e) => updateGuardian({ [flag]: e.target.checked })} /> {flag}</label>)}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                    {editingStudent && (
                      <Button variant="default" onClick={handleCancelStudentEdit}>Cancel</Button>
                    )}
                    <Button 
                      variant="primary" 
                      onClick={submitStudent}
                      disabled={busyStudent || !studentForm.name}
                    >
                      {busyStudent ? <RefreshCw size={14} className="animate-spin" /> : <Plus size={14} />}
                      {editingStudent ? 'Save Profile' : 'Enroll Student'}
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        )}

      </div>

      {/* ACADEMIC HISTORY MODAL */}
      {showHistoryModal && historyStudent && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl shadow-xl max-w-2xl w-full p-6 text-slate-800 animate-in fade-in zoom-in-95 duration-150 relative flex flex-col max-h-[90vh]">
            <button 
              onClick={() => setShowHistoryModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 hover:bg-slate-50 p-1.5 rounded-lg transition"
              title="Close modal"
            >
              <X size={16} />
            </button>

            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <History size={18} className="text-blue-600" />
              <span>Academic Records for {historyStudent.name}</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 mb-5">View promotions history or assign student to a new class and academic year.</p>

            <div className="flex-1 overflow-y-auto pr-1 space-y-4">
              {loadingHistory ? (
                <div className="flex flex-col items-center justify-center py-12 gap-2 text-slate-400 text-xs">
                  <RefreshCw size={24} className="animate-spin text-blue-500" />
                  <span>Loading academic history records...</span>
                </div>
              ) : academicHistory.length === 0 ? (
                <div className="text-center py-8 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                  <GraduationCap size={32} className="mx-auto text-slate-300 mb-2" />
                  <p className="text-xs text-slate-500 font-semibold">No Academic Year Records assigned yet</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">Assign the student to an academic year to schedule classes.</p>
                </div>
              ) : (
                <div className="bg-slate-50 border border-slate-100 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-100/80 text-slate-500 font-semibold border-b border-slate-200/55">
                        <th className="px-4 py-2.5">Academic Year</th>
                        <th className="px-4 py-2.5">Class / Section</th>
                        <th className="px-4 py-2.5">Student Year ID</th>
                        <th className="px-4 py-2.5">Roll No</th>
                        <th className="px-4 py-2.5">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200/60 text-slate-700">
                      {academicHistory.map((rec) => {
                        const targetCls = classes.find(c => c.id === rec.classDocId);
                        return (
                          <tr key={rec.id} className="hover:bg-slate-100/20 transition">
                            <td className="px-4 py-2.5 font-bold text-slate-900">{rec.academicYear}</td>
                            <td className="px-4 py-2.5">{targetCls ? targetCls.name : 'Unknown Class'} · {rec.sectionNo || '—'}</td>
                            <td className="px-4 py-2.5 font-mono text-slate-500">{rec.studentNo || '—'}</td>
                            <td className="px-4 py-2.5 font-semibold text-slate-900">{rec.rollNo || '—'}</td>
                            <td className="px-4 py-2.5"><Badge color={rec.status === 'ACTIVE' ? 'green' : 'slate'}>{rec.status}</Badge></td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="pt-4 border-t border-slate-100 mt-5 flex items-center justify-between">
              <Button variant="primary" size="sm" onClick={openAssignModal}>
                <Plus size={14} /> Promote / Assign to Class
              </Button>
              <Button variant="default" size="sm" onClick={() => setShowHistoryModal(false)}>Close</Button>
            </div>
          </div>
        </div>
      )}

      {/* ASSIGN CLASS MODAL */}
      {showAssignModal && historyStudent && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-55 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl shadow-2xl max-w-md w-full p-6 text-slate-800 animate-in fade-in zoom-in-95 duration-150 relative">
            <button 
              onClick={() => setShowAssignModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 hover:bg-slate-50 p-1.5 rounded-lg transition"
              title="Close modal"
            >
              <X size={16} />
            </button>

            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <GraduationCap size={18} className="text-blue-600" />
              <span>Promote / Assign {historyStudent.name}</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 mb-5">Create or promote the student to a specific class and academic year.</p>

            <div className="space-y-4">
              <Field label="Academic Year" apiName="academicYear" required={false} hint="Required by the promote endpoint; optional for a direct academic-record assignment.">
                <Select 
                  value={assignForm.academicYear}
                  onChange={(e) => setAssignForm({...assignForm, academicYear: e.target.value})}
                >
                  {years.map((y) => (
                    <option key={y.id} value={y.name}>{y.name} {y.name === year ? '(Current)' : ''}</option>
                  ))}
                </Select>
              </Field>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Target Class" apiName="classDocId" required={false}>
                  <Select 
                    value={assignForm.classDocId}
                    onChange={(e) => handleClassSelectChange(e.target.value)}
                  >
                    <option value="">— omitted —</option>
                    {classes.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </Select>
                </Field>
                <Field label="Section" apiName="sectionNo" required={false}>
                  {sections.length === 0 ? (
                    <Input value={assignForm.sectionNo} onChange={(e) => setAssignForm({ ...assignForm, sectionNo: e.target.value })} placeholder="Section number" />
                  ) : (
                    <Select 
                      value={assignForm.sectionNo}
                      onChange={(e) => setAssignForm({...assignForm, sectionNo: e.target.value})}
                    >
                      {sections.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </Select>
                  )}
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Student No" apiName="studentNo" required={false}>
                  <Input 
                    value={assignForm.studentNo}
                    onChange={(e) => setAssignForm({...assignForm, studentNo: e.target.value})}
                    placeholder="e.g. STU-2026-001"
                  />
                </Field>
                <Field label="Roll No" apiName="rollNo" required={false}>
                  <Input 
                    value={assignForm.rollNo}
                    onChange={(e) => setAssignForm({...assignForm, rollNo: e.target.value})}
                    placeholder="e.g. 12"
                  />
                </Field>
              </div>

              <Field label="Hostel Room No" apiName="hostelRoomNo" required={false}>
                <Input value={assignForm.hostelRoomNo} onChange={(e) => setAssignForm({ ...assignForm, hostelRoomNo: e.target.value })} />
              </Field>

              <Field label="Academic Record Status" apiName="status" required={false}>
                <Select 
                  value={assignForm.status}
                  onChange={(e) => setAssignForm({...assignForm, status: e.target.value})}
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                  <option value="ALUMNI">ALUMNI</option>
                </Select>
              </Field>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button variant="default" size="sm" onClick={() => setShowAssignModal(false)}>Cancel</Button>
                <Button 
                  variant="primary" 
                  size="sm" 
                  onClick={submitAcademicRecord}
                  disabled={busyAssign}
                >
                  {busyAssign ? <RefreshCw size={14} className="animate-spin" /> : <Plus size={14} />}
                  Confirm Assignment
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
