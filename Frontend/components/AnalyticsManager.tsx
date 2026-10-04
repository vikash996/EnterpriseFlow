"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "../services/api";
import { useAuth } from "../hooks/useAuth";

type ProjectProgress = { id: string; name: string; status: string; dueDate: string; taskCount: number; completedTasks: number; completionPercent: number };
type Deadline = { id: string; kind: "TASK" | "PROJECT"; title: string; project: string; dueDate: string; assignee: string };
type EmployeeWorkload = { userId: string; name: string; taskCount: number; completedTasks: number; pendingTasks: number; overdueTasks: number };
type Analytics = {
  companyWide: boolean;
  projectCount: number;
  taskCount: number;
  completedTasks: number;
  pendingTasks: number;
  overdueTasks: number;
  delayedProjectCount: number;
  upcomingDeadlineCount: number;
  completionRate: number;
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
  byProjectStatus: Record<string, number>;
  projectProgress: ProjectProgress[];
  delayedProjects: Array<{ id: string; name: string; status: string; dueDate: string }>;
  upcomingDeadlines: Deadline[];
  employeeWorkload: EmployeeWorkload[];
};

export default function AnalyticsManager() {
  const { accessToken } = useAuth();
  const [data, setData] = useState<Analytics | null>(null);
  const [error, setError] = useState("");
  const [selectedMetric, setSelectedMetric] = useState<string | null>(null);

  useEffect(() => {
    if (accessToken) {
      void apiRequest<Analytics>("/api/analytics", { token: accessToken })
        .then(setData)
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load analytics."));
    }
  }, [accessToken]);

  if (error) return <main className="mx-auto min-h-screen max-w-7xl px-6 py-10"><p role="alert" className="text-rose-300">{error}</p></main>;
  if (!data) return <main className="grid min-h-screen place-items-center text-slate-400">Loading analytics…</main>;

  const metrics = [
    { key: "projects", label: "Active projects", value: data.projectCount, hint: "Currently accessible" },
    { key: "completed", label: "Completed tasks", value: data.completedTasks, hint: `of ${data.taskCount} tasks` },
    { key: "pending", label: "Pending tasks", value: data.pendingTasks, hint: "Still in progress" },
    { key: "overdue", label: "Overdue tasks", value: data.overdueTasks, hint: "Past due and unfinished" },
    { key: "delayed", label: "Delayed projects", value: data.delayedProjectCount, hint: "Past due and incomplete" },
    { key: "deadlines", label: "Upcoming deadlines", value: data.upcomingDeadlineCount, hint: "Within the next 7 days" },
  ];

  return <main className="mx-auto min-h-screen max-w-7xl px-4 py-10 sm:px-6">
    <header className="mb-8">
      <p className="text-sm font-medium text-sky-400">{data.companyWide ? "Company insights" : "Personal insights"}</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight">{data.companyWide ? "Company Analytics" : "My Analytics"}</h1>
      <p className="mt-2 text-slate-400">{data.companyWide ? "Delivery health and workload across your active company projects." : "Progress and deadlines for your authorized projects and assigned tasks."}</p>
    </header>

    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {metrics.map((metric) => <button key={metric.key} type="button" aria-pressed={selectedMetric === metric.key} onClick={() => setSelectedMetric(current => current === metric.key ? null : metric.key)} className={`rounded-xl border p-5 text-left transition ${selectedMetric === metric.key ? "border-sky-500 bg-sky-950/30" : "border-slate-800 bg-slate-900/60 hover:border-slate-600"}`}>
        <p className="text-sm text-slate-400">{metric.label}</p><p className="mt-2 text-3xl font-bold">{metric.value}</p><p className="mt-1 text-xs text-slate-500">{metric.hint}</p>
      </button>)}
    </section>

    {selectedMetric && <MetricDetails metric={selectedMetric} data={data} />}

    <section className="mt-6 grid gap-5 lg:grid-cols-[.7fr_1.3fr]">
      <article className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <h2 className="font-semibold">Task completion</h2>
        <div className="mt-5 flex items-center gap-5">
          <div className="grid h-28 w-28 shrink-0 place-items-center rounded-full" style={{ background: `conic-gradient(#38bdf8 ${data.completionRate}%, #1e293b 0)` }}>
            <div className="grid h-20 w-20 place-items-center rounded-full bg-slate-950 text-xl font-bold">{data.completionRate}%</div>
          </div>
          <p className="text-sm leading-6 text-slate-400">{data.completedTasks} of {data.taskCount} authorized tasks are complete.</p>
        </div>
      </article>
      <div className="grid gap-5 md:grid-cols-2">
        <Breakdown title="Task status" values={data.byStatus} />
        <Breakdown title="Task priority" values={data.byPriority} />
        <Breakdown title="Project status" values={data.byProjectStatus} />
      </div>
    </section>

    <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
      <h2 className="font-semibold">Project progress</h2>
      {data.projectProgress.length === 0 ? <Empty>No active authorized projects.</Empty> : <div className="mt-4 space-y-4">
        {data.projectProgress.map((project) => <article key={project.id}>
          <div className="flex flex-wrap justify-between gap-2 text-sm"><span className="font-medium">{project.name}</span><span className="text-slate-400">{project.status.replaceAll("_", " ")} · {project.completedTasks}/{project.taskCount} tasks · {project.completionPercent}%</span></div>
          <div className="mt-2 h-2 rounded-full bg-slate-800"><div className="h-2 rounded-full bg-sky-500" style={{ width: `${project.completionPercent}%` }} /></div>
          <p className="mt-1 text-xs text-slate-500">{project.dueDate ? `Due ${project.dueDate}` : "No project due date"}</p>
        </article>)}
      </div>}
    </section>

    {data.companyWide && <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
      <h2 className="font-semibold">Employee workload</h2>
      {data.employeeWorkload.length === 0 ? <Empty>No active employee workload to report.</Empty> : <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[640px] text-left text-sm"><thead className="border-b border-slate-800 text-slate-400"><tr><th className="pb-3 font-medium">Employee</th><th className="pb-3 font-medium">Tasks</th><th className="pb-3 font-medium">Completed</th><th className="pb-3 font-medium">Pending</th><th className="pb-3 font-medium">Overdue</th></tr></thead>
          <tbody>{data.employeeWorkload.map((person) => <tr key={person.userId} className="border-b border-slate-800/70 last:border-0"><td className="py-3 font-medium">{person.name}</td><td className="py-3">{person.taskCount}</td><td className="py-3 text-emerald-300">{person.completedTasks}</td><td className="py-3 text-slate-300">{person.pendingTasks}</td><td className="py-3 text-rose-300">{person.overdueTasks}</td></tr>)}</tbody>
        </table>
      </div>}
    </section>}

    <section className="mt-6 grid gap-5 lg:grid-cols-2">
      <article className="rounded-xl border border-slate-800 bg-slate-900/60 p-5"><h2 className="font-semibold">Delayed projects</h2>
        {data.delayedProjects.length === 0 ? <Empty>No delayed projects.</Empty> : <ul className="mt-4 space-y-3">{data.delayedProjects.map((project) => <li key={project.id} className="flex justify-between gap-3 text-sm"><span>{project.name}<span className="ml-2 text-slate-500">{project.status.replaceAll("_", " ")}</span></span><span className="shrink-0 text-rose-300">Due {project.dueDate}</span></li>)}</ul>}
      </article>
      <article className="rounded-xl border border-slate-800 bg-slate-900/60 p-5"><h2 className="font-semibold">Upcoming deadlines</h2>
        {data.upcomingDeadlines.length === 0 ? <Empty>No deadlines in the next 7 days.</Empty> : <ul className="mt-4 space-y-3">{data.upcomingDeadlines.map((deadline) => <li key={`${deadline.kind}-${deadline.id}`} className="flex flex-wrap justify-between gap-2 text-sm"><span>{deadline.title}<span className="ml-2 text-xs text-slate-500">{deadline.kind === "TASK" ? `${deadline.project} · ${deadline.assignee}` : "Project"}</span></span><span className="shrink-0 text-sky-300">{deadline.dueDate}</span></li>)}</ul>}
      </article>
    </section>
  </main>;
}

function Breakdown({ title, values }: { title: string; values: Record<string, number> }) {
  const entries = Object.entries(values);
  const total = Math.max(1, entries.reduce((sum, [, value]) => sum + value, 0));
  return <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5"><h2 className="font-semibold">{title}</h2>
    {entries.length ? <div className="mt-4 space-y-3">{entries.map(([name, value]) => <div key={name}>
      <div className="flex justify-between text-sm"><span>{name.replaceAll("_", " ")}</span><span>{value}</span></div><div className="mt-1 h-2 rounded bg-slate-800"><div className="h-2 rounded bg-sky-500" style={{ width: `${value / total * 100}%` }} /></div>
    </div>)}</div> : <Empty>No data yet.</Empty>}
  </section>;
}

function MetricDetails({ metric, data }: { metric: string; data: Analytics }) {
  const title = {
    projects: "Active project data",
    completed: "Completed task data",
    pending: "Pending task data",
    overdue: "Overdue task data",
    delayed: "Delayed project data",
    deadlines: "Upcoming deadline data",
  }[metric] ?? "Analytics data";

  return <section className="mt-5 rounded-xl border border-sky-900/70 bg-slate-900/70 p-5" aria-live="polite">
    <h2 className="font-semibold">{title}</h2>
    {metric === "projects" && (data.projectProgress.length ? <ul className="mt-3 space-y-2">{data.projectProgress.map(project => <li key={project.id} className="flex flex-wrap justify-between gap-2 text-sm"><span>{project.name} · {project.status.replaceAll("_", " ")}</span><span className="text-slate-400">{project.taskCount} tasks · {project.completionPercent}%</span></li>)}</ul> : <Empty>No active projects in this authorized scope.</Empty>)}
    {metric === "completed" && <p className="mt-3 text-sm text-slate-300">{data.completedTasks} of {data.taskCount} tasks are complete ({data.byStatus.DONE ?? data.completedTasks} marked DONE).</p>}
    {metric === "pending" && <><p className="mt-3 text-sm text-slate-300">{data.pendingTasks} unfinished tasks.</p><Breakdown title="Pending status distribution" values={Object.fromEntries(Object.entries(data.byStatus).filter(([status]) => status.toUpperCase() !== "DONE"))} /></>}
    {metric === "overdue" && (data.companyWide ? <ul className="mt-3 space-y-2 text-sm">{data.employeeWorkload.filter(person => person.overdueTasks > 0).map(person => <li key={person.userId} className="flex justify-between gap-2"><span>{person.name}</span><span className="text-rose-300">{person.overdueTasks} overdue</span></li>)}{data.employeeWorkload.every(person => person.overdueTasks === 0) && <li className="text-slate-400">No overdue tasks assigned to active employees.</li>}</ul> : <p className="mt-3 text-sm text-slate-300">{data.overdueTasks} overdue tasks in your authorized scope.</p>)}
    {metric === "delayed" && (data.delayedProjects.length ? <ul className="mt-3 space-y-2 text-sm">{data.delayedProjects.map(project => <li key={project.id} className="flex justify-between gap-2"><span>{project.name}</span><span className="text-rose-300">Due {project.dueDate}</span></li>)}</ul> : <Empty>No delayed projects in this authorized scope.</Empty>)}
    {metric === "deadlines" && (data.upcomingDeadlines.length ? <ul className="mt-3 space-y-2 text-sm">{data.upcomingDeadlines.map(deadline => <li key={`${deadline.kind}-${deadline.id}`} className="flex flex-wrap justify-between gap-2"><span>{deadline.title} <span className="text-slate-500">{deadline.kind === "TASK" ? `· ${deadline.project}` : "· Project"}</span></span><span className="text-sky-300">{deadline.dueDate}</span></li>)}</ul> : <Empty>No upcoming deadlines in this authorized scope.</Empty>)}
  </section>;
}

function Empty({ children }: { children: string }) { return <p className="mt-4 text-sm text-slate-500">{children}</p>; }
