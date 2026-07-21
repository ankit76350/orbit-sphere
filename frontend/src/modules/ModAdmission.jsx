/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getStudents, saveStudents, logAction, getHostels, saveHostels } from "../storage";
import { api } from "../api";
import { Button, Input, Select, useToast } from "../components/ui";
import { Sparkles, FileText, UploadCloud, UserPlus, ShieldCheck } from "lucide-react";
export default function ModAdmission({ user }) {
  const { addToast } = useToast();
  const [studentFullName, setStudentFullName] = useState("");
  const [grade, setGrade] = useState("Grade 7");
  const [gender, setGender] = useState("Male");
  const [dob, setDob] = useState("2014-05-15");
  const [parentName, setParentName] = useState("");
  const [parentPhone, setParentPhone] = useState("");
  const [parentEmail, setParentEmail] = useState("");
  const [address, setAddress] = useState("");
  const [hostelOptIn, setHostelOptIn] = useState(true);
  const [transportOptIn, setTransportOptIn] = useState(false);
  const [feeStructure, setFeeStructure] = useState("tuition-standard");
  const [photoPreview, setPhotoPreview] = useState(null);
  const [tentativeAdNo] = useState(() => `STJ2026-${Math.floor(1100 + Math.random() * 800)}`);
  const handlePhotoSimulate = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onload = (e2) => {
        if (e2.target?.result) {
          setPhotoPreview(e2.target.result);
        }
      };
      reader.readAsDataURL(file);
      addToast("Uploaded", "Photo preview applied successfully");
    }
  };
  const handleRegister = (e) => {
    e.preventDefault();
    if (!studentFullName || !parentName || !parentPhone || !parentEmail) {
      addToast("Error", "Ensure student name, parent name, phone, and parent email are fully written", "error");
      return;
    }
    const students = getStudents();
    const studentId = `student-${Date.now()}`;
    let hostelBuilding;
    let hostelFloor;
    let hostelRoomNumber;
    let hostelBedNumber;
    if (hostelOptIn) {
      const hostels = getHostels();
      const suitableGender = gender === "Female" ? "Girls" : "Boys";
      const vacantRoom = hostels.find(
        (r) => r.gender === suitableGender && r.beds.some((b) => b.studentId === null)
      );
      if (vacantRoom) {
        const vacantBed = vacantRoom.beds.find((b) => b.studentId === null);
        if (vacantBed) {
          vacantBed.studentId = studentId;
          vacantBed.studentName = studentFullName;
          hostelBuilding = vacantRoom.buildingName;
          hostelFloor = vacantRoom.floor;
          hostelRoomNumber = vacantRoom.roomNumber;
          hostelBedNumber = vacantBed.bedNumber;
          saveHostels(hostels);
        }
      }
    }
    const newStudent = {
      id: studentId,
      schoolId: "6a474d2517e9c40cf971ccc2",
      admissionNumber: tentativeAdNo,
      admissionNo: tentativeAdNo,
      name: studentFullName,
      gradeIndex: parseInt(grade.split(" ")[1]) || 7,
      grade,
      gender: (gender || "Male").toUpperCase(),
      dob: dob || "2015-06-19",
      bloodGroup: "AB+",
      photoUrl: photoPreview || "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d",
      walletId: `wallet-${Date.now()}`,
      medicalRecordId: `medical-${Date.now()}`,
      status: "ACTIVE",
      admissionDate: new Date().toISOString().split("T")[0],
      joinedDate: new Date().toISOString().split("T")[0],
      parentName,
      parentEmail,
      parentPhone,
      parentId: `parent-gen-${Date.now()}`,
      address: address || "9 Oak Ave",
      hostelOptIn,
      transportOptIn,
      hostelBuilding,
      hostelFloor,
      hostelRoomNumber,
      hostelBedNumber,
      walletBalance: 100,
      medicalBloodGroup: "AB+",
      medicalAllergies: [],
      medicalConditions: [],
      medicalDoctorLogs: "Initial board enrollment. Awaiting medical summary sheets.",
      guardians: [
        {
          name: parentName,
          relation: "MOTHER",
          phone: parentPhone,
          email: parentEmail,
          address: address || "9 Oak Ave",
          occupation: "Parent",
          primary: true,
          emergencyContact: true,
          pickupApproved: true,
          portalAccess: true
        }
      ],
      currentAcademicRecord: {
        academicYear: "2026-2027",
        studentNo: `STD-${Math.floor(100 + Math.random() * 900)}`,
        rollNo: "9C-01",
        classDocId: grade,
        sectionNo: "Section-A",
        hostelRoomNo: hostelRoomNumber ? `Room ${hostelRoomNumber}` : undefined,
        status: "ACTIVE"
      }
    };
    const updatedStudents = [...students, newStudent];
    saveStudents(updatedStudents);
    api.createStudent(newStudent).catch(() => {});
    api.createAdmission({
      schoolId: 'SCH-001',
      studentName: studentFullName,
      parentName,
      parentPhone,
      parentEmail,
      appliedGrade: grade,
      status: 'APPROVED',
      convertedStudentId: newStudent.id
    }).catch(() => {});
    logAction(
      user.id,
      user.name,
      user.role,
      "Student Admitted",
      `Registered student ${studentFullName} (AdNo: ${tentativeAdNo}) in ${grade}. Hostel assignment: ${hostelRoomNumber ? "Room " + hostelRoomNumber : "None"}`
    );
    addToast("Success", `Student Registered! Assigned Admission No: ${tentativeAdNo}`, "success");
    setStudentFullName("");
    setParentName("");
    setParentPhone("");
    setParentEmail("");
    setAddress("");
    setPhotoPreview(null);
  };
  return <div className="space-y-6 max-w-4xl mx-auto">
      
      {
    /* Intro info */
  }
      <div className="bg-white p-6 rounded-3xl border border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight">Student Admission Desk</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Enlist new boarding scholars, generate official index numbers, assign dormitories instantly, and trigger term fee obligations.
          </p>
        </div>
        <div className="bg-indigo-50 border border-indigo-100 px-4 py-2 rounded-2xl flex items-center gap-2 shrink-0">
          <Sparkles className="h-5 w-5 text-indigo-600 animate-pulse" />
          <div>
            <p className="text-[10px] text-indigo-500 uppercase font-extrabold tracking-wider">NEXT ENROLL ADMISSION NUM</p>
            <p className="text-sm font-black text-slate-800">{tentativeAdNo}</p>
          </div>
        </div>
      </div>

      <form onSubmit={handleRegister} className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {
    /* Left Column: Portrait & Options */
  }
        <div className="md:col-span-1 space-y-6">
          <div className="bg-white p-6 rounded-3xl border border-slate-100 flex flex-col items-center text-center gap-4">
            <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest text-left w-full">Portrait Upload</h3>
            
            <div className="h-36 w-36 bg-slate-50 border-2 border-dashed border-slate-200 rounded-full overflow-hidden flex flex-col items-center justify-center relative cursor-pointer hover:border-indigo-400 group transition">
              {photoPreview ? <img src={photoPreview} alt="Preview" className="h-full w-full object-cover" /> : <div className="flex flex-col items-center justify-center gap-1.5 p-4 text-slate-400 group-hover:text-indigo-600">
                  <UploadCloud className="h-8 w-8" />
                  <span className="text-[10px] font-bold uppercase tracking-wider">Select Photo</span>
                </div>}
              <input
    type="file"
    accept="image/*"
    onChange={handlePhotoSimulate}
    className="absolute inset-0 opacity-0 cursor-pointer"
  />
            </div>
            <p className="text-[10px] text-slate-400 font-semibold leading-normal">
              Supported files: JPG, PNG. Dynamic scale helper will crop image to circle badge.
            </p>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-slate-100 space-y-4">
            <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Boarding Program Opt-ins</h3>
            
            <div className="space-y-3 pt-1">
              <label className="flex items-start gap-3 p-3 bg-slate-50 hover:bg-slate-100/50 rounded-2xl border border-slate-100 cursor-pointer transition select-none">
                <input
    type="checkbox"
    checked={hostelOptIn}
    onChange={(e) => setHostelOptIn(e.target.checked)}
    className="mt-1 h-4.5 w-4.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
  />
                <div>
                  <h4 className="text-xs font-extrabold text-slate-800">Dorm Hostel Allocation</h4>
                  <p className="text-[10px] text-slate-400 font-medium mt-0.5">Assign room & single bunk bed immediately upon registrar submit.</p>
                </div>
              </label>

              <label className="flex items-start gap-3 p-3 bg-slate-50 hover:bg-slate-100/50 rounded-2xl border border-slate-100 cursor-pointer transition select-none">
                <input
    type="checkbox"
    checked={transportOptIn}
    onChange={(e) => setTransportOptIn(e.target.checked)}
    className="mt-1 h-4.5 w-4.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
  />
                <div>
                  <h4 className="text-xs font-extrabold text-slate-800">Assign Fleet Transit Stop</h4>
                  <p className="text-[10px] text-slate-400 font-medium mt-0.5">Link standard pickup corridor stops and dynamic GPS bus tracker codes.</p>
                </div>
              </label>
            </div>
          </div>
        </div>

        {
    /* Right Columns: Roster form */
  }
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white p-6 md:p-8 rounded-3xl border border-slate-100 space-y-5">
            <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100 pb-2">Academic & Personal Profile</h3>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
    label="Student Legal Roster Full Name"
    placeholder="e.g. Liam Benjamin Smith"
    value={studentFullName}
    onChange={(e) => setStudentFullName(e.target.value)}
    required
  />
              <Input
    label="Date of Birth"
    type="date"
    value={dob}
    onChange={(e) => setDob(e.target.value)}
    required
  />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select
    label="Admission Designation Grade"
    options={[
      { label: "Grade 6 Secondary", value: "Grade 6" },
      { label: "Grade 7 Secondary", value: "Grade 7" },
      { label: "Grade 8 Secondary", value: "Grade 8" },
      { label: "Grade 9 Intermediate", value: "Grade 9" },
      { label: "Grade 10 Intermediate", value: "Grade 10" },
      { label: "Grade 11 College Prep", value: "Grade 11" },
      { label: "Grade 12 College Prep", value: "Grade 12" }
    ]}
    value={grade}
    onChange={(e) => setGrade(e.target.value)}
  />
              <Select
    label="Designated Cohort Gender"
    options={[
      { label: "Male / Boys House", value: "Male" },
      { label: "Female / Girls House", value: "Female" }
    ]}
    value={gender}
    onChange={(e) => setGender(e.target.value)}
  />
            </div>

            <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100 pb-2 pt-3">Designated Guardian Registry</h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Input
    label="Parent / Guardian Legal Name"
    placeholder="e.g. Thomas Smith"
    value={parentName}
    onChange={(e) => setParentName(e.target.value)}
    required
  />
              <Input
    label="Guardian Contact Phone"
    type="tel"
    placeholder="+1 (555) 601-3829"
    value={parentPhone}
    onChange={(e) => setParentPhone(e.target.value)}
    required
  />
              <Input
    label="Guardian Registered Email"
    type="email"
    placeholder="smith.thomas@gmail.com"
    value={parentEmail}
    onChange={(e) => setParentEmail(e.target.value)}
    required
  />
            </div>

            <Input
    label="Standard Mail Address"
    placeholder="e.g. Apartment 4B, 309 Whispering Brook Road, Cityville, MD"
    value={address}
    onChange={(e) => setAddress(e.target.value)}
  />

            <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100 pb-2 pt-3">Structure Fee assignment</h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select
    label="Select Fee Template Block"
    options={[
      { label: "Standard Matriculation Academic (Tuition $4,500/term)", value: "tuition-standard" },
      { label: "Premium Scholar Discount (Tuition $3,150/term)", value: "tuition-scholar" },
      { label: "Full Athletic Waiver (Tuition $0/term)", value: "tuition-sport" }
    ]}
    value={feeStructure}
    onChange={(e) => setFeeStructure(e.target.value)}
  />
              <div className="bg-slate-50 border border-slate-100 p-3.5 rounded-2xl flex items-center justify-between text-xs text-slate-500 font-semibold leading-normal">
                <div>
                  <p className="text-slate-700 font-extrabold flex gap-1 items-center">
                    <FileText className="h-3.5 w-3.5 text-indigo-500" /> Auto-Generated Obligations:
                  </p>
                  <p className="text-[10px] text-slate-400 mt-1">
                    Tuition + {hostelOptIn ? "Boarding Fee ($1,800/term)" : "No Dorm"}{" "}
                    {transportOptIn ? " + Transport ($450/term)" : ""}
                  </p>
                </div>
              </div>
            </div>

            <div className="pt-4 flex gap-3 justify-end border-t border-slate-100">
              <span className="text-[10px] text-slate-400 font-semibold flex items-center gap-1.5 mr-auto">
                <ShieldCheck className="h-4 w-4 text-indigo-500" />
                Durable sandbox session persistence active.
              </span>
              <Button type="submit" className="bg-indigo-600 hover:bg-indigo-700 flex gap-2 items-center text-xs py-3.5">
                <UserPlus className="h-4 w-4" /> Finalize Scholar Admission
              </Button>
            </div>

          </div>
        </div>

      </form>

    </div>;
}
