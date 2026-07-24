/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  ShieldCheck,
  Fingerprint,
  Database,
  BookOpenCheck,
  Lock,
  CalendarClock,
  Download,
  RefreshCw,
  Sparkles,
  AlertTriangle,
  CheckCircle,
  Info,
  ArrowRight,
  FileSpreadsheet,
  Send,
  Eye
} from "lucide-react";

// ============ LocalStorage helpers ============
const loadLS = (key, seed) => {
  const raw = localStorage.getItem(key);
  if (raw) return JSON.parse(raw);
  localStorage.setItem(key, JSON.stringify(seed));
  return seed;
};
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));

// ============ Deterministic fake-ID generators ============
const hashNum = (str) => {
  let h = 7;
  for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) >>> 0;
  return h;
};
const fakeDigits = (seedStr, len) => {
  let h = hashNum(seedStr);
  let out = "";
  while (out.length < len) {
    h = (h * 1103515245 + 12345) >>> 0;
    out += String(h).padStart(4, "0").slice(-4);
  }
  return out.slice(0, len);
};

// ============ Seeds ============
const buildApaarSeed = (students) =>
  students.map((s, i) => {
    const mod = i % 7;
    const hasApaar = mod !== 2 && mod !== 5;
    return {
      studentDocsId: s.id,
      name: s.name,
      admissionNo: s.admissionNo,
      grade: s.grade,
      pen: fakeDigits(`pen-${s.id}`, 11),
      apaarNo: hasApaar ? fakeDigits(`apaar-${s.id}`, 12) : null,
      aadhaarVerified: mod !== 5,
      digilockerLinked: hasApaar && mod !== 3,
      status: hasApaar ? "Generated" : mod === 5 ? "Error" : "Pending"
    };
  });

const DPDP_SEED = {
  consents: [
    { id: "c-photo", type: "Photo / School Gallery Use", granted: 118, pending: 22, withdrawn: 4 },
    { id: "c-bio", type: "Biometric Attendance (Fingerprint)", granted: 96, pending: 41, withdrawn: 7 },
    { id: "c-gps", type: "Transport GPS Live Tracking", granted: 84, pending: 12, withdrawn: 2 },
    { id: "c-ai", type: "AI Features Data Processing", granted: 72, pending: 63, withdrawn: 9 },
    { id: "c-wa", type: "WhatsApp Communication", granted: 131, pending: 10, withdrawn: 3 }
  ],
  retention: [
    { record: "Admission & TC Records", years: "10" },
    { record: "Attendance Registers", years: "5" },
    { record: "CCTV Footage", years: "1" },
    { record: "Health / Infirmary Records", years: "7" },
    { record: "Fee & Payment Ledgers", years: "8" }
  ]
};

const COMPLIANCE_CALENDAR = [
  { id: "cal-1", title: "UDISE+ Annual Return (DCF Submission)", authority: "MoE / UDISE+ Portal", due: "2026-07-31" },
  { id: "cal-2", title: "CBSE Board LOC (List of Candidates) Submission", authority: "CBSE Regional Office", due: "2026-08-20" },
  { id: "cal-3", title: "RTE 25% Quota Admission Report", authority: "State Education Dept.", due: "2026-07-15" },
  { id: "cal-4", title: "APAAR ID Saturation Drive Report", authority: "DoSEL / APAAR Cell", due: "2026-09-05" },
  { id: "cal-5", title: "DPDP Consent Register Audit (Internal)", authority: "School DPO", due: "2026-10-01" }
];

const UDISE_CHECKLIST = [
  { id: "u-1", label: "Student Profiles Complete", pct: 92, issues: 12, note: "General info, category, CWSN fields" },
  { id: "u-2", label: "Teacher Profiles & Qualifications", pct: 88, issues: 4, note: "B.Ed / TET details, appointment type" },
  { id: "u-3", label: "Infrastructure & Facility Data", pct: 100, issues: 0, note: "Classrooms, toilets, ramps, library, labs" },
  { id: "u-4", label: "Enrolment by Social Category", pct: 76, issues: 31, note: "SC/ST/OBC/Minority tagging pending" }
];

const UDISE_ISSUES = [
  { id: "i-1", issue: "12 students missing mother's name", block: "Student General Profile", severity: "High" },
  { id: "i-2", issue: "31 students without social category (SC/ST/OBC) tag", block: "Enrolment Profile", severity: "High" },
  { id: "i-3", issue: "3 students missing blood group", block: "Health Profile", severity: "Low" },
  { id: "i-4", issue: "4 teachers missing TET certificate number", block: "Teacher Profile", severity: "Medium" },
  { id: "i-5", issue: "7 Aadhaar numbers failed checksum validation", block: "Student ID Verification", severity: "Medium" },
  { id: "i-6", issue: "2 duplicate PEN entries detected across sections", block: "PEN Registry", severity: "High" }
];

// HPC domains (NEP 2020)
const HPC_DOMAINS = [
  { key: "physical", label: "Physical Development", hint: "Motor skills, fitness, health & yoga habits" },
  { key: "socio", label: "Socio-Emotional Development", hint: "Empathy, teamwork, self-regulation" },
  { key: "cognitive", label: "Cognitive Development", hint: "Critical thinking, numeracy, problem solving" },
  { key: "language", label: "Language & Literacy", hint: "Reading, expression, multilingual ability" },
  { key: "aesthetic", label: "Aesthetic & Cultural", hint: "Art, music, theatre, cultural awareness" }
];

const HPC_LEVELS = [
  { label: "Beginner (Stream)", value: "Stream" },
  { label: "Progressing (River)", value: "River" },
  { label: "Proficient (Mountain)", value: "Mountain" },
  { label: "Advanced (Sky)", value: "Sky" }
];

const HPC_AI_TEXT = {
  physical: (n) => `${n} shows consistent enthusiasm during morning PT and yoga sessions. Gross-motor coordination has improved visibly this term; encouraged to maintain hydration and posture habits during sports periods.`,
  socio: (n) => `${n} demonstrates warmth and inclusiveness in group settings, often mediating small peer conflicts calmly. Continues to build confidence when presenting feelings in circle-time discussions.`,
  cognitive: (n) => `${n} approaches problem-solving tasks with curiosity, breaking multi-step problems into parts. Shows a growing ability to justify reasoning; abstract pattern work is the next growth frontier.`,
  language: (n) => `${n} reads grade-level texts fluently and is expanding expressive vocabulary in both English and Hindi. Creative writing shows imaginative flair; focus area is structured paragraph transitions.`,
  aesthetic: (n) => `${n} engages joyfully in art and music activities, experimenting with colour and rhythm. Participated keenly in the folk-dance ensemble; shows budding appreciation of regional art forms.`
};

const emptyHpcForm = () => {
  const f = {};
  HPC_DOMAINS.forEach((d) => {
    f[d.key] = { level: "River", observation: "" };
  });
  return f;
};

export default function ModCompliance({ user }) {
  const { addToast } = useToast();
  const [students] = useState(() => getStudents());

  const [activeTab, setActiveTab] = useState("dashboard");

  // Persisted stores
  const [apaar, setApaar] = useState(() => loadLS("erp_apaar", buildApaarSeed(getStudents())));
  const [hpcRecords, setHpcRecords] = useState(() => loadLS("erp_hpc", []));
  const [dpdp, setDpdp] = useState(() => loadLS("erp_dpdp", DPDP_SEED));

  // APAAR generation simulation
  const [apaarProgress, setApaarProgress] = useState(-1);

  // HPC builder state
  const [hpcStudentDocsId, setHpcStudentDocsId] = useState(() => (getStudents()[0] ? getStudents()[0].id : ""));
  const [hpcForm, setHpcForm] = useState(() => emptyHpcForm());
  const [hpcSelf, setHpcSelf] = useState("");
  const [hpcPeer, setHpcPeer] = useState("");
  const [hpcParent, setHpcParent] = useState("");
  const [hpcGoals, setHpcGoals] = useState("");
  const [hpcAiBusy, setHpcAiBusy] = useState(false);

  // DPDP drilldown
  const [drillConsent, setDrillConsent] = useState(null);

  const doLog = (action, details) =>
    logAction(user?.id || "sandbox", user?.name || "User", user?.role || "Staff", action, details);

  // ============ Derived KPIs ============
  const apaarGeneratedPct = apaar.length
    ? Math.round((apaar.filter((a) => a.status === "Generated").length / apaar.length) * 100)
    : 0;
  const udiseReadinessPct = Math.round(UDISE_CHECKLIST.reduce((s, c) => s + c.pct, 0) / UDISE_CHECKLIST.length);
  const hpcCompletionPct = students.length ? Math.round((hpcRecords.length / students.length) * 100) : 0;
  const dpdpTotals = dpdp.consents.reduce(
    (acc, c) => ({ granted: acc.granted + c.granted, all: acc.all + c.granted + c.pending + c.withdrawn }),
    { granted: 0, all: 0 }
  );
  const dpdpPct = dpdpTotals.all ? Math.round((dpdpTotals.granted / dpdpTotals.all) * 100) : 0;

  const daysUntil = (dateStr) => {
    const diff = new Date(dateStr).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  };

  // ============ APAAR handlers ============
  const handleGenerateMissing = () => {
    const missing = apaar.filter((a) => a.status !== "Generated").length;
    if (missing === 0) {
      addToast("Nothing Pending", "All students already carry a generated APAAR ID.", "info");
      return;
    }
    setApaarProgress(0);
    const interval = setInterval(() => {
      setApaarProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          const updated = apaar.map((a) =>
            a.status === "Generated"
              ? a
              : {
                  ...a,
                  apaarNo: fakeDigits(`apaar-new-${a.studentDocsId}-${Date.now()}`, 12),
                  aadhaarVerified: true,
                  digilockerLinked: true,
                  status: "Generated"
                }
          );
          setApaar(updated);
          saveLS("erp_apaar", updated);
          doLog("APAAR Bulk Generation", `Generated APAAR IDs for ${missing} pending/errored students via UDISE verification chain.`);
          addToast("APAAR IDs Generated", `${missing} pending records verified against UDISE+ and pushed to DigiLocker.`, "success");
          return -1;
        }
        return prev + 20;
      });
    }, 350);
  };

  const handleRetryRow = (studentDocsId) => {
    const updated = apaar.map((a) =>
      a.studentDocsId === studentDocsId
        ? {
            ...a,
            apaarNo: fakeDigits(`apaar-retry-${studentDocsId}-${Date.now()}`, 12),
            aadhaarVerified: true,
            digilockerLinked: true,
            status: "Generated"
          }
        : a
    );
    setApaar(updated);
    saveLS("erp_apaar", updated);
    const row = apaar.find((a) => a.studentDocsId === studentDocsId);
    doLog("APAAR Retry", `Re-attempted APAAR generation for ${row ? row.name : studentDocsId}. Verification passed.`);
    addToast("Retry Successful", "Aadhaar demographic match passed. APAAR ID minted and DigiLocker-linked.", "success");
  };

  // ============ UDISE CSV export ============
  const handleExportUdiseCsv = () => {
    const header = ["AdmissionNo", "PEN", "StudentName", "Grade", "Gender", "DOB", "SocialCategory", "BloodGroup"];
    const cats = ["General", "OBC", "SC", "ST"];
    const rows = students.slice(0, 10).map((s, i) => {
      const rec = apaar.find((a) => a.studentDocsId === s.id);
      return [
        s.admissionNo,
        rec ? rec.pen : fakeDigits(`pen-${s.id}`, 11),
        `"${s.name}"`,
        `"${s.grade}"`,
        s.gender,
        s.dob,
        cats[i % 4],
        s.medicalBloodGroup || ""
      ].join(",");
    });
    const csv = [header.join(","), ...rows].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "UDISE_Plus_Annual_Return_2026-27.csv";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    doLog("UDISE+ Export", `Exported UDISE+ annual return CSV draft with ${rows.length} student rows.`);
    addToast("UDISE+ Return Exported", `CSV draft with ${rows.length} student rows downloaded. Upload to the UDISE+ portal DCF module.`, "success");
  };

  // ============ HPC handlers ============
  const hpcStudent = students.find((s) => s.id === hpcStudentDocsId);

  const handleHpcAiDraft = () => {
    if (!hpcStudent) return;
    setHpcAiBusy(true);
    setTimeout(() => {
      const next = { ...hpcForm };
      HPC_DOMAINS.forEach((d) => {
        next[d.key] = { ...next[d.key], observation: HPC_AI_TEXT[d.key](hpcStudent.name.split(" ")[0]) };
      });
      setHpcForm(next);
      setHpcSelf("I enjoyed our science projects the most this term. I want to get better at speaking on stage.");
      setHpcPeer("Always helps the group finish charts on time and shares stationery without being asked.");
      setHpcParent("We have seen more independent reading at home. Evenings now include self-planned revision.");
      setHpcGoals("1) Lead one classroom presentation next term. 2) Join inter-house athletics. 3) Read two Hindi storybooks per month.");
      setHpcAiBusy(false);
      doLog("HPC AI Draft", `AI-drafted holistic descriptors for ${hpcStudent.name} across 5 NEP domains.`);
      addToast("AI Draft Ready", "Descriptor drafts filled for all 5 domains. Review and edit before publishing.", "success");
    }, 1600);
  };

  const handleHpcSave = (publish) => {
    if (!hpcStudent) return;
    const record = {
      id: `hpc-${hpcStudent.id}-${Date.now()}`,
      studentDocsId: hpcStudent.id,
      studentName: hpcStudent.name,
      grade: hpcStudent.grade,
      term: "Term 1, AY 2026-27",
      domains: hpcForm,
      feedback: { self: hpcSelf, peer: hpcPeer, parent: hpcParent },
      goals: hpcGoals,
      status: publish ? "Published" : "Draft",
      savedAt: new Date().toISOString().slice(0, 19).replace("T", " ")
    };
    const next = [...hpcRecords.filter((r) => r.studentDocsId !== hpcStudent.id), record];
    setHpcRecords(next);
    saveLS("erp_hpc", next);
    doLog(publish ? "HPC Published" : "HPC Draft Saved", `Holistic Progress Card ${publish ? "published" : "saved"} for ${hpcStudent.name} (${hpcStudent.grade}).`);
    addToast(
      publish ? "HPC Published" : "Draft Saved",
      publish
        ? `360° Holistic Progress Card for ${hpcStudent.name} is now visible on the parent portal.`
        : `Progress card draft stored for ${hpcStudent.name}.`,
      "success"
    );
  };

  // ============ DPDP handlers ============
  const handleSendConsentRequest = (consentType) => {
    doLog("DPDP Consent Request", `Consent request notices dispatched for "${consentType}" via app + SMS.`);
    addToast("Consent Requests Sent", `Digital consent notices for "${consentType}" queued to all pending parents (app push + SMS, bilingual).`, "success");
  };

  const handleRetentionChange = (idx, years) => {
    const next = { ...dpdp, retention: dpdp.retention.map((r, i) => (i === idx ? { ...r, years } : r)) };
    setDpdp(next);
    saveLS("erp_dpdp", next);
    addToast("Retention Policy Updated", `${next.retention[idx].record}: retain for ${years} year(s), then purge.`, "info");
  };

  // ============ Shared small components ============
  const ProgressBar = ({ pct, color = "bg-blue-600" }) => (
    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
      <div className={`${color} h-full rounded-full transition-all duration-500`} style={{ width: `${pct}%` }} />
    </div>
  );

  const tabs = [
    { key: "dashboard", label: "Compliance Dashboard" },
    { key: "apaar", label: "APAAR ID Registry" },
    { key: "udise", label: "UDISE+ Export" },
    { key: "hpc", label: "HPC Builder (NEP 2020)" },
    { key: "dpdp", label: "DPDP Consent Tracker" }
  ];

  return (
    <div className="space-y-6">
      {/* Header banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-emerald-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <ShieldCheck className="h-6 w-6 text-emerald-600" />
            Compliance & Boards (India)
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            APAAR / One Nation One Student ID, UDISE+ returns, NEP 2020 Holistic Progress Cards and DPDP Act 2023 consent registers.
          </p>
        </div>
        <Badge variant="success" className="text-[10px] font-mono font-black uppercase">
          AY 2026-27 • CBSE AFFILIATED
        </Badge>
      </div>

      {/* Tab pills */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key)}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === t.key ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ================= 1. DASHBOARD ================= */}
      {activeTab === "dashboard" && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-3xl">
              <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">APAAR IDs Generated</span>
              <h4 className="text-2xl font-black text-slate-800 mt-1">{apaarGeneratedPct}%</h4>
              <div className="mt-3"><ProgressBar pct={apaarGeneratedPct} color="bg-emerald-500" /></div>
              <p className="text-[10px] text-slate-400 font-semibold mt-2">{apaar.filter((a) => a.status === "Generated").length} / {apaar.length} students</p>
            </div>
            <div className="bg-indigo-900 text-white p-5 rounded-3xl border border-slate-800">
              <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">UDISE+ Readiness</span>
              <h4 className="text-2xl font-black text-white mt-1">{udiseReadinessPct}%</h4>
              <div className="mt-3 w-full bg-white/10 h-2 rounded-full overflow-hidden">
                <div className="bg-indigo-300 h-full rounded-full" style={{ width: `${udiseReadinessPct}%` }} />
              </div>
              <p className="text-[10px] text-indigo-200 font-semibold mt-2">Annual return DCF blocks validated</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-3xl">
              <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">HPC Completion</span>
              <h4 className="text-2xl font-black text-slate-800 mt-1">{hpcCompletionPct}%</h4>
              <div className="mt-3"><ProgressBar pct={hpcCompletionPct} color="bg-amber-500" /></div>
              <p className="text-[10px] text-slate-400 font-semibold mt-2">{hpcRecords.length} holistic cards built</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-3xl">
              <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">DPDP Consents Collected</span>
              <h4 className="text-2xl font-black text-slate-800 mt-1">{dpdpPct}%</h4>
              <div className="mt-3"><ProgressBar pct={dpdpPct} color="bg-blue-600" /></div>
              <p className="text-[10px] text-slate-400 font-semibold mt-2">{dpdpTotals.granted} grants on register</p>
            </div>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
              <CalendarClock className="h-4 w-4 text-blue-600" /> Compliance Calendar — Statutory Deadlines
            </h3>
            <div className="space-y-3">
              {COMPLIANCE_CALENDAR.map((c) => {
                const d = daysUntil(c.due);
                const variant = d < 15 ? "danger" : d < 45 ? "warning" : "success";
                return (
                  <div key={c.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-4 bg-slate-50 border border-slate-100 rounded-2xl">
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{c.title}</h4>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mt-0.5">{c.authority}</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className="text-[10px] font-mono font-bold text-slate-500">Due {c.due}</span>
                      <Badge variant={variant} className="text-[9px] font-black uppercase">
                        {d < 0 ? "Overdue" : `${d} days left`}
                      </Badge>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* ================= 2. APAAR ================= */}
      {activeTab === "apaar" && (
        <div className="space-y-6">
          <div className="bg-blue-50/50 border border-blue-200 p-4 rounded-3xl flex gap-3 text-blue-950 text-xs">
            <Info className="h-4.5 w-4.5 text-blue-600 shrink-0 mt-0.5" />
            <div className="font-semibold leading-relaxed">
              <p className="font-black">APAAR — One Nation One Student ID data chain:</p>
              <p className="text-blue-800 mt-1 flex flex-wrap items-center gap-1.5 font-bold">
                School Records <ArrowRight className="h-3 w-3" /> UDISE+ Verification (PEN) <ArrowRight className="h-3 w-3" /> APAAR ID Minting <ArrowRight className="h-3 w-3" /> DigiLocker Academic Bank of Credits
              </p>
              <p className="text-blue-700 mt-1">Aadhaar demographic match and verified parental consent are mandatory before an APAAR ID can be minted.</p>
            </div>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-50 pb-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Fingerprint className="h-4 w-4 text-emerald-600" /> APAAR ID Registry
              </h3>
              {apaarProgress >= 0 ? (
                <div className="flex items-center gap-3 w-64">
                  <RefreshCw className="h-4 w-4 text-blue-600 animate-spin shrink-0" />
                  <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
                    <div className="bg-blue-600 h-full rounded-full transition-all duration-300" style={{ width: `${apaarProgress}%` }} />
                  </div>
                  <span className="text-[10px] font-mono font-bold text-slate-500 shrink-0">{apaarProgress}%</span>
                </div>
              ) : (
                <Button onClick={handleGenerateMissing} className="text-xs py-2 flex items-center gap-2">
                  <Sparkles className="h-4 w-4" /> Generate Missing IDs
                </Button>
              )}
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-4">Student</th>
                    <th className="p-4">Admission No</th>
                    <th className="p-4">PEN (UDISE+)</th>
                    <th className="p-4">APAAR ID</th>
                    <th className="p-4">Aadhaar</th>
                    <th className="p-4">DigiLocker</th>
                    <th className="p-4">Status</th>
                    <th className="p-4">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {apaar.slice(0, 30).map((a) => (
                    <tr key={a.studentDocsId}>
                      <td className="p-4 font-extrabold text-slate-800">
                        {a.name}
                        <span className="block text-[9px] text-slate-400 font-bold">{a.grade}</span>
                      </td>
                      <td className="p-4 font-mono text-[10px]">{a.admissionNo}</td>
                      <td className="p-4 font-mono text-[10px] text-slate-500">{a.pen}</td>
                      <td className="p-4 font-mono text-[10px]">
                        {a.apaarNo ? a.apaarNo : <span className="text-slate-400 italic">— pending</span>}
                      </td>
                      <td className="p-4">
                        <Badge variant={a.aadhaarVerified ? "success" : "warning"} className="text-[9px] font-black uppercase">
                          {a.aadhaarVerified ? "Verified" : "Mismatch"}
                        </Badge>
                      </td>
                      <td className="p-4">
                        <Badge variant={a.digilockerLinked ? "info" : "default"} className="text-[9px] font-black uppercase">
                          {a.digilockerLinked ? "Linked" : "Not Linked"}
                        </Badge>
                      </td>
                      <td className="p-4">
                        <Badge
                          variant={a.status === "Generated" ? "success" : a.status === "Error" ? "danger" : "warning"}
                          className="text-[9px] font-black uppercase"
                        >
                          {a.status}
                        </Badge>
                      </td>
                      <td className="p-4">
                        {a.status !== "Generated" && (
                          <Button size="sm" variant="outline" onClick={() => handleRetryRow(a.studentDocsId)} className="text-[10px] flex items-center gap-1">
                            <RefreshCw className="h-3 w-3" /> Retry
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {apaar.length > 30 && (
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                Showing 30 of {apaar.length} students — full registry included in exports.
              </p>
            )}
          </div>
        </div>
      )}

      {/* ================= 3. UDISE+ ================= */}
      {activeTab === "udise" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {UDISE_CHECKLIST.map((c) => (
              <div key={c.id} className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                <div className="flex justify-between items-start gap-2">
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">{c.label}</span>
                  {c.issues === 0 ? (
                    <CheckCircle className="h-4 w-4 text-emerald-500 shrink-0" />
                  ) : (
                    <Badge variant={c.issues > 10 ? "danger" : "warning"} className="text-[9px] font-black shrink-0">{c.issues} issues</Badge>
                  )}
                </div>
                <h4 className="text-2xl font-black text-slate-800">{c.pct}%</h4>
                <ProgressBar pct={c.pct} color={c.pct === 100 ? "bg-emerald-500" : c.pct >= 85 ? "bg-blue-600" : "bg-amber-500"} />
                <p className="text-[10px] text-slate-400 font-semibold">{c.note}</p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-50 pb-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Database className="h-4 w-4 text-blue-600" /> Data Quality Issues
              </h3>
              <Button onClick={handleExportUdiseCsv} variant="secondary" className="text-xs py-2 flex items-center gap-2">
                <Download className="h-4 w-4" /> Export UDISE+ Return (CSV)
              </Button>
            </div>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-4">Issue Description</th>
                    <th className="p-4">DCF Block</th>
                    <th className="p-4">Severity</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {UDISE_ISSUES.map((i) => (
                    <tr key={i.id}>
                      <td className="p-4 font-extrabold text-slate-800 flex items-center gap-2">
                        <AlertTriangle className={`h-3.5 w-3.5 shrink-0 ${i.severity === "High" ? "text-rose-500" : i.severity === "Medium" ? "text-amber-500" : "text-slate-400"}`} />
                        {i.issue}
                      </td>
                      <td className="p-4 text-slate-500">{i.block}</td>
                      <td className="p-4">
                        <Badge variant={i.severity === "High" ? "danger" : i.severity === "Medium" ? "warning" : "default"} className="text-[9px] font-black uppercase">
                          {i.severity}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl text-[11px] text-slate-500 font-semibold leading-relaxed flex gap-2">
              <FileSpreadsheet className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
              The CSV export produces a portal-ready draft of the annual return (student block) with PEN, social category and health fields. Resolve High severity issues before final DCF submission.
            </div>
          </div>
        </div>
      )}

      {/* ================= 4. HPC ================= */}
      {activeTab === "hpc" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Builder form */}
          <div className="lg:col-span-6 bg-white border border-slate-100 rounded-3xl p-6 space-y-5 h-fit">
            <div className="flex justify-between items-center border-b border-slate-50 pb-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <BookOpenCheck className="h-4 w-4 text-amber-600" /> Holistic Progress Card Builder
              </h3>
              <Badge variant="warning" className="text-[9px] font-black uppercase">NEP 2020 • 360°</Badge>
            </div>

            <div className="flex gap-3 items-end">
              <Select
                label="Select Learner"
                options={students.map((s) => ({ label: `${s.name} — ${s.grade}`, value: s.id }))}
                value={hpcStudentDocsId}
                onChange={(e) => {
                  setHpcStudentDocsId(e.target.value);
                  const existing = hpcRecords.find((r) => r.studentDocsId === e.target.value);
                  if (existing) {
                    setHpcForm(existing.domains);
                    setHpcSelf(existing.feedback.self);
                    setHpcPeer(existing.feedback.peer);
                    setHpcParent(existing.feedback.parent);
                    setHpcGoals(existing.goals);
                  } else {
                    setHpcForm(emptyHpcForm());
                    setHpcSelf("");
                    setHpcPeer("");
                    setHpcParent("");
                    setHpcGoals("");
                  }
                }}
              />
              <Button onClick={handleHpcAiDraft} disabled={hpcAiBusy} variant="secondary" className="text-xs py-2.5 shrink-0 flex items-center gap-2">
                {hpcAiBusy ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                {hpcAiBusy ? "Drafting..." : "AI Draft Descriptors"}
              </Button>
            </div>

            {hpcAiBusy && (
              <div className="p-3 bg-blue-50/50 border border-blue-100 rounded-2xl text-[10px] font-bold text-blue-700 animate-pulse">
                Analysing term observations, co-curricular logs and assessment rubrics to draft competency descriptors...
              </div>
            )}

            {HPC_DOMAINS.map((d) => (
              <div key={d.key} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-3">
                <div className="flex justify-between items-center">
                  <div>
                    <h4 className="text-xs font-black text-slate-800">{d.label}</h4>
                    <p className="text-[9px] text-slate-400 font-bold uppercase tracking-wider mt-0.5">{d.hint}</p>
                  </div>
                </div>
                <Select
                  label="Competency Level"
                  options={HPC_LEVELS}
                  value={hpcForm[d.key].level}
                  onChange={(e) => setHpcForm({ ...hpcForm, [d.key]: { ...hpcForm[d.key], level: e.target.value } })}
                />
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Teacher Observation</label>
                  <textarea
                    value={hpcForm[d.key].observation}
                    onChange={(e) => setHpcForm({ ...hpcForm, [d.key]: { ...hpcForm[d.key], observation: e.target.value } })}
                    rows={2}
                    placeholder="Anecdotal, evidence-based observation..."
                    className="bg-white border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 transition resize-none"
                  />
                </div>
              </div>
            ))}

            <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-3">
              <h4 className="text-xs font-black text-slate-800">360° Feedback</h4>
              <Input label="Self Assessment (Learner Voice)" value={hpcSelf} onChange={(e) => setHpcSelf(e.target.value)} placeholder="What the learner says about their term..." />
              <Input label="Peer Feedback" value={hpcPeer} onChange={(e) => setHpcPeer(e.target.value)} placeholder="A classmate's reflection..." />
              <Input label="Parent Feedback" value={hpcParent} onChange={(e) => setHpcParent(e.target.value)} placeholder="Observations from home..." />
              <Input label="Goals for Next Term" value={hpcGoals} onChange={(e) => setHpcGoals(e.target.value)} placeholder="Learner-set goals..." />
            </div>

            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => handleHpcSave(false)}>Save Draft</Button>
              <Button onClick={() => handleHpcSave(true)} className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4" /> Publish to Parent Portal
              </Button>
            </div>
          </div>

          {/* Preview pane */}
          <div className="lg:col-span-6 space-y-4">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-5">
              <div className="flex justify-between items-center border-b border-slate-50 pb-3">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <Eye className="h-4 w-4 text-blue-600" /> HPC Preview — 360° Card
                </h3>
                <Badge variant="info" className="text-[9px] font-black uppercase">Term 1 • 2026-27</Badge>
              </div>

              {hpcStudent ? (
                <div className="space-y-4">
                  <div className="p-4 bg-indigo-900 text-white rounded-2xl">
                    <span className="text-[9px] uppercase tracking-widest font-extrabold text-indigo-300">Holistic Progress Card</span>
                    <h4 className="text-lg font-black mt-1">{hpcStudent.name}</h4>
                    <p className="text-[10px] text-indigo-200 font-bold">{hpcStudent.grade} • Adm No: {hpcStudent.admissionNo}</p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {HPC_DOMAINS.map((d) => {
                      const lvl = hpcForm[d.key].level;
                      const variant = lvl === "Sky" ? "success" : lvl === "Mountain" ? "info" : lvl === "River" ? "warning" : "default";
                      return (
                        <div key={d.key} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-2">
                          <div className="flex justify-between items-center gap-2">
                            <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">{d.label}</span>
                            <Badge variant={variant} className="text-[9px] font-black uppercase shrink-0">{lvl}</Badge>
                          </div>
                          <p className="text-[11px] text-slate-600 font-semibold leading-relaxed">
                            {hpcForm[d.key].observation || <span className="italic text-slate-400">No observation recorded yet.</span>}
                          </p>
                        </div>
                      );
                    })}
                  </div>

                  <div className="p-4 bg-amber-50/60 border border-amber-100 rounded-2xl space-y-2">
                    <span className="text-[9px] uppercase tracking-widest font-extrabold text-amber-600">Learner Voice & Community</span>
                    <p className="text-[11px] text-slate-700 font-semibold"><span className="font-black">Self:</span> {hpcSelf || "—"}</p>
                    <p className="text-[11px] text-slate-700 font-semibold"><span className="font-black">Peer:</span> {hpcPeer || "—"}</p>
                    <p className="text-[11px] text-slate-700 font-semibold"><span className="font-black">Parent:</span> {hpcParent || "—"}</p>
                  </div>

                  <div className="p-4 bg-emerald-50/60 border border-emerald-100 rounded-2xl">
                    <span className="text-[9px] uppercase tracking-widest font-extrabold text-emerald-600">Strengths & Goals for Next Term</span>
                    <p className="text-[11px] text-slate-700 font-semibold mt-1.5">{hpcGoals || "Goals will appear here once recorded."}</p>
                  </div>
                </div>
              ) : (
                <div className="p-12 text-center text-slate-400 italic text-xs">Select a learner to preview their card.</div>
              )}
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest mb-3">Published Cards ({hpcRecords.length})</h3>
              {hpcRecords.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No holistic cards saved yet this term.</p>
              ) : (
                <div className="space-y-2">
                  {hpcRecords.slice(-6).reverse().map((r) => (
                    <div key={r.id} className="flex justify-between items-center p-3 bg-slate-50 border border-slate-100 rounded-xl">
                      <div>
                        <span className="text-xs font-extrabold text-slate-800">{r.studentName}</span>
                        <span className="text-[9px] text-slate-400 font-bold block">{r.grade} • {r.savedAt}</span>
                      </div>
                      <Badge variant={r.status === "Published" ? "success" : "default"} className="text-[9px] font-black uppercase">{r.status}</Badge>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ================= 5. DPDP ================= */}
      {activeTab === "dpdp" && (
        <div className="space-y-6">
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
              <Lock className="h-4 w-4 text-rose-600" /> DPDP Act 2023 — Verifiable Parental Consent Register
            </h3>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-4">Consent Purpose</th>
                    <th className="p-4 w-52">Coverage</th>
                    <th className="p-4">Granted</th>
                    <th className="p-4">Pending</th>
                    <th className="p-4">Withdrawn</th>
                    <th className="p-4">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {dpdp.consents.map((c) => {
                    const total = c.granted + c.pending + c.withdrawn;
                    const pct = total ? Math.round((c.granted / total) * 100) : 0;
                    return (
                      <tr key={c.id}>
                        <td className="p-4 font-extrabold text-slate-800">{c.type}</td>
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            <ProgressBar pct={pct} color={pct >= 80 ? "bg-emerald-500" : pct >= 55 ? "bg-amber-500" : "bg-rose-500"} />
                            <span className="text-[10px] font-mono font-bold text-slate-500 shrink-0">{pct}%</span>
                          </div>
                        </td>
                        <td className="p-4"><Badge variant="success" className="text-[9px] font-black">{c.granted}</Badge></td>
                        <td className="p-4"><Badge variant="warning" className="text-[9px] font-black">{c.pending}</Badge></td>
                        <td className="p-4"><Badge variant="danger" className="text-[9px] font-black">{c.withdrawn}</Badge></td>
                        <td className="p-4">
                          <div className="flex gap-2">
                            <Button size="sm" variant="outline" onClick={() => setDrillConsent(c)} className="text-[10px] flex items-center gap-1">
                              <Eye className="h-3 w-3" /> Drilldown
                            </Button>
                            <Button size="sm" variant="secondary" onClick={() => handleSendConsentRequest(c.type)} className="text-[10px] flex items-center gap-1">
                              <Send className="h-3 w-3" /> Send Request
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">
                Data Retention Policy (Purge Schedule)
              </h3>
              {dpdp.retention.map((r, idx) => (
                <div key={r.record} className="flex items-center justify-between gap-4 p-3 bg-slate-50 border border-slate-100 rounded-2xl">
                  <span className="text-xs font-extrabold text-slate-800">{r.record}</span>
                  <div className="w-36 shrink-0">
                    <Select
                      options={[
                        { label: "1 Year", value: "1" },
                        { label: "3 Years", value: "3" },
                        { label: "5 Years", value: "5" },
                        { label: "7 Years", value: "7" },
                        { label: "8 Years", value: "8" },
                        { label: "10 Years", value: "10" }
                      ]}
                      value={r.years}
                      onChange={(e) => handleRetentionChange(idx, e.target.value)}
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="bg-indigo-900 text-white rounded-3xl p-6 space-y-3 h-fit">
              <span className="text-[10px] uppercase tracking-widest font-extrabold text-indigo-300">DPDP Fiduciary Duties</span>
              <ul className="text-[11px] text-indigo-100 font-semibold space-y-2 leading-relaxed list-disc pl-4">
                <li>Children's data requires verifiable parental consent (Section 9) — no behavioural tracking or targeted ads.</li>
                <li>Consent notices must be available in English + scheduled languages (Hindi issued by default).</li>
                <li>Withdrawal of consent must be as easy as granting it; withdrawal halts related processing.</li>
                <li>Data breach notifications go to the Data Protection Board and affected parents without delay.</li>
              </ul>
            </div>
          </div>

          {/* Drilldown dialog */}
          <Dialog
            isOpen={!!drillConsent}
            onClose={() => setDrillConsent(null)}
            title={`Consent Drilldown — ${drillConsent ? drillConsent.type : ""}`}
            maxWidth="max-w-2xl"
          >
            {drillConsent && (
              <div className="space-y-3">
                <p className="text-[11px] text-slate-500 font-semibold">
                  Per-student consent state (sample of {Math.min(students.length, 10)} learners). Statuses are recorded with timestamp, channel and consent artefact ID.
                </p>
                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-3">Student</th>
                        <th className="p-3">Parent</th>
                        <th className="p-3">Status</th>
                        <th className="p-3">Channel</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {students.slice(0, 10).map((s, i) => {
                        const st = (hashNum(`${drillConsent.id}-${s.id}`) + i) % 5;
                        const status = st === 4 ? "Withdrawn" : st === 3 ? "Pending" : "Granted";
                        return (
                          <tr key={s.id}>
                            <td className="p-3 font-extrabold text-slate-800">{s.name}<span className="block text-[9px] text-slate-400">{s.grade}</span></td>
                            <td className="p-3 text-slate-500">{s.parentName}</td>
                            <td className="p-3">
                              <Badge variant={status === "Granted" ? "success" : status === "Pending" ? "warning" : "danger"} className="text-[9px] font-black uppercase">
                                {status}
                              </Badge>
                            </td>
                            <td className="p-3 text-[10px] text-slate-400 font-bold">{st % 2 === 0 ? "Parent App" : "OTP e-Sign"}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                <div className="flex justify-end pt-2 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setDrillConsent(null)}>Close</Button>
                </div>
              </div>
            )}
          </Dialog>
        </div>
      )}
    </div>
  );
}
