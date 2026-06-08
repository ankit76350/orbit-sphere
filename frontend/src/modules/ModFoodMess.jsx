/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from "react";
import { getMessMenu, saveMessMenu, getStudents, logAction } from "../storage";
import { Button, Input, Dialog, Badge, useToast } from "../components/ui";
import { Coffee, HeartCrack, ChefHat } from "lucide-react";
export default function ModFoodMess({ user }) {
  const { addToast } = useToast();
  const [weeklyMenu, setWeeklyMenu] = useState(() => {
    const cached = getMessMenu();
    return cached.length > 0 ? cached : [
      { day: "Monday", breakfast: "Oatmeal & fruits with warm organic milk", lunch: "Grilled chicken breast with herb basmati rice", dinner: "Lentil shepherd\u2019s pie & raw salads" },
      { day: "Tuesday", breakfast: "Boiled eggs, wheat toast and honey", lunch: "Vegetarian wholewheat pasta with napoli sauce", dinner: "Steamed salmon with baby carrots & potatoes" },
      { day: "Wednesday", breakfast: "Whole grain hot pancakes with syrup", lunch: "Rich beef stroganoff with garlic potato mash", dinner: "Minestrone organic broth with toasted seeds" },
      { day: "Thursday", breakfast: "Mixed organic berry smoothies and brioche", lunch: "Four-bean chilli tacos with rich sour cream", dinner: "Baked turkey slices with brown quinoa" },
      { day: "Friday", breakfast: "Cinnamon wholewheat french toast", lunch: "Tuna salads with spinach & avocado olive mix", dinner: "Mozzarella & pepper cheese flatbreads" }
    ];
  });
  const [editingDay, setEditingDay] = useState(null);
  const [bfText, setBfText] = useState("");
  const [lhText, setLhText] = useState("");
  const [dnText, setDnText] = useState("");
  const [kitchenStocks, setKitchenStocks] = useState([
    { item: "Organic Basmati Rice", qty: "320 kg", restock: "Ok" },
    { item: "Fresh Grade-A Whole Eggs", qty: "480 units", restock: "Ok" },
    { item: "Cold-pressed Extra Virgin Olive Oil", qty: "8 Liters", restock: "Needs Restock" },
    { item: "Lactose-Free Milk bags", qty: "12 Liters", restock: "Ok" }
  ]);
  const [students] = useState(() => getStudents());
  const allergyStudents = students.filter((s) => s.allergies && s.allergies.length > 0);
  const handleUpdateMenu = (e) => {
    e.preventDefault();
    if (!editingDay) return;
    const updated = weeklyMenu.map((m) => {
      if (m.day === editingDay.day) {
        return {
          ...m,
          breakfast: bfText,
          lunch: lhText,
          dinner: dnText
        };
      }
      return m;
    });
    setWeeklyMenu(updated);
    saveMessMenu(updated);
    setEditingDay(null);
    logAction(user.id, user.name, user.role, "Kitchen Menu Adjusted", `Updated food selections program for ${editingDay.day}`);
    addToast("Success", `Menu for ${editingDay.day} updated and announced to boarders.`);
  };
  const handleTriggerEdit = (m) => {
    setEditingDay(m);
    setBfText(m.breakfast);
    setLhText(m.lunch);
    setDnText(m.dinner);
  };
  return <div className="space-y-6">

      {
    /* Grid: Menu & Health warning counts */
  }
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-emerald-950 text-white p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-emerald-300">NUTRITIONAL SYSTEM STATUS</span>
            <h4 className="text-2xl font-black text-white mt-1">Menu Announced</h4>
            <p className="text-xs text-emerald-205 mt-1">Calorie-controlled menu sets</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-emerald-305">
            <ChefHat className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white border border-slate-100 p-5 rounded-3xl flex justify-between items-center">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-400">CRITICAL HEALTH LABELS</span>
            <h4 className="text-2xl font-black text-rose-600 mt-1">{allergyStudents.length} Active Warnings</h4>
            <p className="text-xs text-slate-400 font-semibold mt-1">Peanuts, lactose, soy guidelines</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-2xl">
            <HeartCrack className="h-6 w-6 animate-pulse" />
          </div>
        </div>

        <div className="bg-indigo-900 text-white p-5 rounded-3xl flex justify-between items-center border border-slate-850">
          <div>
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-indigo-300">KITCHEN STOCKS LEDGER</span>
            <h4 className="text-2xl font-black text-white mt-1">4 core items</h4>
            <p className="text-xs text-indigo-200 mt-1">Restocks synced daily</p>
          </div>
          <div className="p-3 bg-white/10 rounded-2xl text-white">
            <Coffee className="h-5.5 w-5.5" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {
    /* Left Column: Weekly Planner */
  }
        <div className="lg:col-span-2 bg-white border border-slate-100 p-6 rounded-3xl space-y-4">
          <h3 className="text-sm font-extrabold text-slate-800 uppercase tracking-widest">Boarder Kitchen weekly Menu</h3>
          
          <div className="space-y-3.5 pt-1.5">
            {weeklyMenu.map((m) => <div key={m.day} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-2">
                <div className="flex justify-between items-center pb-2 border-b border-slate-200">
                  <span className="text-xs font-black text-slate-850">{m.day} Program Plan</span>
                  <button
    onClick={() => handleTriggerEdit(m)}
    className="p-1 px-3 bg-white hover:bg-slate-900 border border-slate-205 text-[10px] text-slate-700 hover:text-white font-bold leading-none rounded-lg uppercase cursor-pointer"
  >
                    Adjust
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5 text-xs font-semibold leading-relaxed pt-1.5">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">🍳 breakfast</span>
                    <p className="text-slate-700 italic">"{m.breakfast}"</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">🥗 Lunch menu</span>
                    <p className="text-slate-800 font-extrabold">"{m.lunch}"</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">🍲 evening Dinner</span>
                    <p className="text-slate-700 italic">"{m.dinner}"</p>
                  </div>
                </div>
              </div>)}
          </div>
        </div>

        {
    /* Right side: Dietary restriction list & Dry food stock list */
  }
        <div className="space-y-6">

          {
    /* Allergy warnings lists */
  }
          <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-3.5">
            <h3 className="text-sm font-extrabold text-rose-600 uppercase tracking-widest flex items-center gap-1.5">
              <HeartCrack className="h-4.5 w-4.5" /> Dietary Health Alerts
            </h3>
            <p className="text-xs text-slate-400 font-semibold leading-relaxed">Medical directives verified by physicians for kitchen servers:</p>

            <div className="space-y-2 pt-1">
              {allergyStudents.map((stud) => <div key={stud.id} className="p-3 bg-slate-50 border border-slate-150 rounded-xl leading-none flex justify-between items-center">
                  <div>
                    <p className="text-xs font-black text-slate-800">{stud.name}</p>
                    <p className="text-[10px] text-slate-400 font-bold mt-1.5">{stud.grade} Class</p>
                  </div>

                  <Badge variant="danger">
                    Allergic to: {stud.allergies || "Vegetarian Option"}
                  </Badge>
                </div>)}
            </div>
          </div>

          {
    /* Food dry inventory stock levels */
  }
          <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-4">
            <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest">Kitchen Staples Stock</h4>
            
            <div className="space-y-2.5">
              {kitchenStocks.map((st, sIdx) => <div key={sIdx} className="text-xs font-semibold flex justify-between items-center">
                  <span className="text-slate-600 font-bold">{st.item}</span>
                  <div className="flex gap-2 items-center font-black">
                    <span className="text-slate-850">{st.qty}</span>
                    <Badge variant={st.restock === "Ok" ? "success" : "warning"}>{st.restock}</Badge>
                  </div>
                </div>)}
            </div>
          </div>

        </div>

      </div>

      {
    /* Adjust Day Menu Box */
  }
      <Dialog isOpen={!!editingDay} onClose={() => setEditingDay(null)} title="Adjust Nutrition Schedules">
        {editingDay && <form onSubmit={handleUpdateMenu} className="space-y-4 pt-1">
            <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-150 text-xs font-bold text-indigo-900 leading-none uppercase tracking-wider">
              Adjusting Meals program for: {editingDay.day}
            </div>

            <div className="space-y-3">
              <Input
    label="Breakfast Menu recipe"
    value={bfText}
    onChange={(e) => setBfText(e.target.value)}
    required
  />
              <Input
    label="Lunch Menu recipe"
    value={lhText}
    onChange={(e) => setLhText(e.target.value)}
    required
  />
              <Input
    label="Dinner Menu recipe"
    value={dnText}
    onChange={(e) => setDnText(e.target.value)}
    required
  />
            </div>

            <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setEditingDay(null)}>Cancel Adjust</Button>
              <Button type="submit" className="bg-indigo-650 hover:bg-slate-900 text-white font-extrabold leading-none">
                Announce Meal Updates
              </Button>
            </div>
          </form>}
      </Dialog>

    </div>;
}
