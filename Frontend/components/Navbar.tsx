"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "../hooks/useAuth";
import NotificationsMenu from "./NotificationsMenu";

export default function Navbar() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const router = useRouter(); const pathname = usePathname(); const [menuOpen, setMenuOpen] = useState(false);
  useEffect(() => { document.body.classList.toggle("has-app-shell", Boolean(isAuthenticated)); return () => document.body.classList.remove("has-app-shell"); }, [isAuthenticated]);
  const logoutAndRedirect = () => { logout(); router.replace("/login"); };
  const workspace = [["Overview", "/dashboard", "overview"], ["Projects", "/projects", "projects"], ["Tasks", "/tasks", "tasks"], ["Meetings", "/meetings", "meetings"], ["Documents", "/documents", "documents"], ["Search", "/search", "search"], ["Analytics", "/analytics", "analytics"], ["Knowledge", "/ai-assistance", "knowledge"]] as const;
  const administration = [["Command Center", "/command-center", "command"], ["Company", "/admin", "company"], ["People & access", "/admin/users", "people"]] as const;
  const item = (label: string, href: string, icon: IconName) => <Link onClick={() => setMenuOpen(false)} href={href} className={`flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium ${pathname === href ? "bg-indigo-500/15 text-indigo-200 ring-1 ring-inset ring-indigo-400/20" : "text-slate-400 hover:bg-slate-800/70 hover:text-slate-100"}`}><span className="grid h-5 w-5 place-items-center text-base leading-none"><NavigationIcon name={icon} /></span>{label}</Link>;

  if (!isLoading && isAuthenticated && user) return <>
    <aside className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800 bg-slate-950 px-3 py-5 transition-transform lg:translate-x-0 ${menuOpen ? "translate-x-0" : "-translate-x-full"}`}>
      <Link href="/dashboard" className="flex items-center gap-3 px-3"><span className="grid h-8 w-8 place-items-center rounded-xl bg-indigo-500 font-bold text-white shadow-lg shadow-indigo-900/40">E</span><span className="font-semibold tracking-tight text-white">EnterpriseFlow</span></Link>
      <nav className="mt-8 space-y-1" aria-label="Workspace navigation"><p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[.17em] text-slate-600">Workspace</p>{workspace.map(([label, href, icon]) => <span key={href}>{item(label, href, icon)}</span>)}{user.role === "ADMIN" && <><p className="px-3 pb-2 pt-6 text-[10px] font-semibold uppercase tracking-[.17em] text-slate-600">Administration</p>{administration.map(([label, href, icon]) => <span key={href}>{item(label, href, icon)}</span>)}</>}</nav>
      <div className="mt-auto border-t border-slate-800 pt-3">{item("Your profile", "/profile", "profile")}<button type="button" onClick={logoutAndRedirect} className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-slate-500 hover:bg-rose-500/10 hover:text-rose-300"><span aria-hidden="true"><ExitIcon /></span>Log out</button></div>
    </aside>
    {menuOpen && <button aria-label="Close navigation" className="fixed inset-0 z-30 bg-slate-950/65 lg:hidden" onClick={() => setMenuOpen(false)} />}
    <header className="fixed inset-x-0 top-0 z-20 border-b border-slate-800/80 bg-slate-950/85 backdrop-blur-xl lg:left-64"><nav className="flex h-[4.25rem] items-center justify-between gap-3 px-4 sm:px-6"><button aria-label="Open navigation" onClick={() => setMenuOpen(true)} className="rounded-lg p-2 text-slate-300 hover:bg-slate-800 lg:hidden"><MenuIcon /></button><Link href="/search" className="hidden w-full max-w-md items-center gap-3 rounded-xl border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-500 hover:border-slate-700 sm:flex"><SearchIcon /><span>Search workspace</span><kbd className="ml-auto text-xs">Cmd K</kbd></Link><div className="ml-auto flex items-center gap-3"><NotificationsMenu /><Link href="/profile" className="flex items-center gap-2 rounded-xl p-1.5 pr-2.5 hover:bg-slate-800"><span className="grid h-7 w-7 place-items-center rounded-lg bg-indigo-500/20 text-xs font-bold text-indigo-200">{user.name.slice(0, 1).toUpperCase()}</span><span className="hidden max-w-28 truncate text-sm font-medium text-slate-300 sm:block">{user.name}</span></Link></div></nav></header>
  </>;

  return <header className="sticky top-0 z-20 border-b border-slate-800/80 bg-slate-950/85 backdrop-blur-xl"><nav className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6" aria-label="Primary navigation"><Link href="/" className="flex items-center gap-2 font-semibold tracking-tight text-white"><span className="grid h-8 w-8 place-items-center rounded-xl bg-indigo-500">E</span>EnterpriseFlow</Link>{!isLoading && <div className="flex items-center gap-3 text-sm"><Link href="/login" className="text-slate-300 hover:text-white">Log in</Link><Link href="/register" className="button-primary py-2">Register</Link></div>}</nav></header>;
}


type IconName = "overview" | "projects" | "tasks" | "meetings" | "documents" | "search" | "analytics" | "knowledge" | "command" | "company" | "people" | "profile";

function NavigationIcon({ name }: { name: IconName }) {
  const common = { width: 18, height: 18, viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round" as const, strokeLinejoin: "round" as const, "aria-hidden": true as const };
  switch (name) {
    case "overview": return <svg {...common}><rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/></svg>;
    case "projects": return <svg {...common}><path d="M3 7.5h7l2 2h9v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M3 7V5a2 2 0 0 1 2-2h4l2 2h6"/></svg>;
    case "tasks": return <svg {...common}><rect x="4" y="4" width="16" height="16" rx="3"/><path d="m8 12 2.5 2.5L16 9"/></svg>;
    case "meetings": return <svg {...common}><rect x="3.5" y="5" width="17" height="16" rx="2"/><path d="M7.5 3v4M16.5 3v4M3.5 10h17M8 14h3M8 17h6"/></svg>;
    case "documents": return <svg {...common}><path d="M7 3.5h7l5 5V20a1 1 0 0 1-1 1H7a2 2 0 0 1-2-2V5.5a2 2 0 0 1 2-2Z"/><path d="M14 3.5V9h5M9 13h6M9 16.5h6"/></svg>;
    case "search": return <SearchIcon />;
    case "analytics": return <svg {...common}><path d="M4 20V11M10 20V5M16 20v-8M22 20H2"/><path d="m3 8 5-4 5 3 7-5"/></svg>;
    case "knowledge": return <svg {...common}><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="m19 14 .9 2.1L22 17l-2.1.9L19 20l-.9-2.1L16 17l2.1-.9L19 14ZM5 15l.7 1.3L7 17l-1.3.7L5 19l-.7-1.3L3 17l1.3-.7L5 15Z"/></svg>;
    case "command": return <svg {...common}><circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3"/><path d="M12 3.5v2M20.5 12h-2M12 20.5v-2M3.5 12h2"/></svg>;
    case "company": return <svg {...common}><path d="M4 21V4a1 1 0 0 1 1-1h10v18M15 8h5v13M2 21h20"/><path d="M8 7h3M8 11h3M8 15h3M18 12h.01M18 16h.01"/></svg>;
    case "people": return <svg {...common}><path d="M16 20v-1.5a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4V20M9.5 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM16 3.2a4 4 0 0 1 0 7.6M17 14.5a4 4 0 0 1 4 4V20"/></svg>;
    case "profile": return <svg {...common}><circle cx="12" cy="8" r="3.5"/><path d="M5 21v-1.5a7 7 0 0 1 14 0V21"/></svg>;
  }
}

function SearchIcon() { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true"><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></svg>; }
function MenuIcon() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>; }
function ExitIcon() { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M10 17l5-5-5-5M15 12H3M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></svg>; }
