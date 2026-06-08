/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getInquiries, saveInquiries, logAction } from "../storage";
import { Button, Input, Select, Dialog, useToast } from "../components/ui";
import { Plus, ChevronRight, UserCheck, Inbox, TrendingUp } from "lucide-react";
export default function ModInquiryCRM({ user }) {
  const { addToast } = useToast();
  const [inquiries, setInquiries] = useState(getInquiries());
  const [isNewOpen, setIsNewOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [studentName, setStudentName] = useState("");
  const [parentName, setParentName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [grade, setGrade] = useState("Grade 7");
  const [counselor, setCounselor] = useState("Emma Watson");
  const [notes, setNotes] = useState("");
  const stages = ["Inquiry", "Counseling", "Visit", "Document Verification", "Admission"];
  const handleCreate = (e) => {
    e.preventDefault();
    if (!studentName || !parentName || !phone) {
      addToast("Validation Failed", "Name and contact phone are required", "error");
      return;
    }
    const newInq = {
      id: `inq-${Date.now()}`,
      studentName,
      parentName,
      phone,
      email,
      grade,
      stage: "Inquiry",
      counselor,
      notes,
      createdAt: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
      followUpDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1e3).toISOString().split("T")[0]
    };
    const updated = [newInq, ...inquiries];
    setInquiries(updated);
    saveInquiries(updated);
    logAction(user.id, user.name, user.role, "Lead Inquiry Created", `Captured inquiry CRM lead for applicant student ${studentName}`);
    setStudentName("");
    setParentName("");
    setPhone("");
    setEmail("");
    setNotes("");
    setIsNewOpen(false);
    addToast("Success", "Applicant CRM lead captured successfully");
  };
  const handleMoveStage = (id, currentStage) => {
    const currentIndex = stages.indexOf(currentStage);
    if (currentIndex === stages.length - 1) return;
    const nextStage = stages[currentIndex + 1];
    const updated = inquiries.map((inq) => {
      if (inq.id === id) {
        return { ...inq, stage: nextStage };
      }
      return inq;
    });
    setInquiries(updated);
    saveInquiries(updated);
    addToast("Stage Promoted", `Lead status promoted to "${nextStage}"`);
  };
  const filterInquiries = inquiries.filter(
    (inq) => inq.studentName.toLowerCase().includes(search.toLowerCase()) || inq.parentName.toLowerCase().includes(search.toLowerCase()) || inq.grade.toLowerCase().includes(search.toLowerCase())
  );
  const totalLeads = inquiries.length;
  const admittedLeads = inquiries.filter((i) => i.stage === "Admission").length;
  const docsLeads = inquiries.filter((i) => i.stage === "Document Verification").length;
  const conversionRate = totalLeads ? Math.round((admittedLeads + docsLeads) / totalLeads * 100) : 0;
  return <div className="space-y-6">
      
      {
    /* Upper header action area */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight">Active Inquiries CRM</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Track student leads from cold-call, visits, to final registrar validation.
          </p>
        </div>
        <div className="flex gap-2.5 w-full sm:w-auto">
          <input
    type="text"
    placeholder="Search leads..."
    value={search}
    onChange={(e) => setSearch(e.target.value)}
    className="bg-slate-50 border border-slate-200 text-sm rounded-xl px-4 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-60 font-medium"
  />
          <Button onClick={() => setIsNewOpen(true)} className="flex gap-2 items-center text-xs py-2 bg-indigo-600 hover:bg-indigo-700">
            <Plus className="h-4 w-4" /> Add Lead
          </Button>
        </div>
      </div>

      {
    /* Analytics Widget banner */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-indigo-900 text-white rounded-3xl p-5 flex items-center justify-between relative overflow-hidden">
          <div className="absolute right-0 bottom-0 pr-4 opacity-10 pointer-events-none">
            <Inbox className="h-28 w-28" />
          </div>
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-200">TOTAL CRM DISPATCHED</span>
            <h4 className="text-3xl font-extrabold mt-1">{totalLeads}</h4>
            <p className="text-xs text-indigo-200/80 mt-1 font-semibold">Active active student leads</p>
          </div>
        </div>
        <div className="bg-emerald-950 text-white rounded-3xl p-5 flex items-center justify-between relative overflow-hidden">
          <div className="absolute right-0 bottom-0 pr-4 opacity-10 pointer-events-none">
            <TrendingUp className="h-28 w-28" />
          </div>
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-emerald-300">EST. CONVERSION METRIC</span>
            <h4 className="text-3xl font-extrabold mt-1">{conversionRate}%</h4>
            <p className="text-xs text-emerald-200/80 mt-1 font-semibold">{admittedLeads} fully matriculated</p>
          </div>
        </div>
        <div className="bg-white border border-slate-100 rounded-3xl p-5 flex flex-col justify-between">
          <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">COUNSELOR ASSIGNMENT RATIO</span>
          <div className="flex justify-between items-center mt-3">
            <div>
              <p className="text-sm font-bold text-slate-800">David Vance</p>
              <p className="text-xs text-slate-400 font-semibold">4 active cohorts</p>
            </div>
            <div className="h-10 w-10 rounded-full bg-slate-50 flex items-center justify-center text-xs font-bold text-slate-600 border border-slate-100">
              DV
            </div>
          </div>
        </div>
      </div>

      {
    /* Kanban Board Columns */
  }
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 overflow-x-auto pb-4">
        {stages.map((stage) => {
    const stageLeads = filterInquiries.filter((i) => i.stage === stage);
    let headBg = "border-t-slate-400";
    if (stage === "Counseling") headBg = "border-t-amber-400";
    else if (stage === "Visit") headBg = "border-t-sky-400";
    else if (stage === "Document Verification") headBg = "border-t-indigo-400";
    else if (stage === "Admission") headBg = "border-t-emerald-400";
    return <div key={stage} className={`bg-slate-50/50 border-t-4 ${headBg} rounded-2xl p-4 min-w-[240px] flex flex-col gap-3 h-[480px] overflow-y-auto border border-slate-200/50`}>
              <div className="flex justify-between items-center font-bold text-slate-700 pb-1 border-b border-slate-100 mb-1">
                <span className="text-xs text-slate-800 uppercase tracking-wider font-bold">{stage}</span>
                <span className="bg-slate-200/80 text-slate-800 text-[10px] px-2 py-0.5 rounded-full">{stageLeads.length}</span>
              </div>

              {stageLeads.length === 0 ? <div className="text-center text-slate-400 text-xs py-8 font-medium">
                  Empty stage
                </div> : stageLeads.map((lead) => <div key={lead.id} className="bg-white p-4 rounded-xl border border-slate-100 shadow-xs flex flex-col gap-2 hover:border-indigo-300 transition duration-200">
                    <div>
                      <span className="bg-slate-50 border border-slate-100 text-[10px] font-bold text-slate-600 px-2 py-0.5 rounded-md">
                        {lead.grade}
                      </span>
                    </div>
                    <h5 className="text-sm font-bold text-slate-800 leading-tight">{lead.studentName}</h5>
                    <div className="text-[11px] text-slate-400 font-medium">
                      <p>Parent: {lead.parentName}</p>
                      <p className="mt-0.5">Phone: {lead.phone}</p>
                    </div>
                    {lead.notes && <p className="text-[10px] text-slate-400 italic font-semibold line-clamp-2 bg-slate-50 p-2 rounded-lg leading-normal mt-1 border border-slate-100">
                        "{lead.notes}"
                      </p>}
                    <div className="flex justify-between items-center mt-2 pt-2 border-t border-slate-100">
                      <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-semibold">
                        <UserCheck className="h-3 w-3 text-indigo-500" />
                        {lead.counselor}
                      </div>

                      {stage !== "Admission" && <button
      onClick={() => handleMoveStage(lead.id, lead.stage)}
      className="bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white p-1 rounded-lg transition"
      title="Advance Stage"
    >
                          <ChevronRight className="h-3.5 w-3.5" />
                        </button>}
                    </div>
                  </div>)}
            </div>;
  })}
      </div>

      {
    /* New Lead Dialog Box */
  }
      <Dialog isOpen={isNewOpen} onClose={() => setIsNewOpen(false)} title="Register CRM Lead Inquiry">
        <form onSubmit={handleCreate} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Student Full Name"
    value={studentName}
    onChange={(e) => setStudentName(e.target.value)}
    placeholder="e.g. Liam Johnson"
    required
  />
            <Input
    label="Parent Full Name"
    value={parentName}
    onChange={(e) => setParentName(e.target.value)}
    placeholder="e.g. Edward Johnson"
    required
  />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Contact Mobile"
    type="tel"
    value={phone}
    onChange={(e) => setPhone(e.target.value)}
    placeholder="+1 (555) 231-9080"
    required
  />
            <Input
    label="Contact Email Address"
    type="email"
    value={email}
    onChange={(e) => setEmail(e.target.value)}
    placeholder="parent@outlook.com"
  />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
    label="Desired Grade Index"
    options={[
      { label: "Grade 6", value: "Grade 6" },
      { label: "Grade 7", value: "Grade 7" },
      { label: "Grade 8", value: "Grade 8" },
      { label: "Grade 9", value: "Grade 9" },
      { label: "Grade 10", value: "Grade 10" },
      { label: "Grade 11", value: "Grade 11" },
      { label: "Grade 12", value: "Grade 12" }
    ]}
    value={grade}
    onChange={(e) => setGrade(e.target.value)}
  />
            <Select
    label="Assigned Consultant"
    options={[
      { label: "Emma Watson", value: "Emma Watson" },
      { label: "David Vance", value: "David Vance" },
      { label: "Grover Cleveland", value: "Grover Cleveland" }
    ]}
    value={counselor}
    onChange={(e) => setCounselor(e.target.value)}
  />
          </div>
          <div>
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block mb-1.5">Lead Counsel Remarks</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 max-h-32 focus:bg-white transition"
    rows={3}
    placeholder="Specific guidelines, boarding conditions request or medical concerns details..."
    value={notes}
    onChange={(e) => setNotes(e.target.value)}
  />
          </div>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewOpen(false)}>
              Cancel Request
            </Button>
            <Button type="submit" className="bg-indigo-600 hover:bg-indigo-700">
              Save Applicant
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
