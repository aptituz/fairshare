<template>
  <v-row>
    <v-col cols="12" md="7">
      <OverviewIntroCard />
    </v-col>
    <v-col cols="12" md="5">
      <OverviewTotalsCards :summary="summary" :formatCurrency="formatCurrency" />
    </v-col>
  </v-row>

  <v-row class="mt-6">
    <v-col cols="12">
      <MonthlySummaryCard
        :summary="summary"
        :categories="categories"
        :formatCurrency="formatCurrency"
        :categoryRank="categoryRank"
      />
    </v-col>
  </v-row>
</template>

<script setup>
import OverviewIntroCard from "../components/overview/OverviewIntroCard.vue";
import OverviewTotalsCards from "../components/overview/OverviewTotalsCards.vue";
import MonthlySummaryCard from "../components/overview/MonthlySummaryCard.vue";

const props = defineProps({
  summary: { type: Object, default: null },
  categories: { type: Array, required: true },
  formatCurrency: { type: Function, required: true },
  categoryRank: { type: Function, required: true }
});
</script>
