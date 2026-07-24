/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getCameras,
  saveCameras,
  getCameraGroups,
  saveCameraGroups,
  getCameraAssignments,
  saveCameraAssignments,
  getSecurityIncidents,
  saveSecurityIncidents,
  getCameraRecordings,
  saveCameraRecordings,
  getStudents,
  getStaff,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Video,
  VideoOff,
  ShieldAlert,
  Layers,
  Search,
  Plus,
  CheckCircle,
  Settings,
  MapPin,
  Activity,
  Clock,
  UserCheck,
  Map,
  AlertTriangle,
  Grid,
  Tv,
  Eye,
  RefreshCw,
  ShieldCheck,
  Maximize2,
  Trash2,
  Lock,
  Camera,
  Play,
  Volume2
} from "lucide-react";

export default function ModCctv({ user }) {
  const { addToast } = useToast();

  // State databases
  const [cameras, setCameras] = useState(() => getCameras());
  const [groups, setGroups] = useState(() => getCameraGroups());
  const [assignments, setAssignments] = useState(() => getCameraAssignments());
  const [incidents, setIncidents] = useState(() => getSecurityIncidents());
  const [recordings, setRecordings] = useState(() => getCameraRecordings());

  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());

  // Navigation tab
  // Options: dashboard, live_streams, manager, bus_transit, ai_attendance, incidents
  const [activeTab, setActiveTab] = useState("dashboard");

  // Modal display toggles
  const [isRegisterCamOpen, setIsRegisterCamOpen] = useState(false);
  const [isRaiseIncidentOpen, setIsRaiseIncidentOpen] = useState(false);
  const [isAssignCamOpen, setIsAssignCamOpen] = useState(false);

  // Form states
  // 1. Camera Registration
  const [newCamName, setNewCamName] = useState("");
  const [newCamType, setNewCamType] = useState("IP RTSP");
  const [newCamLoc, setNewCamLoc] = useState("");
  const [newCamUrl, setNewCamUrl] = useState("");

  // 2. Incident Form
  const [incType, setIncType] = useState("Unauthorized Entry");
  const [incSeverity, setIncSeverity] = useState("Medium");
  const [incDesc, setIncDesc] = useState("");
  const [incCamId, setIncCamId] = useState("");

  // 3. Camera Assignment Form
  const [assignCamId, setAssignCamId] = useState("");
  const [assignGrade, setAssignGrade] = useState("Grade 10");
  const [assignSection, setAssignSection] = useState("A");

  // Filter/Search states
  const [cameraSearch, setCameraSearch] = useState("");
  const [cameraFilter, setCameraFilter] = useState("All");

  const [incidentSearch, setIncidentSearch] = useState("");
  const [incidentFilter, setIncidentFilter] = useState("All");

  // Live Stream Player States
  const [selectedCamId, setSelectedCamId] = useState("cam-205");
  const [streamQuality, setStreamQuality] = useState("1080p");
  const [isMuted, setIsMuted] = useState(true);
  const [isLivePaused, setIsLivePaused] = useState(false);
  const [parentSessionsCount, setParentSessionsCount] = useState(14);

  // Bus Transit State Simulation
  const [selectedBusId, setSelectedBusId] = useState("cam-bus-12f");
  const [gpsProgress, setGpsProgress] = useState(0);

  // AI Face Attendance State Simulation
  const [aiClassNo, setAiClassNo] = useState("10-A");
  const [aiScannerState, setAiScannerState] = useState("idle"); // idle, scanning, complete
  const [scannedNames, setScannedNames] = useState([]);

  // Mock parent viewing rules configuration
  const isParent = user?.role === "Parent";
  const isPrincipalOrAdmin = user?.role === "Super Admin" || user?.role === "Principal";
  const isSecurityManager = user?.role === "Super Admin" || user?.role === "Warden" || user?.role === "Principal";

  useEffect(() => {
    if (cameras.length > 0 && !selectedCamId) {
      setSelectedCamId(cameras[0].id);
    }
  }, [cameras]);

  useEffect(() => {
    if (cameras.length > 0) {
      setIncCamId(cameras[0].id);
      setAssignCamId(cameras[0].id);
    }
  }, [cameras]);

  // Periodic parent session count simulation
  useEffect(() => {
    const interval = setInterval(() => {
      setParentSessionsCount(prev => prev + (Math.random() > 0.5 ? 1 : -1));
    }, 4000);
    return () => clearInterval(interval);
  }, []);

  // Periodic GPS school bus route progress simulation
  useEffect(() => {
    const interval = setInterval(() => {
      setGpsProgress(prev => (prev >= 100 ? 0 : prev + 10));
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  // CCTV KPIs
  const totalCamsCount = cameras.length;
  const onlineCamsCount = cameras.filter(c => c.status === "Online").length;
  const offlineCamsCount = cameras.filter(c => c.status === "Offline").length;
  const activeAlertsCount = incidents.filter(i => i.status !== "Resolved").length;

  // HANDLERS
  // 1. Register Camera
  const handleRegisterCamera = (e) => {
    e.preventDefault();
    if (!newCamName || !newCamLoc || !newCamUrl) {
      addToast("Failed to Register", "Please enter all camera particulars.", "error");
      return;
    }

    const newCam = {
      id: `cam-${Date.now()}`,
      name: newCamName,
      cameraType: newCamType,
      location: newCamLoc,
      streamUrl: newCamUrl,
      status: "Online"
    };

    const nextCams = [...cameras, newCam];
    setCameras(nextCams);
    saveCameras(nextCams);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "CCTV Camera Registered",
      `Added camera: "${newCamName}" at location: ${newCamLoc}`
    );

    addToast("Camera Active", `"${newCamName}" successfully mapped and active.`);
    setIsRegisterCamOpen(false);

    setNewCamName("");
    setNewCamLoc("");
    setNewCamUrl("");
  };

  // 2. Toggle Camera Online/Offline Status
  const handleToggleCamHealth = (cameraDocsId) => {
    const updated = cameras.map(c => {
      if (c.id === cameraDocsId) {
        const nextStatus = c.status === "Online" ? "Offline" : "Online";
        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          "CCTV Health Toggled",
          `Camera: ${c.name} health marked as ${nextStatus}`
        );
        addToast("Camera Status Updated", `"${c.name}" is now marked as ${nextStatus}.`);
        return { ...c, status: nextStatus };
      }
      return c;
    });
    setCameras(updated);
    saveCameras(updated);
  };

  // 3. Assign Camera to Section
  const handleAssignCamera = (e) => {
    e.preventDefault();
    if (!assignCamId) return;

    const cam = cameras.find(c => c.id === assignCamId);
    const newAsg = {
      id: `asg-${Date.now()}`,
      cameraDocsId: assignCamId,
      gradeNo: assignGrade,
      classNo: `${assignGrade.split(" ")[1]}-${assignSection}`,
      sectionNo: assignSection
    };

    const nextAsgs = [...assignments, newAsg];
    setAssignments(nextAsgs);
    saveCameraAssignments(nextAsgs);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Camera Mapping Assigned",
      `Assigned camera "${cam?.name}" to section: ${newAsg.classNo}`
    );

    addToast("Camera Mapped", `"${cam?.name}" successfully bound to classroom ${newAsg.classNo}.`);
    setIsAssignCamOpen(false);
  };

  // 4. Raise Incident Alert
  const handleRaiseIncident = (e) => {
    e.preventDefault();
    if (!incDesc) {
      addToast("Failed to Raise", "Incident description is required.", "error");
      return;
    }

    const cam = cameras.find(c => c.id === incCamId);
    const newInc = {
      id: `inc-${Date.now()}`,
      cameraDocsId: incCamId,
      incidentType: incType,
      severity: incSeverity,
      description: incDesc,
      status: "Detection",
      detectedAt: new Date().toISOString().replace("T", " ").substring(0, 19)
    };

    const nextIncs = [newInc, ...incidents];
    setIncidents(nextIncs);
    saveSecurityIncidents(nextIncs);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Security Incident Logged",
      `Raised ${incType} alarm from camera: ${cam?.name}. Severity: ${incSeverity}`
    );

    addToast("Alert Dispatched", `Dispatched incident alert to emergency security cabinets.`, "warning");
    setIsRaiseIncidentOpen(false);
    setIncDesc("");
  };

  // 5. Update Incident Workflow State
  const handleUpdateIncidentStatus = (incidentId, nextState) => {
    const updated = incidents.map(inc => {
      if (inc.id === incidentId) {
        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          "Incident Workflow Shift",
          `Incident type ${inc.incidentType} transitioned state to: ${nextState}`
        );
        addToast("Incident Status Updated", `Incident status shifted to ${nextState}.`);
        return { ...inc, status: nextState };
      }
      return inc;
    });
    setIncidents(updated);
    saveSecurityIncidents(updated);
  };

  // 6. Delete Camera
  const handleDeleteCamera = (cameraDocsId) => {
    const updated = cameras.filter(c => c.id !== cameraDocsId);
    setCameras(updated);
    saveCameras(updated);
    addToast("Camera Deregistered", "Successfully removed camera from database console.");
  };

  // AI Face Attendance Scanner Simulator
  const handleRunAiAttendance = () => {
    setAiScannerState("scanning");
    setScannedNames([]);

    setTimeout(() => {
      setScannedNames(["Liam Smith", "Noah Johnson", "Gianna Patel"]);
      setAiScannerState("complete");

      addToast("AI Attendance Success", "Verified 3 matching student profiles.", "success");
      logAction(
        user?.id || "sandbox",
        user?.name || "User",
        user?.role || "Staff",
        "AI Face Verification Marked",
        `Marked student class attendance for Grade 10-A via CCTV face detection.`
      );
    }, 2500);
  };

  // Filter lists based on role and selections
  // If Parent role, enforce viewing filters: Only see assigned classroom & school bus
  const getAccessibleCameras = () => {
    if (isParent) {
      // Parent can only view class 10-A camera (cam-205) and bus passenger cams
      return cameras.filter(c => c.id === "cam-205" || c.id.includes("bus-12"));
    }
    return cameras;
  };

  const accessibleCams = getAccessibleCameras();

  const filteredCams = accessibleCams.filter(c => {
    const matchesSearch = c.name.toLowerCase().includes(cameraSearch.toLowerCase()) ||
                          c.location.toLowerCase().includes(cameraSearch.toLowerCase());
    const matchesFilter = cameraFilter === "All" ||
                          (cameraFilter === "Online" && c.status === "Online") ||
                          (cameraFilter === "Offline" && c.status === "Offline");
    return matchesSearch && matchesFilter;
  });

  const filteredIncidents = incidents.filter(inc => {
    const cam = cameras.find(c => c.id === inc.cameraDocsId);
    const matchesSearch = inc.incidentType.toLowerCase().includes(incidentSearch.toLowerCase()) ||
                          inc.description.toLowerCase().includes(incidentSearch.toLowerCase()) ||
                          (cam && cam.name.toLowerCase().includes(incidentSearch.toLowerCase()));

    const matchesFilter = incidentFilter === "All" || inc.status === incidentFilter;
    return matchesSearch && matchesFilter;
  });

  const currentCam = cameras.find(c => c.id === selectedCamId) || cameras[0];
  const isPrivacyProtected = currentCam?.location.includes("Restricted") || currentCam?.location.includes("Bathroom");

  return (
    <div className="space-y-6">
      {/* 1. Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Video className="h-6 w-6 text-blue-650 animate-pulse" />
            MOD-19: Smart Campus Surveillance & CCTV Management
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Real-time IP/RTSP streams, privacy-shielded hostel corridors, parent live portals, and AI face recognition attendance validations.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-slate-50 border border-slate-150 px-3 py-1.5 rounded-xl">
          <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
            ROLE: {user?.role || "Visitor"}
          </Badge>
        </div>
      </div>

      {/* 2. Tabs Navigation Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "dashboard" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Security Dashboard
        </button>
        <button
          onClick={() => setActiveTab("live_streams")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "live_streams" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Live Stream Player
        </button>
        {!isParent && (
          <button
            onClick={() => setActiveTab("manager")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "manager" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            Camera Management
          </button>
        )}
        <button
          onClick={() => setActiveTab("bus_transit")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "bus_transit" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          School Bus Streams
        </button>
        {!isParent && (
          <button
            onClick={() => setActiveTab("ai_attendance")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "ai_attendance" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            AI Face Attendance
          </button>
        )}
        {!isParent && (
          <button
            onClick={() => setActiveTab("incidents")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "incidents" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            Incident Investigation ({activeAlertsCount})
          </button>
        )}
      </div>

      {/* ========================================================
          1. SECURITY DASHBOARD TAB
          ======================================================== */}
      {activeTab === "dashboard" && (
        <div className="space-y-6">
          {/* Metrics Grid */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Total Cameras Connected</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalCamsCount} Devices</p>
              <span className="text-[10px] text-emerald-650 font-semibold block mt-1">✓ {onlineCamsCount} Cameras Online</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Offline Cams alert</span>
              <p className={`text-xl font-bold mt-1 ${offlineCamsCount > 0 ? "text-rose-600" : "text-slate-800"}`}>
                {offlineCamsCount} Alerts
              </p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">NVR network diagnostics active</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Parent View Sessions</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{parentSessionsCount} Active</p>
              <span className="text-[10px] text-blue-600 font-semibold block mt-1">E2E secure watermarked streams</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Active incidents log</span>
              <p className={`text-xl font-bold mt-1 ${activeAlertsCount > 0 ? "text-amber-600 animate-pulse" : "text-slate-850"}`}>
                {activeAlertsCount} Reports
              </p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">investigations dispatched</span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Live Security Monitor Grid */}
            <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
              <div className="flex justify-between items-center border-b pb-3 border-slate-50">
                <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest flex items-center gap-1.5">
                  <Grid className="h-4 w-4 text-blue-650" /> CCTV Multi-Camera Matrix View
                </h3>
                <span className="text-[9px] font-mono font-bold text-rose-500 uppercase tracking-widest flex items-center gap-1">
                  <span className="h-2 w-2 rounded-full bg-rose-500 animate-ping" /> LIVE MATRIX
                </span>
              </div>

              {/* 2x2 Camera stream grid */}
              <div className="grid grid-cols-2 gap-4">
                {cameras.slice(0, 4).map(cam => {
                  const isBath = cam.location.includes("Restricted");
                  return (
                    <div
                      key={cam.id}
                      onClick={() => {
                        setSelectedCamId(cam.id);
                        setActiveTab("live_streams");
                      }}
                      className="bg-slate-900 aspect-video rounded-2xl overflow-hidden border border-slate-850 hover:border-blue-500 cursor-pointer relative group transition duration-300"
                    >
                      {/* Scanlines and flicker simulation */}
                      <div className="absolute inset-0 bg-scanlines opacity-10 pointer-events-none" />
                      <div className="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-transparent opacity-60 pointer-events-none" />

                      {isBath ? (
                        <div className="h-full w-full flex flex-col items-center justify-center text-center p-4">
                          <Lock className="h-6 w-6 text-rose-500 mb-1" />
                          <span className="text-[9px] font-bold text-rose-400 uppercase tracking-wider leading-relaxed">
                            Privacy Shield Enabled
                          </span>
                        </div>
                      ) : (
                        <div className="h-full w-full bg-slate-950 flex items-center justify-center relative">
                          {/* Mock CCTV stream noise */}
                          <div className="absolute inset-0 opacity-[0.04] bg-noise" />
                          <Tv className="h-8 w-8 text-slate-800" />
                          
                          <div className="absolute top-2 left-2 flex items-center gap-1.5 bg-black/60 px-2 py-0.5 rounded-lg text-[8px] font-bold text-white font-mono uppercase tracking-wider">
                            <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-pulse" />
                            {cam.name}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* AI Warning Alerts Timeline */}
            <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
              <h3 className="text-xs font-black text-rose-800 uppercase tracking-widest border-b pb-3 border-slate-50 flex items-center gap-1.5">
                <ShieldAlert className="h-4 w-4 text-rose-600 animate-bounce" /> AI Real-time Safety Feed
              </h3>

              <div className="space-y-3 max-h-[320px] overflow-y-auto pr-1">
                <div className="p-3 bg-rose-50/20 border border-rose-250 rounded-xl space-y-1.5">
                  <div className="flex justify-between items-center text-[9px] font-black uppercase text-rose-700">
                    <span>Fight Detection Alarm</span>
                    <span>14:32</span>
                  </div>
                  <p className="text-[10px] text-slate-550 leading-relaxed font-semibold">
                    AI detected violent motion patterns on <strong>Hostel Corridor B</strong>. Security alert dispatched.
                  </p>
                </div>

                <div className="p-3 bg-amber-50/20 border border-amber-250 rounded-xl space-y-1.5">
                  <div className="flex justify-between items-center text-[9px] font-black uppercase text-amber-700">
                    <span>Loitering behavior</span>
                    <span>01:04</span>
                  </div>
                  <p className="text-[10px] text-slate-550 leading-relaxed font-semibold">
                    Loitering detected at **Hostel Entrance A** door. 140s threshold exceeded.
                  </p>
                </div>

                <div className="p-3 bg-blue-50/20 border border-blue-250 rounded-xl space-y-1.5">
                  <div className="flex justify-between items-center text-[9px] font-black uppercase text-blue-700">
                    <span>Intrusion validation</span>
                    <span>Yesterday</span>
                  </div>
                  <p className="text-[10px] text-slate-550 leading-relaxed font-semibold">
                    Main perimeter fence line intrusion detected. Checked and cleared by Officer Robert.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. LIVE STREAM PLAYER TAB
          ======================================================== */}
      {activeTab === "live_streams" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Stream Selector column */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Surveillance Sources
            </h3>

            <div className="space-y-2 max-h-[380px] overflow-y-auto pr-1">
              {accessibleCams.map(cam => {
                const active = selectedCamId === cam.id;
                const isBath = cam.location.includes("Restricted");
                return (
                  <button
                    key={cam.id}
                    onClick={() => setSelectedCamId(cam.id)}
                    className={`w-full text-left p-3.5 rounded-2xl border transition duration-200 flex items-center justify-between gap-3 ${
                      active
                        ? "bg-blue-50/10 border-blue-300 text-blue-700 shadow-2xs font-extrabold"
                        : "bg-white border-slate-150 text-slate-650 hover:bg-slate-50"
                    }`}
                  >
                    <div className="min-w-0">
                      <span className="text-xs block truncate">{cam.name}</span>
                      <span className="text-[9px] text-slate-400 block font-normal font-sans uppercase mt-0.5">{cam.location}</span>
                    </div>

                    {isBath ? (
                      <Lock className="h-4 w-4 text-rose-500 shrink-0" />
                    ) : (
                      <span className={`h-2 w-2 rounded-full shrink-0 ${cam.status === "Online" ? "bg-emerald-500" : "bg-rose-500"}`} />
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Stream Display Box */}
          <div className="lg:col-span-8 flex flex-col justify-between bg-slate-950 border border-slate-900 rounded-3xl p-6 relative overflow-hidden min-h-[480px]">
            {/* Watermark security overlay */}
            <div className="absolute inset-0 flex items-center justify-center opacity-[0.03] select-none pointer-events-none font-black text-[9px] uppercase tracking-widest text-white">
              ST. JUDE CCTV SECURITY ACCESS SYSTEM
            </div>

            {/* Video Stream box */}
            <div className="flex-1 flex flex-col justify-between relative">
              <div className="flex justify-between items-center text-[10px] text-white font-mono bg-black/60 px-3 py-1.5 rounded-xl z-10">
                <span className="flex items-center gap-1.5 uppercase font-bold tracking-widest">
                  <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-ping" />
                  Live Feed: {currentCam?.name}
                </span>
                <span>RTSP Stream Port: 554</span>
              </div>

              {isPrivacyProtected ? (
                /* Privacy Shield Block */
                <div className="flex-1 flex flex-col items-center justify-center text-center p-8 space-y-3 z-10">
                  <Lock className="h-12 w-12 text-rose-500 animate-pulse" />
                  <h4 className="text-sm font-black text-rose-400 uppercase tracking-widest">Privacy Shield Engaged</h4>
                  <p className="text-xs text-slate-400 max-w-sm leading-relaxed font-bold">
                    CCTV viewing in private hostel dormitory sleeping/changing rooms is strictly prohibited under GDPR & FERPA compliance standards.
                  </p>
                </div>
              ) : (
                /* Active feed noise simulator */
                <div className="flex-1 flex flex-col items-center justify-center relative my-4 rounded-2xl bg-slate-900 border border-slate-850 overflow-hidden shadow-inner">
                  <div className="absolute inset-0 bg-scanlines opacity-[0.08] pointer-events-none" />
                  <div className="absolute inset-0 bg-noise opacity-[0.04] pointer-events-none" />
                  
                  {isLivePaused ? (
                    <Play className="h-12 w-12 text-white/40 cursor-pointer hover:text-white transition" onClick={() => setIsLivePaused(false)} />
                  ) : (
                    <Video className="h-12 w-12 text-slate-700 animate-pulse" />
                  )}

                  <span className="text-[10px] text-slate-500 font-mono font-bold mt-2">
                    {isLivePaused ? "STREAM PAUSED" : `STREAM ACTIVE • ${streamQuality}`}
                  </span>
                </div>
              )}

              {/* Stream adjustment toolbar */}
              <div className="flex flex-col sm:flex-row justify-between items-center gap-3 pt-3 border-t border-slate-900 z-10 text-xs font-bold text-slate-400">
                <div className="flex gap-2">
                  <Select
                    options={[
                      { label: "1080p Full HD", value: "1080p" },
                      { label: "720p HD Ready", value: "720p" },
                      { label: "480p SD Quality", value: "480p" }
                    ]}
                    value={streamQuality}
                    onChange={(e) => setStreamQuality(e.target.value)}
                    className="text-[10px] h-8 py-0.5 bg-slate-900 border-slate-800 text-white rounded-lg"
                    disabled={isPrivacyProtected}
                  />

                  <Button
                    onClick={() => {
                      addToast("Snapshot Saved", "Rendered CCTV frame saved to device photos.");
                    }}
                    disabled={isPrivacyProtected}
                    className="text-[10px] h-8 bg-slate-900 border border-slate-800 text-white rounded-lg px-3"
                  >
                    Snapshot
                  </Button>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    onClick={() => setIsLivePaused(!isLivePaused)}
                    disabled={isPrivacyProtected}
                    className="text-[10px] h-8 bg-slate-900 border border-slate-800 text-white rounded-lg px-3"
                  >
                    {isLivePaused ? "Resume" : "Pause Stream"}
                  </Button>

                  <Button
                    onClick={() => {
                      setIsMuted(!isMuted);
                      addToast(isMuted ? "Volume Unmuted" : "Volume Muted", "Classroom environment audio adjusted.");
                    }}
                    disabled={isPrivacyProtected}
                    className="text-[10px] h-8 bg-slate-900 border border-slate-800 text-white rounded-lg px-3"
                  >
                    {isMuted ? "Unmute Audio" : "Mute"}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          3. CAMERA MANAGEMENT TAB
          ======================================================== */}
      {activeTab === "manager" && !isParent && (
        <div className="space-y-5">
          {/* Controls Editor Panel */}
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search camera name or location..."
                value={cameraSearch}
                onChange={(e) => setCameraSearch(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition"
              />
            </div>

            <div className="flex flex-wrap gap-3 w-full md:w-auto">
              <div className="w-36">
                <Select
                  options={[
                    { label: "All Health Status", value: "All" },
                    { label: "Online Cams", value: "Online" },
                    { label: "Offline Alert Cams", value: "Offline" }
                  ]}
                  value={cameraFilter}
                  onChange={(e) => setCameraFilter(e.target.value)}
                  className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                />
              </div>

              {isPrincipalOrAdmin && (
                <>
                  <Button
                    onClick={() => setIsAssignCamOpen(true)}
                    className="text-xs py-1 px-4 bg-slate-905 hover:bg-slate-800 text-slate-700 rounded-xl border border-slate-200 bg-white"
                  >
                    <Layers className="h-4 w-4 mr-1.5 shrink-0" /> Assign Classroom
                  </Button>
                  <Button
                    onClick={() => setIsRegisterCamOpen(true)}
                    className="text-xs py-1 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs"
                  >
                    <Plus className="h-4 w-4 mr-1.5 shrink-0" /> Register Camera
                  </Button>
                </>
              )}
            </div>
          </div>

          {/* Camera list table */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Registered IP Cameras Inventory ({filteredCams.length} devices)
            </h3>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Camera Name</th>
                    <th className="p-3">Category Type</th>
                    <th className="p-3">Physical Location</th>
                    <th className="p-3">RTSP Stream Path</th>
                    <th className="p-3">Health Status</th>
                    {isPrincipalOrAdmin && <th className="p-3 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {filteredCams.map(cam => {
                    const isProtected = cam.location.includes("Restricted");
                    return (
                      <tr key={cam.id}>
                        <td className="p-3 font-extrabold text-slate-850 flex items-center gap-2">
                          <Video className="h-4 w-4 text-blue-605" /> {cam.name}
                        </td>
                        <td className="p-3 font-bold text-slate-650 uppercase font-mono">{cam.cameraType}</td>
                        <td className="p-3 font-bold text-slate-800">
                          {cam.location}
                          {isProtected && <Badge variant="danger" className="ml-2 text-[8px] font-black uppercase">Shielded</Badge>}
                        </td>
                        <td className="p-3 font-mono text-slate-450 font-bold truncate max-w-xs">{cam.streamUrl}</td>
                        <td className="p-3">
                          <Badge variant={cam.status === "Online" ? "success" : "danger"} className="text-[9px] font-black uppercase">
                            {cam.status}
                          </Badge>
                        </td>
                        {isPrincipalOrAdmin && (
                          <td className="p-3 text-right">
                            <div className="flex gap-2 justify-end">
                              <Button
                                onClick={() => handleToggleCamHealth(cam.id)}
                                className="text-[10px] py-1 h-7 px-3 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-650"
                              >
                                Toggle health
                              </Button>
                              <Button
                                onClick={() => handleDeleteCamera(cam.id)}
                                className="text-[10px] py-1 h-7 px-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          </td>
                        )}
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
          4. SCHOOL BUS SURVEILLANCE TAB
          ======================================================== */}
      {activeTab === "bus_transit" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Bus Stream Select side */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50 flex items-center gap-1.5">
              <Map className="h-4 w-4 text-blue-650" /> Transport Fleet Cams
            </h3>

            <div className="space-y-3">
              <button
                onClick={() => setSelectedBusId("cam-bus-12f")}
                className={`w-full text-left p-4 rounded-2xl border transition duration-200 ${
                  selectedBusId === "cam-bus-12f"
                    ? "bg-blue-50/10 border-blue-300 text-blue-700 shadow-2xs font-extrabold"
                    : "bg-white border-slate-150 text-slate-650 hover:bg-slate-50"
                }`}
              >
                <h4 className="text-xs font-black">Bus 12 - Driver Forward Cam</h4>
                <p className="text-[9px] text-slate-400 mt-1">Status: Transit • GPS Route 12</p>
              </button>

              <button
                onClick={() => setSelectedBusId("cam-bus-12p")}
                className={`w-full text-left p-4 rounded-2xl border transition duration-200 ${
                  selectedBusId === "cam-bus-12p"
                    ? "bg-blue-50/10 border-blue-300 text-blue-700 shadow-2xs font-extrabold"
                    : "bg-white border-slate-150 text-slate-650 hover:bg-slate-50"
                }`}
              >
                <h4 className="text-xs font-black">Bus 12 - Passenger Cabin</h4>
                <p className="text-[9px] text-slate-400 mt-1">Status: Transit • GPS Route 12</p>
              </button>
            </div>

            {/* GPS Tracker simulation */}
            <div className="p-4 bg-slate-50 border border-slate-150 rounded-2xl space-y-3.5">
              <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider block">Live GPS Bus Tracker</span>
              <div className="relative h-2 bg-slate-200 rounded-full w-full">
                <div
                  className="absolute h-4 w-4 bg-blue-600 border-2 border-white rounded-full -top-1 transition-all duration-300 shadow-xs"
                  style={{ left: `${gpsProgress}%` }}
                />
              </div>
              <div className="flex justify-between text-[8px] font-bold font-mono text-slate-400">
                <span>CAMPUS GATES</span>
                <span>CITY ROAD</span>
                <span>WHISPERING PINES</span>
              </div>
            </div>
          </div>

          {/* Live Stream View */}
          <div className="lg:col-span-8 bg-slate-950 border border-slate-900 rounded-3xl p-6 flex flex-col justify-between min-h-[420px] relative">
            <div className="absolute inset-0 bg-scanlines opacity-[0.08] pointer-events-none" />
            <div className="absolute inset-0 bg-noise opacity-[0.04] pointer-events-none" />

            <div className="flex justify-between items-center text-[10px] text-white font-mono bg-black/60 px-3 py-1.5 rounded-xl z-10">
              <span className="uppercase font-bold tracking-widest flex items-center gap-1">
                <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-ping" />
                Transit Stream: {selectedBusId === "cam-bus-12f" ? "Driver Front View" : "Passenger Deck"}
              </span>
              <span>4G Network Signal: Strong</span>
            </div>

            <div className="flex-1 flex flex-col items-center justify-center my-6 relative">
              <Video className="h-12 w-12 text-slate-700 animate-pulse" />
              <span className="text-[10px] text-slate-500 font-mono font-bold mt-2 uppercase">
                Transit Mobile Stream active • GPS sync: {gpsProgress}%
              </span>
            </div>

            <div className="text-[9px] text-slate-400 font-bold uppercase tracking-wider text-center border-t border-slate-900 pt-3">
              Parents can view transit stream during designated pickup/drop slots only.
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          5. AI FACE ATTENDANCE TAB
          ======================================================== */}
      {activeTab === "ai_attendance" && !isParent && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Controls column */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-5 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50 flex items-center gap-1.5">
              <UserCheck className="h-4 w-4 text-blue-650" /> AI Attendance Verification
            </h3>

            <div className="space-y-4">
              <Select
                label="Target Class Room *"
                options={[
                  { label: "Grade 10 - Section A", value: "10-A" },
                  { label: "Grade 5 - Section A", value: "5-A" }
                ]}
                value={aiClassNo}
                onChange={(e) => setAiClassNo(e.target.value)}
              />

              <Button
                onClick={handleRunAiAttendance}
                disabled={aiScannerState === "scanning"}
                className="w-full text-xs py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs"
              >
                {aiScannerState === "scanning" ? "Running Face Analytics..." : "Execute AI Face Scan"}
              </Button>
            </div>

            {aiScannerState === "complete" && (
              <div className="p-4 bg-emerald-50/20 border border-emerald-250 rounded-2xl space-y-2.5 animate-scale-up">
                <span className="text-[9px] uppercase font-bold text-emerald-800 tracking-wider flex items-center gap-1">
                  <ShieldCheck className="h-3.5 w-3.5" /> Face Scan Verified
                </span>
                <div className="space-y-1 text-xs font-bold text-slate-700">
                  {scannedNames.map((name, i) => (
                    <div key={i} className="flex justify-between items-center bg-white border border-slate-100 p-1.5 rounded-lg">
                      <span>{name}</span>
                      <Badge variant="success" className="text-[8px] font-black uppercase">PRESENT</Badge>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* AI scan camera viewport */}
          <div className="lg:col-span-8 bg-slate-950 border border-slate-900 rounded-3xl p-6 flex flex-col justify-between min-h-[430px] relative overflow-hidden">
            <div className="absolute inset-0 bg-scanlines opacity-[0.08] pointer-events-none" />
            <div className="absolute inset-0 bg-noise opacity-[0.04] pointer-events-none" />

            <div className="flex justify-between items-center text-[10px] text-white font-mono bg-black/60 px-3 py-1.5 rounded-xl z-10">
              <span className="uppercase font-bold tracking-widest flex items-center gap-1">
                <Activity className="h-4 w-4 text-emerald-500" />
                AI Vision Stream: Classroom 10-A
              </span>
              <span>AI Core Status: Active</span>
            </div>

            {aiScannerState === "scanning" ? (
              /* Simulated face tracking square boxes */
              <div className="flex-1 flex flex-col items-center justify-center my-6 relative min-h-[220px]">
                <div className="relative h-44 w-72 border-2 border-dashed border-blue-500 rounded-3xl flex items-center justify-center animate-pulse">
                  <div className="absolute border-t-2 border-l-2 border-emerald-500 h-8 w-8 top-4 left-4" />
                  <div className="absolute border-t-2 border-r-2 border-emerald-500 h-8 w-8 top-4 right-4" />
                  <div className="absolute border-b-2 border-l-2 border-emerald-500 h-8 w-8 bottom-4 left-4" />
                  <div className="absolute border-b-2 border-r-2 border-emerald-500 h-8 w-8 bottom-4 right-4" />
                  
                  <span className="text-[10px] font-mono text-emerald-500 font-bold animate-pulse">
                    TRACKING DYNAMIC FAClAL VECTORS
                  </span>
                </div>
              </div>
            ) : aiScannerState === "complete" ? (
              /* Successful scan visualization */
              <div className="flex-1 flex flex-col items-center justify-center my-6 relative min-h-[220px] space-y-4">
                <div className="relative h-44 w-72 border-2 border-emerald-500 bg-emerald-500/5 rounded-3xl flex flex-col items-center justify-center text-center p-4">
                  <CheckCircle className="h-10 w-10 text-emerald-500 mb-2" />
                  <h4 className="text-xs font-black text-emerald-450 uppercase tracking-widest">Attendance Roster Synced</h4>
                  <span className="text-[9px] text-slate-400 font-bold block mt-1">3 profiles verified. 0 anomalies detected.</span>
                </div>
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center my-6 relative min-h-[220px]">
                <Camera className="h-12 w-12 text-slate-700" />
                <span className="text-[10px] text-slate-500 font-mono font-bold mt-2 uppercase">
                  Awaiting AI face scan trigger...
                </span>
              </div>
            )}

            <div className="text-[9px] text-slate-450 font-bold uppercase tracking-wider text-center border-t border-slate-900 pt-3">
              Attendance data propagates automatically to the Master Roster logs.
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          6. INCIDENT INVESTIGATION DESK TAB
          ======================================================== */}
      {activeTab === "incidents" && !isParent && (
        <div className="space-y-5">
          {/* Controls & Filter Panel */}
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search incident type, description, camera..."
                value={incidentSearch}
                onChange={(e) => setIncidentSearch(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition"
              />
            </div>

            <div className="flex flex-wrap gap-3 w-full md:w-auto">
              <div className="w-36">
                <Select
                  options={[
                    { label: "All Statuses", value: "All" },
                    { label: "Detection State", value: "Detection" },
                    { label: "Investigation State", value: "Investigation" },
                    { label: "Resolved Incidents", value: "Resolved" }
                  ]}
                  value={incidentFilter}
                  onChange={(e) => setIncidentFilter(e.target.value)}
                  className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                />
              </div>

              {isSecurityManager && (
                <Button
                  onClick={() => setIsRaiseIncidentOpen(true)}
                  className="text-xs py-1 px-4 bg-rose-600 hover:bg-rose-700 text-white rounded-xl shadow-xs"
                >
                  <Plus className="h-4 w-4 mr-1.5 shrink-0" /> Raise Incident Alarm
                </Button>
              )}
            </div>
          </div>

          {/* Incident Logs Table */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Security Incident Investigation Logs ({filteredIncidents.length} entries)
            </h3>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Incident Type</th>
                    <th className="p-3">Camera Source</th>
                    <th className="p-3">Severity</th>
                    <th className="p-3">Occurred Time</th>
                    <th className="p-3">Workflow State</th>
                    <th className="p-3">Audit Details</th>
                    {isSecurityManager && <th className="p-3 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {filteredIncidents.map(inc => {
                    const cam = cameras.find(c => c.id === inc.cameraDocsId);
                    return (
                      <tr key={inc.id}>
                        <td className="p-3 font-extrabold text-slate-850 flex items-center gap-2">
                          <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0" /> {inc.incidentType}
                        </td>
                        <td className="p-3 font-bold text-slate-650">{cam ? cam.name : "IP Camera"}</td>
                        <td className="p-3 font-mono text-slate-500 font-bold">
                          <Badge variant={inc.severity === "Critical" ? "danger" : inc.severity === "High" ? "warning" : "info"}>
                            {inc.severity}
                          </Badge>
                        </td>
                        <td className="p-3 font-mono text-slate-400 font-bold">{inc.detectedAt}</td>
                        <td className="p-3">
                          <Badge variant={inc.status === "Resolved" ? "success" : "warning"} className="text-[9px] font-black uppercase">
                            {inc.status}
                          </Badge>
                        </td>
                        <td className="p-3 text-slate-400 font-semibold italic max-w-xs truncate">{inc.description}</td>
                        {isSecurityManager && (
                          <td className="p-3 text-right">
                            <div className="flex gap-2 justify-end">
                              {inc.status !== "Resolved" ? (
                                <>
                                  <Button
                                    onClick={() => handleUpdateIncidentStatus(inc.id, "Investigation")}
                                    className="text-[9px] py-1 h-7 px-2.5 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-650"
                                  >
                                    Investigate
                                  </Button>
                                  <Button
                                    onClick={() => handleUpdateIncidentStatus(inc.id, "Resolved")}
                                    className="text-[9px] py-1 h-7 px-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-bold"
                                  >
                                    Resolve
                                  </Button>
                                </>
                              ) : (
                                <span className="text-[10px] text-slate-405 font-bold font-mono">Logged & Closed</span>
                              )}
                            </div>
                          </td>
                        )}
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
          DIALOGS & FORMS
          ======================================================== */}
      {/* 1. Camera Registration Modal */}
      <Dialog
        isOpen={isRegisterCamOpen}
        onClose={() => setIsRegisterCamOpen(false)}
        title="Register Campus IP/RTSP Camera"
      >
        <form onSubmit={handleRegisterCamera} className="space-y-4 pt-1">
          <Input
            label="Camera Description Name *"
            value={newCamName}
            onChange={(e) => setNewCamName(e.target.value)}
            placeholder="Corridor Wing A Left Cam"
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Camera Source Type"
              options={[
                { label: "IP RTSP Camera", value: "IP RTSP" },
                { label: "ONVIF Smart Cam", value: "ONVIF Cloud" },
                { label: "Local NVR Feed", value: "NVR Local" },
                { label: "4G Mobile Transit Cam", value: "4G Mobile Cam" }
              ]}
              value={newCamType}
              onChange={(e) => setNewCamType(e.target.value)}
            />
            <Input
              label="Physical Area Location *"
              value={newCamLoc}
              onChange={(e) => setNewCamLoc(e.target.value)}
              placeholder="Academic Block"
              required
            />
          </div>

          <Input
            label="RTSP / Stream URL endpoint *"
            value={newCamUrl}
            onChange={(e) => setNewCamUrl(e.target.value)}
            placeholder="rtsp://streams.stjude.edu/wing_a_left"
            required
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsRegisterCamOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Register Camera Source
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 2. Raise Security Incident Alert Modal */}
      <Dialog
        isOpen={isRaiseIncidentOpen}
        onClose={() => setIsRaiseIncidentOpen(false)}
        title="Raise Security Emergency Incident"
      >
        <form onSubmit={handleRaiseIncident} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Incident Category Alarm"
              options={[
                { label: "Fight / Physical Altercation", value: "Fight / Violence" },
                { label: "Unauthorized Area Entry", value: "Unauthorized Entry" },
                { label: "Vandalism / Property damage", value: "Vandalism" },
                { label: "Loitering behavior", value: "Suspicious Activity" },
                { label: "Vehicle speed violation", value: "Vehicle Violation" }
              ]}
              value={incType}
              onChange={(e) => setIncType(e.target.value)}
            />
            <Select
              label="Severity Priority Rating"
              options={[
                { label: "Low Level", value: "Low" },
                { label: "Medium Level", value: "Medium" },
                { label: "High Level", value: "High" },
                { label: "Critical Alarm", value: "Critical" }
              ]}
              value={incSeverity}
              onChange={(e) => setIncSeverity(e.target.value)}
            />
          </div>

          <Select
            label="Associated Camera Source"
            options={cameras.map(c => ({
              label: `${c.name} (${c.location})`,
              value: c.id
            }))}
            value={incCamId}
            onChange={(e) => setIncCamId(e.target.value)}
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">
              Emergency details & Incident parameters *
            </label>
            <textarea
              value={incDesc}
              onChange={(e) => setIncDesc(e.target.value)}
              placeholder="Provide visual parameters recorded from stream view."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-24 font-semibold"
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsRaiseIncidentOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" size="sm" type="submit">
              Raise Incident Alert
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 3. Assign Camera Section Mapping Modal */}
      <Dialog
        isOpen={isAssignCamOpen}
        onClose={() => setIsAssignCamOpen(false)}
        title="Assign Camera mapping to Classroom"
      >
        <form onSubmit={handleAssignCamera} className="space-y-4 pt-1">
          <Select
            label="Select Camera Source *"
            options={cameras.map(c => ({
              label: `${c.name} (${c.location})`,
              value: c.id
            }))}
            value={assignCamId}
            onChange={(e) => setAssignCamId(e.target.value)}
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Select Grade *"
              options={[
                { label: "Grade 10", value: "Grade 10" },
                { label: "Grade 5", value: "Grade 5" },
                { label: "Grade 1", value: "Grade 1" }
              ]}
              value={assignGrade}
              onChange={(e) => setAssignGrade(e.target.value)}
            />
            <Select
              label="Class Section *"
              options={[
                { label: "Section A", value: "A" },
                { label: "Section B", value: "B" }
              ]}
              value={assignSection}
              onChange={(e) => setAssignSection(e.target.value)}
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsAssignCamOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Map Camera to Class
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
