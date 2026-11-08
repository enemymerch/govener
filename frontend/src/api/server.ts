import { api, ApiError } from "./index";
import type { Operations } from "./generated";

/** Exhaust cursor pages rather than treating the first page as a complete dataset. */
export async function collectPages<T>(
  load: (
    cursor?: string,
  ) => Promise<{ items: T[]; nextCursor: string | null; hasMore: boolean }>,
  signal?: AbortSignal,
): Promise<T[]> {
  const items: T[] = [];
  const cursors = new Set<string>();
  let cursor: string | undefined;
  do {
    if (signal?.aborted) throw new DOMException("Aborted", "AbortError");
    const page = await load(cursor);
    items.push(...page.items);
    if (!page.hasMore) return items;
    if (!page.nextCursor || cursors.has(page.nextCursor))
      throw new ApiError(502, "INVALID_RESPONSE");
    cursors.add(page.nextCursor);
    cursor = page.nextCursor;
  } while (true);
}
export function all<K extends keyof Operations, T>(
  operation: K,
  communityId: string | undefined,
  signal: AbortSignal,
  query: Record<string, unknown> = {},
  extraParams: Record<string, string> = {},
): Promise<T[]> {
  return collectPages<T>(
    (cursor) =>
      api.execute(operation, {
        params: { ...(communityId ? { communityId } : {}), ...extraParams },
        query: { ...query, cursor, limit: 100 },
        signal,
      } as never) as Promise<{
        items: T[];
        nextCursor: string | null;
        hasMore: boolean;
      }>,
    signal,
  );
}
export function errorKey(error: unknown): string {
  if (!(error instanceof ApiError)) return "live.error";
  if (error.code === "INVALID_PATH_PARAMETER") return "live.notFound";
  if (error.code === "INVALID_CREDENTIALS") return "auth.invalidCredentials";
  if (error.status === 409) return "live.conflict";
  if (error.status === 403) return "live.forbidden";
  if (error.status === 404) return "live.notFound";
  if (error.status === 429) return "live.rateLimited";
  if (error.status === 422 || error.status === 400) return "live.invalid";
  if (error.status === 401) return "auth.expired";
  if (error.status === 0) return "live.connectionError";
  return "live.error";
}
function canonical(value: unknown): string {
  return JSON.stringify(value, (_, item) =>
    item instanceof File
      ? {
          name: item.name,
          size: item.size,
          lastModified: item.lastModified,
          type: item.type,
        }
      : item && typeof item === "object" && !Array.isArray(item)
        ? Object.fromEntries(
            Object.keys(item)
              .sort()
              .map((key) => [key, item[key]]),
          )
        : item,
  );
}
/** Persist only a digest and random intent key, never credentials or financial payloads. */
export async function financialWrite<T>(
  scope: string,
  payload: unknown,
  work: (key: string) => Promise<T>,
): Promise<T> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(scope + ":" + canonical(payload)),
  );
  const storageKey =
    "govener.intent." +
    Array.from(new Uint8Array(digest), (value) =>
      value.toString(16).padStart(2, "0"),
    ).join("");
  let key = intentKeys.get(storageKey);
  try {
    key ??= sessionStorage.getItem(storageKey) ?? undefined;
  } catch {
    /* In-memory retries still retain their key. */
  }
  key ??= crypto.randomUUID();
  intentKeys.set(storageKey, key);
  try {
    sessionStorage.setItem(storageKey, key);
  } catch {
    /* Storage is optional. */
  }
  const clear = () => {
    intentKeys.delete(storageKey);
    try {
      sessionStorage.removeItem(storageKey);
    } catch {
      /* Optional storage. */
    }
  };
  try {
    const result = await work(key);
    clear();
    return result;
  } catch (error) {
    if (error instanceof ApiError && error.status >= 400 && error.status < 500)
      clear();
    throw error;
  }
}
const intentKeys = new Map<string, string>();
