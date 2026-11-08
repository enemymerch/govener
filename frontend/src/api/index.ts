import { createApiClient } from "./client";
export const api = createApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL || "/api/v1",
});
export type * from "./generated";
export { ApiError } from "./client";
