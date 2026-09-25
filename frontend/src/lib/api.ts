// Error envelope every FoC backend service returns (see AGENTS.md "API conventions").
export type ApiErrorBody = {
  status: number
  error: string
  message: string
  path?: string
  timestamp?: string
  details?: { field?: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorBody["details"]

  constructor(status: number, code: string, message: string, details?: ApiErrorBody["details"]) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

export type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE"
  body?: unknown
  token?: string | null
  signal?: AbortSignal
}

export async function apiRequest<T>(baseUrl: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" }
  if (options.body !== undefined) headers["Content-Type"] = "application/json"
  if (options.token) headers.Authorization = `Bearer ${options.token}`

  let response: Response
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
      cache: "no-store",
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error
    throw new ApiError(0, "NETWORK_ERROR", "Could not reach the server. Check your connection and try again.")
  }

  if (response.status === 204) return undefined as T

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const body = payload as Partial<ApiErrorBody> | null
    throw new ApiError(
      response.status,
      body?.error ?? "HTTP_ERROR",
      body?.message ?? `Request failed with status ${response.status}.`,
      body?.details,
    )
  }
  return payload as T
}
