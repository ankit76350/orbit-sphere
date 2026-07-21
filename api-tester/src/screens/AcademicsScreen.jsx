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
  const [homeworkAction, setHomeworkAction] = useState(null);
  const [homeworkActionForm, setHomeworkActionForm] = useState({
    assignmentScope: 'CLASS', studentAssignments: [], studentId: '',
    submissionText: '', submissionFileUrl: '', obtainedMarks: '', feedback: '',
  });

  // 1. Attendance Form
  const [attForm, setAttForm] = useState({
    schoolId: schoolId || '',
    academicYear: year || '',
    studentId: '',
    date: new Date().toISOString().slice(0, 10),
    status: 'PRESENT',
    presentBy: 'Class Teacher',
    presentTime: ''
  });

  // 2. Homework Form
  const [hwForm, setHwForm] = useState({
    schoolId: schoolId || '',
    classId: '',
    subject: '',
    title: '',
    instructions: '',
    dueDate: new Date().toISOString().slice(0, 10),
    maxMarks: '100',
    assignmentScope: 'CLASS',
    teacherId: '',
    studentAssignments: []
  });

  // 3. Academic Results Form
  const [resForm, setResForm] = useState({
    schoolId: schoolId || '',
    academicYear: year || '',
    studentId: '',
    grade: '',
    examName: 'Midterm Exam',
    overallGrade: 'A',
    totalPercentage: '',
    feedback: ''
  });
  const [subjectMarks, setSubjectMarks] = useState([]); // Array of { subject, maxMarks, obtainedMarks }
  const [newSubj, setNewSubj] = useState({ subject: '', maxMarks: '100', obtainedMarks: '', grade: '' });

  // 4. Discipline Form
  const [discForm, setDiscForm] = useState({
    schoolId: schoolId || '',
    academicYear: year || '',
    studentId: '',
    violation: '',
    fineAmount: '',
    actionTaken: '',
    incidentDate: new Date().toISOString().slice(0, 16) // datetime-local format
  });

  // 5. Medical Form
  const [medForm, setMedForm] = useState({
    schoolId: schoolId || '',
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
    setAttForm((form) => ({ ...form, schoolId: schoolId || '', academicYear: year || '' }));
    setHwForm((form) => ({ ...form, schoolId: schoolId || '' }));
    setResForm((form) => ({ ...form, schoolId: schoolId || '', academicYear: year || '' }));
    setDiscForm((form) => ({ ...form, schoolId: schoolId || '', academicYear: year || '' }));
    setMedForm((form) => ({ ...form, schoolId: schoolId || '' }));
  }, [schoolId, year]);

  useEffect(() => {
    fetchTabData();
  }, [fetchTabData]);

  const handleCancelEdit = () => {
    setEditingItem(null);
    // Reset forms
    if (subTab === 'attendance') {
      setAttForm({
        schoolId: schoolId || '',
        academicYear: year || '',
        studentId: students.length > 0 ? students[0].id : '',
        date: new Date().toISOString().slice(0, 10),
        status: 'PRESENT',
        presentBy: 'Class Teacher',
        presentTime: ''
      });
    } else if (subTab === 'homework') {
      setHwForm({
        schoolId: schoolId || '',
        classId: classes.length > 0 ? classes[0].id : '',
        subject: '',
        title: '',
        instructions: '',
        dueDate: new Date().toISOString().slice(0, 10),
        maxMarks: '100',
        assignmentScope: 'CLASS',
        teacherId: '',
        studentAssignments: []
      });
    } else if (subTab === 'results') {
      setResForm({
        schoolId: schoolId || '',
        academicYear: year || '',
        studentId: students.length > 0 ? students[0].id : '',
        grade: '',
        examName: 'Midterm Exam',
        overallGrade: 'A',
        totalPercentage: '',
        feedback: ''
      });
      setSubjectMarks([]);
    } else if (subTab === 'discipline') {
      setDiscForm({
        schoolId: schoolId || '',
        academicYear: year || '',
        studentId: students.length > 0 ? students[0].id : '',
        violation: '',
        fineAmount: '',
        actionTaken: '',
        incidentDate: new Date().toISOString().slice(0, 16)
      });
    } else if (subTab === 'medical') {
      setMedForm({
        schoolId: schoolId || '',
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
      const payload = {
        ...attForm,
        academicYear: attForm.academicYear || null,
        presentTime: attForm.presentTime || null,
      };
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
    if (!hwForm.classId) {
      toast.error("Class is required.");
      return;
    }
    setBusy(true);
    try {
      const payload = {
        ...hwForm,
        dueDate: hwForm.dueDate || null,
        assignmentScope: hwForm.assignmentScope || null,
        teacherId: hwForm.teacherId || null,
        maxMarks: hwForm.maxMarks ? parseInt(hwForm.maxMarks) : null,
        studentAssignments: hwForm.studentAssignments.map((assignment) => ({
          studentId: assignment.studentId,
          customInstructions: assignment.customInstructions || null,
        })),
      };

      if (editingItem) {
        const { studentAssignments: _createOnlyAssignments, ...updatePayload } = payload;
        await api.updateHomework(editingItem.id, updatePayload);
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
    setSubjectMarks([...subjectMarks, {
      subject: newSubj.subject || null,
      maxMarks: newSubj.maxMarks === '' ? null : parseInt(newSubj.maxMarks),
      obtainedMarks: newSubj.obtainedMarks === '' ? null : parseInt(newSubj.obtainedMarks),
      grade: newSubj.grade || null
    }]);
    setNewSubj({ subject: '', maxMarks: '100', obtainedMarks: '', grade: '' });
  };

  const removeSubjectMarkRow = (idx) => {
    setSubjectMarks(subjectMarks.filter((_, i) => i !== idx));
  };

  const openHomeworkActions = (item) => {
    setHomeworkAction(item);
    setHomeworkActionForm({
      assignmentScope: item.assignmentScope || 'CLASS',
      studentAssignments: (item.studentAssignments || []).map((assignment) => ({
        studentId: assignment.studentId || '', customInstructions: assignment.customInstructions || '',
      })),
      studentId: students[0]?.id || '', submissionText: '', submissionFileUrl: '', obtainedMarks: '', feedback: '',
    });
  };

  const runHomeworkAction = async (kind) => {
    setBusy(true);
    try {
      if (kind === 'assign') {
        await api.assignHomework(homeworkAction.id, homeworkActionForm.assignmentScope, homeworkActionForm.studentAssignments);
        toast.success('Homework assignment payload accepted.');
      } else if (kind === 'submit') {
        if (!homeworkActionForm.studentId) throw new Error('Choose the student path parameter.');
        await api.submitHomework(homeworkAction.id, homeworkActionForm.studentId, homeworkActionForm.submissionText || null, homeworkActionForm.submissionFileUrl || null);
        toast.success('Homework submission payload accepted.');
      } else {
        if (!homeworkActionForm.studentId) throw new Error('Choose the student path parameter.');
        await api.gradeHomework(
          homeworkAction.id,
          homeworkActionForm.studentId,
          homeworkActionForm.obtainedMarks === '' ? null : parseInt(homeworkActionForm.obtainedMarks),
          homeworkActionForm.feedback || null,
        );
        toast.success('Homework grade payload accepted.');
      }
      fetchTabData();
    } catch (e) { toast.error(e.message || `Failed to ${kind} homework.`); }
    finally { setBusy(false); }
  };

  const submitAcademicResult = async () => {
    if (!resForm.studentId) {
      toast.error("Student is required.");
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
        ...resForm,
        academicYear: resForm.academicYear || null,
        marks: subjectMarks,
        totalPercentage: computedPercentage ?? (resForm.totalPercentage ? parseFloat(resForm.totalPercentage) : null)
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
    if (!discForm.studentId) {
      toast.error("Student is required.");
      return;
    }
    setBusy(true);
    try {
      const payload = {
        ...discForm,
        academicYear: discForm.academicYear || null,
        fineAmount: discForm.fineAmount ? parseFloat(discForm.fineAmount) : null,
        incidentDate: discForm.incidentDate || null
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
    if (!medForm.studentId) {
      toast.error("Student is required.");
      return;
    }
    setBusy(true);
    try {
      const medicines = medForm.medicinesInput 
        ? medForm.medicinesInput.split(',').map(m => m.trim()).filter(Boolean)
        : [];
      
      const { medicinesInput: _displayMedicines, ...medicalFields } = medForm;
      const payload = {
        ...medicalFields,
        visitDate: medicalFields.visitDate || null,
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
        schoolId: item.schoolId || schoolId || '',
        academicYear: item.academicYear || year || '',
        studentId: item.studentId || '',
        date: item.date || '',
        status: item.status || 'PRESENT',
        presentBy: item.presentBy || 'Class Teacher',
        presentTime: item.presentTime ? item.presentTime.slice(0, 16) : ''
      });
    } else if (subTab === 'homework') {
      setHwForm({
        schoolId: item.schoolId || schoolId || '',
        classId: item.classId || '',
        subject: item.subject || '',
        title: item.title || '',
        instructions: item.instructions || '',
        dueDate: item.dueDate || '',
        maxMarks: item.maxMarks ? String(item.maxMarks) : '',
        assignmentScope: item.assignmentScope || 'CLASS',
        teacherId: item.teacherId || '',
        studentAssignments: item.studentAssignments || []
      });
    } else if (subTab === 'results') {
      setResForm({
        schoolId: item.schoolId || schoolId || '',
        academicYear: item.academicYear || year || '',
        studentId: item.studentId || '',
        grade: item.grade || '',
        examName: item.examName || '',
        overallGrade: item.overallGrade || '',
        totalPercentage: item.totalPercentage ? String(item.totalPercentage) : '',
        feedback: item.feedback || ''
      });
      setSubjectMarks(item.marks || []);
    } else if (subTab === 'discipline') {
      setDiscForm({
        schoolId: item.schoolId || schoolId || '',
        academicYear: item.academicYear || year || '',
        studentId: item.studentId || '',
        violation: item.violation || '',
        fineAmount: item.fineAmount ? String(item.fineAmount) : '',
        actionTaken: item.actionTaken || '',
        incidentDate: item.incidentDate ? item.incidentDate.slice(0, 16) : ''
      });
    } else if (subTab === 'medical') {
      setMedForm({
        schoolId: item.schoolId || schoolId || '',
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
            staff={staff}
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
                                  <button onClick={() => openHomeworkActions(it)} className="text-slate-500 hover:text-emerald-600 p-1 rounded-lg transition" title="Test assign, submit, and grade payloads"><BookOpenCheck size={13} /></button>
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
                    <Field label="School ID" apiName="schoolId" required><Input value={attForm.schoolId} onChange={(e) => setAttForm({ ...attForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                    <Field label="Academic Year" apiName="academicYear" required={false}><Input value={attForm.academicYear} onChange={(e) => setAttForm({ ...attForm, academicYear: e.target.value })} /></Field>
                    <Field label="Select Student" apiName="studentId" required>
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
                      <Field label="Date" apiName="date" required>
                        <Input 
                          type="date"
                          value={attForm.date}
                          onChange={(e) => setAttForm({...attForm, date: e.target.value})}
                        />
                      </Field>
                      <Field label="Recorded By" apiName="presentBy" required={false}>
                        <Input 
                          value={attForm.presentBy}
                          onChange={(e) => setAttForm({...attForm, presentBy: e.target.value})}
                          placeholder="e.g. Teacher"
                        />
                      </Field>
                    </div>

                    <Field label="Attendance Status" apiName="status" required>
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

                    <Field label="Present Time" apiName="presentTime" required={false}>
                      <Input type="datetime-local" value={attForm.presentTime} onChange={(e) => setAttForm({ ...attForm, presentTime: e.target.value })} />
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
                  <div className="space-y-4">
                    <Field label="School ID" apiName="schoolId" required><Input value={hwForm.schoolId} onChange={(e) => setHwForm({ ...hwForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                    <Field label="Target Class" apiName="classId" required>
                      <Select 
                        value={hwForm.classId}
                        onChange={(e) => setHwForm({...hwForm, classId: e.target.value})}
                      >
                        <option value="">— select or create a class first —</option>
                        {classes.map(c => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Subject" apiName="subject" required={false}>
                        <Input 
                          value={hwForm.subject}
                          onChange={(e) => setHwForm({...hwForm, subject: e.target.value})}
                          placeholder="e.g. Mathematics"
                        />
                      </Field>
                      <Field label="Due Date" apiName="dueDate" required={false}>
                        <Input 
                          type="date"
                          value={hwForm.dueDate}
                          onChange={(e) => setHwForm({...hwForm, dueDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Max Marks" apiName="maxMarks" required={false}>
                        <Input 
                          type="number"
                          value={hwForm.maxMarks}
                          onChange={(e) => setHwForm({...hwForm, maxMarks: e.target.value})}
                        />
                      </Field>
                      <Field label="Assignment Scope" apiName="assignmentScope" required={false}>
                        <Select 
                          value={hwForm.assignmentScope}
                          onChange={(e) => setHwForm({...hwForm, assignmentScope: e.target.value})}
                        >
                          <option value="">— omitted —</option>
                          <option value="CLASS">CLASS-WIDE</option>
                          <option value="GROUP">GROUP</option>
                          <option value="INDIVIDUAL">INDIVIDUAL</option>
                        </Select>
                      </Field>
                    </div>

                    <Field label="Teacher ID" apiName="teacherId" required={false}>
                      <Select value={hwForm.teacherId} onChange={(e) => setHwForm({ ...hwForm, teacherId: e.target.value })}>
                        <option value="">— omitted —</option>
                        {staff.map((member) => <option key={member.id} value={member.id}>{member.name || member.employeeId || member.id}</option>)}
                      </Select>
                    </Field>

                    <Field label="Homework Title" apiName="title" required={false}>
                      <Input 
                        value={hwForm.title}
                        onChange={(e) => setHwForm({...hwForm, title: e.target.value})}
                        placeholder="e.g. Algebra Exercise 4.2"
                      />
                    </Field>

                    <Field label="Instructions" apiName="instructions" required={false}>
                      <textarea 
                        className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[70px] text-slate-800"
                        value={hwForm.instructions}
                        onChange={(e) => setHwForm({...hwForm, instructions: e.target.value})}
                        placeholder="Write assignment details..."
                      />
                    </Field>

                    {!editingItem && <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-600">Student assignments <code className="font-mono text-[10px] font-medium text-slate-400">studentAssignments[]</code> <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span></span>
                        <button type="button" onClick={() => setHwForm({ ...hwForm, studentAssignments: [...hwForm.studentAssignments, { studentId: '', customInstructions: '' }] })} className="text-[11px] font-semibold text-blue-600 flex items-center gap-1"><Plus size={11} /> Add</button>
                      </div>
                      {hwForm.studentAssignments.map((assignment, index) => (
                        <div key={index} className="grid grid-cols-[1fr_1fr_auto] gap-2">
                          <Select value={assignment.studentId} onChange={(e) => setHwForm({ ...hwForm, studentAssignments: hwForm.studentAssignments.map((item, i) => i === index ? { ...item, studentId: e.target.value } : item) })}>
                            <option value="">studentId (required per row)</option>
                            {students.map((student) => <option key={student.id} value={student.id}>{student.name}</option>)}
                          </Select>
                          <Input value={assignment.customInstructions} onChange={(e) => setHwForm({ ...hwForm, studentAssignments: hwForm.studentAssignments.map((item, i) => i === index ? { ...item, customInstructions: e.target.value } : item) })} placeholder="customInstructions" />
                          <button type="button" onClick={() => setHwForm({ ...hwForm, studentAssignments: hwForm.studentAssignments.filter((_, i) => i !== index) })} className="text-slate-400 hover:text-rose-600"><X size={14} /></button>
                        </div>
                      ))}
                    </div>}

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitHomework} disabled={busy || !hwForm.classId}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingItem ? 'Save Assignment' : 'Assign'}
                      </Button>
                    </div>
                  </div>
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
                    <Field label="School ID" apiName="schoolId" required><Input value={resForm.schoolId} onChange={(e) => setResForm({ ...resForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                    <Field label="Academic Year" apiName="academicYear" required={false}><Input value={resForm.academicYear} onChange={(e) => setResForm({ ...resForm, academicYear: e.target.value })} /></Field>
                    <Field label="Select Student" apiName="studentId" required>
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
                      <Field label="Exam Name" apiName="examName" required={false}>
                        <Input 
                          value={resForm.examName}
                          onChange={(e) => setResForm({...resForm, examName: e.target.value})}
                          placeholder="e.g. Midterm Exams"
                        />
                      </Field>
                      <Field label="Overall Grade" apiName="overallGrade" required={false}>
                        <Input 
                          value={resForm.overallGrade}
                          onChange={(e) => setResForm({...resForm, overallGrade: e.target.value})}
                          placeholder="e.g. A+"
                        />
                      </Field>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Grade / Class" apiName="grade" required={false}><Input value={resForm.grade} onChange={(e) => setResForm({ ...resForm, grade: e.target.value })} placeholder="e.g. Grade 6" /></Field>
                      <Field label="Total Percentage" apiName="totalPercentage" required={false} hint="Auto-computed from marks when rows exist."><Input type="number" value={resForm.totalPercentage} onChange={(e) => setResForm({ ...resForm, totalPercentage: e.target.value })} /></Field>
                    </div>

                    <Field label="Subject Mark Sheets" apiName="marks[]" required={false}>
                      <div className="border border-slate-200 rounded-xl p-3 bg-slate-50/50 space-y-2.5">
                        {subjectMarks.length > 0 && (
                          <div className="space-y-1">
                            {subjectMarks.map((m, idx) => (
                              <div key={idx} className="grid grid-cols-[1.2fr_.8fr_.8fr_.7fr_auto] gap-1.5 items-center bg-white border border-slate-100 rounded p-1.5">
                                <Input value={m.subject ?? ''} onChange={(e) => setSubjectMarks(subjectMarks.map((item, i) => i === idx ? { ...item, subject: e.target.value } : item))} placeholder="subject" className="!px-2 !py-1 text-[10px]" />
                                <Input type="number" value={m.maxMarks ?? ''} onChange={(e) => setSubjectMarks(subjectMarks.map((item, i) => i === idx ? { ...item, maxMarks: e.target.value === '' ? null : Number(e.target.value) } : item))} placeholder="max" className="!px-2 !py-1 text-[10px]" />
                                <Input type="number" value={m.obtainedMarks ?? ''} onChange={(e) => setSubjectMarks(subjectMarks.map((item, i) => i === idx ? { ...item, obtainedMarks: e.target.value === '' ? null : Number(e.target.value) } : item))} placeholder="obtained" className="!px-2 !py-1 text-[10px]" />
                                <Input value={m.grade ?? ''} onChange={(e) => setSubjectMarks(subjectMarks.map((item, i) => i === idx ? { ...item, grade: e.target.value } : item))} placeholder="grade" className="!px-2 !py-1 text-[10px]" />
                                <button onClick={() => removeSubjectMarkRow(idx)} className="text-slate-400 hover:text-rose-600 transition"><Trash2 size={12} /></button>
                              </div>
                            ))}
                          </div>
                        )}

                        <div className="grid grid-cols-2 gap-2">
                          <Input
                            value={newSubj.subject} 
                            onChange={(e) => setNewSubj({...newSubj, subject: e.target.value})} 
                            placeholder="Math" 
                            className="text-xs px-2 py-1"
                          />
                          <Input 
                            type="number"
                            value={newSubj.maxMarks}
                            onChange={(e) => setNewSubj({...newSubj, maxMarks: e.target.value})}
                            placeholder="Max marks"
                            className="text-xs px-2 py-1"
                          />
                          <Input
                            type="number"
                            value={newSubj.obtainedMarks}
                            onChange={(e) => setNewSubj({...newSubj, obtainedMarks: e.target.value})}
                            placeholder="Marks"
                            className="text-xs px-2 py-1"
                          />
                          <Input value={newSubj.grade} onChange={(e) => setNewSubj({ ...newSubj, grade: e.target.value })} placeholder="Grade (optional)" className="text-xs px-2 py-1" />
                          <Button variant="default" size="sm" onClick={addSubjectMarkRow} className="justify-center h-8 text-[11px]"><Plus size={11} /> Add</Button>
                        </div>
                      </div>
                    </Field>

                    <Field label="Feedback / Teacher Notes" apiName="feedback" required={false}>
                      <textarea 
                        className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[60px] text-slate-800"
                        value={resForm.feedback}
                        onChange={(e) => setResForm({...resForm, feedback: e.target.value})}
                        placeholder="Write remarks or comments..."
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitAcademicResult} disabled={busy || !resForm.studentId}>
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
                    <Field label="School ID" apiName="schoolId" required><Input value={discForm.schoolId} onChange={(e) => setDiscForm({ ...discForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                    <Field label="Academic Year" apiName="academicYear" required={false}><Input value={discForm.academicYear} onChange={(e) => setDiscForm({ ...discForm, academicYear: e.target.value })} /></Field>
                    <Field label="Select Student" apiName="studentId" required>
                      <Select 
                        value={discForm.studentId}
                        onChange={(e) => setDiscForm({...discForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.name} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <Field label="Behavior Violation" apiName="violation" required={false}>
                      <Input 
                        value={discForm.violation}
                        onChange={(e) => setDiscForm({...discForm, violation: e.target.value})}
                        placeholder="e.g. Unexcused absence, property damage"
                      />
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Fine Amount ($)" apiName="fineAmount" required={false}>
                        <Input 
                          type="number"
                          value={discForm.fineAmount}
                          onChange={(e) => setDiscForm({...discForm, fineAmount: e.target.value})}
                          placeholder="e.g. 50"
                        />
                      </Field>
                      <Field label="Incident Date" apiName="incidentDate" required={false}>
                        <Input 
                          type="datetime-local"
                          value={discForm.incidentDate}
                          onChange={(e) => setDiscForm({...discForm, incidentDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <Field label="Action Taken" apiName="actionTaken" required={false}>
                      <Input 
                        value={discForm.actionTaken}
                        onChange={(e) => setDiscForm({...discForm, actionTaken: e.target.value})}
                        placeholder="e.g. Parent call, detention warning"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitDisciplineLog} disabled={busy || !discForm.studentId}>
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
                    <Field label="School ID" apiName="schoolId" required><Input value={medForm.schoolId} onChange={(e) => setMedForm({ ...medForm, schoolId: e.target.value })} className="font-mono text-xs" /></Field>
                    <Field label="Select Student" apiName="studentId" required>
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
                      <Field label="Attending Doctor / Nurse" apiName="doctorName" required={false}>
                        <Input 
                          value={medForm.doctorName}
                          onChange={(e) => setMedForm({...medForm, doctorName: e.target.value})}
                          placeholder="e.g. Dr. Adams"
                        />
                      </Field>
                      <Field label="Visit Date" apiName="visitDate" required={false}>
                        <Input 
                          type="date"
                          value={medForm.visitDate}
                          onChange={(e) => setMedForm({...medForm, visitDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <Field label="Diagnosis / Symptoms" apiName="diagnosis" required={false}>
                      <Input 
                        value={medForm.diagnosis}
                        onChange={(e) => setMedForm({...medForm, diagnosis: e.target.value})}
                        placeholder="e.g. Mild headache, viral fever"
                      />
                    </Field>

                    <Field label="Prescribed Medicines" apiName="medicines[]" required={false} hint="Separate medicines with commas.">
                      <Input 
                        value={medForm.medicinesInput}
                        onChange={(e) => setMedForm({...medForm, medicinesInput: e.target.value})}
                        placeholder="e.g. Paracetamol, Ibuprofen"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingItem && <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitMedicalRecord} disabled={busy || !medForm.studentId}>
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

      {homeworkAction && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
            <header className="px-5 py-4 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
              <div><h4 className="font-bold text-sm text-slate-800">Homework request payloads</h4><p className="text-[10px] text-slate-400">{homeworkAction.title || homeworkAction.id} · test assign, submit, and grade endpoints</p></div>
              <button onClick={() => setHomeworkAction(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 grid grid-cols-1 md:grid-cols-3 gap-4 overflow-y-auto">
              <Card title="Assign" subtitle="POST /assign">
                <div className="space-y-3">
                  <Field label="Assignment Scope" apiName="assignmentScope" required><Select value={homeworkActionForm.assignmentScope} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, assignmentScope: e.target.value })}>{['CLASS', 'GROUP', 'INDIVIDUAL'].map((scope) => <option key={scope} value={scope}>{scope}</option>)}</Select></Field>
                  <div className="flex items-center justify-between"><span className="text-xs font-semibold text-slate-600">studentAssignments[] <span className="text-[9px] uppercase text-slate-400">optional</span></span><button type="button" onClick={() => setHomeworkActionForm({ ...homeworkActionForm, studentAssignments: [...homeworkActionForm.studentAssignments, { studentId: '', customInstructions: '' }] })} className="text-[11px] text-blue-600 font-semibold"><Plus size={11} className="inline" /> Add</button></div>
                  {homeworkActionForm.studentAssignments.map((assignment, index) => <div key={index} className="border border-slate-200 rounded-lg p-2 space-y-2"><Select value={assignment.studentId} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, studentAssignments: homeworkActionForm.studentAssignments.map((item, i) => i === index ? { ...item, studentId: e.target.value } : item) })}><option value="">studentId (required)</option>{students.map((student) => <option key={student.id} value={student.id}>{student.name}</option>)}</Select><div className="flex gap-1"><Input value={assignment.customInstructions} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, studentAssignments: homeworkActionForm.studentAssignments.map((item, i) => i === index ? { ...item, customInstructions: e.target.value } : item) })} placeholder="customInstructions" className="flex-1" /><button onClick={() => setHomeworkActionForm({ ...homeworkActionForm, studentAssignments: homeworkActionForm.studentAssignments.filter((_, i) => i !== index) })} className="text-rose-500"><Trash2 size={13} /></button></div></div>)}
                  <Button variant="primary" onClick={() => runHomeworkAction('assign')} disabled={busy} className="w-full justify-center">Send assign</Button>
                </div>
              </Card>
              <Card title="Submit" subtitle="POST /submit/{studentId}">
                <div className="space-y-3">
                  <Field label="Student (path)" required><Select value={homeworkActionForm.studentId} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, studentId: e.target.value })}><option value="">— choose —</option>{students.map((student) => <option key={student.id} value={student.id}>{student.name}</option>)}</Select></Field>
                  <Field label="Submission Text" apiName="submissionText" required={false}><textarea value={homeworkActionForm.submissionText} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, submissionText: e.target.value })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg min-h-20" /></Field>
                  <Field label="Submission File URL" apiName="submissionFileUrl" required={false}><Input value={homeworkActionForm.submissionFileUrl} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, submissionFileUrl: e.target.value })} /></Field>
                  <Button variant="primary" onClick={() => runHomeworkAction('submit')} disabled={busy || !homeworkActionForm.studentId} className="w-full justify-center">Send submission</Button>
                </div>
              </Card>
              <Card title="Grade" subtitle="POST /grade/{studentId}">
                <div className="space-y-3">
                  <Field label="Student (path)" required><Select value={homeworkActionForm.studentId} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, studentId: e.target.value })}><option value="">— choose —</option>{students.map((student) => <option key={student.id} value={student.id}>{student.name}</option>)}</Select></Field>
                  <Field label="Obtained Marks" apiName="obtainedMarks" required={false}><Input type="number" min="0" value={homeworkActionForm.obtainedMarks} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, obtainedMarks: e.target.value })} /></Field>
                  <Field label="Feedback" apiName="feedback" required={false}><textarea value={homeworkActionForm.feedback} onChange={(e) => setHomeworkActionForm({ ...homeworkActionForm, feedback: e.target.value })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg min-h-20" /></Field>
                  <Button variant="primary" onClick={() => runHomeworkAction('grade')} disabled={busy || !homeworkActionForm.studentId} className="w-full justify-center">Send grade</Button>
                </div>
              </Card>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
