/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getVehicles, saveVehicles, logAction } from "../storage";
import { Button, Input, Badge, Dialog, useToast } from "../components/ui";
import { ShieldAlert, Plus, Truck, Video } from "lucide-react";
export default function ModSecurity({ user }) {
  const { addToast } = useToast();
  const [vehicles, setVehicles] = useState(() => getVehicles());
  const [isNewVehicleOpen, setIsNewVehicleOpen] = useState(false);
  const [driverName, setDriverName] = useState("");
  const [plateNumber, setPlateNumber] = useState("");
  const [vehicleDesc, setVehicleDesc] = useState("");
  const [purpose, setPurpose] = useState("");
  const [gateLocked, setGateLocked] = useState(false);
  const handleCreateVehicle = (e) => {
    e.preventDefault();
    if (!driverName || !plateNumber) {
      addToast("Error", "Driver Name and License Plate number are required", "error");
      return;
    }
    const newLog = {
      id: `veh-${Date.now()}`,
      driverName,
      plateNumber: plateNumber.toUpperCase(),
      vehicleDesc,
      purpose,
      entryTime: (/* @__PURE__ */ new Date()).toISOString().replace("T", " ").substring(0, 16)
    };
    const updated = [newLog, ...vehicles];
    setVehicles(updated);
    saveVehicles(updated);
    logAction(user.id, user.name, user.role, "Vehicle Access Logged", `Registered custom vehicle ${newLog.vehicleDesc} (Plate: ${newLog.plateNumber}) driven by ${driverName}`);
    addToast("Success", `Logged entrance checklist for vehicle ${newLog.plateNumber}`);
    setDriverName("");
    setPlateNumber("");
    setVehicleDesc("");
    setPurpose("");
    setIsNewVehicleOpen(false);
  };
  const handleMarkVehicleExit = (id) => {
    const updated = vehicles.map((v) => {
      if (v.id === id) {
        return { ...v, exitTime: (/* @__PURE__ */ new Date()).toISOString().replace("T", " ").substring(0, 16) };
      }
      return v;
    });
    setVehicles(updated);
    saveVehicles(updated);
    addToast("Success", "Vehicle departure logged");
  };
  const handleToggleGateLock = () => {
    const nextState = !gateLocked;
    setGateLocked(nextState);
    logAction(
      user.id,
      user.name,
      user.role,
      "Perimeter Lock Status Overridden",
      `Commanded campus gates state shift to: ${nextState ? "FULL LOCKDOWN" : "NORMAL SECURE ENTRANCES"}`
    );
    if (nextState) {
      addToast("CRITICAL DIRECTIVE ACTIVATED", "Campus perimeter gates locked in emergency lockdown sequence!", "error");
    } else {
      addToast("System Standardized", "Remote gateway released and standard NFC access cards validated", "success");
    }
  };
  return <div className="space-y-6">

      {
    /* Gate Controls alert row */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        
        {
    /* BIG EMERGENCY TOGGLE CONTROL */
  }
        <div
    className={`p-5 rounded-3xl flex justify-between items-center transition duration-350 cursor-pointer ${gateLocked ? "bg-rose-950 border border-rose-500 text-rose-100" : "bg-slate-900 border border-slate-850 text-white"}`}
    onClick={handleToggleGateLock}
  >
          <div>
            <span className="text-[10px] uppercase font-bold tracking-wider text-rose-450 block">REMOTE EMERGENCY OVERRIDE</span>
            <h4 className="text-xl font-black mt-1.5 leading-none">
              {gateLocked ? "CAMPUS LOCKDOWN ON" : "CAMPUS GATE NORMAL"}
            </h4>
            <p className="text-xs text-rose-350/70 mt-1 font-semibold">Click to toggle remote lock loops</p>
          </div>
          <div className={`p-3 rounded-2xl transform transition-transform ${gateLocked ? "bg-rose-600 animate-pulse scale-110 text-white" : "bg-white/10 text-rose-500"}`}>
            <ShieldAlert className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">VEHICLES LOGGED ON-PREMISES</span>
            <h4 className="text-xl font-black text-slate-800 mt-1">
              {vehicles.filter((v) => !v.exitTime).length} Active Vehicles
            </h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Delivery vans & pantry trucks</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Truck className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-emerald-950 text-white p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-emerald-300">CCTV FEED CHANNELS</span>
            <h4 className="text-xl font-black text-white mt-1">3 Monitors Active</h4>
            <p className="text-xs text-emerald-200 mt-1">NFC logging is synchronized</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-emerald-300">
            <Video className="h-6 w-6" />
          </div>
        </div>
      </div>

      {
    /* CCTV Mock Grid Monitors Panel */
  }
      <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4">
        <div>
          <h3 className="text-sm font-extrabold text-slate-805 uppercase tracking-widest flex items-center gap-1.5">
            <Video className="h-4.5 w-4.5 text-indigo-505" /> Telemetry Live Security CCTV Streams
          </h3>
          <p className="text-xs text-slate-400 font-semibold mt-1">Simulated surveillance scanlines. Perimeter cameras verified secure.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 pt-1.5">
          
          {
    /* CAMERA 1 */
  }
          <div className="h-36 bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden relative flex flex-col justify-between p-4">
            <div className="flex justify-between text-[9px] uppercase tracking-wider text-slate-500 font-bold leading-none">
              <span>CAM 01: FRONT ARCH GATEWAY</span>
              <span className="text-emerald-400 font-black animate-pulse">● FEED SECURE</span>
            </div>
            
            <div className="absolute inset-0 bg-linear-to-b from-transparent via-emerald-500/5 to-transparent pointer-events-none animate-scanline" />
            <div className="text-center font-mono text-[9px] text-slate-550 italic">
              -- CHECKPOINTS ONLINE --
            </div>

            <div className="flex justify-between items-center text-[10px] font-bold text-slate-400 mt-2">
              <span>720p H.264 FEED</span>
              <span>UTC +00:00</span>
            </div>
          </div>

          {
    /* CAMERA 2 */
  }
          <div className="h-36 bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden relative flex flex-col justify-between p-4">
            <div className="flex justify-between text-[9px] uppercase tracking-wider text-slate-500 font-bold leading-none">
              <span>CAM 02: DORM WEST WING ROAD</span>
              <span className="text-emerald-400 font-black animate-pulse">● FEED SECURE</span>
            </div>
            
            <div className="absolute inset-0 bg-linear-to-b from-transparent via-emerald-500/5 to-transparent pointer-events-none animate-scanline" />
            <div className="text-center font-mono text-[9px] text-slate-550 italic">
              -- MOTION TRACK SENSORS ON --
            </div>

            <div className="flex justify-between items-center text-[10px] font-bold text-slate-400 mt-2">
              <span>720p H.264 FEED</span>
              <span>UTC +00:00</span>
            </div>
          </div>

          {
    /* CAMERA 3 */
  }
          <div className="h-36 bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden relative flex flex-col justify-between p-4">
            <div className="flex justify-between text-[9px] uppercase tracking-wider text-slate-500 font-bold leading-none">
              <span>CAM 03: PANTRY LOADING DOCK</span>
              <span className="text-emerald-400 font-black animate-pulse">● FEED SECURE</span>
            </div>
            
            <div className="absolute inset-0 bg-linear-to-b from-transparent via-emerald-500/5 to-transparent pointer-events-none animate-scanline" />
            <div className="text-center font-mono text-[9px] text-slate-550 italic">
              -- SUPPLIER ACCESS GATE EXPOSED --
            </div>

            <div className="flex justify-between items-center text-[10px] font-bold text-slate-400 mt-2">
              <span>720p H.264 FEED</span>
              <span>UTC +00:00</span>
            </div>
          </div>

        </div>
      </div>

      {
    /* Roster Controls layout */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight">Suppliers Vehicle checkin Ledger</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Audit cargo carriers, logistics, and vendor fleet entrances. Issue exit stamps systematically.</p>
        </div>
        <Button onClick={() => setIsNewVehicleOpen(true)} className="flex gap-2 items-center text-xs py-2 bg-slate-900 border border-transparent font-extrabold shrink-0">
          <Plus className="h-4 w-4" /> Log Supplier Delivery
        </Button>
      </div>

      {
    /* Vehicle Registry */
  }
      <div className="bg-white border border-slate-100 rounded-3xl p-6">
        <div className="overflow-x-auto border border-slate-100 rounded-2xl">
          <table className="w-full text-xs font-semibold text-slate-700 text-left">
            <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
              <tr>
                <th className="p-4">Supplier Driver Name</th>
                <th className="p-4">Badge Plate Index</th>
                <th className="p-4">Vehicle Description</th>
                <th className="p-4">Delivery Intent</th>
                <th className="p-4">Arrival Timestamp</th>
                <th className="p-4">Departure Timestamp</th>
                <th className="p-4 text-center">Status Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-semibold">
              {vehicles.length === 0 ? <tr>
                  <td colSpan={7} className="p-4 text-center text-slate-400">No vehicles logged inside the archive</td>
                </tr> : vehicles.map((v) => <tr key={v.id}>
                    <td className="p-4 font-extrabold text-slate-850">{v.driverName}</td>
                    <td className="p-4">
                      <span className="bg-slate-100 text-slate-800 px-2 py-1 rounded-md font-mono text-[10px] font-bold">
                        {v.plateNumber}
                      </span>
                    </td>
                    <td className="p-4">{v.vehicleDesc}</td>
                    <td className="p-4 max-w-xs justify-normal text-slate-500 italic">"{v.purpose}"</td>
                    <td className="p-4 text-slate-405 font-bold">{v.entryTime}</td>
                    <td className="p-4 text-emerald-650 font-bold">{v.exitTime || "-- verified on campus --"}</td>
                    <td className="p-4 text-center font-bold">
                      {v.exitTime ? <Badge variant="secondary">Exited Ground</Badge> : <button
    onClick={() => handleMarkVehicleExit(v.id)}
    className="bg-indigo-50 border border-indigo-150 hover:bg-slate-900 border-none font-bold text-[10px] text-indigo-700 hover:text-white px-2.5 py-1.5 rounded-xl transition cursor-pointer"
  >
                          Stamp Exit
                        </button>}
                    </td>
                  </tr>)}
            </tbody>
          </table>
        </div>
      </div>

      {
    /* Model: Log supplier delivery */
  }
      <Dialog isOpen={isNewVehicleOpen} onClose={() => setIsNewVehicleOpen(false)} title="Authenticate Cargo Vehicle Carriage">
        <form onSubmit={handleCreateVehicle} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Supplier Driver Name"
    value={driverName}
    onChange={(e) => setDriverName(e.target.value)}
    placeholder="e.g. Samuel Patterson"
    required
  />
            <Input
    label="License Plate Index Number"
    value={plateNumber}
    onChange={(e) => setPlateNumber(e.target.value)}
    placeholder="e.g. TX-902-XQ"
    required
  />
          </div>

          <Input
    label="Vehicle Make & Description"
    value={vehicleDesc}
    onChange={(e) => setVehicleDesc(e.target.value)}
    placeholder="e.g. White Ford Transit Delivery van, Red Box Toyota truck..."
    required
  />

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Delivery purpose declaration</label>
            <textarea
    className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-505 focus:bg-white max-h-32 transition"
    rows={2}
    placeholder="e.g. Delivering monthly pantry groceries or carton notebook stacks to inventory storage..."
    value={purpose}
    onChange={(e) => setPurpose(e.target.value)}
    required
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewVehicleOpen(false)}>Cancel Authorization</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900 border border-transparent font-extrabold text-white">
              Log Gate In-Check Entrance
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
