"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../hooks/useAuth";

export default function ProtectedRoute({ children }: Readonly<{ children: React.ReactNode }>) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading || !isAuthenticated) {
    return (
      <main className="grid min-h-screen place-items-center px-6">
        <p className="text-sm text-slate-400">Checking your session…</p>
      </main>
    );
  }

  if (user?.membershipStatus && user.membershipStatus !== "ACTIVE") {
    const rejected = user.membershipStatus === "REJECTED";
    return <main className="grid min-h-screen place-items-center px-6"><section className="max-w-md rounded-xl border border-slate-800 bg-slate-900 p-6 text-center"><h1 className="text-xl font-semibold">{rejected ? "Join request rejected" : "Join request pending"}</h1><p className="mt-2 text-sm text-slate-400">{rejected ? "An administrator did not approve your company access. Contact the company owner if you believe this is an error." : "Your account is secure, but company resources will be available only after an administrator approves your request."}</p></section></main>;
  }

  return <>{children}</>;
}
