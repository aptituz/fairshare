<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <AppShell
    :currentView="currentView"
    :currentSubView="currentSubView"
    :datenerfassungNavItems="datenerfassungNavItems"
    :summary="summary"
    :summaryMonth="summaryMonth"
    @navigate="handleNavigate"
    @month-change="handleMonthChange"
  >
    <v-alert v-if="error" type="error" variant="tonal" class="mb-6">
      {{ error }}
    </v-alert>

    <OverviewView
      v-if="currentView === 'overview'"
      :summary="summary"
      :categories="categories"
      :formatCurrency="formatCurrency"
      :categoryRank="categoryRank"
    />

    <KostenverteilungView
      v-else-if="currentView === 'kostenverteilung'"
      :summary="summary"
      :formatCurrency="formatCurrency"
    />

    <DatenerfassungView
      v-else-if="currentView === 'datenerfassung'"
      :currentSubView="currentSubView"
      :persons="persons"
      :budgetItems="budgetItems"
      :summaryMonth="summaryMonth"
      :budgetItemSaving="budgetItemSaving"
      :incomeCategoryOptions="incomeCategoryOptions"
      :expenseCategoryOptions="expenseCategoryOptions"
      :formatCurrency="formatCurrency"
      :categoryPathLabel="categoryPathLabel"
      :createBudgetItem="createBudgetItem"
      :updateBudgetItem="updateBudgetItem"
      :deleteBudgetItem="deleteBudgetItem"
      :overrideBudgetItemForMonth="overrideBudgetItemForMonth"
      :createCategoryCorrection="createCategoryCorrection"
      :createCategory="createCategory"
    />

    <StammdatenView
      v-else-if="currentView === 'stammdaten'"
      :currentSubView="currentSubView"
      :incomeCategoryOptions="incomeCategoryOptions"
      :expenseCategoryOptions="expenseCategoryOptions"
      :persons="persons"
      :categorySaving="categorySaving"
      :personSaving="personSaving"
      :createCategory="createCategory"
      :updateCategory="updateCategory"
      :deleteCategory="deleteCategory"
      :createPerson="createPerson"
      :updatePerson="updatePerson"
    />
  </AppShell>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppShell from "./components/AppShell.vue";
import OverviewView from "./views/OverviewView.vue";
import KostenverteilungView from "./views/KostenverteilungView.vue";
import DatenerfassungView from "./views/DatenerfassungView.vue";
import StammdatenView from "./views/StammdatenView.vue";
import { useBudgetData } from "./composables/useBudgetData";

const {
  categories,
  persons,
  budgetItems,
  summary,
  summaryMonth,
  error,
  categorySaving,
  personSaving,
  budgetItemSaving,
  incomeCategoryOptions,
  expenseCategoryOptions,
  refreshAll,
  createCategory,
  updateCategory,
  deleteCategory,
  createPerson,
  updatePerson,
  createBudgetItem,
  updateBudgetItem,
  deleteBudgetItem,
  overrideBudgetItemForMonth,
  createCategoryCorrection,
  categoryPathLabel,
  categoryRank,
  setSummaryMonth
} = useBudgetData();

const route = useRoute();
const router = useRouter();

const currentView = computed(() => {
  if (route.name === "cost-split") {
    return "kostenverteilung";
  }
  if (route.name === "income" || route.name === "expenses") {
    return "datenerfassung";
  }
  if (route.name === "master-data") {
    return "stammdaten";
  }
  return "overview";
});

const currentSubView = computed(() => {
  if (route.name === "income" || route.name === "expenses") {
    const scope = route.params.scope || "shared";
    const section = route.name === "income" ? "income" : "expenses";
    return `${section}/${scope}`;
  }
  if (route.name === "master-data") {
    return route.params.section || "categories";
  }
  return "income/shared";
});

const datenerfassungNavItems = computed(() => {
  const items = [
    { key: "income/shared", title: "Gemeinsame Einnahmen" },
    { key: "expenses/shared", title: "Gemeinsame Ausgaben" }
  ];
  persons.value
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name))
    .forEach((person) => {
      items.push({ key: `income/person/${person.id}`, title: `Einnahmen ${person.name}` });
      items.push({ key: `expenses/person/${person.id}`, title: `Ausgaben ${person.name}` });
    });
  return items;
});

const currencyFormatter = new Intl.NumberFormat("de-DE", {
  style: "currency",
  currency: "EUR"
});

const formatCurrency = (value) => {
  if (value === null || value === undefined || value === "") {
    return "—";
  }
  const numeric = typeof value === "string" ? Number(value) : Number(value);
  if (Number.isNaN(numeric)) {
    return String(value);
  }
  return currencyFormatter.format(numeric);
};

const handleMonthChange = async (value) => {
  setSummaryMonth(value);
  await refreshAll();
};

const handleNavigate = ({ view, subView }) => {
  if (view === "kostenverteilung") {
    router.push({ name: "cost-split" });
    return;
  }
  if (view === "datenerfassung") {
    const nextSubView = subView || "income/shared";
    if (nextSubView.startsWith("income/")) {
      router.push({ name: "income", params: { scope: nextSubView.replace(/^income\//, "") } });
      return;
    }
    if (nextSubView.startsWith("expenses/")) {
      router.push({ name: "expenses", params: { scope: nextSubView.replace(/^expenses\//, "") } });
      return;
    }
    router.push({ name: "income", params: { scope: "shared" } });
    return;
  }
  if (view === "stammdaten") {
    router.push({ name: "master-data", params: { section: subView || "categories" } });
    return;
  }
  router.push({ name: "overview" });
};

onMounted(() => {
  refreshAll();
});
</script>
