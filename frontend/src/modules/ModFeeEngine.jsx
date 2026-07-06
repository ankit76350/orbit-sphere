/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, getFees, payInvoice, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast, Tabs, TabsList, TabsTrigger, TabsContent } from "../components/ui";
import {
  Layers, Plus, Percent, ShieldCheck, MessageCircle, Send, Phone, FileWarning,
  Zap, CreditCard, Landmark, FileDown, FileSpreadsheet, IndianRupee, CheckCircle2,
  XCircle, Repeat, AlertTriangle, Clock
} from "lucide-react";

const inr = (n) => "₹" + Number(n || 0).toLocaleString("en-IN");
const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));
const hashCode = (str) => { let h = 0; for (let i = 0; i < String(str).length; i++) { h = (h * 31 + String(str).charCodeAt(i)) >>> 0; } return h; };
const today = () => new Date().toISOString().split("T")[0];
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

const SEED_HEADS = [
  { id: "head-1", name: "Tuition Fee", amount: 4500, frequency: "Monthly", taxable: false },
  { id: "head-2", name: "Transport Fee", amount: 1800, frequency: "Monthly", taxable: false },
  { id: "head-3", name: "Hostel Fee", amount: 6500, frequency: "Monthly", taxable: false },
  { id: "head-4", name: "Mess Charges", amount: 3200, frequency: "Monthly", taxable: false },
  { id: "head-5", name: "Lab Fee", amount: 2400, frequency: "Quarterly", taxable: false },
  { id: "head-6", name: "Library Fee", amount: 1200, frequency: "Annual", taxable: false },
  { id: "head-7", name: "Exam Fee", amount: 1500, frequency: "Quarterly", taxable: false },
  { id: "head-8", name: "Annual Charges", amount: 8000, frequency: "Annual", taxable: true }
];

const SEED_TEMPLATES = [
  { id: "tpl-6", grade: "Grade 6", heads: ["Tuition Fee", "Library Fee", "Exam Fee", "Annual Charges"], applicable: 42 },
  { id: "tpl-7", grade: "Grade 7", heads: ["Tuition Fee", "Library Fee", "Exam Fee", "Annual Charges"], applicable: 45 },
  { id: "tpl-8", grade: "Grade 8", heads: ["Tuition Fee", "Lab Fee", "Library Fee", "Exam Fee", "Annual Charges"], applicable: 40 },
  { id: "tpl-9", grade: "Grade 9", heads: ["Tuition Fee", "Lab Fee", "Library Fee", "Exam Fee", "Annual Charges"], applicable: 38 },
  { id: "tpl-10", grade: "Grade 10", heads: ["Tuition Fee", "Lab Fee", "Library Fee", "Exam Fee", "Annual Charges"], applicable: 36 },
  { id: "tpl-12", grade: "Grade 12 (Science)", heads: ["Tuition Fee", "Lab Fee", "Hostel Fee", "Mess Charges", "Exam Fee", "Annual Charges"], applicable: 28 }
];

const SEED_CONCESSIONS = {
  policies: [
    { id: "pol-1", name: "Sibling Discount", percent: "20%", criteria: "2nd child onwards enrolled in same session", active: true },
    { id: "pol-2", name: "Staff Ward", percent: "50%", criteria: "Children of full-time teaching / non-teaching staff", active: true },
    { id: "pol-3", name: "Merit Scholarship", percent: "25%", criteria: "Board / annual exam score above 92%", active: true },
    { id: "pol-4", name: "RTE Quota", percent: "100%", criteria: "Right To Education Act 2009 — 25% intake at entry level", active: true },
    { id: "pol-5", name: "Financial Hardship", percent: "Custom", criteria: "Case-by-case, income certificate required", active: true }
  ],
  queue: [
    { id: "con-1", student: "Ananya Sharma (Grade 8)", type: "Sibling Discount", amount: 10800, requestedBy: "R. Iyer (Accountant)", status: "Pending" },
    { id: "con-2", student: "Kabir Singh (Grade 9)", type: "Merit Scholarship", amount: 13500, requestedBy: "R. Iyer (Accountant)", status: "Pending" },
    { id: "con-3", student: "Meera Patel (Grade 6)", type: "Staff Ward", amount: 27000, requestedBy: "S. Kulkarni (Accountant)", status: "Pending" },
    { id: "con-4", student: "Vivaan Gupta (Grade 7)", type: "Financial Hardship", amount: 18000, requestedBy: "R. Iyer (Accountant)", status: "Approved" }
  ]
};

const SEED_MANDATES = [
  { id: "mnd-1", parent: "Rajesh Sharma", student: "Ananya Sharma", amount: 4500, status: "Active", nextDebit: "2026-07-05", ref: "UPIMNDT8821K@okaxis" },
  { id: "mnd-2", parent: "Priya Verma", student: "Aditya Verma", amount: 6300, status: "Active", nextDebit: "2026-07-10", ref: "UPIMNDT4410P@oksbi" },
  { id: "mnd-3", parent: "Sunil Reddy", student: "Sai Reddy", amount: 4500, status: "Paused", nextDebit: "—", ref: "UPIMNDT7702R@ybl" },
  { id: "mnd-4", parent: "Deepa Mehta", student: "Diya Mehta", amount: 8200, status: "Active", nextDebit: "2026-07-07", ref: "UPIMNDT1198M@paytm" }
];

const LADDER = ["WhatsApp Reminder", "SMS Notice", "Call List", "Meeting Letter"];
const LADDER_TOASTS = [
  "WhatsApp reminder queued via BSP (Meta Business API)",
  "SMS notice pushed to DLT-registered gateway",
  "Added to front-office call list for tomorrow morning",
  "Formal meeting letter generated for principal's signature"
];

export default function ModFeeEngine({ user }) {
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState("structures");
  const [invoices, setInvoices] = useState(() => getFees());
  const [heads, setHeads] = useState(() => loadLS("erp_fee_heads", SEED_HEADS));
  const [templates, setTemplates] = useState(() => loadLS("erp_fee_templates", SEED_TEMPLATES));
  const [concessions, setConcessions] = useState(() => loadLS("erp_concessions", SEED_CONCESSIONS));
  const [reminderLog, setReminderLog] = useState(() => loadLS("erp_reminder_log", {}));
  const [mandates, setMandates] = useState(() => loadLS("erp_mandates", SEED_MANDATES));
  const [gateways, setGateways] = useState(() => loadLS("erp_gateways", [
    { id: "gw-1", name: "Razorpay", connected: true, fee: "0.90% + GST", modes: "UPI / Cards / NetBanking" },
    { id: "gw-2", name: "Cashfree", connected: false, fee: "0.85% + GST", modes: "UPI / Cards / EMI" },
    { id: "gw-3", name: "Easebuzz", connected: false, fee: "0.75% + GST", modes: "UPI / NEFT / e-NACH" }
  ]));
  const [exports, setExports] = useState(() => loadLS("erp_tally_exports", []));

  // ---- dialogs / transient state ----
  const [isHeadOpen, setIsHeadOpen] = useState(false);
  const [headName, setHeadName] = useState("");
  const [headAmount, setHeadAmount] = useState("1000");
  const [headFreq, setHeadFreq] = useState("Monthly");
  const [headTaxable, setHeadTaxable] = useState("No");
  const [editTemplate, setEditTemplate] = useState(null);
  const [tplHeads, setTplHeads] = useState([]);
  const [bulkProgress, setBulkProgress] = useState(null);
  const [isMandateOpen, setIsMandateOpen] = useState(false);
  const [mndStudent, setMndStudent] = useState("");
  const [mndAmount, setMndAmount] = useState("4500");
  const [mndDay, setMndDay] = useState("5");
  const [settlement, setSettlement] = useState(null);
  const [settling, setSettling] = useState(false);
  const [expFrom, setExpFrom] = useState("2026-06-01");
  const [expTo, setExpTo] = useState(today());

  const students = getStudents();
  const canApprove = user.role === "Principal" || user.role === "Super Admin";
  const headTotal = (names) => names.reduce((sum, nm) => { const h = heads.find((x) => x.name === nm); return sum + (h ? h.amount : 0); }, 0);

  // ================= STRUCTURES =================
  const handleAddHead = (e) => {
    e.preventDefault();
    if (!headName.trim()) { addToast("Validation", "Fee head name is required", "error"); return; }
    const next = [...heads, { id: `head-${Date.now()}`, name: headName.trim(), amount: parseFloat(headAmount) || 0, frequency: headFreq, taxable: headTaxable === "Yes" }];
    setHeads(next);
    saveLS("erp_fee_heads", next);
    setIsHeadOpen(false);
    setHeadName("");
    logAction(user.id, user.name, user.role, "Fee Head Created", `Added fee head "${headName.trim()}" ${inr(headAmount)} / ${headFreq}`);
    addToast("Fee Head Added", `"${headName.trim()}" saved to structure master`);
  };

  const openTemplateEditor = (tpl) => { setEditTemplate(tpl); setTplHeads([...tpl.heads]); };
  const toggleTplHead = (name) => setTplHeads((prev) => prev.includes(name) ? prev.filter((h) => h !== name) : [...prev, name]);
  const handleSaveTemplate = () => {
    if (!editTemplate) return;
    const next = templates.map((t) => t.id === editTemplate.id ? { ...t, heads: tplHeads } : t);
    setTemplates(next);
    saveLS("erp_fee_templates", next);
    logAction(user.id, user.name, user.role, "Fee Template Updated", `${editTemplate.grade} template now composes ${tplHeads.length} heads, total ${inr(headTotal(tplHeads))}`);
    addToast("Template Saved", `${editTemplate.grade} fee template recomposed`);
    setEditTemplate(null);
  };

  // ================= CONCESSIONS =================
  const handleConcessionDecision = (id, decision) => {
    const item = concessions.queue.find((q) => q.id === id);
    const next = { ...concessions, queue: concessions.queue.map((q) => q.id === id ? { ...q, status: decision } : q) };
    setConcessions(next);
    saveLS("erp_concessions", next);
    logAction(user.id, user.name, user.role, `Concession ${decision}`, `${decision} ${item?.type} of ${inr(item?.amount)} for ${item?.student} (maker: ${item?.requestedBy})`);
    addToast(`Concession ${decision}`, `${item?.student} — ${item?.type} ${inr(item?.amount)}`, decision === "Approved" ? "success" : "warning");
  };

  // ================= DEFAULTERS =================
  const dueInvoices = invoices.filter((i) => i.status !== "Paid");
  const invAging = (inv) => 3 + (hashCode(inv.id) % 128); // stable pseudo-random days overdue
  const buckets = { "0-30": 0, "31-60": 0, "61-90": 0, "90+": 0 };
  dueInvoices.forEach((inv) => {
    const due = inv.amount - inv.paidAmount;
    const d = invAging(inv);
    if (d <= 30) buckets["0-30"] += due;
    else if (d <= 60) buckets["31-60"] += due;
    else if (d <= 90) buckets["61-90"] += due;
    else buckets["90+"] += due;
  });
  const defaulterMap = {};
  dueInvoices.forEach((inv) => {
    if (!defaulterMap[inv.studentId]) defaulterMap[inv.studentId] = { studentId: inv.studentId, name: inv.studentName, grade: inv.grade, count: 0, due: 0, maxDays: 0 };
    const rec = defaulterMap[inv.studentId];
    rec.count += 1;
    rec.due += inv.amount - inv.paidAmount;
    rec.maxDays = Math.max(rec.maxDays, invAging(inv));
  });
  const defaulters = Object.values(defaulterMap).sort((a, b) => b.due - a.due);

  const advanceReminder = (studentId, name) => {
    const cur = reminderLog[studentId]?.stage ?? 0;
    if (cur >= LADDER.length) return;
    const next = { ...reminderLog, [studentId]: { stage: cur + 1, last: LADDER[cur], date: today() } };
    setReminderLog(next);
    saveLS("erp_reminder_log", next);
    logAction(user.id, user.name, user.role, "Dues Escalation", `Stage ${cur + 1} (${LADDER[cur]}) triggered for defaulter ${name}`);
    addToast(LADDER[cur], LADDER_TOASTS[cur], cur >= 2 ? "warning" : "info");
  };

  const handleBulkEscalate = () => {
    if (bulkProgress !== null) return;
    const targets = defaulters.filter((d) => (reminderLog[d.studentId]?.stage ?? 0) === 0);
    if (targets.length === 0) { addToast("Nothing To Do", "No defaulters remaining at stage-0", "info"); return; }
    setBulkProgress(0);
    let pct = 0;
    const timer = setInterval(() => {
      pct += 12;
      if (pct >= 100) {
        clearInterval(timer);
        setBulkProgress(100);
        const next = { ...reminderLog };
        targets.forEach((d) => { next[d.studentId] = { stage: 1, last: LADDER[0], date: today() }; });
        setReminderLog(next);
        saveLS("erp_reminder_log", next);
        logAction(user.id, user.name, user.role, "Bulk Dues Escalation", `WhatsApp stage-1 reminders queued for ${targets.length} defaulter families`);
        addToast("Bulk Escalation Complete", `${targets.length} WhatsApp reminders queued via BSP`);
        setTimeout(() => setBulkProgress(null), 900);
      } else {
        setBulkProgress(pct);
      }
    }, 180);
  };

  // ================= ONLINE PAYMENTS =================
  const toggleGateway = (id) => {
    const next = gateways.map((g) => g.id === id ? { ...g, connected: !g.connected } : g);
    setGateways(next);
    saveLS("erp_gateways", next);
    const gw = next.find((g) => g.id === id);
    logAction(user.id, user.name, user.role, "Gateway Toggled", `${gw.name} marked ${gw.connected ? "Connected" : "Disconnected"}`);
    addToast(gw.connected ? "Gateway Connected" : "Gateway Disconnected", `${gw.name} keys ${gw.connected ? "verified against live mode" : "revoked from vault"}`, gw.connected ? "success" : "warning");
  };

  const toggleMandate = (id) => {
    const next = mandates.map((m) => m.id === id ? { ...m, status: m.status === "Active" ? "Paused" : "Active", nextDebit: m.status === "Active" ? "—" : "2026-08-05" } : m);
    setMandates(next);
    saveLS("erp_mandates", next);
    addToast("Mandate Updated", "UPI AutoPay mandate state pushed to NPCI switch (mock)", "info");
  };

  const handleCreateMandate = (e) => {
    e.preventDefault();
    const st = students.find((s) => s.id === mndStudent);
    if (!st) { addToast("Validation", "Select a student for the mandate", "error"); return; }
    const ref = `UPIMNDT${Math.floor(1000 + Math.random() * 9000)}${st.name.charAt(0).toUpperCase()}@okaxis`;
    const rec = { id: `mnd-${Date.now()}`, parent: st.parentName, student: st.name, amount: parseFloat(mndAmount) || 0, status: "Active", nextDebit: `2026-08-${String(mndDay).padStart(2, "0")}`, ref };
    const next = [rec, ...mandates];
    setMandates(next);
    saveLS("erp_mandates", next);
    setIsMandateOpen(false);
    logAction(user.id, user.name, user.role, "UPI Mandate Created", `AutoPay mandate ${ref} for ${st.parentName} / ${st.name}, ${inr(mndAmount)} on day ${mndDay}`);
    addToast("Mandate Registered", `Reference ${ref} — first debit day ${mndDay}`);
  };

  const handleSettlement = () => {
    if (settling) return;
    const unpaid = getFees().filter((i) => i.status !== "Paid");
    if (unpaid.length === 0) { addToast("No Dues", "Every invoice is already settled", "info"); return; }
    setSettling(true);
    setTimeout(() => {
      const count = Math.min(unpaid.length, 2 + Math.floor(Math.random() * 2));
      const picked = [...unpaid].sort(() => Math.random() - 0.5).slice(0, count);
      let gross = 0;
      const lines = [];
      picked.forEach((inv) => {
        const due = inv.amount - inv.paidAmount;
        const ok = payInvoice(inv.id, due, "UPI (Gateway)", user.name, user.role);
        if (ok) { gross += due; lines.push({ student: inv.studentName, feeType: inv.feeType, amount: due }); }
      });
      const fee = Math.round(gross * 0.009);
      setInvoices(getFees());
      setSettlement({ lines, gross, fee, net: gross - fee, utr: `UTR${Math.floor(1e9 + Math.random() * 9e9)}` });
      setSettling(false);
      logAction(user.id, user.name, user.role, "Gateway Settlement", `T+1 settlement of ${lines.length} UPI collections, gross ${inr(gross)}, net ${inr(gross - fee)}`);
      addToast("Settlement Received", `${lines.length} invoices auto-reconciled from gateway webhook`);
    }, 1200);
  };

  // ================= TALLY EXPORT =================
  const paidInRange = () => getFees().filter((i) => i.paidAmount > 0 && (i.paymentHistory || []).some((p) => p.date >= expFrom && p.date <= expTo));

  const recordExport = (type, filename, count) => {
    const next = [{ id: `exp-${Date.now()}`, type, filename, count, date: today(), by: user.name }, ...exports].slice(0, 10);
    setExports(next);
    saveLS("erp_tally_exports", next);
  };

  const handleTallyXML = () => {
    const rows = paidInRange();
    if (rows.length === 0) { addToast("Empty Range", "No paid receipts inside the selected date range", "warning"); return; }
    let vouchers = "";
    rows.forEach((inv) => {
      (inv.paymentHistory || []).filter((p) => p.date >= expFrom && p.date <= expTo).forEach((p) => {
        vouchers += `    <VOUCHER VCHTYPE="Receipt" ACTION="Create">\n      <DATE>${p.date.replace(/-/g, "")}</DATE>\n      <NARRATION>Fee collection ${inv.feeType} - ${inv.studentName} (${p.receiptNumber})</NARRATION>\n      <LEDGERENTRY><LEDGERNAME>Fee Collection</LEDGERNAME><AMOUNT>${p.amount}</AMOUNT></LEDGERENTRY>\n      <LEDGERENTRY><LEDGERNAME>${p.method}</LEDGERNAME><AMOUNT>-${p.amount}</AMOUNT></LEDGERENTRY>\n    </VOUCHER>\n`;
      });
    });
    const xml = `<ENVELOPE>\n  <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>\n  <BODY><IMPORTDATA><REQUESTDESC><REPORTNAME>Vouchers</REPORTNAME></REQUESTDESC><REQUESTDATA>\n${vouchers}  </REQUESTDATA></IMPORTDATA></BODY>\n</ENVELOPE>\n`;
    const fname = `tally-vouchers-${expFrom}-to-${expTo}.xml`;
    downloadFile(fname, xml, "application/xml");
    recordExport("Tally XML", fname, rows.length);
    logAction(user.id, user.name, user.role, "Tally XML Export", `Exported ${rows.length} receipt vouchers (${expFrom} → ${expTo}) as ${fname}`);
    addToast("Tally XML Downloaded", `${rows.length} receipt vouchers packaged for Tally Prime import`);
  };

  const handleCSVDayBook = () => {
    const rows = paidInRange();
    if (rows.length === 0) { addToast("Empty Range", "No paid receipts inside the selected date range", "warning"); return; }
    let csv = "Date,Receipt No,Student,Fee Head,Method,Amount (INR)\n";
    rows.forEach((inv) => {
      (inv.paymentHistory || []).filter((p) => p.date >= expFrom && p.date <= expTo).forEach((p) => {
        csv += `${p.date},${p.receiptNumber},"${inv.studentName}",${inv.feeType},${p.method},${p.amount}\n`;
      });
    });
    const fname = `daybook-${expFrom}-to-${expTo}.csv`;
    downloadFile(fname, csv, "text/csv");
    recordExport("CSV Day Book", fname, rows.length);
    logAction(user.id, user.name, user.role, "Day Book Export", `Exported CSV day book (${expFrom} → ${expTo}) as ${fname}`);
    addToast("Day Book Downloaded", "CSV ready for accountant reconciliation");
  };

  const totalDue = dueInvoices.reduce((s, i) => s + (i.amount - i.paidAmount), 0);

  return <div className="space-y-6">

      {/* Header banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <IndianRupee className="h-5 w-5 text-indigo-600" /> Fee Engine &amp; Dues (India)
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Structures, concessions maker-checker, aging escalations, UPI AutoPay and Tally hand-off.</p>
        </div>
        <Badge variant="info">AY 2026-27 · INR</Badge>
      </div>

      <Tabs activeTab={activeTab} onChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="structures">Fee Structures</TabsTrigger>
          <TabsTrigger value="concessions">Concessions</TabsTrigger>
          <TabsTrigger value="defaulters">Defaulters &amp; Aging</TabsTrigger>
          <TabsTrigger value="online_payments">Online Payments</TabsTrigger>
          <TabsTrigger value="tally_export">Tally Export</TabsTrigger>
        </TabsList>

        {/* ================= TAB 1: STRUCTURES ================= */}
        <TabsContent value="structures" activeTab={activeTab}>
          <div className="space-y-6">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <Layers className="h-4 w-4 text-indigo-600" /> Fee Heads Master
                </h3>
                <Button size="sm" onClick={() => setIsHeadOpen(true)} className="flex items-center gap-1.5">
                  <Plus className="h-3.5 w-3.5" /> Add Fee Head
                </Button>
              </div>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-4">Fee Head</th>
                      <th className="p-4">Amount</th>
                      <th className="p-4">Frequency</th>
                      <th className="p-4">GST Treatment</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {heads.map((h) => <tr key={h.id}>
                        <td className="p-4 font-extrabold text-slate-800">{h.name}</td>
                        <td className="p-4">{inr(h.amount)}</td>
                        <td className="p-4">
                          <Badge variant={h.frequency === "Monthly" ? "info" : h.frequency === "Quarterly" ? "secondary" : "default"}>{h.frequency}</Badge>
                        </td>
                        <td className="p-4">
                          <Badge variant={h.taxable ? "warning" : "success"}>{h.taxable ? "Taxable 18%" : "Exempt"}</Badge>
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="space-y-3">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest px-1">Class Fee Templates</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                {templates.map((tpl) => <div key={tpl.id} className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">Template</p>
                        <h4 className="text-base font-black text-slate-800 mt-0.5">{tpl.grade}</h4>
                      </div>
                      <Badge variant="secondary">{tpl.applicable} students</Badge>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {tpl.heads.map((nm) => <span key={nm} className="text-[10px] font-bold bg-slate-50 border border-slate-200 text-slate-600 px-2 py-1 rounded-lg">{nm}</span>)}
                    </div>
                    <div className="flex justify-between items-center pt-2 border-t border-slate-100">
                      <div>
                        <p className="text-[9px] uppercase tracking-widest font-extrabold text-slate-400">Composed Total</p>
                        <p className="text-sm font-black text-indigo-700">{inr(headTotal(tpl.heads))}</p>
                      </div>
                      <Button size="sm" variant="outline" onClick={() => openTemplateEditor(tpl)}>Edit Template</Button>
                    </div>
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 2: CONCESSIONS ================= */}
        <TabsContent value="concessions" activeTab={activeTab}>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Percent className="h-4 w-4 text-indigo-600" /> Concession Policies
              </h3>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-3.5">Policy</th>
                      <th className="p-3.5">Waiver</th>
                      <th className="p-3.5">Eligibility Criteria</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {concessions.policies.map((p) => <tr key={p.id}>
                        <td className="p-3.5 font-extrabold text-slate-800">{p.name}</td>
                        <td className="p-3.5"><Badge variant={p.percent === "100%" ? "danger" : p.percent === "Custom" ? "default" : "info"}>{p.percent}</Badge></td>
                        <td className="p-3.5 text-slate-500 leading-relaxed">{p.criteria}</td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
              <p className="text-[10px] text-slate-400 italic leading-normal">Maker-checker: Accountant proposes a concession, Principal (or Super Admin) counter-signs. No single-hand write-offs.</p>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-emerald-600" /> Approval Queue
              </h3>
              <div className="space-y-3">
                {concessions.queue.map((q) => <div key={q.id} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl space-y-2.5">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="text-xs font-black text-slate-800">{q.student}</p>
                        <p className="text-[10px] text-slate-400 font-bold mt-0.5">{q.type} · proposed by {q.requestedBy}</p>
                      </div>
                      <Badge variant={q.status === "Approved" ? "success" : q.status === "Rejected" ? "danger" : "warning"}>{q.status}</Badge>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm font-black text-indigo-700">{inr(q.amount)} <span className="text-[9px] text-slate-400 uppercase font-extrabold">waiver / annum</span></p>
                      {q.status === "Pending" && (canApprove ? <div className="flex gap-2">
                            <button onClick={() => handleConcessionDecision(q.id, "Rejected")} className="p-1 px-3 border border-rose-200 text-rose-600 hover:bg-rose-50 text-[10px] uppercase rounded-lg font-bold transition cursor-pointer flex items-center gap-1">
                              <XCircle className="h-3 w-3" /> Reject
                            </button>
                            <button onClick={() => handleConcessionDecision(q.id, "Approved")} className="p-1 px-3 bg-emerald-600 text-white hover:bg-emerald-700 text-[10px] uppercase rounded-lg font-bold transition cursor-pointer flex items-center gap-1">
                              <CheckCircle2 className="h-3 w-3" /> Approve
                            </button>
                          </div> : <Badge variant="info">Awaiting Principal sign-off</Badge>)}
                    </div>
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 3: DEFAULTERS ================= */}
        <TabsContent value="defaulters" activeTab={activeTab}>
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-slate-400 tracking-widest">0 – 30 days</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(buckets["0-30"])}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Soft nudge window</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-amber-500 tracking-widest">31 – 60 days</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(buckets["31-60"])}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">SMS notice band</p>
              </div>
              <div className="bg-white border border-slate-100 p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-rose-500 tracking-widest">61 – 90 days</span>
                <h4 className="text-xl font-black text-slate-800 mt-1">{inr(buckets["61-90"])}</h4>
                <p className="text-xs text-slate-400 mt-1 font-semibold">Call-list escalation</p>
              </div>
              <div className="bg-rose-950 text-white p-5 rounded-3xl">
                <span className="text-[10px] uppercase font-extrabold text-rose-300 tracking-widest">90+ days</span>
                <h4 className="text-xl font-black text-white mt-1">{inr(buckets["90+"])}</h4>
                <p className="text-xs text-rose-200 mt-1 font-semibold">Principal meeting letters</p>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
                <div>
                  <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <FileWarning className="h-4 w-4 text-rose-600" /> Defaulter Register
                  </h3>
                  <p className="text-xs text-slate-400 font-semibold mt-1">{defaulters.length} families owe {inr(totalDue)} in total. Escalation ladder: WhatsApp → SMS → Call → Meeting letter.</p>
                </div>
                <Button variant="danger" size="sm" onClick={handleBulkEscalate} disabled={bulkProgress !== null} className="flex items-center gap-1.5">
                  <Zap className="h-3.5 w-3.5" /> {bulkProgress !== null ? `Queuing… ${bulkProgress}%` : "Escalate All Stage-1"}
                </Button>
              </div>
              {bulkProgress !== null && <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div className="h-full bg-rose-500 rounded-full transition-all duration-200" style={{ width: `${bulkProgress}%` }} />
                </div>}
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-4">Student</th>
                      <th className="p-4">Invoices</th>
                      <th className="p-4">Total Due</th>
                      <th className="p-4">Days Overdue</th>
                      <th className="p-4">Last Reminder</th>
                      <th className="p-4 text-center">Next Escalation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {defaulters.slice(0, 15).map((d) => {
                      const log = reminderLog[d.studentId];
                      const stage = log?.stage ?? 0;
                      const icons = [MessageCircle, Send, Phone, FileWarning];
                      const NextIcon = icons[Math.min(stage, 3)];
                      return <tr key={d.studentId}>
                          <td className="p-4">
                            <p className="font-extrabold text-slate-800">{d.name}</p>
                            <p className="text-[10px] text-slate-400 font-bold mt-0.5">{d.grade}</p>
                          </td>
                          <td className="p-4">{d.count}</td>
                          <td className="p-4 text-rose-600 font-extrabold">{inr(d.due)}</td>
                          <td className="p-4">
                            <Badge variant={d.maxDays > 90 ? "danger" : d.maxDays > 60 ? "warning" : "default"}>{d.maxDays}d</Badge>
                          </td>
                          <td className="p-4 text-slate-500">{log ? `${log.last} · ${log.date}` : "—"}</td>
                          <td className="p-4 text-center">
                            {stage >= LADDER.length ? <Badge variant="danger">Ladder exhausted</Badge> : <button
                              onClick={() => advanceReminder(d.studentId, d.name)}
                              className="inline-flex items-center gap-1.5 bg-indigo-50 border border-indigo-100 hover:bg-indigo-600 hover:text-white text-indigo-700 text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer"
                            >
                                <NextIcon className="h-3 w-3" /> {LADDER[stage]}
                              </button>}
                          </td>
                        </tr>;
                    })}
                  </tbody>
                </table>
              </div>
              <p className="text-[10px] text-slate-400 italic leading-normal">Aging buckets are simulated (stable per invoice) for the mockup; escalation states persist in LocalStorage and feed the audit log.</p>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 4: ONLINE PAYMENTS ================= */}
        <TabsContent value="online_payments" activeTab={activeTab}>
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {gateways.map((g) => <div key={g.id} className={`p-5 rounded-3xl border flex flex-col gap-3 ${g.connected ? "bg-indigo-900 text-white border-indigo-900" : "bg-white border-slate-100"}`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className={`text-base font-black ${g.connected ? "text-white" : "text-slate-800"}`}>{g.name}</h4>
                      <p className={`text-[10px] font-bold mt-0.5 ${g.connected ? "text-indigo-200" : "text-slate-400"}`}>{g.modes}</p>
                    </div>
                    <div className={`p-2.5 rounded-2xl ${g.connected ? "bg-white/10 text-indigo-200" : "bg-slate-50 text-slate-500"}`}>
                      <CreditCard className="h-5 w-5" />
                    </div>
                  </div>
                  <div className="flex justify-between items-center pt-2 mt-auto">
                    <div>
                      <p className={`text-[9px] uppercase tracking-widest font-extrabold ${g.connected ? "text-indigo-300" : "text-slate-400"}`}>MDR</p>
                      <p className={`text-xs font-black ${g.connected ? "text-white" : "text-slate-700"}`}>{g.fee}</p>
                    </div>
                    <button
                      onClick={() => toggleGateway(g.id)}
                      className={`text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer ${g.connected ? "bg-white/15 text-white hover:bg-white/25" : "bg-slate-900 text-white hover:bg-slate-800"}`}
                    >
                      {g.connected ? "Connected ✓" : "Connect"}
                    </button>
                  </div>
                </div>)}
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
                <div>
                  <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <Repeat className="h-4 w-4 text-indigo-600" /> UPI AutoPay Mandates
                  </h3>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Recurring e-mandates registered on the NPCI AutoPay rails (mock).</p>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={handleSettlement} disabled={settling} className="flex items-center gap-1.5">
                    <Landmark className="h-3.5 w-3.5" /> {settling ? "Reconciling…" : "Simulate Gateway Settlement"}
                  </Button>
                  <Button size="sm" onClick={() => { setMndStudent(students[0]?.id || ""); setIsMandateOpen(true); }} className="flex items-center gap-1.5">
                    <Plus className="h-3.5 w-3.5" /> Create Mandate
                  </Button>
                </div>
              </div>
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-left text-slate-700">
                  <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                    <tr>
                      <th className="p-4">Parent / Payer</th>
                      <th className="p-4">Student</th>
                      <th className="p-4">Monthly Amount</th>
                      <th className="p-4">Mandate Ref</th>
                      <th className="p-4">Status</th>
                      <th className="p-4">Next Debit</th>
                      <th className="p-4 text-center">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {mandates.map((m) => <tr key={m.id}>
                        <td className="p-4 font-extrabold text-slate-800">{m.parent}</td>
                        <td className="p-4">{m.student}</td>
                        <td className="p-4 font-extrabold text-indigo-700">{inr(m.amount)}</td>
                        <td className="p-4 text-slate-500 font-mono text-[10px]">{m.ref}</td>
                        <td className="p-4"><Badge variant={m.status === "Active" ? "success" : "warning"}>{m.status}</Badge></td>
                        <td className="p-4">{m.nextDebit}</td>
                        <td className="p-4 text-center">
                          <button onClick={() => toggleMandate(m.id)} className="bg-slate-50 border border-slate-200 hover:bg-slate-900 hover:text-white text-slate-600 text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer">
                            {m.status === "Active" ? "Pause" : "Resume"}
                          </button>
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= TAB 5: TALLY EXPORT ================= */}
        <TabsContent value="tally_export" activeTab={activeTab}>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <FileDown className="h-4 w-4 text-indigo-600" /> Accounting Hand-off
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input label="From Date" type="date" value={expFrom} onChange={(e) => setExpFrom(e.target.value)} />
                  <Input label="To Date" type="date" value={expTo} onChange={(e) => setExpTo(e.target.value)} />
                </div>
                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-left text-slate-700">
                    <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
                      <tr>
                        <th className="p-3.5">Tally Voucher Type</th>
                        <th className="p-3.5">ERP Source Ledger</th>
                        <th className="p-3.5">Mapping</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      <tr><td className="p-3.5 font-extrabold text-slate-800">Receipt</td><td className="p-3.5">Fee Collection (Tuition / Transport / Hostel)</td><td className="p-3.5"><Badge variant="success">Auto</Badge></td></tr>
                      <tr><td className="p-3.5 font-extrabold text-slate-800">Journal</td><td className="p-3.5">Concessions &amp; Scholarships write-off</td><td className="p-3.5"><Badge variant="success">Auto</Badge></td></tr>
                      <tr><td className="p-3.5 font-extrabold text-slate-800">Payment</td><td className="p-3.5">Gateway MDR &amp; settlement charges</td><td className="p-3.5"><Badge variant="info">On settlement</Badge></td></tr>
                      <tr><td className="p-3.5 font-extrabold text-slate-800">Credit Note</td><td className="p-3.5">Refunds &amp; TC-time adjustments</td><td className="p-3.5"><Badge variant="default">Manual review</Badge></td></tr>
                    </tbody>
                  </table>
                </div>
                <div className="flex flex-wrap gap-2.5 pt-2 border-t border-slate-100">
                  <Button onClick={handleTallyXML} className="flex items-center gap-1.5">
                    <FileDown className="h-4 w-4" /> Export Tally XML
                  </Button>
                  <Button variant="outline" onClick={handleCSVDayBook} className="flex items-center gap-1.5">
                    <FileSpreadsheet className="h-4 w-4" /> Export CSV Day Book
                  </Button>
                </div>
              </div>

              <div className="bg-amber-50 border border-amber-200 p-5 rounded-3xl space-y-2.5">
                <h4 className="text-xs font-bold text-amber-900 uppercase tracking-widest flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4" /> GST Note
                </h4>
                <p className="text-xs text-amber-950 font-semibold leading-relaxed">
                  Core educational services (tuition, hostel, transport for students) are <span className="font-black">GST-exempt</span> under Notification 12/2017.
                  Taxable store sales — uniforms, stationery kits, canteen à-la-carte — attract 5–18% GST and export under a separate sales ledger with HSN codes.
                </p>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Clock className="h-4 w-4 text-slate-500" /> Export History
              </h3>
              {exports.length === 0 && <p className="text-xs text-slate-400 font-semibold italic">No exports yet. Generated files will be listed here.</p>}
              <div className="space-y-3">
                {exports.map((ex) => <div key={ex.id} className="p-3.5 bg-slate-50 border border-slate-150 rounded-2xl">
                    <div className="flex justify-between items-start">
                      <p className="text-xs font-black text-slate-800">{ex.type}</p>
                      <Badge variant="info">{ex.count} inv</Badge>
                    </div>
                    <p className="text-[10px] text-slate-400 font-bold mt-1 break-all">{ex.filename}</p>
                    <p className="text-[10px] text-slate-400 font-semibold mt-0.5">{ex.date} · by {ex.by}</p>
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>

      {/* ================= DIALOG: Add Fee Head ================= */}
      <Dialog isOpen={isHeadOpen} onClose={() => setIsHeadOpen(false)} title="Add Fee Head">
        <form onSubmit={handleAddHead} className="space-y-4 pt-1">
          <Input label="Fee Head Name" value={headName} onChange={(e) => setHeadName(e.target.value)} placeholder="e.g. Smart Class Levy" required />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Amount (₹)" type="number" value={headAmount} onChange={(e) => setHeadAmount(e.target.value)} required />
            <Select
              label="Frequency"
              options={[
                { label: "Monthly", value: "Monthly" },
                { label: "Quarterly", value: "Quarterly" },
                { label: "Annual", value: "Annual" }
              ]}
              value={headFreq}
              onChange={(e) => setHeadFreq(e.target.value)}
            />
          </div>
          <Select
            label="GST Applicable?"
            options={[{ label: "No — Exempt educational service", value: "No" }, { label: "Yes — Taxable @ 18%", value: "Yes" }]}
            value={headTaxable}
            onChange={(e) => setHeadTaxable(e.target.value)}
          />
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsHeadOpen(false)}>Cancel</Button>
            <Button type="submit">Save Fee Head</Button>
          </div>
        </form>
      </Dialog>

      {/* ================= DIALOG: Edit Template ================= */}
      <Dialog isOpen={!!editTemplate} onClose={() => setEditTemplate(null)} title={`Edit Template — ${editTemplate?.grade || ""}`}>
        {editTemplate && <div className="space-y-4 pt-1">
            <p className="text-xs text-slate-400 font-semibold">Toggle the fee heads composed into this class template. Total recomputes live.</p>
            <div className="space-y-2">
              {heads.map((h) => {
                const on = tplHeads.includes(h.name);
                return <button
                  key={h.id}
                  onClick={() => toggleTplHead(h.name)}
                  className={`w-full flex justify-between items-center p-3 rounded-xl border text-xs font-bold transition cursor-pointer ${on ? "bg-indigo-50 border-indigo-200 text-indigo-800" : "bg-slate-50 border-slate-200 text-slate-500 hover:bg-slate-100"}`}
                >
                    <span className="flex items-center gap-2">
                      {on ? <CheckCircle2 className="h-4 w-4 text-indigo-600" /> : <XCircle className="h-4 w-4 text-slate-300" />}
                      {h.name} <span className="text-[9px] uppercase text-slate-400">({h.frequency})</span>
                    </span>
                    <span>{inr(h.amount)}</span>
                  </button>;
              })}
            </div>
            <div className="flex justify-between items-center bg-slate-900 text-white p-4 rounded-2xl">
              <span className="text-[10px] uppercase tracking-widest font-extrabold text-slate-300">Template Total</span>
              <span className="text-lg font-black">{inr(headTotal(tplHeads))}</span>
            </div>
            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setEditTemplate(null)}>Cancel</Button>
              <Button onClick={handleSaveTemplate}>Save Template</Button>
            </div>
          </div>}
      </Dialog>

      {/* ================= DIALOG: Create Mandate ================= */}
      <Dialog isOpen={isMandateOpen} onClose={() => setIsMandateOpen(false)} title="Create UPI AutoPay Mandate">
        <form onSubmit={handleCreateMandate} className="space-y-4 pt-1">
          <Select
            label="Student"
            options={students.slice(0, 40).map((s) => ({ label: `${s.name} — ${s.grade} (${s.admissionNumber})`, value: s.id }))}
            value={mndStudent}
            onChange={(e) => setMndStudent(e.target.value)}
          />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Monthly Amount (₹)" type="number" value={mndAmount} onChange={(e) => setMndAmount(e.target.value)} required />
            <Select
              label="Debit Day of Month"
              options={["1", "5", "7", "10", "15"].map((d) => ({ label: `Day ${d}`, value: d }))}
              value={mndDay}
              onChange={(e) => setMndDay(e.target.value)}
            />
          </div>
          <p className="text-[10px] text-slate-400 italic leading-normal">Parent will receive a UPI collect request on their PSP app to authorise the recurring mandate (₹1 verification debit).</p>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsMandateOpen(false)}>Cancel</Button>
            <Button type="submit">Register Mandate</Button>
          </div>
        </form>
      </Dialog>

      {/* ================= DIALOG: Settlement Summary ================= */}
      <Dialog isOpen={!!settlement} onClose={() => setSettlement(null)} title="Gateway Settlement Advice" maxWidth="max-w-md">
        {settlement && <div className="space-y-4 pt-1">
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-150 text-xs font-semibold text-slate-600 space-y-1.5">
              <p>Settlement UTR: <span className="font-mono text-slate-800 font-bold">{settlement.utr}</span></p>
              <p>Cycle: <span className="text-slate-800 font-bold">T+1 · {today()}</span></p>
            </div>
            <div className="space-y-2">
              {settlement.lines.map((ln, i) => <div key={i} className="flex justify-between items-center text-xs font-semibold text-slate-700 p-2.5 bg-white border border-slate-100 rounded-xl">
                  <span>{ln.student} <span className="text-slate-400">· {ln.feeType}</span></span>
                  <span className="font-black text-emerald-700">{inr(ln.amount)}</span>
                </div>)}
            </div>
            <div className="space-y-2 text-xs font-semibold text-slate-700 border-t border-dashed border-slate-200 pt-3">
              <div className="flex justify-between"><span className="text-slate-400">Gross collections</span><span className="font-extrabold">{inr(settlement.gross)}</span></div>
              <div className="flex justify-between text-rose-600"><span>Gateway fee (0.9%)</span><span className="font-extrabold">− {inr(settlement.fee)}</span></div>
              <div className="flex justify-between text-base text-slate-900"><span className="font-black">Net credited</span><span className="font-black">{inr(settlement.net)}</span></div>
            </div>
            <div className="flex justify-end pt-3 border-t border-slate-100">
              <Button onClick={() => setSettlement(null)}>Close Advice</Button>
            </div>
          </div>}
      </Dialog>

    </div>;
}
