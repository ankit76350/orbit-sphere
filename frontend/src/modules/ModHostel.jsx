/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import {
  getHostels,
  saveHostels,
  getVisitors,
  saveVisitors,
  getOutPasses,
  saveOutPasses,
  getStudents,
  saveStudents,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, Tabs, TabsList, TabsTrigger, TabsContent, useToast } from "../components/ui";
import { Shield, Users, Bed, Check, X, ClipboardPlus, Clock } from "lucide-react";
export default function ModHostel({ user }) {
  const { addToast } = useToast();
  const [rooms, setRooms] = useState(getHostels());
  const [visitors, setVisitors] = useState(getVisitors());
  const [outpasses, setOutpasses] = useState(getOutPasses());
  const [vTab, setVTab] = useState("bunks");
  const [isVisitorOpen, setIsVisitorOpen] = useState(false);
  const [visitorName, setVisitorName] = useState("");
  const [relationship, setRelationship] = useState("Parent");
  const [visitorIdSelection, setVisitorIdSelection] = useState("student-1");
  const [purpose, setPurpose] = useState("");
  const totalBeds = rooms.reduce((sum, r) => sum + r.capacity, 0);
  const occupiedBeds = rooms.flat().reduce((sum, r) => {
    return sum + r.beds.filter((b) => b.studentDocsId !== null).length;
  }, 0);
  const vacancyBeds = totalBeds - occupiedBeds;
  const handleCreateVisitor = (e) => {
    e.preventDefault();
    if (!visitorName || !purpose) {
      addToast("Error", "Visitor Name and Purpose of meeting are required", "error");
      return;
    }
    const students = getStudents();
    const targetStudent = students.find((s) => s.id === visitorIdSelection);
    const newLog = {
      id: `vlog-${Date.now()}`,
      visitorName,
      relationship,
      studentDocsId: visitorIdSelection,
      studentName: targetStudent ? targetStudent.name : "Unknown Roster",
      entryTime: (/* @__PURE__ */ new Date()).toISOString().replace("T", " ").substring(0, 16),
      purpose
    };
    const updated = [newLog, ...visitors];
    setVisitors(updated);
    saveVisitors(updated);
    logAction(user.id, user.name, user.role, "Visitor Entrance Logged", `Registered visit from ${visitorName} (Rel: ${relationship}) for student ${newLog.studentName}`);
    addToast("Success", `Registered checkin for guest ${visitorName}`);
    setVisitorName("");
    setPurpose("");
    setIsVisitorOpen(false);
  };
  const handleVisitorExit = (vlogId) => {
    const updated = visitors.map((v) => {
      if (v.id === vlogId) {
        return { ...v, exitTime: (/* @__PURE__ */ new Date()).toISOString().replace("T", " ").substring(0, 16) };
      }
      return v;
    });
    setVisitors(updated);
    saveVisitors(updated);
    addToast("Success", "Guest marked as exited");
  };
  const handleOutpassStatus = (id, newStatus) => {
    const freshOut = getOutPasses();
    const updated = freshOut.map((op) => {
      if (op.id === id) {
        return { ...op, status: newStatus, approvedByName: user.name };
      }
      return op;
    });
    setOutpasses(updated);
    saveOutPasses(updated);
    const match = updated.find((op) => op.id === id);
    logAction(user.id, user.name, user.role, "Outpass Approved/Declined", `Set status of Outpass ID ${id} (Student: ${match?.studentName}) to: ${newStatus}`);
    addToast("Success", `Mailed outpass status update "${newStatus}" to guardian`);
  };
  const handleVacateBed = (roomId, bedNum, studentDocsId) => {
    const freshHostels = getHostels();
    const roomIdx = freshHostels.findIndex((r) => r.id === roomId);
    if (roomIdx === -1) return;
    const bed = freshHostels[roomIdx].beds.find((b) => b.bedNo === bedNum);
    if (bed) {
      bed.studentDocsId = null;
      bed.studentName = void 0;
    }
    setRooms(freshHostels);
    saveHostels(freshHostels);
    const freshStudents = getStudents();
    const sIdx = freshStudents.findIndex((s) => s.id === studentDocsId);
    if (sIdx !== -1) {
      freshStudents[sIdx].hostelBuilding = void 0;
      freshStudents[sIdx].hostelFloor = void 0;
      freshStudents[sIdx].hostelRoomNo = void 0;
      freshStudents[sIdx].hostelBedNo = void 0;
      freshStudents[sIdx].hostelOptIn = false;
      saveStudents(freshStudents);
    }
    logAction(user.id, user.name, user.role, "Bed Vacated", `De-allocated Bed ${bedNum} from Room ${freshHostels[roomIdx].roomNo} inside Sandbox`);
    addToast("Success", "Bed released. Roster file updated.");
  };
  return <div className="space-y-6">

      {
    /* RosterOccupancy gauge banner */
  }
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">HOSTEL CAPACITY</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{totalBeds} Beds</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Across 24 premium rooms</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Bed className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 p-5 rounded-3xl flex justify-between items-center text-white">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">ACTIVE OCCUPANCY</span>
            <h4 className="text-2xl font-black text-emerald-400 mt-1">{occupiedBeds} Beds</h4>
            <p className="text-xs text-indigo-200 mt-1">
              {Math.round(occupiedBeds / totalBeds * 100)}% Occupancy index
            </p>
          </div>
          <div className="p-3 bg-white/10 text-white rounded-2xl">
            <Users className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-emerald-950 p-5 rounded-3xl flex justify-between items-center text-white">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-emerald-300">AVAILABLE BUNK PLACES</span>
            <h4 className="text-2xl font-black text-white mt-1">{vacancyBeds} Vacancies</h4>
            <p className="text-xs text-emerald-200 mt-1">Ready for mid-term admits</p>
          </div>
          <div className="p-3 bg-white/10 text-emerald-300 rounded-2xl">
            <Shield className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">PENDING OUT-PASSES</span>
            <h4 className="text-2xl font-black text-rose-600 mt-1">
              {outpasses.filter((op) => op.status.startsWith("Pending")).length} Tickets
            </h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Requires Warden authority</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl">
            <Clock className="h-6 w-6 animate-pulse" />
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-100 p-6 rounded-3xl">
        <Tabs activeTab={vTab} onChange={setVTab}>
          <TabsList className="border-b border-slate-100 flex gap-2">
            <TabsTrigger value="bunks">Rooms & Bunks Map</TabsTrigger>
            <TabsTrigger value="visitors">Guest Visitor Logs</TabsTrigger>
            <TabsTrigger value="outpasses">Out-Pass Workflows</TabsTrigger>
          </TabsList>

          {
    /* TAB 1: Bunks Map */
  }
          <TabsContent value="bunks">
            <div className="space-y-6 pt-4">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Premises Rooms Board</h4>
                  <p className="text-xs text-slate-450 mt-1">Manage single-beds allocations inside Vanguard (Boys) & Seraphina (Girls) houses.</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {rooms.map((room) => {
    const occupantsCount = room.beds.filter((b) => b.studentDocsId !== null).length;
    return <div key={room.id} className="bg-slate-50 border border-slate-150 p-5 rounded-2xl flex flex-col gap-4">
                      
                      {
      /* Room descriptive row */
    }
                      <div className="flex justify-between items-center pb-2.5 border-b border-slate-200">
                        <div>
                          <p className="text-xs font-black text-slate-800">{room.buildingName}</p>
                          <p className="text-[10px] text-slate-400 font-extrabold uppercase mt-1">Room {room.roomNo} - {room.floor}</p>
                        </div>
                        <Badge variant={occupantsCount === room.capacity ? "danger" : occupantsCount === 0 ? "success" : "warning"}>
                          {occupantsCount} / {room.capacity} Bunks
                        </Badge>
                      </div>

                      {
      /* Beds array status */
    }
                      <div className="space-y-2 text-xs">
                        {room.beds.map((bed, bIdx) => <div key={bIdx} className="bg-white p-3 rounded-xl border border-slate-100 flex items-center justify-between font-bold">
                            <span className="text-slate-650 flex gap-1.5 items-center">
                              <Bed className={`h-4 w-4 ${bed.studentDocsId ? "text-indigo-600" : "text-slate-300"}`} />
                              {bed.bedNo}
                            </span>

                            {bed.studentDocsId ? <div className="flex items-center gap-2">
                                <span className="text-slate-800 font-black line-clamp-1">{bed.studentName}</span>
                                <button
      type="button"
      onClick={() => handleVacateBed(room.id, bed.bedNo, bed.studentDocsId)}
      className="text-[9px] uppercase font-bold text-rose-500 bg-rose-50 px-2 py-1 rounded-md hover:bg-rose-600 hover:text-white transition cursor-pointer"
      title="Vacate Bed and update files"
    >
                                  Release
                                </button>
                              </div> : <span className="text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-md text-[10px] tracking-wide uppercase font-black">
                                Vacant
                              </span>}
                          </div>)}
                      </div>

                    </div>;
  })}
              </div>

            </div>
          </TabsContent>

          {
    /* TAB 2: Visitors Logs */
  }
          <TabsContent value="visitors">
            <div className="space-y-5 pt-4">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Campus Guest Checkin Console</h4>
                  <p className="text-xs text-slate-450 mt-1">Record on-premise guardian verification, exit timestamps, and safety purposes audits.</p>
                </div>
                <Button onClick={() => setIsVisitorOpen(true)} className="flex gap-2 items-center text-xs bg-slate-900 border border-transparent">
                  <ClipboardPlus className="h-4 w-4" /> Guest Checkin
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-100 rounded-3xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-4">Visitor Guest Name</th>
                      <th className="p-4">Entity Relationship</th>
                      <th className="p-4">Student Associated</th>
                      <th className="p-4">Entry Time</th>
                      <th className="p-4">Exit Time</th>
                      <th className="p-4">Purpose Audit</th>
                      <th className="p-4 text-center">Status Act</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium">
                    {visitors.length === 0 ? <tr>
                        <td colSpan={7} className="p-4 text-center text-slate-400">Empty logs files loaded check Sandbox seeding</td>
                      </tr> : visitors.map((vl) => <tr key={vl.id}>
                          <td className="p-4 font-bold text-slate-805">{vl.visitorName}</td>
                          <td className="p-4">{vl.relationship}</td>
                          <td className="p-4 font-black text-indigo-700">{vl.studentName}</td>
                          <td className="p-4 font-bold text-slate-400">{vl.entryTime}</td>
                          <td className="p-4 font-bold text-emerald-805">{vl.exitTime || "-- ongoing --"}</td>
                          <td className="p-4 text-xs text-slate-400 max-w-xs">{vl.purpose}</td>
                          <td className="p-4 text-center font-bold">
                            {vl.exitTime ? <Badge variant="secondary">Exited Safe</Badge> : <button
    onClick={() => handleVisitorExit(vl.id)}
    className="bg-rose-50 text-rose-600 border border-rose-100 text-[10px] px-2.5 py-1 rounded-xl font-bold hover:bg-rose-600 hover:text-white transition cursor-pointer"
  >
                                Mark Checkout
                              </button>}
                          </td>
                        </tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          </TabsContent>

          {
    /* TAB 3: Out-passes workflow */
  }
          <TabsContent value="outpasses">
            <div className="space-y-4 pt-4">
              <div>
                <h4 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Guardian Out-Pass Approval Ledger</h4>
                <p className="text-xs text-slate-450 mt-1">Approve/Decline student curfew passes for weekend releases or emergency dental checkups.</p>
              </div>

              <div className="space-y-3.5 pt-1">
                {outpasses.length === 0 ? <p className="text-xs text-center text-slate-400 py-8">No current outpass petitions on folder</p> : outpasses.map((pass) => <div key={pass.id} className="p-5 bg-slate-50 border border-slate-100 rounded-3xl flex flex-col md:flex-row justify-between gap-4 items-start md:items-center">
                      <div className="space-y-1.5 flex-1 select-text">
                        <div className="flex gap-2.5 items-center flex-wrap">
                          <span className="text-sm font-black text-slate-800 leading-none">{pass.studentName}</span>
                          <span className="bg-indigo-50 text-indigo-600 text-[9px] px-2 py-0.5 rounded font-black uppercase">
                            Outpass Request
                          </span>
                        </div>
                        <p className="text-xs text-slate-400 font-bold">Parent Signee: {pass.parentName}</p>
                        <p className="text-xs text-slate-650 leading-relaxed max-w-2xl bg-white p-3 rounded-xl border border-slate-150 mt-2 font-medium">
                          <span className="font-bold text-slate-600 uppercase text-[9px] block mb-1">Reason declaration:</span>
                          "{pass.reason}"
                        </p>
                        <div className="flex gap-4 pt-2.5 text-[11px] text-slate-400 font-bold flex-wrap">
                          <span className="flex items-center gap-1.5">
                            <Clock className="h-3.5 w-3.5 text-indigo-500" />
                            Release: {pass.leaveDate}
                          </span>
                          <span className="flex items-center gap-1.5">
                            <Clock className="h-3.5 w-3.5 text-rose-500" />
                            Est. Return: {pass.returnDate}
                          </span>
                        </div>
                      </div>

                      <div className="flex flex-col items-end gap-3 shrink-0 w-full md:w-auto self-stretch justify-between">
                        <Badge
    variant={pass.status === "Approved" ? "success" : pass.status === "Rejected" ? "danger" : "warning"}
  >
                          {pass.status}
                        </Badge>

                        {pass.status.startsWith("Pending") && <div className="flex gap-2">
                            <button
    onClick={() => handleOutpassStatus(pass.id, "Rejected")}
    className="font-bold flex items-center justify-center p-2 rounded-xl border border-rose-200 text-rose-600 bg-white hover:bg-rose-50 text-xs transition cursor-pointer"
    title="Decline Pass"
  >
                              <X className="h-4.5 w-4.5" />
                            </button>
                            <button
    onClick={() => handleOutpassStatus(pass.id, "Approved")}
    className="font-bold flex items-center justify-center p-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 text-xs transition cursor-pointer"
    title="Approve Pass"
  >
                              <Check className="h-4.5 w-4.5" />
                            </button>
                          </div>}

                        {pass.approvedByName && <p className="text-[10px] text-slate-400 font-semibold italic">Approved by {pass.approvedByName}</p>}
                      </div>
                    </div>)}
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {
    /* Guest Checkin Modal Dialog */
  }
      <Dialog isOpen={isVisitorOpen} onClose={() => setIsVisitorOpen(false)} title="Register Guest Entry Protocol">
        <form onSubmit={handleCreateVisitor} className="space-y-4 pt-1">
          <Input
    label="Guest Visitor Name"
    value={visitorName}
    onChange={(e) => setVisitorName(e.target.value)}
    placeholder="e.g. Margaret Thatcher"
    required
  />
          <div className="grid grid-cols-2 gap-4">
            <Select
    label="Legal Relationship"
    options={[
      { label: "Mother", value: "Mother" },
      { label: "Father", value: "Father" },
      { label: "Uncle / Aunt", value: "Uncle/Aunt" },
      { label: "Sibling Guardian", value: "Sibling" },
      { label: "Courier Services", value: "Exchange Agent" }
    ]}
    value={relationship}
    onChange={(e) => setRelationship(e.target.value)}
  />
            <Select
    label="Associated Boarder Student"
    options={getStudents().slice(0, 45).map((s) => ({ label: `${s.name} (${s.admissionNo})`, value: s.id }))}
    value={visitorIdSelection}
    onChange={(e) => setVisitorIdSelection(e.target.value)}
  />
          </div>
          <div>
            <label className="text-xs font-bold text-slate-600 block mb-1.5 uppercase tracking-wider">Purpose of visit (Auditable)</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 max-h-32 focus:bg-white transition"
    rows={3}
    placeholder="e.g. Delivering custom study lamps or picking child for approved family dinner..."
    value={purpose}
    onChange={(e) => setPurpose(e.target.value)}
    required
  />
          </div>
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsVisitorOpen(false)}>Cancel Checkin</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900">Authorize Campus Entry</Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
