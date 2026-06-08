/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getAuditLogs, getSchools, saveSchools, logAction } from "../storage";
import { Button, Input, Select, Badge, Dialog, useToast } from "../components/ui";
import { Settings, Shield, Plus, ListCollapse, ToggleLeft, ToggleRight, Sparkles, RefreshCw } from "lucide-react";
export default function ModSuperAdmin({ user }) {
  const { addToast } = useToast();
  const [schools, setSchools] = useState(() => getSchools());
  const [logs, setLogs] = useState(() => getAuditLogs());
  const [isAddBranchOpen, setIsAddBranchOpen] = useState(false);
  const [newSchoolName, setNewSchoolName] = useState("");
  const [newDomain, setNewDomain] = useState("");
  const [newSubscription, setNewSubscription] = useState("Enterprise Plus");
  const [toggles, setToggles] = useState({
    walletEnabled: true,
    aiCopilotEnabled: true,
    autoAttendanceSms: false,
    lockdownEmergency: false
  });
  const handleToggleFeature = (key, label) => {
    const nextVal = !toggles[key];
    setToggles((prev) => ({ ...prev, [key]: nextVal }));
    logAction(
      user.id,
      user.name,
      user.role,
      "System Settings Changed",
      `SuperAdmin toggled institutional feature "${label}" to state: ${nextVal ? "ENABLED" : "DISABLED"}`
    );
    addToast("Config Saved", `Successfully ${nextVal ? "Enabled" : "Disabled"} system feature "${label}"`);
  };
  const handleCreateBranch = (e) => {
    e.preventDefault();
    if (!newSchoolName || !newDomain) {
      addToast("Error", "School Name and Subdomain are required", "error");
      return;
    }
    const newTenant = {
      id: `school-tenant-${Date.now()}`,
      name: newSchoolName,
      domain: `${newDomain}.judeschools.com`,
      subdomain: `${newDomain}.judeschools.com`,
      plan: "Enterprise",
      subscriptionPlan: newSubscription,
      status: "Active",
      createdDate: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
      registeredDate: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
      contactEmail: `admin@${newDomain}.judeschools.com`
    };
    const updated = [...schools, newTenant];
    setSchools(updated);
    saveSchools(updated);
    logAction(user.id, user.name, user.role, "School Tenant Created", `SuperAdmin launched school branch: ${newSchoolName}`);
    addToast("Success", `New school tenant "${newSchoolName}" provisioned successfully!`, "success");
    setNewSchoolName("");
    setNewDomain("");
    setIsAddBranchOpen(false);
  };
  const handleClearAuditLogs = () => {
    localStorage.removeItem("erp_audit_logs");
    setLogs([]);
    addToast("Audit Purged", "Temporary sandbox session analytics logs cleared", "info");
  };
  const handleRefreshRecords = () => {
    setLogs(getAuditLogs());
    setSchools(getSchools());
    addToast("Telemetry Synced", "Rerendered system telemetry caches", "success");
  };
  return <div className="space-y-6">

      {
    /* Stats summaries cards */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">ACTIVE CAMPUS BRANCHES</span>
            <h4 className="text-xl font-black text-slate-800 mt-1">{schools.length} Campuses</h4>
            <p className="text-xs text-slate-450 mt-1">St Jude Sandbox Federation</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-605 rounded-2xl">
            <Plus className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 text-indigo-150 p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-305">ROLE RIGHTS RESTRICTIONS</span>
            <h4 className="text-xl font-black text-white mt-1">Super Admin Core</h4>
            <p className="text-xs text-indigo-200 mt-1">Authorized core system credentials</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <Shield className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">TELEMETRY AUDITED EVENTS</span>
            <h4 className="text-xl font-black text-rose-600 mt-1">{logs.length} Live Operations</h4>
            <p className="text-xs text-slate-450 mt-1">Monitored chronologically</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl">
            <ListCollapse className="h-6 w-6 animate-pulse" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {
    /* Left column: Schools tenants & Features */
  }
        <div className="lg:col-span-2 space-y-6">

          {
    /* SaaS Toggles */
  }
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-sm font-extrabold text-slate-805 uppercase tracking-widest flex items-center gap-1.5">
              <Sparkles className="h-4.5 w-4.5 text-indigo-505" /> Institutional SaaS Feature Toggles
            </h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Instantly enable or disable premium SaaS logic gates across the database models.</p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1.5 font-bold">
              
              {
    /* Toggle 1 */
  }
              <div
    onClick={() => handleToggleFeature("walletEnabled", "RFID Pocket Purse")}
    className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between cursor-pointer select-none transition hover:border-slate-300"
  >
                <div>
                  <h4 className="text-xs font-black text-slate-800">RFID Pocket Wallet System</h4>
                  <p className="text-[10px] text-slate-400 font-semibold mt-0.5">Allows store buy debits and parent topups</p>
                </div>
                {toggles.walletEnabled ? <ToggleRight className="h-8 w-8 text-indigo-600" /> : <ToggleLeft className="h-8 w-8 text-slate-400" />}
              </div>

              {
    /* Toggle 2 */
  }
              <div
    onClick={() => handleToggleFeature("aiCopilotEnabled", "AI Assistant")}
    className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between cursor-pointer select-none transition hover:border-slate-300"
  >
                <div>
                  <h4 className="text-xs font-black text-slate-800">Gemini AI Copilot Module</h4>
                  <p className="text-[10px] text-slate-400 font-semibold mt-0.5">Enables simulated CRM grading models</p>
                </div>
                {toggles.aiCopilotEnabled ? <ToggleRight className="h-8 w-8 text-indigo-600" /> : <ToggleLeft className="h-8 w-8 text-slate-400" />}
              </div>

              {
    /* Toggle 3 */
  }
              <div
    onClick={() => handleToggleFeature("autoAttendanceSms", "SMS parent alerts")}
    className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between cursor-pointer select-none transition hover:border-slate-300"
  >
                <div>
                  <h4 className="text-xs font-black text-slate-800">Automated SMS Curfew Alerts</h4>
                  <p className="text-[10px] text-slate-400 font-semibold mt-0.5">Broadcast absent status directly to guardians</p>
                </div>
                {toggles.autoAttendanceSms ? <ToggleRight className="h-8 w-8 text-indigo-600" /> : <ToggleLeft className="h-8 w-8 text-slate-400" />}
              </div>

              {
    /* Toggle 4 */
  }
              <div
    onClick={() => handleToggleFeature("lockdownEmergency", "Overridden Safety loop")}
    className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-between cursor-pointer select-none transition hover:border-slate-300"
  >
                <div>
                  <h4 className="text-xs font-black text-slate-800">Overridden Global Lockdown</h4>
                  <p className="text-[10px] text-slate-400 font-semibold mt-0.5">Locks all gates (synchronized with Security module)</p>
                </div>
                {toggles.lockdownEmergency ? <ToggleRight className="h-8 w-8 text-indigo-600" /> : <ToggleLeft className="h-8 w-8 text-slate-400" />}
              </div>

            </div>
          </div>

          {
    /* Tenants listing */
  }
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex justify-between items-center leading-none">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Multi-Tenant Branch directory</h3>
              <button
    onClick={() => setIsAddBranchOpen(true)}
    className="bg-slate-900 text-white font-bold text-[10px] p-2 px-3.5 rounded-xl uppercase transition cursor-pointer"
  >
                Provision New Branch
              </button>
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">School Name</th>
                    <th className="p-3">Access Domain</th>
                    <th className="p-3">Billing System Quota</th>
                    <th className="p-3">Filing Date</th>
                    <th className="p-3">Tenant Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 font-semibold">
                  {schools.map((sch) => <tr key={sch.id}>
                      <td className="p-3 font-extrabold text-slate-850">{sch.name}</td>
                      <td className="p-3 font-mono text-[10px] text-indigo-750">{sch.subdomain}</td>
                      <td className="p-3">{sch.subscriptionPlan}</td>
                      <td className="p-3 text-slate-400">{sch.registeredDate}</td>
                      <td className="p-3">
                        <Badge variant="success">{sch.status}</Badge>
                      </td>
                    </tr>)}
                </tbody>
              </table>
            </div>
          </div>

        </div>

        {
    /* Right column: System Audit Logs list */
  }
        <div className="bg-white border border-slate-1.5 p-6 rounded-3xl flex flex-col justify-between h-[480px]">
          <div className="space-y-4">
            <div className="flex justify-between items-center leading-none">
              <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex items-center gap-1.5">
                <Settings className="h-4.5 w-4.5 text-indigo-505" /> System Audit Trail
              </h3>
              <div className="flex gap-1.5">
                <button
    type="button"
    onClick={handleRefreshRecords}
    className="p-1 text-slate-405 hover:text-slate-905"
    title="Force recalculations cache"
  >
                  <RefreshCw className="h-4.5 w-4.5" />
                </button>
                <button
    onClick={handleClearAuditLogs}
    className="uppercase text-[9px] font-black text-rose-500 bg-rose-50 p-1 px-2.5 rounded-lg hover:bg-rose-600 hover:text-white cursor-pointer"
  >
                  Purge Logs
                </button>
              </div>
            </div>
            <p className="text-xs text-slate-440 font-semibold leading-normal">
              Chronological security list records captured by browser actions:
            </p>
          </div>

          <div className="flex-1 overflow-y-auto mt-4 p-4 border border-slate-155 bg-slate-900 rounded-2xl font-mono text-[11px] text-indigo-305 space-y-3 scrollbar-thin scrollbar-thumb-slate-800">
            {logs.length === 0 ? <p className="text-slate-500 italic text-center pt-8">No current activity on record board</p> : logs.slice(0, 40).map((lg) => <div key={lg.id} className="border-b border-slate-850 pb-2 leading-relaxed">
                  <div className="flex justify-between font-bold text-slate-500">
                    <span>{lg.timestamp}</span>
                    <span className="text-rose-455 font-black">[{lg.userRole}]</span>
                  </div>
                  <p className="text-slate-300 font-extrabold mt-0.5">{lg.userName}: <span className="text-emerald-400">{lg.actionType}</span></p>
                  <p className="text-xs text-slate-450 mt-1 italic leading-relaxed">"{lg.details}"</p>
                </div>)}
          </div>
        </div>

      </div>

      {
    /* Provision School Branch Popover dialog */
  }
      <Dialog isOpen={isAddBranchOpen} onClose={() => setIsAddBranchOpen(false)} title="Provision New school Branch Tenant">
        <form onSubmit={handleCreateBranch} className="space-y-4 pt-1">
          <Input
    label="School Campus Name"
    value={newSchoolName}
    onChange={(e) => setNewSchoolName(e.target.value)}
    placeholder="e.g. St Jude Secondary West Campus"
    required
  />

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Tenant Subdomain Code</label>
              <div className="flex items-center bg-slate-50 border border-slate-205 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-indigo-505 transition pr-3">
                <input
    type="text"
    className="w-full bg-transparent p-3 text-sm focus:outline-none text-slate-805"
    value={newDomain}
    onChange={(e) => setNewDomain(e.target.value)}
    placeholder="west-campus"
    required
  />
                <span className="text-xs font-semibold text-slate-400">.judeschools.com</span>
              </div>
            </div>

            <Select
    label="Subscription billing tier Plan"
    options={[
      { label: "Standard Lite Quotas", value: "Standard Lite" },
      { label: "SaaS Enterprise Plus unlimited", value: "Enterprise Plus" }
    ]}
    value={newSubscription}
    onChange={(e) => setNewSubscription(e.target.value)}
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddBranchOpen(false)}>Cancel Provision</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900 border border-transparent font-extrabold text-white">
              Launch School Branch
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
