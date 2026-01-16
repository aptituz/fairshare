<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <div class="month-year-picker d-flex ga-2 align-center">
    <v-btn
      v-if="showArrows"
      icon
      size="small"
      variant="text"
      @click="shiftPeriod(-1)"
    >
      <v-icon icon="mdi-chevron-left" />
    </v-btn>
    <v-select
      v-if="showMonth"
      v-model="selectedMonth"
      :items="monthOptions"
      item-title="label"
      item-value="value"
      :label="label"
      density="compact"
      hide-details
      :clearable="clearable"
      class="flex-grow-1 month-year-picker__select"
    />
    <v-select
      v-model="selectedYear"
      :items="yearOptions"
      :label="yearLabel"
      density="compact"
      hide-details
      :clearable="clearable"
      class="flex-grow-1 month-year-picker__select"
    />
    <v-btn
      v-if="showArrows"
      icon
      size="small"
      variant="text"
      @click="shiftPeriod(1)"
    >
      <v-icon icon="mdi-chevron-right" />
    </v-btn>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "" },
  label: { type: String, default: "Monat" },
  yearLabel: { type: String, default: "Jahr" },
  yearRange: { type: Number, default: 5 },
  allowEmpty: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  showArrows: { type: Boolean, default: false },
  showMonth: { type: Boolean, default: true }
});

const emit = defineEmits(["update:modelValue"]);

const now = new Date();
const currentYear = now.getFullYear();
const currentMonth = String(now.getMonth() + 1).padStart(2, "0");

const selectedMonth = ref(props.allowEmpty ? "" : currentMonth);
const selectedYear = ref(props.allowEmpty ? "" : currentYear);

const monthOptions = [
  { label: "Januar", value: "01" },
  { label: "Februar", value: "02" },
  { label: "Maerz", value: "03" },
  { label: "April", value: "04" },
  { label: "Mai", value: "05" },
  { label: "Juni", value: "06" },
  { label: "Juli", value: "07" },
  { label: "August", value: "08" },
  { label: "September", value: "09" },
  { label: "Oktober", value: "10" },
  { label: "November", value: "11" },
  { label: "Dezember", value: "12" }
];

const yearOptions = computed(() => {
  const start = currentYear - props.yearRange;
  const end = currentYear + props.yearRange;
  const years = [];
  for (let year = start; year <= end; year += 1) {
    years.push(year);
  }
  return years;
});

const emitValue = () => {
  const monthValue = selectedMonth.value || currentMonth;
  const yearValue = selectedYear.value || "";
  if (!yearValue || (props.showMonth && !monthValue)) {
    if (props.allowEmpty) {
      emit("update:modelValue", "");
    }
    return;
  }
  emit("update:modelValue", `${yearValue}-${monthValue}`);
};

const shiftPeriod = (delta) => {
  const yearValue = selectedYear.value || currentYear;
  const monthValue = selectedMonth.value || currentMonth;
  if (props.showMonth) {
    const date = new Date(Number(yearValue), Number(monthValue) - 1, 1);
    date.setMonth(date.getMonth() + delta);
    selectedYear.value = date.getFullYear();
    selectedMonth.value = String(date.getMonth() + 1).padStart(2, "0");
    return;
  }
  selectedYear.value = Number(yearValue) + delta;
};

watch(
  () => props.modelValue,
  (value) => {
    if (!value) {
      if (props.allowEmpty) {
        selectedYear.value = "";
        selectedMonth.value = "";
      }
      return;
    }
    const [year, month] = value.split("-");
    if (year) {
      selectedYear.value = Number(year);
    }
    if (month) {
      selectedMonth.value = month;
    }
  },
  { immediate: true }
);

watch([selectedYear, selectedMonth], ([year, month]) => {
  if (year == null) {
    selectedYear.value = "";
  }
  if (month == null) {
    selectedMonth.value = "";
  }
  emitValue();
});
</script>
