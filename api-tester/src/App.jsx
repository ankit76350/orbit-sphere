import React, { useState, useEffect } from 'react'
import {
  Server,
  Activity,
  Layers,
  Calendar,
  Megaphone,
  Bell,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Edit2,
  CheckCircle,
  AlertTriangle,
  Check,
  X,
  PlusCircle,
  Info
} from 'lucide-react'

// Enums definitions
const SUBSCRIPTION_TIERS = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE']
const HOLIDAY_TYPES = [
  'WEEKLY_OFF',
  'PUBLIC_HOLIDAY',
  'FESTIVAL',
  'RELIGIOUS',
  'SCHOOL_EVENT',
  'VACATION',
  'EXAM_BREAK',
  'OTHER'
]
const NOTIFICATION_CHANNELS = ['PUSH', 'EMAIL', 'SMS', 'WHATSAPP']
const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
]

export default function App() {
  const [activeTab, setActiveTab] = useState('schools')
  const [backendStatus, setBackendStatus] = useState('checking') // 'online' | 'offline' | 'checking'

  // Data states
  const [schools, setSchools] = useState([])
  const [academicYears, setAcademicYears] = useState([])
  const [announcements, setAnnouncements] = useState([])
  const [notifications, setNotifications] = useState([])

  // School Form states
  const [schoolForm, setSchoolForm] = useState({
    id: '',
    schoolName: '',
    subdomain: '',
    logo: '',
    address: '',
    phone: '',
    email: '',
    subscriptionTier: 'FREE',
    maxStudents: 500,
    maxUsers: 50,
    active: true
  })
  const [schoolQueryId, setSchoolQueryId] = useState('')
  const [schoolQuerySubdomain, setSchoolQuerySubdomain] = useState('')
  const [schoolFilterActive, setSchoolFilterActive] = useState(false)

  // Academic Year Form states
  const [yearForm, setYearForm] = useState({
    id: '',
    schoolId: '',
    name: '',
    startDate: '',
    endDate: ''
  })
  const [yearQueryId, setYearQueryId] = useState('')
  const [yearQuerySchoolId, setYearQuerySchoolId] = useState('')
  const [yearQueryName, setYearQueryName] = useState('')
  const [yearQueryDate, setYearQueryDate] = useState('')
  
  // Holidays & Weekly-offs inside Academic Year
  const [selectedYearForHolidays, setSelectedYearForHolidays] = useState(null)
  const [holidayForm, setHolidayForm] = useState({
    name: '',
    description: '',
    type: 'PUBLIC_HOLIDAY',
    date: ''
  })
  const [weeklyOffForm, setWeeklyOffForm] = useState({
    dayOfWeek: 'SUNDAY',
    name: 'Weekly Off'
  })
  const [holidayRange, setHolidayRange] = useState({
    start: '',
    end: ''
  })
  const [holidayCheckDate, setHolidayCheckDate] = useState('')

  // Announcement Form states
  const [announcementForm, setAnnouncementForm] = useState({
    id: '',
    schoolId: '',
    title: '',
    content: '',
    target: 'All',
    date: new Date().toISOString().split('T')[0],
    sender: 'System Administrator'
  })
  const [announcementQueryId, setAnnouncementQueryId] = useState('')
  const [announcementQuerySchoolId, setAnnouncementQuerySchoolId] = useState('')
  const [announcementQueryTarget, setAnnouncementQueryTarget] = useState('')

  // Notification Form states
  const [notificationForm, setNotificationForm] = useState({
    id: '',
    schoolId: '',
    recipientId: '',
    channel: 'EMAIL',
    message: '',
    sent: false
  })
  const [notificationQueryId, setNotificationQueryId] = useState('')
  const [notificationQueryRecipientId, setNotificationQueryRecipientId] = useState('')
  const [notificationQuerySchoolId, setNotificationQuerySchoolId] = useState('')
  const [notificationFilterUnsent, setNotificationFilterUnsent] = useState(false)


  const callApi = async (url, method = 'GET', body = null) => {
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json'
      }
    }
    if (body) {
      options.body = JSON.stringify(body)
    }

    try {
      const res = await fetch(url, options)
      let data = null
      
      // Attempt to parse json
      const text = await res.text()
      if (text) {
        try {
          data = JSON.parse(text)
        } catch (e) {
          data = { message: text }
        }
      }

      if (!res.ok) {
        throw new Error(data?.message || `${res.status} ${res.statusText}`)
      }
      return data
    } catch (err) {
      console.error('API call error:', err)
      if (err.message.includes('Failed to fetch')) {
        setBackendStatus('offline')
      }
      throw err
    }
  }

  const checkBackendStatus = async () => {
    setBackendStatus('checking')
    try {
      await fetch('/api/schools')
      setBackendStatus('online')
    } catch (e) {
      setBackendStatus('offline')
    }
  }

  // Load Initial Data
  useEffect(() => {
    checkBackendStatus()
  }, [])

  // Auto trigger check backend when shifting tabs
  useEffect(() => {
    if (backendStatus === 'online') {
      if (activeTab === 'schools') fetchSchools()
      if (activeTab === 'years') fetchAcademicYears()
      if (activeTab === 'announcements') fetchAnnouncements()
      if (activeTab === 'notifications') fetchNotifications()
    }
  }, [activeTab, backendStatus])

  // --- SCHOOLS CONTROLLER API CALLS ---
  const fetchSchools = async () => {
    try {
      const url = schoolFilterActive ? '/api/schools/active' : '/api/schools'
      const data = await callApi(url)
      setSchools(data || [])
    } catch (err) {}
  }

  const handleCreateSchool = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...schoolForm }
      if (!payload.id) delete payload.id // backend generates ID if not provided
      await callApi('/api/schools', 'POST', payload)
      fetchSchools()
      // reset form
      setSchoolForm({
        id: '',
        schoolName: '',
        subdomain: '',
        logo: '',
        address: '',
        phone: '',
        email: '',
        subscriptionTier: 'FREE',
        maxStudents: 500,
        maxUsers: 50,
        active: true
      })
    } catch (err) {}
  }

  const handlePatchSchool = async (e) => {
    e.preventDefault()
    if (!schoolForm.id) return alert('Enter a School ID to update')
    try {
      const payload = { ...schoolForm }
      const schoolId = payload.id
      delete payload.id
      await callApi(`/api/schools/${schoolId}`, 'PATCH', payload)
      fetchSchools()
    } catch (err) {}
  }

  const handleDeleteSchool = async (id) => {
    if (!window.confirm(`Delete school with ID: ${id}?`)) return
    try {
      await callApi(`/api/schools/${id}`, 'DELETE')
      fetchSchools()
    } catch (err) {}
  }

  const handleQuerySchoolId = async () => {
    if (!schoolQueryId) return
    try {
      const data = await callApi(`/api/schools/${schoolQueryId}`)
      if (data) setSchools([data])
    } catch (err) {}
  }

  const handleQuerySchoolSubdomain = async () => {
    if (!schoolQuerySubdomain) return
    try {
      const data = await callApi(`/api/schools/subdomain/${schoolQuerySubdomain}`)
      if (data) setSchools([data])
    } catch (err) {}
  }

  // --- ACADEMIC YEARS API CALLS ---
  const fetchAcademicYears = async () => {
    try {
      const data = await callApi('/api/academic-years')
      setAcademicYears(data || [])
    } catch (err) {}
  }

  const handleCreateYear = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...yearForm }
      if (!payload.id) delete payload.id
      await callApi('/api/academic-years', 'POST', payload)
      fetchAcademicYears()
      setYearForm({ id: '', schoolId: '', name: '', startDate: '', endDate: '' })
    } catch (err) {}
  }

  const handleUpdateYear = async (e) => {
    e.preventDefault()
    if (!yearForm.id) return alert('Enter Academic Year ID to update')
    try {
      const payload = { ...yearForm }
      const id = payload.id
      delete payload.id
      await callApi(`/api/academic-years/${id}`, 'PUT', payload)
      fetchAcademicYears()
    } catch (err) {}
  }

  const handleDeleteYear = async (id) => {
    if (!window.confirm(`Delete Academic Year with ID: ${id}?`)) return
    try {
      await callApi(`/api/academic-years/${id}`, 'DELETE')
      fetchAcademicYears()
      if (selectedYearForHolidays?.id === id) {
        setSelectedYearForHolidays(null)
      }
    } catch (err) {}
  }

  const handleQueryYearId = async () => {
    if (!yearQueryId) return
    try {
      const data = await callApi(`/api/academic-years/${yearQueryId}`)
      if (data) setAcademicYears([data])
    } catch (err) {}
  }

  const handleQueryYearSchoolId = async () => {
    if (!yearQuerySchoolId) return
    try {
      const data = await callApi(`/api/academic-years/school/${yearQuerySchoolId}`)
      setAcademicYears(data || [])
    } catch (err) {}
  }

  const handleQueryYearSchoolAndName = async () => {
    if (!yearQuerySchoolId || !yearQueryName) return alert('School ID and Name both required')
    try {
      const data = await callApi(`/api/academic-years/school/${yearQuerySchoolId}/name/${yearQueryName}`)
      if (data) setAcademicYears([data])
    } catch (err) {}
  }

  const handleQueryYearForDate = async () => {
    if (!yearQuerySchoolId || !yearQueryDate) return alert('School ID and Date both required')
    try {
      const data = await callApi(`/api/academic-years/school/${yearQuerySchoolId}/for-date/${yearQueryDate}`)
      if (data) setAcademicYears([data])
    } catch (err) {}
  }

  // --- HOLIDAYS & WEEKLY OFFS CALLS ---
  const handleAddHoliday = async (e) => {
    e.preventDefault()
    if (!selectedYearForHolidays) return alert('Select an academic year first')
    try {
      const res = await callApi(`/api/academic-years/${selectedYearForHolidays.id}/holidays`, 'POST', [holidayForm])
      setSelectedYearForHolidays(res)
      setHolidayForm({ name: '', description: '', type: 'PUBLIC_HOLIDAY', date: '' })
      fetchAcademicYears()
    } catch (err) {}
  }

  const handleAddWeeklyOff = async (e) => {
    e.preventDefault()
    if (!selectedYearForHolidays) return alert('Select an academic year first')
    try {
      const res = await callApi(
        `/api/academic-years/${selectedYearForHolidays.id}/weekly-offs?dayOfWeek=${weeklyOffForm.dayOfWeek}&name=${encodeURIComponent(weeklyOffForm.name)}`,
        'POST'
      )
      setSelectedYearForHolidays(res)
      fetchAcademicYears()
    } catch (err) {}
  }

  const handleDeleteHoliday = async (date, name) => {
    if (!selectedYearForHolidays) return
    if (!window.confirm(`Delete holiday on ${date} (${name})?`)) return
    try {
      const url = `/api/academic-years/${selectedYearForHolidays.id}/holidays?date=${date}${name ? `&name=${encodeURIComponent(name)}` : ''}`
      const res = await callApi(url, 'DELETE')
      setSelectedYearForHolidays(res)
      fetchAcademicYears()
    } catch (err) {}
  }

  const handleDeleteWeeklyOff = async (day) => {
    if (!selectedYearForHolidays) return
    if (!window.confirm(`Delete all weekly-offs for: ${day}?`)) return
    try {
      const res = await callApi(`/api/academic-years/${selectedYearForHolidays.id}/weekly-offs?dayOfWeek=${day}`, 'DELETE')
      setSelectedYearForHolidays(res)
      fetchAcademicYears()
    } catch (err) {}
  }

  const handleQueryHolidaysRange = async () => {
    if (!selectedYearForHolidays || !holidayRange.start || !holidayRange.end) return alert('Select year and dates')
    try {
      await callApi(`/api/academic-years/${selectedYearForHolidays.id}/holidays/range?start=${holidayRange.start}&end=${holidayRange.end}`)
    } catch (err) {}
  }

  const handleCheckHolidayDate = async () => {
    if (!selectedYearForHolidays || !holidayCheckDate) return alert('Select year and date')
    try {
      await callApi(`/api/academic-years/${selectedYearForHolidays.id}/holidays/check?date=${holidayCheckDate}`)
    } catch (err) {}
  }

  // --- ANNOUNCEMENTS CONTROLLER API CALLS ---
  const fetchAnnouncements = async () => {
    try {
      const data = await callApi('/api/announcements')
      setAnnouncements(data || [])
    } catch (err) {}
  }

  const handleCreateAnnouncement = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...announcementForm }
      if (!payload.id) delete payload.id
      await callApi('/api/announcements', 'POST', payload)
      fetchAnnouncements()
      setAnnouncementForm({
        id: '',
        schoolId: '',
        title: '',
        content: '',
        target: 'All',
        date: new Date().toISOString().split('T')[0],
        sender: 'System Administrator'
      })
    } catch (err) {}
  }

  const handleUpdateAnnouncement = async (e) => {
    e.preventDefault()
    if (!announcementForm.id) return alert('Enter Announcement ID to update')
    try {
      const payload = { ...announcementForm }
      const id = payload.id
      delete payload.id
      await callApi(`/api/announcements/${id}`, 'PUT', payload)
      fetchAnnouncements()
    } catch (err) {}
  }

  const handleDeleteAnnouncement = async (id) => {
    if (!window.confirm(`Delete announcement ID: ${id}?`)) return
    try {
      await callApi(`/api/announcements/${id}`, 'DELETE')
      fetchAnnouncements()
    } catch (err) {}
  }

  const handleQueryAnnouncementId = async () => {
    if (!announcementQueryId) return
    try {
      const data = await callApi(`/api/announcements/${announcementQueryId}`)
      if (data) setAnnouncements([data])
    } catch (err) {}
  }

  const handleQueryAnnouncementSchoolId = async () => {
    if (!announcementQuerySchoolId) return
    try {
      let url = `/api/announcements/school/${announcementQuerySchoolId}`
      if (announcementQueryTarget) {
        url += `/target/${announcementQueryTarget}`
      }
      const data = await callApi(url)
      setAnnouncements(data || [])
    } catch (err) {}
  }

  // --- NOTIFICATIONS CONTROLLER API CALLS ---
  const fetchNotifications = async () => {
    try {
      const data = await callApi('/api/notifications')
      setNotifications(data || [])
    } catch (err) {}
  }

  const handleCreateNotification = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...notificationForm }
      if (!payload.id) delete payload.id
      await callApi('/api/notifications', 'POST', payload)
      fetchNotifications()
      setNotificationForm({
        id: '',
        schoolId: '',
        recipientId: '',
        channel: 'EMAIL',
        message: '',
        sent: false
      })
    } catch (err) {}
  }

  const handleMarkNotificationSent = async (id) => {
    try {
      await callApi(`/api/notifications/${id}/mark-sent`, 'PUT')
      fetchNotifications()
    } catch (err) {}
  }

  const handleDeleteNotification = async (id) => {
    if (!window.confirm(`Delete notification ID: ${id}?`)) return
    try {
      await callApi(`/api/notifications/${id}`, 'DELETE')
      fetchNotifications()
    } catch (err) {}
  }

  const handleQueryNotificationId = async () => {
    if (!notificationQueryId) return
    try {
      const data = await callApi(`/api/notifications/${notificationQueryId}`)
      if (data) setNotifications([data])
    } catch (err) {}
  }

  const handleQueryNotificationSchoolId = async () => {
    if (!notificationQuerySchoolId) return
    try {
      const data = await callApi(`/api/notifications/school/${notificationQuerySchoolId}`)
      setNotifications(data || [])
    } catch (err) {}
  }

  const handleQueryNotificationRecipient = async () => {
    if (!notificationQueryRecipientId) return
    try {
      const url = notificationFilterUnsent
        ? `/api/notifications/recipient/${notificationQueryRecipientId}/unsent`
        : `/api/notifications/recipient/${notificationQueryRecipientId}`
      const data = await callApi(url)
      setNotifications(data || [])
    } catch (err) {}
  }

  return (
    <div className="app-container">
      {/* Header */}
      <header className="header">
        <div className="brand-section">
          <span className="logo-badge">API</span>
          <h1 className="app-title">Timetable & Core API Tester Console</h1>
        </div>

        <div className="button-group">
          <button className="btn btn-secondary btn-sm" onClick={checkBackendStatus}>
            <RefreshCw size={14} className={backendStatus === 'checking' ? 'animate-spin' : ''} />
            Check Connection
          </button>
          
          <div className="health-status">
            <span className={`status-dot ${backendStatus}`}></span>
            {backendStatus === 'online' && <span className="text-success font-bold">Backend Online (Port 5030)</span>}
            {backendStatus === 'offline' && <span className="text-danger font-bold">Backend Offline</span>}
            {backendStatus === 'checking' && <span className="text-warning font-bold">Checking...</span>}
          </div>
        </div>
      </header>

      {/* Main Content Pane */}
      <div className="main-content">
        
        {/* Sidebar Nav */}
        <aside className="sidebar">
          <p className="sidebar-title">Controller Endpoints</p>
          
          <button
            className={`sidebar-btn ${activeTab === 'schools' ? 'active' : ''}`}
            onClick={() => setActiveTab('schools')}
          >
            <Layers size={16} />
            Schools API
          </button>

          <button
            className={`sidebar-btn ${activeTab === 'years' ? 'active' : ''}`}
            onClick={() => setActiveTab('years')}
          >
            <Calendar size={16} />
            Academic Years API
          </button>

          <button
            className={`sidebar-btn ${activeTab === 'announcements' ? 'active' : ''}`}
            onClick={() => setActiveTab('announcements')}
          >
            <Megaphone size={16} />
            Announcements API
          </button>

          <button
            className={`sidebar-btn ${activeTab === 'notifications' ? 'active' : ''}`}
            onClick={() => setActiveTab('notifications')}
          >
            <Bell size={16} />
            Notifications API
          </button>
        </aside>

        {/* Workspace Panels */}
        <div className="workspace">
          
          {/* TAB 1: SCHOOLS */}
          {activeTab === 'schools' && (
            <div className="tab-content-area animate-fade-in">
              
              {/* Form Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Plus size={16} className="text-indigo-400" />
                    School Data Entry (POST / PATCH)
                  </span>
                </div>
                <div className="card-body">
                  <form onSubmit={handleCreateSchool} className="flex flex-col gap-4">
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">School ID (Optional for Post)</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. school-01"
                          value={schoolForm.id}
                          onChange={(e) => setSchoolForm({ ...schoolForm, id: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">School Name</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="St. Jude's Academy"
                          value={schoolForm.schoolName}
                          onChange={(e) => setSchoolForm({ ...schoolForm, schoolName: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Subdomain</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="stjude"
                          value={schoolForm.subdomain}
                          onChange={(e) => setSchoolForm({ ...schoolForm, subdomain: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Logo Link</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="https://image-url"
                          value={schoolForm.logo}
                          onChange={(e) => setSchoolForm({ ...schoolForm, logo: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Email</label>
                        <input
                          type="email"
                          className="form-control"
                          placeholder="admin@school.com"
                          value={schoolForm.email}
                          onChange={(e) => setSchoolForm({ ...schoolForm, email: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Phone</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="+1 234 567"
                          value={schoolForm.phone}
                          onChange={(e) => setSchoolForm({ ...schoolForm, phone: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Address</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="123 Education Lane"
                        value={schoolForm.address}
                        onChange={(e) => setSchoolForm({ ...schoolForm, address: e.target.value })}
                      />
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Subscription Tier</label>
                        <select
                          className="form-control"
                          value={schoolForm.subscriptionTier}
                          onChange={(e) => setSchoolForm({ ...schoolForm, subscriptionTier: e.target.value })}
                        >
                          {SUBSCRIPTION_TIERS.map((tier) => (
                            <option key={tier} value={tier}>{tier}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Max Students</label>
                        <input
                          type="number"
                          className="form-control"
                          value={schoolForm.maxStudents}
                          onChange={(e) => setSchoolForm({ ...schoolForm, maxStudents: parseInt(e.target.value) || 0 })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Max Users</label>
                        <input
                          type="number"
                          className="form-control"
                          value={schoolForm.maxUsers}
                          onChange={(e) => setSchoolForm({ ...schoolForm, maxUsers: parseInt(e.target.value) || 0 })}
                        />
                      </div>
                      <div className="form-group" style={{ justifyContent: 'center' }}>
                        <label className="checkbox-label">
                          <input
                            type="checkbox"
                            className="checkbox-input"
                            checked={schoolForm.active}
                            onChange={(e) => setSchoolForm({ ...schoolForm, active: e.target.checked })}
                          />
                          Active School Status
                        </label>
                      </div>
                    </div>

                    <div className="button-group">
                      <button type="submit" className="btn btn-primary">
                        <Plus size={14} /> Create (POST)
                      </button>
                      <button type="button" className="btn btn-success" onClick={handlePatchSchool}>
                        <Edit2 size={14} /> Update (PATCH by ID)
                      </button>
                    </div>
                  </form>

                  <div className="border-t border-gray-700 my-2 pt-4">
                    <p className="form-label mb-2">Search / Query Actions</p>
                    <div className="flex flex-col gap-3">
                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Search School ID"
                          value={schoolQueryId}
                          onChange={(e) => setSchoolQueryId(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQuerySchoolId}>
                          <Search size={14} /> ID Lookup
                        </button>
                      </div>

                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Search Subdomain"
                          value={schoolQuerySubdomain}
                          onChange={(e) => setSchoolQuerySubdomain(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQuerySchoolSubdomain}>
                          <Search size={14} /> Subdomain Lookup
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* List View Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Activity size={16} className="text-emerald-400" />
                    Schools Ledger ({schools.length})
                  </span>
                  <div className="flex items-center gap-3">
                    <label className="checkbox-label text-xs">
                      <input
                        type="checkbox"
                        className="checkbox-input"
                        checked={schoolFilterActive}
                        onChange={(e) => {
                          setSchoolFilterActive(e.target.checked)
                          fetchSchools()
                        }}
                      />
                      Active Only
                    </label>
                    <button className="btn btn-secondary btn-sm" onClick={fetchSchools}>
                      <RefreshCw size={12} />
                    </button>
                  </div>
                </div>
                <div className="card-body">
                  {schools.length === 0 ? (
                    <div className="empty-placeholder">No schools retrieved from backend. Create one or refresh.</div>
                  ) : (
                    <div className="table-container">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>ID</th>
                            <th>School Name</th>
                            <th>Subdomain</th>
                            <th>Tier</th>
                            <th>Active</th>
                            <th>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {schools.map((sch) => (
                            <tr key={sch.id}>
                              <td className="font-mono text-indigo-300">{sch.id}</td>
                              <td>{sch.schoolName}</td>
                              <td>{sch.subdomain}</td>
                              <td>
                                <span className={`badge badge-primary`}>{sch.subscriptionTier}</span>
                              </td>
                              <td>
                                <span className={`badge ${sch.active ? 'badge-success' : 'badge-danger'}`}>
                                  {sch.active ? 'Active' : 'Inactive'}
                                </span>
                              </td>
                              <td>
                                <div className="action-cell">
                                  <button
                                    className="btn btn-secondary btn-sm"
                                    onClick={() => {
                                      setSchoolForm({
                                        id: sch.id || '',
                                        schoolName: sch.schoolName || '',
                                        subdomain: sch.subdomain || '',
                                        logo: sch.logo || '',
                                        address: sch.address || '',
                                        phone: sch.phone || '',
                                        email: sch.email || '',
                                        subscriptionTier: sch.subscriptionTier || 'FREE',
                                        maxStudents: sch.maxStudents || 500,
                                        maxUsers: sch.maxUsers || 50,
                                        active: sch.active !== false
                                      })
                                    }}
                                  >
                                    Edit
                                  </button>
                                  <button
                                    className="btn btn-danger btn-sm"
                                    onClick={() => handleDeleteSchool(sch.id)}
                                  >
                                    <Trash2 size={12} />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: ACADEMIC YEARS */}
          {activeTab === 'years' && (
            <div className="tab-content-area animate-fade-in">
              {/* Form Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Plus size={16} className="text-indigo-400" />
                    Academic Year Setup (POST / PUT)
                  </span>
                </div>
                <div className="card-body">
                  <form onSubmit={handleCreateYear} className="flex flex-col gap-4">
                    <div className="form-group">
                      <label className="form-label">Academic Year ID (Optional for Post)</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. acadyear-id"
                        value={yearForm.id}
                        onChange={(e) => setYearForm({ ...yearForm, id: e.target.value })}
                      />
                    </div>
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">School ID</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="school-01"
                          value={yearForm.schoolId}
                          onChange={(e) => setYearForm({ ...yearForm, schoolId: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Name (e.g. 2026-2027)</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="2026-2027"
                          value={yearForm.name}
                          onChange={(e) => setYearForm({ ...yearForm, name: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Start Date</label>
                        <input
                          type="date"
                          className="form-control"
                          required
                          value={yearForm.startDate}
                          onChange={(e) => setYearForm({ ...yearForm, startDate: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">End Date</label>
                        <input
                          type="date"
                          className="form-control"
                          required
                          value={yearForm.endDate}
                          onChange={(e) => setYearForm({ ...yearForm, endDate: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="button-group">
                      <button type="submit" className="btn btn-primary">
                        <Plus size={14} /> Create (POST)
                      </button>
                      <button type="button" className="btn btn-success" onClick={handleUpdateYear}>
                        <Edit2 size={14} /> Update (PUT by ID)
                      </button>
                    </div>
                  </form>

                  <div className="border-t border-gray-700 my-2 pt-4">
                    <p className="form-label mb-2">Search & Lookup Endpoints</p>
                    <div className="flex flex-col gap-3">
                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Search Academic Year ID"
                          value={yearQueryId}
                          onChange={(e) => setYearQueryId(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQueryYearId}>
                          <Search size={14} /> ID Lookup
                        </button>
                      </div>

                      <div className="form-row">
                        <div className="form-group">
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Filter by School ID"
                            value={yearQuerySchoolId}
                            onChange={(e) => setYearQuerySchoolId(e.target.value)}
                          />
                        </div>
                        <div className="form-group">
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Year Name (e.g. 2026)"
                            value={yearQueryName}
                            onChange={(e) => setYearQueryName(e.target.value)}
                          />
                        </div>
                      </div>
                      
                      <div className="button-group">
                        <button className="btn btn-secondary btn-sm" onClick={handleQueryYearSchoolId}>
                          School Query
                        </button>
                        <button className="btn btn-secondary btn-sm" onClick={handleQueryYearSchoolAndName}>
                          School & Name Query
                        </button>
                      </div>

                      <div className="quick-search-box border-t border-gray-800 pt-3">
                        <input
                          type="date"
                          className="form-control"
                          value={yearQueryDate}
                          onChange={(e) => setYearQueryDate(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQueryYearForDate}>
                          Academic Year for Date
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* List View and Holidays Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Calendar size={16} className="text-indigo-400" />
                    Academic Years ({academicYears.length})
                  </span>
                  <button className="btn btn-secondary btn-sm" onClick={fetchAcademicYears}>
                    <RefreshCw size={12} />
                  </button>
                </div>
                <div className="card-body">
                  <div className="table-container mb-4">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>School ID</th>
                          <th>Name</th>
                          <th>Start Date</th>
                          <th>End Date</th>
                          <th>Holidays</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {academicYears.map((yr) => (
                          <tr
                            key={yr.id}
                            className={selectedYearForHolidays?.id === yr.id ? 'bg-indigo-950/40 border-l-4 border-indigo-500' : ''}
                          >
                            <td className="font-mono text-indigo-300">{yr.id}</td>
                            <td>{yr.schoolId}</td>
                            <td className="font-bold">{yr.name}</td>
                            <td>{yr.startDate}</td>
                            <td>{yr.endDate}</td>
                            <td>
                              <span className="badge badge-primary">
                                {(yr.holidays || []).length} Entries
                              </span>
                            </td>
                            <td>
                              <div className="action-cell">
                                <button
                                  className="btn btn-secondary btn-sm"
                                  onClick={() => {
                                    setSelectedYearForHolidays(yr)
                                    setYearForm({
                                      id: yr.id || '',
                                      schoolId: yr.schoolId || '',
                                      name: yr.name || '',
                                      startDate: yr.startDate || '',
                                      endDate: yr.endDate || ''
                                    })
                                  }}
                                >
                                  Manage
                                </button>
                                <button
                                  className="btn btn-danger btn-sm"
                                  onClick={() => handleDeleteYear(yr.id)}
                                >
                                  <Trash2 size={12} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Holiday management sub-panel */}
                  {selectedYearForHolidays ? (
                    <div className="border-t border-gray-700 pt-4 mt-2">
                      <div className="flex items-center justify-between mb-3">
                        <p className="text-sm font-bold text-indigo-300">
                          🏖️ Holidays / Calendar: {selectedYearForHolidays.name}
                        </p>
                        <button
                          className="text-gray-400 hover:text-white"
                          onClick={() => setSelectedYearForHolidays(null)}
                        >
                          <X size={16} />
                        </button>
                      </div>

                      {/* Add Holiday Form */}
                      <form onSubmit={handleAddHoliday} className="bg-gray-900/40 p-3 rounded-lg border border-gray-700 flex flex-col gap-3 mb-3">
                        <div className="text-[11px] font-bold text-indigo-400 uppercase">Add dated holiday</div>
                        <div className="form-row">
                          <div className="form-group">
                            <label className="form-label text-[10px]">Name</label>
                            <input
                              type="text"
                              className="form-control form-control-sm"
                              required
                              placeholder="New Year's Day"
                              value={holidayForm.name}
                              onChange={(e) => setHolidayForm({ ...holidayForm, name: e.target.value })}
                            />
                          </div>
                          <div className="form-group">
                            <label className="form-label text-[10px]">Holiday Type</label>
                            <select
                              className="form-control form-control-sm"
                              value={holidayForm.type}
                              onChange={(e) => setHolidayForm({ ...holidayForm, type: e.target.value })}
                            >
                              {HOLIDAY_TYPES.map((type) => (
                                <option key={type} value={type}>{type}</option>
                              ))}
                            </select>
                          </div>
                        </div>

                        <div className="form-row">
                          <div className="form-group">
                            <label className="form-label text-[10px]">Date</label>
                            <input
                              type="date"
                              className="form-control form-control-sm"
                              required
                              value={holidayForm.date}
                              onChange={(e) => setHolidayForm({ ...holidayForm, date: e.target.value })}
                            />
                          </div>
                          <div className="form-group">
                            <label className="form-label text-[10px]">Description</label>
                            <input
                              type="text"
                              className="form-control form-control-sm"
                              placeholder="Optional description"
                              value={holidayForm.description}
                              onChange={(e) => setHolidayForm({ ...holidayForm, description: e.target.value })}
                            />
                          </div>
                        </div>

                        <button type="submit" className="btn btn-primary btn-sm align-self-start">
                          <PlusCircle size={12} /> Add Holiday
                        </button>
                      </form>

                      {/* Add Weekly Off Series Form */}
                      <form onSubmit={handleAddWeeklyOff} className="bg-gray-900/40 p-3 rounded-lg border border-gray-700 flex flex-col gap-3 mb-3">
                        <div className="text-[11px] font-bold text-indigo-400 uppercase">Generate Weekly-Off Series</div>
                        <div className="form-row">
                          <div className="form-group">
                            <label className="form-label text-[10px]">Day of Week</label>
                            <select
                              className="form-control form-control-sm"
                              value={weeklyOffForm.dayOfWeek}
                              onChange={(e) => setWeeklyOffForm({ ...weeklyOffForm, dayOfWeek: e.target.value })}
                            >
                              {DAYS_OF_WEEK.map((day) => (
                                <option key={day} value={day}>{day}</option>
                              ))}
                            </select>
                          </div>
                          <div className="form-group">
                            <label className="form-label text-[10px]">Holiday Label</label>
                            <input
                              type="text"
                              className="form-control form-control-sm"
                              required
                              value={weeklyOffForm.name}
                              onChange={(e) => setWeeklyOffForm({ ...weeklyOffForm, name: e.target.value })}
                            />
                          </div>
                        </div>
                        <div className="button-group">
                          <button type="submit" className="btn btn-success btn-sm">
                            Generate Series (~52 occurrences)
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDeleteWeeklyOff(weeklyOffForm.dayOfWeek)}
                          >
                            Remove Weekly-Off Weekday
                          </button>
                        </div>
                      </form>

                      {/* Query Ranges */}
                      <div className="grid grid-cols-2 gap-3 mb-4">
                        <div className="bg-gray-900/20 p-2.5 rounded border border-gray-800">
                          <div className="text-[10px] font-bold text-indigo-300 uppercase mb-2">Holidays in Range</div>
                          <div className="flex flex-col gap-2">
                            <input
                              type="date"
                              className="form-control form-control-sm"
                              value={holidayRange.start}
                              onChange={(e) => setHolidayRange({ ...holidayRange, start: e.target.value })}
                            />
                            <input
                              type="date"
                              className="form-control form-control-sm"
                              value={holidayRange.end}
                              onChange={(e) => setHolidayRange({ ...holidayRange, end: e.target.value })}
                            />
                            <button className="btn btn-secondary btn-sm" type="button" onClick={handleQueryHolidaysRange}>
                              Find Holidays
                            </button>
                          </div>
                        </div>

                        <div className="bg-gray-900/20 p-2.5 rounded border border-gray-800">
                          <div className="text-[10px] font-bold text-indigo-300 uppercase mb-2">Check Holiday Date</div>
                          <div className="flex flex-col gap-2">
                            <input
                              type="date"
                              className="form-control form-control-sm"
                              value={holidayCheckDate}
                              onChange={(e) => setHolidayCheckDate(e.target.value)}
                            />
                            <button className="btn btn-secondary btn-sm" type="button" onClick={handleCheckHolidayDate}>
                              Is Holiday Check
                            </button>
                          </div>
                        </div>
                      </div>

                      {/* Display existing holidays */}
                      <div className="text-xs font-bold mb-2">Existing Holiday Detail Roster:</div>
                      {(!selectedYearForHolidays.holidays || selectedYearForHolidays.holidays.length === 0) ? (
                        <div className="empty-placeholder">No holiday entries generated yet.</div>
                      ) : (
                        <div className="holiday-pill-container max-h-48 overflow-y-auto bg-gray-950/60 p-2 rounded border border-gray-800">
                          {selectedYearForHolidays.holidays.map((h, index) => (
                            <div key={index} className="holiday-pill">
                              <span className="font-mono text-indigo-300 font-bold">{h.date}</span>
                              <span className="text-white">{h.name}</span>
                              <span className="badge badge-secondary text-[9px]">{h.type}</span>
                              <span className="close-btn" onClick={() => handleDeleteHoliday(h.date, h.name)}>✕</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="empty-placeholder mt-2">
                      <Info size={16} style={{ display: 'inline', marginRight: '6px' }} />
                      Click "Manage" on any academic year to setup and test holidays / weekly-offs calendar endpoints.
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: ANNOUNCEMENTS */}
          {activeTab === 'announcements' && (
            <div className="tab-content-area animate-fade-in">
              {/* Form Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Plus size={16} className="text-indigo-400" />
                    New Announcement Board Post (POST / PUT)
                  </span>
                </div>
                <div className="card-body">
                  <form onSubmit={handleCreateAnnouncement} className="flex flex-col gap-4">
                    <div className="form-group">
                      <label className="form-label">Announcement ID (Optional for Post)</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. ann-id"
                        value={announcementForm.id}
                        onChange={(e) => setAnnouncementForm({ ...announcementForm, id: e.target.value })}
                      />
                    </div>
                    
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">School ID</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="school-01"
                          value={announcementForm.schoolId}
                          onChange={(e) => setAnnouncementForm({ ...announcementForm, schoolId: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Sender Name / Role</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="Office Principal"
                          value={announcementForm.sender}
                          onChange={(e) => setAnnouncementForm({ ...announcementForm, sender: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Title</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="Annual Day Announcement"
                          value={announcementForm.title}
                          onChange={(e) => setAnnouncementForm({ ...announcementForm, title: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Target Audience / Tag</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="All, Staff, Students"
                          value={announcementForm.target}
                          onChange={(e) => setAnnouncementForm({ ...announcementForm, target: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Content Body</label>
                      <textarea
                        rows={3}
                        className="form-control"
                        required
                        placeholder="Content text for the bulletin board..."
                        value={announcementForm.content}
                        onChange={(e) => setAnnouncementForm({ ...announcementForm, content: e.target.value })}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">Date String</label>
                      <input
                        type="date"
                        className="form-control"
                        value={announcementForm.date}
                        onChange={(e) => setAnnouncementForm({ ...announcementForm, date: e.target.value })}
                      />
                    </div>

                    <div className="button-group">
                      <button type="submit" className="btn btn-primary">
                        <Plus size={14} /> Create Announcement
                      </button>
                      <button type="button" className="btn btn-success" onClick={handleUpdateAnnouncement}>
                        <Edit2 size={14} /> Update Announcement
                      </button>
                    </div>
                  </form>

                  <div className="border-t border-gray-700 my-2 pt-4">
                    <p className="form-label mb-2">Lookup & Filters</p>
                    <div className="flex flex-col gap-3">
                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Query Announcement ID"
                          value={announcementQueryId}
                          onChange={(e) => setAnnouncementQueryId(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQueryAnnouncementId}>
                          Find ID
                        </button>
                      </div>

                      <div className="form-row">
                        <div className="form-group">
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Filter by School ID"
                            value={announcementQuerySchoolId}
                            onChange={(e) => setAnnouncementQuerySchoolId(e.target.value)}
                          />
                        </div>
                        <div className="form-group">
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Target (e.g. Staff)"
                            value={announcementQueryTarget}
                            onChange={(e) => setAnnouncementQueryTarget(e.target.value)}
                          />
                        </div>
                      </div>
                      <button className="btn btn-secondary btn-sm" type="button" onClick={handleQueryAnnouncementSchoolId}>
                        Query School Announcements
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* List View Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Megaphone size={16} className="text-indigo-400" />
                    Bulletin Announcements ({announcements.length})
                  </span>
                  <button className="btn btn-secondary btn-sm" onClick={fetchAnnouncements}>
                    <RefreshCw size={12} />
                  </button>
                </div>
                <div className="card-body">
                  {announcements.length === 0 ? (
                    <div className="empty-placeholder">No announcements posted on the board.</div>
                  ) : (
                    <div className="flex flex-col gap-4">
                      {announcements.map((ann) => (
                        <div key={ann.id} className="bg-gray-900/30 p-4 rounded-xl border border-gray-700 flex flex-col gap-2 relative">
                          <div className="flex justify-between items-start">
                            <div>
                              <span className="badge badge-success text-[10px] mr-2">{ann.target || 'All'}</span>
                              <span className="text-[10px] text-gray-500 font-mono">School: {ann.schoolId}</span>
                            </div>
                            <div className="flex gap-2">
                              <button
                                className="btn btn-secondary btn-sm px-2 py-1 text-[10px]"
                                onClick={() => {
                                  setAnnouncementForm({
                                    id: ann.id || '',
                                    schoolId: ann.schoolId || '',
                                    title: ann.title || '',
                                    content: ann.content || '',
                                    target: ann.target || 'All',
                                    date: ann.date || '',
                                    sender: ann.sender || ''
                                  })
                                }}
                              >
                                Edit
                              </button>
                              <button
                                className="btn btn-danger btn-sm px-2 py-1"
                                onClick={() => handleDeleteAnnouncement(ann.id)}
                              >
                                <Trash2 size={10} />
                              </button>
                            </div>
                          </div>
                          
                          <h4 className="text-sm font-bold text-white mt-1">{ann.title}</h4>
                          <p className="text-xs text-gray-300 whitespace-pre-wrap">{ann.content}</p>
                          
                          <div className="flex justify-between text-[10px] text-gray-500 font-bold border-t border-gray-800/80 pt-2 mt-1">
                            <span>Posted by: {ann.sender}</span>
                            <span>Date: {ann.date}</span>
                          </div>
                          <span className="absolute bottom-2 right-4 text-[9px] font-mono text-gray-600">ID: {ann.id}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: NOTIFICATIONS */}
          {activeTab === 'notifications' && (
            <div className="tab-content-area animate-fade-in">
              {/* Form Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Plus size={16} className="text-indigo-400" />
                    Dispatch Notification Queue (POST)
                  </span>
                </div>
                <div className="card-body">
                  <form onSubmit={handleCreateNotification} className="flex flex-col gap-4">
                    <div className="form-group">
                      <label className="form-label">Notification ID (Optional for Post)</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. notif-id"
                        value={notificationForm.id}
                        onChange={(e) => setNotificationForm({ ...notificationForm, id: e.target.value })}
                      />
                    </div>
                    
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">School ID</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="school-01"
                          value={notificationForm.schoolId}
                          onChange={(e) => setNotificationForm({ ...notificationForm, schoolId: e.target.value })}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Recipient ID</label>
                        <input
                          type="text"
                          className="form-control"
                          required
                          placeholder="recipient-user-01"
                          value={notificationForm.recipientId}
                          onChange={(e) => setNotificationForm({ ...notificationForm, recipientId: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Notification Channel</label>
                        <select
                          className="form-control"
                          value={notificationForm.channel}
                          onChange={(e) => setNotificationForm({ ...notificationForm, channel: e.target.value })}
                        >
                          {NOTIFICATION_CHANNELS.map((ch) => (
                            <option key={ch} value={ch}>{ch}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group" style={{ justifyContent: 'center' }}>
                        <label className="checkbox-label">
                          <input
                            type="checkbox"
                            className="checkbox-input"
                            checked={notificationForm.sent}
                            onChange={(e) => setNotificationForm({ ...notificationForm, sent: e.target.checked })}
                          />
                          Mark Sent Instantly
                        </label>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Message Payload</label>
                      <textarea
                        rows={3}
                        className="form-control"
                        required
                        placeholder="Alert message details..."
                        value={notificationForm.message}
                        onChange={(e) => setNotificationForm({ ...notificationForm, message: e.target.value })}
                      />
                    </div>

                    <button type="submit" className="btn btn-primary">
                      <Plus size={14} /> Dispatch/Queue Notification
                    </button>
                  </form>

                  <div className="border-t border-gray-700 my-2 pt-4">
                    <p className="form-label mb-2">Search & Filters</p>
                    <div className="flex flex-col gap-3">
                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Lookup Notification ID"
                          value={notificationQueryId}
                          onChange={(e) => setNotificationQueryId(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQueryNotificationId}>
                          Find ID
                        </button>
                      </div>

                      <div className="quick-search-box">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Filter School ID"
                          value={notificationQuerySchoolId}
                          onChange={(e) => setNotificationQuerySchoolId(e.target.value)}
                        />
                        <button className="btn btn-secondary btn-sm text-nowrap" onClick={handleQueryNotificationSchoolId}>
                          School Query
                        </button>
                      </div>

                      <div className="quick-search-box border-t border-gray-800 pt-3">
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Filter Recipient ID"
                          value={notificationQueryRecipientId}
                          onChange={(e) => setNotificationQueryRecipientId(e.target.value)}
                        />
                        <div className="flex flex-col gap-2">
                          <label className="checkbox-label text-xs">
                            <input
                              type="checkbox"
                              className="checkbox-input"
                              checked={notificationFilterUnsent}
                              onChange={(e) => setNotificationFilterUnsent(e.target.checked)}
                            />
                            Unsent Logs Only
                          </label>
                          <button className="btn btn-secondary btn-sm" type="button" onClick={handleQueryNotificationRecipient}>
                            Find Recipient Logs
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* List View Side */}
              <div className="card-panel">
                <div className="card-header">
                  <span className="card-title">
                    <Bell size={16} className="text-indigo-400" />
                    Notification Dispatch Ledger ({notifications.length})
                  </span>
                  <button className="btn btn-secondary btn-sm" onClick={fetchNotifications}>
                    <RefreshCw size={12} />
                  </button>
                </div>
                <div className="card-body">
                  {notifications.length === 0 ? (
                    <div className="empty-placeholder">No notification logs recorded.</div>
                  ) : (
                    <div className="table-container">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>ID</th>
                            <th>School ID</th>
                            <th>Recipient ID</th>
                            <th>Channel</th>
                            <th>Status</th>
                            <th>Message</th>
                            <th>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {notifications.map((notif) => (
                            <tr key={notif.id}>
                              <td className="font-mono text-indigo-300">{notif.id}</td>
                              <td className="text-gray-400">{notif.schoolId}</td>
                              <td>{notif.recipientId}</td>
                              <td>
                                <span className="badge badge-primary">{notif.channel}</span>
                              </td>
                              <td>
                                <span className={`badge ${notif.sent ? 'badge-success' : 'badge-danger'}`}>
                                  {notif.sent ? 'Sent' : 'Pending'}
                                </span>
                                {notif.sentAt && <div className="text-[9px] text-gray-500 font-mono mt-0.5">{notif.sentAt.replace('T', ' ').substring(0, 16)}</div>}
                              </td>
                              <td className="max-w-xs truncate" title={notif.message}>{notif.message}</td>
                              <td>
                                <div className="action-cell">
                                  {!notif.sent && (
                                    <button
                                      className="btn btn-success btn-sm"
                                      onClick={() => handleMarkNotificationSent(notif.id)}
                                    >
                                      Mark Sent
                                    </button>
                                  )}
                                  <button
                                    className="btn btn-danger btn-sm"
                                    onClick={() => handleDeleteNotification(notif.id)}
                                  >
                                    <Trash2 size={12} />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}



        </div>

      </div>
    </div>
  )
}
