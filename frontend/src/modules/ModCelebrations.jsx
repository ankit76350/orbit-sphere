/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import React, { useState, useEffect } from "react";
import {
  getStudents,
  getStaff,
  getBirthdays,
  saveBirthdays,
  getBirthdayNotifications,
  saveBirthdayNotifications,
  getBirthdayCards,
  saveBirthdayCards,
  getBirthdayGallery,
  saveBirthdayGallery,
  getGalleryMedia,
  saveGalleryMedia,
  logAction
} from "../storage";
import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Dialog, Input, Select, useToast } from "../components/ui";
import {
  Cake,
  Gift,
  Bell,
  Mail,
  MessageSquare,
  Share2,
  Calendar,
  Award,
  Download,
  Search,
  Filter,
  Plus,
  Trash2,
  Edit,
  Camera,
  CheckCircle,
  FileText,
  User,
  Coffee,
  Utensils,
  ChevronRight,
  Send,
  Zap,
  Info,
  Database,
  Smartphone,
  Check,
  Code
} from "lucide-react";

export default function ModCelebrations({ user }) {
  const { addToast } = useToast();

  // Load state from local storage
  const [students, setStudents] = useState(() => getStudents());
  const [staff, setStaff] = useState(() => getStaff());
  const [customEvents, setCustomEvents] = useState(() => getBirthdays());
  const [notifications, setNotifications] = useState(() => getBirthdayNotifications());
  const [birthdayCards, setBirthdayCards] = useState(() => getBirthdayCards());
  const [birthdayGallery, setBirthdayGallery] = useState(() => getBirthdayGallery());

  // Navigation Tabs: dashboard, greeting_cards, scheduler, warden_desk, teacher_desk, parent_portal, custom_events, api_docs
  const [activeTab, setActiveTab] = useState("dashboard");

  // Filters
  const [searchQuery, setSearchQuery] = useState("");
  const [rosterType, setRosterType] = useState("all"); // all, student, staff

  // Card Creator Customizer State
  const [selectedPerson, setSelectedPerson] = useState(null);
  const [cardTheme, setCardTheme] = useState("classic"); // classic, modern, festive, minimalist
  const [cardMessage, setCardMessage] = useState("Wishing you a year filled with growth, joy, and success! Happy Birthday!");
  const [principalSig, setPrincipalSig] = useState("Dr. Arthur Pendelton");
  const [isGeneratingCard, setIsGeneratingCard] = useState(false);

  // Dialog toggles
  const [isAddEventOpen, setIsAddEventOpen] = useState(false);
  const [isUploadPhotoOpen, setIsUploadPhotoOpen] = useState(false);
  const [isMealRequestOpen, setIsMealRequestOpen] = useState(false);

  // Form states
  // 1. Add Custom Event
  const [newEventTitle, setNewEventTitle] = useState("");
  const [newEventType, setNewEventType] = useState("School Foundation Day");
  const [newEventDate, setNewEventDate] = useState("");
  const [newEventDesc, setNewEventDesc] = useState("");
  const [newEventTarget, setNewEventTarget] = useState("All");

  // 2. Upload Celebration Photo
  const [photoStudentDocsId, setPhotoStudentDocsId] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [photoCaption, setPhotoCaption] = useState("");

  // 3. Warden Meal / Cake Request
  const [mealStudentDocsId, setMealStudentDocsId] = useState("");
  const [mealCakeFlavor, setMealCakeFlavor] = useState("Chocolate Fudge");
  const [mealSpecialDiet, setMealSpecialDiet] = useState("None");
  const [mealDate, setMealDate] = useState("");
  const [mealRequestsList, setMealRequestsList] = useState([
    { id: "meal-1", studentName: "Aarav Sharma", grade: "Grade 6", flavor: "Red Velvet", diet: "Nut Allergy Alert", date: new Date().toISOString().split("T")[0], status: "Cake Ordered" },
    { id: "meal-2", studentName: "Priya Patel", grade: "Grade 8", flavor: "Strawberry Cream", diet: "Lactose Intolerant", date: new Date().toISOString().split("T")[0], status: "Pending Warden" }
  ]);

  // Scheduler Cron Simulator State
  const [isSchedulerRunning, setIsSchedulerRunning] = useState(false);
  const [schedulerLogs, setSchedulerLogs] = useState([]);
  const [schedulerProgress, setSchedulerProgress] = useState(0);

  // User Role Permissions
  const isSuperAdmin = user?.role === "Super Admin" || user?.role === "School Admin";
  const isPrincipal = user?.role === "Principal" || isSuperAdmin;
  const isWarden = user?.role === "Warden" || isSuperAdmin;
  const isTeacher = user?.role === "Teacher" || isSuperAdmin;
  const isParent = user?.role === "Parent";
  const isStudent = user?.role === "Student";

  // Birthday Evaluator Helper
  const getBirthdayStatus = (dobString) => {
    if (!dobString) return null;
    const today = new Date();
    // Parse DOB string
    const parts = dobString.split("-");
    if (parts.length !== 3) return null;
    const bMonth = parseInt(parts[1], 10) - 1;
    const bDay = parseInt(parts[2], 10);

    const currentYear = today.getFullYear();
    const nextBDay = new Date(currentYear, bMonth, bDay);

    // Set hours to zero for clean date comparisons
    today.setHours(0, 0, 0, 0);
    nextBDay.setHours(0, 0, 0, 0);

    // If birthday already passed this year, look at next year's birthday
    if (nextBDay < today && (today.getDate() !== bDay || today.getMonth() !== bMonth)) {
      nextBDay.setFullYear(currentYear + 1);
    }

    const diffTime = nextBDay - today;
    const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

    const isToday = today.getDate() === bDay && today.getMonth() === bMonth;
    const isThisWeek = isToday || (diffDays > 0 && diffDays <= 7);
    const isThisMonth = bMonth === today.getMonth();

    return {
      isToday,
      daysUntil: isToday ? 0 : diffDays,
      isThisWeek,
      isThisMonth
    };
  };

  // Build Students Birthday Roster
  const studentRoster = students.map(st => ({
    ...st,
    type: "Student",
    birthday: getBirthdayStatus(st.dob)
  })).filter(st => st.birthday !== null);

  // Build Staff Birthday Roster
  const staffRoster = staff.map(sf => ({
    ...sf,
    type: "Staff",
    birthday: getBirthdayStatus(sf.dob)
  })).filter(sf => sf.birthday !== null);

  // Combine both rosters
  const combinedRoster = [...studentRoster, ...staffRoster];

  // Filtering lists
  const todayBirthdays = combinedRoster.filter(p => p.birthday.isToday);
  const weekBirthdays = combinedRoster.filter(p => p.birthday.isThisWeek && !p.birthday.isToday).sort((a, b) => a.birthday.daysUntil - b.birthday.daysUntil);
  const monthBirthdays = combinedRoster.filter(p => p.birthday.isThisMonth && !p.birthday.isToday && !p.birthday.isThisWeek);

  const filteredRoster = combinedRoster.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (p.type === "Student" && p.admissionNo.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (p.type === "Staff" && p.role.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesType = rosterType === "all" || p.type.toLowerCase() === rosterType;
    return matchesSearch && matchesType;
  }).sort((a, b) => a.birthday.daysUntil - b.birthday.daysUntil);

  // Find parent's child if user is a Parent
  const myChild = isParent ? students.find(s => s.parentEmail.toLowerCase() === user.email.toLowerCase()) || students[0] : null;
  const childBirthday = myChild ? getBirthdayStatus(myChild.dob) : null;

  // Find logged in student profile
  const studentProfile = isStudent ? students.find(s => s.name.toLowerCase() === user.name.toLowerCase()) || students[0] : null;
  const loggedStudentBirthday = studentProfile ? getBirthdayStatus(studentProfile.dob) : null;

  // HANDLERS

  // 1. Add Custom Celebration Event
  const handleAddCustomEvent = (e) => {
    e.preventDefault();
    if (!newEventTitle.trim() || !newEventDate) {
      addToast("Error", "Title and Date are required.", "error");
      return;
    }

    const newEvent = {
      id: `event-${Date.now()}`,
      schoolId: "school-01",
      title: newEventTitle.trim(),
      eventType: newEventType,
      date: newEventDate,
      description: newEventDesc.trim(),
      targetAudience: newEventTarget
    };

    const updatedEvents = [...customEvents, newEvent];
    setCustomEvents(updatedEvents);
    saveBirthdays(updatedEvents);

    logAction(
      user.id,
      user.name,
      user.role,
      "Celebration Event Added",
      `Created custom event "${newEventTitle}" for ${newEventTarget}`
    );

    addToast("Event Registered", `"${newEventTitle}" has been scheduled successfully.`);
    setIsAddEventOpen(false);
    setNewEventTitle("");
    setNewEventDate("");
    setNewEventDesc("");
  };

  // 2. Delete Custom Event
  const handleDeleteEvent = (id, title) => {
    const updated = customEvents.filter(ev => ev.id !== id);
    setCustomEvents(updated);
    saveBirthdays(updated);

    logAction(
      user.id,
      user.name,
      user.role,
      "Celebration Event Deleted",
      `Removed event "${title}"`
    );

    addToast("Event Deleted", `"${title}" has been removed.`);
  };

  // 3. Custom Card Generator
  const handleGenerateCard = () => {
    if (!selectedPerson) {
      addToast("Select Recipient", "Please select a student or staff member first.", "warning");
      return;
    }

    setIsGeneratingCard(true);

    setTimeout(() => {
      const newCard = {
        id: `card-${Date.now()}`,
        schoolId: "school-01",
        personType: selectedPerson.type.toLowerCase(),
        personDocsId: selectedPerson.id,
        personName: selectedPerson.name,
        cardUrl: cardTheme === "festive"
          ? "https://images.unsplash.com/photo-1513201099705-a9746e1e201f?w=800"
          : cardTheme === "academic"
            ? "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800"
            : "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?w=800",
        message: cardMessage,
        theme: cardTheme,
        createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
      };

      const updatedCards = [newCard, ...birthdayCards];
      setBirthdayCards(updatedCards);
      saveBirthdayCards(updatedCards);

      logAction(
        user.id,
        user.name,
        user.role,
        "Birthday Card Generated",
        `Created digital card for ${selectedPerson.name} (${cardTheme} theme)`
      );

      setIsGeneratingCard(false);
      addToast("Card Generated", `Greeting card created successfully for ${selectedPerson.name}!`);
    }, 1200);
  };

  // 4. Trigger Scheduler wishes (Midnight simulation)
  const handleRunScheduler = () => {
    if (todayBirthdays.length === 0) {
      addToast("No Birthdays Today", "There are no birthdays today to process wishes for.", "info");
      return;
    }

    setIsSchedulerRunning(true);
    setSchedulerProgress(5);
    setSchedulerLogs(["[Cron Service] Initializing midnight birthday cron job at 00:00:00..."]);

    const logList = [];
    const pushLog = (msg) => {
      logList.push(msg);
      setSchedulerLogs([...logList]);
    };

    setTimeout(() => {
      setSchedulerProgress(20);
      pushLog(`[Database] Found ${todayBirthdays.length} birthday records active for today.`);
    }, 600);

    todayBirthdays.forEach((person, index) => {
      const stepTime = 1200 + index * 1000;
      setTimeout(() => {
        setSchedulerProgress(Math.min(95, 20 + Math.floor((index + 1) / todayBirthdays.length * 70)));
        pushLog(`[Mailing Engine] Generating wish payloads for: ${person.name} (${person.type})`);

        // Send wishes across different channels
        const channels = ["Email", "SMS", "WhatsApp", "Push"];
        const notificationsDispatched = [];

        channels.forEach(ch => {
          let rec = "";
          let msg = "";

          if (person.type === "Student") {
            if (ch === "Email") {
              rec = person.parentEmail;
              msg = `Dear ${person.parentName}, St. Jude Academy wishes your child ${person.name} a wonderful birthday!`;
            } else if (ch === "WhatsApp") {
              rec = person.parentPhone;
              msg = `🎂 St. Jude wishes ${person.name} a Happy Birthday! Have a wonderful day of celebration!`;
            } else if (ch === "SMS") {
              rec = person.parentPhone;
              msg = `Happy Birthday wishes for ${person.name} from St. Jude Boarding School. Check parent portal for greeting cards.`;
            } else {
              rec = person.email || `${person.name.toLowerCase().replace(" ", "")}@stjude.edu`;
              msg = `Happy Birthday ${person.name}! Please collect your birthday card and certificate from the Principal's Cabinet at High Tea.`;
            }
          } else {
            // Staff
            if (ch === "Email") {
              rec = person.email;
              msg = `Dear Prof. ${person.name}, Happy Birthday! Thank you for your continued dedication to academic excellence.`;
            } else if (ch === "Push") {
              rec = person.email;
              msg = `🎂 Happy Birthday! Principal Smith and the HR department wish you a magnificent day!`;
            } else {
              rec = person.phone;
              msg = `Happy Birthday ${person.name}! St. Jude Boarding ERP wishes you a spectacular work anniversary and birthday.`;
            }
          }

          const newNotif = {
            id: `notif-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
            schoolId: "school-01",
            personType: person.type.toLowerCase(),
            personDocsId: person.id,
            personName: person.name,
            notificationType: ch,
            recipient: rec,
            message: msg,
            sentAt: new Date().toISOString().replace("T", " ").substring(0, 19)
          };

          notificationsDispatched.push(newNotif);
        });

        // Save simulated notifications
        const freshNotifs = [...notificationsDispatched, ...notifications];
        setNotifications(freshNotifs);
        saveBirthdayNotifications(freshNotifs);

        pushLog(`[Channel Manager] Dispatched Push/Email/SMS/WhatsApp wishes to ${person.name} and guardians.`);
      }, stepTime);
    });

    const completionTime = 1200 + todayBirthdays.length * 1000 + 800;
    setTimeout(() => {
      setSchedulerProgress(100);
      pushLog(`[Cron Service] Midnight wishes process completed successfully. Sent ${todayBirthdays.length * 4} alerts.`);
      setIsSchedulerRunning(false);

      logAction(
        user.id,
        user.name,
        user.role,
        "Midnight Wishes Dispatched",
        `Simulated birthday wishes cron execution for ${todayBirthdays.length} celebrants.`
      );

      addToast("Scheduler Processed", `Midnight wishes successfully sent to all ${todayBirthdays.length} birthday celebrants.`);
    }, completionTime);
  };

  // 5. Submit Special Meal Request (Warden)
  const handleAddMealRequest = (e) => {
    e.preventDefault();
    if (!mealStudentDocsId || !mealDate) {
      addToast("Required Fields", "Please select a student and target celebration date.", "warning");
      return;
    }

    const matchedStudent = students.find(s => s.id === mealStudentDocsId);
    if (!matchedStudent) return;

    const newRequest = {
      id: `meal-${Date.now()}`,
      studentName: matchedStudent.name,
      grade: matchedStudent.grade,
      flavor: mealCakeFlavor,
      diet: mealSpecialDiet,
      date: mealDate,
      status: "Pending Warden"
    };

    setMealRequestsList([newRequest, ...mealRequestsList]);
    setIsMealRequestOpen(false);

    logAction(
      user.id,
      user.name,
      user.role,
      "Hostel Meal Request Registered",
      `Requested birthday cake (${mealCakeFlavor}) and special dinner for student: ${matchedStudent.name}`
    );

    addToast("Meal Request Logged", `Birthday dinner and cake request added for ${matchedStudent.name}.`);
  };

  // 6. Approve Warden Cake Order
  const handleCakeStatusUpdate = (id, newStatus) => {
    setMealRequestsList(prev => prev.map(m => m.id === id ? { ...m, status: newStatus } : m));
    addToast("Status Updated", `Birthday order changed to: "${newStatus}"`);
  };

  // 7. Upload Birthday Celebration Photo
  const handleUploadBirthdayPhoto = (e) => {
    e.preventDefault();
    if (!photoStudentDocsId || !photoUrl) {
      addToast("Incomplete details", "Please select a student and input a media URL.", "error");
      return;
    }

    const st = students.find(s => s.id === photoStudentDocsId);
    if (!st) return;

    // Create record in birthday gallery
    const newPic = {
      id: `bg-${Date.now()}`,
      schoolId: "school-01",
      personType: "student",
      personDocsId: st.id,
      personName: st.name,
      mediaUrl: photoUrl.trim(),
      caption: photoCaption.trim() || `Birthday cake cutting photo of ${st.name}`,
      createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
    };

    const updatedGallery = [newPic, ...birthdayGallery];
    setBirthdayGallery(updatedGallery);
    saveBirthdayGallery(updatedGallery);

    // Also push to main Media Gallery store
    const mainGalleryMedia = getGalleryMedia();
    const newMainMedia = {
      id: `med-birth-${Date.now()}`,
      albumDocsId: "alb-birthdays", // Seed album reference
      mediaType: "Photo",
      mediaUrl: photoUrl.trim()
    };
    saveGalleryMedia([...mainGalleryMedia, newMainMedia]);

    logAction(
      user.id,
      user.name,
      user.role,
      "Birthday Photo Uploaded",
      `Linked cake cutting photo for student: ${st.name}`
    );

    setIsUploadPhotoOpen(false);
    setPhotoUrl("");
    setPhotoCaption("");
    addToast("Photo Published", `Cake cutting photo added and linked to ${st.name}'s profile.`);
  };

  return (
    <div className="space-y-6">
      {/* 1. Header Banner */}
      <div className="relative overflow-hidden bg-gradient-to-r from-blue-700 via-indigo-700 to-purple-800 text-white rounded-3xl p-6 shadow-md border border-indigo-900">
        <div className="absolute right-0 top-0 -mt-8 -mr-8 h-40 w-40 bg-white/10 rounded-full blur-2xl pointer-events-none" />
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 z-10 relative">
          <div className="space-y-2">
            <span className="bg-white/20 text-white border border-white/10 font-bold text-[10px] uppercase tracking-widest px-3 py-1 rounded-full">
              MOD-22 Engagement Desk
            </span>
            <h2 className="text-2xl font-black tracking-tight flex items-center gap-2">
              <Cake className="h-7 w-7 text-yellow-300 animate-bounce" /> Birthday & Celebration Hub
            </h2>
            <p className="text-xs text-indigo-100 font-medium max-w-xl">
              Track student and staff birthdays, send automatic wishes across multi-channel SaaS streams, customized digital greeting certificates, and schedule hostel feasts.
            </p>
          </div>

          <div className="flex flex-wrap gap-2 shrink-0">
            {isSuperAdmin && (
              <Button
                onClick={handleRunScheduler}
                disabled={isSchedulerRunning}
                className="bg-yellow-400 hover:bg-yellow-500 text-slate-900 font-extrabold flex items-center gap-1.5 size-sm text-xs rounded-xl"
              >
                <Zap className="h-4 w-4 fill-slate-900" /> Trigger Midnight Wishes
              </Button>
            )}
            <Badge variant="info" className="bg-white/10 border-white/20 text-white font-mono uppercase font-black tracking-wide text-xs">
              Role: {user.role}
            </Badge>
          </div>
        </div>
      </div>

      {/* 2. Top-level KPIs Dashboard Roster */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Today's Birthdays</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-slate-800 leading-none">{todayBirthdays.length}</p>
              <div className="p-1.5 bg-rose-50 text-rose-600 rounded-lg"><Cake className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">This Week Birthdays</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-slate-800 leading-none">{weekBirthdays.length}</p>
              <div className="p-1.5 bg-blue-50 text-blue-600 rounded-lg"><Calendar className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Wishes Sent (Today)</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-emerald-600 leading-none">
                {notifications.filter(n => n.sentAt.split(" ")[0] === new Date().toISOString().split("T")[0]).length}
              </p>
              <div className="p-1.5 bg-emerald-50 text-emerald-600 rounded-lg"><Mail className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Cards Generated</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-indigo-600 leading-none">{birthdayCards.length}</p>
              <div className="p-1.5 bg-indigo-50 text-indigo-600 rounded-lg"><Award className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Hostel Feast Requests</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-amber-600 leading-none">{mealRequestsList.length}</p>
              <div className="p-1.5 bg-amber-50 text-amber-600 rounded-lg"><Utensils className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-100 shadow-2xs hover:shadow-sm">
          <CardContent className="p-4 flex flex-col justify-between h-24">
            <span className="text-[9px] uppercase font-bold text-slate-400 tracking-wider">Custom Celebrations</span>
            <div className="flex justify-between items-end">
              <p className="text-2xl font-black text-purple-600 leading-none">{customEvents.length}</p>
              <div className="p-1.5 bg-purple-50 text-purple-600 rounded-lg"><Gift className="h-4.5 w-4.5" /></div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 3. Horizontal Module Navigation Tabs */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("dashboard")}
          className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "dashboard" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
            }`}
        >
          Overview & Calendar
        </button>

        <button
          onClick={() => setActiveTab("greeting_cards")}
          className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "greeting_cards" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
            }`}
        >
          Certificate & Card Canvas
        </button>

        <button
          onClick={() => setActiveTab("scheduler")}
          className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "scheduler" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
            }`}
        >
          Midnight Scheduler Logs
        </button>

        {isWarden && (
          <button
            onClick={() => setActiveTab("warden_desk")}
            className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "warden_desk" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
              }`}
          >
            Hostel Warden Desk
          </button>
        )}

        {isTeacher && (
          <button
            onClick={() => setActiveTab("teacher_desk")}
            className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "teacher_desk" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
              }`}
          >
            Classroom Roster
          </button>
        )}

        {isParent && (
          <button
            onClick={() => setActiveTab("parent_portal")}
            className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "parent_portal" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
              }`}
          >
            Parent App
          </button>
        )}

        <button
          onClick={() => setActiveTab("gallery")}
          className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "gallery" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
            }`}
        >
          Celebration Gallery
        </button>

        {isPrincipal && (
          <button
            onClick={() => setActiveTab("custom_events")}
            className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "custom_events" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
              }`}
          >
            Custom Events Editor
          </button>
        )}

        <button
          onClick={() => setActiveTab("api_docs")}
          className={`px-4.5 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${activeTab === "api_docs" ? "bg-white text-blue-700 shadow-2xs" : "text-slate-500 hover:text-slate-800"
            }`}
        >
          Developer Console (API)
        </button>
      </div>

      {/* =========================================================
          TAB CONTENT: DASHBOARD (OVERVIEW & CALENDAR)
          ========================================================= */}
      {activeTab === "dashboard" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left / Middle: Celebrant Lists */}
          <div className="lg:col-span-2 space-y-6">

            {/* Live Today Wishes Display */}
            {todayBirthdays.length > 0 ? (
              <div className="bg-rose-50/50 border border-rose-150 p-6 rounded-3xl space-y-4">
                <div className="flex justify-between items-center leading-none">
                  <h3 className="text-sm font-black text-rose-900 uppercase tracking-widest flex items-center gap-2">
                    <Cake className="h-5 w-5 text-rose-600 animate-spin" /> Happening Today: school-wide wishes!
                  </h3>
                  <Badge variant="danger" className="bg-rose-600 text-white font-extrabold uppercase animate-pulse">Celebration active</Badge>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {todayBirthdays.map((person) => (
                    <div key={person.id} className="bg-white border border-rose-100 p-4.5 rounded-2xl flex items-center justify-between shadow-2xs hover:shadow-xs transition duration-200">
                      <div className="flex items-center gap-3">
                        <div className="h-12 w-12 rounded-full bg-gradient-to-tr from-rose-450 to-pink-500 text-white flex items-center justify-center font-black text-sm shadow-md uppercase">
                          {person.name.split(" ").map(n => n[0]).slice(0, 2).join("")}
                        </div>
                        <div>
                          <p className="text-xs font-black text-slate-800 flex items-center gap-1">
                            {person.name}
                            <Badge variant="secondary" className="text-[8px] px-1 py-0 px-2 rounded">{person.type}</Badge>
                          </p>
                          <p className="text-[10px] text-slate-450 font-bold mt-1">
                            {person.type === "Student"
                              ? `${person.grade} • Bed ${person.hostelRoomNo || "Day Boarder"}`
                              : `${person.department} • ${person.role}`}
                          </p>
                        </div>
                      </div>

                      <Button
                        size="sm"
                        onClick={() => {
                          setSelectedPerson(person);
                          setCardMessage(`Dear ${person.name}, St. Jude Boarding School wishes you a fabulous birthday filled with high marks and pleasant memories. Happy Birthday!`);
                          setActiveTab("greeting_cards");
                        }}
                        className="bg-rose-550 text-white hover:bg-rose-600 font-bold uppercase py-1 px-3 text-[10px] rounded-lg cursor-pointer"
                      >
                        Create Card
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="bg-slate-50 border border-slate-200 p-6 rounded-3xl text-center text-slate-400 italic text-xs">
                🎈 No birthdays active on campus today. Check out upcoming celebrations in the lists below!
              </div>
            )}

            {/* Roster Database Board */}
            <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3 border-b pb-4 border-slate-100">
                <div>
                  <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Celebrations Registry</h3>
                  <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Filtered directory of student & staff profiles</p>
                </div>

                <div className="flex gap-2 w-full md:w-auto">
                  <div className="relative flex-1 md:w-44">
                    <Search className="h-3.5 w-3.5 text-slate-400 absolute left-2.5 top-2.5" />
                    <input
                      type="text"
                      placeholder="Search celebrant..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="bg-slate-50 border border-slate-200 text-xs font-bold rounded-xl pl-8 pr-3 py-1.5 focus:outline-none focus:bg-white w-full transition"
                    />
                  </div>

                  <Select
                    options={[
                      { label: "All Roster", value: "all" },
                      { label: "Students", value: "student" },
                      { label: "Staff", value: "staff" }
                    ]}
                    value={rosterType}
                    onChange={(e) => setRosterType(e.target.value)}
                    className="text-xs bg-slate-50 border border-slate-200 rounded-xl h-8 py-1"
                  />
                </div>
              </div>

              {/* Roster table */}
              <div className="overflow-x-auto border border-slate-100 rounded-2xl">
                <table className="w-full text-xs font-semibold text-slate-700 text-left">
                  <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-3">Name</th>
                      <th className="p-3">Type</th>
                      <th className="p-3">Birthdate</th>
                      <th className="p-3">Dorm / Dept</th>
                      <th className="p-3">Days Until</th>
                      <th className="p-3 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50">
                    {filteredRoster.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="p-8 text-center text-slate-400 italic">No records match filters.</td>
                      </tr>
                    ) : (
                      filteredRoster.map((person) => {
                        const statusColor = person.birthday.isToday
                          ? "bg-rose-50 text-rose-600 border-rose-200"
                          : person.birthday.isThisWeek
                            ? "bg-blue-50 text-blue-700 border-blue-200"
                            : "bg-slate-100 text-slate-650 border-slate-200";

                        return (
                          <tr key={person.id} className="hover:bg-slate-50/50 transition">
                            <td className="p-3 font-extrabold text-slate-800">{person.name}</td>
                            <td className="p-3">
                              <Badge variant={person.type === "Student" ? "secondary" : "default"}>{person.type}</Badge>
                            </td>
                            <td className="p-3 font-mono text-slate-450">{person.dob}</td>
                            <td className="p-3 text-slate-500 font-bold">
                              {person.type === "Student"
                                ? `${person.grade} (Bed ${person.hostelRoomNo || "N/A"})`
                                : person.department}
                            </td>
                            <td className="p-3 font-extrabold text-slate-800">
                              {person.birthday.isToday ? (
                                <span className="text-rose-600 font-black animate-pulse">🎂 TODAY</span>
                              ) : (
                                `${person.birthday.daysUntil} Days`
                              )}
                            </td>
                            <td className="p-3 text-center">
                              <span className={`inline-block px-2 py-0.5 text-[9px] font-black border rounded-md uppercase ${statusColor}`}>
                                {person.birthday.isToday ? "Today" : person.birthday.isThisWeek ? "Weekly" : "Monthly"}
                              </span>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>

          </div>

          {/* Right Column: Custom Celebration Events & Recently Celebrated */}
          <div className="space-y-6">

            {/* Upcoming School Celebrations */}
            <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
              <div className="flex justify-between items-center border-b pb-3 border-slate-50">
                <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest flex items-center gap-1.5">
                  <Gift className="h-4.5 w-4.5 text-indigo-650" /> Celebration Calendar
                </h3>
                {isPrincipal && (
                  <button
                    onClick={() => setIsAddEventOpen(true)}
                    className="p-1 text-indigo-655 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition shrink-0 cursor-pointer"
                    title="Add Custom Event"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                )}
              </div>

              <div className="space-y-3.5">
                {customEvents.map((ev) => (
                  <div key={ev.id} className="p-3.5 bg-slate-50 border border-slate-150 rounded-2xl relative group hover:border-indigo-300 transition duration-200">
                    {isPrincipal && (
                      <button
                        onClick={() => handleDeleteEvent(ev.id, ev.title)}
                        className="absolute top-3 right-3 text-slate-400 hover:text-rose-600 opacity-0 group-hover:opacity-100 transition duration-150 p-1 bg-white hover:bg-rose-50 border border-slate-100 rounded-lg"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    )}
                    <span className="bg-indigo-100 text-indigo-800 text-[8px] font-black px-2 py-0.5 rounded uppercase">{ev.eventType}</span>
                    <h4 className="text-xs font-black text-slate-900 mt-2 leading-tight pr-4">{ev.title}</h4>
                    <p className="text-[10px] text-indigo-600 font-extrabold mt-1 flex items-center gap-1">
                      <Calendar className="h-3 w-3" /> Date Scheduled: {ev.date}
                    </p>
                    <p className="text-[10px] text-slate-450 italic mt-1.5 leading-relaxed">{ev.description}</p>
                    <div className="mt-2 border-t border-slate-100 pt-2 text-[8px] font-bold text-slate-400 uppercase">
                      Target Audience: <span className="text-slate-600 font-black">{ev.targetAudience}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Recently Celebrated list */}
            <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest flex items-center gap-1.5">
                <CheckCircle className="h-4.5 w-4.5 text-emerald-600" /> Recently Celebrated
              </h3>

              <div className="space-y-3.5">
                {combinedRoster.filter(p => p.birthday.daysUntil > 330).slice(0, 3).map((p) => (
                  <div key={p.id} className="flex gap-3 items-center p-2 rounded-xl">
                    <div className="h-8 w-8 rounded-full bg-emerald-50 border border-emerald-100 text-emerald-700 text-xs font-black flex items-center justify-center uppercase">
                      {p.name.split(" ").map(n => n[0]).slice(0, 2).join("")}
                    </div>
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{p.name}</h4>
                      <p className="text-[9px] text-slate-400 font-bold">Passed {365 - p.birthday.daysUntil} days ago ({p.dob.substring(5)})</p>
                    </div>
                    <Badge variant="success" className="ml-auto text-[8px] uppercase">Wishes Sent</Badge>
                  </div>
                ))}
              </div>
            </div>

          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: GREETING CARDS (CANVAS CREATOR)
          ========================================================= */}
      {activeTab === "greeting_cards" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Customizer sidebar controls */}
          <div className="lg:col-span-4 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs h-fit">
            <div>
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Card Designer Canvas</h3>
              <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Configure digital certificate templates</p>
            </div>

            <div className="space-y-4 pt-2">
              <Select
                label="Choose Roster Recipient *"
                options={[
                  { label: "-- Select Student / Staff --", value: "" },
                  ...combinedRoster.map(p => ({ label: `${p.name} (${p.type})`, value: p.id }))
                ]}
                value={selectedPerson ? selectedPerson.id : ""}
                onChange={(e) => {
                  const match = combinedRoster.find(p => p.id === e.target.value);
                  if (match) {
                    setSelectedPerson(match);
                    setCardMessage(`Dear ${match.name}, St. Jude Boarding School wishes you a fabulous birthday filled with high marks and pleasant memories. Happy Birthday!`);
                  } else {
                    setSelectedPerson(null);
                  }
                }}
              />

              <Select
                label="Select Card Theme Layout"
                options={[
                  { label: "Classic Premium Blue", value: "classic" },
                  { label: "Festive Sparkles Gold", value: "festive" },
                  { label: "Academic Graduation Crest", value: "academic" }
                ]}
                value={cardTheme}
                onChange={(e) => setCardTheme(e.target.value)}
              />

              <div className="flex flex-col gap-1.5 w-full">
                <label className="text-xs font-bold text-slate-650 uppercase tracking-wider">Card Greeting Text</label>
                <textarea
                  rows={4}
                  value={cardMessage}
                  onChange={(e) => setCardMessage(e.target.value)}
                  className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
                  placeholder="Enter customized birthday greeting wishes..."
                />
              </div>

              <Input
                label="Signatory Officer"
                value={principalSig}
                onChange={(e) => setPrincipalSig(e.target.value)}
              />

              <Button
                onClick={handleGenerateCard}
                disabled={isGeneratingCard}
                className="w-full bg-indigo-650 hover:bg-slate-900 text-white font-extrabold py-2.5 rounded-xl transition duration-200"
              >
                {isGeneratingCard ? "Saving Card database..." : "Publish & Generate Card"}
              </Button>
            </div>
          </div>

          {/* Interactive Card Canvas Preview */}
          <div className="lg:col-span-8 space-y-6">
            <div className="bg-slate-950 p-6 rounded-3xl text-slate-400 font-mono text-xs flex justify-between items-center leading-none border border-slate-900 shadow-sm">
              <span className="uppercase text-[10px] tracking-wider text-slate-500 font-bold flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-500" /> Card Rendering Engine View
              </span>
              <span>1920x1080 SVG Sandbox Canvas</span>
            </div>

            {/* Card Body Render */}
            <div className="flex justify-center items-center p-8 bg-slate-900 border border-slate-950 rounded-3xl relative overflow-hidden min-h-[460px]">
              {selectedPerson ? (
                /* The custom themed card */
                <div className={`w-full max-w-lg aspect-[1.6] bg-white rounded-2xl shadow-2xl p-8 relative flex flex-col justify-between border-8 overflow-hidden animate-scale-up ${cardTheme === "festive"
                    ? "border-amber-400 bg-radial-gradient from-amber-50 to-white"
                    : cardTheme === "academic"
                      ? "border-purple-800 bg-radial-gradient from-purple-50 to-white"
                      : "border-blue-800 bg-radial-gradient from-blue-50 to-white"
                  }`}>
                  {/* Backdrop subtle graphics */}
                  <div className="absolute top-0 right-0 -mt-16 -mr-16 h-36 w-36 rounded-full bg-blue-500/5 blur-xl pointer-events-none" />

                  {/* Card Top Branding Header */}
                  <div className="flex justify-between items-start border-b pb-4 border-slate-100">
                    <div className="flex items-center gap-2">
                      <div className={`h-8 w-8 rounded-lg flex items-center justify-center font-black text-white text-[11px] shadow-sm ${cardTheme === "festive" ? "bg-amber-500" : cardTheme === "academic" ? "bg-purple-800" : "bg-blue-600"
                        }`}>
                        SJ
                      </div>
                      <div className="leading-tight text-left">
                        <h4 className="text-[10px] font-black text-slate-800 tracking-tight leading-none">ST. JUDE'S ACADEMY</h4>
                        <span className="text-[7px] text-slate-400 font-bold uppercase tracking-wider leading-none mt-0.5 block">BOARDING ERP WISHES</span>
                      </div>
                    </div>
                    <Badge variant="success" className="text-[7px] uppercase font-bold tracking-widest px-2 py-0.5">Official wish</Badge>
                  </div>

                  {/* Card Middle: Recipient & Wish Message */}
                  <div className="my-6 text-center space-y-3">
                    <span className="text-[10px] font-bold text-slate-450 uppercase tracking-widest block">HAPPY BIRTHDAY WISHES</span>
                    <h3 className="text-xl font-black text-slate-900 tracking-tight">{selectedPerson.name}</h3>
                    <p className="text-xs text-slate-650 leading-relaxed font-semibold italic max-w-sm mx-auto px-4">
                      "{cardMessage}"
                    </p>
                  </div>

                  {/* Card Bottom: Principal Signature & Date */}
                  <div className="flex justify-between items-end border-t pt-4 border-slate-100">
                    <div className="text-left font-semibold">
                      <p className="text-[7px] text-slate-400 font-bold uppercase tracking-wider">Dated</p>
                      <p className="text-[9px] text-slate-800 mt-0.5">{new Date().toISOString().split("T")[0]}</p>
                    </div>

                    <div className="text-right">
                      {/* Signature graphic simulation */}
                      <p className="text-sm font-handwriting text-slate-800 italic pr-2 font-black leading-none">{principalSig}</p>
                      <p className="text-[7px] text-slate-400 font-bold uppercase tracking-wider mt-1 border-t border-slate-100 pt-0.5">Dean of Cabinet</p>
                    </div>
                  </div>
                </div>
              ) : (
                /* No person selected placeholder */
                <div className="text-center space-y-3 text-slate-500">
                  <Gift className="h-12 w-12 text-slate-700 mx-auto animate-pulse" />
                  <p className="text-xs font-semibold uppercase tracking-wider">Recipient Standby Mode</p>
                  <p className="text-[10px] text-slate-600 max-w-xs font-medium leading-relaxed">
                    Select a student or teacher from the customizer sidebar layout to display card canvas coordinates.
                  </p>
                </div>
              )}
            </div>

            {/* Generated Cards library history */}
            <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Digital Certificate Archives</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {birthdayCards.length === 0 ? (
                  <div className="col-span-2 text-center text-slate-400 py-6 italic text-xs">No generated cards found in database.</div>
                ) : (
                  birthdayCards.map((c) => (
                    <div key={c.id} className="bg-slate-50 border border-slate-150 p-4 rounded-2xl flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 bg-indigo-50 border border-indigo-200 text-indigo-700 flex items-center justify-center rounded-xl shrink-0">
                          <Award className="h-5.5 w-5.5" />
                        </div>
                        <div>
                          <h4 className="text-xs font-black text-slate-800">{c.personName}</h4>
                          <p className="text-[9px] text-slate-450 font-bold uppercase mt-1">Theme: {c.theme} • Date: {c.createdAt.split(" ")[0]}</p>
                        </div>
                      </div>

                      <button
                        onClick={() => {
                          const w = combinedRoster.find(x => x.id === c.personDocsId);
                          if (w) setSelectedPerson(w);
                          setCardTheme(c.theme);
                          setCardMessage(c.message);
                          addToast("Card Loaded", `Viewing generated card of ${c.personName}`);
                        }}
                        className="p-2 hover:bg-indigo-150 border border-slate-200 text-indigo-700 hover:bg-indigo-50 rounded-xl transition cursor-pointer"
                        title="View details on Canvas"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: SCHEDULER LOGS SIMULATOR
          ========================================================= */}
      {activeTab === "scheduler" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Info Details panel */}
          <div className="lg:col-span-4 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs h-fit">
            <div>
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Cron Scheduler Controller</h3>
              <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Simulate midnight wish automation</p>
            </div>

            <p className="text-xs text-slate-500 leading-relaxed font-semibold">
              The automated engagement engine checks birthdays at midnight (00:00:00). If a student has their birthday today, wishes are automatically generated and dispatched across 4 SaaS pipelines.
            </p>

            <div className="p-4 bg-indigo-50 border border-indigo-150 rounded-2xl space-y-3 font-semibold text-xs text-indigo-950">
              <h4 className="font-extrabold uppercase tracking-wide flex items-center gap-1.5 leading-none">
                <Info className="h-4 w-4 text-indigo-650" /> SaaS Communication Pipelines
              </h4>
              <ul className="space-y-1.5 list-disc pl-4 text-[11px] leading-normal">
                <li><strong>Push Notification</strong>: Placed in student mobile app drawer</li>
                <li><strong>Email Wish</strong>: Sent to parent's registered email</li>
                <li><strong>WhatsApp Alert</strong>: Sent to parent's active mobile number</li>
                <li><strong>SMS Message</strong>: Cellular emergency wish fallback</li>
              </ul>
            </div>

            {isSuperAdmin && (
              <Button
                onClick={handleRunScheduler}
                disabled={isSchedulerRunning}
                className="w-full bg-rose-600 hover:bg-slate-900 text-white font-extrabold py-2.5 rounded-xl shadow-xs transition duration-200"
              >
                {isSchedulerRunning ? "Dispatched executing..." : "Execute Automated Birthday Cron"}
              </Button>
            )}
          </div>

          {/* Interactive live logs console */}
          <div className="lg:col-span-8 bg-slate-950 rounded-3xl p-6 flex flex-col justify-between border border-slate-900 shadow-md relative overflow-hidden min-h-[460px]">
            <div className="absolute inset-0 bg-scanlines opacity-[0.08] pointer-events-none" />

            {/* Header console */}
            <div className="flex justify-between items-center text-[10px] text-white font-mono bg-black/60 px-3.5 py-2 rounded-xl z-10 border border-slate-900">
              <span className="uppercase font-bold tracking-widest flex items-center gap-1.5">
                <span className={`h-2.5 w-2.5 rounded-full ${isSchedulerRunning ? "bg-rose-500 animate-ping" : "bg-emerald-500"}`} />
                Live Wishes Dispatch logs Console
              </span>
              <span className="text-slate-400">Chronos Service v1.2</span>
            </div>

            {/* Logger box */}
            <div className="flex-1 overflow-y-auto space-y-1 font-mono text-[11px] text-slate-350 bg-black/40 border border-slate-900 rounded-2xl p-4 my-4 max-h-72 text-left leading-relaxed">
              {schedulerLogs.length === 0 ? (
                <div className="h-full flex items-center justify-center text-slate-600 italic">
                  Run simulated midnight scheduler wishes to show dispatch logs.
                </div>
              ) : (
                schedulerLogs.map((log, idx) => {
                  let color = "text-slate-400";
                  if (log.includes("[Error]")) color = "text-rose-500";
                  else if (log.includes("[Cron Service]")) color = "text-indigo-400 font-extrabold";
                  else if (log.includes("[Database]")) color = "text-purple-400";
                  else if (log.includes("[Channel Manager]")) color = "text-amber-400";
                  else if (log.includes("completed")) color = "text-emerald-400 font-black";

                  return (
                    <div key={idx} className={`${color}`}>
                      {log}
                    </div>
                  );
                })
              )}
            </div>

            {/* Progress status indicators */}
            <div className="space-y-2 z-10 relative">
              <div className="w-full bg-slate-900 h-1.5 rounded-full overflow-hidden">
                <div
                  className="bg-indigo-500 h-full rounded-full transition-all duration-300"
                  style={{ width: `${schedulerProgress}%` }}
                />
              </div>
              <div className="flex justify-between items-center text-[9px] font-mono text-slate-400">
                <span>Progress: {schedulerProgress}%</span>
                <span>{isSchedulerRunning ? "CRON RUNNING..." : "SYSTEM IDLE"}</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: HOSTEL WARDEN DESK
          ========================================================= */}
      {activeTab === "warden_desk" && isWarden && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Warden Controls Dashboard */}
          <div className="lg:col-span-4 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs h-fit">
            <div>
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Warden Feasts Planner</h3>
              <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Hostel Birthday Event Requests</p>
            </div>

            <p className="text-xs text-slate-500 leading-relaxed font-semibold">
              Warden Brody and Sarah Jenkins can request custom cakes and coordinate special dinners for hostellers celebrating their birthday.
            </p>

            <Button
              onClick={() => setIsMealRequestOpen(true)}
              className="w-full bg-indigo-650 hover:bg-slate-900 text-white font-extrabold py-2.5 rounded-xl shadow-xs text-xs"
            >
              Order Cake & Dinner Feast
            </Button>

            <div className="p-4 bg-amber-50 border border-amber-150 rounded-2xl space-y-2 text-xs font-semibold text-amber-950">
              <h4 className="font-extrabold uppercase tracking-wide flex items-center gap-1.5 leading-none">
                <Coffee className="h-4 w-4 text-amber-600" /> Warden Task Checklist
              </h4>
              <ul className="space-y-1.5 list-disc pl-4 text-[11px] leading-normal text-amber-900">
                <li>Submit cake orders 24 hours early.</li>
                <li>Coordinate dining hall decorations.</li>
                <li>Flag allergy alerts to mess cooks.</li>
                <li>Confirm curfew extensions for lighting candles.</li>
              </ul>
            </div>
          </div>

          {/* Feasts Orders & Requests list */}
          <div className="lg:col-span-8 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-850 uppercase tracking-widest">Active Feast Orders</h3>

            <div className="overflow-x-auto border border-slate-100 rounded-2xl">
              <table className="w-full text-xs font-semibold text-slate-700 text-left">
                <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                  <tr>
                    <th className="p-3">Student Name</th>
                    <th className="p-3">Grade</th>
                    <th className="p-3">Cake Flavor</th>
                    <th className="p-3">Allergies/Diet</th>
                    <th className="p-3">Feast Date</th>
                    <th className="p-3 text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {mealRequestsList.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="p-8 text-center text-slate-400 italic">No feast requests scheduled.</td>
                    </tr>
                  ) : (
                    mealRequestsList.map((m) => (
                      <tr key={m.id}>
                        <td className="p-3 font-extrabold text-slate-850">{m.studentName}</td>
                        <td className="p-3 text-slate-500">{m.grade}</td>
                        <td className="p-3 text-indigo-700 font-extrabold">{m.flavor}</td>
                        <td className="p-3">
                          {m.diet !== "None" ? (
                            <span className="bg-rose-50 text-rose-700 border border-rose-200 px-1.5 py-0.5 rounded-md text-[9px] font-bold uppercase">
                              {m.diet}
                            </span>
                          ) : (
                            <span className="text-slate-400 font-bold text-[10px]">-</span>
                          )}
                        </td>
                        <td className="p-3 font-mono">{m.date}</td>
                        <td className="p-3">
                          <div className="flex gap-1.5 justify-center items-center">
                            <Badge variant={m.status === "Cake Ordered" ? "success" : "warning"}>
                              {m.status}
                            </Badge>
                            {m.status === "Pending Warden" && (
                              <button
                                onClick={() => handleCakeStatusUpdate(m.id, "Cake Ordered")}
                                className="p-1 text-[8px] font-bold uppercase tracking-wider bg-emerald-600 hover:bg-emerald-700 text-white rounded cursor-pointer"
                              >
                                Approve
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: CLASSROOM ROSTER (TEACHERS)
          ========================================================= */}
      {activeTab === "teacher_desk" && isTeacher && (
        <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
          <div>
            <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Classroom Birthday view</h3>
            <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">View births categorized by academic grades</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
            {/* Grade 6 */}
            <div className="bg-slate-50 border border-slate-150 p-4.5 rounded-2xl space-y-3">
              <h4 className="text-xs font-black text-slate-900 border-b pb-2 border-slate-200 flex justify-between items-center leading-none">
                <span>Grade 6 Cohort</span>
                <span className="text-[9px] font-bold text-slate-400 font-mono">{combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 6")).length} Roster</span>
              </h4>
              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 6")).map(p => (
                  <div key={p.id} className="bg-white p-2.5 rounded-xl border border-slate-100 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-extrabold text-slate-850">{p.name}</p>
                      <p className="text-[9px] text-slate-450 mt-0.5">DOB: {p.dob.substring(5)} • ({p.birthday.daysUntil} Days)</p>
                    </div>
                    {p.birthday.isToday ? (
                      <span className="text-[8px] bg-rose-600 text-white font-black px-1.5 py-0.5 rounded uppercase animate-bounce">Today</span>
                    ) : (
                      <span className="text-[9px] text-slate-400 font-bold">{p.birthday.daysUntil}d</span>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Grade 7 */}
            <div className="bg-slate-50 border border-slate-150 p-4.5 rounded-2xl space-y-3">
              <h4 className="text-xs font-black text-slate-900 border-b pb-2 border-slate-200 flex justify-between items-center leading-none">
                <span>Grade 7 Cohort</span>
                <span className="text-[9px] font-bold text-slate-400 font-mono">{combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 7")).length} Roster</span>
              </h4>
              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 7")).map(p => (
                  <div key={p.id} className="bg-white p-2.5 rounded-xl border border-slate-100 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-extrabold text-slate-850">{p.name}</p>
                      <p className="text-[9px] text-slate-450 mt-0.5">DOB: {p.dob.substring(5)} • ({p.birthday.daysUntil} Days)</p>
                    </div>
                    {p.birthday.isToday ? (
                      <span className="text-[8px] bg-rose-600 text-white font-black px-1.5 py-0.5 rounded uppercase animate-bounce">Today</span>
                    ) : (
                      <span className="text-[9px] text-slate-400 font-bold">{p.birthday.daysUntil}d</span>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Grade 8 */}
            <div className="bg-slate-50 border border-slate-150 p-4.5 rounded-2xl space-y-3">
              <h4 className="text-xs font-black text-slate-900 border-b pb-2 border-slate-200 flex justify-between items-center leading-none">
                <span>Grade 8 Cohort</span>
                <span className="text-[9px] font-bold text-slate-400 font-mono">{combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 8")).length} Roster</span>
              </h4>
              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {combinedRoster.filter(p => p.type === "Student" && p.grade.includes("Grade 8")).map(p => (
                  <div key={p.id} className="bg-white p-2.5 rounded-xl border border-slate-100 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-extrabold text-slate-850">{p.name}</p>
                      <p className="text-[9px] text-slate-450 mt-0.5">DOB: {p.dob.substring(5)} • ({p.birthday.daysUntil} Days)</p>
                    </div>
                    {p.birthday.isToday ? (
                      <span className="text-[8px] bg-rose-600 text-white font-black px-1.5 py-0.5 rounded uppercase animate-bounce">Today</span>
                    ) : (
                      <span className="text-[9px] text-slate-400 font-bold">{p.birthday.daysUntil}d</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: PARENT PORTAL
          ========================================================= */}
      {activeTab === "parent_portal" && isParent && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Child Birthday parameters */}
          <div className="lg:col-span-5 space-y-6">
            <div className="bg-gradient-to-tr from-indigo-900 to-indigo-950 text-white rounded-3xl p-6 border border-slate-850 relative overflow-hidden shadow-md">
              <div className="absolute right-0 bottom-0 -mb-16 -mr-16 h-36 w-36 bg-blue-500/10 rounded-full blur-2xl pointer-events-none" />

              <div className="flex items-center gap-4.5 z-10 relative">
                <div className="h-14 w-14 rounded-full bg-indigo-600 border-2 border-indigo-400 text-white flex items-center justify-center text-lg font-black shrink-0 uppercase shadow-md">
                  {myChild.name.split(" ").map(n => n[0]).slice(0, 2).join("")}
                </div>
                <div>
                  <div className="flex gap-2 items-center">
                    <h4 className="text-base font-extrabold tracking-tight">{myChild.name}</h4>
                    <Badge variant="success">Active Scholar</Badge>
                  </div>
                  <p className="text-xs text-indigo-300 mt-1.5 font-semibold">
                    {myChild.grade} | Admission ID: <span className="text-white font-black">{myChild.admissionNo}</span>
                  </p>
                </div>
              </div>

              <div className="mt-6 border-t border-indigo-800 pt-4 space-y-3 z-10 relative">
                <div className="flex justify-between items-center text-xs leading-relaxed font-semibold">
                  <span className="text-indigo-300">Birth Date</span>
                  <span className="font-mono">{myChild.dob}</span>
                </div>
                <div className="flex justify-between items-center text-xs leading-relaxed font-semibold">
                  <span className="text-indigo-300">Birthday Status</span>
                  {childBirthday.isToday ? (
                    <span className="text-rose-450 font-black animate-pulse">🎂 CELEBRATING TODAY!</span>
                  ) : (
                    <span>In {childBirthday.daysUntil} Days</span>
                  )}
                </div>
              </div>
            </div>

            {/* Wishes template box */}
            <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest flex items-center gap-1.5">
                <Send className="h-4.5 w-4.5 text-indigo-650" /> Send Birthday Wish
              </h3>
              <p className="text-xs text-slate-400 font-semibold leading-relaxed">Submit a personal wish that will show in your child's mobile app drawer.</p>

              <div className="space-y-3 pt-1">
                <textarea
                  rows={3}
                  className="w-full bg-slate-50 border border-slate-150 text-slate-808 rounded-xl p-3 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
                  placeholder="Dear, Wishing you a wonderful birthday! Stay healthy and keep making us proud..."
                />
                <Button
                  onClick={() => addToast("Wish Submitted", "Personal greeting wish queued to child portal display.")}
                  className="w-full bg-indigo-650 hover:bg-slate-900 text-white font-bold py-2 rounded-xl text-xs"
                >
                  Send Greetings Wish
                </Button>
              </div>
            </div>
          </div>

          {/* Child greeting certificates downloads */}
          <div className="lg:col-span-7 bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-850 uppercase tracking-widest">Digital Wishes & Cards Library</h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Download and view cards generated by St. Jude dean cabinet for your child.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              {birthdayCards.filter(c => c.personDocsId === myChild.id).length === 0 ? (
                <div className="col-span-2 text-center text-slate-400 py-12 italic text-xs border border-dashed rounded-2xl">
                  No greeting cards have been generated for your child yet.
                </div>
              ) : (
                birthdayCards.filter(c => c.personDocsId === myChild.id).map(c => (
                  <div key={c.id} className="bg-slate-50 border border-slate-150 rounded-2xl overflow-hidden shadow-3xs flex flex-col justify-between">
                    <div className="h-32 bg-slate-200 relative">
                      <img src={c.cardUrl} alt="Greeting Card" className="h-full w-full object-cover" />
                      <div className="absolute top-2 left-2 bg-black/60 text-white text-[8px] font-bold px-2 py-0.5 rounded-lg">
                        {c.theme.toUpperCase()}
                      </div>
                    </div>
                    <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
                      <p className="text-[11px] text-slate-650 leading-relaxed font-semibold italic">"{c.message}"</p>

                      <div className="border-t pt-3 flex justify-between items-center">
                        <span className="text-[8px] font-mono text-slate-400">{c.createdAt.split(" ")[0]}</span>
                        <Button
                          size="sm"
                          onClick={() => addToast("PDF Download Initiated", "Simulated digital certificate download complete.")}
                          className="bg-indigo-650 hover:bg-slate-900 text-white text-[9px] font-bold uppercase py-1 px-3 rounded flex items-center gap-1 cursor-pointer"
                        >
                          <Download className="h-3 w-3" /> Download Card (PDF)
                        </Button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: CELEBRATION GALLERY
          ========================================================= */}
      {activeTab === "gallery" && (
        <div className="space-y-6">
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div>
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Birthday Media Gallery</h3>
              <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Browse cake cutting photos and dorm party snapshots</p>
            </div>

            {isWarden && (
              <Button
                onClick={() => setIsUploadPhotoOpen(true)}
                className="bg-indigo-650 hover:bg-slate-900 text-white font-extrabold py-1.5 px-4 rounded-xl text-xs flex items-center gap-1.5"
              >
                <Camera className="h-4 w-4 shrink-0" /> Upload Celebration Photo
              </Button>
            )}
          </div>

          {birthdayGallery.length === 0 ? (
            <div className="bg-white border border-slate-100 rounded-3xl p-12 text-center text-slate-400 italic text-xs">No birthday celebration photos uploaded yet.</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {birthdayGallery.map((img) => (
                <div key={img.id} className="bg-white border border-slate-100 rounded-2xl overflow-hidden shadow-2xs hover:shadow-md transition duration-200 flex flex-col">
                  <div className="h-40 bg-slate-900 overflow-hidden relative">
                    <img src={img.mediaUrl} alt={img.caption} className="h-full w-full object-cover" />
                    <div className="absolute top-2.5 left-2.5 bg-black/60 text-white text-[8px] font-mono font-bold px-2 py-0.5 rounded uppercase">
                      {img.personName}
                    </div>
                  </div>
                  <div className="p-4 flex-1 flex flex-col justify-between">
                    <p className="text-[11px] text-slate-650 leading-relaxed font-semibold italic">"{img.caption}"</p>
                    <div className="pt-3 border-t border-slate-50 mt-3 flex justify-between items-center text-[9px] font-mono text-slate-400 leading-none">
                      <span>Ref: {img.id}</span>
                      <span>{img.createdAt.split(" ")[0]}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: CUSTOM EVENTS (ADMIN CALENDAR EDITOR)
          ========================================================= */}
      {activeTab === "custom_events" && isPrincipal && (
        <div className="bg-white border border-slate-105 rounded-3xl p-6 space-y-4 shadow-2xs">
          <div className="flex justify-between items-center border-b pb-4 border-slate-100">
            <div>
              <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest">Custom Celebration Planner</h3>
              <p className="text-[10px] text-slate-400 font-bold mt-0.5 uppercase tracking-wider">Schedule institutional milestones and work anniversaries</p>
            </div>

            <Button
              onClick={() => setIsAddEventOpen(true)}
              className="bg-indigo-650 hover:bg-slate-900 text-white font-extrabold py-2 px-4 rounded-xl text-xs flex items-center gap-1.5"
            >
              <Plus className="h-4.5 w-4.5 shrink-0" /> Schedule Custom Event
            </Button>
          </div>

          <div className="overflow-x-auto border border-slate-100 rounded-2xl">
            <table className="w-full text-xs font-semibold text-slate-700 text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400 tracking-wider">
                <tr>
                  <th className="p-3">Event Title</th>
                  <th className="p-3">Category Type</th>
                  <th className="p-3">Scheduled Date</th>
                  <th className="p-3">Description</th>
                  <th className="p-3">Target Audience</th>
                  <th className="p-3 text-center">Desk Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {customEvents.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-slate-400 italic">No scheduled custom events.</td>
                  </tr>
                ) : (
                  customEvents.map((ev) => (
                    <tr key={ev.id}>
                      <td className="p-3 font-extrabold text-slate-850">{ev.title}</td>
                      <td className="p-3">
                        <Badge variant="secondary">{ev.eventType}</Badge>
                      </td>
                      <td className="p-3 font-mono text-indigo-705 font-bold">{ev.date}</td>
                      <td className="p-3 text-slate-500 max-w-xs truncate">{ev.description}</td>
                      <td className="p-3 text-slate-808 font-black">{ev.targetAudience}</td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => handleDeleteEvent(ev.id, ev.title)}
                          className="bg-rose-50 border border-rose-100 hover:bg-rose-600 hover:text-white text-rose-700 text-[10px] uppercase p-1.5 px-3 rounded-lg font-bold transition cursor-pointer"
                        >
                          Cancel Event
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* =========================================================
          TAB CONTENT: DEVELOPER CONSOLE (REST API & SCHEMA REFERENCE)
          ========================================================= */}
      {activeTab === "api_docs" && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* REST API Endpoints spec */}
          <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest flex items-center gap-1.5">
              <Code className="h-4.5 w-4.5 text-indigo-650" /> REST API Endpoint Reference
            </h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Integrated OpenAPI specs for multi-tenant backend developer teams.</p>

            <div className="space-y-4 pt-1 text-xs">
              <div className="p-4 bg-slate-900 border border-slate-950 text-slate-200 rounded-2xl font-mono leading-relaxed space-y-2">
                <div className="flex gap-2 items-center border-b border-slate-800 pb-2 mb-2">
                  <span className="bg-emerald-600 text-white font-extrabold text-[8px] px-2 py-0.5 rounded uppercase font-mono">GET</span>
                  <span className="text-white text-[11px] font-black tracking-wide">/api/v1/birthdays/today</span>
                </div>
                <p className="text-[10px] text-slate-400 italic">Fetch active student and staff birthdays for the current tenant's date.</p>
                <div className="text-[9px] text-slate-350 bg-slate-950/80 p-2 rounded border border-slate-900">
                  {`{\n  "tenant_id": "school-01",\n  "date": "2026-06-11",\n  "celebrants": [\n    { "id": "student-1", "name": "Aarav Sharma", "type": "student", "class": "Class 5-A" }\n  ]\n}`}
                </div>
              </div>

              <div className="p-4 bg-slate-900 border border-slate-950 text-slate-200 rounded-2xl font-mono leading-relaxed space-y-2">
                <div className="flex gap-2 items-center border-b border-slate-800 pb-2 mb-2">
                  <span className="bg-blue-600 text-white font-extrabold text-[8px] px-2 py-0.5 rounded uppercase font-mono">POST</span>
                  <span className="text-white text-[11px] font-black tracking-wide">/api/v1/wishes/dispatch</span>
                </div>
                <p className="text-[10px] text-slate-400 italic">Dispatches midnight automated greeting card cards and communications alerts.</p>
                <div className="text-[9px] text-slate-350 bg-slate-950/80 p-2 rounded border border-slate-900">
                  {`// Request Body\n{\n  "tenant_id": "school-01",\n  "recipient_type": "student",\n  "recipient_id": "student-1",\n  "channels": ["Email", "SMS", "WhatsApp"]\n}`}
                </div>
              </div>
            </div>
          </div>

          {/* Database Schema Reference */}
          <div className="bg-white border border-slate-100 p-6 rounded-3xl space-y-4 shadow-2xs">
            <h3 className="text-xs font-black text-slate-808 uppercase tracking-widest flex items-center gap-1.5">
              <Database className="h-4.5 w-4.5 text-indigo-650" /> Database Schema Spec (SQL)
            </h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Relational database configurations for Postgres/MySQL core structures.</p>

            <div className="p-4 bg-slate-900 border border-slate-950 text-slate-200 rounded-2xl font-mono leading-relaxed text-[10px] space-y-4 max-h-[360px] overflow-y-auto">
              <pre className="text-slate-300 leading-normal font-mono">{`CREATE TABLE birthdays (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  person_type VARCHAR(16) CHECK (person_type IN ('student', 'staff')),
  person_id VARCHAR(64) NOT NULL,
  birth_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE birthday_notifications (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  birthday_id VARCHAR(64) REFERENCES birthdays(id),
  notification_type VARCHAR(16) CHECK (notification_type IN ('Push', 'Email', 'SMS', 'WhatsApp')),
  recipient VARCHAR(128) NOT NULL,
  sent_at TIMESTAMP NOT NULL
);

CREATE TABLE birthday_cards (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  birthday_id VARCHAR(64) REFERENCES birthdays(id),
  card_url VARCHAR(256) NOT NULL,
  message TEXT NOT NULL,
  theme VARCHAR(32) DEFAULT 'classic',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE birthday_gallery (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  birthday_id VARCHAR(64) REFERENCES birthdays(id),
  media_url VARCHAR(256) NOT NULL,
  caption VARCHAR(256),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);`}</pre>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          POPUPS & DIALOG DIALOG BOXES
          ========================================================= */}

      {/* 1. Add Custom Event Dialog */}
      <Dialog
        isOpen={isAddEventOpen}
        onClose={() => setIsAddEventOpen(false)}
        title="Schedule Custom Celebration Event"
      >
        <form onSubmit={handleAddCustomEvent} className="space-y-4 pt-1">
          <Input
            label="Celebration Title *"
            value={newEventTitle}
            onChange={(e) => setNewEventTitle(e.target.value)}
            placeholder="School Foundation Day 2026"
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Event Category Type"
              options={[
                { label: "School Foundation Day", value: "School Foundation Day" },
                { label: "Work Anniversary", value: "Work Anniversary" },
                { label: "Teacher's Day Tribute", value: "Teacher's Day" },
                { label: "Graduation Day Ceremony", value: "Graduation Day" },
                { label: "Achievement Celebration", value: "Achievement Celebrations" }
              ]}
              value={newEventType}
              onChange={(e) => setNewEventType(e.target.value)}
            />

            <Input
              label="Scheduled Date *"
              type="date"
              value={newEventDate}
              onChange={(e) => setNewEventDate(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5 w-full">
            <label className="text-xs font-bold text-slate-650 uppercase tracking-wider">Event Description</label>
            <textarea
              rows={3}
              value={newEventDesc}
              onChange={(e) => setNewEventDesc(e.target.value)}
              className="bg-slate-50 border border-slate-200 text-slate-800 rounded-xl p-3 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
              placeholder="Provide event details, timing, and room locations..."
            />
          </div>

          <Select
            label="Target Audience"
            options={[
              { label: "All School Members", value: "All" },
              { label: "Students & Parents", value: "Students" },
              { label: "Staff & Teachers", value: "Staff" }
            ]}
            value={newEventTarget}
            onChange={(e) => setNewEventTarget(e.target.value)}
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsAddEventOpen(false)}>Cancel</Button>
            <Button variant="secondary" size="sm" type="submit">Schedule Celebration</Button>
          </div>
        </form>
      </Dialog>

      {/* 2. Upload Celebration Photo Dialog */}
      <Dialog
        isOpen={isUploadPhotoOpen}
        onClose={() => setIsUploadPhotoOpen(false)}
        title="Upload Cake Cutting Photo"
      >
        <form onSubmit={handleUploadBirthdayPhoto} className="space-y-4 pt-1">
          <Select
            label="Select Birthday Student *"
            options={[
              { label: "-- Select Celebrant Student --", value: "" },
              ...combinedRoster.filter(p => p.type === "Student").map(s => ({ label: `${s.name} (${s.grade})`, value: s.id }))
            ]}
            value={photoStudentDocsId}
            onChange={(e) => setPhotoStudentDocsId(e.target.value)}
            required
          />

          <Input
            label="Photo URL Reference *"
            value={photoUrl}
            onChange={(e) => setPhotoUrl(e.target.value)}
            placeholder="https://images.unsplash.com/photo-..."
            required
          />

          <Input
            label="Photo Caption Description"
            value={photoCaption}
            onChange={(e) => setPhotoCaption(e.target.value)}
            placeholder="Cutting the birthday chocolate cake in Vanguard Hall..."
          />

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsUploadPhotoOpen(false)}>Cancel</Button>
            <Button variant="secondary" size="sm" type="submit">Upload and Link Photo</Button>
          </div>
        </form>
      </Dialog>

      {/* 3. Warden Meal Feasts Requests Dialog */}
      <Dialog
        isOpen={isMealRequestOpen}
        onClose={() => setIsMealRequestOpen(false)}
        title="Submit Hostel Feast & Cake Request"
      >
        <form onSubmit={handleAddMealRequest} className="space-y-4 pt-1">
          <Select
            label="Celebrant Student *"
            options={[
              { label: "-- Select Student --", value: "" },
              ...studentRoster.map(s => ({ label: `${s.name} (${s.grade})`, value: s.id }))
            ]}
            value={mealStudentDocsId}
            onChange={(e) => setMealStudentDocsId(e.target.value)}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Birthday Cake Flavor"
              options={[
                { label: "Chocolate Fudge Premium", value: "Chocolate Fudge" },
                { label: "Classic Red Velvet", value: "Red Velvet" },
                { label: "Vanilla Buttercream Fruit", value: "Vanilla Buttercream" },
                { label: "Strawberry Cheesecake Cupcake", value: "Strawberry Cheesecake" }
              ]}
              value={mealCakeFlavor}
              onChange={(e) => setMealCakeFlavor(e.target.value)}
            />

            <Select
              label="Special Dietary/Allergy Notes"
              options={[
                { label: "None (Standard menu)", value: "None" },
                { label: "Nut Allergy Alert", value: "Nut Allergy Alert" },
                { label: "Gluten Free Cake Request", value: "Gluten Free" },
                { label: "Lactose Intolerant Substitutes", value: "Lactose Intolerant" }
              ]}
              value={mealSpecialDiet}
              onChange={(e) => setMealSpecialDiet(e.target.value)}
            />
          </div>

          <Input
            label="Target Dinner Feast Date *"
            type="date"
            value={mealDate}
            onChange={(e) => setMealDate(e.target.value)}
            required
          />

          <div className="p-3 bg-amber-50/50 border border-amber-200 text-amber-900 text-[10px] rounded-xl font-bold leading-normal">
            Feast orders will be directly synchronized with the Food & Mess module dashboard for kitchen staff.
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsMealRequestOpen(false)}>Cancel</Button>
            <Button variant="secondary" size="sm" type="submit">Order Feast Dinner</Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
