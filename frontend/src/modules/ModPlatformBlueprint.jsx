import { useMemo, useState } from "react";
import {
  BarChart3,
  Bot,
  Boxes,
  Building2,
  Check,
  ChevronRight,
  Database,
  KeyRound,
  Layers3,
  LockKeyhole,
  Plug,
  Search,
  ShieldCheck,
  Sparkles,
  Workflow
} from "lucide-react";
import { Badge, Button, useToast } from "../components/ui";
import { logAction } from "../storage";
import {
  aiRoadmapCatalog,
  dataDomainCatalog,
  enterpriseModules,
  integrationCatalog,
  reportCatalog,
  roleCatalog,
  securityControlCatalog,
  workflowCatalog
} from "./enterprise/enterpriseCatalog";

const sections = [
  ["overview", "Scope Overview", Layers3],
  ["roles", "Roles & Access", KeyRound],
  ["workflows", "Workflow Registry", Workflow],
  ["reports", "Report Catalogue", BarChart3],
  ["integrations", "Integrations", Plug],
  ["security", "Privacy & Security", ShieldCheck],
  ["ai", "AI Roadmap", Bot],
  ["data", "SaaS & Data Model", Database]
];

const loadSet = (key, defaults = []) => {
  try {
    return new Set(JSON.parse(localStorage.getItem(key) || JSON.stringify(defaults)));
  } catch {
    return new Set(defaults);
  }
};

const saveSet = (key, value) => localStorage.setItem(key, JSON.stringify([...value]));

export default function ModPlatformBlueprint({ user, context }) {
  const { addToast } = useToast();
  const [activeSection, setActiveSection] = useState("overview");
  const [query, setQuery] = useState("");
  const [activeWorkflows, setActiveWorkflows] = useState(() =>
    loadSet("erp_blueprint_workflows", workflowCatalog.slice(0, 6).map(([name]) => name))
  );
  const [connectedIntegrations, setConnectedIntegrations] = useState(() =>
    loadSet("erp_blueprint_integrations", ["Identity", "Communication"])
  );
  const [verifiedControls, setVerifiedControls] = useState(() =>
    loadSet("erp_blueprint_security", securityControlCatalog.slice(0, 3))
  );
  const [expandedRoleGroup, setExpandedRoleGroup] = useState("Academic leadership");

  const filteredModules = useMemo(
    () =>
      enterpriseModules.filter((module) =>
        `${module.label} ${module.group} ${module.summary} ${module.capabilities.join(" ")}`
          .toLowerCase()
          .includes(query.toLowerCase())
      ),
    [query]
  );

  const toggleSetItem = (item, current, setter, storageKey, actionLabel) => {
    const next = new Set(current);
    if (next.has(item)) next.delete(item);
    else next.add(item);
    setter(next);
    saveSet(storageKey, next);
    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      actionLabel,
      `${item}: ${next.has(item) ? "enabled" : "disabled"}`
    );
  };

  const roleCount = roleCatalog.reduce((sum, [, roles]) => sum + roles.length, 0);
  const capabilityCount = enterpriseModules.reduce((sum, module) => sum + module.capabilities.length, 0);
  const securityPct = Math.round((verifiedControls.size / securityControlCatalog.length) * 100);

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-white overflow-hidden">
        <div className="bg-slate-950 text-white p-7 relative overflow-hidden">
          <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-violet-600/30 blur-3xl" />
          <div className="relative flex flex-col xl:flex-row xl:items-end justify-between gap-6">
            <div className="max-w-3xl">
              <div className="flex gap-2 flex-wrap mb-3">
                <Badge className="bg-violet-500/10 text-violet-200 border-violet-400/20">ERP Scope Studio</Badge>
                <Badge className="bg-white/10 text-slate-200 border-white/10">{context?.academicYear || "2026–27"}</Badge>
              </div>
              <h2 className="text-2xl font-black tracking-tight">Platform Blueprint & Control Centre</h2>
              <p className="text-sm text-slate-300 leading-relaxed mt-2">
                A frontend specification for the shared roles, workflows, reports, integrations, controls, AI governance
                and data domains required by every School ERP module.
              </p>
            </div>
            <Button
              onClick={() => {
                addToast("Blueprint Snapshot Saved", "The current scope configuration was captured for review.", "success");
                logAction(user?.id, user?.name, user?.role, "Platform Blueprint Snapshot", "Captured frontend scope configuration");
              }}
              className="bg-violet-600 hover:bg-violet-500"
            >
              <Sparkles className="h-4 w-4 mr-2" /> Save Scope Snapshot
            </Button>
          </div>
        </div>

        <div className="flex gap-1 px-5 pt-4 overflow-x-auto border-b border-slate-100">
          {sections.map(([value, label, Icon]) => (
            <button
              key={value}
              onClick={() => setActiveSection(value)}
              className={`px-4 py-3 border-b-2 text-xs font-extrabold whitespace-nowrap flex items-center gap-2 ${
                activeSection === value
                  ? "border-violet-600 text-violet-700"
                  : "border-transparent text-slate-400 hover:text-slate-700"
              }`}
            >
              <Icon className="h-4 w-4" /> {label}
            </button>
          ))}
        </div>

        {activeSection === "overview" && (
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
              {[
                ["New workspaces", enterpriseModules.length, Building2, "bg-violet-950 text-white"],
                ["Defined capabilities", capabilityCount, Boxes, "bg-white border border-slate-200 text-slate-900"],
                ["Role personas", roleCount, KeyRound, "bg-indigo-50 border border-indigo-200 text-indigo-950"],
                ["Shared workflows", workflowCatalog.length, Workflow, "bg-emerald-50 border border-emerald-200 text-emerald-950"]
              ].map(([label, value, Icon, className]) => (
                <div key={label} className={`rounded-2xl p-5 ${className}`}>
                  <Icon className="h-5 w-5 opacity-70" />
                  <p className="text-2xl font-black mt-3">{value}</p>
                  <p className="text-[10px] uppercase tracking-widest font-black opacity-60 mt-1">{label}</p>
                </div>
              ))}
            </div>

            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Expansion workspaces</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Every workspace includes capabilities, a state machine, reports, integrations and mock operations.
                </p>
              </div>
              <div className="relative md:w-96">
                <Search className="absolute h-4 w-4 left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search modules or capabilities..."
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-3 text-xs font-semibold outline-none focus:ring-2 focus:ring-violet-500"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {filteredModules.map((module) => (
                <div key={module.id} className="rounded-2xl border border-slate-200 p-5 hover:border-violet-300 hover:shadow-sm transition">
                  <div className="flex items-start justify-between gap-3">
                    <Badge variant="secondary">{module.group}</Badge>
                    <span className="text-[10px] font-black text-slate-400">{module.capabilities.length} capabilities</span>
                  </div>
                  <h4 className="font-black text-sm text-slate-900 mt-3">{module.label}</h4>
                  <p className="text-xs text-slate-500 leading-relaxed mt-2 line-clamp-3">{module.summary}</p>
                  <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                    <span className="text-[10px] uppercase tracking-widest font-black text-emerald-600">Added to navigation</span>
                    <Check className="h-4 w-4 text-emerald-600" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeSection === "roles" && (
          <div className="p-6 grid grid-cols-1 xl:grid-cols-3 gap-6">
            <div className="xl:col-span-2 space-y-3">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Role persona catalogue</h3>
                <p className="text-xs text-slate-400 mt-1">Roles become assignments with tenant, campus and data scopes.</p>
              </div>
              {roleCatalog.map(([group, roles]) => {
                const expanded = expandedRoleGroup === group;
                return (
                  <div key={group} className="rounded-2xl border border-slate-200 overflow-hidden">
                    <button
                      onClick={() => setExpandedRoleGroup(expanded ? "" : group)}
                      className="w-full p-4 flex items-center justify-between bg-slate-50 text-left"
                    >
                      <div>
                        <p className="font-black text-sm text-slate-900">{group}</p>
                        <p className="text-[10px] text-slate-400 mt-1">{roles.length} scoped personas</p>
                      </div>
                      <ChevronRight className={`h-4 w-4 text-slate-400 transition ${expanded ? "rotate-90" : ""}`} />
                    </button>
                    {expanded && (
                      <div className="p-4 flex flex-wrap gap-2">
                        {roles.map((role) => <Badge key={role} variant="secondary" className="py-2 px-3">{role}</Badge>)}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
            <div className="space-y-4">
              <div className="rounded-2xl bg-slate-950 text-white p-5">
                <LockKeyhole className="h-6 w-6 text-violet-400" />
                <h4 className="font-black text-sm mt-3">Authorization dimensions</h4>
                <div className="mt-4 space-y-2">
                  {["Tenant", "Campus", "Academic year", "Class & section", "Subject", "Child", "Dormitory", "Route", "Sensitive fields"].map(
                    (scope) => (
                      <div key={scope} className="flex items-center gap-2 text-xs text-slate-300">
                        <Check className="h-3.5 w-3.5 text-emerald-400" /> {scope}
                      </div>
                    )
                  )}
                </div>
              </div>
              <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
                <p className="font-black text-sm text-amber-950">Production rule</p>
                <p className="text-xs text-amber-800 leading-relaxed mt-2">
                  The frontend may hide actions, but the API must independently enforce every permission and scope.
                </p>
              </div>
            </div>
          </div>
        )}

        {activeSection === "workflows" && (
          <div className="p-6 space-y-4">
            <div>
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Shared state-machine registry</h3>
              <p className="text-xs text-slate-400 mt-1">Enable the workflows that the tenant will configure before go-live.</p>
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {workflowCatalog.map(([name, flow]) => {
                const active = activeWorkflows.has(name);
                return (
                  <button
                    key={name}
                    onClick={() =>
                      toggleSetItem(name, activeWorkflows, setActiveWorkflows, "erp_blueprint_workflows", "Workflow Entitlement Changed")
                    }
                    className={`rounded-2xl border p-5 text-left transition ${
                      active ? "border-violet-300 bg-violet-50/50" : "border-slate-200 bg-slate-50 opacity-60"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-black text-sm text-slate-900">{name}</p>
                        <p className="text-xs text-slate-500 leading-relaxed mt-2">{flow}</p>
                      </div>
                      <span className={`h-6 w-10 rounded-full p-1 ${active ? "bg-violet-600" : "bg-slate-300"}`}>
                        <span className={`block h-4 w-4 bg-white rounded-full transition ${active ? "translate-x-4" : ""}`} />
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {activeSection === "reports" && (
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-4">
            {reportCatalog.map(([family, coverage], index) => (
              <div key={family} className="rounded-2xl border border-slate-200 p-5 flex gap-4">
                <span className="h-10 w-10 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center font-black text-sm shrink-0">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <div>
                  <h4 className="font-black text-sm text-slate-900">{family}</h4>
                  <p className="text-xs text-slate-500 leading-relaxed mt-1">{coverage}</p>
                  <div className="flex gap-2 mt-3">
                    <Badge variant="secondary">Permission-aware</Badge>
                    <Badge>Schedulable</Badge>
                    <Badge>Audited</Badge>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeSection === "integrations" && (
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-4">
            {integrationCatalog.map(([family, providers]) => {
              const connected = connectedIntegrations.has(family);
              return (
                <div key={family} className="rounded-2xl border border-slate-200 p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex gap-3">
                      <span className={`h-10 w-10 rounded-xl flex items-center justify-center ${connected ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-400"}`}>
                        <Plug className="h-5 w-5" />
                      </span>
                      <div>
                        <h4 className="font-black text-sm text-slate-900">{family}</h4>
                        <p className="text-xs text-slate-500 leading-relaxed mt-1">{providers}</p>
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant={connected ? "outline" : "secondary"}
                      onClick={() =>
                        toggleSetItem(
                          family,
                          connectedIntegrations,
                          setConnectedIntegrations,
                          "erp_blueprint_integrations",
                          "Integration Family Changed"
                        )
                      }
                    >
                      {connected ? "Connected" : "Connect"}
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {activeSection === "security" && (
          <div className="p-6 grid grid-cols-1 xl:grid-cols-3 gap-6">
            <div className="xl:col-span-2 space-y-3">
              {securityControlCatalog.map((control) => {
                const verified = verifiedControls.has(control);
                return (
                  <button
                    key={control}
                    onClick={() =>
                      toggleSetItem(control, verifiedControls, setVerifiedControls, "erp_blueprint_security", "Security Control Reviewed")
                    }
                    className={`w-full rounded-2xl border p-4 text-left flex gap-3 ${
                      verified ? "border-emerald-200 bg-emerald-50/60" : "border-slate-200"
                    }`}
                  >
                    <span className={`h-6 w-6 rounded-lg flex items-center justify-center shrink-0 ${verified ? "bg-emerald-600 text-white" : "bg-slate-100 text-slate-300"}`}>
                      <Check className="h-4 w-4" />
                    </span>
                    <span className="text-xs font-bold text-slate-800 leading-relaxed">{control}</span>
                  </button>
                );
              })}
            </div>
            <div className="space-y-4">
              <div className="rounded-3xl bg-emerald-950 text-white p-6">
                <p className="text-4xl font-black text-emerald-300">{securityPct}%</p>
                <p className="text-[10px] uppercase tracking-widest font-black text-emerald-200 mt-2">Control review readiness</p>
                <div className="h-2 rounded-full bg-white/10 mt-5 overflow-hidden">
                  <div className="h-full bg-emerald-400 rounded-full" style={{ width: `${securityPct}%` }} />
                </div>
              </div>
              <div className="rounded-2xl border border-slate-200 p-5">
                <p className="font-black text-sm text-slate-900">Compliance packs</p>
                <div className="flex flex-wrap gap-2 mt-3">
                  {["DPDP", "CBSE/UDISE", "RTE", "POCSO", "GDPR", "FERPA", "COPPA", "WCAG 2.2"].map((pack) => (
                    <Badge key={pack} variant="secondary">{pack}</Badge>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {activeSection === "ai" && (
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {aiRoadmapCatalog.map(([family, coverage]) => (
                <div key={family} className="rounded-2xl border border-slate-200 p-5">
                  <div className="flex gap-3">
                    <span className="h-10 w-10 rounded-xl bg-violet-50 text-violet-700 flex items-center justify-center shrink-0">
                      <Bot className="h-5 w-5" />
                    </span>
                    <div>
                      <h4 className="font-black text-sm text-slate-900">{family}</h4>
                      <p className="text-xs text-slate-500 leading-relaxed mt-1">{coverage}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <div className="rounded-3xl bg-slate-950 text-white p-6">
              <h3 className="font-black text-sm uppercase tracking-widest">Consequential-decision guardrails</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3 mt-4">
                {["No autonomous admissions", "No autonomous discipline", "No autonomous health diagnosis", "No emotion recognition"].map(
                  (guardrail) => (
                    <div key={guardrail} className="rounded-xl border border-white/10 bg-white/5 p-4 flex gap-2 text-xs font-bold text-slate-200">
                      <ShieldCheck className="h-4 w-4 text-violet-400 shrink-0" /> {guardrail}
                    </div>
                  )
                )}
              </div>
            </div>
          </div>
        )}

        {activeSection === "data" && (
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {dataDomainCatalog.map(([domain, entities]) => (
                <div key={domain} className="rounded-2xl border border-slate-200 p-5">
                  <Database className="h-5 w-5 text-indigo-600" />
                  <h4 className="font-black text-sm text-slate-900 mt-3">{domain}</h4>
                  <p className="text-xs text-slate-500 leading-relaxed mt-2">{entities}</p>
                </div>
              ))}
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
              {[
                ["Control plane", "Plans, subscriptions, entitlements, regions, provisioning, metering and support access."],
                ["School data plane", "Tenant-isolated operational domains with effective-dated configuration and row-level security."],
                ["Analytics plane", "Event-driven warehouse, semantic metrics, snapshots, AI evaluation and permission-aware reporting."]
              ].map(([title, detail]) => (
                <div key={title} className="rounded-2xl bg-slate-950 text-white p-5">
                  <p className="text-xs uppercase tracking-widest font-black text-violet-300">{title}</p>
                  <p className="text-xs text-slate-300 leading-relaxed mt-3">{detail}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

