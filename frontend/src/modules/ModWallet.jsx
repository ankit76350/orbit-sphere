/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import {
  getStudents,
  getTransactions,
  creditWallet,
  deductWallet
} from "../storage";
import { api } from "../api";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { ArrowDownLeft, Plus, Bookmark, HelpCircle } from "lucide-react";
export default function ModWallet({ user }) {
  const { addToast } = useToast();
  const [students, setStudents] = useState(() => getStudents());
  const [transactions, setTransactions] = useState(() => getTransactions());
  const [isTopupOpen, setIsTopupOpen] = useState(false);
  const [isDeductOpen, setIsDeductOpen] = useState(false);
  const [topupStudentId, setTopupStudentId] = useState("student-1");
  const [topupAmount, setTopupAmount] = useState("50");
  const [topupRemarks, setTopupRemarks] = useState("Monthly pocket money allowance");
  const [deductStudentId, setDeductStudentId] = useState("student-1");
  const [deductAmount, setDeductAmount] = useState("15");
  const [deductCategory, setDeductCategory] = useState("Store Purchase");
  const [deductRemarks, setDeductRemarks] = useState("Purchased secondary compass geometry set");
  const handleTopupSubmit = (e) => {
    e.preventDefault();
    const amt = parseFloat(topupAmount);
    if (!amt || amt <= 0) {
      addToast("Error", "Input valid amount larger than 0", "error");
      return;
    }
    const success = creditWallet(topupStudentId, amt, topupRemarks, user.name, user.role);
    api.creditWallet(topupStudentId, amt, topupRemarks).catch(() => {});
    if (success) {
      setStudents(getStudents());
      setTransactions(getTransactions());
      setIsTopupOpen(false);
      addToast("Success", `Credited $${amt} directly to pocket wallet balance`);
    } else {
      addToast("Error", "Target student roster not found", "error");
    }
  };
  const handleDeductSubmit = (e) => {
    e.preventDefault();
    const amt = parseFloat(deductAmount);
    if (!amt || amt <= 0) {
      addToast("Error", "Input valid deduction quantity", "error");
      return;
    }
    const success = deductWallet(deductStudentId, amt, deductCategory, deductRemarks, user.name, user.role);
    api.debitWallet(deductStudentId, amt, deductRemarks).catch(() => {});
    if (success) {
      setStudents(getStudents());
      setTransactions(getTransactions());
      setIsDeductOpen(false);
      addToast("Success", `Deducted $${amt} from student wallet. Logs validated.`);
    } else {
      addToast("Failed", "Purchase failed: Check if student has sufficient wallet balance", "error");
    }
  };
  return <div className="space-y-6">

      {
    /* Roster overview row */
  }
      <div className="bg-white p-5 rounded-3xl border border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight">Active Student Wallets</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Supervise automated campus RFID pocket balances. Deduct store material lists or inject parent deposits instantly.
          </p>
        </div>
        <div className="flex gap-2.5 w-full md:w-auto shrink-0">
          <Button
    onClick={() => setIsDeductOpen(true)}
    variant="outline"
    className="flex-1 md:flex-none flex gap-2 items-center text-xs py-2.5 cursor-pointer text-rose-600 border-rose-200"
  >
            <ArrowDownLeft className="h-4.5 w-4.5 text-rose-500" /> Store / Fine Debit
          </Button>
          <Button
    onClick={() => setIsTopupOpen(true)}
    className="flex-1 md:flex-none flex gap-2 items-center text-xs py-2.5 bg-indigo-600 hover:bg-indigo-700 cursor-pointer"
  >
            <Plus className="h-4.5 w-4.5" /> Guardian Top-up
          </Button>
        </div>
      </div>

      {
    /* Main ledger grid */
  }
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {
    /* Left Column: Top Lists */
  }
        <div className="lg:col-span-2 bg-white border border-slate-100 p-6 rounded-3xl space-y-4">
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Active Balance Ledger</h3>
          
          <div className="overflow-x-auto border border-slate-100 rounded-2xl">
            <table className="w-full text-xs font-semibold text-slate-700 text-left">
              <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400">
                <tr>
                  <th className="p-3">Scholar Name</th>
                  <th className="p-3">AdNo</th>
                  <th className="p-3">Designation</th>
                  <th className="p-3">Wallet balance</th>
                  <th className="p-3 text-center">Roster Code</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {students.slice(0, 10).map((st) => <tr key={st.id}>
                    <td className="p-3 font-extrabold text-slate-850">{st.name}</td>
                    <td className="p-3 font-mono text-[10px] text-slate-400">{st.admissionNumber}</td>
                    <td className="p-3">{st.grade}</td>
                    <td className="p-3">
                      <span className="font-extrabold text-emerald-600">${st.walletBalance}</span>
                    </td>
                    <td className="p-3 text-center">
                      <Badge variant="info">RFID {st.admissionNumber.replace("STJ", "CHIP")}</Badge>
                    </td>
                  </tr>)}
              </tbody>
            </table>
          </div>
          <p className="text-[10px] text-slate-400 italic">Showing top 10 boarding student balances. Integrate QR scans to lookup matching indices.</p>
        </div>

        {
    /* Right Column: Visual RFID mock */
  }
        <div className="space-y-6">
          
          <div className="bg-slate-900 text-white rounded-3xl p-6 relative overflow-hidden flex flex-col justify-between h-[230px] shadow-lg border border-slate-850">
            <div className="absolute right-0 bottom-0 -mb-12 -mr-12 h-36 w-36 bg-emerald-500 rounded-full blur-3xl opacity-25 pointer-events-none" />
            
            <div className="flex justify-between items-start">
              <div>
                <p className="text-[9px] uppercase tracking-wider text-indigo-300 font-extrabold">ST. JUDE BOARDS ERP</p>
                <h4 className="text-sm font-black text-white mt-1.5 leading-none">RFID Secure Sandbox chip</h4>
              </div>
              <div className="h-9 w-9 bg-white/15 rounded-xl border border-white/10 flex items-center justify-center">
                <Bookmark className="h-5 w-5 text-indigo-300 animate-pulse" />
              </div>
            </div>

            <div className="space-y-0.5 text-left pt-6 select-all font-mono">
              <p className="text-xl tracking-widest font-black text-rose-300">•••• •••• •••• 1024</p>
              <p className="text-[8px] uppercase tracking-wider text-slate-500 font-extrabold mt-1">HOLDER CHIP VERIFICATION</p>
              <p className="text-xs font-bold font-sans text-slate-300">{students[0]?.name || "Edward Smith"}</p>
            </div>

            <div className="flex justify-between items-center mt-3 pt-3 border-t border-white/5 text-[11px] font-bold">
              <span className="text-slate-400">Sandbox RFID Active Rate</span>
              <span className="text-emerald-400 animate-pulse font-extrabold">● SECURE CHIP ON</span>
            </div>
          </div>

          <div className="bg-slate-50 border border-slate-100 p-5 rounded-3xl space-y-4">
            <h4 className="text-xs font-bold text-slate-700 uppercase tracking-widest flex gap-2 items-center">
              <HelpCircle className="h-4.5 w-4.5 text-indigo-500" /> Pocket Rule Manual
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed font-semibold">
              Monthly pocket spend budget is capped by the student index grade level. Incident fines warnings can deduct pocket money automatically to encourage dorm Curfew obedience.
            </p>
          </div>

        </div>

      </div>

      <div className="bg-white border border-slate-100 p-6 rounded-3xl">
        <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Live Ledger transactions</h3>
        <div className="overflow-x-auto border border-slate-100 rounded-2xl">
          <table className="w-full text-xs font-semibold text-slate-700 text-left">
            <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] font-bold text-slate-400">
              <tr>
                <th className="p-3">Reference ID</th>
                <th className="p-3">Student</th>
                <th className="p-3">Date</th>
                <th className="p-3">Activity Class</th>
                <th className="p-3">Deduction amount</th>
                <th className="p-3 font-medium">Remarks</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {transactions.slice(0, 15).map((tx) => <tr key={tx.id}>
                  <td className="p-3 font-mono text-[10px] text-slate-400 font-bold uppercase">{tx.id}</td>
                  <td className="p-3 font-extrabold text-slate-800">{tx.studentName}</td>
                  <td className="p-3 text-slate-400 font-bold">{tx.date}</td>
                  <td className="p-3">
                    <Badge variant={tx.type === "Credit" ? "success" : "danger"}>
                      {tx.type === "Credit" ? "Deposit Credit" : tx.category}
                    </Badge>
                  </td>
                  <td className="p-3">
                    <span className={tx.type === "Credit" ? "text-emerald-600 font-extrabold" : "text-slate-800 font-bold"}>
                      {tx.type === "Credit" ? "+" : "-"}${tx.amount}
                    </span>
                  </td>
                  <td className="p-3 text-slate-450 italic max-w-sm">{tx.remarks}</td>
                </tr>)}
            </tbody>
          </table>
        </div>
      </div>

      {
    /* Modal: Guardian Topup deposit */
  }
      <Dialog isOpen={isTopupOpen} onClose={() => setIsTopupOpen(false)} title="Inject Pocket Money Topup Card">
        <form onSubmit={handleTopupSubmit} className="space-y-4 pt-1">
          <Select
    label="Associate Boarder Student"
    options={students.slice(0, 45).map((s) => ({ label: `${s.name} (${s.admissionNumber}) - Current: $${s.walletBalance}`, value: s.id }))}
    value={topupStudentId}
    onChange={(e) => setTopupStudentId(e.target.value)}
  />
          <Input
    label="Topup Amount ($ USD)"
    type="number"
    value={topupAmount}
    onChange={(e) => setTopupAmount(e.target.value)}
    placeholder="50"
    required
  />
          <Input
    label="Remarks description"
    value={topupRemarks}
    onChange={(e) => setTopupRemarks(e.target.value)}
    placeholder="Monthly allocation, birthday prize..."
    required
  />
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsTopupOpen(false)}>Cancel Deposit</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900">Credit Pocket Balance</Button>
          </div>
        </form>
      </Dialog>

      {
    /* Modal: Store Debit checkout */
  }
      <Dialog isOpen={isDeductOpen} onClose={() => setIsDeductOpen(false)} title="Debit Student Purse Checkout Drawer">
        <form onSubmit={handleDeductSubmit} className="space-y-4 pt-1">
          <Select
    label="Associate Boarder Student"
    options={students.slice(0, 45).map((s) => ({ label: `${s.name} (${s.admissionNumber}) - Available: $${s.walletBalance}`, value: s.id }))}
    value={deductStudentId}
    onChange={(e) => setDeductStudentId(e.target.value)}
  />
          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Deduction Amount ($ USD)"
    type="number"
    value={deductAmount}
    onChange={(e) => setDeductAmount(e.target.value)}
    placeholder="15"
    required
  />
            <Select
    label="Deduction Category"
    options={[
      { label: "Store Buy Checkout", value: "Store Purchase" },
      { label: "Disciplinary Fine Charge", value: "Fine Deduction" }
    ]}
    value={deductCategory}
    onChange={(e) => setDeductCategory(e.target.value)}
  />
          </div>
          <Input
    label="Remarks explanation log"
    value={deductRemarks}
    onChange={(e) => setDeductRemarks(e.target.value)}
    placeholder="Uniform crest replacements, geometry notebooks..."
    required
  />
          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsDeductOpen(false)}>Cancel Debit</Button>
            <Button type="submit" className="bg-indigo-600 hover:bg-indigo-700">Deduct & Register Transactions</Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
