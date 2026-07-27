/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { ref } from "vue";
import {
  clearStoredToken,
  hasStoredToken,
  refreshAccessToken,
  requestJson,
  setStoredToken
} from "./authHttp";

export const useAuth = () => {
  const ready = ref(false);
  const setupRequired = ref(true);
  const loading = ref(false);
  const error = ref("");

  const setToken = (token) => {
    setStoredToken(token);
  };

  const clearToken = () => {
    clearStoredToken();
  };

  const hasToken = () => hasStoredToken();

  const fetchStatus = async () => {
    loading.value = true;
    error.value = "";
    try {
      const result = await requestJson("/api/auth/status", { skipAuth: true });
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
      const response = await requestJson("/api/auth/setup", {
        method: "POST",
        skipAuth: true,
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
      const response = await requestJson("/api/auth/login", {
        method: "POST",
        skipAuth: true,
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
      return await requestJson("/api/auth/me");
    } catch (err) {
      error.value = err?.message || "Benutzerdaten konnten nicht geladen werden.";
      return null;
    }
  };

  const changePassword = async (currentPassword, newPassword) => {
    loading.value = true;
    error.value = "";
    try {
      await requestJson("/api/auth/change-password", {
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

  const restoreSession = async () => {
    if (hasToken()) {
      return true;
    }
    return refreshAccessToken();
  };

  const logout = async () => {
    try {
      await requestJson("/api/auth/logout", { method: "POST" }, false);
    } catch (_err) {
      // Local cleanup still logs the user out if backend logout fails.
    } finally {
      clearToken();
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
    restoreSession,
    fetchMe,
    changePassword,
    logout,
    hasToken,
    clearToken
  };
};
