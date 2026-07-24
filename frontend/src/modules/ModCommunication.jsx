/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, getStaff, logAction } from "../storage";
import { api } from "../api";
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
  Megaphone,
  MessageCircle,
  MessageSquare,
  Mail,
  Bell,
  Send,
  Paperclip,
  Users,
  CalendarDays,
  Plus,
  BookOpen,
  CheckCheck,
  Languages,
  ChevronDown,
  ChevronUp,
  ShieldCheck,
  Clock,
  FileText
} from "lucide-react";

const loadLS = (key, seed) => { const raw = localStorage.getItem(key); if (raw) return JSON.parse(raw); localStorage.setItem(key, JSON.stringify(seed)); return seed; };
const saveLS = (key, data) => localStorage.setItem(key, JSON.stringify(data));

const GRADES = ["Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"];
const ROUTES = ["Route Alpha (North)", "Route Beta (Metro)", "Route Gamma (South Peaks)"];

const CHANNELS = [
  { id: "WhatsApp", label: "WhatsApp", note: "Business API · rich media", tone: "text-emerald-600 bg-emerald-50 border-emerald-200" },
  { id: "SMS", label: "SMS", note: "DLT registered · 160 ch/credit", tone: "text-indigo-600 bg-indigo-50 border-indigo-200" },
  { id: "Email", label: "Email", note: "Transactional · attachments", tone: "text-sky-600 bg-sky-50 border-sky-200" },
  { id: "Push", label: "App Push", note: "Parent app notification", tone: "text-amber-600 bg-amber-50 border-amber-200" }
];

const DLT_TEMPLATES = [
  { id: "1107169876543210001", name: "Fee Reminder (EN)", body: "Dear Parent, the school fee of Rs.{#var#} for {#var#} is due on {#var#}. Kindly pay online at portal. - STJUDE" },
  { id: "1107169876543210002", name: "Absent Alert", body: "Dear Parent, your ward {#var#} was marked ABSENT today {#var#}. Please contact class teacher. - STJUDE" },
  { id: "1107169876543210003", name: "Exam Datesheet", body: "Dear Parent, the {#var#} datesheet for {#var#} is published on the parent app. - STJUDE" },
  { id: "1107169876543210004", name: "PTM Invitation", body: "Dear Parent, PTM for {#var#} is on {#var#}. Book your slot on the parent app. - STJUDE" }
];

const SEED_COMM_LOG = [
  { id: "msg-1", date: "2026-07-03 09:15", channel: "WhatsApp", audience: "Whole School", preview: "School reopens on Monday, 6th July after campus maintenance. Buses run on normal schedule.", sent: 520, delivered: 511, read: 468, credits: 0, by: "Admin Office" },
  { id: "msg-2", date: "2026-07-02 14:40", channel: "SMS", audience: "Fee Defaulters", preview: "Dear Parent, the school fee of Rs.12,500 for Term 1 is due on 10-07-2026...", sent: 86, delivered: 84, read: 61, credits: 172, templateDocsId: "1107169876543210001", by: "Accounts Cell" },
  { id: "msg-3", date: "2026-07-01 11:05", channel: "Email", audience: "Grade 10", preview: "Half Yearly Examination pattern & syllabus blueprint attached. Kindly review with your ward.", sent: 74, delivered: 73, read: 49, credits: 0, by: "Examination Cell" },
  { id: "msg-4", date: "2026-06-28 08:30", channel: "Push", audience: "Hostel Boarders", preview: "Hostel mess menu updated for July. Special Sunday breakfast: chhole bhature.", sent: 84, delivered: 80, read: 72, credits: 0, by: "Hostel Warden" },
  { id: "msg-5", date: "2026-06-25 16:20", channel: "WhatsApp", audience: "Route Beta (Metro)", preview: "Route Beta bus will depart 15 minutes early tomorrow due to metro station roadworks.", sent: 38, delivered: 38, read: 35, credits: 0, by: "Transport Desk" }
];

const SEED_CIRCULARS = [
  {
    id: "circ-1", title: "Monsoon Timing Change & Rainy Day SOP", audience: "Whole School", date: "2026-07-01", attachment: "circular_monsoon_timings.pdf", total: 520, read: 412,
    perClass: [{ grade: "Grade 6", pct: 88 }, { grade: "Grade 7", pct: 84 }, { grade: "Grade 8", pct: 79 }, { grade: "Grade 9", pct: 76 }, { grade: "Grade 10", pct: 81 }, { grade: "Grade 11", pct: 72 }, { grade: "Grade 12", pct: 74 }]
  },
  {
    id: "circ-2", title: "Half Yearly Examination Guidelines & Syllabus", audience: "Grade 9 - Grade 12", date: "2026-06-26", attachment: "half_yearly_guidelines.pdf", total: 296, read: 231,
    perClass: [{ grade: "Grade 9", pct: 82 }, { grade: "Grade 10", pct: 85 }, { grade: "Grade 11", pct: 68 }, { grade: "Grade 12", pct: 77 }]
  },
  {
    id: "circ-3", title: "Independence Day Cultural Programme — Parent Invite", audience: "Whole School", date: "2026-06-20", attachment: "independence_day_invite.pdf", total: 520, read: 388,
    perClass: [{ grade: "Grade 6", pct: 80 }, { grade: "Grade 7", pct: 78 }, { grade: "Grade 8", pct: 74 }, { grade: "Grade 9", pct: 70 }, { grade: "Grade 10", pct: 73 }, { grade: "Grade 11", pct: 69 }, { grade: "Grade 12", pct: 76 }]
  }
];

const SEED_PTM = [
  {
    id: "ptm-1", title: "Term 1 Parent-Teacher Meeting", date: "2026-07-18", slotMins: 15, startTime: "09:00", slotCount: 8,
    bookings: {
      "staff-teacher-1|09:00 AM": { studentName: "Aarav Sharma", parentName: "Rakesh Sharma" },
      "staff-teacher-2|09:15 AM": { studentName: "Diya Patel", parentName: "Mehul Patel" }
    }
  }
];

const buildTodayIso = () => {
  const t = new Date();
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, "0")}-${String(t.getDate()).padStart(2, "0")}`;
};

const SEED_DIARY = [
  { id: "de-1", grade: "Grade 10", date: buildTodayIso(), type: "Homework", subject: "Mathematics", text: "Complete Exercise 4.2 (Quadratic Equations) Q1-Q12. Show full working in fair notebook.", teacherName: "Prof. Liam Johnson", acks: 41, total: 74 },
  { id: "de-2", grade: "Grade 10", date: buildTodayIso(), type: "Reminder", subject: "General", text: "Bring ₹150 for the Science Museum field trip consent + fee by Friday.", teacherName: "Class Teacher", acks: 52, total: 74 },
  { id: "de-3", grade: "Grade 7", date: buildTodayIso(), type: "Remark", subject: "English", text: "Excellent participation in the elocution practice today. Keep revising the poem for assembly.", teacherName: "Prof. Olivia Williams", acks: 18, total: 74 }
];

const buildSlots = (startTime, slotMins, slotCount) => {
  const [h, m] = startTime.split(":").map(Number);
  return Array.from({ length: slotCount }, (_, i) => {
    const t = h * 60 + m + i * slotMins;
    const hh = Math.floor(t / 60);
    const mm = t % 60;
    const h12 = (hh + 11) % 12 + 1;
    return `${String(h12).padStart(2, "0")}:${String(mm).padStart(2, "0")} ${hh >= 12 ? "PM" : "AM"}`;
  });
};

const channelIcon = (ch, cls = "h-4 w-4") => {
  if (ch === "WhatsApp") return <MessageCircle className={cls} />;
  if (ch === "SMS") return <MessageSquare className={cls} />;
  if (ch === "Email") return <Mail className={cls} />;
  return <Bell className={cls} />;
};

export default function ModCommunication({ user }) {
  const { addToast } = useToast();
  const [tab, setTab] = useState("dashboard");
  const [commLog, setCommLog] = useState(() => loadLS("erp_comm_log", SEED_COMM_LOG));
  const [circulars, setCirculars] = useState(() => loadLS("erp_circulars", SEED_CIRCULARS));
  const [ptmEvents, setPtmEvents] = useState(() => loadLS("erp_ptm", SEED_PTM));
  const [diary, setDiary] = useState(() => loadLS("erp_diary", SEED_DIARY));
  const [students] = useState(() => getStudents());
  const [staffList] = useState(() => getStaff());

  const teachers = staffList.filter((s) => s.role === "Teacher").slice(0, 6);
  const todayIso = buildTodayIso();

  /* ---- broadcast state ---- */
  const [channel, setChannel] = useState("WhatsApp");
  const [audienceType, setAudienceType] = useState("school");
  const [audienceGrade, setAudienceGrade] = useState("Grade 10");
  const [audienceRoute, setAudienceRoute] = useState(ROUTES[0]);
  const [dltTemplateDocsId, setDltTemplateDocsId] = useState(DLT_TEMPLATES[0].id);
  const [message, setMessage] = useState("");
  const [language, setLanguage] = useState("English");
  const [autoTranslate, setAutoTranslate] = useState(false);
  const [sending, setSending] = useState(false);

  /* ---- circulars state ---- */
  const [expandedCircular, setExpandedCircular] = useState("");
  const [isCircularOpen, setIsCircularOpen] = useState(false);
  const [circTitle, setCircTitle] = useState("");
  const [circAudience, setCircAudience] = useState("Whole School");
  const [circAttachment, setCircAttachment] = useState("circular.pdf");

  /* ---- PTM state ---- */
  const [activePtmId, setActivePtmId] = useState(ptmEvents[0]?.id || "");
  const [isPtmCreateOpen, setIsPtmCreateOpen] = useState(false);
  const [ptmTitle, setPtmTitle] = useState("");
  const [ptmDate, setPtmDate] = useState("2026-08-08");
  const [ptmSlotMins, setPtmSlotMins] = useState("15");
  const [bookTarget, setBookTarget] = useState(null);
  const [bookStudentDocsId, setBookStudentDocsId] = useState(students[0]?.id || "");

  /* ---- diary state ---- */
  const [diaryGrade, setDiaryGrade] = useState("Grade 10");
  const [diaryDate, setDiaryDate] = useState(todayIso);
  const [isDiaryOpen, setIsDiaryOpen] = useState(false);
  const [entryType, setEntryType] = useState("Homework");
  const [entrySubject, setEntrySubject] = useState("Mathematics");
  const [entryText, setEntryText] = useState("");

  /* ============ audience helpers ============ */
  const audienceLabel = audienceType === "school" ? "Whole School"
    : audienceType === "class" ? audienceGrade
    : audienceType === "hostel" ? "Hostel Boarders"
    : audienceType === "transport" ? audienceRoute
    : "Fee Defaulters";

  const audienceCount = audienceType === "school" ? students.length
    : audienceType === "class" ? students.filter((s) => s.grade === audienceGrade).length
    : audienceType === "hostel" ? students.filter((s) => s.hostelOptIn).length
    : audienceType === "transport" ? students.filter((s) => s.transportRoute === audienceRoute).length
    : 86;

  const smsCredits = Math.max(1, Math.ceil(message.length / 160));
  const selectedTemplate = DLT_TEMPLATES.find((t) => t.id === dltTemplateDocsId);

  /* ============ broadcast send ============ */
  const handleSend = () => {
    if (!message.trim()) {
      addToast("Empty Message", "Type a message before sending the broadcast", "error");
      return;
    }
    if (channel === "SMS" && !selectedTemplate) {
      addToast("DLT Template Required", "SMS in India must map to an approved DLT template", "error");
      return;
    }
    setSending(true);
    setTimeout(() => {
      const sent = audienceCount;
      const deliveredPct = channel === "SMS" ? 0.96 : channel === "WhatsApp" ? 0.98 : channel === "Email" ? 0.99 : 0.93;
      const readPct = channel === "WhatsApp" ? 0.86 : channel === "Push" ? 0.78 : channel === "SMS" ? 0.7 : 0.62;
      const delivered = Math.round(sent * deliveredPct);
      const read = Math.round(delivered * readPct);
      const now = new Date();
      const stamp = `${todayIso} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
      const entry = {
        id: `msg-${Date.now()}`,
        date: stamp,
        channel,
        audience: audienceLabel,
        preview: message.slice(0, 120),
        sent,
        delivered,
        read,
        credits: channel === "SMS" ? sent * smsCredits : 0,
        templateDocsId: channel === "SMS" ? dltTemplateDocsId : undefined,
        language: autoTranslate ? `${language} (auto-translated)` : language,
        by: user.name
      };
      const updated = [entry, ...commLog];
      setCommLog(updated);
      saveLS("erp_comm_log", updated);
      api.createAnnouncement({
        schoolId: 'SCH-001',
        title: `Broadcast (${channel}) - ${audienceLabel}`,
        content: message,
        targetAudience: audienceLabel,
        publishedBy: user.name
      }).catch(() => {});
      setSending(false);
      setMessage("");
      logAction(user.id, user.name, user.role, "Broadcast Sent", `${channel} broadcast to ${audienceLabel} (${sent} recipients, ${delivered} delivered${channel === "SMS" ? `, ${entry.credits} SMS credits` : ""})`);
      addToast("Broadcast Dispatched", `${channel} message queued to ${sent} recipients of ${audienceLabel}. ${delivered} delivered (simulated).`, "success");
    }, 1200);
  };

  /* ============ circulars ============ */
  const handleIssueCircular = (e) => {
    e.preventDefault();
    if (!circTitle) {
      addToast("Missing Title", "Circular title is required", "error");
      return;
    }
    const circ = {
      id: `circ-${Date.now()}`,
      title: circTitle,
      audience: circAudience,
      date: todayIso,
      attachment: circAttachment || "circular.pdf",
      total: circAudience === "Whole School" ? students.length : students.filter((s) => s.grade === circAudience).length,
      read: 0,
      perClass: (circAudience === "Whole School" ? GRADES : [circAudience]).map((g) => ({ grade: g, pct: 0 }))
    };
    const updated = [circ, ...circulars];
    setCirculars(updated);
    saveLS("erp_circulars", updated);
    api.createAnnouncement({
      schoolId: 'SCH-001',
      title: `Circular: ${circTitle}`,
      content: `Official circular issued for ${circAudience}`,
      targetAudience: circAudience,
      publishedBy: user.name
    }).catch(() => {});
    logAction(user.id, user.name, user.role, "Circular Issued", `Issued circular "${circTitle}" to ${circAudience} with attachment ${circ.attachment}`);
    addToast("Circular Issued", `"${circTitle}" pushed to ${circ.total} parents with read receipts enabled`, "success");
    setIsCircularOpen(false);
    setCircTitle("");
  };

  /* ============ PTM ============ */
  const activePtm = ptmEvents.find((p) => p.id === activePtmId);

  const handleCreatePtm = (e) => {
    e.preventDefault();
    if (!ptmTitle || !ptmDate) {
      addToast("Missing Fields", "PTM title and date are required", "error");
      return;
    }
    const ev = { id: `ptm-${Date.now()}`, title: ptmTitle, date: ptmDate, slotMins: parseInt(ptmSlotMins), startTime: "09:00", slotCount: 8, bookings: {} };
    const updated = [...ptmEvents, ev];
    setPtmEvents(updated);
    saveLS("erp_ptm", updated);
    setActivePtmId(ev.id);
    logAction(user.id, user.name, user.role, "PTM Scheduled", `Created PTM "${ptmTitle}" on ${ptmDate} with ${ptmSlotMins}-minute slots for ${teachers.length} teachers`);
    addToast("PTM Created", `"${ptmTitle}" scheduled for ${ptmDate}. Booking grid is live.`, "success");
    setIsPtmCreateOpen(false);
    setPtmTitle("");
  };

  const handleBookSlot = (e) => {
    e.preventDefault();
    if (!bookTarget || !activePtm) return;
    const st = students.find((s) => s.id === bookStudentDocsId);
    if (!st) return;
    const key = `${bookTarget.teacherDocsId}|${bookTarget.slot}`;
    const updated = ptmEvents.map((p) => p.id === activePtm.id
      ? { ...p, bookings: { ...p.bookings, [key]: { studentName: st.name, parentName: st.parentName } } }
      : p);
    setPtmEvents(updated);
    saveLS("erp_ptm", updated);
    logAction(user.id, user.name, user.role, "PTM Slot Booked", `Booked ${bookTarget.slot} with ${bookTarget.teacherName} for ${st.name} (parent: ${st.parentName}) — ${activePtm.title}`);
    addToast("Slot Booked", `${bookTarget.slot} with ${bookTarget.teacherName} reserved for ${st.parentName}`, "success");
    setBookTarget(null);
  };

  /* ============ diary ============ */
  const diaryEntries = diary.filter((d) => d.grade === diaryGrade && d.date === diaryDate);
  const diaryClassSize = students.filter((s) => s.grade === diaryGrade).length;

  const handleAddDiaryEntry = (e) => {
    e.preventDefault();
    if (!entryText.trim()) {
      addToast("Empty Entry", "Write the diary note before publishing", "error");
      return;
    }
    const entry = { id: `de-${Date.now()}`, grade: diaryGrade, date: diaryDate, type: entryType, subject: entrySubject, text: entryText, teacherName: user.name, acks: 0, total: diaryClassSize };
    const updated = [entry, ...diary];
    setDiary(updated);
    saveLS("erp_diary", updated);
    logAction(user.id, user.name, user.role, "Diary Entry Published", `${entryType} entry for ${diaryGrade} (${entrySubject}) on ${diaryDate}: ${entryText.slice(0, 60)}`);
    addToast("Diary Updated", `${entryType} pushed to ${diaryClassSize} parents of ${diaryGrade}. Acknowledgements tracking started.`, "success");
    setIsDiaryOpen(false);
    setEntryText("");
  };

  /* ============ dashboard metrics ============ */
  const monthPrefix = todayIso.slice(0, 7);
  const monthLog = commLog.filter((m) => m.date.startsWith(monthPrefix));
  const monthByChannel = CHANNELS.map((c) => ({ ...c, count: monthLog.filter((m) => m.channel === c.id).length }));
  const totSent = monthLog.reduce((s, m) => s + m.sent, 0);
  const totDelivered = monthLog.reduce((s, m) => s + m.delivered, 0);
  const totRead = monthLog.reduce((s, m) => s + m.read, 0);
  const deliveryRate = totSent ? Math.round(totDelivered / totSent * 100) : 0;
  const readRate = totDelivered ? Math.round(totRead / totDelivered * 100) : 0;

  return <div className="space-y-6">

      {/* Module header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <Megaphone className="h-5 w-5 text-indigo-600" /> Communication Hub
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">WhatsApp / SMS / Email / Push broadcasts, circulars with read receipts, PTM slot booking and the digital diary.</p>
        </div>
        <Badge variant="success" className="uppercase tracking-widest text-[10px] flex items-center gap-1"><ShieldCheck className="h-3 w-3" /> DLT Compliant</Badge>
      </div>

      <Tabs activeTab={tab} onChange={setTab}>
        <TabsList>
          <TabsTrigger value="dashboard">Dashboard</TabsTrigger>
          <TabsTrigger value="broadcast">Broadcast</TabsTrigger>
          <TabsTrigger value="circulars">Circulars</TabsTrigger>
          <TabsTrigger value="ptm">PTM Scheduler</TabsTrigger>
          <TabsTrigger value="diary">Digital Diary</TabsTrigger>
        </TabsList>

        {/* ================= DASHBOARD ================= */}
        <TabsContent value="dashboard">
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
              <div className="bg-indigo-900 text-indigo-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">MESSAGES THIS MONTH</span>
                  <h4 className="text-2xl font-black text-white mt-1">{monthLog.length}</h4>
                  <p className="text-xs text-indigo-200 mt-1">{monthByChannel.map((c) => `${c.count} ${c.id}`).join(" · ")}</p>
                </div>
                <div className="p-3 bg-white/10 rounded-2xl text-white"><Send className="h-6 w-6" /></div>
              </div>

              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">DELIVERY RATE</span>
                  <h4 className="text-2xl font-black text-emerald-600 mt-1">{deliveryRate}%</h4>
                  <p className="text-xs text-slate-400 mt-1">{totDelivered.toLocaleString("en-IN")} of {totSent.toLocaleString("en-IN")} delivered</p>
                </div>
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl"><CheckCheck className="h-6 w-6" /></div>
              </div>

              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">READ RATE</span>
                  <h4 className="text-2xl font-black text-slate-800 mt-1">{readRate}%</h4>
                  <p className="text-xs text-slate-400 mt-1">{totRead.toLocaleString("en-IN")} messages opened</p>
                </div>
                <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl"><Mail className="h-6 w-6" /></div>
              </div>

              <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">DLT TEMPLATES</span>
                  <h4 className="text-2xl font-black text-indigo-600 mt-1">{DLT_TEMPLATES.length} Approved</h4>
                  <p className="text-xs text-slate-400 mt-1">TRAI DLT registered header: STJUDE</p>
                </div>
                <div className="p-3 bg-indigo-50 text-indigo-600 rounded-2xl"><ShieldCheck className="h-6 w-6" /></div>
              </div>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Recent Sends</h3>
              <div className="space-y-3">
                {commLog.slice(0, 6).map((m) => <div key={m.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border border-slate-100 rounded-2xl p-4 hover:bg-slate-50/60 transition">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="p-2.5 rounded-2xl bg-slate-50 text-slate-600 shrink-0">{channelIcon(m.channel, "h-5 w-5")}</div>
                      <div className="min-w-0">
                        <p className="text-xs font-extrabold text-slate-800 truncate">{m.preview}</p>
                        <p className="text-[10px] uppercase tracking-wider font-bold text-slate-400 mt-1 flex items-center gap-1.5">
                          <Clock className="h-3 w-3" /> {m.date} · {m.audience} · by {m.by}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <Badge variant="default">{m.channel}</Badge>
                      <Badge variant="success">{m.delivered}/{m.sent} delivered</Badge>
                      <Badge variant="info">{m.read} read</Badge>
                    </div>
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= BROADCAST ================= */}
        <TabsContent value="broadcast">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-5">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Compose Broadcast</h3>

              {/* channel pills */}
              <div>
                <p className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">Channel</p>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {CHANNELS.map((c) => <button
                      key={c.id}
                      type="button"
                      onClick={() => setChannel(c.id)}
                      className={`border rounded-2xl p-3 text-left transition cursor-pointer ${channel === c.id ? `${c.tone} ring-2 ring-offset-1 ring-indigo-400` : "bg-white border-slate-200 text-slate-500 hover:bg-slate-50"}`}
                    >
                      <div className="flex items-center gap-2 font-extrabold text-xs">{channelIcon(c.id)} {c.label}</div>
                      <p className="text-[9px] font-semibold mt-1 opacity-70">{c.note}</p>
                    </button>)}
                </div>
              </div>

              {/* audience */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Select
                  label="Audience"
                  value={audienceType}
                  onChange={(e) => setAudienceType(e.target.value)}
                  options={[
                    { label: "Whole School", value: "school" },
                    { label: "Specific Class", value: "class" },
                    { label: "Hostel Boarders", value: "hostel" },
                    { label: "Transport Route", value: "transport" },
                    { label: "Fee Defaulters", value: "defaulters" }
                  ]}
                />
                {audienceType === "class" && <Select label="Class" value={audienceGrade} onChange={(e) => setAudienceGrade(e.target.value)} options={GRADES.map((g) => ({ label: g, value: g }))} />}
                {audienceType === "transport" && <Select label="Route" value={audienceRoute} onChange={(e) => setAudienceRoute(e.target.value)} options={ROUTES.map((r) => ({ label: r, value: r }))} />}
                {audienceType !== "class" && audienceType !== "transport" && <div className="flex items-end">
                    <div className="bg-slate-50 border border-slate-100 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-600 flex items-center gap-2 w-full">
                      <Users className="h-4 w-4 text-slate-400" /> {audienceCount} recipients resolved
                    </div>
                  </div>}
              </div>

              {/* DLT for SMS */}
              {channel === "SMS" && <div className="space-y-2">
                  <Select
                    label="DLT Template (TRAI approved)"
                    value={dltTemplateDocsId}
                    onChange={(e) => setDltTemplateDocsId(e.target.value)}
                    options={DLT_TEMPLATES.map((t) => ({ label: `${t.name} · ID ${t.id}`, value: t.id }))}
                  />
                  {selectedTemplate && <p className="text-[10px] font-semibold text-slate-400 bg-slate-50 border border-slate-100 rounded-xl p-3 leading-relaxed">{selectedTemplate.body}</p>}
                  <p className="text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 rounded-xl p-3 flex items-center gap-2">
                    <ShieldCheck className="h-3.5 w-3.5 shrink-0" /> DLT registration is mandatory in India — promotional/transactional SMS must match an approved template on the operator DLT platform.
                  </p>
                </div>}

              {/* message */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Message</label>
                <textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  rows={4}
                  placeholder={channel === "SMS" ? "Fill template variables e.g. Dear Parent, the school fee of Rs.12,500 for Aarav is due on 10-07-2026..." : "Type the announcement for parents..."}
                  className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition resize-none"
                />
                <div className="flex justify-between text-[10px] font-bold text-slate-400">
                  <span>{message.length} characters</span>
                  {channel === "SMS" && <span className={smsCredits > 1 ? "text-amber-600" : ""}>{smsCredits} SMS credit{smsCredits > 1 ? "s" : ""} / recipient (160 ch each) · est. total {audienceCount * smsCredits} credits</span>}
                </div>
              </div>

              {/* language */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-end">
                <Select label="Language" value={language} onChange={(e) => setLanguage(e.target.value)} options={[{ label: "English", value: "English" }, { label: "Hindi (हिन्दी)", value: "Hindi" }]} />
                <label className="flex items-center gap-2.5 text-xs font-bold text-slate-700 bg-slate-50 border border-slate-100 rounded-xl px-4 py-3 cursor-pointer">
                  <input type="checkbox" checked={autoTranslate} onChange={(e) => setAutoTranslate(e.target.checked)} className="h-4 w-4 accent-indigo-600" />
                  <Languages className="h-4 w-4 text-slate-400" /> Auto-translate per parent's preferred language
                </label>
              </div>

              <div className="flex justify-end pt-3 border-t border-slate-100">
                <Button onClick={handleSend} disabled={sending} className="flex gap-2 items-center text-xs py-2.5">
                  <Send className={`h-4 w-4 ${sending ? "animate-pulse" : ""}`} />
                  {sending ? "Dispatching via gateway..." : `Send to ${audienceCount} recipients`}
                </Button>
              </div>
            </div>

            {/* side: recent log */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Send Log</h3>
              <div className="space-y-3">
                {commLog.slice(0, 8).map((m) => <div key={m.id} className="border border-slate-100 rounded-2xl p-3">
                    <div className="flex items-center gap-2 text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
                      {channelIcon(m.channel, "h-3.5 w-3.5")} {m.channel} · {m.audience}
                    </div>
                    <p className="text-xs font-semibold text-slate-600 mt-1.5 line-clamp-2">{m.preview}</p>
                    <div className="flex justify-between items-center mt-2 text-[9px] font-bold text-slate-400">
                      <span>{m.date}</span>
                      <span className="text-emerald-600">{m.delivered}/{m.sent} ✓ · {m.read} read</span>
                    </div>
                  </div>)}
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ================= CIRCULARS ================= */}
        <TabsContent value="circulars">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div>
                <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Official Circulars</h3>
                <p className="text-xs text-slate-400 font-semibold mt-1">Issued to parent app with per-family read receipts and class-wise analytics.</p>
              </div>
              <Button onClick={() => setIsCircularOpen(true)} className="flex gap-2 items-center text-xs py-2.5"><Plus className="h-4 w-4" /> Issue Circular</Button>
            </div>

            {circulars.map((c) => {
              const pct = c.total ? Math.round(c.read / c.total * 100) : 0;
              const expanded = expandedCircular === c.id;
              return <div key={c.id} className="bg-white border border-slate-100 rounded-3xl p-6">
                  <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                    <div className="min-w-0">
                      <h4 className="text-sm font-extrabold text-slate-800 flex items-center gap-2 flex-wrap">
                        <FileText className="h-4 w-4 text-indigo-600 shrink-0" /> {c.title}
                      </h4>
                      <p className="text-[10px] uppercase tracking-wider font-bold text-slate-400 mt-1">{c.date} · {c.audience}</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <Badge variant="secondary" className="flex items-center gap-1"><Paperclip className="h-3 w-3" /> {c.attachment}</Badge>
                      <Button variant="ghost" size="sm" onClick={() => setExpandedCircular(expanded ? "" : c.id)} className="flex gap-1 items-center">
                        {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />} Class-wise
                      </Button>
                    </div>
                  </div>

                  <div className="mt-4">
                    <div className="flex justify-between text-[10px] font-extrabold uppercase tracking-wider text-slate-400 mb-1.5">
                      <span>Read Receipts</span>
                      <span className="text-slate-600">{c.read}/{c.total} read ({pct}%)</span>
                    </div>
                    <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full transition-all duration-500 ${pct >= 75 ? "bg-emerald-500" : pct >= 40 ? "bg-amber-500" : "bg-rose-400"}`} style={{ width: `${pct}%` }} />
                    </div>
                  </div>

                  {expanded && <div className="overflow-x-auto border border-slate-100 rounded-2xl mt-4 animate-fade-in">
                      <table className="w-full text-xs font-semibold text-slate-700 text-left">
                        <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                          <tr><th className="p-3">Class</th><th className="p-3">Read %</th><th className="p-3 w-1/2">Progress</th></tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {c.perClass.map((r) => <tr key={r.grade}>
                              <td className="p-3 font-extrabold text-slate-800">{r.grade}</td>
                              <td className="p-3">{r.pct}%</td>
                              <td className="p-3">
                                <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                                  <div className="h-full bg-indigo-500 rounded-full" style={{ width: `${r.pct}%` }} />
                                </div>
                              </td>
                            </tr>)}
                        </tbody>
                      </table>
                    </div>}
                </div>;
            })}

            <Dialog isOpen={isCircularOpen} onClose={() => setIsCircularOpen(false)} title="Issue Official Circular">
              <form onSubmit={handleIssueCircular} className="space-y-4 pt-1">
                <Input label="Circular Title" value={circTitle} onChange={(e) => setCircTitle(e.target.value)} placeholder="e.g. Diwali Break Schedule 2026" required />
                <div className="grid grid-cols-2 gap-4">
                  <Select label="Audience" value={circAudience} onChange={(e) => setCircAudience(e.target.value)} options={[{ label: "Whole School", value: "Whole School" }, ...GRADES.map((g) => ({ label: g, value: g }))]} />
                  <Input label="Attachment File Name" value={circAttachment} onChange={(e) => setCircAttachment(e.target.value)} placeholder="circular.pdf" />
                </div>
                <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs text-slate-500 font-semibold leading-relaxed">
                  The circular is pushed to the parent app with read receipts on. Class-wise read analytics update as parents open it.
                </div>
                <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setIsCircularOpen(false)}>Cancel</Button>
                  <Button type="submit">Issue & Notify Parents</Button>
                </div>
              </form>
            </Dialog>
          </div>
        </TabsContent>

        {/* ================= PTM ================= */}
        <TabsContent value="ptm">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-end gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div className="w-full sm:max-w-sm">
                {ptmEvents.length > 0
                  ? <Select label="PTM Event" value={activePtmId} onChange={(e) => setActivePtmId(e.target.value)} options={ptmEvents.map((p) => ({ label: `${p.title} · ${p.date}`, value: p.id }))} />
                  : <p className="text-xs font-semibold text-slate-400">No PTM events yet — create one to open the booking grid.</p>}
              </div>
              <Button onClick={() => setIsPtmCreateOpen(true)} className="flex gap-2 items-center text-xs py-2.5"><Plus className="h-4 w-4" /> Create PTM Event</Button>
            </div>

            {activePtm && <div className="bg-white border border-slate-100 rounded-3xl p-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-5">
                  <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <CalendarDays className="h-4 w-4 text-indigo-600" /> {activePtm.title} · {activePtm.date}
                  </h3>
                  <div className="flex gap-2">
                    <Badge variant="info">{activePtm.slotMins} min slots</Badge>
                    <Badge variant="success">{Object.keys(activePtm.bookings).length} booked</Badge>
                  </div>
                </div>

                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-4 whitespace-nowrap">Teacher</th>
                        {buildSlots(activePtm.startTime, activePtm.slotMins, activePtm.slotCount).map((slot) => <th key={slot} className="p-3 text-center whitespace-nowrap">{slot}</th>)}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {teachers.map((t) => <tr key={t.id}>
                          <td className="p-4 whitespace-nowrap">
                            <p className="font-extrabold text-slate-800">{t.name}</p>
                            <p className="text-[9px] uppercase tracking-wider font-bold text-slate-400">{t.department}</p>
                          </td>
                          {buildSlots(activePtm.startTime, activePtm.slotMins, activePtm.slotCount).map((slot) => {
                            const booking = activePtm.bookings[`${t.id}|${slot}`];
                            return <td key={slot} className="p-2 text-center">
                                {booking
                                  ? <div className="bg-indigo-50 border border-indigo-200 rounded-lg px-2 py-1.5 min-w-[86px]" title={`Student: ${booking.studentName}`}>
                                      <p className="text-[9px] font-extrabold text-indigo-700 truncate">{booking.parentName}</p>
                                      <p className="text-[8px] font-bold text-indigo-400 uppercase tracking-wider">Booked</p>
                                    </div>
                                  : <button
                                      onClick={() => { setBookTarget({ teacherDocsId: t.id, teacherName: t.name, slot }); setBookStudentDocsId(students[0]?.id || ""); }}
                                      className="bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg px-2 py-1.5 text-[9px] font-extrabold uppercase tracking-wider hover:bg-emerald-100 transition cursor-pointer min-w-[86px]"
                                    >
                                      Free
                                    </button>}
                              </td>;
                          })}
                        </tr>)}
                    </tbody>
                  </table>
                </div>
                <p className="text-[10px] text-slate-400 italic mt-3">Click a free slot to reserve it for a parent. Bookings sync instantly to the parent app (simulated).</p>
              </div>}

            {/* Create PTM dialog */}
            <Dialog isOpen={isPtmCreateOpen} onClose={() => setIsPtmCreateOpen(false)} title="Schedule Parent-Teacher Meeting">
              <form onSubmit={handleCreatePtm} className="space-y-4 pt-1">
                <Input label="PTM Title" value={ptmTitle} onChange={(e) => setPtmTitle(e.target.value)} placeholder="e.g. Half Yearly Result Discussion PTM" required />
                <div className="grid grid-cols-2 gap-4">
                  <Input label="Date" type="date" value={ptmDate} onChange={(e) => setPtmDate(e.target.value)} required />
                  <Select label="Slot Length" value={ptmSlotMins} onChange={(e) => setPtmSlotMins(e.target.value)} options={[{ label: "10 minutes", value: "10" }, { label: "15 minutes", value: "15" }, { label: "20 minutes", value: "20" }]} />
                </div>
                <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs text-slate-500 font-semibold leading-relaxed">
                  A booking grid of 8 slots from 09:00 AM is generated for each of the {teachers.length} teachers. Parents book via the app; the front desk can also book below.
                </div>
                <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setIsPtmCreateOpen(false)}>Cancel</Button>
                  <Button type="submit">Create & Open Bookings</Button>
                </div>
              </form>
            </Dialog>

            {/* Book slot dialog */}
            <Dialog isOpen={!!bookTarget} onClose={() => setBookTarget(null)} title="Book PTM Slot" maxWidth="max-w-md">
              {bookTarget && <form onSubmit={handleBookSlot} className="space-y-4 pt-1">
                  <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs font-semibold text-slate-600 space-y-1">
                    <p>Teacher: <span className="font-extrabold text-slate-800">{bookTarget.teacherName}</span></p>
                    <p>Slot: <span className="font-extrabold text-slate-800">{bookTarget.slot}</span> · {activePtm ? activePtm.date : ""}</p>
                  </div>
                  <Select label="Student (parent auto-linked)" value={bookStudentDocsId} onChange={(e) => setBookStudentDocsId(e.target.value)} options={students.map((s) => ({ label: `${s.name} (${s.grade}) — parent: ${s.parentName}`, value: s.id }))} />
                  <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
                    <Button variant="outline" onClick={() => setBookTarget(null)}>Cancel</Button>
                    <Button type="submit">Confirm Booking</Button>
                  </div>
                </form>}
            </Dialog>
          </div>
        </TabsContent>

        {/* ================= DIARY ================= */}
        <TabsContent value="diary">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-end gap-4 bg-white p-5 rounded-3xl border border-slate-100">
              <div className="grid grid-cols-2 gap-4 w-full sm:max-w-md">
                <Select label="Class" value={diaryGrade} onChange={(e) => setDiaryGrade(e.target.value)} options={GRADES.map((g) => ({ label: g, value: g }))} />
                <Input label="Date" type="date" value={diaryDate} onChange={(e) => setDiaryDate(e.target.value)} />
              </div>
              <Button onClick={() => setIsDiaryOpen(true)} className="flex gap-2 items-center text-xs py-2.5"><Plus className="h-4 w-4" /> Add Diary Entry</Button>
            </div>

            <div className="bg-white border border-slate-100 rounded-3xl p-6">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4 flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-indigo-600" /> {diaryGrade} · {diaryDate}
              </h3>

              {diaryEntries.length === 0 && <div className="border border-dashed border-slate-200 rounded-2xl p-10 text-center text-xs font-semibold text-slate-400">
                  No diary entries for {diaryGrade} on this date. Use "Add Diary Entry" to publish homework, remarks or reminders to parents.
                </div>}

              <div className="space-y-4">
                {diaryEntries.map((d) => {
                  const ackPct = d.total ? Math.round(d.acks / d.total * 100) : 0;
                  return <div key={d.id} className="border border-slate-100 rounded-2xl p-4">
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                        <div className="flex items-center gap-2 flex-wrap">
                          <Badge variant={d.type === "Homework" ? "secondary" : d.type === "Remark" ? "success" : "warning"}>{d.type}</Badge>
                          <span className="text-xs font-extrabold text-slate-800">{d.subject}</span>
                          <span className="text-[10px] font-bold text-slate-400">by {d.teacherName}</span>
                        </div>
                        <span className="text-[10px] font-extrabold text-slate-500 flex items-center gap-1.5 shrink-0">
                          <CheckCheck className="h-3.5 w-3.5 text-emerald-500" /> {d.acks}/{d.total} parents acknowledged
                        </span>
                      </div>
                      <p className="text-xs font-semibold text-slate-600 mt-2.5 leading-relaxed">{d.text}</p>
                      <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden mt-3">
                        <div className="h-full bg-emerald-500 rounded-full transition-all duration-500" style={{ width: `${ackPct}%` }} />
                      </div>
                    </div>;
                })}
              </div>
            </div>

            <Dialog isOpen={isDiaryOpen} onClose={() => setIsDiaryOpen(false)} title={`New Diary Entry · ${diaryGrade}`}>
              <form onSubmit={handleAddDiaryEntry} className="space-y-4 pt-1">
                <div className="grid grid-cols-2 gap-4">
                  <Select label="Entry Type" value={entryType} onChange={(e) => setEntryType(e.target.value)} options={[{ label: "Homework", value: "Homework" }, { label: "Remark", value: "Remark" }, { label: "Reminder", value: "Reminder" }]} />
                  <Select label="Subject" value={entrySubject} onChange={(e) => setEntrySubject(e.target.value)} options={["Mathematics", "English", "Hindi", "Science", "Social Science", "Computer Applications", "General"].map((s) => ({ label: s, value: s }))} />
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Note to Parents</label>
                  <textarea
                    value={entryText}
                    onChange={(e) => setEntryText(e.target.value)}
                    rows={4}
                    placeholder="e.g. Learn the periodic table groups 1-2 for Monday's oral quiz..."
                    className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition resize-none"
                  />
                </div>
                <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs text-slate-500 font-semibold leading-relaxed">
                  Publishing notifies all {diaryClassSize} parents of {diaryGrade}. Acknowledgement taps are counted live on the entry.
                </div>
                <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setIsDiaryOpen(false)}>Cancel</Button>
                  <Button type="submit">Publish to Parents</Button>
                </div>
              </form>
            </Dialog>
          </div>
        </TabsContent>
      </Tabs>
    </div>;
}
