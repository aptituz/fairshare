<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <div v-if="currentSubView === 'categories'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Strukturiere Kategorien fuer wiederkehrende Posten.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <CategoryManagementCard
          :incomeCategoryOptions="incomeCategoryOptions"
          :expenseCategoryOptions="expenseCategoryOptions"
          :categorySaving="categorySaving"
          :createCategory="createCategory"
          :updateCategory="updateCategory"
          :deleteCategory="deleteCategory"
        />
      </v-col>
    </v-row>
  </div>

  <div v-if="currentSubView === 'persons'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Verwalte Personen fuer gemeinsame Budgets.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <PersonManagementCard
          :persons="persons"
          :personSaving="personSaving"
          :createPerson="createPerson"
          :updatePerson="updatePerson"
          :setPersonPassword="setPersonPassword"
        />
      </v-col>
    </v-row>
  </div>

  <div v-if="currentSubView === 'savings-accounts'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Pflege Sparkonten und ordne sie Personen zu.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <SavingsAccountManagementCard
          :savingsAccounts="savingsAccounts"
          :persons="persons"
          :summaryMonth="summaryMonth"
          :createSavingsAccount="createSavingsAccount"
          :updateSavingsAccount="updateSavingsAccount"
          :deleteSavingsAccount="deleteSavingsAccount"
        />
      </v-col>
    </v-row>
  </div>

  <div v-if="currentSubView === 'data-transfer'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Exportiere oder importiere den gesamten Datenbestand.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <DataTransferCard
          :exportData="exportData"
          :importData="importData"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import CategoryManagementCard from "../components/stammdaten/CategoryManagementCard.vue";
import PersonManagementCard from "../components/stammdaten/PersonManagementCard.vue";
import SavingsAccountManagementCard from "../components/stammdaten/SavingsAccountManagementCard.vue";
import DataTransferCard from "../components/stammdaten/DataTransferCard.vue";

const props = defineProps({
  currentSubView: { type: String, required: true },
  incomeCategoryOptions: { type: Array, required: true },
  expenseCategoryOptions: { type: Array, required: true },
  persons: { type: Array, required: true },
  savingsAccounts: { type: Array, required: true },
  summaryMonth: { type: String, required: true },
  categorySaving: { type: Boolean, required: true },
  personSaving: { type: Boolean, required: true },
  createCategory: { type: Function, required: true },
  updateCategory: { type: Function, required: true },
  deleteCategory: { type: Function, required: true },
  createPerson: { type: Function, required: true },
  updatePerson: { type: Function, required: true },
  setPersonPassword: { type: Function, required: true },
  createSavingsAccount: { type: Function, required: true },
  updateSavingsAccount: { type: Function, required: true },
  deleteSavingsAccount: { type: Function, required: true },
  exportData: { type: Function, required: true },
  importData: { type: Function, required: true }
});
</script>
