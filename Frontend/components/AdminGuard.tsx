"use client";

import Link from "next/link";
import { useAuth } from "../hooks/useAuth";

export default function AdminGuard({ children }: Readonly<{ children: React.ReactNode }>) {
  const { isLoading, user } = useAuth();
  if (isLoading) return <main className="grid min-h-screen place-items-center px-6 text-sm text-slate-400">Checking access…</main>;
  if (user?.role !== "ADMIN") return <main className="mx-auto flex min-h-screen max-w-xl items-center px-6"><section className="rounded-xl border border-rose-900 bg-rose-950/30 p-6"><h1 className="text-xl font-semibold">Administrator access required</h1><p className="mt-2 text-slate-300">This company-management area is available only to administrators.</p><Link href="/dashboard" className="mt-5 inline-block text-sky-400 hover:text-sky-300">Return to your workspace</Link></section></main>;
  return <>{children}</>;
}
