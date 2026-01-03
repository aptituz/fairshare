<template>
  <v-card>
    <v-card-title>Monatsübersicht</v-card-title>
    <v-card-subtitle>Monatliche Auswertung der wiederkehrenden Posten.</v-card-subtitle>
    <v-card-text class="text-caption">* markiert Einmalposten in diesem Monat.</v-card-text>
    <v-divider class="my-4" />
    <div class="d-flex flex-column ga-6">
      <v-sheet class="pa-4" border rounded>
        <div class="text-subtitle-1 font-weight-bold mb-3">Einnahmen</div>
        <IncomeSummaryList :summary="summary" :formatCurrency="formatCurrency" />
      </v-sheet>
      <v-sheet class="pa-4" border rounded>
        <div class="text-subtitle-1 font-weight-bold mb-3">Ausgaben</div>
        <ExpenseSummaryList
          :summary="summary"
          :categories="categories"
          :formatCurrency="formatCurrency"
          :categoryRank="categoryRank"
        />
      </v-sheet>
    </div>
  </v-card>
</template>

<script setup>
import IncomeSummaryList from "./IncomeSummaryList.vue";
import ExpenseSummaryList from "./ExpenseSummaryList.vue";

const props = defineProps({
  summary: { type: Object, default: null },
  categories: { type: Array, required: true },
  formatCurrency: { type: Function, required: true },
  categoryRank: { type: Function, required: true }
});
</script>
