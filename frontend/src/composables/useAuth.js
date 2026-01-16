/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { ref } from "vue";

const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const TOKEN_KEY = "fairshare.jwt";

const request = async (path, options) => {
  const token = typeof window !== "undefined" ? window.localStorage.getItem(TOKEN_KEY) : null;
  const headers = {
    ...(options?.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
  const response = await fetch(`${apiBase}${path}`, { ...options, headers });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with ${response.status}`);
  }
  return response.json();
};

export const useAuth = () => {
  const ready = ref(false);
  const setupRequired = ref(true);
  const loading = ref(false);
  const error = ref("");

  const setToken = (token) => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(TOKEN_KEY, token);
    }
  };

  const clearToken = () => {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(TOKEN_KEY);
    }
  };

  const hasToken = () => {
    if (typeof window === "undefined") {
      return false;
    }
    return Boolean(window.localStorage.getItem(TOKEN_KEY));
  };

  const fetchStatus = async () => {
    loading.value = true;
    error.value = "";
    try {
      const result = await request("/api/auth/status");
      setupRequired.value = !result.ready;
      ready.value = true;
    } catch (err) {
      error.value = err?.message || "Status konnte nicht geladen werden.";
      setupRequired.value = false;
      ready.value = false;
    } finally {
      loading.value = false;
    }
  };

  const setup = async (username, password) => {
    loading.value = true;
    error.value = "";
    try {
      const response = await request("/api/auth/setup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });
      setToken(response.token);
      setupRequired.value = false;
      return true;
    } catch (err) {
      error.value = err?.message || "Setup fehlgeschlagen.";
      return false;
    } finally {
      loading.value = false;
    }
  };

  const login = async (username, password) => {
    loading.value = true;
    error.value = "";
    try {
      const response = await request("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });
      setToken(response.token);
      return true;
    } catch (err) {
      error.value = err?.message || "Login fehlgeschlagen.";
      return false;
    } finally {
      loading.value = false;
    }
  };

  const fetchMe = async () => {
    try {
      return await request("/api/auth/me");
    } catch (err) {
      error.value = err?.message || "Benutzerdaten konnten nicht geladen werden.";
      return null;
    }
  };

  const changePassword = async (currentPassword, newPassword) => {
    loading.value = true;
    error.value = "";
    try {
      await request("/api/auth/change-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPassword, newPassword })
      });
      return true;
    } catch (err) {
      error.value = err?.message || "Passwort konnte nicht geaendert werden.";
      return false;
    } finally {
      loading.value = false;
    }
  };

  return {
    ready,
    setupRequired,
    loading,
    error,
    fetchStatus,
    setup,
    login,
    fetchMe,
    changePassword,
    hasToken,
    clearToken
  };
};
