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

export function getBirthdateWithOffset(offsetDays, birthYear) {
  const target = new Date();
  target.setDate(target.getDate() + offsetDays);
  const mm = String(target.getMonth() + 1).padStart(2, '0');
  const dd = String(target.getDate()).padStart(2, '0');
  return `${birthYear}-${mm}-${dd}`;
}

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
      reviewRating: 4.9,
      dob: getBirthdateWithOffset(0, 1968) // Birthday today
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
      reviewRating: 4.6,
      dob: getBirthdateWithOffset(2, 1978) // Birthday in 2 days (this week)
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
      reviewRating: 4.8,
      dob: getBirthdateWithOffset(0, 1982) // Birthday today
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
      reviewRating: 4.5,
      dob: getBirthdateWithOffset(22, 1975) // Birthday in 22 days (this month)
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
      reviewRating: 4.7,
      dob: getBirthdateWithOffset(-3, 1980) // Birthday 3 days ago (recently celebrated)
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
      reviewRating: 4.4,
      dob: getBirthdateWithOffset(5, 1984) // Birthday in 5 days (this week)
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
    // Give teacher 1 a birthday today, others a spread of birthdays
    const offset = i === 0 ? 0 : randomRange(-180, 180);
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
      reviewRating: Number((parseFloat(String(Math.random() * 1.5)) + 3.5).toFixed(1)),
      dob: getBirthdateWithOffset(offset, randomRange(1975, 1993))
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
      dob: i === 1 ? getBirthdateWithOffset(0, 2013) :
           i === 2 ? getBirthdateWithOffset(0, 2014) :
           i === 3 ? getBirthdateWithOffset(1, 2012) :
           i === 4 ? getBirthdateWithOffset(3, 2011) :
           i === 5 ? getBirthdateWithOffset(5, 2013) :
           i === 6 ? getBirthdateWithOffset(15, 2014) :
           i === 7 ? getBirthdateWithOffset(-2, 2012) :
           getBirthdateWithOffset(randomRange(-180, 180), randomRange(2010, 2014)),
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
    classTeacherDocsId: "staff-teacher-1",
    subjects: [
      { name: "Mathematics", teacherDocsId: "staff-teacher-2" },
      { name: "English Literature", teacherDocsId: "staff-teacher-3" },
      { name: "Computer Science", teacherDocsId: "staff-teacher-4" }
    ]
  },
  {
    id: "class-7a",
    name: "Grade 7-A Pioneers",
    classTeacherDocsId: "staff-teacher-2",
    subjects: [
      { name: "Mathematics", teacherDocsId: "staff-teacher-2" },
      { name: "Classical Physics", teacherDocsId: "staff-teacher-5" },
      { name: "Art & Design", teacherDocsId: "staff-teacher-6" }
    ]
  },
  {
    id: "class-8a",
    name: "Grade 8-A Olympians",
    classTeacherDocsId: "staff-teacher-3",
    subjects: [
      { name: "World History", teacherDocsId: "staff-teacher-7" },
      { name: "Mathematics", teacherDocsId: "staff-teacher-2" },
      { name: "Advanced Biology", teacherDocsId: "staff-teacher-8" }
    ]
  },
  {
    id: "class-12s",
    name: "Grade 12-Science Elite",
    classTeacherDocsId: "staff-teacher-5",
    subjects: [
      { name: "Classical Physics", teacherDocsId: "staff-teacher-5" },
      { name: "Inorganic Chemistry", teacherDocsId: "staff-teacher-9" },
      { name: "Advanced Biology", teacherDocsId: "staff-teacher-8" }
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

export const initialBooks = [
  {
    id: "book-1",
    title: "Advanced Algebraic Structures & Functions",
    author: "Dr. Elizabeth Sharma",
    category: "Mathematics",
    grade: "Grade 12",
    isbn: "978-3-16-148410-0",
    publishedYear: 2024,
    pages: 312,
    coverGradient: "from-blue-600 to-indigo-800",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "An in-depth guide covering matrices, complex vectors, calculus progressions, and spatial coordinate geometries."
  },
  {
    id: "book-2",
    title: "Inorganic Chemistry: Principles & Lab Protocols",
    author: "Prof. Diya Verma",
    category: "Science",
    grade: "Grade 12",
    isbn: "978-0-12-345678-6",
    publishedYear: 2025,
    pages: 420,
    coverGradient: "from-emerald-500 to-teal-700",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "Comprehensive textbook on transition metal chemistry, catalysis mechanisms, crystal field theory, and lab safety."
  },
  {
    id: "book-3",
    title: "A History of the Modern World (1789-Present)",
    author: "Prof. Rohan Sen",
    category: "History",
    grade: "Grade 10",
    isbn: "978-1-56619-909-4",
    publishedYear: 2023,
    pages: 560,
    coverGradient: "from-amber-600 to-orange-850",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "Chronicles major historical events, from the French Revolution to the rise of globalization and digital society."
  },
  {
    id: "book-4",
    title: "Introduction to Computer Science & Algorithms",
    author: "Prof. Chloe Smith",
    category: "Computer Science",
    grade: "Grade 9",
    isbn: "978-0-262-03384-8",
    publishedYear: 2024,
    pages: 280,
    coverGradient: "from-indigo-600 to-purple-800",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "Foundational programming logic, sorting/searching algorithms, data structure designs, and object-oriented paradigms."
  },
  {
    id: "book-5",
    title: "English Literature: The Anthology of Classics",
    author: "Prof. Olivia Williams",
    category: "Literature",
    grade: "Grade 8",
    isbn: "978-0-19-953556-9",
    publishedYear: 2022,
    pages: 340,
    coverGradient: "from-rose-500 to-pink-700",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "Selected poetry, short stories, essays, and dramatic works from Shakespeare to Orwell with analysis frameworks."
  },
  {
    id: "book-6",
    title: "Introductory Biology: Evolution & Cell Anatomy",
    author: "Prof. Mia Patel",
    category: "Science",
    grade: "Grade 7",
    isbn: "978-0-03-054749-3",
    publishedYear: 2023,
    pages: 260,
    coverGradient: "from-teal-600 to-emerald-800",
    pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    description: "Explore the living world: cell structure functions, genetic inheritance models, and modern evolutionary theories."
  }
];

export const initialVehicles = [
  { id: "veh-1", vehicleNo: "BUS-402X", capacity: 40, insuranceExpiry: "2026-12-15", fitnessExpiry: "2026-11-20", fuelType: "Diesel", status: "Operational", maintenanceSchedule: "2026-07-10" },
  { id: "veh-2", vehicleNo: "COACH-77Y", capacity: 55, insuranceExpiry: "2027-02-10", fitnessExpiry: "2026-09-05", fuelType: "CNG", status: "Operational", maintenanceSchedule: "2026-07-22" },
  { id: "veh-3", vehicleNo: "BUS-110A", capacity: 30, insuranceExpiry: "2026-08-30", fitnessExpiry: "2026-08-15", fuelType: "Diesel", status: "Maintenance", maintenanceSchedule: "2026-06-12" }
];

export const initialDrivers = [
  { id: "drv-1", name: "Harlan Coben", phone: "555-092-2281", licenseNo: "DL-908234-A", licenseExpiry: "2026-06-25", vehicleId: "veh-1" },
  { id: "drv-2", name: "Donald Sutherland", phone: "555-882-9382", licenseNo: "DL-381928-B", licenseExpiry: "2028-04-12", vehicleId: "veh-2" },
  { id: "drv-3", name: "Winston Churchill", phone: "555-103-9122", licenseNo: "DL-120492-C", licenseExpiry: "2026-06-15", vehicleId: "veh-3" }
];

export const initialTransportAllocations = [
  { id: "alloc-1", studentId: "student-1", studentName: "Liam Smith", routeId: "tr-1", pickupStopName: "Pines Terminal", dropStopName: "Academic Gate", feeAmount: 120, startDate: "2025-08-20", status: "Active" },
  { id: "alloc-2", studentId: "student-2", studentName: "Noah Johnson", routeId: "tr-1", pickupStopName: "Whispering Valley Gate", dropStopName: "Academic Gate", feeAmount: 100, startDate: "2025-08-20", status: "Active" },
  { id: "alloc-3", studentId: "student-3", studentName: "Oliver Williams", routeId: "tr-2", pickupStopName: "Union Grand Station", dropStopName: "Sandalwood Gate", feeAmount: 150, startDate: "2025-08-20", status: "Active" },
  { id: "alloc-4", studentId: "student-4", studentName: "James Brown", routeId: "tr-2", pickupStopName: "West Side Crossing", dropStopName: "Sandalwood Gate", feeAmount: 140, startDate: "2025-08-20", status: "Active" },
  { id: "alloc-5", studentId: "student-5", studentName: "Elijah Jones", routeId: "tr-3", pickupStopName: "Marine Crest Parade", dropStopName: "Academic Gate", feeAmount: 110, startDate: "2025-08-20", status: "Active" }
];

export const initialReviewCycles = [
  { id: "cycle-1", name: "Term 1 Academic Feedback 2026", startDate: "2026-05-01", endDate: "2026-06-30", status: "Active" },
  { id: "cycle-2", name: "Annual Performance Appraisal 2025", startDate: "2025-11-01", endDate: "2025-12-15", status: "Closed" }
];

export const initialTeacherReviews = [
  { id: "tr-rev-1", teacherId: "staff-teacher-1", studentId: "student-1", parentId: null, reviewCycleId: "cycle-1", rating: 4.8, reviewText: "Explains complex mathematical induction processes with structured simplicity. Extremely helpful in assignment review hours.", anonymous: false, created_at: "2026-05-15" },
  { id: "tr-rev-2", teacherId: "staff-teacher-1", studentId: null, parentId: "parent-2", reviewCycleId: "cycle-1", rating: 4.5, reviewText: "Very responsive during parent-teacher calls. Keeps track of student development metrics.", anonymous: true, created_at: "2026-05-20" },
  { id: "tr-rev-3", teacherId: "staff-teacher-2", studentId: "student-3", parentId: null, reviewCycleId: "cycle-1", rating: 3.8, reviewText: "Strong literature critiques, but class exercises are sometimes overly rapid.", anonymous: false, created_at: "2026-05-18" }
];

export const initialStudentReviews = [
  { id: "st-rev-1", studentId: "student-1", teacherId: "staff-teacher-1", reviewCycleId: "cycle-1", academicScore: 4.5, disciplineScore: 4.8, participationScore: 4.0, behaviorScore: 5.0, comments: "Outstanding diligence and focus on algebra homework completions. Highly cooperative.", created_at: "2026-05-22" },
  { id: "st-rev-2", studentId: "student-2", teacherId: "staff-teacher-1", reviewCycleId: "cycle-1", academicScore: 3.8, disciplineScore: 4.0, participationScore: 4.5, behaviorScore: 4.2, comments: "Great classroom engagement. Needs slight improvement in homework timelines.", created_at: "2026-05-24" }
];

export const initialTeacherPerformanceReviews = [
  { id: "perf-rev-1", teacherId: "staff-teacher-1", reviewerId: "staff-principal", reviewerRole: "Principal", rating: 4.7, comments: "Maintains exceptional classroom management. Administrative compliance and lesson plans submissions are outstanding.", reviewCycleId: "cycle-1" }
];

export const initialAlumniProfiles = [
  { id: "alum-1", name: "Sarah Jenkins", graduationYear: 2020, batch: "Class of 2020", profession: "Software Engineer", company: "Google", city: "San Francisco", country: "USA", linkedinUrl: "https://linkedin.com/in/sarah-jenkins", status: "Verified", coverGradient: "from-blue-600 to-indigo-800" },
  { id: "alum-2", name: "David Hasselhoff", graduationYear: 2021, batch: "Class of 2021", profession: "Investment Analyst", company: "Goldman Sachs", city: "New York", country: "USA", linkedinUrl: "https://linkedin.com/in/david-h", status: "Verified", coverGradient: "from-emerald-500 to-teal-700" },
  { id: "alum-3", name: "Marcus Brody", graduationYear: 2022, batch: "Class of 2022", profession: "Biotech Researcher", company: "Moderna", city: "Boston", country: "USA", linkedinUrl: "https://linkedin.com/in/marcus-b", status: "Verified", coverGradient: "from-amber-600 to-orange-850" },
  { id: "alum-4", name: "Chloe Smith", graduationYear: 2023, batch: "Class of 2023", profession: "Product Manager", company: "Microsoft", city: "Seattle", country: "USA", linkedinUrl: "https://linkedin.com/in/chloe-s", status: "Verified", coverGradient: "from-indigo-600 to-purple-800" }
];

export const initialAlumniEvents = [
  { id: "al-evt-1", title: "Decadal Grand Reunion 2026", eventType: "Annual Reunion", eventDate: "2026-11-20", location: "St. Jude Assembly Hall", description: "Celebrating a decade of excellence. Join us for banquets, networking grids, and panel discussions.", rsvps: 124 },
  { id: "al-evt-2", title: "Career Insights: AI & Softwares", eventType: "Career Talk", eventDate: "2026-07-15", location: "Online (Webinar)", description: "Alumni panel discussing future trends in software engineering and artificial intelligence pathways.", rsvps: 68 }
];

export const initialAlumniDonations = [
  { id: "don-1", alumniId: "alum-1", campaignId: "camp-scholarship", amount: 1500, donationDate: "2026-04-12", paymentReference: "REF-908234-X" },
  { id: "don-2", alumniId: "alum-2", campaignId: "camp-library", amount: 2500, donationDate: "2026-05-01", paymentReference: "REF-238492-Y" },
  { id: "don-3", alumniId: "alum-4", campaignId: "camp-scholarship", amount: 500, donationDate: "2026-05-18", paymentReference: "REF-103942-Z" }
];

export const initialMentorshipPrograms = [
  { id: "ment-1", mentorAlumniId: "alum-1", studentId: "student-1", studentName: "Liam Smith", category: "Career Guidance", status: "Active", sessionDate: "2026-06-18" },
  { id: "ment-2", mentorAlumniId: "alum-3", studentId: "student-2", studentName: "Noah Johnson", category: "College Admissions", status: "Completed", sessionDate: "2026-05-20" }
];

export const initialJobPostings = [
  { id: "job-1", alumniId: "alum-1", title: "Software Engineering Internship", company: "Google", location: "San Francisco, CA", description: "Summer internship in Google Cloud systems. Requires foundational data structures proficiency and Python/Java skills.", expiryDate: "2026-08-30" },
  { id: "job-2", alumniId: "alum-4", title: "Associate Product Manager", company: "Microsoft", location: "Redmond, WA", description: "Entry-level APM role focusing on cloud integrations and customer lifecycle tools.", expiryDate: "2026-09-15" }
];

export const initialDocumentTemplates = [
  { id: "tmpl-student-id", name: "Standard Student ID Card", category: "ID Card", templateContent: "{\"layout\":\"portrait\",\"width\":250,\"height\":400,\"fields\":[\"logo\",\"photo\",\"name\",\"student_id\",\"class\",\"qr\"]}", status: "Active" },
  { id: "tmpl-staff-id", name: "Corporate Staff ID Card", category: "ID Card", templateContent: "{\"layout\":\"portrait\",\"width\":250,\"height\":400,\"fields\":[\"logo\",\"photo\",\"name\",\"employee_id\",\"department\",\"qr\"]}", status: "Active" },
  { id: "tmpl-bonafide", name: "Bonafide Study Certificate", category: "Student Certificate", templateContent: "This is to certify that {{student_name}} (Student ID: {{student_id}}) is a bonafide student of St. Jude's Boarding School in Class {{class}}, Section {{section}} for the academic year 2025-2026.", status: "Active" },
  { id: "tmpl-transfer", name: "Transfer Certificate (TC)", category: "Student Certificate", templateContent: "Transfer Certificate for {{student_name}}. It is certified that the pupil has cleared all institutional fees and Hostel Boarding records and is hereby released for higher studies.", status: "Active" },
  { id: "tmpl-salary", name: "Employment & Salary Certificate", category: "Staff HR", templateContent: "This certificate confirms that {{staff_name}} is employed at St. Jude's in the department of {{department}} as a {{designation}}. Monthly net salary package stands at {{salary}}.", status: "Active" },
  { id: "tmpl-receipt", name: "Finance Fee Receipt", category: "Finance", templateContent: "Received with thanks from {{student_name}} (Admission No: {{admission_no}}) fee payment of {{amount}} for the term {{term}}.", status: "Active" }
];

export const initialGeneratedDocuments = [
  { id: "doc-1", documentNumber: "DOC-2026-0001", documentType: "Bonafide Certificate", entityId: "student-1", entityType: "Student", pdfUrl: "Bonafide_Liam_Smith.pdf", verificationCode: "VCODE-48293", generatedAt: "2026-06-01", status: "Valid" },
  { id: "doc-2", documentNumber: "DOC-2026-0002", documentType: "Transfer Certificate", entityId: "student-2", entityType: "Student", pdfUrl: "TC_Noah_Johnson.pdf", verificationCode: "VCODE-10294", generatedAt: "2026-06-03", status: "Valid" },
  { id: "doc-3", documentNumber: "DOC-2026-0003", documentType: "Salary Certificate", entityId: "staff-teacher-1", entityType: "Staff", pdfUrl: "Salary_Teacher_1.pdf", verificationCode: "VCODE-99081", generatedAt: "2026-06-05", status: "Valid" }
];

export const initialIdCards = [
  { id: "card-student-1", cardType: "Student", holderId: "student-1", holderName: "Liam Smith", details: "Grade 10 | Adm: STJ-1002", qrCode: "STJ-STUDENT-LIAM-SMITH", barcode: "10029384", issuedDate: "2026-01-10", expiryDate: "2027-06-30", status: "Active" },
  { id: "card-staff-1", cardType: "Staff", holderId: "staff-teacher-1", holderName: "John Doe", details: "Teacher | Math Department", qrCode: "STJ-STAFF-JOHN-DOE", barcode: "90082348", issuedDate: "2026-01-10", expiryDate: "2028-12-31", status: "Active" },
  { id: "card-parent-1", cardType: "Parent", holderId: "parent-1", holderName: "Margaret Thatcher", details: "Parent of Liam Smith", qrCode: "STJ-PARENT-MARGARET", barcode: "30082349", issuedDate: "2026-01-12", expiryDate: "2027-06-30", status: "Active" }
];

export const initialDocumentApprovals = [
  { id: "appr-1", documentId: "doc-2", documentTitle: "Transfer Certificate (Noah Johnson)", requestorId: "staff-teacher-1", requestorName: "John Doe", requestorRole: "Teacher", approverId: "staff-principal", approverName: "Chloe Smith", status: "Approved", remarks: "All academic marks and clearance approved.", approvedAt: "2026-06-03" },
  { id: "appr-2", documentId: "temp-doc-12", documentTitle: "Salary Increment Letter (John Doe)", requestorId: "staff-teacher-1", requestorName: "John Doe", requestorRole: "Teacher", approverId: "staff-principal", approverName: "Chloe Smith", status: "Pending", remarks: "Awaiting final review of HR appraisal parameters.", approvedAt: "" }
];

export const initialDocumentSignatures = [
  { id: "sig-1", signerId: "staff-principal", signerName: "Chloe Smith", designation: "Principal", signatureUrl: "Principal_Signature_V3.png", active: true },
  { id: "sig-2", signerId: "staff-accountant", signerName: "Liam Davies", designation: "Chief Accountant", signatureUrl: "Accountant_Signature_V2.png", active: true }
];

export const initialCameras = [
  { id: "cam-101", name: "Classroom 5-A Camera", cameraType: "IP RTSP", location: "Class 5-A", streamUrl: "rtsp://streams.stjude.edu/class5a", status: "Online" },
  { id: "cam-102", name: "Classroom 5-B Camera", cameraType: "ONVIF Cloud", location: "Class 5-B", streamUrl: "rtsp://streams.stjude.edu/class5b", status: "Online" },
  { id: "cam-205", name: "Classroom 10-A Camera", cameraType: "IP RTSP", location: "Class 10-A", streamUrl: "rtsp://streams.stjude.edu/class10a", status: "Online" },
  { id: "cam-hst-1", name: "Hostel Entrance A", cameraType: "NVR Local", location: "Hostel Entrance", streamUrl: "rtsp://streams.stjude.edu/hostel_ent_a", status: "Online" },
  { id: "cam-hst-2", name: "Hostel Corridor B", cameraType: "NVR Local", location: "Hostel Corridor", streamUrl: "rtsp://streams.stjude.edu/hostel_cor_b", status: "Online" },
  { id: "cam-hst-3", name: "Hostel Bathroom Common", cameraType: "ONVIF", location: "Hostel Bathroom Common (Restricted)", streamUrl: "rtsp://streams.stjude.edu/hostel_bath_common", status: "Online" },
  { id: "cam-gate", name: "Main Gate Security", cameraType: "NVR Local", location: "Main Perimeter Gate", streamUrl: "rtsp://streams.stjude.edu/main_gate", status: "Online" },
  { id: "cam-bus-12f", name: "Bus 12 Front Camera", cameraType: "4G Mobile Cam", location: "Bus 12 Front", streamUrl: "rtsp://streams.stjude.edu/bus12_front", status: "Online" },
  { id: "cam-bus-12p", name: "Bus 12 Passenger Area", cameraType: "4G Mobile Cam", location: "Bus 12 Passenger", streamUrl: "rtsp://streams.stjude.edu/bus12_pass", status: "Online" }
];

export const initialCameraGroups = [
  { id: "grp-academic", groupName: "Academic Building Wing", groupType: "Classrooms" },
  { id: "grp-hostel", groupName: "Boys & Girls Dormitories", groupType: "Hostel Monitoring" },
  { id: "grp-transport", groupName: "School Bus Fleet Cams", groupType: "Transit Cameras" },
  { id: "grp-perimeter", groupName: "Security Gates & Fences", groupType: "Gate Monitoring" }
];

export const initialCameraAssignments = [
  { id: "asg-1", cameraId: "cam-101", gradeId: "Grade 5", classId: "5-A", sectionId: "A" },
  { id: "asg-2", cameraId: "cam-102", gradeId: "Grade 5", classId: "5-B", sectionId: "B" },
  { id: "asg-3", cameraId: "cam-205", gradeId: "Grade 10", classId: "10-A", sectionId: "A" }
];

export const initialSecurityIncidents = [
  { id: "inc-1", cameraId: "cam-hst-2", incidentType: "Fight / Violence", severity: "Critical", description: "Altercation detected in B-Wing corridor between two senior pupils.", status: "Investigation", detectedAt: "2026-06-08 14:32:00" },
  { id: "inc-2", cameraId: "cam-gate", incidentType: "Unauthorized Entry", severity: "High", description: "Tailgating event registered at vehicle access gate barrier.", status: "Resolved", detectedAt: "2026-06-09 09:15:00" },
  { id: "inc-3", cameraId: "cam-hst-1", incidentType: "Suspicious Activity", severity: "Low", description: "Loitering behavior flagged at hostel rear access door past midnight.", status: "Alert", detectedAt: "2026-06-10 01:04:00" }
];

export const initialCameraRecordings = [
  { id: "rec-1", cameraId: "cam-205", recordingUrl: "recording_class10a_0609.mp4", startTime: "2026-06-09 09:00:00", endTime: "2026-06-09 10:30:00" },
  { id: "rec-2", cameraId: "cam-gate", recordingUrl: "recording_gate_0608.mp4", startTime: "2026-06-08 08:00:00", endTime: "2026-06-08 18:00:00" }
];

export const initialOnlineClasses = [
  { id: "class-1", subjectId: "Math", subjectName: "Grade 10 Mathematics", teacherId: "staff-teacher-1", teacherName: "John Doe", classId: "10-A", sectionId: "A", meetingLink: "https://meet.google.com/abc-defg-hij", date: "2026-06-12", startTime: "09:00", endTime: "10:00", status: "Upcoming" },
  { id: "class-2", subjectId: "Science", subjectName: "Grade 10 Science", teacherId: "staff-teacher-1", teacherName: "John Doe", classId: "10-A", sectionId: "A", meetingLink: "https://meet.google.com/xyz-pdqk-wuv", date: "2026-06-10", startTime: "10:30", endTime: "11:30", status: "Live" },
  { id: "class-3", subjectId: "History", subjectName: "Grade 5 History", teacherId: "staff-teacher-2", teacherName: "Jane Smith", classId: "5-A", sectionId: "A", meetingLink: "https://meet.google.com/mnp-qrst-uvw", date: "2026-06-09", startTime: "14:00", endTime: "15:00", status: "Completed" }
];

export const initialClassRecordings = [
  { id: "rec-cls-3", classId: "class-3", recordingUrl: "https://storage.googleapis.com/stjude-classes/history_g5_0609.mp4", transcript: "Speaker 1 (Jane Smith): Welcome class. Today we discuss the origins of the Roman Republic, founded in 509 BC after the overthrow of the Roman monarchy. Speaker 2 (Liam Smith): Did they have elections back then? Speaker 1: Yes, the citizens elected their representatives, forming the early Senate.", summary: "Introduction to the Roman Republic. Key points: Founded in 509 BC, overthrow of monarchic rule, senate representation, and Roman citizen voting rights." }
];

export const initialAiNotes = [
  { id: "note-cls-3", classId: "class-3", notesContent: "St. Jude's AI Notes Generator - Session G5 History. Key Concepts: Roman Republic. Important Dates: 509 BC (Republic Foundation). Definitions: Senate (Council of senior Roman representatives). Flashcards: 1. Q: When was Roman Republic founded? A: 509 BC. Mind Map: Early Rome -> Monarchy -> 509 BC Republic -> Senate -> Consuls.", generatedAt: "2026-06-09 15:15:00" }
];

export const initialGalleryAlbums = [
  { id: "alb-annual", title: "Decadal Annual Day Celebration", eventType: "Cultural Program", coverImage: "annual_day_cover.jpg", status: "Approved" },
  { id: "alb-sports", title: "Inter-House Sports Championship", eventType: "Sports Day", coverImage: "sports_day_cover.jpg", status: "Approved" },
  { id: "alb-science", title: "Innovative Science Fair 2026", eventType: "Science Exhibition", coverImage: "science_fair_cover.jpg", status: "Pending" }
];

export const initialGalleryMedia = [
  { id: "med-1", albumId: "alb-annual", mediaType: "Photo", mediaUrl: "https://images.unsplash.com/photo-1511578314322-379afb476865" },
  { id: "med-2", albumId: "alb-sports", mediaType: "Photo", mediaUrl: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211" },
  { id: "med-3", albumId: "alb-science", mediaType: "Photo", mediaUrl: "https://images.unsplash.com/photo-1507679799987-c73779587ccf" }
];




