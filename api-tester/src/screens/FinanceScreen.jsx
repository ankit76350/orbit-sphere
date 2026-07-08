import { useState, useEffect, useCallback } from 'react';
import { 
  Coins, Plus, Trash2, Edit2, CreditCard, DollarSign, Wallet, 
  History, ArrowUpRight, ArrowDownRight, RefreshCw, X, User, Check
} from 'lucide-react';
import { api } from '../api.js';
import { Card, Button, Field, Input, Select, Badge, Empty, useToast } from '../components/ui.jsx';

export default function FinanceScreen({ schoolId, years, year, reload }) {
  const toast = useToast();
  const [subTab, setSubTab] = useState('fees'); // 'fees', 'wallets'

  // Context lists
  const [students, setStudents] = useState([]);
  
  // Data lists
  const [fees, setFees] = useState([]);
  const [wallets, setWallets] = useState({}); // studentId -> wallet object
  const [auditTransactions, setAuditTransactions] = useState([]);
  const [selectedAuditStudent, setSelectedAuditStudent] = useState(null);

  // Load / UI states
  const [loadingData, setLoadingData] = useState(false);
  const [busy, setBusy] = useState(false);
  const [showPayModal, setShowPayModal] = useState(null); // fee object for cash payment
  const [payAmount, setPayAmount] = useState('');
  
  // Form states
  const [editingFee, setEditingFee] = useState(null);
  const [feeForm, setFeeForm] = useState({
    studentId: '',
    type: 'TUITION',
    amount: '',
    dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10), // +30 days
  });

  const [walletForm, setWalletForm] = useState({
    studentId: '',
    operation: 'CREDIT', // 'CREDIT', 'DEBIT'
    amount: '',
    remarks: ''
  });

  // Helpers
  const getStudentName = (sid) => {
    const s = students.find((x) => x.id === sid);
    return s ? `${s.firstName} ${s.lastName || ''}` : sid || '—';
  };

  const getStudentAdmission = (sid) => {
    const s = students.find((x) => x.id === sid);
    return s ? s.admissionNo : '';
  };

  const fetchContextAndWallets = useCallback(async () => {
    if (!schoolId) return;
    try {
      const stList = await api.students(schoolId);
      setStudents(stList || []);
      
      if (stList && stList.length > 0) {
        setFeeForm((f) => ({ ...f, studentId: stList[0].id }));
        setWalletForm((f) => ({ ...f, studentId: stList[0].id }));
        
        // Fetch wallet details for all students
        const walletMap = {};
        await Promise.all(
          stList.map(async (s) => {
            try {
              const wallet = await api.getWallet(s.id);
              if (wallet) {
                walletMap[s.id] = wallet;
              }
            } catch (err) {
              console.error(`Failed to load wallet for student ${s.id}`, err);
            }
          })
        );
        setWallets(walletMap);
      }
    } catch (e) {
      console.error("Failed to load context", e);
    }
  }, [schoolId]);

  const fetchFees = useCallback(async () => {
    if (!schoolId) return;
    setLoadingData(true);
    try {
      const data = year 
        ? await api.feesByYear(schoolId, year)
        : await api.fees(schoolId);
      setFees(data || []);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load fee records.");
    } finally {
      setLoadingData(false);
    }
  }, [schoolId, year, toast]);

  useEffect(() => {
    fetchContextAndWallets();
  }, [fetchContextAndWallets]);

  useEffect(() => {
    if (subTab === 'fees') {
      fetchFees();
    }
  }, [subTab, fetchFees]);

  const handleCancelFeeEdit = () => {
    setEditingFee(null);
    setFeeForm({
      studentId: students.length > 0 ? students[0].id : '',
      type: 'TUITION',
      amount: '',
      dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
    });
  };

  const reloadData = () => {
    fetchContextAndWallets();
    fetchFees();
  };

  // --- CRUD FEE INVOICE ---
  const submitFee = async () => {
    if (!feeForm.studentId || !feeForm.amount) {
      toast.error("Student and Amount are required.");
      return;
    }
    setBusy(true);
    try {
      const payload = {
        schoolId,
        academicYear: year,
        ...feeForm,
        amount: parseFloat(feeForm.amount),
        paidAmount: editingFee ? editingFee.paidAmount : 0,
        status: editingFee ? editingFee.status : 'UNPAID'
      };

      if (editingFee) {
        await api.updateFee(editingFee.id, payload);
        toast.success("Fee invoice record updated.");
      } else {
        await api.createFee(payload);
        toast.success("New fee invoice issued.");
      }
      handleCancelFeeEdit();
      fetchFees();
    } catch (e) {
      toast.error(e.message || "Failed to save fee invoice.");
    } finally {
      setBusy(false);
    }
  };

  const handleEditFee = (fee) => {
    setEditingFee(fee);
    setFeeForm({
      studentId: fee.studentId || '',
      type: fee.type || 'TUITION',
      amount: String(fee.amount || ''),
      dueDate: fee.dueDate || '',
    });
  };

  const handleDeleteFee = async (fee) => {
    if (!confirm(`Are you sure you want to delete this invoice for ${getStudentName(fee.studentId)}?`)) return;
    try {
      await api.deleteFee(fee.id);
      toast.success("Fee record deleted.");
      fetchFees();
    } catch (e) {
      toast.error("Failed to delete invoice: " + e.message);
    }
  };

  // --- CASH & WALLET PAYMENTS ---
  const handleOpenPayModal = (fee) => {
    setShowPayModal(fee);
    setPayAmount(String(fee.amount - (fee.paidAmount || 0)));
  };

  const handleCashPayment = async () => {
    if (!payAmount || parseFloat(payAmount) <= 0) {
      toast.error("Please enter a valid payment amount.");
      return;
    }
    setBusy(true);
    try {
      await api.payFee(showPayModal.id, parseFloat(payAmount));
      toast.success("Cash payment processed successfully.");
      setShowPayModal(null);
      fetchFees();
    } catch (e) {
      toast.error(e.message || "Payment transaction failed.");
    } finally {
      setBusy(false);
    }
  };

  const handleWalletPayment = async (fee) => {
    const due = fee.amount - (fee.paidAmount || 0);
    const balance = wallets[fee.studentId]?.balance || 0;

    if (balance < due) {
      toast.error(`Insufficient wallet funds. Balance: $${balance.toLocaleString()}. Required: $${due.toLocaleString()}`);
      return;
    }

    if (!confirm(`Pay $${due.toLocaleString()} invoice using ${getStudentName(fee.studentId)}'s virtual wallet balance?`)) return;

    setBusy(true);
    try {
      await api.payFeeViaWallet(fee.id, due);
      toast.success("Payment debited from virtual wallet.");
      reloadData();
    } catch (e) {
      toast.error("Wallet transaction failed: " + e.message);
    } finally {
      setBusy(false);
    }
  };

  // --- WALLET TOP-UPS & DEBITS ---
  const submitWalletOperation = async () => {
    if (!walletForm.studentId || !walletForm.amount || !walletForm.remarks) {
      toast.error("Student, Amount, and Remarks are required.");
      return;
    }
    setBusy(true);
    try {
      const amount = parseFloat(walletForm.amount);
      if (walletForm.operation === 'CREDIT') {
        await api.creditWallet(walletForm.studentId, amount, walletForm.remarks);
        toast.success(`Credited $${amount.toLocaleString()} to wallet.`);
      } else {
        await api.debitWallet(walletForm.studentId, amount, walletForm.remarks);
        toast.success(`Debited $${amount.toLocaleString()} from wallet.`);
      }
      setWalletForm({
        ...walletForm,
        amount: '',
        remarks: ''
      });
      fetchContextAndWallets();
    } catch (e) {
      toast.error(e.message || "Failed to process wallet transaction.");
    } finally {
      setBusy(false);
    }
  };

  const viewAuditLog = async (sid) => {
    setSelectedAuditStudent(sid);
    setAuditTransactions([]);
    try {
      const list = await api.walletTransactions(sid);
      setAuditTransactions(list || []);
    } catch (e) {
      toast.error("Failed to load audit transaction log.");
    }
  };

  // Color mappings
  const getStatusColor = (s) => {
    switch (s) {
      case 'PAID': return 'green';
      case 'PARTIALLY_PAID': return 'blue';
      case 'UNPAID': return 'rose';
      case 'OVERDUE': return 'amber';
      default: return 'slate';
    }
  };

  if (!schoolId) {
    return <Empty icon={Coins} title="Pick a school to begin" hint="Select a school context in the top bar to manage finance registries." />;
  }

  return (
    <div className="flex flex-col h-full gap-4 text-slate-800 animate-in fade-in duration-200">
      {/* Navigation sub-tabs header */}
      <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm justify-between items-center shrink-0">
        <div className="flex gap-1">
          <button
            onClick={() => setSubTab('fees')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'fees' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <DollarSign size={16} />
            Fees Ledger
          </button>
          <button
            onClick={() => setSubTab('wallets')}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors -mb-px ${subTab === 'wallets' ? 'border-blue-600 text-blue-600 bg-blue-50/30' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
          >
            <Wallet size={16} />
            Student Wallets
          </button>
        </div>
        
        <button 
          onClick={reloadData}
          className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 rounded-lg text-slate-500 text-xs font-semibold mr-2 mb-2 transition"
          title="Refresh Data"
        >
          <RefreshCw size={13} />
          Reload Data
        </button>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto">
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 h-full">
          {/* LEFT PANEL: TABLE DISPLAY LIST */}
          <div className="xl:col-span-8 flex flex-col bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <header className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-slate-800 text-sm">
                  {subTab === 'fees' ? 'Issued Fee Invoices' : 'Virtual Wallets Registry'}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  {subTab === 'fees' ? 'List of all student fee dues and balance logs.' : 'Student cash accounts and deposit transaction entries.'}
                </p>
              </div>
            </header>

            <div className="flex-1 overflow-x-auto">
              {loadingData ? (
                <Empty icon={RefreshCw} title="Loading records..." hint="Please wait." />
              ) : (
                <>
                  {/* FEES TABLE */}
                  {subTab === 'fees' && (
                    fees.length === 0 ? (
                      <Empty icon={DollarSign} title="No invoices issued yet" hint="Create invoices using the issuance card on the right panel." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student Name</th>
                            <th className="px-4 py-3">Fee Type</th>
                            <th className="px-4 py-3 text-right">Invoiced</th>
                            <th className="px-4 py-3 text-right">Paid</th>
                            <th className="px-4 py-3 text-right">Balance Due</th>
                            <th className="px-4 py-3 text-center">Due Date</th>
                            <th className="px-4 py-3 text-center">Status</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {fees.map((f) => {
                            const due = f.amount - (f.paidAmount || 0);
                            return (
                              <tr key={f.id} className="hover:bg-slate-50/50 transition">
                                <td className="px-4 py-3 font-bold text-slate-900">
                                  <div>{getStudentName(f.studentId)}</div>
                                  <div className="text-[9px] text-slate-400 font-mono mt-0.5">{getStudentAdmission(f.studentId)}</div>
                                </td>
                                <td className="px-4 py-3 font-semibold text-slate-700">{f.type}</td>
                                <td className="px-4 py-3 text-right font-mono font-bold text-slate-800">${f.amount.toLocaleString()}</td>
                                <td className="px-4 py-3 text-right font-mono text-green-700">${(f.paidAmount || 0).toLocaleString()}</td>
                                <td className="px-4 py-3 text-right font-mono font-bold text-rose-700">${due > 0 ? due.toLocaleString() : '0'}</td>
                                <td className="px-4 py-3 text-center text-slate-400 font-medium">
                                  {f.dueDate ? new Date(f.dueDate).toLocaleDateString() : '—'}
                                </td>
                                <td className="px-4 py-3 text-center">
                                  <Badge color={getStatusColor(f.status)}>{f.status}</Badge>
                                </td>
                                <td className="px-4 py-3 text-right">
                                  <div className="flex justify-end gap-1 items-center">
                                    {due > 0 && (
                                      <>
                                        <button 
                                          onClick={() => handleOpenPayModal(f)}
                                          className="flex items-center gap-0.5 bg-green-50 text-green-700 border border-green-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-green-100 transition"
                                          title="Record Cash Payment"
                                        >
                                          <DollarSign size={10} /> Pay
                                        </button>
                                        <button 
                                          onClick={() => handleWalletPayment(f)}
                                          className="flex items-center gap-0.5 bg-blue-50 text-blue-700 border border-blue-200 px-2 py-1 rounded text-[10px] font-bold hover:bg-blue-100 transition"
                                          title="Pay using Student Wallet"
                                        >
                                          <Wallet size={10} /> Wallet
                                        </button>
                                      </>
                                    )}
                                    <button onClick={() => handleEditFee(f)} className="text-slate-400 hover:text-blue-600 p-1.5 rounded-lg transition"><Edit2 size={13} /></button>
                                    <button onClick={() => handleDeleteFee(f)} className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg transition"><Trash2 size={13} /></button>
                                  </div>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    )
                  )}

                  {/* WALLETS TABLE */}
                  {subTab === 'wallets' && (
                    students.length === 0 ? (
                      <Empty icon={User} title="No students registered" hint="Issue student profiles to set up virtual wallets." />
                    ) : (
                      <table className="w-full text-left border-collapse text-xs">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                            <th className="px-4 py-3">Student Name</th>
                            <th className="px-4 py-3">Admission ID</th>
                            <th className="px-4 py-3 text-right">Available Balance</th>
                            <th className="px-4 py-3 text-center">Last Updated</th>
                            <th className="px-4 py-3 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                          {students.map((s) => {
                            const wallet = wallets[s.id];
                            const bal = wallet?.balance || 0;
                            return (
                              <tr key={s.id} className="hover:bg-slate-50/50 transition">
                                <td className="px-4 py-3 font-bold text-slate-900">{s.firstName} {s.lastName || ''}</td>
                                <td className="px-4 py-3 font-mono text-slate-500">{s.admissionNo}</td>
                                <td className="px-4 py-3 text-right font-mono font-bold text-blue-700 text-sm">
                                  ${bal.toLocaleString(undefined, {minimumFractionDigits: 2})}
                                </td>
                                <td className="px-4 py-3 text-center text-slate-400">
                                  {wallet?.updatedAt ? new Date(wallet.updatedAt).toLocaleDateString() : 'Never'}
                                </td>
                                <td className="px-4 py-3 text-right">
                                  <div className="flex justify-end gap-2">
                                    <button 
                                      onClick={() => { setWalletForm({...walletForm, studentId: s.id, operation: 'CREDIT'}); }}
                                      className="px-2.5 py-1 text-[10px] font-bold border border-slate-200 rounded hover:bg-slate-50 transition"
                                    >
                                      (+) Deposit
                                    </button>
                                    <button 
                                      onClick={() => { setWalletForm({...walletForm, studentId: s.id, operation: 'DEBIT'}); }}
                                      className="px-2.5 py-1 text-[10px] font-bold border border-slate-200 rounded hover:bg-slate-50 transition text-rose-600"
                                    >
                                      (-) Debit
                                    </button>
                                    <button 
                                      onClick={() => viewAuditLog(s.id)}
                                      className="flex items-center gap-1 px-2.5 py-1 text-[10px] font-bold bg-slate-100 hover:bg-slate-200 rounded transition"
                                      title="Audit Transaction Ledger"
                                    >
                                      <History size={11} /> Audit Ledger
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    )
                  )}
                </>
              )}
            </div>
          </div>

          {/* RIGHT PANEL: ACTION / SETUP FORM PANEL */}
          <div className="xl:col-span-4">
            {/* FEES LEDGER FORM */}
            {subTab === 'fees' && (
              <Card
                title={editingFee ? "Modify Fee Dues" : "Issue Invoice"}
                subtitle={editingFee ? `Modifying fee billing parameters.` : "Generate a new invoice dues on a student profile."}
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students found" hint="Please enroll students first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Select Student *">
                      <Select 
                        value={feeForm.studentId}
                        onChange={(e) => setFeeForm({...feeForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.firstName} {s.lastName || ''} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Fee Category *">
                        <Select 
                          value={feeForm.type}
                          onChange={(e) => setFeeForm({...feeForm, type: e.target.value})}
                        >
                          <option value="TUITION">Tuition Dues</option>
                          <option value="HOSTEL">Hostel Dues</option>
                          <option value="TRANSPORT">Bus & Transport</option>
                          <option value="MESS">Mess & Food</option>
                          <option value="LIBRARY">Library Fee</option>
                          <option value="OTHER">Other Dues</option>
                        </Select>
                      </Field>
                      <Field label="Due Date *">
                        <Input 
                          type="date"
                          value={feeForm.dueDate}
                          onChange={(e) => setFeeForm({...feeForm, dueDate: e.target.value})}
                        />
                      </Field>
                    </div>

                    <Field label="Invoice Dues Amount ($) *">
                      <Input 
                        type="number"
                        value={feeForm.amount}
                        onChange={(e) => setFeeForm({...feeForm, amount: e.target.value})}
                        placeholder="e.g. 1200"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                      {editingFee && <Button variant="default" onClick={handleCancelFeeEdit}>Cancel</Button>}
                      <Button variant="primary" onClick={submitFee} disabled={busy || !feeForm.studentId || !feeForm.amount}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Plus size={14} />}
                        {editingFee ? 'Save Invoice' : 'Issue Invoice'}
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* STUDENT WALLETS OPERATION FORM */}
            {subTab === 'wallets' && (
              <Card
                title="Wallet Operation"
                subtitle="Deposit or debit credits to student virtual lunch/fine wallets."
              >
                {students.length === 0 ? (
                  <Empty icon={User} title="No students found" hint="Please enroll students first." />
                ) : (
                  <div className="space-y-4">
                    <Field label="Target Student *">
                      <Select 
                        value={walletForm.studentId}
                        onChange={(e) => setWalletForm({...walletForm, studentId: e.target.value})}
                      >
                        {students.map(s => (
                          <option key={s.id} value={s.id}>{s.firstName} {s.lastName || ''} ({s.admissionNo})</option>
                        ))}
                      </Select>
                    </Field>

                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Action Type *">
                        <Select 
                          value={walletForm.operation}
                          onChange={(e) => setWalletForm({...walletForm, operation: e.target.value})}
                        >
                          <option value="CREDIT">Deposit (+)</option>
                          <option value="DEBIT">Withdraw (-)</option>
                        </Select>
                      </Field>
                      <Field label="Amount ($) *">
                        <Input 
                          type="number"
                          value={walletForm.amount}
                          onChange={(e) => setWalletForm({...walletForm, amount: e.target.value})}
                          placeholder="e.g. 50"
                        />
                      </Field>
                    </div>

                    <Field label="Transaction Reference / Remarks *">
                      <Input 
                        value={walletForm.remarks}
                        onChange={(e) => setWalletForm({...walletForm, remarks: e.target.value})}
                        placeholder="e.g. Parent deposit, Canteen purchase"
                      />
                    </Field>

                    <div className="pt-3 border-t border-slate-100 flex justify-end">
                      <Button variant="primary" onClick={submitWalletOperation} disabled={busy || !walletForm.studentId || !walletForm.amount || !walletForm.remarks}>
                        {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />}
                        Post Transaction
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            )}
          </div>
        </div>
      </div>

      {/* POPUP MODAL: CASH PAYMENT DIALOG */}
      {showPayModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Record Cash Receipt</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">Recording payment for {getStudentName(showPayModal.studentId)}</p>
              </div>
              <button onClick={() => setShowPayModal(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 space-y-4">
              <div className="grid grid-cols-2 text-xs border border-slate-100 bg-slate-50/50 rounded-lg p-3">
                <div>
                  <span className="text-slate-400 font-medium">Category:</span>
                  <div className="font-bold text-slate-700 mt-0.5">{showPayModal.type}</div>
                </div>
                <div>
                  <span className="text-slate-400 font-medium">Remaining Due:</span>
                  <div className="font-bold text-rose-700 mt-0.5 font-mono">${(showPayModal.amount - (showPayModal.paidAmount || 0)).toLocaleString()}</div>
                </div>
              </div>

              <Field label="Cash Amount Paid ($) *">
                <Input 
                  type="number"
                  value={payAmount}
                  onChange={(e) => setPayAmount(e.target.value)}
                  placeholder="e.g. 500"
                />
              </Field>

              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <Button variant="default" onClick={() => setShowPayModal(null)}>Cancel</Button>
                <Button variant="primary" onClick={handleCashPayment} disabled={busy || !payAmount}>
                  {busy ? <RefreshCw className="animate-spin" size={14} /> : <Check size={14} />}
                  Complete Payment
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* POPUP MODAL: WALLET TRANSACTION HISTORY AUDIT */}
      {selectedAuditStudent && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[85vh]">
            <header className="px-5 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Wallet Transaction Ledger Audit</h4>
                <p className="text-[10px] text-slate-400 mt-0.5">Auditing profile: {getStudentName(selectedAuditStudent)}</p>
              </div>
              <button onClick={() => setSelectedAuditStudent(null)} className="text-slate-400 hover:text-slate-600"><X size={16} /></button>
            </header>
            <div className="p-5 flex-1 overflow-y-auto min-h-0">
              {auditTransactions.length === 0 ? (
                <Empty icon={History} title="No transaction records found" hint="Wallet deposits and debits ledger will display here." />
              ) : (
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
                      <th className="px-4 py-3">Transaction Date</th>
                      <th className="px-4 py-3 text-center">Type</th>
                      <th className="px-4 py-3 text-right">Amount</th>
                      <th className="px-4 py-3">Reference / Remarks</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                    {auditTransactions.map((tx) => (
                      <tr key={tx.id} className="hover:bg-slate-50/50 transition">
                        <td className="px-4 py-3 text-slate-400">
                          {tx.createdAt ? new Date(tx.createdAt).toLocaleString() : '—'}
                        </td>
                        <td className="px-4 py-3 text-center">
                          <span className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full text-[10px] font-bold ${tx.type === 'CREDIT' ? 'bg-green-50 text-green-700' : 'bg-rose-50 text-rose-700'}`}>
                            {tx.type === 'CREDIT' ? <ArrowUpRight size={10} /> : <ArrowDownRight size={10} />}
                            {tx.type}
                          </span>
                        </td>
                        <td className={`px-4 py-3 text-right font-mono font-bold ${tx.type === 'CREDIT' ? 'text-green-700' : 'text-rose-700'}`}>
                          ${tx.amount.toLocaleString(undefined, {minimumFractionDigits: 2})}
                        </td>
                        <td className="px-4 py-3 text-slate-600 italic select-all">
                          {tx.remarks || '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <footer className="px-5 py-3 border-t border-slate-100 bg-slate-50 shrink-0 flex justify-end">
              <Button variant="default" onClick={() => setSelectedAuditStudent(null)}>Close Audit</Button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );
}
