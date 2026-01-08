<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-app>
    <v-navigation-drawer permanent width="280">
      <v-list nav v-model:opened="openGroups">
        <v-list-item
          title="Overview"
          :active="currentView === 'overview'"
          @click="navigate('overview')"
        />
        <v-list-item
          title="Kostenverteilung"
          :active="currentView === 'kostenverteilung'"
          @click="navigate('kostenverteilung')"
        />
        <v-list-group value="datenerfassung">
          <template #activator="{ props }">
            <v-list-item
              v-bind="props"
              title="Datenerfassung"
              :active="currentView === 'datenerfassung'"
              @click="navigate('datenerfassung', currentSubView)"
            />
          </template>
          <v-list-item
            v-for="item in datenerfassungNavItems"
            :key="item.key"
            :title="item.title"
            :active="currentView === 'datenerfassung' && currentSubView === item.key"
            @click="navigate('datenerfassung', item.key)"
          />
        </v-list-group>
        <v-list-group value="stammdaten">
          <template #activator="{ props }">
            <v-list-item
              v-bind="props"
              title="Stammdaten"
              :active="currentView === 'stammdaten'"
              @click="navigate('stammdaten', 'categories')"
            />
          </template>
          <v-list-item
            title="Kategorien"
            :active="currentView === 'stammdaten' && currentSubView === 'categories'"
            @click="navigate('stammdaten', 'categories')"
          />
          <v-list-item
            title="Personen"
            :active="currentView === 'stammdaten' && currentSubView === 'persons'"
            @click="navigate('stammdaten', 'persons')"
          />
        </v-list-group>
      </v-list>
      <v-divider />
      <v-list density="compact">
        <v-list-item title="Letzte Aktualisierung" :subtitle="summary ? 'Aktuell' : 'Laedt...'" />
      </v-list>
      <v-divider class="my-2" />
      <div class="px-4 pb-4">
        <MonthYearPicker :model-value="summaryMonth" @update:modelValue="updateMonth" />
      </div>
    </v-navigation-drawer>

    <v-main>
      <v-container fluid class="pa-6">
        <slot />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref } from "vue";
import MonthYearPicker from "./MonthYearPicker.vue";

const props = defineProps({
  currentView: { type: String, required: true },
  currentSubView: { type: String, required: true },
  datenerfassungNavItems: { type: Array, required: true },
  summary: { type: Object, default: null },
  summaryMonth: { type: String, required: true }
});

const emit = defineEmits(["navigate", "month-change"]);

const openGroups = ref(["datenerfassung", "stammdaten"]);

const navigate = (view, subView) => {
  emit("navigate", { view, subView });
};

const updateMonth = (value) => {
  emit("month-change", value);
};
</script>
