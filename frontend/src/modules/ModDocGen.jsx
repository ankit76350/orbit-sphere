/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getDocumentTemplates,
  saveDocumentTemplates,
  getGeneratedDocuments,
  saveGeneratedDocuments,
  getIdCards,
  saveIdCards,
  getDocumentApprovals,
  saveDocumentApprovals,
  getDocumentSignatures,
  saveDocumentSignatures,
  getStudents,
  getStaff,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  FileText,
  Printer,
  QrCode,
  CheckCircle,
  Plus,
  Search,
  Users,
  Award,
  Trophy,
  DollarSign,
  Briefcase,
  TrendingUp,
  Clock,
  Layers,
  Signature,
  FileCheck,
  Eye,
  Settings,
  RefreshCw,
  Trash2,
  Lock,
  UploadCloud,
  FileCode,
  Sparkles
} from "lucide-react";

export default function ModDocGen({ user }) {
  const { addToast } = useToast();

  // Load state databases
  const [templates, setTemplates] = useState(() => getDocumentTemplates());
  const [genDocs, setGenDocs] = useState(() => getGeneratedDocuments());
  const [idCards, setIdCards] = useState(() => getIdCards());
  const [approvals, setApprovals] = useState(() => getDocumentApprovals());
  const [signatures, setSignatures] = useState(() => getDocumentSignatures());

  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());

  // Navigation tab
  // Options: dashboard, designer, certificates, approvals, verification, bulk_archive
  const [activeTab, setActiveTab] = useState("dashboard");

  // Modal control states
  const [isCreateTemplateOpen, setIsCreateTemplateOpen] = useState(false);
  const [isRequestDocOpen, setIsRequestDocOpen] = useState(false);
  const [isUploadSignatureOpen, setIsUploadSignatureOpen] = useState(false);
  const [isBulkGenOpen, setIsBulkGenOpen] = useState(false);
  const [isViewDocOpen, setIsViewDocOpen] = useState(false);

  // Form states
  // 1. Template Creator
  const [newTemplateName, setNewTemplateName] = useState("");
  const [newTemplateCat, setNewTemplateCat] = useState("Student Certificate");
  const [newTemplateContent, setNewTemplateContent] = useState("");

  // 2. ID Card Designer States
  const [designLayout, setDesignLayout] = useState("portrait"); // portrait or landscape
  const [cardPreset, setCardPreset] = useState("Student"); // Student, Staff, Parent, Alumni, Visitor
  const [bgColor, setBgColor] = useState("#2563eb"); // Primary Branding Color
  const [secondaryColor, setSecondaryColor] = useState("#1e3a8a");
  const [showLogo, setShowLogo] = useState(true);
  const [showPhoto, setShowPhoto] = useState(true);
  const [showQR, setShowQR] = useState(true);
  const [showBarcode, setShowBarcode] = useState(true);
  const [cardTitle, setCardTitle] = useState("ST. JUDE'S ACADEMY");
  const [selectedSignatureId, setSelectedSignatureId] = useState("");
  const [isCardBackSide, setIsCardBackSide] = useState(false);

  // 3. Document Generator / Request Form
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [targetEntityType, setTargetEntityType] = useState("Student"); // Student, Staff, Alumni
  const [targetEntityId, setTargetEntityId] = useState("");
  const [customTerm, setCustomTerm] = useState("Fall 2026");
  const [customAmount, setCustomAmount] = useState("1500");

  // 4. Signature Uploder Form
  const [sigName, setSigName] = useState("");
  const [sigRole, setSigRole] = useState("Principal");
  const [drawnSigText, setDrawnSigText] = useState("");

  // 5. Verification Portal Form
  const [searchDocNumber, setSearchDocNumber] = useState("");
  const [verifiedDoc, setVerifiedDoc] = useState(null);
  const [hasSearchedVerify, setHasSearchedVerify] = useState(false);

  // 6. Bulk Generation Form
  const [bulkTargetType, setBulkTargetType] = useState("Class"); // Class, Hostel, Staff, Route
  const [bulkTargetVal, setBulkTargetVal] = useState("Grade 10");
  const [bulkProgress, setBulkProgress] = useState(-1); // -1 means inactive
  const [bulkTmplId, setBulkTmplId] = useState("tmpl-student-id");

  // 7. Preview Document Modal State
  const [previewDocContent, setPreviewDocContent] = useState("");
  const [previewDocTitle, setPreviewDocTitle] = useState("");
  const [previewDocNum, setPreviewDocNum] = useState("");

  // Search/Filters
  const [archiveSearch, setArchiveSearch] = useState("");
  const [archiveFilter, setArchiveFilter] = useState("All");

  // Load defaults
  useEffect(() => {
    if (templates.length > 0 && !selectedTemplateId) {
      setSelectedTemplateId(templates[0].id);
    }
  }, [templates]);

  useEffect(() => {
    if (signatures.length > 0 && !selectedSignatureId) {
      setSelectedSignatureId(signatures[0].id);
    }
  }, [signatures]);

  useEffect(() => {
    if (targetEntityType === "Student" && students.length > 0) {
      setTargetEntityId(students[0].id);
    } else if (targetEntityType === "Staff" && staff.length > 0) {
      setTargetEntityId(staff[0].id);
    }
  }, [targetEntityType, students, staff]);

  // Role Validation
  const isSuperAdmin = user?.role === "Super Admin";
  const isAdminOrPrincipal = user?.role === "Super Admin" || user?.role === "Principal" || user?.role === "Warden";
  const isAccountant = user?.role === "Accountant" || user?.role === "Super Admin";
  const isHR = user?.role === "Super Admin" || user?.role === "Principal"; // staff/HR letters

  // Portal Calculations
  const docsGeneratedToday = genDocs.filter(d => d.generatedAt === new Date().toISOString().split("T")[0]).length;
  const totalCertificates = genDocs.filter(d => d.documentType.includes("Certificate") || d.documentType.includes("Letter")).length;
  const activeIDCards = idCards.filter(c => c.status === "Active").length;
  const pendingApprovalsCount = approvals.filter(a => a.status === "Pending").length;

  // HANDLERS
  // 1. Create custom document templates
  const handleCreateTemplate = (e) => {
    e.preventDefault();
    if (!newTemplateName || !newTemplateContent) {
      addToast("Failed to Save", "Fill in template name and content.", "error");
      return;
    }

    const newTmpl = {
      id: `tmpl-${Date.now()}`,
      name: newTemplateName,
      category: newTemplateCat,
      templateContent: newTemplateContent,
      status: "Active"
    };

    const nextList = [...templates, newTmpl];
    setTemplates(nextList);
    saveDocumentTemplates(nextList);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Document Template Created",
      `Added custom template: "${newTemplateName}" under category: ${newTemplateCat}`
    );

    addToast("Template Saved", `"${newTemplateName}" is now active in library.`);
    setIsCreateTemplateOpen(false);

    setNewTemplateName("");
    setNewTemplateContent("");
  };

  // 2. Request Certificate / Direct Document Generation
  const handleRequestDocument = (e) => {
    e.preventDefault();
    if (!selectedTemplateId || !targetEntityId) {
      addToast("Failed to Request", "Select template and target entity.", "error");
      return;
    }

    const template = templates.find(t => t.id === selectedTemplateId);
    let entityName = "";
    let entityClass = "Grade 10";
    let entitySec = "A";

    if (targetEntityType === "Student") {
      const stud = students.find(s => s.id === targetEntityId);
      entityName = stud ? stud.name : "Scholar Name";
      entityClass = stud ? stud.grade : "Grade 10";
    } else {
      const stf = staff.find(s => s.id === targetEntityId);
      entityName = stf ? stf.name : "Staff Name";
      entityClass = stf ? stf.department : "Science";
    }

    // Check if document needs approval workflow
    const needsApproval = template.name.includes("Transfer Certificate") || template.name.includes("Salary");

    const newDocId = `doc-${Date.now()}`;
    const docNumber = `DOC-2026-${Math.floor(1000 + Math.random() * 9000)}`;
    const verificationCode = `VCODE-${Math.floor(10000 + Math.random() * 90000)}`;

    if (needsApproval) {
      // Create Approval Request
      const newApproval = {
        id: `appr-${Date.now()}`,
        documentId: newDocId,
        documentTitle: `${template.name} (${entityName})`,
        requestorId: user?.id || "staff-teacher-1",
        requestorName: user?.name || "Teacher John",
        requestorRole: user?.role || "Teacher",
        approverId: "staff-principal",
        approverName: "Chloe Smith",
        status: "Pending",
        remarks: "Automatic routing system initialized. Awaiting Principal signing seal.",
        approvedAt: "",
        // Store payload details to compile later upon approval
        payload: {
          docNumber,
          documentType: template.name,
          entityId: targetEntityId,
          entityType: targetEntityType,
          verificationCode,
          content: compileTemplate(template.templateContent, entityName, targetEntityId, entityClass, entitySec)
        }
      };

      const nextApprovals = [newApproval, ...approvals];
      setApprovals(nextApprovals);
      saveDocumentApprovals(nextApprovals);

      logAction(
        user?.id || "sandbox",
        user?.name || "User",
        user?.role || "Staff",
        "Document Approval Requested",
        `Routed "${template.name}" for ${entityName} to Principal cabinet`
      );

      addToast("Workflow Initialized", `Approval ticket generated for ${entityName}. Principal notified.`);
    } else {
      // Direct Generation
      const compiledText = compileTemplate(template.templateContent, entityName, targetEntityId, entityClass, entitySec);

      const newGenerated = {
        id: newDocId,
        documentNumber: docNumber,
        documentType: template.name,
        entityId: targetEntityId,
        entityType: targetEntityType,
        pdfUrl: `${template.name.replace(/ /g, "_")}_${entityName.replace(/ /g, "_")}.pdf`,
        verificationCode: verificationCode,
        generatedAt: new Date().toISOString().split("T")[0],
        status: "Valid",
        compiledContent: compiledText
      };

      const nextGen = [newGenerated, ...genDocs];
      setGenDocs(nextGen);
      saveGeneratedDocuments(nextGen);

      logAction(
        user?.id || "sandbox",
        user?.name || "User",
        user?.role || "Staff",
        "Document Generated",
        `Created document ${docNumber} (${template.name}) for ${entityName}`
      );

      addToast("Document Ready", `Successfully generated ${docNumber}. Available in archive.`);
    }

    setIsRequestDocOpen(false);
  };

  // Helper template string compilation
  const compileTemplate = (text, name, id, cls, sec) => {
    let result = text || "";
    result = result.replace(/\{\{student_name\}\}/g, name);
    result = result.replace(/\{\{staff_name\}\}/g, name);
    result = result.replace(/\{\{student_id\}\}/g, id);
    result = result.replace(/\{\{employee_id\}\}/g, id);
    result = result.replace(/\{\{class\}\}/g, cls);
    result = result.replace(/\{\{section\}\}/g, sec);
    result = result.replace(/\{\{department\}\}/g, cls);
    result = result.replace(/\{\{designation\}\}/g, "Senior Specialist");
    result = result.replace(/\{\{salary\}\}/g, "$4,500/Month");
    result = result.replace(/\{\{admission_no\}\}/g, "STJ-" + id.substring(id.length - 4));
    result = result.replace(/\{\{amount\}\}/g, "$" + customAmount);
    result = result.replace(/\{\{term\}\}/g, customTerm);
    return result;
  };

  // 3. Approve Certificate request
  const handleApproveRequest = (approvalId) => {
    const updatedApprovals = approvals.map(app => {
      if (app.id === approvalId) {
        // Compile and create the generated document
        const pld = app.payload;
        if (pld) {
          const newGenerated = {
            id: app.documentId,
            documentNumber: pld.docNumber,
            documentType: pld.documentType,
            entityId: pld.entityId,
            entityType: pld.entityType,
            pdfUrl: `${pld.documentType.replace(/ /g, "_")}_Release.pdf`,
            verificationCode: pld.verificationCode,
            generatedAt: new Date().toISOString().split("T")[0],
            status: "Valid",
            compiledContent: pld.content
          };

          const nextGen = [newGenerated, ...genDocs];
          setGenDocs(nextGen);
          saveGeneratedDocuments(nextGen);
        }

        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          "Approval Granted",
          `Signed off and issued: ${app.documentTitle}`
        );

        addToast("Document Approved", "Digitally signed document released to archive.");
        return {
          ...app,
          status: "Approved",
          approvedAt: new Date().toISOString().split("T")[0],
          remarks: "Principal digital seal applied. Document re-routed and valid."
        };
      }
      return app;
    });

    setApprovals(updatedApprovals);
    saveDocumentApprovals(updatedApprovals);
  };

  // Reject Request
  const handleRejectRequest = (approvalId) => {
    const updatedApprovals = approvals.map(app => {
      if (app.id === approvalId) {
        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          "Approval Denied",
          `Rejected ticket: ${app.documentTitle}`
        );

        addToast("Document Rejected", "Request flagged as rejected.", "warning");
        return {
          ...app,
          status: "Rejected",
          remarks: "Declined due to audit discrepancies. Re-submission required."
        };
      }
      return app;
    });

    setApprovals(updatedApprovals);
    saveDocumentApprovals(updatedApprovals);
  };

  // 4. Upload/Draw Digital Signature
  const handleUploadSignature = (e) => {
    e.preventDefault();
    if (!sigName || !drawnSigText) {
      addToast("Form Incomplete", "Please specify name and signature drawings.", "error");
      return;
    }

    const newSig = {
      id: `sig-${Date.now()}`,
      signerId: `staff-${sigRole.toLowerCase()}`,
      signerName: sigName,
      designation: sigRole,
      signatureUrl: `${drawnSigText.replace(/ /g, "_")}_sig.png`,
      active: true
    };

    const nextSigs = [...signatures, newSig];
    setSignatures(nextSigs);
    saveDocumentSignatures(nextSigs);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Signature Registered",
      `Saved signature for ${sigName} (${sigRole})`
    );

    addToast("Signature Loaded", `Digital seal for ${sigName} is active.`);
    setIsUploadSignatureOpen(false);

    setSigName("");
    setDrawnSigText("");
  };

  // 5. Verification Portal Search
  const handleVerifySearch = (e) => {
    e.preventDefault();
    if (!searchDocNumber) return;

    // Search in generated documents
    const match = genDocs.find(
      d => d.documentNumber.toLowerCase() === searchDocNumber.trim().toLowerCase() ||
           d.verificationCode.toLowerCase() === searchDocNumber.trim().toLowerCase()
    );

    setHasSearchedVerify(true);
    setVerifiedDoc(match || null);

    if (match) {
      addToast("Document Verified", `Record ${match.documentNumber} is active and valid.`, "success");
    } else {
      addToast("Record Not Found", "No match found for this document code.", "error");
    }
  };

  // 6. Bulk Generation Compiler Simulator
  const handleRunBulkGeneration = (e) => {
    e.preventDefault();
    setBulkProgress(0);

    const interval = setInterval(() => {
      setBulkProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          
          // Inject mock ID Cards/Certificates bulk compiled
          const count = bulkTargetType === "Class" ? 35 : bulkTargetType === "Hostel" ? 120 : 45;
          addToast("Bulk Job Complete", `Successfully compiled ${count} print files. routed to print queue.`);

          logAction(
            user?.id || "sandbox",
            user?.name || "User",
            user?.role || "Staff",
            "Bulk Documents Compiled",
            `Generated ${count} files under template type target: ${bulkTargetVal}`
          );

          setIsBulkGenOpen(false);
          return -1;
        }
        return prev + 25;
      });
    }, 400);
  };

  // View Document contents
  const handleOpenDocPreview = (doc) => {
    setSelectedJob(doc);
    setPreviewDocTitle(doc.documentType);
    setPreviewDocNum(doc.documentNumber);
    setPreviewDocContent(doc.compiledContent || `Bonafide record confirming details of user index: ${doc.entityId}. Clearances active.`);
    setIsViewDocOpen(true);
  };

  // Archive filtered
  const filteredArchive = genDocs.filter(doc => {
    const matchesSearch =
      doc.documentNumber.toLowerCase().includes(archiveSearch.toLowerCase()) ||
      doc.documentType.toLowerCase().includes(archiveSearch.toLowerCase()) ||
      doc.entityId.toLowerCase().includes(archiveSearch.toLowerCase());

    const matchesFilter =
      archiveFilter === "All" ||
      (archiveFilter === "Certificates" && doc.documentType.includes("Certificate")) ||
      (archiveFilter === "Finance" && doc.documentType.includes("Receipt")) ||
      (archiveFilter === "HR" && doc.documentType.includes("Salary"));

    return matchesSearch && matchesFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Layers className="h-6 w-6 text-blue-600 animate-pulse" />
            MOD-18: Document Generation & ID Card Management
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Automated template libraries, drag-and-drop ID designers, QR verification gates, and digital signatures.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-slate-50 border border-slate-150 px-3 py-1.5 rounded-xl">
          <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
            ROLE: {user?.role || "Visitor"}
          </Badge>
        </div>
      </div>

      {/* Tabs Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "dashboard" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Console Summary
        </button>
        <button
          onClick={() => setActiveTab("designer")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "designer" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          ID Card Designer
        </button>
        <button
          onClick={() => setActiveTab("certificates")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "certificates" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Certificate Console
        </button>
        <button
          onClick={() => setActiveTab("approvals")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "approvals" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Approvals & Signatures ({pendingApprovalsCount})
        </button>
        <button
          onClick={() => setActiveTab("verification")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "verification" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Verification Portal
        </button>
        <button
          onClick={() => setActiveTab("bulk_archive")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "bulk_archive" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Archive & Bulk Queue
        </button>
      </div>

      {/* ========================================================
          1. DASHBOARD SUMMARY TAB
          ======================================================== */}
      {activeTab === "dashboard" && (
        <div className="space-y-6">
          {/* KPI Dashboard Metrics */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Issued Today</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{docsGeneratedToday} Documents</p>
              <span className="text-[10px] text-emerald-600 font-semibold block mt-1">✓ Instantly verified QR</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Total Certificates</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalCertificates} PDF Files</p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">archived in secure storage</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Active ID Cards</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{activeIDCards} Profiles</p>
              <span className="text-[10px] text-blue-600 font-semibold block mt-1">Student & Staff PVC badges</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Pending Approvals</span>
              <p className={`text-xl font-bold mt-1 ${pendingApprovalsCount > 0 ? "text-amber-600" : "text-slate-800"}`}>
                {pendingApprovalsCount} Requests
              </p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">awaiting Principal seal</span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left: Pending Approval workflows queue */}
            <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50 flex justify-between items-center">
                <span>Recent Approval Queue</span>
                <Badge variant="warning" className="text-[9px] font-black uppercase">Requires Action</Badge>
              </h3>

              <div className="space-y-3">
                {approvals.slice(0, 3).map(app => (
                  <div key={app.id} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                    <div>
                      <h4 className="text-xs font-black text-slate-850">{app.documentTitle}</h4>
                      <p className="text-[10px] text-slate-400 font-bold mt-0.5">
                        Requested by: {app.requestorName} ({app.requestorRole})
                      </p>
                      <p className="text-[10px] text-slate-500 italic mt-1 font-semibold">"{app.remarks}"</p>
                    </div>

                    <div className="flex gap-2 self-end sm:self-auto shrink-0">
                      {app.status === "Pending" ? (
                        <>
                          {isAdminOrPrincipal ? (
                            <>
                              <Button
                                onClick={() => handleApproveRequest(app.id)}
                                className="text-[9px] h-7 px-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold"
                              >
                                Sign & Approve
                              </Button>
                              <Button
                                onClick={() => handleRejectRequest(app.id)}
                                className="text-[9px] h-7 px-3 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-bold"
                              >
                                Reject
                              </Button>
                            </>
                          ) : (
                            <span className="text-[10px] text-slate-400 font-bold font-mono">Principal Action Only</span>
                          )}
                        </>
                      ) : (
                        <Badge variant={app.status === "Approved" ? "success" : "danger"} className="text-[9px] font-black uppercase">
                          {app.status}
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Right: Template Quick List */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b pb-3 border-slate-50">
                Active Templates ({templates.length})
              </h3>
              <div className="space-y-3.5 max-h-[260px] overflow-y-auto pr-1">
                {templates.map(tmpl => (
                  <div key={tmpl.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <h4 className="text-xs font-black text-slate-800 truncate">{tmpl.name}</h4>
                      <span className="text-[9px] font-mono text-slate-450 uppercase tracking-wider block mt-0.5">{tmpl.category}</span>
                    </div>
                    <Badge variant="success" className="text-[8px] font-black uppercase">Active</Badge>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. ID CARD DESIGNER TAB
          ======================================================== */}
      {activeTab === "designer" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Controls Editor Panel */}
          <div className="lg:col-span-5 bg-white border border-slate-100 rounded-3xl p-6 space-y-5 shadow-2xs">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Branding & Layout Settings
            </h3>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Select
                  label="Preset Badge Type"
                  options={[
                    { label: "Student ID Card", value: "Student" },
                    { label: "Staff Employee Badge", value: "Staff" },
                    { label: "Parent Escort Pass", value: "Parent" },
                    { label: "Alumni Lifetime ID", value: "Alumni" },
                    { label: "Temporary Visitor Pass", value: "Visitor" }
                  ]}
                  value={cardPreset}
                  onChange={(e) => setCardPreset(e.target.value)}
                />
                <Select
                  label="Card Layout Orientation"
                  options={[
                    { label: "Portrait Layout", value: "portrait" },
                    { label: "Landscape Layout", value: "landscape" }
                  ]}
                  value={designLayout}
                  onChange={(e) => setDesignLayout(e.target.value)}
                />
              </div>

              <Input
                label="Header Branding Text"
                value={cardTitle}
                onChange={(e) => setCardTitle(e.target.value)}
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Primary Accent color"
                  type="color"
                  value={bgColor}
                  onChange={(e) => setBgColor(e.target.value)}
                />
                <Input
                  label="Secondary Accent color"
                  type="color"
                  value={secondaryColor}
                  onChange={(e) => setSecondaryColor(e.target.value)}
                />
              </div>

              {/* Toggles */}
              <div className="space-y-2.5 pt-2">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Dynamic Component Layers</p>
                <div className="grid grid-cols-2 gap-3">
                  <label className="flex items-center gap-2 text-xs font-bold text-slate-650 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={showLogo}
                      onChange={(e) => setShowLogo(e.target.checked)}
                      className="rounded text-blue-600 focus:ring-blue-500"
                    />
                    School Emblem
                  </label>
                  <label className="flex items-center gap-2 text-xs font-bold text-slate-650 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={showPhoto}
                      onChange={(e) => setShowPhoto(e.target.checked)}
                      className="rounded text-blue-600 focus:ring-blue-500"
                    />
                    Holder Photo
                  </label>
                  <label className="flex items-center gap-2 text-xs font-bold text-slate-650 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={showQR}
                      onChange={(e) => setShowQR(e.target.checked)}
                      className="rounded text-blue-600 focus:ring-blue-500"
                    />
                    Verification QR
                  </label>
                  <label className="flex items-center gap-2 text-xs font-bold text-slate-650 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={showBarcode}
                      onChange={(e) => setShowBarcode(e.target.checked)}
                      className="rounded text-blue-600 focus:ring-blue-500"
                    />
                    Scanning Barcode
                  </label>
                </div>
              </div>

              <Select
                label="Signatory Authorization"
                options={signatures.map(s => ({
                  label: `${s.signerName} (${s.designation})`,
                  value: s.id
                }))}
                value={selectedSignatureId}
                onChange={(e) => setSelectedSignatureId(e.target.value)}
              />
            </div>

            <div className="flex gap-3 pt-4 border-t border-slate-100">
              <Button
                onClick={() => setIsBulkGenOpen(true)}
                className="flex-1 text-xs py-2 bg-slate-900 text-white hover:bg-slate-800 rounded-xl"
              >
                Bulk Generate Card Cohort
              </Button>
              <Button
                onClick={() => {
                  addToast("Exporting PDF", "Starting print job translation...");
                  setTimeout(() => addToast("PDF Exported", "Document print template saved to downloads."), 1200);
                }}
                className="text-xs py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-xl"
              >
                Export PDF Preview
              </Button>
            </div>
          </div>

          {/* Designer Preview Output */}
          <div className="lg:col-span-7 flex flex-col items-center justify-center bg-slate-50 border border-slate-200 p-8 rounded-3xl min-h-[450px]">
            <div className="flex gap-2.5 mb-6 bg-slate-200/60 p-1.5 rounded-xl">
              <button
                onClick={() => setIsCardBackSide(false)}
                className={`text-[10px] font-bold px-3 py-1.5 rounded-lg transition ${!isCardBackSide ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500"}`}
              >
                Front Side Face
              </button>
              <button
                onClick={() => setIsCardBackSide(true)}
                className={`text-[10px] font-bold px-3 py-1.5 rounded-lg transition ${isCardBackSide ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500"}`}
              >
                Back Side Face
              </button>
            </div>

            {/* Simulated PVC Card */}
            <div
              className={`bg-white rounded-3xl border border-slate-300 shadow-xl overflow-hidden transition-all duration-300 ${
                designLayout === "portrait" ? "w-[280px] h-[430px]" : "w-[430px] h-[280px]"
              }`}
              style={{
                fontFamily: "Inter, sans-serif"
              }}
            >
              {!isCardBackSide ? (
                /* FRONT SIDE DESIGN LAYOUT */
                <div className="h-full flex flex-col justify-between p-4 text-center select-none relative">
                  {/* Branding Color header */}
                  <div
                    className="absolute inset-x-0 top-0 h-20 flex flex-col items-center justify-center p-2 text-white"
                    style={{
                      background: `linear-gradient(135deg, ${bgColor}, ${secondaryColor})`
                    }}
                  >
                    {showLogo && (
                      <div className="h-6 w-6 bg-white rounded-full flex items-center justify-center font-black text-[9px] text-blue-700 shadow-xs mb-0.5">
                        SJ
                      </div>
                    )}
                    <h4 className="text-[10px] font-black tracking-widest">{cardTitle}</h4>
                    <span className="text-[7px] font-black uppercase tracking-wider opacity-90">{cardPreset} CREDENTIAL</span>
                  </div>

                  {/* Holder Profile content block */}
                  <div className="mt-20 flex-1 flex flex-col items-center justify-center space-y-3 py-2">
                    {showPhoto && (
                      <div className="h-24 w-24 rounded-full border-4 border-slate-100 bg-slate-200 flex items-center justify-center shadow-xs overflow-hidden">
                        <div className="h-full w-full bg-gradient-to-tr from-slate-300 to-slate-200 flex items-center justify-center text-slate-500">
                          <Users className="h-10 w-10 text-slate-400" />
                        </div>
                      </div>
                    )}

                    <div className="space-y-1">
                      <h3 className="text-sm font-black text-slate-850">
                        {cardPreset === "Student" ? "Liam Smith" : cardPreset === "Staff" ? "Prof. John Doe" : "Margaret Thatcher"}
                      </h3>
                      <p className="text-[10px] text-blue-650 font-bold uppercase tracking-wider">
                        {cardPreset === "Student" ? "Grade 10 | Section A" : cardPreset === "Staff" ? "Math Department" : "Primary Guardian"}
                      </p>
                    </div>

                    <div className="space-y-0.5 font-mono text-[9px] font-bold text-slate-450 text-left border border-slate-50 p-2 rounded-xl bg-slate-50/50 w-52 mx-auto">
                      <p><span className="text-slate-400">ID:</span> STJ-{cardPreset === "Student" ? "1002" : "9082"}</p>
                      <p><span className="text-slate-400">Blood Group:</span> O+</p>
                      <p><span className="text-slate-400">Dorm:</span> Resident Wing B</p>
                    </div>
                  </div>

                  {/* ID Card Footer */}
                  <div className="border-t border-slate-100 pt-2.5 flex justify-between items-center px-2">
                    {showQR ? (
                      <div className="h-9 w-9 bg-slate-50 border border-slate-200 rounded flex items-center justify-center p-0.5">
                        <QrCode className="h-8 w-8 text-slate-800" />
                      </div>
                    ) : <div className="w-9" />}

                    {/* Signature preview */}
                    <div className="text-right">
                      <span className="text-[7px] text-slate-400 block font-bold">ISSUING AUTHORITY</span>
                      {(() => {
                        const sigObj = signatures.find(s => s.id === selectedSignatureId);
                        return (
                          <span className="text-[10px] font-bold text-blue-700 italic font-serif">
                            {sigObj ? sigObj.signerName : "Chloe Smith"}
                          </span>
                        );
                      })()}
                    </div>
                  </div>
                </div>
              ) : (
                /* BACK SIDE DESIGN LAYOUT */
                <div className="h-full flex flex-col justify-between p-5 text-xs text-slate-600 font-semibold relative">
                  <div className="space-y-4 pt-4">
                    <h4 className="text-[10px] font-black text-slate-800 border-b pb-1 border-slate-100 uppercase tracking-wider">
                      TERMS & CONDITIONS
                    </h4>
                    <ul className="space-y-2 text-[9px] text-slate-450 list-disc pl-4 font-bold font-sans">
                      <li>This credential card is non-transferable and remains the property of St. Jude's Boarding Academy.</li>
                      <li>Loss or theft must be reported immediately to administrative office.</li>
                      <li>In emergency, dial administrative cabinet helpline at +1 (555) 601-1932.</li>
                    </ul>
                  </div>

                  {/* QR/Barcode back footer */}
                  <div className="space-y-2 border-t border-slate-100 pt-3 flex flex-col items-center">
                    {showBarcode && (
                      <div className="w-full h-8 bg-slate-100 border border-slate-200 rounded flex items-center justify-center p-1 text-[7px] font-mono tracking-widest text-slate-500">
                        ||||||| | ||||| | ||||| | ||||| | ||||||||
                      </div>
                    )}
                    <span className="text-[7px] text-slate-400 font-bold uppercase">St. Jude's ERP • Powered by Antigravity OS</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          3. CERTIFICATE GENERATION TAB
          ======================================================== */}
      {activeTab === "certificates" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Document request configuration console */}
          <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <div>
                <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Document Auto-Population</h3>
                <p className="text-[11px] text-slate-400 font-semibold mt-1">Select a certificate preset and holder entity to auto-populate variables.</p>
              </div>
              <Button onClick={() => setIsCreateTemplateOpen(true)} className="text-xs py-1.5">
                <Plus className="h-3.5 w-3.5 mr-1" /> Create Template
              </Button>
            </div>

            <form onSubmit={handleRequestDocument} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Select
                  label="Select Template Preset *"
                  options={templates.map(t => ({
                    label: `${t.name} (${t.category})`,
                    value: t.id
                  }))}
                  value={selectedTemplateId}
                  onChange={(e) => setSelectedTemplateId(e.target.value)}
                />
                <Select
                  label="Holder Cohort Type *"
                  options={[
                    { label: "Student Scholar", value: "Student" },
                    { label: "Staff Member / Employee", value: "Staff" }
                  ]}
                  value={targetEntityType}
                  onChange={(e) => setTargetEntityType(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {targetEntityType === "Student" ? (
                  <Select
                    label="Select Target Student *"
                    options={students.map(s => ({
                      label: `${s.name} (Grade: ${s.grade} | Wallet: $${s.walletBalance})`,
                      value: s.id
                    }))}
                    value={targetEntityId}
                    onChange={(e) => setTargetEntityId(e.target.value)}
                  />
                ) : (
                  <Select
                    label="Select Target Staff *"
                    options={staff.map(s => ({
                      label: `${s.name} (${s.role} • ${s.department})`,
                      value: s.id
                    }))}
                    value={targetEntityId}
                    onChange={(e) => setTargetEntityId(e.target.value)}
                  />
                )}

                {/* Additional parameters input block */}
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    label="Term Variable"
                    value={customTerm}
                    onChange={(e) => setCustomTerm(e.target.value)}
                  />
                  <Input
                    label="Amount ($)"
                    type="number"
                    value={customAmount}
                    onChange={(e) => setCustomAmount(e.target.value)}
                  />
                </div>
              </div>

              {/* Template Content preview section */}
              {(() => {
                const tmpl = templates.find(t => t.id === selectedTemplateId);
                return (
                  <div className="p-4 bg-slate-50 rounded-2xl border border-slate-150 space-y-2">
                    <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Raw Template Content Preview</span>
                    <p className="text-xs font-semibold text-slate-500 italic leading-relaxed">
                      {tmpl ? tmpl.templateContent : "No template selected."}
                    </p>
                  </div>
                );
              })()}

              <div className="pt-3 border-t border-slate-100 flex justify-end">
                <Button type="submit" variant="secondary" className="text-xs py-2 px-6">
                  Compile & Run Document Job
                </Button>
              </div>
            </form>
          </div>

          {/* List of active document templates */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Certificate Categories
            </h3>

            <div className="space-y-2 text-xs font-bold text-slate-650">
              <div className="flex justify-between items-center p-2.5 hover:bg-slate-50 rounded-xl transition">
                <span>Bonafide Certificate Presets</span>
                <Badge variant="info">3 Templates</Badge>
              </div>
              <div className="flex justify-between items-center p-2.5 hover:bg-slate-50 rounded-xl transition">
                <span>Transfer & Study Letters</span>
                <Badge variant="info">2 Templates</Badge>
              </div>
              <div className="flex justify-between items-center p-2.5 hover:bg-slate-50 rounded-xl transition">
                <span>Appreciation & Appraisals</span>
                <Badge variant="info">4 Templates</Badge>
              </div>
              <div className="flex justify-between items-center p-2.5 hover:bg-slate-50 rounded-xl transition">
                <span>Outstanding Dues Notices</span>
                <Badge variant="info">1 Template</Badge>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          4. APPROVAL & SIGNING DESK TAB
          ======================================================== */}
      {activeTab === "approvals" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Document approvals queue table */}
          <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <div>
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Awaiting Digital Signatures</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Multi-stage certificate verification workflows. Principal authorization adds digital seals.</p>
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Certificate Title</th>
                    <th className="p-3">Requestor Info</th>
                    <th className="p-3">State Status</th>
                    <th className="p-3">Remarks</th>
                    {isAdminOrPrincipal && <th className="p-3 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {approvals.map(app => (
                    <tr key={app.id}>
                      <td className="p-3 font-extrabold text-slate-850">{app.documentTitle}</td>
                      <td className="p-3">
                        <span className="font-bold">{app.requestorName}</span>
                        <span className="text-[9px] text-slate-400 block font-normal font-sans">{app.requestorRole}</span>
                      </td>
                      <td className="p-3">
                        <Badge variant={app.status === "Approved" ? "success" : app.status === "Pending" ? "warning" : "danger"} className="text-[9px] font-black uppercase">
                          {app.status}
                        </Badge>
                      </td>
                      <td className="p-3 text-slate-400 font-semibold italic max-w-xs truncate">{app.remarks}</td>
                      {isAdminOrPrincipal && (
                        <td className="p-3 text-right">
                          {app.status === "Pending" ? (
                            <div className="flex gap-1.5 justify-end">
                              <Button
                                onClick={() => handleApproveRequest(app.id)}
                                className="text-[9px] py-1 px-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold"
                              >
                                Sign
                              </Button>
                              <Button
                                onClick={() => handleRejectRequest(app.id)}
                                className="text-[9px] py-1 px-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-bold"
                              >
                                Deny
                              </Button>
                            </div>
                          ) : (
                            <span className="text-[10px] text-slate-400 font-bold font-mono">Released</span>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Signature Manager console */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Signature Seals</h3>
              <Button onClick={() => setIsUploadSignatureOpen(true)} className="text-[10px] py-1 px-2 rounded-lg">
                <Plus className="h-3 w-3 mr-1" /> Add Seal
              </Button>
            </div>

            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              {signatures.map(sig => (
                <div key={sig.id} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-2">
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{sig.signerName}</h4>
                      <p className="text-[9px] text-slate-400 font-bold uppercase tracking-wide mt-0.5">{sig.designation}</p>
                    </div>
                    <Badge variant="success" className="text-[8px] font-black uppercase">Active</Badge>
                  </div>
                  <div className="border-t border-slate-200/60 pt-2 flex justify-between items-center text-[10px] font-bold text-slate-400">
                    <span className="font-mono text-[9px] text-slate-400">Key ID: {sig.signerId}</span>
                    <span className="text-blue-700 italic font-serif tracking-wider font-extrabold pr-2">
                      {sig.signerName}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          5. VERIFICATION PORTAL TAB
          ======================================================== */}
      {activeTab === "verification" && (
        <div className="flex flex-col items-center justify-center bg-slate-50 border border-slate-200 p-8 rounded-3xl min-h-[450px]">
          <div className="max-w-xl w-full bg-white p-8 rounded-3xl border border-slate-300 shadow-xl space-y-6">
            <div className="text-center space-y-2">
              <div className="h-12 w-12 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mx-auto shadow-xs">
                <QrCode className="h-6 w-6" />
              </div>
              <h3 className="text-md font-black text-slate-850 uppercase tracking-widest">Public Verification Portal</h3>
              <p className="text-xs text-slate-400 font-semibold">
                Scan QR codes or input generated Document Numbers / verification codes to crosscheck state credentials.
              </p>
            </div>

            <form onSubmit={handleVerifySearch} className="space-y-4">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Enter Document Code (e.g. DOC-2026-0002 or VCODE-48293)"
                  value={searchDocNumber}
                  onChange={(e) => setSearchDocNumber(e.target.value)}
                  className="bg-slate-50 border border-slate-250 text-slate-800 rounded-2xl pl-4 pr-24 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition font-semibold"
                  required
                />
                <Button
                  type="submit"
                  variant="secondary"
                  className="absolute right-1.5 top-1.5 text-xs py-1.5 px-4 h-9 bg-blue-600 text-white rounded-xl font-bold"
                >
                  Verify Now
                </Button>
              </div>
            </form>

            {/* Results block */}
            {hasSearchedVerify && (
              <div className="border-t border-slate-100 pt-6 animate-scale-up">
                {verifiedDoc ? (
                  <div className="p-5 bg-emerald-50/20 border border-emerald-250 rounded-2xl space-y-4">
                    <div className="flex justify-between items-center border-b border-emerald-100 pb-3">
                      <div>
                        <h4 className="text-xs font-black text-emerald-800 uppercase tracking-wider">Verification Approved</h4>
                        <span className="text-[9px] text-slate-400 font-mono block mt-0.5">Reference ID: {verifiedDoc.documentNumber}</span>
                      </div>
                      <Badge variant="success" className="text-[9px] font-black uppercase">Valid Record</Badge>
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-xs font-bold text-slate-700">
                      <div>
                        <span className="text-[9px] text-slate-400 block uppercase">Document Category</span>
                        <span>{verifiedDoc.documentType}</span>
                      </div>
                      <div>
                        <span className="text-[9px] text-slate-400 block uppercase">Holder Reference</span>
                        <span className="font-mono">{verifiedDoc.entityId} ({verifiedDoc.entityType})</span>
                      </div>
                      <div>
                        <span className="text-[9px] text-slate-400 block uppercase">Timestamp Issued</span>
                        <span>{verifiedDoc.generatedAt}</span>
                      </div>
                      <div>
                        <span className="text-[9px] text-slate-400 block uppercase">Verif Code matching</span>
                        <span className="font-mono text-blue-650">{verifiedDoc.verificationCode}</span>
                      </div>
                    </div>

                    <div className="pt-2">
                      <Button
                        onClick={() => handleOpenDocPreview(verifiedDoc)}
                        className="w-full text-xs py-2 bg-slate-900 text-white hover:bg-slate-800 rounded-xl"
                      >
                        <Eye className="h-4 w-4 mr-1.5" /> View Compiled Document
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="p-5 bg-rose-50/20 border border-rose-200 rounded-2xl text-center space-y-2">
                    <h4 className="text-xs font-black text-rose-700 uppercase tracking-wider">Invalid / Revoked Record</h4>
                    <p className="text-[11px] text-slate-400 font-bold leading-relaxed">
                      No database matches exist for code: <strong className="text-slate-700 font-mono font-bold">"{searchDocNumber}"</strong>. Confirm spelling parameters.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          6. ARCHIVE & BULK QUEUE TAB
          ======================================================== */}
      {activeTab === "bulk_archive" && (
        <div className="space-y-5">
          {/* Actions & Filters */}
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search by Document ID or Holder..."
                value={archiveSearch}
                onChange={(e) => setArchiveSearch(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition"
              />
            </div>

            <div className="flex flex-wrap gap-3 w-full md:w-auto">
              <div className="w-40">
                <Select
                  options={[
                    { label: "All Categories", value: "All" },
                    { label: "Academic Certificates", value: "Certificates" },
                    { label: "Finance Receipts", value: "Finance" },
                    { label: "HR Letters", value: "HR" }
                  ]}
                  value={archiveFilter}
                  onChange={(e) => setArchiveFilter(e.target.value)}
                  className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                />
              </div>

              {isAdminOrPrincipal && (
                <Button onClick={() => setIsBulkGenOpen(true)} className="text-xs py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl">
                  <Printer className="h-4 w-4 mr-1.5 shrink-0" /> Bulk Print Job
                </Button>
              )}
            </div>
          </div>

          {/* Archive Grid List */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Generated PDF Archive Log ({filteredArchive.length} records)
            </h3>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Document Number</th>
                    <th className="p-3">Document Category</th>
                    <th className="p-3">Holder Reference</th>
                    <th className="p-3">Verification ID</th>
                    <th className="p-3">Timestamp Issued</th>
                    <th className="p-3">Security status</th>
                    <th className="p-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {filteredArchive.map(doc => (
                    <tr key={doc.id}>
                      <td className="p-3 font-extrabold text-blue-700 font-mono">{doc.documentNumber}</td>
                      <td className="p-3 font-bold text-slate-800">{doc.documentType}</td>
                      <td className="p-3 font-bold text-slate-650 uppercase font-mono">{doc.entityId} ({doc.entityType})</td>
                      <td className="p-3 font-mono text-slate-500 font-bold">{doc.verificationCode}</td>
                      <td className="p-3 font-mono text-slate-400 font-bold">{doc.generatedAt}</td>
                      <td className="p-3">
                        <Badge variant="success" className="text-[9px] font-black uppercase">Valid</Badge>
                      </td>
                      <td className="p-3 text-right">
                        <div className="flex gap-2 justify-end">
                          <Button
                            onClick={() => handleOpenDocPreview(doc)}
                            className="text-[10px] py-1 h-7 px-2.5 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-650"
                          >
                            <Eye className="h-3 w-3 mr-1 shrink-0" /> Preview
                          </Button>
                          <Button
                            onClick={() => {
                              addToast("Exporting", `Printing PDF layout: ${doc.documentNumber}`);
                            }}
                            className="text-[10px] py-1 h-7 px-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-lg"
                          >
                            Print PDF
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL FORMS & DIALOGS
          ======================================================== */}
      {/* 1. Template Creator Dialog */}
      <Dialog
        isOpen={isCreateTemplateOpen}
        onClose={() => setIsCreateTemplateOpen(false)}
        title="Create Custom Document Template"
      >
        <form onSubmit={handleCreateTemplate} className="space-y-4 pt-1">
          <Input
            label="Template Name *"
            value={newTemplateName}
            onChange={(e) => setNewTemplateName(e.target.value)}
            placeholder="Appreciation Letter Template"
            required
          />

          <Select
            label="Template Category *"
            options={[
              { label: "Student Certificate", value: "Student Certificate" },
              { label: "Staff HR Form / Letter", value: "Staff HR" },
              { label: "Alumni Membership Letter", value: "Alumni" },
              { label: "Finance Receipt / Statements", value: "Finance" },
              { label: "Academic Marksheets / Report Cards", value: "Academic" }
            ]}
            value={newTemplateCat}
            onChange={(e) => setNewTemplateCat(e.target.value)}
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">
              Rich Content Content Template *
            </label>
            <textarea
              value={newTemplateContent}
              onChange={(e) => setNewTemplateContent(e.target.value)}
              placeholder="This is to certify that {{student_name}} of class {{class}}..."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-32 font-semibold"
              required
            />
            <span className="text-[8px] text-slate-400 font-bold block">
              Supported Variables: {"{{student_name}}"}, {"{{student_id}}"}, {"{{class}}"}, {"{{section}}"}, {"{{admission_no}}"}, {"{{amount}}"}, {"{{term}}"}
            </span>
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsCreateTemplateOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Save Template in Library
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 2. Upload/Draw Signature Dialog */}
      <Dialog
        isOpen={isUploadSignatureOpen}
        onClose={() => setIsUploadSignatureOpen(false)}
        title="Register Digital Signing seal"
      >
        <form onSubmit={handleUploadSignature} className="space-y-4 pt-1">
          <Input
            label="Signatory Full Name *"
            value={sigName}
            onChange={(e) => setSigName(e.target.value)}
            placeholder="Chloe Smith"
            required
          />

          <Select
            label="Officer Authorization Role *"
            options={[
              { label: "Principal", value: "Principal" },
              { label: "Chief Accountant", value: "Chief Accountant" },
              { label: "HR Manager", value: "HR Manager" },
              { label: "Warden Cabinets", value: "Warden" }
            ]}
            value={sigRole}
            onChange={(e) => setSigRole(e.target.value)}
          />

          <Input
            label="Signature Initials (Drawn Text Seal) *"
            value={drawnSigText}
            onChange={(e) => setDrawnSigText(e.target.value)}
            placeholder="ChloeS"
            required
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsUploadSignatureOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Register Signature Seal
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 3. Bulk Generator Dialog */}
      <Dialog
        isOpen={isBulkGenOpen}
        onClose={() => setIsBulkGenOpen(false)}
        title="Configure Bulk Document Compilation"
      >
        {bulkProgress >= 0 ? (
          <div className="p-8 text-center space-y-4">
            <RefreshCw className="h-10 w-10 text-blue-600 animate-spin mx-auto" />
            <h4 className="text-xs font-black uppercase text-slate-800 tracking-wider">Compiling Print batches...</h4>
            <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden max-w-md mx-auto">
              <div
                className="bg-blue-600 h-full rounded-full transition-all duration-300"
                style={{ width: `${bulkProgress}%` }}
              />
            </div>
            <span className="text-[10px] text-slate-400 font-bold font-mono">Progress: {bulkProgress}%</span>
          </div>
        ) : (
          <form onSubmit={handleRunBulkGeneration} className="space-y-4 pt-1">
            <div className="grid grid-cols-2 gap-4">
              <Select
                label="Target Group Type"
                options={[
                  { label: "Grade Cohort", value: "Class" },
                  { label: "Hostel Wing", value: "Hostel" },
                  { label: "Staff Department", value: "Staff" }
                ]}
                value={bulkTargetType}
                onChange={(e) => setBulkTargetType(e.target.value)}
              />
              <Input
                label="Target Value"
                value={bulkTargetVal}
                onChange={(e) => setBulkTargetVal(e.target.value)}
                placeholder="Grade 10"
                required
              />
            </div>

            <Select
              label="Selected Document Template"
              options={templates.map(t => ({
                label: t.name,
                value: t.id
              }))}
              value={bulkTmplId}
              onChange={(e) => setBulkTmplId(e.target.value)}
            />

            <div className="p-4 bg-blue-50/10 border border-blue-200 rounded-2xl text-[11px] text-blue-700 leading-relaxed font-bold">
              Bulk generation will issue documents to all matching records inside storage. Progress parameters will track compiling operations.
            </div>

            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" size="sm" onClick={() => setIsBulkGenOpen(false)}>
                Cancel
              </Button>
              <Button variant="secondary" size="sm" type="submit">
                Execute Bulk Generation
              </Button>
            </div>
          </form>
        )}
      </Dialog>

      {/* 4. Preview Document Modal */}
      <Dialog
        isOpen={isViewDocOpen}
        onClose={() => setIsViewDocOpen(false)}
        title={`${previewDocTitle} (${previewDocNum})`}
      >
        <div className="p-6 bg-slate-50 border border-slate-200 rounded-3xl min-h-[250px] relative font-serif text-slate-800 leading-relaxed shadow-inner">
          {/* Header watermark */}
          <div className="absolute inset-0 flex items-center justify-center opacity-[0.03] select-none pointer-events-none">
            <Trophy className="h-64 w-64 text-slate-900" />
          </div>

          <div className="space-y-4">
            <div className="text-center border-b pb-4 border-slate-200">
              <h2 className="text-lg font-black tracking-widest font-sans text-slate-900">ST. JUDE'S BOARDING SCHOOL</h2>
              <span className="text-[8px] tracking-wider uppercase font-bold text-slate-400 font-sans block mt-1">
                Institutional Academic Record Seal
              </span>
            </div>

            <p className="text-sm italic text-slate-700 leading-relaxed pt-2">
              "{previewDocContent}"
            </p>

            <div className="pt-8 flex justify-between items-end border-t border-slate-200/60 text-[10px] font-sans font-bold text-slate-400">
              <div>
                <span className="block font-mono">CODE: {previewDocNum}</span>
                <span className="block mt-0.5">ISSUED ON: {new Date().toISOString().split("T")[0]}</span>
              </div>
              <div className="text-right">
                <span className="block">DIGITALLY SIGNED BY:</span>
                <span className="block font-serif text-xs font-extrabold text-blue-700 italic mt-0.5">
                  Principal Cabinet ChloeS
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4 gap-2">
          <Button variant="outline" size="sm" onClick={() => setIsViewDocOpen(false)}>
            Close
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => {
              addToast("Document printed", `Sent ${previewDocNum} to physical printer.`);
              setIsViewDocOpen(false);
            }}
          >
            Send to Print
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
