/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
export const TOKEN_KEY = "fairshare.jwt";

let refreshPromise = null;

const readErrorMessage = async (response) => {
  const text = await response.text();
  return text || `Request failed with ${response.status}`;
};

export const getStoredToken = () =>
  typeof window !== "undefined" ? window.localStorage.getItem(TOKEN_KEY) : null;

export const hasStoredToken = () => Boolean(getStoredToken());

export const setStoredToken = (token) => {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(TOKEN_KEY, token);
  }
};

export const clearStoredToken = () => {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(TOKEN_KEY);
  }
};

export const refreshAccessToken = async () => {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    const response = await fetch(`${apiBase}/api/auth/refresh`, {
      method: "POST",
      credentials: "include"
    });
    if (!response.ok) {
      clearStoredToken();
      return false;
    }
    const payload = await response.json().catch(() => null);
    if (!payload?.token) {
      clearStoredToken();
      return false;
    }
    setStoredToken(payload.token);
    return true;
  })();
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
};

const shouldRetryOnUnauthorized = (path) => ![
  "/api/auth/login",
  "/api/auth/setup",
  "/api/auth/refresh",
  "/api/auth/status"
].includes(path);

export const requestJson = async (path, options = {}, allowRefreshRetry = true) => {
  const { headers: optionHeaders, skipAuth = false, ...fetchOptions } = options;
  const token = skipAuth ? null : getStoredToken();
  const headers = {
    ...(optionHeaders || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };

  const response = await fetch(`${apiBase}${path}`, {
    ...fetchOptions,
    headers,
    credentials: "include"
  });

  if (response.status === 401 && allowRefreshRetry && shouldRetryOnUnauthorized(path)) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return requestJson(path, options, false);
    }
  }

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
};

