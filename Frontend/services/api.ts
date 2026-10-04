import type { ApiResponse, ValidationErrors } from "../types/auth";

const apiUrl = process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fieldErrors?: ValidationErrors,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  token?: string | null;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  if (!apiUrl) {
    throw new ApiError("The application API URL is not configured.", 0);
  }

  const { body, token, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers);
  headers.set("Accept", "application/json");

  if (body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(`${apiUrl}${path}`, {
      ...requestOptions,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError("Unable to reach the server. Please try again shortly.", 0);
  }

  const payload = (await response.json().catch(() => null)) as ApiResponse<T | ValidationErrors> | null;
  const message = payload?.message || "The request could not be completed.";
  const fieldErrors = payload && isValidationErrors(payload.data) ? payload.data : undefined;

  if (!response.ok || !payload?.success) {
    if (response.status === 401 && typeof window !== "undefined") {
      window.dispatchEvent(new Event("enterpriseflow:unauthorized"));
    }
    throw new ApiError(message, response.status, fieldErrors);
  }

  return payload.data as T;
}

function isValidationErrors(value: unknown): value is ValidationErrors {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}
