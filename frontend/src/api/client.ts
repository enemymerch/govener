import { operationMetadata, type Operations, type Problem } from "./generated";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    public readonly problem?: Problem,
  ) {
    super(problem?.detail ?? code);
    this.name = "ApiError";
  }
}
type Input<K extends keyof Operations> = Omit<Operations[K], "response"> & {
  signal?: AbortSignal;
};
type Options = {
  baseUrl?: string;
  fetcher?: typeof fetch;
  timeoutMs?: number;
  language?: () => string;
  onUnauthorized?: () => void;
};
export function createApiClient(options: Options = {}) {
  const fetcher = options.fetcher ?? fetch;
  const base = (options.baseUrl ?? "/api/v1").replace(/\/$/, "");
  let csrfToken = "";
  async function execute<K extends keyof Operations>(
    operation: K,
    input: Input<K>,
  ): Promise<Operations[K]["response"]> {
    const meta = operationMetadata[operation];
    const request = input as {
      params?: Record<string, string>;
      query?: Record<string, string | number | boolean | undefined>;
      body?: unknown;
      idempotencyKey?: string;
      signal?: AbortSignal;
    };
    let url =
      base +
      meta.path.replace(/\{(\w+)\}/g, (_, key: string) => {
        const value = request.params?.[key];
        if (!value || !/^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$/.test(value))
          throw new ApiError(0, "INVALID_PATH_PARAMETER");
        return encodeURIComponent(value);
      });
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(request.query ?? {}))
      if (value !== undefined) query.set(key, String(value));
    if (query.size) url += "?" + query.toString();
    if (meta.financial && !request.idempotencyKey)
      throw new ApiError(0, "IDEMPOTENCY_KEY_REQUIRED");
    if (meta.csrf && !csrfToken) throw new ApiError(403, "CSRF_TOKEN_REQUIRED");
    const headers: Record<string, string> = {
      Accept: meta.binary ? "application/octet-stream" : "application/json",
      "Accept-Language":
        options.language?.() ?? document.documentElement.lang ?? "en",
    };
    if (meta.csrf) headers["X-CSRF-Token"] = csrfToken;
    if (request.idempotencyKey)
      headers["Idempotency-Key"] = request.idempotencyKey;
    if (request.body !== undefined && !meta.multipart)
      headers["Content-Type"] = "application/json";
    const controller = new AbortController();
    let timedOut = false;
    const abort = () => controller.abort();
    if (request.signal?.aborted) controller.abort();
    request.signal?.addEventListener("abort", abort, { once: true });
    const timer = setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, options.timeoutMs ?? 15000);
    try {
      const response = await fetcher(url, {
        method: meta.method,
        credentials: "include",
        headers,
        signal: controller.signal,
        body:
          request.body === undefined
            ? undefined
            : meta.multipart
              ? (request.body as FormData)
              : JSON.stringify(request.body),
      });
      if (response.status === 401) {
        if (options.onUnauthorized) options.onUnauthorized();
        else window.dispatchEvent(new Event("govener:unauthorized"));
      }
      if (!response.ok) {
        let problem: Problem | undefined;
        try {
          problem = await response.json();
        } catch {
          /* Some network gateways return non-JSON errors. */
        }
        throw new ApiError(
          response.status,
          problem?.code ?? "HTTP_ERROR",
          problem,
        );
      }
      if (response.status === 204 || response.status === 202)
        return undefined as Operations[K]["response"];
      if (meta.binary)
        return (await response.blob()) as Operations[K]["response"];
      let result: unknown;
      try {
        result = await response.json();
      } catch {
        throw new ApiError(502, "INVALID_RESPONSE");
      }
      if (
        result &&
        typeof result === "object" &&
        "csrfToken" in result &&
        typeof result.csrfToken === "string"
      )
        csrfToken = result.csrfToken;
      return result as Operations[K]["response"];
    } catch (failure) {
      if (failure instanceof ApiError) throw failure;
      if (request.signal?.aborted)
        throw new DOMException("Aborted", "AbortError");
      throw new ApiError(0, timedOut ? "TIMEOUT" : "NETWORK_ERROR");
    } finally {
      clearTimeout(timer);
      request.signal?.removeEventListener("abort", abort);
    }
  }
  return {
    execute,
    setCsrfToken(value: string) {
      csrfToken = value;
    },
    clearSession() {
      csrfToken = "";
    },
  };
}
