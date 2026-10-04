"use client";

import { FormEvent, useEffect, useState } from "react";
import { ApiError, apiRequest } from "../services/api";
import { useAuth } from "../hooks/useAuth";
import type { User } from "../types/auth";

export default function ProfileEditor() {
  const { accessToken, user } = useAuth();
  const [name, setName] = useState(user?.name ?? "");
  const [profile, setProfile] = useState<User | null>(user);
  const [state, setState] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [message, setMessage] = useState("");

  useEffect(() => { if (!accessToken) return; apiRequest<User>("/api/users/me", { token: accessToken }).then((result) => { setProfile(result); setName(result.name); }).catch((error: ApiError) => { setState("error"); setMessage(error.message); }); }, [accessToken]);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!accessToken) return; setState("saving"); setMessage(""); try { const updated = await apiRequest<User>("/api/users/me", { method: "PATCH", token: accessToken, body: { name } }); setProfile(updated); setName(updated.name); setState("saved"); setMessage("Profile updated successfully."); } catch (error) { setState("error"); setMessage(error instanceof Error ? error.message : "Unable to update profile."); } }
  return <main className="mx-auto min-h-screen max-w-3xl px-6 py-12"><p className="text-sm font-medium text-sky-400">Account settings</p><h1 className="mt-1 text-3xl font-bold tracking-tight">Your profile</h1><form onSubmit={submit} className="mt-8 space-y-5 rounded-xl border border-slate-800 bg-slate-900/60 p-6"><label className="block text-sm font-medium">Full name<input required minLength={2} maxLength={150} value={name} onChange={(event) => setName(event.target.value)} className="mt-2 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100" /></label><div className="grid gap-4 sm:grid-cols-2"><div><p className="text-sm text-slate-400">Email</p><p className="mt-1 font-medium">{profile?.email ?? "Loading…"}</p></div><div><p className="text-sm text-slate-400">Role</p><p className="mt-1 font-medium">{profile?.role ?? "—"}</p></div></div>{message && <p className={state === "error" ? "text-sm text-red-300" : "text-sm text-emerald-300"}>{message}</p>}<button disabled={state === "saving"} className="rounded-md bg-sky-500 px-4 py-2 font-medium text-slate-950 disabled:opacity-60">{state === "saving" ? "Saving…" : "Save changes"}</button></form></main>;
}
