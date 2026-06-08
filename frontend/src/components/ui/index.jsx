/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import React, { useState, createContext, useContext } from "react";
import { X } from "lucide-react";
const ToastContext = createContext(void 0);
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const addToast = (title, description, type = "success") => {
    const id = Math.random().toString(36).substr(2, 9);
    setToasts((prev) => [...prev, { id, title, description, type }]);
  };
  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };
  return <ToastContext.Provider value={{ addToast, toasts, removeToast }}>
      {children}
      {
    /* Toast Render Panel */
  }
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3 w-96 max-w-full">
        {toasts.map((toast) => {
    let bg = "bg-emerald-50 border-emerald-200 text-emerald-900";
    let indicator = "bg-emerald-500";
    if (toast.type === "error") {
      bg = "bg-rose-50 border-rose-200 text-rose-900";
      indicator = "bg-rose-500";
    } else if (toast.type === "warning") {
      bg = "bg-amber-50 border-amber-200 text-amber-900";
      indicator = "bg-amber-500";
    } else if (toast.type === "info") {
      bg = "bg-blue-50 border-blue-200 text-blue-900";
      indicator = "bg-blue-500";
    }
    return <div
      key={toast.id}
      className={`flex items-start gap-3 p-4 rounded-xl border shadow-lg transition-all duration-300 transform translate-y-0 ${bg}`}
    >
              <div className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${indicator}`} />
              <div className="flex-1">
                <h4 className="text-sm font-semibold">{toast.title}</h4>
                {toast.description && <p className="text-xs mt-1 text-slate-600 font-medium leading-relaxed">
                    {toast.description}
                  </p>}
              </div>
              <button
      onClick={() => removeToast(toast.id)}
      className="text-slate-400 hover:text-slate-600 transition"
    >
                <X className="h-4 w-4" />
              </button>
            </div>;
  })}
      </div>
    </ToastContext.Provider>;
}
export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
export function Card({
  children,
  className = "",
  id
}) {
  return <div
    id={id}
    className={`bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-shadow duration-300 p-6 ${className}`}
  >
      {children}
    </div>;
}
export function CardHeader({ children, className = "" }) {
  return <div className={`flex flex-col gap-1 mb-4 ${className}`}>{children}</div>;
}
export function CardTitle({ children, className = "" }) {
  return <h3 className={`text-lg font-bold text-slate-800 tracking-tight ${className}`}>{children}</h3>;
}
export function CardDescription({ children, className = "" }) {
  return <p className={`text-xs text-slate-400 font-medium ${className}`}>{children}</p>;
}
export function CardContent({ children, className = "" }) {
  return <div className={className}>{children}</div>;
}
export function Badge({
  children,
  variant = "default",
  className = ""
}) {
  let colors = "bg-slate-100 text-slate-800 border-slate-200";
  if (variant === "success") colors = "bg-emerald-50 text-emerald-700 border-emerald-200";
  else if (variant === "warning") colors = "bg-amber-50 text-amber-700 border-amber-200";
  else if (variant === "danger") colors = "bg-rose-50 text-rose-700 border-rose-200";
  else if (variant === "info") colors = "bg-sky-50 text-sky-700 border-sky-200";
  else if (variant === "secondary") colors = "bg-blue-50 text-blue-700 border-blue-200";
  return <span
    className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${colors} ${className}`}
  >
      {children}
    </span>;
}
export function Button({
  children,
  onClick,
  variant = "primary",
  size = "md",
  disabled = false,
  className = "",
  type = "button",
  id
}) {
  let baseStyle = "inline-flex items-center justify-center font-semibold rounded-xl transition duration-250 shrink-0 select-none";
  let variantStyle = "bg-slate-900 text-white hover:bg-slate-800 active:scale-[0.98] border border-transparent";
  if (variant === "secondary") variantStyle = "bg-blue-600 text-white hover:bg-blue-700 active:scale-[0.98]";
  else if (variant === "danger") variantStyle = "bg-rose-600 text-white hover:bg-rose-700 active:scale-[0.98]";
  else if (variant === "outline") variantStyle = "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50";
  else if (variant === "ghost") variantStyle = "text-slate-600 hover:bg-slate-100 hover:text-slate-900";
  let sizeStyle = "px-4 py-2 text-sm";
  if (size === "sm") sizeStyle = "px-3 py-1.5 text-xs rounded-lg";
  else if (size === "lg") sizeStyle = "px-6 py-3 text-base";
  if (disabled) {
    variantStyle = "bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed";
  }
  return <button
    id={id}
    type={type}
    onClick={onClick}
    disabled={disabled}
    className={`${baseStyle} ${variantStyle} ${sizeStyle} ${className}`}
  >
      {children}
    </button>;
}
export function Input({
  label,
  error,
  className = "",
  id,
  type = "text",
  value,
  onChange,
  placeholder,
  required,
  disabled,
  min,
  max,
  step
}) {
  const inputId = id || `input-${Math.random().toString(36).substr(2, 4)}`;
  return <div className="flex flex-col gap-1.5 w-full">
      {label && <label htmlFor={inputId} className="text-xs font-bold text-slate-600 uppercase tracking-wider">{label}</label>}
      <input
    id={inputId}
    type={type}
    value={value}
    onChange={onChange}
    placeholder={placeholder}
    required={required}
    disabled={disabled}
    min={min}
    max={max}
    step={step}
    className={`bg-slate-50 border border-slate-200 text-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition ${className}`}
  />
      {error && <p className="text-rose-500 text-xs font-medium">{error}</p>}
    </div>;
}
export function Select({
  label,
  options,
  error,
  className = "",
  id,
  value,
  onChange,
  disabled
}) {
  const selectId = id || `select-${Math.random().toString(36).substr(2, 4)}`;
  return <div className="flex flex-col gap-1.5 w-full">
      {label && <label htmlFor={selectId} className="text-xs font-bold text-slate-600 uppercase tracking-wider">{label}</label>}
      <select
    id={selectId}
    value={value}
    onChange={onChange}
    disabled={disabled}
    className={`bg-slate-50 border border-slate-200 text-slate-805 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition select-none ${className}`}
  >
        {options.map((opt) => <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>)}
      </select>
      {error && <p className="text-rose-500 text-xs font-medium">{error}</p>}
    </div>;
}
export function Tabs({
  activeTab,
  onChange,
  children
}) {
  return <div className="flex flex-col gap-4 w-full">
      {React.Children.map(children, (child) => {
    if (React.isValidElement(child)) {
      return React.cloneElement(child, { activeTab, onChange });
    }
    return child;
  })}
    </div>;
}
export function TabsList({
  children,
  activeTab,
  onChange,
  className = ""
}) {
  return <div className={`flex border-b border-slate-100 overflow-x-auto scroller-hidden gap-1 pb-1 ${className}`}>
      {React.Children.map(children, (child) => {
    if (React.isValidElement(child)) {
      return React.cloneElement(child, { activeTab, onChange });
    }
    return child;
  })}
    </div>;
}
export function TabsTrigger({
  value,
  children,
  activeTab,
  onChange
}) {
  const isActive = activeTab === value;
  return <button
    onClick={() => onChange?.(value)}
    className={`px-4 py-2.5 text-sm font-semibold select-none border-b-2 transition relative whitespace-nowrap ${isActive ? "border-blue-600 text-blue-600" : "border-transparent text-slate-400 hover:text-slate-600"}`}
  >
      {children}
    </button>;
}
export function TabsContent({
  value,
  activeTab,
  children
}) {
  if (activeTab !== value) return null;
  return <div className="animate-fade-in pt-1">{children}</div>;
}
export function Dialog({
  isOpen,
  onClose,
  title,
  children,
  maxWidth = "max-w-xl"
}) {
  if (!isOpen) return null;
  return <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {
    /* Backdrop */
  }
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity" onClick={onClose} />
      
      {
    /* Box */
  }
      <div className={`relative bg-white rounded-3xl w-full ${maxWidth} shadow-2xl p-6 overflow-hidden max-h-[90vh] flex flex-col z-10 animate-scale-up`}>
        <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-4">
          <h3 className="text-lg font-bold text-slate-800">{title}</h3>
          <button onClick={onClose} className="rounded-full p-1.5 hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto pr-1">
          {children}
        </div>
      </div>
    </div>;
}
