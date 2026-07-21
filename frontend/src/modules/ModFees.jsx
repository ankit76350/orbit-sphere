/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from "react";
import { getFees, payInvoice } from "../storage";
import { api } from "../api";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { DollarSign, FileText, CheckCircle, Download, Eye } from "lucide-react";
export default function ModFees({ user }) {
  const { addToast } = useToast();
  const [invoices, setInvoices] = useState(() => getFees());
  const [filterStatus, setFilterStatus] = useState("All");
  const [search, setSearch] = useState("");
  const [activeReceipt, setActiveReceipt] = useState(null);
  const [isPayOpen, setIsPayOpen] = useState(false);
  const [payTargetInvoice, setPayTargetInvoice] = useState(null);
  const [payAmount, setPayAmount] = useState("2250");
  const [payMethod, setPayMethod] = useState("Bank Transfer");

  useEffect(() => {
    let isMounted = true;
    api.getFees().then((res) => {
      if (isMounted && Array.isArray(res) && res.length > 0) {
        setInvoices(res);
      }
    }).catch(() => {});
    return () => { isMounted = false; };
  }, []);

  const handleManualPay = (e) => {
    e.preventDefault();
    if (!payTargetInvoice) return;
    const amt = parseFloat(payAmount);
    if (!amt || amt <= 0) {
      addToast("Error", "Input valid payment amount", "error");
      return;
    }
    const maxAcceptable = payTargetInvoice.amount - payTargetInvoice.paidAmount;
    if (amt > maxAcceptable) {
      addToast("Validation", `Payment exceeds remaining balance error ($${maxAcceptable})`, "warning");
      return;
    }
    const success = payInvoice(payTargetInvoice.id, amt, payMethod, user.name, user.role);
    api.recordFeePayment(payTargetInvoice.id, {
      amount: amt,
      paymentMode: payMethod,
      remarks: "Fee Payment",
      collectedBy: user ? user.name : "Admin"
    }).catch(() => {});
    if (success) {
      const freshInvoices = getFees();
      setInvoices(freshInvoices);
      setIsPayOpen(false);
      addToast("Success", `Registered billing payment of $${amt} on ledger`);
    } else {
      addToast("Error", "Durable invoice lookup crash", "error");
    }
  };
  const handleTriggerPayBox = (inv) => {
    setPayTargetInvoice(inv);
    const balance = inv.amount - inv.paidAmount;
    setPayAmount(String(balance));
    setIsPayOpen(true);
  };
  const filteredInvoices = invoices.filter((inv) => {
    const matchStatus = filterStatus === "All" || inv.status === filterStatus;
    const matchSearch = inv.studentName.toLowerCase().includes(search.toLowerCase()) || inv.feeType.toLowerCase().includes(search.toLowerCase());
    return matchStatus && matchSearch;
  });
  const totalInvoiced = invoices.reduce((sum, i) => sum + i.amount, 0);
  const totalCollected = invoices.reduce((sum, i) => sum + i.paidAmount, 0);
  const totalOutstanding = totalInvoiced - totalCollected;
  return <div className="space-y-6">

      {
    /* Analytics dashboard banner */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-slate-900 text-slate-100 rounded-3xl p-5 flex justify-between items-center relative overflow-hidden border border-slate-850">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-indigo-300">TOTAL TERM INVOICED</span>
            <h4 className="text-2xl font-black text-white mt-1">${totalInvoiced.toLocaleString()} USD</h4>
            <p className="text-xs text-slate-450 mt-1">Fall / Spring session quotas</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-indigo-300">
            <FileText className="h-5.5 w-5.5" />
          </div>
        </div>

        <div className="bg-emerald-950 text-emerald-100 rounded-3xl p-5 flex justify-between items-center relative overflow-hidden">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-emerald-300">COLLECTED REVENUE REGISTERED</span>
            <h4 className="text-2xl font-black text-white mt-1">${totalCollected.toLocaleString()} USD</h4>
            <p className="text-xs text-emerald-200 mt-1">
              {Math.round(totalCollected / totalInvoiced * 100)}% Collection clearance rate
            </p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-emerald-300">
            <DollarSign className="h-5.5 w-5.5" />
          </div>
        </div>

        <div className="bg-rose-950 text-rose-100 rounded-3xl p-5 flex justify-between items-center relative overflow-hidden">
          <div>
            <span className="text-[10px] uppercase font-extrabold text-rose-300">OUTSTANDING UNPAID COHORTS</span>
            <h4 className="text-2xl font-black text-white mt-1">${totalOutstanding.toLocaleString()} USD</h4>
            <p className="text-xs text-rose-200 mt-1">Under strict finance reminders</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-rose-300">
            <CheckCircle className="h-5.5 w-5.5" />
          </div>
        </div>
      </div>

      {
    /* Control Area */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-105">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight">Fee Ledger Books</h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">Filter due statements, execute ledger installments and render premium receipt bills PDF.</p>
        </div>
        
        <div className="flex flex-wrap gap-2.5 w-full sm:w-auto">
          <input
    type="text"
    placeholder="Search student or item..."
    value={search}
    onChange={(e) => setSearch(e.target.value)}
    className="bg-slate-50 border border-slate-200 text-xs rounded-xl px-4 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-505 font-medium flex-1 sm:flex-none sm:w-48"
  />
          <Select
    options={[
      { label: "All Invoices", value: "All" },
      { label: "Unpaid Dues", value: "Unpaid" },
      { label: "Partially Cleared", value: "Partially Paid" },
      { label: "Fully Paid Statements", value: "Paid" }
    ]}
    value={filterStatus}
    onChange={(e) => setFilterStatus(e.target.value)}
    className="text-xs py-2 h-9 rounded-xl max-w-[150px]"
  />
        </div>
      </div>

      {
    /* Table structure */
  }
      <div className="bg-white border border-slate-100 rounded-3xl p-6">
        <div className="overflow-x-auto rounded-2xl border border-slate-100">
          <table className="w-full text-xs text-slate-705 text-left font-semibold">
            <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 font-extrabold tracking-wider">
              <tr>
                <th className="p-4">Student associated</th>
                <th className="p-4">Billing Item Type</th>
                <th className="p-4">Grade level</th>
                <th className="p-4">Invoiced quota</th>
                <th className="p-4">Paid quantity</th>
                <th className="p-4">Remaining dues</th>
                <th className="p-4">Billing status</th>
                <th className="p-4 text-center">Record Desk</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {filteredInvoices.slice(0, 15).map((inv) => {
    const due = inv.amount - inv.paidAmount;
    return <tr key={inv.id}>
                    <td className="p-4 font-extrabold text-slate-850">{inv.studentName}</td>
                    <td className="p-4 font-bold text-slate-500">{inv.feeType}</td>
                    <td className="p-4">{inv.grade}</td>
                    <td className="p-4">${inv.amount}</td>
                    <td className="p-4 text-emerald-600 font-extrabold">${inv.paidAmount}</td>
                    <td className="p-4 text-rose-500">${due}</td>
                    <td className="p-4">
                      <Badge variant={inv.status === "Paid" ? "success" : inv.status === "Partially Paid" ? "warning" : "danger"}>
                        {inv.status}
                      </Badge>
                    </td>
                    <td className="p-4 flex gap-2 justify-center items-center">
                      <button
      onClick={() => setActiveReceipt(inv)}
      className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-slate-900 transition flex items-center justify-center border border-slate-200 cursor-pointer"
      title="Display Receipt Chit"
    >
                        <Eye className="h-4 w-4" />
                      </button>

                      {due > 0 && <button
      onClick={() => handleTriggerPayBox(inv)}
      className="bg-indigo-600 text-white font-bold p-1 px-2.5 rounded-lg hover:bg-indigo-700 text-[10px] uppercase transition cursor-pointer"
    >
                          Manual Pay
                        </button>}
                    </td>
                  </tr>;
  })}
            </tbody>
          </table>
        </div>
        <p className="text-[10px] text-slate-400 italic mt-3 leading-normal">
          Showing top invoices. Deducting and clearing billing updates are immediately synced with pocket ledger databases in LocalStorage.
        </p>
      </div>

      {
    /* Drawer Dialog: manual partial invoices pay */
  }
      <Dialog isOpen={isPayOpen} onClose={() => setIsPayOpen(false)} title="Execute Billing Statement Payment">
        <form onSubmit={handleManualPay} className="space-y-4 pt-1">
          {payTargetInvoice && <div className="bg-slate-50 p-4 rounded-xl border border-slate-150 space-y-1.5 text-xs text-slate-600 font-semibold leading-relaxed">
              <p>Invoiced Target: <span className="text-slate-800 font-bold">{payTargetInvoice.studentName}</span></p>
              <p>Obligation Section: <span className="text-slate-850 font-bold">{payTargetInvoice.feeType}</span></p>
              <p>Term Total: <span className="text-slate-850 font-extrabold">${payTargetInvoice.amount} USD</span></p>
              <p>Cleared Balance: <span className="text-emerald-700 font-bold">${payTargetInvoice.paidAmount} USD</span></p>
              <p className="text-rose-500 font-bold text-xs mt-2.5">Pending Remainder: ${payTargetInvoice.amount - payTargetInvoice.paidAmount} USD</p>
            </div>}

          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Payment Amount ($ USD)"
    type="number"
    value={payAmount}
    onChange={(e) => setPayAmount(e.target.value)}
    required
  />
            <Select
    label="Instrument Option"
    options={[
      { label: "Cash Desk", value: "Cash" },
      { label: "Card Swipe Reader", value: "Card" },
      { label: "Bank Direct Transfer", value: "Bank Transfer" },
      { label: "Online Parent Purse", value: "Online Wallet" }
    ]}
    value={payMethod}
    onChange={(e) => setPayMethod(e.target.value)}
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsPayOpen(false)}>Cancel Transaction</Button>
            <Button type="submit" className="bg-indigo-600 hover:bg-slate-900 font-extrabold">Finalize Cash Receipt</Button>
          </div>
        </form>
      </Dialog>

      {
    /* Dialog: Receipt slip visual */
  }
      <Dialog isOpen={!!activeReceipt} onClose={() => setActiveReceipt(null)} title="Official Term Billing Receipt" maxWidth="max-w-md">
        {activeReceipt && <div className="space-y-6 pt-1 select-text">
            
            {
    /* Header portion looking premium */
  }
            <div className="border-b-2 border-dashed border-slate-200 pb-5 text-center relative">
              <div className="h-10 w-10 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-2.5">
                <FileText className="h-6 w-6 animate-pulse" />
              </div>
              <h4 className="text-base font-black text-slate-800 uppercase tracking-widest leading-none">St. Jude Boarding</h4>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 mt-1 font-bold">Official Registrar Treasury Chit</p>
              <div className="absolute left-0 -ml-6 bottom-0 translate-y-3.5 h-3 w-3 bg-white rounded-full border border-slate-200" />
              <div className="absolute right-0 -mr-6 bottom-0 translate-y-3.5 h-3 w-3 bg-white rounded-full border border-slate-200" />
            </div>

            {
    /* Receipt metrics */
  }
            <div className="space-y-3.5 text-xs font-semibold text-slate-700">
              <div className="flex justify-between">
                <span className="text-slate-400">Student associated</span>
                <span className="text-slate-900 font-extrabold">{activeReceipt.studentName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Class Grade Index</span>
                <span>{activeReceipt.grade}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Session Interval</span>
                <span>Fall Term 2026</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Charge Entity Type</span>
                <span className="text-slate-900 font-bold">{activeReceipt.feeType}</span>
              </div>

              <div className="border-t border-slate-100 pt-3 flex justify-between text-slate-800">
                <span>Invoiced total amount</span>
                <span className="font-extrabold">${activeReceipt.amount}</span>
              </div>
              <div className="flex justify-between text-emerald-600">
                <span>Cleared collected amount</span>
                <span className="font-black">${activeReceipt.paidAmount}</span>
              </div>

              <div className="pt-3.5 border-t border-dashed border-slate-200 flex justify-between items-center text-slate-800">
                <span className="text-slate-450 uppercase text-[9px] tracking-wide font-black">STATEMENT LEDGER STATUS</span>
                <Badge variant={activeReceipt.status === "Paid" ? "success" : activeReceipt.status === "Partially Paid" ? "warning" : "danger"}>
                  {activeReceipt.status}
                </Badge>
              </div>
            </div>

            {
    /* Print button mock */
  }
            <div className="pt-4 border-t border-slate-100 flex gap-2 justify-end">
              <Button
    variant="outline"
    className="text-xs flex gap-1.5 items-center py-2 shrink-0 cursor-pointer"
    onClick={() => {
      addToast("Dispatched", "Simulated hardware receipt stream triggered", "success");
      setActiveReceipt(null);
    }}
  >
                <Download className="h-4 w-4" /> Download PDF
              </Button>
              <Button variant="ghost" onClick={() => setActiveReceipt(null)} className="text-xs">
                Close Slip
              </Button>
            </div>

          </div>}
      </Dialog>

    </div>;
}
