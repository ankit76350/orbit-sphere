import { useState, useEffect, useCallback } from 'react';
import { 
  Briefcase, Users, Plus, Trash2, Edit2, Calendar, 
  Mail, Phone, DollarSign, RefreshCw, UserCheck, Award
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

export default function StaffScreen({ schoolId, reload }) {
  const toast = useToast();
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  // Form State
  const [editingStaff, setEditingStaff] = useState(null);
  const [form, setForm] = useState({
    schoolId: schoolId || '',
    employeeId: '',
    name: '',
    department: 'Academic',
    designation: 'Teacher',
    salary: '',
    joiningDate: new Date().toISOString().slice(0, 10),
    role: 'TEACHER',
    dob: ''
  });

  // Fetch Staff List
  const fetchStaff = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    try {
      const data = await api.staff(schoolId);
      setStaffList(data || []);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load staff list.");
    } finally {
      setLoading(false);
    }
  }, [schoolId, toast]);

  useEffect(() => {
    fetchStaff();
  }, [fetchStaff]);

  useEffect(() => setForm((current) => ({ ...current, schoolId: schoolId || '' })), [schoolId]);

  const handleEditClick = (s) => {
    setEditingStaff(s);
    setForm({
      schoolId: schoolId || '',
      employeeId: s.employeeId || '',
      name: s.name || '',
      department: s.department || 'Academic',
      designation: s.designation || 'Teacher',
      salary: s.salary || '',
      joiningDate: s.joiningDate || new Date().toISOString().slice(0, 10),
      role: s.role || 'TEACHER',
      dob: s.dob || ''
    });
  };

  const handleCancelEdit = () => {
    setEditingStaff(null);
    setForm({
      schoolId: schoolId || '',
      employeeId: '',
      name: '',
      department: 'Academic',
      designation: 'Teacher',
      salary: '',
      joiningDate: new Date().toISOString().slice(0, 10),
      role: 'TEACHER',
      dob: ''
    });
  };

  const submitStaff = async () => {
    if (!form.name) {
      toast.error("Name is required.");
      return;
    }
    setBusy(true);
    try {
      const { schoolId: payloadSchoolId, ...staffFields } = form;
      const normalized = {
        ...staffFields,
        salary: form.salary ? parseFloat(form.salary) : null,
        joiningDate: form.joiningDate || null,
        dob: form.dob || null,
        role: form.role || null,
      };

      if (editingStaff) {
        const { role: _createOnlyRole, ...updatePayload } = normalized;
        await api.updateStaff(editingStaff.id, updatePayload);
        toast.success(`Staff profile "${form.name}" updated successfully.`);
      } else {
        await api.createStaff({ schoolId: payloadSchoolId, ...normalized });
        toast.success(`Staff profile "${form.name}" registered.`);
      }
      handleCancelEdit();
      fetchStaff();
      if (reload) reload();
    } catch (e) {
      toast.error(e.message || "Failed to save staff profile.");
    } finally {
      setBusy(false);
    }
  };

  const deleteStaffMember = async (s) => {
    if (!confirm(`Are you sure you want to delete staff profile "${s.name}"?`)) return;
    try {
      await api.deleteStaff(s.id);
      toast.success("Staff profile deleted.");
      fetchStaff();
      if (reload) reload();
    } catch (e) {
      toast.error("Failed to delete staff: " + e.message);
    }
  };

  const getRoleColor = (role) => {
    switch (role) {
      case 'PRINCIPAL': return 'green';
      case 'SCHOOL_ADMIN': return 'amber';
      case 'ACCOUNTANT': return 'blue';
      case 'TEACHER': return 'indigo';
      case 'WARDEN': return 'purple';
      default: return 'slate';
    }
  };

  if (!schoolId) {
    return <Empty icon={Briefcase} title="Pick a school to begin" hint="Select a school context in the top bar to manage staff registries." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      <div className="flex bg-white border border-slate-200 rounded-xl px-5 py-4 shadow-sm items-center justify-between shrink-0">
        <div>
          <h2 className="font-bold text-slate-900 text-lg flex items-center gap-2">
            <Briefcase className="text-blue-600" size={20} />
            <span>Staff Registry</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">Manage administrative officers, educators, accountants, and supporting personnel profiles.</p>
        </div>
        
        <button 
          onClick={fetchStaff}
          className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold transition"
          title="Refresh Data"
        >
          <RefreshCw size={13} />
          Reload List
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto">
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
          {/* Staff List Table */}
          <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-slate-800 text-sm">Staff Profiles Roster</h3>
                <p className="text-xs text-slate-500 mt-0.5">List of personnel currently registered in this school.</p>
              </div>
              <span className="text-xs font-semibold px-2.5 py-0.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-full">
                {staffList.length} Registered
              </span>
            </header>

            <div className="flex-1 overflow-x-auto">
              {loading ? (
                <Empty icon={RefreshCw} title="Loading staff profiles..." hint="Please wait." />
              ) : staffList.length === 0 ? (
                <Empty icon={Users} title="No staff members registered" hint="Enroll staff members using the form panel on the right." />
              ) : (
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                      <th className="px-4 py-3">Staff details</th>
                      <th className="px-4 py-3">Employee ID</th>
                      <th className="px-4 py-3">Dept & Designation</th>
                      <th className="px-4 py-3 text-right">Salary</th>
                      <th className="px-4 py-3 text-center">System Role</th>
                      <th className="px-4 py-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                    {staffList.map((s) => (
                      <tr key={s.id} className="hover:bg-slate-50/50 transition">
                        {/* details */}
                        <td className="px-4 py-3 min-w-[180px]">
                          <div className="font-bold text-slate-900 text-xs">{s.name}</div>
                          <div className="text-[10px] text-slate-400 font-medium mt-0.5">
                            Joined: {s.joiningDate ? new Date(s.joiningDate).toLocaleDateString(undefined, {month: 'short', day: 'numeric', year: 'numeric'}) : '—'}
                          </div>
                        </td>
                        {/* Employee ID */}
                        <td className="px-4 py-3 font-mono font-bold text-slate-800 select-all">{s.employeeId}</td>
                        {/* Dept & Designation */}
                        <td className="px-4 py-3">
                          <div className="text-slate-800">{s.designation || 'Staff'}</div>
                          <div className="text-slate-400 text-[10px] mt-0.5">{s.department || 'General'}</div>
                        </td>
                        {/* Salary */}
                        <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                          {s.salary ? `$${Number(s.salary).toLocaleString()}` : '—'}
                        </td>
                        {/* system role badge */}
                        <td className="px-4 py-3 text-center">
                          <Badge color={getRoleColor(s.role)}>{s.role || 'TEACHER'}</Badge>
                        </td>
                        {/* actions */}
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <button 
                              onClick={() => handleEditClick(s)}
                              className="text-slate-500 hover:text-blue-600 hover:bg-slate-100 p-1.5 rounded-lg transition"
                              title="Edit staff profile"
                            >
                              <Edit2 size={13} />
                            </button>
                            <button 
                              onClick={() => deleteStaffMember(s)}
                              className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-1.5 rounded-lg transition"
                              title="Delete staff profile"
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

          {/* Form Card */}
          <div className="xl:col-span-4">
            <Card
              title={editingStaff ? "Edit Staff Details" : "Register Staff Member"}
              subtitle={editingStaff ? `Modifying profile: ${editingStaff.name}` : "Enlist a new employee into the school roster."}
            >
              <div className="space-y-4">
                {!editingStaff && <Field label="School ID" apiName="schoolId" required><Input value={form.schoolId} onChange={(e) => setForm({ ...form, schoolId: e.target.value })} className="font-mono text-xs" /></Field>}
                <Field label="Full Name" apiName="name" required>
                  <Input 
                    value={form.name}
                    onChange={(e) => setForm({...form, name: e.target.value})}
                    placeholder="e.g. Sarah Jenkins"
                  />
                </Field>

                <div className="grid grid-cols-2 gap-4">
                  <Field label="Employee ID" apiName="employeeId" required={false}>
                    <Input
                      value={form.employeeId}
                      onChange={(e) => setForm({...form, employeeId: e.target.value})}
                      placeholder="e.g. EMP-1092"
                    />
                  </Field>
                  {!editingStaff && <Field label="System Role" apiName="role" required={false}>
                    <Select 
                      value={form.role}
                      onChange={(e) => setForm({...form, role: e.target.value})}
                    >
                      <option value="">— omitted —</option>
                      <option value="TEACHER">Teacher</option>
                      <option value="PRINCIPAL">Principal</option>
                      <option value="SCHOOL_ADMIN">School Admin</option>
                      <option value="ACCOUNTANT">Accountant</option>
                      <option value="HR_MANAGER">HR Manager</option>
                      <option value="WARDEN">Warden</option>
                      <option value="STORE_MANAGER">Store Manager</option>
                      <option value="DRIVER">Driver</option>
                      <option value="SUPER_ADMIN">Super Admin</option>
                      <option value="PARENT">Parent</option>
                      <option value="STUDENT">Student</option>
                    </Select>
                  </Field>}
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Field label="Department" apiName="department" required={false}>
                    <Input
                      value={form.department}
                      onChange={(e) => setForm({...form, department: e.target.value})}
                      placeholder="e.g. Academic"
                    />
                  </Field>
                  <Field label="Designation" apiName="designation" required={false}>
                    <Input 
                      value={form.designation}
                      onChange={(e) => setForm({...form, designation: e.target.value})}
                      placeholder="e.g. Science Teacher"
                    />
                  </Field>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Field label="Monthly Salary ($)" apiName="salary" required={false}>
                    <Input 
                      type="number"
                      value={form.salary}
                      onChange={(e) => setForm({...form, salary: e.target.value})}
                      placeholder="e.g. 4500"
                    />
                  </Field>
                  <Field label="Joining Date" apiName="joiningDate" required={false}>
                    <Input 
                      type="date"
                      value={form.joiningDate}
                      onChange={(e) => setForm({...form, joiningDate: e.target.value})}
                    />
                  </Field>
                </div>

                <Field label="Date of Birth" apiName="dob" required={false}>
                  <Input 
                    type="date"
                    value={form.dob}
                    onChange={(e) => setForm({...form, dob: e.target.value})}
                  />
                </Field>

                <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                  {editingStaff && (
                    <Button variant="default" onClick={handleCancelEdit}>Cancel</Button>
                  )}
                  <Button 
                    variant="primary" 
                    onClick={submitStaff}
                    disabled={busy || !form.name}
                  >
                    {busy ? <RefreshCw size={14} className="animate-spin" /> : <Plus size={14} />}
                    {editingStaff ? 'Save Profile' : 'Enroll Staff'}
                  </Button>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
