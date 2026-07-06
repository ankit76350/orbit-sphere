/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, getStaff, getFees, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Sparkles,
  Bot,
  GraduationCap,
  School,
  MessageCircle,
  ShieldAlert,
  Cpu,
  Send,
  RefreshCw,
  FileText,
  BookOpen,
  Radar,
  AlertTriangle,
  CheckCircle,
  Camera,
  Bus,
  BedDouble,
  Gauge,
  SlidersHorizontal,
  Languages,
  ClipboardList,
  Wallet,
  CalendarDays,
  Play,
  Zap,
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

const hashNum = (str) => {
  let h = 7;
  for (let i = 0; i < String(str).length; i++) h = (h * 31 + String(str).charCodeAt(i)) >>> 0;
  return h;
};
const inr = (n) => `₹${Math.round(n).toLocaleString("en-IN")}`;

// ============ Seeds ============
const USAGE_SEED = {
  teacher: { label: "Teacher AI", used: 68500, quota: 200000, tier: "PREMIUM", desc: "Remarks, question papers, lesson plans" },
  principal: { label: "Principal AI", used: 41200, quota: 150000, tier: "PREMIUM", desc: "Admin copilot & early-warning radar" },
  parent: { label: "Parent AI", used: 22750, quota: 100000, tier: "BASIC", desc: "Helpdesk bot & weekly digests" },
  safety: { label: "Safety AI", used: 8100, quota: 50000, tier: "ENTERPRISE", desc: "CCTV vision, bus telemetry, hostel curfew" }
};

const AUDIT_SEED = [
  { id: "aud-seed-3", at: "2026-07-03 14:22:10", bundle: "Teacher AI", feature: "Report Remarks", detail: "Generated remark for Class 8 student (Encouraging, English). Approved by teacher.", tokens: 850, actor: "Meera Krishnan" },
  { id: "aud-seed-2", at: "2026-07-03 11:05:44", bundle: "Principal AI", feature: "Admin Copilot", detail: "Query: 'Fee collection this month?' answered from live fee ledger.", tokens: 1240, actor: "Principal" },
  { id: "aud-seed-1", at: "2026-07-02 09:41:03", bundle: "Safety AI", feature: "CCTV Anomaly", detail: "Loitering event at Rear Gate Cam-07 flagged for human review.", tokens: 400, actor: "System" }
];

const SAFETY_SEED = {
  cctv: [
    { id: "cctv-1", type: "Loitering", camera: "Rear Gate Cam-07", time: "2026-07-04 15:42", severity: "Medium", note: "Unidentified adult near boundary wall for 6+ minutes after dispersal.", acknowledged: false },
    { id: "cctv-2", type: "Intrusion", camera: "Lab Block Cam-03", time: "2026-07-04 21:18", severity: "High", note: "Motion detected in Chemistry Lab corridor after lockdown hours.", acknowledged: false },
    { id: "cctv-3", type: "Crowding", camera: "Canteen Cam-01", time: "2026-07-04 13:05", severity: "Low", note: "Density threshold crossed near juice counter during lunch break.", acknowledged: true },
    { id: "cctv-4", type: "Loitering", camera: "Bus Bay Cam-05", time: "2026-07-03 17:55", severity: "Medium", note: "Student group lingering in bus bay 25 minutes after last trip.", acknowledged: false }
  ],
  bus: [
    { id: "bus-1", vehicle: "BUS-402X", route: "Route Alpha (North)", speed: 67, limit: 50, location: "NH-48 Service Rd, Km 14", time: "2026-07-04 07:52", acknowledged: false },
    { id: "bus-2", vehicle: "COACH-77Y", route: "Route Gamma (South Peaks)", speed: 58, limit: 50, location: "Outer Ring Road Flyover", time: "2026-07-03 16:20", acknowledged: false },
    { id: "bus-3", vehicle: "BUS-110A", route: "Route Beta (Metro)", speed: 54, limit: 40, location: "School Zone, Sector 21", time: "2026-07-02 08:03", acknowledged: true }
  ],
  hostel: [
    { id: "hst-1", block: "Vanguard Hall (Boys)", event: "Curfew Anomaly", detail: "2 late biometric entries at 22:47 (curfew 22:00). Warden notified.", time: "2026-07-03 22:47", severity: "Medium", acknowledged: false },
    { id: "hst-2", block: "Seraphina House (Girls)", event: "Roll-call Gap", detail: "Bed 204-3 unaccounted at 21:30 roll call; resolved at 21:41 (infirmary visit).", time: "2026-07-02 21:30", severity: "Low", acknowledged: true }
  ]
};

const GOV_SEED = {
  remarksApproval: true,
  botCurriculumBound: true,
  riskHumanReview: true,
  cctvHumanConfirm: true,
  dataMinimisation: true,
  parentBotHandoff: false
};

// ============ Teacher AI canned banks ============
const STRENGTHS = [
  "consistent class participation",
  "neat and well-organised notebooks",
  "a strong conceptual grasp of new topics",
  "natural leadership during group activities",
  "genuine curiosity during science experiments",
  "excellent reading fluency and recitation"
];
const IMPROVEMENTS = [
  "time management during unit tests",
  "regularity in homework submission",
  "confidence while answering aloud in class",
  "attention to detail in calculations",
  "structured presentation of written answers"
];
const HINDI_REMARKS = [
  (n, g) => `${n} (${g}) इस सत्र में निरंतर प्रगति कर रहा/रही है। कक्षा में सक्रिय भागीदारी और गृहकार्य की नियमितता सराहनीय है। आगामी सत्र में परीक्षा के समय-प्रबंधन पर ध्यान देने से और बेहतर परिणाम मिलेंगे।`,
  (n, g) => `${n} ने ${g} में विषयों की अच्छी समझ विकसित की है। समूह गतिविधियों में नेतृत्व क्षमता उभर कर आई है। लिखित उत्तरों की प्रस्तुति पर थोड़ा और अभ्यास अपेक्षित है।`,
  (n, g) => `${n} का प्रदर्शन ${g} में संतोषजनक रहा है। पठन-कौशल उत्तम है; गणितीय गणनाओं में सावधानी बढ़ाने की आवश्यकता है। कुल मिलाकर एक परिश्रमी विद्यार्थी।`
];

const QUESTION_BANKS = {
  Maths: {
    chapterDefault: "Linear Equations in One Variable",
    mcq: [
      { q: "The solution of 3x − 7 = 8 is:", opts: ["x = 3", "x = 5", "x = 7", "x = 15"] },
      { q: "If 2(x + 4) = 18, then x equals:", opts: ["5", "7", "9", "13"] },
      { q: "Which of these is a linear equation in one variable?", opts: ["x² + 1 = 5", "2x + 3 = 9", "xy = 4", "x + y = 7"] },
      { q: "The root of x/4 = 6 is:", opts: ["1.5", "10", "24", "2"] },
      { q: "Transposing −5 from LHS to RHS changes its sign to:", opts: ["−5", "+5", "0", "1/5"] }
    ],
    short: [
      "Solve for x: 5x − 3 = 3x + 11. Verify your answer by substitution.",
      "The sum of three consecutive integers is 51. Frame a linear equation and find the integers.",
      "Ravi's age is three times his son's age. After 12 years it will be twice. Form the equation and solve.",
      "Solve: (2x − 1)/3 = (x + 2)/2 and check the solution."
    ],
    long: [
      "A boat travels 24 km upstream and returns downstream in a total of 5 hours. If the stream speed is 2 km/h, form a linear equation for the boat's still-water speed and solve it, showing every step of the transposition method.",
      "The perimeter of a rectangular school playground is 250 m. Its length exceeds twice its breadth by 5 m. Frame linear equations, find the dimensions, and calculate the cost of fencing at ₹85 per metre.",
      "A shopkeeper sells a school bag at 10% profit. Had he bought it for ₹50 less and sold for ₹15 more, profit would be 25%. Form the equation and find the original cost price."
    ]
  },
  Science: {
    chapterDefault: "Crop Production and Management",
    mcq: [
      { q: "Rabi crops are sown in:", opts: ["June–July", "October–November", "March–April", "January"] },
      { q: "The process of loosening and turning soil is called:", opts: ["Sowing", "Tilling", "Winnowing", "Threshing"] },
      { q: "Rhizobium bacteria in leguminous roots fix:", opts: ["Oxygen", "Carbon", "Nitrogen", "Phosphorus"] },
      { q: "Which implement is used for sowing seeds uniformly?", opts: ["Plough", "Seed drill", "Hoe", "Combine"] },
      { q: "Separation of grain from chaff is called:", opts: ["Harvesting", "Winnowing", "Irrigation", "Manuring"] }
    ],
    short: [
      "Differentiate between manure and fertiliser with two points and one example each.",
      "Why is it advised to grow leguminous crops in rotation with cereals? Explain the role of Rhizobium.",
      "List the steps of crop production in correct sequence and state the purpose of irrigation.",
      "What is meant by broadcasting? State one advantage of a seed drill over broadcasting."
    ],
    long: [
      "Describe the traditional and modern methods of irrigation. Explain drip irrigation with a labelled diagram and justify why it suits water-scarce regions of India.",
      "Explain the complete journey of a wheat crop from soil preparation to storage, naming the implement or process used at each stage and one precaution during grain storage.",
      "What are nitrogenous, phosphatic and potassic fertilisers? Discuss the effects of excessive fertiliser use on soil and water bodies, and suggest two sustainable alternatives."
    ]
  },
  English: {
    chapterDefault: "The Best Christmas Present in the World",
    mcq: [
      { q: "Choose the correct synonym of 'reluctant':", opts: ["Eager", "Unwilling", "Rapid", "Careless"] },
      { q: "Identify the tense: 'She has been reading since morning.'", opts: ["Past perfect", "Present perfect continuous", "Simple present", "Future perfect"] },
      { q: "Pick the correctly punctuated sentence:", opts: ["its raining outside", "It's raining outside.", "Its' raining outside.", "it's raining, Outside"] },
      { q: "A group of words with a subject and predicate is a:", opts: ["Phrase", "Clause", "Preposition", "Interjection"] },
      { q: "The plural of 'phenomenon' is:", opts: ["Phenomenons", "Phenomena", "Phenomenas", "Phenomenon"] }
    ],
    short: [
      "Write a diary entry (60–80 words) describing the day your class won the inter-house quiz.",
      "Change the narration: The teacher said to Ravi, \"Have you completed your project?\"",
      "Use the following in sentences of your own: (a) turn up (b) look after (c) give away.",
      "Punctuate: mother said were leaving for shimla on friday morning dont forget your sweater"
    ],
    long: [
      "Write a formal letter (100–120 words) to your Principal requesting the addition of Hindi storybooks and regional literature to the school library, giving at least three reasons.",
      "Develop a story (150 words) beginning with: 'The old banyan tree behind the school had a secret...' Give it a suitable title and moral.",
      "Write an article for the school magazine on 'Digital India: How Technology is Changing Our Classrooms' in about 150 words."
    ]
  }
};

const LESSON_TOPICS_HINT = {
  Maths: "e.g. Linear Equations, Mensuration",
  Science: "e.g. Crop Production, Friction",
  English: "e.g. Reported Speech, Letter Writing"
};

// ============ Component ============
export default function ModAiHub({ user }) {
  const { addToast } = useToast();

  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());
  const [fees] = useState(() => getFees());

  const [activeTab, setActiveTab] = useState("overview");

  // Persisted AI stores
  const [usage, setUsage] = useState(() => loadLS("erp_ai_usage", USAGE_SEED));
  const [audit, setAudit] = useState(() => loadLS("erp_ai_audit", AUDIT_SEED));
  const [remarksHistory, setRemarksHistory] = useState(() => loadLS("erp_ai_remarks", []));
  const [chat, setChat] = useState(() =>
    loadLS("erp_ai_chat", [
      { sender: "ai", text: "Namaste! I am Orbit, your Admin Copilot. Ask me about fee collection, defaulters, staff leave or transport compliance — I answer from your live ERP ledgers." }
    ])
  );
  const [riskReviewed, setRiskReviewed] = useState(() => loadLS("erp_ai_risk", []));
  const [safety, setSafety] = useState(() => loadLS("erp_ai_safety", SAFETY_SEED));
  const [gov, setGov] = useState(() => loadLS("erp_ai_governance", GOV_SEED));

  const doLog = (action, details) =>
    logAction(user?.id || "sandbox", user?.name || "User", user?.role || "Staff", action, details);

  // Every AI generation flows through here: usage meter + governance audit trail
  const recordAi = (bundleKey, bundleLabel, feature, detail, tokens) => {
    setUsage((prev) => {
      const next = { ...prev, [bundleKey]: { ...prev[bundleKey], used: prev[bundleKey].used + tokens } };
      saveLS("erp_ai_usage", next);
      return next;
    });
    setAudit((prev) => {
      const entry = {
        id: `aud-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
        at: new Date().toISOString().slice(0, 19).replace("T", " "),
        bundle: bundleLabel,
        feature,
        detail,
        tokens,
        actor: user?.name || "User"
      };
      const next = [entry, ...prev].slice(0, 120);
      saveLS("erp_ai_audit", next);
      return next;
    });
  };

  // ================= TEACHER AI state =================
  const [teacherTool, setTeacherTool] = useState("remarks");

  const grades = [...new Set(students.map((s) => s.grade))];
  const [remGrade, setRemGrade] = useState(() => (grades[0] ? grades[0] : "Grade 8"));
  const remStudents = students.filter((s) => s.grade === remGrade);
  const [remStudentId, setRemStudentId] = useState("");
  const [remTone, setRemTone] = useState("Encouraging");
  const [remLang, setRemLang] = useState("English");
  const [remBusy, setRemBusy] = useState(false);
  const [remOutput, setRemOutput] = useState(null);
  const [remVariant, setRemVariant] = useState(0);

  const buildRemark = (student, tone, lang, variant) => {
    const first = student.name.split(" ")[0];
    const h = hashNum(student.id) + variant;
    if (lang === "Hindi") return HINDI_REMARKS[h % HINDI_REMARKS.length](first, student.grade);
    const s1 = STRENGTHS[h % STRENGTHS.length];
    const s2 = STRENGTHS[(h + 3) % STRENGTHS.length];
    const imp = IMPROVEMENTS[(h + 1) % IMPROVEMENTS.length];
    if (tone === "Formal") {
      return `${student.name} has maintained a satisfactory academic standard in ${student.grade} this term. Notable strengths include ${s1} and ${s2}. It is recommended that focused attention be given to ${imp} in the coming term.`;
    }
    if (tone === "Direct") {
      return `${first} shows ${s1} — keep it up. However, ${imp} needs immediate and consistent effort. With a fixed daily study routine, ${first} can move into the top band of ${student.grade} next term.`;
    }
    return `${first} has had a wonderful term in ${student.grade}! ${first} impressed us with ${s1} and ${s2}. With a little more focus on ${imp}, we are confident ${first} will shine even brighter next term. Keep up the great spirit!`;
  };

  const handleGenerateRemark = (isRegen) => {
    const student = students.find((s) => s.id === remStudentId) || remStudents[0];
    if (!student) {
      addToast("No Student", "Pick a class and student first.", "warning");
      return;
    }
    setRemBusy(true);
    setRemOutput(null);
    const nextVariant = isRegen ? remVariant + 1 : 0;
    setTimeout(() => {
      setRemVariant(nextVariant);
      setRemOutput({ student, text: buildRemark(student, remTone, remLang, nextVariant), tone: remTone, lang: remLang });
      setRemBusy(false);
      recordAi("teacher", "Teacher AI", "Report Remarks", `Remark ${isRegen ? "regenerated" : "generated"} for ${student.name} (${remTone}, ${remLang}).`, 850);
    }, 1500);
  };

  const handleApproveRemark = () => {
    if (!remOutput) return;
    const entry = {
      id: `rem-${Date.now()}`,
      studentId: remOutput.student.id,
      studentName: remOutput.student.name,
      grade: remOutput.student.grade,
      tone: remOutput.tone,
      lang: remOutput.lang,
      text: remOutput.text,
      approvedBy: user?.name || "Teacher",
      at: new Date().toISOString().slice(0, 19).replace("T", " ")
    };
    const next = [entry, ...remarksHistory].slice(0, 50);
    setRemarksHistory(next);
    saveLS("erp_ai_remarks", next);
    doLog("AI Remark Approved", `Approved AI report-card remark for ${entry.studentName} (${entry.tone}, ${entry.lang}).`);
    addToast("Remark Saved", `Approved remark stored for ${entry.studentName}'s report card.`, "success");
    setRemOutput(null);
  };

  // Question paper
  const [qpSubject, setQpSubject] = useState("Maths");
  const [qpClass, setQpClass] = useState("Class 8");
  const [qpChapter, setQpChapter] = useState("");
  const [qpDifficulty, setQpDifficulty] = useState("Medium");
  const [qpBloom, setQpBloom] = useState("Understanding");
  const [qpProgress, setQpProgress] = useState(-1);
  const [qpPaper, setQpPaper] = useState(null);

  const handleGeneratePaper = () => {
    setQpProgress(0);
    setQpPaper(null);
    const interval = setInterval(() => {
      setQpProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          const bank = QUESTION_BANKS[qpSubject];
          const off = hashNum(`${qpDifficulty}-${qpBloom}`) % 2;
          const paper = {
            subject: qpSubject,
            klass: qpClass,
            chapter: qpChapter.trim() || bank.chapterDefault,
            difficulty: qpDifficulty,
            bloom: qpBloom,
            mcq: [0, 1, 2, 3].map((i) => bank.mcq[(i + off) % bank.mcq.length]),
            short: [0, 1, 2].map((i) => bank.short[(i + off) % bank.short.length]),
            long: [0, 1].map((i) => bank.long[(i + off) % bank.long.length])
          };
          setQpPaper(paper);
          recordAi("teacher", "Teacher AI", "Question Paper", `Generated ${qpSubject} paper (${qpClass}, ${qpDifficulty}, Bloom's: ${qpBloom}).`, 2100);
          addToast("Paper Generated", `${qpSubject} question paper drafted for ${qpClass} — review before use.`, "success");
          return -1;
        }
        return prev + 20;
      });
    }, 320);
  };

  // Lesson plan
  const [lpSubject, setLpSubject] = useState("Science");
  const [lpTopic, setLpTopic] = useState("");
  const [lpDuration, setLpDuration] = useState("40");
  const [lpBusy, setLpBusy] = useState(false);
  const [lpPlan, setLpPlan] = useState(null);

  const handleGenerateLesson = () => {
    setLpBusy(true);
    setLpPlan(null);
    setTimeout(() => {
      const topic = lpTopic.trim() || (lpSubject === "Maths" ? "Linear Equations" : lpSubject === "Science" ? "Crop Production" : "Reported Speech");
      const mins = parseInt(lpDuration) || 40;
      const seg = (f) => Math.max(3, Math.round(mins * f));
      setLpPlan({
        subject: lpSubject,
        topic,
        mins,
        outcomes: [
          `Learners can explain the core idea of ${topic} in their own words.`,
          `Learners can apply ${topic} to at least two real-life Indian contexts (market, farm, festival budgeting).`,
          `Learners can self-assess using the exit-ticket rubric aligned to NCERT learning outcomes.`
        ],
        materials: ["NCERT textbook & workbook", "Chart paper + sketch pens", "Flash cards / concrete manipulatives", "Smartboard slides (offline)"],
        fiveE: [
          { phase: "Engage", mins: seg(0.1), activity: `Hook: a 2-minute story/real-life puzzle on ${topic} (e.g. from a mandi or railway timetable). Quick think-pair-share.` },
          { phase: "Explore", mins: seg(0.25), activity: "Small groups investigate guided task cards with manipulatives; teacher circulates with probing questions, no direct answers." },
          { phase: "Explain", mins: seg(0.25), activity: `Groups present findings; teacher consolidates formal vocabulary and the NCERT definition/procedure for ${topic} on the board.` },
          { phase: "Elaborate", mins: seg(0.25), activity: "Differentiated practice: core worksheet for all, challenge extension for advanced learners, scaffolded version for support group." },
          { phase: "Evaluate", mins: seg(0.15), activity: "Exit ticket: 3 quick items + one 'explain it to a friend' line. Collect for formative record." }
        ],
        assessment: "Formative: exit tickets + observation checklist. Assignment: 5 practice questions from NCERT exercise. Next class starts with a 3-minute recap quiz."
      });
      setLpBusy(false);
      recordAi("teacher", "Teacher AI", "Lesson Plan", `Generated ${mins}-min 5E lesson plan: ${lpSubject} — ${topic}.`, 1600);
      addToast("Lesson Plan Ready", `5E plan for "${topic}" (${mins} min) drafted in NCERT style.`, "success");
    }, 1700);
  };

  // ================= PRINCIPAL AI =================
  const [chatInput, setChatInput] = useState("");
  const [chatTyping, setChatTyping] = useState(false);

  const pushChat = (msgs) => {
    setChat((prev) => {
      const next = [...prev, ...msgs].slice(-40);
      saveLS("erp_ai_chat", next);
      return next;
    });
  };

  const answerQuery = (raw) => {
    const q = raw.toLowerCase();
    if (q.includes("fee") || q.includes("collection")) {
      const collected = fees.reduce((s, f) => s + (f.paidAmount || 0), 0);
      const total = fees.reduce((s, f) => s + (f.amount || 0), 0);
      const pending = total - collected;
      const pct = total ? Math.round((collected / total) * 100) : 0;
      return {
        text: `Fee position for this cycle: ${inr(collected)} collected out of ${inr(total)} billed (${pct}%). ${inr(pending)} is still pending across ${fees.filter((f) => f.status !== "Paid").length} open invoices. Tuition head is the largest pending bucket — recommend a reminder blast before the 15th.`,
        chart: [
          { label: "Collected", value: collected },
          { label: "Pending", value: pending }
        ]
      };
    }
    if (q.includes("defaulter") || q.includes("class 8") || q.includes("unpaid")) {
      const g8 = students.filter((s) => s.grade === "Grade 8").map((s) => s.id);
      const defInvoices = fees.filter((f) => f.status === "Unpaid" && g8.includes(f.studentId));
      const names = [...new Set(defInvoices.map((f) => f.studentName))];
      const amt = defInvoices.reduce((s, f) => s + (f.amount - (f.paidAmount || 0)), 0);
      return {
        text: names.length
          ? `Class 8 has ${names.length} fee defaulters with ${defInvoices.length} unpaid invoices totalling ${inr(amt)}. Top names: ${names.slice(0, 5).join(", ")}${names.length > 5 ? "…" : ""}. Suggest routing these to the counsellor-first follow-up track rather than a blunt reminder.`
          : "Good news — Class 8 currently has zero fully-unpaid invoices on the ledger.",
        chart: null
      };
    }
    if (q.includes("leave") || q.includes("staff")) {
      const teachers = staff.filter((st) => st.role === "Teacher");
      const onLeave = staff.filter((_, i) => i % 9 === 4).slice(0, 2);
      return {
        text: `Staff snapshot: ${staff.length} on rolls (${teachers.length} teaching). Today ${onLeave.length} are on approved leave: ${onLeave.map((s) => `${s.name} (${s.department})`).join(", ") || "—"}. Substitution grid is covered; Period 5 in Grade 7-B needs a proctor.`,
        chart: null
      };
    }
    if (q.includes("bus") || q.includes("insurance") || q.includes("transport")) {
      return {
        text: "Transport compliance: BUS-402X insurance expires on 28-Jul-2026 (24 days) and its speed governor certificate on 04-Aug-2026. COACH-77Y fitness certificate renewal is due 12-Sep-2026. BUS-110A is fully compliant. Recommend booking BUS-402X for renewal this week to avoid RTO penalty.",
        chart: null
      };
    }
    return {
      text: "I can answer from live ERP data on: fee collection & defaulters, staff on leave, transport/insurance compliance, and enrolment. Try one of the suggested prompts below.",
      chart: null
    };
  };

  const handleCopilotSend = (preset) => {
    const text = (preset !== undefined ? preset : chatInput).trim();
    if (!text) return;
    pushChat([{ sender: "user", text }]);
    setChatInput("");
    setChatTyping(true);
    setTimeout(() => {
      const ans = answerQuery(text);
      setChatTyping(false);
      pushChat([{ sender: "ai", text: ans.text, chart: ans.chart }]);
      recordAi("principal", "Principal AI", "Admin Copilot", `Copilot answered: "${text}"`, 1240);
    }, 1000);
  };

  // Early-warning radar (computed once, deterministic-ish)
  const [riskList] = useState(() => {
    const st = getStudents();
    const fe = getFees();
    return st
      .slice(0, 10)
      .map((s) => {
        const h = hashNum(s.id);
        const attendance = 68 + (h % 30);
        const attDrop = 4 + (h % 12);
        const overdue = fe.filter((f) => f.studentId === s.id && f.status !== "Paid").length;
        const gradeDecline = h % 3 === 0;
        let score = 0;
        const reasons = [];
        if (attendance < 80) {
          score += 35;
          reasons.push(`Attendance ${attendance}% (dropped ${attDrop}%)`);
        } else if (attendance < 90) {
          score += 15;
          reasons.push(`Attendance dipped to ${attendance}%`);
        }
        if (overdue > 0) {
          score += Math.min(overdue * 14, 28);
          reasons.push(`${overdue} overdue invoice${overdue > 1 ? "s" : ""}`);
        }
        if (gradeDecline) {
          score += 25;
          reasons.push("Grade decline in Maths (2 tests)");
        }
        score += h % 12;
        const severity = score >= 60 ? "High" : score >= 35 ? "Medium" : "Low";
        const intervention =
          severity === "High"
            ? "Schedule parent meeting this week; pair with peer mentor; counsellor check-in within 3 days."
            : severity === "Medium"
            ? "Class teacher to have a 1:1 chat; send gentle fee-plan option to parent; monitor for 2 weeks."
            : "No action needed beyond routine monitoring; celebrate a recent win to reinforce engagement.";
        return { studentId: s.id, name: s.name, grade: s.grade, score: Math.min(score, 96), reasons, severity, intervention };
      })
      .filter((r) => r.reasons.length > 0)
      .sort((a, b) => b.score - a.score);
  });

  const handleMarkReviewed = (r) => {
    const entry = { studentId: r.studentId, name: r.name, reviewedBy: user?.name || "Principal", at: new Date().toISOString().slice(0, 19).replace("T", " ") };
    const next = [...riskReviewed.filter((x) => x.studentId !== r.studentId), entry];
    setRiskReviewed(next);
    saveLS("erp_ai_risk", next);
    doLog("Risk Case Reviewed", `Early-warning case reviewed for ${r.name} (score ${r.score}).`);
    addToast("Marked Reviewed", `${r.name}'s risk case logged as human-reviewed.`, "success");
  };

  // ================= PARENT AI =================
  const [waLang, setWaLang] = useState("en");
  const [waChat, setWaChat] = useState([
    { sender: "ai", text: "🙏 Welcome to St. Jude's Assistant! Tap a quick question below — replies in English or हिंदी." }
  ]);
  const [waTyping, setWaTyping] = useState(false);

  const demoStudent = students[0];
  const demoDue = demoStudent
    ? fees.filter((f) => f.studentId === demoStudent.id).reduce((s, f) => s + (f.amount - (f.paidAmount || 0)), 0)
    : 0;

  const WA_QA = {
    fee: {
      en: () => `Dear parent, pending fees for ${demoStudent ? demoStudent.name : "your ward"}: ${inr(demoDue)} (next instalment due 15-Jul-2026). Pay via the parent app in one tap — UPI, card or wallet. Receipt is issued instantly.`,
      hi: () => `प्रिय अभिभावक, ${demoStudent ? demoStudent.name : "आपके बच्चे"} की बकाया फीस ${inr(demoDue)} है (अगली किस्त 15-जुलाई-2026)। पेरेंट ऐप से UPI/कार्ड द्वारा एक टैप में भुगतान करें। रसीद तुरंत मिलेगी।`
    },
    bus: {
      en: () => `Bus BUS-402X (Route Alpha) is currently near Civic Center stop, running 4 minutes late. ETA at your stop: 4:22 PM. Live GPS tracking is available in the parent app under Transport.`,
      hi: () => `बस BUS-402X (रूट अल्फ़ा) अभी सिविक सेंटर स्टॉप के पास है, 4 मिनट देरी से। आपके स्टॉप पर अनुमानित समय: 4:22 PM। लाइव GPS ट्रैकिंग पेरेंट ऐप के 'ट्रांसपोर्ट' में उपलब्ध है।`
    },
    hw: {
      en: () => `Today's homework: Maths — NCERT Ex 2.3 Q1–Q5; Science — draw the drip-irrigation diagram; English — read chapter 4 and note 5 new words. Submission: tomorrow, Period 1.`,
      hi: () => `आज का गृहकार्य: गणित — NCERT अभ्यास 2.3 प्रश्न 1–5; विज्ञान — ड्रिप सिंचाई का चित्र; अंग्रेज़ी — अध्याय 4 पढ़कर 5 नए शब्द लिखें। जमा करने की तिथि: कल, पहला पीरियड।`
    },
    ptm: {
      en: () => `Next Parent-Teacher Meeting: Saturday, 12-Jul-2026, 9:00 AM – 12:30 PM in the respective classrooms. Slot booking opens 08-Jul in the parent app. HPC Term-1 cards will be shared during the PTM.`,
      hi: () => `अगली अभिभावक-शिक्षक बैठक: शनिवार, 12-जुलाई-2026, सुबह 9:00 से 12:30 बजे तक, संबंधित कक्षाओं में। स्लॉट बुकिंग 08-जुलाई से पेरेंट ऐप में खुलेगी। HPC टर्म-1 कार्ड PTM में दिए जाएँगे।`
    }
  };

  const handleWaChip = (key, label) => {
    setWaChat((prev) => [...prev, { sender: "user", text: label }]);
    setWaTyping(true);
    setTimeout(() => {
      setWaTyping(false);
      setWaChat((prev) => [...prev, { sender: "ai", text: WA_QA[key][waLang]() }]);
      recordAi("parent", "Parent AI", "Helpdesk Bot", `Parent bot answered "${label}" (${waLang === "hi" ? "Hindi" : "English"}).`, 480);
    }, 1100);
  };

  // Weekly digest
  const [digestStudentId, setDigestStudentId] = useState(() => (getStudents()[0] ? getStudents()[0].id : ""));
  const digestStudent = students.find((s) => s.id === digestStudentId);
  const digestDue = digestStudent
    ? fees.filter((f) => f.studentId === digestStudent.id).reduce((s, f) => s + (f.amount - (f.paidAmount || 0)), 0)
    : 0;
  const digestHash = digestStudent ? hashNum(digestStudent.id) : 0;
  const weekStrip = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((d, i) => ({
    day: d,
    mark: (digestHash + i) % 7 === 2 ? "A" : (digestHash + i) % 11 === 3 ? "L" : "P"
  }));

  const handleSendDigest = () => {
    if (!digestStudent) return;
    recordAi("parent", "Parent AI", "Weekly Digest", `Weekly digest compiled & sent for ${digestStudent.name}.`, 620);
    doLog("Weekly Digest Sent", `AI weekly digest for ${digestStudent.name} dispatched to parent via WhatsApp.`);
    addToast("Digest Sent", `Weekly digest for ${digestStudent.name} delivered to ${digestStudent.parentName} on WhatsApp.`, "success");
  };

  // ================= SAFETY AI =================
  const [clipEvent, setClipEvent] = useState(null);

  const ackSafety = (section, id) => {
    const next = { ...safety, [section]: safety[section].map((e) => (e.id === id ? { ...e, acknowledged: true } : e)) };
    setSafety(next);
    saveLS("erp_ai_safety", next);
    doLog("Safety Alert Acknowledged", `Acknowledged ${section} alert ${id}.`);
    addToast("Acknowledged", "Alert marked as human-reviewed and closed on the safety board.", "success");
  };

  // ================= GOVERNANCE =================
  const toggleGov = (key) => {
    const next = { ...gov, [key]: !gov[key] };
    setGov(next);
    saveLS("erp_ai_governance", next);
    doLog("AI Governance Changed", `Toggled "${key}" to ${next[key] ? "ON" : "OFF"}.`);
    addToast("Governance Updated", `Human-in-the-loop setting "${key}" is now ${next[key] ? "ON" : "OFF"}.`, "info");
  };

  const totalTokens = Object.values(usage).reduce((s, u) => s + u.used, 0);
  const totalQuota = Object.values(usage).reduce((s, u) => s + u.quota, 0);
  const monthCost = (totalTokens / 1000) * 0.42;

  // ================= Small shared UI =================
  const Meter = ({ used, quota, color = "bg-blue-600" }) => (
    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
      <div className={`${color} h-full rounded-full transition-all duration-500`} style={{ width: `${Math.min(100, Math.round((used / quota) * 100))}%` }} />
    </div>
  );

  const Toggle = ({ on, onClick }) => (
    <button
      onClick={onClick}
      className={`w-10 h-6 rounded-full transition relative shrink-0 ${on ? "bg-emerald-500" : "bg-slate-300"}`}
    >
      <span className={`absolute top-1 h-4 w-4 bg-white rounded-full shadow transition-all ${on ? "left-5" : "left-1"}`} />
    </button>
  );

  const TypingDots = () => (
    <div className="flex gap-1 items-center p-3">
      <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" />
      <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "120ms" }} />
      <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "240ms" }} />
    </div>
  );

  const tabs = [
    { key: "overview", label: "Overview" },
    { key: "teacher_ai", label: "Teacher AI" },
    { key: "principal_ai", label: "Principal AI" },
    { key: "parent_ai", label: "Parent AI" },
    { key: "safety_ai", label: "Safety AI" },
    { key: "governance", label: "Governance" }
  ];

  const bundleMeta = [
    { key: "teacher", icon: GraduationCap, color: "bg-blue-600", chip: "info" },
    { key: "principal", icon: School, color: "bg-indigo-600", chip: "secondary" },
    { key: "parent", icon: MessageCircle, color: "bg-emerald-600", chip: "success" },
    { key: "safety", icon: ShieldAlert, color: "bg-rose-600", chip: "danger" }
  ];

  return (
    <div className="space-y-6">
      {/* Header banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-violet-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Sparkles className="h-6 w-6 text-violet-600" />
            AI Suite (Orbit Intelligence)
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Persona-bundled AI copilots for teachers, principals, parents and campus safety — every generation is human-approved and audit-logged.
          </p>
        </div>
        <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
          {totalTokens.toLocaleString("en-IN")} / {totalQuota.toLocaleString("en-IN")} TOKENS
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

      {/* ================= 1. OVERVIEW ================= */}
      {activeTab === "overview" && (
        <div className="space-y-6">
          <div className="bg-indigo-900 text-white p-6 rounded-3xl border border-slate-800 relative overflow-hidden">
            <div className="absolute right-0 bottom-0 -mb-10 -mr-10 h-44 w-44 bg-violet-400 rounded-full blur-3xl opacity-20 pointer-events-none" />
            <span className="text-[10px] uppercase tracking-widest font-extrabold text-indigo-300">Orbit Intelligence Platform</span>
            <h3 className="text-2xl font-black mt-1">Four AI bundles. One human-in-the-loop spine.</h3>
            <p className="text-xs text-indigo-200 font-semibold mt-2 max-w-2xl leading-relaxed">
              Teacher AI drafts, teachers approve. Principal AI answers from live ERP ledgers. Parent AI speaks English and हिंदी on WhatsApp. Safety AI watches
              cameras, buses and hostels — and always escalates to a human. Usage is metered per bundle and every generation lands in the governance audit log.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {bundleMeta.map((b) => {
              const u = usage[b.key];
              const Icon = b.icon;
              const pct = Math.round((u.used / u.quota) * 100);
              return (
                <div key={b.key} className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3">
                  <div className="flex justify-between items-start">
                    <div className={`p-2.5 ${b.color} text-white rounded-2xl`}>
                      <Icon className="h-5 w-5" />
                    </div>
                    <Badge variant={u.tier === "ENTERPRISE" ? "danger" : u.tier === "PREMIUM" ? "secondary" : "default"} className="text-[9px] font-black">
                      {u.tier}
                    </Badge>
                  </div>
                  <div>
                    <h4 className="text-sm font-black text-slate-800">{u.label}</h4>
                    <p className="text-[10px] text-slate-400 font-semibold mt-0.5">{u.desc}</p>
                  </div>
                  <Meter used={u.used} quota={u.quota} color={pct > 80 ? "bg-rose-500" : b.color} />
                  <p className="text-[10px] font-mono font-bold text-slate-500">
                    {u.used.toLocaleString("en-IN")} / {u.quota.toLocaleString("en-IN")} tokens ({pct}%)
                  </p>
                </div>
              );
            })}
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-5 flex flex-wrap gap-3 items-center text-[11px] font-bold text-slate-500">
            <Zap className="h-4 w-4 text-amber-500" />
            Tier gating:
            <Badge variant="default" className="text-[9px] font-black">BASIC — Parent bot, digests</Badge>
            <Badge variant="secondary" className="text-[9px] font-black">PREMIUM — Teacher & Principal copilots</Badge>
            <Badge variant="danger" className="text-[9px] font-black">ENTERPRISE — Safety AI vision + telemetry</Badge>
          </div>
        </div>
      )}

      {/* ================= 2. TEACHER AI ================= */}
      {activeTab === "teacher_ai" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { key: "remarks", icon: FileText, title: "Report-Card Remarks", desc: "Tone & language aware remarks, teacher-approved before saving." },
              { key: "paper", icon: ClipboardList, title: "Question Paper Generator", desc: "Blueprint-based papers with Bloom's alignment & marks scheme." },
              { key: "lesson", icon: BookOpen, title: "Lesson Plan Generator", desc: "NCERT-style 5E plans with outcomes, materials & assessment." }
            ].map((c) => {
              const Icon = c.icon;
              const active = teacherTool === c.key;
              return (
                <button
                  key={c.key}
                  onClick={() => setTeacherTool(c.key)}
                  className={`text-left p-5 rounded-3xl border transition ${
                    active ? "bg-blue-50/60 border-blue-300 shadow-xs" : "bg-white border-slate-100 hover:bg-slate-50"
                  }`}
                >
                  <Icon className={`h-5 w-5 ${active ? "text-blue-600" : "text-slate-400"}`} />
                  <h4 className="text-sm font-black text-slate-800 mt-2">{c.title}</h4>
                  <p className="text-[10px] text-slate-400 font-semibold mt-1 leading-relaxed">{c.desc}</p>
                </button>
              );
            })}
          </div>

          {/* --- Remarks tool --- */}
          {teacherTool === "remarks" && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <div className="lg:col-span-5 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 h-fit">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">Remark Parameters</h3>
                <Select
                  label="Class"
                  options={grades.map((g) => ({ label: g, value: g }))}
                  value={remGrade}
                  onChange={(e) => {
                    setRemGrade(e.target.value);
                    setRemStudentId("");
                  }}
                />
                <Select
                  label="Student"
                  options={[{ label: "— Select student —", value: "" }, ...remStudents.map((s) => ({ label: `${s.name} (${s.admissionNumber})`, value: s.id }))]}
                  value={remStudentId}
                  onChange={(e) => setRemStudentId(e.target.value)}
                />
                <div className="grid grid-cols-2 gap-4">
                  <Select
                    label="Tone"
                    options={[
                      { label: "Encouraging", value: "Encouraging" },
                      { label: "Formal", value: "Formal" },
                      { label: "Direct", value: "Direct" }
                    ]}
                    value={remTone}
                    onChange={(e) => setRemTone(e.target.value)}
                  />
                  <Select
                    label="Language"
                    options={[
                      { label: "English", value: "English" },
                      { label: "Hindi (हिंदी)", value: "Hindi" }
                    ]}
                    value={remLang}
                    onChange={(e) => setRemLang(e.target.value)}
                  />
                </div>
                <Button onClick={() => handleGenerateRemark(false)} disabled={remBusy} className="w-full text-xs py-2.5 flex items-center gap-2">
                  {remBusy ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                  {remBusy ? "Generating..." : "Generate Remark"}
                </Button>
                {gov.remarksApproval && (
                  <p className="text-[10px] text-slate-400 font-bold flex items-center gap-1.5">
                    <ShieldAlert className="h-3.5 w-3.5 text-emerald-500" /> Governance: remarks require teacher approval before saving.
                  </p>
                )}
              </div>

              <div className="lg:col-span-7 space-y-4">
                <div className="bg-white border border-slate-100 rounded-3xl p-6 min-h-[180px]">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3 mb-4">AI Draft</h3>
                  {remBusy ? (
                    <div className="space-y-3 animate-pulse">
                      <div className="h-3 bg-slate-100 rounded-full w-11/12" />
                      <div className="h-3 bg-slate-100 rounded-full w-full" />
                      <div className="h-3 bg-slate-100 rounded-full w-4/5" />
                      <p className="text-[10px] text-blue-600 font-black uppercase tracking-widest pt-2">Composing remark…</p>
                    </div>
                  ) : remOutput ? (
                    <div className="space-y-4">
                      <div className="p-4 bg-violet-50/60 border border-violet-100 rounded-2xl">
                        <p className="text-sm text-slate-800 font-semibold leading-relaxed">{remOutput.text}</p>
                        <p className="text-[9px] text-slate-400 font-black uppercase tracking-wider mt-3">
                          {remOutput.student.name} • {remOutput.tone} • {remOutput.lang} • Variant {remVariant + 1}
                        </p>
                      </div>
                      <div className="flex gap-3 justify-end">
                        <Button variant="outline" onClick={() => handleGenerateRemark(true)} className="text-xs flex items-center gap-2">
                          <RefreshCw className="h-4 w-4" /> Regenerate
                        </Button>
                        <Button onClick={handleApproveRemark} className="text-xs flex items-center gap-2">
                          <CheckCircle className="h-4 w-4" /> Approve & Save
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">Set parameters and generate — the draft appears here for your review.</p>
                  )}
                </div>

                <div className="bg-white border border-slate-100 rounded-3xl p-6">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest mb-3">Approved Remark History ({remarksHistory.length})</h3>
                  {remarksHistory.length === 0 ? (
                    <p className="text-xs text-slate-400 italic">No remarks approved yet.</p>
                  ) : (
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                      {remarksHistory.slice(0, 8).map((r) => (
                        <div key={r.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl">
                          <div className="flex justify-between items-center gap-2">
                            <span className="text-xs font-extrabold text-slate-800">{r.studentName} <span className="text-slate-400 font-bold">• {r.grade}</span></span>
                            <Badge variant="info" className="text-[9px] font-black shrink-0">{r.lang}</Badge>
                          </div>
                          <p className="text-[11px] text-slate-500 font-semibold mt-1 leading-relaxed">{r.text}</p>
                          <p className="text-[9px] text-slate-400 font-bold mt-1">Approved by {r.approvedBy} • {r.at}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* --- Question paper tool --- */}
          {teacherTool === "paper" && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 h-fit">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">Paper Blueprint</h3>
                <Select
                  label="Subject"
                  options={[
                    { label: "Mathematics", value: "Maths" },
                    { label: "Science", value: "Science" },
                    { label: "English", value: "English" }
                  ]}
                  value={qpSubject}
                  onChange={(e) => setQpSubject(e.target.value)}
                />
                <Select
                  label="Class"
                  options={["Class 6", "Class 7", "Class 8", "Class 9", "Class 10"].map((c) => ({ label: c, value: c }))}
                  value={qpClass}
                  onChange={(e) => setQpClass(e.target.value)}
                />
                <Input
                  label="Chapter / Topic"
                  value={qpChapter}
                  onChange={(e) => setQpChapter(e.target.value)}
                  placeholder={QUESTION_BANKS[qpSubject].chapterDefault}
                />
                <div className="grid grid-cols-2 gap-4">
                  <Select
                    label="Difficulty"
                    options={["Easy", "Medium", "Hard"].map((d) => ({ label: d, value: d }))}
                    value={qpDifficulty}
                    onChange={(e) => setQpDifficulty(e.target.value)}
                  />
                  <Select
                    label="Bloom's Level"
                    options={["Remembering", "Understanding", "Applying", "Analysing", "Evaluating", "Creating"].map((b) => ({ label: b, value: b }))}
                    value={qpBloom}
                    onChange={(e) => setQpBloom(e.target.value)}
                  />
                </div>
                {qpProgress >= 0 ? (
                  <div className="space-y-2 pt-1">
                    <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
                      <div className="bg-blue-600 h-full rounded-full transition-all duration-300" style={{ width: `${qpProgress}%` }} />
                    </div>
                    <p className="text-[10px] text-blue-600 font-black uppercase tracking-widest">Assembling blueprint… {qpProgress}%</p>
                  </div>
                ) : (
                  <Button onClick={handleGeneratePaper} className="w-full text-xs py-2.5 flex items-center gap-2">
                    <Sparkles className="h-4 w-4" /> Generate Question Paper
                  </Button>
                )}
              </div>

              <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
                <div className="flex justify-between items-center border-b border-slate-50 pb-3">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Generated Paper</h3>
                  {qpPaper && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => {
                        doLog("Question Paper Exported", `Exported ${qpPaper.subject} paper (${qpPaper.klass}) to the question bank.`);
                        addToast("Exported", "Paper added to the institutional question bank with blueprint tags.", "success");
                      }}
                      className="text-[10px]"
                    >
                      Export to Question Bank
                    </Button>
                  )}
                </div>
                {!qpPaper ? (
                  <p className="text-xs text-slate-400 italic p-8 text-center">Configure the blueprint and generate — the formatted paper renders here.</p>
                ) : (
                  <div className="space-y-5">
                    <div className="text-center border-b border-dashed border-slate-200 pb-4">
                      <h4 className="text-sm font-black text-slate-800 uppercase tracking-wide">St. Jude's Senior Secondary School</h4>
                      <p className="text-xs font-bold text-slate-600 mt-1">{qpPaper.subject} — {qpPaper.klass} • Unit Test</p>
                      <p className="text-[10px] text-slate-400 font-bold mt-0.5">
                        Chapter: {qpPaper.chapter} • Difficulty: {qpPaper.difficulty} • Bloom's: {qpPaper.bloom} • Max Marks: 20 • Time: 60 min
                      </p>
                    </div>

                    <div>
                      <h5 className="text-[10px] font-black uppercase tracking-widest text-blue-700 mb-2">Section A — MCQ (4 × 1 = 4 marks)</h5>
                      <ol className="space-y-2.5 list-decimal pl-5">
                        {qpPaper.mcq.map((m, i) => (
                          <li key={i} className="text-xs font-semibold text-slate-700">
                            {m.q}
                            <div className="flex flex-wrap gap-x-5 gap-y-1 mt-1 text-[11px] text-slate-500 font-semibold">
                              {m.opts.map((o, j) => (
                                <span key={j}>({String.fromCharCode(97 + j)}) {o}</span>
                              ))}
                            </div>
                          </li>
                        ))}
                      </ol>
                    </div>

                    <div>
                      <h5 className="text-[10px] font-black uppercase tracking-widest text-blue-700 mb-2">Section B — Short Answer (3 × 2 = 6 marks)</h5>
                      <ol className="space-y-2 list-decimal pl-5" start={5}>
                        {qpPaper.short.map((q, i) => (
                          <li key={i} className="text-xs font-semibold text-slate-700">{q}</li>
                        ))}
                      </ol>
                    </div>

                    <div>
                      <h5 className="text-[10px] font-black uppercase tracking-widest text-blue-700 mb-2">Section C — Long Answer (2 × 5 = 10 marks)</h5>
                      <ol className="space-y-2 list-decimal pl-5" start={8}>
                        {qpPaper.long.map((q, i) => (
                          <li key={i} className="text-xs font-semibold text-slate-700">{q}</li>
                        ))}
                      </ol>
                    </div>

                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl text-[10px] font-bold text-slate-500 leading-relaxed">
                      Marks Scheme: Sec A — 1 mark each, no negative marking. Sec B — 2 marks (1 concept + 1 working). Sec C — 5 marks (2 method + 2 accuracy + 1 presentation).
                      Internal choice may be added in Sec C during moderation.
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* --- Lesson plan tool --- */}
          {teacherTool === "lesson" && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 h-fit">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">Plan Parameters</h3>
                <Select
                  label="Subject"
                  options={[
                    { label: "Mathematics", value: "Maths" },
                    { label: "Science", value: "Science" },
                    { label: "English", value: "English" }
                  ]}
                  value={lpSubject}
                  onChange={(e) => setLpSubject(e.target.value)}
                />
                <Input label="Topic" value={lpTopic} onChange={(e) => setLpTopic(e.target.value)} placeholder={LESSON_TOPICS_HINT[lpSubject]} />
                <Select
                  label="Duration"
                  options={[
                    { label: "40 minutes (1 period)", value: "40" },
                    { label: "60 minutes (extended)", value: "60" },
                    { label: "90 minutes (double period)", value: "90" }
                  ]}
                  value={lpDuration}
                  onChange={(e) => setLpDuration(e.target.value)}
                />
                <Button onClick={handleGenerateLesson} disabled={lpBusy} className="w-full text-xs py-2.5 flex items-center gap-2">
                  {lpBusy ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                  {lpBusy ? "Drafting 5E plan..." : "Generate Lesson Plan"}
                </Button>
              </div>

              <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">NCERT-Style 5E Plan</h3>
                {lpBusy ? (
                  <div className="space-y-3 animate-pulse p-4">
                    <div className="h-3 bg-slate-100 rounded-full w-2/3" />
                    <div className="h-3 bg-slate-100 rounded-full w-full" />
                    <div className="h-3 bg-slate-100 rounded-full w-5/6" />
                    <div className="h-3 bg-slate-100 rounded-full w-3/4" />
                    <p className="text-[10px] text-blue-600 font-black uppercase tracking-widest pt-2">Mapping learning outcomes & 5E activities…</p>
                  </div>
                ) : !lpPlan ? (
                  <p className="text-xs text-slate-400 italic p-8 text-center">Choose subject, topic and duration to draft a plan.</p>
                ) : (
                  <div className="space-y-4">
                    <div className="p-4 bg-indigo-900 text-white rounded-2xl">
                      <span className="text-[9px] uppercase tracking-widest font-extrabold text-indigo-300">Lesson Plan</span>
                      <h4 className="text-base font-black mt-1">{lpPlan.subject}: {lpPlan.topic}</h4>
                      <p className="text-[10px] text-indigo-200 font-bold">{lpPlan.mins} minutes • 5E instructional model • NEP-aligned</p>
                    </div>
                    <div>
                      <h5 className="text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2">Learning Outcomes</h5>
                      <ul className="list-disc pl-5 space-y-1 text-xs font-semibold text-slate-700">
                        {lpPlan.outcomes.map((o, i) => (<li key={i}>{o}</li>))}
                      </ul>
                    </div>
                    <div>
                      <h5 className="text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2">Materials</h5>
                      <div className="flex flex-wrap gap-2">
                        {lpPlan.materials.map((m, i) => (<Badge key={i} variant="default" className="text-[10px]">{m}</Badge>))}
                      </div>
                    </div>
                    <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                      <table className="w-full text-xs font-semibold text-slate-700 text-left">
                        <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                          <tr>
                            <th className="p-3">Phase</th>
                            <th className="p-3">Min</th>
                            <th className="p-3">Activity</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {lpPlan.fiveE.map((p) => (
                            <tr key={p.phase}>
                              <td className="p-3 font-black text-blue-700">{p.phase}</td>
                              <td className="p-3 font-mono text-[10px]">{p.mins}′</td>
                              <td className="p-3 leading-relaxed">{p.activity}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div className="p-4 bg-emerald-50/60 border border-emerald-100 rounded-2xl text-[11px] font-semibold text-slate-700 leading-relaxed">
                      <span className="font-black text-emerald-700 uppercase text-[9px] tracking-widest block mb-1">Assessment & Follow-up</span>
                      {lpPlan.assessment}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ================= 3. PRINCIPAL AI ================= */}
      {activeTab === "principal_ai" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Admin Copilot */}
          <div className="lg:col-span-6 bg-white border border-slate-100 rounded-3xl overflow-hidden flex flex-col h-[560px]">
            <div className="bg-slate-900 p-4 text-white flex items-center justify-between shrink-0">
              <div className="flex items-center gap-2">
                <Bot className="h-5 w-5 text-violet-400" />
                <div>
                  <h4 className="text-xs font-black uppercase tracking-wider">Admin Copilot</h4>
                  <span className="text-[8px] text-slate-400 font-bold block mt-0.5">Answers computed live from ERP ledgers</span>
                </div>
              </div>
              <Badge variant="success" className="text-[8px] font-black uppercase">Grounded</Badge>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50">
              {chat.map((m, idx) => (
                <div key={idx} className={`flex ${m.sender === "user" ? "justify-end" : "justify-start"}`}>
                  <div
                    className={`max-w-[85%] p-3.5 rounded-2xl text-xs font-semibold leading-relaxed ${
                      m.sender === "user" ? "bg-blue-600 text-white rounded-tr-none" : "bg-white border border-slate-200 text-slate-800 rounded-tl-none"
                    }`}
                  >
                    {m.text}
                    {m.chart && (
                      <div className="mt-3 space-y-1.5">
                        {m.chart.map((bar) => {
                          const max = Math.max(...m.chart.map((b) => b.value), 1);
                          return (
                            <div key={bar.label} className="space-y-0.5">
                              <div className="flex justify-between text-[9px] font-black uppercase tracking-wider text-slate-400">
                                <span>{bar.label}</span>
                                <span>{inr(bar.value)}</span>
                              </div>
                              <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                                <div
                                  className={`h-full rounded-full ${bar.label === "Pending" ? "bg-rose-500" : "bg-emerald-500"}`}
                                  style={{ width: `${Math.round((bar.value / max) * 100)}%` }}
                                />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              {chatTyping && (
                <div className="flex justify-start">
                  <div className="bg-white border border-slate-200 rounded-2xl rounded-tl-none"><TypingDots /></div>
                </div>
              )}
            </div>

            <div className="p-3 bg-white border-t border-slate-100 shrink-0 space-y-2">
              <div className="flex gap-1.5 flex-wrap">
                {["Fee collection this month?", "Class 8 defaulters", "Staff on leave today", "Buses with expiring insurance"].map((c) => (
                  <button
                    key={c}
                    onClick={() => handleCopilotSend(c)}
                    className="px-2.5 py-1 bg-violet-50 border border-violet-200 text-violet-700 rounded-full text-[10px] font-bold hover:bg-violet-100 transition"
                  >
                    {c}
                  </button>
                ))}
              </div>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleCopilotSend();
                }}
                className="flex gap-2"
              >
                <input
                  type="text"
                  placeholder="Ask about fees, defaulters, staff, transport..."
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 w-full transition font-semibold"
                />
                <Button type="submit" variant="secondary" className="px-4 py-2">
                  <Send className="h-4 w-4" />
                </Button>
              </form>
            </div>
          </div>

          {/* Early-warning radar */}
          <div className="lg:col-span-6 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 h-[560px] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-slate-50 pb-3 sticky top-0 bg-white">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <Radar className="h-4 w-4 text-rose-600" /> Early-Warning Radar
              </h3>
              <Badge variant="info" className="text-[9px] font-black uppercase">{riskList.length} flagged</Badge>
            </div>

            {riskList.map((r) => {
              const reviewed = riskReviewed.some((x) => x.studentId === r.studentId);
              return (
                <div
                  key={r.studentId}
                  className={`p-4 rounded-2xl border space-y-2.5 ${
                    r.severity === "High" ? "bg-rose-50/50 border-rose-200" : r.severity === "Medium" ? "bg-amber-50/50 border-amber-200" : "bg-slate-50 border-slate-100"
                  }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{r.name}</h4>
                      <p className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">{r.grade}</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className={`text-lg font-black ${r.severity === "High" ? "text-rose-600" : r.severity === "Medium" ? "text-amber-600" : "text-slate-500"}`}>
                        {r.score}
                      </span>
                      <Badge variant={r.severity === "High" ? "danger" : r.severity === "Medium" ? "warning" : "default"} className="text-[9px] font-black uppercase">
                        {r.severity}
                      </Badge>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {r.reasons.map((reason, i) => (
                      <span key={i} className="px-2 py-0.5 bg-white border border-slate-200 rounded-full text-[9px] font-bold text-slate-600">
                        {reason}
                      </span>
                    ))}
                  </div>
                  <p className="text-[10px] text-slate-500 font-semibold leading-relaxed">
                    <span className="font-black text-slate-700">Suggested intervention:</span> {r.intervention}
                  </p>
                  <div className="flex justify-end">
                    {reviewed ? (
                      <Badge variant="success" className="text-[9px] font-black uppercase">Reviewed</Badge>
                    ) : (
                      <Button size="sm" variant="outline" onClick={() => handleMarkReviewed(r)} className="text-[10px] flex items-center gap-1">
                        <CheckCircle className="h-3 w-3" /> Mark Reviewed
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ================= 4. PARENT AI ================= */}
      {activeTab === "parent_ai" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* WhatsApp bot */}
          <div className="lg:col-span-5">
            <div className="bg-white border border-slate-200 rounded-3xl overflow-hidden flex flex-col h-[540px] shadow-xs">
              <div className="bg-emerald-700 p-4 text-white flex items-center justify-between shrink-0">
                <div className="flex items-center gap-2.5">
                  <div className="h-9 w-9 bg-white/20 rounded-full flex items-center justify-center">
                    <MessageCircle className="h-5 w-5" />
                  </div>
                  <div>
                    <h4 className="text-xs font-black">St. Jude's Assistant</h4>
                    <span className="text-[9px] text-emerald-200 font-bold block">online • replies instantly</span>
                  </div>
                </div>
                <button
                  onClick={() => setWaLang(waLang === "en" ? "hi" : "en")}
                  className="flex items-center gap-1.5 px-2.5 py-1 bg-white/15 rounded-full text-[10px] font-black hover:bg-white/25 transition"
                >
                  <Languages className="h-3.5 w-3.5" /> {waLang === "en" ? "EN" : "हिंदी"}
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-3" style={{ background: "#e9e2d5" }}>
                {waChat.map((m, idx) => (
                  <div key={idx} className={`flex ${m.sender === "user" ? "justify-end" : "justify-start"}`}>
                    <div
                      className={`max-w-[85%] p-3 rounded-2xl text-xs font-semibold leading-relaxed shadow-xs ${
                        m.sender === "user" ? "bg-emerald-100 text-slate-800 rounded-tr-none" : "bg-white text-slate-800 rounded-tl-none"
                      }`}
                    >
                      {m.text}
                    </div>
                  </div>
                ))}
                {waTyping && (
                  <div className="flex justify-start">
                    <div className="bg-white rounded-2xl rounded-tl-none shadow-xs"><TypingDots /></div>
                  </div>
                )}
              </div>

              <div className="p-3 bg-slate-50 border-t border-slate-200 shrink-0">
                <div className="flex gap-1.5 flex-wrap">
                  {(waLang === "en"
                    ? [
                        { key: "fee", label: "Fee due?" },
                        { key: "bus", label: "Bus location?" },
                        { key: "hw", label: "Today's homework?" },
                        { key: "ptm", label: "Next PTM?" }
                      ]
                    : [
                        { key: "fee", label: "फीस बकाया?" },
                        { key: "bus", label: "बस कहाँ है?" },
                        { key: "hw", label: "आज का होमवर्क?" },
                        { key: "ptm", label: "अगली PTM?" }
                      ]
                  ).map((c) => (
                    <button
                      key={c.key}
                      onClick={() => handleWaChip(c.key, c.label)}
                      className="px-3 py-1.5 bg-white border border-emerald-300 text-emerald-700 rounded-full text-[10px] font-bold hover:bg-emerald-50 transition"
                    >
                      {c.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Weekly digest */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-3 border-b border-slate-50 pb-3">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                  <CalendarDays className="h-4 w-4 text-emerald-600" /> Weekly Digest Preview
                </h3>
                <div className="w-full sm:w-64">
                  <Select
                    options={students.map((s) => ({ label: `${s.name} — ${s.grade}`, value: s.id }))}
                    value={digestStudentId}
                    onChange={(e) => setDigestStudentId(e.target.value)}
                  />
                </div>
              </div>

              {digestStudent && (
                <div className="space-y-4">
                  <div className="p-4 bg-emerald-700 text-white rounded-2xl">
                    <span className="text-[9px] uppercase tracking-widest font-extrabold text-emerald-200">Weekly Digest • 29 Jun – 04 Jul 2026</span>
                    <h4 className="text-base font-black mt-1">{digestStudent.name}</h4>
                    <p className="text-[10px] text-emerald-100 font-bold">{digestStudent.grade} • To: {digestStudent.parentName} ({digestStudent.parentPhone})</p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl">
                      <span className="text-[9px] uppercase tracking-widest font-extrabold text-slate-400">Attendance This Week</span>
                      <div className="flex gap-2 mt-2.5">
                        {weekStrip.map((d) => (
                          <div key={d.day} className="flex flex-col items-center gap-1">
                            <span
                              className={`h-7 w-7 rounded-full flex items-center justify-center text-[10px] font-black ${
                                d.mark === "P" ? "bg-emerald-100 text-emerald-700" : d.mark === "L" ? "bg-amber-100 text-amber-700" : "bg-rose-100 text-rose-700"
                              }`}
                            >
                              {d.mark}
                            </span>
                            <span className="text-[8px] font-bold text-slate-400 uppercase">{d.day}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl">
                      <span className="text-[9px] uppercase tracking-widest font-extrabold text-slate-400">Marks Highlights</span>
                      <ul className="mt-2 space-y-1 text-[11px] font-semibold text-slate-700">
                        <li>Maths Unit Test: {14 + (digestHash % 6)}/20 {digestHash % 2 === 0 ? "(class avg 13.2 — above average)" : "(steady vs last test)"}</li>
                        <li>Science Practical: Grade {digestHash % 3 === 0 ? "A" : "B+"} — {digestHash % 3 === 0 ? "best diagram in section" : "neat record work"}</li>
                      </ul>
                    </div>
                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between">
                      <div>
                        <span className="text-[9px] uppercase tracking-widest font-extrabold text-slate-400">Homework Pending</span>
                        <p className="text-xl font-black text-slate-800 mt-1">{digestHash % 4}</p>
                      </div>
                      <ClipboardList className="h-6 w-6 text-slate-300" />
                    </div>
                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between">
                      <div>
                        <span className="text-[9px] uppercase tracking-widest font-extrabold text-slate-400">Fee Dues</span>
                        <p className={`text-xl font-black mt-1 ${digestDue > 0 ? "text-rose-600" : "text-emerald-600"}`}>{inr(digestDue)}</p>
                      </div>
                      <Wallet className="h-6 w-6 text-slate-300" />
                    </div>
                  </div>

                  <div className="p-4 bg-blue-50/60 border border-blue-100 rounded-2xl">
                    <span className="text-[9px] uppercase tracking-widest font-extrabold text-blue-600">Upcoming Events</span>
                    <ul className="mt-1.5 space-y-1 text-[11px] font-semibold text-slate-700">
                      <li>Sat 12 Jul — Parent-Teacher Meeting (HPC Term-1 cards shared)</li>
                      <li>Tue 15 Jul — Inter-house Kabaddi tournament</li>
                      <li>Fri 18 Jul — Van Mahotsav tree-planting drive</li>
                    </ul>
                  </div>

                  <div className="flex justify-end">
                    <Button onClick={handleSendDigest} variant="secondary" className="text-xs flex items-center gap-2">
                      <Send className="h-4 w-4" /> Send to Parent (WhatsApp)
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ================= 5. SAFETY AI ================= */}
      {activeTab === "safety_ai" && (
        <div className="space-y-6">
          <div className="bg-rose-50/60 border border-rose-200 p-4 rounded-3xl flex gap-3 text-rose-950 text-xs">
            <ShieldAlert className="h-4.5 w-4.5 text-rose-600 shrink-0 mt-0.5" />
            <p className="font-semibold leading-relaxed">
              <span className="font-black">Safety AI requires the Enterprise tier</span> and recorded DPDP consent for biometric/CCTV processing. All detections
              below are machine-flagged and must be confirmed by a human before any action — no automated discipline decisions.
            </p>
          </div>

          {/* CCTV anomalies */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
              <Camera className="h-4 w-4 text-rose-600" /> CCTV Anomaly Feed (Vision Model)
            </h3>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-4">Event</th>
                    <th className="p-4">Camera</th>
                    <th className="p-4">Time</th>
                    <th className="p-4">Severity</th>
                    <th className="p-4">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {safety.cctv.map((e) => (
                    <tr key={e.id} className={e.acknowledged ? "opacity-60" : ""}>
                      <td className="p-4">
                        <span className="font-extrabold text-slate-800">{e.type}</span>
                        <span className="block text-[10px] text-slate-400 font-semibold mt-0.5 max-w-xs">{e.note}</span>
                      </td>
                      <td className="p-4 font-mono text-[10px]">{e.camera}</td>
                      <td className="p-4 font-mono text-[10px] text-slate-500">{e.time}</td>
                      <td className="p-4">
                        <Badge variant={e.severity === "High" ? "danger" : e.severity === "Medium" ? "warning" : "default"} className="text-[9px] font-black uppercase">
                          {e.severity}
                        </Badge>
                      </td>
                      <td className="p-4">
                        <div className="flex gap-2">
                          <Button size="sm" variant="outline" onClick={() => setClipEvent(e)} className="text-[10px] flex items-center gap-1">
                            <Play className="h-3 w-3" /> Review Clip
                          </Button>
                          {!e.acknowledged && (
                            <Button size="sm" variant="secondary" onClick={() => ackSafety("cctv", e.id)} className="text-[10px]">
                              Acknowledge
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Bus overspeed */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
                <Bus className="h-4 w-4 text-amber-600" /> Bus Overspeed Alerts (GPS Telemetry)
              </h3>
              {safety.bus.map((b) => (
                <div key={b.id} className={`p-4 rounded-2xl border ${b.acknowledged ? "bg-slate-50 border-slate-100 opacity-60" : "bg-amber-50/50 border-amber-200"}`}>
                  <div className="flex justify-between items-start gap-2">
                    <div>
                      <h4 className="text-xs font-black text-slate-800 flex items-center gap-2">
                        <Gauge className="h-3.5 w-3.5 text-amber-600" /> {b.vehicle} — {b.speed} km/h <span className="text-slate-400 font-bold">(limit {b.limit})</span>
                      </h4>
                      <p className="text-[10px] text-slate-500 font-semibold mt-1">{b.route} • {b.location}</p>
                      <p className="text-[9px] text-slate-400 font-mono mt-0.5">{b.time}</p>
                    </div>
                    {b.acknowledged ? (
                      <Badge variant="success" className="text-[9px] font-black uppercase shrink-0">Closed</Badge>
                    ) : (
                      <Button size="sm" variant="outline" onClick={() => ackSafety("bus", b.id)} className="text-[10px] shrink-0">
                        Acknowledge
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Hostel curfew */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
                <BedDouble className="h-4 w-4 text-indigo-600" /> Hostel Curfew Anomalies
              </h3>
              {safety.hostel.map((h) => (
                <div key={h.id} className={`p-4 rounded-2xl border ${h.acknowledged ? "bg-slate-50 border-slate-100 opacity-60" : "bg-indigo-50/50 border-indigo-200"}`}>
                  <div className="flex justify-between items-start gap-2">
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{h.event} — {h.block}</h4>
                      <p className="text-[10px] text-slate-500 font-semibold mt-1 leading-relaxed">{h.detail}</p>
                      <p className="text-[9px] text-slate-400 font-mono mt-0.5">{h.time}</p>
                    </div>
                    <div className="flex flex-col items-end gap-2 shrink-0">
                      <Badge variant={h.severity === "Medium" ? "warning" : "default"} className="text-[9px] font-black uppercase">{h.severity}</Badge>
                      {h.acknowledged ? (
                        <Badge variant="success" className="text-[9px] font-black uppercase">Closed</Badge>
                      ) : (
                        <Button size="sm" variant="outline" onClick={() => ackSafety("hostel", h.id)} className="text-[10px]">
                          Acknowledge
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Fake clip player dialog */}
          <Dialog isOpen={!!clipEvent} onClose={() => setClipEvent(null)} title={`Clip Review — ${clipEvent ? clipEvent.camera : ""}`} maxWidth="max-w-2xl">
            {clipEvent && (
              <div className="space-y-4">
                <div
                  className="relative w-full h-64 rounded-2xl overflow-hidden border border-slate-800"
                  style={{
                    background: "repeating-linear-gradient(0deg, #0f172a 0px, #0f172a 3px, #16213c 3px, #16213c 4px)"
                  }}
                >
                  <div className="absolute inset-0 bg-gradient-to-b from-transparent via-white/5 to-transparent animate-pulse" />
                  <div className="absolute top-3 left-3 flex items-center gap-2 text-[10px] font-mono font-black text-emerald-400">
                    <span className="h-2 w-2 bg-rose-500 rounded-full animate-ping" /> REC • {clipEvent.camera}
                  </div>
                  <div className="absolute top-3 right-3 text-[10px] font-mono font-bold text-slate-400">{clipEvent.time}</div>
                  <div className="absolute inset-0 flex flex-col items-center justify-center gap-2">
                    <Play className="h-10 w-10 text-white/40" />
                    <span className="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest">Simulated playback — {clipEvent.type} detection</span>
                  </div>
                  <div className="absolute bottom-3 left-3 right-3 h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full w-1/3 bg-emerald-400/70 rounded-full" />
                  </div>
                </div>
                <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl text-[11px] font-semibold text-slate-600 leading-relaxed">
                  <span className="font-black text-slate-800">AI annotation:</span> {clipEvent.note} Confidence 0.{70 + (hashNum(clipEvent.id) % 25)}. Bounding
                  boxes and identities are blurred until a human confirms the event (DPDP data-minimisation).
                </div>
                <div className="flex gap-3 justify-end pt-2 border-t border-slate-100">
                  <Button variant="outline" onClick={() => setClipEvent(null)}>Close</Button>
                  {!clipEvent.acknowledged && (
                    <Button
                      onClick={() => {
                        ackSafety("cctv", clipEvent.id);
                        setClipEvent(null);
                      }}
                      className="flex items-center gap-2"
                    >
                      <CheckCircle className="h-4 w-4" /> Confirm & Acknowledge
                    </Button>
                  )}
                </div>
              </div>
            )}
          </Dialog>
        </div>
      )}

      {/* ================= 6. GOVERNANCE ================= */}
      {activeTab === "governance" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-50 pb-3">
                <SlidersHorizontal className="h-4 w-4 text-blue-600" /> Human-in-the-Loop Controls
              </h3>
              {[
                { key: "remarksApproval", title: "Remarks require teacher approval", desc: "AI report-card remarks are drafts until a teacher clicks Approve & Save." },
                { key: "botCurriculumBound", title: "Student/parent bot is curriculum-bounded", desc: "Bot answers only from school data and NCERT syllabus; refuses open web topics." },
                { key: "riskHumanReview", title: "Risk radar needs human review", desc: "Early-warning cases never trigger parent messages until marked reviewed." },
                { key: "cctvHumanConfirm", title: "CCTV events need human confirmation", desc: "Identities stay blurred and no incident is filed until a staff member confirms." },
                { key: "dataMinimisation", title: "DPDP data minimisation", desc: "AI prompts strip Aadhaar, phone numbers and addresses before processing." },
                { key: "parentBotHandoff", title: "Auto-handoff bot to front office", desc: "Unresolved parent queries create a front-office ticket automatically." }
              ].map((t) => (
                <div key={t.key} className="flex items-center justify-between gap-4 p-4 bg-slate-50 border border-slate-100 rounded-2xl">
                  <div>
                    <h4 className="text-xs font-black text-slate-800">{t.title}</h4>
                    <p className="text-[10px] text-slate-400 font-semibold mt-0.5 leading-relaxed">{t.desc}</p>
                  </div>
                  <Toggle on={!!gov[t.key]} onClick={() => toggleGov(t.key)} />
                </div>
              ))}
            </div>

            <div className="space-y-4">
              <div className="bg-indigo-900 text-white rounded-3xl p-6 space-y-3">
                <span className="text-[10px] uppercase tracking-widest font-extrabold text-indigo-300 flex items-center gap-2">
                  <Cpu className="h-4 w-4" /> Model Cost Meter
                </span>
                <h4 className="text-3xl font-black">{inr(monthCost)}</h4>
                <p className="text-[10px] text-indigo-200 font-bold">estimated this month • {totalTokens.toLocaleString("en-IN")} tokens across 4 bundles</p>
                <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                  <div className="bg-violet-400 h-full rounded-full" style={{ width: `${Math.min(100, Math.round((totalTokens / totalQuota) * 100))}%` }} />
                </div>
                <p className="text-[10px] text-indigo-200 font-semibold leading-relaxed">
                  Blended rate ₹0.42 / 1K tokens. Budget alert fires at 85% of pooled quota; generation pauses at 100% (drafts queue for next cycle).
                </p>
              </div>
              <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-2">
                <span className="text-[10px] uppercase tracking-widest font-extrabold text-slate-400 flex items-center gap-2">
                  <Eye className="h-4 w-4 text-slate-400" /> Audit Coverage
                </span>
                <h4 className="text-2xl font-black text-slate-800">{audit.length} events</h4>
                <p className="text-[10px] text-slate-400 font-semibold">Every generation across Teacher, Principal, Parent and Safety bundles is logged below with actor, tokens and purpose.</p>
              </div>
            </div>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-50 pb-3">Generation Audit Log</h3>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-100 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-4">Timestamp</th>
                    <th className="p-4">Bundle</th>
                    <th className="p-4">Feature</th>
                    <th className="p-4">Detail</th>
                    <th className="p-4">Tokens</th>
                    <th className="p-4">Actor</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {audit.slice(0, 20).map((a) => (
                    <tr key={a.id}>
                      <td className="p-4 font-mono text-[10px] text-slate-500 whitespace-nowrap">{a.at}</td>
                      <td className="p-4">
                        <Badge
                          variant={a.bundle === "Safety AI" ? "danger" : a.bundle === "Teacher AI" ? "info" : a.bundle === "Parent AI" ? "success" : "secondary"}
                          className="text-[9px] font-black uppercase"
                        >
                          {a.bundle}
                        </Badge>
                      </td>
                      <td className="p-4 font-extrabold text-slate-800 whitespace-nowrap">{a.feature}</td>
                      <td className="p-4 text-slate-500 leading-relaxed">{a.detail}</td>
                      <td className="p-4 font-mono text-[10px]">{a.tokens.toLocaleString("en-IN")}</td>
                      <td className="p-4 text-slate-500 whitespace-nowrap">{a.actor}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
