import { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle2, XCircle, X } from 'lucide-react';

// ---------- Buttons ----------
export function Button({ variant = 'default', size = 'md', className = '', ...props }) {
  const variants = {
    default: 'bg-slate-100 hover:bg-slate-200 text-slate-800',
    primary: 'bg-blue-600 hover:bg-blue-700 text-white',
    danger: 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200',
    ghost: 'hover:bg-slate-100 text-slate-600',
  };
  const sizes = { sm: 'px-2.5 py-1 text-xs', md: 'px-4 py-2 text-sm', lg: 'px-5 py-2.5 text-sm' };
  return (
    <button
      className={`inline-flex items-center gap-1.5 rounded-lg font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    />
  );
}

// ---------- Card ----------
export function Card({ title, subtitle, right, children, className = '' }) {
  return (
    <section className={`bg-white border border-slate-200 rounded-2xl shadow-sm ${className}`}>
      {(title || right) && (
        <header className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <div>
            {title && <h2 className="font-semibold text-slate-800">{title}</h2>}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
          </div>
          {right}
        </header>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}

// ---------- Form fields ----------
export function Field({ label, hint, apiName, required, children }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="flex flex-wrap items-center gap-1.5 text-xs font-semibold text-slate-600">
        <span>{label}</span>
        {apiName && <code className="font-mono text-[10px] font-medium text-slate-400">{apiName}</code>}
        {required === true && <span className="text-[9px] uppercase tracking-wide text-rose-600">required</span>}
        {required === false && <span className="text-[9px] uppercase tracking-wide text-slate-400">optional</span>}
      </span>
      {children}
      {hint && <span className="text-[11px] text-slate-400">{hint}</span>}
    </label>
  );
}
const inputBase =
  'px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-white';
export const Input = (p) => <input {...p} className={`${inputBase} ${p.className || ''}`} />;
export const Select = (p) => <select {...p} className={`${inputBase} ${p.className || ''}`} />;

// ---------- Badge ----------
export function Badge({ color = 'slate', children }) {
  const colors = {
    slate: 'bg-slate-100 text-slate-600',
    blue: 'bg-blue-100 text-blue-700',
    green: 'bg-emerald-100 text-emerald-700',
    amber: 'bg-amber-100 text-amber-700',
    rose: 'bg-rose-100 text-rose-700',
  };
  return <span className={`inline-block text-[11px] font-semibold px-2 py-0.5 rounded-full ${colors[color]}`}>{children}</span>;
}

// ---------- Empty state ----------
export function Empty({ icon: Icon, title, hint }) {
  return (
    <div className="text-center py-10 text-slate-400">
      {Icon && <Icon size={30} className="mx-auto mb-2 opacity-60" />}
      <p className="font-medium text-slate-500">{title}</p>
      {hint && <p className="text-xs mt-1">{hint}</p>}
    </div>
  );
}

// ---------- Toasts ----------
const ToastCtx = createContext(null);
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const push = useCallback((type, msg) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, type, msg }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 5000);
  }, []);
  const toast = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
  };
  return (
    <ToastCtx.Provider value={toast}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-md">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`flex items-start gap-2 rounded-xl shadow-lg px-4 py-3 text-sm border ${t.type === 'success' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-rose-50 border-rose-200 text-rose-800'}`}
          >
            {t.type === 'success' ? <CheckCircle2 size={17} className="mt-0.5 shrink-0" /> : <XCircle size={17} className="mt-0.5 shrink-0" />}
            <span className="flex-1 whitespace-pre-wrap">{t.msg}</span>
            <button onClick={() => setToasts((x) => x.filter((y) => y.id !== t.id))} className="text-current opacity-50 hover:opacity-100">
              <X size={15} />
            </button>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}
