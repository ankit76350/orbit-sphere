/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getAuthUser, logoutUser, logAction, getStudents, getFees, getHostels, getVisitors } from "./storage";
import Login from "./pages/Login";
import { ToastProvider } from "./components/ui";
import ModInquiryCRM from "./modules/ModInquiryCRM";
import ModAdmission from "./modules/ModAdmission";
import ModStudentMaster from "./modules/ModStudentMaster";
import ModHostel from "./modules/ModHostel";
import ModWallet from "./modules/ModWallet";
import ModFees from "./modules/ModFees";
import ModStaffHRMS from "./modules/ModStaffHRMS";
import ModAttendance from "./modules/ModAttendance";
import ModAcademics from "./modules/ModAcademics";
import ModDiscipline from "./modules/ModDiscipline";
import ModInventory from "./modules/ModInventory";
import ModFoodMess from "./modules/ModFoodMess";
import ModInfirmary from "./modules/ModInfirmary";
import ModSecurity from "./modules/ModSecurity";
import ModSuperAdmin from "./modules/ModSuperAdmin";
import ModLibrary from "./modules/ModLibrary";
import ModTransport from "./modules/ModTransport";
import ModFeedback from "./modules/ModFeedback";
import ModAlumni from "./modules/ModAlumni";
import ModDocGen from "./modules/ModDocGen";
import {
  GraduationCap,
  Sparkles,
  Trophy,
  IdCard,
  Users,
  Home,
  Wallet,
  DollarSign,
  Briefcase,
  UserCheck,
  BookOpen,
  Book,
  Bus,
  Award,
  Scale,
  Boxes,
  ChefHat,
  HeartPulse,
  Shield,
  Settings,
  LogOut,
  Bell,
  Search,
  ChevronDown
} from "lucide-react";
function MainApp() {
  const [currentUser, setCurrentUser] = useState(getAuthUser());
  const [activeModule, setActiveModule] = useState("home");
  const [searchQuery, setSearchQuery] = useState("");
  const [showRoleSwitcher, setShowRoleSwitcher] = useState(false);
  const handleLoginSuccess = (user) => {
    setCurrentUser(user);
    setActiveModule("home");
  };
  const handleLogout = () => {
    if (currentUser) {
      logAction(currentUser.id, currentUser.name, currentUser.role, "User Signed Out", "Cleared local session token");
    }
    logoutUser();
    setCurrentUser(null);
  };
  const handleSwapRole = (nextRole) => {
    if (!currentUser) return;
    let swapName = currentUser.name;
    let permissions = currentUser.permissions;
    if (nextRole === "Super Admin") {
      swapName = "Alistair Sterling (Founder)";
      permissions = ["ALL"];
    } else if (nextRole === "Principal") {
      swapName = "Principal Chloe Smith";
      permissions = ["CRM", "ADMISSIONS", "STUDENT_ROSTER", "STAFF_HR", "FEES", "ATTENDANCE", "ACADEMICS", "DISCIPLINE_LOGS", "SECURITY_PERIMETER"];
    } else if (nextRole === "Warden") {
      swapName = "Warden Robert Miller";
      permissions = ["STUDENT_ROSTER", "HOSTEL_BOARDING", "STUDENT_WALLET", "ATTENDANCE", "DISCIPLINE_LOGS", "KITCHEN_MESS", "HEALTH_CLINIC", "SECURITY_PERIMETER"];
    } else if (nextRole === "Accountant") {
      swapName = "Chief Accountant Liam Davies";
      permissions = ["STUDENT_ROSTER", "STUDENT_WALLET", "FEES", "STAFF_HR", "INVENTORY_STORE"];
    } else if (nextRole === "Parent") {
      swapName = "Guardian Margaret Thatcher";
      permissions = ["MY_CHILD_VIEW", "STUDENT_WALLET", "FEES", "ATTENDANCE", "ACADEMICS", "DISCIPLINE_LOGS", "KITCHEN_MESS"];
    }
    const swapped = {
      ...currentUser,
      role: nextRole,
      name: swapName,
      permissions
    };
    localStorage.setItem("erp_auth_user", JSON.stringify(swapped));
    setCurrentUser(swapped);
    setActiveModule("home");
    setShowRoleSwitcher(false);
    logAction(swapped.id, swapped.name, swapped.role, "Role Swapped (Sandbox)", `Shifted active role permissions to: ${nextRole}`);
  };
  if (!currentUser) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }
  const modulesList = [
    { id: "inquiry", label: "Inquiry CRM", icon: Sparkles, roles: ["Super Admin", "Principal"], comp: ModInquiryCRM },
    { id: "admission", label: "Admissions Panel", icon: GraduationCap, roles: ["Super Admin", "Principal"], comp: ModAdmission },
    { id: "student", label: "Student Master", icon: Users, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent"], comp: ModStudentMaster },
    { id: "hostel", label: "Hostel Boarding", icon: Home, roles: ["Super Admin", "Principal", "Warden"], comp: ModHostel },
    { id: "wallet", label: "Pocket Wallet", icon: Wallet, roles: ["Super Admin", "Warden", "Accountant", "Parent"], comp: ModWallet },
    { id: "fees", label: "Fee Accounting", icon: DollarSign, roles: ["Super Admin", "Principal", "Accountant", "Parent"], comp: ModFees },
    { id: "staff", label: "Staff HRMS", icon: Briefcase, roles: ["Super Admin", "Principal", "Accountant"], comp: ModStaffHRMS },
    { id: "attendance", label: "Class Attendance", icon: UserCheck, roles: ["Super Admin", "Principal", "Warden", "Parent"], comp: ModAttendance },
    { id: "academics", label: "Academic Studies", icon: BookOpen, roles: ["Super Admin", "Principal", "Parent"], comp: ModAcademics },
    { id: "library", label: "Campus Library", icon: Book, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent"], comp: ModLibrary },
    { id: "transport", label: "Transport Desk", icon: Bus, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent", "Driver"], comp: ModTransport },
    { id: "feedback", label: "Feedback Desk", icon: Award, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent"], comp: ModFeedback },
    { id: "alumni", label: "Alumni Directory", icon: Trophy, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent"], comp: ModAlumni },
    { id: "docgen", label: "Doc Gen & ID Cards", icon: IdCard, roles: ["Super Admin", "Principal", "Warden", "Accountant", "Parent"], comp: ModDocGen },
    { id: "discipline", label: "Curfew Discipline", icon: Scale, roles: ["Super Admin", "Principal", "Warden", "Parent"], comp: ModDiscipline },
    { id: "inventory", label: "Store Inventory", icon: Boxes, roles: ["Super Admin", "Accountant"], comp: ModInventory },
    { id: "mess", label: "Food & Mess", icon: ChefHat, roles: ["Super Admin", "Warden", "Parent"], comp: ModFoodMess },
    { id: "infirmary", label: "Infirmary Clinic", icon: HeartPulse, roles: ["Super Admin", "Principal", "Warden"], comp: ModInfirmary },
    { id: "security", label: "Security & Gate", icon: Shield, roles: ["Super Admin", "Principal", "Warden"], comp: ModSecurity },
    { id: "super", label: "SuperAdmin Systems", icon: Settings, roles: ["Super Admin"], comp: ModSuperAdmin }
  ];
  const accessibleModules = modulesList.filter((item) => {
    return item.roles.includes(currentUser.role);
  });
  const searchFilteredModules = accessibleModules.filter(
    (m) => m.label.toLowerCase().includes(searchQuery.toLowerCase())
  );
  const CurrentSubModuleComp = () => {
    const match = accessibleModules.find((m) => m.id === activeModule);
    if (match) {
      const Comp = match.comp;
      return <Comp user={currentUser} />;
    }
    return <WelcomeSummaryDashboard user={currentUser} setActiveModule={setActiveModule} modulesList={accessibleModules} />;
  };
  return <div className="min-h-screen flex text-slate-800 bg-[#f8fafc]">
      
      {
    /* 1. LEFT DASHBOARD SIDEBAR */
  }
      <aside className="w-[260px] bg-white text-slate-600 flex flex-col shrink-0 border-r border-slate-200 h-screen sticky top-0 font-sans shadow-xs">
        
        {
    /* Brand header */
  }
        <div className="p-6 border-b border-slate-100 flex gap-3 items-center">
          <div className="h-8 w-8 bg-blue-600 rounded-lg flex items-center justify-center font-bold text-white text-sm">
            SJ
          </div>
          <div className="leading-tight">
            <h1 className="text-sm font-bold text-slate-900 tracking-tight">ST. JUDE'S</h1>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-0.5">Boarding ERP</p>
          </div>
        </div>

        {
    /* Sidebar link navigation scrolling directory */
  }
        <nav className="flex-1 overflow-y-auto px-4 py-6 space-y-1 scrollbar-thin">
          <button
    onClick={() => setActiveModule("home")}
    className={`w-full flex items-center gap-3 px-3.5 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${activeModule === "home" ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-50 text-slate-600 hover:text-slate-900"}`}
  >
            <div className={`w-1.5 h-1.5 rounded-full ${activeModule === "home" ? "bg-blue-600" : "bg-slate-300"}`} /> Dashboard Welcome
          </button>

          <div className="h-px bg-slate-100 my-4" />
          <p className="text-[10px] uppercase font-bold text-slate-400 tracking-widest px-3.5 mb-2">CAMPUS CHANNELS</p>

          {accessibleModules.map((item) => {
    const Icon = item.icon;
    const active = activeModule === item.id;
    return <button
      key={item.id}
      onClick={() => setActiveModule(item.id)}
      className={`w-full flex items-center gap-3 px-3.5 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${active ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-50 text-slate-600 hover:text-slate-900"}`}
    >
                <div className={`w-1.5 h-1.5 rounded-full ${active ? "bg-blue-600" : "bg-slate-300"}`} />
                {item.label}
              </button>;
  })}
        </nav>

        {
    /* Footer credentials info */
  }
        <div className="p-4 border-t border-slate-100 bg-slate-50 text-[10px] font-bold text-center text-slate-400 font-mono tracking-wide">
          SYSTEM: v4.11-RELEASE
        </div>
      </aside>

      {
    /* 2. MAIN HUB INTERFACE */
  }
      <main className="flex-1 flex flex-col min-w-0 h-screen overflow-y-auto">
        
        {
    /* TOP BAR BAR HEADER */
  }
        <header className="bg-white border-b border-slate-200 px-8 py-4 flex justify-between items-center h-16 shrink-0 sticky top-0 z-40 shadow-xs">
          
          {
    /* Quick Search bar */
  }
          <div className="relative w-80 max-w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
    type="text"
    placeholder="Search features..."
    value={searchQuery}
    onChange={(e) => setSearchQuery(e.target.value)}
    className="bg-slate-50 border border-slate-200 rounded-full pl-10 pr-4 py-1.5 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-blue-505 focus:bg-white w-full transition"
  />
            {searchQuery && <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-slate-200 rounded-2xl shadow-lg p-2.5 space-y-1.5 max-h-56 overflow-y-auto z-50">
                <p className="text-[10px] uppercase font-bold text-slate-400 px-2 tracking-wide">Suggested Modules</p>
                {searchFilteredModules.length === 0 ? <p className="text-xs text-slate-400 p-2 font-medium">No matches available</p> : searchFilteredModules.map((m) => <button
    key={m.id}
    onClick={() => {
      setActiveModule(m.id);
      setSearchQuery("");
    }}
    className="w-full text-left px-2.5 py-1.5 rounded-xl hover:bg-slate-50 text-xs font-bold block"
  >
                      {m.label}
                    </button>)}
              </div>}
          </div>

          {
    /* User badge and Sandbox Role Swapper! */
  }
          <div className="flex items-center gap-4.5">
            
            {
    /* Quick switcher picker */
  }
            <div className="relative">
              <button
    onClick={() => setShowRoleSwitcher(!showRoleSwitcher)}
    className="bg-blue-50 border border-blue-100 hover:bg-blue-100 px-3 py-1.5 rounded-xl text-xs text-blue-700 font-extrabold flex items-center gap-1.5 transition cursor-pointer focus:outline-none"
    title="Simulate other users instant"
  >
                SWAP PROFILE: <span className="underline">{currentUser.role}</span>
                <ChevronDown className="h-3.5 w-3.5" />
              </button>

              {showRoleSwitcher && <div className="absolute right-0 top-full mt-2 bg-white border border-slate-200 rounded-2xl shadow-xl p-2.5 w-56 space-y-1 z-50">
                  <p className="text-[9px] uppercase font-bold text-slate-400 p-2.5 tracking-wider border-b border-slate-100">Simulate Boarding Role</p>
                  
                  <button onClick={() => handleSwapRole("Super Admin")} className="w-full text-left px-3 py-2 text-xs font-bold rounded-xl hover:bg-slate-50 block text-slate-705">
                    ♛ Super Admin (All Access)
                  </button>
                  <button onClick={() => handleSwapRole("Principal")} className="w-full text-left px-3 py-2 text-xs font-bold rounded-xl hover:bg-slate-50 block text-slate-705">
                    💼 Principal / Dean Dashboard
                  </button>
                  <button onClick={() => handleSwapRole("Warden")} className="w-full text-left px-3 py-2 text-xs font-bold rounded-xl hover:bg-slate-50 block text-slate-705">
                    🔑 Warden / Housemaster
                  </button>
                  <button onClick={() => handleSwapRole("Accountant")} className="w-full text-left px-3 py-2 text-xs font-bold rounded-xl hover:bg-slate-50 block text-slate-705">
                    🪙 Accountant Treasury
                  </button>
                  <button onClick={() => handleSwapRole("Parent")} className="w-full text-left px-3 py-2 text-xs font-bold rounded-xl hover:bg-slate-50 block text-slate-705">
                    👪 Parent / Guardian login
                  </button>
                </div>}
            </div>

            {
    /* Profile Avatar label */
  }
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200 text-right font-sans">
              <div>
                <p className="text-xs font-bold text-slate-900 leading-none">{currentUser.name}</p>
                <p className="text-[9px] uppercase text-blue-600 font-bold tracking-widest mt-1">{currentUser.role === "Principal" ? "Principal Cabinet" : currentUser.role}</p>
              </div>
              <div className="h-9 w-9 bg-slate-100 border border-slate-200 rounded-full flex items-center justify-center font-bold text-slate-700 text-xs">
                {currentUser.name.substring(0, 2).toUpperCase()}
              </div>
            </div>

            {
    /* Log out */
  }
            <button
    onClick={handleLogout}
    className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition cursor-pointer"
    title="Logout Session"
  >
              <LogOut className="h-5 w-5" />
            </button>
          </div>

        </header>

        {
    /* 3. CORE SUB-SCREEN RENDERS */
  }
        <div className="flex-1 p-8 space-y-6 overflow-y-auto">
          <CurrentSubModuleComp />
        </div>

      </main>

    </div>;
}
export default function App() {
  return <ToastProvider>
      <MainApp />
    </ToastProvider>;
}
function WelcomeSummaryDashboard({
  user,
  setActiveModule,
  modulesList
}) {
  const students = getStudents();
  const hostels = getHostels();
  const visitors = getVisitors();
  const fees = getFees();
  return <div className="space-y-6">
      
      {
    /* Dynamic Greetings banner */
  }
      <div className="bg-white border border-slate-200 rounded-2xl p-7 relative overflow-hidden shadow-xs">
        <div className="absolute right-0 bottom-0 -mb-24 -mr-24 h-56 w-56 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        
        <div className="space-y-2 max-w-xl">
          <span className="bg-blue-50 text-blue-700 font-bold text-[10px] uppercase tracking-widest px-3 py-1 rounded-full border border-blue-100">
            ST. JUDE'S BOARDING ERP HOME
          </span>
          <h2 className="text-2xl font-bold text-slate-900 leading-tight">Welcome back, {user.name}!</h2>
          <p className="text-xs text-slate-500 leading-relaxed font-semibold">
            Institutional databases are persistent inside LocalStorage. Currently logged in under active permissions of <strong className="text-emerald-600">"{user.role}"</strong> credentials. Use the top swapper badge to shift roles during sandbox evaluation.
          </p>
        </div>
      </div>

      {
    /* Numerical metric statistics */
  }
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-xs flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-bold text-slate-400">STUDENTS ON ROSTER</span>
            <p className="text-xl font-bold text-slate-800 mt-1">{students.length} Registered</p>
          </div>
          <div className="h-10 w-10 bg-blue-50 rounded-xl flex items-center justify-center text-blue-600">
            <Users className="h-5 w-5" />
          </div>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-xs flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-bold text-slate-400">ACTIVE DORM ROOMS</span>
            <p className="text-xl font-bold text-slate-800 mt-1">{hostels.length} Rooms</p>
          </div>
          <div className="h-10 w-10 bg-emerald-50 rounded-xl flex items-center justify-center text-emerald-600">
            <Home className="h-5 w-5" />
          </div>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-xs flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-bold text-slate-400">CAMPUS VISITOR COUNT</span>
            <p className="text-xl font-bold text-slate-800 mt-1">{visitors.length} Logs</p>
          </div>
          <div className="h-10 w-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-600">
            <UserCheck className="h-5 w-5" />
          </div>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-xs flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-bold text-slate-400">CLEARED TERM FEES</span>
            <p className="text-xl font-bold text-slate-800 mt-1">
              {fees.filter((f) => f.status === "Paid").length} Fully Paid
            </p>
          </div>
          <div className="h-10 w-10 bg-blue-50 rounded-xl flex items-center justify-center text-blue-600">
            <DollarSign className="h-5 w-5" />
          </div>
        </div>

      </div>

      {
    /* Bento grid panel layout links */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {
    /* Left Bento: Quick access directory */
  }
        <div className="md:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 space-y-4 shadow-xs">
          <h3 className="text-sm font-bold text-slate-800 uppercase tracking-widest">Your Role Quick Launch Module Desk</h3>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 pt-1">
            {modulesList.map((m) => {
    const Icon = m.icon;
    return <button
      key={m.id}
      onClick={() => setActiveModule(m.id)}
      className="flex items-center gap-4.5 p-4 bg-slate-50 border border-slate-150 rounded-2xl text-left hover:border-blue-400 hover:bg-blue-50/10 transition group cursor-pointer shadow-xs"
    >
                  <div className="h-10 w-10 bg-white border border-slate-200 rounded-xl flex items-center justify-center text-slate-500 group-hover:text-blue-605 group-hover:border-blue-200 transition shrink-0">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-800 group-hover:text-blue-600 transition capitalize">{m.label}</h4>
                    <p className="text-[10px] text-slate-400 font-bold mt-1 uppercase tracking-wide">Enter Console</p>
                  </div>
                </button>;
  })}
          </div>
        </div>

        {
    /* Right Bento: School Event bulletins */
  }
        <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs space-y-4">
          <h3 className="text-sm font-bold text-slate-800 uppercase tracking-widest flex items-center gap-1.5 leading-none">
            <Bell className="h-4.5 w-4.5 text-blue-650" /> Institutional Bulletins
          </h3>
          <p className="text-xs text-slate-400 font-semibold leading-relaxed">Daily camp-fire announcements approved by the chancellor cabinet:</p>

          <div className="space-y-4 pt-1 font-semibold leading-relaxed text-xs">
            
            <div className="p-4 bg-slate-50 border border-slate-150 rounded-2xl">
              <span className="bg-blue-50 text-blue-700 text-[9px] font-bold px-2 py-0.5 rounded uppercase">CAMPUS SECURITY</span>
              <h4 className="text-xs font-bold text-slate-900 mt-1.5 leading-tight">Secondary NFC Gate System Active</h4>
              <p className="text-[11px] text-slate-500 mt-1 italic">"Please ensure all boarder students carry their physical badge chips for entrance scan logs."</p>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-155 rounded-2xl">
              <span className="bg-emerald-50 text-emerald-700 text-[9px] font-bold px-2 py-0.5 rounded uppercase">ACADEMICS</span>
              <h4 className="text-xs font-bold text-slate-900 mt-1.5 leading-tight">Midterm transcripts announced</h4>
              <p className="text-[11px] text-slate-500 mt-1 italic">"All mid-semester worksheets marks have been mapped. Guardians can pay dues in the Treasury tab."</p>
            </div>

          </div>
        </div>

      </div>

    </div>;
}
