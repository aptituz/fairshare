<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-row>
    <v-col cols="12">
      <h2 class="text-h5 mb-2">Kostenverteilung</h2>
      <p class="text-body-2">
        Ausgleich der gemeinsamen Kosten, damit beide das gleiche Restbudget behalten.
      </p>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="12">
      <CostSplitCard :summary="summary" :formatCurrency="formatCurrency" />
    </v-col>
  </v-row>
</template>

<script setup>
import CostSplitCard from "../components/kostenverteilung/CostSplitCard.vue";

const props = defineProps({
  summary: { type: Object, default: null },
  formatCurrency: { type: Function, required: true }
});
</script>
