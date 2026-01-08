<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-list density="compact">
    <template v-if="groupedIncomeByPerson.length">
      <div v-for="group in groupedIncomeByPerson" :key="group.name" class="mb-4">
        <div class="text-subtitle-2 font-weight-bold mb-2">
          {{ group.name }}
        </div>
        <div class="d-flex flex-column ga-2">
          <div
            v-for="item in group.items"
            :key="item.budgetItemId ?? item.budgetItemName"
            class="d-flex align-center justify-space-between"
          >
            <div class="d-flex align-center ga-1">
              <span :class="item.frequency === 'ONE_TIME' ? 'text-blue font-weight-medium' : ''">
                {{ item.budgetItemName }}
              </span>
              <span v-if="item.frequency === 'ONE_TIME'" class="text-blue font-weight-bold">*</span>
            </div>
            <span :class="item.frequency === 'ONE_TIME' ? 'text-blue font-weight-medium' : ''">
              {{ formatCurrency(item.monthlyAmount) }}
            </span>
          </div>
          <div class="d-flex align-center justify-space-between font-weight-bold">
            <span>{{ group.name }} Gesamt</span>
            <span>{{ formatCurrency(incomeTotalsByPerson.get(group.name)) }}</span>
          </div>
        </div>
      </div>
      <div class="d-flex align-center justify-space-between font-weight-bold">
        <span>Gesamteinkommen</span>
        <span>{{ formatCurrency(summary?.totalIncome) }}</span>
      </div>
    </template>
    <v-list-item v-else>
      <v-list-item-title>Füge Einnahmen hinzu, um diese Liste zu füllen.</v-list-item-title>
    </v-list-item>
  </v-list>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  summary: { type: Object, default: null },
  formatCurrency: { type: Function, required: true }
});

const groupedIncomeByPerson = computed(() => {
  const items = props.summary?.incomeByBudgetItem || [];
  const groups = new Map();
  items.forEach((item) => {
    const personName = item.personName || "Gemeinsam";
    if (!groups.has(personName)) {
      groups.set(personName, []);
    }
    groups.get(personName).push(item);
  });
  const ordered = Array.from(groups.entries())
    .map(([name, groupedItems]) => ({
      name,
      items: groupedItems.slice().sort((a, b) => a.budgetItemName.localeCompare(b.budgetItemName))
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
  return [
    ...ordered.filter((entry) => entry.name === "Gemeinsam"),
    ...ordered.filter((entry) => entry.name !== "Gemeinsam")
  ];
});

const incomeTotalsByPerson = computed(() => {
  const totals = new Map();
  (props.summary?.incomeByPerson || []).forEach((item) => {
    totals.set(item.personName, item.monthlyAmount);
  });
  return totals;
});

</script>
