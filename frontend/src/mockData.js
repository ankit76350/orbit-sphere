/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
const firstNames = [
  "Liam",
  "Noah",
  "Oliver",
  "James",
  "Elijah",
  "William",
  "Henry",
  "Lucas",
  "Benjamin",
  "Theodore",
  "Emma",
  "Olivia",
  "Charlotte",
  "Amelia",
  "Sophia",
  "Mia",
  "Isabella",
  "Harper",
  "Evelyn",
  "Gianna",
  "Ethan",
  "Mason",
  "Logan",
  "Aiden",
  "Alexander",
  "Sebastian",
  "Michael",
  "Daniel",
  "David",
  "Joseph",
  "Emily",
  "Abigail",
  "Madison",
  "Elizabeth",
  "Avery",
  "Sofia",
  "Chloe",
  "Ella",
  "Grace",
  "Harper",
  "Rohan",
  "Aarav",
  "Vivaan",
  "Ananya",
  "Diya",
  "Siddharth",
  "Kabir",
  "Aditya",
  "Meera",
  "Sai"
];
const lastNames = [
  "Smith",
  "Johnson",
  "Williams",
  "Brown",
  "Jones",
  "Miller",
  "Davis",
  "Garcia",
  "Rodriguez",
  "Wilson",
  "Martinez",
  "Anderson",
  "Taylor",
  "Thomas",
  "Hernandez",
  "Moore",
  "Martin",
  "Jackson",
  "Thompson",
  "White",
  "Sharma",
  "Patel",
  "Verma",
  "Singh",
  "Gupta",
  "Mehta",
  "Rao",
  "Reddy",
  "Das",
  "Sen"
];
const subjectsList = [
  "Mathematics",
  "English Literature",
  "Inorganic Chemistry",
  "Classical Physics",
  "Advanced Biology",
  "World History",
  "Computer Science",
  "Art & Design"
];
export const mockSchools = [
  {
    id: "school-01",
    name: "St. Jude Boarding Academy",
    domain: "stjude.edu.erpsaas.com",
    plan: "Premium",
    status: "Active",
    createdDate: "2024-01-15",
    contactEmail: "admin@stjude.edu"
  },
  {
    id: "school-02",
    name: "Oak Creek International School",
    domain: "oakcreek.edu.erpsaas.com",
    plan: "Enterprise",
    status: "Active",
    createdDate: "2023-08-10",
    contactEmail: "hq@oakcreek.edu"
  }
];
const randomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];
const randomRange = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
export function generateStaff() {
  const staff = [
    {
      id: "staff-principal",
      name: "Dr. Arthur Pendelton",
      email: "principal@stjude.edu",
      role: "Principal",
      phone: "+1 (555) 101-2030",
      department: "Administration",
      salary: 12500,
      joiningDate: "2019-06-01",
      status: "Active",
      leavesAllowed: 24,
      leavesTaken: 3,
      reviewRating: 4.9
    },
    {
      id: "staff-warden-boys",
      name: "Marcus Brody",
      email: "m.brody@stjude.edu",
      role: "Warden",
      phone: "+1 (555) 302-3921",
      department: "Hostel Affairs",
      salary: 5400,
      joiningDate: "2021-08-15",
      status: "Active",
      leavesAllowed: 18,
      leavesTaken: 4,
      reviewRating: 4.6
    },
    {
      id: "staff-warden-girls",
      name: "Sarah Jenkins",
      email: "s.jenkins@stjude.edu",
      role: "Warden",
      phone: "+1 (555) 302-4412",
      department: "Hostel Affairs",
      salary: 5400,
      joiningDate: "2022-01-10",
      status: "Active",
      leavesAllowed: 18,
      leavesTaken: 2,
      reviewRating: 4.8
    },
    {
      id: "staff-accountant",
      name: "Grover Cleveland",
      email: "finance@stjude.edu",
      role: "Accountant",
      phone: "+1 (555) 489-1200",
      department: "Finance & Accounts",
      salary: 6800,
      joiningDate: "2020-03-24",
      status: "Active",
      leavesAllowed: 18,
      leavesTaken: 1,
      reviewRating: 4.5
    },
    {
      id: "staff-hrmanager",
      name: "Eleanor Vance",
      email: "hr@stjude.edu",
      role: "HR Manager",
      phone: "+1 (555) 203-1992",
      department: "Human Resources",
      salary: 7200,
      joiningDate: "2021-11-01",
      status: "Active",
      leavesAllowed: 20,
      leavesTaken: 5,
      reviewRating: 4.7
    },
    {
      id: "staff-storemanager",
      name: "Silas Marner",
      email: "store@stjude.edu",
      role: "Store Manager",
      phone: "+1 (555) 883-9372",
      department: "Inventory / Estates",
      salary: 4800,
      joiningDate: "2022-05-18",
      status: "Active",
      leavesAllowed: 15,
      leavesTaken: 0,
      reviewRating: 4.4
    }
  ];
  const teacherSubjects = [
    { sub: "Mathematics", dept: "Science & Math" },
    { sub: "English Literature", dept: "Languages" },
    { sub: "Inorganic Chemistry", dept: "Science & Math" },
    { sub: "Classical Physics", dept: "Science & Math" },
    { sub: "Advanced Biology", dept: "Science & Math" },
    { sub: "World History", dept: "Humanities" },
    { sub: "Computer Science", dept: "Technology" },
    { sub: "Art & Design", dept: "Creative Arts" }
  ];
  for (let i = 0; i < 15; i++) {
    const fn = randomItem(firstNames);
    const ln = randomItem(lastNames);
    const subjObj = teacherSubjects[i % teacherSubjects.length];
    staff.push({
      id: `staff-teacher-${i + 1}`,
      name: `Prof. ${fn} ${ln}`,
      email: `${fn.toLowerCase()}.${ln.toLowerCase()}@stjude.edu`,
      role: "Teacher",
      phone: `+1 (555) ${randomRange(200, 999)}-${randomRange(1e3, 9999)}`,
      department: subjObj.dept,
      salary: randomRange(5e3, 7500),
      joiningDate: `2022-0${randomRange(1, 9)}-12`,
      status: i === 14 ? "On Leave" : "Active",
      leavesAllowed: 20,
      leavesTaken: randomRange(1, 10),
      reviewRating: Number((parseFloat(String(Math.random() * 1.5)) + 3.5).toFixed(1))
    });
  }
  return staff;
}
export function generateStudents() {
  const list = [];
  const grades = ["Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"];
  for (let i = 1; i <= 105; i++) {
    const gender = i % 2 === 0 ? "Male" : "Female";
    const firstSeed = gender === "Male" ? firstNames.slice(0, 25) : firstNames.slice(25, 50);
    const fn = randomItem(firstSeed);
    const ln = randomItem(lastNames);
    const gradeIndex = i % grades.length;
    const grade = grades[gradeIndex];
    const hostelOptIn = i % 5 !== 0;
    const transportOptIn = !hostelOptIn || i % 8 === 0;
    const admissionNum = `STJ2025-${1e3 + i}`;
    const studentId = `student-${i}`;
    let hostelBuilding;
    let hostelFloor;
    let hostelRoomNumber;
    let hostelBedNumber;
    if (hostelOptIn) {
      if (gender === "Male") {
        hostelBuilding = "Vanguard Hall (Boys)";
        hostelFloor = i % 2 === 0 ? "Floor 1" : "Floor 2";
        const rn = 100 + i % 15;
        hostelRoomNumber = String(rn);
        hostelBedNumber = `Bed-${i % 4 + 1}`;
      } else {
        hostelBuilding = "Seraphina House (Girls)";
        hostelFloor = i % 2 === 0 ? "Floor 1" : "Floor 2";
        const rn = 200 + i % 15;
        hostelRoomNumber = String(rn);
        hostelBedNumber = `Bed-${i % 4 + 1}`;
      }
    }
    let transportRoute;
    let transportStop;
    let transportVehicleNumber;
    if (transportOptIn) {
      const routes = ["Route Alpha (North)", "Route Beta (Metro)", "Route Gamma (South Peaks)"];
      const stops = ["Civic Center", "Grand Station Square", "Westway Cross", "Lakeside Junction"];
      const vehicles = ["BUS-402X", "BUS-110A", "COACH-77Y"];
      transportRoute = routes[i % routes.length];
      transportStop = stops[i % stops.length];
      transportVehicleNumber = vehicles[i % vehicles.length];
    }
    const bloodGroups = ["O+", "A+", "B+", "AB+", "O-", "A-"];
    const allergies = [["Peanuts", "Penicillin"], [], ["Sulfonamides"], ["Gluten"], [], [], []];
    const conditions = [["Mild Asthma"], [], [], ["Lactose Intolerant"], [], [], []];
    list.push({
      id: studentId,
      admissionNumber: admissionNum,
      name: `${fn} ${ln}`,
      gradeIndex: gradeIndex + 6,
      grade,
      gender,
      dob: `201${randomRange(0, 4)}-0${randomRange(1, 9)}-${randomRange(10, 28)}`,
      parentName: `${randomItem(firstNames)} ${ln}`,
      parentEmail: `${fn.toLowerCase()}.${ln.toLowerCase()}.parent@gmail.com`,
      parentPhone: `+1 (555) 601-${randomRange(100, 999)}2`,
      parentId: `parent-${i}`,
      // Linked login ID
      address: `${randomRange(100, 999)} Whispering Pines Road, Cityville`,
      joinedDate: "2025-08-20",
      hostelOptIn,
      transportOptIn,
      hostelBuilding,
      hostelFloor,
      hostelRoomNumber,
      hostelBedNumber,
      transportRoute,
      transportStop,
      transportVehicleNumber,
      walletBalance: randomRange(50, 450),
      medicalBloodGroup: randomItem(bloodGroups),
      medicalAllergies: randomItem(allergies),
      medicalConditions: randomItem(conditions),
      medicalDoctorLogs: "Last health check: May 12, 2026. General vitals normal. Height: 156cm, Weight: 48kg.",
      status: "Active"
    });
  }
  return list;
}
export function generateHostels(students) {
  const rooms = [];
  for (let roomNo = 101; roomNo <= 112; roomNo++) {
    const floorNum = roomNo >= 110 ? "Floor 2" : "Floor 1";
    const beds = [
      { bedNumber: "Bed-1", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-2", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-3", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-4", studentId: null, studentName: void 0 }
    ];
    rooms.push({
      id: `room-boys-${roomNo}`,
      buildingName: "Vanguard Hall (Boys)",
      floor: floorNum,
      roomNumber: String(roomNo),
      capacity: 4,
      gender: "Boys",
      beds
    });
  }
  for (let roomNo = 201; roomNo <= 212; roomNo++) {
    const floorNum = roomNo >= 210 ? "Floor 2" : "Floor 1";
    const beds = [
      { bedNumber: "Bed-1", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-2", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-3", studentId: null, studentName: void 0 },
      { bedNumber: "Bed-4", studentId: null, studentName: void 0 }
    ];
    rooms.push({
      id: `room-girls-${roomNo}`,
      buildingName: "Seraphina House (Girls)",
      floor: floorNum,
      roomNumber: String(roomNo),
      capacity: 4,
      gender: "Girls",
      beds
    });
  }
  const hostelStudents = students.filter((s) => s.hostelOptIn && s.hostelRoomNumber && s.hostelBedNumber);
  hostelStudents.forEach((st) => {
    const room = rooms.find(
      (r) => r.buildingName === st.hostelBuilding && r.roomNumber === st.hostelRoomNumber
    );
    if (room) {
      const bed = room.beds.find((b) => b.bedNumber === st.hostelBedNumber);
      if (bed) {
        bed.studentId = st.id;
        bed.studentName = st.name;
      }
    }
  });
  return rooms;
}
export const initialInquiries = [
  {
    id: "inq-01",
    studentName: "Julian Smith",
    parentName: "Sarah Smith",
    phone: "+1 (555) 233-1029",
    email: "sarah.s@outlook.com",
    grade: "Grade 9",
    stage: "Inquiry",
    counselor: "David Vance",
    notes: "Inquired about sports facilities and soccer scholarship program.",
    createdAt: "2026-05-18",
    followUpDate: "2026-06-02"
  },
  {
    id: "inq-02",
    studentName: "Harriet Martinez",
    parentName: "Carlos Martinez",
    phone: "+1 (555) 781-9302",
    email: "carlos.m@martinezco.com",
    grade: "Grade 10",
    stage: "Counseling",
    counselor: "David Vance",
    notes: "Had counseling session. Concerned about organic chemistry and STEM preparation.",
    createdAt: "2026-05-12",
    followUpDate: "2026-05-31"
  },
  {
    id: "inq-03",
    studentName: "Clara Oswald",
    parentName: "John Oswald",
    phone: "+1 (555) 881-2292",
    email: "j.oswald@earth.net",
    grade: "Grade 11",
    stage: "Visit",
    counselor: "Emma Watson",
    notes: "Tour of hostel took place last Saturday. Checked female dormitory Seraphina House. Parent extremely satisfied.",
    createdAt: "2026-05-05",
    followUpDate: "2026-06-01",
    visitScheduledDate: "2026-05-27"
  },
  {
    id: "inq-04",
    studentName: "Zane Malik",
    parentName: "Yaseen Malik",
    phone: "+1 (555) 441-2831",
    email: "malik.y@yaseen.org",
    grade: "Grade 8",
    stage: "Document Verification",
    counselor: "Emma Watson",
    notes: "Awaiting birth certificate translation and physical fitness medical sheet verification.",
    createdAt: "2026-04-20",
    followUpDate: "2026-05-28"
  },
  {
    id: "inq-05",
    studentName: "Penelope Featherington",
    parentName: "Portia Featherington",
    phone: "+1 (555) 919-4112",
    email: "portia.f@mayfair.co.uk",
    grade: "Grade 12",
    stage: "Admission",
    counselor: "David Vance",
    notes: "Formal registration finalized. Security deposit receipt created. Allocated Room 204 Bed 1.",
    createdAt: "2026-04-10",
    followUpDate: "2026-05-22"
  },
  {
    id: "inq-06",
    studentName: "Leo Fitz",
    parentName: "Peter Fitz",
    phone: "+1 (555) 341-2091",
    email: "pfitz@shield.com",
    grade: "Grade 11",
    stage: "Inquiry",
    counselor: "David Vance",
    notes: "Emailed inquiries regarding campus engineering laboratory workshops.",
    createdAt: "2026-05-25",
    followUpDate: "2026-06-05"
  },
  {
    id: "inq-07",
    studentName: "Daisy Johnson",
    parentName: "Calvin Johnson",
    phone: "+1 (555) 662-3812",
    email: "calvin.quake@gmail.com",
    grade: "Grade 9",
    stage: "Counseling",
    counselor: "Emma Watson",
    notes: "Follow-up regarding remedial mathematics counseling required.",
    createdAt: "2026-05-20",
    followUpDate: "2026-05-29"
  }
];
export function generateWalletTransactions(students) {
  const trans = [];
  const categories = ["Parent Topup", "Store Purchase", "Fine Deduction", "Refund"];
  for (let i = 1; i <= 25; i++) {
    const st = students[i - 1];
    if (!st) continue;
    trans.push({
      id: `tx-${i}-1`,
      studentId: st.id,
      studentName: st.name,
      type: "Credit",
      category: "Parent Topup",
      amount: 250,
      date: `2026-05-10`,
      remarks: "Monthly Allowance from Parent"
    });
    trans.push({
      id: `tx-${i}-2`,
      studentId: st.id,
      studentName: st.name,
      type: "Debit",
      category: "Store Purchase",
      amount: 45,
      date: `2026-05-15`,
      remarks: "Purchase of custom crested navy blazer from Stationery Store"
    });
    if (i % 6 === 0) {
      trans.push({
        id: `tx-${i}-3`,
        studentId: st.id,
        studentName: st.name,
        type: "Debit",
        category: "Fine Deduction",
        amount: 20,
        date: `2026-05-18`,
        remarks: "Warden Fine: Late return from Friday Outpass"
      });
    }
  }
  return trans;
}
export function generateFeeInvoices(students) {
  const invoices = [];
  students.forEach((st, idx) => {
    const tuitionAmount = 4500;
    const isPaidTuition = idx % 3 === 0;
    const isPartialTuition = idx % 3 === 1;
    invoices.push({
      id: `inv-tuition-${idx + 1}`,
      studentId: st.id,
      studentName: st.name,
      grade: st.grade,
      feeType: "Tuition Fee",
      amount: tuitionAmount,
      paidAmount: isPaidTuition ? tuitionAmount : isPartialTuition ? 2250 : 0,
      dueDate: "2026-06-15",
      status: isPaidTuition ? "Paid" : isPartialTuition ? "Partially Paid" : "Unpaid",
      installments: [
        { dueDate: "2026-06-15", amount: 2250, paid: isPaidTuition || isPartialTuition },
        { dueDate: "2026-09-15", amount: 2250, paid: isPaidTuition }
      ],
      paymentHistory: isPaidTuition ? [
        { date: "2026-05-10", amount: tuitionAmount, method: "Bank Transfer", receiptNumber: `REC-${1204 + idx}` }
      ] : isPartialTuition ? [
        { date: "2026-05-11", amount: 2250, method: "Online Wallet", receiptNumber: `REC-${1204 + idx}` }
      ] : []
    });
    if (st.hostelOptIn) {
      const hostelAmount = 1800;
      const hostelPaid = idx % 2 === 0;
      invoices.push({
        id: `inv-hostel-${idx + 1}`,
        studentId: st.id,
        studentName: st.name,
        grade: st.grade,
        feeType: "Hostel Fee",
        amount: hostelAmount,
        paidAmount: hostelPaid ? hostelAmount : 0,
        dueDate: "2026-06-10",
        status: hostelPaid ? "Paid" : "Unpaid",
        paymentHistory: hostelPaid ? [
          { date: "2026-05-12", amount: hostelAmount, method: "Online Wallet", receiptNumber: `REC-${4839 + idx}` }
        ] : []
      });
    }
    if (st.transportOptIn) {
      const transAmount = 450;
      const transPaid = idx % 4 !== 0;
      invoices.push({
        id: `inv-trans-${idx + 1}`,
        studentId: st.id,
        studentName: st.name,
        grade: st.grade,
        feeType: "Transport Fee",
        amount: transAmount,
        paidAmount: transPaid ? transAmount : 0,
        dueDate: "2026-06-15",
        status: transPaid ? "Paid" : "Unpaid",
        paymentHistory: transPaid ? [
          { date: "2026-05-14", amount: transAmount, method: "Card", receiptNumber: `REC-${7710 + idx}` }
        ] : []
      });
    }
  });
  return invoices;
}
export function generateDisciplineViolations(students) {
  const list = [];
  const violations = [
    { type: "Curfew Breach: Sneaking snacks post roll-call", severity: "Medium", fine: 25, action: "Verbal warming and automatic fine deducted from pocket money wallet." },
    { type: "Incomplete Uniform during morning high assembly", severity: "Low", fine: 10, action: "Uniform corrected; system fine deducted." },
    { type: "Possession of prohibited high frequency game console", severity: "Medium", fine: 40, action: "Console confiscated; Parent summoned." },
    { type: "Unexcused Absence from Evening Warden Assembly", severity: "Low", fine: 15, action: "Deducted fine. Assigned extra library hour." },
    { type: "Vandalism (Writing on hostel study desk planks)", severity: "High", fine: 100, action: "Deducted $100 fine, 2 weeks suspension from outpasses." }
  ];
  for (let i = 0; i < 11; i++) {
    const student = students[i * 7] || students[0];
    const viol = violations[i % violations.length];
    list.push({
      id: `violation-${i + 1}`,
      studentId: student.id,
      studentName: student.name,
      violationType: viol.type,
      severity: viol.severity,
      incidentDate: `2026-05-${12 + i}`,
      wardenOrTeacher: i % 2 === 0 ? "Warden Brody" : "Prof. Sarah Jenkins",
      status: i === 0 ? "Suspended" : "Fine Deducted",
      fineAmount: viol.fine,
      actionsTaken: viol.action
    });
  }
  return list;
}
export const mockClasses = [
  {
    id: "class-6a",
    name: "Grade 6-A Bluebirds",
    classTeacher: "Prof. Arthur Pendelton",
    subjects: [
      { name: "Mathematics", teacher: "Prof. Liam Johnson" },
      { name: "English Literature", teacher: "Prof. Olivia Williams" },
      { name: "Computer Science", teacher: "Prof. Chloe Smith" }
    ]
  },
  {
    id: "class-7a",
    name: "Grade 7-A Pioneers",
    classTeacher: "Prof. Liam Johnson",
    subjects: [
      { name: "Mathematics", teacher: "Prof. Liam Johnson" },
      { name: "Classical Physics", teacher: "Prof. Sebastian Brown" },
      { name: "Art & Design", teacher: "Prof. Harper Sharma" }
    ]
  },
  {
    id: "class-8a",
    name: "Grade 8-A Olympians",
    classTeacher: "Prof. Olivia Williams",
    subjects: [
      { name: "World History", teacher: "Prof. Rohan Sen" },
      { name: "Mathematics", teacher: "Prof. Liam Johnson" },
      { name: "Advanced Biology", teacher: "Prof. Mia Patel" }
    ]
  },
  {
    id: "class-12s",
    name: "Grade 12-Science Elite",
    classTeacher: "Prof. Sebastian Brown",
    subjects: [
      { name: "Classical Physics", teacher: "Prof. Sebastian Brown" },
      { name: "Inorganic Chemistry", teacher: "Prof. Diya Verma" },
      { name: "Advanced Biology", teacher: "Prof. Mia Patel" }
    ]
  }
];
export const mockTimetableEvents = [
  { id: "tt-1", className: "Grade 7-A Pioneers", subject: "Mathematics", teacher: "Prof. Liam Johnson", day: "Monday", time: "08:30 AM - 09:30 AM", room: "Room 101" },
  { id: "tt-2", className: "Grade 7-A Pioneers", subject: "Classical Physics", teacher: "Prof. Sebastian Brown", day: "Monday", time: "09:40 AM - 10:40 AM", room: "Science Lab B" },
  { id: "tt-3", className: "Grade 7-A Pioneers", subject: "English Literature", teacher: "Prof. Olivia Williams", day: "Monday", time: "11:00 AM - 12:00 PM", room: "Room 101" },
  { id: "tt-4", className: "Grade 7-A Pioneers", subject: "World History", teacher: "Prof. Rohan Sen", day: "Tuesday", time: "08:30 AM - 09:30 AM", room: "Room 101" },
  { id: "tt-5", className: "Grade 7-A Pioneers", subject: "Art & Design", teacher: "Prof. Harper Sharma", day: "Tuesday", time: "09:40 AM - 10:40 AM", room: "Creative Studio" },
  { id: "tt-6", className: "Grade 12-Science Elite", subject: "Classical Physics", teacher: "Prof. Sebastian Brown", day: "Monday", time: "08:30 AM - 09:30 AM", room: "Lecture Hall 4" },
  { id: "tt-7", className: "Grade 12-Science Elite", subject: "Inorganic Chemistry", teacher: "Prof. Diya Verma", day: "Monday", time: "09:40 AM - 10:40 AM", room: "Chem Lab A" },
  { id: "tt-8", className: "Grade 12-Science Elite", subject: "Advanced Biology", teacher: "Prof. Mia Patel", day: "Tuesday", time: "11:00 AM - 12:00 PM", room: "Bio Lab 1" }
];
export const mockHomework = [
  {
    id: "hw-1",
    classId: "class-7a",
    className: "Grade 7-A Pioneers",
    subject: "Mathematics",
    title: "Algebraic Identities and factoring quadratics",
    instructions: "Solve Exercises 4.3 and 4.4 on Page 129. Submit scanned sheets with step-by-step breakdowns.",
    dueDate: "2026-06-03",
    submittedCount: 14
  },
  {
    id: "hw-2",
    classId: "class-7a",
    className: "Grade 7-A Pioneers",
    subject: "Classical Physics",
    title: "Kinetic energy vs heat equations worksheet",
    instructions: "Answer the 5 conceptual questions. Focus on the conservation of mechanical structures.",
    dueDate: "2026-06-05",
    submittedCount: 9
  },
  {
    id: "hw-3",
    classId: "class-12s",
    className: "Grade 12-Science Elite",
    subject: "Inorganic Chemistry",
    title: "D-block metal elements transitions and complex stability catalysts",
    instructions: "Complete write-up on complex ions of chromium. Focus on charge balance and electron configs.",
    dueDate: "2026-06-04",
    submittedCount: 19
  }
];
export function generateAcademicResults(students) {
  const cards = [];
  students.forEach((st, idx) => {
    if (idx < 30) {
      const MathObt = randomRange(50, 99);
      const EngObt = randomRange(65, 98);
      const SciObt = randomRange(55, 97);
      const HistObt = randomRange(50, 96);
      const obtained = MathObt + EngObt + SciObt + HistObt;
      const pct = obtained / 400 * 100;
      let overallGrade = "B";
      if (pct >= 90) overallGrade = "A+";
      else if (pct >= 80) overallGrade = "A";
      else if (pct >= 70) overallGrade = "B+";
      else if (pct >= 60) overallGrade = "C";
      cards.push({
        id: `res-midterm-${idx + 1}`,
        studentId: st.id,
        studentName: st.name,
        grade: st.grade,
        examName: "Midterm 2026",
        marks: [
          { subject: "Mathematics", maxMarks: 100, obtainedMarks: MathObt, grade: MathObt >= 90 ? "A" : MathObt >= 75 ? "B" : "C" },
          { subject: "English Literature", maxMarks: 100, obtainedMarks: EngObt, grade: EngObt >= 90 ? "A" : EngObt >= 75 ? "B" : "C" },
          { subject: "Science", maxMarks: 100, obtainedMarks: SciObt, grade: SciObt >= 90 ? "A" : SciObt >= 75 ? "B" : "C" },
          { subject: "World History", maxMarks: 100, obtainedMarks: HistObt, grade: HistObt >= 90 ? "A" : HistObt >= 75 ? "B" : "C" }
        ],
        totalPercentage: Number(pct.toFixed(1)),
        overallGrade,
        feedback: "Outstanding boarding conduct. Diligently keeps up study habits and maintains good physical room discipline."
      });
    }
  });
  return cards;
}
export function generateAttendanceHistory(students, staff) {
  const logs = [];
  const days = ["2026-05-25", "2026-05-26", "2026-05-27", "2026-05-28", "2026-05-29"];
  days.forEach((day) => {
    students.slice(0, 35).forEach((st) => {
      const stateSeed = randomRange(1, 100);
      const statusValue = stateSeed > 92 ? "Absent" : stateSeed > 87 ? "Late" : "Present";
      logs.push({
        id: `att-stud-${st.id}-${day}`,
        type: "Student Attendance",
        personId: st.id,
        personName: st.name,
        date: day,
        status: statusValue,
        timestamp: statusValue === "Present" ? "08:24 AM" : statusValue === "Late" ? "08:42 AM" : void 0
      });
    });
    students.slice(0, 35).filter((s) => s.hostelOptIn).forEach((st) => {
      const stateSeed = randomRange(1, 100);
      const statusValue = stateSeed > 95 ? "Absent" : "Present";
      logs.push({
        id: `att-night-${st.id}-${day}`,
        type: "Night Attendance",
        personId: st.id,
        personName: st.name,
        date: day,
        status: statusValue,
        timestamp: "09:05 PM"
      });
    });
    staff.forEach((sf) => {
      const stateSeed = randomRange(1, 100);
      const statusValue = stateSeed > 95 ? "Absent" : "Present";
      logs.push({
        id: `att-staff-${sf.id}-${day}`,
        type: "Staff Attendance",
        personId: sf.id,
        personName: sf.name,
        date: day,
        status: statusValue,
        timestamp: "07:55 AM"
      });
    });
  });
  return logs;
}
export const initialInventory = [
  { id: "inv-item-1", itemName: "Boarding Blazer (Navy, Medium)", category: "Uniform", stock: 125, minAlertStock: 20, unitPrice: 45, supplier: "Royal Garments Inc.", supplierContact: "contracts@royalgarments.com" },
  { id: "inv-item-2", itemName: "Boarding Blazer (Navy, Large)", category: "Uniform", stock: 85, minAlertStock: 15, unitPrice: 48, supplier: "Royal Garments Inc.", supplierContact: "contracts@royalgarments.com" },
  { id: "inv-item-3", itemName: "Official Brass Crest Badge", category: "Uniform", stock: 320, minAlertStock: 40, unitPrice: 12, supplier: "Gold Shield Craftsmen", supplierContact: "badgeorders@goldshield.net" },
  { id: "inv-item-4", itemName: "Math Rule Quadrant Notebook (120p)", category: "Stationery", stock: 48, minAlertStock: 50, unitPrice: 4.5, supplier: "Paperways Wholesale", supplierContact: "account@paperways.org" },
  { id: "inv-item-5", itemName: "Boarding Pillow Cover (Egyptian Plain)", category: "Bedding", stock: 8, minAlertStock: 15, unitPrice: 15, supplier: "Loom & Thread Co", supplierContact: "sales@loomandthread.com" },
  { id: "inv-item-6", itemName: "Antiseptic Liquid Soap (CarePro)", category: "Toiletries", stock: 110, minAlertStock: 25, unitPrice: 3.2, supplier: "CarePro Biotech Ltd", supplierContact: "b2b@carepro-bio.com" },
  { id: "inv-item-7", itemName: "Double-Bunk Wool Blanket (Grey)", category: "Bedding", stock: 40, minAlertStock: 10, unitPrice: 35, supplier: "Loom & Thread Co", supplierContact: "sales@loomandthread.com" }
];
export const initialRoutes = [
  { id: "tr-1", routeCode: "RT-ALPHA", routeName: "North Foothills Corridor", stops: ["Pines Terminal", "Whispering Valley Gate", "Cedar Falls Hub", "Academic Gate"], vehicleNumber: "BUS-402X", driverName: "Harlan Coben", driverPhone: "555-092-2281" },
  { id: "tr-2", routeCode: "RT-BETA", routeName: "Metro High Transit Link", stops: ["Union Grand Station", "West Side Crossing", "Oak Creek Green", "Sandalwood Gate"], vehicleNumber: "COACH-77Y", driverName: "Donald Sutherland", driverPhone: "555-882-9382" },
  { id: "tr-3", routeCode: "RT-GAMMA", routeName: "Southern Lakeshore Loop", stops: ["Marine Crest Parade", "Lakeside Esplanade", "Shady Grove Turn", "Athletic Complex Gate"], vehicleNumber: "BUS-110A", driverName: "Winston Churchill", driverPhone: "555-103-9122" }
];
export const initialVisitors = [
  { id: "vlog-1", visitorName: "Margaret Thatcher", relationship: "Mother", studentId: "student-2", studentName: "Noah Johnson", entryTime: "2026-05-28 14:30", exitTime: "2026-05-28 16:15", purpose: "Delivering winter boarding garments." },
  { id: "vlog-2", visitorName: "Arthur Dent", relationship: "Uncle", studentId: "student-4", studentName: "James Brown", entryTime: "2026-05-29 11:00", exitTime: "2026-05-29 13:00", purpose: "Weekend lunch pick-up authorization." }
];
export const initialOutPasses = [
  { id: "op-1", studentId: "student-3", studentName: "Oliver Williams", parentName: "Thomas Williams", reason: "Orthodontist dental crown alignment appointment in metropolis.", leaveDate: "2026-06-03 08:00", returnDate: "2026-06-03 17:00", status: "Approved", approvedBy: "Warden Brody" },
  { id: "op-2", studentId: "student-5", studentName: "Elijah Jones", parentName: "Willa Jones", reason: "Attending elder sister's wedding rehearsal dinner banquet.", leaveDate: "2026-06-05 15:00", returnDate: "2026-06-07 19:00", status: "Pending Warden" },
  { id: "op-3", studentId: "student-7", studentName: "Henry Miller", parentName: "Gwen Miller", reason: "Family celebration brunch.", leaveDate: "2026-06-10 09:00", returnDate: "2026-06-10 16:00", status: "Pending Parent" }
];
export const initialAnnouncements = [
  { id: "ann-1", title: "Curfew Timing Adjustment for Pre-Exam Prep", content: "Pre-exam silence hours are strictly instituted from 08:30 PM. All hostel study desks must be active and lights in standard rooms dimmed by 10:15 PM.", target: "Hostel", date: "2026-05-28", sender: "Warden Marcus Brody" },
  { id: "ann-2", title: "Uniform Compliance High Assembly Protocol", content: "As summer heat indices rise, linen blazers may be swapped for formal crest-stamped short-sleeve white shirts. Footwear rules remain strictly oxfords.", target: "All", date: "2026-05-27", sender: "Principal Arthur Pendelton" },
  { id: "ann-3", title: "Leave & Outpass Online Submissions Only", content: "Please register all terminal outpasses through the portal 48 hours prior. Physical chits are no longer routed for warden signature chains.", target: "Parents", date: "2026-05-26", sender: "HR Management Office" }
];
