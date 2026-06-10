/**
 * @license
 * SPDX-License-Identifier: Apache-2.5
 */
import { useState, useEffect } from "react";
import {
  getReviewCycles,
  saveReviewCycles,
  getTeacherReviews,
  saveTeacherReviews,
  getStudentReviews,
  saveStudentReviews,
  getTeacherPerformanceReviews,
  saveTeacherPerformanceReviews,
  getStudents,
  getStaff,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Award,
  Star,
  User,
  Clock,
  Calendar,
  MessageSquare,
  TrendingUp,
  AlertOctagon,
  Users,
  Settings,
  Plus,
  Trash2,
  CheckCircle,
  BarChart2,
  Shield,
  FileText,
  Bookmark
} from "lucide-react";

export default function ModFeedback({ user }) {
  const { addToast } = useToast();

  // Load local state databases
  const [cycles, setCycles] = useState(() => getReviewCycles());
  const [teacherReviews, setTeacherReviews] = useState(() => getTeacherReviews());
  const [studentReviews, setStudentReviews] = useState(() => getStudentReviews());
  const [mgmtReviews, setMgmtReviews] = useState(() => getTeacherPerformanceReviews());
  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());

  // Active navigation tab
  // Options: dashboard, studentEval, teacherEval, management, cycles, profiles
  const [activeTab, setActiveTab] = useState("dashboard");

  // Modals state
  const [isCycleOpen, setIsCycleOpen] = useState(false);
  const [isStudentReviewOpen, setIsStudentReviewOpen] = useState(false);
  const [isTeacherReviewOpen, setIsTeacherReviewOpen] = useState(false);
  const [isMgmtReviewOpen, setIsMgmtReviewOpen] = useState(false);

  // Profile detail modals
  const [selectedStudentProfileId, setSelectedStudentProfileId] = useState("");
  const [selectedTeacherProfileId, setSelectedTeacherProfileId] = useState("");

  // Target selectors
  const [activeCycleId, setActiveCycleId] = useState(cycles.find(c => c.status === "Active")?.id || cycles[0]?.id || "");
  const [targetStudentId, setTargetStudentId] = useState("");
  const [targetTeacherId, setTargetTeacherId] = useState("");

  // Review Forms state
  // 1. Cycle Form
  const [cycleName, setCycleName] = useState("");
  const [cycleStart, setCycleStart] = useState("");
  const [cycleEnd, setCycleEnd] = useState("");

  // 2. Student Evaluation Form (Teacher -> Student)
  const [scoreAcad, setScoreAcad] = useState(5);
  const [scorePart, setScorePart] = useState(5);
  const [scoreDiscipline, setScoreDiscipline] = useState(5);
  const [scoreBehavior, setScoreBehavior] = useState(5);
  const [stStrengths, setStStrengths] = useState("");
  const [stImprovements, setStImprovements] = useState("");
  const [stComments, setStComments] = useState("");

  // 3. Teacher Evaluation Form (Student/Parent -> Teacher)
  const [reviewType, setReviewType] = useState("Student"); // Student or Parent
  const [teacherRating, setTeacherRating] = useState(5);
  const [teacherComments, setTeacherComments] = useState("");
  const [isAnonymous, setIsAnonymous] = useState(false);

  // 4. Principal Appraisal Form (Principal -> Teacher)
  const [mgmtComments, setMgmtComments] = useState("");
  const [mgmtRating, setMgmtRating] = useState(5);
  const [appraisalStatus, setAppraisalStatus] = useState("Approved"); // Approved, Promotion Recommended, PIP Recommended

  // Dynamic calculations for scores
  const getTeacherRatings = (teacherId) => {
    const reviews = teacherReviews.filter(r => r.teacherId === teacherId);
    const studReviews = reviews.filter(r => r.studentId !== null);
    const parentReviews = reviews.filter(r => r.parentId !== null);
    const principalReviews = mgmtReviews.filter(r => r.teacherId === teacherId);

    const calcAvg = (list) => list.length === 0 ? 4.5 : Number((list.reduce((sum, r) => sum + r.rating, 0) / list.length).toFixed(1));

    const sAvg = calcAvg(studReviews);
    const pAvg = calcAvg(parentReviews);
    const mAvg = calcAvg(principalReviews);

    // Mock Attendance Rating mapping (100% attendance = 5 stars)
    // Most teachers have active status (rating around 95% -> 4.75 stars)
    const attRating = 4.8;

    // Weight Calculation: 40% Student, 30% Parent, 20% Principal, 10% Attendance
    const overallScore = Number((sAvg * 0.4 + pAvg * 0.3 + mAvg * 0.2 + attRating * 0.1).toFixed(1));

    return {
      studentAvg: sAvg,
      parentAvg: pAvg,
      mgmtAvg: mAvg,
      attendanceScore: attRating,
      overall: overallScore,
      totalCount: reviews.length + principalReviews.length
    };
  };

  // Setup initial active targets
  useEffect(() => {
    if (students.length > 0 && !targetStudentId) {
      setTargetStudentId(students[0].id);
      setSelectedStudentProfileId(students[0].id);
    }
    const teachers = staff.filter(s => s.role === "Teacher");
    if (teachers.length > 0 && !targetTeacherId) {
      setTargetTeacherId(teachers[0].id);
      setSelectedTeacherProfileId(teachers[0].id);
    }
  }, [students, staff]);

  // Form Handlers
  const handleCreateCycle = (e) => {
    e.preventDefault();
    if (!cycleName) return;

    const newCycle = {
      id: `cycle-${Date.now()}`,
      name: cycleName,
      startDate: cycleStart || new Date().toISOString().split("T")[0],
      endDate: cycleEnd || new Date(Date.now() + 30*24*60*60*1000).toISOString().split("T")[0],
      status: "Active"
    };

    const next = [newCycle, ...cycles];
    setCycles(next);
    saveReviewCycles(next);
    logAction(user.id, user.name, user.role, "Review Cycle Created", `Opened feedback window: "${cycleName}"`);
    addToast("Cycle Opened", `Feedback cycle "${cycleName}" is now active.`);

    setCycleName("");
    setIsCycleOpen(false);
  };

  const handleToggleCycleStatus = (id, currentStatus) => {
    const next = cycles.map(c => {
      if (c.id === id) {
        return { ...c, status: currentStatus === "Active" ? "Closed" : "Active" };
      }
      return c;
    });
    setCycles(next);
    saveReviewCycles(next);
    addToast("Cycle Updated", `Feedback window status updated.`);
  };

  const handleSubmitStudentReview = (e) => {
    e.preventDefault();
    if (!targetStudentId) return;

    const targetStudent = students.find(s => s.id === targetStudentId);
    
    const newRev = {
      id: `st-rev-${Date.now()}`,
      studentId: targetStudentId,
      teacherId: user.id || "staff-teacher-1",
      reviewCycleId: activeCycleId,
      academicScore: parseFloat(scoreAcad),
      disciplineScore: parseFloat(scoreDiscipline),
      participationScore: parseFloat(scorePart),
      behaviorScore: parseFloat(scoreBehavior),
      comments: `${stComments}. Strengths: ${stStrengths}. Improvements: ${stImprovements}.`,
      created_at: new Date().toISOString().split("T")[0]
    };

    const next = [newRev, ...studentReviews];
    setStudentReviews(next);
    saveStudentReviews(next);
    logAction(user.id, user.name, user.role, "Student Evaluation Submitted", `Evaluated academic conduct for scholar: ${targetStudent?.name}`);
    
    // Simulate notification dispatches
    addToast("Review Published", `${targetStudent?.name} evaluated. Report card sent to Parent portal.`);
    setIsStudentReviewOpen(false);
  };

  const handleSubmitTeacherReview = (e) => {
    e.preventDefault();
    if (!targetTeacherId) return;

    const teacherObj = staff.find(s => s.id === targetTeacherId);

    const newRev = {
      id: `tr-rev-${Date.now()}`,
      teacherId: targetTeacherId,
      studentId: reviewType === "Student" ? "student-1" : null,
      parentId: reviewType === "Parent" ? "parent-1" : null,
      reviewCycleId: activeCycleId,
      rating: parseFloat(teacherRating),
      reviewText: teacherComments,
      anonymous: isAnonymous,
      created_at: new Date().toISOString().split("T")[0]
    };

    const next = [newRev, ...teacherReviews];
    setTeacherReviews(next);
    saveTeacherReviews(next);
    logAction(user.id, user.name, user.role, "Teacher Review Submitted", `Submitted evaluation score for: ${teacherObj?.name}`);

    // If score is low (< 2 stars), log alert triggers
    if (teacherRating <= 2) {
      addToast("Low Score Alert Triggered", `Principal cabinet alerted regarding low rating for ${teacherObj?.name}.`, "warning");
    } else {
      addToast("Review Registered", `Feedback recorded for ${teacherObj?.name}.`);
    }

    setTeacherComments("");
    setIsTeacherReviewOpen(false);
  };

  const handleSubmitMgmtReview = (e) => {
    e.preventDefault();
    if (!targetTeacherId) return;

    const teacherObj = staff.find(s => s.id === targetTeacherId);

    const newRev = {
      id: `perf-rev-${Date.now()}`,
      teacherId: targetTeacherId,
      reviewerId: user.id || "staff-principal",
      reviewerRole: "Principal",
      rating: parseFloat(mgmtRating),
      comments: `${mgmtComments}. Appraisal Status: ${appraisalStatus}.`,
      reviewCycleId: activeCycleId
    };

    const next = [newRev, ...mgmtReviews];
    setMgmtReviews(next);
    saveTeacherPerformanceReviews(next);
    logAction(user.id, user.name, user.role, "Principal Appraisal Logged", `Completed appraisal review for Prof. ${teacherObj?.name}. Status: ${appraisalStatus}`);

    addToast("Appraisal Registered", `Performance appraisal logged for ${teacherObj?.name}.`);
    setMgmtComments("");
    setIsMgmtReviewOpen(false);
  };

  // Role Permissions validation
  const isPrincipalOrAdmin = user.role === "Principal" || user.role === "Super Admin";
  const isTeacher = user.role === "Teacher" || user.role === "Warden"; // Wardens can simulate teacher reviews
  const isParent = user.role === "Parent";

  // Dashboard calculations
  const totalReviewsCount = teacherReviews.length + studentReviews.length + mgmtReviews.length;
  const lowRatingsList = teacherReviews.filter(r => r.rating <= 2.0);

  const activeStudentProfile = students.find(s => s.id === selectedStudentProfileId);
  const activeTeacherProfile = staff.find(s => s.id === selectedTeacherProfileId);

  return (
    <div className="space-y-6">
      {/* Title Header banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Award className="h-6 w-6 text-blue-600" />
            MOD-16: Feedback & Performance Review Management
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Analyze 360-degree performance scorecards. Complete student evaluations, parents' teacher feedback, and principal appraisals.
          </p>
        </div>

        {/* Global Cycle Select */}
        <div className="flex gap-3 text-xs items-center">
          <span className="font-bold text-slate-500 font-mono">Cycle:</span>
          <div className="w-56">
            <Select
              options={cycles.map(c => ({ label: `${c.name} (${c.status})`, value: c.id }))}
              value={activeCycleId}
              onChange={(e) => setActiveCycleId(e.target.value)}
              className="text-xs py-1 h-8 bg-slate-50 rounded-xl"
            />
          </div>
        </div>
      </div>

      {/* Tab Navigation Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "dashboard" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-805"
          }`}
        >
          Scorecard Analytics
        </button>

        {isTeacher && (
          <button
            onClick={() => setActiveTab("studentEval")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "studentEval" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Review My Students
          </button>
        )}

        <button
          onClick={() => setActiveTab("teacherEval")}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "teacherEval" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
          }`}
        >
          Evaluate Teachers
        </button>

        {isPrincipalOrAdmin && (
          <button
            onClick={() => setActiveTab("management")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "management" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Appraisal Desk
          </button>
        )}

        {isPrincipalOrAdmin && (
          <button
            onClick={() => setActiveTab("cycles")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "cycles" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Review Cycles
          </button>
        )}

        <button
          onClick={() => setActiveTab("profiles")}
          className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "profiles" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
        >
          Performance Profiles
        </button>
      </div>

      {/* ========================================================
          1. DASHBOARD OVERVIEW TAB
          ======================================================== */}
      {activeTab === "dashboard" && (
        <div className="space-y-6">
          {/* Metrics Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Reviews Submitted</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalReviewsCount} Logs</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Avg Teacher Score</span>
              <p className="text-xl font-bold text-slate-800 mt-1 flex items-center gap-1.5">
                4.4 <Star className="h-4.5 w-4.5 text-amber-500 fill-amber-500" />
              </p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Parent Satisfaction</span>
              <p className="text-xl font-bold text-slate-800 mt-1">92.4% Satisfied</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Evaluation Window</span>
              <p className="text-xl font-bold text-emerald-600 mt-1">Active</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Left: Top rated staff ranking */}
            <div className="md:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Teacher Rankings (360-degree Scorecard)</h3>
              <div className="divide-y divide-slate-50 pt-1">
                {staff.filter(s => s.role === "Teacher").slice(0, 5).map(teacher => {
                  const scores = getTeacherRatings(teacher.id);
                  return (
                    <div key={teacher.id} className="py-4.5 flex items-center justify-between gap-4">
                      <div className="space-y-1">
                        <h4 className="text-xs font-extrabold text-slate-800">{teacher.name}</h4>
                        <p className="text-[10px] text-slate-400 font-semibold">{teacher.department} Department</p>
                      </div>
                      
                      {/* Weights Breakdown */}
                      <div className="flex gap-5 text-right font-mono text-[11px] font-bold">
                        <div className="hidden sm:block">
                          <span className="text-[9px] text-slate-400 font-sans block">Student (40%)</span>
                          <span>{scores.studentAvg} / 5</span>
                        </div>
                        <div className="hidden sm:block">
                          <span className="text-[9px] text-slate-400 font-sans block">Parent (30%)</span>
                          <span>{scores.parentAvg} / 5</span>
                        </div>
                        <div className="hidden sm:block">
                          <span className="text-[9px] text-slate-400 font-sans block">Principal (20%)</span>
                          <span>{scores.mgmtAvg} / 5</span>
                        </div>
                        <div>
                          <span className="text-[9px] text-blue-600 font-sans block">Overall Index</span>
                          <span className="text-blue-700 font-black">{scores.overall} ★</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Right: Low Rating Alerts */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex items-center gap-1.5 border-b pb-3 border-slate-50">
                <AlertOctagon className="h-4.5 w-4.5 text-rose-500" />
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest text-rose-800">Critical Reviews Log</h3>
              </div>
              <div className="space-y-3.5 max-h-[300px] overflow-y-auto pr-1">
                {lowRatingsList.length === 0 ? (
                  <p className="text-xs text-slate-400 italic">No critical reviews reported.</p>
                ) : (
                  lowRatingsList.map(rev => {
                    const teacherObj = staff.find(s => s.id === rev.teacherId);
                    return (
                      <div key={rev.id} className="p-3.5 bg-rose-50/40 border border-rose-100 rounded-2xl space-y-1.5">
                        <div className="flex justify-between items-center text-[10px] font-black">
                          <span className="text-rose-700">Prof. {teacherObj?.name || "Teacher"}</span>
                          <span className="text-rose-600 font-mono">{rev.rating} ★</span>
                        </div>
                        <p className="text-[10px] text-slate-500 font-medium italic">"{rev.reviewText}"</p>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. TEACHER -> STUDENT EVALUATION TAB
          ======================================================== */}
      {activeTab === "studentEval" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center border-b pb-3 border-slate-50">
            <div>
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Evaluate Pupil Conduct</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Publish multi-criteria student reports for academic and development goals.</p>
            </div>
            <Button onClick={() => setIsStudentReviewOpen(true)} className="text-xs py-1.5">
              <Plus className="h-3.5 w-3.5" /> Submit Student Review
            </Button>
          </div>

          <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
            <table className="w-full text-xs text-slate-700 font-semibold text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Scholar Student</th>
                  <th className="p-3">Roster Grade</th>
                  <th className="p-3 text-center">Academic (Max 5)</th>
                  <th className="p-3 text-center">Discipline (Max 5)</th>
                  <th className="p-3 text-center">Participation</th>
                  <th className="p-3 text-center">Behavior</th>
                  <th className="p-3">Detailed Comments</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {studentReviews.map(rev => {
                  const student = students.find(s => s.id === rev.studentId);
                  return (
                    <tr key={rev.id}>
                      <td className="p-3 font-extrabold text-slate-800">{student ? student.name : "Scholar Roster"}</td>
                      <td className="p-3 font-bold text-slate-450">{student ? student.grade : "Grade 7"}</td>
                      <td className="p-3 text-center font-bold font-mono text-blue-700">{rev.academicScore} ★</td>
                      <td className="p-3 text-center font-bold font-mono text-blue-700">{rev.disciplineScore} ★</td>
                      <td className="p-3 text-center font-bold font-mono text-blue-700">{rev.participationScore} ★</td>
                      <td className="p-3 text-center font-bold font-mono text-blue-700">{rev.behaviorScore} ★</td>
                      <td className="p-3 text-slate-400 font-semibold italic max-w-xs truncate">{rev.comments}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================
          3. STUDENT/PARENT -> TEACHER EVALUATION TAB
          ======================================================== */}
      {activeTab === "teacherEval" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center border-b pb-3 border-slate-50">
            <div>
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Evaluate School Instructors</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Submit feedback evaluations for teachers regarding teaching quality, responsive support, and professionalism.</p>
            </div>
            <Button onClick={() => setIsTeacherReviewOpen(true)} className="text-xs py-1.5 bg-blue-600 hover:bg-blue-700 text-white">
              <Plus className="h-3.5 w-3.5" /> Evaluate Teacher
            </Button>
          </div>

          {/* Reviews List */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5 pt-2">
            {teacherReviews.map(rev => {
              const teacherObj = staff.find(s => s.id === rev.teacherId);
              const author = rev.anonymous ? "Anonymous" : rev.studentId ? "Student Submitter" : "Parent Submitter";
              return (
                <div key={rev.id} className="bg-slate-50 border border-slate-150 p-5 rounded-2xl flex flex-col justify-between h-44 hover:border-slate-350 transition">
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="bg-slate-100 text-slate-700 text-[9px] font-black px-2 py-0.5 rounded uppercase">
                        Prof. {teacherObj ? teacherObj.name : "Teacher"}
                      </span>
                      <span className="text-[10px] text-blue-700 font-black font-mono flex items-center gap-1">
                        {rev.rating} ★
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 leading-relaxed font-semibold line-clamp-3 italic bg-white p-2.5 border border-slate-100 rounded-xl">
                      "{rev.reviewText}"
                    </p>
                  </div>
                  <div className="flex justify-between text-[9px] font-mono font-black text-slate-400 uppercase tracking-wider pt-2 border-t border-slate-100">
                    <span>By: {author}</span>
                    <span>Date: {rev.created_at}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ========================================================
          4. PRINCIPAL APPRAISAL TAB
          ======================================================== */}
      {activeTab === "management" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center border-b pb-3 border-slate-50">
            <div>
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Management Appraisal Console</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Principals record annual teacher performance reviews and approve promotions or PIP schedules.</p>
            </div>
            <Button onClick={() => setIsMgmtReviewOpen(true)} className="text-xs py-1.5">
              <Plus className="h-3.5 w-3.5" /> Submit Appraisal
            </Button>
          </div>

          <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
            <table className="w-full text-xs text-slate-700 font-semibold text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Instructor Name</th>
                  <th className="p-3">Department</th>
                  <th className="p-3 text-center">Appraisal Score</th>
                  <th className="p-3">Status recommendations</th>
                  <th className="p-3">Appraisal Comments</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {mgmtReviews.map(rev => {
                  const teacher = staff.find(s => s.id === rev.teacherId);
                  
                  // Simple logic to parse the appraisal recommendation status from comments
                  const isPip = rev.comments.includes("PIP");
                  const isPromotion = rev.comments.includes("Promotion");
                  
                  return (
                    <tr key={rev.id}>
                      <td className="p-3 font-extrabold text-slate-800">Prof. {teacher ? teacher.name : "Teacher"}</td>
                      <td className="p-3 font-bold text-slate-450">{teacher ? teacher.department : "Science"}</td>
                      <td className="p-3 text-center font-bold font-mono text-blue-700">{rev.rating} ★</td>
                      <td className="p-3">
                        <Badge variant={isPip ? "danger" : isPromotion ? "success" : "secondary"} className="text-[9px] font-black uppercase">
                          {isPip ? "PIP Recommended" : isPromotion ? "Promotion Recommended" : "Appraisal Approved"}
                        </Badge>
                      </td>
                      <td className="p-3 text-slate-400 font-semibold italic max-w-md truncate">{rev.comments}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================
          5. REVIEW CYCLES TAB
          ======================================================== */}
      {activeTab === "cycles" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center border-b pb-3 border-slate-50">
            <div>
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Feedback Windows & Review Cycles</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Administrators create and toggle active feedback windows.</p>
            </div>
            <Button onClick={() => setIsCycleOpen(true)} className="text-xs py-1.5">
              <Plus className="h-3.5 w-3.5" /> Create Cycle
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5 pt-2">
            {cycles.map(cyc => (
              <div key={cyc.id} className="p-5 bg-slate-50 border border-slate-150 rounded-2xl flex flex-col justify-between h-44 hover:border-slate-350 transition">
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <h4 className="text-xs font-black text-slate-800">{cyc.name}</h4>
                    <Badge variant={cyc.status === "Active" ? "success" : "danger"} className="text-[9px] font-black uppercase">
                      {cyc.status}
                    </Badge>
                  </div>
                  <div className="space-y-1 text-[10px] font-bold text-slate-500 font-mono">
                    <p>Start Date: {cyc.startDate}</p>
                    <p>Closure Target: {cyc.endDate}</p>
                  </div>
                </div>

                <div className="flex gap-2 pt-3 border-t border-slate-100">
                  <Button
                    onClick={() => handleToggleCycleStatus(cyc.id, cyc.status)}
                    className="flex-1 text-[10px] py-1.5 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50"
                  >
                    Toggle Active Window
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ========================================================
          6. PERFORMANCE PROFILES TAB
          ======================================================== */}
      {activeTab === "profiles" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Left Student Profile Dashboard */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b pb-3 border-slate-50">Student Review History</h3>
            
            <div className="w-full">
              <Select
                options={students.map(s => ({ label: `${s.name} (${s.admissionNumber})`, value: s.id }))}
                value={selectedStudentProfileId}
                onChange={(e) => setSelectedStudentProfileId(e.target.value)}
                className="text-xs py-1.5 h-9 rounded-xl bg-slate-50"
              />
            </div>

            {activeStudentProfile && (
              <div className="space-y-4 pt-2">
                {/* Profile Meta Cards */}
                <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-3">
                  <div className="flex justify-between items-center text-xs font-bold font-mono">
                    <span className="text-slate-400">Class Grade:</span>
                    <span>{activeStudentProfile.grade}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs font-bold font-mono">
                    <span className="text-slate-400">Parent Contacts:</span>
                    <span>{activeStudentProfile.parentName}</span>
                  </div>
                </div>

                {/* Reviews historic logs */}
                <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                  <h4 className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Teacher Feedback logs</h4>
                  {studentReviews.filter(r => r.studentId === selectedStudentProfileId).length === 0 ? (
                    <p className="text-xs text-slate-400 italic">No feedback entries recorded for this student.</p>
                  ) : (
                    studentReviews.filter(r => r.studentId === selectedStudentProfileId).map(rev => {
                      const teacher = staff.find(s => s.id === rev.teacherId);
                      return (
                        <div key={rev.id} className="p-3.5 bg-white border border-slate-150 rounded-2xl space-y-2">
                          <div className="flex justify-between items-center text-[10px] font-black">
                            <span>Prof. {teacher?.name || "Teacher"}</span>
                            <span className="text-blue-700 font-mono">Acad Score: {rev.academicScore} ★</span>
                          </div>
                          <p className="text-[10px] text-slate-500 font-medium italic">"{rev.comments}"</p>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Right Teacher Profile Dashboard */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b pb-3 border-slate-50">Teacher 360-degree Profile</h3>

            <div className="w-full">
              <Select
                options={staff.filter(s => s.role === "Teacher").map(t => ({ label: t.name, value: t.id }))}
                value={selectedTeacherProfileId}
                onChange={(e) => setSelectedTeacherProfileId(e.target.value)}
                className="text-xs py-1.5 h-9 rounded-xl bg-slate-50"
              />
            </div>

            {activeTeacherProfile && (
              <div className="space-y-4 pt-2">
                {/* 360 Score card visual wrapper */}
                {(() => {
                  const ratingMetrics = getTeacherRatings(selectedTeacherProfileId);
                  return (
                    <div className="p-5 bg-blue-50/10 border border-blue-200 rounded-3xl space-y-4">
                      <div className="flex justify-between items-center">
                        <span className="text-xs font-black uppercase tracking-wider text-blue-700">360-Degree Index Score</span>
                        <span className="text-xl font-black text-blue-800">{ratingMetrics.overall} ★</span>
                      </div>
                      
                      {/* Metric lines progress */}
                      <div className="space-y-2.5 text-[11px] font-bold">
                        <div>
                          <div className="flex justify-between font-mono">
                            <span className="text-slate-400">Student reviews (40%)</span>
                            <span>{ratingMetrics.studentAvg} / 5</span>
                          </div>
                        </div>
                        <div>
                          <div className="flex justify-between font-mono">
                            <span className="text-slate-400">Parent feedback (30%)</span>
                            <span>{ratingMetrics.parentAvg} / 5</span>
                          </div>
                        </div>
                        <div>
                          <div className="flex justify-between font-mono">
                            <span className="text-slate-400">Principal appraisals (20%)</span>
                            <span>{ratingMetrics.mgmtAvg} / 5</span>
                          </div>
                        </div>
                        <div>
                          <div className="flex justify-between font-mono">
                            <span className="text-slate-400">Mock attendance metric (10%)</span>
                            <span>{ratingMetrics.attendanceScore} / 5</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })()}

                {/* Submissions list */}
                <div className="space-y-3.5 max-h-[200px] overflow-y-auto pr-1">
                  <h4 className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Submissions Details</h4>
                  {teacherReviews.filter(r => r.teacherId === selectedTeacherProfileId).length === 0 ? (
                    <p className="text-xs text-slate-400 italic">No feedback entries recorded.</p>
                  ) : (
                    teacherReviews.filter(r => r.teacherId === selectedTeacherProfileId).map(rev => (
                      <div key={rev.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl space-y-1.5">
                        <div className="flex justify-between text-[9px] font-black text-slate-400 font-mono">
                          <span>Rating: {rev.rating} ★</span>
                          <span>{rev.created_at}</span>
                        </div>
                        <p className="text-[10px] text-slate-550 leading-relaxed font-semibold italic">"{rev.reviewText}"</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          DIALOGS & FORMS
          ======================================================== */}
      {/* 1. REVIEW CYCLE DIALOG */}
      <Dialog
        isOpen={isCycleOpen}
        onClose={() => setIsCycleOpen(false)}
        title="Open Review Cycle Window"
      >
        <form onSubmit={handleCreateCycle} className="space-y-4 pt-1">
          <Input
            label="Cycle Name / Description"
            value={cycleName}
            onChange={(e) => setCycleName(e.target.value)}
            placeholder="e.g. Semester 2 Feedback Appraisal"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Evaluation Start Date"
              type="date"
              value={cycleStart}
              onChange={(e) => setCycleStart(e.target.value)}
            />
            <Input
              label="Evaluation End Date"
              type="date"
              value={cycleEnd}
              onChange={(e) => setCycleEnd(e.target.value)}
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsCycleOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Open Feedback Window</Button>
          </div>
        </form>
      </Dialog>

      {/* 2. STUDENT EVALUATION DIALOG */}
      <Dialog
        isOpen={isStudentReviewOpen}
        onClose={() => setIsStudentReviewOpen(false)}
        title="Submit Student Performance Review"
      >
        <form onSubmit={handleSubmitStudentReview} className="space-y-4 pt-1">
          <Select
            label="Select Scholar Student"
            options={students.map(s => ({ label: `${s.name} (${s.admissionNumber})`, value: s.id }))}
            value={targetStudentId}
            onChange={(e) => setTargetStudentId(e.target.value)}
            required
          />

          <div className="grid grid-cols-2 gap-4 text-xs font-bold text-slate-700">
            <div className="space-y-1.5">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Academic Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={scoreAcad} onChange={(e) => setScoreAcad(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{scoreAcad} ★</div>
            </div>
            <div className="space-y-1.5">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Participation Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={scorePart} onChange={(e) => setScorePart(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{scorePart} ★</div>
            </div>
            <div className="space-y-1.5">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Discipline Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={scoreDiscipline} onChange={(e) => setScoreDiscipline(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{scoreDiscipline} ★</div>
            </div>
            <div className="space-y-1.5">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Behavior Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={scoreBehavior} onChange={(e) => setScoreBehavior(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{scoreBehavior} ★</div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Student Core Strengths"
              value={stStrengths}
              onChange={(e) => setStStrengths(e.target.value)}
              placeholder="e.g. algebraic proofs"
            />
            <Input
              label="Areas for Improvement"
              value={stImprovements}
              onChange={(e) => setStImprovements(e.target.value)}
              placeholder="e.g. homework submission delays"
            />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Detailed Comments</label>
            <textarea
              className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white max-h-24 transition"
              rows={3}
              placeholder="Provide general remarks and specific guidelines for home study support..."
              value={stComments}
              onChange={(e) => setStComments(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsStudentReviewOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Publish Evaluation</Button>
          </div>
        </form>
      </Dialog>

      {/* 3. TEACHER EVALUATION DIALOG */}
      <Dialog
        isOpen={isTeacherReviewOpen}
        onClose={() => setIsTeacherReviewOpen(false)}
        title="Submit Teacher Evaluation"
      >
        <form onSubmit={handleSubmitTeacherReview} className="space-y-4 pt-1">
          <Select
            label="Select Instructor"
            options={staff.filter(s => s.role === "Teacher").map(t => ({ label: t.name, value: t.id }))}
            value={targetTeacherId}
            onChange={(e) => setTargetTeacherId(e.target.value)}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Your Submitter Role"
              options={[
                { label: "Student", value: "Student" },
                { label: "Parent / Guardian", value: "Parent" }
              ]}
              value={reviewType}
              onChange={(e) => setReviewType(e.target.value)}
            />
            <div className="space-y-1.5 text-xs font-bold text-slate-700">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Rating Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={teacherRating} onChange={(e) => setTeacherRating(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{teacherRating} ★</div>
            </div>
          </div>

          <div className="flex items-center gap-2 pt-1.5">
            <input
              type="checkbox"
              id="isAnon"
              checked={isAnonymous}
              onChange={(e) => setIsAnonymous(e.target.checked)}
              className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
            />
            <label htmlFor="isAnon" className="text-xs font-bold text-slate-600 uppercase tracking-wider select-none">
              Submit Review Anonymously
            </label>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Feedback Comments</label>
            <textarea
              className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white max-h-24 transition"
              rows={3}
              placeholder="Review teaching quality, engagement level, responsive communication, and assignments support..."
              value={teacherComments}
              onChange={(e) => setTeacherComments(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsTeacherReviewOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Publish Review</Button>
          </div>
        </form>
      </Dialog>

      {/* 4. PRINCIPAL APPRAISAL DIALOG */}
      <Dialog
        isOpen={isMgmtReviewOpen}
        onClose={() => setIsMgmtReviewOpen(false)}
        title="Teacher Performance Appraisal"
      >
        <form onSubmit={handleSubmitMgmtReview} className="space-y-4 pt-1">
          <Select
            label="Select Teacher Roster"
            options={staff.filter(s => s.role === "Teacher").map(t => ({ label: t.name, value: t.id }))}
            value={targetTeacherId}
            onChange={(e) => setTargetTeacherId(e.target.value)}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Appraisal Decision"
              options={[
                { label: "Approve Appraisal Status", value: "Approved" },
                { label: "Recommend Promotion", value: "Promotion Recommended" },
                { label: "PIP Recommended (Performance Plan)", value: "PIP Recommended" }
              ]}
              value={appraisalStatus}
              onChange={(e) => setAppraisalStatus(e.target.value)}
            />
            <div className="space-y-1.5 text-xs font-bold text-slate-700">
              <label className="uppercase tracking-wider text-[9px] text-slate-400">Appraisal Score (1-5)</label>
              <input type="range" min="1" max="5" step="0.5" value={mgmtRating} onChange={(e) => setMgmtRating(e.target.value)} className="w-full accent-blue-600" />
              <div className="text-right">{mgmtRating} ★</div>
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Appraisal Comments & Directives</label>
            <textarea
              className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white max-h-24 transition"
              rows={3}
              placeholder="Outline administrative compliance directives, classroom observations reviews..."
              value={mgmtComments}
              onChange={(e) => setMgmtComments(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsMgmtReviewOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Save Appraisal</Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
