"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "../services/api";
import { useAuth } from "../hooks/useAuth";

type Activity = { id: string; summary: string; actor: string; createdAt: string };
type Task = { id: string; title: string; status: string; priority: string; dueDate: string | null; projectName: string; assignee: string | null };
type Project = { id: string; name: string; status: string; dueDate: string };
type Dashboard = { projects: number; projectItems?: Project[] | null; myTasks: number; myTaskItems?: Task[] | null; overdue: number; unreadNotifications: number; recentActivity?: Activity[] | null };

export default function WorkspacePage({ kind }: { kind: "dashboard" }) {
  const { accessToken } = useAuth();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!accessToken) return;
    apiRequest<Dashboard>("/api/dashboard", { token: accessToken })
      .then(setDashboard)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load the dashboard."));
  }, [accessToken, kind]);

  if (!dashboard && !error) return <main className="grid min-h-screen place-items-center px-6"><p className="text-sm text-slate-400">Loading your workspace…</p></main>;

  const projectItems = dashboard?.projectItems ?? [];
  const myTaskItems = dashboard?.myTaskItems ?? [];
  const recentActivity = dashboard?.recentActivity ?? [];

  return <main className="mx-auto min-h-screen max-w-6xl px-4 py-10 sm:px-6"><div className="mb-8"><p className="text-sm font-medium text-sky-400">EnterpriseFlow workspace</p><h1 className="mt-1 text-3xl font-bold tracking-tight">Company dashboard</h1><p className="mt-2 text-slate-400">A focused view of your work, deadlines, and updates.</p></div>{error ? <p role="alert" className="rounded-lg border border-rose-900 bg-rose-950/40 p-4 text-rose-200">{error}</p> : dashboard && <><section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[{ label: "Projects", value: dashboard.projects, hint: "Available to you" }, { label: "My tasks", value: dashboard.myTasks, hint: "Assigned work" }, { label: "Overdue", value: dashboard.overdue, hint: "Needs attention" }, { label: "Notifications", value: dashboard.unreadNotifications, hint: "Unread updates" }].map((metric) => <article key={metric.label} className="rounded-xl border border-slate-800 bg-slate-900/60 p-5"><p className="text-sm font-medium text-slate-400">{metric.label}</p><p className="mt-2 text-3xl font-bold tracking-tight">{metric.value}</p><p className="mt-1 text-sm text-slate-500">{metric.hint}</p></article>)}</section><section className="mt-8 rounded-xl border border-slate-800 bg-slate-900/60 p-5"><h2 className="font-semibold">Your projects</h2>{projectItems.length === 0 ? <p className="mt-5 rounded-lg border border-dashed border-slate-700 p-5 text-sm text-slate-400">No projects are assigned to your workspace yet.</p> : <div className="mt-5 grid gap-3 md:grid-cols-2">{projectItems.map((project) => <article key={project.id} className="rounded-xl border border-slate-800 p-4"><div className="flex justify-between gap-3"><h3 className="font-medium">{project.name}</h3><span className="text-xs text-sky-300">{project.status.replace("_", " ")}</span></div><p className="mt-3 text-xs text-slate-500">{project.dueDate ? `Due ${project.dueDate}` : "No due date"}</p></article>)}</div>}</section><section className="mt-8 rounded-xl border border-slate-800 bg-slate-900/60 p-5"><h2 className="font-semibold">Your tasks</h2>{myTaskItems.length === 0 ? <p className="mt-5 rounded-lg border border-dashed border-slate-700 p-5 text-sm text-slate-400">No assigned tasks yet.</p> : <div className="mt-5 grid gap-3 md:grid-cols-2">{myTaskItems.map((task) => <article key={task.id} className="rounded-xl border border-slate-800 p-4"><div className="flex justify-between gap-3"><h3 className="font-medium">{task.title}</h3><span className="text-xs text-sky-300">{task.status.replace("_", " ")}</span></div><p className="mt-2 text-xs text-slate-400">{task.projectName}{task.assignee ? ` · ${task.assignee}` : ""}</p><p className="mt-3 text-xs text-slate-500">{task.priority}{task.dueDate ? ` · due ${task.dueDate}` : ""}</p></article>)}</div>}</section><section className="mt-8 rounded-xl border border-slate-800 bg-slate-900/60 p-5"><div><h2 className="font-semibold">Recent activity</h2><p className="mt-1 text-sm text-slate-400">Actions performed in your workspace.</p></div>{recentActivity.length === 0 ? <p className="mt-5 rounded-lg border border-dashed border-slate-700 p-5 text-sm text-slate-400">No activity yet. Create a project or task to get started.</p> : <ol className="mt-5 divide-y divide-slate-800">{recentActivity.map((activity) => <li key={activity.id} className="flex flex-wrap items-center justify-between gap-2 py-4 first:pt-0"><p className="text-sm text-slate-200">{activity.summary}</p><time className="text-xs text-slate-500" dateTime={activity.createdAt}>{new Date(activity.createdAt).toLocaleString()}</time></li>)}</ol>}</section></>}</main>;
}
