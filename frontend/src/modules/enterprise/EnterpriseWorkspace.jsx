import { useMemo, useState } from "react";
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  ClipboardList,
  FileBarChart,
  Layers3,
  Plus,
  Search,
  Settings2,
  ShieldCheck,
  Workflow
} from "lucide-react";
import { Badge, Button, Dialog, Input, Select, useToast } from "../../components/ui";
import { logAction } from "../../storage";

const toneClasses = [
  "bg-indigo-950 text-white",
  "bg-white text-slate-800 border border-slate-200",
  "bg-emerald-50 text-emerald-900 border border-emerald-200",
  "bg-amber-50 text-amber-900 border border-amber-200"
];

const starterItems = (module) => [
  {
    id: `${module.id}-seed-1`,
    subject: `${module.label} readiness review`,
    owner: "School Admin",
    priority: "High",
    due: "2026-08-05",
    statusIndex: Math.min(2, module.workflow.length - 1),
    notes: "Validate configuration, ownership and evidence before publishing."
  },
  {
    id: `${module.id}-seed-2`,
    subject: module.capabilities[0],
    owner: module.roles.find((role) => !["Super Admin", "School Admin"].includes(role)) || "Principal",
    priority: "Medium",
    due: "2026-08-12",
    statusIndex: Math.min(1, module.workflow.length - 1),
    notes: "Mock work item created to define the complete frontend scope."
  }
];

const loadItems = (module) => {
  const key = `erp_enterprise_workspace_${module.id}`;
  const stored = localStorage.getItem(key);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return starterItems(module);
    }
  }
  const items = starterItems(module);
  localStorage.setItem(key, JSON.stringify(items));
  return items;
};

export default function EnterpriseWorkspace({ module, user, context }) {
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState("operations");
  const [items, setItems] = useState(() => loadItems(module));
  const [query, setQuery] = useState("");
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [subject, setSubject] = useState("");
  const [owner, setOwner] = useState(module.roles[0] || "School Admin");
  const [priority, setPriority] = useState("Medium");
  const [due, setDue] = useState("2026-08-15");
  const [notes, setNotes] = useState("");
  const [enabledCapabilities, setEnabledCapabilities] = useState(() =>
    Object.fromEntries(module.capabilities.map((capability) => [capability, true]))
  );

  const storageKey = `erp_enterprise_workspace_${module.id}`;
  const visibleItems = useMemo(
    () =>
      items.filter((item) =>
        `${item.subject} ${item.owner} ${item.priority} ${module.workflow[item.statusIndex]}`
          .toLowerCase()
          .includes(query.toLowerCase())
      ),
    [items, module.workflow, query]
  );

  const persistItems = (next) => {
    setItems(next);
    localStorage.setItem(storageKey, JSON.stringify(next));
  };

  const advance = (item) => {
    const nextIndex = Math.min(item.statusIndex + 1, module.workflow.length - 1);
    const next = items.map((candidate) =>
      candidate.id === item.id ? { ...candidate, statusIndex: nextIndex } : candidate
    );
    persistItems(next);
    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      `${module.label} Workflow Advanced`,
      `${item.subject}: ${module.workflow[item.statusIndex]} → ${module.workflow[nextIndex]}`
    );
    addToast("Workflow Updated", `"${item.subject}" moved to ${module.workflow[nextIndex]}.`, "success");
  };

  const createItem = (event) => {
    event.preventDefault();
    if (!subject.trim()) {
      addToast("Subject Required", "Describe the record or work item before creating it.", "error");
      return;
    }
    const next = [
      {
        id: `${module.id}-${Date.now()}`,
        subject: subject.trim(),
        owner,
        priority,
        due,
        statusIndex: 0,
        notes: notes.trim()
      },
      ...items
    ];
    persistItems(next);
    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      `${module.label} Record Created`,
      subject.trim()
    );
    addToast("Record Created", `"${subject.trim()}" added to ${module.label}.`, "success");
    setSubject("");
    setNotes("");
    setIsCreateOpen(false);
  };

  const readiness = Math.round(
    (Object.values(enabledCapabilities).filter(Boolean).length / module.capabilities.length) * 100
  );

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-slate-950 text-white overflow-hidden relative">
        <div className="absolute right-0 top-0 h-64 w-64 rounded-full bg-indigo-600/20 blur-3xl" />
        <div className="relative p-7">
          <div className="flex flex-col xl:flex-row xl:items-center justify-between gap-5">
            <div className="max-w-3xl">
              <div className="flex flex-wrap gap-2 mb-3">
                <Badge className="bg-white/10 text-indigo-100 border-white/10">{module.group}</Badge>
                <Badge className="bg-emerald-500/10 text-emerald-300 border-emerald-500/20">
                  Scope mockup
                </Badge>
                <Badge className="bg-white/10 text-slate-200 border-white/10">
                  {context?.campus || "All Campuses"}
                </Badge>
              </div>
              <h2 className="text-2xl font-black tracking-tight">{module.label}</h2>
              <p className="text-sm text-slate-300 font-medium leading-relaxed mt-2">{module.summary}</p>
            </div>
            <div className="flex gap-3 shrink-0">
              <Button
                variant="outline"
                onClick={() => setActiveTab("configuration")}
                className="bg-white/5 border-white/20 text-white hover:bg-white/10"
              >
                <Settings2 className="h-4 w-4 mr-2" /> Configure
              </Button>
              <Button onClick={() => setIsCreateOpen(true)} className="bg-indigo-600 hover:bg-indigo-500">
                <Plus className="h-4 w-4 mr-2" /> New Record
              </Button>
            </div>
          </div>
        </div>
      </section>

      <section className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        {module.metrics.map(([label, value], index) => (
          <div key={label} className={`rounded-2xl p-5 ${toneClasses[index % toneClasses.length]}`}>
            <p className="text-[10px] uppercase tracking-widest font-black opacity-60">{label}</p>
            <p className="text-2xl font-black mt-1">{value}</p>
          </div>
        ))}
      </section>

      <section className="bg-white border border-slate-200 rounded-3xl overflow-hidden">
        <div className="px-6 pt-5 border-b border-slate-100 flex gap-1 overflow-x-auto">
          {[
            ["operations", "Operations", ClipboardList],
            ["workflow", "Workflow", Workflow],
            ["capabilities", "Capabilities", Layers3],
            ["reports", "Reports & Connections", FileBarChart],
            ["configuration", "Configuration", Settings2]
          ].map(([value, label, Icon]) => (
            <button
              key={value}
              onClick={() => setActiveTab(value)}
              className={`flex items-center gap-2 px-4 py-3 text-xs font-extrabold border-b-2 whitespace-nowrap ${
                activeTab === value
                  ? "border-indigo-600 text-indigo-700"
                  : "border-transparent text-slate-400 hover:text-slate-700"
              }`}
            >
              <Icon className="h-4 w-4" /> {label}
            </button>
          ))}
        </div>

        {activeTab === "operations" && (
          <div className="p-6 space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Active work queue</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Interactive mock records persist locally for product-scope review.
                </p>
              </div>
              <div className="relative md:w-80">
                <Search className="absolute h-4 w-4 left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search the work queue..."
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-3 text-xs font-semibold outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-[9px] uppercase tracking-widest text-slate-400">
                  <tr>
                    <th className="p-4">Record</th>
                    <th className="p-4">Owner</th>
                    <th className="p-4">Priority</th>
                    <th className="p-4">Due</th>
                    <th className="p-4">State</th>
                    <th className="p-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {visibleItems.map((item) => {
                    const complete = item.statusIndex === module.workflow.length - 1;
                    return (
                      <tr key={item.id} className="hover:bg-slate-50/60">
                        <td className="p-4">
                          <p className="font-extrabold text-slate-900">{item.subject}</p>
                          <p className="text-[10px] text-slate-400 mt-1 max-w-xl">{item.notes || "No note added."}</p>
                        </td>
                        <td className="p-4 font-semibold text-slate-600">{item.owner}</td>
                        <td className="p-4">
                          <Badge variant={item.priority === "High" ? "danger" : item.priority === "Low" ? "default" : "warning"}>
                            {item.priority}
                          </Badge>
                        </td>
                        <td className="p-4 font-semibold text-slate-500">{item.due}</td>
                        <td className="p-4">
                          <Badge variant={complete ? "success" : "secondary"}>
                            {module.workflow[item.statusIndex]}
                          </Badge>
                        </td>
                        <td className="p-4 text-right">
                          <Button
                            size="sm"
                            variant={complete ? "ghost" : "outline"}
                            disabled={complete}
                            onClick={() => advance(item)}
                          >
                            {complete ? "Complete" : "Advance"} {!complete && <ArrowRight className="h-3.5 w-3.5 ml-1" />}
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === "workflow" && (
          <div className="p-6">
            <div className="grid grid-cols-1 lg:grid-cols-5 gap-3">
              {module.workflow.map((stage, index) => (
                <div key={stage} className="relative rounded-2xl border border-slate-200 bg-slate-50 p-4 min-h-32">
                  <span className="h-7 w-7 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px] font-black">
                    {index + 1}
                  </span>
                  <h4 className="font-black text-sm text-slate-900 mt-3">{stage}</h4>
                  <p className="text-[10px] leading-relaxed text-slate-500 mt-1">
                    Ownership, validation, evidence and SLA rules are configured for this transition.
                  </p>
                  {index < module.workflow.length - 1 && (
                    <ArrowRight className="hidden lg:block absolute -right-3 top-1/2 h-5 w-5 text-slate-300 z-10" />
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === "capabilities" && (
          <div className="p-6 space-y-5">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Functional coverage</h3>
                <p className="text-xs text-slate-400 mt-1">Toggle capabilities to test a tenant-specific module pack.</p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-black text-indigo-700">{readiness}%</p>
                <p className="text-[9px] uppercase tracking-widest font-black text-slate-400">Enabled</p>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {module.capabilities.map((capability) => {
                const enabled = enabledCapabilities[capability];
                return (
                  <button
                    key={capability}
                    onClick={() =>
                      setEnabledCapabilities((current) => ({ ...current, [capability]: !current[capability] }))
                    }
                    className={`p-4 rounded-2xl border text-left flex items-start gap-3 transition ${
                      enabled ? "border-indigo-200 bg-indigo-50/50" : "border-slate-200 bg-slate-50 opacity-60"
                    }`}
                  >
                    <CheckCircle2 className={`h-5 w-5 shrink-0 mt-0.5 ${enabled ? "text-indigo-600" : "text-slate-300"}`} />
                    <span className="text-xs font-bold text-slate-800 leading-relaxed">{capability}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {activeTab === "reports" && (
          <div className="p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div>
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest mb-3">Reports and dashboards</h3>
              <div className="space-y-3">
                {module.reports.map((report) => (
                  <div key={report} className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex gap-3">
                    <FileBarChart className="h-5 w-5 text-indigo-600 shrink-0" />
                    <div>
                      <p className="text-xs font-black text-slate-900">{report}</p>
                      <p className="text-[10px] text-slate-400 mt-1">Permission-aware, schedulable and export-audited.</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest mb-3">Integration touchpoints</h3>
              <div className="flex flex-wrap gap-2">
                {(module.integrations.length ? module.integrations : ["Workflow engine", "Notification hub", "Reporting"]).map(
                  (connection) => (
                    <Badge key={connection} variant="secondary" className="px-3 py-2">
                      {connection}
                    </Badge>
                  )
                )}
              </div>
              <div className="mt-6 rounded-2xl bg-emerald-50 border border-emerald-200 p-5">
                <ShieldCheck className="h-6 w-6 text-emerald-700" />
                <h4 className="font-black text-sm text-emerald-950 mt-3">Shared platform controls</h4>
                <p className="text-xs text-emerald-800 mt-1 leading-relaxed">
                  Tenant scope, audit, consent, retention, approvals, notification preferences and external identifiers
                  apply to every record in this workspace.
                </p>
              </div>
            </div>
          </div>
        )}

        {activeTab === "configuration" && (
          <div className="p-6 grid grid-cols-1 lg:grid-cols-3 gap-5">
            {[
              ["Module entitlement", "Enterprise", "Plan-controlled feature access and tenant overrides."],
              ["Data scope", context?.campus || "All Campuses", "Campus, academic-year and programme boundaries."],
              ["Default workflow", module.workflow.join(" → "), "Versioned stages, approvals, timers and escalation."]
            ].map(([title, value, description]) => (
              <div key={title} className="rounded-2xl border border-slate-200 p-5">
                <p className="text-[9px] uppercase tracking-widest font-black text-slate-400">{title}</p>
                <p className="font-black text-sm text-slate-900 mt-2">{value}</p>
                <p className="text-xs text-slate-500 leading-relaxed mt-2">{description}</p>
              </div>
            ))}
            <div className="lg:col-span-3 rounded-2xl bg-slate-950 text-white p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex gap-3">
                <Activity className="h-5 w-5 text-indigo-400 shrink-0" />
                <div>
                  <p className="text-sm font-black">Configuration preview only</p>
                  <p className="text-xs text-slate-400 mt-1">
                    The backend will persist effective-dated, tenant-scoped versions after database design is approved.
                  </p>
                </div>
              </div>
              <Button
                onClick={() => addToast("Readiness Check Complete", `${module.label} configuration is ${readiness}% enabled.`, "success")}
                className="bg-indigo-600 hover:bg-indigo-500"
              >
                Run Readiness Check
              </Button>
            </div>
          </div>
        )}
      </section>

      <Dialog isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title={`Create ${module.label} Record`}>
        <form onSubmit={createItem} className="space-y-4">
          <Input
            label="Record / Work Item"
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
            placeholder={`Describe a ${module.label.toLowerCase()} task...`}
            required
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Select
              label="Owner Role"
              value={owner}
              onChange={(event) => setOwner(event.target.value)}
              options={module.roles.map((role) => ({ label: role, value: role }))}
            />
            <Select
              label="Priority"
              value={priority}
              onChange={(event) => setPriority(event.target.value)}
              options={["Low", "Medium", "High"].map((value) => ({ label: value, value }))}
            />
          </div>
          <Input label="Due Date" type="date" value={due} onChange={(event) => setDue(event.target.value)} />
          <div>
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block mb-1.5">Notes</label>
            <textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              rows={3}
              className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Evidence, dependencies, acceptance criteria or context..."
            />
          </div>
          <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsCreateOpen(false)}>Cancel</Button>
            <Button type="submit">Create Record</Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}

