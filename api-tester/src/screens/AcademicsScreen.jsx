import { useState, useEffect, useCallback } from 'react';
import { 
  GraduationCap, Calendar, CheckSquare, BookOpen, Award, ShieldAlert, Heart, 
  Plus, Trash2, Edit2, RefreshCw, X, User, DollarSign, CalendarCheck, BookOpenCheck, Activity, LayoutGrid
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';
import AcademicYearsScreen from './AcademicYearsScreen.jsx';
import ClassesScreen from './ClassesScreen.jsx';
import TimetableView from './TimetableView.jsx';
import TimetableBuilder from './TimetableBuilder.jsx';

export default function AcademicsScreen({ schoolId, years, year, staff, reload }) {
  const toast = useToast();
  const [subTab, setSubTab] = useState('academicYears'); // 'academicYears', 'classes', 'timetable', 'attendance', ...
  const [timetableSubTab, setTimetableSubTab] = useState('view'); // 'view', 'build'

  // Context lists
  const [students, setStudents] = useState([]);
  const [classes, setClasses] = useState([]);

  // Data lists
  const [attendance, setAttendance] = useState([]);
  const [homework, setHomework] = useState([]);
  const [results, setResults] = useState([]);
  const [discipline, setDiscipline] = useState([]);
  const [medical, setMedical] = useState([]);

  // Load states
  const [loadingData, setLoadingData] = useState(false);
  const [busy, setBusy] = useState(false);

  // Form states
  const [editingItem, setEditingItem] = useState(null);

  // 1. Attendance Form
  const [attForm, setAttForm] = useState({
    studentId: '',
    date: new Date().toISOString().slice(0, 10),
    status: 'PRESENT',
    presentBy: 'Class Teacher'
  });

  // 2. Homework Form
  const [hwForm, setHwForm] = useState({
    classId: '',
    subject: '',
    title: '',
    instructions: '',
    dueDate: new Date().toISOString().slice(0, 10),
    maxMarks: '100',
    assignmentScope: 'CLASS'
  });

  // 3. Academic Results Form
  const [resForm, setResForm] = useState({
    studentId: '',
    examName: 'Midterm Exam',
    overallGrade: 'A',
    totalPercentage: '',
    feedback: ''
  });
  const [subjectMarks, setSubjectMarks] = useState([]); // Array of { subject, maxMarks, obtainedMarks }
  const [newSubj, setNewSubj] = useState({ subject: '', maxMarks: '100', obtainedMarks: '' });

  // 4. Discipline Form
  const [discForm, setDiscForm] = useState({
    studentId: '',
    violation: '',
    fineAmount: '',
    actionTaken: '',
    incidentDate: new Date().toISOString().slice(0, 16) // datetime-local format
  });

  // 5. Medical Form
  const [medForm, setMedForm] = useState({
    studentId: '',
    visitDate: new Date().toISOString().slice(0, 10),
    diagnosis: '',
    doctorName: '',
    medicinesInput: '' // comma separated
  });

  // Helper getters
  const getStudentName = (sid) => {
    const s = students.find((x) => x.id === sid);
    return s ? `${s.name || ''}` : sid || '—';
  };

  const getClassName = (cid) => {
    const c = classes.find((x) => x.id === cid);
    return c ? c.name : cid || '—';
  };

  // Fetch initial context (students, classes)
  const fetchContext = useCallback(async () => {
    if (!schoolId) return;
    try {
      const [stList, clList] = await Promise.all([
        api.students(schoolId),
        year ? api.classesByYear(schoolId, year) : api.classes(schoolId)
      ]);
      setStudents(stList || []);
      setClasses(clList || []);

      // Set default IDs for selects
      if (stList && stList.length > 0) {
        setAttForm((f) => ({ ...f, studentId: stList[0].id }));
        setResForm((f) => ({ ...f, studentId: stList[0].id }));
        setDiscForm((f) => ({ ...f, studentId: stList[0].id }));
        setMedForm((f) => ({ ...f, studentId: stList[0].id }));
      }
      if (clList && clList.length > 0) {
        setHwForm((f) => ({ ...f, classId: clList[0].id }));
      }
    } catch (e) {
      console.error(e);
    }
  }, [schoolId, year]);

  // Fetch lists based on active subTab
  const fetchTabData = useCallback(async () => {
    if (!schoolId) return;
    setLoadingData(true);
    try {
      if (subTab === 'attendance') {
        const data = year 
          ? await api.attendanceByYear(schoolId, year)
          : await api.attendance(schoolId);
        setAttendance(data || []);
      } else if (subTab === 'homework') {
        const data = year 
          ? await api.homeworkByYear(schoolId, year)
          : await api.homework(schoolId);
        setHomework(data || []);
      } else if (subTab === 'results') {
        const data = year 
          ? await api.academicResultsByYear(schoolId, year)
          : await api.academicResults(schoolId);
        setResults(data || []);
      } else if (subTab === 'discipline') {
        const data = year 
          ? await api.disciplineLogsByYear(schoolId, year)
          : await api.disciplineLogs(schoolId);
        setDiscipline(data || []);
      } else if (subTab === 'medical') {
        const data = await api.medicalRecords(schoolId);
        setMedical(data || []);
      }
    } catch (e) {
      console.error(e);
      toast.error(`Failed to load ${subTab} data.`);
    } finally {
      setLoadingData(false);
    }
  }, [schoolId, year, subTab, toast]);

  useEffect(() => {
    fetchContext();
  }, [fetchContext]);

  useEffect(() => {
    fetchTabData();
  }, [fetchTabData]);

  const handleCancelEdit = () => {
    setEditingItem(null);
    // Reset forms
    if (subTab === 'attendance') {
      setAttForm({
        studentId: students.length > 0 ? students[0].id : '',
        date: new Date().toISOString().slice(0, 10),
        status: 'PRESENT',
        presentBy: 'Class Teacher'
      });
    } else if (subTab === 'homework') {
      setHwForm({
        classId: classes.length > 0 ? classes[0].id : '',
        subject: '',
        title: '',
        instructions: '',
        dueDate: new Date().toISOString().slice(0, 10),
        maxMarks: '100',
        assignmentScope: 'CLASS'
      });
    } else if (subTab === 'results') {
      setResForm({
        studentId: students.length > 0 ? students[0].id : '',
        examName: 'Midterm Exam',
        overallGrade: 'A',
        totalPercentage: '',
        feedback: ''
      });
      setSubjectMarks([]);
    } else if (subTab === 'discipline') {
      setDiscForm({
        studentId: students.length > 0 ? students[0].id : '',
        violation: '',
        fineAmount: '',
        actionTaken: '',
        incidentDate: new Date().toISOString().slice(0, 16)
      });
    } else if (subTab === 'medical') {
      setMedForm({
        studentId: students.length > 0 ? students[0].id : '',
        visitDate: new Date().toISOString().slice(0, 10),
        diagnosis: '',
        doctorName: '',
        medicinesInput: ''
      });
    }
  };

  // --- CRUD HANDLERS ---

  // 1. Attendance Submit
  const submitAttendance = async () => {
    if (!attForm.studentId || !attForm.date) {
      toast.error("Student and Date are required.");
      return;
    }
    setBusy(true);
    try {
      const payload = { schoolId, academicYear: year, ...attForm };
      if (editingItem) {
        await api.updateAttendance(editingItem.id, payload);
        toast.success("Attendance entry updated.");
      } else {
        await api.createAttendance(payload);
        toast.success("Attendance marked successfully.");
      }
      handleCancelEdit();
      fetchTabData();
    } catch (e) {
      toast.error(e.message || "Failed to mark attendance.");
    } finally {
      setBusy(false);
    }
  };

  // 2. Homework Submit
  const submitHomework = async () => {
    if (!hwForm.classId || !hwForm.subject || !hwForm.title) {
      toast.error("Class, Subject, and Assignment Title are required.");
      return;
    }
    setBusy(true);
    try {
      const payload = {
        schoolId,
        academicYear: year,
        ...hwForm,
        maxMarks: hwForm.maxMarks ? parseInt(hwForm.maxMarks) : null
      };

      if (editingItem) {
        await api.updateHomework(editingItem.id, payload);
        toast.success("Homework assignment updated.");
      } else {
        await api.createHomework(payload);
        toast.success("Homework assignment posted.");
      }
      handleCancelEdit();
      fetchTabData();
    } catch (e) {
      toast.error(e.message || "Failed to save homework assignment.");
    } finally {
      setBusy(false);
    }
  };

  // 3. Academic Results Submit
  const addSubjectMarkRow = () => {
    if (!newSubj.subject || !newSubj.obtainedMarks) {
      toast.error("Subject and Obtained Marks are required.");
      return;
    }
    setSubjectMarks([...subjectMarks, {
      subject: newSubj.subject,
      maxMarks: parseInt(newSubj.maxMarks) || 100,
      obtainedMarks: parseInt(newSubj.obtainedMarks) || 0
    }]);
    setNewSubj({ subject: '', maxMarks: '100', obtainedMarks: '' });
  };

  const removeSubjectMarkRow = (idx) => {
    setSubjectMarks(subjectMarks.filter((_, i) => i !== idx));
  };

  const submitAcademicResult = async () => {
    if (!resForm.studentId || !resForm.examName) {
      toast.error("Student and Exam Name are required.");
      return;
    }
    setBusy(true);
    try {
      // Auto-compute total percentage if subject marks are entered
      let computedPercentage = null;
      if (subjectMarks.length > 0) {
        const totalMax = subjectMarks.reduce((sum, item) => sum + item.maxMarks, 0);
        const totalObtained = subjectMarks.reduce((sum, item) => sum + item.obtainedMarks, 0);
        if (totalMax > 0) {
          computedPercentage = Math.round((totalObtained / totalMax) * 100 * 100) / 100;
        }
      }

      const payload = {
        schoolId,
        academicYear: year,
        ...resForm,
        marks: subjectMarks,
        totalPercentage: computedPercentage || (resForm.totalPercentage ? parseFloat(resForm.totalPercentage) : null)
      };

      if (editingItem) {
        await api.updateAcademicResult(editingItem.id, payload);
        toast.success("Academic exam results updated.");
      } else {
        await api.createAcademicResult(payload);
        toast.success("Academic exam report card registered.");
      }
      handleCancelEdit();
      fetchTabData();
    } catch (e) {
      toast.error(e.message || "Failed to save exam results.");
    } finally {
      setBusy(false);
    }
  };

  // 4. Discipline Log Submit
  const submitDisciplineLog = async () => {
    if (!discForm.studentId || !discForm.violation) {
      toast.error("Student and Violation name are required.");
      return;
    }
    setBusy(true);
    try {
      const payload = {
        schoolId,
        academicYear: year,
        ...discForm,
        fineAmount: discForm.fineAmount ? parseFloat(discForm.fineAmount) : null,
        incidentDate: discForm.incidentDate ? new Date(discForm.incidentDate).toISOString() : null
      };

      if (editingItem) {
        await api.updateDisciplineLog(editingItem.id, payload);
        toast.success("Discipline incident log updated.");
      } else {
        await api.createDisciplineLog(payload);
        toast.success("Discipline log entry registered.");
      }
      handleCancelEdit();
      fetchTabData();
    } catch (e) {
      toast.error(e.message || "Failed to record log.");
    } finally {
      setBusy(false);
    }
  };

  // 5. Medical Record Submit
  const submitMedicalRecord = async () => {
    if (!medForm.studentId || !medForm.diagnosis) {
      toast.error("Student and Diagnosis are required.");
      return;
    }
    setBusy(true);
    try {
      const medicines = medForm.medicinesInput 
        ? medForm.medicinesInput.split(',').map(m => m.trim()).filter(Boolean)
        : [];
      
      const payload = {
        schoolId,
        ...medForm,
        medicines
      };

      if (editingItem) {
        await api.updateMedicalRecord(editingItem.id, payload);
        toast.success("Medical visit record updated.");
      } else {
        await api.createMedicalRecord(payload);
        toast.success("Medical clinic log recorded.");
      }
      handleCancelEdit();
      fetchTabData();
    } catch (e) {
      toast.error(e.message || "Failed to log clinic visit.");
    } finally {
      setBusy(false);
    }
  };

  // Delete Action
  const deleteItem = async (item) => {
    if (!confirm(`Are you sure you want to delete this ${subTab} record?`)) return;
    try {
      if (subTab === 'attendance') await api.deleteAttendance(item.id);
      else if (subTab === 'homework') await api.deleteHomework(item.id);
      else if (subTab === 'results') await api.deleteAcademicResult(item.id);
      else if (subTab === 'discipline') await api.deleteDisciplineLog(item.id);
      else if (subTab === 'medical') await api.deleteMedicalRecord(item.id);

      toast.success(`${subTab.charAt(0).toUpperCase() + subTab.slice(1)} entry deleted.`);
      fetchTabData();
    } catch (e) {
      toast.error(`Failed to delete record: ` + e.message);
    }
  };

  const handleEditClick = (item) => {
    setEditingItem(item);
    if (subTab === 'attendance') {
      setAttForm({
        studentId: item.studentId || '',
        date: item.date || '',
        status: item.status || 'PRESENT',
        presentBy: item.presentBy || 'Class Teacher'
      });
    } else if (subTab === 'homework') {
      setHwForm({
        classId: item.classId || '',
        subject: item.subject || '',
        title: item.title || '',
        instructions: item.instructions || '',
        dueDate: item.dueDate || '',
        maxMarks: item.maxMarks ? String(item.maxMarks) : '',
        assignmentScope: item.assignmentScope || 'CLASS'
      });
    } else if (subTab === 'results') {
      setResForm({
        studentId: item.studentId || '',
        examName: item.examName || '',
        overallGrade: item.overallGrade || '',
        totalPercentage: item.totalPercentage ? String(item.totalPercentage) : '',
        feedback: item.feedback || ''
      });
      setSubjectMarks(item.marks || []);
    } else if (subTab === 'discipline') {
      setDiscForm({
        studentId: item.studentId || '',
        violation: item.violation || '',
        fineAmount: item.fineAmount ? String(item.fineAmount) : '',
        actionTaken: item.actionTaken || '',
        incidentDate: item.incidentDate ? item.incidentDate.slice(0, 16) : ''
      });
    } else if (subTab === 'medical') {
      setMedForm({
        studentId: item.studentId || '',
        visitDate: item.visitDate || '',
        diagnosis: item.diagnosis || '',
        doctorName: item.doctorName || '',
        medicinesInput: item.medicines ? item.medicines.join(', ') : ''
      });
    }
  };

  if (!schoolId) {
    return <Empty icon={GraduationCap} title="Pick a school to begin" hint="Select a school context in the top bar to manage academic registries." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Navigation Headers Bar */}
      <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm justify-between items-center shrink-0">
        <div className="flex gap-1">
          <button
            onClick={() => { setSubTab('academicYears'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'academicYears' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Calendar size={16} />
            Academic Years
          </button>
          <button
            onClick={() => { setSubTab('classes'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'classes' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <BookOpenCheck size={16} />
            Classes
          </button>
          <button
            onClick={() => { setSubTab('timetable'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'timetable' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <LayoutGrid size={16} />
            Timetable
          </button>
          <button
            onClick={() => { setSubTab('attendance'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'attendance' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <CheckSquare size={16} />
            Attendance
          </button>
          <button
            onClick={() => { setSubTab('homework'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'homework' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <BookOpen size={16} />
            Homework
          </button>
          <button
            onClick={() => { setSubTab('results'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'results' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Award size={16} />
            Exam Results
          </button>
          <button
            onClick={() => { setSubTab('discipline'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'discipline' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <ShieldAlert size={16} />
            Discipline Logs
          </button>
          <button
            onClick={() => { setSubTab('medical'); handleCancelEdit(); }}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'medical' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Activity size={16} />
            Medical Roster
          </button>
        </div>
        
        <button 
          onClick={fetchTabData}
          className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold mr-2 mb-2 transition"
          title="Refresh Current Tab List"
        >
          <RefreshCw size={13} />
          Reload Tab
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto">
        {subTab === 'academicYears' && (
          <AcademicYearsScreen 
            schoolId={schoolId} 
            years={years} 
            year={year} 
            reload={reload} 
          />
        )}

        {subTab === 'classes' && (
          <ClassesScreen 
            schoolId={schoolId} 
            year={year} 
          />
        )}

        {subTab === 'timetable' && (
          <div className="flex flex-col h-full gap-4">
            <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm shrink-0">
              <button
                onClick={() => setTimetableSubTab('view')}
                className={`px-4 py-2 text-sm font-semibold border-b-2 transition-colors -mb-px ${timetableSubTab === 'view' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
              >
                View Schedule
              </button>
              <button
                onClick={() => setTimetableSubTab('build')}
                className={`px-4 py-2 text-sm font-semibold border-b-2 transition-colors -mb-px ${timetableSubTab === 'build' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
              >
                Build Timetable
              </button>
            </div>
            <div className="flex-1 overflow-y-auto min-h-0 bg-white border border-slate-200 rounded-b-xl p-4 shadow-sm">
              {timetableSubTab === 'view' ? (
                <TimetableView
                  schoolId={schoolId}
                  classes={classes}
                  staff={staff || []}
                />
              ) : (
                <TimetableBuilder
                  schoolId={schoolId}
                  year={year}
                  yearDoc={years ? years.find((y) => y.name === year) : null}
                  classes={classes}
                  staff={staff || []}
                />
              )}
            </div>
          </div>
        )}

        {subTab !== 'academicYears' && subTab !== 'classes' && subTab !== 'timetable' && (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
          {/* LEFT PANEL: TABLE DISPLAY LIST */}
          <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-slate-800 text-sm">
                  {subTab.charAt(0).toUpperCase() + subTab.slice(1)} Dashboard List
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Scope context: School and year {year || 'General'}.
                </p>
              </div>
            </header>

            <div className="flex-1 overflow-x-auto">
              {loadingData ? (
                <Empty icon={RefreshCw} title="Loading academic entries..." hint="Please wait." />
              ) : (
                <>
                  {/* ATTENDANCE TABLE */}
                  {subTab === 'attendance' && (
                    attendance.length === 0 ? (
                      <Empty icon={CheckSquare} title="No attendance logs found" hint="Log attendance for a student using the dashboard panel on the right." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student Name</th>
                            <th className="px-4 py-3">Log Date</th>
                            <th className="px-4 py-3">Recorded By</th>
                            <th className="px-4 py-3 text-center">Status</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {attendance.map((it) => (
                            <tr key={it.id} className="hover:bg-slate-50/50 transition">
                              <td className="px-4 py-3 font-bold text-slate-900">{getStudentName(it.studentId)}</td>
                              <td className="px-4 py-3">{it.date ? new Date(it.date).toLocaleDateString() : '—'}</td>
                              <td className="px-4 py-3 text-slate-400 font-medium">{it.presentBy || '—'}</td>
                              <td className="px-4 py-3 text-center">
                                <Badge color={it.status === 'PRESENT' ? 'green' : it.status === 'ABSENT' ? 'rose' : 'amber'}>
                                  {it.status}
                                </Badge>
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-1.5">
                                  <button onClick={() => handleEditClick(it)} className="text-slate-500 hover:text-blue-600 p-1 rounded-lg transition"><Edit2 size={13} /></button>
                                  <button onClick={() => deleteItem(it)} className="text-slate-400 hover:text-rose-600 p-1 rounded-lg transition"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )
                  )}

                  {/* HOMEWORK TABLE */}
                  {subTab === 'homework' && (
                    homework.length === 0 ? (
                      <Empty icon={BookOpen} title="No homework logs found" hint="Assign new homework using the panel on the right." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Class & Subject</th>
                            <th className="px-4 py-3">Assignment Title</th>
                            <th className="px-4 py-3">Due Date</th>
                            <th className="px-4 py-3 text-center">Max Marks</th>
                            <th className="px-4 py-3 text-center">Scope</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {homework.map((it) => (
                            <tr key={it.id} className="hover:bg-slate-50/50 transition">
                              <td className="px-4 py-3">
                                <div className="font-bold text-slate-900">{it.subject}</div>
                                <div className="text-slate-400 text-[10px]">{getClassName(it.classId)}</div>
                              </td>
                              <td className="px-4 py-3 font-semibold text-slate-800">{it.title}</td>
                              <td className="px-4 py-3">{it.dueDate ? new Date(it.dueDate).toLocaleDateString() : '—'}</td>
                              <td className="px-4 py-3 text-center font-mono font-bold">{it.maxMarks || '—'}</td>
                              <td className="px-4 py-3 text-center"><Badge color={it.assignmentScope === 'CLASS' ? 'blue' : 'purple'}>{it.assignmentScope}</Badge></td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-1.5">
                                  <button onClick={() => handleEditClick(it)} className="text-slate-500 hover:text-blue-600 p-1 rounded-lg transition"><Edit2 size={13} /></button>
                                  <button onClick={() => deleteItem(it)} className="text-slate-400 hover:text-rose-600 p-1 rounded-lg transition"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )
                  )}

                  {/* EXAM RESULTS TABLE */}
                  {subTab === 'results' && (
                    results.length === 0 ? (
                      <Empty icon={Award} title="No academic reports found" hint="Log student grades and marks on the right." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student</th>
                            <th className="px-4 py-3">Exam Name</th>
                            <th className="px-4 py-3">Overall Grade</th>
                            <th className="px-4 py-3 text-center">Score %</th>
                            <th className="px-4 py-3">Subjects Marks</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {results.map((it) => (
                            <tr key={it.id} className="hover:bg-slate-50/50 transition">
                              <td className="px-4 py-3 font-bold text-slate-900">{getStudentName(it.studentId)}</td>
                              <td className="px-4 py-3">{it.examName}</td>
                              <td className="px-4 py-3"><Badge color="green">{it.overallGrade || '—'}</Badge></td>
                              <td className="px-4 py-3 text-center font-mono font-bold text-slate-900">{it.totalPercentage ? `${it.totalPercentage}%` : '—'}</td>
                              <td className="px-4 py-3 min-w-[150px]">
                                <div className="flex flex-wrap gap-1">
                                  {(it.marks || []).map((m, i) => (
                                    <span key={i} className="text-[10px] font-semibold bg-slate-100 border border-slate-200 rounded px-1 text-slate-600">
                                      {m.subject}: {m.obtainedMarks}/{m.maxMarks}
                                    </span>
                                  ))}
                                </div>
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-1.5">
                                  <button onClick={() => handleEditClick(it)} className="text-slate-500 hover:text-blue-600 p-1 rounded-lg transition"><Edit2 size={13} /></button>
                                  <button onClick={() => deleteItem(it)} className="text-slate-400 hover:text-rose-600 p-1 rounded-lg transition"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )
                  )}

                  {/* DISCIPLINE TABLE */}
                  {subTab === 'discipline' && (
                    discipline.length === 0 ? (
                      <Empty icon={ShieldAlert} title="No disciplinary log entries" hint="Record student violation logs on the right." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student Name</th>
                            <th className="px-4 py-3">Incident Date</th>
                            <th className="px-4 py-3">Violation</th>
                            <th className="px-4 py-3">Action Taken</th>
                            <th className="px-4 py-3 text-right">Fine</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {discipline.map((it) => (
                            <tr key={it.id} className="hover:bg-slate-50/50 transition">
                              <td className="px-4 py-3 font-bold text-slate-900">{getStudentName(it.studentId)}</td>
                              <td className="px-4 py-3 text-slate-400">{it.incidentDate ? new Date(it.incidentDate).toLocaleString() : '—'}</td>
                              <td className="px-4 py-3 text-slate-800 font-semibold">{it.violation}</td>
                              <td className="px-4 py-3 font-medium text-slate-600">{it.actionTaken || '—'}</td>
                              <td className="px-4 py-3 text-right font-mono font-bold text-rose-600">
                                {it.fineAmount ? `$${Number(it.fineAmount).toFixed(2)}` : '—'}
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-1.5">
                                  <button onClick={() => handleEditClick(it)} className="text-slate-500 hover:text-blue-600 p-1 rounded-lg transition"><Edit2 size={13} /></button>
                                  <button onClick={() => deleteItem(it)} className="text-slate-400 hover:text-rose-600 p-1 rounded-lg transition"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )
                  )}

                  {/* CLINIC/MEDICAL TABLE */}
                  {subTab === 'medical' && (
                    medical.length === 0 ? (
                      <Empty icon={Heart} title="No medical records found" hint="Log student medical visits and medication details on the right." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student Name</th>
                            <th className="px-4 py-3">Visit Date</th>
                            <th className="px-4 py-3">Diagnosis</th>
                            <th className="px-4 py-3">Attending Doctor</th>
                            <th className="px-4 py-3">Prescription</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {medical.map((it) => (
                            <tr key={it.id} className="hover:bg-slate-50/50 transition">
                              <td className="px-4 py-3 font-bold text-slate-900">{getStudentName(it.studentId)}</td>
                              <td className="px-4 py-3 text-slate-400">{it.visitDate ? new Date(it.visitDate).toLocaleDateString() : '—'}</td>
                              <td className="px-4 py-3 font-semibold text-rose-800">{it.diagnosis}</td>
                              <td className="px-4 py-3 text-slate-600">{it.doctorName || 'School Nurse'}</td>
                              <td className="px-4 py-3">
                                <div className="flex flex-wrap gap-1">
                                  {(it.medicines || []).map((m, idx) => (
                                    <span key={idx} className="bg-rose-50 border border-rose-200 text-rose-700 rounded px-1 py-0.5 text-[9px] font-semibold">
                                      {m}
                                    </span>
                                  ))}
                                </div>
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-1.5">
                                  <button onClick={() => handleEditClick(it)} className="text-slate-500 hover:text-blue-600 p-1 rounded-lg transition"><Edit2 size={13} /></button>
                                  <button onClick={() => deleteItem(it)} className="text-slate-400 hover:text-rose-600 p-1 rounded-lg transition"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )
                  )}
                </>
              )}
            </div>
          </div>

          {/* RIGHT PANEL: ADD/EDIT FORM CARDS */}
          <div className="xl:col-span-4">
            {/* 1. ATTENDANCE FORM */}
            {subTab === 'attendance' && (
              <Card
                title={editingItem ? "Edit Attendance" : "Mark Attendance"}
                subtitle={editingItem ? `Modifying log entry` : "Mark daily presence/absence status for a student."}
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students registered" hint="Setup student rosters first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Select Student *">
                      <Select 
                        value={attForm.studentId}
                        onChange={(e) => setAttForm({...attForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.name} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Date *">
                        <Input 
                          type="date"
                          value={attForm.date}
                          onChange={(e) => setAttForm({...attForm, date: e.target.value})}
                        />
                      </Field>
                      <Field label="Recorded By">
                        <Input 
                          value={attForm.presentBy}
                          onChange={(e) => setAttForm({...attForm, presentBy: e.target.value})}
                          placeholder="e.g. Teacher"
                        />
                      </Field>
                    </div>

                    <Field label="Attendance Status *">
                      <Select 
                        value={attForm.status}
                        onChange={(e) => setAttForm({...attForm, status: e.target.value})}
                      >
                        <option value="PRESENT">PRESENT</option>
                        <option value="ABSENT">ABSENT</option>
                        <option value="LATE">LATE</option>
                        <option value="EXCUSED">EXCUSED</option>
                      </Select>
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitAttendance} disabled={busy || !attForm.studentId || !attForm.date}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Log' : 'Mark Log'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* 2. HOMEWORK FORM */}
            {subTab === 'homework' && (
              <Card
                title={editingItem ? "Edit Homework" : "Assign Homework"}
                subtitle={editingItem ? `Modifying task` : "Assign new homework tasks to a class."}
              >
                {classes.length === 0 ? (
                  <Empty icon={BookOpen} title="No classes found" hint="Register classes in the school first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Target Class *">
                      <Select 
                        value={hwForm.classId}
                        onChange={(e) => setHwForm({...hwForm, classId: e.target.value})}
                      >
                        {classes.map(c => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Subject *">
                        <Input 
                          value={hwForm.subject}
                          onChange={(e) => setHwForm({...hwForm, subject: e.target.value})}
                          placeholder="e.g. Mathematics"
                        />
                      </Field>
                      <Field label="Due Date *">
                        <Input 
                          type="date"
                          value={hwForm.dueDate}
                          onChange={(e) => setHwForm({...hwForm, dueDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Max Marks">
                        <Input 
                          type="number"
                          value={hwForm.maxMarks}
                          onChange={(e) => setHwForm({...hwForm, maxMarks: e.target.value})}
                        />
                      </Field>
                      <Field label="Assignment Scope">
                        <Select 
                          value={hwForm.assignmentScope}
                          onChange={(e) => setHwForm({...hwForm, assignmentScope: e.target.value})}
                        >
                          <option value="CLASS">CLASS-WIDE</option>
                          <option value="STUDENT">SPECIFIC STUDENTS</option>
                        </Select>
                      </Field>
                    </div>

                    <Field label="Homework Title *">
                      <Input 
                        value={hwForm.title}
                        onChange={(e) => setHwForm({...hwForm, title: e.target.value})}
                        placeholder="e.g. Algebra Exercise 4.2"
                      />
                    </Field>

                    <Field label="Instructions">
                      <textarea 
                        className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[70px] text-slate-800"
                        value={hwForm.instructions}
                        onChange={(e) => setHwForm({...hwForm, instructions: e.target.value})}
                        placeholder="Write assignment details..."
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitHomework} disabled={busy || !hwForm.classId || !hwForm.subject || !hwForm.title}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Assignment' : 'Assign'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* 3. EXAM RESULTS FORM */}
            {subTab === 'results' && (
              <Card
                title={editingItem ? "Edit Report Card" : "Add Exam Result"}
                subtitle={editingItem ? `Modifying grade sheet` : "Record exam results and grades for a student."}
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students registered" hint="Enroll student profiles first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Select Student *">
                      <Select 
                        value={resForm.studentId}
                        onChange={(e) => setResForm({...resForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.name} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Exam Name *">
                        <Input 
                          value={resForm.examName}
                          onChange={(e) => setResForm({...resForm, examName: e.target.value})}
                          placeholder="e.g. Midterm Exams"
                        />
                      </Field>
                      <Field label="Overall Grade">
                        <Input 
                          value={resForm.overallGrade}
                          onChange={(e) => setResForm({...resForm, overallGrade: e.target.value})}
                          placeholder="e.g. A+"
                        />
                      </Field>
                    </div>

                    <Field label="Subject Mark Sheets *">
                      <div className="border border-slate-200 rounded-xl p-3 bg-slate-50/50 space-y-2.5">
                        {subjectMarks.length > 0 && (
                          <div className="space-y-1">
                            {subjectMarks.map((m, idx) => (
                              <div key={idx} className="flex justify-between items-center text-xs bg-white border border-slate-100 rounded px-2.5 py-1 text-slate-700">
                                <span>{m.subject} : <b className="text-slate-900">{m.obtainedMarks}</b> / {m.maxMarks}</span>
                                <button onClick={() => removeSubjectMarkRow(idx)} className="text-slate-400 hover:text-rose-600 transition"><Trash2 size={12} /></button>
                              </div>
                            ))}
                          </div>
                        )}

                        <div className="grid grid-cols-3 gap-2">
                          <Input 
                            value={newSubj.subject} 
                            onChange={(e) => setNewSubj({...newSubj, subject: e.target.value})} 
                            placeholder="Math" 
                            className="text-xs px-2 py-1"
                          />
                          <Input 
                            type="number" 
                            value={newSubj.obtainedMarks} 
                            onChange={(e) => setNewSubj({...newSubj, obtainedMarks: e.target.value})} 
                            placeholder="Marks" 
                            className="text-xs px-2 py-1"
                          />
                          <Button variant="default" size="sm" onClick={addSubjectMarkRow} className="justify-center h-8 text-[11px]"><Plus size={11} /> Add</Button>
                        </div>
                      </div>
                    </Field>

                    <Field label="Feedback / Teacher Notes">
                      <textarea 
                        className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[60px] text-slate-800"
                        value={resForm.feedback}
                        onChange={(e) => setResForm({...resForm, feedback: e.target.value})}
                        placeholder="Write remarks or comments..."
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitAcademicResult} disabled={busy || !resForm.studentId || !resForm.examName}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Results' : 'Add Report'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* 4. DISCIPLINE LOG FORM */}
            {subTab === 'discipline' && (
              <Card
                title={editingItem ? "Edit Violation Log" : "Log Disciplinary Incident"}
                subtitle={editingItem ? `Modifying incident detail` : "Log student behavioral violation incident and penalties."}
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students registered" hint="Enroll student profiles first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Select Student *">
                      <Select 
                        value={discForm.studentId}
                        onChange={(e) => setDiscForm({...discForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.name} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <Field label="Behavior Violation *">
                      <Input 
                        value={discForm.violation}
                        onChange={(e) => setDiscForm({...discForm, violation: e.target.value})}
                        placeholder="e.g. Unexcused absence, property damage"
                      />
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Fine Amount ($)">
                        <Input 
                          type="number"
                          value={discForm.fineAmount}
                          onChange={(e) => setDiscForm({...discForm, fineAmount: e.target.value})}
                          placeholder="e.g. 50"
                        />
                      </Field>
                      <Field label="Incident Date *">
                        <Input 
                          type="datetime-local"
                          value={discForm.incidentDate}
                          onChange={(e) => setDiscForm({...discForm, incidentDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <Field label="Action Taken">
                      <Input 
                        value={discForm.actionTaken}
                        onChange={(e) => setDiscForm({...discForm, actionTaken: e.target.value})}
                        placeholder="e.g. Parent call, detention warning"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitDisciplineLog} disabled={busy || !discForm.studentId || !discForm.violation}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Log' : 'Register Incident'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* 5. MEDICAL RECORD FORM */}
            {subTab === 'medical' && (
              <Card
                title={editingItem ? "Edit Medical Record" : "Add Medical Clinic Log"}
                subtitle={editingItem ? `Modifying clinic log` : "Record health clinic visits and medication."}
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students registered" hint="Enroll student profiles first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Select Student *">
                      <Select 
                        value={medForm.studentId}
                        onChange={(e) => setMedForm({...medForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.name} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Attending Doctor / Nurse">
                        <Input 
                          value={medForm.doctorName}
                          onChange={(e) => setMedForm({...medForm, doctorName: e.target.value})}
                          placeholder="e.g. Dr. Adams"
                        />
                      </Field>
                      <Field label="Visit Date *">
                        <Input 
                          type="date"
                          value={medForm.visitDate}
                          onChange={(e) => setMedForm({...medForm, visitDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <Field label="Diagnosis / Symptoms *">
                      <Input 
                        value={medForm.diagnosis}
                        onChange={(e) => setMedForm({...medForm, diagnosis: e.target.value})}
                        placeholder="e.g. Mild headache, viral fever"
                      />
                    </Field>

                    <Field label="Prescribed Medicines" hint="Separate medicines with commas.">
                      <Input 
                        value={medForm.medicinesInput}
                        onChange={(e) => setMedForm({...medForm, medicinesInput: e.target.value})}
                        placeholder="e.g. Paracetamol, Ibuprofen"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitMedicalRecord} disabled={busy || !medForm.studentId || !medForm.diagnosis}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Record' : 'Log Visit'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}
          </div>
        </div>
        )}
      </div>
    </div>
  );
}
