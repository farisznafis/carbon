import { apiFetch, parseJsonResponse } from "@/api/_shared/httpClient";
import type { UserList } from "./types";

interface UsersResponse {
  statusCode?: string;
  users?: UserList[];
  data?: UserList[];
  message?: string;
}

export async function getUsers(): Promise<{ statusCode: string; users: UserList[] }> {
  const res = await apiFetch('/user', {
    method: 'GET',
  });

  const { data: json, rawBody } = await parseJsonResponse<UsersResponse>(res);

  if (!res.ok) throw new Error(json?.message || rawBody || `Failed to fetch users: HTTP ${res.status}`);

  const users = Array.isArray(json?.users) ? json.users : Array.isArray(json?.data) ? json.data : [];
  return { statusCode: json?.statusCode || "OK", users };
}
