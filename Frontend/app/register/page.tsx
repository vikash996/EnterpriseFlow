"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useAuth } from "../../hooks/useAuth";
import { ApiError, apiRequest } from "../../services/api";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
type Mode = "CREATE" | "JOIN";
type Company = { id: string; name: string };

export default function RegisterPage() {
  const { register } = useAuth();
  const [mode, setMode] = useState<Mode>("CREATE");
  const [name, setName] = useState(""); const [email, setEmail] = useState(""); const [password, setPassword] = useState("");
  const [companyName, setCompanyName] = useState(""); const [companyId, setCompanyId] = useState("");
  const [companies, setCompanies] = useState<Company[]>([]); const [canCreate, setCanCreate] = useState(true);
  const [error, setError] = useState<string | null>(null); const [success, setSuccess] = useState<string | null>(null); const [submitting, setSubmitting] = useState(false);

  useEffect(() => { void Promise.all([apiRequest<Company[]>("/api/companies"), apiRequest<{ canCreateInitialCompany: boolean }>("/api/companies/bootstrap-status")]).then(([found, status]) => { setCompanies(found); setCanCreate(status.canCreateInitialCompany); if (!status.canCreateInitialCompany) setMode("JOIN"); }).catch(() => setError("Unable to load company registration options.")); }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim() || name.trim().length > 150) return setError(mode === "CREATE" ? "Enter the owner name (up to 150 characters)." : "Enter your name (up to 150 characters).");
    if (!emailPattern.test(email.trim())) return setError("Enter a valid email address.");
    if (password.length < 12 || password.length > 72) return setError("Password must be between 12 and 72 characters.");
    if (mode === "CREATE" && !companyName.trim()) return setError("Enter your company name.");
    if (mode === "JOIN" && !companyId) return setError("Select the company you want to join.");
    setError(null); setSuccess(null); setSubmitting(true);
    try {
      if (mode === "CREATE") { await apiRequest("/api/auth/register-company", { method: "POST", body: { companyName: companyName.trim(), ownerName: name.trim(), email: email.trim(), password } }); setSuccess("Your company is ready. You are its administrator and can log in now."); }
      else { await register({ name: name.trim(), email: email.trim(), password, companyId }); setSuccess("Your join request was sent. Company access begins after an administrator approves it."); }
      setPassword("");
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : "Unable to create your account. Please try again."); }
    finally { setSubmitting(false); }
  }

  return <main className="mx-auto flex min-h-[calc(100vh-65px)] max-w-2xl items-center px-6 py-16"><form onSubmit={submit} className="w-full space-y-6 rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-xl sm:p-8"><div><p className="text-sm font-medium text-sky-400">EnterpriseFlow onboarding</p><h1 className="mt-1 text-3xl font-bold">Set up your workspace</h1><p className="mt-2 text-sm text-slate-400">Create the initial company or request access to an existing one.</p></div><div className="grid gap-3 sm:grid-cols-2"><button type="button" disabled={!canCreate} onClick={() => setMode("CREATE")} className={`rounded-xl border p-4 text-left transition ${mode === "CREATE" ? "border-sky-500 bg-sky-950/40" : "border-slate-700 hover:border-slate-500"} disabled:cursor-not-allowed disabled:opacity-50`}><span className="block font-semibold">Create your company</span><span className="mt-1 block text-sm text-slate-400">Become the first company owner and administrator.</span></button><button type="button" onClick={() => setMode("JOIN")} className={`rounded-xl border p-4 text-left transition ${mode === "JOIN" ? "border-sky-500 bg-sky-950/40" : "border-slate-700 hover:border-slate-500"}`}><span className="block font-semibold">Join a company</span><span className="mt-1 block text-sm text-slate-400">Send a request for administrator approval.</span></button></div>{!canCreate && <p className="rounded-lg border border-amber-900 bg-amber-950/30 p-3 text-sm text-amber-100">A company owner already exists. New accounts must join an existing company.</p>}{error && <p role="alert" className="rounded-lg border border-rose-900 bg-rose-950/50 p-3 text-sm text-rose-200">{error}</p>}{success && <p role="status" className="rounded-lg border border-emerald-900 bg-emerald-950/50 p-3 text-sm text-emerald-200">{success} <Link className="underline" href="/login">Log in</Link></p>}{mode === "CREATE" && <label className="block text-sm font-medium">Company name<input value={companyName} onChange={e=>setCompanyName(e.target.value)} required maxLength={160} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /></label>}<label className="block text-sm font-medium">{mode === "CREATE" ? "Owner name" : "Your name"}<input value={name} onChange={e=>setName(e.target.value)} autoComplete="name" required maxLength={150} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /></label>{mode === "JOIN" && <label className="block text-sm font-medium">Company<select value={companyId} onChange={e=>setCompanyId(e.target.value)} required className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"><option value="">Select a company</option>{companies.map(company=><option key={company.id} value={company.id}>{company.name}</option>)}</select></label>}<label className="block text-sm font-medium">Email<input value={email} onChange={e=>setEmail(e.target.value)} type="email" autoComplete="email" required maxLength={254} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /></label><label className="block text-sm font-medium">Password<input value={password} onChange={e=>setPassword(e.target.value)} type="password" autoComplete="new-password" required minLength={12} maxLength={72} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /><span className="mt-1 block text-xs font-normal text-slate-400">12–72 characters</span></label><button disabled={submitting || (mode === "CREATE" && !canCreate)} className="w-full rounded-md bg-sky-500 px-4 py-2.5 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Submitting…" : mode === "CREATE" ? "Create company" : "Request to join"}</button><p className="text-center text-sm text-slate-400">Already have an account? <Link className="text-sky-400 hover:text-sky-300" href="/login">Log in</Link></p></form></main>;
}
