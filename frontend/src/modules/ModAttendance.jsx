/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import { getStudents, getStaff, getAttendance, saveAttendance, logAction } from "../storage";
import { api } from "../api";
import { Button, Select, Badge, useToast } from "../components/ui";
import { Calendar, CheckCircle2, QrCode, Sparkles, XCircle } from "lucide-react";
export default function ModAttendance({ user }) {
  const { addToast } = useToast();
  const [students, setStudents] = useState(() => getStudents());
  const [staff, setStaff] = useState(() => getStaff());
  const [attendance, setAttendance] = useState(() => getAttendance());
  const [attView, setAttView] = useState("students");
  const [selectedGrade, setSelectedGrade] = useState("Grade 7");
  const [attDate, setAttDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [scanningStatus, setScanningStatus] = useState("Idle");

  useEffect(() => {
    let isMounted = true;
    api.getAttendance().then((res) => {
      if (isMounted && Array.isArray(res) && res.length > 0) {
        setAttendance(res);
      }
    }).catch(() => {});
    return () => { isMounted = false; };
  }, []);

  const handleToggleStudentAtt = (studentDocsId, name, type) => {
    const existingIdx = attendance.findIndex(
      (r) => r.personDocsId === studentDocsId && r.date === attDate && r.type === type
    );
    let updated = [...attendance];
    let recordToSave;
    if (existingIdx !== -1) {
      const currentStatus = updated[existingIdx].status;
      const nextStatus = currentStatus === "Present" ? "Absent" : currentStatus === "Absent" ? "Late" : "Present";
      updated[existingIdx].status = nextStatus;
      recordToSave = updated[existingIdx];
      addToast("Status Changed", `Set ${name} to "${nextStatus}"`);
    } else {
      recordToSave = {
        id: `att-${studentDocsId}-${attDate}-${Date.now()}`,
        type,
        personDocsId: studentDocsId,
        personName: name,
        date: attDate,
        status: "Present",
        timestamp: "08:30 AM"
      };
      updated.push(recordToSave);
      addToast("Status Logged", `Logged ${name} as "Present"`);
    }
    setAttendance(updated);
    saveAttendance(updated);
    if (recordToSave) {
      api.createAttendance({
        schoolId: 'SCH-001',
        academicYear: '2026-2027',
        studentDocsId: recordToSave.personDocsId,
        studentName: recordToSave.personName,
        date: recordToSave.date,
        status: recordToSave.status,
        type: recordToSave.type || 'CLASSROOM'
      }).catch(() => {});
    }
  };
  const handleSaveAll = () => {
    logAction(user.id, user.name, user.role, "Attendance List Saved", `Saved morning classroom checklist for ${selectedGrade} dated ${attDate}`);
    addToast("Successfully Saved", `Roster files locked in for ${selectedGrade} on ${attDate}`, "success");
  };
  const handleScanSimulation = () => {
    setScanningStatus("Searching...");
    setTimeout(() => {
      setScanningStatus("Verified: Student #104");
      addToast("QR Scanned", "Alistair Smith verified at West gate scanner", "success");
    }, 1200);
  };
  const gradeStudents = students.filter((s) => s.grade === selectedGrade);
  const boysHostelStudents = students.filter((s) => s.hostelOptIn && s.gender === "Male");
  return <div className="space-y-6">

      {
    /* View Select header area */
  }
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight">Institution Attendance Matrix</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Check morning lessons rosters, night dorm curfews, and trigger simulated QR Face ID chip readers.</p>
        </div>

        <div className="flex gap-2 bg-slate-100 p-1 rounded-2xl w-full md:w-auto overflow-x-auto self-stretch md:self-auto shrink-0 scrollbar-none">
          <button
    onClick={() => setAttView("students")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition ${attView === "students" ? "bg-white text-indigo-600 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Morning Lesson
          </button>
          <button
    onClick={() => setAttView("wardens")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition ${attView === "wardens" ? "bg-white text-indigo-600 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Night Assembly
          </button>
          <button
    onClick={() => setAttView("staff")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition ${attView === "staff" ? "bg-white text-indigo-600 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            Staff Registry
          </button>
          <button
    onClick={() => setAttView("faceid")}
    className={`px-4 py-2 text-xs font-bold rounded-xl transition ${attView === "faceid" ? "bg-white text-indigo-600 shadow-xs" : "text-slate-500 hover:text-slate-800"}`}
  >
            QR Gateway
          </button>
        </div>
      </div>

      {
    /* Main Content panels */
  }
      {attView === "students" && <div className="space-y-4">
          <div className="flex flex-wrap gap-3.5 items-center bg-white p-4 rounded-2xl border border-slate-100 text-xs font-bold text-slate-700">
            <span className="text-slate-400">SELECT LESSON COHORT:</span>
            <Select
    options={[
      { label: "Grade 6 Secondary", value: "Grade 6" },
      { label: "Grade 7 Secondary", value: "Grade 7" },
      { label: "Grade 8 Secondary", value: "Grade 8" },
      { label: "Grade 9 Intermediate", value: "Grade 9" },
      { label: "Grade 10 Intermediate", value: "Grade 10" },
      { label: "Grade 11 College Prep", value: "Grade 11" },
      { label: "Grade 12 College Prep", value: "Grade 12" }
    ]}
    value={selectedGrade}
    onChange={(e) => setSelectedGrade(e.target.value)}
    className="text-xs py-2 h-9 rounded-xl max-w-[170px]"
  />
            <input
    type="date"
    value={attDate}
    onChange={(e) => setAttDate(e.target.value)}
    className="bg-slate-50 border border-slate-200 text-xs rounded-xl px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-indigo-505 font-medium"
  />
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-sm font-extrabold text-slate-850 uppercase tracking-widest">{selectedGrade} Morning Roll-Call</h3>
              <Button onClick={handleSaveAll} className="text-xs py-1.5 bg-slate-900 border border-transparent">
                Lock and Save List
              </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5 pt-1">
              {gradeStudents.map((st) => {
    const record = attendance.find((r) => r.personDocsId === st.id && r.date === attDate && r.type === "Student Attendance");
    const status = record ? record.status : "Present";
    let cardBorder = "border-slate-100 hover:border-indigo-300";
    if (status === "Absent") cardBorder = "border-rose-200 bg-rose-50/10";
    else if (status === "Late") cardBorder = "border-amber-200 bg-amber-50/10";
    return <button
      key={st.id}
      onClick={() => handleToggleStudentAtt(st.id, st.name, "Student Attendance")}
      className={`flex items-center justify-between p-4 rounded-2xl border text-left select-none cursor-pointer transition ${cardBorder}`}
    >
                    <div>
                      <p className="text-xs font-extrabold text-slate-850">{st.name}</p>
                      <p className="text-[10px] text-slate-400 mt-1 font-bold">{st.admissionNo}</p>
                    </div>

                    <div className="flex items-center gap-1.5 font-bold text-xs shrink-0 select-none">
                      {status === "Present" && <span className="bg-emerald-50 text-emerald-700 p-1 px-2.5 rounded-lg border border-emerald-100 flex items-center gap-1 text-[10px]">
                          <CheckCircle2 className="h-3 w-3" /> PRESENT
                        </span>}
                      {status === "Late" && <span className="bg-amber-50 text-amber-700 p-1 px-2.5 rounded-lg border border-amber-100 flex items-center gap-1 text-[10px]">
                          <Calendar className="h-3 w-3" /> LATE
                        </span>}
                      {status === "Absent" && <span className="bg-rose-50 text-rose-700 p-1 px-2.5 rounded-lg border border-rose-100 flex items-center gap-1 text-[10px]">
                          <XCircle className="h-3 w-3" /> ABSENT
                        </span>}
                    </div>
                  </button>;
  })}
            </div>
          </div>
        </div>}

      {attView === "wardens" && <div className="space-y-4">
          <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
            <div>
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Warden Curfew Night Roll-call</h3>
              <p className="text-xs text-slate-400 font-semibold mt-1">Trigger night beds checks. Toggle present/absent for boys resident cohort.</p>
            </div>
            <input
    type="date"
    value={attDate}
    onChange={(e) => setAttDate(e.target.value)}
    className="bg-slate-50 border border-slate-200 text-xs rounded-xl px-3 py-1.5 focus:outline-none"
  />
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5 pt-1">
              {boysHostelStudents.slice(0, 15).map((st) => {
    const record = attendance.find((r) => r.personDocsId === st.id && r.date === attDate && r.type === "Night Attendance");
    const status = record ? record.status : "Present";
    return <button
      key={st.id}
      onClick={() => handleToggleStudentAtt(st.id, st.name, "Night Attendance")}
      className={`flex items-center justify-between p-4 rounded-2xl border text-left select-none cursor-pointer transition ${status === "Absent" ? "border-rose-200 bg-rose-50/10" : "border-slate-100 hover:border-indigo-300"}`}
    >
                    <div>
                      <p className="text-xs font-black text-slate-800">{st.name}</p>
                      <p className="text-[10px] text-indigo-650 mt-1 font-bold">Room {st.hostelRoomNo || "101"} - {st.hostelBedNo || "Bed-1"}</p>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0 select-none">
                      <Badge variant={status === "Present" ? "success" : "danger"}>
                        {status === "Present" ? "In Bed" : "Curfew Breach"}
                      </Badge>
                    </div>
                  </button>;
  })}
            </div>
          </div>
        </div>}

      {attView === "staff" && <div className="bg-white border border-slate-105 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center leading-none">
            <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Institutional Staff Daily Register</h3>
            <span className="text-xs font-bold text-slate-400">{attDate}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 pt-1">
            {staff.map((sf) => <div key={sf.id} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl flex justify-between items-center">
                <div>
                  <p className="text-sm font-bold text-slate-800">{sf.name}</p>
                  <p className="text-xs text-slate-400 mt-1 font-semibold">{sf.role} - {sf.department}</p>
                </div>
                <Badge variant="success">Checked In (07:44 AM)</Badge>
              </div>)}
          </div>
        </div>}

      {attView === "faceid" && <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-1 bg-slate-900 border border-slate-850 p-6 rounded-3xl text-white flex flex-col justify-between items-center text-center h-[350px]">
            <div className="w-full">
              <span className="text-[9px] uppercase tracking-wider text-indigo-300 font-extrabold">SANDBOX PERIMETER GATEWAY</span>
              <h4 className="text-sm font-black text-white mt-1.5 leading-none">RFID Reader Hardware Emulator</h4>
            </div>

            <div className="h-32 w-32 bg-slate-800 border-2 border-indigo-500 rounded-2xl overflow-hidden flex flex-col items-center justify-center relative shadow-inner animate-pulse">
              <QrCode className="h-20 w-20 text-indigo-400" />
              <div className="absolute top-0 left-0 right-0 h-1 bg-indigo-500/80 animate-bounce mt-4 shadow-md" />
            </div>

            <Button onClick={handleScanSimulation} className="w-full bg-indigo-600 hover:bg-slate-800 text-xs py-2 rounded-xl h-10 shrink-0">
              Trigger NFC Badge Swipe
            </Button>
          </div>

          <div className="md:col-span-2 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 flex flex-col justify-between h-[350px]">
            <div>
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Hardware verification telemetry logs</h3>
              <p className="text-xs text-slate-440 mt-1 leading-normal font-semibold">Simulated reader status events logged by NFC board chips:</p>
            </div>

            <div className="bg-slate-900 p-5 rounded-2xl border border-slate-800 font-mono text-xs text-emerald-400 space-y-2.5 min-h-[140px]">
              <p className="text-slate-500 font-sans tracking-wide font-black">--- SANDBOX HARDWARE ACTIVITY STREAM ---</p>
              <p className="font-medium text-slate-400 leading-none">[09:02 PM] NFC Scanner board online... 10.0.3.14</p>
              
              {scanningStatus === "Idle" && <p className="font-bold text-slate-500 leading-none">&gt; Status: Waiting for RFID badge swipe...</p>}
              {scanningStatus === "Searching..." && <p className="font-bold text-indigo-400 animate-pulse leading-none">&gt; SCANNING... decrypting hex-pass codes...</p>}
              {scanningStatus === "Verified: Student #104" && <div className="space-y-1.5 font-bold">
                  <p className="text-emerald-400 leading-none">&gt; SUCCESS: Card code checked and authorized!</p>
                  <p className="text-slate-300 leading-none">Holder: Liam Smith (Grade 7 Pioneers)</p>
                  <p className="text-slate-500 leading-none">Allocated: Vanguard House 104 Bed 2</p>
                </div>}
            </div>

            <div className="bg-indigo-50 border border-indigo-100 p-3 rounded-xl text-[10px] text-indigo-905 flex items-center gap-1.5 font-black">
              <Sparkles className="h-4 w-4 shrink-0 text-indigo-600" /> Swipe simulator writes credentials straight to log lists.
            </div>
          </div>
        </div>}

    </div>;
}
