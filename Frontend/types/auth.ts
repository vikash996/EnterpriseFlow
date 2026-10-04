export type UserRole = "ADMIN" | "MEMBER";

export type User = {
  id: string | null;
  name: string;
  email: string;
  role: UserRole;
  membershipStatus: "PENDING" | "ACTIVE" | "REJECTED";
  companyId: string | null;
  companyName: string | null;
  createdAt: string | null;
};

export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
  companyId: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  user: User;
};

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export type ValidationErrors = Record<string, string>;
