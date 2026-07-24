/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getAlumniProfiles,
  saveAlumniProfiles,
  getAlumniEvents,
  saveAlumniEvents,
  getAlumniDonations,
  saveAlumniDonations,
  getMentorshipPrograms,
  saveMentorshipPrograms,
  getJobPostings,
  saveJobPostings,
  getStudents,
  getStaff,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Trophy,
  Search,
  Plus,
  Heart,
  Award,
  Briefcase,
  Calendar,
  MapPin,
  Linkedin,
  CheckCircle,
  TrendingUp,
  Users,
  DollarSign,
  BookOpen,
  UserCheck,
  Clock,
  ChevronRight,
  Book,
  Send,
  UploadCloud,
  FileText
} from "lucide-react";

const DEFAULT_CAMPAIGNS = [
  { id: "camp-scholarship", title: "Merit Scholarship Fund", target: 15000, description: "Supporting deserving students from underrepresented backgrounds with full/partial tuitions." },
  { id: "camp-library", title: "Modern Library Extension", target: 25000, description: "Procuring digital catalog systems, e-readers, and advanced journal access subscriptions." },
  { id: "camp-sports", title: "Indoor Sports Complex Upgrade", target: 30000, description: "Refurbishing the synthetic badminton courts and gymnastics floor mats." }
];

export default function ModAlumni({ user }) {
  const { addToast } = useToast();

  // State databases from LocalStorage
  const [profiles, setProfiles] = useState(() => getAlumniProfiles());
  const [events, setEvents] = useState(() => getAlumniEvents());
  const [donations, setDonations] = useState(() => getAlumniDonations());
  const [mentorships, setMentorships] = useState(() => getMentorshipPrograms());
  const [jobs, setJobs] = useState(() => getJobPostings());
  const [students] = useState(() => getStudents());
  const [staff] = useState(() => getStaff());

  // Campaigns state
  const [campaigns, setCampaigns] = useState(() => {
    const stored = localStorage.getItem("erp_alumni_campaigns");
    return stored ? JSON.parse(stored) : DEFAULT_CAMPAIGNS;
  });

  const saveCampaigns = (newCampaigns) => {
    setCampaigns(newCampaigns);
    localStorage.setItem("erp_alumni_campaigns", JSON.stringify(newCampaigns));
  };

  // Navigation state
  // Options: dashboard, directory, mentorship, events, donations, jobs
  const [activeTab, setActiveTab] = useState("dashboard");

  // Modal display toggles
  const [isRegisterAlumniOpen, setIsRegisterAlumniOpen] = useState(false);
  const [isRequestMentorshipOpen, setIsRequestMentorshipOpen] = useState(false);
  const [isCreateEventOpen, setIsCreateEventOpen] = useState(false);
  const [isDonateOpen, setIsDonateOpen] = useState(false);
  const [isCreateCampaignOpen, setIsCreateCampaignOpen] = useState(false);
  const [isPostJobOpen, setIsPostJobOpen] = useState(false);
  const [isApplyJobOpen, setIsApplyJobOpen] = useState(false);

  // Selected details
  const [selectedJob, setSelectedJob] = useState(null);

  // Filter & Search states (Directory Tab)
  const [searchQuery, setSearchQuery] = useState("");
  const [filterYear, setFilterYear] = useState("All");
  const [filterCountry, setFilterCountry] = useState("All");

  // Filter & Search states (Job Tab)
  const [jobSearch, setJobSearch] = useState("");

  // Form states
  // 1. Alumni Register Form
  const [newName, setNewName] = useState("");
  const [newYear, setNewYear] = useState(new Date().getFullYear());
  const [newProfession, setNewProfession] = useState("");
  const [newCompany, setNewCompany] = useState("");
  const [newCity, setNewCity] = useState("");
  const [newCountry, setNewCountry] = useState("");
  const [newLinkedin, setNewLinkedin] = useState("");

  // 2. Mentorship Booking Form
  const [selectedMentorId, setSelectedMentorId] = useState("");
  const [selectedStudentDocsId, setSelectedStudentDocsId] = useState("");
  const [mentorshipCategory, setMentorshipCategory] = useState("Career Guidance");
  const [mentorshipDate, setMentorshipDate] = useState("");

  // 3. Reunion Event Form
  const [eventTitle, setEventTitle] = useState("");
  const [eventType, setEventType] = useState("Annual Reunion");
  const [eventDate, setEventDate] = useState("");
  const [eventLocation, setEventLocation] = useState("");
  const [eventDesc, setEventDesc] = useState("");

  // 4. Donation Form
  const [donorName, setDonorName] = useState(user?.name || "");
  const [donationAmount, setDonationAmount] = useState("");
  const [selectedCampaignDocsId, setSelectedCampaignDocsId] = useState("");
  const [paymentRef, setPaymentRef] = useState("");

  // 5. Campaign Creation Form
  const [campaignTitle, setCampaignTitle] = useState("");
  const [campaignTarget, setCampaignTarget] = useState("");
  const [campaignDesc, setCampaignDesc] = useState("");

  // 6. Job Referral Posting Form
  const [jobTitle, setJobTitle] = useState("");
  const [jobCompany, setJobCompany] = useState("");
  const [jobLocation, setJobLocation] = useState("");
  const [jobDesc, setJobDesc] = useState("");
  const [jobExpiry, setJobExpiry] = useState("");

  // 7. Job Application Form
  const [applicantName, setApplicantName] = useState(user?.name || "");
  const [applicantEmail, setApplicantEmail] = useState("");
  const [applicantCoverNote, setApplicantCoverNote] = useState("");
  const [resumeName, setResumeName] = useState("");

  // Role Validation
  const isManagement = user?.role === "Super Admin" || user?.role === "Principal" || user?.role === "Warden";
  const isAlumni = user?.role === "Alumni";

  // Pre-fill student dropdown if available
  useEffect(() => {
    if (students?.length > 0) {
      setSelectedStudentDocsId(students[0].id);
    }
  }, [students]);

  // Pre-fill mentor dropdown
  useEffect(() => {
    const mentors = profiles.filter(p => p.status === "Verified");
    if (mentors.length > 0) {
      setSelectedMentorId(mentors[0].id);
    }
  }, [profiles]);

  // Pre-fill campaign dropdown
  useEffect(() => {
    if (campaigns?.length > 0) {
      setSelectedCampaignDocsId(campaigns[0].id);
    }
  }, [campaigns]);

  // Dynamic calculations for Dashboard
  const totalAlumniCount = profiles.length;
  const verifiedAlumniCount = profiles.filter(p => p.status === "Verified").length;
  const totalDonationsAmount = donations.reduce((sum, d) => sum + Number(d.amount), 0);
  const activeMentorshipsCount = mentorships.filter(m => m.status === "Active").length;
  const jobReferralsCount = jobs.length;

  // Calculate campaign funds raised
  const getRaisedAmountForCampaign = (campaignDocsId) => {
    return donations
      .filter(d => d.campaignDocsId === campaignDocsId)
      .reduce((sum, d) => sum + Number(d.amount), 0);
  };

  // HANDLERS
  // 1. Verify/Unverify Alumni Profile
  const handleToggleVerification = (profileId) => {
    const updated = profiles.map(p => {
      if (p.id === profileId) {
        const nextStatus = p.status === "Verified" ? "Pending" : "Verified";
        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          `Alumni Profile ${nextStatus}`,
          `Verification status updated for: ${p.name}`
        );
        addToast(
          `Profile ${nextStatus}`,
          `${p.name} is now marked as ${nextStatus}.`
        );
        return { ...p, status: nextStatus };
      }
      return p;
    });
    setProfiles(updated);
    saveAlumniProfiles(updated);
  };

  // 2. Register Alumni
  const handleRegisterAlumni = (e) => {
    e.preventDefault();
    if (!newName || !newProfession || !newCompany) {
      addToast("Failed to Register", "Please fill in all mandatory fields.", "error");
      return;
    }

    const gradients = [
      "from-blue-600 to-indigo-800",
      "from-emerald-500 to-teal-700",
      "from-amber-600 to-orange-850",
      "from-indigo-600 to-purple-800",
      "from-rose-500 to-pink-700"
    ];
    const randomGradient = gradients[Math.floor(Math.random() * gradients.length)];

    const newAlum = {
      id: `alum-${Date.now()}`,
      name: newName,
      graduationYear: Number(newYear),
      batch: `Class of ${newYear}`,
      profession: newProfession,
      company: newCompany,
      city: newCity || "Unknown",
      country: newCountry || "Global",
      linkedinUrl: newLinkedin || "https://linkedin.com",
      status: "Verified", // Automatically verify registration done by admin/staff
      coverGradient: randomGradient
    };

    const nextList = [newAlum, ...profiles];
    setProfiles(nextList);
    saveAlumniProfiles(nextList);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Alumni Profile Registered",
      `Created database record for graduate: ${newName}`
    );

    addToast("Registration Success", `${newName} successfully registered and verified.`);
    setIsRegisterAlumniOpen(false);

    // Reset Form
    setNewName("");
    setNewProfession("");
    setNewCompany("");
    setNewCity("");
    setNewCountry("");
    setNewLinkedin("");
  };

  // 3. Book Mentorship Guidance Session
  const handleBookMentorship = (e) => {
    e.preventDefault();
    if (!selectedMentorId || !mentorshipDate) {
      addToast("Failed to Book", "Select a mentor and session target date.", "error");
      return;
    }

    const mentor = profiles.find(p => p.id === selectedMentorId);
    const student = students.find(s => s.id === selectedStudentDocsId);
    const studentName = student ? student.name : "St. Jude Student";

    const newBooking = {
      id: `ment-${Date.now()}`,
      mentorAlumniDocsId: selectedMentorId,
      studentDocsId: selectedStudentDocsId || "student-1",
      studentName: studentName,
      category: mentorshipCategory,
      status: "Active",
      sessionDate: mentorshipDate
    };

    const nextBookings = [newBooking, ...mentorships];
    setMentorships(nextBookings);
    saveMentorshipPrograms(nextBookings);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Mentorship Session Scheduled",
      `Booked mentorship session with ${mentor?.name} for student ${studentName}`
    );

    addToast("Session Booked", `Mentorship requested with ${mentor?.name} for ${mentorshipDate}`);
    setIsRequestMentorshipOpen(false);
    setMentorshipDate("");
  };

  // Toggle Mentorship status
  const handleToggleMentorshipStatus = (bookingId) => {
    const updated = mentorships.map(m => {
      if (m.id === bookingId) {
        const nextStatus = m.status === "Active" ? "Completed" : "Active";
        addToast("Mentorship Updated", `Session marked as ${nextStatus}.`);
        return { ...m, status: nextStatus };
      }
      return m;
    });
    setMentorships(updated);
    saveMentorshipPrograms(updated);
  };

  // 4. Create Reunion Event
  const handleCreateEvent = (e) => {
    e.preventDefault();
    if (!eventTitle || !eventDate || !eventLocation) {
      addToast("Form Incomplete", "Please complete all event details.", "error");
      return;
    }

    const newEvt = {
      id: `al-evt-${Date.now()}`,
      title: eventTitle,
      eventType: eventType,
      eventDate: eventDate,
      location: eventLocation,
      description: eventDesc,
      rsvps: 0
    };

    const nextEvts = [newEvt, ...events];
    setEvents(nextEvts);
    saveAlumniEvents(nextEvts);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Alumni Event Published",
      `Organized reunion/event: "${eventTitle}"`
    );

    addToast("Reunion Published", `Event "${eventTitle}" successfully announced.`);
    setIsCreateEventOpen(false);

    // Reset Form
    setEventTitle("");
    setEventDate("");
    setEventLocation("");
    setEventDesc("");
  };

  // RSVP to Event
  const [myRsvps, setMyRsvps] = useState([]);
  const handleRsvp = (eventId) => {
    if (myRsvps.includes(eventId)) {
      addToast("Already Registered", "You have already RSVPed to this event.", "info");
      return;
    }

    const updated = events.map(e => {
      if (e.id === eventId) {
        return { ...e, rsvps: e.rsvps + 1 };
      }
      return e;
    });

    setEvents(updated);
    saveAlumniEvents(updated);
    setMyRsvps(prev => [...prev, eventId]);

    addToast("RSVP Confirmed", "Your seat registration has been successfully recorded.");
  };

  // 5. Submit Donation
  const handleSubmitDonation = (e) => {
    e.preventDefault();
    if (!donationAmount || !selectedCampaignDocsId) {
      addToast("Failed to Process", "Specify donation amount and select target campaign.", "error");
      return;
    }

    const amountNum = Number(donationAmount);
    if (isNaN(amountNum) || amountNum <= 0) {
      addToast("Invalid Amount", "Please enter a valid donation amount.", "error");
      return;
    }

    const campaign = campaigns.find(c => c.id === selectedCampaignDocsId);
    const generatedRef = paymentRef || `REF-${Math.floor(100000 + Math.random() * 900000)}-AL`;

    const newDonation = {
      id: `don-${Date.now()}`,
      alumniDocsId: "sandbox-donor",
      alumniName: donorName || "Generous Supporter",
      campaignDocsId: selectedCampaignDocsId,
      amount: amountNum,
      donationDate: new Date().toISOString().split("T")[0],
      paymentReference: generatedRef
    };

    const nextDonations = [newDonation, ...donations];
    setDonations(nextDonations);
    saveAlumniDonations(nextDonations);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Donation Processed",
      `Received contribution of $${amountNum} for ${campaign?.title}. Ref: ${generatedRef}`
    );

    addToast("Thank You!", `Donation of $${amountNum} received for ${campaign?.title}. Receipt generated.`);
    setIsDonateOpen(false);

    // Reset Form
    setDonationAmount("");
    setPaymentRef("");
  };

  // 6. Create Donation Campaign
  const handleCreateCampaign = (e) => {
    e.preventDefault();
    if (!campaignTitle || !campaignTarget) {
      addToast("Incomplete Form", "Campaign title and financial targets are required.", "error");
      return;
    }

    const targetNum = Number(campaignTarget);
    if (isNaN(targetNum) || targetNum <= 0) {
      addToast("Invalid Target", "Target must be a positive integer.", "error");
      return;
    }

    const newCampaign = {
      id: `camp-${Date.now()}`,
      title: campaignTitle,
      target: targetNum,
      description: campaignDesc || "Supporting St. Jude developments and student cohorts."
    };

    const nextCampaigns = [...campaigns, newCampaign];
    saveCampaigns(nextCampaigns);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Donation Campaign Created",
      `Initiated funding target for: "${campaignTitle}"`
    );

    addToast("Campaign Launched", `Donation campaign "${campaignTitle}" is now live.`);
    setIsCreateCampaignOpen(false);

    // Reset Form
    setCampaignTitle("");
    setCampaignTarget("");
    setCampaignDesc("");
  };

  // 7. Post Job Referral
  const handlePostJob = (e) => {
    e.preventDefault();
    if (!jobTitle || !jobCompany || !jobLocation || !jobExpiry) {
      addToast("Fields Required", "Please fill in all details for job referral listing.", "error");
      return;
    }

    const newJob = {
      id: `job-${Date.now()}`,
      alumniDocsId: "sandbox-poster",
      referrerName: user?.name || "St. Jude Alumni",
      title: jobTitle,
      company: jobCompany,
      location: jobLocation,
      description: jobDesc,
      expiryDate: jobExpiry
    };

    const nextJobs = [newJob, ...jobs];
    setJobs(nextJobs);
    saveJobPostings(nextJobs);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Job Referral Posted",
      `Shared career opening: "${jobTitle}" at ${jobCompany}`
    );

    addToast("Opportunity Shared", `Career referral for "${jobTitle}" successfully published.`);
    setIsPostJobOpen(false);

    // Reset Form
    setJobTitle("");
    setJobCompany("");
    setJobLocation("");
    setJobDesc("");
    setJobExpiry("");
  };

  // 8. Apply for Referral / Resume Submission
  const handleApplyJob = (e) => {
    e.preventDefault();
    if (!applicantEmail || !resumeName) {
      addToast("Details Required", "Provide contact email and upload resume to apply.", "error");
      return;
    }

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Referral Application Submitted",
      `Applied for referral to "${selectedJob?.title}" at ${selectedJob?.company}`
    );

    addToast(
      "Application Sent",
      `Your credentials and resume "${resumeName}" have been routed to the alumni referrer.`
    );
    setIsApplyJobOpen(false);

    // Reset Form
    setApplicantEmail("");
    setApplicantCoverNote("");
    setResumeName("");
  };

  // Filter Directory graduates
  const filteredProfiles = profiles.filter(p => {
    const matchesSearch =
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.profession.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.company.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.country.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesYear = filterYear === "All" || p.graduationYear.toString() === filterYear;
    const matchesCountry = filterCountry === "All" || p.country === filterCountry;

    return matchesSearch && matchesYear && matchesCountry;
  });

  // Filter Jobs listing
  const filteredJobs = jobs.filter(j => {
    return (
      j.title.toLowerCase().includes(jobSearch.toLowerCase()) ||
      j.company.toLowerCase().includes(jobSearch.toLowerCase()) ||
      j.location.toLowerCase().includes(jobSearch.toLowerCase())
    );
  });

  // Unique lists for filter dropdowns
  const uniqueYears = Array.from(new Set(profiles.map(p => p.graduationYear))).sort((a, b) => b - a);
  const uniqueCountries = Array.from(new Set(profiles.map(p => p.country))).sort();

  return (
    <div className="space-y-6">
      {/* 1. Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-105 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Trophy className="h-6 w-6 text-amber-500 animate-pulse" />
            MOD-17: Alumni Management & Networks
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Maintain lifework directories, track placements, organize reunions, pledge endowments, and access referral mentorships.
          </p>
        </div>

        {/* User Role Sandbox Tag */}
        <div className="flex items-center gap-2 bg-slate-50 border border-slate-150 px-3 py-1.5 rounded-xl">
          <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
            ROLE: {user?.role || "Visitor"}
          </Badge>
        </div>
      </div>

      {/* 2. Custom Tabs Navigation */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "dashboard" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Portal Summary
        </button>
        <button
          onClick={() => setActiveTab("directory")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "directory" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Alumni Directory
        </button>
        <button
          onClick={() => setActiveTab("mentorship")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "mentorship" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Mentorship Guidance
        </button>
        <button
          onClick={() => setActiveTab("events")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "events" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Reunions & Events
        </button>
        <button
          onClick={() => setActiveTab("donations")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "donations" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Endowment campaigns
        </button>
        <button
          onClick={() => setActiveTab("jobs")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "jobs" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Referral Job Board
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
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Total Registered Alumni</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{totalAlumniCount} Profiles</p>
              <span className="text-[10px] text-emerald-650 font-semibold block mt-1">✓ {verifiedAlumniCount} Verified Members</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Endowments Contributed</span>
              <p className="text-xl font-bold text-slate-800 mt-1">${totalDonationsAmount.toLocaleString()}</p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">across {donations.length} receipts</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Active Mentors matching</span>
              <p className="text-xl font-bold text-slate-800 mt-1">{activeMentorshipsCount} Sessions</p>
              <span className="text-[10px] text-blue-600 font-semibold block mt-1">career path advisories</span>
            </div>
            <div className="bg-white border border-slate-100 p-5 rounded-2xl shadow-xs">
              <span className="text-[9px] uppercase tracking-wider font-extrabold text-slate-400">Placement Success Rate</span>
              <p className="text-xl font-bold text-emerald-600 mt-1">96.8% Success</p>
              <span className="text-[10px] text-slate-400 font-semibold block mt-1">{jobReferralsCount} open job referrals</span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Donation Campaigns Snapshot */}
            <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
              <div className="flex justify-between items-center border-b pb-3 border-slate-50">
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-1.5">
                  <Heart className="h-4 w-4 text-rose-500" /> Donation Progress Leaderboard
                </h3>
                <Button onClick={() => setActiveTab("donations")} className="text-[10px] px-2.5 py-1.5 bg-slate-50 text-slate-650 hover:bg-slate-100 border border-slate-150 rounded-lg">
                  View Campaigns
                </Button>
              </div>

              <div className="space-y-4">
                {campaigns.slice(0, 3).map(campaign => {
                  const raised = getRaisedAmountForCampaign(campaign.id);
                  const percent = Math.min(Math.round((raised / campaign.target) * 100), 100);
                  return (
                    <div key={campaign.id} className="space-y-2">
                      <div className="flex justify-between text-xs font-bold">
                        <span className="text-slate-800">{campaign.title}</span>
                        <span className="text-slate-500 font-mono">${raised.toLocaleString()} / ${campaign.target.toLocaleString()} ({percent}%)</span>
                      </div>
                      <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
                        <div
                          className="bg-blue-600 h-full rounded-full transition-all duration-500"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Quick Referrals feed */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-1.5 border-b pb-3 border-slate-50">
                <Briefcase className="h-4 w-4 text-blue-500" /> Career Placements Board
              </h3>
              <div className="space-y-3 max-h-[220px] overflow-y-auto pr-1">
                {jobs.length === 0 ? (
                  <p className="text-xs text-slate-400 italic">No job postings shared yet.</p>
                ) : (
                  jobs.map(job => (
                    <div key={job.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl space-y-1.5">
                      <div className="flex justify-between items-start">
                        <div>
                          <h4 className="text-xs font-black text-slate-850">{job.title}</h4>
                          <p className="text-[10px] text-slate-400 font-bold">{job.company} • {job.location}</p>
                        </div>
                        <Badge variant="info" className="text-[9px] font-black uppercase">
                          Referral
                        </Badge>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          2. ALUMNI DIRECTORY TAB
          ======================================================== */}
      {activeTab === "directory" && (
        <div className="space-y-5">
          {/* Controls Panel */}
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search name, company, country, batch..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition"
              />
            </div>

            <div className="flex flex-wrap gap-3 w-full md:w-auto">
              <div className="w-32">
                <Select
                  options={[{ label: "All Years", value: "All" }, ...uniqueYears.map(y => ({ label: y.toString(), value: y.toString() }))]}
                  value={filterYear}
                  onChange={(e) => setFilterYear(e.target.value)}
                  className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                />
              </div>
              <div className="w-36">
                <Select
                  options={[{ label: "All Countries", value: "All" }, ...uniqueCountries.map(c => ({ label: c, value: c }))]}
                  value={filterCountry}
                  onChange={(e) => setFilterCountry(e.target.value)}
                  className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                />
              </div>

              {isManagement && (
                <Button
                  onClick={() => setIsRegisterAlumniOpen(true)}
                  className="text-xs py-1 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs"
                >
                  <Plus className="h-4 w-4 mr-1.5 shrink-0" /> Register Alumni
                </Button>
              )}
            </div>
          </div>

          {/* Directory Profiles Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
            {filteredProfiles.length === 0 ? (
              <div className="col-span-full bg-white p-12 text-center border border-slate-100 rounded-2xl">
                <p className="text-slate-400 text-sm font-semibold italic">No alumni profiles matched the current filters.</p>
              </div>
            ) : (
              filteredProfiles.map(profile => (
                <div key={profile.id} className="bg-white rounded-2xl border border-slate-150 overflow-hidden hover:shadow-lg transition duration-300 flex flex-col justify-between h-[280px]">
                  <div>
                    {/* Header gradient banner */}
                    <div className={`h-16 bg-gradient-to-r ${profile.coverGradient || "from-slate-600 to-slate-800"} flex items-end p-3 relative`}>
                      {profile.status === "Verified" && (
                        <div className="absolute right-3 top-3 bg-white/90 backdrop-blur-xs p-1 rounded-full text-blue-600" title="Verified Alumni Profile">
                          <CheckCircle className="h-4 w-4 fill-blue-100" />
                        </div>
                      )}
                    </div>

                    <div className="px-4 pt-3 space-y-1.5">
                      <h4 className="text-sm font-black text-slate-805 truncate">{profile.name}</h4>
                      <p className="text-[10px] text-blue-650 font-bold uppercase tracking-widest">{profile.batch}</p>

                      <div className="space-y-1 pt-1.5">
                        <p className="text-xs text-slate-700 font-semibold flex items-center gap-1.5">
                          <Briefcase className="h-3.5 w-3.5 text-slate-450 shrink-0" />
                          <span className="truncate">{profile.profession} at <strong className="text-slate-850 font-extrabold">{profile.company}</strong></span>
                        </p>
                        <p className="text-xs text-slate-400 font-medium flex items-center gap-1.5">
                          <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span className="truncate">{profile.city}, {profile.country}</span>
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 pt-0 space-y-2">
                    <div className="flex gap-2">
                      <a
                        href={profile.linkedinUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="flex-1 text-center py-1.5 border border-slate-250 hover:border-slate-350 text-slate-650 hover:text-slate-900 text-[10px] font-bold rounded-xl transition flex items-center justify-center gap-1.5"
                      >
                        <Linkedin className="h-3.5 w-3.5 text-blue-600" /> LinkedIn Profile
                      </a>
                    </div>

                    {isManagement && (
                      <Button
                        onClick={() => handleToggleVerification(profile.id)}
                        className={`w-full text-[10px] py-1 h-7 rounded-lg font-bold border transition ${
                          profile.status === "Verified"
                            ? "bg-slate-50 border-slate-200 text-slate-500 hover:bg-slate-100"
                            : "bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100"
                        }`}
                      >
                        {profile.status === "Verified" ? "Revoke Verification" : "Verify Graduate"}
                      </Button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          3. MENTORSHIP GUIDANCE TAB
          ======================================================== */}
      {activeTab === "mentorship" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Mentorships Tracker List */}
          <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
            <div className="flex justify-between items-center border-b pb-3 border-slate-50">
              <div>
                <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest">Alumni Mentor Bookings</h3>
                <p className="text-[11px] text-slate-400 font-semibold mt-1">Active matched career advisory pathways and consultations.</p>
              </div>
              <Button onClick={() => setIsRequestMentorshipOpen(true)} className="text-xs py-1.5">
                <Plus className="h-3.5 w-3.5 mr-1" /> Request Mentorship
              </Button>
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl pt-1">
              <table className="w-full text-xs text-slate-700 font-semibold text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Assigned Mentor</th>
                    <th className="p-3">Scholar Student</th>
                    <th className="p-3">Category Focus</th>
                    <th className="p-3">Session Date</th>
                    <th className="p-3">Progress Status</th>
                    {isManagement && <th className="p-3 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {mentorships.map(booking => {
                    const mentor = profiles.find(p => p.id === booking.mentorAlumniDocsId);
                    return (
                      <tr key={booking.id}>
                        <td className="p-3 font-extrabold text-slate-850">
                          {mentor ? mentor.name : "Alumni Mentor"}
                          {mentor && <span className="text-[9px] text-slate-400 block font-normal font-sans">{mentor.profession} at {mentor.company}</span>}
                        </td>
                        <td className="p-3 font-bold text-slate-650">{booking.studentName}</td>
                        <td className="p-3">
                          <Badge variant="info" className="text-[9px] font-black uppercase">
                            {booking.category}
                          </Badge>
                        </td>
                        <td className="p-3 font-mono font-bold text-slate-500">{booking.sessionDate}</td>
                        <td className="p-3">
                          <Badge variant={booking.status === "Active" ? "warning" : "success"} className="text-[9px] font-black uppercase">
                            {booking.status}
                          </Badge>
                        </td>
                        {isManagement && (
                          <td className="p-3 text-right">
                            <Button
                              onClick={() => handleToggleMentorshipStatus(booking.id)}
                              className="text-[10px] py-0.5 px-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-655"
                            >
                              Toggle Progress
                            </Button>
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mentors Directory Panels */}
          <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Verified Mentors Available
            </h3>
            <div className="space-y-4 max-h-[350px] overflow-y-auto pr-1">
              {profiles.filter(p => p.status === "Verified").map(mentor => (
                <div key={mentor.id} className="p-3.5 bg-slate-50 border border-slate-100 rounded-2xl flex gap-3 items-center">
                  <div className="h-10 w-10 bg-amber-50 text-amber-600 rounded-xl flex items-center justify-center font-bold text-sm shrink-0">
                    {mentor.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-xs font-black text-slate-800 truncate">{mentor.name}</h4>
                    <p className="text-[10px] text-slate-500 font-bold truncate">{mentor.profession} at {mentor.company}</p>
                    <p className="text-[9px] text-blue-650 font-black uppercase tracking-wider mt-0.5">Verified Alumni</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          4. REUNIONS & EVENTS TAB
          ======================================================== */}
      {activeTab === "events" && (
        <div className="space-y-5">
          <div className="flex justify-between items-center bg-white p-4 rounded-2xl border border-slate-105">
            <div>
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Reunion Schedules & Campus Panels</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Register RSVPs for reunions and online webinars hosted by alumni networks.</p>
            </div>
            {isManagement && (
              <Button onClick={() => setIsCreateEventOpen(true)} className="text-xs py-1.5">
                <Plus className="h-3.5 w-3.5 mr-1" /> Create Reunion Event
              </Button>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {events.map(evt => (
              <div key={evt.id} className="bg-white border border-slate-150 p-5 rounded-3xl flex flex-col justify-between h-56 hover:shadow-lg transition">
                <div className="space-y-3">
                  <div className="flex justify-between items-start gap-4">
                    <h4 className="text-sm font-black text-slate-850 leading-tight">{evt.title}</h4>
                    <Badge variant="info" className="text-[9px] font-black uppercase shrink-0">
                      {evt.eventType}
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-450 leading-relaxed font-semibold line-clamp-3">
                    {evt.description}
                  </p>
                </div>

                <div className="pt-3 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-[10px] font-bold text-slate-500">
                  <div className="space-y-1">
                    <div className="flex items-center gap-1.5 text-slate-600">
                      <Calendar className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span>Date: <strong className="text-slate-800 font-bold">{evt.eventDate}</strong></span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-600">
                      <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">Location: <strong className="text-slate-805 font-bold">{evt.location}</strong></span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 self-end sm:self-auto">
                    <span className="font-mono text-blue-700 font-bold bg-blue-50 px-2 py-1 rounded-lg">
                      {evt.rsvps} RSVPs
                    </span>
                    <Button
                      onClick={() => handleRsvp(evt.id)}
                      disabled={myRsvps.includes(evt.id)}
                      className={`text-[10px] py-1.5 px-3 rounded-lg font-bold transition border ${
                        myRsvps.includes(evt.id)
                          ? "bg-slate-50 border-slate-200 text-slate-400 cursor-not-allowed"
                          : "bg-blue-600 hover:bg-blue-700 text-white"
                      }`}
                    >
                      {myRsvps.includes(evt.id) ? "Registered" : "RSVP Seat"}
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ========================================================
          5. ENDOWMENT CAMPAIGNS TAB
          ======================================================== */}
      {activeTab === "donations" && (
        <div className="space-y-6">
          <div className="flex justify-between items-center bg-white p-4 rounded-2xl border border-slate-100">
            <div>
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Endowments & Donation Campaigns</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Review fund raising drives and allocate financial contributions securely.</p>
            </div>
            <div className="flex gap-2">
              {isManagement && (
                <Button onClick={() => setIsCreateCampaignOpen(true)} className="text-xs py-1.5 bg-slate-900 hover:bg-slate-800 text-white">
                  <Plus className="h-3.5 w-3.5 mr-1" /> Create Campaign
                </Button>
              )}
              <Button onClick={() => setIsDonateOpen(true)} className="text-xs py-1.5 bg-blue-600 hover:bg-blue-700 text-white">
                <DollarSign className="h-3.5 w-3.5 mr-1" /> Donate to School
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Campaigns list display */}
            <div className="lg:col-span-2 space-y-5">
              {campaigns.map(campaign => {
                const raised = getRaisedAmountForCampaign(campaign.id);
                const percent = Math.min(Math.round((raised / campaign.target) * 100), 100);
                return (
                  <div key={campaign.id} className="bg-white border border-slate-150 p-5 rounded-3xl space-y-4 hover:shadow-sm transition">
                    <div className="flex justify-between items-start gap-4">
                      <div>
                        <h4 className="text-sm font-black text-slate-850">{campaign.title}</h4>
                        <p className="text-xs text-slate-450 mt-1.5 leading-relaxed font-semibold">{campaign.description}</p>
                      </div>
                      <Badge variant={percent >= 100 ? "success" : "info"} className="text-[9px] font-black uppercase shrink-0">
                        {percent >= 100 ? "Goal Met" : "Active"}
                      </Badge>
                    </div>

                    <div className="space-y-2">
                      <div className="flex justify-between text-xs font-bold font-mono">
                        <span className="text-slate-400">Raised: ${raised.toLocaleString()}</span>
                        <span className="text-slate-800">Target Goal: ${campaign.target.toLocaleString()}</span>
                      </div>
                      <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden">
                        <div
                          className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                      <div className="text-right text-[10px] font-bold text-slate-400">
                        Campaign completion: {percent}%
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Recent Contributions feed table */}
            <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
                Recent Contributions
              </h3>
              <div className="space-y-4 max-h-[380px] overflow-y-auto pr-1">
                {donations.map(don => {
                  const campaign = campaigns.find(c => c.id === don.campaignDocsId);
                  const alumniMatch = profiles.find(p => p.id === don.alumniDocsId);
                  const displayName = don.alumniName || (alumniMatch ? alumniMatch.name : "Anonymous Supporter");
                  return (
                    <div key={don.id} className="p-3 bg-slate-50 border border-slate-100 rounded-2xl space-y-2">
                      <div className="flex justify-between items-center text-[10px] font-black">
                        <span className="text-slate-800 truncate max-w-[130px]">{displayName}</span>
                        <span className="text-emerald-700 font-mono">+${don.amount.toLocaleString()}</span>
                      </div>
                      <div className="flex justify-between text-[9px] font-bold text-slate-400 font-mono">
                        <span className="truncate max-w-[120px]">{campaign ? campaign.title : "School Fund"}</span>
                        <span>{don.donationDate}</span>
                      </div>
                      <p className="text-[8px] text-slate-350 font-mono font-bold uppercase truncate">Ref: {don.paymentReference}</p>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          6. REFERRAL JOB BOARD TAB
          ======================================================== */}
      {activeTab === "jobs" && (
        <div className="space-y-5">
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div>
              <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">Referral Directory & Career Placements</h3>
              <p className="text-[11px] text-slate-400 font-semibold mt-1">Apply for placement referrals and internships shared by St. Jude alumni.</p>
            </div>
            <div className="flex items-center gap-3 w-full md:w-auto">
              <div className="relative flex-1 md:w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Filter by title or company..."
                  value={jobSearch}
                  onChange={(e) => setJobSearch(e.target.value)}
                  className="bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white w-full transition"
                />
              </div>
              <Button onClick={() => setIsPostJobOpen(true)} className="text-xs py-1.5 shrink-0 bg-blue-600 hover:bg-blue-700 text-white rounded-xl">
                <Plus className="h-4 w-4 mr-1 shrink-0" /> Share Opportunity
              </Button>
            </div>
          </div>

          {/* Jobs Board list display */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {filteredJobs.length === 0 ? (
              <div className="col-span-full bg-white p-12 text-center border border-slate-100 rounded-2xl">
                <p className="text-slate-400 text-sm font-semibold italic">No jobs postings shared yet matching filters.</p>
              </div>
            ) : (
              filteredJobs.map(job => {
                const author = profiles.find(p => p.id === job.alumniDocsId);
                const referrer = job.referrerName || (author ? author.name : "St. Jude Graduate");
                return (
                  <div key={job.id} className="bg-white border border-slate-150 p-5 rounded-3xl flex flex-col justify-between h-[230px] hover:shadow-md transition">
                    <div className="space-y-2.5">
                      <div className="flex justify-between items-start gap-4">
                        <div>
                          <h4 className="text-sm font-black text-slate-850 truncate max-w-[220px]">{job.title}</h4>
                          <p className="text-xs text-blue-700 font-extrabold">{job.company} • <span className="text-slate-450 font-bold">{job.location}</span></p>
                        </div>
                        <Badge variant="secondary" className="text-[9px] font-black uppercase shrink-0">
                          Expires: {job.expiryDate}
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-400 leading-relaxed font-semibold line-clamp-3">
                        {job.description}
                      </p>
                    </div>

                    <div className="pt-3 border-t border-slate-100 flex justify-between items-center text-[10px] font-bold text-slate-400">
                      <span>Shared by: <strong className="text-slate-700 font-bold">{referrer}</strong></span>
                      <Button
                        onClick={() => {
                          setSelectedJob(job);
                          setIsApplyJobOpen(true);
                        }}
                        className="text-[10px] py-1 h-7 px-3.5 bg-slate-900 hover:bg-slate-800 text-white rounded-lg"
                      >
                        Ask for Referral
                      </Button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL FORMS & DIALOGS
          ======================================================== */}
      {/* 1. Alumni Manual Profile Registration Modal */}
      <Dialog
        isOpen={isRegisterAlumniOpen}
        onClose={() => setIsRegisterAlumniOpen(false)}
        title="Register Graduate Alumni Profile"
      >
        <form onSubmit={handleRegisterAlumni} className="space-y-4 pt-1">
          <Input
            label="Alumnus Graduate Name *"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Sarah Jenkins"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Graduation Year *"
              type="number"
              value={newYear}
              onChange={(e) => setNewYear(e.target.value)}
              min="1990"
              max="2030"
              required
            />
            <Input
              label="Profession/Title *"
              value={newProfession}
              onChange={(e) => setNewProfession(e.target.value)}
              placeholder="Software Engineer"
              required
            />
          </div>
          <Input
            label="Current Company/Organization *"
            value={newCompany}
            onChange={(e) => setNewCompany(e.target.value)}
            placeholder="Google Inc."
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Current City"
              value={newCity}
              onChange={(e) => setNewCity(e.target.value)}
              placeholder="San Francisco"
            />
            <Input
              label="Current Country"
              value={newCountry}
              onChange={(e) => setNewCountry(e.target.value)}
              placeholder="USA"
            />
          </div>
          <Input
            label="LinkedIn URL"
            value={newLinkedin}
            onChange={(e) => setNewLinkedin(e.target.value)}
            placeholder="https://linkedin.com/in/username"
          />
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsRegisterAlumniOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Register & Verify Record
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 2. Request Mentorship Guidance Session Modal */}
      <Dialog
        isOpen={isRequestMentorshipOpen}
        onClose={() => setIsRequestMentorshipOpen(false)}
        title="Request Mentorship Consultation"
      >
        <form onSubmit={handleBookMentorship} className="space-y-4 pt-1">
          <Select
            label="Select Mentorship Guide *"
            options={profiles.filter(p => p.status === "Verified").map(p => ({
              label: `${p.name} (${p.profession} at ${p.company})`,
              value: p.id
            }))}
            value={selectedMentorId}
            onChange={(e) => setSelectedMentorId(e.target.value)}
          />

          <Select
            label="Select Scholar Student *"
            options={students.map(s => ({
              label: `${s.name} (Grade ${s.grade} | Adm: ${s.admissionNo})`,
              value: s.id
            }))}
            value={selectedStudentDocsId}
            onChange={(e) => setSelectedStudentDocsId(e.target.value)}
          />

          <Select
            label="Mentorship Category Focus"
            options={[
              { label: "Career Guidance & Development", value: "Career Guidance" },
              { label: "College Admissions & Prep", value: "College Admissions" },
              { label: "Research Pathways", value: "Research Pathways" },
              { label: "Entrepreneurship Ventures", value: "Entrepreneurship" }
            ]}
            value={mentorshipCategory}
            onChange={(e) => setMentorshipCategory(e.target.value)}
          />

          <Input
            label="Target Consultation Date *"
            type="date"
            value={mentorshipDate}
            onChange={(e) => setMentorshipDate(e.target.value)}
            required
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsRequestMentorshipOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Request Match Session
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 3. Create Reunion Event Modal */}
      <Dialog
        isOpen={isCreateEventOpen}
        onClose={() => setIsCreateEventOpen(false)}
        title="Schedule Alumni Reunion Event"
      >
        <form onSubmit={handleCreateEvent} className="space-y-4 pt-1">
          <Input
            label="Event Title *"
            value={eventTitle}
            onChange={(e) => setEventTitle(e.target.value)}
            placeholder="Decadal Grand Reunion 2026"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Event Type"
              options={[
                { label: "Annual Reunion Gala", value: "Annual Reunion" },
                { label: "Career Talk Webinar", value: "Career Talk" },
                { label: "Fundraising Banquet", value: "Fundraiser" },
                { label: "Campus Panel Session", value: "Panel Discussion" }
              ]}
              value={eventType}
              onChange={(e) => setEventType(e.target.value)}
            />
            <Input
              label="Scheduled Date *"
              type="date"
              value={eventDate}
              onChange={(e) => setEventDate(e.target.value)}
              required
            />
          </div>
          <Input
            label="Venue Location / Link *"
            value={eventLocation}
            onChange={(e) => setEventLocation(e.target.value)}
            placeholder="St. Jude Assembly Hall or Zoom link"
            required
          />
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Event Details & Description</label>
            <textarea
              value={eventDesc}
              onChange={(e) => setEventDesc(e.target.value)}
              placeholder="Join us for batch dinners, presentations, and workshops."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-24"
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsCreateEventOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Publish Event Notice
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 4. Pledge Donation / Contribution Modal */}
      <Dialog
        isOpen={isDonateOpen}
        onClose={() => setIsDonateOpen(false)}
        title="Submit Contribution to Endowment"
      >
        <form onSubmit={handleSubmitDonation} className="space-y-4 pt-1">
          <Input
            label="Donor Full Name"
            value={donorName}
            onChange={(e) => setDonorName(e.target.value)}
            placeholder="John Doe"
          />

          <Select
            label="Allocate Contribution drive *"
            options={campaigns.map(c => ({
              label: c.title,
              value: c.id
            }))}
            value={selectedCampaignDocsId}
            onChange={(e) => setSelectedCampaignDocsId(e.target.value)}
          />

          <Input
            label="Donation Amount ($ USD) *"
            type="number"
            value={donationAmount}
            onChange={(e) => setDonationAmount(e.target.value)}
            placeholder="1500"
            min="1"
            required
          />

          <Input
            label="Payment Reference / Check ID"
            value={paymentRef}
            onChange={(e) => setPaymentRef(e.target.value)}
            placeholder="REF-908234-X (Optional)"
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsDonateOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Post Contribution Ledger
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 5. Create Donation Campaign Modal */}
      <Dialog
        isOpen={isCreateCampaignOpen}
        onClose={() => setIsCreateCampaignOpen(false)}
        title="Initiate Donation Campaign"
      >
        <form onSubmit={handleCreateCampaign} className="space-y-4 pt-1">
          <Input
            label="Campaign Title *"
            value={campaignTitle}
            onChange={(e) => setCampaignTitle(e.target.value)}
            placeholder="Merit Scholarship Fund"
            required
          />
          <Input
            label="Financial Target Goal ($ USD) *"
            type="number"
            value={campaignTarget}
            onChange={(e) => setCampaignTarget(e.target.value)}
            placeholder="15000"
            min="100"
            required
          />
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Campaign Mission Statement</label>
            <textarea
              value={campaignDesc}
              onChange={(e) => setCampaignDesc(e.target.value)}
              placeholder="Supporting deserving students from underrepresented backgrounds with full/partial tuitions."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-24"
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsCreateCampaignOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Launch Campaign Drive
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 6. Post Job Referral Modal */}
      <Dialog
        isOpen={isPostJobOpen}
        onClose={() => setIsPostJobOpen(false)}
        title="Share Job Referral Listing"
      >
        <form onSubmit={handlePostJob} className="space-y-4 pt-1">
          <Input
            label="Job / Internship Title *"
            value={jobTitle}
            onChange={(e) => setJobTitle(e.target.value)}
            placeholder="Software Engineering Intern"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Company / Employer *"
              value={jobCompany}
              onChange={(e) => setJobCompany(e.target.value)}
              placeholder="Google"
              required
            />
            <Input
              label="Location/Remote *"
              value={jobLocation}
              onChange={(e) => setJobLocation(e.target.value)}
              placeholder="San Francisco, CA or Remote"
              required
            />
          </div>

          <Input
            label="Expiry Deadline Date *"
            type="date"
            value={jobExpiry}
            onChange={(e) => setJobExpiry(e.target.value)}
            required
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Job Description & Referral steps</label>
            <textarea
              value={jobDesc}
              onChange={(e) => setJobDesc(e.target.value)}
              placeholder="Provide a summary of the position requirements and referral application details."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-24"
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsPostJobOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Post Referral Opening
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 7. Apply for Referral / Resume Submission Modal */}
      <Dialog
        isOpen={isApplyJobOpen}
        onClose={() => setIsApplyJobOpen(false)}
        title={`Apply for Referral: ${selectedJob?.title} at ${selectedJob?.company}`}
      >
        <form onSubmit={handleApplyJob} className="space-y-4 pt-1">
          <Input
            label="Applicant Full Name *"
            value={applicantName}
            onChange={(e) => setApplicantName(e.target.value)}
            required
          />
          <Input
            label="Contact Email Address *"
            type="email"
            value={applicantEmail}
            onChange={(e) => setApplicantEmail(e.target.value)}
            placeholder="student@gmail.com"
            required
          />
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Cover Note / Referral pitch</label>
            <textarea
              value={applicantCoverNote}
              onChange={(e) => setApplicantCoverNote(e.target.value)}
              placeholder="Introduce yourself and explain why you're a great fit for this referral opportunity."
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition h-20"
            />
          </div>

          {/* Simple Mock Resume Upload */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Upload Resume (PDF/DOCX) *</label>
            {resumeName ? (
              <div className="flex justify-between items-center bg-slate-50 border border-slate-200 px-4 py-2.5 rounded-xl text-xs font-bold">
                <span className="flex items-center gap-2 text-slate-800">
                  <FileText className="h-4 w-4 text-blue-500" /> {resumeName}
                </span>
                <button
                  type="button"
                  onClick={() => setResumeName("")}
                  className="text-rose-500 hover:text-rose-700"
                >
                  Remove
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setResumeName("Resume_StJude_Scholar.pdf")}
                className="bg-slate-50 border border-dashed border-slate-300 hover:bg-slate-100 rounded-xl p-6 flex flex-col items-center justify-center gap-1.5 transition text-slate-500 select-none cursor-pointer"
              >
                <UploadCloud className="h-6 w-6 text-slate-400" />
                <span className="text-xs font-bold">Mock upload resume file</span>
              </button>
            )}
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsApplyJobOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Submit Application to Referrer
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
