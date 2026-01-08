/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { computed, ref } from "vue";

const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const request = async (path, options) => {
  const response = await fetch(`${apiBase}${path}`, options);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with ${response.status}`);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
};

const flattenCategories = (items) =>
  items
    .slice()
    .sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0))
    .map((item) => ({
      ...item,
      depth: 0,
      label: item.name
    }));

const STORAGE_KEY = "fairshare.selectedMonth";

const currentMonth = () => {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  return `${date.getFullYear()}-${month}`;
};

export const useBudgetData = () => {
  const categories = ref([]);
  const persons = ref([]);
  const budgetItems = ref([]);
  const summary = ref(null);
  const error = ref("");
  const categorySaving = ref(false);
  const personSaving = ref(false);
  const budgetItemSaving = ref(false);
  const initialMonth = typeof window !== "undefined"
    ? window.localStorage.getItem(STORAGE_KEY) || currentMonth()
    : currentMonth();
  const summaryMonth = ref(initialMonth);

  const categoryOptions = computed(() => flattenCategories(categories.value));
  const incomeCategoryOptions = computed(() =>
    flattenCategories(categories.value.filter((category) => category.type === "INCOME"))
  );
  const expenseCategoryOptions = computed(() =>
    flattenCategories(categories.value.filter((category) => category.type === "EXPENSE"))
  );

  const fetchCategories = async () => {
    categories.value = await request("/api/categories");
  };

  const fetchPersons = async () => {
    persons.value = await request("/api/persons");
  };

  const fetchBudgetItems = async () => {
    const query = summaryMonth.value ? `?month=${summaryMonth.value}` : "";
    budgetItems.value = await request(`/api/budget-items${query}`);
  };

  const fetchSummary = async () => {
    const query = summaryMonth.value ? `?month=${summaryMonth.value}` : "";
    summary.value = await request(`/api/budget/monthly-summary${query}`);
  };

  const refreshAll = async () => {
    error.value = "";
    try {
      await Promise.all([fetchCategories(), fetchPersons(), fetchBudgetItems(), fetchSummary()]);
    } catch (err) {
      error.value = err?.message || "Daten konnten nicht geladen werden.";
    }
  };

  const setSummaryMonth = (value) => {
    summaryMonth.value = value;
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, value);
    }
  };

  const createCategory = async (type, name) => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return null;
    }
    categorySaving.value = true;
    error.value = "";
    try {
      const created = await request("/api/categories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: trimmedName, type })
      });
      await refreshAll();
      return created || null;
    } catch (err) {
      error.value = err?.message || "Kategorie konnte nicht gespeichert werden.";
      return null;
    } finally {
      categorySaving.value = false;
    }
  };

  const updateCategory = async (id, name) => {
    if (!name.trim()) {
      error.value = "Bitte einen gueltigen Kategorienamen angeben.";
      return false;
    }
    error.value = "";
    try {
      await request(`/api/categories/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Kategorie konnte nicht aktualisiert werden.";
      return false;
    }
  };

  const deleteCategory = async (id) => {
    error.value = "";
    try {
      await request(`/api/categories/${id}`, { method: "DELETE" });
      await refreshAll();
    } catch (err) {
      error.value = err?.message || "Kategorie konnte nicht geloescht werden.";
    }
  };

  const createPerson = async (name) => {
    if (!name.trim()) {
      return false;
    }
    personSaving.value = true;
    error.value = "";
    try {
      await request("/api/persons", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Person konnte nicht gespeichert werden.";
      return false;
    } finally {
      personSaving.value = false;
    }
  };

  const updatePerson = async (id, name) => {
    if (!name.trim()) {
      error.value = "Bitte einen gueltigen Personennamen angeben.";
      return false;
    }
    error.value = "";
    try {
      await request(`/api/persons/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Person konnte nicht aktualisiert werden.";
      return false;
    }
  };

  const categoryNameFromId = (categoryId) => {
    if (!categoryId) {
      return "";
    }
    const match = categories.value.find((category) => category.id === categoryId);
    return match?.name || "";
  };

  const resolveCategoryId = async (categoryId, type) => {
    if (categoryId === null || categoryId === "") {
      return null;
    }
    if (typeof categoryId === "object") {
      if (categoryId.id) {
        return categoryId.id;
      }
      const label = categoryId.name || categoryId.label || "";
      if (!label) {
        return null;
      }
      categoryId = label;
    }
    if (typeof categoryId === "number") {
      return categoryId;
    }
    const rawValue = String(categoryId).trim();
    if (!rawValue) {
      return null;
    }
    const existing = categories.value.find(
      (category) => category.type === type && category.name.toLowerCase() === rawValue.toLowerCase()
    );
    if (existing) {
      return existing.id;
    }
    const created = await request("/api/categories", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: rawValue, type })
    });
    await fetchCategories();
    return created?.id ?? null;
  };

  const createBudgetItem = async ({
    name,
    amount,
    categoryId,
    personId,
    type,
    frequency,
    startDate,
    endDate,
    planned
  }) => {
    const numericAmount = Number(amount);
    if (Number.isNaN(numericAmount)) {
      error.value = "Bitte Betrag angeben.";
      return false;
    }
    const normalizedCategoryId = await resolveCategoryId(categoryId, type);
    if (normalizedCategoryId === undefined) {
      return false;
    }
    const normalizedPersonId = personId === null || personId === "" ? null : Number(personId);
    if (normalizedPersonId !== null && Number.isNaN(Number(normalizedPersonId))) {
      error.value = "Bitte eine gueltige Person waehlen.";
      return false;
    }
    const itemName = name?.trim() || categoryNameFromId(normalizedCategoryId) || "Einnahme";
    budgetItemSaving.value = true;
    error.value = "";
    try {
      await request("/api/budget-items", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: itemName,
          amount: numericAmount,
          type,
          categoryId: normalizedCategoryId,
          personId: normalizedPersonId,
          frequency: frequency || null,
          startDate: startDate || null,
          endDate: endDate || null,
          planned: planned ?? true
        })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Transaktion konnte nicht gespeichert werden.";
      return false;
    } finally {
      budgetItemSaving.value = false;
    }
  };

  const updateBudgetItem = async (
    id,
    name,
    amount,
    categoryId,
    personId,
    type,
    frequency,
    startDate,
    endDate,
    planned
  ) => {
    const trimmedName = name.trim();
    const numericAmount = Number(amount);
    if (Number.isNaN(numericAmount)) {
      error.value = "Bitte einen gueltigen Betrag angeben.";
      return false;
    }
    const resolvedType =
      type || budgetItems.value.find((item) => item.id === id)?.type;
    if (!resolvedType) {
      error.value = "Bitte eine gueltige Kategorie waehlen.";
      return false;
    }
    const normalizedCategoryId = await resolveCategoryId(categoryId, resolvedType);
    if (normalizedCategoryId === undefined) {
      return false;
    }
    const normalizedPersonId = personId === null || personId === "" ? null : Number(personId);
    if (normalizedPersonId !== null && Number.isNaN(Number(normalizedPersonId))) {
      error.value = "Bitte eine gueltige Person waehlen.";
      return false;
    }
    const resolvedName = trimmedName || categoryNameFromId(normalizedCategoryId) || "Einnahme";
    error.value = "";
    try {
      await request(`/api/budget-items/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: resolvedName,
          amount: numericAmount,
          categoryId: normalizedCategoryId,
          personId: normalizedPersonId,
          frequency: frequency || null,
          startDate: startDate || null,
          endDate: endDate || null,
          planned: planned ?? null
        })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Transaktion konnte nicht aktualisiert werden.";
      return false;
    }
  };

  const overrideBudgetItemForMonth = async (id, month, amount) => {
    const numericAmount = Number(amount);
    if (Number.isNaN(numericAmount)) {
      error.value = "Bitte einen gueltigen Betrag angeben.";
      return false;
    }
    if (!month) {
      error.value = "Bitte einen gueltigen Monat waehlen.";
      return false;
    }
    error.value = "";
    try {
      await request(`/api/budget-items/${id}/month-override`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ month, amount: numericAmount })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Monatswert konnte nicht aktualisiert werden.";
      return false;
    }
  };

  const suspendBudgetItem = async (id, startMonth, endMonth) => {
    if (!startMonth) {
      error.value = "Bitte einen gueltigen Startmonat waehlen.";
      return false;
    }
    error.value = "";
    try {
      await request(`/api/budget-items/${id}/suspend`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          startMonth,
          endMonth: endMonth || null
        })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Budget-Posten konnte nicht ausgesetzt werden.";
      return false;
    }
  };

  const resumeBudgetItem = async (id, startMonth) => {
    if (!startMonth) {
      error.value = "Bitte einen gueltigen Startmonat waehlen.";
      return false;
    }
    error.value = "";
    try {
      await request(`/api/budget-items/${id}/resume`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ startMonth })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Budget-Posten konnte nicht fortgesetzt werden.";
      return false;
    }
  };

  const fetchBudgetItemHistory = async (id) => {
    if (!id) {
      error.value = "Bitte einen gueltigen Budget-Posten waehlen.";
      return [];
    }
    error.value = "";
    try {
      return await request(`/api/budget-items/${id}/history`);
    } catch (err) {
      error.value = err?.message || "Historie konnte nicht geladen werden.";
      return [];
    }
  };

  const deleteBudgetItemSuspension = async (id) => {
    if (!id) {
      return false;
    }
    error.value = "";
    try {
      await request(`/api/budget-items/suspensions/${id}`, { method: "DELETE" });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Aussetzung konnte nicht geloescht werden.";
      return false;
    }
  };

  const deleteBudgetItem = async (id) => {
    error.value = "";
    try {
      await request(`/api/budget-items/${id}`, { method: "DELETE" });
      await refreshAll();
    } catch (err) {
      error.value = err?.message || "Transaktion konnte nicht geloescht werden.";
    }
  };

  const createCategoryCorrection = async ({ categoryId, personId, month, actualAmount }) => {
    const numericAmount = Number(actualAmount);
    if (!categoryId) {
      error.value = "Bitte eine Kategorie waehlen.";
      return false;
    }
    if (!month) {
      error.value = "Bitte einen gueltigen Monat waehlen.";
      return false;
    }
    if (Number.isNaN(numericAmount)) {
      error.value = "Bitte einen gueltigen Betrag angeben.";
      return false;
    }
    error.value = "";
    try {
      await request("/api/budget-items/category-correction", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          categoryId,
          personId: personId ?? null,
          month,
          actualAmount: numericAmount
        })
      });
      await refreshAll();
      return true;
    } catch (err) {
      error.value = err?.message || "Korrektur konnte nicht gespeichert werden.";
      return false;
    }
  };

  const categoryPathLabel = (categoryId) => {
    if (!categoryId) {
      return "Keine Kategorie";
    }
    const match = categories.value.find((category) => category.id === categoryId);
    return match?.name || "Keine Kategorie";
  };

  const categoryRank = (categoryId, fallbackName) => {
    if (!categoryId) {
      return Number.MAX_SAFE_INTEGER;
    }
    const match = categories.value.find((category) => category.id === categoryId);
    if (match) {
      return match.rank ?? Number.MAX_SAFE_INTEGER;
    }
    const fallbackMatch = categories.value.find((category) => category.name === fallbackName);
    return fallbackMatch?.rank ?? Number.MAX_SAFE_INTEGER;
  };

  return {
    categories,
    persons,
    budgetItems,
    summary,
    summaryMonth,
    error,
    categorySaving,
    personSaving,
    budgetItemSaving,
    categoryOptions,
    incomeCategoryOptions,
    expenseCategoryOptions,
    refreshAll,
    setSummaryMonth,
    createCategory,
    updateCategory,
    deleteCategory,
    createPerson,
    updatePerson,
    createBudgetItem,
    updateBudgetItem,
    deleteBudgetItem,
    overrideBudgetItemForMonth,
    suspendBudgetItem,
    resumeBudgetItem,
    fetchBudgetItemHistory,
    deleteBudgetItemSuspension,
    createCategoryCorrection,
    categoryPathLabel,
    categoryRank
  };
};
