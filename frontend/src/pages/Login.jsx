/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { useToast, Button, Input, Select } from "../components/ui";
import { setAuthUser, initializeStorage, logAction } from "../storage";
import { Shield, Sparkles, BookOpen, Key, AlertCircle } from "lucide-react";
export default function Login({ onLoginSuccess }) {
  const { addToast } = useToast();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [view, setView] = useState("sign-in");
  const [forgotEmail, setForgotEmail] = useState("");
  const [otpCode, setOtpCode] = useState(["", "", "", ""]);
  const [simulatedRole, setSimulatedRole] = useState("School Admin");
  const presets = [
    { role: "Super Admin", email: "superadmin@erp.com", name: "Alastair Vance", desc: "Manage tenants, SaaS plans" },
    { role: "School Admin", email: "admin@stjude.edu", name: "Eleanor Vance", desc: "All operations control" },
    { role: "Principal", email: "principal@stjude.edu", name: "Dr. Arthur Pendelton", desc: "Academics & reports" },
    { role: "Accountant", email: "finance@stjude.edu", name: "Grover Cleveland", desc: "Fees ledger, billing" },
    { role: "HR Manager", email: "hr@stjude.edu", name: "Eleanor Vance", desc: "Staff payroll, leaves" },
    { role: "Teacher", email: "lh.maths@stjude.edu", name: "Prof. Liam Johnson", desc: "Class roster, grades" },
    { role: "Warden", email: "m.brody@stjude.edu", name: "Marcus Brody", desc: "Dorm rooms & out-passes" },
    { role: "Store Manager", email: "store@stjude.edu", name: "Silas Marner", desc: "Inventory stock & sales" },
    { role: "Parent", email: "liam.smith.parent@gmail.com", name: "Edward Smith", desc: "Child dashboard, wallets" },
    { role: "Student", email: "liam.smith@stjude.edu", name: "Liam Smith", desc: "Check homework, balance" }
  ];
  const handleStandardSubmit = (e) => {
    e.preventDefault();
    if (!email) {
      addToast("Error", "Please input your login email", "error");
      return;
    }
    setLoading(true);
    const match = presets.find((p) => p.email.toLowerCase() === email.toLowerCase());
    setTimeout(() => {
      setLoading(false);
      setView("otp");
      addToast("Information", "Demo OTP: 4022 has been sent to your simulated device", "info");
    }, 800);
  };
  const handleOtpVerify = (e) => {
    e.preventDefault();
    const joined = otpCode.join("");
    if (joined !== "4022" && joined !== "1234") {
      addToast("Incorrect Code", "Please input 4022 (demo code) or 1234.", "error");
      return;
    }
    const match = presets.find((p) => p.email.toLowerCase() === email.toLowerCase()) || presets.find((p) => p.role === simulatedRole);
    const loggedUser = {
      id: `usr-${match.role.toLowerCase().replace(" ", "-")}`,
      name: match.name,
      email: match.email,
      role: match.role,
      schoolId: "school-01",
      permissions: ["ALL_PERMISSIONS"]
    };
    initializeStorage();
    setAuthUser(loggedUser);
    logAction(loggedUser.id, loggedUser.name, loggedUser.role, "User Logged In", "Successfully completed standard credentials + OTP verification");
    addToast("Success", `Welcome back, ${loggedUser.name} (${loggedUser.role})`, "success");
    onLoginSuccess(loggedUser);
  };
  const handleQuickLogin = (preset) => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const loggedUser = {
        id: `usr-${preset.role.toLowerCase().replace(" ", "-")}`,
        name: preset.name,
        email: preset.email,
        role: preset.role,
        schoolId: "school-01",
        permissions: ["ALL_PERMISSIONS"]
      };
      initializeStorage();
      setAuthUser(loggedUser);
      logAction(loggedUser.id, loggedUser.name, loggedUser.role, "User Presets Login", `Bypassed standard layout. Simulation role loaded: ${preset.role}`);
      addToast("Welcome Back", `Successfully simulated ${preset.role} login`, "success");
      onLoginSuccess(loggedUser);
    }, 300);
  };
  const handleForgotSubmit = (e) => {
    e.preventDefault();
    if (!forgotEmail) {
      addToast("Error", "Input email to receive reset code", "error");
      return;
    }
    addToast("Passcode Sent", "Simulated password reset email has been dispatched", "info");
    setView("sign-in");
  };
  return <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row antialiased">

    {
      /* Branding Column */
    }
    <div className="w-full md:w-5/12 bg-slate-900 flex flex-col justify-between p-8 md:p-12 text-white relative overflow-hidden shrink-0">
      <div className="absolute top-0 right-0 -mr-24 -mt-24 w-96 h-96 bg-indigo-600 rounded-full blur-3xl opacity-20 pointer-events-none" />
      <div className="absolute bottom-0 left-0 -ml-24 -mb-24 w-96 h-96 bg-emerald-600 rounded-full blur-3xl opacity-20 pointer-events-none" />

      <div className="flex items-center gap-2.5 z-10">
        <div className="bg-indigo-600 p-2.5 rounded-2xl flex items-center justify-center">
          <BookOpen className="h-6 w-6 text-white" />
        </div>
        <div>
          <h1 className="text-xl font-bold tracking-tight">St. Jude ERP</h1>
          <p className="text-[10px] uppercase text-indigo-400 font-bold tracking-widest leading-none mt-0.5">Boarding Management Suite</p>
        </div>
      </div>

      <div className="my-10 z-10 max-w-sm">
        <span className="bg-white/10 text-emerald-400 px-3 py-1 rounded-full text-xs font-semibold mb-4 inline-flex items-center gap-1.5 backdrop-blur-md">
          <Sparkles className="h-3.5 w-3.5" /> Core Release v4.2
        </span>
        <h2 className="text-3xl font-extrabold tracking-tight mt-3 text-slate-100 leading-tight">
          Comprehensive Management for High-Tier Boarding Institutions.
        </h2>
        <p className="text-sm mt-4 text-slate-400 leading-relaxed font-medium">
          Seamlessly orchestrate student profiles, medical registers, automated pocket wallets, outpass approvals, and hostel inventory lists.
        </p>
      </div>

      <div className="text-xs text-slate-500 z-10 font-bold flex items-center gap-1.5">
        <Shield className="h-3.5 w-3.5 text-indigo-500" />
        Secured with State Sandbox Encryption
      </div>
    </div>

    {
      /* Control Actions / Form Column */
    }
    <div className="flex-1 flex flex-col justify-center items-center p-6 md:p-16 overflow-y-auto">
      <div className="w-full max-w-xl flex flex-col gap-8">

        {
          /* Main Card */
        }
        <div className="bg-white p-8 md:p-10 rounded-3xl border border-slate-100 shadow-xl">
          {view === "sign-in" && <form onSubmit={handleStandardSubmit} className="flex flex-col gap-5">
            <div>
              <h3 className="text-2xl font-bold text-slate-800 tracking-tight">Institution Portal Sign-In</h3>
              <p className="text-sm text-slate-400 font-medium mt-1">
                Input your school credentials to verify your workspace.
              </p>
            </div>

            <div className="bg-yellow-50/50 border border-yellow-200/50 rounded-2xl p-4 flex gap-3 text-yellow-800 text-xs font-medium leading-relaxed">
              <AlertCircle className="h-4 w-4 text-yellow-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Investor Sandbox Info:</span> Use the pain-free quick role shortcuts below to instantly boot any role with zero typing!
              </div>
            </div>

            <Input
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@stjude.edu"
            />

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Password</label>
                <button
                  type="button"
                  onClick={() => setView("forgot")}
                  className="text-xs font-bold text-indigo-600 hover:text-indigo-800 transition"
                >
                  Forgot?
                </button>
              </div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
                placeholder="••••••••"
              />
            </div>

            <Select
              label="Quick Preset Designation Override"
              options={presets.map((p) => ({ label: `${p.role} - Demo Simulation`, value: p.role }))}
              value={simulatedRole}
              onChange={(e) => {
                const preset = presets.find((p) => p.role === e.target.value);
                if (preset) {
                  setSimulatedRole(preset.role);
                  setEmail(preset.email);
                  setPassword("demo-1234");
                }
              }}
            />

            <Button type="submit" disabled={loading} className="py-3 rounded-xl w-full">
              {loading ? "Authenticating Workspace..." : "Initiate Session"}
            </Button>
          </form>}

          {view === "forgot" && <form onSubmit={handleForgotSubmit} className="flex flex-col gap-5">
            <div>
              <h3 className="text-2xl font-bold text-slate-800">Restore Credentials</h3>
              <p className="text-sm text-slate-400 font-medium mt-1">
                Provide your institutional registered email to acquire a secure link.
              </p>
            </div>

            <Input
              label="Registered Email Address"
              type="email"
              value={forgotEmail}
              onChange={(e) => setForgotEmail(e.target.value)}
              placeholder="headofschool@stjude.edu"
            />

            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setView("sign-in")}
                className="flex-1 py-3"
              >
                Back to Form
              </Button>
              <Button type="submit" className="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700">
                Send Link
              </Button>
            </div>
          </form>}

          {view === "otp" && <form onSubmit={handleOtpVerify} className="flex flex-col gap-6">
            <div className="text-center">
              <div className="inline-flex bg-indigo-50 p-4 rounded-full text-indigo-600 mb-3">
                <Key className="h-6 w-6 animate-pulse" />
              </div>
              <h3 className="text-2xl font-bold text-slate-800">Multi-Factor Gateway</h3>
              <p className="text-sm text-slate-400 font-medium mt-1">
                Input the 4-digit code dispatched to simulated system parameters.
              </p>
              <p className="text-xs text-indigo-600 font-bold mt-2">DEMO CODE: 4022</p>
            </div>

            <div className="flex justify-center gap-4">
              {otpCode.map((digit, index) => <input
                key={index}
                id={`otp-${index}`}
                type="text"
                maxLength={1}
                value={digit}
                onChange={(e) => {
                  const val = e.target.value;
                  const newCode = [...otpCode];
                  newCode[index] = val;
                  setOtpCode(newCode);
                  if (val && index < 3) {
                    document.getElementById(`otp-${index + 1}`)?.focus();
                  }
                }}
                className="h-14 w-12 text-center text-xl font-bold bg-slate-50 border-2 border-slate-200/80 rounded-2xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:bg-white text-slate-800 transition"
              />)}
            </div>

            <div className="flex gap-3.5">
              <Button
                type="button"
                variant="outline"
                onClick={() => setView("sign-in")}
                className="flex-1 py-3"
              >
                Cancel
              </Button>
              <Button type="submit" className="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700">
                Confirm Verification
              </Button>
            </div>
          </form>}
        </div>

        {
          /* Preset Demo Access shortcuts */
        }
        {view === "sign-in" && <div className="flex flex-col gap-4">
          <div className="flex items-center gap-3">
            <div className="h-px bg-slate-200 flex-1" />
            <span className="text-xs uppercase font-extrabold text-slate-400 tracking-widest shrink-0">INSTANT ACCESS SIMULATION PANEL</span>
            <div className="h-px bg-slate-200 flex-1" />
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            {presets.map((preset) => <button
              key={preset.role}
              type="button"
              onClick={() => handleQuickLogin(preset)}
              className="flex flex-col items-center justify-between text-center bg-white border border-slate-100 hover:border-indigo-400 hover:bg-indigo-50/10 hover:shadow-xs p-3.5 rounded-2xl transition group relative cursor-pointer"
            >
              <div className="h-8 w-8 rounded-full bg-slate-100 group-hover:bg-indigo-100 text-slate-600 group-hover:text-indigo-600 flex items-center justify-center text-[10px] font-bold mb-2.5 transition">
                {preset.role.split(" ").map((n) => n[0]).join("")}
              </div>
              <span className="text-xs font-bold text-slate-800 line-clamp-1 leading-none">{preset.role}</span>
              <span className="text-[9px] text-slate-400 mt-1 font-medium leading-none line-clamp-1 hidden md:block">{preset.name.split(" ")[0]}</span>
            </button>)}
          </div>
        </div>}

      </div>
    </div>
  </div>;
}
