<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-row>
    <v-col cols="12">
      <h2 class="text-h5 mb-2">Datenerfassung</h2>
      <p class="text-body-2">
        Pflege Einnahmen und Ausgaben als wiederkehrende Posten.
      </p>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="12">
      <BudgetItemPanel
        :title="currentDatenerfassungView.title"
        :description="currentDatenerfassungView.description"
        :placeholder="currentDatenerfassungView.placeholder"
        :type="currentDatenerfassungView.type"
        :categories="currentDatenerfassungView.type === 'INCOME' ? incomeCategoryOptions : expenseCategoryOptions"
        :persons="persons"
        :budgetItems="filteredBudgetItems(currentDatenerfassungView.type, currentDatenerfassungView.personId)"
        :summaryMonth="summaryMonth"
        :fetchExpenseYearlySummary="fetchExpenseYearlySummary"
        :saving="budgetItemSaving"
        :formatCurrency="formatCurrency"
        :categoryPathLabel="categoryPathLabel"
        :onCreateBudgetItem="createBudgetItem"
        :onUpdateBudgetItem="updateBudgetItem"
        :onDeleteBudgetItem="deleteBudgetItem"
        :onOverrideBudgetItemForMonth="overrideBudgetItemForMonth"
        :onChangeBudgetItemValue="changeBudgetItemValue"
        :onSuspendBudgetItem="suspendBudgetItem"
        :onResumeBudgetItem="resumeBudgetItem"
        :onFetchBudgetItemHistory="fetchBudgetItemHistory"
        :onDeleteBudgetItemSuspension="deleteBudgetItemSuspension"
        :onCreateCategoryCorrection="createCategoryCorrection"
        :onCreateCategory="createCategory"
        :fixedPersonId="currentDatenerfassungView.personId"
        :showPersonSelector="false"
        :allowCategoryCreate="true"
      />
    </v-col>
  </v-row>
</template>

<script setup>
import { computed } from "vue";
import BudgetItemPanel from "../components/BudgetItemPanel.vue";

const props = defineProps({
  currentSubView: { type: String, required: true },
  persons: { type: Array, required: true },
  budgetItems: { type: Array, required: true },
  summaryMonth: { type: String, required: true },
  fetchExpenseYearlySummary: { type: Function, required: true },
  budgetItemSaving: { type: Boolean, required: true },
  incomeCategoryOptions: { type: Array, required: true },
  expenseCategoryOptions: { type: Array, required: true },
  formatCurrency: { type: Function, required: true },
  categoryPathLabel: { type: Function, required: true },
  createBudgetItem: { type: Function, required: true },
  updateBudgetItem: { type: Function, required: true },
  deleteBudgetItem: { type: Function, required: true },
  overrideBudgetItemForMonth: { type: Function, required: true },
  changeBudgetItemValue: { type: Function, required: true },
  suspendBudgetItem: { type: Function, required: true },
  resumeBudgetItem: { type: Function, required: true },
  fetchBudgetItemHistory: { type: Function, required: true },
  deleteBudgetItemSuspension: { type: Function, required: true },
  createCategoryCorrection: { type: Function, required: true },
  createCategory: { type: Function, required: true }
});

const filteredBudgetItems = (type, personId) => {
  const monthStart = new Date(`${props.summaryMonth}-01T00:00:00`);
  const monthEnd = new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0, 23, 59, 59);

  return props.budgetItems.filter((item) => {
    if (item.type !== type) {
      return false;
    }
    const startDate = item.startDate ? new Date(`${item.startDate}T00:00:00`) : null;
    const endDate = item.endDate ? new Date(`${item.endDate}T23:59:59`) : null;
    if (startDate && startDate > monthEnd) {
      return false;
    }
    if (endDate && endDate < monthStart) {
      return false;
    }
    if (personId == null) {
      return !item.person;
    }
    return item.person?.id === personId;
  });
};

const currentDatenerfassungView = computed(() => {
  const key = props.currentSubView || "income/shared";
  const [section, ...rest] = key.split("/");
  const type = section === "expenses" ? "EXPENSE" : "INCOME";
  const scope = rest.join("/") || "shared";
  if (scope === "shared") {
    return {
      title: type === "INCOME" ? "Gemeinsame Einnahmen" : "Gemeinsame Ausgaben",
      description:
        type === "INCOME"
          ? "Plane regelmaessige Einnahmen wie Gehalt oder Bonus."
          : "Erfasse laufende Ausgaben fuer die Monatsplanung.",
      placeholder: type === "INCOME" ? "Gehalt" : "Miete",
      type,
      personId: null
    };
  }
  const match = scope.match(/^person\/(\d+)$/);
  const personId = match ? Number(match[1]) : null;
  const personName = props.persons.find((person) => person.id === personId)?.name || "Person";
  return {
    title: type === "INCOME" ? `Einnahmen ${personName}` : `Ausgaben ${personName}`,
    description:
      type === "INCOME"
        ? "Plane regelmaessige Einnahmen wie Gehalt oder Bonus."
        : "Erfasse laufende Ausgaben fuer die Monatsplanung.",
    placeholder: type === "INCOME" ? "Gehalt" : "Miete",
    type,
    personId
  };
});
</script>
