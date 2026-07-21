/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import { getStaff, saveStaff, logAction } from "../storage";
import { api } from "../api";
import { Button, Input, Select, Badge, Dialog, useToast } from "../components/ui";
import { Briefcase, CalendarCheck, Star, Users, Landmark } from "lucide-react";
export default function ModStaffHRMS({ user }) {
  const { addToast } = useToast();
  const [staff, setStaff] = useState(() => getStaff());
  const [selectedStaff, setSelectedStaff] = useState(null);
  const [salaryVal, setSalaryVal] = useState("5000");
  const [ratingVal, setRatingVal] = useState("4.5");
  const [leaves, setLeaves] = useState([
    { id: "leave-1", name: "Prof. Chloe Smith", reason: "Attending acute clinical root canal surgery.", days: 3, status: "Pending Review" },
    { id: "leave-2", name: "Prof. Rohan Sen", reason: "Academic research forum presentations on CS.", days: 2, status: "Pending Review" }
  ]);

  useEffect(() => {
    let isMounted = true;
    api.getStaff().then((res) => {
      if (isMounted && Array.isArray(res) && res.length > 0) {
        setStaff(res);
      }
    }).catch(() => {});
    return () => { isMounted = false; };
  }, []);

  const handleUpdateStaff = (e) => {
    e.preventDefault();
    if (!selectedStaff) return;
    const salaryNum = parseFloat(salaryVal) || selectedStaff.salary;
    const ratingNum = parseFloat(ratingVal) || selectedStaff.reviewRating;
    const updated = staff.map((sf) => {
      if (sf.id === selectedStaff.id) {
        return {
          ...sf,
          salary: salaryNum,
          reviewRating: ratingNum
        };
      }
      return sf;
    });
    setStaff(updated);
    saveStaff(updated);
    api.updateStaff(selectedStaff.id, { salary: salaryNum, reviewRating: ratingNum }).catch(() => {});
    setSelectedStaff(null);
    logAction(user.id, user.name, user.role, "Staff File Updated", `Adjusted payroll salary for ${selectedStaff.name} to $${salaryVal}. Set score rating: ${ratingVal}`);
    addToast("Success", "Staff file updated inside LocalStorage and Backend REST API");
  };
  const handleTriggerEdit = (sf) => {
    setSelectedStaff(sf);
    setSalaryVal(String(sf.salary));
    setRatingVal(String(sf.reviewRating));
  };
  const handleLeaveDecision = (id, decision) => {
    setLeaves((prev) => prev.map((l) => l.id === id ? { ...l, status: decision } : l));
    addToast("Success", `Registered Leave Application status: "${decision}"`);
  };
  const activeStaffTotal = staff.length;
  const averageRating = (staff.reduce((sum, s) => sum + s.reviewRating, 0) / activeStaffTotal).toFixed(2);
  return <div className="space-y-6">

      {
    /* Stats summaries banner */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-slate-400">TEAM INDEX SIZE</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{activeStaffTotal} Employees</h4>
            <p className="text-xs text-slate-450 mt-1">Admin, teachers & dorm wardens</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Users className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 text-indigo-100 p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-indigo-300">AVERAGE PROFESSORIAL RATIO</span>
            <h4 className="text-2xl font-black text-white mt-1">{averageRating} Stars</h4>
            <p className="text-xs text-indigo-200 mt-1">Based on student term summaries</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <Star className="h-5.5 w-5.5 text-amber-350 fill-amber-350" />
          </div>
        </div>

        <div className="bg-emerald-950 text-white p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-emerald-300 font-extrabold">MONTHLY OPERATING PAYROLL</span>
            <h4 className="text-2xl font-black text-emerald-300 mt-1">
              ${staff.reduce((sum, s) => sum + s.salary, 0).toLocaleString()} USD
            </h4>
            <p className="text-xs text-emerald-200 mt-1">SaaS budgeted ledger allocations</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-emerald-400">
            <Briefcase className="h-5.5 w-5.5" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {
    /* Left main: Staff roster list */
  }
        <div className="lg:col-span-2 bg-white border border-slate-105 rounded-3xl p-6 space-y-4">
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Active Staff Registry</h3>
          
          <div className="overflow-x-auto border border-slate-100 rounded-2xl">
            <table className="w-full text-xs font-semibold text-slate-750 text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Staff Designation</th>
                  <th className="p-3">Role Type</th>
                  <th className="p-3">Department Section</th>
                  <th className="p-3 font-medium">Rating Index</th>
                  <th className="p-3">Payroll Salary</th>
                  <th className="p-3 text-center">Desk Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {staff.map((sf) => <tr key={sf.id}>
                    <td className="p-3 font-extrabold text-slate-850">
                      <div>
                        <p>{sf.name}</p>
                        <p className="text-[10px] text-slate-400 font-bold mt-0.5">{sf.email}</p>
                      </div>
                    </td>
                    <td className="p-3">
                      <Badge variant={sf.role === "Principal" || sf.role === "Warden" ? "secondary" : "default"}>
                        {sf.role}
                      </Badge>
                    </td>
                    <td className="p-3 text-slate-500 font-bold">{sf.department}</td>
                    <td className="p-3 flex items-center gap-1.5 font-black text-amber-600 mt-1.5 border-none">
                      <Star className="h-3.5 w-3.5 fill-amber-500 text-amber-500" />
                      {sf.reviewRating}
                    </td>
                    <td className="p-3 text-slate-850 font-extrabold">${sf.salary}/mo</td>
                    <td className="p-3 text-center">
                      <button
    onClick={() => handleTriggerEdit(sf)}
    className="bg-indigo-50 border border-indigo-100 hover:bg-indigo-600 hover:text-white text-indigo-700 text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer"
  >
                        Adjust Pay
                      </button>
                    </td>
                  </tr>)}
              </tbody>
            </table>
          </div>
        </div>

        {
    /* Right side: Leaves approvals list and slip visual */
  }
        <div className="space-y-6">

          <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4">
            <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest flex gap-2 items-center">
              <CalendarCheck className="h-4.5 w-4.5 text-indigo-650" /> Leave Application Chits
            </h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Approve or reject medical leave requests from professors.</p>
            
            <div className="space-y-3.5 pt-1.5">
              {leaves.map((lv) => <div key={lv.id} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl flex flex-col gap-3">
                  <div className="flex justify-between items-start leading-none">
                    <div>
                      <p className="text-xs font-black text-slate-800">{lv.name}</p>
                      <p className="text-[10px] text-slate-450 mt-1 font-bold">Request Period: {lv.days} Calendar days</p>
                    </div>
                    <Badge variant={lv.status === "Approved" ? "success" : lv.status === "Rejected" ? "danger" : "warning"}>
                      {lv.status}
                    </Badge>
                  </div>

                  <p className="text-xs text-slate-500 italic font-semibold leading-relaxed bg-white p-2.5 rounded-lg border border-slate-100">
                    "{lv.reason}"
                  </p>

                  {lv.status === "Pending Review" && <div className="flex gap-2 justify-end pt-1">
                      <button
    onClick={() => handleLeaveDecision(lv.id, "Rejected")}
    className="p-1 px-3 border border-rose-200 text-rose-600 hover:bg-rose-50 text-[10px] uppercase rounded-lg font-bold transition cursor-pointer"
  >
                        Decline
                      </button>
                      <button
    onClick={() => handleLeaveDecision(lv.id, "Approved")}
    className="p-1 px-3 bg-indigo-600 text-white hover:bg-indigo-700 text-[10px] uppercase rounded-lg font-bold transition cursor-pointer"
  >
                        Authorize
                      </button>
                    </div>}
                </div>)}
            </div>
          </div>

          <div className="bg-indigo-50 border border-indigo-100 p-5 rounded-3xl space-y-3.5">
            <h4 className="text-xs font-bold text-indigo-900 uppercase tracking-widest flex items-center gap-2">
              <Landmark className="h-4 w-4" /> Treasury Audit Notes
            </h4>
            <p className="text-xs text-indigo-950 font-semibold leading-relaxed">
              Durable HR evaluations are validated by sandbox credentials. Changes to salary ranges update active operating models seamlessly.
            </p>
          </div>

        </div>

      </div>

      {
    /* Adjust payroll box popup */
  }
      <Dialog isOpen={!!selectedStaff} onClose={() => setSelectedStaff(null)} title="Modify Payroll & Scores">
        {selectedStaff && <form onSubmit={handleUpdateStaff} className="space-y-4 pt-1">
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-150 leading-relaxed text-xs font-semibold text-slate-600 space-y-1">
              <p>Employee Designation: <span className="text-slate-800 font-bold">{selectedStaff.name}</span></p>
              <p>Role Type: <span className="text-slate-800 font-bold">{selectedStaff.role}</span></p>
              <p>Department: <span className="text-slate-850 font-bold">{selectedStaff.department}</span></p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Input
    label="Monthly Salary ($ USD)"
    type="number"
    value={salaryVal}
    onChange={(e) => setSalaryVal(e.target.value)}
    required
  />
              <Select
    label="Performance Rating Review"
    options={[
      { label: "5.0 Stars (Outstanding)", value: "5.0" },
      { label: "4.5 Stars (Excellent)", value: "4.5" },
      { label: "4.0 Stars (Good Conduct)", value: "4.0" },
      { label: "3.5 Stars (Needs review)", value: "3.5" }
    ]}
    value={ratingVal}
    onChange={(e) => setRatingVal(e.target.value)}
  />
            </div>

            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setSelectedStaff(null)}>Cancel Edit</Button>
              <Button type="submit" className="bg-indigo-650 hover:bg-slate-900">Apply Treasury Settings</Button>
            </div>
          </form>}
      </Dialog>

    </div>;
}
