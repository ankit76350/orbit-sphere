import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Coins, Plus, Trash2, Edit2, Wallet, DollarSign, Search,
  History, ArrowUpRight, ArrowDownRight, RefreshCw, X, User, Check, Receipt
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

const PAYMENT_MODES = ['CASH', 'WALLET', 'ONLINE', 'CHEQUE'];

export default function FinanceScreen({ schoolId, year }) {
  const toast = useToast();

  // Context
  const [students, setStudents] = useState([]);
  const [fees, setFees] = useState([]);
  const [wallets, setWallets] = useState({}); // studentId -> wallet
  const [loadingData, setLoadingData] = useState(false);
  const [busy, setBusy] = useState(false);

  // Master-detail selection
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [studentSearch, setStudentSearch] = useState('');
  const [detailTab, setDetailTab] = useState('fees'); // 'fees' | 'wallet'

  // Modals
  const [payModal, setPayModal] = useState(null); // fee invoice being paid
  const [payForm, setPayForm] = useState({ amount: '', paymentMode: 'CASH', remarks: '' });
  const [auditStudent, setAuditStudent] = useState(null);
  const [auditTransactions, setAuditTransactions] = useState([]);
  const [receiptsFee, setReceiptsFee] = useState(null); // fee whose receipts are shown
  const [receipts, setReceipts] = useState([]);

  // Forms
  const [editingFee, setEditingFee] = useState(null);
  const emptyFeeForm = {
    type: 'TUITION',
    amount: '',
    dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
  };
  const [feeForm, setFeeForm] = useState(emptyFeeForm);
  const [walletForm, setWalletForm] = useState({ operation: 'CREDIT', amount: '', remarks: '' });

  // ---- Data loading ----
  const fetchAll = useCallback(async () => {
    if (!schoolId) return;
    setLoadingData(true);
    try {
      const stList = year ? await api.studentsByYear(schoolId, year) : await api.students(schoolId);
      const feeList = year ? await api.feesByYear(schoolId, year) : await api.fees(schoolId);
      setStudents(stList || []);
      setFees(feeList || []);

      const walletMap = {};
      await Promise.all(
        (stList || []).map(async (s) => {
          try {
            const w = await api.getWallet(s.id);
            if (w) walletMap[s.id] = w;
          } catch (err) {
            console.error(`Failed to load wallet for ${s.id}`, err);
          }
        })
      );
      setWallets(walletMap);
    } catch (e) {
      console.error(e);
      toast.error('Failed to load finance data.');
    } finally {
      setLoadingData(false);
    }
  }, [schoolId, year, toast]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  // Keep a valid selection when the student list changes
  useEffect(() => {
    if (students.length === 0) {
      setSelectedStudentId(null);
    } else if (!students.some((s) => s.id === selectedStudentId)) {
      setSelectedStudentId(students[0].id);
    }
  }, [students, selectedStudentId]);

  // ---- Helpers ----
  const getStudentName = (sid) => {
    const s = students.find((x) => x.id === sid);
    return s ? `${s.firstName} ${s.lastName || ''}`.trim() : sid || '—';
  };

  const dueOf = (f) => (f.amount || 0) - (f.paidAmount || 0);

  const feesByStudent = useMemo(() => {
    const map = {};
    fees.forEach((f) => {
      (map[f.studentId] = map[f.studentId] || []).push(f);
    });
    return map;
  }, [fees]);

  const studentDues = useCallback(
    (sid) => (feesByStudent[sid] || []).reduce((sum, f) => sum + Math.max(0, dueOf(f)), 0),
    [feesByStudent]
  );

  const filteredStudents = useMemo(() => {
    const q = studentSearch.trim().toLowerCase();
    if (!q) return students;
    return students.filter(
      (s) =>
        `${s.firstName} ${s.lastName || ''}`.toLowerCase().includes(q) ||
        (s.admissionNo || '').toLowerCase().includes(q)
    );
  }, [students, studentSearch]);

  const selectedStudent = students.find((s) => s.id === selectedStudentId) || null;
  const selectedFees = feesByStudent[selectedStudentId] || [];
  const selectedWallet = wallets[selectedStudentId];
  const selectedBalance = selectedWallet?.balance || 0;

  const getStatusColor = (s) => {
    switch (s) {
      case 'PAID': return 'green';
      case 'PARTIALLY_PAID': return 'blue';
      case 'UNPAID': return 'rose';
      case 'OVERDUE': return 'amber';
      default: return 'slate';
    }
  };

  // ---- Fee invoice CRUD ----
  const resetFeeForm = () => {
    setEditingFee(null);
    setFeeForm(emptyFeeForm);
  };

  const submitFee = async () => {
    if (!selectedStudentId || !feeForm.amount) {
      toast.error('Select a student and enter an amount.');
      return;
    }
    setBusy(true);
    try {
      const payload = {
        schoolId,
        academicYear: year,
        studentId: selectedStudentId,
        type: feeForm.type,
        amount: parseFloat(feeForm.amount),
        dueDate: feeForm.dueDate,
      };
      if (editingFee) {
        await api.updateFee(editingFee.id, payload);
        toast.success('Invoice updated.');
      } else {
        await api.createFee(payload);
        toast.success('New invoice issued.');
      }
      resetFeeForm();
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Failed to save invoice.');
    } finally {
      setBusy(false);
    }
  };

  const handleEditFee = (fee) => {
    setEditingFee(fee);
    setFeeForm({
      type: fee.type || 'TUITION',
      amount: String(fee.amount || ''),
      dueDate: fee.dueDate || emptyFeeForm.dueDate,
    });
  };

  const handleDeleteFee = async (fee) => {
    if (!confirm(`Delete this ${fee.type} invoice for ${getStudentName(fee.studentId)}?`)) return;
    try {
      await api.deleteFee(fee.id);
      toast.success('Invoice deleted.');
      fetchAll();
    } catch (e) {
      toast.error('Failed to delete invoice: ' + e.message);
    }
  };

  // ---- Payments (unified recordPayment) ----
  const openPayModal = (fee) => {
    setPayModal(fee);
    setPayForm({ amount: String(Math.max(0, dueOf(fee))), paymentMode: 'CASH', remarks: '' });
  };

  const submitPayment = async () => {
    const amount = parseFloat(payForm.amount);
    if (!amount || amount <= 0) {
      toast.error('Enter a valid payment amount.');
      return;
    }
    if (payForm.paymentMode === 'WALLET' && selectedBalance < amount) {
      toast.error(`Insufficient wallet balance ($${selectedBalance.toLocaleString()}).`);
      return;
    }
    setBusy(true);
    try {
      await api.recordPayment(payModal.id, {
        amount,
        paymentMode: payForm.paymentMode,
        remarks: payForm.remarks,
      });
      toast.success(`Payment recorded (${payForm.paymentMode}).`);
      setPayModal(null);
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Payment failed.');
    } finally {
      setBusy(false);
    }
  };

  // ---- Wallet ops ----
  const submitWalletOp = async () => {
    if (!selectedStudentId || !walletForm.amount || !walletForm.remarks) {
      toast.error('Amount and remarks are required.');
      return;
    }
    setBusy(true);
    try {
      const amount = parseFloat(walletForm.amount);
      if (walletForm.operation === 'CREDIT') {
        await api.creditWallet(selectedStudentId, amount, walletForm.remarks);
        toast.success(`Deposited $${amount.toLocaleString()}.`);
      } else {
        await api.debitWallet(selectedStudentId, amount, walletForm.remarks);
        toast.success(`Debited $${amount.toLocaleString()}.`);
      }
      setWalletForm({ ...walletForm, amount: '', remarks: '' });
      fetchAll();
    } catch (e) {
      toast.error(e.message || 'Wallet transaction failed.');
    } finally {
      setBusy(false);
    }
  };

  const viewAudit = async (sid) => {
    setAuditStudent(sid);
    setAuditTransactions([]);
    try {
      setAuditTransactions((await api.walletTransactions(sid)) || []);
    } catch (e) {
      toast.error('Failed to load wallet ledger.');
    }
  };

  const viewReceipts = async (fee) => {
    setReceiptsFee(fee);
    setReceipts([]);
    try {
      setReceipts((await api.feePayments(fee.id)) || []);
    } catch (e) {
      toast.error('Failed to load receipts.');
    }
  };

  if (!schoolId) {
    return <Empty icon={Coins} title="Pick a school to begin" hint="Select a school context in the top bar to manage finance." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex items-center justify-between bg-white px-4 py-3 rounded-xl shadow-sm shrink-0">
        <div>
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
            <Coins size={16} className="text-blue-600" /> Finance
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {year ? `Academic year ${year}` : 'All academic years'} · {students.length} student{students.length === 1 ? '' : 's'}
          </p>
        </div>
        <button
          onClick={fetchAll}
          className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold transition"
          title="Refresh Data"
        >
          <RefreshCw size={13} className={loadingData ? 'animate-spin' : ''} />
          Reload Data
        </button>
      </div>

      <div className="flex-1 min-h-0 grid grid-cols-1 xl:grid-cols-12 gap-4">
        {/* LEFT: STUDENT MASTER LIST */}
        <div className="xl:col-span-4 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <header className="px-4 py-3 border-b border-slate-100 bg-slate-50/50 shrink-0">
            <h3 className="font-bold text-slate-800 text-sm">Students</h3>
            <div className="relative mt-2">
              <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={studentSearch}
                onChange={(e) => setStudentSearch(e.target.value)}
                placeholder="Search name or admission no."
                className="w-full pl-8 pr-3 py-1.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
              />
            </div>
          </header>
          <div className="flex-1 overflow-y-auto">
            {loadingData ? (
              <Empty icon={RefreshCw} title="Loading students..." hint="Please wait." />
            ) : filteredStudents.length === 0 ? (
              <Empty icon={User} title="No students found" hint={year ? `No students enrolled for ${year}.` : 'Please enroll students first.'} />
            ) : (
              <ul className="divide-y divide-slate-100">
                {filteredStudents.map((s) => {
                  const dues = studentDues(s.id);
                  const bal = wallets[s.id]?.balance || 0;
                  const active = s.id === selectedStudentId;
                  return (
                    <li key={s.id}>
                      <button
                        onClick={() => { setSelectedStudentId(s.id); resetFeeForm(); }}
                        className={`w-full text-left px-4 py-3 flex items-center justify-between transition ${active ? 'bg-blue-50/70 border-l-2 border-blue-600' : 'hover:bg-slate-50 border-l-2 border-transparent'}`}
                      >
                        <div className="min-w-0">
                          <div className={`text-xs font-bold truncate ${active ? 'text-blue-700' : 'text-slate-800'}`}>
                            {s.firstName} {s.lastName || ''}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono">{s.admissionNo || '—'}</div>
                        </div>
                        <div className="text-right shrink-0 ml-2">
                          {dues > 0
                            ? <Badge color="rose">${dues.toLocaleString()} due</Badge>
                            : <Badge color="green">Clear</Badge>}
                          <div className="text-[10px] text-slate-400 mt-1">Wallet ${bal.toLocaleString()}</div>
                        </div>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>

        {/* RIGHT: DETAIL FOR SELECTED STUDENT */}
        <div className="xl:col-span-8 flex flex-col min-h-0 gap-4">
          {!selectedStudent ? (
            <div className="flex-1 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center justify-center">
              <Empty icon={User} title="Select a student" hint="Pick a student from the list to view fees and wallet." />
            </div>
          ) : (
            <>
              {/* Student header */}
              <div className="bg-white border border-slate-200 rounded-2xl shadow-sm px-5 py-4 flex items-center justify-between shrink-0">
                <div>
                  <h3 className="font-bold text-slate-900 text-base">{selectedStudent.firstName} {selectedStudent.lastName || ''}</h3>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">
                    {selectedStudent.admissionNo || '—'}
                    {selectedStudent.currentAcademicRecord?.rollNo ? ` · Roll ${selectedStudent.currentAcademicRecord.rollNo}` : ''}
                  </p>
                </div>
                <div className="text-right">
                  <div className="text-[10px] uppercase tracking-wide text-slate-400 font-semibold">Wallet Balance</div>
                  <div className="text-lg font-bold font-mono text-blue-700">${selectedBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                </div>
              </div>

              {/* Detail tabs */}
              <div className="flex gap-1 border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm shrink-0">
                <button
                  onClick={() => setDetailTab('fees')}
                  className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${detailTab === 'fees' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
                >
                  <DollarSign size={16} /> Fees Ledger
                </button>
                <button
                  onClick={() => setDetailTab('wallet')}
                  className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors ${detailTab === 'wallet' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
                >
                  <Wallet size={16} /> Student Wallet
                </button>
              </div>

              <div className="flex-1 min-h-0 overflow-y-auto grid grid-cols-1 lg:grid-cols-3 gap-4">
                {detailTab === 'fees' && (
                  <>
                    {/* Invoices table */}
                    <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
                      <header className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                        <h4 className="font-bold text-slate-800 text-sm">Issued Fee Invoices</h4>
                        <p className="text-[11px] text-slate-500 mt-0.5">Dues and collection history for this student.</p>
                      </header>
                      <div className="flex-1 overflow-x-auto">
                        {selectedFees.length === 0 ? (
                          <Empty icon={DollarSign} title="No invoices issued yet" hint="Use the issuance card to generate one." />
                        ) : (
                          <table className="w-full text-left border-collapse text-xs">
                            <thead>
                              <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                                <th className="px-4 py-3">Type</th>
                                <th className="px-4 py-3 text-right">Invoiced</th>
                                <th className="px-4 py-3 text-right">Paid</th>
                                <th className="px-4 py-3 text-right">Due</th>
                                <th className="px-4 py-3 text-center">Status</th>
                                <th className="px-4 py-3 text-right">Actions</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                              {selectedFees.map((f) => {
                                const due = dueOf(f);
                                return (
                                  <tr key={f.id} className="hover:bg-slate-50/50 transition">
                                    <td className="px-4 py-3 font-semibold text-slate-800">
                                      {f.type}
                                      <div className="text-[9px] text-slate-400 font-normal">
                                        Due {f.dueDate ? new Date(f.dueDate).toLocaleDateString() : '—'}
                                      </div>
                                    </td>
                                    <td className="px-4 py-3 text-right font-mono font-bold">${(f.amount || 0).toLocaleString()}</td>
                                    <td className="px-4 py-3 text-right font-mono text-green-700">${(f.paidAmount || 0).toLocaleString()}</td>
                                    <td className="px-4 py-3 text-right font-mono font-bold text-rose-700">${due > 0 ? due.toLocaleString() : '0'}</td>
                                    <td className="px-4 py-3 text-center"><Badge color={getStatusColor(f.status)}>{f.status}</Badge></td>
                                    <td className="px-4 py-3">
                                      <div className="flex justify-end gap-1 items-center">
                                        {due > 0 && (
                                          <button
                                            onClick={() => openPayModal(f)}
                                            className="flex items-center gap-0.5 bg-green-50 text-green-700 border border-green-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-green-100 transition"
                                            title="Collect payment"
                                          >
                                            <DollarSign size={10} /> Collect
                                          </button>
                                        )}
                                        <button onClick={() => viewReceipts(f)} className="text-slate-400 hover:text-blue-600 p-1.5 rounded-lg transition" title="Receipts"><Receipt size={13} /></button>
                                        <button onClick={() => handleEditFee(f)} className="text-slate-400 hover:text-blue-600 p-1.5 rounded-lg transition" title="Edit"><Edit2 size={13} /></button>
                                        <button onClick={() => handleDeleteFee(f)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition" title="Delete"><Trash2 size={13} /></button>
                                      </div>
                                    </td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        )}
                      </div>
                    </div>

                    {/* Issue invoice card */}
                    <div className="lg:col-span-1">
                      <Card
                        title={editingFee ? 'Modify Invoice' : 'Issue Invoice'}
                        subtitle={editingFee ? 'Update invoice parameters.' : `Generate a new invoice for ${selectedStudent.firstName}.`}
                      >
                        <div className="space-y-4">
                          <Field label="Fee Category *">
                            <Select value={feeForm.type} onChange={(e) => setFeeForm({ ...feeForm, type: e.target.value })}>
                              <option value="TUITION">Tuition Dues</option>
                              <option value="HOSTEL">Hostel Dues</option>
                              <option value="TRANSPORT">Bus & Transport</option>
                              <option value="MESS">Mess & Food</option>
                              <option value="LIBRARY">Library Fee</option>
                              <option value="OTHER">Other Dues</option>
                            </Select>
                          </Field>
                          <Field label="Due Date *">
                            <Input type="date" value={feeForm.dueDate} onChange={(e) => setFeeForm({ ...feeForm, dueDate: e.target.value })} />
                          </Field>
                          <Field label="Invoice Amount ($) *">
                            <Input type="number" value={feeForm.amount} onChange={(e) => setFeeForm({ ...feeForm, amount: e.target.value })} placeholder="e.g. 1200" />
                          </Field>
                          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                            {editingFee && <Button variant="default" onClick={resetFeeForm}>Cancel</Button>}
                            <Button variant="primary" onClick={submitFee} disabled={busy || !feeForm.amount}>
                              {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                              {editingFee ? 'Save' : 'Issue'}
                            </Button>
                          </div>
                        </div>
                      </Card>
                    </div>
                  </>
                )}

                {detailTab === 'wallet' && (
                  <>
                    {/* Wallet overview + ledger button */}
                    <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl shadow-sm p-6 flex flex-col items-center justify-center gap-3">
                      <Wallet size={32} className="text-blue-500" />
                      <div className="text-3xl font-bold font-mono text-slate-800">
                        ${selectedBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </div>
                      <p className="text-xs text-slate-400">Available prepaid balance</p>
                      <Button variant="default" onClick={() => viewAudit(selectedStudentId)}>
                        <History size={14} /> View Transaction Ledger
                      </Button>
                    </div>

                    {/* Wallet operation form */}
                    <div className="lg:col-span-1">
                      <Card title="Wallet Operation" subtitle="Deposit or debit prepaid credits.">
                        <div className="space-y-4">
                          <Field label="Action *">
                            <Select value={walletForm.operation} onChange={(e) => setWalletForm({ ...walletForm, operation: e.target.value })}>
                              <option value="CREDIT">Deposit (+)</option>
                              <option value="DEBIT">Withdraw (-)</option>
                            </Select>
                          </Field>
                          <Field label="Amount ($) *">
                            <Input type="number" value={walletForm.amount} onChange={(e) => setWalletForm({ ...walletForm, amount: e.target.value })} placeholder="e.g. 50" />
                          </Field>
                          <Field label="Remarks *">
                            <Input value={walletForm.remarks} onChange={(e) => setWalletForm({ ...walletForm, remarks: e.target.value })} placeholder="e.g. Parent deposit" />
                          </Field>
                          <div className="pt-3 border-t border-slate-100 flex justify-end">
                            <Button variant="primary" onClick={submitWalletOp} disabled={busy || !walletForm.amount || !walletForm.remarks}>
                              {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />}
                              Post
                            </Button>
                          </div>
                        </div>
                      </Card>
                    </div>
                  </>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* PAYMENT MODAL */}
      {payModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Collect Payment</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">{getStudentName(payModal.studentId)} · {payModal.type}</p>
              </div>
              <button onClick={() => setPayModal(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 space-y-4">
              <div className="grid grid-cols-2 text-xs border border-slate-100 bg-slate-50/50 rounded-lg p-3">
                <div>
                  <span className="text-slate-400 font-medium">Remaining Due</span>
                  <div className="font-bold text-rose-700 mt-0.5 font-mono">${Math.max(0, dueOf(payModal)).toLocaleString()}</div>
                </div>
                <div>
                  <span className="text-slate-400 font-medium">Wallet Balance</span>
                  <div className="font-bold text-blue-700 mt-0.5 font-mono">${selectedBalance.toLocaleString()}</div>
                </div>
              </div>
              <Field label="Payment Mode *">
                <Select value={payForm.paymentMode} onChange={(e) => setPayForm({ ...payForm, paymentMode: e.target.value })}>
                  {PAYMENT_MODES.map((m) => <option key={m} value={m}>{m}</option>)}
                </Select>
              </Field>
              <Field label="Amount ($) *">
                <Input type="number" value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} placeholder="e.g. 500" />
              </Field>
              <Field label="Remarks">
                <Input value={payForm.remarks} onChange={(e) => setPayForm({ ...payForm, remarks: e.target.value })} placeholder="Optional note" />
              </Field>
              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <Button variant="default" onClick={() => setPayModal(null)}>Cancel</Button>
                <Button variant="primary" onClick={submitPayment} disabled={busy || !payForm.amount}>
                  {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />}
                  Record Payment
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* RECEIPTS MODAL */}
      {receiptsFee && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[85vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Payment Receipts</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">{receiptsFee.type} · {getStudentName(receiptsFee.studentId)}</p>
              </div>
              <button onClick={() => setReceiptsFee(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 flex-1 overflow-y-auto min-h-0">
              {receipts.length === 0 ? (
                <Empty icon={Receipt} title="No payments recorded" hint="Collections against this invoice will appear here." />
              ) : (
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                      <th className="px-4 py-3">Receipt No.</th>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3 text-center">Mode</th>
                      <th className="px-4 py-3 text-right">Amount</th>
                      <th className="px-4 py-3">Remarks</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                    {receipts.map((r) => (
                      <tr key={r.id} className="hover:bg-slate-50/50 transition">
                        <td className="px-4 py-3 font-mono text-slate-500">{r.receiptNo}</td>
                        <td className="px-4 py-3 text-slate-400">{r.paidOn ? new Date(r.paidOn).toLocaleString() : '—'}</td>
                        <td className="px-4 py-3 text-center"><Badge color="slate">{r.paymentMode}</Badge></td>
                        <td className="px-4 py-3 text-right font-mono font-bold text-green-700">${(r.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                        <td className="px-4 py-3 text-slate-600 italic">{r.remarks || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 shrink-0 flex justify-end">
              <Button variant="default" onClick={() => setReceiptsFee(null)}>Close</Button>
            </footer>
          </div>
        </div>
      )}

      {/* WALLET LEDGER MODAL */}
      {auditStudent && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[85vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Wallet Transaction Ledger</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">{getStudentName(auditStudent)}</p>
              </div>
              <button onClick={() => setAuditStudent(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 flex-1 overflow-y-auto min-h-0">
              {auditTransactions.length === 0 ? (
                <Empty icon={History} title="No transactions found" hint="Wallet deposits and debits will display here." />
              ) : (
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3 text-center">Type</th>
                      <th className="px-4 py-3 text-right">Amount</th>
                      <th className="px-4 py-3 text-right">Balance After</th>
                      <th className="px-4 py-3">Remarks</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                    {auditTransactions.map((tx) => (
                      <tr key={tx.id} className="hover:bg-slate-50/50 transition">
                        <td className="px-4 py-3 text-slate-400">{tx.transactionDate ? new Date(tx.transactionDate).toLocaleString() : (tx.createdAt ? new Date(tx.createdAt).toLocaleString() : '—')}</td>
                        <td className="px-4 py-3 text-center">
                          <span className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full text-[10px] font-bold ${tx.type === 'CREDIT' ? 'bg-green-50 text-green-700' : 'bg-rose-50 text-rose-700'}`}>
                            {tx.type === 'CREDIT' ? <ArrowUpRight size={10} /> : <ArrowDownRight size={10} />} {tx.type}
                          </span>
                        </td>
                        <td className={`px-4 py-3 text-right font-mono font-bold ${tx.type === 'CREDIT' ? 'text-green-700' : 'text-rose-700'}`}>${(tx.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                        <td className="px-4 py-3 text-right font-mono text-slate-500">${(tx.balanceAfter || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                        <td className="px-4 py-3 text-slate-600 italic">{tx.remarks || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 shrink-0 flex justify-end">
              <Button variant="default" onClick={() => setAuditStudent(null)}>Close</Button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );
}
