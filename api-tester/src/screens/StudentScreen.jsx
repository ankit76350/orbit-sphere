import { useState, useEffect, useCallback } from 'react';
import { 
  Users, User, Plus, Trash2, Edit2, Calendar, Award, Phone, Mail, 
  MapPin, X, GraduationCap, Heart, Info, History, ShieldAlert, RefreshCw
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

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
  const [studentForm, setStudentForm] = useState({
    name: '',
    admissionNo: '',
    dob: '',
    gender: 'MALE',
    bloodGroup: '',
    photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
    status: 'ACTIVE',
    admissionDate: new Date().toISOString().slice(0, 10)
  });

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
    classDocId: '',
    sectionId: '',
    rollNo: '',
    studentId: '',
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

  const reloadAll = () => {
    fetchStudents();
    fetchClasses();
    if (reload) reload(year);
  };

  // --- STUDENT ACTIONS ---
  const handleEditStudentClick = (s) => {
    setEditingStudent(s);
    setStudentForm({
      name: s.name || '',
      admissionNo: s.admissionNo || '',
      dob: s.dob || '',
      gender: s.gender || 'MALE',
      bloodGroup: s.bloodGroup || '',
      photoUrl: s.photoUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
      status: s.status || 'ACTIVE',
      admissionDate: s.admissionDate || new Date().toISOString().slice(0, 10)
    });
  };

  const handleCancelStudentEdit = () => {
    setEditingStudent(null);
    setStudentForm({
      name: '',
      admissionNo: '',
      dob: '',
      gender: 'MALE',
      bloodGroup: '',
      photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
      status: 'ACTIVE',
      admissionDate: new Date().toISOString().slice(0, 10)
    });
  };

  const submitStudent = async () => {
    if (!studentForm.name || !studentForm.admissionNo) {
      toast.error("Name and Admission Number are required.");
      return;
    }
    setBusyStudent(true);
    try {
      if (editingStudent) {
        await api.updateStudent(editingStudent.id, { schoolId, ...studentForm });
        toast.success(`Student "${studentForm.name}" updated successfully.`);
        setEditingStudent(null);
      } else {
        await api.createStudent({ schoolId, ...studentForm });
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
    if (!classes || classes.length === 0) {
      toast.error("No classes available in this year context. Setup classes first.");
      return;
    }
    setAssignForm({
      academicYear: year || '',
      classDocId: classes[0].id,
      sectionId: classes[0].sections && classes[0].sections.length > 0 ? classes[0].sections[0] : '',
      rollNo: '',
      studentId: historyStudent ? historyStudent.admissionNo : '',
      status: 'ACTIVE'
    });
    setShowAssignModal(true);
  };

  const handleClassSelectChange = (classId) => {
    const cls = classes.find(c => c.id === classId);
    setAssignForm(f => ({
      ...f,
      classDocId: classId,
      sectionId: cls && cls.sections && cls.sections.length > 0 ? cls.sections[0] : ''
    }));
  };

  const submitAcademicRecord = async () => {
    if (!assignForm.classDocId) {
      toast.error("Please select a target class.");
      return;
    }
    setBusyAssign(true);
    try {
      await api.assignAcademicRecord(historyStudent.id, {
        schoolId,
        ...assignForm
      });
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
                  <Field label="Full Name *">
                    <Input
                      value={studentForm.name}
                      onChange={(e) => setStudentForm({...studentForm, name: e.target.value})}
                      placeholder="e.g. John Doe"
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Admission No *">
                      <Input 
                        value={studentForm.admissionNo}
                        onChange={(e) => setStudentForm({...studentForm, admissionNo: e.target.value})}
                        placeholder="ADM-00212"
                        disabled={!!editingStudent}
                      />
                    </Field>
                    <Field label="Blood Group">
                      <Input 
                        value={studentForm.bloodGroup}
                        onChange={(e) => setStudentForm({...studentForm, bloodGroup: e.target.value})}
                        placeholder="e.g. O+"
                      />
                    </Field>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Date of Birth *">
                      <Input 
                        type="date"
                        value={studentForm.dob}
                        onChange={(e) => setStudentForm({...studentForm, dob: e.target.value})}
                      />
                    </Field>
                    <Field label="Gender *">
                      <Select 
                        value={studentForm.gender}
                        onChange={(e) => setStudentForm({...studentForm, gender: e.target.value})}
                      >
                        <option value="MALE">MALE</option>
                        <option value="FEMALE">FEMALE</option>
                        <option value="OTHER">OTHER</option>
                      </Select>
                    </Field>
                  </div>

                  <Field label="Avatar Photo URL">
                    <Input 
                      value={studentForm.photoUrl}
                      onChange={(e) => setStudentForm({...studentForm, photoUrl: e.target.value})}
                      placeholder="https://images.unsplash.com/..."
                    />
                  </Field>

                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Status">
                      <Select 
                        value={studentForm.status}
                        onChange={(e) => setStudentForm({...studentForm, status: e.target.value})}
                      >
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="INACTIVE">INACTIVE</option>
                        <option value="SUSPENDED">SUSPENDED</option>
                        <option value="ALUMNI">ALUMNI</option>
                      </Select>
                    </Field>
                    <Field label="Admission Date">
                      <Input 
                        type="date"
                        value={studentForm.admissionDate}
                        onChange={(e) => setStudentForm({...studentForm, admissionDate: e.target.value})}
                      />
                    </Field>
                  </div>

                  <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                    {editingStudent && (
                      <Button variant="default" onClick={handleCancelStudentEdit}>Cancel</Button>
                    )}
                    <Button 
                      variant="primary" 
                      onClick={submitStudent}
                      disabled={busyStudent || !studentForm.name || !studentForm.admissionNo}
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
                            <td className="px-4 py-2.5">{targetCls ? targetCls.name : 'Unknown Class'} · {rec.sectionId || '—'}</td>
                            <td className="px-4 py-2.5 font-mono text-slate-500">{rec.studentId || '—'}</td>
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
              <Field label="Academic Year context *">
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
                <Field label="Target Class *">
                  <Select 
                    value={assignForm.classDocId}
                    onChange={(e) => handleClassSelectChange(e.target.value)}
                  >
                    {classes.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </Select>
                </Field>
                <Field label="Section">
                  {sections.length === 0 ? (
                    <Select disabled className="text-slate-400">
                      <option value="">No sections</option>
                    </Select>
                  ) : (
                    <Select 
                      value={assignForm.sectionId}
                      onChange={(e) => setAssignForm({...assignForm, sectionId: e.target.value})}
                    >
                      {sections.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </Select>
                  )}
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Student Year ID">
                  <Input 
                    value={assignForm.studentId}
                    onChange={(e) => setAssignForm({...assignForm, studentId: e.target.value})}
                    placeholder="e.g. STU-2026-001"
                  />
                </Field>
                <Field label="Roll No">
                  <Input 
                    value={assignForm.rollNo}
                    onChange={(e) => setAssignForm({...assignForm, rollNo: e.target.value})}
                    placeholder="e.g. 12"
                  />
                </Field>
              </div>

              <Field label="Academic Record Status">
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
                  disabled={busyAssign || !assignForm.classDocId}
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
