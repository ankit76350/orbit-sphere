/**
 * @license
 * SPDX-License-Identifier: Apache-2.5
 */
import { useState, useEffect, useRef } from "react";
import {
  getRoutes,
  saveRoutes,
  getTransportVehicles,
  saveTransportVehicles,
  getDrivers,
  saveDrivers,
  getTransportAllocations,
  saveTransportAllocations,
  getTransportAttendance,
  saveTransportAttendance,
  getStudents,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Bus,
  MapPin,
  Clock,
  Navigation,
  Calendar,
  DollarSign,
  User,
  ShieldAlert,
  ClipboardList,
  Activity,
  Plus,
  Trash2,
  Play,
  Pause,
  AlertTriangle,
  FileText,
  Compass,
  CheckCircle,
  XCircle,
  HelpCircle,
  Bell
} from "lucide-react";

export default function ModTransport({ user }) {
  const { addToast } = useToast();
  
  // Data State
  const [routes, setRoutes] = useState(() => getRoutes());
  const [vehicles, setVehicles] = useState(() => getTransportVehicles());
  const [drivers, setDrivers] = useState(() => getDrivers());
  const [allocations, setAllocations] = useState(() => getTransportAllocations());
  const [attendance, setAttendance] = useState(() => getTransportAttendance());
  const [studentsList] = useState(() => getStudents());
  
  // Active UI Navigation Tab
  // Options: dashboard, routes, fleet, allocations, attendance, livegps, ledger
  const [activeTab, setActiveTab] = useState("dashboard");

  // Selection states for actions
  const [selectedRouteDocsId, setSelectedRouteDocsId] = useState(routes[0]?.id || "");
  const [selectedAttendanceRoute, setSelectedAttendanceRoute] = useState(routes[0]?.id || "");
  
  // Modals state
  const [isAddRouteOpen, setIsAddRouteOpen] = useState(false);
  const [isAddStopOpen, setIsAddStopOpen] = useState(false);
  const [isAddVehicleOpen, setIsAddVehicleOpen] = useState(false);
  const [isAddDriverOpen, setIsAddDriverOpen] = useState(false);
  const [isAddAllocOpen, setIsAddAllocOpen] = useState(false);

  // Form states
  const [routeNo, setRouteNo] = useState("");
  const [routeName, setRouteName] = useState("");
  const [routeStart, setRouteStart] = useState("");
  const [routeEnd, setRouteEnd] = useState("");
  const [routeDist, setRouteDist] = useState("");

  const [stopName, setStopName] = useState("");
  const [stopPickupTime, setStopPickupTime] = useState("07:30");
  const [stopDropTime, setStopDropTime] = useState("16:30");
  const [stopLat, setStopLat] = useState("40.712");
  const [stopLng, setStopLng] = useState("-74.006");

  const [vehNo, setVehNo] = useState("");
  const [vehCapacity, setVehCapacity] = useState("");
  const [vehFuel, setVehFuel] = useState("Diesel");
  const [vehInsurance, setVehInsurance] = useState("2026-12-31");
  const [vehFitness, setVehFitness] = useState("2026-12-31");

  const [drvName, setDrvName] = useState("");
  const [drvPhone, setDrvPhone] = useState("");
  const [drvLicense, setDrvLicense] = useState("");
  const [drvExpiry, setDrvExpiry] = useState("2028-06-30");
  const [drvVehId, setDrvVehId] = useState("");

  const [allocStudentDocsId, setAllocStudentDocsId] = useState("");
  const [allocRouteDocsId, setAllocRouteDocsId] = useState("");
  const [allocPickupStop, setAllocPickupStop] = useState("");
  const [allocDropStop, setAllocDropStop] = useState("");
  const [allocFee, setAllocFee] = useState("120");

  // GPS Simulation state
  const [simulating, setSimulating] = useState(false);
  const [busProgress, setBusProgress] = useState(0); // 0 to 100 percentage
  const [currentETA, setCurrentETA] = useState(15); // minutes
  const [currentSpeed, setCurrentSpeed] = useState(0); // km/h
  const [geofenceAlert, setGeofenceAlert] = useState(false);
  const [notificationsAlerts, setNotificationsAlerts] = useState([
    { id: 1, time: "07:30 AM", msg: "RT-ALPHA dispatcher logged: All pre-start inspections complete." },
    { id: 2, time: "07:45 AM", msg: "BUS-402X has successfully departed Pines Terminal." }
  ]);

  // Ref for simulation interval
  const simInterval = useRef(null);

  // Setup initial route details if routes are empty
  useEffect(() => {
    if (routes.length > 0 && !selectedRouteDocsId) {
      setSelectedRouteDocsId(routes[0].id);
    }
  }, [routes, selectedRouteDocsId]);

  // Simulated GPS Telemetry Interval
  useEffect(() => {
    if (simulating) {
      setCurrentSpeed(42);
      simInterval.current = setInterval(() => {
        setBusProgress((prev) => {
          const next = prev + 2;
          if (next >= 100) {
            setSimulating(false);
            setCurrentSpeed(0);
            setCurrentETA(0);
            addToast("Bus Arrived", "Simulated route RT-ALPHA has reached St. Jude School Terminal!");
            setNotificationsAlerts((nList) => [
              { id: Date.now(), time: "08:15 AM", msg: "Transit Alert: BUS-402X has arrived at St. Jude Gate. Parents notified." },
              ...nList
            ]);
            return 100;
          }
          
          // Calculate dynamic ETA
          const remainingMinutes = Math.max(1, Math.round(15 - (next * 0.15)));
          setCurrentETA(remainingMinutes);
          
          // Speed variance
          setCurrentSpeed(Math.floor(35 + Math.random() * 15));
          
          // Trigger mock geofence alert at 40% and 80%
          if (Math.round(next) === 40) {
            setGeofenceAlert(true);
            setNotificationsAlerts((nList) => [
              { id: Date.now(), time: "07:55 AM", msg: "Geofence Crossed: Route Alpha has entered the 1km Radius Zone." },
              ...nList
            ]);
            addToast("Geofence Notification", "Route Alpha entering school periphery geofence!");
          }
          if (Math.round(next) === 50) {
            setGeofenceAlert(false);
          }
          
          return next;
        });
      }, 800);
    } else {
      if (simInterval.current) {
        clearInterval(simInterval.current);
      }
    }
    return () => {
      if (simInterval.current) {
        clearInterval(simInterval.current);
      }
    };
  }, [simulating]);

  // Dynamic values depending on active sandbox role
  const isDriverRole = user.role === "Driver";
  const isAccountantRole = user.role === "Accountant";
  const isParentRole = user.role === "Parent";
  
  // Set default tabs based on roles
  useEffect(() => {
    if (isDriverRole) {
      setActiveTab("attendance");
    } else if (isAccountantRole) {
      setActiveTab("ledger");
    } else if (isParentRole) {
      setActiveTab("livegps");
    }
  }, [user.role]);

  // Form Submissions
  const handleCreateRoute = (e) => {
    e.preventDefault();
    if (!routeNo || !routeName) return;
    
    const newRoute = {
      id: `tr-${Date.now()}`,
      routeNo: routeNo.toUpperCase(),
      routeName,
      startLocation: routeStart || "Main Terminal",
      endLocation: routeEnd || "St. Jude Terminal",
      distance_km: parseFloat(routeDist) || 10.0,
      stops: [],
      status: "Active"
    };

    const next = [...routes, newRoute];
    setRoutes(next);
    saveRoutes(next);
    logAction(user.id, user.name, user.role, "Route Added", `Created route ${routeNo}: ${routeName}`);
    addToast("Route Created", `Route "${routeNo}" successfully added.`);
    
    setRouteNo("");
    setRouteName("");
    setRouteStart("");
    setRouteEnd("");
    setRouteDist("");
    setIsAddRouteOpen(false);
  };

  const handleDeleteRoute = (id, code) => {
    if (window.confirm(`Are you sure you want to delete route ${code}?`)) {
      const next = routes.filter((r) => r.id !== id);
      setRoutes(next);
      saveRoutes(next);
      logAction(user.id, user.name, user.role, "Route Deleted", `Removed route ID ${id}: ${code}`);
      addToast("Route Deleted", `Route ${code} removed.`);
    }
  };

  const handleAddStop = (e) => {
    e.preventDefault();
    if (!stopName) return;

    const targetIdx = routes.findIndex(r => r.id === selectedRouteDocsId);
    if (targetIdx === -1) return;

    const updatedRoutes = [...routes];
    const routeStops = updatedRoutes[targetIdx].stops || [];
    
    const newStop = {
      name: stopName,
      pickupTime: stopPickupTime,
      dropTime: stopDropTime,
      latitude: stopLat,
      longitude: stopLng,
      sequence: routeStops.length + 1
    };

    updatedRoutes[targetIdx].stops = [...routeStops, newStop];
    setRoutes(updatedRoutes);
    saveRoutes(updatedRoutes);
    
    logAction(user.id, user.name, user.role, "Stop Added", `Added stop "${stopName}" to route ${updatedRoutes[targetIdx].routeNo}`);
    addToast("Stop Registered", `Stop "${stopName}" added to timeline.`);
    
    setStopName("");
    setIsAddStopOpen(false);
  };

  const handleCreateVehicle = (e) => {
    e.preventDefault();
    if (!vehNo || !vehCapacity) return;

    const newVeh = {
      id: `veh-${Date.now()}`,
      vehicleNo: vehNo.toUpperCase(),
      capacity: parseInt(vehCapacity) || 40,
      insuranceExpiry: vehInsurance,
      fitnessExpiry: vehFitness,
      fuelType: vehFuel,
      status: "Operational"
    };

    const next = [...vehicles, newVeh];
    setVehicles(next);
    saveTransportVehicles(next);
    logAction(user.id, user.name, user.role, "Vehicle Registered", `Catalogued vehicle ${vehNo}`);
    addToast("Vehicle Registered", `Vehicle ${vehNo} catalogued.`);
    
    setVehNo("");
    setVehCapacity("");
    setIsAddVehicleOpen(false);
  };

  const handleCreateDriver = (e) => {
    e.preventDefault();
    if (!drvName || !drvPhone || !drvLicense) return;

    const newDrv = {
      id: `drv-${Date.now()}`,
      name: drvName,
      phone: drvPhone,
      licenseNo: drvLicense,
      licenseExpiry: drvExpiry,
      vehicleDocsId: drvVehId || null
    };

    const next = [...drivers, newDrv];
    setDrivers(next);
    saveDrivers(next);
    logAction(user.id, user.name, user.role, "Driver Profile Created", `Registered driver: ${drvName}`);
    addToast("Driver Profile Saved", `Driver ${drvName} registered.`);

    setDrvName("");
    setDrvPhone("");
    setDrvLicense("");
    setIsAddDriverOpen(false);
  };

  const handleCreateAllocation = (e) => {
    e.preventDefault();
    if (!allocStudentDocsId || !allocRouteDocsId) return;

    const studentInfo = studentsList.find(s => s.id === allocStudentDocsId);
    if (!studentInfo) return;

    const newAlloc = {
      id: `alloc-${Date.now()}`,
      studentDocsId: allocStudentDocsId,
      studentName: studentInfo.name,
      routeDocsId: allocRouteDocsId,
      pickupStopName: allocPickupStop || "First Stop",
      dropStopName: allocDropStop || "Academic Gate",
      feeAmount: parseFloat(allocFee) || 120.00,
      startDate: new Date().toISOString().split("T")[0],
      status: "Active"
    };

    const next = [...allocations, newAlloc];
    setAllocations(next);
    saveTransportAllocations(next);
    logAction(user.id, user.name, user.role, "Student Transport Allocated", `Assigned student ${studentInfo.name} to route ID ${allocRouteDocsId}`);
    addToast("Allocation Success", `${studentInfo.name} assigned to route.`);

    setAllocStudentDocsId("");
    setIsAddAllocOpen(false);
  };

  const handleToggleAttendance = (studentDocsId, status) => {
    const updated = [...attendance];
    const today = new Date().toISOString().split("T")[0];
    
    const idx = updated.findIndex(a => a.studentDocsId === studentDocsId && a.date === today);
    const studentInfo = allocations.find(a => a.studentDocsId === studentDocsId);

    if (idx !== -1) {
      updated[idx].status = status;
      updated[idx].timestamp = new Date().toLocaleTimeString();
    } else {
      updated.unshift({
        id: `att-${Date.now()}-${studentDocsId}`,
        studentDocsId,
        studentName: studentInfo?.studentName || "Scholar",
        routeDocsId: selectedAttendanceRoute,
        date: today,
        status,
        timestamp: new Date().toLocaleTimeString()
      });
    }

    setAttendance(updated);
    saveTransportAttendance(updated);

    // Trigger simulated notification alerts
    const parentMsg = `Alert triggered to parents: ${studentInfo?.studentName} logged as "${status}" for daily transport route.`;
    setNotificationsAlerts(prev => [
      { id: Date.now(), time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), msg: parentMsg },
      ...prev
    ]);

    addToast("Attendance Registered", `Logged ${studentInfo?.studentName} as ${status}. Parents alerted!`);
  };

  // Calculations for reports & dashboard
  const activeRoute = routes.find(r => r.id === selectedRouteDocsId);
  const routeAllocatedCount = (routeDocsId) => allocations.filter(a => a.routeDocsId === routeDocsId).length;
  const todayAttendanceLogs = attendance.filter(a => a.date === new Date().toISOString().split("T")[0] && a.routeDocsId === selectedAttendanceRoute);
  
  const totalAllocatedStudents = allocations.length;
  const operationalVehicles = vehicles.filter(v => v.status === "Operational").length;

  // Render Sidebar and Role Filter
  const canModifyCatalog = user.role === "Super Admin" || user.role === "Principal" || user.role === "Transport Manager";

  // Parent Child Info Lookups
  const parentAllocations = allocations.filter(a => {
    const stDetail = studentsList.find(s => s.id === a.studentDocsId);
    return stDetail && (stDetail.parentEmail === user.email || user.role === "Parent");
  });

  return (
    <div className="space-y-6">
      {/* Title Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Bus className="h-6 w-6 text-blue-600" />
            MOD-14: Transport Management Desk
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Configure routes, track bus fleets, allocate day scholars, log boarding safety checklists, and simulate telemetry.
          </p>
        </div>

        {/* Action picker based on role */}
        <div className="flex gap-2">
          {canModifyCatalog && (
            <>
              <Button onClick={() => setIsAddRouteOpen(true)} className="text-xs py-2 bg-slate-900 border border-transparent shadow-xs">
                <Plus className="h-4 w-4" /> Add Route
              </Button>
              <Button onClick={() => setIsAddAllocOpen(true)} variant="outline" className="text-xs py-2 border-slate-200">
                Allocate Student
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Tab Navigation Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        {!isDriverRole && !isAccountantRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("dashboard")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "dashboard" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-805"
            }`}
          >
            Dashboard Overview
          </button>
        )}
        
        {!isDriverRole && !isAccountantRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("routes")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "routes" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Routes & Stops
          </button>
        )}

        {!isDriverRole && !isAccountantRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("fleet")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "fleet" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Vehicles & Crew
          </button>
        )}

        {!isDriverRole && !isAccountantRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("allocations")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "allocations" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Allocations Desk
          </button>
        )}

        {!isAccountantRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("attendance")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "attendance" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Boarding Check-In
          </button>
        )}

        {!isDriverRole && !isAccountantRole && (
          <button
            onClick={() => setActiveTab("livegps")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "livegps" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            GPS Live Simulation
          </button>
        )}

        {!isDriverRole && !isParentRole && (
          <button
            onClick={() => setActiveTab("ledger")}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "ledger" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-850"
            }`}
          >
            Transport Finance
          </button>
        )}
      </div>

      {/* ========================================================
          1. DASHBOARD OVERVIEW TAB
          ======================================================== */}
      {activeTab === "dashboard" && (
        <div className="space-y-6">
          {/* Metrics Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Total Routes</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{routes.length} Active</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Transit Fleet</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{operationalVehicles} / {vehicles.length} Active</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Allocated Day Scholars</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalAllocatedStudents} Enrolled</p>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Active Crew</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{drivers.length} Drivers</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Route Stats Lists */}
            <div className="md:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Active Routes Utilization</h3>
              <div className="divide-y divide-slate-50 pt-1">
                {routes.map(r => {
                  const count = routeAllocatedCount(r.id);
                  const activeVeh = vehicles.find(v => v.vehicleNo === r.vehicleNo);
                  const pct = Math.min(100, Math.round((count / (activeVeh?.capacity || 40)) * 100));
                  return (
                    <div key={r.id} className="py-3.5 flex items-center justify-between gap-4">
                      <div className="space-y-1">
                        <h4 className="text-xs font-bold text-slate-800">{r.routeNo}: {r.routeName}</h4>
                        <p className="text-[10px] text-slate-400 font-semibold">{r.startLocation} ➔ {r.endLocation}</p>
                      </div>
                      
                      <div className="w-44 space-y-1.5 shrink-0">
                        <div className="flex justify-between text-[10px] font-black font-mono">
                          <span className="text-slate-400">{count} pupils</span>
                          <span className={`${pct > 90 ? "text-rose-600" : "text-blue-600"}`}>{pct}% filled</span>
                        </div>
                        <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                          <div 
                            className={`h-full rounded-full transition-all ${pct > 90 ? "bg-rose-500" : "bg-blue-500"}`} 
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Notification logs panel */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
              <div className="flex items-center gap-1.5 border-b pb-3 border-slate-50">
                <Bell className="h-4 w-4 text-blue-600" />
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Parent Dispatch Logs</h3>
              </div>
              <div className="space-y-3.5 max-h-[300px] overflow-y-auto pr-1">
                {notificationsAlerts.map(alert => (
                  <div key={alert.id} className="p-3 bg-slate-50 border border-slate-100 rounded-2xl space-y-1.5">
                    <div className="flex justify-between items-center">
                      <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 text-[8px] font-black px-1.5 py-0.5 rounded uppercase font-mono">SMS Dispatched</span>
                      <span className="text-[9px] text-slate-400 font-bold font-mono">{alert.time}</span>
                    </div>
                    <p className="text-[10px] text-slate-500 leading-relaxed font-semibold italic">"{alert.msg}"</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. ROUTES & TIMELINES TAB
          ======================================================== */}
      {activeTab === "routes" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Left: Routes selection */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 h-[500px] overflow-y-auto">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Select Transit Line</h3>
            <div className="space-y-2 pt-1">
              {routes.map((r) => (
                <button
                  key={r.id}
                  onClick={() => setSelectedRouteDocsId(r.id)}
                  className={`w-full text-left p-4 rounded-2xl border transition ${
                    selectedRouteDocsId === r.id
                      ? "border-blue-500 bg-blue-50/10 shadow-xs" 
                      : "border-slate-150 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <Badge variant="secondary" className="text-[9px] font-black">{r.routeNo}</Badge>
                    {canModifyCatalog && (
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteRoute(r.id, r.routeNo);
                        }} 
                        className="text-slate-400 hover:text-rose-600 transition"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                  <h4 className="text-xs font-extrabold text-slate-800 mt-2">{r.routeName}</h4>
                  <p className="text-[10px] text-slate-400 mt-1 font-semibold">Distance: {r.distance_km} km</p>
                </button>
              ))}
            </div>
          </div>

          {/* Right: Vertical stops timeline */}
          <div className="md:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-5">
            {activeRoute ? (
              <>
                <div className="flex justify-between items-center border-b pb-4 border-slate-50">
                  <div>
                    <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">
                      {activeRoute.routeNo} Route Timeline & Stops
                    </h3>
                    <p className="text-xs text-slate-400 mt-1 font-semibold">
                      Sequenced halts mapping coordinate nodes for morning/evening runs.
                    </p>
                  </div>
                  {canModifyCatalog && (
                    <Button onClick={() => setIsAddStopOpen(true)} className="text-xs py-1.5">
                      <Plus className="h-3.5 w-3.5" /> Add Stop
                    </Button>
                  )}
                </div>

                {/* Timeline visualizer */}
                <div className="relative pl-6 space-y-6 pt-2">
                  <div className="absolute left-2.5 top-2.5 bottom-2.5 w-0.5 bg-slate-200" />
                  
                  {(!activeRoute.stops || activeRoute.stops.length === 0) ? (
                    <p className="text-xs text-slate-400 italic">No stops added. Add stop coordinates using the action button.</p>
                  ) : (
                    activeRoute.stops.map((stop, index) => (
                      <div key={index} className="relative flex items-start justify-between gap-4">
                        {/* Dot marker */}
                        <div className="absolute -left-[22px] top-1 h-3.5 w-3.5 rounded-full bg-blue-600 border-2 border-white ring-2 ring-blue-100" />
                        
                        <div className="space-y-1">
                          <h4 className="text-xs font-black text-slate-850">
                            Stop {stop.sequence}: {stop.name}
                          </h4>
                          <div className="flex gap-4.5 text-[10px] font-bold text-slate-450 font-mono">
                            <span className="flex items-center gap-1"><MapPin className="h-3 w-3" /> Lat: {stop.latitude}, Lng: {stop.longitude}</span>
                          </div>
                        </div>

                        {/* Timing labels */}
                        <div className="flex gap-2 shrink-0">
                          <span className="bg-emerald-50 text-emerald-850 text-[10px] font-bold px-2 py-0.5 rounded-md font-mono flex items-center gap-1">
                            <Clock className="h-3 w-3" /> AM {stop.pickupTime}
                          </span>
                          <span className="bg-amber-50 text-amber-850 text-[10px] font-bold px-2 py-0.5 rounded-md font-mono flex items-center gap-1">
                            <Clock className="h-3 w-3" /> PM {stop.dropTime}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (
              <p className="text-xs text-slate-400 italic">Please select or register a route first.</p>
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          3. VEHICLES & CREW TAB
          ======================================================== */}
      {activeTab === "fleet" && (
        <div className="space-y-6">
          {/* Vehicles list */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <div>
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Digital Fleet Management</h3>
                <p className="text-[11px] text-slate-400 font-semibold mt-1">Status logs of core boarding school buses.</p>
              </div>
              {canModifyCatalog && (
                <Button onClick={() => setIsAddVehicleOpen(true)} className="text-xs py-1.5">
                  <Plus className="h-3.5 w-3.5" /> Add Vehicle
                </Button>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-5 pt-1">
              {vehicles.map(veh => (
                <div key={veh.id} className="p-5 bg-slate-50 border border-slate-150 rounded-2xl space-y-3 hover:border-blue-400 transition">
                  <div className="flex justify-between items-center">
                    <h4 className="text-xs font-black text-slate-800">{veh.vehicleNo}</h4>
                    <Badge variant={veh.status === "Operational" ? "success" : "warning"} className="text-[9px] font-black">
                      {veh.status}
                    </Badge>
                  </div>
                  <div className="space-y-1 text-[11px] font-bold text-slate-500 font-mono">
                    <p>Capacity limit: {veh.capacity} Seats</p>
                    <p>Fuel Standard: {veh.fuelType}</p>
                    <p className="text-amber-700">Insurance Limit: {veh.insuranceExpiry}</p>
                    <p className="text-emerald-700">Fitness Expiry: {veh.fitnessExpiry}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Drivers catalog */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <div>
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Active Crew Profiles</h3>
                <p className="text-[11px] text-slate-400 font-semibold mt-1">Assigned licensing warnings and emergency contacts.</p>
              </div>
              {canModifyCatalog && (
                <Button onClick={() => setIsAddDriverOpen(true)} className="text-xs py-1.5">
                  <Plus className="h-3.5 w-3.5" /> Add Driver
                </Button>
              )}
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Driver Name</th>
                    <th className="p-3">Phone Line</th>
                    <th className="p-3">License Code</th>
                    <th className="p-3">License Expiration</th>
                    <th className="p-3">Assigned Vehicle</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {drivers.map(drv => {
                    const assignedVeh = vehicles.find(v => v.id === drv.vehicleDocsId);
                    
                    // Simple alerts triggers
                    const isExpiringSoon = new Date(drv.licenseExpiry) < new Date("2026-07-01");
                    
                    return (
                      <tr key={drv.id}>
                        <td className="p-3 font-extrabold text-slate-800">{drv.name}</td>
                        <td className="p-3 font-bold font-mono text-slate-450">{drv.phone}</td>
                        <td className="p-3 font-bold text-slate-500 font-mono">{drv.licenseNo}</td>
                        <td className="p-3">
                          <span className={`font-mono ${isExpiringSoon ? "text-rose-600 font-bold" : ""}`}>
                            {drv.licenseExpiry} 
                            {isExpiringSoon && <span className="ml-1 text-[9px] bg-rose-50 text-rose-700 px-1 py-0.5 rounded font-sans">Alert! Expiry</span>}
                          </span>
                        </td>
                        <td className="p-3">
                          <Badge variant="secondary" className="font-mono text-[9px]">
                            {assignedVeh ? assignedVeh.vehicleNo : "None Assigned"}
                          </Badge>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          4. STUDENT ALLOCATIONS DESK
          ======================================================== */}
      {activeTab === "allocations" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
          <div className="flex justify-between items-center border-b pb-3 border-slate-50">
            <div>
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Active Pupil Route Allocations</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Student roster mapped to route timelines and pickup milestones.</p>
            </div>
          </div>

          <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
            <table className="w-full text-xs text-slate-700 font-semibold text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Scholar Student</th>
                  <th className="p-3">Roster Grade</th>
                  <th className="p-3">Assigned Route</th>
                  <th className="p-3">Pickup Stop</th>
                  <th className="p-3">Drop-off Stop</th>
                  <th className="p-3 text-right">Fee Rate</th>
                  <th className="p-3">StartDate</th>
                  <th className="p-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {allocations.map(alloc => {
                  const studentInfo = studentsList.find(s => s.id === alloc.studentDocsId);
                  const routeInfo = routes.find(r => r.id === alloc.routeDocsId);
                  
                  return (
                    <tr key={alloc.id}>
                      <td className="p-3 font-extrabold text-slate-800">{alloc.studentName}</td>
                      <td className="p-3 font-bold text-slate-450">{studentInfo ? studentInfo.grade : "Grade 7"}</td>
                      <td className="p-3 text-slate-500 font-bold font-mono">
                        {routeInfo ? `${routeInfo.routeNo} (${routeInfo.routeName})` : "N/A"}
                      </td>
                      <td className="p-3 text-slate-500 font-bold">{alloc.pickupStopName}</td>
                      <td className="p-3 text-slate-500 font-bold">{alloc.dropStopName}</td>
                      <td className="p-3 text-right font-mono font-bold text-blue-700">${alloc.feeAmount}</td>
                      <td className="p-3 text-slate-400 font-semibold font-mono">{alloc.startDate}</td>
                      <td className="p-3">
                        <Badge variant="success" className="text-[9px] font-black">{alloc.status}</Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================
          5. DAILY BOARDING ATTENDANCE CHECKLIST
          ======================================================== */}
      {activeTab === "attendance" && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-5">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b pb-4 border-slate-50">
            <div>
              <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">
                Daily Boarding Safety Roster Check-In
              </h3>
              <p className="text-xs text-slate-400 mt-1 font-semibold">
                Single-tap status logging dispatching automatic alerts to student parent contacts.
              </p>
            </div>

            <div className="w-56 shrink-0">
              <Select
                options={routes.map(r => ({ label: `${r.routeNo} - ${r.routeName}`, value: r.id }))}
                value={selectedAttendanceRoute}
                onChange={(e) => setSelectedAttendanceRoute(e.target.value)}
                className="text-xs py-1.5 h-8.5 rounded-xl bg-slate-50"
              />
            </div>
          </div>

          {/* Student Roster attendance items */}
          <div className="space-y-3">
            {allocations.filter(a => a.routeDocsId === selectedAttendanceRoute).length === 0 ? (
              <p className="text-xs text-slate-400 italic p-4 text-center">No students allocated to this transit line.</p>
            ) : (
              allocations.filter(a => a.routeDocsId === selectedAttendanceRoute).map((alloc) => {
                const log = todayAttendanceLogs.find(l => l.studentDocsId === alloc.studentDocsId);
                const activeStatus = log ? log.status : "Pending";
                
                return (
                  <div key={alloc.studentDocsId} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:border-slate-300 transition">
                    <div className="space-y-1">
                      <h4 className="text-xs font-black text-slate-800">{alloc.studentName}</h4>
                      <p className="text-[10px] text-slate-400 font-bold">Halt Stop: {alloc.pickupStopName} ➔ {alloc.dropStopName}</p>
                    </div>

                    {/* Check-in controls */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleToggleAttendance(alloc.studentDocsId, "Boarded")}
                        className={`px-3 py-1.5 rounded-xl text-[10px] font-black uppercase cursor-pointer transition ${
                          activeStatus === "Boarded" 
                            ? "bg-emerald-600 text-white shadow-xs" 
                            : "bg-white text-emerald-600 border border-emerald-100 hover:bg-emerald-50/20"
                        }`}
                      >
                        Boarded
                      </button>
                      <button
                        onClick={() => handleToggleAttendance(alloc.studentDocsId, "Missed")}
                        className={`px-3 py-1.5 rounded-xl text-[10px] font-black uppercase cursor-pointer transition ${
                          activeStatus === "Missed" 
                            ? "bg-amber-500 text-white shadow-xs" 
                            : "bg-white text-amber-600 border border-amber-100 hover:bg-amber-50/20"
                        }`}
                      >
                        Missed
                      </button>
                      <button
                        onClick={() => handleToggleAttendance(alloc.studentDocsId, "Absent")}
                        className={`px-3 py-1.5 rounded-xl text-[10px] font-black uppercase cursor-pointer transition ${
                          activeStatus === "Absent" 
                            ? "bg-slate-500 text-white shadow-xs" 
                            : "bg-white text-slate-500 border border-slate-150 hover:bg-slate-100"
                        }`}
                      >
                        Absent
                      </button>

                      {log && (
                        <span className="text-[9px] font-mono text-slate-400 font-black ml-2">
                          Logged: {log.timestamp}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          6. LIVE GPS SIMULATOR MAP TAB
          ======================================================== */}
      {activeTab === "livegps" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Left panel: Simulation telemetry */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Bus Live Telemetry</h3>
            
            <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-3 pt-3">
              <div className="flex justify-between items-center font-mono text-xs font-bold text-slate-655">
                <span>Active Bus:</span>
                <span className="text-blue-700">BUS-402X (RT-ALPHA)</span>
              </div>
              <div className="flex justify-between items-center font-mono text-xs font-bold text-slate-655">
                <span>Current Velocity:</span>
                <span className="text-emerald-700">{currentSpeed} km/h</span>
              </div>
              <div className="flex justify-between items-center font-mono text-xs font-bold text-slate-655">
                <span>Est. Time Arrival (ETA):</span>
                <span className="text-amber-700">{currentETA} Minutes</span>
              </div>
              <div className="flex justify-between items-center font-mono text-xs font-bold text-slate-655">
                <span>Total Mileage:</span>
                <span>14.5 Kilometers</span>
              </div>
            </div>

            {/* Geofence warning */}
            {geofenceAlert && (
              <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-2xl flex gap-2.5 items-start">
                <AlertTriangle className="h-5 w-5 shrink-0 text-rose-500 animate-bounce" />
                <div>
                  <h4 className="text-[11px] font-black uppercase">Geofence Warning</h4>
                  <p className="text-[10px] mt-0.5 font-bold">Bus has crossed 1km boundary perimeter circle. Arriving shortly.</p>
                </div>
              </div>
            )}

            {/* Simulation controls */}
            <div className="pt-2">
              <Button
                onClick={() => {
                  if (busProgress >= 100) {
                    setBusProgress(0);
                  }
                  setSimulating(!simulating);
                }}
                className="w-full text-xs font-black py-2.5 flex justify-center gap-2 shadow-xs cursor-pointer"
              >
                {simulating ? <><Pause className="h-4 w-4" /> Pause Simulation</> : <><Play className="h-4 w-4" /> Run GPS Simulation</>}
              </Button>
            </div>
          </div>

          {/* Right panel: Animated SVG City Map visual */}
          <div className="md:col-span-2 bg-slate-900 border border-slate-950 rounded-3xl p-6 text-slate-100 flex flex-col justify-between h-[450px]">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-xs font-black uppercase tracking-widest text-slate-300">Live Route Map Visualization</h3>
                <p className="text-[10px] text-slate-500 font-semibold mt-1">Simulated map showing real-time GPS coordinate coordinates progress.</p>
              </div>
              <Badge className="bg-slate-800 text-slate-200 border-slate-700 text-[9px] font-black uppercase font-mono">
                Progress: {Math.round(busProgress)}%
              </Badge>
            </div>

            {/* Simulated Grid SVG */}
            <div className="flex-1 my-6 border border-slate-800 bg-slate-950 rounded-2xl relative overflow-hidden flex items-center justify-center">
              {/* Grid Background lines */}
              <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-slate-900 via-slate-950 to-slate-950 opacity-40" />
              
              <svg className="w-full h-full p-4" viewBox="0 0 400 200">
                {/* City Blocks outline */}
                <rect x="20" y="20" width="80" height="60" rx="6" fill="#1e293b" opacity="0.4" />
                <rect x="120" y="20" width="160" height="60" rx="6" fill="#1e293b" opacity="0.4" />
                <rect x="300" y="20" width="80" height="60" rx="6" fill="#1e293b" opacity="0.4" />
                <rect x="20" y="120" width="180" height="60" rx="6" fill="#1e293b" opacity="0.4" />
                <rect x="220" y="120" width="160" height="60" rx="6" fill="#1e293b" opacity="0.4" />

                {/* Route Path Line */}
                <path 
                  id="route-path" 
                  d="M 30,100 L 100,100 L 100,45 L 290,45 L 290,100 L 370,100" 
                  fill="none" 
                  stroke="#334155" 
                  strokeWidth="6" 
                  strokeLinecap="round" 
                />
                
                {/* Active path indicator */}
                <path 
                  d="M 30,100 L 100,100 L 100,45 L 290,45 L 290,100 L 370,100" 
                  fill="none" 
                  stroke="#3b82f6" 
                  strokeWidth="4" 
                  strokeLinecap="round"
                  strokeDasharray="400"
                  strokeDashoffset={400 - (400 * (busProgress / 100))}
                  className="transition-all duration-200"
                />

                {/* Stop dots */}
                <circle cx="30" cy="100" r="5" fill="#10b981" />
                <circle cx="100" cy="70" r="5" fill="#3b82f6" />
                <circle cx="210" cy="45" r="5" fill="#3b82f6" />
                <circle cx="290" cy="70" r="5" fill="#3b82f6" />
                <circle cx="370" cy="100" r="5" fill="#ef4444" />

                {/* Text Labels */}
                <text x="35" y="115" fill="#94a3b8" fontSize="8" fontWeight="bold">Pines Term.</text>
                <text x="110" y="65" fill="#94a3b8" fontSize="8" fontWeight="bold">Square</text>
                <text x="215" y="40" fill="#94a3b8" fontSize="8" fontWeight="bold">Valley</text>
                <text x="315" y="90" fill="#f87171" fontSize="8" fontWeight="bold">St. Jude Gate</text>

                {/* Bus Marker along the path */}
                {/* Simply interpolating coords based on progress percentage ranges */}
                {(() => {
                  let x = 30;
                  let y = 100;
                  const p = busProgress;

                  if (p <= 20) { // Segment 1: M 30,100 to 100,100 (diff dx = 70)
                    x = 30 + (70 * (p / 20));
                    y = 100;
                  } else if (p <= 40) { // Segment 2: 100,100 to 100,45 (diff dy = -55)
                    x = 100;
                    y = 100 - (55 * ((p - 20) / 20));
                  } else if (p <= 70) { // Segment 3: 100,45 to 290,45 (diff dx = 190)
                    x = 100 + (190 * ((p - 40) / 30));
                    y = 45;
                  } else if (p <= 85) { // Segment 4: 290,45 to 290,100 (diff dy = 55)
                    x = 290;
                    y = 45 + (55 * ((p - 70) / 15));
                  } else { // Segment 5: 290,100 to 370,100 (diff dx = 80)
                    x = 290 + (80 * ((p - 85) / 15));
                    y = 100;
                  }

                  return (
                    <g transform={`translate(${x}, ${y})`}>
                      <circle cx="0" cy="0" r="8" fill="#fbbf24" stroke="#d97706" strokeWidth="2" className="animate-ping opacity-75" />
                      <circle cx="0" cy="0" r="7" fill="#f59e0b" stroke="#b45309" strokeWidth="2" />
                      {/* Inner bus shape representation */}
                      <rect x="-3.5" y="-2.5" width="7" height="5" fill="#ffffff" rx="1" />
                    </g>
                  );
                })()}
              </svg>
            </div>

            <div className="flex gap-4.5 justify-center items-center text-[10px] font-mono text-slate-500 font-black tracking-widest uppercase border-t border-slate-800/60 pt-3">
              <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-emerald-500" /> Start</span>
              <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-blue-500" /> Waypoints</span>
              <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-rose-500" /> School Campus</span>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          7. TRANSPORT FINANCE LEDGER TAB
          ======================================================== */}
      {activeTab === "ledger" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Revenue metrics chart summaries */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Financial Summary</h3>
            <div className="pt-2 space-y-4.5">
              <div className="p-4 bg-blue-50/20 border border-blue-100 rounded-2xl">
                <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Total Monthly Ledger Billing</span>
                <p className="text-xl font-black text-blue-700 mt-1">$620.00</p>
              </div>
              <div className="p-4 bg-emerald-50/20 border border-emerald-100 rounded-2xl">
                <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Collected Collections (This Month)</span>
                <p className="text-xl font-black text-emerald-700 mt-1">$500.00</p>
              </div>
              <div className="p-4 bg-rose-50/20 border border-rose-100 rounded-2xl">
                <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Outstanding Uncollected Balance</span>
                <p className="text-xl font-black text-rose-700 mt-1">$120.00</p>
              </div>
            </div>
          </div>

          {/* Ledger logs collections list */}
          <div className="md:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Outstanding Transport Balances</h3>
            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Scholar Pupil Name</th>
                    <th className="p-3">Assigned Route</th>
                    <th className="p-3">Stop Location</th>
                    <th className="p-3 text-right">Fee Dues</th>
                    <th className="p-3">Billing Cycle</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {allocations.map((alloc) => {
                    const route = routes.find(r => r.id === alloc.routeDocsId);
                    return (
                      <tr key={alloc.id}>
                        <td className="p-3 font-extrabold text-slate-800">{alloc.studentName}</td>
                        <td className="p-3 font-mono font-bold text-slate-450">{route ? route.routeNo : "N/A"}</td>
                        <td className="p-3 font-bold text-slate-500">{alloc.pickupStopName}</td>
                        <td className="p-3 text-right font-mono font-black text-rose-600">${alloc.feeAmount}</td>
                        <td className="p-3 text-slate-400 font-semibold font-mono">Monthly June 2026</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          FORM DIALOGS
          ======================================================== */}
      {/* 1. ADD ROUTE DIALOG */}
      <Dialog
        isOpen={isAddRouteOpen}
        onClose={() => setIsAddRouteOpen(false)}
        title="Register New Route"
      >
        <form onSubmit={handleCreateRoute} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Route Code"
              value={routeNo}
              onChange={(e) => setRouteNo(e.target.value)}
              placeholder="e.g. RT-EPSILON"
              required
            />
            <Input
              label="Route Name"
              value={routeName}
              onChange={(e) => setRouteName(e.target.value)}
              placeholder="e.g. Westside Hills Hub"
              required
            />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <Input
              label="Start point location"
              value={routeStart}
              onChange={(e) => setRouteStart(e.target.value)}
              placeholder="e.g. West Terminal"
            />
            <Input
              label="Destination gate"
              value={routeEnd}
              onChange={(e) => setRouteEnd(e.target.value)}
              placeholder="e.g. Academic Gate"
            />
            <Input
              label="Distance (km)"
              type="number"
              value={routeDist}
              onChange={(e) => setRouteDist(e.target.value)}
              placeholder="12.5"
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddRouteOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Finalize Route Setup</Button>
          </div>
        </form>
      </Dialog>

      {/* 2. ADD STOP DIALOG */}
      <Dialog
        isOpen={isAddStopOpen}
        onClose={() => setIsAddStopOpen(false)}
        title="Register Halt Stop Coordinate"
      >
        <form onSubmit={handleAddStop} className="space-y-4 pt-1">
          <Input
            label="Stop Name"
            value={stopName}
            onChange={(e) => setStopName(e.target.value)}
            placeholder="e.g. Union Green Square"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="AM Pickup Timing"
              type="time"
              value={stopPickupTime}
              onChange={(e) => setStopPickupTime(e.target.value)}
              required
            />
            <Input
              label="PM Drop-off Timing"
              type="time"
              value={stopDropTime}
              onChange={(e) => setStopDropTime(e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Stop Latitude (Decimal)"
              value={stopLat}
              onChange={(e) => setStopLat(e.target.value)}
              required
            />
            <Input
              label="Stop Longitude (Decimal)"
              value={stopLng}
              onChange={(e) => setStopLng(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddStopOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Register Stop</Button>
          </div>
        </form>
      </Dialog>

      {/* 3. ADD VEHICLE DIALOG */}
      <Dialog
        isOpen={isAddVehicleOpen}
        onClose={() => setIsAddVehicleOpen(false)}
        title="Register Vehicle Asset"
      >
        <form onSubmit={handleCreateVehicle} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Registration Plate No."
              value={vehNo}
              onChange={(e) => setVehNo(e.target.value)}
              placeholder="e.g. COACH-99Z"
              required
            />
            <Input
              label="Seating Capacity limit"
              type="number"
              value={vehCapacity}
              onChange={(e) => setVehCapacity(e.target.value)}
              placeholder="40"
              required
            />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <Select
              label="Fuel Source type"
              options={[
                { label: "Diesel Standard", value: "Diesel" },
                { label: "CNG Ecological", value: "CNG" },
                { label: "Electric Battery", value: "Electric" }
              ]}
              value={vehFuel}
              onChange={(e) => setVehFuel(e.target.value)}
            />
            <Input
              label="Insurance Expiration"
              type="date"
              value={vehInsurance}
              onChange={(e) => setVehInsurance(e.target.value)}
              required
            />
            <Input
              label="Road Fitness Expiry"
              type="date"
              value={vehFitness}
              onChange={(e) => setVehFitness(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddVehicleOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Finalize Registration</Button>
          </div>
        </form>
      </Dialog>

      {/* 4. ADD DRIVER DIALOG */}
      <Dialog
        isOpen={isAddDriverOpen}
        onClose={() => setIsAddDriverOpen(false)}
        title="Add Driver Profile"
      >
        <form onSubmit={handleCreateDriver} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Driver Name"
              value={drvName}
              onChange={(e) => setDrvName(e.target.value)}
              placeholder="e.g. David Hasselhoff"
              required
            />
            <Input
              label="Contact Phone Line"
              value={drvPhone}
              onChange={(e) => setDrvPhone(e.target.value)}
              placeholder="555-829-1029"
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Commercial License Code"
              value={drvLicense}
              onChange={(e) => setDrvLicense(e.target.value)}
              placeholder="DL-381920-X"
              required
            />
            <Input
              label="License Expiry Calendar"
              type="date"
              value={drvExpiry}
              onChange={(e) => setDrvExpiry(e.target.value)}
              required
            />
          </div>
          <Select
            label="Assigned Vehicle (Asset)"
            options={[
              { label: "None Assigned", value: "" },
              ...vehicles.map(v => ({ label: v.vehicleNo, value: v.id }))
            ]}
            value={drvVehId}
            onChange={(e) => setDrvVehId(e.target.value)}
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddDriverOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Create Profile</Button>
          </div>
        </form>
      </Dialog>

      {/* 5. STUDENT ALLOCATION DIALOG */}
      <Dialog
        isOpen={isAddAllocOpen}
        onClose={() => setIsAddAllocOpen(false)}
        title="Allocate Scholar Route Assignment"
      >
        <form onSubmit={handleCreateAllocation} className="space-y-4 pt-1">
          <Select
            label="Select Scholar Student"
            options={[
              { label: "Select pupil...", value: "" },
              ...studentsList.map(s => ({ label: `${s.name} (${s.admissionNo})`, value: s.id }))
            ]}
            value={allocStudentDocsId}
            onChange={(e) => setAllocStudentDocsId(e.target.value)}
            required
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Select
              label="Select Target Route"
              options={[
                { label: "Select transit line...", value: "" },
                ...routes.map(r => ({ label: `${r.routeNo}: ${r.routeName}`, value: r.id }))
              ]}
              value={allocRouteDocsId}
              onChange={(e) => setAllocRouteDocsId(e.target.value)}
              required
            />
            <Input
              label="Billing Fee Amount ($)"
              type="number"
              value={allocFee}
              onChange={(e) => setAllocFee(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Morning Pickup stop name"
              value={allocPickupStop}
              onChange={(e) => setAllocPickupStop(e.target.value)}
              placeholder="e.g. Cedar Falls Hub"
            />
            <Input
              label="Evening Drop stop name"
              value={allocDropStop}
              onChange={(e) => setAllocDropStop(e.target.value)}
              placeholder="e.g. Cedar Falls Hub"
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAddAllocOpen(false)}>Cancel</Button>
            <Button type="submit" className="bg-slate-900 text-white font-extrabold">Finalize Allocation</Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
