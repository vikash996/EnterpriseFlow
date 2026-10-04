import type { AuthResponse, LoginRequest, RegisterRequest, User } from "../types/auth";
import { apiRequest } from "./api";

const SESSION_STORAGE_KEY = "enterpriseflow.auth-session";

type StoredSession = {
  accessToken: string;
  user: User;
};

export async function register(request: RegisterRequest): Promise<User> {
  return apiRequest<User>("/api/auth/register", {
    method: "POST",
    body: request,
  });
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: request,
  });
}

export function saveSession(session: AuthResponse): void {
  if (typeof window === "undefined") {
    return;
  }
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function getCurrentUser(): StoredSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const serializedSession = localStorage.getItem(SESSION_STORAGE_KEY);
  if (!serializedSession) {
    return null;
  }

  try {
    const session = JSON.parse(serializedSession) as StoredSession;
    return isStoredSession(session) ? session : null;
  } catch {
    logout();
    return null;
  }
}

export function logout(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem(SESSION_STORAGE_KEY);
  }
}

function isStoredSession(value: unknown): value is StoredSession {
  if (!value || typeof value !== "object") {
    return false;
  }
  const session = value as Partial<StoredSession>;
  return typeof session.accessToken === "string" && Boolean(session.accessToken) && Boolean(session.user);
}
