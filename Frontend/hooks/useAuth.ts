"use client";

import { createContext, createElement, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { LoginRequest, RegisterRequest, User } from "../types/auth";
import * as authService from "../services/authService";

type AuthContextValue = {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<User>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const clearSession = useCallback(() => {
    authService.logout();
    setUser(null);
    setAccessToken(null);
  }, []);

  useEffect(() => {
    const storedSession = authService.getCurrentUser();
    if (storedSession) {
      setUser(storedSession.user);
      setAccessToken(storedSession.accessToken);
    }
    setIsLoading(false);

    window.addEventListener("enterpriseflow:unauthorized", clearSession);
    return () => window.removeEventListener("enterpriseflow:unauthorized", clearSession);
  }, [clearSession]);

  const login = useCallback(async (request: LoginRequest) => {
    const session = await authService.login(request);
    authService.saveSession(session);
    setUser(session.user);
    setAccessToken(session.accessToken);
  }, []);

  const register = useCallback((request: RegisterRequest) => authService.register(request), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      isAuthenticated: Boolean(user && accessToken),
      isLoading,
      login,
      register,
      logout: clearSession,
    }),
    [accessToken, clearSession, isLoading, login, register, user],
  );

  return createElement(AuthContext.Provider, { value }, children);
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider.");
  }
  return context;
}
