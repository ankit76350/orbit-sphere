/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getStudents,
  getFees,
  getTransactions,
  getAttendance,
  getDiscipline,
  getResults,
  saveStudents
} from "../storage";
import { Badge, Tabs, TabsList, TabsTrigger, TabsContent, useToast } from "../components/ui";
import {
  User as UserIcon,
  BookOpen,
  Home,
  DollarSign,
  Wallet,
  Clock,
  AlertTriangle,
  Heart,
  Folder,
  Award,
  Search,
  CheckCircle,
  FileText,
  Users
} from "lucide-react";
export default function ModStudentMaster({ user }) {
  const { addToast } = useToast();
  const [students, setStudents] = useState(getStudents());
  const [selectedStudentId, setSelectedStudentId] = useState("student-1");
  const [searchTerm, setSearchTerm] = useState("");
  const [activeTab, setActiveTab] = useState("personal");
  const [currentStudent, setCurrentStudent] = useState(null);
  useEffect(() => {
    const list = getStudents();
    setStudents(list);
    const match = list.find((s) => s.id === selectedStudentId);
    if (match) {
      setCurrentStudent(match);
    } else if (list.length > 0) {
      setCurrentStudent(list[0]);
      setSelectedStudentId(list[0].id);
    }
  }, [selectedStudentId]);
  const filteredStudents = students.filter(
    (st) => st.name.toLowerCase().includes(searchTerm.toLowerCase()) || st.admissionNumber.toLowerCase().includes(searchTerm.toLowerCase())
  );
  if (!currentStudent) {
    return <div className="p-8 text-center bg-white rounded-3xl border border-slate-100 text-slate-400 font-medium">
        No students registered in LocalStorage. Please use the admissions desk.
      </div>;
  }
  const studentInvoices = getFees().filter((inv) => inv.studentId === currentStudent.id);
  const studentTransactions = getTransactions().filter((tx) => tx.studentId === currentStudent.id);
  const studentAttendance = getAttendance().filter((record) => record.personId === currentStudent.id);
  const studentViolations = getDiscipline().filter((viol) => viol.studentId === currentStudent.id);
  const studentResults = getResults().filter((res) => res.studentId === currentStudent.id);
  const formatCurrency = (val) => `$${Number(val).toLocaleString()}`;
  const handleUpdateDoctorLog = (newLogs) => {
    const freshStudents = getStudents();
    const idx = freshStudents.findIndex((st) => st.id === currentStudent.id);
    if (idx !== -1) {
      freshStudents[idx].medicalDoctorLogs = newLogs;
      saveStudents(freshStudents);
      setCurrentStudent(freshStudents[idx]);
      addToast("Updated", "Infirmary physician log updated inside LocalStorage");
    }
  };
  return <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
      
      {
    /* Sidebar Search Column */
  }
      <div className="lg:col-span-1 bg-white border border-slate-100 rounded-3xl p-4 flex flex-col gap-4 max-h-[750px] overflow-hidden">
        <div>
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Student Index</h3>
          <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Cohort database Query</p>
        </div>

        <div className="relative">
          <input
    type="text"
    placeholder="Search student..."
    value={searchTerm}
    onChange={(e) => setSearchTerm(e.target.value)}
    className="w-full bg-slate-50 border border-slate-200 text-xs rounded-xl pl-9 pr-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
  />
          <Search className="h-4 w-4 text-slate-400 absolute left-3 top-3" />
        </div>

        <div className="flex-1 overflow-y-auto space-y-1.5 pr-1 scroller-hidden">
          {filteredStudents.length === 0 ? <p className="text-xs text-center text-slate-400 py-6 font-medium">No records found</p> : filteredStudents.map((st) => {
    const active = st.id === selectedStudentId;
    return <button
      key={st.id}
      onClick={() => setSelectedStudentId(st.id)}
      className={`w-full text-left p-3 rounded-2xl border transition flex items-center justify-between pointer-events-auto cursor-pointer ${active ? "bg-indigo-50/50 border-indigo-200 text-indigo-900 shadow-xs" : "border-slate-100 hover:border-slate-200 text-slate-700 bg-slate-50/20"}`}
    >
                  <div className="flex items-center gap-2.5">
                    <div className="h-8 w-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-black text-xs shrink-0 uppercase">
                      {st.name.split(" ").map((n) => n[0]).slice(0, 2).join("")}
                    </div>
                    <div>
                      <p className="text-xs font-bold leading-tight line-clamp-1">{st.name}</p>
                      <p className="text-[10px] text-slate-400 font-bold tracking-wider leading-none mt-1">{st.admissionNumber}</p>
                    </div>
                  </div>
                  <span className="text-[9px] bg-white border border-slate-200/80 text-slate-500 font-bold px-1.5 py-0.5 rounded-md shrink-0 uppercase">
                    {st.grade.split(" ")[1]}
                  </span>
                </button>;
  })}
        </div>
      </div>

      {
    /* Roster Profile Tabs Content Column */
  }
      <div className="lg:col-span-3 space-y-6">
        
        {
    /* Profile Card Header banner */
  }
        <div className="bg-slate-900 border border-slate-850 rounded-3xl p-6 text-white flex flex-col sm:flex-row gap-5 items-center justify-between relative overflow-hidden">
          <div className="absolute right-0 top-0 -mt-10 -mr-10 h-40 w-40 bg-indigo-600 rounded-full blur-3xl opacity-20 pointer-events-none" />
          
          <div className="flex flex-col sm:flex-row items-center gap-4.5 text-center sm:text-left">
            <div className="h-16 w-16 rounded-full bg-indigo-600 border-2 border-indigo-400 text-white flex items-center justify-center text-xl font-black shrink-0 uppercase shadow-md">
              {currentStudent.name.split(" ").map((n) => n[0]).slice(0, 2).join("")}
            </div>
            <div>
              <div className="flex flex-wrap gap-2 justify-center sm:justify-start items-center">
                <h2 className="text-xl font-extrabold tracking-tight">{currentStudent.name}</h2>
                <Badge variant="success">Active scholar</Badge>
              </div>
              <p className="text-xs text-indigo-300 mt-1.5 font-semibold flex items-center gap-1.5 justify-center sm:justify-start">
                <BookOpen className="h-3.5 w-3.5" />
                {currentStudent.grade} | Admission ID: <span className="text-white font-black">{currentStudent.admissionNumber}</span>
              </p>
            </div>
          </div>

          <div className="flex gap-4.5 text-center shrink-0">
            <div className="bg-white/10 px-4 py-2 rounded-2xl backdrop-blur-md border border-white/5">
              <span className="text-[9px] uppercase tracking-wider text-indigo-300 font-bold">Pocket Wallet</span>
              <p className="text-lg font-black text-emerald-400 mt-0.5">{formatCurrency(currentStudent.walletBalance)}</p>
            </div>
            <div className="bg-white/10 px-4 py-2 rounded-2xl backdrop-blur-md border border-white/5">
              <span className="text-[9px] uppercase tracking-wider text-indigo-300 font-bold">Unpaid Fees</span>
              <p className="text-lg font-black text-rose-400 mt-0.5">
                {formatCurrency(studentInvoices.filter((i) => i.status !== "Paid").reduce((sum, inv) => sum + (inv.amount - inv.paidAmount), 0))}
              </p>
            </div>
          </div>
        </div>

        {
    /* Modular Tabs list */
  }
        <div className="bg-white border border-slate-100 rounded-3xl p-6">
          <Tabs activeTab={activeTab} onChange={setActiveTab}>
            <TabsList className="border-b border-slate-100 max-w-full overflow-x-auto gap-0.5 scrollbar-thin">
              <TabsTrigger value="personal">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <UserIcon className="h-3.5 w-3.5" /> Personal
                </div>
              </TabsTrigger>
              <TabsTrigger value="academic">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <BookOpen className="h-3.5 w-3.5" /> Academics
                </div>
              </TabsTrigger>
              <TabsTrigger value="hostel">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Home className="h-3.5 w-3.5" /> Hostel Dorm
                </div>
              </TabsTrigger>
              <TabsTrigger value="fees">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <DollarSign className="h-3.5 w-3.5" /> Fees
                </div>
              </TabsTrigger>
              <TabsTrigger value="wallet">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Wallet className="h-3.5 w-3.5" /> Wallet
                </div>
              </TabsTrigger>
              <TabsTrigger value="attendance">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Clock className="h-3.5 w-3.5" /> Attendance
                </div>
              </TabsTrigger>
              <TabsTrigger value="discipline">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <AlertTriangle className="h-3.5 w-3.5" /> Discipline
                </div>
              </TabsTrigger>
              <TabsTrigger value="medical">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Heart className="h-3.5 w-3.5" /> Medical
                </div>
              </TabsTrigger>
              <TabsTrigger value="documents">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Folder className="h-3.5 w-3.5" /> Documents
                </div>
              </TabsTrigger>
              <TabsTrigger value="results">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Award className="h-3.5 w-3.5" /> Results
                </div>
              </TabsTrigger>
              <TabsTrigger value="siblings">
                <div className="flex items-center gap-1.5 text-xs font-bold leading-none py-1">
                  <Users className="h-3.5 w-3.5" /> Siblings
                </div>
              </TabsTrigger>
            </TabsList>

            {
    /* TAB CONTENT: Personal */
  }
            <TabsContent value="personal">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
                <div className="space-y-4 bg-slate-50 p-5 rounded-2xl border border-slate-100">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-widest">Roster Identity Logs</h4>
                  <div className="grid grid-cols-2 gap-4 text-xs font-semibold text-slate-700">
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">DOB</p>
                      <p className="mt-1 text-slate-800">{currentStudent.dob}</p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Gender</p>
                      <p className="mt-1 text-slate-800">{currentStudent.gender}</p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Blood Group</p>
                      <p className="mt-1 text-slate-800">{currentStudent.medicalBloodGroup}</p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Dorm Optin</p>
                      <p className="mt-1 text-slate-800">{currentStudent.hostelOptIn ? "Resident Yes" : "No"}</p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4 bg-slate-50 p-5 rounded-2xl border border-slate-100">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-widest">Linked Guardian Parameters</h4>
                  <div className="space-y-3.5 text-xs font-semibold text-slate-700">
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Name</p>
                      <p className="mt-1 text-slate-800">{currentStudent.parentName}</p>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Phone</p>
                        <p className="mt-1 text-slate-800">{currentStudent.parentPhone}</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Email</p>
                        <p className="mt-1 text-slate-800">{currentStudent.parentEmail}</p>
                      </div>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Street Mail Address</p>
                      <p className="mt-1 text-slate-800">{currentStudent.address}</p>
                    </div>
                  </div>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Academic */
  }
            <TabsContent value="academic">
              <div className="space-y-5 pt-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                    <p className="text-[9px] uppercase font-bold text-slate-400">Class Assigned Teacher</p>
                    <p className="text-sm font-bold text-slate-800 mt-1">Prof. Liam Johnson</p>
                  </div>
                  <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                    <p className="text-[9px] uppercase font-bold text-slate-400">Joined Date</p>
                    <p className="text-sm font-bold text-slate-800 mt-1">{currentStudent.joinedDate}</p>
                  </div>
                </div>
                <div className="bg-slate-50/50 p-4 rounded-2xl border border-slate-200/50">
                  <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest mb-3">Academic Subjects</h4>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    {["Mathematics", "Inorganic Chemistry", "Classical Physics"].map((sub, idx) => <div key={idx} className="bg-white p-3 rounded-xl border border-slate-100 flex items-center justify-between text-xs font-semibold text-slate-800">
                        <span>{sub}</span>
                        <Badge variant="info">Passing</Badge>
                      </div>)}
                  </div>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Hostel */
  }
            <TabsContent value="hostel">
              <div className="pt-4 space-y-4">
                {currentStudent.hostelOptIn ? <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                    <div className="bg-indigo-50 p-5 rounded-2xl border border-indigo-100/50 text-indigo-950 font-semibold space-y-3">
                      <h4 className="text-xs font-bold text-indigo-900 uppercase tracking-widest flex items-center gap-2">
                        <Home className="h-4 w-4" /> Active Bunk Allocation Details
                      </h4>
                      <p className="text-xs text-indigo-800">Assigning accurate rooms coordinates within sandbox premises:</p>
                      <div className="grid grid-cols-2 gap-3 text-xs leading-relaxed">
                        <div>
                          <p className="text-[9px] text-indigo-500 uppercase font-black uppercase">Hall / House</p>
                          <p className="font-bold text-indigo-950">{currentStudent.hostelBuilding}</p>
                        </div>
                        <div>
                          <p className="text-[9px] text-indigo-500 uppercase font-black uppercase">Floor Coordinate</p>
                          <p className="font-bold text-indigo-805">{currentStudent.hostelFloor}</p>
                        </div>
                        <div>
                          <p className="text-[9px] text-indigo-500 uppercase font-black uppercase">Room Designation</p>
                          <p className="font-bold text-indigo-805">Room {currentStudent.hostelRoomNumber}</p>
                        </div>
                        <div>
                          <p className="text-[9px] text-indigo-500 uppercase font-black uppercase">Bunk Assignment</p>
                          <p className="font-bold text-indigo-805">{currentStudent.hostelBedNumber}</p>
                        </div>
                      </div>
                    </div>

                    <div className="bg-slate-50 p-5 rounded-2xl border border-slate-100 flex flex-col justify-between">
                      <div>
                        <h4 className="text-xs font-extrabold text-slate-700 uppercase tracking-widest">Warden Guidelines</h4>
                        <p className="text-xs text-slate-400 mt-2 font-semibold">
                          Weekly inspection check list score is good. Double blanket and study desk set in good condition. curfew alert checklist: No warnings reported.
                        </p>
                      </div>
                      <div className="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-[11px] text-slate-500 font-bold">
                        <span>Last Evening Roster Status</span>
                        <Badge variant="success">Present in Room</Badge>
                      </div>
                    </div>
                  </div> : <div className="p-8 text-center bg-slate-50 border border-slate-100 rounded-2xl text-slate-400 font-semibold">
                    This scholar has not opted into boarding programs.
                  </div>}
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Fees */
  }
            <TabsContent value="fees">
              <div className="pt-4 space-y-4">
                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Obligation Ledger Invoice list</h4>
                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-3">Fee Type</th>
                        <th className="p-3">Invoiced Term</th>
                        <th className="p-3">Pay Amount</th>
                        <th className="p-3">Paid amount</th>
                        <th className="p-3">Ledger Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {studentInvoices.length === 0 ? <tr>
                          <td colSpan={5} className="p-4 text-center text-slate-400">No active term invoices found</td>
                        </tr> : studentInvoices.map((inv) => <tr key={inv.id}>
                            <td className="p-3 font-bold text-slate-800">{inv.feeType}</td>
                            <td className="p-3 text-slate-450 font-bold">Fall Term 2026</td>
                            <td className="p-3 text-slate-800">{formatCurrency(inv.amount)}</td>
                            <td className="p-3 text-slate-800 font-black">{formatCurrency(inv.paidAmount)}</td>
                            <td className="p-3">
                              <Badge variant={inv.status === "Paid" ? "success" : inv.status === "Partially Paid" ? "warning" : "danger"}>
                                {inv.status}
                              </Badge>
                            </td>
                          </tr>)}
                    </tbody>
                  </table>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Wallet */
  }
            <TabsContent value="wallet">
              <div className="pt-4 space-y-4">
                <div className="flex justify-between items-center bg-emerald-50/50 border border-emerald-100 p-4 rounded-2xl">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-emerald-100 text-emerald-805 flex items-center justify-center font-bold">
                      <Wallet className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-[10px] text-emerald-600 font-extrabold uppercase uppercase">Available RFID Balance</p>
                      <p className="text-xl font-extrabold text-slate-800 mt-0.5">{formatCurrency(currentStudent.walletBalance)}</p>
                    </div>
                  </div>
                  <Badge variant="success">Authorized</Badge>
                </div>

                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest pt-2">Transaction Logs</h4>
                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-3">Transaction ID</th>
                        <th className="p-3">Timestamp</th>
                        <th className="p-3">Remarks / material details</th>
                        <th className="p-3">Debit/Credit</th>
                        <th className="p-3">Amount</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {studentTransactions.length === 0 ? <tr>
                          <td colSpan={5} className="p-4 text-center text-slate-400">No pocket wallet material buys recorded.</td>
                        </tr> : studentTransactions.map((tx) => <tr key={tx.id}>
                            <td className="p-3 font-mono text-[10px] text-slate-400 font-bold uppercase">{tx.id}</td>
                            <td className="p-3 font-bold text-slate-400">{tx.date}</td>
                            <td className="p-3 text-slate-800">{tx.remarks}</td>
                            <td className="p-3">
                              <Badge variant={tx.type === "Credit" ? "success" : "danger"}>
                                {tx.type === "Credit" ? "Deposited" : "Deducted"}
                              </Badge>
                            </td>
                            <td className="p-3 font-bold text-slate-800">{formatCurrency(tx.amount)}</td>
                          </tr>)}
                    </tbody>
                  </table>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Attendance */
  }
            <TabsContent value="attendance">
              <div className="pt-4 space-y-4">
                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Attendance Logs</h4>
                <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                  <table className="w-full text-xs font-semibold text-slate-700 text-left">
                    <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                      <tr>
                        <th className="p-3">Checkin Type</th>
                        <th className="p-3">Date</th>
                        <th className="p-3">Audit timestamp</th>
                        <th className="p-3">Registered Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {studentAttendance.length === 0 ? <tr>
                          <td colSpan={4} className="p-4 text-center text-slate-400">No attendance registers written for this student yet.</td>
                        </tr> : studentAttendance.map((reg) => <tr key={reg.id}>
                            <td className="p-3 font-bold text-slate-800">{reg.type}</td>
                            <td className="p-3 text-slate-400 font-bold">{reg.date}</td>
                            <td className="p-3 text-slate-800">{reg.timestamp || "N/A"}</td>
                            <td className="p-3">
                              <Badge variant={reg.status === "Present" ? "success" : reg.status === "Late" ? "warning" : "danger"}>
                                {reg.status}
                              </Badge>
                            </td>
                          </tr>)}
                    </tbody>
                  </table>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Discipline */
  }
            <TabsContent value="discipline">
              <div className="pt-4 space-y-4">
                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Violation Records</h4>
                {studentViolations.length === 0 ? <div className="p-8 text-center bg-slate-50 border border-slate-100 rounded-2xl text-slate-400 font-semibold flex flex-col items-center gap-2">
                    <CheckCircle className="h-10 w-10 text-emerald-500" />
                    <span>No active disciplinary incidents reported. Impeccable conduct.</span>
                  </div> : <div className="space-y-3.5">
                    {studentViolations.map((viol) => <div key={viol.id} className="bg-slate-50 border border-slate-100 p-4 rounded-2xl flex flex-col sm:flex-row justify-between gap-4">
                        <div className="space-y-1.5">
                          <div className="flex flex-wrap gap-2 items-center">
                            <span className="text-xs font-bold text-slate-800">{viol.violationType}</span>
                            <Badge variant="danger">{viol.severity} Severity</Badge>
                          </div>
                          <p className="text-xs text-slate-400 font-semibold">Incident logged on {viol.incidentDate} by {viol.wardenOrTeacher}</p>
                          <p className="text-xs text-slate-500 italic mt-1 bg-white p-2.5 rounded-lg border border-slate-100">"{viol.actionsTaken}"</p>
                        </div>
                        <div className="text-right shrink-0 flex flex-col justify-between items-end">
                          <Badge variant="warning">{viol.status}</Badge>
                          {viol.fineAmount && <p className="text-xs font-bold text-rose-600 mt-2">Fine: {formatCurrency(viol.fineAmount)}</p>}
                        </div>
                      </div>)}
                  </div>}
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Medical */
  }
            <TabsContent value="medical">
              <div className="pt-4 space-y-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-rose-50 border border-rose-100 p-5 rounded-2xl">
                    <h5 className="text-xs font-bold text-rose-850 uppercase tracking-wider flex items-center gap-2">
                      <Heart className="h-4 w-4 text-rose-600" /> Allergy Alerts
                    </h5>
                    {currentStudent.medicalAllergies.length === 0 ? <p className="text-xs text-rose-800 font-semibold mt-3">No allergies declared.</p> : <div className="flex flex-wrap gap-1.5 mt-3">
                        {currentStudent.medicalAllergies.map((all) => <span key={all} className="bg-rose-100 text-rose-800 text-[10px] font-extrabold px-2.5 py-1 rounded-md uppercase">
                            {all}
                          </span>)}
                      </div>}
                  </div>

                  <div className="bg-slate-50 border border-slate-150 p-5 rounded-2xl">
                    <h5 className="text-xs font-bold text-slate-705 uppercase tracking-wider flex items-center gap-2">
                      <Heart className="h-4 w-4 text-emerald-600" /> Chronic Conditions
                    </h5>
                    {currentStudent.medicalConditions.length === 0 ? <p className="text-xs text-slate-600 font-semibold mt-3">No long-term ailments reported.</p> : <div className="flex flex-wrap gap-1.5 mt-3">
                        {currentStudent.medicalConditions.map((cond) => <span key={cond} className="bg-white border border-slate-200 text-slate-700 text-[10px] font-extrabold px-2.5 py-1 rounded-md uppercase">
                            {cond}
                          </span>)}
                      </div>}
                  </div>
                </div>

                <div className="bg-white border border-slate-150 rounded-2xl p-4 space-y-3">
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-widest block">Physician Log (Editable Archive)</label>
                  <textarea
    rows={4}
    value={currentStudent.medicalDoctorLogs}
    onChange={(e) => handleUpdateDoctorLog(e.target.value)}
    className="w-full bg-slate-50 border border-slate-150 text-slate-800 rounded-xl p-3 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
    placeholder="Provide height, weight, vaccines tracking..."
  />
                  <p className="text-[10px] text-slate-400 italic">Typing in details auto-saves directly to LocalStorage sandbox records.</p>
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Documents */
  }
            <TabsContent value="documents">
              <div className="pt-4 space-y-4">
                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Document Verification locker</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {[
    { name: "Birth_Certificate_Scan.pdf", size: "1.4 MB", status: "Verified" },
    { name: "Immunization_Hepatitis_Details.pdf", size: "890 KB", status: "Verified" },
    { name: "Previous_Report_Card_Official_Sign.pdf", size: "2.1 MB", status: "Pending Review" }
  ].map((doc, idx) => <div key={idx} className="bg-slate-50 border border-slate-100 p-4 rounded-xl flex justify-between items-center text-xs font-semibold">
                      <div className="flex gap-2.5 items-center">
                        <FileText className="h-8 w-8 text-indigo-500 shrink-0" />
                        <div>
                          <p className="text-slate-800 leading-tight font-bold">{doc.name}</p>
                          <p className="text-[10px] text-slate-400 mt-1 font-bold">{doc.size}</p>
                        </div>
                      </div>
                      <Badge variant={doc.status === "Verified" ? "success" : "warning"}>{doc.status}</Badge>
                    </div>)}
                </div>
              </div>
            </TabsContent>

            {
    /* TAB CONTENT: Results */
  }
            <TabsContent value="results">
              <div className="pt-4 space-y-4">
                <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Midterm Academic Card</h4>
                {studentResults.length === 0 ? <div className="p-8 text-center bg-slate-50 border border-slate-100 rounded-2xl text-slate-400 font-semibold">
                    No active examinations cards submitted for this grade.
                  </div> : <div className="space-y-4">
                    {studentResults.map((res) => <div key={res.id} className="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
                        <div className="flex justify-between items-center border-b border-white pb-3 mb-3">
                          <div>
                            <h5 className="text-sm font-bold text-indigo-950">{res.examName}</h5>
                            <p className="text-xs text-slate-450 mt-1">Status: Checked and Signed by Board</p>
                          </div>
                          <div className="text-right">
                            <p className="text-xl font-black text-indigo-600">{res.totalPercentage}%</p>
                            <p className="text-xs font-extrabold text-slate-500 uppercase">Grade: {res.overallGrade}</p>
                          </div>
                        </div>

                        <div className="space-y-2 text-xs">
                          {res.marks.map((subMark, subIdx) => <div key={subIdx} className="bg-white p-3 rounded-lg border border-slate-100 flex justify-between items-center font-bold">
                              <span className="text-slate-800">{subMark.subject}</span>
                              <div className="flex gap-4 items-center">
                                <span className="text-slate-400 text-[11px] font-bold">Obtained: {subMark.obtainedMarks} / {subMark.maxMarks}</span>
                                <Badge variant="secondary">{subMark.grade}</Badge>
                              </div>
                            </div>)}
                        </div>

                        <p className="text-xs text-slate-450 italic mt-4 bg-indigo-50 p-3 rounded-xl border border-indigo-100/30">
                          <span className="font-extrabold text-slate-700 not-italic uppercase text-[10px] block mb-1">Dean Feedbacks:</span>
                          "{res.feedback}"
                        </p>
                      </div>)}
                  </div>}
              </div>
            </TabsContent>

            {/* TAB CONTENT: Siblings */}
            <TabsContent value="siblings">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
                <div className="space-y-4 bg-slate-900 border border-slate-850 p-6 rounded-3xl text-white relative overflow-hidden flex flex-col justify-between h-56">
                  <div className="absolute right-0 top-0 -mt-10 -mr-10 h-36 w-36 bg-indigo-600 rounded-full blur-3xl opacity-20 pointer-events-none" />
                  
                  <div className="flex items-center gap-4.5 z-10">
                    <div className="h-14 w-14 rounded-full bg-indigo-600 border-2 border-indigo-400 text-white flex items-center justify-center text-lg font-black shrink-0 uppercase shadow-md">
                      GP
                    </div>
                    <div>
                      <div className="flex gap-2 items-center">
                        <h4 className="text-base font-extrabold tracking-tight">Gianna Patel</h4>
                        <Badge variant="success">Active scholar</Badge>
                      </div>
                      <p className="text-xs text-indigo-300 mt-1.5 font-semibold">
                        Grade 10 | Admission ID: <span className="text-white font-black">STJ2025-1018</span>
                      </p>
                    </div>
                  </div>

                  <div className="flex gap-4 text-center z-10 pt-2">
                    <div className="bg-white/10 px-4 py-2 rounded-2xl border border-white/5 flex-1">
                      <span className="text-[9px] uppercase tracking-wider text-indigo-300 font-bold">Pocket Wallet</span>
                      <p className="text-base font-black text-emerald-400 mt-0.5">$305</p>
                    </div>
                    <div className="bg-white/10 px-4 py-2 rounded-2xl border border-white/5 flex-1">
                      <span className="text-[9px] uppercase tracking-wider text-indigo-300 font-bold">Unpaid Fees</span>
                      <p className="text-base font-black text-rose-400 mt-0.5">$6,300</p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="space-y-4 bg-slate-50 p-5 rounded-2xl border border-slate-100">
                    <h4 className="text-xs font-bold text-slate-800 uppercase tracking-widest">Roster Identity Logs</h4>
                    <div className="grid grid-cols-2 gap-4 text-xs font-semibold text-slate-700">
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">DOB</p>
                        <p className="mt-1 text-slate-800">2011-06-26</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Gender</p>
                        <p className="mt-1 text-slate-800">Male</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Blood Group</p>
                        <p className="mt-1 text-slate-800">O+</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Dorm Optin</p>
                        <p className="mt-1 text-slate-800">Resident Yes</p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4 bg-slate-50 p-5 rounded-2xl border border-slate-100">
                    <h4 className="text-xs font-bold text-slate-800 uppercase tracking-widest">Linked Guardian Parameters</h4>
                    <div className="space-y-3.5 text-xs font-semibold text-slate-700">
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Name</p>
                        <p className="mt-1 text-slate-800">Diya Patel</p>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Phone</p>
                          <p className="mt-1 text-slate-800">+1 (555) 601-1932</p>
                        </div>
                        <div>
                          <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Guardian Email</p>
                          <p className="mt-1 text-slate-800 font-mono">gianna.patel.parent@gmail.com</p>
                        </div>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Street Mail Address</p>
                        <p className="mt-1 text-slate-800">666 Whispering Pines Road, Cityville</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>

      </div>

    </div>;
}
