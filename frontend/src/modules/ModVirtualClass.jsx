/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getOnlineClasses,
  saveOnlineClasses,
  getClassRecordings,
  saveClassRecordings,
  getAiNotes,
  saveAiNotes,
  getStudents,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Laptop,
  Video,
  Users,
  Award,
  BookOpen,
  Book,
  FileText,
  CheckCircle,
  TrendingUp,
  Plus,
  Search,
  MessageSquare,
  Calendar,
  Clock,
  Settings,
  HelpCircle,
  Send,
  RefreshCw,
  Play,
  Volume2,
  ArrowRight
} from "lucide-react";

export default function ModVirtualClass({ user }) {
  const { addToast } = useToast();

  // Load state databases
  const [onlineClasses, setOnlineClasses] = useState(() => getOnlineClasses());
  const [recordings, setRecordings] = useState(() => getClassRecordings());
  const [aiNotes, setAiNotes] = useState(() => getAiNotes());
  const [students] = useState(() => getStudents());

  // Tabs navigation
  // Options: virtual_classroom, ai_study_assistant, doubt_assistant, homework_builder
  const [activeTab, setActiveTab] = useState("virtual_classroom");

  // Dialog toggles
  const [isScheduleClassOpen, setIsScheduleClassOpen] = useState(false);

  // Form states
  // 1. Online Class Scheduler
  const [schedSubject, setSchedSubject] = useState("Math");
  const [schedSubjectName, setSchedSubjectName] = useState("Grade 10 Mathematics");
  const [schedGrade, setSchedGrade] = useState("Grade 10");
  const [schedSection, setSchedSection] = useState("A");
  const [schedDate, setSchedDate] = useState("");
  const [schedStart, setSchedStart] = useState("");
  const [schedEnd, setSchedEnd] = useState("");

  // 2. Chatbot Assistant States
  const [chatInput, setChatInput] = useState("");
  const [chatLog, setChatLog] = useState([
    { sender: "ai", text: "Hello! I am your St. Jude's AI Learning Assistant. You can ask me to summarize today's classes, show revision notes, or fetch Roman Republic flashcards." }
  ]);

  // 3. AI Homework Generator
  const [hwSubject, setHwSubject] = useState("History");
  const [hwDifficulty, setHwDifficulty] = useState("Medium");
  const [hwProgress, setHwProgress] = useState(-1); // -1 inactive
  const [generatedHwList, setGeneratedHwList] = useState([]);

  // Search/Filters
  const [classSearch, setClassSearch] = useState("");

  // Selected details
  const [selectedClassForNotes, setSelectedClassForNotes] = useState("class-3");
  const [activeRecordingSession, setActiveRecordingSession] = useState(null);

  // Flashcards state
  const [showAnswer, setShowAnswer] = useState(false);

  // Role permissions validation
  const isSuperAdmin = user?.role === "Super Admin";
  const isTeacher = user?.role === "Teacher" || user?.role === "Super Admin";
  const isParent = user?.role === "Parent";
  const isStudent = user?.role === "Student" || user?.role === "Parent"; // Parents simulate student features



  // Metrics
  const totalClassesConducted = onlineClasses.filter(c => c.status === "Completed").length;
  const averageAttendance = "96.4%";
  const totalRecordingHours = "18.5 Hrs";
  const totalNotesGenerated = aiNotes.length;

  // HANDLERS
  // 1. Schedule Online Class
  const handleScheduleClass = (e) => {
    e.preventDefault();
    if (!schedDate || !schedStart || !schedEnd) {
      addToast("Failed to Schedule", "Date and Time coordinates are required.", "error");
      return;
    }

    const uniqueMeetId = `meet.google.com/${Math.random().toString(36).substring(2, 5)}-${Math.random().toString(36).substring(2, 6)}-${Math.random().toString(36).substring(2, 5)}`;

    const newClass = {
      id: `class-${Date.now()}`,
      subjectId: schedSubject,
      subjectName: schedSubjectName,
      teacherId: user?.id || "staff-teacher-1",
      teacherName: user?.name || "Teacher John",
      classId: `${schedGrade.split(" ")[1]}-${schedSection}`,
      sectionId: schedSection,
      meetingLink: uniqueMeetId,
      date: schedDate,
      startTime: schedStart,
      endTime: schedEnd,
      status: "Upcoming"
    };

    const nextClasses = [...onlineClasses, newClass];
    setOnlineClasses(nextClasses);
    saveOnlineClasses(nextClasses);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Online Class Scheduled",
      `Scheduled class "${schedSubjectName}" with Google Meet link: ${uniqueMeetId}`
    );

    addToast("Class Scheduled", `Created Google Meet Link. Calendars and Parent portals updated.`);
    setIsScheduleClassOpen(false);

    setSchedDate("");
    setSchedStart("");
    setSchedEnd("");
  };

  // 2. Join Online Class
  const handleJoinClass = (meetingLink) => {
    addToast("Joining Classroom", "Initializing secure connection. Audio/Video check...");
    window.open(meetingLink, "_blank");

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Joined Online Class",
      `Launched virtual classroom meeting interface: ${meetingLink}`
    );
  };

  // 3. Start Session Recording
  const handleToggleRecording = (classObj) => {
    if (activeRecordingSession === classObj.id) {
      // Stop Recording & Auto Generate AI notes
      setActiveRecordingSession(null);
      addToast("Recording Stopped", "Processing audio track via AI Transcription engine...");

      // Generate mock Transcript/Summary
      const newRecId = `rec-cls-${classObj.id}`;
      const newRec = {
        id: newRecId,
        classId: classObj.id,
        recordingUrl: `https://storage.googleapis.com/stjude-classes/${classObj.subjectId.toLowerCase()}_session.mp4`,
        transcript: `Speaker 1 (${classObj.teacherName}): Good day, class. Today we discussed advanced equations. Specifically, quadratic equations with complex roots. Speaker 2 (Liam Smith): How do we calculate the discriminant? Speaker 1: Excellent. Use the formula: b squared minus four a c.`,
        summary: `Advanced complex equations. Key points: Quadratic discriminant calculus, complex root parameters, and coefficient variables.`
      };

      const nextRecs = [...recordings, newRec];
      setRecordings(nextRecs);
      saveClassRecordings(nextRecs);

      // Generate mock Notes
      const newNote = {
        id: `note-cls-${classObj.id}`,
        classId: classObj.id,
        notesContent: `St. Jude's AI Study Notes - ${classObj.subjectName}. Key Concepts: Complex root systems. Important Formulas: Discriminant D = b² - 4ac. High Score Practice Questions: Calculate complex roots for x² + 4x + 5 = 0. Flashcards: 1. Q: What does D < 0 indicate? A: Complex roots. Mind Map: Quadratic -> Discriminant -> Real or Complex.`,
        generatedAt: new Date().toISOString().replace("T", " ").substring(0, 19)
      };

      const nextNotes = [...aiNotes, newNote];
      setAiNotes(nextNotes);
      saveAiNotes(nextNotes);

      // Change class status to Completed
      const updatedClasses = onlineClasses.map(c => {
        if (c.id === classObj.id) {
          return { ...c, status: "Completed" };
        }
        return c;
      });
      setOnlineClasses(updatedClasses);
      saveOnlineClasses(updatedClasses);

      logAction(
        user?.id || "sandbox",
        user?.name || "User",
        user?.role || "Staff",
        "AI Transcript Generated",
        `Created summary and notes index for session: ${classObj.subjectName}`
      );

      addToast("AI Notes Published", "Full transcript and revision notes available in library.");
    } else {
      // Start Recording
      setActiveRecordingSession(classObj.id);
      addToast("Recording Active", "Session recording captured to cloud storage bucket.");
    }
  };

  // 4. Doubt Chatbot Submission
  const handleChatSubmit = (e) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    const query = chatInput.trim().toLowerCase();
    const userLog = { sender: "user", text: chatInput };
    const nextLog = [...chatLog, userLog];
    setChatLog(nextLog);
    setChatInput("");

    setTimeout(() => {
      let aiResponse = "I have scanned the curriculum database but couldn't find a direct matches. Can you expand your question?";

      if (query.includes("taught today") || query.includes("history notes")) {
        aiResponse = "Today's Grade 5 History session covered the origins of the Roman Republic (509 BC). Senate configurations and early Roman elections were validated.";
      } else if (query.includes("chapter 5") || query.includes("formulas") || query.includes("chapter five")) {
        aiResponse = "Chapter 5 Equations: Discriminant D = b² - 4ac. Complex roots criteria is D < 0. Flashcard index is compiled.";
      } else if (query.includes("flashcards") || query.includes("roman")) {
        aiResponse = "Flashcard 1: When was Roman Republic founded? Answer: 509 BC. Flashcard 2: Who held absolute authority in emergencies? Answer: Dictator.";
      }

      setChatLog(prev => [...prev, { sender: "ai", text: aiResponse }]);
    }, 1000);
  };

  // 5. AI Homework Generator
  const handleGenerateHomework = (e) => {
    e.preventDefault();
    setHwProgress(0);

    const interval = setInterval(() => {
      setHwProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setHwProgress(-1);

          const mockQuestions = [
            { q: "Evaluate the historical significance of the Roman Senate configuration in 509 BC.", score: "5 Marks" },
            { q: "Compare and contrast democratic elections with Republic consensus councils.", score: "10 Marks" },
            { q: "MCQ: Who was the last king of Rome before the Republic? A) Tarquin B) Romulus C) Numa", score: "2 Marks" }
          ];
          setGeneratedHwList(mockQuestions);
          addToast("AI Homework Formulated", `Successfully compiled 3 homework questions (${hwDifficulty}).`);

          logAction(
            user?.id || "sandbox",
            user?.name || "User",
            user?.role || "Staff",
            "AI Homework Created",
            `Generated ${hwDifficulty} worksheets for history syllabus.`
          );
          return -1;
        }
        return prev + 25;
      });
    }, 400);
  };



  // Filter lists
  const filteredClasses = onlineClasses.filter(c => {
    const matchesSearch = c.subjectName.toLowerCase().includes(classSearch.toLowerCase()) ||
                          c.teacherName.toLowerCase().includes(classSearch.toLowerCase());
    return matchesSearch;
  });

  const activeNotesRecord = aiNotes.find(n => n.classId === selectedClassForNotes);
  const activeTranscriptRecord = recordings.find(r => r.classId === selectedClassForNotes);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Laptop className="h-6 w-6 text-indigo-650 animate-pulse" />
            MOD-20: Virtual Classroom & AI Learning Hub
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Emergency online class failovers, automatic AWS/S3 session recording vaults, and speech-to-text notes generation.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-slate-50 border border-slate-150 px-3 py-1.5 rounded-xl">
          <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
            ROLE: {user?.role || "Visitor"}
          </Badge>
        </div>
      </div>

      {/* Tabs Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("virtual_classroom")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "virtual_classroom" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Virtual Classrooms
        </button>
        <button
          onClick={() => setActiveTab("ai_study_assistant")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "ai_study_assistant" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          AI Transcripts & Study Sheets
        </button>
        {isStudent && (
          <button
            onClick={() => setActiveTab("doubt_assistant")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "doubt_assistant" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            AI Doubt Assistant Tutor
          </button>
        )}
        {isTeacher && (
          <button
            onClick={() => setActiveTab("homework_builder")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "homework_builder" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            AI Homework & Question Banks
          </button>
        )}
      </div>

      {/* ========================================================
          1. VIRTUAL CLASSROOMS & SCHEDULER TAB
          ======================================================== */}
      {activeTab === "virtual_classroom" && (
        <div className="space-y-6">
          {/* Metrics Dashboard */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Classes Conducted</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalClassesConducted} Session logs</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Class Attendance Rate</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{averageAttendance}</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Cloud Recording Hours</span>
              <p className="text-xl font-bold text-indigo-600 mt-1">{totalRecordingHours}</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">AI Notes Compiled</span>
              <p className="text-xl font-bold text-slate-850 mt-1">{totalNotesGenerated} Files</p>
            </div>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <div className="flex items-center gap-2">
                <Calendar className="h-4.5 w-4.5 text-blue-600" />
                <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">St. Jude's Virtual Calendar</h3>
              </div>
              <div className="flex gap-2">
                <div className="relative w-48">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search subject or teacher..."
                    value={classSearch}
                    onChange={(e) => setClassSearch(e.target.value)}
                    className="bg-slate-50 border border-slate-205 rounded-xl pl-8 pr-4 py-1 text-xs focus:outline-none w-full transition"
                  />
                </div>
                {isTeacher && (
                  <Button onClick={() => setIsScheduleClassOpen(true)} className="text-xs py-1 px-3.5">
                    <Plus className="h-4 w-4 mr-1 shrink-0" /> Schedule Online Session
                  </Button>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {filteredClasses.map(cls => {
                const isLive = cls.status === "Live";
                const isRecording = activeRecordingSession === cls.id;
                return (
                  <div key={cls.id} className="p-5 bg-slate-50 border border-slate-150 rounded-3xl flex flex-col justify-between h-[230px] hover:shadow-xs transition relative">
                    {isLive && (
                      <div className="absolute right-4 top-4 flex items-center gap-1.5 text-[8px] font-black uppercase font-mono text-rose-500 tracking-wider bg-rose-50 border border-rose-200 px-2 py-0.5 rounded">
                        <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-ping" />
                        LIVE
                      </div>
                    )}

                    <div className="space-y-2.5">
                      <div className="space-y-1">
                        <h4 className="text-sm font-black text-slate-850">{cls.subjectName}</h4>
                        <p className="text-[10px] text-blue-650 font-bold uppercase tracking-wider">
                          {cls.classId} • Section {cls.sectionId}
                        </p>
                      </div>

                      <div className="space-y-1 font-mono text-[10px] text-slate-450 font-bold">
                        <p>Date Scheduled: {cls.date}</p>
                        <p>Hours: {cls.startTime} - {cls.endTime}</p>
                        <p>Teacher: {cls.teacherName}</p>
                      </div>
                    </div>

                    <div className="pt-3 border-t border-slate-200/60 flex justify-between items-center gap-3">
                      {isTeacher && cls.status !== "Completed" && (
                        <Button
                          onClick={() => handleToggleRecording(cls)}
                          className={`text-[10px] py-1.5 h-8 px-3 rounded-lg border transition ${
                            isRecording
                              ? "bg-rose-50 border-rose-200 text-rose-600 hover:bg-rose-100"
                              : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
                          }`}
                        >
                          {isRecording ? "Stop Recording (Save AI)" : "Start Recording"}
                        </Button>
                      )}

                      {cls.status === "Completed" ? (
                        <Badge variant="success" className="text-[9px] font-black uppercase">Session Completed</Badge>
                      ) : (
                        <Button
                          onClick={() => handleJoinClass(cls.meetingLink)}
                          className="text-[10px] py-1.5 h-8 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
                        >
                          Join Google Meet
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. AI TRANSCRIPTS & STUDY SHEETS TAB
          ======================================================== */}
      {activeTab === "ai_study_assistant" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Class Select Side */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50 flex items-center gap-1.5">
              <BookOpen className="h-4 w-4 text-blue-650" /> Completed Lessons
            </h3>

            <div className="space-y-3">
              {onlineClasses.filter(c => c.status === "Completed").map(cls => {
                const active = selectedClassForNotes === cls.id;
                return (
                  <button
                    key={cls.id}
                    onClick={() => setSelectedClassForNotes(cls.id)}
                    className={`w-full text-left p-3.5 rounded-2xl border transition duration-200 ${
                      active
                        ? "bg-blue-50/10 border-blue-300 text-blue-700 shadow-2xs font-extrabold"
                        : "bg-white border-slate-150 text-slate-650 hover:bg-slate-50"
                    }`}
                  >
                    <h4 className="text-xs font-black">{cls.subjectName}</h4>
                    <p className="text-[9px] text-slate-400 mt-1">{cls.classId} • Date: {cls.date}</p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* AI summaries content pane */}
          <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 space-y-5 shadow-2xs">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <h3 className="text-xs font-black text-slate-850 uppercase tracking-widest flex items-center gap-1.5">
                <FileText className="h-4.5 w-4.5 text-blue-650" /> AI Classroom study Sheet
              </h3>
              <Badge variant="success" className="text-[8px] font-black uppercase tracking-wider">Processed by speech-to-text</Badge>
            </div>

            {activeNotesRecord ? (
              <div className="space-y-5">
                {/* Notes and Revision block */}
                <div className="p-4 bg-slate-50 border border-slate-150 rounded-2xl space-y-3">
                  <h4 className="text-[10px] uppercase font-bold text-slate-450 tracking-wider">AI Study Notes Summary</h4>
                  <p className="text-xs text-slate-650 leading-relaxed font-semibold italic">
                    "{activeNotesRecord.notesContent}"
                  </p>
                </div>

                {/* Speaker timeline transcript */}
                {activeTranscriptRecord && (
                  <div className="space-y-3">
                    <h4 className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">AI Generated Voice Transcript</h4>
                    <div className="p-4 bg-slate-950 text-emerald-450 border border-slate-900 rounded-2xl space-y-2.5 font-mono text-[11px] leading-relaxed shadow-inner">
                      <p className="text-emerald-500 font-bold">{activeTranscriptRecord.transcript}</p>
                    </div>
                  </div>
                )}

                {/* Flip Flashcard Interaction */}
                <div className="p-5 bg-indigo-50/10 border border-indigo-200 rounded-3xl flex flex-col items-center justify-center text-center space-y-3.5 min-h-[140px]">
                  <span className="text-[9px] uppercase font-bold text-indigo-700 tracking-wider">Lesson Interactive Flashcard</span>
                  <div className="space-y-1">
                    <h4 className="text-xs font-black text-slate-850">
                      {!showAnswer ? "Question: When was the Roman Republic officially founded?" : "Answer: 509 BC, after the overthrow of the monarchy."}
                    </h4>
                  </div>
                  <Button
                    onClick={() => setShowAnswer(!showAnswer)}
                    className="text-[10px] py-1 h-7 px-4 bg-indigo-650 hover:bg-indigo-750 text-white rounded-lg font-bold"
                  >
                    {!showAnswer ? "Reveal Answer" : "See Question"}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="p-12 text-center text-slate-400 italic">
                No AI transcripts or notes records found for this class slot.
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          3. AI DOUBT ASSISTANT TAB
          ======================================================== */}
      {activeTab === "doubt_assistant" && isStudent && (
        <div className="flex flex-col items-center justify-center bg-slate-50 border border-slate-200 p-8 rounded-3xl min-h-[450px]">
          <div className="max-w-xl w-full bg-white rounded-3xl border border-slate-300 shadow-xl overflow-hidden flex flex-col h-[460px]">
            {/* Chatbot Header */}
            <div className="bg-slate-900 p-4 text-white flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Laptop className="h-5 w-5 text-indigo-400" />
                <div>
                  <h4 className="text-xs font-black uppercase tracking-wider">AI Doubt Assistant Tutor</h4>
                  <span className="text-[8px] text-slate-400 font-bold block mt-0.5">St. Jude's Virtual Assistant Core</span>
                </div>
              </div>
              <Badge variant="success" className="text-[8px] font-black uppercase">Active</Badge>
            </div>

            {/* Chat Log View */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50">
              {chatLog.map((chat, idx) => (
                <div key={idx} className={`flex ${chat.sender === "user" ? "justify-end" : "justify-start"}`}>
                  <div
                    className={`max-w-[75%] p-3.5 rounded-2xl text-xs font-bold leading-relaxed ${
                      chat.sender === "user"
                        ? "bg-blue-600 text-white rounded-tr-none"
                        : "bg-white border border-slate-200 text-slate-800 rounded-tl-none shadow-3xs"
                    }`}
                  >
                    {chat.text}
                  </div>
                </div>
              ))}
            </div>

            {/* Chat Input form */}
            <form onSubmit={handleChatSubmit} className="p-3 bg-white border-t border-slate-200 flex gap-2">
              <input
                type="text"
                placeholder="Ask e.g. 'Show Roman history flashcards' or 'Chapter 5 formulas'..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                className="bg-slate-50 border border-slate-250 text-slate-800 rounded-xl px-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 w-full transition font-semibold"
              />
              <Button type="submit" variant="secondary" className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl">
                <Send className="h-4 w-4" />
              </Button>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          4. AI HOMEWORK & QUESTION BANKS TAB
          ======================================================== */}
      {activeTab === "homework_builder" && isTeacher && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Homework builder parameter settings */}
          <div className="lg:col-span-5 bg-white border border-slate-100 rounded-3xl p-6 space-y-5 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Question Generation Core
            </h3>

            {hwProgress >= 0 ? (
              <div className="p-8 text-center space-y-4">
                <RefreshCw className="h-10 w-10 text-blue-600 animate-spin mx-auto" />
                <h4 className="text-xs font-black uppercase text-slate-800 tracking-wider">AI Question Compiler generating...</h4>
                <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden max-w-xs mx-auto">
                  <div
                    className="bg-blue-600 h-full rounded-full transition-all duration-300"
                    style={{ width: `${hwProgress}%` }}
                  />
                </div>
                <span className="text-[10px] text-slate-400 font-bold font-mono">Progress: {hwProgress}%</span>
              </div>
            ) : (
              <form onSubmit={handleGenerateHomework} className="space-y-4">
                <Select
                  label="Target Course Syllabus"
                  options={[
                    { label: "Grade 5 History - Roman Republic", value: "History" },
                    { label: "Grade 10 Mathematics - Discriminants", value: "Math" }
                  ]}
                  value={hwSubject}
                  onChange={(e) => setHwSubject(e.target.value)}
                />

                <Select
                  label="Difficulty Criteria Level"
                  options={[
                    { label: "Easy Level (Fundamentals)", value: "Easy" },
                    { label: "Medium Level (Standard Exam)", value: "Medium" },
                    { label: "Hard Level (Advanced Special)", value: "Hard" }
                  ]}
                  value={hwDifficulty}
                  onChange={(e) => setHwDifficulty(e.target.value)}
                />

                <div className="p-4 bg-blue-50/10 border border-blue-200 rounded-2xl text-[11px] text-blue-700 leading-relaxed font-bold">
                  AI will analyze course transcripts in storage and generate dynamic chapter tests, mock exams, and practice exercises.
                </div>

                <div className="pt-2">
                  <Button type="submit" variant="secondary" className="w-full text-xs py-2.5">
                    Execute AI Worksheet Compile
                  </Button>
                </div>
              </form>
            )}
          </div>

          {/* Generated Homework View */}
          <div className="lg:col-span-7 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-850 uppercase tracking-widest border-b pb-3 border-slate-50 flex justify-between items-center">
              <span>Dynamic AI Worksheets</span>
              {generatedHwList.length > 0 && (
                <Button
                  onClick={() => {
                    addToast("Homework Assigned", "Worksheets sent to Parent/Student dashboard.");
                    setGeneratedHwList([]);
                  }}
                  className="text-[9px] py-1 h-7 px-3.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg"
                >
                  Publish to Class
                </Button>
              )}
            </h3>

            <div className="space-y-3">
              {generatedHwList.length === 0 ? (
                <div className="p-12 text-center text-slate-400 italic">
                  Select parameters and trigger the compiler to generate worksheets.
                </div>
              ) : (
                generatedHwList.map((hw, idx) => (
                  <div key={idx} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex justify-between items-center gap-3">
                    <div className="space-y-1">
                      <span className="text-[9px] text-slate-400 font-mono block">QUESTION {idx + 1}</span>
                      <h4 className="text-xs font-black text-slate-800 leading-relaxed">{hw.q}</h4>
                    </div>
                    <Badge variant="info" className="text-[9px] font-black uppercase shrink-0">
                      {hw.score}
                    </Badge>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
         {/* ========================================================
          MODAL FORMS & DIALOGS
          ======================================================== */}
      {/* 1. Online Class Scheduler Modal */}
      <Dialog
        isOpen={isScheduleClassOpen}
        onClose={() => setIsScheduleClassOpen(false)}
        title="Schedule Google Meet Virtual Class"
      >
        <form onSubmit={handleScheduleClass} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Select Subject"
              options={[
                { label: "Grade 10 Mathematics", value: "Math" },
                { label: "Grade 10 Science", value: "Science" },
                { label: "Grade 5 History", value: "History" }
              ]}
              value={schedSubject}
              onChange={(e) => {
                setSchedSubject(e.target.value);
                const match = e.target.value === "Math" ? "Grade 10 Mathematics" : e.target.value === "Science" ? "Grade 10 Science" : "Grade 5 History";
                setSchedSubjectName(match);
              }}
            />
            <Select
              label="Student Grade"
              options={[
                { label: "Grade 10", value: "Grade 10" },
                { label: "Grade 5", value: "Grade 5" }
              ]}
              value={schedGrade}
              onChange={(e) => setSchedGrade(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Class Section"
              options={[
                { label: "Section A", value: "A" },
                { label: "Section B", value: "B" }
              ]}
              value={schedSection}
              onChange={(e) => setSchedSection(e.target.value)}
            />
            <Input
              label="Class Date *"
              type="date"
              value={schedDate}
              onChange={(e) => setSchedDate(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Start Time *"
              type="time"
              value={schedStart}
              onChange={(e) => setSchedStart(e.target.value)}
              required
            />
            <Input
              label="End Time *"
              type="time"
              value={schedEnd}
              onChange={(e) => setSchedEnd(e.target.value)}
              required
            />
          </div>

          <div className="p-4 bg-indigo-50/10 border border-indigo-200 rounded-2xl text-[11px] text-indigo-700 leading-relaxed font-bold">
            Virtual classroom scheduler automatically configures a secure calendar entry with integrated Google Meet parameters.
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsScheduleClassOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Schedule Google Meet Link
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}

