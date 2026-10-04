"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { ApiError } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginPage() {
  const { login, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.replace("/dashboard");
    }
  }, [isAuthenticated, isLoading, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!emailPattern.test(email.trim())) {
      setError("Enter a valid email address.");
      return;
    }
    if (!password || password.length > 72) {
      setError("Enter a valid password.");
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await login({ email: email.trim(), password });
      router.replace("/dashboard");
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Unable to log in. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-[calc(100vh-65px)] max-w-md items-center px-6 py-16">
      <form onSubmit={handleSubmit} className="w-full space-y-5 rounded-xl border border-slate-800 bg-slate-900 p-6 shadow-xl">
        <div><h1 className="text-2xl font-bold">Log in</h1><p className="mt-1 text-sm text-slate-400">Use your EnterpriseFlow account.</p></div>
        {error && <p role="alert" className="rounded-md border border-rose-900 bg-rose-950/50 p-3 text-sm text-rose-200">{error}</p>}
        <label className="block text-sm font-medium">Email<input value={email} onChange={(event) => setEmail(event.target.value)} type="email" autoComplete="email" required maxLength={254} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /></label>
        <label className="block text-sm font-medium">Password<input value={password} onChange={(event) => setPassword(event.target.value)} type="password" autoComplete="current-password" required maxLength={72} className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" /></label>
        <button disabled={submitting} className="w-full rounded-md bg-sky-500 px-4 py-2 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Logging in…" : "Log in"}</button>
        <p className="text-center text-sm text-slate-400">Need an account? <Link className="text-sky-400 hover:text-sky-300" href="/register">Register</Link></p>
      </form>
    </main>
  );
}
