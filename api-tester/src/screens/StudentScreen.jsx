import { useState, useEffect, useCallback } from 'react';
import {
  Users, User, Plus, Trash2, Edit2, Calendar, Award, Phone, Mail,
  MapPin, X, GraduationCap, Heart, Info, History, ShieldAlert, RefreshCw, FileText
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';
import { generateAdmissionNo, generateIdentityNo } from '../lib/date.js';

const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT', 'LEGAL_GUARDIAN', 'SIBLING', 'OTHER'];

const emptyAcademicRecord = (prefillIdentityNo = false) => ({
  academicYear: '', identityNo: prefillIdentityNo ? generateIdentityNo() : '', rollNo: '',
  classDocsId: '', sectionNo: '', hostelRoomNo: '', status: '',
});

const emptyInlineGuardian = () => ({
  name: '', relation: '', phone: '', email: '', address: '', occupation: '',
  primary: false, emergencyContact: false, pickupApproved: false, portalAccess: false,
});

const emptyExistingGuardianLink = () => ({
  guardianDocsId: '', relation: '',
  primary: false, emergencyContact: false, pickupApproved: false, portalAccess: false,
});

const emptyStudentForm = (schoolId = '') => ({
  schoolId,
  name: '', admissionNo: generateAdmissionNo(), dob: '', gender: '', bloodGroup: '',
  photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
  walletDocsId: '', medicalRecordDocsId: '', documents: '', medicalRemark: '', status: '', admissionDate: '',
  guardians: [], existingGuardianLinks: [], currentAcademicRecord: emptyAcademicRecord(true),
});

const nullable = (value) => value === '' ? null : value;
const commaSeparatedValues = (value = '') => value.split(',').map((item) => item.trim()).filter(Boolean);
const hasAcademicRecordValue = (record = {}) => Object.values(record).some(
  (value) => value != null && String(value).trim() !== ''
);

function StudentListEditor({
  label,
  apiName,
  items,
  itemLabel,
  addLabel,
  emptyMessage,
  placeholder,
  icon: Icon,
  showForm,
  draft,
  onOpen,
  onDraftChange,
  onAdd,
  onCancel,
  onRemove,
}) {
  return (
    <div className="border border-slate-200 bg-slate-50 rounded-xl p-3 space-y-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-1.5 text-xs font-semibold text-slate-600">
          <span>{label}</span>
          <code className="font-mono text-[10px] font-medium text-slate-400">{apiName}</code>
          <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span>
        </div>
        <button
          type="button"
          onClick={onOpen}
          disabled={showForm}
          className="shrink-0 text-[11px] font-semibold text-blue-600 hover:text-blue-700 disabled:text-slate-400 flex items-center gap-1"
        >
          <Plus size={12} /> {addLabel}
        </button>
      </div>

      {items.length > 0 ? (
        <div className="space-y-2">
          {items.map((item, index) => (
            <div
              key={`${item}-${index}`}
              className="flex items-start gap-2 bg-white border border-slate-200 rounded-lg px-3 py-2"
            >
              <Icon size={14} className="text-blue-500 mt-0.5 shrink-0" />
              <div className="min-w-0 flex-1">
                <div className="text-[9px] uppercase tracking-wide text-slate-400">{itemLabel} {index + 1}</div>
                <div className="text-xs font-medium text-slate-700 break-words select-all">{item}</div>
              </div>
              <button
                type="button"
                onClick={() => onRemove(index)}
                className="text-slate-400 hover:text-rose-600 p-0.5"
                title={`Remove ${itemLabel.toLowerCase()}`}
              >
                <X size={13} />
              </button>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-[11px] text-slate-400 bg-white border border-dashed border-slate-200 rounded-lg px-3 py-2">
          {emptyMessage}
        </p>
      )}

      {showForm && (
        <div className="border border-blue-200 bg-white rounded-lg p-3 space-y-3">
          <Input
            value={draft}
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                onAdd();
              }
            }}
            placeholder={placeholder}
            autoFocus
          />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="default" onClick={onCancel}>Cancel</Button>
            <Button type="button" variant="primary" onClick={onAdd}>
              <Plus size={14} />
              {addLabel}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function StudentDetailCard({ label, value, mono = false }) {
  const displayValue = value === null || value === undefined || value === '' ? '—' : String(value);
  return (
    <div className="border border-slate-100 bg-slate-50 rounded-xl px-4 py-3 min-w-0">
      <div className="text-[9px] font-bold uppercase tracking-wider text-slate-400">{label}</div>
      <div className={`mt-1 text-xs text-slate-800 break-all ${mono ? 'font-mono select-all' : 'font-semibold'}`}>
        {displayValue}
      </div>
    </div>
  );
}

export default function StudentScreen({ schoolId, years, year, reload }) {
  const toast = useToast();

  // Lists
  const [students, setStudents] = useState([]);
  const [classes, setClasses] = useState([]);
  const [assignmentClasses, setAssignmentClasses] = useState([]);

  // Loading states
  const [loadingStudents, setLoadingStudents] = useState(false);

  // Form busy states
  const [busyStudent, setBusyStudent] = useState(false);

  // Student Form State
  const [editingStudent, setEditingStudent] = useState(null);
  const [studentForm, setStudentForm] = useState(() => emptyStudentForm(schoolId || ''));
  const [showStudentDocumentForm, setShowStudentDocumentForm] = useState(false);
  const [studentDocument, setStudentDocument] = useState('');
  const [showStudentMedicalRemarkForm, setShowStudentMedicalRemarkForm] = useState(false);
  const [studentMedicalRemark, setStudentMedicalRemark] = useState('');

  // Complete student-details modal
  const [studentDetails, setStudentDetails] = useState(null);
  const [loadingStudentDetails, setLoadingStudentDetails] = useState(false);

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
    identityNo: generateIdentityNo(),
    rollNo: '',
    classDocsId: '',
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
    setShowStudentDocumentForm(false);
    setStudentDocument('');
    setShowStudentMedicalRemarkForm(false);
    setStudentMedicalRemark('');
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
    setShowStudentDocumentForm(false);
    setStudentDocument('');
    setShowStudentMedicalRemarkForm(false);
    setStudentMedicalRemark('');
  };

  const addStudentListValue = (field, draft, clearDraft, closeForm, label) => {
    const value = draft.trim();
    if (!value) {
      toast.error(`Enter a ${label.toLowerCase()}.`);
      return;
    }
    setStudentForm((current) => ({
      ...current,
      [field]: [...commaSeparatedValues(current[field]), value].join(', '),
    }));
    clearDraft('');
    closeForm(false);
  };

  const removeStudentListValue = (field, index) => {
    setStudentForm((current) => ({
      ...current,
      [field]: commaSeparatedValues(current[field])
        .filter((_, itemIndex) => itemIndex !== index)
        .join(', '),
    }));
  };

  const submitStudent = async () => {
    if (!studentForm.name) {
      toast.error("Name is required.");
      return;
    }
    if (hasAcademicRecordValue(studentForm.currentAcademicRecord)
      && !studentForm.currentAcademicRecord.academicYear?.trim()) {
      toast.error("Academic Year is required when current academic record details are provided.");
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
        documents: commaSeparatedValues(studentForm.documents),
        medicalRemark: commaSeparatedValues(studentForm.medicalRemark),
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
          guardians: [
            ...studentForm.guardians.filter((guardian) => guardian.name && guardian.name.trim()),
            ...studentForm.existingGuardianLinks.filter((guardian) => guardian.guardianDocsId && guardian.guardianDocsId.trim()),
          ].map((guardian) => Object.fromEntries(
            Object.entries(guardian).map(([key, value]) => [
              key,
              typeof value === 'string' ? nullable(value.trim()) : value,
            ])
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

  const openStudentDetails = async (student) => {
    setStudentDetails(student);
    setLoadingStudentDetails(true);
    try {
      setStudentDetails(await api.getStudent(student.id));
    } catch (error) {
      toast.error(error.message || 'Failed to load complete student details.');
    } finally {
      setLoadingStudentDetails(false);
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
    const availableClasses = assignmentClasses.length > 0 ? assignmentClasses : classes;
    const firstClass = availableClasses[0];
    setAssignmentClasses(availableClasses);
    setAssignForm({
      academicYear: year || '',
      identityNo: generateIdentityNo(),
      rollNo: '',
      classDocsId: firstClass?.id || '',
      sectionNo: firstClass?.sections && firstClass.sections.length > 0 ? firstClass.sections[0] : '',
      hostelRoomNo: '',
      status: 'ACTIVE'
    });
    setShowAssignModal(true);
  };

  const handleClassSelectChange = (classDocsId) => {
    const cls = assignmentClasses.find(c => c.id === classDocsId);
    setAssignForm(f => ({
      ...f,
      classDocsId: classDocsId,
      sectionNo: cls && cls.sections && cls.sections.length > 0 ? cls.sections[0] : ''
    }));
  };

  const handleAcademicYearChange = async (academicYear) => {
    setAssignForm((form) => ({ ...form, academicYear, classDocsId: '', sectionNo: '' }));
    if (!academicYear) {
      setAssignmentClasses([]);
      return;
    }
    try {
      const targetClasses = await api.classesByYear(schoolId, academicYear);
      setAssignmentClasses(targetClasses || []);
    } catch (e) {
      setAssignmentClasses([]);
      toast.error(e.message || 'Failed to load classes for this academic year.');
    }
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

  const selectedClass = assignmentClasses.find(c => c.id === assignForm.classDocsId);
  const sections = selectedClass ? (selectedClass.sections || []) : [];
  const studentDocuments = commaSeparatedValues(studentForm.documents);
  const studentMedicalRemarks = commaSeparatedValues(studentForm.medicalRemark);

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
                        <th className="px-4 py-3">MongoDB Object ID</th>
                        <th className="px-4 py-3">Admission No</th>
                        <th className="px-4 py-3">Gender / DOB</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {students.map((s) => {
                        return (
                          <tr
                            key={s.id}
                            onClick={() => openStudentDetails(s)}
                            className="hover:bg-blue-50/40 transition cursor-pointer"
                            title="Click to view complete student details"
                          >
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
                            {/* MongoDB document id */}
                            <td className="px-4 py-3 align-top">
                              <code className="block max-w-[180px] break-all text-[10px] font-mono text-slate-500 select-all" title={s.id || ''}>
                                {s.id || '—'}
                              </code>
                            </td>
                            {/* admission no */}
                            <td className="px-4 py-3 font-mono font-bold text-slate-800 select-all">{s.admissionNo}</td>
                            {/* gender / dob */}
                            <td className="px-4 py-3">
                              <div className="text-slate-700">{s.gender}</div>
                              <div className="text-slate-400 text-[10px] mt-0.5">{s.dob ? new Date(s.dob).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}</div>
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
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    openAcademicHistory(s);
                                  }}
                                  className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                                  title="Academic Promotion History"
                                >
                                  <History size={14} />
                                </button>
                                <button
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleEditStudentClick(s);
                                  }}
                                  className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                                  title="Edit student profile"
                                >
                                  <Edit2 size={13} />
                                </button>
                                <button
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    deleteStudent(s);
                                  }}
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
                      onChange={(e) => setStudentForm({ ...studentForm, name: e.target.value })}
                      placeholder="e.g. John Doe"
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Admission No" apiName="admissionNo" required hint="Generated as ADM/YYYY/MM/DDSS and editable before submitting.">
                      <Input
                        value={studentForm.admissionNo}
                        onChange={(e) => setStudentForm({ ...studentForm, admissionNo: e.target.value })}
                        placeholder="ADM/2026/05/0519"
                      />
                    </Field>
                    <Field label="Blood Group" apiName="bloodGroup" required={false}>
                      <Input
                        value={studentForm.bloodGroup}
                        onChange={(e) => setStudentForm({ ...studentForm, bloodGroup: e.target.value })}
                        placeholder="e.g. O+"
                      />
                    </Field>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Date of Birth" apiName="dob" required={false}>
                      <Input
                        type="date"
                        value={studentForm.dob}
                        onChange={(e) => setStudentForm({ ...studentForm, dob: e.target.value })}
                      />
                    </Field>
                    <Field label="Gender" apiName="gender" required={false}>
                      <Select
                        value={studentForm.gender}
                        onChange={(e) => setStudentForm({ ...studentForm, gender: e.target.value })}
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
                      onChange={(e) => setStudentForm({ ...studentForm, photoUrl: e.target.value })}
                      placeholder="https://images.unsplash.com/..."
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Status" apiName="status" required={false}>
                      <Select
                        value={studentForm.status}
                        onChange={(e) => setStudentForm({ ...studentForm, status: e.target.value })}
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
                        onChange={(e) => setStudentForm({ ...studentForm, admissionDate: e.target.value })}
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

                  <StudentListEditor
                    label="Documents"
                    apiName="documents[]"
                    items={studentDocuments}
                    itemLabel="Document"
                    addLabel="Add Document"
                    emptyMessage="No documents have been added."
                    placeholder="Document name, storage reference, or URL"
                    icon={FileText}
                    showForm={showStudentDocumentForm}
                    draft={studentDocument}
                    onOpen={() => setShowStudentDocumentForm(true)}
                    onDraftChange={setStudentDocument}
                    onAdd={() => addStudentListValue(
                      'documents',
                      studentDocument,
                      setStudentDocument,
                      setShowStudentDocumentForm,
                      'Document'
                    )}
                    onCancel={() => {
                      setShowStudentDocumentForm(false);
                      setStudentDocument('');
                    }}
                    onRemove={(index) => removeStudentListValue('documents', index)}
                  />

                  <StudentListEditor
                    label="Medical Remarks"
                    apiName="medicalRemark[]"
                    items={studentMedicalRemarks}
                    itemLabel="Remark"
                    addLabel="Add Remark"
                    emptyMessage="No medical remarks have been added."
                    placeholder="Medical remark"
                    icon={Heart}
                    showForm={showStudentMedicalRemarkForm}
                    draft={studentMedicalRemark}
                    onOpen={() => setShowStudentMedicalRemarkForm(true)}
                    onDraftChange={setStudentMedicalRemark}
                    onAdd={() => addStudentListValue(
                      'medicalRemark',
                      studentMedicalRemark,
                      setStudentMedicalRemark,
                      setShowStudentMedicalRemarkForm,
                      'Medical remark'
                    )}
                    onCancel={() => {
                      setShowStudentMedicalRemarkForm(false);
                      setStudentMedicalRemark('');
                    }}
                    onRemove={(index) => removeStudentListValue('medicalRemark', index)}
                  />

                  <div className="space-y-3 border-t border-slate-100 pt-3">
                    <div className="text-xs font-bold text-slate-700">Current academic record <code className="font-mono text-[10px] font-medium text-slate-400">currentAcademicRecord</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></div>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Academic Year" apiName="academicYear" required={hasAcademicRecordValue(studentForm.currentAcademicRecord)} hint="Required whenever any current academic record value is provided."><Input value={studentForm.currentAcademicRecord.academicYear} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, academicYear: e.target.value } })} /></Field>
                      <Field label="Identity No" apiName="identityNo" required={false} hint="Prefilled as IDN/YYYY/MM/DDSS. You can replace it with any value before submitting."><Input value={studentForm.currentAcademicRecord.identityNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, identityNo: e.target.value } })} placeholder="IDN/2026/05/0611" className="font-mono" /></Field>
                      <Field label="Roll No" apiName="rollNo" required={false}><Input value={studentForm.currentAcademicRecord.rollNo} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, rollNo: e.target.value } })} /></Field>
                      <Field label="Class Document ID" apiName="classDocsId" required={false}><Input value={studentForm.currentAcademicRecord.classDocsId} onChange={(e) => setStudentForm({ ...studentForm, currentAcademicRecord: { ...studentForm.currentAcademicRecord, classDocsId: e.target.value } })} /></Field>
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
                    <>
                      <div className="space-y-3 border border-slate-200 bg-slate-50 rounded-xl p-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-700">Inline guardians <code className="font-mono text-[10px] font-medium text-slate-400">guardians[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
                          <button type="button" onClick={() => setStudentForm({ ...studentForm, guardians: [...studentForm.guardians, emptyInlineGuardian()] })} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add Guardian</button>
                        </div>
                        {studentForm.guardians.length === 0 && (
                          <p className="text-[11px] text-slate-400 bg-white border border-dashed border-slate-200 rounded-lg px-3 py-2">
                            No inline guardians have been added.
                          </p>
                        )}
                        {studentForm.guardians.map((guardian, index) => {
                          const updateGuardian = (patch) => setStudentForm({ ...studentForm, guardians: studentForm.guardians.map((item, i) => i === index ? { ...item, ...patch } : item) });
                          return (
                            <div key={index} className="border border-slate-200 rounded-lg p-3 space-y-2 bg-white">
                              <div className="flex justify-between items-center"><span className="text-[11px] font-semibold text-slate-500">New guardian {index + 1}</span><button type="button" onClick={() => setStudentForm({ ...studentForm, guardians: studentForm.guardians.filter((_, i) => i !== index) })} className="text-slate-400 hover:text-rose-600"><X size={14} /></button></div>
                              <div className="grid grid-cols-2 gap-2">
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

                      <div className="space-y-3 border border-slate-200 bg-slate-50 rounded-xl p-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-700">Existing guardian links <code className="font-mono text-[10px] font-medium text-slate-400">guardians[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
                          <button
                            type="button"
                            onClick={() => setStudentForm({
                              ...studentForm,
                              existingGuardianLinks: [...studentForm.existingGuardianLinks, emptyExistingGuardianLink()],
                            })}
                            className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"
                          >
                            <Plus size={11} /> Add
                          </button>
                        </div>
                        {studentForm.existingGuardianLinks.length === 0 && (
                          <p className="text-[11px] text-slate-400 bg-white border border-dashed border-slate-200 rounded-lg px-3 py-2">
                            No existing guardian documents have been linked.
                          </p>
                        )}
                        {studentForm.existingGuardianLinks.map((guardian, index) => {
                          const updateGuardianLink = (patch) => setStudentForm({
                            ...studentForm,
                            existingGuardianLinks: studentForm.existingGuardianLinks.map((item, itemIndex) =>
                              itemIndex === index ? { ...item, ...patch } : item
                            ),
                          });
                          return (
                            <div key={index} className="border border-slate-200 rounded-lg p-3 space-y-2 bg-white">
                              <div className="flex items-center justify-between">
                                <span className="text-[11px] font-semibold text-slate-500">Existing guardian link {index + 1}</span>
                                <button
                                  type="button"
                                  onClick={() => setStudentForm({
                                    ...studentForm,
                                    existingGuardianLinks: studentForm.existingGuardianLinks.filter((_, itemIndex) => itemIndex !== index),
                                  })}
                                  className="text-slate-400 hover:text-rose-600"
                                  title="Remove guardian link"
                                >
                                  <X size={14} />
                                </button>
                              </div>
                              <div className="grid grid-cols-2 gap-2">
                                <Input
                                  value={guardian.guardianDocsId}
                                  onChange={(e) => updateGuardianLink({ guardianDocsId: e.target.value })}
                                  placeholder="guardianDocsId (MongoDB Object ID)"
                                  className="font-mono"
                                />
                                <Select value={guardian.relation} onChange={(e) => updateGuardianLink({ relation: e.target.value })}>
                                  <option value="">relation (optional)</option>
                                  {RELATIONS.map((relation) => <option key={relation} value={relation}>{relation}</option>)}
                                </Select>
                              </div>
                              <div className="grid grid-cols-2 gap-1 text-[10px] text-slate-600">
                                {['primary', 'emergencyContact', 'pickupApproved', 'portalAccess'].map((flag) => (
                                  <label key={flag} className="flex items-center gap-1">
                                    <input type="checkbox" checked={guardian[flag]} onChange={(e) => updateGuardianLink({ [flag]: e.target.checked })} /> {flag}
                                  </label>
                                ))}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </>
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

      {/* COMPLETE STUDENT DETAILS MODAL */}
      {studentDetails && (
        <div
          className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-150"
          onClick={() => setStudentDetails(null)}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="student-details-title"
            className="bg-white border border-slate-200 rounded-2xl shadow-xl w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-200"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="px-5 py-4 border-b border-slate-100 bg-slate-50 flex items-start justify-between gap-4 shrink-0">
              <div className="flex items-center gap-3 min-w-0">
                <img
                  src={studentDetails.photoUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80'}
                  alt=""
                  className="w-11 h-11 rounded-full object-cover border border-slate-200 bg-white shrink-0"
                  onError={(event) => {
                    event.currentTarget.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80';
                  }}
                />
                <div className="min-w-0">
                  <h3 id="student-details-title" className="font-bold text-slate-900 text-sm truncate">
                    {studentDetails.name || 'Student details'}
                  </h3>
                  <div className="mt-1 flex flex-wrap items-center gap-2">
                    <Badge color={studentDetails.status === 'ACTIVE' ? 'green' : studentDetails.status === 'SUSPENDED' ? 'rose' : 'slate'}>
                      {studentDetails.status || '—'}
                    </Badge>
                    <span className="text-[10px] font-mono text-slate-400 select-all">
                      {studentDetails.id}
                    </span>
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setStudentDetails(null)}
                className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                title="Close student details"
              >
                <X size={17} />
              </button>
            </header>

            <div className="p-5 overflow-y-auto space-y-5">
              {loadingStudentDetails && (
                <div className="flex items-center gap-2 text-xs text-blue-600">
                  <RefreshCw size={13} className="animate-spin" />
                  Loading the complete student document…
                </div>
              )}

              <section>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Student profile</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  <StudentDetailCard label="Full Name" value={studentDetails.name} />
                  <StudentDetailCard label="MongoDB Object ID" value={studentDetails.id} mono />
                  <StudentDetailCard label="School ID" value={studentDetails.schoolId} mono />
                  <StudentDetailCard label="Admission No" value={studentDetails.admissionNo} mono />
                  <StudentDetailCard label="Admission Docs ID" value={studentDetails.admissionDocsId} mono />
                  <StudentDetailCard label="Current Academic Record Docs ID" value={studentDetails.currentAcademicRecordDocsId} mono />
                  <StudentDetailCard label="Date of Birth" value={studentDetails.dob} />
                  <StudentDetailCard label="Gender" value={studentDetails.gender} />
                  <StudentDetailCard label="Blood Group" value={studentDetails.bloodGroup} />
                  <StudentDetailCard label="Status" value={studentDetails.status} />
                  <StudentDetailCard label="Admission Date" value={studentDetails.admissionDate} />
                  <StudentDetailCard label="Wallet Docs ID" value={studentDetails.walletDocsId} mono />
                  <StudentDetailCard label="Medical Record Docs ID" value={studentDetails.medicalRecordDocsId} mono />
                  <StudentDetailCard label="Photo URL" value={studentDetails.photoUrl} mono />
                  <StudentDetailCard
                    label="Created At"
                    value={studentDetails.createdAt ? new Date(studentDetails.createdAt).toLocaleString() : null}
                  />
                  <StudentDetailCard
                    label="Updated At"
                    value={studentDetails.updatedAt ? new Date(studentDetails.updatedAt).toLocaleString() : null}
                  />
                </div>
              </section>

              <section>
                <div className="flex items-center justify-between gap-3 mb-2">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Guardian links</h4>
                  <span className="text-[10px] font-semibold text-slate-400">
                    {(studentDetails.guardians || []).length} linked
                  </span>
                </div>
                {(studentDetails.guardians || []).length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {studentDetails.guardians.map((guardian, index) => (
                      <div key={`${guardian.guardianDocsId || 'guardian'}-${index}`} className="border border-slate-200 rounded-xl p-4">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <div className="text-[10px] text-slate-400">Guardian {index + 1}</div>
                            <div className="text-xs font-mono font-semibold text-slate-800 break-all select-all mt-0.5">
                              {guardian.guardianDocsId || '—'}
                            </div>
                          </div>
                          <Badge color="blue">{guardian.relation ? guardian.relation.replaceAll('_', ' ') : '—'}</Badge>
                        </div>
                        <div className="grid grid-cols-2 gap-2 mt-3 text-[10px]">
                          {[
                            ['Primary', guardian.primary],
                            ['Emergency Contact', guardian.emergencyContact],
                            ['Pickup Approved', guardian.pickupApproved],
                            ['Portal Access', guardian.portalAccess],
                          ].map(([label, enabled]) => (
                            <div key={label} className={`rounded-lg px-2 py-1.5 font-semibold ${enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-50 text-slate-400'
                              }`}>
                              {label}: {enabled ? 'YES' : 'NO'}
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">
                    No guardian links recorded.
                  </div>
                )}
              </section>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <section>
                  <div className="flex items-center justify-between gap-3 mb-2">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Documents</h4>
                    <span className="text-[10px] font-semibold text-slate-400">{(studentDetails.documents || []).length}</span>
                  </div>
                  {(studentDetails.documents || []).length > 0 ? (
                    <div className="space-y-2">
                      {studentDetails.documents.map((document, index) => (
                        <div key={`${document}-${index}`} className="flex items-start gap-2 border border-slate-200 bg-slate-50 rounded-lg px-3 py-2">
                          <FileText size={13} className="text-blue-500 mt-0.5 shrink-0" />
                          <span className="text-xs text-slate-700 break-all select-all">{document}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">No documents recorded.</div>
                  )}
                </section>

                <section>
                  <div className="flex items-center justify-between gap-3 mb-2">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Medical remarks</h4>
                    <span className="text-[10px] font-semibold text-slate-400">{(studentDetails.medicalRemark || []).length}</span>
                  </div>
                  {(studentDetails.medicalRemark || []).length > 0 ? (
                    <div className="space-y-2">
                      {studentDetails.medicalRemark.map((remark, index) => (
                        <div key={`${remark}-${index}`} className="flex items-start gap-2 border border-slate-200 bg-slate-50 rounded-lg px-3 py-2">
                          <Heart size={13} className="text-rose-500 mt-0.5 shrink-0" />
                          <span className="text-xs text-slate-700 break-words select-all">{remark}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">No medical remarks recorded.</div>
                  )}
                </section>
              </div>

              <section>
                <div className="flex items-center justify-between gap-3 mb-2">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Current academic record</h4>
                  {studentDetails.currentAcademicRecord?.status && (
                    <Badge color={studentDetails.currentAcademicRecord.status === 'ACTIVE' ? 'green' : 'slate'}>
                      {studentDetails.currentAcademicRecord.status}
                    </Badge>
                  )}
                </div>
                {studentDetails.currentAcademicRecord ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {[
                      ['MongoDB Object ID', studentDetails.currentAcademicRecord.id, true],
                      ['School ID', studentDetails.currentAcademicRecord.schoolId, true],
                      ['Student Docs ID', studentDetails.currentAcademicRecord.studentDocsId, true],
                      ['Academic Year', studentDetails.currentAcademicRecord.academicYear],
                      ['Identity No', studentDetails.currentAcademicRecord.identityNo],
                      ['Roll No', studentDetails.currentAcademicRecord.rollNo],
                      ['Class Docs ID', studentDetails.currentAcademicRecord.classDocsId, true],
                      ['Section No', studentDetails.currentAcademicRecord.sectionNo],
                      ['Hostel Room No', studentDetails.currentAcademicRecord.hostelRoomNo],
                      ['Status', studentDetails.currentAcademicRecord.status],
                      ['Created At', studentDetails.currentAcademicRecord.createdAt ? new Date(studentDetails.currentAcademicRecord.createdAt).toLocaleString() : null],
                      ['Updated At', studentDetails.currentAcademicRecord.updatedAt ? new Date(studentDetails.currentAcademicRecord.updatedAt).toLocaleString() : null],
                    ].map(([label, value, mono]) => (
                      <StudentDetailCard key={label} label={label} value={value} mono={mono} />
                    ))}
                  </div>
                ) : (
                  <div className="text-xs text-slate-500 bg-slate-50 border border-dashed border-slate-200 rounded-xl px-4 py-3">
                    No current academic record assigned.
                  </div>
                )}
              </section>

              <details className="border border-slate-200 rounded-xl overflow-hidden">
                <summary className="cursor-pointer px-4 py-3 bg-slate-50 text-xs font-semibold text-slate-600">
                  Raw API document
                </summary>
                <pre className="p-4 bg-slate-950 text-slate-100 text-[11px] leading-relaxed overflow-x-auto whitespace-pre-wrap break-words select-all">
                  {JSON.stringify(studentDetails, null, 2)}
                </pre>
              </details>
            </div>

            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 flex justify-end shrink-0">
              <Button variant="default" onClick={() => setStudentDetails(null)}>Close</Button>
            </footer>
          </section>
        </div>
      )}

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
                        <th className="px-4 py-2.5">Identity No</th>
                        <th className="px-4 py-2.5">Roll No</th>
                        <th className="px-4 py-2.5">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200/60 text-slate-700">
                      {academicHistory.map((rec) => {
                        const targetCls = classes.find(c => c.id === rec.classDocsId);
                        return (
                          <tr key={rec.id} className="hover:bg-slate-100/20 transition">
                            <td className="px-4 py-2.5 font-bold text-slate-900">{rec.academicYear}</td>
                            <td className="px-4 py-2.5">{targetCls ? targetCls.name : 'Unknown Class'} · {rec.sectionNo || '—'}</td>
                            <td className="px-4 py-2.5 font-mono text-slate-500">{rec.identityNo || '—'}</td>
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
                  onChange={(e) => handleAcademicYearChange(e.target.value)}
                >
                  {years.map((y) => (
                    <option key={y.id} value={y.name}>{y.name} {y.name === year ? '(Current)' : ''}</option>
                  ))}
                </Select>
              </Field>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Target Class" apiName="classDocsId" required={false}>
                  <Select
                    value={assignForm.classDocsId}
                    onChange={(e) => handleClassSelectChange(e.target.value)}
                  >
                    <option value="">— omitted —</option>
                    {assignmentClasses.map((c) => (
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
                      onChange={(e) => setAssignForm({ ...assignForm, sectionNo: e.target.value })}
                    >
                      {sections.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </Select>
                  )}
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Identity No" apiName="identityNo" required={false} hint="Prefilled as IDN/YYYY/MM/DDSS and editable.">
                  <Input
                    value={assignForm.identityNo}
                    onChange={(e) => setAssignForm({ ...assignForm, identityNo: e.target.value })}
                    placeholder="IDN/2026/05/0611"
                    className="font-mono"
                  />
                </Field>
                <Field label="Roll No" apiName="rollNo" required={false}>
                  <Input
                    value={assignForm.rollNo}
                    onChange={(e) => setAssignForm({ ...assignForm, rollNo: e.target.value })}
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
                  onChange={(e) => setAssignForm({ ...assignForm, status: e.target.value })}
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
