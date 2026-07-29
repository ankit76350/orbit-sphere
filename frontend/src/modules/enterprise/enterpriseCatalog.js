const allSchoolLeaders = ["Super Admin", "School Admin", "Principal"];
const academicLeaders = [...allSchoolLeaders, "Curriculum Coordinator", "Teacher"];
const operationsLeaders = [...allSchoolLeaders, "Operations Manager"];

const spec = ({
  id,
  label,
  group,
  summary,
  roles = allSchoolLeaders,
  metrics,
  capabilities,
  workflow,
  reports,
  integrations = []
}) => ({
  id,
  label,
  group,
  summary,
  roles,
  metrics,
  capabilities,
  workflow,
  reports,
  integrations
});

export const enterpriseModules = [
  spec({
    id: "institution_setup",
    label: "Institution & Master Setup",
    group: "Foundation",
    summary: "Configure school groups, legal entities, campuses, boards, calendars, number series, locales, currencies and operating rules.",
    metrics: [["Campuses", "3"], ["Academic years", "2"], ["Configuration health", "91%"], ["Pending decisions", "7"]],
    capabilities: [
      "Organization, legal entity, campus and branch hierarchy",
      "Board affiliation and curriculum programme activation",
      "Academic years, terms, holidays, shifts and working weeks",
      "Grades, sections, houses, departments, subjects and rooms",
      "Languages, currencies, time zones and regional formats",
      "Admission, receipt, invoice and certificate number series",
      "Custom fields, configurable code sets and validation rules",
      "Versioned configuration inheritance from group to campus"
    ],
    workflow: ["Draft configuration", "Validate dependencies", "Leadership review", "Publish version", "Effective"],
    reports: ["Configuration readiness", "Campus comparison", "Master-data exceptions"],
    integrations: ["Calendar feeds", "School directory", "Data-import templates"]
  }),
  spec({
    id: "identity_access",
    label: "Identity & Access Studio",
    group: "Foundation",
    summary: "Design scoped roles, permissions, delegations, maker-checker controls and support access across every school workspace.",
    roles: ["Super Admin", "School Admin", "IT Admin", "Privacy Officer"],
    metrics: [["Active identities", "1,284"], ["Role templates", "38"], ["MFA coverage", "86%"], ["Access reviews due", "12"]],
    capabilities: [
      "RBAC plus tenant, campus, year, class, subject, dorm and route scopes",
      "Field masking for health, payroll, safeguarding, custody and identity data",
      "Maker-checker separation for money, marks, admissions and certificates",
      "Temporary delegation, substitutes and time-bound elevated access",
      "SSO, MFA, passkeys, session control and account recovery",
      "Guardian-to-child and teacher-to-class relationship enforcement",
      "Service accounts, API clients and integration scopes",
      "Periodic access certification and break-glass review"
    ],
    workflow: ["Requested", "Manager approval", "Data-owner approval", "Provisioned", "Review or revoke"],
    reports: ["Effective access", "Privilege exceptions", "Dormant accounts", "Support access audit"],
    integrations: ["SAML 2.0", "OpenID Connect", "SCIM", "Google Workspace", "Microsoft Entra ID"]
  }),
  spec({
    id: "curriculum_programmes",
    label: "Curriculum & Programmes",
    group: "Academics",
    summary: "Plan and version CBSE, ICSE, State Board, IB and Cambridge curricula, standards, outcomes, units and coverage.",
    roles: academicLeaders,
    metrics: [["Programmes", "6"], ["Outcome coverage", "84%"], ["Units in review", "19"], ["Alignment issues", "8"]],
    capabilities: [
      "Curriculum frameworks, standards, competencies and learning outcomes",
      "CBSE, ICSE, State Board, IB and Cambridge programme packs",
      "PYP programme of inquiry, MYP units, DP/CP core and CAS tracking",
      "Cambridge syllabus, component and assessment-objective mapping",
      "Collaborative unit planning, review comments and approvals",
      "Vertical and horizontal curriculum alignment",
      "Interdisciplinary planning and service-learning evidence",
      "Coverage, gap and curriculum-change impact analysis"
    ],
    workflow: ["Author", "Collaborate", "Coordinator review", "Approve", "Teach and evaluate"],
    reports: ["Outcome coverage", "Unit-plan completion", "Programme alignment", "Curriculum review history"],
    integrations: ["ManageBac", "Google Drive", "Microsoft 365", "Content repositories"]
  }),
  spec({
    id: "timetable_resources",
    label: "Timetable & Resource Scheduler",
    group: "Academics",
    summary: "Build conflict-free class, teacher, room, laboratory, elective and examination schedules with cover management.",
    roles: [...academicLeaders, "Timetable Coordinator"],
    metrics: [["Scheduled periods", "1,248"], ["Conflicts", "4"], ["Teacher utilization", "78%"], ["Covers today", "6"]],
    capabilities: [
      "Constraint-based master timetable generation",
      "Teacher workload, availability and consecutive-period limits",
      "Room, laboratory, equipment and capacity constraints",
      "Elective blocks, split groups, combined classes and rotations",
      "Recurring, fortnightly and custom week-pattern timetables",
      "Substitution, cover recommendations and absence impact",
      "Examination, activity and facility scheduling",
      "Version comparison, approval and targeted change notifications"
    ],
    workflow: ["Collect constraints", "Generate draft", "Resolve conflicts", "Approve", "Publish"],
    reports: ["Teacher workload", "Room utilization", "Conflict audit", "Substitution register"],
    integrations: ["Calendar sync", "HR leave", "Facilities booking", "OneRoster"]
  }),
  spec({
    id: "learning_platform",
    label: "Learning & Assessment Platform",
    group: "Academics",
    summary: "Deliver content, assignments, quizzes, discussions, submissions, rubrics and accessible learning experiences.",
    roles: [...academicLeaders, "Student", "Parent"],
    metrics: [["Active courses", "74"], ["Submission rate", "89%"], ["Items awaiting grade", "126"], ["Learners at risk", "18"]],
    capabilities: [
      "Courses, modules, resources and learning sequences",
      "Assignments, group work, submissions, resubmission and feedback",
      "Question bank, quizzes, online examinations and item analysis",
      "Rubrics, standards mastery and weighted grade categories",
      "Discussion, announcements and teacher-moderated collaboration",
      "Plagiarism workflow and academic-integrity evidence",
      "Captions, alternative formats and accommodation profiles",
      "Learner progress, interventions and parent visibility"
    ],
    workflow: ["Design", "Assign", "Submit", "Grade and moderate", "Publish and intervene"],
    reports: ["Missing work", "Standards mastery", "Course engagement", "Assessment item analysis"],
    integrations: ["LTI", "QTI", "OneRoster", "Google Classroom", "Microsoft Teams", "Moodle", "Turnitin"]
  }),
  spec({
    id: "governance_board",
    label: "School Governance",
    group: "Governance",
    summary: "Manage governing bodies, committees, policies, meetings, resolutions, declarations and school-improvement oversight.",
    roles: ["Super Admin", "School Admin", "Principal", "Governing Body Member", "Compliance Officer"],
    metrics: [["Committees", "8"], ["Open resolutions", "14"], ["Policies due review", "5"], ["Attendance", "92%"]],
    capabilities: [
      "Trust, society, company and school-managing-committee records",
      "Membership terms, declarations and conflicts of interest",
      "Meeting calendar, agenda packs, minutes and attendance",
      "Resolution ownership, voting and action tracking",
      "Policy authoring, consultation, approval and review cycles",
      "Strategic and school-improvement plan monitoring",
      "Accreditation evidence and inspection preparation",
      "Restricted board document repository"
    ],
    workflow: ["Propose", "Consult", "Committee review", "Approve", "Monitor"],
    reports: ["Resolution progress", "Policy review calendar", "Committee attendance", "Improvement-plan status"],
    integrations: ["eSignature", "Board portal", "Document storage"]
  }),
  spec({
    id: "general_accounting",
    label: "General Accounting & Budget",
    group: "Finance & People",
    summary: "Connect fees, payroll, procurement and operations to a controlled double-entry ledger and financial close.",
    roles: ["Super Admin", "School Admin", "Accountant", "Finance Controller", "Auditor"],
    metrics: [["Cash position", "₹48.2L"], ["Unposted batches", "9"], ["Budget consumed", "63%"], ["Reconciliation breaks", "3"]],
    capabilities: [
      "Chart of accounts, fiscal periods and cost centres",
      "Double-entry journals with immutable posting and reversal",
      "Accounts payable, receivable, cash and bank books",
      "Budget preparation, approvals and variance control",
      "Bank, gateway and petty-cash reconciliation",
      "Accruals, prepayments, deposits and period close",
      "Tax configuration for applicable ancillary services",
      "Balance sheet, income statement and cash-flow reporting"
    ],
    workflow: ["Draft batch", "Validate", "Approve", "Post", "Reconcile and close"],
    reports: ["Trial balance", "Income statement", "Balance sheet", "Cash flow", "Budget variance"],
    integrations: ["Tally", "Zoho Books", "QuickBooks", "Xero", "Bank feeds"]
  }),
  spec({
    id: "procurement_vendors",
    label: "Procurement & Vendors",
    group: "Finance & People",
    summary: "Control requisitions, budgets, quotations, purchase orders, receipts, contracts, invoices and supplier performance.",
    roles: ["Super Admin", "School Admin", "Accountant", "Procurement Officer", "Store Manager"],
    metrics: [["Open requisitions", "23"], ["PO value", "₹12.8L"], ["Receipts pending", "7"], ["Vendor SLA", "94%"]],
    capabilities: [
      "Department requisitions and budget availability checks",
      "Quotation requests, bid comparison and committee evaluation",
      "Approval matrices and segregation of duties",
      "Purchase orders, amendments and contract call-offs",
      "Goods receipt, inspection, rejection and return",
      "Three-way match between PO, receipt and invoice",
      "Vendor onboarding, compliance and performance",
      "Framework contracts, renewals and spend analytics"
    ],
    workflow: ["Requisition", "Source and compare", "Approve and order", "Receive", "Match and pay"],
    reports: ["Procurement cycle time", "Vendor performance", "Spend by category", "Open commitments"],
    integrations: ["Accounting", "Inventory", "eProcurement", "eSignature"]
  }),
  spec({
    id: "facilities_assets",
    label: "Facilities, Assets & Maintenance",
    group: "Campus Operations",
    summary: "Track spaces, equipment, custody, depreciation, inspections, preventive maintenance and service work.",
    roles: [...operationsLeaders, "Facilities Manager", "Asset Custodian"],
    metrics: [["Tracked assets", "2,418"], ["Work orders open", "31"], ["Preventive compliance", "88%"], ["Rooms booked today", "42"]],
    capabilities: [
      "Campus, building, floor, room and facility hierarchy",
      "Fixed assets, serials, custody, warranties and depreciation",
      "Resource booking for rooms, laboratories and equipment",
      "Corrective and preventive maintenance work orders",
      "AMC, vendor visits, spares and service history",
      "Inspections, meter readings and safety certificates",
      "Energy, water and sustainability tracking",
      "Capital project, lifecycle and replacement planning"
    ],
    workflow: ["Request", "Triage", "Assign", "Complete and inspect", "Close"],
    reports: ["Asset register", "Maintenance SLA", "Facility utilization", "Certificate expiry"],
    integrations: ["Procurement", "Accounting", "IoT meters", "Calendar"]
  }),
  spec({
    id: "scholarships_aid",
    label: "Scholarships & Financial Aid",
    group: "Admissions & SIS",
    summary: "Administer merit, need, RTE/EWS, sibling, staff-child, sponsor and donor-funded assistance.",
    roles: ["Super Admin", "School Admin", "Principal", "Admission Officer", "Accountant"],
    metrics: [["Applications", "86"], ["Awards active", "54"], ["Funds committed", "₹18.4L"], ["Renewals due", "11"]],
    capabilities: [
      "Aid programmes, funding sources and eligibility rules",
      "Applications, evidence, household assessment and verification",
      "RTE/EWS quota tracking and authority reporting",
      "Committee review, scoring, conflict declarations and award",
      "Fee-ledger allocation, caps and sponsor billing",
      "Merit, need, sibling and staff-child concession rules",
      "Renewal, academic conditions and withdrawal",
      "Fund utilization and donor outcome reporting"
    ],
    workflow: ["Apply", "Verify", "Committee review", "Award", "Disburse and renew"],
    reports: ["Aid distribution", "Quota readiness", "Fund utilization", "Renewal outcomes"],
    integrations: ["Admissions", "Fees", "Accounting", "Government portals"]
  }),
  spec({
    id: "recruitment_onboarding",
    label: "Recruitment & Onboarding",
    group: "Finance & People",
    summary: "Run vacancy approval, candidate screening, interviews, verification, offers, induction and probation.",
    roles: ["Super Admin", "School Admin", "Principal", "HR Manager", "Recruiter"],
    metrics: [["Open vacancies", "12"], ["Candidates", "148"], ["Offers pending", "6"], ["Time to hire", "24d"]],
    capabilities: [
      "Position control, vacancy justification and approval",
      "Career site, applications, resume and document capture",
      "Screening, interview panels and structured scorecards",
      "Reference, qualification and background verification",
      "Offer, contract, eSignature and joining workflow",
      "Employee profile and service-book creation",
      "Induction tasks, policy acknowledgement and equipment",
      "Probation goals, review and confirmation"
    ],
    workflow: ["Vacancy approved", "Source and screen", "Interview", "Offer", "Join and confirm"],
    reports: ["Recruitment funnel", "Time to hire", "Verification status", "Probation due"],
    integrations: ["Career website", "Background checks", "eSignature", "HRMS"]
  }),
  spec({
    id: "professional_development",
    label: "Staff Development & Cover",
    group: "Finance & People",
    summary: "Maintain qualifications, CPD, mandatory training, licences, teacher workload and substitute coverage.",
    roles: ["Super Admin", "School Admin", "Principal", "HR Manager", "Curriculum Coordinator"],
    metrics: [["CPD hours", "1,420"], ["Certificates expiring", "9"], ["Cover gaps", "3"], ["Training compliance", "93%"]],
    capabilities: [
      "Qualification, licence and certificate registry",
      "Mandatory training matrix and renewal reminders",
      "CPD plans, requests, budgets and evidence",
      "Teacher observation and instructional coaching",
      "Workload, contact hours and duty allocation",
      "Substitute pool, availability and suitability",
      "Absence impact and automatic cover suggestions",
      "Career pathways, succession and leadership development"
    ],
    workflow: ["Need identified", "Plan", "Approve", "Complete", "Evaluate impact"],
    reports: ["Training compliance", "Teacher workload", "Cover register", "Skills gaps"],
    integrations: ["HRMS", "Timetable", "Learning providers"]
  }),
  spec({
    id: "wellbeing_counselling",
    label: "Counselling & Wellbeing",
    group: "Student Support",
    summary: "Provide confidential counselling, wellbeing screening, referrals, interventions and follow-up with strict access controls.",
    roles: ["Super Admin", "Principal", "Counsellor", "School Psychologist", "Nurse"],
    metrics: [["Active plans", "27"], ["Appointments today", "8"], ["Follow-ups due", "14"], ["High-priority cases", "3"]],
    capabilities: [
      "Referral, self-referral and appointment intake",
      "Confidential session notes and visibility segmentation",
      "Wellbeing and SEL assessments with consent",
      "Risk triage and safeguarding escalation",
      "Goals, intervention plans and outcome reviews",
      "External specialist referral and correspondence",
      "Parent involvement rules and student confidentiality",
      "Aggregated, de-identified wellbeing trends"
    ],
    workflow: ["Referral", "Triage", "Assessment", "Intervention", "Review or close"],
    reports: ["Caseload and SLA", "Intervention outcomes", "Referral sources", "Restricted risk overview"],
    integrations: ["Safeguarding", "Health", "Attendance", "External providers"]
  }),
  spec({
    id: "inclusion_support",
    label: "Inclusion, SEN & CWSN",
    group: "Student Support",
    summary: "Manage individual plans, accommodations, therapies, assistive resources and inclusive assessment support.",
    roles: ["Super Admin", "Principal", "SENCO", "Special Educator", "Counsellor", "Teacher"],
    metrics: [["Active support plans", "42"], ["Reviews due", "7"], ["Accommodation coverage", "96%"], ["Open referrals", "11"]],
    capabilities: [
      "Needs referral, assessment and eligibility review",
      "IEP, ILP and individual accommodation plans",
      "Goals, strategies, services and evidence",
      "Special educator, therapist and aide allocation",
      "Classroom and examination accommodations",
      "Assistive technology and resource custody",
      "Parent/student participation and plan consent",
      "Progress review, transition and inclusive reporting"
    ],
    workflow: ["Concern", "Assess", "Plan and consent", "Deliver support", "Review"],
    reports: ["Plan review calendar", "Service delivery", "Goal progress", "Accommodation readiness"],
    integrations: ["Academics", "Exams", "Health", "Timetable"]
  }),
  spec({
    id: "safeguarding",
    label: "Child Safeguarding",
    group: "Student Support",
    summary: "Handle child-protection concerns, restricted evidence, risk decisions, referrals and protective actions.",
    roles: ["Super Admin", "Principal", "Safeguarding Officer", "Counsellor"],
    metrics: [["Open concerns", "9"], ["Immediate actions", "2"], ["Agency referrals", "3"], ["Reviews overdue", "1"]],
    capabilities: [
      "Confidential concern intake from staff, students and guardians",
      "Designated safeguarding lead triage and risk assessment",
      "Restricted chronology, evidence and disclosure controls",
      "POCSO and local child-protection referral workflows",
      "Protective plans, safety actions and responsible owners",
      "Allegation management and role-conflict controls",
      "Agency correspondence and legally controlled sharing",
      "Review, closure, retention and immutable access audit"
    ],
    workflow: ["Concern received", "Immediate safety triage", "Investigate or refer", "Protective plan", "Review and close"],
    reports: ["Restricted caseload", "Action timeliness", "Referral register", "Access audit"],
    integrations: ["Wellbeing", "Discipline", "Emergency", "Secure document vault"]
  }),
  spec({
    id: "houses_activities",
    label: "Houses, Sports & Activities",
    group: "Engagement",
    summary: "Coordinate houses, clubs, teams, fixtures, competitions, awards, student leadership and participation.",
    roles: [...academicLeaders, "Activities Coordinator", "Coach", "Student"],
    metrics: [["Active activities", "34"], ["Participants", "812"], ["Fixtures this month", "18"], ["House events", "9"]],
    capabilities: [
      "House membership, leadership and points",
      "Clubs, teams, eligibility and enrollment",
      "Fixtures, competitions, venues and officials",
      "Practice schedules, attendance and coaching notes",
      "Consent, medical restrictions and equipment",
      "Awards, records, certificates and achievements",
      "Service learning, CAS and community-action evidence",
      "Parent calendar and participation analytics"
    ],
    workflow: ["Propose", "Approve", "Enroll", "Run activity", "Record outcomes"],
    reports: ["Participation equity", "House standings", "Activity attendance", "Achievement register"],
    integrations: ["Calendar", "Health", "Facilities", "Documents"]
  }),
  spec({
    id: "trips_excursions",
    label: "Trips & Excursions",
    group: "Engagement",
    summary: "Plan safe educational visits with budgets, risk assessments, consent, medical clearance and live manifests.",
    roles: [...academicLeaders, "Trips Coordinator", "Parent"],
    metrics: [["Trips planned", "14"], ["Consents pending", "37"], ["Risk reviews due", "4"], ["Travellers today", "86"]],
    capabilities: [
      "Proposal, educational purpose, itinerary and cost plan",
      "Risk assessment, venue checks and approval",
      "Guardian consent and payment collection",
      "Medical, dietary and accessibility clearance",
      "Staffing ratios, duties and emergency contacts",
      "Passenger manifests, transport and accommodation",
      "Live check-in, incident and guardian communication",
      "Expense reconciliation and post-trip evaluation"
    ],
    workflow: ["Propose", "Risk and budget review", "Approve", "Consent and prepare", "Travel and close"],
    reports: ["Consent readiness", "Trip cost variance", "Safety actions", "Participation"],
    integrations: ["Payments", "Transport", "Health", "Emergency alerts"]
  }),
  spec({
    id: "career_guidance",
    label: "Career & University Guidance",
    group: "Student Support",
    summary: "Support course planning, careers, applications, recommendations, predicted grades, offers and destinations.",
    roles: ["Super Admin", "Principal", "Career Counsellor", "Teacher", "Student", "Parent"],
    metrics: [["Active applicants", "92"], ["Applications due", "24"], ["Offers received", "37"], ["Counselling tasks", "16"]],
    capabilities: [
      "Interests, strengths and career-exploration profile",
      "Subject and pathway planning against prerequisites",
      "Counsellor appointments and action plans",
      "University, course and scholarship shortlist",
      "Application tasks, essays and document checklist",
      "Predicted grades and recommendation workflow",
      "Offer, acceptance, visa and destination tracking",
      "Alumni mentoring and outcome analytics"
    ],
    workflow: ["Explore", "Plan", "Prepare", "Apply", "Offer and destination"],
    reports: ["Application pipeline", "Offer outcomes", "Destination report", "Counsellor workload"],
    integrations: ["Alumni", "Documents", "Assessment", "University platforms"]
  }),
  spec({
    id: "early_years",
    label: "Early Years & Daycare",
    group: "Student Support",
    summary: "Record daily care, developmental observations, learning stories and parent handover for younger learners.",
    roles: ["Super Admin", "School Admin", "Early Years Educator", "Nurse", "Parent"],
    metrics: [["Children checked in", "96"], ["Daily updates pending", "12"], ["Observations this week", "184"], ["Pickup changes", "5"]],
    capabilities: [
      "Check-in, room, key-worker and ratio monitoring",
      "Meals, bottles, naps, toileting and medication",
      "Developmental observations and learning stories",
      "Photo/video consent and parent sharing",
      "Milestones, concerns and support referrals",
      "Daily diary and parent handover",
      "Authorized pickup and custody restrictions",
      "Incident, illness and emergency contact workflow"
    ],
    workflow: ["Check in", "Daily care", "Observe and document", "Parent handover", "Check out"],
    reports: ["Staff-child ratio", "Daily care completion", "Development coverage", "Pickup exceptions"],
    integrations: ["Parent app", "Health", "Dismissal", "Gallery"]
  }),
  spec({
    id: "dismissal_pickup",
    label: "Dismissal & Pickup",
    group: "Campus Operations",
    summary: "Reconcile bus, parent, walker, activity and hostel dismissal with verified handover and custody rules.",
    roles: [...operationsLeaders, "Dismissal Coordinator", "Security Guard", "Parent"],
    metrics: [["Students dismissed", "742"], ["Awaiting pickup", "18"], ["Pickup changes", "23"], ["Exceptions", "4"]],
    capabilities: [
      "Daily dismissal plan and transport-mode reconciliation",
      "Authorized adult, photo, relationship and expiry",
      "Custody and court-order restrictions",
      "One-time pickup change with guardian verification",
      "QR/token handover and staff confirmation",
      "Sibling pickup, activity transfer and late room",
      "Bus-versus-parent conflict and missing-student alert",
      "Complete handover history and exception review"
    ],
    workflow: ["Plan", "Guardian change request", "Verify", "Handover", "Reconcile"],
    reports: ["Dismissal completion", "Late pickup", "Authorization exceptions", "Handover audit"],
    integrations: ["Transport", "Security", "Parent app", "Emergency"]
  }),
  spec({
    id: "emergency_crisis",
    label: "Emergency & Crisis Management",
    group: "Campus Operations",
    summary: "Coordinate drills, alerts, evacuation, lockdown, accountability, reunification and after-action reviews.",
    roles: ["Super Admin", "School Admin", "Principal", "Emergency Coordinator", "Security Guard", "Nurse"],
    metrics: [["Open incidents", "1"], ["People accounted", "98%"], ["Drills due", "2"], ["Actions overdue", "3"]],
    capabilities: [
      "Emergency plans, teams, roles and contact trees",
      "Fire, medical, weather, security and transport playbooks",
      "Mass alert with channel fallback and acknowledgement",
      "Live student, staff, visitor and contractor accountability",
      "Evacuation areas, lockdown rooms and missing-person escalation",
      "Guardian reunification and verified release",
      "Drill planning, observations and corrective actions",
      "Incident command log and after-action review"
    ],
    workflow: ["Alert", "Activate command", "Account and respond", "Reunify or recover", "Review"],
    reports: ["Accountability status", "Drill compliance", "Alert delivery", "Corrective actions"],
    integrations: ["SMS/voice", "Attendance", "Visitors", "Transport", "CCTV"]
  }),
  spec({
    id: "it_helpdesk",
    label: "IT Helpdesk & Devices",
    group: "Campus Operations",
    summary: "Manage support requests, devices, licences, assignments, repairs, knowledge articles and service levels.",
    roles: ["Super Admin", "School Admin", "IT Admin", "Helpdesk Agent"],
    metrics: [["Tickets open", "46"], ["SLA at risk", "8"], ["Devices assigned", "1,126"], ["Licences expiring", "6"]],
    capabilities: [
      "Omnichannel ticket intake, categories and priority",
      "Assignment groups, SLA timers and escalation",
      "Knowledge base, self-service and request catalogue",
      "Device inventory, assignment and acceptable-use consent",
      "Repair, loaner, warranty and disposal workflow",
      "Software licence and subscription management",
      "Network/service outage and problem management",
      "User satisfaction and support analytics"
    ],
    workflow: ["New", "Triage", "In progress", "Resolved", "Verified and closed"],
    reports: ["SLA performance", "Ticket trends", "Device custody", "Licence utilization"],
    integrations: ["Identity", "Assets", "Email", "Monitoring"]
  }),
  spec({
    id: "legal_insurance",
    label: "Legal, Insurance & Contracts",
    group: "Governance",
    summary: "Track contracts, claims, legal matters, insurance coverage, obligations, renewals and controlled evidence.",
    roles: ["Super Admin", "School Admin", "Principal", "Legal Officer", "Compliance Officer"],
    metrics: [["Active contracts", "67"], ["Renewals due", "9"], ["Claims open", "4"], ["Obligations overdue", "2"]],
    capabilities: [
      "Contract repository, parties, value and key clauses",
      "Review, negotiation, approval and eSignature",
      "Obligations, milestones, notices and renewals",
      "Insurance policies, assets, persons and coverage",
      "Claims, evidence, correspondence and settlement",
      "Legal matters, privileged access and hearing dates",
      "Vendor and data-processing agreement linkage",
      "Risk, exposure and renewal analytics"
    ],
    workflow: ["Intake", "Review", "Approve and sign", "Monitor", "Renew or close"],
    reports: ["Contract calendar", "Obligation status", "Claims exposure", "Insurance gaps"],
    integrations: ["eSignature", "Procurement", "Document vault", "Calendar"]
  }),
  spec({
    id: "workflow_forms",
    label: "Workflow & Form Builder",
    group: "Platform",
    summary: "Create versioned forms, conditional fields, checklists, approvals, SLAs and automations without code.",
    roles: ["Super Admin", "School Admin", "Workflow Designer", "IT Admin"],
    metrics: [["Published workflows", "42"], ["Runs active", "318"], ["SLA breaches", "7"], ["Automation rate", "71%"]],
    capabilities: [
      "Form schema, sections, conditional fields and validation",
      "Versioning, effective dates and draft/publish lifecycle",
      "State-machine designer with guarded transitions",
      "Sequential, parallel and committee approvals",
      "SLA timers, reminders, escalation and delegation",
      "Tasks, checklists, documents and eSignature",
      "Rules, event triggers, webhooks and scheduled actions",
      "Run history, replay, analytics and audit"
    ],
    workflow: ["Design", "Test", "Approve", "Publish", "Monitor and revise"],
    reports: ["Workflow throughput", "SLA exceptions", "Approval bottlenecks", "Automation success"],
    integrations: ["All ERP modules", "Webhooks", "eSignature", "Notification hub"]
  }),
  spec({
    id: "integration_center",
    label: "Integration Marketplace",
    group: "Platform",
    summary: "Configure standards-based connectors, mappings, sync jobs, webhooks, retries and reconciliation.",
    roles: ["Super Admin", "School Admin", "IT Admin", "Integration Manager"],
    metrics: [["Connections", "18"], ["Healthy", "15"], ["Records synced today", "48.6K"], ["Errors queued", "29"]],
    capabilities: [
      "Connector catalogue, tenant connection and secret references",
      "OneRoster, LTI, QTI and education data exchange",
      "Identity, payment, accounting and communication providers",
      "Field mapping, transformations and external identifiers",
      "Full and incremental synchronization schedules",
      "Signed webhooks, idempotency and replay",
      "Retry, dead-letter, exception resolution and reconciliation",
      "Connection health, usage and version compatibility"
    ],
    workflow: ["Connect", "Map", "Test", "Activate", "Monitor and reconcile"],
    reports: ["Sync health", "Error backlog", "Data freshness", "Connector usage"],
    integrations: ["OneRoster", "LTI", "Google", "Microsoft", "Payments", "Accounting", "Government portals"]
  }),
  spec({
    id: "privacy_center",
    label: "Privacy & Data Governance",
    group: "Governance",
    summary: "Operate notices, consent, processing inventory, privacy requests, disclosures, retention, legal holds and breaches.",
    roles: ["Super Admin", "School Admin", "Privacy Officer", "Compliance Officer"],
    metrics: [["Consent coverage", "89%"], ["Rights requests open", "6"], ["Retention jobs due", "4"], ["High-risk processors", "2"]],
    capabilities: [
      "Versioned privacy notices, purposes and lawful basis",
      "Verifiable guardian consent and withdrawal propagation",
      "Processing activity and processor/subprocessor inventory",
      "Access, correction, deletion and grievance requests",
      "Education-record disclosure and access register",
      "Retention, legal hold, deletion and anonymization evidence",
      "Data breach assessment, notification and remediation",
      "DPIA, cross-border transfer and data-residency controls"
    ],
    workflow: ["Intake", "Verify identity and scope", "Review", "Fulfil", "Evidence and close"],
    reports: ["Consent exceptions", "Rights-request SLA", "Data disclosures", "Retention execution", "Processor risk"],
    integrations: ["Identity", "Documents", "All data domains", "Notification hub"]
  }),
  spec({
    id: "ai_governance",
    label: "AI Governance & Evaluation",
    group: "Platform",
    summary: "Control AI policies, approved models, prompts, grounding, evaluations, human review, costs and incidents.",
    roles: ["Super Admin", "School Admin", "AI Administrator", "Privacy Officer", "Curriculum Coordinator"],
    metrics: [["Approved use cases", "24"], ["Runs this month", "18.4K"], ["Human approval", "96%"], ["Evaluations failing", "3"]],
    capabilities: [
      "Use-case inventory, risk tier and prohibited-use controls",
      "Tenant policies, age gates, consent and feature entitlements",
      "Provider/model catalogue, residency and retention settings",
      "Prompt, grounding source and model version registry",
      "Permission-aware retrieval and PII minimization",
      "Human review, approval, correction and appeal",
      "Accuracy, bias, safety and curriculum-alignment evaluations",
      "Usage, cost, feedback, incident and kill-switch operations"
    ],
    workflow: ["Propose use case", "Risk and privacy review", "Evaluate", "Approve deployment", "Monitor"],
    reports: ["AI usage and cost", "Evaluation scorecards", "Human override", "Safety incidents", "Model inventory"],
    integrations: ["Model providers", "Vector stores", "DLP", "Observability"]
  }),
  spec({
    id: "saas_operations",
    label: "SaaS Customer Operations",
    group: "Platform",
    summary: "Provision, meter, support, bill, migrate, restore and offboard tenant workspaces across hosting regions.",
    roles: ["Super Admin", "SaaS Operations", "Support Engineer", "Billing Admin"],
    metrics: [["Active tenants", "126"], ["MRR", "₹42.8L"], ["SLA health", "99.94%"], ["Migrations running", "4"]],
    capabilities: [
      "Tenant contract, trial, subscription and renewal lifecycle",
      "Plan catalogue, entitlements, quotas and add-ons",
      "Region, database placement and data-residency policy",
      "Workspace provisioning, custom domain and white-labeling",
      "Usage, storage, messages, integrations and AI metering",
      "Migration, backup, per-tenant restore and export",
      "Time-bound support access, approvals and customer visibility",
      "Suspension, offboarding, deletion and evidence"
    ],
    workflow: ["Contract or trial", "Provision", "Configure and migrate", "Operate and renew", "Export and offboard"],
    reports: ["MRR and renewal", "Tenant health", "Usage and margin", "SLA", "Support access"],
    integrations: ["Billing", "CRM", "Support desk", "Status page", "Cloud control plane"]
  }),
  spec({
    id: "website_cms",
    label: "Public Website & CMS",
    group: "Engagement",
    summary: "Publish admissions, calendars, news, policies, mandatory disclosures and multilingual public content.",
    roles: ["Super Admin", "School Admin", "Communications Manager", "Compliance Officer"],
    metrics: [["Published pages", "84"], ["Drafts pending", "11"], ["Disclosure health", "94%"], ["Monthly visitors", "28.6K"]],
    capabilities: [
      "Page, navigation, content block and theme management",
      "News, calendar, gallery and public notice publishing",
      "Admissions landing pages and enquiry/application forms",
      "Mandatory public disclosure and document expiry",
      "Multilingual content and translation review",
      "Author, editor, compliance and publisher approvals",
      "Accessibility, SEO, redirects and analytics",
      "Consent-aware media and scheduled archive/takedown"
    ],
    workflow: ["Draft", "Editorial review", "Compliance review", "Publish", "Review and archive"],
    reports: ["Content review calendar", "Disclosure readiness", "Accessibility issues", "Audience analytics"],
    integrations: ["Admissions", "Gallery", "Compliance", "Analytics", "CDN"]
  })
];

export const roleCatalog = [
  ["Group & governance", ["School Group Owner", "Trustee", "Governing Body Member", "Group Administrator", "Campus Administrator"]],
  ["Academic leadership", ["Principal", "Vice Principal", "Academic Coordinator", "Curriculum Coordinator", "IB Coordinator", "Cambridge Exams Officer", "Head of Department", "Grade Leader"]],
  ["Teaching", ["Class Teacher", "Subject Teacher", "Substitute Teacher", "Teaching Assistant", "Early Years Educator", "Coach"]],
  ["Admissions & front office", ["Admission Officer", "Registrar", "Admission Counsellor", "Front Desk Officer", "Records Officer"]],
  ["Finance & people", ["Finance Controller", "Accountant", "Cashier", "Auditor", "HR Manager", "Recruiter", "Payroll Officer", "Procurement Officer"]],
  ["Student support", ["Counsellor", "School Psychologist", "SENCO", "Special Educator", "Safeguarding Officer", "Nurse", "Doctor"]],
  ["Campus operations", ["Operations Manager", "Facilities Manager", "Librarian", "Transport Manager", "Driver", "Bus Attendant", "Warden", "Matron", "Security Guard", "Mess Manager", "Store Manager"]],
  ["Platform & assurance", ["IT Admin", "Helpdesk Agent", "Privacy Officer", "Compliance Officer", "AI Administrator", "Integration Manager", "Support Engineer"]],
  ["Community", ["Student", "Parent or Guardian", "Authorized Pickup Person", "Applicant", "Alumnus", "Vendor", "External Auditor"]]
];

export const workflowCatalog = [
  ["Inquiry to enrollment", "Lead → Application → Review → Offer → Acceptance → Enrollment"],
  ["Academic-year rollover", "Freeze → Copy structure → Promote → Carry balances → Publish"],
  ["Timetable publication", "Constraints → Generate → Resolve → Approve → Notify"],
  ["Attendance intervention", "Capture → Validate → Notify → Acknowledge → Intervene"],
  ["Assessment publication", "Plan → Deliver → Mark → Moderate → Lock → Publish → Appeal"],
  ["Fee collection", "Assign → Invoice → Collect → Allocate → Settle → Reconcile → Refund"],
  ["Recruit to onboard", "Vacancy → Screen → Interview → Verify → Offer → Join → Confirm"],
  ["Procure to pay", "Request → Source → Approve → Order → Receive → Match → Pay"],
  ["Safeguarding concern", "Receive → Immediate triage → Refer/investigate → Protect → Review"],
  ["Privacy request", "Receive → Verify → Locate → Review → Fulfil → Evidence"],
  ["Emergency response", "Alert → Activate → Account → Respond → Reunify → Review"],
  ["AI deployment", "Propose → Risk review → Evaluate → Approve → Monitor → Retire"]
];

export const reportCatalog = [
  ["Executive", "Enrollment, retention, attendance, attainment, finance, workforce, capacity and safeguarding indicators"],
  ["Admissions", "Source, conversion, response SLA, waitlist, yield, intake capacity and scholarship demand"],
  ["Academics", "Standards mastery, growth, grade distribution, missing work, interventions and predicted outcomes"],
  ["Finance", "Collection, aging, concession, settlement, reconciliation, budget, P&L, balance sheet and cash flow"],
  ["People", "Headcount, workload, absence, turnover, payroll, qualification and training compliance"],
  ["Campus", "Route, hostel, library, food, health, facilities, procurement and inventory performance"],
  ["Compliance", "Board readiness, evidence expiry, submissions, audit actions, consent, privacy and data quality"],
  ["SaaS", "Tenant activation, subscription, feature use, reliability, support, infrastructure and AI cost"]
];

export const integrationCatalog = [
  ["Identity", "SAML, OIDC, SCIM, Google Workspace and Microsoft Entra ID"],
  ["Learning", "OneRoster, LTI, QTI, Google Classroom, Teams, Moodle, Canvas and Turnitin"],
  ["India education", "UDISE+, PEN/APAAR, DigiLocker, CBSE SARAS/OASIS/LOC and state exports"],
  ["Payments", "UPI, Razorpay, PayU, Cashfree, Stripe, virtual accounts and mandates"],
  ["Accounting", "Tally, Zoho Books, QuickBooks, Xero and bank feeds"],
  ["Communication", "DLT SMS, WhatsApp Business, email, FCM/APNs and IVR"],
  ["Operations", "Biometric/RFID, GPS/AIS-140, maps, ONVIF/RTSP, barcode and POS"],
  ["Documents & data", "S3, Google Drive, OneDrive, eSign, webhooks, SFTP and BI connectors"]
];

export const dataDomainCatalog = [
  ["SaaS & tenant", "tenants, plans, subscriptions, entitlements, usage, domains and support sessions"],
  ["Organization", "legal entities, campuses, affiliations, programmes, academic years, terms and calendars"],
  ["Identity & people", "people, users, profiles, roles, scopes, guardian relationships, custody and pickup authority"],
  ["Admissions & enrollment", "enquiries, applications, answers, evidence, reviews, offers, enrollments and history"],
  ["Academic", "curriculum, outcomes, courses, offerings, timetable, learning, assessment, grades and attendance"],
  ["Finance", "chart of accounts, journals, fees, invoices, payments, allocations, refunds, wallets and reconciliation"],
  ["People operations", "positions, employment, contracts, leave, shifts, payroll, development and recruitment"],
  ["Campus operations", "transport, hostel, library, food, health, facilities, assets, inventory and procurement"],
  ["Engagement", "communications, PTM, diary, activities, trips, gallery, alumni and public content"],
  ["Assurance", "safeguarding, incidents, consent, privacy, compliance, audit, retention and legal holds"],
  ["Platform", "workflow, integrations, external IDs, outbox/inbox, AI runs, documents and report definitions"]
];

export const securityControlCatalog = [
  "Server-enforced tenant isolation and row-level security",
  "MFA, passkeys, SSO, session revocation and device visibility",
  "Field encryption for identity, health, counselling, custody and safeguarding data",
  "Immutable audit of access, export, approval, document view and support actions",
  "Signed object URLs, malware scanning, content validation and data-loss prevention",
  "API authorization, rate limits, idempotency, schema validation and signed webhooks",
  "Backups, point-in-time recovery, per-tenant restore, RPO/RTO and disaster tests",
  "Data residency, retention, legal holds, anonymization and deletion evidence",
  "SAST, DAST, SBOM, vulnerability management, penetration tests and security monitoring",
  "WCAG 2.2 AA, keyboard navigation, screen readers, captions and accessible documents"
];

export const aiRoadmapCatalog = [
  ["Teaching", "Curriculum-grounded plans, differentiated content, rubrics, questions, feedback and misconception analysis"],
  ["Student", "Permission-aware tutor, study planner, early warning, interventions, accessibility and career support"],
  ["Administration", "OCR, document validation, data-quality review, timetable optimization and natural-language analytics"],
  ["Finance & HR", "Reconciliation suggestions, anomaly detection, forecasting, resume extraction and cover recommendations"],
  ["Campus", "Route ETA, maintenance prediction, stock and meal forecasting, camera health and human-reviewed safety alerts"],
  ["Governance", "Policy/compliance copilot, evidence mapping, PII minimization, evaluation, human approval and incident controls"]
];

