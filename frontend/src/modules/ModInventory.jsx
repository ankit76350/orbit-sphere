/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import {
  getInventory,
  saveInventory,
  getStudents,
  deductWallet,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import { Box, ShoppingCart, AlertCircle, Plus, Sparkles, Building } from "lucide-react";
export default function ModInventory({ user }) {
  const { addToast } = useToast();
  const [items, setItems] = useState(() => getInventory());
  const [students, setStudents] = useState(() => getStudents());
  const [isNewItemOpen, setIsNewItemOpen] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [itemName, setItemName] = useState("");
  const [category, setCategory] = useState("Uniform");
  const [stock, setStock] = useState("50");
  const [minAlertStock, setMinAlertStock] = useState("15");
  const [unitPrice, setUnitPrice] = useState("20");
  const [supplier, setSupplier] = useState("Royal Garments Inc.");
  const [supplierContact, setSupplierContact] = useState("contracts@royalgarments.com");
  const [chkStudentId, setChkStudentId] = useState("student-1");
  const [chkItemId, setChkItemId] = useState("inv-item-1");
  const [chkQty, setChkQty] = useState("1");
  const handleCreateItem = (e) => {
    e.preventDefault();
    if (!itemName || !supplier) {
      addToast("Error", "Ensure item name and supplier description are written", "error");
      return;
    }
    const newItem = {
      id: `inv-item-${Date.now()}`,
      itemName,
      category,
      stock: parseInt(stock) || 0,
      minAlertStock: parseInt(minAlertStock) || 0,
      unitPrice: parseFloat(unitPrice) || 0,
      supplier,
      supplierContact
    };
    const updated = [...items, newItem];
    setItems(updated);
    saveInventory(updated);
    logAction(user.id, user.name, user.role, "Inventory Item Created", `Registered stock entity: ${itemName} ($${unitPrice})`);
    addToast("Success", `Registered ${itemName} in store catalogs!`);
    setIsNewItemOpen(false);
    setItemName("");
  };
  const handleCheckoutSubmit = (e) => {
    e.preventDefault();
    const qtyVal = parseInt(chkQty) || 1;
    if (qtyVal <= 0) return;
    const cleanItems = getInventory();
    const itemIdx = cleanItems.findIndex((i) => i.id === chkItemId);
    if (itemIdx === -1) {
      addToast("Error", "Target item absent", "error");
      return;
    }
    const storeItem = cleanItems[itemIdx];
    if (storeItem.stock < qtyVal) {
      addToast("Out of Stock", `The store has only ${storeItem.stock} units left of this item`, "warning");
      return;
    }
    const totalCost = storeItem.unitPrice * qtyVal;
    const cleanStudents = getStudents();
    const targetStudent = cleanStudents.find((s) => s.id === chkStudentId);
    if (!targetStudent) {
      addToast("Error", "Target student roster not found", "error");
      return;
    }
    if (targetStudent.walletBalance < totalCost) {
      addToast("Checkout Blocked", `Student has only $${targetStudent.walletBalance} remaining in wallet. (Total: $${totalCost})`, "error");
      return;
    }
    const walletPassed = deductWallet(
      chkStudentId,
      totalCost,
      "Store Purchase",
      `Bought ${qtyVal}x ${storeItem.itemName} from Stationery Store`,
      user.name,
      user.role
    );
    if (walletPassed) {
      storeItem.stock -= qtyVal;
      cleanItems[itemIdx] = storeItem;
      setItems(cleanItems);
      saveInventory(cleanItems);
      setStudents(getStudents());
      logAction(
        user.id,
        user.name,
        user.role,
        "Inventory Purchased",
        `Sold ${qtyVal}x ${storeItem.itemName} to student ${targetStudent.name}. Total wallet balance deducted: $${totalCost}`
      );
      addToast("Purchase Completed", `Deducted $${totalCost} from ${targetStudent.name}'s wallet. Stock decreased.`, "success");
      setIsCheckoutOpen(false);
    } else {
      addToast("Error", "Transactional ledger lock failure", "error");
    }
  };
  const lowStockItems = items.filter((i) => i.stock <= i.minAlertStock);
  const totalStockQuantities = items.reduce((sum, i) => sum + i.stock, 0);
  return <div className="space-y-6">

      {
    /* Overview alerts row */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">TOTAL STOCK CATEGORY CATALOG</span>
            <h4 className="text-2xl font-black text-slate-800 mt-1">{items.length} Unique Items</h4>
            <p className="text-xs text-slate-450 mt-1">Sash insignia, blazers & sheets</p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-600 rounded-2xl">
            <Box className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-indigo-900 text-indigo-150 p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">TOTAL STOCK QUANTITY</span>
            <h4 className="text-2xl font-black text-white mt-1">{totalStockQuantities} Units</h4>
            <p className="text-xs text-indigo-200 mt-1">Campus store ledger holdings</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <ShoppingCart className="h-5.5 w-5.5" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">LOW STOCK ALERTS</span>
            <h4 className="text-2xl font-black text-rose-600 mt-1">{lowStockItems.length} Warnings</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Below target restock levels</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl animate-pulse">
            <AlertCircle className="h-6 w-6" />
          </div>
        </div>
      </div>

      {
    /* Alert panels if any are low stock */
  }
      {lowStockItems.length > 0 && <div className="bg-red-50/50 border border-red-200 p-4 rounded-3xl flex gap-3 text-red-950 text-xs">
          <AlertCircle className="h-4.5 w-4.5 text-red-600 shrink-0 mt-0.5" />
          <div className="font-semibold">
            <p className="font-black">Inventory Reorder Warning:</p>
            <p className="text-red-800 mt-1 font-semibold leading-relaxed">
              The following material items are currently critical: {lowStockItems.map((i) => `${i.itemName} (${i.stock} level)`).join(", ")}. Please contact distributors immediately.
            </p>
          </div>
        </div>}

      {
    /* Main Boards actions */
  }
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100">
        <div>
          <h2 className="text-lg font-bold text-slate-800 tracking-tight">Stationery & Uniform Storage</h2>
          <p className="text-xs text-slate-440 font-semibold mt-1">Enlist material stock logs, look up manufacturers, and checkout crest layers directly to scholar pocket RFID cards.</p>
        </div>

        <div className="flex gap-2.5 w-full sm:w-auto shrink-0">
          <Button
    onClick={() => setIsCheckoutOpen(true)}
    variant="outline"
    className="flex-1 sm:flex-none flex gap-2 items-center text-xs py-2.5 text-indigo-700 border-indigo-200 bg-white"
  >
            <ShoppingCart className="h-4.5 w-4.5 text-indigo-505" /> Student Checkout Register
          </Button>
          <Button
    onClick={() => setIsNewItemOpen(true)}
    className="flex-1 sm:flex-none flex gap-2 items-center text-xs py-2.5 bg-slate-900 border border-transparent font-extrabold text-white"
  >
            <Plus className="h-4.5 w-4.5" /> Add Material Item
          </Button>
        </div>
      </div>

      {
    /* Master Inventory Directory */
  }
      <div className="bg-white border border-slate-100 rounded-3xl p-6">
        <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest mb-4">Stock Directory catalog</h3>

        <div className="overflow-x-auto border border-slate-100 rounded-2xl animate-fade-in">
          <table className="w-full text-xs font-semibold text-slate-700 text-left">
            <thead className="bg-slate-50 border-b border-slate-150 uppercase text-[9px] text-slate-400 tracking-wider">
              <tr>
                <th className="p-4">Material description</th>
                <th className="p-4">Category section</th>
                <th className="p-4">In-Store Stock level</th>
                <th className="p-4">Alert threshold</th>
                <th className="p-4">Unit Retail Value</th>
                <th className="p-4">Supplier & Contacts</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {items.map((item) => {
    const low = item.stock <= item.minAlertStock;
    return <tr key={item.id} className={low ? "bg-rose-50/5" : ""}>
                    <td className="p-4 font-extrabold text-slate-850">{item.itemName}</td>
                    <td className="p-4">
                      <Badge variant={item.category === "Uniform" ? "secondary" : "default"}>
                        {item.category}
                      </Badge>
                    </td>
                    <td className="p-4">
                      <span className={`font-black text-xs ${low ? "text-rose-550" : "text-slate-800"}`}>
                        {item.stock} units
                      </span>
                    </td>
                    <td className="p-4 text-slate-400 font-bold">{item.minAlertStock} reorder level</td>
                    <td className="p-4 font-bold text-slate-800">${item.unitPrice}</td>
                    <td className="p-4 uppercase text-[10px] font-bold text-slate-550">
                      <div>
                        <p className="font-extrabold text-slate-700 flex gap-1 items-center">
                          <Building className="h-3 w-3 text-slate-400" /> {item.supplier}
                        </p>
                        <p className="text-[9px] mt-0.5 lowercase text-slate-400">{item.supplierContact}</p>
                      </div>
                    </td>
                  </tr>;
  })}
            </tbody>
          </table>
        </div>
      </div>

      {
    /* Dialog Model: New material item */
  }
      <Dialog isOpen={isNewItemOpen} onClose={() => setIsNewItemOpen(false)} title="Register High Material Entry">
        <form onSubmit={handleCreateItem} className="space-y-4 pt-1">
          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Item Name summary"
    value={itemName}
    onChange={(e) => setItemName(e.target.value)}
    placeholder="e.g. Bed Sheets Gray single, Blazer XL..."
    required
  />
            <Select
    label="Select Stock Category"
    options={[
      { label: "Official Uniform", value: "Uniform" },
      { label: "Books & Stationery", value: "Stationery" },
      { label: "Hostel Beddings", value: "Bedding" },
      { label: "Infirmary First Aid", value: "First Aid" },
      { label: "Toiletries & soap", value: "Toiletries" }
    ]}
    value={category}
    onChange={(e) => setCategory(e.target.value)}
  />
          </div>

          <div className="grid grid-cols-3 gap-4">
            <Input
    label="Current stock level"
    type="number"
    value={stock}
    onChange={(e) => setStock(e.target.value)}
    required
  />
            <Input
    label="Min Alert reorder"
    type="number"
    value={minAlertStock}
    onChange={(e) => setMinAlertStock(e.target.value)}
    required
  />
            <Input
    label="Unit Price ($ USD)"
    type="number"
    step="0.1"
    value={unitPrice}
    onChange={(e) => setUnitPrice(e.target.value)}
    required
  />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
    label="Supplier distributor"
    value={supplier}
    onChange={(e) => setSupplier(e.target.value)}
    placeholder="e.g. Royal Garments Ltd"
    required
  />
            <Input
    label="Supplier business email"
    type="email"
    value={supplierContact}
    onChange={(e) => setSupplierContact(e.target.value)}
    placeholder="contracts@royal.org"
  />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewItemOpen(false)}>Cancel Edit</Button>
            <Button type="submit" className="bg-indigo-650 hover:bg-slate-900">Authorize Stock Placement</Button>
          </div>
        </form>
      </Dialog>

      {
    /* Dialog Model: Student checkout cash register checkout */
  }
      <Dialog isOpen={isCheckoutOpen} onClose={() => setIsCheckoutOpen(false)} title="Student Checkout Registrar Card">
        <form onSubmit={handleCheckoutSubmit} className="space-y-4 pt-1">
          <Select
    label="Designate Scholar Student Client"
    options={students.map((s) => ({ label: `${s.name} (${s.admissionNumber}) - Pocket Purse: $${s.walletBalance}`, value: s.id }))}
    value={chkStudentId}
    onChange={(e) => setChkStudentId(e.target.value)}
  />

          <div className="grid grid-cols-2 gap-4">
            <Select
    label="Select Material Item Buy"
    options={items.map((i) => ({ label: `${i.itemName} (Price: $${i.unitPrice} | Stock: ${i.stock})`, value: i.id }))}
    value={chkItemId}
    onChange={(e) => setChkItemId(e.target.value)}
  />
            <Input
    label="Checkout Quantity multiplier"
    type="number"
    value={chkQty}
    onChange={(e) => setChkQty(e.target.value)}
    min={1}
    required
  />
          </div>

          <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl text-xs text-slate-500 font-semibold leading-relaxed">
            <p className="font-extrabold text-slate-700 flex gap-1 items-center mb-1">
              <Sparkles className="h-4.5 w-4.5 text-indigo-505" /> Telemetry checkout calculations:
            </p>
            <p>Subtracts chosen item quantity stock in LocalStorage. Deducts checkout cost from chosen student's pocket wallet and logs debit reports instantly.</p>
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsCheckoutOpen(false)}>Cancel Buy</Button>
            <Button type="submit" className="bg-indigo-600 hover:bg-slate-900 border border-transparent font-extrabold text-white">
              Deduct Wallet & Trigger Stock Buy
            </Button>
          </div>
        </form>
      </Dialog>

    </div>;
}
