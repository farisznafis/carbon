/**
 * API Configuration
 * ==================
 * Shared configuration and utility functions for API calls
 */

/**
 * Base URL for all API requests
 * TODO: Replace with your actual backend API URL
 */
// export const BASE_URL = "http://localhost:8081/api"; // For local development
export const BASE_URL = "/api";
// export const BASE_URL = "https://wannabe-kilowatt-overeater.ngrok-free.dev";

/**
 * Helper function to get authentication headers
 * Retrieves the stored auth token from localStorage
 */
function getFallbackToken(): string | null {
  const user = storage.get(storageKeys.user);

  if (!user) {
    return null;
  }

  try {
    const parsedUser = JSON.parse(user) as { token?: unknown };
    return typeof parsedUser.token === "string" ? parsedUser.token : null;
  } catch {
    return null;
  }
}

export const getAuthHeaders = (): HeadersInit => {
  const authToken = storage.get(storageKeys.authToken);
  const fallbackToken = getFallbackToken();
  const token = authToken || fallbackToken;

  return {
    "Content-Type": "application/json",
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

import { storage, storageKeys } from '@/lib/storage';
