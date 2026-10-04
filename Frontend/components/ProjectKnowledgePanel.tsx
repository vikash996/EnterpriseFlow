"use client";
import { useCallback, useEffect, useState } from "react";
import { apiRequest } from "../services/api";
import { useAuth } from "../hooks/useAuth";

type Context = {
  project: { id: string; name: string; status: string };
  people: { id: string; name: string }[];
  tasks: { id: string; title: string; status: string; assignee: string | null; dueDate: string | null }[];
  meetings: { id: string; title: string; scheduledAt: string | null; actionItems: { id: string; title: string; status: string }[] }[];
  documents: { id: string; title: string; owner: string; createdAt: string }[];
  activity: { id: string; summary: string; actor: string; createdAt: string }[];
};
type Reply = { answer: string; sources: { id: string; title: string; excerpt: string }[] };

export default function ProjectKnowledgePanel({ projectId }: { projectId: string }) {
  const { accessToken } = useAuth();
  const [context, setContext] = useState<Context | null>(null);
  const [reply, setReply] = useState<Reply | null>(null);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadContext = useCallback(async () => {
    if (!accessToken || !projectId) return;
    setError("");
    try { setContext(await apiRequest<Context>(`/api/projects/${projectId}/knowledge`, { token: accessToken })); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load project context."); }
  }, [accessToken, projectId]);

  useEffect(() => {
    if (!accessToken || !projectId) {
      setContext(null);
      setError("");
      return;
    }
    setContext(null);
    void loadContext();
  }, [accessToken, projectId, loadContext]);

  async function ask(value: string) {
    if (!accessToken || loading) return;
    setLoading(true); setError(""); setQuestion(value);
    try { setReply(await apiRequest<Reply>(`/api/projects/${projectId}/understand`, { method: "POST", token: accessToken, body: { question: value } })); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to understand this project."); }
    finally { setLoading(false); }
  }

  const questions = ["Understand this project", "What happened in this project recently?", "Who is working on this project?", "What tasks are overdue?", "What meetings are related to this project?", "What documents are associated with this project?"];
  return <section className="panel mt-6" aria-busy={!context}>
    <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-sm font-semibold text-sky-400">Knowledge / Context</p><h2 className="mt-1 text-xl font-semibold">Connected project workspace</h2><p className="mt-1 text-sm text-slate-400">People, delivery work, meetings, documents, and recent activity you can access.</p></div><div className="flex gap-4 text-sm"><button onClick={() => void loadContext()} className="text-slate-300 hover:text-white">Refresh context</button><a href="/documents" className="text-sky-300 hover:text-sky-200">Open documents →</a></div></div>
    {!context && !error && <p className="mt-5 text-sm text-slate-400">Loading authorized project context…</p>}
    {error && <p role="alert" className="mt-4 text-sm text-rose-300">{error}</p>}
    {context && <>
      {/* EnterpriseFlow — Built by Vikash Panday */}
      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <article className="rounded-xl border border-sky-900/70 bg-sky-950/20 p-4"><p className="text-xs uppercase tracking-wide text-sky-300">Project</p><p className="mt-2 font-medium">{context.project.name}</p><p className="mt-1 text-xs text-slate-400">{context.project.status.replaceAll("_", " ")}</p></article>
        <ContextGroup title="People" empty="No project members" items={context.people.map(person => person.name)} />
        <ContextGroup title="Tasks" empty="No tasks" items={context.tasks.map(task => `${task.title} · ${task.status.replaceAll("_", " ")}${task.assignee ? ` · ${task.assignee}` : ""}`)} />
        <ContextGroup title="Meetings" empty="No related meetings" items={context.meetings.map(meeting => `${meeting.title}${meeting.actionItems.length ? ` · ${meeting.actionItems.length} action items` : ""}`)} />
        <ContextGroup title="Documents" empty="No associated documents" items={context.documents.map(document => `${document.title} · ${document.owner}`)} />
      </div>
      <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_1fr]">
        <div className="rounded-xl border border-slate-800 p-4"><h3 className="font-medium">Recent activity</h3>{context.activity.length ? <ul className="mt-3 space-y-2">{context.activity.slice(0, 6).map(item => <li key={item.id} className="text-sm text-slate-300"><span>{item.summary}</span><span className="ml-2 text-xs text-slate-500">{item.actor}</span></li>)}</ul> : <p className="mt-2 text-sm text-slate-500">No related activity recorded.</p>}</div>
        <div className="rounded-xl border border-slate-800 p-4"><h3 className="font-medium">Understand this project</h3><p className="mt-1 text-xs text-slate-400">Answers use authorized project records and the existing Work Insights service.</p><div className="mt-3 flex flex-wrap gap-2">{questions.map(item => <button key={item} disabled={loading} onClick={() => void ask(item)} className="rounded-full border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:border-sky-500 hover:text-sky-200 disabled:opacity-50">{item}</button>)}</div><form className="mt-3 flex gap-2" onSubmit={event => { event.preventDefault(); if (question.trim()) void ask(question.trim()); }}><input value={question} onChange={event => setQuestion(event.target.value)} placeholder="Ask a project question" className="field min-w-0 flex-1 text-sm"/><button disabled={loading || !question.trim()} className="button-secondary text-sm">{loading ? "Thinking…" : "Ask"}</button></form>{reply && <div className="mt-3 rounded-lg bg-slate-950 p-3"><p className="text-sm leading-6 text-slate-200">{reply.answer}</p>{reply.sources.length > 0 && <p className="mt-2 text-xs text-slate-500">Based on {reply.sources.map(source => source.title).join(", ")}</p>}</div>}</div>
      </div>
    </>}
  </section>;
}

function ContextGroup({ title, empty, items }: { title: string; empty: string; items: string[] }) {
  return <article className="rounded-xl border border-slate-800 bg-slate-950/30 p-4"><p className="text-xs uppercase tracking-wide text-slate-400">{title}</p>{items.length ? <ul className="mt-2 space-y-1.5">{items.slice(0, 5).map((item, index) => <li key={`${item}-${index}`} className="text-sm text-slate-200">{item}</li>)}</ul> : <p className="mt-2 text-sm text-slate-500">{empty}</p>}</article>;
}
