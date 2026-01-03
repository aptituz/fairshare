<template>
  <div class="d-flex ga-2">
    <v-select
      v-model="selectedMonth"
      :items="monthOptions"
      item-title="label"
      item-value="value"
      :label="label"
      density="compact"
      hide-details
      :clearable="clearable"
      class="flex-grow-1"
    />
    <v-select
      v-model="selectedYear"
      :items="yearOptions"
      :label="yearLabel"
      density="compact"
      hide-details
      :clearable="clearable"
      class="flex-grow-1"
    />
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
  clearable: { type: Boolean, default: false }
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
  const monthValue = selectedMonth.value || "";
  const yearValue = selectedYear.value || "";
  if (!monthValue || !yearValue) {
    if (props.allowEmpty) {
      emit("update:modelValue", "");
    }
    return;
  }
  emit("update:modelValue", `${yearValue}-${monthValue}`);
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
