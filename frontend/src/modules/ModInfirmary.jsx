/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import { getInfirmary, saveInfirmary, getStudents, logAction } from "../storage";
import { api } from "../api";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { Heart, Activity, Pill, ShieldAlert, Plus } from "lucide-react";
export default function ModInfirmary({ user }) {
  const { addToast } = useToast();
  const [records, setRecords] = useState(() => getInfirmary());
  const [students] = useState(() => getStudents());
  const [isLogOpen, setIsLogOpen] = useState(false);
  const [logStudentDocsId, setLogStudentDocsId] = useState("student-1");
  const [symptoms, setSymptoms] = useState("Acute dry cough and low-grade fatigue");
  const [temperature, setTemperature] = useState("98.6");
  const [treatment, setTreatment] = useState("Administered OTC lozenges, requested dorm bedrest");
  const [status, setStatus] = useState("Placed on Bedrest");

  useEffect(() => {
    let isMounted = true;
    api.getMedicalRecords().then((res) => {
      if (isMounted && Array.isArray(res) && res.length > 0) {
        setRecords(res);
      }
    }).catch(() => {});
    return () => { isMounted = false; };
  }, []);

  const handleCreateRecord = (e) => {
    e.preventDefault();
    const targetStudentDetails = students.find((s) => s.id === logStudentDocsId);
    if (!targetStudentDetails) {
      addToast("Error", " Roster file student lookup mismatched", "error");
      return;
    }
    const tempParsed = parseFloat(temperature) || 98.6;
    const newRecord = {
      id: `health-${Date.now()}`,
      studentDocsId: logStudentDocsId,
      studentName: targetStudentDetails.name,
      checkInTime: new Date().toISOString().replace("T", " ").substring(0, 16),
      temperature: tempParsed,
      symptoms,
      treatment,
      status
    };
    const updated = [newRecord, ...records];
    setRecords(updated);
    saveInfirmary(updated);
    api.createMedicalRecord({
      schoolId: 'SCH-001',
      studentDocsId: logStudentDocsId,
      studentName: targetStudentDetails.name,
      temperature: tempParsed,
      symptoms,
      treatment,
      status
    }).catch(() => {});
    logAction(
      user.id,
      user.name,
      user.role,
      "Infirmary Record Logged",
      `Filed health checkup for student ${newRecord.studentName}. Diagnostic status: ${status}. Temperature: ${temperature}\xB0F`
    );
    if (tempParsed >= 101) {
      addToast("Crisis Alert Triggered", "High body heat index (>101\xB0F) reported. Parent quarantine email sent.", "warning");
    } else {
      addToast("Success", `Logged general checkup visit of ${newRecord.studentName}`);
    }
    setIsLogOpen(false);
  };
  const [cabinetInv, setCabinetInv] = useState([
    { name: "Paracetamol Tablets 500mg", stock: "240 pills", level: "Ok" },
    { name: "Cold Compress Packs", stock: "8 packs", level: "Needs Restock" },
    { name: "Antibacterial Bandages", stock: "180 units", level: "Ok" },
    { name: "Digital Probe Cover Shields", stock: "30 units", level: "Ok" }
  ]);
  return <div className="space-y-6">

      {
    /* Stats analytical header banners */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">TOTAL INFIRMARY CHECK-INS</span>
            <h4 className="text-2xl font-black text-slate-850 mt-1">{records.length} Admissions</h4>
            <p className="text-xs text-slate-450 mt-1">Under strict nursing logs</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Activity className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-rose-950 text-white p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-rose-300 font-extrabold">BEDREST CONFINEMENT</span>
            <h4 className="text-2xl font-black text-white mt-1">
              {records.filter((r) => r.status === "Placed on Bedrest").length} Students
            </h4>
            <p className="text-xs text-rose-200 mt-1">Recovering in premium wards</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <ShieldAlert className="h-5.5 w-5.5 text-rose-300 animate-pulse" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">CABINET STOCK ALERT</span>
            <h4 className="text-2xl font-black text-slate-805 mt-1">1 Needs Restock</h4>
            <p className="text-xs text-slate-400 mt-1">First-aid cold packs levels</p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl">
            <Pill className="h-6 w-6" />
          </div>
        </div>
      </div>

      {
    /* Controls row */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight">Infirmary Medical Desk</h2>
          <p className="text-xs text-slate-440 font-semibold mt-1">Record student fever indexes, administer medication chits, and monitor quarantine bed recovery quotas.</p>
        </div>
        <Button onClick={() => setIsLogOpen(true)} className="flex gap-2 items-center text-xs py-2 bg-slate-900 border border-transparent font-extrabold shrink-0">
          <Plus className="h-4.5 w-4.5" /> Prescribe Treatment Log
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {
    /* Left lists: Clinic Trails */
  }
        <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Live Treatment logs</h3>

          <div className="space-y-3.5 pt-1">
            {records.map((rec) => <div key={rec.id} className="p-4 bg-slate-50 border border-slate-100 rounded-3xl flex flex-col sm:flex-row justify-between gap-4">
                <div className="space-y-1.5 flex-1 select-text">
                  <div className="flex flex-wrap gap-2.5 items-center">
                    <span className="text-xs font-black text-slate-805">{rec.studentName}</span>
                    <Badge variant={rec.status === "Resolved" ? "success" : rec.status === "Placed on Bedrest" ? "danger" : "warning"}>
                      {rec.status}
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-450 font-bold">Diagnosed: {rec.checkInTime}</p>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                    <div className="bg-white p-2.5 border border-slate-150 rounded-xl">
                      <span className="text-[9px] uppercase tracking-wider text-slate-400 font-extrabold block">Symptoms declaration:</span>
                      <p className="text-xs text-slate-700 font-semibold italic">"{rec.symptoms}"</p>
                    </div>
                    <div className="bg-white p-2.5 border border-slate-150 rounded-xl">
                      <span className="text-[9px] uppercase tracking-wider text-slate-400 font-extrabold block">Prescribed Action:</span>
                      <p className="text-xs text-slate-700 font-semibold">"{rec.treatment}"</p>
                    </div>
                  </div>
                </div>

                <div className="flex flex-col justify-end items-end shrink-0 w-full sm:w-auto">
                  <span className={`text-base font-black flex items-center gap-1 ${rec.temperature >= 101 ? "text-rose-600 animate-pulse" : "text-slate-705"}`}>
                    <Activity className="h-4 w-4" /> {rec.temperature}°F
                  </span>
                  <p className="text-[10px] text-slate-400 font-semibold mt-1">Thermometer scale</p>
                </div>
              </div>)}
          </div>
        </div>

        {
    /* Right side checkup cards */
  }
        <div className="space-y-6">

          <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-4">
            <h4 className="text-xs font-bold text-slate-700 uppercase tracking-widest">Clinic Cabinet stocks</h4>
            <div className="space-y-2.5">
              {cabinetInv.map((cab, idx) => <div key={idx} className="text-xs font-semibold flex justify-between items-center leading-none">
                  <span className="text-slate-600 font-bold">{cab.name}</span>
                  <div className="flex gap-2 items-center font-black">
                    <span className="text-slate-800">{cab.stock}</span>
                    <Badge variant={cab.level === "Ok" ? "success" : "warning"}>{cab.level}</Badge>
                  </div>
                </div>)}
            </div>
          </div>

          <div className="bg-indigo-50 border border-indigo-100 p-5 rounded-3xl space-y-3">
            <h4 className="text-xs font-bold text-indigo-900 uppercase tracking-widest flex items-center gap-1.5">
              <Heart className="h-4.5 w-4.5 text-indigo-700" /> Medical Ward Directives
            </h4>
            <p className="text-xs text-indigo-950 font-semibold leading-relaxed">
              Isolation protocols are automatically activated if body temperature registers above 101 Fahrenheit (38.3 Celsius) to prevent classroom contagion spreads.
            </p>
          </div>

        </div>

      </div>

      {
    /* Modal: New Treatment Form */
  }
      <Dialog isOpen={isLogOpen} onClose={() => setIsLogOpen(false)} title="Prescribe Treatment Clinic log">
        <form onSubmit={handleCreateRecord} className="space-y-4 pt-1">
          <Select
    label="Select Patient Student"
    options={students.slice(0, 45).map((s) => ({ label: `${s.name} (${s.admissionNo})`, value: s.id }))}
    value={logStudentDocsId}
    onChange={(e) => setLogStudentDocsId(e.target.value)}
  />

          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Diagnostic body temperature (°F)"
    type="text"
    value={temperature}
    onChange={(e) => setTemperature(e.target.value)}
    placeholder="98.6"
    required
  />
            <Select
    label="Infirmary Recovery status"
    options={[
      { label: "Place under quarantine Bedrest", value: "Placed on Bedrest" },
      { label: "Refer to outside clinic partner", value: "Referred to Hospital" },
      { label: "Resolved (Sent back to dorm)", value: "Resolved" }
    ]}
    value={status}
    onChange={(e) => setStatus(e.target.value)}
  />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-600 tracking-wider block mb-1.5 uppercase">Symptoms details</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-205 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-505 focus:bg-white max-h-32 transition"
    rows={2}
    placeholder="Dry sore throat, migraine complaints, minor sprained ankle..."
    value={symptoms}
    onChange={(e) => setSymptoms(e.target.value)}
    required
  />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-600 tracking-wider block mb-1.5 uppercase">Prescribed Treatment or Medicines</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-205 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-505 focus:bg-white max-h-32 transition"
    rows={2}
    placeholder="Administered paracetamol tablets, bandaged ankle joint with cold compresses..."
    value={treatment}
    onChange={(e) => setTreatment(e.target.value)}
    required
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsLogOpen(false)}>Cancel Log</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900 border border-transparent font-extrabold text-white">
              Log Clinic Checkup Visit
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
