<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <div class="d-flex flex-column ga-4">
    <template v-if="groupedExpensesByPerson.length">
      <div v-for="group in groupedExpensesByPerson" :key="group.name">
        <div class="text-subtitle-2 font-weight-bold mb-2">
          {{ group.name }}
        </div>
        <div class="d-flex flex-column ga-2">
          <v-expansion-panels variant="accordion" multiple>
            <v-expansion-panel v-for="category in group.categories" :key="category.key">
              <template #title>
                <div class="d-flex align-center justify-space-between w-100">
                  <div class="d-flex align-center ga-1">
                    <span class="font-weight-medium" :class="categoryColorClass(category)">
                      {{ category.name }}
                    </span>
                  </div>
                  <span :class="categoryColorClass(category)">
                    {{ formatCurrency(category.total) }}
                  </span>
                </div>
              </template>
              <template #text>
                <v-list density="compact">
                  <v-list-item
                    v-for="item in category.items"
                    :key="item.budgetItemId ?? item.budgetItemName"
                  >
                    <v-list-item-title
                      :class="expenseItemClass(item)"
                    >
                      <span>{{ item.budgetItemName }}</span>
                    </v-list-item-title>
                    <template #append>
                      <span :class="expenseItemClass(item)">
                        {{ formatCurrency(item.monthlyAmount) }}
                      </span>
                    </template>
                  </v-list-item>
                </v-list>
              </template>
            </v-expansion-panel>
          </v-expansion-panels>
          <div class="d-flex align-center justify-space-between font-weight-bold">
            <span>{{ group.name }} Gesamt</span>
            <span>{{ formatCurrency(group.total) }}</span>
          </div>
        </div>
      </div>
      <div class="d-flex align-center justify-space-between font-weight-bold">
        <span>Gesamthaushaltsausgaben</span>
        <span>{{ formatCurrency(summary?.totalHouseholdExpenditure) }}</span>
      </div>
    </template>
    <div v-else>Füge Ausgaben hinzu, um diese Liste zu füllen.</div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  summary: { type: Object, default: null },
  categories: { type: Array, required: true },
  formatCurrency: { type: Function, required: true },
  categoryRank: { type: Function, required: true }
});

const groupedExpensesByPerson = computed(() => {
  const items = props.summary?.expensesByBudgetItem || [];
  const groups = new Map();
  items.forEach((item) => {
    const personName = item.personName || "Gemeinsam";
    const categoryKey = item.categoryId ?? item.categoryName ?? "unknown";
    const categoryName = item.categoryName || "Keine Kategorie";
    if (!groups.has(personName)) {
      groups.set(personName, new Map());
    }
    const categoryMap = groups.get(personName);
    if (!categoryMap.has(categoryKey)) {
      categoryMap.set(categoryKey, {
        name: categoryName,
        total: 0,
        categoryId: item.categoryId,
        hasOneTime: false,
        items: [],
        oneTimeDelta: 0
      });
    }
    const entry = categoryMap.get(categoryKey);
    entry.total += Number(item.monthlyAmount || 0);
    entry.items.push(item);
    if (item.frequency === "ONE_TIME") {
      entry.hasOneTime = true;
      entry.oneTimeDelta += Number(item.monthlyAmount || 0);
    }
  });

  const orderedGroups = Array.from(groups.entries())
    .map(([name, categoryMap]) => {
      const categories = Array.from(categoryMap.entries())
        .map(([key, value]) => ({
          key,
          name: value.name,
          total: value.total,
          rank: props.categoryRank(value.categoryId, value.name),
          hasOneTime: value.hasOneTime,
          oneTimeDelta: value.oneTimeDelta,
          items: value.items.slice().sort((a, b) => a.budgetItemName.localeCompare(b.budgetItemName))
        }))
        .sort((a, b) => a.rank - b.rank);
      const total = categories.reduce((sum, category) => sum + category.total, 0);
      return { name, categories, total };
    })
    .sort((a, b) => a.name.localeCompare(b.name));

  return [
    ...orderedGroups.filter((entry) => entry.name === "Gemeinsam"),
    ...orderedGroups.filter((entry) => entry.name !== "Gemeinsam")
  ];
});

const colorClassForAmount = (amount) => {
  const numeric = Number(amount || 0);
  if (numeric < 0) {
    return "text-green font-weight-medium";
  }
  if (numeric > 0) {
    return "text-red font-weight-medium";
  }
  return "";
};

const expenseItemClass = (item) => {
  if (item.frequency === "ONE_TIME") {
    return colorClassForAmount(item.monthlyAmount);
  }
  return "";
};

const categoryColorClass = (category) => {
  if (!category.hasOneTime) {
    return "";
  }
  return colorClassForAmount(category.oneTimeDelta);
};
</script>
