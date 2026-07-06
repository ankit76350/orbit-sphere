/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useMemo } from "react";
import {
  getStudents,
  getStaff,
  getFees,
  getAttendance,
  getInventory,
  getVisitors,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, Tabs, TabsList, TabsTrigger, TabsContent, useToast } from "../components/ui";
import {
  BarChart3,
  IndianRupee,
  Users,
  Download,
  Save,
  Play,
  Trash2,
  FileText,
  CalendarClock,
  Printer,
  Filter,
  Plus,
  Layers,
  TrendingUp,
  Bed
} from "lucide-react";

const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));
const inr = (n) => "₹" + Number(n || 0).toLocaleString("en-IN");

const SAVED_KEY = "erp_saved_reports";
const SCHED_KEY = "erp_scheduled_reports";

const SOURCES = {
  students: {
    label: "Students",
    fields: ["admissionNumber", "name", "grade", "gender", "dob", "parentName", "parentPhone", "walletBalance", "hostelOptIn", "transportOptIn", "status"],
    filterFields: ["grade", "status", "gender"],
    load: () => getStudents()
  },
  fees: {
    label: "Fee Invoices",
    fields: ["id", "studentName", "grade", "feeType", "amount", "paidAmount", "dueDate", "status"],
    filterFields: ["status"],
    load: () => getFees()
  },
  staff: {
    label: "Staff",
    fields: ["id", "name", "role", "department", "status"],
    filterFields: ["department", "role"],
    load: () => getStaff()
  }
};

const SCHED_SEED = [
  { id: "sch-1", reportName: "Daily Fee Collection Summary", frequency: "Daily", recipients: "principal@stjosephs.edu.in", channel: "Email", lastRun: "2026-07-04 07:00", status: "Active" },
  { id: "sch-2", reportName: "Weekly Fee Defaulter List", frequency: "Weekly", recipients: "accounts@stjosephs.edu.in", channel: "Email", lastRun: "2026-06-29 08:00", status: "Active" },
  { id: "sch-3", reportName: "Monthly Enrollment by Grade", frequency: "Monthly", recipients: "+91 98450 12233", channel: "WhatsApp", lastRun: "2026-06-01 09:00", status: "Paused" }
];

export default function ModReports({ user }) {
  const { addToast } = useToast();
  const [vTab, setVTab] = useState("dashboard");

  /* ---------- shared real data ---------- */
  const students = useMemo(() => getStudents(), []);
  const fees = useMemo(() => getFees(), []);
  const attendance = useMemo(() => getAttendance(), []);

  /* ---------- dashboard aggregates ---------- */
  const totalInvoiced = fees.reduce((s, f) => s + (f.amount || 0), 0);
  const totalCollected = fees.reduce((s, f) => s + (f.paidAmount || 0), 0);
  const totalOutstanding = totalInvoiced - totalCollected;
  const collectedPct = totalInvoiced > 0 ? Math.round(totalCollected / totalInvoiced * 100) : 0;

  const gradeGroups = useMemo(() => {
    const map = {};
    students.forEach((s) => { map[s.grade] = (map[s.grade] || 0) + 1; });
    return Object.entries(map).sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }));
  }, [students]);
  const maxGradeCount = Math.max(1, ...gradeGroups.map(([, c]) => c));

  const maleCount = students.filter((s) => s.gender === "Male").length;
  const femaleCount = students.length - maleCount;
  const malePct = students.length > 0 ? Math.round(maleCount / students.length * 100) : 0;

  const hostelCount = students.filter((s) => s.hostelOptIn).length;
  const dayCount = students.length - hostelCount;
  const hostelPct = students.length > 0 ? Math.round(hostelCount / students.length * 100) : 0;

  const attTrend = useMemo(() => {
    const byDate = {};
    attendance.forEach((a) => {
      if (!byDate[a.date]) byDate[a.date] = { total: 0, present: 0 };
      byDate[a.date].total += 1;
      if (a.status === "Present") byDate[a.date].present += 1;
    });
    const real = Object.entries(byDate).sort((a, b) => a[0].localeCompare(b[0])).map(([date, v]) => ({
      date,
      pct: v.total > 0 ? Math.round(v.present / v.total * 100) : 0
    }));
    const mock = [{ date: "2026-05-23", pct: 91 }, { date: "2026-05-24", pct: 88 }];
    return [...mock, ...real].slice(-7);
  }, [attendance]);

  /* ---------- builder state ---------- */
  const [step, setStep] = useState(1);
  const [source, setSource] = useState("students");
  const [filters, setFilters] = useState([{ field: "grade", op: "equals", value: "" }]);
  const [columns, setColumns] = useState(SOURCES.students.fields.slice(0, 5));
  const [previewRows, setPreviewRows] = useState(null);
  const [totalMatched, setTotalMatched] = useState(0);
  const [reportName, setReportName] = useState("");
  const [savedReports, setSavedReports] = useState(() => loadLS(SAVED_KEY, []));

  const changeSource = (val) => {
    setSource(val);
    setFilters([{ field: SOURCES[val].filterFields[0], op: "equals", value: "" }]);
    setColumns(SOURCES[val].fields.slice(0, 5));
    setPreviewRows(null);
    setStep(1);
  };

  const toggleColumn = (field) => {
    setColumns((prev) => prev.includes(field) ? prev.filter((c) => c !== field) : [...prev, field]);
  };

  const updateFilter = (idx, patch) => {
    setFilters((prev) => prev.map((f, i) => i === idx ? { ...f, ...patch } : f));
  };

  const runQuery = (src, flt) => {
    let rows = SOURCES[src].load();
    flt.forEach((f) => {
      if (!f.value) return;
      const needle = String(f.value).toLowerCase();
      rows = rows.filter((r) => {
        const hay = String(r[f.field] ?? "").toLowerCase();
        return f.op === "contains" ? hay.includes(needle) : hay === needle;
      });
    });
    return rows;
  };

  const handleGenerate = () => {
    if (columns.length === 0) {
      addToast("No Columns Chosen", "Pick at least one output column chip before previewing.", "warning");
      return;
    }
    const rows = runQuery(source, filters);
    setTotalMatched(rows.length);
    setPreviewRows(rows.slice(0, 50));
    setStep(4);
    logAction(user.id, user.name, user.role, "Report Preview Generated", `Custom report on source "${SOURCES[source].label}" matched ${rows.length} rows (${filters.filter((f) => f.value).length} filters, ${columns.length} columns)`);
    addToast("Preview Ready", `Query matched ${rows.length} record(s) from ${SOURCES[source].label}.`, "success");
  };

  const handleDownloadCSV = () => {
    if (!previewRows) {
      addToast("Nothing to Download", "Generate a preview first.", "warning");
      return;
    }
    const allRows = runQuery(source, filters);
    const esc = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
    const csv = [columns.map(esc).join(","), ...allRows.map((r) => columns.map((c) => esc(r[c])).join(","))].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${(reportName || SOURCES[source].label).replace(/\s+/g, "_").toLowerCase()}_report.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    logAction(user.id, user.name, user.role, "Report CSV Downloaded", `Exported ${allRows.length} rows / ${columns.length} columns from ${SOURCES[source].label}`);
    addToast("CSV Exported", `${allRows.length} rows written to CSV file.`, "success");
  };

  const handleSaveReport = () => {
    if (!reportName.trim()) {
      addToast("Name Required", "Give the report definition a name before saving.", "error");
      return;
    }
    const def = {
      id: `rpt-${Date.now()}`,
      name: reportName.trim(),
      source,
      filters: filters.filter((f) => f.value),
      columns,
      createdAt: new Date().toISOString().slice(0, 10)
    };
    const updated = [def, ...savedReports];
    setSavedReports(updated);
    saveLS(SAVED_KEY, updated);
    logAction(user.id, user.name, user.role, "Report Definition Saved", `Saved custom report "${def.name}" (source: ${SOURCES[source].label})`);
    addToast("Report Saved", `"${def.name}" added to the saved reports library.`, "success");
    setReportName("");
  };

  const handleRunSaved = (def) => {
    setSource(def.source);
    const flt = def.filters.length > 0 ? def.filters : [{ field: SOURCES[def.source].filterFields[0], op: "equals", value: "" }];
    setFilters(flt);
    setColumns(def.columns);
    const rows = runQuery(def.source, def.filters);
    setTotalMatched(rows.length);
    setPreviewRows(rows.slice(0, 50));
    setStep(4);
    logAction(user.id, user.name, user.role, "Saved Report Executed", `Ran saved report "${def.name}" — ${rows.length} rows matched`);
    addToast("Report Executed", `"${def.name}" returned ${rows.length} record(s).`, "success");
  };

  const handleDeleteSaved = (id) => {
    const target = savedReports.find((r) => r.id === id);
    const updated = savedReports.filter((r) => r.id !== id);
    setSavedReports(updated);
    saveLS(SAVED_KEY, updated);
    addToast("Definition Removed", `Deleted saved report "${target?.name}".`, "info");
  };

  /* ---------- scheduled state ---------- */
  const [schedules, setSchedules] = useState(() => loadLS(SCHED_KEY, SCHED_SEED));
  const [isSchedOpen, setIsSchedOpen] = useState(false);
  const [schedReport, setSchedReport] = useState("");
  const [schedFreq, setSchedFreq] = useState("Daily");
  const [schedRecipients, setSchedRecipients] = useState("");
  const [schedChannel, setSchedChannel] = useState("Email");

  const schedReportOptions = savedReports.length > 0
    ? savedReports.map((r) => ({ label: r.name, value: r.name }))
    : SCHED_SEED.map((s) => ({ label: s.reportName, value: s.reportName }));

  const handleAddSchedule = (e) => {
    e.preventDefault();
    const pick = schedReport || schedReportOptions[0]?.value;
    if (!pick || !schedRecipients.trim()) {
      addToast("Missing Fields", "Choose a report and enter at least one recipient.", "error");
      return;
    }
    const entry = {
      id: `sch-${Date.now()}`,
      reportName: pick,
      frequency: schedFreq,
      recipients: schedRecipients.trim(),
      channel: schedChannel,
      lastRun: "Never",
      status: "Active"
    };
    const updated = [entry, ...schedules];
    setSchedules(updated);
    saveLS(SCHED_KEY, updated);
    logAction(user.id, user.name, user.role, "Report Scheduled", `Scheduled "${pick}" (${schedFreq} via ${schedChannel}) to ${schedRecipients}`);
    addToast("Schedule Created", `"${pick}" will run ${schedFreq.toLowerCase()} via ${schedChannel}.`, "success");
    setIsSchedOpen(false);
    setSchedRecipients("");
  };

  const toggleSchedule = (id) => {
    const updated = schedules.map((s) => s.id === id ? { ...s, status: s.status === "Active" ? "Paused" : "Active" } : s);
    setSchedules(updated);
    saveLS(SCHED_KEY, updated);
    const t = updated.find((s) => s.id === id);
    addToast("Schedule Updated", `"${t?.reportName}" is now ${t?.status}.`, "info");
  };

  /* ---------- statutory registers ---------- */
  const [regRanges, setRegRanges] = useState({});
  const [regPreview, setRegPreview] = useState(null);

  const setRange = (regId, key, val) => {
    setRegRanges((prev) => ({ ...prev, [regId]: { ...prev[regId], [key]: val } }));
  };

  const REGISTERS = [
    {
      id: "reg-att",
      name: "Attendance Register",
      desc: "Daily student & staff presence marks",
      getData: () => ({
        headers: ["Date", "Name", "Type", "Status", "Marked At"],
        rows: getAttendance().slice(0, 15).map((a) => [a.date, a.personName, a.type, a.status, a.timestamp || "—"])
      })
    },
    {
      id: "reg-fee",
      name: "Fee Collection Register",
      desc: "Receipt-wise realised fee entries",
      getData: () => ({
        headers: ["Student", "Fee Head", "Invoiced", "Collected", "Status"],
        rows: getFees().filter((f) => (f.paidAmount || 0) > 0).slice(0, 15).map((f) => [f.studentName, f.feeType, inr(f.amount), inr(f.paidAmount), f.status])
      })
    },
    {
      id: "reg-tc",
      name: "TC Register",
      desc: "Transfer certificates issued serial-wise",
      getData: () => {
        const certs = JSON.parse(localStorage.getItem("erp_certificates") || "[]").filter((c) => c.type === "Transfer Certificate");
        return {
          headers: ["Serial No", "Date", "Student", "Reason", "Issued By"],
          rows: certs.length > 0
            ? certs.slice(0, 15).map((c) => [c.serial, c.date, c.studentName, c.reason, c.issuedBy])
            : [["—", "—", "No TC entries yet — issue from Front Office Desk", "—", "—"]]
        };
      }
    },
    {
      id: "reg-stock",
      name: "Stock Register",
      desc: "Store & consumables holding levels",
      getData: () => ({
        headers: ["Item", "Category", "Stock", "Reorder Level", "Unit Price"],
        rows: getInventory().slice(0, 15).map((i) => [i.itemName, i.category, `${i.stock} units`, i.minAlertStock, inr(i.unitPrice)])
      })
    },
    {
      id: "reg-visitor",
      name: "Visitor Register",
      desc: "Campus gate entry & exit log",
      getData: () => ({
        headers: ["Visitor", "Relationship", "Student", "Entry", "Exit"],
        rows: getVisitors().slice(0, 15).map((v) => [v.visitorName, v.relationship, v.studentName, v.entryTime, v.exitTime || "— on premises —"])
      })
    }
  ];

  const openRegisterPreview = (reg) => {
    const range = regRanges[reg.id] || {};
    setRegPreview({ ...reg, data: reg.getData(), from: range.from || "", to: range.to || "" });
    logAction(user.id, user.name, user.role, "Register Preview Opened", `Opened print preview for ${reg.name}${range.from ? ` (${range.from} to ${range.to || "today"})` : ""}`);
  };

  const activeFilterCount = filters.filter((f) => f.value).length;

  return <div className="space-y-6">

      {/* KPI banner row */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">TOTAL INVOICED</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{inr(totalInvoiced)}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">{fees.length} fee invoices raised</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <IndianRupee className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 p-5 rounded-3xl flex justify-between items-center text-white">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">COLLECTED SO FAR</span>
            <h4 className="text-2xl font-black text-emerald-400 mt-1">{inr(totalCollected)}</h4>
            <p className="text-xs text-indigo-200 mt-1">{collectedPct}% realisation rate</p>
          </div>
          <div className="p-3 bg-white/10 text-white rounded-2xl">
            <TrendingUp className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">OUTSTANDING DUES</span>
            <h4 className="text-2xl font-black text-rose-600 mt-1">{inr(totalOutstanding)}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Pending across all fee heads</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl">
            <BarChart3 className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">ENROLLED STUDENTS</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{students.length}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">{gradeGroups.length} grade levels active</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Users className="h-6 w-6" />
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-100 p-6 rounded-3xl">
        <Tabs activeTab={vTab} onChange={setVTab}>
          <TabsList className="border-b border-slate-100 flex gap-2">
            <TabsTrigger value="dashboard">Executive Dashboard</TabsTrigger>
            <TabsTrigger value="builder">Custom Report Builder</TabsTrigger>
            <TabsTrigger value="scheduled">Scheduled Reports</TabsTrigger>
            <TabsTrigger value="registers">Statutory Registers</TabsTrigger>
          </TabsList>

          {/* TAB 1: Dashboard */}
          <TabsContent value="dashboard">
            <div className="space-y-6 pt-4">

              {/* Fee collection stacked bar */}
              <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-1">Fee Collection Position</h4>
                <p className="text-xs text-slate-400 font-semibold mb-4">Invoiced {inr(totalInvoiced)} · Collected {inr(totalCollected)} · Outstanding {inr(totalOutstanding)}</p>
                <div className="w-full h-7 rounded-full overflow-hidden flex bg-slate-200">
                  <div className="bg-emerald-500 h-full flex items-center justify-center text-[10px] font-black text-white" style={{ width: `${collectedPct}%` }}>
                    {collectedPct >= 12 ? `${collectedPct}% COLLECTED` : ""}
                  </div>
                  <div className="bg-rose-400 h-full flex items-center justify-center text-[10px] font-black text-white" style={{ width: `${100 - collectedPct}%` }}>
                    {100 - collectedPct >= 12 ? `${100 - collectedPct}% DUE` : ""}
                  </div>
                </div>
                <div className="flex gap-5 mt-3 text-[10px] uppercase font-extrabold tracking-wider">
                  <span className="flex items-center gap-1.5 text-emerald-700"><span className="h-2.5 w-2.5 rounded-full bg-emerald-500" /> Collected {inr(totalCollected)}</span>
                  <span className="flex items-center gap-1.5 text-rose-700"><span className="h-2.5 w-2.5 rounded-full bg-rose-400" /> Outstanding {inr(totalOutstanding)}</span>
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Enrollment by grade */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Enrollment by Grade</h4>
                  <div className="flex items-end gap-3 h-40">
                    {gradeGroups.map(([grade, count]) => <div key={grade} className="flex-1 flex flex-col items-center justify-end gap-1.5 h-full">
                        <span className="text-[10px] font-black text-slate-700">{count}</span>
                        <div className="w-full bg-indigo-500 rounded-t-lg transition-all" style={{ height: `${Math.max(6, Math.round(count / maxGradeCount * 100))}%` }} />
                        <span className="text-[9px] uppercase font-extrabold text-slate-400 tracking-wide text-center leading-tight">{grade}</span>
                      </div>)}
                  </div>
                </div>

                {/* Attendance trend sparkline */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Attendance Trend (7 Days)</h4>
                  <div className="flex items-end gap-3 h-40">
                    {attTrend.map((d) => <div key={d.date} className="flex-1 flex flex-col items-center justify-end gap-1.5 h-full">
                        <span className="text-[10px] font-black text-slate-700">{d.pct}%</span>
                        <div className={`w-full rounded-t-lg ${d.pct >= 90 ? "bg-emerald-500" : d.pct >= 80 ? "bg-amber-400" : "bg-rose-500"}`} style={{ height: `${Math.max(6, d.pct)}%` }} />
                        <span className="text-[9px] uppercase font-extrabold text-slate-400 tracking-wide">{d.date.slice(5)}</span>
                      </div>)}
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Gender split */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Gender Split</h4>
                  <div className="w-full h-6 rounded-full overflow-hidden flex bg-slate-200">
                    <div className="bg-sky-500 h-full" style={{ width: `${malePct}%` }} />
                    <div className="bg-pink-400 h-full" style={{ width: `${100 - malePct}%` }} />
                  </div>
                  <div className="flex gap-5 mt-3 text-[10px] uppercase font-extrabold tracking-wider">
                    <span className="flex items-center gap-1.5 text-sky-700"><span className="h-2.5 w-2.5 rounded-full bg-sky-500" /> Boys {maleCount} ({malePct}%)</span>
                    <span className="flex items-center gap-1.5 text-pink-700"><span className="h-2.5 w-2.5 rounded-full bg-pink-400" /> Girls {femaleCount} ({100 - malePct}%)</span>
                  </div>
                </div>

                {/* Hostel vs day scholar */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4 flex items-center gap-2"><Bed className="h-4 w-4 text-indigo-500" /> Hostel vs Day Scholar</h4>
                  <div className="w-full h-6 rounded-full overflow-hidden flex bg-slate-200">
                    <div className="bg-indigo-500 h-full" style={{ width: `${hostelPct}%` }} />
                    <div className="bg-amber-400 h-full" style={{ width: `${100 - hostelPct}%` }} />
                  </div>
                  <div className="flex gap-5 mt-3 text-[10px] uppercase font-extrabold tracking-wider">
                    <span className="flex items-center gap-1.5 text-indigo-700"><span className="h-2.5 w-2.5 rounded-full bg-indigo-500" /> Boarders {hostelCount} ({hostelPct}%)</span>
                    <span className="flex items-center gap-1.5 text-amber-700"><span className="h-2.5 w-2.5 rounded-full bg-amber-400" /> Day Scholars {dayCount} ({100 - hostelPct}%)</span>
                  </div>
                </div>
              </div>

            </div>
          </TabsContent>

          {/* TAB 2: Builder */}
          <TabsContent value="builder">
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 pt-4">
              <div className="xl:col-span-2 space-y-5">

                {/* Step chips */}
                <div className="flex gap-2 flex-wrap">
                  {[{ n: 1, t: "Source" }, { n: 2, t: "Filters" }, { n: 3, t: "Columns" }, { n: 4, t: "Preview" }].map((s) => <button
                    key={s.n}
                    onClick={() => setStep(s.n)}
                    className={`px-3.5 py-1.5 rounded-full text-[10px] uppercase tracking-wider font-extrabold border transition cursor-pointer ${step === s.n ? "bg-slate-900 text-white border-slate-900" : "bg-white text-slate-500 border-slate-200 hover:border-slate-400"}`}
                  >
                      {s.n}. {s.t}
                    </button>)}
                </div>

                {/* Step 1: Source */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-700 uppercase tracking-widest flex items-center gap-2"><Layers className="h-4 w-4 text-indigo-500" /> 1 · Data Source</h4>
                  <Select
                    label="Choose entity to report on"
                    options={Object.entries(SOURCES).map(([k, v]) => ({ label: v.label, value: k }))}
                    value={source}
                    onChange={(e) => changeSource(e.target.value)}
                  />
                </div>

                {/* Step 2: Filters */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl space-y-3">
                  <div className="flex justify-between items-center">
                    <h4 className="text-xs font-extrabold text-slate-700 uppercase tracking-widest flex items-center gap-2"><Filter className="h-4 w-4 text-indigo-500" /> 2 · Filters ({activeFilterCount} active)</h4>
                    <Button size="sm" variant="outline" onClick={() => setFilters([...filters, { field: SOURCES[source].filterFields[0], op: "equals", value: "" }])} className="flex gap-1 items-center">
                      <Plus className="h-3.5 w-3.5" /> Add Filter
                    </Button>
                  </div>
                  {filters.map((f, idx) => <div key={idx} className="grid grid-cols-1 sm:grid-cols-[1fr_1fr_1fr_auto] gap-3 items-end">
                      <Select
                        label={idx === 0 ? "Field" : undefined}
                        options={SOURCES[source].filterFields.map((ff) => ({ label: ff, value: ff }))}
                        value={f.field}
                        onChange={(e) => updateFilter(idx, { field: e.target.value })}
                      />
                      <Select
                        label={idx === 0 ? "Operator" : undefined}
                        options={[{ label: "equals", value: "equals" }, { label: "contains", value: "contains" }]}
                        value={f.op}
                        onChange={(e) => updateFilter(idx, { op: e.target.value })}
                      />
                      <Input
                        label={idx === 0 ? "Value" : undefined}
                        value={f.value}
                        onChange={(e) => updateFilter(idx, { value: e.target.value })}
                        placeholder="e.g. Grade 8 / Unpaid / Female"
                      />
                      <Button size="sm" variant="ghost" onClick={() => setFilters(filters.filter((_, i) => i !== idx))} className="text-rose-500 mb-1">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>)}
                </div>

                {/* Step 3: Columns */}
                <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl space-y-3">
                  <h4 className="text-xs font-extrabold text-slate-700 uppercase tracking-widest">3 · Output Columns ({columns.length} picked)</h4>
                  <div className="flex flex-wrap gap-2">
                    {SOURCES[source].fields.map((field) => {
                      const on = columns.includes(field);
                      return <button
                        key={field}
                        onClick={() => toggleColumn(field)}
                        className={`px-3 py-1.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide border transition cursor-pointer ${on ? "bg-indigo-600 text-white border-indigo-600" : "bg-white text-slate-500 border-slate-200 hover:border-indigo-300"}`}
                      >
                          {field}
                        </button>;
                    })}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-wrap gap-3 items-end">
                  <Button onClick={handleGenerate} className="flex gap-2 items-center">
                    <Play className="h-4 w-4" /> Generate Preview
                  </Button>
                  <Button variant="outline" onClick={handleDownloadCSV} className="flex gap-2 items-center">
                    <Download className="h-4 w-4" /> Download CSV
                  </Button>
                  <div className="flex gap-2 items-end flex-1 min-w-[220px]">
                    <Input label="Report Name" value={reportName} onChange={(e) => setReportName(e.target.value)} placeholder="e.g. Grade 8 Fee Defaulters" />
                    <Button variant="secondary" onClick={handleSaveReport} className="flex gap-2 items-center shrink-0">
                      <Save className="h-4 w-4" /> Save
                    </Button>
                  </div>
                </div>

                {/* Step 4: Preview */}
                {previewRows && <div className="space-y-2">
                    <p className="text-[10px] uppercase tracking-widest font-extrabold text-slate-400">
                      4 · Preview — showing {previewRows.length} of {totalMatched} matching rows
                    </p>
                    <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                      <table className="w-full text-xs font-semibold text-slate-700 text-left">
                        <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                          <tr>
                            {columns.map((c) => <th key={c} className="p-3.5">{c}</th>)}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {previewRows.length === 0 ? <tr><td colSpan={columns.length} className="p-4 text-center text-slate-400">No rows matched the filter criteria</td></tr> : previewRows.map((row, ri) => <tr key={ri}>
                              {columns.map((c) => <td key={c} className="p-3.5">{typeof row[c] === "boolean" ? (row[c] ? "Yes" : "No") : String(row[c] ?? "—")}</td>)}
                            </tr>)}
                        </tbody>
                      </table>
                    </div>
                  </div>}
              </div>

              {/* Saved reports panel */}
              <div className="bg-slate-50 border border-slate-100 p-5 rounded-2xl h-fit space-y-3">
                <h4 className="text-xs font-extrabold text-slate-700 uppercase tracking-widest flex items-center gap-2"><FileText className="h-4 w-4 text-indigo-500" /> Saved Reports Library</h4>
                {savedReports.length === 0 ? <p className="text-xs text-slate-400 font-semibold py-6 text-center">No saved definitions yet. Build one and hit Save.</p> : savedReports.map((r) => <div key={r.id} className="bg-white border border-slate-100 p-3.5 rounded-xl flex justify-between items-center gap-2">
                    <div className="min-w-0">
                      <p className="text-xs font-black text-slate-800 truncate">{r.name}</p>
                      <p className="text-[9px] uppercase font-extrabold text-slate-400 tracking-wide mt-0.5">{SOURCES[r.source]?.label} · {r.filters.length} filters · {r.columns.length} cols</p>
                    </div>
                    <div className="flex gap-1.5 shrink-0">
                      <button onClick={() => handleRunSaved(r)} title="Run report" className="p-2 rounded-lg bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white transition cursor-pointer">
                        <Play className="h-3.5 w-3.5" />
                      </button>
                      <button onClick={() => handleDeleteSaved(r.id)} title="Delete definition" className="p-2 rounded-lg bg-rose-50 text-rose-500 hover:bg-rose-600 hover:text-white transition cursor-pointer">
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>)}
              </div>
            </div>
          </TabsContent>

          {/* TAB 3: Scheduled */}
          <TabsContent value="scheduled">
            <div className="space-y-5 pt-4">
              <div className="bg-amber-50 border border-amber-200 text-amber-900 p-4 rounded-2xl text-xs font-semibold flex gap-2 items-start">
                <CalendarClock className="h-4 w-4 shrink-0 mt-0.5 text-amber-600" />
                <span><span className="font-black">Delivery simulated in sandbox.</span> Email / WhatsApp dispatch runs are recorded locally; no messages leave this environment.</span>
              </div>

              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Auto-Delivery Schedules</h4>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Recurring report dispatch to management & guardians.</p>
                </div>
                <Button onClick={() => setIsSchedOpen(true)} className="flex gap-2 items-center text-xs">
                  <Plus className="h-4 w-4" /> Schedule Report
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Report Name</th>
                      <th className="p-4">Frequency</th>
                      <th className="p-4">Recipients</th>
                      <th className="p-4">Channel</th>
                      <th className="p-4">Last Run</th>
                      <th className="p-4 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {schedules.map((s) => <tr key={s.id}>
                        <td className="p-4 font-extrabold text-slate-800">{s.reportName}</td>
                        <td className="p-4"><Badge variant="info">{s.frequency}</Badge></td>
                        <td className="p-4 text-slate-500">{s.recipients}</td>
                        <td className="p-4"><Badge variant={s.channel === "Email" ? "secondary" : "success"}>{s.channel}</Badge></td>
                        <td className="p-4 text-slate-400 font-bold">{s.lastRun}</td>
                        <td className="p-4 text-center">
                          <button
                            onClick={() => toggleSchedule(s.id)}
                            className={`text-[10px] uppercase font-black px-3 py-1.5 rounded-full border transition cursor-pointer ${s.status === "Active" ? "bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-600 hover:text-white" : "bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-600 hover:text-white"}`}
                          >
                            {s.status}
                          </button>
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </TabsContent>

          {/* TAB 4: Registers */}
          <TabsContent value="registers">
            <div className="space-y-5 pt-4">
              <div>
                <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Statutory Registers — Quick Print</h4>
                <p className="text-xs text-slate-400 font-semibold mt-1">Board-inspection ready registers generated from live sandbox records.</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                {REGISTERS.map((reg) => <div key={reg.id} className="bg-slate-50 border border-slate-100 p-5 rounded-2xl space-y-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="text-xs font-black text-slate-800">{reg.name}</p>
                        <p className="text-[10px] text-slate-400 font-bold mt-0.5">{reg.desc}</p>
                      </div>
                      <div className="p-2 bg-white border border-slate-100 rounded-xl text-slate-500">
                        <FileText className="h-4 w-4" />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <Input label="From" type="date" value={regRanges[reg.id]?.from || ""} onChange={(e) => setRange(reg.id, "from", e.target.value)} />
                      <Input label="To" type="date" value={regRanges[reg.id]?.to || ""} onChange={(e) => setRange(reg.id, "to", e.target.value)} />
                    </div>
                    <Button variant="outline" size="sm" onClick={() => openRegisterPreview(reg)} className="w-full flex gap-2 items-center justify-center">
                      <Printer className="h-3.5 w-3.5" /> Print Preview
                    </Button>
                  </div>)}
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Schedule dialog */}
      <Dialog isOpen={isSchedOpen} onClose={() => setIsSchedOpen(false)} title="Schedule Report Delivery">
        <form onSubmit={handleAddSchedule} className="space-y-4 pt-1">
          <Select
            label="Report to deliver"
            options={schedReportOptions}
            value={schedReport || schedReportOptions[0]?.value || ""}
            onChange={(e) => setSchedReport(e.target.value)}
          />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Frequency"
              options={[{ label: "Daily", value: "Daily" }, { label: "Weekly", value: "Weekly" }, { label: "Monthly", value: "Monthly" }]}
              value={schedFreq}
              onChange={(e) => setSchedFreq(e.target.value)}
            />
            <Select
              label="Delivery Channel"
              options={[{ label: "Email", value: "Email" }, { label: "WhatsApp", value: "WhatsApp" }]}
              value={schedChannel}
              onChange={(e) => setSchedChannel(e.target.value)}
            />
          </div>
          <Input
            label="Recipients"
            value={schedRecipients}
            onChange={(e) => setSchedRecipients(e.target.value)}
            placeholder="e.g. principal@stjosephs.edu.in, +91 98450 12233"
            required
          />
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsSchedOpen(false)}>Cancel</Button>
            <Button type="submit">Create Schedule</Button>
          </div>
        </form>
      </Dialog>

      {/* Register print preview dialog */}
      <Dialog isOpen={!!regPreview} onClose={() => setRegPreview(null)} title={regPreview ? `${regPreview.name} — Print Preview` : ""} maxWidth="max-w-3xl">
        {regPreview && <div className="space-y-4">
            <div className="text-center border-b border-slate-200 pb-3">
              <p className="text-sm font-black text-slate-800 uppercase tracking-widest">St. Joseph's International School, Bengaluru</p>
              <p className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400 mt-1">
                {regPreview.name} {regPreview.from ? `· Period: ${regPreview.from} to ${regPreview.to || "date"}` : "· Full period"} · Academic Year 2026-27
              </p>
            </div>
            <div className="overflow-x-auto border border-slate-200 rounded-xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>{regPreview.data.headers.map((h) => <th key={h} className="p-3">{h}</th>)}</tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {regPreview.data.rows.map((row, ri) => <tr key={ri}>
                      {row.map((cell, ci) => <td key={ci} className="p-3">{cell}</td>)}
                    </tr>)}
                </tbody>
              </table>
            </div>
            <div className="flex gap-3 justify-end pt-2 border-t border-slate-100">
              <Button variant="outline" onClick={() => setRegPreview(null)}>Close</Button>
              <Button onClick={() => { addToast("Sent to Printer", `${regPreview.name} queued on office printer (simulated).`, "success"); logAction(user.id, user.name, user.role, "Register Printed", `Printed ${regPreview.name} (${regPreview.data.rows.length} rows)`); }} className="flex gap-2 items-center">
                <Printer className="h-4 w-4" /> Print
              </Button>
            </div>
          </div>}
      </Dialog>

    </div>;
}
