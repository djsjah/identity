export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ErrorResponse = {
  message?: unknown;
};

type CsrfResponse = {
  token?: unknown;
  headerName?: unknown;
};

const API_URL = (import.meta.env.VITE_API_URL || "").replace(/\/$/, "");

const readBody = async (response: Response): Promise<unknown> => {
  if (response.status === 204) return null;

  const contentType = response.headers.get("Content-Type") || "";
  if (!contentType.includes("application/json")) return null;

  try {
    return await response.json();
  } catch {
    return null;
  }
};

const getErrorMessage = (body: unknown, status: number): string => {
  if (body && typeof body === "object" && "message" in body) {
    const message = (body as ErrorResponse).message;
    if (
      typeof message === "string" &&
      message.length <= 250 &&
      message.trim()
    ) {
      return message;
    }
  }

  if (status === 401) return "Authentication required";
  if (status === 403) return "Access denied";
  return "Request failed";
};

const csrfHeaders = async (signal?: AbortSignal): Promise<Headers> => {
  const response = await fetch(`${API_URL}/auth/csrf`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    redirect: "error",
    signal,
  });

  const data = (await readBody(response)) as CsrfResponse | null;
  if (!response.ok) {
    throw new ApiError(response.status, getErrorMessage(data, response.status));
  }

  if (
    !data ||
    typeof data.token !== "string" ||
    !data.token ||
    data.headerName !== "X-XSRF-TOKEN"
  ) {
    throw new ApiError(0, "CSRF token is unavailable");
  }

  return new Headers({ "X-XSRF-TOKEN": data.token });
};

export const apiRequest = async <T>(
  path: string,
  options: RequestInit = {},
): Promise<T> => {
  const method = (options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await csrfHeaders(options.signal || undefined);
    csrf.forEach((value, name) => headers.set(name, value));
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: "include",
    cache: "no-store",
    redirect: "error",
  });

  const body = await readBody(response);
  if (!response.ok) {
    throw new ApiError(response.status, getErrorMessage(body, response.status));
  }

  return body as T;
};
