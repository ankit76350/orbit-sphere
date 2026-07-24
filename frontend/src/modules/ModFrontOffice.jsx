/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, getStaff, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, Tabs, TabsList, TabsTrigger, TabsContent, useToast } from "../components/ui";
import {
  Phone,
  PhoneIncoming,
  PhoneOutgoing,
  Mail,
  Send,
  AlertTriangle,
  Award,
  Plus,
  Printer,
  ArrowRight,
  Inbox,
  FileBadge,
  Copy,
  Eye,
  MessageSquareWarning
} from "lucide-react";

const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));

const CALLS_KEY = "erp_call_log";
const POSTAL_KEY = "erp_postal";
const GRIEV_KEY = "erp_grievances";
const CERT_KEY = "erp_certificates";

const today = () => new Date().toISOString().slice(0, 10);
const nowTime = () => new Date().toTimeString().slice(0, 5);
const daysOpen = (dateStr) => Math.max(0, Math.floor((Date.now() - new Date(dateStr).getTime()) / 864e5));

const TODAY = today();

const CALLS_SEED = [
  { id: "call-1", date: TODAY, time: "09:15", caller: "Rajesh Verma", phone: "+91 98220 44121", type: "Incoming", purpose: "Admission Enquiry", assignedToName: "Front Desk", followUp: TODAY, status: "Open", sentToCRM: false },
  { id: "call-2", date: TODAY, time: "10:05", caller: "Sunita Iyer", phone: "+91 99860 77310", type: "Incoming", purpose: "Fee Query", assignedToName: "Accounts Office", followUp: "", status: "Closed", sentToCRM: false },
  { id: "call-3", date: TODAY, time: "11:42", caller: "Transport Desk", phone: "+91 90080 21455", type: "Outgoing", purpose: "Transport", assignedToName: "Transport Incharge", followUp: "", status: "Open", sentToCRM: false },
  { id: "call-4", date: "2026-07-03", time: "15:20", caller: "Mohd. Faizan Khan", phone: "+91 97400 88213", type: "Incoming", purpose: "Admission Enquiry", assignedToName: "Admissions Cell", followUp: "2026-07-06", status: "Open", sentToCRM: true },
  { id: "call-5", date: "2026-07-02", time: "12:10", caller: "Deepa Nair", phone: "+91 98450 33902", type: "Incoming", purpose: "Complaint", assignedToName: "Vice Principal", followUp: "2026-07-05", status: "Closed", sentToCRM: false }
];

const POSTAL_SEED = [
  { id: "post-1", direction: "Inward", date: TODAY, refNo: "IN/2026/041", party: "CBSE Regional Office, Bengaluru", subject: "Board exam centre allotment circular", mode: "Post", handlerName: "Meena Kulkarni", status: "Received" },
  { id: "post-2", direction: "Inward", date: "2026-07-03", refNo: "IN/2026/040", party: "Karnataka Textbook Society", subject: "Grade 6-8 textbook consignment invoice", mode: "Courier", handlerName: "Meena Kulkarni", status: "Received" },
  { id: "post-3", direction: "Outward", date: TODAY, refNo: "OUT/2026/027", party: "District Education Officer, Bengaluru South", subject: "Staff statistics return for AY 2026-27", mode: "Post", handlerName: "Suresh Gowda", status: "Dispatched" },
  { id: "post-4", direction: "Outward", date: "2026-07-02", refNo: "OUT/2026/026", party: "M/s Royal Garments, Chickpet", subject: "Purchase order — winter uniform blazers", mode: "Courier", handlerName: "Suresh Gowda", status: "In Transit" }
];

const GRIEV_SEED = [
  { id: "GRV-101", date: "2026-06-24", raisedBy: "Parent", raisedName: "Anand Krishnamurthy", category: "Transport", description: "Route Beta bus arriving 25 minutes late at Lakeside Junction for the past week, children missing first period.", severity: "High", assignedToName: "Transport Incharge", status: "In Review", resolutionNote: "" },
  { id: "GRV-102", date: "2026-07-01", raisedBy: "Staff", raisedName: "Kavitha Rao (Teacher)", category: "Safety", description: "Loose electrical conduit near the chemistry lab corridor on second floor; needs urgent estate attention.", severity: "High", assignedToName: "Estate Manager", status: "Open", resolutionNote: "" },
  { id: "GRV-103", date: TODAY, raisedBy: "Parent", raisedName: "Shalini Menon", category: "Fee", description: "Sibling discount not reflected in Term 1 tuition invoice despite approval mail from accounts.", severity: "Medium", assignedToName: "Accounts Office", status: "Open", resolutionNote: "" },
  { id: "GRV-104", date: "2026-06-18", raisedBy: "Student", raisedName: "Aarav Shetty (Grade 9)", category: "Academic", description: "Library reference section closed during study hall hours three days in a row.", severity: "Low", assignedToName: "Librarian", status: "Resolved", resolutionNote: "Library timings extended to 6 PM; additional assistant rostered for study hall." }
];

const CERT_TYPES = ["Transfer Certificate", "Bonafide", "Character", "Migration", "Fee Clearance NOC"];
const CERT_PREFIX = { "Transfer Certificate": "TC", "Bonafide": "BON", "Character": "CHR", "Migration": "MIG", "Fee Clearance NOC": "NOC" };

const CERT_SEED = [
  { id: "cert-1", serial: "TC/2026/007", date: "2026-06-20", studentDocsId: "student-9", studentName: "Ishaan Patel", grade: "Grade 8", admissionNo: "STJ2025-1009", type: "Transfer Certificate", reason: "Family relocating to Pune", issuedBy: "Principal", duplicate: false, lastClass: "Grade 8", conduct: "Good" },
  { id: "cert-2", serial: "BON/2026/014", date: "2026-06-28", studentDocsId: "student-14", studentName: "Ananya Reddy", grade: "Grade 7", admissionNo: "STJ2025-1014", type: "Bonafide", reason: "Bank account opening (minor)", issuedBy: "Office Superintendent", duplicate: false }
];

const GRIEV_FLOW = { "Open": "In Review", "In Review": "Resolved", "Resolved": "Closed" };

export default function ModFrontOffice({ user }) {
  const { addToast } = useToast();
  const [vTab, setVTab] = useState("dashboard");
  const students = getStudents();
  const staff = getStaff();

  const [calls, setCalls] = useState(() => loadLS(CALLS_KEY, CALLS_SEED));
  const [postal, setPostal] = useState(() => loadLS(POSTAL_KEY, POSTAL_SEED));
  const [grievances, setGrievances] = useState(() => loadLS(GRIEV_KEY, GRIEV_SEED));
  const [certs, setCerts] = useState(() => loadLS(CERT_KEY, CERT_SEED));

  /* ---------- dashboard aggregates ---------- */
  const callsToday = calls.filter((c) => c.date === TODAY).length;
  const postalInToday = postal.filter((p) => p.direction === "Inward" && p.date === TODAY).length;
  const postalOutToday = postal.filter((p) => p.direction === "Outward" && p.date === TODAY).length;
  const openGrievances = grievances.filter((g) => g.status === "Open" || g.status === "In Review").length;

  const activityFeed = [
    ...calls.map((c) => ({ date: c.date, time: c.time || "—", icon: "call", text: `${c.type} call from ${c.caller} — ${c.purpose}`, tag: c.status })),
    ...postal.map((p) => ({ date: p.date, time: "—", icon: "post", text: `${p.direction} mail ${p.refNo}: ${p.subject}`, tag: p.status })),
    ...grievances.map((g) => ({ date: g.date, time: "—", icon: "grv", text: `${g.id} raised by ${g.raisedName} (${g.category})`, tag: g.status })),
    ...certs.map((c) => ({ date: c.date, time: "—", icon: "cert", text: `${c.type} ${c.serial} issued to ${c.studentName}`, tag: c.duplicate ? "Duplicate" : "Original" }))
  ].sort((a, b) => (b.date + b.time).localeCompare(a.date + a.time)).slice(0, 8);

  /* ---------- calls ---------- */
  const [isCallOpen, setIsCallOpen] = useState(false);
  const [cCaller, setCCaller] = useState("");
  const [cPhone, setCPhone] = useState("");
  const [cType, setCType] = useState("Incoming");
  const [cPurpose, setCPurpose] = useState("Admission Enquiry");
  const [cAssigned, setCAssigned] = useState("");
  const [cFollowUp, setCFollowUp] = useState("");

  const handleLogCall = (e) => {
    e.preventDefault();
    if (!cCaller.trim() || !cPhone.trim()) {
      addToast("Missing Details", "Caller name and phone number are required.", "error");
      return;
    }
    const entry = {
      id: `call-${Date.now()}`,
      date: today(),
      time: nowTime(),
      caller: cCaller.trim(),
      phone: cPhone.trim(),
      type: cType,
      purpose: cPurpose,
      assignedToName: cAssigned || staff[0]?.name || "Front Desk",
      followUp: cFollowUp,
      status: "Open",
      sentToCRM: false
    };
    const updated = [entry, ...calls];
    setCalls(updated);
    saveLS(CALLS_KEY, updated);
    logAction(user.id, user.name, user.role, "Front Office Call Logged", `${cType} call from ${entry.caller} (${entry.phone}) — ${cPurpose}, assigned to ${entry.assignedToName}`);
    addToast("Call Logged", `Recorded ${cType.toLowerCase()} call from ${entry.caller}.`, "success");
    setIsCallOpen(false);
    setCCaller(""); setCPhone(""); setCFollowUp("");
  };

  const handleSendToCRM = (id) => {
    const updated = calls.map((c) => c.id === id ? { ...c, sentToCRM: true } : c);
    setCalls(updated);
    saveLS(CALLS_KEY, updated);
    const call = updated.find((c) => c.id === id);
    logAction(user.id, user.name, user.role, "Call Pushed to CRM", `Admission enquiry from ${call?.caller} (${call?.phone}) forwarded to Inquiry CRM pipeline`);
    addToast("Pushed to Inquiry CRM pipeline", `${call?.caller}'s enquiry is now with the admissions team.`, "success");
  };

  const toggleCallStatus = (id) => {
    const updated = calls.map((c) => c.id === id ? { ...c, status: c.status === "Open" ? "Closed" : "Open" } : c);
    setCalls(updated);
    saveLS(CALLS_KEY, updated);
    addToast("Status Updated", "Call record status toggled.", "info");
  };

  /* ---------- postal ---------- */
  const [postalView, setPostalView] = useState("Inward");
  const [isPostOpen, setIsPostOpen] = useState(false);
  const [pParty, setPParty] = useState("");
  const [pSubject, setPSubject] = useState("");
  const [pMode, setPMode] = useState("Courier");
  const [pHandlerName, setPHandlerName] = useState("");

  const nextRefNo = (direction) => {
    const prefix = direction === "Inward" ? "IN" : "OUT";
    const nums = postal.filter((p) => p.direction === direction).map((p) => parseInt(String(p.refNo).split("/").pop(), 10) || 0);
    const next = (nums.length > 0 ? Math.max(...nums) : 0) + 1;
    return `${prefix}/2026/${String(next).padStart(3, "0")}`;
  };

  const handleAddPostal = (e) => {
    e.preventDefault();
    if (!pParty.trim() || !pSubject.trim()) {
      addToast("Missing Details", "Party name and subject are required.", "error");
      return;
    }
    const entry = {
      id: `post-${Date.now()}`,
      direction: postalView,
      date: today(),
      refNo: nextRefNo(postalView),
      party: pParty.trim(),
      subject: pSubject.trim(),
      mode: pMode,
      handlerName: pHandlerName || staff[0]?.name || "Front Desk",
      status: postalView === "Inward" ? "Received" : "Dispatched"
    };
    const updated = [entry, ...postal];
    setPostal(updated);
    saveLS(POSTAL_KEY, updated);
    logAction(user.id, user.name, user.role, "Postal Entry Registered", `${postalView} ${entry.refNo} (${pMode}) — ${entry.subject}`);
    addToast("Entry Registered", `${entry.refNo} added to the ${postalView.toLowerCase()} register.`, "success");
    setIsPostOpen(false);
    setPParty(""); setPSubject("");
  };

  /* ---------- grievances ---------- */
  const [isGrvOpen, setIsGrvOpen] = useState(false);
  const [gRaisedBy, setGRaisedBy] = useState("Parent");
  const [gRaisedName, setGRaisedName] = useState("");
  const [gCategory, setGCategory] = useState("Academic");
  const [gDesc, setGDesc] = useState("");
  const [gSeverity, setGSeverity] = useState("Medium");
  const [gAssigned, setGAssigned] = useState("");
  const [resolveTarget, setResolveTarget] = useState(null);
  const [resolutionNote, setResolutionNote] = useState("");

  const handleRaiseGrievance = (e) => {
    e.preventDefault();
    if (!gRaisedName.trim() || !gDesc.trim()) {
      addToast("Missing Details", "Complainant name and description are required.", "error");
      return;
    }
    const entry = {
      id: `GRV-${100 + grievances.length + 1}`,
      date: today(),
      raisedBy: gRaisedBy,
      raisedName: gRaisedName.trim(),
      category: gCategory,
      description: gDesc.trim(),
      severity: gSeverity,
      assignedToName: gAssigned || staff[0]?.name || "Vice Principal",
      status: "Open",
      resolutionNote: ""
    };
    const updated = [entry, ...grievances];
    setGrievances(updated);
    saveLS(GRIEV_KEY, updated);
    logAction(user.id, user.name, user.role, "Grievance Raised", `${entry.id} (${gCategory}, ${gSeverity}) by ${entry.raisedName}, assigned to ${entry.assignedToName}`);
    addToast("Grievance Registered", `${entry.id} logged and routed to ${entry.assignedToName}.`, "success");
    setIsGrvOpen(false);
    setGRaisedName(""); setGDesc("");
  };

  const advanceGrievance = (grv) => {
    const next = GRIEV_FLOW[grv.status];
    if (!next) return;
    if (next === "Resolved") {
      setResolveTarget(grv);
      setResolutionNote("");
      return;
    }
    const updated = grievances.map((g) => g.id === grv.id ? { ...g, status: next } : g);
    setGrievances(updated);
    saveLS(GRIEV_KEY, updated);
    logAction(user.id, user.name, user.role, "Grievance Status Advanced", `${grv.id} moved from ${grv.status} to ${next}`);
    addToast("Workflow Advanced", `${grv.id} is now "${next}".`, "success");
  };

  const handleResolveSubmit = (e) => {
    e.preventDefault();
    if (!resolutionNote.trim()) {
      addToast("Resolution Note Required", "A closing note must be recorded before resolving.", "error");
      return;
    }
    const updated = grievances.map((g) => g.id === resolveTarget.id ? { ...g, status: "Resolved", resolutionNote: resolutionNote.trim() } : g);
    setGrievances(updated);
    saveLS(GRIEV_KEY, updated);
    logAction(user.id, user.name, user.role, "Grievance Resolved", `${resolveTarget.id} resolved: ${resolutionNote.trim()}`);
    addToast("Grievance Resolved", `${resolveTarget.id} marked resolved with note on file.`, "success");
    setResolveTarget(null);
  };

  /* ---------- certificates ---------- */
  const [isCertOpen, setIsCertOpen] = useState(false);
  const [certType, setCertType] = useState("Transfer Certificate");
  const [certStudentDocsId, setCertStudentDocsId] = useState(students[0]?.id || "");
  const [certReason, setCertReason] = useState("");
  const [tcLastClass, setTcLastClass] = useState("");
  const [tcDuesCleared, setTcDuesCleared] = useState(false);
  const [tcConduct, setTcConduct] = useState("Good");
  const [duplicateOf, setDuplicateOf] = useState(null);
  const [previewCert, setPreviewCert] = useState(null);

  const nextSerial = (type) => {
    const prefix = CERT_PREFIX[type];
    const nums = certs.filter((c) => c.type === type).map((c) => parseInt(String(c.serial).split("/").pop(), 10) || 0);
    const base = type === "Transfer Certificate" ? 7 : type === "Bonafide" ? 14 : 0;
    const next = Math.max(base, ...(nums.length > 0 ? nums : [0])) + 1;
    return `${prefix}/2026/${String(next).padStart(3, "0")}`;
  };

  const openIssueDialog = (dupSource) => {
    if (dupSource) {
      setDuplicateOf(dupSource);
      setCertType(dupSource.type);
      setCertStudentDocsId(dupSource.studentDocsId);
      setCertReason(`Duplicate of ${dupSource.serial} — original reported lost`);
      setTcLastClass(dupSource.lastClass || "");
      setTcConduct(dupSource.conduct || "Good");
      setTcDuesCleared(true);
    } else {
      setDuplicateOf(null);
      setCertReason("");
      setTcLastClass("");
      setTcDuesCleared(false);
      setTcConduct("Good");
    }
    setIsCertOpen(true);
  };

  const handleIssueCert = (e) => {
    e.preventDefault();
    const student = students.find((s) => s.id === certStudentDocsId);
    if (!student) {
      addToast("Student Missing", "Select a valid student from the roster.", "error");
      return;
    }
    if (!certReason.trim()) {
      addToast("Reason Required", "Record the reason / purpose for issuing this certificate.", "error");
      return;
    }
    if (certType === "Transfer Certificate" && !tcDuesCleared) {
      addToast("Issue Blocked", "TC cannot be issued until all fee dues are certified as cleared.", "error");
      return;
    }
    const entry = {
      id: `cert-${Date.now()}`,
      serial: nextSerial(certType),
      date: today(),
      studentDocsId: student.id,
      studentName: student.name,
      grade: student.grade,
      admissionNo: student.admissionNo,
      type: certType,
      reason: certReason.trim(),
      issuedBy: user.name,
      duplicate: !!duplicateOf,
      lastClass: certType === "Transfer Certificate" ? (tcLastClass || student.grade) : undefined,
      conduct: certType === "Transfer Certificate" ? tcConduct : undefined
    };
    const updated = [entry, ...certs];
    setCerts(updated);
    saveLS(CERT_KEY, updated);
    logAction(user.id, user.name, user.role, "Certificate Issued", `${entry.duplicate ? "DUPLICATE " : ""}${certType} ${entry.serial} issued to ${student.name} (${student.admissionNo}) — ${entry.reason}`);
    addToast("Certificate Issued", `${entry.serial} recorded in the issue register.`, "success");
    setIsCertOpen(false);
    setPreviewCert(entry);
  };

  const certBodyText = (c) => {
    if (c.type === "Transfer Certificate") return `This is to certify that ${c.studentName} (Admission No. ${c.admissionNo}) was a bonafide student of this institution and studied up to ${c.lastClass || c.grade}. All dues to the institution stand cleared. Their conduct during the period of study was found to be ${(c.conduct || "Good").toLowerCase()}. This Transfer Certificate is issued on request: ${c.reason}.`;
    if (c.type === "Bonafide") return `This is to certify that ${c.studentName} (Admission No. ${c.admissionNo}) is a bonafide student of this institution, presently studying in ${c.grade}. This certificate is issued for the purpose of: ${c.reason}.`;
    if (c.type === "Character") return `This is to certify that ${c.studentName} (Admission No. ${c.admissionNo}), a student of ${c.grade}, bears a good moral character to the best of our knowledge. Issued for: ${c.reason}.`;
    if (c.type === "Migration") return `This is to certify that ${c.studentName} (Admission No. ${c.admissionNo}) has no objection from this institution to migrate to another board / institution. Issued for: ${c.reason}.`;
    return `This is to certify that ${c.studentName} (Admission No. ${c.admissionNo}) of ${c.grade} has cleared all fee dues payable to the institution as on date. No Objection Certificate issued for: ${c.reason}.`;
  };

  const staffOptions = staff.map((s) => ({ label: `${s.name} (${s.role})`, value: s.name }));

  return <div className="space-y-6">

      {/* KPI row */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">CALLS LOGGED TODAY</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{callsToday}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">{calls.filter((c) => c.status === "Open").length} open follow-ups overall</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Phone className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 p-5 rounded-3xl flex justify-between items-center text-white">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">POSTAL TODAY</span>
            <h4 className="text-2xl font-black text-white mt-1">{postalInToday} In / {postalOutToday} Out</h4>
            <p className="text-xs text-indigo-200 mt-1">{postal.length} entries in register</p>
          </div>
          <div className="p-3 bg-white/10 text-white rounded-2xl">
            <Mail className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">OPEN GRIEVANCES</span>
            <h4 className="text-2xl font-black text-rose-600 mt-1">{openGrievances}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">{grievances.filter((g) => daysOpen(g.date) > 7 && g.status !== "Closed" && g.status !== "Resolved").length} breaching 7-day SLA</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl">
            <AlertTriangle className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">CERTIFICATES ISSUED</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{certs.length}</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">{certs.filter((c) => c.duplicate).length} duplicate re-issues</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Award className="h-6 w-6" />
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-100 p-6 rounded-3xl">
        <Tabs activeTab={vTab} onChange={setVTab}>
          <TabsList className="border-b border-slate-100 flex gap-2">
            <TabsTrigger value="dashboard">Desk Dashboard</TabsTrigger>
            <TabsTrigger value="calls">Call Log</TabsTrigger>
            <TabsTrigger value="postal">Postal Register</TabsTrigger>
            <TabsTrigger value="grievances">Grievances</TabsTrigger>
            <TabsTrigger value="certificates">Certificates & TC</TabsTrigger>
          </TabsList>

          {/* TAB 1: Dashboard */}
          <TabsContent value="dashboard">
            <div className="space-y-5 pt-4">
              <div>
                <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Recent Desk Activity</h4>
                <p className="text-xs text-slate-400 font-semibold mt-1">Latest movements across calls, postal, grievances and certificate issue registers.</p>
              </div>
              <div className="space-y-2.5">
                {activityFeed.map((a, idx) => <div key={idx} className="flex items-center gap-3 bg-slate-50 border border-slate-100 p-3.5 rounded-2xl">
                    <div className={`p-2 rounded-xl shrink-0 ${a.icon === "call" ? "bg-sky-50 text-sky-600" : a.icon === "post" ? "bg-indigo-50 text-indigo-600" : a.icon === "grv" ? "bg-rose-50 text-rose-600" : "bg-emerald-50 text-emerald-600"}`}>
                      {a.icon === "call" ? <Phone className="h-4 w-4" /> : a.icon === "post" ? <Mail className="h-4 w-4" /> : a.icon === "grv" ? <MessageSquareWarning className="h-4 w-4" /> : <Award className="h-4 w-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-bold text-slate-700 truncate">{a.text}</p>
                      <p className="text-[9px] uppercase font-extrabold text-slate-400 tracking-wide mt-0.5">{a.date}{a.time !== "—" ? ` · ${a.time}` : ""}</p>
                    </div>
                    <Badge variant={a.tag === "Open" || a.tag === "In Review" ? "warning" : a.tag === "Duplicate" ? "danger" : "success"}>{a.tag}</Badge>
                  </div>)}
              </div>
            </div>
          </TabsContent>

          {/* TAB 2: Calls */}
          <TabsContent value="calls">
            <div className="space-y-5 pt-4">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Telephone Call Register</h4>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Every reception-desk call with routing, follow-up dates and CRM handoff.</p>
                </div>
                <Button onClick={() => setIsCallOpen(true)} className="flex gap-2 items-center text-xs">
                  <Plus className="h-4 w-4" /> Log Call
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Time</th>
                      <th className="p-4">Caller</th>
                      <th className="p-4">Phone</th>
                      <th className="p-4">Type</th>
                      <th className="p-4">Purpose</th>
                      <th className="p-4">Assigned To</th>
                      <th className="p-4">Follow-up</th>
                      <th className="p-4 text-center">Status</th>
                      <th className="p-4 text-center">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {calls.map((c) => <tr key={c.id}>
                        <td className="p-4 text-slate-400 font-bold whitespace-nowrap">{c.date}<br />{c.time}</td>
                        <td className="p-4 font-extrabold text-slate-800">{c.caller}</td>
                        <td className="p-4 text-slate-500 whitespace-nowrap">{c.phone}</td>
                        <td className="p-4">
                          <span className="flex items-center gap-1.5">
                            {c.type === "Incoming" ? <PhoneIncoming className="h-3.5 w-3.5 text-emerald-500" /> : <PhoneOutgoing className="h-3.5 w-3.5 text-sky-500" />}
                            {c.type}
                          </span>
                        </td>
                        <td className="p-4"><Badge variant={c.purpose === "Complaint" ? "danger" : c.purpose === "Admission Enquiry" ? "secondary" : "default"}>{c.purpose}</Badge></td>
                        <td className="p-4 text-slate-500">{c.assignedToName}</td>
                        <td className="p-4 text-slate-400 font-bold">{c.followUp || "—"}</td>
                        <td className="p-4 text-center">
                          <button onClick={() => toggleCallStatus(c.id)} className="cursor-pointer" title="Toggle status">
                            <Badge variant={c.status === "Open" ? "warning" : "success"}>{c.status}</Badge>
                          </button>
                        </td>
                        <td className="p-4 text-center">
                          {c.purpose === "Admission Enquiry" ? (c.sentToCRM ? <Badge variant="info">In CRM</Badge> : <button
                            onClick={() => handleSendToCRM(c.id)}
                            className="inline-flex items-center gap-1 bg-indigo-50 text-indigo-600 border border-indigo-100 text-[10px] px-2.5 py-1.5 rounded-xl font-bold hover:bg-indigo-600 hover:text-white transition cursor-pointer"
                          >
                              <Send className="h-3 w-3" /> Send to CRM
                            </button>) : <span className="text-slate-300">—</span>}
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </TabsContent>

          {/* TAB 3: Postal */}
          <TabsContent value="postal">
            <div className="space-y-5 pt-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Dispatch & Receipt Register</h4>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Statutory inward / outward correspondence log with auto reference numbers.</p>
                </div>
                <div className="flex gap-3 items-center">
                  <div className="flex bg-slate-100 rounded-full p-1">
                    {["Inward", "Outward"].map((d) => <button
                      key={d}
                      onClick={() => setPostalView(d)}
                      className={`px-4 py-1.5 rounded-full text-[10px] uppercase font-extrabold tracking-wider transition cursor-pointer ${postalView === d ? "bg-slate-900 text-white" : "text-slate-500 hover:text-slate-800"}`}
                    >
                        {d}
                      </button>)}
                  </div>
                  <Button onClick={() => setIsPostOpen(true)} className="flex gap-2 items-center text-xs">
                    <Plus className="h-4 w-4" /> New Entry
                  </Button>
                </div>
              </div>

              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Date</th>
                      <th className="p-4">Ref No</th>
                      <th className="p-4">{postalView === "Inward" ? "Received From" : "Addressed To"}</th>
                      <th className="p-4">Subject</th>
                      <th className="p-4">Mode</th>
                      <th className="p-4">HandlerName</th>
                      <th className="p-4 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {postal.filter((p) => p.direction === postalView).length === 0 ? <tr>
                        <td colSpan={7} className="p-4 text-center text-slate-400">No {postalView.toLowerCase()} entries yet</td>
                      </tr> : postal.filter((p) => p.direction === postalView).map((p) => <tr key={p.id}>
                          <td className="p-4 text-slate-400 font-bold whitespace-nowrap">{p.date}</td>
                          <td className="p-4 font-black text-indigo-700 whitespace-nowrap">{p.refNo}</td>
                          <td className="p-4 font-extrabold text-slate-800">{p.party}</td>
                          <td className="p-4 text-slate-500 max-w-xs">{p.subject}</td>
                          <td className="p-4"><Badge variant="default">{p.mode}</Badge></td>
                          <td className="p-4 text-slate-500">{p.handlerName}</td>
                          <td className="p-4 text-center">
                            <Badge variant={p.status === "Received" || p.status === "Dispatched" ? "success" : "warning"}>{p.status}</Badge>
                          </td>
                        </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </TabsContent>

          {/* TAB 4: Grievances */}
          <TabsContent value="grievances">
            <div className="space-y-5 pt-4">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Grievance & Complaint Tracker</h4>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Workflow: Open <ArrowRight className="h-3 w-3 inline" /> In Review <ArrowRight className="h-3 w-3 inline" /> Resolved <ArrowRight className="h-3 w-3 inline" /> Closed. SLA target 7 days.</p>
                </div>
                <Button onClick={() => setIsGrvOpen(true)} className="flex gap-2 items-center text-xs">
                  <Plus className="h-4 w-4" /> Raise Grievance
                </Button>
              </div>

              <div className="space-y-3.5">
                {grievances.map((g) => {
                  const open = g.status !== "Closed" && g.status !== "Resolved";
                  const age = daysOpen(g.date);
                  const slaBreach = open && age > 7;
                  return <div key={g.id} className="p-5 bg-slate-50 border border-slate-100 rounded-3xl flex flex-col md:flex-row justify-between gap-4 items-start md:items-center">
                      <div className="space-y-1.5 flex-1">
                        <div className="flex gap-2.5 items-center flex-wrap">
                          <span className="text-sm font-black text-slate-800">{g.id}</span>
                          <Badge variant={g.severity === "High" ? "danger" : g.severity === "Medium" ? "warning" : "default"}>{g.severity}</Badge>
                          <Badge variant="info">{g.category}</Badge>
                          <span className={`text-[9px] uppercase font-black px-2 py-0.5 rounded-full border ${slaBreach ? "bg-rose-50 text-rose-700 border-rose-200" : "bg-slate-100 text-slate-500 border-slate-200"}`}>
                            {age} day{age === 1 ? "" : "s"} open {slaBreach ? "· SLA BREACH" : ""}
                          </span>
                        </div>
                        <p className="text-xs text-slate-400 font-bold">{g.raisedBy}: {g.raisedName} · Raised {g.date} · Assigned to {g.assignedToName}</p>
                        <p className="text-xs text-slate-600 leading-relaxed max-w-2xl bg-white p-3 rounded-xl border border-slate-150 mt-2 font-medium">{g.description}</p>
                        {g.resolutionNote && <p className="text-xs text-emerald-700 leading-relaxed max-w-2xl bg-emerald-50 p-3 rounded-xl border border-emerald-100 font-medium">
                            <span className="font-black uppercase text-[9px] block mb-1">Resolution note:</span>{g.resolutionNote}
                          </p>}
                      </div>
                      <div className="flex flex-col items-end gap-3 shrink-0">
                        <Badge variant={g.status === "Open" ? "danger" : g.status === "In Review" ? "warning" : g.status === "Resolved" ? "success" : "default"}>{g.status}</Badge>
                        {GRIEV_FLOW[g.status] && <Button size="sm" variant="outline" onClick={() => advanceGrievance(g)} className="flex gap-1.5 items-center">
                            {GRIEV_FLOW[g.status] === "Resolved" ? "Resolve" : `Move to ${GRIEV_FLOW[g.status]}`} <ArrowRight className="h-3.5 w-3.5" />
                          </Button>}
                      </div>
                    </div>;
                })}
              </div>
            </div>
          </TabsContent>

          {/* TAB 5: Certificates */}
          <TabsContent value="certificates">
            <div className="space-y-5 pt-4">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Certificate & TC Issue Register</h4>
                  <p className="text-xs text-slate-400 font-semibold mt-1">Serial-controlled issue log. TC issue is blocked until fee dues are certified clear.</p>
                </div>
                <Button onClick={() => openIssueDialog(null)} className="flex gap-2 items-center text-xs">
                  <FileBadge className="h-4 w-4" /> Issue Certificate
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Serial No</th>
                      <th className="p-4">Date</th>
                      <th className="p-4">Student</th>
                      <th className="p-4">Type</th>
                      <th className="p-4">Reason</th>
                      <th className="p-4">Issued By</th>
                      <th className="p-4 text-center">Flags</th>
                      <th className="p-4 text-center">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {certs.map((c) => <tr key={c.id}>
                        <td className="p-4 font-black text-indigo-700 whitespace-nowrap">{c.serial}</td>
                        <td className="p-4 text-slate-400 font-bold whitespace-nowrap">{c.date}</td>
                        <td className="p-4">
                          <p className="font-extrabold text-slate-800">{c.studentName}</p>
                          <p className="text-[9px] uppercase font-bold text-slate-400 mt-0.5">{c.admissionNo} · {c.grade}</p>
                        </td>
                        <td className="p-4"><Badge variant={c.type === "Transfer Certificate" ? "danger" : "secondary"}>{c.type}</Badge></td>
                        <td className="p-4 text-slate-500 max-w-xs">{c.reason}</td>
                        <td className="p-4 text-slate-500">{c.issuedBy}</td>
                        <td className="p-4 text-center">{c.duplicate ? <Badge variant="danger">DUPLICATE</Badge> : <Badge variant="success">Original</Badge>}</td>
                        <td className="p-4 text-center">
                          <div className="flex gap-1.5 justify-center">
                            <button onClick={() => setPreviewCert(c)} title="Preview certificate" className="p-2 rounded-lg bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white transition cursor-pointer">
                              <Eye className="h-3.5 w-3.5" />
                            </button>
                            <button onClick={() => openIssueDialog(c)} title="Issue duplicate" className="p-2 rounded-lg bg-amber-50 text-amber-600 hover:bg-amber-500 hover:text-white transition cursor-pointer">
                              <Copy className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Log Call dialog */}
      <Dialog isOpen={isCallOpen} onClose={() => setIsCallOpen(false)} title="Log Telephone Call">
        <form onSubmit={handleLogCall} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input label="Caller Name" value={cCaller} onChange={(e) => setCCaller(e.target.value)} placeholder="e.g. Rajesh Verma" required />
            <Input label="Phone Number" value={cPhone} onChange={(e) => setCPhone(e.target.value)} placeholder="+91 98XXX XXXXX" required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Call Type"
              options={[{ label: "Incoming", value: "Incoming" }, { label: "Outgoing", value: "Outgoing" }]}
              value={cType}
              onChange={(e) => setCType(e.target.value)}
            />
            <Select
              label="Purpose Category"
              options={["Admission Enquiry", "Fee Query", "Complaint", "Transport", "Other"].map((p) => ({ label: p, value: p }))}
              value={cPurpose}
              onChange={(e) => setCPurpose(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Assign To Staff"
              options={staffOptions}
              value={cAssigned || staffOptions[0]?.value || ""}
              onChange={(e) => setCAssigned(e.target.value)}
            />
            <Input label="Follow-up Date" type="date" value={cFollowUp} onChange={(e) => setCFollowUp(e.target.value)} />
          </div>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsCallOpen(false)}>Cancel</Button>
            <Button type="submit">Save Call Record</Button>
          </div>
        </form>
      </Dialog>

      {/* Postal entry dialog */}
      <Dialog isOpen={isPostOpen} onClose={() => setIsPostOpen(false)} title={`New ${postalView} Postal Entry — ${nextRefNo(postalView)}`}>
        <form onSubmit={handleAddPostal} className="space-y-4 pt-1">
          <Input
            label={postalView === "Inward" ? "Received From (Party)" : "Addressed To (Party)"}
            value={pParty}
            onChange={(e) => setPParty(e.target.value)}
            placeholder="e.g. CBSE Regional Office, Bengaluru"
            required
          />
          <Input label="Subject / Contents" value={pSubject} onChange={(e) => setPSubject(e.target.value)} placeholder="e.g. Board exam centre allotment circular" required />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Mode"
              options={[{ label: "Courier", value: "Courier" }, { label: "Post", value: "Post" }, { label: "Hand Delivery", value: "Hand" }]}
              value={pMode}
              onChange={(e) => setPMode(e.target.value)}
            />
            <Select
              label="Handled By"
              options={staffOptions}
              value={pHandlerName || staffOptions[0]?.value || ""}
              onChange={(e) => setPHandlerName(e.target.value)}
            />
          </div>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsPostOpen(false)}>Cancel</Button>
            <Button type="submit" className="flex gap-2 items-center"><Inbox className="h-4 w-4" /> Register Entry</Button>
          </div>
        </form>
      </Dialog>

      {/* Raise grievance dialog */}
      <Dialog isOpen={isGrvOpen} onClose={() => setIsGrvOpen(false)} title="Raise New Grievance">
        <form onSubmit={handleRaiseGrievance} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Raised By"
              options={["Parent", "Staff", "Student"].map((t) => ({ label: t, value: t }))}
              value={gRaisedBy}
              onChange={(e) => setGRaisedBy(e.target.value)}
            />
            <Input label="Complainant Name" value={gRaisedName} onChange={(e) => setGRaisedName(e.target.value)} placeholder="e.g. Anand Krishnamurthy" required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Category"
              options={["Academic", "Transport", "Fee", "Safety", "Staff Conduct"].map((c) => ({ label: c, value: c }))}
              value={gCategory}
              onChange={(e) => setGCategory(e.target.value)}
            />
            <Select
              label="Severity"
              options={["Low", "Medium", "High"].map((s) => ({ label: s, value: s }))}
              value={gSeverity}
              onChange={(e) => setGSeverity(e.target.value)}
            />
          </div>
          <Select
            label="Assign To"
            options={staffOptions}
            value={gAssigned || staffOptions[0]?.value || ""}
            onChange={(e) => setGAssigned(e.target.value)}
          />
          <div>
            <label className="text-xs font-bold text-slate-600 block mb-1.5 uppercase tracking-wider">Description</label>
            <textarea
              className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 max-h-32 focus:bg-white transition"
              rows={3}
              placeholder="Describe the complaint with dates, names and specifics..."
              value={gDesc}
              onChange={(e) => setGDesc(e.target.value)}
              required
            />
          </div>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsGrvOpen(false)}>Cancel</Button>
            <Button type="submit">Register Grievance</Button>
          </div>
        </form>
      </Dialog>

      {/* Resolution note dialog */}
      <Dialog isOpen={!!resolveTarget} onClose={() => setResolveTarget(null)} title={resolveTarget ? `Resolve ${resolveTarget.id}` : ""}>
        {resolveTarget && <form onSubmit={handleResolveSubmit} className="space-y-4 pt-1">
            <p className="text-xs text-slate-500 font-semibold bg-slate-50 border border-slate-100 p-3 rounded-xl leading-relaxed">{resolveTarget.description}</p>
            <div>
              <label className="text-xs font-bold text-slate-600 block mb-1.5 uppercase tracking-wider">Resolution Note (required)</label>
              <textarea
                className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 max-h-32 focus:bg-white transition"
                rows={3}
                placeholder="Record the corrective action taken and outcome..."
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                required
              />
            </div>
            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setResolveTarget(null)}>Cancel</Button>
              <Button type="submit" variant="secondary">Mark Resolved</Button>
            </div>
          </form>}
      </Dialog>

      {/* Issue certificate dialog */}
      <Dialog isOpen={isCertOpen} onClose={() => setIsCertOpen(false)} title={duplicateOf ? `Issue Duplicate of ${duplicateOf.serial}` : "Issue Certificate"}>
        <form onSubmit={handleIssueCert} className="space-y-4 pt-1">
          {duplicateOf && <div className="bg-amber-50 border border-amber-200 text-amber-900 p-3 rounded-xl text-xs font-semibold">
              This issue will be stamped <span className="font-black">DUPLICATE</span> and cross-referenced to original serial {duplicateOf.serial}.
            </div>}
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Certificate Type"
              options={CERT_TYPES.map((t) => ({ label: t, value: t }))}
              value={certType}
              onChange={(e) => setCertType(e.target.value)}
              disabled={!!duplicateOf}
            />
            <Select
              label="Student"
              options={students.map((s) => ({ label: `${s.name} (${s.admissionNo})`, value: s.id }))}
              value={certStudentDocsId}
              onChange={(e) => setCertStudentDocsId(e.target.value)}
              disabled={!!duplicateOf}
            />
          </div>
          <Input label="Reason / Purpose" value={certReason} onChange={(e) => setCertReason(e.target.value)} placeholder="e.g. Family relocating to Pune" required />

          {certType === "Transfer Certificate" && <div className="bg-slate-50 border border-slate-100 p-4 rounded-2xl space-y-4">
              <p className="text-[10px] uppercase tracking-widest font-extrabold text-slate-400">Transfer Certificate Particulars</p>
              <div className="grid grid-cols-2 gap-4">
                <Input label="Last Class Attended" value={tcLastClass} onChange={(e) => setTcLastClass(e.target.value)} placeholder="e.g. Grade 8" />
                <Select
                  label="Conduct Remark"
                  options={["Excellent", "Good", "Satisfactory"].map((c) => ({ label: c, value: c }))}
                  value={tcConduct}
                  onChange={(e) => setTcConduct(e.target.value)}
                />
              </div>
              <label className="flex items-center gap-2.5 text-xs font-bold text-slate-700 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={tcDuesCleared}
                  onChange={(e) => setTcDuesCleared(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300 accent-indigo-600"
                />
                I certify all fee, hostel, library and store dues are cleared for this student.
              </label>
            </div>}

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsCertOpen(false)}>Cancel</Button>
            <Button type="submit" className="flex gap-2 items-center"><FileBadge className="h-4 w-4" /> Issue & Register</Button>
          </div>
        </form>
      </Dialog>

      {/* Certificate preview dialog */}
      <Dialog isOpen={!!previewCert} onClose={() => setPreviewCert(null)} title="Certificate Preview" maxWidth="max-w-2xl">
        {previewCert && <div className="space-y-4">
            <div className="border-4 border-double border-slate-700 rounded-xl p-8 bg-amber-50/40 relative">
              {previewCert.duplicate && <span className="absolute top-4 right-4 rotate-12 text-[10px] font-black uppercase tracking-widest text-rose-600 border-2 border-rose-500 px-3 py-1 rounded">DUPLICATE</span>}
              <div className="text-center border-b-2 border-slate-300 pb-4">
                <p className="text-lg font-black text-slate-900 uppercase tracking-widest">St. Joseph's International School</p>
                <p className="text-[10px] uppercase tracking-wider font-extrabold text-slate-500 mt-1">No. 14, Residency Road, Bengaluru — 560025 · Affiliation No. 830412 (CBSE)</p>
              </div>
              <div className="flex justify-between text-[10px] uppercase font-extrabold text-slate-500 tracking-wider mt-4">
                <span>Serial No: {previewCert.serial}</span>
                <span>Date: {previewCert.date}</span>
              </div>
              <p className="text-center text-sm font-black text-slate-800 uppercase tracking-widest mt-5 underline underline-offset-4">{previewCert.type}</p>
              <p className="text-xs text-slate-700 leading-relaxed mt-5 font-medium text-justify">{certBodyText(previewCert)}</p>
              <div className="flex justify-between mt-12 text-[10px] uppercase font-extrabold text-slate-500 tracking-wider">
                <div className="text-center">
                  <div className="border-t-2 border-slate-400 w-36 mb-1.5" />
                  Office Superintendent
                </div>
                <div className="text-center">
                  <div className="border-t-2 border-slate-400 w-36 mb-1.5" />
                  Principal & Seal
                </div>
              </div>
            </div>
            <div className="flex gap-3 justify-end pt-2 border-t border-slate-100">
              <Button variant="outline" onClick={() => setPreviewCert(null)}>Close</Button>
              <Button onClick={() => { addToast("Sent to Printer", `${previewCert.serial} queued on office printer (simulated).`, "success"); logAction(user.id, user.name, user.role, "Certificate Printed", `Printed ${previewCert.type} ${previewCert.serial} for ${previewCert.studentName}`); }} className="flex gap-2 items-center">
                <Printer className="h-4 w-4" /> Print
              </Button>
            </div>
          </div>}
      </Dialog>

    </div>;
}
