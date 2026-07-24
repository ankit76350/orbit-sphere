/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, getStaff, logAction } from "../storage";
import {
  Button,
  Input,
  Select,
  Dialog,
  Badge,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  useToast
} from "../components/ui";
import {
  ClipboardList,
  CalendarDays,
  Plus,
  Lock,
  Save,
  Printer,
  Send,
  GraduationCap,
  QrCode,
  CheckCircle,
  AlertCircle,
  ArrowRight,
  Users,
  BookOpen,
  Award,
  Eye,
  EyeOff,
  RefreshCw
} from "lucide-react";

const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));

const SUBJECTS = ["Mathematics", "English", "Hindi", "Science", "Social Science", "Computer Applications"];
const GRADES = ["Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"];
const INVIGILATORS = ["Mrs. Kavita Iyer", "Mr. Rajesh Nair", "Ms. Pooja Deshmukh", "Mr. Amit Chauhan", "Mrs. Sunita Rao", "Mr. Vikram Joshi"];

const hashNum = (str) => { let h = 7; for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) % 9973; return h; };
const gradeLetter = (pct) => pct >= 91 ? "A+" : pct >= 81 ? "A" : pct >= 71 ? "B" : pct >= 61 ? "C" : pct >= 45 ? "D" : "F";
const gradeBadgeVariant = (letter) => letter === "A+" || letter === "A" ? "success" : letter === "B" || letter === "C" ? "info" : letter === "D" ? "warning" : "danger";

const buildDatesheet = (startDate) => SUBJECTS.map((sub, i) => {
  const d = new Date(startDate + "T00:00:00");
  d.setDate(d.getDate() + i + (i > 2 ? 1 : 0));
  const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return {
    date: iso,
    subject: sub,
    time: i % 2 === 0 ? "09:00 AM - 12:00 PM" : "10:00 AM - 01:00 PM",
    room: `Exam Hall ${String.fromCharCode(65 + i % 3)}`,
    invigilatorName: INVIGILATORS[i % INVIGILATORS.length]
  };
});

const SEED_EXAMS = [
  { id: "exam-1", name: "Term 1 - Unit Test 1", term: "Term 1", classes: "All Classes", startDate: "2026-07-20", published: true, datesheet: buildDatesheet("2026-07-20") },
  { id: "exam-2", name: "Half Yearly Examination", term: "Term 1", classes: "All Classes", startDate: "2026-09-14", published: false, datesheet: buildDatesheet("2026-09-14") },
  { id: "exam-3", name: "Annual Board Pattern Exam", term: "Term 2", classes: "Grade 10 & Grade 12", startDate: "2026-02-16", published: true, datesheet: buildDatesheet("2026-02-16") }
];

const SEED_PROMO_HISTORY = [
  { id: "promo-2025", date: "2025-04-01", from: "2024-25", to: "2025-26", promoted: 98, detained: 4, duesCarried: "₹1,84,500", by: "Dr. Arthur Pendelton" }
];

const CO_SCHOLASTIC = [
  { area: "Work Education", grades: ["A", "A+", "B+"] },
  { area: "Art Education", grades: ["A+", "A", "A"] },
  { area: "Health & Physical Education", grades: ["A", "B+", "A+"] }
];

export default function ModExams({ user }) {
  const { addToast } = useToast();
  const [tab, setTab] = useState("dashboard");
  const [exams, setExams] = useState(() => loadLS("erp_exams", SEED_EXAMS));
  const [marksStore, setMarksStore] = useState(() => loadLS("erp_exam_marks", {}));
  const [promoHistory, setPromoHistory] = useState(() => loadLS("erp_promotion_history", SEED_PROMO_HISTORY));
  const [publishedReports, setPublishedReports] = useState(() => loadLS("erp_report_published", ["student-3", "student-8", "student-15"]));
  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());

  // ---- schedule tab state ----
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const [newTerm, setNewTerm] = useState("Term 1");
  const [newClasses, setNewClasses] = useState("All Classes");
  const [newStart, setNewStart] = useState("2026-08-10");
  const [openSheetId, setOpenSheetId] = useState("exam-1");

  // ---- admit card tab state ----
  const [acExamDocsId, setAcExamDocsId] = useState(exams[0]?.id || "");
  const [acGrade, setAcGrade] = useState("Grade 10");
  const [bulkProgress, setBulkProgress] = useState(null);

  // ---- marks entry tab state ----
  const [meExamDocsId, setMeExamDocsId] = useState(exams[0]?.id || "");
  const [meGrade, setMeGrade] = useState("Grade 10");
  const [meSubject, setMeSubject] = useState("Mathematics");

  // ---- report card tab state ----
  const [rcStudentDocsId, setRcStudentDocsId] = useState(students[0]?.id || "");

  // ---- promotion wizard state ----
  const [wizStep, setWizStep] = useState(1);
  const [detainMap, setDetainMap] = useState({});
  const [carryDues, setCarryDues] = useState(true);
  const [rollProgress, setRollProgress] = useState(null);

  const today = new Date();
  const todayIso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  const examOptions = exams.map((e) => ({ label: e.name, value: e.id }));
  const gradeOptions = GRADES.map((g) => ({ label: g, value: g }));
  const teacherNames = staff.filter((s) => s.role === "Teacher").map((s) => s.name);

  /* ============ SCHEDULE handlers ============ */
  const handleCreateExam = (e) => {
    e.preventDefault();
    if (!newName || !newStart) {
      addToast("Missing fields", "Exam name and start date are required", "error");
      return;
    }
    const exam = {
      id: `exam-${Date.now()}`,
      name: newName,
      term: newTerm,
      classes: newClasses,
      startDate: newStart,
      published: false,
      datesheet: buildDatesheet(newStart)
    };
    const updated = [...exams, exam];
    setExams(updated);
    saveLS("erp_exams", updated);
    logAction(user.id, user.name, user.role, "Exam Created", `Created exam "${newName}" (${newTerm}, ${newClasses}) with auto-generated datesheet for ${SUBJECTS.length} subjects starting ${newStart}`);
    addToast("Exam Created", `"${newName}" scheduled with a ${SUBJECTS.length}-subject datesheet`, "success");
    setIsCreateOpen(false);
    setNewName("");
    setOpenSheetId(exam.id);
  };

  const togglePublish = (exam) => {
    const updated = exams.map((e) => e.id === exam.id ? { ...e, published: !e.published } : e);
    setExams(updated);
    saveLS("erp_exams", updated);
    const nowPublished = !exam.published;
    logAction(user.id, user.name, user.role, nowPublished ? "Datesheet Published" : "Datesheet Unpublished", `${exam.name} datesheet ${nowPublished ? "published to students & parents" : "withdrawn from student portal"}`);
    addToast(nowPublished ? "Datesheet Published" : "Datesheet Unpublished", `${exam.name} is now ${nowPublished ? "visible on student & parent portals" : "hidden (draft mode)"}`, nowPublished ? "success" : "warning");
  };

  /* ============ ADMIT CARD handlers ============ */
  const acExam = exams.find((e) => e.id === acExamDocsId) || exams[0];
  const acStudents = students.filter((s) => s.grade === acGrade);

  const handleBulkGenerate = () => {
    if (bulkProgress !== null) return;
    setBulkProgress(0);
    const timer = setInterval(() => {
      setBulkProgress((p) => {
        if (p === null) return null;
        const next = p + 10;
        if (next >= 100) {
          clearInterval(timer);
          setTimeout(() => setBulkProgress(null), 900);
          addToast("Admit Cards Generated", `${acStudents.length} admit cards rendered for ${acGrade} - ${acExam ? acExam.name : ""}`, "success");
          logAction(user.id, user.name, user.role, "Admit Cards Bulk Generated", `Generated ${acStudents.length} admit cards for ${acGrade} (${acExam ? acExam.name : "exam"})`);
          return 100;
        }
        return next;
      });
    }, 130);
  };

  /* ============ MARKS ENTRY handlers ============ */
  const meKey = `${meExamDocsId}__${meGrade}__${meSubject}`;
  const sheet = marksStore[meKey] || { marks: {}, maxMarks: 100, locked: false };
  const meStudents = students.filter((s) => s.grade === meGrade);
  const meExam = exams.find((e) => e.id === meExamDocsId);

  const updateMark = (sid, val) => {
    if (sheet.locked) return;
    const next = { ...marksStore, [meKey]: { ...sheet, marks: { ...sheet.marks, [sid]: val } } };
    setMarksStore(next);
  };

  const handleSaveDraft = () => {
    saveLS("erp_exam_marks", marksStore);
    const filled = Object.values(sheet.marks).filter((v) => v !== "" && v !== undefined).length;
    logAction(user.id, user.name, user.role, "Marks Draft Saved", `Saved draft marks for ${meSubject} / ${meGrade} / ${meExam ? meExam.name : ""} (${filled}/${meStudents.length} entered)`);
    addToast("Draft Saved", `${filled} of ${meStudents.length} marks stored for ${meSubject} (${meGrade})`, "info");
  };

  const handleLockSubmit = () => {
    const next = { ...marksStore, [meKey]: { ...sheet, locked: true } };
    setMarksStore(next);
    saveLS("erp_exam_marks", next);
    logAction(user.id, user.name, user.role, "Marks Locked & Submitted", `Locked marksheet ${meSubject} / ${meGrade} / ${meExam ? meExam.name : ""}. Further edits disabled.`);
    addToast("Marksheet Locked", `${meSubject} marks for ${meGrade} submitted to Examination Cell. Editing is now disabled.`, "success");
  };

  const lockedCount = Object.values(marksStore).filter((s) => s && s.locked).length;
  const pendingSheets = Math.max(0, exams.filter((e) => e.published).length * SUBJECTS.length - lockedCount);

  /* ============ REPORT CARD helpers ============ */
  const rcStudent = students.find((s) => s.id === rcStudentDocsId);
  const rcRows = SUBJECTS.map((sub) => {
    const t1 = 35 + hashNum(rcStudentDocsId + sub + "T1") % 61;
    const t2 = 35 + hashNum(rcStudentDocsId + sub + "T2") % 61;
    const avg = Math.round((t1 + t2) / 2);
    return { subject: sub, t1, t2, avg, letter: gradeLetter(avg) };
  });
  const rcOverall = Math.round(rcRows.reduce((sum, r) => sum + r.avg, 0) / rcRows.length);
  const rcAttendance = 85 + hashNum(rcStudentDocsId + "att") % 13;
  const rcRemark = rcOverall >= 85 ? "Outstanding performance. Keep up the excellent consistency across terms." : rcOverall >= 70 ? "A very good result. Focused revision will push the weaker subjects higher." : rcOverall >= 55 ? "Satisfactory progress. Needs regular practice in core subjects." : "Requires close attention and remedial classes in core subjects.";

  const handlePublishReport = () => {
    if (!rcStudent) return;
    const updated = publishedReports.includes(rcStudentDocsId) ? publishedReports : [...publishedReports, rcStudentDocsId];
    setPublishedReports(updated);
    saveLS("erp_report_published", updated);
    logAction(user.id, user.name, user.role, "Report Card Published", `Published report card of ${rcStudent.name} (${rcStudent.admissionNo}) to parent app`);
    addToast("Published to Parent App", `${rcStudent.name}'s report card is now visible to ${rcStudent.parentName}`, "success");
  };

  /* ============ PROMOTION helpers ============ */
  const classCounts = GRADES.map((g) => ({ grade: g, count: students.filter((s) => s.grade === g).length }));
  const totalDetained = GRADES.reduce((sum, g) => sum + (parseInt(detainMap[g]) || 0), 0);
  const totalStudents = students.length;
  const totalPromoted = totalStudents - totalDetained;

  const handleFinishRollover = () => {
    if (rollProgress !== null) return;
    setRollProgress(0);
    const timer = setInterval(() => {
      setRollProgress((p) => {
        if (p === null) return null;
        const next = p + 5;
        if (next >= 100) {
          clearInterval(timer);
          const entry = {
            id: `promo-${Date.now()}`,
            date: todayIso,
            from: "2025-26",
            to: "2026-27",
            promoted: totalPromoted,
            detained: totalDetained,
            duesCarried: carryDues ? "₹2,36,400" : "₹0 (written off)",
            by: user.name
          };
          const updatedHistory = [entry, ...promoHistory];
          setPromoHistory(updatedHistory);
          saveLS("erp_promotion_history", updatedHistory);
          logAction(user.id, user.name, user.role, "Session Rollover Simulated", `Sandbox rollover 2025-26 -> 2026-27: ${totalPromoted} promoted, ${totalDetained} detained, dues ${carryDues ? "carried forward" : "not carried"}. No live records mutated.`);
          addToast("Session rolled over (sandbox simulation)", `${totalPromoted} students promoted, ${totalDetained} detained. Live student records untouched.`, "success");
          setTimeout(() => { setRollProgress(null); setWizStep(1); setDetainMap({}); }, 1200);
          return 100;
        }
        return next;
      });
    }, 70);
  };

  /* ============ dashboard metrics ============ */
  const upcomingExams = exams.filter((e) => e.startDate >= todayIso);
  const sortedExams = [...exams].sort((a, b) => a.startDate.localeCompare(b.startDate));

  return <div className="space-y-6">

      {/* Module header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <ClipboardList className="h-5 w-5 text-indigo-600" /> Examination Cell
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Datesheets, admit cards, marks entry, board-style report cards and session promotion — Academic Session 2025-26.</p>
        </div>
        <Badge variant="info" className="uppercase tracking-widest text-[10px]">Session 2025-26</Badge>
      </div>

      <Tabs activeTab={tab} onChange={setTab}>
        <TabsList>
          <TabsTrigger value="dashboard">Dashboard</TabsTrigger>
          <TabsTrigger value="schedule">Exam Schedule</TabsTrigger>
          <TabsTrigger value="admit_cards">Admit Cards</TabsTrigger>
          <TabsTrigger value="marks_entry">Marks Entry</TabsTrigger>
          <TabsTrigger value="report_cards">Report Cards</TabsTrigger>
          <TabsTrigger value="promotion">Promotion</TabsTrigger>
        </TabsList>

        {/* ================= DASHBOARD ================= */}
        <TabsContent value="dashboard">
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">UPCOMING EXAMS</span>
                  <h4 className="text-2xl font-black text-slate-800 mt-1">{upcomingExams.length}</h4>
                  <p className="text-xs text-slate-400 mt-1">Scheduled this session</p>
                </div>
                <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl"><CalendarDays className="h-6 w-6" /></div>
              </div>

              <div className="bg-indigo-900 text-indigo-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">MARKS ENTRY PENDING</span>
                  <h4 className="text-2xl font-black text-white mt-1">{pendingSheets} Sheets</h4>
                  <p className="text-xs text-indigo-200 mt-1">{lockedCount} locked & submitted</p>
                </div>
                <div className="p-3 bg-white/10 rounded-2xl text-white"><BookOpen className="h-6 w-6" /></div>
              </div>

              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">REPORT CARDS PUBLISHED</span>
                  <h4 className="text-2xl font-black text-emerald-600 mt-1">{publishedReports.length}</h4>
                  <p className="text-xs text-slate-400 mt-1">Visible in parent app</p>
                </div>
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl"><Award className="h-6 w-6" /></div>
              </div>

              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">PROMOTION STATUS</span>
                  <h4 className="text-2xl font-black text-slate-800 mt-1">2025-26</h4>
                  <p className="text-xs text-amber-600 font-semibold mt-1">{promoHistory.length} rollover run{promoHistory.length === 1 ? "" : "s"} logged</p>
                </div>
                <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl"><GraduationCap className="h-6 w-6" /></div>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Exam Calendar</h3>
              <div className="space-y-3">
                {sortedExams.map((e) => {
                  const days = Math.ceil((new Date(e.startDate) - today) / 864e5);
                  return <div key={e.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border border-slate-100 rounded-2xl p-4 hover:bg-slate-50/60 transition">
                      <div className="flex items-center gap-3">
                        <div className="h-11 w-11 rounded-2xl bg-indigo-50 text-indigo-700 flex flex-col items-center justify-center shrink-0">
                          <span className="text-sm font-black leading-none">{e.startDate.slice(8, 10)}</span>
                          <span className="text-[8px] uppercase font-extrabold tracking-wider">{new Date(e.startDate + "T00:00:00").toLocaleString("en-IN", { month: "short" })}</span>
                        </div>
                        <div>
                          <p className="text-sm font-extrabold text-slate-800">{e.name}</p>
                          <p className="text-[10px] uppercase tracking-wider font-bold text-slate-400 mt-0.5">{e.term} · {e.classes} · {e.datesheet.length} papers</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {days >= 0 ? <Badge variant="info">{days === 0 ? "Today" : `In ${days} days`}</Badge> : <Badge variant="secondary">Completed</Badge>}
                        <Badge variant={e.published ? "success" : "warning"}>{e.published ? "Published" : "Draft"}</Badge>
                      </div>
                    </div>;
                })}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= SCHEDULE ================= */}
        <TabsContent value="schedule">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div>
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Exam & Datesheet Manager</h3>
                <p className="text-xs text-slate-400 font-semibold mt-1">Create exams — a per-subject datesheet is auto-generated and can be published to the portal.</p>
              </div>
              <Button onClick={() => setIsCreateOpen(true)} className="flex gap-2 items-center text-xs py-2.5">
                <Plus className="h-4 w-4" /> Create Exam
              </Button>
            </div>

            {exams.map((e) => <div key={e.id} className="bg-white border border-slate-100 rounded-3xl p-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                  <div>
                    <h4 className="text-sm font-extrabold text-slate-800 flex items-center gap-2">
                      {e.name}
                      <Badge variant={e.published ? "success" : "warning"}>{e.published ? "Published" : "Draft"}</Badge>
                    </h4>
                    <p className="text-[10px] uppercase tracking-wider font-bold text-slate-400 mt-1">{e.term} · {e.classes} · Starts {e.startDate}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => setOpenSheetId(openSheetId === e.id ? "" : e.id)} className="flex gap-1.5 items-center">
                      {openSheetId === e.id ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                      {openSheetId === e.id ? "Hide Datesheet" : "View Datesheet"}
                    </Button>
                    <Button variant={e.published ? "danger" : "secondary"} size="sm" onClick={() => togglePublish(e)} className="flex gap-1.5 items-center">
                      <Send className="h-3.5 w-3.5" /> {e.published ? "Unpublish" : "Publish"}
                    </Button>
                  </div>
                </div>

                {openSheetId === e.id && <div className="overflow-x-auto border border-slate-100 rounded-2xl mt-4 animate-fade-in">
                    <table className="w-full text-xs font-semibold text-slate-700 text-left">
                      <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                        <tr>
                          <th className="p-4">Date</th>
                          <th className="p-4">Subject Paper</th>
                          <th className="p-4">Time</th>
                          <th className="p-4">Room</th>
                          <th className="p-4">InvigilatorName</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {e.datesheet.map((row, i) => <tr key={i}>
                            <td className="p-4 font-extrabold text-slate-800">{row.date}</td>
                            <td className="p-4"><Badge variant="secondary">{row.subject}</Badge></td>
                            <td className="p-4">{row.time}</td>
                            <td className="p-4 text-slate-500">{row.room}</td>
                            <td className="p-4 text-slate-500">{row.invigilatorName}</td>
                          </tr>)}
                      </tbody>
                    </table>
                  </div>}
              </div>)}

            <Dialog isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Examination">
              <form onSubmit={handleCreateExam} className="space-y-4 pt-1">
                <Input label="Exam Name" value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="e.g. Term 2 - Unit Test 3" required />
                <div className="grid grid-cols-2 gap-4">
                  <Select label="Term" value={newTerm} onChange={(e) => setNewTerm(e.target.value)} options={[{ label: "Term 1", value: "Term 1" }, { label: "Term 2", value: "Term 2" }]} />
                  <Select label="Classes Covered" value={newClasses} onChange={(e) => setNewClasses(e.target.value)} options={[{ label: "All Classes", value: "All Classes" }, ...GRADES.map((g) => ({ label: `${g} only`, value: g })), { label: "Grade 10 & Grade 12", value: "Grade 10 & Grade 12" }]} />
                </div>
                <Input label="Start Date" type="date" value={newStart} onChange={(e) => setNewStart(e.target.value)} required />
                <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs text-slate-500 font-semibold leading-relaxed">
                  A datesheet for {SUBJECTS.length} subjects ({SUBJECTS.join(", ")}) will be auto-generated from the start date with rooms and invigilators assigned.
                </div>
                <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setIsCreateOpen(false)}>Cancel</Button>
                  <Button type="submit">Create & Generate Datesheet</Button>
                </div>
              </form>
            </Dialog>
          </div>
        </TabsContent>

        {/* ================= ADMIT CARDS ================= */}
        <TabsContent value="admit_cards">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-end gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div className="grid grid-cols-2 gap-4 w-full sm:max-w-md">
                <Select label="Examination" value={acExamDocsId} onChange={(e) => setAcExamDocsId(e.target.value)} options={examOptions} />
                <Select label="Class" value={acGrade} onChange={(e) => setAcGrade(e.target.value)} options={gradeOptions} />
              </div>
              <div className="flex flex-col items-end gap-2 w-full sm:w-auto">
                {bulkProgress !== null && <div className="w-48 h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div className="h-full bg-indigo-600 rounded-full transition-all duration-150" style={{ width: `${bulkProgress}%` }} />
                  </div>}
                <Button onClick={handleBulkGenerate} disabled={bulkProgress !== null} className="flex gap-2 items-center text-xs py-2.5">
                  <RefreshCw className={`h-4 w-4 ${bulkProgress !== null ? "animate-spin" : ""}`} />
                  {bulkProgress !== null ? `Generating ${bulkProgress}%...` : `Bulk Generate (${acStudents.length} cards)`}
                </Button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
              {acStudents.map((s) => {
                const initials = s.name.split(" ").map((w) => w[0]).slice(0, 2).join("");
                return <div key={s.id} className="bg-white border border-slate-200 rounded-3xl p-5 shadow-xs hover:shadow-md transition-shadow">
                    <div className="text-center border-b border-dashed border-slate-200 pb-3">
                      <p className="text-xs font-black uppercase tracking-widest text-slate-800">St. Jude Boarding Academy</p>
                      <p className="text-[9px] uppercase tracking-wider font-bold text-indigo-600 mt-0.5">ADMIT CARD · {acExam ? acExam.name : ""}</p>
                    </div>
                    <div className="flex items-center gap-3 mt-4">
                      <div className="h-12 w-12 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-black text-sm shrink-0">{initials}</div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-extrabold text-slate-800 truncate">{s.name}</p>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{s.admissionNo} · {s.grade}</p>
                      </div>
                      <div className="grid grid-cols-6 gap-[1.5px] p-1 bg-white border border-slate-300 rounded shrink-0" title="QR verification code">
                        {Array.from({ length: 36 }).map((_, i) => <div key={i} className={`h-1.5 w-1.5 ${(hashNum(s.id) + i * 7) % 3 === 0 ? "bg-slate-900" : "bg-white"}`} />)}
                      </div>
                    </div>
                    <table className="w-full text-[10px] font-semibold text-slate-600 mt-4">
                      <thead>
                        <tr className="text-[8px] uppercase tracking-wider text-slate-400 border-b border-slate-100">
                          <th className="py-1.5 text-left">Date</th>
                          <th className="py-1.5 text-left">Subject</th>
                          <th className="py-1.5 text-left">Time</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-50">
                        {(acExam ? acExam.datesheet : []).map((row, i) => <tr key={i}>
                            <td className="py-1.5">{row.date.slice(5)}</td>
                            <td className="py-1.5 font-extrabold text-slate-700">{row.subject}</td>
                            <td className="py-1.5 text-slate-400">{row.time.split(" - ")[0]}</td>
                          </tr>)}
                      </tbody>
                    </table>
                    <div className="flex justify-between items-end mt-5 pt-3 border-t border-slate-100">
                      <div className="flex items-center gap-1 text-[9px] font-bold text-slate-400 uppercase tracking-wider"><QrCode className="h-3.5 w-3.5" /> Verify at gate</div>
                      <div className="text-center">
                        <div className="w-24 border-b border-slate-400 mb-1" />
                        <p className="text-[8px] uppercase tracking-widest font-extrabold text-slate-400">Principal Signature</p>
                      </div>
                    </div>
                  </div>;
              })}
            </div>
            {acStudents.length === 0 && <div className="bg-white border border-slate-100 rounded-3xl p-10 text-center text-xs font-semibold text-slate-400">No students enrolled in {acGrade}.</div>}
          </div>
        </TabsContent>

        {/* ================= MARKS ENTRY ================= */}
        <TabsContent value="marks_entry">
          <div className="space-y-6">
            <div className="flex flex-col lg:flex-row justify-between items-end gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full lg:max-w-2xl">
                <Select label="Examination" value={meExamDocsId} onChange={(e) => setMeExamDocsId(e.target.value)} options={examOptions} />
                <Select label="Class" value={meGrade} onChange={(e) => setMeGrade(e.target.value)} options={gradeOptions} />
                <Select label="Subject" value={meSubject} onChange={(e) => setMeSubject(e.target.value)} options={SUBJECTS.map((s) => ({ label: s, value: s }))} />
              </div>
              <div className="flex gap-2 shrink-0">
                <Button variant="outline" onClick={handleSaveDraft} disabled={sheet.locked} className="flex gap-1.5 items-center text-xs">
                  <Save className="h-4 w-4" /> Save Draft
                </Button>
                <Button onClick={handleLockSubmit} disabled={sheet.locked} className="flex gap-1.5 items-center text-xs">
                  <Lock className="h-4 w-4" /> Lock & Submit
                </Button>
              </div>
            </div>

            {sheet.locked && <div className="bg-emerald-50 border border-emerald-200 p-4 rounded-3xl flex gap-3 text-emerald-900 text-xs font-semibold items-center">
                <CheckCircle className="h-4.5 w-4.5 text-emerald-600 shrink-0" />
                This marksheet has been locked and submitted to the Examination Cell. Inputs are disabled. Contact the Controller of Examinations to reopen.
              </div>}

            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">{meSubject} · {meGrade} · Max Marks {sheet.maxMarks}</h3>
                <Badge variant={sheet.locked ? "success" : "warning"}>{sheet.locked ? "Locked" : "Draft"}</Badge>
              </div>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Admission No</th>
                      <th className="p-4">Student</th>
                      <th className="p-4">Marks Obtained (/{sheet.maxMarks})</th>
                      <th className="p-4">Grade</th>
                      <th className="p-4">Result</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {meStudents.map((s) => {
                      const raw = sheet.marks[s.id];
                      const num = raw === "" || raw === undefined ? null : Number(raw);
                      const pct = num === null ? null : Math.round(num / sheet.maxMarks * 100);
                      const letter = pct === null ? "-" : gradeLetter(pct);
                      return <tr key={s.id}>
                          <td className="p-4 text-slate-400 font-bold">{s.admissionNo}</td>
                          <td className="p-4 font-extrabold text-slate-800">{s.name}</td>
                          <td className="p-4">
                            <input
                              type="number"
                              min={0}
                              max={sheet.maxMarks}
                              value={raw === undefined ? "" : raw}
                              disabled={sheet.locked}
                              onChange={(e) => updateMark(s.id, e.target.value)}
                              placeholder="—"
                              className="w-24 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition disabled:bg-slate-100 disabled:text-slate-400"
                            />
                          </td>
                          <td className="p-4">
                            {pct === null ? <span className="text-slate-300">—</span> : <Badge variant={gradeBadgeVariant(letter)}>{letter}</Badge>}
                          </td>
                          <td className="p-4">
                            {pct === null ? <span className="text-slate-300">Not entered</span> : <Badge variant={pct >= 33 ? "success" : "danger"}>{pct >= 33 ? "Pass" : "Fail"}</Badge>}
                          </td>
                        </tr>;
                    })}
                  </tbody>
                </table>
              </div>
              <p className="text-[10px] text-slate-400 italic mt-3">Grades: A+ (91-100) · A (81-90) · B (71-80) · C (61-70) · D (45-60) · F (below 45). Pass mark 33%.</p>
            </div>
          </div>
        </TabsContent>

        {/* ================= REPORT CARDS ================= */}
        <TabsContent value="report_cards">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-end gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div className="w-full sm:max-w-sm">
                <Select label="Select Student" value={rcStudentDocsId} onChange={(e) => setRcStudentDocsId(e.target.value)} options={students.map((s) => ({ label: `${s.name} (${s.admissionNo} · ${s.grade})`, value: s.id }))} />
              </div>
              <div className="flex gap-2 shrink-0">
                <Button variant="outline" onClick={() => addToast("Print Queued", "Report card PDF sent to print spooler (simulation)", "info")} className="flex gap-1.5 items-center text-xs">
                  <Printer className="h-4 w-4" /> Print / Download PDF
                </Button>
                <Button onClick={handlePublishReport} className="flex gap-1.5 items-center text-xs">
                  <Send className="h-4 w-4" /> Publish to Parent App
                </Button>
              </div>
            </div>

            {rcStudent && <div className="bg-white border border-slate-200 rounded-3xl p-8 max-w-3xl mx-auto shadow-xs">
                {/* School header */}
                <div className="text-center border-b-2 border-slate-800 pb-4">
                  <h3 className="text-xl font-black uppercase tracking-widest text-slate-900">St. Jude Boarding Academy</h3>
                  <p className="text-[10px] uppercase tracking-wider font-bold text-slate-500 mt-1">Affiliated to CBSE · Affiliation No. 2130456 · Whispering Pines Road, Cityville</p>
                  <p className="text-xs font-extrabold text-indigo-700 uppercase tracking-widest mt-2">Report Card · Academic Session 2025-26</p>
                </div>

                {/* Student info */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-5 border-b border-slate-100 text-xs">
                  <div><p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Student</p><p className="font-extrabold text-slate-800 mt-0.5">{rcStudent.name}</p></div>
                  <div><p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Admission No</p><p className="font-extrabold text-slate-800 mt-0.5">{rcStudent.admissionNo}</p></div>
                  <div><p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Class</p><p className="font-extrabold text-slate-800 mt-0.5">{rcStudent.grade}</p></div>
                  <div><p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Parent / Guardian</p><p className="font-extrabold text-slate-800 mt-0.5">{rcStudent.parentName}</p></div>
                </div>

                {/* Scholastic */}
                <p className="text-[10px] uppercase tracking-widest font-extrabold text-slate-500 mt-5 mb-2">Part A · Scholastic Areas</p>
                <div className="overflow-x-auto border border-slate-200 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-3">Subject</th>
                        <th className="p-3 text-center">Term 1 (100)</th>
                        <th className="p-3 text-center">Term 2 (100)</th>
                        <th className="p-3 text-center">Overall</th>
                        <th className="p-3 text-center">Grade</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {rcRows.map((r) => <tr key={r.subject}>
                          <td className="p-3 font-extrabold text-slate-800">{r.subject}</td>
                          <td className="p-3 text-center">{r.t1}</td>
                          <td className="p-3 text-center">{r.t2}</td>
                          <td className="p-3 text-center font-black">{r.avg}</td>
                          <td className="p-3 text-center"><Badge variant={gradeBadgeVariant(r.letter)}>{r.letter}</Badge></td>
                        </tr>)}
                      <tr className="bg-slate-50">
                        <td className="p-3 font-black text-slate-900 uppercase text-[10px] tracking-wider">Aggregate</td>
                        <td className="p-3 text-center" colSpan={2}>{rcOverall}%</td>
                        <td className="p-3 text-center font-black">{rcOverall}</td>
                        <td className="p-3 text-center"><Badge variant={gradeBadgeVariant(gradeLetter(rcOverall))}>{gradeLetter(rcOverall)}</Badge></td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                {/* Co-scholastic */}
                <p className="text-[10px] uppercase tracking-widest font-extrabold text-slate-500 mt-5 mb-2">Part B · Co-Scholastic Areas (3-point scale)</p>
                <div className="overflow-x-auto border border-slate-200 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                      <tr><th className="p-3">Area</th><th className="p-3 text-center">Term 1</th><th className="p-3 text-center">Term 2</th></tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {CO_SCHOLASTIC.map((c) => <tr key={c.area}>
                          <td className="p-3 font-extrabold text-slate-800">{c.area}</td>
                          <td className="p-3 text-center">{c.grades[hashNum(rcStudentDocsId + c.area) % 3]}</td>
                          <td className="p-3 text-center">{c.grades[hashNum(rcStudentDocsId + c.area + "2") % 3]}</td>
                        </tr>)}
                    </tbody>
                  </table>
                </div>

                {/* Attendance + remarks */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-5">
                  <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
                    <p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Attendance</p>
                    <p className="text-lg font-black text-slate-800 mt-1">{rcAttendance}%</p>
                    <p className="text-[10px] text-slate-400 font-semibold">of 220 working days</p>
                  </div>
                  <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 sm:col-span-2">
                    <p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Class Teacher's Remarks</p>
                    <p className="text-xs font-semibold text-slate-700 mt-1 leading-relaxed">{rcRemark}</p>
                  </div>
                </div>

                {/* Footer legend + signatures */}
                <div className="flex justify-between items-end mt-8">
                  <div className="text-center"><div className="w-28 border-b border-slate-400 mb-1" /><p className="text-[8px] uppercase tracking-widest font-extrabold text-slate-400">Class Teacher</p></div>
                  <div className="text-center"><div className="w-28 border-b border-slate-400 mb-1" /><p className="text-[8px] uppercase tracking-widest font-extrabold text-slate-400">Exam Controller</p></div>
                  <div className="text-center"><div className="w-28 border-b border-slate-400 mb-1" /><p className="text-[8px] uppercase tracking-widest font-extrabold text-slate-400">Principal</p></div>
                </div>
                <p className="text-[9px] text-slate-400 font-semibold mt-6 pt-3 border-t border-dashed border-slate-200 leading-relaxed">
                  Grading Scale: A+ (91-100) · A (81-90) · B (71-80) · C (61-70) · D (45-60) · F (Below 45 — Essential Repeat). Minimum pass 33%. This is a computer-generated document{publishedReports.includes(rcStudentDocsId) ? " · PUBLISHED TO PARENT APP" : ""}.
                </p>
              </div>}
          </div>
        </TabsContent>

        {/* ================= PROMOTION ================= */}
        <TabsContent value="promotion">
          <div className="space-y-6">
            {/* Session banner */}
            <div className="bg-indigo-900 text-indigo-100 p-6 rounded-3xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-white/10 rounded-2xl text-white"><GraduationCap className="h-6 w-6" /></div>
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">SESSION ROLLOVER WIZARD (SANDBOX)</span>
                  <div className="flex items-center gap-3 mt-1">
                    <h4 className="text-2xl font-black text-white">2025-26</h4>
                    <ArrowRight className="h-5 w-5 text-indigo-300" />
                    <h4 className="text-2xl font-black text-emerald-300">2026-27</h4>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2 text-[10px] font-bold text-indigo-200 bg-white/10 px-3 py-2 rounded-xl">
                <AlertCircle className="h-4 w-4" /> Simulation only — live student records are never mutated.
              </div>
            </div>

            {/* Step indicator */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <div className="flex items-center gap-2 mb-6">
                {["Review Classes", "Carry Forward Dues", "Confirm & Roll Over"].map((label, i) => {
                  const stepNo = i + 1;
                  const active = wizStep === stepNo;
                  const done = wizStep > stepNo;
                  return <div key={label} className="flex items-center gap-2 flex-1 min-w-0">
                      <div className={`h-7 w-7 rounded-full flex items-center justify-center text-[10px] font-black shrink-0 ${done ? "bg-emerald-500 text-white" : active ? "bg-indigo-600 text-white" : "bg-slate-100 text-slate-400"}`}>
                        {done ? <CheckCircle className="h-4 w-4" /> : stepNo}
                      </div>
                      <span className={`text-[10px] uppercase tracking-wider font-extrabold truncate ${active ? "text-indigo-700" : done ? "text-emerald-600" : "text-slate-400"}`}>{label}</span>
                      {i < 2 && <div className={`flex-1 h-0.5 rounded ${done ? "bg-emerald-300" : "bg-slate-100"}`} />}
                    </div>;
                })}
              </div>

              {/* Step 1: review */}
              {wizStep === 1 && <div className="animate-fade-in">
                  <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                    <table className="w-full text-xs font-semibold text-slate-700 text-left">
                      <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                        <tr>
                          <th className="p-4">Current Class</th>
                          <th className="p-4">Students</th>
                          <th className="p-4">Moves To</th>
                          <th className="p-4">Detain (count)</th>
                          <th className="p-4">Promoting</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {classCounts.map(({ grade, count }, i) => {
                          const detained = Math.min(parseInt(detainMap[grade]) || 0, count);
                          const next = i === GRADES.length - 1 ? "Pass Out / Alumni" : GRADES[i + 1];
                          return <tr key={grade}>
                              <td className="p-4 font-extrabold text-slate-800">{grade}</td>
                              <td className="p-4"><span className="inline-flex items-center gap-1.5"><Users className="h-3.5 w-3.5 text-slate-400" /> {count}</span></td>
                              <td className="p-4"><Badge variant={i === GRADES.length - 1 ? "info" : "secondary"}>{next}</Badge></td>
                              <td className="p-4">
                                <input
                                  type="number"
                                  min={0}
                                  max={count}
                                  value={detainMap[grade] || ""}
                                  placeholder="0"
                                  onChange={(e) => setDetainMap({ ...detainMap, [grade]: e.target.value })}
                                  className="w-20 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
                                />
                              </td>
                              <td className="p-4 font-black text-emerald-600">{count - detained}</td>
                            </tr>;
                        })}
                      </tbody>
                    </table>
                  </div>
                  <div className="flex justify-between items-center mt-4">
                    <p className="text-[10px] text-slate-400 font-semibold">Promote All is the default — enter a detain count per class to hold students back.</p>
                    <Button onClick={() => setWizStep(2)} className="flex gap-1.5 items-center text-xs">Next: Dues <ArrowRight className="h-4 w-4" /></Button>
                  </div>
                </div>}

              {/* Step 2: dues */}
              {wizStep === 2 && <div className="animate-fade-in space-y-4">
                  <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                    <table className="w-full text-xs font-semibold text-slate-700 text-left">
                      <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                        <tr><th className="p-4">Class</th><th className="p-4">Defaulters</th><th className="p-4">Pending Dues</th><th className="p-4">Action</th></tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {[
                          { grade: "Grade 7", defaulters: 4, dues: "₹48,200" },
                          { grade: "Grade 9", defaulters: 6, dues: "₹86,500" },
                          { grade: "Grade 10", defaulters: 3, dues: "₹52,700" },
                          { grade: "Grade 12", defaulters: 2, dues: "₹49,000" }
                        ].map((r) => <tr key={r.grade}>
                            <td className="p-4 font-extrabold text-slate-800">{r.grade}</td>
                            <td className="p-4">{r.defaulters} students</td>
                            <td className="p-4 font-black text-rose-600">{r.dues}</td>
                            <td className="p-4"><Badge variant={carryDues ? "warning" : "danger"}>{carryDues ? "Carry to 2026-27" : "Write off"}</Badge></td>
                          </tr>)}
                      </tbody>
                    </table>
                  </div>
                  <label className="flex items-center gap-2.5 text-xs font-bold text-slate-700 bg-slate-50 border border-slate-100 rounded-2xl p-4 cursor-pointer">
                    <input type="checkbox" checked={carryDues} onChange={(e) => setCarryDues(e.target.checked)} className="h-4 w-4 accent-indigo-600" />
                    Carry forward pending dues (₹2,36,400 total) as opening balance into the 2026-27 fee ledger
                  </label>
                  <div className="flex justify-between items-center">
                    <Button variant="outline" onClick={() => setWizStep(1)} className="text-xs">Back</Button>
                    <Button onClick={() => setWizStep(3)} className="flex gap-1.5 items-center text-xs">Next: Confirm <ArrowRight className="h-4 w-4" /></Button>
                  </div>
                </div>}

              {/* Step 3: confirm */}
              {wizStep === 3 && <div className="animate-fade-in space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-center">
                      <p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Promoting</p>
                      <p className="text-2xl font-black text-emerald-600 mt-1">{totalPromoted}</p>
                    </div>
                    <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-center">
                      <p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Detaining</p>
                      <p className="text-2xl font-black text-rose-600 mt-1">{totalDetained}</p>
                    </div>
                    <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-center">
                      <p className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Dues Handling</p>
                      <p className="text-sm font-black text-slate-800 mt-2">{carryDues ? "Carry ₹2,36,400" : "Write off"}</p>
                    </div>
                  </div>

                  {rollProgress !== null && <div>
                      <div className="flex justify-between text-[10px] font-extrabold uppercase tracking-wider text-slate-400 mb-1.5">
                        <span>Rolling over session...</span><span>{rollProgress}%</span>
                      </div>
                      <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full bg-linear-to-r from-indigo-500 to-emerald-500 rounded-full transition-all duration-100" style={{ width: `${rollProgress}%` }} />
                      </div>
                    </div>}

                  <div className="flex justify-between items-center">
                    <Button variant="outline" onClick={() => setWizStep(2)} disabled={rollProgress !== null} className="text-xs">Back</Button>
                    <Button onClick={handleFinishRollover} disabled={rollProgress !== null} className="flex gap-1.5 items-center text-xs">
                      <RefreshCw className={`h-4 w-4 ${rollProgress !== null ? "animate-spin" : ""}`} />
                      {rollProgress !== null ? "Processing..." : "Confirm & Roll Over Session"}
                    </Button>
                  </div>
                </div>}
            </div>

            {/* History */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Past Rollover Runs</h3>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr><th className="p-4">Run Date</th><th className="p-4">Session</th><th className="p-4">Promoted</th><th className="p-4">Detained</th><th className="p-4">Dues Carried</th><th className="p-4">Executed By</th></tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {promoHistory.map((h) => <tr key={h.id}>
                        <td className="p-4 font-extrabold text-slate-800">{h.date}</td>
                        <td className="p-4"><Badge variant="info">{h.from} → {h.to}</Badge></td>
                        <td className="p-4 font-black text-emerald-600">{h.promoted}</td>
                        <td className="p-4 font-black text-rose-600">{h.detained}</td>
                        <td className="p-4">{h.duesCarried}</td>
                        <td className="p-4 text-slate-500">{h.by}</td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>
    </div>;
}
