/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStaff, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast, Tabs, TabsList, TabsTrigger, TabsContent } from "../components/ui";
import {
  Wallet, FileText, Fingerprint, TrendingUp, ShieldCheck, Download, Mail,
  Users, Star, Clock, IndianRupee, Play, CheckCircle2, AlertTriangle, RefreshCw, Landmark
} from "lucide-react";

const inr = (n) => "₹" + Number(n || 0).toLocaleString("en-IN");
const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));
const hashCode = (str) => { let h = 0; for (let i = 0; i < String(str).length; i++) { h = (h * 31 + String(str).charCodeAt(i)) >>> 0; } return h; };
const today = () => new Date().toISOString().split("T")[0];
const ym = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
const currentMonth = () => ym(new Date());
const downloadFile = (filename, content, mime) => {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
};

// ---- Indian payroll math (mock but shaped like the real thing) ----
const computeSlip = (sf) => {
  const gross = Number(sf.salary) || 0;
  const basic = Math.round(gross * 0.5);
  const hra = Math.round(gross * 0.2);
  const da = Math.round(gross * 0.1);
  const special = gross - basic - hra - da;
  const pf = Math.min(Math.round(basic * 0.12), 1800);
  const esi = gross < 21000 ? Math.round(gross * 0.0075) : 0;
  const pt = 200;
  const annual = gross * 12;
  let tax = 0;
  if (annual > 300000) tax += Math.min(annual - 300000, 400000) * 0.05;
  if (annual > 700000) tax += Math.min(annual - 700000, 300000) * 0.10;
  if (annual > 1000000) tax += (annual - 1000000) * 0.20;
  const tds = Math.round(tax / 12);
  const deductions = pf + esi + pt + tds;
  return { staffDocsId: sf.id, name: sf.name, department: sf.department, role: sf.role, gross, basic, hra, da, special, pf, esi, pt, tds, deductions, net: gross - deductions };
};

const ONES = ["", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"];
const TENS = ["", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"];
const twoDigits = (n) => n < 20 ? ONES[n] : (TENS[Math.floor(n / 10)] + (n % 10 ? " " + ONES[n % 10] : ""));
const numToWordsINR = (num) => {
  let n = Math.round(Number(num) || 0);
  if (n === 0) return "Zero Rupees Only";
  let out = "";
  const crore = Math.floor(n / 1e7); n %= 1e7;
  const lakh = Math.floor(n / 1e5); n %= 1e5;
  const thousand = Math.floor(n / 1e3); n %= 1e3;
  const hundred = Math.floor(n / 100); n %= 100;
  if (crore) out += twoDigits(crore) + " Crore ";
  if (lakh) out += twoDigits(lakh) + " Lakh ";
  if (thousand) out += twoDigits(thousand) + " Thousand ";
  if (hundred) out += ONES[hundred] + " Hundred ";
  if (n) out += (out ? "and " : "") + twoDigits(n) + " ";
  return out.trim() + " Rupees Only";
};

const monthOptions = () => {
  const opts = [];
  const d = new Date();
  for (let i = 0; i < 4; i++) {
    const m = new Date(d.getFullYear(), d.getMonth() - i, 1);
    opts.push({ label: m.toLocaleString("en-IN", { month: "long", year: "numeric" }), value: ym(m) });
  }
  return opts;
};

// stable seeded punch times per staff
const punchFor = (sf) => {
  const h = hashCode(sf.id);
  const inMin = 8 * 60 + 10 + (h % 65); // 08:10 - 09:15
  const outMin = 16 * 60 + 30 + (h % 90); // 16:30 - 18:00
  const fmt = (m) => `${String(Math.floor(m / 60)).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;
  return { inTime: fmt(inMin), outTime: fmt(outMin), late: inMin > 8 * 60 + 45 };
};
const monthlyPAL = (sf) => {
  const h = hashCode(sf.id + "pal");
  const a = h % 3;
  const l = (h >> 3) % 3;
  return { P: 24 - a - l, A: a, L: l };
};

const INCR_OPTIONS = [
  { label: "0% — Hold", value: "0" },
  { label: "5% — Standard", value: "5" },
  { label: "8% — Exceeds expectations", value: "8" },
  { label: "12% — Outstanding", value: "12" }
];

const CHALLANS = [
  { id: "ch-pf", name: "PF Challan (ECR)", due: "15th of month", day: 15, authority: "EPFO Unified Portal" },
  { id: "ch-esi", name: "ESI Contribution", due: "15th of month", day: 15, authority: "ESIC Portal" },
  { id: "ch-tds", name: "TDS Deposit (24Q)", due: "7th of month", day: 7, authority: "TIN / TRACES" }
];

export default function ModPayroll({ user }) {
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState("dashboard");
  const staff = getStaff();
  const [payslips, setPayslips] = useState(() => loadLS("erp_payslips", {}));
  const [increments, setIncrements] = useState(() => loadLS("erp_increments", {}));
  const [statLog, setStatLog] = useState(() => loadLS("erp_statutory_log", []));

  const [runProgress, setRunProgress] = useState(null);
  const [slipMonth, setSlipMonth] = useState(currentMonth());
  const [activeSlip, setActiveSlip] = useState(null);
  const [syncing, setSyncing] = useState(false);
  const [extraPunches, setExtraPunches] = useState(0);
  const [drafts, setDrafts] = useState({});
  const [applying, setApplying] = useState(false);
  const [form16Staff, setForm16Staff] = useState(staff[0]?.id || "");
  const [ptState, setPtState] = useState("Maharashtra");

  const canApprove = user.role === "Principal" || user.role === "Super Admin";
  const slips = staff.map(computeSlip);
  const totalGross = slips.reduce((s, x) => s + x.gross, 0);
  const pfEmployer = slips.reduce((s, x) => s + Math.min(Math.round(x.basic * 0.12), 1800), 0);
  const esiEmployer = slips.reduce((s, x) => s + (x.gross < 21000 ? Math.round(x.gross * 0.0325) : 0), 0);
  const totalTDS = slips.reduce((s, x) => s + x.tds, 0);
  const monthRun = !!payslips[currentMonth()];
  const slipsGenerated = Object.values(payslips).reduce((s, arr) => s + arr.length, 0);

  // ================= DASHBOARD: run payroll =================
  const handleRunPayroll = () => {
    if (runProgress !== null) return;
    if (monthRun) { addToast("Already Processed", `Payroll for ${currentMonth()} is locked. Re-runs need a supplementary cycle.`, "info"); return; }
    setRunProgress(0);
    let pct = 0;
    const timer = setInterval(() => {
      pct += 10;
      if (pct >= 100) {
        clearInterval(timer);
        setRunProgress(100);
        const records = staff.map((sf) => ({ ...computeSlip(sf), month: currentMonth(), status: "Generated", generatedOn: today() }));
        const next = { ...payslips, [currentMonth()]: records };
        setPayslips(next);
        saveLS("erp_payslips", next);
        logAction(user.id, user.name, user.role, "Payroll Run Executed", `Processed ${records.length} payslips for ${currentMonth()}, gross ${inr(totalGross)}, TDS ${inr(totalTDS)}`);
        addToast("Payroll Processed", `${records.length} payslips generated for ${currentMonth()}`);
        setTimeout(() => setRunProgress(null), 900);
      } else {
        setRunProgress(pct);
      }
    }, 200);
  };

  // ================= BIOMETRIC =================
  const handleSyncDevice = () => {
    if (syncing) return;
    setSyncing(true);
    setTimeout(() => {
      const added = 2 + Math.floor(Math.random() * 3);
      setExtraPunches((p) => p + added);
      setSyncing(false);
      logAction(user.id, user.name, user.role, "Biometric Sync", `Pulled ${added} fresh punches from eSSL-Gate-01 controller`);
      addToast("Device Synced", `${added} new punches ingested from eSSL-Gate-01`);
    }, 1500);
  };

  // ================= INCREMENTS =================
  const setDraftPct = (staffDocsId, pct) => setDrafts((prev) => ({ ...prev, [staffDocsId]: pct }));
  const pctFor = (sf) => drafts[sf.id] ?? increments[sf.id]?.pct ?? (sf.reviewRating >= 4.8 ? "12" : sf.reviewRating >= 4.5 ? "8" : sf.reviewRating >= 4.0 ? "5" : "0");
  const statusFor = (sf) => increments[sf.id]?.status || "Draft";

  const approveIncrement = (sf) => {
    const pct = pctFor(sf);
    const newSalary = Math.round(sf.salary * (1 + Number(pct) / 100));
    const next = { ...increments, [sf.id]: { pct, newSalary, status: "Approved", effectiveDate: "" } };
    setIncrements(next);
    saveLS("erp_increments", next);
    logAction(user.id, user.name, user.role, "Increment Approved", `${sf.name}: +${pct}% → ${inr(newSalary)} (rating ${sf.reviewRating})`);
    addToast("Increment Approved", `${sf.name} moves to ${inr(newSalary)} on next cycle`);
  };

  const handleApplyIncrements = () => {
    if (applying) return;
    const approved = staff.filter((sf) => increments[sf.id]?.status === "Approved" && !increments[sf.id]?.effectiveDate);
    if (approved.length === 0) { addToast("Nothing To Apply", "No approved increments pending an effective date", "info"); return; }
    setApplying(true);
    setTimeout(() => {
      const eff = "2026-08-01";
      const next = { ...increments };
      approved.forEach((sf) => { next[sf.id] = { ...next[sf.id], effectiveDate: eff, appliedByName: user.name }; });
      setIncrements(next);
      saveLS("erp_increments", next);
      setApplying(false);
      logAction(user.id, user.name, user.role, "Increments Applied", `${approved.length} appraisal increments stamped effective ${eff} (payroll master untouched — mock ledger)`);
      addToast("Increments Scheduled", `${approved.length} letters queued, effective ${eff}`);
    }, 1500);
  };

  // ================= STATUTORY =================
  const logStatutory = (report, filename) => {
    const next = [{ id: `st-${Date.now()}`, report, filename, date: today(), by: user.name }, ...statLog].slice(0, 12);
    setStatLog(next);
    saveLS("erp_statutory_log", next);
  };

  const downloadECR = () => {
    let csv = "UAN,Member Name,Gross Wages,EPF Wages,EPS Wages,EE Share,ER Share\n";
    slips.forEach((s, i) => {
      const pf = Math.min(Math.round(s.basic * 0.12), 1800);
      csv += `10011${String(2200 + i)},${s.name},${s.gross},${s.basic},${Math.min(s.basic, 15000)},${pf},${pf}\n`;
    });
    const fname = `pf-ecr-${currentMonth()}.csv`;
    downloadFile(fname, csv, "text/csv");
    logStatutory("PF ECR", fname);
    logAction(user.id, user.name, user.role, "PF ECR Generated", `ECR text for ${slips.length} members, month ${currentMonth()}`);
    addToast("PF ECR Downloaded", "Upload to EPFO unified portal before the 15th");
  };

  const downloadESI = () => {
    const eligible = slips.filter((s) => s.gross < 21000);
    let csv = "IP Number,Name,Days,Wages,IP Contribution (0.75%),Employer (3.25%)\n";
    eligible.forEach((s, i) => {
      csv += `31000${String(500 + i)},${s.name},26,${s.gross},${s.esi},${Math.round(s.gross * 0.0325)}\n`;
    });
    const fname = `esi-return-${currentMonth()}.csv`;
    downloadFile(fname, csv, "text/csv");
    logStatutory("ESI Return", fname);
    logAction(user.id, user.name, user.role, "ESI Return Generated", `${eligible.length} insured persons, month ${currentMonth()}`);
    addToast("ESI Return Downloaded", `${eligible.length} eligible IPs included (gross < ₹21,000)`);
  };

  const downloadPT = () => {
    let csv = `State,${ptState}\nSlab,Employees,PT per head,Total\n`;
    csv += `Above ₹10000,${slips.length},200,${slips.length * 200}\n`;
    const fname = `pt-return-${ptState.toLowerCase()}-${currentMonth()}.csv`;
    downloadFile(fname, csv, "text/csv");
    logStatutory("Professional Tax", fname);
    logAction(user.id, user.name, user.role, "PT Return Generated", `Professional tax summary for ${ptState}, ${slips.length} employees`);
    addToast("PT Return Downloaded", `${ptState} slab computation ready`);
  };

  const generateForm16 = () => {
    const sf = staff.find((s) => s.id === form16Staff);
    if (!sf) return;
    logStatutory("Form 16", `form16-${sf.name.replace(/\s+/g, "-").toLowerCase()}.pdf`);
    logAction(user.id, user.name, user.role, "Form 16 Generated", `Part A + Part B for ${sf.name}, FY 2025-26`);
    addToast("Form 16 Queued", `TRACES Part-A merge started for ${sf.name} (mock)`);
  };

  const monthSlips = payslips[slipMonth] || [];
  const dayOfMonth = new Date().getDate();

  return <div className="space-y-6">

      {/* Header banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <Wallet className="h-5 w-5 text-emerald-700" /> Payroll &amp; Statutory (India)
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Salary runs, payslips, biometric attendance, appraisal increments and PF / ESI / TDS compliance.</p>
        </div>
        <Badge variant="success">FY 2026-27 · PF Code MH/12345</Badge>
      </div>

      <Tabs activeTab={activeTab} onChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="dashboard">Dashboard</TabsTrigger>
          <TabsTrigger value="payslips">Payslips</TabsTrigger>
          <TabsTrigger value="biometric">Biometric</TabsTrigger>
          <TabsTrigger value="increments">Increments</TabsTrigger>
          <TabsTrigger value="statutory">Statutory</TabsTrigger>
        </TabsList>

        {/* ================= TAB 1: DASHBOARD ================= */}
        <TabsContent value="dashboard" activeTab={activeTab}>
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-5">
              <div className="bg-emerald-950 text-white p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-emerald-300 tracking-widest">Monthly Gross</span>
                <h4 className="text-xl font-black text-white mt-1">{inr(totalGross)}</h4>
                <p className="text-xs text-emerald-200 mt-1 font-semibold">{staff.length} employees on rolls</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">PF Employer (12%)</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(pfEmployer)}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Capped ₹1,800 / member</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">ESI Employer (3.25%)</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(esiEmployer)}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Gross &lt; ₹21,000 eligible</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">TDS Deducted</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(totalTDS)}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Sec 192 · monthly average</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">Payslips Generated</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{slipsGenerated}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Across all processed months</p>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
                <div>
                  <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <Play className="h-4 w-4 text-emerald-600" /> Payroll Run — {monthOptions()[0].label}
                  </h3>
                  <p className="text-xs text-slate-400 font-semibold mt-1">
                    Sequence: lock attendance → compute earnings &amp; LOP → statutory deductions → bank advice (NEFT batch) → payslips.
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <Badge variant={monthRun ? "success" : "warning"}>{monthRun ? "Processed & Locked" : "Pending Run"}</Badge>
                  <Button onClick={handleRunPayroll} disabled={runProgress !== null || monthRun} className="flex items-center gap-1.5">
                    <Play className="h-4 w-4" /> {runProgress !== null ? `Processing… ${runProgress}%` : "Run Payroll"}
                  </Button>
                </div>
              </div>
              {runProgress !== null && <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div className="h-full bg-emerald-500 rounded-full transition-all duration-200" style={{ width: `${runProgress}%` }} />
                </div>}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 pt-2 border-t border-slate-100">
                {["Attendance Lock", "Earnings & LOP", "Statutory Deductions", "Bank NEFT Advice"].map((step, i) => <div key={step} className={`p-3 rounded-2xl border text-xs font-bold flex items-center gap-2 ${monthRun ? "bg-emerald-50 border-emerald-200 text-emerald-800" : "bg-slate-50 border-slate-150 text-slate-400"}`}>
                    {monthRun ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <Clock className="h-4 w-4" />}
                    {i + 1}. {step}
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 2: PAYSLIPS ================= */}
        <TabsContent value="payslips" activeTab={activeTab}>
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-3">
              <div>
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <FileText className="h-4 w-4 text-indigo-600" /> Payslip Register
                </h3>
                <p className="text-xs text-slate-400 font-semibold mt-1">Click a row to open the detailed payslip with earnings, deductions and net-in-words.</p>
              </div>
              <div className="w-full sm:w-56">
                <Select label="Payroll Month" options={monthOptions()} value={slipMonth} onChange={(e) => setSlipMonth(e.target.value)} />
              </div>
            </div>
            {monthSlips.length === 0 ? <div className="p-8 text-center bg-slate-50 border border-slate-150 rounded-2xl">
                <p className="text-xs font-bold text-slate-500">No payroll run found for this month.</p>
                <p className="text-[10px] text-slate-400 font-semibold mt-1">Execute "Run Payroll" on the Dashboard tab to generate payslips.</p>
              </div> : <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-4">Employee</th>
                      <th className="p-4">Department</th>
                      <th className="p-4">Gross</th>
                      <th className="p-4">Deductions</th>
                      <th className="p-4">Net Pay</th>
                      <th className="p-4">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {monthSlips.map((s) => <tr key={s.staffDocsId} onClick={() => setActiveSlip(s)} className="cursor-pointer hover:bg-slate-50 transition">
                        <td className="p-4 font-extrabold text-slate-800">{s.name}</td>
                        <td className="p-4 text-slate-500">{s.department}</td>
                        <td className="p-4">{inr(s.gross)}</td>
                        <td className="p-4 text-rose-600">− {inr(s.deductions)}</td>
                        <td className="p-4 font-black text-emerald-700">{inr(s.net)}</td>
                        <td className="p-4"><Badge variant="success">{s.status}</Badge></td>
                      </tr>)}
                  </tbody>
                </table>
              </div>}
          </div>
        </TabsContent>

        {/* ================= TAB 3: BIOMETRIC ================= */}
        <TabsContent value="biometric" activeTab={activeTab}>
          <div className="space-y-6">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
                <div>
                  <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <Fingerprint className="h-4 w-4 text-indigo-600" /> Today's Punch Log — {today()}
                  </h3>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Controller eSSL-Gate-01 · shift 08:30 – 16:30 · grace till 08:45.{extraPunches > 0 && ` ${extraPunches} punches added by last sync.`}</p>
                </div>
                <Button variant="outline" size="sm" onClick={handleSyncDevice} disabled={syncing} className="flex items-center gap-1.5">
                  <RefreshCw className={`h-3.5 w-3.5 ${syncing ? "animate-spin" : ""}`} /> {syncing ? "Syncing…" : "Sync Device"}
                </Button>
              </div>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-4">Employee</th>
                      <th className="p-4">Department</th>
                      <th className="p-4">Punch In</th>
                      <th className="p-4">Punch Out</th>
                      <th className="p-4">Flag</th>
                      <th className="p-4">Device</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {staff.map((sf) => {
                      const p = punchFor(sf);
                      return <tr key={sf.id}>
                          <td className="p-4 font-extrabold text-slate-800">{sf.name}</td>
                          <td className="p-4 text-slate-500">{sf.department}</td>
                          <td className="p-4 font-mono">{p.inTime}</td>
                          <td className="p-4 font-mono">{p.outTime}</td>
                          <td className="p-4">{p.late ? <Badge variant="warning">Late</Badge> : <Badge variant="success">On Time</Badge>}</td>
                          <td className="p-4 text-slate-400 font-bold">eSSL-Gate-01</td>
                        </tr>;
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Users className="h-4 w-4 text-slate-500" /> Monthly Summary (P / A / L)
              </h3>
              <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-3">
                {staff.map((sf) => {
                  const pal = monthlyPAL(sf);
                  return <div key={sf.id} className="p-3.5 bg-slate-50 border border-slate-150 rounded-2xl">
                      <p className="text-xs font-black text-slate-800 truncate">{sf.name}</p>
                      <div className="flex gap-2 mt-2 text-[10px] font-extrabold">
                        <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-2 py-0.5 rounded-lg">P {pal.P}</span>
                        <span className="bg-rose-50 border border-rose-200 text-rose-700 px-2 py-0.5 rounded-lg">A {pal.A}</span>
                        <span className="bg-amber-50 border border-amber-200 text-amber-700 px-2 py-0.5 rounded-lg">L {pal.L}</span>
                      </div>
                    </div>;
                })}
              </div>
              <p className="text-[10px] text-slate-400 italic leading-normal">Three late marks convert to a half-day LOP — late marks auto-reflect in payroll LOP during the monthly run.</p>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 4: INCREMENTS ================= */}
        <TabsContent value="increments" activeTab={activeTab}>
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
              <div>
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <TrendingUp className="h-4 w-4 text-emerald-600" /> Appraisal-Linked Increments
                </h3>
                <p className="text-xs text-slate-400 font-semibold mt-1">
                  Suggested band from review rating. {canApprove ? "You can approve proposals." : "Approval requires Principal / Super Admin."} Applying writes an increment ledger, not the live salary master.
                </p>
              </div>
              <Button onClick={handleApplyIncrements} disabled={applying} className="flex items-center gap-1.5">
                <CheckCircle2 className="h-4 w-4" /> {applying ? "Stamping letters…" : "Apply Increments"}
              </Button>
            </div>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-left text-slate-700">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                  <tr>
                    <th className="p-4">Employee</th>
                    <th className="p-4">Current Salary</th>
                    <th className="p-4">Rating</th>
                    <th className="p-4">Proposed %</th>
                    <th className="p-4">New Salary</th>
                    <th className="p-4">Status</th>
                    <th className="p-4 text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {staff.map((sf) => {
                    const pct = pctFor(sf);
                    const status = statusFor(sf);
                    const eff = increments[sf.id]?.effectiveDate;
                    const newSalary = Math.round(sf.salary * (1 + Number(pct) / 100));
                    return <tr key={sf.id}>
                        <td className="p-4">
                          <p className="font-extrabold text-slate-800">{sf.name}</p>
                          <p className="text-[10px] text-slate-400 font-bold mt-0.5">{sf.department}</p>
                        </td>
                        <td className="p-4">{inr(sf.salary)}</td>
                        <td className="p-4">
                          <span className="inline-flex items-center gap-1 font-black text-amber-600">
                            <Star className="h-3.5 w-3.5 fill-amber-500 text-amber-500" /> {sf.reviewRating}
                          </span>
                        </td>
                        <td className="p-4">
                          <select
                            value={pct}
                            disabled={status === "Approved"}
                            onChange={(e) => setDraftPct(sf.id, e.target.value)}
                            className="bg-slate-50 border border-slate-200 rounded-lg px-2 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            {INCR_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                          </select>
                        </td>
                        <td className="p-4 font-black text-emerald-700">{inr(newSalary)}</td>
                        <td className="p-4">
                          <Badge variant={eff ? "info" : status === "Approved" ? "success" : "default"}>{eff ? `Effective ${eff}` : status}</Badge>
                        </td>
                        <td className="p-4 text-center">
                          {status !== "Approved" && (canApprove ? <button onClick={() => approveIncrement(sf)} className="bg-emerald-600 text-white hover:bg-emerald-700 text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer">
                                Approve
                              </button> : <Badge variant="warning">Principal only</Badge>)}
                        </td>
                      </tr>;
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 5: STATUTORY ================= */}
        <TabsContent value="statutory" activeTab={activeTab}>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4 text-indigo-600" /> PF ECR File
                  </h4>
                  <p className="text-xs text-slate-400 font-semibold leading-relaxed">Electronic Challan-cum-Return for {slips.length} UAN members. Employer + employee share at 12% of EPF wages.</p>
                  <Button size="sm" variant="outline" onClick={downloadECR} className="flex items-center gap-1.5">
                    <Download className="h-3.5 w-3.5" /> Download ECR CSV
                  </Button>
                </div>
                <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4 text-emerald-600" /> ESI Return
                  </h4>
                  <p className="text-xs text-slate-400 font-semibold leading-relaxed">Monthly contribution for insured persons under ₹21,000 gross. IP 0.75% + employer 3.25%.</p>
                  <Button size="sm" variant="outline" onClick={downloadESI} className="flex items-center gap-1.5">
                    <Download className="h-3.5 w-3.5" /> Download ESI CSV
                  </Button>
                </div>
                <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <FileText className="h-4 w-4 text-slate-600" /> Form 16 (FY 2025-26)
                  </h4>
                  <Select
                    options={staff.map((s) => ({ label: `${s.name} — ${s.department}`, value: s.id }))}
                    value={form16Staff}
                    onChange={(e) => setForm16Staff(e.target.value)}
                  />
                  <Button size="sm" variant="outline" onClick={generateForm16} className="flex items-center gap-1.5">
                    <Mail className="h-3.5 w-3.5" /> Generate &amp; Queue
                  </Button>
                </div>
                <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <Landmark className="h-4 w-4 text-amber-600" /> Professional Tax
                  </h4>
                  <Select
                    options={["Maharashtra", "Karnataka", "West Bengal", "Telangana", "Gujarat"].map((s) => ({ label: s, value: s }))}
                    value={ptState}
                    onChange={(e) => setPtState(e.target.value)}
                  />
                  <Button size="sm" variant="outline" onClick={downloadPT} className="flex items-center gap-1.5">
                    <Download className="h-3.5 w-3.5" /> Download PT CSV
                  </Button>
                </div>
              </div>

              <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-3">
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" /> Challan Due-Date Reminders
                </h3>
                <div className="space-y-2.5">
                  {CHALLANS.map((c) => {
                    const overdue = dayOfMonth > c.day;
                    const dueSoon = !overdue && c.day - dayOfMonth <= 4;
                    return <div key={c.id} className="flex justify-between items-center p-3.5 bg-slate-50 border border-slate-150 rounded-2xl">
                        <div>
                          <p className="text-xs font-black text-slate-800">{c.name}</p>
                          <p className="text-[10px] text-slate-400 font-bold mt-0.5">Due {c.due} · {c.authority}</p>
                        </div>
                        <Badge variant={overdue ? "danger" : dueSoon ? "warning" : "success"}>
                          {overdue ? "Deposit overdue" : dueSoon ? `Due in ${c.day - dayOfMonth}d` : "On track"}
                        </Badge>
                      </div>;
                  })}
                </div>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Clock className="h-4 w-4 text-slate-500" /> Filing Activity
              </h3>
              {statLog.length === 0 && <p className="text-xs text-slate-400 font-semibold italic">No statutory files generated yet this session.</p>}
              <div className="space-y-3">
                {statLog.map((s) => <div key={s.id} className="p-3.5 bg-slate-50 border border-slate-150 rounded-2xl">
                    <p className="text-xs font-black text-slate-800">{s.report}</p>
                    <p className="text-[10px] text-slate-400 font-bold mt-1 break-all">{s.filename}</p>
                    <p className="text-[10px] text-slate-400 font-semibold mt-0.5">{s.date} · by {s.by}</p>
                  </div>)}
              </div>
              <div className="bg-indigo-50 border border-indigo-100 p-4 rounded-2xl">
                <p className="text-[10px] text-indigo-950 font-semibold leading-relaxed">
                  <IndianRupee className="h-3 w-3 inline mr-1" />
                  Mock statutory math: PF capped at ₹1,800, ESI at 0.75% / 3.25% below ₹21,000 gross, PT flat ₹200 and new-regime TDS slabs averaged monthly.
                </p>
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>

      {/* ================= DIALOG: Payslip ================= */}
      <Dialog isOpen={!!activeSlip} onClose={() => setActiveSlip(null)} title="Salary Slip" maxWidth="max-w-2xl">
        {activeSlip && <div className="space-y-5 pt-1 select-text">
            <div className="text-center border-b-2 border-dashed border-slate-200 pb-4">
              <h4 className="text-base font-black text-slate-800 uppercase tracking-widest">St. Jude Boarding Academy</h4>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 mt-1 font-bold">Payslip for {slipMonth} · PF Code MH/12345 · TAN MUMS12345A</p>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs font-semibold text-slate-700 bg-slate-50 border border-slate-150 rounded-2xl p-4">
              <p><span className="text-slate-400">Employee:</span> <span className="font-extrabold text-slate-900">{activeSlip.name}</span></p>
              <p><span className="text-slate-400">Department:</span> {activeSlip.department}</p>
              <p><span className="text-slate-400">Designation:</span> {activeSlip.role}</p>
              <p><span className="text-slate-400">Pay Days:</span> 26 / 26</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="border border-slate-100 rounded-2xl overflow-hidden">
                <div className="bg-slate-50 p-2.5 text-[9px] uppercase tracking-widest font-extrabold text-slate-400 border-b border-slate-100">Earnings</div>
                <div className="p-3.5 space-y-2 text-xs font-semibold text-slate-700">
                  <div className="flex justify-between"><span>Basic (50%)</span><span>{inr(activeSlip.basic)}</span></div>
                  <div className="flex justify-between"><span>HRA (20%)</span><span>{inr(activeSlip.hra)}</span></div>
                  <div className="flex justify-between"><span>DA (10%)</span><span>{inr(activeSlip.da)}</span></div>
                  <div className="flex justify-between"><span>Special Allowance</span><span>{inr(activeSlip.special)}</span></div>
                  <div className="flex justify-between border-t border-slate-100 pt-2 font-black text-slate-900"><span>Gross</span><span>{inr(activeSlip.gross)}</span></div>
                </div>
              </div>
              <div className="border border-slate-100 rounded-2xl overflow-hidden">
                <div className="bg-slate-50 p-2.5 text-[9px] uppercase tracking-widest font-extrabold text-slate-400 border-b border-slate-100">Deductions</div>
                <div className="p-3.5 space-y-2 text-xs font-semibold text-slate-700">
                  <div className="flex justify-between"><span>PF (12% of basic, cap ₹1,800)</span><span>{inr(activeSlip.pf)}</span></div>
                  <div className="flex justify-between"><span>ESI (0.75%)</span><span>{inr(activeSlip.esi)}</span></div>
                  <div className="flex justify-between"><span>Professional Tax</span><span>{inr(activeSlip.pt)}</span></div>
                  <div className="flex justify-between"><span>TDS (Sec 192)</span><span>{inr(activeSlip.tds)}</span></div>
                  <div className="flex justify-between border-t border-slate-100 pt-2 font-black text-rose-600"><span>Total</span><span>{inr(activeSlip.deductions)}</span></div>
                </div>
              </div>
            </div>

            <div className="bg-emerald-950 text-white rounded-2xl p-4 flex flex-col sm:flex-row justify-between sm:items-center gap-2">
              <div>
                <p className="text-[9px] uppercase tracking-widest font-extrabold text-emerald-300">Net Pay</p>
                <p className="text-xl font-black">{inr(activeSlip.net)}</p>
              </div>
              <p className="text-[10px] font-bold text-emerald-200 sm:max-w-[55%] sm:text-right italic">{numToWordsINR(activeSlip.net)}</p>
            </div>

            <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" size="sm" onClick={() => addToast("PDF Generated", `Payslip PDF for ${activeSlip.name} rendered (mock stream)`)} className="flex items-center gap-1.5">
                <Download className="h-3.5 w-3.5" /> Download PDF
              </Button>
              <Button variant="outline" size="sm" onClick={() => addToast("Email Queued", `Payslip mailed to ${activeSlip.name.split(" ")[0].toLowerCase()}@stjude.edu`, "info")} className="flex items-center gap-1.5">
                <Mail className="h-3.5 w-3.5" /> Email Payslip
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setActiveSlip(null)}>Close</Button>
            </div>
          </div>}
      </Dialog>

    </div>;
}
