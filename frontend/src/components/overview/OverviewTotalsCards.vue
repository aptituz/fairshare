<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-row>
    <v-col cols="12">
      <v-card variant="tonal">
        <v-card-title class="text-subtitle-1">Einnahmen</v-card-title>
        <v-card-text class="text-h6">
          {{ formatCurrency(summary?.totalIncome) }}
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12">
      <v-card variant="tonal">
        <v-card-title class="text-subtitle-1">Ausgaben</v-card-title>
        <v-card-text class="text-h6">
          {{ formatCurrency(summary?.totalExpenses) }}
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12">
      <v-card color="secondary" variant="tonal">
        <v-card-title class="text-subtitle-1">Saldo</v-card-title>
        <v-card-text class="text-h6">
          {{ formatCurrency(summary?.netResult) }}
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
const props = defineProps({
  summary: { type: Object, default: null },
  formatCurrency: { type: Function, required: true }
});
</script>
