/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import {
  getStudents,
  getResults,
  saveResults,
  getHomework,
  saveHomework,
  logAction
} from "../storage";
import { api } from "../api";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { Calendar, Plus } from "lucide-react";
import { mockTimetableEvents } from "../mockData";
export default function ModAcademics({ user }) {
  const { addToast } = useToast();
  const [results, setResults] = useState(() => getResults());
  const [homework, setHomework] = useState(() => getHomework());
  const [students, setStudents] = useState(() => getStudents().slice(0, 15));
  const [acTab, setAcTab] = useState("timetable");
  const [isHwOpen, setIsHwOpen] = useState(false);
  const [hwClass, setHwClass] = useState("Grade 7-A Pioneers");
  const [hwSubject, setHwSubject] = useState("Mathematics");
  const [hwTitle, setHwTitle] = useState("");
  const [hwInstructions, setHwInstructions] = useState("");
  const [hwDueDate, setHwDueDate] = useState("2026-06-05");
  const [editExam, setEditExam] = useState("Midterm 2026");
  const [editSubjectStr, setEditSubjectStr] = useState("Mathematics");

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      api.getHomework().catch(() => []),
      api.getAcademicResults().catch(() => [])
    ]).then(([hwRes, resRes]) => {
      if (isMounted) {
        if (Array.isArray(hwRes) && hwRes.length > 0) setHomework(hwRes);
        if (Array.isArray(resRes) && resRes.length > 0) setResults(resRes);
      }
    });
    return () => { isMounted = false; };
  }, []);

  const handleCreateHw = (e) => {
    e.preventDefault();
    if (!hwTitle || !hwInstructions) {
      addToast("Error", "Homework topic title and instructions are required", "error");
      return;
    }
    const newHw = {
      id: `hw-${Date.now()}`,
      classId: "class-custom",
      className: hwClass,
      subject: hwSubject,
      title: hwTitle,
      instructions: hwInstructions,
      dueDate: hwDueDate,
      submittedCount: 0
    };
    const updated = [newHw, ...homework];
    setHomework(updated);
    saveHomework(updated);
    api.createHomework({
      schoolId: 'SCH-001',
      academicYear: '2026-2027',
      classDocsId: 'class-custom',
      sectionNo: 'A',
      subject: hwSubject,
      title: hwTitle,
      instructions: hwInstructions,
      dueDate: hwDueDate,
      teacherDocsId: user?.id || 'TEACHER_ID_HERE'
    }).catch(() => {});
    logAction(user.id, user.name, user.role, "Homework Assignment Published", `Published task: "${hwTitle}" to class ${hwClass}`);
    addToast("Success", "Homework topic dispatched to student feeds!");
    setIsHwOpen(false);
    setHwTitle("");
    setHwInstructions("");
  };
  const handleScoreChange = (studentId, obtained) => {
    if (obtained < 0 || obtained > 100) return;
    const freshResults = getResults();
    const idx = freshResults.findIndex((r) => r.studentId === studentId && r.examName === editExam);
    const calcGrade = (pct) => {
      if (pct >= 90) return "A+";
      if (pct >= 80) return "A";
      if (pct >= 70) return "B+";
      if (pct >= 60) return "C";
      return "D";
    };
    if (idx !== -1) {
      const studentSheet = freshResults[idx];
      const matchSubIdx = studentSheet.marks.findIndex((m) => m.subject === editSubjectStr);
      if (matchSubIdx !== -1) {
        studentSheet.marks[matchSubIdx].obtainedMarks = obtained;
        studentSheet.marks[matchSubIdx].grade = calcGrade(obtained);
        const totalMax = studentSheet.marks.reduce((sum, m) => sum + m.maxMarks, 0);
        const totalObt = studentSheet.marks.reduce((sum, m) => sum + m.obtainedMarks, 0);
        studentSheet.totalPercentage = Number((totalObt / totalMax * 100).toFixed(1));
        studentSheet.overallGrade = calcGrade(studentSheet.totalPercentage);
      }
      freshResults[idx] = studentSheet;
    } else {
      const stDetails = getStudents().find((s) => s.id === studentId);
      const newSheet = {
        id: `res-${Date.now()}-${studentId}`,
        studentId,
        studentName: stDetails ? stDetails.name : "Unknown Roster",
        grade: stDetails ? stDetails.grade : "Grade 7",
        examName: editExam,
        marks: [
          { subject: editSubjectStr, maxMarks: 100, obtainedMarks: obtained, grade: calcGrade(obtained) }
        ],
        totalPercentage: obtained,
        overallGrade: calcGrade(obtained),
        feedback: "Written marks score update uploaded via tutor console spreadsheet."
      };
      freshResults.unshift(newSheet);
    }
    setResults(freshResults);
    saveResults(freshResults);
  };
  const handleCommitTranscript = () => {
    logAction(user.id, user.name, user.role, "Exam Transcripts Locked", `Authorized and published checked marks files for ${editSubjectStr} under ${editExam}`);
    addToast("Success", "Roster academic scores published and locked into parent dashboards!", "success");
  };
  return <div className="space-y-6">

      {
    /* Title block */
  }
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight">Academics & Study Suite</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Supervise school lessons schedules, publish homework feeds, and grade exam score transcripts inside LocalStorage.</p>
        </div>

        <div className="flex gap-2 bg-slate-100 p-1 rounded-2xl w-full md:w-auto overflow-x-auto self-stretch md:self-auto shrink-0 scrollbar-none">
          <button
    onClick={() => setAcTab("timetable")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer ${acTab === "timetable" ? "bg-white text-indigo-650 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Lessons Lessons Timetable
          </button>
          <button
    onClick={() => setAcTab("homework")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer ${acTab === "homework" ? "bg-white text-indigo-650 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Homework Feed
          </button>
          <button
    onClick={() => setAcTab("gradebook")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer ${acTab === "gradebook" ? "bg-white text-indigo-650 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Examinations Gradebook
          </button>
        </div>
      </div>

      {
    /* TIMETABLE VIEW */
  }
      {acTab === "timetable" && <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-5">
          <div>
            <h3 className="text-sm font-extrabold text-slate-805 uppercase tracking-widest">Secondary Timetable Grid</h3>
            <p className="text-xs text-slate-400 mt-1">Daily academic block sessions. Designed for elite boarding preparations.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 pt-1">
            {mockTimetableEvents.map((event) => <div key={event.id} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl hover:border-indigo-400 hover:bg-indigo-50/10 transition flex flex-col justify-between h-40">
                <div className="space-y-1">
                  <span className="bg-white border border-slate-200 text-slate-500 text-[9px] font-black tracking-wide uppercase px-2 py-0.5 rounded-md">
                    {event.day}
                  </span>
                  <h4 className="text-xs font-black text-slate-800 pt-1 leading-normal">{event.subject}</h4>
                  <p className="text-[10px] text-slate-400 font-bold leading-none">{event.className}</p>
                </div>

                <div className="border-t border-slate-200 pt-2 text-[10px] text-slate-450 font-bold flex flex-col gap-0.5">
                  <p className="flex items-center gap-1.5 text-indigo-600">
                    <Calendar className="h-3 w-3" />
                    {event.time}
                  </p>
                  <p className="text-slate-500 font-semibold">{event.teacher} — {event.room}</p>
                </div>
              </div>)}
          </div>
        </div>}

      {
    /* HOMEWORK VIEW */
  }
      {acTab === "homework" && <div className="space-y-4">
          <div className="flex justify-between items-center bg-white p-5 rounded-3xl border border-slate-100">
            <div>
              <h3 className="text-sm font-extrabold text-slate-805 uppercase tracking-widest">Active Homework Feeds</h3>
              <p className="text-xs text-slate-450 mt-1">Review topics and assignments published dynamically inside the parent/student screens.</p>
            </div>
            <Button onClick={() => setIsHwOpen(true)} className="text-xs py-1.5 bg-slate-900 border border-transparent">
              <Plus className="h-4 w-4" /> Publish Task
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {homework.map((hw) => <div key={hw.id} className="bg-white border border-slate-100 p-5 rounded-3xl flex flex-col justify-between h-56 hover:border-indigo-350 transition">
                <div className="space-y-2">
                  <div className="flex gap-2.5 items-center">
                    <span className="bg-slate-100 text-slate-700 text-[9px] font-black px-2 py-0.5 rounded uppercase">
                      {hw.className}
                    </span>
                    <span className="bg-indigo-50 text-indigo-750 text-[9px] font-black px-2.5 py-0.5 rounded uppercase">
                      {hw.subject}
                    </span>
                  </div>
                  <h4 className="text-sm font-black text-slate-800 leading-tight">{hw.title}</h4>
                  <p className="text-xs text-slate-400 leading-relaxed font-semibold line-clamp-2 italic bg-slate-50 p-2 border border-slate-100 rounded-lg">
                    "{hw.instructions}"
                  </p>
                </div>

                <div className="flex justify-between items-center border-t border-slate-100 pt-3 text-[11px] font-bold text-slate-450">
                  <span className="text-amber-600">Due limit: {hw.dueDate}</span>
                  <span className="text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-md font-extrabold">{hw.submittedCount} Submitted docs</span>
                </div>
              </div>)}
          </div>
        </div>}

      {
    /* GRADEBOOK VIEW */
  }
      {acTab === "gradebook" && <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h3 className="text-sm font-extrabold text-slate-805 uppercase tracking-widest">Active Lessons Grading Sheet</h3>
              <p className="text-xs text-slate-400 mt-1">Input scores directly. sandbox percentages recalculate instantly in browser state.</p>
            </div>

            <div className="flex gap-3 text-xs font-bold text-slate-700 items-center">
              <Select
    options={[
      { label: "Midterm exams Spring 2026", value: "Midterm 2026" },
      { label: "Standard Unit Test 1", value: "Unit Test 1" }
    ]}
    value={editExam}
    onChange={(e) => setEditExam(e.target.value)}
    className="text-xs py-2 h-9 rounded-xl max-w-[200px]"
  />
              <Select
    options={[
      { label: "Mathematics Lesson", value: "Mathematics" },
      { label: "Inorganic Chemistry", value: "Inorganic Chemistry" },
      { label: "Classical Physics", value: "Classical Physics" }
    ]}
    value={editSubjectStr}
    onChange={(e) => setEditSubjectStr(e.target.value)}
    className="text-xs py-2 h-9 rounded-xl max-w-[170px]"
  />

              <Button onClick={handleCommitTranscript} className="text-xs py-2.5 bg-indigo-600 hover:bg-slate-900 border border-transparent">
                Publish Sheets
              </Button>
            </div>
          </div>

          <div className="overflow-x-auto border border-slate-100 rounded-2xl">
            <table className="w-full text-xs text-slate-700 font-semibold text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Scholar student associated</th>
                  <th className="p-3">Exam Module Name</th>
                  <th className="p-3">Subject Section</th>
                  <th className="p-3 text-center">Score obtained (Max 100)</th>
                  <th className="p-3 text-center">Registered performance grade</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {students.map((st) => {
    const sheet = results.find((r) => r.studentId === st.id && r.examName === editExam);
    const marksItem = sheet ? sheet.marks.find((m) => m.subject === editSubjectStr) : null;
    const score = marksItem ? marksItem.obtainedMarks : 75;
    const grade = marksItem ? marksItem.grade : "B";
    return <tr key={st.id}>
                      <td className="p-3 font-extrabold text-slate-800">{st.name} ({st.admissionNumber})</td>
                      <td className="p-3 font-bold text-slate-450">{editExam}</td>
                      <td className="p-3 text-slate-500 font-bold">{editSubjectStr}</td>
                      <td className="p-3 text-center">
                        <input
      type="number"
      value={score}
      onChange={(e) => handleScoreChange(st.id, parseInt(e.target.value) || 0)}
      className="w-20 bg-slate-50 border border-slate-205 text-center text-xs font-black py-1 rounded focus:ring-1 focus:ring-indigo-505"
      min={0}
      max={100}
    />
                      </td>
                      <td className="p-3 text-center">
                        <Badge variant="secondary">{grade}</Badge>
                      </td>
                    </tr>;
  })}
              </tbody>
            </table>
          </div>
        </div>}

      {
    /* Publish Homework Dialog */
  }
      <Dialog isOpen={isHwOpen} onClose={() => setIsHwOpen(false)} title="Broadcast Homework Lesson Assignment">
        <form onSubmit={handleCreateHw} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Select
    label="Select Recipient Cohort"
    options={[
      { label: "Grade 6 Secondary Bluebirds", value: "Grade 6-A Bluebirds" },
      { label: "Grade 7 Secondary Pioneers", value: "Grade 7-A Pioneers" },
      { label: "Grade 8 Secondary Olympians", value: "Grade 8-A Olympians" },
      { label: "Grade 12 CS Advanced Elite", value: "Grade 12-Science Elite" }
    ]}
    value={hwClass}
    onChange={(e) => setHwClass(e.target.value)}
  />
            <Select
    label="Selective Subject Code"
    options={[
      { label: "Mathematics", value: "Mathematics" },
      { label: "English Literature", value: "English Literature" },
      { label: "Inorganic Chemistry", value: "Inorganic Chemistry" },
      { label: "Classical Physics", value: "Classical Physics" }
    ]}
    value={hwSubject}
    onChange={(e) => setHwSubject(e.target.value)}
  />
          </div>

          <Input
    label="Homework Topic Summary Title"
    value={hwTitle}
    onChange={(e) => setHwTitle(e.target.value)}
    placeholder="e.g. factoring algebraic quadratics worksheets"
    required
  />

          <div className="grid grid-cols-1 gap-4">
            <Input
    label="Est. Due Limit Calendar Date"
    type="date"
    value={hwDueDate}
    onChange={(e) => setHwDueDate(e.target.value)}
    required
  />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Instructions description</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-200 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-505 focus:bg-white max-h-32 transition"
    rows={3}
    placeholder="Provide Page numbers reference guidelines, scanning formulas criteria..."
    value={hwInstructions}
    onChange={(e) => setHwInstructions(e.target.value)}
    required
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsHwOpen(false)}>Cancel Publish</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900 font-extrabold">Finalize Assignment Publish</Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
