import { useState, useEffect, useCallback } from 'react';
import { 
  Users, Megaphone, Bell, Plus, Trash2, Edit2, Check, 
  Mail, Phone, MapPin, Send, RefreshCw, Layers, ShieldAlert,
  Calendar, Award
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';
import AcademicYearsScreen from './AcademicYearsScreen.jsx';

export default function CoreScreen({ schoolId, schools, year, years, reload }) {
  const toast = useToast();
  const [subTab, setSubTab] = useState('schools'); // 'schools', 'announcements', 'notifications'
  
  // Data lists
  const [localSchools, setLocalSchools] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [notifications, setNotifications] = useState([]);

  // Load states
  const [loadingSchools, setLoadingSchools] = useState(false);
  const [loadingAnnouncements, setLoadingAnnouncements] = useState(false);
  const [loadingNotifications, setLoadingNotifications] = useState(false);

  // Form busy states
  const [busySchool, setBusySchool] = useState(false);
  const [busyAnnouncement, setBusyAnnouncement] = useState(false);
  const [busyNotification, setBusyNotification] = useState(false);

  // --- SCHOOL FORM STATE ---
  const [editingSchool, setEditingSchool] = useState(null); // null if creating, school object if editing
  const [schoolForm, setSchoolForm] = useState({
    schoolName: '',
    subdomain: '',
    logo: 'https://placehold.co/150',
    address: '',
    phone: '',
    email: '',
    subscriptionTier: 'FREE',
    maxStudents: 500,
    maxUsers: 50,
    active: true
  });

  // --- ANNOUNCEMENT FORM STATE ---
  const [announcementForm, setAnnouncementForm] = useState({
    title: '',
    content: '',
    target: 'ALL',
    sender: "Principal's Office",
    date: new Date().toISOString().slice(0, 10)
  });

  // --- NOTIFICATION FORM STATE ---
  const [notificationForm, setNotificationForm] = useState({
    recipientId: '',
    channel: 'PUSH',
    message: '',
    sent: false
  });

  // Load Schools
  const fetchSchools = useCallback(async () => {
    setLoadingSchools(true);
    try {
      const data = await api.getAllSchools();
      setLocalSchools(data);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load schools list.");
    } finally {
      setLoadingSchools(false);
    }
  }, [toast]);

  // Load Announcements (filtered by school if schoolId is selected)
  const fetchAnnouncements = useCallback(async () => {
    setLoadingAnnouncements(true);
    try {
      const data = schoolId 
        ? await api.getAnnouncementsBySchool(schoolId)
        : await api.getAllAnnouncements();
      // Sort newest first
      const sorted = (data || []).sort((a, b) => (b.date || '').localeCompare(a.date || ''));
      setAnnouncements(sorted);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load announcements.");
    } finally {
      setLoadingAnnouncements(false);
    }
  }, [schoolId, toast]);

  // Load Notifications (filtered by school if schoolId is selected)
  const fetchNotifications = useCallback(async () => {
    setLoadingNotifications(true);
    try {
      const data = schoolId
        ? await api.getNotificationsBySchool(schoolId)
        : await api.getAllNotifications();
      // Sort by creation date or ID descending
      setNotifications(data || []);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load notifications log.");
    } finally {
      setLoadingNotifications(false);
    }
  }, [schoolId, toast]);

  // Initial load & reactive loads
  useEffect(() => {
    fetchSchools();
  }, [fetchSchools]);

  useEffect(() => {
    fetchAnnouncements();
  }, [fetchAnnouncements]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  // Reload all helper
  const reloadAll = () => {
    fetchSchools();
    fetchAnnouncements();
    fetchNotifications();
    if (reload) reload(); // reload top bar schools
  };

  // --- SCHOOLS CRUD ---
  const handleEditSchoolClick = (s) => {
    setEditingSchool(s);
    setSchoolForm({
      schoolName: s.schoolName || '',
      subdomain: s.subdomain || '',
      logo: s.logo || 'https://placehold.co/150',
      address: s.address || '',
      phone: s.phone || '',
      email: s.email || '',
      subscriptionTier: s.subscriptionTier || 'FREE',
      maxStudents: s.maxStudents || 500,
      maxUsers: s.maxUsers || 50,
      active: s.active !== false
    });
  };

  const handleCancelSchoolEdit = () => {
    setEditingSchool(null);
    setSchoolForm({
      schoolName: '',
      subdomain: '',
      logo: 'https://placehold.co/150',
      address: '',
      phone: '',
      email: '',
      subscriptionTier: 'FREE',
      maxStudents: 500,
      maxUsers: 50,
      active: true
    });
  };

  const submitSchool = async () => {
    if (!schoolForm.schoolName || !schoolForm.subdomain) {
      toast.error("School Name and Subdomain are required.");
      return;
    }
    setBusySchool(true);
    try {
      if (editingSchool) {
        // Edit School
        await api.updateSchool(editingSchool.id, schoolForm);
        toast.success(`School "${schoolForm.schoolName}" updated.`);
        setEditingSchool(null);
      } else {
        // Create School
        await api.createSchool(schoolForm);
        toast.success(`School "${schoolForm.schoolName}" registered.`);
      }
      // Reset Form
      setSchoolForm({
        schoolName: '',
        subdomain: '',
        logo: 'https://placehold.co/150',
        address: '',
        phone: '',
        email: '',
        subscriptionTier: 'FREE',
        maxStudents: 500,
        maxUsers: 50,
        active: true
      });
      reloadAll();
    } catch (e) {
      toast.error(e.message || "Failed to save school.");
    } finally {
      setBusySchool(false);
    }
  };

  const toggleSchoolActive = async (s) => {
    try {
      const nextActive = !s.active;
      await api.updateSchool(s.id, { active: nextActive });
      toast.success(`School "${s.schoolName}" is now ${nextActive ? 'Active' : 'Inactive'}.`);
      reloadAll();
    } catch (e) {
      toast.error("Failed to update status: " + e.message);
    }
  };

  const deleteSchool = async (s) => {
    if (!confirm(`Delete School "${s.schoolName}"? This will delete all associated data (timetables, classes, academic years).`)) return;
    try {
      await api.deleteSchool(s.id);
      toast.success(`School "${s.schoolName}" deleted.`);
      reloadAll();
    } catch (e) {
      toast.error("Failed to delete school: " + e.message);
    }
  };

  // --- ANNOUNCEMENT CRUD ---
  const postAnnouncement = async () => {
    if (!schoolId) {
      toast.error("Please pick a school in the top bar before posting.");
      return;
    }
    if (!announcementForm.title || !announcementForm.content) {
      toast.error("Title and Content are required.");
      return;
    }
    setBusyAnnouncement(true);
    try {
      await api.createAnnouncement({
        schoolId,
        ...announcementForm
      });
      toast.success("Announcement broadcasted successfully!");
      setAnnouncementForm({
        title: '',
        content: '',
        target: 'ALL',
        sender: "Principal's Office",
        date: new Date().toISOString().slice(0, 10)
      });
      fetchAnnouncements();
    } catch (e) {
      toast.error("Failed to post announcement: " + e.message);
    } finally {
      setBusyAnnouncement(false);
    }
  };

  const removeAnnouncement = async (ann) => {
    if (!confirm(`Retract announcement "${ann.title}"?`)) return;
    try {
      await api.deleteAnnouncement(ann.id);
      toast.success("Announcement retracted.");
      fetchAnnouncements();
    } catch (e) {
      toast.error("Failed to delete announcement: " + e.message);
    }
  };

  // --- NOTIFICATION CRUD ---
  const dispatchNotification = async () => {
    if (!schoolId) {
      toast.error("Please pick a school in the top bar first.");
      return;
    }
    if (!notificationForm.recipientId || !notificationForm.message) {
      toast.error("Recipient ID and Message content are required.");
      return;
    }
    setBusyNotification(true);
    try {
      await api.createNotification({
        schoolId,
        ...notificationForm
      });
      toast.success("Notification created.");
      setNotificationForm({
        recipientId: '',
        channel: 'PUSH',
        message: '',
        sent: false
      });
      fetchNotifications();
    } catch (e) {
      toast.error("Failed to dispatch notification: " + e.message);
    } finally {
      setBusyNotification(false);
    }
  };

  const markNotificationSent = async (notif) => {
    try {
      await api.markNotificationAsSent(notif.id);
      toast.success("Notification status marked as DELIVERED.");
      fetchNotifications();
    } catch (e) {
      toast.error("Failed to update status: " + e.message);
    }
  };

  const deleteNotification = async (notif) => {
    if (!confirm(`Delete notification record?`)) return;
    try {
      await api.deleteNotification(notif.id);
      toast.success("Notification record removed.");
      fetchNotifications();
    } catch (e) {
      toast.error("Failed to delete notification: " + e.message);
    }
  };

  // Color mappings
  const getTierColor = (tier) => {
    switch (tier) {
      case 'ENTERPRISE': return 'amber';
      case 'PREMIUM': return 'green';
      case 'BASIC': return 'blue';
      default: return 'slate';
    }
  };

  const getChannelColor = (channel) => {
    switch (channel) {
      case 'EMAIL': return 'green';
      case 'SMS': return 'amber';
      case 'WHATSAPP': return 'emerald';
      default: return 'blue'; // PUSH
    }
  };

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800">
      {/* Tab Select Header */}
      <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm justify-between items-center shrink-0">
        <div className="flex gap-1">
          <button
            onClick={() => setSubTab('schools')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'schools' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Users size={16} />
            Manage Schools
          </button>
          <button
            onClick={() => setSubTab('academicYears')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'academicYears' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Calendar size={16} />
            Academic Years
          </button>
          <button
            onClick={() => setSubTab('announcements')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'announcements' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Megaphone size={16} />
            Announcements Broadcast
          </button>
          <button
            onClick={() => setSubTab('notifications')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'notifications' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Bell size={16} />
            Notifications Dispatcher
          </button>
        </div>
        
        <button 
          onClick={reloadAll}
          className="flex items-center gap-1 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold mr-2 mb-2 transition"
          title="Refresh Data"
        >
          <RefreshCw size={13} />
          Reload List
        </button>
      </div>

      {/* Main panel layout */}
      <div className="flex-1 min-h-0 overflow-y-auto">
        {/* ==================== ACADEMIC YEARS TAB ==================== */}
        {subTab === 'academicYears' && (
          <AcademicYearsScreen 
            schoolId={schoolId} 
            years={years} 
            year={year} 
            reload={reload} 
          />
        )}

        {/* ==================== SCHOOLS TAB ==================== */}
        {subTab === 'schools' && (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
            {/* Left side: Schools Table */}
            <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">Registered Schools</h3>
                  <p className="text-xs text-slate-500 mt-0.5">View and manage schools registered on Edusphere.</p>
                </div>
                <span className="text-xs font-medium px-2 py-0.5 bg-blue-100 text-blue-800 rounded-full font-semibold">
                  {localSchools.length} Total
                </span>
              </header>

              <div className="flex-1 overflow-x-auto">
                {loadingSchools ? (
                  <Empty icon={RefreshCw} title="Loading schools..." hint="Please wait." />
                ) : localSchools.length === 0 ? (
                  <Empty icon={Users} title="No schools found" hint="Register your first school using the panel on the right." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">School details</th>
                        <th className="px-4 py-3">Contact</th>
                        <th className="px-4 py-3">Subscription</th>
                        <th className="px-4 py-3 text-center">Limits</th>
                        <th className="px-4 py-3 text-center">Status</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {localSchools.map((s) => (
                        <tr key={s.id} className="hover:bg-slate-50/50 transition">
                          {/* details */}
                          <td className="px-4 py-3 min-w-[200px]">
                            <div className="flex items-center gap-3">
                              <img 
                                src={s.logo || 'https://placehold.co/150'} 
                                alt="logo" 
                                className="w-9 h-9 rounded-lg object-cover border border-slate-200 shrink-0"
                                onError={(e) => { e.target.src = 'https://placehold.co/150'; }} 
                              />
                              <div className="min-w-0">
                                <div className="font-bold text-slate-900 text-xs truncate">{s.schoolName}</div>
                                <div className="text-[10px] text-slate-400 font-mono select-all mt-0.5">{s.subdomain}.edusphere.com</div>
                              </div>
                            </div>
                          </td>
                          {/* contact */}
                          <td className="px-4 py-3">
                            <div className="flex flex-col gap-1 text-[11px] text-slate-600">
                              {s.email && <div className="flex items-center gap-1.5"><Mail size={11} className="text-slate-400" /> {s.email}</div>}
                              {s.phone && <div className="flex items-center gap-1.5"><Phone size={11} className="text-slate-400" /> {s.phone}</div>}
                            </div>
                          </td>
                          {/* subscription */}
                          <td className="px-4 py-3">
                            <Badge color={getTierColor(s.subscriptionTier)}>{s.subscriptionTier || 'FREE'}</Badge>
                          </td>
                          {/* limits */}
                          <td className="px-4 py-3 text-center text-[11px]">
                            <div>Students: {s.maxStudents || '—'}</div>
                            <div className="text-slate-400 mt-0.5">Users: {s.maxUsers || '—'}</div>
                          </td>
                          {/* status */}
                          <td className="px-4 py-3 text-center">
                            <button
                              onClick={() => toggleSchoolActive(s)}
                              className={`px-3 py-1 rounded-full text-[10px] font-bold transition-all border ${
                                s.active !== false
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                                  : 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100'
                              }`}
                              title="Click to toggle active status"
                            >
                              {s.active !== false ? 'Active' : 'Inactive'}
                            </button>
                          </td>
                          {/* actions */}
                          <td className="px-4 py-3 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              <button 
                                onClick={() => handleEditSchoolClick(s)}
                                className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                                title="Edit school info"
                              >
                                <Edit2 size={13} />
                              </button>
                              <button 
                                onClick={() => deleteSchool(s)}
                                className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-1.5 rounded-lg transition"
                                title="Delete school"
                              >
                                <Trash2 size={13} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Right side: Add / Edit Panel */}
            <div className="xl:col-span-4">
              <Card 
                title={editingSchool ? "Edit School Info" : "Register New School"} 
                subtitle={editingSchool ? `Modifying school profile: ${editingSchool.schoolName}` : "Add a new school institute profile to the network."}
              >
                <div className="space-y-4">
                  <Field label="School Name *">
                    <Input 
                      value={schoolForm.schoolName} 
                      onChange={(e) => setSchoolForm({...schoolForm, schoolName: e.target.value})} 
                      placeholder="e.g. St. Xavier Academy"
                    />
                  </Field>
                  <Field label="Unique Subdomain *">
                    <div className="flex items-center gap-1.5 bg-slate-50 border border-slate-200 rounded-lg pr-3 overflow-hidden focus-within:border-blue-400 focus-within:ring-2 focus-within:ring-blue-100">
                      <input 
                        className="flex-1 px-3 py-2 text-sm bg-transparent outline-none border-none text-slate-800"
                        value={schoolForm.subdomain} 
                        onChange={(e) => setSchoolForm({...schoolForm, subdomain: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '')})} 
                        placeholder="st-xavier" 
                      />
                      <span className="text-[11px] font-semibold text-slate-400">.edusphere</span>
                    </div>
                  </Field>
                  <Field label="Logo URL">
                    <Input 
                      value={schoolForm.logo} 
                      onChange={(e) => setSchoolForm({...schoolForm, logo: e.target.value})} 
                      placeholder="https://example.com/logo.png"
                    />
                  </Field>
                  <Field label="Institute Email">
                    <Input 
                      type="email"
                      value={schoolForm.email} 
                      onChange={(e) => setSchoolForm({...schoolForm, email: e.target.value})} 
                      placeholder="admin@stxavier.edu"
                    />
                  </Field>
                  <Field label="Institute Phone">
                    <Input 
                      value={schoolForm.phone} 
                      onChange={(e) => setSchoolForm({...schoolForm, phone: e.target.value})} 
                      placeholder="+1 555-0199"
                    />
                  </Field>
                  <Field label="Postal Address">
                    <Input 
                      value={schoolForm.address} 
                      onChange={(e) => setSchoolForm({...schoolForm, address: e.target.value})} 
                      placeholder="123 Education Lane"
                    />
                  </Field>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Max Students">
                      <Input 
                        type="number"
                        value={schoolForm.maxStudents} 
                        onChange={(e) => setSchoolForm({...schoolForm, maxStudents: parseInt(e.target.value) || 0})} 
                      />
                    </Field>
                    <Field label="Max Admin Users">
                      <Input 
                        type="number"
                        value={schoolForm.maxUsers} 
                        onChange={(e) => setSchoolForm({...schoolForm, maxUsers: parseInt(e.target.value) || 0})} 
                      />
                    </Field>
                  </div>

                  <Field label="Subscription Tier">
                    <Select 
                      value={schoolForm.subscriptionTier} 
                      onChange={(e) => setSchoolForm({...schoolForm, subscriptionTier: e.target.value})}
                    >
                      <option value="FREE">FREE</option>
                      <option value="BASIC">BASIC</option>
                      <option value="PREMIUM">PREMIUM</option>
                      <option value="ENTERPRISE">ENTERPRISE</option>
                    </Select>
                  </Field>

                  <div className="flex items-center gap-2 pt-2">
                    <input 
                      type="checkbox" 
                      id="form_active_check" 
                      checked={schoolForm.active}
                      onChange={(e) => setSchoolForm({...schoolForm, active: e.target.checked})}
                      className="h-4 w-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500" 
                    />
                    <label htmlFor="form_active_check" className="text-xs font-semibold text-slate-600 select-none">Active Profile</label>
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                    {editingSchool && (
                      <Button variant="default" onClick={handleCancelSchoolEdit} disabled={busySchool}>
                        Cancel
                      </Button>
                    )}
                    <Button variant="primary" onClick={submitSchool} disabled={busySchool || !schoolForm.schoolName || !schoolForm.subdomain}>
                      {busySchool ? <RefreshCw size={14} className="animate-spin" /> : <Check size={14} />}
                      {editingSchool ? 'Update School' : 'Register School'}
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        )}

        {/* ==================== ANNOUNCEMENTS TAB ==================== */}
        {subTab === 'announcements' && (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
            {/* Left side: Announcements Timeline Feed */}
            <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">School Bulletins & Announcements</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {schoolId ? "Showing bulletins for current school." : "Select a school to view or post bulletins."}
                  </p>
                </div>
                <Badge color="blue">{announcements.length} Total</Badge>
              </header>

              <div className="flex-1 p-5 overflow-y-auto bg-slate-50/40">
                {loadingAnnouncements ? (
                  <Empty icon={RefreshCw} title="Loading announcement history..." hint="Please wait." />
                ) : announcements.length === 0 ? (
                  <Empty icon={Megaphone} title="No bulletins posted yet" hint="Draft your first bulletin announcement on the right." />
                ) : (
                  <div className="space-y-4">
                    {announcements.map((ann) => (
                      <div key={ann.id} className="bg-white border border-slate-200/80 rounded-2xl p-5 hover:shadow-sm transition duration-200 relative group">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <h4 className="font-bold text-slate-900 text-sm">{ann.title}</h4>
                              <Badge color={ann.target === 'STUDENT' ? 'green' : ann.target === 'STAFF' ? 'blue' : 'slate'}>
                                To: {ann.target || 'ALL'}
                              </Badge>
                            </div>
                            <div className="flex items-center gap-4 text-[10px] text-slate-400 mt-1 font-semibold">
                              <span className="flex items-center gap-1"><Calendar size={11} /> {ann.date || '—'}</span>
                              <span className="flex items-center gap-1"><Award size={11} /> Posted by {ann.sender || 'System'}</span>
                            </div>
                          </div>
                          
                          <button
                            onClick={() => removeAnnouncement(ann)}
                            className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-2 rounded-xl transition duration-150 shrink-0"
                            title="Retract / Delete announcement"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>

                        <p className="text-xs text-slate-600 leading-relaxed mt-4 whitespace-pre-wrap select-text border-t border-slate-50 pt-3">
                          {ann.content}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Right side: Post Announcement form */}
            <div className="xl:col-span-4">
              <Card 
                title="Post Announcement" 
                subtitle="Broadcast notification bulletins to staff, students, or everyone."
              >
                {!schoolId ? (
                  <Empty icon={Megaphone} title="Pick a school context" hint="Use the school selector in the top bar to enable posting." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Bulletin Title *">
                      <Input 
                        value={announcementForm.title}
                        onChange={(e) => setAnnouncementForm({...announcementForm, title: e.target.value})}
                        placeholder="e.g. Summer Vacation Commencement"
                      />
                    </Field>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Broadcast Date">
                        <Input 
                          type="date"
                          value={announcementForm.date}
                          onChange={(e) => setAnnouncementForm({...announcementForm, date: e.target.value})}
                        />
                      </Field>
                      <Field label="Audience Target">
                        <Select 
                          value={announcementForm.target}
                          onChange={(e) => setAnnouncementForm({...announcementForm, target: e.target.value})}
                        >
                          <option value="ALL">ALL (Everyone)</option>
                          <option value="STAFF">STAFF ONLY</option>
                          <option value="STUDENT">STUDENTS ONLY</option>
                        </Select>
                      </Field>
                    </div>

                    <Field label="Posted By / Sender Signature">
                      <Input 
                        value={announcementForm.sender}
                        onChange={(e) => setAnnouncementForm({...announcementForm, sender: e.target.value})}
                        placeholder="Principal's Office"
                      />
                    </Field>

                    <Field label="Bulletin Content Text *">
                      <textarea 
                        className="px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[120px] text-slate-800 leading-normal"
                        value={announcementForm.content}
                        onChange={(e) => setAnnouncementForm({...announcementForm, content: e.target.value})}
                        placeholder="Write announcement body details..."
                      />
                    </Field>

                    <div className="pt-2 border-t border-slate-100 flex justify-end">
                      <Button 
                        variant="primary" 
                        onClick={postAnnouncement} 
                        disabled={busyAnnouncement || !announcementForm.title || !announcementForm.content}
                      >
                        {busyAnnouncement ? <RefreshCw size={14} className="animate-spin" /> : <Send size={14} />}
                        Publish Bulletin
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            </div>
          </div>
        )}

        {/* ==================== NOTIFICATIONS TAB ==================== */}
        {subTab === 'notifications' && (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
            {/* Left side: Notifications Log Table */}
            <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">Direct Notification Dispatches</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {schoolId ? "Showing sent/unsent logs for current school." : "Select a school to view logs."}
                  </p>
                </div>
                <Badge color="blue">{notifications.length} Logs</Badge>
              </header>

              <div className="flex-1 overflow-x-auto">
                {loadingNotifications ? (
                  <Empty icon={RefreshCw} title="Loading notification logs..." hint="Please wait." />
                ) : notifications.length === 0 ? (
                  <Empty icon={Bell} title="No direct notifications logged" hint="Send a push/email/SMS message to a recipient on the right." />
                ) : (
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                        <th className="px-4 py-3">Recipient</th>
                        <th className="px-4 py-3">Channel</th>
                        <th className="px-4 py-3">Message</th>
                        <th className="px-4 py-3 text-center">Status</th>
                        <th className="px-4 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {notifications.slice().reverse().map((n) => (
                        <tr key={n.id} className="hover:bg-slate-50/50 transition">
                          {/* recipient */}
                          <td className="px-4 py-3 font-mono font-semibold text-slate-900 select-all">
                            {n.recipientId}
                          </td>
                          {/* channel */}
                          <td className="px-4 py-3">
                            <Badge color={getChannelColor(n.channel)}>{n.channel || 'PUSH'}</Badge>
                          </td>
                          {/* message */}
                          <td className="px-4 py-3 min-w-[240px] max-w-[320px]">
                            <p className="truncate text-slate-600 font-medium" title={n.message}>{n.message}</p>
                          </td>
                          {/* status */}
                          <td className="px-4 py-3 text-center">
                            <div className="flex items-center justify-center gap-1.5">
                              {n.sent ? (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                                  <Check size={10} /> Delivered
                                </span>
                              ) : (
                                <>
                                  <span className="text-[10px] font-bold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-full shrink-0">
                                    Pending
                                  </span>
                                  <button
                                    onClick={() => markNotificationSent(n)}
                                    className="text-emerald-600 hover:text-emerald-800 hover:bg-emerald-50 border border-emerald-200 rounded-md p-1 font-bold flex items-center transition"
                                    title="Mark sent / deliver now"
                                  >
                                    <Check size={11} />
                                  </button>
                                </>
                              )}
                            </div>
                          </td>
                          {/* delete */}
                          <td className="px-4 py-3 text-right">
                            <button
                              onClick={() => deleteNotification(n)}
                              className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-1.5 rounded-lg transition"
                              title="Delete notification record"
                            >
                              <Trash2 size={13} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            {/* Right side: Dispatch Notification Form */}
            <div className="xl:col-span-4">
              <Card 
                title="Send Notification" 
                subtitle="Dispatch direct communications to a specific user ID."
              >
                {!schoolId ? (
                  <Empty icon={Bell} title="Pick a school context" hint="Use the school selector in the top bar to enable dispatcher." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Recipient User ID *">
                      <Input 
                        value={notificationForm.recipientId}
                        onChange={(e) => setNotificationForm({...notificationForm, recipientId: e.target.value})}
                        placeholder="e.g. staff_id or parent_id"
                      />
                    </Field>

                    <Field label="Delivery Channel">
                      <Select 
                        value={notificationForm.channel}
                        onChange={(e) => setNotificationForm({...notificationForm, channel: e.target.value})}
                      >
                        <option value="PUSH">App Push Notification</option>
                        <option value="EMAIL">Email Address</option>
                        <option value="SMS">SMS Mobile Alert</option>
                        <option value="WHATSAPP">WhatsApp Message</option>
                      </Select>
                    </Field>

                    <Field label="Message Text *">
                      <textarea 
                        className="px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white min-h-[90px] text-slate-800 leading-normal"
                        value={notificationForm.message}
                        onChange={(e) => setNotificationForm({...notificationForm, message: e.target.value})}
                        placeholder="Type alert notification body..."
                      />
                    </Field>

                    <div className="flex items-center gap-2 pt-2">
                      <input 
                        type="checkbox" 
                        id="form_sent_check" 
                        checked={notificationForm.sent}
                        onChange={(e) => setNotificationForm({...notificationForm, sent: e.target.checked})}
                        className="h-4 w-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500" 
                      />
                      <label htmlFor="form_sent_check" className="text-xs font-semibold text-slate-600 select-none">Send Immediately (Delivered)</label>
                    </div>

                    <div className="pt-2 border-t border-slate-100 flex justify-end">
                      <Button 
                        variant="primary" 
                        onClick={dispatchNotification} 
                        disabled={busyNotification || !notificationForm.recipientId || !notificationForm.message}
                      >
                        {busyNotification ? <RefreshCw size={14} className="animate-spin" /> : <Send size={14} />}
                        Dispatch Alert
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
