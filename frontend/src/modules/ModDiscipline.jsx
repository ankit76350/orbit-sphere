/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import {
  getStudents,
  getDiscipline,
  saveDiscipline,
  deductWallet,
  logAction
} from "../storage";
import { api } from "../api";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { AlertOctagon, Scale, Plus, ShieldCheck, HelpCircle } from "lucide-react";
export default function ModDiscipline({ user }) {
  const { addToast } = useToast();
  const [violations, setViolations] = useState(() => getDiscipline());
  const [students, setStudents] = useState(() => getStudents());
  const [isNewOpen, setIsNewOpen] = useState(false);
  const [targetStudentId, setTargetStudentId] = useState("student-1");
  const [violationType, setViolationType] = useState("Curfew Breach: Sneaking snacks post roll-call");
  const [severity, setSeverity] = useState("Medium");
  const [incidentDate, setIncidentDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [fineAmount, setFineAmount] = useState("25");
  const [actionsTaken, setActionsTaken] = useState("Auto-deducted fine from NFC balance and sent warning alert.");

  useEffect(() => {
    let isMounted = true;
    api.getDisciplineLogs().then((res) => {
      if (isMounted && Array.isArray(res) && res.length > 0) {
        setViolations(res);
      }
    }).catch(() => {});
    return () => { isMounted = false; };
  }, []);

  const handleCreateViolation = (e) => {
    e.preventDefault();
    const studentsList = getStudents();
    const targetStudent = studentsList.find((s) => s.id === targetStudentId);
    if (!targetStudent) {
      addToast("Error", "Target student roster not found in LocalStorage", "error");
      return;
    }
    const fineVal = parseFloat(fineAmount) || 0;
    if (fineVal > 0) {
      const pass = deductWallet(
        targetStudentId,
        fineVal,
        "Fine Deduction",
        `Disciplinary penalty: ${violationType}`,
        user.name,
        user.role
      );
      if (!pass) {
        addToast("Information", "Executed fine debit from student RFID ledger", "info");
      }
    }
    const newViolation = {
      id: `violation-${Date.now()}`,
      studentId: targetStudentId,
      studentName: targetStudent.name,
      violationType,
      severity,
      incidentDate,
      wardenOrTeacher: user.name,
      status: fineVal > 0 ? "Fine Deducted" : "Warning Issued",
      fineAmount: fineVal > 0 ? fineVal : void 0,
      actionsTaken
    };
    const updated = [newViolation, ...violations];
    setViolations(updated);
    saveDiscipline(updated);
    api.createDisciplineLog({
      schoolId: 'SCH-001',
      academicYear: '2026-2027',
      studentId: targetStudentId,
      studentName: targetStudent.name,
      violationType,
      severity,
      incidentDate,
      actionTaken: actionsTaken,
      fineAmount: fineVal
    }).catch(() => {});
    logAction(
      user.id,
      user.name,
      user.role,
      "Discipline Ticket Created",
      `Filed infraction for student ${targetStudent.name}. Penalty: $${fineVal} fine deducted. Status: ${newViolation.status}`
    );
    addToast("Success", `Infraction registered. Penalty of $${fineVal} processed.`, "success");
    setIsNewOpen(false);
    setFineAmount("25");
    setActionsTaken("Auto-deducted fine from NFC balance and sent warning alert.");
  };
  const totalCases = violations.length;
  const highSeverityCount = violations.filter((v) => v.severity === "High").length;
  const totalFinesCollected = violations.reduce((sum, v) => sum + (v.fineAmount || 0), 0);
  return <div className="space-y-6">

      {
    /* Analytical row cards */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">TOTAL REPORTED CASES</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{totalCases} Infractions</h4>
            <p className="text-xs text-slate-450 mt-1">Dorm curfews & assembly absences</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Scale className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 text-indigo-100 p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">HIGH SEVERITY BREACHES</span>
            <h4 className="text-2xl font-black text-white mt-1">{highSeverityCount} Cases</h4>
            <p className="text-xs text-indigo-200 mt-1">Under strict principal review</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <AlertOctagon className="h-5.5 w-5.5 text-rose-400 animate-bounce" />
          </div>
        </div>

        <div className="bg-emerald-950 text-white p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-emerald-300">PENALTY VALS DEBITED</span>
            <h4 className="text-2xl font-black text-white mt-1">${totalFinesCollected.toLocaleString()} USD</h4>
            <p className="text-xs text-emerald-200 mt-1">Deducted directly from RFID tags</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-emerald-300">
            <ShieldCheck className="h-5.5 w-5.5" />
          </div>
        </div>
      </div>

      {
    /* Main Board Action Area */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight">Disciplinary Oversight Desk</h2>
          <p className="text-xs text-slate-404 font-semibold mt-1">Log curfews breach tickets, execute direct fine deductions from pocket student wallets, and manage suspensions.</p>
        </div>
        <Button onClick={() => setIsNewOpen(true)} className="flex gap-2 items-center text-xs py-2 bg-slate-900 border border-transparent font-extrabold shrink-0">
          <Plus className="h-4.5 w-4.5" /> Log Infraction Ticket
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {
    /* Left: active violations trail */
  }
        <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Active Incident Logs</h3>

          <div className="space-y-3.5 pt-1">
            {violations.map((viol) => <div key={viol.id} className="p-4 bg-slate-50 border border-slate-100 rounded-3xl flex flex-col sm:flex-row justify-between gap-4">
                <div className="space-y-1.5 flex-1 select-text">
                  <div className="flex flex-wrap gap-2.5 items-center">
                    <span className="text-xs font-black text-slate-800">{viol.studentName}</span>
                    <Badge variant={viol.severity === "High" ? "danger" : viol.severity === "Medium" ? "warning" : "default"}>
                      {viol.severity} Breach
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-450 font-bold">Warden: {viol.wardenOrTeacher} | Date: {viol.incidentDate}</p>
                  
                  <div className="bg-white p-3 border border-slate-150 rounded-xl mt-2">
                    <p className="text-[10px] text-slate-400 font-extrabold uppercase mb-1">Violation: {viol.violationType}</p>
                    <p className="text-xs text-slate-600 font-semibold leading-relaxed">"{viol.actionsTaken}"</p>
                  </div>
                </div>

                <div className="flex flex-col justify-between items-end shrink-0 w-full sm:w-auto">
                  <Badge variant="secondary">{viol.status}</Badge>
                  {viol.fineAmount && <p className="text-xs font-black text-rose-600 mt-2">NFC Penalty: ${viol.fineAmount} USD</p>}
                </div>
              </div>)}
          </div>
        </div>

        {
    /* Right checklist rulebook */
  }
        <div className="space-y-6">
          <div className="bg-slate-900 text-white p-6 rounded-3xl space-y-4 shadow-md border border-slate-850">
            <h4 className="text-xs font-bold text-indigo-305 uppercase tracking-widest flex items-center gap-2">
              <Scale className="h-4.5 w-4.5" /> High Assembly Codes
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed font-semibold">
              Warden fine limits are capped up to $100 per semester. All wallet fine deductions issue systematic telemetry receipt logs straight to guardian profiles via Parent Portal.
            </p>
            <div className="h-px bg-white/5 pt-1" />
            <div className="text-[11px] text-indigo-300 font-bold flex items-center justify-between">
              <span>Automatic parent notification email dispatch</span>
              <span className="text-emerald-400">● REAL-TIME ON</span>
            </div>
          </div>

          <div className="bg-indigo-50 border border-indigo-100 p-5 rounded-3xl space-y-3">
            <h4 className="text-xs font-bold text-indigo-900 uppercase tracking-widest flex items-center gap-2">
              <HelpCircle className="h-4.5 w-4.5 text-indigo-600 animate-pulse" /> Fine Deducting Safety
            </h4>
            <p className="text-xs text-indigo-950 font-semibold leading-relaxed">
              If an arbitrary fine amount excels over the student's current wallet balance, the balance will deduct into negative values to hold them accountable.
            </p>
          </div>
        </div>

      </div>

      {
    /* Popup infractor ticket */
  }
      <Dialog isOpen={isNewOpen} onClose={() => setIsNewOpen(false)} title="Log New Infraction Desk Ticket">
        <form onSubmit={handleCreateViolation} className="space-y-4 pt-1">
          <Select
    label="Designate Scholar Student"
    options={students.slice(0, 45).map((s) => ({ label: `${s.name} (${s.admissionNumber}) - available NFC balance: $${s.walletBalance}`, value: s.id }))}
    value={targetStudentId}
    onChange={(e) => setTargetStudentId(e.target.value)}
  />

          <div className="grid grid-cols-2 gap-4">
            <Select
    label="Infraction Category Type"
    options={[
      { label: "Curfew Breach: Sneaking snacks post roll-call", value: "Curfew Breach: Sneaking snacks post roll-call" },
      { label: "Incomplete Uniform during assembly", value: "Incomplete Uniform during assembly" },
      { label: "Possession of prohibited game console", value: "Possession of prohibited game console" },
      { label: "Absent from Evening Roll-call", value: "Absent from Evening Roll-call" },
      { label: "Vandalism study desks planks", value: "Vandalism study desks planks" }
    ]}
    value={violationType}
    onChange={(e) => setViolationType(e.target.value)}
  />
            <Select
    label="Incident Severity"
    options={[
      { label: "Low Infraction", value: "Low" },
      { label: "Medium Infraction", value: "Medium" },
      { label: "High Infraction (Suspension Check)", value: "High" }
    ]}
    value={severity}
    onChange={(e) => setSeverity(e.target.value)}
  />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Incident Calendar Date"
    type="date"
    value={incidentDate}
    onChange={(e) => setIncidentDate(e.target.value)}
    required
  />
            <Input
    label="RFID Automatic Penalty ($ USD)"
    type="number"
    value={fineAmount}
    onChange={(e) => setFineAmount(e.target.value)}
    placeholder="25"
    required
  />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-600 block mb-1.5 uppercase">Actions Taken Notes</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-505 focus:bg-white max-h-32 transition"
    rows={3}
    placeholder="e.g. Conducted counseling session, confiscated game console and auto-charged card..."
    value={actionsTaken}
    onChange={(e) => setActionsTaken(e.target.value)}
    required
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewOpen(false)}>Cancel File</Button>
            <Button type="submit" className="bg-rose-600 hover:bg-slate-900 border border-transparent font-extrabold text-white">
              Deduct Face Fine & Submit Ticket
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
